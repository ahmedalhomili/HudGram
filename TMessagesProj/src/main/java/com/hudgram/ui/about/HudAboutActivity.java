package com.hudgram.ui.about;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.LayoutHelper;

public class HudAboutActivity extends BaseFragment {

    private ScrollView scrollView;
    private LinearLayout scrollContent;
    
    private ImageView logoView;
    private TextView titleView;
    private TextView versionView;

    private int accentColor;
    private int primaryTextColor;
    private int secondaryTextColor;
    private int windowBgColor;
    private int cardBgColor;

    @Override
    public View createView(Context context) {
        // Configure Action Bar
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("AboutHudgram", R.string.AboutHudgram));
        actionBar.setActionBarMenuOnItemClick(new org.telegram.ui.ActionBar.ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);

        scrollContent = new LinearLayout(context);
        scrollContent.setOrientation(LinearLayout.VERTICAL);

        scrollView.addView(scrollContent, LayoutHelper.createScroll(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.NO_GRAVITY));

        updateColors();
        return scrollView;
    }

    private void updateColors() {
        accentColor = Theme.getColor(Theme.key_chats_actionBackground, resourceProvider);
        primaryTextColor = Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourceProvider);
        secondaryTextColor = Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2, resourceProvider);
        windowBgColor = Theme.getColor(Theme.key_windowBackgroundGray, resourceProvider);
        cardBgColor = Theme.getColor(Theme.key_windowBackgroundWhite, resourceProvider);

        scrollView.setBackgroundColor(windowBgColor);
        
        // Action Bar colors
        actionBar.setBackgroundColor(cardBgColor);
        actionBar.setTitleColor(primaryTextColor);
        actionBar.setItemsColor(primaryTextColor, false);

        rebuildContent();
    }

    private void rebuildContent() {
        Context context = getParentActivity();
        if (context == null) return;

        scrollContent.removeAllViews();
        boolean isRtl = LocaleController.isRTL;

        // 1. Header Section Container (White flat background, standard for settings headers)
        LinearLayout headerContainer = new LinearLayout(context);
        headerContainer.setOrientation(LinearLayout.VERTICAL);
        headerContainer.setGravity(Gravity.CENTER_HORIZONTAL);
        headerContainer.setBackgroundColor(cardBgColor);
        headerContainer.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(32), AndroidUtilities.dp(24), AndroidUtilities.dp(24));

        logoView = new ImageView(context);
        logoView.setImageResource(R.drawable.logo_hudgram_wthiout_bg);
        logoView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        logoView.setColorFilter(new android.graphics.PorterDuffColorFilter(accentColor, android.graphics.PorterDuff.Mode.SRC_IN));
        headerContainer.addView(logoView, LayoutHelper.createLinear(72, 72, Gravity.CENTER_HORIZONTAL));

        titleView = new TextView(context);
        titleView.setText(LocaleController.getString("AppName", R.string.AppName));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 22);
        titleView.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        titleView.setTextColor(primaryTextColor);
        titleView.setGravity(Gravity.CENTER);
        headerContainer.addView(titleView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 12, 0, 0));

        String versionName = "1.0.0";
        String packageName = "com.hudgram.messenger";
        try {
            android.content.pm.PackageInfo pInfo = ApplicationLoader.applicationContext.getPackageManager().getPackageInfo(ApplicationLoader.applicationContext.getPackageName(), 0);
            versionName = pInfo.versionName;
            packageName = pInfo.packageName;
        } catch (Exception e) {
            org.telegram.messenger.FileLog.e(e);
        }
        versionView = new TextView(context);
        versionView.setText(LocaleController.formatString("AboutHudgramVersion", R.string.AboutHudgramVersion, versionName, packageName));
        versionView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        versionView.setTextColor(secondaryTextColor);
        versionView.setGravity(Gravity.CENTER);
        headerContainer.addView(versionView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 4, 0, 0));

        scrollContent.addView(headerContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // 2. Intro descriptive text below Header (Standard TextInfoPrivacyCell style)
        TextInfoPrivacyCell introCell = new TextInfoPrivacyCell(context, resourceProvider);
        introCell.setText(LocaleController.getString("AboutHudgramIntro", R.string.AboutHudgramIntro));
        introCell.setTextGravity(isRtl ? Gravity.RIGHT : Gravity.LEFT);
        scrollContent.addView(introCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // Background card layout parameters (16dp left/right margin, 8dp top/bottom margin)
        LinearLayout.LayoutParams cardLp = LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT);
        cardLp.leftMargin = AndroidUtilities.dp(16);
        cardLp.rightMargin = AndroidUtilities.dp(16);
        cardLp.topMargin = AndroidUtilities.dp(8);
        cardLp.bottomMargin = AndroidUtilities.dp(8);

        // Card corner drawable
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setCornerRadius(AndroidUtilities.dp(12));
        cardBg.setColor(cardBgColor);

        // 3. Section 1: Features List rounded card (Modern UX)
        LinearLayout featuresCard = new LinearLayout(context);
        featuresCard.setOrientation(LinearLayout.VERTICAL);
        featuresCard.setBackground(cardBg);
        featuresCard.setClipToOutline(true);

        HeaderCell featuresHeader = new HeaderCell(context, resourceProvider);
        featuresHeader.setText(LocaleController.getString("AboutHudgramWhy", R.string.AboutHudgramWhy));
        featuresCard.addView(featuresHeader, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextDetailSettingsCell f1 = new TextDetailSettingsCell(context);
        f1.setMultilineDetail(true);
        f1.setTextAndValueAndIcon(LocaleController.getString("AboutHudgramFeature1Title", R.string.AboutHudgramFeature1Title), 
                LocaleController.getString("AboutHudgramFeature1Desc", R.string.AboutHudgramFeature1Desc), 
                R.drawable.msg_customize, true);
        featuresCard.addView(f1, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextDetailSettingsCell f2 = new TextDetailSettingsCell(context);
        f2.setMultilineDetail(true);
        f2.setTextAndValueAndIcon(LocaleController.getString("AboutHudgramFeature2Title", R.string.AboutHudgramFeature2Title), 
                LocaleController.getString("AboutHudgramFeature2Desc", R.string.AboutHudgramFeature2Desc), 
                R.drawable.msg_permissions, true);
        featuresCard.addView(f2, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextDetailSettingsCell f3 = new TextDetailSettingsCell(context);
        f3.setMultilineDetail(true);
        f3.setTextAndValueAndIcon(LocaleController.getString("AboutHudgramFeature3Title", R.string.AboutHudgramFeature3Title), 
                LocaleController.getString("AboutHudgramFeature3Desc", R.string.AboutHudgramFeature3Desc), 
                R.drawable.msg_speed, true);
        featuresCard.addView(f3, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextDetailSettingsCell f4 = new TextDetailSettingsCell(context);
        f4.setMultilineDetail(true);
        f4.setTextAndValueAndIcon(LocaleController.getString("AboutHudgramFeature4Title", R.string.AboutHudgramFeature4Title), 
                LocaleController.getString("AboutHudgramFeature4Desc", R.string.AboutHudgramFeature4Desc), 
                R.drawable.msg_settings, false);
        featuresCard.addView(f4, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        scrollContent.addView(featuresCard, cardLp);

        // 4. Section 2: Links & Resources rounded card (Modern UX)
        GradientDrawable cardBg2 = new GradientDrawable();
        cardBg2.setCornerRadius(AndroidUtilities.dp(12));
        cardBg2.setColor(cardBgColor);

        LinearLayout linksCard = new LinearLayout(context);
        linksCard.setOrientation(LinearLayout.VERTICAL);
        linksCard.setBackground(cardBg2);
        linksCard.setClipToOutline(true);

        HeaderCell linksHeader = new HeaderCell(context, resourceProvider);
        linksHeader.setText(LocaleController.getString("AboutHudgramLinks", R.string.AboutHudgramLinks));
        linksCard.addView(linksHeader, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextSettingsCell l1 = new TextSettingsCell(context, resourceProvider);
        l1.setTextAndIcon(LocaleController.getString("HudOfficialChannel", R.string.HudOfficialChannel), R.drawable.msg_channel, true);
        l1.setOnClickListener(v -> Browser.openUrl(getParentActivity(), "https://t.me/hudgramchannel"));
        linksCard.addView(l1, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextSettingsCell l2 = new TextSettingsCell(context, resourceProvider);
        l2.setTextAndIcon(LocaleController.getString("AboutHudgramSupport", R.string.AboutHudgramSupport), R.drawable.msg_help, true);
        l2.setOnClickListener(v -> Browser.openUrl(getParentActivity(), "https://t.me/hudgramsupport"));
        linksCard.addView(l2, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextSettingsCell l3 = new TextSettingsCell(context, resourceProvider);
        l3.setTextAndIcon(LocaleController.getString("AboutHudgramSource", R.string.AboutHudgramSource), R.drawable.msg_link, true);
        l3.setOnClickListener(v -> Browser.openUrl(getParentActivity(), "https://github.com/ahmedalhomili/Telegram"));
        linksCard.addView(l3, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextSettingsCell l4 = new TextSettingsCell(context, resourceProvider);
        l4.setTextAndIcon(LocaleController.getString("AboutHudgramWebsite", R.string.AboutHudgramWebsite), R.drawable.msg_link2, false);
        l4.setOnClickListener(v -> Browser.openUrl(getParentActivity(), "https://hudgram.com"));
        linksCard.addView(l4, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        scrollContent.addView(linksCard, cardLp);

        // 5. Footer / Slogan
        TextInfoPrivacyCell sloganCell = new TextInfoPrivacyCell(context, resourceProvider);
        sloganCell.setText(LocaleController.getString("AboutHudgramSlogan", R.string.AboutHudgramSlogan));
        sloganCell.setTextGravity(Gravity.CENTER);
        scrollContent.addView(sloganCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }
}
