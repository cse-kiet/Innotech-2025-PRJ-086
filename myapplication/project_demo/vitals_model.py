# # vitals_model.py
# import cv2
# import mediapipe as mp
# import numpy as np
# from scipy.signal import butter, filtfilt, detrend, welch, find_peaks
# from scipy.ndimage import median_filter
# from collections import deque
# import time, csv, os

# # -------------------- FILTERS -------------------- #
# def bandpass_filter(signal, fs, low=0.7, high=3.0, order=4):
#     b, a = butter(order, [low/(0.5*fs), high/(0.5*fs)], btype='band')
#     return filtfilt(b, a, detrend(signal))

# def lowpass_filter(signal, fs, cutoff=0.4, order=2):
#     b, a = butter(order, cutoff/(0.5*fs), btype='low')
#     return filtfilt(b, a, signal)


# # -------------------- HR + HRV -------------------- #
# def calculate_hr(peaks, times):
#     if len(peaks) > 1:
#         rr_intervals = np.diff([times[p] for p in range(len(peaks))]) * 1000  # ms
#         rr_intervals = rr_intervals[(rr_intervals > 400) & (rr_intervals < 1200)]  # 50-150 BPM
#         rr_intervals = median_filter(rr_intervals, size=3)
#         hr = 60000.0 / np.mean(rr_intervals)
#         return hr, rr_intervals
#     return 0, []

# def calculate_hrv(rr):
#     if len(rr) < 2: return 0,0,0
#     sdnn = np.std(rr)
#     rmssd = np.sqrt(np.mean(np.diff(rr)**2))
#     pnn50 = np.sum(np.abs(np.diff(rr)) > 50) / len(rr) * 100
#     return sdnn, rmssd, pnn50

# # -------------------- SpO2 -------------------- #
# def calculate_spo2(red_signal, blue_signal):
#     red_dc = np.mean(red_signal)
#     blue_dc = np.mean(blue_signal)
#     red_ac = red_signal - red_dc
#     blue_ac = blue_signal - blue_dc
#     if blue_dc == 0 or np.std(blue_ac) == 0: return 0
#     R = (np.std(red_ac)/red_dc) / (np.std(blue_ac)/blue_dc)
#     spo2 = 102 - 20 * R
#     return np.clip(spo2, 95, 100)


# # -------------------- Respiratory Rate -------------------- #
# def calculate_resp_rate(signal, fs):
#     filtered = lowpass_filter(signal, fs, cutoff=0.4)
#     freqs, psd = welch(filtered, fs, nperseg=min(256, len(filtered)))
#     resp_band = (freqs >= 0.1) & (freqs <= 0.4)
#     if np.any(resp_band):
#         return float(freqs[resp_band][np.argmax(psd[resp_band])] * 60)
#     return 0

# # -------------------- Face ROI -------------------- #
# def get_face_roi_avg(frame, landmarks):
#     h, w, _ = frame.shape
#     regions = [(10, 338), (234, 454), (93, 323)]  # forehead + cheeks
#     rgb_vals = []
#     for lm1, lm2 in regions:
#         y1, x1 = int(landmarks.landmark[lm1].y*h), int(landmarks.landmark[lm1].x*w)
#         y2, x2 = int(landmarks.landmark[lm2].y*h), int(landmarks.landmark[lm2].x*w)
#         y_top, y_bottom = max(0,min(y1,y2)-20), min(h,max(y1,y2)+20)
#         x_left, x_right = max(0,min(x1,x2)-20), min(w,max(x1,x2)+20)
#         roi = frame[y_top:y_bottom, x_left:x_right]
#         if roi.size>0:
#             rgb_vals.append(np.mean(roi.reshape(-1,3), axis=0))
#     return np.mean(rgb_vals,axis=0) if rgb_vals else np.array([0,0,0])

# # -------------------- Video-processing wrapper (keeps logic same) -------------------- #

# import subprocess, shlex, tempfile
# from scipy.ndimage import uniform_filter1d

# def reencode_to_30fps(src_path):
#     """Re-encode input to 30 fps and ~640px width. Returns path to temp file."""
#     dst = tempfile.NamedTemporaryFile(delete=False, suffix=".mp4")
#     dst.close()
#     cmd = f'ffmpeg -y -i "{src_path}" -r 30 -vf scale=640:-2 -c:v libx264 -preset veryfast -crf 23 "{dst.name}"'
#     try:
#         print(f"[INFO] Re-encoding {src_path} → 30 FPS, 640px width")

#         subprocess.run(shlex.split(cmd), stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=True, timeout=60)
#         return dst.name
#     except Exception:
#         # If ffmpeg not available or fails, remove temp and return None
#         try:
#             os.unlink(dst.name)
#         except:
#             pass
#         return None

# def normalize_frame_brightness(frame):
#     """Use CLAHE + gamma correction to stabilize lighting variations."""
#     try:
#         lab = cv2.cvtColor(frame, cv2.COLOR_BGR2LAB)
#         l, a, b = cv2.split(lab)
#         clahe = cv2.createCLAHE(clipLimit=3.0, tileGridSize=(8,8))
#         l2 = clahe.apply(l)
#         lab2 = cv2.merge((l2, a, b))
#         enhanced = cv2.cvtColor(lab2, cv2.COLOR_LAB2BGR)
        
#         # ✅ Gamma correction for brightness normalization
#         gamma = 1.2
#         invGamma = 1.0 / gamma
#         table = np.array([(i / 255.0) ** invGamma * 255 for i in np.arange(0, 256)]).astype("uint8")
#         return cv2.LUT(enhanced, table)
#     except Exception as e:
#         print(f"[WARN] Brightness normalization failed: {e}")
#         return frame


