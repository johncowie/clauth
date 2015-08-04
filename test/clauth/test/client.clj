(ns clauth.test.client
  (:require [clojure.test :refer :all]
            [clauth.client :as base]
            [clauth.store :as store]))

(def client-store (store/create-memory-store))

(deftest client-registration
  (base/reset-client-store! client-store)
  (let [record (base/register-client client-store "Super company inc" "http://example.com")
        client-id (:client-id record)
        client-secret (:client-secret record)]
    (is (= "Super company inc" (:name record))
        "should add extra attributes to client")
    (is (not (nil? client-id)) "should include client_id field")
    (is (not (nil? client-secret)) "should include client_secret field")
    (is (= 1 (count (base/clients client-store))) "added one")
    (is (= record (first (base/clients client-store))) "added one")
    (is (= record (base/authenticate-client client-store client-id client-secret))
        "should authenticate client")
    (is (nil? (base/authenticate-client client-store client-id "bad"))
        "should not authenticate client with wrong password")
    (is (nil? (base/authenticate-client client-store "idontexist" "bad"))
        "should not authenticate client with wrong id")))

(deftest client-store-implementation
  (base/reset-client-store! client-store)
  (is (= 0 (count (base/clients client-store))) "starts out empty")
  (let [record (base/client-app)]
    (is (nil? (base/fetch-client client-store (:client-id record))))
    (do (base/store-client client-store record)
        (is (= record (base/fetch-client client-store (:client-id record))))
        (is (= 1 (count (base/clients client-store))) "added one"))))
