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

###New GitHub notification detection

* Configurable pooling interval
* GitHub API limit saving pooling method used

###Android notification
* Android notification is fired when new unread GithHub notification is detected
* Notifications filter can be set in global Preferences and 'per watched repository' level. 
  This allows you to flexibly configure which type of GitHub notifications will be 
  fired as Android notifications. Available types are 'everything', 'participating only', 'nothing'. - **new in 1.5**    
* Can be disabled at all
* Configurable sound
* Configurable vibration
* Inbox style Big view when more GitHub notifications available
  
###Unread notifications list view

* Shows list of unread GitHub notifications
* Shows notification type (Issue, Pull Request, ...) 
* Shows reason why you received notification (subscribed, author, comment, ...)
* Swipe right to mark notification as read
* Click notification to view details - Detail informations are shown by 
  GitHub web (which is mobile friendly) or using GitHub android application, 
  so you see all necessary details and performs distinct actions (eg. reply in discussions etc.) directly.
* Shows list of repositories some notifications are available for, together with notifications count
* Notifications filtering by repository
* Context menu to Mute notification thread
* Mark all unread notifications as read by one action
* Mark all unread notifications from selected repository as read by one action
* Manually refresh unread notifications list from server

###Watched repositories list view

* Shows list of repositories you watch on GitHub
* Click repository to view details - Detail informations are shown 
  by GitHub web (which is mobile friendly) or using GitHub android 
  application, so you see all necessary details and performs distinct actions directly.
* Context menu to Unwatch repository
* Manually refresh list of watched repositories from server
* Notification Filter shown for each repository, you can overwrite 
  globally set filter on per repository basis there - **new in 1.5**

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