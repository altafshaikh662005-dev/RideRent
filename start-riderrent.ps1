param([switch]$SkipComposeStart)

$ErrorActionPreference = 'Stop'

$projectRoot = $PSScriptRoot
$composeFile = Join-Path $projectRoot 'docker-compose.yml'
$composeArgs = @('-f', $composeFile)
$testEmail = $null
$testUserId = $null
$testBookingId = $null

function Invoke-Compose {
    param([Parameter(Mandatory = $true)][string[]]$Arguments)
    & docker compose @composeArgs @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Docker Compose command failed: docker compose $($Arguments -join ' ')"
    }
}

function Get-ComposeContainerId {
    param([Parameter(Mandatory = $true)][string]$Service)
    $id = (& docker compose @composeArgs ps -q $Service | Out-String).Trim()
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($id)) {
        throw "Required Compose service '$Service' has no container."
    }
    return $id
}

function Get-ContainerHealth {
    param([Parameter(Mandatory = $true)][string]$ContainerId)
    $health = (& docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' $ContainerId | Out-String).Trim()
    if ($LASTEXITCODE -ne 0) {
        return 'missing'
    }
    return $health
}

function Show-ComposeFailure {
    param([Parameter(Mandatory = $true)][string]$Service)
    Write-Host "Recent logs for ${Service}:" -ForegroundColor Yellow
    & docker compose @composeArgs logs --no-color --tail 60 $Service
}

function Wait-Healthy {
    param(
        [Parameter(Mandatory = $true)][string]$Service,
        [int]$TimeoutSeconds = 300
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        $containerId = Get-ComposeContainerId $Service
        $health = Get-ContainerHealth $containerId
        if ($health -eq 'healthy') {
            Write-Host "[OK] $Service is healthy" -ForegroundColor Green
            return
        }
        if ($health -eq 'exited' -or $health -eq 'dead') {
            Show-ComposeFailure $Service
            throw "$Service stopped before becoming healthy."
        }
        Start-Sleep -Seconds 5
    } while ((Get-Date) -lt $deadline)

    Show-ComposeFailure $Service
    throw "$Service did not become healthy within $TimeoutSeconds seconds."
}

function Get-HttpStatus {
    param(
        [Parameter(Mandatory = $true)][string]$Uri,
        [hashtable]$Headers = @{}
    )
    try {
        $response = Invoke-WebRequest -UseBasicParsing -Method Get -Uri $Uri -Headers $Headers -TimeoutSec 15
        return [int]$response.StatusCode
    } catch {
        if ($null -ne $_.Exception.Response) {
            return [int]$_.Exception.Response.StatusCode
        }
        return 0
    }
}

function Get-JwtSubject {
    param([Parameter(Mandatory = $true)][string]$Token)
    $payload = $Token.Split('.')[1].Replace('-', '+').Replace('_', '/')
    while (($payload.Length % 4) -ne 0) {
        $payload += '='
    }
    $json = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($payload))
    return (ConvertFrom-Json $json).sub
}

function Remove-TestData {
    if ([string]::IsNullOrWhiteSpace($testEmail)) {
        return
    }

    try {
        if ($null -ne $testBookingId) {
            $statement = "DELETE FROM bookings WHERE id = $testBookingId;"
            & docker compose @composeArgs exec -T -e "MYSQL_PWD=$env:DB_PASSWORD" mysql-booking mysql -uroot booking_db -e $statement *> $null
        }
        $statement = "DELETE FROM users WHERE email = '$testEmail';"
        & docker compose @composeArgs exec -T -e "MYSQL_PWD=$env:DB_PASSWORD" mysql-user mysql -uroot user_db -e $statement *> $null
    } catch {
        Write-Host 'Warning: temporary verification data could not be removed.' -ForegroundColor Yellow
    }
}

