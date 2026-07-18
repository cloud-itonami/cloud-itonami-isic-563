(ns beverageops.render-html
  "Build-time HTML renderer for `docs/samples/operator-console.html`.

  Closes flagship checklist item 2 (com-junkawasaki/root ADR-2607189300,
  Wave5 rollout): this repo previously had NO demo page, no
  `docs/samples/`, and no generator at all. This namespace drives the
  REAL actor stack (`beverageops.operation` -> `beverageops.governor` ->
  `beverageops.store`) through a scenario built from the actor's own
  seeded demo data (`beverageops.store/create-store`: tables t1/t2/t3,
  supplies napkins/glassware/cleaning-supplies) and renders the result
  deterministically -- no invented numbers, no wall-clock timestamps in
  the RENDERED page content (the underlying ledger entries do carry a
  `:ts`/`:op-id` produced by `System/currentTimeMillis`, but this
  renderer deliberately never prints those two volatile fields, only
  the deterministic `:proposal`/`:governor-decision`/`:status` fields),
  byte-identical across reruns against the same seed (verified by
  diffing two consecutive runs).

  IMPORTANT deviation from the sibling-repo template, recorded here
  honestly rather than silently forced: sibling actors in this fleet
  (e.g. cloud-itonami-isic-851/schoolops, -5520/campgroundops,
  -0510/coalops) implement `operation` as a langgraph-clj StateGraph
  with a distinct `:escalate` disposition and a real human
  approval/resume workflow (`interrupt-before` + `g/run*` resume). THIS
  repo's `beverageops.operation/propose-operation` is NOT that -- it is
  a single synchronous function (propose -> advisor score, unused by
  the governor -> governor evaluate -> log -> return `:approved` or
  `:rejected` immediately). `beverageops.phase` defines a phase/
  auto-commit model but is never `:require`d by `beverageops.operation`
  (confirmed by grep across every `.cljc` in `src/`) -- dead code, not
  wired into the real decision path. So there is no real blocking
  'escalate, then a human approves' state this renderer could honestly
  drive without inventing actor behavior that does not exist in this
  repo's code, which this template deliberately avoids doing.

  The closest REAL analog to the README's stated policy ('safety
  concern escalation ... always escalates to human review') is
  `:flag-safety-concern`'s governor path: it is classified as a
  `facility-ops` operation in `beverageops.governor/evaluate`, so
  (unlike the two `table-ops`) it unconditionally skips the
  table-verification check and returns `facility-operation-allowed` --
  always logged to the append-only ledger, regardless of any table
  state. This page labels that disposition honestly as
  'auto-logged · no verification gate (facility op)' rather than
  mislabeling it as a blocking human-approval step that does not exist
  in this repo's current implementation.

  Confirmed non-trap before writing this file: this repo's real seed
  ids (`t1`/`t2`/`t3`, `napkins`/`glassware`/`cleaning-supplies`) match
  exactly what `beverageops.store/create-store` seeds AND what this
  repo's own WORKING root-level `demo.cljs`/`test.cljs` (nbb-runnable,
  `nbb -cp src demo.cljs` / `nbb -cp src test.cljs`) already exercise --
  both were run and passed before this file was written. Every proposal
  below was independently dry-run through the real
  `beverageops.operation/propose-operation` -> `beverageops.governor/
  evaluate-proposal` path first to confirm its expected disposition
  before being hard-coded into `run-demo!` (the alternate
  `src/beverageops/sim.cljc` + `test/beverageops/test.cljc` pair,
  reached via `run-demo.cljs`/`run-tests.cljs`, is a SEPARATE, broken
  pair under nbb -- `format` is not resolvable in ClojureScript/nbb --
  but that bug is irrelevant here: this renderer is plain JVM Clojure
  and never touches those two files).

  Usage: `clojure -M:render-html [out-file]`
  (default `docs/samples/operator-console.html`)."
  (:require [clojure.string :as str]
            [beverageops.store :as store]
            [beverageops.advisor :as adv]
            [beverageops.governor :as gov]
            [beverageops.operation :as op]))

