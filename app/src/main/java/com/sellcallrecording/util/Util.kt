package com.sellcallrecording.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.sellcallrecording.R
import com.sellcallrecording.loadmore.OnLoadMoreListener
import com.sellcallrecording.loadmore.RecyclerViewLoadMoreScroll
import com.sellcallrecording.permission.PermissionHandler
import com.sellcallrecording.permission.PermissionsHandler
import com.sellcallrecording.service.CallRecordingAccessibilityService
import java.util.ArrayList

object Util {

    const val BASE_URL = "http://103.148.164.183:8081/rutul/Call_Recording/Mydesk/API/"

    const val LOGIN_URL = "login.php"
    const val LOAD_CALL_DATA_URL = "get_calls.php"
    const val LOAD_CALL_HISTORY_DATA_URL = "get_calls.php"
    const val LOAD_API_CALL_TYPE_DATA_URL = "category.php"
    const val LOAD_API_CALL_STAUS_LIST_URL = "status.php"
    const val LOAD_API_CALL_AGENT_URL = "calltypeapi.php"
    const val LOAD_API_CALL_WhatsappStatus_URL = "calltypeapi.php"
    const val LOAD_CALL_HISTORY_URL = "update_call.php"
    private var ansTrue: Runnable? = null


    interface GetPermission {
        fun getPermission(permission: Boolean)
    }

