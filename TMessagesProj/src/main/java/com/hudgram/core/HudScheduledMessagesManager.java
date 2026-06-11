package com.hudgram.core;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.SendMessagesHelper;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.UUID;

public class HudScheduledMessagesManager {

    private static final String PREFS_NAME = "hudgram_scheduled_messages";
    private static final String KEY_MESSAGES = "scheduled_messages_list";
    private static final String KEY_LOGS = "scheduled_messages_logs_list";
    private static final String KEY_TEMPLATES = "scheduled_messages_templates";

    public static class ScheduledMessage {
        public String id;
        public ArrayList<Long> chatIds = new ArrayList<>();
        public String message;
        public long timestamp;
        public int accountId;
        
        // Pro repeat fields
        public int repeatType; // 0 = Never, 1 = Daily, 2 = Weekly, 3 = Monthly, 4 = Custom
        public long repeatInterval; // in milliseconds (for custom repeat)

        public ScheduledMessage() {
            this.id = UUID.randomUUID().toString();
        }
    }

    public static class ScheduledMessageLogEntry {
        public String id;
        public ArrayList<Long> chatIds = new ArrayList<>();
        public String message;
        public long timestamp;
        public int accountId;
        public boolean success;
        public String errorReason;

        public ScheduledMessageLogEntry() {
            this.id = UUID.randomUUID().toString();
        }
    }

    private static SharedPreferences getPrefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static ArrayList<ScheduledMessage> getScheduledMessages() {
        ArrayList<ScheduledMessage> list = new ArrayList<>();
        String json = getPrefs().getString(KEY_MESSAGES, "[]");
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                ScheduledMessage msg = new ScheduledMessage();
                msg.id = obj.optString("id", UUID.randomUUID().toString());
                msg.message = obj.optString("message", "");
                msg.timestamp = obj.optLong("timestamp", 0);
                msg.accountId = obj.optInt("accountId", 0);
                msg.repeatType = obj.optInt("repeatType", 0);
                msg.repeatInterval = obj.optLong("repeatInterval", 0);
                
                JSONArray chatsArr = obj.optJSONArray("chatIds");
                if (chatsArr != null) {
                    for (int j = 0; j < chatsArr.length(); j++) {
                        msg.chatIds.add(chatsArr.getLong(j));
                    }
                }
                list.add(msg);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return list;
    }

