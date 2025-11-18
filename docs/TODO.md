# NTAG424 DNA CLI 개발 TODO

## 0. 아키텍처 및 설계 로드맵

### 0.1 전체 아키텍처 수립
- [ ] 클린 아키텍처 4계층(Interface / Application / Domain / Infrastructure) 구조 정의
- [ ] CLI → Application Service → Domain Service → Reader Adapter 흐름을 시퀀스 다이어그램으로 정리
- [ ] 공통 로깅·설정·예외 처리를 담당할 `core` 모듈 설계 및 모듈 의존성 규칙 수립
- [ ] 설정 파일, 시크릿, 인증 키 관리 전략(KMS/환경변수/CLI 입력) 문서화

### 0.2 리더기 전략 패턴 강화
- [ ] `NfcReaderStrategy`를 Reader 어댑터 계층의 최상위 인터페이스로 명확히 정의하고 DI 구성
- [ ] 리더 선택/등록을 담당할 `ReaderRegistry` 또는 팩토리 설계 (CLI 파라미터/환경변수/자동감지)
- [ ] 신규 리더기 드라이버 추가 시 애플리케이션 코드 변경이 없도록 SPI/플러그인 구조 연구
- [ ] Mock/Stub Reader 구현으로 통합 테스트 가능성 확보
- [ ] Reader 이벤트(카드 접촉/제거) 발행을 위한 옵저버 혹은 리액티브 스트림 설계

### 0.3 서비스 및 도메인 모듈화
- [ ] `TagProgrammingService`를 UseCase 계층으로 승격하고 시나리오별 메서드를 구분 (기본 설정/배치/복구)
- [ ] `CryptoService`, `ApduCommand`, `ApduStatusWord` 등 도메인 지원 모듈을 `domain-support` 패키지로 재배치
- [ ] 설정/이력/상태 저장을 담당할 Repository 인터페이스 정의 (SQLite ↔ 파일 ↔ 원격 API 교체 용이)
- [ ] SDM 설정, 키 관리, NDEF 설정 등 복잡한 값 객체(Value Object)와 Validator 설계
- [ ] 에러/상태 코드 맵핑을 도메인 이벤트로 승격해 상위 레이어에서 일관되게 처리

### 0.4 문서 및 다이어그램
- [ ] 상기 구조를 docs/architecture.md에 정리하고 패키지 구조/의존성 다이어그램 작성
- [ ] 주요 시퀀스(AuthenticateEV2, SDM 설정, NDEF 쓰기 등)를 UML 시퀀스로 작성
- [ ] 상태 전이 다이어그램(TagStatus, SetupStep 등) 작성

## 1. 오류 코드 수집 및 문서화

### 1.1 ISO/IEC 7816-4 표준 오류 코드
- [ ] ISO/IEC 7816-4 표준 오류 코드를 전부 수집하고 문서화
  - [ ] 6xxx 시리즈 오류 코드 정리
  - [ ] 9xxx 상태 코드 정리
  - [ ] 각 코드별 상세 설명 작성

### 1.2 NTAG424 DNA 전용 오류 코드
- [ ] NTAG424 DNA 전용 91xx 상태 코드 전체 확보 (Table 23, 27~95)
  - [ ] 91AE - 인증 오류
  - [ ] 917E - 길이 오류
  - [ ] 911E - 무결성 오류 (MAC 검증 실패)
  - [ ] 911D - 권한 거부
  - [ ] 911C - 파라미터 오류
  - [ ] 기타 91xx 코드 조사 및 문서화

### 1.3 오류 코드 구현
- [ ] 모든 SW 코드와 설명을 담은 `ApduStatusWord`/`ErrorCode` enum 완성
- [ ] 오류 처리 유틸리티 클래스 작성

## 2. 기본 통신 프로토콜 구축

### 2.1 PC/SC 통신 레이어
- [ ] PC/SC 통신 레이어 재검증 (javax.smartcardio API)
- [ ] 리더 자동 감지 로직 구현
- [ ] 카드 접속/해제 관리 로직 구현

