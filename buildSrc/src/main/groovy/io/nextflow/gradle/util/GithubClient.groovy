package io.nextflow.gradle.util

import com.google.gson.Gson
import groovy.util.logging.Slf4j

/**
 * Commit & push a file change to a Github repo
 *
 * https://stackoverflow.com/a/63461333/395921
 */
@Slf4j
class GithubClient {

    String authToken
    String userName
    String branch
    String repo
    String owner
    String email

    private Gson gson = new Gson()

    private String getEncodedAuthToken() {
        if( !userName )
            throw new IllegalArgumentException("Missing Github userName")
        if( !authToken )
            throw new IllegalArgumentException("Missing Github authToken")
        return "$userName:$authToken".bytes.encodeBase64().toString()
    }

    private HttpURLConnection getHttpConnection(String url) {
        new URL(url).openConnection() as HttpURLConnection
    }

    private sendHttpMessage(String endpoint, String payload, String method = 'POST') {
        if (!endpoint)
            throw new IllegalArgumentException("Missing Github target endpoint")

        def responseCode
        def con = getHttpConnection(endpoint)
        // Make header settings
        con.setRequestMethod(method)
        con.setRequestProperty("Content-Type", "application/json")
        con.setRequestProperty("Authorization","Basic ${getEncodedAuthToken()}")

        con.setDoOutput(true)

        // Send POST request
        if( payload ) {
            DataOutputStream output = new DataOutputStream(con.getOutputStream())
            output.writeBytes(payload)
            output.flush()
            output.close()
        }

        int code
        try {
            code = con.responseCode
            final text = con.getInputStream().text
            log.trace "resp code=$code, text=$text"
            return gson.fromJson(text, Map)
        }
        catch (IOException e) {
            final text = con.getErrorStream().text
            throw new IllegalStateException("Unexpected response code=$code\n- response=$text\n- request=$endpoint\n- payload=$payload")
        }
    }

    /**
     * 1. Get the last commit SHA of a specific branch
     *
     * @return The SHA id of the last commit
     */
    String lastCommitId() {
        def resp = sendHttpMessage("https://api.github.com/repos/$owner/$repo/branches/$branch", null, 'GET')
        return resp.commit.sha
    }

    /**
     *  2. Create the blobs with the files content
     *
     * @param file content
     * @return The SHA id of the uploaded content
     */
    String uploadBlob(String file) {
        def content = "{\"encoding\": \"base64\", \"content\": \"${file.bytes.encodeBase64().toString()}\"}"
        def resp = sendHttpMessage("https://api.github.com/repos/$owner/$repo/git/blobs", content, 'POST')
        return resp.sha
    }

    /**
     * 3. Create a tree which defines the file structure
     *
     * @param fileName The name of the file changed
     * @param blobId The id of the changed content
     * @param lastCommitId The last commit id
     * @return The SHA id of the tree structure
     */
    String createTree(String fileName, String blobId, String lastCommitId) {
        def content = "{ \"base_tree\": \"$lastCommitId\", \"tree\": [{\"path\": \"$fileName\",\"mode\": \"100644\",\"type\": \"blob\",\"sha\": \"$blobId\"}]}"
        def resp = sendHttpMessage("https://api.github.com/repos/$owner/$repo/git/trees", content, 'POST')
        return resp.sha
    }

    /**
     * 4. Create the commit
     *
     * @param treeId The change tree SHA id
     * @param lastCommitId The last commit SHA id
     * @param message The commit message
     * @param author The commit author name
     * @param email The commit author name
     * @return The SHA id of the commit
     */
    String createCommit(String treeId, String lastCommitId, String message, String email) {
        def content = "{\"message\": \"$message\", \"author\": {\"name\": \"$userName\", \"email\": \"$email\"}, \"parents\": [\"$lastCommitId\"], \"tree\": \"$treeId\" }"
        def resp = sendHttpMessage("https://api.github.com/repos/$owner/$repo/git/commits", content, 'POST')
        return resp.sha
    }

    /**
     * 5. Update the reference of your branch to point to the new commit SHA
     *
     * @param commitId
     * @return The response message
     */
    def updateRef(String commitId) {
        def content = "{\"ref\": \"refs/heads/$branch\", \"sha\": \"$commitId\"}"
        def resp = sendHttpMessage("https://api.github.com/repos/$owner/$repo/git/refs/heads/$branch", content, 'POST')
        return resp
    }

    void pushChange(File file, String message) {
        pushChange(file.name, file.text, message)
    }

    void pushChange(String fileName, String content, String message) {
        if( content==null )
            throw new IllegalArgumentException("Missing content argument")
        if( !fileName )
            throw new IllegalArgumentException("Missing fileName argument")
        if( !email )
            throw new IllegalArgumentException("Missing email argument")

        final lastCommit = this.lastCommitId()
        final blobId = uploadBlob(content)
        final treeId = createTree(fileName, blobId, lastCommit)
        final commitId = createCommit(treeId, lastCommit, message, email)
        updateRef(commitId)
    }

    static void main(String... args) {
        def github = new GithubClient(
                authToken: System.getenv('GITHUB_TOKEN'),
                userName: 'pditommaso',
                branch: 'main',
                repo: 'plugins' ,
                owner: 'nextflow-io',
                email: 'paolo.ditommaso@gmail.com' )

        github.pushChange('Xyz', 'hello.txt', 'One more try')
    }

}


