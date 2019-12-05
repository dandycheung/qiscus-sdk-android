package com.qiscus.sdk.chat.core.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.qiscus.sdk.chat.core.BuildConfig;
import com.qiscus.sdk.chat.core.data.model.QMessage;

import java.util.HashMap;
import java.util.List;

public class QiscusHashMapUtil {

    public static HashMap<String, Object> eventReport(String moduleName, String event, String message) {
        HashMap<String, Object> hashMap = new HashMap<>();

        hashMap.put("module_name", moduleName);
        hashMap.put("event", event);
        hashMap.put("message", message);

        return hashMap;
    }

    public static HashMap<String, Object> login(String identityToken) {
        HashMap<String, Object> hashMap = new HashMap<>();

        hashMap.put("identity_token", identityToken);

        return hashMap;
    }

    public static HashMap<String, Object> loginOrRegister(String email, String password, String username, String avatarUrl, String extras) {
        HashMap<String, Object> hashMap = new HashMap<>();

        JsonObject jsonExtras = null;
        if (extras != null && !extras.equals("")) {
            jsonExtras = new JsonParser().parse(extras).getAsJsonObject();
        }

        hashMap.put("email", email);
        hashMap.put("password", password);
        hashMap.put("username", username);
        hashMap.put("avatar_url", avatarUrl);
        hashMap.put("extras", jsonExtras);

        return hashMap;
    }

    public static HashMap<String, Object> updateProfile(String username, String avatarUrl, String extras) {
        HashMap<String, Object> hashMap = new HashMap<>();

        JsonObject jsonExtras = null;
        if (extras != null && !extras.equals("")) {
            jsonExtras = new JsonParser().parse(extras).getAsJsonObject();
        }

        hashMap.put("name", username);
        hashMap.put("avatar_url", avatarUrl);
        hashMap.put("extras", jsonExtras);

        return hashMap;
    }

    public static HashMap<String, Object> getChatRoom(Object withEmail, String options) {
        HashMap<String, Object> hashMap = new HashMap<>();

        hashMap.put("emails", withEmail);
        hashMap.put("options", options);

        return hashMap;
    }

    public static HashMap<String, Object> createGroupChatRoom(String name, List<String> emails, String avatarUrl, String options) {
        HashMap<String, Object> hashMap = new HashMap<>();

        hashMap.put("name", name);
        hashMap.put("participants", emails);
        hashMap.put("avatar_url", avatarUrl);
        hashMap.put("options", options);

        return hashMap;
    }

    public static HashMap<String, Object> getGroupChatRoom(String uniqueId, String name, String avatarUrl, String options) {
        HashMap<String, Object> hashMap = new HashMap<>();

        hashMap.put("unique_id", uniqueId);
        hashMap.put("name", name);
        hashMap.put("avatar_url", avatarUrl);
        hashMap.put("options", options);

        return hashMap;
    }

    public static HashMap<String, Object> postComment(QMessage qiscusMessage) {
        HashMap<String, Object> hashMap = new HashMap<>();

        JsonObject jsonPayload = null;
        JsonObject jsonExtras = null;

        if (qiscusMessage.getPayload() != null) {
            jsonPayload = new JsonParser().parse(qiscusMessage.getPayload().toString()).getAsJsonObject();
        }

        if (qiscusMessage.getExtras() != null) {
            jsonExtras = new JsonParser().parse(qiscusMessage.getExtras().toString()).getAsJsonObject();
        }

        hashMap.put("comment", qiscusMessage.getMessage());
        hashMap.put("topic_id", String.valueOf(qiscusMessage.getChatRoomId()));
        hashMap.put("unique_temp_id", qiscusMessage.getUniqueId());
        hashMap.put("type", qiscusMessage.getRawType());
        hashMap.put("payload", jsonPayload);
        hashMap.put("extras", jsonExtras);

        return hashMap;
    }

    public static HashMap<String, Object> updateChatRoom(String roomId, String name, String avatarUrl, String options) {
        HashMap<String, Object> hashMap = new HashMap<>();

        hashMap.put("id", roomId);
        hashMap.put("room_name", name);
        hashMap.put("avatar_url", avatarUrl);
        hashMap.put("options", options);

        return hashMap;
    }

    public static HashMap<String, Object> updateCommentStatus(String roomId, String lastReadId, String lastReceivedId) {
        HashMap<String, Object> hashMap = new HashMap<>();

        hashMap.put("room_id", roomId);
        hashMap.put("last_comment_read_id", lastReadId);
        hashMap.put("last_comment_received_id", lastReceivedId);

        return hashMap;
    }

    public static HashMap<String, Object> registerOrRemoveFcmToken(String fcmToken) {
        HashMap<String, Object> hashMap = new HashMap<>();

        hashMap.put("device_platform", "android");
        hashMap.put("device_token", fcmToken);
        hashMap.put("is_development", BuildConfig.DEBUG);

        return hashMap;
    }

    public static HashMap<String, Object> getChatRooms(Object roomIds, Object roomUniqueIds, boolean showParticipants,
                                                       boolean showRemoved) {
        HashMap<String, Object> hashMap = new HashMap<>();


        if (roomIds != null) {
            hashMap.put("room_id", roomIds);
        }

        if (roomUniqueIds != null) {
            hashMap.put("room_unique_id", roomUniqueIds);
        }

        hashMap.put("show_participants", showParticipants);
        hashMap.put("show_removed", showRemoved);

        return hashMap;
    }

    public static HashMap<String, Object> addRoomMember(String roomId, List<String> emails) {
        HashMap<String, Object> hashMap = new HashMap<>();

        hashMap.put("room_id", roomId);
        hashMap.put("emails", emails);

        return hashMap;
    }

    public static HashMap<String, Object> removeRoomMember(String roomId, List<String> emails) {
        HashMap<String, Object> hashMap = new HashMap<>();

        hashMap.put("room_id", roomId);
        hashMap.put("emails", emails);

        return hashMap;
    }

    public static HashMap<String, Object> blockUser(String userEmail) {
        HashMap<String, Object> hashMap = new HashMap<>();

        hashMap.put("user_email", userEmail);

        return hashMap;
    }

    public static HashMap<String, Object> unblockUser(String userEmail) {
        HashMap<String, Object> hashMap = new HashMap<>();

        hashMap.put("user_email", userEmail);

        return hashMap;
    }
}