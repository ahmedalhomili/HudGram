package com.hudgram.ui.cells;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.graphics.drawable.Drawable;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.FileLoader;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.StaticLayoutEx;
import org.telegram.ui.Components.TextStyleSpan;
import org.telegram.ui.Components.spoilers.SpoilerEffect;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class UpdatesChannelCell extends FrameLayout {

    private final ImageReceiver avatarImage;
    private final AvatarDrawable avatarDrawable;
    private final ImageReceiver mediaImage;
    private final TextPaint namePaint;
    private final TextPaint messagePaint;
    private final TextPaint timePaint;
    private final TextPaint counterPaint;
    private final Paint counterBgPaint;
    private final Paint dividerPaint;
    private final RectF counterRect = new RectF();
    private final RectF capsuleRect = new RectF();

    private final Paint checkBgPaint;
    private final Paint checkPaint;
    private final Paint avatarBgPaint;
    private boolean isChecked;
    private boolean isPinned;
    private boolean isMuted;
    private boolean isActionMessage;
    private boolean isAttachMessage;
    private boolean hasMediaThumb;

    private final Stack<SpoilerEffect> spoilersPool = new Stack<>();
    private final List<SpoilerEffect> spoilers = new ArrayList<>();

    private long dialogId;
    private int currentAccount;
    private String channelName = "";
    private CharSequence lastMessage = "";
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
        
        mediaImage = new ImageReceiver(this);
        mediaImage.setRoundRadius(dp(2));
        mediaImage.ignoreNotifications = true;
        mediaImage.setAllowLoadingOnAttachedOnly(true);

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

        checkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        checkPaint.setStrokeWidth(dp(2));
        checkPaint.setStrokeCap(Paint.Cap.ROUND);
        checkPaint.setStyle(Paint.Style.STROKE);

        avatarBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        updateColors();
    }

    private void updateColors() {
        namePaint.setColor(Theme.getColor(Theme.key_chats_name));
        
        int messageColor;
        if (isActionMessage) {
            messageColor = Theme.getColor(Theme.key_chats_actionMessage);
        } else if (isAttachMessage) {
            messageColor = Theme.getColor(Theme.key_chats_attachMessage);
        } else {
            messageColor = Theme.getColor(Theme.key_chats_message);
        }
        messagePaint.setColor(messageColor);
        
        timePaint.setColor(Theme.getColor(Theme.key_chats_date));
        counterPaint.setColor(Theme.getColor(Theme.key_chats_unreadCounterText));
        dividerPaint.setColor(Theme.getColor(Theme.key_divider));
        avatarBgPaint.setColor(Theme.getColor(isChecked ? Theme.key_chats_tabletSelectedOverlay : Theme.key_windowBackgroundWhite));
        checkBgPaint.setColor(Theme.getColor(Theme.key_checkbox));
        checkPaint.setColor(Theme.getColor(Theme.key_checkboxCheck));

        isMuted = MessagesController.getInstance(currentAccount).isDialogMuted(dialogId, 0);
        TLRPC.Dialog dialog = MessagesController.getInstance(currentAccount).dialogs_dict.get(dialogId);
        isPinned = dialog != null && dialog.pinned;

        if (isMuted) {
            counterBgPaint.setColor(Theme.getColor(Theme.key_chats_unreadCounterMuted));
        } else {
            counterBgPaint.setColor(Theme.getColor(Theme.key_chats_unreadCounter));
        }

        setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 2));
    }

    public void setChecked(boolean checked, boolean animated) {
        if (this.isChecked != checked) {
            this.isChecked = checked;
            updateColors();
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
        isActionMessage = false;
        isAttachMessage = false;
        hasMediaThumb = false;
        
        ArrayList<MessageObject> messages = MessagesController.getInstance(currentAccount).dialogMessage.get(dialog.id);
        if (messages != null && !messages.isEmpty()) {
            msg = messages.get(0);
            if (msg != null) {
                if (msg.messageOwner != null && msg.messageOwner.action != null) {
                    isActionMessage = true;
                } else if (msg.messageOwner != null && msg.messageOwner.media != null && !(msg.messageOwner.media instanceof TLRPC.TL_messageMediaEmpty)) {
                    isAttachMessage = true;
                }
                
                if (msg.messageText != null) {
                    // Build a SpannableStringBuilder and add TextStyleSpan runs
                    // (including spoiler/strike) from the message entities,
                    // exactly as DialogCell does.
                    CharSequence rawText = msg.messageText;
                    SpannableStringBuilder sb = new SpannableStringBuilder(rawText);
                    // Replace newlines while preserving spans
                    for (int i = 0; i < sb.length(); i++) {
                        if (sb.charAt(i) == '\n') {
                            sb.replace(i, i + 1, " ");
                        }
                    }
                    // Add spoiler and strikethrough style spans from entities
                    MediaDataController.addTextStyleRuns(msg, sb, TextStyleSpan.FLAG_STYLE_SPOILER | TextStyleSpan.FLAG_STYLE_STRIKE);
                    lastMessage = sb;
                } else {
                    lastMessage = "";
                }
                
                // Try loading media thumbnail if it has media
                if (msg.messageOwner != null && msg.messageOwner.media != null) {
                    TLRPC.MessageMedia media = msg.messageOwner.media;
                    TLObject object = null;
                    ArrayList<TLRPC.PhotoSize> photoThumbs = null;
                    if (media instanceof TLRPC.TL_messageMediaPhoto && media.photo != null) {
                         object = media.photo;
                         photoThumbs = media.photo.sizes;
                    } else if (media instanceof TLRPC.TL_messageMediaDocument && media.document != null && MessageObject.isVideoDocument(media.document)) {
                         object = media.document;
                         photoThumbs = media.document.thumbs;
                    }
                    
                    if (photoThumbs != null && !photoThumbs.isEmpty()) {
                         TLRPC.PhotoSize smallThumb = FileLoader.getStrippedPhotoSize(photoThumbs);
                         if (smallThumb == null) {
                             smallThumb = FileLoader.getClosestPhotoSizeWithSize(photoThumbs, 40);
                         }
                         if (smallThumb != null) {
                             hasMediaThumb = true;
                             mediaImage.setImage(ImageLocation.getForObject(smallThumb, object), "20_20", null, null, 0, null, msg, 0);
                         }
                    }
                }
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

    private boolean getDrawCount() {
        TLRPC.Dialog dialog = MessagesController.getInstance(currentAccount).dialogs_dict.get(dialogId);
        boolean hasUnreadMark = dialog != null && dialog.unread_mark;
        return unreadCount > 0 || hasUnreadMark;
    }

    private TextPaint getTimeTextPaint() {
        if (Theme.dialogs_timePaint == null) {
            return timePaint;
        }
        return getDrawCount() ? (isMuted ? Theme.dialogs_timePaintBold : Theme.dialogs_timePaintBoldAccent) : Theme.dialogs_timePaint;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int textLeft = dp(76);
        int rightPadding = dp(16);

        TextPaint timePaintToUse = getTimeTextPaint();
        float timeWidth = 0;
        if (!TextUtils.isEmpty(timeText)) {
            timeWidth = timePaintToUse.measureText(timeText);
        }

        float nameMaxWidth = width - textLeft - rightPadding - timeWidth - dp(8);
        if (drawCheck) {
            nameMaxWidth -= dp(24); // room for checkmarks
        }

        Drawable pd = getDrawCount() && !isMuted ? Theme.dialogs_pinnedDrawable2Accent : Theme.dialogs_pinnedDrawable2;
        int pinWidth = isPinned && pd != null ? pd.getIntrinsicWidth() : 0;
        int muteWidth = isMuted && Theme.dialogs_muteDrawable != null ? Theme.dialogs_muteDrawable.getIntrinsicWidth() : 0;

        if (pinWidth > 0) {
            nameMaxWidth -= pinWidth + dp(16);
        }
        if (muteWidth > 0) {
            nameMaxWidth -= muteWidth + dp(6);
        }

        if (nameMaxWidth > 0) {
            CharSequence formattedName = Emoji.replaceEmoji(channelName, namePaint.getFontMetricsInt(), false);
            nameLayout = StaticLayoutEx.createStaticLayout(formattedName, namePaint, (int) nameMaxWidth, StaticLayoutEx.ALIGN_LEFT(), 1.0f, 0.0f, false, TextUtils.TruncateAt.END, (int) nameMaxWidth, 1);
        } else {
            nameLayout = null;
        }

        float msgMaxWidth = width - textLeft - rightPadding;
        if (hasMediaThumb) {
            msgMaxWidth -= dp(24);
        }
        TLRPC.Dialog dialog = MessagesController.getInstance(currentAccount).dialogs_dict.get(dialogId);
        boolean hasUnreadMark = dialog != null && dialog.unread_mark;
        if (unreadCount > 0 || hasUnreadMark) {
            msgMaxWidth -= dp(32);
        }
        // Recycle spoiler effects into pool before rebuilding
        spoilersPool.addAll(spoilers);
        spoilers.clear();

        if (msgMaxWidth > 0 && !TextUtils.isEmpty(lastMessage)) {
            CharSequence formattedMsg = Emoji.replaceEmoji(lastMessage, messagePaint.getFontMetricsInt(), false);
            messageLayout = StaticLayoutEx.createStaticLayout(formattedMsg, messagePaint, (int) msgMaxWidth, StaticLayoutEx.ALIGN_LEFT(), 1.0f, 0.0f, false, TextUtils.TruncateAt.END, (int) msgMaxWidth, 1);
            // Parse spoiler spans from the layout text and populate spoilers list
            if (messageLayout != null && messageLayout.getText() instanceof Spanned) {
                SpoilerEffect.addSpoilers(this, messageLayout, spoilersPool, spoilers);
            }
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
        if (isChecked) {
            canvas.drawColor(Theme.getColor(Theme.key_chats_tabletSelectedOverlay));
        }
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
        TextPaint timePaintToUse = getTimeTextPaint();
        float timeWidth = 0;
        if (!TextUtils.isEmpty(timeText)) {
            timeWidth = timePaintToUse.measureText(timeText);
        }

        float capsuleWidth = 0;
        if (isPinned && Theme.dialogs_pinnedDrawable2 != null) {
            Drawable pd = getDrawCount() && !isMuted ? Theme.dialogs_pinnedDrawable2Accent : Theme.dialogs_pinnedDrawable2;
            int pinWidth = pd.getIntrinsicWidth();
            int pinHeight = pd.getIntrinsicHeight();
            capsuleWidth = pinWidth + (timeWidth > 0 ? dp(4) : 0) + timeWidth + dp(16);
            int capsuleHeight = dp(17);
            float capsuleX = w - rightPadding - capsuleWidth;
            float capsuleY = dp(21) + (dp(14) - capsuleHeight) / 2f;

            capsuleRect.set(capsuleX, capsuleY, capsuleX + capsuleWidth, capsuleY + capsuleHeight);
            int originalAlpha = timePaintToUse.getAlpha();
            timePaintToUse.setAlpha(27);
            canvas.drawRoundRect(capsuleRect, dp(17 / 2f), dp(17 / 2f), timePaintToUse);
            timePaintToUse.setAlpha(originalAlpha);

            int pinX = (int) (capsuleX + dp(8));
            int pinY = (int) (capsuleY + (capsuleHeight - pinHeight) / 2);

            pd.setBounds(pinX, pinY, pinX + pinWidth, pinY + pinHeight);
            pd.draw(canvas);

            if (!TextUtils.isEmpty(timeText)) {
                float timeX = pinX + pinWidth + dp(4);
                Paint.FontMetricsInt fontMetrics = timePaintToUse.getFontMetricsInt();
                float timeBaselineY = capsuleRect.centerY() - (fontMetrics.descent + fontMetrics.ascent) / 2f;
                canvas.drawText(timeText, timeX, timeBaselineY, timePaintToUse);
            }
        } else {
            if (!TextUtils.isEmpty(timeText)) {
                canvas.drawText(timeText, w - rightPadding - timeWidth, dp(28), timePaintToUse);
            }
        }

        // Draw checkmarks if sent by us (aligned vertically and matching native DialogCell dimensions)
        if (drawCheck) {
            int checkHeight = drawDoubleCheck 
                ? Theme.dialogs_checkReadDrawable.getIntrinsicHeight() 
                : Theme.dialogs_checkDrawable.getIntrinsicHeight();
            
            int checkTop;
            if (isPinned && Theme.dialogs_pinnedDrawable2 != null) {
                checkTop = (int) (capsuleRect.centerY() - checkHeight / 2f);
            } else {
                checkTop = (int) (dp(24) - checkHeight / 2f);
            }
            
            int checkLeft;
            if (isPinned && Theme.dialogs_pinnedDrawable2 != null) {
                checkLeft = w - rightPadding - (int) capsuleWidth - dp(4);
            } else {
                checkLeft = w - rightPadding - (int) timeWidth - dp(4);
            }

            if (drawDoubleCheck) {
                int width1 = Theme.dialogs_halfCheckDrawable.getIntrinsicWidth();
                int height1 = Theme.dialogs_halfCheckDrawable.getIntrinsicHeight();
                int width2 = Theme.dialogs_checkReadDrawable.getIntrinsicWidth();
                int height2 = Theme.dialogs_checkReadDrawable.getIntrinsicHeight();
                
                // Natively, checkReadDrawable (full check) is drawn on the left, and halfCheckDrawable (second tick) is on the right!
                int checkDrawLeft = checkLeft - width1 - (int) dp(5.5f);
                
                Theme.dialogs_checkReadDrawable.setBounds(checkDrawLeft, checkTop, checkDrawLeft + width2, checkTop + height2);
                Theme.dialogs_checkReadDrawable.draw(canvas);

                Theme.dialogs_halfCheckDrawable.setBounds(checkDrawLeft + (int) dp(5.5f), checkTop, checkDrawLeft + (int) dp(5.5f) + width1, checkTop + height1);
                Theme.dialogs_halfCheckDrawable.draw(canvas);
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

        // Draw media preview thumbnail if available
        if (hasMediaThumb) {
            mediaImage.setImageCoords(textLeft, dp(38) + dp(1), dp(18), dp(18));
            mediaImage.draw(canvas);
        }

        // Draw last message (shifted dynamically if there is a media thumbnail)
        // With spoiler clipping and dot particle rendering
        if (messageLayout != null) {
            canvas.save();
            canvas.translate(textLeft + (hasMediaThumb ? dp(24) : 0), dp(38));
            if (!spoilers.isEmpty()) {
                try {
                    canvas.save();
                    SpoilerEffect.clipOutCanvas(canvas, spoilers);
                    SpoilerEffect.layoutDrawMaybe(messageLayout, canvas);
                    canvas.restore();

                    for (int i = 0; i < spoilers.size(); i++) {
                        SpoilerEffect eff = spoilers.get(i);
                        eff.setColor(messagePaint.getColor());
                        eff.draw(canvas);
                    }
                } catch (Exception e) {
                    // fallback: draw without spoiler masking
                    messageLayout.draw(canvas);
                }
            } else {
                messageLayout.draw(canvas);
            }
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
            float badgeSize = dp(10);
            float badgeX = w - rightPadding - badgeSize - dp(4);
            float badgeY = dp(38) + dp(5);
            counterRect.set(badgeX, badgeY, badgeX + badgeSize, badgeY + badgeSize);
            canvas.drawRoundRect(counterRect, badgeSize / 2, badgeSize / 2, counterBgPaint);
        }

        // Divider removed to match visual preferences
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        avatarImage.onAttachedToWindow();
        mediaImage.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        avatarImage.onDetachedFromWindow();
        mediaImage.onDetachedFromWindow();
        // Clean up spoiler effects to avoid memory leaks
        spoilersPool.addAll(spoilers);
        spoilers.clear();
    }
}
