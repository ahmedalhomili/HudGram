package com.hudgram.ui;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

import java.util.ArrayList;

public class HudGeneralSettingsActivity extends BaseHudSettingsActivity {

    // Category navigation rows
    private final int generalRow = rowId++;
    private final int appearanceRow = rowId++;
    private final int chatRow = rowId++;
    private final int otherRow = rowId++;

    private final int notificationsRow = rowId++;

    // Footer links
    private final int officialChannelRow = rowId++;
    private final int aboutHudgramRow = rowId++;

    private HudgramHeaderCell headerCell;

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {

        // === App Header (Logo + Name + Version) ===
        if (headerCell == null) {
            headerCell = new HudgramHeaderCell(getParentActivity());
        }
        items.add(UItem.asCustom(headerCell));

        // === Settings Categories ===
        items.add(UItem.asHeader(getString("HudSettingsCategories")));

        items.add(TextSettingsCellFactory.of(generalRow, getString("CommonSettings"), null)
                .slug("settingsGeneral"));
        items.add(TextSettingsCellFactory.of(appearanceRow, getString("MainScreenSettings"), null)
                .slug("settingsAppearance"));
        items.add(TextSettingsCellFactory.of(chatRow, getString("HudSettingsChat"), null)
                .slug("settingsChat"));
        items.add(TextSettingsCellFactory.of(notificationsRow, getString("HudSettingsNotifications"), null)
                .slug("settingsNotifications"));
        items.add(TextSettingsCellFactory.of(otherRow, getString("HudSettingsOther"), null)
                .slug("settingsOther"));
        items.add(UItem.asShadow(null));

        // === About & Links Footer ===
        items.add(UItem.asHeader(getString("HudAboutAndLinks")));
        items.add(TextSettingsCellFactory.of(officialChannelRow, getString("HudOfficialChannel"), "@hudgramchannel")
                .slug("officialChannel"));
        items.add(TextSettingsCellFactory.of(aboutHudgramRow, getString("AboutHudgram"), null)
                .slug("aboutHudgram"));
        items.add(UItem.asShadow(null));
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        int id = item.id;
        if (id == generalRow) {
            presentFragment(new HudCommonSettingsActivity());
        } else if (id == appearanceRow) {
            presentFragment(new HudMainScreenSettingsActivity());
        } else if (id == chatRow) {
            presentFragment(new HudChatSettingsActivity());
        } else if (id == notificationsRow) {
            presentFragment(new HudNotificationsSettingsActivity());
        } else if (id == otherRow) {
            presentFragment(new HudOtherSettingsActivity());
        } else if (id == officialChannelRow) {
            // Open the official channel
            try {
                MessagesController.getInstance(currentAccount).getUserNameResolver().resolve("hudgramchannel", peerId -> {
                    if (peerId != null) {
                        AndroidUtilities.runOnUIThread(() -> {
                            Bundle args = new Bundle();
                            args.putLong("chat_id", -peerId);
                            presentFragment(new org.telegram.ui.ChatActivity(args));
                        });
                    }
                });
            } catch (Exception e) {
                org.telegram.messenger.FileLog.e(e);
            }
        } else if (id == aboutHudgramRow) {
            // Show About dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
            builder.setTitle(getString("AboutHudgram"));
            builder.setMessage(getString("AboutHudgramText"));
            builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
            builder.show();
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

    // ========================
    // Hudgram Header Cell
    // ========================

    public static class HudgramHeaderCell extends LinearLayout {

        public HudgramHeaderCell(Context context) {
            super(context);
            setOrientation(VERTICAL);
            setGravity(Gravity.CENTER_HORIZONTAL);
            setPadding(0, AndroidUtilities.dp(28), 0, AndroidUtilities.dp(20));

            // App Icon
            ImageView iconView = new ImageView(context);
            iconView.setImageResource(R.mipmap.ic_launcher);
            iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            addView(iconView, new LinearLayout.LayoutParams(AndroidUtilities.dp(80), AndroidUtilities.dp(80)));

            // App Name
            TextView nameView = new TextView(context);
            nameView.setText("Hudgram");
            nameView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 22);
            nameView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            nameView.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            nameView.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            nameLp.topMargin = AndroidUtilities.dp(10);
            addView(nameView, nameLp);

            // Version
            TextView versionView = new TextView(context);
            String versionText = "v" + BuildVars.BUILD_VERSION_STRING;
            try {
                PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                versionText = "v" + pInfo.versionName + " (" + pInfo.versionCode + ")";
            } catch (Exception ignored) {}
            versionView.setText(versionText);
            versionView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            versionView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            versionView.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams versionLp = new LinearLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            versionLp.topMargin = AndroidUtilities.dp(4);
            addView(versionView, versionLp);
        }
    }
}
