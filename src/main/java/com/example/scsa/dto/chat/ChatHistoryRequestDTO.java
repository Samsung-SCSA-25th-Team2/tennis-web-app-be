package com.example.scsa.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChatHistoryRequestDTO {

    private Long chatRoomId;
    private String cursor;   // ISO-8601 문자열
    private Integer size;    // default 20
}
