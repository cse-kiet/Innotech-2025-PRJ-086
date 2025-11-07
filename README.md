🩺 HealLens — AI-Powered Vital Signs Monitoring System

HealLens is an AI-based health monitoring system that uses computer vision and physiological signal processing to measure vital
health parameters like Heart Rate (HR), Heart Rate Variability (HRV), Stress Level, SpO₂, Respiratory Rate, and Blood Pressure (BP) 
— all through a simple face scan using a camera.
This project brings hospital-grade monitoring to your desktop or smartphone using remote photoplethysmography (rPPG)
and machine learning-based BP estimation.

🚀 Key Features
🔹 Real-Time Monitoring
    Detects Heart Rate directly from facial micro-color variations captured by a webcam.
    Provides live visualization with HR graph and metrics.
🔹 HRV Analysis
    Calculates advanced HRV metrics:
    SDNN (Standard Deviation of NN Intervals)
    RMSSD (Root Mean Square of Successive Differences)
    pNN50 (% of intervals differing >50ms)
    Used to assess stress levels and cardiac health.
🔹 SpO₂ & Respiratory Rate
    Estimates blood oxygen saturation and breathing rate using finger and facial signal fusion.
    Adaptive filtering ensures stable results even in ambient lighting.
🔹 Stress Level Estimation
    Classifies user’s condition into:
    😌 Relaxed
    😐 Moderate Stress
    😣 High Stress
    Based on HRV pattern and signal smoothness.
🔹 Blood Pressure Estimation (AI-based)
    Predicts Systolic & Diastolic BP using trained ML models (Random Forest) on open physiological datasets (PhysioNet MIMIC).
    Uses HR, SDNN, RMSSD, and pNN50 as predictive features.

🧠 Technical Stack
   Category- Tools / Libraries
   Programming Language	- Python 3.9
   Frontend (App) -	Kotlin / Android (for mobile integration)
   Web Framework (API) -	FastAPI
   Computer Vision -	OpenCV, MediaPipe
   Signal Processing -	SciPy, NumPy
   Data Visualization - Matplotlib, Pandas
   Machine Learning (BP Model)- Scikit-learn (RandomForestRegressor)
   Dataset -  PhysioNet MIMIC HRV–BP dataset

⚙️ How It Works
    User opens the app → camera activates for face or fingertip scan.
    rPPG signal extraction → system analyzes color variations in ROI.
    Filtering and FFT → HR and HRV metrics are computed.
    AI Model predicts BP based on physiological parameters.

🧩 Innovations
    Completely contactless vitals monitoring using only a webcam.
    AI-driven BP prediction (first of its kind in student-level projects).
    Combines Computer Vision + Signal Processing + ML.
    Generates a personalized health report automatically.
