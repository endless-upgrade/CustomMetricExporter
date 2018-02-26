pipeline {
  agent any
  stages {
    stage('Config System') {
      steps {
        echo 'Setup the system'
        echo 'wget, curl, java, sbt and spark are now installed by Config Management system :)'
      }
    }
    stage('Test the System') {
      steps {
        sh 'java -version'
        sh 'sbt about'
      }
    }
    stage('Unit Tests') {
      steps {
        sh 'sbt clean test'
        archiveArtifacts 'target/test-reports/*.xml'
      }
    }
    stage('Build') {
      steps {
        sh 'sbt clean compile package assembly'
        archiveArtifacts 'target/scala-*/*.jar'
      }
    }
    stage('Deploy') {
      steps {
        sh 'sudo cp target/*/*.war /opt/deploy/realTimeMovieRec/'
        sh 'sudo cp target/*/*.jar /opt/deploy/realTimeMovieRec/'
        sh 'sudo cp conf/* /opt/deploy/realTimeMovieRec/'
        sh 'sudo cp target/*/*.jar /opt/staging/IntegrationStagingProject/lib'
      }
    }
    stage('Integration Tests') {
      steps {
        sh 'cd /opt/staging/IntegrationStagingProject/ && sbt clean test'
      }
    }
    stage('Production Deploy') {
      steps {
        echo 'Safe to Deploy in Production, Great Job :D'
      }
    }
  }
}