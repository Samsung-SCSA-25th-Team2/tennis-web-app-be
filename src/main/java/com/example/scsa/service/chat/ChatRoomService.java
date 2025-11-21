package com.example.scsa.service.chat;

import com.example.scsa.dto.chat.ChatRoomCreateRequestDTO;
import com.example.scsa.dto.chat.ChatRoomCreateResponseDTO;
import com.example.scsa.exception.ChatRoomAlreadyExistsException;
import com.example.scsa.domain.entity.ChatRoom;
import com.example.scsa.domain.entity.Match;
import com.example.scsa.domain.entity.User;
import com.example.scsa.exception.MatchNotFoundException;
import com.example.scsa.exception.UserNotFoundException;
import com.example.scsa.repository.ChatRoomRepository;
import com.example.scsa.repository.MatchRepository;
import com.example.scsa.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final MatchRepository matchRepository;
    private final UserRepository userRepository;

    @Transactional
    public ChatRoomCreateResponseDTO createChatRoom(Long currentUserId, ChatRoomCreateRequestDTO request) {

        // 매치 조회
        Match match = matchRepository.findById(request.getMatchId())
                .orElseThrow(() -> new MatchNotFoundException(request.getMatchId()));

        // 호스트 = 매치 주최자
        User host = match.getHost();
        Long hostId = host.getId();

        // 게스트 조회
        Long guestId = request.getGuestId();
        User guest = userRepository.findById(guestId)
                .orElseThrow(() -> new UserNotFoundException());

        // 이미 개설된 채팅방 체크
        boolean exists = chatRoomRepository.existsByMatchIdAndUser1_IdAndUser2_Id(
                match.getId(), hostId, guestId
        );
        if (exists) {
            throw new ChatRoomAlreadyExistsException(match.getId(), hostId, guestId);
        }

        // ChatRoom 생성
        ChatRoom chatRoom = chatRoomRepository.save(new ChatRoom(match.getId(), host, guest));

        return ChatRoomCreateResponseDTO.builder()
                .chatRoomId(chatRoom.getId())
                .matchId(chatRoom.getMatchId())
                .hostId(hostId)
                .hostNickname(host.getNickname())
                .hostImgUrl(host.getImgUrl())
                .guestId(guestId)
                .guestNickname(guest.getNickname())
                .guestImgUrl(guest.getImgUrl())
                .build();
    }
}