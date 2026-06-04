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
    public static boolean openArchiveOnPull = getPrefs().getBoolean("openArchiveOnPull", true);
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
}
