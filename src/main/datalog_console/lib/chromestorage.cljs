(ns datalog-console.lib.chromestorage
  {:no-doc true}
  (:require [goog.object :as gobj]))

(defn set-item!
  "Set `key` in browser's extension storage to `val`. Call optional function `f` when done."
  [k v]
  (js/chrome.storage.local.set (clj->js {(str k) v}) nil))

(defn get-item
  "Returns value of `key` from browser's extension storage. Call optional function `f` when done."
  [key cb]
  (js/chrome.storage.local.get (str key) cb))