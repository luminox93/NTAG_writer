<div align="center">

![header](https://capsule-render.vercel.app/api?type=waving&color=gradient&customColorList=6&height=280&section=header&text=NTAG%20Writer&fontSize=80&fontAlignY=35&desc=NTAG424%20DNA%20자동%20프로그래밍%20도구&descAlignY=58&descSize=25)

</div>

<br>

## 📋 목차

<details open>
<summary>펼쳐서 전체 목차 보기</summary>

- [프로젝트 개요](#프로젝트-개요)
- [설계 철학](#설계-철학)
- [클린 아키텍처](#클린-아키텍처) 🏗️
- [전체 워크플로우](#전체-워크플로우) 📊
- [기술적 도전](#기술적-도전)
- [시작하기](#시작하기)
- [기술 스택](#기술-스택)

</details>

<br>

---

## 📋 프로젝트 개요

NTAG424 DNA 태그의 보안 설정을 자동화하는 프로그래밍 도구입니다. 공장에서 요구한 높은 개발 비용을 절감하기 위해 직접 개발했습니다.

### 문제 상황

- **비용 문제**: 공장에서 보안 설정 프로그래밍에 매우 높은 비용 요구
- **기존 도구 한계**: TagXplorer는 2019년 지원 종료, 수동 작업만 가능
- **반복 작업**: 대량 태그 처리 시 동일한 파라미터를 매번 입력
- **오류 위험**: 수동 입력으로 인한 설정 실수 가능성

### 해결 방안

태그를 리더기에 올리기만 하면 자동으로 모든 보안 설정이 완료되는 자동화 도구를 개발했습니다.


<br>

---

## 🎨 설계 철학

### 확장성과 유지보수성의 균형

프로젝트의 핵심 고민은 **"변경에는 닫혀있고 확장에는 열려있는 구조"**를 만드는 것이었습니다.

#### 적용한 설계 원칙들

**1. 클린 아키텍처 (의존성 역전)**

도메인이 인프라에 의존하지 않도록 포트/어댑터 패턴을 적용했습니다.

```java
// AS-IS: 도메인이 인프라에 직접 의존 (❌)
class SetupService {
    void setup() {
        String url = ConsoleHelper.input("URL 입력");  // UI에 직접 의존
        reader.sendCommand(cmd);                       // 하드웨어에 직접 의존
    }
}

// TO-BE: 추상화에만 의존 (✅)
interface UserInteractionPort {
    String requestInput(String prompt);
}

class SetupWorkflow {
    private final UserInteractionPort uiPort;  // 인터페이스에 의존

    void execute() {
        String url = uiPort.requestInput("URL");  // 구현체를 모름
    }
}
```

**2. 전략 패턴 (다양한 리더기 지원)**

NFC 리더기를 교체 가능하도록 전략 패턴을 적용했습니다.

```java
interface NfcReaderPort {
    boolean connect();
    byte[] sendCommand(byte[] command);
}

// 현재: Identiv 리더기
class IdentivNfcReader implements NfcReaderPort { }

// 추후: 다른 리더기 추가 가능
class ACR122UReader implements NfcReaderPort { }
class PN532Reader implements NfcReaderPort { }
```

**3. Command 패턴 (APDU 명령 객체화)**

각 APDU 명령을 객체로 캡슐화하여 재사용성을 높였습니다.

```java
interface ApduCommand {
    byte[] build();
    String name();
}

class SelectApplicationCommand implements ApduCommand { }
class AuthenticateEV2FirstCommand implements ApduCommand { }
```

**4. 불변 객체와 명확한 변수명**

- ❌ **No Setter**: 모든 도메인 객체는 불변
- ❌ **No 매직넘버**: 모든 상수는 의미있는 이름으로 정의
- ❌ **No 대충 지은 변수명**: `i` 대신 `columnIndex`, `barPosition` 등 사용

```java
// 불변 객체 - public final 필드
public final class Tag {
    public final byte[] uid;
    public final NtagType type;
    public final TagStatus status;

    // 상태 변경 시 새 인스턴스 반환
    public Tag withStatus(TagStatus newStatus) {
        return new Tag(uid, type, newStatus, ...);
    }
}
```
