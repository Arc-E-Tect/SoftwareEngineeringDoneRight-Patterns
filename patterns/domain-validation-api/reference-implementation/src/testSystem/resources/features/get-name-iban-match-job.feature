Feature: Get a name-iban-match validation job
  GET /payments/v1/validations/name-iban-match/{validationId} answers how an asynchronous
  validation is getting on, and its result once it has one.

  Background:
    Given the payee verification scheme knows these accounts:
      | iban                   | registered name | answers    |
      | NL91ABNA0417164300     | Jan de Vries    | in time    |
      | DE89370400440532013000 | Anna Schmidt    | too late   |
      | NL20INGB0001234567     | Anna Bakker     | not at all |
    And the scheme covers accounts in "DE, NL"

  Scenario: A job that has not run yet says when to read it again
    Given a payer validated the name "Anna Schmidt" against the IBAN "DE89370400440532013000", preferring an asynchronous answer with Idempotency-Key "4f8d2e6c-5a7b-4c9d-8e3f-4a5b6c7d8e9f"
    When the client reads the job
    Then the answer is 200
    And the job is "pending"
    And the answer says when to read the job
    And the answer is not stored

  Scenario: A job that has run carries the result
    Given a payer validated the name "Anna Schmidt" against the IBAN "DE89370400440532013000", preferring an asynchronous answer with Idempotency-Key "5a9e3f7d-6b8c-4d0e-9f4a-5b6c7d8e9f0a"
    When the client reads the job until it has run
    Then the answer is 200
    And the job is "completed"
    And the job's result has outcome "indeterminate"
    And the answer does not say when to read the job

  Scenario: There is no validation job with the identifier
    When a client asks for the validation job "8d4e2a10-5b7c-4e9f-a3d6-1c2b3a4d5e6f"
    Then the answer is 404
    And the answer is a problem of type "https://api.example.com/problems/validation-job-not-found"
    And the answer is not stored

  Scenario: An identifier that is not a lower-case UUID is refused
    When a client asks for the validation job "8D4E2A10-5B7C-4E9F-A3D6-1C2B3A4D5E6F"
    Then the answer is 400
    And the answer is a problem of type "https://api.example.com/problems/invalid-request"
    And the problem says what is wrong at "validationId"
    And the answer is not stored
