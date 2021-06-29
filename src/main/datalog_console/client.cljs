(ns datalog-console.client
  {:no-doc true}
  (:require [reagent.dom :as rdom]
            [reagent.core :as r]
            [datalog-console.components.schema :as c.schema]
            [datalog-console.components.entity :as c.entity]
            [datalog-console.components.entities :as c.entities]
            [datalog-console.components.query :as c.query]
            [datalog-console.components.transact :as c.transact]
            [datascript.core :as d]
            [goog.object :as gobj]
            [clojure.string :as str]
            [datalog-console.components.feature-flag :as feature-flag]
            [cljs.reader]))

(def rconn (r/atom (d/create-conn {})))
(def rerror (r/atom nil))
(def entity-lookup-ratom (r/atom ""))
(def integration-version (r/atom nil))

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
                    (when-let [remote-message (cljs.reader/read-string (gobj/getValueByKeys msg ":datalog-console.remote/remote-message"))]
                      (cond
                        (d/db? remote-message)
                        (reset! rconn (d/conn-from-db remote-message))

                        (:version remote-message) 
                        (reset! integration-version (:version remote-message))

                        (:datalog-console.client.response/transact! remote-message)
                        (post-message devtool-port :datalog-console.client/request-whole-database-as-string {})

                        (:error remote-message)
                        (reset! rerror (:error remote-message)))))) 
            
    (.postMessage port #js {:name ":datalog-console.client/init" :tab-id current-tab-id})
    (post-message devtool-port :datalog-console.client/request-integration-version {}))
  (catch js/Error _e nil))

(defn tabs []
  (let [active-tab (r/atom "Entity")
        tabs ["Entity" "Query" "Transact"]
        on-tx-submit (fn [tx-str] (post-message devtool-port :datalog-console.client/transact! tx-str))]
    @(r/track! #(do @entity-lookup-ratom
                    (reset! active-tab "Entity")))
    (fn [rconn entity-lookup-ratom]
      [:div {:class "flex flex-col overflow-hidden col-span-2"}
       [:ul {:class "text-xl border-b flex flex-row"}
        (doall (for [tab-name tabs]
                 ^{:key (str tab-name)}
                 [:li {:class (str (when (= tab-name @active-tab) "border-b-4 border-blue-400 ") "px-2 pt-2 cursor-pointer hover:bg-blue-100 focus:bg-blue-100")
                       :on-click #(reset! active-tab tab-name)}
                  [:h2 tab-name]]))]
       (case @active-tab
         "Entity" [:div {:class "overflow-auto h-full w-full mt-2"}
                   [c.entity/entity @rconn entity-lookup-ratom]]
         "Query"  [:div {:class "overflow-auto h-full w-full mt-2"}
                   [c.query/query @rconn]]
         "Transact" [feature-flag/version-check
                     {:title "Transact"
                      :required-version "0.3.1"
                      :current-version @integration-version}
                     [:div {:class "overflow-auto h-full w-full mt-2"}
                      [c.transact/transact on-tx-submit rerror]]])])))

(defn root []
  (let [loaded-db? (r/atom false)]
    (fn []
      [:div {:class "relative text-xs h-full w-full grid grid-cols-4"}
       [:div {:class "flex flex-col border-r pt-2 overflow-hidden col-span-1 "}
        [:h2 {:class "pl-1 text-xl border-b flex center"} "Schema"]
        [:div {:class "overflow-auto h-full w-full"}
         [c.schema/schema @rconn]]]
       [:div {:class "flex flex-col border-r overflow-hidden col-span-1 "}
        [:h2 {:class "px-1 text-xl border-b pt-2"} "Entities"]
        [:div {:class "overflow-auto h-full w-full"}
         [c.entities/entities @rconn entity-lookup-ratom]]]
       [tabs rconn entity-lookup-ratom]
       [:button
        {:class "absolute top-2 right-1 py-1 px-2 rounded bg-gray-200 border"
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