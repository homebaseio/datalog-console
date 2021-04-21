(ns datalog-console.chrome.extension.devtool.main
  (:require
   [clojure.pprint :refer [pprint]]
   [cljs.reader]
   [reagent.dom :as rdom]
   [goog.object :as gobj]
   [taoensso.timbre :as log]
   ))


(println ::loaded)


(def current-tab-id js/chrome.devtools.inspectedWindow.tabId)

(def create-port #(js/chrome.runtime.connect #js {:name %}))

(defn post-message [port type data]
  (.postMessage port #js {:devtool-message (pr-str {:type type :data data :timestamp (js/Date.)})
                          :tab-id        current-tab-id}))



(let [port (create-port "devtool")]
  (.addListener (gobj/get port "onMessage")
                (fn [msg]
                  (println "MSG" msg)))

  (js/console.log #js {:port port
                       :tab-id js/chrome.devtools.inspectedWindow.tabId})

  (.postMessage port #js {:name "init" :tab-id current-tab-id})
  (post-message port :hello-console/type {}))


(defn root []
  [:div [:h1 "Rendered component"]
   [:button
    {:on-click #((js/console.log "hello")
                 (post-message (create-port "devtool-button") :hello {}))}
    "Fetch the db"]])

(defn mount! []
  (rdom/render [root] (js/document.getElementById "root")))

(defn init! []
  (mount!))

(defn ^:dev/after-load remount!
  "Remounts the whole UI on every save. Def state you want to persist between remounts with defonce."
  []
  (mount!))

(mount!)