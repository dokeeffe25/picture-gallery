(ns picture-gallery.core
  (:require [ajax.core :as ajax :refer [GET POST]]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [markdown.core :refer [md->html]]
            [picture-gallery.ajax :refer [load-interceptors!]]
            [picture-gallery.components.common :as common]
            [picture-gallery.components.gallery :as gallery]
            [picture-gallery.components.login :as login]
            [picture-gallery.components.registration :as registration]
            [picture-gallery.components.upload :as upload]
            [reagent.core :as reagent]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true])
  (:import goog.History))


(defn modal []
  (when-let [session-modal (session/get :modal)]
    [session-modal]))


(defn account-actions [id]
  (let [expanded? (reagent/atom false)]
    (fn []
      [:div.dropdown
       {:class    (when @expanded? "open")
        :on-click #(swap! expanded? not)}
       [:button.btn.btn-secondary.dropdown-toggle
        {:type :button}
        [:span.glyphicon.glyphicon-user] " " id [:span.caret]]
       [:div.dropdown-menu.user-actions
        [:a.dropdown-item.btn
         {:on-click
          #(session/put!
             :modal registration/delete-account-modal)}
         "delete account"]
        [:a.dropdown-item.btn
         {:on-click
          #(ajax/POST
             "/logout"
             {:handler (fn [] (session/remove! :identity))})}
         "sign out"]]])))


(defn user-menu []
  (if-let [id (session/get :identity)]
    [:ul.nav.navbar-nav.pull-right
     [:li.nav-item [upload/upload-button]]
     [:li.nav-item [account-actions id]]]
    [:ul.nav.navbar-nav.pull-right
     [:li.nav-item [login/login-button]]
     [:li.nav-item [registration/registration-button]]]))


(defn nav-link [uri title page collapsed?]
  [:li.nav-item
   {:class (when (= page (session/get :page)) "active")}
   [:a.nav-link
    {:href     uri
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


(defn galleries [gallery-links]
  [:div.text-xs-center
   (for [row (partition-all 3 gallery-links)]
     ^{:key row}
     [:div.row
      (for [{:keys [owner name]} row]
        ^{:key (str owner name)}
        [:div.col-sm-4
         [:a {:href (str "#/gallery/" owner)}
          [:img {:src (str js/context "/gallery/" owner "/" name)}]]])])])


(defn list-galleries! []
  (ajax/GET "/list-galleries"
            {:handler #(session/put! :gallery-links %)}))


(defn about-page []
  [:div.container
   [:div.row
    [:div.col-md-12
     [:img {:src (str js/context "/img/warning_clojure.png")}]]]])


(defn home-page []
  (list-galleries!)
  (fn []
    [:div.container
     [:div.row
      [:div.col-md-12>h2 "Available Galleries"]]
     (when-let [gallery-links (session/get :gallery-links)]
       [:div.row>div.col-md-12
        [galleries gallery-links]])]))



(def pages
  {:home    #'home-page
   :about   #'about-page
   :gallery #'gallery/gallery-page})


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


(secretary/defroute "/gallery/:owner" [owner]
                    (gallery/fetch-gallery-thumbs! owner)
                    (session/put! :page :gallery))


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
  (session/put! :identity js/identity)
  (mount-components))