    public static void saveScheduledMessages(ArrayList<ScheduledMessage> list) {
        JSONArray array = new JSONArray();
        try {
            for (ScheduledMessage msg : list) {
                JSONObject obj = new JSONObject();
                obj.put("id", msg.id);
                obj.put("message", msg.message);
                obj.put("timestamp", msg.timestamp);
                obj.put("accountId", msg.accountId);
                obj.put("repeatType", msg.repeatType);
                obj.put("repeatInterval", msg.repeatInterval);
                
                JSONArray chatsArr = new JSONArray();
                for (Long chatId : msg.chatIds) {
                    chatsArr.put(chatId);
                }
                obj.put("chatIds", chatsArr);
                array.put(obj);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        getPrefs().edit().putString(KEY_MESSAGES, array.toString()).apply();
    }

    public static void addScheduledMessage(Context context, ScheduledMessage message) {
        if (message == null) return;
        ArrayList<ScheduledMessage> list = getScheduledMessages();
        list.add(message);
        saveScheduledMessages(list);
        scheduleAlarm(context, message);
    }

    public static void deleteScheduledMessage(Context context, String id) {
        if (TextUtils.isEmpty(id)) return;
        ArrayList<ScheduledMessage> list = getScheduledMessages();
        ScheduledMessage target = null;
        for (ScheduledMessage msg : list) {
            if (id.equals(msg.id)) {
                target = msg;
                break;
            }
        }
        if (target != null) {
            cancelAlarm(context, target);
            list.remove(target);
            saveScheduledMessages(list);
        }
    }

    public static void rescheduleAllAlarms(Context context) {
        ArrayList<ScheduledMessage> list = getScheduledMessages();
        long now = System.currentTimeMillis();
        ArrayList<ScheduledMessage> toRemove = new ArrayList<>();
        
        for (ScheduledMessage msg : list) {
            if (msg.timestamp > now) {
                scheduleAlarm(context, msg);
            } else {
                if (msg.repeatType != 0) {
                    // Update repeating message to future occurrence
                    long nextTime = calculateNextOccurrence(msg.timestamp, msg.repeatType, msg.repeatInterval);
                    msg.timestamp = nextTime;
                    scheduleAlarm(context, msg);
                } else {
                    // Discard old one-time messages
                    toRemove.add(msg);
                }
            }
        }
        
        if (!toRemove.isEmpty()) {
            list.removeAll(toRemove);
            saveScheduledMessages(list);
        }
    }

    public static void sendMessage(Context context, String messageId) {
        if (TextUtils.isEmpty(messageId)) return;
        ArrayList<ScheduledMessage> list = getScheduledMessages();
        ScheduledMessage target = null;
        int targetIndex = -1;
        for (int i = 0; i < list.size(); i++) {
            ScheduledMessage msg = list.get(i);
            if (messageId.equals(msg.id)) {
                target = msg;
                targetIndex = i;
                break;
            }
        }
        if (target != null) {
            sendScheduledMessage(target);
            
            // Record a log entry
            ScheduledMessageLogEntry logEntry = new ScheduledMessageLogEntry();
            logEntry.chatIds.addAll(target.chatIds);
            logEntry.message = target.message;
            logEntry.timestamp = System.currentTimeMillis();
            logEntry.accountId = target.accountId;
            logEntry.success = true;
            logEntry.errorReason = "";
            addLogEntry(logEntry);

            // Post local notification
            showNotification(context, target);

            // Handle recurrence
            if (target.repeatType != 0) {
                long nextTime = calculateNextOccurrence(target.timestamp, target.repeatType, target.repeatInterval);
                target.timestamp = nextTime;
                saveScheduledMessages(list);
                scheduleAlarm(context, target);
            } else {
                cancelAlarm(context, target);
                list.remove(targetIndex);
                saveScheduledMessages(list);
            }
        }
    }

    private static String resolveRecipientNames(int currentAccount, ArrayList<Long> chatIds) {
        if (chatIds == null || chatIds.isEmpty()) {
            return "—";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chatIds.size(); i++) {
            long chatId = chatIds.get(i);
            String name = null;
            if (chatId > 0) {
                org.telegram.tgnet.TLRPC.User user = org.telegram.messenger.MessagesController.getInstance(currentAccount).getUser(chatId);
                if (user != null) {
                    name = org.telegram.messenger.UserObject.getUserName(user);
                }
            } else {
                long rawId = -chatId;
                org.telegram.tgnet.TLRPC.Chat chat = org.telegram.messenger.MessagesController.getInstance(currentAccount).getChat(rawId);
                if (chat != null) {
                    name = chat.title;
                }
            }
            if (TextUtils.isEmpty(name)) {
                name = "ID: " + chatId;
            }
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(name);
        }
        return sb.toString();
    }

    private static void showNotification(Context context, ScheduledMessage msg) {
        try {
            String title = org.telegram.messenger.LocaleController.getString("HudScheduledMessagesNotificationTitle", org.telegram.messenger.R.string.HudScheduledMessagesNotificationTitle);
            String recipientsStr = resolveRecipientNames(msg.accountId, msg.chatIds);
            String bodyFormat = org.telegram.messenger.LocaleController.getString("HudScheduledMessagesNotificationBody", org.telegram.messenger.R.string.HudScheduledMessagesNotificationBody);
            String body = String.format(bodyFormat, recipientsStr);

            androidx.core.app.NotificationCompat.Builder builder = new androidx.core.app.NotificationCompat.Builder(context);
            builder.setWhen(System.currentTimeMillis());
            builder.setSmallIcon(org.telegram.messenger.R.drawable.notification);
            
            Intent intent = new Intent(context, org.telegram.ui.LaunchActivity.class);
            intent.setAction("com.hudgram.open_scheduled_messages_log");
            intent.putExtra("currentAccount", msg.accountId);
            
            int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 23) {
                pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
            }
            PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, pendingFlags);
            builder.setContentIntent(contentIntent);

            builder.setContentTitle(title);
            builder.setContentText(body);
            builder.setSubText(msg.message);
            
            androidx.core.app.NotificationCompat.BigTextStyle bigStyle = new androidx.core.app.NotificationCompat.BigTextStyle();
            bigStyle.setBigContentTitle(title);
            bigStyle.bigText(body + "\n\n" + msg.message);
            builder.setStyle(bigStyle);

            builder.setAutoCancel(true);
            builder.setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH);
            builder.setDefaults(androidx.core.app.NotificationCompat.DEFAULT_ALL);

            org.telegram.messenger.NotificationsController.checkOtherNotificationsChannel();
            builder.setChannelId(org.telegram.messenger.NotificationsController.OTHER_NOTIFICATIONS_CHANNEL);

            androidx.core.app.NotificationManagerCompat notificationManager = androidx.core.app.NotificationManagerCompat.from(context);
            notificationManager.notify(msg.id.hashCode(), builder.build());
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private static void sendScheduledMessage(final ScheduledMessage msg) {
        if (msg == null || msg.chatIds == null || msg.chatIds.isEmpty() || TextUtils.isEmpty(msg.message)) {
            return;
        }

        Handler handler = new Handler(Looper.getMainLooper());
        for (int i = 0; i < msg.chatIds.size(); i++) {
            final long chatId = msg.chatIds.get(i);
            final int delayMs = i * 1500; // 1.5 second staggered delay per recipient
            handler.postDelayed(() -> {
                try {
                    SendMessagesHelper.SendMessageParams params = SendMessagesHelper.SendMessageParams.of(msg.message, chatId);
                    SendMessagesHelper.getInstance(msg.accountId).sendMessage(params);
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }, delayMs);
        }
    }

    private static long calculateNextOccurrence(long currentTimestamp, int repeatType, long repeatInterval) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(currentTimestamp);
        
        switch (repeatType) {
            case 1: // Daily
                cal.add(Calendar.DAY_OF_YEAR, 1);
                break;
            case 2: // Weekly
                cal.add(Calendar.WEEK_OF_YEAR, 1);
                break;
            case 3: // Monthly
                cal.add(Calendar.MONTH, 1);
                break;
            case 4: // Custom
                if (repeatInterval > 0) {
                    return currentTimestamp + repeatInterval;
                } else {
                    cal.add(Calendar.DAY_OF_YEAR, 1);
                }
                break;
            default:
                cal.add(Calendar.DAY_OF_YEAR, 1);
                break;
        }
        
        long nextTime = cal.getTimeInMillis();
        long now = System.currentTimeMillis();
        while (nextTime <= now) {
            cal.setTimeInMillis(nextTime);
            switch (repeatType) {
                case 1:
                    cal.add(Calendar.DAY_OF_YEAR, 1);
                    break;
                case 2:
                    cal.add(Calendar.WEEK_OF_YEAR, 1);
                    break;
                case 3:
                    cal.add(Calendar.MONTH, 1);
                    break;
                case 4:
                    if (repeatInterval > 0) {
                        nextTime += repeatInterval;
                        continue;
                    } else {
                        cal.add(Calendar.DAY_OF_YEAR, 1);
                    }
                    break;
            }
            nextTime = cal.getTimeInMillis();
        }
        return nextTime;
    }

    // === Sent Logs Storage ===
    public static ArrayList<ScheduledMessageLogEntry> getScheduledMessagesLog() {
        ArrayList<ScheduledMessageLogEntry> list = new ArrayList<>();
        String json = getPrefs().getString(KEY_LOGS, "[]");
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                ScheduledMessageLogEntry entry = new ScheduledMessageLogEntry();
                entry.id = obj.optString("id", UUID.randomUUID().toString());
                entry.message = obj.optString("message", "");
                entry.timestamp = obj.optLong("timestamp", 0);
                entry.accountId = obj.optInt("accountId", 0);
                entry.success = obj.optBoolean("success", true);
                entry.errorReason = obj.optString("errorReason", "");
                
                JSONArray chatsArr = obj.optJSONArray("chatIds");
                if (chatsArr != null) {
                    for (int j = 0; j < chatsArr.length(); j++) {
                        entry.chatIds.add(chatsArr.getLong(j));
                    }
                }
                list.add(entry);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return list;
    }

    public static void saveScheduledMessagesLog(ArrayList<ScheduledMessageLogEntry> list) {
        JSONArray array = new JSONArray();
        try {
            for (ScheduledMessageLogEntry entry : list) {
                JSONObject obj = new JSONObject();
                obj.put("id", entry.id);
                obj.put("message", entry.message);
                obj.put("timestamp", entry.timestamp);
                obj.put("accountId", entry.accountId);
                obj.put("success", entry.success);
                obj.put("errorReason", entry.errorReason);
                
                JSONArray chatsArr = new JSONArray();
                for (Long chatId : entry.chatIds) {
                    chatsArr.put(chatId);
                }
                obj.put("chatIds", chatsArr);
                array.put(obj);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        getPrefs().edit().putString(KEY_LOGS, array.toString()).apply();
    }

    public static void addLogEntry(ScheduledMessageLogEntry entry) {
        if (entry == null) return;
        ArrayList<ScheduledMessageLogEntry> list = getScheduledMessagesLog();
        list.add(0, entry);
        while (list.size() > 100) {
            list.remove(list.size() - 1);
        }
        saveScheduledMessagesLog(list);
    }

    public static void clearLog() {
        getPrefs().edit().remove(KEY_LOGS).apply();
    }

    // === Message Templates Storage ===
    public static ArrayList<String> getTemplates() {
        ArrayList<String> list = new ArrayList<>();
        String json = getPrefs().getString(KEY_TEMPLATES, "[]");
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                list.add(array.getString(i));
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return list;
    }

    public static void saveTemplates(ArrayList<String> list) {
        JSONArray array = new JSONArray();
        for (String template : list) {
            array.put(template);
        }
        getPrefs().edit().putString(KEY_TEMPLATES, array.toString()).apply();
    }

    public static void addTemplate(String template) {
        if (TextUtils.isEmpty(template)) return;
        ArrayList<String> list = getTemplates();
        if (!list.contains(template)) {
            list.add(0, template);
            saveTemplates(list);
        }
    }

    public static void deleteTemplate(String template) {
        if (TextUtils.isEmpty(template)) return;
        ArrayList<String> list = getTemplates();
        if (list.remove(template)) {
            saveTemplates(list);
        }
    }

    public static void scheduleAlarm(Context context, ScheduledMessage msg) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, HudScheduledMessagesReceiver.class);
        intent.setAction("com.hudgram.SEND_SCHEDULED_MESSAGE");
        intent.putExtra("message_id", msg.id);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, msg.id.hashCode(), intent, flags);

        if (Build.VERSION.SDK_INT >= 23) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, msg.timestamp, pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, msg.timestamp, pendingIntent);
        }
    }

    public static void cancelAlarm(Context context, ScheduledMessage msg) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, HudScheduledMessagesReceiver.class);
        intent.setAction("com.hudgram.SEND_SCHEDULED_MESSAGE");
        intent.putExtra("message_id", msg.id);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, msg.id.hashCode(), intent, flags);
        alarmManager.cancel(pendingIntent);
    }
}
