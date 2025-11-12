# NTAG Writer

NTAG424 NFC 태그 자동 프로그래밍 도구

## 프로젝트 배경

### 문제 상황
NTAG424 칩셋을 사용한 제품 생산 과정에서 보안 설정에 상당한 추가 비용이 발생했습니다. NTAG424 칩셋을 판매하는 공장에서 해당 보안 설정을 위한 프로그래밍 개발 비용으로 상당히 높은 값을 불렀습니다.
비용 절감을 위해 직접 태그를 프로그래밍해야 했지만, 기존 도구인 **TagXplorer**는 다음과 같은 문제가 있었습니다:

- **2019년 유지보수 종료** - 더 이상 업데이트되지 않음
- **수동 작업** - 각 태그마다 파라미터를 일일이 입력해야 함
- **반복성** - 여러 태그 프로그래밍 시 매번 동일한 작업 반복
- **오류 가능성** - 수동 입력으로 인한 설정 실수 위험
- **비효율성** - 태그 하나당 여러 단계의 복잡한 설정 필요

### 해결 방안
NTAG424 칩셋의 빠르고 정확한 설정을 위한 **자동화 프로그래밍 도구**를 개발하기로 결정했습니다.

---

## 프로젝트 목표

### 1단계: PoC (CLI 기반)
- ✅ 커맨드라인 인터페이스로 핵심 기능 검증
- ✅ 태그를 리더기에 대면 자동으로 프로그래밍
- ✅ 설정 오류 최소화
- ✅ 프로그래밍 이력 관리
- ✅ 배치 프로그래밍 지원 (대량 태그 처리)
- ✅ 확장 가능한 리더기 지원 (identiv uTrust 3700 F 기반 → 다른 리더기 추가 가능)

### 2단계: GUI 개발 (추후)
- ✅ JavaFX 기반 GUI 인터페이스 구현
- ✅ 직관적인 사용자 경험 제공
- ✅ 실시간 로그 및 진행 상황 시각화

---

## 도전적인 요소

### 1. 새로운 기술 스택
- **Java 첫 경험** - 이전에 주로 사용하던 TypeScript/JavaScript와 다른 패러다임
- **JavaFX** - GUI 프레임워크 처음 사용
- **데스크톱 애플리케이션 개발** - 웹 개발과 다른 응용 프로그램 개발 경험
- **Java 8 호환성** - 최대한 많은 사용자를 위해 Java 8 타겟 설정

### 2. 하드웨어 통신
- **NFC 통신 경험 부족** - 저수준 하드웨어 통신 처음 다뤄봄
- **APDU 명령어** - 스마트카드 통신 프로토콜 학습
- **PC/SC API** - javax.smartcardio 라이브러리 사용
- **identiv uTrust 3700 F 리더기** - NTAG424 DNA 호환 리더기 제어

### 3. 암호화 알고리즘
- **AES-CMAC 구현** - 보안 인증 메커니즘 직접 구현
- **NTAG424 보안 프로토콜** - NXP 데이터시트 분석 및 구현
- **세션 키 생성** - 동적 암호화 키 관리

---

## 주요 고민 사항

### 1. NFC 설정 로직
- NTAG424 인증 프로세스 (AuthenticateEV2First)
- SDM (Secure Dynamic Messaging) 설정
- NDEF 메시지 작성
- CMAC 생성 및 검증
- 오프셋 계산 및 미러링 설정

### 2. 확장 가능한 설계
- **전략 패턴** 활용 - 다양한 NFC 리더기 지원
  - 현재: identiv uTrust 3700 F 기반으로 개발
  - 추후: ACR122U, PN532, PN7150 등 다른 리더기 추가 가능
  - 인터페이스 기반 설계로 새 리더기 추가 시 기존 코드 수정 최소화
- **계층형 아키텍처** - UI, Service, Repository 분리
- **SOLID 원칙** 적용 (특히 OCP - 개방-폐쇄 원칙)

### 3. 호환성 vs 개발 생산성
- Java 8 타겟 선택 (국내 70% 사용률)
- Lombok으로 boilerplate 코드 감소
- Stream API + 람다로 함수형 프로그래밍 스타일 적용

