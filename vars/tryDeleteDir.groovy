def call() {
    try {
        deleteDir()
    } catch(err) {
        // because windows SUCKS
        echo "Unable to delete dir: ${err}"
    }
}