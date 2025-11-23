package com.example.scsa.dto.chat;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatRoomListRequestDTO {

    private String cursor;   // ISO-8601 String (nullable)
    private Integer size;    // default 10

    public ChatRoomListRequestDTO(String cursor, Integer size) {
        this.cursor = cursor;
        this.size = size;
    }

    public int getPageSize() {
        return (size == null || size < 1) ? 10 : size;
    }
}