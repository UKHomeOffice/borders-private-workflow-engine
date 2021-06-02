package uk.gov.homeoffice.borders.workflow.cases


import org.springframework.beans.factory.annotation.Autowired
import uk.gov.homeoffice.borders.workflow.BaseSpec
import uk.gov.homeoffice.borders.workflow.identity.PlatformUser

class CaseActionServiceSpec extends BaseSpec {

    @Autowired
    private CaseActionService caseActionService


    def 'can get actions'() {
        given: 'a user and case'
        def platformUser = new PlatformUser()
        platformUser.roles = ['special-role']

        def caseDetails = new CaseDetail()
        def instance = new CaseDetail.ProcessInstanceReference()
        instance.key = 'test-process'
        caseDetails.processInstances = [instance]

        when: 'action invoked'
        def result = caseActionService.getAvailableActions(caseDetails, platformUser)

        then: 'there should be 2 actions'
        result.size() == 2

        and: 'the first action should be generate-case-pdf'
        result.first().process.processDefinitionDto.key == 'generate-case-pdf'

    }

    def 'returns default action if decision returns no result'() {
        given: 'a user and case'
        def platformUser = new PlatformUser()
        platformUser.roles = ['normal-role']

        def caseDetails = new CaseDetail()
        def instance = new CaseDetail.ProcessInstanceReference()
        instance.key = 'test-process'
        caseDetails.processInstances = [instance]

        when: 'action invoked'
        def result = caseActionService.getAvailableActions(caseDetails, platformUser)

        then: 'there should be 1 actions'
        result.size() == 1
        result.first().process.processDefinitionDto.key == 'generate-case-pdf'
    }

    def 'returns default action process does not match'() {
        given: 'a user and case'
        def platformUser = new PlatformUser()
        platformUser.roles = ['special-role']

        def caseDetails = new CaseDetail()
        def instance = new CaseDetail.ProcessInstanceReference()
        instance.key = 'non-compliant'
        caseDetails.processInstances = [instance]

        when: 'action invoked'
        def result = caseActionService.getAvailableActions(caseDetails, platformUser)

        then: 'there should be 1 actions'
        result.size() == 1
        result.first().process.processDefinitionDto.key == 'generate-case-pdf'
    }

    def 'returns default action if service fails to evaluate rules'() {
        given: 'a user and case'
        def platformUser = new PlatformUser()
        platformUser.roles = ['special-role']

        and: 'case process instances are null'
        def caseDetails = new CaseDetail()
        caseDetails.processInstances = null

        when: 'action invoked'
        def result = caseActionService.getAvailableActions(caseDetails, platformUser)

        then: 'there should be 1 actions'
        result.size() == 1
        result.first().process.processDefinitionDto.key == 'generate-case-pdf'
    }
}
