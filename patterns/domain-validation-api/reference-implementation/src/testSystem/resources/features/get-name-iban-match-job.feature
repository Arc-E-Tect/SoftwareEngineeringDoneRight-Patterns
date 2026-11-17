Feature: Get a name-iban-match validation job
  GET /payments/v1/validations/name-iban-match/{validationId} answers how an asynchronous
  validation is getting on, and its result once it has one.

  Scenario: There is no validation job with the identifier
    When a client asks for the validation job "8d4e2a10-5b7c-4e9f-a3d6-1c2b3a4d5e6f"
    Then the answer is 404
    And the answer is a problem of type "https://api.example.com/problems/validation-job-not-found"
    And the answer is not stored