# def process_video_file(video_path, capture_seconds=60, reencode=True, target_fps=30.0):
#     """
#     Robust processing for uploaded videos (phone recordings).
#     Steps:
#      - optional ffmpeg re-encode to constant FPS (+ resize)
#      - read frames (up to capture_seconds)
#      - normalize frames (CLAHE)
#      - extract ROI rgb averages using face mesh
#      - create fixed timestamps = np.arange(n)/fs
#      - baseline removal, detrend, median filter, low-pass
#      - peak detection + HR/HRV + SpO2 + RespRate
#     Returns same dict as before or None if insufficient data.
#     """
#     # initialize face mesh
#     mp_face = mp.solutions.face_mesh
#     face_mesh = mp_face.FaceMesh(static_image_mode=False, max_num_faces=1, refine_landmarks=True)

#     temp_encoded = None
#     use_path = video_path

#     # 1) optional re-encode to fixed FPS for more stable timing
#     if reencode:
#         try:
#             enc = reencode_to_30fps(video_path)
#             if enc:
#                 temp_encoded = enc
#                 use_path = temp_encoded
#                 fs = float(target_fps)
#             else:
#                 # ffmpeg not available: we'll try to read metadata but still enforce fs later
#                 cap_tmp = cv2.VideoCapture(video_path)
#                 fs = cap_tmp.get(cv2.CAP_PROP_FPS)
#                 cap_tmp.release()
#                 if fs == 0 or np.isnan(fs) or fs is None:
#                     fs = float(target_fps)
#         except Exception:
#             fs = float(target_fps)
#     else:
#         cap_tmp = cv2.VideoCapture(video_path)
#         fs = cap_tmp.get(cv2.CAP_PROP_FPS)
#         cap_tmp.release()
#         if fs == 0 or fs is None or np.isnan(fs):
#             print("[WARN] FPS not detected, forcing 30 FPS")
#             fs = float(target_fps)
#         else:
#              # Normalize to nearest common frame rate
#             fs = 30.0 if abs(fs - 30) < 3 else fs
#             print(f"[INFO] Using FPS: {fs}")


#     # Ensure fs is a reasonable float
#     try:
#         fs = float(fs)
#     except:
#         fs = float(target_fps)

#     # 2) read frames
#     cap = cv2.VideoCapture(use_path)
#     frames = []
#     frame_times = []  # we will ignore system times and use fixed timestamps later

#     start = time.time()
#     while True:
#         ret, frame = cap.read()
#         if not ret:
#             break
#         frames.append(frame)
#         # stop if we've collected enough seconds (use count / fs)
#         if (len(frames) / fs) >= capture_seconds:
#             break
#     cap.release()

#     # cleanup temporary encoded file
#     if temp_encoded:
#         try:
#             os.unlink(temp_encoded)
#         except:
#             pass

#     if len(frames) < int(fs * 3):  # need at least ~3 seconds of face data
#         return None

#     # 3) process frames: normalize + extract face ROI averages
#     rgbs = []
#     red_buf = []
#     blue_buf = []

#     for f in frames:
#         # normalize brightness to reduce AE/AWB flicker effects
#         f_norm = normalize_frame_brightness(f)

#         # mediapipe expects RGB images
#         try:
#             rgb_frame = cv2.cvtColor(f_norm, cv2.COLOR_BGR2RGB)
#         except:
#             rgb_frame = f_norm

#         results = face_mesh.process(rgb_frame)
#         if results is None or not results.multi_face_landmarks:
#             # append zeros placeholder so indices match timestamps (but avoid long gaps)
#             rgbs.append(np.array([0.0, 0.0, 0.0]))
#         else:
#             landmarks = results.multi_face_landmarks[0]
#             rgb_avg = get_face_roi_avg(f_norm, landmarks)  # returns BGR mean as earlier
#             rgbs.append(rgb_avg)
#         # finger ROI (if present) - keep same location for consistency
#         h, w, _ = f.shape
#         box_size = 80
#         x1, y1 = 30, h-30-box_size
#         x2, y2 = x1+box_size, y1+box_size
#         roi_finger = f[y1:y2, x1:x2]
#         if roi_finger is not None and roi_finger.size>0:
#             red_buf.append(np.mean(roi_finger[:,:,2]))
#             blue_buf.append(np.mean(roi_finger[:,:,0]))

#     # Convert to numpy arrays
#     rgbs = np.array(rgbs)  # shape (n,3)
#     red_arr = np.array(red_buf) if len(red_buf)>0 else np.array([])
#     blue_arr = np.array(blue_buf) if len(blue_buf)>0 else np.array([])

#     # 4) build fixed timestamps and green signal
#     n = len(rgbs)
#     timestamps = np.arange(n) / fs  # seconds, uniform spacing

#     # Use green channel (index 1). Some frames may be zeros (no face detected). We'll mask them out
#     green = rgbs[:,1].astype(float)
#     valid_mask = green != 0
#     if np.sum(valid_mask) < int(fs * 3):  # require at least ~3 seconds of valid face data
#         return None

#     # For frames with missing face detection, interpolate linearly to keep continuity
#     if not np.all(valid_mask):
#         # simple linear interpolation over missing values
#         valid_idx = np.where(valid_mask)[0]
#         invalid_idx = np.where(~valid_mask)[0]
#         if len(valid_idx) >= 2:
#             green = np.interp(np.arange(n), valid_idx, green[valid_idx])
#         else:
#             # fallback: fill with nearest valid or zeros
#             if len(valid_idx) == 1:
#                 green[:] = green[valid_idx[0]]
#             else:
#                 return None

