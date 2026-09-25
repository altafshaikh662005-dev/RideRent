def invokeDocker(String arguments, boolean failOnError = true) {
    def status = powershell(returnStatus: true, script: """
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
            \$dockerPath = \$candidates | Where-Object { Test-Path -LiteralPath \$_ } | Select-Object -First 1
        }
        if ([string]::IsNullOrWhiteSpace(\$dockerPath)) {
            Write-Error 'Docker executable was not found. Configure DOCKER_EXE or add Docker to the Jenkins service PATH.'
            exit 127
        }
        & \$dockerPath ${arguments}
        exit \$LASTEXITCODE
    """)
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
                checkout scm
            }
        }

        stage('Maven clean test package') {
            steps {
                bat '''
                    java -version
                    mvn -version
                    mvn -B clean test package
                    if not exist "user-service\\target\\user-service-1.0.0.jar" (
                        echo ERROR: Missing user-service\\target\\user-service-1.0.0.jar
                        exit /b 1
                    )
                    if not exist "booking-service\\target\\booking-service-1.0.0.jar" (
                        echo ERROR: Missing booking-service\\target\\booking-service-1.0.0.jar
                        exit /b 1
                    )
                    echo Generated JAR files:
                    dir /-C "user-service\\target\\user-service-1.0.0.jar"
                    dir /-C "booking-service\\target\\booking-service-1.0.0.jar"
                '''
            }
        }

        stage('Validate docker-compose.yml') {
            steps {
                script {
                    invokeDocker('compose config --quiet')
                }
            }
        }

        stage('Build user-service Docker image') {
            steps {
                script {
                    invokeDocker('compose build user-service')
                }
            }
        }

        stage('Build booking-service Docker image') {
            steps {
                script {
                    invokeDocker('compose build booking-service')
                }
            }
        }

        stage('Start Docker Compose stack') {
            steps {
                script {
                    invokeDocker('compose up -d')
                }
            }
        }

        stage('Verify services and APIs') {
            steps {
                powershell '''
                    $ErrorActionPreference = 'Stop'
                    $scriptPath = Join-Path (Get-Location) 'start-riderrent.ps1'
                    & $scriptPath -SkipComposeStart
                    if ($LASTEXITCODE -ne 0) {
                        throw "RideRent-New verification failed with exit code $LASTEXITCODE."
                    }
                '''
            }
        }
    }

    post {
        success {
            echo 'RideRent-New Jenkins pipeline completed successfully.'
        }
        failure {
            echo 'RideRent-New Jenkins pipeline failed. Inspect the stage output and Compose logs.'
        }
        always {
            script {
                if (getContext(hudson.FilePath) != null) {
                    invokeDocker('compose down --remove-orphans', false)
                } else {
                    echo 'Skipping Docker Compose cleanup because the Jenkins workspace is no longer available.'
                }
            }
        }
    }
}
