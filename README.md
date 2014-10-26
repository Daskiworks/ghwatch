GH::watch
=========
Stay in touch with what is happening around GitHub project of your interest. 
This application for Android allows you to follow GitHub [notifications](https://help.github.com/articles/notifications) directly from your device.
GitHub fires it's notifications for repositories you are watching or for actions/discussion you participate in. 

This application periodically checks GitHub server for new unread notifications and fire Android notification 
with configurable sound and vibration to alert you. You can show list of unread GitHub notifications, filter 
them by repositories, mark one or all notifications as read, mute selected notification thread, or view detail 
informations about notified event. You can also show list of Watched repositories and unwatch them or view 
detailed informations.

<a href="https://play.google.com/store/apps/details?id=com.daskiworks.ghwatch" alt="Download from Google Play">
  <img src="http://www.android.com/images/brand/android_app_on_play_large.png">
</a>

Tip: You can lighten your email box thanks to this application also. 
Simply go to the GitHub.com > 'Account settings' > 'Notification center' and disable emails for notifications there.

Main features
-------------

###Authentication
* OAuth used
* No GithHub password stored on device
* You can revoke access from GitHub website at any time
* Support for GitHub two-factor authentication
* Info about logged in user in navigation drawer

###New GitHub notification detection
* Configurable pooling interval
* GitHub API limit saving pooling method used (with full check once a six hours on mobile and one hour on WiFi to prevent problems and allow widget decrease count when notification is read on another device) - **improved in 1.9**

###Android notification
* Android notification is fired when new unread GithHub notification is detected
* Notifications filter can be set in global Preferences and 'per watched repository' level. 
  This allows you to flexibly configure which type of GitHub notifications will be 
  fired as Android notifications. Available types are 'everything', 'participating only', 'nothing'.    
* Can be disabled at all
* Configurable sound
* Configurable vibration
* Inbox style Big view when more GitHub notifications available
* Action to directly mark GitHub notification as read - **new in 1.9**
  
###Homescreen and Lockscreen widget
* Shows number of unread Github notifications
* Number is highlighted if there are some new unread notifications from your last view of app. So you can use widget as less obtrusive 
  way instead of Android notifications.
* Resizeable  
  
###Unread notifications list view
* Shows list of unread GitHub notifications
* Shows notification type (Issue, Pull Request, ...) 
* Shows reason why you received notification (subscribed, author, comment, ...)
* Swipe down to refresh list - **new in 1.10**
* Swipe right to mark notification as read
* Click notification to view details - Detail informations are shown by 
  GitHub web (which is mobile friendly) or using GitHub android application, 
  so you see all necessary details and performs distinct actions (eg. reply in discussions etc.) directly.
* Shows list of repositories some notifications are available for, together with notifications count
* Notifications filtering by repository
* Context menu to Unsubscribe from selected notification thread
* Mark all unread notifications as read by one action
* Mark all unread notifications from selected repository as read by one action
* Manually refresh unread notifications list from server

###Watched repositories list view
* Shows list of repositories you watch on GitHub
* Swipe down to refresh list - **new in 1.10**
* Click repository to view details - Detail informations are shown 
  by GitHub web (which is mobile friendly) or using GitHub android 
  application, so you see all necessary details and performs distinct actions directly.
* Context menu to Unwatch repository
* Manually refresh list of watched repositories from server
* Notification Filter shown for each repository, you can overwrite 
  globally set filter on per repository basis there

###GitHub API request info
* Available from Preferences
* Shows info about GitHub API limit (limit, remaining, reset timestamp)
* Shows info about last GitHub API call 

###Support GH::watch Development dialog
* Dialog where you can take some actions to support GH::watch development
  * Rate app in Google Play
  * Share App info over social media
  * Donate small amount to the developer - In App payment
  * Report a Bug
  * Add Feature request

###Preferences persistence
* All important preferences (but no GitHub authentication details) are stored in Google cloud to be restored after device reset or on new device - **new in 1.10**
  
Changelog
---------

See [CHANGES.md](CHANGES.md) file content.

License for source code
-----------------------

   Copyright 2014 contributors as indicated by the @authors tag.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.


For details see [LICENSE.txt](LICENSE.txt) file content.
   