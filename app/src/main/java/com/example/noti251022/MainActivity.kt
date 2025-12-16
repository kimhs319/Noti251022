package com.example.noti251022

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.work.*
import com.example.noti251022.sender.SenderList
import com.example.noti251022.util.AppLogger
import com.example.noti251022.util.KeyStoreUtils
import com.example.noti251022.worker.DailySeparatorWorker
import com.example.noti251022.worker.WaterHeaterReminderWorker
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity(), AppLogger.LogListener {

    private lateinit var permissionButton: Button
    private lateinit var senderContainer: LinearLayout
    private lateinit var saveButton: Button
    private lateinit var logTextView: TextView
    private lateinit var clearLogButton: Button
    private lateinit var copyLogButton: Button
    private lateinit var waterHeaterSwitch: SwitchCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        permissionButton = findViewById(R.id.permissionButton)
        senderContainer = findViewById(R.id.senderContainer)
        saveButton = findViewById(R.id.saveButton)
        logTextView = findViewById(R.id.logTextView)
        clearLogButton = findViewById(R.id.clearLogButton)
        copyLogButton = findViewById(R.id.copyLogButton)
        waterHeaterSwitch = findViewById(R.id.waterHeaterSwitch)

        // 로그 리스너 등록
        AppLogger.registerListener(this)
        
        // 기존 로그 표시
        displayLogs()

        // JSON에서 센더 이름 목록 로드
        SenderList.loadSenderNames(this)

        // KeyStore에서 저장된 credential 로드
        SenderList.loadSenderCredentials(this)

        // 권한 요청 버튼
        permissionButton.setOnClickListener {
            PermissionHelper.requestNotificationListener(this)
        }

        // 센더별 입력 필드 동적 생성
        createSenderInputFields()

        // 저장 버튼
        saveButton.setOnClickListener {
            saveSenderCredentials()
        }
        
        // 로그 삭제 버튼
        clearLogButton.setOnClickListener {
            AppLogger.clearLogs()
            logTextView.text = "로그가 여기에 표시됩니다..."
            Toast.makeText(this, "로그를 삭제했습니다", Toast.LENGTH_SHORT).show()
        }
        
        // 로그 복사 버튼
        copyLogButton.setOnClickListener {
            copyLogsToClipboard()
        }
        
        // 온수기 알림 스위치 초기화 및 리스너
        initWaterHeaterSwitch()
        
        // 매일 0시 구분선 전송 예약
        scheduleDailySeparator()
        
        // 온수기 알림 예약
        scheduleWaterHeaterReminder()
    }

    override fun onDestroy() {
        super.onDestroy()
        AppLogger.unregisterListener(this)
    }

    override fun onNewLog(entry: AppLogger.LogEntry) {
        // UI 스레드에서 실행
        runOnUiThread {
            val logLine = "[${entry.timestamp}] ${entry.level}: ${entry.message}\n"
            logTextView.append(logLine)
            
            // 자동 스크롤
            val scrollView = findViewById<ScrollView>(R.id.mainScrollView)
            scrollView?.post {
                scrollView.fullScroll(ScrollView.FOCUS_DOWN)
            }
        }
    }
    
    private fun displayLogs() {
        val logs = AppLogger.getAllLogs()
        if (logs.isEmpty()) {
            logTextView.text = "로그가 여기에 표시됩니다..."
        } else {
            logTextView.text = logs.joinToString("\n") { 
                "[${it.timestamp}] ${it.level}: ${it.message}" 
            }
        }
    }
    
    private fun copyLogsToClipboard() {
        val logs = AppLogger.getAllLogs()
        
        if (logs.isEmpty()) {
            Toast.makeText(this, "복사할 로그가 없습니다", Toast.LENGTH_SHORT).show()
            return
        }
        
        val logText = logs.joinToString("\n") { 
            "[${it.timestamp}] ${it.level}: ${it.message}" 
        }
        
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("앱 로그", logText)
        clipboard.setPrimaryClip(clip)
        
        Toast.makeText(this, "로그를 클립보드에 복사했습니다 (${logs.size}개)", Toast.LENGTH_SHORT).show()
    }

    private fun createSenderInputFields() {
        val senders = SenderList.getAllSenders()

        senders.forEach { sender ->
            // 센더 이름 표시
            val nameTextView = TextView(this).apply {
                text = "센더: ${sender.name}"
                textSize = 16f
                setPadding(0, 16, 0, 8)
            }
            senderContainer.addView(nameTextView)

            // Token 입력
            val tokenEditText = EditText(this).apply {
                hint = "${sender.name} Token"
                tag = "token_${sender.name}"
                setText(sender.token ?: "")
            }
            senderContainer.addView(tokenEditText)

            // ChatId 입력
            val chatIdEditText = EditText(this).apply {
                hint = "${sender.name} ChatId"
                tag = "chatid_${sender.name}"
                setText(sender.chatId ?: "")
            }
            senderContainer.addView(chatIdEditText)
        }
    }

    private fun saveSenderCredentials() {
        val senders = SenderList.getAllSenders()
        var allSaved = true

        senders.forEach { sender ->
            val tokenEditText = senderContainer.findViewWithTag<EditText>("token_${sender.name}")
            val chatIdEditText = senderContainer.findViewWithTag<EditText>("chatid_${sender.name}")

            val token = tokenEditText?.text.toString().trim()
            val chatId = chatIdEditText?.text.toString().trim()

            if (token.isNotEmpty() && chatId.isNotEmpty()) {
                SenderList.saveSenderCredentials(this, sender.name, token, chatId)
            } else {
                allSaved = false
            }
        }

        if (allSaved) {
            Toast.makeText(this, "모든 센더 정보 저장 완료!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "일부 센더 정보가 누락되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun initWaterHeaterSwitch() {
        // SharedPreferences에서 저장된 상태 불러오기
        val prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("water_heater_enabled", true)
        waterHeaterSwitch.isChecked = isEnabled
        
        // 스위치 상태 변경 리스너
        waterHeaterSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("water_heater_enabled", isChecked).apply()
            
            val status = if (isChecked) "활성화" else "비활성화"
            Toast.makeText(this, "온수기 알림 $status", Toast.LENGTH_SHORT).show()
            AppLogger.log("[온수기알림] $status")
            
            // 상태가 변경되면 Worker 재예약
            scheduleWaterHeaterReminder()
        }
    }
    
    private fun scheduleDailySeparator() {
        // 현재 시간
        val currentDate = Calendar.getInstance()
        
        // 다음 0시 계산
        val dueDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            
            // 이미 오늘 0시가 지났다면 내일 0시로 설정
            if (before(currentDate)) {
                add(Calendar.DATE, 1)
            }
        }
        
        // 지금부터 다음 0시까지의 시간 계산
        val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis
        
        // 24시간마다 반복
        val dailyWorkRequest = PeriodicWorkRequestBuilder<DailySeparatorWorker>(
            24, TimeUnit.HOURS
        ).setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        )
        .build()
        
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DailySeparator",
            ExistingPeriodicWorkPolicy.KEEP,  // 이미 예약되어 있으면 유지
            dailyWorkRequest
        )
        
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREAN)
        AppLogger.log("[일일구분선] 예약 완료 - 다음 실행: ${dateFormat.format(dueDate.time)}")
    }
    
    private fun scheduleWaterHeaterReminder() {
        val prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("water_heater_enabled", true)
        
        if (!isEnabled) {
            // 비활성화 시 기존 Worker 취소
            WorkManager.getInstance(this).cancelUniqueWork("WaterHeaterWeekday")
            WorkManager.getInstance(this).cancelUniqueWork("WaterHeaterSaturday")
            AppLogger.log("[온수기알림] 예약 취소됨")
            return
        }
        
        // 평일 18:00 예약
        scheduleWeekdayReminder()
        
        // 토요일 12:00 예약
        scheduleSaturdayReminder()
    }
    
    private fun scheduleWeekdayReminder() {
        val currentDate = Calendar.getInstance()
        val targetTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 18)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        // 오늘이 평일이고 아직 18:00 전이면 오늘, 아니면 다음 평일
        val dayOfWeek = currentDate.get(Calendar.DAY_OF_WEEK)
        if (dayOfWeek in Calendar.MONDAY..Calendar.FRIDAY && currentDate.before(targetTime)) {
            // 오늘 18:00 예약
        } else {
            // 다음 평일 18:00로 이동
            do {
                targetTime.add(Calendar.DATE, 1)
            } while (targetTime.get(Calendar.DAY_OF_WEEK) !in Calendar.MONDAY..Calendar.FRIDAY)
        }
        
        val timeDiff = targetTime.timeInMillis - currentDate.timeInMillis
        
        // 24시간마다 반복 (평일만 실행되도록 Worker 내부에서 체크)
        val workRequest = PeriodicWorkRequestBuilder<WaterHeaterReminderWorker>(
            24, TimeUnit.HOURS
        ).setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        )
        .addTag("weekday")
        .build()
        
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "WaterHeaterWeekday",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
        
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss (E)", Locale.KOREAN)
        AppLogger.log("[온수기알림] 평일 예약 - 다음: ${dateFormat.format(targetTime.time)}")
    }
    
    private fun scheduleSaturdayReminder() {
        val currentDate = Calendar.getInstance()
        val targetTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        // 오늘이 토요일이고 아직 12:00 전이면 오늘, 아니면 다음 토요일
        if (currentDate.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY && currentDate.before(targetTime)) {
            // 오늘 12:00 예약
        } else {
            // 다음 토요일로 이동
            do {
                targetTime.add(Calendar.DATE, 1)
            } while (targetTime.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY)
        }
        
        val timeDiff = targetTime.timeInMillis - currentDate.timeInMillis
        
        // 7일마다 반복 (토요일)
        val workRequest = PeriodicWorkRequestBuilder<WaterHeaterReminderWorker>(
            7, TimeUnit.DAYS
        ).setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        )
        .addTag("saturday")
        .build()
        
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "WaterHeaterSaturday",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
        
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss (E)", Locale.KOREAN)
        AppLogger.log("[온수기알림] 토요일 예약 - 다음: ${dateFormat.format(targetTime.time)}")
    }
}