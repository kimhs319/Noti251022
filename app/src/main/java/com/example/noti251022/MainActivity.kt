package com.example.noti251022

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.noti251022.sender.SenderList
import com.example.noti251022.util.KeyStoreUtils

class MainActivity : AppCompatActivity() {

    private lateinit var permissionButton: Button
    private lateinit var senderContainer: LinearLayout
    private lateinit var saveButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        permissionButton = findViewById(R.id.permissionButton)
        senderContainer = findViewById(R.id.senderContainer)
        saveButton = findViewById(R.id.saveButton)

        // JSON에서 센더 이름 목록 로드
        SenderList.loadSenderNames(this)

        // KeyStore에서 저장된 credential 로드
        SenderList.loadSenderCredentials(this)

        // 권한 요청 버튼
        permissionButton.setOnClickListener {
            PermissionHelper.requestNotificationListener(this)
            PermissionHelper.requestSMSPermission(this)
        }

        // 센더별 입력 필드 동적 생성
        createSenderInputFields()

        // 저장 버튼
        saveButton.setOnClickListener {
            saveSenderCredentials()
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

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "SMS 권한 승인됨", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "SMS 권한 거부됨", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
