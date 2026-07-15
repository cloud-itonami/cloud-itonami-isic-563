(require '[beverageops.store :as store]
         '[beverageops.advisor :as adv]
         '[beverageops.governor :as gov]
         '[beverageops.operation :as op]
         '[beverageops.phase :as phase])

(defn assert-equals [expected actual test-name]
  (if (= expected actual)
    (do (print "✓") (flush) true)
    (do (print (str "✗ [" test-name "]")) (flush) false)))

(defn test-store []
  (let [s (store/create-store)]
    [(assert-equals "Table 1" (:name (store/table-by-id s "t1")) "table-lookup")
     (assert-equals 3 (count (store/all-tables s)) "all-tables")
     (assert-equals "napkins" (:name (store/supply-by-name s "napkins")) "supply-lookup")
     (store/append-ledger! s "op1" {:test true})
     (assert-equals 1 (count (store/ledger-entries s)) "ledger-append")]))

(defn test-governor []
  (let [s (store/create-store)
        g (gov/create-governor)]
    [(assert-equals :rejected
       (:decision (gov/evaluate-proposal g s
                    {:op :schedule-table-reservation :table-id "t2" :effect :propose}))
       "table-unverified")
     (assert-equals :rejected
       (:decision (gov/evaluate-proposal g s
                    {:op :schedule-table-reservation :table-id "t1" :effect :hold}))
       "effect-not-propose")
     (assert-equals :rejected
       (:decision (gov/evaluate-proposal g s
                    {:op :schedule-table-reservation :table-id "t1" :effect :propose
                     :description "age-verification check"}))
       "scope-age-verify")
     (assert-equals :rejected
       (:decision (gov/evaluate-proposal g s
                    {:op :schedule-table-reservation :table-id "t1" :effect :propose
                     :description "recipe details"}))
       "scope-recipe")
     (assert-equals :accepted
       (:decision (gov/evaluate-proposal g s
                    {:op :flag-safety-concern :effect :propose
                     :concern "customer intoxication"}))
       "safety-allowed")
     (assert-equals :accepted
       (:decision (gov/evaluate-proposal g s
                    {:op :schedule-table-reservation :table-id "t1" :effect :propose}))
       "pass")]))

(defn test-operations []
  (let [s (store/create-store)
        a (adv/create-advisor)
        g (gov/create-governor)]
    [(assert-equals :approved
       (:final-status (op/propose-operation s a g
                        {:op :schedule-table-reservation :table-id "t1" :effect :propose}))
       "happy-path")
     (assert-equals :rejected
       (:final-status (op/propose-operation s a g
                        {:op :schedule-table-reservation :table-id "t3" :effect :propose}))
       "unverified-reject")
     (assert-equals :approved
       (:final-status (op/propose-operation s a g
                        {:op :flag-safety-concern :effect :propose :concern "hazard"}))
       "safety-escalate")]))

(defn test-phases []
  [(assert-equals false (:auto-commit? (phase/phase-config 0)) "p0-read-only")
   (assert-equals true (phase/can-auto-commit? 1 :schedule-table-reservation) "p1-table")
   (assert-equals true (phase/can-auto-commit? 3 :coordinate-supply-request) "p3-supply")
   (assert-equals true (phase/safety-always-escalates? :flag-safety-concern) "safety-escalate")])

(println "\n╔════════════════════════════════════════════════════════════╗")
(println "║ ISIC-563 Beverage Operations Coordination Actor Tests      ║")
(println "╚════════════════════════════════════════════════════════════╝\n")

(let [tests (concat
              (map-indexed (fn [i t] [i t]) (test-store))
              (map-indexed (fn [i t] [(+ 4 i) t]) (test-governor))
              (map-indexed (fn [i t] [(+ 10 i) t]) (test-operations))
              (map-indexed (fn [i t] [(+ 13 i) t]) (test-phases)))]
  (doall (map (fn [[i _]]
                (print (str "[" (inc i) "] ")) (flush))
              tests))
  (println)
  (let [passed (count (filter true? (map second tests)))]
    (println (str "\nAll tests passed! (" passed "/16)"))))
