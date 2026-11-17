Feature: Tell the webhook's subscribers that a validation job has run
  The validationCompleted webhook tells every subscriber, once and best effort, that an
  asynchronous validation has completed or failed. It refers to the job: a subscriber reads the
  result from the job resource.

  Background:
    Given the payee verification scheme knows these accounts:
      | iban                   | registered name | answers    |
      | NL91ABNA0417164300     | Jan de Vries    | in time    |
      | DE89370400440532013000 | Anna Schmidt    | too late   |
      | NL20INGB0001234567     | Anna Bakker     | not at all |
    And the scheme covers accounts in "DE, NL"
    And the validationCompleted webhook has these subscribers:
      | path                        | answers |
      | /hooks/validation-completed | 204     |
      | /hooks/unavailable          | 503     |

  Scenario: A subscriber is told that a job completed, and where to read it
    When a payer validates the name "Jan de Vries" against the IBAN "NL91ABNA0417164300", preferring an asynchronous answer with Idempotency-Key "6b0f4a8e-7c9d-4e1f-8a5b-6c7d8e9f0a1b"
    Then "/hooks/validation-completed" is told that the job is "completed" with outcome "valid"
    And the subscriber is told where the job is

  Scenario: A subscriber is told that a job failed, without an outcome
    When a payer validates the name "Maria Souza" against the IBAN "BR1800360305000010009795493C1", preferring an asynchronous answer with Idempotency-Key "7c1a5b9f-8d0e-4f2a-9b6c-7d8e9f0a1b2c"
    Then "/hooks/validation-completed" is told that the job is "failed" without an outcome

  Scenario: A subscriber that answers with an error keeps neither the others nor the job from theirs
    When a payer validates the name "Anna Bakker" against the IBAN "NL20INGB0001234567", preferring an asynchronous answer with Idempotency-Key "8d2b6c0a-9e1f-4a3b-8c7d-8e9f0a1b2c3d"
    Then "/hooks/unavailable" is told that the job is "completed" with outcome "indeterminate"
    And "/hooks/validation-completed" is told that the job is "completed" with outcome "indeterminate"
    When the client reads the job until it has run
    Then the job is "completed"
