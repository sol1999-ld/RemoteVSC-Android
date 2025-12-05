package com.example.remotevsc

import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.Properties

class SshManager {

    // Hardcoded for demonstration. In a real app, this would be updated or fetched dynamically.
    private val VSCODE_SERVER_COMMIT_ID = "af28b32d7e553898b2a91af498b1fb666fdebe0c" // Example commit ID

    suspend fun executeCommand(
        host: String,
        port: Int,
        user: String,
        password: String,
        command: String
    ): String = withContext(Dispatchers.IO) {
        val jsch = JSch()
        var session: Session? = null
        var output = ""

        try {
            session = jsch.getSession(user, host, port)
            session.setPassword(password)
            session.setConfig(Properties().apply {
                put("StrictHostKeyChecking", "no")
            })
            session.connect(10000) // 10 second timeout

            val channel = session.openChannel("exec") as ChannelExec
            channel.setCommand(command)
            channel.setInputStream(null)
            channel.setErrStream(System.err)

            val outputStream = ByteArrayOutputStream()
            channel.setOutputStream(outputStream)

            channel.connect(3000) // 3 second timeout for channel connect

            while (!channel.isClosed) {
                Thread.sleep(100)
            }

            output = outputStream.toString("UTF-8")
            channel.disconnect()

        } catch (e: Exception) {
            output = "Error: ${e.message}"
            e.printStackTrace()
        } finally {
            session?.disconnect()
        }
        output
    }

    suspend fun downloadAndExtractVscodeServer(
        session: Session,
        downloadUrl: String,
        installPath: String
    ): String = withContext(Dispatchers.IO) {
        var output = ""
        try {
            val downloadCommand = "mkdir -p $installPath && wget -O ${installPath}/vscode-server.tar.gz $downloadUrl"
            output += executeCommandWithSession(session, downloadCommand) + "\n"

            val extractCommand = "tar -xzf ${installPath}/vscode-server.tar.gz -C $installPath --strip-components 1"
            output += executeCommandWithSession(session, extractCommand) + "\n"

            val cleanupCommand = "rm ${installPath}/vscode-server.tar.gz"
            output += executeCommandWithSession(session, cleanupCommand) + "\n"

        } catch (e: Exception) {
            output = "Error during download/extract: ${e.message}"
            e.printStackTrace()
        }
        output
    }

    suspend fun startVscodeServer(
        session: Session,
        installPath: String
    ): Pair<String, Int> = withContext(Dispatchers.IO) {
        var output = ""
        var port = -1
        try {
            // Start the server and capture its output, looking for the port it listens on
            // The --port=0 argument tells VS Code Server to pick a random available port.
            // We need to parse the output to find this port.
            // Note: This command will keep running. We need to run it in a non-blocking way
            // and then separately check for the port. For simplicity here, we're assuming
            // the port is output quickly. In a real app, you'd daemonize this process.
            val startCommand = "${installPath}/bin/code-server --port=0 --host=127.0.0.1"
            val result = executeCommandWithSession(session, startCommand) // This might block, need to handle it differently

            val portRegex = "Port: (\\d+)".toRegex()
            val matchResult = portRegex.find(result)
            port = matchResult?.groups?.get(1)?.value?.toIntOrNull() ?: -1
            output = result

        } catch (e: Exception) {
            output = "Error starting vscode-server: ${e.message}"
            e.printStackTrace()
        }
        Pair(output, port)
    }

    // Helper function to execute command using an existing session
    private suspend fun executeCommandWithSession(
        session: Session,
        command: String
    ): String = withContext(Dispatchers.IO) {
        var output = ""
        var channel: ChannelExec? = null
        try {
            channel = session.openChannel("exec") as ChannelExec
            channel.setCommand(command)
            channel.setInputStream(null)
            channel.setErrStream(System.err)

            val outputStream = ByteArrayOutputStream()
            channel.setOutputStream(outputStream)

            channel.connect(3000)

            while (!channel.isClosed) {
                Thread.sleep(100)
            }

            output = outputStream.toString("UTF-8")
        } catch (e: Exception) {
            output = "Error executing command with session: ${e.message}"
            e.printStackTrace()
        } finally {
            channel?.disconnect()
        }
        output
    }

    // New function to build the vscode-server download URL
    fun buildVscodeServerDownloadUrl(
        commitId: String = VSCODE_SERVER_COMMIT_ID,
        arch: String = "x64" // Assuming Linux x64 for now
    ): String {
        return "https://update.code.visualstudio.com/commit:$commitId/server-linux-$arch/stable"
    }

    // New function to set up SSH local port forwarding
    fun setupPortForwarding(
        session: Session,
        localPort: Int,
        remotePort: Int
    ): String {
        return try {
            session.setPortForwardingL(localPort, "localhost", remotePort)
            "Port forwarding established: localhost:$localPort -> remote:localhost:$remotePort"
        } catch (e: Exception) {
            "Error setting up port forwarding: ${e.message}"
        }
    }
}