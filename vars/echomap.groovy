#!groovy
def call(String stageParams) {
    
pipeline {
   agent any

   stages {
      stage('Hello') {
         steps {
            echo 'ls stageParams'
            echo stageParams
         }
      }
   }
}



}