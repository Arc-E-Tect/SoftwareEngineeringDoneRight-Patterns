Feature: Validate a name against an IBAN
  POST /payments/v1/validations/name-iban-match tells a payer whether the name they give for the
  holder of an account matches the name the account is registered to, before they pay.
  A verdict is answered with 200: valid, invalid or indeterminate. A request the domain cannot
  process is answered with 422, and a request that breaks the contract with 400.
  With Prefer: respond-async and an Idempotency-Key, the validation runs as a job instead, which
  is answered with 202 and read from the job resource.

  Background:
    Given the payee verification scheme knows these accounts:
      | iban                   | registered name | answers    |
      | NL91ABNA0417164300     | Jan de Vries    | in time    |
      | DE89370400440532013000 | Anna Schmidt    | too late   |
      | NL20INGB0001234567     | Anna Bakker     | not at all |
    And the scheme covers accounts in "DE, NL"

  Scenario: The name is the registered name
    When a payer validates the name "jan de vries" against the IBAN "NL91ABNA0417164300"
    Then the answer is 200
    And the outcome is "valid"
    And the match is "exact"
    And there is no registered name
    And the answer is not stored

  Scenario: The name is close to the registered name, which is returned so the payer can correct it
    When a payer validates the name "Jan de Vris" against the IBAN "NL91ABNA0417164300"
    Then the answer is 200
    And the outcome is "invalid"
    And the match is "close"
    And the registered name is "Jan de Vries"
    And there is a finding "NAME_IBAN_MISMATCH" about "/accountHolderName"

  Scenario: The name is somebody else's
    When a payer validates the name "Piet Jansen" against the IBAN "NL91ABNA0417164300"
    Then the answer is 200
    And the outcome is "invalid"
    And the match is "none"
    And there is no registered name
    And there is a finding "NAME_IBAN_MISMATCH" about "/accountHolderName"

  Scenario: The scheme does not know the account
    When a payer validates the name "Jan de Vries" against the IBAN "NL39RABO0300065264"
    Then the answer is 200
    And the outcome is "invalid"
    And the match is "none"

  Scenario Outline: The scheme cannot answer, so the payer can decide whether to pay
    When a payer validates the name "<name>" against the IBAN "<iban>"
    Then the answer is 200
    And the outcome is "indeterminate"
    And there are no details
    And there is a finding "PAYEE_VERIFICATION_UNAVAILABLE"

    Examples:
      | iban                   | name         | because                         |
      | DE89370400440532013000 | Anna Schmidt | the scheme answers too late     |
      | NL20INGB0001234567     | Anna Bakker  | the scheme cannot answer at all |

  Scenario: The scheme does not cover the account's country
    When a payer validates the name "Maria Souza" against the IBAN "BR1800360305000010009795493C1"
    Then the answer is 422
    And the answer is a problem of type "https://api.example.com/payments/problems/unsupported-country"
    And the problem names the country "BR"
    And the answer is not stored

  Scenario: The IBAN has the wrong check digits
    When a payer validates the name "Jan de Vries" against the IBAN "NL92ABNA0417164300"
    Then the answer is 422
    And the answer is a problem of type "https://api.example.com/payments/problems/invalid-iban-checksum"

  Scenario Outline: A request that breaks the contract says where
    When a client sends the request <body>
    Then the answer is 400
    And the answer is a problem of type "https://api.example.com/problems/invalid-request"
    And the problem says what is wrong at "<at>"
    And the answer is not stored

    Examples:
      | body                                                                                                    | at                 | because                                    |
      | {"iban": "NL91", "accountHolderName": "Jan de Vries"}                                                   | /iban              | the IBAN has no IBAN's shape               |
      | {"iban": "NL91ABNA0417164300", "accountHolderName": "   "}                                              | /accountHolderName | the name is only whitespace                |
      | {"iban": "NL91ABNA0417164300", "accountHolderName": ""}                                                 | /accountHolderName | the name is empty                          |
      | {"iban": "NL91ABNA0417164300"}                                                                          | /accountHolderName | the name is missing                        |
      | {"iban": "NL91ABNA0417164300", "accountHolderName": null}                                               | /accountHolderName | the name is null                           |
      | {"iban": ["NL91ABNA0417164300"], "accountHolderName": "Jan de Vries"}                                   | /iban              | the IBAN is not a string                   |
      | {"iban": "NL91ABNA0417164300", "accountHolderName": "Jan de Vries", "nickname": "Jan"}                  | /nickname          | the body has a member the contract lacks   |
      | {"iban": "NL91ABNA0417164300", "accountHolderName": "Jan de Vries", "a/b~c": "Jan"}                     | /a~1b~0c           | that member's name has a / and a ~         |
      | {"iban": "NL91ABNA0417164300", "accountHolderName": "Aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"} | /accountHolderName | the name is too long |

  Scenario Outline: A request that breaks the contract as a whole
    When a client sends the request <body>
    Then the answer is 400
    And the answer is a problem of type "https://api.example.com/problems/invalid-request"
    And the answer is not stored

    Examples:
      | body                                       | because                 |
      | ["NL91ABNA0417164300", "Jan de Vries"]     | the body is no object   |
      | {"iban": "NL91ABNA0417164300",             | the body is not JSON    |

  Scenario Outline: A name of only Unicode whitespace is refused
    When a client sends the request {"iban": "NL91ABNA0417164300", "accountHolderName": "<name>"}
    Then the answer is 400
    And the answer is a problem of type "https://api.example.com/problems/invalid-request"
    And the answer is not stored

    Examples:
      | name           | because                       |
      |      | em spaces                     |
      |      | no-break spaces               |

  Scenario: A name written with other whitespace is the same name
    When a payer validates the name "Jan de Vries" against the IBAN "NL91ABNA0417164300"
    Then the answer is 200
    And the outcome is "valid"
    And the match is "exact"

  Scenario: A request without a body
    When a client sends a request without a body
    Then the answer is 400
    And the answer is a problem of type "https://api.example.com/problems/invalid-request"
    And the answer is not stored

  Scenario: A body of a media type the contract does not accept
    When a client sends a valid request as "text/plain"
    Then the answer is 415
    And the answer is a problem of type "https://api.example.com/problems/unsupported-media-type"
    And the answer is not stored

  Scenario: A client that accepts no media type the contract answers with
    When a client sends a valid request, accepting only "text/plain"
    Then the answer is 406
    And the answer is a problem of type "https://api.example.com/problems/not-acceptable"
    And the answer is not stored

  Scenario: An Idempotency-Key that is not a lower-case UUID
    When a payer validates the name "Jan de Vries" against the IBAN "NL91ABNA0417164300", preferring an asynchronous answer with Idempotency-Key "Not-A-UUID"
    Then the answer is 400
    And the answer is a problem of type "https://api.example.com/problems/invalid-request"
    And the problem says what is wrong at "Idempotency-Key"
    And the answer is not stored

  Scenario: The payer prefers an asynchronous answer
    When a payer validates the name "Jan de Vries" against the IBAN "NL91ABNA0417164300", preferring an asynchronous answer with Idempotency-Key "0b4f8a2e-1c3d-4e5f-8a9b-0c1d2e3f4a5b"
    Then the answer is 202
    And the answer is a pending "name-iban-match" job
    And the answer says where the job is
    And the answer says when to read the job
    And the answer is not stored
    When the client reads the job until it has run
    Then the job is "completed"
    And the job's result has outcome "valid"

  Scenario: The same request with the same Idempotency-Key gets the same job
    Given a payer validated the name "Jan de Vries" against the IBAN "NL91ABNA0417164300", preferring an asynchronous answer with Idempotency-Key "1c5a9b3f-2d4e-4f6a-9b0c-1d2e3f4a5b6c"
    When a payer validates the name "Jan de Vries" against the IBAN "NL91ABNA0417164300", preferring an asynchronous answer with Idempotency-Key "1c5a9b3f-2d4e-4f6a-9b0c-1d2e3f4a5b6c"
    Then the answer is 202
    And it is the same job

  Scenario: Another request with the same Idempotency-Key is refused
    Given a payer validated the name "Jan de Vries" against the IBAN "NL91ABNA0417164300", preferring an asynchronous answer with Idempotency-Key "2d6b0c4a-3e5f-4a7b-8c1d-2e3f4a5b6c7d"
    When a payer validates the name "Piet Jansen" against the IBAN "NL91ABNA0417164300", preferring an asynchronous answer with Idempotency-Key "2d6b0c4a-3e5f-4a7b-8c1d-2e3f4a5b6c7d"
    Then the answer is 422
    And the answer is a problem of type "https://api.example.com/problems/idempotency-key-reused"
    And the answer is not stored

  Scenario: An asynchronous answer needs an Idempotency-Key, so that a retry does not start a second job
    When a payer validates the name "Jan de Vries" against the IBAN "NL91ABNA0417164300", preferring an asynchronous answer without an Idempotency-Key
    Then the answer is 400
    And the answer is a problem of type "https://api.example.com/problems/invalid-request"
    And the problem says what is wrong at "Idempotency-Key"
    And the answer is not stored

  Scenario: A request the domain refuses fails its job
    When a payer validates the name "Maria Souza" against the IBAN "BR1800360305000010009795493C1", preferring an asynchronous answer with Idempotency-Key "3e7c1d5b-4f6a-4b8c-9d2e-3f4a5b6c7d8e"
    Then the answer is 202
    When the client reads the job until it has run
    Then the job is "failed"
    And the job's problem is of type "https://api.example.com/payments/problems/unsupported-country"
