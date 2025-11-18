# NTAG 424 DNA 상태 코드 레퍼런스

NT4H2421Gx 데이터시트(Table 23, 24, 27~95)와 AN12196 예제에서 확인되는 모든 상태 워드(SW1/SW2)를 한 곳에 정리했다. SW1이 `0x91`인 경우는 NTAG 고유 명령(CLA `0x90`)에 대한 응답이며, SW1이 `0x90`/`0x00` 혹은 ISO/IEC 7816-4 표준 영역인 경우에는 래핑된 ISO 명령(CLA `0x00`)에 해당한다.

> 표에 명시된 명령들은 상태 워드가 실제로 등장하는 대표 사례다. 동일한 오류 코드는 동일한 의미로 다른 명령에서도 재사용된다.

## 1. CLA = 0x90 (SW1 = 0x91) — NTAG 고유 명령

| 코드 | 이름 | 주요 발생 명령 | 설명 |
| --- | --- | --- | --- |
| `0x9100` | OPERATION_OK | 모든 NTAG 고유 명령 | 명령이 정상 완료됨. (Table 23)
| `0x910B` | COMMAND_NOT_FOUND | `Read_Sig` | Random ID 환경에서 인증 없이 접근하거나, 인증 후 MAC가 틀린 경우. (Table 95)
| `0x910C` | COMMAND_FORMAT_ERROR | `Read_Sig` | 명령 길이나 주소 형식이 허용 범위를 벗어남. (Table 95)
| `0x911C` | ILLEGAL_COMMAND_CODE | 전역 | CLA=0x90에서 지원하지 않는 INS를 호출함. (Table 23)
| `0x911E` | INTEGRITY_ERROR | `SetConfiguration`, `GetCardUID`, `ChangeKey`, `ChangeFileSettings`, `ReadData`, `WriteData` 등 | CMAC/CRC/패딩 검증 실패 혹은 암호문 구조가 잘못됨. SDM Read Counter 오버플로도 여기로 매핑됨. (Tables 23, 52, 62, 65, 71, 80, 83)
| `0x9140` | NO_SUCH_KEY | `Authenticate*`, `ChangeKey`, `ChangeFileSettings` | 지정한 키 번호가 존재하지 않음. (Tables 23, 27, 33, 39, 45, 65, 71)
| `0x917E` | LENGTH_ERROR | `Authenticate*`, `SetConfiguration`, `GetVersion`, `GetCardUID`, `ChangeKey`, `ChangeFileSettings`, `ReadData`, `WriteData` | 전송한 데이터 길이가 명령 정의와 다름. (Tables 23, 27, 30, 33, 36, 39, 42, 45, 48, 52, 59, 62, 65, 68, 71, 74, 77, 80, 83)
| `0x919D` | PERMISSION_DENIED | `Authenticate*`, `SetConfiguration`, `ChangeFileSettings`, `GetFileSettings`, `GetFileCounters`, `ReadData`, `WriteData` | 현재 인증 상태나 구성에서 해당 명령이 허용되지 않음(예: 필요한 키가 Fh가 아니고 인증되지 않음, PICC 레벨 선택, SDM 옵션 충돌 등). (Tables 23, 27, 33, 39, 45, 52, 71, 74, 77, 80, 83)
| `0x919E` | PARAMETER_ERROR | `Authenticate*`, `SetConfiguration`, `ChangeKey`, `ChangeFileSettings`, `GetFileSettings`, `GetFileCounters`, `ReadData`, `WriteData` | P1/P2/Data 필드 값이 허용 영역을 벗어남(예: 지원되지 않는 옵션, SDM 오프셋 중첩 등). (Tables 23, 27, 33, 39, 45, 52, 65, 68, 71, 74, 77, 80, 83)
| `0x91AD` | AUTHENTICATION_DELAY | `AuthenticateEV2NonFirst`, `AuthenticateLRPFirst`, `AuthenticateLRPNonFirst` | 실패 횟수 제한에 의해 지연 상태. 지정된 대기 시간이 끝나야 재시도 가능. (Tables 23, 33, 39, 45)
| `0x91AE` | AUTHENTICATION_ERROR | `Authenticate*`, `SetConfiguration`, `GetCardUID`, `ChangeFileSettings`, `ReadData`, `WriteData` | 인증 또는 세션 상태가 요구 사항과 다름(RndA/RndB 검증 실패, 인증 없이 보호 명령 실행 등). (Tables 23, 30, 36, 42, 48, 52, 62, 65, 71, 77, 80, 83)
| `0x91AF` | ADDITIONAL_FRAME | `AuthenticateEV2First`, `AuthenticateEV2NonFirst`, `AuthenticateLRP*`, `WriteData` 체인 모드 등 | 더 많은 프레임(AF)이 필요함(부분 응답). 성공 상태가 아니며 이어지는 AF를 전송해야 한다. (Table 23)
| `0x91BE` | BOUNDARY_ERROR | `WriteData`, `ISOUpdateBinary`(CommMode.Full 사용 시) | 파일 끝을 넘어 쓰기 시도. (Table 83)
| `0x91CA` | COMMAND_ABORTED | `Authenticate*`, `SetConfiguration`, `GetVersion`, `GetCardUID`, `ChangeKey`, `ChangeFileSettings`, `GetFileSettings`, `GetFileCounters`, `ReadData`, `WriteData`, `Read_Sig` | 체인 명령/멀티 패스가 미완료 상태에서 중단됨. 이전 응답에 대한 후속 프레임을 보내지 않았거나 도중 취소됨. (Tables 23, 27, 33, 39, 45, 52, 59, 62, 65, 68, 71, 74, 77, 80, 83, 95)
| `0x91EE` | MEMORY_ERROR | `Authenticate*`, `SetConfiguration`, `GetVersion`, `GetCardUID`, `ChangeKey`, `GetKeyVersion`, `ChangeFileSettings`, `GetFileSettings`, `GetFileCounters`, `ReadData`, `WriteData` | 비휘발성 메모리 읽기/쓰기 실패. (Tables 30, 33, 36, 39, 42, 45, 48, 52, 59, 62, 65, 68, 71, 74, 77, 80, 83)
| `0x91F0` | FILE_NOT_FOUND | `ChangeFileSettings`, `GetFileSettings`, `GetFileCounters`, `ReadData`, `WriteData` | 지정한 파일 번호가 존재하지 않음. (Tables 23, 71, 74, 77, 80, 83)

