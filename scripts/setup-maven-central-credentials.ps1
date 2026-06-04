# Copyright (c) 2026 Zolkers
#
# Licensed under the MIT License.
# SPDX-License-Identifier: MIT

param(
    [string] $Username,
    [string] $Password,
    [string] $TokenBase64,
    [string] $SigningInMemoryKeyFile,
    [string] $SigningInMemoryKeyPassword,
    [string] $GradleUserHome
)

$ErrorActionPreference = "Stop"

function Resolve-GradleUserHome {
    param([string] $ExplicitHome)
    if ($ExplicitHome) {
        return [System.IO.Path]::GetFullPath($ExplicitHome)
    }
    if ($env:GRADLE_USER_HOME) {
        return [System.IO.Path]::GetFullPath($env:GRADLE_USER_HOME)
    }
    return Join-Path $HOME ".gradle"
}

function Decode-CentralToken {
    param([string] $Encoded)
    if ($Encoded.Contains(":")) {
        throw "TokenBase64 must be the base64-encoded value of 'username:password', not the raw 'username:password' text. Either encode it first or use -Username and -Password."
    }
    try {
        $decoded = [System.Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($Encoded))
    } catch {
        throw "TokenBase64 is not valid base64. Pass the encoded value, or use -Username and -Password."
    }
    $separator = $decoded.IndexOf(":")
    if ($separator -lt 1 -or $separator -eq ($decoded.Length - 1)) {
        throw "TokenBase64 must decode to 'username:password'."
    }
    return @{
        Username = $decoded.Substring(0, $separator)
        Password = $decoded.Substring($separator + 1)
    }
}

function Escape-GradlePropertyValue {
    param([string] $Value)
    return $Value -replace "\\", "\\\\" -replace "`r`n", "\n" -replace "`n", "\n"
}

function Set-GradleProperty {
    param(
        [string[]] $Lines,
        [string] $Key,
        [string] $Value
    )

    $escaped = Escape-GradlePropertyValue $Value
    $replacement = "$Key=$escaped"
    $updated = $false
    $next = [System.Collections.Generic.List[string]]::new()
    foreach ($line in $Lines) {
        if ($line -match "^\s*$([regex]::Escape($Key))\s*=") {
            $updated = $true
            $next.Add($replacement)
        } else {
            $next.Add($line)
        }
    }

    if (-not $updated) {
        $next.Add($replacement)
    }
    return $next.ToArray()
}

if ($TokenBase64) {
    $token = Decode-CentralToken $TokenBase64
    $Username = $token.Username
    $Password = $token.Password
}

if (-not $Username -or -not $Password) {
    throw "Provide either -Username and -Password, or -TokenBase64."
}

$homeDir = Resolve-GradleUserHome $GradleUserHome
$propertiesFile = Join-Path $homeDir "gradle.properties"
New-Item -ItemType Directory -Path $homeDir -Force | Out-Null

$lines = @()
if (Test-Path $propertiesFile) {
    $lines = Get-Content -Path $propertiesFile
}

$lines = Set-GradleProperty $lines "mavenCentralUsername" $Username
$lines = Set-GradleProperty $lines "mavenCentralPassword" $Password

if ($SigningInMemoryKeyFile) {
    $key = Get-Content -Path $SigningInMemoryKeyFile -Raw
    $lines = Set-GradleProperty $lines "signingInMemoryKey" $key
}

if ($SigningInMemoryKeyPassword) {
    $lines = Set-GradleProperty $lines "signingInMemoryKeyPassword" $SigningInMemoryKeyPassword
}

Set-Content -Path $propertiesFile -Value $lines -Encoding UTF8

Write-Host "Maven Central Gradle properties updated at $propertiesFile"
Write-Host "Stored keys: mavenCentralUsername, mavenCentralPassword$(if ($SigningInMemoryKeyFile) { ', signingInMemoryKey' })$(if ($SigningInMemoryKeyPassword) { ', signingInMemoryKeyPassword' })"
