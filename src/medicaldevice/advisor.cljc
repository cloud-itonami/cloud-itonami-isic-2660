(ns medicaldevice.advisor
  "Medical Device Advisor -- the LLM node that proposes batch intake, maintenance
  scheduling, safety deviation escalations, and device-release reviews. The advisor
  is sealed into a single graph node and never sees governance logic or compliance
  boundaries (those are governor's domain). The proposal is ALWAYS routed through
  the Governor before committing.")

;; ======================= Advisor protocol =======================

(defprotocol Advisor
  "LLM advisor interface for medical device manufacturing operations."
  (-advise [this store request]
    "Propose an action based on request and current store state.
    Returns a proposal map: {:confidence, :effect, :value, :summary, :cites}"))

;; ======================= Mock advisor (testing) =======================

(deftype MockAdvisor []
  Advisor
  (-advise [_this _store request]
    {:confidence 0.8
     :effect :propose
     :value (:value request)
     :summary (str "Mock proposal for op " (:op request))
     :cites []}))

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
