# Changelog

## 0.13.0

The latest version of the Vision Teaser brings automatic camera recalibration and improvements in lane detection. The full list of changes:

- Add Replay & Recording modes 
- Add reverse landscape orientation support
- Add lane visualization to Lane Detection mode
- Change camera default resolution to 960x540 if supported
- Improve lane detection
- Introduce automatic camera recalibration
- Stop sending some inaccurate events until the camera is calibrated
- Fix bug that prevented new China users authorization
- Fix bug with speed estimation when a vehicle has stopped
- Integrate new rendering engine for Object & Lane Detection, Segmentation & AR
- Expand Japan region to include Okinawa

## 0.12.0
This version of the Vision Teaser includes improvements in algorithm performance, camera calibration process, 
and lane detection accuracy. 
The new version contains new improved ML-models for CV tasks like detection, segmentation, and classification. 
The full list of changes:

- Added support for non-Snapdragon powered devices. Most chips on the market are supported now,
including Exynos by Samsung, Kirin by Huawei, Mediatek, etc.
- Added japanese traffic signs 
- Improved lanes detection algorithm
- Improved camera calibration speed
- Utilized new ML models that reduce resource consumption

## 0.10.1

### Technical improvements

- Fixed bug with session not being recorded

### User experience

- Hotfix for 0.10.0 

## 0.10.0

### Technical improvements

- Added new `recorder` module to build `Session Recorder` app
- Added new `common` module to share code between `app` and `recorder` modules
- `VisionView` renders with OpenGL ES
- Improved `VisionView` lifecycle management

### User experience

- Added Snapdragon 855 support
- Added detection of construction cones
- Improved quality of detection/segmentation, especially at night
- Improved segmentation, now it's more focused on road specific elements. New segmentation model recognizes the following classes: Crosswalk, Hood, MarkupDashed, MarkupDouble, MarkupOther, MarkupSolid, Other, Road, RoadEdge, Sidewalk
- Bug fixes

## 0.9.0

- New AR Lane

## 0.8.0

- Start monitoring performance related device info

## 0.7.1

- Fixed detections of sign objects

## 0.7.0

- Improved lane detection

## 0.6.0

- AR Lane drawing improved

## 0.5.0

- Fixed collision bound calculation in `SafetyModeView`
- Removed draw distance logic from `SafetyModeView`
- Added `clean()` method to `SafetyModeView`
- Fixed `FOREGROUND_SERVICE` permission check for devices with Android < 9
- `Vision` callbacks moved from `bg` to `UI`
- Missing `FOREGROUND_SERVICE` permission added
- Signs from `UK` and `Europe` are classified now
- Added classifier and assets for `UK`
