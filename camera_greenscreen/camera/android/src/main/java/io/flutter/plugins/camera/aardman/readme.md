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
  
 