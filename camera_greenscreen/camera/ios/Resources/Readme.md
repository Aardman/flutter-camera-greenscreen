The metal libary must be compiled offline, ie: from a terminal

    using metal tool to compile metal source into air file.
    
    xcrun -sdk iphoneos metal -c -target air64-apple-ios12.0 CustomShaders.ci.metal
    
    using metallib tool to assemble air files into metallib file.
    
    xcrun -sdk iphoneos metallib CustomShaders.ci.air -o default.metallib
    
The default.metallib file can also be compiled as part of a sample project and copied from the Build products in derived data to 
confirm that the shader code compiles and runs correctly.

Cocoapods can then make it available as a resource by adding the following line in camera.podspec 
 
  s.resources = "Resources/default.metallib"

NB:
Adding build configurations is insufficient as build scripts are also required 
To minimize complexity of Cocopods use, pre-compilation and Resources are simpler 
and likely requiring less maintenance than adding build rules to the generation process.
   
