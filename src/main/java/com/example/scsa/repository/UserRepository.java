package com.example.scsa.repository;

import com.example.scsa.domain.entity.User;
import com.example.scsa.domain.vo.Age;
import com.example.scsa.domain.vo.Gender;
import com.example.scsa.domain.vo.Period;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
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
     * 닉네임으로 유저 조회
     * 닉네임은 유니크 제약조건이 있어 단일 결과 반환
     *
     * 사용 시나리오:
     * - 닉네임 중복 체크
     * - 닉네임으로 유저 검색
     *
     * @param nickname 닉네임
     * @return 해당 닉네임의 유저
     */
    Optional<User> findByNickname(String nickname);

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

    /**
     * 카카오 이름으로 유저 조회
     *
     * 사용 시나리오:
     * - 카카오 로그인 시 기존 회원 확인
     *
     * @param name 카카오 이름
     * @return 해당 이름의 유저 목록
     */
    List<User> findByName(String name);

    /**
     * 성별로 유저 목록 조회
     *
     * 사용 시나리오:
     * - 특정 성별 유저 검색
     * - 매치 추천 등
     *
     * @param gender 성별
     * @return 해당 성별의 유저 목록
     */
    List<User> findByGender(Gender gender);

    /**
     * 나이대로 유저 목록 조회
     *
     * 사용 시나리오:
     * - 특정 나이대 유저 검색
     * - 매치 추천 등
     *
     * @param age 나이대
     * @return 해당 나이대의 유저 목록
     */
    List<User> findByAge(Age age);

    /**
     * 테니스 경력으로 유저 목록 조회
     *
     * 사용 시나리오:
     * - 특정 경력 유저 검색
     * - 매치 추천 등
     *
     * @param period 테니스 경력
     * @return 해당 경력의 유저 목록
     */
    List<User> findByPeriod(Period period);

    /**
     * 복합 조건으로 유저 검색 (성별, 나이대, 경력)
     *
     * 사용 시나리오:
     * - 매치 조건에 맞는 유저 추천
     * - 필터링 검색
     *
     * @param gender 성별
     * @param age 나이대
     * @param period 테니스 경력
     * @return 조건에 맞는 유저 목록
     */
    @Query("SELECT u FROM User u " +
           "WHERE u.gender = :gender " +
           "AND u.age = :age " +
           "AND u.period = :period")
    List<User> findByConditions(
            @Param("gender") Gender gender,
            @Param("age") Age age,
            @Param("period") Period period
    );

    /**
     * 전체 유저 수 조회
     *
     * @return 전체 유저 수
     */
    @Query("SELECT COUNT(u) FROM User u")
    long countAllUsers();

    Optional<User> findByProviderId(String username);
}