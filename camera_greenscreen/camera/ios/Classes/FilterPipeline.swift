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
    
    //BACKGROUND Image for comping
    var backgroundCIImage:CIImage?
    var chromaFilter:CIFilter?
    let compositor = CIFilter(name:"CISourceOverCompositing")
    
    //Initialise CI resources once as this is a costly operation
    @objc
    public override init(){
        super.init()
        if let backgroundImage = UIImage(named: "morph") {
            backgroundCIImage = CIImage(image: backgroundImage)
        }
        chromaFilter = chromaKeyFilter()
    }
     
    func chromaKeyFilter() -> CIFilter? {
        CIFilter(name: "CIColorCube",
                 parameters: ["inputCubeDimension": Constants.cubeSize,
                              "inputCubeData": ChromaCubeFactory().chromaCubeData(fromHue: 0.2, toHue: 0.4)])
    }
    
    @objc
    public func applyFilters(inputImage camImage: CIImage) -> CIImage? {
         
        guard let backgroundCIImage = backgroundCIImage,
              let filter = self.chromaFilter else { return nil }
         
        //Chroma
        filter.setValue(camImage, forKey: kCIInputImageKey)
        
        //Apply and composite
        if let sourceCIImageWithoutBackground = filter.outputImage {
            compositor?.setValue(sourceCIImageWithoutBackground, forKey: kCIInputImageKey)
            compositor?.setValue(backgroundCIImage, forKey: kCIInputBackgroundImageKey)
            guard let compositedCIImage = compositor?.outputImage   else { return nil}
            return compositedCIImage
        }
         
        fatalError("could not apply filter")
    }
      
 
}
