def call( args ) {
    withMaven( maven: 'Petco AEM Maven', mavenSettingsConfig: 'default-mvn-settings') {
//        bat "mvn -Dmaven.test.failure.ignore -U ${args}"  // DO NOT IGNORE TEST RELSULTS
        bat "mvn -U ${args}"
    }
}
