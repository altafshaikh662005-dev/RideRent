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

        DOCKER_EXE = 'C:\\Users\\Altaf Shaikh\\AppData\\Local\\Programs\\DockerDesktop\\resources\\bin\\docker.exe'
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
                        echo ERROR: user-service JAR NOT FOUND
                        echo Expected:
                        echo user-service\\target\\user-service-1.0.0.jar
                        exit /b 1
                    )

                    if not exist "booking-service\\target\\booking-service-1.0.0.jar" (
                        echo ERROR: booking-service JAR NOT FOUND
                        echo Expected:
                        echo booking-service\\target\\booking-service-1.0.0.jar
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


        stage('Validate Docker Compose') {
            steps {
                bat '''
                    echo ========================================
                    echo DOCKER VERSION
                    echo ========================================
                    "%DOCKER_EXE%" version

                    echo ========================================
                    echo DOCKER COMPOSE VERSION
                    echo ========================================
                    "%DOCKER_EXE%" compose version

                    echo ========================================
                    echo VALIDATING DOCKER COMPOSE
                    echo ========================================
                    "%DOCKER_EXE%" compose config -q

                    if errorlevel 1 (
                        echo ERROR: Docker Compose configuration is invalid.
                        exit /b 1
                    )

                    echo Docker Compose configuration is valid.
                '''
            }
        }


        stage('Build user-service Docker image') {
            steps {
                bat '''
                    echo ========================================
                    echo BUILDING USER-SERVICE DOCKER IMAGE
                    echo ========================================

                    "%DOCKER_EXE%" compose build user-service

                    if errorlevel 1 (
                        echo ERROR: user-service Docker build failed.
                        exit /b 1
                    )

                    echo user-service Docker image built successfully.
                '''
            }
        }


        stage('Build booking-service Docker image') {
            steps {
                bat '''
                    echo ========================================
                    echo BUILDING BOOKING-SERVICE DOCKER IMAGE
                    echo ========================================

                    "%DOCKER_EXE%" compose build booking-service

                    if errorlevel 1 (
                        echo ERROR: booking-service Docker build failed.
                        exit /b 1
                    )

                    echo booking-service Docker image built successfully.
                '''
            }
        }


        stage('Start Docker Compose stack') {
            steps {
                bat '''
                    echo ========================================
                    echo STARTING RIDERENT DOCKER STACK
                    echo ========================================

                    "%DOCKER_EXE%" compose up -d

                    if errorlevel 1 (
                        echo ERROR: Docker Compose startup failed.
                        exit /b 1
                    )

                    echo Docker Compose stack started successfully.
                '''
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

                    if (-not (Test-Path $scriptPath)) {
                        throw "start-riderrent.ps1 was not found."
                    }

                    & $scriptPath -SkipComposeStart

                    if ($LASTEXITCODE -ne 0) {
                        throw "RideRent verification failed with exit code $LASTEXITCODE."
                    }

                    Write-Host "========================================"
                    Write-Host "RIDERENT VERIFICATION SUCCESSFUL"
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
            bat '''
                echo ========================================
                echo DOCKER COMPOSE CLEANUP
                echo ========================================

                "%DOCKER_EXE%" compose down --remove-orphans

                if errorlevel 1 (
                    echo WARNING: Docker Compose cleanup returned an error.
                ) else (
                    echo Docker Compose cleanup completed.
                )
            '''
        }
    }
}