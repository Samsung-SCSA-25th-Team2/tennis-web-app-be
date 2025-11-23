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

        // 1. DB 저장
        ChatMessageResponseDTO saved = chatService.saveChat(requestDTO);

        // 2. STOMP Broker Relay를 통해 모든 구독자에게 전송
        // RabbitMQ가 메시지를 모든 서버 인스턴스에 자동으로 브로드캐스트
        // RabbitMQ STOMP에서는 슬래시 대신 점(.)을 사용해야 함
        String destination = "/topic/chatroom." + saved.getChatRoomId();
        messagingTemplate.convertAndSend(destination, saved);
    }
}
