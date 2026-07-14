(ns medicaldevice.facts
  "Regulatory facts for medical device manufacturing (ISIC 2660: irradiation,
  electromedical and electrotherapeutic equipment). These are the canonical
  jurisdiction-specific requirements that all proposals must cite or reject.

  CRITICAL SCOPE BOUNDARY: This actor performs operations coordination only
  (batch records, maintenance scheduling, safety flagging). Device release
  for market, regulatory certification, and patient-safety sign-off are
  EXCLUSIVELY the qualified person's domain under medical-device regulations
  (FDA 21 CFR, CE marking directives, PMDA, etc.). No proposal in this actor
  can release, certify, or claim regulatory compliance for any device.")

;; Regulatory framework anchors
(def fda-21-cfr-part-820
  {:jurisdiction :fda
   :code "21 CFR Part 820"
   :scope "Quality System Regulation (QSR)"
   :applicability :class-ii-iii-devices
   :canonical-url "https://www.ecfr.gov/current/title-21/part-820"})

(def ce-mdcg-guidelines
  {:jurisdiction :ce
   :code "MDCG Guidelines"
   :scope "Quality Management System for Medical Devices"
   :applicability :class-iii-devices
   :canonical-url "https://ec.europa.eu/health/medical-devices_en"})

(def iso-13485-2016
  {:jurisdiction :international
   :code "ISO 13485:2016"
   :scope "Medical devices — Quality management systems"
   :applicability :all-classes
   :canonical-url "https://www.iso.org/standard/59752.html"})

;; Factory record requirements
(def batch-record-checklist
  {:item :batch-record-required
   :condition "Every manufacturing batch must have a verified production record on file"
   :citations [fda-21-cfr-part-820 iso-13485-2016]})

(def maintenance-log-required
  {:item :maintenance-log-required
   :condition "Equipment maintenance and calibration must be logged and scheduled per jurisdiction"
   :citations [fda-21-cfr-part-820 ce-mdcg-guidelines iso-13485-2016]})

(def traceability-required
  {:item :traceability-required
   :condition "All materials and components must be traceable to source and batch"
   :citations [fda-21-cfr-part-820 iso-13485-2016]})

;; Safety-critical deviation handling
(def safety-deviation-escalation-required
  {:item :safety-deviation-escalation
   :condition "Any deviation affecting device safety or performance must be immediately escalated to management and never silently logged"
   :citations [fda-21-cfr-part-820 ce-mdcg-guidelines iso-13485-2016]
   :consequence :hard-escalation})

;; CRITICAL BOUNDARY: Device release is NOT in this actor's authority
(def device-release-qualified-person-only
  {:item :device-release-qualified-person
   :condition "Device release for market distribution requires written approval by a qualified person with regulatory responsibility — NO automated release in this actor"
   :citations [fda-21-cfr-part-820 ce-mdcg-guidelines iso-13485-2016]
   :consequence :permanent-hard-block
   :scope-exclusion "This actor proposes device-release reviews but never executes them"})

(def regulatory-certification-human-only
  {:item :regulatory-certification-human-only
   :condition "Regulatory certification (FDA clearance, CE marking, PMDA approval) is performed by human regulatory affairs and quality management — NOT by this actor"
   :citations [fda-21-cfr-part-820 ce-mdcg-guidelines]
   :consequence :permanent-hard-block
   :scope-exclusion "This actor may surface compliance gaps but never claims to certify regulatory compliance"})

;; Valid operations (proposal-only, no direct actuation)
(def valid-operation-scopes
  {:production-batch-intake
   {:op :production-batch/intake
    :scope "Normalize and log a manufacturing batch intake event"
    :effect :propose
    :requires [batch-record-checklist traceability-required]}

   :maintenance-scheduling
   {:op :maintenance/schedule
    :scope "Propose equipment maintenance/calibration scheduling"
    :effect :propose
    :requires [maintenance-log-required]}

   :safety-deviation-flagging
   {:op :safety/flag-deviation
    :scope "Surface a safety-critical deviation for escalation"
    :effect :propose
    :requires []
    :escalation :mandatory}

   :device-release-review-request
   {:op :device-release/request-review
    :scope "Request a review slot with the qualified person for device-release sign-off"
    :effect :propose
    :requires [batch-record-checklist traceability-required]
    :note "The request is a proposal only; release itself is the qualified person's exclusive act"}})