### 4. 사용자 경험
- 직관적인 GUI 설계
- 실시간 로그 표시
- 배치 모드로 대량 처리
- 프로그래밍 이력 관리
- 오류 처리 및 재시도 로직

---

## 기술 스택

### 개발 환경
- **Language**: Java 8+
- **Build Tool**: Gradle 7.x
- **GUI Framework**: JavaFX 11

### 주요 라이브러리
- **javax.smartcardio** - NFC 리더기 통신 (Java 내장)
- **BouncyCastle** - AES-CMAC 암호화
- **Gson** - JSON 처리
- **Apache HttpClient** - API 통신
- **SQLite** - 로컬 데이터베이스 (프로그래밍 이력)
- **Lombok** - Boilerplate 코드 감소
- **JUnit 4** - 단위 테스트

---

## 프로젝트 구조

```
ntag-writer/
├── src/main/java/ntagwriter/
│   ├── NtagWriterApplication.java      # 메인 진입점
│   ├── domain/                         # 도메인 모델
│   │   ├── Tag.java
│   │   ├── TagStatus.java
│   │   ├── SdmConfig.java
│   │   ├── ProgrammingResult.java
│   │   └── SetupStep.java              # 자동 설정 단계 enum
│   ├── service/                        # 비즈니스 로직
│   │   ├── NfcReaderService.java
│   │   ├── Ntag424SetupService.java    # NTAG424 자동 설정 서비스
│   │   ├── TagProgrammingService.java
│   │   ├── CryptoService.java
│   │   └── ApiService.java
│   ├── ui/                             # JavaFX UI (추후)
│   │   ├── MainController.java
│   │   ├── ConfigController.java
│   │   └── BatchController.java
│   ├── repository/                     # 데이터 접근 (추후)
│   │   └── ProgrammingHistoryRepository.java
│   ├── crypto/                         # 암호화 유틸
│   │   ├── CmacCalculator.java
│   │   └── AesEncryption.java
│   ├── reader/                         # NFC 리더기 전략 (확장 가능)
│   │   ├── NfcReaderStrategy.java          # 리더기 인터페이스
│   │   ├── IdentivReader.java              # identiv uTrust 3700 F 구현체
│   │   ├── ACR122UReader.java              # ACR122U 구현체 (호환성)
│   │   ├── ReaderException.java            # 리더기 예외
│   │   └── (추후 PN532Reader.java 등 추가 예정)
│   └── util/                           # 유틸리티
│       ├── HexUtils.java
│       ├── ApduCommand.java
│       └── ConsoleHelper.java          # 대화형 콘솔 UI 헬퍼
├── src/main/resources/
│   ├── fxml/                           # JavaFX FXML (추후)
│   └── config/                         # 설정 파일
└── src/test/java/ntagwriter/           # 테스트
```

---

## NFC 리더기 지원 전략

### 현재 지원
- **identiv uTrust 3700 F** - NTAG424 DNA 완벽 지원 리더기

### 확장성 설계
전략 패턴(Strategy Pattern)을 활용하여 다양한 리더기를 지원할 수 있도록 설계:

```java
// 리더기 인터페이스
public interface NfcReaderStrategy {
    void connect() throws ReaderException;
    byte[] readUid() throws ReaderException;
    ResponseAPDU sendCommand(byte[] apdu) throws ReaderException;
    void disconnect();
}

// identiv uTrust 3700 F 구현체
public class IdentivReader implements NfcReaderStrategy {
    // identiv 전용 구현
}

// ACR122U 구현체 (호환성)
public class ACR122UReader implements NfcReaderStrategy {
    // ACR122U 전용 구현
}

// 추후 추가 가능한 리더기
public class PN532Reader implements NfcReaderStrategy {
    // PN532 전용 구현
}
```

**확장 시 장점:**
- 새 리더기 추가 시 구현체만 추가하면 됨
- 기존 코드 수정 불필요 (OCP 원칙)
- 설정에서 리더기 타입 선택 가능
- 테스트 시 Mock 리더기로 대체 가능

---

## 주요 기능

### 1. 자동 프로그래밍
- 태그를 리더기에 올리면 자동으로 인식 및 프로그래밍
- NTAG424 인증, 키 변경, SDM 설정, NDEF 작성 자동화

### 2. 배치 모드
- 여러 태그를 연속으로 프로그래밍
- 진행 상황 실시간 표시
- 성공/실패 통계 제공

