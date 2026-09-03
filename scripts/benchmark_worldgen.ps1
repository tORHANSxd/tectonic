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
    [ValidateRange(1, 256)]
    [int]$TileWidthChunks = 16,
    [ValidateRange(1, 256)]
    [int]$TileHeightChunks = 16,
    [ValidateRange(0, 2147483647)]
    [int]$SaveEveryTiles = 4,
    [ValidateRange(0, 2147483647)]
    [int]$RestartEveryTiles = 8,
    [ValidateRange(1, 2147483647)]
    [int]$StageTimeoutMinutes = 20,
    [ValidateRange(0, 2147483647)]
    [int]$CaseTimeoutMinutes = 0,
    [ValidateRange(1, 255)]
    [int]$MaxBackgroundThreads = 1,
    [string]$ProductionServerRoot = "",
    [string]$ProductionForgeVersion = "",
    [switch]$IncludeMaterials,
    [switch]$SkipOreAnalysis,
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

function Get-TilePlan(
    [int]$MinX,
    [int]$MinZ,
    [int]$MaxX,
    [int]$MaxZ,
    [int]$TileWidth,
    [int]$TileHeight
) {
    $expectedCount = ([long]$MaxX - [long]$MinX + 1) * ([long]$MaxZ - [long]$MinZ + 1)
    $seen = [Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
    $tiles = [Collections.Generic.List[object]]::new()
    $chunkOrdinal = 0
    $tileOrdinal = 0

    for ($tileMinZ = $MinZ; $tileMinZ -le $MaxZ; $tileMinZ += $TileHeight) {
        $tileMaxZ = [Math]::Min($tileMinZ + $TileHeight - 1, $MaxZ)
        for ($tileMinX = $MinX; $tileMinX -le $MaxX; $tileMinX += $TileWidth) {
            $tileMaxX = [Math]::Min($tileMinX + $TileWidth - 1, $MaxX)
            $chunks = [Collections.Generic.List[object]]::new()
            $tileOrdinal++

            for ($chunkZ = $tileMinZ; $chunkZ -le $tileMaxZ; $chunkZ++) {
                for ($chunkX = $tileMinX; $chunkX -le $tileMaxX; $chunkX++) {
                    $key = "$chunkX,$chunkZ"
                    if (-not $seen.Add($key)) {
                        throw "tile 计划包含重复区块: $key"
                    }
                    $chunkOrdinal++
                    $chunks.Add([pscustomobject]@{
                        Ordinal = $chunkOrdinal
                        X = $chunkX
                        Z = $chunkZ
                    })
                }
            }

            if ($chunks.Count -gt 256) {
                throw "tile $tileOrdinal 超过 forceload 的 256 区块限制: $($chunks.Count)"
            }
            $tiles.Add([pscustomobject]@{
                Ordinal = $tileOrdinal
                MinX = $tileMinX
                MinZ = $tileMinZ
                MaxX = $tileMaxX
                MaxZ = $tileMaxZ
                Chunks = @($chunks)
            })
        }
    }

    if ($seen.Count -ne $expectedCount) {
        throw "tile 计划区块数错误: expected=$expectedCount actual=$($seen.Count)"
    }
    return @($tiles)
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

$script:serverFailurePattern = '(?i)\b(?:ERROR|FATAL)\b|InvalidInjection|InjectionError|Mixin apply failed|Failed to load datapacks|Unbound values in registry|No chunks were marked|No chunks were removed|Unknown or incomplete command'

function Write-HarnessEvent(
    [IO.StreamWriter]$Writer,
    [string]$EventName,
    [Collections.IDictionary]$Fields
) {
    $event = [ordered]@{
        time_utc = (Get-Date).ToUniversalTime().ToString("o")
        event = $EventName
    }
    foreach ($entry in $Fields.GetEnumerator()) {
        $event[$entry.Key] = $entry.Value
    }
    $Writer.WriteLine(($event | ConvertTo-Json -Depth 8 -Compress))
    $Writer.Flush()
}

function Write-ServerLine([object]$State, [string]$Line) {
    $State.LastLine = $Line
    if ($line -match $script:serverFailurePattern) {
        $State.FailureLine = $Line
    }
    $State.ConsoleWriter.WriteLine($Line)
    $State.ConsoleWriter.Flush()
    $State.AggregateWriter.WriteLine($Line)
    $State.AggregateWriter.Flush()
    if ($VerboseConsole) {
        Write-Host "[$($State.CaseId)/session-$($State.Ordinal)] $Line"
    }
}

function Assert-WaitBudget(
    [Diagnostics.Stopwatch]$StageStopwatch,
    [Diagnostics.Stopwatch]$CaseStopwatch,
    [string]$Stage
) {
    if ($StageStopwatch.Elapsed.TotalMinutes -gt $StageTimeoutMinutes) {
        throw "阶段超过 $StageTimeoutMinutes 分钟: $Stage"
    }
    if ($CaseTimeoutMinutes -gt 0 -and $CaseStopwatch.Elapsed.TotalMinutes -gt $CaseTimeoutMinutes) {
        throw "case 超过 $CaseTimeoutMinutes 分钟，当前阶段: $Stage"
    }
}

function Wait-ServerSignal(
    [object]$State,
    [string[]]$ExpectedFragments,
    [string]$Stage,
    [Diagnostics.Stopwatch]$CaseStopwatch
) {
    $stageStopwatch = [Diagnostics.Stopwatch]::StartNew()
    while ($true) {
        Assert-WaitBudget $stageStopwatch $CaseStopwatch $Stage
        if (-not $State.ReadTask.Wait(250)) {
            continue
        }

        $line = $State.ReadTask.Result
        if ($null -eq $line) {
            $State.Process.WaitForExit()
            throw "服务端在等待 $Stage 时退出，exit=$($State.Process.ExitCode)，last=$($State.LastLine)"
        }

        Write-ServerLine $State $line
        $failed = $line -match $script:serverFailurePattern
        $matched = $true
        foreach ($fragment in $ExpectedFragments) {
            if (-not $line.Contains($fragment)) {
                $matched = $false
                break
            }
        }
        $State.ReadTask = $State.Process.StandardOutput.ReadLineAsync()
        if ($failed) {
            throw "服务端在 $Stage 报告失败: $line"
        }
        if ($matched) {
            return $line
        }
    }
}

function Invoke-ServerCommandAndWait(
    [object]$State,
    [string]$Command,
    [string[]]$ExpectedFragments,
    [string]$Stage,
    [Diagnostics.Stopwatch]$CaseStopwatch
) {
    $sentAt = (Get-Date).ToUniversalTime()
    $stopwatch = [Diagnostics.Stopwatch]::StartNew()
    Send-ServerCommand $State.Process $Command
    $line = Wait-ServerSignal $State $ExpectedFragments $Stage $CaseStopwatch
    return [pscustomobject]@{
        SentAt = $sentAt
        AckAt = (Get-Date).ToUniversalTime()
        ElapsedSeconds = [Math]::Round($stopwatch.Elapsed.TotalSeconds, 3)
        Line = $line
    }
}

function Start-ServerSession(
    [string]$CaseId,
    [int]$SessionOrdinal,
    [string]$ResultPath,
    [string]$GradleWrapper,
    [string]$RepoRoot,
    [string]$RunRoot,
    [string]$ServerMode,
    [string]$ForgeVersion,
    [IO.StreamWriter]$AggregateWriter,
    [IO.StreamWriter]$EventWriter
) {
    $consolePath = Join-Path $ResultPath ("console-session-{0:D4}.log" -f $SessionOrdinal)
    $info = [Diagnostics.ProcessStartInfo]::new()
    $info.FileName = "$env:ComSpec"
    if ($ServerMode -eq "production") {
        $forgeArgs = "libraries\net\minecraftforge\forge\1.20.1-$ForgeVersion\win_args.txt"
        $info.Arguments = "/d /s /c `"`"$JavaHome\bin\java.exe`" @user_jvm_args.txt @`"$forgeArgs`" nogui 2>&1`""
        $info.WorkingDirectory = $RunRoot
    } else {
        $gradleJavaPathOption = "`"-Porg.gradle.java.installations.paths=$JavaHome`""
        $info.Arguments = "/d /s /c `"`"$GradleWrapper`" --no-daemon $gradleJavaPathOption -Porg.gradle.java.installations.auto-detect=false -Porg.gradle.java.installations.auto-download=false runForge1201Server --console=plain 2>&1`""
        $info.WorkingDirectory = $RepoRoot
    }
    $info.UseShellExecute = $false
    $info.RedirectStandardInput = $true
    $info.RedirectStandardOutput = $true
    $info.CreateNoWindow = $true
    $info.Environment["JAVA_HOME"] = $JavaHome
    $info.Environment["PATH"] = "$JavaHome\bin;" + $info.Environment["PATH"]
    $info.Environment["TERM"] = "dumb"

    $maxThreadsPattern = '(?i)-Dmax\.bg\.threads(?:=[^\s"'']+)?'
    $optionSources = [ordered]@{
        JAVA_TOOL_OPTIONS = [string]$info.Environment["JAVA_TOOL_OPTIONS"]
        JDK_JAVA_OPTIONS = [string]$info.Environment["JDK_JAVA_OPTIONS"]
        _JAVA_OPTIONS = [string]$info.Environment["_JAVA_OPTIONS"]
        JAVA_OPTS = [string]$info.Environment["JAVA_OPTS"]
        GRADLE_OPTS = [string]$info.Environment["GRADLE_OPTS"]
    }
    if ($ServerMode -eq "production") {
        foreach ($argumentFile in @(
            (Join-Path $RunRoot "user_jvm_args.txt"),
            (Join-Path $RunRoot $forgeArgs)
        )) {
            $activeArguments = (Get-Content -LiteralPath $argumentFile | Where-Object {
                $_ -notmatch '^\s*#'
            }) -join " "
            $optionSources[$argumentFile] = $activeArguments
        }
    }
    foreach ($source in $optionSources.GetEnumerator()) {
        if ($source.Value -match $maxThreadsPattern) {
            throw "$($source.Key) 已设置 max.bg.threads；压力脚本必须独占该参数"
        }
    }

    $threadOption = "-Dmax.bg.threads=$MaxBackgroundThreads"
    $toolOptions = [string]$info.Environment["JAVA_TOOL_OPTIONS"]
    $toolOptions = ($toolOptions.Trim() + " " + $threadOption).Trim()
    $info.Environment["JAVA_TOOL_OPTIONS"] = $toolOptions

    $consoleWriter = [IO.StreamWriter]::new($consolePath, $false, [Text.UTF8Encoding]::new($false))
    $process = [Diagnostics.Process]::new()
    $process.StartInfo = $info
    try {
        if (-not $process.Start()) {
            throw "无法启动服务端 session $SessionOrdinal"
        }
    } catch {
        $consoleWriter.Dispose()
        $process.Dispose()
        throw
    }
    $script:activeProcess = $process

    try {
        $state = [pscustomobject]@{
            CaseId = $CaseId
            Ordinal = $SessionOrdinal
            Process = $process
            ReadTask = $process.StandardOutput.ReadLineAsync()
            ConsoleWriter = $consoleWriter
            ConsolePath = $consolePath
            AggregateWriter = $AggregateWriter
            StartedAt = (Get-Date).ToUniversalTime()
            LastLine = ""
            FailureLine = ""
            JavaToolOptions = $toolOptions
        }
        $AggregateWriter.WriteLine("=== session $SessionOrdinal pid=$($process.Id) ===")
        $AggregateWriter.Flush()
        Write-HarnessEvent $EventWriter "session_started" ([ordered]@{
            session = $SessionOrdinal
            pid = $process.Id
            server_mode = $ServerMode
            forge_version = $ForgeVersion
            java_tool_options = $toolOptions
        })
        Write-Host "[$CaseId/session-$SessionOrdinal] PID=$($process.Id)，等待服务端就绪"
        return $state
    } catch {
        Stop-ProcessTree $process
        $consoleWriter.Dispose()
        $process.Dispose()
        $script:activeProcess = $null
        throw
    }
}

function Stop-ServerSession(
    [object]$State,
    [string]$RunRoot,
    [string]$ResultPath,
    [IO.StreamWriter]$EventWriter,
    [Diagnostics.Stopwatch]$CaseStopwatch
) {
    $stopwatch = [Diagnostics.Stopwatch]::StartNew()
    try {
        if (-not $State.Process.HasExited) {
            try {
                Send-ServerCommand $State.Process "stop"
            } catch {
                # The process may have exited between HasExited and the write.
            }
        }

        while ($true) {
            Assert-WaitBudget $stopwatch $CaseStopwatch "waiting-session-exit"
            if ($State.ReadTask.Wait(250)) {
                $line = $State.ReadTask.Result
                if ($null -eq $line) {
                    break
                }
                Write-ServerLine $State $line
                $State.ReadTask = $State.Process.StandardOutput.ReadLineAsync()
            }
        }
        $State.Process.WaitForExit()
        $exitCode = $State.Process.ExitCode

        $latestLog = Join-Path $RunRoot "logs\latest.log"
        if (Test-Path -LiteralPath $latestLog -PathType Leaf) {
            Copy-Item -LiteralPath $latestLog -Destination (
                Join-Path $ResultPath ("latest-session-{0:D4}.log" -f $State.Ordinal)
            ) -Force
        }
        Write-HarnessEvent $EventWriter "session_stopped" ([ordered]@{
            session = $State.Ordinal
            exit_code = $exitCode
            elapsed_seconds = [Math]::Round(((Get-Date).ToUniversalTime() - $State.StartedAt).TotalSeconds, 3)
        })
        return $exitCode
    } catch {
        Stop-ProcessTree $State.Process
        throw
    } finally {
        $State.ConsoleWriter.Dispose()
        $State.Process.StandardInput.Dispose()
        $State.Process.StandardOutput.Dispose()
        $State.Process.Dispose()
        $script:activeProcess = $null
    }
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
    [string]$ServerMode,
    [string]$ForgeVersion,
    [string]$JavaVersionOutput,
    [long]$Seed,
    [int]$MinY,
    [string]$AppliedConfig,
    [string]$AppliedProperties
) {
    New-Item -ItemType Directory -Path $ResultPath | Out-Null
    Write-Utf8NoBom (Join-Path $ResultPath "requested-tectonic.json") $AppliedConfig
    Write-Utf8NoBom (Join-Path $ResultPath "requested-server.properties") $AppliedProperties

    $tilePlan = @(Get-TilePlan $ChunkMinX $ChunkMinZ $ChunkMaxX $ChunkMaxZ $TileWidthChunks $TileHeightChunks)
    $aggregateWriter = [IO.StreamWriter]::new(
        (Join-Path $ResultPath "console.log"),
        $false,
        [Text.UTF8Encoding]::new($false)
    )
    $eventWriter = [IO.StreamWriter]::new(
        (Join-Path $ResultPath "events.ndjson"),
        $false,
        [Text.UTF8Encoding]::new($false)
    )
    $startedAt = (Get-Date).ToUniversalTime()
    $caseStopwatch = [Diagnostics.Stopwatch]::StartNew()
    $session = $null
    $sessionOrdinal = 0
    $completedTiles = 0
    $completedChunks = 0
    $completed = $false
    $sessionRecords = [Collections.Generic.List[object]]::new()
    $javaToolOptions = ""

    try {
        foreach ($tile in $tilePlan) {
            if ($null -eq $session) {
                $sessionOrdinal++
                $session = Start-ServerSession `
                    $CaseId `
                    $sessionOrdinal `
                    $ResultPath `
                    $GradleWrapper `
                    $RepoRoot `
                    $RunRoot `
                    $ServerMode `
                    $ForgeVersion `
                    $aggregateWriter `
                    $eventWriter
                $javaToolOptions = $session.JavaToolOptions
                $ready = Wait-ServerSignal $session @("Done (") "waiting-ready" $caseStopwatch
                Write-HarnessEvent $eventWriter "session_ready" ([ordered]@{
                    session = $session.Ordinal
                    line = $ready
                })
            }

            foreach ($chunk in $tile.Chunks) {
                $blockX = [long]$chunk.X * 16
                $blockZ = [long]$chunk.Z * 16
                $expected = "Marked chunk [$($chunk.X), $($chunk.Z)] in minecraft:overworld to be force loaded"
                $result = Invoke-ServerCommandAndWait `
                    $session `
                    "execute in minecraft:overworld run forceload add $blockX $blockZ" `
                    @($expected) `
                    "waiting-chunk-$($chunk.Ordinal)-full" `
                    $caseStopwatch
                $completedChunks++
                Write-HarnessEvent $eventWriter "chunk_full" ([ordered]@{
                    session = $session.Ordinal
                    tile = $tile.Ordinal
                    ordinal = $chunk.Ordinal
                    chunk_x = $chunk.X
                    chunk_z = $chunk.Z
                    command_sent_utc = $result.SentAt.ToString("o")
                    ack_utc = $result.AckAt.ToString("o")
                    elapsed_seconds = $result.ElapsedSeconds
                    state = "full"
                })
            }

            $blockMinX = [long]$tile.MinX * 16
            $blockMinZ = [long]$tile.MinZ * 16
            $blockMaxX = ([long]$tile.MaxX + 1) * 16 - 1
            $blockMaxZ = ([long]$tile.MaxZ + 1) * 16 - 1
            if ($tile.Chunks.Count -eq 1) {
                $removeExpected = "Unmarked chunk [$($tile.MinX), $($tile.MinZ)] in minecraft:overworld for force loading"
                $removeExpectedFragments = @($removeExpected)
            } else {
                $removeExpected = "from [$($tile.MinX), $($tile.MinZ)] to [$($tile.MaxX), $($tile.MaxZ)] for force loading"
                $removeExpectedFragments = @("Unmarked ", $removeExpected)
            }
            $removeResult = Invoke-ServerCommandAndWait `
                $session `
                "execute in minecraft:overworld run forceload remove $blockMinX $blockMinZ $blockMaxX $blockMaxZ" `
                $removeExpectedFragments `
                "waiting-tile-$($tile.Ordinal)-remove" `
                $caseStopwatch
            $completedTiles++
            Write-HarnessEvent $eventWriter "tile_removed" ([ordered]@{
                session = $session.Ordinal
                tile = $tile.Ordinal
                min_x = $tile.MinX
                min_z = $tile.MinZ
                max_x = $tile.MaxX
                max_z = $tile.MaxZ
                chunk_count = $tile.Chunks.Count
                command_sent_utc = $removeResult.SentAt.ToString("o")
                ack_utc = $removeResult.AckAt.ToString("o")
                elapsed_seconds = $removeResult.ElapsedSeconds
            })

            $isLastTile = $tile.Ordinal -eq $tilePlan.Count
            $restartDue = -not $isLastTile -and
                $RestartEveryTiles -gt 0 -and
                $completedTiles % $RestartEveryTiles -eq 0
            $saveDue = $isLastTile -or $restartDue -or (
                $SaveEveryTiles -gt 0 -and $completedTiles % $SaveEveryTiles -eq 0
            )
            if ($saveDue) {
                $saveResult = Invoke-ServerCommandAndWait `
                    $session `
                    "save-all flush" `
                    @("Saved the game") `
                    "waiting-save-after-tile-$($tile.Ordinal)" `
                    $caseStopwatch
                Write-HarnessEvent $eventWriter "world_saved" ([ordered]@{
                    session = $session.Ordinal
                    after_tile = $tile.Ordinal
                    command_sent_utc = $saveResult.SentAt.ToString("o")
                    ack_utc = $saveResult.AckAt.ToString("o")
                    elapsed_seconds = $saveResult.ElapsedSeconds
                })
            }

            if ($restartDue -or $isLastTile) {
                $closedSession = $session
                $exitCode = Stop-ServerSession $closedSession $RunRoot $ResultPath $eventWriter $caseStopwatch
                $session = $null
                $sessionRecords.Add([pscustomobject]@{
                    ordinal = $closedSession.Ordinal
                    exit_code = $exitCode
                    started_at_utc = $closedSession.StartedAt.ToString("o")
                    finished_at_utc = (Get-Date).ToUniversalTime().ToString("o")
                })
                if ($closedSession.FailureLine) {
                    throw "session $($closedSession.Ordinal) 在停服阶段报告失败: $($closedSession.FailureLine)"
                }
                if ($exitCode -ne 0) {
                    throw "服务端 session $($closedSession.Ordinal) 退出码为 $exitCode"
                }
            }
        }
        $completed = $true
    } catch {
        Write-HarnessEvent $eventWriter "case_failed" ([ordered]@{
            session = if ($null -eq $session) { $sessionOrdinal } else { $session.Ordinal }
            completed_tiles = $completedTiles
            completed_chunks = $completedChunks
            message = $_.Exception.Message
            last_line = if ($null -eq $session) { "" } else { $session.LastLine }
        })
        throw
    } finally {
        if ($null -ne $session) {
            try {
                $closedSession = $session
                $exitCode = Stop-ServerSession $closedSession $RunRoot $ResultPath $eventWriter $caseStopwatch
                $sessionRecords.Add([pscustomobject]@{
                    ordinal = $closedSession.Ordinal
                    exit_code = $exitCode
                    started_at_utc = $closedSession.StartedAt.ToString("o")
                    finished_at_utc = (Get-Date).ToUniversalTime().ToString("o")
                })
            } catch {
                Stop-ProcessTree $session.Process
                $session.ConsoleWriter.Dispose()
                $script:activeProcess = $null
            }
        }
        $aggregateWriter.Dispose()
        $eventWriter.Dispose()
    }

    if (-not $completed) {
        throw "case 未完成: $CaseId"
    }
    if (-not (Test-Path -LiteralPath $WorldPath -PathType Container)) {
        throw "服务端未创建世界目录: $WorldPath"
    }

    Copy-Item -LiteralPath (Join-Path $RunRoot "config\tectonic.json") -Destination (Join-Path $ResultPath "effective-tectonic.json")
    Copy-Item -LiteralPath (Join-Path $RunRoot "server.properties") -Destination (Join-Path $ResultPath "effective-server.properties")

    $gitCommit = (& git -C $RepoRoot rev-parse HEAD).Trim()
    if ($LASTEXITCODE -ne 0) {
        throw "无法读取 Git HEAD"
    }
    $gitStatus = @(& git -C $RepoRoot status --porcelain=v1 --untracked-files=no)
    if ($LASTEXITCODE -ne 0) {
        throw "无法读取 Git 工作区状态"
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
        server_mode = $ServerMode
        forge_version = $ForgeVersion
        chunks_inclusive = @($ChunkMinX, $ChunkMinZ, $ChunkMaxX, $ChunkMaxZ)
        chunk_count = [long]($ChunkMaxX - $ChunkMinX + 1) * [long]($ChunkMaxZ - $ChunkMinZ + 1)
        chunk_order = "tile-z-x_then_chunk-z-x"
        tile_size = @($TileWidthChunks, $TileHeightChunks)
        tile_count = $tilePlan.Count
        save_every_tiles = $SaveEveryTiles
        restart_every_tiles = $RestartEveryTiles
        stage_timeout_minutes = $StageTimeoutMinutes
        case_timeout_minutes = $CaseTimeoutMinutes
        max_background_threads = $MaxBackgroundThreads
        java_tool_options = $javaToolOptions
        sessions = @($sessionRecords)
        git_commit = $gitCommit
        git_tracked_changes = $gitStatus
        server_mods = @($serverArtifacts)
        java_home = $JavaHome
        java_version = $JavaVersionOutput
        started_at_utc = $startedAt.ToString("o")
        finished_at_utc = (Get-Date).ToUniversalTime().ToString("o")
        elapsed_seconds = [Math]::Round($caseStopwatch.Elapsed.TotalSeconds, 3)
        server_exit_code = $sessionRecords[$sessionRecords.Count - 1].exit_code
        ore_analysis = [ordered]@{
            enabled = -not $SkipOreAnalysis
            status = if ($SkipOreAnalysis) { "skipped" } else { "pending" }
        }
    }
    Write-Utf8NoBom (Join-Path $ResultPath "manifest.json") (($manifest | ConvertTo-Json -Depth 8) + "`n")

    if (-not $SkipOreAnalysis) {
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
        $manifest["ore_analysis"]["status"] = "passed"
        Write-Utf8NoBom (Join-Path $ResultPath "manifest.json") (($manifest | ConvertTo-Json -Depth 8) + "`n")
    }
}


$repoRoot = (Resolve-Path -LiteralPath $RepositoryRoot).Path
if ($ProductionServerRoot) {
    if ($ProductionForgeVersion -notmatch '^\d+\.\d+\.\d+$') {
        throw "生产专服模式需要形如 47.4.22 的 ProductionForgeVersion"
    }
    $runRoot = Resolve-FullPath $ProductionServerRoot $repoRoot
    $serverMode = "production"
} else {
    if ($ProductionForgeVersion) {
        throw "ProductionForgeVersion 只能与 ProductionServerRoot 一起使用"
    }
    $runRoot = Join-Path $repoRoot "run"
    $serverMode = "gradle-userdev"
}
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

if ($serverMode -eq "gradle-userdev" -and -not (Test-Path -LiteralPath $gradleWrapper -PathType Leaf)) {
    throw "找不到 Gradle wrapper: $gradleWrapper"
}
if ($serverMode -eq "production") {
    $forgeArgsPath = Join-Path $runRoot "libraries\net\minecraftforge\forge\1.20.1-$ProductionForgeVersion\win_args.txt"
    if (-not (Test-Path -LiteralPath $forgeArgsPath -PathType Leaf)) {
        throw "找不到生产专服参数文件: $forgeArgsPath"
    }
    if (-not (Test-Path -LiteralPath (Join-Path $runRoot "user_jvm_args.txt") -PathType Leaf)) {
        throw "生产专服缺少 user_jvm_args.txt: $runRoot"
    }
}
$serverArtifacts = @()
if ($serverMode -eq "production") {
    $modsPath = Join-Path $runRoot "mods"
    if (-not (Test-Path -LiteralPath $modsPath -PathType Container)) {
        throw "生产专服缺少 mods 目录: $modsPath"
    }
    $modFiles = @(Get-ChildItem -LiteralPath $modsPath -Filter "*.jar" -File | Sort-Object Name)
    $tectonicFiles = @($modFiles | Where-Object { $_.Name -match '^tectonic(?:[-_].*)?\.jar$' })
    if ($tectonicFiles.Count -ne 1) {
        throw "生产专服必须恰好包含一个以 tectonic 命名的 JAR，实际为 $($tectonicFiles.Count): $modsPath"
    }
    $serverArtifacts = @($modFiles | ForEach-Object {
        [ordered]@{
            file = $_.Name
            path = $_.FullName
            size_bytes = $_.Length
            sha256 = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
        }
    })
}
if (-not $SkipOreAnalysis -and -not (Test-Path -LiteralPath $analyzerPath -PathType Leaf)) {
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
$tileArea = [long]$TileWidthChunks * [long]$TileHeightChunks
if ($tileArea -gt 256) {
    throw "单个 tile 超过 forceload 的 256 区块限制: $TileWidthChunks x $TileHeightChunks = $tileArea"
}
$chunkWidth = [long]$ChunkMaxX - [long]$ChunkMinX + 1
$chunkHeight = [long]$ChunkMaxZ - [long]$ChunkMinZ + 1
$chunkCount = $chunkWidth * $chunkHeight
$validatedTilePlan = @(Get-TilePlan $ChunkMinX $ChunkMinZ $ChunkMaxX $ChunkMaxZ $TileWidthChunks $TileHeightChunks)
$tileCount = $validatedTilePlan.Count
$validatedTilePlan = $null
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
if ($SkipOreAnalysis -and $IncludeMaterials) {
    throw "SkipOreAnalysis 与 IncludeMaterials 不能同时使用"
}

$baseConfigText = Get-Content -LiteralPath $configPath -Raw
$basePropertiesText = Get-Content -LiteralPath $serverPropertiesPath -Raw
$port = [int](Get-ServerProperty $basePropertiesText "server-port")

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

Write-Host "批处理: scenario=$Scenario, mode=$serverMode, cases=$($cases.Count), chunks/case=$chunkCount, tiles/case=$tileCount, runId=$RunId"
if ($DryRun) {
    $cases | Select-Object CaseId, Seed, MinY, WorldPath, ResultPath | Format-Table -AutoSize
    return
}
Assert-PortAvailable $port

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
            $serverMode `
            $ProductionForgeVersion `
            $javaVersionOutput `
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
        server_mode = $serverMode
        forge_version = $ProductionForgeVersion
        chunks_inclusive = @($ChunkMinX, $ChunkMinZ, $ChunkMaxX, $ChunkMaxZ)
        chunks_per_case = $chunkCount
        chunk_order = "tile-z-x_then_chunk-z-x"
        tile_size = @($TileWidthChunks, $TileHeightChunks)
        tiles_per_case = $tileCount
        save_every_tiles = $SaveEveryTiles
        restart_every_tiles = $RestartEveryTiles
        stage_timeout_minutes = $StageTimeoutMinutes
        case_timeout_minutes = $CaseTimeoutMinutes
        max_background_threads = $MaxBackgroundThreads
        ore_analysis_enabled = -not $SkipOreAnalysis
        server_mods = @($serverArtifacts)
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
