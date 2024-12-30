package com.minhanh.coregbackend.socket;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class GraphController {

    @MessageMapping("/transfactor")
    @SendTo("/topic/nodeinfo")
    public RbpResult getRbpResult(TfMessage node) throws Exception {
        return null;
    }
}