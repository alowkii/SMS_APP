package com.example.sms_app.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.EditText
import androidx.annotation.RequiresApi
import com.example.sms_app.R
import com.example.sms_app.sms_info.OutboxMessages

class OutboxSmsAdapter(private val context: Context, private val dataSource: ArrayList<OutboxMessages>) : BaseAdapter() {

    override fun getCount(): Int {
        return dataSource.size
    }

    override fun getItem(position: Int): Any {
        return dataSource[position]
    }

    override fun getItemId(position: Int): Long {
        return dataSource[position].id
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val rowView: View
        val viewHolder: ViewHolder

        if (convertView == null) {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            rowView = inflater.inflate(R.layout.sms_card, parent, false)
            viewHolder = ViewHolder(rowView)
            rowView.tag = viewHolder
        } else {
            rowView = convertView
            viewHolder = rowView.tag as ViewHolder
        }

        val sms = getItem(position) as OutboxMessages
        viewHolder.tvContactNo.setText(sms.contactNo)
        viewHolder.tvSmsBody.setText(sms.body)

        return rowView
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private class ViewHolder(view: View) {
        val tvContactNo: EditText = view.findViewById(R.id.tv_contact_no)
        val tvSmsBody: EditText = view.findViewById(R.id.tv_sms_body)

        init {
            tvContactNo.focusable = View.NOT_FOCUSABLE
            tvContactNo.isFocusableInTouchMode = false
            tvContactNo.setBackgroundResource(android.R.color.transparent)
            tvSmsBody.focusable = View.NOT_FOCUSABLE
            tvSmsBody.isFocusableInTouchMode = false
            tvSmsBody.setBackgroundResource(android.R.color.transparent)
        }
    }
}
