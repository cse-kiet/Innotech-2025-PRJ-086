# app.py
from flask import Flask, request, jsonify
import numpy as np, cv2, tempfile, os
from vitals_model import process_video_file

app = Flask(__name__)

@app.route("/predict", methods=["POST"])
def predict():
    if "video" not in request.files:
        return jsonify({"error": "No video uploaded. Use form field 'video'."}), 400

    vid = request.files["video"]
    # save temporarily
    tmp = tempfile.NamedTemporaryFile(delete=False, suffix=".mp4")
    try:
        vid.save(tmp.name)
        tmp.flush()

        # You can optionally read a form parameter 'seconds' to set capture duration
        capture_seconds = int(request.form.get("seconds", 30))

        result = process_video_file(tmp.name, capture_seconds=capture_seconds)
        if result is None:
            return jsonify({"error": "Not enough face frames detected or video too short."}), 400

        return jsonify(result)
    finally:
        tmp.close()
        try:
            os.unlink(tmp.name)
        except:
            pass

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=False, use_reloader=False)

