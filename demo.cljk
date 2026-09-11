(require '[beverageops.store :as store]
         '[beverageops.advisor :as adv]
         '[beverageops.governor :as gov]
         '[beverageops.operation :as op])

(let [s (store/create-store)
      a (adv/create-advisor)
      g (gov/create-governor)]
  (println "\n╔════════════════════════════════════════════════════════════╗")
  (println "║ ISIC-563 Beverage-Service Administrative Coordination Sim  ║")
  (println "╚════════════════════════════════════════════════════════════╝\n")

  (print "[1] Table Reservation ... ")
  (flush)
  (op/propose-operation s a g
    {:op :schedule-table-reservation :table-id "t1" :effect :propose :time-slot "18:00"})
  (println "✓")

  (print "[2] Order Status Update ... ")
  (flush)
  (op/propose-operation s a g
    {:op :coordinate-order-status-update :table-id "t1" :effect :propose :order-id "o123" :status "ready"})
  (println "✓")

  (print "[3] Supply Request ... ")
  (flush)
  (op/propose-operation s a g
    {:op :coordinate-supply-request :effect :propose :supply-name "napkins" :quantity 100})
  (println "✓")

  (print "[4] Staff Shift Proposal ... ")
  (flush)
  (op/propose-operation s a g
    {:op :schedule-staff-shift-proposal :effect :propose :staff-id "s1" :shift "evening"})
  (println "✓")

  (print "[5] Safety Concern Escalation ... ")
  (flush)
  (op/propose-operation s a g
    {:op :flag-safety-concern :effect :propose :concern "customer intoxication" :severity "moderate"})
  (println "✓")

  (println)
  (println "Demo complete. Ledger entries:")
  (let [entries (store/ledger-entries s)]
    (println (str "  Total operations: " (count entries))))
  (println))
