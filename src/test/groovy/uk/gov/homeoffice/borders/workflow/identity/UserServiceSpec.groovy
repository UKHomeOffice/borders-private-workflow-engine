package uk.gov.homeoffice.borders.workflow.identity

import com.github.tomakehurst.wiremock.junit.WireMockRule
import com.github.tomjankes.wiremock.WireMockGroovy
import org.junit.Rule
import org.springframework.web.client.RestTemplate
import spock.lang.Specification
import uk.gov.homeoffice.borders.workflow.PlatformDataUrlBuilder
import uk.gov.homeoffice.borders.workflow.config.PlatformDataBean
import uk.gov.homeoffice.borders.workflow.exception.InternalWorkflowException

class UserServiceSpec extends Specification {

    def wmPort = 8911

    @Rule
    WireMockRule wireMockRule = new WireMockRule(wmPort)

    def wireMockStub = new WireMockGroovy(wmPort)
    def userService

    def setup() {
        def platformDataBean = new PlatformDataBean()
        platformDataBean.url = new URI("http://localhost:8911")
        def platformDataUrlBuilder = new PlatformDataUrlBuilder(platformDataBean)
        def teamService = Mock(TeamService)
        userService = new UserService(new RestTemplate(), platformDataUrlBuilder, teamService)
        userService.self = userService
    }

    def 'can find user by id'() {
        given:
        wireMockStub.stub {
            request {
                method 'GET'
                url '/v1/shift?email=eq.email'
            }

            response {
                status 200
                body """ [
                            {
                                "shiftid" : "id",
                                "staffid" : "staffid",
                                "teamid" : "teamid",
                                "phone" : "phone",
                                "email" : "email"
                              }
                         ]
                     """
                headers {
                    "Content-Type" "application/json"
                }
            }

        }

        wireMockStub.stub {
            request {
                method 'POST'
                url '/v1/rpc/staffdetails'
            }
            response {
                status: 200
                body """
                       [{
                          "phone": "phone",
                          "email": "email",
                          "gradeid": "gradeid",
                          "firstname": "firstname",
                          "surname": "surname",
                          "linemanagerid": "linemanagerid",
                          "roles" : [
                            "systemuser"
                          ],
                          "qualificationtypes": [
                            {
                              "qualificationname": "dummy",
                              "qualificationtype": "1"
                            },
                            {
                              "qualificationname": "staff",
                              "qualificationtype": "2"
                            }
                          ],
                          "staffid": "staffid"
                        }]
                     """
                headers {
                    "Content-Type" "application/json"
                }
            }
        }

        wireMockStub.stub {
            request {
                method 'POST'
                url '/v1/rpc/teamchildren'

            }
            response {
                status: 200
                body """
                       [
                          {
                            "id": "teamid",
                            "parentteamid": null,
                            "name": "teamname",
                            "code": "teamcode"
                          }
                        ]
                     """
                headers {
                    "Content-Type" "application/json"
                }
            }
        }



        when:
        def result = userService.findByUserId("email")

        then:
        result
        result.email == 'email'

    }

    def 'can find by team id'() {
        given:
        wireMockStub.stub {
            request {
                method 'GET'
                url '/v1/shift?teamid=eq.teamId'
            }

            response {
                status 200
                body """ [
                            {
                                "shiftid" : "id",
                                "staffid" : "staffid",
                                "teamid" : "teamId",
                                "phone" : "phone",
                                "email" : "email"
                              }
                         ]
                     """
                headers {
                    "Content-Type" "application/json"
                }
            }

        }

        wireMockStub.stub {
            request {
                method 'GET'
                url '/v1/staffview?staffid=in.(staffid)'
            }
            response {
                status: 200
                body """
                       [{
                          "phone": "phone",
                          "email": "email",
                          "gradeid": "gradeid",
                          "firstname": "firstname",
                          "surname": "surname",
                          "qualificationtypes": [
                            {
                              "qualificationname": "dummy",
                              "qualificationtype": "1"
                            },
                            {
                              "qualificationname": "staff",
                              "qualificationtype": "2"
                            }
                          ],
                          "staffid": "staffid"
                        }]
                     """
                headers {
                    "Content-Type" "application/json"
                }
            }
        }

        when:
        def query = new UserQuery()
        query.memberOfGroup("teamId")
        def result = userService.findByQuery(query)


        then:
        result
        !result.isEmpty()
    }

