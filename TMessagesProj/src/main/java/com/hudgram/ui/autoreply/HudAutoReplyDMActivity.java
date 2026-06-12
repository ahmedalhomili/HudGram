package com.hudgram.ui.autoreply;
import com.hudgram.ui.settings.BaseHudSettingsActivity;
import com.hudgram.core.HudConfig;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.ScaleStateListAnimator;
import org.telegram.ui.Components.Switch;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;
import org.telegram.ui.Components.FragmentFloatingButton;

import java.util.ArrayList;

public class HudAutoReplyDMActivity extends BaseHudSettingsActivity {

    private final int logRow = rowId++;

    private Switch actionBarSwitch;
    private FragmentFloatingButton fabContainer;
    private ArrayList<HudConfig.AutoReplyDMRule> rules;

    @Override
    public boolean onFragmentCreate() {
        rules = HudConfig.getAutoReplyDMRules();
        return super.onFragmentCreate();
    }

    @Override
    public ActionBar createActionBar(Context context) {
        ActionBar actionBar = super.createActionBar(context);
        ActionBarMenu menu = actionBar.createMenu();

        // Help info item
        menu.addItem(2, R.drawable.msg_info);

        actionBarSwitch = new Switch(context);
        actionBarSwitch.setColors(Theme.key_switchTrack, Theme.key_switchTrackChecked, Theme.key_windowBackgroundWhite, Theme.key_windowBackgroundWhite);
        actionBarSwitch.setChecked(HudConfig.autoReplyDMEnabled, false);
        actionBarSwitch.setOnCheckedChangeListener((view, isChecked) -> {
            if (HudConfig.autoReplyDMEnabled != isChecked) {
                HudConfig.toggleAutoReplyDMEnabled();
                updateFabVisibility();
                listView.adapter.update(true);
            }
        });

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

    @Override
    public View createView(Context context) {
        View view = super.createView(context);

        // FAB
        fabContainer = new FragmentFloatingButton(context, getResourceProvider());
        fabContainer.setImageResource(R.drawable.msg_add);
        fabContainer.setOnClickListener(v -> {
            presentFragment(new HudAutoReplyDMAddActivity(null));
        });

        contentView.addView(fabContainer, FragmentFloatingButton.createDefaultLayoutParams());

        updateFabVisibility();

        return view;
    }

    private void updateFabVisibility() {
        if (fabContainer != null) {
            fabContainer.setButtonVisible(HudConfig.autoReplyDMEnabled, true);
        }
    }

    private void showHelpDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(getString("HudAutoReplyDMHelpTitle"));
        builder.setMessage(getString("HudAutoReplyDMHelpText"));
        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
        showDialog(builder.create());
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        if (!HudConfig.autoReplyDMEnabled) {
            items.add(UItem.asShadow(getString("HudAutoReplyDMDisabledBanner")));
            return;
        }

        items.add(UItem.asShadow(getString("HudAutoReplyDMEnabled")));

        if (rules.isEmpty()) {
            items.add(UItem.asShadow(getString("HudAutoReplyDMEmpty")));
        } else {
            items.add(UItem.asHeader(getString("HudAutoReplyDMTitle")));
            for (int i = 0; i < rules.size(); i++) {
                HudConfig.AutoReplyDMRule rule = rules.get(i);
                UItem uItem = RuleCardCellFactory.of(100 + i, rule);
                uItem.accent = (i < rules.size() - 1);
                items.add(uItem);
            }
            items.add(UItem.asShadow(null));
        }

        // Log row
        items.add(UItem.asHeader(getString("HudAutoReplyDMLogHeader")));
        ArrayList<HudConfig.AutoReplyDMLogEntry> log = HudConfig.getAutoReplyDMLog();
        String logCount = log.isEmpty() ? getString("HudAutoReplyDMLogEmpty") : log.size() + " replies";
        items.add(TextSettingsCellFactory.of(logRow, getString("HudAutoReplyDMLog"), logCount).slug("dmLog"));
        items.add(UItem.asShadow(null));
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        if (item.id == logRow) {
            presentFragment(new HudAutoReplyDMLogActivity());
        } else if (item.id >= 100) {
            int index = item.id - 100;
            if (index >= 0 && index < rules.size()) {
                presentFragment(new HudAutoReplyDMAddActivity(rules.get(index)));
            }
        }
    }

