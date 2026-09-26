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
                echo '===== CHECKOUT ====='
                checkout scm
            }
        }

        stage('Maven Build') {
            steps {
                bat '''
                    echo ===== JAVA =====
                    java -version

                    echo ===== MAVEN =====
                    call mvn -version

                    echo ===== MAVEN BUILD START =====
                    call mvn -B clean test package

                    if errorlevel 1 (
                        echo ===== MAVEN BUILD FAILED =====
                        exit /b 1
                    )

                    echo ===== CHECK USER SERVICE JAR =====
                    if not exist "user-service\\target\\user-service-1.0.0.jar" (
                        echo ERROR: user-service JAR NOT FOUND
                        dir "user-service\\target"
                        exit /b 1
                    )

                    echo ===== CHECK BOOKING SERVICE JAR =====
                    if not exist "booking-service\\target\\booking-service-1.0.0.jar" (
                        echo ERROR: booking-service JAR NOT FOUND
                        dir "booking-service\\target"
                        exit /b 1
                    )

                    echo ===== JARS FOUND =====
                    dir "user-service\\target\\*.jar"
                    dir "booking-service\\target\\*.jar"

                    echo ===== MAVEN BUILD SUCCESS =====
                '''
            }
        }

        stage('Docker Check') {
            steps {
                bat '''
                    echo ===== DOCKER VERSION =====
                    "%DOCKER_EXE%" version

                    echo ===== DOCKER COMPOSE VERSION =====
                    "%DOCKER_EXE%" compose version
                '''
            }
        }

        stage('Validate Docker Compose') {
            steps {
                bat '''
                    echo ===== VALIDATING COMPOSE =====
                    "%DOCKER_EXE%" compose config -q

                    if errorlevel 1 (
                        echo ===== COMPOSE VALIDATION FAILED =====
                        exit /b 1
                    )

                    echo ===== COMPOSE VALIDATION SUCCESS =====
                '''
            }
        }

        stage('Build user-service Docker image') {
            steps {
                bat '''
                    echo ===== BUILDING USER SERVICE =====
                    "%DOCKER_EXE%" compose build user-service

                    if errorlevel 1 (
                        echo ===== USER SERVICE DOCKER BUILD FAILED =====
                        exit /b 1
                    )

                    echo ===== USER SERVICE IMAGE SUCCESS =====
                '''
            }
        }

        stage('Build booking-service Docker image') {
            steps {
                bat '''
                    echo ===== BUILDING BOOKING SERVICE =====
                    "%DOCKER_EXE%" compose build booking-service

                    if errorlevel 1 (
                        echo ===== BOOKING SERVICE DOCKER BUILD FAILED =====
                        exit /b 1
                    )

                    echo ===== BOOKING SERVICE IMAGE SUCCESS =====
                '''
            }
        }

        stage('Start Docker Compose') {
            steps {
                bat '''
                    echo ===== STARTING DOCKER COMPOSE =====
                    "%DOCKER_EXE%" compose up -d

                    if errorlevel 1 (
                        echo ===== DOCKER COMPOSE START FAILED =====
                        exit /b 1
                    )

                    echo ===== DOCKER COMPOSE STARTED =====
                '''
            }
        }

        stage('Verify Services') {
            steps {
                powershell '''
                    $ErrorActionPreference = "Stop"

                    Write-Host "===== VERIFYING RIDERENT ====="

                    $scriptPath = Join-Path (Get-Location) "start-riderrent.ps1"

                    if (!(Test-Path $scriptPath)) {
                        throw "start-riderrent.ps1 was not found."
                    }

                    & $scriptPath -SkipComposeStart

                    if ($LASTEXITCODE -ne 0) {
                        throw "RideRent verification failed with exit code $LASTEXITCODE."
                    }

                    Write-Host "===== VERIFICATION SUCCESS ====="
                '''
            }
        }
    }

    post {

        success {
            echo '===== RIDERENT PIPELINE SUCCESS ====='
        }

        failure {
            echo '===== RIDERENT PIPELINE FAILED ====='
        }

        always {
            bat '''
                echo ===== DOCKER CLEANUP =====
                "%DOCKER_EXE%" compose down --remove-orphans
            '''
        }
    }
}