package com.sellcallrecording.ui.home

import android.annotation.SuppressLint
import android.content.Intent
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.provider.Settings
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity.TELEPHONY_SERVICE
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.sellcallrecording.R
import com.sellcallrecording.adapter.CallDataViewAdapter
import com.sellcallrecording.adapter.CallTypeAdapter
import com.sellcallrecording.data.model.CallType
import com.sellcallrecording.database.AppDatabase
import com.sellcallrecording.database.Recording
import com.sellcallrecording.databinding.BottadialogAllBinding
import com.sellcallrecording.databinding.FragmentHomeBinding
import com.sellcallrecording.loadmore.RecyclerViewLoadMoreScroll
import com.sellcallrecording.service.CallRecordingService
import com.sellcallrecording.util.AudioRecorder
import com.sellcallrecording.util.ClickListener
import com.sellcallrecording.util.Session
import com.sellcallrecording.util.Util
import com.sellcallrecording.util.Util.LOAD_CALL_DATA_URL
import com.sellcallrecording.util.Util.LOAD_CALL_HISTORY_DATA_URL
import com.sellcallrecording.util.Util.checkPermissions
import com.sellcallrecording.util.Util.confirmationDialog
import com.sellcallrecording.util.Util.requiredPermissions
import com.sellcallrecording.util.Util.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : Fragment() {

    @Inject
    lateinit var session: Session
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var isCallFromApp = false
    private var item: CallType? = null
    private var communicationType: String? = "0"
    private var search: String? = "0"
    private var startTime: String? = ""
    private var endTime: String? = ""
    private lateinit var adapter: CallDataViewAdapter
    private val viewModel: HomeViewModel by viewModels()
    private val allCallsData: ArrayList<CallType> = ArrayList()
    private var callTypeData: ArrayList<CallType> = ArrayList()
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var scrollListener: RecyclerViewLoadMoreScroll? = null
    private var baseUrl: String = ""
    private lateinit var audioRecorder: AudioRecorder


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        baseUrl = session.getString("baseUrl", "").toString()
        init()
        return root
    }

    private fun init() {
        audioRecorder = AudioRecorder()
        checkPermissions(
            requireActivity(),
            requiredPermissions,
            object : Util.GetPermission {
                override fun getPermission(permission: Boolean) {
                    if (permission) {
                        binding.tvPermission.visibility = View.GONE
                        binding.recyclerView.visibility = View.VISIBLE
                        val telephonyManager =
                            requireActivity().getSystemService(TELEPHONY_SERVICE) as TelephonyManager
                        telephonyManager.listen(
                            phoneStateListener,
                            PhoneStateListener.LISTEN_CALL_STATE
                        )
                        setupRecyclerView()
                        setupTabs()
                    } else {
                        binding.tvPermission.visibility = View.VISIBLE
                        binding.recyclerView.visibility = View.GONE
                    }
                }
            })

        binding.tvPermission.setOnClickListener { init() }

        binding.searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.searchData(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
        setData()
    }

    private fun setData() {
        viewModel.list.observe(requireActivity()) { data ->
            binding.recyclerView.visibility = View.VISIBLE
            binding.tvNoDataFound.visibility = View.GONE
            scrollListener?.setLoaded()
            adapter.hideShimmer()
            adapter.setLoading(false)
            data?.let { adapter.addData(it) }
        }

        viewModel.errorMessage.observe(requireActivity()) {
            adapter.hideShimmer()
            adapter.setLoading(false)
            binding.recyclerView.visibility = View.GONE
            binding.tvNoDataFound.visibility = View.VISIBLE
        }

        viewModel.callType.observe(requireActivity()) { data ->
            if (data.isNullOrEmpty()) {
                showToast(requireActivity(), "No data found")
            } else {
                callTypeData = data as ArrayList<CallType>
            }
        }

        viewModel.errorMsg.observe(requireActivity()) { error ->
            showToast(requireActivity(), error)
        }
    }

    private fun fetchData(string: String) {
        adapter.clearList()
        binding.recyclerView.visibility = View.VISIBLE
        binding.tvNoDataFound.visibility = View.GONE
        adapter.showShimmer()
        viewModel.fetchData(baseUrl, string, communicationType!!, search!!)
    }

    private fun setupRecyclerView() {
        adapter = CallDataViewAdapter(requireActivity(), allCallsData, object : ClickListener {
            override fun onItemSelected(position: Int, model: Any?) {
                item = model as CallType
                val mobile = Util.sanitizeMobileNumber(item!!.m_no)

                if (mobile.length == 10) {
                    checkPermissions(requireActivity(), requiredPermissions, object :
                        Util.GetPermission {
                        override fun getPermission(permission: Boolean) {
                            if (permission) {
                                isCallFromApp = true
                                Util.makeCall(requireActivity(), mobile)
                            } else {
                                showToast(requireActivity(), "Permission Denied")
                            }
                        }
                    })
                } else {
                    showToast(requireActivity(), "It's not a valid number$mobile")
                }
            }
        },
            object : ClickListener {
                override fun onItemSelected(position: Int, model: Any?) {
                    val item = model as CallType
                    val currentDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
                    viewModel.agentWhatsappCallStatus(baseUrl, item.d_id, currentDate)
                    Util.openWhatsAppOrBusiness(requireActivity(), item.m_no)
                }
            },
            object : ClickListener {
                override fun onItemSelected(position: Int, model: Any?) {
                    val item = model as CallType
                    if (!item.remarks.isNullOrBlank()) {
                        confirmationDialog(requireActivity(), "Remarks", item.remarks, "Close") {

                        }
                    }
                }
            }
        )
        scrollListener =
            Util.bindLoadMoreRecyclerView(binding.recyclerView, 1, RecyclerView.VERTICAL,
                object : ClickListener {
                    override fun onLoadListener() {
                    }
                }
            )
        binding.recyclerView.adapter = adapter
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTabs() {
        binding.btnAllCalls.setOnClickListener {
            binding.rlAllCalls.isSelected = true
            binding.rlAll.isSelected = false
            communicationType = "0"
            fetchData(LOAD_CALL_HISTORY_DATA_URL)
        }

        binding.btnAll.setOnClickListener {
            binding.rlAllCalls.isSelected = false
            binding.rlAll.isSelected = true
            communicationType = "0"
            fetchData(LOAD_CALL_DATA_URL)
            viewModel.fetchCallTypeData(baseUrl)
        }
        binding.btnAll.setOnTouchListener { v, event -> handleDrawableEndClick(v, event) }
        binding.btnAllCalls.performClick()
    }

    private fun handleDrawableEndClick(v: View, event: MotionEvent): Boolean {
        val button = v as AppCompatButton

        val drawableEndWidth = button.compoundDrawables[2]?.intrinsicWidth ?: 0
        val drawableEndX = button.width - button.paddingRight - drawableEndWidth

        if (event.action == MotionEvent.ACTION_UP && event.x > drawableEndX) {
            if (binding.btnAll.isSelected) {
                bottomShitDialogAll()
            }
            return true
        }
        return false
    }

    private fun bottomShitDialogAll() {
        val dialog = BottomSheetDialog(requireActivity(), R.style.BottomSheetDialogTheme)

        val dialogBinding = BottadialogAllBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        dialogBinding.recyclerView.adapter =
            CallTypeAdapter(
                requireActivity(),
                callTypeData,
                communicationType,
                object : ClickListener {
                    override fun onItemSelected(position: Int, model: Any?) {
                        val item = model as CallType
                        communicationType = item.id
                        fetchData(LOAD_CALL_DATA_URL)
                        dialog.dismiss()
                    }
                })
        dialog.show()
    }


    private val phoneStateListener: PhoneStateListener = object : PhoneStateListener() {
        @Deprecated("Deprecated in Java")
        override fun onCallStateChanged(state: Int, incomingNumber: String) {
            super.onCallStateChanged(state, incomingNumber)

            when (state) {
                TelephonyManager.CALL_STATE_RINGING -> {}
                TelephonyManager.CALL_STATE_OFFHOOK -> if (isCallFromApp) startRecordingService()
                TelephonyManager.CALL_STATE_IDLE -> if (isCallFromApp) {
                    stopRecordingService()
                    isCallFromApp = false
                }
            }
        }
    }

    private fun startRecordingService() {
        viewModel.agentCallStatus(baseUrl)
        val serviceIntent = Intent(requireActivity(), CallRecordingService::class.java)
        audioRecorder.startRecording(requireActivity(), item!!.m_no)
        startTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
//        startRecording()
        ContextCompat.startForegroundService(requireActivity(), serviceIntent)
    }

    private fun stopRecordingService() {
        val serviceIntent = Intent(requireActivity(), CallRecordingService::class.java)
        outputFile = audioRecorder.stopRecording()
        endTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        uploadRecordingToServer(outputFile)
//        stopRecording()
        requireActivity().stopService(serviceIntent)
    }

    override fun onResume() {
        ensureAccessibilityServiceEnabled()
        super.onResume()
    }

    private fun ensureAccessibilityServiceEnabled() {
        if (!Util.isAccessibilityServiceEnabled(requireActivity())) {
            confirmationDialog(
                requireActivity(),
                getString(R.string.enable_accessibility),
                getString(R.string.accessibility_msg),
                getString(R.string.enable), getString(R.string.close), aProcedure = Runnable {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }, aProcedure2 = Runnable {
                    ensureAccessibilityServiceEnabled()
                })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun uploadRecordingToServer(recordingFile: File?) {
        recordingFile?.let { file ->
            try {
                val currentDate = SimpleDateFormat("d-M-yyyy", Locale.getDefault()).format(Date())
                val mediaPlayer = MediaPlayer().apply {
                    setDataSource(file.absolutePath)
                    prepare()
                }
                val durationInSeconds = mediaPlayer.duration / 1000
                mediaPlayer.release()

                if (Util.isNetworkAvailable(requireActivity())) {
                    uploadToServer(file, durationInSeconds, currentDate)
                } else {
                    saveRecordingLocally(file, durationInSeconds, currentDate)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } ?: run {
        }
    }

    private fun uploadToServer(file: File, duration: Int, date: String) {
        viewModel.uploadRecording(
            baseUrl = baseUrl,
            recordingFile = file,
            fileName = file.name,
            startTime = startTime.toString(),
            endTime = endTime.toString(),
            duration = duration,
            token = session.getString("token", "")!!,
            d_id = item!!.d_id,
            date = date
        )
    }

    private fun saveRecordingLocally(file: File, duration: Int, date: String) {
        val recording = Recording(
            fileName = file.name,
            filePath = file.absolutePath,
            startTime = startTime.toString(),
            endTime = endTime.toString(),
            duration = duration,
            token = session.getString("token", "") ?: "",
            d_id = item!!.d_id,
            date = date
        )

        CoroutineScope(Dispatchers.IO).launch {
            val db =
                Room.databaseBuilder(requireActivity(), AppDatabase::class.java, "recordings.db")
                    .build()
            db.recordingDao().insertRecording(recording)
        }
    }
}