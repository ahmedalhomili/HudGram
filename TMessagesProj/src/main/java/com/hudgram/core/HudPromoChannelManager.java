package com.hudgram.core;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;

public class HudPromoChannelManager {
    private static final String PREFS_NAME = "hudgram_promo_channel";
    private static final String KEY_CHANNEL_ID = "channel_id";
    private static final String KEY_ACCESS_HASH = "access_hash";
    private static final String KEY_RESOLVED_TIME = "resolved_time";
    private static final String KEY_HIDDEN = "is_hidden";
    private static final String CHANNEL_USERNAME = "hudgramchannel";

    private final int currentAccount;
    private long promoChannelId = 0L;
    private boolean isResolving = false;
    private TLRPC.Dialog mockDialog;

    private static final HudPromoChannelManager[] instances = new HudPromoChannelManager[4];

    public static boolean isInitialized(int account) {
        return instances[account] != null;
    }

    public static HudPromoChannelManager getInstance(int account) {
        if (instances[account] == null) {
            instances[account] = new HudPromoChannelManager(account);
        }
        return instances[account];
    }

    // Add any official channel, group, bot, or user IDs here to show the official mascot badge
    private static final long[] OFFICIAL_IDS = {
        3921220948L
        // Example: 123456789L, 987654321L
    };

    public static boolean isOfficialId(long id) {
        for (long officialId : OFFICIAL_IDS) {
            if (officialId == id) {
                return true;
            }
        }
        return false;
    }

    public static boolean isOfficialChannel(int account, long channelId) {
        if (channelId == 0) return false;
        return channelId == getInstance(account).getPromoChannelId() || isOfficialId(channelId);
    }

    public static boolean isOfficialDialog(int account, long dialogId) {
        if (dialogId == 0) return false;
        long absId = Math.abs(dialogId);
        return absId == getInstance(account).getPromoChannelId() || isOfficialId(absId);
    }

    public static boolean isOfficialChannel(int account, TLRPC.Chat chat) {
        return chat != null && isOfficialChannel(account, chat.id);
    }

    public static boolean isOfficialUser(int account, TLRPC.User user) {
        return user != null && isOfficialChannel(account, user.id);
    }

    public static boolean isOfficial(int account, TLRPC.User user, TLRPC.Chat chat) {
        return (user != null && isOfficialChannel(account, user.id)) || (chat != null && isOfficialChannel(account, chat.id));
    }

    private HudPromoChannelManager(int account) {
        this.currentAccount = account;
        SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME + "_" + account, Context.MODE_PRIVATE);
        promoChannelId = prefs.getLong(KEY_CHANNEL_ID, 0L);
        long accessHash = prefs.getLong(KEY_ACCESS_HASH, 0);
        long resolvedTime = prefs.getLong(KEY_RESOLVED_TIME, 0);

        // Clear the old collision-prone legacy ID (3964157552L collides with 330809744L in 32-bit)
        if (promoChannelId == 3964157552L) {
            promoChannelId = 0L;
            prefs.edit()
                 .remove(KEY_CHANNEL_ID)
                 .remove(KEY_ACCESS_HASH)
                 .remove(KEY_RESOLVED_TIME)
                 .apply();
            resolvedTime = 0;
        }

        if (promoChannelId != 0) {
            // Use direct map access to avoid calling getChat() before MessagesController finishes init
            // (calling getChat() would call isInitialized() -> getInstance() -> re-enter this constructor)
            TLRPC.Chat existing = MessagesController.getInstance(currentAccount).getChats().get(promoChannelId);
            if (existing == null) {
                TLRPC.Chat mockChat = createMockChat(promoChannelId, accessHash);
                MessagesController.getInstance(currentAccount).putChat(mockChat, true);
            }

            // Put mock dialog in dialogs_dict immediately to prevent recycled cells bug
            long dialogId = -promoChannelId;
            if (MessagesController.getInstance(currentAccount).dialogs_dict.get(dialogId) == null) {
                TLRPC.Dialog d = new TLRPC.TL_dialog();
                d.id = dialogId;
                d.peer = new TLRPC.TL_peerChannel();
                d.peer.channel_id = promoChannelId;
                d.top_message = 1;
                d.unread_count = 0;
                MessagesController.getInstance(currentAccount).dialogs_dict.put(dialogId, d);
            }

            loadCachedChat();
        }

