(ns datalog-console.client
  (:require [reagent.dom :as rdom]
            [reagent.core :as r]
            [datalog-console.components.schema :as c.schema]
            [datalog-console.components.entity :as c.entity]
            [datalog-console.components.entity-scanner :as c.entity-scanner]
            [datascript.core :as d]
            [goog.object :as gobj]
            [cljs.reader]))



(def rconn (r/atom (d/create-conn {})))
(def entity-lookup-ratom (r/atom ""))

(try
  (def current-tab-id js/chrome.devtools.inspectedWindow.tabId)

  (def create-port #(js/chrome.runtime.connect #js {:name %}))
  (def devtool-port (create-port ":datalog-console.client/devtool-port"))

  (defn post-message [port type data]
    (.postMessage port #js {":datalog-console.client/devtool-message" (pr-str {:type type :data data :timestamp (js/Date.)})
                            :tab-id        current-tab-id}))


  (let [port devtool-port]
    (.addListener (gobj/get port "onMessage")
                  (fn [msg]
                    (when-let [db-str (gobj/getValueByKeys msg ":datalog-console.remote/remote-message")]
                      (reset! rconn (d/conn-from-db (cljs.reader/read-string db-str))))))

    (.postMessage port #js {:name ":datalog-console.client/init" :tab-id current-tab-id}))
  (catch js/Error _e nil))

  
(defn root []
  (let [loaded-db? (r/atom false)]
    (fn []
      [:div {:class "relative text-xs h-full w-full flex flex-row"}
       [:div {:class "w-80 flex flex-col flex-1 border-r stack-small pt-2"}
        [:h2 {:class "pl-1 text-xl border-b flex center"} "Schema Viewer"]
        [c.schema/schema @rconn]]
       [:div {:class "w-80 flex flex-col flex-1 border-r"}
        [:h2 {:class "px-1 text-xl border-b pt-2"} "Entity Scanner"]
        [c.entity-scanner/entity-scan @rconn entity-lookup-ratom]]
       [:div {:class "flex flex-col stack-small"
              :style {:flex 2}}
        [:h2 {:class "px-1 text-xl border-b pt-2"} "Entity Inspector"]
        [c.entity/entity @rconn entity-lookup-ratom]]
       [:button
        {:class "absolute top-2 right-1 py-1 px-2 rounded bg-gray-200 border shadow-hard btn-border"
         :on-click (fn []
                     (when-not @loaded-db? (reset! loaded-db? true))
                     (post-message devtool-port :datalog-console.client/request-whole-database-as-string {}))}
        (if @loaded-db? "Refresh database" "Load database")]])))

(defn mount! []
  (rdom/render [root] (js/document.getElementById "root")))

(defn init! []
  (mount!))

(defn ^:dev/after-load remount!
  "Remounts the whole UI on every save. Def state you want to persist between remounts with defonce."
  []
  (mount!))