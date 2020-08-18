#!groovy
def call(Map stageParams) {
    
pipeline {
   agent any

   stages {
      stage('Hello') {
         steps {
            echo 'ls stageParams'
            echo stageParams.web
            echo stageParams.buidname
            echo "chama job"
         }
      }
   }
}



}
