//
//  CustomCIShaders.metal
//  camera
//
//  Created by Paul Freeman on 13/04/2022.
//

#include <CoreImage/CoreImage.h> // includes CIKernelMetalLib.h
using namespace metal;
 

extern "C" float4 PassThrough (coreimage::sample_t s, coreimage::destination dest)
{
     
//    if (s.g > 0.25){
//       return float4(2.0, 0.0, 0.0, 1.0);
//    }
    return s;
    
}
