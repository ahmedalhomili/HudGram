package com.hudgram.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import androidx.collection.LongSparseArray;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.ScaleStateListAnimator;
import org.telegram.ui.Components.Switch;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Cells.DialogCell;

import java.util.ArrayList;
import java.util.Collections;

public class HudDraftsActivity extends BaseHudSettingsActivity {

    private CategoryTabsView tabsView;
    private int selectedTab = 0; // 0: All, 1: DMs, 2: Groups, 3: Channels, 4: Bots
    private final ArrayList<Long> filteredDialogs = new ArrayList<>();

    @Override
    public boolean onFragmentCreate() {
        return super.onFragmentCreate();
    }

    @Override
    public ActionBar createActionBar(Context context) {
        ActionBar actionBar = super.createActionBar(context);
        ActionBarMenu menu = actionBar.createMenu();

        Switch switchView = new Switch(context);
        switchView.setClickable(false);
        switchView.setFocusable(false);
        switchView.setColors(Theme.key_switchTrack, Theme.key_switchTrackChecked, Theme.key_windowBackgroundWhite, Theme.key_windowBackgroundWhite);
        switchView.setChecked(HudConfig.draftsManagerEnabled, false);
        switchView.setOnCheckedChangeListener((view, isChecked) -> {
            if (HudConfig.draftsManagerEnabled != isChecked) {
                HudConfig.toggleDraftsManagerEnabled();
                if (tabsView != null) {
                    tabsView.updateTabs();
                }
                listView.adapter.update(true);
                BulletinFactory.of(this).createSimpleBulletin(
                    isChecked ? R.drawable.msg_saved : R.drawable.msg_close,
                    getString(isChecked ? "HudDraftsEnabled" : "HudDraftsDisabled")
                ).show();
            }
        });

        ActionBarMenuItem menuItem = menu.addItem(2, 0);
        menuItem.removeAllViews();
        menuItem.addView(switchView, LayoutHelper.createFrame(37, 50, Gravity.CENTER));
        menuItem.setOnClickListener(v -> {
            switchView.setChecked(!switchView.isChecked(), true);
        });

        // Clear All button in the top action bar
        ActionBarMenuItem clearAllItem = menu.addItem(1, R.drawable.msg_delete, resourceProvider);
        clearAllItem.setContentDescription(getString("HudDraftsClearAll"));
        clearAllItem.setOnClickListener(v -> showClearAllConfirmation());

        return actionBar;
    }

    @Override
    public View createView(Context context) {
        // Tabs view
        tabsView = new CategoryTabsView(context);
        tabsView.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        return super.createView(context);
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asShadow(getString("HudDraftsInfo")));

        if (!HudConfig.draftsManagerEnabled) {
            return;
        }

        items.add(UItem.asCustom(tabsView));

        filteredDialogs.clear();
        MediaDataController controller = getMediaDataController();
        if (controller != null) {
            LongSparseArray<LongSparseArray<TLRPC.DraftMessage>> allDrafts = controller.getDrafts();
            if (allDrafts != null) {
                for (int i = 0; i < allDrafts.size(); i++) {
                    long dialogId = allDrafts.keyAt(i);
                    LongSparseArray<TLRPC.DraftMessage> threads = allDrafts.valueAt(i);
                    if (threads == null || threads.size() == 0) continue;

                    boolean hasContent = false;
                    for (int j = 0; j < threads.size(); j++) {
                        TLRPC.DraftMessage dm = threads.valueAt(j);
                        if (dm != null && !TextUtils.isEmpty(dm.message)) {
                            hasContent = true;
                            break;
                        }
                    }
                    if (!hasContent) continue;

                    boolean match = false;
                    if (selectedTab == 0) {
                        match = true;
                    } else if (selectedTab == 1) {
                        if (DialogObject.isUserDialog(dialogId)) {
                            TLRPC.User user = getMessagesController().getUser(dialogId);
                            if (user == null || !user.bot) {
                                match = true;
                            }
                        }
                    } else if (selectedTab == 2) {
                        if (DialogObject.isChatDialog(dialogId)) {
                            TLRPC.Chat chat = getMessagesController().getChat(-dialogId);
                            if (chat != null && (!ChatObject.isChannel(chat) || chat.megagroup)) {
                                match = true;
                            }
                        }
                    } else if (selectedTab == 3) {
                        if (DialogObject.isChatDialog(dialogId)) {
                            TLRPC.Chat chat = getMessagesController().getChat(-dialogId);
                            if (chat != null && ChatObject.isChannel(chat) && !chat.megagroup) {
                                match = true;
                            }
                        }
                    } else if (selectedTab == 4) {
                        if (DialogObject.isUserDialog(dialogId)) {
                            TLRPC.User user = getMessagesController().getUser(dialogId);
                            if (user != null && user.bot) {
                                match = true;
                            }
                        }
                    }

                    if (match) {
                        filteredDialogs.add(dialogId);
                    }
                }
            }
        }

