package com.hudgram.ui;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.telegram.messenger.ApplicationLoader;

public class HudConfig {

    public static final int TRANS_TYPE_HUD = 0;
    public static final int TRANS_TYPE_TG = 1;

    public static final int ID_TYPE_HIDDEN = 0;
    public static final int ID_TYPE_API = 1;
    public static final int ID_TYPE_BOTAPI = 2;

    private static SharedPreferences getPrefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences("hudgram_settings", Context.MODE_PRIVATE);
    }

    // preferIPv6
    public static boolean preferIPv6 = getPrefs().getBoolean("preferIPv6", false);
    public static void toggleIPv6() {
        preferIPv6 = !preferIPv6;
        getPrefs().edit().putBoolean("preferIPv6", preferIPv6).apply();
    }

    // hideStoriesBar
    public static boolean hideStoriesBar = getPrefs().getBoolean("hideStoriesBar", true);
    public static void toggleHideStoriesBar() {
        hideStoriesBar = !hideStoriesBar;
        getPrefs().edit().putBoolean("hideStoriesBar", hideStoriesBar).apply();
    }
    
    public static int customDoubleTapAction = getPrefs().getInt("customDoubleTapAction", 0);
    public static void setCustomDoubleTapAction(int action) {
        customDoubleTapAction = action;
        getPrefs().edit().putInt("customDoubleTapAction", action).apply();
    }

    // showAvatarInHeader
    public static boolean showAvatarInHeader = getPrefs().getBoolean("showAvatarInHeader", true);
    public static void toggleShowAvatarInHeader() {
        showAvatarInHeader = !showAvatarInHeader;
        getPrefs().edit().putBoolean("showAvatarInHeader", showAvatarInHeader).apply();
    }

    // showMyNameInHeader
    public static boolean showMyNameInHeader = getPrefs().getBoolean("showMyNameInHeader", false);
    public static void toggleShowMyNameInHeader() {
        showMyNameInHeader = !showMyNameInHeader;
        getPrefs().edit().putBoolean("showMyNameInHeader", showMyNameInHeader).apply();
    }

    // showBioAsSubtitle
    public static boolean showBioAsSubtitle = getPrefs().getBoolean("showBioAsSubtitle", false);
    public static void toggleShowBioAsSubtitle() {
        showBioAsSubtitle = !showBioAsSubtitle;
        getPrefs().edit().putBoolean("showBioAsSubtitle", showBioAsSubtitle).apply();
    }

    // hideSettingsTab
    public static boolean hideSettingsTab = getPrefs().getBoolean("hideSettingsTab", true);
    public static void toggleHideSettingsTab() {
        hideSettingsTab = !hideSettingsTab;
        getPrefs().edit().putBoolean("hideSettingsTab", hideSettingsTab).apply();
    }

    // hideSearchBar
    public static boolean hideSearchBar = getPrefs().getBoolean("hideSearchBar", false);
    public static void toggleHideSearchBar() {
        hideSearchBar = !hideSearchBar;
        getPrefs().edit().putBoolean("hideSearchBar", hideSearchBar).apply();
    }

    // hideFolderTabs
    public static boolean hideFolderTabs = getPrefs().getBoolean("hideFolderTabs", false);
    public static void toggleHideFolderTabs() {
        hideFolderTabs = !hideFolderTabs;
        getPrefs().edit().putBoolean("hideFolderTabs", hideFolderTabs).apply();
    }

    // disableInstantCamera
    public static boolean disableInstantCamera = getPrefs().getBoolean("disableInstantCamera", false);
    public static void toggleDisabledInstantCamera() {
        disableInstantCamera = !disableInstantCamera;
        getPrefs().edit().putBoolean("disableInstantCamera", disableInstantCamera).apply();
    }

    // askBeforeCall
    public static boolean askBeforeCall = getPrefs().getBoolean("askBeforeCall", false);
    public static void toggleAskBeforeCall() {
        askBeforeCall = !askBeforeCall;
        getPrefs().edit().putBoolean("askBeforeCall", askBeforeCall).apply();
    }

    // openArchiveOnPull
    public static boolean openArchiveOnPull = getPrefs().getBoolean("openArchiveOnPull", false);
    public static void toggleOpenArchiveOnPull() {
        openArchiveOnPull = !openArchiveOnPull;
        getPrefs().edit().putBoolean("openArchiveOnPull", openArchiveOnPull).apply();
    }

    // accentAsNotificationColor
    public static boolean accentAsNotificationColor = getPrefs().getBoolean("accentAsNotificationColor", false);
    public static void toggleAccentAsNotificationColor() {
        accentAsNotificationColor = !accentAsNotificationColor;
        getPrefs().edit().putBoolean("accentAsNotificationColor", accentAsNotificationColor).apply();
    }

    // silenceNonContacts
    public static boolean silenceNonContacts = getPrefs().getBoolean("silenceNonContacts", false);
    public static void toggleSilenceNonContacts() {
        silenceNonContacts = !silenceNonContacts;
        getPrefs().edit().putBoolean("silenceNonContacts", silenceNonContacts).apply();
    }

    // showOriginal
    public static boolean showOriginal = getPrefs().getBoolean("showOriginal", false);
    public static void toggleShowOriginal() {
        showOriginal = !showOriginal;
        getPrefs().edit().putBoolean("showOriginal", showOriginal).apply();
    }

    // autoTranslate
    public static boolean autoTranslate = getPrefs().getBoolean("autoTranslate", false);
    public static void toggleAutoTranslate() {
        autoTranslate = !autoTranslate;
        getPrefs().edit().putBoolean("autoTranslate", autoTranslate).apply();
    }

    // translationProvider
    public static String translationProvider = getPrefs().getString("translationProvider", "google");
    public static void setTranslationProvider(String provider) {
        translationProvider = provider;
        getPrefs().edit().putString("translationProvider", provider).apply();
    }

    // translationTarget
    public static String translationTarget = getPrefs().getString("translationTarget", "app");
    public static void setTranslationTarget(String target) {
        translationTarget = target;
        getPrefs().edit().putString("translationTarget", target).apply();
    }

    // translationEnabled
    public static boolean translationEnabled = getPrefs().getBoolean("translationEnabled", true);
    public static void toggleTranslationEnabled() {
        translationEnabled = !translationEnabled;
        getPrefs().edit().putBoolean("translationEnabled", translationEnabled).apply();
    }

    // transType
    public static int transType = getPrefs().getInt("transType", TRANS_TYPE_HUD);
    public static void setTransType(int type) {
        transType = type;
        getPrefs().edit().putInt("transType", type).apply();
    }

    // nameOrder
    public static int nameOrder = getPrefs().getInt("nameOrder", 1);
    public static void setNameOrder(int order) {
        nameOrder = order;
        getPrefs().edit().putInt("nameOrder", order).apply();
    }

    // idType
    public static int idType = getPrefs().getInt("idType", ID_TYPE_API);
    public static void setIdType(int type) {
        idType = type;
        getPrefs().edit().putInt("idType", type).apply();
    }

    // Restricted languages (do not translate list)
    private static Set<String> restrictedLanguagesSet = getPrefs().getStringSet("restrictedLanguages", null);
    public static ArrayList<String> getRestrictedLanguages() {
        if (restrictedLanguagesSet == null) {
            ArrayList<String> defaultLangs = new ArrayList<>();
            defaultLangs.add("en");
            defaultLangs.add("ar");
            return defaultLangs;
        }
        return new ArrayList<>(restrictedLanguagesSet);
    }
    public static void setRestrictedLanguages(ArrayList<String> langs) {
        restrictedLanguagesSet = new HashSet<>(langs);
        getPrefs().edit().putStringSet("restrictedLanguages", restrictedLanguagesSet).apply();
    }

    // hideNotificationContent
    public static boolean hideNotificationContent = getPrefs().getBoolean("hideNotificationContent", false);
    public static void toggleHideNotificationContent() {
        hideNotificationContent = !hideNotificationContent;
        getPrefs().edit().putBoolean("hideNotificationContent", hideNotificationContent).apply();
    }

    // confirmStickers
    public static boolean confirmStickers = getPrefs().getBoolean("confirmStickers", false);
    public static void toggleConfirmStickers() {
        confirmStickers = !confirmStickers;
        getPrefs().edit().putBoolean("confirmStickers", confirmStickers).apply();
    }

    // confirmVoiceMessages
    public static boolean confirmVoiceMessages = getPrefs().getBoolean("confirmVoiceMessages", false);
    public static void toggleConfirmVoiceMessages() {
        confirmVoiceMessages = !confirmVoiceMessages;
        getPrefs().edit().putBoolean("confirmVoiceMessages", confirmVoiceMessages).apply();
    }

    // partialCopy
    public static boolean partialCopy = getPrefs().getBoolean("partialCopy", false);
    public static void togglePartialCopy() {
        partialCopy = !partialCopy;
        getPrefs().edit().putBoolean("partialCopy", partialCopy).apply();
    }

    // quickReplyEnabled
    public static boolean quickReplyEnabled = getPrefs().getBoolean("quickReplyEnabled", true);
    public static void toggleQuickReplyEnabled() {
        quickReplyEnabled = !quickReplyEnabled;
        getPrefs().edit().putBoolean("quickReplyEnabled", quickReplyEnabled).apply();
    }

    // autoReplyMentionEnabled
    public static boolean autoReplyMentionEnabled = getPrefs().getBoolean("autoReplyMentionEnabled", false);
    public static void toggleAutoReplyMentionEnabled() {
        autoReplyMentionEnabled = !autoReplyMentionEnabled;
        getPrefs().edit().putBoolean("autoReplyMentionEnabled", autoReplyMentionEnabled).apply();
    }

    // autoReplyMentionText
    public static String autoReplyMentionText = getPrefs().getString("autoReplyMentionText", "أهلاً بك، سأطلع على رسالتك وأرد عليك قريباً.");
    public static void setAutoReplyMentionText(String text) {
        autoReplyMentionText = text;
        getPrefs().edit().putString("autoReplyMentionText", autoReplyMentionText).apply();
    }

    // autoReplyMentionCooldown
    public static int autoReplyMentionCooldown = getPrefs().getInt("autoReplyMentionCooldown", 30);
    public static void setAutoReplyMentionCooldown(int cooldown) {
        autoReplyMentionCooldown = cooldown;
        getPrefs().edit().putInt("autoReplyMentionCooldown", autoReplyMentionCooldown).apply();
    }

    // autoReplyMode: 0=single, 1=multiple, 2=smart
    public static int autoReplyMode = getPrefs().getInt("autoReplyMode", 0);
    public static void setAutoReplyMode(int mode) {
        autoReplyMode = mode;
        getPrefs().edit().putInt("autoReplyMode", mode).apply();
    }

    // autoReplyCooldownMode: 0=per-group, 1=per-sender
    public static int autoReplyCooldownMode = getPrefs().getInt("autoReplyCooldownMode", 1);
    public static void setAutoReplyCooldownMode(int mode) {
        autoReplyCooldownMode = mode;
        getPrefs().edit().putInt("autoReplyCooldownMode", mode).apply();
    }

    // Schedule
    public static boolean autoReplyScheduleEnabled = getPrefs().getBoolean("autoReplyScheduleEnabled", false);
    public static void toggleAutoReplyScheduleEnabled() {
        autoReplyScheduleEnabled = !autoReplyScheduleEnabled;
        getPrefs().edit().putBoolean("autoReplyScheduleEnabled", autoReplyScheduleEnabled).apply();
    }
    public static int autoReplyScheduleStartHour = getPrefs().getInt("autoReplyScheduleStartHour", 23);
    public static int autoReplyScheduleStartMinute = getPrefs().getInt("autoReplyScheduleStartMinute", 0);
    public static void setAutoReplyScheduleStart(int hour, int minute) {
        autoReplyScheduleStartHour = hour;
        autoReplyScheduleStartMinute = minute;
        getPrefs().edit().putInt("autoReplyScheduleStartHour", hour).putInt("autoReplyScheduleStartMinute", minute).apply();
    }
    public static int autoReplyScheduleEndHour = getPrefs().getInt("autoReplyScheduleEndHour", 8);
    public static int autoReplyScheduleEndMinute = getPrefs().getInt("autoReplyScheduleEndMinute", 0);
    public static void setAutoReplyScheduleEnd(int hour, int minute) {
        autoReplyScheduleEndHour = hour;
        autoReplyScheduleEndMinute = minute;
        getPrefs().edit().putInt("autoReplyScheduleEndHour", hour).putInt("autoReplyScheduleEndMinute", minute).apply();
    }

    // Smart time-based reply messages
    public static String autoReplyMorningText = getPrefs().getString("autoReplyMorningText", "صباح الخير، سأرد عليك بعد قليل إن شاء الله.");
    public static void setAutoReplyMorningText(String text) {
        autoReplyMorningText = text;
        getPrefs().edit().putString("autoReplyMorningText", text).apply();
    }
    public static String autoReplyAfternoonText = getPrefs().getString("autoReplyAfternoonText", "أهلاً، سأطلع على رسالتك وأرد عليك قريباً إن شاء الله.");
    public static void setAutoReplyAfternoonText(String text) {
        autoReplyAfternoonText = text;
        getPrefs().edit().putString("autoReplyAfternoonText", text).apply();
    }
    public static String autoReplyEveningText = getPrefs().getString("autoReplyEveningText", "مساء الخير، سأرجع لك بأقرب وقت إن شاء الله.");
    public static void setAutoReplyEveningText(String text) {
        autoReplyEveningText = text;
        getPrefs().edit().putString("autoReplyEveningText", text).apply();
    }
    public static String autoReplyNightText = getPrefs().getString("autoReplyNightText", "شكراً على رسالتك، سأرد عليك صباحاً إن شاء الله.");
    public static void setAutoReplyNightText(String text) {
        autoReplyNightText = text;
        getPrefs().edit().putString("autoReplyNightText", text).apply();
    }

    // Group filter: 0=all, 1=whitelist, 2=blacklist
    public static int autoReplyFilterMode = getPrefs().getInt("autoReplyFilterMode", 0);
    public static void setAutoReplyFilterMode(int mode) {
        autoReplyFilterMode = mode;
        getPrefs().edit().putInt("autoReplyFilterMode", mode).apply();
    }
    public static java.util.Set<String> getAutoReplyFilterGroups() {
        return getPrefs().getStringSet("autoReplyFilterGroups", new java.util.HashSet<>());
    }
    public static void setAutoReplyFilterGroups(java.util.Set<String> groups) {
        getPrefs().edit().putStringSet("autoReplyFilterGroups", groups).apply();
    }
    public static void addAutoReplyFilterGroup(long dialogId) {
        java.util.Set<String> groups = new java.util.HashSet<>(getAutoReplyFilterGroups());
        groups.add(String.valueOf(dialogId));
        setAutoReplyFilterGroups(groups);
    }
    public static void removeAutoReplyFilterGroup(long dialogId) {
        java.util.Set<String> groups = new java.util.HashSet<>(getAutoReplyFilterGroups());
        groups.remove(String.valueOf(dialogId));
        setAutoReplyFilterGroups(groups);
    }
    public static boolean isGroupInAutoReplyFilter(long dialogId) {
        return getAutoReplyFilterGroups().contains(String.valueOf(dialogId));
    }

    // Multiple auto-reply messages
    public static ArrayList<String> getAutoReplyMessages() {
        String defaultJson = "[\"أهلاً بك، سأطلع على رسالتك وأرد عليك قريباً.\",\"شكراً على الإشارة، سأرجع لك بأقرب وقت.\",\"تم الاستلام، سأرد عليك في أقرب فرصة إن شاء الله.\"]";
        String json = getPrefs().getString("autoReplyMessages", defaultJson);
        ArrayList<String> list = new ArrayList<>();
        try {
            org.json.JSONArray array = new org.json.JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                list.add(array.getString(i));
            }
        } catch (Exception e) {
            org.telegram.messenger.FileLog.e(e);
        }
        return list;
    }
    public static void setAutoReplyMessages(ArrayList<String> list) {
        org.json.JSONArray array = new org.json.JSONArray();
        for (String s : list) {
            array.put(s);
        }
        getPrefs().edit().putString("autoReplyMessages", array.toString()).apply();
    }

    // Auto-reply log
    public static class AutoReplyLogEntry {
        public String groupName;
        public String senderName;
        public String replyText;
        public long timestamp;
        public long groupId;
        public long senderId;
        public int messageId;
        public AutoReplyLogEntry(String groupName, String senderName, String replyText, long timestamp, long groupId, long senderId, int messageId) {
            this.groupName = groupName;
            this.senderName = senderName;
            this.replyText = replyText;
            this.timestamp = timestamp;
            this.groupId = groupId;
            this.senderId = senderId;
            this.messageId = messageId;
        }
    }
    public static ArrayList<AutoReplyLogEntry> getAutoReplyLog() {
        String json = getPrefs().getString("autoReplyLog", "[]");
        ArrayList<AutoReplyLogEntry> list = new ArrayList<>();
        try {
            org.json.JSONArray array = new org.json.JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                org.json.JSONObject obj = array.getJSONObject(i);
                list.add(new AutoReplyLogEntry(
                        obj.optString("group", ""),
                        obj.optString("sender", ""),
                        obj.optString("text", ""),
                        obj.optLong("time", 0),
                        obj.optLong("groupId", 0),
                        obj.optLong("senderId", 0),
                        obj.optInt("messageId", 0)
                ));
            }
        } catch (Exception e) {
            org.telegram.messenger.FileLog.e(e);
        }
        return list;
    }
    public static void addAutoReplyLogEntry(String groupName, String senderName, String replyText, long groupId, long senderId, int messageId) {
        ArrayList<AutoReplyLogEntry> log = getAutoReplyLog();
        log.add(0, new AutoReplyLogEntry(groupName, senderName, replyText, System.currentTimeMillis(), groupId, senderId, messageId));
        if (log.size() > 50) log = new ArrayList<>(log.subList(0, 50));
        org.json.JSONArray array = new org.json.JSONArray();
        try {
            for (AutoReplyLogEntry entry : log) {
                org.json.JSONObject obj = new org.json.JSONObject();
                obj.put("group", entry.groupName);
                obj.put("sender", entry.senderName);
                obj.put("text", entry.replyText);
                obj.put("time", entry.timestamp);
                obj.put("groupId", entry.groupId);
                obj.put("senderId", entry.senderId);
                obj.put("messageId", entry.messageId);
                array.put(obj);
            }
        } catch (Exception e) {
            org.telegram.messenger.FileLog.e(e);
        }
        getPrefs().edit().putString("autoReplyLog", array.toString()).apply();
    }
    @Deprecated
    public static void addAutoReplyLogEntry(String groupName, String senderName, String replyText, long groupId, long senderId) {
        addAutoReplyLogEntry(groupName, senderName, replyText, groupId, senderId, 0);
    }
    @Deprecated
    public static void addAutoReplyLogEntry(String groupName, String senderName, String replyText) {
        addAutoReplyLogEntry(groupName, senderName, replyText, 0, 0, 0);
    }
    public static void clearAutoReplyLog() {
        getPrefs().edit().putString("autoReplyLog", "[]").apply();
    }

    // Quick Replies
    public static class QuickReplyItem {
        public String label;
        public String value;
        public QuickReplyItem(String label, String value) {
            this.label = label;
            this.value = value;
        }
    }

    public static ArrayList<QuickReplyItem> getQuickReplies() {
        String defaultJson = "[" +
                "{\"label\":\"سلام\",\"value\":\"السلام عليكم ورحمة الله وبركاته\"}," +
                "{\"label\":\"وعليكم\",\"value\":\"وعليكم السلام ورحمة الله وبركاته\"}," +
                "{\"label\":\"شكر\",\"value\":\"شكراً جزيلاً لك على لطفك وتعاونك\"}," +
                "{\"label\":\"اهل\",\"value\":\"أهلاً وسهلاً بك، حياك الله\"}," +
                "{\"label\":\"جزاك\",\"value\":\"جزاك الله خيراً وبارك فيك\"}," +
                "{\"label\":\"انشاءالله\",\"value\":\"إن شاء الله تعالى\"}," +
                "{\"label\":\"الحمدلله\",\"value\":\"الحمد لله رب العالمين\"}," +
                "{\"label\":\"لاحول\",\"value\":\"لا حول ولا قوة إلا بالله العلي العظيم\"}," +
                "{\"label\":\"امان\",\"value\":\"في أمان الله ورعايته، مع السلامة\"}," +
                "{\"label\":\"صلي\",\"value\":\"اللهم صل وسلم وبارك على نبينا محمد وعلى آله وصحبه أجمعين\"}" +
                "]";
        if (getPrefs().getInt("quickRepliesVersion", 1) < 2) {
            getPrefs().edit()
                    .putString("quickRepliesJson", defaultJson)
                    .putInt("quickRepliesVersion", 2)
                    .apply();
        }

        String json = getPrefs().getString("quickRepliesJson", defaultJson);
        ArrayList<QuickReplyItem> list = new ArrayList<>();
        try {
            org.json.JSONArray array = new org.json.JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                org.json.JSONObject obj = array.getJSONObject(i);
                list.add(new QuickReplyItem(obj.getString("label"), obj.getString("value")));
            }
        } catch (Exception e) {
            org.telegram.messenger.FileLog.e(e);
        }
        return list;
    }

    public static void setQuickReplies(ArrayList<QuickReplyItem> list) {
        org.json.JSONArray array = new org.json.JSONArray();
        try {
            for (QuickReplyItem item : list) {
                org.json.JSONObject obj = new org.json.JSONObject();
                obj.put("label", item.label);
                obj.put("value", item.value);
                array.put(obj);
            }
        } catch (Exception e) {
            org.telegram.messenger.FileLog.e(e);
        }
        getPrefs().edit().putString("quickRepliesJson", array.toString()).apply();
    }

    // draftsManagerEnabled
    public static boolean draftsManagerEnabled = getPrefs().getBoolean("draftsManagerEnabled", true);
    public static void toggleDraftsManagerEnabled() {
        draftsManagerEnabled = !draftsManagerEnabled;
        getPrefs().edit().putBoolean("draftsManagerEnabled", draftsManagerEnabled).apply();
    }
}

