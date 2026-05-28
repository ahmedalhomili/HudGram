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
            case "Connection": return LocaleController.isRTL ? "الاتصال" : "Connection";
            case "PreferIPv6": return LocaleController.isRTL ? "تفضيل IPv6" : "Prefer IPv6";
            case "Translator": return LocaleController.isRTL ? "مترجم" : "Translator";
            case "TranslatorType": return LocaleController.isRTL ? "النوع" : "Type";
            case "TranslatorTypeTG": return LocaleController.isRTL ? "مترجم تيليجرام" : "Telegram Translator";
            case "TranslatorTypeExternal": return LocaleController.isRTL ? "تطبيق خارجي" : "External App";
            case "TranslatorTypeNeko": return LocaleController.isRTL ? "في الرسالة" : "In Message";
            case "TranslatorShowOriginal": return LocaleController.isRTL ? "مع النص الأصلي" : "Show original text";
            case "TranslationProviderShort": return LocaleController.isRTL ? "مزود الخدمة" : "Service Provider";
            case "TranslationTarget": return LocaleController.isRTL ? "اللغة المراد الترجمة إليها" : "Target Language";
            case "TranslationTargetApp": return LocaleController.isRTL ? "لغة التطبيق" : "App Language";
            case "DoNotTranslate": return LocaleController.isRTL ? "عدم ترجمة" : "Do Not Translate";
            case "AutoTranslate": return LocaleController.isRTL ? "ترجمة تلقائية" : "Auto Translate";
            case "AutoTranslateAbout": return LocaleController.isRTL ? "يمكنك ترجمة الرسائل المكتوبة بلغة أخرى تلقائياً." : "Translate messages automatically when received";
            case "TranslateMessagesInfo1": return LocaleController.isRTL ? "سيظهر زر «الترجمة» عند الضغط مرة واحدة على رسالة نصية." : "Settings for incoming and outgoing message translation";
            case "Notifications": return LocaleController.isRTL ? "الإشعارات" : "Notifications";
            case "AccentAsNotificationColor": return LocaleController.isRTL ? "لون الإشعار كلون التطبيق" : "Accent as notification color";
            case "SilenceNonContacts": return LocaleController.isRTL ? "كتم غير جهات الاتصال" : "Silence Non-Contacts";
            case "SilenceNonContactsAbout": return LocaleController.isRTL ? "كتم صوت الإشعارات للرسائل من غير جهات الاتصال" : "Silence notifications for messages from people not in contacts";
            case "UserColorTabProfile": return LocaleController.isRTL ? "الملف الشخصي والأسماء" : "Profile & Name Display";
            case "NameOrder": return LocaleController.isRTL ? "ترتيب الأسماء" : "Name Order";
            case "FirstLast": return LocaleController.isRTL ? "الاسم الأول ثم الأخير" : "First, Last";
            case "LastFirst": return LocaleController.isRTL ? "الاسم الأخير ثم الأول" : "Last, First";
            case "IdType": return LocaleController.isRTL ? "نوع المعرف" : "ID Type";
            case "IdTypeHidden": return LocaleController.isRTL ? "مخفي" : "Hidden";
            case "IdTypeAPI": return LocaleController.isRTL ? "معرف API" : "API ID";
            case "IdTypeBOTAPI": return LocaleController.isRTL ? "معرف Bot API" : "Bot API ID";
            case "IdTypeAbout": return LocaleController.isRTL ? "طريقة عرض معرفات المستخدمين والمجموعات" : "Choose how user and group IDs are displayed";
            case "General": return LocaleController.isRTL ? "عام" : "General";
            case "DisableInstantCamera": return LocaleController.isRTL ? "تعطيل الكاميرا الفورية" : "Disable Instant Camera";
            case "AskBeforeCalling": return LocaleController.isRTL ? "تأكيد قبل الاتصال" : "Confirm before calling";
            case "OpenArchiveOnPull": return LocaleController.isRTL ? "فتح الأرشيف عند السحب" : "Open archive on pull";
            case "RestartAppToTakeEffect": return LocaleController.isRTL ? "يرجى إعادة تشغيل التطبيق لتطبيق التغييرات" : "Restart app to take effect";
            case "Languages": return LocaleController.isRTL ? "اللغات" : "Languages";
            case "CopyLink": return LocaleController.isRTL ? "نسخ الرابط" : "Copy Link";
            case "Search": return LocaleController.isRTL ? "بحث" : "Search";
            case "TranslationSettings": return LocaleController.isRTL ? "إعدادات الترجمة" : "Translation Settings";
            case "TranslationEnabled": return LocaleController.isRTL ? "تفعيل الترجمة" : "Enable Translation";
            case "TranslationEnabledAbout": return LocaleController.isRTL ? "تفعيل أو تعطيل نظام الترجمة بالكامل" : "Enable or disable the entire translation system";
            case "TranslationProviderTitle": return LocaleController.isRTL ? "مزود الترجمة" : "Translation Provider";
            case "GoogleTranslate": return LocaleController.isRTL ? "ترجمة جوجل" : "Google Translate";
            case "GoogleTranslateDesc": return LocaleController.isRTL ? "ترجمة سريعة ودقيقة تدعم أكثر من 100 لغة" : "Fast and accurate translation supporting 100+ languages";
            case "TelegramTranslate": return LocaleController.isRTL ? "ترجمة تيليجرام" : "Telegram Translate";
            case "TelegramTranslateDesc": return LocaleController.isRTL ? "ترجمة مدمجة من تيليجرام باستخدام خوادمهم" : "Built-in Telegram translation using their servers";
            case "TranslationBehavior": return LocaleController.isRTL ? "سلوك الترجمة" : "Translation Behavior";
            case "TranslateSelectedMessages": return LocaleController.isRTL ? "ترجمة الرسائل المحددة" : "Translate Selected Messages";
            case "TranslateSelectedMessagesAbout": return LocaleController.isRTL ? "إظهار زر الترجمة عند الضغط على رسالة" : "Show translate button when selecting a message";
            case "TranslateChatMessages": return LocaleController.isRTL ? "ترجمة المحادثة" : "Translate Chat";
            case "TranslateChatMessagesAbout": return LocaleController.isRTL ? "إظهار زر ترجمة المحادثة في الأعلى" : "Show translate chat button at the top";
            case "TranslationExceptions": return LocaleController.isRTL ? "الاستثناءات" : "Exceptions";
            case "TranslationExceptionsAbout": return LocaleController.isRTL ? "اللغات التي لن يتم ترجمتها تلقائياً" : "Languages that will not be automatically translated";
            case "TranslationSettingsInfo": return LocaleController.isRTL ? "تتحكم هذه الإعدادات في كيفية ترجمة الرسائل في جميع المحادثات" : "These settings control how messages are translated across all chats";
            case "MainScreen": return LocaleController.isRTL ? "الشاشة الرئيسية" : "Main Screen";
            case "HideStoriesBar": return LocaleController.isRTL ? "إخفاء شريط القصص" : "Hide Stories Bar";
            case "HideStoriesBarAbout": return LocaleController.isRTL ? "إخفاء أو إظهار شريط القصص أعلى الشاشة الرئيسية" : "Show or hide the stories bar at the top of the main screen";
            case "MainScreenSettings": return LocaleController.isRTL ? "إعدادات الشاشة الرئيسية" : "Main Screen Settings";
            case "MainScreenSettingsAbout": return LocaleController.isRTL ? "تخصيص شريط القصص، القوائم، والمظهر الخاص بالشاشة الرئيسية" : "Customize stories bar, menus, and layout of the main screen";
            case "SettingsSections": return LocaleController.isRTL ? "أقسام الإعدادات" : "Settings Sections";
            case "HudgramSettings": return LocaleController.isRTL ? "إعدادات هدهد جرام" : "Hudgram Settings";
            case "CommonSettings": return LocaleController.isRTL ? "الإعدادات العامة" : "General Settings";
            case "CommonSettingsAbout": return LocaleController.isRTL ? "تخصيص الكاميرا، تأكيد الاتصال، وتفاعلات الأرشيف" : "Customize camera, call confirmation, and archive interactions";
            case "ShowAvatarInHeader": return LocaleController.isRTL ? "عرض الملف الشخصي في العنوان" : "Show Profile in Header";
            case "ShowAvatarInHeaderAbout": return LocaleController.isRTL ? "إخفاء صورة الملف الشخصي من الشريط السفلي وعرضها بشكل دائري في شريط العنوان بالأعلى" : "Hide profile avatar from bottom bar and show it in the top title bar";
            case "ShowMyNameInHeader": return LocaleController.isRTL ? "عرض اسمي بدل عنوان هدهدجرام" : "Show my name instead of Hudgram";
            case "ShowMyNameInHeaderAbout": return LocaleController.isRTL ? "عرض اسمك الأول والأخير في شريط العنوان بالأعلى بدلاً من شعار أو كلمة Hudgram" : "Show your first and last name in the top title bar instead of Hudgram title";
            case "ShowBioAsSubtitle": return LocaleController.isRTL ? "عرض البايو كعنوان فرعي" : "Show bio as subtitle";
            case "ShowBioAsSubtitleAbout": return LocaleController.isRTL ? "عرض السيرة الذاتية (Bio) الخاصة بك كعنوان فرعي صغير أسفل اسمك في شريط العنوان" : "Show your biography (Bio) as a small subtitle text below your name in the title bar";
            case "HideSettingsTab": return LocaleController.isRTL ? "إخفاء تبويب الإعدادات" : "Hide Settings Tab";
            case "HideSettingsTabAbout": return LocaleController.isRTL ? "إخفاء زر الإعدادات من الشريط السفلي ونقله إلى قائمة الثلاث نقاط بالأعلى" : "Hide the Settings button from the bottom navigation bar and move it to the top 3-dots menu";
            case "HideSearchBar": return LocaleController.isRTL ? "إخفاء شريط البحث" : "Hide Search Bar";
            case "HideSearchBarAbout": return LocaleController.isRTL ? "إخفاء شريط البحث في قائمة المحادثات مع إبقاء أيقونة البحث في الأعلى" : "Hide the search bar in the chats list but keep the search icon in the action bar";
            case "HideFolderTabs": return LocaleController.isRTL ? "إخفاء شريط المجلدات" : "Hide Folder Tabs";
            case "HideFolderTabsAbout": return LocaleController.isRTL ? "إخفاء شريط التبويبات والمجلدات أسفل شريط العنوان" : "Hide folder and tab capsules under the title bar";
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
