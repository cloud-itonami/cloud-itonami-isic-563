(ns beverageops.governor
  (:require [beverageops.store :as store]))

;;; Three HARD, un-overridable Governor checks

(defn- table-verified?
  "Check 1: Table must exist AND be :registered? AND :verified?"
  [store table-id]
  (when-let [table (store/table-by-id store table-id)]
    (and (:registered? table) (:verified? table))))

(defn- effect-is-propose?
  "Check 2: Effect must be :propose"
  [proposal]
  (= (:effect proposal) :propose))

(defn- scope-excluded?
  "Check 3: Scope exclusion — block age-verification, responsible-service,
   recipe, alcohol AND food-safety decisions. EN+JA substring matching,
   combined into a single explicit boolean return value.

   The docstring used to sit AFTER the argument vector, making it a discarded
   string literal rather than documentation.

   food-safety was added 2026-07-25 to reconcile a real disagreement: the test
   `scope-exclusion-food-safety` asserted a food-safety description must be
   :rejected, but no food pattern existed here and README's \"Explicitly OUT of
   Scope\" list did not mention it either, so the assertion had been failing
   silently -- the suite hardcoded its own total and reported \"All tests
   passed!\" regardless. Reconciled toward the STRICTER reading (block it)
   rather than deleting the assertion, because dropping a test that asserts a
   HARD gate rejects something is the loosening direction. A beverage-serving
   coordinator making food-safety determinations is the same
   licensed-professional boundary the other exclusions here draw. README is
   updated to match, so the declared scope and the gate now agree."
  [proposal]
  (let [proposal-str (.toLowerCase (str proposal))
        blocked-patterns
        ["age-verification" "id-check" "id-checking" "age verify"
         "responsible-service" "responsible service" "alcohol-service" "alcohol service"
         "recipe" "drink-recipe" "drink content" "beverage recipe"
         "alcohol-inventory" "alcohol ordering" "alcohol purchase"
         "food-safety" "food safety" "haccp"
         "年齢確認" "未成年" "飲酒責任" "調理" "アルコール" "年齢"
         "id確認" "本人確認"]
        is-excluded (some (fn [pattern]
                           (>= (.indexOf proposal-str (.toLowerCase pattern)) 0))
                         blocked-patterns)]
    ;; Special case: flag-safety-concern is allowed even if it mentions safety
    (and (not= (:op proposal) :flag-safety-concern)
         is-excluded)))

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
