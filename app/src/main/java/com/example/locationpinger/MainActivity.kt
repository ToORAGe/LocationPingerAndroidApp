package com.example.locationpinger

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var client: OkHttpClient
    private lateinit var handler: Handler
    private var isPinging: Boolean = false
    private var pingInterval: Long = 1000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        client = OkHttpClient()
        handler = Handler(Looper.getMainLooper())
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val sendLocationButton: Button = findViewById(R.id.sendLocationButton)
        val intervalSpinner: Spinner = findViewById(R.id.intervalSpinner)

        // Set up the spinner with options
        val intervals = arrayOf("1", "2", "5", "100")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, intervals)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        intervalSpinner.adapter = adapter

        // Set listener on the spinner to capture the selected interval
        intervalSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                when (position) {
                    0 -> pingInterval = 1000L
                    1 -> pingInterval = 2000L
                    2 -> pingInterval = 5000L
                    3 -> pingInterval = 100000L
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }

        sendLocationButton.setOnClickListener {
            if (!isPinging) {
                isPinging = true
                startPingingLocation()
                sendLocationButton.text = "Stop Location"
            } else {
                isPinging = false
                stopPingingLocation()
                sendLocationButton.text = "Start Location"
            }
        }

        requestLocationPermission()
    }

    private fun startPingingLocation() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (isPinging) {
                    sendLocation()
                    handler.postDelayed(this, pingInterval)
                }
            }
        }, pingInterval)
    }

    private fun stopPingingLocation() {
        handler.removeCallbacksAndMessages(null)
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        }
    }

    private fun sendLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                location?.let {
                    sendLocationToServer(it.latitude, it.longitude)
                } ?: run {
                    Toast.makeText(this, "Failed to get location", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun sendLocationToServer(latitude: Double, longitude: Double) {
        val url = "https://better-ducks-float.loca.lt/location" //https server url from ngrok woth location end-point

        val jsonBody = JSONObject().apply {
            put("latitude", latitude)
            put("longitude", longitude)
        }

        val requestBody = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), jsonBody.toString())

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Failed to send location: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Location sent successfully", Toast.LENGTH_LONG).show()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Failed to send location: ${response.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }
}
