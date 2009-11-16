(ns com.mmazur.dynclj.test.dynclj
  (:use [com.mmazur.dynclj.dynclj] :reload)
  (:use [clojure.test]))

(def cache-test-filename "/tmp/dynclj-test.cache")

(def cache-with-one-entry {:host "test.dyndns.org" :ip "100.99.88.77" :date "Mon, 16 Nov 2009 11:22:52 SGT"})
(def cache-with-one-entry-string "{:host \"test.dyndns.org\", :ip \"100.99.88.77\", :date \"Mon, 16 Nov 2009 11:22:52 SGT\"}\n")

(deftest test-update-needed?
  (testing "IP is the same and last update was less than 28 days ago"
    (is (= false
           (update-needed? {:ip "220.255.77.147" :date "Sun, 01 Nov 2009 21:04:47 SGT"}
                           {:ip "220.255.77.147" :date "Sat, 30 Oct 2009 04:54:23 SGT"}))))
  (testing "IP is the same but last update was more than 28 days ago"
    (is (= true
           (update-needed? {:ip "220.255.77.147" :date "Sun, 29 Nov 2009 21:04:47 SGT"}
                           {:ip "220.255.77.147" :date "Sun, 01 Nov 2009 21:04:23 SGT"}))))
  (testing "IP is different"
    (is (= true
           (update-needed? {:ip "220.255.77.147" :date "Sun, 01 Nov 2009 21:04:47 SGT"}
                           {:ip "220.255.88.158" :date "Sat, 30 Oct 2009 04:54:23 SGT"})))))

(deftest test-write-cache
  (testing "Cache contains one entry"
    (is (= cache-with-one-entry-string
           (do
             (write-cache cache-with-one-entry cache-test-filename)
             (slurp cache-test-filename)))))
  (testing
    (binding [*cache-file-name* cache-test-filename]
      (is (= cache-with-one-entry-string
             (do
               (write-cache cache-with-one-entry)
               (slurp cache-test-filename)))))))


(defn my-run-tests
   []
   (binding [*test-out* *out*] (run-tests)))

(my-run-tests)
