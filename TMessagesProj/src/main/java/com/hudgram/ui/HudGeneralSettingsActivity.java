package com.hudgram.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationsController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

import java.util.ArrayList;

public class HudGeneralSettingsActivity extends BaseHudSettingsActivity {

    private final int translationSettingsRow = rowId++;
    private final int mainScreenSettingsRow = rowId++;
    private final int commonSettingsRow = rowId++;
    private final int hideNotificationContentRow = rowId++;

    private NotificationColorPickerCell colorPickerCell;

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        // === Sections / Sub-Settings Screens ===
        items.add(UItem.asHeader(getString("SettingsSections")));
        items.add(TextSettingsCellFactory.of(translationSettingsRow, getString("TranslationSettings"), null).slug("translationSettings"));
        items.add(TextSettingsCellFactory.of(mainScreenSettingsRow, getString("MainScreenSettings"), null).slug("mainScreenSettings"));
        items.add(TextSettingsCellFactory.of(commonSettingsRow, getString("CommonSettings"), null).slug("commonSettings"));
        items.add(UItem.asShadow(null));

        // === Notification Icon Color ===
        items.add(UItem.asHeader(LocaleController.getString("NotificationAppearance", R.string.NotificationAppearance)));

        items.add(UItem.asCheck(hideNotificationContentRow, getString("HideNotificationContent"))
                .slug("hideNotificationContent")
                .setChecked(HudConfig.hideNotificationContent));
        items.add(UItem.asShadow(getString("HideNotificationContentAbout")));

        if (colorPickerCell == null) {
            int currentAccount = UserConfig.selectedAccount;
            SharedPreferences preferences = MessagesController.getNotificationsSettings(currentAccount);
            int selectedColor = preferences.getInt("hud_notification_color", 0xff11acfa);
            colorPickerCell = new NotificationColorPickerCell(getParentActivity(), selectedColor, color -> {
                preferences.edit().putInt("hud_notification_color", color).apply();
                for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
                    NotificationsController.getInstance(i).loadNotificationColors();
                }
                BulletinFactory.of(this).createSimpleBulletin(
                        R.drawable.notification,
                        LocaleController.getString("NotificationColorApplied", R.string.NotificationColorApplied)
                ).show();
            });
        }
        items.add(UItem.asCustom(colorPickerCell));
        items.add(UItem.asShadow(null));
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        int id = item.id;
        if (id == translationSettingsRow) {
            presentFragment(new HudTranslationSettingsActivity());
        } else if (id == mainScreenSettingsRow) {
            presentFragment(new HudMainScreenSettingsActivity());
        } else if (id == commonSettingsRow) {
            presentFragment(new HudCommonSettingsActivity());
        } else if (id == hideNotificationContentRow) {
            HudConfig.toggleHideNotificationContent();
            if (view instanceof org.telegram.ui.Cells.TextCheckCell) {
                ((org.telegram.ui.Cells.TextCheckCell) view).setChecked(HudConfig.hideNotificationContent);
            }
        }
    }

    @Override
    protected String getActionBarTitle() {
        return getString("HudgramSettings");
    }

    @Override
    protected String getKey() {
        return "g";
    }

    public static class NotificationColorPickerCell extends FrameLayout {
        
        private final int[] colors = {
            0xff11acfa, // Blue (Hudgram Default)
            0xff00cbd6, // Teal
            0xff00d262, // Green
            0xffff9800, // Orange
            0xfff44336, // Red
            0xffe91e63, // Pink
            0xff9c27b0, // Purple
            0xffffc107  // Yellow
        };
        
        private final ArrayList<FrameLayout> optionViews = new ArrayList<>();
        private int selectedColor;
        private final Utilities.Callback<Integer> onColorSelected;

        public NotificationColorPickerCell(Context context, int selectedColor, Utilities.Callback<Integer> onColorSelected) {
            super(context);
            this.selectedColor = selectedColor;
            this.onColorSelected = onColorSelected;

            setPadding(0, AndroidUtilities.dp(12), 0, AndroidUtilities.dp(16));

            // Title
            TextView titleView = new TextView(context);
            titleView.setText(LocaleController.getString("NotificationIconColor", R.string.NotificationIconColor));
            titleView.setTextSize(14);
            titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            addView(titleView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP, 21, 0, 21, 0));

            // Horizontal ScrollView for colors
            HorizontalScrollView scrollView = new HorizontalScrollView(context);
            scrollView.setHorizontalScrollBarEnabled(false);
            scrollView.setOverScrollMode(OVER_SCROLL_NEVER);
            
            LinearLayout container = new LinearLayout(context);
            container.setOrientation(LinearLayout.HORIZONTAL);
            container.setPadding(AndroidUtilities.dp(15), 0, AndroidUtilities.dp(15), 0);
            
            for (int i = 0; i < colors.length; i++) {
                final int color = colors[i];
                final FrameLayout optionView = new FrameLayout(context);
                optionView.setClickable(true);
                optionView.setFocusable(true);
                
                // Add the inner circle showing the color/icon
                View colorCircle = new View(context);
                optionView.addView(colorCircle, LayoutHelper.createFrame(40, 40, Gravity.CENTER));
                
                ImageView iconView = new ImageView(context);
                iconView.setImageResource(R.drawable.notification);
                iconView.setScaleType(ImageView.ScaleType.CENTER);
                optionView.addView(iconView, LayoutHelper.createFrame(22, 22, Gravity.CENTER));
                
                updateOptionState(colorCircle, iconView, color, color == selectedColor);
                
                optionView.setOnClickListener(v -> {
                    if (this.selectedColor != color) {
                        this.selectedColor = color;
                        for (int j = 0; j < colors.length; j++) {
                            FrameLayout child = optionViews.get(j);
                            View childCircle = child.getChildAt(0);
                            ImageView childIcon = (ImageView) child.getChildAt(1);
                            updateOptionState(childCircle, childIcon, colors[j], colors[j] == this.selectedColor);
                        }
                        if (this.onColorSelected != null) {
                            this.onColorSelected.run(color);
                        }
                    }
                });
                
                // Apply background selector (ripple)
                optionView.setBackground(Theme.createSimpleSelectorCircleDrawable(AndroidUtilities.dp(48), 0, Theme.getColor(Theme.key_listSelector)));
                
                optionViews.add(optionView);
                container.addView(optionView, LayoutHelper.createLinear(48, 48, 6, 0, 6, 0));
            }
            
            scrollView.addView(container, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
            addView(scrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, Gravity.TOP, 0, 26, 0, 0));
        }

        private void updateOptionState(View circle, ImageView icon, int color, boolean selected) {
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            if (selected) {
                drawable.setColor(color);
                drawable.setStroke(0, 0);
            } else {
                drawable.setColor(0x00000000);
                drawable.setStroke(AndroidUtilities.dp(1.5f), color);
            }
            circle.setBackground(drawable);
            
            icon.setColorFilter(new android.graphics.PorterDuffColorFilter(
                selected ? 0xffffffff : color,
                android.graphics.PorterDuff.Mode.SRC_IN
            ));
        }
    }
}
