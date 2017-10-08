(ns picture-gallery.routes.services
  (:require [buddy.auth :refer [authenticated?]]
            [buddy.auth.accessrules :refer [restrict]]
            [compojure.api.meta :refer [restructure-param]]
            [compojure.api.sweet :refer :all]
            [compojure.api.upload :as api.upload]
            [picture-gallery.routes.services.auth :as auth]
            [picture-gallery.routes.services.upload :as upload]
            [ring.util.http-response :refer :all]
            [schema.core :as s]))

(s/defschema UserRegistration
  {:id           String
   :pass         String
   :pass-confirm String})


(s/defschema Result
  {:result                   s/Keyword
   (s/optional-key :message) String})


(defn access-error [_ _]
  (unauthorized {:error "unauthorized"}))


(defn wrap-restricted [handler rule]
  (restrict handler {:handler  rule
                     :on-error access-error}))


(defmethod restructure-param :auth-rules
  [_ rule acc]
  (update-in acc [:middleware] conj [wrap-restricted rule]))


(defmethod restructure-param :current-user
  [_ binding acc]
  (update-in acc [:letks] into [binding `(:identity ~'+compojure-api-request+)]))


(defapi service-routes
  {:swagger {:ui "/swagger-ui"
             :spec "/swagger.json"
             :data {:info {:version "1.0.0"
                           :title "Picture Gallery API"
                           :description "Public Services"}}}}
  (POST "/register" req
    :return Result
    :body [user UserRegistration]
    :summary "register a new user"
    (auth/register! req user))
  (POST "/login" req
    :header-params [authorization :- String]
    :summary "log in the user and create a session"
    :return Result
    (auth/login! req authorization))
  (POST "/logout" []
    :summary "remove user session"
    :return Result
    (auth/logout!)))


(defapi restricted-service-routes
  {:swagger {:ui "/swagger-ui-private"
             :spec "/swagger-private.json"
             :data {:info {:version "1.0.0"
                           :title "Picture Gallery API"
                           :description "Private Services"}}}}
  (POST "/upload" req
    :multipart-params [file :- api.upload/TempFileUpload]
    :middleware [api.upload/wrap-multipart-params]
    :summary "handles image upload"
    :return Result
    (upload/save-image! (:identity req) file)))
