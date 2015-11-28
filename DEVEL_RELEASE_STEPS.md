Release steps for GH::watch app
================================

### Eclipse project build 

1. `com.daskiworks.ghwatch.backend.GHConstants` - set `DEBUG` to `false`
2. update NewVersionInfoDialogFragment version value and text
3. `AndroidManifest.xml` - check android:versionCode and android:versionName
4. update `CHANGES.md` file 
5. update `README.md` file (mark features with `- **improved in x.y**` or `- **new in x.y**`)
6. commit everything (both `ghwatch` and `ghwatch-test` projects) to code git repos
7. run unit tests from `ghwatch-test` project (in Eclipse or using `ant clean debug install test`)
8. build signed APK - `Right click on project > Android Tools > Export Signed application Package...` 

### Github Repo https://github.com/Daskiworks/ghwatch

1. check [Issue tracker](https://github.com/Daskiworks/ghwatch/issues) - relevant Issues are in Milestone and are Closed
2. close current Milestone
3. prepare next Milestone

### Google Play

1. deploy signed APK, use changes description from `CHANGES.md` file


### Eclipse project prepare for next version devel

1. create `vX.Y` tag in both `ghwatch` and `ghwatch-test` code git repos
2. `AndroidManifest.xml` - increase `android:versionCode` and `android:versionName`
3. `com.daskiworks.ghwatch.backend.GHConstants` - set `DEBUG` to `true` to keep tracking stats cleaner

