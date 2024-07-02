package com.example.sms_app

import android.Manifest
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.view.LayoutInflater
import android.view.View
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.sms_app.adapters.InboxSmsAdapter
import com.example.sms_app.sms_info.InboxMessages
import com.google.android.material.bottomnavigation.BottomNavigationView
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.util.Base64

@RequiresApi(Build.VERSION_CODES.O)
fun stringToSecretKey(secretKeyString: String): SecretKey {
    val decodedKey = Base64.getDecoder().decode(secretKeyString)
    return SecretKeySpec(decodedKey, 0, decodedKey.size, "AES")
}

@RequiresApi(Build.VERSION_CODES.O)
fun decrypt(encryptedText: String, ivString: String, secretKey: SecretKey): String {
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    val iv = Base64.getDecoder().decode(ivString)
    val spec = GCMParameterSpec(128, iv)
    cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
    val decodedBytes = Base64.getDecoder().decode(encryptedText)
    val decryptedBytes = cipher.doFinal(decodedBytes)
    return String(decryptedBytes)
}

@RequiresApi(Build.VERSION_CODES.O)
class InboxActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 111
    lateinit var lvAdapter: InboxSmsAdapter
    private lateinit var smsReceiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inbox)

        val bnav = findViewById<BottomNavigationView>(R.id.bnav)
        bnav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.outbox -> {
                    Intent(this, OutboxActivity::class.java).apply {
                        startActivity(this)
                    }
                    return@setOnItemSelectedListener true
                }
                else -> return@setOnItemSelectedListener true
            }
        }

        val lvInboxSms = findViewById<ListView>(R.id.lv_inbox_sms)
        lvAdapter = InboxSmsAdapter(this, InboxMessages.inboxSmsArr)
        lvInboxSms.adapter = lvAdapter

        // Check and request permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.READ_SMS
                ),
                PERMISSION_REQUEST_CODE
            )
        } else {
            receiveMsg()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                receiveMsg()
            } else {
                Toast.makeText(this, "Permissions are necessary to receive and send SMS", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun receiveMsg() {
        smsReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    for (sms in Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                        val contactNo = sms.originatingAddress.toString()
                        val messageParts = sms.displayMessageBody.split(":")
                        if (messageParts.size == 3) {
                            val encryptedMessage = messageParts[0]
                            val ivString = messageParts[1]
                            val secretKeyString = messageParts[2]
                            val secretKey = stringToSecretKey(secretKeyString)

                            val body = decrypt(encryptedMessage, ivString, secretKey)

                            showSmsDialog(contactNo, body)
                            val newSms = InboxMessages(contactNo, body)
                            InboxMessages.addSMS(newSms)
                            lvAdapter.notifyDataSetChanged()
                        }
                    }
                }
            }
        }
        registerReceiver(smsReceiver, IntentFilter("android.provider.Telephony.SMS_RECEIVED"))
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun showSmsDialog(contactNo: String, smsBody: String) {
        val dialogTitle = "New SMS Received"
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(dialogTitle)

        val customLayout: View = LayoutInflater.from(this).inflate(R.layout.sms_card, null)
        val tvContactNo = customLayout.findViewById<TextView>(R.id.tv_contact_no)
        val tvSmsBody = customLayout.findViewById<TextView>(R.id.tv_sms_body)

        tvContactNo.text = contactNo
        tvSmsBody.text = smsBody

        builder.setView(customLayout)
        builder.setPositiveButton("Close", null)

        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(smsReceiver)
    }
}
