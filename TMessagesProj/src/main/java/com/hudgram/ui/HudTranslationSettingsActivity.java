package com.hudgram.ui;

import android.text.TextUtils;
import android.view.View;

import androidx.core.text.HtmlCompat;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.TranslateController;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

import java.util.ArrayList;
import java.util.Locale;

public class HudTranslationSettingsActivity extends BaseHudSettingsActivity {

    // Row IDs
    private final int translationEnabledRow = rowId++;
    private final int providerHeaderRow = rowId++;
    private final int translationProviderRow = rowId++;
    private final int targetLanguageRow = rowId++;
    private final int behaviorHeaderRow = rowId++;
    private final int showOriginalRow = rowId++;
    private final int autoTranslateRow = rowId++;
    private final int contextTranslateRow = rowId++;
    private final int chatTranslateRow = rowId++;
    private final int exceptionsHeaderRow = rowId++;
    private final int doNotTranslateRow = rowId++;

    private CharSequence getTranslationProvider() {
        String provider = HudConfig.translationProvider;
        if ("telegram".equals(provider)) {
            return getString("TelegramTranslate");
        }
        return getString("GoogleTranslate");
    }

    private CharSequence getTranslationTarget() {
        String language = HudConfig.translationTarget;
        if (language.equals("app")) {
            return getString("TranslationTargetApp");
        }
        Locale locale = Locale.forLanguageTag(language);
        if (!TextUtils.isEmpty(locale.getScript())) {
            return HtmlCompat.fromHtml(locale.getDisplayScript(), HtmlCompat.FROM_HTML_MODE_LEGACY);
        }
        return locale.getDisplayName();
    }

    private CharSequence getRestrictedLanguages() {
        java.util.HashSet<String> langCodes = org.telegram.ui.RestrictedLanguagesSelectActivity.getRestrictedLanguages();
        if (langCodes.isEmpty()) {
            return LocaleController.formatPluralString("Languages", 0);
        } else if (langCodes.size() == 1) {
            Locale locale = Locale.forLanguageTag(langCodes.iterator().next());
            if (!TextUtils.isEmpty(locale.getScript())) {
                return HtmlCompat.fromHtml(locale.getDisplayScript(), HtmlCompat.FROM_HTML_MODE_LEGACY);
            }
            return locale.getDisplayName();
        }
        return LocaleController.formatPluralString("Languages", langCodes.size());
    }

    private boolean getContextValue() {
        return getMessagesController().getTranslateController().isContextTranslateEnabled();
    }

    private boolean getChatValue() {
        return getMessagesController().getTranslateController().isFeatureAvailable();
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        // === Translation Master Switch ===
        items.add(UItem.asCheck(translationEnabledRow, getString("TranslationEnabled"))
                .slug("translationEnabled")
                .setChecked(HudConfig.translationEnabled));
        items.add(UItem.asShadow(getString("TranslationEnabledAbout")));

        if (HudConfig.translationEnabled) {
            // === Provider Section ===
            items.add(UItem.asHeader(getString("TranslationProviderTitle")));
            items.add(TextSettingsCellFactory.of(translationProviderRow, getString("TranslationProviderShort"), getTranslationProvider()).slug("translationProvider"));
            items.add(TextSettingsCellFactory.of(targetLanguageRow, getString("TranslationTarget"), getTranslationTarget()).slug("translationTarget"));
            items.add(UItem.asShadow(null));

            // === Behavior Section ===
            items.add(UItem.asHeader(getString("TranslationBehavior")));
            items.add(UItem.asCheck(showOriginalRow, getString("TranslatorShowOriginal"))
                    .slug("showOriginal")
                    .setChecked(HudConfig.showOriginal));
            items.add(UItem.asCheck(autoTranslateRow, getString("AutoTranslate"))
                    .slug("autoTranslate")
                    .setChecked(HudConfig.autoTranslate));
            items.add(UItem.asCheck(contextTranslateRow, getString("TranslateSelectedMessages"))
                    .slug("contextTranslate")
                    .setChecked(getContextValue()));
            items.add(UItem.asCheck(chatTranslateRow, getString("TranslateChatMessages"))
                    .slug("chatTranslate")
                    .setChecked(getChatValue()));
            items.add(UItem.asShadow(null));

            // === Exceptions Section ===
            items.add(UItem.asHeader(getString("TranslationExceptions")));
            items.add(TextSettingsCellFactory.of(doNotTranslateRow, getString("DoNotTranslate"), getRestrictedLanguages()).slug("doNotTranslate"));
            items.add(UItem.asShadow(getString("TranslationExceptionsAbout")));
        }
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        int id = item.id;
        if (id == translationEnabledRow) {
            HudConfig.toggleTranslationEnabled();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(HudConfig.translationEnabled);
            }
            // Rebuild the list to show/hide sections
            listView.adapter.update(true);
        } else if (id == translationProviderRow) {
            Translator.showTranslationProviderSelector(getParentActivity(), view, param -> {
                item.textValue = getTranslationProvider();
                listView.adapter.notifyItemChanged(position, PARTIAL);
            }, resourcesProvider);
        } else if (id == targetLanguageRow) {
            presentFragment(new HudLanguagesSelectActivity(HudLanguagesSelectActivity.TYPE_TARGET));
        } else if (id == showOriginalRow) {
            HudConfig.toggleShowOriginal();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(HudConfig.showOriginal);
            }
        } else if (id == autoTranslateRow) {
            HudConfig.toggleAutoTranslate();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(HudConfig.autoTranslate);
            }
        } else if (id == contextTranslateRow) {
            boolean value = !getContextValue();
            getMessagesController().getTranslateController().setContextTranslateEnabled(value);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(value);
            }
        } else if (id == chatTranslateRow) {
            boolean value = !getChatValue();
            getMessagesController().getTranslateController().setChatTranslateEnabled(value);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(value);
            }
        } else if (id == doNotTranslateRow) {
            presentFragment(new HudLanguagesSelectActivity(HudLanguagesSelectActivity.TYPE_RESTRICTED));
        }
    }

    @Override
    protected String getActionBarTitle() {
        return getString("TranslationSettings");
    }

    @Override
    protected String getKey() {
        return "ts";
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listView != null) {
            // Update restricted languages display
            UItem restrictedLanguageItem = listView.findItemByItemId(doNotTranslateRow);
            if (restrictedLanguageItem != null) {
                restrictedLanguageItem.textValue = getRestrictedLanguages();
                notifyItemChanged(doNotTranslateRow, PARTIAL);
            }
            // Update target language display
            UItem translationTargetItem = listView.findItemByItemId(targetLanguageRow);
            if (translationTargetItem != null) {
                translationTargetItem.textValue = getTranslationTarget();
                notifyItemChanged(targetLanguageRow, PARTIAL);
            }
            // Update provider display
            UItem providerItem = listView.findItemByItemId(translationProviderRow);
            if (providerItem != null) {
                providerItem.textValue = getTranslationProvider();
                notifyItemChanged(translationProviderRow, PARTIAL);
            }
        }
    }
}
