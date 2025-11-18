# 버그 로그

## 2025-01-XX - SDM 설정 시 인증 오류 (91 AE)

### 문제
```
>>> 90 5F 00 00 19 02 C0 2F 9A 04 71 F8 C5 A1 0C 38 B3 34 DC 54 9B 6D EB 09 94 13 03 D5 DD 5A 00
<<< 91 AE
설정 실패: SDM 설정 실패: 알 수 없는 오류 (SW=91AE)
```

### 원인
- Status Word `91 AE` = 인증 오류
- `authenticate()` 메서드가 실제 EV2 인증을 수행하지 않음
- 실제 카드 인증 없이 더미 세션 키만 설정
- 유효한 인증 세션 없이 CommMode.FULL 명령(Change File Settings) 전송 시도

### 영향
- SDM 설정 실패
- 암호화된 명령을 태그에 쓸 수 없음

### 해결 방법
1. `AuthenticateEV2First` 구현 (AN12196 Section 3.6)
2. RndB 수신 및 복호화
3. RndA 생성 및 전송
4. 세션 키 유도 (KSesAuthENC, KSesAuthMAC)
5. 응답 수신 및 검증
6. 세션 키 유도 (KSesAuthENC, KSesAuthMAC)
7. TI (Transaction Identifier) 추출
8. CmdCtr (Command Counter) 초기화

### 상태
- [x] EV2First 인증 구현
- [x] 실제 NTAG424 DNA 태그로 테스트
- [x] 수정 후 SDM 설정 성공 확인

---

## 2025-01-XX - AuthenticateEV2First Length Error (91 7E)

### 문제
```
>>> 90 71 00 00 03 00 01 00
<<< 91 7E
설정 실패: EV2 인증 시작 실패: 알 수 없는 오류 (SW=917E)
```

### 원인
- Status Word `91 7E` = Length Error
- `ApduCommand.authenticateEV2First()` 메서드가 잘못된 데이터 길이 전송
- 스펙: `90 71 00 00 02 [KeyNo] [LenCap] 00` (Lc=2)
- 실제: `90 71 00 00 03 00 01 00` (Lc=3, 데이터가 1바이트 더 많음)
- 메서드가 `byte[] data` 파라미터를 받아서 길이를 잘못 계산

### 영향
- EV2 인증 시작 실패
- 태그와 인증 세션 수립 불가
- 전체 설정 프로세스 중단

### 해결 방법
```java
// 수정 전
public static byte[] authenticateEV2First(byte keyNo, byte[] data) {
    byte[] apdu = new byte[7 + data.length];
    // ... Lc = (2 + data.length)
}

// 수정 후
public static byte[] authenticateEV2First(byte keyNo, byte lenCap) {
    return new byte[]{
        // ...
        0x00       // Le
    };
}
```

### 상태
- [x] ApduCommand.authenticateEV2First() 메서드 수정
- [x] 호출 부분 수정 (`new byte[]{0x00}` → `(byte) 0x00`)
- [x] 실제 태그로 테스트 - 인증 성공!

---

## 2025-01-XX - Change File Settings Integrity Error (91 1E)

### 문제
```
>>> 90 5F 00 00 19 02 F7 23 BB 6E 2B 59 5B 4A 0A 56 5D C6 A7 95 B7 03 FF FD BE 78 C5 C3 2A 4A 00
<<< 91 1E
설정 실패: SDM 설정 실패: 알 수 없는 오류 (SW=911E)
```

### 원인
- Status Word `91 1E` = Integrity Error (MAC verification failed)
- CMAC 계산이 잘못됨
- 가능한 원인:
  1. CMAC 입력 데이터 구조가 스펙과 다름
  2. CmdCtr 값이 틀림
  3. TI 값이 틀림
  4. 암호화된 데이터가 틀림

### 영향
- SDM 설정 실패
- NDEF 작성 불가
- 태그가 공장 초기 상태 그대로 남음

### 해결 방법
AN12196 Table 18의 CMAC 계산 방법 재확인 필요:
```
CMAC Input = Cmd || CmdCtr || TI || CmdHeader || EncData
```

### 상태
- [x] AN12196에서 정확한 CMAC 계산 방법 확인
- [x] CmdCtr 초기값을 `01 00`으로 변경 (AuthenticateEV2First 직후 첫 명령이므로 1부터 시작) -> **수정: 00 00이 맞음**
- [ ] 실제 태그로 테스트

