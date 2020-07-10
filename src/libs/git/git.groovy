#!groovy
package libs.git;

def void commitMessage(){
    script {
        current_commit_message = sh(script: '''
             git rev-list --format=%B --max-count=1 HEAD |head -2 |tail -1
        ''', returnStdout: true).trim()

        if (current_commit_message == 'Prepare for next Release') {
            currentBuild.result = 'ABORTED'
            error('Parando build por ser um commit de CI.')
        }
    }
}

def void checkbranchName(){
    script {
        if ( GIT_BRANCH == ("origin/v2")) {
            echo "***** Let's go to the Build *****"

        } else {
            currentBuild.result = 'ABORTED'
            error('Parando o build por não estar de acordo com a nomenclatura de Branch.')
        }
    }
}

def void checkCommitBehind() {
    sh 'echo "Verifica se branch necessita de merge com master."'
    script {
        sh(script: '''set +x; set +e;
                      git fetch;
                      commitsBehind=$(git rev-list --left-right --count origin/master... |awk '{print $1}');
                      if [ ${commitsBehind} -ne 0 ]
                      then
                        echo "Esta branch está ${commitsBehind} commits atrás da master!"
                        exit 1
                      else
                        echo "Esta branch não tem commits atrás da master."
                      fi''')
    }

}

def String updateVersion(boolean isMaster){
    bumpci_tag=version_code_tag()
    def oldVersion = "${bumpci_tag}".tokenize('.')
    major = oldVersion[0].toInteger()
    minor = oldVersion[1].toInteger()
    patch = oldVersion[2].toInteger()

    if(isMaster){
      minor += 1
      patch = 0
    }else{
      patch += 1
    }
    newVersion = major + '.' + minor + '.' + patch

    bump_version_tag(newVersion)
    return newVersion
}

def String version_code_tag() {
    echo "getting Git version Tag"
    script {
        sh "git fetch --tags"
        bumpci_tag = sh(script: '''
            current_tag=`git tag -n9 -l |grep version |awk '{print $1}' |sort -V |tail -1`
            if [ -z $current_tag ]; then
                current_tag=0.0.1
            fi
            echo ${current_tag}
            ''', returnStdout: true).trim()
        return bumpci_tag
    }
  }

def void bump_version_tag(String newVersion) {
  echo "Bumping version CI Tag"
  script {
      sh "git tag -a ${newVersion} -m version && git push origin refs/tags/${newVersion}"
  }
}

def Map prebuild() {
      script {
          echo "entrou na funcao"
          def Map resultPreBuild =[:]
          REPO_NAME_STACK = sh(script: '''
                                     git remote show -n origin | grep Fetch | sed -r 's,.*:(.*).git,\\1,' |tr -d '\n'
                                     ''', returnStdout: true).trim()
          stack_name = sh(script: '''
                               echo $(git remote -v |grep fetch |sed -r 's,.*\\.com:[^/]*/(.*)\\.git.*,\\1,')
                               ''', returnStdout: true).trim()
          if (GIT_BRANCH == ("origin/v2")) {
              echo "***** PERFORMING STEPS ON MASTER *****"
              resultPreBuild["newVersion"] = updateVersion(true)
              resultPreBuild["RUN_DEPLOY"] = "false"

          }
          if (GIT_BRANCH == ("origin/develop")) {
              echo "***** PERFORMING STEPS ON MASTER *****"
              resultPreBuild["newVersion"] = updateVersion(false)
              resultPreBuild["RUN_DEPLOY"] = "true"
          }
          echo "***** FINISHED PRE-BUILD STEP *****"
          return resultPreBuild
      }
}


return this
