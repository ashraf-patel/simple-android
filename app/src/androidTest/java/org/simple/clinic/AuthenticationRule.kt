package org.simple.clinic

import com.google.common.truth.Truth.assertThat
import io.bloco.faker.Faker
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.simple.clinic.facility.FacilitySyncApi
import org.simple.clinic.patient.SyncStatus
import org.simple.clinic.registration.RegistrationResult
import org.simple.clinic.user.User
import org.simple.clinic.user.UserSession
import org.simple.clinic.user.UserStatus
import javax.inject.Inject

/** Runs every test with an authenticated user.
 *
 **/
class AuthenticationRule : TestRule {

  @Inject
  lateinit var userSession: UserSession

  @Inject
  lateinit var testData: TestData

  @Inject
  lateinit var facilityApi: FacilitySyncApi

  @Inject
  lateinit var faker: Faker

  @Inject
  lateinit var appDatabase: AppDatabase

  override fun apply(base: Statement, description: Description): Statement {
    return object : Statement() {
      override fun evaluate() {
        TestClinicApp.appComponent().inject(this@AuthenticationRule)

        try {
          // Login also needs to happen inside this try block so that in case
          // of a failure, logout() still gets called to reset all app data.
          register()
          base.evaluate()

        } finally {
          logout()
        }
      }
    }
  }

  private fun register() {
    val facilities = facilityApi.pull(10)
        .map { it.payloads }
        .map { facilities -> facilities.map { it.toDatabaseModel(SyncStatus.DONE) } }
        .blockingGet()
    appDatabase.facilityDao().save(facilities)

    val registerFacilityAt = facilities.first()

    while (true) {
      val registrationEntry = testData.ongoingRegistrationEntry(
          phoneNumber = faker.number.number(10),
          pin = testData.qaUserPin(),
          registrationFacility = registerFacilityAt)

      val registrationResult = userSession.saveOngoingRegistrationEntry(registrationEntry)
          .andThen(userSession.saveOngoingRegistrationEntryAsUser())
          .andThen(userSession.register())
          .blockingGet()

      if (registrationResult is RegistrationResult.Success) {
        break
      }
    }

    val (accessToken) = userSession.accessToken()
    assertThat(accessToken).isNotNull()

    val (loggedInUser) = userSession.loggedInUser().blockingFirst()
    assertThat(userSession.isUserLoggedIn()).isTrue()
    assertThat(loggedInUser!!.status).isEqualTo(UserStatus.ApprovedForSyncing)
    assertThat(loggedInUser.loggedInStatus).isEqualTo(User.LoggedInStatus.LOGGED_IN)
  }

  private fun logout() {
    appDatabase.clearAllTables()
  }
}
