package com.sellcallrecording.ui

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.sellcallrecording.R
import com.sellcallrecording.adapter.RecordingAdapter
import com.sellcallrecording.database.AppDatabase
import com.sellcallrecording.database.Recording
import com.sellcallrecording.databinding.ActivityCallRecordingListBinding
import com.sellcallrecording.ui.home.HomeViewModel
import com.sellcallrecording.util.ClickListener
import com.sellcallrecording.util.Session
import com.sellcallrecording.util.Util.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class CallRecordingListActivity : AppCompatActivity() {

    @Inject
    lateinit var session: Session

    private val TAG: String = "CallRecordingService"
    private lateinit var binding: ActivityCallRecordingListBinding
    private lateinit var adapter: RecordingAdapter
    private val recordings: MutableList<Recording> = mutableListOf()
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var database: AppDatabase
    private lateinit var progressDialog: ProgressDialog
    private var baseUrl: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallRecordingListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = AppDatabase.getDatabase(this)
        progressDialog = ProgressDialog(this).apply {
            setMessage("Loading...")
            setCancelable(false)
        }
        baseUrl = session.getString("baseUrl", "").toString()
        init()
    }

    private fun init() {
        setupRecyclerView()
        fetchRecordings()
    }

    private fun fetchRecordings() {
        lifecycleScope.launch {
            try {
                val fetchedRecordings = database.recordingDao().getAllRecordings()
                updateUIWithRecordings(fetchedRecordings)
            } catch (e: Exception) {
                showToast(this@CallRecordingListActivity, "Failed to fetch recordings")
            }
        }
    }

    private fun updateUIWithRecordings(fetchedRecordings: List<Recording>) {
        runOnUiThread {
            if (fetchedRecordings.isEmpty()) {
                binding.tvNoRecordings.visibility = View.VISIBLE
                binding.rvRecordings.visibility = View.GONE
            } else {
                binding.tvNoRecordings.visibility = View.GONE
                binding.rvRecordings.visibility = View.VISIBLE
                recordings.clear()
                recordings.addAll(fetchedRecordings)
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun setupRecyclerView() {
        binding.rvRecordings.layoutManager = LinearLayoutManager(this)
        adapter = RecordingAdapter(
            context = this,
            recordings = recordings,
            listener = object : ClickListener {
                override fun onItemSelected(position: Int, model: Any?) {
                    val recording = model as? Recording
                    recording?.let {
                        if (isNetworkAvailable()) {
                            uploadRecordingToServer(it)
                        } else {
                            showNoInternetDialog()
                        }
                    }
                }
            }
        )
        binding.rvRecordings.adapter = adapter
    }

    private fun uploadRecordingToServer(recording: Recording) {
        val file = File(recording.filePath)
        if (!file.exists()){
            lifecycleScope.launch {
                try {
                    database.recordingDao().deleteRecording(recording)
                    fetchRecordings()
                } catch (_: Exception) {
                }
            }
            return;
        }
        try {
            progressDialog.show()
            val durationInSeconds = getRecordingDuration(file.path)
            viewModel.uploadRecording(
                baseUrl = baseUrl,
                recordingFile = file,
                fileName = recording.fileName,
                startTime = recording.startTime,
                endTime = recording.endTime,
                duration = durationInSeconds,
                token = recording.token,
                d_id = recording.d_id,
                date = recording.date,
                status = recording.status,
                remarks = recording.remark
            )

            observeViewModelResponses(recording)
        } catch (e: Exception) {
            progressDialog.dismiss()
            Toast.makeText(this, "Failed to upload recording", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getRecordingDuration(filePath: String): Int {
        var mediaPlayer: MediaPlayer? = null
        return try {
            mediaPlayer = MediaPlayer()
            mediaPlayer.setDataSource(filePath)
            mediaPlayer.prepare()
            mediaPlayer.duration / 1000
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating duration: ${e.message}", e)
        } finally {
            mediaPlayer?.release()
        }
    }

    private fun observeViewModelResponses(recording: Recording) {
        viewModel.successMsg.observe(this) {
            lifecycleScope.launch {
                try {
                    progressDialog.dismiss()
                    database.recordingDao().deleteRecording(recording)
                    fetchRecordings()
                } catch (e: Exception) {
                    progressDialog.dismiss()
                    Log.e(TAG, "Error deleting recording: ${e.message}", e)
                }
            }
        }

        viewModel.errorMsg1.observe(this) { message ->
            progressDialog.dismiss()
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    private fun showNoInternetDialog() {
        val builder = AlertDialog.Builder(this, R.style.CustomAlertDialogTheme)
        builder.setTitle("No Internet Connection")
        builder.setMessage("Please check your internet connection and try again.")
        builder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }

        val dialog = builder.create()
        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(
            ContextCompat.getColor(this, R.color.colorPrimary)
        )
    }
}
