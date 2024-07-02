package com.example.sms_app

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.telephony.SmsManager
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.sms_app.adapters.OutboxSmsAdapter
import com.example.sms_app.sms_info.OutboxMessages
import com.google.android.material.bottomnavigation.BottomNavigationView
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.security.SecureRandom
import java.util.Base64

fun generateSecretKey(): SecretKey {
    val keyGenerator = KeyGenerator.getInstance("AES")
    keyGenerator.init(256)
    return keyGenerator.generateKey()
}

@RequiresApi(Build.VERSION_CODES.O)
fun encrypt(plainText: String, secretKey: SecretKey): Pair<String, String> {
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    val iv = ByteArray(12)
    SecureRandom().nextBytes(iv)
    val spec = GCMParameterSpec(128, iv)
    cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)
    val encryptedBytes = cipher.doFinal(plainText.toByteArray())
    val encryptedText = Base64.getEncoder().encodeToString(encryptedBytes)
    val ivString = Base64.getEncoder().encodeToString(iv)
    return Pair(encryptedText, ivString)
}

@RequiresApi(Build.VERSION_CODES.O)
fun secretKeyToString(secretKey: SecretKey): String {
    return Base64.getEncoder().encodeToString(secretKey.encoded)
}

class OutboxActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 111
    lateinit var lvAdapter: OutboxSmsAdapter

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_outbox)

        // Set up bottom navigation
        val bnav = findViewById<BottomNavigationView>(R.id.bnav)
        bnav.selectedItemId = R.id.outbox
        bnav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.inbox -> {
                    Intent(this, InboxActivity::class.java).apply {
                        startActivity(this)
                    }
                    return@setOnItemSelectedListener true
                }
                else -> return@setOnItemSelectedListener true
            }
        }

        // Set up ListView and adapter
        val lvOutboxSms = findViewById<ListView>(R.id.lv_outbox_sms)
        lvAdapter = OutboxSmsAdapter(this, OutboxMessages.outboxSmsArr)
        lvOutboxSms.adapter = lvAdapter

        // Check and request SMS permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.READ_PHONE_STATE),
                PERMISSION_REQUEST_CODE
            )
        } else {
            displayPhoneNumber()
        }

        val btnNewSms = findViewById<Button>(R.id.btn_new_sms)
        btnNewSms.setOnClickListener {
            showNewSmsDialog()
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    private fun getPhoneNumber(): String? {
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val subscriptionManager = getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return null
        }

        val activeSubscriptionInfoList: List<SubscriptionInfo>? = subscriptionManager.activeSubscriptionInfoList
        return if (!activeSubscriptionInfoList.isNullOrEmpty()) {
            activeSubscriptionInfoList[0].number // Get the phone number from the first active SIM card
        } else {
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    private fun displayPhoneNumber() {
        val phoneNumber = getPhoneNumber()
        val tvPhoneNumber = findViewById<TextView>(R.id.tv_phone_number)
        tvPhoneNumber.text = if (phoneNumber.isNullOrEmpty()) {
            "Your Phone Number: Not Available"
        } else {
            "Your Phone Number: $phoneNumber"
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendSMS(phone: String, message: String) {
        if (phone.isNotEmpty() && message.isNotEmpty()) {
            val smsManager = SmsManager.getDefault()
            val secretKey = generateSecretKey() // Generate AES secret key
            val (encryptedMessage, ivString) = encrypt(message, secretKey) // Encrypt message
            val encodedKey = secretKeyToString(secretKey) // Encode secret key to string
            val digestMessage = "$encryptedMessage:$ivString:$encodedKey" // Concatenate encrypted message, IV, and encoded key
            smsManager.sendTextMessage(phone, null, digestMessage, null, null) // Send SMS with encrypted content
            Toast.makeText(this, "SMS Sent!", Toast.LENGTH_SHORT).show()

            val newSms = OutboxMessages(phone, message) // Create new SMS object for the outbox
            OutboxMessages.addSMS(newSms) // Add SMS to the outbox messages
            lvAdapter.notifyDataSetChanged() // Update the ListView adapter
        } else {
            Toast.makeText(this, "Please enter a phone number and message.", Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun showNewSmsDialog() {
        val dialogTitle = "Send SMS"
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(dialogTitle)

        val customLayout: View = LayoutInflater.from(this).inflate(R.layout.sms_card, null)
        val etContactNo = customLayout.findViewById<EditText>(R.id.tv_contact_no)
        val etBody = customLayout.findViewById<EditText>(R.id.tv_sms_body)

        builder.setView(customLayout)
        builder.setPositiveButton("Send") { dialog, which ->
            val contactNo = etContactNo.text.toString()
            val body = etBody.text.toString()

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    sendSMS(contactNo, body)
                }
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }

        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "Permissions Granted", Toast.LENGTH_SHORT).show()
                displayPhoneNumber()
            } else {
                Toast.makeText(this, "Permissions Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
