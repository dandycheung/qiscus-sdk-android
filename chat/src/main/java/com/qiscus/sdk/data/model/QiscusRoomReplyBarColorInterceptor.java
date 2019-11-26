package com.qiscus.sdk.data.model;

import androidx.annotation.ColorRes;

import com.qiscus.sdk.chat.core.data.model.QMessage;

/**
 * @author yuana <andhikayuana@gmail.com>
 * @since 1/10/18
 */

public interface QiscusRoomReplyBarColorInterceptor {
    @ColorRes
    int getColor(QMessage qiscusMessage);
}
