//
//  FilterParameters.swift
//  camera
//
//  Created by Paul Freeman on 04/03/2022.
//

import Foundation
import CoreGraphics

typealias HueRange = (Double, Double)

@objc
public class FilterParameters: NSObject {
    var chromaKeyRange:HueRange
    var backgroundImage:String
    var maskBounds:Array<CGPoint>

    @objc
    //Convenience for initialising default
    override public init()  {
        self.chromaKeyRange  = (0.35, 0.45)
        self.backgroundImage = ""
        self.maskBounds = [CGPoint.zero, CGPoint.zero, CGPoint.zero, CGPoint.zero]
        super.init()
    }
    
    @objc
    public init(hueLow:Double = 0.35, hueHigh:Double = 0.45, backgroundImage:String = "",
                maskVertex1: CGPoint,
                maskVertex2: CGPoint,
                maskVertex3: CGPoint,
                maskVertex4: CGPoint){
        self.chromaKeyRange  = (hueLow, hueHigh)
        self.backgroundImage = backgroundImage
        self.maskBounds = [maskVertex1, maskVertex2, maskVertex3, maskVertex4]
    }
     
}
