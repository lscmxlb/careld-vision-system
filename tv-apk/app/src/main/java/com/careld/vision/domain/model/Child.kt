package com.careld.vision.domain.model

/**
 * Child domain model
 */
data class Child(
    val id: Long,
    val childId: String,
    val name: String,
    val phone: String?,
    val birthDate: String?,
    val gender: Gender?,
    val medicalHistory: String?,
    val age: Int
) {
    enum class Gender {
        FEMALE, MALE
    }

    companion object {
        fun fromGenderCode(code: Int?): Gender? {
            return when (code) {
                0 -> Gender.FEMALE
                1 -> Gender.MALE
                else -> null
            }
        }

        fun toGenderCode(gender: Gender?): Int? {
            return when (gender) {
                Gender.FEMALE -> 0
                Gender.MALE -> 1
                null -> null
            }
        }
    }

    fun getGenderDisplay(): String {
        return when (gender) {
            Gender.FEMALE -> "女"
            Gender.MALE -> "男"
            null -> "未知"
        }
    }
}
