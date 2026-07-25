(ns beverageops.test
  (:require [beverageops.store :as store]
            [beverageops.advisor :as adv]
            [beverageops.governor :as gov]
            [beverageops.operation :as op]
            [beverageops.phase :as phase]))

(defn assert-equals [expected actual test-name]
  (if (= expected actual)
    (do (print "✓") (flush) true)
    (do (print (str "✗ [" test-name "]")) (flush) false)))

(defn test-store []
  (let [store (store/create-store)]
    ;; Setup, not an assertion. This call used to sit INSIDE the vector below,
    ;; where it contributed a non-boolean element that could never count as a
    ;; pass -- invisible while the summary hardcoded its own total.
    (store/append-ledger! store "op1" {:test true})
    [(assert-equals "Table 1" (:name (store/table-by-id store "t1")) "table-lookup")
     (assert-equals 3 (count (store/all-tables store)) "all-tables")
     (assert-equals "napkins" (:name (store/supply-by-name store "napkins")) "supply-lookup")
     (assert-equals 1 (count (store/ledger-entries store)) "ledger-append")]))

(defn test-governor []
  (let [store (store/create-store)
        governor (gov/create-governor)]
    [(assert-equals :rejected
       (:decision (gov/evaluate-proposal governor store
                    {:op :schedule-table-reservation :table-id "t2" :effect :propose}))
       "table-unverified-check")
     (assert-equals :rejected
       (:decision (gov/evaluate-proposal governor store
                    {:op :schedule-table-reservation :table-id "t1" :effect :hold}))
       "effect-not-propose-check")
     (assert-equals :rejected
       (:decision (gov/evaluate-proposal governor store
                    {:op :schedule-table-reservation :table-id "t1" :effect :propose
                     :description "food-safety decision"}))
       "scope-exclusion-food-safety")
     (assert-equals :rejected
       (:decision (gov/evaluate-proposal governor store
                    {:op :schedule-table-reservation :table-id "t1" :effect :propose
                     :description "recipe details"}))
       "scope-exclusion-recipe")
     (assert-equals :accepted
       (:decision (gov/evaluate-proposal governor store
                    {:op :flag-safety-concern :effect :propose
                     :concern "customer intoxication"}))
       "flag-safety-allowed")
     (assert-equals :accepted
       (:decision (gov/evaluate-proposal governor store
                    {:op :schedule-table-reservation :table-id "t1" :effect :propose}))
       "governor-full-pass")]))

(defn test-operations []
  (let [store (store/create-store)
        advisor (adv/create-advisor)
        governor (gov/create-governor)]
    [(assert-equals :approved
       (:final-status (op/propose-operation store advisor governor
                        {:op :schedule-table-reservation :table-id "t1" :effect :propose}))
       "operation-happy-path")
     (assert-equals :rejected
       (:final-status (op/propose-operation store advisor governor
                        {:op :schedule-table-reservation :table-id "t3" :effect :propose}))
       "operation-unverified-rejection")
     (assert-equals :approved
       (:final-status (op/propose-operation store advisor governor
                        {:op :flag-safety-concern :effect :propose :concern "hazard"}))
       "operation-safety-escalation")]))

(defn test-phases []
  (let [p0 (phase/phase-config 0)
        p1 (phase/phase-config 1)
        p3 (phase/phase-config 3)]
    [(assert-equals false (:auto-commit? p0) "phase-0-read-only")
     (assert-equals true (phase/can-auto-commit? 1 :schedule-table-reservation) "phase-1-table-reservation")
     (assert-equals true (phase/can-auto-commit? 3 :coordinate-supply-request) "phase-3-supply")
     (assert-equals true (phase/safety-always-escalates? :flag-safety-concern) "safety-always-escalates")]))

(defn run-tests []
  (println "\n╔════════════════════════════════════════════════════════════╗")
  (println "║ ISIC-563 Beverage Operations Coordination Actor Tests      ║")
  (println "╚════════════════════════════════════════════════════════════╝\n")
  ;; Indices are derived, not hardcoded: the previous +4/+10/+13 offsets no
  ;; longer matched the section sizes, so the printed numbers overlapped.
  (let [tests (map-indexed (fn [i t] [i t])
                           (concat (test-store) (test-governor)
                                   (test-operations) (test-phases)))]
    (doall (map (fn [[i result]]
                  (print (str "[" (inc i) "] ")) (flush))
                tests))
    (println)
    ;; The summary used to print "All tests passed!" unconditionally, with the
    ;; count interpolated -- so a 10/16 run announced success. It also compared
    ;; against a hardcoded 16 that nothing kept in step with the actual tests.
    ;; Both fixed: the total is derived, and the wording follows the result.
    (let [total (count tests)
          passed (count (filter true? (map second tests)))
          ok? (and (pos? total) (= passed total))]
      (println (str "\n" (if ok? "All tests passed!" "TESTS FAILED")
                    " (" passed "/" total ")"))
      ok?)))