#     # 5) baseline removal and smoothing
#     # smooth baseline with 1.5s window
#     baseline_window = max(1, int(fs * 1.5))
#     baseline = uniform_filter1d(green, size=baseline_window, mode='nearest')
#     g_corr = green - baseline

#     # detrend and median-filter
#     g_detr = detrend(g_corr)
#     g_med = median_filter(g_detr, size=3)

#     # low-pass filter to remove very high freq noise (cutoff 6 Hz)
#     try:
#         b, a = butter(2, 6.0/(0.5*fs), btype='low')
#         g_smooth = filtfilt(b, a, g_med)
#     except Exception:
#         g_smooth = g_med

#     # 6) Peak detection (distance ensures at most 150 BPM)
#     min_distance = int(max(1, fs * 0.4))  # 0.4s -> 150 BPM
#     peaks, _ = find_peaks(g_smooth, distance=min_distance)

#     # If too few peaks, try relaxing distance / use prominence
#     if len(peaks) < 2:
#         peaks, _ = find_peaks(g_smooth, distance=max(1,int(fs*0.3)), prominence=np.std(g_smooth)*0.5)

#     if len(peaks) < 2:
#         return None

#     # Convert peak indices to times using uniform timestamps
#     peak_times = timestamps[peaks]

#     # 7) HR and HRV (same logic but using peak_times)
#     hr_bpm, rr_intervals = calculate_hr(peaks, peak_times)  # calculate_hr expects (peaks, times) as original
#     sdnn, rmssd, pnn50 = calculate_hrv(rr_intervals)
#     stress_level = "Relaxed" if rmssd > 20 else "High Stress"

#     # 8) SpO2 from red/blue buffers (if available)
#     spo2 = 0.0
#     if red_arr.size > 0 and blue_arr.size > 0 and len(red_arr) >= 3 and len(blue_arr) >= 3:
#         # Resample red/blue to same length as frames if needed (simple interpolation)
#         if len(red_arr) != n:
#             try:
#                 red_arr = np.interp(np.linspace(0, len(red_arr)-1, n), np.arange(len(red_arr)), red_arr)
#                 blue_arr = np.interp(np.linspace(0, len(blue_arr)-1, n), np.arange(len(blue_arr)), blue_arr)
#             except:
#                 pass
#         spo2 = calculate_spo2(red_arr, blue_arr)
#     else:
#         spo2 = 0.0  # not available reliably

#     # 9) Resp rate from filtered green signal
#     resp_bpm = calculate_resp_rate(g_smooth, fs)
#     resp_bpm = float(np.clip(resp_bpm, 8, 30))

#     # final safety checks
#     try:
#         hr_bpm = float(np.clip(hr_bpm, 30.0, 220.0))
#     except:
#         hr_bpm = 0.0

#     # return same structure as before
#     print(f"[RESULT] HR: {hr_bpm:.1f} BPM, SpO₂: {spo2:.1f}%, Resp: {resp_bpm:.1f}, Stress: {stress_level}")

#     return {
#         "heart_rate": float(hr_bpm),
#         "sdnn": float(sdnn),
#         "rmssd": float(rmssd),
#         "pnn50": float(pnn50),
#         "stress_level": stress_level,
#         "spo2": float(spo2),
#         "resp_rate": float(resp_bpm)
#     }





# def process_video_file(video_path, capture_seconds=60):
#     """
#     Runs your existing pipeline on frames read from video_path (mp4/webm).
#     capture_seconds: how many seconds to process (if video shorter, will use all frames)
#     Returns a dict of vitals (heart_rate, sdnn, rmssd, pnn50, stress_level, spo2, resp_rate)
#     """
#     # Initialize face mesh just like original
#     mp_face = mp.solutions.face_mesh
#     face_mesh = mp_face.FaceMesh(static_image_mode=False, max_num_faces=1, refine_landmarks=True)

#     cap = cv2.VideoCapture(video_path)
#     fs = cap.get(cv2.CAP_PROP_FPS)
#     if fs == 0 or np.isnan(fs) or fs is None:
#         fs = 30.0
#     # Buffers (same sizes as original)
#     hr_buffer = deque(maxlen=int(fs*60))
#     red_buffer = deque(maxlen=int(fs*60))
#     blue_buffer = deque(maxlen=int(fs*60))
#     timestamps = deque(maxlen=int(fs*60))

#     start_time = time.time()
#     prev_bpm = 0.0

#     # Read frames for up to capture_seconds or until video ends
#     while True:
#         ret, frame = cap.read()
#         if not ret:
#             break
#         # convert as in original
#         frame_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
#         results = face_mesh.process(frame_rgb)

#         # finger ROI - in phone upload scenario we may not have fingertip; keep logic but do not rely on it
#         h, w, _ = frame.shape
#         # mimic same finger box location; if user includes finger in video, this will collect red/blue
#         box_size = 80
#         x1, y1 = 30, h-30-box_size
#         x2, y2 = x1+box_size, y1+box_size
#         roi_finger = frame[y1:y2, x1:x2]
#         if roi_finger.size>0:
#             mean_red = np.mean(roi_finger[:,:,2])
#             mean_blue = np.mean(roi_finger[:,:,0])
#             red_buffer.append(mean_red)
#             blue_buffer.append(mean_blue)

#         # face ROI
#         if results.multi_face_landmarks:
#             landmarks = results.multi_face_landmarks[0]
#             rgb_avg = get_face_roi_avg(frame, landmarks)
#             hr_buffer.append(rgb_avg[1])  # green channel
#             timestamps.append(time.time() - start_time)

