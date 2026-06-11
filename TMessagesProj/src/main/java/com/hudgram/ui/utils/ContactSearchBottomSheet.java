package com.hudgram.ui.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.os.Bundle;
import android.telephony.TelephonyManager;
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
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.PhoneFormat.PhoneFormat;
import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.INavigationLayout;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RadialProgressView;
import org.telegram.ui.Components.voip.VoIPHelper;
import org.telegram.ui.Components.AnimatedPhoneNumberEditText;
import org.telegram.ui.CountrySelectActivity;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.Components.OutlineEditText;
import org.telegram.ui.Components.OutlineTextContainerView;
import android.view.inputmethod.EditorInfo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class ContactSearchBottomSheet extends BottomSheet {

    private final Activity activity;
    private final int currentAccount;
    private final INavigationLayout navigationLayout;

    private int currentTab = 0;
    private TextView[] tabViews;

    // Containers
    private FrameLayout inputContainer;
    private FrameLayout idContainer;
    private OutlineTextContainerView phoneOutlineView;
    private LinearLayout phoneContainer;
    private FrameLayout usernameContainer;

    // Fields
    private OutlineEditText idField;
    private OutlineEditText usernameField;
    private AnimatedPhoneNumberEditText codeField;
    private AnimatedPhoneNumberEditText phoneField;
    private TextView countryFlag;
    private TextView plusTextView;

    // Verification Card
    private FrameLayout verifyCard;
    private BackupImageView avatarView;
    private TextView nameText;
    private TextView statusText;
    private TextView detailsText;
    private ImageView verifiedBadge;
    private ImageView premiumBadge;
    private TextView scamBadge;
    private TextView fakeBadge;
    private FrameLayout verifyButton;
    private TextView verifyButtonText;
    private RadialProgressView verifyProgressView;

    // Action Buttons
    private FrameLayout chatButton;
    private FrameLayout callButton;
    private ImageView chatIcon;
    private ImageView callIcon;
    private TextView chatText;
    private TextView callText;

    // Resolved Peer Info
    private TLRPC.User resolvedUser;
    private TLRPC.Chat resolvedChat;
    private long resolvedPeerId;

    // Country selection helpers
    private ArrayList<CountrySelectActivity.Country> countriesArray = new ArrayList<>();
    private HashMap<String, List<CountrySelectActivity.Country>> codesMap = new HashMap<>();
    private HashMap<String, List<String>> phoneFormatMap = new HashMap<>();
    private boolean ignoreOnTextChange;

    public ContactSearchBottomSheet(final Activity activity, final int currentAccount, final Theme.ResourcesProvider resourcesProvider, final INavigationLayout navigationLayout) {
        super(activity, true, resourcesProvider);
        this.activity = activity;
        this.currentAccount = currentAccount;
        this.navigationLayout = navigationLayout;

        setDimBehind(true);

        Context context = getContext();

        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);

        LinearLayout contentLayout = new LinearLayout(context);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(16), AndroidUtilities.dp(20), AndroidUtilities.dp(16));

        // 1. Title
        TextView titleText = new TextView(context);
        titleText.setText(LocaleController.getString("HudMessageByNumber", R.string.HudMessageByNumber));
        titleText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider));
        titleText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 19);
        titleText.setTypeface(AndroidUtilities.bold());
        titleText.setGravity(Gravity.CENTER);
        contentLayout.addView(titleText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        // 2. Tab segment bar
        LinearLayout tabLayout = new LinearLayout(context);
        tabLayout.setOrientation(LinearLayout.HORIZONTAL);
        tabLayout.setWeightSum(3);
        tabLayout.setBackground(Theme.createSimpleSelectorRoundRectDrawable(
                AndroidUtilities.dp(8),
                Theme.getColor(Theme.key_windowBackgroundGray, resourcesProvider),
                Theme.getColor(Theme.key_windowBackgroundGray, resourcesProvider)
        ));
        tabLayout.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4));
        contentLayout.addView(tabLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        tabViews = new TextView[3];
        for (int i = 0; i < 3; i++) {
            final int index = i;
            tabViews[i] = new TextView(context);
            tabViews[i].setGravity(Gravity.CENTER);
            tabViews[i].setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            tabViews[i].setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(8));
            tabViews[i].setTypeface(AndroidUtilities.bold());
            tabViews[i].setOnClickListener(v -> switchTab(index));

            if (i == 0) {
                tabViews[i].setText(LocaleController.getString("HudSearchTabId", R.string.HudSearchTabId));
            } else if (i == 1) {
                tabViews[i].setText(LocaleController.getString("HudSearchTabPhone", R.string.HudSearchTabPhone));
            } else {
                tabViews[i].setText(LocaleController.getString("HudSearchTabUsername", R.string.HudSearchTabUsername));
            }

            tabLayout.addView(tabViews[i], LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f, 2, 0, 2, 0));
        }

        // 3. Input fields container
        inputContainer = new FrameLayout(context);
        contentLayout.addView(inputContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 58, 0, 0, 0, 16));

        // 3a. ID input layout
        idContainer = new FrameLayout(context);
        idField = new OutlineEditText(context);
        idField.setHint(LocaleController.getString("HudSearchTabId", R.string.HudSearchTabId) + " (e.g. 1234567)");
        idField.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
        idField.getEditText().setSingleLine(true);
        idField.getEditText().setTextDirection(View.TEXT_DIRECTION_LTR);
        idField.getEditText().setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        idField.getEditText().setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        idField.getEditText().addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { resetVerification(); }
        });
        idContainer.addView(idField, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 58, Gravity.CENTER_VERTICAL));
        inputContainer.addView(idContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // 3b. Phone input layout
        phoneOutlineView = new OutlineTextContainerView(context);
        phoneOutlineView.setText(LocaleController.getString("HudSearchTabPhone", R.string.HudSearchTabPhone));

        phoneContainer = new LinearLayout(context);
        phoneContainer.setOrientation(LinearLayout.HORIZONTAL);
        phoneContainer.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        phoneContainer.setGravity(Gravity.CENTER_VERTICAL);
        phoneContainer.setPadding(0, AndroidUtilities.dp(4), 0, AndroidUtilities.dp(4));

        countryFlag = new TextView(context);
        countryFlag.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        countryFlag.setGravity(Gravity.CENTER);
        countryFlag.setText("🌐");
        org.telegram.messenger.NotificationCenter.listenEmojiLoading(countryFlag);

        FrameLayout countryContainer = new FrameLayout(context);
        countryContainer.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(6), 0, Theme.getColor(Theme.key_listSelector, resourcesProvider)));
        countryContainer.addView(countryFlag, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));
        countryContainer.setOnClickListener(v -> {
            CountrySelectActivity countrySelectActivity = new CountrySelectActivity(true);
            countrySelectActivity.setCountrySelectActivityDelegate(country -> {
                selectCountry(country);
                AndroidUtilities.runOnUIThread(() -> AndroidUtilities.showKeyboard(phoneField), 300);
                phoneField.requestFocus();
                phoneField.setSelection(phoneField.length());
            });
            LaunchActivity.getLastFragment().showAsSheet(countrySelectActivity);
        });
        phoneContainer.addView(countryContainer, LayoutHelper.createLinear(42, LayoutHelper.MATCH_PARENT));

        plusTextView = new TextView(context);
        plusTextView.setText("+");
        plusTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        plusTextView.setFocusable(false);
        plusTextView.setGravity(Gravity.CENTER_VERTICAL);
        plusTextView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider));
        phoneContainer.addView(plusTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER_VERTICAL, 4, 0, 0, 0));

        codeField = new AnimatedPhoneNumberEditText(context);
        codeField.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider));
        codeField.setInputType(InputType.TYPE_CLASS_PHONE);
        codeField.setBackground(null);
        codeField.setCursorSize(AndroidUtilities.dp(20));
        codeField.setCursorWidth(1.5f);
        codeField.setPadding(AndroidUtilities.dp(10), 0, 0, 0);
        codeField.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        codeField.setMaxLines(1);
        codeField.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        codeField.setImeOptions(EditorInfo.IME_ACTION_NEXT | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        codeField.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        codeField.setTextDirection(View.TEXT_DIRECTION_LTR);
        phoneContainer.addView(codeField, LayoutHelper.createLinear(55, 36, -9, 0, 0, 0));

        codeField.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (ignoreOnTextChange) return;
                ignoreOnTextChange = true;
                resetVerification();
                String text = PhoneFormat.stripExceptNumbers(codeField.getText().toString());
                codeField.setText(text);
                codeField.setSelection(codeField.getText().length());
                if (text.length() == 0) {
                    countryFlag.setText("🌐");
                    phoneField.setHint(LocaleController.getString("HudMessageByNumberHint", R.string.HudMessageByNumberHint));
                } else {
                    CountrySelectActivity.Country country = null;
                    List<CountrySelectActivity.Country> list = codesMap.get(text);
                    if (list != null && !list.isEmpty()) {
                        country = list.get(0);
                    }
                    if (country != null) {
                        setCountryFlagAndHint(country);
                    } else {
                        countryFlag.setText("🌐");
                        phoneField.setHint(LocaleController.getString("HudMessageByNumberHint", R.string.HudMessageByNumberHint));
                    }
                }
                ignoreOnTextChange = false;
            }
        });
        codeField.setOnEditorActionListener((textView, i, keyEvent) -> {
            if (i == EditorInfo.IME_ACTION_NEXT) {
                phoneField.requestFocus();
                phoneField.setSelection(phoneField.length());
                return true;
            }
            return false;
        });

        View codeDivider = new View(context);
        codeDivider.setBackgroundColor(Theme.getColor(Theme.key_divider, resourcesProvider));
        android.widget.LinearLayout.LayoutParams dividerParams = LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 4, 8, 12, 8);
        dividerParams.width = Math.max(2, AndroidUtilities.dp(0.5f));
        phoneContainer.addView(codeDivider, dividerParams);

        phoneField = new AnimatedPhoneNumberEditText(context);
        phoneField.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider));
        phoneField.setInputType(InputType.TYPE_CLASS_PHONE);
        phoneField.setBackground(null);
        phoneField.setPadding(0, 0, 0, 0);
        phoneField.setCursorSize(AndroidUtilities.dp(20));
        phoneField.setCursorWidth(1.5f);
        phoneField.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        phoneField.setMaxLines(1);
        phoneField.setHint(LocaleController.getString("HudMessageByNumberHint", R.string.HudMessageByNumberHint));
        phoneField.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint, resourcesProvider));
        phoneField.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        phoneField.setImeOptions(EditorInfo.IME_ACTION_DONE | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        phoneField.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        phoneField.setTextDirection(View.TEXT_DIRECTION_LTR);
        phoneField.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { resetVerification(); }
        });
        phoneContainer.addView(phoneField, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 36));
        
        phoneOutlineView.addView(phoneContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 4, 8, 16, 8));
        
        codeField.setOnFocusChangeListener((v, hasFocus) -> phoneOutlineView.animateSelection(hasFocus || phoneField.isFocused() ? 1f : 0f));
        phoneField.setOnFocusChangeListener((v, hasFocus) -> phoneOutlineView.animateSelection(hasFocus || codeField.isFocused() ? 1f : 0f));
        
        inputContainer.addView(phoneOutlineView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // 3c. Username input layout
        usernameContainer = new FrameLayout(context);
        usernameField = new OutlineEditText(context);
        usernameField.setHint(LocaleController.getString("HudSearchTabUsername", R.string.HudSearchTabUsername) + " (e.g. @username)");
        usernameField.getEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        usernameField.getEditText().setSingleLine(true);
        usernameField.getEditText().setTextDirection(View.TEXT_DIRECTION_LTR);
        usernameField.getEditText().setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        usernameField.getEditText().setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        usernameField.getEditText().addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { resetVerification(); }
        });
        usernameContainer.addView(usernameField, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 58, Gravity.CENTER_VERTICAL));
        inputContainer.addView(usernameContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // 4. Verify Card
        verifyCard = new FrameLayout(context);
        verifyCard.setBackground(Theme.createSimpleSelectorRoundRectDrawable(
                AndroidUtilities.dp(12),
                Theme.getColor(Theme.key_windowBackgroundGray, resourcesProvider),
                Theme.getColor(Theme.key_windowBackgroundGray, resourcesProvider)
        ));
        verifyCard.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(12));
        contentLayout.addView(verifyCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        LinearLayout cardLayout = new LinearLayout(context);
        cardLayout.setOrientation(LinearLayout.HORIZONTAL);
        cardLayout.setGravity(Gravity.CENTER_VERTICAL);
        verifyCard.addView(cardLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        avatarView = new BackupImageView(context);
        avatarView.setRoundRadius(AndroidUtilities.dp(27));
        avatarView.setVisibility(View.GONE);
        cardLayout.addView(avatarView, LayoutHelper.createLinear(54, 54, Gravity.CENTER_VERTICAL, 0, 0, 12, 0));

        LinearLayout textLayout = new LinearLayout(context);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        cardLayout.addView(textLayout, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f, Gravity.CENTER_VERTICAL, 0, 0, 8, 0));

        // Name Row
        LinearLayout nameRow = new LinearLayout(context);
        nameRow.setOrientation(LinearLayout.HORIZONTAL);
        nameRow.setGravity(Gravity.CENTER_VERTICAL);
        textLayout.addView(nameRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        nameText = new TextView(context);
        nameText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider));
        nameText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        nameText.setTypeface(AndroidUtilities.bold());
        nameRow.addView(nameText, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        verifiedBadge = new ImageView(context);
        verifiedBadge.setImageResource(R.drawable.verified_profile);
        verifiedBadge.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_chats_verifiedBackground, resourcesProvider), PorterDuff.Mode.SRC_IN));
        verifiedBadge.setVisibility(View.GONE);
        nameRow.addView(verifiedBadge, LayoutHelper.createLinear(16, 16, Gravity.CENTER_VERTICAL, 6, 0, 0, 0));

        premiumBadge = new ImageView(context);
        premiumBadge.setImageResource(R.drawable.msg_premium_liststar);
        premiumBadge.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_chats_verifiedBackground, resourcesProvider), PorterDuff.Mode.SRC_IN));
        premiumBadge.setVisibility(View.GONE);
        nameRow.addView(premiumBadge, LayoutHelper.createLinear(16, 16, Gravity.CENTER_VERTICAL, 6, 0, 0, 0));

        scamBadge = new TextView(context);
        scamBadge.setText("SCAM");
        scamBadge.setTextColor(Color.WHITE);
        scamBadge.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 9);
        scamBadge.setTypeface(AndroidUtilities.bold());
        scamBadge.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(1), AndroidUtilities.dp(4), AndroidUtilities.dp(1));
        scamBadge.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(3), 0xFFD83C3C));
        scamBadge.setVisibility(View.GONE);
        nameRow.addView(scamBadge, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 6, 0, 0, 0));

        fakeBadge = new TextView(context);
        fakeBadge.setText("FAKE");
        fakeBadge.setTextColor(Color.WHITE);
        fakeBadge.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 9);
        fakeBadge.setTypeface(AndroidUtilities.bold());
        fakeBadge.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(1), AndroidUtilities.dp(4), AndroidUtilities.dp(1));
        fakeBadge.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(3), 0xFFD83C3C));
        fakeBadge.setVisibility(View.GONE);
        nameRow.addView(fakeBadge, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 6, 0, 0, 0));

        // Status Text
        statusText = new TextView(context);
        statusText.setText(LocaleController.getString("HudSearchVerifyStatusReady", R.string.HudSearchVerifyStatusReady));
        statusText.setTextColor(Theme.getColor(Theme.key_dialogTextHint, resourcesProvider));
        statusText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13.5f);
        textLayout.addView(statusText, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

        // Details Text
        detailsText = new TextView(context);
        detailsText.setTextColor(Theme.getColor(Theme.key_dialogTextHint, resourcesProvider));
        detailsText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f);
        detailsText.setVisibility(View.GONE);
        textLayout.addView(detailsText, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

        // 5. Search Button
        int accentColor = Theme.getColor(Theme.key_featuredStickers_addButton, resourcesProvider);
        int defaultButtonTextColor = Theme.getColor(Theme.key_featuredStickers_buttonText, resourcesProvider);
        int contrastButtonTextColor = ColorUtils.getContrastColor(accentColor, defaultButtonTextColor);

        verifyButton = new FrameLayout(context);
        verifyButton.setBackground(Theme.createSimpleSelectorRoundRectDrawable(
                AndroidUtilities.dp(10),
                accentColor,
                ColorUtils.blendARGB(accentColor, Color.BLACK, 0.15f)
        ));
        verifyButton.setOnClickListener(v -> performVerification());
        contentLayout.addView(verifyButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48, 0, 0, 0, 16));

        verifyButtonText = new TextView(context);
        verifyButtonText.setText(LocaleController.getString("HudSearchVerifyBtn", R.string.HudSearchVerifyBtn));
        verifyButtonText.setTextColor(contrastButtonTextColor);
        verifyButtonText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        verifyButtonText.setTypeface(AndroidUtilities.bold());
        verifyButtonText.setGravity(Gravity.CENTER);
        verifyButton.addView(verifyButtonText, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

        verifyProgressView = new RadialProgressView(context);
        verifyProgressView.setSize(AndroidUtilities.dp(20));
        verifyProgressView.setProgressColor(contrastButtonTextColor);
        verifyProgressView.setVisibility(View.GONE);
        verifyButton.addView(verifyProgressView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

        // 6. Action Buttons (Open Chat, Voice Call)
        LinearLayout actionLayout = new LinearLayout(context);
        actionLayout.setOrientation(LinearLayout.HORIZONTAL);
        actionLayout.setWeightSum(2);
        contentLayout.addView(actionLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        // Chat Button
        chatButton = new FrameLayout(context);
        chatButton.setBackground(Theme.createSimpleSelectorRoundRectDrawable(
                AndroidUtilities.dp(10),
                accentColor,
                ColorUtils.blendARGB(accentColor, Color.BLACK, 0.15f)
        ));
        chatButton.setOnClickListener(v -> openChat());
        actionLayout.addView(chatButton, LayoutHelper.createLinear(0, 48, 1.0f, 0, 0, 8, 0));

        LinearLayout chatInner = new LinearLayout(context);
        chatInner.setOrientation(LinearLayout.HORIZONTAL);
        chatInner.setGravity(Gravity.CENTER);
        chatButton.addView(chatInner, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        chatIcon = new ImageView(context);
        chatIcon.setImageResource(R.drawable.msg_discussion);
        chatInner.addView(chatIcon, LayoutHelper.createLinear(24, 24, Gravity.CENTER_VERTICAL, 0, 0, 8, 0));

        chatText = new TextView(context);
        chatText.setText(LocaleController.getString("HudMessageOpenChat", R.string.HudMessageOpenChat));
        chatText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        chatText.setTypeface(AndroidUtilities.bold());
        chatInner.addView(chatText, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));

        // Call Button (Outlined Accent Button)
        callButton = new FrameLayout(context);
        
        android.graphics.drawable.GradientDrawable normalDrawable = new android.graphics.drawable.GradientDrawable();
        normalDrawable.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        normalDrawable.setCornerRadius(AndroidUtilities.dp(10));
        normalDrawable.setColor(0x00000000);
        normalDrawable.setStroke(AndroidUtilities.dp(1.5f), accentColor);

        android.graphics.drawable.GradientDrawable pressedDrawable = new android.graphics.drawable.GradientDrawable();
        pressedDrawable.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        pressedDrawable.setCornerRadius(AndroidUtilities.dp(10));
        pressedDrawable.setColor(ColorUtils.blendARGB(accentColor, 0x00000000, 0.85f));
        pressedDrawable.setStroke(AndroidUtilities.dp(1.5f), accentColor);

        android.graphics.drawable.StateListDrawable callButtonBg = new android.graphics.drawable.StateListDrawable();
        callButtonBg.addState(new int[]{android.R.attr.state_pressed}, pressedDrawable);
        callButtonBg.addState(new int[]{}, normalDrawable);

        callButton.setBackground(callButtonBg);
        callButton.setOnClickListener(v -> startCall());
        actionLayout.addView(callButton, LayoutHelper.createLinear(0, 48, 1.0f, 8, 0, 0, 0));

        LinearLayout callInner = new LinearLayout(context);
        callInner.setOrientation(LinearLayout.HORIZONTAL);
        callInner.setGravity(Gravity.CENTER);
        callButton.addView(callInner, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        callIcon = new ImageView(context);
        callIcon.setImageResource(R.drawable.msg_calls);
        callInner.addView(callIcon, LayoutHelper.createLinear(24, 24, Gravity.CENTER_VERTICAL, 0, 0, 8, 0));

        callText = new TextView(context);
        callText.setText(LocaleController.getString("HudMessageVoiceCall", R.string.HudMessageVoiceCall));
        callText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        callText.setTypeface(AndroidUtilities.bold());
        callInner.addView(callText, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));

        // 7. Divider and Guidelines
        View divider = new View(context);
        divider.setBackgroundColor(Theme.getColor(Theme.key_divider, resourcesProvider));
        contentLayout.addView(divider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 0, 8, 0, 12));

        TextView guidelinesTitle = new TextView(context);
        guidelinesTitle.setText(LocaleController.getString("HudSearchGuidelinesTitle", R.string.HudSearchGuidelinesTitle));
        guidelinesTitle.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider));
        guidelinesTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        guidelinesTitle.setTypeface(AndroidUtilities.bold());
        contentLayout.addView(guidelinesTitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 4, 0, 4, 6));

        TextView guidelinesText = new TextView(context);
        StringBuilder sb = new StringBuilder();
        sb.append(LocaleController.getString("HudSearchGuidelineId", R.string.HudSearchGuidelineId)).append("\n");
        sb.append(LocaleController.getString("HudSearchGuidelinePhone", R.string.HudSearchGuidelinePhone)).append("\n");
        sb.append(LocaleController.getString("HudSearchGuidelineUsername", R.string.HudSearchGuidelineUsername));
        guidelinesText.setText(sb.toString());
        guidelinesText.setTextColor(Theme.getColor(Theme.key_dialogTextHint, resourcesProvider));
        guidelinesText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        guidelinesText.setLineSpacing(AndroidUtilities.dp(3), 1.0f);
        contentLayout.addView(guidelinesText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 4, 0, 4, 8));

        scrollView.addView(contentLayout);
        setCustomView(scrollView);

        // Load country codes list
        loadCountries();

        // Switch to default tab (User ID)
        switchTab(0);

        setActionsEnabled(false);

        setOnShowListener(dialog -> AndroidUtilities.runOnUIThread(() -> {
            idField.getEditText().requestFocus();
            AndroidUtilities.showKeyboard(idField.getEditText());
        }, 150));
    }

    private void switchTab(int tabIndex) {
        currentTab = tabIndex;
        resetVerification();

        Theme.ResourcesProvider rp = resourcesProvider;
        for (int i = 0; i < 3; i++) {
            boolean selected = (i == tabIndex);
            tabViews[i].setTextColor(Theme.getColor(selected ? Theme.key_featuredStickers_addButton : Theme.key_windowBackgroundWhiteGrayText, rp));
            if (selected) {
                tabViews[i].setBackground(Theme.createSimpleSelectorRoundRectDrawable(
                        AndroidUtilities.dp(6),
                        Theme.getColor(Theme.key_windowBackgroundWhite, rp),
                        Theme.getColor(Theme.key_listSelector, rp)
                ));
            } else {
                tabViews[i].setBackground(Theme.createRadSelectorDrawable(Theme.getColor(Theme.key_listSelector, rp), 6, 6));
            }
        }

        idContainer.setVisibility(tabIndex == 0 ? View.VISIBLE : View.GONE);
        phoneOutlineView.setVisibility(tabIndex == 1 ? View.VISIBLE : View.GONE);
        usernameContainer.setVisibility(tabIndex == 2 ? View.VISIBLE : View.GONE);

        AndroidUtilities.hideKeyboard(getWindow().getDecorView());
        if (tabIndex == 0) {
            idField.getEditText().requestFocus();
            AndroidUtilities.showKeyboard(idField.getEditText());
        } else if (tabIndex == 1) {
            phoneField.requestFocus();
            AndroidUtilities.showKeyboard(phoneField);
        } else {
            usernameField.getEditText().requestFocus();
            AndroidUtilities.showKeyboard(usernameField.getEditText());
        }
    }

    private void resetVerification() {
        resolvedUser = null;
        resolvedChat = null;
        resolvedPeerId = 0;
        avatarView.setVisibility(View.GONE);
        nameText.setText("");
        verifiedBadge.setVisibility(View.GONE);
        premiumBadge.setVisibility(View.GONE);
        scamBadge.setVisibility(View.GONE);
        fakeBadge.setVisibility(View.GONE);
        detailsText.setVisibility(View.GONE);
        statusText.setText(LocaleController.getString("HudSearchVerifyStatusReady", R.string.HudSearchVerifyStatusReady));
        statusText.setTextColor(Theme.getColor(Theme.key_dialogTextHint, resourcesProvider));
        setActionsEnabled(false);
        if (verifyButtonText != null) {
            verifyButtonText.setVisibility(View.VISIBLE);
        }
        if (verifyProgressView != null) {
            verifyProgressView.setVisibility(View.GONE);
        }
        if (verifyButton != null) {
            verifyButton.setEnabled(true);
        }
    }

    private void setActionsEnabled(boolean enabled) {
        chatButton.setEnabled(enabled);
        callButton.setEnabled(enabled);
        Theme.ResourcesProvider rp = resourcesProvider;
        if (enabled) {
            chatButton.setAlpha(1.0f);
            callButton.setAlpha(1.0f);

            int accentColor = Theme.getColor(Theme.key_featuredStickers_addButton, rp);
            int defaultButtonTextColor = Theme.getColor(Theme.key_featuredStickers_buttonText, rp);
            int contrastButtonTextColor = ColorUtils.getContrastColor(accentColor, defaultButtonTextColor);

            chatIcon.setColorFilter(new PorterDuffColorFilter(contrastButtonTextColor, PorterDuff.Mode.SRC_IN));
            chatText.setTextColor(contrastButtonTextColor);

            callIcon.setColorFilter(new PorterDuffColorFilter(accentColor, PorterDuff.Mode.SRC_IN));
            callText.setTextColor(accentColor);
        } else {
            chatButton.setAlpha(0.4f);
            callButton.setAlpha(0.4f);
            int disabledColor = Theme.getColor(Theme.key_dialogTextHint, rp);
            chatIcon.setColorFilter(new PorterDuffColorFilter(disabledColor, PorterDuff.Mode.SRC_IN));
            callIcon.setColorFilter(new PorterDuffColorFilter(disabledColor, PorterDuff.Mode.SRC_IN));
            chatText.setTextColor(disabledColor);
            callText.setTextColor(disabledColor);
        }
    }

    private void performVerification() {
        AndroidUtilities.hideKeyboard(getWindow().getDecorView());
        verifyButtonText.setVisibility(View.INVISIBLE);
        verifyProgressView.setVisibility(View.VISIBLE);
        verifyButton.setEnabled(false);
        statusText.setText(LocaleController.getString("HudMessageByNumberVerify", R.string.HudMessageByNumberVerify));
        statusText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider));

        if (currentTab == 0) { // User ID
            String query = idField.getEditText().getText().toString().trim();
            if (TextUtils.isEmpty(query)) {
                onVerificationFailed();
                return;
            }
            long id = 0;
            try {
                id = Long.parseLong(query);
            } catch (Exception ignored) {}

            if (id > 0) {
                TLRPC.User cachedUser = MessagesController.getInstance(currentAccount).getUser(id);
                if (cachedUser != null) {
                    resolvedPeerId = id;
                    onVerificationSuccess();
                    return;
                }
                TLRPC.Chat cachedChat = MessagesController.getInstance(currentAccount).getChat(id);
                if (cachedChat != null) {
                    resolvedPeerId = -id;
                    onVerificationSuccess();
                    return;
                }
            }
            onVerificationFailed();
        } else if (currentTab == 1) { // Phone Number
            String code = codeField.getText().toString().replaceAll("[^\\d]", "");
            String number = phoneField.getText().toString().replaceAll("[^\\d]", "");
            if (TextUtils.isEmpty(code) || TextUtils.isEmpty(number)) {
                onVerificationFailed();
                return;
            }
            String fullPhone = "+" + code + number;
            MessagesController.getInstance(currentAccount).getUserNameResolver().resolve(fullPhone, resolvedId -> {
                AndroidUtilities.runOnUIThread(() -> {
                    if (resolvedId != null && resolvedId != 0) {
                        resolvedPeerId = resolvedId;
                        onVerificationSuccess();
                    } else {
                        onVerificationFailed();
                    }
                });
            });
        } else { // Username
            String username = usernameField.getEditText().getText().toString().trim();
            if (TextUtils.isEmpty(username)) {
                onVerificationFailed();
                return;
            }
            if (username.startsWith("@")) {
                username = username.substring(1);
            }
            MessagesController.getInstance(currentAccount).getUserNameResolver().resolve(username, resolvedId -> {
                AndroidUtilities.runOnUIThread(() -> {
                    if (resolvedId != null && resolvedId != 0) {
                        resolvedPeerId = resolvedId;
                        onVerificationSuccess();
                    } else {
                        onVerificationFailed();
                    }
                });
            });
        }
    }

    private void onVerificationSuccess() {
        verifyButtonText.setVisibility(View.VISIBLE);
        verifyProgressView.setVisibility(View.GONE);
        verifyButton.setEnabled(true);
        avatarView.setVisibility(View.VISIBLE);
        detailsText.setVisibility(View.VISIBLE);
        setActionsEnabled(true);

        Theme.ResourcesProvider rp = resourcesProvider;
        nameText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, rp));

        boolean isVerified = false;
        boolean isPremium = false;
        boolean isScam = false;
        boolean isFake = false;
        String statusString = "";
        boolean isOnline = false;
        String detailsString = "";

        if (resolvedPeerId > 0) {
            resolvedUser = MessagesController.getInstance(currentAccount).getUser(resolvedPeerId);
            resolvedChat = null;
            if (resolvedUser != null) {
                avatarView.setForUserOrChat(resolvedUser, new AvatarDrawable(resolvedUser));
                nameText.setText(org.telegram.messenger.UserObject.getUserName(resolvedUser));
                isVerified = resolvedUser.verified;
                isPremium = resolvedUser.premium;
                isScam = resolvedUser.scam;
                isFake = resolvedUser.fake;

                if (resolvedUser.bot) {
                    statusString = LocaleController.getString("Bot", R.string.Bot);
                } else {
                    statusString = LocaleController.formatUserStatus(currentAccount, resolvedUser);
                    isOnline = (resolvedUser.id == org.telegram.messenger.UserConfig.getInstance(currentAccount).getClientUserId()
                                || (resolvedUser.status != null && resolvedUser.status.expires > org.telegram.tgnet.ConnectionsManager.getInstance(currentAccount).getCurrentTime())
                                || MessagesController.getInstance(currentAccount).onlinePrivacy.containsKey(resolvedUser.id));
                }

                if (!TextUtils.isEmpty(resolvedUser.username)) {
                    detailsString = "@" + resolvedUser.username + "  •  ID: " + resolvedUser.id;
                } else {
                    detailsString = "ID: " + resolvedUser.id;
                }
            } else {
                avatarView.setVisibility(View.GONE);
                nameText.setText("User ID: " + resolvedPeerId);
                detailsString = "ID: " + resolvedPeerId;
            }
        } else {
            resolvedChat = MessagesController.getInstance(currentAccount).getChat(-resolvedPeerId);
            resolvedUser = null;
            if (resolvedChat != null) {
                avatarView.setForUserOrChat(resolvedChat, new AvatarDrawable(resolvedChat));
                nameText.setText(resolvedChat.title);
                isVerified = resolvedChat.verified;
                isScam = resolvedChat.scam;
                isFake = resolvedChat.fake;

                if (resolvedChat.participants_count != 0) {
                    if (org.telegram.messenger.ChatObject.isChannelAndNotMegaGroup(resolvedChat)) {
                        statusString = LocaleController.formatPluralStringComma("Subscribers", resolvedChat.participants_count);
                    } else {
                        statusString = LocaleController.formatPluralStringComma("Members", resolvedChat.participants_count);
                    }
                } else {
                    statusString = org.telegram.messenger.ChatObject.isPublic(resolvedChat) ? "Public Group" : "Private Group";
                }

                if (!TextUtils.isEmpty(resolvedChat.username)) {
                    detailsString = "@" + resolvedChat.username + "  •  ID: " + (-resolvedPeerId);
                } else {
                    detailsString = "ID: " + (-resolvedPeerId);
                }
            } else {
                avatarView.setVisibility(View.GONE);
                nameText.setText("Chat ID: " + resolvedPeerId);
                detailsString = "ID: " + resolvedPeerId;
            }
        }

        verifiedBadge.setVisibility(isVerified ? View.VISIBLE : View.GONE);
        premiumBadge.setVisibility(isPremium ? View.VISIBLE : View.GONE);
        scamBadge.setVisibility(isScam ? View.VISIBLE : View.GONE);
        fakeBadge.setVisibility(isFake ? View.VISIBLE : View.GONE);

        statusText.setText(statusString);
        statusText.setTextColor(Theme.getColor(isOnline ? Theme.key_windowBackgroundWhiteBlueText : Theme.key_dialogTextHint, rp));
        detailsText.setText(detailsString);
    }

    private void onVerificationFailed() {
        verifyButtonText.setVisibility(View.VISIBLE);
        verifyProgressView.setVisibility(View.GONE);
        verifyButton.setEnabled(true);
        avatarView.setVisibility(View.GONE);
        nameText.setText("");
        verifiedBadge.setVisibility(View.GONE);
        premiumBadge.setVisibility(View.GONE);
        scamBadge.setVisibility(View.GONE);
        fakeBadge.setVisibility(View.GONE);
        detailsText.setVisibility(View.GONE);
        resolvedUser = null;
        resolvedChat = null;
        resolvedPeerId = 0;
        statusText.setText(LocaleController.getString("HudSearchVerifyStatusNotFound", R.string.HudSearchVerifyStatusNotFound));
        statusText.setTextColor(Theme.getColor(Theme.key_text_RedRegular, resourcesProvider));
        setActionsEnabled(false);
    }

    private void openChat() {
        if (resolvedPeerId != 0) {
            dismiss();
            Bundle args = new Bundle();
            if (resolvedPeerId > 0) {
                args.putLong("user_id", resolvedPeerId);
            } else {
                args.putLong("chat_id", -resolvedPeerId);
            }
            navigationLayout.presentFragment(new ChatActivity(args));
        }
    }

    private void startCall() {
        if (resolvedPeerId != 0) {
            dismiss();
            if (resolvedPeerId > 0) {
                if (resolvedUser != null) {
                    VoIPHelper.startCall(resolvedUser, false, false, activity, null, AccountInstance.getInstance(currentAccount));
                }
            } else {
                if (resolvedChat != null) {
                    VoIPHelper.startCall(resolvedChat, null, null, false, activity, null, AccountInstance.getInstance(currentAccount));
                }
            }
        }
    }

    // Helper functions for loading and selecting countries
    private void setCountryFlagAndHint(CountrySelectActivity.Country country) {
        String flag = LocaleController.getLanguageFlag(country.shortname);
        if (flag != null) {
            countryFlag.setText(Emoji.replaceEmoji(flag, countryFlag.getPaint().getFontMetricsInt(), false));
        } else {
            countryFlag.setText("🌐");
        }
        if (phoneFormatMap.get(country.code) != null && !phoneFormatMap.get(country.code).isEmpty()) {
            phoneField.setHint(phoneFormatMap.get(country.code).get(0));
        } else {
            phoneField.setHint(LocaleController.getString("HudMessageByNumberHint", R.string.HudMessageByNumberHint));
        }
    }

    private void selectCountry(CountrySelectActivity.Country country) {
        ignoreOnTextChange = true;
        String currentCode = codeField.getText().toString();
        if (!currentCode.equals(country.code)) {
            codeField.setText(country.code);
            codeField.setSelection(codeField.getText().length());
        }
        setCountryFlagAndHint(country);
        ignoreOnTextChange = false;
    }

    private void loadCountries() {
        HashMap<String, String> languageMap = new HashMap<>();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(ApplicationLoader.applicationContext.getResources().getAssets().open("countries.txt")));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] args = line.split(";");
                CountrySelectActivity.Country countryWithCode = new CountrySelectActivity.Country();
                countryWithCode.name = args[2];
                countryWithCode.code = args[0];
                countryWithCode.shortname = args[1];
                countriesArray.add(0, countryWithCode);

                List<CountrySelectActivity.Country> countryList = codesMap.get(args[0]);
                if (countryList == null) {
                    codesMap.put(args[0], countryList = new ArrayList<>());
                }
                countryList.add(countryWithCode);

                if (args.length > 3) {
                    phoneFormatMap.put(args[0], Collections.singletonList(args[3]));
                }
                languageMap.put(args[1], args[2]);
            }
            reader.close();
        } catch (Exception e) {
            FileLog.e(e);
        }

        Collections.sort(countriesArray, Comparator.comparing(o -> o.name));

        String country = null;
        try {
            TelephonyManager telephonyManager = (TelephonyManager) ApplicationLoader.applicationContext.getSystemService(Context.TELEPHONY_SERVICE);
            if (telephonyManager != null) {
                country = telephonyManager.getSimCountryIso().toUpperCase();
            }
        } catch (Exception e) {
            FileLog.e(e);
        }

        if (country != null) {
            String countryName = languageMap.get(country);
            if (countryName != null) {
                CountrySelectActivity.Country cSelected = null;
                for (CountrySelectActivity.Country c : countriesArray) {
                    if (Objects.equals(c.name, countryName)) {
                        cSelected = c;
                        break;
                    }
                }
                if (cSelected != null) {
                    selectCountry(cSelected);
                }
            }
        }
    }

    private static class ColorUtils {
        public static int blendARGB(int color1, int color2, float ratio) {
            final float inverseRatio = 1.0f - ratio;
            float a = (Color.alpha(color1) * inverseRatio) + (Color.alpha(color2) * ratio);
            float r = (Color.red(color1) * inverseRatio) + (Color.red(color2) * ratio);
            float g = (Color.green(color1) * inverseRatio) + (Color.green(color2) * ratio);
            float b = (Color.blue(color1) * inverseRatio) + (Color.blue(color2) * ratio);
            return Color.argb((int) a, (int) r, (int) g, (int) b);
        }

        public static int getContrastColor(int backgroundColor, int preferredTextColor) {
            double bgLuminance = getLuminance(backgroundColor);
            double textLuminance = getLuminance(preferredTextColor);
            double contrast = (Math.max(bgLuminance, textLuminance) + 0.05) / (Math.min(bgLuminance, textLuminance) + 0.05);
            if (contrast < 3.0) {
                return bgLuminance > 0.5 ? 0xff1c1c1e : 0xffffffff;
            }
            return preferredTextColor;
        }

        private static double getLuminance(int color) {
            double r = Color.red(color) / 255.0;
            double g = Color.green(color) / 255.0;
            double b = Color.blue(color) / 255.0;
            return 0.2126 * (r <= 0.03928 ? r / 12.92 : Math.pow((r + 0.055) / 1.055, 2.4))
                 + 0.7152 * (g <= 0.03928 ? g / 12.92 : Math.pow((g + 0.055) / 1.055, 2.4))
                 + 0.0722 * (b <= 0.03928 ? b / 12.92 : Math.pow((b + 0.055) / 1.055, 2.4));
        }
    }
}
