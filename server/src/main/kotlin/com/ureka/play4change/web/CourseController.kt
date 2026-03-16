package com.ureka.play4change.web

import com.ureka.play4change.domain.CreateCourseRequest
import com.ureka.play4change.domain.CreateCourseResponse
import com.ureka.play4change.domain.EnrollResponse
import com.ureka.play4change.services.CourseService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("play4change/courses")
class CourseController(
    private val courseService: CourseService
) {

    @PostMapping("/create")
    fun createCourse(
        @RequestBody request: CreateCourseRequest
    ): ResponseEntity<CreateCourseResponse> {
        val response = courseService.createCourse(request)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/{courseId}/enroll")
    fun enroll(
        @RequestHeader("X-User-Id") userId: String,
        @PathVariable courseId: String
    ): ResponseEntity<EnrollResponse> {
        val response = courseService.enrollUser(userId, courseId)
        return ResponseEntity.ok(response)
    }
}