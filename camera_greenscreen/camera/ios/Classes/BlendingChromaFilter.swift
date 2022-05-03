//
//  BlendingChromaFilter.swift
//  camera
//
//  Created by Paul Freeman on 03/05/2022.
//

import Foundation


/// BlendingChromaFilter replaces instances of the input colour using red,green,blue values
/// with the transparent colour.  Threshold affects sensitivity and  smoothing provides a gradation
/// in the effect at the edge of a colour transition to transparent.
@available(iOS 11.0, *)
class BlendingChromaFilter: CIFilter {
     
    var cameraImage: CIImage?
    var backgroundImage: CIImage?
    
    var green: Float = 1.0
    var red:Float = 0.0
    var blue:Float = 0.0
    var threshold:Float = 0.4
    var smoothing:Float = 0.1
    static var myBundle:Bundle?
      
    static var kernel: CIColorKernel = { () -> CIColorKernel in
        
        guard let url = myBundle?.url(forResource: "ChromaBlendShader", withExtension: "metallib") else {
            fatalError("Unable to load ChromaBlendShader.metallib from \(myBundle?.bundlePath)")
        }
             
        guard let data = try? Data(contentsOf: url) else {
           fatalError("Unable to create data from ChromaBlendShader.metallib")
        }

        guard let kernel = try? CIColorKernel(
          functionName: "blendingChromaKernel",
          fromMetalLibraryData: data) else {
          fatalError("Unable to create color kernel")
        }

        return kernel
       }()

       override var outputImage : CIImage? {
        get {
            guard let cameraImage = cameraImage,
                  let backgroundImage = backgroundImage else {return nil}
            return BlendingChromaFilter.kernel.apply(
              extent: cameraImage.extent,
              roiCallback: { _, rect in
                return rect
              },
              arguments: [cameraImage, backgroundImage, red, green, blue, threshold, smoothing])
          }
       }
    
 }
 
