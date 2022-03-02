//
//  import-plugin.h
//  Pods
//
//  Created by Paul Freeman on 02/03/2022.
//
#if __has_include(<camera/camera-Swift.h>)
#import <camera/camera-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "camera-Swift.h"
#endif
