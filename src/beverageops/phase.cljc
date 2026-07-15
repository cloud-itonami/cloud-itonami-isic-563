(ns beverageops.phase)

;;; Phase progression (0-3) with auto-commit gates

(def phases
  {0 {:name "read-only" :auto-commit? false :allowed-ops #{}}
   1 {:name "table+order" :auto-commit? true
      :allowed-ops #{:schedule-table-reservation :coordinate-order-status-update}}
   2 {:name "table+order+supply+shift" :auto-commit? true
      :allowed-ops #{:schedule-table-reservation :coordinate-order-status-update
                     :coordinate-supply-request :schedule-staff-shift-proposal}}
   3 {:name "all-non-safety" :auto-commit? true
      :allowed-ops #{:schedule-table-reservation :coordinate-order-status-update
                     :coordinate-supply-request :schedule-staff-shift-proposal}}})

(defn phase-config [phase-num]
  (get phases phase-num))

(defn can-auto-commit? [phase-num op-type]
  "Check if operation type can auto-commit in given phase"
  (let [phase-info (phase-config phase-num)]
    (and (:auto-commit? phase-info)
         (contains? (:allowed-ops phase-info) op-type))))

(defn safety-always-escalates? [op-type]
  "Safety concerns always escalate regardless of phase"
  (= op-type :flag-safety-concern))
