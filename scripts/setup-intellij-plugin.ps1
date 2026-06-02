param(
    [switch] $SkipBuild,
    [string] $PluginId = "dev.riege.buildmycommand.intellij",
    [string] $PluginVersion = "0.1.0"
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
    Write-Host "Install it from IntelliJ: Settings > Plugins > gear > Install Plugin from Disk..."
} else {
    Write-Host "No plugin ZIP found yet. Run .\gradlew.bat :intellij-plugin:buildPlugin"
}
