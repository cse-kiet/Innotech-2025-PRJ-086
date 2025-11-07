import wfdb
import matplotlib.pyplot as plt
import numpy as np
from scipy.signal import find_peaks

# Load record (without .dat/.hea extension)
record = wfdb.rdrecord('3544749_0005')  
wfdb.plot_wfdb(record=record, title='Waveform Signals')

print("Signals:", record.sig_name)
print("Sampling Frequency:", record.fs)



signals = record.sig_name
data = record.p_signal

print("\nAvailable signals:", signals)

if "ABP" in signals:
    abp = data[:, signals.index("ABP")]

    # --- Basic BP stats ---
    systolic = np.max(abp)
    diastolic = np.min(abp)
    mean_bp = np.mean(abp)

    print(f"🩸 Systolic BP: {systolic:.1f} mmHg")
    print(f"🩸 Diastolic BP: {diastolic:.1f} mmHg")
    print(f"🩸 Mean BP: {mean_bp:.1f} mmHg")

    # --- Estimate HR from ABP waveform ---
    peaks, _ = find_peaks(abp, distance=record.fs*0.5)
    if len(peaks) > 1:
        hr = 60 * record.fs / np.mean(np.diff(peaks))
        print(f"💓 Estimated Heart Rate: {hr:.1f} BPM")
    else:
        print("⚠ Not enough peaks to estimate HR.")
else:
    print("⚠ ABP signal not found in this record.")