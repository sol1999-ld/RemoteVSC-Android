package com.example.remotevsc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.remotevsc.ui.theme.RemoteVSCTheme
import kotlinx.coroutines.launch
import com.jcraft.jsch.Session // Import Session for port forwarding later
import java.util.Properties // Import Properties for SSH session config
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RemoteVSCTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SshConnectionScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SshConnectionScreen() {
    var host by remember { mutableStateOf("your_host") }
    var port by remember { mutableStateOf("22") }
    var user by remember { mutableStateOf("your_username") }
    var password by remember { mutableStateOf("your_password") }
    var command by remember { mutableStateOf("ls -l") }
    var output by remember { mutableStateOf("SSH Output will appear here") }
    var vscodeServerPort by remember { mutableStateOf(-1) } // To store the port of vscode-server
    var localForwardedPort by remember { mutableStateOf(-1) } // Local port for forwarding
    val coroutineScope = rememberCoroutineScope()
    val sshManager = remember { SshManager() }

    var currentSession by remember { mutableStateOf<Session?>(null) }
    var showWebView by remember { mutableStateOf(false) }

    if (showWebView && localForwardedPort != -1) {
        // Display WebView if showWebView is true and port is forwarded
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.setSupportMultipleWindows(true) // Enable for popups if needed
                    webViewClient = WebViewClient()
                    loadUrl("http://localhost:$localForwardedPort")
                }
            },
            update = { webView ->
                webView.loadUrl("http://localhost:$localForwardedPort")
            }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                label = { Text("Host") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = port,
                onValueChange = { port = it },
                label = { Text("Port") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = user,
                onValueChange = { user = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = command,
                onValueChange = { command = it },
                label = { Text("Command") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Button(
                    onClick = {
                        output = "Connecting and executing command..."
                        coroutineScope.launch {
                            val result = sshManager.executeCommand(
                                host = host,
                                port = port.toIntOrNull() ?: 22,
                                user = user,
                                password = password,
                                command = command
                            )
                            output = result
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Execute SSH Command")
                }

                Spacer(Modifier.width(8.dp))

                Button(
                    onClick = {
                        output = "Initializing VS Code Server setup..."
                        coroutineScope.launch {
                            try {
                                // Establish a session first
                                val jsch = JSch()
                                val session = jsch.getSession(user, host, port.toIntOrNull() ?: 22)
                                session.setPassword(password)
                                session.setConfig(Properties().apply {
                                    put("StrictHostKeyChecking", "no")
                                })
                                session.connect(10000)
                                currentSession = session // Store the session for later use

                                // 1. Get download URL
                                val downloadUrl = sshManager.buildVscodeServerDownloadUrl()
                                output = "VS Code Server download URL: $downloadUrl\n"

                                // 2. Download and extract
                                val installPath = "/tmp/vscode-server-mobile" // Temporary install path on remote
                                output += "Downloading and extracting VS Code Server to $installPath\n"
                                val downloadResult = sshManager.downloadAndExtractVscodeServer(session, downloadUrl, installPath)
                                output += "Download/Extract Result:\n$downloadResult\n"

                                // 3. Start vscode-server
                                output += "Starting VS Code Server...\n"
                                val (startOutput, detectedPort) = sshManager.startVscodeServer(session, installPath)
                                output += "Start Result:\n$startOutput\n"
                                vscodeServerPort = detectedPort

                                if (vscodeServerPort != -1) {
                                    output += "VS Code Server started on remote port: $vscodeServerPort\n"

                                    // 4. Setup Port Forwarding
                                    localForwardedPort = 8080 // Choose a local port for forwarding
                                    output += "Setting up port forwarding: localhost:$localForwardedPort -> remote:$vscodeServerPort\n"
                                    val forwardResult = sshManager.setupPortForwarding(session, localForwardedPort, vscodeServerPort)
                                    output += "$forwardResult\n"

                                    if (forwardResult.startsWith("Port forwarding established")) {
                                        output += "Port forwarding successful. Launching WebView...\n"
                                        showWebView = true // Show WebView
                                    } else {
                                        output += "Port forwarding failed.\n"
                                    }

                                } else {
                                    output += "Failed to determine VS Code Server port.\n"
                                }
                            } catch (e: Exception) {
                                output = "Error during VS Code Server setup: ${e.message}"
                                e.printStackTrace()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Setup VS Code Server & Connect")
                }
            }


            Text(
                text = output,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 8.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    RemoteVSCTheme {
        SshConnectionScreen()
    }
}