package com.sellcallrecording.ui

import android.app.Dialog
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.sellcallrecording.R
import com.sellcallrecording.data.network.RetrofitClient
import com.sellcallrecording.databinding.ActivityLoginBinding
import com.sellcallrecording.databinding.DialogChangeBaseUrlBinding
import com.sellcallrecording.util.Session
import com.sellcallrecording.util.Util.BASE_URL
import com.sellcallrecording.util.Util.LOGIN_URL
import com.sellcallrecording.util.Util.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    @Inject
    lateinit var session: Session
    @Inject
    lateinit var retrofitClient: RetrofitClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session.putString("baseUrl", BASE_URL)
        init()
    }

    private fun init() {

        if (session.getBoolean("isLogin", false)) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            binding.imgChangeBaseUrl.visibility = View.GONE
            binding.imgChangeBaseUrl.setOnClickListener { showChangeBaseUrlDialog() }

            binding.loginButton.setOnClickListener {
                val contact = binding.emailEditText.text.toString().trim()
                val password = binding.passwordEditText.text.toString().trim()

                if (contact.isEmpty() || password.isEmpty()) {
                    showToast(this, "Please enter both contact and password")
                    return@setOnClickListener
                }
                loginUser(contact, password)
            }
        }
    }

    private fun loginUser(contact: String, password: String) {
        val progressDialog = ProgressDialog(this@LoginActivity).apply {
            setMessage("Logging in, please wait...")
            setCancelable(false)
        }

        lifecycleScope.launch {
            progressDialog.show()
            try {
                val baseUrl = session.getString("baseUrl", "")
                if (baseUrl.isNullOrEmpty()) {
                    progressDialog.dismiss()
                    showToast(
                        this@LoginActivity,
                        "Server URL not configured. Please check your settings."
                    )
                    return@launch
                }

                val requestData = HashMap<String, Any>().apply {
                    put("username", contact)
                    put("password", password)
                }
                val response = retrofitClient.getInstance(baseUrl).postGetData(LOGIN_URL, requestData)
                progressDialog.dismiss()

                if (response.status == "0") {
                    showToast(this@LoginActivity, "Login successful")

                    session.putString("token", response.token)
                    session.putBoolean("isLogin", true)

                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    showToast(this@LoginActivity, response.msg ?: "Login failed. Please try again.")
                }
            } catch (e: HttpException) {
                progressDialog.dismiss()
                showToast(this@LoginActivity, "Server error: ${e.message}. Please try again later.")
            } catch (e: IOException) {
                progressDialog.dismiss()
                showToast(
                    this@LoginActivity,
                    "Network error. Please check your internet connection."
                )
            } catch (e: Exception) {
                progressDialog.dismiss()
                showToast(
                    this@LoginActivity,
                    "An unexpected error occurred: ${e.message}. Please try again."
                )
            }
        }
    }

    private fun showChangeBaseUrlDialog() {
        val dialog = Dialog(this, R.style.CustomAlertDialogTheme)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogBinding: DialogChangeBaseUrlBinding = DialogChangeBaseUrlBinding.inflate(
            layoutInflater
        )
        dialog.setContentView(dialogBinding.getRoot())
        dialog.setCancelable(true)

        dialogBinding.editTextBaseUrl.setText(session.getString("baseUrl", ""))

        dialogBinding.btnCancel.setOnClickListener { dialog.dismiss() }

        dialogBinding.btnSubmit.setOnClickListener {
            val newBaseUrl: String = dialogBinding.editTextBaseUrl.getText().toString().trim()
            if (newBaseUrl.isNotEmpty()) {
                session.putString("baseUrl", newBaseUrl)
                dialog.dismiss()
            } else {
                dialogBinding.editTextBaseUrl.error = "Base URL cannot be empty!"
            }
        }
        dialog.show()
    }

}
