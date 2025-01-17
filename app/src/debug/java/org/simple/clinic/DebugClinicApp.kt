package org.simple.clinic

import android.annotation.SuppressLint
import android.app.Activity
import com.facebook.stetho.Stetho
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.squareup.leakcanary.LeakCanary
import com.tspoon.traceur.Traceur
import io.github.inflationx.viewpump.ViewPump
import io.reactivex.Single
import org.simple.clinic.activity.TheActivity
import org.simple.clinic.crash.CrashReporterModule
import org.simple.clinic.crash.NoOpCrashReporter
import org.simple.clinic.di.AppComponent
import org.simple.clinic.di.AppModule
import org.simple.clinic.di.DaggerDebugAppComponent
import org.simple.clinic.di.DebugAppComponent
import org.simple.clinic.di.NetworkModule
import org.simple.clinic.facility.FacilityRepository
import org.simple.clinic.login.LoginModule
import org.simple.clinic.login.applock.AppLockConfig
import org.simple.clinic.remoteconfig.FirebaseRemoteConfigCacheExpiration
import org.simple.clinic.security.pin.BruteForceProtectionConfig
import org.simple.clinic.security.pin.BruteForceProtectionModule
import org.simple.clinic.sync.SyncScheduler
import org.simple.clinic.sync.indicator.SyncIndicatorStatusCalculator
import org.simple.clinic.user.UserSession
import org.simple.clinic.util.AppSignature
import org.simple.clinic.widgets.ProxySystemKeyboardEnterToImeOption
import org.simple.clinic.activity.SimpleActivityLifecycleCallbacks
import org.threeten.bp.Duration
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@SuppressLint("Registered")
class DebugClinicApp : ClinicApp() {

  @Inject
  lateinit var syncScheduler: SyncScheduler

  private lateinit var signature: AppSignature

  @Inject
  lateinit var syncIndicatorStatusCalculator: SyncIndicatorStatusCalculator

  companion object {
    fun appComponent(): DebugAppComponent {
      return ClinicApp.appComponent as DebugAppComponent
    }
  }

  override fun onCreate() {
    if (LeakCanary.isInAnalyzerProcess(this)) {
      return
    }
    Traceur.enableLogging()
    super.onCreate()
    LeakCanary.install(this)

    appComponent().inject(this)

    Timber.plant(Timber.DebugTree())
    Stetho.initializeWithDefaults(this)
    syncScheduler.schedule().subscribe()
    syncIndicatorStatusCalculator.updateSyncResults()
    showDebugNotification()

    ViewPump.init(ViewPump.builder()
        .addInterceptor(ProxySystemKeyboardEnterToImeOption())
        .build())

    signature = AppSignature(this)
  }

  private fun showDebugNotification() {
    registerActivityLifecycleCallbacks(object : SimpleActivityLifecycleCallbacks() {
      override fun onActivityStarted(activity: Activity) {
        if (activity is TheActivity) {
          DebugNotification.show(activity, signature.appSignatures)
        }
      }

      override fun onActivityStopped(activity: Activity) {
        if (activity is TheActivity) {
          DebugNotification.stop(activity)
        }
      }
    })
  }

  override fun buildDaggerGraph(): AppComponent {
    return DaggerDebugAppComponent.builder()
        .appModule(AppModule(this))
        .loginModule(object : LoginModule() {
          override fun appLockConfig(): Single<AppLockConfig> {
            return Single.just(AppLockConfig(lockAfterTimeMillis = TimeUnit.SECONDS.toMillis(4)))
          }
        })
        .crashReporterModule(object : CrashReporterModule() {
          override fun crashReporter(
              userSession: UserSession,
              facilityRepository: FacilityRepository
          ) = NoOpCrashReporter()
        })
        .bruteForceProtectionModule(object : BruteForceProtectionModule() {
          override fun config(): Single<BruteForceProtectionConfig> {
            return super.config().map {
              it.copy(blockDuration = Duration.ofSeconds(5))
            }
          }
        })
        .networkModule(object : NetworkModule() {
          override fun remoteConfig(): FirebaseRemoteConfig {
            return super.remoteConfig().apply {
              // Enable developer mode so that calls are not throttled during dev.
              // More details in FirebaseRemoteConfigCacheExpiration.
              setConfigSettings(FirebaseRemoteConfigSettings.Builder()
                  .setDeveloperModeEnabled(true)
                  .build())
            }
          }

          override fun remoteConfigCacheExpiration() = FirebaseRemoteConfigCacheExpiration.DEBUG
        })
        .build()
  }
}
