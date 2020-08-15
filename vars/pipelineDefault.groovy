#!groovy
def call(Map stageParams) {
    //Parser Configuration Received from Pipeline
    
    //Instanciate Objects from Libs
    def git = new libs.git.git()
    def notify = new libs.notify.slack()
    def rundeck = new libs.deploy.rundeck()
    def cf = new libs.cf.cf()
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
                    current_commit_message=""
                    echo stageParams.RUN_POST_BUILD
                    echo stageParams.RUN_COMPILE
                    echo stageParams.RUN_CHECKS
                    echo stageParams.S3_BUCKET_ARTIFACT
                    echo stageParams.S3_BUCKET_TEMPLATE
                    env.build_name=stageParams.build_name
                    echo stageParams.arquitetura
                    echo stageParams.jobid
                    echo stageParams.folder
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
                            [ $class: 'StringParameterValue', name: 'RUN_DEPLOY_PRD', value: "${RUN_DEPLOY_PRD}" ],
                            [ $class: 'StringParameterValue', name: 'RUN_DEPLOY_HML', value: "${RUN_DEPLOY_HML}" ],
                            [ $class: 'StringParameterValue', name: 'RUN_DEPLOY_DEV', value: "${RUN_DEPLOY_DEV}" ],
                            [ $class: 'StringParameterValue', name: 'RUN_HOTFIX', value: "${RUN_HOTFIX}" ],
                            [ $class: 'StringParameterValue', name: 'RUN_CI', value: "${stageParams.RUN_CI}" ],
                            [ $class: 'StringParameterValue', name: 'cloudformation', value: "${stageParams.CLOUDFORMATION}" ],
                            [ $class: 'StringParameterValue', name: 'kubernetes', value: "${stageParams.KUBERNETES}" ],
                            [ $class: 'StringParameterValue', name: 'architecture', value: "${stageParams.ARCHITECTURE}" ],
                            [ $class: 'StringParameterValue', name: 'path_deploy', value: "${stageParams.PATH_DEPLOY}" ],
                            [ $class: 'StringParameterValue', name: 'templateFile', value: "${stageParams.TEMPLATEFILE}" ],
                            [ $class: 'StringParameterValue', name: 'environment', value: "${environment}" ],
                            [ $class: 'StringParameterValue', name: 'newVersion', value: "${newVersion}" ],
                            [ $class: 'StringParameterValue', name: 'jobId', value: "${jobId}" ],
                            [ $class: 'StringParameterValue', name: 'reponame', value: "${reponame}" ],
                            [ $class: 'StringParameterValue', name: 'channel', value: "${stageParams.CHANNEL}" ],
                            [ $class: 'StringParameterValue', name: 'cluster', value: "${resourceVars.CLUSTER_NAME}" ],
                            [ $class: 'StringParameterValue', name: 'namespace', value: "${stageParams.NAMESPACE}" ],
                            [ $class: 'StringParameterValue', name: 'current_commit_message', value: "${current_commit_message}" ],
                            [ $class: 'StringParameterValue', name: 'cucumber_docker_image', value: "${stageParams.CUCUMBER_DOCKER_IMAGE}" ],
                            [ $class: 'StringParameterValue', name: 'config_vars', value: "${stageParams.CUCUMBER_CONFIG_VARS}" ],
                            [ $class: 'StringParameterValue', name: 'cucumber_tag', value: "${stageParams.CUCUMBER_TAG}" ],
                            [ $class: 'StringParameterValue', name: 'cucumber_job', value: "${stageParams.CUCUMBER_JOB}" ],
                        ]
                    }
                }
                deleteDir()
            }
        }
  
    }
}
