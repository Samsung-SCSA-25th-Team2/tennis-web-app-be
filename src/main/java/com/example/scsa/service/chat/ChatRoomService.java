package com.example.scsa.service.chat;

import com.example.scsa.dto.chat.*;
import com.example.scsa.exception.ChatRoomAlreadyExistsException;
import com.example.scsa.domain.entity.ChatRoom;
import com.example.scsa.domain.entity.Match;
import com.example.scsa.domain.entity.User;
import com.example.scsa.exception.MatchNotFoundException;
import com.example.scsa.exception.UserNotFoundException;
import com.example.scsa.repository.ChatRepository;
import com.example.scsa.repository.ChatRoomRepository;
import com.example.scsa.repository.MatchRepository;
import com.example.scsa.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRepository chatRepository;
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

    @Transactional(readOnly = true)
    public ChatRoomListResponseDTO getMyChatRooms(Long currentUserId, ChatRoomListRequestDTO request) {

        LocalDateTime cursorTime = null;
        if (request.getCursor() != null) {
            cursorTime = LocalDateTime.parse(request.getCursor());
        }

        int size = request.getPageSize();
        int fetchSize = size + 1;

        List<ChatRoom> rooms = chatRoomRepository
                .findChatRoomsByUserWithCursor(currentUserId, cursorTime)
                .stream()
                .limit(fetchSize)
                .collect(Collectors.toList());

        boolean hasNext = rooms.size() > size;

        // 실제 반환 데이터는 size 만큼만
        if (hasNext) {
            rooms = rooms.subList(0, size);
        }

        List<ChatRoomDTO> roomDtos = rooms.stream()
                .map(room -> {
                    // 상대 유저
                    var opponent = room.getOtherUser(
                            room.getUser1().getId().equals(currentUserId)
                                    ? room.getUser1()
                                    : room.getUser2()
                    );

                    int unread = (int) chatRepository.countUnreadMessages(room.getId(), currentUserId);

                    return ChatRoomDTO.builder()
                            .chatRoomId(room.getId())
                            .matchId(room.getMatchId())
                            .opponentId(opponent.getId())
                            .opponentNickname(opponent.getNickname())
                            .opponentImgUrl(opponent.getImgUrl())
                            .lastMessagePreview(room.getLastMessagePreview())
                            .lastMessageAt(room.getLastMessageAt() == null ? null : room.getLastMessageAt().toString())
                            .unreadCount(unread)
                            .build();
                })
                .collect(Collectors.toList());

        String nextCursor = hasNext
                ? rooms.get(rooms.size() - 1).getLastMessageAt().toString()
                : null;

        return ChatRoomListResponseDTO.builder()
                .rooms(roomDtos)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
    }
}