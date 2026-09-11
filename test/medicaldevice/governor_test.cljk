(ns medicaldevice.governor-test
  (:require [clojure.test :refer [deftest is]]
            [medicaldevice.governor :as governor]
            [medicaldevice.phase :as phase]
            [medicaldevice.store :as store]
            [medicaldevice.facts :as facts]))

;; ======================= Hard violation tests =======================

(deftest device-release-hard-block
  (let [s (store/mem-store)
        proposal {:effect :release-device
                  :value {:released-for-market true}
                  :cites [facts/fda-21-cfr-part-820]
                  :confidence 0.9}
        request {:op :production-batch/intake :subject "batch-001"}
        context {:actor-id "advisor-1" :phase 3}
        verdict (governor/check request context proposal s)]
    (is (seq (:hard-violations verdict))
        "Device release should trigger hard violation")
    (is (some #(= :device-release-authority-violation (:rule %))
              (:hard-violations verdict)))))

(deftest regulatory-certification-hard-block
  (let [s (store/mem-store)
        proposal {:effect :certify-regulatory-compliance
                  :value {:fda-cleared true}
                  :cites [facts/fda-21-cfr-part-820]
                  :confidence 0.9}
        request {:op :production-batch/intake :subject "batch-001"}
        context {:actor-id "advisor-1" :phase 3}
        verdict (governor/check request context proposal s)]
    (is (seq (:hard-violations verdict))
        "Regulatory certification should trigger hard violation")
    (is (some #(= :regulatory-certification-violation (:rule %))
              (:hard-violations verdict)))))

(deftest spec-basis-hard-block
  (let [s (store/mem-store)
        proposal {:effect :propose
                  :value {:batch-id "batch-001"}
                  :cites []
                  :confidence 0.8}
        request {:op :production-batch/intake :subject "batch-001"}
        context {:actor-id "advisor-1" :phase 3}
        verdict (governor/check request context proposal s)]
    (is (seq (:hard-violations verdict))
        "Empty spec-basis citations should trigger hard violation")
    (is (some #(= :no-spec-basis (:rule %))
              (:hard-violations verdict)))))

;; ======================= Soft escalation tests =======================

(deftest safety-deviation-always-escalates
  (let [s (store/mem-store)
        proposal {:effect :propose
                  :value {:deviation-type :electrical-safety}
                  :cites [facts/fda-21-cfr-part-820]
                  :confidence 0.95}
        request {:op :safety/flag-deviation :subject "batch-001"}
        context {:actor-id "advisor-1" :phase 3}
        verdict (governor/check request context proposal s)]
    (is (:high-stakes? verdict)
        "Safety deviation should be marked high-stakes")
    (is (some #(= :safety-deviation-always-escalates (:rule %))
              (:violations verdict)))))

(deftest device-release-review-always-escalates
  (let [s (store/mem-store)
        proposal {:effect :propose
                  :value {:batch-id "batch-001"}
                  :cites [facts/fda-21-cfr-part-820]
                  :confidence 0.9}
        request {:op :device-release/request-review :subject "batch-001"}
        context {:actor-id "advisor-1" :phase 3}
        verdict (governor/check request context proposal s)]
    (is (:high-stakes? verdict)
        "Device-release review should be marked high-stakes")
    (is (some #(= :device-release-review-always-escalates (:rule %))
              (:violations verdict)))))

(deftest low-confidence-escalates
  (let [s (store/mem-store)
        proposal {:effect :propose
                  :value {:batch-id "batch-001"}
                  :cites [facts/fda-21-cfr-part-820]
                  :confidence 0.4}
        request {:op :production-batch/intake :subject "batch-001"}
        context {:actor-id "advisor-1" :phase 3}
        verdict (governor/check request context proposal s)]
    (is (some #(= :low-confidence (:rule %))
              (:violations verdict)))))

;; ======================= Soft vs hard: the DISPOSITION, not just the list ====
;;
;; The three soft checks used to be asserted only through `:violations`, which
;; every check lands in. That hid the fact that they also landed in
;; `:hard-violations` (nothing set `:soft`), so `verdict->disposition` HELD them
;; and the human-in-the-loop `:request-approval` node was unreachable. Assert
;; the disposition so the regression cannot come back silently.

(deftest soft-escalations-are-not-hard-violations
  ;; The review op runs the batch-record hard checks, so it needs a real
  ;; seeded batch -- against an empty store it HOLDs on :batch-not-found and
  ;; proves nothing about softness.
  (let [s (store/sample-data! (store/mem-store))
        seeded (:batch-id (first store/demo-batches))
        clean {:effect :propose :value {:batch-id seeded}
               :cites [facts/fda-21-cfr-part-820] :confidence 0.95}
        context {:actor-id "advisor-1" :phase 3}
        safety (governor/check {:op :safety/flag-deviation :subject seeded}
                               context clean s)
        review (governor/check {:op :device-release/request-review :subject seeded}
                               context (assoc clean :value {}) s)]
    (is (empty? (:hard-violations safety))
        "safety-deviation-always-escalates is soft, not a hard block")
    (is (= :escalate (phase/verdict->disposition safety))
        "a clean safety-deviation flag must reach a human, not HOLD")
    (is (empty? (:hard-violations review))
        "device-release-review-always-escalates is soft, not a hard block")
    (is (= :escalate (phase/verdict->disposition review))
        "a clean device-release review request must reach a human, not HOLD")))

(deftest low-confidence-escalates-rather-than-holds
  (let [s (store/mem-store)
        verdict (governor/check {:op :safety/flag-deviation :subject "batch-001"}
                                {:actor-id "advisor-1" :phase 3}
                                {:effect :propose :value {}
                                 :cites [facts/iso-13485-2016] :confidence 0.4}
                                s)]
    (is (empty? (:hard-violations verdict)))
    (is (= :escalate (phase/verdict->disposition verdict)))))

(deftest hard-blocks-still-outrank-soft-escalations
  (let [s (store/mem-store)
        verdict (governor/check {:op :device-release/request-review :subject "batch-001"}
                                {:actor-id "advisor-1" :phase 3}
                                {:effect :propose :value {} :cites [] :confidence 0.9}
                                s)]
    ;; no cites + unknown batch: hard blocks fire alongside the soft escalation
    (is (seq (:hard-violations verdict)))
    (is (= :hold (phase/verdict->disposition verdict))
        "a hard block must never be softened by a co-occurring escalation")))

;; ======================= Batch record validation tests =======================

(deftest batch-not-found-hard-block
  (let [s (store/mem-store)
        proposal {:effect :propose
                  :value {}
                  :cites [facts/iso-13485-2016]
                  :confidence 0.9}
        request {:op :production-batch/intake :subject "nonexistent-batch"}
        context {:actor-id "advisor-1" :phase 3}
        verdict (governor/check request context proposal s)]
    (is (seq (:hard-violations verdict))
        "Non-existent batch should trigger hard violation")
    (is (some #(= :batch-not-found (:rule %))
              (:hard-violations verdict)))))

;; ======================= Hold-fact generation test =======================

(deftest hold-fact-generation
  (let [request {:op :production-batch/intake :subject "batch-001"}
        context {:actor-id "advisor-1" :phase 3}
        verdict {:violations [{:rule :no-spec-basis :detail "test"}]
                 :confidence 0.8}
        fact (governor/hold-fact request context verdict)]
    (is (= :governor-hold (:t fact)))
    ;; assert on the produced fact, not on the input we just wrote: `hold-fact`
    ;; is what renames context's `:actor-id` to the ledger's `:actor`.
    (is (= "advisor-1" (:actor fact)))
    (is (seq (:violations fact)))))
