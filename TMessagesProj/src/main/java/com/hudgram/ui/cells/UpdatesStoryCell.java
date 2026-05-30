package com.hudgram.ui.cells;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.widget.FrameLayout;

import androidx.core.content.ContextCompat;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_stories;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.ButtonBounce;
import org.telegram.ui.Components.CombinedDrawable;
import org.telegram.ui.Components.RadialProgress;
import org.telegram.ui.Stories.StoriesController;
import org.telegram.ui.Stories.StoriesUtilities;

import java.util.ArrayList;

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

    // Interactive & Visual helpers
    private ButtonBounce cellBounce;
    private RadialProgress radialProgress;
    private boolean progressWasDrawn;
    private boolean isVerified;
    private Drawable verifiedDrawable;

    // Stories parameter
    private final StoriesUtilities.AvatarStoryParams storyParams = new StoriesUtilities.AvatarStoryParams(true);

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

        // Plus button background
        plusBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Plus icon paint
        plusPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        plusPaint.setStrokeWidth(dp(1.8f));
        plusPaint.setStrokeCap(Paint.Cap.ROUND);

        // Name text inside card at bottom
        namePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        namePaint.setTextSize(dp(11));
        namePaint.setTypeface(AndroidUtilities.bold());

        // Button Bounce animation for card press feedback
        cellBounce = new ButtonBounce(this, 1.0f, 3.0f);

        // Radial progress for uploading story state
        radialProgress = new RadialProgress(this);
        radialProgress.setBackground(null, true, false);

        storyParams.isDialogStoriesCell = true;

        updateColors();
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

        StoriesController storiesController = MessagesController.getInstance(currentAccount).getStoriesController();
        this.isUnread = peerStories != null &&
                storiesController.getUnreadState(dialogId) != StoriesController.STATE_READ;
        this.hasStories = peerStories != null && peerStories.stories != null && !peerStories.stories.isEmpty();

        // Load user/chat info
        TLRPC.User user = null;
        TLRPC.Chat chat = null;
        if (dialogId > 0) {
            user = MessagesController.getInstance(currentAccount).getUser(dialogId);
            if (user != null) {
                avatarDrawable.setInfo(currentAccount, user);
            }
        } else if (dialogId < 0) {
            chat = MessagesController.getInstance(currentAccount).getChat(-dialogId);
            if (chat != null) {
                avatarDrawable.setInfo(currentAccount, chat);
            }
        }

        this.isVerified = user != null ? user.verified : (chat != null && chat.verified);

        boolean isFail = storiesController.isLastUploadingFailed(dialogId);
        boolean isUploading = !Utilities.isNullOrEmpty(storiesController.getUploadingStories(dialogId)) || storiesController.getEditingStory(dialogId) != null;

        if (isSelf) {
            name = LocaleController.getString("UpdatesMyStatus", org.telegram.messenger.R.string.UpdatesMyStatus);
        } else if (user != null) {
            name = user.first_name == null ? "" : user.first_name.trim();
            int index = name.indexOf(" ");
            if (index > 0) {
                name = name.substring(0, index);
            }
        } else if (chat != null) {
            name = chat.title;
        }

        if (isFail) {
            name = LocaleController.getString("FailedStory", org.telegram.messenger.R.string.FailedStory);
            this.isVerified = false; // Don't show checkmark if failed
        } else if (isUploading) {
            name = LocaleController.getString("UploadingStory", org.telegram.messenger.R.string.UploadingStory);
            this.isVerified = false; // Don't show checkmark if uploading
        }

        updateColors();

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
            // No story content: use theme solid background
            cardImageReceiver.setImageBitmap(new ColorDrawable(Theme.getColor(Theme.key_windowBackgroundGray)));
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

    private void initVerifiedDrawable(Context context) {
        Drawable verifyDrawable = ContextCompat.getDrawable(context, org.telegram.messenger.R.drawable.verified_area).mutate();
        Drawable checkDrawable = ContextCompat.getDrawable(context, org.telegram.messenger.R.drawable.verified_check).mutate();
        CombinedDrawable combinedDrawable = new CombinedDrawable(verifyDrawable, checkDrawable) {
            @Override
            public void draw(Canvas canvas) {
                verifyDrawable.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteBlueIcon), PorterDuff.Mode.SRC_IN));
                checkDrawable.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
                super.draw(canvas);
            }
        };
        combinedDrawable.setFullsize(true);
        verifiedDrawable = combinedDrawable;
    }

    private void buildNameLayout() {
        if (name == null || name.isEmpty()) {
            nameLayout = null;
            return;
        }
        int maxWidth = dp(CARD_WIDTH) - dp(8);

        CharSequence textToDraw = name;
        if (isVerified) {
            SpannableStringBuilder builder = new SpannableStringBuilder(name);
            builder.append("  ");
            if (verifiedDrawable == null) {
                initVerifiedDrawable(getContext());
            }
            verifiedDrawable.setBounds(0, 0, dp(12), dp(12));
            ImageSpan span = new ImageSpan(verifiedDrawable, ImageSpan.ALIGN_CENTER);
            builder.setSpan(span, builder.length() - 1, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            textToDraw = builder;
        }

        nameLayout = new StaticLayout(
                TextUtils.ellipsize(textToDraw, namePaint, maxWidth * 2, TextUtils.TruncateAt.END),
                namePaint, maxWidth,
                Layout.Alignment.ALIGN_CENTER,
                1f, 0f, false
        );
    }

    public long getDialogId() {
        return dialogId;
    }

    @Override
    public void setPressed(boolean pressed) {
        super.setPressed(pressed);
        if (cellBounce != null) {
            cellBounce.setPressed(pressed);
        }
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

        // 2. Bottom gradient for text readability (only if hasStories)
        if (hasStories) {
            if (lastWidth != w || lastHeight != h) {
                gradient = new LinearGradient(0, h * 0.5f, 0, h,
                        Color.TRANSPARENT, 0xCC000000, Shader.TileMode.CLAMP);
                gradientPaint.setShader(gradient);
                lastWidth = w;
                lastHeight = h;
            }
            canvas.drawRect(0, 0, w, h, gradientPaint);
        }

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

        // Apply bounce scale only to the avatar drawing section (avatar, progress, plus, fail)
        float avatarScale = cellBounce != null ? cellBounce.getScale(0.08f) : 1f;
        canvas.save();
        canvas.scale(avatarScale, avatarScale, avatarCx, avatarCy);

        // Check uploading state
        StoriesController storiesController = MessagesController.getInstance(currentAccount).getStoriesController();
        ArrayList<StoriesController.UploadingStory> uploadingOrEditingStories = storiesController.getUploadingAndEditingStories(dialogId);
        boolean hasUploadingStories = (uploadingOrEditingStories != null && !uploadingOrEditingStories.isEmpty());
        boolean isFail = storiesController.isLastUploadingFailed(dialogId);
        boolean isUploading = !Utilities.isNullOrEmpty(storiesController.getUploadingStories(dialogId)) || storiesController.getEditingStory(dialogId) != null;
        boolean drawProgress = hasUploadingStories || (progressWasDrawn && radialProgress != null && radialProgress.getAnimatedProgress() < 0.98f);

        if (drawProgress) {
            float uploadingProgress = 0;
            boolean closeFriends = false;
            if (!hasUploadingStories) {
                uploadingProgress = 1f;
            } else {
                for (int i = 0; i < uploadingOrEditingStories.size(); i++) {
                    uploadingProgress += uploadingOrEditingStories.get(i).progress;
                }
                uploadingProgress = (storiesController.uploadedStories + uploadingProgress) / (storiesController.uploadedStories + uploadingOrEditingStories.size());
                closeFriends = uploadingOrEditingStories.get(uploadingOrEditingStories.size() - 1).isCloseFriends();
            }
            invalidate();

            // Draw avatar directly (without segment rings)
            avatarImageReceiver.draw(canvas);

            radialProgress.setDiff(0);
            Paint paint = closeFriends ?
                    StoriesUtilities.getCloseFriendsPaint(avatarImageReceiver) :
                    StoriesUtilities.getUnreadCirclePaint(avatarImageReceiver, true);
            paint.setAlpha(255);
            radialProgress.setPaint(paint);

            float avatarX = avatarCx - avatarR;
            float avatarY = avatarCy - avatarR;
            float avatarX2 = avatarCx + avatarR;
            float avatarY2 = avatarCy + avatarR;

            radialProgress.setProgressRect(
                    (int) (avatarX - dp(3)), (int) (avatarY - dp(3)),
                    (int) (avatarX2 + dp(3)), (int) (avatarY2 + dp(3))
            );
            radialProgress.setProgress(Utilities.clamp(uploadingProgress, 1f, 0), progressWasDrawn);
            radialProgress.draw(canvas);
            progressWasDrawn = true;
        } else {
            progressWasDrawn = false;
            if (hasStories) {
                // Draw unread/read ring and avatar dynamically with segmented borders!
                StoriesUtilities.drawAvatarWithStory(dialogId, canvas, avatarImageReceiver, hasStories, storyParams);
            } else {
                // No stories: draw avatar directly
                avatarImageReceiver.draw(canvas);
            }
        }

        // 4. "+" button for self (only if no stories and not uploading)
        boolean drawPlus = isSelf && !hasStories && !isUploading && !isFail;
        int cardBgColor = hasStories ? 0xFF1B2024 : Theme.getColor(Theme.key_windowBackgroundGray);
        if (drawPlus) {
            float plusR = dp(PLUS_SIZE / 2f);
            float plusCx = avatarCx + (LocaleController.isRTL ? -avatarR * 0.7f : avatarR * 0.7f);
            float plusCy = avatarCy + avatarR * 0.7f;

            // Dark outline matching card background color to blend perfectly
            avatarBgPaint.setColor(cardBgColor);
            canvas.drawCircle(plusCx, plusCy, plusR + dp(1.5f), avatarBgPaint);
            // Yellow plus circle
            plusBgPaint.setColor(Theme.getColor(Theme.key_telegram_color));
            canvas.drawCircle(plusCx, plusCy, plusR, plusBgPaint);
            // White/Black "+" symbol
            plusPaint.setColor(Theme.getColor(Theme.key_actionBarDefault));
            float l = dp(4f);
            canvas.drawLine(plusCx - l, plusCy, plusCx + l, plusCy, plusPaint);
            canvas.drawLine(plusCx, plusCy - l, plusCx, plusCy + l, plusPaint);
        }

        // 4.1. Red exclamation warning badge for failed upload
        if (isFail) {
            drawFail(canvas, avatarCx, avatarCy, cardBgColor);
        }

        // Restore avatarScale canvas
        canvas.restore();

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

    private void drawFail(Canvas canvas, float cx, float cy, int cardBgColor) {
        float cx2 = cx + (LocaleController.isRTL ? -dp(AVATAR_SIZE / 2f) * 0.7f : dp(AVATAR_SIZE / 2f) * 0.7f);
        float cy2 = cy + dp(AVATAR_SIZE / 2f) * 0.7f;

        // Draw dark outline
        plusBgPaint.setColor(cardBgColor);
        canvas.drawCircle(cx2, cy2, dp(9) + dp(1.5f), plusBgPaint);

        // Draw red circle
        plusBgPaint.setColor(Theme.getColor(Theme.key_text_RedBold));
        canvas.drawCircle(cx2, cy2, dp(9), plusBgPaint);

        // Draw white exclamation mark
        plusPaint.setColor(Color.WHITE);
        plusPaint.setStrokeWidth(dp(1.8f));

        // Exclamation mark line
        canvas.drawLine(cx2, cy2 - dp(3.5f), cx2, cy2 + dp(1f), plusPaint);
        // Exclamation mark dot
        canvas.drawPoint(cx2, cy2 + dp(3.5f), plusPaint);
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

    private void updateColors() {
        int cardBgColor;
        boolean isFail = MessagesController.getInstance(currentAccount).getStoriesController().isLastUploadingFailed(dialogId);

        if (isFail) {
            namePaint.setColor(Theme.getColor(Theme.key_text_RedBold));
            cardBgColor = Theme.getColor(Theme.key_windowBackgroundGray);
        } else if (hasStories) {
            cardBgColor = 0xFF1B2024; // Keep dark background under story thumbnail edges
            namePaint.setColor(Color.WHITE);
        } else {
            cardBgColor = Theme.getColor(Theme.key_windowBackgroundGray);
            namePaint.setColor(Theme.getColor(Theme.key_chats_name));
        }

        avatarBgPaint.setColor(cardBgColor);
        plusBgPaint.setColor(Theme.getColor(Theme.key_telegram_color));
        plusPaint.setColor(Theme.getColor(Theme.key_actionBarDefault));
    }
}
