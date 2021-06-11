package uk.gov.homeoffice.borders.workflow.process;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.AuthorizationService;
import org.camunda.bpm.engine.authorization.Authorization;
import org.camunda.bpm.engine.authorization.Permissions;
import org.camunda.bpm.engine.authorization.Resources;
import org.camunda.bpm.engine.delegate.Expression;
import org.camunda.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener;
import org.camunda.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.camunda.bpm.engine.impl.util.xml.Element;
import uk.gov.homeoffice.borders.workflow.exception.InternalWorkflowException;

import java.util.Arrays;
import java.util.List;

@Slf4j
public class ProcessDefinitionAuthorizationParser {

    private AuthorizationService authorizationService;

    public ProcessDefinitionAuthorizationParser(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    public void parseProcess(ProcessDefinitionEntity processDefinition) {
        log.info("Processing authorization for '{}'", processDefinition.getKey());
        if (processDefinition.getKey() == null) {
            throw new InternalWorkflowException("Process definition " + processDefinition.getName() + " does not have a key");
        }

        List<Authorization> auth = authorizationService.createAuthorizationQuery()
                .resourceId(processDefinition.getKey())
                .list();

        if (auth != null && auth.size() > 0 ) {
                auth.forEach(a -> {
                    authorizationService.deleteAuthorization(a.getId());
                    log.info("Deleted authorization for {}", processDefinition.getKey());
                });
        }

        if (processDefinition.getCandidateStarterGroupIdExpressions().isEmpty()) {
            log.warn("Process definition " + processDefinition.getKey() + " does not have any candidate groups defined. " +
                    "This will not be visible to end users");
            return;
        }

        processDefinition
                .getCandidateStarterGroupIdExpressions().stream()
                .map((Expression::getExpressionText))
                .filter(StringUtils::isNotBlank)
                .map((code) -> code.split(","))
                .flatMap(Arrays::stream)
                .filter(code -> !code.equalsIgnoreCase("STAFF"))
                .map((code) -> {
                    Authorization newAuthorization = authorizationService
                            .createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
                    newAuthorization.setGroupId(code);
                    newAuthorization.setResource(Resources.PROCESS_DEFINITION);
                    newAuthorization.setResourceId(processDefinition.getKey());
                    newAuthorization.addPermission(Permissions.READ);
                    newAuthorization.addPermission(Permissions.CREATE_INSTANCE);
                    newAuthorization.addPermission(Permissions.READ_HISTORY);
                    log.info("Authorization for {} = {}", processDefinition.getKey(), code);
                    return newAuthorization;
                }).forEach((authorization -> {
                    authorizationService.saveAuthorization(authorization);
                    log.info("Created authorization for {}", processDefinition.getKey());
                }));


    }


}
