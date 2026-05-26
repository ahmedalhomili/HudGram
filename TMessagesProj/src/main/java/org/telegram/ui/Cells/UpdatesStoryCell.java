package org.telegram.ui.Cells;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_stories;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Stories.StoriesController;
import org.telegram.ui.Stories.StoriesUtilities;

public class UpdatesStoryCell extends FrameLayout {

    // Card background (story thumbnail)
    private final ImageReceiver cardImageReceiver;
    // Circular profile avatar
    private final ImageReceiver avatarImageReceiver;
    private final AvatarDrawable avatarDrawable;

    // Paints
    private final Paint gradientPaint;
    private final Paint avatarBgPaint;
    private final Paint plusBgPaint;
    private final Paint plusPaint;
    private final TextPaint namePaint;
    private final RectF rectF = new RectF();
    private final Path clipPath = new Path();

    // State
    private String name = "";
    private boolean isSelf;
    private boolean hasStories;
    private boolean isUnread;
    private long dialogId;
    private int currentAccount;
    private LinearGradient gradient;
    private int lastWidth, lastHeight;
    private StaticLayout nameLayout;

    // Stories parameter
    private final StoriesUtilities.AvatarStoryParams storyParams = new StoriesUtilities.AvatarStoryParams(false);

    // Dimensions
    private static final int CARD_WIDTH = 76;
    private static final int CARD_HEIGHT = 135;
    private static final int CARD_RADIUS = 14;

    // Avatar centered at top of card
    private static final int AVATAR_SIZE = 44;
    private static final int AVATAR_TOP = 18;
    private static final int PLUS_SIZE = 16;

    public UpdatesStoryCell(Context context) {
        super(context);
        setWillNotDraw(false);

        // Card background image
        cardImageReceiver = new ImageReceiver(this);
        cardImageReceiver.setRoundRadius(dp(CARD_RADIUS));

        avatarDrawable = new AvatarDrawable();

        // Circular avatar
        avatarImageReceiver = new ImageReceiver(this);
        avatarImageReceiver.setRoundRadius(dp(AVATAR_SIZE / 2));

        // Bottom gradient on card
        gradientPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Dark circle behind avatar (matches card background)
        avatarBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        avatarBgPaint.setColor(0xFF1B2024);

        // Plus button background (Yellow matching screenshot exactly!)
        plusBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        plusBgPaint.setColor(0xFFFAB814); // Gold/yellow color

        // Plus icon paint
        plusPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        plusPaint.setColor(0xFF1B2024); // Dark charcoal/black
        plusPaint.setStrokeWidth(dp(1.8f));
        plusPaint.setStrokeCap(Paint.Cap.ROUND);

        // Name text inside card at bottom
        namePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        namePaint.setTextSize(dp(11));
        namePaint.setColor(Color.WHITE);
        namePaint.setTypeface(AndroidUtilities.bold());
    }

    public void setStory(int currentAccount, TL_stories.PeerStories peerStories, boolean isSelf) {
        this.currentAccount = currentAccount;
        this.isSelf = isSelf;

        avatarImageReceiver.setRoundRadius(dp(AVATAR_SIZE / 2));

        if (peerStories != null && peerStories.peer != null) {
            this.dialogId = peerStories.peer.user_id != 0
                    ? peerStories.peer.user_id
                    : (peerStories.peer.chat_id != 0 ? -peerStories.peer.chat_id : -peerStories.peer.channel_id);
        } else if (isSelf) {
            this.dialogId = UserConfig.getInstance(currentAccount).getClientUserId();
        }

        this.isUnread = peerStories != null &&
                MessagesController.getInstance(currentAccount).getStoriesController().getUnreadState(dialogId) != StoriesController.STATE_READ;
        this.hasStories = peerStories != null && peerStories.stories != null && !peerStories.stories.isEmpty();

        // Load user/chat info
        TLRPC.User user = null;
        TLRPC.Chat chat = null;
        if (dialogId > 0) {
            user = MessagesController.getInstance(currentAccount).getUser(dialogId);
            if (user != null) {
                name = isSelf ? org.telegram.messenger.LocaleController.getString("UpdatesMyStatus", org.telegram.messenger.R.string.UpdatesMyStatus) : UserObject.getFirstName(user);
                avatarDrawable.setInfo(currentAccount, user);
            }
        } else if (dialogId < 0) {
            chat = MessagesController.getInstance(currentAccount).getChat(-dialogId);
            if (chat != null) {
                name = chat.title;
                avatarDrawable.setInfo(currentAccount, chat);
            }
        }

        // Build name layout
        buildNameLayout();

        // === Load card background ===
        boolean cardLoaded = false;
        if (hasStories) {
            TL_stories.StoryItem storyItem = peerStories.stories.get(peerStories.stories.size() - 1);

            if (storyItem.media instanceof TLRPC.TL_messageMediaPhoto && storyItem.media.photo != null) {
                TLRPC.PhotoSize size = FileLoader.getClosestPhotoSizeWithSize(storyItem.media.photo.sizes, 320);
                if (size != null) {
                    cardImageReceiver.setImage(
                            ImageLocation.getForPhoto(size, storyItem.media.photo), "76_135",
                            null, null, null, 0
                    );
                    cardLoaded = true;
                }
            } else if (storyItem.media instanceof TLRPC.TL_messageMediaDocument && storyItem.media.document != null) {
                TLRPC.PhotoSize thumbSize = FileLoader.getClosestPhotoSizeWithSize(storyItem.media.document.thumbs, 320);
                if (thumbSize != null) {
                    cardImageReceiver.setImage(
                            ImageLocation.getForDocument(thumbSize, storyItem.media.document), "76_135",
                            null, null, null, 0
                    );
                    cardLoaded = true;
                }
            }
        }

        if (!cardLoaded) {
            // No story content: use dark solid background
            cardImageReceiver.setImageBitmap(new ColorDrawable(0xFF1B2024));
        }

        // === Load circular avatar ===
        if (user != null) {
            avatarImageReceiver.setForUserOrChat(user, avatarDrawable);
        } else if (chat != null) {
            avatarImageReceiver.setForUserOrChat(chat, avatarDrawable);
        } else {
            avatarImageReceiver.setImageBitmap(avatarDrawable);
        }

        invalidate();
    }

