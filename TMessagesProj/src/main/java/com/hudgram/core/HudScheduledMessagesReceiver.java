package com.hudgram.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;

public class HudScheduledMessagesReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (TextUtils.isEmpty(action)) return;

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            try {
                ApplicationLoader.postInitApplication();
                HudScheduledMessagesManager.rescheduleAllAlarms(context);
            } catch (Exception e) {
                FileLog.e(e);
            }
        } else if ("com.hudgram.SEND_SCHEDULED_MESSAGE".equals(action)) {
            if (!HudConfig.scheduledMessagesEnabled) {
                return;
            }
            String messageId = intent.getStringExtra("message_id");
            if (!TextUtils.isEmpty(messageId)) {
                try {
                    ApplicationLoader.postInitApplication();
                    HudScheduledMessagesManager.sendMessage(context, messageId);
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
        }
    }
}
