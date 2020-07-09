#!groovy
package libs.python;

def void build(String build_name) {
    script{
        sh 'python3 -m pip install --target . -r requirements.txt'
        sh "zip -r ${build_name}.zip ."
    } 
}