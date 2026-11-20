[CmdletBinding()]
param(
    [ValidateSet('Start', 'Stop', 'RestartApi', 'Status', 'Logins', 'Prepare')]
    [string]$Action = 'Start'
)

$ErrorActionPreference = 'Stop'
$state = Join-Path $PSScriptRoot '.state'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$envFile = Join-Path $state 'preview.env'
$realm = Join-Path $state 'quincaisoft-realm.json'
$issuerDiscovery = 'http://localhost:8180/realms/quincaisoft/.well-known/openid-configuration'
$compose = @('compose', '--project-name', 'quincai-local-preview', '--env-file', $envFile,
    '-f', (Join-Path $PSScriptRoot 'compose.yaml'))
$secretNames = @('PREVIEW_DB_ADMIN_PASSWORD', 'PREVIEW_DB_MIGRATOR_PASSWORD',
    'PREVIEW_DB_RUNTIME_PASSWORD', 'QUINCAI_PREVIEW_WEB_SECRET',
    'QUINCAI_PREVIEW_OWNER_PASSWORD', 'QUINCAI_PREVIEW_CASHIER_PASSWORD', 'QUINCAI_PREVIEW_AUDITOR_PASSWORD')

function Invoke-Docker {
    & docker @args
    if ($LASTEXITCODE -ne 0) { throw "Docker command failed with exit $LASTEXITCODE." }
}

function Wait-Http([string]$Url) {
    for ($attempt = 0; $attempt -lt 80; $attempt++) {
        try {
            Invoke-RestMethod -Uri $Url -TimeoutSec 3 | Out-Null
            return
        } catch { Start-Sleep -Seconds 3 }
    }
    throw "The service did not respond successfully at $Url. Inspect Docker Compose logs."
}

function Wait-LoginRoute {
    $handler = [Net.Http.HttpClientHandler]::new()
    $handler.AllowAutoRedirect = $false
    $client = [Net.Http.HttpClient]::new($handler)
    $client.Timeout = [TimeSpan]::FromSeconds(5)
    try {
        for ($attempt = 0; $attempt -lt 120; $attempt++) {
            try {
                $response = $client.GetAsync('http://localhost:3100/auth/login').GetAwaiter().GetResult()
                try {
                    $location = [string]$response.Headers.Location
                    if ([int]$response.StatusCode -eq 307 -and
                        $location.StartsWith('http://localhost:8180/realms/quincaisoft/protocol/openid-connect/auth?')) {
                        return
                    }
                } finally { $response.Dispose() }
            } catch { Write-Verbose $_ }
            Start-Sleep -Seconds 3
        }
        throw 'Next.js did not redirect to the local OIDC sign-in. Inspect its Docker Compose logs.'
    } finally { $client.Dispose() }
}

function Initialize-Preview {
    New-Item -ItemType Directory -Path $state -Force | Out-Null
    $utf8 = [Text.UTF8Encoding]::new($false)
    $values = @{}
    if (Test-Path -LiteralPath $envFile) {
        $values = ConvertFrom-StringData ([IO.File]::ReadAllText($envFile))
    }
    foreach ($name in $secretNames) {
        if (!$values.ContainsKey($name)) {
            $bytes = [byte[]]::new(32)
            $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
            try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
            $values[$name] = [BitConverter]::ToString($bytes).Replace('-', '').ToLowerInvariant()
        }
    }
    $lines = foreach ($name in $secretNames) { $name + '=' + $values[$name] }
    [IO.File]::WriteAllLines($envFile, [string[]]$lines, $utf8)
    $theme = Join-Path $state 'theme/login'
    $scripts = Join-Path $theme 'resources/js'
    New-Item -ItemType Directory -Path $scripts -Force | Out-Null
    [IO.File]::WriteAllText((Join-Path $theme 'theme.properties'), "parent=keycloak.v2`nscripts=js/demo.js`n", $utf8)
    $accounts = [ordered]@{
        owner = $values.QUINCAI_PREVIEW_OWNER_PASSWORD
        cashier = $values.QUINCAI_PREVIEW_CASHIER_PASSWORD
        auditor = $values.QUINCAI_PREVIEW_AUDITOR_PASSWORD
    }
    $demoScript = 'const previewAccounts = ' + ($accounts | ConvertTo-Json -Compress) + ';' + [Environment]::NewLine +
        [IO.File]::ReadAllText((Join-Path $PSScriptRoot 'demo-login.js'))
    [IO.File]::WriteAllText((Join-Path $scripts 'demo.js'), $demoScript, $utf8)
    Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'login.css') -Destination (Join-Path $theme 'resources/login.css') -Force
    Copy-Item -LiteralPath (Join-Path $repo 'assets/logo.png') -Destination (Join-Path $theme 'resources/logo.png') -Force
    $realmConfig = if (Test-Path -LiteralPath $realm) { Get-Content $realm -Raw | ConvertFrom-Json }
    if ($realmConfig.loginTheme -ne 'quincai-preview' -or 'preview-auditor' -notin $realmConfig.users.username) {
        $replacement = Join-Path $state ("realm-" + [guid]::NewGuid() + ".generated.json")
        $previous = @{}
        try {
            foreach ($line in [IO.File]::ReadAllLines($envFile)) {
                $name, $value = $line -split '=', 2
                if ($name -in $secretNames) {
                    $previous[$name] = [Environment]::GetEnvironmentVariable($name)
                    [Environment]::SetEnvironmentVariable($name, $value)
                }
            }
            & (Join-Path $PSScriptRoot 'New-Realm.ps1') -OutputPath $replacement
            Move-Item -LiteralPath $replacement -Destination $realm -Force
        } finally {
            foreach ($name in $previous.Keys) {
                if ($null -eq $previous[$name]) {
                    Remove-Item -LiteralPath "Env:$name" -ErrorAction SilentlyContinue
                } else {
                    [Environment]::SetEnvironmentVariable($name, $previous[$name])
                }
            }
        }
    }

    $roles = [IO.File]::ReadAllText((Join-Path $PSScriptRoot '../postgres/010-roles.sh'))
    [IO.File]::WriteAllText((Join-Path $state '010-roles.sh'), $roles.Replace("`r`n", "`n"), $utf8)
}

