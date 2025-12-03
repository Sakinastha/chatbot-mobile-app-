package com.example.chatbotapp.data

data class CourseEntry(
    val code: String? = null,
    val title: String? = null,
    val credits: Double? = null,
    val grade: String? = null,
    val status: String? = null,
    val term: String? = null
)

data class StudentProfile(
    val gpa: Double? = null,
    val totalCredits: Double? = null,
    val major: String? = null,
    val catalogYear: String? = null,
    val completedCourses: List<CourseEntry> = emptyList(),
    val inProgressCourses: List<CourseEntry> = emptyList()
)
