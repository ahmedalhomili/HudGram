package com.hudgram.ui.autoreply;
import com.hudgram.ui.settings.BaseHudSettingsActivity;
import com.hudgram.core.HudConfig;

import android.content.Context;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.UserCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class HudAutoReplyDMFilterActivity extends BaseHudSettingsActivity {

    public interface OnFilterChangedListener {
        void onFilterChanged(Set<String> chats);
    }

    private final ArrayList<DialogInfo> dialogs = new ArrayList<>();
    private final Set<String> selectedChats;
    private final int filterMode;
    private final OnFilterChangedListener listener;

    private static class DialogInfo {
        long dialogId;
        String title;
        DialogInfo(long dialogId, String title) {
            this.dialogId = dialogId;
            this.title = title;
        }
    }

    public HudAutoReplyDMFilterActivity(Set<String> selectedChats, int filterMode, OnFilterChangedListener listener) {
        super();
        this.selectedChats = selectedChats != null ? new HashSet<>(selectedChats) : new HashSet<>();
        this.filterMode = filterMode;
        this.listener = listener;
    }

    @Override
    public boolean onFragmentCreate() {
        loadDialogs();
        return super.onFragmentCreate();
    }

    private void loadDialogs() {
        dialogs.clear();
        int currentAccount = UserConfig.selectedAccount;
        MessagesController messagesController = MessagesController.getInstance(currentAccount);

        // Load main folder dialogs
        ArrayList<TLRPC.Dialog> allDialogs = messagesController.getDialogs(0);
        for (TLRPC.Dialog dialog : allDialogs) {
            addDialogIfNeeded(dialog, messagesController);
        }

        // Load archived folder dialogs
        ArrayList<TLRPC.Dialog> archivedDialogs = messagesController.getDialogs(1);
        for (TLRPC.Dialog dialog : archivedDialogs) {
            addDialogIfNeeded(dialog, messagesController);
        }
    }

    private void addDialogIfNeeded(TLRPC.Dialog dialog, MessagesController messagesController) {
        // Check for duplicates
        for (DialogInfo d : dialogs) {
            if (d.dialogId == dialog.id) {
                return;
            }
        }

        if (dialog.id > 0) {
            // Private chat (user)
            TLRPC.User user = messagesController.getUser(dialog.id);
            if (user != null && !user.bot && !user.self) {
                String name = ContactsController.formatName(user.first_name, user.last_name);
                dialogs.add(new DialogInfo(dialog.id, name));
            }
        } else if (dialog.id < 0) {
            // Group or channel
            TLRPC.Chat chat = messagesController.getChat(-dialog.id);
            if (chat != null && (!ChatObject.isChannel(chat) || ChatObject.isMegagroup(chat))) {
                // Groups and supergroups only (not broadcast channels)
                String title = chat.title != null ? chat.title : "";
                dialogs.add(new DialogInfo(dialog.id, title));
            }
        }
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        String filterDescription = filterMode == 1
                ? getString("HudAutoReplyFilterWhitelist")
                : getString("HudAutoReplyFilterBlacklist");
        items.add(UItem.asShadow(filterDescription));

        if (dialogs.isEmpty()) {
            items.add(UItem.asShadow(getString("HudAutoReplyLogEmpty")));
            return;
        }

        for (int i = 0; i < dialogs.size(); i++) {
            DialogInfo info = dialogs.get(i);
            boolean isSelected = selectedChats.contains(String.valueOf(info.dialogId));
            items.add(UItem.asFilterChat(true, info.dialogId).setId(100 + i).setChecked(isSelected));
        }
        items.add(UItem.asShadow(getString("HudAutoReplyFilterAbout")));
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        if (item.id >= 100) {
            int index = item.id - 100;
            if (index >= 0 && index < dialogs.size()) {
                DialogInfo info = dialogs.get(index);
                String dialogIdStr = String.valueOf(info.dialogId);

                if (selectedChats.contains(dialogIdStr)) {
                    selectedChats.remove(dialogIdStr);
                } else {
                    selectedChats.add(dialogIdStr);
                }

                if (listener != null) {
                    listener.onFilterChanged(selectedChats);
                }

                if (view instanceof UserCell) {
                    ((UserCell) view).setChecked(selectedChats.contains(dialogIdStr), true);
                }
            }
        }
    }

    @Override
    protected String getActionBarTitle() {
        return getString("HudAutoReplyDMFilterManage");
    }

    @Override
    protected String getKey() {
        return "ardmf";
    }
}
