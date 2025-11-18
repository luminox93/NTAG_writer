# NTAG Writer 아키텍처 문서

## 1. 클린 아키텍처 레이어 구조

```mermaid
graph TB
    subgraph "Domain Layer (핵심)"
        D1[Domain Entities<br/>Tag, NtagType, TagStatus]
        D2[Domain Ports<br/>Interfaces]
        D3[Value Objects<br/>SdmConfiguration]
    end

    subgraph "Application Layer (유스케이스)"
        A1[Workflow<br/>SetupWorkflow]
        A2[Handlers<br/>StepHandler]
        A3[Context<br/>WorkflowContext]
    end

    subgraph "Infrastructure Layer (외부)"
        I1[NFC Reader<br/>IdentivNfcReader]
        I2[UI<br/>ConsoleUserInteraction]
        I3[APDU<br/>Command Pattern]
        I4[Crypto<br/>AES, CMAC]
    end

    subgraph "Main"
        M[NtagWriterApplication]
    end

    %% 의존성 방향 (아래에서 위로)
    M --> A1
    A1 --> A2
    A2 --> D2
    I1 -.-> D2
    I2 -.-> D2
    I3 --> I1
    I4 -.-> D2

    style D1 fill:#e1f5e1
    style D2 fill:#e1f5e1
    style D3 fill:#e1f5e1
    style A1 fill:#e1e5f5
    style A2 fill:#e1e5f5
    style A3 fill:#e1e5f5
    style I1 fill:#f5e1e1
    style I2 fill:#f5e1e1
    style I3 fill:#f5e1e1
    style I4 fill:#f5e1e1
```

## 2. 의존성 역전 원칙 (DIP)

```mermaid
graph LR
    subgraph "도메인 (안정적)"
        P1[NfcReaderPort<br/><<interface>>]
        P2[UserInteractionPort<br/><<interface>>]
        P3[CryptoPort<br/><<interface>>]
    end

    subgraph "인프라 (변경 가능)"
        I1[IdentivNfcReader]
        I2[ConsoleUserInteraction]
        I3[CryptoAdapter]
    end

    subgraph "애플리케이션"
        H1[ConnectReaderHandler]
        H2[DetectTagHandler]
    end

    I1 -.implements.-> P1
    I2 -.implements.-> P2
    I3 -.implements.-> P3

    H1 --uses--> P1
    H1 --uses--> P2
    H2 --uses--> P1
    H2 --uses--> P2

    style P1 fill:#ffe6cc
    style P2 fill:#ffe6cc
    style P3 fill:#ffe6cc
```

## 3. 워크플로우 상태 다이어그램

```mermaid
stateDiagram-v2
    [*] --> INITIALIZED: 시작

    INITIALIZED --> CONNECTING: connect()
    CONNECTING --> CONNECTED: 성공
    CONNECTING --> ERROR: 실패

    CONNECTED --> DETECTING_TAG: detectTag()
    DETECTING_TAG --> TAG_DETECTED: 태그 감지
    DETECTING_TAG --> ERROR: 타임아웃

    TAG_DETECTED --> AUTHENTICATING: authenticate()
    AUTHENTICATING --> AUTHENTICATED: 인증 성공
    AUTHENTICATING --> ERROR: 인증 실패

    AUTHENTICATED --> CONFIGURING_SDM: configureSdm()
    CONFIGURING_SDM --> SDM_CONFIGURED: 설정 완료
    CONFIGURING_SDM --> ERROR: 설정 실패

    SDM_CONFIGURED --> WRITING_NDEF: writeNdef()
    WRITING_NDEF --> NDEF_WRITTEN: 쓰기 완료
    WRITING_NDEF --> ERROR: 쓰기 실패

    NDEF_WRITTEN --> CHANGING_KEYS: changeKeys()
    CHANGING_KEYS --> KEYS_CHANGED: 키 변경 완료
    CHANGING_KEYS --> ERROR: 키 변경 실패

    KEYS_CHANGED --> VERIFYING: verify()
    VERIFYING --> COMPLETED: 검증 성공
    VERIFYING --> ERROR: 검증 실패

    ERROR --> [*]: 종료
    COMPLETED --> [*]: 완료
    CANCELLED --> [*]: 취소
```

## 4. 핸들러 체인 패턴

```mermaid
sequenceDiagram
    participant M as Main
    participant W as Workflow
    participant C as Context
    participant H1 as ConnectReaderHandler
    participant H2 as DetectTagHandler
    participant H3 as AuthenticateHandler
    participant R as NfcReaderPort
    participant U as UserInteractionPort

    M->>W: execute(context)

    rect rgb(200, 220, 240)
        Note over W,H1: Step 1: 리더기 연결
        W->>H1: execute(context)
        H1->>U: beginTask("리더기 연결")
        H1->>R: connect()
        R-->>H1: true
        H1->>U: showSuccess("연결 성공")
        H1->>C: transitionTo(CONNECTED)
    end

    rect rgb(220, 240, 200)
        Note over W,H2: Step 2: 태그 감지
        W->>H2: execute(context)
        H2->>U: showInfo("태그를 올려놓으세요")
        H2->>R: waitForTag(30000)
        R-->>H2: Tag
        H2->>C: updateTag(tag)
        H2->>C: transitionTo(TAG_DETECTED)
    end

    rect rgb(240, 220, 200)
        Note over W,H3: Step 3: 인증
        W->>H3: execute(context)
        H3->>R: sendCommand(authCmd)
        R-->>H3: response
        H3->>C: setAttribute("sessionKeys", keys)
        H3->>C: transitionTo(AUTHENTICATED)
    end

    W-->>M: WorkflowResult
```

