@file:Suppress("DEPRECATION")

package com.heatingcontrol

import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.os.AsyncTask
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.heatingcontrol.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL


data class SensorData(
    val temperature: Float,
    val humidity: Float,
    val reftemp: Float
)

interface ApiService {

    @GET("/settemperature")
    fun setTemperature(@Query("temperature") temperature: String): Call<Void>

    @GET("/sendambienttemperature")
    fun sendAmbientTemperature(@Query("ambienttemperature") ambienttemperature: String): Call<Void>

    @GET("/setmode")
    fun setMode(@Query("mode") mode: String): Call<Void>

    @GET("/setsource")
    fun setSource(@Query("mode") mode: String): Call<Void>

    @GET
    suspend fun sendRequest(@Url url: String): Call<Void>
}

class MainActivity : AppCompatActivity() {

    private lateinit var phoneTemperatureTextView: TextView
    private lateinit var roomTemperatureTextView: TextView
    private lateinit var roomRefTempTextView: TextView
    private lateinit var roomHumidityTextView: TextView
    //private val serverUrl = "http://stayconnected.freedynamicdns.net:8083"
    private val serverUrl = "http://192.168.1.1:8083"
    private var modeStatus = true
    private var sourceStatus = true


    private var lastNotificationTime: Long = 0

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)



        phoneTemperatureTextView = findViewById(R.id.phoneTemperatureTextView)
        roomTemperatureTextView = findViewById(R.id.espTemperatureTextView)
        roomRefTempTextView = findViewById(R.id.espRefTempTextView)
        roomHumidityTextView = findViewById(R.id.espHumidityTextView)

        lifecycleScope.launch {
            while (true) {
                updatePhoneTemperature()
                updateRoomTemperature()
                delay(5000)
            }
        }

        // Set up mode switch
        binding.switchMode.setOnCheckedChangeListener { _, isChecked ->
            val mode = if (isChecked) "manual" else "automatic"
            modeStatus = mode == "manual"
            binding.switchHeating.isEnabled = modeStatus

            lifecycleScope.launch {
                sendModeRequest(mode)
            }
        }

        // Set up source switch
        binding.switchSource.setOnCheckedChangeListener { _, isChecked ->
            val mode = if (isChecked) "ambient" else "room"
            sourceStatus = mode == "room"

            lifecycleScope.launch {
                sendSourceRequest(mode)
            }
        }

        // Set up LED switch
        binding.switchLedControl.setOnCheckedChangeListener { _, isChecked ->
            val action = if (isChecked) "led1on" else "led1off"
            lifecycleScope.launch {
                sendRequest("/$action")
            }
        }

        // Set up heating switch
        binding.switchHeating.isEnabled = false
        binding.switchHeating.setOnCheckedChangeListener { _, isChecked ->
            val action = if (isChecked) "heatingon" else "heatingoff"
            lifecycleScope.launch {
                sendRequest("/$action")

            }
        }

        // Set a click listener for the Submit button
        binding.btnSubmit.setOnClickListener {
            if (!isAutomaticMode()) {
                // Get the entered temperature value
                val temperatureValue = binding.editTextTemperature.text.toString()

                sendTemperatureRequest(temperatureValue)
            } else {
                // Manual mode, provide user feedback or handle accordingly
                Toast.makeText(
                    this,
                    "Temperature can only be entered in Automatic mode",
                    Toast.LENGTH_SHORT
                ).show()
                Log.d("MIAU", "Temperature can only be entered in Automatic mode")
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private suspend fun updatePhoneTemperature() {
        val phoneTemperature = getBatteryTemperature()
        phoneTemperature?.let {
            withContext(Dispatchers.Main) {
                val result = calculateAmbientTemperature(it)
                phoneTemperatureTextView.text = "Ambient Temperature: $result °C"
                delay(1000)
                sendAmbientTemperatureRequest(result.toString())
            }
        }
    }

    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun updateRoomTemperature() {
        getRoomTemperature { result ->
            runOnUiThread {
                Log.d("MIAU", result)
                try {
                    val sensorData: SensorData = Gson().fromJson(result, SensorData::class.java)
                    roomTemperatureTextView.text = "Room Temperature: ${sensorData.temperature} °C"
                    roomRefTempTextView.text = "Room Current RefTemp: ${sensorData.reftemp} °C"
                    roomHumidityTextView.text = "Room Humidity: ${sensorData.humidity} %"

                    if (sensorData.humidity > 50) {
                        val currentTime = System.currentTimeMillis()
                        val elapsedTimeSinceLastNotification = currentTime - lastNotificationTime

                        if (elapsedTimeSinceLastNotification > 10 * 60 * 1000 || lastNotificationTime.toInt() == 0) {
                            MyNotificationService.showNotification(this@MainActivity)
                            lastNotificationTime = currentTime
                        }
                    }

                } catch (e: Exception) {
                    Log.d("MIAU", e.toString())
                }
            }
        }
    }

    private fun getRoomTemperature(callback: (String) -> Unit) {
        val route = "$serverUrl/gettemperature"

        class SensorDataTask : AsyncTask<Void, Void, String>() {
            @Deprecated("Deprecated in Java")
            override fun doInBackground(vararg params: Void?): String {
                var result = ""
                try {
                    val url = URL(route)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"

                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    result = reader.readLine()
                    reader.close()
                    connection.disconnect()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return result
            }

            @Deprecated("Deprecated in Java")
            override fun onPostExecute(result: String?) {
                super.onPostExecute(result)
                callback.invoke(result ?: "Error retrieving data")
            }
        }

        SensorDataTask().execute()
    }


    private fun isAutomaticMode(): Boolean {
        return modeStatus
    }

    private fun getBatteryTemperature(): Float? {
        val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val temperature = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
        return temperature?.toFloat()?.div(10) // Convert to Celsius
    }

    private fun calculateAmbientTemperature(batteryTemperature: Float): Float {
        return batteryTemperature - 5.4f
    }

    private suspend fun sendRequest(route: String) {
        try {

            val url = URL(serverUrl)
            val retrofit = Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val apiService = retrofit.create(ApiService::class.java)
            val call = apiService.sendRequest(route)

            call.enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        // Handle a successful response
                        Log.d("MIAU", "Temperature set successfully")
                        Log.d("MIAU", response.message())

                    } else {
                        // Handle an unsuccessful response
                        Log.d("MIAU", "Failed to set temperature")
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    // Handle failure
                    Log.d("MIAU", "Network error: ${t.message}")
                }
            })
        } catch (e: Exception) {
            // Handle the exception
            Log.d("MIAU", "Network error: ${e.message}")
        }
    }

    private fun sendTemperatureRequest(temperature: String) {
        try {
            val url = URL(serverUrl)


            val retrofit = Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val apiService = retrofit.create(ApiService::class.java)
            val call = apiService.setTemperature(temperature)

            call.enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        // Handle a successful response
                        Log.d("MIAU", "Temperature set successfully")
                    } else {
                        // Handle an unsuccessful response
                        Log.d("MIAU", "Failed to set temperature")
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    // Handle failure
                    Log.d("MIAU", "Network error: ${t.message}")
                }
            })
        } catch (e: Exception) {
            // Handle the exception
            Log.d("MIAU", "Network error: ${e.message}")
        }
    }

    private fun sendAmbientTemperatureRequest(ambienttemperature: String) {
        try {
            val url = URL(serverUrl)


            val retrofit = Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val apiService = retrofit.create(ApiService::class.java)
            val call = apiService.sendAmbientTemperature(ambienttemperature)

            call.enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        // Handle a successful response
                        Log.d("MIAU", "Temperature $ambienttemperature sent successfully")
                    } else {
                        // Handle an unsuccessful response
                        Log.d("MIAU", "Failed to send temperature")
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    // Handle failure
                    Log.d("MIAU", "Network error: ${t.message}")
                }
            })
        } catch (e: Exception) {
            // Handle the exception
            Log.d("MIAU", "Network error: ${e.message}")
        }
    }

    private fun sendModeRequest(mode: String) {
        try {
            val url = URL(serverUrl)


            val retrofit = Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val apiService = retrofit.create(ApiService::class.java)
            val call = apiService.setMode(mode)

            call.enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        // Handle a successful response
                        Log.d("MIAU", "Mode changed successfully")
                    } else {
                        // Handle an unsuccessful response
                        Log.d("MIAU", "Failed to change mode")
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    // Handle failure
                    Log.d("MIAU", "Network error: ${t.message}")
                }
            })
        } catch (e: Exception) {
            // Handle the exception
            Log.d("MIAU", "Network error: ${e.message}")
        }
    }

    private fun sendSourceRequest(mode: String) {
        try {
            val url = URL(serverUrl)


            val retrofit = Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val apiService = retrofit.create(ApiService::class.java)
            val call = apiService.setSource(mode)

            call.enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        // Handle a successful response
                        Log.d("MIAU", "Source changed successfully")
                    } else {
                        // Handle an unsuccessful response
                        Log.d("MIAU", "Failed to change source")
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    // Handle failure
                    Log.d("MIAU", "Network error: ${t.message}")
                }
            })
        } catch (e: Exception) {
            // Handle the exception
            Log.d("MIAU", "Network error: ${e.message}")
        }
    }

}