(ns picture-gallery.components.login
  (:require [ajax.core :as ajax]
            [clojure.string :as string]
            [goog.crypt.base64 :as b64]
            [picture-gallery.components.common :as common]
            [reagent.core :as reagent]
            [reagent.session :as session]))


(defn encode-auth [user pass]
  (->> (str user ":" pass)
       (b64/encodeString)
       (str "Basic ")))


(defn login! [fields error]
  (let [{:keys [id pass]} @fields]
    (reset! error nil)
    (ajax/POST "/login"
        {:headers {"Authorization" (encode-auth (string/trim id) pass)}
         :handler #(do
                     (session/remove! :modal)
                     (session/put! :identity id)
                     (reset! fields nil))
         :error-handler #(reset! error (get-in % [:response :message]))})))


(defn login-form []
  (let [fields (reagent/atom {})
        error (reagent/atom nil)]
    (fn []
      (common/modal
       [:div "Picture Gallery Login"]
       [:div
        [:div.well.well-sm
         [:strong "* required field"]]
        [common/text-input "name" :id "enter a user name" fields]
        [common/password-input "password" :pass "enter a password" fields]
        (when-let [error @error]
          [:div.alert.alert-danger error])]
       [:div
        [:button.btn.btn-primary
         {:on-click #(login! fields error)}
         "Login"]
        [:button.btn.btn-danger
         {:on-click #(session/remove! :modal)}
         "Cancel"]]))))


(defn login-button []
  [:a.btn
   {:on-click #(session/put! :modal login-form)}
   "login"])
