# test-without-jms.ps1
# ------------------------------------------------------------------
# Runs Maven tests while the still-unwritten step 2.2/2.3 messaging
# classes are temporarily parked aside, then ALWAYS restores them.
#
# WHY THIS EXISTS
#   SwipeMessage.java / SwipeProducer.java / SwipeConsumer.java reference
#   model.SwipeAction, dao.SwipeDao and dao.MatchDao, which are planned
#   for steps 2.2 and 2.3 but not written yet. Until then they break
#   `mvn compile`, so no test can run. This script works around that
#   WITHOUT changing pom.xml or deleting anyone's code: it moves those
#   3 files to *.bak, runs the build, and restores them in a finally,
#   which is guaranteed to run even if tests fail or you press Ctrl+C.
#
# WHAT YOU DO
#   From anywhere, run:
#       .\scripts\test-without-jms.ps1                       # all tests
#       .\scripts\test-without-jms.ps1 -Test Step14SecurityTest   # one class
#       .\scripts\test-without-jms.ps1 -Goals clean,test      # custom goals
#
# After it finishes, `git status` shows NO residual changes from this
# script (only whatever you actually edited). If something ever leaves
# .bak files behind (power loss, killed shell), rerun with -RestoreOnly
# to put them back, or run `git status` and move any *.bak back by hand.
# ------------------------------------------------------------------

[CmdletBinding()]
param(
    [string]$Test = "",            # e.g. "Step14SecurityTest" or "Step14*"
    [string]$Goals = "test",       # comma list, e.g. "clean,test"
    [switch]$RestoreOnly           # just restore any leftovers and exit
)

$ErrorActionPreference = "Stop"

# Repo root = parent of the scripts/ folder this file lives in.
$repoRoot   = Split-Path -Parent $PSScriptRoot
$backendDir = Join-Path $repoRoot "backend\zuzdog-backend"
$msgDir     = Join-Path $backendDir "src\main\java\com\zuzdog\messaging"

# The 3 messaging files that forward-reference not-yet-written step 2.2/2.3
# classes. Keep this list in sync with the project plan.
$blockingFiles = @("SwipeMessage.java", "SwipeProducer.java", "SwipeConsumer.java")

# Maps from "$movedAsidePath" -> "$originalPath" so the finally block can
# restore the exact pair even if we only moved some of them.
$restoration = @{}

function Move-Aside {
    foreach ($name in $blockingFiles) {
        $orig = Join-Path $msgDir $name
        $bak  = "$orig.bak"
        if (Test-Path -LiteralPath $orig) {
            Move-Item -LiteralPath $orig -Destination $bak -Force
            $restoration[$bak] = $orig
            Write-Host "  parked: $name"
        } elseif (Test-Path -LiteralPath $bak) {
            # Already parked (leftover from a crashed run). Track it so the
            # finally still restores it.
            $restoration[$bak] = $orig
            Write-Host "  already parked: $name"
        }
    }
}

function Restore-All {
    foreach ($kv in $restoration.GetEnumerator()) {
        $bak  = $kv.Key
        $orig = $kv.Value
        if (Test-Path -LiteralPath $bak) {
            Move-Item -LiteralPath $bak -Destination $orig -Force
            Write-Host "  restored: $(Split-Path -Leaf $orig)"
        }
    }
    $restoration.Clear()
}

if ($RestoreOnly) {
    Write-Host "RestoreOnly: putting back any parked messaging files..."
    # Rebuild the map from whatever *.bak exists now.
    foreach ($name in $blockingFiles) {
        $orig = Join-Path $msgDir $name
        $bak  = "$orig.bak"
        if (Test-Path -LiteralPath $bak) { $restoration[$bak] = $orig }
    }
    Restore-All
    exit 0
}

if (-not (Test-Path -LiteralPath $backendDir)) {
    Write-Error "Backend dir not found: $backendDir"
    exit 1
}

# Trap ensures restoration even on Ctrl+C (PowerShell runs trap on
# terminating errors and on interruptions of an interactive session).
trap {
    Write-Host ""
    Write-Host "Interrupted/error: restoring parked files..." -ForegroundColor Yellow
    Restore-All
    break
}

try {
    Write-Host "==> Parking unfinished messaging files aside" -ForegroundColor Cyan
    Move-Aside

    Write-Host ""
    Write-Host "==> Running Maven" -ForegroundColor Cyan
    Push-Location -LiteralPath $backendDir
    try {
        $mvw = ".\mvnw.cmd"
        if (-not (Test-Path -LiteralPath $mvw)) {
            Write-Error "Maven wrapper not found at $mvw"
            exit 1
        }

        $args = @()
        foreach ($g in ($Goals -split ',')) { $args += $g.Trim() }
        if ($Test -ne "") {
            $args += "-Dtest=$Test"
            $args += "-DfailIfNoTests=false"
        }
        # -q keeps it quiet; -e prints clean error stacks. Let's not use -q
        # so test pass/fail summaries are visible.
        & $mvw @args
        $code = $LASTEXITCODE
        Write-Host ""
        if ($code -eq 0) {
            Write-Host "==> Maven finished OK (exit $code)" -ForegroundColor Green
        } else {
            Write-Host "==> Maven failed (exit $code)" -ForegroundColor Yellow
        }
        $scriptExit = $code
    }
    finally {
        Pop-Location
    }
}
finally {
    Write-Host ""
    Write-Host "==> Restoring parked messaging files" -ForegroundColor Cyan
    Restore-All
}

exit $scriptExit