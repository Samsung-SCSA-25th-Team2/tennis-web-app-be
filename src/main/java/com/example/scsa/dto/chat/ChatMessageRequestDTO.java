package com.example.scsa.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "채팅 메시지 전송 요청 (STOMP WebSocket 사용)")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageRequestDTO {

    @Schema(description = "채팅방 ID", example = "1", required = true)
    private Long chatRoomId;

    @Schema(description = "발신자 사용자 ID", example = "1", required = true)
    private Long senderId;

    @Schema(description = "메시지 내용 (최대 500자)", example = "안녕하세요!", required = true)
    private String message;
}