#         # stop if we've run for requested seconds
#         if (time.time() - start_time) > capture_seconds:
#             break

#     cap.release()

#     # ---------- POST PROCESSING (exact same calculations) ----------
#     hr_signal = np.array(hr_buffer)
#     timestamps_arr = np.array(timestamps)
#     if len(hr_signal) < 2 or len(timestamps_arr) < 2:
#         return None  # not enough data

#     filtered_hr = bandpass_filter(hr_signal, fs)
#     peaks, _ = find_peaks(filtered_hr, distance=fs*0.5)
#     peak_times = timestamps_arr[peaks[peaks < len(timestamps_arr)]] if len(peaks) > 0 else timestamps_arr
#     hr_bpm, rr_intervals = calculate_hr(peaks, peak_times)
#     sdnn, rmssd, pnn50 = calculate_hrv(rr_intervals)
#     stress_level = "Relaxed" if rmssd > 20 else "High Stress"
#     spo2 = calculate_spo2(np.array(red_buffer), np.array(blue_buffer))
#     resp_bpm = calculate_resp_rate(filtered_hr, fs)
#     resp_bpm = float(np.clip(resp_bpm, 12, 20))

#     # return same fields as your script
#     return {
#         "heart_rate": float(hr_bpm),
#         "sdnn": float(sdnn),
#         "rmssd": float(rmssd),
#         "pnn50": float(pnn50),
#         "stress_level": stress_level,
#         "spo2": float(spo2),
#         "resp_rate": float(resp_bpm)
#     }

# import cv2
# import mediapipe as mp
# import numpy as np
# from scipy.signal import butter, filtfilt, detrend, welch, find_peaks
# from scipy.ndimage import median_filter, uniform_filter1d
# from collections import deque
# import time, os, tempfile, subprocess, shlex

# # -------------------- FILTERS -------------------- #
# def bandpass_filter(signal, fs, low=0.7, high=3.0, order=4):
#     b, a = butter(order, [low/(0.5*fs), high/(0.5*fs)], btype='band')
#     return filtfilt(b, a, detrend(signal))

# def lowpass_filter(signal, fs, cutoff=0.6, order=1): 
#     b, a = butter(order, cutoff/(0.5*fs), btype='low')
#     return filtfilt(b, a, signal)

# # -------------------- HR + HRV -------------------- #
# def calculate_hr(peaks, times):
#     if len(peaks) > 1:
#         rr_intervals = np.diff(times[peaks]) * 1000  # ms
#         rr_intervals = rr_intervals[(rr_intervals > 400) & (rr_intervals < 1200)]  # 50–150 BPM range
#         if len(rr_intervals) == 0:
#             return 0, []
#         rr_intervals = median_filter(rr_intervals, size=3)
#         hr = 60000.0 / np.mean(rr_intervals)
#         return hr, rr_intervals
#     return 0, []

# def calculate_hrv(rr):
#     if len(rr) < 2:
#         return 0, 0, 0
#     sdnn = np.std(rr)
#     rmssd = np.sqrt(np.mean(np.diff(rr)**2))
#     pnn50 = np.sum(np.abs(np.diff(rr)) > 50) / len(rr) * 100
#     return sdnn, rmssd, pnn50

# # -------------------- SpO₂ -------------------- #
# def calculate_spo2(red_signal, blue_signal):
#     red_dc = np.mean(red_signal)
#     blue_dc = np.mean(blue_signal)
#     red_ac = red_signal - red_dc
#     blue_ac = blue_signal - blue_dc
#     if blue_dc == 0 or np.std(blue_ac) == 0:
#         return 0
#     R = (np.std(red_ac)/red_dc) / (np.std(blue_ac)/blue_dc)
#     spo2 = 102 - 20 * R
#     return np.clip(spo2, 90, 100)

# # -------------------- Respiration Rate -------------------- #
# # def calculate_resp_rate(signal, fs):
# #     filtered = lowpass_filter(signal, fs, cutoff=0.4)
# #     freqs, psd = welch(filtered, fs, nperseg=min(256, len(filtered)))
# #     resp_band = (freqs >= 0.1) & (freqs <= 0.4)
# #     if np.any(resp_band):
# #         return float(freqs[resp_band][np.argmax(psd[resp_band])] * 60)
# #     return 0
# def calculate_resp_rate(signal, fps):
#     import numpy as np
#     from scipy.signal import butter, filtfilt, welch

#     # Normalize
#     signal = (signal - np.mean(signal)) / np.std(signal)

#     # ---- Bandpass filter (0.08 - 0.4 Hz ≈ 5–24 breaths/min) ----
#     lowcut = 0.08
#     highcut = 0.4
#     nyq = 0.5 * fps
#     b, a = butter(2, [lowcut / nyq, highcut / nyq], btype='band')
#     filtered = filtfilt(b, a, signal)

#     # ---- Power Spectral Density ----
#     f, Pxx = welch(filtered, fs=fps, nperseg=min(256, len(filtered)))
#     valid = (f >= lowcut) & (f <= highcut)

#     # Peak frequency
#     f_peak = f[valid][np.argmax(Pxx[valid])]
#     resp_rate = f_peak * 60  # convert Hz -> BPM

#     return round(resp_rate, 1)



