package org.simple.clinic.phone

import io.reactivex.Completable
import io.reactivex.Observable
import org.simple.clinic.activity.TheActivity
import javax.inject.Inject

class PhoneCaller @Inject constructor(
    private val configProvider: Observable<PhoneNumberMaskerConfig>,
    private val activity: TheActivity
) {

  fun normalCall(number: String, dialer: Dialer): Completable =
      Completable.fromAction {
        dialer.call(context = activity, phoneNumber = number)
      }

  fun secureCall(numberToMask: String, dialer: Dialer): Completable =
      configProvider
          .map { config -> maskNumber(config, numberToMask) }
          .flatMapCompletable { maskedNumber -> normalCall(maskedNumber, dialer) }

  private fun maskNumber(config: PhoneNumberMaskerConfig, numberToMask: String): String {
    val proxyNumber = config.proxyPhoneNumber

    val stopCharacter = "#"
    val dtmfTones = "$numberToMask$stopCharacter"
    return "$proxyNumber,$dtmfTones"
  }
}