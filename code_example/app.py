import datetime
import os
import time
from numbers import Rational
from uuid import uuid4

from flask import Flask, Response, abort, jsonify, request, send_file
from PIL import ExifTags, Image

app = Flask(__name__, static_folder="./static", static_url_path="/")


@app.get("/")
def default_route():
    return app.send_static_file("index.html")


@app.get("/uploads")
def get_uploaded_images():
    if not os.path.exists("uploads"):
        return jsonify({"images": []})

    return jsonify(
        {
            "images": [
                {
                    "name": filename,
                    "url": f"/uploads/{filename}",
                    "thumb": f"/thumbnails/{filename}",
                }
                for filename in os.listdir("thumbnails")
            ]
        }
    )


@app.get("/uploads/<filename>")
def get_one_uploaded_image(filename):
    if not os.path.exists(f"uploads/{filename}"):
        return abort(404)
    return send_file(f"uploads/{filename}")


@app.get("/thumbnails/<filename>")
def get_image_thumbnail(filename):
    if not os.path.exists(f"thumbnails/{filename}"):
        return abort(404)
    return send_file(f"thumbnails/{filename}")


@app.route("/upload", methods=["POST"])
def upload_image():
    """
    Upload an image
    """

    if "file" not in request.files:
        return jsonify({"error": "No file part"}), 400
    f = request.files["file"]
    if f.filename == "":
        return jsonify({"error": "No selected file"}), 400

    ext = f.filename.rsplit(".", 1)[-1].lower()
    if ext not in {"jpg", "jpeg", "png"}:
        return jsonify({"error": "Invalid file type"}), 400

    try:
        if not os.path.exists("uploads"):
            os.makedirs("uploads")
        # compose filename as %Y%m%d_%H%M%S_<original_filename>
        n = datetime.datetime.now().strftime("%Y%m%d_%H%M%S_") + f.filename
        # join folder name and filename
        p = os.path.join("uploads", n)
        # save file
        f.save(p)

        # TODO rotate image by EXIF metadata
        # with Image.open(p) as i:
        #     exif = i.getexif()
        #     if exif:
        #         if 274 in exif:
        #             o = exif[274]
        #             if o == 3:
        #                 i = i.rotate(180, expand=True)
        #                 i.save(p)
        #             elif o == 6:
        #                 i = i.rotate(270, expand=True)
        #                 i.save(p)
        #             elif o == 8:
        #                 i = i.rotate(90, expand=True)
        #                 i.save(p)

        # create thumbnail
        if not os.path.exists("thumbnails"):
            os.makedirs("thumbnails")
        with Image.open(p) as i:
            i.thumbnail((200, 200))
            t = os.path.join("thumbnails", n)
            i.save(t)

        # extract EXIF
        m = {}
        try:
            with Image.open(p) as i:
                exif = i.getexif()
                if exif:
                    for tag, value in exif.items():
                        x = ExifTags.TAGS.get(tag, tag)
                        if not isinstance(value, int) and isinstance(value, Rational):
                            value = float(value)
                        m[x] = value
        except Exception as e:
            m["error"] = str(e)

        # log manually
        with open("upload.log", "a") as k:
            k.write(f"{datetime.datetime.now()} uploaded {n}\n")

        return jsonify(
            {
                "filename": n,
                "path": p,
                "thumbnail": t,
                "metadata": m,
            }
        )
    except Exception as e:
        return jsonify({"error": str(e)}), 500


# Support auto-reload of remote page
app_id = uuid4()


@app.get("/ping")
def ping():
    def generate_status():
        while True:
            time.sleep(0.5)
            yield f"data: {app_id}\n\n"

    return Response(generate_status(), content_type="text/event-stream")


if __name__ == "__main__":
    app.run(debug=True)
