//
//  CIPipeline.swift
//  CoreImageGreenScreen
//
//  Created by Paul Freeman on 02/03/2022.
//

import UIKit
import CoreImage
import AVFoundation

@objc
public class FilterPipeline : NSObject {
    
    //Resources
    var ciContext : CIContext!
      
    //Filters
    @objc public var filterParameters:FilterParameters = FilterParameters() {
         didSet {
             updateFilters()
         }
    }
     
    var backgroundCIImage:CIImage?
    var scaledBackgroundCIImage:CIImage?
    var chromaFilter:CIFilter?
    let compositor = CIFilter(name:"CISourceOverCompositing")
    
    //MARK:- Initialise pipeline
    @objc
    public override init(){
        super.init()
    }
    
    @objc
    public init(filterParameters:FilterParameters){
        super.init()
        ///background image
        if let backgroundImage = UIImage(named: filterParameters.backgroundImage) {
           backgroundCIImage = CIImage(image: backgroundImage)
        }
        //else load demo asset
        else {
            if let backgroundImage = UIImage(named: "demo_background") {
                backgroundCIImage = CIImage(image:backgroundImage)
            }
        }
        //one time init
        setupCoreImage()
        //filter stack
        chromaFilter = chromaKeyFilter(range: self.filterParameters.chromaKeyRange)
    }
      
    ///The default hw device will be selected, currently a MTLDevice, no need to explicitly add
    ///using using the alternative constructor for CIContext
    func setupCoreImage(){
        ciContext = CIContext()
    }
    
    
    //MARK:- Filters
    
    func updateFilters() {
        if let backgroundImage = UIImage(named: filterParameters.backgroundImage) {
           backgroundCIImage = CIImage(image: backgroundImage)
        }
        chromaFilter = chromaKeyFilter(range: self.filterParameters.chromaKeyRange)
    }
    
    
    func chromaKeyFilter(range: HueRange) -> CIFilter? {
        CIFilter(name: "CIColorCube",
                 parameters: ["inputCubeDimension": Constants.cubeSize,
                              "inputCubeData": ChromaCubeFactory().chromaCubeData(fromHue: range.0, toHue: range.1)])
    }
    
    
    @objc
    //No need to lock pixel buffer currently
    public func filter(_ buffer:CVPixelBuffer?) {
        guard let buf = buffer else { return  }
        let outputImage = CIImage(cvPixelBuffer: buf, options:[:])
        if let background = backgroundCIImage {
            scaledBackgroundCIImage = transformBackgroundToFit(backgroundCIImage: background, cameraImage: outputImage)
        }
        guard let filtered = applyFilters(inputImage: outputImage) else { return }
        ciContext.render(filtered, to: buf)
    }
       
    
    @objc
    @available(iOS 11.0, *)
    //For filtering the still image
    //photo?.normalisedData() performs any input transform, eg: rotation
    public func filter(asPhoto photo: AVCapturePhoto?) -> NSData? {
        guard let inputData =  photo?.normalisedData(),
              let inputImage = CIImage(data: inputData) else { return nil }
        if let background = backgroundCIImage {
            scaledBackgroundCIImage = transformBackgroundToFit(backgroundCIImage: background, cameraImage: inputImage)
        }
        guard let filtered = applyFilters(inputImage: inputImage),
              let colourspace = CGColorSpace(name:CGColorSpace.sRGB)
        else { return nil }
        guard
            let data = ciContext.jpegRepresentation(of: filtered, colorSpace:colourspace)
        else { return nil }
        return data as NSData?
    }
    
    
    /// Filters and transforms for the input image which must be correctly rotated
    /// prior to application of filters
    func applyFilters(inputImage camImage: CIImage) -> CIImage? {
         
        if scaledBackgroundCIImage == nil {
          //Test background when there is no background image available
          let colourGen = CIFilter(name: "CIConstantColorGenerator")
          colourGen?.setValue(CIColor(red: 1.0, green: 0.0, blue: 0.0), forKey: "inputColor")
          scaledBackgroundCIImage = colourGen?.outputImage
        }
          
        guard let chromaFilter = self.chromaFilter else {  return camImage }

        //Chroma
        chromaFilter.setValue(camImage, forKey: kCIInputImageKey)

        //Apply and composite with the background image
        guard let photoWithChromaColourRemoved = chromaFilter.outputImage else { return camImage }
        compositor?.setValue(photoWithChromaColourRemoved, forKey: kCIInputImageKey)
        compositor?.setValue(scaledBackgroundCIImage, forKey: kCIInputBackgroundImageKey)
        
        guard let compositedCIImage = compositor?.outputImage   else { return nil }
 
        return compositedCIImage
    }
      
    
    //MARK:- Handling background
    
   //Camera image is a correctly oriented CI image from the camera, ie: if an AVPhotoResponse
   //it has already been rotated to align with the input background
   func transformBackgroundToFit(backgroundCIImage:CIImage, cameraImage:CIImage) -> CIImage?  {
       let scaledImage = scaleImage(fromImage: backgroundCIImage, into: cameraImage.getSize())
       let croppedImage = cropImage(ciImage: scaledImage, toSize: cameraImage.getSize())
       return croppedImage
   }
    
    /// - into image is only provided for calculating the  desired size of the scaled output
    func scaleImage(fromImage:CIImage, into targetDimensions:CGSize) -> CIImage? {
        let sourceDimensions = fromImage.getSize()
        let scale = calculateScale(input: sourceDimensions, toFitWithinHeightOf: targetDimensions)
        guard let filter = CIFilter(name: "CILanczosScaleTransform") else { return nil }
        filter.setValue(fromImage, forKey: kCIInputImageKey)
        filter.setValue(scale,   forKey: kCIInputScaleKey)
        filter.setValue(1.0,     forKey: kCIInputAspectRatioKey)
        return filter.outputImage
    }
    
    //Scale to fit the height
    //We will allow the background to misalign with the center at this point. We may need
    //a CIAffineTransform step for that if width input <> output.
    func calculateScale(input: CGSize, toFitWithinHeightOf: CGSize) -> CGFloat {
        return  toFitWithinHeightOf.height / input.height
    }
    
    func cropImage(ciImage: CIImage?, toSize:CGSize) -> CIImage? {
        guard let image = ciImage else { return ciImage }
        return image
    }
    
}


//Helper extensions

extension CIImage {
    func getSize() -> CGSize {
        return CGSize(width: extent.width, height:extent.height)
    }
}


@available(iOS 11.0, *)
extension AVCapturePhoto {
    func normalisedData() -> Data? {
        fileDataRepresentation()
    }
}
 



