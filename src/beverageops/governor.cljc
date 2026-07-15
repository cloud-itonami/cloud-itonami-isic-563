(ns beverageops.governor
  (:require [beverageops.store :as store]))

;;; Three HARD, un-overridable Governor checks

(defn- table-verified? [store table-id]
  "Check 1: Table must exist AND be :registered? AND :verified?"
  (when-let [table (store/table-by-id store table-id)]
    (and (:registered? table) (:verified? table))))

(defn- effect-is-propose? [proposal]
  "Check 2: Effect must be :propose"
  (= (:effect proposal) :propose))

(defn- scope-excluded? [proposal]
  "Check 3: Scope exclusion — block age-verification, responsible-service, recipe, alcohol decisions
   EN+JA substring matching combined into single explicit boolean return value"
  (let [proposal-str (str proposal)
        blocked-patterns
        ["age-verification" "ID-check" "ID-checking" "id-checking" "age verify"
         "responsible-service" "responsible service" "alcohol-service" "alcohol service"
         "recipe" "drink-recipe" "drink content" "beverage recipe"
         "alcohol-inventory" "alcohol ordering" "alcohol purchase"
         "年齢確認" "未成年" "飲酒責任" "調理" "アルコール" "年齢"
         "ID確認" "本人確認"]]
    ;; Special case: flag-safety-concern is allowed even if it mentions safety
    (and (not= (:op proposal) :flag-safety-concern)
         (some #(.contains (.toLowerCase proposal-str) (.toLowerCase %)) blocked-patterns))))

(defprotocol IGovernor
  (evaluate [governor store proposal]))

(defrecord HardGovernor []
  IGovernor
  (evaluate [_ store proposal]
    (let [table-id (:table-id proposal)
          table-ops #{:schedule-table-reservation :coordinate-order-status-update}
          facility-ops #{:coordinate-supply-request :schedule-staff-shift-proposal :flag-safety-concern}
          op (:op proposal)]
      (cond
        ;; Check 2: Effect not :propose
        (not (effect-is-propose? proposal))
        {:decision :rejected :reason "effect-not-propose"}

        ;; Check 3: Scope exclusion
        (scope-excluded? proposal)
        {:decision :rejected :reason "scope-excluded"}

        ;; Check 1: Table verification (only for table-specific ops)
        (and (table-ops op) (not (table-verified? store table-id)))
        {:decision :rejected :reason "table-unverified"}

        ;; Check 1: Facility ops don't require table verification
        (facility-ops op)
        {:decision :accepted :reason "facility-operation-allowed"}

        ;; All table-specific ops passed checks
        (table-ops op)
        {:decision :accepted :reason "table-operation-verified"}

        :else
        {:decision :rejected :reason "operation-unknown"}))))

(defn create-governor []
  (HardGovernor.))

(defn evaluate-proposal [governor store proposal]
  (evaluate governor store proposal))
