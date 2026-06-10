package com.hudgram.ui.settings;
import com.hudgram.core.HudConfig;

import android.view.View;

import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;

import java.util.ArrayList;

public class HudOtherSettingsActivity extends BaseHudSettingsActivity {

    private final int preferIPv6Row = rowId++;
    private final int nameOrderRow = rowId++;
    private final int idTypeRow = rowId++;
    private final int backupHeaderRow = rowId++;
    private final int exportSettingsRow = rowId++;
    private final int importSettingsRow = rowId++;

    private CharSequence getNameOrderString() {
        if (HudConfig.nameOrder == 1) {
            return getString("FirstLast");
        } else {
            return getString("LastFirst");
        }
    }

    private CharSequence getIdTypeString() {
        switch (HudConfig.idType) {
            case HudConfig.ID_TYPE_API:
                return getString("IdTypeAPI");
            case HudConfig.ID_TYPE_BOTAPI:
                return getString("IdTypeBOTAPI");
            case HudConfig.ID_TYPE_HIDDEN:
            default:
                return getString("IdTypeHidden");
        }
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asHeader(getString("HudSettingsOther")));
        
        items.add(UItem.asCheck(preferIPv6Row, getString("PreferIPv6")).slug("preferIPv6").setChecked(HudConfig.preferIPv6));
        items.add(UItem.asShadow(getString("Connection")));
        
        items.add(TextSettingsCellFactory.of(nameOrderRow, getString("NameOrder"), getNameOrderString()).slug("nameOrder"));
        items.add(UItem.asShadow(null));
        
        items.add(TextSettingsCellFactory.of(idTypeRow, getString("IdType"), getIdTypeString()).slug("idType"));
        items.add(UItem.asShadow(getString("IdTypeAbout")));

        items.add(UItem.asHeader(getString("BackupAndRestore")));
        items.add(TextSettingsCellFactory.of(exportSettingsRow, getString("ExportSettings"), null, R.drawable.msg_download, 0).slug("exportSettings"));
        items.add(TextSettingsCellFactory.of(importSettingsRow, getString("ImportSettings"), null, R.drawable.msg_openin, 0).slug("importSettings"));
        items.add(UItem.asShadow(null));
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        int id = item.id;
        if (id == preferIPv6Row) {
            HudConfig.toggleIPv6();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(HudConfig.preferIPv6);
            }
        } else if (id == nameOrderRow) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
            builder.setTitle(getString("NameOrder"));
            builder.setItems(new CharSequence[]{
                    getString("FirstLast"),
                    getString("LastFirst")
            }, (dialog, which) -> {
                HudConfig.setNameOrder(which == 0 ? 1 : 2);
                item.textValue = getNameOrderString();
                listView.adapter.notifyItemChanged(position, PARTIAL);
                org.telegram.messenger.NotificationCenter.getInstance(currentAccount).postNotificationName(org.telegram.messenger.NotificationCenter.mainUserInfoChanged);
            });
            showDialog(builder.create());
        } else if (id == idTypeRow) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
            builder.setTitle(getString("IdType"));
            builder.setItems(new CharSequence[]{
                    getString("IdTypeHidden"),
                    getString("IdTypeAPI"),
                    getString("IdTypeBOTAPI")
            }, (dialog, which) -> {
                HudConfig.setIdType(which);
                item.textValue = getIdTypeString();
                listView.adapter.notifyItemChanged(position, PARTIAL);
                org.telegram.messenger.NotificationCenter.getInstance(currentAccount).postNotificationName(org.telegram.messenger.NotificationCenter.mainUserInfoChanged);
            });
            showDialog(builder.create());
        } else if (id == exportSettingsRow) {
            exportSettings();
        } else if (id == importSettingsRow) {
            importSettings();
        }
    }

    private String backupJsonToSave;

    private void exportSettings() {
        if (getParentActivity() == null) return;
        String json = HudConfig.exportBackup();
        if (json == null) {
            org.telegram.ui.Components.BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_block2, getString("ExportError")).show();
            return;
        }
        backupJsonToSave = json;
        try {
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(android.content.Intent.CATEGORY_OPENABLE);
            intent.setType("application/json");
            intent.putExtra(android.content.Intent.EXTRA_TITLE, "hudgram_backup.json");
            startActivityForResult(intent, 103);
        } catch (Exception e) {
            org.telegram.messenger.FileLog.e(e);
            org.telegram.ui.Components.BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_block2, getString("ExportError")).show();
        }
    }

    private void importSettings() {
        if (getParentActivity() == null) return;
        try {
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_GET_CONTENT);
            intent.setType("application/json");
            intent.addCategory(android.content.Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, 99);
        } catch (Exception e) {
            org.telegram.messenger.FileLog.e(e);
        }
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, android.content.Intent data) {
        if (requestCode == 99 && resultCode == android.app.Activity.RESULT_OK && data != null && data.getData() != null) {
            try {
                android.net.Uri uri = data.getData();
                java.io.InputStream inputStream = getParentActivity().getContentResolver().openInputStream(uri);
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(inputStream));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                inputStream.close();
                boolean success = HudConfig.importBackup(stringBuilder.toString());
                if (success) {
                    org.telegram.ui.Components.BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_check_s, getString("ImportSuccess")).show();
                    if (listView != null && listView.adapter != null) {
                        listView.adapter.update(true);
                    }
                } else {
                    org.telegram.ui.Components.BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_block2, getString("ImportError")).show();
                }
            } catch (Exception e) {
                org.telegram.messenger.FileLog.e(e);
                org.telegram.ui.Components.BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_block2, getString("ImportError")).show();
            }
        } else if (requestCode == 103 && resultCode == android.app.Activity.RESULT_OK && data != null && data.getData() != null) {
            if (backupJsonToSave == null) return;
            try {
                android.net.Uri uri = data.getData();
                java.io.OutputStream outputStream = getParentActivity().getContentResolver().openOutputStream(uri);
                outputStream.write(backupJsonToSave.getBytes("UTF-8"));
                outputStream.close();
                org.telegram.ui.Components.BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_check_s, getString("ExportSuccess")).show();
            } catch (Exception e) {
                org.telegram.messenger.FileLog.e(e);
                org.telegram.ui.Components.BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_block2, getString("ExportError")).show();
            } finally {
                backupJsonToSave = null;
            }
        }
    }

    @Override
    protected String getActionBarTitle() {
        return getString("HudSettingsOther");
    }

    @Override
    protected String getKey() {
        return "o";
    }
}
