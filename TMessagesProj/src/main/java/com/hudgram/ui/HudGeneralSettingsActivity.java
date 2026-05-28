package com.hudgram.ui;

import android.view.View;

import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

import java.util.ArrayList;

public class HudGeneralSettingsActivity extends BaseHudSettingsActivity {

    private final int translationSettingsRow = rowId++;
    private final int mainScreenSettingsRow = rowId++;
    private final int commonSettingsRow = rowId++;

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        // === Sections / Sub-Settings Screens ===
        items.add(UItem.asHeader(getString("SettingsSections")));
        items.add(TextSettingsCellFactory.of(translationSettingsRow, getString("TranslationSettings"), null).slug("translationSettings"));
        items.add(TextSettingsCellFactory.of(mainScreenSettingsRow, getString("MainScreenSettings"), null).slug("mainScreenSettings"));
        items.add(TextSettingsCellFactory.of(commonSettingsRow, getString("CommonSettings"), null).slug("commonSettings"));
        items.add(UItem.asShadow(null));
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        int id = item.id;
        if (id == translationSettingsRow) {
            presentFragment(new HudTranslationSettingsActivity());
        } else if (id == mainScreenSettingsRow) {
            presentFragment(new HudMainScreenSettingsActivity());
        } else if (id == commonSettingsRow) {
            presentFragment(new HudCommonSettingsActivity());
        }
    }

    @Override
    protected String getActionBarTitle() {
        return getString("HudgramSettings");
    }

    @Override
    protected String getKey() {
        return "g";
    }
}
