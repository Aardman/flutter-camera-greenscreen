package io.flutter.plugins.camera;

import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.util.Range;
import android.util.Rational;
import android.util.Size;

import androidx.annotation.RequiresApi;

public class CameraPropertiesImpl implements CameraProperties {
    /**
     * Implementation of the @see CameraProperties interface using the @see
     * android.hardware.camera2.CameraCharacteristics class to access the different characteristics.
     */
        private final CameraCharacteristics cameraCharacteristics;
        private final String cameraName;

        public CameraPropertiesImpl(String cameraName, CameraManager cameraManager)
                throws CameraAccessException {
            this.cameraName = cameraName;
            this.cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraName);
        }

        @Override
        public String getCameraName() {
            return cameraName;
        }

        @Override
        public Range<Integer>[] getControlAutoExposureAvailableTargetFpsRanges() {
            return cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
        }

        @Override
        public Range<Integer> getControlAutoExposureCompensationRange() {
            return cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
        }

        @Override
        public double getControlAutoExposureCompensationStep() {
            Rational rational =
                    cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP);

            return rational == null ? 0.0 : rational.doubleValue();
        }

        @Override
        public int[] getControlAutoFocusAvailableModes() {
            return cameraCharacteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
        }

        @Override
        public Integer getControlMaxRegionsAutoExposure() {
            return cameraCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE);
        }

        @Override
        public Integer getControlMaxRegionsAutoFocus() {
            return cameraCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF);
        }

        @RequiresApi(api = Build.VERSION_CODES.P)
        @Override
        public int[] getDistortionCorrectionAvailableModes() {
            return cameraCharacteristics.get(CameraCharacteristics.DISTORTION_CORRECTION_AVAILABLE_MODES);
        }

        @Override
        public Boolean getFlashInfoAvailable() {
            return cameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
        }

        @Override
        public int getLensFacing() {
            return cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
        }

        @Override
        public Float getLensInfoMinimumFocusDistance() {
            return cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
        }

        @Override
        public Float getScalerAvailableMaxDigitalZoom() {
            return cameraCharacteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
        }

        @Override
        public Rect getSensorInfoActiveArraySize() {
            return cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        }

        @Override
        public Size getSensorInfoPixelArraySize() {
            return cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE);
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public Rect getSensorInfoPreCorrectionActiveArraySize() {
            return cameraCharacteristics.get(
                    CameraCharacteristics.SENSOR_INFO_PRE_CORRECTION_ACTIVE_ARRAY_SIZE);
        }

        @Override
        public int getSensorOrientation() {
            return cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        }

        @Override
        public int getHardwareLevel() {
            return cameraCharacteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        }

        @Override
        public int[] getAvailableNoiseReductionModes() {
            return cameraCharacteristics.get(
                    CameraCharacteristics.NOISE_REDUCTION_AVAILABLE_NOISE_REDUCTION_MODES);
        }
    }

