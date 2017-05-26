def call(String tag) {
    def clog = StringBuilder.newInstance()
//    clog << "<html><body><ul>"
    try {
        bat "git rev-list ${tag}"
        range="${tag}..HEAD "
        echo "${tag} tag not found"
    } catch (err) {
        range=""
        echo "${tag} tag found"
    }
    gitLog = bat "git log ${range}--pretty=format:\"%%h %%s\""
    clog << gitLog.toString()
//    clog << "</ul></body></html>"
    return clog.toString()
}
