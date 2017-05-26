def call(username, password, org, menv, serverName, appName, archive) {
    println "mulePipelineDeploy BEGIN env = ${menv}, serverName=${serverName}, appName=${appName}, archive=${archive}"
    // authenticate
    def authReq = "{\"username\":\"${username}\",\"password\":\"${password}\"}"
    def authResp = httpPost("https://anypoint.mulesoft.com/accounts/login", authReq, ["Content-Type": "application/json"])
    def authJson = readJSON text: authResp
    def auth = authJson['token_type'] + " " + authJson['access_token']
    // get org id
    def userResp = httpGet('https://anypoint.mulesoft.com/accounts/api/me', ["Authorization":auth])
    def userJson = readJSON text: userResp
    def orgJson = null
    for (json in userJson['user']['memberOfOrganizations'] ) {
        if( json['name'].equals(org) ) {
            orgJson = json
            break
        }
    }
    if( orgJson == null ) {
        error("User not in org "+org)
    }
    def orgId = orgJson['id']

    // get env id
    def envReqResp = httpGet("https://anypoint.mulesoft.com/accounts/api/organizations/"+orgId+"/environments",["Authorization":auth])
    def envReqJson = readJSON text: envReqResp
    def envJson = null
    for (json in envReqJson['data'] ) {
        if( json['name'].equals(menv) ) {
            envJson = json
            break;
        }
    }
    if( envJson == null ) {
        error("Environment not found "+menv)
    }
    def envId = envJson['id']

    // get server id
    def servReqResp = httpGet("https://anypoint.mulesoft.com/hybrid/api/v1/serverGroups",
            ["Authorization":auth,'X-ANYPNT-ORG-ID':orgId,'X-ANYPNT-ENV-ID': envId])
    def servReqJson = readJSON text: servReqResp
    def serverJson = null
    for (json in servReqJson['data'] ) {
        if(json['name'] == serverName) {
            serverJson = json
            break
        }
    }
    if( serverJson == null ) {
        error("Server not found: ${serverName}")
    }
    def serverId = serverJson['id']

    // get application id
    def appReqResp = httpGet("https://anypoint.mulesoft.com/hybrid/api/v1/applications",
            ["Authorization":auth,'X-ANYPNT-ORG-ID':orgId,'X-ANYPNT-ENV-ID': envId])
    def appReqJson = readJSON text: appReqResp
    def appId = null
    for (json in appReqJson['data'] ) {
        if(json['name'] == appName) {
            appId = json['id']
            break
        }
    }

    File appFile = new File(archive)
    if( ! appFile.exists() ) {
        error "mule archive not found: ${appFile.getPath()}"
    }
    if( appId != null ) {
        println "Redeploying application: ${appId}"
        httpMultipartPatch("https://anypoint.mulesoft.com/hybrid/api/v1/applications/${appId}",
                ["targetId":serverId,"artifactName":appName],
                ["file":appFile],
                ["Authorization":auth,'X-ANYPNT-ORG-ID':orgId,'X-ANYPNT-ENV-ID': envId]
        )
    } else {
        println "Application not deployed"
        httpMultipartPost("https://anypoint.mulesoft.com/hybrid/api/v1/applications",
                ["targetId":serverId,"artifactName":appName],
                ["file":appFile],
                ["Authorization":auth,'X-ANYPNT-ORG-ID':orgId,'X-ANYPNT-ENV-ID': envId]
        )
    }
    echo "Deployed ${appName} to ${org} / ${menv} / ${serverName}"
}