# Git Branch 전략 가이드 (Main-Develop-Feature)
> IntelliJ IDEA 환경에서의 실전 가이드

## 📌 브랜치 구조 개요

```
main (배포용)
  ↑
develop (개발 통합)
  ↑
feature/기능명 (개별 작업)
```

---

## 🌳 브랜치별 역할

### 1. **main** 브랜치
- **용도**: 실제 서비스 배포용
- **특징**: 항상 안정적인 상태 유지
- **규칙**: 직접 커밋 금지, develop에서 merge만 허용

### 2. **develop** 브랜치
- **용도**: 개발 중인 기능들을 통합하는 브랜치
- **특징**: 다음 배포를 준비하는 곳
- **규칙**: feature 브랜치들이 merge되는 곳

### 3. **feature/** 브랜치
- **용도**: 새로운 기능 개발
- **명명규칙**: `feature/기능명` (예: `feature/login`, `feature/user-profile`)
- **특징**: 작업 완료 후 develop에 merge하고 삭제

---

## 🚀 IntelliJ에서 실전 작업 흐름

### **Step 1: 새 기능 개발 시작**

#### IntelliJ UI 사용
1. 우측 하단 또는 좌측 하단의 **Git 브랜치 이름** 클릭
2. `develop` 브랜치 선택 → `Checkout` 클릭
3. 상단 메뉴 `Git` → `Pull` (또는 `Ctrl+T`) - 최신 코드 받기
4. 다시 브랜치 이름 클릭 → `New Branch from Selected...` 
5. 브랜치 이름 입력: `feature/login`
6. `Checkout branch` 체크 → `Create` 클릭

#### 터미널 사용 (IntelliJ 내장 터미널 `Alt+F12`)
```bash
# 1. develop 브랜치로 이동
git checkout develop

# 2. 최신 코드 받아오기
git pull origin develop

# 3. feature 브랜치 생성 및 이동
git checkout -b feature/login
```

---

### **Step 2: 작업하기**

#### IntelliJ에서 변경사항 확인 및 커밋
1. 코드 작성 완료 후
2. `Ctrl+K` 또는 상단 메뉴 `Git` → `Commit` 클릭
3. **Commit 창**에서:
   - 변경된 파일 확인 및 선택
   - 커밋 메시지 작성 (컨벤션 참고)
   - `Commit` 또는 `Commit and Push` 버튼 클릭

#### 터미널 사용
```bash
# 코드 작성...

# 변경사항 확인
git status

# 파일 추가
git add .

# 커밋
git commit -m "feat: 로그인 기능 구현"
```

---

### **Step 3: 원격 저장소에 푸시**

#### IntelliJ UI 사용
1. `Ctrl+Shift+K` 또는 상단 메뉴 `Git` → `Push`
2. **Push Commits** 창에서 푸시할 커밋 확인
3. `Push` 버튼 클릭

#### 터미널 사용
```bash
# feature 브랜치를 원격에 푸시
git push origin feature/login
```

---

### **Step 4: Pull Request (PR) 생성**

1. IntelliJ 우측 상단 GitHub 알림 또는 GitHub 웹사이트 접속
2. `Compare & pull request` 버튼 클릭
3. **base**: `develop` ← **compare**: `feature/login` 확인
4. PR 제목과 설명 작성
5. 팀원에게 코드 리뷰 요청
6. `Create pull request` 클릭

---

### **Step 5: 코드 리뷰 & Merge**

#### GitHub에서 Merge 후 로컬 업데이트 (권장)
1. GitHub에서 리뷰 승인 후 `Merge pull request` 클릭
2. IntelliJ로 돌아와서:
   - 좌측 하단 브랜치 클릭 → `develop` 선택 → `Checkout`
   - `Ctrl+T` (또는 `Git` → `Pull`) - 최신 코드 받기

#### 터미널 사용
```bash
# develop으로 이동
git checkout develop
```

---

### **Step 6: 배포 준비 (develop → main)**
> 이 부분은 신경 안쓰셔도 됩니다. CI/CD를 통해 AWS 클라우드에 배포될 예정!

#### IntelliJ UI 사용
1. 브랜치 클릭 → `main` 선택 → `Checkout`
2. `Ctrl+T` - 최신 코드 받기
3. 브랜치 클릭 → `develop` 우클릭 → `Merge 'develop' into 'main'`
4. 충돌 해결 (필요시)
5. `Ctrl+Shift+K` - Push

