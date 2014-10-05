Release steps for GH::watch app
================================

### Eclipse project build 

1. `com.daskiworks.ghwatch.backend.GHConstants` - set `DEBUG` to `false`
2. `AndroidManifest.xml` - check android:versionCode and android:versionName
3. update `CHANGES.md` file 
4. update `README.md` file
5. commit everything (both `ghwatch` and `ghwatch-test` projects) to code git repos
6. run unit tests from `ghwatch-test` project (in Eclipse or using `ant clean debug install test`)
7. build signed APK - `Right click on project > Android Tools > Export Signed application Package...` 

### Github Repo https://github.com/Daskiworks/ghwatch

4. check [Issue tracker](https://github.com/Daskiworks/ghwatch/issues) - relevant Issues are in Milestone and are Closed
5. close current Milestone
6. prepare next Milestone

### Google Play

1. deploy signed APK, use changes description from `CHANGES.md` file


### Eclipse project prepare for next version devel

1. create `vX.Y` tag in both `ghwatch` and `ghwatch-test` code git repos
2. `AndroidManifest.xml` - increase `android:versionCode` and `android:versionName`
3. `com.daskiworks.ghwatch.backend.GHConstants` - set `DEBUG` to `true` to keep tracking stats cleaner

