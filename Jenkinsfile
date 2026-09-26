def invokeDocker(String arguments, boolean failOnError = true) {
    def status = powershell(
        returnStatus: true,
        script: """
            \$ErrorActionPreference = 'Stop'

            \$dockerPath = \$env:DOCKER_EXE

            if ([string]::IsNullOrWhiteSpace(\$dockerPath)) {
                \$dockerCommand = Get-Command docker.exe -ErrorAction SilentlyContinue
                if (\$null -ne \$dockerCommand) {
                    \$dockerPath = \$dockerCommand.Source
                }
            }

            if ([string]::IsNullOrWhiteSpace(\$dockerPath)) {
                \$candidates = @(
                    'C:/Users/Altaf Shaikh/AppData/Local/Programs/DockerDesktop/resources/bin/docker.exe',
                    (Join-Path \$env:ProgramFiles 'Docker\\Docker\\resources\\bin\\docker.exe'),
                    (Join-Path \$env:ProgramFiles 'DockerDesktop\\resources\\bin\\docker.exe'),
                    (Join-Path \$env:LOCALAPPDATA 'Programs\\DockerDesktop\\resources\\bin\\docker.exe')
                )

                \$dockerPath = \$candidates |
                    Where-Object { Test-Path -LiteralPath \$_ } |
                    Select-Object -First 1
            }

            if ([string]::IsNullOrWhiteSpace(\$dockerPath)) {
                Write-Error 'Docker executable was not found.'
                exit 127
            }

            # IMPORTANT:
            # Split Docker arguments into separate arguments.
            \$dockerArgs = \$arguments -split '\\s+'

            Write-Host "Docker: \$dockerPath"
            Write-Host "Arguments: \$arguments"

            & \$dockerPath @dockerArgs

            exit \$LASTEXITCODE
        """
    )

    if (failOnError && status != 0) {
        error "Docker command failed with exit code ${status}: ${arguments}"
    }

    return status
}


pipeline {
    agent any

    tools {
        maven 'Maven'
    }

    options {
        skipDefaultCheckout(true)
        disableConcurrentBuilds()
        timestamps()
    }

    environment {
        DB_USERNAME = credentials('riderrent-db-username')
        DB_PASSWORD = credentials('riderrent-db-password')
        JWT_SECRET = credentials('riderrent-jwt-secret')

        RATE_LIMIT_REQUESTS_PER_MINUTE = '60'
        COMPOSE_PROJECT_NAME = "riderrent-jenkins-${BUILD_NUMBER}"
    }

    stages {

        stage('Checkout') {
            steps {
                echo 'Checking out RideRent source code...'
                checkout scm
            }
        }


        stage('Maven clean test package') {
            steps {
                bat '''
                    echo ========================================
                    echo JAVA VERSION
                    echo ========================================
                    java -version

                    echo ========================================
                    echo MAVEN VERSION
                    echo ========================================
                    call mvn -version

                    echo ========================================
                    echo RUNNING MAVEN BUILD
                    echo ========================================
                    call mvn -B clean test package

                    if errorlevel 1 (
                        echo ERROR: Maven build failed.
                        exit /b 1
                    )

                    echo ========================================
                    echo VERIFYING GENERATED JARS
                    echo ========================================

                    if not exist "user-service\\target\\user-service-1.0.0.jar" (
                        echo ERROR: Missing user-service\\target\\user-service-1.0.0.jar
                        echo Maven did not generate the required user-service JAR.
                        exit /b 1
                    )

                    if not exist "booking-service\\target\\booking-service-1.0.0.jar" (
                        echo ERROR: Missing booking-service\\target\\booking-service-1.0.0.jar
                        echo Maven did not generate the required booking-service JAR.
                        exit /b 1
                    )

                    echo ========================================
                    echo GENERATED JAR FILES
                    echo ========================================

                    dir "user-service\\target\\user-service-1.0.0.jar"
                    dir "booking-service\\target\\booking-service-1.0.0.jar"

                    echo ========================================
                    echo MAVEN BUILD SUCCESSFUL
                    echo ========================================
                '''
            }
        }


        stage('Validate docker-compose.yml') {
            steps {
                script {
                    echo 'Validating Docker Compose configuration...'
                    invokeDocker('compose config --quiet')
                }
            }
        }


        stage('Build user-service Docker image') {
            steps {
                script {
                    echo 'Building user-service Docker image...'
                    invokeDocker('compose build user-service')
                }
            }
        }


        stage('Build booking-service Docker image') {
            steps {
                script {
                    echo 'Building booking-service Docker image...'
                    invokeDocker('compose build booking-service')
                }
            }
        }


        stage('Start Docker Compose stack') {
            steps {
                script {
                    echo 'Starting Docker Compose stack...'
                    invokeDocker('compose up -d')
                }
            }
        }


        stage('Verify services and APIs') {
            steps {
                powershell '''
                    $ErrorActionPreference = 'Stop'

                    Write-Host "========================================"
                    Write-Host "VERIFYING RIDERENT SERVICES"
                    Write-Host "========================================"

                    $scriptPath = Join-Path (Get-Location) 'start-riderrent.ps1'

                    & $scriptPath -SkipComposeStart

                    if ($LASTEXITCODE -ne 0) {
                        throw "RideRent verification failed with exit code $LASTEXITCODE."
                    }

                    Write-Host "========================================"
                    Write-Host "RIDERRRENT VERIFICATION SUCCESSFUL"
                    Write-Host "========================================"
                '''
            }
        }
    }


    post {

        success {
            echo '========================================'
            echo 'RideRent Jenkins pipeline completed successfully.'
            echo '========================================'
        }

        failure {
            echo '========================================'
            echo 'RideRent Jenkins pipeline FAILED.'
            echo 'Check the failed stage above.'
            echo '========================================'
        }

        always {
            script {
                if (getContext(hudson.FilePath) != null) {
                    echo 'Cleaning up Docker Compose stack...'
                    invokeDocker('compose down --remove-orphans', false)
                } else {
                    echo 'Skipping Docker cleanup because Jenkins workspace is unavailable.'
                }
            }
        }
    }
}