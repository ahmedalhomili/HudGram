package com.hudgram.ui.scheduledmessages;

import com.hudgram.ui.settings.BaseHudSettingsActivity;
import com.hudgram.core.HudScheduledMessagesManager;
import com.hudgram.core.HudScheduledMessagesManager.ScheduledMessageLogEntry;

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
import org.telegram.messenger.UserObject;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
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

public class HudScheduledMessagesLogActivity extends BaseHudSettingsActivity {

    private ArrayList<ScheduledMessageLogEntry> logEntries;

    @Override
    public boolean onFragmentCreate() {
        logEntries = HudScheduledMessagesManager.getScheduledMessagesLog();
        return super.onFragmentCreate();
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        if (logEntries.isEmpty()) {
            items.add(UItem.asShadow(getString("HudScheduledMessagesHistoryLogEmpty")));
            return;
        }

        items.add(UItem.asHeader(getString("HudScheduledMessagesHistoryLog")));

        for (int i = 0; i < logEntries.size(); i++) {
            ScheduledMessageLogEntry entry = logEntries.get(i);
            boolean hasDivider = (i < logEntries.size() - 1);

            UItem uItem = LogEntryCellFactory.of(100 + i, entry);
            uItem.accent = hasDivider;
            items.add(uItem);
        }

        items.add(UItem.asShadow(null));

        // Clear button
        UItem clearItem = TextSettingsCellFactory.of(1, getString("HudScheduledMessagesHistoryClear"));
        clearItem.red = true;
        items.add(clearItem);
        items.add(UItem.asShadow(null));
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        if (item.id == 1) {
            HudScheduledMessagesManager.clearLog();
            logEntries.clear();
            listView.adapter.update(true);
            BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_delete, getString("HudScheduledMessagesHistoryCleared")).show();
        }
    }

    @Override
    protected String getActionBarTitle() {
        return getString("HudScheduledMessagesHistoryHeader");
    }

    @Override
    protected String getKey() {
        return "sml";
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
            cell.setData((ScheduledMessageLogEntry) item.object, item.accent || divider);
        }

        public static UItem of(int id, ScheduledMessageLogEntry entry) {
            UItem item = UItem.ofFactory(LogEntryCellFactory.class);
            item.id = id;
            item.object = entry;
            return item;
        }
    }

    protected static class LogEntryCell extends FrameLayout {
        private final int currentAccount;
        private final BackupImageView avatarView;
        private final AvatarDrawable avatarDrawable;
        private final TextView titleView;
        private final TextView contentView;
        private final TextView timeView;
        private final TextView statusView;
        private boolean needsDivider;

        public LogEntryCell(Context context, int currentAccount) {
            super(context);
            this.currentAccount = currentAccount;

            // Avatar Container
            avatarView = new BackupImageView(context);
            avatarView.setRoundRadius(AndroidUtilities.dp(21));
            addView(avatarView, LayoutHelper.createFrame(42, 42,
                    (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL,
                    16, 0, 16, 0));
            avatarDrawable = new AvatarDrawable();

            // Text Container
            LinearLayout textContainer = new LinearLayout(context);
            textContainer.setOrientation(LinearLayout.VERTICAL);
            int leftMargin = LocaleController.isRTL ? 16 : 74;
            int rightMargin = LocaleController.isRTL ? 74 : 16;
            addView(textContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT,
                    Gravity.CENTER_VERTICAL,
                    leftMargin, 8, rightMargin, 8));

            // Recipients Title Row
            LinearLayout titleRow = new LinearLayout(context);
            titleRow.setOrientation(LinearLayout.HORIZONTAL);
            titleRow.setGravity(Gravity.CENTER_VERTICAL);
            textContainer.addView(titleRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            titleView = new TextView(context);
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            titleView.setSingleLine(true);
            titleView.setEllipsize(TextUtils.TruncateAt.END);
            titleView.setGravity(Gravity.START);
            titleRow.addView(titleView, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));

            statusView = new TextView(context);
            statusView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
            statusView.setTypeface(AndroidUtilities.bold());
            titleRow.addView(statusView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 8, 0, 0, 0));

            // Message text
            contentView = new TextView(context);
            contentView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            contentView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
            contentView.setMaxLines(2);
            contentView.setEllipsize(TextUtils.TruncateAt.END);
            contentView.setGravity(Gravity.START);
            textContainer.addView(contentView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 3, 0, 0));

            // Timestamp
            timeView = new TextView(context);
            timeView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
            timeView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText3));
            timeView.setSingleLine(true);
            timeView.setGravity(Gravity.START);
            textContainer.addView(timeView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 0));

            setWillNotDraw(false);
        }

        public void setData(ScheduledMessageLogEntry entry, boolean divider) {
            needsDivider = divider;

            // Load first recipient info
            long firstChatId = 0;
            if (entry.chatIds != null && !entry.chatIds.isEmpty()) {
                firstChatId = entry.chatIds.get(0);
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
                name = "Unknown";
            }

            // Build recipients title (e.g. User + 2 others)
            SpannableStringBuilder builder = new SpannableStringBuilder();
            builder.append(name);
            builder.setSpan(new TypefaceSpan(AndroidUtilities.bold()), 0, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (entry.chatIds != null && entry.chatIds.size() > 1) {
                builder.append(" + ").append(String.valueOf(entry.chatIds.size() - 1));
            }
            titleView.setText(builder);

            contentView.setText(entry.message);

            // Format timestamp
            if (entry.timestamp > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault());
                timeView.setText(sdf.format(new Date(entry.timestamp)));
            } else {
                timeView.setText("");
            }

            // Status indicator
            if (entry.success) {
                statusView.setText("SUCCESS");
                statusView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGreenText));
            } else {
                statusView.setText("FAILED");
                statusView.setTextColor(Theme.getColor(Theme.key_text_RedBold));
            }

            invalidate();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(
                    MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            );
            int minHeight = AndroidUtilities.dp(72);
            int measuredH = getMeasuredHeight();
            if (measuredH < minHeight) measuredH = minHeight;
            setMeasuredDimension(getMeasuredWidth(), measuredH + (needsDivider ? 1 : 0));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (needsDivider && Theme.dividerPaint != null) {
                int startX = LocaleController.isRTL ? 0 : AndroidUtilities.dp(74);
                int endX = getMeasuredWidth() - (LocaleController.isRTL ? AndroidUtilities.dp(74) : 0);
                canvas.drawLine(startX, getMeasuredHeight() - 1, endX, getMeasuredHeight() - 1, Theme.dividerPaint);
            }
        }
    }
}