### 2.2 APDU 명령 검증
- [ ] APDU 형식(CLA/INS/P1/P2/Lc/Data/Le) 검증
- [ ] APDU 빌더 클래스 리팩터링
- [ ] APDU 응답 파서 구현

## 3. 암호 헬퍼 함수 구현

### 3.1 기본 암호 함수
- [ ] Rnd_RotateLeft() (1바이트 좌회전) 구현
  - [ ] 단위 테스트 작성
  - [ ] AN12196 예제 값으로 검증

### 3.2 MAC 처리 함수
- [ ] MAC_Truncate() 함수 구현 (Response MAC 홀수 인덱스 추출)
  - [ ] 인덱스 [1,3,5,7,9,11,13,15] 추출 로직 구현
  - [ ] 단위 테스트 작성

- [ ] MAC_Truncate_Command() 함수 구현 (Command MAC 상위 8바이트)
  - [ ] 0~7바이트 추출 로직 구현
  - [ ] 단위 테스트 작성

### 3.3 고급 암호 함수
- [ ] PRF_CMAC() 함수 구현 (NIST SP 800-38B 기반)
  - [ ] CMAC 알고리즘 구현
  - [ ] KDF용 PRF 구현
  - [ ] 단위 테스트 작성

- [ ] SV_Construct() 함수 구현 (SV1/SV2 생성 공식)
  - [ ] SV1 = 0x00||0x01||0x00||0x80||RndA[0:1]||TI[0:3]||RndB[0:5]||0x00*5
  - [ ] SV2 = 0x00||0x02||0x00||0x80||RndA[0:1]||TI[0:3]||RndB[0:5]||0x00*5
  - [ ] 단위 테스트 작성

### 3.4 패딩 구현
- [ ] ISO/IEC 9797-1 Padding Method 2 구현 검증
  - [ ] 0x80 추가 후 0x00으로 패딩
  - [ ] PKCS#7과 차이점 문서화
  - [ ] 단위 테스트 작성

## 4. EV2 인증 모듈 구현

### 4.1 AuthenticateEV2First Part 1
- [ ] AuthenticateEV2First Part 1 구현 (0x90 0x71, RndB 수신)
  - [ ] APDU 생성: 90 71 00 00 02 [KeyNo] [LenCap] 00
  - [ ] 암호화된 RndB 응답 처리

- [ ] RndB 복호화 구현 (IV=0, AES-128 CBC)
  - [ ] IV = 16바이트 0x00 설정
  - [ ] AES-128 CBC로 복호화
  - [ ] 복호화된 RndB 저장

