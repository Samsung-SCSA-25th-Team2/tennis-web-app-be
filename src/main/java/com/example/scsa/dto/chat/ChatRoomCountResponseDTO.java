package com.example.scsa.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 매치별 채팅방 개수 조회 응답 DTO
 */
@Getter
@AllArgsConstructor
@Schema(description = "매치별 채팅방 개수 조회 응답")
public class ChatRoomCountResponseDTO {

    @Schema(description = "매치 ID", example = "1")
    private Long matchId;

    @Schema(description = "해당 매치의 채팅방 개수", example = "5")
    private long chatRoomCount;

    public static ChatRoomCountResponseDTO of(Long matchId, long count) {
        return new ChatRoomCountResponseDTO(matchId, count);
    }
}