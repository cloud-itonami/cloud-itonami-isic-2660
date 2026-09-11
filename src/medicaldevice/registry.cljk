(ns medicaldevice.registry
  "Ground-truth field validators for batch records, maintenance logs,
  safety deviations, and device-release reviews. All governors delegate
  field-level checks to this ns (independent re-derivation, not stored-verdict
  lookups)."
  (:require [medicaldevice.store :as store]))

;; ======================= Batch record validation =======================

(defn batch-exists?
  "Does a batch record with this ID already exist on file?"
  [store batch-id]
  (boolean (store/get-batch store batch-id)))

(defn batch-record-complete?
  "Is the batch record complete (has all required fields)?
  Required: batch-id, production-date, equipment-used, materials,
  traceability-id, quality-checks-passed?, maintained-by."
  [store batch-id]
  (when-let [batch (store/get-batch store batch-id)]
    (let [required-keys [:batch-id :production-date :equipment-used
                         :materials :traceability-id :quality-checks-passed
                         :maintained-by]]
      (every? (fn [k] (contains? batch k)) required-keys))))

(defn batch-traceability-valid?
  "Is the batch's material traceability chain complete?"
  [store batch-id]
  (when-let [batch (store/get-batch store batch-id)]
    (boolean (:traceability-id batch))))

;; ======================= Maintenance validation =======================

(defn maintenance-logged?
  "Has maintenance for this equipment been logged for the given date?"
  [store equipment-id date]
  (let [log (store/get-maintenance-log store equipment-id)
        date-logs (filter #(= (:date %) date) log)]
    (seq date-logs)))

(defn maintenance-next-due
  "Calculate next maintenance-due date based on last logged maintenance.
  Returns a date or nil if no maintenance record exists."
  [store equipment-id]
  (let [log (store/get-maintenance-log store equipment-id)
        last-maintenance (when (seq log)
                          (last (sort-by :date log)))]
    (when last-maintenance
      (let [interval-days (or (:interval-days last-maintenance) 90)]
        (+ (:date last-maintenance) interval-days)))))

;; ======================= Safety deviation validation =======================

(defn safety-deviations-open?
  "Are there any unresolved safety deviations on file?"
  [store]
  (seq (store/get-safety-deviations store)))

(defn open-safety-deviation-affecting-batch
  "Find any open safety deviation that could affect this batch.
  Returns the deviation or nil."
  [store batch-id]
  (let [deviations (store/get-safety-deviations store)]
    (first (filter #(and (= (:batch-id %) batch-id)
                        (not (:resolved? %)))
                   deviations))))

;; ======================= Device-release review validation =======================

(defn device-release-review-pending?
  "Is a device-release review already pending for this batch?"
  [store batch-id]
  (let [batch (store/get-batch store batch-id)]
    (when batch
      (= (:release-status batch) :review-pending))))

(defn device-already-released?
  "Has this batch already been released for market?"
  [store batch-id]
  (let [batch (store/get-batch store batch-id)]
    (when batch
      (= (:release-status batch) :released))))

;; ======================= CRITICAL: Scope exclusions =======================

(defn batch-was-certified?
  "PERMANENT HARD BLOCK: Has this batch been marked as regulatory-certified?
  Certification (FDA clearance, CE marking, PMDA) is the qualified person's
  exclusive act and must NEVER be performed by this actor.
  Always returns false (the flag is never set by this actor)."
  [_store _batch-id]
  false)

(defn block-device-release-claim
  "PERMANENT HARD BLOCK: Reject any proposal claiming to release or certify
  a device for market. This actor proposes reviews only; release is not its
  authority."
  [_store _batch-id]
  {:violation :device-release-authority-violation
   :detail "This actor cannot release devices for market. Only qualified person review can proceed."})
