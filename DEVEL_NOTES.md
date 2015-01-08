Development notes for GH::watch
===============================

GitHub OAuth keys for login
---------------------------
GitHub OAuth keys necessary to allow user login to GitHub from GH::watch app must stay secure, 
so they are not stored in the public git repository.

If you want to compile app yourself then you have to Register GitHub application at https://github.com/settings/applications/new

Then you have to create text file `/src/com/daskiworks/ghwatch/backend/clients.properties` with content containing two OAuth 
secrets from the registered application:

````
clients=<GitHub OAuth Client Secret>
clienti=<GitHub OAuth Client ID>
````


UI theme generation
-------------------
[Android Action Bar Style Generator](http://jgilfelt.github.io/android-actionbarstylegenerator)

App colors used in generator:

* Base color: `#32689E`
* Accent color: `#66A5E5`
* Stacked color: `#4C8CCC`
* Action mode: `#00264C`

[Android Holo Colors Generator](http://android-holo-colors.com/)