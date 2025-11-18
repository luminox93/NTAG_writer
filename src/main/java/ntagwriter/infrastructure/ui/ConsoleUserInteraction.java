package ntagwriter.infrastructure.ui;

import ntagwriter.domain.port.UserInteractionPort;
import ntagwriter.util.HexUtils;

import java.util.List;
import java.util.Scanner;

/**
 * 콘솔 기반 사용자 인터랙션 구현
 */
public class ConsoleUserInteraction implements UserInteractionPort {

    private final Scanner scanner = new Scanner(System.in);

    // ANSI 색상 코드
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_CYAN = "\u001B[36m";

    // UI 상수
    private static final String INFO_SYMBOL = "ℹ ";
    private static final String WARNING_SYMBOL = "⚠ ";
    private static final String ERROR_SYMBOL = "✗ ";
    private static final String SUCCESS_SYMBOL = "✓ ";
    private static final String TASK_START_SYMBOL = "▶ ";
    private static final String SECTION_BORDER = "═══ ";

    // 진행 표시줄 설정
    private static final int PROGRESS_BAR_LENGTH = 20;
    private static final int PERCENTAGE_MAX = 100;
    private static final String PROGRESS_FILLED = "█";
    private static final String PROGRESS_EMPTY = "░";

    // 테이블 표시 설정
    private static final String TABLE_VERTICAL_BORDER = "│";
    private static final String TABLE_HORIZONTAL_BORDER = "─";
    private static final String TABLE_CROSS = "┼";
    private static final String TABLE_LEFT_JUNCTION = "├";
    private static final String TABLE_RIGHT_JUNCTION = "┤";
    private static final int TABLE_PADDING = 2;

    // 16진수 표시 설정
    private static final int HEX_BYTES_PER_LINE = 16;
    private static final int HEX_ADDRESS_WIDTH = 4;

    @Override
    public void showInfo(String message) {
        System.out.println(ANSI_CYAN + INFO_SYMBOL + ANSI_RESET + message);
    }

    @Override
    public void showWarning(String message) {
        System.out.println(ANSI_YELLOW + WARNING_SYMBOL + ANSI_RESET + message);
    }

    @Override
    public void showError(String message) {
        System.out.println(ANSI_RED + ERROR_SYMBOL + ANSI_RESET + message);
    }

    @Override
    public void showSuccess(String message) {
        System.out.println(ANSI_GREEN + SUCCESS_SYMBOL + ANSI_RESET + message);
    }

    @Override
    public void showSection(String title) {
        System.out.println();
        System.out.println(ANSI_BLUE + SECTION_BORDER + title + " " + SECTION_BORDER + ANSI_RESET);
        System.out.println();
    }

    @Override
    public void showProgress(int current, int total, String description) {
        int percentage = (current * PERCENTAGE_MAX) / total;
        int filledBarLength = (PROGRESS_BAR_LENGTH * current) / total;

        StringBuilder progressBar = new StringBuilder("[");
        for (int barPosition = 0; barPosition < PROGRESS_BAR_LENGTH; barPosition++) {
            if (barPosition < filledBarLength) {
                progressBar.append(PROGRESS_FILLED);
            } else {
                progressBar.append(PROGRESS_EMPTY);
            }
        }
        progressBar.append("]");

        System.out.printf("\r%s %3d%% [%d/%d] %s",
            progressBar.toString(), percentage, current, total, description);

        if (current == total) {
            System.out.println();
        }
    }

    @Override
    public String requestInput(String prompt) {
        System.out.print(prompt + ": ");
        return scanner.nextLine().trim();
    }

    @Override
    public String requestInput(String prompt, String defaultValue) {
        System.out.print(prompt + " [" + defaultValue + "]: ");
        String input = scanner.nextLine().trim();
        return input.isEmpty() ? defaultValue : input;
    }

    @Override
    public String requestPassword(String prompt) {
        // Console 모드에서는 마스킹 불가능
        System.out.print(prompt + " (입력이 화면에 표시됩니다): ");
        return scanner.nextLine();
    }

    @Override
    public boolean requestConfirmation(String prompt) {
        while (true) {
            System.out.print(prompt + " (Y/N): ");
            String input = scanner.nextLine().trim().toUpperCase();
            if (input.equals("Y") || input.equals("YES")) {
                return true;
            } else if (input.equals("N") || input.equals("NO")) {
                return false;
            }
            showWarning("Y 또는 N을 입력해주세요.");
        }
    }

