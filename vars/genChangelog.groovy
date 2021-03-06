def call() {
    def changelog = StringBuilder.newInstance()
    echo "----=-=-=-=-=-=-=-=-=-==-=-=-=-=-=-=-=="
    def changeLogSets = currentBuild.changeSets
    for (int i = 0; i < changeLogSets.size(); i++) {
        def entries = changeLogSets[i].items
        for (int j = 0; j < entries.length; j++) {
            def entry = entries[j]
            echo "${entry.commitId} by ${entry.author} on ${new Date(entry.timestamp)}: ${entry.msg}"
            changelog << "${entry.commitId} by ${entry.author} on ${new Date(entry.timestamp)}: ${entry.msg}"
            def files = new ArrayList(entry.affectedFiles)
            for (int k = 0; k < files.size(); k++) {
                def file = files[k]
                echo "  ${file.editType.name} ${file.path}"
                changelog << "  ${file.editType.name} ${file.path}"
            }
        }
    }
    echo "_+_+_+_+_+_+_+: ${changelog}"
    return changelog.toString()
}