package com.hudgram.ui.autoreply;
import com.hudgram.ui.settings.BaseHudSettingsActivity;
import com.hudgram.core.HudConfig;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.ScaleStateListAnimator;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class HudAutoReplyDMAddActivity extends BaseHudSettingsActivity {

    // Row IDs
    private final int ruleNameRow = rowId++;
    // Match
    private final int matchAllRow = rowId++;
    private final int matchContainsRow = rowId++;
    private final int matchEqualsRow = rowId++;
    private final int matchKeywordRow = rowId++;
    // Reply
    private final int replyModeRow = rowId++;
    private final int replySingleTextRow = rowId++;
    private final int replyMultipleManageRow = rowId++;
    private final int replySmartMorningRow = rowId++;
    private final int replySmartAfternoonRow = rowId++;
    private final int replySmartEveningRow = rowId++;
    private final int replySmartNightRow = rowId++;
    // Delay
    private final int delayRow = rowId++;
    // Scope
    private final int scopeBothRow = rowId++;
    private final int scopeGroupsRow = rowId++;
    private final int scopePrivateRow = rowId++;
    // Advanced
    private final int firstOnlyRow = rowId++;
    private final int excludeBotsRow = rowId++;
    private final int excludeForwardedRow = rowId++;
    private final int cooldownRow = rowId++;
    // Schedule
    private final int scheduleEnableRow = rowId++;
    private final int scheduleFromRow = rowId++;
    private final int scheduleToRow = rowId++;
    // Filter
    private final int filterModeRow = rowId++;
    private final int filterManageRow = rowId++;

    private HudConfig.AutoReplyDMRule rule;
    private final boolean isEditing;

    private final int[] cooldownValues = {10, 30, 60, 120, 300, 600};
    private final int[] delayValues = {1, 2, 3, 5, 7, 10};

    public HudAutoReplyDMAddActivity(HudConfig.AutoReplyDMRule existingRule) {
        if (existingRule != null) {
            this.isEditing = true;
            // Deep copy for editing
            this.rule = new HudConfig.AutoReplyDMRule();
            this.rule.id = existingRule.id;
            this.rule.name = existingRule.name;
            this.rule.enabled = existingRule.enabled;
            this.rule.matchMode = existingRule.matchMode;
            this.rule.matchKeyword = existingRule.matchKeyword;
            this.rule.replyMode = existingRule.replyMode;
            this.rule.replyText = existingRule.replyText;
            this.rule.replyTexts = new ArrayList<>(existingRule.replyTexts);
            this.rule.morningText = existingRule.morningText;
            this.rule.afternoonText = existingRule.afternoonText;
            this.rule.eveningText = existingRule.eveningText;
            this.rule.nightText = existingRule.nightText;
            this.rule.scope = existingRule.scope;
            this.rule.delay = existingRule.delay;
            this.rule.cooldown = existingRule.cooldown;
            this.rule.firstMessageOnly = existingRule.firstMessageOnly;
            this.rule.excludeBots = existingRule.excludeBots;
            this.rule.excludeForwarded = existingRule.excludeForwarded;
            this.rule.scheduleEnabled = existingRule.scheduleEnabled;
            this.rule.scheduleStartHour = existingRule.scheduleStartHour;
            this.rule.scheduleStartMinute = existingRule.scheduleStartMinute;
            this.rule.scheduleEndHour = existingRule.scheduleEndHour;
            this.rule.scheduleEndMinute = existingRule.scheduleEndMinute;
            this.rule.filterMode = existingRule.filterMode;
            this.rule.filterChats = new HashSet<>(existingRule.filterChats);
        } else {
            this.isEditing = false;
            this.rule = new HudConfig.AutoReplyDMRule();
        }
    }

    @Override
    public View createView(Context context) {
        View view = super.createView(context);

        // Save FAB
        FrameLayout fabContainer = new FrameLayout(context);
        GradientDrawable fabBg = new GradientDrawable();
        fabBg.setShape(GradientDrawable.RECTANGLE);
        fabBg.setCornerRadius(AndroidUtilities.dp(12));
        fabBg.setColor(Theme.getColor(Theme.key_chats_actionBackground));
        fabContainer.setBackground(fabBg);
        fabContainer.setElevation(AndroidUtilities.dp(4));
        ScaleStateListAnimator.apply(fabContainer);

        ImageView fabIcon = new ImageView(context);
        fabIcon.setImageResource(R.drawable.msg_saved);
        fabIcon.setColorFilter(Theme.getColor(Theme.key_chats_actionIcon));
        fabIcon.setScaleType(ImageView.ScaleType.CENTER);
        fabContainer.addView(fabIcon, LayoutHelper.createFrame(24, 24, Gravity.CENTER));

        fabContainer.setOnClickListener(v -> saveRule());

        contentView.addView(fabContainer, LayoutHelper.createFrame(56, 56, (LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT) | Gravity.BOTTOM, 16, 0, 16, 16));

        return view;
    }

    private void saveRule() {
        // Validate
        if (TextUtils.isEmpty(rule.name)) {
            BulletinFactory.of(this).createErrorBulletin(getString("HudAutoReplyDMRuleNameHint")).show();
            return;
        }
        if (rule.replyMode == 0 && TextUtils.isEmpty(rule.replyText)) {
            BulletinFactory.of(this).createErrorBulletin(getString("HudAutoReplyDMReplyHint")).show();
            return;
        }
        if ((rule.matchMode == 1 || rule.matchMode == 2) && TextUtils.isEmpty(rule.matchKeyword)) {
            BulletinFactory.of(this).createErrorBulletin(getString("HudAutoReplyDMKeywordHint")).show();
            return;
        }

        if (isEditing) {
            HudConfig.updateAutoReplyDMRule(rule);
        } else {
            HudConfig.addAutoReplyDMRule(rule);
        }
        BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_saved, getString("HudAutoReplyDMSaved")).show();
        finishFragment();
    }

    // === Helper methods ===

    private String getCooldownString(int seconds) {
        if (seconds == 10) return getString("HudAutoReplyCooldown10s");
        else if (seconds == 30) return getString("HudAutoReplyCooldown30s");
        else if (seconds == 60) return getString("HudAutoReplyCooldown1m");
        else if (seconds == 120) return getString("HudAutoReplyCooldown2m");
        else if (seconds == 300) return getString("HudAutoReplyCooldown5m");
        else if (seconds == 600) return getString("HudAutoReplyCooldown10m");
        return seconds + "s";
    }

    private String getDelayString(int seconds) {
        return seconds + "s";
    }

    private String getReplyModeString(int mode) {
        switch (mode) {
            case 1: return getString("HudAutoReplyDMReplyModeMultiple");
            case 2: return getString("HudAutoReplyDMReplyModeSmart");
            default: return getString("HudAutoReplyDMReplyModeSingle");
        }
    }

    private String getFilterModeString(int mode) {
        switch (mode) {
            case 1: return getString("HudAutoReplyDMFilterWhitelist");
            case 2: return getString("HudAutoReplyDMFilterBlacklist");
            default: return getString("HudAutoReplyDMFilterAll");
        }
    }

    private String formatTime(int hour, int minute) {
        return String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        // === 1. Rule Name ===
        items.add(UItem.asHeader(getString("HudAutoReplyDMRuleName")));
        items.add(TextDetailSettingsCellFactory.of(ruleNameRow,
                getString("HudAutoReplyDMRuleName"),
                rule.name.isEmpty() ? getString("HudAutoReplyDMRuleNameHint") : rule.name).slug("ruleName"));
        items.add(UItem.asShadow(null));

        // === 2. Match Mode ===
        items.add(UItem.asHeader(getString("HudAutoReplyDMMatchHeader")));
        items.add(UItem.asRadio(matchAllRow, getString("HudAutoReplyDMMatchAll")).setChecked(rule.matchMode == 0));
        items.add(UItem.asRadio(matchContainsRow, getString("HudAutoReplyDMMatchContains")).setChecked(rule.matchMode == 1));
        items.add(UItem.asRadio(matchEqualsRow, getString("HudAutoReplyDMMatchEquals")).setChecked(rule.matchMode == 2));
        if (rule.matchMode != 0) {
            items.add(TextDetailSettingsCellFactory.of(matchKeywordRow,
                    getString("HudAutoReplyDMKeyword"),
                    rule.matchKeyword.isEmpty() ? getString("HudAutoReplyDMKeywordHint") : rule.matchKeyword).slug("keyword"));
        }
        items.add(UItem.asShadow(null));

        // === 3. Reply Mode ===
        items.add(UItem.asHeader(getString("HudAutoReplyDMReplyHeader")));
        items.add(TextSettingsCellFactory.of(replyModeRow, getString("HudAutoReplyDMReplyMode"), getReplyModeString(rule.replyMode)).slug("replyMode"));

        if (rule.replyMode == 0) {
            items.add(TextDetailSettingsCellFactory.of(replySingleTextRow,
                    getString("HudAutoReplyDMReplyHeader"),
                    rule.replyText.isEmpty() ? getString("HudAutoReplyDMReplyHint") : rule.replyText).slug("replyText"));
        } else if (rule.replyMode == 1) {
            String count = rule.replyTexts.size() + " messages";
            items.add(TextSettingsCellFactory.of(replyMultipleManageRow, getString("HudAutoReplyDMManageTexts"), count).slug("manageTexts"));
        } else if (rule.replyMode == 2) {
            items.add(TextDetailSettingsCellFactory.of(replySmartMorningRow, getString("HudAutoReplyDMSmartMorning"), rule.morningText).slug("morning"));
            items.add(TextDetailSettingsCellFactory.of(replySmartAfternoonRow, getString("HudAutoReplyDMSmartAfternoon"), rule.afternoonText).slug("afternoon"));
            items.add(TextDetailSettingsCellFactory.of(replySmartEveningRow, getString("HudAutoReplyDMSmartEvening"), rule.eveningText).slug("evening"));
            items.add(TextDetailSettingsCellFactory.of(replySmartNightRow, getString("HudAutoReplyDMSmartNight"), rule.nightText).slug("night"));
        }
        items.add(UItem.asShadow(null));

        // === 4. Delay ===
        items.add(UItem.asHeader(getString("HudAutoReplyDMDelay")));
        items.add(TextSettingsCellFactory.of(delayRow, getString("HudAutoReplyDMDelay"), getDelayString(rule.delay)).slug("delay"));
        items.add(UItem.asShadow(getString("HudAutoReplyDMDelayAbout")));

        // === 5. Scope ===
        items.add(UItem.asHeader(getString("HudAutoReplyDMScopeHeader")));
        items.add(UItem.asRadio(scopeBothRow, getString("HudAutoReplyDMScopeBoth")).setChecked(rule.scope == 0));
        items.add(UItem.asRadio(scopeGroupsRow, getString("HudAutoReplyDMScopeGroups")).setChecked(rule.scope == 1));
        items.add(UItem.asRadio(scopePrivateRow, getString("HudAutoReplyDMScopeContacts")).setChecked(rule.scope == 2));
        items.add(UItem.asShadow(null));

        // === 6. Advanced ===
        items.add(UItem.asHeader(getString("HudAutoReplyDMAdvancedHeader")));
        items.add(UItem.asCheck(firstOnlyRow, getString("HudAutoReplyDMFirstOnly")).slug("firstOnly").setChecked(rule.firstMessageOnly));
        items.add(UItem.asCheck(excludeBotsRow, getString("HudAutoReplyDMExcludeBots")).slug("excludeBots").setChecked(rule.excludeBots));
        items.add(UItem.asCheck(excludeForwardedRow, getString("HudAutoReplyDMExcludeForwarded")).slug("excludeFwd").setChecked(rule.excludeForwarded));
        items.add(TextSettingsCellFactory.of(cooldownRow, getString("HudAutoReplyDMCooldown"), getCooldownString(rule.cooldown)).slug("cooldown"));
        items.add(UItem.asShadow(getString("HudAutoReplyDMFirstOnlyAbout")));

        // === 7. Schedule ===
        items.add(UItem.asHeader(getString("HudAutoReplyDMScheduleHeader")));
        items.add(UItem.asCheck(scheduleEnableRow, getString("HudAutoReplyDMScheduleEnabled")).slug("schedEnable").setChecked(rule.scheduleEnabled));
        if (rule.scheduleEnabled) {
            items.add(TextSettingsCellFactory.of(scheduleFromRow, getString("HudAutoReplyDMScheduleFrom"), formatTime(rule.scheduleStartHour, rule.scheduleStartMinute)).slug("schedFrom"));
            items.add(TextSettingsCellFactory.of(scheduleToRow, getString("HudAutoReplyDMScheduleTo"), formatTime(rule.scheduleEndHour, rule.scheduleEndMinute)).slug("schedTo"));
        }
        items.add(UItem.asShadow(null));

        // === 8. Filter ===
        items.add(UItem.asHeader(getString("HudAutoReplyDMFilterHeader")));
        items.add(TextSettingsCellFactory.of(filterModeRow, getString("HudAutoReplyDMFilterHeader"), getFilterModeString(rule.filterMode)).slug("filterMode"));
        if (rule.filterMode != 0) {
            int chatCount = rule.filterChats != null ? rule.filterChats.size() : 0;
            items.add(TextSettingsCellFactory.of(filterManageRow, getString("HudAutoReplyDMFilterManage"), chatCount + " chats").slug("filterManage"));
        }
        items.add(UItem.asShadow(null));
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        int id = item.id;

        if (id == ruleNameRow) {
            showTextEditDialog(getString("HudAutoReplyDMRuleName"), rule.name, text -> {
                rule.name = text;
                listView.adapter.update(true);
            });

        } else if (id == matchAllRow) {
            rule.matchMode = 0;
            listView.adapter.update(true);
        } else if (id == matchContainsRow) {
            rule.matchMode = 1;
            listView.adapter.update(true);
        } else if (id == matchEqualsRow) {
            rule.matchMode = 2;
            listView.adapter.update(true);
        } else if (id == matchKeywordRow) {
            showTextEditDialog(getString("HudAutoReplyDMKeyword"), rule.matchKeyword, text -> {
                rule.matchKeyword = text;
                listView.adapter.update(true);
            });

        } else if (id == replyModeRow) {
            showReplyModeDialog();
        } else if (id == replySingleTextRow) {
            showTextEditDialog(getString("HudAutoReplyDMReplyHeader"), rule.replyText, text -> {
                rule.replyText = text;
                listView.adapter.update(true);
            });
        } else if (id == replyMultipleManageRow) {
            presentFragment(new HudAutoReplyDMMessagesActivity(rule.replyTexts, texts -> {
                rule.replyTexts = texts;
                listView.adapter.update(true);
            }));
        } else if (id == replySmartMorningRow) {
            showTextEditDialog(getString("HudAutoReplyDMSmartMorning"), rule.morningText, text -> {
                rule.morningText = text;
                listView.adapter.update(true);
            });
        } else if (id == replySmartAfternoonRow) {
            showTextEditDialog(getString("HudAutoReplyDMSmartAfternoon"), rule.afternoonText, text -> {
                rule.afternoonText = text;
                listView.adapter.update(true);
            });
        } else if (id == replySmartEveningRow) {
            showTextEditDialog(getString("HudAutoReplyDMSmartEvening"), rule.eveningText, text -> {
                rule.eveningText = text;
                listView.adapter.update(true);
            });
        } else if (id == replySmartNightRow) {
            showTextEditDialog(getString("HudAutoReplyDMSmartNight"), rule.nightText, text -> {
                rule.nightText = text;
                listView.adapter.update(true);
            });

        } else if (id == delayRow) {
            showDelayDialog();
        } else if (id == scopeBothRow) {
            rule.scope = 0;
            listView.adapter.update(true);
        } else if (id == scopeGroupsRow) {
            rule.scope = 1;
            listView.adapter.update(true);
        } else if (id == scopePrivateRow) {
            rule.scope = 2;
            listView.adapter.update(true);

        } else if (id == firstOnlyRow) {
            rule.firstMessageOnly = !rule.firstMessageOnly;
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(rule.firstMessageOnly);
            }
        } else if (id == excludeBotsRow) {
            rule.excludeBots = !rule.excludeBots;
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(rule.excludeBots);
            }
        } else if (id == excludeForwardedRow) {
            rule.excludeForwarded = !rule.excludeForwarded;
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(rule.excludeForwarded);
            }
        } else if (id == cooldownRow) {
            showCooldownDialog();

        } else if (id == scheduleEnableRow) {
            rule.scheduleEnabled = !rule.scheduleEnabled;
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(rule.scheduleEnabled);
            }
            listView.adapter.update(true);
        } else if (id == scheduleFromRow) {
            showTimePicker(getString("HudAutoReplyDMScheduleFrom"), rule.scheduleStartHour, rule.scheduleStartMinute, (hour, minute) -> {
                rule.scheduleStartHour = hour;
                rule.scheduleStartMinute = minute;
                listView.adapter.update(true);
            });
        } else if (id == scheduleToRow) {
            showTimePicker(getString("HudAutoReplyDMScheduleTo"), rule.scheduleEndHour, rule.scheduleEndMinute, (hour, minute) -> {
                rule.scheduleEndHour = hour;
                rule.scheduleEndMinute = minute;
                listView.adapter.update(true);
            });

        } else if (id == filterModeRow) {
            showFilterModeDialog();
        } else if (id == filterManageRow) {
            if (rule.filterChats == null) rule.filterChats = new HashSet<>();
            presentFragment(new HudAutoReplyDMFilterActivity(rule.filterChats, rule.filterMode, chats -> {
                rule.filterChats = chats;
                listView.adapter.update(true);
            }));
        }
    }

    // === Dialogs ===

    private void showReplyModeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(getString("HudAutoReplyDMReplyMode"));
        CharSequence[] modeItems = {
                getString("HudAutoReplyDMReplyModeSingle"),
                getString("HudAutoReplyDMReplyModeMultiple"),
                getString("HudAutoReplyDMReplyModeSmart")
        };
        builder.setItems(modeItems, (dialog, which) -> {
            rule.replyMode = which;
            listView.adapter.update(true);
        });
        showDialog(builder.create());
    }

    private void showDelayDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(getString("HudAutoReplyDMDelay"));
        CharSequence[] items = new CharSequence[delayValues.length];
        for (int i = 0; i < delayValues.length; i++) {
            items[i] = getDelayString(delayValues[i]);
        }
        builder.setItems(items, (dialog, which) -> {
            rule.delay = delayValues[which];
            listView.adapter.update(true);
        });
        showDialog(builder.create());
    }

    private void showCooldownDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(getString("HudAutoReplyDMCooldown"));
        CharSequence[] items = new CharSequence[cooldownValues.length];
        for (int i = 0; i < cooldownValues.length; i++) {
            items[i] = getCooldownString(cooldownValues[i]);
        }
        builder.setItems(items, (dialog, which) -> {
            rule.cooldown = cooldownValues[which];
            listView.adapter.update(true);
        });
        showDialog(builder.create());
    }

    private void showFilterModeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(getString("HudAutoReplyDMFilterHeader"));
        CharSequence[] items = {
                getString("HudAutoReplyDMFilterAll"),
                getString("HudAutoReplyDMFilterWhitelist"),
                getString("HudAutoReplyDMFilterBlacklist")
        };
        builder.setItems(items, (dialog, which) -> {
            rule.filterMode = which;
            listView.adapter.update(true);
        });
        showDialog(builder.create());
    }

    private interface TextEditCallback {
        void onSave(String text);
    }

    private void showTextEditDialog(String title, String currentText, TextEditCallback callback) {
        final EditTextBoldCursor editText = new EditTextBoldCursor(getParentActivity());
        editText.setBackgroundDrawable(Theme.createEditTextDrawable(getParentActivity(), true));

        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setDialogButtonColorKey(Theme.key_dialogButton);
        builder.setTitle(title);
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), (dialog, which) -> AndroidUtilities.hideKeyboard(editText));

        LinearLayout linearLayout = new LinearLayout(getParentActivity());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        builder.setView(linearLayout);

        editText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        editText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        editText.setMaxLines(4);
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        editText.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP);
        editText.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
        editText.setCursorColor(Theme.getColor(Theme.key_dialogTextBlack));
        editText.setCursorSize(AndroidUtilities.dp(20));
        editText.setCursorWidth(1.5f);
        editText.setPadding(0, AndroidUtilities.dp(4), 0, AndroidUtilities.dp(4));
        linearLayout.addView(editText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.LEFT, 24, 6, 24, 0));

        final TextView charCounter = new TextView(getParentActivity());
        charCounter.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        charCounter.setTextColor(Theme.getColor(Theme.key_dialogTextGray3));
        charCounter.setGravity(LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT);
        linearLayout.addView(charCounter, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP, 24, 4, 24, 0));

        if (!TextUtils.isEmpty(currentText)) {
            editText.setText(currentText);
            editText.setSelection(editText.length());
            charCounter.setText(String.valueOf(currentText.length()));
        }

        editText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                charCounter.setText(s.length() + "");
            }
        });

        builder.setPositiveButton(LocaleController.getString(R.string.Save), (dialog, which) -> {
            AndroidUtilities.hideKeyboard(editText);
            String text = editText.getText().toString().trim();
            if (!TextUtils.isEmpty(text)) {
                callback.onSave(text);
            }
        });

        final AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(dialog -> AndroidUtilities.runOnUIThread(() -> {
            editText.requestFocus();
            AndroidUtilities.showKeyboard(editText);
        }));
        alertDialog.setOnDismissListener(dialog -> AndroidUtilities.hideKeyboard(editText));
        showDialog(alertDialog);
        alertDialog.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        editText.requestFocus();
    }

    private interface TimePickerCallback {
        void onTimePicked(int hour, int minute);
    }

    private void showTimePicker(String title, int currentHour, int currentMinute, TimePickerCallback callback) {
        AlertsCreator.createTimePickerDialog(
                getParentActivity(),
                title,
                currentHour * 60 + currentMinute,
                0,
                24 * 60,
                time -> {
                    int hour = time / 60;
                    int minute = time % 60;
                    callback.onTimePicked(hour, minute);
                }
        );
    }

    @Override
    protected String getActionBarTitle() {
        return isEditing ? getString("HudAutoReplyDMRuleName") : getString("HudAutoReplyDMTitle");
    }

    @Override
    protected String getKey() {
        return "ardma";
    }
}
