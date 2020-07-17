#!groovy
def call(Map stageParams) {
    //Parser Configuration Received from Pipeline
    
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
        stage('echovars'){
            steps{
                script{
                    echo stageParams.RUN_PRE_BUILD
                    echo stageParams.RUN_POST_BUILD
                    echo stageParams.RUN_COMPILE
                    echo stageParams.RUN_CHECKS
                    echo stageParams.S3_BUCKET_ARTIFACT
                    echo stageParams.S3_BUCKET_TEMPLATE
                    echo stageParams.build_name
                    echo stageParams.arquitetura
                    echo stageParams.jobid
                    echo stageParams.folder
                }
            }
        }
        stage('callVarsEcho'){
            steps{
                buid job: echomap('start')
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
                    if (stageParams.RUN_CHECKS == 'true'){
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
                script{python.build(stageParams.build_name)}
            }
        }
        stage('Pre-Build'){
            steps{
                script{
                    echo "entrou aqui"
                    if (stageParams.RUN_PRE_BUILD == 'true'){
                        Map map = [:]
                        echo "criou o mapa"
                        map = git.prebuild()
                        newVersion = map.newVersion
                        RUN_DEPLOY = map.RUN_DEPLOY
                    }
                    else{
                        echo "pulou Pre build"
                    }    
                }
            }
        }
        stage('sam'){
            steps {
                script {
                    if (stageParams.sam == 'true'){
                        cf.sam(stageParams.S3_BUCKET_ARTIFACT)
                    }
                }
            }
        }
        stage('upload cloudformation templates and parameter files'){
            steps {
                script{
                    if (stageParams.RUN_PRE_BUILD == 'true'){
                        cf.uploadTemplate(stageParams.S3_BUCKET_TEMPLATE,newVersion,stageParams.folder)
                        //cf.uploadParameter(stageParams.S3_BUCKET_TEMPLATE,newVersion,stageParams.folder)
                    } else {
                        echo "pulou upload"
                    }
                }
            }
        }
        stage('Deploy environment'){
            steps{
                script{
                    echo "run deploy"
                    echo RUN_DEPLOY
                    if (RUN_DEPLOY == 'true'){
                        rundeck.rundeck(stageParams.jobid,stageParams.arquitetura,newVersion,stageParams.folder)
                    }
                }
            }
        }
    }
        post {
            success {
                script{
                    notify.notifyBuild('SUCCESSFUL',stageParams.channel,newVersion,stageParams.folder)
                    echo "success"
                }
            }
            failure {
                script{
                    //notify.notifyBuild('FAILED')
                    //echo "failure"
                    buid job: echomap('start')
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