# # -------------------- Face ROI -------------------- #
# def get_face_roi_avg(frame, landmarks):
#     h, w, _ = frame.shape
#     regions = [(10, 338), (234, 454), (93, 323)]  # forehead + cheeks
#     rgb_vals = []
#     for lm1, lm2 in regions:
#         y1, x1 = int(landmarks.landmark[lm1].y*h), int(landmarks.landmark[lm1].x*w)
#         y2, x2 = int(landmarks.landmark[lm2].y*h), int(landmarks.landmark[lm2].x*w)
#         y_top, y_bottom = max(0, min(y1, y2)-20), min(h, max(y1, y2)+20)
#         x_left, x_right = max(0, min(x1, x2)-20), min(w, max(x1, x2)+20)
#         roi = frame[y_top:y_bottom, x_left:x_right]
#         if roi.size > 0:
#             rgb_vals.append(np.mean(roi.reshape(-1, 3), axis=0))
#     return np.mean(rgb_vals, axis=0) if rgb_vals else np.array([0, 0, 0])

# # -------------------- Brightness Normalization -------------------- #
# def normalize_frame_brightness(frame):
#     """Stabilize lighting using CLAHE + gamma correction."""
#     lab = cv2.cvtColor(frame, cv2.COLOR_BGR2LAB)
#     l, a, b = cv2.split(lab)
#     clahe = cv2.createCLAHE(clipLimit=3.0, tileGridSize=(8, 8))
#     l2 = clahe.apply(l)
#     lab2 = cv2.merge((l2, a, b))
#     enhanced = cv2.cvtColor(lab2, cv2.COLOR_LAB2BGR)
#     gamma = 1.1
#     invGamma = 1.0 / gamma
#     table = np.array([(i / 255.0) ** invGamma * 255 for i in np.arange(0, 256)]).astype("uint8")
#     return cv2.LUT(enhanced, table)

# # -------------------- Re-encode Helper -------------------- #
# def reencode_to_fixed_fps(src_path, fps=30):
#     """Ensures constant FPS (phone cameras vary)."""
#     dst = tempfile.NamedTemporaryFile(delete=False, suffix=".mp4")
#     dst.close()
#     cmd = f'ffmpeg -y -i "{src_path}" -r {fps} -vf scale=640:-2 -c:v libx264 -preset veryfast -crf 23 "{dst.name}"'
#     try:
#         subprocess.run(shlex.split(cmd), stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=True)
#         return dst.name
#     except:
#         try: os.unlink(dst.name)
#         except: pass
#         return src_path

# # -------------------- MAIN PIPELINE -------------------- #
# def process_video_file(video_path, capture_seconds=60, reencode=True, target_fps=30.0):
#     mp_face = mp.solutions.face_mesh
#     face_mesh = mp_face.FaceMesh(static_image_mode=False, max_num_faces=1, refine_landmarks=True)

#     # Step 1: Fix FPS (critical for phone vs webcam consistency)
#     if reencode:
#         video_path = reencode_to_fixed_fps(video_path, fps=target_fps)
#     fs = target_fps

#     cap = cv2.VideoCapture(video_path)
#     frames, rgbs, red_buf, blue_buf = [], [], [], []

#     while True:
#         ret, frame = cap.read()
#         if not ret:
#             break
#         frames.append(frame)
#         if len(frames) / fs >= capture_seconds:
#             break
#     cap.release()

#     if len(frames) < int(fs * 3):
#         return None

#     for f in frames:
#         f_norm = normalize_frame_brightness(f)
#         rgb_frame = cv2.cvtColor(f_norm, cv2.COLOR_BGR2RGB)
#         results = face_mesh.process(rgb_frame)

#         if results.multi_face_landmarks:
#             rgb_avg = get_face_roi_avg(f_norm, results.multi_face_landmarks[0])
#         else:
#             rgb_avg = np.array([0, 0, 0])
#         rgbs.append(rgb_avg)

#         h, w, _ = f.shape
#         box_size = 80
#         roi = f[h-box_size-30:h-30, 30:30+box_size]
#         if roi.size > 0:
#             red_buf.append(np.mean(roi[:, :, 2]))
#             blue_buf.append(np.mean(roi[:, :, 0]))

#     rgbs = np.array(rgbs)
#     n = len(rgbs)
#     timestamps = np.arange(n) / fs
#     green = rgbs[:, 1]

#     # Fill missing frames (linear interpolation)
#     mask = green != 0
#     if np.sum(mask) < int(fs * 3):
#         return None
#     if not np.all(mask):
#         green = np.interp(np.arange(n), np.where(mask)[0], green[mask])

#     # Normalize + detrend
#     baseline = uniform_filter1d(green, size=int(fs*1.5))
#     g_corr = green - baseline
#     g_detr = detrend(g_corr)
#     g_filt = bandpass_filter(g_detr, fs, low=0.7, high=3.0)
#     g_filt = median_filter(g_filt, size=3)

#     # Peak detection (heart beats)
#     peaks, _ = find_peaks(g_filt, distance=int(fs*0.5))
#     if len(peaks) < 2:
#         peaks, _ = find_peaks(g_filt, distance=int(fs*0.3), prominence=np.std(g_filt)*0.5)
#     if len(peaks) < 2:
#         return None

#     hr_bpm, rr = calculate_hr(peaks, timestamps)
#     sdnn, rmssd, pnn50 = calculate_hrv(rr)
#     stress = "Relaxed" if rmssd > 20 else "High Stress"

#     spo2 = calculate_spo2(np.array(red_buf), np.array(blue_buf)) if len(red_buf) > 5 else 0.0
#     resp_bpm = calculate_resp_rate(g_filt, fs)
#     # resp_bpm = float(np.clip(resp_bpm, 8, 30))
#     resp_bpm = float(np.clip(resp_bpm, 10, 30))


#     hr_bpm = float(np.clip(hr_bpm, 30, 220))

