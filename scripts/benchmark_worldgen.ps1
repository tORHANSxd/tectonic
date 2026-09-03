[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$Scenario,

    [Parameter(Mandatory = $true)]
    [ValidateSet("true", "false", "unsupported")]
    [string]$OreFix,

    [bool]$ModEnabled = $true,
    [long[]]$Seeds = @(0, 1, -1, 123456789, -987654321987654321),
    [int[]]$MinYValues = @(-64, -128, -320),
    [int]$MaxY = 320,
    [int]$ChunkMinX = 64,
    [int]$ChunkMinZ = 64,
    [int]$ChunkMaxX = 79,
    [int]$ChunkMaxZ = 79,
    [string]$RunId = (Get-Date).ToUniversalTime().ToString("yyyyMMdd-HHmmss"),
    [string]$RepositoryRoot = (Join-Path $PSScriptRoot ".."),
    [string]$JavaHome = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot",
    [string]$PythonExecutable = "python",
    [int]$CaseTimeoutMinutes = 20,
    [switch]$IncludeMaterials,
    [switch]$VerboseConsole,
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

function Resolve-FullPath([string]$Path, [string]$Base) {
    if ([IO.Path]::IsPathRooted($Path)) {
        return [IO.Path]::GetFullPath($Path)
    }
    return [IO.Path]::GetFullPath((Join-Path $Base $Path))
}

function Assert-ChildPath([string]$Path, [string]$Parent, [string]$Label) {
    $fullPath = [IO.Path]::GetFullPath($Path)
    $fullParent = [IO.Path]::GetFullPath($Parent).TrimEnd('\') + '\'
    if (-not $fullPath.StartsWith($fullParent, [StringComparison]::OrdinalIgnoreCase)) {
        throw "$Label 不在允许目录内: $fullPath"
    }
}

function Write-Utf8NoBom([string]$Path, [string]$Content) {
    [IO.File]::WriteAllText($Path, $Content, [Text.UTF8Encoding]::new($false))
}

function Set-ServerProperty([string]$Text, [string]$Name, [string]$Value) {
    $lines = [Collections.Generic.List[string]]::new()
    $found = $false
    foreach ($line in ($Text -split "`r?`n")) {
        if ($line -match ('^' + [Regex]::Escape($Name) + '=')) {
            $lines.Add("$Name=$Value")
            $found = $true
        } else {
            $lines.Add($line)
        }
    }
    if (-not $found) {
        $lines.Add("$Name=$Value")
    }
    return ($lines -join "`n").TrimEnd("`n") + "`n"
}

function Get-ServerProperty([string]$Text, [string]$Name) {
    foreach ($line in ($Text -split "`r?`n")) {
        if ($line -match ('^' + [Regex]::Escape($Name) + '=(.*)$')) {
            return $Matches[1]
        }
    }
    throw "server.properties 缺少 $Name"
}

function Assert-PortAvailable([int]$Port) {
    $listener = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, $Port)
    $listener.Server.ExclusiveAddressUse = $true
    try {
        $listener.Start()
    } catch {
        throw "服务端端口 $Port 已被占用，批处理未启动"
    } finally {
        $listener.Stop()
    }
}

function Convert-ToToken([long]$Value) {
    $text = $Value.ToString([Globalization.CultureInfo]::InvariantCulture)
    if ($text.StartsWith("-", [StringComparison]::Ordinal)) {
        return "m" + $text.Substring(1)
    }
    return $text
}

function Send-ServerCommand([Diagnostics.Process]$Process, [string]$Command) {
    $Process.StandardInput.WriteLine($Command)
    $Process.StandardInput.Flush()
}

function Stop-ProcessTree([Diagnostics.Process]$Process) {
    if ($null -eq $Process -or $Process.HasExited) {
        return
    }
    try {
        Send-ServerCommand $Process "stop"
        if ($Process.WaitForExit(30000)) {
            return
        }
    } catch {
        # Fall through to terminating the exact child process tree.
    }
    try {
        $Process.Kill($true)
    } catch {
        $Process.Kill()
    }
    $Process.WaitForExit()
}

function Invoke-ServerCase(
    [string]$CaseId,
    [string]$WorldName,
    [string]$WorldPath,
    [string]$ResultPath,
    [string]$GradleWrapper,
    [string]$RepoRoot,
    [string]$RunRoot,
    [string]$AnalyzerPath,
    [long]$Seed,
    [int]$MinY,
    [string]$AppliedConfig,
    [string]$AppliedProperties
) {
    New-Item -ItemType Directory -Path $ResultPath | Out-Null
    Write-Utf8NoBom (Join-Path $ResultPath "requested-tectonic.json") $AppliedConfig
    Write-Utf8NoBom (Join-Path $ResultPath "requested-server.properties") $AppliedProperties

    $consolePath = Join-Path $ResultPath "console.log"
    $consoleWriter = [IO.StreamWriter]::new($consolePath, $false, [Text.UTF8Encoding]::new($false))
    $startedAt = (Get-Date).ToUniversalTime()
    $stopwatch = [Diagnostics.Stopwatch]::StartNew()
    $stage = "waiting-ready"
    $process = $null
    $completed = $false

    try {
        $info = [Diagnostics.ProcessStartInfo]::new()
        $info.FileName = "$env:ComSpec"
        $info.Arguments = "/d /s /c `"`"$GradleWrapper`" --no-daemon runForge1201Server --console=plain 2>&1`""
        $info.WorkingDirectory = $RepoRoot
        $info.UseShellExecute = $false
        $info.RedirectStandardInput = $true
        $info.RedirectStandardOutput = $true
        $info.CreateNoWindow = $true
        $info.Environment["JAVA_HOME"] = $JavaHome
        $info.Environment["PATH"] = "$JavaHome\bin;" + $info.Environment["PATH"]
        $info.Environment["TERM"] = "dumb"

        $process = [Diagnostics.Process]::new()
        $process.StartInfo = $info
        if (-not $process.Start()) {
            throw "无法启动 Gradle 服务端"
        }
        $script:activeProcess = $process
        Write-Host "[$CaseId] PID=$($process.Id)，等待服务端就绪"

        $readTask = $process.StandardOutput.ReadLineAsync()
        while ($true) {
            if ($readTask.Wait(250)) {
                $line = $readTask.Result
                if ($null -eq $line) {
                    break
                }
                $consoleWriter.WriteLine($line)
                $consoleWriter.Flush()
                if ($VerboseConsole) {
                    Write-Host "[$CaseId] $line"
                }

                if ($stage -eq "waiting-ready" -and $line.Contains("Done (")) {
                    Write-Host "[$CaseId] 服务端就绪，生成目标区块"
                    $blockMinX = $ChunkMinX * 16
                    $blockMinZ = $ChunkMinZ * 16
                    $blockMaxX = ($ChunkMaxX + 1) * 16 - 1
                    $blockMaxZ = ($ChunkMaxZ + 1) * 16 - 1
                    Send-ServerCommand $process "execute in minecraft:overworld run forceload add $blockMinX $blockMinZ $blockMaxX $blockMaxZ"
                    $stage = "waiting-force-add"
                } elseif (
                    $stage -eq "waiting-force-add" -and
                    $line.Contains("Marked ") -and
                    $line.Contains("from [$ChunkMinX, $ChunkMinZ] to [$ChunkMaxX, $ChunkMaxZ] to be force loaded")
                ) {
                    Write-Host "[$CaseId] 目标区块已生成，解除强加载"
                    $blockMinX = $ChunkMinX * 16
                    $blockMinZ = $ChunkMinZ * 16
                    $blockMaxX = ($ChunkMaxX + 1) * 16 - 1
                    $blockMaxZ = ($ChunkMaxZ + 1) * 16 - 1
                    Send-ServerCommand $process "execute in minecraft:overworld run forceload remove $blockMinX $blockMinZ $blockMaxX $blockMaxZ"
                    $stage = "waiting-force-remove"
                } elseif (
                    $stage -eq "waiting-force-remove" -and
                    $line.Contains("Unmarked ") -and
                    $line.Contains("from [$ChunkMinX, $ChunkMinZ] to [$ChunkMaxX, $ChunkMaxZ] for force loading")
                ) {
                    Send-ServerCommand $process "save-all flush"
                    $stage = "waiting-save"
                } elseif ($stage -eq "waiting-save" -and $line.Contains("Saved the game")) {
                    Write-Host "[$CaseId] 世界已落盘，停止服务端"
                    Send-ServerCommand $process "stop"
                    $stage = "waiting-exit"
                }
                $readTask = $process.StandardOutput.ReadLineAsync()
            }

            if ($stopwatch.Elapsed.TotalMinutes -gt $CaseTimeoutMinutes) {
                throw "case 超过 $CaseTimeoutMinutes 分钟，当前阶段: $stage"
            }
            if ($process.HasExited -and $readTask.IsCompleted) {
                continue
            }
        }
        $process.WaitForExit()
        if ($process.ExitCode -ne 0) {
            throw "Gradle 服务端退出码为 $($process.ExitCode)，阶段: $stage"
        }
        if ($stage -ne "waiting-exit") {
            throw "服务端在完成协议前退出，阶段: $stage"
        }
        $completed = $true
    } finally {
        $consoleWriter.Dispose()
        Stop-ProcessTree $process
        $script:activeProcess = $null
    }

    if (-not $completed) {
        throw "case 未完成: $CaseId"
    }
    if (-not (Test-Path -LiteralPath $WorldPath -PathType Container)) {
        throw "服务端未创建世界目录: $WorldPath"
    }

    $latestLog = Join-Path $RunRoot "logs\latest.log"
    if (Test-Path -LiteralPath $latestLog -PathType Leaf) {
        Copy-Item -LiteralPath $latestLog -Destination (Join-Path $ResultPath "latest.log")
    }
    Copy-Item -LiteralPath (Join-Path $RunRoot "config\tectonic.json") -Destination (Join-Path $ResultPath "effective-tectonic.json")
    Copy-Item -LiteralPath (Join-Path $RunRoot "server.properties") -Destination (Join-Path $ResultPath "effective-server.properties")

    $gitCommit = (& git -C $RepoRoot rev-parse HEAD).Trim()
    if ($LASTEXITCODE -ne 0) {
        throw "无法读取 Git HEAD"
    }
    $manifest = [ordered]@{
        case_id = $CaseId
        scenario = $Scenario
        seed = $Seed.ToString([Globalization.CultureInfo]::InvariantCulture)
        min_y = $MinY
        max_y = $MaxY
        mod_enabled = $ModEnabled
        ore_fix = $OreFix
        world_name = $WorldName
        world = $WorldPath
        chunks_inclusive = @($ChunkMinX, $ChunkMinZ, $ChunkMaxX, $ChunkMaxZ)
        chunk_count = ($ChunkMaxX - $ChunkMinX + 1) * ($ChunkMaxZ - $ChunkMinZ + 1)
        git_commit = $gitCommit
        java_home = $JavaHome
        started_at_utc = $startedAt.ToString("o")
        finished_at_utc = (Get-Date).ToUniversalTime().ToString("o")
        elapsed_seconds = [Math]::Round($stopwatch.Elapsed.TotalSeconds, 3)
        gradle_exit_code = $process.ExitCode
    }
    Write-Utf8NoBom (Join-Path $ResultPath "manifest.json") (($manifest | ConvertTo-Json -Depth 8) + "`n")

    $analyzerArgs = @(
        $AnalyzerPath,
        "scan",
        $WorldPath,
        "--chunks", $ChunkMinX, $ChunkMinZ, $ChunkMaxX, $ChunkMaxZ,
        "--case-id", $CaseId,
        "--scenario", $Scenario,
        "--seed", $Seed,
        "--expected-min-y", $MinY,
        "--expected-max-y", $MaxY,
        "--mod-enabled", $ModEnabled.ToString().ToLowerInvariant(),
        "--ore-fix", $OreFix,
        "--csv", (Join-Path $ResultPath "ore-counts.csv"),
        "--summary", (Join-Path $ResultPath "ore-summary.json")
    )
    if ($IncludeMaterials) {
        $analyzerArgs += "--include-materials"
    }
    & $PythonExecutable @analyzerArgs
    if ($LASTEXITCODE -ne 0) {
        throw "矿物分析器失败: $CaseId"
    }
}

$repoRoot = (Resolve-Path -LiteralPath $RepositoryRoot).Path
$runRoot = Join-Path $repoRoot "run"
$configPath = Join-Path $runRoot "config\tectonic.json"
$serverPropertiesPath = Join-Path $runRoot "server.properties"
$gradleWrapper = Join-Path $repoRoot "gradlew.bat"
$analyzerPath = Join-Path $PSScriptRoot "analyze_ore_distribution.py"
$resultRoot = Join-Path $runRoot "batch-results\$RunId"
$trackedPaths = @(
    $serverPropertiesPath,
    $configPath,
    "$configPath.bak",
    "$configPath.invalid"
)

if (-not (Test-Path -LiteralPath $gradleWrapper -PathType Leaf)) {
    throw "找不到 Gradle wrapper: $gradleWrapper"
}
if (-not (Test-Path -LiteralPath $analyzerPath -PathType Leaf)) {
    throw "找不到矿物分析器: $analyzerPath"
}
if (-not (Test-Path -LiteralPath "$JavaHome\bin\java.exe" -PathType Leaf)) {
    throw "找不到指定 JDK 21: $JavaHome"
}
$javaVersionOutput = (& "$JavaHome\bin\java.exe" -version 2>&1) -join "`n"
if ($LASTEXITCODE -ne 0 -or $javaVersionOutput -notmatch 'version "21(?:\.|\")') {
    throw "指定 Java 不是 JDK 21: $JavaHome`n$javaVersionOutput"
}
if (-not (Test-Path -LiteralPath $configPath -PathType Leaf)) {
    throw "找不到基准配置: $configPath"
}
if (-not (Test-Path -LiteralPath $serverPropertiesPath -PathType Leaf)) {
    throw "找不到 server.properties: $serverPropertiesPath"
}
$eulaPath = Join-Path $runRoot "eula.txt"
if (-not (Test-Path -LiteralPath $eulaPath -PathType Leaf) -or
    -not (Get-Content -LiteralPath $eulaPath -Raw).Contains("eula=true")) {
    throw "run/eula.txt 未包含 eula=true；脚本不会代替用户接受 EULA"
}
if ($ChunkMinX -gt $ChunkMaxX -or $ChunkMinZ -gt $ChunkMaxZ) {
    throw "chunk 闭区间非法"
}
$chunkCount = ($ChunkMaxX - $ChunkMinX + 1) * ($ChunkMaxZ - $ChunkMinZ + 1)
if ($chunkCount -gt 256) {
    throw "forceload 单次最多 256 个区块，当前请求 $chunkCount"
}
foreach ($minY in $MinYValues) {
    if ($minY % 16 -ne 0 -or $minY -lt -2032 -or $minY -gt -64) {
        throw "minY=$minY 非法；必须是 [-2032,-64] 内的 16 倍数"
    }
}
if ($MaxY % 16 -ne 0 -or $MaxY -le ($MinYValues | Measure-Object -Minimum).Minimum) {
    throw "MaxY=$MaxY 非法"
}
if (-not $ModEnabled -and $OreFix -eq "true") {
    throw "mod_enabled=false 时不能声明 ore_fix=true"
}

$baseConfigText = Get-Content -LiteralPath $configPath -Raw
$basePropertiesText = Get-Content -LiteralPath $serverPropertiesPath -Raw
$port = [int](Get-ServerProperty $basePropertiesText "server-port")
Assert-PortAvailable $port

$cases = [Collections.Generic.List[object]]::new()
foreach ($minY in $MinYValues) {
    foreach ($seed in $Seeds) {
        $minToken = Convert-ToToken $minY
        $seedToken = Convert-ToToken $seed
        $caseId = "$RunId-$Scenario-miny-$minToken-seed-$seedToken"
        $worldName = "batch-$caseId"
        $worldPath = Join-Path $runRoot $worldName
        $resultPath = Join-Path $resultRoot $caseId
        Assert-ChildPath $worldPath $runRoot "世界目录"
        Assert-ChildPath $resultPath $runRoot "结果目录"
        if (Test-Path -LiteralPath $worldPath) {
            throw "世界目录已存在，拒绝覆盖或复用: $worldPath"
        }
        if (Test-Path -LiteralPath $resultPath) {
            throw "结果目录已存在，拒绝覆盖: $resultPath"
        }
        $cases.Add([pscustomobject]@{
            CaseId = $caseId
            WorldName = $worldName
            WorldPath = $worldPath
            ResultPath = $resultPath
            Seed = $seed
            MinY = $minY
        })
    }
}

Write-Host "批处理: scenario=$Scenario, cases=$($cases.Count), chunks/case=$chunkCount, runId=$RunId"
if ($DryRun) {
    $cases | Select-Object CaseId, Seed, MinY, WorldPath, ResultPath | Format-Table -AutoSize
    return
}

$tempBase = [IO.Path]::GetFullPath([IO.Path]::GetTempPath())
$tempRoot = Join-Path $tempBase ("tectonic-worldgen-batch-" + [Guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $tempRoot | Out-Null
$saved = [Collections.Generic.List[object]]::new()
foreach ($path in $trackedPaths) {
    $exists = Test-Path -LiteralPath $path -PathType Leaf
    $backup = Join-Path $tempRoot ([Guid]::NewGuid().ToString("N"))
    if ($exists) {
        Copy-Item -LiteralPath $path -Destination $backup
    }
    $saved.Add([pscustomobject]@{ Path = $path; Existed = $exists; Backup = $backup })
}

$script:activeProcess = $null
$completedCases = [Collections.Generic.List[string]]::new()
try {
    foreach ($case in $cases) {
        $config = $baseConfigText | ConvertFrom-Json
        $config.general.mod_enabled = $ModEnabled
        $config.global_terrain.min_y = $case.MinY
        if ($OreFix -eq "unsupported") {
            $config.caves.PSObject.Properties.Remove("ore_fix")
        } elseif ($null -eq $config.caves.PSObject.Properties["ore_fix"]) {
            $config.caves | Add-Member -NotePropertyName "ore_fix" -NotePropertyValue ($OreFix -eq "true")
        } else {
            $config.caves.ore_fix = ($OreFix -eq "true")
        }
        $appliedConfig = ($config | ConvertTo-Json -Depth 20) + "`n"
        $appliedProperties = Set-ServerProperty $basePropertiesText "level-name" $case.WorldName
        $appliedProperties = Set-ServerProperty $appliedProperties "level-seed" ($case.Seed.ToString([Globalization.CultureInfo]::InvariantCulture))
        Write-Utf8NoBom $configPath $appliedConfig
        Write-Utf8NoBom $serverPropertiesPath $appliedProperties

        Invoke-ServerCase `
            $case.CaseId `
            $case.WorldName `
            $case.WorldPath `
            $case.ResultPath `
            $gradleWrapper `
            $repoRoot `
            $runRoot `
            $analyzerPath `
            $case.Seed `
            $case.MinY `
            $appliedConfig `
            $appliedProperties
        $completedCases.Add($case.CaseId)
    }

    New-Item -ItemType Directory -Path $resultRoot -Force | Out-Null
    $batchManifest = [ordered]@{
        run_id = $RunId
        scenario = $Scenario
        ore_fix = $OreFix
        mod_enabled = $ModEnabled
        seeds = @($Seeds | ForEach-Object { $_.ToString([Globalization.CultureInfo]::InvariantCulture) })
        min_y_values = @($MinYValues)
        max_y = $MaxY
        chunks_inclusive = @($ChunkMinX, $ChunkMinZ, $ChunkMaxX, $ChunkMaxZ)
        chunks_per_case = $chunkCount
        completed_cases = @($completedCases)
        finished_at_utc = (Get-Date).ToUniversalTime().ToString("o")
    }
    Write-Utf8NoBom (Join-Path $resultRoot "batch-manifest.json") (($batchManifest | ConvertTo-Json -Depth 8) + "`n")
} finally {
    Stop-ProcessTree $script:activeProcess
    foreach ($entry in $saved) {
        Assert-ChildPath $entry.Path $runRoot "恢复目标"
        if ($entry.Existed) {
            Copy-Item -LiteralPath $entry.Backup -Destination $entry.Path -Force
        } elseif (Test-Path -LiteralPath $entry.Path -PathType Leaf) {
            Remove-Item -LiteralPath $entry.Path -Force
        }
    }
    $resolvedTempRoot = [IO.Path]::GetFullPath($tempRoot)
    if ($resolvedTempRoot.StartsWith($tempBase, [StringComparison]::OrdinalIgnoreCase) -and
        [IO.Path]::GetFileName($resolvedTempRoot).StartsWith("tectonic-worldgen-batch-", [StringComparison]::Ordinal)) {
        Remove-Item -LiteralPath $resolvedTempRoot -Recurse -Force
    }
}

Write-Host "批处理完成: $($completedCases.Count)/$($cases.Count)，结果: $resultRoot"
