package org.simple.clinic.scanid

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxbinding2.widget.RxTextView
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.screen_scan_simple.view.*
import org.simple.clinic.R
import org.simple.clinic.activity.TheActivity
import org.simple.clinic.addidtopatient.searchforpatient.AddIdToPatientSearchScreenKey
import org.simple.clinic.bindUiToController
import org.simple.clinic.patient.businessid.Identifier
import org.simple.clinic.router.screen.RouterDirection
import org.simple.clinic.router.screen.ScreenRouter
import org.simple.clinic.summary.OpenIntention
import org.simple.clinic.summary.PatientSummaryScreenKey
import org.simple.clinic.util.UtcClock
import org.simple.clinic.widgets.ScreenDestroyed
import org.simple.clinic.widgets.UiEvent
import org.simple.clinic.widgets.hideKeyboard
import org.threeten.bp.Instant
import java.util.UUID
import javax.inject.Inject

class ScanSimpleIdScreen(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {

  @Inject
  lateinit var screenRouter: ScreenRouter

  @Inject
  lateinit var controller: ScanSimpleIdScreenController

  @Inject
  lateinit var utcClock: UtcClock

  private val keyboardEvents = PublishSubject.create<ScanSimpleIdScreenEvent>()

  override fun onFinishInflate() {
    super.onFinishInflate()
    if (isInEditMode) {
      return
    }
    TheActivity.component.inject(this)
    // It is possible that going back via the app bar from future screens will come back to this
    // screen with the keyboard open. So, we hide it here.
    hideKeyboard()
    toolBar.setNavigationOnClickListener { screenRouter.pop() }

    bindUiToController(
        ui = this,
        events = Observable.mergeArray(qrScans(), keyboardEvents, doneClicks()),
        controller = controller,
        screenDestroys = RxView.detaches(this).map { ScreenDestroyed() }
    )
  }

  private fun qrScans(): Observable<UiEvent> {
    return qrCodeScannerView
        .scans()
        .map(::ScanSimpleIdScreenQrCodeScanned)
  }

  private fun doneClicks(): Observable<UiEvent> {
    return RxTextView
        .editorActionEvents(shortCodeText)
        .map { ShortCodeSearched(ShortCodeInput(shortCodeText.text.toString())) }
  }

  fun openPatientSummary(patientUuid: UUID) {
    screenRouter.popAndPush(
        PatientSummaryScreenKey(
            patientUuid = patientUuid,
            intention = OpenIntention.ViewExistingPatient,
            screenCreatedTimestamp = Instant.now(utcClock)
        ),
        RouterDirection.FORWARD
    )
  }

  fun openAddIdToPatientScreen(identifier: Identifier) {
    screenRouter.popAndPush(AddIdToPatientSearchScreenKey(identifier), RouterDirection.FORWARD)
  }

  fun showShortCodeValidationError() {
    shortCodeErrorText.visibility = View.VISIBLE
    shortCodeErrorText.text = resources.getString(R.string.scansimpleid_shortcode_error)
  }

  fun hideShortCodeValidationError() {
    shortCodeErrorText.visibility = View.GONE
  }

  fun openPatientShortCodeSearch(validShortCode: String) {
    TODO("not implemented")
  }

  fun hideQrCodeScannerView() {
    qrCodeScannerView.hideQrCodeScanner()
  }

  fun showQrCodeScannerView() {
    qrCodeScannerView.showQrCodeScanner()
  }
}
