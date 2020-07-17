#!groovy
package libs.notify
import net.sf.json.JSONArray
import net.sf.json.JSONObject


def void notifyBuild(String buildStatus = 'STARTED', String channel = 'jenkins' , String newVersion = '0.0.0', String folder = 'default') {
    buildStatus = buildStatus ?: 'SUCCESSFUL'
    String colorCode = '#FF0000'
    String subject = "${buildStatus}: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'"
    String summary = "${subject} \n (${env.BUILD_URL})  "
    String details = """<p>${buildStatus}: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':</p>
    <p>Check console output at &QUOT;<a href='${env.BUILD_URL}'>${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>&QUOT;</p>"""
    JSONArray attachments = new JSONArray();
    JSONObject attachment = new JSONObject();
    if (buildStatus == 'STARTED') {
        colorCode = '#FFFF00'
        attachment.put('text','Iniciando build.')
        attachment.put('thumb_url','https://1.bp.blogspot.com/-Ap--uqsD9s8/TTeHtGLRMkI/AAAAAAAAACA/Uz5g23u3kFE/s200/chaves2.jpg')
    } else if (buildStatus == 'SUCCESSFUL') {
        colorCode = '#00FF00'
        attachment.put('text','Build finalizado.')
        attachment.put('thumb_url','https://i.pinimg.com/236x/02/54/8d/02548dfa7d2604b5be3f0db4f60de6b4.jpg')

        JSONArray fields = new JSONArray();
        JSONObject field = new JSONObject();

        field.put('title', 'Template S3');
        field.put('value', 'cloudformation.yml');
        fields.add(field);

        field = new JSONObject();

        field.put('title', 'Version');
        field.put('value', newVersion);
        fields.add(field);

        field.put('title', 'Path');
        field.put('value', folder);
        fields.add(field);

        attachment.put('fields',fields);

    } else if (buildStatus == 'ABORTED') {
        attachment.put('text','Abortado')
        attachment.put('thumb_url','https://i.pinimg.com/236x/d5/59/d7/d559d7ce261c58ea5f41084293e4b194.jpg')
        colorCode = '#FF0000'
    } else {
        attachment.put('text','Erro no build.')
        attachment.put('thumb_url','https://i.pinimg.com/236x/d5/59/d7/d559d7ce261c58ea5f41084293e4b194.jpg')
        colorCode = '#FF0000'
    }

    String buildUrl = "${env.BUILD_URL}";
    attachment.put('title', subject);
    attachment.put('callback_id', buildUrl);
    attachment.put('title_link', buildUrl);
    attachment.put('fallback', subject);
    attachment.put('color', colorCode);

    attachments.add(attachment);
    echo attachments.toString();
    slackSend(channel: channel,attachments: attachments.toString())


}