#!groovy

import hudson.model.*
import hudson.EnvVars
import groovy.json.JsonSlurperClassic
import groovy.json.JsonBuilder
import groovy.json.JsonOutput
import jenkins.util.*;
import jenkins.model.*;
import java.net.URL

//def call(String devBranch="integration",String pipelineBranch="develop", String appVersion="1.0"){
def call(String apiName, String apiVersion="v1", String appVersion="1.0") {
  node {
       def ARCHIVE_DIR = "D:\\ArchivedArtifact";
	   //archiving the artifact to the local dir, please change it accordingly 
       def TARGET_ENV= "";
       try {
         TARGET_ENV = "$DEPLOY_ENV";
         echo "DEPLOY_ENV = $DEPLOY_ENV"
       } catch (groovy.lang.MissingPropertyException e) {
         // ignore
       }
       if (TARGET_ENV == null || TARGET_ENV == "") {
          TARGET_ENV = "int";
       }

       def JOB_NAME = "$JOB_NAME";
       def ARTIFACT_JOB_NAME= "";
       def PIPELINE_DEPLOY_STR = "pipeline-deploy";
       if (JOB_NAME.endsWith(PIPELINE_DEPLOY_STR)) {
          ARTIFACT_JOB_NAME = JOB_NAME.substring(0, JOB_NAME.length()-(PIPELINE_DEPLOY_STR.length()+1));
       } else {
          ARTIFACT_JOB_NAME = JOB_NAME;
       }
       
       def APP_NAME = "";
       def MULE_PFX = "mule-";
       if (ARTIFACT_JOB_NAME.startsWith(MULE_PFX)) {
         APP_NAME = ARTIFACT_JOB_NAME.substring(MULE_PFX.length());
       }
       
       if (APP_NAME == null || APP_NAME == "") {
         echo "Getting APP_NAME from POM" 
         pom = readMavenPom file: 'pom.xml'
         APP_NAME = pom.artifactId
       }
                
       def BUILD_NBR= "";
       def RELEASE_BUILD_NBR = "";
       try {
         BUILD_NBR = "$BUILD_NUMBER";
       } catch (groovy.lang.MissingPropertyException e) {
         // ignore
       }

       def RELEASE_BUILD_PKG = "";
       try {
         RELEASE_BUILD_PKG = "$RELEASE_BUILD_PACKAGE";
         echo "RELEASE_BUILD_PACKAGE = " + RELEASE_BUILD_PKG;
       } catch (groovy.lang.MissingPropertyException e) {
         // ignore
       }
       if (RELEASE_BUILD_PKG != null && RELEASE_BUILD_PKG != "") { 
         if (RELEASE_BUILD_PKG.endsWith("/")) {
           def releasePkg = RELEASE_BUILD_PKG.substring(0,(RELEASE_BUILD_PKG.length()-1));
           int lastSlash = releasePkg.lastIndexOf("/");
           if (lastSlash > 0 && lastSlash < releasePkg.length()) {
             RELEASE_BUILD_NBR = releasePkg.substring(lastSlash+1);
           }
         }
       }

       if (RELEASE_BUILD_NBR == null || RELEASE_BUILD_NBR == "") {
            RELEASE_BUILD_NBR = BUILD_NBR;
       }

       def BUILD_NBR_TAG = "${RELEASE_BUILD_NBR}-RELEASE";
       def BUILD_NBR_VERSION = "${appVersion}.${BUILD_NBR_TAG}";

       echo "Deploying ${ARTIFACT_JOB_NAME} # ${RELEASE_BUILD_NBR} [${BUILD_NBR_VERSION}] to ${TARGET_ENV}";
       
        def ARCHIVE_PATH = ARCHIVE_DIR + "\\" + ARTIFACT_JOB_NAME + "-" + BUILD_NBR_VERSION;
        def ARCHIVE_LOCATION = ARCHIVE_PATH + "\\target\\" + APP_NAME + "-" + BUILD_NBR_VERSION + ".zip";
        def JMETER_TEST_SCRIPT_NAME = "functional-tests.jmx";
        def TEST_SCRIPT = ARCHIVE_PATH + "\\src\\test\\jmeter\\" + JMETER_TEST_SCRIPT_NAME;
        
	/*******************************
	*  Checkout code from GIT 
	********************************/
	
     if ((TARGET_ENV == 'int') && (RELEASE_BUILD_PKG == null || RELEASE_BUILD_PKG == "")) {

        def GIT_URL = 'https://bitbucket.org.git'
        def GIT_BRANCH = '*/develop'
        def MAVEN_GOALS = 'mvn clean -X dependency:tree package -Dbuild.nbr=' +  BUILD_NBR_TAG;
   
		stage('\u2776 Git Checkout Code, Clean + Package & Archive Artifact') 
        
		echo "INFO => Checking out from URL: ${GIT_URL} and BRANCH: ${GIT_BRANCH}"
		checkout([$class: 'GitSCM', branches: [[name: "${GIT_BRANCH}"]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'RelativeTargetDirectory', extensions: [[$class: 'CleanBeforeCheckout']], relativeTargetDir: 'D:\\workspace\\${JOB_NAME}']], submoduleCfg: [], userRemoteConfigs: [[url: "${GIT_URL}"]]])
//		checkout([$class: 'GitSCM', branches: [[name: "${GIT_BRANCH"}]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'RelativeTargetDirectory', extensions: [[$class: 'CleanBeforeCheckout']], relativeTargetDir: 'D:\\workspace\\${JOB_NAME}']], submoduleCfg: [], userRemoteConfigs: [[url: "${GIT_URL}"]]])

     /************************
       * Build stage to trigger the maven build
       **************************/
   
		     echo "INFO => MAVEN_HOME: ${MAVEN_HOME}"
		     echo "INFO => JAVA_HOME : ${JAVA_HOME}"
		     bat "cd D:\\workspace\\${JOB_NAME} && ${MAVEN_GOALS}"
             
             bat "unzip -v -t target\\*zip"
             bat "type target\\classes\\ping.json"

     /*****************************************************
       * Archive Artifact
      ************************************************************/
 	
            fingerprint 'target\\*.zip'
                
	     	stash( name: "archive", includes: 'target\\*.zip,src\\test\\jmeter\\*.jmx' )
            stash( name: "jmeter", includes: 'src\\test\\jmeter\\*.jmx' )
            archiveArtifacts 'target\\*.zip'

            echo "Archiving to ${ARCHIVE_PATH}"
            bat "mkdir ${ARCHIVE_PATH}"
            bat "cd ${ARCHIVE_PATH}"
            dir (ARCHIVE_PATH) {
              unstash(name: "archive")
              unstash(name: "jmeter")
            }
		}

        bat "unzip -v -t ${ARCHIVE_LOCATION}"

        /*************************************************
       * now deploy on target env
        ****************************************************/
       mulePipelineDeploy(APP_NAME, appVersion, apiName, apiVersion, TARGET_ENV, BUILD_NBR_VERSION, ARCHIVE_LOCATION, TEST_SCRIPT)  
  }
}
