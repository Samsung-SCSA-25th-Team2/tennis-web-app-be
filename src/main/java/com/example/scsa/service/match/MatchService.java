package com.example.scsa.service.match;

import com.example.scsa.domain.entity.Court;
import com.example.scsa.domain.entity.Match;
import com.example.scsa.domain.entity.User;
import com.example.scsa.domain.vo.Age;
import com.example.scsa.domain.vo.GameType;
import com.example.scsa.domain.vo.MatchStatus;
import com.example.scsa.domain.vo.Period;
import com.example.scsa.dto.match.MatchDTO;
import com.example.scsa.dto.match.MatchResponseDTO;
import com.example.scsa.exception.court.CourtNotFoundException;
import com.example.scsa.exception.match.InvalidMatchStatusChangeException;
import com.example.scsa.exception.match.MatchAccessDeniedException;
import com.example.scsa.exception.match.MatchNotFoundException;
import com.example.scsa.exception.UserNotFoundException;
import com.example.scsa.repository.ChatRoomRepository;
import com.example.scsa.repository.CourtRepository;
import com.example.scsa.repository.MatchRepository;
import com.example.scsa.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final CourtRepository courtRepository;
    private final ChatRoomRepository chatRoomRepository;

    /**
     * 매치 생성
     *
     * 처리 흐름:
     *  1) hostId로 사용자 조회 (없으면 예외)
     *  2) courtId로 테니스장 조회 (없으면 예외)
     *  3) 시작/종료 시간 검증 (종료가 시작보다 빨라서는 안 됨)
     *  4) Match 엔티티 생성
     *  5) Age, Period (Enum Set) 값 매핑
     *  6) matchRepository.save() 로 저장 후 ID 반환
     *
     * 역할:
     *  - 매치 등록 시 필요한 모든 도메인 검증을 담당
     *  - 생성된 matchId를 클라이언트에게 응답
     */
    @Transactional
    public MatchResponseDTO createMatch(Long hostId, MatchDTO dto){
        // 1. host 조회
        User host = userRepository.findById(hostId)
                .orElseThrow(UserNotFoundException::new);

        // 2. court 조회
        Court court = courtRepository.findById(dto.getCourtId())
                .orElseThrow(CourtNotFoundException::new);

        // 3. 시간 검증
        LocalDateTime start = dto.getStartDateTime();
        LocalDateTime end = dto.getEndDateTime();
        if (end.isBefore(start)){
            throw new IllegalArgumentException("종료 시간은 시작 시간 이후이어야 합니다.");
        }

        // 4. 매치 생성 (기본 상태는 RECRUITING)
        Match match = Match.builder()
                .host(host)
                .court(court)
                .matchStartDateTime(start)
                .matchEndDateTime(end)
                .gameType(GameType.valueOf(dto.getGameType()))
                .matchStatus(MatchStatus.RECRUITING)
                .fee(dto.getFee())
                .playerCountMen(dto.getPlayerCountMen())
                .playerCountWomen(dto.getPlayerCountWomen())
                .description(dto.getDescription())
                .build();

        // 5. AgeRange EnumSet 매핑
        dto.getAgeRange().forEach(ageStr -> {
            Age age = Age.valueOf(ageStr);
            match.addAge(age);
        });

        // 6. Period EnumSet 매핑
        dto.getPeriod().forEach(periodStr -> {
            Period period = Period.valueOf(periodStr);
            match.addPeriod(period);
        });

        // 7. 저장 후 결과 반환
        Match saved = matchRepository.save(match);

        return new MatchResponseDTO(saved.getId(), "매치가 성공적으로 등록되었습니다.");
    }

    /**
     * 매치 삭제
     *
     * 비즈니스 규칙:
     *  - 매치의 host 본인만 삭제 가능
     *  - 매치를 삭제하면 연결된 채팅방도 모두 삭제해야 함
     *    (JPA cascade 대신 업무 규칙에 따라 ChatRoomRepository에서 명시적으로 삭제)
     */
    @Transactional
    public void deleteMatch(Long hostId, Long matchId) {

        // 1. 매치 조회
        Match match = matchRepository.findById(matchId)
                .orElseThrow(MatchNotFoundException::new);

        // 2. host 검증 (본인 소유 매치인지 확인)
        if (!match.getHost().getId().equals(hostId)){
            throw new MatchAccessDeniedException(matchId);
        }

        // 3. 채팅방 삭제 (매치가 사라지면 채팅방도 삭제됨)
        chatRoomRepository.deleteByMatchId(matchId);

        // 4. 매치 삭제
        matchRepository.delete(match);
    }

    /**
     * 매치 상태 변경 (RECRUITING <-> COMPLETED)
     *
     * 비즈니스 규칙:
     *  - host 본인만 상태 변경 가능
     *  - 매치 시작 시간이 이미 지났고 상태가 COMPLETED라면
     *      COMPLETED → RECRUITING 으로 되돌리는 것은 금지
     *  - 그 외에는 단순 토글
     *
     * 목적:
     *  - UI에서 "매치 마감" 또는 "모집 재개" 같은 기능을 토글 방식으로 구현하기 위함
     */
    @Transactional
    public MatchResponseDTO changeMatchStatus(Long matchId, Long currentUserId) {

        // 1. 매치 조회
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new MatchNotFoundException(matchId));

        // 2. host 본인인지 검증
        if (!match.getHost().getId().equals(currentUserId)) {
            throw new MatchAccessDeniedException(matchId, currentUserId);
        }

        LocalDateTime now = LocalDateTime.now();
        MatchStatus currentStatus = match.getMatchStatus();

        // 3. 이미 시작된 매치는 COMPLETED → RECRUITING 변경 금지
        //    (이미 끝났거나 시작된 매치를 되살려 모집할 수 없기 때문)
        if (match.getMatchStartDateTime().isBefore(now)
                && currentStatus == MatchStatus.COMPLETED) {
            throw new InvalidMatchStatusChangeException(
                    "이미 시작된 매치는 COMPLETED → RECRUITING으로 변경할 수 없습니다."
            );
        }

        // 4. 상태 토글 (RECRUITING <-> COMPLETED)
        MatchStatus newStatus =
                (currentStatus == MatchStatus.RECRUITING)
                        ? MatchStatus.COMPLETED
                        : MatchStatus.RECRUITING;

        match.updateMatchStatus(newStatus); // JPA 더티체킹으로 상태 업데이트

        return new MatchResponseDTO(matchId, "매치가 성공적으로 변경되었습니다.");
    }
}