# NTAG424 DNA 설정 워크플로우

## 1. 전체 워크플로우 개요

```mermaid
flowchart TD
    Start([시작]) --> Init[초기화]
    Init --> ConnectReader[리더기 연결]

    ConnectReader -->|성공| DetectTag[태그 감지]
    ConnectReader -->|실패| Error1[리더기 오류]

    DetectTag -->|감지됨| CheckType{NTAG424?}
    DetectTag -->|타임아웃| Error2[태그 없음]

    CheckType -->|예| SelectApp[애플리케이션 선택]
    CheckType -->|아니오| Error3[잘못된 태그]

    SelectApp --> Auth[EV2 인증]

    Auth -->|성공| ConfigSDM[SDM 설정]
    Auth -->|실패| Error4[인증 실패]

    ConfigSDM --> WriteNDEF[NDEF 쓰기]
    WriteNDEF --> ChangeKeys[키 변경]
    ChangeKeys --> Verify[검증]

    Verify -->|성공| Complete([완료])
    Verify -->|실패| Error5[검증 실패]

    Error1 --> End([종료])
    Error2 --> End
    Error3 --> End
    Error4 --> End
    Error5 --> End

    style Start fill:#e1f5e1
    style Complete fill:#e1f5e1
    style Error1 fill:#ffe1e1
    style Error2 fill:#ffe1e1
    style Error3 fill:#ffe1e1
    style Error4 fill:#ffe1e1
    style Error5 fill:#ffe1e1
```

## 2. EV2 인증 프로세스

```mermaid
sequenceDiagram
    participant PCD as Reader (PCD)
    participant PICC as Tag (PICC)
    participant Crypto as Crypto Module

    Note over PCD,PICC: Step 1: 인증 시작
    PCD->>PICC: AuthenticateEV2First(KeyNo=0x00)
    PICC->>Crypto: Generate RndB (16 bytes)
    Crypto-->>PICC: RndB
    PICC->>Crypto: Encrypt(RndB, K_auth)
    Crypto-->>PICC: E(K_auth, RndB)
    PICC-->>PCD: E(K_auth, RndB) + SW=9100

    Note over PCD,PICC: Step 2: 챌린지 응답
    PCD->>Crypto: Decrypt(E(RndB), K_auth)
    Crypto-->>PCD: RndB
    PCD->>Crypto: Generate RndA (16 bytes)
    Crypto-->>PCD: RndA
    PCD->>Crypto: RotateLeft(RndB)
    Crypto-->>PCD: RndB'
    PCD->>Crypto: Encrypt(RndA || RndB', K_auth)
    Crypto-->>PCD: E(K_auth, RndA || RndB')
    PCD->>PICC: AuthenticateEV2Part2(E(RndA || RndB'))

    Note over PCD,PICC: Step 3: 상호 인증 완료
    PICC->>Crypto: Decrypt(E(RndA || RndB'), K_auth)
    Crypto-->>PICC: RndA || RndB'
    PICC->>Crypto: Verify RndB' = RotateLeft(RndB)
    PICC->>Crypto: RotateLeft(RndA)
    Crypto-->>PICC: RndA'
    PICC->>Crypto: Generate TI (4 bytes)
    PICC->>Crypto: Encrypt(TI || RndA' || PDcap || PCDcap)
    Crypto-->>PICC: E(K_auth, response)
    PICC-->>PCD: E(K_auth, response) + SW=9000

    Note over PCD,PICC: Step 4: 세션 키 생성
    PCD->>Crypto: Decrypt(E(response), K_auth)
    Crypto-->>PCD: TI || RndA' || PDcap || PCDcap
    PCD->>Crypto: Verify RndA' = RotateLeft(RndA)
    PCD->>Crypto: GenerateSessionKeys(K_auth, RndA, RndB)
    Crypto-->>PCD: K_SesAuthENC, K_SesAuthMAC

    rect rgb(200, 240, 200)
        Note over PCD: 인증 완료!<br/>세션 키 획득
    end
```

## 3. SDM (Secure Dynamic Messaging) 설정

```mermaid
flowchart LR
    subgraph "SDM 파라미터 설정"
        URL[Base URL 설정]
        UID[UID 미러링 활성화]
        CTR[Read Counter 활성화]
        MAC[MAC 추가]
    end

    subgraph "오프셋 계산"
        PIC[PICDataOffset]
        MACO[SDMMACOffset]
        ENCO[SDMENCOffset]
    end

    subgraph "NDEF 메시지 구성"
        NDEF[NDEF Header]
        URI[URI Record]
        PARAM[URL Parameters]
    end

    URL --> PIC
    UID --> PIC
    CTR --> MACO
    MAC --> MACO

    PIC --> URI
    MACO --> PARAM
    ENCO --> PARAM

    URI --> NDEF
    PARAM --> NDEF
```

## 4. 보안 메시징 프로토콜

```mermaid
sequenceDiagram
    participant App as Application
    participant SM as SecureMessaging
    participant Tag as NTAG424

    Note over App,Tag: ChangeFileSettings 명령
    App->>SM: PrepareCommand(settings, keys)

    rect rgb(220, 240, 255)
        Note right of SM: 1. IV 생성
        SM->>SM: IVc = generateIV(TI, CmdCtr, padding)

        Note right of SM: 2. 데이터 암호화
        SM->>SM: EncData = AES-CBC(K_SesAuthENC, IVc, settings)

        Note right of SM: 3. MAC 계산
        SM->>SM: MAC = CMAC(K_SesAuthMAC, Cmd || CmdCtr || TI || EncData)
        SM->>SM: MACt = truncate(MAC, 8)
    end

    SM-->>App: EncData || MACt
    App->>Tag: ChangeFileSettings(0x02, EncData || MACt)
    Tag-->>App: Response + SW=9100

    rect rgb(220, 255, 220)
        Note over App: CmdCtr 증가
        App->>App: CmdCtr++
    end
```

