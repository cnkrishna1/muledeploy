# muledeploy
MuleSoft deployment with arm rest commands and binary source promotion. 
#Runtime Manager is the Anypoint Platform tool used to deploy and manage all of your Mule applications from one central location, whether your apps are running in the cloud or on-premises.


Setup a Global pipeline library with in Jenkins and add these files to global repo where the pipeline library. 
Put the Jenkins file in the project repo, and setup the project as pipeline. 

Here is the artifcat is build as Project-name-1.0.${BUILD_NUMBER}-RELEASE.zip

Update the pom.xml with following code
      <groupId>Enter your group ID</groupId>
+        <artifactId>Enter your artifact id</artifactId>
+	      <version>${app.build_version_nbr}</version>
+	        <packaging>mule</packaging>

refer to this link 
https://tedvinke.blog/2013/04/20/using-jenkins-build-version-with-maven/




