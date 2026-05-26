package org.telegram.ui.Cells;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.StaticLayoutEx;

import java.util.ArrayList;

public class UpdatesChannelCell extends FrameLayout {

    private final ImageReceiver avatarImage;
    private final AvatarDrawable avatarDrawable;
    private final TextPaint namePaint;
    private final TextPaint messagePaint;
    private final TextPaint timePaint;
    private final TextPaint counterPaint;
    private final Paint counterBgPaint;
    private final Paint dividerPaint;
    private final RectF counterRect = new RectF();
    private final RectF capsuleRect = new RectF();
    private final Paint capsuleBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint checkBgPaint;
    private final Paint checkPaint;
    private final Paint avatarBgPaint;
    private boolean isChecked;
    private boolean isPinned;
    private boolean isMuted;

    private long dialogId;
    private int currentAccount;
    private String channelName = "";
    private String lastMessage = "";
    private String timeText = "";
    private int unreadCount;

    private StaticLayout nameLayout;
    private StaticLayout messageLayout;
    private boolean drawCheck;
    private boolean drawDoubleCheck;

    public UpdatesChannelCell(Context context) {
        super(context);
        setWillNotDraw(false);

        avatarImage = new ImageReceiver(this);
        avatarImage.setRoundRadius(dp(24));

        avatarDrawable = new AvatarDrawable();

        namePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        namePaint.setTextSize(dp(16));
        namePaint.setTypeface(AndroidUtilities.bold());

        messagePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        messagePaint.setTextSize(dp(14));

        timePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        timePaint.setTextSize(dp(12));

        counterPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        counterPaint.setTextSize(dp(12));
        counterPaint.setTypeface(AndroidUtilities.bold());

        counterBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        dividerPaint = new Paint();
        dividerPaint.setStrokeWidth(1);

        checkBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        checkBgPaint.setColor(0xFF25D366); // WhatsApp green

        checkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        checkPaint.setColor(android.graphics.Color.WHITE);
        checkPaint.setStrokeWidth(dp(2));
        checkPaint.setStrokeCap(Paint.Cap.ROUND);
        checkPaint.setStyle(Paint.Style.STROKE);

        avatarBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        updateColors();
    }

    private void updateColors() {
        namePaint.setColor(Theme.getColor(Theme.key_chats_name));
        messagePaint.setColor(Theme.getColor(Theme.key_chats_message));
        timePaint.setColor(Theme.getColor(Theme.key_chats_date));
        counterPaint.setColor(Theme.getColor(Theme.key_chats_unreadCounterText));
        dividerPaint.setColor(Theme.getColor(Theme.key_divider));
        avatarBgPaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        boolean isDark = Theme.isCurrentThemeDark();
        capsuleBgPaint.setColor(isDark ? 0x25FFFFFF : 0x1E000000);

        isMuted = MessagesController.getInstance(currentAccount).isDialogMuted(dialogId, 0);
        TLRPC.Dialog dialog = MessagesController.getInstance(currentAccount).dialogs_dict.get(dialogId);
        isPinned = dialog != null && dialog.pinned;

        if (isMuted) {
            counterBgPaint.setColor(Theme.getColor(Theme.key_chats_unreadCounterMuted));
        } else {
            counterBgPaint.setColor(Theme.getColor(Theme.key_chats_unreadCounter));
        }
    }

    public void setChecked(boolean checked, boolean animated) {
        if (this.isChecked != checked) {
            this.isChecked = checked;
            invalidate();
        }
    }

    public boolean isChecked() {
        return isChecked;
    }

    public boolean isPointInsideAvatar(float x, float y) {
        int avatarLeft = dp(16);
        int avatarTop = dp(12);
        int avatarSize = dp(48);
        return x >= avatarLeft && x <= avatarLeft + avatarSize && y >= avatarTop && y <= avatarTop + avatarSize;
    }

