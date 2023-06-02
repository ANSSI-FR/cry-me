/*************************** The CRY.ME project (2023) *************************************************
 *
 *  This file is part of the CRY.ME project (https://github.com/ANSSI-FR/cry-me).
 *  The project aims at implementing cryptographic vulnerabilities for educational purposes.
 *  Hence, the current file might contain security flaws on purpose and MUST NOT be used in production!
 *  Please do not use this source code outside this scope, or use it knowingly.
 *
 *  Many files come from the Android element (https://github.com/vector-im/element-android), the
 *  Matrix SDK (https://github.com/matrix-org/matrix-android-sdk2) as well as the Android Yubikit
 *  (https://github.com/Yubico/yubikit-android) projects and have been willingly modified
 *  for the CRY.ME project purposes. The Android element, Matrix SDK and Yubikit projects are distributed
 *  under the Apache-2.0 license, and so is the CRY.ME project.
 *
 ***************************  (END OF CRY.ME HEADER)   *************************************************/

package com.yubico.yubikit.android.app.ui.oath

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yubico.yubikit.android.app.R
import com.yubico.yubikit.oath.Code
import com.yubico.yubikit.oath.Credential
import java.util.*

class OathListAdapter(private val listener: ItemListener) : ListAdapter<Pair<Credential, Code?>, OathListAdapter.ViewHolder>(object : DiffUtil.ItemCallback<Pair<Credential, Code?>>() {
    override fun areItemsTheSame(oldItem: Pair<Credential, Code?>, newItem: Pair<Credential, Code?>): Boolean {
        return Arrays.equals(oldItem.first.id, newItem.first.id)
    }

    override fun areContentsTheSame(oldItem: Pair<Credential, Code?>, newItem: Pair<Credential, Code?>): Boolean {
        return oldItem.second?.value == newItem.second?.value
    }
}) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.listitem_oath_entry, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        getItem(position).let { (credential, code) ->
            with(holder) {
                idView.text = credential.issuer?.let { "$it (${credential.accountName})" }
                        ?: credential.accountName
                contentView.text = code?.value ?: ""
                deleteBtn.setOnClickListener {
                    listener.onDelete(credential.id)
                }
            }
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val idView: TextView = view.findViewById(R.id.item_number)
        val contentView: TextView = view.findViewById(R.id.content)
        val deleteBtn: Button = view.findViewById(R.id.btn_delete)
    }

    interface ItemListener {
        fun onDelete(credentialId: ByteArray)
    }
}