### 3. 프로그래밍 이력 관리
- SQLite 로컬 DB에 모든 프로그래밍 기록 저장
- UID, 시각, 상태, 오류 메시지 기록
- 이력 조회 및 Excel 내보내기

### 4. 확장 가능한 리더기 지원
- 전략 패턴으로 다양한 리더기 지원
- 현재: identiv uTrust 3700 F
- 추후 확장 가능: ACR122U, PN532, PN7150 등

### 5. 보안 관리
- AES 키 암호화 저장
- CMAC 생성 및 검증
- 민감 정보 로깅 방지

---

## 시작하기

### 필수 요구사항
- **Java 8 이상** (Java 8, 11, 17, 21 모두 지원)
- **NFC 리더기** (identiv uTrust 3700 F 권장)
- **NTAG424 DNA 태그**

### 설치 및 실행

```bash
# 저장소 클론
git clone https://github.com/yourusername/ntag-writer.git
cd ntag-writer

# Gradle 빌드
./gradlew build

# 실행
./gradlew run

# 실행 가능한 JAR 생성
./gradlew fatJar
java -jar build/libs/ntag-writer.jar
```

---

## 사용 방법

### 모드 선택

프로그램 실행 시 두 가지 모드 중 선택할 수 있습니다:

```bash
./gradlew run
```

**1. 간단 테스트 모드** (모드 1)
- 리더기 연결 및 태그 UID 읽기
- 빠른 동작 확인용

**2. NTAG424 자동 설정 모드** (모드 2)
- 단계별 대화형 태그 설정
- 전체 SDM 설정 자동화

---

### NTAG424 자동 설정 모드 사용법

모드 2를 선택하면 다음 8단계가 순차적으로 진행됩니다:

#### [단계 1] 리더기 연결
- NFC 리더기 자동 검색 및 연결
- 태그를 리더기에 올리도록 안내

#### [단계 2] 태그 UID 읽기
- 태그의 고유 식별자(UID) 읽기
- UID 정보 표시

#### [단계 3] 애플리케이션 선택
- NTAG424 DNA 애플리케이션 선택 (AID: D2760000850101)
- 태그 인식 확인

#### [단계 4] 기본 키 인증
- 공장 초기화 상태의 기본 키(00...00)로 인증
- EV2 인증 프로토콜 수행

