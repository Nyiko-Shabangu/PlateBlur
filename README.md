# License Plate Blur - Android Privacy Protection App

## Project Overview

License Plate Blur is an Android application designed to automatically detect and anonymize South African license plates in images, protecting personal privacy through intelligent image processing.

### Key Features

- 📱 **Image Source Flexibility**
  - Capture images directly using the device camera
  - Select images from the device gallery
  - Supports various image orientations

- 🔍 **Advanced License Plate Detection**
  - Utilizes Google ML Kit's Text Recognition
  - Supports multiple South African license plate formats
  - Robust regex-based detection strategies

- 🔒 **Automatic Plate Anonymization**
  - Automatically detects license plate regions
  - Applies blur effect to conceal sensitive information
  - Preserves original image quality and metadata

- 🛡️ **Privacy-First Design**
  - No images are shared or stored externally
  - Local processing ensures data privacy
  - Systematic image naming and storage

## Technical Implementation

### Detection Strategies
- Multiple regex patterns for comprehensive plate matching
- Character substitution to handle OCR variations
- Confidence-based detection mechanism

### Image Processing
- Handles image rotation and orientation
- Supports various image formats
- Preserves EXIF metadata

### Technologies Used
- Language: Kotlin
- ML Framework: Google ML Kit
- Image Processing: Android Bitmap API
- UI: Android XML, Material Design

## Installation

1. Clone the repository
2. Open in Android Studio
3. Build and run on an Android device

## Permissions Required
- Camera access
- Storage read/write permissions

## Screanshots


![Screenshot_20241217_233735_plateblur](https://github.com/user-attachments/assets/6a57a68c-39c5-4ae7-8e68-0c393a36859b)

![Screenshot_20241217_233745_plateblur](https://github.com/user-attachments/assets/c2c8de15-4a68-425b-9392-77bf1bb4fd3b)



## Future Improvements
- Support for international license plate formats
- Machine learning model training for improved accuracy
- Enhanced blur techniques
- User-configurable detection sensitivity

## License
MIT License
