(ns datalog-console.chrome.extension.devtool.main
  (:require
   [clojure.pprint :refer [pprint]]
   [cljs.reader]
   [cljs.core.async :as async :refer [go <! put!]]
   [com.wsscode.common.async-cljs :refer [<?maybe]]
  ;;  [com.wsscode.pathom.core :as p]
  ;;  [com.wsscode.pathom.fulcro.network :as pfn]
  ;;  [fulcro-css.css-injection :as cssi]
  ;;  [fulcro.client :as fulcro]
  ;;  [fulcro.client.localized-dom :as dom]
  ;;  [fulcro.client.mutations :as fm]
  ;;  [fulcro.client.primitives :as fp]
  ;;  [fulcro.i18n :as fulcro-i18n]
  ;;  [fulcro.inspect.helpers :as h]
  ;;  [fulcro.inspect.lib.diff :as diff]
  ;;  [fulcro.inspect.lib.history :as hist]
  ;;  [fulcro.inspect.lib.local-storage :as storage]
  ;;  [fulcro.inspect.lib.misc :as misc]
  ;;  [fulcro.inspect.lib.version :as version]
  ;;  [fulcro.inspect.remote.transit :as encode]
  ;;  [fulcro.inspect.ui-parser :as ui-parser]
  ;;  [fulcro.inspect.ui.data-history :as data-history]
  ;;  [fulcro.inspect.ui.data-watcher :as data-watcher]
  ;;  [fulcro.inspect.ui.db-explorer :as db-explorer]
  ;;  [fulcro.inspect.ui.element :as element]
  ;;  [fulcro.inspect.ui.i18n :as i18n]
  ;;  [fulcro.inspect.ui.index-explorer :as fiex]
  ;;  [fulcro.inspect.ui.inspector :as inspector]
  ;;  [fulcro.inspect.ui.multi-inspector :as multi-inspector]
  ;;  [fulcro.inspect.ui.multi-oge :as multi-oge]
  ;;  [fulcro.inspect.ui.network :as network]
  ;;  [fulcro.inspect.ui.settings :as settings]
  ;;  [fulcro.inspect.ui.transactions :as transactions]
   [goog.object :as gobj]
  ;;  [goog.functions :refer [debounce]]
   [taoensso.timbre :as log]
  ;;  [taoensso.encore :as enc]
   ))


;; This namespace is what runs in the Chrome panel

(println ::loaded)


;; (declare fill-last-entry!)

;; (fp/defsc GlobalRoot [this {:keys [ui/root]}]
;;   {:initial-state (fn [params] {:ui/root
;;                                 (-> (fp/get-initial-state multi-inspector/MultiInspector params)
;;                                     (assoc-in [::multi-inspector/settings :ui/hide-websocket?] true))})
;;    :query         [{:ui/root (fp/get-query multi-inspector/MultiInspector)}]
;;    :css           [[:html {:overflow "hidden"}]
;;                    [:body {:margin "0" :padding "0" :box-sizing "border-box"}]]
;;    :css-include   [multi-inspector/MultiInspector]}
;;   (dom/div
;;    (cssi/style-element {:component this})
;;    (multi-inspector/multi-inspector root)))

;; (def app-uuid-key :fulcro.inspect.core/app-uuid)

(defonce global-inspector* (atom nil))

;(def current-tab-id js/chrome.devtools.inspectedWindow.tabId)