## 5. 키 변경 프로세스

```mermaid
flowchart TD
    Start([키 변경 시작]) --> GenKey[새 키 생성<br/>16 bytes random]

    GenKey --> EncKey[키 암호화<br/>K_new XOR K_old]

    EncKey --> CalcCRC[CRC32 계산]

    CalcCRC --> PadData[패딩 추가<br/>0x80 + 0x00...]

    PadData --> EncData[데이터 암호화<br/>AES-CBC]

    EncData --> CalcMAC[MAC 계산<br/>CMAC]

    CalcMAC --> SendCmd[ChangeKey 명령 전송]

    SendCmd -->|성공| UpdateKeys[로컬 키 업데이트]
    SendCmd -->|실패| Error[키 변경 실패]

    UpdateKeys --> End([완료])
    Error --> End

    style Start fill:#e1f5e1
    style End fill:#e1f5e1
    style Error fill:#ffe1e1
```

## 6. 핸들러 실행 순서

```mermaid
graph TB
    subgraph "Phase 1: 준비"
        H1[ConnectReaderHandler]
        H2[DetectTagHandler]
    end

    subgraph "Phase 2: 인증"
        H3[SelectApplicationHandler]
        H4[AuthenticateHandler]
    end

    subgraph "Phase 3: 설정"
        H5[ConfigureSdmHandler]
        H6[WriteNdefHandler]
    end

    subgraph "Phase 4: 보안"
        H7[ChangeKeysHandler]
        H8[VerifyHandler]
    end

    H1 -->|CONNECTED| H2
    H2 -->|TAG_DETECTED| H3
    H3 -->|APP_SELECTED| H4
    H4 -->|AUTHENTICATED| H5
    H5 -->|SDM_CONFIGURED| H6
    H6 -->|NDEF_WRITTEN| H7
    H7 -->|KEYS_CHANGED| H8
    H8 -->|VERIFIED| Complete([완료])

    style H1 fill:#e1e5f5
    style H2 fill:#e1e5f5
    style H3 fill:#f5f5e1
    style H4 fill:#f5f5e1
    style H5 fill:#e1f5e1
    style H6 fill:#e1f5e1
    style H7 fill:#f5e1e1
    style H8 fill:#f5e1e1
```

## 7. 오류 처리 및 복구

```mermaid
stateDiagram-v2
    [*] --> Normal: 정상 실행

    Normal --> Error: 오류 발생

    state Error {
        [*] --> Identify: 오류 식별
        Identify --> Recoverable: 복구 가능
        Identify --> Fatal: 복구 불가능

        Recoverable --> Retry: 재시도
        Retry --> Recovery: 복구 시도
        Recovery --> [*]: 성공

        Fatal --> Cleanup: 정리
        Cleanup --> [*]: 종료
    }

    Error --> Normal: 복구 성공
    Error --> Terminated: 복구 실패

    Normal --> Completed: 완료
    Completed --> [*]
    Terminated --> [*]
```

## 8. 워크플로우 모드별 동작

| 모드 | 설명 | 사용자 개입 | 적용 시나리오 |
|------|------|------------|--------------|
| **INTERACTIVE** | 대화형 모드 | 각 단계마다 확인 요청 | 개발/디버깅 |
| **AUTOMATIC** | 자동 모드 | 개입 없음 | 대량 처리 |
| **STEP_BY_STEP** | 단계별 모드 | 단계 완료 후 대기 | 교육/데모 |

## 9. 컨텍스트 데이터 흐름

```mermaid
graph LR
    subgraph "Input"
        I1[SDM Config]
        I2[Workflow Mode]
        I3[Default Keys]
    end

    subgraph "Context Attributes"
        C1[Current Tag]
        C2[Session Keys]
        C3[Transaction ID]
        C4[Command Counter]
        C5[RndA/RndB]
    end

    subgraph "Output"
        O1[Configured Tag]
        O2[Step Results]
        O3[Error Messages]
    end

    I1 --> C1
    I2 --> C1
    I3 --> C2

    C1 --> O1
    C2 --> O1
    C3 --> O1
    C4 --> O2
    C5 --> O3
```

## 10. 성능 고려사항

| 단계 | 예상 시간 | 병목 요소 | 최적화 방안 |
|------|----------|----------|------------|
| 리더기 연결 | < 1초 | USB 초기화 | 연결 풀링 |
| 태그 감지 | 0.5-30초 | 사용자 액션 | 시각/청각 피드백 |
| 인증 | < 0.5초 | 암호화 연산 | 세션 키 캐싱 |
| SDM 설정 | < 1초 | APDU 통신 | 배치 명령 |
| NDEF 쓰기 | < 2초 | 메모리 쓰기 | 청크 단위 쓰기 |
| 키 변경 | < 1초 | 암호화 연산 | 병렬 처리 |
| 검증 | < 1초 | 읽기/비교 | 체크섬 활용 |

**총 예상 시간**: 5-40초 (태그 감지 시간에 따라 변동)