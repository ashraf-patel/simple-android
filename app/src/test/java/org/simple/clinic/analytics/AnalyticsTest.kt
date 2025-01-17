package org.simple.clinic.analytics

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.simple.clinic.analytics.MockAnalyticsReporter.Event
import org.threeten.bp.Duration
import org.threeten.bp.Instant
import java.util.UUID

class AnalyticsTest {

  @After
  fun tearDown() {
    Analytics.clearReporters()
  }

  @Test
  fun `when reporting interaction events without any reporters, no error should be thrown`() {
    Analytics.reportUserInteraction("Test")
  }

  @Test
  fun `when reporting screen change events without any reporters, no error should be thrown`() {
    Analytics.reportScreenChange("Screen 1", "Screen 2")
  }

  @Test
  fun `when reporting input validation errors without any reporters, no error should be thrown`() {
    Analytics.reportInputValidationError("Error")
  }

  @Test
  fun `when setting the user id without any reporters, no error should be thrown`() {
    Analytics.setUserId(UUID.randomUUID())
  }

  @Test
  fun `when reporting an audit event without any reporters, no error should be thrown`() {
    Analytics.reportViewedPatient(UUID.randomUUID(), "Test")
  }

  @Test
  fun `when reporting a network call without any reporters, no error should be thrown`() {
    Analytics.reportNetworkCall(
        url = "test",
        method = "get",
        responseCode = 1,
        contentLength = 1,
        durationMillis = 1)
  }

  @Test
  fun `when reporting a network timeout without any reporters, no error should be thrown`() {
    Analytics.reportNetworkTimeout(
        url = "test",
        method = "get",
        metered = true,
        networkTransportType = Analytics.NetworkTransportType.WIFI,
        downstreamBandwidthKbps = 100,
        upstreamBandwidthKbps = 100)
  }

  @Test
  fun `when reporting a time taken event without any reporters, no error should be thrown`() {
    Analytics.reportTimeTaken(
        operationName = "test",
        timeTaken = Duration.ofMillis(500L))
  }

  @Test
  fun `when reporting a data cleared event without any reporters, no error should be thrown`() {
    Analytics.reportDataCleared(
        patientCount = 1,
        bloodPressureCount = 1,
        appointmentCount = 1,
        prescribedDrugCount = 1,
        medicalHistoryCount = 1,
        since = Instant.EPOCH
    )
  }

  @Test
  fun `when clearing the user identity without any reporters, no error should be thrown`() {
    Analytics.clearUserId()
  }

  @Test
  fun `when a reporter fails when sending interaction events, no error should be thrown`() {
    Analytics.addReporter(FailingAnalyticsReporter())
    Analytics.reportUserInteraction("Test")
  }

  @Test
  fun `when a reporter fails when sending screen change events, no error should be thrown`() {
    Analytics.addReporter(FailingAnalyticsReporter())
    Analytics.reportScreenChange("Screen 1", "Screen 2")
  }

  @Test
  fun `when a reporter fails when sending validation error events, no error should be thrown`() {
    Analytics.addReporter(FailingAnalyticsReporter())
    Analytics.reportInputValidationError("Error")
  }

  @Test
  fun `when a reporter fails when setting the user id, no  error should be thrown`() {
    Analytics.addReporter(FailingAnalyticsReporter())
    Analytics.setUserId(UUID.randomUUID())
  }

  @Test
  fun `when a reporter fails when sending an audit event, no error should be thrown`() {
    Analytics.addReporter(FailingAnalyticsReporter())
    Analytics.reportViewedPatient(UUID.randomUUID(), "Test")
  }

  @Test
  fun `when a reporter fails sending a network event, no error should be thrown`() {
    Analytics.addReporter(FailingAnalyticsReporter())
    Analytics.reportNetworkCall("test", "get", 1, 1, 1)
  }

  @Test
  fun `when a reporter fails sending a network timeout event, no error should be thrown`() {
    Analytics.addReporter(FailingAnalyticsReporter())
    Analytics.reportNetworkTimeout(
        url = "test",
        method = "get",
        metered = true,
        networkTransportType = Analytics.NetworkTransportType.WIFI,
        downstreamBandwidthKbps = 100,
        upstreamBandwidthKbps = 100)
  }

  @Test
  fun `when a reporter fails sending a time taken event, no error should be thrown`() {
    Analytics.addReporter(FailingAnalyticsReporter())
    Analytics.reportTimeTaken("test", Duration.ofMillis(100L))
  }

  @Test
  fun `when a reporter fails sending a data cleared event, no error should be thrown`() {
    Analytics.addReporter(FailingAnalyticsReporter())
    Analytics.reportDataCleared(
        patientCount = 1,
        bloodPressureCount = 1,
        appointmentCount = 1,
        prescribedDrugCount = 1,
        medicalHistoryCount = 1,
        since = Instant.EPOCH
    )
  }

  @Test
  fun `when a reporter fails clearing the user id, no  error should be thrown`() {
    Analytics.addReporter(FailingAnalyticsReporter())
    Analytics.clearUserId()
  }

  @Test
  fun `when setting the user id, the property must also be set on the reporters`() {
    val reporter = MockAnalyticsReporter()
    Analytics.addReporter(reporter)

    val uuid = UUID.randomUUID()
    Analytics.setUserId(uuid)
    assertThat(reporter.userId).isEqualTo(uuid.toString())
  }

  @Test
  fun `when clearing the user id, the user ID must be cleared from the reporters`() {
    val reporter = MockAnalyticsReporter()
    Analytics.addReporter(reporter)

    val uuid = UUID.randomUUID()
    Analytics.setUserId(uuid)
    assertThat(reporter.userId).isEqualTo(uuid.toString())
    Analytics.clearUserId()
    assertThat(reporter.userId).isNull()
  }

