package com.example.bluetoothtransferapp

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import com.example.bluetoothtransferapp.ui.theme.BluetoothTransferAppTheme

class MainActivity : ComponentActivity() {
    private val fileRequestCode = 120
    private val requestBluetooth = 226
    private val discoverDuration = 300

    private lateinit var txtLocation: TextView
    private lateinit var btnSelect: Button
    private lateinit var btnSend: Button
    private lateinit var imgSelectedFile: ImageView
    private var btAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var fileUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // Make sure this is called to load the layout

        // Initialize views
        txtLocation = findViewById(R.id.txtFileLocation)
        btnSelect = findViewById(R.id.btnSelectFile)
        btnSend = findViewById(R.id.btnSendFile)
        imgSelectedFile = findViewById(R.id.imgSelectedFile)

        // Set up listeners
        btnSelect.setOnClickListener {
            txtLocation.text = ""
            openFile()
        }

        btnSend.setOnClickListener {
            try {
                sendViaBluetooth()
            } catch (e: Exception) {
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }

    }


    // Function to exit the app and turn off Bluetooth
    @RequiresApi(Build.VERSION_CODES.S)
    fun exit(v: View) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request permission if not granted
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                1
            )
            return
        }
        btAdapter?.disable() // Turn off Bluetooth
        Toast.makeText(this, "Turning off Bluetooth\nThank you for using our product", Toast.LENGTH_SHORT).show()
        finish()
    }

    // Handle file selection and Bluetooth request
    // Handle onActivityResult safely
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            fileRequestCode -> {
                if (resultCode == RESULT_OK && data != null) {
                    fileUri = data.data // Get file URI
                    if (fileUri == null) {
                        Toast.makeText(this, "No file selected", Toast.LENGTH_LONG).show()
                        return
                    }
                    txtLocation.text = fileUri.toString() // Display file URI
                    btnSend.visibility = View.VISIBLE // Show send button

                    // Check if the selected file is an image
                    val type = contentResolver.getType(fileUri!!)
                    type?.let {
                        if (it.startsWith("image/")) {
                            imgSelectedFile.setImageURI(fileUri)
                            imgSelectedFile.visibility = View.VISIBLE
                        } else {
                            imgSelectedFile.visibility = View.GONE
                        }
                    }
                }
            }

            requestBluetooth -> {
                if (resultCode == discoverDuration) {
                    // Proceed with Bluetooth file transfer
                    val btIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "*/*"
                        putExtra(Intent.EXTRA_STREAM, fileUri)
                    }

                    // Find available Bluetooth applications to handle the intent
                    val pm = packageManager
                    val list: MutableList<ResolveInfo> = pm.queryIntentActivities(btIntent, 0)

                    if (list.isNotEmpty()) {
                        var found = false
                        for (info in list) {
                            if (info.activityInfo.packageName == "com.android.bluetooth") {
                                btIntent.setClassName(info.activityInfo.packageName, info.activityInfo.name)
                                startActivity(btIntent)
                                found = true
                                break
                            }
                        }
                        if (!found) {
                            Toast.makeText(this, "Could not find Bluetooth application", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(this, "No Bluetooth app found", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this, "Bluetooth discovery failed or timed out", Toast.LENGTH_LONG).show()
                }
            }

            else -> {
                Toast.makeText(this, "Unknown request code encountered", Toast.LENGTH_SHORT).show()
            }
        }
    }


    // Function to send file via Bluetooth
    fun sendViaBluetooth() {
        if (txtLocation.text.isNotEmpty()) {
            if (btAdapter == null) {
                Toast.makeText(this, "Device does not support Bluetooth", Toast.LENGTH_LONG).show()
            } else {
                // Ensure Bluetooth permission is granted
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 1)
                    return
                }
                enableBluetooth()
            }
        } else {
            Toast.makeText(this, "Please select a file first", Toast.LENGTH_LONG).show()
        }
    }

    // Function to enable Bluetooth
    fun enableBluetooth() {
        val discoveryIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, discoverDuration)
        }
        startActivityForResult(discoveryIntent, requestBluetooth)
    }

    // Function to open file explorer to select files
    private fun openFile() {
        val fileIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*" // Allow any file type
        }
        startActivityForResult(fileIntent, fileRequestCode)
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BluetoothTransferAppTheme {
        Greeting(name = "Android")
    }
}
