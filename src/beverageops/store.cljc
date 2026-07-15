(ns beverageops.store
  (:require #?@(:clj [[clojure.edn :as edn]]
                 :cljs [[clojure.edn :as edn]]))
  #?(:clj (:import (java.time ZonedDateTime))))

;;; STORE protocol & MemStore implementation
(defprotocol IStore
  (table-by-id [store id])
  (all-tables [store])
  (supply-by-name [store name])
  (all-supplies [store])
  (append-ledger! [store op-id operation])
  (ledger-entries [store]))

(defrecord MemStore [tables supplies ledger]
  IStore
  (table-by-id [_ id] (get @tables id))
  (all-tables [_] (vals @tables))
  (supply-by-name [_ name] (get @supplies name))
  (all-supplies [_] (vals @supplies))
  (append-ledger! [_ op-id operation]
    (swap! ledger conj {:op-id op-id :op operation :ts #?(:clj (System/currentTimeMillis)
                                                            :cljs (js/Date.now))}))
  (ledger-entries [_] @ledger))

(defn create-store []
  (MemStore.
    (atom {;; Table directory: id -> {:id :name :seats :registered? :verified?}
           "t1" {:id "t1" :name "Table 1" :seats 4 :registered? true :verified? true}
           "t2" {:id "t2" :name "Table 2" :seats 2 :registered? true :verified? false}
           "t3" {:id "t3" :name "Table 3" :seats 6 :registered? false :verified? false}})
    (atom {;; Supply inventory: name -> {:name :stock :unit}
           "napkins" {:name "napkins" :stock 500 :unit "units"}
           "glassware" {:name "glassware" :stock 200 :unit "pieces"}
           "cleaning-supplies" {:name "cleaning-supplies" :stock 50 :unit "units"}})
    (atom [])))

;;; Demo tables & supplies
(def demo-tables
  [{"t1" {:id "t1" :name "Table 1" :seats 4 :registered? true :verified? true}}
   {"t2" {:id "t2" :name "Table 2" :seats 2 :registered? true :verified? false}}
   {"t3" {:id "t3" :name "Table 3" :seats 6 :registered? false :verified? false}}])

(def demo-supplies
  [{"napkins" {:name "napkins" :stock 500 :unit "units"}}
   {"glassware" {:name "glassware" :stock 200 :unit "pieces"}}
   {"cleaning-supplies" {:name "cleaning-supplies" :stock 50 :unit "units"}}])
