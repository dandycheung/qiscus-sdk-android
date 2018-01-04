package com.qiscus.sdk.chat.presentation.mobile.chatroom.adapter.delegates

import android.content.Context
import android.support.v7.util.SortedList
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.qiscus.sdk.chat.presentation.mobile.R
import com.qiscus.sdk.chat.presentation.model.MessageContactViewModel
import com.qiscus.sdk.chat.presentation.model.MessageViewModel
import com.qiscus.sdk.chat.presentation.uikit.adapter.ItemClickListener
import com.qiscus.sdk.chat.presentation.uikit.adapter.ItemLongClickListener

/**
 * Created on : December 21, 2017
 * Author     : zetbaitsu
 * Name       : Zetra
 * GitHub     : https://github.com/zetbaitsu
 */
class OpponentContactAdapterDelegate @JvmOverloads constructor(private val context: Context,
                                                                private val itemClickListener: ItemClickListener? = null,
                                                                private val itemLongClickListener: ItemLongClickListener? = null)
    : MessageAdapterDelegate() {

    override fun isForViewType(data: SortedList<MessageViewModel>, position: Int): Boolean {
        val messageViewModel = data[position]
        return messageViewModel is MessageContactViewModel && messageViewModel.message.sender != account.user
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_qiscus_message_contact, parent, false)
        return OpponentContactViewHolder(view, itemClickListener, itemLongClickListener)
    }
}

open class OpponentContactViewHolder @JvmOverloads constructor(view: View,
                                                               itemClickListener: ItemClickListener? = null,
                                                               itemLongClickListener: ItemLongClickListener? = null)
    : OpponentMessageViewHolder(view, itemClickListener, itemLongClickListener) {

    private val contactNameView: TextView = itemView.findViewById(R.id.contact_name)
    private val contactValueView: TextView = itemView.findViewById(R.id.contact_id)

    override fun determineDateView(): TextView {
        return itemView.findViewById(R.id.date)
    }

    override fun determineFirstMessageBubbleIndicatorView(): ImageView {
        return itemView.findViewById(R.id.bubble)
    }

    override fun determineBubbleView(): ViewGroup {
        return itemView.findViewById(R.id.message)
    }

    override fun determineTimeView(): TextView {
        return itemView.findViewById(R.id.time)
    }

    override fun determineSenderNameView(): TextView {
        return itemView.findViewById(R.id.name)
    }

    override fun determineSenderAvatarView(): ImageView {
        return itemView.findViewById(R.id.avatar)
    }

    override fun renderMessageContents(messageViewModel: MessageViewModel) {
        contactNameView.text = (messageViewModel as MessageContactViewModel).contactName
        contactValueView.text = messageViewModel.contactValue
    }
}