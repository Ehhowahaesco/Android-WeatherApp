package com.example.weatherapp
import android.Manifest
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    //Не стал добавлять список, мне кажется он тут не нужен, надеюсь это не ошибка
    // Так и не успел gps локацию доделать


    private var tvGpsLocation: TextView? = null
    private var locationHelper: GPSHelper? = null
    private var locationCallback: LocationCallback? = null


    var CITY: String = "Ivanovo"
    val API: String = "47c801fd7ba1cea24ebff8bdb54307ec"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        var edtxt = findViewById<EditText>(R.id.edit_text)
        locationHelper = GPSHelper(this)
        tvGpsLocation = findViewById(R.id.textView)

        edtxt.setOnClickListener {
            myEnter(edtxt) }

        weatherTask().execute()
        getLocation()




    }
    fun myEnter(editText : EditText){
        editText.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
            if(keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP){
                if(CITY != editText?.text.toString()) {
                    CITY = editText?.text.toString()
                    weatherTask().execute()
                }
                return@OnKeyListener true
            }
            false
        })
    }


    inner class weatherTask() : AsyncTask<String, Void, String>() {
        override fun onPreExecute() {
            super.onPreExecute()
            /* Отображение ProgressBar, Создание дизайна */
            findViewById<ProgressBar>(R.id.loader).visibility = View.VISIBLE
            findViewById<RelativeLayout>(R.id.mainContainer).visibility = View.GONE
            findViewById<TextView>(R.id.errorText).visibility = View.GONE
        }



        override fun doInBackground(vararg params: String?): String? {
            var response:String?
            try{
                // https://api.openweathermap.org/data/2.5/weather?q=$CITY&units=metric&appid=$API
                response = URL(" https://api.openweathermap.org/data/2.5/weather?q=$CITY&units=metric&appid=$API ").readText(
                        Charsets.UTF_8
                )
            }catch (e: Exception){
                response = null
            }
            return response
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            try {
                /* Получаем данные с  API */
                val jsonObj = JSONObject(result)
                val main = jsonObj.getJSONObject("main")
                val sys = jsonObj.getJSONObject("sys")
                val wind = jsonObj.getJSONObject("wind")
                val weather = jsonObj.getJSONArray("weather").getJSONObject(0)

                val updatedAt:Long = jsonObj.getLong("dt")
                val updatedAtText = "Updated at: "+ SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.ENGLISH).format(Date(updatedAt*1000))
                val temp = main.getInt("temp")

                val tempMin = "Min Temp: " + main.getInt("temp_min")+"°C"
                val tempMax = "Max Temp: " + main.getInt("temp_max")+"°C"
                val pressure = main.getString("pressure")
                val humidity = main.getString("humidity")

                val sunrise:Long = sys.getLong("sunrise")
                val sunset:Long = sys.getLong("sunset")
                val windSpeed = wind.getString("speed")
                val weatherDescription = weather.getString("description")

                val address = jsonObj.getString("name")+", "+sys.getString("country")

                /* Заполняем view */
                findViewById<TextView>(R.id.address).text = address
                findViewById<TextView>(R.id.updated_at).text =  updatedAtText
                findViewById<TextView>(R.id.status).text = weatherDescription.capitalize()
                findViewById<TextView>(R.id.temp).text = temp.toString() + "°C"
                findViewById<TextView>(R.id.temp_min).text = tempMin
                findViewById<TextView>(R.id.temp_max).text = tempMax
                findViewById<TextView>(R.id.sunrise).text = SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date(sunrise*1000))
                findViewById<TextView>(R.id.sunset).text = SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date(sunset*1000))
                findViewById<TextView>(R.id.wind).text = windSpeed
                findViewById<TextView>(R.id.pressure).text = pressure
                findViewById<TextView>(R.id.humidity).text = humidity

                /* Views populated, Hiding the loader, Showing the main design */
                findViewById<ProgressBar>(R.id.loader).visibility = View.GONE
                findViewById<RelativeLayout>(R.id.mainContainer).visibility = View.VISIBLE

            } catch (e: Exception) {
                findViewById<ProgressBar>(R.id.loader).visibility = View.GONE
                findViewById<TextView>(R.id.errorText).visibility = View.VISIBLE
            }

        }
    }

    fun getLocation(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_NETWORK_STATE), 2)
        } else {
            locationHelper?.getLocation {
                tvGpsLocation?.text = it
            }
        }

        locationCallback = object: LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                if (locationHelper?.locationRequest == null) {
                    tvGpsLocation?.text = "locationRequest == null"
                    return
                }

                val location = locationResult?.lastLocation

                tvGpsLocation?.text = "Latitude: ${location?.latitude} Longitude: ${location?.longitude}"

                if (locationResult != null) {
                    for (loc in locationResult.locations) {
                        tvGpsLocation?.text = "Latitude: ${loc.latitude} Longitude: ${loc.longitude}"
                    }
                }
            }
        }

    }


    override fun onResume() {
        super.onResume()
        locationHelper?.startLocationUpdates(locationCallback)
    }

    override fun onPause() {
        super.onPause()
        locationHelper?.stopLocationUpdates(locationCallback)
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        when (requestCode) {
            2 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
                    locationHelper?.getLocation {
                        tvGpsLocation?.text = it
                    }
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
            }
        }
    }
}
