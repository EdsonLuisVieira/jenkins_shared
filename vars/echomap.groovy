#!groovy
def call(String stageParams, String stageParams1) {
    
pipeline {
   agent any

   stages {
      stage('Hello') {
         steps {
            echo 'ls stageParams'
            echo stageParams
            echo stageParams1
            echo "chama job"
         }
      }
   }
}



}
