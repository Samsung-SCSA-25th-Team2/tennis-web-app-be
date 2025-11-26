package com.example.scsa.dto.chat;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatRoomCreateRequestDTO {

    @NotNull
    private Long matchId;   // 채팅방을 개설할 매치

    public ChatRoomCreateRequestDTO(Long matchId) {
        this.matchId = matchId;
    }
}