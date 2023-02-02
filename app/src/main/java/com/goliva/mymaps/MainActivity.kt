package com.goliva.mymaps

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.goliva.mymaps.databinding.ActivityMainBinding
import com.goliva.mymaps.models.UserMap
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.io.*

const val EXTRA_USER_MAP = "EXTRA_USER_MAP"
const val EXTRA_MAP_TITLE = "EXTRA_MAP_TITLE"
const val EXTRA_MAP_LOCATION = "EXTRA_MAP_LOCATION"
private const val FILENAME = "UserMaps.data"

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var userMaps: MutableList<UserMap>
    private lateinit var mapAdapter: MapsAdapter
    private lateinit var getContent: ActivityResultLauncher<Intent>
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var currLocation: DoubleArray

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userMaps = deserializeUserMaps(this).toMutableList()
        // Set layout manager on the recycler view
        binding.rvMaps.layoutManager = LinearLayoutManager(this)
        // Set adapter on the recycler view
        mapAdapter = MapsAdapter(this, userMaps, object: MapsAdapter.OnClickListener {
            override fun onItemClick(position: Int) {
                // When user taps on View in RV, navigate to new activity
                val intent = Intent(this@MainActivity, DisplayMapActivity::class.java)
                intent.putExtra(EXTRA_USER_MAP, userMaps[position])
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        })

        binding.rvMaps.adapter = mapAdapter

        getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                //  you will get result here in result.data
                val userMap = result.data?.getSerializableExtra(EXTRA_USER_MAP) as UserMap
                userMaps.add(userMap)
                mapAdapter.notifyItemInserted(userMaps.size - 1)
                serializeUserMaps(this, userMaps)
            }
        }

        binding.fabCreateMap.setOnClickListener {
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
            getCurrentLocation()
            showAlertDialog()
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                // final latitude and longitude
                fusedLocationProviderClient.lastLocation.addOnCompleteListener(this) {task ->
                    val location: Location?= task.result
                    if (location == null) {
                        Toast.makeText(this, "Null Received", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Success", Toast.LENGTH_SHORT).show()
                        currLocation = doubleArrayOf(location.latitude, location.longitude)
                    }
                }
            } else {
                // setting open here
                Toast.makeText(this, "Turn on location", Toast.LENGTH_SHORT).show()
                val intent2 = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent2)
            }
        } else {
            // request permission here
            requestPermission()
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_ACCESS_LOCATIONS)
    }

    companion object {
        private const val PERMISSION_REQUEST_ACCESS_LOCATIONS = 100
    }

    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true
        }
        return false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_ACCESS_LOCATIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(applicationContext, "Granted", Toast.LENGTH_SHORT).show()
                getCurrentLocation()
            } else {
                Toast.makeText(applicationContext, "Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAlertDialog() {
        val mapFormView = LayoutInflater.from(this).inflate(R.layout.dialog_create_map, null)
        val dialog =
            AlertDialog.Builder(this)
                .setTitle("Create New Map Title")
                .setView(mapFormView)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("OK", null)
                .show()

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val title = mapFormView.findViewById<EditText>(R.id.etTitle).text.toString()
            if (title.trim().isEmpty()) {
                Toast.makeText(this, "Map must have non-empty title", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Navigate to create map activity
            val intent = Intent(this@MainActivity, CreateMapActivity::class.java)
            intent.putExtra(EXTRA_MAP_TITLE, title)
            intent.putExtra(EXTRA_MAP_LOCATION, currLocation)
            getContent.launch(intent)
            dialog.dismiss()
        }
    }

    private fun serializeUserMaps(context: Context, userMaps: List<UserMap>) {
        ObjectOutputStream(FileOutputStream(getDataFile(context))).use { it.writeObject((userMaps)) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun deserializeUserMaps(context: Context): List<UserMap> {
        val dataFile = getDataFile(context)
        if (!dataFile.exists()) {
            return emptyList()
        }
        ObjectInputStream(FileInputStream(dataFile)).use{return it.readObject() as List<UserMap>}
    }

    private fun getDataFile(context: Context): File {
        return File(context.filesDir, FILENAME)
    }
}