#!groovy
def call() {
    
pipeline {
   agent any

   stages {
      stage('Hello') {
         steps {
            echo 'ls stageParams'
         }
      }
   }
}



}