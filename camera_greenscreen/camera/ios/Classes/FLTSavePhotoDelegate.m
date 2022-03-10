// Copyright 2013 The Flutter Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#import "FLTSavePhotoDelegate.h"
#import "FLTSavePhotoDelegate_Test.h"
#import "import-plugin-swift.h"

@interface FLTSavePhotoDelegate ()
/// The file path for the captured photo.
@property(readonly, nonatomic) NSString *path;
/// The queue on which captured photos are written to disk.
@property(readonly, nonatomic) dispatch_queue_t ioQueue;
@property(weak, nonatomic) FilterPipeline * filterpipeline;
@end

@implementation FLTSavePhotoDelegate

- (instancetype)initWithPath:(NSString *)path
                     ioQueue:(dispatch_queue_t)ioQueue
           completionHandler:(FLTSavePhotoDelegateCompletionHandler)completionHandler {
  self = [super init];
  NSAssert(self, @"super init cannot be nil");
  _path = path;
  _ioQueue = ioQueue;
  _filterpipeline = nil;
  _completionHandler = completionHandler;
  return self;
}

- (void) setFilters:(FilterPipeline *) filterpipeline {
    _filterpipeline = filterpipeline;
}

- (void)handlePhotoCaptureResultWithError:(NSError *)error
                        photoDataProvider:(NSData * (^)(void))photoDataProvider {
  if (error) {
    self.completionHandler(nil, error);
    return;
  }
  dispatch_async(self.ioQueue, ^{
    NSData *data = photoDataProvider();
    NSError *ioError;
    if ([data writeToFile:self.path options:NSDataWritingAtomic error:&ioError]) {
      self.completionHandler(self.path, nil);
    } else {
      self.completionHandler(nil, ioError);
    }
  });
}

- (void)captureOutput:(AVCapturePhotoOutput *)output
    didFinishProcessingPhotoSampleBuffer:(CMSampleBufferRef)photoSampleBuffer
                previewPhotoSampleBuffer:(CMSampleBufferRef)previewPhotoSampleBuffer
                        resolvedSettings:(AVCaptureResolvedPhotoSettings *)resolvedSettings
                         bracketSettings:(AVCaptureBracketedStillImageSettings *)bracketSettings
                                   error:(NSError *)error API_AVAILABLE(ios(10)) {
  [self handlePhotoCaptureResultWithError:error
                        photoDataProvider:^NSData * {
                          return [AVCapturePhotoOutput
                              JPEGPhotoDataRepresentationForJPEGSampleBuffer:photoSampleBuffer
                                                    previewPhotoSampleBuffer:
                                                        previewPhotoSampleBuffer];
                        }];
}

- (void)captureOutput:(AVCapturePhotoOutput *)output
    didFinishProcessingPhoto:(AVCapturePhoto *)photo
                       error:(NSError *)error API_AVAILABLE(ios(11.0)) {
  [self handlePhotoCaptureResultWithError:error
                        photoDataProvider:^NSData * {
                        
//      if(self.filterpipeline){
//          NSData * photoData =  [self.filterpipeline filterAsPhoto:photo];
//          if (photoData) {
//              return photoData;
//          }
//      }
       
      //return unfiltered data if no-filtering is available
      //or if filtered data is nil
      return [photo fileDataRepresentation];
       
  }];
}

@end
