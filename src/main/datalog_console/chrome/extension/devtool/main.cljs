(ns datalog-console.chrome.extension.devtool.main
  (:require
   [clojure.pprint :refer [pprint]]
   [clojure.edn]
   [cljs.reader]
   [reagent.dom :as rdom]
   [reagent.core :as r]
   [goog.object :as gobj]
   [taoensso.timbre :as log]
   [datalog-console.workspaces.entity-cards :refer [conn]]
   [datalog-console.components.entity :as c.entity]
   [datascript.core :as d]
   ))


(println ::loaded)

(def remote-conn (r/atom nil))


(def current-tab-id js/chrome.devtools.inspectedWindow.tabId)

(def create-port #(js/chrome.runtime.connect #js {:name %}))
(def devtool-port (create-port "devtool"))

(defn post-message [port type data]
  (.postMessage port #js {:devtool-message (pr-str {:type type :data data :timestamp (js/Date.)})
                          :tab-id        current-tab-id}))



(let [port devtool-port]
  (.addListener (gobj/get port "onMessage")
                (fn [msg]
                  (println "content-script-message got from keys: " (gobj/getValueByKeys msg "content-script-message"))
                  (when-let [db-str (gobj/getValueByKeys msg "datalog-remote-message")]
                    (let [db-conn (d/conn-from-db (clojure.edn/read-string
                                                   {:readers d/data-readers} db-str))]

                      (reset! remote-conn db-conn)))))

  (.postMessage port #js {:name "init" :tab-id current-tab-id})
  (post-message port :hello-console/type {}))




(defn root []
  (fn []
    [:div
     [:h1 "Datalog Console"]
     [:button
      {:on-click #(post-message devtool-port :db-request {})}
      "Fetch the db"]
     (if @remote-conn
       [c.entity/entity @remote-conn]
       [:h2 "No database available"])]))

(defn mount! []
  (rdom/render [root] (js/document.getElementById "root")))

(defn init! []
  (mount!))

(defn ^:dev/after-load remount!
  "Remounts the whole UI on every save. Def state you want to persist between remounts with defonce."
  []
  (mount!))

(mount!)