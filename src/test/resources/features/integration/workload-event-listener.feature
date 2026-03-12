Feature: Workload event listener integration

  Scenario: ADD event updates monthly summary
    Given trainer workload repositories are clean
    And a workload ADD event for trainer "john.doe" with duration 120
    When the workload listener consumes the event
    Then monthly summary for trainer "john.doe" should have total duration 120

  Scenario: DELETE event decreases monthly summary
    Given trainer workload repositories are clean
    And an existing monthly summary created by ADD event for trainer "john.doe" with duration 180
    And a workload DELETE event for trainer "john.doe" with duration 60
    When the workload listener consumes the event
    Then monthly summary for trainer "john.doe" should have total duration 120

  Scenario: duplicate event id is ignored
    Given trainer workload repositories are clean
    And a workload ADD event with event id "event-123" for trainer "john.doe" with duration 90
    When the workload listener consumes the event
    And the same workload listener event is consumed again
    Then monthly summary for trainer "john.doe" should have total duration 90

  Scenario: invalid payload without event id fails
    Given trainer workload repositories are clean
    And a workload event without event id for trainer "john.doe" with duration 60
    When the workload listener consumes the invalid event
    Then listener processing should fail