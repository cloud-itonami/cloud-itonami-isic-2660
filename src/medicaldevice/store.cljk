(ns medicaldevice.store
  "Single Source of Truth (SSoT) and append-only audit ledger for medical
  device manufacturing operations. Backed by in-mem store today (forkable
  for Datomic/kotoba-server seam tomorrow).")

;; ======================= Store protocol =======================

(defprotocol Store
  "Abstraction over manufacturing batch records, equipment maintenance
  records, safety deviations, and device-release reviews."
  (get-batch [store batch-id]
    "Retrieve a batch record by ID, or nil if not found.")
  (get-batches [store]
    "Retrieve all batch records.")
  (get-maintenance-log [store equipment-id]
    "Retrieve maintenance records for an equipment unit.")
  (get-safety-deviations [store]
    "Retrieve all open (unresolved) safety deviations.")
  (commit-record! [store record]
    "Commit a record (batch, maintenance, etc.) to the SSoT.
    record: {:effect :propose, :path [subject-id], :value {...}, :payload {...}}")
  (append-ledger! [store fact]
    "Append an immutable fact to the audit ledger (never overwrite).
    fact: {:t :committed|:approval-requested|..., :op, :actor, :subject, ...}"))

;; ======================= In-memory implementation =======================

(deftype MemStore [batches-atom ledger-atom maintenance-atom deviations-atom]
  Store
  (get-batch [_store batch-id]
    (get @batches-atom batch-id))

  (get-batches [_store]
    (vals @batches-atom))

  (get-maintenance-log [_store equipment-id]
    (get @maintenance-atom equipment-id []))

  (get-safety-deviations [_store]
    (filter (fn [d] (not (:resolved? d))) @deviations-atom))

  (commit-record! [_store record]
    (let [{:keys [path value]} record
          [subject-id] path]
      (swap! batches-atom assoc subject-id value)
      nil))

  (append-ledger! [_store fact]
    (swap! ledger-atom conj fact)
    nil))

(defn mem-store
  "Create an in-memory Store (development/testing)."
  []
  (->MemStore (atom {}) (atom []) (atom {}) (atom [])))

(defn get-ledger
  "Retrieve the complete audit ledger."
  [store]
  (when (instance? MemStore store)
    @(.-ledger-atom ^MemStore store)))

;; ======================= Demo seed data =======================
;;
;; The only batch/deviation data this repo declares. Every demo driver
;; (`medicaldevice.render-html`) and every ground-truth id it may drive
;; comes from here -- a demo may not invent a subject. Deliberately
;; shaped so each of the Manufacturing Governor's batch-record checks
;; has a record that exercises it:
;;
;;   batch-2660-001  complete + traceable, no open deviation  -> clean
;;   batch-2660-002  record INCOMPLETE (no quality-checks-passed,
;;                   no maintained-by)                        -> hard block
;;   batch-2660-003  every field present but traceability-id nil
;;                                                            -> hard block
;;   batch-2660-004  complete + traceable, but carries an OPEN
;;                   safety deviation                         -> hard block
;;
;; Fields are ISIC-2660 product classes and internal record ids only.
;; No customer, no manufacturer, no lot quantities, no measurements --
;; nothing that would put a fabricated figure on the operator console.

(def demo-batches
  "Four manufacturing batch records covering the four states the
  Manufacturing Governor distinguishes. Required-field set is
  `medicaldevice.registry/batch-record-complete?`'s."
  [{:batch-id "batch-2660-001"
    :product-line :patient-monitor
    :device-class :class-ii
    :production-date "2026-07-02"
    :equipment-used ["assembly-cell-01" "calibration-bench-01"]
    :materials ["mat-ecg-electrode" "mat-display-module" "mat-power-supply"]
    :traceability-id "trace-2660-001"
    :quality-checks-passed false
    :maintained-by "qe-1"
    :release-status :in-process}

   ;; missing :quality-checks-passed and :maintained-by
   {:batch-id "batch-2660-002"
    :product-line :diagnostic-imaging-xray
    :device-class :class-ii
    :production-date "2026-07-06"
    :equipment-used ["assembly-cell-02"]
    :materials ["mat-xray-tube" "mat-collimator"]
    :traceability-id "trace-2660-002"
    :release-status :in-process}

   ;; complete field set, but the material traceability chain is not closed
   {:batch-id "batch-2660-003"
    :product-line :electrotherapy-stimulator
    :device-class :class-ii
    :production-date "2026-07-09"
    :equipment-used ["assembly-cell-01"]
    :materials ["mat-stimulator-board" "mat-lead-set"]
    :traceability-id nil
    :quality-checks-passed true
    :maintained-by "qe-2"
    :release-status :in-process}

   {:batch-id "batch-2660-004"
    :product-line :radiotherapy-linac
    :device-class :class-iii
    :production-date "2026-07-11"
    :equipment-used ["assembly-cell-03" "calibration-bench-02"]
    :materials ["mat-linac-waveguide" "mat-dosimetry-module"]
    :traceability-id "trace-2660-004"
    :quality-checks-passed true
    :maintained-by "qe-2"
    :release-status :in-process}])

(def demo-safety-deviations
  "One OPEN and one RESOLVED safety deviation. `get-safety-deviations`
  returns only the unresolved ones, so the resolved entry proves the
  filter: batch-2660-001 stays clean despite carrying a deviation
  record, while batch-2660-004 cannot proceed."
  [{:deviation-id "dev-2660-001"
    :batch-id "batch-2660-004"
    :deviation-type :radiation-output-drift
    :detail "Radiotherapy output drifted outside the verified dose window"
    :resolved? false}
   {:deviation-id "dev-2660-002"
    :batch-id "batch-2660-001"
    :deviation-type :electrical-safety
    :detail "Leakage-current check failed on the first article; corrected and re-tested"
    :resolved? true}])

(defn sample-data!
  "Seed a MemStore with `demo-batches` / `demo-safety-deviations` and
  return it (development/demo only -- a production Store arrives already
  populated)."
  [store]
  (when (instance? MemStore store)
    (reset! (.-batches-atom ^MemStore store)
            (reduce (fn [m b] (assoc m (:batch-id b) b)) {} demo-batches))
    (reset! (.-deviations-atom ^MemStore store) (vec demo-safety-deviations)))
  store)
