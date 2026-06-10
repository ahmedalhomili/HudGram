package com.hudgram.ui.utils;
import com.hudgram.core.HudConfig;

import android.app.Activity;
import android.util.Pair;
import android.view.View;
import java.util.ArrayList;
import org.telegram.messenger.LocaleController;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;

public class TranslationUiHelper {

    public interface OnProviderSelectedListener {
        void onSelected(boolean changed);
    }

    public static Pair<ArrayList<String>, ArrayList<String>> getProviders() {
        ArrayList<String> names = new ArrayList<>();
        ArrayList<String> keys = new ArrayList<>();

        names.add(LocaleController.getString("GoogleTranslate", org.telegram.messenger.R.string.GoogleTranslate));
        keys.add("google");

        names.add(LocaleController.getString("TelegramTranslate", org.telegram.messenger.R.string.TelegramTranslate));
        keys.add("telegram");

        return new Pair<>(names, keys);
    }

    public static ArrayList<String> getRestrictedLanguages() {
        return new ArrayList<>(org.telegram.ui.RestrictedLanguagesSelectActivity.getRestrictedLanguages());
    }

    public static void showTranslationProviderSelector(Activity activity, View anchor, OnProviderSelectedListener listener, Theme.ResourcesProvider resourcesProvider) {
        Pair<ArrayList<String>, ArrayList<String>> providers = getProviders();
        PopupHelper.show(providers.first,
                LocaleController.getString("ChooseTranslationProvider", org.telegram.messenger.R.string.ChooseTranslationProvider),
                providers.second.indexOf(HudConfig.translationProvider),
                activity, anchor, i -> {
                    String old = HudConfig.translationProvider;
                    String selected = providers.second.get(i);
                    HudConfig.setTranslationProvider(selected);
                    if (listener != null) {
                        listener.onSelected(!old.equals(selected));
                    }
                }, resourcesProvider);
    }

    public static String getCurrentTargetLanguage() {
        return HudConfig.translationTarget;
    }

    public static String stripLanguageCode(String languageCode) {
        if (languageCode == null) return "";
        int index = languageCode.indexOf('-');
        if (index > 0) {
            return languageCode.substring(0, index);
        }
        index = languageCode.indexOf('_');
        if (index > 0) {
            return languageCode.substring(0, index);
        }
        return languageCode;
    }

    public static ArrayList<String> getCurrentTargetLanguages() {
        ArrayList<String> langs = new ArrayList<>();
        langs.addAll(java.util.Arrays.asList(
            "af", "am", "ar", "az", "be", "bg", "bn", "bs", "ca", "ceb",
            "co", "cs", "cy", "da", "de", "el", "en", "eo", "es", "et",
            "eu", "fa", "fi", "fil", "fr", "fy", "ga", "gd", "gl", "gu",
            "ha", "haw", "he", "hi", "hmn", "hr", "ht", "hu", "hy", "id",
            "ig", "is", "it", "ja", "jv", "ka", "kk", "km", "kn", "ko",
            "ku", "ky", "la", "lb", "lo", "lt", "lv", "mg", "mi", "mk",
            "ml", "mn", "mr", "ms", "mt", "my", "ne", "nl", "no", "ny",
            "pa", "pl", "ps", "pt", "ro", "ru", "sd", "si", "sk", "sl",
            "sm", "sn", "so", "sq", "sr", "st", "su", "sv", "sw", "ta",
            "te", "tg", "th", "tr", "uk", "ur", "uz", "vi", "xh", "yi",
            "yo", "zh", "zu"
        ));
        return langs;
    }

    public static void saveRestrictedLanguages(ArrayList<String> langs) {
        org.telegram.ui.RestrictedLanguagesSelectActivity.updateRestrictedLanguages(new java.util.HashSet<>(langs), true);
    }
}
