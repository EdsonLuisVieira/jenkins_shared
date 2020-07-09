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
        stage('echo'){
            steps{
                script{
                    echo RUN_PRE_BUILD
                    echo RUN_POST_BUILD
                    echo RUN_COMPILE
                    echo RUN_CHECKS
                    echo S3_BUCKET_ARTIFACT
                    echo S3_BUCKET_TEMPLATE
                    echo build_name
                    echo arquitetura
                    echo jobid
                    echo path
                }
            }
        }
        stage('notify'){
            steps{
                script{notify.notifyBuild('STARTED')}
            }
        }
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
        stage('Pre-Build CheckList'){
            steps{
                script{
                    if (env.RUN_CHECKS){
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
                script{python.build(env.build_name)}
            }
        }
        stage('Pre-Build'){
            steps{
                script{
                    if (env.RUN_PRE_BUILD){
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
                script {cf.sam(env.S3_BUCKET_ARTIFACT)}
            }
        }
        stage('upload cloudformation templates and parameter files'){
            steps {
                script{
                    uploadTemplate(env.S3_BUCKET_TEMPLATE,newVersion)
                    uploadParameter(env.S3_BUCKET_TEMPLATE,newVersion)
                }
            }
        }
        stage('Deploy environment'){
            steps{
                script{
                    rundeck.rundeck(env.jobid,env.arquitetura,newVersion,env.path)
                }
            }
        }
    }
        post {
            success {
                script{
                    notify.notifyBuild('SUCCESSFUL',env.channel,newVersion,env.path)
                    echo "success"
                }
            }
            failure {
                script{
                    notify.notifyBuild('FAILED')
                    echo "failure"
                }
            }
            aborted {
                script{
                    notify.notifyBuild('ABORTED')
                    echo "failure"
                }
            }
            always {
                deleteDir()
            }
        }
  
    }
}