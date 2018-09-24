package org.simple.clinic.medicalhistory.newentry

import android.content.Context
import android.support.v7.widget.Toolbar
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.Button
import android.widget.RelativeLayout
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.schedulers.Schedulers.io
import kotterknife.bindView
import org.simple.clinic.R
import org.simple.clinic.activity.TheActivity
import org.simple.clinic.medicalhistory.MedicalHistoryQuestion
import org.simple.clinic.medicalhistory.MedicalHistoryQuestionView
import org.simple.clinic.router.screen.ScreenRouter
import org.simple.clinic.summary.PatientSummaryCaller
import org.simple.clinic.summary.PatientSummaryScreen
import org.simple.clinic.widgets.UiEvent
import java.util.UUID
import javax.inject.Inject

class NewMedicalHistoryScreen(context: Context, attrs: AttributeSet) : RelativeLayout(context, attrs) {

  companion object {
    val KEY = ::NewMedicalHistoryScreenKey
  }

  @Inject
  lateinit var controller: NewMedicalHistoryScreenController

  @Inject
  lateinit var screenRouter: ScreenRouter

  private val toolbar by bindView<Toolbar>(R.id.newmedicalhistory_toolbar)
  private val heartAttackQuestionView by bindView<MedicalHistoryQuestionView>(R.id.newmedicalhistory_question_heartattack)
  private val strokeQuestionView by bindView<MedicalHistoryQuestionView>(R.id.newmedicalhistory_question_stroke)
  private val kidneyDiseaseQuestionView by bindView<MedicalHistoryQuestionView>(R.id.newmedicalhistory_question_kidney)
  private val hypertensionQuestionView by bindView<MedicalHistoryQuestionView>(R.id.newmedicalhistory_question_hypertension)
  private val diabetesQuestionView by bindView<MedicalHistoryQuestionView>(R.id.newmedicalhistory_question_diabetes)
  private val noneQuestionView by bindView<MedicalHistoryQuestionView>(R.id.newmedicalhistory_question_none)
  private val nextButtonFrame by bindView<ViewGroup>(R.id.newmedicalhistory_next_frame)
  private val saveButton by bindView<Button>(R.id.newmedicalhistory_save)

  override fun onFinishInflate() {
    super.onFinishInflate()
    if (isInEditMode) {
      return
    }
    TheActivity.component.inject(this)

    toolbar.setNavigationOnClickListener {
      screenRouter.pop()
    }

    heartAttackQuestionView.render(MedicalHistoryQuestion.HAS_HAD_A_HEART_ATTACK)
    strokeQuestionView.render(MedicalHistoryQuestion.HAS_HAD_A_STROKE)
    kidneyDiseaseQuestionView.render(MedicalHistoryQuestion.HAS_HAD_A_KIDNEY_DISEASE)
    hypertensionQuestionView.render(MedicalHistoryQuestion.IS_ON_TREATMENT_FOR_HYPERTENSION)
    diabetesQuestionView.render(MedicalHistoryQuestion.HAS_DIABETES)
    noneQuestionView.render(MedicalHistoryQuestion.NONE)

    Observable.mergeArray(screenCreates(), answerToggles(), saveClicks())
        .observeOn(io())
        .compose(controller)
        .observeOn(mainThread())
        .takeUntil(RxView.detaches(this))
        .subscribe { it(this) }
  }

  private fun screenCreates(): Observable<UiEvent> {
    val screenKey = screenRouter.key<NewMedicalHistoryScreenKey>(this)
    return Observable.just(NewMedicalHistoryScreenCreated(screenKey.patientUuid))
  }

  private fun answerToggles(): ObservableSource<UiEvent> {
    return Observable.mergeArray(
        heartAttackQuestionView.toggles,
        strokeQuestionView.toggles,
        kidneyDiseaseQuestionView.toggles,
        hypertensionQuestionView.toggles,
        diabetesQuestionView.toggles,
        noneQuestionView.toggles)
  }

  private fun saveClicks() =
      RxView
          .clicks(saveButton)
          .map { SaveMedicalHistoryClicked() }

  fun unSelectNoneAnswer() {
    noneQuestionView.checkBox.isChecked = false
  }

  fun unSelectAllAnswersExceptNone() {
    heartAttackQuestionView.checkBox.isChecked = false
    strokeQuestionView.checkBox.isChecked = false
    kidneyDiseaseQuestionView.checkBox.isChecked = false
    hypertensionQuestionView.checkBox.isChecked = false
    diabetesQuestionView.checkBox.isChecked = false
  }

  fun openPatientSummaryScreen(patientUuid: UUID) {
    screenRouter.push(PatientSummaryScreen.KEY(patientUuid, PatientSummaryCaller.NEW_PATIENT))
  }

  fun setSaveButtonEnabled(enabled: Boolean) {
    nextButtonFrame.setBackgroundResource(when {
      enabled -> R.color.newmedicalhistory_save_button_frame_enabled
      else -> R.color.newmedicalhistory_save_button_frame_disabled
    })
    saveButton.isEnabled = enabled
  }
}