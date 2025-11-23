package com.example.scsa.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "채팅방 생성 응답")
@Getter
@Builder
public class ChatRoomCreateResponseDTO {

    @Schema(description = "생성된 채팅방 ID (WebSocket 연결 시 사용)", example = "1")
    private Long chatRoomId;

    @Schema(description = "매치 ID", example = "1")
    private Long matchId;

    @Schema(description = "호스트 사용자 ID", example = "1")
    private Long hostId;

    @Schema(description = "호스트 닉네임", example = "테니스왕")
    private String hostNickname;

    @Schema(description = "호스트 프로필 이미지 URL", example = "https://example.com/profile.jpg")
    private String hostImgUrl;

    @Schema(description = "게스트 사용자 ID", example = "2")
    private Long guestId;

    @Schema(description = "게스트 닉네임", example = "테니스러버")
    private String guestNickname;

    @Schema(description = "게스트 프로필 이미지 URL", example = "https://example.com/profile2.jpg")
    private String guestImgUrl;
}