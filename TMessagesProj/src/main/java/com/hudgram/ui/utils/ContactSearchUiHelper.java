package com.hudgram.ui.utils;

import android.app.Activity;
import org.telegram.ui.ActionBar.INavigationLayout;
import org.telegram.ui.ActionBar.Theme;

public class ContactSearchUiHelper {

    public static void showSearchDialog(final Activity activity, final int currentAccount, final Theme.ResourcesProvider resourcesProvider, final INavigationLayout navigationLayout) {
        if (activity == null || navigationLayout == null) {
            return;
        }
        ContactSearchBottomSheet bottomSheet = new ContactSearchBottomSheet(activity, currentAccount, resourcesProvider, navigationLayout);
        bottomSheet.show();
    }
}
