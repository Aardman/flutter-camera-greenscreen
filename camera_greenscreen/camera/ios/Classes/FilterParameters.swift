//
//  FilterParameters.swift
//  camera
//
//  Created by Paul Freeman on 04/03/2022.
//

import Foundation
import CoreGraphics

typealias HueRange   = (Double, Double)
typealias MaskBounds = Array<CGPoint>

enum FilterParamNames: String {
    case backgroundPath
    case colour
    case sensitivity
    case hueRange
    case polygon
}

@objc
public class FilterParameters: NSObject {
    
    var backgroundImage:String?
    var maskColor:(Float, Float, Float)?
    var threshold:Float?
    var maskBounds:MaskBounds?

    @objc
    //Convenience for initialising default
    override public init()  {
        super.init()
    }
    
    @objc
    public init(backgroundImage:String = "",
                red:Float = 0.0, green:Float = 1.0, blue:Float = 0.0,
                threshold:Float = 0.4,
                maskVertex1: CGPoint,
                maskVertex2: CGPoint,
                maskVertex3: CGPoint,
                maskVertex4: CGPoint){
        self.maskColor = (red, green, blue)
        self.backgroundImage = backgroundImage
        self.maskBounds = [maskVertex1, maskVertex2, maskVertex3, maskVertex4]
        self.threshold = threshold
    }
    
   @objc
   public init(dictionary: Dictionary<String,AnyObject>){
        if let filename = dictionary[FilterParamNames.backgroundPath.rawValue] as? String {
            backgroundImage = filename
        }
       if let colours = dictionary[FilterParamNames.colour.rawValue] as? [NSNumber],
          let red     = colours[0] as? Float,
          let green   = colours[1] as? Float,
          let blue    = colours[2] as? Float {
          self.maskColor = (red/255.0, green/255.0, blue/255.0)
        }
       if let sensitivity = dictionary[FilterParamNames.sensitivity.rawValue] as? NSNumber {
           self.threshold = sensitivity.floatValue
       }
       if let vectorBounds = dictionary[FilterParamNames.polygon.rawValue] as? [[NSNumber]],
          let point1 = CGPoint(polygonPoint:vectorBounds[0]),
          let point2 = CGPoint(polygonPoint:vectorBounds[1]),
          let point3 = CGPoint(polygonPoint:vectorBounds[2]),
          let point4 = CGPoint(polygonPoint:vectorBounds[3])
       {
          self.maskBounds = [point1, point2, point3, point4]
       }
   }
}

extension CGPoint {
    init?(polygonPoint:[NSNumber]) {
        if polygonPoint.count != 2 { return nil }
        self.init()
        let xnum = polygonPoint[0] as NSNumber
        self.x = xnum.doubleValue
        let ynum = polygonPoint[1] as NSNumber
        self.y = ynum.doubleValue
    }
}
