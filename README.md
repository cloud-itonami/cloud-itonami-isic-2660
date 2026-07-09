# cloud-itonami-2660

Open Business Blueprint for **ISIC Rev.5 2660**: manufacture of
irradiation, electromedical and electrotherapeutic equipment (imaging,
radiotherapy, monitoring and electrotherapy devices).

This repository designs a forkable OSS business for community medical
device manufacturing: design-control records, robotics-assisted
production and inspection, and device-history/release records — run by
a qualified operator so a manufacturer keeps its own design and
production history instead of renting a closed quality-management
platform.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot
performs the physical domain work**. Here robots (assembly,
calibration, inspection) operate under an actor that proposes actions
and an independent **Medical Device Governor** that gates them. The
governor never releases a device to market itself; `:high`/
`:safety-critical` actions (releasing a device batch that has not
passed verification, any change to a safety-critical design
specification) require human sign-off.

## Core Contract

```text
intake + identity + design specification + production/inspection mission
        |
        v
Manufacturing Advisor -> Medical Device Governor -> design record, batch release, or human approval
        |
        v
robot actions (gated) + device history record + release record + audit ledger
```

No automated advice can release a device batch the governor refuses,
approve a design change outside its verified specification, or publish a
release record without governor approval and audit evidence.

## Capability layer

Resolves via [`kotoba-lang/industry`](https://github.com/kotoba-lang/industry)
(ISIC `2660`). Implemented by:

- [`kotoba-lang/robotics`](https://github.com/kotoba-lang/robotics) — missions, actions, safety-stops, telemetry proofs
- [`kotoba-lang/eda`](https://github.com/kotoba-lang/eda) — electronic design automation contracts
- [`kotoba-lang/cae-solver`](https://github.com/kotoba-lang/cae-solver) — computer-aided engineering simulation contracts

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
