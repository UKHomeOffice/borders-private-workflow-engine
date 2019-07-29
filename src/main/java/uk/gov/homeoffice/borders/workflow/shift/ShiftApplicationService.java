package uk.gov.homeoffice.borders.workflow.shift;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.runtime.VariableInstance;
import org.camunda.spin.Spin;
import org.camunda.spin.impl.json.jackson.format.JacksonJsonDataFormat;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;
import uk.gov.homeoffice.borders.workflow.PlatformDataUrlBuilder;
import uk.gov.homeoffice.borders.workflow.RefDataUrlBuilder;
import uk.gov.homeoffice.borders.workflow.exception.ResourceNotFound;
import uk.gov.homeoffice.borders.workflow.identity.PlatformUser.ShiftDetails;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.*;

/**
 * Application Service responsible for dealing with the internal
 * workflow for creating an active shift
 */

@Slf4j
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class ShiftApplicationService {

    private RuntimeService runtimeService;

    private RestTemplate restTemplate;

    private PlatformDataUrlBuilder platformDataUrlBuilder;
    private RefDataUrlBuilder refDataUrlBuilder;

    private JacksonJsonDataFormat formatter;

    /**
     * Start a shift workflow
     *
     * @param shiftInfo Contains information about a shift
     * @return processInstance created
     * @see ShiftDetails
     * @see ProcessInstance
     */
    ProcessInstance startShift(@NotNull @Valid ShiftDetails shiftInfo) {

        String email = shiftInfo.getEmail();
        log.info("Starting a request to start a shift for '{}'", email);

        deleteShift(email, "new-shift");

        setEndTime(shiftInfo);

        Spin<?> shiftVariableObject = Spin.S(shiftInfo, formatter);

        Map<String, Object> variables = new HashMap<>();
        variables.put("shiftInfo", shiftVariableObject);
        variables.put("type", "non-notification");

        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey("activate-shift", email, variables);
        log.info("Shift process for '{}' has started '{}'", email, processInstance.getProcessInstanceId());
        return processInstance;
    }

    private void setEndTime(ShiftDetails shiftInfo) {
        Integer shiftHours = shiftInfo.getShiftHours();
        Integer shiftMinutes = shiftInfo.getShiftMinutes();
        Date startTime = new DateTime(shiftInfo.getStartDateTime())
                .withZone(org.joda.time.DateTimeZone.UTC).toDate();
        shiftInfo.setEndDateTime(new DateTime(startTime)
                .plusHours(shiftHours)
                .plusMinutes(shiftMinutes)
                .withZone(org.joda.time.DateTimeZone.UTC)
                .toDate());
    }

    /**
     * Deletes a workflow with the given email
     *
     * @param email        identifies the shift that needs to be deleted
     * @param deleteReason This is required and explains why the workflow was cancelled.
     * @see ProcessInstance
     */
    @CacheEvict(cacheNames = {"shifts"}, key = "#email")
    public void deleteShift(@NotNull String email, @NotNull String deleteReason) {

        List<ProcessInstance> instances = runtimeService.createProcessInstanceQuery()
                .processDefinitionKey("activate-shift")
                .processInstanceBusinessKey(email).list();
        HttpHeaders headers = new HttpHeaders();

        if (!CollectionUtils.isEmpty(instances)) {
            List<String> ids = instances.stream()
                    .map(ProcessInstance::getProcessInstanceId)
                    .collect(toList());
            List<VariableInstance> shifts = runtimeService.createVariableInstanceQuery()
                    .variableNameIn("shiftId", "shiftHistoryId")
                    .processInstanceIdIn(ids.toArray(new String[]{})).list()
                    .stream()
                    .filter(Objects::nonNull)
                    .collect(toList());

            shifts.stream().filter(variableInstance -> variableInstance.getName().equalsIgnoreCase("shiftId")
                    || variableInstance.getName().equalsIgnoreCase("shiftHistoryId"))
                    .collect(Collectors.groupingBy(VariableInstance::getName))
                    .forEach((key, values) -> {
                        if (key.equalsIgnoreCase("shiftId")) {
                            values.stream().map(v ->(String) v.getValue())
                                    .forEach(shiftId -> {
                                        restTemplate.exchange(platformDataUrlBuilder.shiftUrlById(shiftId),
                                                HttpMethod.DELETE, new HttpEntity<>(headers), String.class);
                                        log.info("Deleted shift with id {}", shiftId);
                                    });
                        }
                        if (key.equalsIgnoreCase("shiftHistoryId")) {
                            values.stream().map(v ->(String) v.getValue())
                                    .forEach(shiftHistoryId -> {
                                        HttpEntity<Map> body = new HttpEntity<>(Collections.singletonMap("enddatetime", new Date()), headers);
                                        restTemplate.exchange(platformDataUrlBuilder.shiftHistoryById(shiftHistoryId), HttpMethod.PATCH, body, String.class);
                                        log.info("Updated shift history with id {}", shiftHistoryId);
                                    });

                        }
                    });

            runtimeService.deleteProcessInstances(ids, deleteReason, false, true);

            log.info("Shift deleted for '{}'", email);
        } else {
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            List<ShiftDetails> shifts = restTemplate
                    .exchange(platformDataUrlBuilder.shiftUrlByEmail(email), HttpMethod.GET, new HttpEntity<>(headers),
                            new ParameterizedTypeReference<List<ShiftDetails>>() {
                            }).getBody();

            if (!CollectionUtils.isEmpty(shifts)) {
                ResponseEntity<String> response = restTemplate.exchange(platformDataUrlBuilder.shiftUrlById(shifts.get(0).getShiftId()),
                        HttpMethod.DELETE, new HttpEntity<>(headers), String.class);
                log.info("No process instance found but deleted from platform data...shift {}", response.getStatusCode());

            }

        }

    }


    /**
     * Get shift info for given email
     *
     * @throws ResourceNotFound shift info cannot be found
     * @see ShiftDetails
     */
    ShiftDetails getShiftInfo(@NotNull String email) {
        ProcessInstance shift = runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(email)
                .singleResult();

        if (shift == null) {
            throw new ResourceNotFound("Shift detail not found for '" + email + "'");
        }

        VariableInstance variableInstance = runtimeService.createVariableInstanceQuery()
                .processInstanceIdIn(shift.getProcessInstanceId())
                .variableName("shiftInfo").singleResult();

        return ofNullable(variableInstance).map(variable -> {
            ShiftDetails shiftInfo = Spin.S(variable.getValue(), formatter).mapTo(ShiftDetails.class);
            HttpHeaders httpHeaders = new HttpHeaders();
            HttpEntity<?> entity = new HttpEntity<>(httpHeaders);

            ResponseEntity<Map<String, String>> response = restTemplate
                    .exchange(refDataUrlBuilder.getLocation(shiftInfo.getLocationId()),
                            HttpMethod.GET,
                            entity,
                            new ParameterizedTypeReference<Map<String, String>>() {});

            if (response.getStatusCode().is2xxSuccessful()) {
                String locationName = ofNullable(response.getBody())
                        .orElse(Collections.singletonMap("locationname", "Unknown")).get("locationname");
                shiftInfo.setCurrentLocationName(locationName);
            }
            return shiftInfo;
        }).orElseThrow(() -> new ResourceNotFound("Shift data could not be found"));

    }


}
