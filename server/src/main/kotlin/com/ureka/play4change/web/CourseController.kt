package com.ureka.play4change.web

import com.ureka.play4change.services.CourseService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("play4change/courses")
class CourseController(
    private val courseService: CourseService
){

    @PostMapping("/create")
    fun createCourse(){

    }
}