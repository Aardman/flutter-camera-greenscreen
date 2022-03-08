## Flutter Camera Greenscreen plugin ##

A fork of the [main Flutter camera plugin](https://pub.dev/packages/camera) to add chroma-key greenscreen functionality.

This repo was created using [this guide for forking a single package from a monorepo](https://gist.github.com/alfredringstad/ac0f7a1e081e9ee485e653b6a8351212). That guide also contains details on how to merge in any future updates to the main plugin.


### Updating API ###

Changes were made to two packages 

**camera** (the main one) 

and 

**camera_platform_interface**

**camera** is dependent on **camera_platform_interface** and as a result, any changes to the API require editing the **camera_platform.dart** and **method_channel_camera.dart** files

dependencies are all pointing at this github repo in the **pubspec.yaml** files to keep changes relevant. This means also that the other modules **camera_web** and **camera_windows** are pointing at this repo for consistency. 

updating a dependency (package) requires following package updating housekeeping. 

For example, if making changes to the API in camera_platform_interface, the pubspec.yaml needs its version updating to reflect package evolution, after this is committed then dependent client packages require updating. 

If working on implementation of new API in the camera package (plugin) then this requires the additional steps, eg: in the application directory, such as **example** application 

```
$ rm camera/example/pubspec.lock
$ flutter pub get 
```

This will update **pubspec.cache** to contain the latest version of the API in **camera_plaform_interface**
