# 개발 환경 및 APDU 통신 규약

## 1. 기본 개발 환경
- **언어**: Java 8 이상
- **의존 라이브러리**
  - `javax.smartcardio`: PC/SC 호환 NFC 리더기와 통신
  - `org.bouncycastle:bcprov-jdk15on`: AES-CMAC 등 복잡한 암호 알고리즘 구현
- **빌드 도구**: Gradle 7.x (Java 8 타깃)
- **원칙**: 단일 책임 원칙과 OOP 설계 규율을 우선시하여 각 모듈을 느슨하게 결합

## 2. APDU 통신 규약
- 모든 명령은 ISO/IEC 7816-4 프로토콜 형식으로 래핑된 APDU를 사용한다.
- NTAG424 DNA의 인증 명령은 CLA `0x90`을 사용하며, 표준 ISO 명령은 CLA `0x00`을 사용한다.

### 2.1 명령 APDU 구조
| 필드 | 길이(Byte) | 설명 |
| --- | --- | --- |
| CLA | 1 | Class byte. NTAG 고유 명령은 `0x90`, ISO 래핑 명령은 `0x00`. |
| INS | 1 | Instruction byte. 수행할 명령 코드. |
| P1  | 1 | Parameter 1. 명령별 세부 옵션. |
| P2  | 1 | Parameter 2. 명령별 세부 옵션. |
| Lc  | 0 또는 1 | 전송할 데이터(CmdData)의 길이. 데이터가 없으면 생략. |
| CmdData | 0~255 | 명령에 포함되는 데이터 바이트. |
| Le  | 0 또는 1 | 기대하는 응답 데이터 길이. 0 또는 생략하면 리더기 기본값. |

### 2.2 인증 명령 기본 규칙
- Authenticate 계열 명령(APDU CLA `0x90`)은 NTAG 고유 명령이므로 항상 `0x90` 클래스를 유지한다.
- EV2 인증 1/2단계, SDM 관련 보안 명령 모두 위 구조를 따른다.

### 2.3 ISO 래핑 명령
- 파일 선택, NDEF 읽기 등 ISO 표준 명령은 CLA `0x00`을 사용한다.
- 동일한 APDU 구조를 그대로 사용하며, INS 값만 ISO/IEC 7816-4 정의에 따른다.

## 3. 구현시 고려 사항
- APDU 빌더, 리더기 어댑터, 암호 서비스, 인증 워크플로를 각각 독립 클래스로 분리해 단일 책임을 지키고, 생성자 주입으로 의존성을 연결한다.
- Bouncy Castle은 AES-CMAC, AES-CBC 등 모든 암호 처리를 담당하며, 키/벡터 관리 로직은 별도의 서비스에서 처리한다.
- PC/SC 리더기는 전략 패턴(`NfcReaderStrategy`)으로 추상화해 다른 기기 교체 시 영향도를 최소화한다.
