package org.simple.clinic.medicalhistory

import androidx.annotation.VisibleForTesting
import androidx.room.TypeConverter
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import org.simple.clinic.util.SafeEnumTypeAdapter

sealed class Answer {

  object Yes : Answer()

  object No: Answer()

  object Unanswered: Answer()

  data class Unknown(val actualValue: String): Answer()

  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  object TypeAdapter: SafeEnumTypeAdapter<Answer>(
      knownMappings = mapOf(
          Yes to "yes",
          No to "no",
          // The actual value of the Unanswered enum should be "unanswered",
          // but the current api representation is "unknown". This is a little
          // confusing because we use Unknown as the convention for values which
          // this app version doesn't know about yet.
          // TODO 16-Jul-19 : See if it's possible to change this in a future api version
          Unanswered to "unknown"
      ),
      unknownStringToEnumConverter = { Unknown(it) },
      unknownEnumToStringConverter = { (it as Unknown).actualValue }
  )

  class RoomTypeConverter {

    @TypeConverter
    fun toEnum(value: String?): Answer? = TypeAdapter.toEnum(value)

    @TypeConverter
    fun fromEnum(answer: Answer): String? = TypeAdapter.fromEnum(answer)
  }

  class MoshiTypeAdapter {

    @FromJson
    fun fromJson(value: String?): Answer? = TypeAdapter.toEnum(value)

    @ToJson
    fun toJson(answer: Answer): String? = TypeAdapter.fromEnum(answer)
  }
}
