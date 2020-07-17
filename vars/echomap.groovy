#!groovy
def call(String stageParams) {
    echo stageParams
    
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