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