        // Resolve username on start if not resolved or if resolved more than 24 hours ago
        if (promoChannelId == 0 || System.currentTimeMillis() - resolvedTime > 24 * 60 * 60 * 1000) {
            resolveChannel();
        }
    }

    private TLRPC.Chat createMockChat(long chatId, long accessHash) {
        TLRPC.TL_channel channel = new TLRPC.TL_channel();
        channel.id = chatId;
        channel.access_hash = accessHash;
        channel.title = LocaleController.getString("HudPromoChannelTitle", R.string.HudPromoChannelTitle);
        channel.username = CHANNEL_USERNAME;
        channel.left = true;
        channel.broadcast = true;
        channel.min = true; // Allow putChat to update this entry when real data arrives
        channel.photo = new TLRPC.TL_chatPhotoEmpty();
        return channel;
    }

    private void loadCachedChat() {
        if (promoChannelId == 0) return;

        // Query database asynchronously and load it into memory
        MessagesStorage.getInstance(currentAccount).getStorageQueue().postRunnable(() -> {
            TLRPC.Chat chat = MessagesStorage.getInstance(currentAccount).getChat(promoChannelId);
            if (chat != null) {
                AndroidUtilities.runOnUIThread(() -> {
                    MessagesController.getInstance(currentAccount).putChat(chat, true);
                    // Notify list to reload with data change to refresh views immediately
                    NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.dialogsNeedReload, true);
                });
            } else {
                // If not found in DB, resolve it again from servers
                AndroidUtilities.runOnUIThread(this::resolveChannel);
            }
        });
    }

    public void resolveChannel() {
        if (isResolving) return;
        isResolving = true;

        MessagesController.getInstance(currentAccount).getUserNameResolver().resolve(CHANNEL_USERNAME, peerId -> {
            isResolving = false;
            if (peerId != null && peerId < 0) { // Channel peerId is negative
                promoChannelId = -peerId; // channelId is positive in dialog
                
                // Get access_hash from the resolved chat!
                TLRPC.Chat resolvedChat = MessagesController.getInstance(currentAccount).getChat(promoChannelId);
                long accessHash = 0;
                if (resolvedChat != null) {
                    accessHash = resolvedChat.access_hash;
                }

                SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME + "_" + currentAccount, Context.MODE_PRIVATE);
                prefs.edit()
                     .putLong(KEY_CHANNEL_ID, promoChannelId)
                     .putLong(KEY_ACCESS_HASH, accessHash)
                     .putLong(KEY_RESOLVED_TIME, System.currentTimeMillis())
                     .apply();
                     
                // Put mock chat in memory immediately if not already there
                if (MessagesController.getInstance(currentAccount).getChat(promoChannelId) == null) {
                    TLRPC.Chat mockChat = createMockChat(promoChannelId, accessHash);
                    MessagesController.getInstance(currentAccount).putChat(mockChat, true);
                }

                // Put mock dialog in dialogs_dict immediately to prevent recycled cells bug
                long dialogId = -promoChannelId;
                if (MessagesController.getInstance(currentAccount).dialogs_dict.get(dialogId) == null) {
                    TLRPC.Dialog d = new TLRPC.TL_dialog();
                    d.id = dialogId;
                    d.peer = new TLRPC.TL_peerChannel();
                    d.peer.channel_id = promoChannelId;
                    d.top_message = 1;
                    d.unread_count = 0;
                    MessagesController.getInstance(currentAccount).dialogs_dict.put(dialogId, d);
                }

                // Ensure the chat is loaded into memory cache
                TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(promoChannelId);
                if (chat != null) {
                    AndroidUtilities.runOnUIThread(() -> {
                        NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.dialogsNeedReload, true);
                    });
                } else {
                    loadCachedChat();
                }
            }
        });
    }

    public ArrayList<TLRPC.Dialog> injectPromoIfNeeded(ArrayList<TLRPC.Dialog> dialogs) {
        if (promoChannelId == 0 || dialogs == null || isPromoHidden()) {
            return dialogs;
        }

        long dialogId = -promoChannelId; // negative channel ID is the dialog ID
        TLRPC.Dialog targetDialog = null;
        ArrayList<TLRPC.Dialog> cleanDialogs = new ArrayList<>(dialogs);

        // Find and remove the channel dialog if it natively exists in the list to avoid duplicate
        for (int i = 0; i < cleanDialogs.size(); i++) {
            TLRPC.Dialog d = cleanDialogs.get(i);
            if (d != null && d.id == dialogId) {
                targetDialog = d;
                cleanDialogs.remove(i);
                break;
            }
        }

        // If not found natively in the list, determine what dialog object to use
        if (targetDialog == null) {
            TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(promoChannelId);
            boolean isJoined = chat != null && !chat.left;

            if (isJoined) {
                // If user is already subscribed, find in existing memory dict or construct a dialog
                TLRPC.Dialog existingDialog = MessagesController.getInstance(currentAccount).dialogs_dict.get(dialogId);
                if (existingDialog != null) {
                    targetDialog = existingDialog;
                } else {
                    targetDialog = new TLRPC.TL_dialog();
                    targetDialog.id = dialogId;
                    targetDialog.peer = new TLRPC.TL_peerChannel();
                    targetDialog.peer.channel_id = promoChannelId;
                    targetDialog.top_message = 0;
                    targetDialog.unread_count = 0;
                    MessagesController.getInstance(currentAccount).dialogs_dict.put(dialogId, targetDialog);
                }
            } else {
                // If not subscribed, construct/use the mock dialog
                if (mockDialog == null || mockDialog.id != dialogId) {
                    mockDialog = new TLRPC.TL_dialog();
                    mockDialog.id = dialogId;
                    mockDialog.peer = new TLRPC.TL_peerChannel();
                    mockDialog.peer.channel_id = promoChannelId;
                    mockDialog.top_message = 1;
                    mockDialog.unread_count = 0;

                    // Create a mock welcome message in dialogMessage map so it displays nicely in the list
                    TLRPC.TL_message message = new TLRPC.TL_message();
                    message.id = 1;
                    message.dialog_id = dialogId;
                    message.peer_id = mockDialog.peer;
                    
                    message.message = LocaleController.getString("HudPromoChannelWelcome", R.string.HudPromoChannelWelcome);
                    message.date = (int) (System.currentTimeMillis() / 1000);

                    MessageObject messageObject = new MessageObject(currentAccount, message, (androidx.collection.LongSparseArray<TLRPC.User>) null, false, false);
                    ArrayList<MessageObject> messages = new ArrayList<>();
                    messages.add(messageObject);
                    MessagesController.getInstance(currentAccount).dialogMessage.put(dialogId, messages);

                    // Put in dialogs_dict so DialogCell can find it
                    MessagesController.getInstance(currentAccount).dialogs_dict.put(dialogId, mockDialog);
                }
                targetDialog = mockDialog;
            }
        }

        // Calculate the safe insert index: below folders (like Archive) to keep list layout clean
        int insertIndex = 0;
        for (int i = 0, size = cleanDialogs.size(); i < size; i++) {
            TLRPC.Dialog d = cleanDialogs.get(i);
            if (d != null && DialogObject.isFolderDialogId(d.id)) {
                insertIndex = i + 1;
            } else {
                break;
            }
        }

        // Clamp index to avoid IndexOutOfBoundsException
        if (insertIndex > cleanDialogs.size()) {
            insertIndex = cleanDialogs.size();
        }

        // Insert the channel dialog exactly at the calculated index (top of chats, below folders)
        cleanDialogs.add(insertIndex, targetDialog);
        return cleanDialogs;
    }

    public long getPromoChannelId() {
        return promoChannelId;
    }

    public TLRPC.Chat getPromoChat() {
        TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(promoChannelId);
        if (chat == null) {
            SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME + "_" + currentAccount, Context.MODE_PRIVATE);
            long accessHash = prefs.getLong(KEY_ACCESS_HASH, 0);
            chat = createMockChat(promoChannelId, accessHash);
        } else if (chat.access_hash == 0) {
            SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME + "_" + currentAccount, Context.MODE_PRIVATE);
            long accessHash = prefs.getLong(KEY_ACCESS_HASH, 0);
            if (accessHash != 0) {
                chat.access_hash = accessHash;
            }
        }
        return chat;
    }

    public TLRPC.Dialog getPromoDialog() {
        if (mockDialog == null) {
            mockDialog = new TLRPC.TL_dialog();
            mockDialog.id = -promoChannelId;
            mockDialog.peer = new TLRPC.TL_peerChannel();
            mockDialog.peer.channel_id = promoChannelId;
            mockDialog.top_message = 1;
            mockDialog.unread_count = 0;
        }
        return mockDialog;
    }

    public long getStoredAccessHash() {
        SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME + "_" + currentAccount, Context.MODE_PRIVATE);
        return prefs.getLong(KEY_ACCESS_HASH, 0);
    }

    public boolean isPromoHidden() {
        SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME + "_" + currentAccount, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_HIDDEN, false);
    }

    public void hidePromoChannel() {
        SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME + "_" + currentAccount, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_HIDDEN, true).apply();
        // Remove the mock dialog from in-memory dict so it doesn't linger
        if (promoChannelId != 0) {
            MessagesController.getInstance(currentAccount).dialogs_dict.remove(-promoChannelId);
        }
        // Trigger list reload so the cell disappears with animation
        AndroidUtilities.runOnUIThread(() ->
            NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.dialogsNeedReload, true)
        );
    }

    public void unhidePromoChannel() {
        SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME + "_" + currentAccount, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_HIDDEN).apply();
        AndroidUtilities.runOnUIThread(() ->
            NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.dialogsNeedReload, true)
        );
    }
}