### 4.2 AuthenticateEV2First Part 2
- [ ] AuthenticateEV2First Part 2 구현 (0x90 0xAF, RndA||RndB' 전송)
  - [ ] RndA 랜덤 16바이트 생성
  - [ ] RndB 좌로 1바이트 회전(RndB')
  - [ ] RndA||RndB' 결합 후 암호화
  - [ ] APDU 생성 및 전송

### 4.3 인증 응답 처리
- [ ] TI/RndA'/PDcap2/PCDcap2 응답 파싱 및 복호화
  - [ ] 응답 데이터 복호화
  - [ ] TI(4바이트) 추출/저장
  - [ ] RndA' 검증 (회전된 RndA와 비교)
  - [ ] PDcap2, PCDcap2 파싱

### 4.4 세션 키 생성
- [ ] 세션 키 파생(KSesAuthENC, KSesAuthMAC)
  - [ ] SV1 생성 후 PRF_CMAC → KSesAuthENC
  - [ ] SV2 생성 후 PRF_CMAC → KSesAuthMAC
  - [ ] 세션 키 저장 및 관리

### 4.5 카운터/식별자 관리
- [ ] CmdCtr 초기화/증분 로직 구현 (0x0000 시작)
  - [ ] 인증 직후 0x0000으로 초기화
  - [ ] 명령 실행마다 증분
  - [ ] CommMode별 증분 규칙 구현

- [ ] TI(Transaction Identifier) 저장 및 관리
  - [ ] 4바이트 TI 저장
  - [ ] 세션별 TI 관리

## 5. 보안 명령 실행 모듈 (CommMode.Full)

### 5.1 IV 생성
- [ ] constructIV() 함수 구현 (label + TI + CmdCtr + padding)
  - [ ] label(예: 0xA5)별 IV 생성
  - [ ] TI + CmdCtr 연결
  - [ ] ISO/IEC 9797-1 패딩 적용

### 5.2 암호화 및 MAC
- [ ] CommMode.Full 암호화 구현 (KSesAuthENC 사용 AES-CBC)
  - [ ] CmdData 암호화
  - [ ] CBC 모드 IV 처리

- [ ] CommMode.Full CMAC 계산 구현 (Cmd||CmdCtr||TI||CmdHeader||EncData)
  - [ ] CMAC 입력 데이터 구성
  - [ ] KSesAuthMAC 사용
  - [ ] MACt(상위 8바이트) 생성

## 6. 명령 래퍼 구현

### 6.1 키 관리 명령
- [ ] ChangeKey (C4h) 명령 래퍼 구현
  - [ ] 키 데이터 암호화
  - [ ] CMAC 생성
  - [ ] APDU 구성 및 전송
  - [ ] 응답 검증

### 6.2 구성 명령
- [ ] SetConfiguration (5Ch) 명령 래퍼 구현
  - [ ] 구성 옵션 직렬화
  - [ ] 암호화 및 MAC 처리
  - [ ] 응답 검증

- [ ] ChangeFileSettings (5Fh) 명령 래퍼 구현
  - [ ] 파일 설정 구조 정의
  - [ ] SDM 설정 인코딩
  - [ ] 암호화 및 MAC 처리
  - [ ] 응답 검증

### 6.3 데이터 명령
- [ ] WriteData (8Dh) CommMode.Full 구현
  - [ ] 오프셋/길이 인코딩
  - [ ] 데이터 암호화
  - [ ] CMAC 생성
  - [ ] 응답 검증

- [ ] ReadData (ADh) 명령 구현
  - [ ] Plain/MAC/Full 모드 지원
  - [ ] Full 모드 응답 복호화
  - [ ] MAC 검증

### 6.4 응답 처리
- [ ] Response MAC 검증 로직 구현 (0x9100 응답 처리)
  - [ ] 응답 데이터 파싱
  - [ ] Response MAC 계산
  - [ ] MAC 비교 및 검증

## 7. CLI 구현 및 테스트

### 7.1 CLI 인터페이스
- [ ] CLI 명령 파서 구현 (change_key, set_config, read_file 등)
  - [ ] 명령 파싱 로직
  - [ ] 파라미터 검증
  - [ ] 도움말 시스템

### 7.2 디버그 기능
- [ ] 디버그 모드 구현 (APDU 로깅, 암호 중간 값 출력)
  - [ ] APDU 송수신 로깅
  - [ ] 암호화 단계별 값 출력
  - [ ] 세션 키 및 MAC 값 표시

### 7.3 검증 및 테스트
- [ ] AN12196 Table 18 예제로 ChangeFileSettings 검증
  - [ ] 예제 입력 값 구현
  - [ ] 중간 계산 비교
  - [ ] 최종 APDU 검증

- [ ] 실물 NTAG424 DNA 태그로 전체 워크플로 테스트
  - [ ] 인증 테스트
  - [ ] 키 변경 테스트
  - [ ] SDM 설정 테스트
  - [ ] 데이터 읽기/쓰기 테스트

## 8. 문서화

### 8.1 개발 문서
- [ ] API 문서 작성
- [ ] 암호 프로토콜 흐름 다이어그램 작성
- [ ] 오류 처리 가이드 작성
