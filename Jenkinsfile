pipeline {
  agent any
  environment {
    TESTCONTAINERS_RYUK_DISABLED = 'true'
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Backend Verify') {
      steps {
        dir('backend') {
          sh 'chmod +x stock-service/mvnw'
          sh './stock-service/mvnw -f pom.xml verify -B --no-transfer-progress -Dsurefire.excludes=**/*IT.java,**/*ApplicationTests.java'
        }
      }
    }
  }
}
