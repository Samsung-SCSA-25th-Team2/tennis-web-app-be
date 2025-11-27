package com.example.scsa.service.chat;

import com.example.scsa.dto.chat.*;
import com.example.scsa.exception.chat.ChatRoomAlreadyExistsException;
import com.example.scsa.domain.entity.ChatRoom;
import com.example.scsa.domain.entity.Match;
import com.example.scsa.domain.entity.User;
import com.example.scsa.exception.chat.InvalidCursorFormatException;
import com.example.scsa.exception.match.MatchNotFoundException;
import com.example.scsa.exception.chat.SelfChatRoomNotAllowedException;
import com.example.scsa.exception.UserNotFoundException;
import com.example.scsa.repository.ChatRepository;
import com.example.scsa.repository.ChatRoomRepository;
import com.example.scsa.repository.MatchRepository;
import com.example.scsa.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRepository chatRepository;
    private final MatchRepository matchRepository;
    private final UserRepository userRepository;

    /**
     * 채팅방 생성
     *
     * 시나리오:
     *  - 매치 상세 화면에서 "채팅하기"를 누른 현재 로그인 유저(currentUserId)가 guest가 되고,
     *    해당 매치의 host와 1:1 채팅방을 생성한다.
     *
     * 비즈니스 규칙:
     *  1) 매치가 존재하지 않으면 안 된다.
     *  2) currentUser가 실제 존재하는 유저여야 한다.
     *  3) host와 guest가 동일한 유저이면(자기 자신과 채팅) 채팅방을 만들 수 없다.
     *  4) 동일한 (matchId, hostId, guestId) 조합의 채팅방은 하나만 존재해야 한다.
     *
     * @param currentUserId 현재 로그인한 사용자 ID (guest 역할)
     * @param request       채팅방 생성 요청 정보 (matchId 등)
     */
    @Transactional
    public ChatRoomCreateResponseDTO createChatRoom(Long currentUserId, ChatRoomCreateRequestDTO request) {

        // 1. 매치 조회 (없으면 예외)
        Match match = matchRepository.findById(request.getMatchId())
                .orElseThrow(() -> new MatchNotFoundException(request.getMatchId()));

        User host = match.getHost();
        Long hostId = host.getId();

        // 2. 현재 로그인 유저를 guest로 사용
        User guest = userRepository.findById(currentUserId)
                .orElseThrow(UserNotFoundException::new);

        // 3. host와 guest가 같으면 자기 자신과의 채팅이므로 허용하지 않음
        if (hostId.equals(currentUserId)) {
            throw new SelfChatRoomNotAllowedException(hostId);
        }

        // 4. 동일한 match + host + guest 조합의 채팅방이 이미 존재하는지 검사
        boolean exists = chatRoomRepository.existsByMatchIdAndUser1_IdAndUser2_Id(
                match.getId(), hostId, currentUserId
        );
        if (exists) {
            throw new ChatRoomAlreadyExistsException(match.getId(), hostId, currentUserId);
        }

        // 5. 채팅방 생성 및 저장
        ChatRoom chatRoom = chatRoomRepository.save(new ChatRoom(match.getId(), host, guest));

        // 6. 응답 DTO로 변환
        return ChatRoomCreateResponseDTO.builder()
                .chatRoomId(chatRoom.getId())
                .matchId(chatRoom.getMatchId())
                .hostId(hostId)
                .hostNickname(host.getNickname())
                .hostImgUrl(host.getImgUrl())
                .guestId(currentUserId)
                .guestNickname(guest.getNickname())
                .guestImgUrl(guest.getImgUrl())
                .build();
    }

    /**
     * 내가 속한 채팅방 목록 조회 (커서 기반 페이징)
     *
     * - currentUserId가 user1 또는 user2로 속해 있는 채팅방 목록을 조회한다.
     * - lastMessageAt(마지막 메시지 시간) 기준으로 내림차순 정렬된 목록에서
     *   cursor(마지막으로 읽은 시간) 이전 데이터만 가져오는 방식.
     *
     * 페이징 로직:
     *  1) cursor가 있으면 LocalDateTime으로 파싱
     *  2) size + 1개를 조회해서 hasNext 여부를 판단
     *  3) 실제 응답에는 size개만 담는다.
     *  4) nextCursor는 마지막 채팅방의 lastMessageAt 값으로 설정
     */
    @Transactional(readOnly = true)
    public ChatRoomListResponseDTO getMyChatRooms(Long currentUserId, ChatRoomListRequestDTO request) {

        // 1. cursor → LocalDateTime 파싱 (null이면 첫 페이지 조회)
        LocalDateTime cursorTime = null;
        if (request.getCursor() != null) {
            try {
                cursorTime = LocalDateTime.parse(request.getCursor());
            } catch(DateTimeParseException e){
                throw new InvalidCursorFormatException();
            }
        }

        int size = request.getPageSize();
        int fetchSize = size + 1;

        /**
         * findChatRoomsByUserWithCursor:
         *  - currentUserId가 user1 또는 user2인 채팅방을 조회
         *  - lastMessageAt 기준 내림차순 정렬
         */
        List<ChatRoom> rooms = chatRoomRepository
                .findChatRoomsByUserWithCursor(currentUserId, cursorTime)
                .stream()
                .limit(fetchSize) // size + 1만큼만 사용
                .collect(Collectors.toList());

        // 2. hasNext 판단
        boolean hasNext = rooms.size() > size;

        // 3. 실제 응답 목록은 size까지만 사용
        if (hasNext) {
            rooms = rooms.subList(0, size);
        }

        // 4. Entity → DTO 변환
        // 현재 사용자가 user1인지 user2인지에 따라 "상대방(opponent)"를 구한다.
        List<ChatRoomDTO> roomDtos = rooms.stream()
                .map(room -> {
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
                            .lastMessageAt(room.getLastMessageAt() == null
                                    ? null
                                    : room.getLastMessageAt().format(ISO_FORMATTER))
                            .unreadCount(unread)
                            .build();
                })
                .collect(Collectors.toList());

        // 5. nextCursor: 마지막 채팅방의 lastMessageAt 기준
        String nextCursor = hasNext
                ? rooms.get(rooms.size() - 1).getLastMessageAt().format(ISO_FORMATTER)
                : null;

        return ChatRoomListResponseDTO.builder()
                .rooms(roomDtos)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
    }
}