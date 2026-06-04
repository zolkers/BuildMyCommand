param(
    [string] $Token = "",
    [switch] $SkipUserEnvironment,
    [switch] $SkipGitHubSecret
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($Token)) {
    $Token = $env:JETBRAINS_MARKETPLACE_TOKEN
}

if ([string]::IsNullOrWhiteSpace($Token)) {
    $secure = Read-Host "JetBrains Marketplace token" -AsSecureString
    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try {
        $Token = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
    } finally {
        if ($bstr -ne [IntPtr]::Zero) {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
        }
    }
}

if ([string]::IsNullOrWhiteSpace($Token)) {
    throw "JETBRAINS_MARKETPLACE_TOKEN is empty."
}

if (-not $Token.StartsWith("perm:")) {
    Write-Warning "JetBrains Marketplace permanent tokens usually start with 'perm:'. Continuing anyway."
}

if (-not $SkipUserEnvironment) {
    [Environment]::SetEnvironmentVariable("JETBRAINS_MARKETPLACE_TOKEN", $Token, "User")
    $env:JETBRAINS_MARKETPLACE_TOKEN = $Token
    Write-Host "Stored JETBRAINS_MARKETPLACE_TOKEN in the Windows user environment."
}

if (-not $SkipGitHubSecret) {
    $gh = Get-Command gh -ErrorAction SilentlyContinue
    if (-not $gh) {
        throw "GitHub CLI 'gh' was not found. Install gh or rerun with -SkipGitHubSecret."
    }

    gh secret set JETBRAINS_MARKETPLACE_TOKEN --body $Token
    Write-Host "Stored JETBRAINS_MARKETPLACE_TOKEN as a GitHub Actions repository secret."
}

Write-Host "JetBrains Marketplace token setup complete."
