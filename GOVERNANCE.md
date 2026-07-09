# Governance

`cloud-itonami-2660` is an OSS open-business blueprint for community
medical device manufacturing, robotics-premised.

## Maintainers
Maintainers may merge changes that preserve these invariants:
- a robot action the governor refuses is never dispatched to hardware.
- the Medical Device Governor remains independent of the advisor.
- hard policy violations (out-of-specification design, unverified batch release, evidenceless release record) cannot be overridden by human approval.
- every dispatch, sign-off, design and release-record path is auditable.
- sensitive design and production data stays outside Git.

## Decision Records
Architecture decisions live in `docs/adr/`. Changes to the trust model, storage contract, public business model, operator certification or license should add or update an ADR.

## Operator Governance
Anyone may fork and operate independently. itonami.cloud certification is a separate trust mark and should require security, robot-safety, audit and data-flow review.

Certified operators can lose certification for:
- bypassing robot-safety or specification-scope checks
- mishandling design or production data
- misrepresenting certification status
- failing to respond to safety incidents