## 2. CLA = 0x00 — ISO/IEC 7816-4 래핑 명령 상태

### 2.1 공통 ISO 상태(데이터시트 Table 24)

| 코드 | 이름 | 설명 |
| --- | --- | --- |
| `0x9000` | NORMAL_PROCESSING | 명령 성공. |
| `0x6700` | WRONG_LENGTH | APDU 길이가 사양과 다름. |
| `0x6982` | SECURITY_STATUS_NOT_SATISFIED | 보안 조건 불충족(인증 누락, SDM 제한 초과 등). |
| `0x6985` | CONDITIONS_OF_USE_NOT_SATISFIED | 사용 조건 미충족 / 체인 명령 진행 중. |
| `0x6A80` | INCORRECT_PARAMETERS_IN_DATA | Data 필드 값이 잘못됨. |
| `0x6A82` | FILE_OR_APPLICATION_NOT_FOUND | 지정한 파일/애플리케이션이 없음. |
| `0x6A86` | INCORRECT_P1P2 | P1 또는 P2 값이 사양과 다름. |
| `0x6A87` | LC_INCONSISTENT_WITH_P1P2 | Lc 값이 P1/P2 조합과 일치하지 않음. |
| `0x6C00` | WRONG_LE | Le가 잘못됨. |
| `0x6Cxx` | WRONG_LE_WITH_HINT | SW2가 사용 가능한 정확한 응답 길이를 알려줌. |
| `0x6D00` | INSTRUCTION_NOT_SUPPORTED | 지원하지 않는 INS. |
| `0x6E00` | CLASS_NOT_SUPPORTED | 지원하지 않는 CLA. |

### 2.2 ISOSelectFile (Table 86)
- 0x9000: 성공
- 0x6700: APDU 길이 오류
- 0x6985: 체인 명령 진행 중 (종료 전)
- 0x6A82: 애플리케이션/파일 없음 (현재 선택된 애플리케이션은 유지)
- 0x6A86: P1/ P2 오류
- 0x6A87: Lc와 P1/P2 불일치
- 0x6E00: 잘못된 CLA

### 2.3 ISOReadBinary (Table 89)
- 0x9000: 성공
- 0x6581: 메모리 오류 (NVM 읽기 실패)
- 0x6700: 길이 오류
- 0x6982: 보안 조건 불충족 (SDM 카운터 초과, 인증 요구, EV2/LRP 금지 등)
- 0x6985: 체인 명령 진행 중 / 파일 미선택 / 대상 파일이 StandardData가 아님 / 애플리케이션에 TransactionMAC 파일 존재
- 0x6A82: 파일 없음
- 0x6A86: P1/P2 오류
- 0x6E00: CLA 오류

### 2.4 ISOUpdateBinary (Table 92)
- 0x9000: 성공
- 0x6581: 메모리 오류 (쓰기 실패)
- 0x6700: 길이 오류
- 0x6982: Free Write 조건, EV2/LRP 허용 여부 위반 등 보안 조건 위반
- 0x6985: 체인 명령 진행 중 / 파일 미선택 / 파일 경계 초과 쓰기 시도
- 0x6A82: 파일 없음
- 0x6A86: P1/P2 오류
- 0x6E00: CLA 오류

## 3. 참조
- `docs/NT4H2421Gx.pdf` — Table 23(상태 워드), Table 24(ISO 공통), Table 27~95(각 명령별 반환 코드)
- `docs/AN12196.pdf` — Table 18 등 NTAG 424 DNA Secure Messaging 예제. 오류 발생 시 어떤 명령을 보냈는지 맥락 확인용.
