package uk.gov.homeoffice.borders.workflow.identity

import com.github.tomakehurst.wiremock.junit.WireMockRule
import com.github.tomjankes.wiremock.WireMockGroovy
import org.junit.Rule
import org.springframework.web.client.RestTemplate
import spock.lang.Specification
import uk.gov.homeoffice.borders.workflow.PlatformDataUrlBuilder

class TeamServiceSpec extends Specification {

    def wmPort = 8182

    @Rule
    WireMockRule wireMockRule = new WireMockRule(wmPort)

    def wireMockStub = new WireMockGroovy(wmPort)


    def platformDataUrlBuilder = new PlatformDataUrlBuilder('http://localhost:8182')
    def teamService = new TeamService(new RestTemplate(), platformDataUrlBuilder)


    def 'can find by id'() {
        given:
        wireMockStub.stub {
            request {
                method 'GET'
                url '/team?teamcode=eq.teamcode'
            }

            response {
                status 200
                body """ [
                            {
                                "teamid" : "id",
                                "teamcode" : "teamcode",
                                "teamname" : "teamname"
                            }
                         ]
                     """
                headers {
                    "Content-Type" "application/vnd.pgrst.object+json"
                }
            }

        }
        when:
        def result = teamService.findById("teamcode")

        then:
        result
        result.name == 'teamname'
        result.teamCode == 'teamcode'
    }

    def 'can find by query'() {
        given:
        wireMockStub.stub {
            request {
                method 'GET'
                url '/team?teamname=eq.teamname'
            }

            response {
                status 200
                body """ [
                            {
                                "teamid" : "id",
                                "teamcode" : "teamcode",
                                "teamname" : "teamname"
                            }
                         ]
                     """
                headers {
                    "Content-Type" "application/vnd.pgrst.object+json"
                }
            }

        }
        when:
        def teamQuery = new TeamQuery().groupName("teamname")
        def result = teamService.findByQuery(teamQuery)

        then:
        result
        result.size() == 1
        result[0].name == 'teamname'

    }
}