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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.widget.FrameLayout;
import androidx.core.graphics.ColorUtils;

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

    private static class SearchItem {
        String titleKey;
        Class<? extends BaseHudSettingsActivity> activityClass;
        String slug;

        SearchItem(String titleKey, Class<? extends BaseHudSettingsActivity> activityClass, String slug) {
            this.titleKey = titleKey;
            this.activityClass = activityClass;
            this.slug = slug;
        }
    }

    private final ArrayList<SearchItem> searchItems = new ArrayList<>();
    {
        // Common settings
        searchItems.add(new SearchItem("DisableInstantCamera", HudCommonSettingsActivity.class, "disabledInstantCamera"));
        searchItems.add(new SearchItem("AskBeforeCalling", HudCommonSettingsActivity.class, "askBeforeCall"));
        
        // Appearance/Main screen settings
        searchItems.add(new SearchItem("HideStoriesBar", HudMainScreenSettingsActivity.class, "hideStoriesBar"));
        searchItems.add(new SearchItem("ShowAvatarInHeader", HudMainScreenSettingsActivity.class, "showAvatarInHeader"));
        searchItems.add(new SearchItem("ShowMyNameInHeader", HudMainScreenSettingsActivity.class, "showMyNameInHeader"));
        searchItems.add(new SearchItem("ShowBioAsSubtitle", HudMainScreenSettingsActivity.class, "showBioAsSubtitle"));
        searchItems.add(new SearchItem("HideSettingsTab", HudMainScreenSettingsActivity.class, "hideSettingsTab"));
        searchItems.add(new SearchItem("HideSearchBar", HudMainScreenSettingsActivity.class, "hideSearchBar"));
        searchItems.add(new SearchItem("HideFolderTabs", HudMainScreenSettingsActivity.class, "hideFolderTabs"));
        searchItems.add(new SearchItem("OpenArchiveOnPull", HudMainScreenSettingsActivity.class, "openArchiveOnPull"));
        
        // Chat settings
        searchItems.add(new SearchItem("DoubleTapSetting", HudChatSettingsActivity.class, "customDoubleTapAction"));
        searchItems.add(new SearchItem("ConfirmStickers", HudChatSettingsActivity.class, "confirmStickers"));
        searchItems.add(new SearchItem("ConfirmVoiceMessages", HudChatSettingsActivity.class, "confirmVoiceMessages"));
        searchItems.add(new SearchItem("PartialCopy", HudChatSettingsActivity.class, "partialCopy"));
        searchItems.add(new SearchItem("HudQuickReplyRow", HudChatSettingsActivity.class, "quickReply"));
        searchItems.add(new SearchItem("HudAutoReplyRow", HudChatSettingsActivity.class, "autoReply"));
        searchItems.add(new SearchItem("ShowChatToolsFab", HudChatSettingsActivity.class, "chatToolsFab"));
        searchItems.add(new SearchItem("TranslationEnabled", HudChatSettingsActivity.class, "translationEnabled"));
        
        // Notifications settings
        searchItems.add(new SearchItem("HideNotificationContent", HudNotificationsSettingsActivity.class, "hideNotificationContent"));
        searchItems.add(new SearchItem("SilenceNonContacts", HudNotificationsSettingsActivity.class, "silenceNonContacts"));
        searchItems.add(new SearchItem("AccentAsNotificationColor", HudNotificationsSettingsActivity.class, "accentAsNotificationColor"));
        
        // Other settings
        searchItems.add(new SearchItem("PreferIPv6", HudOtherSettingsActivity.class, "preferIPv6"));
        searchItems.add(new SearchItem("NameOrder", HudOtherSettingsActivity.class, "nameOrder"));
        searchItems.add(new SearchItem("IdType", HudOtherSettingsActivity.class, "idType"));
    }

    private boolean isSearching;
    private String searchQuery;

    private final int menu_export = 101;
    private final int menu_import = 102;

    @Override
    public View createView(Context context) {
        View fragmentView = super.createView(context);

        org.telegram.ui.ActionBar.ActionBarMenu menu = actionBar.createMenu();
        createSearchItem(menu, new org.telegram.ui.ActionBar.ActionBarMenuItem.ActionBarMenuItemSearchListener() {
            @Override
            public void onSearchExpand() {
                isSearching = true;
                updateActionBarVisible();
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
            }

            @Override
            public void onSearchCollapse() {
                isSearching = false;
                searchQuery = null;
                updateActionBarVisible();
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
            }

            @Override
            public void onTextChanged(android.widget.EditText editText) {
                searchQuery = editText.getText().toString();
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
            }
        });

        org.telegram.ui.ActionBar.ActionBarMenuItem otherItem = menu.addItem(100, R.drawable.ic_ab_other);
        otherItem.addSubItem(menu_export, R.drawable.msg_download, getString("ExportSettings"));
        otherItem.addSubItem(menu_import, R.drawable.msg_openin, getString("ImportSettings"));

        actionBar.setActionBarMenuOnItemClick(new org.telegram.ui.ActionBar.ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == menu_export) {
                    exportSettings();
                } else if (id == menu_import) {
                    importSettings();
                }
            }
        });

        return fragmentView;
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        if (isSearching) {
            String query = searchQuery != null ? searchQuery.toLowerCase() : "";
            int resultId = 10000;
            for (SearchItem sItem : searchItems) {
                String localizedTitle = getString(sItem.titleKey);
                if (localizedTitle.toLowerCase().contains(query)) {
                    UItem item = TextSettingsCellFactory.of(resultId++, localizedTitle);
                    item.object = sItem;
                    items.add(item);
                }
            }
            if (items.isEmpty()) {
                items.add(UItem.asHeader(LocaleController.getString("NoResult", R.string.NoResult)));
            }
            return;
        }

        // === App Header (Logo + Name + Version) ===
        if (headerCell == null) {
            org.telegram.tgnet.TLRPC.User currentUser = org.telegram.messenger.UserConfig.getInstance(currentAccount).getCurrentUser();
            headerCell = new HudgramHeaderCell(getParentActivity(), currentUser, resourcesProvider, v -> {
                if (currentUser != null) {
                    Bundle args = new Bundle();
                    args.putLong("user_id", currentUser.id);
                    presentFragment(new org.telegram.ui.ProfileActivity(args));
                }
            });
        } else {
            org.telegram.tgnet.TLRPC.User currentUser = org.telegram.messenger.UserConfig.getInstance(currentAccount).getCurrentUser();
            headerCell.update(currentUser);
        }
        items.add(UItem.asCustom(headerCell));

        // === Settings Categories ===
        items.add(UItem.asHeader(getString("HudSettingsCategories")));

        items.add(TextSettingsCellFactory.of(generalRow, getString("CommonSettings"), null, R.drawable.msg_settings, 0xff50a8eb)
                .slug("settingsGeneral"));
        items.add(TextSettingsCellFactory.of(appearanceRow, getString("MainScreenSettings"), null, R.drawable.msg_customize, 0xff8f3bf7)
                .slug("settingsAppearance"));
        items.add(TextSettingsCellFactory.of(chatRow, getString("HudSettingsChat"), null, R.drawable.msg_msgbubble3, 0xff4caf50)
                .slug("settingsChat"));
        items.add(TextSettingsCellFactory.of(notificationsRow, getString("HudSettingsNotifications"), null, R.drawable.msg_notifications, 0xffff9800)
                .slug("settingsNotifications"));
        items.add(TextSettingsCellFactory.of(otherRow, getString("HudSettingsOther"), null, R.drawable.msg_permissions, 0xfff44336)
                .slug("settingsOther"));
        items.add(UItem.asShadow(null));

        // === About & Links Footer ===
        items.add(UItem.asHeader(getString("HudAboutAndLinks")));
        items.add(TextSettingsCellFactory.of(officialChannelRow, getString("HudOfficialChannel"), "@hudgramchannel", R.drawable.msg_channel, 0xff00bcd4)
                .slug("officialChannel"));
        items.add(TextSettingsCellFactory.of(aboutHudgramRow, getString("AboutHudgram"), null, R.drawable.logo_hudgram_wthiout_bg, 0xffe91e63)
                .slug("aboutHudgram"));
        items.add(UItem.asShadow(null));
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        if (isSearching) {
            if (item.object instanceof SearchItem) {
                SearchItem sItem = (SearchItem) item.object;
                try {
                    Bundle bundle = new Bundle();
                    bundle.putString("scroll_to", sItem.slug);
                    BaseHudSettingsActivity activity = sItem.activityClass.getDeclaredConstructor().newInstance();
                    activity.setArguments(bundle);
                    presentFragment(activity);
                } catch (Exception e) {
                    org.telegram.messenger.FileLog.e(e);
                }
            }
            return;
        }

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
            presentFragment(new HudAboutActivity());
        }
    }

    @Override
    protected String getActionBarTitle() {
        return getString("HudgramSettings");
    }

    private String backupJsonToSave;

    private void exportSettings() {
        if (getParentActivity() == null) return;
        String json = HudConfig.exportBackup();
        if (json == null) {
            org.telegram.ui.Components.BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_block2, getString("ExportError")).show();
            return;
        }
        backupJsonToSave = json;
        try {
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(android.content.Intent.CATEGORY_OPENABLE);
            intent.setType("application/json");
            intent.putExtra(android.content.Intent.EXTRA_TITLE, "hudgram_backup.json");
            startActivityForResult(intent, 103);
        } catch (Exception e) {
            org.telegram.messenger.FileLog.e(e);
            org.telegram.ui.Components.BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_block2, getString("ExportError")).show();
        }
    }

    private void importSettings() {
        if (getParentActivity() == null) return;
        try {
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_GET_CONTENT);
            intent.setType("application/json");
            intent.addCategory(android.content.Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, 99);
        } catch (Exception e) {
            org.telegram.messenger.FileLog.e(e);
        }
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, android.content.Intent data) {
        if (requestCode == 99 && resultCode == android.app.Activity.RESULT_OK && data != null && data.getData() != null) {
            try {
                android.net.Uri uri = data.getData();
                java.io.InputStream inputStream = getParentActivity().getContentResolver().openInputStream(uri);
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(inputStream));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                inputStream.close();
                boolean success = HudConfig.importBackup(stringBuilder.toString());
                if (success) {
                    org.telegram.ui.Components.BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_check_s, getString("ImportSuccess")).show();
                    if (listView != null && listView.adapter != null) {
                        listView.adapter.update(true);
                    }
                } else {
                    org.telegram.ui.Components.BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_block2, getString("ImportError")).show();
                }
            } catch (Exception e) {
                org.telegram.messenger.FileLog.e(e);
                org.telegram.ui.Components.BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_block2, getString("ImportError")).show();
            }
        } else if (requestCode == 103 && resultCode == android.app.Activity.RESULT_OK && data != null && data.getData() != null) {
            if (backupJsonToSave == null) return;
            try {
                android.net.Uri uri = data.getData();
                java.io.OutputStream outputStream = getParentActivity().getContentResolver().openOutputStream(uri);
                outputStream.write(backupJsonToSave.getBytes("UTF-8"));
                outputStream.close();
                org.telegram.ui.Components.BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_check_s, getString("ExportSuccess")).show();
            } catch (Exception e) {
                org.telegram.messenger.FileLog.e(e);
                org.telegram.ui.Components.BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_block2, getString("ExportError")).show();
            } finally {
                backupJsonToSave = null;
            }
        }
    }

    @Override
    protected String getKey() {
        return "g";
    }

    // ========================
    // Hudgram Header Cell
    // ========================

    public static class HudgramHeaderCell extends android.widget.LinearLayout {

        private org.telegram.ui.Components.BackupImageView avatarView;
        private TextView nameView;
        private TextView usernameView;
        private TextView idView;
        private TextView bioView;
        private android.view.View divider;
        private android.widget.ImageView verifiedIcon;
        private int currentAccount = org.telegram.messenger.UserConfig.selectedAccount;
        private Theme.ResourcesProvider resourcesProvider;

        public HudgramHeaderCell(Context context, org.telegram.tgnet.TLRPC.User currentUser, Theme.ResourcesProvider resourcesProvider, View.OnClickListener clickListener) {
            super(context);
            this.resourcesProvider = resourcesProvider;
            
            setOrientation(LinearLayout.VERTICAL);
            setGravity(Gravity.CENTER_HORIZONTAL);
            setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(24), AndroidUtilities.dp(20), AndroidUtilities.dp(24));
            
            if (android.os.Build.VERSION.SDK_INT >= 23) {
                TypedValue outValue = new TypedValue();
                context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
                setForeground(context.getDrawable(outValue.resourceId));
            }
            setClickable(true);
            setFocusable(true);
            setOnClickListener(clickListener);
            
            // Premium circular avatar frame with a glowing custom border ring
            FrameLayout avatarWrapper = new FrameLayout(context) {
                private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                
                @Override
                protected void dispatchDraw(Canvas canvas) {
                    int accent = Theme.getColor(Theme.key_chats_actionBackground, resourcesProvider);
                    int textColor = Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider);
                    float radius = getMeasuredWidth() / 2f;
                    
                    // Draw outer glow ring (20% opacity gold/accent)
                    glowPaint.setStyle(Paint.Style.STROKE);
                    glowPaint.setStrokeWidth(AndroidUtilities.dp(3f));
                    glowPaint.setColor(ColorUtils.setAlphaComponent(accent, 0x33));
                    canvas.drawCircle(radius, radius, radius - AndroidUtilities.dp(2f), glowPaint);
                    
                    // Draw inner ring (86% opacity theme primary text color)
                    ringPaint.setStyle(Paint.Style.STROKE);
                    ringPaint.setStrokeWidth(AndroidUtilities.dp(2f));
                    ringPaint.setColor(ColorUtils.setAlphaComponent(textColor, 0xdd));
                    canvas.drawCircle(radius, radius, radius - AndroidUtilities.dp(5f), ringPaint);
                    
                    super.dispatchDraw(canvas);
                }
            };
            
            avatarView = new org.telegram.ui.Components.BackupImageView(context);
            avatarView.setRoundRadius(AndroidUtilities.dp(34)); // 68dp width/height
            avatarWrapper.addView(avatarView, LayoutHelper.createFrame(68, 68, Gravity.CENTER));
            
            LinearLayout nameLayout = new LinearLayout(context);
            nameLayout.setOrientation(LinearLayout.HORIZONTAL);
            nameLayout.setGravity(Gravity.CENTER);
            
            nameView = new TextView(context);
            nameView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 21);
            nameView.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            nameView.setGravity(Gravity.CENTER);
            nameView.setMaxLines(1);
            nameView.setEllipsize(TextUtils.TruncateAt.END);
            
            verifiedIcon = new ImageView(context);
            verifiedIcon.setImageResource(R.drawable.logo_hudgram_wthiout_bg);
            verifiedIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            
            if (LocaleController.isRTL) {
                nameLayout.addView(verifiedIcon, LayoutHelper.createLinear(18, 18, Gravity.CENTER_VERTICAL, 0, 0, 8, 0));
                nameLayout.addView(nameView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
            } else {
                nameLayout.addView(nameView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
                nameLayout.addView(verifiedIcon, LayoutHelper.createLinear(18, 18, Gravity.CENTER_VERTICAL, 8, 0, 0, 0));
            }
            
            usernameView = new TextView(context);
            usernameView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            usernameView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            usernameView.setGravity(Gravity.CENTER);
            
            idView = new TextView(context);
            idView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
            idView.setGravity(Gravity.CENTER);
            idView.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(3.5f), AndroidUtilities.dp(10), AndroidUtilities.dp(4f));
            
            GradientDrawable idBg = new GradientDrawable();
            idBg.setCornerRadius(AndroidUtilities.dp(12));
            idView.setBackground(idBg);
            
            // Premium elegant thin horizontal line
            divider = new View(context);
            GradientDrawable divBg = new GradientDrawable();
            divider.setBackground(divBg);
            
            bioView = new TextView(context);
            bioView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13.5f);
            bioView.setMaxLines(3);
            bioView.setEllipsize(TextUtils.TruncateAt.END);
            bioView.setGravity(Gravity.CENTER);
            bioView.setTypeface(Typeface.create("sans-serif", Typeface.ITALIC));
            
            addView(avatarWrapper, LayoutHelper.createLinear(78, 78, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 12));
            addView(nameLayout, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL));
            addView(usernameView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 4, 0, 0));
            addView(idView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 6, 0, 0));
            addView(divider, LayoutHelper.createLinear(AndroidUtilities.dp(80), 1, Gravity.CENTER_HORIZONTAL, 0, 12, 0, 12));
            addView(bioView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 16, 0, 16, 0));
            
            update(currentUser);
        }

        public void update(org.telegram.tgnet.TLRPC.User currentUser) {
            int accentColor = Theme.getColor(Theme.key_chats_actionBackground, resourcesProvider);
            int bgColor = Theme.getColor(Theme.key_windowBackgroundWhite, resourcesProvider);
            int primaryTextColor = Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider);
            int secondaryTextColor = Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2, resourcesProvider);
            
            // Set cell background to match settings rows
            setBackgroundColor(bgColor);
            
            // Dynamic text and drawable tinting using official theme keys
            nameView.setTextColor(primaryTextColor);
            verifiedIcon.setColorFilter(new android.graphics.PorterDuffColorFilter(accentColor, android.graphics.PorterDuff.Mode.SRC_IN));
            usernameView.setTextColor(accentColor);
            
            // ID badge styling - gold integrated & 100% theme-adaptive
            idView.setTextColor(ColorUtils.blendARGB(secondaryTextColor, accentColor, 0.15f));
            GradientDrawable idBg = (GradientDrawable) idView.getBackground();
            if (idBg != null) {
                idBg.setColor(ColorUtils.setAlphaComponent(secondaryTextColor, 0x1a)); // 10% opacity grey
                idBg.setStroke(AndroidUtilities.dp(1f), ColorUtils.setAlphaComponent(accentColor, 0x33)); // 20% opacity gold/accent
            }
            
            // Divider color styling
            GradientDrawable divBg = (GradientDrawable) divider.getBackground();
            if (divBg != null) {
                divBg.setColor(ColorUtils.setAlphaComponent(accentColor, 0x22)); // 13% opacity gold/accent
            }
            
            // Bio text color
            bioView.setTextColor(secondaryTextColor);
            
            if (currentUser != null) {
                org.telegram.ui.Components.AvatarDrawable avatarDrawable = new org.telegram.ui.Components.AvatarDrawable();
                avatarDrawable.setInfo(currentUser);
                avatarView.setForUserOrChat(currentUser, avatarDrawable);
                
                nameView.setText(org.telegram.messenger.UserObject.getUserName(currentUser));
                
                if (!TextUtils.isEmpty(currentUser.username)) {
                    usernameView.setText("@" + currentUser.username);
                    usernameView.setVisibility(VISIBLE);
                } else {
                    usernameView.setVisibility(GONE);
                }
                
                idView.setText((LocaleController.isRTL ? "المعرف: " : "ID: ") + currentUser.id);
                idView.setVisibility(VISIBLE);
                
                String bioText = null;
                org.telegram.tgnet.TLRPC.UserFull userFull = org.telegram.messenger.MessagesController.getInstance(currentAccount).getUserFull(currentUser.id);
                if (userFull != null && !TextUtils.isEmpty(userFull.about)) {
                    bioText = userFull.about;
                }
                if (!TextUtils.isEmpty(bioText)) {
                    bioView.setText("“" + bioText + "”");
                    bioView.setVisibility(VISIBLE);
                    divider.setVisibility(VISIBLE);
                } else {
                    bioView.setVisibility(GONE);
                    divider.setVisibility(GONE);
                }
            } else {
                nameView.setText("Hudgram User");
                usernameView.setVisibility(GONE);
                idView.setVisibility(GONE);
                bioView.setVisibility(GONE);
                divider.setVisibility(GONE);
            }
        }
    }
}