#     print(f"[RESULT] HR={hr_bpm:.1f} BPM | SpO₂={spo2:.1f}% | Resp={resp_bpm:.1f} | Stress={stress}")
#     return {
#         "heart_rate": hr_bpm,
#         "sdnn": float(sdnn),
#         "rmssd": float(rmssd),
#         "pnn50": float(pnn50),
#         "stress_level": stress,
#         "spo2": float(spo2),
#         "resp_rate": float(resp_bpm)
#     }



import cv2
import mediapipe as mp
import numpy as np
from scipy.signal import butter, filtfilt, detrend, welch, find_peaks
from scipy.ndimage import median_filter, uniform_filter1d
from collections import deque
import time, os, tempfile, subprocess, shlex
import joblib
import pandas as pd
import numpy as np

# -------------------- LOAD BP MODELS -------------------- #
try:
    rf_sys = joblib.load("systolic_model.pkl")
    rf_dia = joblib.load("diastolic_model.pkl")
    MODELS_AVAILABLE = True
except Exception as e:
    print("⚠️ Warning: Could not load BP models:", e)
    rf_sys = rf_dia = None
    MODELS_AVAILABLE = False

# -------------------- FILTERS -------------------- #
def bandpass_filter(signal, fs, low=0.7, high=3.0, order=4):
    """Keeps only heart-related frequency (0.7–3.0 Hz = 42–180 BPM)."""
    b, a = butter(order, [low / (0.5 * fs), high / (0.5 * fs)], btype='band')
    return filtfilt(b, a, detrend(signal))

def lowpass_filter(signal, fs, cutoff=0.4, order=2):
    """Used for respiration estimation."""
    b, a = butter(order, cutoff / (0.5 * fs), btype='low')
    return filtfilt(b, a, signal)

# -------------------- HR + HRV -------------------- #
def calculate_hr(peaks, times):
    """Calculates heart rate and RR intervals."""
    if len(peaks) > 1:
        rr_intervals = np.diff(times[peaks]) * 1000  # ms
        rr_intervals = rr_intervals[(rr_intervals > 400) & (rr_intervals < 1200)]  # 50–150 BPM
        # if len(rr_intervals) == 0:
        #     return 0, []
        rr_intervals = median_filter(rr_intervals, size=3)
        hr = 60000.0 / np.mean(rr_intervals)
        return hr, rr_intervals
    return 0, []

def calculate_hrv(rr):
    """Calculates HRV metrics (SDNN, RMSSD, pNN50)."""
    if len(rr) < 2:
        return 0, 0, 0
    sdnn = np.std(rr)
    rmssd = np.sqrt(np.mean(np.diff(rr)**2))
    pnn50 = np.sum(np.abs(np.diff(rr)) > 50) / len(rr) * 100
    return sdnn, rmssd, pnn50

# -------------------- SpO₂ -------------------- #
def calculate_spo2(red_signal, blue_signal):
    """Estimates SpO₂ using ratio of ratios."""
    red_dc = np.mean(red_signal)
    blue_dc = np.mean(blue_signal)
    red_ac = red_signal - red_dc
    blue_ac = blue_signal - blue_dc
    if blue_dc == 0 or np.std(blue_ac) == 0:
        return 0
    R = (np.std(red_ac) / red_dc) / (np.std(blue_ac) / blue_dc)
    spo2 = 102 - 20 * R
    return np.clip(spo2, 95, 100)

# -------------------- Respiration Rate -------------------- #
# def calculate_resp_rate(signal, fs):
#     """Estimates respiration rate from slow intensity oscillations."""
#     signal = (signal - np.mean(signal)) / np.std(signal)
#     # Bandpass 0.08–0.4 Hz (≈ 5–24 breaths/min)
#     lowcut, highcut = 0.08, 0.4
#     nyq = 0.5 * fs
#     b, a = butter(2, [lowcut / nyq, highcut / nyq], btype='band')
#     filtered = filtfilt(b, a, signal)

#     f, Pxx = welch(filtered, fs=fs, nperseg=min(256, len(filtered)))
#     valid = (f >= lowcut) & (f <= highcut)
#     if not np.any(valid):
#         return 0
#     f_peak = f[valid][np.argmax(Pxx[valid])]
#     resp_rate = f_peak * 60  # convert Hz -> breaths/min
#     return float(np.clip(resp_rate, 10, 25))
# def calculate_resp_rate(signal, fs):
#     filtered = lowpass_filter(signal, fs, cutoff=0.4)
#     freqs, psd = welch(filtered, fs, nperseg=min(256, len(filtered)))
#     resp_band = (freqs >= 0.1) & (freqs <= 0.4)
#     if np.any(resp_band):
#         return freqs[resp_band][np.argmax(psd[resp_band])] * 60
#     return 0
def calculate_resp_rate(signal, fs):
    filtered = lowpass_filter(signal, fs, cutoff=0.4)
    freqs, psd = welch(filtered, fs, nperseg=min(256, len(filtered)))
    resp_band = (freqs >= 0.1) & (freqs <= 0.4)
    if np.any(resp_band):
        return freqs[resp_band][np.argmax(psd[resp_band])] * 60
    return 0


    # signal = (signal - np.mean(signal)) / np.std(signal)
    # b, a = butter(2, [0.08 / (0.5 * fs), 0.4 / (0.5 * fs)], btype='band')
    # filtered = filtfilt(b, a, signal)
    # freqs, psd = welch(filtered, fs, nperseg=min(256, len(filtered)))
    # valid = (freqs >= 0.08) & (freqs <= 0.4)
    # if not np.any(valid):
    #     return 0
    # f_peak = freqs[valid][np.argmax(psd[valid])]
    # return float(np.clip(f_peak * 60, 8, 24))


