#!groovy
package libs.python;

def void rundeck(String jobid, String arquitetura, String newVersion = '0.0.0', String = 'default') {
    script {
        step([$class: "RundeckNotifier",
            includeRundeckLogs: true,
            jobId: jobid,
            nodeFilters: "",
            options: """
                    Arquitetura=${arquitetura}
                    template=cloudformation.yml
                    version=${newVersion}
                    path=${path}
                    """,
            rundeckInstance: "rundeck.devtools.caradhras.io",
            shouldFailTheBuild: true,
            shouldWaitForRundeckJob: true,
            tags: "",
            tailLog: true])
    }
}