        // Sort: newest first
        Collections.sort(filteredDialogs, (o1, o2) -> {
            LongSparseArray<TLRPC.DraftMessage> threads1 = getMediaDataController().getDrafts().get(o1);
            LongSparseArray<TLRPC.DraftMessage> threads2 = getMediaDataController().getDrafts().get(o2);
            int date1 = (threads1 != null && threads1.size() > 0) ? threads1.valueAt(0).date : 0;
            int date2 = (threads2 != null && threads2.size() > 0) ? threads2.valueAt(0).date : 0;
            return Integer.compare(date2, date1);
        });

        if (filteredDialogs.isEmpty()) {
            items.add(UItem.asShadow(getString("HudDraftsEmpty")));
        } else {
            for (int i = 0; i < filteredDialogs.size(); i++) {
                items.add(DraftDialogCellFactory.of(filteredDialogs.get(i)));
            }
        }
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        if (item.instanceOf(DraftDialogCellFactory.class)) {
            showOptionsDialog(item.dialogId);
        }
    }

    @Override
    protected boolean onItemLongClick(UItem item, View view, int position, float x, float y) {
        if (item.instanceOf(DraftDialogCellFactory.class)) {
            showOptionsDialog(item.dialogId);
            return true;
        }
        return false;
    }

    private void showOptionsDialog(long dialogId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        
        CharSequence title = "";
        if (DialogObject.isUserDialog(dialogId)) {
            TLRPC.User user = getMessagesController().getUser(dialogId);
            title = user != null ? user.first_name : "";
        } else {
            TLRPC.Chat chat = getMessagesController().getChat(-dialogId);
            title = chat != null ? chat.title : "";
        }
        builder.setTitle(title);

        CharSequence[] options = new CharSequence[]{
            LocaleController.getString("Open", R.string.Open),
            getString("HudDraftsSendInstantly"),
            getString("HudDraftsCopyText"),
            getString("HudDraftsClearDraft")
        };

        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                openChat(dialogId);
            } else if (which == 1) {
                sendDraftInstantly(dialogId);
            } else if (which == 2) {
                copyDraftText(dialogId);
            } else if (which == 3) {
                clearSingleDraft(dialogId);
            }
        });
        showDialog(builder.create());
    }

    private void openChat(long dialogId) {
        Bundle args = new Bundle();
        if (DialogObject.isEncryptedDialog(dialogId)) {
            args.putInt("enc_id", DialogObject.getEncryptedChatId(dialogId));
        } else if (DialogObject.isUserDialog(dialogId)) {
            args.putLong("user_id", dialogId);
        } else {
            args.putLong("chat_id", -dialogId);
        }
        presentFragment(new ChatActivity(args));
    }

    private void sendDraftInstantly(long dialogId) {
        LongSparseArray<TLRPC.DraftMessage> threads = getMediaDataController().getDrafts().get(dialogId);
        if (threads == null || threads.size() == 0) return;

        long threadId = threads.keyAt(0);
        TLRPC.DraftMessage draft = threads.valueAt(0);
        if (draft == null || TextUtils.isEmpty(draft.message)) return;

        SendMessagesHelper.SendMessageParams params = SendMessagesHelper.SendMessageParams.of(draft.message, dialogId);
        params.entities = draft.entities;

        SendMessagesHelper.getInstance(currentAccount).sendMessage(params);

        // Delete draft from memory and sync to server
        getMediaDataController().saveDraft(dialogId, (int) threadId, "", null, null, true, 0);
        getMediaDataController().cleanDraft(dialogId, threadId, false);
        TLRPC.Dialog dialog = getMessagesController().dialogs_dict.get(dialogId);
        if (dialog != null) {
            dialog.draft = null;
        }

        tabsView.updateTabs();
        listView.adapter.update(true);

        BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_send, getString("HudDraftsSendSuccess")).show();
    }

    private void copyDraftText(long dialogId) {
        LongSparseArray<TLRPC.DraftMessage> threads = getMediaDataController().getDrafts().get(dialogId);
        if (threads == null || threads.size() == 0) return;

        TLRPC.DraftMessage draft = threads.valueAt(0);
        if (draft == null || TextUtils.isEmpty(draft.message)) return;

        AndroidUtilities.addToClipboard(draft.message);
        BulletinFactory.of(this).createCopyLinkBulletin().show();
    }

    private void clearSingleDraft(long dialogId) {
        LongSparseArray<TLRPC.DraftMessage> threads = getMediaDataController().getDrafts().get(dialogId);
        if (threads != null) {
            for (int i = 0; i < threads.size(); i++) {
                long threadId = threads.keyAt(i);
                getMediaDataController().saveDraft(dialogId, (int) threadId, "", null, null, true, 0);
                getMediaDataController().cleanDraft(dialogId, threadId, false);
            }
        }
        TLRPC.Dialog dialog = getMessagesController().dialogs_dict.get(dialogId);
        if (dialog != null) {
            dialog.draft = null;
        }
        tabsView.updateTabs();
        listView.adapter.update(true);
    }

    private void showClearAllConfirmation() {
        if (filteredDialogs.isEmpty()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(getString("HudDraftsClearAll"));
        builder.setMessage(getString("HudDraftsClearAllConfirm"));
        builder.setPositiveButton(LocaleController.getString("Delete", R.string.Delete), (dialog, which) -> {
            for (long dialogId : filteredDialogs) {
                LongSparseArray<TLRPC.DraftMessage> threads = getMediaDataController().getDrafts().get(dialogId);
                if (threads != null) {
                    for (int i = 0; i < threads.size(); i++) {
                        long threadId = threads.keyAt(i);
                        getMediaDataController().saveDraft(dialogId, (int) threadId, "", null, null, true, 0);
                        getMediaDataController().cleanDraft(dialogId, threadId, false);
                    }
                }
                TLRPC.Dialog d = getMessagesController().dialogs_dict.get(dialogId);
                if (d != null) {
                    d.draft = null;
                }
            }
            tabsView.updateTabs();
            listView.adapter.update(true);
            BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_delete, getString("HudDraftsClearAllSuccess")).show();
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);

        AlertDialog dialog = builder.create();
        showDialog(dialog);

        TextView button = (TextView) dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (button != null) {
            button.setTextColor(getThemedColor(Theme.key_text_RedBold));
        }
    }

    @Override
    protected String getActionBarTitle() {
        return getString("HudDraftsTitle");
    }

    @Override
    protected String getKey() {
        return "drafts";
    }

    // Custom cell factory for dialog cells
    protected static class DraftDialogCellFactory extends UItem.UItemFactory<DialogCell> {
        static {
            setup(new DraftDialogCellFactory());
        }

        @Override
        public DialogCell createView(Context context, org.telegram.ui.Components.RecyclerListView listView, int currentAccount, int classGuid, Theme.ResourcesProvider resourcesProvider) {
            return new DialogCell(null, context, false, false, currentAccount, resourcesProvider);
        }

        @Override
        public void bindView(View view, UItem item, boolean divider, UniversalAdapter adapter, org.telegram.ui.Components.UniversalRecyclerView listView) {
            DialogCell cell = (DialogCell) view;
            long dialogId = item.dialogId;

            int currentAccount = adapter.currentAccount;
            TLRPC.Dialog dialog = MessagesController.getInstance(currentAccount).dialogs_dict.get(dialogId);
            if (dialog == null) {
                dialog = new TLRPC.TL_dialog();
                dialog.id = dialogId;
                LongSparseArray<TLRPC.DraftMessage> threads = MediaDataController.getInstance(currentAccount).getDrafts().get(dialogId);
                if (threads != null && threads.size() > 0) {
                    dialog.draft = threads.valueAt(0);
                    dialog.last_message_date = dialog.draft.date;
                }
            }

            cell.useSeparator = divider;
            cell.setDialog(dialog, 0, 0);
        }

        public static UItem of(long dialogId) {
            UItem item = UItem.ofFactory(DraftDialogCellFactory.class);
            item.dialogId = dialogId;
            return item;
        }
    }

    // Category Tabs View class
    private class CategoryTabsView extends FrameLayout {
        private final LinearLayout container;
        private final ArrayList<TextView> tabViews = new ArrayList<>();
        private final String[] tabKeys = new String[]{
            "HudDraftsTabAll",
            "HudDraftsTabPrivate",
            "HudDraftsTabGroups",
            "HudDraftsTabChannels",
            "HudDraftsTabBots"
        };

        public CategoryTabsView(Context context) {
            super(context);

            android.widget.HorizontalScrollView scrollView = new android.widget.HorizontalScrollView(context);
            scrollView.setHorizontalScrollBarEnabled(false);
            scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
            addView(scrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            container = new LinearLayout(context);
            container.setOrientation(LinearLayout.HORIZONTAL);
            container.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(8), AndroidUtilities.dp(16), AndroidUtilities.dp(8));
            scrollView.addView(container, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT));

            updateTabs();
        }

        public void updateTabs() {
            container.removeAllViews();
            tabViews.clear();

            int[] counts = new int[5];
            MediaDataController controller = getMediaDataController();
            if (controller != null) {
                LongSparseArray<LongSparseArray<TLRPC.DraftMessage>> allDrafts = controller.getDrafts();
                if (allDrafts != null) {
                    for (int i = 0; i < allDrafts.size(); i++) {
                        long dialogId = allDrafts.keyAt(i);
                        LongSparseArray<TLRPC.DraftMessage> threads = allDrafts.valueAt(i);
                        if (threads == null || threads.size() == 0) continue;

                        boolean hasContent = false;
                        for (int j = 0; j < threads.size(); j++) {
                            TLRPC.DraftMessage dm = threads.valueAt(j);
                            if (dm != null && !TextUtils.isEmpty(dm.message)) {
                                hasContent = true;
                                break;
                            }
                        }
                        if (!hasContent) continue;

                        counts[0]++; // All

                        if (DialogObject.isUserDialog(dialogId)) {
                            TLRPC.User user = getMessagesController().getUser(dialogId);
                            if (user != null && user.bot) {
                                counts[4]++; // Bots
                            } else {
                                counts[1]++; // Private
                            }
                        } else if (DialogObject.isChatDialog(dialogId)) {
                            TLRPC.Chat chat = getMessagesController().getChat(-dialogId);
                            if (chat != null) {
                                if (ChatObject.isChannel(chat) && !chat.megagroup) {
                                    counts[3]++; // Channels
                                } else {
                                    counts[2]++; // Groups
                                }
                            }
                        }
                    }
                }
            }

            for (int i = 0; i < 5; i++) {
                final int index = i;
                TextView tabView = new TextView(getContext());
                tabView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
                tabView.setTypeface(Typeface.DEFAULT_BOLD);
                tabView.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(8), AndroidUtilities.dp(16), AndroidUtilities.dp(8));

                String label = getString(tabKeys[i]);
                if (counts[i] > 0) {
                    label += " (" + counts[i] + ")";
                }
                tabView.setText(label);

                int activeColor = getThemedColor(Theme.key_featuredStickers_addButton);
                int inactiveColor = getThemedColor(Theme.key_windowBackgroundGray);

                if (selectedTab == index) {
                    tabView.setTextColor(0xFFFFFFFF);
                    tabView.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(16), activeColor));
                } else {
                    tabView.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
                    tabView.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(16), inactiveColor));
                }

                tabView.setOnClickListener(v -> {
                    if (selectedTab != index) {
                        selectedTab = index;
                        updateTabs();
                        listView.adapter.update(true);
                    }
                });

                ScaleStateListAnimator.apply(tabView, 0.9f, 1.05f);

                container.addView(tabView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT,
                    LocaleController.isRTL ? 0 : 4, 0, LocaleController.isRTL ? 4 : 0, 0));
                tabViews.add(tabView);
            }
        }
    }
}
