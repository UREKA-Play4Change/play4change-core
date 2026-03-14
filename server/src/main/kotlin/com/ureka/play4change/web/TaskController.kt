package com.ureka.play4change.web

import com.ureka.play4change.services.TaskService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("play4change/tasks")
class TaskController(
    private val taskService: TaskService
) {

    @GetMapping("/daily")
    fun dailyTask(){

    }

    @GetMapping("/submit")
    fun submitTask(){
        TODO()
    }

}