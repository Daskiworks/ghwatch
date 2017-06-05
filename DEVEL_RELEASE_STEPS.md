Release steps for GH::watch app
================================

### Android Studio project build 

1. `com.daskiworks.ghwatch.backend.GHConstants` - set `DEBUG` to `false`
2. update `NewVersionInfoDialogFragment` version value and text if necessary (nothing shown if not changed)
3. `AndroidManifest.xml` - check android:versionCode and android:versionName
4. update `CHANGES.md` file 
5. update `README.md` file (mark features with `- **improved in x.y**` or `- **new in x.y**`)
6. commit and push everything into code git repos
7. run unit tests from `ghwatch-test` project (in Eclipse or using `ant clean debug install test`)
8. build signed APK - on the Android Studio menu bar, click `Build > Generate Signed APK` . Use file name like `ghwatch-x_y.apk`

### Github Repo https://github.com/Daskiworks/ghwatch

1. check [Issue tracker](https://github.com/Daskiworks/ghwatch/issues) - relevant Issues are in Milestone and are Closed
2. close current Milestone
3. prepare next Milestone

### Google Play

1. deploy signed APK, use changes description from `CHANGES.md` file

### Github repo release
1. create `vX.Y` tag in code git repo and push it
2. create new Release on GitHub for given tag, add changelog and `.apk` file to it

### Android Studio project prepare for next version devel

1. `AndroidManifest.xml` - increase `android:versionCode` and `android:versionName`
2. `com.daskiworks.ghwatch.backend.GHConstants` - set `DEBUG` to `true` to keep tracking stats cleaner

