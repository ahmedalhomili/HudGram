package com.hudgram.core;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import org.telegram.messenger.ApplicationLoader;

public class HudConfig {

    public static final int TRANS_TYPE_HUD = 0;
    public static final int TRANS_TYPE_TG = 1;

    public static final int ID_TYPE_HIDDEN = 0;
    public static final int ID_TYPE_API = 1;
    public static final int ID_TYPE_BOTAPI = 2;

    // Account tracking for multi-account support
    private static int currentAccount = 0;
    private static final boolean[] accountMigrated = new boolean[4];

    public static int getCurrentAccount() {
        return currentAccount;
    }

    public static void switchAccount(int account) {
        if (account < 0 || account >= 4) return;
        currentAccount = account;
        ensureMigration(account);
        reloadConfig();
    }

    // Global prefs — shared across all accounts (UI settings)
    private static SharedPreferences getPrefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences("hudgram_settings", Context.MODE_PRIVATE);
    }

    // Per-account prefs
    private static SharedPreferences getAccountPrefs() {
        return getAccountPrefs(currentAccount);
    }

    private static SharedPreferences getAccountPrefs(int account) {
        String name = account == 0 ? "hudgram_account_settings" : "hudgram_account_settings_" + account;
        SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences(name, Context.MODE_PRIVATE);
        if (!accountMigrated[account]) {
            ensureMigration(account);
        }
        return prefs;
    }

    private static void ensureMigration(int account) {
        if (accountMigrated[account]) return;
        String name = account == 0 ? "hudgram_account_settings" : "hudgram_account_settings_" + account;
        SharedPreferences accountPrefs = ApplicationLoader.applicationContext.getSharedPreferences(name, Context.MODE_PRIVATE);
        if (accountPrefs.getBoolean("migrated_from_global_v1", false)) {
            accountMigrated[account] = true;
            return;
        }
        SharedPreferences globalPrefs = getPrefs();
        SharedPreferences.Editor editor = accountPrefs.edit();

        // Migrate boolean keys
        String[] boolKeys = {
            "quickReplyEnabled", "scheduledMessagesEnabled", "draftsManagerEnabled",
            "autoReplyMentionEnabled", "autoReplyScheduleEnabled",
            "autoReplyDMEnabled", "translationEnabled", "autoTranslate", "showOriginal"
        };
        for (String key : boolKeys) {
            if (globalPrefs.contains(key)) editor.putBoolean(key, globalPrefs.getBoolean(key, false));
        }
        // Fix defaults that should be true
        if (!globalPrefs.contains("quickReplyEnabled")) editor.putBoolean("quickReplyEnabled", true);
        if (!globalPrefs.contains("scheduledMessagesEnabled")) editor.putBoolean("scheduledMessagesEnabled", true);
        if (!globalPrefs.contains("draftsManagerEnabled")) editor.putBoolean("draftsManagerEnabled", true);
        if (!globalPrefs.contains("translationEnabled")) editor.putBoolean("translationEnabled", true);
        if (!globalPrefs.contains("autoTranslate")) editor.putBoolean("autoTranslate", true);
        if (!globalPrefs.contains("showOriginal")) editor.putBoolean("showOriginal", true);

        // Migrate int keys
        String[] intKeys = {
            "autoReplyMentionCooldown", "autoReplyMode", "autoReplyCooldownMode",
            "autoReplyScheduleStartHour", "autoReplyScheduleStartMinute",
            "autoReplyScheduleEndHour", "autoReplyScheduleEndMinute",
            "autoReplyFilterMode", "transType", "quickRepliesVersion"
        };
        for (String key : intKeys) {
            if (globalPrefs.contains(key)) editor.putInt(key, globalPrefs.getInt(key, 0));
        }

        // Migrate String keys
        String[] stringKeys = {
            "autoReplyMentionText", "autoReplyMorningText", "autoReplyAfternoonText",
            "autoReplyEveningText", "autoReplyNightText", "autoReplyMessages",
            "autoReplyLog", "autoReplyDMRules", "autoReplyDMLog",
            "quickRepliesJson", "translationProvider", "translationTarget"
        };
        for (String key : stringKeys) {
            if (globalPrefs.contains(key)) editor.putString(key, globalPrefs.getString(key, ""));
        }

        // Migrate StringSet keys
        if (globalPrefs.contains("autoReplyFilterGroups")) {
            editor.putStringSet("autoReplyFilterGroups", globalPrefs.getStringSet("autoReplyFilterGroups", new java.util.HashSet<>()));
        }
        if (globalPrefs.contains("restrictedLanguages")) {
            editor.putStringSet("restrictedLanguages", globalPrefs.getStringSet("restrictedLanguages", null));
        }

        // Migrate dmFirstMsg_ keys
        java.util.Map<String, ?> allEntries = globalPrefs.getAll();
        for (java.util.Map.Entry<String, ?> entry : allEntries.entrySet()) {
            if (entry.getKey().startsWith("dmFirstMsg_") && entry.getValue() instanceof String) {
                editor.putString(entry.getKey(), (String) entry.getValue());
            }
        }

        editor.putBoolean("migrated_from_global_v1", true);
        editor.apply();
        accountMigrated[account] = true;
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

    // showDefaultTabs
    public static boolean showDefaultTabs = getPrefs().getBoolean("showDefaultTabs", true);
    public static void toggleShowDefaultTabs() {
        showDefaultTabs = !showDefaultTabs;
        getPrefs().edit().putBoolean("showDefaultTabs", showDefaultTabs).apply();
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
    public static boolean showOriginal = getAccountPrefs().getBoolean("showOriginal", true);
    public static void toggleShowOriginal() {
        showOriginal = !showOriginal;
        getAccountPrefs().edit().putBoolean("showOriginal", showOriginal).apply();
    }

    // autoTranslate
    public static boolean autoTranslate = getAccountPrefs().getBoolean("autoTranslate", true);
    public static void toggleAutoTranslate() {
        autoTranslate = !autoTranslate;
        getAccountPrefs().edit().putBoolean("autoTranslate", autoTranslate).apply();
    }


    // translationProvider
    public static String translationProvider = getAccountPrefs().getString("translationProvider", "google");
    public static void setTranslationProvider(String provider) {
        translationProvider = provider;
        getAccountPrefs().edit().putString("translationProvider", provider).apply();
    }

    // translationTarget
    public static String translationTarget = getAccountPrefs().getString("translationTarget", "app");
    public static void setTranslationTarget(String target) {
        translationTarget = target;
        getAccountPrefs().edit().putString("translationTarget", target).apply();
    }

    // translationEnabled
    public static boolean translationEnabled = getAccountPrefs().getBoolean("translationEnabled", true);
    public static void toggleTranslationEnabled() {
        translationEnabled = !translationEnabled;
        getAccountPrefs().edit().putBoolean("translationEnabled", translationEnabled).apply();
    }

    // transType
    public static int transType = getAccountPrefs().getInt("transType", TRANS_TYPE_HUD);
    public static void setTransType(int type) {
        transType = type;
        getAccountPrefs().edit().putInt("transType", type).apply();
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
    private static Set<String> restrictedLanguagesSet = getAccountPrefs().getStringSet("restrictedLanguages", null);
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
        getAccountPrefs().edit().putStringSet("restrictedLanguages", restrictedLanguagesSet).apply();
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
    public static boolean quickReplyEnabled = getAccountPrefs().getBoolean("quickReplyEnabled", true);
    public static void toggleQuickReplyEnabled() {
        quickReplyEnabled = !quickReplyEnabled;
        getAccountPrefs().edit().putBoolean("quickReplyEnabled", quickReplyEnabled).apply();
    }

    // scheduledMessagesEnabled
    public static boolean scheduledMessagesEnabled = getAccountPrefs().getBoolean("scheduledMessagesEnabled", true);
    public static void toggleScheduledMessagesEnabled() {
        scheduledMessagesEnabled = !scheduledMessagesEnabled;
        getAccountPrefs().edit().putBoolean("scheduledMessagesEnabled", scheduledMessagesEnabled).apply();
    }

    // autoReplyMentionEnabled
    public static boolean autoReplyMentionEnabled = getAccountPrefs().getBoolean("autoReplyMentionEnabled", false);
    public static void toggleAutoReplyMentionEnabled() {
        autoReplyMentionEnabled = !autoReplyMentionEnabled;
        getAccountPrefs().edit().putBoolean("autoReplyMentionEnabled", autoReplyMentionEnabled).apply();
    }

    // autoReplyMentionText
    public static String autoReplyMentionText = getAccountPrefs().getString("autoReplyMentionText", "أهلاً بك، سأطلع على رسالتك وأرد عليك قريباً.");
    public static void setAutoReplyMentionText(String text) {
        autoReplyMentionText = text;
        getAccountPrefs().edit().putString("autoReplyMentionText", autoReplyMentionText).apply();
    }

    // autoReplyMentionCooldown
    public static int autoReplyMentionCooldown = getAccountPrefs().getInt("autoReplyMentionCooldown", 30);
    public static void setAutoReplyMentionCooldown(int cooldown) {
        autoReplyMentionCooldown = cooldown;
        getAccountPrefs().edit().putInt("autoReplyMentionCooldown", autoReplyMentionCooldown).apply();
    }

    // autoReplyMode: 0=single, 1=multiple, 2=smart
    public static int autoReplyMode = getAccountPrefs().getInt("autoReplyMode", 0);
    public static void setAutoReplyMode(int mode) {
        autoReplyMode = mode;
        getAccountPrefs().edit().putInt("autoReplyMode", mode).apply();
    }

    // autoReplyCooldownMode: 0=per-group, 1=per-sender
    public static int autoReplyCooldownMode = getAccountPrefs().getInt("autoReplyCooldownMode", 1);
    public static void setAutoReplyCooldownMode(int mode) {
        autoReplyCooldownMode = mode;
        getAccountPrefs().edit().putInt("autoReplyCooldownMode", mode).apply();
    }

    // Schedule
    public static boolean autoReplyScheduleEnabled = getAccountPrefs().getBoolean("autoReplyScheduleEnabled", false);
    public static void toggleAutoReplyScheduleEnabled() {
        autoReplyScheduleEnabled = !autoReplyScheduleEnabled;
        getAccountPrefs().edit().putBoolean("autoReplyScheduleEnabled", autoReplyScheduleEnabled).apply();
    }
    public static int autoReplyScheduleStartHour = getAccountPrefs().getInt("autoReplyScheduleStartHour", 23);
    public static int autoReplyScheduleStartMinute = getAccountPrefs().getInt("autoReplyScheduleStartMinute", 0);
    public static void setAutoReplyScheduleStart(int hour, int minute) {
        autoReplyScheduleStartHour = hour;
        autoReplyScheduleStartMinute = minute;
        getAccountPrefs().edit().putInt("autoReplyScheduleStartHour", hour).putInt("autoReplyScheduleStartMinute", minute).apply();
    }
    public static int autoReplyScheduleEndHour = getAccountPrefs().getInt("autoReplyScheduleEndHour", 8);
    public static int autoReplyScheduleEndMinute = getAccountPrefs().getInt("autoReplyScheduleEndMinute", 0);
    public static void setAutoReplyScheduleEnd(int hour, int minute) {
        autoReplyScheduleEndHour = hour;
        autoReplyScheduleEndMinute = minute;
        getAccountPrefs().edit().putInt("autoReplyScheduleEndHour", hour).putInt("autoReplyScheduleEndMinute", minute).apply();
    }

    // Smart time-based reply messages
    public static String autoReplyMorningText = getAccountPrefs().getString("autoReplyMorningText", "صباح الخير، سأرد عليك بعد قليل إن شاء الله.");
    public static void setAutoReplyMorningText(String text) {
        autoReplyMorningText = text;
        getAccountPrefs().edit().putString("autoReplyMorningText", text).apply();
    }
    public static String autoReplyAfternoonText = getAccountPrefs().getString("autoReplyAfternoonText", "أهلاً، سأطلع على رسالتك وأرد عليك قريباً إن شاء الله.");
    public static void setAutoReplyAfternoonText(String text) {
        autoReplyAfternoonText = text;
        getAccountPrefs().edit().putString("autoReplyAfternoonText", text).apply();
    }
    public static String autoReplyEveningText = getAccountPrefs().getString("autoReplyEveningText", "مساء الخير، سأرجع لك بأقرب وقت إن شاء الله.");
    public static void setAutoReplyEveningText(String text) {
        autoReplyEveningText = text;
        getAccountPrefs().edit().putString("autoReplyEveningText", text).apply();
    }
    public static String autoReplyNightText = getAccountPrefs().getString("autoReplyNightText", "شكراً على رسالتك، سأرد عليك صباحاً إن شاء الله.");
    public static void setAutoReplyNightText(String text) {
        autoReplyNightText = text;
        getAccountPrefs().edit().putString("autoReplyNightText", text).apply();
    }

    // Group filter: 0=all, 1=whitelist, 2=blacklist
    public static int autoReplyFilterMode = getAccountPrefs().getInt("autoReplyFilterMode", 0);
    public static void setAutoReplyFilterMode(int mode) {
        autoReplyFilterMode = mode;
        getAccountPrefs().edit().putInt("autoReplyFilterMode", mode).apply();
    }
    public static java.util.Set<String> getAutoReplyFilterGroups() {
        return getAccountPrefs().getStringSet("autoReplyFilterGroups", new java.util.HashSet<>());
    }
    public static void setAutoReplyFilterGroups(java.util.Set<String> groups) {
        getAccountPrefs().edit().putStringSet("autoReplyFilterGroups", groups).apply();
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
        String json = getAccountPrefs().getString("autoReplyMessages", defaultJson);
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
        getAccountPrefs().edit().putString("autoReplyMessages", array.toString()).apply();
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
        String json = getAccountPrefs().getString("autoReplyLog", "[]");
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
        getAccountPrefs().edit().putString("autoReplyLog", array.toString()).apply();
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
        getAccountPrefs().edit().putString("autoReplyLog", "[]").apply();
    }

    // === Auto-Reply DM Pro ===

    // Master toggle
    public static boolean autoReplyDMEnabled = getAccountPrefs().getBoolean("autoReplyDMEnabled", false);
    public static void toggleAutoReplyDMEnabled() {
        autoReplyDMEnabled = !autoReplyDMEnabled;
        getAccountPrefs().edit().putBoolean("autoReplyDMEnabled", autoReplyDMEnabled).apply();
    }

    // AutoReplyDMRule model
    public static class AutoReplyDMRule {
        public String id;
        public String name;
        public boolean enabled;

        // Match
        public int matchMode;       // 0=all, 1=contains, 2=equals
        public String matchKeyword;

        // Reply
        public int replyMode;       // 0=single, 1=multiple, 2=smart
        public String replyText;
        public ArrayList<String> replyTexts;
        public String morningText;
        public String afternoonText;
        public String eveningText;
        public String nightText;

        // Scope
        public int scope;           // 0=both, 1=groups, 2=private

        // Advanced
        public int delay;           // 1-10 seconds
        public int cooldown;        // 10-600 seconds
        public boolean firstMessageOnly;
        public boolean excludeBots;
        public boolean excludeForwarded;

        // Schedule
        public boolean scheduleEnabled;
        public int scheduleStartHour, scheduleStartMinute;
        public int scheduleEndHour, scheduleEndMinute;

        // Filter
        public int filterMode;     // 0=all, 1=whitelist, 2=blacklist
        public Set<String> filterChats;

        public AutoReplyDMRule() {
            this.id = java.util.UUID.randomUUID().toString();
            this.name = "";
            this.enabled = true;
            this.matchMode = 0;
            this.matchKeyword = "";
            this.replyMode = 0;
            this.replyText = "";
            this.replyTexts = new ArrayList<>();
            this.morningText = "صباح الخير، سأرد عليك بعد قليل إن شاء الله.";
            this.afternoonText = "أهلاً، سأطلع على رسالتك وأرد عليك قريباً إن شاء الله.";
            this.eveningText = "مساء الخير، سأرجع لك بأقرب وقت إن شاء الله.";
            this.nightText = "شكراً على رسالتك، سأرد عليك صباحاً إن شاء الله.";
            this.scope = 0;
            this.delay = 1;
            this.cooldown = 30;
            this.firstMessageOnly = false;
            this.excludeBots = true;
            this.excludeForwarded = false;
            this.scheduleEnabled = false;
            this.scheduleStartHour = 23;
            this.scheduleStartMinute = 0;
            this.scheduleEndHour = 8;
            this.scheduleEndMinute = 0;
            this.filterMode = 0;
            this.filterChats = new HashSet<>();
        }

        public AutoReplyDMRule copy() {
            AutoReplyDMRule c = new AutoReplyDMRule();
            c.id = java.util.UUID.randomUUID().toString();
            c.name = this.name + " (copy)";
            c.enabled = this.enabled;
            c.matchMode = this.matchMode;
            c.matchKeyword = this.matchKeyword;
            c.replyMode = this.replyMode;
            c.replyText = this.replyText;
            c.replyTexts = new ArrayList<>(this.replyTexts);
            c.morningText = this.morningText;
            c.afternoonText = this.afternoonText;
            c.eveningText = this.eveningText;
            c.nightText = this.nightText;
            c.scope = this.scope;
            c.delay = this.delay;
            c.cooldown = this.cooldown;
            c.firstMessageOnly = this.firstMessageOnly;
            c.excludeBots = this.excludeBots;
            c.excludeForwarded = this.excludeForwarded;
            c.scheduleEnabled = this.scheduleEnabled;
            c.scheduleStartHour = this.scheduleStartHour;
            c.scheduleStartMinute = this.scheduleStartMinute;
            c.scheduleEndHour = this.scheduleEndHour;
            c.scheduleEndMinute = this.scheduleEndMinute;
            c.filterMode = this.filterMode;
            c.filterChats = new HashSet<>(this.filterChats);
            return c;
        }

        public org.json.JSONObject toJson() {
            try {
                org.json.JSONObject obj = new org.json.JSONObject();
                obj.put("id", id);
                obj.put("name", name);
                obj.put("enabled", enabled);
                obj.put("matchMode", matchMode);
                obj.put("matchKeyword", matchKeyword != null ? matchKeyword : "");
                obj.put("replyMode", replyMode);
                obj.put("replyText", replyText != null ? replyText : "");
                org.json.JSONArray textsArr = new org.json.JSONArray();
                if (replyTexts != null) {
                    for (String t : replyTexts) textsArr.put(t);
                }
                obj.put("replyTexts", textsArr);
                obj.put("morningText", morningText != null ? morningText : "");
                obj.put("afternoonText", afternoonText != null ? afternoonText : "");
                obj.put("eveningText", eveningText != null ? eveningText : "");
                obj.put("nightText", nightText != null ? nightText : "");
                obj.put("scope", scope);
                obj.put("delay", delay);
                obj.put("cooldown", cooldown);
                obj.put("firstMessageOnly", firstMessageOnly);
                obj.put("excludeBots", excludeBots);
                obj.put("excludeForwarded", excludeForwarded);
                obj.put("scheduleEnabled", scheduleEnabled);
                obj.put("scheduleStartHour", scheduleStartHour);
                obj.put("scheduleStartMinute", scheduleStartMinute);
                obj.put("scheduleEndHour", scheduleEndHour);
                obj.put("scheduleEndMinute", scheduleEndMinute);
                obj.put("filterMode", filterMode);
                org.json.JSONArray filterArr = new org.json.JSONArray();
                if (filterChats != null) {
                    for (String c : filterChats) filterArr.put(c);
                }
                obj.put("filterChats", filterArr);
                return obj;
            } catch (Exception e) {
                org.telegram.messenger.FileLog.e(e);
                return new org.json.JSONObject();
            }
        }

        public static AutoReplyDMRule fromJson(org.json.JSONObject obj) {
            AutoReplyDMRule rule = new AutoReplyDMRule();
            rule.id = obj.optString("id", rule.id);
            rule.name = obj.optString("name", "");
            rule.enabled = obj.optBoolean("enabled", true);
            rule.matchMode = obj.optInt("matchMode", 0);
            rule.matchKeyword = obj.optString("matchKeyword", "");
            rule.replyMode = obj.optInt("replyMode", 0);
            rule.replyText = obj.optString("replyText", "");
            rule.replyTexts = new ArrayList<>();
            org.json.JSONArray textsArr = obj.optJSONArray("replyTexts");
            if (textsArr != null) {
                for (int i = 0; i < textsArr.length(); i++) {
                    rule.replyTexts.add(textsArr.optString(i, ""));
                }
            }
            rule.morningText = obj.optString("morningText", rule.morningText);
            rule.afternoonText = obj.optString("afternoonText", rule.afternoonText);
            rule.eveningText = obj.optString("eveningText", rule.eveningText);
            rule.nightText = obj.optString("nightText", rule.nightText);
            rule.scope = obj.optInt("scope", 0);
            rule.delay = obj.optInt("delay", 1);
            rule.cooldown = obj.optInt("cooldown", 30);
            rule.firstMessageOnly = obj.optBoolean("firstMessageOnly", false);
            rule.excludeBots = obj.optBoolean("excludeBots", true);
            rule.excludeForwarded = obj.optBoolean("excludeForwarded", false);
            rule.scheduleEnabled = obj.optBoolean("scheduleEnabled", false);
            rule.scheduleStartHour = obj.optInt("scheduleStartHour", 23);
            rule.scheduleStartMinute = obj.optInt("scheduleStartMinute", 0);
            rule.scheduleEndHour = obj.optInt("scheduleEndHour", 8);
            rule.scheduleEndMinute = obj.optInt("scheduleEndMinute", 0);
            rule.filterMode = obj.optInt("filterMode", 0);
            rule.filterChats = new HashSet<>();
            org.json.JSONArray filterArr = obj.optJSONArray("filterChats");
            if (filterArr != null) {
                for (int i = 0; i < filterArr.length(); i++) {
                    rule.filterChats.add(filterArr.optString(i, ""));
                }
            }
            return rule;
        }
    }

    // CRUD operations for AutoReplyDMRule
    public static ArrayList<AutoReplyDMRule> getAutoReplyDMRules() {
        String json = getAccountPrefs().getString("autoReplyDMRules", "[]");
        ArrayList<AutoReplyDMRule> list = new ArrayList<>();
        try {
            org.json.JSONArray array = new org.json.JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                list.add(AutoReplyDMRule.fromJson(array.getJSONObject(i)));
            }
        } catch (Exception e) {
            org.telegram.messenger.FileLog.e(e);
        }
        return list;
    }

    private static void saveAutoReplyDMRules(ArrayList<AutoReplyDMRule> rules) {
        org.json.JSONArray array = new org.json.JSONArray();
        for (AutoReplyDMRule rule : rules) {
            array.put(rule.toJson());
        }
        getAccountPrefs().edit().putString("autoReplyDMRules", array.toString()).apply();
    }

    public static void addAutoReplyDMRule(AutoReplyDMRule rule) {
        ArrayList<AutoReplyDMRule> rules = getAutoReplyDMRules();
        rules.add(rule);
        saveAutoReplyDMRules(rules);
    }

    public static void updateAutoReplyDMRule(AutoReplyDMRule updatedRule) {
        ArrayList<AutoReplyDMRule> rules = getAutoReplyDMRules();
        for (int i = 0; i < rules.size(); i++) {
            if (rules.get(i).id.equals(updatedRule.id)) {
                rules.set(i, updatedRule);
                break;
            }
        }
        saveAutoReplyDMRules(rules);
    }

    public static void deleteAutoReplyDMRule(String ruleId) {
        ArrayList<AutoReplyDMRule> rules = getAutoReplyDMRules();
        for (int i = 0; i < rules.size(); i++) {
            if (rules.get(i).id.equals(ruleId)) {
                rules.remove(i);
                break;
            }
        }
        saveAutoReplyDMRules(rules);
    }

    public static void toggleAutoReplyDMRule(String ruleId) {
        ArrayList<AutoReplyDMRule> rules = getAutoReplyDMRules();
        for (AutoReplyDMRule rule : rules) {
            if (rule.id.equals(ruleId)) {
                rule.enabled = !rule.enabled;
                break;
            }
        }
        saveAutoReplyDMRules(rules);
    }

    // Auto-Reply DM Log
    public static class AutoReplyDMLogEntry {
        public String ruleName;
        public String chatName;
        public String senderName;
        public String replyText;
        public long timestamp;
        public long chatId;
        public long senderId;
        public AutoReplyDMLogEntry(String ruleName, String chatName, String senderName, String replyText, long timestamp, long chatId, long senderId) {
            this.ruleName = ruleName;
            this.chatName = chatName;
            this.senderName = senderName;
            this.replyText = replyText;
            this.timestamp = timestamp;
            this.chatId = chatId;
            this.senderId = senderId;
        }
    }

    public static ArrayList<AutoReplyDMLogEntry> getAutoReplyDMLog() {
        String json = getAccountPrefs().getString("autoReplyDMLog", "[]");
        ArrayList<AutoReplyDMLogEntry> list = new ArrayList<>();
        try {
            org.json.JSONArray array = new org.json.JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                org.json.JSONObject obj = array.getJSONObject(i);
                list.add(new AutoReplyDMLogEntry(
                        obj.optString("rule", ""),
                        obj.optString("chat", ""),
                        obj.optString("sender", ""),
                        obj.optString("text", ""),
                        obj.optLong("time", 0),
                        obj.optLong("chatId", 0),
                        obj.optLong("senderId", 0)
                ));
            }
        } catch (Exception e) {
            org.telegram.messenger.FileLog.e(e);
        }
        return list;
    }

    public static void addAutoReplyDMLogEntry(String ruleName, String chatName, String senderName, String replyText, long chatId, long senderId) {
        ArrayList<AutoReplyDMLogEntry> log = getAutoReplyDMLog();
        log.add(0, new AutoReplyDMLogEntry(ruleName, chatName, senderName, replyText, System.currentTimeMillis(), chatId, senderId));
        if (log.size() > 100) log = new ArrayList<>(log.subList(0, 100));
        org.json.JSONArray array = new org.json.JSONArray();
        try {
            for (AutoReplyDMLogEntry entry : log) {
                org.json.JSONObject obj = new org.json.JSONObject();
                obj.put("rule", entry.ruleName);
                obj.put("chat", entry.chatName);
                obj.put("sender", entry.senderName);
                obj.put("text", entry.replyText);
                obj.put("time", entry.timestamp);
                obj.put("chatId", entry.chatId);
                obj.put("senderId", entry.senderId);
                array.put(obj);
            }
        } catch (Exception e) {
            org.telegram.messenger.FileLog.e(e);
        }
        getAccountPrefs().edit().putString("autoReplyDMLog", array.toString()).apply();
    }

    public static void clearAutoReplyDMLog() {
        getAccountPrefs().edit().putString("autoReplyDMLog", "[]").apply();
    }

    // First-message-per-day tracker
    public static boolean hasAutoReplyDMRepliedInChat(String ruleId, long dialogId) {
        String key = "dmFirstMsg_" + ruleId + "_" + dialogId;
        String today = new java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(new java.util.Date());
        return today.equals(getAccountPrefs().getString(key, ""));
    }

    public static void markAutoReplyDMRepliedInChat(String ruleId, long dialogId) {
        String key = "dmFirstMsg_" + ruleId + "_" + dialogId;
        String today = new java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(new java.util.Date());
        getAccountPrefs().edit().putString(key, today).apply();
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
        if (getAccountPrefs().getInt("quickRepliesVersion", 1) < 2) {
            getAccountPrefs().edit()
                    .putString("quickRepliesJson", defaultJson)
                    .putInt("quickRepliesVersion", 2)
                    .apply();
        }

        String json = getAccountPrefs().getString("quickRepliesJson", defaultJson);
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
        getAccountPrefs().edit().putString("quickRepliesJson", array.toString()).apply();
    }

    // draftsManagerEnabled
    public static boolean draftsManagerEnabled = getAccountPrefs().getBoolean("draftsManagerEnabled", true);
    public static void toggleDraftsManagerEnabled() {
        draftsManagerEnabled = !draftsManagerEnabled;
        getAccountPrefs().edit().putBoolean("draftsManagerEnabled", draftsManagerEnabled).apply();
    }

    // showChatToolsFab
    public static boolean showChatToolsFab = getPrefs().getBoolean("showChatToolsFab", true);
    public static void toggleShowChatToolsFab() {
        showChatToolsFab = !showChatToolsFab;
        getPrefs().edit().putBoolean("showChatToolsFab", showChatToolsFab).apply();
    }

    public static String exportBackup() {
        try {
            org.json.JSONObject backupJson = new org.json.JSONObject();
            // Export global settings
            SharedPreferences globalPrefs = getPrefs();
            java.util.Map<String, ?> globalEntries = globalPrefs.getAll();
            for (java.util.Map.Entry<String, ?> entry : globalEntries.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (value instanceof java.util.Set) {
                    org.json.JSONArray array = new org.json.JSONArray();
                    for (String s : (java.util.Set<String>) value) {
                        array.put(s);
                    }
                    backupJson.put(key, array);
                } else {
                    backupJson.put(key, value);
                }
            }
            // Export per-account settings
            SharedPreferences accountPrefs = getAccountPrefs();
            java.util.Map<String, ?> accountEntries = accountPrefs.getAll();
            for (java.util.Map.Entry<String, ?> entry : accountEntries.entrySet()) {
                String key = entry.getKey();
                if (key.equals("migrated_from_global_v1")) continue;
                Object value = entry.getValue();
                if (value instanceof java.util.Set) {
                    org.json.JSONArray array = new org.json.JSONArray();
                    for (String s : (java.util.Set<String>) value) {
                        array.put(s);
                    }
                    backupJson.put(key, array);
                } else {
                    backupJson.put(key, value);
                }
            }
            return backupJson.toString();
        } catch (Exception e) {
            org.telegram.messenger.FileLog.e(e);
            return null;
        }
    }

    public static boolean importBackup(String json) {
        try {
            if (android.text.TextUtils.isEmpty(json)) return false;
            org.json.JSONObject backupJson = new org.json.JSONObject(json);
            SharedPreferences.Editor globalEditor = getPrefs().edit();
            SharedPreferences.Editor accountEditor = getAccountPrefs().edit();
            globalEditor.clear();
            accountEditor.clear();
            
            // Per-account keys set
            java.util.Set<String> perAccountKeys = new java.util.HashSet<>(java.util.Arrays.asList(
                "quickReplyEnabled", "scheduledMessagesEnabled", "draftsManagerEnabled",
                "autoReplyMentionEnabled", "autoReplyScheduleEnabled",
                "autoReplyDMEnabled", "translationEnabled", "autoTranslate", "showOriginal",
                "autoReplyMentionCooldown", "autoReplyMode", "autoReplyCooldownMode",
                "autoReplyScheduleStartHour", "autoReplyScheduleStartMinute",
                "autoReplyScheduleEndHour", "autoReplyScheduleEndMinute",
                "autoReplyFilterMode", "transType", "quickRepliesVersion",
                "autoReplyMentionText", "autoReplyMorningText", "autoReplyAfternoonText",
                "autoReplyEveningText", "autoReplyNightText", "autoReplyMessages",
                "autoReplyLog", "autoReplyDMRules", "autoReplyDMLog",
                "quickRepliesJson", "translationProvider", "translationTarget",
                "autoReplyFilterGroups", "restrictedLanguages"
            ));
            
            java.util.Iterator<String> keys = backupJson.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object value = backupJson.get(key);
                boolean isPerAccount = perAccountKeys.contains(key) || key.startsWith("dmFirstMsg_");
                SharedPreferences.Editor editor = isPerAccount ? accountEditor : globalEditor;
                if (value instanceof org.json.JSONArray) {
                    org.json.JSONArray array = (org.json.JSONArray) value;
                    java.util.Set<String> set = new java.util.HashSet<>();
                    for (int i = 0; i < array.length(); i++) {
                        set.add(array.getString(i));
                    }
                    editor.putStringSet(key, set);
                } else if (value instanceof Boolean) {
                    editor.putBoolean(key, (Boolean) value);
                } else if (value instanceof Integer) {
                    editor.putInt(key, (Integer) value);
                } else if (value instanceof Long) {
                    editor.putLong(key, (Long) value);
                } else if (value instanceof Double) {
                    editor.putFloat(key, ((Double) value).floatValue());
                } else if (value instanceof Float) {
                    editor.putFloat(key, (Float) value);
                } else if (value instanceof String) {
                    editor.putString(key, (String) value);
                }
            }
            accountEditor.putBoolean("migrated_from_global_v1", true);
            globalEditor.apply();
            accountEditor.apply();
            reloadConfig();
            return true;
        } catch (Exception e) {
            org.telegram.messenger.FileLog.e(e);
            return false;
        }
    }

    public static void reloadConfig() {
        preferIPv6 = getPrefs().getBoolean("preferIPv6", false);
        hideStoriesBar = getPrefs().getBoolean("hideStoriesBar", true);
        customDoubleTapAction = getPrefs().getInt("customDoubleTapAction", 0);
        showAvatarInHeader = getPrefs().getBoolean("showAvatarInHeader", true);
        showMyNameInHeader = getPrefs().getBoolean("showMyNameInHeader", false);
        showBioAsSubtitle = getPrefs().getBoolean("showBioAsSubtitle", false);
        hideSettingsTab = getPrefs().getBoolean("hideSettingsTab", true);
        hideSearchBar = getPrefs().getBoolean("hideSearchBar", false);
        hideFolderTabs = getPrefs().getBoolean("hideFolderTabs", false);
        showDefaultTabs = getPrefs().getBoolean("showDefaultTabs", true);
        disableInstantCamera = getPrefs().getBoolean("disableInstantCamera", false);
        askBeforeCall = getPrefs().getBoolean("askBeforeCall", false);
        openArchiveOnPull = getPrefs().getBoolean("openArchiveOnPull", false);
        accentAsNotificationColor = getPrefs().getBoolean("accentAsNotificationColor", false);
        silenceNonContacts = getPrefs().getBoolean("silenceNonContacts", false);
        showOriginal = getAccountPrefs().getBoolean("showOriginal", true);
        autoTranslate = getAccountPrefs().getBoolean("autoTranslate", true);
        translationProvider = getAccountPrefs().getString("translationProvider", "google");
        translationTarget = getAccountPrefs().getString("translationTarget", "app");
        translationEnabled = getAccountPrefs().getBoolean("translationEnabled", true);
        transType = getAccountPrefs().getInt("transType", TRANS_TYPE_HUD);
        nameOrder = getPrefs().getInt("nameOrder", 1);
        idType = getPrefs().getInt("idType", ID_TYPE_API);
        restrictedLanguagesSet = getAccountPrefs().getStringSet("restrictedLanguages", null);
        hideNotificationContent = getPrefs().getBoolean("hideNotificationContent", false);
        confirmStickers = getPrefs().getBoolean("confirmStickers", false);
        confirmVoiceMessages = getPrefs().getBoolean("confirmVoiceMessages", false);
        partialCopy = getPrefs().getBoolean("partialCopy", false);
        quickReplyEnabled = getAccountPrefs().getBoolean("quickReplyEnabled", true);
        scheduledMessagesEnabled = getAccountPrefs().getBoolean("scheduledMessagesEnabled", true);
        autoReplyMentionEnabled = getAccountPrefs().getBoolean("autoReplyMentionEnabled", false);
        autoReplyMentionText = getAccountPrefs().getString("autoReplyMentionText", "أهلاً بك، سأطلع على رسالتك وأرد عليك قريباً.");
        autoReplyMentionCooldown = getAccountPrefs().getInt("autoReplyMentionCooldown", 30);
        autoReplyMode = getAccountPrefs().getInt("autoReplyMode", 0);
        autoReplyCooldownMode = getAccountPrefs().getInt("autoReplyCooldownMode", 1);
        autoReplyScheduleEnabled = getAccountPrefs().getBoolean("autoReplyScheduleEnabled", false);
        autoReplyScheduleStartHour = getAccountPrefs().getInt("autoReplyScheduleStartHour", 23);
        autoReplyScheduleStartMinute = getAccountPrefs().getInt("autoReplyScheduleStartMinute", 0);
        autoReplyScheduleEndHour = getAccountPrefs().getInt("autoReplyScheduleEndHour", 8);
        autoReplyScheduleEndMinute = getAccountPrefs().getInt("autoReplyScheduleEndMinute", 0);
        autoReplyMorningText = getAccountPrefs().getString("autoReplyMorningText", "صباح الخير، سأرد عليك بعد قليل إن شاء الله.");
        autoReplyAfternoonText = getAccountPrefs().getString("autoReplyAfternoonText", "أهلاً، سأطلع على رسالتك وأرد عليك قريباً إن شاء الله.");
        autoReplyEveningText = getAccountPrefs().getString("autoReplyEveningText", "مساء الخير، سأرجع لك بأقرب وقت إن شاء الله.");
        autoReplyNightText = getAccountPrefs().getString("autoReplyNightText", "شكراً على رسالتك، سأرد عليك صباحاً إن شاء الله.");
        autoReplyFilterMode = getAccountPrefs().getInt("autoReplyFilterMode", 0);
        draftsManagerEnabled = getAccountPrefs().getBoolean("draftsManagerEnabled", true);
        showChatToolsFab = getPrefs().getBoolean("showChatToolsFab", true);
        autoReplyDMEnabled = getAccountPrefs().getBoolean("autoReplyDMEnabled", false);
    }

    // Per-account settings getters/setters for background tasks
    public static boolean isScheduledMessagesEnabled(int account) {
        return getAccountPrefs(account).getBoolean("scheduledMessagesEnabled", true);
    }
    public static boolean isAutoReplyMentionEnabled(int account) {
        return getAccountPrefs(account).getBoolean("autoReplyMentionEnabled", false);
    }
    public static int getAutoReplyFilterMode(int account) {
        return getAccountPrefs(account).getInt("autoReplyFilterMode", 0);
    }
    public static java.util.Set<String> getAutoReplyFilterGroups(int account) {
        return getAccountPrefs(account).getStringSet("autoReplyFilterGroups", new java.util.HashSet<>());
    }
    public static boolean isGroupInAutoReplyFilter(int account, long dialogId) {
        return getAutoReplyFilterGroups(account).contains(String.valueOf(dialogId));
    }
    public static boolean isAutoReplyScheduleEnabled(int account) {
        return getAccountPrefs(account).getBoolean("autoReplyScheduleEnabled", false);
    }
    public static int getAutoReplyScheduleStartHour(int account) {
        return getAccountPrefs(account).getInt("autoReplyScheduleStartHour", 23);
    }
    public static int getAutoReplyScheduleStartMinute(int account) {
        return getAccountPrefs(account).getInt("autoReplyScheduleStartMinute", 0);
    }
    public static int getAutoReplyScheduleEndHour(int account) {
        return getAccountPrefs(account).getInt("autoReplyScheduleEndHour", 8);
    }
    public static int getAutoReplyScheduleEndMinute(int account) {
        return getAccountPrefs(account).getInt("autoReplyScheduleEndMinute", 0);
    }
    public static int getAutoReplyCooldownMode(int account) {
        return getAccountPrefs(account).getInt("autoReplyCooldownMode", 1);
    }
    public static int getAutoReplyMentionCooldown(int account) {
        return getAccountPrefs(account).getInt("autoReplyMentionCooldown", 30);
    }
    public static int getAutoReplyMode(int account) {
        return getAccountPrefs(account).getInt("autoReplyMode", 0);
    }
    public static ArrayList<String> getAutoReplyMessages(int account) {
        String defaultJson = "[\"أهلاً بك، سأطلع على رسالتك وأرد عليك قريباً.\",\"شكراً على الإشارة، سأرجع لك بأقرب وقت.\",\"تم الاستلام، سأرد عليك في أقرب فرصة إن شاء الله.\"]";
        String json = getAccountPrefs(account).getString("autoReplyMessages", defaultJson);
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
    public static String getAutoReplyMorningText(int account) {
        return getAccountPrefs(account).getString("autoReplyMorningText", "صباح الخير، سأرد عليك بعد قليل إن شاء الله.");
    }
    public static String getAutoReplyAfternoonText(int account) {
        return getAccountPrefs(account).getString("autoReplyAfternoonText", "أهلاً، سأطلع على رسالتك وأرد عليك قريباً إن شاء الله.");
    }
    public static String getAutoReplyEveningText(int account) {
        return getAccountPrefs(account).getString("autoReplyEveningText", "مساء الخير، سأرجع لك بأقرب وقت إن شاء الله.");
    }
    public static String getAutoReplyNightText(int account) {
        return getAccountPrefs(account).getString("autoReplyNightText", "شكراً على رسالتك، سأرد عليك صباحاً إن شاء الله.");
    }
    public static String getAutoReplyMentionText(int account) {
        return getAccountPrefs(account).getString("autoReplyMentionText", "أهلاً بك، سأطلع على رسالتك وأرد عليك قريباً.");
    }
    public static ArrayList<AutoReplyLogEntry> getAutoReplyLog(int account) {
        String json = getAccountPrefs(account).getString("autoReplyLog", "[]");
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
    public static void addAutoReplyLogEntry(int account, String groupName, String senderName, String replyText, long groupId, long senderId, int messageId) {
        ArrayList<AutoReplyLogEntry> log = getAutoReplyLog(account);
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
        getAccountPrefs(account).edit().putString("autoReplyLog", array.toString()).apply();
    }
    public static boolean isAutoReplyDMEnabled(int account) {
        return getAccountPrefs(account).getBoolean("autoReplyDMEnabled", false);
    }
    public static ArrayList<AutoReplyDMRule> getAutoReplyDMRules(int account) {
        String json = getAccountPrefs(account).getString("autoReplyDMRules", "[]");
        ArrayList<AutoReplyDMRule> list = new ArrayList<>();
        try {
            org.json.JSONArray array = new org.json.JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                list.add(AutoReplyDMRule.fromJson(array.getJSONObject(i)));
            }
        } catch (Exception e) {
            org.telegram.messenger.FileLog.e(e);
        }
        return list;
    }
    public static boolean hasAutoReplyDMRepliedInChat(int account, String ruleId, long dialogId) {
        String key = "dmFirstMsg_" + ruleId + "_" + dialogId;
        String today = new java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(new java.util.Date());
        return today.equals(getAccountPrefs(account).getString(key, ""));
    }
    public static void markAutoReplyDMRepliedInChat(int account, String ruleId, long dialogId) {
        String key = "dmFirstMsg_" + ruleId + "_" + dialogId;
        String today = new java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(new java.util.Date());
        getAccountPrefs(account).edit().putString(key, today).apply();
    }
    public static ArrayList<AutoReplyDMLogEntry> getAutoReplyDMLog(int account) {
        String json = getAccountPrefs(account).getString("autoReplyDMLog", "[]");
        ArrayList<AutoReplyDMLogEntry> list = new ArrayList<>();
        try {
            org.json.JSONArray array = new org.json.JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                org.json.JSONObject obj = array.getJSONObject(i);
                list.add(new AutoReplyDMLogEntry(
                        obj.optString("rule", ""),
                        obj.optString("chat", ""),
                        obj.optString("sender", ""),
                        obj.optString("text", ""),
                        obj.optLong("time", 0),
                        obj.optLong("chatId", 0),
                        obj.optLong("senderId", 0)
                ));
            }
        } catch (Exception e) {
            org.telegram.messenger.FileLog.e(e);
        }
        return list;
    }
    public static void addAutoReplyDMLogEntry(int account, String ruleName, String chatName, String senderName, String replyText, long chatId, long senderId) {
        ArrayList<AutoReplyDMLogEntry> log = getAutoReplyDMLog(account);
        log.add(0, new AutoReplyDMLogEntry(ruleName, chatName, senderName, replyText, System.currentTimeMillis(), chatId, senderId));
        if (log.size() > 100) log = new ArrayList<>(log.subList(0, 100));
        org.json.JSONArray array = new org.json.JSONArray();
        try {
            for (AutoReplyDMLogEntry entry : log) {
                org.json.JSONObject obj = new org.json.JSONObject();
                obj.put("rule", entry.ruleName);
                obj.put("chat", entry.chatName);
                obj.put("sender", entry.senderName);
                obj.put("text", entry.replyText);
                obj.put("time", entry.timestamp);
                obj.put("chatId", entry.chatId);
                obj.put("senderId", entry.senderId);
                array.put(obj);
            }
        } catch (Exception e) {
            org.telegram.messenger.FileLog.e(e);
        }
        getAccountPrefs(account).edit().putString("autoReplyDMLog", array.toString()).apply();
    }
}

