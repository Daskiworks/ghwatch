Development notes for GH::watch
===============================

GitHub OAuth keys for login
---------------------------
GitHub OAuth keys necessary to allow user login to GitHub from GH::watch app must stay secure, 
so they are not stored in the public git repository.

If you want to compile app yourself then you have to Register GitHub application at https://github.com/settings/applications/new

Then you have to create text file `/app/src/main/resources/com/daskiworks/ghwatch/backend/clients.properties` with content containing two OAuth 
secrets from the registered application:

````
clients=<GitHub OAuth Client Secret>
clienti=<GitHub OAuth Client ID>
````
