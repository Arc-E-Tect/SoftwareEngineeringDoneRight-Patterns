Feature: Publish that a validation job has run
  Every asynchronous validation that has completed or failed is published to the topic
  payments.validation.completed.v1, keyed by its validationId so that the events of one job stay
  in order. Like the webhook, the event refers to the job: a consumer reads the result from the
  job resource.

  Background:
    Given the payee verification scheme knows these accounts:
      | iban                   | registered name | answers    |
      | NL91ABNA0417164300     | Jan de Vries    | in time    |
      | DE89370400440532013000 | Anna Schmidt    | too late   |
      | NL20INGB0001234567     | Anna Bakker     | not at all |
    And the scheme covers accounts in "DE, NL"

  Scenario: The completion of a job is published, keyed by the job's identifier
    When a payer validates the name "Jan de Vries" against the IBAN "NL91ABNA0417164300", preferring an asynchronous answer with Idempotency-Key "9e3c7d1b-0f2a-4b4c-9d8e-9f0a1b2c3d4e"
    Then an event on "payments.validation.completed.v1" says that the job is "completed" with outcome "valid"
    And the event is keyed by the job's identifier
    And the event says where the job is

  Scenario: An indeterminate verdict is published as such
    When a payer validates the name "Anna Schmidt" against the IBAN "DE89370400440532013000", preferring an asynchronous answer with Idempotency-Key "0f4d8e2c-1a3b-4c5d-8e9f-0a1b2c3d4e5f"
    Then an event on "payments.validation.completed.v1" says that the job is "completed" with outcome "indeterminate"

  Scenario: The failure of a job is published, without an outcome
    When a payer validates the name "Maria Souza" against the IBAN "BR1800360305000010009795493C1", preferring an asynchronous answer with Idempotency-Key "1a5e9f3d-2b4c-4d6e-9f0a-1b2c3d4e5f6a"
    Then an event on "payments.validation.completed.v1" says that the job is "failed" without an outcome
    And the event is keyed by the job's identifier
