package ntagwriter.debug;

import ntagwriter.domain.NtagDefaultConfig;

/**
 * SDMAccessRights 인코딩 수정 확인
 */
public class TestSDMAccessRightsFix {

    public static void main(String[] args) {
        NtagDefaultConfig config = NtagDefaultConfig.WALKD_PRODUCTION;

        System.out.println("===== SDMAccessRights 인코딩 수정 테스트 =====");
        System.out.println();

        System.out.println("설정값:");
        System.out.println("  SDM Meta Read: 0x" + String.format("%02X", config.getSdmMetaRead()));
        System.out.println("  SDM File Read: 0x" + String.format("%02X", config.getSdmFileRead()));
        System.out.println("  SDM Counter Ret: 0x" + String.format("%02X", config.getSdmCounterRet()));
        System.out.println();

        // 이전 잘못된 인코딩
        byte wrongByte1 = (byte) ((config.getSdmMetaRead() << 4) | config.getSdmFileRead());
        byte wrongByte2 = (byte) ((config.getSdmCounterRet() << 4) | 0x0F);
        System.out.println("잘못된 인코딩 (버그):");
        System.out.println("  Byte 1: 0x" + String.format("%02X", wrongByte1));
        System.out.println("  Byte 2: 0x" + String.format("%02X", wrongByte2) + " (SDMCtrRet이 상위 4비트에 위치)");
        System.out.println("  결과: 0x21 0x1F");
        System.out.println();

        // 수정된 올바른 인코딩
        byte correctByte1 = (byte) ((config.getSdmMetaRead() << 4) | (config.getSdmFileRead() & 0x0F));
        byte correctByte2 = (byte) ((0x0F << 4) | (config.getSdmCounterRet() & 0x0F));
        System.out.println("올바른 인코딩 (수정됨):");
        System.out.println("  Byte 1: 0x" + String.format("%02X", correctByte1) + " = (0x02 << 4) | 0x01");
        System.out.println("  Byte 2: 0x" + String.format("%02X", correctByte2) + " = (0x0F << 4) | 0x01 (Reserved=F, SDMCtrRet=1)");
        System.out.println("  결과: 0x21 0xF1");
        System.out.println();

        System.out.println("AN12196 예제와 비교:");
        System.out.println("  AN12196 SDMAccessRights: 0xF121 (little-endian)");
        System.out.println("  → Byte 1: 0x21, Byte 2: 0xF1");
        System.out.println("  수정된 값과 일치: " + (correctByte1 == 0x21 && correctByte2 == (byte)0xF1));
    }
}