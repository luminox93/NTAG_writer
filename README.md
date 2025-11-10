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

- ✅ 간단한 GUI 인터페이스로 사용자 편의성 향상
- ✅ 태그를 리더기에 대면 자동으로 프로그래밍
- ✅ 설정 오류 최소화
- ✅ 프로그래밍 이력 관리
- ✅ 배치 프로그래밍 지원 (대량 태그 처리)
- ✅ 확장 가능한 리더기 지원 (ACR122U 기반 → 다른 리더기 추가 가능)

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
- **ACR122U 리더기** - 특정 하드웨어 제어 및 드라이버 연동

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
  - 현재: ACR122U 기반으로 개발
  - 추후: PN532, PN7150 등 NTAG424 호환 리더기 추가 가능
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
│   ├── NtagWriterApplication.java  # 메인 진입점
│   ├── domain/                     # 도메인 모델
│   │   ├── Tag.java
│   │   ├── TagStatus.java
│   │   ├── SdmConfig.java
│   │   └── ProgrammingResult.java
│   ├── service/                    # 비즈니스 로직
│   │   ├── NfcReaderService.java
│   │   ├── TagProgrammingService.java
│   │   ├── CryptoService.java
│   │   └── ApiService.java
│   ├── ui/                         # JavaFX UI
│   │   ├── MainController.java
│   │   ├── ConfigController.java
│   │   └── BatchController.java
│   ├── repository/                 # 데이터 접근
│   │   └── ProgrammingHistoryRepository.java
│   ├── crypto/                     # 암호화 유틸
│   │   ├── CmacCalculator.java
│   │   └── AesEncryption.java
│   ├── reader/                     # NFC 리더기 전략 (확장 가능)
│   │   ├── NfcReaderStrategy.java      # 리더기 인터페이스
│   │   ├── ACR122UReader.java          # ACR122U 구현체
│   │   └── (추후 PN532Reader.java 등 추가 예정)
│   └── util/                       # 유틸리티
│       ├── HexUtils.java
│       └── ApduCommand.java
├── src/main/resources/
│   ├── fxml/                       # JavaFX FXML
│   └── config/                     # 설정 파일
└── src/test/java/ntagwriter/       # 테스트
```

---

## NFC 리더기 지원 전략

### 현재 지원
- **ACR122U** - 보유 중인 리더기 기반으로 개발 및 테스트

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

// ACR122U 구현체
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
- 현재: ACR122U
- 추후 확장 가능: PN532, PN7150 등

### 5. 보안 관리
- AES 키 암호화 저장
- CMAC 생성 및 검증
- 민감 정보 로깅 방지

---

## 시작하기

### 필수 요구사항
- **Java 8 이상** (Java 8, 11, 17, 21 모두 지원)
- **NFC 리더기** (ACR122U 권장)
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

- **Phase 1**: 기본 구조 및 NFC 통신 (2주)
- **Phase 2**: 프로그래밍 로직 구현 (3주)
- **Phase 3**: GUI 및 고급 기능 (2주)
- **Phase 4**: 테스트 및 배포 (1주)

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
