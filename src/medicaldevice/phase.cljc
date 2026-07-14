(ns medicaldevice.phase
  "Rollout phase gates (0-3). Each phase gate adds caution on top of
  governor disposition. No phase allows auto-commit of high-stakes actions
  (safety/flag-deviation, device-release/request-review).

  Phase 0 (Development): all high-stakes escalate, all low-stakes may auto-commit
  Phase 1 (Pilot):       all high-stakes escalate, all low-stakes may auto-commit
  Phase 2 (Stage):       all high-stakes escalate, many low-stakes may auto-commit
  Phase 3 (Production):  all high-stakes escalate, all low-stakes may auto-commit"
  (:require [medicaldevice.governor :as governor]))

(def default-phase :phase/0)

(def phase-descriptions
  {:phase/0 "Development -- high caution, all high-stakes escalate"
   :phase/1 "Pilot -- continued caution, all high-stakes escalate"
   :phase/2 "Stage -- reduced caution, all high-stakes still escalate"
   :phase/3 "Production -- full confidence, but high-stakes always escalate"})

(defn verdict->disposition
  "Convert governor verdict to base disposition (before phase gate).
  Hard violations -> HOLD (no phase can override)
  High-stakes -> ESCALATE (no phase can override)
  Low confidence -> ESCALATE
  Clean -> COMMIT"
  [verdict]
  (cond
    (seq (:hard-violations verdict)) :hold
    (:high-stakes? verdict) :escalate
    (< (:confidence verdict 1.0) governor/confidence-floor) :escalate
    :else :commit))

(defn gate
  "Apply phase-gate caution on top of base disposition.
  Hard holds (base=:hold) are never overridden.
  High-stakes actions (base=:escalate) are never overridden.
  Returns {:disposition :hold|:escalate|:commit :reason (optional)}"
  [phase _request base-disposition]
  (cond
    (= base-disposition :hold)
    {:disposition :hold
     :reason "Governor hard violation"}

    (= base-disposition :escalate)
    {:disposition :escalate
     :reason (str "Phase " phase " gate: high-stakes action requires human approval")}

    (= base-disposition :commit)
    {:disposition :commit
     :reason nil}

    :else
    {:disposition :hold
     :reason (str "Unrecognized disposition " base-disposition)}))
