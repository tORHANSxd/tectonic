param(
    [string]$Revision = "upstream-3.0.17-forge-1.20.1",
    [string]$Output = "UPSTREAM_BASELINE_FILES.txt"
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$outputPath = [IO.Path]::GetFullPath((Join-Path $repoRoot $Output))
$tempBase = [IO.Path]::GetFullPath([IO.Path]::GetTempPath())
$tempRoot = Join-Path $tempBase ("tectonic-baseline-" + [Guid]::NewGuid().ToString("N"))
$archivePath = Join-Path $tempRoot "baseline.zip"
$snapshotPath = Join-Path $tempRoot "snapshot"

function Get-SourceSet([string]$path) {
    switch -Regex ($path) {
        '^src/common/main/' { return "common:main" }
        '^src/common/test/' { return "common:test" }
        '^src/shared/([^/]+)/main/' { return "shared:$($Matches[1]):main" }
        '^src/shared/([^/]+)/test/' { return "shared:$($Matches[1]):test" }
        '^src/(fabric|forge|neoforge)/([^/]+)/main/' { return "$($Matches[1]):$($Matches[2]):main" }
        '^src/(fabric|forge|neoforge)/([^/]+)/test/' { return "$($Matches[1]):$($Matches[2]):test" }
        '^\.github/' { return "ci" }
        '^gradle/' { return "build" }
        '^(build|settings)\.gradle\.kts$' { return "build" }
        '^gradle\.properties$' { return "build" }
        default { return "repository" }
    }
}

function Get-JarInclusion([string]$path) {
    if ($path -match '^src/(common/main|shared/1\.20\.1/main|forge/1\.20\.1/main)/java/.+\.java$') {
        return @("YES", "compiled class")
    }

    if ($path -match '^src/(common/main|shared/1\.20\.1/main|forge/1\.20\.1/main)/resources/') {
        return @("YES", "resource")
    }

    if ($path -in @(
        "src/common/main/tectonic.mixins.json",
        "src/shared/1.20.1/main/tectonic_1.20.1.mixins.json"
    )) {
        return @("YES", "generated resource input")
    }

    return @("NO", "-")
}

New-Item -ItemType Directory -Path $snapshotPath -Force | Out-Null

try {
    & git -C $repoRoot archive --format=zip --output=$archivePath $Revision
    if ($LASTEXITCODE -ne 0) {
        throw "git archive failed for revision '$Revision'"
    }

    $revisionTimestamp = (& git -C $repoRoot show -s --format=%cI $Revision).Trim()
    if ($LASTEXITCODE -ne 0) {
        throw "git show failed for revision '$Revision'"
    }

    Expand-Archive -LiteralPath $archivePath -DestinationPath $snapshotPath

    $lines = [Collections.Generic.List[string]]::new()
    $lines.Add("# Tectonic upstream baseline file manifest")
    $lines.Add("# revision`t$Revision")
    $lines.Add("# revision_timestamp`t$revisionTimestamp")
    $lines.Add("path`tsize_bytes`tsha256`tsource_set`tin_final_forge_1_20_1_jar`tinclusion_mode")

    Get-ChildItem -LiteralPath $snapshotPath -Recurse -File |
        Sort-Object FullName |
        ForEach-Object {
            $relativePath = [IO.Path]::GetRelativePath($snapshotPath, $_.FullName).Replace('\', '/')
            $sourceSet = Get-SourceSet $relativePath
            $inclusion = Get-JarInclusion $relativePath
            $sha256 = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
            $lines.Add("$relativePath`t$($_.Length)`t$sha256`t$sourceSet`t$($inclusion[0])`t$($inclusion[1])")
        }

    [IO.File]::WriteAllLines($outputPath, $lines, [Text.UTF8Encoding]::new($false))
} finally {
    $resolvedTempRoot = [IO.Path]::GetFullPath($tempRoot)
    if ($resolvedTempRoot.StartsWith($tempBase, [StringComparison]::OrdinalIgnoreCase) -and
        [IO.Path]::GetFileName($resolvedTempRoot).StartsWith("tectonic-baseline-", [StringComparison]::Ordinal)) {
        Remove-Item -LiteralPath $resolvedTempRoot -Recurse -Force -ErrorAction SilentlyContinue
    }
}
