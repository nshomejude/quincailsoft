# QuincaiSoft v3 coverage

This is a local UI implementation, not a completed production platform.

| Item | Count / outcome |
|---|---|
| screens | 152 |
| field_placements | 1140 |
| actions | 656 |
| reports | 76 |
| documents | 26 |
| render | PASS |

## Action classifications

| Kind | Count |
|---|---|
| navigate | 231 |
| detail | 39 |
| filter | 36 |
| export | 39 |
| integration_required | 97 |
| create | 37 |
| edit | 52 |
| transition | 81 |
| document | 19 |
| print | 11 |
| duplicate | 5 |
| policy | 8 |
| refresh | 1 |

Navigation bindings open the associated local screen; they do not implement the corresponding backend command. Field count is authored implementation coverage, not a proof of exhaustive requirements discovery.

## Explicitly incomplete actions

- **S03**: acknowledge
- **S23**: snooze alert
- **S38**: issue
- **S54**: Zoom
- **S54**: mark copy
- **S55**: disable
- **S56**: revoke
- **S60**: Discover
- **S61**: Backup now
- **S61**: choose external drive
- **S62**: safety backup
- **S62**: restore
- **S63**: commit
- **S65**: integrity check
- **S66**: system check
- **S68**: generate recovery
- **S69**: commit opening ledger
- **S70**: run backup
- **S70**: restore test
- **S71**: Sign in
- **S71**: reveal password
- **S72**: Unlock
- **S72**: switch user
- **S72**: close session
- **S73**: Change password
- **S73**: recover owner
- **S73**: revoke sessions
- **S76**: refund where authorized
- **S77**: refund
- **S79**: restore
- **S80**: mark recovered
- **S85**: return for correction
- **S88**: silence
- **N01**: Verify
- **N01**: provision via M01/M03
- **N02**: pay/query status
- **N06**: Reserve
- **N06**: dispatch
- **N07**: resend
- **N07**: revoke
- **N07**: switch active membership
- **N08**: Enroll factor
- **N08**: revoke session/device
- **N08**: recover with step-up
- **N09**: test
- **N09**: rotate
- **N09**: enable after evidence
- **N12**: Resume provisioning
- **N12**: suspend permitted paths
- **N12**: schedule migration
- **N13**: grant
- **N13**: revoke
- **N14**: activate cutover
- **N15**: retry same ID
- **N16**: Grant disjoint quota
- **N16**: freeze
- **N16**: recall with proof
- **N18**: locally commit permitted sale
- **N19**: sync
- **N19**: dispatch/receive only under authorized authority
- **N20**: Retry
- **N20**: resnapshot
- **N21**: sync
- **N25**: remove
- **N26**: Reserve
- **N26**: query payment
- **N26**: retry safely
- **N27**: Verify access
- **N28**: Verify DNS
- **N28**: publish
- **N28**: disable
- **N29**: hold/release
- **N34**: Run checks
- **N36**: revert to prior public version
- **N37**: Verify identity
- **N38**: dispatch
- **N39**: issue grant
- **N39**: expire unused challenge
- **X03**: Issue
- **X11**: release
- **X12**: freeze exposure
- **X13**: revoke
- **X13**: recover
- **X13**: retire
- **X14**: dispatch
- **X15**: publish management consolidation
- **X16**: confirm cut
- **X21**: block
- **X21**: notify authorized recipients
- **X21**: recover
- **X21**: dispose
- **X22**: Rehearse
- **X22**: freeze
- **X22**: restore/migrate
- **X22**: reopen
- **X23**: Activate offline
- **X23**: recover host
