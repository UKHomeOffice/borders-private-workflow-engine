package uk.gov.homeoffice.borders.workflow.shift


import org.joda.time.LocalDateTime
import org.springframework.http.MediaType
import spock.lang.Title
import uk.gov.homeoffice.borders.workflow.BaseSpec
import uk.gov.homeoffice.borders.workflow.identity.PlatformUser
import uk.gov.homeoffice.borders.workflow.identity.Team
import uk.gov.homeoffice.borders.workflow.security.WorkflowAuthentication

import static com.github.tomakehurst.wiremock.http.Response.response
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@Title("Shift API Spec")
class ShiftApiControllerSpec extends BaseSpec {



    def 'can create a shift at /api/workflow/shift'() {
        given:
        def shift = createActiveShift()

        and:
        deleteShift()

        when:
        def result = mvc.perform(post('/api/workflow/shift')
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(shift))
                .header("nginxId", "correlationId"))

        then:
        result.andExpect(status().isCreated())

    }

    private deleteShift() {
        wireMockStub.stub {
            request {
                method 'GET'
                url '/v1/shift?email=eq.testEmail'
                headers {
                    "nginxId" {
                        equalTo "correlationId"
                    }
                }

            }
            response {
                status 200
                body """
                        [{
                          "shiftid": "shiftId",
                          "enddatetime": "2018-11-06T16:23:26"
                        }]
                     """
                headers {
                    "Content-Type" "application/json"
                }
            }
        }
        wireMockStub.stub {
            request {
                method 'DELETE'
                url '/v1/shift?shiftid=eq.shiftId'
                headers {
                    "nginxId" {
                        equalTo "correlationId"
                    }
                }

            }
            response {
                status 204
                headers {
                    "Content-Type" "application/json"
                }
            }
        }
    }

     def "can get shift info at /api/workflow/shift"() {
         given:
         def shift = createActiveShift()

         and:
         deleteShift()
         logInUser()

         and:
         wireMockStub.stub {
             request {
                 method 'GET'
                 url '/location?locationid=eq.current'
                 headers {
                     "nginxId" {
                         equalTo "correlationId"
                     }
                 }

             }
             response {
                 status 200
                 headers {
                     "Content-Type" "application/json"
                 }
                 body '''
                         {
                          "locationname" : "current"
                         }
                     '''
             }
         }

         and:
         mvc.perform(post('/api/workflow/shift')
                 .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(shift)).header("nginxId", "correlationId"))

         when:
         def result = mvc.perform(get('/api/workflow/shift/testEmail')
                 .contentType(MediaType.APPLICATION_JSON).header("nginxId", "correlationId"))

         then:
         result.andExpect(status().is2xxSuccessful())
         PlatformUser.ShiftDetails shiftInfo = objectMapper.readValue(result.andReturn().response.contentAsString, PlatformUser.ShiftDetails)
         shiftInfo
         shiftInfo.getCurrentLocationName() == 'current'

     }

    def 'can delete a shift at /api/workflow/shift'() {
        given:
        def shift = createActiveShift()

        and:
        wireMockStub.stub {
            request {
                method 'DELETE'
                url '/v1/shift?shiftid=eq.xxxxx'
                headers {
                    "nginxId" {
                        equalTo "correlationId"
                    }
                }
            }
            response {
                status 200
            }
        }
        wireMockStub.stub {
            request {
                method 'PATCH'
                url '/v1/shifthistory?shifthistoryid=eq.xxxxx'
                headers {
                    "nginxId" {
                        equalTo "correlationId"
                    }
                }
            }
            response {
                status 200
            }
        }
        and:
        deleteShift()

        and:
        mvc.perform(post('/api/workflow/shift')
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(shift))
                .header("nginxId", "correlationId"))


        and:
        def instance = runtimeService.createProcessInstanceQuery().processInstanceBusinessKey(shift.email).singleResult()
        runtimeService.setVariable(instance.processInstanceId, "shiftId", "xxxxx")
        runtimeService.setVariable(instance.processInstanceId, "shiftHistoryId", "xxxxx")

        when:
        def result = mvc.perform(delete('/api/workflow/shift/testEmail?deletedReason=notNeeded')
                                .header("nginxId", "correlationId"))

        then:
        result.andExpect(status().is2xxSuccessful())
        def instances = runtimeService.createProcessInstanceQuery().processInstanceBusinessKey('testEmail')
                .list()
        instances.isEmpty()

    }

    def '4xx client error thrown if shift has no start time'() {
        given:
        def shiftInfo = new  PlatformUser.ShiftDetails()
        shiftInfo.setEmail("testEmail")
        shiftInfo.setStaffId(UUID.randomUUID().toString())
        shiftInfo.setTeamId("teamid")
        shiftInfo.setLocationId("locationid")
        shiftInfo.setPhone("phone")
        shiftInfo.setShiftHours(1)
        shiftInfo.setShiftMinutes(0)

        when:
        def result = mvc.perform(post('/api/workflow/shift')
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(shiftInfo)))

        then:
        result.andExpect(status().is4xxClientError())
    }

    def '4xx client error thrown if shift has locationid id'() {
        given:
        def shiftInfo = new  PlatformUser.ShiftDetails()
        shiftInfo.setEmail("testEmail")
        shiftInfo.setStaffId(UUID.randomUUID().toString())
        shiftInfo.setTeamId("teamid")
        shiftInfo.setPhone("phone")
        shiftInfo.setShiftHours(1)
        shiftInfo.setShiftMinutes(0)
        shiftInfo.setStartDateTime(LocalDateTime.now().toDate())

        when:
        def result = mvc.perform(post('/api/workflow/shift')
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(shiftInfo)))

        then:
        result.andExpect(status().is4xxClientError())
    }

    def '4xx client error thrown if shift has no team id'() {
        def shiftInfo = new  PlatformUser.ShiftDetails()
        shiftInfo.setEmail("testEmail")
        shiftInfo.setStaffId(UUID.randomUUID().toString())
        shiftInfo.setLocationId("locationid")
        shiftInfo.setLocationId("current")
        shiftInfo.setPhone("phone")
        shiftInfo.setShiftHours(1)
        shiftInfo.setShiftMinutes(0)
        shiftInfo.setStartDateTime(LocalDateTime.now().toDate())

        when:
        def result = mvc.perform(post('/api/workflow/shift')
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(shiftInfo)))

        then:
        result.andExpect(status().is4xxClientError())
    }
    def '4xx client error thrown if shift has no email'() {
        def shiftInfo = new  PlatformUser.ShiftDetails()
        shiftInfo.setStaffId(UUID.randomUUID().toString())
        shiftInfo.setTeamId("teamid")
        shiftInfo.setLocationId("locationid")
        shiftInfo.setLocationId("current")
        shiftInfo.setPhone("phone")
        shiftInfo.setShiftHours(1)
        shiftInfo.setShiftMinutes(0)
        shiftInfo.setStartDateTime(LocalDateTime.now().toDate())

        when:
        def result = mvc.perform(post('/api/workflow/shift')
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(shiftInfo)))

        then:
        result.andExpect(status().is4xxClientError())
    }

    def '4xx client error thrown if shift has no phone'() {
        def shiftInfo = new  PlatformUser.ShiftDetails()
        shiftInfo.setEmail("email")
        shiftInfo.setStaffId(UUID.randomUUID().toString())
        shiftInfo.setTeamId("teamid")
        shiftInfo.setLocationId("locationid")
        shiftInfo.setLocationId("current")
        shiftInfo.setShiftHours(1)
        shiftInfo.setShiftMinutes(0)
        shiftInfo.setStartDateTime(LocalDateTime.now().toDate())

        when:
        def result = mvc.perform(post('/api/workflow/shift')
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(shiftInfo)))

        then:
        result.andExpect(status().is4xxClientError())
    }

    PlatformUser logInUser() {
        def user = new PlatformUser()
        user.id = 'test'
        user.email = 'testEmail'

        def team = new Team()
        user.teams = []
        team.code = 'teamA'
        user.teams << team
        identityService.getCurrentAuthentication() >> new WorkflowAuthentication(user)
        user
    }

    PlatformUser.ShiftDetails createActiveShift() {
        PlatformUser.ShiftDetails shiftInfo = new  PlatformUser.ShiftDetails()
        shiftInfo.setTeamId("teamId")
        shiftInfo.setLocationId("location")
        shiftInfo.setPhone("phone")
        shiftInfo.setEmail("testEmail")
        shiftInfo.setStartDateTime(LocalDateTime.now().toDate())
        shiftInfo.setStaffId(UUID.randomUUID().toString())
        shiftInfo.setShiftHours(1)
        shiftInfo.setShiftMinutes(0)
        shiftInfo.setLocationId("current")
        shiftInfo
    }
}
