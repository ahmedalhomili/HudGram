package com.hudgram.ui.views;
import com.hudgram.ui.settings.HudGeneralSettingsActivity;

import static org.telegram.ui.Components.Premium.LimitReachedBottomSheet.TYPE_ACCOUNTS;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.graphics.Outline;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.MediaActivity;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.MainTabsActivity;
import org.telegram.ui.ProfileActivity;

import com.hudgram.ui.utils.ContactSearchUiHelper;

public class HudSideMenuView extends FrameLayout
        implements org.telegram.messenger.NotificationCenter.NotificationCenterDelegate {

    private BackupImageView avatarView;
    private TextView        nameTextView;
    private TextView        phoneTextView;
    private LinearLayout    itemsContainer;
    private FrameLayout     headerCard;
    private View            headerBgLayer;
    private ScrollView      scrollView;
    private FrameLayout     footerLayout;
    private TextView        footerVersionText;
    private View            footerDivider;
    private ImageView       arrowView;
    private LinearLayout    accountsContainer;
    private FrameLayout     drawerContent;

    private boolean isAccountsExpanded = false;
    private long    currentUserId      = 0;
    private View[]  menuViews;

    private final org.telegram.ui.Stories.StoriesUtilities.AvatarStoryParams storyParams =
            new org.telegram.ui.Stories.StoriesUtilities.AvatarStoryParams(false) {
                @Override
                public void openStory(long dialogId, Runnable onDone) {
                    LaunchActivity.instance.close3DDrawer();
                    org.telegram.ui.ActionBar.BaseFragment last = LaunchActivity.getLastFragment();
                    if (last != null) {
                        last.getOrCreateStoryViewer().doOnAnimationReady(onDone);
                        last.getOrCreateStoryViewer().open(getContext(), dialogId, null);
                    } else if (onDone != null) onDone.run();
                }
            };

    private static final int STAGGER_MS = 35;

    // ─── Icon resources only — no hardcoded tint colors ───────
    private static final int[] ICON_RES = {
            R.drawable.msg_discussion,
            R.drawable.msg_contacts,
            R.drawable.msg_saved,
            R.drawable.msg_stories_saved,
            R.drawable.msg_customize,
            R.drawable.msg_palette,
            R.drawable.msg_info,
    };

    public HudSideMenuView(Context context) {
        super(context);
        buildUI(context);
    }

    private void buildUI(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= 17) {
            setLayoutDirection(isRTL() ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        }
        setBackgroundColor(Theme.getColor(Theme.key_chats_menuBackground));

        setPadding(0, 0, 0, 0);

        drawerContent = new FrameLayout(context);
        int edgeGap = AndroidUtilities.dp(36);
        drawerContent.setPadding(isRTL() ? edgeGap : 0, 0, isRTL() ? 0 : edgeGap, 0);

        FrameLayout.LayoutParams lp = LayoutHelper.createFrame(280, LayoutHelper.MATCH_PARENT, (isRTL() ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP);
        addView(drawerContent, lp);

        buildHeader(context);
        buildScrollArea(context);
        buildFooter(context);

        updateUserProfile();
        updateThemeColors();
    }

    // ──────────────────────────────────────────────────────────
    //  HEADER
    // ──────────────────────────────────────────────────────────
    private void buildHeader(Context context) {
        headerCard = new FrameLayout(context);
        headerCard.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View v, Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), AndroidUtilities.dp(24));
            }
        });
        headerCard.setClipToOutline(true);
        drawerContent.addView(headerCard, LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT, 88, Gravity.TOP,
                12, 36, 12, 0));

        // ── Gradient background ──────────────────────────────
        headerBgLayer = new View(context) {
            private final Paint gPaint = new Paint();
            private final Paint cPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override
            protected void onDraw(Canvas canvas) {
                int w = getWidth(), h = getHeight();
                // Use Telegram theme colors for gradient
                int base = Theme.getColor(Theme.key_chats_menuTopBackground);
                if ((base >>> 24) < 10) base = Theme.getColor(Theme.key_chats_menuBackground);
                int accentColor = Theme.getColor(Theme.key_actionBarDefault);
                int s = ColorUtils.blendARGB(base, accentColor, 0.60f);
                int e = ColorUtils.blendARGB(base, ColorUtils.blendARGB(accentColor, Color.BLACK, 0.3f), 0.75f);
                gPaint.setShader(new LinearGradient(0, 0, w, h, s, e, Shader.TileMode.CLAMP));
                canvas.drawRect(0, 0, w, h, gPaint);
                
                double lum = ColorUtils.calculateLuminance(s);
                cPaint.setStyle(Paint.Style.FILL);
                cPaint.setColor(ColorUtils.setAlphaComponent(lum > 0.6 ? accentColor : Color.WHITE, lum > 0.6 ? 12 : 18));
                canvas.drawCircle(w * 0.88f, -AndroidUtilities.dp(12), AndroidUtilities.dp(56), cPaint);
                cPaint.setColor(ColorUtils.setAlphaComponent(lum > 0.6 ? accentColor : Color.WHITE, lum > 0.6 ? 6 : 10));
                canvas.drawCircle(-AndroidUtilities.dp(8), h + AndroidUtilities.dp(8), AndroidUtilities.dp(44), cPaint);
            }
        };
        headerBgLayer.setWillNotDraw(false);
        headerCard.addView(headerBgLayer, LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        // ── Avatar ───────────────────────────────────────────
        FrameLayout avatarWrap = new FrameLayout(context) {
            private final Paint ringP = new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override
            protected void dispatchDraw(Canvas canvas) {
                ringP.setStyle(Paint.Style.STROKE);
                ringP.setStrokeWidth(AndroidUtilities.dp(2f));
                
                int base = Theme.getColor(Theme.key_chats_menuTopBackground);
                if ((base >>> 24) < 10) base = Theme.getColor(Theme.key_chats_menuBackground);
                int accentColor = Theme.getColor(Theme.key_actionBarDefault);
                int s = ColorUtils.blendARGB(base, accentColor, 0.60f);
                double lum = ColorUtils.calculateLuminance(s);
                
                ringP.setColor(lum > 0.6 ? ColorUtils.setAlphaComponent(accentColor, 120) : ColorUtils.setAlphaComponent(Color.WHITE, 90));
                
                float r = getMeasuredWidth() / 2f;
                canvas.drawCircle(r, r, r - AndroidUtilities.dp(1.2f), ringP);
                super.dispatchDraw(canvas);
            }
        };
        avatarWrap.setOnClickListener(v -> {
            LaunchActivity.instance.close3DDrawer();
            TLRPC.User u = UserConfig.getInstance(UserConfig.selectedAccount).getCurrentUser();
            if (u != null) {
                Bundle a = new Bundle();
                a.putLong("user_id", u.id);
                presentFragment(new ProfileActivity(a));
            }
        });
        headerCard.addView(avatarWrap, LayoutHelper.createFrame(
                58, 58,
                (isRTL() ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL,
                isRTL() ? 0 : 16, 0, isRTL() ? 16 : 0, 0));

        avatarView = new BackupImageView(context) {
            @Override
            protected void onDraw(Canvas canvas) {
                if (currentUserId != 0) {
                    boolean hasSelf = org.telegram.messenger.MessagesController
                            .getInstance(UserConfig.selectedAccount)
                            .getStoriesController().hasSelfStories();
                    storyParams.originalAvatarRect.set(0, 0, getMeasuredWidth(), getMeasuredHeight());
                    org.telegram.ui.Stories.StoriesUtilities.drawAvatarWithStory(
                            currentUserId, canvas, imageReceiver, hasSelf, storyParams);
                } else super.onDraw(canvas);
            }
            @Override
            public boolean onTouchEvent(MotionEvent e) {
                return storyParams.checkOnTouchEvent(e, this) || super.onTouchEvent(e);
            }
        };
        avatarView.setRoundRadius(AndroidUtilities.dp(29));
        avatarWrap.addView(avatarView, LayoutHelper.createFrame(58, 58, Gravity.CENTER));

        // ── Name + Phone ──────────────────────────────────────
        LinearLayout infoCol = new LinearLayout(context);
        infoCol.setOrientation(LinearLayout.VERTICAL);
        infoCol.setGravity(Gravity.CENTER_VERTICAL);

        nameTextView = new TextView(context);
        nameTextView.setTextColor(Color.WHITE);
        nameTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15.5f);
        nameTextView.setTypeface(AndroidUtilities.bold());
        nameTextView.setSingleLine(true);
        nameTextView.setEllipsize(TextUtils.TruncateAt.END);
        nameTextView.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        if (android.os.Build.VERSION.SDK_INT >= 17) {
            nameTextView.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        }
        nameTextView.setShadowLayer(AndroidUtilities.dp(1f), 0, AndroidUtilities.dp(1f),
                ColorUtils.setAlphaComponent(Color.BLACK, 55));
        infoCol.addView(nameTextView, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        phoneTextView = new TextView(context);
        phoneTextView.setTextColor(ColorUtils.setAlphaComponent(Color.WHITE, 185));
        phoneTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f);
        phoneTextView.setSingleLine(true);
        phoneTextView.setEllipsize(TextUtils.TruncateAt.END);
        phoneTextView.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        if (android.os.Build.VERSION.SDK_INT >= 17) {
            phoneTextView.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        }
        LinearLayout.LayoutParams phoneLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        phoneLp.topMargin = AndroidUtilities.dp(2);
        infoCol.addView(phoneTextView, phoneLp);

        // infoCol: margins calculated so it fits beautifully between avatar and arrow icon
        int infoStart = 80;
        int infoEnd   = 64;
        int infoLeft  = isRTL() ? infoEnd : infoStart;
        int infoRight = isRTL() ? infoStart : infoEnd;
        headerCard.addView(infoCol, LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT,
                Gravity.CENTER_VERTICAL,
                infoLeft, 0, infoRight, 0));

        // ── Arrow — positioned at the edge since the drawer card is now bounded and not cut off ──
        arrowView = new ImageView(context);
        arrowView.setImageResource(R.drawable.msg_expand);
        arrowView.setColorFilter(new PorterDuffColorFilter(
                ColorUtils.setAlphaComponent(Color.WHITE, 200), PorterDuff.Mode.SRC_IN));
        arrowView.setScaleType(ImageView.ScaleType.CENTER);
        arrowView.setBackground(makeRippleCircle(0x28ffffff));
        arrowView.setOnClickListener(v -> toggleAccounts());
        headerCard.addView(arrowView, LayoutHelper.createFrame(
                40, 40,
                (isRTL() ? Gravity.LEFT : Gravity.RIGHT) | Gravity.CENTER_VERTICAL,
                isRTL() ? 12 : 0, 0, isRTL() ? 0 : 12, 0));

        headerCard.setOnClickListener(v -> toggleAccounts());
    }

    // ──────────────────────────────────────────────────────────
    //  SCROLL AREA
    // ──────────────────────────────────────────────────────────
    private void buildScrollArea(Context context) {
        scrollView = new ScrollView(context);
        scrollView.setVerticalScrollBarEnabled(false);
        scrollView.setFillViewport(true);
        scrollView.setOverScrollMode(OVER_SCROLL_NEVER);
        drawerContent.addView(scrollView, LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT,
                Gravity.TOP, 0, 136, 0, 56));

        LinearLayout scrollContent = new LinearLayout(context);
        scrollContent.setOrientation(LinearLayout.VERTICAL);
        android.animation.LayoutTransition lt = new android.animation.LayoutTransition();
        lt.enableTransitionType(android.animation.LayoutTransition.CHANGING);
        lt.setDuration(260);
        scrollContent.setLayoutTransition(lt);
        scrollView.addView(scrollContent, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        accountsContainer = new LinearLayout(context);
        accountsContainer.setOrientation(LinearLayout.VERTICAL);
        accountsContainer.setVisibility(View.GONE);
        scrollContent.addView(accountsContainer, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        itemsContainer = new LinearLayout(context);
        itemsContainer.setOrientation(LinearLayout.VERTICAL);
        itemsContainer.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(12));
        scrollContent.addView(itemsContainer, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        buildMenuItems(context);
    }

    // ──────────────────────────────────────────────────────────
    //  MENU ITEMS
    // ──────────────────────────────────────────────────────────
    private void buildMenuItems(Context context) {
        menuViews = new View[8];

        menuViews[0] = makeItem(R.drawable.msg_discussion,
                LocaleController.getString("Chats", R.string.Chats),
                () -> { close(); tabTo(0); });

        menuViews[1] = makeItem(R.drawable.msg_contacts,
                LocaleController.getString("Contacts", R.string.Contacts),
                () -> { close(); tabTo(3); });

        addDivider();

        menuViews[2] = makeItem(R.drawable.msg_saved,
                LocaleController.getString("SavedMessages", R.string.SavedMessages),
                () -> {
                    close();
                    Bundle a = new Bundle();
                    a.putLong("user_id", UserConfig.getInstance(
                            UserConfig.selectedAccount).getClientUserId());
                    presentFragment(new org.telegram.ui.ChatActivity(a));
                });

        menuViews[3] = makeItem(R.drawable.msg_stories_saved,
                LocaleController.getString("SavedStories", R.string.SavedStories),
                () -> {
                    close();
                    Bundle a = new Bundle();
                    a.putLong("dialog_id", UserConfig.getInstance(
                            UserConfig.selectedAccount).getClientUserId());
                    a.putInt("type", MediaActivity.TYPE_STORIES);
                    presentFragment(new MediaActivity(a, null));
                });

        addDivider();

        menuViews[4] = makeItem(R.drawable.msg_customize,
                LocaleController.getString("HudgramSettings", R.string.HudgramSettings),
                () -> { close(); presentFragment(new HudGeneralSettingsActivity()); });

        menuViews[5] = makeItem(R.drawable.msg_palette,
                LocaleController.getString("ThemesAndAppearance", R.string.ThemesAndAppearance),
                () -> { close(); presentFragment(new org.telegram.ui.ThemeActivity(
                        org.telegram.ui.ThemeActivity.THEME_TYPE_OTHER)); });

        addDivider();

        menuViews[6] = makeItem(R.drawable.msg_contacts,
                LocaleController.getString("HudMessageByNumber", R.string.HudMessageByNumber),
                () -> {
                    close();
                    ContactSearchUiHelper.showSearchDialog(
                        LaunchActivity.instance,
                        UserConfig.selectedAccount,
                        null,
                        LaunchActivity.instance.actionBarLayout
                    );
                });

        menuViews[7] = makeItem(R.drawable.msg_info,
                LocaleController.getString("AboutHudgram", R.string.AboutHudgram),
                () -> {
                    close();
                    presentFragment(new com.hudgram.ui.about.HudAboutActivity());
                });
    }

    // ──────────────────────────────────────────────────────────
    //  SINGLE MENU ITEM — NO square box behind icon
    // ──────────────────────────────────────────────────────────
    private View makeItem(int iconRes, String label, Runnable action) {
        return makeItem(iconRes, label, action, true);
    }

    private View makeItem(int iconRes, String label,
                          Runnable action, boolean addToItems) {
        Context ctx = getContext();

        // Use Telegram theme icon color
        int iconTint = Theme.getColor(Theme.key_chats_menuItemIcon);
        int rippleColor = Theme.getColor(Theme.key_listSelector);

        FrameLayout wrapper = new FrameLayout(ctx);

        FrameLayout row = new FrameLayout(ctx);
        row.setBackground(makeRoundRipple(
                AndroidUtilities.dp(14),
                Color.TRANSPARENT,
                rippleColor));
        row.setOnTouchListener((v, e) -> {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:   scaleSpring(v, 0.97f, 80);  break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: scaleSpring(v, 1.00f, 220); break;
            }
            return false;
        });
        row.setOnClickListener(v -> { if (action != null) action.run(); });

        // Fix #2: Icon WITHOUT any square background box
        ImageView icon = new ImageView(ctx);
        icon.setImageResource(iconRes);
        icon.setScaleType(ImageView.ScaleType.CENTER);
        icon.setColorFilter(new PorterDuffColorFilter(iconTint, PorterDuff.Mode.SRC_IN));

        int iconMarginStart = isRTL() ? 0 : 14;
        int iconMarginEnd   = isRTL() ? 14 : 0;
        row.addView(icon, LayoutHelper.createFrame(
                28, 28,
                (isRTL() ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL,
                iconMarginStart, 0, iconMarginEnd, 0));

        // Fix #3: Label — fixed position regardless of language direction
        // Use a consistent margin that works for both Arabic and English
        TextView tv = new TextView(ctx);
        tv.setText(label);
        tv.setTextColor(Theme.getColor(Theme.key_chats_menuItemText));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15.5f);
        tv.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        tv.setSingleLine(true);
        tv.setEllipsize(TextUtils.TruncateAt.END);
        // Fix: use same gravity/padding approach for both languages — no displacement
        tv.setGravity(Gravity.CENTER_VERTICAL);
        // Fixed start margin = icon(28) + iconMargin(14) + gap(12) = 54dp
        // This is the same calculation regardless of language
        int tvStart = isRTL() ? 14 : 54;
        int tvEnd   = isRTL() ? 54 : 14;
        row.addView(tv, LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT,
                Gravity.CENTER_VERTICAL,
                tvStart, 0, tvEnd, 0));

        wrapper.addView(row, LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT, 52,
                Gravity.CENTER, 10, 2, 10, 2));

        if (addToItems) {
            itemsContainer.addView(wrapper, LayoutHelper.createLinear(
                    LayoutHelper.MATCH_PARENT, 56));
        }
        return wrapper;
    }

    private void addDivider() {
        View d = new View(getContext());
        d.setBackgroundColor(ColorUtils.setAlphaComponent(
                Theme.getColor(Theme.key_divider), 120));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        lp.topMargin    = AndroidUtilities.dp(4);
        lp.bottomMargin = AndroidUtilities.dp(4);
        lp.leftMargin   = AndroidUtilities.dp(isRTL() ? 16 : 54);
        lp.rightMargin  = AndroidUtilities.dp(isRTL() ? 54 : 16);
        itemsContainer.addView(d, lp);
    }

    // ──────────────────────────────────────────────────────────
    //  FOOTER
    // ──────────────────────────────────────────────────────────
    private void buildFooter(Context context) {
        footerLayout = new FrameLayout(context);
        footerLayout.setPadding(
                AndroidUtilities.dp(20), 0,
                AndroidUtilities.dp(20), 0);
        drawerContent.addView(footerLayout, LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT, 56, Gravity.BOTTOM));

        footerDivider = new View(context);
        footerDivider.setBackgroundColor(Theme.getColor(Theme.key_divider));
        drawerContent.addView(footerDivider, LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT, 1, Gravity.BOTTOM, 0, 0, 0, 56));

        LinearLayout footRow = new LinearLayout(context);
        footRow.setOrientation(LinearLayout.HORIZONTAL);
        footRow.setGravity(Gravity.CENTER_VERTICAL);

        View dot = new View(context) {
            private final Paint dp2 = new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override protected void onDraw(Canvas canvas) {
                // Use Telegram accent color for dot
                dp2.setColor(Theme.getColor(Theme.key_actionBarDefault));
                canvas.drawCircle(getWidth()/2f, getHeight()/2f, getWidth()/2f, dp2);
            }
        };
        dot.setWillNotDraw(false);
        footRow.addView(dot, new LinearLayout.LayoutParams(
                AndroidUtilities.dp(7), AndroidUtilities.dp(7)));

        footerVersionText = new TextView(context);
        footerVersionText.setText("Hudgram  v26.6");
        footerVersionText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f);
        footerVersionText.setAlpha(0.5f);
        LinearLayout.LayoutParams tvLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        tvLp.leftMargin = AndroidUtilities.dp(8);
        footRow.addView(footerVersionText, tvLp);

        footerLayout.addView(footRow, LayoutHelper.createFrame(
                LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT,
                (isRTL() ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL));
    }

    // ══════════════════════════════════════════════════════════
    //  ACCOUNTS ACCORDION
    // ══════════════════════════════════════════════════════════
    private void toggleAccounts() {
        isAccountsExpanded = !isAccountsExpanded;

        arrowView.animate()
                .rotation(isAccountsExpanded ? 180f : 0f)
                .setDuration(320)
                .setInterpolator(new android.view.animation.OvershootInterpolator(1.4f))
                .start();

        headerCard.animate()
                .scaleX(0.975f).scaleY(0.975f).setDuration(75)
                .withEndAction(() ->
                        headerCard.animate()
                                .scaleX(1f).scaleY(1f).setDuration(200)
                                .setInterpolator(new org.telegram.ui.Components.CubicBezierInterpolator(
                                        0.25f, 1f, 0.5f, 1f))
                                .start())
                .start();

        if (isAccountsExpanded) {
            populateAccounts();
            accountsContainer.setVisibility(View.VISIBLE);
            accountsContainer.setAlpha(0f);
            accountsContainer.setScaleY(0.92f);
            accountsContainer.setPivotY(0f);
            accountsContainer.setTranslationY(-AndroidUtilities.dp(6));
            accountsContainer.animate()
                    .alpha(1f).scaleY(1f).translationY(0)
                    .setDuration(260)
                    .setInterpolator(new org.telegram.ui.Components.CubicBezierInterpolator(
                            0.25f, 1f, 0.5f, 1f))
                    .start();
        } else {
            accountsContainer.animate()
                    .alpha(0f).scaleY(0.92f).translationY(-AndroidUtilities.dp(6))
                    .setDuration(180)
                    .setInterpolator(new android.view.animation.AccelerateInterpolator(1.5f))
                    .withEndAction(() -> {
                        accountsContainer.setVisibility(View.GONE);
                        accountsContainer.setAlpha(1f);
                        accountsContainer.setScaleY(1f);
                        accountsContainer.setTranslationY(0);
                    }).start();
        }
    }

    public void populateAccounts() {
        if (accountsContainer == null) return;
        accountsContainer.removeAllViews();

        Context ctx = getContext();
        int count = UserConfig.getActivatedAccountsCount();

        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);

        GradientDrawable cardBg = new GradientDrawable();
        int surfaceColor = Theme.getColor(Theme.key_chats_menuBackground);
        double lum = ColorUtils.calculateLuminance(surfaceColor);
        int elevatedSurface = lum > 0.5
                ? ColorUtils.blendARGB(surfaceColor, Color.BLACK, 0.05f)
                : ColorUtils.blendARGB(surfaceColor, Color.WHITE, 0.07f);
        cardBg.setColor(elevatedSurface);
        cardBg.setCornerRadius(AndroidUtilities.dp(20));

        card.setBackground(cardBg);
        card.setPadding(0, AndroidUtilities.dp(6), 0, AndroidUtilities.dp(6));

        boolean hasAny = false;

        for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
            if (!UserConfig.getInstance(i).isClientActivated()) continue;
            if (i == UserConfig.selectedAccount) continue;

            final int accId = i;
            TLRPC.User u = UserConfig.getInstance(accId).getCurrentUser();
            if (u == null)
                u = org.telegram.messenger.MessagesController.getInstance(accId)
                        .getUser(UserConfig.getInstance(accId).getClientUserId());
            if (u == null) continue;
            final TLRPC.User fu = u;
            hasAny = true;

            FrameLayout row = new FrameLayout(ctx);
            GradientDrawable rowBg = new GradientDrawable();
            rowBg.setColor(Color.TRANSPARENT);
            rowBg.setCornerRadius(AndroidUtilities.dp(12));
            GradientDrawable rowMask = new GradientDrawable();
            rowMask.setColor(0xffffffff);
            rowMask.setCornerRadius(AndroidUtilities.dp(12));
            row.setBackground(new RippleDrawable(
                    ColorStateList.valueOf(Theme.getColor(Theme.key_listSelector)),
                    rowBg, rowMask));
            row.setOnTouchListener((v, e) -> {
                if (e.getAction() == MotionEvent.ACTION_DOWN)   scaleSpring(v, 0.97f, 80);
                else if (e.getAction() == MotionEvent.ACTION_UP
                        || e.getAction() == MotionEvent.ACTION_CANCEL) scaleSpring(v, 1f, 220);
                return false;
            });
            row.setOnClickListener(v -> {
                LaunchActivity.instance.close3DDrawer();
                LaunchActivity.instance.switchToAccount(accId, true);
            });

            BackupImageView av = new BackupImageView(ctx);
            av.setRoundRadius(AndroidUtilities.dp(21));
            av.setForUserOrChat(fu, new AvatarDrawable(fu));
            row.addView(av, LayoutHelper.createFrame(
                    42, 42,
                    (isRTL() ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL,
                    isRTL() ? 0 : 14, 0, isRTL() ? 14 : 0, 0));

            LinearLayout info = new LinearLayout(ctx);
            info.setOrientation(LinearLayout.VERTICAL);
            info.setGravity(Gravity.CENTER_VERTICAL);

            TextView name = new TextView(ctx);
            name.setTextColor(Theme.getColor(Theme.key_chats_menuItemText));
            name.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14.5f);
            name.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            name.setSingleLine(true);
            name.setEllipsize(TextUtils.TruncateAt.END);
            name.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
            if (android.os.Build.VERSION.SDK_INT >= 17) {
                name.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            }
            name.setText(UserObject.getUserName(fu));
            info.addView(name, LayoutHelper.createLinear(
                    LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            String sub = (fu.phone != null && !fu.phone.isEmpty())
                    ? "+" + fu.phone
                    : (fu.username != null && !fu.username.isEmpty() ? "@" + fu.username : null);
            if (sub != null) {
                TextView subTv = new TextView(ctx);
                subTv.setText(sub);
                subTv.setTextColor(Theme.getColor(Theme.key_chats_menuItemText));
                subTv.setAlpha(0.55f);
                subTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11.5f);
                subTv.setSingleLine(true);
                subTv.setEllipsize(TextUtils.TruncateAt.END);
                subTv.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
                if (android.os.Build.VERSION.SDK_INT >= 17) {
                    subTv.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
                }
                LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                subLp.topMargin = AndroidUtilities.dp(1);
                info.addView(subTv, subLp);
            }

            int accInfoStart = 68;
            int accInfoEnd   = 56;
            int accInfoLeft  = isRTL() ? accInfoEnd : accInfoStart;
            int accInfoRight = isRTL() ? accInfoStart : accInfoEnd;
            row.addView(info, LayoutHelper.createFrame(
                    LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT,
                    (isRTL() ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL,
                    accInfoLeft, 0, accInfoRight, 0));

            ImageView switchIc = new ImageView(ctx);
            switchIc.setImageResource(R.drawable.msg_photo_switch2);
            switchIc.setScaleType(ImageView.ScaleType.CENTER);
            switchIc.setAlpha(0.35f);
            switchIc.setColorFilter(new PorterDuffColorFilter(
                    Theme.getColor(Theme.key_chats_menuItemText), PorterDuff.Mode.SRC_IN));
            row.addView(switchIc, LayoutHelper.createFrame(
                    20, 20,
                    (isRTL() ? Gravity.LEFT : Gravity.RIGHT) | Gravity.CENTER_VERTICAL,
                    isRTL() ? 12 : 0, 0, isRTL() ? 0 : 12, 0));

            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(58));
            rowLp.leftMargin  = AndroidUtilities.dp(8);
            rowLp.rightMargin = AndroidUtilities.dp(8);
            rowLp.topMargin   = AndroidUtilities.dp(2);
            rowLp.bottomMargin = AndroidUtilities.dp(2);
            card.addView(row, rowLp);
        }

        // ── Add account button ───────────────────────────────
        if (count < UserConfig.MAX_ACCOUNT_COUNT) {
            if (hasAny) {
                View div = new View(ctx);
                div.setBackgroundColor(ColorUtils.setAlphaComponent(
                        Theme.getColor(Theme.key_divider), 80));
                LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(1));
                divLp.leftMargin  = AndroidUtilities.dp(16);
                divLp.rightMargin = AndroidUtilities.dp(16);
                divLp.topMargin   = AndroidUtilities.dp(4);
                divLp.bottomMargin = AndroidUtilities.dp(4);
                card.addView(div, divLp);
            }

            LinearLayout addRow = new LinearLayout(ctx);
            addRow.setOrientation(LinearLayout.HORIZONTAL);
            addRow.setGravity(Gravity.CENTER_VERTICAL);

            GradientDrawable addBg = new GradientDrawable();
            addBg.setColor(Color.TRANSPARENT);
            addBg.setCornerRadius(AndroidUtilities.dp(12));
            GradientDrawable addMask = new GradientDrawable();
            addMask.setColor(0xffffffff);
            addMask.setCornerRadius(AndroidUtilities.dp(12));
            addRow.setBackground(new RippleDrawable(
                    ColorStateList.valueOf(Theme.getColor(Theme.key_listSelector)),
                    addBg, addMask));

            addRow.setOnClickListener(v -> {
                LaunchActivity.instance.close3DDrawer();

                int freeAccounts = 0;
                Integer availableAccount = null;

                for (int a = UserConfig.MAX_ACCOUNT_COUNT - 1; a >= 0; a--) {
                    if (!UserConfig.getInstance(a).isClientActivated()) {
                        freeAccounts++;
                        if (availableAccount == null) {
                            availableAccount = a;
                        }
                    }
                }

                if (!UserConfig.hasPremiumOnAccounts()) {
                    freeAccounts -= (UserConfig.MAX_ACCOUNT_COUNT - UserConfig.MAX_ACCOUNT_DEFAULT_COUNT);
                }

                if (freeAccounts > 0 && availableAccount != null) {
                    LaunchActivity.instance.presentFragment(new org.telegram.ui.LoginActivity(availableAccount));
                } else {
                   // Toast.makeText(ctx, "عذراً، لقد وصلت للحد الأقصى من الحسابات", Toast.LENGTH_SHORT).show();
                    if (!UserConfig.hasPremiumOnAccounts()) {
                        if (LaunchActivity.instance != null && LaunchActivity.instance.getActionBarLayout() != null) {
                            org.telegram.ui.ActionBar.BaseFragment lastFragment = LaunchActivity.instance.getActionBarLayout().getLastFragment();
                            if (lastFragment != null) {
                                lastFragment.showDialog(new org.telegram.ui.Components.Premium.LimitReachedBottomSheet(lastFragment, ctx,TYPE_ACCOUNTS , UserConfig.selectedAccount, null));
                            }
                        }
                    }

                }
            });

            addRow.setPadding(
                    AndroidUtilities.dp(14), 0,
                    AndroidUtilities.dp(14), 0);

            ImageView addIc = new ImageView(ctx);
            addIc.setImageResource(R.drawable.msg_add);
            addIc.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            addIc.setColorFilter(new PorterDuffColorFilter(
                    Theme.getColor(Theme.key_chats_menuItemIcon), PorterDuff.Mode.SRC_IN));

            LinearLayout.LayoutParams icLp = new LinearLayout.LayoutParams(
                    AndroidUtilities.dp(26), AndroidUtilities.dp(26));
            addRow.addView(addIc, icLp);

            View space = new View(ctx);
            addRow.addView(space, new LinearLayout.LayoutParams(
                    AndroidUtilities.dp(16), AndroidUtilities.dp(1)));

            TextView addLabel = new TextView(ctx);
            addLabel.setText(LocaleController.getString("AddAccount", R.string.AddAccount));
            addLabel.setTextColor(Theme.getColor(Theme.key_chats_menuItemText));
            addLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15f);
            addLabel.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            addLabel.setSingleLine(true);
            addLabel.setEllipsize(TextUtils.TruncateAt.END);
            addLabel.setTextDirection(View.TEXT_DIRECTION_LOCALE);
            addLabel.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            addLabel.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);

            LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            addRow.addView(addLabel, labelLp);

            LinearLayout.LayoutParams addLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(54));
            addLp.leftMargin  = AndroidUtilities.dp(8);
            addLp.rightMargin = AndroidUtilities.dp(8);
            addLp.topMargin   = AndroidUtilities.dp(2);
            addLp.bottomMargin = AndroidUtilities.dp(2);
            card.addView(addRow, addLp);
        }

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardLp.leftMargin   = AndroidUtilities.dp(12);
        cardLp.rightMargin  = AndroidUtilities.dp(12);
        cardLp.topMargin    = AndroidUtilities.dp(8);
        cardLp.bottomMargin = AndroidUtilities.dp(4);
        accountsContainer.addView(card, cardLp);

        // Thin separator below card — kept subtle
        View sep = new View(ctx);
        sep.setBackgroundColor(ColorUtils.setAlphaComponent(
                Theme.getColor(Theme.key_divider), 70));
        LinearLayout.LayoutParams sepLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(1));
        sepLp.leftMargin  = AndroidUtilities.dp(24);
        sepLp.rightMargin = AndroidUtilities.dp(24);
        sepLp.bottomMargin = AndroidUtilities.dp(4);
        accountsContainer.addView(sep, sepLp);
    }
    // ══════════════════════════════════════════════════════════
    //  OPEN ANIMATION
    // ══════════════════════════════════════════════════════════
    public void playOpenAnimation() {
        headerCard.setAlpha(0f);
        headerCard.setTranslationY(-AndroidUtilities.dp(16));
        headerCard.animate().alpha(1f).translationY(0)
                .setDuration(340)
                .setInterpolator(new org.telegram.ui.Components.CubicBezierInterpolator(
                        0.25f, 1f, 0.5f, 1f))
                .start();

        int dir = AndroidUtilities.dp(isRTL() ? 22 : -22);
        for (int i = 0; i < menuViews.length; i++) {
            View v = menuViews[i];
            if (v == null) continue;
            v.setAlpha(0f);
            v.setTranslationX(dir);
            v.animate().alpha(1f).translationX(0)
                    .setDuration(280)
                    .setStartDelay(90L + i * STAGGER_MS)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator(2f))
                    .start();
        }
    }

    // ══════════════════════════════════════════════════════════
    //  PROFILE UPDATE
    // ══════════════════════════════════════════════════════════
    public void updateUserProfile() {
        TLRPC.User u = UserConfig.getInstance(UserConfig.selectedAccount).getCurrentUser();
        if (u == null) return;
        currentUserId = u.id;
        AvatarDrawable ad = new AvatarDrawable();
        ad.setInfo(u);
        avatarView.setForUserOrChat(u, ad);
        nameTextView.setText(UserObject.getUserName(u));
        if (u.phone != null && !u.phone.isEmpty()) {
            phoneTextView.setText("+" + u.phone);
            phoneTextView.setVisibility(VISIBLE);
        } else if (u.username != null && !u.username.isEmpty()) {
            phoneTextView.setText("@" + u.username);
            phoneTextView.setVisibility(VISIBLE);
        } else {
            phoneTextView.setVisibility(GONE);
        }
    }

    // ══════════════════════════════════════════════════════════
    //  THEME REFRESH
    // ══════════════════════════════════════════════════════════
    public void updateThemeColors() {
        setBackgroundColor(Theme.getColor(Theme.key_chats_menuBackground));
        if (headerBgLayer != null) headerBgLayer.invalidate();

        // Calculate dynamic text/icon/ripple colors for the header card based on gradient luminance
        int base = Theme.getColor(Theme.key_chats_menuTopBackground);
        if ((base >>> 24) < 10) base = Theme.getColor(Theme.key_chats_menuBackground);
        int accentColor = Theme.getColor(Theme.key_actionBarDefault);
        int s = ColorUtils.blendARGB(base, accentColor, 0.60f);
        double lum = ColorUtils.calculateLuminance(s);

        int nameColor, phoneColor, arrowColor;
        if (lum > 0.6) {
            nameColor = Theme.getColor(Theme.key_chats_menuItemText);
            phoneColor = ColorUtils.setAlphaComponent(nameColor, 185);
            arrowColor = Theme.getColor(Theme.key_chats_menuItemIcon);
        } else {
            nameColor = Color.WHITE;
            phoneColor = ColorUtils.setAlphaComponent(Color.WHITE, 185);
            arrowColor = ColorUtils.setAlphaComponent(Color.WHITE, 200);
        }

        if (nameTextView != null) {
            nameTextView.setTextColor(nameColor);
        }
        if (phoneTextView != null) {
            phoneTextView.setTextColor(phoneColor);
        }
        if (arrowView != null) {
            arrowView.setColorFilter(new PorterDuffColorFilter(arrowColor, PorterDuff.Mode.SRC_IN));
            arrowView.setBackground(makeRippleCircle(lum > 0.6 ? 0x1f000000 : 0x28ffffff));
        }

        for (int i = 0; i < itemsContainer.getChildCount(); i++) {
            View c = itemsContainer.getChildAt(i);
            if (!(c instanceof FrameLayout)) {
                c.setBackgroundColor(ColorUtils.setAlphaComponent(
                        Theme.getColor(Theme.key_divider), 120));
            }
        }
        refreshItemColors();

        if (footerLayout != null)
            footerLayout.setBackgroundColor(Theme.getColor(Theme.key_chats_menuBackground));
        if (footerDivider != null)
            footerDivider.setBackgroundColor(Theme.getColor(Theme.key_divider));
        if (footerVersionText != null)
            footerVersionText.setTextColor(Theme.getColor(Theme.key_chats_menuItemIcon));

        populateAccounts();
        invalidate();
    }

    private void refreshItemColors() {
        if (menuViews == null) return;
        int iconTint   = Theme.getColor(Theme.key_chats_menuItemIcon);
        int rippleColor = Theme.getColor(Theme.key_listSelector);

        for (int i = 0; i < menuViews.length; i++) {
            if (!(menuViews[i] instanceof FrameLayout)) continue;
            FrameLayout wrapper = (FrameLayout) menuViews[i];
            if (wrapper.getChildCount() == 0) continue;
            View rowV = wrapper.getChildAt(0);
            if (!(rowV instanceof FrameLayout)) continue;
            FrameLayout row = (FrameLayout) rowV;

            row.setBackground(makeRoundRipple(AndroidUtilities.dp(14),
                    Color.TRANSPARENT, rippleColor));

            for (int j = 0; j < row.getChildCount(); j++) {
                View c = row.getChildAt(j);
                if (c instanceof ImageView) {
                    // Plain icon — no box
                    ((ImageView) c).setColorFilter(
                            new PorterDuffColorFilter(iconTint, PorterDuff.Mode.SRC_IN));
                } else if (c instanceof TextView) {
                    ((TextView) c).setTextColor(
                            Theme.getColor(Theme.key_chats_menuItemText));
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    //  NOTIFICATION CENTER
    // ══════════════════════════════════════════════════════════
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
            org.telegram.messenger.NotificationCenter.getInstance(i)
                    .addObserver(this, org.telegram.messenger.NotificationCenter.appDidLogout);
            org.telegram.messenger.NotificationCenter.getInstance(i)
                    .addObserver(this, org.telegram.messenger.NotificationCenter.mainUserInfoChanged);
        }
        org.telegram.messenger.NotificationCenter.getGlobalInstance()
                .addObserver(this, org.telegram.messenger.NotificationCenter.didSetNewTheme);
        org.telegram.messenger.NotificationCenter.getGlobalInstance()
                .addObserver(this, org.telegram.messenger.NotificationCenter.needSetDayNightTheme);
        org.telegram.messenger.NotificationCenter.getGlobalInstance()
                .addObserver(this, org.telegram.messenger.NotificationCenter.reloadInterface);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
            org.telegram.messenger.NotificationCenter.getInstance(i)
                    .removeObserver(this, org.telegram.messenger.NotificationCenter.appDidLogout);
            org.telegram.messenger.NotificationCenter.getInstance(i)
                    .removeObserver(this, org.telegram.messenger.NotificationCenter.mainUserInfoChanged);
        }
        org.telegram.messenger.NotificationCenter.getGlobalInstance()
                .removeObserver(this, org.telegram.messenger.NotificationCenter.didSetNewTheme);
        org.telegram.messenger.NotificationCenter.getGlobalInstance()
                .removeObserver(this, org.telegram.messenger.NotificationCenter.needSetDayNightTheme);
        org.telegram.messenger.NotificationCenter.getGlobalInstance()
                .removeObserver(this, org.telegram.messenger.NotificationCenter.reloadInterface);
        storyParams.onDetachFromWindow();
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == org.telegram.messenger.NotificationCenter.appDidLogout
                || id == org.telegram.messenger.NotificationCenter.mainUserInfoChanged) {
            updateUserProfile();
            populateAccounts();
        } else if (id == org.telegram.messenger.NotificationCenter.didSetNewTheme
                || id == org.telegram.messenger.NotificationCenter.needSetDayNightTheme) {
            updateThemeColors();
        } else if (id == org.telegram.messenger.NotificationCenter.reloadInterface) {
            rebuildUI();
        }
    }

    // ══════════════════════════════════════════════════════════
    //  DRAWABLE HELPERS
    // ══════════════════════════════════════════════════════════
    private static RippleDrawable makeRoundRipple(int cornerRadius, int bgColor, int rippleColor) {
        GradientDrawable bg   = new GradientDrawable();
        bg.setColor(bgColor);
        bg.setCornerRadius(cornerRadius);
        GradientDrawable mask = new GradientDrawable();
        mask.setColor(0xffffffff);
        mask.setCornerRadius(cornerRadius);
        return new RippleDrawable(ColorStateList.valueOf(rippleColor), bg, mask);
    }

    private static RippleDrawable makeRippleCircle(int rippleColor) {
        GradientDrawable mask = new GradientDrawable();
        mask.setShape(GradientDrawable.OVAL);
        mask.setColor(0xffffffff);
        return new RippleDrawable(ColorStateList.valueOf(rippleColor), null, mask);
    }

    // ══════════════════════════════════════════════════════════
    //  ANIMATION HELPER
    // ══════════════════════════════════════════════════════════
    private void scaleSpring(View v, float to, int dur) {
        AnimatorSet s = new AnimatorSet();
        s.playTogether(
                ObjectAnimator.ofFloat(v, View.SCALE_X, to),
                ObjectAnimator.ofFloat(v, View.SCALE_Y, to));
        s.setDuration(dur);
        s.setInterpolator(to < 1f
                ? new android.view.animation.AccelerateInterpolator()
                : new org.telegram.ui.Components.CubicBezierInterpolator(0.25f, 1f, 0.5f, 1f));
        s.start();
    }

    // ══════════════════════════════════════════════════════════
    //  UTILITY
    // ══════════════════════════════════════════════════════════
    public void rebuildUI() {
        isAccountsExpanded = false;
        removeAllViews();
        buildUI(getContext());
        if (getParent() instanceof Hud3DDrawerLayout) {
            ((Hud3DDrawerLayout) getParent()).onLanguageChanged();
        }
    }

    private boolean isRTL() { return LocaleController.isRTL; }

    private String str(String ar, String en) {
        return isRTL() ? ar : en;
    }

    private void close() { LaunchActivity.instance.close3DDrawer(); }

    private void tabTo(int tab) {
        MainTabsActivity t = findTabs();
        if (t != null) { t.selectTab(tab, true); t.scrollToPosition(tab); }
    }

    private MainTabsActivity findTabs() {
        if (LaunchActivity.instance == null
                || LaunchActivity.instance.actionBarLayout == null) return null;
        for (org.telegram.ui.ActionBar.BaseFragment f :
                LaunchActivity.instance.actionBarLayout.getFragmentStack())
            if (f instanceof MainTabsActivity) return (MainTabsActivity) f;
        return null;
    }

    private void presentFragment(org.telegram.ui.ActionBar.BaseFragment f) {
        if (LaunchActivity.instance != null
                && LaunchActivity.instance.actionBarLayout != null)
            LaunchActivity.instance.actionBarLayout.presentFragment(f);
    }
}