function Update-Jar {
    Invoke-Docker run --rm --name quincai-codex-backend --mount "type=bind,source=$repo,target=/workspace" `
        --mount 'type=volume,source=quincai-codex-maven,target=/root/.m2' `
        --workdir /workspace/backend maven:3.9-eclipse-temurin-25 `
        mvn -B -ntp -DskipTests -pl api-server -am package
    $jar = Join-Path $repo 'backend/api-server/target/api-server-0.1.0-SNAPSHOT-exec.jar'
    Copy-Item -LiteralPath $jar -Destination (Join-Path $state 'app.jar') -Force
}

if ($Action -in 'Start', 'Prepare', 'RestartApi') { Initialize-Preview }

switch ($Action) {
    'Prepare' {
        Invoke-Docker @compose up -d database identity sessions web
    }
    'Start' {
        Invoke-Docker @compose up -d --wait database identity sessions web
        if (!(Test-Path -LiteralPath (Join-Path $state 'app.jar'))) { Update-Jar }
        # Existing development volumes predate the cross-tenant V35 backfill.
        Invoke-Docker @compose exec -T database psql -U postgres -d quincaisoft -v ON_ERROR_STOP=1 -c @'
DO $$ BEGIN
 IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname='quincai_runtime' AND (rolsuper OR rolbypassrls)) THEN
  RAISE EXCEPTION 'Preview runtime must remain NOSUPERUSER NOBYPASSRLS';
 END IF;
END $$;
ALTER ROLE quincai_migrator BYPASSRLS;
'@
        Wait-Http $issuerDiscovery
        Invoke-Docker @compose up -d api
        Wait-Http 'http://localhost:8080/actuator/health'
        Invoke-Docker @compose exec -T database psql -U postgres -d quincaisoft -v preview_only=1 -v ON_ERROR_STOP=1 -f /preview/seed.sql
        Wait-LoginRoute
    }
    'RestartApi' {
        Update-Jar
        Wait-Http $issuerDiscovery
        Invoke-Docker @compose up -d --no-deps --force-recreate api
        Wait-Http 'http://localhost:8080/actuator/health'
    }
    'Stop' {
        Invoke-Docker @compose down
    }
    'Status' {
        Invoke-Docker @compose ps
    }
    'Logins' {
        if (!(Test-Path -LiteralPath $envFile)) { throw 'Run Start before retrieving local logins.' }
        $values = ConvertFrom-StringData (Get-Content -LiteralPath $envFile -Raw)
        Write-Output 'Web URL: http://localhost:3100'
        Write-Output "preview-owner   $($values.QUINCAI_PREVIEW_OWNER_PASSWORD)"
        Write-Output "preview-cashier $($values.QUINCAI_PREVIEW_CASHIER_PASSWORD)"
        Write-Output "preview-auditor $($values.QUINCAI_PREVIEW_AUDITOR_PASSWORD)"
    }
}
