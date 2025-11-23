package com.example.scsa.dto.chat;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatRoomCreateRequestDTO {

    @NotNull
    private Long matchId;   // 채팅방을 개설할 매치

    @NotNull
    private Long guestId;   // 매치 호스트에게 채팅을 하고자 할 게스트

    public ChatRoomCreateRequestDTO(Long matchId, Long guestId) {
        this.matchId = matchId;
        this.guestId = guestId;
    }
}