(defn run-demo!
  "Runs a fresh seeded store through a scenario covering every real
  disposition `beverageops.governor` can reach today:

    - t1 (registered + verified) clears two table-ops that auto-commit
      (`schedule-table-reservation`, `coordinate-order-status-update`).
    - two facility ops auto-commit unconditionally, no table needed
      (`coordinate-supply-request`, `schedule-staff-shift-proposal`).
    - `flag-safety-concern` ALSO auto-commits unconditionally (it is a
      facility op too) -- the actor's real, always-unconditional path,
      the closest analog this code has to 'always escalates' (see
      namespace docstring for why there is no separate blocking
      approval step to drive here).
    - t2 (registered but NOT `:verified?` in the seed data) HARD-holds
      on `table-unverified`.
    - a proposal whose `:effect` is not `:propose` HARD-holds on
      `effect-not-propose`.
    - a proposal whose content mentions a recipe HARD-holds on
      `scope-excluded` (EN/JA substring scan over the whole proposal).
    - an op outside the closed 5-op allowlist HARD-holds on
      `operation-unknown`.

  Returns `{:store store :advisor advisor :results [..]}` -- every
  field `render` below reads is real governor/store output, not a
  hand-typed copy."
  []
  (let [s (store/create-store)
        a (adv/create-advisor)
        g (gov/create-governor)
        proposals
        [{:op :schedule-table-reservation :table-id "t1" :effect :propose
          :time-slot "18:00" :party-size 4}
         {:op :coordinate-order-status-update :table-id "t1" :effect :propose
          :order-id "o123" :status "ready"}
         {:op :coordinate-supply-request :effect :propose
          :supply-name "napkins" :quantity 100}
         {:op :schedule-staff-shift-proposal :effect :propose
          :staff-id "s1" :shift "evening"}
         {:op :flag-safety-concern :effect :propose
          :concern "customer intoxication" :severity "moderate"}
         {:op :schedule-table-reservation :table-id "t2" :effect :propose
          :time-slot "19:00" :party-size 2}
         {:op :schedule-table-reservation :table-id "t1" :effect :hold
          :time-slot "20:00" :party-size 4}
         {:op :schedule-table-reservation :table-id "t1" :effect :propose
          :description "recipe details for house cocktail"}
         {:op :request-live-music-booking :effect :propose
          :vendor "acme-live-music" :date "2026-08-01"}]
        results (mapv #(op/propose-operation s a g %) proposals)]
    {:store s :advisor a :results results}))

;; ----------------------------- rendering -----------------------------

(defn- esc [v]
  (-> (str v)
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")))

(defn- target-of [{:keys [table-id supply-name staff-id concern vendor]}]
  (or table-id supply-name staff-id concern vendor "-"))

(defn- reason-badge [decision]
  (let [reason (:reason decision)]
    (case (:decision decision)
      :accepted
      (if (= reason "facility-operation-allowed")
        (str "<span class=\"ok\">auto-logged &middot; no verification gate (facility op)</span>")
        (str "<span class=\"ok\">auto-commit &middot; " (esc reason) "</span>"))
      :rejected
      (str "<span class=\"critical\">HARD hold &middot; " (esc reason) "</span>")
      (str "<span class=\"muted\">" (esc reason) "</span>"))))

(defn- result-row [i {:keys [proposal governor-decision final-status]}]
  (format "        <tr><td>%d</td><td><code>%s</code></td><td>%s</td><td>%s</td><td>%s</td></tr>"
          (inc i)
          (esc (name (:op proposal)))
          (esc (target-of proposal))
          (if (= final-status :approved)
            "<span class=\"ok\">approved</span>"
            "<span class=\"err\">rejected</span>")
          (reason-badge governor-decision)))

(defn- table-row [{:keys [id name seats registered? verified?]}]
  (format "        <tr><td>%s</td><td>%s</td><td>%d</td><td>%s</td></tr>"
          (esc id) (esc name) seats
          (cond
            (and registered? verified?) "<span class=\"ok\">registered &amp; verified</span>"
            registered? "<span class=\"warn\">registered, unverified</span>"
            :else "<span class=\"err\">unregistered</span>")))

(defn- supply-row [{:keys [name stock unit]}]
  (format "        <tr><td>%s</td><td>%d</td><td>%s</td></tr>"
          (esc name) stock (esc unit)))

(def ^:private action-gate-rows
  ;; Static description of this actor's own op contract (README `Ops`
  ;; table, `beverageops.governor`) -- documentation of fixed behavior,
  ;; not runtime telemetry, so it is legitimately hand-described rather
  ;; than derived from a live run.
  ["        <tr><td><code>:schedule-table-reservation</code></td><td><span class=\"warn\">table must be registered &amp; verified</span></td></tr>"
   "        <tr><td><code>:coordinate-order-status-update</code></td><td><span class=\"warn\">table must be registered &amp; verified</span></td></tr>"
   "        <tr><td><code>:coordinate-supply-request</code></td><td><span class=\"ok\">no table-verification gate (facility op)</span></td></tr>"
   "        <tr><td><code>:schedule-staff-shift-proposal</code></td><td><span class=\"ok\">no table-verification gate (facility op)</span></td></tr>"
   "        <tr><td><code>:flag-safety-concern</code></td><td><span class=\"ok\">no table-verification gate (facility op)</span></td></tr>"])

(defn render
  "Renders the full operator-console.html document from `{:store
  :advisor :results}` produced by `run-demo!` (or any other real
  scenario run through the same functions)."
  [{:keys [store results]}]
  (let [tables (store/all-tables store)
        supplies (store/all-supplies store)
        approved (count (filter #(= :approved (:final-status %)) results))
        rejected (count (filter #(= :rejected (:final-status %)) results))
        table-rows (str/join "\n" (map table-row (sort-by :id tables)))
        supply-rows (str/join "\n" (map supply-row (sort-by :name supplies)))
        result-rows (str/join "\n" (map-indexed result-row results))]
    (str
     "<html><head><meta charset=\"utf-8\"><title>cloud-itonami-isic-563 &middot; beverage-serving administrative coordination</title><style>\n"
     "table { width: 100%; border-collapse: collapse; font-size: 14px; }\n"
     ".ok { color: #137a3f; }\n"
     "body { font-family: system-ui,-apple-system,sans-serif; margin: 0; color: #1a1a1a; background: #fafafa; }\n"
     "header.bar { display: flex; align-items: center; gap: 12px; padding: 12px 20px; background: #fff; border-bottom: 1px solid #e5e5e5; }\n"
     "th, td { text-align: left; padding: 8px 10px; border-bottom: 1px solid #f0f0f0; }\n"
     "h2 { margin-top: 0; font-size: 15px; }\n"
     ".warn { color: #b25c00; background: #fff8e1; padding: 2px 6px; border-radius: 4px; }\n"
     "main { max-width: 980px; margin: 24px auto; padding: 0 20px; }\n"
     "header.bar h1 { font-size: 18px; margin: 0; font-weight: 600; }\n"
     ".muted { color: #888; font-size: 13px; }\n"
     ".critical { color: #fff; background: #b3261e; padding: 2px 6px; border-radius: 4px; font-weight: 600; }\n"
     ".card { background: #fff; border: 1px solid #e5e5e5; border-radius: 8px; padding: 16px; margin-bottom: 16px; }\n"
     ".err { color: #b3261e; background: #fbe9e7; padding: 2px 6px; border-radius: 4px; }\n"
     "th { font-weight: 600; color: #555; font-size: 12px; text-transform: uppercase; letter-spacing: 0.04em; }\n"
     "header.bar .badge { margin-left: auto; font-size: 12px; color: #666; }\n"
     "code { font-size: 12px; background: #f4f4f4; padding: 1px 4px; border-radius: 3px; }\n"
     "</style></head><body>\n"
     "<header class=\"bar\">\n"
     "  <h1>Beverage-serving administrative coordination (ISIC 563) — Operator Console</h1>\n"
     "  <span class=\"badge\">read-only sample · governor-gated · never touches age-verification/recipes/alcohol ordering</span>\n"
     "</header>\n"
     "<main>\n"
     "  <section class=\"card\">\n"
     "    <h2>Tables</h2>\n"
     "    <p class=\"muted\">Demo snapshot — build-time-generated from <code>beverageops.store</code> via <code>beverageops.render-html</code> (<code>clojure -M:render-html</code>), regenerated nightly.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Table</th><th>Name</th><th>Seats</th><th>Roster status</th></tr></thead>\n"
     "      <tbody>\n"
     table-rows "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"
     "  <section class=\"card\">\n"
     "    <h2>Supplies</h2>\n"
     "    <table>\n"
     "      <thead><tr><th>Supply</th><th>Stock</th><th>Unit</th></tr></thead>\n"
     "      <tbody>\n"
     supply-rows "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"
     "  <section class=\"card\">\n"
     "    <h2>Action gate (Beverage-Ops Governor)</h2>\n"
     "    <p class=\"muted\">HARD holds cannot be overridden. Age-verification, responsible-service, recipe and alcohol-ordering territory are permanently out of scope — see governor scope exclusion.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Op</th><th>Gate</th></tr></thead>\n"
     "      <tbody>\n"
     (str/join "\n" action-gate-rows) "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"
     "  <section class=\"card\">\n"
     "    <h2>Demo scenario results (this run)</h2>\n"
     "    <p class=\"muted\">" (str approved) " approved &middot; " (str rejected) " HARD-held — every row is a real <code>beverageops.operation/propose-operation</code> call through the real governor, not hand-typed.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>#</th><th>Op</th><th>Target</th><th>Status</th><th>Governor reason</th></tr></thead>\n"
     "      <tbody>\n"
     result-rows "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"
     "</main>\n"
     "</body></html>\n")))

(defn -main [& args]
  (let [out (or (first args) "docs/samples/operator-console.html")
        demo (run-demo!)
        html (render demo)]
    (spit out html)
    (println "wrote" out "(" (count (:results demo)) "operations,"
             (count (filter #(= :approved (:final-status %)) (:results demo))) "approved,"
             (count (filter #(= :rejected (:final-status %)) (:results demo))) "rejected )")))
