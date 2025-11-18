package ntagwriter.domain.port;

import java.util.List;

/**
 * 사용자 인터랙션 포트 인터페이스
 * UI와 도메인 레이어를 분리하기 위한 추상화
 */
public interface UserInteractionPort {

    /**
     * 정보 메시지 표시
     *
     * @param message 표시할 메시지
     */
    void showInfo(String message);

    /**
     * 경고 메시지 표시
     *
     * @param message 경고 메시지
     */
    void showWarning(String message);

    /**
     * 오류 메시지 표시
     *
     * @param message 오류 메시지
     */
    void showError(String message);

    /**
     * 성공 메시지 표시
     *
     * @param message 성공 메시지
     */
    void showSuccess(String message);

    /**
     * 섹션 구분 표시
     *
     * @param title 섹션 제목
     */
    void showSection(String title);

    /**
     * 진행 상황 표시
     *
     * @param current 현재 단계
     * @param total 전체 단계
     * @param description 설명
     */
    void showProgress(int current, int total, String description);

    /**
     * 사용자로부터 텍스트 입력 받기
     *
     * @param prompt 입력 프롬프트
     * @return 사용자 입력 문자열
     */
    String requestInput(String prompt);

    /**
     * 사용자로부터 텍스트 입력 받기 (기본값 포함)
     *
     * @param prompt 입력 프롬프트
     * @param defaultValue 기본값
     * @return 사용자 입력 문자열 또는 기본값
     */
    String requestInput(String prompt, String defaultValue);

    /**
     * 사용자로부터 비밀번호 입력 받기
     *
     * @param prompt 입력 프롬프트
     * @return 비밀번호 문자열
     */
    String requestPassword(String prompt);

    /**
     * 사용자로부터 확인 받기 (Y/N)
     *
     * @param prompt 확인 메시지
     * @return 확인하면 true, 취소하면 false
     */
    boolean requestConfirmation(String prompt);

    /**
     * 선택지 중 하나를 선택받기
     *
     * @param prompt 선택 프롬프트
     * @param options 선택 가능한 옵션들
     * @return 선택된 옵션의 인덱스 (0부터 시작)
     */
    int requestChoice(String prompt, List<String> options);

    /**
     * 16진수 데이터 표시
     *
     * @param label 레이블
     * @param data 바이트 배열
     */
    void showHexData(String label, byte[] data);

    /**
     * 테이블 형태로 데이터 표시
     *
     * @param headers 헤더 목록
     * @param rows 데이터 행들
     */
    void showTable(List<String> headers, List<List<String>> rows);

    /**
     * 작업 시작 알림
     *
     * @param taskName 작업 이름
     */
    void beginTask(String taskName);

    /**
     * 작업 완료 알림
     *
     * @param taskName 작업 이름
     * @param success 성공 여부
     */
    void endTask(String taskName, boolean success);
}