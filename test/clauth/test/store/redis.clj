(ns clauth.test.store.redis
  (:require [clojure.test :refer :all]
            [clauth.store.redis :as base]
            [clauth
             [store :as store]
             [token :as token]
             [client :as client]
             [user :as user]]
            [taoensso.carmine :as car :refer (wcar)]))

(def server-conn
  {:pool {}
   :spec {:host "127.0.0.1"
          :port 6379
          :db 15}})

(deftest redis-store-implementation
    (let [st (base/create-redis-store "testing" server-conn)]
      (store/reset-store! st)
      (is (= 0 (count (store/entries st))))
      (is (= [] (store/entries st)))
      (is (nil? (store/fetch st "item")))
      (let [item (store/store! st :key {:key "item" :hello "world"})]
        (is (= 1 (count (store/entries st))))
        (is (= item (store/fetch st "item")))
        (is (= [item] (store/entries st)))
        (let [_ (store/revoke! st "item")]
          (is (nil? (store/fetch st "item"))))
        (do (store/reset-store! st)
            (is (= 0 (count (store/entries st))))
            (is (= [] (store/entries st)))
            (is (nil? (store/fetch st "item")))))))

(deftest token-store-implementation
  (let [store (base/create-redis-store "tokens" server-conn)]
    (token/reset-token-store! store)
    (is (= 0 (count (token/tokens store))) "starts out empty")
    (let
      [record (token/oauth-token "my-client" "my-user")]
      (is (nil? (token/fetch-token store (:token record))))
      (do (token/store-token store record)
          (is (= record (token/fetch-token store (:token record))))
          (is (= 1 (count (token/tokens store))) "added one")))
    (token/reset-token-store! store)))

(deftest client-store-implementation
    (reset! client/client-store (base/create-redis-store "clients" server-conn))
    (client/reset-client-store!)
    (is (= 0 (count (client/clients))) "starts out empty")
    (let [record (client/client-app)]
      (is (nil? (client/fetch-client (:client-id record))))
      (do (client/store-client record)
          (is (= record (client/fetch-client (:client-id record))))
          (is (= 1 (count (client/clients))) "added one"))))
  (reset! client/client-store (store/create-memory-store))

(deftest user-store-implementation
  (let [user-store (base/create-redis-store "users" server-conn)]
    (user/reset-user-store! user-store)
    (is (= 0 (count (user/users user-store))) "starts out empty")
    (let [record (user/new-user "john@example.com" "password")]
      (is (nil? (user/fetch-user user-store "john@example.com")))
      (do (user/store-user user-store record)
          (is (= record (user/fetch-user user-store "john@example.com")))
          (is (= 1 (count (user/users user-store))) "added one")))
    (user/reset-user-store! user-store)))
