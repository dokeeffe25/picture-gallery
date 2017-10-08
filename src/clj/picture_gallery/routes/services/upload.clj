(ns picture-gallery.routes.services.upload
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [picture-gallery.db.core :as db]
            [ring.util.http-response :as http-response])
  (:import java.awt.geom.AffineTransform
           [java.awt.image AffineTransformOp BufferedImage]
           [java.io ByteArrayOutputStream File FileInputStream]
           java.net.URLEncoder
           javax.imageio.ImageIO))

(def thumb-size 150)
(def thumb-prefix "thumb_")


(defn- scale [image ratio width height]
  (let [scale (AffineTransform/getScaleInstance (double ratio) (double ratio))
        transform-op (AffineTransformOp. scale AffineTransformOp/TYPE_BILINEAR)]
    (.filter transform-op image (BufferedImage. width height (.getType image)))))


(defn scale-image [file thumb-size]
  (let [image  (ImageIO/read file)
        width  (.getWidth image)
        height (.getHeight image)
        ratio  (/ thumb-size height)]
    (scale image ratio (int (* width ratio)) thumb-size)))


(defn image->byte-array [image]
  (let [byte-output-stream (ByteArrayOutputStream.)]
    (ImageIO/write image "png" byte-output-stream)
    (.toByteArray byte-output-stream)))


(defn- file->byte-array [tempfile]
  (with-open [input  (FileInputStream. tempfile)
              buffer (ByteArrayOutputStream.)]
    (clojure.java.io/copy input buffer)
    (.toByteArray buffer)))


(defn save-image! [user {:keys [tempfile filename content-type]}]
  (try
    (let [db-filename (str user (.replaceAll filename "[^a-zA-Z0-9-_\\.]" ""))]
      (db/save-file! {:owner user
                      :type content-type
                      :name db-filename
                      :data (file->byte-array tempfile)})
      (db/save-file! {:owner user
                      :type "image/png"
                      :name (str thumb-prefix db-filename)
                      :data (image->byte-array
                             (scale-image tempfile thumb-size))}))
    (http-response/ok {:result :ok})
    (catch Exception e
      (log/error e)
      (http-response/internal-server-error "error"))))
