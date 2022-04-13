//
//  CustomCIFilterTest.swift
//  camera
//
//  Created by Paul Freeman on 13/04/2022.
//

import Foundation

@available(iOS 11.0, *)
class PassThroughFilter: CIFilter {
     
    var inputImage: CIImage?
    
    var green: Float = 1.0;
    var red:Float = 0.0;
    var blue:Float = 0.0;

    static var kernel: CIColorKernel = { () -> CIColorKernel in
        let url = Bundle.main.url(forResource: "MyKernels",
                                withExtension: "ci.metallib")!
        let data = try! Data(contentsOf: url)
        return try! CIColorKernel(functionName: "TestShader",  fromMetalLibraryData: data)
    }()

      override var outputImage : CIImage? {
        get {
            guard let input = inputImage else {return nil}
            return PassThroughFilter.kernel.apply(extent: input.extent,
                                             arguments: [input])
        }
    }
    
}
