(ns com.mmazur.dynclj.dynclj
  (:gen-class)
  (:use [clojure.contrib.base64 :only [encode-str]]
        [clojure.contrib.duck-streams :only [file-str writer]]
        [clojure.http.client :only [request]])
  (:import (java.util Date Calendar)))

(def *cache-file-name* "/tmp/dynclj.cache")

; from http://www.dyndns.com/services/dns/dyndns/readme.html#abuse
(def *days-between-nochg-updates* 28)

; Format: Mon, 16 Nov 2009 03:00:26 GMT
(def *date-format* "E, d MMM yyyy HH:mm:ss z")

(defn now [] (Date.))

(defn serialize-date [d]
  (.format (java.text.SimpleDateFormat. *date-format*) d))

(defn deserialize-date [s]
  (.parse (java.text.SimpleDateFormat. *date-format*) s))

(defn nochg-update-ok? [current last-update]
  (let [cal (Calendar/getInstance)]
    (.setTime cal (deserialize-date last-update))
    (.add cal Calendar/DATE *days-between-nochg-updates*)
    (.after (deserialize-date current) (.getTime cal))))

(defn update-needed? [current last-update]
  (or
    (not (= (:ip current) (:ip last-update)))
    (nochg-update-ok? (:date current) (:date last-update))))

(defn get-current-ip-address-from-dyndns []
  (let [response (request "http://checkip.dyndns.org/")
        response-body (first (:body-seq response))]
    (re-find #"\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}" response-body)))

; use read-string to instantiate the data structure
(defn write-cache
  ([cache] write-cache cache *cache-file-name*)
  ([cache filename]
   (with-open [cache-file (writer (file-str filename))]
     (doall
       (for [cache-item cache]
         (.write cache-file (str cache-item "\n")))))))

(def username "test")
(def password "test")
(def user-pass-base64-encoded (encode-str (apply str (concat username ":" password))))
(def update-url "https://members.dyndns.org/nic/update?hostname=test.dyndns.org&myip=110.24.1.55")
(def additional-headers {"Authorization" (apply str (concat "Basic " user-pass-base64-encoded))})

;(def response (request update-url "GET" additional-headers))
;(:body-seq response)
;(defn -main [& args] (println "application works" (:body-seq response)))

;(defn -main [& args]
;  (get-current-ip)
;  (read-cache)
;  (determine-whether-to-update)
;  (perform-update)
;  (handle-return-code)
;  (write-cache))
