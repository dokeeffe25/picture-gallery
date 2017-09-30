(ns picture-gallery.core
  (:require [ajax.core :refer [GET POST]]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [markdown.core :refer [md->html]]
            [picture-gallery.ajax :refer [load-interceptors!]]
            [picture-gallery.components.common :as common]
            [picture-gallery.components.registration :as registration]
            [reagent.core :as reagent]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true])
  (:import goog.History))


(defn modal []
  (when-let [session-modal (session/get :modal)]
    [session-modal]))


(defn user-menu []
  (if-let [id (session/get :identity)]
    [:ul.nav.navbar-nav.pull-right
     [:li.nav-item
      [:a.dropdown-item.btn
       {:on-click #(session/remove! :identity)}
       [:i.fa.fa-user] " " id " | sign out"]]]
    [:ul.nav.navbar-nav.pull-right
     [:li.nav-item [registration/registration-button]]]))


(defn nav-link [uri title page collapsed?]
  [:li.nav-item
   {:class (when (= page (session/get :page)) "active")}
   [:a.nav-link
    {:href uri
     :on-click #(reset! collapsed? true)} title]])


(defn navbar []
  (let [collapsed? (reagent/atom [])]
    (fn []
      [:nav.navbar.navbar-light.bg-faded
       [:button.navbar-toggler.hidden-sm-up
        {:on-click #(swap! collapsed? not)} "☰"]
       [:div.collapse.navbar-toggleable-xs
        (when-not @collapsed? {:class "in"})
        [:a.navbar-brand {:href "#/"} "picture-gallery"]
        [:ul.nav.navbar-nav
         [nav-link "#/" "Home" :home collapsed?]
         [nav-link "#/" "About" :about collapsed?]
         [user-menu]]]])))


(defn about-page []
  [:div.container
   [:div.row
    [:div.col-md-12
     [:img {:src (str js/context "/img/warning_clojure.png")}]]]])


(defn home-page []
  [:div.container
   (when-let [docs (session/get :docs)]
     [:div.row>div.col-sm-12
      [:div {:dangerouslySetInnerHTML
             {:__html (md->html docs)}}]])])


(def pages
  {:home #'home-page
   :about #'about-page})


(defn page []
  [:div
   [modal]
   [(pages (session/get :page))]])


;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")


(secretary/defroute "/" []
  (session/put! :page :home))


(secretary/defroute "/about" []
  (session/put! :page :about))


;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
        (events/listen
          HistoryEventType/NAVIGATE
          (fn [event]
              (secretary/dispatch! (.-token event))))
        (.setEnabled true)))


;; -------------------------
;; Initialize app
(defn fetch-docs! []
  (GET "/docs" {:handler #(session/put! :docs %)}))


(defn mount-components []
  (reagent/render [#'navbar] (.getElementById js/document "navbar"))
  (reagent/render [#'page] (.getElementById js/document "app")))


(defn init! []
  (load-interceptors!)
  (hook-browser-navigation!)
  (mount-components))
