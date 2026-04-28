param(
    [ValidateSet("all", "re", "rwa")]
    [string]$Suite = "all",

    [int]$Runs = 30,

    [int]$Cores = -1,

    [string]$OutputDir = "experiments/rq4_validation/results/representative-configs",

    [switch]$CompileProject,

    [switch]$RunAblations,

    [ValidateSet("forward", "knockout")]
    [string]$AblationMode = "forward",

    [int]$AblationNrep = 5,

    [switch]$CompilePaper,

    [switch]$DryRun
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$RuntimeClasspathFile = Join-Path $RepoRoot "target/runtime-classpath.txt"
$ValidationMain = "org.uma.evolver.example.validation.RepresentativeConfigurationValidationStudy"
$JavaExe = "java"
$ClasspathSeparator = [IO.Path]::PathSeparator

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "==> $Message"
}

function Resolve-MavenCommand {
    $wrapper = Join-Path $RepoRoot "mvnw.cmd"
    if (Test-Path $wrapper) {
        return $wrapper
    }

    foreach ($name in @("mvn.cmd", "mvn")) {
        $command = Get-Command $name -ErrorAction SilentlyContinue
        if ($null -ne $command) {
            return $command.Source
        }
    }

    $candidates = @(
        "$env:USERPROFILE\Desktop\mcMFLP_jMetal\tools\apache-maven-*\bin\mvn.cmd",
        "$env:ProgramFiles\JetBrains\IntelliJ IDEA*\plugins\maven\lib\maven3\bin\mvn.cmd"
    )

    foreach ($pattern in $candidates) {
        $match = Get-ChildItem -Path $pattern -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($null -ne $match) {
            return $match.FullName
        }
    }

    throw "No se ha encontrado Maven. Usa -CompileProject con mvn/mvnw disponible o compila el proyecto antes."
}

function Resolve-PythonCommand {
    foreach ($name in @("python", "python3")) {
        $command = Get-Command $name -ErrorAction SilentlyContinue
        if ($null -ne $command) {
            return $command.Source
        }
    }

    throw "No se ha encontrado Python. Instala python3 o expone python/python3 en PATH."
}

function Invoke-LoggedCommand {
    param(
        [string]$FilePath,
        [string[]]$Arguments,
        [string]$WorkingDirectory = $RepoRoot
    )

    $rendered = @($FilePath) + $Arguments
    Write-Host ($rendered -join " ")
    if (-not $DryRun) {
        Push-Location $WorkingDirectory
        try {
            & $FilePath @Arguments
        }
        finally {
            Pop-Location
        }
    }
}

function Ensure-BuildArtifacts {
    $targetClasses = Join-Path $RepoRoot "target\classes"
    if ($CompileProject -or -not (Test-Path $targetClasses) -or -not (Test-Path $RuntimeClasspathFile)) {
        $maven = Resolve-MavenCommand

        Write-Step "Compilando proyecto Java"
        Invoke-LoggedCommand -FilePath $maven -Arguments @("-DskipTests", "compile")

        Write-Step "Generando runtime classpath"
        Invoke-LoggedCommand -FilePath $maven -Arguments @(
            "org.apache.maven.plugins:maven-dependency-plugin:3.8.1:build-classpath",
            "-Dmdep.outputFile=target/runtime-classpath.txt"
        )
    }
}

function Get-RuntimeClasspath {
    if (-not (Test-Path $RuntimeClasspathFile)) {
        throw "Falta target\runtime-classpath.txt. Ejecuta primero el paso de compilación."
    }

    $dependencyClasspath = (Get-Content $RuntimeClasspathFile -Raw).Trim()
    return "target/classes$ClasspathSeparator$dependencyClasspath"
}

function Invoke-ValidationStudy {
    param(
        [string]$SuiteName,
        [string]$AblationBaseTag = "",
        [string]$AblationModeOverride = ""
    )

    $cp = Get-RuntimeClasspath
    $arguments = @(
        "-cp",
        $cp,
        $ValidationMain,
        "--suite",
        $SuiteName,
        "--output-dir",
        $OutputDir,
        "--cores",
        $Cores.ToString(),
        "--runs",
        $Runs.ToString(),
        "--run-algorithms"
    )

    if ($AblationBaseTag -ne "") {
        $arguments += @("--ablation-base", $AblationBaseTag)
        $modeToUse = if ($AblationModeOverride -ne "") { $AblationModeOverride } else { $AblationMode }
        $arguments += @("--ablation-mode", $modeToUse)
        if ($modeToUse -eq "forward") {
            $arguments += @("--ablation-nrep", $AblationNrep.ToString())
        }
    }

    Invoke-LoggedCommand -FilePath $JavaExe -Arguments $arguments
}

function Invoke-Rq4Postprocess {
    $python = if ($DryRun) { "python" } else { Resolve-PythonCommand }

    Write-Step "Regenerando resumen RQ4"
    Invoke-LoggedCommand -FilePath $python -Arguments @("experiments/rq4_validation/generate_validation_summary.py")

    Write-Step "Regenerando figura de asimetria de transferencia RQ4"
    Invoke-LoggedCommand -FilePath $python -Arguments @("experiments/rq4_validation/generate_validation_transfer_delta.py")

    if ($RunAblations) {
        Write-Step "Regenerando ablaciones RQ4"
        Invoke-LoggedCommand -FilePath $python -Arguments @("experiments/rq4_validation/generate_validation_ablation.py", "--mode", $AblationMode)
    }

    if ($CompilePaper) {
        Write-Step "Compilando manuscrito"
        Invoke-LoggedCommand -FilePath $python -Arguments @("paper/scripts/08_compile_manuscript_pdf.py", "--no-sync")
    }
}

Ensure-BuildArtifacts

switch ($Suite) {
    "all" {
        Write-Step "Ejecutando validacion compacta en RE y RWA"
        Invoke-ValidationStudy -SuiteName "re"
        Invoke-ValidationStudy -SuiteName "rwa"
    }
    "re" {
        Write-Step "Ejecutando validacion compacta en RE"
        Invoke-ValidationStudy -SuiteName "re"
    }
    "rwa" {
        Write-Step "Ejecutando validacion compacta en RWA"
        Invoke-ValidationStudy -SuiteName "rwa"
    }
}

if ($RunAblations) {
    Write-Step "Ejecutando ablaciones compactas"

    if ($Suite -in @("all", "re")) {
        Invoke-ValidationStudy -SuiteName "re" -AblationBaseTag "Complete-RE3D"
        Invoke-ValidationStudy -SuiteName "re" -AblationBaseTag "Extreme-RE3D"
    }

    if ($Suite -in @("all", "rwa")) {
        Invoke-ValidationStudy -SuiteName "rwa" -AblationBaseTag "Complete-RWA3D"
        Invoke-ValidationStudy -SuiteName "rwa" -AblationBaseTag "Extreme-RWA3D"
    }
}

Invoke-Rq4Postprocess
