$ErrorActionPreference = 'Stop'

$projectRoot = $PSScriptRoot
$requiredVariables = @('DB_PASSWORD', 'JWT_SECRET')

foreach ($name in $requiredVariables) {
  if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name))) {
    throw "$name is not set in the Windows environment. Set it once, then rerun this script."
  }
}

if ([string]::IsNullOrWhiteSpace($env:DB_USERNAME)) {
  $env:DB_USERNAME = 'root'
}

function Test-PortInUse([int]$port) {
  return $null -ne (Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue)
}

function Start-RideRentService([string]$module, [int]$port) {
  if (Test-PortInUse $port) {
    Write-Host "$module is already listening on port $port."
    return
  }

  $command = "Set-Location '$projectRoot'; mvn.cmd -pl $module spring-boot:run"
  Start-Process powershell.exe -ArgumentList '-NoExit', '-NoProfile', '-Command', $command
  Write-Host "Starting $module on port $port."
}

Start-RideRentService 'user-service' 8081
Start-RideRentService 'booking-service' 8082
Start-RideRentService 'api-gateway' 8085