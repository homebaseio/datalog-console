(ns datalog-console.workspaces.chrome.extension-cards
  (:require [nubank.workspaces.core :as ws]
            [goog.object :as gobj]
            [nubank.workspaces.card-types.react :as ct.react]
            [datalog-console.workspaces.entity-cards :refer [conn]]))



(set! (.-datascriptDbConn js/window) conn)

(def counter (atom 0))


(.addEventListener js/window "message"
                   (fn [event] 
                    ;;  (println "extension cards")
                      (case (gobj/get event "data")
                        
                        "db-request"
                        (let [db-string (pr-str @conn)]
                          (js/console.log "application recieved *db-request*")
                          (swap! counter inc)
                          ;; (.postMessage js/window "db-forward" "*")
                          (.postMessage js/window #js {:datalog-remote-message db-string} "*")
                          (println "application sent the *db* to *content script*")
                          #_(js/console.log (pr-str conn)))
                        
                        (js/console.log "Ignoring (workspace)" (gobj/get event "data")))))

; add lisener to reset rconn atom
#_(js/chrome.runtime.onConnect.addListener
 (fn [port] (js/console.log "main tab the port: " port)))

#_(js/chrome.runtime.onRequest.addListener
   (fn [port] (js/console.log "main tab the request: " port)))

(defn element [name props & children]
  (apply js/React.createElement name (clj->js props) children))



(ws/defcard chrome-extension-card
  (ct.react/react-card
   counter
   (element "div" {}
            (element "div" {:className "font-black"} "Install the chrome extension and the open datalog panel. It should connect to the running datascript DB in this card.")
            (str "DB Requested " @counter " times.")))
  #_(do
      (js/console.log "Install chrome extension and open datalog panel")
    ;; (js/chrome.runtime.connect #js {:name "datalog-console-remote"})
      #_(ct.react/react-card
         (element "div" {:className "font-black"} "Install the chrome extension and the open datalog panel. It should connect to the running datascript DB in this card."))))