(ns medicaldevice.advisor
  "Medical Device Advisor -- the LLM node that proposes batch intake, maintenance
  scheduling, safety deviation escalations, and device-release reviews. The advisor
  is sealed into a single graph node and never sees governance logic or compliance
  boundaries (those are governor's domain). The proposal is ALWAYS routed through
  the Governor before committing."
  (:require [medicaldevice.facts :as facts]))

;; ======================= Advisor protocol =======================

(defprotocol Advisor
  "LLM advisor interface for medical device manufacturing operations."
  (-advise [this store request]
    "Propose an action based on request and current store state.
    Returns a proposal map: {:confidence, :effect, :value, :summary, :cites}"))

;; ======================= Spec basis =======================

(defn- op-scope
  "The `medicaldevice.facts/valid-operation-scopes` entry declaring `op`."
  [op]
  (first (filter (fn [scope] (= op (:op scope)))
                 (vals facts/valid-operation-scopes))))

(defn cites-for
  "The regulatory citations this repo's OWN facts table attaches to `op`.

  The Governor hard-blocks any proposal for a known op that cites no
  regulatory requirement, so a proposal has to carry its spec basis --
  and that basis is data this repo already declares, never a literal
  written here. `:safety/flag-deviation` is the one op whose
  `valid-operation-scopes` entry declares `:requires []`; its
  requirement is `facts/safety-deviation-escalation-required`, which the
  entry points at with `:escalation :mandatory`.

  An op this repo does not declare gets no citations."
  [op]
  (let [scope (op-scope op)
        requirements (or (seq (:requires scope))
                         (when (= :mandatory (:escalation scope))
                           [facts/safety-deviation-escalation-required]))]
    (vec (distinct (mapcat :citations requirements)))))

;; ======================= Mock advisor (testing) =======================

(deftype MockAdvisor []
  Advisor
  (-advise [_this _store request]
    ;; Echoes the request's own `:confidence` when the caller supplies one --
    ;; the same passthrough as `:value` -- so a demo or test can drive the
    ;; Governor's confidence floor deterministically without a live LLM.
    {:confidence (:confidence request 0.8)
     :effect :propose
     :value (:value request)
     :summary (str "Mock proposal for op " (:op request))
     :cites (cites-for (:op request))}))

(defn mock-advisor []
  (->MockAdvisor))

;; ======================= Trace for audit ledger =======================

(defn trace
  "Create an audit ledger entry for advisor proposal."
  [request proposal]
  {:t :advisor-proposed
   :op (:op request)
   :subject (:subject request)
   :confidence (:confidence proposal)
   :summary (:summary proposal)})