    private void buildNameLayout() {
        if (name == null || name.isEmpty()) {
            nameLayout = null;
            return;
        }
        int maxWidth = dp(CARD_WIDTH) - dp(8);
        nameLayout = new StaticLayout(
                TextUtils.ellipsize(name, namePaint, maxWidth * 2, TextUtils.TruncateAt.END),
                namePaint, maxWidth,
                Layout.Alignment.ALIGN_CENTER,
                1f, 0f, false
        );
    }

    public long getDialogId() {
        return dialogId;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(
                MeasureSpec.makeMeasureSpec(dp(CARD_WIDTH), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(dp(CARD_HEIGHT), MeasureSpec.EXACTLY)
        );
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        int w = getMeasuredWidth();
        int h = getMeasuredHeight();

        // Card background fills entire view
        cardImageReceiver.setImageCoords(0, 0, w, h);

        // Avatar position centered at top
        int size = dp(AVATAR_SIZE);
        int ax = (w - size) / 2;
        int ay;
        if (isSelf) {
            int textHeight = nameLayout != null ? nameLayout.getHeight() : dp(15);
            int availableHeight = h - textHeight - dp(8);
            ay = (availableHeight - size) / 2;
        } else {
            ay = dp(AVATAR_TOP);
        }
        avatarImageReceiver.setImageCoords(ax, ay, size, size);
        storyParams.originalAvatarRect.set(ax, ay, ax + size, ay + size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getMeasuredWidth();
        int h = getMeasuredHeight();

        // Clip everything to card rounded rect
        canvas.save();
        clipPath.reset();
        rectF.set(0, 0, w, h);
        clipPath.addRoundRect(rectF, dp(CARD_RADIUS), dp(CARD_RADIUS), Path.Direction.CW);
        canvas.clipPath(clipPath);

        // 1. Draw card background
        cardImageReceiver.draw(canvas);

        // 2. Bottom gradient for text readability
        if (lastWidth != w || lastHeight != h) {
            gradient = new LinearGradient(0, h * 0.5f, 0, h,
                    Color.TRANSPARENT, 0xCC000000, Shader.TileMode.CLAMP);
            gradientPaint.setShader(gradient);
            lastWidth = w;
            lastHeight = h;
        }
        canvas.drawRect(0, 0, w, h, gradientPaint);

        // 3. Avatar - centered at top of card
        float avatarR = dp(AVATAR_SIZE) / 2f;
        float avatarCx = w / 2f;
        float avatarTopY;
        if (isSelf) {
            float textHeight = nameLayout != null ? nameLayout.getHeight() : dp(15);
            float availableHeight = h - textHeight - dp(8);
            avatarTopY = (availableHeight - dp(AVATAR_SIZE)) / 2f;
        } else {
            avatarTopY = dp(AVATAR_TOP);
        }
        float avatarCy = avatarTopY + avatarR;

        if (hasStories) {
            // Draw unread/read ring and avatar dynamically with segmented borders!
            StoriesUtilities.drawAvatarWithStory(dialogId, canvas, avatarImageReceiver, hasStories, storyParams);
        } else {
            // No stories (like self user initially): draw avatar directly
            avatarImageReceiver.draw(canvas);
        }

        // 4. "+" button for self (Yellow matching screenshot exactly!)
        if (isSelf) {
            float plusR = dp(PLUS_SIZE / 2f);
            float plusCx = avatarCx + avatarR * 0.7f;
            float plusCy = avatarCy + avatarR * 0.7f;

            // Dark outline matching card background color to blend perfectly
            canvas.drawCircle(plusCx, plusCy, plusR + dp(1.5f), avatarBgPaint);
            // Yellow plus circle
            canvas.drawCircle(plusCx, plusCy, plusR, plusBgPaint);
            // White/Black "+" symbol
            float l = dp(4f);
            canvas.drawLine(plusCx - l, plusCy, plusCx + l, plusCy, plusPaint);
            canvas.drawLine(plusCx, plusCy - l, plusCx, plusCy + l, plusPaint);
        }

        // 5. Name text inside card at bottom, center-aligned
        if (nameLayout != null) {
            canvas.save();
            float textX = (w - nameLayout.getWidth()) / 2f;
            canvas.translate(textX, h - nameLayout.getHeight() - dp(8));
            nameLayout.draw(canvas);
            canvas.restore();
        }

        // Restore clip
        canvas.restore();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        cardImageReceiver.onAttachedToWindow();
        avatarImageReceiver.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        cardImageReceiver.onDetachedFromWindow();
        avatarImageReceiver.onDetachedFromWindow();
    }
}