## 5. APDU Command 패턴 구조

```mermaid
classDiagram
    class ApduCommand {
        <<interface>>
        +build() byte[]
        +name() String
        +description() String
        +expectedResponseLength() int
    }

    class SelectApplicationCommand {
        +byte[] aid
        +build() byte[]
        +ntag424() SelectApplicationCommand$
    }

    class AuthenticateEV2FirstCommand {
        +byte keyNumber
        +byte[] lengthCapability
        +build() byte[]
    }

    class ChangeKeyCommand {
        +byte keyNumber
        +byte[] encryptedKeyData
        +byte[] mac
        +build() byte[]
    }

    class WriteDataCommand {
        +byte fileNumber
        +int offset
        +byte[] data
        +build() byte[]
    }

    ApduCommand <|.. SelectApplicationCommand
    ApduCommand <|.. AuthenticateEV2FirstCommand
    ApduCommand <|.. ChangeKeyCommand
    ApduCommand <|.. WriteDataCommand
```

## 6. 도메인 모델 관계

```mermaid
erDiagram
    Tag ||--o| NtagType : has
    Tag ||--o| TagStatus : has
    Tag {
        byte[] uid
        NtagType type
        TagStatus status
        LocalDateTime detectedAt
        byte[] atr
        int memorySize
        boolean isFormatted
        boolean isLocked
    }

    NtagType {
        String displayName
        int totalMemory
        boolean supportsSdm
    }

    TagStatus {
        String description
    }

    WorkflowContext ||--o| Tag : contains
    WorkflowContext ||--o| SdmConfiguration : contains
    WorkflowContext ||--o| WorkflowState : has
    WorkflowContext {
        WorkflowMode mode
        SdmConfiguration sdmConfig
        Tag currentTag
        WorkflowState state
        Map attributes
    }

    SdmConfiguration {
        String baseUrl
        byte[] sdmMetaReadKey
        byte[] sdmFileReadKey
        boolean enableUidMirroring
        boolean enableReadCounter
        boolean enableEncryption
        int picDataOffset
        int sdmMacOffset
        int sdmEncOffset
    }
```

## 7. 패키지 구조

```
src/main/java/ntagwriter/
├── domain/                    # 도메인 레이어 (핵심 비즈니스)
│   ├── port/                  # 포트 인터페이스 (의존성 역전)
│   │   ├── NfcReaderPort
│   │   ├── UserInteractionPort
│   │   └── CryptoPort
│   ├── tag/                   # 태그 관련 도메인 모델
│   │   ├── Tag
│   │   ├── NtagType
│   │   └── TagStatus
│   └── config/                # 설정 값 객체
│       └── SdmConfiguration
│
├── application/               # 애플리케이션 레이어 (유스케이스)
│   ├── workflow/              # 워크플로우 관리
│   │   ├── SetupWorkflow
│   │   ├── WorkflowContext
│   │   ├── WorkflowState
│   │   ├── WorkflowMode
│   │   └── WorkflowResult
│   └── handler/               # 단계별 핸들러
│       ├── StepHandler
│       ├── ConnectReaderHandler
│       ├── DetectTagHandler
│       └── AuthenticateHandler
│
├── infrastructure/            # 인프라스트럭처 레이어 (외부 연동)
│   ├── nfc/                   # NFC 관련
│   │   ├── reader/
│   │   │   └── IdentivNfcReader
│   │   └── apdu/              # APDU 명령
│   │       ├── ApduCommand
│   │       ├── SelectApplicationCommand
│   │       └── AuthenticateEV2FirstCommand
│   ├── ui/                    # 사용자 인터페이스
│   │   └── ConsoleUserInteraction
│   └── persistence/           # 영속성 (추후 구현)
│       └── FileConfigRepository
│
├── crypto/                    # 암호화 유틸리티 (기존)
│   ├── AesEncryption
│   ├── CmacCalculator
│   ├── ByteRotation
│   └── SecureMessaging
│
└── util/                      # 공통 유틸리티
    ├── HexUtils
    └── LoggerHelper
```

## 8. 주요 설계 원칙

### 8.1 SOLID 원칙 적용

- **S**RP (단일 책임): 각 클래스는 하나의 책임만 가짐
- **O**CP (개방-폐쇄): 확장에는 열려있고 수정에는 닫혀있음
- **L**SP (리스코프 치환): 인터페이스 구현체는 상호 교체 가능
- **I**SP (인터페이스 분리): 작고 구체적인 인터페이스
- **D**IP (의존성 역전): 추상화에 의존, 구체화에 의존하지 않음

### 8.2 패턴 적용

- **포트/어댑터 패턴**: 도메인과 인프라 분리
- **전략 패턴**: StepHandler로 각 단계 캡슐화
- **커맨드 패턴**: APDU 명령 객체화
- **빌더 패턴**: SdmConfiguration, WorkflowResult
- **불변 객체 패턴**: 도메인 엔티티의 불변성 보장

### 8.3 코드 품질

- **매직넘버 제거**: 모든 상수 명시적 정의
- **명확한 변수명**: 의미있는 이름 사용
- **불변성**: public final 필드, no setter
- **순수 함수**: 부작용 최소화