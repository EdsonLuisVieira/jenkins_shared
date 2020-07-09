#!groovy
package libs.cf;

def void sam(String S3_BUCKET_ARTIFACT){
    script{
        sh "aws cloudformation package --template-file cloudformation/template/cloudformation.yml --s3-bucket ${S3_BUCKET_ARTIFACT} --output-template-file cloudformation/template/cloudformation.yml"
}

def void uploadTemplate(String S3_BUCKET_TEMPLATE, String newVersion) {
    script{
        echo "upload template to s3://${S3_BUCKET_TEMPLATE}/dynamodb-backups/${newVersion}/templates/"
        sh "aws s3 cp cloudformation/template/cloudformation.yml s3://${S3_BUCKET_TEMPLATE}/dynamodb-backups/${newVersion}/templates/"
    }
}

def void uploadParameter(String S3_BUCKET_TEMPLATE, String newVersion){
    script{
        echo "upload parameter files to s3://${env.S3_BUCKET_TEMPLATE}/dynamodb-backups/${newVersion}/parameters/"
        sh "aws s3 sync cloudformation/parameters/ s3://${env.S3_BUCKET_TEMPLATE}/dynamodb-backups/${newVersion}/parameters/"
    }
}