try {
    if (-not (Test-Path -LiteralPath $composeFile)) {
        throw "docker-compose.yml was not found at $composeFile"
    }
    if ($null -eq (Get-Command docker -ErrorAction SilentlyContinue)) {
        throw 'Docker Desktop or Docker Engine is not available on PATH.'
    }
    & docker info *> $null
    if ($LASTEXITCODE -ne 0) {
        throw 'Docker Engine is unavailable. Start Docker Desktop and run this script again.'
    }

    if ([string]::IsNullOrWhiteSpace($env:DB_USERNAME)) {
        throw 'DB_USERNAME is not set. Configure the Jenkins DB username credential or set DB_USERNAME for local execution.'
    }
    if ([string]::IsNullOrWhiteSpace($env:DB_PASSWORD)) {
        throw 'DB_PASSWORD is not set. Configure the Jenkins DB password credential or set DB_PASSWORD for local execution.'
    }
    if ([string]::IsNullOrWhiteSpace($env:JWT_SECRET)) {
        throw 'JWT_SECRET is not set. Configure the Jenkins JWT secret credential or set JWT_SECRET for local execution.'
    }
    if ([string]::IsNullOrWhiteSpace($env:RATE_LIMIT_REQUESTS_PER_MINUTE)) {
        $env:RATE_LIMIT_REQUESTS_PER_MINUTE = '60'
    }

    if ([int]$env:RATE_LIMIT_REQUESTS_PER_MINUTE -le 0) {
        throw 'RATE_LIMIT_REQUESTS_PER_MINUTE must be greater than zero.'
    }

    & docker compose @composeArgs config --quiet
    if ($LASTEXITCODE -ne 0) {
        throw 'docker-compose.yml is invalid.'
    }

    if (-not $SkipComposeStart) {
        Write-Host 'Starting RideRent-New Docker Compose stack...' -ForegroundColor Cyan
        Invoke-Compose @('up', '-d', '--build')
    }

    Wait-Healthy 'mysql-user'
    Wait-Healthy 'mysql-booking'
    Wait-Healthy 'user-service'
    Wait-Healthy 'booking-service'

    $userHealth = Get-HttpStatus 'http://localhost:8081/actuator/health'
    $bookingHealth = Get-HttpStatus 'http://localhost:8082/actuator/health'
    if ($userHealth -ne 200 -or $bookingHealth -ne 200) {
        throw "Health endpoint verification failed: user=$userHealth booking=$bookingHealth"
    }

    $swaggerUser = Get-HttpStatus 'http://localhost:8081/swagger-ui/index.html'
    $swaggerBooking = Get-HttpStatus 'http://localhost:8082/swagger-ui/index.html'
    if ($swaggerUser -ne 200 -or $swaggerBooking -ne 200) {
        throw "Swagger verification failed: user=$swaggerUser booking=$swaggerBooking"
    }

    $composeConfig = (& docker compose @composeArgs config | Out-String)
    if ($composeConfig -notmatch 'USER_SERVICE_URL:\s*http://user-service:8081') {
        throw 'Compose does not configure Booking Service to use http://user-service:8081.'
    }

    $testEmail = "riderrent-startup-$([Guid]::NewGuid().ToString('N'))@example.com"
    $testPassword = "StartupCheck_$([Guid]::NewGuid().ToString('N'))"
    $registerBody = @{ name = 'RideRent Startup Check'; email = $testEmail; password = $testPassword; phone = '0000000000' } | ConvertTo-Json
    $register = Invoke-WebRequest -UseBasicParsing -Method Post -Uri 'http://localhost:8081/api/auth/register' -Body $registerBody -ContentType 'application/json' -TimeoutSec 15
    if ([int]$register.StatusCode -ne 201) {
        throw "Registration verification failed with HTTP $($register.StatusCode)."
    }

    $loginBody = @{ email = $testEmail; password = $testPassword } | ConvertTo-Json
    $login = Invoke-RestMethod -Method Post -Uri 'http://localhost:8081/api/auth/login' -Body $loginBody -ContentType 'application/json' -TimeoutSec 15
    if ([string]::IsNullOrWhiteSpace($login.token)) {
        throw 'Login did not return a JWT token.'
    }
    $token = [string]$login.token
    $testUserId = [long](Get-JwtSubject $token)
    $authHeaders = @{ Authorization = "Bearer $token" }

    $protectedUser = Get-HttpStatus -Uri "http://localhost:8081/api/users/$testUserId" -Headers $authHeaders
    if ($protectedUser -ne 200) {
        throw "Protected User Service verification failed with HTTP $protectedUser."
    }

    $bookingBody = @{ userId = $testUserId; vehicleName = 'RideRent Startup Check'; bookingDate = (Get-Date).AddDays(1).ToString('yyyy-MM-dd'); status = 'CONFIRMED' } | ConvertTo-Json
    $booking = Invoke-RestMethod -Method Post -Uri 'http://localhost:8082/api/bookings' -Headers $authHeaders -Body $bookingBody -ContentType 'application/json' -TimeoutSec 15
    $testBookingId = [long]$booking.id
    if ($testBookingId -le 0) {
        throw 'Booking verification did not return a booking ID.'
    }

    $attempts = [int]$env:RATE_LIMIT_REQUESTS_PER_MINUTE + 10
    $successfulRequests = 0
    $rateLimitedRequests = 0
    for ($requestNumber = 1; $requestNumber -le $attempts; $requestNumber++) {
        $status = Get-HttpStatus -Uri "http://localhost:8081/api/users/$testUserId" -Headers $authHeaders
        if ($status -eq 200) {
            $successfulRequests++
        } elseif ($status -eq 429) {
            $rateLimitedRequests++
        }
    }
    if ($rateLimitedRequests -eq 0) {
        throw "Rate limiting was not observed after $attempts requests."
    }

    Invoke-Compose @('restart', 'user-service', 'booking-service')
    Wait-Healthy 'user-service'
    Wait-Healthy 'booking-service'

    Write-Host ''
    Write-Host 'RideRent-New STARTUP COMPLETE' -ForegroundColor Green
    Write-Host ''
    Write-Host '[OK] MySQL User DB'
    Write-Host '[OK] MySQL Booking DB'
    Write-Host '[OK] User Service :8081'
    Write-Host '[OK] Booking Service :8082'
    Write-Host '[OK] JWT Authentication'
    Write-Host '[OK] User -> Database'
    Write-Host '[OK] Booking -> User Service REST connection'
    Write-Host "[OK] Rate Limiting (successful: $successfulRequests, 429: $rateLimitedRequests)"
    Write-Host '[OK] Swagger Documentation'
    Write-Host '[OK] Docker Compose'
    Write-Host ''
    Write-Host 'URLs:'
    Write-Host 'User Service: http://localhost:8081'
    Write-Host 'Booking Service: http://localhost:8082'
    Write-Host 'User Swagger: http://localhost:8081/swagger-ui/index.html'
    Write-Host 'Booking Swagger: http://localhost:8082/swagger-ui/index.html'
} catch {
    Write-Host ''
    Write-Host "RideRent-New startup failed: $($_.Exception.Message)" -ForegroundColor Red
    if ($null -ne $testEmail) {
        Write-Host 'The Compose stack was left running for diagnosis; no database volumes were removed.' -ForegroundColor Yellow
    }
    exit 1
} finally {
    Remove-TestData
}
