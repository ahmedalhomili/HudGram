package com.hudgram.ui.scheduledmessages;

import com.hudgram.core.HudConfig;
import com.hudgram.ui.settings.BaseHudSettingsActivity;
import com.hudgram.core.HudScheduledMessagesManager;
import com.hudgram.core.HudScheduledMessagesManager.ScheduledMessage;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.os.Build;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.Switch;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.UniversalRecyclerView;
import org.telegram.ui.Components.RecyclerListView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class HudScheduledMessagesActivity extends BaseHudSettingsActivity {

    private ArrayList<ScheduledMessage> scheduledMessages = new ArrayList<>();
    private FrameLayout fab;
    private Switch actionBarSwitch;

    private final int logRow = 500;
    private final int logClearRow = 501;

    @Override
    public boolean onFragmentCreate() {
        loadMessages();
        return super.onFragmentCreate();
    }

    private void loadMessages() {
        scheduledMessages = HudScheduledMessagesManager.getScheduledMessages();
    }

    @Override
    public ActionBar createActionBar(Context context) {
        ActionBar actionBar = super.createActionBar(context);
        ActionBarMenu menu = actionBar.createMenu();

        // Help info item (ID = 2)
        menu.addItem(2, R.drawable.msg_info);

        actionBarSwitch = new Switch(context);
        actionBarSwitch.setColors(Theme.key_switchTrack, Theme.key_switchTrackChecked, Theme.key_windowBackgroundWhite, Theme.key_windowBackgroundWhite);
        actionBarSwitch.setChecked(HudConfig.scheduledMessagesEnabled, false);
        actionBarSwitch.setOnCheckedChangeListener((view, isChecked) -> {
            if (HudConfig.scheduledMessagesEnabled != isChecked) {
                HudConfig.toggleScheduledMessagesEnabled();
                updateFabVisibility();
                listView.adapter.update(true);
                BulletinFactory.of(this).createSimpleBulletin(
                    isChecked ? R.drawable.msg_saved : R.drawable.msg_close,
                    getString(isChecked ? "HudScheduledMessagesEnabled" : "HudScheduledMessagesDisabled")
                ).show();
            }
        });

        // Create a menu item wrapper to give the switch a native clickable area and touch selector background
        ActionBarMenuItem menuItem = menu.addItem(1, 0);
        menuItem.removeAllViews();
        menuItem.addView(actionBarSwitch, LayoutHelper.createFrame(37, 50, Gravity.CENTER));
        menuItem.setOnClickListener(v -> {
            actionBarSwitch.setChecked(!actionBarSwitch.isChecked(), true);
        });

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == 2) {
                    showHelpDialog();
                }
            }
        });

        return actionBar;
    }

    private void showHelpDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(getString("HudScheduledMessagesHelpTitle"));
        builder.setMessage(getString("HudScheduledMessagesHelpText"));
        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
        showDialog(builder.create());
    }

    private void updateFabVisibility() {
        if (fab != null) {
            fab.setVisibility(HudConfig.scheduledMessagesEnabled ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public View createView(Context context) {
        super.createView(context);

        // Green Floating Action Button (FAB) at bottom right - matching Quick Replies Style (RoundRect)
        fab = new FrameLayout(context);
        int fabColor = getThemedColor(Theme.key_featuredStickers_addButton);
        int fabPressedColor = getThemedColor(Theme.key_featuredStickers_addButtonPressed);
        if (fabPressedColor == 0) {
            fabPressedColor = Theme.blendOver(fabColor, 0x1A000000);
        }
        fab.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(12), fabColor, fabPressedColor));
        
        if (Build.VERSION.SDK_INT >= 21) {
            fab.setElevation(AndroidUtilities.dp(4));
            fab.setOutlineProvider(new android.view.ViewOutlineProvider() {
                @Override
                public void getOutline(View view, android.graphics.Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), AndroidUtilities.dp(12));
                }
            });
        }

        ImageView fabIcon = new ImageView(context);
        fabIcon.setImageResource(R.drawable.msg_add);
        fabIcon.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_chats_actionIcon), PorterDuff.Mode.SRC_IN));
        fab.addView(fabIcon, LayoutHelper.createFrame(24, 24, Gravity.CENTER));

        FrameLayout.LayoutParams lp = LayoutHelper.createFrame(56, 56, 
                Gravity.BOTTOM | Gravity.END, 
                16, 0, 16, 16);
        contentView.addView(fab, lp);

        fab.setOnClickListener(v -> {
            presentFragment(new HudScheduledMessageAddActivity());
        });

        org.telegram.ui.Components.ScaleStateListAnimator.apply(fab, 0.85f, 1.2f);
        updateFabVisibility();

        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMessages();
        updateFabVisibility();
        if (actionBarSwitch != null) {
            actionBarSwitch.setChecked(HudConfig.scheduledMessagesEnabled, false);
        }
        if (listView != null) {
            listView.adapter.update(true);
        }
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        // Descriptive Header card about scheduled messages
        items.add(UItem.asShadow(getString("HudScheduledMessagesRowAbout")));

        if (!HudConfig.scheduledMessagesEnabled) {
            items.add(UItem.asShadow(getString("HudScheduledMessagesFeatureDisabled")));
            return;
        }

        // Message List Items
        for (int i = 0; i < scheduledMessages.size(); i++) {
            ScheduledMessage msg = scheduledMessages.get(i);
            UItem uItem = ScheduledMessageCellFactory.of(100 + i, msg);
            items.add(uItem);
        }

        if (scheduledMessages.isEmpty()) {
            items.add(UItem.asShadow(getString("HudScheduledMessagesEmpty")));
        } else {
            items.add(UItem.asShadow(null));
        }

        // === Sent History Logs Section ===
        items.add(UItem.asHeader(getString("HudScheduledMessagesHistoryHeader")));
        ArrayList<HudScheduledMessagesManager.ScheduledMessageLogEntry> logs = HudScheduledMessagesManager.getScheduledMessagesLog();
        String logCountText = logs.isEmpty() 
            ? getString("HudScheduledMessagesHistoryLogEmpty") 
            : LocaleController.formatPluralString("HudScheduledMessagesHistoryLogCount", logs.size());
        
        items.add(TextSettingsCellFactory.of(logRow, getString("HudScheduledMessagesHistoryLog"), logCountText).slug("log"));
        
        if (!logs.isEmpty()) {
            UItem clearItem = TextSettingsCellFactory.of(logClearRow, getString("HudScheduledMessagesHistoryClear"));
            clearItem.red = true;
            items.add(clearItem);
        }
        items.add(UItem.asShadow(null));
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        if (item.id == logRow) {
            presentFragment(new HudScheduledMessagesLogActivity());
        } else if (item.id == logClearRow) {
            HudScheduledMessagesManager.clearLog();
            listView.adapter.update(true);
            BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_delete, getString("HudScheduledMessagesHistoryCleared")).show();
        }
    }

    private void showDeleteConfirmation(int index) {
        if (index < 0 || index >= scheduledMessages.size()) return;
        ScheduledMessage target = scheduledMessages.get(index);

        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(getString("HudScheduledMessagesDeleteTitle"));
        builder.setMessage(getString("HudScheduledMessagesDeleteConfirm"));
        builder.setPositiveButton(LocaleController.getString("Delete", R.string.Delete), (dialog, which) -> {
            HudScheduledMessagesManager.deleteScheduledMessage(getParentActivity(), target.id);
            loadMessages();
            listView.adapter.update(true);
            BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_delete, getString("HudScheduledMessagesDeleted")).show();
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
        return getString("HudScheduledMessagesTitle");
    }

    @Override
    protected String getKey() {
        return "scheduledMessages";
    }

    // === Custom UItem Factory ===
    protected static class ScheduledMessageCellFactory extends UItem.UItemFactory<ScheduledMessageCell> {
        static {
            setup(new ScheduledMessageCellFactory());
        }

        @Override
        public ScheduledMessageCell createView(Context context, RecyclerListView listView, int currentAccount, int classGuid, Theme.ResourcesProvider resourcesProvider) {
            return new ScheduledMessageCell(context, currentAccount);
        }

        @Override
        public void bindView(View view, UItem item, boolean divider, UniversalAdapter adapter, UniversalRecyclerView listView) {
            ScheduledMessageCell cell = (ScheduledMessageCell) view;
            ScheduledMessage msg = (ScheduledMessage) item.object;
            cell.setData(msg);
            
            // Set up trash icon click listener
            cell.deleteView.setOnClickListener(v -> {
                HudScheduledMessagesActivity fragment = getFragment(listView);
                if (fragment != null) {
                    int index = item.id - 100;
                    fragment.showDeleteConfirmation(index);
                }
            });
        }

        private static HudScheduledMessagesActivity getFragment(View view) {
            Context context = view.getContext();
            while (context instanceof android.content.ContextWrapper) {
                if (context instanceof org.telegram.ui.LaunchActivity) {
                    break;
                }
                context = ((android.content.ContextWrapper) context).getBaseContext();
            }
            if (context instanceof org.telegram.ui.LaunchActivity) {
                org.telegram.ui.ActionBar.INavigationLayout layout = ((org.telegram.ui.LaunchActivity) context).getActionBarLayout();
                if (layout != null && layout.getLastFragment() instanceof HudScheduledMessagesActivity) {
                    return (HudScheduledMessagesActivity) layout.getLastFragment();
                }
            }
            return null;
        }

        public static UItem of(int id, ScheduledMessage msg) {
            UItem item = UItem.ofFactory(ScheduledMessageCellFactory.class);
            item.id = id;
            item.object = msg;
            return item;
        }
    }

    // === Custom Cell Layout (RoundRect Card) ===
    protected static class ScheduledMessageCell extends FrameLayout {
        private final int currentAccount;
        private final BackupImageView avatarView;
        private final AvatarDrawable avatarDrawable;
        private final TextView recipientsView;
        private final TextView dateTimeView;
        private final TextView messageView;
        private final ImageView calendarIcon;
        private final ImageView repeatIcon;
        public final ImageView deleteView;

        public ScheduledMessageCell(Context context, int currentAccount) {
            super(context);
            this.currentAccount = currentAccount;
            setBackgroundColor(0); // Transparent outer container to show grey bg behind card

            // White rounded card container
            FrameLayout cardContainer = new FrameLayout(context);
            cardContainer.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(12), Theme.getColor(Theme.key_windowBackgroundWhite)));
            addView(cardContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.LEFT, 16, 6, 16, 6));

            boolean isRtl = LocaleController.isRTL;

            // Avatar View
            avatarView = new BackupImageView(context);
            avatarView.setRoundRadius(AndroidUtilities.dp(21));
            cardContainer.addView(avatarView, LayoutHelper.createFrame(42, 42,
                    (isRtl ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL,
                    12, 12, 12, 12));
            avatarDrawable = new AvatarDrawable();

            // Trash delete button
            deleteView = new ImageView(context);
            deleteView.setImageResource(R.drawable.msg_delete);
            deleteView.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_text_RedBold), PorterDuff.Mode.SRC_IN));
            deleteView.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8));
            org.telegram.ui.Components.ScaleStateListAnimator.apply(deleteView, 0.8f, 1.2f);
            cardContainer.addView(deleteView, LayoutHelper.createFrame(36, 36,
                    (isRtl ? Gravity.LEFT : Gravity.RIGHT) | Gravity.CENTER_VERTICAL,
                    12, 12, 12, 12));

            // Text layout vertical
            LinearLayout textLayout = new LinearLayout(context);
            textLayout.setOrientation(LinearLayout.VERTICAL);

            int leftMargin = isRtl ? (12 + 36 + 12) : (12 + 42 + 12);
            int rightMargin = isRtl ? (12 + 42 + 12) : (12 + 36 + 12);
            cardContainer.addView(textLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT,
                    Gravity.CENTER_VERTICAL,
                    leftMargin, 12, rightMargin, 12));

            recipientsView = new TextView(context);
            recipientsView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            recipientsView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            recipientsView.setTypeface(Typeface.DEFAULT_BOLD);
            recipientsView.setSingleLine(true);
            recipientsView.setEllipsize(TextUtils.TruncateAt.END);
            recipientsView.setGravity(Gravity.START);
            textLayout.addView(recipientsView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            // Time and Repeat row
            LinearLayout timeRow = new LinearLayout(context);
            timeRow.setOrientation(LinearLayout.HORIZONTAL);
            timeRow.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
            textLayout.addView(timeRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 0));

            calendarIcon = new ImageView(context);
            calendarIcon.setImageResource(R.drawable.msg_calendar2);
            calendarIcon.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText2), PorterDuff.Mode.SRC_IN));

            dateTimeView = new TextView(context);
            dateTimeView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            dateTimeView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText2));
            dateTimeView.setTypeface(AndroidUtilities.bold());
            dateTimeView.setSingleLine(true);
            dateTimeView.setGravity(Gravity.START);

            repeatIcon = new ImageView(context);
            repeatIcon.setImageResource(R.drawable.msg_retry); // Native repeat/retry icon
            repeatIcon.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGreenText), PorterDuff.Mode.SRC_IN));

            if (isRtl) {
                timeRow.addView(repeatIcon, LayoutHelper.createLinear(14, 14, Gravity.CENTER_VERTICAL, 0, 0, 6, 0));
                timeRow.addView(dateTimeView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));
                timeRow.addView(calendarIcon, LayoutHelper.createLinear(14, 14, Gravity.CENTER_VERTICAL, 4, 0, 0, 0));
            } else {
                timeRow.addView(calendarIcon, LayoutHelper.createLinear(14, 14, Gravity.CENTER_VERTICAL, 0, 0, 4, 0));
                timeRow.addView(dateTimeView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));
                timeRow.addView(repeatIcon, LayoutHelper.createLinear(14, 14, Gravity.CENTER_VERTICAL, 6, 0, 0, 0));
            }

            messageView = new TextView(context);
            messageView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            messageView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
            messageView.setMaxLines(2);
            messageView.setEllipsize(TextUtils.TruncateAt.END);
            messageView.setGravity(Gravity.START);
            textLayout.addView(messageView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 6, 0, 0));
        }

        public void setData(ScheduledMessage msg) {
            // Load recipient avatar (using first chat ID)
            long firstChatId = 0;
            if (msg.chatIds != null && !msg.chatIds.isEmpty()) {
                firstChatId = msg.chatIds.get(0);
            }

            String name = "";
            if (firstChatId != 0) {
                if (firstChatId > 0) {
                    org.telegram.tgnet.TLRPC.User user = org.telegram.messenger.MessagesController.getInstance(currentAccount).getUser(firstChatId);
                    if (user != null) {
                        avatarView.setForUserOrChat(user, avatarDrawable);
                        name = UserObject.getUserName(user);
                    } else {
                        avatarDrawable.setInfo(firstChatId, "User", null);
                        avatarView.setImageDrawable(avatarDrawable);
                        name = "User ID: " + firstChatId;
                    }
                } else {
                    org.telegram.tgnet.TLRPC.Chat chat = org.telegram.messenger.MessagesController.getInstance(currentAccount).getChat(-firstChatId);
                    if (chat != null) {
                        avatarView.setForUserOrChat(chat, avatarDrawable);
                        name = chat.title;
                    } else {
                        avatarDrawable.setInfo(firstChatId, "Chat", null);
                        avatarView.setImageDrawable(avatarDrawable);
                        name = "Chat ID: " + firstChatId;
                    }
                }
            } else {
                avatarDrawable.setInfo(0, "Empty", null);
                avatarView.setImageDrawable(avatarDrawable);
                name = "—";
            }

            // Build recipients label (+N others)
            StringBuilder sb = new StringBuilder(name);
            if (msg.chatIds != null && msg.chatIds.size() > 1) {
                sb.append(" + ").append(msg.chatIds.size() - 1);
            }
            recipientsView.setText(sb.toString());

            // Format timestamp
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault());
            dateTimeView.setText(sdf.format(new Date(msg.timestamp)));

            // Message text
            messageView.setText(msg.message);

            // Repeat indicator visibility
            if (msg.repeatType != 0) {
                repeatIcon.setVisibility(View.VISIBLE);
            } else {
                repeatIcon.setVisibility(View.GONE);
            }
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(
                    MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            );
        }
    }
}
