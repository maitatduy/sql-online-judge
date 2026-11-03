package com.sqljudge.problem.integration;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

final class StudioFixtures {

    private static final String PROBLEMS = "/api/studio/problems";
    private static final String DATASETS = "/api/studio/datasets";
    private static final String SCHEMA =
            """
            {"tables": [
              {"name": "departments", "columns": [
                {"name": "id", "type": "INTEGER", "isPrimaryKey": true, "isNullable": false},
                {"name": "name", "type": "TEXT", "isPrimaryKey": false, "isNullable": false}]}
            ]}
            """;

    private final TestRestTemplate restTemplate;
    private final String token;

    StudioFixtures(TestRestTemplate restTemplate, String token) {
        this.restTemplate = restTemplate;
        this.token = token;
    }

    String createProblem(String title) {
        return idOf(send(HttpMethod.POST, PROBLEMS, "{\"title\":\"" + title + "\"}"));
    }

    String createReadyDataset(String name) {
        String id = idOf(send(HttpMethod.POST, DATASETS, "{\"name\":\"" + name + "\"}"));
        send(HttpMethod.PUT, DATASETS + "/" + id + "/schema", SCHEMA);
        addVariant(id, "Mẫu", true);
        return id;
    }

    String addVariant(String datasetId, String name, boolean isPublic) {
        List<String> variantIds =
                JsonPath.parse(
                                send(
                                                HttpMethod.POST,
                                                DATASETS + "/" + datasetId + "/variants",
                                                "{\"name\":\""
                                                        + name
                                                        + "\",\"isPublic\":"
                                                        + isPublic
                                                        + "}")
                                        .getBody())
                        .read("$.variants[*].id");
        String variantId = variantIds.get(variantIds.size() - 1);
        send(
                HttpMethod.PUT,
                DATASETS + "/" + datasetId + "/variants/" + variantId + "/tables/departments",
                "{\"csv\":\"id,name\\n1,Kỹ thuật\\n\"}");
        return variantId;
    }

    List<String> variantIdsOf(String datasetId) {
        return JsonPath.parse(send(HttpMethod.GET, DATASETS + "/" + datasetId, null).getBody())
                .read("$.variants[*].id");
    }

    void chooseDataset(String problemId, String datasetId) {
        send(
                HttpMethod.PUT,
                PROBLEMS + "/" + problemId + "/dataset",
                "{\"datasetId\":\"" + datasetId + "\"}");
    }

    ResponseEntity<String> send(HttpMethod method, String path, String body) {
        return send(method, path, body, token);
    }

    ResponseEntity<String> send(HttpMethod method, String path, String body, String bearer) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearer);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(path, method, new HttpEntity<>(body, headers), String.class);
    }

    private static String idOf(ResponseEntity<String> response) {
        return JsonPath.parse(response.getBody()).read("$.id", String.class);
    }
}