  @Test
  fun `when multiple reporters are present and one throws an error, the user id must be set on the others`() {
    val reporter1 = MockAnalyticsReporter()
    val reporter2 = FailingAnalyticsReporter()
    val reporter3 = MockAnalyticsReporter()

    Analytics.addReporter(reporter1, reporter2, reporter3)
    val userId = UUID.randomUUID()
    Analytics.setUserId(userId)

    assertThat(reporter1.userId).isEqualTo(userId.toString())
    assertThat(reporter3.userId).isEqualTo(userId.toString())

    Analytics.clearUserId()

    assertThat(reporter1.userId).isNull()
    assertThat(reporter3.userId).isNull()
  }

  @Test
  fun `when multiple reporters are present and one throws an error, the others should receive the events`() {
    val reporter1 = MockAnalyticsReporter()
    val reporter2 = FailingAnalyticsReporter()
    val reporter3 = MockAnalyticsReporter()

    Analytics.addReporter(reporter1, reporter2, reporter3)

    val uuid1 = UUID.randomUUID()
    val uuid2 = UUID.randomUUID()

    Analytics.reportUserInteraction("Test 1")
    Analytics.reportUserInteraction("Test 2")
    Analytics.reportTimeTaken("Operation 1", Duration.ofMillis(500L))
    Analytics.reportUserInteraction("Test 3")
    Analytics.reportScreenChange("Screen 1", "Screen 2")
    Analytics.reportInputValidationError("Error 1")
    Analytics.reportInputValidationError("Error 2")
    Analytics.reportTimeTaken("Operation 2", Duration.ofMinutes(1L).plusMillis(750L))
    Analytics.reportNetworkCall("Test 1", "GET", 200, 500, 400)
    Analytics.reportNetworkTimeout(
        url = "Test 1",
        method = "GET",
        metered = true,
        networkTransportType = Analytics.NetworkTransportType.WIFI,
        downstreamBandwidthKbps = 100,
        upstreamBandwidthKbps = 50)
    Analytics.reportViewedPatient(uuid1, "Test 2")
    Analytics.reportViewedPatient(uuid2, "Test 1")
    Analytics.reportNetworkCall("Test 2", "POST", 400, 1000, 300)
    Analytics.reportNetworkTimeout(
        url = "Test 3",
        method = "POST",
        metered = false,
        networkTransportType = Analytics.NetworkTransportType.CELLULAR,
        downstreamBandwidthKbps = 50,
        upstreamBandwidthKbps = 100)
    Analytics.reportTimeTaken("Operation 1", Duration.ofHours(3L).plusMinutes(30L).plusMillis(1L))
    Analytics.reportDataCleared(
        patientCount = 1,
        bloodPressureCount = 2,
        appointmentCount = 3,
        prescribedDrugCount = 4,
        medicalHistoryCount = 5,
        since = Instant.parse("2018-12-03T10:15:30.00Z")
    )

    val expected = listOf(
        Event("UserInteraction", mapOf("name" to "Test 1")),
        Event("UserInteraction", mapOf("name" to "Test 2")),
        Event("TimeTaken", mapOf(
            "operationName" to "Operation 1",
            "timeTakenInMillis" to 500L)
        ),
        Event("UserInteraction", mapOf("name" to "Test 3")),
        Event("ScreenChange", mapOf("outgoing" to "Screen 1", "incoming" to "Screen 2")),
        Event("InputValidationError", mapOf("name" to "Error 1")),
        Event("InputValidationError", mapOf("name" to "Error 2")),
        Event("TimeTaken", mapOf(
            "operationName" to "Operation 2",
            "timeTakenInMillis" to 60750L)),
        Event("NetworkCall", mapOf(
            "url" to "Test 1", "method" to "GET", "responseCode" to 200, "contentLength" to 500, "durationMs" to 400)
        ),
        Event("NetworkTimeout", mapOf(
            "url" to "Test 1",
            "method" to "GET",
            "metered" to true,
            "transport" to Analytics.NetworkTransportType.WIFI,
            "downstreamKbps" to 100,
            "upstreamKbps" to 50)
        ),
        Event("ViewedPatient", mapOf("patientId" to uuid1.toString(), "from" to "Test 2")),
        Event("ViewedPatient", mapOf("patientId" to uuid2.toString(), "from" to "Test 1")),
        Event("NetworkCall", mapOf(
            "url" to "Test 2", "method" to "POST", "responseCode" to 400, "contentLength" to 1000, "durationMs" to 300)
        ),
        Event("NetworkTimeout", mapOf(
            "url" to "Test 3",
            "method" to "POST",
            "metered" to false,
            "transport" to Analytics.NetworkTransportType.CELLULAR,
            "downstreamKbps" to 50,
            "upstreamKbps" to 100)
        ),
        Event("TimeTaken", mapOf(
            "operationName" to "Operation 1",
            "timeTakenInMillis" to 12600001L)
        ),
        Event("DataCleared", mapOf(
            "pendingPatientCount" to 1,
            "pendingBpCount" to 2,
            "pendingAppointmentCount" to 3,
            "pendingPrescribedDrugCount" to 4,
            "pendingMedicalHistoryCount" to 5,
            "since" to "2018-12-03T10:15:30Z"
        ))
    )

    assertThat(reporter1.receivedEvents).isEqualTo(expected)
    assertThat(reporter3.receivedEvents).isEqualTo(expected)
  }

  private class FailingAnalyticsReporter : AnalyticsReporter {

    override fun setUserIdentity(id: String) {
      throw RuntimeException()
    }

    override fun resetUserIdentity() {
      throw RuntimeException()
    }

    override fun createEvent(event: String, props: Map<String, Any>) {
      throw RuntimeException()
    }
  }
}
