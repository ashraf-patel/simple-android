package org.resolvetosavelives.red.sync

import android.app.Application
import dagger.Module
import dagger.Provides
import io.reactivex.Single
import org.resolvetosavelives.red.R
import org.resolvetosavelives.red.bp.BloodPressureModule
import org.resolvetosavelives.red.sync.patient.PatientSyncModule
import org.threeten.bp.Duration
import retrofit2.Retrofit
import javax.inject.Named

@Module(includes = [PatientSyncModule::class, BloodPressureModule::class])
open class SyncModule {

  @Provides
  @Named("RedApp")
  fun retrofit(appContext: Application, commonRetrofitBuilder: Retrofit.Builder): Retrofit {
    val baseUrl = appContext.getString(R.string.redapp_endpoint)
    return commonRetrofitBuilder
        .baseUrl(baseUrl)
        .build()
  }

  @Provides
  open fun syncConfig(): Single<SyncConfig> {
    // In the future, this may come from the server.
    return Single.just(SyncConfig(frequency = Duration.ofHours(1), batchSize = 50))
  }
}
