package com.hudgram.ui.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewOutlineProvider;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.ui.ActionBar.Theme;

/**
 * Premium 3D drawer layout that reveals the side menu from BEHIND the main content.
 *
 * Architecture:
 * - The drawer (HudSideMenuView) sits at z-index 0 (background), always stationary.
 * - The main content view sits on top at z-index 1.
 * - When opening, only the content view animates: it scales down, gets rounded corners,
 *   translates to the right, and tilts slightly — revealing the drawer underneath.
 * - A shadow overlay on the content card adds depth.
 *
 */
public class Hud3DDrawerLayout extends FrameLayout {

    private View contentView;
    private View drawerView;
    private FrameLayout contentWrapper;
    private android.graphics.drawable.GradientDrawable contentBackgroundDrawable;

    // Shadow overlay drawn on top of content when drawer is open
    private final Paint shadowPaint = new Paint();
    private final android.graphics.RectF shadowRect = new android.graphics.RectF();
    private int lastShadowWidth = 0;

    private boolean isOpen = false;
    private boolean isDragging = false;
    private float startX;
    private float startY;
    private float lastX;
    private float drawerProgress = 0f; // 0.0 (closed) to 1.0 (open)

    private final int drawerWidth = AndroidUtilities.dp(280);
    private final int touchSlop;
    private ValueAnimator animator;

    // Premium animation interpolator (ease-out cubic)
    private static final PathInterpolator PREMIUM_INTERPOLATOR =
            new PathInterpolator(0.25f, 1f, 0.5f, 1f);

    private static final float CONTENT_SCALE_CLOSED = 1.0f;
    private static final float CONTENT_SCALE_OPEN = 0.85f; // Keep it large enough, rely on 3D tilt for effect
    private static final float CONTENT_ROTATION_Y = -18f; // Perfect balance of strong 3D tilt and readability
    private static final float CONTENT_CORNER_RADIUS_DP = 22f; // Refined corner radius
    private static final float CONTENT_SHADOW_MAX_ALPHA = 0.35f;
    private static final int ANIMATION_DURATION_OPEN = 350;
    private static final int ANIMATION_DURATION_CLOSE = 300;

    // Drawer parallax (subtle slide from left when opening)
    private static final float DRAWER_PARALLAX_DP = 50f;

    public Hud3DDrawerLayout(Context context) {
        super(context);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setClipChildren(false);
        setClipToPadding(false);
    }

    /**
     * Sets the drawer view (side menu). This is placed at z-index 0 (BEHIND everything).
     */
    public void setDrawerView(View view) {
        if (drawerView != null) {
            removeView(drawerView);
        }
        drawerView = view;
        // Add at index 0 — this goes BEHIND the content
        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        addView(drawerView, 0, lp);
        // Start fully visible but hidden behind content
        drawerView.setAlpha(1f);
    }

