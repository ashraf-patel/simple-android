package org.simple.clinic.registration.register

import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.ObservableTransformer
import io.reactivex.rxkotlin.ofType
import org.simple.clinic.registration.RegistrationResult.NetworkError
import org.simple.clinic.registration.RegistrationResult.Success
import org.simple.clinic.registration.RegistrationResult.UnexpectedError
import org.simple.clinic.user.UserSession
import org.simple.clinic.widgets.ScreenCreated
import org.simple.clinic.widgets.UiEvent
import javax.inject.Inject

typealias Ui = RegistrationLoadingScreen
typealias UiChange = (Ui) -> Unit

class RegistrationLoadingScreenController @Inject constructor(
    private val userSession: UserSession
) : ObservableTransformer<UiEvent, UiChange> {

  override fun apply(events: Observable<UiEvent>): ObservableSource<UiChange> {
    return registerOnStart(events)
  }

  private fun registerOnStart(events: Observable<UiEvent>): Observable<UiChange> {

    val retryClicks = events.ofType<RegisterErrorRetryClicked>()
    val creates = events.ofType<ScreenCreated>()

    val register = Observable
        .merge(creates, retryClicks)
        .flatMap {
          userSession
              .register()
              .toObservable()
        }
        .replay()
        .refCount()

    val clearOngoingEntry = register
        .filter { it is Success }
        .flatMap {
          userSession.clearOngoingRegistrationEntry()
              .andThen(Observable.never<UiChange>())
        }

    val showScreenChanges = register.map {
      { ui: Ui ->
        when (it) {
          Success -> ui.openHomeScreen()
          NetworkError -> ui.showNetworkError()
          UnexpectedError -> ui.showUnexpectedError()
        }
      }
    }
    return showScreenChanges.mergeWith(clearOngoingEntry)
  }
}
