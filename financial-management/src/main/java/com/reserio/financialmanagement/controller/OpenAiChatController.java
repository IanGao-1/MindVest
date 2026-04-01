package com.reserio.financialmanagement.controller;

import com.reserio.financialmanagement.dto.OpenAiChatRequest;
import com.reserio.financialmanagement.dto.OpenAiChatResponse;
import com.reserio.financialmanagement.service.OpenAiChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@Tag(name = "OpenAI Chat", description = "OpenAI chat API")
public class OpenAiChatController {

    @Autowired
    private OpenAiChatService openAiChatService;

    @Operation(summary = "Chat with OpenAI")
    @PostMapping("/chat")
    public ResponseEntity<OpenAiChatResponse> chat(@RequestBody OpenAiChatRequest request) {
        return ResponseEntity.ok(openAiChatService.chat(request));
    }
}