# -------------------- Face ROI -------------------- #
def get_face_roi_avg(frame, landmarks):
    """Extracts RGB mean from forehead + cheek regions."""
    h, w, _ = frame.shape
    regions = [(10, 338), (234, 454), (93, 323)]
    rgb_vals = []
    for lm1, lm2 in regions:
        y1, x1 = int(landmarks.landmark[lm1].y*h), int(landmarks.landmark[lm1].x*w)
        y2, x2 = int(landmarks.landmark[lm2].y*h), int(landmarks.landmark[lm2].x*w)
        y_top, y_bottom = max(0, min(y1, y2)-20), min(h, max(y1, y2)+20)
        x_left, x_right = max(0, min(x1, x2)-20), min(w, max(x1, x2)+20)
        roi = frame[y_top:y_bottom, x_left:x_right]
        if roi.size > 0:
            rgb_vals.append(np.mean(roi.reshape(-1, 3), axis=0))
    return np.mean(rgb_vals, axis=0) if rgb_vals else np.array([0, 0, 0])

# -------------------- Brightness Normalization -------------------- #

def normalize_frame_brightness(frame):
    # Convert to LAB color space
    lab = cv2.cvtColor(frame, cv2.COLOR_BGR2LAB)
    l, a, b = cv2.split(lab)

    # CLAHE (mild contrast normalization)
    clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8, 8))
    cl = clahe.apply(l)

    # Merge and convert back
    limg = cv2.merge((cl, a, b))
    final = cv2.cvtColor(limg, cv2.COLOR_LAB2BGR)

    # Gamma correction (slightly below 1 → preserve slow variation)
    gamma = 0.95
    invGamma = 1.0 / gamma
    table = np.array([(i / 255.0) ** invGamma * 255 for i in np.arange(256)]).astype("uint8")
    final = cv2.LUT(final, table)

    # Light blend: only apply 80% normalization → 20% raw frame retained
    blended = cv2.addWeighted(final, 0.6, frame, 0.4, 0)


    return blended


# def normalize_frame_brightness(frame):
    # """Normalizes lighting using CLAHE + gentle gamma correction."""
    # lab = cv2.cvtColor(frame, cv2.COLOR_BGR2LAB)
    # l, a, b = cv2.split(lab)
    # clahe = cv2.createCLAHE(clipLimit=3.0, tileGridSize=(8, 8))
    # l2 = clahe.apply(l)
    # lab2 = cv2.merge((l2, a, b))
    # enhanced = cv2.cvtColor(lab2, cv2.COLOR_LAB2BGR)

    # gamma = 1.1
    # invGamma = 1.0 / gamma
    # table = np.array([(i / 255.0) ** invGamma * 255 for i in np.arange(0, 256)]).astype("uint8")
    # return cv2.LUT(enhanced, table)

# -------------------- Re-encode Helper -------------------- #
def reencode_to_fixed_fps(src_path, fps=30):
    """Ensures constant FPS (especially for phone recordings)."""
    dst = tempfile.NamedTemporaryFile(delete=False, suffix=".mp4")
    dst.close()
    cmd = f'ffmpeg -y -i "{src_path}" -r {fps} -vf scale=640:-2 -c:v libx264 -preset veryfast -crf 23 "{dst.name}"'
    try:
        subprocess.run(shlex.split(cmd), stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=True)
        return dst.name
    except:
        try:
            os.unlink(dst.name)
        except:
            pass
        return src_path

