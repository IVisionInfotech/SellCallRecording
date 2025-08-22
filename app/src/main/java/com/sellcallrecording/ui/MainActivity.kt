package com.sellcallrecording.ui

import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.sellcallrecording.BuildConfig
import com.sellcallrecording.BuildConfig.*
import com.sellcallrecording.databinding.ActivityMainBinding
import com.sellcallrecording.util.Session
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
open class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    @Inject
    lateinit var session: Session
    private var back: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()
    }

    private fun init() {
//        val uri = Uri.parse("package:$APPLICATION_ID")
//
//        startActivity(
//            Intent(
//                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
//                uri
//            )
//        )
        val params = binding.drawerMenu.layoutParams
        params.width = Resources.getSystem().displayMetrics.widthPixels / 2.5f.toInt()
        binding.drawerMenu.layoutParams = params

        binding.llMenu.setOnClickListener { toggleDrawer() }

        binding.btnLogout.setOnClickListener {
            closeDrawer()
            session.putBoolean("isLogin", false)
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
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
}
