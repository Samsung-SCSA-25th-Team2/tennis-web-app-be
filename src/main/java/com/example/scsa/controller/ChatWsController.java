package com.example.scsa.controller;

import com.example.scsa.dto.chat.ChatMessageRequestDTO;
import com.example.scsa.dto.chat.ChatMessageResponseDTO;
import com.example.scsa.service.chat.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatWsController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;

    @MessageMapping("/chat.send")
    public void send(ChatMessageRequestDTO requestDTO) {

        // DB 저장
        ChatMessageResponseDTO saved = chatService.saveChat(requestDTO);

        // /topic/chat-room/{id} 모든 구독자에게 전송
        String destination = "/topic/chat-room/" + saved.getChatRoomId();
        messagingTemplate.convertAndSend(destination, saved);
    }
}
