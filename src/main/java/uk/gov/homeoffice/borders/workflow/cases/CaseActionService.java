package uk.gov.homeoffice.borders.workflow.cases;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.dmn.engine.DmnDecisionResult;
import org.camunda.bpm.engine.DecisionService;
import org.camunda.bpm.engine.FormService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.rest.dto.repository.ProcessDefinitionDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.homeoffice.borders.workflow.identity.PlatformUser;
import uk.gov.homeoffice.borders.workflow.process.ProcessDefinitionDtoResource;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

@Slf4j
@Service
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class CaseActionService {

    private RepositoryService repositoryService;
    private FormService formService;
    private DecisionService decisionService;

    public List<CaseDetail.Action> getAvailableActions(CaseDetail caseDetail, PlatformUser platformUser) {
        final CaseDetail.Action defaultAction = defaultPdfAction(caseDetail, platformUser);
        try {
        final DmnDecisionResult result;

            final List<String> caseProcessKeys = caseDetail.getProcessInstances()
                    .stream().map(CaseDetail.ProcessInstanceReference::getKey).collect(toList());
            result = decisionService.evaluateDecisionByKey("caseActions")
                    .variables(Map.of("caseProcessKeys", caseProcessKeys,
                            "platformUser", platformUser)).evaluate();

            if (result.isEmpty()) {
                return List.of(defaultAction);
            }

        final List<String> actionProcessKeys = result.getResultList().stream()
                .map(decisionResult ->
                        decisionResult.get("actionProcessKey").toString())
                .collect(toList());

        final List<CaseDetail.Action> actions = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKeysIn(
                        actionProcessKeys.toArray(new String[]{})
                ).latestVersion()
                .list()
                .stream()
                .map(processDefinition -> {
                    CaseDetail.Action pdf = new CaseDetail.Action();
                    pdf.setCompletionMessage(String.format("%s successfully triggered",
                            processDefinition.getName()));
                    String startFormKey = formService.getStartFormKey(processDefinition.getId());
                    ProcessDefinitionDtoResource
                            dtoResource = new ProcessDefinitionDtoResource();
                    dtoResource.setFormKey(startFormKey);
                    dtoResource.setProcessDefinitionDto(ProcessDefinitionDto.fromProcessDefinition(processDefinition));
                    pdf.setProcess(dtoResource);
                    return pdf;
                }).collect(toList());

        actions.addAll(0, List.of(defaultAction));

        return actions;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Failed to perform action rules '{}'...returning default action", e.getMessage());
            return List.of(defaultAction);
        }
    }


    private CaseDetail.Action defaultPdfAction(CaseDetail caseDetail, PlatformUser platformUser) {
        CaseDetail.Action pdf = new CaseDetail.Action();
        pdf.setCompletionMessage("Your pdf will be sent to your email.");

        ProcessDefinition processDefinition = repositoryService
                .createProcessDefinitionQuery()
                .latestVersion()
                .processDefinitionKey("generate-case-pdf").singleResult();

        ProcessDefinitionDtoResource
                dtoResource = new ProcessDefinitionDtoResource();


        if (processDefinition.hasStartFormKey()) {
            String startFormKey = formService.getStartFormKey(processDefinition.getId());
            dtoResource.setFormKey(startFormKey);
        }

        dtoResource.setProcessDefinitionDto(ProcessDefinitionDto.fromProcessDefinition(processDefinition));
        pdf.setProcess(dtoResource);
        return pdf;
    }
}