#### 터미널 사용
```bash
# 충분히 테스트 후

# main으로 이동
git checkout main

# main 최신화
git pull origin main

# develop을 main에 merge
git merge develop

# 배포용 태그 추가 (선택)
git tag -a v1.0.0 -m "첫 배포"

# main 푸시
git push origin main
git push origin v1.0.0
```

---

## 📝 커밋 메시지 컨벤션

IntelliJ Commit 창에서 메시지 작성 시 다음 형식 사용:

```
⚙️Feat: 새로운 기능 추가
🪄Fix: 버그 수정
📚Docs: 문서 수정
🎨Style: 코드 포맷팅 (기능 변경 없음)
✨Refactor: 코드 리팩토링
📄Test: 테스트 코드
🔗Chore: 빌드, 설정 파일 수정
```

**예시:**
```
⚙️Feat: 사용자 로그인 API 구현
🪄Fix: 로그인 시 비밀번호 검증 오류 수정
📚Docs: README에 설치 가이드 추가
✨Refactor: UserService 코드 정리 및 최적화
```

---

## ⚠️ 주의사항

### ❌ 하지 말아야 할 것

1. **main에 직접 푸시하지 않기**
   - main 브랜치에서 직접 코드 수정 금지
   - 반드시 develop을 거쳐서 merge

2. **develop에서 직접 작업하지 않기**
   - 항상 feature 브랜치를 생성해서 작업
   - develop은 feature들을 통합하는 용도로만 사용

3. **여러 기능을 한 브랜치에서 작업하지 않기**
   - 기능별로 브랜치를 분리
   - 작은 단위로 자주 merge

4. **커밋 전 코드 검토 생략하지 않기**
   - IntelliJ의 `Ctrl+K` 커밋 창에서 변경사항 확인
   - 불필요한 파일(`.idea` 내부 설정 등) 커밋 방지

### ✅ 해야 할 것

1. **자주 커밋하기**
   - 작은 단위로 의미있는 커밋
   - 한 커밋에 하나의 목적

2. **푸시 전에 pull 받기**
   - `Ctrl+T`로 항상 최신 코드 확인
   - 충돌 가능성 최소화

3. **브랜치 이름 명확하게 짓기**
   - Good: `feature/user-authentication`, `feature/order-payment`
   - Bad: `feature/fix`, `feature/test`, `feature/my-work`

4. **.gitignore 관리**
   - IntelliJ 프로젝트 설정 파일 제외
   - `.idea/workspace.xml`, `*.iml` 등

---

## 🔧 IntelliJ에서 자주 발생하는 문제 해결

### 1. 충돌(Conflict) 발생 시

#### IntelliJ Merge Tool 사용 (권장)
1. 충돌 발생 시 IntelliJ가 자동으로 **Conflicts** 창 표시
2. 충돌 파일 더블클릭 → **Merge Revisions** 창 열림
3. 3개 패널에서 변경사항 확인:
   - 왼쪽: 현재 브랜치 (Your changes)
   - 오른쪽: 가져올 브랜치 (Changes from server)
   - 중앙: 최종 결과
4. `Accept Left` / `Accept Right` / 직접 수정
5. 모든 충돌 해결 후 `Apply` 클릭
6. `Ctrl+K`로 커밋

#### 터미널 사용
```bash
# 1. develop의 최신 코드 가져오기
git checkout develop
git pull origin develop

# 2. feature 브랜치로 돌아가서 develop merge
git checkout feature/login
git merge develop

# 3. 충돌 파일 수정 (IntelliJ Merge Tool 자동 실행)

# 4. 해결 후 커밋
git add .
git commit -m "merge: develop 브랜치와 충돌 해결"
git push origin feature/login
```

---

### 2. 실수로 잘못된 브랜치에서 작업한 경우

#### IntelliJ UI 사용
1. 상단 메뉴 `Git` → `Uncommitted Changes` → `Stash Changes`
2. Stash 이름 입력 후 `Create Stash`
3. 좌측 하단 브랜치 클릭 → 올바른 브랜치로 체크아웃
4. 상단 메뉴 `Git` → `Uncommitted Changes` → `Unstash Changes`
5. 해당 Stash 선택 후 `Apply Stash`

