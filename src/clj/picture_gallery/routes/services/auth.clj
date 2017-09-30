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
      (log/info "HERE")
      (db/create-user!
       (-> user
           (dissoc :pass-confirm)
           (update :pass hashers/encrypt)))
      (-> {:result :ok}
          http-response/ok
          (assoc :session (assoc session :identity (:id user))))
      (catch Exception e
        (handle-registration-error e)))))
