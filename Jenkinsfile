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

        // ============================================================
        // CHECKOUT
        // ============================================================
        stage('Checkout') {
            steps {
                echo '===== CHECKOUT ====='

                checkout scm
            }
        }


        // ============================================================
        // MAVEN BUILD
        // ============================================================
        stage('Maven Build') {
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
                    echo MAVEN BUILD START
                    echo ========================================
                    call mvn -B clean test package

                    if errorlevel 1 (
                        echo ========================================
                        echo MAVEN BUILD FAILED
                        echo ========================================
                        exit /b 1
                    )

                    echo ========================================
                    echo CHECKING USER SERVICE JAR
                    echo ========================================

                    if not exist "user-service\\target\\user-service-1.0.0.jar" (
                        echo ERROR: user-service JAR NOT FOUND
                        echo Contents of user-service target:
                        dir "user-service\\target"
                        exit /b 1
                    )

                    echo ========================================
                    echo CHECKING BOOKING SERVICE JAR
                    echo ========================================

                    if not exist "booking-service\\target\\booking-service-1.0.0.jar" (
                        echo ERROR: booking-service JAR NOT FOUND
                        echo Contents of booking-service target:
                        dir "booking-service\\target"
                        exit /b 1
                    )

                    echo ========================================
                    echo GENERATED JARS
                    echo ========================================

                    dir "user-service\\target\\*.jar"
                    dir "booking-service\\target\\*.jar"

                    echo ========================================
                    echo MAVEN BUILD SUCCESS
                    echo ========================================
                '''
            }
        }


        // ============================================================
        // DOCKER ENVIRONMENT CHECK
        // ============================================================
        stage('Docker Check') {
            steps {
                bat '''
                    echo ========================================
                    echo JENKINS WINDOWS USER
                    echo ========================================
                    whoami

                    echo ========================================
                    echo DOCKER LOCATION
                    echo ========================================
                    where docker

                    echo ========================================
                    echo DOCKER EXECUTABLE
                    echo ========================================
                    echo %DOCKER_EXE%

                    if not exist "%DOCKER_EXE%" (
                        echo ========================================
                        echo ERROR: DOCKER EXECUTABLE NOT FOUND
                        echo ========================================
                        exit /b 1
                    )

                    echo ========================================
                    echo DOCKER VERSION
                    echo ========================================
                    "%DOCKER_EXE%" --version

                    if errorlevel 1 (
                        echo ERROR: Docker version command failed.
                        exit /b 1
                    )

                    echo ========================================
                    echo DOCKER COMPOSE VERSION
                    echo ========================================
                    "%DOCKER_EXE%" compose version

                    if errorlevel 1 (
                        echo ERROR: Docker Compose command failed.
                        exit /b 1
                    )

                    echo ========================================
                    echo DOCKER CONTEXT
                    echo ========================================
                    "%DOCKER_EXE%" context show

                    if errorlevel 1 (
                        echo ERROR: Docker context command failed.
                        exit /b 1
                    )

                    echo ========================================
                    echo DOCKER INFO
                    echo ========================================
                    "%DOCKER_EXE%" info

                    if errorlevel 1 (
                        echo ========================================
                        echo DOCKER ENGINE CONNECTION FAILED
                        echo ========================================
                        exit /b 1
                    )

                    echo ========================================
                    echo DOCKER CHECK SUCCESS
                    echo ========================================
                '''
            }
        }


        // ============================================================
        // VALIDATE DOCKER COMPOSE
        // ============================================================
        stage('Validate Docker Compose') {
            steps {
                bat '''
                    echo ========================================
                    echo VALIDATING DOCKER COMPOSE
                    echo ========================================

                    "%DOCKER_EXE%" compose config -q

                    if errorlevel 1 (
                        echo ========================================
                        echo COMPOSE VALIDATION FAILED
                        echo ========================================
                        exit /b 1
                    )

                    echo ========================================
                    echo COMPOSE VALIDATION SUCCESS
                    echo ========================================
                '''
            }
        }


        // ============================================================
        // BUILD USER SERVICE IMAGE
        // ============================================================
        stage('Build user-service Docker image') {
            steps {
                bat '''
                    echo ========================================
                    echo BUILDING USER SERVICE DOCKER IMAGE
                    echo ========================================

                    "%DOCKER_EXE%" compose build user-service

                    if errorlevel 1 (
                        echo ========================================
                        echo USER SERVICE DOCKER BUILD FAILED
                        echo ========================================
                        exit /b 1
                    )

                    echo ========================================
                    echo USER SERVICE IMAGE BUILD SUCCESS
                    echo ========================================
                '''
            }
        }


        // ============================================================
        // BUILD BOOKING SERVICE IMAGE
        // ============================================================
        stage('Build booking-service Docker image') {
            steps {
                bat '''
                    echo ========================================
                    echo BUILDING BOOKING SERVICE DOCKER IMAGE
                    echo ========================================

                    "%DOCKER_EXE%" compose build booking-service

                    if errorlevel 1 (
                        echo ========================================
                        echo BOOKING SERVICE DOCKER BUILD FAILED
                        echo ========================================
                        exit /b 1
                    )

                    echo ========================================
                    echo BOOKING SERVICE IMAGE BUILD SUCCESS
                    echo ========================================
                '''
            }
        }


        // ============================================================
        // START DOCKER COMPOSE
        // ============================================================
        stage('Start Docker Compose') {
            steps {
                bat '''
                    echo ========================================
                    echo STARTING DOCKER COMPOSE
                    echo ========================================

                    "%DOCKER_EXE%" compose up -d

                    if errorlevel 1 (
                        echo ========================================
                        echo DOCKER COMPOSE START FAILED
                        echo ========================================
                        exit /b 1
                    )

                    echo ========================================
                    echo DOCKER COMPOSE STARTED
                    echo ========================================

                    "%DOCKER_EXE%" compose ps
                '''
            }
        }


        // ============================================================
        // VERIFY SERVICES
        // ============================================================
        stage('Verify Services') {
            steps {
                powershell '''
                    $ErrorActionPreference = "Stop"

                    Write-Host "========================================"
                    Write-Host "VERIFYING RIDERENT SERVICES"
                    Write-Host "========================================"

                    $scriptPath = Join-Path (Get-Location) "start-riderrent.ps1"

                    if (!(Test-Path $scriptPath)) {
                        throw "start-riderrent.ps1 was not found."
                    }

                    Write-Host "Verification script:"
                    Write-Host $scriptPath

                    & $scriptPath -SkipComposeStart

                    if ($LASTEXITCODE -ne 0) {
                        throw "RideRent verification failed with exit code $LASTEXITCODE."
                    }

                    Write-Host "========================================"
                    Write-Host "VERIFICATION SUCCESS"
                    Write-Host "========================================"
                '''
            }
        }
    }


    // ================================================================
    // POST BUILD
    // ================================================================
    post {

        success {
            echo '========================================'
            echo 'RIDERRRENT PIPELINE SUCCESS'
            echo '========================================'
        }

        failure {
            echo '========================================'
            echo 'RIDERRRENT PIPELINE FAILED'
            echo 'Check the failed stage above.'
            echo '========================================'
        }

        always {
            bat '''
                echo ========================================
                echo DOCKER CLEANUP
                echo ========================================

                "%DOCKER_EXE%" compose down --remove-orphans

                if errorlevel 1 (
                    echo Docker cleanup failed or Docker was not available.
                ) else (
                    echo Docker cleanup completed.
                )

                echo ========================================
                echo CLEANUP FINISHED
                echo ========================================
            '''
        }
    }
}