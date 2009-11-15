; TODO
; - store the last updated IP and date of last update to disk
; - read last updated IP and last update date from disk
; - if update necessary, perform it
; - process return codes
; - add logging
; - if no file found, `host <ip>` to check current setting

;(require 'clojure.contrib.str-utils)
(ns com.mmazur.dynclj.dynclj
  (:gen-class)
  (:use [clojure.contrib.base64 :only [encode-str]]
        [clojure.http.client :only [request]])
  (:import (java.io BufferedReader InputStreamReader)
           (java.util Date Calendar)))

(defn cmd [p] (.. Runtime getRuntime (exec (str p))))

(defn cmdout [o]
  (let [r (BufferedReader.
            (InputStreamReader.
              (.getInputStream o)))]
    (first (line-seq r))))

; from http://www.dyndns.com/services/dns/dyndns/readme.html#abuse
(def days-between-nochg-updates 28)

(def date-format "yyyy-MM-dd HH:mm:ss z")

(defn now [] (Date.))

(defn serialize-date [d]
  (.format (java.text.SimpleDateFormat. date-format) d))

(defn deserialize-date [s]
  (.parse (java.text.SimpleDateFormat. date-format) s))

;(def current-actual-ip (cmdout (cmd "./get_ip_address.sh")))
;(def now-status {:ip current-actual-ip :date (serialize-date (now))})

(defn nochg-update-ok? [current last-update]
  (let [cal (Calendar/getInstance)]
    (.setTime cal (deserialize-date last-update))
    (.add cal Calendar/DATE days-between-nochg-updates)
    (.after (deserialize-date current) (.getTime cal))))

(defn update-needed? [current last-update]
  (or
    (not (= (:ip current) (:ip last-update)))
    (nochg-update-ok? (:date current) (:date last-update))))

;(def current-configured-ip (last (clojure.contrib.str-utils/re-split #" " (cmdout (cmd "host bt.selfip.com")))))
;(println current-configured-ip)
;(println (= current-actual-ip current-configured-ip))

(def username "test")
(def password "test")
(def user-pass-base64-encoded (encode-str (apply str (concat username ":" password))))
(def update-url "https://members.dyndns.org/nic/update?hostname=test.dyndns.org&myip=110.24.1.55")
(def additional-headers {"Authorization" (apply str (concat "Basic " user-pass-base64-encoded))})

;(def response (request update-url "GET" additional-headers))
;(defn -main [& args] (println "application works" (:body-seq response)))
;(:body-seq response)
