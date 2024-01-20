package com.vdv.happydonia

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.QueryMap

data class WikipediaResponse(
    val query: Query
)

data class Query(
    val geosearch: List<Geosearch>
)

data class Geosearch(
    val title: String,
    val lat: Double,
    val lon: Double
)

data class WikipediaRequestParams(
    val action: String,
    val format: String,
    val list: String,
    val gscoord: String,
    val gsradius: Int
)

interface WikipediaApiService {
    @GET("w/api.php")
    fun getNearbyArticles(@QueryMap queryParams: Map<String, String>): Call<WikipediaResponse>
}


class MainActivity : AppCompatActivity() {

    private val REQUEST_LOCATION_PERMISSION = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val btnRequestLocation: Button = findViewById(R.id.btnRequestLocation)
        btnRequestLocation.setOnClickListener {
            if (checkLocationPermission()) {
                // If location permission granted
                obtenerUbicacion()
            } else {
                // If no location permission requested or granted
                requestLocationPermission()
            }
        }
    }

    // Function to check if location permission is granted
    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Function to request location permission
    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_LOCATION_PERMISSION
        )
    }

    // Function to manage request permission
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult( requestCode, permissions, grantResults )
        when( requestCode ) {
            REQUEST_LOCATION_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Location permission granted
                    obtenerUbicacion()
                } else {
                    // User Location permission rejected
                    Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    // Values to send to Wikipedia API
    private val WIKIPEDIA_API_BASE_URL = "https://en.wikipedia.org/"
    private val WIKIPEDIA_API_ACTION = "query"
    private val WIKIPEDIA_API_FORMAT = "json"
    private val WIKIPEDIA_API_LIST = "geosearch"
    private val WIKIPEDIA_API_RADIUS = 10000

    // Function to get current location from user
    private fun obtenerUbicacion() {
        checkLocationPermission()
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let { it ->
                // Save in variables current user location
                val latitude = it.latitude.toString()
                val longitude = it.longitude.toString()

                // Let user know his current location
                Toast.makeText(this, "Latitud: $latitude, Longitud: $longitude", Toast.LENGTH_LONG).show()
                // Log user current location to compare and debug
                Log.d("Ubicación de usuario", "Latitud: $latitude, Longitud: $longitude")

                // Create the Retrofit instance
                val retrofit = Retrofit.Builder()
                    .baseUrl(WIKIPEDIA_API_BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                // Create the instance to send to Wikipedia api when the body is completed
                val wikipediaApiService = retrofit.create(WikipediaApiService::class.java)

                // Asign value from coordinates to save it in one variable only
                val coordenadas = "$latitude|$longitude"
                // Params to send to Wikipedia
                val queryParams = WikipediaRequestParams(
                    action = WIKIPEDIA_API_ACTION,
                    format = WIKIPEDIA_API_FORMAT,
                    list = WIKIPEDIA_API_LIST,
                    gscoord = coordenadas,
                    gsradius = WIKIPEDIA_API_RADIUS
                )

                // Map those params
                val queryParamsMap = mapOf(
                    "action" to queryParams.action,
                    "format" to queryParams.format,
                    "list" to queryParams.list,
                    "gscoord" to queryParams.gscoord,
                    "gsradius" to queryParams.gsradius.toString()
                )

                Log.d("queryParamsMap",queryParamsMap.toString());
                /*
                 Example tried in Postman, this is the data we send to 'https://en.wikipedia.org/w/api.php'
                    RAW:
                    {
                        "action":"query",
                        "format":"json",
                        "list":"geosearch",
                        "gscoord":"42.3557134|-3.6646324",
                        "gsradius":"10000"
                    }
                 */

                val call = wikipediaApiService.getNearbyArticles(queryParamsMap)

                call.enqueue(object : Callback<WikipediaResponse> {
                override fun onResponse(
                    call: Call<WikipediaResponse>,
                    response: Response<WikipediaResponse>
                ) {
                    Log.d("call", call.toString())
                    if (response.isSuccessful) {
                        Log.d("response.message", response.message())
                        Log.d("response.raw", response.raw().toString())

//                        val articles = response.body()?.query?.geosearch

//                        articles?.let {
//                            for (article in it) {
//                                Log.d("Artículo de Wikipedia", "Título: ${article.title}, Latitud: ${article.lat}, Longitud: ${article.lon}")
//                            }
//                        }
                    } else {
                        Log.e("ERROR Wikipedia", "Error en la solicitud a la API de Wikipedia: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<WikipediaResponse>, t: Throwable) {
                    Log.e("ERROR Wikipedia", "Error en la solicitud a la API de Wikipedia: ${t.message}")
                }
            })
            }
        }


    }

}

