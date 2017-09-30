(ns picture-gallery.components.registration
  (:require [ajax.core :as ajax]
            [picture-gallery.components.common :as common]
            [picture-gallery.validation :as validation]
            [reagent.core :as reagent]
            [reagent.session :as session]))


(defn register! [fields errors]
  (reset! errors (validation/registration-errors @fields))
  (when-not @errors
    (ajax/POST "/register"
        {:params @fields
         :handler #(do
                     (session/put! :identity (:id @fields))
                     (reset! fields {}))
         :error-handler #(reset!
                          errors
                          {:server-error (get-in % [:response :message])})})))


(defn registration-form []
  (let [fields (reagent/atom {})
        error (reagent/atom nil)]
    (fn []
      (common/modal
       [:div "Picture Gallery Registration"]
       [:div
        [:div.well.well-sm
         [:strong "* required field"]]
        [common/text-input "name" :id "enter a user name" fields]
        [common/password-input "password" :pass "enter a password" fields]
        [common/password-input
         "password" :pass-confirm "re-enter the password" fields]
        (when-let [error {:server-error @error}]
          [:div.alert.alert-danger error])]
       [:div
        [:button.btn.btn-primary
         {:on-click #(register! fields error)}
         "Register"]
        [:button.btn.btn-danger "Cancel"]]))))