#### [단계 5] SDM 설정
- **Base URL 입력**: 서버 검증 URL 입력 (예: https://yourdomain.com/verify)
- **AES 키 생성/입력**:
  - 자동 생성 (권장): 안전한 16바이트 랜덤 키 생성
  - 수동 입력: 기존 키 사용 (32자리 16진수)
- **SDM 오프셋 자동 계산**: PICC Data, CMAC 위치 자동 계산
- **중요**: 생성된 AES 키는 반드시 안전하게 보관하세요!

#### [단계 6] 보안 키 변경
- 기본 키를 새로운 AES 키로 변경
- 태그 보안 활성화

#### [단계 7] 설정 검증
- 모든 설정 확인
- 설정 정보 요약 표시
- 중요 정보 백업 안내

#### [단계 8] 완료
- 최종 URL 형식 안내
- 예상 출력: `https://yourdomain.com/verify?p=<32자리>&c=<16자리>`
- 태그 제거 가능

---

### 대화형 진행 방식

각 단계마다:
1. **단계 설명**: 현재 단계에서 무엇을 할지 설명
2. **진행 확인**: "이 단계를 진행하시겠습니까? (y/n)" 질문
3. **실행**: 사용자가 'y' 입력 시 해당 단계 실행
4. **결과 표시**: 성공/실패 여부 및 상세 정보 표시
5. **오류 처리**: 실패 시 재시도 또는 중단 선택 가능

---

### 사용 예시

```
=============================================================
 NTAG424 DNA 태그 자동 설정
=============================================================
ℹ 이 프로세스는 NTAG424 태그를 안전하게 설정합니다.
⚠ 진행 중에는 태그를 리더기에서 떼지 마세요!

설정을 시작하시겠습니까? (y/n): y

------------------------------------------------------------
[단계 1] 리더기 연결
------------------------------------------------------------
ℹ NFC 리더기를 연결하고 태그를 인식합니다.

이 단계를 진행하시겠습니까? (y/n): y
→ 리더기를 검색하고 연결 중...
✓ 리더기 연결됨: identiv uTrust 3700 F Contact Reader
ℹ 태그를 리더기에 올려주세요.
✓ 리더기 연결 완료!

------------------------------------------------------------
[단계 2] 태그 UID 읽기
------------------------------------------------------------
ℹ 태그의 고유 식별자(UID)를 읽습니다.

이 단계를 진행하시겠습니까? (y/n): y
→ 태그 UID를 읽는 중...
✓ UID: 04E5A1B2C3D4E5
✓ 태그 UID 읽기 완료!

...

------------------------------------------------------------
[단계 5] SDM 설정
------------------------------------------------------------
ℹ Secure Dynamic Messaging 파라미터를 설정합니다.

이 단계를 진행하시겠습니까? (y/n): y
→ SDM 파라미터 설정 중...

ℹ SDM을 설정하기 위해 Base URL이 필요합니다.
ℹ 예: https://yourdomain.com/verify

Base URL을 입력하세요: https://myserver.com/api/verify

ℹ SDM 오프셋 계산:
ℹ   - PICC Data Offset: 35
ℹ   - CMAC Offset: 70
ℹ   - CMAC Input Offset: 70

새로운 AES 키를 자동 생성하시겠습니까? (y/n): y
✓ AES 키 생성됨: 1A2B3C4D5E6F7A8B9C0D1E2F3A4B5C6D
⚠ ⚠ 이 키를 안전하게 보관하세요! 분실 시 태그를 사용할 수 없습니다.
✓ SDM 설정 완료!

...

=============================================================
 설정 완료!
=============================================================
✓ NTAG424 태그가 성공적으로 설정되었습니다.

ℹ 예상 URL 형식:
ℹ https://myserver.com/api/verify?p=<32자리 PICC 데이터>&c=<16자리 CMAC>

ℹ 이제 태그를 NFC 리더기에서 제거해도 됩니다.
ℹ 설정된 태그를 스마트폰으로 태핑하면 위 URL로 리다이렉트됩니다.

=============================================================
 설정 요약
=============================================================
ℹ 태그 UID: 04E5A1B2C3D4E5
ℹ Base URL: https://myserver.com/api/verify
ℹ AES 키: 1A2B3C4D5E6F7A8B9C0D1E2F3A4B5C6D

⚠ 이 정보를 안전한 곳에 저장하세요!
```

---

### 일반 프로그래밍 사용법 (추후 GUI)

1. **리더기 연결**
   - NFC 리더기를 PC에 연결
   - 드라이버가 자동으로 인식됨

2. **설정**
   - 서버 URL 입력
   - AES 키 설정
   - Base URL 설정

3. **프로그래밍**
   - "프로그래밍 시작" 버튼 클릭
   - 태그를 리더기에 올림
   - 자동으로 프로그래밍 진행

4. **배치 모드**
   - "배치 모드" 버튼 클릭
   - 목표 개수 입력 (선택)
   - 태그를 하나씩 올렸다가 빼기 반복

---

## 개발 일정

### 1단계: PoC (CLI)
- **Phase 1**: 기본 구조 및 NFC 통신
- **Phase 2**: 프로그래밍 로직 구현
- **Phase 3**: CLI 인터페이스 및 배치 모드
- **Phase 4**: 테스트 및 검증

### 2단계: GUI 개발 (추후)
- **Phase 5**: JavaFX GUI 인터페이스 구현
- **Phase 6**: 고급 기능 및 사용자 경험 개선
- **Phase 7**: 통합 테스트 및 배포

---

## 참고 자료

- [NTAG424 DNA Datasheet](https://www.nxp.com/docs/en/data-sheet/NTAG424DNA.pdf)
- [NTAG424 Application Note](https://www.nxp.com/docs/en/application-note/AN12196.pdf)
- [javax.smartcardio API](https://docs.oracle.com/javase/8/docs/jre/api/security/smartcardio/spec/)
- [BouncyCastle Crypto API](https://www.bouncycastle.org/java.html)

---

## 라이선스

MIT License

---

## 문의

프로젝트 관련 문의사항이나 버그 리포트는 Issues에 등록해주세요.
