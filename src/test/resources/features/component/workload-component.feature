Feature: Workload Service Component Tests

  Scenario: Apply workload event successfully
    Given an authorized user
    And a valid trainer workload request
    When the client sends POST request to apply workload event
    Then the response status should be 200

  Scenario: Apply workload event without authorization
    Given no authorization token
    And a valid trainer workload request
    When the client sends POST request to apply workload event
    Then the response status should be 403

  Scenario: Apply workload event with invalid request
    Given an authorized user
    And a trainer workload request without username
    When the client sends POST request to apply workload event
    Then the response status should be 400

  Scenario: Apply workload event with past training date
    Given an authorized user
    And a trainer workload request with past training date
    When the client sends POST request to apply workload event
    Then the response status should be 400

  Scenario: Get monthly summary successfully
    Given an authorized user
    And a valid trainer workload request
    When the client sends POST request to apply workload event
    And the client sends GET request for monthly summary
    Then the response status should be 200
    And the monthly summary username should be "trainer1"
    And the monthly summary total duration should be 60

  Scenario: Get yearly summary successfully
    Given an authorized user
    And a valid trainer workload request
    When the client sends POST request to apply workload event
    And the client sends GET request for yearly summary
    Then the response status should be 200
    And the yearly summary username should be "trainer1"

  Scenario: Get monthly summary without authorization
    Given no authorization token
    When the client sends GET request for missing monthly summary authorization
    Then the response status should be 403