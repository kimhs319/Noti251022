package com.example.noti251022

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.noti251022.sender.SenderList
import com.example.noti251022.util.AppLogger
import com.example.noti251022.util.KeyStoreUtils

class MainActivity : AppCompatActivity(), AppLogger.LogListener {

    private lateinit var permissionButton: Button
    private lateinit var senderContainer: LinearLayout
    private lateinit var saveButton: Button
    private lateinit var logTextView: TextView
    private lateinit var clearLogButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        permissionButton = findViewById(R.id.permissionButton)
        senderContainer = findViewById(R.id.senderContainer)
        saveButton = findViewById(R.id.saveButton)
        logTextView = findViewById(R.id.logTextView)
        clearLogButton = findViewById(R.id.clearLogButton)

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
        }
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
}
