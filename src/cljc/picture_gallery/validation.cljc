(ns picture-gallery.validation
  (:require [struct.core :as st]))


(def validate=
  {:message "Fields are not identical"
   :optional true
   :validate #(= %1 %2)})


(defn registration-errors [{:keys [pass-confirm] :as params}]
  (first
   (st/validate
    params
    {:id st/required
     :pass [st/required
            [st/min-count 7 :message "Password must contain at least 8 characters"]
            [validate= pass-confirm :message "Re-entered password does not match"]]})))
