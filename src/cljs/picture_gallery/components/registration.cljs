(ns picture-gallery.components.registration
  (:require [ajax.core :as ajax]
            [picture-gallery.components.common :as common]
            [picture-gallery.validation :as validation]
            [reagent.core :as reagent]
            [reagent.session :as session]))

(enable-console-print!)


(defn register! [fields errors]
  (reset! errors (validation/registration-errors @fields))
  (println "REGISTER" @errors)
  (when-not @errors
    (ajax/POST "/register"
        {:params @fields
         :handler #(do
                     (session/put! :identity (:id @fields))
                     (reset! fields {})
                     (session/remove! :modal))
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
        (when-let [error (:id @error)]
          [:div.alert.alert-danger error])
        [common/password-input "password" :pass "enter a password" fields]
        (when-let [error (:pass @error)]
          [:div.alert.alert-danger {:role "alert"} error])
        [common/password-input
         "password" :pass-confirm "re-enter the password" fields]
        (when-let [error (:server-error @error)]
          [:div.alert.alert-danger error])]
       [:div
        [:button.btn.btn-primary
         {:on-click #(register! fields error)}
         "Register"]
        [:button.btn.btn-danger
         {:on-click #(session/remove! :modal)}
         "Cancel"]]))))


(defn registration-button []
  [:a.btn
   {:on-click #(session/put! :modal registration-form)}
   "register"])


(defn delete-account! []
  (ajax/DELETE "/account"
               {:handler #(do
                            (session/remove! :identity)
                            (session/put! :page :home))}))


(defn delete-account-modal []
  (fn []
    [common/modal
     [:h2.alert.alert-danger "Delete Account"]
     [:p "Are you sure you wish to delete the account and associated gallery?"]
     [:div
      [:button.btn.btn-primary
       {:on-click (fn []
                    (delete-account!)
                    (session/remove! :modal))}
       "Delete"]
      [:button.btn.btn-danger
       {:on-click (fn []
                    (session/remove! :modal))}
       "Cancel"]]]))