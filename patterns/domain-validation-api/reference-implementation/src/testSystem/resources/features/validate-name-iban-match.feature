Feature: Validate a name against an IBAN
  POST /payments/v1/validations/name-iban-match tells a payer whether the name they give for the
  holder of an account matches the name the account is registered to, before they pay.
  A verdict is answered with 200: valid, invalid or indeterminate. A request the domain cannot
  process is answered with 422, and a request that breaks the contract with 400.

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

  Scenario Outline: A request that breaks the contract
    When a client sends the request <body>
    Then the answer is 400

    Examples:
      | body                                                                                                    | because                      |
      | {"iban": "NL91", "accountHolderName": "Jan de Vries"}                                                   | the IBAN has no IBAN's shape |
      | {"iban": "NL91ABNA0417164300", "accountHolderName": "   "}                                              | the name is only whitespace  |
      | {"iban": "NL91ABNA0417164300", "accountHolderName": ""}                                                 | the name is empty            |
      | {"iban": "NL91ABNA0417164300"}                                                                          | the name is missing          |
      | {"iban": "NL91ABNA0417164300", "accountHolderName": "Aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"} | the name is too long |
