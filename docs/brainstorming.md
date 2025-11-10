# 브레인스토밍 - NTAG Writer 프로젝트

## 시작

- 우테코 오픈 미션으로 진행
- TagXplorer 대체할 NFC 태그 프로그래밍 도구 만들기
- **핵심**: 새로운 기술 스택 도전 + 실제로 사용 가능한 도구

---

## 기술 스택 어떻게 할까?

### 일단 생각해본 것들

1. **Electron + React**
   - 기존에 walkd_proto에 TypeScript로 CMAC 로직 있음
   - 그거 그대로 가져다 쓰면 빠름
   - 근데... 우테코가 Java 백엔드 교육인데 이건 아닌듯?
   - 기존에 하던거라 "도전"이 아님

2. **Java + JavaFX**
   - 우테코 취지에 맞음
   - Java 실력 늘릴 수 있음
   - 객체지향 설계 연습하기 좋음
   - 하드웨어 통신이라 특이함

3. **Kotlin + Compose Desktop**
   - 모던한데... 학습 곡선 좀 있을듯

4. **Rust + Tauri**
   - 너무 어려움
   - NFC 라이브러리도 별로 없음

**→ 일단 Java + JavaFX로 가자!**

---

## Java 버전 문제

### 고민의 시작
"되도록 많은 사람들이 써야 한다" → 호환성이 중요함

- 안정적임 (10년 넘게 씀)
- 레거시 시스템 많음
- Oracle 라이선스 문제 (8까지 무료였음)
- 기업들이 보수적임 ("돌아가는데 왜 바꿔?")

### 컴파일 설정 관련 헷갈렸던거

```gradle
sourceCompatibility = '1.8'
targetCompatibility = '1.8'
```

이렇게 하면:
- Java 21로 개발해도 Java 8 바이트코드로 컴파일됨
- **BUT** Java 8 문법만 써야함
- Java 21 문법(var, record 등) 쓰면 → **컴파일 에러남**

그래서 결국 Java 8 문법만 써야함

### 고민한 옵션들

**1. Java 8 타겟**
- 장점: 국내 70% 커버, 실용적
- 단점: 코드 장황함, 개발 느림

**2. Java 11 타겟**
- 장점: 모던 문법
- 단점: Java 8 사용자 못씀

**3. Java 11로 개발 + JRE 번들링**
- 장점: 개발은 편하고, 배포할 때 JRE 포함하면 사용자는 Java 없어도 됨
- 단점: 파일 크기 개큼 (50-100MB), 설정 복잡

### 최종 결정: Java 8

왜?
- 국내 타겟이면 Java 8 환경 많음
- 우테코 미션이 "실제 사용 가능한 도구" 만들기임
- "누구나 실행 가능"이 강점임
- 제약이 있는 환경에서 좋은 설계 연습 (이게 미션 취지 아닐까?)

### Lombok으로 해결

Java 8이 verbose한건 Lombok으로 해결:
```java
// Java 8 순수 - 너무 김
public class Tag {
    private final byte[] uid;
    // getter, constructor, equals, hashCode... 다 써야함
}

// Lombok - 간결
@Getter
@AllArgsConstructor
public class Tag {
    private final byte[] uid;
}
```

Stream API + 람다도 적극 활용하면 괜찮을듯

---

## 최종 기술 스택

- Java 8 (sourceCompatibility = 1.8)
- JavaFX 11
- Gradle
- Lombok (boilerplate 감소)
- BouncyCastle (암호화)
- Gson (JSON)
- SQLite (로컬 DB)
- JUnit 4 (테스트)

---

## NFC 리더기 지원 전략

### 현재 상황
- 지금 가지고 있는거: **ACR122U**
- 일단 이걸로 개발하고 테스트

### 확장성 고려
- 나중에 다른 NTAG424 호환 리더기도 추가할 수 있어야 함
- PN532, PN7150 같은거?

### 설계 방향
- **전략 패턴 (Strategy Pattern)** 쓰면 될듯
- ReaderStrategy 인터페이스 만들고
- ACR122UReader, PN532Reader... 이런식으로 구현체 추가
- 설정에서 리더기 타입 선택 가능하게

```java
// 대충 이런 느낌?
interface NfcReaderStrategy {
    void connect();
    byte[] readUid();
    void sendCommand(byte[] apdu);
}

class ACR122UReader implements NfcReaderStrategy { ... }
class PN532Reader implements NfcReaderStrategy { ... }  // 나중에 추가
```

- 처음엔 ACR122U만 구현
- 나중에 다른 리더기 추가하고 싶으면 구현체만 추가하면 됨
- 코드 수정 최소화 (OCP 원칙)

---

## 프로젝트 구조 대충

```
src/main/java/ntagwriter/
  ├── domain/          # Tag, SdmConfig 같은 도메인 모델
  ├── service/         # NFC, Crypto, API 로직
  ├── ui/              # JavaFX 컨트롤러
  ├── repository/      # DB 접근
  ├── crypto/          # CMAC 계산 등
  ├── reader/          # NFC 리더기 전략 (ACR122U, PN532...)
  └── util/            # Hex 변환 같은거
```

계층형 아키텍처로 가면 될듯

---

## 우테코 미션 관점에서 어필 포인트

- 새로운 기술 도전 (JavaFX 처음)
- 하드웨어 통신 (NFC)
- 암호화 알고리즘 (AES-CMAC)
- 객체지향 설계 (SOLID, 디자인 패턴)
- 테스트 코드
- 실용성 (실제로 쓸 수 있음)
- 호환성 고려 (Java 8)
- 차별화 (데스크톱 앱)

---

## Java 8에서 주의할 것

못 쓰는거:
- var
- record
- text blocks (""")
- switch expression
- sealed class

쓸 수 있는거:
- 람다
- Stream API
- Optional
- try-with-resources
- 인터페이스 default method

→ Lombok + Stream API

---

## 다음 할일

- [x] Gradle 프로젝트 설정
- [x] Java 8 설정
- [x] 의존성 추가
- [ ] 디렉토리 구조 만들기
- [ ] 도메인 모델 작성
- [ ] NFC 리더기 연결 테스트
- [ ] GUI 기본 틀
- [ ] CMAC 로직 구현
- ...

---

## 메모
- ntag_todo.md에 상세 스펙 있음
- walkd_proto에 TypeScript CMAC 로직 있음 (참고용)
- Java 8 제약 있지만 그게 오히려 학습 기회
- "제약 속에서 좋은 설계" 만드는게 이번 미션 핵심인듯
