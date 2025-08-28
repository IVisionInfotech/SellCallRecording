package com.sellcallrecording.ui

import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import com.sellcallrecording.BuildConfig
import com.sellcallrecording.BuildConfig.*
import com.sellcallrecording.data.network.RetrofitClient
import com.sellcallrecording.databinding.ActivityMainBinding
import com.sellcallrecording.util.Session
import com.sellcallrecording.util.Util.LOAD_CALL_LOGOUT_URL
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
open class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    @Inject
    lateinit var session: Session

    @Inject
    lateinit var retrofitClient: RetrofitClient

    @Inject
    @Named("token")
    lateinit var token: String
    private var back: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()
    }

    private fun init() {
        val params = binding.drawerMenu.layoutParams
        params.width = Resources.getSystem().displayMetrics.widthPixels / 2.5f.toInt()
        binding.drawerMenu.layoutParams = params

        binding.llMenu.setOnClickListener { toggleDrawer() }

        binding.btnLogout.setOnClickListener {
            closeDrawer()
            callLogoutApi()

        }

        binding.btnPending.setOnClickListener {
            closeDrawer()
            val intent = Intent(this, CallRecordingListActivity::class.java)
            startActivity(intent)
        }
        binding.tvBuild.text = "V - $VERSION_NAME"
    }

    private fun toggleDrawer() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.END)) {
            binding.drawerLayout.closeDrawer(GravityCompat.END)
        } else {
            binding.drawerLayout.openDrawer(GravityCompat.END)
        }
    }

    private fun closeDrawer() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.END)) {
            binding.drawerLayout.closeDrawer(GravityCompat.END)
        }
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.END)) {
            binding.drawerLayout.closeDrawer(GravityCompat.END)
        } else {
            if (back + 2000 > System.currentTimeMillis()) {
                super.onBackPressed()
            } else {

                Toast.makeText(this, "Press once again to exit...", Toast.LENGTH_SHORT).show()
            }
            back = System.currentTimeMillis()
        }
    }

    private fun callLogoutApi() {
        val baseUrl = session.getString("baseUrl", "").toString()
        val headers = mutableMapOf<String, String>()
        headers["Authorization"] = token

        lifecycleScope.launch {
            try {
                val response = retrofitClient.getInstance(baseUrl)
                    .postGetData(LOAD_CALL_LOGOUT_URL, headers = headers)

                if (response.status == "0") {
                    Toast.makeText(this@MainActivity, response.msg ?: "Logout successful", Toast.LENGTH_SHORT).show()
                    session.putBoolean("isLogin", false)
                    startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        response.msg ?: "Logout failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Logout error: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

}