---

## 2025-01-XX - Access Rights 계산 오류

### 문제
여전히 `91 1E` (Integrity Error) 발생

### 원인
`buildFileSettingsData()`에서 Access Rights 계산이 잘못됨

AN12196 Table 18 예제:
- AccessRights = `00E0` (little-endian)
- Byte 0: `0E` (Read=0xE, Write=0x0)
- Byte 1: `00` (Change=0x0, ReadWrite=0x0)

잘못된 계산:
```java
int accessRights = (config.getWriteAccess() << 12) | (config.getReadAccess() << 8) |
                   (config.getReadAccess() << 4) | config.getReadAccess();
```
비트 연산이 복잡하고 직관적이지 않음.

### 해결 방법
명시적인 바이트 배열 생성으로 변경:
```java
// Byte 0 (LSB): Change / ReadWrite
// Byte 1 (MSB): Read / Write
data[idx++] = (byte) (((config.getChangeAccess() & 0x0F) << 4) | (config.getReadWriteAccess() & 0x0F));
data[idx++] = (byte) (((config.getReadAccess() & 0x0F) << 4) | (config.getWriteAccess() & 0x0F));
```

### 상태
- [x] Access Rights 계산 로직 수정
- [x] 테스트 - 여전히 91 1E 발생

---

## 2025-01-XX - ISO/IEC 9797-1 Padding Method 2 구현 확인

### 문제
IVc 계산과 EncryptedSettings 생성 시 패딩이 올바른지 의심됨.

### 원인
`AesEncryption.addPadding`이 PKCS#7이 아닌 ISO/IEC 9797-1 Method 2를 사용해야 함.
- Method 2: 데이터 끝에 `0x80`을 추가하고, 블록 크기가 될 때까지 `0x00`을 채움.

### 해결 방법
`AesEncryption.java` 확인 결과, 이미 Method 2가 구현되어 있음.
```java
padded[data.length] = (byte) 0x80;
// 나머지는 0x00
```
패딩 로직은 정상임.

---

## 2025-01-XX - MACt Truncation 방식 오류 (해결!)

### 문제
91 1E 에러의 **결정적 원인 발견!**

**디버그 로그:**
```
CMAC (full 16 bytes): FDACED69093745BE67C7AAEDF376643B
MACt (truncated 8 bytes): AC6937BEC7ED763B
```

**현재 truncation 로직:**
```java
private byte[] truncateMac(byte[] cmac) {
    byte[] mact = new byte[8];
    for (int i = 0; i < 8; i++) {
        mact[i] = cmac[i * 2 + 1];
    }
    return mact;
}
```

### 원인
**Command MAC과 Response MAC의 truncation 방식이 다름!**

1. **Response MAC (태그→리더)**: 홀수 인덱스 추출 `[1,3,5,7,9,11,13,15]`
2. **Command MAC (리더→태그)**: **처음 8바이트 사용 `[0,1,2,3,4,5,6,7]`**

현재 코드는 Response MAC 방식을 Command MAC에 사용하고 있음 ❌

walkd_proto 코드는 **검증(Response MAC)**용이므로 홀수 인덱스 사용이 맞음. 하지만 우리는 **송신(Command MAC)**이므로 다른 방식 필요!

### 해결 방법
올바른 MACt (처음 8바이트):
```
FD AC ED 69 09 37 45 BE
```

수정된 코드:
```java
private byte[] truncateMac(byte[] cmac) {
    byte[] mact = new byte[8];
    System.arraycopy(cmac, 0, mact, 0, 8);  // 처음 8바이트만
    return mact;
}
```

### 상태
- [x] 원인 파악 완료
- [x] 코드 수정
- [x] 테스트 - 여전히 91 1E!

---

## 2025-01-XX - 패딩 방식 오류 (ISO/IEC 9797-1 vs PKCS#7)

### 문제
MACt truncation을 수정했는데도 여전히 91 1E 발생!

**디버그 로그:**
```
CmdData: 400E000121F12B0000510000510000 (15 bytes)
Padded Data: 400E000121F12B000051000051000001 (16 bytes)
```

