(ns picture-gallery.routes.services.gallery
  (:require [picture-gallery.layout :as layout]
            [picture-gallery.db.core :as db]
            [ring.util.http-response :as http-response])
  (:import java.io.ByteArrayInputStream))


(defn get-image [owner name]
  (if-let [{:keys [type data]} (db/get-image {:owner owner :name name})]
    (-> (ByteArrayInputStream. data)
        http-response/ok
        (http-response/content-type type))
    (layout/error-page {:status 404
                        :title "page not found"})))


(defn list-thumbnails [owner]
  (http-response/ok (db/list-thumbnails {:owner owner})))


(defn list-galleries []
  (http-response/ok (db/select-gallery-previews)))


(defn delete-image! [owner thumb-name image-name]
  (db/delete-file! {:owner owner :name thumb-name})
  (db/delete-file! {:owner owner :name image-name})
  (http-response/ok {:result :ok}))