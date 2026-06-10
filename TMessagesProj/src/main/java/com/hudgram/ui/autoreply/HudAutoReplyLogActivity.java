package com.hudgram.ui.autoreply;
import com.hudgram.ui.settings.BaseHudSettingsActivity;
import com.hudgram.core.HudConfig;

import android.content.Context;
import android.graphics.Canvas;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.TypefaceSpan;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class HudAutoReplyLogActivity extends BaseHudSettingsActivity {

    private ArrayList<HudConfig.AutoReplyLogEntry> logEntries;

    @Override
    public boolean onFragmentCreate() {
        logEntries = HudConfig.getAutoReplyLog();
        return super.onFragmentCreate();
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        if (logEntries.isEmpty()) {
            items.add(UItem.asShadow(getString("HudAutoReplyLogEmpty")));
            return;
        }

        items.add(UItem.asHeader(getString("HudAutoReplyLogHeader")));

        for (int i = 0; i < logEntries.size(); i++) {
            HudConfig.AutoReplyLogEntry entry = logEntries.get(i);
            boolean hasDivider = (i < logEntries.size() - 1);

            UItem uItem = LogEntryCellFactory.of(100 + i, entry);
            uItem.accent = hasDivider;
            items.add(uItem);
        }

        items.add(UItem.asShadow(null));

        // Clear button
        UItem clearItem = TextSettingsCellFactory.of(1, getString("HudAutoReplyLogClear"));
        clearItem.red = true;
        items.add(clearItem);
        items.add(UItem.asShadow(null));
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        if (item.id == 1) {
            HudConfig.clearAutoReplyLog();
            logEntries.clear();
            listView.adapter.update(true);
            BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_delete, getString("HudAutoReplyLogCleared")).show();
        } else if (item.id >= 100) {
            int index = item.id - 100;
            if (index >= 0 && index < logEntries.size()) {
                HudConfig.AutoReplyLogEntry entry = logEntries.get(index);
                if (entry.groupId != 0) {
                    android.os.Bundle args = new android.os.Bundle();
                    args.putLong("chat_id", entry.groupId);
                    if (entry.messageId != 0) {
                        args.putInt("message_id", entry.messageId);
                    }
                    org.telegram.ui.ChatActivity chatActivity = new org.telegram.ui.ChatActivity(args);
                    if (entry.messageId != 0) {
                        chatActivity.setHighlightMessageId(entry.messageId);
                    }
                    presentFragment(chatActivity);
                }
            }
        }
    }

    @Override
    protected String getActionBarTitle() {
        return getString("HudAutoReplyLog");
    }

    @Override
    protected String getKey() {
        return "arl";
    }

    // === Log entry cell factory ===
    protected static class LogEntryCellFactory extends UItem.UItemFactory<LogEntryCell> {
        static {
            setup(new LogEntryCellFactory());
        }

        @Override
        public LogEntryCell createView(Context context, RecyclerListView listView, int currentAccount, int classGuid, Theme.ResourcesProvider resourcesProvider) {
            return new LogEntryCell(context, currentAccount);
        }

        @Override
        public void bindView(View view, UItem item, boolean divider, UniversalAdapter adapter, UniversalRecyclerView listView) {
            LogEntryCell cell = (LogEntryCell) view;
            cell.setData((HudConfig.AutoReplyLogEntry) item.object, item.accent || divider);
        }

        public static UItem of(int id, HudConfig.AutoReplyLogEntry entry) {
            UItem item = UItem.ofFactory(LogEntryCellFactory.class);
            item.id = id;
            item.object = entry;
            return item;
        }
    }

    protected static class LogEntryCell extends FrameLayout {
        private final int currentAccount;
        private final org.telegram.ui.Components.BackupImageView groupAvatarView;
        private final org.telegram.ui.Components.BackupImageView senderAvatarView;
        private final org.telegram.ui.Components.AvatarDrawable groupAvatarDrawable;
        private final org.telegram.ui.Components.AvatarDrawable senderAvatarDrawable;
        private final TextView titleView;
        private final TextView replyView;
        private final TextView timeView;
        private boolean needsDivider;

        public LogEntryCell(Context context, int currentAccount) {
            super(context);
            this.currentAccount = currentAccount;

            // Avatars Container
            FrameLayout avatarsContainer = new FrameLayout(context);
            addView(avatarsContainer, LayoutHelper.createFrame(48, 48,
                    (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP,
                    21, 12, 21, 0));

            // Group Avatar
            groupAvatarView = new org.telegram.ui.Components.BackupImageView(context);
            groupAvatarView.setRoundRadius(AndroidUtilities.dp(18));
            avatarsContainer.addView(groupAvatarView, LayoutHelper.createFrame(36, 36, Gravity.START | Gravity.TOP));
            groupAvatarDrawable = new org.telegram.ui.Components.AvatarDrawable();

            // Sender Avatar Border Frame
            FrameLayout senderAvatarFrame = new FrameLayout(context);
            android.graphics.drawable.GradientDrawable borderDrawable = new android.graphics.drawable.GradientDrawable();
            borderDrawable.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            borderDrawable.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            senderAvatarFrame.setBackground(borderDrawable);
            avatarsContainer.addView(senderAvatarFrame, LayoutHelper.createFrame(28, 28, Gravity.END | Gravity.BOTTOM));

            // Sender Avatar
            senderAvatarView = new org.telegram.ui.Components.BackupImageView(context);
            senderAvatarView.setRoundRadius(AndroidUtilities.dp(12));
            senderAvatarFrame.addView(senderAvatarView, LayoutHelper.createFrame(24, 24, Gravity.CENTER));
            senderAvatarDrawable = new org.telegram.ui.Components.AvatarDrawable();

            // Text Container
            LinearLayout textContainer = new LinearLayout(context);
            textContainer.setOrientation(LinearLayout.VERTICAL);
            addView(textContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT,
                    Gravity.CENTER_VERTICAL,
                    LocaleController.isRTL ? 21 : 83, 8, LocaleController.isRTL ? 83 : 21, 8));

            // Group & Sender title
            titleView = new TextView(context);
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            titleView.setSingleLine(true);
            titleView.setEllipsize(TextUtils.TruncateAt.END);
            titleView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
            textContainer.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            // Reply text
            replyView = new TextView(context);
            replyView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            replyView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
            replyView.setMaxLines(2);
            replyView.setEllipsize(TextUtils.TruncateAt.END);
            replyView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
            replyView.setLineSpacing(AndroidUtilities.dp(1.5f), 1.0f);
            textContainer.addView(replyView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 3, 0, 0));

            // Timestamp
            timeView = new TextView(context);
            timeView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
            timeView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText3));
            timeView.setSingleLine(true);
            timeView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
            textContainer.addView(timeView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 0));

            setWillNotDraw(false);
        }

        public void setData(HudConfig.AutoReplyLogEntry entry, boolean divider) {
            needsDivider = divider;

            // Load user and chat
            org.telegram.tgnet.TLRPC.Chat chat = org.telegram.messenger.MessagesController.getInstance(currentAccount).getChat(entry.groupId);
            org.telegram.tgnet.TLRPC.User user = org.telegram.messenger.MessagesController.getInstance(currentAccount).getUser(entry.senderId);

            if (chat != null) {
                groupAvatarView.setForUserOrChat(chat, groupAvatarDrawable);
            } else {
                groupAvatarDrawable.setInfo(entry.groupId, entry.groupName, null);
                groupAvatarView.setImageDrawable(groupAvatarDrawable);
            }

            if (user != null) {
                senderAvatarView.setForUserOrChat(user, senderAvatarDrawable);
            } else {
                senderAvatarDrawable.setInfo(entry.senderId, entry.senderName, null);
                senderAvatarView.setImageDrawable(senderAvatarDrawable);
            }

            // Group Name & Sender Name title builder
            SpannableStringBuilder builder = new SpannableStringBuilder();
            builder.append(entry.groupName != null ? entry.groupName : "");
            builder.setSpan(new TypefaceSpan(AndroidUtilities.bold()), 0, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.append("  •  ");
            builder.append(entry.senderName != null ? entry.senderName : "");
            titleView.setText(builder);

            replyView.setText(entry.replyText);

            // Format timestamp
            if (entry.timestamp > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault());
                timeView.setText(sdf.format(new Date(entry.timestamp)));
            } else {
                timeView.setText("");
            }

            invalidate();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(
                    MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            );
            int minHeight = AndroidUtilities.dp(76);
            int measuredH = getMeasuredHeight();
            if (measuredH < minHeight) measuredH = minHeight;
            setMeasuredDimension(getMeasuredWidth(), measuredH + (needsDivider ? 1 : 0));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (needsDivider && Theme.dividerPaint != null) {
                int startX = LocaleController.isRTL ? 0 : AndroidUtilities.dp(83);
                int endX = getMeasuredWidth() - (LocaleController.isRTL ? AndroidUtilities.dp(83) : 0);
                canvas.drawLine(startX, getMeasuredHeight() - 1, endX, getMeasuredHeight() - 1, Theme.dividerPaint);
            }
        }
    }
}
