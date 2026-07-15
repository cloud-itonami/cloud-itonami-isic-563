(ns beverageops.operation
  (:require [beverageops.store :as store]
            [beverageops.governor :as gov]
            [beverageops.advisor :as adv]))

;;; StateGraph-style operation orchestration (single-run, no internal loops)

(defn propose-operation [store advisor governor proposal]
  "Single operation: propose, score, evaluate, log"
  (let [op-id (str "op-" #?(:clj (System/currentTimeMillis)
                           :cljs (js/Date.now)))
        score (adv/score-proposal advisor proposal)
        decision (gov/evaluate-proposal governor store proposal)]
    (store/append-ledger! store op-id
      {:proposal proposal
       :advisor-score score
       :governor-decision decision
       :status (if (= (:decision decision) :accepted) :approved :rejected)})
    {:op-id op-id
     :proposal proposal
     :advisor-score score
     :governor-decision decision
     :final-status (if (= (:decision decision) :accepted) :approved :rejected)}))

;;; Demo operations
(defn demo-table-reservation [store advisor governor]
  (propose-operation store advisor governor
    {:op :schedule-table-reservation
     :table-id "t1"
     :effect :propose
     :time-slot "18:00"
     :party-size 4}))

(defn demo-order-status [store advisor governor]
  (propose-operation store advisor governor
    {:op :coordinate-order-status-update
     :table-id "t1"
     :effect :propose
     :order-id "o123"
     :status "ready"}))

(defn demo-supply-request [store advisor governor]
  (propose-operation store advisor governor
    {:op :coordinate-supply-request
     :effect :propose
     :supply-name "napkins"
     :quantity 100}))

(defn demo-shift-proposal [store advisor governor]
  (propose-operation store advisor governor
    {:op :schedule-staff-shift-proposal
     :effect :propose
     :staff-id "s1"
     :shift "evening"}))

(defn demo-safety-concern [store advisor governor]
  (propose-operation store advisor governor
    {:op :flag-safety-concern
     :effect :propose
     :concern "customer intoxication"
     :severity "moderate"}))
