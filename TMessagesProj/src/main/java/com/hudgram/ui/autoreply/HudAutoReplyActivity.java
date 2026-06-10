package com.hudgram.ui.autoreply;
import com.hudgram.ui.settings.BaseHudSettingsActivity;
import com.hudgram.core.HudConfig;

import android.content.Context;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.Components.Switch;
import org.telegram.ui.Components.AlertsCreator;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

import java.util.ArrayList;
import java.util.Locale;

public class HudAutoReplyActivity extends BaseHudSettingsActivity {

    // === Row IDs ===
    private final int enableRow = rowId++;
    // Message settings
    private final int modeRow = rowId++;
    private final int singleTextRow = rowId++;
    private final int multipleManageRow = rowId++;
    private final int smartMorningRow = rowId++;
    private final int smartAfternoonRow = rowId++;
    private final int smartEveningRow = rowId++;
    private final int smartNightRow = rowId++;
    // Advanced
    private final int cooldownRow = rowId++;
    private final int cooldownModeRow = rowId++;
    // Schedule
    private final int scheduleEnableRow = rowId++;
    private final int scheduleFromRow = rowId++;
    private final int scheduleToRow = rowId++;
    // Group filter
    private final int filterModeRow = rowId++;
    private final int filterManageRow = rowId++;
    // Log
    private final int logRow = rowId++;
    private final int logClearRow = rowId++;

    private final int[] cooldownValues = {10, 30, 60, 120, 300, 600};

    private String getCooldownString(int seconds) {
        if (seconds == 10) return getString("HudAutoReplyCooldown10s");
        else if (seconds == 30) return getString("HudAutoReplyCooldown30s");
        else if (seconds == 60) return getString("HudAutoReplyCooldown1m");
        else if (seconds == 120) return getString("HudAutoReplyCooldown2m");
        else if (seconds == 300) return getString("HudAutoReplyCooldown5m");
        else if (seconds == 600) return getString("HudAutoReplyCooldown10m");
        return seconds + "s";
    }

    private String getModeString(int mode) {
        switch (mode) {
            case 1: return getString("HudAutoReplyModeMultiple");
            case 2: return getString("HudAutoReplyModeSmart");
            default: return getString("HudAutoReplyModeSingle");
        }
    }

    private String getCooldownModeString(int mode) {
        return mode == 1 ? getString("HudAutoReplyCooldownPerSender") : getString("HudAutoReplyCooldownPerGroup");
    }

    private String getFilterModeString(int mode) {
        switch (mode) {
            case 1: return getString("HudAutoReplyFilterWhitelist");
            case 2: return getString("HudAutoReplyFilterBlacklist");
            default: return getString("HudAutoReplyFilterAll");
        }
    }

    private String formatTime(int hour, int minute) {
        return String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
    }

    private Switch actionBarSwitch;

