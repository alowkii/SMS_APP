package com.example.sms_app.sms_info

class OutboxMessages(val contactNo: String, val body: String) {

    val id: Long = idGeneration()

    companion object {
        private var idCounter: Long = 0
        val outboxSmsArr: ArrayList<OutboxMessages> = ArrayList()

        @Synchronized
        private fun idGeneration(): Long {
            idCounter += 1
            return idCounter
        }

        fun addSMS(sms: OutboxMessages) {
            outboxSmsArr.add(sms)
        }

        fun getAllSMS(): List<OutboxMessages> {
            return outboxSmsArr
        }

        fun getSMSById(id: Long): OutboxMessages? {
            return outboxSmsArr.find { it.id == id }
        }

        fun deleteSMSById(id: Long): Boolean {
            return outboxSmsArr.removeIf { it.id == id }
        }
    }
}
