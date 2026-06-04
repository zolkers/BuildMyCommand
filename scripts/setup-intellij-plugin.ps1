# Copyright (c) 2026 Zolkers
#
# Licensed under the MIT License.
# SPDX-License-Identifier: MIT

param(
    [switch] $SkipBuild,
    [switch] $Install,
    [string] $IdeConfigDir = "",
    [string] $PluginId = "dev.riege.buildmycommand.dsl",
    [string] $PluginVersion = "0.2.3"
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$root = Resolve-Path (Join-Path $scriptDir "..")
$ideaDir = Join-Path $root ".idea"
$externalDependencies = Join-Path $ideaDir "externalDependencies.xml"

New-Item -ItemType Directory -Force -Path $ideaDir | Out-Null

$xml = @"
<?xml version="1.0" encoding="UTF-8"?>
<project version="4">
  <component name="ExternalDependencies">
    <plugin id="$PluginId" min-version="$PluginVersion" />
  </component>
</project>
"@

Set-Content -LiteralPath $externalDependencies -Value $xml -Encoding UTF8
Write-Host "Declared required IntelliJ plugin in $externalDependencies"

if (-not $SkipBuild) {
    Push-Location $root
    try {
        & ".\gradlew.bat" ":intellij-plugin:buildPlugin"
    } finally {
        Pop-Location
    }
}

$distributionDir = Join-Path $root "modules/intellij-plugin/build/distributions"
$zip = Get-ChildItem -LiteralPath $distributionDir -Filter "*.zip" -ErrorAction SilentlyContinue |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

if ($zip) {
    Write-Host "Plugin ZIP ready: $($zip.FullName)"
} else {
    Write-Host "No plugin ZIP found yet. Run .\gradlew.bat :intellij-plugin:buildPlugin"
}

if ($Install) {
    if (-not $zip) {
        throw "Cannot install the IntelliJ plugin because no ZIP was found."
    }

    if ([string]::IsNullOrWhiteSpace($IdeConfigDir)) {
        $jetBrainsDir = Join-Path $env:APPDATA "JetBrains"
        $ideaConfig = Get-ChildItem -LiteralPath $jetBrainsDir -Directory -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -like "IntelliJIdea*" -or $_.Name -like "IdeaIC*" } |
            Sort-Object LastWriteTime -Descending |
            Select-Object -First 1

        if (-not $ideaConfig) {
            throw "No IntelliJ config directory found under $jetBrainsDir. Pass -IdeConfigDir explicitly."
        }

        $IdeConfigDir = $ideaConfig.FullName
    }

    $resolvedConfig = (Resolve-Path -LiteralPath $IdeConfigDir).Path
    $pluginsDir = Join-Path $resolvedConfig "plugins"
    $installDir = Join-Path $pluginsDir "intellij-plugin"
    $resolvedPluginsParent = [System.IO.Path]::GetFullPath($pluginsDir)
    $resolvedInstallDir = [System.IO.Path]::GetFullPath($installDir)

    if (-not $resolvedInstallDir.StartsWith($resolvedPluginsParent, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to install outside IntelliJ plugins directory: $resolvedInstallDir"
    }

    New-Item -ItemType Directory -Force -Path $pluginsDir | Out-Null
    if (Test-Path -LiteralPath $installDir) {
        Remove-Item -LiteralPath $installDir -Recurse -Force
    }

    Expand-Archive -LiteralPath $zip.FullName -DestinationPath $pluginsDir -Force
    Write-Host "Installed BuildMyCommand plugin into $installDir"
    Write-Host "Restart IntelliJ IDEA to load it."
} elseif ($zip) {
    Write-Host "Install it from IntelliJ: Settings > Plugins > gear > Install Plugin from Disk..."
    Write-Host "Or run: .\scripts\setup-intellij-plugin.ps1 -SkipBuild -Install"
}