    @Override
    public ActionBar createActionBar(Context context) {
        ActionBar actionBar = super.createActionBar(context);
        ActionBarMenu menu = actionBar.createMenu();

        // Help info item
        menu.addItem(2, R.drawable.msg_info);

        actionBarSwitch = new Switch(context);
        actionBarSwitch.setColors(Theme.key_switchTrack, Theme.key_switchTrackChecked, Theme.key_windowBackgroundWhite, Theme.key_windowBackgroundWhite);
        actionBarSwitch.setChecked(HudConfig.autoReplyMentionEnabled, false);
        actionBarSwitch.setOnCheckedChangeListener((view, isChecked) -> {
            if (HudConfig.autoReplyMentionEnabled != isChecked) {
                HudConfig.toggleAutoReplyMentionEnabled();
                listView.adapter.update(true);
            }
        });

        // Create a menu item wrapper to give the switch a native clickable area and touch selector background
        ActionBarMenuItem menuItem = menu.addItem(1, 0);
        menuItem.removeAllViews();
        menuItem.addView(actionBarSwitch, LayoutHelper.createFrame(37, 50, Gravity.CENTER));
        menuItem.setOnClickListener(v -> {
            actionBarSwitch.setChecked(!actionBarSwitch.isChecked(), true);
        });

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == 2) {
                    showHelpDialog();
                }
            }
        });

        return actionBar;
    }

    private void showHelpDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(getString("HudAutoReplyHelpTitle"));
        builder.setMessage(getString("HudAutoReplyHelpText"));
        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
        showDialog(builder.create());
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asShadow(getString("HudAutoReplyMentionEnabled")));

        if (!HudConfig.autoReplyMentionEnabled) return;

        // === Message Settings ===
        items.add(UItem.asHeader(getString("HudAutoReplyMessageSettings")));
        items.add(TextSettingsCellFactory.of(modeRow, getString("HudAutoReplyReplyMode"), getModeString(HudConfig.autoReplyMode)).slug("mode"));

        if (HudConfig.autoReplyMode == 0) {
            // Single message
            items.add(TextDetailSettingsCellFactory.of(singleTextRow, getString("HudAutoReplyMentionText"), HudConfig.autoReplyMentionText).slug("text"));
        } else if (HudConfig.autoReplyMode == 1) {
            // Multiple messages
            ArrayList<String> msgs = HudConfig.getAutoReplyMessages();
            String count = msgs.size() + " " + (msgs.size() == 1 ? "message" : "messages");
            items.add(TextSettingsCellFactory.of(multipleManageRow, getString("HudAutoReplyManageMessages"), count).slug("manage"));
        } else if (HudConfig.autoReplyMode == 2) {
            // Smart time-based
            items.add(UItem.asShadow(null));
            items.add(UItem.asHeader(getString("HudAutoReplySmartHeader")));
            items.add(TextDetailSettingsCellFactory.of(smartMorningRow, getString("HudAutoReplySmartMorning"), HudConfig.autoReplyMorningText).slug("morning"));
            items.add(TextDetailSettingsCellFactory.of(smartAfternoonRow, getString("HudAutoReplySmartAfternoon"), HudConfig.autoReplyAfternoonText).slug("afternoon"));
            items.add(TextDetailSettingsCellFactory.of(smartEveningRow, getString("HudAutoReplySmartEvening"), HudConfig.autoReplyEveningText).slug("evening"));
            items.add(TextDetailSettingsCellFactory.of(smartNightRow, getString("HudAutoReplySmartNight"), HudConfig.autoReplyNightText).slug("night"));
        }
        items.add(UItem.asShadow(null));

        // === Advanced Settings ===
        items.add(UItem.asHeader(getString("HudAutoReplyAdvanced")));
        items.add(TextSettingsCellFactory.of(cooldownRow, getString("HudAutoReplyMentionCooldown"), getCooldownString(HudConfig.autoReplyMentionCooldown)).slug("cooldown"));
        items.add(TextSettingsCellFactory.of(cooldownModeRow, getString("HudAutoReplyCooldownMode"), getCooldownModeString(HudConfig.autoReplyCooldownMode)).slug("cooldownMode"));
        items.add(UItem.asShadow(getString("HudAutoReplyCooldownModeAbout")));

        // === Schedule ===
        items.add(UItem.asHeader(getString("HudAutoReplyScheduleHeader")));
        items.add(UItem.asCheck(scheduleEnableRow, getString("HudAutoReplyScheduleEnabled")).slug("scheduleEnable").setChecked(HudConfig.autoReplyScheduleEnabled));
        if (HudConfig.autoReplyScheduleEnabled) {
            items.add(TextSettingsCellFactory.of(scheduleFromRow, getString("HudAutoReplyScheduleFrom"), formatTime(HudConfig.autoReplyScheduleStartHour, HudConfig.autoReplyScheduleStartMinute)).slug("scheduleFrom"));
            items.add(TextSettingsCellFactory.of(scheduleToRow, getString("HudAutoReplyScheduleTo"), formatTime(HudConfig.autoReplyScheduleEndHour, HudConfig.autoReplyScheduleEndMinute)).slug("scheduleTo"));
        }
        items.add(UItem.asShadow(getString("HudAutoReplyScheduleAbout")));

        // === Group Filter ===
        items.add(UItem.asHeader(getString("HudAutoReplyFilterHeader")));
        items.add(TextSettingsCellFactory.of(filterModeRow, getString("HudAutoReplyFilterMode"), getFilterModeString(HudConfig.autoReplyFilterMode)).slug("filterMode"));
        if (HudConfig.autoReplyFilterMode != 0) {
            int groupCount = HudConfig.getAutoReplyFilterGroups().size();
            String groupCountStr = groupCount + " groups";
            items.add(TextSettingsCellFactory.of(filterManageRow, getString("HudAutoReplyFilterManage"), groupCountStr).slug("filterManage"));
        }
        items.add(UItem.asShadow(getString("HudAutoReplyFilterAbout")));

        // === Log ===
        items.add(UItem.asHeader(getString("HudAutoReplyLogHeader")));
        ArrayList<HudConfig.AutoReplyLogEntry> log = HudConfig.getAutoReplyLog();
        String logCount = log.isEmpty() ? getString("HudAutoReplyLogEmpty") : log.size() + " replies";
        items.add(TextSettingsCellFactory.of(logRow, getString("HudAutoReplyLog"), logCount).slug("log"));
        if (!log.isEmpty()) {
            UItem clearItem = TextSettingsCellFactory.of(logClearRow, getString("HudAutoReplyLogClear"));
            clearItem.red = true;
            items.add(clearItem);
        }
        items.add(UItem.asShadow(null));
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        int id = item.id;

        if (id == modeRow) {
            showModeSelectDialog();

        } else if (id == singleTextRow) {
            showTextEditDialog(getString("HudAutoReplyMentionText"), HudConfig.autoReplyMentionText, text -> {
                HudConfig.setAutoReplyMentionText(text);
                listView.adapter.update(true);
            });

        } else if (id == multipleManageRow) {
            presentFragment(new HudAutoReplyMessagesActivity());

        } else if (id == smartMorningRow) {
            showTextEditDialog(getString("HudAutoReplySmartMorning"), HudConfig.autoReplyMorningText, text -> {
                HudConfig.setAutoReplyMorningText(text);
                listView.adapter.update(true);
            });
        } else if (id == smartAfternoonRow) {
            showTextEditDialog(getString("HudAutoReplySmartAfternoon"), HudConfig.autoReplyAfternoonText, text -> {
                HudConfig.setAutoReplyAfternoonText(text);
                listView.adapter.update(true);
            });
        } else if (id == smartEveningRow) {
            showTextEditDialog(getString("HudAutoReplySmartEvening"), HudConfig.autoReplyEveningText, text -> {
                HudConfig.setAutoReplyEveningText(text);
                listView.adapter.update(true);
            });
        } else if (id == smartNightRow) {
            showTextEditDialog(getString("HudAutoReplySmartNight"), HudConfig.autoReplyNightText, text -> {
                HudConfig.setAutoReplyNightText(text);
                listView.adapter.update(true);
            });

        } else if (id == cooldownRow) {
            showCooldownSelectDialog();
        } else if (id == cooldownModeRow) {
            showCooldownModeDialog();

        } else if (id == scheduleEnableRow) {
            HudConfig.toggleAutoReplyScheduleEnabled();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(HudConfig.autoReplyScheduleEnabled);
            }
            listView.adapter.update(true);
        } else if (id == scheduleFromRow) {
            showTimePicker(getString("HudAutoReplyScheduleFrom"), HudConfig.autoReplyScheduleStartHour, HudConfig.autoReplyScheduleStartMinute, (hour, minute) -> {
                HudConfig.setAutoReplyScheduleStart(hour, minute);
                listView.adapter.update(true);
            });
        } else if (id == scheduleToRow) {
            showTimePicker(getString("HudAutoReplyScheduleTo"), HudConfig.autoReplyScheduleEndHour, HudConfig.autoReplyScheduleEndMinute, (hour, minute) -> {
                HudConfig.setAutoReplyScheduleEnd(hour, minute);
                listView.adapter.update(true);
            });

        } else if (id == filterModeRow) {
            showFilterModeDialog();
        } else if (id == filterManageRow) {
            presentFragment(new HudAutoReplyGroupFilterActivity());

        } else if (id == logRow) {
            presentFragment(new HudAutoReplyLogActivity());
        } else if (id == logClearRow) {
            HudConfig.clearAutoReplyLog();
            listView.adapter.update(true);
            BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_delete, getString("HudAutoReplyLogCleared")).show();
        }
    }

    // === Dialogs ===

    private void showModeSelectDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(getString("HudAutoReplyReplyMode"));
        CharSequence[] modeItems = {
                getString("HudAutoReplyModeSingle"),
                getString("HudAutoReplyModeMultiple"),
                getString("HudAutoReplyModeSmart")
        };
        builder.setItems(modeItems, (dialog, which) -> {
            HudConfig.setAutoReplyMode(which);
            listView.adapter.update(true);
            BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_saved, getString("HudAutoReplySaved")).show();
        });
        showDialog(builder.create());
    }

    private void showCooldownSelectDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(getString("HudAutoReplyMentionCooldown"));
        CharSequence[] items = new CharSequence[cooldownValues.length];
        for (int i = 0; i < cooldownValues.length; i++) {
            items[i] = getCooldownString(cooldownValues[i]);
        }
        builder.setItems(items, (dialog, which) -> {
            HudConfig.setAutoReplyMentionCooldown(cooldownValues[which]);
            listView.adapter.update(true);
            BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_saved, getString("HudAutoReplySaved")).show();
        });
        showDialog(builder.create());
    }

    private void showCooldownModeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(getString("HudAutoReplyCooldownMode"));
        CharSequence[] items = {
                getString("HudAutoReplyCooldownPerGroup"),
                getString("HudAutoReplyCooldownPerSender")
        };
        builder.setItems(items, (dialog, which) -> {
            HudConfig.setAutoReplyCooldownMode(which);
            listView.adapter.update(true);
            BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_saved, getString("HudAutoReplySaved")).show();
        });
        showDialog(builder.create());
    }

    private void showFilterModeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(getString("HudAutoReplyFilterMode"));
        CharSequence[] items = {
                getString("HudAutoReplyFilterAll"),
                getString("HudAutoReplyFilterWhitelist"),
                getString("HudAutoReplyFilterBlacklist")
        };
        builder.setItems(items, (dialog, which) -> {
            HudConfig.setAutoReplyFilterMode(which);
            listView.adapter.update(true);
            BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_saved, getString("HudAutoReplySaved")).show();
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
        editText.setHint(getString("HudAutoReplyMentionTextHint"));
        editText.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
        editText.setCursorColor(Theme.getColor(Theme.key_dialogTextBlack));
        editText.setCursorSize(AndroidUtilities.dp(20));
        editText.setCursorWidth(1.5f);
        editText.setPadding(0, AndroidUtilities.dp(4), 0, AndroidUtilities.dp(4));
        linearLayout.addView(editText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.LEFT, 24, 6, 24, 0));

        // Character counter
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
                BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_saved, getString("HudAutoReplySaved")).show();
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
                    BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_saved, getString("HudAutoReplySaved")).show();
                }
        );
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
    }

    @Override
    protected String getActionBarTitle() {
        return getString("HudAutoReplyTitle");
    }

    @Override
    protected String getKey() {
        return "ar";
    }
}
