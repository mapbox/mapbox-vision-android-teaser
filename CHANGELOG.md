# Changelog

## v0.10.1

### Technical improvements

- Fixed bug with session not being recorded

### User experience

- Hotfix for v0.10.0 

## v0.10.0

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

## v0.9.0

- New AR Lane

## v0.8.0

- Start monitoring performance related device info

## v0.7.1

- Fixed detections of sign objects

## v0.7.0

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
