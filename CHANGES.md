GH::watch changelog
===================


- \#109 fixed login when password contains utf-8 characters
- \#108 added ability to log Github API call errors into file to better diagnose cause - must be enabled in app settings
- patched typo "due" to "due to" in distinct texts 

## v1.25 (Dec 4th, 2017)
- \#90 Dark theme implemented - it is available in app settings

## v1.24 (Nov 28th, 2017)
- \#103 notification and watched repository action titles updated to match GitHub
- minor android notification improvements (color, priority, category) 
- widget preview image updated
- \#105 fixed update of number in Launcher icon badge when bundled Android notification action is used 
- \#107 rare exceptions bugfix

## v1.23 (Nov 17th, 2017)
- \#84 Bundled notifications style for Nougat+ devices with actions for individual notifications
- \#101 improved quality of the icon used in the app widget and notification 
- \#99 \#100 rare exceptions bugfix

## v1.22 (Nov 9th, 2017)
- \#97 added gzip compression for GitHub REST API communication to save network bandwidth
- \#94 added app setting option to enable background notification check over WiFi only to save mobile data
- \#92 added longer period options (6h, 12h, 24h) to "New notifications check period" 
- \#93 changed default value of "New notifications check period" preference from 5 minutes to 1 hour to lover network bandwidth
- \#96 patched UI bug in "filter by repository" - list of repos scroll back to the top possible, correct dismiss of dialog when swiped down  
- \#98 homescreen widget design is now vertical for widget width 1 cell to better show content

## v1.21 (Oct 28th, 2017)
- \#46 update to the Material design
- \#65 sorting of "Filter by repository" list similar as on github (most notified repos first) 

## v1.20 (Jul 11th, 2017)
- \#88 added option to mark notification as read when clicked to be shown
- \#85 added support to load all notifications (more than 50 returned by one REST API call) 

## v1.19 (Jun 4th, 2017)
- \#80 control visibility of repositories in this app, use optimized REST API call if only one repo is visible
- \#75 show Labels related to the notification subject (Issue, Pull Request) - enable in preferences
- \#76 don't clear status bar color when updating notifications list, wait to get new status to change color
- \#77 added launcher icon badge with number of new notifications for launchers which support badges (using https://github.com/leolin310148/ShortcutBadger)  
- \#78 bigger menu button in notification list item for easier usability
- \#73 project updated to Android Studio and gradle build
- \#81 updated targetSdkVersion to 25, minSdkVersion to 16 (from 15), updated build tools, Google Play service and support libraries to latest versions
- \#87 \#89 patched bug in some notification view URL loading

## v1.18 (Nov 28th, 2015)
- \#57 - Shows current status of Issue or Pull Request the notification is for by color bar on left side (open - green, closed - red, merged - purple)

## v1.17 (Oct 28th, 2015)
- \#74 - bugfix of authentication fail on new device when old token exists on github.com  
- \#68 - bugfix for rare exception when notification list screen is rotated during loading info to show notification  

## v1.16 (Jan 28th, 2015)
- \#67 - bugfix of NPE during widget add and remove

## v1.15 (Jan 24th, 2015)
- \#62 - bugfix of NPE during OS start with widget used
- \#63 - tapping "unread notifications" in navigation drawer resets notifications filter  
- \#66 - "View" function removed from notification's dropdown menu as it is duplicate to row click 

## v1.14 (Jan 8th, 2015)
- Google Play Services updated to latest version
- \#60 - patched force close on some Android 5 devices due billing service binding

## v1.13 (Dec 4th, 2014)
- \#49 - unread notification vanishes from list (is marked as read) when user Unsubscribes from it.
- \#54 - android notification is removed if there is not any unread notification on Github server (eg. it was read on another device or web).
- \#54 - added preference which allows to enable better detection of reads on another device/web - background check do full read from Github API always,
no incremental updates. Use may lead to a bit higher consumption of resources (network, battery, GitHub API limit).
- minor UI improvements and patches 

## v1.12 (Nov 18th, 2014)
- \#53 - patched disabling of Android Notification from preferences when widget is present

## v1.11 (Nov 15th, 2014)
- \#52 - GA library upgraded from v2 to v4 to patch crash on Android 5
- \#50, #51 - increased size of menu buttons in lists items for simpler clicking
- \#48 - patched "Mark everything as read" action item is not shown bug
- \#47 - improved message when no any unread notification is available

## v1.10 (Oct 26th, 2014)
- \#45 - "Pull to refresh" patter used for lists of unread notifications an watched repositories 
- \#42 - backup app settings in the cloud to be restored after device reset or on new device 
- small texts polishing

## v1.9 (Sep 24th, 2014)
- \#25 - android notification now contains action to directly mark GitHub notifications as read
- \#38 - unread notifications counter in widget decreases if notif has been read on other devices (check once a 6 hours (1 hour on WiFi) only to save battery and data plan), thx squilter for report
- \#39 - click on widget/notification opens list of unread notifications always 
- \#40 - android notification is now fired when github unread notification is updated  
- no animation between StartActivity and MainActivity if user is logged in already

## v1.8 (Aug 26th, 2014)
- support for GitHub two-factor authentication 
- updated more colorful app design
- navigation drawer used for main navigation instead of action bar spinner
- info about current user shown in navigation drawer, click shows user profile on github 
- hide Android notification when app is opened
- GitHub API incremental update is patched now, so full update period of background check is increased from 1 to 6 hours to save data FUP
- patched notification detail opening for private repositories

## v1.7 (May 24th, 2014)
- added homescreen and lockscreen widget showing number of unread Github notifications. Number is highlighted 
  if there are some new unread notifications from your last view of app. So you can use this as less obtrusive 
  way instead of Android notifications.

## v1.6 (May 17th, 2014)
- background check for new notifications do full update once a hour (instead of optimized incremental update) to get 
  around current problem with GitHub API, when incremental update doesn't return any changes even if any exist.

## v1.5 (May 3rd, 2014)
- added 'Notification filter' which allows you to reduce number of fired android notifications 
  based on reason you received them (Everything, Participating only, Nothing). It is configurable 
  in global Preferences and can be overridden on 'Per watched repository' basis in 'Watched Repositories' view.

## v1.4 (Apr 15th, 2014)
- patched Back button on initial Login dialog. Thanks Lukas Koranda for report.
- other small improvements

## v1.3 (Apr 6th, 2014)
- Show notification reason in list (subscribed, mentioned, participating)
- 'Support Development' dialog (InApp billing perm)  

## v1.2 (Mar 15th, 2014)
- List of Watched repositories with possibility to show or unwatch repository
- GitHub API request info shown in Preferences
- Minor bugfix and info texts improvements

## v1.1 (Jan 15th, 2014)
- List of repositories unread notifications are available for is shown in navigation drawer (small devices) or left side panel (large devices, tablets).
- Repository may be selected to filter notifications.
- All unread notifications from selected repository may be marked as read. 
- Show 'updated_at' timestamp for GitHub notification in notifications list.
- Progress dialog shown when notification view URL is loaded from GitHub server can be cancelled now by back button.
- Various small GUI improvements (info text show and some actions are hidden if no notifications available etc.).

## v1.0 (Jan 2nd, 2014)
- first release with basic features