    /**
     * Sets the content view (main workspace). This is placed ON TOP of the drawer.
     */
    public void setContentView(View view) {
        if (contentWrapper != null) {
            removeView(contentWrapper);
        }
        contentView = view;
        contentWrapper = new FrameLayout(getContext());
        // Set GradientDrawable background so we can directly round the card surface
        contentBackgroundDrawable = new android.graphics.drawable.GradientDrawable();
        contentBackgroundDrawable.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        contentWrapper.setBackground(contentBackgroundDrawable);

        // Use hardware-accelerated outline provider for perfect anti-aliased rounded corners
        if (Build.VERSION.SDK_INT >= 21) {
            contentWrapper.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    float radius = AndroidUtilities.dp(CONTENT_CORNER_RADIUS_DP) * drawerProgress;
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
                }
            });
            contentWrapper.setClipToOutline(true);
        }

        // Add content to the wrapper
        contentWrapper.addView(contentView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        // Add the wrapper on top of the drawer
        addView(contentWrapper, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    public boolean isDrawerOpen() {
        return isOpen;
    }

    public void openDrawer(boolean animated) {
        if (isOpen && drawerProgress == 1f) return;
        isOpen = true;
        // Refresh theme colors and user info when the drawer is about to open
        if (drawerView instanceof HudSideMenuView) {
            ((HudSideMenuView) drawerView).updateThemeColors();
            ((HudSideMenuView) drawerView).updateUserProfile();
            ((HudSideMenuView) drawerView).playOpenAnimation();
        }
        animateToProgress(1f, animated ? ANIMATION_DURATION_OPEN : 0);
    }

    public void closeDrawer(boolean animated) {
        if (!isOpen && drawerProgress == 0f) return;
        isOpen = false;
        animateToProgress(0f, animated ? ANIMATION_DURATION_CLOSE : 0);
    }

    private void animateToProgress(float targetProgress, int duration) {
        if (animator != null) {
            animator.cancel();
        }

        if (duration <= 0) {
            updateDrawerProgress(targetProgress);
            return;
        }

        animator = ValueAnimator.ofFloat(drawerProgress, targetProgress);
        animator.setDuration(duration);
        animator.setInterpolator(PREMIUM_INTERPOLATOR);
        animator.addUpdateListener(animation -> updateDrawerProgress((float) animation.getAnimatedValue()));
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                animator = null;
                isOpen = targetProgress == 1f;
            }
        });
        animator.start();
    }

    private void updateDrawerProgress(float progress) {
        this.drawerProgress = progress;

        // Enable Hardware Acceleration for the main screen when 3D effect is active
        // This offloads the heavy 3D rendering and chat list drawing completely to the GPU, eliminating lag.
        if (contentWrapper != null) {
            if (progress > 0) {
                if (contentWrapper.getLayerType() != View.LAYER_TYPE_HARDWARE) {
                    contentWrapper.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                }
            } else {
                if (contentWrapper.getLayerType() != View.LAYER_TYPE_NONE) {
                    contentWrapper.setLayerType(View.LAYER_TYPE_NONE, null);
                }
            }
        }

        // ==============================================
        // 1. CONTENT VIEW — The main card that slides away
        // ==============================================
        if (contentWrapper != null) {
            float scale = CONTENT_SCALE_CLOSED - (CONTENT_SCALE_CLOSED - CONTENT_SCALE_OPEN) * progress;
            float transX;
            float rotY;
            if (LocaleController.isRTL) {
                transX = -drawerWidth * 0.95f * progress;
                rotY = -CONTENT_ROTATION_Y * progress;
            } else {
                transX = drawerWidth * 0.95f * progress;
                rotY = CONTENT_ROTATION_Y * progress;
            }

            // Pivot: left edge for LTR, right edge for RTL
            contentWrapper.setPivotX(LocaleController.isRTL ? contentWrapper.getWidth() : 0f);
            contentWrapper.setPivotY(contentWrapper.getHeight() / 2f);

            // Decreased to 1800 for a much more pronounced and sharp tilt
            contentWrapper.setCameraDistance(contentWrapper.getResources().getDisplayMetrics().density * 1800);

            contentWrapper.setScaleX(scale);
            contentWrapper.setScaleY(scale);
            contentWrapper.setTranslationX(transX);
            contentWrapper.setRotationY(rotY);

            // Elevation for shadow depth and update outline
            if (Build.VERSION.SDK_INT >= 21) {
                // We rely ENTIRELY on ViewOutlineProvider for corner clipping on the GPU
                // Modifying the Drawable's radius here would force a CPU redraw of the entire chat list!
                contentWrapper.setElevation(AndroidUtilities.dp(8) * progress);
                contentWrapper.invalidateOutline(); // Extremely fast, GPU-only outline update
            }
            // DO NOT call contentWrapper.invalidate() here! It causes massive lag.
        }

        // ==============================================
        // 2. DRAWER VIEW — Stationary behind content with parallax
        // ==============================================
        if (drawerView != null) {
            // Subtle parallax: drawer slides in slightly from the left as content opens
            float parallaxOffset = AndroidUtilities.dp(DRAWER_PARALLAX_DP);
            float drawerTranslation;
            if (LocaleController.isRTL) {
                drawerTranslation = parallaxOffset * (1f - progress);
            } else {
                drawerTranslation = -parallaxOffset * (1f - progress);
            }
            drawerView.setTranslationX(drawerTranslation);

            // Subtle alpha: drawer fades in slightly for polish
            drawerView.setAlpha(0.6f + 0.4f * progress);
        }

        invalidate();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        // Draw a semi-transparent overlay on the content card when drawer is open
        // This darkens the content to emphasize the drawer
        if (drawerProgress > 0f && contentWrapper != null) {
            canvas.save();
            canvas.concat(contentWrapper.getMatrix());
            canvas.translate(contentWrapper.getLeft(), contentWrapper.getTop());

            // Dynamic 3D Lighting Effect (Ambient Occlusion)
            // Creates a gradient shadow that is darker deeper into the screen
            if (shadowPaint.getShader() == null || lastShadowWidth != contentWrapper.getWidth()) {
                lastShadowWidth = contentWrapper.getWidth();
                int darkColor = Color.argb((int)(CONTENT_SHADOW_MAX_ALPHA * 255), 0, 0, 0);
                int lightColor = Color.argb(10, 0, 0, 0); // Almost transparent

                // If RTL, the left edge is deeper. If LTR, the right edge is deeper.
                android.graphics.LinearGradient gradient = new android.graphics.LinearGradient(
                        0, 0, lastShadowWidth, 0,
                        LocaleController.isRTL ? new int[]{darkColor, lightColor} : new int[]{lightColor, darkColor},
                        new float[]{0f, 1f},
                        android.graphics.Shader.TileMode.CLAMP
                );
                shadowPaint.setShader(gradient);
            }
            shadowPaint.setAlpha((int) (255 * drawerProgress));

            float radius = AndroidUtilities.dp(CONTENT_CORNER_RADIUS_DP) * drawerProgress;
            shadowRect.set(0, 0, contentWrapper.getWidth(), contentWrapper.getHeight());
            canvas.drawRoundRect(shadowRect, radius, radius, shadowPaint);

            canvas.restore();
        }
    }

    // ==============================================
    // TOUCH HANDLING (Gesture-based open/close)
    // ==============================================

    private boolean isSwipeAllowed() {
        if (contentView instanceof org.telegram.ui.ActionBar.DrawerLayoutContainer) {
            org.telegram.ui.ActionBar.DrawerLayoutContainer container = (org.telegram.ui.ActionBar.DrawerLayoutContainer) contentView;
            if (container.getParentActionBarLayout() != null) {
                java.util.List<org.telegram.ui.ActionBar.BaseFragment> stack = container.getParentActionBarLayout().getFragmentStack();
                if (stack != null && !stack.isEmpty()) {
                    org.telegram.ui.ActionBar.BaseFragment topFragment = stack.get(stack.size() - 1);
                    if (topFragment instanceof org.telegram.ui.MainTabsActivity) {
                        return true;
                    }
                }
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (contentView == null || drawerView == null || !isSwipeAllowed()) {
            return false;
        }

        int action = ev.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            startX = ev.getX();
            startY = ev.getY();
            lastX = startX;
            isDragging = false;

            if (!isOpen) {
                // Intercept ACTION_DOWN only if the touch is below the top ActionBar header (100dp).
                // This ensures ActionBar buttons (optionsItem, avatar, etc.) remain clickable,
                // while preserving edge swipes on the main chat list body.
                boolean isBelowHeader = startY > AndroidUtilities.dp(100);
                if (isBelowHeader) {
                    boolean edgeSwipe = LocaleController.isRTL ?
                            startX >= getWidth() - AndroidUtilities.dp(30) :
                            startX <= AndroidUtilities.dp(30);
                    if (edgeSwipe) {
                        return true;
                    }
                }
            } else {
                // If open, tapping on the content card closes it. We intercept immediately.
                boolean tapOnContent = LocaleController.isRTL ?
                        startX < getWidth() - drawerWidth :
                        startX > drawerWidth;
                if (tapOnContent) {
                    return true;
                }
            }
        } else if (action == MotionEvent.ACTION_MOVE) {
            float dx = ev.getX() - startX;
            float dy = ev.getY() - startY;

            if (Math.abs(dx) > touchSlop && Math.abs(dx) > Math.abs(dy)) {
                if (!isOpen) {
                    // Check if the gesture started from the edge
                    boolean edgeSwipe = LocaleController.isRTL ?
                            startX >= getWidth() - AndroidUtilities.dp(30) :
                            startX <= AndroidUtilities.dp(30);
                    // Check swipe direction: from right to left in RTL, left to right in LTR
                    boolean correctDirection = LocaleController.isRTL ? dx < 0 : dx > 0;
                    if (edgeSwipe && correctDirection) {
                        isDragging = true;
                        return true;
                    }
                } else {
                    // If open, horizontal swipe in either direction can drag it
                    isDragging = true;
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (contentView == null || drawerView == null || !isSwipeAllowed()) {
            return false;
        }

        int action = ev.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            startX = ev.getX();
            startY = ev.getY();
            lastX = startX;
            return true;
        } else if (action == MotionEvent.ACTION_MOVE) {
            float currentX = ev.getX();
            float dx = currentX - lastX;
            lastX = currentX;

            float deltaProgress = dx / drawerWidth;
            if (LocaleController.isRTL) {
                deltaProgress = -deltaProgress;
            }

            float newProgress = Math.max(0f, Math.min(1f, drawerProgress + deltaProgress));
            updateDrawerProgress(newProgress);
            return true;
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            // Determine velocity direction and gesture type
            float totalDx = ev.getX() - startX;
            float totalDy = ev.getY() - startY;
            boolean isTap = Math.abs(totalDx) < touchSlop && Math.abs(totalDy) < touchSlop;

            if (isOpen && isTap) {
                // If open and it's a tap, check if it was on the content card area
                boolean tapOnContent = LocaleController.isRTL ?
                        startX < getWidth() - drawerWidth :
                        startX > drawerWidth;
                if (tapOnContent) {
                    closeDrawer(true);
                    return true;
                }
            }

            boolean isQuickSwipe = Math.abs(totalDx) > AndroidUtilities.dp(40);
            if (isQuickSwipe) {
                // Quick swipe — follow the direction
                boolean openDirection = LocaleController.isRTL ? totalDx < 0 : totalDx > 0;
                if (openDirection) {
                    openDrawer(true);
                } else {
                    closeDrawer(true);
                }
            } else {
                // Snap to nearest state
                if (drawerProgress >= 0.45f) {
                    openDrawer(true);
                } else {
                    closeDrawer(true);
                }
            }
            return true;
        }
        return super.onTouchEvent(ev);
    }
}
