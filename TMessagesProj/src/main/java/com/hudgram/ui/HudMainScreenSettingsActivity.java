package com.hudgram.ui;

import android.view.View;
import java.util.ArrayList;

import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

public class HudMainScreenSettingsActivity extends BaseHudSettingsActivity {

    private final int hideStoriesBarRow = rowId++;
    private final int showAvatarInHeaderRow = rowId++;
    private final int showMyNameInHeaderRow = rowId++;
    private final int showBioAsSubtitleRow = rowId++;
    private final int hideSettingsTabRow = rowId++;
    private final int hideSearchBarRow = rowId++;
    private final int hideFolderTabsRow = rowId++;
    private final int openArchiveOnPullRow = rowId++;

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asHeader(getString("MainScreen")));
        items.add(UItem.asCheck(hideStoriesBarRow, getString("HideStoriesBar")).slug("hideStoriesBar").setChecked(HudConfig.hideStoriesBar));
        items.add(UItem.asShadow(getString("HideStoriesBarAbout")));
        items.add(UItem.asCheck(showAvatarInHeaderRow, getString("ShowAvatarInHeader")).slug("showAvatarInHeader").setChecked(HudConfig.showAvatarInHeader));
        items.add(UItem.asShadow(getString("ShowAvatarInHeaderAbout")));
        items.add(UItem.asCheck(showMyNameInHeaderRow, getString("ShowMyNameInHeader")).slug("showMyNameInHeader").setChecked(HudConfig.showMyNameInHeader));
        items.add(UItem.asShadow(getString("ShowMyNameInHeaderAbout")));
        items.add(UItem.asCheck(showBioAsSubtitleRow, getString("ShowBioAsSubtitle")).slug("showBioAsSubtitle").setChecked(HudConfig.showBioAsSubtitle).setEnabled(HudConfig.showMyNameInHeader));
        items.add(UItem.asShadow(getString("ShowBioAsSubtitleAbout")));
        items.add(UItem.asCheck(hideSettingsTabRow, getString("HideSettingsTab")).slug("hideSettingsTab").setChecked(HudConfig.hideSettingsTab));
        items.add(UItem.asShadow(getString("HideSettingsTabAbout")));
        items.add(UItem.asCheck(hideSearchBarRow, getString("HideSearchBar")).slug("hideSearchBar").setChecked(HudConfig.hideSearchBar));
        items.add(UItem.asShadow(getString("HideSearchBarAbout")));
        items.add(UItem.asCheck(hideFolderTabsRow, getString("HideFolderTabs")).slug("hideFolderTabs").setChecked(HudConfig.hideFolderTabs));
        items.add(UItem.asShadow(getString("HideFolderTabsAbout")));
        items.add(UItem.asCheck(openArchiveOnPullRow, getString("OpenArchiveOnPull")).slug("openArchiveOnPull").setChecked(HudConfig.openArchiveOnPull));
        items.add(UItem.asShadow(null));
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        int id = item.id;
        if (id == hideStoriesBarRow) {
            HudConfig.toggleHideStoriesBar();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(HudConfig.hideStoriesBar);
            }
            org.telegram.messenger.NotificationCenter.getInstance(currentAccount).postNotificationName(org.telegram.messenger.NotificationCenter.storiesEnabledUpdate);
        } else if (id == showAvatarInHeaderRow) {
            HudConfig.toggleShowAvatarInHeader();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(HudConfig.showAvatarInHeader);
            }
            org.telegram.messenger.NotificationCenter.getInstance(currentAccount).postNotificationName(org.telegram.messenger.NotificationCenter.mainUserInfoChanged);
        } else if (id == showMyNameInHeaderRow) {
            HudConfig.toggleShowMyNameInHeader();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(HudConfig.showMyNameInHeader);
            }
            org.telegram.messenger.NotificationCenter.getInstance(currentAccount).postNotificationName(org.telegram.messenger.NotificationCenter.mainUserInfoChanged);
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        } else if (id == showBioAsSubtitleRow) {
            if (HudConfig.showMyNameInHeader) {
                HudConfig.toggleShowBioAsSubtitle();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HudConfig.showBioAsSubtitle);
                }
                org.telegram.messenger.NotificationCenter.getInstance(currentAccount).postNotificationName(org.telegram.messenger.NotificationCenter.mainUserInfoChanged);
            }
        } else if (id == hideSettingsTabRow) {
            HudConfig.toggleHideSettingsTab();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(HudConfig.hideSettingsTab);
            }
            org.telegram.messenger.NotificationCenter.getInstance(currentAccount).postNotificationName(org.telegram.messenger.NotificationCenter.mainUserInfoChanged);
        } else if (id == hideSearchBarRow) {
            HudConfig.toggleHideSearchBar();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(HudConfig.hideSearchBar);
            }
            org.telegram.messenger.NotificationCenter.getInstance(currentAccount).postNotificationName(org.telegram.messenger.NotificationCenter.mainUserInfoChanged);
        } else if (id == hideFolderTabsRow) {
            HudConfig.toggleHideFolderTabs();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(HudConfig.hideFolderTabs);
            }
            org.telegram.messenger.NotificationCenter.getInstance(currentAccount).postNotificationName(org.telegram.messenger.NotificationCenter.mainUserInfoChanged);
        } else if (id == openArchiveOnPullRow) {
            HudConfig.toggleOpenArchiveOnPull();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(HudConfig.openArchiveOnPull);
            }
            org.telegram.messenger.NotificationCenter.getInstance(currentAccount).postNotificationName(org.telegram.messenger.NotificationCenter.dialogsNeedReload);
        }
    }

    @Override
    protected String getActionBarTitle() {
        return getString("MainScreenSettings");
    }

    @Override
    protected String getKey() {
        return "m";
    }
}