    @Override
    protected boolean onItemLongClick(UItem item, View view, int position, float x, float y) {
        if (item.id >= 100) {
            int index = item.id - 100;
            if (index >= 0 && index < rules.size()) {
                HudConfig.AutoReplyDMRule rule = rules.get(index);
                showRuleOptionsDialog(rule, index);
                return true;
            }
        }
        return false;
    }

    private void showRuleOptionsDialog(HudConfig.AutoReplyDMRule rule, int index) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(rule.name.isEmpty() ? getString("HudAutoReplyDMRuleName") : rule.name);
        CharSequence[] optionItems = {
                LocaleController.getString("Edit", R.string.Edit),
                getString("HudAutoReplyDMDuplicate"),
                LocaleController.getString("Delete", R.string.Delete)
        };
        builder.setItems(optionItems, (dialog, which) -> {
            if (which == 0) {
                // Edit
                presentFragment(new HudAutoReplyDMAddActivity(rule));
            } else if (which == 1) {
                // Duplicate
                HudConfig.AutoReplyDMRule copy = rule.copy();
                HudConfig.addAutoReplyDMRule(copy);
                rules = HudConfig.getAutoReplyDMRules();
                listView.adapter.update(true);
                BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_saved, getString("HudAutoReplyDMSaved")).show();
            } else if (which == 2) {
                // Delete confirmation
                AlertDialog.Builder deleteBuilder = new AlertDialog.Builder(getParentActivity());
                deleteBuilder.setTitle(getString("HudAutoReplyDMDeleteConfirm"));
                deleteBuilder.setPositiveButton(LocaleController.getString("Delete", R.string.Delete), (d, w) -> {
                    HudConfig.deleteAutoReplyDMRule(rule.id);
                    rules = HudConfig.getAutoReplyDMRules();
                    listView.adapter.update(true);
                    BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_delete, getString("HudAutoReplyDMDeleted")).show();
                });
                deleteBuilder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                showDialog(deleteBuilder.create());
            }
        });
        showDialog(builder.create());
    }

    @Override
    public void onResume() {
        super.onResume();
        rules = HudConfig.getAutoReplyDMRules();
        if (actionBarSwitch != null) {
            actionBarSwitch.setChecked(HudConfig.autoReplyDMEnabled, false);
        }
        updateFabVisibility();
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
    }

    @Override
    protected String getActionBarTitle() {
        return getString("HudAutoReplyDMTitle");
    }

    @Override
    protected String getKey() {
        return "ardm";
    }

    // === Rule Card Cell Factory ===
    protected static class RuleCardCellFactory extends UItem.UItemFactory<RuleCardCell> {
        static {
            setup(new RuleCardCellFactory());
        }

        @Override
        public RuleCardCell createView(Context context, RecyclerListView listView, int currentAccount, int classGuid, Theme.ResourcesProvider resourcesProvider) {
            return new RuleCardCell(context);
        }

        @Override
        public void bindView(View view, UItem item, boolean divider, UniversalAdapter adapter, UniversalRecyclerView listView) {
            RuleCardCell cell = (RuleCardCell) view;
            cell.setData((HudConfig.AutoReplyDMRule) item.object, item.accent || divider);
        }

        public static UItem of(int id, HudConfig.AutoReplyDMRule rule) {
            UItem item = UItem.ofFactory(RuleCardCellFactory.class);
            item.id = id;
            item.object = rule;
            return item;
        }
    }

    protected static class RuleCardCell extends FrameLayout {
        private final TextView nameView;
        private final TextView summaryView;
        private final Switch enableSwitch;
        private boolean needsDivider;
        private HudConfig.AutoReplyDMRule currentRule;

        public RuleCardCell(Context context) {
            super(context);

            // Text container
            LinearLayout textContainer = new LinearLayout(context);
            textContainer.setOrientation(LinearLayout.VERTICAL);
            addView(textContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT,
                    Gravity.CENTER_VERTICAL,
                    LocaleController.isRTL ? 64 : 21, 12, LocaleController.isRTL ? 21 : 64, 12));

            // Rule name
            nameView = new TextView(context);
            nameView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            nameView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            nameView.setTypeface(AndroidUtilities.bold());
            nameView.setSingleLine(true);
            nameView.setEllipsize(TextUtils.TruncateAt.END);
            nameView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
            textContainer.addView(nameView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            // Summary
            summaryView = new TextView(context);
            summaryView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            summaryView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
            summaryView.setSingleLine(true);
            summaryView.setEllipsize(TextUtils.TruncateAt.END);
            summaryView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
            textContainer.addView(summaryView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 0));

            // Enable switch
            enableSwitch = new Switch(context);
            enableSwitch.setColors(Theme.key_switchTrack, Theme.key_switchTrackChecked, Theme.key_windowBackgroundWhite, Theme.key_windowBackgroundWhite);
            addView(enableSwitch, LayoutHelper.createFrame(37, 40,
                    (LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT) | Gravity.CENTER_VERTICAL,
                    14, 0, 14, 0));

            enableSwitch.setOnCheckedChangeListener((v, isChecked) -> {
                if (currentRule != null && currentRule.enabled != isChecked) {
                    HudConfig.toggleAutoReplyDMRule(currentRule.id);
                    currentRule.enabled = isChecked;
                    updateAlpha();
                }
            });

            enableSwitch.setOnClickListener(v -> {
                enableSwitch.setChecked(!enableSwitch.isChecked(), true);
            });

            setWillNotDraw(false);
        }

        private void updateAlpha() {
            float alpha = (currentRule != null && currentRule.enabled) ? 1.0f : 0.5f;
            nameView.setAlpha(alpha);
            summaryView.setAlpha(alpha);
        }

        public void setData(HudConfig.AutoReplyDMRule rule, boolean divider) {
            currentRule = rule;
            needsDivider = divider;

            nameView.setText(rule.name.isEmpty() ? "—" : rule.name);
            enableSwitch.setChecked(rule.enabled, false);

            // Build summary
            StringBuilder sb = new StringBuilder();
            // Match mode
            switch (rule.matchMode) {
                case 1: sb.append("⊃ "); break;
                case 2: sb.append("= "); break;
                default: sb.append("✱ "); break;
            }
            // Scope
            switch (rule.scope) {
                case 1: sb.append(LocaleController.getString("FilterGroups", R.string.FilterGroups)); break;
                case 2: sb.append(LocaleController.getString("NotificationsPrivateChats", R.string.NotificationsPrivateChats)); break;
                default: sb.append(LocaleController.getString("FilterAllChatsShort", R.string.FilterAllChatsShort)); break;
            }
            // Schedule
            if (rule.scheduleEnabled) {
                sb.append(" • ⏰");
            }
            // Reply mode
            switch (rule.replyMode) {
                case 1: sb.append(" • 🔀"); break;
                case 2: sb.append(" • 🧠"); break;
            }

            summaryView.setText(sb.toString());
            updateAlpha();
            invalidate();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(
                    MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(60), MeasureSpec.EXACTLY)
            );
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (needsDivider && Theme.dividerPaint != null) {
                int startX = LocaleController.isRTL ? 0 : AndroidUtilities.dp(21);
                int endX = getMeasuredWidth() - (LocaleController.isRTL ? AndroidUtilities.dp(21) : 0);
                canvas.drawLine(startX, getMeasuredHeight() - 1, endX, getMeasuredHeight() - 1, Theme.dividerPaint);
            }
        }
    }
}
