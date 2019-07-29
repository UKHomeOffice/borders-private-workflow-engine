package uk.gov.homeoffice.borders.workflow.identity

import com.github.tomakehurst.wiremock.junit.WireMockRule
import com.github.tomjankes.wiremock.WireMockGroovy
import org.junit.Rule
import org.springframework.web.client.RestTemplate
import spock.lang.Specification
import uk.gov.homeoffice.borders.workflow.PlatformDataUrlBuilder
import uk.gov.homeoffice.borders.workflow.config.PlatformDataBean

class TeamServiceSpec extends Specification {

    def wmPort = 8182

    @Rule
    WireMockRule wireMockRule = new WireMockRule(wmPort)

    def wireMockStub = new WireMockGroovy(wmPort)
    def teamService

    def setup() {
        def platformDataBean = new PlatformDataBean()
        platformDataBean.url=new URI("http://localhost:8182")
        def platformDataUrlBuilder = new PlatformDataUrlBuilder(platformDataBean)
        teamService = new TeamService(new RestTemplate(), platformDataUrlBuilder)
    }

    def 'can find by id'() {
        given:
        wireMockStub.stub {
            request {
                method 'GET'
                url '/v1/team?code=eq.code'
            }

            response {
                status 200
                body """ [
                            {
                                "id" : "id",
                                "code" : "code",
                                "name" : "teamname"
                            }
                         ]
                     """
                headers {
                }
            }

        }
        when:
        def result = teamService.findById("code")

        then:
        result
        result.name == 'teamname'
        result.code == 'code'
    }

    def 'can find by query'() {
        given:
        wireMockStub.stub {
            request {
                method 'GET'
                url '/v1/team?name=eq.name'
            }

            response {
                status 200
                body """ [
                            {
                                "id" : "id",
                                "code" : "code",
                                "name" : "teamname"
                            }
                         ]
                     """
                headers {
                }
            }

        }
        when:
        def teamQuery = new TeamQuery().groupName("name")
        def result = teamService.findByQuery(teamQuery)

        then:
        result
        result.size() == 1
        result[0].name == 'teamname'

    }
}
