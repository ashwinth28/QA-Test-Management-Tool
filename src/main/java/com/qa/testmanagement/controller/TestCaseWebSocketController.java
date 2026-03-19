package com.qa.testmanagement.controller;

import com.qa.testmanagement.model.TestCase;
import com.qa.testmanagement.repository.TestCaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class TestCaseWebSocketController {

    @Autowired
    private TestCaseRepository repository;

    @Autowired
    private SimpMessagingTemplate template;

    // Broadcast updated test case to all subscribers
    @MessageMapping("/updateStatus")
    public void updateStatus(TestCase tc) {
        // save to DB
        repository.save(tc);

        // broadcast updated testcase list
        template.convertAndSend("/topic/testcases", repository.findAll());
    }
}