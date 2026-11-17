# Validations

Every validation is an operation of its own, under `/v1/validations/`, with a literal path: `/v1/validations/name-iban-match`.
Its request and its result are typed per validation.

## Outcome

Every result has the same envelope: the `validation` that produced it, an `outcome`, and `findings`.

* `valid`: the input was checked and satisfies the rules.
* `invalid`: the input was checked and violates the rules; the findings say which.
* `indeterminate`: the input was checked, but no verdict could be reached, for example because a dependency was unavailable.

The outcome and the findings are independent: a `valid` outcome can carry findings with severity `warning` or `info`.

## Verdicts and request errors

An `invalid` input is a successful answer, with status `200`.
Status codes in the 4xx range are reserved for problems with the call itself, and carry Problem Details (RFC 9457) as `application/problem+json`:

* `400`: the request is malformed, or breaks the contract.
* `422`: the request is well-formed, but cannot be processed, for example because the account's country is not supported.

## Why POST

A validation changes no state: it is safe and idempotent.
It uses `POST` only because its input needs a request body, and because that input is often sensitive and must never appear in a URL.
Every validation operation carries `x-idempotent: true`, so gateways and clients can retry it.
Responses carry `Cache-Control: no-store`.

## Asynchronous validations

A client that prefers not to wait sends `Prefer: respond-async` and an `Idempotency-Key`.
The validation then runs as a job: the response is `202 Accepted`, with the job's `Location` and a `Retry-After` in seconds.
The job is `pending` until it is `completed`, with the result, or `failed`, with a problem.

Submitting the same `Idempotency-Key` with the same body returns the existing job; the same key with a different body is a `422`.
Without `Prefer: respond-async`, the validation answers synchronously and the key is ignored.

When a job finishes, a `ValidationCompleted` event is published to the `payments.validation.completed.v1` topic, and sent to the subscribers of the `validationCompleted` webhook.
The event refers to the job; the result is read from the job resource.
