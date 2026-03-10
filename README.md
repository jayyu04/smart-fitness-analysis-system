# Smart Fitness Analysis System

An AI-based fitness assistance system integrating pose estimation, EMG sensing, BLE communication, and Android app development.

## Project Overview
This project combines computer vision, muscle signal sensing, Bluetooth communication, and mobile application development to build a smart fitness analysis system.  
The system uses MediaPipe Pose for exercise motion recognition and repetition counting, while EMG signals are collected through an Arduino-based device for fatigue detection.

## Features
- Real-time exercise motion recognition
- Repetition counting based on body keypoint angles
- EMG-based muscle fatigue detection
- BLE communication between sensing device and Android app
- Real-time physiological signal visualization
- Exercise record storage and display

## Tech Stack
- Android / Java
- MediaPipe Pose
- Arduino
- BLE (HM-10)
- SQLite
- MPAndroidChart

## My Contributions
- Integrated Android app, MediaPipe pose estimation, Arduino sensing, and BLE communication
- Designed angle-based motion detection and repetition counting logic
- Implemented EMG signal collection and RMS-based fatigue detection
- Developed real-time BLE data receiving and chart visualization
- Completed system integration and testing

## Project Resources
- Project Slides: (docs/project-slides.pdf)
- Project Poster: (docs/project-poster.pdf)
- Technical Report: (docs/technical-report.pdf)
- Demo Video: (https://www.youtube.com/watch?v=ea8bsDLSvK8)

## System Architecture
The system consists of two main pipelines:

1. Camera input → MediaPipe Pose → motion recognition / repetition counting  
2. EMG sensor → Arduino → BLE transmission → Android app → real-time visualization / fatigue detection

## Outcome
This project demonstrates my practical experience in AI application development, sensor integration, BLE communication, Android development, and cross-system integration.
