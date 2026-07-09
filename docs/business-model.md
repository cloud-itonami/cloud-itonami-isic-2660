# Business Model: Community Medical Device Manufacturing

## Classification
- Repository: `cloud-itonami-2660`
- ISIC Rev.5: `2660` — manufacture of irradiation, electromedical and
  electrotherapeutic equipment
- Social impact: patient safety, device traceability, public health

## Customer
- independent medical device manufacturers needing an auditable
  quality-management platform
- contract manufacturers producing electromedical/irradiation
  equipment
- clinical engineering teams needing device-history and release
  records
- programs that cannot accept closed, unauditable quality-management
  platforms

## Offer
- design-control and specification-version management
- robotics-assisted production, calibration and inspection
- device history records
- batch-release and disclosure records
- role-based access and immutable audit ledger

## Revenue
- self-host setup fee
- managed hosting subscription per production line
- support retainer with SLA
- production/inspection robot integration and maintenance

## Trust Controls
- a robot action the governor refuses is never dispatched
- safety-critical actions (releasing a device batch that has not
  passed verification, changing a safety-critical design
  specification) require human sign-off
- a device batch cannot be released outside its verified specification
- release records require source verification evidence
- sensitive design and production data stays outside Git