    @Override
    public int requestChoice(String prompt, List<String> options) {
        System.out.println(prompt);

        for (int optionIndex = 0; optionIndex < options.size(); optionIndex++) {
            int displayNumber = optionIndex + 1;
            System.out.printf("  %d. %s%n", displayNumber, options.get(optionIndex));
        }

        int maxOption = options.size();
        while (true) {
            System.out.print("선택 [1-" + maxOption + "]: ");
            try {
                int userChoice = Integer.parseInt(scanner.nextLine().trim());
                if (userChoice >= 1 && userChoice <= maxOption) {
                    return userChoice - 1;  // 0-based index 반환
                }
            } catch (NumberFormatException ignored) {
                // 숫자가 아닌 입력 무시
            }
            showWarning("1부터 " + maxOption + " 사이의 숫자를 입력해주세요.");
        }
    }

    @Override
    public void showHexData(String label, byte[] data) {
        if (data == null || data.length == 0) {
            System.out.println(label + ": (empty)");
            return;
        }

        String hexString = HexUtils.bytesToHex(data);
        System.out.println(label + ": " + hexString);

        // 16바이트씩 줄바꿈하여 표시
        if (data.length > HEX_BYTES_PER_LINE) {
            for (int byteOffset = 0; byteOffset < data.length; byteOffset += HEX_BYTES_PER_LINE) {
                int endOffset = Math.min(byteOffset + HEX_BYTES_PER_LINE, data.length);
                System.out.printf("  %0" + HEX_ADDRESS_WIDTH + "X: ", byteOffset);

                for (int byteIndex = byteOffset; byteIndex < endOffset; byteIndex++) {
                    System.out.printf("%02X ", data[byteIndex]);
                }
                System.out.println();
            }
        }
    }

    @Override
    public void showTable(List<String> headers, List<List<String>> rows) {
        if (headers.isEmpty() || rows.isEmpty()) {
            return;
        }

        // 각 컬럼의 최대 너비 계산
        int[] columnWidths = calculateColumnWidths(headers, rows);

        // 헤더 출력
        printTableRow(headers, columnWidths);
        printTableSeparator(columnWidths);

        // 데이터 출력
        for (List<String> row : rows) {
            printTableRow(row, columnWidths);
        }
    }

    private int[] calculateColumnWidths(List<String> headers, List<List<String>> rows) {
        int[] widths = new int[headers.size()];

        // 헤더 너비로 초기화
        for (int columnIndex = 0; columnIndex < headers.size(); columnIndex++) {
            widths[columnIndex] = headers.get(columnIndex).length();
        }

        // 각 행의 데이터 너비 확인
        for (List<String> row : rows) {
            for (int columnIndex = 0; columnIndex < Math.min(row.size(), headers.size()); columnIndex++) {
                String cellValue = row.get(columnIndex);
                widths[columnIndex] = Math.max(widths[columnIndex], cellValue.length());
            }
        }

        return widths;
    }

    private void printTableRow(List<String> values, int[] columnWidths) {
        System.out.print(TABLE_VERTICAL_BORDER);

        for (int columnIndex = 0; columnIndex < columnWidths.length; columnIndex++) {
            String cellValue = columnIndex < values.size() ? values.get(columnIndex) : "";
            int columnWidth = columnWidths[columnIndex];
            System.out.printf(" %-" + columnWidth + "s " + TABLE_VERTICAL_BORDER, cellValue);
        }
        System.out.println();
    }

    private void printTableSeparator(int[] columnWidths) {
        System.out.print(TABLE_LEFT_JUNCTION);

        for (int columnIndex = 0; columnIndex < columnWidths.length; columnIndex++) {
            int borderWidth = columnWidths[columnIndex] + TABLE_PADDING;

            for (int borderPosition = 0; borderPosition < borderWidth; borderPosition++) {
                System.out.print(TABLE_HORIZONTAL_BORDER);
            }

            boolean isLastColumn = (columnIndex == columnWidths.length - 1);
            System.out.print(isLastColumn ? TABLE_RIGHT_JUNCTION : TABLE_CROSS);
        }
        System.out.println();
    }

    @Override
    public void beginTask(String taskName) {
        System.out.println();
        System.out.println(ANSI_BLUE + TASK_START_SYMBOL + taskName + ANSI_RESET);
    }

    @Override
    public void endTask(String taskName, boolean success) {
        if (success) {
            System.out.println(ANSI_GREEN + SUCCESS_SYMBOL + taskName + " 완료" + ANSI_RESET);
        } else {
            System.out.println(ANSI_RED + ERROR_SYMBOL + taskName + " 실패" + ANSI_RESET);
        }
    }
}