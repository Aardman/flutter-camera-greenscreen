//
//  CustomChromaFilter.swift
//  camera
//
//  Created by Paul Freeman on 13/04/2022.
//
 
import Foundation
import Metal
import CoreImage


//class CustomChromaFilter : CIFilter {
//
//       private let kernel: CIKernel
//
//       var inputImage: CIImage?
//
//       //custom shader parameters
//       var colourToReplace: (CGFloat, CGFloat, CGFloat) = (0.0, 1.0, 0.0);
//       var thresholdSensitivity: CGFloat = 0.4;
//       var smoothing: CGFloat = 0.1;
//
//       override init() {
//           let url = Bundle.main.url(forResource: "default", withExtension: "metallib")!
//           let data = try! Data(contentsOf: url)
//           guard #available(iOS 11, *) else { fatalError("not available prior to iOS 11"); }
//           kernel = try! CIKernel(functionName: "chroma", fromMetalLibraryData: data)
//           super.init()
//       }
//
//       required init?(coder: NSCoder) {
//           fatalError("init(coder:) has not been implemented")
//       }
//
//       override var outputImage: CIImage? {
//           guard let inputImage = self.inputImage else { return nil }
//
//           return self.kernel.apply(colorRed:   colourToReplace.0,
//                                    colorGreen: colourToReplace.1,
//                                    colorBlue:  colourToReplace.2,
//                                    threshold: thresholdSensitivity,
//                                    smoothing:smoothing);
//
//       }
//
//   }


