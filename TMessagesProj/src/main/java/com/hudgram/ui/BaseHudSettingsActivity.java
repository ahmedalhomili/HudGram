package com.hudgram.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.INavigationLayout;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckbox2Cell;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.ScaleStateListAnimator;
import org.telegram.ui.Components.SizeNotifierFrameLayout;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.URLSpanNoUnderline;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;
import org.telegram.ui.Components.blur3.DownscaleScrollableNoiseSuppressor;
import org.telegram.ui.Components.blur3.ViewGroupPartRenderer;
import org.telegram.ui.Components.blur3.capture.IBlur3Capture;
import org.telegram.ui.Components.blur3.source.BlurredBackgroundSourceRenderNode;

import java.util.ArrayList;
import java.util.Locale;

public abstract class BaseHudSettingsActivity extends BaseFragment {

    protected static final Object PARTIAL = new Object();

    protected SizeNotifierFrameLayout contentView;
    protected UniversalRecyclerView listView;
    protected LinearLayoutManager layoutManager;
    protected Theme.ResourcesProvider resourcesProvider;
    protected View actionBarBackground;
    protected ActionBarMenuItem searchItem;

    protected int rowId = 1;

    public BaseHudSettingsActivity() {
        this(null);
    }

    public BaseHudSettingsActivity(Bundle args) {
        super(args);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            scrollableViewNoiseSuppressor = new DownscaleScrollableNoiseSuppressor();
            iBlur3SourceGlassFrosted = new BlurredBackgroundSourceRenderNode(null);
            iBlur3SourceGlass = new BlurredBackgroundSourceRenderNode(null);
        } else {
            scrollableViewNoiseSuppressor = null;
            iBlur3SourceGlassFrosted = null;
            iBlur3SourceGlass = null;
        }
    }

    @Override
    public View createView(Context context) {
        contentView = new SizeNotifierFrameLayout(context) {
            @Override
            protected void dispatchDraw(Canvas canvas) {
                if (Build.VERSION.SDK_INT >= 31 && scrollableViewNoiseSuppressor != null) {
                    blur3_InvalidateBlur();

                    final int width = getMeasuredWidth();
                    final int height = getMeasuredHeight();
                    if (iBlur3SourceGlassFrosted != null && !iBlur3SourceGlassFrosted.inRecording()) {
                        final Canvas c = iBlur3SourceGlassFrosted.beginRecording(width, height);
                        c.drawColor(getThemedColor(Theme.key_windowBackgroundWhite));
                        if (SharedConfig.chatBlurEnabled()) {
                            scrollableViewNoiseSuppressor.draw(c, DownscaleScrollableNoiseSuppressor.DRAW_FROSTED_GLASS);
                        }
                        iBlur3SourceGlassFrosted.endRecording();
                    }
                    if (iBlur3SourceGlass != null && !iBlur3SourceGlass.inRecording()) {
                        final Canvas c = iBlur3SourceGlass.beginRecording(width, height);
                        c.drawColor(getThemedColor(Theme.key_windowBackgroundWhite));
                        if (SharedConfig.chatBlurEnabled()) {
                            scrollableViewNoiseSuppressor.draw(c, DownscaleScrollableNoiseSuppressor.DRAW_GLASS);
                        }
                        iBlur3SourceGlass.endRecording();
                    }
                    iBlur3Invalidated = false;
                }
                super.dispatchDraw(canvas);
            }

            @Override
            public void drawBlurRect(Canvas canvas, float y, Rect rectTmp, Paint blurScrimPaint, boolean top) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || !SharedConfig.chatBlurEnabled() || iBlur3SourceGlassFrosted == null) {
                    canvas.drawRect(rectTmp, blurScrimPaint);
                    return;
                }

                canvas.save();
                canvas.translate(0, -y);
                iBlur3SourceGlassFrosted.draw(canvas, rectTmp.left, rectTmp.top + y, rectTmp.right, rectTmp.bottom + y);
                canvas.restore();

                final int oldScrimAlpha = blurScrimPaint.getAlpha();
                blurScrimPaint.setAlpha(ChatActivity.ACTION_BAR_BLUR_ALPHA);
                canvas.drawRect(rectTmp, blurScrimPaint);
                blurScrimPaint.setAlpha(oldScrimAlpha);
            }
        };
        contentView.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundGray));

        listView = new UniversalRecyclerView(this, this::fillItems, this::onItemClick, (item, view, position, x, y) -> {
            if (onItemLongClick(item, view, position, x, y)) {
                return true;
            }
            String slug = item.slug;
            String key = getKey();
            if (key != null && item.enabled && !TextUtils.isEmpty(slug)) {
                ItemOptions.makeOptions(this, view)
                        .setScrimViewBackground(listView.getClipBackground(view))
                        .add(R.drawable.msg_copy, getString("CopyLink"), () -> {
                            if ("copyReportId".equals(slug)) {
                                AndroidUtilities.addToClipboard(String.format(Locale.getDefault(), "https://%s/hudsettings/%s", getMessagesController().linkPrefix, "reportId"));
                            } else if ("checkUpdate".equals(slug)) {
                                AndroidUtilities.addToClipboard(String.format(Locale.getDefault(), "https://%s/hudsettings/%s", getMessagesController().linkPrefix, "update"));
                            } else {
                                AndroidUtilities.addToClipboard(String.format(Locale.getDefault(), "https://%s/hudsettings/%s?r=%s", getMessagesController().linkPrefix, key, slug));
                            }
                            BulletinFactory.of(this).createCopyLinkBulletin().show();
                        })
                        .setMinWidth(190)
                        .show();
                return true;
            }
            return false;
        }) {

            @Override
            public Integer getSelectorColor(int position) {
                return BaseHudSettingsActivity.this.getSelectorColor(position);
            }
        };
        listView.adapter.setApplyBackground(false);
        listView.setClipToPadding(false);
        listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && scrollableViewNoiseSuppressor != null) {
                    scrollableViewNoiseSuppressor.onScrolled(dx, dy);
                    blur3_InvalidateBlur();
                }
            }
        });
        iBlur3Capture = new ViewGroupPartRenderer(listView, contentView, listView::drawChild);
        listView.addEdgeEffectListener(() -> listView.postOnAnimation(this::blur3_InvalidateBlur));
        listView.setSections();
        if (!actionBar.getOccupyStatusBar()) {
            listView.setPadding(0, needActionBarPadding() ? ActionBar.getCurrentActionBarHeight() : AndroidUtilities.dp(12), 0, 0);
        }
        contentView.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.FILL));

        actionBarBackground = new View(context) {
            private final Paint blurScrimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

            @Override
            protected void onDraw(@NonNull Canvas canvas) {
                int top = actionBar.getHeight();
                AndroidUtilities.rectTmp2.set(0, 0, getMeasuredWidth(), top);
                blurScrimPaint.setColor(Theme.getColor(Theme.key_actionBarDefault, resourceProvider));
                contentView.drawBlurRect(canvas, 0, AndroidUtilities.rectTmp2, blurScrimPaint, true);
                if (getParentLayout() != null) {
                    getParentLayout().drawHeaderShadow(canvas, top);
                }
            }
        };
        contentView.addView(actionBarBackground, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 200, Gravity.TOP));
        contentView.addView(actionBar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.FILL_HORIZONTAL | Gravity.TOP));

        listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                updateActionBarVisible();
            }
        });

        updateActionBarVisible(true, false);

        return fragmentView = contentView;
    }

    protected void createSearchItem(ActionBarMenu menu, ActionBarMenuItem.ActionBarMenuItemSearchListener searchListener) {
        searchItem = menu.addItem(0, R.drawable.outline_header_search, resourceProvider).setIsSearchField(true).setActionBarMenuItemSearchListener(searchListener);
        searchItem.setSearchFieldHint(getString("Search"));
        searchItem.setContentDescription(getString("Search"));
    }

    protected boolean isSearchFieldVisible() {
        return searchItem != null && searchItem.isSearchFieldVisible2();
    }

    @Override
    public boolean onBackPressed(boolean invoked) {
        if (isSearchFieldVisible()) {
            if (invoked) actionBar.closeSearchField();
            return false;
        }
        return super.onBackPressed(invoked);
    }

    private boolean actionBarVisible;
    private ValueAnimator actionBarVisibleAnimator;

    protected void updateActionBarVisible() {
        updateActionBarVisible(false, true);
    }

    private void updateActionBarVisible(boolean force, boolean animated) {
        final boolean visible;
        if (isSearchFieldVisible()) {
            visible = true;
        } else if (listView.getChildCount() > 0) {
            View firstChild = listView.getChildAt(0);
            visible = needActionBarPadding() ? listView.canScrollVertically(-1) : (
                    listView.getChildAdapterPosition(firstChild) > 0 ||
                            firstChild.getY() + firstChild.getHeight() < actionBar.getHeight()
            );
        } else {
            visible = false;
        }
        if (actionBarVisible == visible && !force) return;

        actionBarVisible = visible;
        if (actionBarVisibleAnimator != null) {
            actionBarVisibleAnimator.cancel();
            actionBarVisibleAnimator = null;
        }
        if (!animated) {
            if (!needActionBarPadding())
                actionBar.getTitlesContainer().setAlpha(visible ? 1.0f : 0.0f);
            actionBarBackground.setAlpha(visible ? 1.0f : 0.0f);
        } else {
            actionBarVisibleAnimator = ValueAnimator.ofFloat(actionBarBackground.getAlpha(), visible ? 1.0f : 0.0f);
            actionBarVisibleAnimator.addUpdateListener(a -> {
                final float t = (float) a.getAnimatedValue();
                if (!needActionBarPadding()) actionBar.getTitlesContainer().setAlpha(t);
                actionBarBackground.setAlpha(t);
            });
            actionBarVisibleAnimator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
            actionBarVisibleAnimator.setDuration(420);
            actionBarVisibleAnimator.start();
        }
    }


    @Override
    public void setParentLayout(INavigationLayout layout) {
        if (layout != null && layout.getLastFragment() != null) {
            resourcesProvider = layout.getLastFragment().getResourceProvider();
        }
        super.setParentLayout(layout);
    }

    @Override
    public ActionBar createActionBar(Context context) {
        ActionBar actionBar = super.createActionBar(context);
        actionBar.setBackgroundColor(Color.TRANSPARENT);
        actionBar.setAddToContainer(false);
        actionBar.setUseContainerForTitles();
        actionBar.setOccupyStatusBar(!AndroidUtilities.isTablet());
        actionBar.setTitle(getActionBarTitle());
        actionBar.setTitleColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
        actionBar.setItemsColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText), false);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        return actionBar;
    }

    protected String getKey() {
        return null;
    }

    protected abstract void fillItems(ArrayList<UItem> items, UniversalAdapter adapter);

    protected abstract void onItemClick(UItem item, View view, int position, float x, float y);

    protected boolean onItemLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }

    protected abstract String getActionBarTitle();

    public Integer getSelectorColor(int position) {
        return getThemedColor(Theme.key_listSelector);
    }

    protected boolean needActionBarPadding() {
        return true;
    }

    protected void showRestartBulletin() {
        BulletinFactory.of(this).createErrorBulletin(getString("RestartAppToTakeEffect")).show();
    }

    protected void updateRows() {
        listView.adapter.updateWithoutNotify();
    }

    protected void notifyItemChanged(int itemId) {
        notifyItemChanged(itemId, null);
    }

    protected void notifyItemChanged(int itemId, Object payload) {
        listView.adapter.notifyItemChanged(listView.findPositionByItemId(itemId), payload);
    }

    protected void notifyItemRemoved(int itemId) {
        listView.adapter.notifyItemRemoved(listView.findPositionByItemId(itemId));
    }

    protected void notifyItemInserted(int itemId) {
        listView.adapter.notifyItemInserted(listView.findPositionByItemId(itemId));
    }

    protected void notifyItemRangeRemoved(int itemId, int itemCount) {
        listView.adapter.notifyItemRangeRemoved(listView.findPositionByItemId(itemId), itemCount);
    }

    protected void notifyItemRangeInserted(int itemId, int itemCount) {
        listView.adapter.notifyItemRangeInserted(listView.findPositionByItemId(itemId), itemCount);
    }

    public static CharSequence getSpannedString(int id, String url) {
        String text = LocaleController.getString(id);
        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        int index1 = text.indexOf("**");
        int index2 = text.lastIndexOf("**");
        if (index1 >= 0 && index2 >= 0 && index1 != index2) {
            builder.replace(index2, index2 + 2, "");
            builder.replace(index1, index1 + 2, "");
            builder.setSpan(new URLSpanNoUnderline(url), index1, index2 - 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return builder;
    }

    public void scrollToRow(String key, Runnable unknown) {
        if (listView == null) return;
        int position = listView.findPositionByItemSlug(key);
        if (position != -1) {
            listView.highlightRow(() -> {
                LinearLayoutManager layoutManager = (LinearLayoutManager) listView.getLayoutManager();
                layoutManager.scrollToPositionWithOffset(position, AndroidUtilities.dp(60));
                return position;
            });
        } else {
            unknown.run();
        }
    }

    @Override
    public boolean isSupportEdgeToEdge() {
        return true;
    }

    @Override
    public void onInsets(int left, int top, int right, int bottom) {
        int topPadding = needActionBarPadding() ? ActionBar.getCurrentActionBarHeight() : AndroidUtilities.dp(12);
        listView.setPadding(0, top + topPadding, 0, bottom);
        super.onInsets(left, top, right, bottom);
    }

    /* Localization helper dynamic mappings */
    public String getString(String key) {
        int resId = LocaleController.getStringResId(key);
        if (resId != 0) {
            return LocaleController.getString(key);
        }
        switch (key) {
            case "Connection": return LocaleController.getString("Connection", R.string.Connection);
            case "PreferIPv6": return LocaleController.getString("PreferIPv6", R.string.PreferIPv6);
            case "Translator": return LocaleController.getString("Translator", R.string.Translator);
            case "TranslatorType": return LocaleController.getString("TranslatorType", R.string.TranslatorType);
            case "TranslatorTypeTG": return LocaleController.getString("TranslatorTypeTG", R.string.TranslatorTypeTG);
            case "TranslatorTypeExternal": return LocaleController.getString("TranslatorTypeExternal", R.string.TranslatorTypeExternal);
            case "TranslatorTypeNeko": return LocaleController.getString("TranslatorTypeNeko", R.string.TranslatorTypeNeko);
            case "TranslatorShowOriginal": return LocaleController.getString("TranslatorShowOriginal", R.string.TranslatorShowOriginal);
            case "TranslationProviderShort": return LocaleController.getString("TranslationProviderShort", R.string.TranslationProviderShort);
            case "TranslationTarget": return LocaleController.getString("TranslationTarget", R.string.TranslationTarget);
            case "TranslationTargetApp": return LocaleController.getString("TranslationTargetApp", R.string.TranslationTargetApp);
            case "DoNotTranslate": return LocaleController.getString("DoNotTranslate", R.string.DoNotTranslate);
            case "AutoTranslate": return LocaleController.getString("AutoTranslate", R.string.AutoTranslate);
            case "AutoTranslateAbout": return LocaleController.getString("AutoTranslateAbout", R.string.AutoTranslateAbout);
            case "TranslateMessagesInfo1": return LocaleController.getString("TranslateMessagesInfo1", R.string.TranslateMessagesInfo1);
            case "Notifications": return LocaleController.getString("Notifications", R.string.Notifications);
            case "AccentAsNotificationColor": return LocaleController.getString("AccentAsNotificationColor", R.string.AccentAsNotificationColor);
            case "SilenceNonContacts": return LocaleController.getString("SilenceNonContacts", R.string.SilenceNonContacts);
            case "SilenceNonContactsAbout": return LocaleController.getString("SilenceNonContactsAbout", R.string.SilenceNonContactsAbout);
            case "UserColorTabProfile": return LocaleController.getString("UserColorTabProfile", R.string.UserColorTabProfile);
            case "NameOrder": return LocaleController.getString("NameOrder", R.string.NameOrder);
            case "FirstLast": return LocaleController.getString("FirstLast", R.string.FirstLast);
            case "LastFirst": return LocaleController.getString("LastFirst", R.string.LastFirst);
            case "IdType": return LocaleController.getString("IdType", R.string.IdType);
            case "IdTypeHidden": return LocaleController.getString("IdTypeHidden", R.string.IdTypeHidden);
            case "IdTypeAPI": return LocaleController.getString("IdTypeAPI", R.string.IdTypeAPI);
            case "IdTypeBOTAPI": return LocaleController.getString("IdTypeBOTAPI", R.string.IdTypeBOTAPI);
            case "IdTypeAbout": return LocaleController.getString("IdTypeAbout", R.string.IdTypeAbout);
            case "General": return LocaleController.getString("General", R.string.General);
            case "DisableInstantCamera": return LocaleController.getString("DisableInstantCamera", R.string.DisableInstantCamera);
            case "AskBeforeCalling": return LocaleController.getString("AskBeforeCalling", R.string.AskBeforeCalling);
            case "OpenArchiveOnPull": return LocaleController.getString("OpenArchiveOnPull", R.string.OpenArchiveOnPull);
            case "RestartAppToTakeEffect": return LocaleController.getString("RestartAppToTakeEffect", R.string.RestartAppToTakeEffect);
            case "Languages": return LocaleController.getString("Languages", R.string.Languages);
            case "CopyLink": return LocaleController.getString("CopyLink", R.string.CopyLink);
            case "Search": return LocaleController.getString("Search", R.string.Search);
            case "TranslationSettings": return LocaleController.getString("TranslationSettings", R.string.TranslationSettings);
            case "TranslationEnabled": return LocaleController.getString("TranslationEnabled", R.string.TranslationEnabled);
            case "TranslationEnabledAbout": return LocaleController.getString("TranslationEnabledAbout", R.string.TranslationEnabledAbout);
            case "TranslationProviderTitle": return LocaleController.getString("TranslationProviderTitle", R.string.TranslationProviderTitle);
            case "GoogleTranslate": return LocaleController.getString("GoogleTranslate", R.string.GoogleTranslate);
            case "GoogleTranslateDesc": return LocaleController.getString("GoogleTranslateDesc", R.string.GoogleTranslateDesc);
            case "TelegramTranslate": return LocaleController.getString("TelegramTranslate", R.string.TelegramTranslate);
            case "TelegramTranslateDesc": return LocaleController.getString("TelegramTranslateDesc", R.string.TelegramTranslateDesc);
            case "TranslationBehavior": return LocaleController.getString("TranslationBehavior", R.string.TranslationBehavior);
            case "TranslateSelectedMessages": return LocaleController.getString("TranslateSelectedMessages", R.string.TranslateSelectedMessages);
            case "TranslateSelectedMessagesAbout": return LocaleController.getString("TranslateSelectedMessagesAbout", R.string.TranslateSelectedMessagesAbout);
            case "TranslateChatMessages": return LocaleController.getString("TranslateChatMessages", R.string.TranslateChatMessages);
            case "TranslateChatMessagesAbout": return LocaleController.getString("TranslateChatMessagesAbout", R.string.TranslateChatMessagesAbout);
            case "TranslationExceptions": return LocaleController.getString("TranslationExceptions", R.string.TranslationExceptions);
            case "TranslationExceptionsAbout": return LocaleController.getString("TranslationExceptionsAbout", R.string.TranslationExceptionsAbout);
            case "TranslationSettingsInfo": return LocaleController.getString("TranslationSettingsInfo", R.string.TranslationSettingsInfo);
            case "MainScreen": return LocaleController.getString("MainScreen", R.string.MainScreen);
            case "HideStoriesBar": return LocaleController.getString("HideStoriesBar", R.string.HideStoriesBar);
            case "HideStoriesBarAbout": return LocaleController.getString("HideStoriesBarAbout", R.string.HideStoriesBarAbout);
            case "MainScreenSettings": return LocaleController.getString("MainScreenSettings", R.string.MainScreenSettings);
            case "MainScreenSettingsAbout": return LocaleController.getString("MainScreenSettingsAbout", R.string.MainScreenSettingsAbout);
            case "SettingsSections": return LocaleController.getString("SettingsSections", R.string.SettingsSections);
            case "HudgramSettings": return LocaleController.getString("HudgramSettings", R.string.HudgramSettings);
            case "CommonSettings": return LocaleController.getString("CommonSettings", R.string.CommonSettings);
            case "CommonSettingsAbout": return LocaleController.getString("CommonSettingsAbout", R.string.CommonSettingsAbout);
            case "ShowAvatarInHeader": return LocaleController.getString("ShowAvatarInHeader", R.string.ShowAvatarInHeader);
            case "ShowAvatarInHeaderAbout": return LocaleController.getString("ShowAvatarInHeaderAbout", R.string.ShowAvatarInHeaderAbout);
            case "ShowMyNameInHeader": return LocaleController.getString("ShowMyNameInHeader", R.string.ShowMyNameInHeader);
            case "ShowMyNameInHeaderAbout": return LocaleController.getString("ShowMyNameInHeaderAbout", R.string.ShowMyNameInHeaderAbout);
            case "ShowBioAsSubtitle": return LocaleController.getString("ShowBioAsSubtitle", R.string.ShowBioAsSubtitle);
            case "ShowBioAsSubtitleAbout": return LocaleController.getString("ShowBioAsSubtitleAbout", R.string.ShowBioAsSubtitleAbout);
            case "HideSettingsTab": return LocaleController.getString("HideSettingsTab", R.string.HideSettingsTab);
            case "HideSettingsTabAbout": return LocaleController.getString("HideSettingsTabAbout", R.string.HideSettingsTabAbout);
            case "HideSearchBar": return LocaleController.getString("HideSearchBar", R.string.HideSearchBar);
            case "HideSearchBarAbout": return LocaleController.getString("HideSearchBarAbout", R.string.HideSearchBarAbout);
            case "HideFolderTabs": return LocaleController.getString("HideFolderTabs", R.string.HideFolderTabs);
            case "HideFolderTabsAbout": return LocaleController.getString("HideFolderTabsAbout", R.string.HideFolderTabsAbout);
            case "HideNotificationContent": return LocaleController.getString("HideNotificationContent", R.string.HideNotificationContent);
            case "HideNotificationContentAbout": return LocaleController.getString("HideNotificationContentAbout", R.string.HideNotificationContentAbout);
            case "PartialCopy": return LocaleController.getString("PartialCopy", R.string.PartialCopy);
            case "PartialCopyAbout": return LocaleController.getString("PartialCopyAbout", R.string.PartialCopyAbout);
            case "ConfirmStickers": return LocaleController.getString("ConfirmStickers", R.string.ConfirmStickers);
            case "ConfirmStickersAbout": return LocaleController.getString("ConfirmStickersAbout", R.string.ConfirmStickersAbout);
            case "ConfirmVoiceMessages": return LocaleController.getString("ConfirmVoiceMessages", R.string.ConfirmVoiceMessages);
            case "ConfirmVoiceMessagesAbout": return LocaleController.getString("ConfirmVoiceMessagesAbout", R.string.ConfirmVoiceMessagesAbout);
            case "HudSettingsGeneral": return LocaleController.getString("HudSettingsGeneral", R.string.HudSettingsGeneral);
            case "HudSettingsAppearance": return LocaleController.getString("HudSettingsAppearance", R.string.HudSettingsAppearance);
            case "HudSettingsChat": return LocaleController.getString("HudSettingsChat", R.string.HudSettingsChat);
            case "HudSettingsNotifications": return LocaleController.getString("HudSettingsNotifications", R.string.HudSettingsNotifications);
            case "HudSettingsOther": return LocaleController.getString("HudSettingsOther", R.string.HudSettingsOther);
            case "HudOfficialChannel": return LocaleController.getString("HudOfficialChannel", R.string.HudOfficialChannel);
            case "HudSettingsCategories": return LocaleController.getString("HudSettingsCategories", R.string.HudSettingsCategories);
            case "HudAboutAndLinks": return LocaleController.getString("HudAboutAndLinks", R.string.HudAboutAndLinks);
            case "AboutHudgram": return LocaleController.getString("AboutHudgram", R.string.AboutHudgram);
            case "AboutHudgramText": return LocaleController.getString("AboutHudgramText", R.string.AboutHudgramText);
            case "NotificationAppearance": return LocaleController.getString("NotificationAppearance", R.string.NotificationAppearance);
            case "HudQuickReplyTitle": return LocaleController.getString("HudQuickReplyTitle", R.string.HudQuickReplyTitle);
            case "HudQuickReplyRow": return LocaleController.getString("HudQuickReplyRow", R.string.HudQuickReplyRow);
            case "HudQuickReplySettingsInfo": return LocaleController.getString("HudQuickReplySettingsInfo", R.string.HudQuickReplySettingsInfo);
            case "HudQuickReplyLabelHint": return LocaleController.getString("HudQuickReplyLabelHint", R.string.HudQuickReplyLabelHint);
            case "HudQuickReplyValueHint": return LocaleController.getString("HudQuickReplyValueHint", R.string.HudQuickReplyValueHint);
            case "HudQuickReplyDeleteConfirm": return LocaleController.getString("HudQuickReplyDeleteConfirm", R.string.HudQuickReplyDeleteConfirm);
            case "HudQuickReplyDeleteTitle": return LocaleController.getString("HudQuickReplyDeleteTitle", R.string.HudQuickReplyDeleteTitle);
            case "HudQuickReplyEdit": return LocaleController.getString("HudQuickReplyEdit", R.string.HudQuickReplyEdit);
            case "HudQuickReplyAdded": return LocaleController.getString("HudQuickReplyAdded", R.string.HudQuickReplyAdded);
            case "HudQuickReplyUpdated": return LocaleController.getString("HudQuickReplyUpdated", R.string.HudQuickReplyUpdated);
            case "HudQuickReplyDeleted": return LocaleController.getString("HudQuickReplyDeleted", R.string.HudQuickReplyDeleted);
            case "HudQuickReplyEmpty": return LocaleController.getString("HudQuickReplyEmpty", R.string.HudQuickReplyEmpty);
            case "HudQuickReplyFillFields": return LocaleController.getString("HudQuickReplyFillFields", R.string.HudQuickReplyFillFields);
            case "HudQuickReplyInvalidLabel": return LocaleController.getString("HudQuickReplyInvalidLabel", R.string.HudQuickReplyInvalidLabel);
            case "HudQuickReplyDuplicate": return LocaleController.getString("HudQuickReplyDuplicate", R.string.HudQuickReplyDuplicate);
            case "HudQuickReplyAddNew": return LocaleController.getString("HudQuickReplyAddNew", R.string.HudQuickReplyAddNew);
            case "HudQuickReplyEditMode": return LocaleController.getString("HudQuickReplyEditMode", R.string.HudQuickReplyEditMode);
            case "HudQuickReplyEnabled": return LocaleController.getString("HudQuickReplyEnabled", R.string.HudQuickReplyEnabled);
            case "HudQuickReplyDisabled": return LocaleController.getString("HudQuickReplyDisabled", R.string.HudQuickReplyDisabled);
        }
        return key;
    }

    /* Blur */

    private final @Nullable DownscaleScrollableNoiseSuppressor scrollableViewNoiseSuppressor;
    private final @Nullable BlurredBackgroundSourceRenderNode iBlur3SourceGlassFrosted;
    private final @Nullable BlurredBackgroundSourceRenderNode iBlur3SourceGlass;

    private IBlur3Capture iBlur3Capture;
    private boolean iBlur3Invalidated;

    private final ArrayList<RectF> iBlur3Positions = new ArrayList<>();
    private final RectF iBlur3PositionActionBar = new RectF();

    {
        iBlur3Positions.add(iBlur3PositionActionBar);
    }

    private void blur3_InvalidateBlur() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || scrollableViewNoiseSuppressor == null) {
            return;
        }

        final int additionalList = AndroidUtilities.dp(48);
        iBlur3PositionActionBar.set(0, -additionalList, fragmentView.getMeasuredWidth(), actionBar.getMeasuredHeight() + additionalList);

        scrollableViewNoiseSuppressor.setupRenderNodes(iBlur3Positions, 1);
        scrollableViewNoiseSuppressor.invalidateResultRenderNodes(iBlur3Capture, fragmentView.getMeasuredWidth(), fragmentView.getMeasuredHeight());
    }

    protected static class TextSettingsCellFactory extends UItem.UItemFactory<TextSettingsCell> {
        static {
            setup(new TextSettingsCellFactory());
        }

        private Theme.ResourcesProvider resourcesProvider;

        @Override
        public TextSettingsCell createView(Context context, RecyclerListView listView, int currentAccount, int classGuid, Theme.ResourcesProvider resourcesProvider) {
            this.resourcesProvider = resourcesProvider;
            return new TextSettingsCell(context, resourcesProvider);
        }

        @Override
        public void bindView(View view, UItem item, boolean divider, UniversalAdapter adapter, UniversalRecyclerView listView) {
            TextSettingsCell textCell = (TextSettingsCell) view;
            textCell.setCanDisable(true);
            if (!TextUtils.isEmpty(item.textValue)) {
                CharSequence valueText = textCell.getValueTextView().getText();
                boolean animated = !TextUtils.isEmpty(valueText) && !item.textValue.equals(valueText);
                textCell.setTextAndValue(item.text, item.textValue, animated, divider);
            } else {
                textCell.setText(item.text, divider);
            }
            if (item.red) {
                textCell.setTextColor(Theme.getColor(Theme.key_text_RedRegular, resourcesProvider));
            } else if (item.accent) {
                textCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText2, resourcesProvider));
            } else {
                textCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider));
            }
        }

        public static UItem of(int id, CharSequence title) {
            return of(id, title, null);
        }

        public static UItem of(int id, CharSequence title, CharSequence value) {
            UItem item = UItem.ofFactory(TextSettingsCellFactory.class);
            item.id = id;
            item.text = title;
            item.textValue = value;
            return item;
        }
    }

    protected static class TextDetailSettingsCellFactory extends UItem.UItemFactory<TextDetailSettingsCell> {
        static {
            setup(new TextDetailSettingsCellFactory());
        }

        @Override
        public TextDetailSettingsCell createView(Context context, RecyclerListView listView, int currentAccount, int classGuid, Theme.ResourcesProvider resourcesProvider) {
            return new TextDetailSettingsCell(context);
        }

        @Override
        public void bindView(View view, UItem item, boolean divider, UniversalAdapter adapter, UniversalRecyclerView listView) {
            TextDetailSettingsCell textCell = (TextDetailSettingsCell) view;
            textCell.setMultilineDetail(true);
            textCell.setTextAndValue(item.text, item.subtext, divider);
            textCell.setEnabled(item.enabled);
        }

        public static UItem of(int id, CharSequence title, CharSequence subtitle) {
            UItem item = UItem.ofFactory(TextDetailSettingsCellFactory.class);
            item.id = id;
            item.text = title;
            item.subtext = subtitle;
            return item;
        }
    }

    protected static class TextCheckbox2CellFactory extends UItem.UItemFactory<TextCheckbox2Cell> {
        static {
            setup(new TextCheckbox2CellFactory());
        }

        @Override
        public TextCheckbox2Cell createView(Context context, RecyclerListView listView, int currentAccount, int classGuid, Theme.ResourcesProvider resourcesProvider) {
            return new TextCheckbox2Cell(context);
        }

        @Override
        public void bindView(View view, UItem item, boolean divider, UniversalAdapter adapter, UniversalRecyclerView listView) {
            TextCheckbox2Cell textCell = (TextCheckbox2Cell) view;
            if (TextUtils.isEmpty(item.subtext)) {
                textCell.setTextAndCheck(item.text == null ? null : item.text.toString(), item.checked, divider);
            } else {
                textCell.setTextAndValueAndCheck(item.text == null ? null : item.text.toString(), item.subtext == null ? null : item.subtext.toString(), item.checked, true, divider);
            }
        }

        public static UItem of(int id, CharSequence title) {
            UItem item = UItem.ofFactory(TextCheckbox2CellFactory.class);
            item.id = id;
            item.text = title;
            return item;
        }

        public static UItem of(int id, CharSequence title, CharSequence subtext) {
            UItem item = UItem.ofFactory(TextCheckbox2CellFactory.class);
            item.id = id;
            item.text = title;
            item.subtext = subtext;
            return item;
        }
    }
}
