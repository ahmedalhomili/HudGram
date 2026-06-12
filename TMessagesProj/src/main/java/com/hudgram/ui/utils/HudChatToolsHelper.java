package com.hudgram.ui.utils;

import android.app.Activity;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;

public class HudChatToolsHelper {

    public static void openToolsBottomSheet(BaseFragment parentFragment, int currentAccount, Theme.ResourcesProvider resourceProvider) {
        if (parentFragment == null || parentFragment.getParentActivity() == null) {
            return;
        }
        Activity activity = parentFragment.getParentActivity();
        BottomSheet.Builder builder = new BottomSheet.Builder(activity, false, resourceProvider);

        CharSequence[] items = new CharSequence[]{
            LocaleController.getString("HudQuickReplyTitle", R.string.HudQuickReplyTitle),
            LocaleController.getString("HudAutoReplyTitle", R.string.HudAutoReplyTitle),
            LocaleController.getString("HudAutoReplyDMTitle", R.string.HudAutoReplyDMTitle),
            LocaleController.getString("HudDraftsTitle", R.string.HudDraftsTitle),
            LocaleController.getString("HudScheduledMessagesTitle", R.string.HudScheduledMessagesTitle),
            LocaleController.getString("HudMessageByNumber", R.string.HudMessageByNumber)
        };

        int[] icons = new int[]{
            R.drawable.msg_reply_small,
            R.drawable.msg_mention,
            R.drawable.msg_bots,
            R.drawable.msg_edit,
            R.drawable.msg_calendar2,
            R.drawable.msg_contacts
        };

        builder.setItems(items, icons, (dialogInterface, i) -> {
            if (i == 0) {
                parentFragment.presentFragment(new com.hudgram.ui.quickreply.HudQuickReplyActivity());
            } else if (i == 1) {
                parentFragment.presentFragment(new com.hudgram.ui.autoreply.HudAutoReplyActivity());
            } else if (i == 2) {
                parentFragment.presentFragment(new com.hudgram.ui.autoreply.HudAutoReplyDMActivity());
            } else if (i == 3) {
                parentFragment.presentFragment(new com.hudgram.ui.drafts.HudDraftsActivity());
            } else if (i == 4) {
                parentFragment.presentFragment(new com.hudgram.ui.scheduledmessages.HudScheduledMessagesActivity());
            } else if (i == 5) {
                com.hudgram.ui.utils.ContactSearchUiHelper.showSearchDialog(
                    activity,
                    currentAccount,
                    resourceProvider,
                    parentFragment.getParentLayout()
                );
            }
        });

        parentFragment.showDialog(builder.create());
    }
}
