@Grapes([
        @Grab(group = 'org.apache.httpcomponents', module = 'httpclient', version = '4.5.3'),
        @Grab(group = 'org.apache.httpcomponents', module = 'httpmime', version = '4.5.3')
])
import org.apache.http.HttpEntity
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.RequestBuilder
import org.apache.http.entity.mime.FormBodyPart
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.ContentBody
import org.apache.http.entity.mime.content.FileBody
import org.apache.http.entity.mime.content.InputStreamBody
import org.apache.http.entity.mime.content.StringBody
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils

def call(String url, Map<String, String> text = null, Map<String, File> binaries = null,
         Map<String, String> headers = null) {
    CloseableHttpClient client = HttpClients.createDefault()
    MultipartEntityBuilder mpeBuild = MultipartEntityBuilder.create()
    if (text != null) {
        for (Map.Entry<String, String> entry : text.entrySet()) {
            String name = entry.getKey()
            String value = entry.getValue()
            mpeBuild.addPart(new FormBodyPart(name, new StringBody(value)))
        }
    }
    if( binaries != null ) {
        for (Map.Entry<String, File> entry : binaries.entrySet()) {
            String name = entry.getKey()
            ContentBody body = new FileBody(entry.getValue())
            mpeBuild.addPart(new FormBodyPart(name, body))
        }
    }
    RequestBuilder reqBuilder = RequestBuilder.post().setUri(url)
    if (headers != null) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            reqBuilder.setHeader(entry.getKey(), entry.getValue())
        }
    }
    reqBuilder.setEntity(mpeBuild.build())
    CloseableHttpResponse response = client.execute(reqBuilder.build())
    def statusCode = response.getStatusLine().getStatusCode()
    if (statusCode > 299) {
        if( response.getEntity() != null ) {
            def body = EntityUtils.toString(response.getEntity())
            throw new IOException("Got response ${response.getStatusLine()} : ${body}")
        } else {
            throw new IOException("Got response ${response.getStatusLine()}")
        }
    } else {
        try {
            HttpEntity entity = response.getEntity()
            if (entity != null) {
                long len = entity.getContentLength()
                if (len != -1) {
                    return EntityUtils.toString(entity)
                }
            }
        } finally {
            response.close()
        }
        return null
    }
}
