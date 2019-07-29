package uk.gov.homeoffice.borders.workflow


import spock.lang.Specification
import uk.gov.homeoffice.borders.workflow.config.PlatformDataBean
import uk.gov.homeoffice.borders.workflow.identity.TeamQuery

class PlatformDataUrlBuilderSpec extends Specification {

    def platformDataUrl = new URI('http://localhost:9000')
    def platformDataBean = new PlatformDataBean()

    def underTest

    def setup() {
        platformDataBean.url = platformDataUrl
        underTest = new PlatformDataUrlBuilder(platformDataBean)
    }


    def 'can get shift url by email'() {
        given:
        def email = 'myemail@host.com'

        when:
        def url = underTest.shiftUrlByEmail(email)

        then:
        url
        url == 'http://localhost:9000/v1/shift?email=eq.myemail@host.com'

    }

    def 'can get shift url by id'() {
        given:
        def id = 'uuid'

        when:
        def url = underTest.shiftUrlById(id)

        then:
        url
        url == 'http://localhost:9000/v1/shift?shiftid=eq.uuid'
    }
    def 'can get shift url by team id'() {
        given:
        def teamId = "teamId"

        when:
        def url = underTest.queryShiftByTeamId(teamId)

        then:
        url
        url == 'http://localhost:9000/v1/shift?teamid=eq.teamId'
    }

    def 'can get shift url by location id'() {
        given:
        def locationId = "locationId"

        when:
        def url = underTest.queryShiftByLocationId(locationId)

        then:
        url
        url == 'http://localhost:9000/v1/shift?locationid=eq.locationId'
    }


    def 'can get staff url'() {
       when:
        def url = underTest.getStaffUrl()

        then:
        url
        url == 'http://localhost:9000/v1/rpc/staffdetails'
    }

    def 'can get url for comments'() {
        when:
        def url = underTest.comments()

        then:
        url
        url == 'http://localhost:9000/v1/comment'
    }

    def 'can get url for comments for task'() {
        given:
        def taskId = 'taskId'

        when:
        def url = underTest.getCommentsById(taskId)

        then:
        url
        url == 'http://localhost:9000/v1/comment?taskid=eq.taskId&order=createdon.desc'
    }
}
