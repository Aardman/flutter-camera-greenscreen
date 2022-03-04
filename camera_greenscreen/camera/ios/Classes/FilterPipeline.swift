//
//  CIPipeline.swift
//  CoreImageGreenScreen
//
//  Created by Paul Freeman on 02/03/2022.
//

import UIKit
import CoreImage

@objc
public class FilterPipeline : NSObject {
    
    //Resources
    var ciContext : CIContext!
      
    //Filters
    @objc public var backgroundImageId:String? {
        willSet {
            guard let id = newValue else { return }
            if let backgroundImage = UIImage(named: id ) {
               backgroundCIImage = CIImage(image: backgroundImage)
            }
        }
    }
    var backgroundCIImage:CIImage?
    var chromaFilter:CIFilter?
    let compositor = CIFilter(name:"CISourceOverCompositing")
    
    //MARK:- Initialise pipeline
    @objc
    public override init(){
        super.init()
    }
    
    @objc
    public init(backgroundImageId:String){
        super.init()
        ///background image
        if let backgroundImage = UIImage(named: backgroundImageId ) {
           backgroundCIImage = CIImage(image: backgroundImage)
        }
        //one time init
        setupCoreImage()
        //filter stack
        chromaFilter = chromaKeyFilter()
    }
      
    ///The default hw device will be selected, currently a MTLDevice, no need to explicitly add
    ///using using the alternative constructor for CIContext
    func setupCoreImage(){
        ciContext = CIContext()
    }
    
    
    //MARK:- Filters
    func chromaKeyFilter() -> CIFilter? {
        CIFilter(name: "CIColorCube",
                 parameters: ["inputCubeDimension": Constants.cubeSize,
                              "inputCubeData": ChromaCubeFactory().chromaCubeData(fromHue: 0.35, toHue: 0.4)])
    }
    
    @objc
    //May need to lock pixel buffer
    public func filter(_ buffer:CVPixelBuffer?) {
        guard let buf = buffer else { return  }
        let outputImage = CIImage(cvPixelBuffer: buf, options:[:])
        guard let filtered = applyFilters(inputImage: outputImage) else { return }
        ciContext.render(filtered, to: buf)
    }
       
    func applyFilters(inputImage camImage: CIImage) -> CIImage? {
         
        if backgroundCIImage == nil {
          //Test background when there is no background image available
          let colourGen = CIFilter(name: "CIConstantColorGenerator")
          colourGen?.setValue(CIColor(red: 1.0, green: 0.0, blue: 0.0), forKey: "inputColor")
          backgroundCIImage = colourGen?.outputImage
          guard let _ = backgroundCIImage else { return camImage }
        }
        
        guard let chromaFilter = self.chromaFilter else {  return camImage }

        //Chroma
        chromaFilter.setValue(camImage, forKey: kCIInputImageKey)

        //Apply and composite
        if let sourceCIImageWithoutBackground = chromaFilter.outputImage {
            compositor?.setValue(sourceCIImageWithoutBackground, forKey: kCIInputImageKey)
            compositor?.setValue(backgroundCIImage, forKey: kCIInputBackgroundImageKey)
            guard let compositedCIImage = compositor?.outputImage   else { return nil}
            return compositedCIImage
        }
         
        return nil
    }
       
}
