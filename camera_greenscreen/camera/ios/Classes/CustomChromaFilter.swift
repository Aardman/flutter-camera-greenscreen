//
//  CustomCIFilterTest.swift
//  camera
//
//  Created by Paul Freeman on 13/04/2022.
//

import Foundation


/// CustomChromaFIlter replaces instances of the input colour using red,green,blue values
/// with the transparent colour.  Threshold affects sensitivity and  smoothing provides a gradation
/// in the effect at the edge of a colour transition to transparent.
@available(iOS 11.0, *)
class CustomChromaFilter: CIFilter {
     
    var inputImage: CIImage?
    
    var green: Float = 1.0
    var red:Float = 0.0
    var blue:Float = 0.0
    var threshold:Float = 0.4
    var smoothing:Float = 0.1
    static var myBundle:Bundle?
      
    static var kernel: CIColorKernel = { () -> CIColorKernel in
            
        guard let url = myBundle?.url(
          forResource: "ChromaShader",
          withExtension: "metallib"),
          let data = try? Data(contentsOf: url) else {
          fatalError("Unable to load metallib")
        }

        guard let kernel = try? CIColorKernel(
          functionName: "colorFilterKernel",
          fromMetalLibraryData: data) else {
          fatalError("Unable to create color kernel")
        }

        return kernel
       }()

       override var outputImage : CIImage? {
        get {
            guard let inputImage = inputImage else {return nil}
            return CustomChromaFilter.kernel.apply(
              extent: inputImage.extent,
              roiCallback: { _, rect in
                return rect
              },
              arguments: [inputImage, red, green, blue, threshold, smoothing])
          }
       }
    
 }
 