    public void setChannel(int currentAccount, TLRPC.Dialog dialog) {
        this.currentAccount = currentAccount;
        this.dialogId = dialog.id;
        this.unreadCount = dialog.unread_count;

        TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-dialog.id);
        if (chat == null) {
            return;
        }

        channelName = chat.title != null ? chat.title : "";
        avatarDrawable.setInfo(currentAccount, chat);
        avatarImage.setForUserOrChat(chat, avatarDrawable);

        // Get time
        if (dialog.last_message_date != 0) {
            timeText = LocaleController.stringForMessageListDate(dialog.last_message_date);
        } else {
            timeText = "";
        }

        // Get last message
        MessageObject msg = null;
        ArrayList<MessageObject> messages = MessagesController.getInstance(currentAccount).dialogMessage.get(dialog.id);
        if (messages != null && !messages.isEmpty()) {
            msg = messages.get(0);
            if (msg != null && msg.messageText != null) {
                lastMessage = msg.messageText.toString().replace("\n", " ");
            } else {
                lastMessage = "";
            }
        } else {
            lastMessage = "";
        }

        // Check if sent by us to draw checkmarks
        drawCheck = false;
        drawDoubleCheck = false;
        if (msg != null && msg.isOut()) {
            drawCheck = true;
            if (!msg.isUnread()) {
                drawDoubleCheck = true;
            }
        }

