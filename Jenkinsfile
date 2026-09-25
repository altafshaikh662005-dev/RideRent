pipeline {
    agent any

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
                bat 'mvn.cmd clean test package'
            }
        }

        stage('Validate docker-compose.yml') {
            steps {
                bat 'docker compose config --quiet'
            }
        }

        stage('Build user-service Docker image') {
            steps {
                bat 'docker compose build user-service'
            }
        }

        stage('Build booking-service Docker image') {
            steps {
                bat 'docker compose build booking-service'
            }
        }

        stage('Start Docker Compose stack') {
            steps {
                bat 'docker compose up -d'
            }
        }

        stage('Verify services and APIs') {
            steps {
                powershell '''
                    $ErrorActionPreference = 'Stop'
                    & './start-riderrent.ps1' -SkipComposeStart
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
            bat 'docker compose down --remove-orphans'
        }
    }
}
