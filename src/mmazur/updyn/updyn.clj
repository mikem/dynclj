; TODO
; - store the last updated IP and date of last update to disk
; - read last updated IP and last update date from disk
; - if update necessary, perform it
; - process return codes
; - add logging
; - if no file found, `host <ip>` to check current setting

;(require 'clojure.contrib.str-utils)
(import '(java.io BufferedReader InputStreamReader))
(use 'clojure.contrib.base64)
(use 'clojure.http.client)

(defn cmd [p] (.. Runtime getRuntime (exec (str p))))

(defn cmdout [o]
  (let [r (BufferedReader.
            (InputStreamReader.
              (.getInputStream o)))]
    (first (line-seq r))))

; from http://www.dyndns.com/services/dns/dyndns/readme.html#abuse
(def days-between-nochg-updates 28)

(def date-format "yyyy-MM-dd HH:mm:ss z")

(defn now [] (java.util.Date.))

(defn serialize-date [d]
  (.format (java.text.SimpleDateFormat. date-format) d))

(defn deserialize-date [s]
  (.parse (java.text.SimpleDateFormat. date-format) s))

;(def current-actual-ip (cmdout (cmd "./get_ip_address.sh")))
;(def now-status {:ip current-actual-ip :date (serialize-date (now))})

(defn nochg-update-ok? [current last-update]
  (let [cal (java.util.Calendar/getInstance)]
    (.setTime cal (deserialize-date last-update))
    (.add cal java.util.Calendar/DATE days-between-nochg-updates)
    (.after (deserialize-date current) (.getTime cal))))

(defn update-needed? [current last-update]
  (or
    (not (= (:ip current) (:ip last-update)))
    (nochg-update-ok? (:date current) (:date last-update))))

;(def current-configured-ip (last (clojure.contrib.str-utils/re-split #" " (cmdout (cmd "host bt.selfip.com")))))
;(println current-configured-ip)
;(println (= current-actual-ip current-configured-ip))

; Tests
(use '[clojure.test])

(deftest test-update-needed?
  (testing "IP is the same and last update was less than 28 days ago"
    (is (= false
           (update-needed? {:ip "220.255.77.147" :date "2009-11-01 21:04:47 SGT"}
                           {:ip "220.255.77.147" :date "2009-10-30 04:54:23 SGT"}))))
  (testing "IP is the same but last update was more than 28 days ago"
  (is (= true
         (update-needed? {:ip "220.255.77.147" :date "2009-11-29 21:04:47 SGT"}
                         {:ip "220.255.77.147" :date "2009-11-01 21:04:23 SGT"}))))
  (testing "IP is different"
  (is (= true
         (update-needed? {:ip "220.255.77.147" :date "2009-11-01 21:04:47 SGT"}
                         {:ip "220.255.88.158" :date "2009-10-30 04:54:23 SGT"})))))

(defn my-run-tests
   []
   (binding [*test-out* *out*] (run-tests)))

(my-run-tests)

(def username "test")
(def password "test")
(def user-pass-base64-encoded (encode-str (apply str (concat username ":" password))))
(def update-url "http://members.dyndns.org/nic/update?hostname=test.dyndns.org&myip=110.24.1.55")
(def additional-headers {"Authorization" (apply str (concat "Basic " user-pass-base64-encoded))})

(def response (request update-url "GET" additional-headers))
(:body-seq response)