    def 'can find by location'() {
        given:
        wireMockStub.stub {
            request {
                method 'GET'
                url '/v1/shift?locationid=eq.locationId'
            }

            response {
                status 200
                body """ [
                            {
                                "shiftid" : "id",
                                "staffid" : "staffid",
                                "teamid" : "teamId",
                                "phone" : "phone",
                                "email" : "email",
                                "locationid": "locationId"
                              }
                         ]
                     """
                headers {
                    "Content-Type" "application/json"
                }
            }

        }

        wireMockStub.stub {
            request {
                method 'GET'
                url '/v1/staffview?staffid=in.(staffid)'
            }
            response {
                status: 200
                body """
                       [{
                          "phone": "phone",
                          "email": "email",
                          "gradeid": "gradeid",
                          "firstname": "firstname",
                          "surname": "surname",
                          "qualificationtypes": [
                            {
                              "qualificationname": "dummy",
                              "qualificationtype": "1"
                            },
                            {
                              "qualificationname": "staff",
                              "qualificationtype": "2"
                            }
                          ],
                          "staffid": "staffid",
                          "gradename": "grade"
                        }]
                     """
                headers {
                    "Content-Type" "application/json"
                }
            }
        }

        when:
        def query = new UserQuery()
        query.location("locationId")
        def result = userService.findByQuery(query)


        then:
        result
        !result.isEmpty()
    }

    def 'illegal argument thrown if url is empty or null'() {
        when:
        def query = new UserQuery()
        userService.findByQuery(query)


        then:
        thrown(IllegalArgumentException)
    }

    def 'can find by user id in query'() {
        given:
        wireMockStub.stub {
            request {
                method 'GET'
                url '/v1/shift?email=eq.email'
            }

            response {
                status 200
                body """ [
                            {
                                "shiftid" : "id",
                                "staffid" : "staffid",
                                "teamid" : "teamid",
                                "phone" : "phone",
                                "email" : "email"
                              }
                         ]
                     """
                headers {
                    "Content-Type" "application/json"
                }
            }

        }

        wireMockStub.stub {
            request {
                method 'POST'
                url '/v1/rpc/staffdetails'
            }
            response {
                status: 200
                body """
                       [{
                          "phone": "phone",
                          "email": "email",
                          "gradeid": "gradeid",
                          "firstname": "firstname",
                          "surname": "surname",
                          "qualificationtypes": [
                            {
                              "qualificationname": "dummy",
                              "qualificationtype": "1"
                            },
                            {
                              "qualificationname": "staff",
                              "qualificationtype": "2"
                            }
                          ],
                          "staffid": "staffid",
                          "gradename": "grade"
                        }]
                     """
                headers {
                    "Content-Type" "application/json"
                }
            }
        }

        wireMockStub.stub {
            request {
                method 'POST'
                url '/v1/rpc/teamchildren'

            }
            response {
                status: 200
                body """
                       [
                          {
                            "id": "teamid",
                            "parentteamid": null,
                            "name": "teamname",
                            "code": "teamcode"
                          }
                        ]
                     """
                headers {
                    "Content-Type" "application/json"
                }
            }
        }


        when:
        def query = new UserQuery()
        query.userId("email")
        def result = userService.findByQuery(query)


        then:
        result
        !result.isEmpty()
    }

    def 'no shift info returned if remote service throws Exception'() {
        given:
        wireMockStub.stub {
            request {
                method 'GET'
                url '/v1/shift?email=eq.email'
            }

            response {
                status 504
                headers {
                    "Content-Type" "application/json"
                }
            }

        }
        when:
        def result = userService.findByUserId("email")

        then:
        !result

    }

    def 'no shift info returned if remote service returns empty result'() {
        given:
        wireMockStub.stub {
            request {
                method 'GET'
                url '/v1/shift?email=eq.email'
            }

            response {
                status 200
                body '''[]'''
                headers {
                    "Content-Type" "application/json"
                }
            }

        }
        when:
        def result = userService.findByUserId("email")

        then:
        !result

    }

    def 'exception thrown if staff details cannot be located'() {
        given:
        wireMockStub.stub {
            request {
                method 'GET'
                url '/v1/shift?email=eq.email'
            }

            response {
                status 200
                body """ [
                            {
                                "shiftid" : "id",
                                "staffid" : "staffid",
                                "teamid" : "teamid",
                                "phone" : "phone",
                                "email" : "email"
                              }
                         ]
                     """
                headers {
                    "Content-Type" "application/json"
                }
            }

        }

        wireMockStub.stub {
            request {
                method 'POST'
                url '/v1/rpc/staffdetails'
            }
            response {
                status: 200
                headers {
                    "Content-Type" "application/json"
                }
            }
        }

        when:
        def query = new UserQuery()
        query.userId("email")
        userService.findByQuery(query)


        then:
        thrown(InternalWorkflowException)
    }

}
