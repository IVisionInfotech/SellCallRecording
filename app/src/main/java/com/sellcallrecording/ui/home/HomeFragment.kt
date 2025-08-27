package com.sellcallrecording.ui.home

import android.annotation.SuppressLint
import android.app.AlertDialog
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
import android.widget.ArrayAdapter
import android.widget.Toast
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
import com.sellcallrecording.data.model.CallResponse
import com.sellcallrecording.data.model.Category
import com.sellcallrecording.data.model.StatusList
import com.sellcallrecording.database.AppDatabase
import com.sellcallrecording.database.Recording
import com.sellcallrecording.databinding.BottadialogAllBinding
import com.sellcallrecording.databinding.DialogStatusUpdateBinding
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.collections.isNullOrEmpty

@AndroidEntryPoint
class HomeFragment : Fragment() {

    @Inject
    lateinit var session: Session
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var isCallFromApp = false
    private var item: CallResponse? = null
    private var communicationType: String? = "0"
    private var search: String? = "0"
    private var selectedStatusId: String = "0"
    private var startTime: String? = ""
    private var endTime: String? = ""
    private lateinit var adapter: CallDataViewAdapter
    private val viewModel: HomeViewModel by viewModels()
    private val allCallsData: ArrayList<CallResponse> = ArrayList()
    private var callTypeData: ArrayList<Category> = ArrayList()
    private var statusList: ArrayList<StatusList> = ArrayList()
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
        viewModel.fetchStatusListData(baseUrl)
    }

    private fun setData() {
        viewModel.list.observe(requireActivity()) { data ->
            binding.recyclerView.visibility = View.VISIBLE
            binding.tvNoDataFound.visibility = View.GONE
            scrollListener?.setLoaded()
            adapter.hideShimmer()
            adapter.setLoading(false)
            data?.let { list ->
                val filteredList = if (binding.rlAllCalls.isSelected) {
                    list.filter { it.convert_status == "1" }
                } else {
                    list.filter { it.convert_status != "1" }
                }
                adapter.addData(filteredList)
            }
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
                callTypeData = data as ArrayList<Category>
            }
        }

        viewModel.statusList.observe(requireActivity()) { data ->
            if (data.isNullOrEmpty()) {
                showToast(requireActivity(), "No data found")
            } else {
                statusList = data as ArrayList<StatusList>
            }
        }

        viewModel.errorMsg.observe(requireActivity()) { error ->
            showToast(requireActivity(), error)
        }
    }

    private fun fetchData(string: String, communicationType: String?) {
        adapter.clearList()
        binding.recyclerView.visibility = View.VISIBLE
        binding.tvNoDataFound.visibility = View.GONE
        adapter.showShimmer()
        viewModel.fetchData(baseUrl, string, communicationType!!, search!!)
    }

    private fun setupRecyclerView() {
        adapter = CallDataViewAdapter(
            requireActivity(), allCallsData, object : ClickListener {
                override fun onItemSelected(position: Int, model: Any?) {
                    item = model as CallResponse
                    val mobile = Util.sanitizeMobileNumber(item!!.M_no)

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
                    val item = model as CallResponse
                    val currentDate =
                        SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
                    viewModel.agentWhatsappCallStatus(baseUrl, item.call_id, "Whatsapp")
                    Util.openWhatsAppOrBusiness(requireActivity(), item.M_no)
                }
            },
            object : ClickListener {
                override fun onItemSelected(position: Int, model: Any?) {
                    val item = model as CallResponse
                    if (!item.feedback.isNullOrBlank()) {
                        confirmationDialog(requireActivity(), "Remarks", item.feedback, "Close") {

                        }
                    }
                }
            }
        )
        scrollListener =
            Util.bindLoadMoreRecyclerView(
                binding.recyclerView, 1, RecyclerView.VERTICAL,
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
            fetchData(LOAD_CALL_HISTORY_DATA_URL, communicationType)
        }

        binding.btnAll.setOnClickListener {
            binding.rlAllCalls.isSelected = false
            binding.rlAll.isSelected = true
            communicationType = "0"
            fetchData(LOAD_CALL_DATA_URL, communicationType)
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
                callTypeData,
                communicationType,
                object : ClickListener {
                    override fun onItemSelected(position: Int, model: Any?) {
                        val item = model as Category
                        communicationType = item.category_id
                        fetchData(LOAD_CALL_DATA_URL, communicationType)
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
        viewModel.agentWhatsappCallStatus(baseUrl,item!!.call_id,"Call")
        val serviceIntent = Intent(requireActivity(), CallRecordingService::class.java)
        audioRecorder.startRecording(requireActivity(), item!!.M_no)
        startTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
//        startRecording()
        ContextCompat.startForegroundService(requireActivity(), serviceIntent)
    }

    private fun stopRecordingService() {
        val serviceIntent = Intent(requireActivity(), CallRecordingService::class.java)
        outputFile = audioRecorder.stopRecording()
        endTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        showStatusUpdateDialog()
//        uploadRecordingToServer(outputFile)
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
                getString(R.string.enable), getString(R.string.close), aProcedure = {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }, aProcedure2 = {
                    ensureAccessibilityServiceEnabled()
                })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun uploadRecordingToServer(
        recordingFile: File?,
        selectedStatus1: String,
        remark1: String
    ) {
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
                    uploadToServer(file, durationInSeconds, currentDate, selectedStatus1, remark1)
                } else {
                    saveRecordingLocally(
                        file,
                        durationInSeconds,
                        currentDate,
                        selectedStatus1,
                        remark1
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } ?: run {
        }
    }

    private fun uploadToServer(
        file: File,
        duration: Int,
        date: String,
        selectedStatus1: String,
        remark1: String
    ) {
        viewModel.uploadRecording(
            baseUrl = baseUrl,
            recordingFile = file,
            fileName = file.name,
            startTime = startTime.toString(),
            endTime = endTime.toString(),
            duration = duration,
            token = session.getString("token", "")!!,
            d_id = item!!.call_id,
            date = date,
            status = selectedStatus1,
            remarks = remark1,
        )
    }

    private fun saveRecordingLocally(
        file: File,
        duration: Int,
        date: String,
        selectedStatus1: String,
        remark1: String
    ) {
        val recording = Recording(
            fileName = file.name,
            filePath = file.absolutePath,
            startTime = startTime.toString(),
            endTime = endTime.toString(),
            duration = duration,
            token = session.getString("token", "") ?: "",
            d_id = item!!.call_id,
            date = date,
            status = selectedStatus1,
            remark = remark1
        )

        CoroutineScope(Dispatchers.IO).launch {
            val db =
                Room.databaseBuilder(requireActivity(), AppDatabase::class.java, "recordings.db")
                    .build()
            db.recordingDao().insertRecording(recording)
        }
    }

    fun showStatusUpdateDialog() {
        val binding = DialogStatusUpdateBinding.inflate(LayoutInflater.from(context))
        val dialogView = binding.root

        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        dialog.show()

        if (statusList.isNotEmpty()) {
            val statusNames = statusList.map { it.status }
            val adapter = ArrayAdapter(
                requireActivity(),
                android.R.layout.simple_dropdown_item_1line,
                statusNames
            )
            binding.statusDropdown.setAdapter(adapter)
            binding.statusDropdown.showDropDown()

            binding.statusDropdown.setOnItemClickListener { parent, view, position, id ->
                val selectedStatusObject = statusList[position]
                val selectedStatusId = selectedStatusObject.status_id
                this.selectedStatusId = selectedStatusId
            }
        }

        binding.saveButton.setOnClickListener {
            val selectedStatus = binding.statusDropdown.text.toString()
            val remark = binding.remarkEdittext.text.toString()

            if (selectedStatus.isEmpty() || this.selectedStatusId.isEmpty()) {
                Toast.makeText(context, "Please select a status.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            uploadRecordingToServer(outputFile, selectedStatusId, remark)
            dialog.dismiss()
        }
    }
}