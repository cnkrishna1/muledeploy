@Grapes(
        @Grab(group = 'org.apache.httpcomponents', module = 'httpclient', version = '4.5.3')
)
import org.apache.http.HttpEntity
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.RequestBuilder
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils

def call(String url, Map<String, String> headers = null, long timeout = (5*60*1000)) {
    def content = null;
    CloseableHttpClient client = HttpClients.createDefault()
    def reqBuilder = RequestBuilder.get().setUri(url)
    if (headers != null) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            reqBuilder.setHeader(entry.getKey(), entry.getValue())
        }
    }
    
    def startTime = System.currentTimeMillis()
    def endTime = startTime + timeout
    def sleepTime = 1;  // 1 second    
    //while (timeout != 0 && endTime > System.currentTimeMillis())  {
      def request = reqBuilder.build()
      CloseableHttpResponse response = client.execute(request)
      def statusCode = response.getStatusLine().getStatusCode()
      if (statusCode > 299) {
        //if (timeout > 0 && endTime < System.currentTimeMillis()) {
        //  println("${url}: Got status code " + statusCode)
        //  sleep(sleepTime);
        //} else {
          throw new IOException("Got status code " + statusCode)
        //}
        //else
          //break;
      } else {
        content = null
        try {
            HttpEntity entity = response.getEntity()
            if (entity != null) {
                content = EntityUtils.toString(entity)
            }
        } finally {
            response.close()
        }
        //if (content != null && content != "")
          //break;
      }
    //}
    
    return content;
}