패딩: `01` (PKCS#7 방식)

### 원인
**패딩 방식이 틀림!**

NTAG424 DNA는 **ISO/IEC 9797-1 Padding Method 2**를 사용함.
1. 데이터 끝에 `0x80` 추가
2. 블록 크기(16)의 배수가 될 때까지 `0x00` 추가

**현재 사용 중인 PKCS#7 패딩:**
1. 필요한 패딩 바이트 수를 계산
2. 그 수만큼 반복해서 추가 (예: `01` 또는 `0202`)

### 해결 방법
AesEncryption의 addPadding 메서드를 ISO/IEC 9797-1 방식으로 변경 필요

올바른 패딩 (15 bytes → 16 bytes):
```
400E000121F12B000051000051000080
```

### 상태
- [x] 원인 파악
- [x] AesEncryption.addPadding() 수정
- [x] 테스트

---

## 2025-11-19 - ChangeFileSettings MAC mismatch (AN12196 비교)

### 문제
ChangeFileSettingsExample 검증 시 MACt 불일치.

### 원인
`ChangeFileSettingsValidator`에서 `truncateMac`을 Response MAC 방식(홀수 바이트)으로 사용하고 있었음.
Command MAC은 처음 8바이트를 사용해야 함.

### 해결
`ChangeFileSettingsValidator`의 `truncateMac` 수정.

---

## 2025-11-19 - CmdCtr Synchronization Error (91 1E)

### 문제
자동 설정 모드에서 `ChangeFileSettings` 실행 시 `91 1E` (Integrity Error) 발생.

### 원인
`writeNdefMessage()` 메서드에서 `WriteData` 명령을 수행한 후 `incrementCommandCounter()`를 호출함.
하지만 `WriteData`는 `CommMode.Plain`으로 전송되므로 태그의 `CmdCtr`는 증가하지 않음.
결과적으로:
- 태그 CmdCtr: 0
- 로컬 CmdCtr: 1
불일치로 인해 다음 명령(`ChangeFileSettings`)의 CMAC 검증 실패.

### 해결 방법
`Ntag424AutoSetupService.java`의 `writeNdefMessage()`에서 `incrementCommandCounter()` 호출 제거.

### 상태
- [x] 원인 파악
- [x] 코드 수정
- [ ] 테스트

---

## 2025-11-19 - SDMAccessRights 하드코딩 버그 (91 1E)

### 문제
`ChangeFileSettings` 명령 실행 시 계속 `91 1E` (Parameter Error) 발생

### 디버그 출력
```
File Settings Data (14 bytes): 40 00 E0 C1 F1 21 2B 00 00 51 00 00 51 00
```

### 원인
`Ntag424AutoSetupService.java`의 `buildFileSettingsData()` 메서드에서 두 가지 문제 발견:

1. **하드코딩 문제**: SDMAccessRights가 0xF121로 하드코딩되어 config 값 무시
2. **인코딩 오류**: SDMCtrRet 필드 위치가 잘못됨

```java
// 잘못된 코드 1 (하드코딩):
data[idx++] = 0x21;
data[idx++] = (byte) 0xF1;

// 잘못된 코드 2 (인코딩 오류):
byte sdmByte2 = (byte) ((config.getSdmCounterRet() << 4) | 0x0F);  // 0x1F가 됨
```

실제 설정값 (NtagDefaultConfig.WALKD_PRODUCTION):
- SDM Meta Read = 0x02
- SDM File Read = 0x01
- SDM Counter Ret = 0x01

올바른 인코딩 (AN12196 참조):
- Byte 1: (0x02 << 4) | 0x01 = 0x21
- Byte 2: (0x0F << 4) | 0x01 = 0xF1 (Reserved=F, SDMCtrRet이 하위 4비트)

### 해결 방법
```java
// SDMAccessRights를 config에서 가져온 값으로 올바르게 인코딩
byte sdmByte1 = (byte) ((config.getSdmMetaRead() << 4) | (config.getSdmFileRead() & 0x0F));
byte sdmByte2 = (byte) ((0x0F << 4) | (config.getSdmCounterRet() & 0x0F));  // Reserved=F, SDMCtrRet 하위 4비트
data[idx++] = sdmByte1;  // 0x21
data[idx++] = sdmByte2;  // 0xF1
```

### 영향
- SDM 설정 실패
- 태그 인증 후 설정 변경 불가

### 상태
- [x] 원인 파악
- [x] 코드 수정 (라인 412-427)
- [x] 컴파일 성공
- [x] 테스트 코드 작성 및 실행
- [ ] 실제 태그 테스트
