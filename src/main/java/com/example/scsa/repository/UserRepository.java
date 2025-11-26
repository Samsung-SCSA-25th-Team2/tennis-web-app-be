package com.example.scsa.repository;

import com.example.scsa.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * User 엔티티 Repository
 * 사용자 관련 데이터베이스 접근을 담당
 *
 * 설계 참고:
 * - User는 다른 엔티티들과 달리 컬렉션을 가지지 않음
 * - 따라서 fetch join이 필요 없음 (N+1 문제 없음)
 * - 단순한 조회 메서드들로 구성
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 닉네임 존재 여부 확인
     *
     * 사용 시나리오:
     * - 회원가입 시 닉네임 중복 체크
     *
     * @param nickname 닉네임
     * @return 존재 여부 (true: 존재, false: 미존재)
     */
    boolean existsByNickname(String nickname);

    /**
     * 닉네임 존재 여부 확인
     *
     * 사용 시나리오:
     * - 프로필 수정 시 자신 이외의 닉네임 중복 체크
     *
     * @param nickname 닉네임
     * @param id 자신의 회원 아이디
     * @return 존재 여부 (true: 존재, false: 미존재)
     */
    boolean existsByNicknameAndIdNot(String nickname, Long id);

    /**
     * OAuth2 Provider와 ProviderId로 유저 조회
     *
     * 사용 시나리오:
     * - 소셜 로그인 시 기존 회원 확인
     * - 카카오 로그인 등
     *
     * @param provider 소셜 로그인 제공자 (kakao, google 등)
     * @param providerId 제공자로부터 받은 고유 ID
     * @return 해당 소셜 계정의 유저
     */
    Optional<User> findByProviderAndProviderId(String provider, String providerId);
}