        updateColors();
        invalidate();
    }

    public long getDialogId() {
        return dialogId;
    }

    public boolean getHasUnread() {
        TLRPC.Dialog dialog = MessagesController.getInstance(currentAccount).dialogs_dict.get(dialogId);
        return unreadCount > 0 || (dialog != null && dialog.unread_mark);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int textLeft = dp(76);
        int rightPadding = dp(16);

        float timeWidth = 0;
        if (!TextUtils.isEmpty(timeText)) {
            timeWidth = timePaint.measureText(timeText);
        }

        float nameMaxWidth = width - textLeft - rightPadding - timeWidth - dp(8);
        if (drawCheck) {
            nameMaxWidth -= dp(24); // room for checkmarks
        }

        int pinWidth = isPinned && Theme.dialogs_pinnedDrawable2 != null ? Theme.dialogs_pinnedDrawable2.getIntrinsicWidth() : 0;
        int muteWidth = isMuted && Theme.dialogs_muteDrawable != null ? Theme.dialogs_muteDrawable.getIntrinsicWidth() : 0;

        if (pinWidth > 0) {
            nameMaxWidth -= pinWidth + dp(16);
        }
        if (muteWidth > 0) {
            nameMaxWidth -= muteWidth + dp(6);
        }

        if (nameMaxWidth > 0) {
            CharSequence formattedName = Emoji.replaceEmoji(channelName, namePaint.getFontMetricsInt(), false);
            nameLayout = StaticLayoutEx.createStaticLayout(formattedName, namePaint, (int) nameMaxWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false, TextUtils.TruncateAt.END, (int) nameMaxWidth, 1);
        } else {
            nameLayout = null;
        }

        float msgMaxWidth = width - textLeft - rightPadding;
        TLRPC.Dialog dialog = MessagesController.getInstance(currentAccount).dialogs_dict.get(dialogId);
        boolean hasUnreadMark = dialog != null && dialog.unread_mark;
        if (unreadCount > 0 || hasUnreadMark) {
            msgMaxWidth -= dp(32);
        }
        if (msgMaxWidth > 0 && !TextUtils.isEmpty(lastMessage)) {
            CharSequence formattedMsg = Emoji.replaceEmoji(lastMessage, messagePaint.getFontMetricsInt(), false);
            messageLayout = StaticLayoutEx.createStaticLayout(formattedMsg, messagePaint, (int) msgMaxWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false, TextUtils.TruncateAt.END, (int) msgMaxWidth, 1);
        } else {
            messageLayout = null;
        }

        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(dp(72), MeasureSpec.EXACTLY));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        int avatarLeft = dp(16);
        int avatarTop = dp(12);
        int avatarSize = dp(48);
        avatarImage.setImageCoords(avatarLeft, avatarTop, avatarSize, avatarSize);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getMeasuredWidth();
        int h = getMeasuredHeight();

        // Draw avatar
        avatarImage.draw(canvas);

        // Draw green checkmark badge if checked
        if (isChecked) {
            int avatarLeft = dp(16);
            int avatarTop = dp(12);
            int avatarSize = dp(48);

            float cx = avatarLeft + avatarSize - dp(4);
            float cy = avatarTop + avatarSize - dp(4);

            // White outline/shadow (using window background)
            canvas.drawCircle(cx, cy, dp(10) + dp(1.5f), avatarBgPaint);
            // Green circle
            canvas.drawCircle(cx, cy, dp(10), checkBgPaint);
            // White checkmark lines
            canvas.drawLine(cx - dp(4), cy, cx - dp(1), cy + dp(3), checkPaint);
            canvas.drawLine(cx - dp(1), cy + dp(3), cx + dp(5), cy - dp(3), checkPaint);
        }

        int textLeft = dp(76);
        int rightPadding = dp(16);

        // Draw time inside capsule if pinned, otherwise draw normally
        float timeWidth = 0;
        if (!TextUtils.isEmpty(timeText)) {
            timeWidth = timePaint.measureText(timeText);
        }

        float capsuleWidth = 0;
        if (isPinned && Theme.dialogs_pinnedDrawable2 != null) {
            int pinWidth = Theme.dialogs_pinnedDrawable2.getIntrinsicWidth();
            int pinHeight = Theme.dialogs_pinnedDrawable2.getIntrinsicHeight();
            capsuleWidth = pinWidth + (timeWidth > 0 ? dp(4) : 0) + timeWidth + dp(16);
            int capsuleHeight = dp(20);
            float capsuleX = w - rightPadding - capsuleWidth;
            float capsuleY = dp(28) - dp(14.5f);

            capsuleRect.set(capsuleX, capsuleY, capsuleX + capsuleWidth, capsuleY + capsuleHeight);
            canvas.drawRoundRect(capsuleRect, dp(10), dp(10), capsuleBgPaint);

            int pinX = (int) (capsuleX + dp(8));
            int pinY = (int) (capsuleY + (capsuleHeight - pinHeight) / 2);

            int originalColor = Theme.getColor(Theme.key_chats_pinnedIcon);
            Theme.dialogs_pinnedDrawable2.setColorFilter(new android.graphics.PorterDuffColorFilter(0xFFE5A93B, android.graphics.PorterDuff.Mode.SRC_IN));
            Theme.dialogs_pinnedDrawable2.setBounds(pinX, pinY, pinX + pinWidth, pinY + pinHeight);
            Theme.dialogs_pinnedDrawable2.draw(canvas);
            Theme.dialogs_pinnedDrawable2.setColorFilter(new android.graphics.PorterDuffColorFilter(originalColor, android.graphics.PorterDuff.Mode.SRC_IN));

            if (!TextUtils.isEmpty(timeText)) {
                int originalTimeColor = timePaint.getColor();
                timePaint.setColor(0xFFE5A93B);
                float timeX = pinX + pinWidth + dp(4);
                canvas.drawText(timeText, timeX, dp(28), timePaint);
                timePaint.setColor(originalTimeColor);
            }
        } else {
            if (!TextUtils.isEmpty(timeText)) {
                canvas.drawText(timeText, w - rightPadding - timeWidth, dp(28), timePaint);
            }
        }

        // Draw checkmarks if sent by us
        if (drawCheck) {
            int checkTop = dp(16);
            int checkLeft;
            if (isPinned && Theme.dialogs_pinnedDrawable2 != null) {
                checkLeft = w - rightPadding - (int) capsuleWidth - dp(4);
            } else {
                checkLeft = w - rightPadding - (int) timeWidth - dp(4);
            }

            if (drawDoubleCheck) {
                int width1 = Theme.dialogs_halfCheckDrawable.getIntrinsicWidth();
                int height1 = Theme.dialogs_halfCheckDrawable.getIntrinsicHeight();
                Theme.dialogs_halfCheckDrawable.setBounds(checkLeft - width1 - dp(4), checkTop, checkLeft - dp(4), checkTop + height1);
                Theme.dialogs_halfCheckDrawable.draw(canvas);

                int width2 = Theme.dialogs_checkDrawable.getIntrinsicWidth();
                int height2 = Theme.dialogs_checkDrawable.getIntrinsicHeight();
                Theme.dialogs_checkDrawable.setBounds(checkLeft - width2, checkTop, checkLeft, checkTop + height2);
                Theme.dialogs_checkDrawable.draw(canvas);
            } else {
                int width = Theme.dialogs_checkDrawable.getIntrinsicWidth();
                int height = Theme.dialogs_checkDrawable.getIntrinsicHeight();
                Theme.dialogs_checkDrawable.setBounds(checkLeft - width, checkTop, checkLeft, checkTop + height);
                Theme.dialogs_checkDrawable.draw(canvas);
            }
        }

        // Draw channel name
        if (nameLayout != null) {
            canvas.save();
            canvas.translate(textLeft, dp(12));
            nameLayout.draw(canvas);
            canvas.restore();
        }

        // Draw muted icon if muted
        if (isMuted && Theme.dialogs_muteDrawable != null) {
            int muteWidth = Theme.dialogs_muteDrawable.getIntrinsicWidth();
            int muteHeight = Theme.dialogs_muteDrawable.getIntrinsicHeight();
            float nameWidth = nameLayout != null ? nameLayout.getLineWidth(0) : 0;
            int muteX = textLeft + (int) nameWidth + dp(4);
            int muteY = dp(12) + (dp(16) - muteHeight) / 2 + dp(1);
            Theme.dialogs_muteDrawable.setBounds(muteX, muteY, muteX + muteWidth, muteY + muteHeight);
            Theme.dialogs_muteDrawable.draw(canvas);
        }

        // Draw last message
        if (messageLayout != null) {
            canvas.save();
            canvas.translate(textLeft, dp(38));
            messageLayout.draw(canvas);
            canvas.restore();
        }

        // Draw unread counter or manual unread mark
        TLRPC.Dialog dialog = MessagesController.getInstance(currentAccount).dialogs_dict.get(dialogId);
        boolean hasUnreadMark = dialog != null && dialog.unread_mark;

        if (unreadCount > 0) {
            String countText = unreadCount > 99 ? "99+" : String.valueOf(unreadCount);
            float countWidth = counterPaint.measureText(countText);
            float badgeWidth = Math.max(dp(20), countWidth + dp(10));
            float badgeHeight = dp(20);
            float badgeX = w - rightPadding - badgeWidth;
            float badgeY = dp(38);

            counterRect.set(badgeX, badgeY, badgeX + badgeWidth, badgeY + badgeHeight);
            canvas.drawRoundRect(counterRect, badgeHeight / 2, badgeHeight / 2, counterBgPaint);
            canvas.drawText(countText, badgeX + (badgeWidth - countWidth) / 2, badgeY + dp(14.5f), counterPaint);
        } else if (hasUnreadMark) {
            float badgeSize = dp(20);
            float badgeX = w - rightPadding - badgeSize;
            float badgeY = dp(38);
            counterRect.set(badgeX, badgeY, badgeX + badgeSize, badgeY + badgeSize);
            canvas.drawRoundRect(counterRect, badgeSize / 2, badgeSize / 2, counterBgPaint);
        }

        // Draw divider
        // canvas.drawLine(textLeft, h - 1, w, h - 1, dividerPaint);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        avatarImage.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        avatarImage.onDetachedFromWindow();
    }
}
