def call(username, password, org, menv) {
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

    // get application info
    def appReqResp = httpGet("https://anypoint.mulesoft.com/hybrid/api/v1/applications",
            ["Authorization":auth,'X-ANYPNT-ORG-ID':orgId,'X-ANYPNT-ENV-ID': envId])
    def appReqJson = readJSON text: appReqResp

    echo "Applications ${org} / ${menv}"
    echo appReqJson
}