package com.example.scsa.dto.chat;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ChatRoomListResponseDTO {

    private List<ChatRoomDTO> rooms;
    private String nextCursor;
    private boolean hasNext;
}