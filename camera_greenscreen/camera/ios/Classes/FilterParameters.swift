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
    
    var chromaKeyRange:HueRange?
    var maskColor:(Float, Float, Float)?
    var backgroundImage:String?
    var maskBounds:MaskBounds?
    var threshold:Float?

    @objc
    //Convenience for initialising default
    override public init()  {
        super.init()
    }
    
    @objc
    public init(backgroundImage:String = "",
                hueLow:Double = 0.2, hueHigh:Double = 0.65,
                red:Float = 0.0, green:Float = 1.0, blue:Float = 0.0,
                threshold:Float = 0.4,
                maskVertex1: CGPoint,
                maskVertex2: CGPoint,
                maskVertex3: CGPoint,
                maskVertex4: CGPoint){
        self.chromaKeyRange  = (hueLow, hueHigh)
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
        if let hueRange = dictionary[FilterParamNames.hueRange.rawValue] as? [NSNumber],
           let low  = hueRange[0] as? Double,
           let high = hueRange[1] as? Double {
                self.chromaKeyRange = (low, high)
       }
       if let colours = dictionary[FilterParamNames.colour.rawValue] as? [NSNumber],
          let red     = colours[0] as? Float,
          let green   = colours[1] as? Float,
          let blue    = colours[2] as? Float {
           self.maskColor = (red, green, blue)
        }
       if let sensitivity = dictionary[FilterParamNames.sensitivity.rawValue] as? NSNumber,
          let threshold = sensitivity as? Float {
             self.threshold = threshold
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
