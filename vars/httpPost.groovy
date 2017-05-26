@Grapes(
        @Grab(group = 'org.apache.httpcomponents', module = 'httpclient', version = '4.5.3')
)
import org.apache.http.HttpEntity
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.RequestBuilder
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils

def call(String url, String data, Map<String, String> headers = null) {
    CloseableHttpClient client = HttpClients.createDefault()
    def reqBuilder = RequestBuilder.post().setUri(url).setEntity(new StringEntity(data))
    if (headers != null) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            reqBuilder.setHeader(entry.getKey(), entry.getValue())
        }
    }
    def request = reqBuilder.build()
    CloseableHttpResponse response = client.execute(request)
    def statusCode = response.getStatusLine().getStatusCode()
    if (statusCode > 299) {
        throw new IOException("Got status code " + statusCode)
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
