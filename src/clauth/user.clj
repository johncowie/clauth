(ns clauth.user
  (:require [clauth.store :as store])
  (:import [org.mindrot.jbcrypt BCrypt]))

(defn bcrypt
  "Perform BCrypt hash of password"
  [password]
  (BCrypt/hashpw password (BCrypt/gensalt)))

(defn valid-password?
  "Verify that candidate password matches the hashed bcrypted password"
  [candidate hashed]
  (BCrypt/checkpw candidate hashed))

(defn new-user
  "Create new user record"
  ([attrs] ; Swiss army constructor. There must be a better way.
     (if attrs
       (if (:encrypt-password attrs)
         (assoc (dissoc attrs :encrypt-password)
           :password (bcrypt (:encrypt-password attrs)))
         attrs)))
  
  ([login password] (new-user login password nil nil))
  ([login password name url] (new-user { :login login :encrypt-password password :name name :url url})))

(defn reset-user-store!
  "mainly for used in testing. Clears out all users."
  [user-store]
  (store/reset-store! user-store))

(defn fetch-user
  "Find user based on login"
  [user-store t]
  (new-user (store/fetch user-store t)))

(defn store-user
  "Store the given User and return it."
  [user-store t]
  (store/store! user-store :login t))

(defn users
  "Sequence of users"
  [user-store]
  (store/entries user-store))

(defn register-user
  "create a unique user and store it in the user store"
  ([user-store attrs]
     (store-user user-store (new-user attrs)))
  ([user-store login password] (register-user user-store login password nil nil))
  ([user-store login password name url]
     (register-user user-store (new-user login password name url))))

(defn authenticate-user
  "authenticate user application using login and password"
  [user-store login password]
  (if-let [user (fetch-user user-store login)]
    (if (valid-password? password (:password user))
      user)))
