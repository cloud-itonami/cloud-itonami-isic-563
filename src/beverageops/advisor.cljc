(ns beverageops.advisor)

;;; Deterministic proposal confidence scoring
(defprotocol IAdvisor
  (confidence-score [advisor proposal]))

(defrecord DemoAdvisor []
  IAdvisor
  (confidence-score [_ proposal]
    (case (:op proposal)
      :schedule-table-reservation 0.95
      :coordinate-order-status-update 0.92
      :coordinate-supply-request 0.88
      :schedule-staff-shift-proposal 0.85
      :flag-safety-concern 0.99
      0.5)))

(defn create-advisor []
  (DemoAdvisor.))

(defn score-proposal [advisor proposal]
  (confidence-score advisor proposal))
