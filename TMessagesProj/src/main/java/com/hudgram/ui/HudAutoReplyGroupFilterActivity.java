package com.hudgram.ui;

import android.content.Context;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
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

public class HudAutoReplyGroupFilterActivity extends BaseHudSettingsActivity {

    private final ArrayList<GroupInfo> groups = new ArrayList<>();
    private Set<String> selectedGroups;

    private static class GroupInfo {
        long dialogId;
        String title;
        GroupInfo(long dialogId, String title) {
            this.dialogId = dialogId;
            this.title = title;
        }
    }

    @Override
    public boolean onFragmentCreate() {
        selectedGroups = new HashSet<>(HudConfig.getAutoReplyFilterGroups());
        loadGroups();
        return super.onFragmentCreate();
    }

    private void loadGroups() {
        groups.clear();
        int currentAccount = UserConfig.selectedAccount;
        MessagesController messagesController = MessagesController.getInstance(currentAccount);
        ArrayList<TLRPC.Dialog> allDialogs = messagesController.getDialogs(0);

        for (TLRPC.Dialog dialog : allDialogs) {
            if (dialog.id < 0) {
                // It's a group/channel
                TLRPC.Chat chat = messagesController.getChat(-dialog.id);
                if (chat != null && !ChatObject.isChannel(chat) || (chat != null && ChatObject.isMegagroup(chat))) {
                    // Groups and supergroups only (not channels)
                    String title = chat.title != null ? chat.title : "";
                    groups.add(new GroupInfo(dialog.id, title));
                }
            }
        }

        // Also check archived
        ArrayList<TLRPC.Dialog> archivedDialogs = messagesController.getDialogs(1);
        for (TLRPC.Dialog dialog : archivedDialogs) {
            if (dialog.id < 0) {
                TLRPC.Chat chat = messagesController.getChat(-dialog.id);
                if (chat != null && !ChatObject.isChannel(chat) || (chat != null && ChatObject.isMegagroup(chat))) {
                    String title = chat.title != null ? chat.title : "";
                    boolean alreadyAdded = false;
                    for (GroupInfo g : groups) {
                        if (g.dialogId == dialog.id) {
                            alreadyAdded = true;
                            break;
                        }
                    }
                    if (!alreadyAdded) {
                        groups.add(new GroupInfo(dialog.id, title));
                    }
                }
            }
        }
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        String filterDescription = HudConfig.autoReplyFilterMode == 1
                ? getString("HudAutoReplyFilterWhitelist")
                : getString("HudAutoReplyFilterBlacklist");
        items.add(UItem.asShadow(filterDescription));

        if (groups.isEmpty()) {
            items.add(UItem.asShadow(getString("HudAutoReplyLogEmpty")));
            return;
        }

        for (int i = 0; i < groups.size(); i++) {
            GroupInfo group = groups.get(i);
            boolean isSelected = selectedGroups.contains(String.valueOf(group.dialogId));
            items.add(UItem.asFilterChat(true, group.dialogId).setId(100 + i).setChecked(isSelected));
        }
        items.add(UItem.asShadow(getString("HudAutoReplyFilterAbout")));
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        if (item.id >= 100) {
            int index = item.id - 100;
            if (index >= 0 && index < groups.size()) {
                GroupInfo group = groups.get(index);
                String groupIdStr = String.valueOf(group.dialogId);

                if (selectedGroups.contains(groupIdStr)) {
                    selectedGroups.remove(groupIdStr);
                } else {
                    selectedGroups.add(groupIdStr);
                }

                HudConfig.setAutoReplyFilterGroups(selectedGroups);

                if (view instanceof UserCell) {
                    ((UserCell) view).setChecked(selectedGroups.contains(groupIdStr), true);
                }
            }
        }
    }

    @Override
    protected String getActionBarTitle() {
        return getString("HudAutoReplyFilterManage");
    }

    @Override
    protected String getKey() {
        return "argf";
    }
}
