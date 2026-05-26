# Telegram Android (TMessagesProj) UI Architecture & Redesign Guide

This document is a comprehensive architectural guide of the user interface (UI) components and screens within the Telegram Android application codebase (`TMessagesProj`). It is tailored specifically to assist in **visual styling, visual custom redesigns, and layout modifications**.

---

## 1. Core Architectural Concepts

Unlike standard Android applications that use XML layouts (`res/layout/...`), Telegram Android is designed for extreme performance and smooth animations. Therefore, it uses a **custom programmatic rendering architecture**.

### Key Rules of the UI Engine:
1. **Single-Activity Architecture:** The entire app runs inside a single Android `Activity` class: [LaunchActivity](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/LaunchActivity.java).
2. **Fragment-Like Screen Management:** Individual screens are subclassed from [BaseFragment](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/ActionBar/BaseFragment.java) (instead of Android's default `Fragment`).
3. **Custom Navigation & Backstack:** Transitions, navigation, and backstacks are custom-drawn and managed by [ActionBarLayout](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/ActionBar/ActionBarLayout.java).
4. **Canvas-Based and Programmatic Drawing:** Views are built programmatically in Java. Complex UI elements (like chat bubbles and avatar badges) are drawn directly on Android's `Canvas` in custom views, bypassing the XML inflation overhead.
5. **Dynamic Theme Engine:** Colors, backgrounds, and sizes are resolved dynamically through [Theme](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/ActionBar/Theme.java). Styling changes should utilize theme keys (e.g., `Theme.key_windowBackgroundWhite`) or overwrite their color values.

---

## 2. The Core 10 Screens (Detailed Reference)

Below is a breakdown of the 10 primary UI screens in the application, detailing their functions, components, and exact classes.

---

### I. The Launch & Navigation Container Screen
* **Class Name:** [LaunchActivity](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/LaunchActivity.java)
* **Function:** The application's core hub. It intercepts intents (like share or deep links), hosts the main fragment container, and handles drawer navigation.
* **UI Components:**
  * **Navigation Drawer (Menu):** A custom sliding menu drawer displaying accounts, settings, folders, and other custom menu additions.
  * **ActionBarLayout container:** The view container that swaps individual `BaseFragment` screens.
  * **Passcode Lock View:** Shown if a lock code is set.
* **Secondary / Helper Classes:**
  * [DrawerLayoutContainer](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/ActionBar/DrawerLayoutContainer.java) (Custom navigation drawer holder)
  * [ActionBarLayout](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/ActionBar/ActionBarLayout.java) (Transitions and fragment management)

---

### II. The Chat List (Home) Screen
* **Class Name:** [DialogsActivity](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/DialogsActivity.java)
* **Function:** The primary home screen displaying active chat conversations, channels, and groups grouped under folders (tabs).
* **UI Components:**
  * **Chat List RecyclerView:** A custom scrollable list of conversations.
  * **Folder Tabs:** Swipeable tab bar displaying distinct chat folders.
  * **Search Bar:** A fast filtering bar on top of the list.
  * **Floating Action Button (FAB):** Pencil icon button to create groups, chats, or channels.
* **Secondary / Helper Classes:**
  * [DialogsAdapter](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/Adapters/DialogsAdapter.java) (Supplies the list items)
  * [DialogCell](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/Cells/DialogCell.java) (The programmatic layout for individual chat rows)

---

### III. The Chat Thread Screen
* **Class Name:** [ChatActivity](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java)
* **Function:** The highly detailed chat view showing messages, stickers, voice notes, media player, and input fields.
* **UI Components:**
  * **Message ListView:** A customized `RecyclerListView` designed for high scroll rates.
  * **Chat Input Bar:** Multi-functional input area with attachment drawers, emoji/sticker triggers, and voice record buttons.
  * **Reactions Bar:** Bubble layout overlay for reacting to messages.
  * **Pinned Message Header:** Sticky top layout with pinned messages.
* **Secondary / Helper Classes:**
  * [ChatMessageCell](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/Cells/ChatMessageCell.java) (Extremely complex canvas-drawing class for message bubbles, text, images, and bubbles shadows!)
  * [ChatActivityEnterView](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/Components/ChatActivityEnterView.java) (The bottom input view)

---

### IV. The Main Settings Screen
* **Class Name:** [SettingsActivity](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/SettingsActivity.java)
* **Function:** User options management screen containing user profile details, chat customization, notifications, and privacy options.
* **UI Components:**
  * **Avatar Profile Header:** Large profile picture with name, username, and bio.
  * **Settings Grid/List:** Standard categories (Notification settings, Privacy, Data/Storage, Theme).
* **Secondary / Helper Classes:**
  * [HeaderCell](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/Cells/HeaderCell.java) (Visual headers)
  * [TextCell](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/Cells/TextCell.java) / [TextSettingsCell](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/Cells/TextSettingsCell.java) (General menu item layouts)

---

### V. The Profile & Info Screen
* **Class Name:** [ProfileActivity](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/ProfileActivity.java)
* **Function:** Detail overview page for a contact, group, or channel. Shows settings, administrative controls, and shared media folders.
* **UI Components:**
  * **Collapsing Header Layout:** Dynamic profile avatar that shrinks as you scroll up.
  * **Shared Media Folders:** Grid tabs displaying Shared Photos, Files, Links, Voice, and GIFs.
  * **Action Bar:** Icons to call, search, mute, or edit profile.
* **Secondary / Helper Classes:**
  * [SharedMediaLayout](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/Components/SharedMediaLayout.java) (Manages the shared media lists and grids)

---

### VI. The Login & Authentication Screen
* **Class Name:** [LoginActivity](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/LoginActivity.java)
* **Function:** Handles registration, SMS authentication code entry, 2FA password verification, and account creation.
* **UI Components:**
  * **Country Selector:** Interactive sheet to select phone code.
  * **Code Fields:** Customized grid fields displaying SMS authentication codes.
  * **Password Input:** Secure field for 2FA verification.
* **Secondary / Helper Classes:**
  * [CountrySelectActivity](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/CountrySelectActivity.java) (Handles searching and selecting a country)

---

### VII. The Custom Theme Settings Screen
* **Class Name:** [ThemeActivity](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/ThemeActivity.java)
* **Function:** Theme styling and selection interface. Allows switching between Day, Night, and customized user-imported styles.
* **UI Components:**
  * **Theme Carousel Selector:** Beautiful horizontal cards showing theme previews.
  * **Day/Night Switcher:** Toggle and slider options to manage automatic scheduling.
  * **Color Palette Grid:** Grid showing preset color overlays.
* **Secondary / Helper Classes:**
  * [ThemePreviewActivity](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/ThemePreviewActivity.java) (Loads a full-screen interactive fake chat view simulating theme changes)

---

### VIII. The Contacts Screen
* **Class Name:** [ContactsActivity](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/ContactsActivity.java)
* **Function:** A simple search and select screen representing Telegram contacts sorted alphabetically.
* **UI Components:**
  * **Alphabet Index Scroll:** Right-side overlay for fast navigation by letter.
  * **Contacts List:** Vertical scroll list of contacts.
  * **Header Shortcuts:** "Find People Nearby", "Invite Friends".
* **Secondary / Helper Classes:**
  * [UserCell](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/Cells/UserCell.java) (Row layout showing name, profile picture, status)

---

### IX. Cache & Storage Management Screen
* **Class Name:** [CacheControlActivity](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/CacheControlActivity.java)
* **Function:** Critical utility screen detailing device storage space, database size, and clear cache options.
* **UI Components:**
  * **Donut Storage Chart:** Programmatic radial chart representing space distribution.
  * **Interactive Slider:** Standard custom slider to adjust "Keep Media" durations.
  * **Cache Lists:** Detailed breakdown of spaces by categories (Photos, Videos, Files).

---

### X. Group & Channel Creation Screens
* **Class Name:** [GroupCreateActivity](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/GroupCreateActivity.java) & [ChannelCreateActivity](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/ChannelCreateActivity.java)
* **Function:** Multi-step wizard to configure group names, add members, assign URLs, and select privacy levels.
* **UI Components:**
  * **Recycler List of Users:** Searchable select items.
  * **Input Form Layout:** Fields for name, description, and link validity.

---

## 3. Full Index of Secondary & Specialized Screens

In addition to the 10 core interfaces, the user interacts with **dozens of specialized sub-screens**. Below is the complete catalog categorized logically by functionality.

### A. Privacy, Security & Account Management
These screens handle secure access, permissions, and cloud credential operations:

1. **Privacy Settings Main Page**
   * **Class Name:** [PrivacySettingsActivity](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/PrivacySettingsActivity.java)
   * **Function:** Gatekeeper screen for all app safety features (2FA, active sessions, blocked lists, lock codes).
2. **Granular Privacy Controls**
   * **Class Name:** [PrivacyControlActivity](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/PrivacyControlActivity.java)
   * **Function:** Controls specific access levels (e.g. who can see profile pictures, phone numbers, last seen statuses, or forward links).
3. **Active Logged-In Sessions**
   * **Class Name:** [SessionsActivity](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/SessionsActivity.java)
   * **Function:** Displays all active devices connected to the account, listing details like IP address, client version, and device type with clear "Terminate" buttons.
4. **Cloud Password (2FA Verification)**
   * **Class Name:** [TwoStepVerificationActivity](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/TwoStepVerificationActivity.java)
   * **Function:** The password prompt displayed when logging in or entering protected security panels.
5. **Cloud Password Setup Wizard**
   * **Class Name:** [TwoStepVerificationSetupActivity](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/TwoStepVerificationSetupActivity.java)
   * **Function:** A step-by-step setup screen to establish/change recovery email, hints, and security keys.
6. **Passcode & Pin Setup**
   * **Class Name:** [PasscodeActivity](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/PasscodeActivity.java)
   * **Function:** Handles localized PIN/Password/Pattern settings, fingerprint unlock toggles, and autolock intervals.
7. **Biometric Passkeys**
   * **Class Name:** [PasskeysActivity](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/PasskeysActivity.java)
   * **Function:** Modern screen allowing enrollments of hardware/operating system passkeys for rapid authentication.

---

### B. Group & Channel Administration
Screens loaded by owners and administrators to moderate, customize, or analyze communities:

1. **Main Community Settings Editor**
   * **Class Name:** [ChatEditActivity](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/ChatEditActivity.java)
   * **Function:** Command center for group/channel profile pictures, types (public/private), topic setups, and member rosters.
2. **Community Type Selector**
   * **Class Name:** [ChatEditTypeActivity](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/ChatEditTypeActivity.java)
   * **Function:** Setup page for choosing usernames, custom invite links, and approving join request flows.
3. **Admin & User Permissions Board**
   * **Class Name:** [ChatRightsEditActivity](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/ChatRightsEditActivity.java)
   * **Function:** Granular control checklist to toggle permissions (e.g. sending media, pin messages, adding members) for standard users or administrators.
4. **Member & Ban Managers**
   * **Class Name:** [ChatUsersActivity](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/ChatUsersActivity.java)
   * **Function:** Searchable lists to review Admins, Members, Banned Users, and Kicked lists inside the community.
5. **Invite Link Manager**
   * **Class Name:** [ManageLinksActivity](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/ManageLinksActivity.java)
   * **Function:** Summary list showing all active invite links, who created them, how many users joined, and link-specific statistics.
6. **Custom Invite Link Creator**
   * **Class Name:** [LinkEditActivity](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/LinkEditActivity.java)
   * **Function:** Visual configuration overlay to set expiration dates, member limits, or toggling Admin Approval requirements for a specific link.
7. **Discussion Chat Connector**
   * **Class Name:** [ChatLinkActivity](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/ChatLinkActivity.java)
   * **Function:** Allows channels to link a specific public group as a comment section.
8. **Allowed Reactions Settings**
   * **Class Name:** [ChatReactionsEditActivity](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/ChatReactionsEditActivity.java)
   * **Function:** Checklist allowing admins to toggle exactly which emoji reactions are permitted in the chat.
9. **Admin Actions Audit Log**
   * **Class Name:** [ChannelAdminLogActivity](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/ChannelAdminLogActivity.java)
   * **Function:** Chronological stream detailing moderator events (e.g., deleted messages, banned users, username changes).

---

### C. Data, Network, & Storage Configuration
Utilities that manage network performance, disk space, and data usage structures:

1. **Main Data & Storage Settings**
   * **Class Name:** [DataSettingsActivity](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/DataSettingsActivity.java)
   * **Function:** Central selector for data usage saving settings, automatic downloads, background uploads, and system proxy integrations.
2. **Network Traffic Usage Monitors**
   * **Class Name:** [DataUsageActivity](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/DataUsageActivity.java) / [DataUsage2Activity](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/DataUsage2Activity.java)
   * **Function:** Gorgeous screens displaying detailed byte analytics, charts, and counts of data sent/received across Mobile, Wi-Fi, and Roaming connections.
3. **Proxy Setup List**
   * **Class Name:** [ProxyListActivity](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/ProxyListActivity.java)
   * **Function:** Shows configured SOCKS5 / MTProto proxies, their connection status, speed ping values, and active select checks.
4. **Proxy Parameter Creator**
   * **Class Name:** [ProxySettingsActivity](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/ProxySettingsActivity.java)
   * **Function:** Form inputs for server address, port numbers, usernames, passwords, or MTProto secrets.

---

### D. Media Viewers & Interactive Editors
Screens displaying rich multimedia content, interactive widgets, or media-creation tools:

1. **The Ultimate Photo & Video Viewer**
   * **Class Name:** [PhotoViewer](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/PhotoViewer.java)
   * **Function:** Fullscreen presentation player for photos and videos supporting swipe gestures, zoom, dynamic descriptions, and sharing.
   * **Visual Redesign Tip:** Houses the entire embedded canvas image editor (drawing tools, visual filters, saturation controllers, text, stickers overlays).
2. **Photo Crop & Rotation Canvas**
   * **Class Name:** [PhotoCropActivity](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/PhotoCropActivity.java)
   * **Function:** Dedicated screen displaying grid lines and angle rotators to crop or re-orient an image prior to setting as avatar or sending.
3. **Media Galleries & Albums Picker**
   * **Class Name:** [PhotoPickerActivity](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/PhotoPickerActivity.java) / [PhotoAlbumPickerActivity](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/PhotoAlbumPickerActivity.java)
   * **Function:** Multiselect grids scanning local gallery folders, complete with web search selectors (`PhotoPickerSearchActivity`) to choose and send images.
4. **Self-Destructing Secret Media Viewer**
   * **Class Name:** [SecretMediaViewer](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/SecretMediaViewer.java)
   * **Function:** Extremely secure overlay displaying self-destructing images/videos with animated timer widgets. Disables screenshots and blocks system overlay caching.
5. **Interactive Poll & Quiz Builder**
   * **Class Name:** [PollCreateActivity](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/PollCreateActivity.java)
   * **Function:** Multi-option form allowing users to create anonymous/public polls, choose multi-selection answers, or set up correct answers for Quiz Mode with customizable explanations.
6. **Dynamic Map & Location Picker**
   * **Class Name:** [LocationActivity](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/LocationActivity.java)
   * **Function:** Map screen (Google Maps API interface) that lets the user choose a static location pinpoint, search for nearby venues, or establish a live real-time location stream showing active movements on a map.

---

### E. Social Communication & Live VoIP calling
Real-time calling infrastructure, contact list management, and call history logs:

1. **One-on-One Call Screen**
   * **Class Name:** [VoIPFragment](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/VoIPFragment.java)
   * **Function:** Dynamic visual layout overlay displaying active voice/video call states, profile photo blurs, mic toggles, camera triggers, and speakers routes.
2. **Community Group Video / Voice Chat**
   * **Class Name:** [GroupCallActivity](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/GroupCallActivity.java)
   * **Function:** Heavily customized multi-speaker workspace listing participant cards, status indicators (speaking, muted, hands raised), screen-sharing previews, volume sliders, and stream recording triggers.
3. **Call History Logs**
   * **Class Name:** [CallLogActivity](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/CallLogActivity.java)
   * **Function:** Chronological feed representing incoming, outgoing, and missed voice or video calls with fast phone-tap shortcuts.
4. **Invite Friends SMS Hub**
   * **Class Name:** [InviteContactsActivity](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/InviteContactsActivity.java)
   * **Function:** Scans local contacts who do not have Telegram accounts, displaying invite checklists to send promotional SMS messages.

---

### F. Personalization, Style & Language
Custom styling controllers, layouts, and translations:

1. **Dynamic Language Selector**
   * **Class Name:** [LanguageSelectActivity](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/LanguageSelectActivity.java)
   * **Function:** Comprehensive list of language packs, integrated with a live search bar and auto-refresh indicators.
2. **Wallpaper Selection Gallery**
   * **Class Name:** [WallpapersListActivity](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/WallpapersListActivity.java)
   * **Function:** Catalog showcasing official custom chat backgrounds, Solid Color picker configurations, and live parallax/motion presets.

---

### G. Advanced Analytics, Premium & Utilities
Specialized growth trackers, paid portals, and advanced navigation components:

1. **Growth Analytics & Statistics**
   * **Class Name:** [StatisticActivity](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/StatisticActivity.java) / [MessageStatisticActivity](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/MessageStatisticActivity.java)
   * **Function:** Interactive chart views (drawn programmatically using canvas line engines) displaying member growth, notification mute ratios, viewer sources, and reaction numbers.
2. **Telegram Premium Portal**
   * **Class Name:** [PremiumPreviewFragment](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/PremiumPreviewFragment.java)
   * **Function:** Premium showcases displaying lists of exclusive benefits (faster download speeds, unique badge animations, double boundaries, automated filters) using outstanding visual slide carousels.
3. **Forum Topics List**
   * **Class Name:** [TopicsFragment](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/TopicsFragment.java)
   * **Function:** A message list feed categorizing conversations inside highly active groups enabled with Forum Threads.
4. **Custom QR Code Generator**
   * **Class Name:** [QrActivity](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/QrActivity.java)
   * **Function:** Shows dynamic QR vectors with centered icons, custom gradients, and sharing options.
5. **In-App Browser Container**
   * **Class Name:** [WebviewActivity](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/WebviewActivity.java)
   * **Function:** Internal web window displaying external sites, bot-enabled payment interfaces, or instant preview links.

---

## 4. Redesign Cheat Sheet: Where to Change Styles

If you are focusing on redesigning the visual style, here are the exact files and methods you should target:

| Redesign Target | Files to Modify | Primary Methods / Variables |
| :--- | :--- | :--- |
| **App-wide Colors & Themes** | [Theme.java](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/ActionBar/Theme.java) | Overwrite standard colors inside default themes (e.g. `ThemeColors`) |
| **Chat Bubbles Design** | [ChatMessageCell.java](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/Cells/ChatMessageCell.java) | `onDraw()` (Renders background bubbles, shadows, corners, text boundaries) |
| **Navigation Drawer Menu** | [LaunchActivity.java](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/LaunchActivity.java) | Search for `createDrawer()` / `DrawerLayoutContainer` |
| **Chats List Rows** | [DialogCell.java](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/Cells/DialogCell.java) | `onDraw()` (Customize spacing, status badges, item heights) |
| **Input Bar Layout** | [ChatActivityEnterView.java](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/Components/ChatActivityEnterView.java) | Search for input background fields, padding adjustment, and button placements |
| **Bottom Menus (Sheets)** | [BottomSheet.java](file:///f:/ProjectsAPP/TelegramApp/TMessagesProj/src/main/java/org/telegram/ui/ActionBar/BottomSheet.java) | Modify popup background drawable, round corners, and animations |
