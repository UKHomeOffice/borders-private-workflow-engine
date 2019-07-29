package uk.gov.homeoffice.borders.workflow.identity;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.identity.Group;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import uk.gov.homeoffice.borders.workflow.PlatformDataUrlBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Slf4j
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class TeamService {

    private RestTemplate restTemplate;
    private PlatformDataUrlBuilder platformDataUrlBuilder;

    public Team findById(String teamId) {
        HttpHeaders httpHeaders = new HttpHeaders();
        HttpEntity httpEntity = new HttpEntity(httpHeaders);
        ResponseEntity<List<Team>> response = restTemplate.exchange(platformDataUrlBuilder.teamById(teamId),
                HttpMethod.GET, httpEntity, new ParameterizedTypeReference<List<Team>>() {});
        return response.getStatusCode().is2xxSuccessful() && !response.getBody().isEmpty() ? response.getBody().get(0) : null;

    }

    public List<Group> findByQuery(TeamQuery query) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        List<Team> teams = restTemplate.exchange(
                platformDataUrlBuilder.teamQuery(query),
                HttpMethod.GET,
                new HttpEntity<>(httpHeaders),
                new ParameterizedTypeReference<List<Team>>() {
                },
                new HashMap<>()
        ).getBody();
        return new ArrayList<>(teams);
    }
}
