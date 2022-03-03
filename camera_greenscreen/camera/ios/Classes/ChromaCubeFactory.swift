//
//  ChromaCubeFactory.swift
//  CoreImageGreenScreen
//
//  Created by Paul Freeman on 02/03/2022.
//

import UIKit
 
enum Constants  {
   public static let cubeSize:Int  = 64
}

struct ChromaCubeFactory {
      
    func chromaCubeData(fromHue: CGFloat, toHue: CGFloat) -> Data {
        asData(chromaCube(fromHue:fromHue, toHue:toHue))
    }
     
    //re: warning probably unimportant here but SO suggests fix
    //https://stackoverflow.com/questions/60857760/warning-initialization-of-unsafebufferpointert-results-in-a-dangling-buffer
    func asData(_ cubeRGB:[Float]) -> Data {
        var cubeRGB = cubeRGB
        return Data(buffer: UnsafeBufferPointer(start: &cubeRGB, count: cubeRGB.count))
        //return Data(bytesNoCopy: &cubeRGB, count: cubeRGB.count, deallocator: .none)
    }
      
    // A Chroma Cube maps from and toHue values in the source
    // to transparent pixels in the output
    func chromaCube(fromHue: CGFloat, toHue: CGFloat) ->  [Float] {
        
       let cubeSize = Constants.cubeSize
       var cubeRGB = [Float]()
            
        for z in 0 ..< cubeSize {
            let blue = CGFloat(z) / CGFloat(cubeSize-1)
            for y in 0 ..< cubeSize {
                let green = CGFloat(y) / CGFloat(cubeSize-1)
                for x in 0 ..< cubeSize {
                    let red = CGFloat(x) / CGFloat(cubeSize-1)
                        
                    let hue = getHue(red: red, green: green, blue: blue)
                    let alpha: CGFloat = (hue >= fromHue && hue <= toHue) ? 0: 1
                        
                    cubeRGB.append(Float(red * alpha))
                    cubeRGB.append(Float(green * alpha))
                    cubeRGB.append(Float(blue * alpha))
                    cubeRGB.append(Float(alpha))
                }
            }
        }
        
        return cubeRGB
    }
    
    func getHue(red: CGFloat, green: CGFloat, blue: CGFloat) -> CGFloat
    {
        let color = UIColor(red: red, green: green, blue: blue, alpha: 1)
        var hue: CGFloat = 0
        color.getHue(&hue, saturation: nil, brightness: nil, alpha: nil)
        return hue
    }
    
}