#### 터미널 사용
```bash
# 아직 커밋 안 했다면
git stash                          # 작업 내용 임시 저장
git checkout feature/correct-branch # 올바른 브랜치로 이동
git stash pop                      # 작업 내용 복구
```

---

### 3. Git Graph가 안 보일 때

1. 좌측 하단 `Git` 탭 클릭
2. 없다면: `View` → `Tool Windows` → `Git`
3. Git 탭에서 `Log` 탭 선택 - 브랜치 그래프 확인 가능
4. 또는 `Alt+9`로 Git 창 토글

---

### 4. VCS 메뉴가 안 보일 때

1. `VCS` → `Enable Version Control Integration` 선택
2. Git 선택 후 OK
3. 또는 `File` → `Invalidate Caches` → `Invalidate and Restart`

---

## 📊 실전 예시 시나리오

### 회원가입 기능 개발 전체 과정 (IntelliJ)

```
1. 브랜치 생성
   - 좌측 하단 브랜치 클릭 → develop 체크아웃
   - Ctrl+T (Pull)
   - 브랜치 클릭 → New Branch → feature/signup

2. 작업 & 커밋
   - UserController.java 작성
   - Ctrl+K → "feat: 회원가입 API 엔드포인트 추가" → Commit
   
   - UserService.java 작성
   - Ctrl+K → "feat: 이메일 중복 검증 로직 구현" → Commit

3. 푸시 & PR
   - Ctrl+Shift+K → Push
   - GitHub에서 PR 생성 (develop ← feature/signup)

4. 리뷰 완료 후 merge
   - GitHub에서 "Merge pull request" 클릭

5. 로컬 정리
   - 브랜치 클릭 → develop 체크아웃
   - Ctrl+T (Pull)
   - feature/signup 우클릭 → Delete
```

---

## 🎯 IntelliJ 단축키 정리

| 기능 | 단축키 | 설명 |
|------|--------|------|
| **Commit** | `Ctrl+K` | 커밋 창 열기 |
| **Push** | `Ctrl+Shift+K` | 원격 저장소에 푸시 |
| **Pull** | `Ctrl+T` | 원격 저장소에서 가져오기 |
| **Git 창** | `Alt+9` | Git 도구 창 토글 |
| **터미널** | `Alt+F12` | 내장 터미널 열기 |
| **VCS 팝업** | `Alt+\`` (백틱) | VCS 작업 빠른 메뉴 |
| **변경사항 보기** | `Alt+9` → `Local Changes` | 로컬 변경 파일 확인 |

---

## 💡 팁과 권장사항

### IntelliJ 설정 최적화

1. **Git 자동 완성 활성화**
   - `Settings` → `Version Control` → `Commit` 
   - "Use non-modal commit interface" 체크 (선택사항)

2. **.gitignore 템플릿 사용**
   - 프로젝트 루트에 `.gitignore` 파일 생성
   - IntelliJ가 자동으로 추천하는 항목 추가
   ```gitignore
   # IntelliJ
   .idea/
   *.iml
   out/
   
   # Gradle
   .gradle/
   build/
   
   # Maven
   target/
   ```

3. **코드 리뷰 전 체크리스트**
   - `Ctrl+Alt+L`: 코드 포맷팅
   - `Ctrl+Alt+O`: import 정리
   - IntelliJ 경고 확인 및 수정

4. **Commit 전 검사 활성화**
   - Commit 창(`Ctrl+K`)에서 우측 체크박스:
     - ☑ Reformat code
     - ☑ Optimize imports
     - ☑ Analyze code

### 협업 시 주의사항

- PR 설명은 구체적으로 작성 (변경 이유, 영향 범위)
- 스크린샷 첨부 (UI 변경 시)
- 관련 이슈 번호 연결 (있는 경우)
- 테스트 완료 여부 명시

---

## 🔗 참고 자료

- [Git 공식 문서](https://git-scm.com/doc)
- [IntelliJ Git 통합 가이드](https://www.jetbrains.com/help/idea/version-control-integration.html)
- [GitHub Flow](https://docs.github.com/en/get-started/quickstart/github-flow)

---

**마지막 업데이트**: 2024-11-17  
**작성자**: Git Branch Strategy Guide for Beginners
