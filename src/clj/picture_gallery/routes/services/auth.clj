(ns picture-gallery.routes.services.auth
  (:require [buddy.hashers :as hashers]
            [clojure.tools.logging :as log]
            [picture-gallery.db.core :as db]
            [picture-gallery.validation :as validation]
            [ring.util.http-response :as http-response]))


(defn- handle-registration-error [e]
  (if (and
       (instance? java.sql.SQLException e)
       (-> (.getNextException e)
           (.getMessage)
           (.startsWith "ERROR: duplicate key value")))
    (http-response/precondition-failed
     {:result :error
      :message "User with the selected ID already exists"})
    (do
      (log/error e)
      (http-response/internal-server-error
       {:result :error
        :message "Server error occurred while adding the user"}))))


(defn register! [{:keys [session]} user]
  (if (validation/registration-errors user)
    (http-response/precondition-failed {:result :error})
    (try
      (db/create-user!
       (-> user
           (dissoc :pass-confirm)
           (update :pass hashers/encrypt)))
      (-> {:result :ok}
          http-response/ok
          (assoc :session (assoc session :identity (:id user))))
      (catch Exception e
        (handle-registration-error e)))))


(defn decode-auth [encoded]
  (let [auth (second (.split encoded " "))]
    (-> (.decode (java.util.Base64/getDecoder) auth)
        (String. (java.nio.charset.Charset/forName "UTF-8"))
        (.split ":"))))


(defn authenticate [[id pass]]
  (when-let [user (db/get-user {:id id})]
    (when (hashers/check pass (:pass user))
      id)))


(defn login! [{:keys [session]} auth]
  (if-let [id (authenticate (decode-auth auth))]
    (-> {:result :ok}
        http-response/ok
        (assoc :session (assoc session :identity id)))
    (http-response/unauthorized {:result :unauthorized
                                 :message "login failure"})))


(defn logout! []
  (-> {:result :ok}
      http-response/ok
      (assoc :session nil)))
