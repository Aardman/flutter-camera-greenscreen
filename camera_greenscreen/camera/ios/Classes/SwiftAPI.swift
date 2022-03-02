//
//  SwiftAPI.swift
//  objupswift
//
//  Created by Paul Freeman on 02/03/2022.
//
 
///This is used as the entry point to Swift code/API from Obj-C code
 
@objc
public class SwiftAPI: NSObject {
   
   @objc
    public static func log(_ msg:String) {
       print("SWIFT >> LOG: method=\(msg)")
       // To show call out to ObjC if this is required at later point
       // CameraPlugin.helloFromObj();
   }
   
}
 
