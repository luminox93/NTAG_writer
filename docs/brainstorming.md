# 브레인스토밍 - NTAG Writer 프로젝트

제공해주신 NTAG 424 DNA (NT4H2421Gx) 데이터 시트의 내용을 챕터별로 정리해 드립니다. 이 문서는 NXP 반도체의 보안 NFC 칩에 대한 기술 사양을 담고 있습니다.

주요 챕터별 핵심 내용은 다음과 같습니다.

1. 일반 설명 (General description)

개요: NTAG 424 DNA는 보안 및 개인정보 보호를 위한 최신 기능을 갖춘 새로운 NTAG DNA 칩 세대입니다.


주요 기능: AES-128 암호화 및 SUN(Secure Unique NFC) 메시지 기능을 통해 태그할 때마다 고유한 인증 데이터를 생성합니다.


호환성: NFC Forum Type 4 Tag 사양 및 ISO/IEC 14443-4 프로토콜을 준수합니다.

2. 특징 및 이점 (Features and benefits)

RF 인터페이스: 최대 848 kbit/s의 데이터 속도와 ISO/IEC 14443A 표준을 지원합니다.


메모리: 416바이트의 사용자 메모리를 제공하며, 50년의 데이터 보존 기간과 200,000회의 쓰기 내구성을 가집니다.


보안: EAL4 인증 하드웨어/소프트웨어, SUN 메시지, LRP(Leakage Resilient Primitive) 암호화, 5개의 고객 정의 AES 키 등을 포함합니다.


3. 애플리케이션 (Applications)
위조 방지, 독점 사용자 경험 제공, 민감 데이터 보호, 문서 인증 등 다양한 보안 응용 분야에 사용됩니다.



4. 주문 정보 (Ordering information)
NT4H2421G0DUD, NT4H2421G0DA8 등 다양한 패키지 타입(FFC, MOA8)에 따른 주문 코드를 제공합니다.

5. 빠른 참조 데이터 (Quick reference data)
입력 커패시턴스(50pF), 입력 주파수(13.56MHz), EEPROM 특성 등의 주요 전기적 파라미터를 요약합니다.

6. 블록 다이어그램 (Block diagram)
아날로그(클럭, 통신, 전원) 및 디지털(CPU, 암호화 코프로세서, 메모리) 블록 간의 연결 구조를 시각적으로 보여줍니다.

7. 핀 정보 (Pinning information)
SOT500-4 (MOA8) 패키지의 안테나 코일 연결 핀(LA, LB) 구성을 설명합니다.

8. 기능 설명 (Functional description)

프로토콜: ISO/IEC 14443 초기화 및 프로토콜을 따릅니다.


메모리 구조: 파일 기반 메모리 구조(CC 파일, NDEF 파일, 독점 데이터 파일)를 가집니다.


접근 제어: 파일별로 읽기/쓰기 권한 및 통신 모드(Plain, MAC, Full)를 설정할 수 있습니다.


9. 보안 메시징 (Secure Messaging)

AES 모드: 표준 AES-128을 사용한 상호 인증 및 메시지 보호를 설명합니다.


LRP 모드: 부채널 공격 저항성을 높인 LRP(Leakage Resilient Primitive) 래퍼를 사용한 암호화 방식을 다룹니다.


SDM (Secure Dynamic Messaging): 태그할 때마다 URL에 고유 인증 코드와 암호화된 데이터를 미러링하여 백엔드 서버로 전송하는 기능을 상세히 설명합니다.


10. 명령어 세트 (Command set)

인증 명령: AuthenticateEV2First, AuthenticateLRPFirst 등 인증 관련 명령어를 정의합니다.



설정 및 키 관리: SetConfiguration, ChangeKey, ChangeFileSettings 등의 설정 변경 명령어를 포함합니다.




데이터 관리: ReadData, WriteData 및 ISO 표준 명령어(ISOSelectFile 등)를 설명합니다.




원본 확인: Read_Sig 명령어를 통한 ECC 기반 서명 확인 기능을 설명합니다.

11. 패키지 외형 (Package outline)
SOT500-4 (MOA8) 패키지의 물리적 치수와 도면을 제공합니다.


12. 한계값 (Limiting values)
입력 전류, 총 전력 소비, 보관 온도, 정전기 방전(ESD) 허용치 등의 절대 최대 정격 값을 명시합니다.

13. 특성 (Characteristics)
동작 온도 범위 내에서의 전기적 특성 및 EEPROM의 데이터 보존/쓰기 내구성을 상세화합니다.

14. 약어 (Abbreviations)
문서에 사용된 AES, APDU, LRP, SUN, UID 등 기술 용어의 약어 정의를 나열합니다.

15. 참고 문헌 (References)
ISO/IEC 표준, NIST 문서 등 기술 사양의 기반이 된 참고 문헌 목록입니다.

16. 개정 이력 (Revision history)
문서의 버전별 변경 사항 및 업데이트 날짜를 기록합니다.

17. 법적 정보 (Legal information)
데이터 시트의 상태 정의, 면책 조항, 라이선스 및 상표권 정보를 포함합니다.



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

### 리더기 변경 로그

**2025-11-18**
- ACR122U → ACR1252로 변경 시도
- 문제 발견: ACR1252가 NTAG424 DNA 지원 안 됨 (ISO 14443-4 제한적)
- 최종 결정: **identiv uTrust 3700 F CL Reader**로 변경
  - NTAG424 DNA 완벽 지원 확인
  - ISO 14443 Type A/B 지원
  - PC/SC 표준 호환
- 전략 패턴으로 설계했기 때문에 IdentivReader 클래스만 추가하면 됨
- ACR122UReader는 호환성을 위해 유지

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
