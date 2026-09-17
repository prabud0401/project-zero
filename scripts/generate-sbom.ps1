<#
.SYNOPSIS
Generates an SBOM (Software Bill of Materials) for the Project Zero Android client.

.DESCRIPTION
This script runs the Gradle dependencies task and saves the output as a rudimentary SBOM, 
or invokes a dedicated SBOM plugin if configured (e.g., spdx-gradle-plugin or cyclonedx-gradle-plugin).
For now, we output the dependencies tree to a text file for auditing.

.EXAMPLE
.\generate-sbom.ps1
#>

Set-StrictMode -Version 2.0
$ErrorActionPreference = "Stop"

$ProjectRoot = Resolve-Path "$PSScriptRoot\.."
$AppDir = "$ProjectRoot\android-client"
$OutFile = "$ProjectRoot\docs\sbom-output.txt"

Write-Host "Generating SBOM for android-client..."
Set-Location $AppDir

# Using Gradle dependencies as a fallback SBOM generation. 
# In a real pipeline, apply id("org.spdx.sbom") and run :spdxSbomForRelease
.\gradlew.bat app:dependencies > $OutFile

if ($LASTEXITCODE -eq 0) {
    Write-Host "SBOM generated successfully at $OutFile"
} else {
    Write-Error "Failed to generate SBOM. Gradle exit code: $LASTEXITCODE"
}
