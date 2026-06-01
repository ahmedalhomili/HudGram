package com.hudgram.ui;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.INavigationLayout;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.ScaleStateListAnimator;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.MainTabsActivity;

public class HudUiHelper {

    public static class StoryHeaderAvatarView extends BackupImageView {
        private org.telegram.ui.Components.GradientTools gradientTools;

        public StoryHeaderAvatarView(Context context) {
            super(context);
            setRoundRadius(AndroidUtilities.dp(15));
            gradientTools = new org.telegram.ui.Components.GradientTools();
            gradientTools.isDiagonal = true;
            gradientTools.isRotate = true;
            gradientTools.paint.setStrokeWidth(AndroidUtilities.dpf2(1.5f));
            gradientTools.paint.setStyle(android.graphics.Paint.Style.STROKE);
            gradientTools.paint.setStrokeCap(android.graphics.Paint.Cap.ROUND);
        }

        @Override
        protected void onDraw(android.graphics.Canvas canvas) {
            float padding = AndroidUtilities.dp(3f);
            imageReceiver.setImageCoords(padding, padding, getWidth() - padding * 2, getHeight() - padding * 2);
            imageReceiver.draw(canvas);
            if (blurImageReceiver != null && blurAllowed) {
                blurImageReceiver.setImageCoords(padding, padding, getWidth() - padding * 2, getHeight() - padding * 2);
                blurImageReceiver.draw(canvas);
            }

            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float radius = cx - AndroidUtilities.dp(1f);
            
            int color1 = org.telegram.ui.ActionBar.Theme.getColor(org.telegram.ui.ActionBar.Theme.key_stories_circle_dialog1);
            int color2 = org.telegram.ui.ActionBar.Theme.getColor(org.telegram.ui.ActionBar.Theme.key_stories_circle_dialog2);
            if (color1 == 0 || color2 == 0) {
                color1 = 0xFF34C759; // Fallback green
                color2 = 0xFF26A69A; // Fallback cyan/teal
            }
            gradientTools.setColors(color1, color2);
            gradientTools.setBounds(0, 0, getWidth(), getHeight());
            
            canvas.drawCircle(cx, cy, radius, gradientTools.paint);
        }
    }

    public static BackupImageView createHeaderAvatarView(Context context, INavigationLayout parentLayout) {
        BackupImageView headerAvatarView = new StoryHeaderAvatarView(context);
        headerAvatarView.setOnClickListener(v -> onHeaderAvatarClick(parentLayout));
        headerAvatarView.setOnLongClickListener(v -> {
            onHeaderAvatarLongClick(v, parentLayout);
            return true;
        });
        ScaleStateListAnimator.apply(headerAvatarView);
        return headerAvatarView;
    }

    public static void updateHeaderAvatar(BackupImageView headerAvatarView, ActionBar actionBar, boolean show, TLRPC.User currentUser) {
        if (headerAvatarView == null || actionBar == null) {
            return;
        }
        headerAvatarView.setVisibility(show ? View.VISIBLE : View.GONE);

        if (show) {
            android.view.ViewGroup.LayoutParams rawLp = headerAvatarView.getLayoutParams();
            if (rawLp instanceof android.widget.FrameLayout.LayoutParams) {
                android.widget.FrameLayout.LayoutParams lp = (android.widget.FrameLayout.LayoutParams) rawLp;
                lp.gravity = Gravity.LEFT | Gravity.TOP;
                lp.leftMargin = AndroidUtilities.dp(14);
                lp.rightMargin = 0;
                int additionalTop = actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0;
                int actionBarHeight = ActionBar.getCurrentActionBarHeight();
                int avatarHeight = AndroidUtilities.dp(36);
                lp.topMargin = additionalTop + (actionBarHeight - avatarHeight) / 2 - AndroidUtilities.dp(2.5f);
                headerAvatarView.setLayoutParams(lp);
            }
            actionBar.setTitleRightMargin(AndroidUtilities.dp(48)); // 42dp translation + 6dp buffer
        } else {
            actionBar.setTitleRightMargin(0);
        }

        float translationX = show ? AndroidUtilities.dp(42) : 0;
        SimpleTextView titleView = actionBar.getTitleTextView();
        SimpleTextView titleView2 = actionBar.getTitleTextView2();
        SimpleTextView subtitleView = actionBar.getSubtitleTextView();

        if (titleView != null) {
            titleView.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        }
        if (titleView2 != null) {
            titleView2.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        }
        if (subtitleView != null) {
            subtitleView.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        }

        android.widget.FrameLayout titlesContainer = actionBar.getTitlesContainer();
        if (titlesContainer != null) {
            titlesContainer.setTranslationX(translationX);
        } else {
            if (titleView != null) {
                titleView.setTranslationX(translationX);
            }
            if (titleView2 != null) {
                titleView2.setTranslationX(translationX);
            }
        }

        if (subtitleView != null) {
            subtitleView.setTranslationX(translationX);
        }

        if (show && currentUser != null) {
            AvatarDrawable avatarDrawable = new AvatarDrawable();
            avatarDrawable.setInfo(currentUser);
            headerAvatarView.setForUserOrChat(currentUser, avatarDrawable);
        }
    }

