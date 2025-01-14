package org.simple.clinic.patient.sync

import io.reactivex.Single
import org.simple.clinic.sync.DataPushResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface PatientSyncApi {

  @POST("patients/sync")
  fun push(
      @Body body: PatientPushRequest
  ): Single<DataPushResponse>

  @Headers(value = ["X-RESYNC-TOKEN: 1"])
  @GET("patients/sync")
  fun pull(
      @Query("limit") recordsToPull: Int,
      @Query("process_token") lastPullTimestamp: String? = null
  ): Single<PatientPullResponse>
}
