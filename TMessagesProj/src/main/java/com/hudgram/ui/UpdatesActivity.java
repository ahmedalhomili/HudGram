package com.hudgram.ui;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;

import org.telegram.messenger.SharedConfig;
import org.telegram.ui.Components.SwipeGestureSettingsView;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_stories;

// Native Telegram UI packages
import org.telegram.ui.*;
import org.telegram.ui.ActionBar.*;
import org.telegram.ui.Cells.DialogCell;
import org.telegram.ui.Components.*;
import org.telegram.ui.Stories.*;
import org.telegram.ui.Stories.recorder.*;
import org.telegram.ui.Components.Premium.LimitReachedBottomSheet;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.ChatObject;

// Custom cells under the com.hudgram.ui.cells package
import com.hudgram.ui.cells.UpdatesStoryCell;

import android.os.Bundle;
import android.widget.ImageView;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.view.accessibility.AccessibilityNodeInfo;
import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationsController;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLObject;

import java.util.ArrayList;
import java.util.Collections;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

public class UpdatesActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private RecyclerListView listView;
    private UpdatesAdapter adapter;
    private LinearLayout storiesContainer;
    private RecyclerListView storiesRecyclerView;
    private StoriesAdapter storiesAdapter;
    private ItemTouchHelper itemTouchHelper;
    private boolean hasMainTabs;
    private boolean searching;
    private String searchQuery;
    private View blurredView;
    private UndoView undoView;
    private StickerEmptyView noChannelsView;
    private BackupImageView headerAvatarView;
    private AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable statusDrawable;
    private FragmentFloatingButton cameraFab;
    private FragmentFloatingButton liveFab;
    private TextView statusHeader;

    public UpdatesActivity(android.os.Bundle args) {
        super(args);
    }

    private static final int TYPE_STORIES_PLACEHOLDER = 0;
    private static final int TYPE_HEADER = 1;
    private static final int TYPE_CHANNEL = 2;
    private static final int TYPE_EMPTY = 3;
    private final java.util.HashSet<Long> selectedDialogIds = new java.util.HashSet<>();
    private boolean inSelectionMode = false;
    private org.telegram.ui.Components.NumberTextView selectedDialogsCountTextView;
    private org.telegram.ui.ActionBar.ActionBarMenuItem muteItem;
    private org.telegram.ui.ActionBar.ActionBarMenuItem archiveItem;
    private org.telegram.ui.ActionBar.ActionBarMenuItem deleteItem;
    private org.telegram.ui.ActionBar.ActionBarMenuSubItem pinItem;
    private org.telegram.ui.ActionBar.ActionBarMenuSubItem readItem;
    private org.telegram.ui.ActionBar.ActionBarMenuSubItem copyLinkItem;
    private org.telegram.ui.ActionBar.ActionBarMenuSubItem viewChannelItem;
    private org.telegram.ui.ActionBar.ActionBarMenuItem themeToggleItem;
    private org.telegram.ui.ActionBar.ActionBarMenuItem otherItem;
    private boolean showArchivedChannels = false;
    private TextView channelsArchiveButton;
    private TextView channelsHeaderTextView;

    private final ArrayList<TL_stories.PeerStories> storyItems = new ArrayList<>();
    private final ArrayList<TLRPC.Dialog> channelDialogs = new ArrayList<>();

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        if (getArguments() != null) {
            hasMainTabs = getArguments().getBoolean("hasMainTabs", false);
        }

        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.storiesUpdated);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.dialogsNeedReload);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.updateInterfaces);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.mainUserInfoChanged);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.userInfoDidLoad);

        loadData();
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.storiesUpdated);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.dialogsNeedReload);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.updateInterfaces);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.mainUserInfoChanged);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.userInfoDidLoad);
    }

    private void loadData() {
        // Load stories
        storyItems.clear();
        StoriesController storiesController = MessagesController.getInstance(currentAccount).getStoriesController();
        ArrayList<TL_stories.PeerStories> allStories = showArchivedChannels ? storiesController.getHiddenList() : storiesController.getDialogListStories();
        if (allStories != null) {
            long selfId = UserConfig.getInstance(currentAccount).getClientUserId();
            for (TL_stories.PeerStories ps : allStories) {
                // Skip self stories (already shown in the self card)
                long peerId = getDialogIdFromPeerStories(ps);
                if (peerId != selfId) {
                    if (searching && searchQuery != null && !searchQuery.isEmpty()) {
                        String name = "";
                        if (peerId > 0) {
                            TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(peerId);
                            if (user != null) {
                                name = (user.first_name != null ? user.first_name : "") + " " + (user.last_name != null ? user.last_name : "");
                            }
                        } else {
                            TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-peerId);
                            if (chat != null) {
                                name = chat.title != null ? chat.title : "";
                            }
                        }
                        if (!name.toLowerCase().contains(searchQuery.toLowerCase())) {
                            continue;
                        }
                    }
                    storyItems.add(ps);
                }
            }
        }

        // Load channels
        channelDialogs.clear();
        ArrayList<TLRPC.Dialog> channels = MessagesController.getInstance(currentAccount).dialogsChannelsOnly;
        if (channels != null) {
            for (TLRPC.Dialog dialog : channels) {
                if (showArchivedChannels) {
                    if (dialog.folder_id != 1) {
                        continue; // Skip active channels if showing archived
                    }
                } else {
                    if (dialog.folder_id == 1) {
                        continue; // Skip archived channels if showing active
                    }
                }
                if (searching && searchQuery != null && !searchQuery.isEmpty()) {
                    TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-dialog.id);
                    String title = chat != null && chat.title != null ? chat.title : "";
                    if (!title.toLowerCase().contains(searchQuery.toLowerCase())) {
                        continue;
                    }
                }
                channelDialogs.add(dialog);
            }
        }
    }

    @Override
    public View createView(Context context) {
        hasOwnBackground = true;

        // Action bar
        actionBar.setTitle(getString(R.string.MainTabsUpdates));
        actionBar.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        actionBar.setTitleColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        actionBar.setSubtitleColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        actionBar.setItemsColor(Theme.getColor(Theme.key_actionBarDefaultIcon), false);
        actionBar.setItemsBackgroundColor(Theme.getColor(Theme.key_actionBarDefaultSelector), false);
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        if (actionBar.getBackButton() != null) {
            actionBar.getBackButton().setVisibility(View.GONE);
        }
        if (LocaleController.isRTL) {
            actionBar.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        } else {
            actionBar.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        }

        headerAvatarView = com.hudgram.ui.HudUiHelper.createHeaderAvatarView(context, parentLayout);
        actionBar.addView(headerAvatarView, LayoutHelper.createFrame(36, 36, Gravity.LEFT | Gravity.CENTER_VERTICAL, 14, 0, 0, 0));

        actionBar.setActionBarMenuOnItemClick(new org.telegram.ui.ActionBar.ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    if (inSelectionMode) {
                        hideActionMode();
                    } else if (searching) {
                        actionBar.closeSearchField();
                    } else {
                        finishFragment();
                    }
                } else if (id == 1) { // Status Privacy
                    try {
                        org.telegram.ui.Stories.recorder.StoryPrivacyBottomSheet sheet = 
                            new org.telegram.ui.Stories.recorder.StoryPrivacyBottomSheet(getContext(), 86400, getResourceProvider());
                        showDialog(sheet);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (id == 2) { // New Channel
                    Bundle args = new Bundle();
                    presentFragment(new ChannelCreateActivity(args));
                } else if (id == 3) { // Archived Stories
                    Bundle args = new Bundle();
                    args.putLong("dialog_id", getUserConfig().getClientUserId());
                    args.putInt("type", MediaActivity.TYPE_STORIES);
                    args.putInt("start_from", SharedMediaLayout.TAB_ARCHIVED_STORIES);
                    presentFragment(new MediaActivity(args, null));
                } else if (id == 4) { // Saved Stories
                    Bundle args = new Bundle();
                    args.putLong("dialog_id", getUserConfig().getClientUserId());
                    args.putInt("type", MediaActivity.TYPE_STORIES);
                    presentFragment(new MediaActivity(args, null));
                } else if (id == 5) { // Hudgram Settings
                    presentFragment(new com.hudgram.ui.HudGeneralSettingsActivity());
                } else if (id == 101) { // Mute
                    toggleMuteSelectedChannels();
                } else if (id == 102) { // Archive
                    archiveSelectedChannels();
                } else if (id == 103) { // Delete
                    deleteSelectedChannels();
                } else if (id == 105) { // Copy link
                    copySelectedChannelLink();
                } else if (id == 201) { // Pin/Unpin
                    pinOrUnpinSelectedChannels();
                } else if (id == 202) { // Add to folder
                    addToFolderSelectedChannels();
                } else if (id == 203) { // Mark as read/unread
                    markSelectedChannelsReadOrUnread();
                } else if (id == 204) { // Clear Cache
                    deleteCacheSelectedChannels();
                } else if (id == 205) { // Select all
                    selectAllChannels();
                } else if (id == 206) { // Channel Info
                    openSelectedChannelInfo();
                } else if (id == 207) { // Unfollow
                    unfollowSelectedChannels();
                }
            }
        });

        org.telegram.ui.ActionBar.ActionBarMenu menu = actionBar.createMenu();
        
        // 1. Search icon
        org.telegram.ui.ActionBar.ActionBarMenuItem searchItem = menu.addItem(20, R.drawable.outline_header_search).setIsSearchField(true);
        searchItem.setSearchFieldHint(LocaleController.getString(R.string.Search));
        searchItem.setActionBarMenuItemSearchListener(new org.telegram.ui.ActionBar.ActionBarMenuItem.ActionBarMenuItemSearchListener() {
            @Override
            public void onSearchExpand() {
                searching = true;
                if (actionBar.getBackButton() != null) {
                    actionBar.getBackButton().setVisibility(View.VISIBLE);
                    actionBar.getBackButton().setAlpha(0f);
                    actionBar.getBackButton().animate().alpha(1f).setDuration(150).start();
                    actionBar.getBackButton().setOnClickListener(v -> {
                        actionBar.closeSearchField();
                    });
                }
                updateHeaderAvatar();
            }

            @Override
            public void onSearchCollapse() {
                searching = false;
                searchQuery = null;
                if (actionBar.getBackButton() != null) {
                    actionBar.getBackButton().animate().alpha(0f).setDuration(150).withEndAction(() -> {
                        actionBar.getBackButton().setVisibility(View.GONE);
                    }).start();
                    actionBar.getBackButton().setOnClickListener(v -> {
                        if (actionBar.getActionBarMenuOnItemClick() != null) {
                            actionBar.getActionBarMenuOnItemClick().onItemClick(-1);
                        }
                    });
                }
                updateUI();
            }

            @Override
            public void onTextChanged(android.widget.EditText editText) {
                searchQuery = editText.getText().toString();
                updateUI();
            }
        });

        // 2. Theme toggle switcher (replacing camera icon)
        boolean isDark = getResourceProvider() != null ? getResourceProvider().isDark() : Theme.isCurrentThemeDark();
        themeToggleItem = menu.addItem(10, isDark ? R.drawable.menu_day_mode_24 : R.drawable.menu_night_mode_24);
        themeToggleItem.setContentDescription(getString(isDark ? R.string.SwitchThemeToDay : R.string.SwitchThemeToNight));
        themeToggleItem.setOnClickListener(v -> {
            com.hudgram.ui.HudUiHelper.toggleTheme(this, this::switchTheme);
        });

        // 3. Three-dots icon
        otherItem = menu.addItem(30, R.drawable.ic_ab_other);
        otherItem.addSubItem(1, R.drawable.msg_secret, LocaleController.getString(R.string.StoryPrivacyAlertEditTitle));
        otherItem.addSubItem(2, R.drawable.msg_channel, LocaleController.getString(R.string.NewChannel));
        otherItem.addSubItem(3, R.drawable.msg_stories_archive, LocaleController.getString(R.string.ArchivedStories));
        otherItem.addSubItem(4, R.drawable.msg_stories_saved, LocaleController.getString(R.string.SavedStories));
        otherItem.addSubItem(5, R.drawable.msg_customize, LocaleController.isRTL ? "إعدادات هدهد جرام" : "Hudgram Settings");

        statusDrawable = new AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable(null, dp(26));
        statusDrawable.center = true;

        // Main container
        FrameLayout rootLayout = new FrameLayout(context) {
            @Override
            protected void onAttachedToWindow() {
                super.onAttachedToWindow();
                if (statusDrawable != null) {
                    statusDrawable.attach();
                }
            }

            @Override
            protected void onDetachedFromWindow() {
                super.onDetachedFromWindow();
                if (statusDrawable != null) {
                    statusDrawable.detach();
                }
            }
        };
        rootLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        if (LocaleController.isRTL) {
            rootLayout.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        } else {
            rootLayout.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        }

        // RecyclerListView initialization
        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setClipToPadding(false);
        adapter = new UpdatesAdapter(context);
        listView.setAdapter(adapter);
        rootLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listView.setOnItemClickListener((view, position, x, y) -> {
            if (position < 2) {
                return;
            }
            int dialogIndex = position - 2;
            if (dialogIndex >= 0 && dialogIndex < channelDialogs.size()) {
                TLRPC.Dialog dialog = channelDialogs.get(dialogIndex);
                if (inSelectionMode) {
                    toggleSelection(dialog.id);
                } else {
                    presentFragment(ChatActivity.of(dialog.id));
                }
            }
        });

        listView.setOnItemLongClickListener(new RecyclerListView.OnItemLongClickListenerExtended() {
            @Override
            public boolean onItemClick(View view, int position, float x, float y) {
                if (position < 2) {
                    return false;
                }
                int dialogIndex = position - 2;
                if (dialogIndex >= 0 && dialogIndex < channelDialogs.size()) {
                    TLRPC.Dialog dialog = channelDialogs.get(dialogIndex);
                    if (view instanceof DialogCell) {
                        DialogCell cell = (DialogCell) view;
                        if (cell.isPointInsideAvatar(x, y)) {
                            if (!inSelectionMode) {
                                return showChatPreview(cell);
                            }
                        } else {
                            toggleSelection(dialog.id);
                            return true;
                        }
                    }
                }
                return false;
            }

            @Override
            public void onMove(float dx, float dy) {
            }

            @Override
            public void onLongClickRelease() {
            }
        });

        // Floating storiesContainer
        storiesContainer = new LinearLayout(context);
        storiesContainer.setOrientation(LinearLayout.VERTICAL);
        storiesContainer.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        
        statusHeader = createSectionHeader(context, getString(R.string.UpdatesStatusHeader));
        storiesContainer.addView(statusHeader, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 16, 0, 8));

        // Horizontal stories RecyclerView
        storiesRecyclerView = new RecyclerListView(context);
        storiesRecyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        if (LocaleController.isRTL) {
            storiesRecyclerView.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        } else {
            storiesRecyclerView.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        }
        storiesRecyclerView.setClipToPadding(false);
        storiesRecyclerView.setPadding(dp(12), 0, dp(12), 0);
        storiesRecyclerView.setSelectorRadius(dp(14)); // match card radius
        storiesRecyclerView.setSelectorDrawableColor(Theme.getColor(Theme.key_listSelector));
        storiesAdapter = new StoriesAdapter(context);
        storiesRecyclerView.setAdapter(storiesAdapter);
        storiesRecyclerView.setOnItemClickListener((view, position) -> {
            if (position >= 0 && position < (showArchivedChannels ? storyItems.size() : storyItems.size() + 1)) {
                long dialogId;
                if (!showArchivedChannels && position == 0) {
                    dialogId = UserConfig.getInstance(currentAccount).getClientUserId();
                } else {
                    dialogId = getDialogIdFromPeerStories(storyItems.get(showArchivedChannels ? position : position - 1));
                }

                StoriesController storiesController = MessagesController.getInstance(currentAccount).getStoriesController();
                if (!showArchivedChannels && position == 0) {
                    if (storiesController.hasSelfStories() || !Utilities.isNullOrEmpty(storiesController.getUploadingStories(dialogId))) {
                        openStoryViewer(dialogId);
                    } else {
                        if (getParentActivity() != null) {
                            final StoriesController.StoryLimit storyLimit = storiesController.checkStoryLimit();
                            if (storyLimit != null && storyLimit.active(currentAccount)) {
                                showDialog(new LimitReachedBottomSheet(this, getContext(), storyLimit.getLimitReachedType(), currentAccount, null));
                            } else {
                                StoryRecorder.getInstance(getParentActivity(), currentAccount).open(null);
                            }
                        }
                    }
                } else {
                    if (storiesController.hasStories(dialogId) || !Utilities.isNullOrEmpty(storiesController.getUploadingStories(dialogId)) || showArchivedChannels) {
                        openStoryViewer(dialogId);
                    }
                }
            }
        });
        storiesRecyclerView.setOnItemLongClickListener((view, position) -> {
            if (position >= 0 && position < (showArchivedChannels ? storyItems.size() : storyItems.size() + 1)) {
                long dialogId;
                if (!showArchivedChannels && position == 0) {
                    dialogId = UserConfig.getInstance(currentAccount).getClientUserId();
                } else {
                    dialogId = getDialogIdFromPeerStories(storyItems.get(showArchivedChannels ? position : position - 1));
                }
                showStoryItemMenu(view, dialogId);
            }
            return true;
        });

        storiesContainer.addView(storiesRecyclerView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 140, 0, 0, 0, 8));
        rootLayout.addView(storiesContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP));
        storiesContainer.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            int oldHeight = oldBottom - oldTop;
            int newHeight = bottom - top;
            if (oldHeight != newHeight && adapter != null) {
                adapter.notifyItemChanged(0);
            }
        });

        // Add action bar after list and stories container to draw it on top
        rootLayout.addView(actionBar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // No channels placeholder
        noChannelsView = new StickerEmptyView(context, null, StickerEmptyView.STICKER_TYPE_NO_CONTACTS, getResourceProvider());
        noChannelsView.title.setText(LocaleController.isRTL ? "لا توجد قنوات بعد" : "No channels yet");
        noChannelsView.setSubtitle(LocaleController.isRTL ? "يمكنك إنشاء قناة جديدة أو البحث عن قنوات لمتابعتها." : "You can create a new channel or search for channels to follow.");
        noChannelsView.createButtonLayout(LocaleController.isRTL ? "إنشاء قناة" : "Create Channel", () -> {
            Bundle args = new Bundle();
            presentFragment(new ChannelCreateActivity(args));
        });

        // Blurred view for premium chat previews
        blurredView = new View(context);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            blurredView.setForeground(new android.graphics.drawable.ColorDrawable(ColorUtils.setAlphaComponent(Theme.getColor(Theme.key_windowBackgroundWhite), 100)));
        }
        blurredView.setVisibility(View.GONE);
        blurredView.setFocusable(false);
        blurredView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        blurredView.setOnClickListener(e -> {
            finishPreviewFragment();
        });

        // === Telegram-style floating action buttons ===
        int bottomTabsHeight = DialogsActivity.MAIN_TABS_HEIGHT_WITH_MARGINS;

        // 1. Camera Floating Button (Bottom - Main)
        cameraFab = new FragmentFloatingButton(context, getResourceProvider());
        cameraFab.setImageResource(R.drawable.msg_camera);
        cameraFab.setOnClickListener(v -> {
            if (getParentActivity() != null) {
                StoryRecorder.getInstance(getParentActivity(), currentAccount).open(null);
            }
        });

        rootLayout.addView(cameraFab, LayoutHelper.createFrame(48, 48, (LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT) | Gravity.BOTTOM, 20, 0, 20, 80));

        // 2. Live Stream Floating Button (Above Camera - Sub)
        liveFab = new FragmentFloatingButton(context, getResourceProvider(), true);
        liveFab.setImageResource(R.drawable.media_live_on);
        liveFab.setOnClickListener(v -> {
            if (getParentActivity() != null) {
                StoryRecorder.getInstance(getParentActivity(), currentAccount)
                        .setMode(StoryRecorder.MODE_LIVE)
                        .open(null);
            }
        });

        rootLayout.addView(liveFab, LayoutHelper.createFrame(48, 48, (LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT) | Gravity.BOTTOM, 20, 0, 20, 70 + 48 + 16));

        // Auto-hide buttons and animate stories container on scroll
        listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                updateStoriesScroll();
                if (dy > dp(4)) {
                    cameraFab.setButtonVisible(false, true);
                    liveFab.setButtonVisible(false, true);
                } else if (dy < -dp(4) || getStoriesScrollY() <= 0) {
                    cameraFab.setButtonVisible(true, true);
                    liveFab.setButtonVisible(true, true);
                }
            }
        });

        // Undo view for actions
        undoView = new UndoView(context);
        rootLayout.addView(undoView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM | Gravity.LEFT, 8, 0, 8, 8));

        fragmentView = rootLayout;

        ViewCompat.setOnApplyWindowInsetsListener(fragmentView, (v, insets) -> {
            int top = AndroidUtilities.getStatusBarHeight(context) + ActionBar.getCurrentActionBarHeight();
            int bottomPadding = insets.getSystemWindowInsetBottom() + dp(DialogsActivity.MAIN_TABS_HEIGHT_WITH_MARGINS);
            listView.setPadding(0, top, 0, bottomPadding);
            storiesContainer.setTranslationY(top);
            updateStoriesScroll();
            return insets;
        });

        // Attach Swipe Gestures
        SwipeController swipeController = new SwipeController();
        itemTouchHelper = new ItemTouchHelper(swipeController);
        itemTouchHelper.attachToRecyclerView(listView);

        updateUI();

        return fragmentView;
    }

    private void openStoryViewer(long startFromDialogId) {
        if (getParentActivity() == null) return;

        StoriesController storiesController = MessagesController.getInstance(currentAccount).getStoriesController();
        
        ArrayList<Long> ids = new ArrayList<>();
        if (!showArchivedChannels && (storiesController.hasSelfStories() || !Utilities.isNullOrEmpty(storiesController.getUploadingStories(UserConfig.getInstance(currentAccount).getClientUserId())))) {
            ids.add(UserConfig.getInstance(currentAccount).getClientUserId());
        }
        for (TL_stories.PeerStories ps : storyItems) {
            long id = getDialogIdFromPeerStories(ps);
            if (showArchivedChannels || id != UserConfig.getInstance(currentAccount).getClientUserId()) {
                ids.add(id);
            }
        }
        
        int position = ids.indexOf(startFromDialogId);
        if (position < 0) {
            return;
        }

        ArrayList<Long> peerIds = new ArrayList<>();
        boolean allStoriesIsRead = true;

        for (int i = 0; i < ids.size(); i++) {
            long dialogId = ids.get(i);
            if (dialogId != UserConfig.getInstance(currentAccount).getClientUserId() && storiesController.hasUnreadStories(dialogId)) {
                allStoriesIsRead = false;
                break;
            }
        }

        boolean onlySelfStories = false;
        boolean onlyUnreadStories = false;
        boolean isSelf = startFromDialogId == UserConfig.getInstance(currentAccount).getClientUserId();
        
        if (isSelf && (!allStoriesIsRead || ids.size() == 1)) {
            peerIds.add(startFromDialogId);
            onlySelfStories = true;
            position = 0;
        } else {
            boolean isUnreadStory = !isSelf && storiesController.hasUnreadStories(startFromDialogId);
            if (isUnreadStory) {
                onlyUnreadStories = true;
                for (int i = 0; i < ids.size(); i++) {
                    long dialogId = ids.get(i);
                    if (dialogId != UserConfig.getInstance(currentAccount).getClientUserId() && storiesController.hasUnreadStories(dialogId)) {
                        peerIds.add(dialogId);
                    }
                    if (dialogId == startFromDialogId) {
                        position = peerIds.size() - 1;
                    }
                }
            } else {
                for (int i = 0; i < ids.size(); i++) {
                    long dialogId = ids.get(i);
                    if (storiesController.hasStories(dialogId) || !Utilities.isNullOrEmpty(storiesController.getUploadingStories(dialogId))) {
                        peerIds.add(dialogId);
                    } else if (i <= position) {
                        position--;
                    }
                }
            }
        }
        
        if (peerIds.isEmpty()) {
            return;
        }
        
        StoryViewer storyViewer = getOrCreateStoryViewer();
        storyViewer.doOnAnimationReady(() -> storiesController.setLoading(startFromDialogId, false));
        boolean finalOnlySelfStories = onlySelfStories;
        
        storyViewer.open(getContext(), null, peerIds, position, null, null, StoriesListPlaceProvider.of(storiesRecyclerView).with(forward -> {
            if (finalOnlySelfStories) {
                return;
            }
            if (forward) {
                storiesController.loadNextStories(showArchivedChannels);
            }
        }), false);
    }

    private void showStoryItemMenu(View view, long dialogId) {
        if (getParentActivity() == null) return;
        
        org.telegram.ui.Components.ItemOptions options = org.telegram.ui.Components.ItemOptions.makeOptions(this, view)
            .setScrimViewBackground(Theme.createRoundRectDrawable(
                dp(14),
                dp(14),
                Theme.getColor(Theme.key_windowBackgroundWhite)
            ));
            
        StoriesController storiesController = MessagesController.getInstance(currentAccount).getStoriesController();
        boolean storiesEnabled = MessagesController.getInstance(currentAccount).storiesEnabled();
        
        if (dialogId == UserConfig.getInstance(currentAccount).getClientUserId()) {
            if (!storiesEnabled) {
                return;
            }
            options.add(R.drawable.msg_stories_add, LocaleController.getString(R.string.AddStory), Theme.key_actionBarDefaultSubmenuItemIcon, Theme.key_actionBarDefaultSubmenuItem, () -> {
                if (getParentActivity() != null) {
                    StoryRecorder.getInstance(getParentActivity(), currentAccount).open(null);
                }
            });
            options.add(R.drawable.msg_stories_archive, LocaleController.getString(R.string.ArchivedStories), Theme.key_actionBarDefaultSubmenuItemIcon, Theme.key_actionBarDefaultSubmenuItem, () -> {
                Bundle args = new Bundle();
                args.putLong("dialog_id", UserConfig.getInstance(currentAccount).getClientUserId());
                args.putInt("type", MediaActivity.TYPE_STORIES);
                args.putInt("start_from", SharedMediaLayout.TAB_ARCHIVED_STORIES);
                presentFragment(new MediaActivity(args, null));
            });
            options.add(R.drawable.msg_stories_saved, LocaleController.getString(R.string.SavedStories), Theme.key_actionBarDefaultSubmenuItemIcon, Theme.key_actionBarDefaultSubmenuItem, () -> {
                Bundle args = new Bundle();
                args.putLong("dialog_id", UserConfig.getInstance(currentAccount).getClientUserId());
                args.putInt("type", MediaActivity.TYPE_STORIES);
                presentFragment(new MediaActivity(args, null));
            });
        } else {
            TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(dialogId);
            TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-dialogId);
            final String key = NotificationsController.getSharedPrefKey(dialogId, 0);
            boolean muted = !NotificationsCustomSettingsActivity.areStoriesNotMuted(currentAccount, dialogId);
            boolean isPremiumBlocked = MessagesController.getInstance(currentAccount).premiumFeaturesBlocked();
            boolean isPremium = UserConfig.getInstance(currentAccount).isPremium();
            boolean isUnread = storiesController.hasUnreadStories(dialogId);
            boolean isLive = storiesController.hasLiveStory(dialogId);
            
            CombinedDrawable stealthModeLockedDrawable = null;
            if (!isPremiumBlocked && dialogId > 0 && !isPremium) {
                Drawable lockIcon = ContextCompat.getDrawable(getParentActivity(), R.drawable.msg_gallery_locked2);
                if (lockIcon != null) {
                    Drawable stealthDrawable = ContextCompat.getDrawable(getParentActivity(), R.drawable.msg_stealth_locked);
                    if (stealthDrawable != null) {
                        stealthDrawable.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_actionBarDefaultSubmenuItemIcon), PorterDuff.Mode.MULTIPLY));
                    }
                    lockIcon.setColorFilter(new PorterDuffColorFilter(ColorUtils.blendARGB(Color.WHITE, Color.BLACK, 0.5f), PorterDuff.Mode.MULTIPLY));
                    stealthModeLockedDrawable = new CombinedDrawable(stealthDrawable, lockIcon);
                }
            }
            if (dialogId < 0 && storiesController.canPostStories(dialogId)) {
                options.add(R.drawable.msg_stories_add, LocaleController.getString(R.string.AddStory), Theme.key_actionBarDefaultSubmenuItemIcon, Theme.key_actionBarDefaultSubmenuItem, () -> {
                    StoryRecorder.getInstance(getParentActivity(), currentAccount)
                        .selectedPeerId(dialogId)
                        .canChangePeer(false)
                        .open(null);
                });
            }
            final boolean fromTopPeer = user != null && !user.contact && MediaDataController.getInstance(currentAccount).containsTopPeer(dialogId);
            
            options.addIf(dialogId > 0, R.drawable.msg_discussion, LocaleController.getString(R.string.SendMessage), () -> {
                presentFragment(ChatActivity.of(dialogId));
            });
            options.addIf(dialogId > 0, R.drawable.msg_openprofile, LocaleController.getString(R.string.OpenProfile), () -> {
                presentFragment(ProfileActivity.of(dialogId));
            });
            options.addIf(dialogId < 0, R.drawable.msg_channel, LocaleController.getString(ChatObject.isChannelAndNotMegaGroup(chat) ? R.string.OpenChannel2 : R.string.OpenGroup2), () -> {
                presentFragment(ChatActivity.of(dialogId));
            });
            options.addIf(!muted && dialogId > 0, R.drawable.msg_mute, LocaleController.getString(R.string.NotificationsStoryMute2), () -> {
                MessagesController.getNotificationsSettings(currentAccount).edit().putBoolean("stories_" + key, false).apply();
                NotificationsController.getInstance(currentAccount).updateServerNotificationsSettings(dialogId, 0);
                String name = user == null ? "" : user.first_name.trim();
                int index = name.indexOf(" ");
                if (index > 0) {
                    name = name.substring(0, index);
                }
                BulletinFactory.of(this).createUsersBulletin(java.util.Arrays.asList(user), AndroidUtilities.replaceTags(LocaleController.formatString("NotificationsStoryMutedHint", R.string.NotificationsStoryMutedHint, name))).show();
            });
            options.addIf(muted && dialogId > 0, R.drawable.msg_unmute, LocaleController.getString(R.string.NotificationsStoryUnmute2), () -> {
                MessagesController.getNotificationsSettings(currentAccount).edit().putBoolean("stories_" + key, true).apply();
                NotificationsController.getInstance(currentAccount).updateServerNotificationsSettings(dialogId, 0);
                String name = user == null ? "" : user.first_name.trim();
                int index = name.indexOf(" ");
                if (index > 0) {
                    name = name.substring(0, index);
                }
                BulletinFactory.of(this).createUsersBulletin(java.util.Arrays.asList(user), AndroidUtilities.replaceTags(LocaleController.formatString("NotificationsStoryUnmutedHint", R.string.NotificationsStoryUnmutedHint, name))).show();
            });
            options.addIf(!isPremiumBlocked && dialogId > 0 && isPremium && isUnread && !isLive, R.drawable.msg_stories_stealth2, LocaleController.getString(R.string.ViewAnonymously), () -> {
                TL_stories.TL_storiesStealthMode stealthMode = storiesController.getStealthMode();
                if (stealthMode != null && org.telegram.tgnet.ConnectionsManager.getInstance(currentAccount).getCurrentTime() < stealthMode.active_until_date) {
                    openStoryViewer(dialogId);
                } else {
                    StealthModeAlert stealthModeAlert = new StealthModeAlert(getContext(), 0, StealthModeAlert.TYPE_FROM_DIALOGS, getResourceProvider());
                    stealthModeAlert.setListener(isStealthModeEnabled -> {
                        openStoryViewer(dialogId);
                        if (isStealthModeEnabled) {
                            AndroidUtilities.runOnUIThread(StealthModeAlert::showStealthModeEnabledBulletin, 500);
                        }
                    });
                    showDialog(stealthModeAlert);
                }
            });
            options.addIf(!isPremiumBlocked && dialogId > 0 && !isPremium && isUnread && !isLive, R.drawable.msg_stories_stealth2, stealthModeLockedDrawable, LocaleController.getString(R.string.ViewAnonymously), () -> {
                StealthModeAlert stealthModeAlert = new StealthModeAlert(getContext(), 0, StealthModeAlert.TYPE_FROM_DIALOGS, getResourceProvider());
                stealthModeAlert.setListener(isStealthModeEnabled -> {
                    openStoryViewer(dialogId);
                    if (isStealthModeEnabled) {
                        AndroidUtilities.runOnUIThread(StealthModeAlert::showStealthModeEnabledBulletin, 500);
                    }
                });
                showDialog(stealthModeAlert);
            });
            options.addIf(!fromTopPeer && !showArchivedChannels, R.drawable.msg_archive, LocaleController.getString(R.string.ArchivePeerStories), () -> {
                toggleArciveForStory(dialogId);
            });
            options.addIf(!fromTopPeer && showArchivedChannels, R.drawable.msg_unarchive, LocaleController.getString(R.string.UnarchiveStories), () -> {
                toggleArciveForStory(dialogId);
            });
        }
        options.setGravity(Gravity.LEFT)
                .translate(dp(-8), dp(-10))
                .show();
    }

    private void toggleArciveForStory(long dialogId) {
        boolean hide = !showArchivedChannels;
        AndroidUtilities.runOnUIThread(() -> {
            getMessagesController().getStoriesController().toggleHidden(dialogId, hide, false, true);
            BulletinFactory.UndoObject undoObject = new BulletinFactory.UndoObject();
            undoObject.onUndo = () -> {
                getMessagesController().getStoriesController().toggleHidden(dialogId, !hide, false, true);
            };
            undoObject.onAction = () -> {
                getMessagesController().getStoriesController().toggleHidden(dialogId, hide, true, true);
            };
            CharSequence str;
            String name;
            TLObject object;
            if (dialogId >= 0) {
                TLRPC.User user = getMessagesController().getUser(dialogId);
                name = ContactsController.formatName(user.first_name, null, 15);
                object = user;
            } else {
                TLRPC.Chat chat = getMessagesController().getChat(-dialogId);
                name = chat.title;
                object = chat;
            }

            if (showArchivedChannels) {
                str = AndroidUtilities.replaceTags(LocaleController.formatString("StoriesMovedToDialogs", R.string.StoriesMovedToDialogs, name));
            } else {
                str = AndroidUtilities.replaceTags(LocaleController.formatString("StoriesMovedToContacts", R.string.StoriesMovedToContacts, ContactsController.formatName(name, null, 15)));
            }
            
            BulletinFactory.of(this).createUsersBulletin(
                Collections.singletonList(object),
                str,
                null,
                undoObject
            ).show();
        }, 200);
    }

    private long getDialogIdFromPeerStories(TL_stories.PeerStories peerStories) {
        if (peerStories.peer == null) return 0;
        if (peerStories.peer.user_id != 0) return peerStories.peer.user_id;
        if (peerStories.peer.chat_id != 0) return -peerStories.peer.chat_id;
        if (peerStories.peer.channel_id != 0) return -peerStories.peer.channel_id;
        return 0;
    }

    private TextView createSectionHeader(Context context, String text) {
        TextView header = new TextView(context);
        header.setText(text);
        header.setTextSize(17);
        header.setTypeface(AndroidUtilities.bold());
        header.setTextColor(Theme.getColor(Theme.key_chats_menuItemText));
        header.setPadding(dp(16), dp(8), dp(16), dp(4));
        header.setGravity(Gravity.CENTER_VERTICAL | (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT));
        return header;
    }

    private void updateUI() {
        boolean hasArchived = false;
        ArrayList<TLRPC.Dialog> allChannels = MessagesController.getInstance(currentAccount).dialogsChannelsOnly;
        if (allChannels != null) {
            for (TLRPC.Dialog dialog : allChannels) {
                if (dialog.folder_id == 1) {
                    hasArchived = true;
                    break;
                }
            }
        }
        if (!hasArchived && showArchivedChannels) {
            showArchivedChannels = false;
            if (channelsHeaderTextView != null) {
                channelsHeaderTextView.setText(getString(R.string.UpdatesChannelsHeader));
            }
        }
        if (channelsArchiveButton != null) {
            channelsArchiveButton.setVisibility(hasArchived ? View.VISIBLE : View.GONE);
            channelsArchiveButton.setText(showArchivedChannels ? (LocaleController.isRTL ? "القنوات" : "Channels") : (LocaleController.isRTL ? "المؤرشفة" : "Archive"));
        }
        if (statusHeader != null) {
            statusHeader.setText(showArchivedChannels ? (LocaleController.isRTL ? "القصص المخفية" : "Hidden Stories") : getString(R.string.UpdatesStatusHeader));
        }

        loadData();

        if (storiesAdapter != null) {
            storiesAdapter.notifyDataSetChanged();
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        updateStoriesScroll();
        if (themeToggleItem != null) {
            boolean isDark = getResourceProvider() != null ? getResourceProvider().isDark() : Theme.isCurrentThemeDark();
            themeToggleItem.setIcon(isDark ? R.drawable.menu_day_mode_24 : R.drawable.menu_night_mode_24);
            themeToggleItem.setContentDescription(getString(isDark ? R.string.SwitchThemeToDay : R.string.SwitchThemeToNight));
        }
        updateHeaderAvatar();
    }

    private void switchTheme(Theme.ThemeInfo themeInfo, boolean toDark) {
        ActionBarMenuItem originItem = themeToggleItem != null ? themeToggleItem : otherItem;
        if (originItem == null) return;
        int[] pos = new int[2];
        originItem.getLocationInWindow(pos);
        pos[0] += originItem.getIconView().getMeasuredWidth() / 2;
        pos[1] += originItem.getIconView().getMeasuredHeight() / 2;
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.needSetDayNightTheme, themeInfo, false, pos, -1, toDark, null, null, null, true);
    }

    private void updateHeaderAvatar() {
        if (headerAvatarView == null || actionBar == null) {
            return;
        }
        boolean show = com.hudgram.ui.HudConfig.showAvatarInHeader && !searching && !inSelectionMode;

        if (com.hudgram.ui.HudConfig.showMyNameInHeader) {
            TLRPC.User currentUser = getUserConfig().getCurrentUser();
            if (currentUser != null) {
                updateEmojiStatus(currentUser, false);
                actionBar.setTitle(org.telegram.messenger.UserObject.getUserName(currentUser), statusDrawable);
                if (com.hudgram.ui.HudConfig.showBioAsSubtitle) {
                    TLRPC.UserFull userFull = getMessagesController().getUserFull(getUserConfig().getClientUserId());
                    if (userFull != null) {
                        if (!android.text.TextUtils.isEmpty(userFull.about)) {
                            actionBar.setSubtitle(userFull.about);
                        } else {
                            actionBar.setSubtitle(null);
                        }
                    } else {
                        actionBar.setSubtitle(null);
                        getMessagesController().loadUserInfo(getUserConfig().getCurrentUser(), true, classGuid);
                    }
                } else {
                    actionBar.setSubtitle(null);
                }
            } else {
                updateEmojiStatus(null, false);
                actionBar.setTitle(getString(R.string.MainTabsUpdates), statusDrawable);
                actionBar.setSubtitle(null);
            }
        } else {
            updateEmojiStatus(null, false);
            actionBar.setTitle(getString(R.string.MainTabsUpdates), statusDrawable);
            actionBar.setSubtitle(null);
        }

        com.hudgram.ui.HudUiHelper.updateHeaderAvatar(headerAvatarView, actionBar, show, getUserConfig().getCurrentUser());
        actionBar.requestLayout();
    }

    private void updateEmojiStatus(TLRPC.User user, boolean animated) {
        if (statusDrawable == null || actionBar == null) {
            return;
        }
        if (user != null && user.emoji_status != null) {
            long emojiStatusId = DialogObject.getEmojiStatusDocumentId(user.emoji_status);
            boolean isCollectible = user.emoji_status instanceof TLRPC.TL_emojiStatusCollectible;
            statusDrawable.set(emojiStatusId, animated);
            statusDrawable.setParticles(isCollectible, animated);
        } else if (UserConfig.getInstance(currentAccount).isPremium()) {
            Drawable premiumStar = getParentActivity().getResources().getDrawable(R.drawable.msg_premium_liststar).mutate();
            premiumStar.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_profile_verifiedBackground), PorterDuff.Mode.MULTIPLY));
            statusDrawable.set(premiumStar, animated);
            statusDrawable.setParticles(false, animated);
        } else {
            statusDrawable.set((Drawable) null, animated);
            statusDrawable.setParticles(false, animated);
        }
        statusDrawable.setColor(Theme.getColor(Theme.key_profile_verifiedBackground));
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.userInfoDidLoad) {
            long uid = (Long) args[0];
            if (uid == getUserConfig().getClientUserId()) {
                AndroidUtilities.runOnUIThread(this::updateUI);
            }
        } else if (id == NotificationCenter.storiesUpdated ||
                id == NotificationCenter.dialogsNeedReload ||
                id == NotificationCenter.updateInterfaces ||
                id == NotificationCenter.mainUserInfoChanged) {
            AndroidUtilities.runOnUIThread(this::updateUI);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI();
    }

    @Override
    public boolean canBeginSlide() {
        return true;
    }

    @Override
    public boolean onBackPressed(boolean invoked) {
        if (hasShownSheet()) {
            if (invoked) closeSheet();
            return false;
        }
        if (inSelectionMode) {
            if (invoked) {
                hideActionMode();
            }
            return false;
        }
        if (actionBar != null && actionBar.isSearchFieldVisible()) {
            if (invoked) {
                actionBar.closeSearchField();
            }
            return false;
        }
        return super.onBackPressed(invoked);
    }

    public boolean showChatPreview(DialogCell cell) {
        long dialogId = cell.getDialogId();
        Bundle args = new Bundle();
        if (DialogObject.isEncryptedDialog(dialogId)) {
            return false;
        } else {
            if (DialogObject.isUserDialog(dialogId)) {
                args.putLong("user_id", dialogId);
            } else {
                args.putLong("chat_id", -dialogId);
            }
        }

        final ArrayList<Long> dialogIdArray = new ArrayList<>();
        dialogIdArray.add(dialogId);

        boolean hasFolders = getMessagesController().filtersEnabled && getMessagesController().dialogFiltersLoaded && getMessagesController().dialogFilters != null && getMessagesController().dialogFilters.size() > 0;
        final ActionBarPopupWindow.ActionBarPopupWindowLayout[] previewMenu = new ActionBarPopupWindow.ActionBarPopupWindowLayout[1];

        int flags = ActionBarPopupWindow.ActionBarPopupWindowLayout.FLAG_SHOWN_FROM_BOTTOM;
        if (hasFolders) {
            flags |= ActionBarPopupWindow.ActionBarPopupWindowLayout.FLAG_USE_SWIPEBACK;
        }
        final ActionBarPopupWindow.ActionBarPopupWindowLayout popupLayout = new ActionBarPopupWindow.ActionBarPopupWindowLayout(getParentActivity(), R.drawable.popup_fixed_alert4, getResourceProvider(), flags);
        previewMenu[0] = popupLayout;

        LinearLayout foldersMenuView = null;
        int[] foldersMenu = new int[1];
        if (hasFolders) {
            foldersMenuView = new LinearLayout(getParentActivity());
            foldersMenuView.setOrientation(LinearLayout.VERTICAL);

            ScrollView scrollView = new ScrollView(getParentActivity()) {
                @Override
                protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                    super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(
                            (int) Math.min(
                                    MeasureSpec.getSize(heightMeasureSpec),
                                    Math.min(AndroidUtilities.displaySize.y * 0.35f, dp(400))
                             ),
                            MeasureSpec.getMode(heightMeasureSpec)
                    ));
                }
            };
            LinearLayout linearLayout = new LinearLayout(getParentActivity());
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            scrollView.addView(linearLayout);
            final boolean backButtonAtTop = true;

            final int foldersCount = getMessagesController().dialogFilters.size();
            ActionBarMenuSubItem lastItem = null;
            for (int i = 0; i < foldersCount; ++i) {
                MessagesController.DialogFilter folder = getMessagesController().dialogFilters.get(i);
                if (folder.isDefault()) {
                    continue;
                }
                final boolean contains = folder.includesDialog(AccountInstance.getInstance(currentAccount), dialogId);
                final ArrayList<Long> alwaysShow = FiltersListBottomSheet.getDialogsCount(UpdatesActivity.this, folder, dialogIdArray, true, false);
                if (!contains) {
                    int currentCount = folder.alwaysShow.size();
                    if (currentCount + alwaysShow.size() > 100) {
                        continue;
                    }
                }
                ActionBarMenuSubItem folderItem = lastItem = new ActionBarMenuSubItem(getParentActivity(), 2, !backButtonAtTop && linearLayout.getChildCount() == 0, false, null);
                folderItem.setChecked(contains);
                CharSequence title = folder.name;
                title = Emoji.replaceEmoji(title, folderItem.getTextView().getPaint().getFontMetricsInt(), false);
                title = MessageObject.replaceAnimatedEmoji(title, folder.entities, folderItem.getTextView().getPaint().getFontMetricsInt());
                folderItem.setEmojiCacheType(folder.title_noanimate ? AnimatedEmojiDrawable.CACHE_TYPE_NOANIMATE_FOLDER : AnimatedEmojiDrawable.CACHE_TYPE_MESSAGES);
                folderItem.setTextAndIcon(title, 0, new FolderDrawable(getContext(), R.drawable.msg_folders, folder.color));
                folderItem.getTextView().setEmojiColor(Theme.getColor(Theme.key_featuredStickers_addButton));
                folderItem.setMinimumWidth(160);
                folderItem.setOnClickListener(e -> {
                    if (!contains) {
                        if (!alwaysShow.isEmpty()) {
                            for (int a = 0; a < alwaysShow.size(); a++) {
                                folder.neverShow.remove(alwaysShow.get(a));
                            }
                            folder.alwaysShow.addAll(alwaysShow);
                            FilterCreateActivity.saveFilterToServer(folder, folder.flags, folder.name, folder.entities, folder.title_noanimate, folder.color, folder.alwaysShow, folder.neverShow, folder.pinnedDialogs, false, false, true, true, false, UpdatesActivity.this, null);
                        }
                        if (getUndoView() != null) {
                            getUndoView().showWithAction(dialogId, UndoView.ACTION_ADDED_TO_FOLDER, alwaysShow.size(), folder, null, null);
                        }
                    } else {
                        folder.alwaysShow.remove(dialogId);
                        folder.neverShow.add(dialogId);
                        FilterCreateActivity.saveFilterToServer(folder, folder.flags, folder.name, folder.entities, folder.title_noanimate, folder.color, folder.alwaysShow, folder.neverShow, folder.pinnedDialogs, false, false, true, true, false, UpdatesActivity.this, null);
                        if (getUndoView() != null) {
                            getUndoView().showWithAction(dialogId, UndoView.ACTION_REMOVED_FROM_FOLDER, alwaysShow.size(), folder, null, null);
                        }
                    }
                    finishPreviewFragment();
                });
                linearLayout.addView(folderItem);
            }
            if (lastItem != null && backButtonAtTop) {
                lastItem.updateSelectorBackground(false, true);
            }
            if (linearLayout.getChildCount() <= 0) {
                hasFolders = false;
            } else {
                ActionBarPopupWindow.GapView gap = new ActionBarPopupWindow.GapView(getParentActivity(), getResourceProvider(), Theme.key_actionBarDefaultSubmenuSeparator);
                gap.setTag(R.id.fit_width_tag, 1);
                ActionBarMenuSubItem backItem = new ActionBarMenuSubItem(getParentActivity(), backButtonAtTop, !backButtonAtTop);
                backItem.setTextAndIcon(LocaleController.getString(R.string.Back), R.drawable.ic_ab_back);
                backItem.setMinimumWidth(160);
                backItem.setOnClickListener(e -> {
                    if (popupLayout != null && popupLayout.getSwipeBack() != null) {
                        popupLayout.getSwipeBack().closeForeground();
                    }
                });
                if (backButtonAtTop) {
                    foldersMenuView.addView(backItem);
                    foldersMenuView.addView(gap, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 8));
                    foldersMenuView.addView(scrollView);
                } else {
                    foldersMenuView.addView(scrollView);
                    foldersMenuView.addView(gap, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 8));
                    foldersMenuView.addView(backItem);
                }
            }
        }

        final ChatActivity[] chatActivity = new ChatActivity[1];

        if (hasFolders) {
            foldersMenu[0] = previewMenu[0].addViewToSwipeBack(foldersMenuView);
            ActionBarMenuSubItem addToFolderItem = new ActionBarMenuSubItem(getParentActivity(), true, false);
            addToFolderItem.setTextAndIcon(LocaleController.getString(R.string.FilterAddTo), R.drawable.msg_addfolder);
            addToFolderItem.setMinimumWidth(160);
            addToFolderItem.setOnClickListener(e ->
                    previewMenu[0].getSwipeBack().openForeground(foldersMenu[0])
            );
            previewMenu[0].addView(addToFolderItem);
            previewMenu[0].getSwipeBack().setOnHeightUpdateListener(height -> {
                if (chatActivity[0] == null || chatActivity[0].getFragmentView() == null || !chatActivity[0].isInPreviewMode()) {
                    return;
                }
                ViewGroup.LayoutParams lp = chatActivity[0].getFragmentView().getLayoutParams();
                if (lp instanceof ViewGroup.MarginLayoutParams) {
                    ((ViewGroup.MarginLayoutParams) lp).bottomMargin = dp(24 + 16 + 8) + height;
                    chatActivity[0].getFragmentView().setLayoutParams(lp);
                }
            });
        }

        ActionBarMenuSubItem markAsUnreadItem = new ActionBarMenuSubItem(getParentActivity(), true, false);
        if (cell.getHasUnread()) {
            markAsUnreadItem.setTextAndIcon(LocaleController.getString(R.string.MarkAsRead), R.drawable.msg_markread);
        } else {
            markAsUnreadItem.setTextAndIcon(LocaleController.getString(R.string.MarkAsUnread), R.drawable.msg_markunread);
        }
        markAsUnreadItem.setMinimumWidth(160);
        markAsUnreadItem.setOnClickListener(e -> {
            if (cell.getHasUnread()) {
                markAsRead(dialogId);
            } else {
                markAsUnread(dialogId);
            }
            finishPreviewFragment();
            updateUI();
        });
        previewMenu[0].addView(markAsUnreadItem);

        TLRPC.Dialog dialog = findDialogById(dialogId);
        boolean isPinned = dialog != null && dialog.pinned;
        ActionBarMenuSubItem unpinItem = new ActionBarMenuSubItem(getParentActivity(), false, false);
        if (isPinned) {
            unpinItem.setTextAndIcon(LocaleController.getString(R.string.UnpinMessage), R.drawable.msg_unpin);
        } else {
            unpinItem.setTextAndIcon(LocaleController.getString(R.string.PinMessage), R.drawable.msg_pin);
        }
        unpinItem.setMinimumWidth(160);
        unpinItem.setOnClickListener(e -> {
            finishPreviewFragment();
            getMessagesController().pinDialog(dialogId, !isPinned, null, -1);
            if (getUndoView() != null) {
                getUndoView().showWithAction(0, !isPinned ? UndoView.ACTION_PIN_DIALOGS : UndoView.ACTION_UNPIN_DIALOGS, 1, 1600, null, null);
            }
            updateUI();
        });
        previewMenu[0].addView(unpinItem);

        ActionBarMenuSubItem muteItem = new ActionBarMenuSubItem(getParentActivity(), false, false);
        boolean isMuted = getMessagesController().isDialogMuted(dialogId, 0);
        if (!isMuted) {
            muteItem.setTextAndIcon(LocaleController.getString(R.string.Mute), R.drawable.msg_mute);
        } else {
            muteItem.setTextAndIcon(LocaleController.getString(R.string.Unmute), R.drawable.msg_unmute);
        }
        muteItem.setMinimumWidth(160);
        muteItem.setOnClickListener(e -> {
            if (!isMuted) {
                getNotificationsController().setDialogNotificationsSettings(dialogId, 0, NotificationsController.SETTING_MUTE_FOREVER);
            } else {
                getNotificationsController().setDialogNotificationsSettings(dialogId, 0, NotificationsController.SETTING_MUTE_UNMUTE);
            }
            BulletinFactory.createMuteBulletin(UpdatesActivity.this, !isMuted, null).show();
            finishPreviewFragment();
            updateUI();
        });
        previewMenu[0].addView(muteItem);

        ActionBarMenuSubItem deleteItem = new ActionBarMenuSubItem(getParentActivity(), false, true);
        deleteItem.setIconColor(Theme.getColor(Theme.key_text_RedRegular));
        deleteItem.setTextColor(Theme.getColor(Theme.key_text_RedBold));
        deleteItem.setSelectorColor(Theme.multAlpha(Theme.getColor(Theme.key_text_RedBold), .12f));
        deleteItem.setTextAndIcon(LocaleController.getString(R.string.Delete), R.drawable.msg_delete);
        deleteItem.setMinimumWidth(160);
        deleteItem.setOnClickListener(e -> {
            finishPreviewFragment();
            TLRPC.Chat chat = getMessagesController().getChat(-dialogId);
            org.telegram.ui.Components.AlertsCreator.createClearOrDeleteDialogAlert(UpdatesActivity.this, false, chat, null, false, false, (revoke) -> {
                getMessagesController().deleteDialog(dialogId, 0, revoke);
                updateUI();
            });
        });
        previewMenu[0].addView(deleteItem);

        if (getMessagesController().checkCanOpenChat(args, UpdatesActivity.this)) {
            prepareBlurBitmap();
            if (parentLayout != null) {
                parentLayout.setHighlightActionButtons(true);
            }
            if (AndroidUtilities.displaySize.x > AndroidUtilities.displaySize.y) {
                presentFragmentAsPreview(chatActivity[0] = new ChatActivity(args));
            } else {
                presentFragmentAsPreviewWithMenu(chatActivity[0] = new ChatActivity(args), previewMenu[0]);
                if (chatActivity[0] != null) {
                    chatActivity[0].allowExpandPreviewByClick = true;
                    try {
                        chatActivity[0].getAvatarContainer().getAvatarImageView().performAccessibilityAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null);
                    } catch (Exception ignore) {
                    }
                }
            }
            return true;
        }
        return false;
    }

    public UndoView getUndoView() {
        return undoView;
    }

    private void prepareBlurBitmap() {
        if (blurredView == null || parentLayout == null) {
            return;
        }
        int w = (int) (fragmentView.getMeasuredWidth() / 6.0f);
        int h = (int) (fragmentView.getMeasuredHeight() / 6.0f);
        if (w <= 0 || h <= 0) return;
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.scale(1.0f / 6.0f, 1.0f / 6.0f);
        parentLayout.getView().draw(canvas);
        Utilities.stackBlurBitmap(bitmap, Math.max(7, Math.max(w, h) / 180));
        blurredView.setBackground(new BitmapDrawable(getParentActivity().getResources(), bitmap));
        blurredView.setAlpha(0.0f);
        blurredView.setVisibility(View.VISIBLE);
        if (blurredView.getParent() != null) {
            ((ViewGroup) blurredView.getParent()).removeView(blurredView);
        }
        parentLayout.getOverlayContainerView().addView(blurredView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
    }

    private void markAsRead(long did) {
        TLRPC.Dialog dialog = findDialogById(did);
        if (dialog == null) return;
        getMessagesController().markMentionsAsRead(did, 0);
        getMessagesController().markDialogAsRead(did, dialog.top_message, dialog.top_message, dialog.last_message_date, false, 0, 0, true, 0);
    }

    private void markAsUnread(long did) {
        getMessagesController().markDialogAsUnread(did, null, 0);
    }

    private void toggleSelection(long dialogId) {
        if (selectedDialogIds.contains(dialogId)) {
            selectedDialogIds.remove(dialogId);
        } else {
            selectedDialogIds.add(dialogId);
        }

        if (selectedDialogIds.isEmpty()) {
            hideActionMode();
        } else {
            if (!inSelectionMode) {
                inSelectionMode = true;
                createActionMode();
                if (actionBar != null) {
                    actionBar.showActionMode();
                }
            }
            if (selectedDialogsCountTextView != null) {
                selectedDialogsCountTextView.setNumber(selectedDialogIds.size(), true);
            }
            updateActionMode();
            updateUI();
        }
    }

    private TLRPC.Dialog findDialogById(long dialogId) {
        for (TLRPC.Dialog d : channelDialogs) {
            if (d.id == dialogId) {
                return d;
            }
        }
        return getMessagesController().dialogs_dict.get(dialogId);
    }

    private void updateActionMode() {
        if (archiveItem == null) return;

        int canPinCount = 0;
        int canUnpinCount = 0;
        int canMuteCount = 0;
        int canUnmuteCount = 0;
        int canReadCount = 0;
        int canArchiveCount = 0;
        int canUnarchiveCount = 0;

        for (long dialogId : selectedDialogIds) {
            TLRPC.Dialog dialog = findDialogById(dialogId);
            if (dialog == null) {
                continue;
            }

            boolean pinned = dialog.pinned;
            boolean isMuted = getMessagesController().isDialogMuted(dialogId, 0);
            boolean hasUnread = dialog.unread_count != 0 || dialog.unread_mark;

            if (pinned) {
                canUnpinCount++;
            } else {
                canPinCount++;
            }

            if (isMuted) {
                canUnmuteCount++;
            } else {
                canMuteCount++;
            }

            if (hasUnread) {
                canReadCount++;
            }

            if (dialog.folder_id == 1) {
                canUnarchiveCount++;
            } else {
                canArchiveCount++;
            }
        }

        // Update archiveItem icon
        if (canUnarchiveCount != 0) {
            archiveItem.setIcon(R.drawable.msg_unarchive);
        } else {
            archiveItem.setIcon(R.drawable.msg_archive);
        }

        // Update muteItem icon
        if (muteItem != null) {
            if (canUnmuteCount != 0) {
                muteItem.setIcon(R.drawable.msg_unmute);
            } else {
                muteItem.setIcon(R.drawable.msg_mute);
            }
        }

        // Update pinItem icon
        if (pinItem != null) {
            if (canPinCount != 0) {
                pinItem.setTextAndIcon(LocaleController.getString(R.string.PinToTop), R.drawable.msg_pin);
            } else {
                pinItem.setTextAndIcon(LocaleController.getString(R.string.UnpinFromTop), R.drawable.msg_unpin);
            }
        }

        // Update readItem (Mark as read / unread subitem)
        if (readItem != null) {
            if (canReadCount != 0) {
                readItem.setTextAndIcon(LocaleController.getString(R.string.MarkAsRead), R.drawable.msg_markread);
            } else {
                readItem.setTextAndIcon(LocaleController.getString(R.string.MarkAsUnread), R.drawable.msg_markunread);
            }
        }

        boolean singleSelected = selectedDialogIds.size() == 1;
        if (copyLinkItem != null) {
            copyLinkItem.setVisibility(singleSelected ? View.VISIBLE : View.GONE);
        }
        if (viewChannelItem != null) {
            viewChannelItem.setVisibility(singleSelected ? View.VISIBLE : View.GONE);
        }
    }

    private void createActionMode() {
        if (actionBar == null || actionBar.actionModeIsExist(null)) {
            return;
        }
        final org.telegram.ui.ActionBar.ActionBarMenu actionMode = actionBar.createActionMode(true, null);

        ImageView actionModeCloseView = new ImageView(getContext());
        actionModeCloseView.setScaleType(ImageView.ScaleType.CENTER);
        actionModeCloseView.setImageDrawable(new org.telegram.ui.ActionBar.BackDrawable(true));
        actionModeCloseView.setColorFilter(new android.graphics.PorterDuffColorFilter(getThemedColor(Theme.key_actionBarActionModeDefaultIcon), android.graphics.PorterDuff.Mode.MULTIPLY));
        actionModeCloseView.setBackground(Theme.createSelectorDrawable(getThemedColor(Theme.key_actionBarActionModeDefaultSelector)));
        actionModeCloseView.setOnClickListener(v -> hideActionMode());
        actionMode.addView(actionModeCloseView, LayoutHelper.createLinear(54, 54, Gravity.CENTER_VERTICAL));

        selectedDialogsCountTextView = new org.telegram.ui.Components.NumberTextView(actionMode.getContext());
        selectedDialogsCountTextView.setTextSize(18);
        selectedDialogsCountTextView.setTypeface(AndroidUtilities.bold());
        selectedDialogsCountTextView.setTextColor(Theme.getColor(Theme.key_actionBarActionModeDefaultIcon));
        actionMode.addView(selectedDialogsCountTextView, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f, 18, 0, 0, 0));

        // 1. Mute item
        muteItem = actionMode.addItemWithWidth(101, R.drawable.msg_mute, dp(54));
 
        // 2. Archive item
        archiveItem = actionMode.addItemWithWidth(102, R.drawable.msg_archive, dp(54));
 
        // 3. Delete item
        deleteItem = actionMode.addItemWithWidth(103, R.drawable.msg_delete, dp(54));
 
        // 4. Options item
        org.telegram.ui.ActionBar.ActionBarMenuItem otherItem = actionMode.addItemWithWidth(104, R.drawable.ic_ab_other, dp(54));
        pinItem = otherItem.addSubItem(201, R.drawable.msg_pin, LocaleController.getString(R.string.PinToTop)); // Pin / Unpin
        copyLinkItem = otherItem.addSubItem(105, R.drawable.msg_link, LocaleController.getString(R.string.CopyLink)); // Copy link
        readItem = otherItem.addSubItem(203, R.drawable.msg_markread, LocaleController.getString(R.string.MarkAsRead)); // Mark as read / unread
        otherItem.addSubItem(205, R.drawable.msg_contacts, LocaleController.getString(R.string.SelectAll)); // Select all
        viewChannelItem = otherItem.addSubItem(206, R.drawable.msg_info, LocaleController.getString(R.string.ViewChannel)); // Channel Info
        otherItem.addSubItem(207, R.drawable.msg_leave, LocaleController.getString(R.string.LeaveChannel)); // Unfollow / Leave
        otherItem.addSubItem(202, R.drawable.msg_addfolder, LocaleController.getString(R.string.FilterAddTo)); // Add to folder
        otherItem.addSubItem(204, R.drawable.msg_clear, LocaleController.getString(R.string.ClearCache)); // Delete from cache
    }

    private void hideActionMode() {
        if (actionBar != null) {
            actionBar.hideActionMode();
        }
        inSelectionMode = false;
        selectedDialogIds.clear();
        updateUI();
    }

    private void toggleMuteSelectedChannels() {
        if (selectedDialogIds.isEmpty()) return;

        ArrayList<CharSequence> items = new ArrayList<>();
        ArrayList<Integer> itemIds = new ArrayList<>();

        items.add(LocaleController.formatString("MuteFor", R.string.MuteFor, LocaleController.formatPluralString("Hours", 1)));
        itemIds.add(0); // 1 hour

        items.add(LocaleController.formatString("MuteFor", R.string.MuteFor, LocaleController.formatPluralString("Hours", 8)));
        itemIds.add(1); // 8 hours

        items.add(LocaleController.formatString("MuteFor", R.string.MuteFor, LocaleController.formatPluralString("Days", 2)));
        itemIds.add(2); // 2 days

        items.add(LocaleController.getString(R.string.MuteDisable)); // Disable (Mute forever)
        itemIds.add(3);

        boolean hasMuted = false;
        for (long dialogId : selectedDialogIds) {
            if (MessagesController.getInstance(currentAccount).isDialogMuted(dialogId, 0)) {
                hasMuted = true;
                break;
            }
        }
        if (hasMuted) {
            items.add(LocaleController.getString(R.string.Unmute)); // Unmute
            itemIds.add(4);
        }

        org.telegram.ui.ActionBar.BottomSheet.Builder builder = new org.telegram.ui.ActionBar.BottomSheet.Builder(getParentActivity(), false);
        builder.setTitle(LocaleController.getString(R.string.Notifications), true);
        builder.setItems(items.toArray(new CharSequence[0]), (dialogInterface, i) -> {
            int action = itemIds.get(i);
            int setting;
            if (action == 0) {
                setting = NotificationsController.SETTING_MUTE_HOUR;
            } else if (action == 1) {
                setting = NotificationsController.SETTING_MUTE_8_HOURS;
            } else if (action == 2) {
                setting = NotificationsController.SETTING_MUTE_2_DAYS;
            } else if (action == 3) {
                setting = NotificationsController.SETTING_MUTE_FOREVER;
            } else {
                setting = NotificationsController.SETTING_MUTE_UNMUTE;
            }

            for (long dialogId : selectedDialogIds) {
                getNotificationsController().setDialogNotificationsSettings(dialogId, 0, setting);
            }

            BulletinFactory.createMuteBulletin(UpdatesActivity.this, setting, 0, null).show();
            hideActionMode();
        });

        showDialog(builder.create());
    }

    private void archiveSelectedChannels() {
        if (selectedDialogIds.isEmpty()) return;
        ArrayList<Long> copy = new ArrayList<>(selectedDialogIds);
        
        boolean unarchive = false;
        for (long dialogId : selectedDialogIds) {
            TLRPC.Dialog d = findDialogById(dialogId);
            if (d != null && d.folder_id == 1) {
                unarchive = true;
                break;
            }
        }
        
        getMessagesController().addDialogToFolder(copy, unarchive ? 0 : 1, -1, null, 0);
        if (getUndoView() != null) {
            getUndoView().showWithAction(0, unarchive ? UndoView.ACTION_CHAT_UNARCHIVED : UndoView.ACTION_ARCHIVE, selectedDialogIds.size(), 1600, null, null);
        }
        hideActionMode();
    }

    private void deleteSelectedChannels() {
        if (selectedDialogIds.isEmpty()) return;
        if (selectedDialogIds.size() == 1) {
            long dialogId = selectedDialogIds.iterator().next();
            TLRPC.Chat chat = getMessagesController().getChat(-dialogId);
            org.telegram.ui.Components.AlertsCreator.createClearOrDeleteDialogAlert(UpdatesActivity.this, false, chat, null, false, false, (revoke) -> {
                getMessagesController().deleteDialog(dialogId, 0, revoke);
                hideActionMode();
            });
        } else {
            org.telegram.ui.ActionBar.AlertDialog.Builder builder = new org.telegram.ui.ActionBar.AlertDialog.Builder(getParentActivity());
            builder.setTitle(LocaleController.getString(R.string.AppName));
            builder.setMessage(LocaleController.getString(R.string.AreYouSureDeleteFewChats));
            builder.setPositiveButton(LocaleController.getString(R.string.OK), (dialogInterface, i) -> {
                for (long dialogId : selectedDialogIds) {
                    getMessagesController().deleteDialog(dialogId, 0, false);
                }
                hideActionMode();
            });
            builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
            showDialog(builder.create());
        }
    }

    private void pinOrUnpinSelectedChannels() {
        if (selectedDialogIds.isEmpty()) return;
        boolean pin = false;
        for (long dialogId : selectedDialogIds) {
            TLRPC.Dialog d = findDialogById(dialogId);
            if (d != null && !d.pinned) {
                pin = true;
                break;
            }
        }
        for (long dialogId : selectedDialogIds) {
            getMessagesController().pinDialog(dialogId, pin, null, 0L);
        }
        if (getUndoView() != null) {
            getUndoView().showWithAction(0, pin ? UndoView.ACTION_PIN_DIALOGS : UndoView.ACTION_UNPIN_DIALOGS, selectedDialogIds.size(), 1600, null, null);
        }
        hideActionMode();
    }

    private void addToFolderSelectedChannels() {
        if (selectedDialogIds.isEmpty()) return;
        ArrayList<MessagesController.DialogFilter> filters = new ArrayList<>();
        ArrayList<MessagesController.DialogFilter> allFilters = getMessagesController().dialogFilters;
        for (int i = 0; i < allFilters.size(); i++) {
            MessagesController.DialogFilter filter = allFilters.get(i);
            if (!filter.isDefault()) {
                filters.add(filter);
            }
        }
        if (filters.isEmpty()) {
            return;
        }
        org.telegram.ui.ActionBar.BottomSheet.Builder builder = new org.telegram.ui.ActionBar.BottomSheet.Builder(getParentActivity());
        CharSequence[] items = new CharSequence[filters.size()];
        int[] icons = new int[filters.size()];
        for (int i = 0; i < filters.size(); i++) {
            items[i] = filters.get(i).name;
            icons[i] = R.drawable.msg_folders;
        }
        builder.setItems(items, icons, (dialogInterface, position) -> {
            MessagesController.DialogFilter filter = filters.get(position);
            ArrayList<Long> alwaysShow = new ArrayList<>();
            for (long did : selectedDialogIds) {
                if (!filter.alwaysShow.contains(did)) {
                    alwaysShow.add(did);
                }
            }
            if (!alwaysShow.isEmpty()) {
                for (int a = 0; a < alwaysShow.size(); a++) {
                    filter.neverShow.remove(alwaysShow.get(a));
                }
                filter.alwaysShow.addAll(alwaysShow);
                org.telegram.ui.FilterCreateActivity.saveFilterToServer(
                    filter, filter.flags, filter.name, filter.entities, 
                    filter.title_noanimate, filter.color, filter.alwaysShow, 
                    filter.neverShow, filter.pinnedDialogs, false, false, 
                    true, true, false, UpdatesActivity.this, null
                );
                if (getUndoView() != null) {
                    getUndoView().showWithAction(0, UndoView.ACTION_ADDED_TO_FOLDER, alwaysShow.size(), filter, null, null);
                }
            }
            hideActionMode();
        });
        showDialog(builder.create());
    }

    private void markSelectedChannelsReadOrUnread() {
        if (selectedDialogIds.isEmpty()) return;
        boolean markRead = false;
        for (long dialogId : selectedDialogIds) {
            TLRPC.Dialog d = findDialogById(dialogId);
            if (d != null && (d.unread_count != 0 || d.unread_mark)) {
                markRead = true;
                break;
            }
        }
        for (long dialogId : selectedDialogIds) {
            if (markRead) {
                markAsRead(dialogId);
            } else {
                markAsUnread(dialogId);
            }
        }
        hideActionMode();
    }

    private void deleteCacheSelectedChannels() {
        if (selectedDialogIds.isEmpty()) return;
        org.telegram.ui.ActionBar.AlertDialog.Builder builder = new org.telegram.ui.ActionBar.AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString(R.string.ClearCache));
        builder.setMessage(LocaleController.getString(R.string.ClearCacheForChats));
        builder.setPositiveButton(LocaleController.getString(R.string.OK), (dialogInterface, i) -> {
            for (long dialogId : selectedDialogIds) {
                getMessagesController().deleteDialog(dialogId, 2, false);
            }
            hideActionMode();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void copySelectedChannelLink() {
        if (selectedDialogIds.isEmpty()) return;
        long dialogId = selectedDialogIds.iterator().next();
        TLRPC.Chat chat = getMessagesController().getChat(-dialogId);
        if (chat != null) {
            String link;
            if (!android.text.TextUtils.isEmpty(chat.username)) {
                link = "https://t.me/" + chat.username;
            } else {
                link = chat.title;
            }
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getParentActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("channel_link", link);
            clipboard.setPrimaryClip(clip);
            BulletinFactory.of(UpdatesActivity.this).createCopyLinkBulletin().show();
        }
        hideActionMode();
    }

    private void selectAllChannels() {
        selectedDialogIds.clear();
        for (TLRPC.Dialog d : channelDialogs) {
            selectedDialogIds.add(d.id);
        }
        if (selectedDialogsCountTextView != null) {
            selectedDialogsCountTextView.setNumber(selectedDialogIds.size(), true);
        }
        updateActionMode();
        updateUI();
    }

    private void openSelectedChannelInfo() {
        if (selectedDialogIds.size() == 1) {
            long dialogId = selectedDialogIds.iterator().next();
            Bundle args = new Bundle();
            args.putLong("chat_id", -dialogId);
            presentFragment(new org.telegram.ui.ProfileActivity(args));
            hideActionMode();
        }
    }

    private void unfollowSelectedChannels() {
        deleteSelectedChannels();
    }

    @Override
    public void onTransitionAnimationProgress(boolean isOpen, float progress) {
        if (blurredView != null && blurredView.getVisibility() == View.VISIBLE) {
            if (isOpen) {
                blurredView.setAlpha(1.0f - progress);
            } else {
                blurredView.setAlpha(progress);
            }
        }
    }

    @Override
    public void onTransitionAnimationEnd(boolean isOpen, boolean backward) {
        if (isOpen && blurredView != null) {
            if (blurredView.getParent() != null) {
                ((ViewGroup) blurredView.getParent()).removeView(blurredView);
            }
            blurredView.setBackground(null);
        }
    }

    // Stories Adapter
    private class StoriesAdapter extends RecyclerListView.SelectionAdapter {
        private final Context context;

        public StoriesAdapter(Context context) {
            this.context = context;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return true;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            UpdatesStoryCell cell = new UpdatesStoryCell(context);
            cell.setLayoutParams(new RecyclerView.LayoutParams(dp(76), dp(135)));
            return new RecyclerListView.Holder(cell);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            UpdatesStoryCell cell = (UpdatesStoryCell) holder.itemView;

            if (!showArchivedChannels && position == 0) {
                // Self story
                StoriesController storiesController = MessagesController.getInstance(currentAccount).getStoriesController();
                long selfId = UserConfig.getInstance(currentAccount).getClientUserId();
                TL_stories.PeerStories selfStories = storiesController.getStories(selfId);
                cell.setStory(currentAccount, selfStories, true);
            } else {
                int storyIndex = showArchivedChannels ? position : position - 1;
                if (storyIndex < storyItems.size()) {
                    TL_stories.PeerStories peerStories = storyItems.get(storyIndex);
                    long id = getDialogIdFromPeerStories(peerStories);
                    boolean isSelf = id == UserConfig.getInstance(currentAccount).getClientUserId();
                    cell.setStory(currentAccount, peerStories, isSelf);
                }
            }

            // Add spacing between cards
            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) cell.getLayoutParams();
            params.rightMargin = dp(6);
            params.leftMargin = 0;
            cell.setLayoutParams(params);
        }

        @Override
        public int getItemCount() {
            if (showArchivedChannels) {
                return storyItems.size();
            }
            return 1 + storyItems.size(); // +1 for self
        }
    }

    private int getStoriesScrollY() {
        if (listView == null || listView.getChildCount() == 0) {
            return 0;
        }
        View firstChild = listView.getChildAt(0);
        RecyclerView.ViewHolder holder = listView.getChildViewHolder(firstChild);
        if (holder != null && holder.getItemViewType() == TYPE_STORIES_PLACEHOLDER) {
            return listView.getPaddingTop() - firstChild.getTop();
        }
        return storiesContainer != null ? storiesContainer.getHeight() : 0;
    }

    private boolean isStoriesBarVisible() {
        if (showArchivedChannels) {
            return storyItems != null && !storyItems.isEmpty();
        }
        return true;
    }

    private void updateStoriesScroll() {
        if (storiesContainer == null || getContext() == null) {
            return;
        }
        int scrollY = getStoriesScrollY();
        int height = storiesContainer.getHeight();
        if (height == 0) {
            height = storiesContainer.getMeasuredHeight();
        }
        if (height == 0 && isStoriesBarVisible()) {
            return;
        }
        int translationY = -Math.min(scrollY, height);
        int topPadding = AndroidUtilities.getStatusBarHeight(getContext()) + ActionBar.getCurrentActionBarHeight();
        storiesContainer.setTranslationY(topPadding + translationY);

        float alpha = isStoriesBarVisible() ? (1.0f - Math.min(1.0f, (float) scrollY / height)) : 0f;
        storiesContainer.setAlpha(alpha);
        storiesContainer.setVisibility(alpha == 0f ? View.GONE : View.VISIBLE);
    }

    private void executeSwipeAction(long dialogId, int action, int position) {
        if (action == SwipeGestureSettingsView.SWIPE_GESTURE_PIN) {
            TLRPC.Dialog dialog = findDialogById(dialogId);
            boolean isPinned = dialog != null && dialog.pinned;
            getMessagesController().pinDialog(dialogId, !isPinned, null, -1);
            if (getUndoView() != null) {
                getUndoView().showWithAction(0, !isPinned ? UndoView.ACTION_PIN_DIALOGS : UndoView.ACTION_UNPIN_DIALOGS, 1, 1600, null, null);
            }
            updateUI();
        } else if (action == SwipeGestureSettingsView.SWIPE_GESTURE_READ) {
            TLRPC.Dialog dialog = findDialogById(dialogId);
            if (dialog != null) {
                boolean hasUnread = dialog.unread_count != 0 || dialog.unread_mark;
                if (hasUnread) {
                    markAsRead(dialogId);
                } else {
                    markAsUnread(dialogId);
                }
                updateUI();
            }
        } else if (action == SwipeGestureSettingsView.SWIPE_GESTURE_ARCHIVE || action == SwipeGestureSettingsView.SWIPE_GESTURE_FOLDERS) {
            boolean unarchive = false;
            TLRPC.Dialog d = findDialogById(dialogId);
            if (d != null && d.folder_id == 1) {
                unarchive = true;
            }
            ArrayList<Long> copy = new ArrayList<>();
            copy.add(dialogId);
            getMessagesController().addDialogToFolder(copy, unarchive ? 0 : 1, -1, null, 0);
            if (getUndoView() != null) {
                getUndoView().showWithAction(0, unarchive ? UndoView.ACTION_CHAT_UNARCHIVED : UndoView.ACTION_ARCHIVE, 1, 1600, null, null);
            }
            updateUI();
        } else if (action == SwipeGestureSettingsView.SWIPE_GESTURE_MUTE) {
            boolean isMuted = getMessagesController().isDialogMuted(dialogId, 0);
            if (!isMuted) {
                getNotificationsController().setDialogNotificationsSettings(dialogId, 0, NotificationsController.SETTING_MUTE_FOREVER);
            } else {
                getNotificationsController().setDialogNotificationsSettings(dialogId, 0, NotificationsController.SETTING_MUTE_UNMUTE);
            }
            BulletinFactory.createMuteBulletin(UpdatesActivity.this, !isMuted, null).show();
            updateUI();
        } else if (action == SwipeGestureSettingsView.SWIPE_GESTURE_DELETE) {
            TLRPC.Chat chat = getMessagesController().getChat(-dialogId);
            org.telegram.ui.Components.AlertsCreator.createClearOrDeleteDialogAlert(UpdatesActivity.this, false, chat, null, false, false, (revoke) -> {
                getMessagesController().deleteDialog(dialogId, 0, revoke);
                updateUI();
            });
        }
    }

    private class StoriesPlaceholderView extends View {
        public StoriesPlaceholderView(Context context) {
            super(context);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int height = isStoriesBarVisible() && storiesContainer != null ? storiesContainer.getMeasuredHeight() : 0;
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), height);
        }
    }

    private class UpdatesAdapter extends RecyclerListView.SelectionAdapter {
        private final Context context;

        public UpdatesAdapter(Context context) {
            this.context = context;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return holder.getItemViewType() == TYPE_CHANNEL;
        }

        @Override
        public int getItemCount() {
            if (channelDialogs.isEmpty()) {
                return 3; // Placeholder, Header, EmptyView
            }
            return 2 + channelDialogs.size(); // Placeholder, Header, Channels
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0) {
                return TYPE_STORIES_PLACEHOLDER;
            } else if (position == 1) {
                return TYPE_HEADER;
            } else {
                if (channelDialogs.isEmpty()) {
                    return TYPE_EMPTY;
                } else {
                    return TYPE_CHANNEL;
                }
            }
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            if (viewType == TYPE_STORIES_PLACEHOLDER) {
                view = new StoriesPlaceholderView(context);
            } else if (viewType == TYPE_HEADER) {
                FrameLayout headerLayout = new FrameLayout(context);
                
                channelsHeaderTextView = new TextView(context);
                channelsHeaderTextView.setTextSize(17);
                channelsHeaderTextView.setTypeface(AndroidUtilities.bold());
                channelsHeaderTextView.setTextColor(Theme.getColor(Theme.key_chats_menuItemText));
                channelsHeaderTextView.setGravity(Gravity.CENTER_VERTICAL | (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT));
                headerLayout.addView(channelsHeaderTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 
                    (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL, 16, 8, 16, 8));

                channelsArchiveButton = new TextView(context);
                channelsArchiveButton.setTextSize(15);
                channelsArchiveButton.setTypeface(AndroidUtilities.bold());
                channelsArchiveButton.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));
                channelsArchiveButton.setGravity(Gravity.CENTER_VERTICAL | (LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT));
                channelsArchiveButton.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 2));
                channelsArchiveButton.setPadding(dp(8), dp(4), dp(8), dp(4));
                channelsArchiveButton.setOnClickListener(v -> {
                    showArchivedChannels = !showArchivedChannels;
                    updateUI();
                });
                headerLayout.addView(channelsArchiveButton, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 
                    (LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT) | Gravity.CENTER_VERTICAL, 16, 8, 16, 8));
                
                headerLayout.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                view = headerLayout;
            } else if (viewType == TYPE_EMPTY) {
                FrameLayout container = new FrameLayout(context) {
                    @Override
                    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                        int totalHeight = MeasureSpec.getSize(heightMeasureSpec);
                        int storiesHeight = storiesContainer != null ? storiesContainer.getMeasuredHeight() : 0;
                        int headerHeight = dp(40);
                        int minHeight = totalHeight - storiesHeight - headerHeight;
                        if (minHeight < dp(300)) {
                            minHeight = dp(300);
                        }
                        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(minHeight, MeasureSpec.EXACTLY));
                    }
                };
                if (noChannelsView.getParent() != null) {
                    ((ViewGroup) noChannelsView.getParent()).removeView(noChannelsView);
                }
                noChannelsView.setVisibility(View.VISIBLE);
                container.addView(noChannelsView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
                view = container;
            } else {
                DialogCell cell = new DialogCell((DialogsActivity) null, context, true, false);
                cell.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                view = cell;
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            int type = holder.getItemViewType();
            if (type == TYPE_HEADER) {
                boolean hasArchived = false;
                ArrayList<TLRPC.Dialog> allChannels = MessagesController.getInstance(currentAccount).dialogsChannelsOnly;
                if (allChannels != null) {
                    for (TLRPC.Dialog dialog : allChannels) {
                        if (dialog.folder_id == 1) {
                            hasArchived = true;
                            break;
                        }
                    }
                }
                channelsHeaderTextView.setText(showArchivedChannels ? (LocaleController.isRTL ? "القنوات المؤرشفة" : "Archived Channels") : getString(R.string.UpdatesChannelsHeader));
                channelsArchiveButton.setVisibility(hasArchived ? View.VISIBLE : View.GONE);
                channelsArchiveButton.setText(showArchivedChannels ? (LocaleController.isRTL ? "القنوات" : "Channels") : (LocaleController.isRTL ? "المؤرشفة" : "Archive"));
            } else if (type == TYPE_CHANNEL) {
                DialogCell cell = (DialogCell) holder.itemView;
                int dialogIndex = position - 2;
                if (dialogIndex >= 0 && dialogIndex < channelDialogs.size()) {
                    TLRPC.Dialog dialog = channelDialogs.get(dialogIndex);
                    cell.setDialog(dialog, DialogsActivity.DIALOGS_TYPE_CHANNELS_ONLY, 0);
                    cell.setChecked(inSelectionMode && selectedDialogIds.contains(dialog.id), false);


                }
            }
        }
    }

    private class SwipeController extends ItemTouchHelper.Callback {
        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            if (inSelectionMode) {
                return 0;
            }
            if (viewHolder.getItemViewType() == TYPE_CHANNEL && viewHolder.itemView instanceof DialogCell) {
                DialogCell dialogCell = (DialogCell) viewHolder.itemView;
                dialogCell.setSliding(true);
                dialogCell.swipeCanceled = false;
                return makeMovementFlags(0, ItemTouchHelper.LEFT);
            }
            return 0;
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder source, @NonNull RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            if (viewHolder.itemView instanceof DialogCell) {
                DialogCell cell = (DialogCell) viewHolder.itemView;
                long dialogId = cell.getDialogId();
                int position = viewHolder.getAdapterPosition();
                int action = SharedConfig.getChatSwipeAction(currentAccount);
                
                cell.setTranslationX(0);
                if (adapter != null) {
                    adapter.notifyItemChanged(position);
                }
                executeSwipeAction(dialogId, action, position);
            }
        }

        @Override
        public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
            if (viewHolder != null) {
                listView.hideSelector(false);
            }
            if (viewHolder != null && viewHolder.itemView instanceof DialogCell) {
                ((DialogCell) viewHolder.itemView).swipeCanceled = false;
            }
            super.onSelectedChanged(viewHolder, actionState);
        }

        @Override
        public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
            return 0.45f;
        }

        @Override
        public float getSwipeEscapeVelocity(float defaultValue) {
            return 3500f;
        }

        @Override
        public float getSwipeVelocityThreshold(float defaultValue) {
            return Float.MAX_VALUE;
        }
    }

    @Override
    public java.util.ArrayList<org.telegram.ui.ActionBar.ThemeDescription> getThemeDescriptions() {
        java.util.ArrayList<org.telegram.ui.ActionBar.ThemeDescription> themeDescriptions = new java.util.ArrayList<>();

        org.telegram.ui.ActionBar.ThemeDescription.ThemeDescriptionDelegate cellDelegate = () -> {
            if (actionBar != null) {
                actionBar.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                actionBar.setSubtitleColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
                
                // Normal mode popup colors
                actionBar.setPopupBackgroundColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuBackground), false);
                actionBar.setPopupItemsColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuItem), false, false);
                actionBar.setPopupItemsColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuItemIcon), true, false);
                actionBar.setPopupItemsSelectorColor(Theme.getColor(Theme.key_dialogButtonSelector), false);

                // Action mode popup colors
                actionBar.setPopupBackgroundColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuBackground), true);
                actionBar.setPopupItemsColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuItem), false, true);
                actionBar.setPopupItemsColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuItemIcon), true, true);
                actionBar.setPopupItemsSelectorColor(Theme.getColor(Theme.key_dialogButtonSelector), true);

                actionBar.updateColors();
            }
            if (fragmentView != null) {
                fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            }
            if (listView != null) {
                listView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                int count = listView.getChildCount();
                for (int a = 0; a < count; a++) {
                    android.view.View child = listView.getChildAt(a);
                    if (child instanceof org.telegram.ui.Cells.DialogCell) {
                        ((org.telegram.ui.Cells.DialogCell) child).update(0);
                    }
                }
            }
            if (storiesContainer != null) {
                storiesContainer.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            }
            if (storiesRecyclerView != null) {
                int count = storiesRecyclerView.getChildCount();
                for (int a = 0; a < count; a++) {
                    android.view.View child = storiesRecyclerView.getChildAt(a);
                    child.invalidate();
                }
            }
            if (cameraFab != null) {
                cameraFab.updateColors();
            }
            if (liveFab != null) {
                liveFab.updateColors();
            }
            if (storiesAdapter != null) {
                storiesAdapter.notifyDataSetChanged();
            }
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
            if (statusHeader != null) {
                statusHeader.setTextColor(Theme.getColor(Theme.key_chats_menuItemText));
            }
            if (channelsHeaderTextView != null) {
                channelsHeaderTextView.setTextColor(Theme.getColor(Theme.key_chats_menuItemText));
            }
            if (channelsArchiveButton != null) {
                channelsArchiveButton.setTextColor(Theme.getColor(Theme.key_chats_menuItemText));
            }
            if (statusDrawable != null) {
                statusDrawable.setColor(Theme.getColor(Theme.key_profile_verifiedBackground));
            }
            if (noChannelsView != null) {
                noChannelsView.title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                noChannelsView.subtitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            }
        };

        // Root layouts, list, container, and ActionBar
        themeDescriptions.add(new org.telegram.ui.ActionBar.ThemeDescription(fragmentView, org.telegram.ui.ActionBar.ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundWhite));
        themeDescriptions.add(new org.telegram.ui.ActionBar.ThemeDescription(listView, org.telegram.ui.ActionBar.ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundWhite));
        themeDescriptions.add(new org.telegram.ui.ActionBar.ThemeDescription(storiesContainer, org.telegram.ui.ActionBar.ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundWhite));
        themeDescriptions.add(new org.telegram.ui.ActionBar.ThemeDescription(actionBar, org.telegram.ui.ActionBar.ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundWhite));
        themeDescriptions.add(new org.telegram.ui.ActionBar.ThemeDescription(listView, org.telegram.ui.ActionBar.ThemeDescription.FLAG_LISTGLOWCOLOR, null, null, null, null, Theme.key_actionBarDefault));
        themeDescriptions.add(new org.telegram.ui.ActionBar.ThemeDescription(actionBar, org.telegram.ui.ActionBar.ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_actionBarDefaultIcon));
        themeDescriptions.add(new org.telegram.ui.ActionBar.ThemeDescription(actionBar, org.telegram.ui.ActionBar.ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new org.telegram.ui.ActionBar.ThemeDescription(actionBar, org.telegram.ui.ActionBar.ThemeDescription.FLAG_AB_SUBTITLECOLOR, null, null, null, null, Theme.key_windowBackgroundWhiteGrayText2));
        themeDescriptions.add(new org.telegram.ui.ActionBar.ThemeDescription(actionBar, org.telegram.ui.ActionBar.ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_actionBarDefaultSelector));
        themeDescriptions.add(new org.telegram.ui.ActionBar.ThemeDescription(actionBar, org.telegram.ui.ActionBar.ThemeDescription.FLAG_AB_SEARCH, null, null, null, null, Theme.key_actionBarDefaultSearch));
        themeDescriptions.add(new org.telegram.ui.ActionBar.ThemeDescription(actionBar, org.telegram.ui.ActionBar.ThemeDescription.FLAG_AB_SEARCHPLACEHOLDER, null, null, null, null, Theme.key_actionBarDefaultSearchPlaceholder));

        // Submenu popup colors animations
        themeDescriptions.add(new org.telegram.ui.ActionBar.ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_actionBarDefaultSubmenuBackground));
        themeDescriptions.add(new org.telegram.ui.ActionBar.ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_actionBarDefaultSubmenuItem));
        themeDescriptions.add(new org.telegram.ui.ActionBar.ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_actionBarDefaultSubmenuItemIcon));
        themeDescriptions.add(new org.telegram.ui.ActionBar.ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_dialogButtonSelector));

        // Floating action buttons
        if (cameraFab != null && cameraFab.imageView != null) {
            themeDescriptions.add(new org.telegram.ui.ActionBar.ThemeDescription(cameraFab.imageView, org.telegram.ui.ActionBar.ThemeDescription.FLAG_IMAGECOLOR, null, null, null, null, Theme.key_chats_actionIcon));
            themeDescriptions.add(new org.telegram.ui.ActionBar.ThemeDescription(cameraFab.imageView, org.telegram.ui.ActionBar.ThemeDescription.FLAG_BACKGROUNDFILTER, null, null, null, null, Theme.key_chats_actionBackground));
            themeDescriptions.add(new org.telegram.ui.ActionBar.ThemeDescription(cameraFab.imageView, org.telegram.ui.ActionBar.ThemeDescription.FLAG_BACKGROUNDFILTER | org.telegram.ui.ActionBar.ThemeDescription.FLAG_DRAWABLESELECTEDSTATE, null, null, null, null, Theme.key_chats_actionPressedBackground));
        }
        if (liveFab != null && liveFab.imageView != null) {
            themeDescriptions.add(new org.telegram.ui.ActionBar.ThemeDescription(liveFab.imageView, org.telegram.ui.ActionBar.ThemeDescription.FLAG_IMAGECOLOR, null, null, null, null, Theme.key_chats_actionIcon));
            themeDescriptions.add(new org.telegram.ui.ActionBar.ThemeDescription(liveFab.imageView, org.telegram.ui.ActionBar.ThemeDescription.FLAG_BACKGROUNDFILTER, null, null, null, null, Theme.key_chats_actionBackground));
            themeDescriptions.add(new org.telegram.ui.ActionBar.ThemeDescription(liveFab.imageView, org.telegram.ui.ActionBar.ThemeDescription.FLAG_BACKGROUNDFILTER | org.telegram.ui.ActionBar.ThemeDescription.FLAG_DRAWABLESELECTEDSTATE, null, null, null, null, Theme.key_chats_actionPressedBackground));
        }

        // ListView selector & properties
        themeDescriptions.add(new org.telegram.ui.ActionBar.ThemeDescription(listView, org.telegram.ui.ActionBar.ThemeDescription.FLAG_SELECTOR, null, null, null, null, Theme.key_listSelector));
        themeDescriptions.add(new org.telegram.ui.ActionBar.ThemeDescription(listView, 0, new Class[]{org.telegram.ui.Cells.DialogCell.class}, new String[]{"namePaint"}, null, null, cellDelegate, Theme.key_chats_name));
        themeDescriptions.add(new org.telegram.ui.ActionBar.ThemeDescription(listView, 0, new Class[]{org.telegram.ui.Cells.DialogCell.class}, new String[]{"messagePaint"}, null, null, cellDelegate, Theme.key_chats_message));

        // empty placeholder
        themeDescriptions.add(new org.telegram.ui.ActionBar.ThemeDescription(noChannelsView, 0, new Class[]{org.telegram.ui.Components.StickerEmptyView.class}, new String[]{"title"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new org.telegram.ui.ActionBar.ThemeDescription(noChannelsView, 0, new Class[]{org.telegram.ui.Components.StickerEmptyView.class}, new String[]{"subtitle"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText));

        return themeDescriptions;
    }
}
