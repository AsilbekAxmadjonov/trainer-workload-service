package com.discovery.workload.cucumber.component.steps;

import com.discovery.workload.cucumber.component.jwt.TestJwtFactory;
import com.discovery.workload.dto.TrainerWorkloadRequest;
import com.discovery.workload.model.ActionType;
import com.discovery.workload.mongoDb.repository.ProcessedEventMongoRepository;
import com.discovery.workload.mongoDb.repository.TrainerTrainingSummaryMongoRepository;
import io.cucumber.java.Before;
import io.cucumber.java.en.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class WorkloadComponentSteps {

    @LocalServerPort
    private int port;

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private TrainerTrainingSummaryMongoRepository trainerRepository;

    @Autowired
    private ProcessedEventMongoRepository processedEventRepository;

    private String token;
    private TrainerWorkloadRequest request;
    private ResponseEntity<String> stringResponse;
    private ResponseEntity<Map> jsonResponse;

    private LocalDate scenarioTrainingDate;
    private int scenarioYear;
    private int scenarioMonth;

    public WorkloadComponentSteps() {
        restTemplate.setErrorHandler(new org.springframework.web.client.DefaultResponseErrorHandler() {
            @Override
            protected boolean hasError(org.springframework.http.HttpStatusCode statusCode) {
                return false;
            }
        });
    }

    @Before
    public void cleanDb() {
        trainerRepository.deleteAll();
        processedEventRepository.deleteAll();

        token = null;
        request = null;
        stringResponse = null;
        jsonResponse = null;

        scenarioTrainingDate = LocalDate.now().plusDays(1);
        scenarioYear = scenarioTrainingDate.getYear();
        scenarioMonth = scenarioTrainingDate.getMonthValue();
    }

    @Given("an authorized user")
    public void an_authorized_user() {
        token = TestJwtFactory.userToken();
    }

    @Given("no authorization token")
    public void no_authorization_token() {
        token = null;
    }

    @Given("a valid trainer workload request")
    public void a_valid_trainer_workload_request() {
        request = TrainerWorkloadRequest.builder()
                .trainingId("training-1")
                .username("trainer1")
                .firstName("John")
                .lastName("Doe")
                .isActive(true)
                .trainingDate(scenarioTrainingDate)
                .trainingDurationMinutes(60)
                .actionType(ActionType.ADD)
                .build();
    }

    @Given("a trainer workload request without username")
    public void a_trainer_workload_request_without_username() {
        request = TrainerWorkloadRequest.builder()
                .trainingId("training-1")
                .username("")
                .firstName("John")
                .lastName("Doe")
                .isActive(true)
                .trainingDate(scenarioTrainingDate)
                .trainingDurationMinutes(60)
                .actionType(ActionType.ADD)
                .build();
    }

    @Given("a trainer workload request with past training date")
    public void a_trainer_workload_request_with_past_training_date() {
        request = TrainerWorkloadRequest.builder()
                .trainingId("training-1")
                .username("trainer1")
                .firstName("John")
                .lastName("Doe")
                .isActive(true)
                .trainingDate(LocalDate.now().minusDays(1))
                .trainingDurationMinutes(60)
                .actionType(ActionType.ADD)
                .build();
    }

    @When("the client sends POST request to apply workload event")
    public void the_client_sends_post_request_to_apply_workload_event() {
        HttpHeaders headers = authorizedHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Event-Id", "evt-1");

        HttpEntity<TrainerWorkloadRequest> entity = new HttpEntity<>(request, headers);

        stringResponse = restTemplate.exchange(
                baseUrl() + "/api/v1/workloads/events",
                HttpMethod.POST,
                entity,
                String.class
        );
    }

    @When("the client sends GET request for monthly summary")
    public void the_client_sends_get_request_for_monthly_summary() {
        HttpEntity<Void> entity = new HttpEntity<>(authorizedHeaders());

        jsonResponse = restTemplate.exchange(
                baseUrl() + "/api/v1/workloads/trainer1/" + scenarioYear + "/" + scenarioMonth,
                HttpMethod.GET,
                entity,
                Map.class
        );
    }

    @When("the client sends GET request for yearly summary")
    public void the_client_sends_get_request_for_yearly_summary() {
        HttpEntity<Void> entity = new HttpEntity<>(authorizedHeaders());

        jsonResponse = restTemplate.exchange(
                baseUrl() + "/api/v1/workloads/trainers/trainer1/summary",
                HttpMethod.GET,
                entity,
                Map.class
        );
    }

    @When("the client sends GET request for missing monthly summary authorization")
    public void the_client_sends_get_request_for_missing_monthly_summary_authorization() {
        HttpEntity<Void> entity = new HttpEntity<>(new HttpHeaders());

        stringResponse = restTemplate.exchange(
                baseUrl() + "/api/v1/workloads/trainer1/" + scenarioYear + "/" + scenarioMonth,
                HttpMethod.GET,
                entity,
                String.class
        );
    }

    @Then("the response status should be {int}")
    public void the_response_status_should_be(Integer status) {
        if (stringResponse != null) {
            assertThat(stringResponse.getStatusCode().value()).isEqualTo(status);
        } else {
            assertThat(jsonResponse.getStatusCode().value()).isEqualTo(status);
        }
    }

    @Then("the response body should contain {string}")
    public void the_response_body_should_contain(String text) {
        assertThat(stringResponse.getBody()).contains(text);
    }

    @Then("the monthly summary username should be {string}")
    public void the_monthly_summary_username_should_be(String username) {
        assertThat(jsonResponse.getBody()).isNotNull();
        assertThat(jsonResponse.getBody().get("username")).isEqualTo(username);
    }

    @Then("the monthly summary total duration should be {int}")
    public void the_monthly_summary_total_duration_should_be(Integer total) {
        assertThat(jsonResponse.getBody()).isNotNull();
        Object value = jsonResponse.getBody().get("totalDurationMinutes");
        assertThat(((Number) value).intValue()).isEqualTo(total);
    }

    @Then("the yearly summary username should be {string}")
    public void the_yearly_summary_username_should_be(String username) {
        assertThat(jsonResponse.getBody()).isNotNull();
        assertThat(jsonResponse.getBody().get("trainerUsername")).isEqualTo(username);
    }

    private HttpHeaders authorizedHeaders() {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return headers;
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }
}