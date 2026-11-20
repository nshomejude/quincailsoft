[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [string]$OutputPath
)

$ErrorActionPreference = 'Stop'

function Get-PreviewSecret([string]$Name) {
    $value = [Environment]::GetEnvironmentVariable($Name)
    if ([string]::IsNullOrWhiteSpace($value) -or $value.Length -lt 24) {
        throw "Provide a generated secret of at least 24 characters in $Name."
    }
    return $value
}

function New-Mapper([string]$Name, [string]$Mapper, [hashtable]$Config) {
    return @{
        name = $Name
        protocol = 'openid-connect'
        protocolMapper = $Mapper
        consentRequired = $false
        config = $Config
    }
}

$ownerPassword = Get-PreviewSecret 'QUINCAI_PREVIEW_OWNER_PASSWORD'
$cashierPassword = Get-PreviewSecret 'QUINCAI_PREVIEW_CASHIER_PASSWORD'
$auditorPassword = Get-PreviewSecret 'QUINCAI_PREVIEW_AUDITOR_PASSWORD'
$webSecret = Get-PreviewSecret 'QUINCAI_PREVIEW_WEB_SECRET'
if (@($ownerPassword, $cashierPassword, $auditorPassword, $webSecret | Select-Object -Unique).Count -ne 4) {
    throw 'Generate a distinct secret for each preview user and client.'
}

$cashierPermissions = @(
    'catalog:read', 'cash:manage', 'cash:read', 'customers:read', 'documents:print',
    'documents:read', 'documents:write', 'inventory:read', 'payments:write', 'quotations:issue',
    'quotations:read', 'quotations:write', 'returns:read', 'returns:write', 'sales:post',
    'sales:read', 'sales:write', 'tenders:read', 'tenders:write'
)
$ownerPermissions = @($cashierPermissions + @(
    'audit:read', 'bank:approve', 'bank:read', 'bank:write', 'catalog:write', 'commerce:manage',
    'customers:write', 'documents:configure', 'expenses:approve', 'expenses:pay', 'expenses:read',
    'expenses:write', 'finance:approve', 'finance:read', 'finance:write', 'identity:manage',
    'inventory:approve', 'inventory:write', 'journals:approve', 'journals:write',
    'operations:export', 'operations:import', 'operations:integrity', 'operations:recover',
    'periods:approve', 'periods:manage', 'purchasing:approve', 'purchasing:invoice',
    'purchasing:read', 'purchasing:receive', 'purchasing:return', 'purchasing:write',
    'reports:export', 'reports:read', 'returns:approve', 'returns:post',
    'sync:enroll', 'sync:manage', 'sync:recover', 'tenders:refund', 'tenders:supplier-pay'
) | Sort-Object -Unique)

# These fixed local identities must match seed.sql.
$claims = [ordered]@{
    tenant_id = 'b9cacd3e-3ffd-49ec-a7a7-76ee6279d637'
    company_id = '99d8c254-0152-461c-a55b-2400e13ffc85'
    branch_id = '9d6b5a41-2a21-4b51-9be1-df350a2a63e6'
    authorization_epoch = '1'
}

$mappers = @(
    foreach ($entry in $claims.GetEnumerator()) {
        $type = if ($entry.Key -eq 'authorization_epoch') { 'long' } else { 'String' }
        New-Mapper $entry.Key 'oidc-hardcoded-claim-mapper' @{
            'claim.name' = $entry.Key
            'claim.value' = $entry.Value
            'jsonType.label' = $type
            'access.token.claim' = 'true'
        }
    }
    New-Mapper 'Java API audience' 'oidc-audience-mapper' @{
        'included.client.audience' = 'quincai-api'
        'access.token.claim' = 'true'
    }
    New-Mapper 'Java API permissions' 'oidc-usermodel-client-role-mapper' @{
        'usermodel.clientRoleMapping.clientId' = 'quincai-api'
        'claim.name' = 'permissions'
        'jsonType.label' = 'String'
        multivalued = 'true'
        'access.token.claim' = 'true'
    }
)

function New-User([string]$ActorId, [string]$Role, [string]$Password, [array]$Permissions) {
    return @{
        id = $ActorId
        username = "preview-$Role"
        enabled = $true
        emailVerified = $true
        email = "$Role@preview.example.invalid"
        firstName = 'Preview'
        lastName = $Role
        credentials = @(@{type = 'password'; value = $Password; temporary = $false})
        clientRoles = @{'quincai-api' = $Permissions}
    }
}

$realm = @{
    realm = 'quincaisoft'
    enabled = $true
    displayName = 'QuincaiSoft local preview'
    loginTheme = 'quincai-preview'
    sslRequired = 'external'
    registrationAllowed = $false
    resetPasswordAllowed = $false
    bruteForceProtected = $true
    accessTokenLifespan = 900
    roles = @{client = @{'quincai-api' = @($ownerPermissions | ForEach-Object { @{name = $_} })}}
    clients = @(
        @{
            clientId = 'quincai-api'
            enabled = $true
            protocol = 'openid-connect'
            bearerOnly = $true
            standardFlowEnabled = $false
            directAccessGrantsEnabled = $false
        },
        @{
            clientId = 'quincaisoft-web'
            enabled = $true
            protocol = 'openid-connect'
            publicClient = $false
            secret = $webSecret
            fullScopeAllowed = $true
            standardFlowEnabled = $true
            directAccessGrantsEnabled = $false
            implicitFlowEnabled = $false
            serviceAccountsEnabled = $false
            redirectUris = @('http://localhost:3100/auth/callback')
            attributes = @{'pkce.code.challenge.method' = 'S256'}
            protocolMappers = $mappers
        }
    )
    users = @(
        New-User '0543fde0-65cc-4cb4-af22-6afe2d604e25' 'owner' $ownerPassword $ownerPermissions
        New-User '1ca68eb9-1f85-4e05-b969-d63bf95cc3f8' 'cashier' $cashierPassword $cashierPermissions
        New-User '5edc2790-9131-4654-92ba-adc60c05eb8a' 'auditor' $auditorPassword @('audit:read', 'reports:read', 'finance:read', 'catalog:read', 'sales:read', 'inventory:read')
    )
}

$path = $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($OutputPath)
if (Test-Path -LiteralPath $path) {
    throw 'Choose a new realm file path to avoid replacing credentials in use.'
}
[IO.File]::WriteAllText($path, ($realm | ConvertTo-Json -Depth 16), [Text.UTF8Encoding]::new($false))
