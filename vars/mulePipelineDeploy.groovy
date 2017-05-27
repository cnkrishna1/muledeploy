def call(appName, appVersion="1.0", apiName, apiVersion="v1", menv="int", buildNbrVersion, archiveLocation, testScript) {

    def PING_URL = "http://api-${menv}.org.com/${apiName}/${apiVersion}/ping";
    def mule_client_id = "enter client id ";
    def mule_client_secret = "enter the client secret";
    Map<String, String> headers = new HashMap();
    headers.put("client_id", mule_client_id);
    headers.put("client_secret",  mule_client_secret);
    headers.put("mule_client_id",  mule_client_id);
    headers.put("mule_client_secret",  mule_client_secret);
    headers.put("Content-Type", "application/json");
    headers.put("accept", "application/json");

   boolean replacingSameVersion = false;

stage("\u2776 Deploy ${menv}") {

   try {
        def content = httpGet(PING_URL, headers, 0L);
        if (content != null && content != "") {
          if (content.contains("\"build\"") && content.contains(buildNbrVersion)) {
            replacingSameVersion = true;
          }
          println("Existing Version: " + content + ", replacingSameVersion = " + replacingSameVersion);
	    }
      } catch (Exception e) {
        // ignore
      }
  
       node {
           try {
                if (appVersion == null || appVersion == "")
                  appVersion = "1.0"
                def targetEnv = "Int";
                if (menv == "dev")
                  targetEnv = "Dev";
                else if (menv == "sit")
                  targetEnv = "Sit";
                else if (menv == "prf")
                  targetEnv = "Prf";
                else if (menv == "prd")
                  targetEnv = "Prf";
                withCredentials([usernamePassword(credentialsId: 'Anypoint-IntegrationCOE', passwordVariable: 'ANYPOINT_PASSWORD', usernameVariable: 'ANYPOINT_USERNAME')]) {
                    armDeploy(ANYPOINT_USERNAME,ANYPOINT_PASSWORD,"Integration Group",targetEnv,"${targetEnv}Group-1","petco-${menv.toLowerCase()}-${appName}",archiveLocation)
               }
           } finally {
               step([$class: 'Mailer', notifyEveryUnstableBuild: false, recipients: '', sendToIndividuals: true])
            }
       }
    }

    long startTime = System.currentTimeMillis();
    long endTime = startTime + (10*60*1000); // wait up to 10 minutes

    def successful = false;
    
    while (!successful) {
      try {
        println("Pinging " + PING_URL + " for client_id = " + mule_client_id);
	    String content = httpGet(PING_URL, headers, 0L);
        if (content != null && content != "") {
          println("Ping " + PING_URL + ": " + content + "; versionBuildNbr = " + buildNbrVersion);
  	      if (content.contains("\"status\":\"200\"")) {
            if (content.contains("\"build\"") && content.contains(buildNbrVersion)) {
              successful = true;
              println("API Installation Confirmed: " + content);
            }
          }
	    }
      } catch (Exception e) {
        println("Exception: " + e.getMessage());
      }

      if (successful || endTime <= System.currentTimeMillis()) {
        break;
      } else {
        println("Waiting ...");
        sleep(30) 
      }
   }
   
   if (successful) {
      println("Preparing to test ...");
      sleep(replacingSameVersion ? 120 : 15) 
      stage("\u2776 Test ${menv}") {
        node {
           try {
                bat "del target\\testResult.jtl"
           } catch (Throwable t) {
             // ignore any errors attempting to delete old test file
           }
           try {
                bat "C:\\apache-jmeter-3.1\\bin\\jmeter.bat -Jjmeter.save.saveservice.output_format=xml -n -t ${testScript} -l target\\testResult.jtl -Japi.host=api-${menv}.petc.com"
                step([$class: 'XUnitPublisher', testTimeMargin: '4000', thresholdMode: 1, thresholds: [[$class: 'FailedThreshold', failureNewThreshold: '0', failureThreshold: '0', unstableNewThreshold: '0', unstableThreshold: '0'], [$class: 'SkippedThreshold', failureNewThreshold: '0', failureThreshold: '0', unstableNewThreshold: '0', unstableThreshold: '0']], tools: [[$class: 'CustomType', customXSL: 'D:\\workspace\\MULESOFT\\jmeter-to-xunit.xsl', deleteOutputFiles: true, failIfNotNew: false, pattern: 'target\\**\\testResult.jtl', skipNoTestFiles: true, stopProcessingIfError: false]]])
            } finally {
                step([$class: 'Mailer', notifyEveryUnstableBuild: false, recipients: '', sendToIndividuals: true])
           }
        }
      }
    } else {
      println("Failed validating ping for " + surl);
      currentBuild.result = 'FAILURE'
    }
}
