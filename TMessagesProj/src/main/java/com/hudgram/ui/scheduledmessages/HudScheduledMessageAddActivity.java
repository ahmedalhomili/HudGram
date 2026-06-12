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
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.FragmentFloatingButton;

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
    private LinearLayout container; // inner container inside ScrollView
    private int systemBottomInset = 0;
    private org.telegram.ui.Components.SizeNotifierFrameLayout.SizeNotifierFrameLayoutDelegate sizeNotifierDelegate;
    private LinearLayout recipientsContentContainer;
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
        boolean isRtl = LocaleController.isRTL;

        // 1. Initialize messageEdit edit text field first
        messageEdit = new EditTextBoldCursor(context);
        messageEdit.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        messageEdit.setHintTextColor(getThemedColor(Theme.key_windowBackgroundWhiteHintText));
        messageEdit.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
        messageEdit.setBackground(null);
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

        // 4. Create Custom ScrollView (no fillViewport so content padding works for scrolling)
        scrollView = new ScrollView(context);
        scrollView.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundGray));

        // 5. Create Container Layout (inner container - its bottom padding increases scrollable area)
        container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(8), AndroidUtilities.dp(16), AndroidUtilities.dp(80));
        scrollView.addView(container, LayoutHelper.createScroll(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP));

        // --- Card 1: Message Content ---
        LinearLayout messageCard = new LinearLayout(context);
        messageCard.setOrientation(LinearLayout.VERTICAL);
        messageCard.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(12), getThemedColor(Theme.key_windowBackgroundWhite)));
        messageCard.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16));
        container.addView(messageCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 8, 0, 0));

        // Message Header (FrameLayout-based)
        FrameLayout messageHeader = new FrameLayout(context);
        
        LinearLayout titleLayout4 = new LinearLayout(context);
        titleLayout4.setOrientation(LinearLayout.HORIZONTAL);
        titleLayout4.setGravity(Gravity.CENTER_VERTICAL);
        
        ImageView messageIcon = new ImageView(context);
        messageIcon.setImageResource(R.drawable.msg_message);
        messageIcon.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_windowBackgroundWhiteBlueHeader), PorterDuff.Mode.SRC_IN));
        titleLayout4.addView(messageIcon, LayoutHelper.createLinear(20, 20));

        TextView messageTitle = new TextView(context);
        messageTitle.setText(getString("HudScheduledMessagesMsgHint"));
        messageTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        messageTitle.setTypeface(AndroidUtilities.bold());
        messageTitle.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlueHeader));
        titleLayout4.addView(messageTitle, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 8, 0, 8, 0));

        messageHeader.addView(titleLayout4, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL | (isRtl ? Gravity.RIGHT : Gravity.LEFT)));

        TextView templatesBtn = new TextView(context);
        templatesBtn.setText(getString("HudScheduledMessagesTemplates"));
        templatesBtn.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlueText));
        templatesBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        templatesBtn.setTypeface(AndroidUtilities.bold());
        templatesBtn.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(4), AndroidUtilities.dp(8), AndroidUtilities.dp(4));
        messageHeader.addView(templatesBtn, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL | (isRtl ? Gravity.LEFT : Gravity.RIGHT)));
        templatesBtn.setOnClickListener(v -> openTemplatesSelector());

        messageCard.addView(messageHeader, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // Divider
        View divider3 = new View(context);
        divider3.setBackgroundColor(getThemedColor(Theme.key_divider));
        messageCard.addView(divider3, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 0, 12, 0, 12));

        // Rounded Gray Input Container
        FrameLayout inputContainer = new FrameLayout(context);
        int inputBg = getThemedColor(Theme.key_chat_messagePanelBackground);
        if (inputBg == 0) {
            inputBg = 0xf2f5f8f9; // sleek light-gray/blue tint fallback
        }
        inputContainer.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(8), inputBg));
        inputContainer.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(6), AndroidUtilities.dp(12), AndroidUtilities.dp(6));
        messageCard.addView(inputContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 0));

        // Add the message edit text field inside gray container
        if (messageEdit.getParent() != null) {
            ((ViewGroup) messageEdit.getParent()).removeView(messageEdit);
        }
        inputContainer.addView(messageEdit, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // --- Card 2: Recipients ---
        LinearLayout recipientsCard = new LinearLayout(context);
        recipientsCard.setOrientation(LinearLayout.VERTICAL);
        recipientsCard.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(12), getThemedColor(Theme.key_windowBackgroundWhite)));
        recipientsCard.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16));
        container.addView(recipientsCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 16, 0, 0));

        // FrameLayout-based header
        FrameLayout recipientsHeader = new FrameLayout(context);
        
        LinearLayout titleLayout1 = new LinearLayout(context);
        titleLayout1.setOrientation(LinearLayout.HORIZONTAL);
        titleLayout1.setGravity(Gravity.CENTER_VERTICAL);
        
        ImageView recipientsIcon = new ImageView(context);
        recipientsIcon.setImageResource(R.drawable.msg_contacts);
        recipientsIcon.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_windowBackgroundWhiteBlueHeader), PorterDuff.Mode.SRC_IN));
        titleLayout1.addView(recipientsIcon, LayoutHelper.createLinear(20, 20));

        TextView recipientsTitle = new TextView(context);
        recipientsTitle.setText(getString("HudScheduledMessagesSelectChats"));
        recipientsTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        recipientsTitle.setTypeface(AndroidUtilities.bold());
        recipientsTitle.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlueHeader));
        titleLayout1.addView(recipientsTitle, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 8, 0, 8, 0));
        
        recipientsHeader.addView(titleLayout1, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL | (isRtl ? Gravity.RIGHT : Gravity.LEFT)));

        TextView selectHint = new TextView(context);
        selectHint.setText(getString("HudScheduledMessagesEditAction"));
        selectHint.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        selectHint.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlueText));
        selectHint.setTypeface(AndroidUtilities.bold());
        recipientsHeader.addView(selectHint, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL | (isRtl ? Gravity.LEFT : Gravity.RIGHT)));

        recipientsCard.addView(recipientsHeader, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // Divider
        View divider1 = new View(context);
        divider1.setBackgroundColor(getThemedColor(Theme.key_divider));
        recipientsCard.addView(divider1, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 0, 12, 0, 12));

        // Content container
        recipientsContentContainer = new LinearLayout(context);
        recipientsContentContainer.setOrientation(LinearLayout.HORIZONTAL);
        recipientsContentContainer.setGravity(Gravity.CENTER_VERTICAL);
        recipientsCard.addView(recipientsContentContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        recipientsCard.setOnClickListener(v -> openRecipientsPicker());

        // --- Card 3: Time Settings ---
        LinearLayout timeCard = new LinearLayout(context);
        timeCard.setOrientation(LinearLayout.VERTICAL);
        timeCard.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(12), getThemedColor(Theme.key_windowBackgroundWhite)));
        timeCard.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16));
        container.addView(timeCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 16, 0, 0));

        // Time Header (FrameLayout-based)
        FrameLayout timeHeader = new FrameLayout(context);
        
        LinearLayout titleLayout2 = new LinearLayout(context);
        titleLayout2.setOrientation(LinearLayout.HORIZONTAL);
        titleLayout2.setGravity(Gravity.CENTER_VERTICAL);
        
        ImageView timeIcon = new ImageView(context);
        timeIcon.setImageResource(R.drawable.msg_calendar2);
        timeIcon.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_windowBackgroundWhiteBlueHeader), PorterDuff.Mode.SRC_IN));
        titleLayout2.addView(timeIcon, LayoutHelper.createLinear(20, 20));

        TextView timeTitle = new TextView(context);
        timeTitle.setText(getString("HudScheduledMessagesTimeSettings"));
        timeTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        timeTitle.setTypeface(AndroidUtilities.bold());
        timeTitle.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlueHeader));
        titleLayout2.addView(timeTitle, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 8, 0, 8, 0));

        timeHeader.addView(titleLayout2, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL | (isRtl ? Gravity.RIGHT : Gravity.LEFT)));
        timeCard.addView(timeHeader, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // Divider
        View divider2 = new View(context);
        divider2.setBackgroundColor(getThemedColor(Theme.key_divider));
        timeCard.addView(divider2, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 0, 12, 0, 12));

        // Time Content Row (Horizontal)
        LinearLayout timeContentRow = new LinearLayout(context);
        timeContentRow.setOrientation(LinearLayout.HORIZONTAL);
        timeContentRow.setGravity(Gravity.CENTER_VERTICAL);
        timeCard.addView(timeContentRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        ImageView timeContentIcon = new ImageView(context);
        timeContentIcon.setImageResource(R.drawable.menu_premium_clock);
        timeContentIcon.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_windowBackgroundWhiteBlueText2), PorterDuff.Mode.SRC_IN));
        timeContentRow.addView(timeContentIcon, LayoutHelper.createLinear(24, 24, Gravity.CENTER_VERTICAL, 0, 0, 12, 0));

        LinearLayout timeTextLayout = new LinearLayout(context);
        timeTextLayout.setOrientation(LinearLayout.VERTICAL);
        timeContentRow.addView(timeTextLayout, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));

        timeValueText = new TextView(context);
        timeValueText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        timeValueText.setTypeface(AndroidUtilities.bold());
        timeValueText.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
        timeValueText.setGravity(Gravity.START);
        timeTextLayout.addView(timeValueText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView timeSubtext = new TextView(context);
        timeSubtext.setText(getString("HudScheduledMessagesTapToEdit"));
        timeSubtext.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        timeSubtext.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText2));
        timeSubtext.setGravity(Gravity.START);
        timeTextLayout.addView(timeSubtext, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

        timeCard.setOnClickListener(v -> openDateTimePicker());

        // --- Card 4: Repeat Settings (Recurrence) ---
        LinearLayout repeatCard = new LinearLayout(context);
        repeatCard.setOrientation(LinearLayout.VERTICAL);
        repeatCard.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(12), getThemedColor(Theme.key_windowBackgroundWhite)));
        repeatCard.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16));
        container.addView(repeatCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 16, 0, 16));

        // Repeat Header (FrameLayout-based)
        FrameLayout repeatHeader = new FrameLayout(context);
        
        LinearLayout titleLayout3 = new LinearLayout(context);
        titleLayout3.setOrientation(LinearLayout.HORIZONTAL);
        titleLayout3.setGravity(Gravity.CENTER_VERTICAL);

        ImageView repeatIcon = new ImageView(context);
        repeatIcon.setImageResource(R.drawable.msg_retry);
        repeatIcon.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_windowBackgroundWhiteBlueHeader), PorterDuff.Mode.SRC_IN));
        titleLayout3.addView(repeatIcon, LayoutHelper.createLinear(20, 20));

        TextView repeatTitle = new TextView(context);
        repeatTitle.setText(getString("HudScheduledMessagesRepeat"));
        repeatTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        repeatTitle.setTypeface(AndroidUtilities.bold());
        repeatTitle.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlueHeader));
        titleLayout3.addView(repeatTitle, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 8, 0, 8, 0));

        repeatHeader.addView(titleLayout3, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL | (isRtl ? Gravity.RIGHT : Gravity.LEFT)));
        repeatCard.addView(repeatHeader, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // Divider
        View dividerRepeat = new View(context);
        dividerRepeat.setBackgroundColor(getThemedColor(Theme.key_divider));
        repeatCard.addView(dividerRepeat, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 0, 12, 0, 12));

        // Repeat Content Row (Horizontal)
        LinearLayout repeatContentRow = new LinearLayout(context);
        repeatContentRow.setOrientation(LinearLayout.HORIZONTAL);
        repeatContentRow.setGravity(Gravity.CENTER_VERTICAL);
        repeatCard.addView(repeatContentRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        ImageView repeatContentIcon = new ImageView(context);
        repeatContentIcon.setImageResource(R.drawable.msg_retry);
        repeatContentIcon.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_windowBackgroundWhiteGreenText), PorterDuff.Mode.SRC_IN));
        repeatContentRow.addView(repeatContentIcon, LayoutHelper.createLinear(24, 24, Gravity.CENTER_VERTICAL, 0, 0, 12, 0));

        LinearLayout repeatTextLayout = new LinearLayout(context);
        repeatTextLayout.setOrientation(LinearLayout.VERTICAL);
        repeatContentRow.addView(repeatTextLayout, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));

        repeatValueText = new TextView(context);
        repeatValueText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        repeatValueText.setTypeface(AndroidUtilities.bold());
        repeatValueText.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
        repeatValueText.setGravity(Gravity.START);
        repeatTextLayout.addView(repeatValueText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView repeatSubtext = new TextView(context);
        repeatSubtext.setText(getString("HudScheduledMessagesTapToEdit"));
        repeatSubtext.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        repeatSubtext.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText2));
        repeatSubtext.setGravity(Gravity.START);
        repeatTextLayout.addView(repeatSubtext, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

        repeatCard.setOnClickListener(v -> openRepeatSelector());

        // Add custom scroll view to the contentView of the activity (at index 0 so it's drawn behind the action bar)
        scrollView.setClipToPadding(false);
        contentView.addView(scrollView, 0, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.FILL));

        if (actionBarBackground != null) {
            actionBarBackground.setAlpha(1.0f);
        }

        // --- Keyboard handling: change CONTAINER's bottom padding to increase scrollable area ---
        sizeNotifierDelegate = new org.telegram.ui.Components.SizeNotifierFrameLayout.SizeNotifierFrameLayoutDelegate() {
            @Override
            public void onSizeChanged(int keyboardHeight, boolean isWidthGreater) {
                if (container != null) {
                    // Increase the container's bottom padding to create extra scrollable space
                    int baseBottomPadding = AndroidUtilities.dp(80); // space for FAB
                    int extraForKeyboard = keyboardHeight > 0 ? keyboardHeight : 0;
                    container.setPadding(
                        container.getPaddingLeft(),
                        container.getPaddingTop(),
                        container.getPaddingRight(),
                        baseBottomPadding + extraForKeyboard
                    );
                }
                if (keyboardHeight > 0 && messageEdit != null && messageEdit.hasFocus() && scrollView != null) {
                    scrollView.postDelayed(() -> {
                        // Calculate messageEdit position relative to scrollView
                        int scrollTarget = 0;
                        View v = messageEdit;
                        while (v != null && v != scrollView) {
                            scrollTarget += v.getTop();
                            if (v.getParent() instanceof View) {
                                v = (View) v.getParent();
                            } else {
                                break;
                            }
                        }
                        // Scroll so messageEdit's top is visible with some context above
                        int desiredScroll = scrollTarget - AndroidUtilities.dp(100);
                        if (desiredScroll < 0) desiredScroll = 0;
                        scrollView.smoothScrollTo(0, desiredScroll);
                    }, 150);
                }
            }
        };
        contentView.addDelegate(sizeNotifierDelegate);

        // Add RoundRect Floating Action Button (FAB) at bottom right (LTR) or bottom left (RTL)
        FragmentFloatingButton fab = new FragmentFloatingButton(context, getResourceProvider());
        fab.setImageResource(R.drawable.floating_check);
        contentView.addView(fab, FragmentFloatingButton.createDefaultLayoutParams());

        fab.setOnClickListener(v -> saveScheduledMessage());

        // Focus listener: when messageEdit gets focus, scroll to it after keyboard appears
        messageEdit.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && scrollView != null) {
                scrollView.postDelayed(() -> {
                    int keyboardHeight = contentView != null ? contentView.getKeyboardHeight() : 0;
                    if (keyboardHeight > 0) {
                        int scrollTarget = 0;
                        View child = messageEdit;
                        while (child != null && child != scrollView) {
                            scrollTarget += child.getTop();
                            if (child.getParent() instanceof View) {
                                child = (View) child.getParent();
                            } else {
                                break;
                            }
                        }
                        int desiredScroll = scrollTarget - AndroidUtilities.dp(100);
                        if (desiredScroll < 0) desiredScroll = 0;
                        scrollView.smoothScrollTo(0, desiredScroll);
                    }
                }, 400);
            }
        });

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
        if (recipientsContentContainer == null) return;
        recipientsContentContainer.removeAllViews();
        
        if (selectedChatIds.isEmpty()) {
            TextView emptyText = new TextView(getParentActivity());
            emptyText.setText(getString("HudScheduledMessagesSelectChatsSubtitle"));
            emptyText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            emptyText.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText2));
            emptyText.setGravity(Gravity.START);
            recipientsContentContainer.addView(emptyText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
            return;
        }

        FrameLayout avatarsContainer = new FrameLayout(getParentActivity());
        
        int limit = Math.min(selectedChatIds.size(), 4);
        int avatarSize = 32;
        int overlap = 8;
        
        for (int i = 0; i < limit; i++) {
            long chatId = selectedChatIds.get(i);
            BackupImageView avatarView = new BackupImageView(getParentActivity());
            avatarView.setRoundRadius(AndroidUtilities.dp(avatarSize / 2));
            AvatarDrawable avatarDrawable = new AvatarDrawable();
            
            if (chatId > 0) {
                org.telegram.tgnet.TLRPC.User user = org.telegram.messenger.MessagesController.getInstance(currentAccount).getUser(chatId);
                if (user != null) {
                    avatarView.setForUserOrChat(user, avatarDrawable);
                } else {
                    avatarDrawable.setInfo(chatId, "User", null);
                    avatarView.setImageDrawable(avatarDrawable);
                }
            } else {
                org.telegram.tgnet.TLRPC.Chat chat = org.telegram.messenger.MessagesController.getInstance(currentAccount).getChat(-chatId);
                if (chat != null) {
                    avatarView.setForUserOrChat(chat, avatarDrawable);
                } else {
                    avatarDrawable.setInfo(chatId, "Chat", null);
                    avatarView.setImageDrawable(avatarDrawable);
                }
            }
            
            FrameLayout.LayoutParams avatarLp = LayoutHelper.createFrame(avatarSize, avatarSize, Gravity.START | Gravity.CENTER_VERTICAL);
            if (LocaleController.isRTL) {
                avatarLp.rightMargin = i * AndroidUtilities.dp(avatarSize - overlap);
            } else {
                avatarLp.leftMargin = i * AndroidUtilities.dp(avatarSize - overlap);
            }
            avatarsContainer.addView(avatarView, avatarLp);
        }
        
        int avatarsWidth = limit * avatarSize - (limit - 1) * overlap;
        recipientsContentContainer.addView(avatarsContainer, LayoutHelper.createLinear(avatarsWidth, 36, Gravity.CENTER_VERTICAL));
        
        TextView infoText = new TextView(getParentActivity());
        infoText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        infoText.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
        infoText.setTypeface(AndroidUtilities.bold());
        infoText.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        
        if (selectedChatIds.size() == 1) {
            infoText.setText(resolveRecipientNames());
        } else {
            infoText.setText(String.format(Locale.getDefault(), "%d %s", selectedChatIds.size(), LocaleController.getString("HudScheduledMessagesSelectChats", R.string.HudScheduledMessagesSelectChats)));
        }
        
        recipientsContentContainer.addView(infoText, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 12, 0, 12, 0));
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
        systemBottomInset = bottom;
        int topPadding = needActionBarPadding() ? ActionBar.getCurrentActionBarHeight() : AndroidUtilities.dp(12);
        if (scrollView != null) {
            scrollView.setPadding(0, top + topPadding, 0, 0);
        }
    }

    @Override
    protected String getActionBarTitle() {
        return getString("HudScheduledMessagesTitle");
    }

    @Override
    protected void updateActionBarVisible() {
        if (actionBarBackground != null) {
            actionBarBackground.setAlpha(1.0f);
        }
    }

    @Override
    protected String getKey() {
        return "scheduledMessageAdd";
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        if (contentView != null && sizeNotifierDelegate != null) {
            contentView.removeDelegate(sizeNotifierDelegate);
        }
    }
}
