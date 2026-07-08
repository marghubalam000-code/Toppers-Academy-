package com.example.util

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.Socket
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

object SmtpSender {
    private const val TAG = "SmtpSender"

    suspend fun sendEmail(
        host: String,
        port: Int,
        username: String,
        password: String,
        senderName: String,
        recipient: String,
        subject: String,
        bodyText: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Connecting to SMTP Host: $host on port: $port")
            
            val isSsl = (port == 465)
            val baseSocket = if (isSsl) {
                SSLSocketFactory.getDefault().createSocket(host, port)
            } else {
                Socket(host, port)
            }
            
            var socket = baseSocket
            socket.soTimeout = 15000 // 15 seconds read timeout
            
            var reader = BufferedReader(InputStreamReader(socket.getInputStream(), "UTF-8"))
            var writer = PrintWriter(OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true)
            
            fun sendLine(line: String) {
                writer.print(line + "\r\n")
                writer.flush()
            }
            
            var supportsStartTls = false
            
            fun readResponse(expectedCode: String) {
                var responseLine: String?
                while (true) {
                    responseLine = reader.readLine() ?: throw Exception("Empty response from SMTP server")
                    Log.d(TAG, "Server: $responseLine")
                    if (responseLine.lowercase(java.util.Locale.US).contains("starttls")) {
                        supportsStartTls = true
                    }
                    if (responseLine.length >= 3) {
                        val code = responseLine.substring(0, 3)
                        // Multi-line response has '-' at index 3
                        if (responseLine.length > 3 && responseLine[3] == '-') {
                            continue
                        }
                        if (code != expectedCode) {
                            throw Exception("SMTP Protocol Error. Expected code $expectedCode, got: $responseLine")
                        }
                        break
                    } else {
                        throw Exception("Invalid SMTP response format: $responseLine")
                    }
                }
            }
            
            // 1. Connection greeting
            readResponse("220")
            
            // 2. Say EHLO
            sendLine("EHLO localhost")
            readResponse("250")
            
            // 3. STARTTLS if port is 587 or if supported and not SSL
            if (port == 587 || (port != 465 && supportsStartTls)) {
                Log.d(TAG, "Upgrading connection via STARTTLS")
                sendLine("STARTTLS")
                readResponse("220")
                
                val sslSocket = (SSLSocketFactory.getDefault() as SSLSocketFactory)
                    .createSocket(socket, host, port, true) as SSLSocket
                sslSocket.startHandshake()
                
                socket = sslSocket
                reader = BufferedReader(InputStreamReader(socket.getInputStream(), "UTF-8"))
                writer = PrintWriter(OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true)
                
                // Say EHLO again over secure channel
                sendLine("EHLO localhost")
                readResponse("250")
            }
            
            // 4. Authentication Login
            sendLine("AUTH LOGIN")
            readResponse("334")
            
            // Base64 encode Username
            val b64User = Base64.encodeToString(username.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            sendLine(b64User)
            readResponse("334")
            
            // Base64 encode Password
            val b64Pass = Base64.encodeToString(password.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            sendLine(b64Pass)
            readResponse("235")
            
            // 5. MAIL FROM
            sendLine("MAIL FROM:<$username>")
            readResponse("250")
            
            // 6. RCPT TO
            sendLine("RCPT TO:<$recipient>")
            readResponse("250")
            
            // 7. DATA
            sendLine("DATA")
            readResponse("354")
            
            // 8. Headers & Body (HTML Supported)
            sendLine("From: \"$senderName\" <$username>")
            sendLine("To: <$recipient>")
            sendLine("Subject: $subject")
            sendLine("Content-Type: text/html; charset=UTF-8")
            sendLine("") // Headers terminator line
            
            // Normalize any lone \n in bodyText to \r\n to ensure compliant data transfer
            val normalizedBody = bodyText.replace("\r\n", "\n").replace("\n", "\r\n")
            sendLine(normalizedBody)
            sendLine(".") // End of message marker
            readResponse("250")
            
            // 9. QUIT
            sendLine("QUIT")
            try {
                socket.close()
            } catch (e: Exception) {
                // Ignore socket close failure
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "SMTP Email Send Failed: ${e.message}", e)
            Result.failure(e)
        }
    }
}
