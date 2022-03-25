#Documentation for adding Chroma Filter to Flutter Plugin 
 
##Structure 

The architecture of this code follows the double buffering requirement 
Three threads are required 

Main - The flutter app itself and calling plugin API and Camera class 
Camera - The camera callback thread where images are processed 
GLThread - The openGL thread where openGL operations are performed on textures

The Main thread sets up the processing and receives the final results 
The Camera thread processes images from the preview and prepares the input RGB texture used by openGL
The GLThread runs the GPU operations when data is available from the Camera thread then swaps buffers to the main thread/display

A separate pipeline is required to capture a still image and uses an additional handler to do this 
on its own background thread.

Refer to architecture diagram enclosed for an overview of how the objects in this package 
relate to these processes.
  
##Filters Ported from GPUImage

GPUImage cannot easily be extended outside its package due to access controls, private members 
preventing implementation of the desired overridden methods.

I've temporarily ported the filter code needed so that it can be prototyped, then removed and 
replaced with a fixed openGL pipeline using the required parts.

For the  chroma mod to the Flutter plugin a specific filter chain is always loaded and does not 
need to be dynamically updated with different filters in flight.

In order to run filters on a fixed pipeline, these were modified from GPUImage filters to remove
the flexible run queue. 

NB: The alternative to this would be to port these to a native code implementation such 
as https://github.com/ochornenko/Camera2GLPreview

###Requirements

- FixedBaseFilter - from GPUImageFilter, the base class for filters
- FixedFilterGroup  - for combining the Chromakey component filters
- FixedTwoInputFilter - the superclass of the chroma filter
- FixedChromaFilter - the desired two input, grouped filter

###For use in development. 

May be useful for subsequent image tuning
as well as development, eg: hue adjustment and edge detection (sampling).

- FixedHueFilter - A single input filter so that we can test rendering without FilterGroups 
- FixedSketchFilter - A group of filters not requiring two inputs for testing group rendering without an 
input file.
- FixedGrayscaleFilter - A part of the Sketch filter group 
- FixedTextureSampling3x3Filter - A part fo the Sketch filter group 





