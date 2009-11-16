(ns com.mmazur.dynclj.test.dynclj
  (:use [com.mmazur.dynclj.dynclj] :reload)
  (:use [clojure.test]))

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

(defn my-run-tests
   []
   (binding [*test-out* *out*] (run-tests)))

(my-run-tests)
