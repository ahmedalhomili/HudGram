package com.hudgram.ui.settings;
import com.hudgram.ui.autoreply.HudAutoReplyActivity;
import com.hudgram.ui.quickreply.HudQuickReplyActivity;
import com.hudgram.core.HudConfig;
import com.hudgram.ui.utils.TranslationUiHelper;

import android.text.TextUtils;
import android.view.View;

import androidx.core.text.HtmlCompat;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.TranslateController;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

import java.util.ArrayList;
import java.util.Locale;

public class HudChatSettingsActivity extends BaseHudSettingsActivity {

    // Chat Settings Row IDs
    private final int chatSettingsHeaderRow = rowId++;
    private final int confirmStickersRow = rowId++;
    private final int confirmVoiceMessagesRow = rowId++;
    private final int partialCopyRow = rowId++;
    private final int quickReplyRow = rowId++;
    private final int autoReplyRow = rowId++;
    private final int customDoubleTapActionRow = rowId++;
    private final int chatToolsFabRow = rowId++;

    // Translation Master Switch
    private final int translationEnabledRow = rowId++;

    // Provider Section
    private final int providerHeaderRow = rowId++;
    private final int translationProviderRow = rowId++;
    private final int targetLanguageRow = rowId++;

    // Behavior Section
    private final int behaviorHeaderRow = rowId++;
    private final int showOriginalRow = rowId++;
    private final int autoTranslateRow = rowId++;
    private final int contextTranslateRow = rowId++;
    private final int chatTranslateRow = rowId++;

    // Exceptions Section
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

    private CharSequence getCustomDoubleTapActionString() {
        int action = HudConfig.customDoubleTapAction;
        if (action == 1) {
            return getString("TranslateMessage"); // Native telegram string
        } else if (action == 2) {
            return getString("Reply"); // Native telegram string
        } else if (action == 3) {
            return getString("Copy"); // Native telegram string
        }
        return getString("Reactions"); // Native telegram string
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {

        // === Chat Settings Section ===
        items.add(UItem.asHeader(getString("General"))); // We can reuse "General" for chat general settings or no header

        items.add(TextSettingsCellFactory.of(customDoubleTapActionRow, getString("DoubleTapSetting"), getCustomDoubleTapActionString()).slug("customDoubleTapAction"));
        items.add(UItem.asShadow(getString("DoubleTapSettingAbout")));

        items.add(UItem.asCheck(confirmStickersRow, getString("ConfirmStickers")).slug("confirmStickers").setChecked(HudConfig.confirmStickers));
        items.add(UItem.asShadow(getString("ConfirmStickersAbout")));

        items.add(UItem.asCheck(confirmVoiceMessagesRow, getString("ConfirmVoiceMessages")).slug("confirmVoiceMessages").setChecked(HudConfig.confirmVoiceMessages));
        items.add(UItem.asShadow(getString("ConfirmVoiceMessagesAbout")));

        items.add(UItem.asCheck(partialCopyRow, getString("PartialCopy")).slug("partialCopy").setChecked(HudConfig.partialCopy));
        items.add(UItem.asShadow(getString("PartialCopyAbout")));

        items.add(TextSettingsCellFactory.of(quickReplyRow, getString("HudQuickReplyRow")).slug("quickReply"));
        items.add(TextSettingsCellFactory.of(autoReplyRow, getString("HudAutoReplyRow")).slug("autoReply"));
        items.add(UItem.asCheck(chatToolsFabRow, getString("ShowChatToolsFab")).slug("chatToolsFab").setChecked(HudConfig.showChatToolsFab));
        items.add(UItem.asShadow(getString("ShowChatToolsFabAbout")));
        items.add(UItem.asShadow(null));

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
        if (id == customDoubleTapActionRow) {
            org.telegram.ui.ActionBar.AlertDialog.Builder builder = new org.telegram.ui.ActionBar.AlertDialog.Builder(getParentActivity());
            builder.setTitle(getString("DoubleTapSetting"));
            CharSequence[] itemsList = new CharSequence[]{
                    getString("Reactions"),
                    getString("TranslateMessage"),
                    getString("Reply"),
                    getString("Copy")
            };
            builder.setItems(itemsList, (dialog, which) -> {
                HudConfig.setCustomDoubleTapAction(which);
                item.textValue = getCustomDoubleTapActionString();
                listView.adapter.notifyItemChanged(position, PARTIAL);
            });
            showDialog(builder.create());
        } else if (id == confirmStickersRow) {
            HudConfig.toggleConfirmStickers();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(HudConfig.confirmStickers);
            }
        } else if (id == confirmVoiceMessagesRow) {
            HudConfig.toggleConfirmVoiceMessages();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(HudConfig.confirmVoiceMessages);
            }
        } else if (id == partialCopyRow) {
            HudConfig.togglePartialCopy();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(HudConfig.partialCopy);
            }
        } else if (id == quickReplyRow) {
            presentFragment(new HudQuickReplyActivity());
        } else if (id == autoReplyRow) {
            presentFragment(new HudAutoReplyActivity());
        } else if (id == chatToolsFabRow) {
            HudConfig.toggleShowChatToolsFab();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(HudConfig.showChatToolsFab);
            }
        } else if (id == translationEnabledRow) {
            HudConfig.toggleTranslationEnabled();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(HudConfig.translationEnabled);
            }
            listView.adapter.update(true);
        } else if (id == translationProviderRow) {
            TranslationUiHelper.showTranslationProviderSelector(getParentActivity(), view, param -> {
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
        return getString("HudSettingsChat");
    }

    @Override
    protected String getKey() {
        return "chat";
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listView != null) {
            UItem restrictedLanguageItem = listView.findItemByItemId(doNotTranslateRow);
            if (restrictedLanguageItem != null) {
                restrictedLanguageItem.textValue = getRestrictedLanguages();
                notifyItemChanged(doNotTranslateRow, PARTIAL);
            }
            UItem translationTargetItem = listView.findItemByItemId(targetLanguageRow);
            if (translationTargetItem != null) {
                translationTargetItem.textValue = getTranslationTarget();
                notifyItemChanged(targetLanguageRow, PARTIAL);
            }
            UItem providerItem = listView.findItemByItemId(translationProviderRow);
            if (providerItem != null) {
                providerItem.textValue = getTranslationProvider();
                notifyItemChanged(translationProviderRow, PARTIAL);
            }
            UItem doubleTapItem = listView.findItemByItemId(customDoubleTapActionRow);
            if (doubleTapItem != null) {
                doubleTapItem.textValue = getCustomDoubleTapActionString();
                notifyItemChanged(customDoubleTapActionRow, PARTIAL);
            }
        }
    }
}
