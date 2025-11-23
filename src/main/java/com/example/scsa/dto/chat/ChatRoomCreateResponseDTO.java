package com.example.scsa.dto.chat;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatRoomCreateResponseDTO {

    private Long chatRoomId;

    private Long matchId;

    private Long hostId;
    private String hostNickname;
    private String hostImgUrl;

    private Long guestId;
    private String guestNickname;
    private String guestImgUrl;
}