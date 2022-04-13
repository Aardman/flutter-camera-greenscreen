//from https://github.com/satoshi0212/ARKitChromaKeySamples/blob/master/ChromaKeyMetal/ChromaKeyMetal/Shaders.metal
#include <metal_stdlib>
using namespace metal;

kernel void chroma(texture2d<float, access::read> inTexture [[ texture(0) ]],
                            texture2d<float, access::write> outTexture [[ texture(1) ]],
                            const device float *colorRed [[ buffer(0) ]],
                            const device float *colorGreen [[ buffer(1) ]],
                            const device float *colorBlue [[ buffer(2) ]],
                            const device float *threshold [[ buffer(3) ]],
                            const device float *smoothing [[ buffer(4) ]],
                            uint2 gid [[ thread_position_in_grid ]])
{
    const float4 inColor = inTexture.read(gid);
    const float3 maskColor = float3(*colorRed, *colorGreen, *colorBlue);

    const float3 YVector = float3(0.2989, 0.5866, 0.1145);

    const float maskY = dot(maskColor, YVector);
    const float maskCr = 0.7131 * (maskColor.r - maskY);
    const float maskCb = 0.5647 * (maskColor.b - maskY);

    const float Y = dot(inColor.rgb, YVector);
    const float Cr = 0.7131 * (inColor.r - Y);
    const float Cb = 0.5647 * (inColor.b - Y);

    const float alpha = smoothstep(*threshold, *threshold + *smoothing, distance(float2(Cr, Cb), float2(maskCr, maskCb)));

    const float4 outColor = alpha * float4(inColor.r, inColor.g, inColor.b, 1.0);
    outTexture.write(outColor, gid);
}
