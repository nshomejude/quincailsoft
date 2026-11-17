# Mobile money implementation and activation boundary

Reviewed 17 November 2026. This module belongs to the local integration backend, not the future Java production service. No actual provider credentials were supplied, no live payment was sent, and no provider sandbox call was executed. All executed provider tests use an injected fake HTTP transport.

## Status

| Provider | Implemented | Still required |
| --- | --- | --- |
| MTN MoMo Collections | OAuth credential request; RequestToPay; status query; durable intents; duplicate and timeout handling; verified reconciliation; callback reconciliation flag | Merchant credentials, confirmed Cameroon deployment endpoint, provider sandbox acceptance, live account approval, HTTPS deployment, background reconciliation worker, production security review |
| Orange Money | Explicit configuration/status entry and unavailable adapter; requests fail with a clear 503 | Subscribed Cameroon merchant API specification and credentials, concrete adapter implementation, tests against its actual contract, provider approval |

**Orange Money is not integrated. MTN is implemented and mock-tested but not activated or provider-verified.** The Orange public overview establishes service availability; it does not provide a verified transaction contract. Attempts to read the public getting-started/API-reference routes failed. We did not copy unofficial SDK contracts or assume another country's endpoint.

## MTN endpoint migration

MTN's official October 2026 update changes production credentials to UserID/Credential Token and requires `X-Target-Environment` during token generation. The new production origin is `https://momoapi.momo.africa`; the notice marks Cameroon migration in progress and asks merchants to confirm go-live with their account manager. Accordingly, production has no automatic origin selection. Configure the origin confirmed for the merchant's migration state. The adapter accepts only known MTN HTTPS origins, rejects redirects, and includes the target header for all calls.

The implemented calls are `POST /collection/token`, `POST /collection/v1_0/requesttopay`, and `GET /collection/v1_0/requesttopay/{referenceId}`. `202 Accepted` is pending, never paid. A duplicate reference (`409`) is queried using the existing UUID. Network ambiguity retains an Unknown intent and prevents a fresh charge on idempotent replay.

Sandbox uses EUR under MTN's test contract. A sandbox success produces `Sandbox verified`, **not** a completed XAF payment; there is no implied currency conversion. Live Cameroon requires `mtncameroon`, XAF, configured merchant credentials and `QUINCAI_PAYMENTS_LIVE_ENABLED=1`.

## Server wiring

`initialize(c)` creates the intent table. `handle_api(c,user,path,body)` returns a result dictionary or `None` for unrelated routes. Keep normal authenticated RBAC, CSRF and origin checks. Allow GET only for config/intents; initiation and query use POST.

| Endpoint | Input | Output |
| --- | --- | --- |
| GET `/api/mobile-money/config` | None | `{providers:[{id,label,configured,environment,reason}]}` |
| GET `/api/mobile-money/intents` | None | `{intents:[...]}` restricted to readable branch payments |
| POST `/api/mobile-money/initiate` | `{payment_id,provider,phone,operation_id}` | `{intent}` |
| POST `/api/mobile-money/query` | `{intent_id}` | `{intent,payment?}` |

Provider keys are `mtn` and `orange`. Amount, currency and customer binding come from server records/configuration, not a browser-entered collection amount. Initiation requires a Mobile money draft linked to a completed sale. The amount cannot exceed the sale balance after posted and pending payments. Outgoing supplier collections are rejected.

The module commits its new intent and locks the payment as Awaiting provider **before** making an external request. Call it only as a dedicated endpoint, never with unrelated pending database changes. Root server serialization currently protects local concurrent commands; a distributed production implementation needs database locking and an outbox/worker.

Before marking a live payment Completed, the authenticated provider GET must match amount, currency, external ID, payer identity and a nonempty financial transaction ID. Allocation is rechecked against the current sale balance. Duplicate provider transaction IDs across intents are rejected. Mismatches become Review required and do not allocate funds. A confirmed payment with an allocation conflict remains visible for reconciliation; it must not be charged again.

Generic payment completion must reject unverified Mobile money tenders. Edit endpoints must lock Awaiting provider/Sandbox verified records. Provider verification invokes the shared finance helper to post a balanced provider-clearing debit and accounts-receivable credit. A closed period or posting error retains verified provider truth as Review required without allocating the payment. Reconciliation can finish after the accounting issue is corrected. Provider settlement, fees and payouts remain separate unfinished production workflows.

## Callback safety and reconciliation

Optional public POST/PUT callback route: `/api/mobile-money/callback/{provider}/{intent_id}`. Invoke `callback(c,provider,intent_id,body)`. It ignores claimed status and amount; it only sets `query_due=true`. No secrets, payer numbers, raw provider bodies or tokens are returned by API views. Callbacks cannot mark a payment successful or initiate network calls. Add perimeter rate limits at deployment.

An authenticated operator can query pending intents. A production scheduler must poll `query_due` intents with backoff and monitored age limits, because MTN callbacks are not guaranteed to be retried. Unknown intents survive process restart. Do not generate a fresh reference solely because a response timed out. Refunds and provider payouts are not implemented by this Collections adapter.

## Executed tests

Run `python3 -m unittest discover -s tests -p test_mobile_money.py -v`.

17 tests pass: asynchronous acceptance; required token/request headers; one initiation under replay; conflicting operation IDs; durable unknown outcome; 409 reference handling; verified settlement once; mismatched amount rejection; forged callback rejection; sandbox accounting isolation; live activation gate; Orange unavailable state; auditor rejection; cross-branch rejection; excess collection rejection; untrusted provider-origin rejection; closed-period provider truth retained and accounting reconciliation after reopening.

## Official sources

- MTN API enhancement and migration notice: https://momodevelopercommunity.mtn.com/product-updates (12 October 2026 notice).
- MTN RequestToPay workflow: https://momodeveloper.mtn.com/content/html_widgets/v98wn.html
- MTN errors/environment mapping: https://momodeveloper.mtn.com/content/html_widgets/ohu47.html
- MTN callback behavior: https://momodeveloper.mtn.com/content/html_widgets/nqxho.html
- MTN sandbox currency: https://momodeveloper.mtn.com/content/html_widgets/6kfu8.html
- MTN API-user/OAuth model: https://momodeveloper.mtn.co.rw/content/html_widgets/y6m1i.html
- Orange service overview and onboarding: https://developer.orange.com/apis/om-webpay
- Orange Cameroon web-payment offer: https://orangemoney.orange.cm/orange-money/fr/om-partenaires/partenaire-webpayment.html

No public research substitutes for the merchant's signed onboarding contract or provider acceptance testing.
