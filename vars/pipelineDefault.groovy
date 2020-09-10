#!groovy
def call(Map stageParams) {
    //Parser Configuration Received from Pipeline
    
    //Instanciate Objects from Libs
    def git = new libs.git.git()
    def notify = new libs.notify.slack()
    def rundeck = new libs.deploy.rundeck()
    def python = new libs.python.python()
    def resource = libraryResource 'globalvars/vars.yaml'
    def resourceVars = readYaml text: resource

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
                    RUN_DEPLOY_PRD=""
                    RUN_DEPLOY_HML=""
                    RUN_DEPLOY_DEV=""
                    RUN_HOTFIX=""
                    environment=""
                    jobId=""
                    reponame=""
                    newVersion=""
                    current_commit_message=""
  
                    env.build_name=stageParams.build_name
         
                }
            }
        }
        stage('callVarsEcho'){
            steps{
                script{ 
                        echo "resource completo"
                        echo resource
                        echo "id vindo de resource"
                        echo resourceVars.id
                        echo "lint vindo do resource"
                        echo resourceVars.lint
                        echo "name vindo do resource"
                        echo resourceVars.name
                        echo resourceVars.jobid
                        echo resourceVars.branch
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
            always {
                script{
                    if (stageParams.RUN_PRE_BUILD == true){
                    build job: 'Deploy', wait: false, parameters: [
                            [ $class: 'StringParameterValue', name: 'web', value: "edson" ],
                            [ $class: 'StringParameterValue', name: 'buidname', value: "luis" ],
                        ]
                    }
                }
                deleteDir()
            }
        }
  
    }
}