    val requiredPermissions: Array<String> =
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> { // Android 14+
                arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.CALL_PHONE,
                    Manifest.permission.POST_NOTIFICATIONS,
                    Manifest.permission.FOREGROUND_SERVICE
                )
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> { // Android 13
                arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.CALL_PHONE,
                    Manifest.permission.POST_NOTIFICATIONS,
                    Manifest.permission.FOREGROUND_SERVICE
                )
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> { // Android 9+
                arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.CALL_PHONE,
                    Manifest.permission.FOREGROUND_SERVICE
                )
            }
            else -> { // Below Android 9
                arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.CALL_PHONE
                )
            }
        }



    private fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun openWhatsAppOrBusiness(context: Context, phoneNumber: String) {
        try {
            val formattedNumber = phoneNumber.replace(" ", "").replace("-", "")

            val whatsappPackage = "com.whatsapp"
            val whatsappBusinessPackage = "com.whatsapp.w4b"

            val isWhatsAppInstalled = isAppInstalled(context, whatsappPackage)
            val isWhatsAppBusinessInstalled = isAppInstalled(context, whatsappBusinessPackage)

            if (isWhatsAppInstalled && isWhatsAppBusinessInstalled) {
                showWhatsAppChoiceDialog(context, formattedNumber)
            } else if (isWhatsAppInstalled) {
                openApp(context, whatsappPackage, formattedNumber)
            } else if (isWhatsAppBusinessInstalled) {
                openApp(context, whatsappBusinessPackage, formattedNumber)
            } else {
                showToast(context, "Neither WhatsApp nor WhatsApp Business is installed")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showToast(context, "Error opening WhatsApp")
        }
    }

    private fun showWhatsAppChoiceDialog(context: Context, phoneNumber: String) {
        val dialogBuilder = android.app.AlertDialog.Builder(context)
        dialogBuilder.setTitle("Choose WhatsApp App")

        dialogBuilder.setItems(arrayOf("WhatsApp", "WhatsApp Business")) { dialog, which ->
            when (which) {
                0 -> openApp(context, "com.whatsapp", phoneNumber)
                1 -> openApp(context, "com.whatsapp.w4b", phoneNumber)
            }
            dialog.dismiss()
        }

        val dialog = dialogBuilder.create()
        dialog.show()
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun openApp(context: Context, packageName: String, phoneNumber: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://wa.me/$phoneNumber")
            setPackage(packageName)
        }

        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            showToast(context, "The selected app is not installed")
        }
    }

    fun makeCall(context: Context, phoneNumber: String) {
        try {
            val callIntent = Intent(Intent.ACTION_CALL)
            callIntent.setData(Uri.parse("tel:$phoneNumber"))

            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CALL_PHONE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                context.startActivity(callIntent)
            } else {
                Toast.makeText(context, "Call permission is not granted!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SecurityException) {
            val intent = Intent("miui.intent.action.APP_PERM_EDITOR")
            intent.setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity")
            intent.putExtra("extra_pkgname", context.packageName)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Error making call: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun bindLoadMoreRecyclerView(
        recyclerView: RecyclerView,
        spanCount: Int,
        orientation: Int,
        listener: ClickListener
    ): RecyclerViewLoadMoreScroll {

        val layoutManager = StaggeredGridLayoutManager(spanCount, orientation)
        recyclerView.layoutManager = layoutManager
        recyclerView.setItemViewCacheSize(10)
        recyclerView.isDrawingCacheEnabled = true
        recyclerView.drawingCacheQuality = View.DRAWING_CACHE_QUALITY_HIGH
        recyclerView.setHasFixedSize(true)
        val scrollListener = RecyclerViewLoadMoreScroll(layoutManager)
        scrollListener.setOnLoadMoreListener(object : OnLoadMoreListener {
            override fun onLoadMore() {
                listener.onLoadListener()
            }
        })

        recyclerView.addOnScrollListener(scrollListener)

        return scrollListener
    }

    fun bindStaggeredGridRecyclerView(
        recyclerView: RecyclerView,
        spanCount: Int,
        orientation: Int
    ) {
        val layoutManager: RecyclerView.LayoutManager =
            StaggeredGridLayoutManager(spanCount, orientation)
        recyclerView.layoutManager = layoutManager
        recyclerView.itemAnimator = DefaultItemAnimator()
        recyclerView.isDrawingCacheEnabled = true
        recyclerView.drawingCacheQuality = View.DRAWING_CACHE_QUALITY_HIGH
        recyclerView.setItemViewCacheSize(20)
        recyclerView.setHasFixedSize(false)
        recyclerView.isNestedScrollingEnabled = false
    }

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return connectivityManager.activeNetworkInfo?.isConnected == true
    }

    fun showToast(context: Context, msg: String) {
        if (msg.isNotEmpty())
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    fun checkPermissions(
        context: Context,
        permissions: Array<String>,
        getStoragePermission: GetPermission
    ) {
        PermissionsHandler.requestPermission(
            context,
            permissions,
            null,
            null,
            object : PermissionHandler() {
                override fun onPermissionGranted() {
                    getStoragePermission.getPermission(true)
                }

                override fun onPermissionDenied(
                    context: Context?,
                    deniedPermissions: ArrayList<String>?
                ) {
                    getStoragePermission.getPermission(false)
                }

                override fun onPermissionNeverAskAgain(
                    context: Context?,
                    blockedList: ArrayList<String?>?
                ): Boolean {
                    return super.onPermissionNeverAskAgain(context, blockedList)
                }

                override fun onPermissionDeniedOnce(
                    context: Context?,
                    justBlockedList: ArrayList<String?>?,
                    deniedPermissions: ArrayList<String?>?
                ) {
                    super.onPermissionDeniedOnce(context, justBlockedList, deniedPermissions)
                }
            })
    }

    fun confirmationDialog(
        context: Context?,
        title: String?,
        message: String?,
        okBtn: String?,
        aProcedure: Runnable
    ): Boolean {
        ansTrue = aProcedure
        val dialog = AlertDialog.Builder(context!!, R.style.CustomAlertDialogTheme).create()
        dialog.setTitle(title)
        dialog.setMessage(message)
        dialog.setCancelable(false)
        dialog.setButton(
            DialogInterface.BUTTON_POSITIVE,
            okBtn
        ) { dialog, buttonId -> ansTrue!!.run() }
        dialog.setOnShowListener {
            val button = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
            button.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary))
        }
        dialog.show()
        return true
    }

    fun confirmationDialog(
        context: Context?,
        title: String?,
        message: String?,
        positiveButton: String?,
        negativeButton: String?,
        aProcedure: Runnable,
        aProcedure2: Runnable
    ): Boolean {
        val dialog = AlertDialog.Builder(context!!, R.style.CustomAlertDialogTheme).create()
        dialog.setTitle(title)
        dialog.setMessage(message)
        dialog.setCancelable(false)
        dialog.setButton(
            DialogInterface.BUTTON_POSITIVE,
            positiveButton
        ) { _, _ ->
            run {
                aProcedure.run()
                dialog.dismiss()
            }
        }

        if (!negativeButton.isNullOrEmpty()) {
            dialog.setButton(DialogInterface.BUTTON_NEGATIVE, negativeButton) { dialog, _ ->
                run {
                    aProcedure2.run()
                    dialog.dismiss()
                }
            }
        }

        dialog.setOnShowListener {
            val positiveButtonView = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
            positiveButtonView.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary))
            val negativeButtonView = dialog.getButton(DialogInterface.BUTTON_NEGATIVE)
            negativeButtonView?.setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.bottomNavItemColor
                )
            )
        }

        dialog.show()
        return true
    }

    fun isAccessibilityServiceEnabled(requireActivity: FragmentActivity): Boolean {
        var accessibilityEnabled = 0
        val serviceName =
            requireActivity.packageName + "/" + CallRecordingAccessibilityService::class.java.canonicalName

        try {
            accessibilityEnabled = Settings.Secure.getInt(
                requireActivity.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            )
        } catch (e: SettingNotFoundException) {
            e.printStackTrace()
        }

        if (accessibilityEnabled == 1) {
            val enabledServices = Settings.Secure.getString(
                requireActivity.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            if (enabledServices != null) {
                for (enabledService in enabledServices.split(":".toRegex())
                    .dropLastWhile { it.isEmpty() }.toTypedArray()) {
                    if (enabledService.equals(serviceName, ignoreCase = true)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    fun sanitizeMobileNumber(inputNumber: String?): String {
        if (inputNumber.isNullOrEmpty()) {
            return ""
        }
        val sanitizedNumber = inputNumber.replace(Regex("[^\\d]"), "")

        return if (sanitizedNumber.length > 10) sanitizedNumber.takeLast(10) else sanitizedNumber
    }

}