package com.n3t

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress

class MainActivity : AppCompatActivity() {
    private lateinit var ipAddressInput: EditText
    private lateinit var pingButton: Button
    private lateinit var resultTextView: TextView
    private val exceptionService = PingExceptionService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ipAddressInput = findViewById(R.id.ipAddressInput)
        pingButton = findViewById(R.id.pingButton)
        resultTextView = findViewById(R.id.resultTextView)

        pingButton.setOnClickListener {
            val ipAddress = ipAddressInput.text.toString()
            if (ipAddress.isNotEmpty()) {
                pingIpAddress(ipAddress)
            } else {
                resultTextView.text = "Please enter an IP address"
            }
        }
    }

    private fun pingIpAddress(ipAddress: String) {
        resultTextView.text = "Pinging..."
        pingButton.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            val result = try {
                if (!isValidIpAddress(ipAddress)) {
                    throw PingExceptionService.PingException.InvalidIPAddress()
                }

                val runtime = Runtime.getRuntime()
                val process = runtime.exec("/system/bin/ping -c 4 $ipAddress")
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val errorReader = BufferedReader(InputStreamReader(process.errorStream))
                val output = StringBuilder()
                val errorOutput = StringBuilder()
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    output.append(line).append("\n")
                }

                while (errorReader.readLine().also { line = it } != null) {
                    errorOutput.append(line).append("\n")
                }

                val exitVal = process.waitFor()
                parsePingOutput(output.toString(), errorOutput.toString(), exitVal, ipAddress)
            } catch (e: Exception) {
                val pingException = when (e) {
                    is PingExceptionService.PingException -> e
                    else -> exceptionService.handleException(e)
                }
                handlePingException(pingException)
            }

            withContext(Dispatchers.Main) {
                resultTextView.text = result
                pingButton.isEnabled = true
            }
        }
    }

    private fun parsePingOutput(output: String, errorOutput: String, exitVal: Int, ipAddress: String): String {
        return when (exitVal) {
            0 -> "Ping successful.\n\n$output"
            1 -> {
                val reason = when {
                    output.contains("Destination Host Unreachable") -> "Destination host unreachable. The network path to the host could not be found."
                    output.contains("Request timed out") -> "Request timed out. The host didn't respond within the time limit."
                    errorOutput.contains("Operation not permitted") -> "Operation not permitted. The app may not have the necessary permissions."
                    else -> "Unknown reason. Please check the raw output for more details."
                }
                "Ping failed (Exit code: 1). Reason: $reason\n\n" +
                        "This could be due to:\n" +
                        "1. The target host ($ipAddress) is down or not responding\n" +
                        "2. A firewall is blocking ICMP packets\n" +
                        "3. Network connectivity issues\n" +
                        "4. Insufficient permissions for the ping command\n\n" +
                        "Raw output:\n$output\n" +
                        "Error output:\n$errorOutput"
            }
            2 -> "Ping failed (Exit code: 2). This usually indicates an invalid command-line option.\n\n" +
                    "Raw output:\n$output\n" +
                    "Error output:\n$errorOutput"
            else -> "Ping failed with an unexpected exit code: $exitVal\n\n" +
                    "Raw output:\n$output\n" +
                    "Error output:\n$errorOutput"
        }
    }

    private fun isValidIpAddress(ip: String): Boolean {
        return try {
            InetAddress.getByName(ip)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun handlePingException(exception: PingExceptionService.PingException): String {
        return when {
            exceptionService.isNetworkError(exception) ->
                "Network Error: ${exception.message}\nPlease check your internet connection and try again."
            exceptionService.isConfigurationError(exception) ->
                "Configuration Error: ${exception.message}\nPlease check your app settings and permissions."
            else -> "Error: ${exception.message}\nPlease try again or contact support if the problem persists."
        }
    }
}

 /*
 class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Allow network operations on main thread (not recommended for production)
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        val ipAddressInput = findViewById<EditText>(R.id.ipAddressInput)
        val pingButton = findViewById<Button>(R.id.pingButton)
        val resultTextView = findViewById<TextView>(R.id.resultTextView)

        pingButton.setOnClickListener {
            val ipAddress = ipAddressInput.text.toString()
            val result = pingIpAddress(ipAddress)
            resultTextView.text = result
        }
    }
 /*
  */
    /*
    private fun pingIpAddress(ipAddress: String): String {
        return try {
            val process = Runtime.getRuntime().exec("/system/bin/ping $ipAddress")
            val exitValue = process.waitFor()
            if (exitValue == 0) {
                "Ping successful to $ipAddress"
            } else {
                "Ping failed to $ipAddress"
            }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
    */

    private fun pingIpAddress(ipAddress: String): String {
        return try {
            val inetAddress = InetAddress.getByName(ipAddress)
            val startTime = System.currentTimeMillis()
            val isReachable = inetAddress.isReachable(5000) // 5 seconds timeout
            val endTime = System.currentTimeMillis()
            val pingTime = endTime - startTime

            if (isReachable) {
                "Ping successful\nIP: ${inetAddress.hostAddress}\nHostname: ${inetAddress.hostName}\nTime: ${pingTime}ms"
            } else {
                "Ping failed\nIP: ${inetAddress.hostAddress}\nHostname: ${inetAddress.hostName}\nThe host is not reachable (timeout after 5000ms)"
            }
        } catch (e: IOException) {
            "Error: ${e.message}\nMake sure you have an active internet connection and the IP address is correct."
        }
    }

}

/* DRUGI
import android.os.Bundle
import android.os.StrictMode
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.n3t.R
import java.io.IOException
import java.net.InetAddress

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Allow network operations on main thread (not recommended for production)
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        val ipAddressInput = findViewById<EditText>(R.id.ipAddressInput)
        val pingButton = findViewById<Button>(R.id.pingButton)
        val resultTextView = findViewById<TextView>(R.id.resultTextView)

        pingButton.setOnClickListener {
            val ipAddress = ipAddressInput.text.toString()
            val result = pingIpAddress(ipAddress)
            resultTextView.text = result
        }
    }

    private fun pingIpAddress(ipAddress: String): String {
        return try {
            val inetAddress = InetAddress.getByName(ipAddress)
            val startTime = System.currentTimeMillis()
            val isReachable = inetAddress.isReachable(5000) // 5 seconds timeout
            val endTime = System.currentTimeMillis()
            val pingTime = endTime - startTime

            if (isReachable) {
                "Ping successful\nIP: ${inetAddress.hostAddress}\nHostname: ${inetAddress.hostName}\nTime: ${pingTime}ms"
            } else {
                "Ping failed\nIP: ${inetAddress.hostAddress}\nHostname: ${inetAddress.hostName}\nThe host is not reachable (timeout after 5000ms)"
            }
        } catch (e: IOException) {
            "Error: ${e.message}\nMake sure you have an active internet connection and the IP address is correct."
        }
    }
}
 DRUGI */
//192.168.88.39
//
//import android.os.Bundle
//import androidx.activity.enableEdgeToEdge
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.view.ViewCompat
//import androidx.core.view.WindowInsetsCompat
//
//class MainActivity : AppCompatActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//        setContentView(R.layout.activity_main)
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }
//    }
//}

  */