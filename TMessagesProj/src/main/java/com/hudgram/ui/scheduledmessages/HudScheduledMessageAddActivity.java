package com.hudgram.ui.scheduledmessages;

import com.hudgram.ui.settings.BaseHudSettingsActivity;
import com.hudgram.core.HudScheduledMessagesManager;
import com.hudgram.core.HudScheduledMessagesManager.ScheduledMessage;

import org.telegram.ui.UsersSelectActivity;
import org.telegram.ui.Components.AlertsCreator;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.BulletinFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class HudScheduledMessageAddActivity extends BaseHudSettingsActivity {

    private ArrayList<Long> selectedChatIds = new ArrayList<>();
    private int selectedYear;
    private int selectedMonth;
    private int selectedDay;
    private int selectedHour;
    private int selectedMinute;

    // Recurrence fields
    private int repeatType = 0; // 0 = Never, 1 = Daily, 2 = Weekly, 3 = Monthly, 4 = Custom
    private long repeatInterval = 0; // in milliseconds

    private EditTextBoldCursor messageEdit;
    private ScrollView scrollView;
    private TextView recipientsValueText;
    private TextView timeValueText;
    private TextView repeatValueText;

    @Override
    public boolean onFragmentCreate() {
        // Default execution time is current time + 1 hour
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR_OF_DAY, 1);
        selectedYear = cal.get(Calendar.YEAR);
        selectedMonth = cal.get(Calendar.MONTH);
        selectedDay = cal.get(Calendar.DAY_OF_MONTH);
        selectedHour = cal.get(Calendar.HOUR_OF_DAY);
        selectedMinute = cal.get(Calendar.MINUTE);
        return super.onFragmentCreate();
    }

    @Override
    public View createView(Context context) {
        // 1. Initialize messageEdit edit text field first
        messageEdit = new EditTextBoldCursor(context);
        messageEdit.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        messageEdit.setHintTextColor(getThemedColor(Theme.key_windowBackgroundWhiteHintText));
        messageEdit.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
        messageEdit.setBackgroundDrawable(null);
        messageEdit.setLineColors(getThemedColor(Theme.key_windowBackgroundWhiteInputField), getThemedColor(Theme.key_windowBackgroundWhiteInputFieldActivated), getThemedColor(Theme.key_text_RedRegular));
        messageEdit.setPadding(0, AndroidUtilities.dp(6), 0, AndroidUtilities.dp(6));
        messageEdit.setGravity(Gravity.START);
        messageEdit.setHint(getString("HudScheduledMessagesMsgHint"));
        messageEdit.setSingleLine(false);
        messageEdit.setCursorColor(getThemedColor(Theme.key_windowBackgroundWhiteInputFieldActivated));
        messageEdit.setCursorWidth(1.5f);
        messageEdit.setMinLines(4);
        messageEdit.setMaxLines(8);

        // 2. Call parent view builder
        super.createView(context);
        
        // 3. Hide the default listView
        if (listView != null) {
            listView.setVisibility(View.GONE);
        }

        // 4. Create Custom ScrollView
        scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundGray));

        // 5. Create Container Layout
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(8), AndroidUtilities.dp(16), AndroidUtilities.dp(16));
        scrollView.addView(container, LayoutHelper.createScroll(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP));

        // --- Card 1: Recipients ---
        LinearLayout recipientsCard = new LinearLayout(context);
        recipientsCard.setOrientation(LinearLayout.VERTICAL);
        recipientsCard.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(12), getThemedColor(Theme.key_windowBackgroundWhite)));
        recipientsCard.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16));
        container.addView(recipientsCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 8, 0, 0));

        // Recipients Header (Horizontal)
        LinearLayout recipientsHeader = new LinearLayout(context);
        recipientsHeader.setOrientation(LinearLayout.HORIZONTAL);
        recipientsHeader.setGravity(Gravity.CENTER_VERTICAL);
        
        ImageView recipientsIcon = new ImageView(context);
        recipientsIcon.setImageResource(R.drawable.msg_contacts);
        recipientsIcon.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_windowBackgroundWhiteBlueHeader), PorterDuff.Mode.SRC_IN));
        recipientsHeader.addView(recipientsIcon, LayoutHelper.createLinear(20, 20));

        TextView recipientsTitle = new TextView(context);
        recipientsTitle.setText(getString("HudScheduledMessagesSelectChats"));
        recipientsTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        recipientsTitle.setTypeface(AndroidUtilities.bold());
        recipientsTitle.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlueHeader));
        recipientsTitle.setGravity(Gravity.START);
        recipientsHeader.addView(recipientsTitle, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 8, 0, 0, 0));

        // Spacer to push select button to end
        View spacer1 = new View(context);
        recipientsHeader.addView(spacer1, LayoutHelper.createLinear(0, 0, 1f));

        TextView selectHint = new TextView(context);
        selectHint.setText(getString("HudScheduledMessagesEditAction"));
        selectHint.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        selectHint.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlueText));
        recipientsHeader.addView(selectHint, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        recipientsCard.addView(recipientsHeader, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // Divider
        View divider1 = new View(context);
        divider1.setBackgroundColor(getThemedColor(Theme.key_divider));
        recipientsCard.addView(divider1, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 0, 12, 0, 12));

        // Value text
        recipientsValueText = new TextView(context);
        recipientsValueText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        recipientsValueText.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
        recipientsValueText.setGravity(Gravity.START);
        recipientsCard.addView(recipientsValueText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        recipientsCard.setOnClickListener(v -> openRecipientsPicker());

        // --- Card 2: Time Settings ---
        LinearLayout timeCard = new LinearLayout(context);
        timeCard.setOrientation(LinearLayout.VERTICAL);
        timeCard.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(12), getThemedColor(Theme.key_windowBackgroundWhite)));
        timeCard.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16));
        container.addView(timeCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 16, 0, 0));

        // Time Header (Horizontal)
        LinearLayout timeHeader = new LinearLayout(context);
        timeHeader.setOrientation(LinearLayout.HORIZONTAL);
        timeHeader.setGravity(Gravity.CENTER_VERTICAL);
        
        ImageView timeIcon = new ImageView(context);
        timeIcon.setImageResource(R.drawable.msg_calendar2);
        timeIcon.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_windowBackgroundWhiteBlueHeader), PorterDuff.Mode.SRC_IN));
        timeHeader.addView(timeIcon, LayoutHelper.createLinear(20, 20));

        TextView timeTitle = new TextView(context);
        timeTitle.setText(getString("HudScheduledMessagesTimeSettings"));
        timeTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        timeTitle.setTypeface(AndroidUtilities.bold());
        timeTitle.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlueHeader));
        timeTitle.setGravity(Gravity.START);
        timeHeader.addView(timeTitle, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 8, 0, 0, 0));

        timeCard.addView(timeHeader, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // Divider
        View divider2 = new View(context);
        divider2.setBackgroundColor(getThemedColor(Theme.key_divider));
        timeCard.addView(divider2, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 0, 12, 0, 12));

        // Value text (Large and prominent)
        timeValueText = new TextView(context);
        timeValueText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        timeValueText.setTypeface(AndroidUtilities.bold());
        timeValueText.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
        timeValueText.setGravity(Gravity.START);
        timeCard.addView(timeValueText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView timeHintText = new TextView(context);
        timeHintText.setText(getString("HudScheduledMessagesTapToChangeTime"));
        timeHintText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        timeHintText.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText2));
        timeHintText.setGravity(Gravity.START);
        timeCard.addView(timeHintText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 0));

        timeCard.setOnClickListener(v -> openDateTimePicker());

        // --- Card 3: Repeat Settings (Recurrence) ---
        LinearLayout repeatCard = new LinearLayout(context);
        repeatCard.setOrientation(LinearLayout.VERTICAL);
        repeatCard.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(12), getThemedColor(Theme.key_windowBackgroundWhite)));
        repeatCard.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16));
        container.addView(repeatCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 16, 0, 0));

        // Repeat Header (Horizontal)
        LinearLayout repeatHeader = new LinearLayout(context);
        repeatHeader.setOrientation(LinearLayout.HORIZONTAL);
        repeatHeader.setGravity(Gravity.CENTER_VERTICAL);

        ImageView repeatIcon = new ImageView(context);
        repeatIcon.setImageResource(R.drawable.msg_retry); // Native repeat/retry icon
        repeatIcon.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_windowBackgroundWhiteBlueHeader), PorterDuff.Mode.SRC_IN));
        repeatHeader.addView(repeatIcon, LayoutHelper.createLinear(20, 20));

        TextView repeatTitle = new TextView(context);
        repeatTitle.setText(getString("HudScheduledMessagesRepeat"));
        repeatTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        repeatTitle.setTypeface(AndroidUtilities.bold());
        repeatTitle.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlueHeader));
        repeatTitle.setGravity(Gravity.START);
        repeatHeader.addView(repeatTitle, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 8, 0, 0, 0));

        repeatCard.addView(repeatHeader, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // Divider
        View dividerRepeat = new View(context);
        dividerRepeat.setBackgroundColor(getThemedColor(Theme.key_divider));
        repeatCard.addView(dividerRepeat, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 0, 12, 0, 12));

        // Value text
        repeatValueText = new TextView(context);
        repeatValueText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        repeatValueText.setTypeface(AndroidUtilities.bold());
        repeatValueText.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
        repeatValueText.setGravity(Gravity.START);
        repeatCard.addView(repeatValueText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView repeatHintText = new TextView(context);
        repeatHintText.setText(getString("HudScheduledMessagesTapToChangeRepeat"));
        repeatHintText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        repeatHintText.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText2));
        repeatHintText.setGravity(Gravity.START);
        repeatCard.addView(repeatHintText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 0));

        repeatCard.setOnClickListener(v -> openRepeatSelector());

        // --- Card 4: Message Content ---
        LinearLayout messageCard = new LinearLayout(context);
        messageCard.setOrientation(LinearLayout.VERTICAL);
        messageCard.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(12), getThemedColor(Theme.key_windowBackgroundWhite)));
        messageCard.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16));
        container.addView(messageCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 16, 0, 16));

        // Message Header (Horizontal with Templates Button)
        LinearLayout messageHeader = new LinearLayout(context);
        messageHeader.setOrientation(LinearLayout.HORIZONTAL);
        messageHeader.setGravity(Gravity.CENTER_VERTICAL);
        
        ImageView messageIcon = new ImageView(context);
        messageIcon.setImageResource(R.drawable.msg_message);
        messageIcon.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_windowBackgroundWhiteBlueHeader), PorterDuff.Mode.SRC_IN));
        messageHeader.addView(messageIcon, LayoutHelper.createLinear(20, 20));

        TextView messageTitle = new TextView(context);
        messageTitle.setText(getString("HudScheduledMessagesMsgHint"));
        messageTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        messageTitle.setTypeface(AndroidUtilities.bold());
        messageTitle.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlueHeader));
        messageTitle.setGravity(Gravity.START);
        messageHeader.addView(messageTitle, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 8, 0, 0, 0));

        // Spacer to push template button to end
        View spacerMessageHeader = new View(context);
        messageHeader.addView(spacerMessageHeader, LayoutHelper.createLinear(0, 0, 1f));

        // Templates Action text button
        TextView templatesBtn = new TextView(context);
        templatesBtn.setText(getString("HudScheduledMessagesTemplates"));
        templatesBtn.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlueText));
        templatesBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        templatesBtn.setTypeface(AndroidUtilities.bold());
        templatesBtn.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(4), AndroidUtilities.dp(8), AndroidUtilities.dp(4));
        messageHeader.addView(templatesBtn, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
        templatesBtn.setOnClickListener(v -> openTemplatesSelector());

        messageCard.addView(messageHeader, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // Divider
        View divider3 = new View(context);
        divider3.setBackgroundColor(getThemedColor(Theme.key_divider));
        messageCard.addView(divider3, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 0, 12, 0, 12));

        // Add the message edit text field directly (making it fill parent width)
        if (messageEdit.getParent() != null) {
            ((ViewGroup) messageEdit.getParent()).removeView(messageEdit);
        }
        messageCard.addView(messageEdit, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // Add custom scroll view to the contentView of the activity
        contentView.addView(scrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.FILL));

        // Add RoundRect Floating Action Button (FAB) at bottom right
        FrameLayout fab = new FrameLayout(context);
        int fabColor = getThemedColor(Theme.key_featuredStickers_addButton);
        int fabPressedColor = getThemedColor(Theme.key_featuredStickers_addButtonPressed);
        if (fabPressedColor == 0) {
            fabPressedColor = Theme.blendOver(fabColor, 0x1A000000);
        }
        fab.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(12), fabColor, fabPressedColor));
        
        if (Build.VERSION.SDK_INT >= 21) {
            fab.setElevation(AndroidUtilities.dp(4));
            fab.setOutlineProvider(new android.view.ViewOutlineProvider() {
                @Override
                public void getOutline(View view, android.graphics.Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), AndroidUtilities.dp(12));
                }
            });
        }

        ImageView fabIcon = new ImageView(context);
        fabIcon.setImageResource(R.drawable.floating_check);
        fabIcon.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_chats_actionIcon), PorterDuff.Mode.SRC_IN));
        fab.addView(fabIcon, LayoutHelper.createFrame(24, 24, Gravity.CENTER));

        FrameLayout.LayoutParams lp = LayoutHelper.createFrame(56, 56, 
                Gravity.BOTTOM | Gravity.END, 
                16, 0, 16, 16);
        contentView.addView(fab, lp);

        fab.setOnClickListener(v -> saveScheduledMessage());

        org.telegram.ui.Components.ScaleStateListAnimator.apply(fab, 0.85f, 1.2f);

        // Update UI content
        updateSelectedRecipientsUI();
        updateTimeUI();
        updateRepeatUI();

        return fragmentView;
    }

    private void openRecipientsPicker() {
        UsersSelectActivity selectActivity = new UsersSelectActivity(true, selectedChatIds, 0);
        selectActivity.noChatTypes = true;
        selectActivity.setDelegate(new UsersSelectActivity.FilterUsersActivityDelegate() {
            @Override
            public void didSelectChats(ArrayList<Long> ids, int flags) {
                if (ids != null) {
                    selectedChatIds.clear();
                    selectedChatIds.addAll(ids);
                    updateSelectedRecipientsUI();
                }
            }
        });
        presentFragment(selectActivity);
    }

    private void openDateTimePicker() {
        Calendar currentCal = Calendar.getInstance();
        currentCal.set(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute, 0);
        long currentSec = currentCal.getTimeInMillis() / 1000;
        
        String title = getString("HudScheduledMessagesTimeSettings");
        String buttonText = LocaleController.getString(R.string.Set);

        AlertsCreator.createDatePickerDialog(getParentActivity(), title, buttonText, currentSec, (notify, scheduleDate, scheduleRepeatPeriod) -> {
            long timestampMs = (long) scheduleDate * 1000;
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(timestampMs);
            selectedYear = cal.get(Calendar.YEAR);
            selectedMonth = cal.get(Calendar.MONTH);
            selectedDay = cal.get(Calendar.DAY_OF_MONTH);
            selectedHour = cal.get(Calendar.HOUR_OF_DAY);
            selectedMinute = cal.get(Calendar.MINUTE);
            updateTimeUI();
        });
    }

    private void openRepeatSelector() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(getString("HudScheduledMessagesRepeat"));
        CharSequence[] options = new CharSequence[] {
            getString("HudScheduledMessagesRepeatNever"),
            getString("HudScheduledMessagesRepeatDaily"),
            getString("HudScheduledMessagesRepeatWeekly"),
            getString("HudScheduledMessagesRepeatMonthly"),
            getString("HudScheduledMessagesRepeatCustom")
        };
        builder.setItems(options, (dialog, which) -> {
            if (which == 4) {
                // Custom Repeat input
                AlertDialog.Builder inputBuilder = new AlertDialog.Builder(getParentActivity());
                inputBuilder.setTitle(getString("HudScheduledMessagesRepeatEnterHours"));
                final EditTextBoldCursor input = new EditTextBoldCursor(getParentActivity());
                input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                input.setCursorColor(getThemedColor(Theme.key_windowBackgroundWhiteInputFieldActivated));
                input.setCursorWidth(1.5f);
                
                FrameLayout inputContainer = new FrameLayout(getParentActivity());
                inputContainer.addView(input, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.FILL, 24, 16, 24, 16));
                inputBuilder.setView(inputContainer);

                inputBuilder.setPositiveButton(LocaleController.getString("OK", R.string.OK), (dialog2, which2) -> {
                    String val = input.getText().toString().trim();
                    int hours = 0;
                    try {
                        hours = Integer.parseInt(val);
                    } catch (Exception e) {}
                    if (hours <= 0) {
                        hours = 1;
                    }
                    repeatInterval = hours * 60L * 60L * 1000L;
                    repeatType = 4;
                    updateRepeatUI();
                });
                inputBuilder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                showDialog(inputBuilder.create());
            } else {
                repeatType = which;
                repeatInterval = 0;
                updateRepeatUI();
            }
        });
        showDialog(builder.create());
    }

    private void openTemplatesSelector() {
        final ArrayList<String> templates = HudScheduledMessagesManager.getTemplates();
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(getString("HudScheduledMessagesTemplates"));

        ArrayList<CharSequence> items = new ArrayList<>();
        for (String t : templates) {
            items.add(t);
        }
        items.add(getString("HudScheduledMessagesTemplateSave"));

        builder.setItems(items.toArray(new CharSequence[0]), (dialog, which) -> {
            if (which == templates.size()) {
                // Save current message as template
                String currentText = messageEdit.getText().toString().trim();
                if (!TextUtils.isEmpty(currentText)) {
                    HudScheduledMessagesManager.addTemplate(currentText);
                    BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_saved, getString("HudScheduledMessagesTemplateSaved")).show();
                } else {
                    BulletinFactory.of(this).createErrorBulletin(getString("HudScheduledMessagesFillFields")).show();
                }
            } else {
                // Template selected: show Load/Delete dialog
                String selectedTemplate = templates.get(which);
                AlertDialog.Builder optBuilder = new AlertDialog.Builder(getParentActivity());
                optBuilder.setTitle(getString("HudScheduledMessagesTemplateOptions"));
                CharSequence[] options = new CharSequence[] {
                    getString("HudScheduledMessagesTemplateUse"),
                    getString("HudScheduledMessagesTemplateDelete")
                };
                optBuilder.setItems(options, (dialog2, whichOption) -> {
                    if (whichOption == 0) {
                        messageEdit.setText(selectedTemplate);
                        messageEdit.setSelection(selectedTemplate.length());
                    } else if (whichOption == 1) {
                        AlertDialog.Builder confirm = new AlertDialog.Builder(getParentActivity());
                        confirm.setTitle(getString("HudScheduledMessagesTemplateDeleteConfirm"));
                        confirm.setPositiveButton(LocaleController.getString("Delete", R.string.Delete), (d, w) -> {
                            HudScheduledMessagesManager.deleteTemplate(selectedTemplate);
                            BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_delete, getString("HudScheduledMessagesTemplateDeleted")).show();
                        });
                        confirm.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                        showDialog(confirm.create());
                    }
                });
                showDialog(optBuilder.create());
            }
        });
        showDialog(builder.create());
    }

    private void updateSelectedRecipientsUI() {
        if (recipientsValueText == null) return;
        recipientsValueText.setText(resolveRecipientNames());
    }

    private void updateTimeUI() {
        if (timeValueText == null) return;
        timeValueText.setText(formatScheduledTime());
    }

    private void updateRepeatUI() {
        if (repeatValueText == null) return;
        switch (repeatType) {
            case 0:
                repeatValueText.setText(getString("HudScheduledMessagesRepeatNever"));
                break;
            case 1:
                repeatValueText.setText(getString("HudScheduledMessagesRepeatDaily"));
                break;
            case 2:
                repeatValueText.setText(getString("HudScheduledMessagesRepeatWeekly"));
                break;
            case 3:
                repeatValueText.setText(getString("HudScheduledMessagesRepeatMonthly"));
                break;
            case 4:
                long hours = repeatInterval / (60L * 60L * 1000L);
                repeatValueText.setText(String.format(Locale.getDefault(), getString("HudScheduledMessagesRepeatCustomFormatted"), getString("HudScheduledMessagesRepeatCustom"), hours));
                break;
        }
    }

    private void saveScheduledMessage() {
        String msgText = messageEdit.getText().toString().trim();
        if (selectedChatIds.isEmpty() || TextUtils.isEmpty(msgText)) {
            BulletinFactory.of(this).createErrorBulletin(getString("HudScheduledMessagesFillFields")).show();
            return;
        }

        Calendar targetCal = Calendar.getInstance();
        targetCal.set(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute, 0);
        long targetTime = targetCal.getTimeInMillis();
        if (targetTime <= System.currentTimeMillis()) {
            BulletinFactory.of(this).createErrorBulletin(getString("HudScheduledMessagesInvalidTime")).show();
            return;
        }

        ScheduledMessage msg = new ScheduledMessage();
        msg.chatIds.addAll(selectedChatIds);
        msg.message = msgText;
        msg.timestamp = targetTime;
        msg.accountId = currentAccount;
        msg.repeatType = repeatType;
        msg.repeatInterval = repeatInterval;

        HudScheduledMessagesManager.addScheduledMessage(getParentActivity(), msg);

        BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_saved, getString("HudScheduledMessagesAdded")).show();
        finishFragment();
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        // Not used anymore as we have a fully custom view
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        // Not used anymore as we have a fully custom view
    }

    private String resolveRecipientNames() {
        if (selectedChatIds.isEmpty()) {
            return getString("HudScheduledMessagesSelectChatsDetail");
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < selectedChatIds.size(); i++) {
            long chatId = selectedChatIds.get(i);
            String name = null;
            if (chatId > 0) {
                org.telegram.tgnet.TLRPC.User user = org.telegram.messenger.MessagesController.getInstance(currentAccount).getUser(chatId);
                if (user != null) {
                    name = UserObject.getUserName(user);
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

    private String formatScheduledTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute, 0);
        long time = calendar.getTimeInMillis();

        Calendar now = Calendar.getInstance();
        int currentYear = now.get(Calendar.YEAR);
        int currentDay = now.get(Calendar.DAY_OF_YEAR);
        
        boolean isToday = now.get(Calendar.YEAR) == selectedYear && now.get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR);
        
        int num;
        if (isToday) {
            num = 0;
        } else if (currentYear == selectedYear) {
            num = 1;
        } else {
            num = 2;
        }
        return LocaleController.getInstance().getFormatterScheduleSend(num).format(time);
    }

    @Override
    public void onInsets(int left, int top, int right, int bottom) {
        int topPadding = needActionBarPadding() ? ActionBar.getCurrentActionBarHeight() : AndroidUtilities.dp(12);
        if (scrollView != null) {
            scrollView.setPadding(0, top + topPadding, 0, bottom);
        }
    }

    @Override
    protected String getActionBarTitle() {
        return getString("HudScheduledMessagesTitle");
    }

    @Override
    protected String getKey() {
        return "scheduledMessageAdd";
    }
}