(defn post-message [port type data]
  (.postMessage port #js {:datalog-console-devtool-message (pr-str {:type type :data data :timestamp (js/Date.)})
                          :tab-id                         1 #_current-tab-id}))

(defn event-data [event]
  (some-> event (gobj/get "datalog-console-remote-message") cljs.reader/read-string))

;; (defn inc-id [id]
;;   (let [new-id (if-let [[_ prefix d] (re-find #"(.+?)(\d+)$" (str id))]
;;                  (str prefix (inc (js/parseInt d)))
;;                  (str id "-0"))]
;;     (cond
;;       (keyword? id) (keyword (subs new-id 1))
;;       (symbol? id) (symbol new-id)
;;       :else new-id)))

;; (defn inspector-app-names []
;;   (some->> @global-inspector* :reconciler fp/app-state deref ::inspector/id vals
;;            (mapv ::inspector/name) set))

;; (defn dedupe-name [name]
;;   (let [names-in-use (inspector-app-names)]
;;     (loop [new-name name]
;;       (if (contains? names-in-use new-name)
;;         (recur (inc-id new-name))
;;         new-name))))

;; (defonce last-disposed-app* (atom nil))

(defn start-app [data]
  (js/console.log "start-app" data))

;; #_(defn start-app [{:fulcro.inspect.core/keys   [app-id app-uuid]
;;                   :datalog-console.client.client/keys [initial-history-step remotes]}]
;;   (let [{:keys [reconciler] :as inspector} @global-inspector*
;;         {initial-state :value} initial-history-step
;;         new-inspector (-> (fp/get-initial-state inspector/Inspector initial-state)
;;                           (assoc ::inspector/id app-uuid)
;;                           (assoc :fulcro.inspect.core/app-id app-id)
;;                           (assoc ::inspector/name (dedupe-name app-id))
;;                           (assoc-in [::inspector/settings :ui/hide-websocket?] true)
;;                           (assoc-in [::inspector/db-explorer ::db-explorer/id] [app-uuid-key app-uuid])
;;                           (assoc-in [::inspector/app-state ::data-history/history-id] [app-uuid-key app-uuid])
;;                           (assoc-in [::inspector/app-state ::data-history/watcher ::data-watcher/id] [app-uuid-key app-uuid])
;;                           (assoc-in [::inspector/app-state ::data-history/watcher ::data-watcher/watches]
;;                                     (->> (storage/get [::data-watcher/watches app-id] [])
;;                                          (mapv (fn [path]
;;                                                  (fp/get-initial-state data-watcher/WatchPin
;;                                                                        {:path     path
;;                                                                         :expanded (storage/get [::data-watcher/watches-expanded app-id path] {})
;;                                                                         :content  (get-in initial-state path)})))))
;;                           (assoc-in [::inspector/app-state ::data-history/snapshots] (storage/tget [::data-history/snapshots app-id] []))
;;                           (assoc-in [::inspector/network ::network/history-id] [app-uuid-key app-uuid])
;;                           (assoc-in [::inspector/element ::element/panel-id] [app-uuid-key app-uuid])
;;                           (assoc-in [::inspector/i18n ::i18n/id] [app-uuid-key app-uuid])
;;                           (assoc-in [::inspector/i18n ::i18n/current-locale] (-> (get-in initial-state (-> initial-state ::fulcro-i18n/current-locale))
;;                                                                                  ::fulcro-i18n/locale))
;;                           (assoc-in [::inspector/i18n ::i18n/locales] (->> initial-state ::fulcro-i18n/locale-by-id vals vec
;;                                                                            (mapv #(vector (::fulcro-i18n/locale %) (:ui/locale-name %)))))
;;                           (assoc-in [::inspector/transactions ::transactions/tx-list-id] [app-uuid-key app-uuid])
;;                           (assoc-in [::inspector/oge] (fp/get-initial-state multi-oge/OgeView {:app-uuid app-uuid
;;                                                                                                :remotes  remotes}))
;;                           (assoc-in [::inspector/index-explorer] (fp/get-initial-state fiex/IndexExplorer
;;                                                                                        {:app-uuid app-uuid
;;                                                                                         :remotes  remotes})))]

;;     (hist/record-history-step! inspector app-uuid initial-history-step)
;;     (fill-last-entry!)

;;     (fp/transact! reconciler [::multi-inspector/multi-inspector "main"]
;;                   [`(multi-inspector/add-inspector ~new-inspector)])

;;     (when (= app-id @last-disposed-app*)
;;       (reset! last-disposed-app* nil)
;;       (fp/transact! reconciler [::multi-inspector/multi-inspector "main"]
;;                     [`(multi-inspector/set-app {::inspector/id ~app-uuid})]))

;;     (fp/transact! reconciler
;;                   [::db-explorer/id [app-uuid-key app-uuid]]
;;                   [`(db-explorer/set-current-state ~initial-history-step) :current-state])
;;     (fp/transact! reconciler
;;                   [::data-history/history-id [app-uuid-key app-uuid]]
;;                   [`(data-history/set-content ~initial-history-step) ::data-history/history])

;;     new-inspector))

;; (defn dispose-app [{:fulcro.inspect.core/keys [app-uuid]}]
;;   (let [{:keys [reconciler] :as inspector} @global-inspector*
;;         state         (fp/app-state reconciler)
;;         inspector-ref [::inspector/id app-uuid]
;;         app-id        (get (get-in @state inspector-ref) :fulcro.inspect.core/app-id)
;;         {::keys [db-hash-index]} (-> inspector :reconciler :config :shared)]

;;     (if (= (get-in @state [::multi-inspector/multi-inspector "main" ::multi-inspector/current-app])
;;            inspector-ref)
;;       (reset! last-disposed-app* app-id)
;;       (reset! last-disposed-app* nil))

;;     (hist/clear-history! inspector app-uuid)

;;     (fp/transact! reconciler [::multi-inspector/multi-inspector "main"]
;;                   [`(multi-inspector/remove-inspector {::inspector/id ~app-uuid})])))

;; (defn tx-run [{:datalog-console.client.client/keys [tx tx-ref]}]
;;   (let [{:keys [reconciler]} @global-inspector*]
;;     (if tx-ref
;;       (fp/transact! reconciler tx-ref tx)
;;       (fp/transact! reconciler tx))))

;; (defn reset-inspector []
;;   (-> @global-inspector* :reconciler fp/app-state (reset! (fp/tree->db GlobalRoot (fp/get-initial-state GlobalRoot {}) true))))

;; (defn- -fill-last-entry!
;;   []
;;   (enc/if-let [inspector  @global-inspector*
;;                reconciler (fp/get-reconciler inspector)
;;                state-map  @(fp/app-state inspector)
;;                app-uuid   (h/current-app-uuid state-map)
;;                state-id   (hist/latest-state-id inspector app-uuid)]
;;     (fp/transact! reconciler `[(hist/remote-fetch-history-step ~{:id state-id})])
;;     (log/error "Something was nil")))

;; (def fill-last-entry!
;;   "Request the full state for the currently-selected application"
;;   (debounce -fill-last-entry! 250))

;; (defn update-client-db [{:fulcro.inspect.core/keys   [app-uuid]
;;                          :datalog-console.client.client/keys [state-id]}]
;;   (let [step {:id state-id}]
;;     (hist/record-history-step! @global-inspector* app-uuid step)

;;     (fill-last-entry!)

;;     #_(if-let [current-locale (-> new-state ::fulcro-i18n/current-locale p/ident-value*)]
;;         (fp/transact! (:reconciler @global-inspector*)
;;                       [::i18n/id [app-uuid-key app-uuid]]
;;                       [`(fm/set-props ~{::i18n/current-locale current-locale})]))

;;     (fp/transact! (:reconciler @global-inspector*)
;;                   [::db-explorer/id [app-uuid-key app-uuid]]
;;                   [`(db-explorer/set-current-state ~step) :current-state])
;;     (fp/transact! (:reconciler @global-inspector*)
;;                   [::data-history/history-id [app-uuid-key app-uuid]]
;;                   [`(data-history/set-content ~step) ::data-history/history])))

;; (defn new-client-tx [{:fulcro.inspect.core/keys   [app-uuid]
;;                       :datalog-console.client.client/keys [tx]}]
;;   (let [{:fulcro.history/keys [db-before-id
;;                                db-after-id]} tx
;;         inspector @global-inspector*
;;         tx        (assoc tx
;;                          :fulcro.history/db-before (hist/history-step inspector app-uuid db-before-id)
;;                          :fulcro.history/db-after (hist/history-step inspector app-uuid db-after-id))]
;;     (fp/transact! (:reconciler @global-inspector*)
;;                   [:fulcro.inspect.ui.transactions/tx-list-id [app-uuid-key app-uuid]]
;;                   [`(fulcro.inspect.ui.transactions/add-tx ~tx) :fulcro.inspect.ui.transactions/tx-list])))

;; (defn set-active-app [{:fulcro.inspect.core/keys [app-uuid]}]
;;   (let [inspector @global-inspector*]
;;     (fp/transact! (:reconciler inspector) [::multi-inspector/multi-inspector "main"]
;;                   [`(multi-inspector/set-app {::inspector/id ~app-uuid})])))

;; (defn notify-stale-app []
;;   (let [inspector @global-inspector*]
;;     (fp/transact! (:reconciler inspector) [::multi-inspector/multi-inspector "main"]
;;                   [`(fm/set-props {::multi-inspector/client-stale? true})])))

;; (defn fill-history-entry
;;   "Called in response to the client sending us the real state for a given state id, at which time we update
;;    our copy of the history with the new value"
;;   [{:fulcro.inspect.core/keys   [app-uuid state-id]
;;     :datalog-console.client.client/keys [diff based-on state]}]
;;   (let [inspector @global-inspector*
;;         state     (if state
;;                     state
;;                     (let [base-state (hist/state-map-for-id inspector app-uuid based-on)]
;;                       (diff/patch base-state diff)))]
;;     (hist/record-history-step! inspector app-uuid {:id state-id :value state})
;;     (fp/force-root-render! inspector)))

;; LANDMARK: This is where incoming messages from the app are handled
(defn handle-remote-message [{:keys [port event responses*] :as _message}]
  (when-let [{:keys [type data]} (event-data event)]
    (let [data (assoc data ::port port)]
      (case type
        :datalog-console.client.client/init-app
        (start-app data)

        ;; :datalog-console.client.client/reload-db
        ;; (reload-db data)

        ;; :datalog-console.client.client/db-changed!
        ;; (update-client-db data)

        ;; :datalog-console.client.client/new-client-transaction
        ;; (new-client-tx data)

        ;; :datalog-console.client.client/history-entry
        ;; (fill-history-entry data)

        ;; :datalog-console.client.client/transact-inspector
        ;; (tx-run data)

        ;; :datalog-console.client.client/reset
        ;; (reset-inspector)

        ;; :datalog-console.client.client/dispose-app
        ;; (dispose-app data)

        ;; :datalog-console.client.client/set-active-app
        ;; (set-active-app data)

        ;; :datalog-console.client.client/message-response
        ;; (if-let [res-chan (get @responses* (::ui-parser/msg-id data))]
        ;;   (put! res-chan (::ui-parser/msg-response data)))

        ;; :datalog-console.client.client/client-version
        ;; (let [client-version (:version data)]
        ;;   (if (= -1 (version/compare client-version version/last-inspect-version))
        ;;     (notify-stale-app)))

        (log/debug "Unknown message" type)))))

(defonce message-handler-ch (async/chan (async/dropping-buffer 1024)))

(defn event-loop [_app responses*]
  (let [port (js/chrome.runtime.connect #js {:name "datalog-console-devtool"})]
    (.addListener (.-onMessage port)
                  (fn [msg]
                    (put! message-handler-ch
                          {:port       port
                           :event      msg
                           :responses* responses*})))
    (go
      (loop []
        (when-let [msg (<! message-handler-ch)]
          (<?maybe (handle-remote-message msg))
          (recur))))
    (js/console.log "the port:" port)
    (.postMessage port #js {:name "init" :tab-id 1 #_current-tab-id})
    (post-message port :datalog-console.client.client/request-page-apps {})

    port))

;; (defn respond-locally! [responses* type data]
;;   (if-let [res-chan (get @responses* (::ui-parser/msg-id data))]
;;     (put! res-chan (::ui-parser/msg-response data))
;;     (log/error "Failed to respond locally to message:" type "with data:" data)))

;; (defn ?handle-local-message [responses* type data]
;;   (case type
;;     :datalog-console.client.client/load-settings
;;     (let [settings (into {}
;;                          (remove (comp #{::not-found} second))
;;                          (for [k (:query data)]
;;                            [k (storage/get k ::not-found)]))]
;;       (respond-locally! responses* type
;;                         {::ui-parser/msg-response settings
;;                          ::ui-parser/msg-id       (::ui-parser/msg-id data)})
;;       :ok)
;;     :datalog-console.client.client/save-settings
;;     (do
;;       (doseq [[k v] data]
;;         (log/trace "Saving setting:" k "=>" v)
;;         (storage/set! k v))
;;       :ok)
;;     #_else nil))

;; (defn make-network [port* parser responses*]
;;   (pfn/fn-network
;;    (fn [this edn ok error]
;;      (go
;;        (try
;;          (ok (<! (parser {:send-message (fn [type data]
;;                                           (or
;;                                            (?handle-local-message responses* type data)
;;                                            (post-message @port* type data)))
;;                           :responses*   responses*} edn)))
;;          (catch :default e
;;            (error e)))))
;;    false))


(defn start-global-inspector [options]
  (let [port*      (atom nil)
        responses* (atom {})
        _ (reset! port* (event-loop nil responses*))
        ;; #_app        #_(fulcro/new-fulcro-client
        ;;             :started-callback
        ;;             (fn [app]
        ;;               (reset! port* (event-loop app responses*))
        ;;               (post-message @port* :datalog-console.client.client/check-client-version {})
        ;;               (settings/load-settings (:reconciler app)))

        ;;             :shared
        ;;             {::hist/db-hash-index (atom {})}

        ;;             :networking
        ;;             (make-network port* (ui-parser/parser) responses*))
        node       (js/document.createElement "div")]
    (js/document.body.appendChild node)
    #_(fulcro/mount app GlobalRoot node)))

(defn global-inspector
  ([] @global-inspector*)
  ([options]
   (or @global-inspector*
       (reset! global-inspector* (start-global-inspector options)))))

(global-inspector {})