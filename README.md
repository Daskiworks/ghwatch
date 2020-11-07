GH::watch
=========
Stay in touch with what is happening around GitHub project of your interest. 
This application for Android allows you to follow GitHub [notifications](https://help.github.com/articles/notifications) directly from your device.
GitHub fires it's notifications for repositories you are watching or for actions/discussion you participate in. 

This application periodically checks GitHub server for new unread notifications and fire Android notification 
with configurable sound and vibration to alert you. You can show list of unread GitHub notifications, filter 
them by repositories, mark one or all notifications as read, mute selected notification thread, or view detail 
information about notified event. You can also show list of Watched repositories and unwatch them or view 
detailed information.

<a href="https://play.google.com/store/apps/details?id=com.daskiworks.ghwatch" alt="Download from Google Play">
  <img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png'/>
</a>

Tip: You can lighten your email box thanks to this application also. 
Simply go to the [GitHub.com > 'Settings' > 'Notifications'](https://github.com/settings/notifications) and disable emails for notifications there.

Notifications from GitHub Private repositories
----------------------------------------------
It is possible that your GitHub Organization uses OAuth App access restrictions as documented 
in https://help.github.com/articles/about-oauth-app-access-restrictions, which prevents GH::watch 
to read notifications from private repositories. In this case you have to ask organization owner 
to allow access for gh::watch application.

Main features
-------------

### GUI
* Material design
* Light and Dark/Night theme (manual or [automatic](https://developer.android.com/reference/android/support/v7/app/AppCompatDelegate.html#MODE_NIGHT_AUTO) switching) - **new in 1.25**

### Authentication
* Android Account Manager used, account is visible in common Android Settings > Accounts - **new in 1.31**
* No GithHub password entered to the app nor stored on device as browser and GitHub Login page is used - **new  in 1.31**
* Full support for all GitHub two-factor authentications as GitHub Login page is used - **improved in 1.31**
* OAuth used for both authentication and authorization
* You can revoke access from GitHub website at any time
* Info about logged in user in navigation drawer

### New GitHub notification detection
* Configurable pooling interval - **improved in 1.22**
* GitHub API limit saving pooling method used (with full check once a six hours on mobile 
  and one hour on WiFi to prevent problems and allow widget decrease count/android notification remove when 
  notification is read on another device)
* Preference to configure full update for all background checks to better detect notification reads 
  on another device/web. Use may lead to higher consumption of resources (network, battery, GitHub API limit)!
* GitHub background pooling may be restricted to WiFi connection only to save mobile data - **new in 1.22**  

### Android notification
* Android notification is fired when new unread GithHub notification is detected
* Notifications filter can be set in global Preferences and 'per watched repository' level. 
  This allows you to flexibly configure which type of GitHub notifications will be 
  fired as Android notifications. Available types are 'everything', 'participating only', 'nothing'.    
* Can be disabled at all
* Configurable sound
* Configurable vibration
* Inbox style Big view when more GitHub notifications available on pre Nougat devices
* Bundled notifications style for Nougat+ devices with actions for individual notifications - **new in 1.23**
* Action to directly mark GitHub notification as read
* Launcher icon badges with number of new notifications for launchers which support badges 
  (using https://github.com/leolin310148/ShortcutBadger)
  
### Homescreen and Lockscreen widget
* Shows number of unread Github notifications
* Number is highlighted if there are some new unread notifications from your last view of app. So you can use 
  widget as less obtrusive way instead of Android notifications.
* Resizeable - **improved in 1.22** 
  
### Unread notifications list view
* Shows list of unread GitHub notifications from repositories enabled in this app (all by default)
* Shows notification type (Issue, Pull Request, ...) 
* Shows reason why you received notification (subscribed, author, comment, ...)
* Shows current status of Issue or Pull Request the notification is for by color bar on left side (open - green, closed - red, merged - purple) - this is a gift for users who support app development by small donation
* Shows Labels of Issue or Pull Request the notification is for (if enabled in Preferences) - this is a gift for users who support app development by small donation - **improved in 1.31**
* Swipe down to refresh list
* Swipe right to mark notification as read
* Click notification to view details - Detail informations are shown by 
  GitHub web (which is mobile friendly) or using GitHub android application, 
  so you see all necessary details and performs distinct actions (eg. reply in discussions etc.) directly.
* Option to mark notification as read when clicked to be shown (because GitHub android application doesn't do it) - **new in 1.20**
* Shows list of repositories some notifications are available for, together with notifications count
* Notifications filtering by repository
* Context menu to Unsubscribe from selected notification thread, given notification is marked as read also
* Mark all unread notifications as read by one action
* Mark all unread notifications from selected repository as read by one action
* Manually refresh unread notifications list from server

### Watched repositories list view
* Shows list of repositories you watch on GitHub
* Swipe down to refresh list
* Click repository to view details - Detail informations are shown 
  by GitHub web (which is mobile friendly) or using GitHub android 
  application, so you see all necessary details and performs distinct actions directly.
* Context menu to Unwatch repository
* Manually refresh list of watched repositories from server
* Notification Filter shown for each repository, you can overwrite 
  globally set filter on per repository basis there
* Repository visibility in this app shown for each repository, you can overwrite 
  globally set visibility on per repository basis there (so it is possible to hide 
  all repos in main Preferences and only selectively show ones you want)
    
### GitHub API request info
* Available from Settings
* Shows info about GitHub API limit (limit, remaining, reset timestamp)
* Shows info about last GitHub API call 

### Support GH::watch Development dialog
* Dialog where you can take some actions to support GH::watch development
  * Donate small amount to the app development by In App payment, you get small gift for this 
  * Rate app in Google Play
  * Share App info over social media
  * Report a Bug
  * Add Feature request

### Preferences persistence
* All important preferences (but no GitHub authentication details) are stored in Google 
  cloud to be restored after device reset or on new device
  
Changelog
---------
See [CHANGES.md](CHANGES.md) file content.

Third party libraries
---------------------
GH::watch uses these third party libraries, thanks to their developers:
* [ShortcutBadger](https://github.com/leolin310148/ShortcutBadger)
* [ChipCloud](https://github.com/fiskurgit/ChipCloud)
* [CircleImageView](https://github.com/hdodenhof/CircleImageView)

Notes for the app developers
----------------------------
See [DEVEL_NOTES.md](DEVEL_NOTES.md) file content.

License for source code
-----------------------
````
   Copyright 2014-2017 contributors as indicated by the @authors tag.
   
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
   
   http://www.apache.org/licenses/LICENSE-2.0
   
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
````

For details see [LICENSE.txt](LICENSE.txt) file content.
   
