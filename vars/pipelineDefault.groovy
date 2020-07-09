#!groovy
def call(body) {
    //Parser Configuration Received from Pipeline
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    //Instanciate Objects from Libs
    def git = new libs.git.git()
    def notify = new libs.notify.slack()
    def rundeck = new libs.deploy.rundeck()
    def cf = new libs.cf.cf()
    def python = new libs.python.python()

    pipeline {
        agent any

    options {
        buildDiscarder(logRotator(numToKeepStr: '50'))
        timeout(time: 20, unit: 'MINUTES')
    }

    stages {
        stage('Check commit message') {
            steps{
                script{ git.commitMessage()}
            }
        }
        stage('Check Branch Name'){
            steps{
                script{ git.checkbranchName()}
            }
        }
        stage('notify'){
            steps{
                script{notify.notifyBuild('STARTED')}
            }

        }
        stage('Pre-Build CheckList'){
            steps{
                script{
                    if (RUN_CHECKS){
                      git.checkCommitBehind()  
                    }
                    else{
                        echo "skpes Pre build"
                    }    
                }
            }
        }
        stage('compile'){
            steps{
                script{python.build(build_name)}
            }
        }
        stage('Pre-Build'){
            steps{
                script{
                    if (RUN_PRE_BUILD){
                      newVersion = git.preBuild()  
                    }
                    else{
                        echo "skpes Pre build"
                    }    
                }
            }
        }
        stage('sam'){
            steps {
                script {cf.sam(S3_BUCKET_ARTIFACT)}
            }
        }
        stage('upload cloudformation templates and parameter files'){
            steps {
                script{
                    uploadTemplate(S3_BUCKET_TEMPLATE,newVersion)
                    uploadParameter(S3_BUCKET_TEMPLATE,newVersion)
                }
            }
        }
        stage('Deploy environment'){
            steps{
                script{
                    rundeck.rundeck(jobid,arquitetura,newVersion,path)
                }
            }
        }
    }
        post {
            success {
                notifyBuild('SUCCESSFUL',channel,newVersion,path)
                echo "success"
            }
            failure {
                notifyBuild('FAILED',channel,newVersion,path)
                echo "failure"
            }
            always {
                deleteDir()
            }
        }
  
    }
}