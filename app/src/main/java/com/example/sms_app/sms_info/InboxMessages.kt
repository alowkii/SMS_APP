package com.example.sms_app.sms_info

class InboxMessages(val contactNo: String, val body: String) {

    val id: Long = idGeneration()

    companion object {
        private var idCounter: Long = 0
        val inboxSmsArr: ArrayList<InboxMessages> = ArrayList()

        @Synchronized
        private fun idGeneration(): Long {
            idCounter += 1
            return idCounter
        }

        fun addSMS(sms: InboxMessages) {
            inboxSmsArr.add(sms)
        }

        fun getAllSMS(): List<InboxMessages> {
            return inboxSmsArr
        }

        fun getSMSById(id: Long): InboxMessages? {
            return inboxSmsArr.find { it.id == id }
        }

        fun deleteSMSById(id: Long): Boolean {
            return inboxSmsArr.removeIf { it.id == id }
        }
    }
}
