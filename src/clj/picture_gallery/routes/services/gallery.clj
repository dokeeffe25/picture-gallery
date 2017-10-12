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