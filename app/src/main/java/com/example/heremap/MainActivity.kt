package com.example.heremap

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.heremap.fragment.MapFragmentView

class MainActivity : AppCompatActivity() {
    private var m_mapFragmentView: MapFragmentView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()
        if (hasPermissions(this, *RUNTIME_PERMISSIONS)) {
            setupMapFragmentView()
        } else {
            ActivityCompat
                .requestPermissions(this, RUNTIME_PERMISSIONS, REQUEST_CODE_ASK_PERMISSIONS)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_CODE_ASK_PERMISSIONS -> {
                var index = 0
                while (index < permissions.size) {
                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {

                        if (!ActivityCompat
                                .shouldShowRequestPermissionRationale(this, permissions[index])
                        ) {
                            Toast.makeText(this, "Required permission " + permissions[index]
                                    + " not granted. "
                                    + "Please go to settings and turn on for sample app",
                                Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, "Required permission " + permissions[index]
                                    + " not granted", Toast.LENGTH_LONG).show()
                        }
                    }
                    index++
                }
                setupMapFragmentView()
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun setupMapFragmentView() {
        m_mapFragmentView = MapFragmentView(this)
    }

    companion object {
        private const val REQUEST_CODE_ASK_PERMISSIONS = 1
        private val RUNTIME_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE
        )
        private fun hasPermissions(context: Context, vararg permissions: String): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && permissions != null) {
                for (permission in permissions) {
                    if (ActivityCompat.checkSelfPermission(context, permission)
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        return false
                    }
                }
            }
            return true
        }
    }
}