# -------------------- MAIN PIPELINE -------------------- #
def process_video_file(video_path, capture_seconds=60, reencode=True, target_fps=30.0):
    """Main analysis pipeline."""
    mp_face = mp.solutions.face_mesh
    face_mesh = mp_face.FaceMesh(static_image_mode=False, max_num_faces=1, refine_landmarks=True)

    # Step 1: Re-encode to constant FPS for consistency
    if reencode:
        video_path = reencode_to_fixed_fps(video_path, fps=target_fps)
    fs = target_fps

    # Step 2: Read frames
    cap = cv2.VideoCapture(video_path)
    frames, rgbs, red_buf, blue_buf = [], [], [], []
    while True:
        ret, frame = cap.read()
        if not ret:
            break
        frames.append(frame)
        if len(frames) / fs >= capture_seconds:
            break
    cap.release()

    if len(frames) < int(fs * 3):
        return None

    # Step 3: Process each frame
    for f in frames:
        f_norm = normalize_frame_brightness(f)
        rgb_frame = cv2.cvtColor(f_norm, cv2.COLOR_BGR2RGB)
        results = face_mesh.process(rgb_frame)

        if results.multi_face_landmarks:
            rgb_avg = get_face_roi_avg(f_norm, results.multi_face_landmarks[0])
        else:
            rgb_avg = np.array([0, 0, 0])
        rgbs.append(rgb_avg)

        # Finger ROI (for SpO₂)
        h, w, _ = f.shape
        roi = f[h-110:h-30, 30:110]
        if roi.size > 0:
            red_buf.append(np.mean(roi[:, :, 2]))
            blue_buf.append(np.mean(roi[:, :, 0]))

    rgbs = np.array(rgbs)
    n = len(rgbs)
    timestamps = np.arange(n) / fs
    green = rgbs[:, 1]
    green_for_resp = green.copy()  # raw before bandpass filtering


    # Step 4: Interpolate missing frames
    mask = green != 0
    if np.sum(mask) < int(fs * 3):
        return None
    if not np.all(mask):
        green = np.interp(np.arange(n), np.where(mask)[0], green[mask])

    # Step 5: Signal preprocessing
        # Step 5: Signal preprocessing
    baseline = uniform_filter1d(green, size=int(fs * 1.5))
    g_corr = green - baseline
    g_detr = detrend(g_corr)
    g_filt = bandpass_filter(g_detr, fs, low=0.7, high=3.0)
    g_filt = median_filter(g_filt, size=3)

    # Step 6: Peak detection (Heart Rate)
    peaks, _ = find_peaks(g_filt, distance=int(fs * 0.5))
    if len(peaks) < 2:
        peaks, _ = find_peaks(g_filt, distance=int(fs * 0.3), prominence=np.std(g_filt) * 0.4)
    if len(peaks) < 2:
        return None

    # Step 7: Metrics calculation (HR, HRV, SpO₂, Resp)
    hr_bpm, rr = calculate_hr(peaks, timestamps)
    sdnn, rmssd, pnn50 = calculate_hrv(rr)
    stress = "Relaxed" if rmssd > 20 else "High Stress"

    # ----- Stable signal for Respiration -----
    # (Fixes phone camera exposure drift + irregular FPS)
    uniform_t = np.linspace(0, timestamps[-1], len(timestamps))
    uniform_signal = np.interp(uniform_t, timestamps, g_filt)

    # Remove slow exposure drift (very low frequency < 0.05 Hz)
    trend_removed = uniform_signal - uniform_filter1d(uniform_signal, size=int(fs * 5))

    # Respiration Rate (uses your tested logic)
    # resp_bpm = calculate_resp_rate(trend_removed, fs)
    # Use less aggressively normalized raw signal
    resp_baseline = uniform_filter1d(green_for_resp, size=int(fs * 2.5))
    resp_signal = green_for_resp - resp_baseline
    resp_bpm = calculate_resp_rate(resp_signal, fs)


    # SpO₂ (same as before)
    spo2 = calculate_spo2(np.array(red_buf), np.array(blue_buf)) if len(red_buf) > 5 else 0.0

    # Clamp HR to realistic human range
    hr_bpm = float(np.clip(hr_bpm, 40, 200))
   # ---- BP Prediction ----
    corrected_sys = None
    corrected_dia = None

    if MODELS_AVAILABLE:
        try:
            X_user = pd.DataFrame([[hr_bpm, sdnn, rmssd, pnn50]],
                                 columns=["HR", "SDNN", "RMSSD", "pNN50"])

            sys_pred = float(rf_sys.predict(X_user)[0])
            dia_pred = float(rf_dia.predict(X_user)[0])

        # Clip within realistic range
            sys_pred = np.clip(sys_pred, 80, 200)
            dia_pred = np.clip(dia_pred, 40, 130)

        # Apply your corrections
            corrected_sys = round(sys_pred * 0.85 + 15, 1)
            corrected_dia = round(dia_pred + 10, 1)

        except Exception as e:
            print("BP prediction failed:", e)
            corrected_sys = None
            corrected_dia = None



    # Step 8: Output
    # Step 8: Output
    print("\n------------------- RESULTS -------------------")
    print(f"Heart Rate (BPM):     {hr_bpm:.1f}")
    print(f"SpO₂ (%):             {spo2:.1f}")
    print(f"Respiration Rate:     {resp_bpm:.1f} breaths/min")
    print(f"SDNN (ms):            {sdnn:.2f}")
    print(f"RMSSD (ms):           {rmssd:.2f}")
    print(f"pNN50 (%):            {pnn50:.2f}")
    print(f"Stress Level:         {stress}")
    print(f"🩺 Estimated BP (Corrected): {corrected_sys:.1f}/{corrected_dia:.1f} mmHg")

    print("------------------------------------------------\n")
    bp_str = f"{corrected_sys}/{corrected_dia}" if corrected_sys and corrected_dia else None
    return {
        "heart_rate": hr_bpm,
        "sdnn": float(sdnn),
        "rmssd": float(rmssd),
        "pnn50": float(pnn50),
        "stress_level": stress,
        "spo2": float(spo2),
        "resp_rate": float(resp_bpm),
        "blood_pressure":bp_str
        
    }

    # baseline = uniform_filter1d(green, size=int(fs * 1.5))
    # g_corr = green - baseline
    # g_detr = detrend(g_corr)
    # g_filt = bandpass_filter(g_detr, fs, low=0.7, high=3.0)
    # g_filt = median_filter(g_filt, size=3)

    # # Step 6: Peak detection
    # peaks, _ = find_peaks(g_filt, distance=int(fs * 0.5))
    # if len(peaks) < 2:
    #     peaks, _ = find_peaks(g_filt, distance=int(fs * 0.3), prominence=np.std(g_filt) * 0.5)
    # if len(peaks) < 2:
    #     return None

    # # Step 7: Metrics calculation
    # hr_bpm, rr = calculate_hr(peaks, timestamps)
    # sdnn, rmssd, pnn50 = calculate_hrv(rr)
    # stress = "Relaxed" if rmssd > 20 else "High Stress"

    # spo2 = calculate_spo2(np.array(red_buf), np.array(blue_buf)) if len(red_buf) > 5 else 0.0
    # resp_bpm = calculate_resp_rate(g_filt, fs)
    # hr_bpm = float(np.clip(hr_bpm, 40, 200))

    # # Step 8: Output
    # print(f"[RESULT] HR={hr_bpm:.1f} BPM | SpO₂={spo2:.1f}% | Resp={resp_bpm:.1f} | Stress={stress}")
    # return {
    #     "heart_rate": hr_bpm,
    #     "sdnn": float(sdnn),
    #     "rmssd": float(rmssd),
    #     "pnn50": float(pnn50),
    #     "stress_level": stress,
    #     "spo2": float(spo2),
    #     "resp_rate": float(resp_bpm)
    # }


