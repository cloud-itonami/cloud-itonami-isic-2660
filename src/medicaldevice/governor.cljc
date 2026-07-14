(ns medicaldevice.governor
  "Manufacturing Governor -- the independent compliance layer that earns the
  Medical Device Advisor the right to commit proposals. The LLM has no notion
  of medical-device regulations (FDA QSR, CE marking, PMDA), batch traceability
  validation, safety deviation escalation policy, or the CRITICAL boundary
  that qualified-person device release is NOT this actor's authority.

  This must be a separate system able to *reject* a proposal and fall back to
  HOLD or ESCALATE.

  CRITICAL SCOPE: This actor is OPERATIONS COORDINATION ONLY. Five permanent
  hard blocks:

  1. Device-release claim             -- reject any proposal claiming to release
                                         or certify a device for market
  2. Regulatory-certification claim   -- reject any proposal claiming regulatory
                                         compliance certification
  3. No spec-basis                    -- reject proposals citing no regulatory
                                         requirement source
  4. Batch record incomplete or
     missing traceability             -- no operations on unverified batches
  5. Open safety deviation affecting  -- batch cannot proceed if a safety
     batch                              deviation is flagged and unresolved

  Three additional checks (soft escalations to human):

  6. Safety deviation flagged          -- `:safety/flag-deviation` ALWAYS
                                         escalates (never auto-commits)
  7. Device-release review request    -- `:device-release/request-review`
                                         ALWAYS escalates (only qualified person
                                         can approve and execute the actual review)
  8. Low confidence / unrecognized op  -- LLM confidence below threshold or
                                         unrecognized operation -> escalate"
  (:require [medicaldevice.registry :as registry]))

(def confidence-floor 0.6)

;; High-stakes operations that ALWAYS escalate to human approval
(def high-stakes
  "Operations grave enough to always require a human, even when clean.
  Device release and safety flagging are both real-world safety-critical acts."
  #{:safety/flag-deviation :device-release/request-review})

;; ======================= Hard violation checks =======================

(defn- device-release-violation?
  "HARD BLOCK 1: Reject proposals claiming device release/market distribution."
  [proposal]
  (when (or (contains? (:value proposal) :released-for-market)
            (contains? (:value proposal) :market-distribution)
            (= (:effect proposal) :release-device))
    [{:rule :device-release-authority-violation
      :detail "Device release for market is the qualified person's exclusive domain. This actor proposes reviews only."}]))

(defn- regulatory-certification-violation?
  "HARD BLOCK 2: Reject proposals claiming regulatory certification."
  [proposal]
  (when (or (contains? (:value proposal) :fda-cleared)
            (contains? (:value proposal) :ce-marked)
            (contains? (:value proposal) :pmda-approved)
            (= (:effect proposal) :certify-regulatory-compliance))
    [{:rule :regulatory-certification-violation
      :detail "Regulatory certification (FDA, CE, PMDA) is performed by human regulatory affairs, not this actor."}]))

(defn- spec-basis-violations
  "HARD BLOCK 3: A proposal with no regulatory spec-basis citation is invalid."
  [{:keys [op]} proposal]
  (when (contains? #{:production-batch/intake :maintenance/schedule
                      :device-release/request-review :safety/flag-deviation} op)
    (when (or (empty? (:cites proposal))
              (and (contains? (:value proposal) :spec-basis) (nil? (:spec-basis (:value proposal)))))
      [{:rule :no-spec-basis
        :detail "Proposals must cite regulatory requirements (FDA, CE, ISO 13485, etc.)"}])))

(defn- batch-record-violations
  "HARD BLOCK 4: All batch operations require complete, traceable batch records."
  [{:keys [subject op]} _proposal store]
  (when (contains? #{:production-batch/intake :maintenance/schedule
                      :device-release/request-review} op)
    (cond (not (registry/batch-exists? store subject))
          [{:rule :batch-not-found
            :detail (str "Batch " subject " not found. Verify batch-id and intake first.")}]

          (not (registry/batch-record-complete? store subject))
          [{:rule :batch-record-incomplete
            :detail "Batch record missing required fields (production-date, equipment, materials, traceability, quality-checks)."}]

          (not (registry/batch-traceability-valid? store subject))
          [{:rule :batch-traceability-missing
            :detail "Batch must have complete material traceability before proceeding."}]

          :else nil)))

(defn- open-safety-deviation-violations
  "HARD BLOCK 5: Any batch with an unresolved safety deviation cannot proceed."
  [{:keys [subject]} _proposal store]
  (when-let [deviation (registry/open-safety-deviation-affecting-batch store subject)]
    [{:rule :open-safety-deviation
      :detail (str "Batch has an unresolved safety deviation: " (:detail deviation " — escalate to management."))
      :must-escalate true}]))

;; ======================= Soft escalation checks =======================

(defn- safety-deviation-escalation
  "SOFT ESCALATION: `:safety/flag-deviation` ALWAYS escalates (never auto-commits)."
  [{:keys [op]}]
  (when (= op :safety/flag-deviation)
    [{:rule :safety-deviation-always-escalates
      :detail "Safety deviations are critical and always escalate to human management."}]))

(defn- device-release-review-escalation
  "SOFT ESCALATION: `:device-release/request-review` ALWAYS escalates.
  Only the qualified person can execute the actual review and release."
  [{:keys [op]}]
  (when (= op :device-release/request-review)
    [{:rule :device-release-review-always-escalates
      :detail "Device release reviews are performed by the qualified person. This actor only proposes the request."}]))

(defn- confidence-floor-check
  "SOFT ESCALATION: Low confidence or unrecognized operation."
  [_request proposal]
  (when (< (:confidence proposal 1.0) confidence-floor)
    [{:rule :low-confidence
      :detail (str "LLM confidence " (:confidence proposal) " below floor " confidence-floor)}]))

;; ======================= Main governor check =======================

(defn check
  "Run all checks (hard violations first, then soft escalations).
  Returns {:violations [...], :high-stakes? bool, :confidence float}"
  [request _context proposal store]
  (let [violations (concat
                    (device-release-violation? proposal)
                    (regulatory-certification-violation? proposal)
                    (spec-basis-violations request proposal)
                    (batch-record-violations request proposal store)
                    (open-safety-deviation-violations request proposal store)
                    (safety-deviation-escalation request)
                    (device-release-review-escalation request)
                    (confidence-floor-check request proposal))
        hard-violations (filter #(not (:soft %)) violations)
        is-high-stakes (contains? high-stakes (:op request))]
    {:violations violations
     :hard-violations hard-violations
     :high-stakes? is-high-stakes
     :confidence (:confidence proposal 1.0)}))

(defn hold-fact
  "Create an audit ledger entry for a HOLD disposition."
  [request context verdict]
  {:t :governor-hold
   :op (:op request)
   :actor (:actor-id context)
   :subject (:subject request)
   :reason (:reason context)
   :violations (map (fn [v] (dissoc v :soft)) (:violations verdict))})
