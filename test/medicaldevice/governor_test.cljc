(ns medicaldevice.governor-test
  (:require [clojure.test :refer [deftest is]]
            [medicaldevice.governor :as governor]
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
        context {:actor-id "advisor-1" :phase :phase/3}
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
        context {:actor-id "advisor-1" :phase :phase/3}
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
        context {:actor-id "advisor-1" :phase :phase/3}
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
        context {:actor-id "advisor-1" :phase :phase/3}
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
        context {:actor-id "advisor-1" :phase :phase/3}
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
        context {:actor-id "advisor-1" :phase :phase/3}
        verdict (governor/check request context proposal s)]
    (is (some #(= :low-confidence (:rule %))
              (:violations verdict)))))

;; ======================= Batch record validation tests =======================

(deftest batch-not-found-hard-block
  (let [s (store/mem-store)
        proposal {:effect :propose
                  :value {}
                  :cites [facts/iso-13485-2016]
                  :confidence 0.9}
        request {:op :production-batch/intake :subject "nonexistent-batch"}
        context {:actor-id "advisor-1" :phase :phase/3}
        verdict (governor/check request context proposal s)]
    (is (seq (:hard-violations verdict))
        "Non-existent batch should trigger hard violation")
    (is (some #(= :batch-not-found (:rule %))
              (:hard-violations verdict)))))

;; ======================= Hold-fact generation test =======================

(deftest hold-fact-generation
  (let [request {:op :production-batch/intake :subject "batch-001"}
        context {:actor-id "advisor-1" :phase :phase/3}
        verdict {:violations [{:rule :no-spec-basis :detail "test"}]
                 :confidence 0.8}
        fact (governor/hold-fact request context verdict)]
    (is (= :governor-hold (:t fact)))
    (is (= "advisor-1" (:actor context)))
    (is (seq (:violations fact)))))
