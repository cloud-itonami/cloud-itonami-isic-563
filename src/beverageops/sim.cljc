(ns beverageops.sim
  (:require [beverageops.store :as store]
            [beverageops.advisor :as adv]
            [beverageops.governor :as gov]
            [beverageops.operation :as op]))

;;; Demo simulator: runs 5 scenarios offline

(defn run-scenario [name scenario-fn store advisor governor]
  (try
    (let [result (scenario-fn store advisor governor)]
      {:scenario name :status :complete :result result})
    #?(:clj (catch Exception e {:scenario name :status :error :error (.getMessage e)})
       :cljs (catch :default e {:scenario name :status :error :error (str e)}))))

(defn run-demo []
  (let [store (store/create-store)
        advisor (adv/create-advisor)
        governor (gov/create-governor)
        scenarios
        [["Table Reservation" op/demo-table-reservation]
         ["Order Status Update" op/demo-order-status]
         ["Supply Request" op/demo-supply-request]
         ["Staff Shift Proposal" op/demo-shift-proposal]
         ["Safety Concern Escalation" op/demo-safety-concern]]]
    (println "\n╔════════════════════════════════════════════════════════════╗")
    (println "║ ISIC-563 Beverage-Service Administrative Coordination Sim  ║")
    (println "╚════════════════════════════════════════════════════════════╝\n")
    (let [results (map-indexed
                    (fn [i [name fn]]
                      (print (format "[%d] %s ... " (inc i) name))
                      (flush)
                      (let [result (run-scenario name fn store advisor governor)]
                        (println (if (= (:status result) :complete) "✓" "✗"))
                        result))
                    scenarios)]
      (println)
      (println "Demo complete. Ledger entries:")
      (println (format "  Total operations: %d" (count (store/ledger-entries store))))
      results)))