    public static void onHeaderAvatarClick(INavigationLayout parentLayout) {
        if (LaunchActivity.instance != null) {
            LaunchActivity.instance.open3DDrawer();
        }
    }

    public static void onHeaderAvatarLongClick(View v, INavigationLayout parentLayout) {
        MainTabsActivity mainTabsActivity = findMainTabsActivity(parentLayout);
        if (mainTabsActivity != null) {
            mainTabsActivity.openAccountSelector(v);
        }
    }

    public interface ThemeSwitchCallback {
        void run(org.telegram.ui.ActionBar.Theme.ThemeInfo themeInfo, boolean toDark);
    }

    public static void toggleTheme(BaseFragment fragment, ThemeSwitchCallback callback) {
        if (org.telegram.ui.DialogsActivity.switchingTheme) {
            return;
        }
        org.telegram.ui.DialogsActivity.switchingTheme = true;
        android.content.SharedPreferences preferences = org.telegram.messenger.ApplicationLoader.applicationContext.getSharedPreferences("themeconfig", android.app.Activity.MODE_PRIVATE);
        String dayThemeName = preferences.getString("lastDayTheme", "Blue");
        if (org.telegram.ui.ActionBar.Theme.getTheme(dayThemeName) == null || org.telegram.ui.ActionBar.Theme.getTheme(dayThemeName).isDark()) {
            dayThemeName = "Blue";
        }
        String nightThemeName = preferences.getString("lastDarkTheme", "Dark Blue");
        if (org.telegram.ui.ActionBar.Theme.getTheme(nightThemeName) == null || !org.telegram.ui.ActionBar.Theme.getTheme(nightThemeName).isDark()) {
            nightThemeName = "Dark Blue";
        }
        org.telegram.ui.ActionBar.Theme.ThemeInfo themeInfo = org.telegram.ui.ActionBar.Theme.getActiveTheme();
        if (dayThemeName.equals(nightThemeName)) {
            if (themeInfo.isDark() || dayThemeName.equals("Dark Blue") || dayThemeName.equals("Night")) {
                dayThemeName = "Blue";
            } else {
                nightThemeName = "Dark Blue";
            }
        }

        boolean toDark;
        if (toDark = dayThemeName.equals(themeInfo.getKey())) {
            themeInfo = org.telegram.ui.ActionBar.Theme.getTheme(nightThemeName);
        } else {
            themeInfo = org.telegram.ui.ActionBar.Theme.getTheme(dayThemeName);
        }

        if (callback != null) {
            callback.run(themeInfo, toDark);
        }

        org.telegram.ui.ActionBar.Theme.turnOffAutoNight(org.telegram.ui.Components.BulletinFactory.of(fragment), () -> {
            fragment.presentFragment(new org.telegram.ui.ThemeActivity(org.telegram.ui.ThemeActivity.THEME_TYPE_NIGHT));
        });
    }

    private static MainTabsActivity findMainTabsActivity(INavigationLayout parentLayout) {
        if (parentLayout != null) {
            for (BaseFragment fragment : parentLayout.getFragmentStack()) {
                if (fragment instanceof MainTabsActivity) {
                    return (MainTabsActivity) fragment;
                }
            }
        }
        if (LaunchActivity.instance != null && LaunchActivity.instance.getActionBarLayout() != null) {
            for (BaseFragment fragment : LaunchActivity.instance.getActionBarLayout().getFragmentStack()) {
                if (fragment instanceof MainTabsActivity) {
                    return (MainTabsActivity) fragment;
                }
            }
        }
        return null;
    }
}
