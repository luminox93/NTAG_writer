package ntagwriter.debug;

import ntagwriter.domain.NtagDefaultConfig;
import ntagwriter.util.HexUtils;

/**
 * SDMAccessRights 인코딩 테스트
 * 버그 수정 확인용
 */
public class TestSDMAccessRights {

    public static void main(String[] args) {
        NtagDefaultConfig config = NtagDefaultConfig.WALKD_PRODUCTION;

        System.out.println("===== SDMAccessRights 인코딩 테스트 =====");
        System.out.println();

        System.out.println("설정값:");
        System.out.println("  SDM Meta Read: 0x" + String.format("%02X", config.getSdmMetaRead()));
        System.out.println("  SDM File Read: 0x" + String.format("%02X", config.getSdmFileRead()));
        System.out.println("  SDM Counter Ret: 0x" + String.format("%02X", config.getSdmCounterRet()));
        System.out.println();

        // 올바른 인코딩 계산
        byte sdmByte1 = (byte) ((config.getSdmMetaRead() << 4) | (config.getSdmFileRead() & 0x0F));
        byte sdmByte2 = (byte) ((config.getSdmCounterRet() << 4) | 0x0F);

        System.out.println("계산된 SDMAccessRights (2 bytes):");
        System.out.println("  Byte 1: 0x" + String.format("%02X", sdmByte1) + " = (0x" +
                         String.format("%02X", config.getSdmMetaRead()) + " << 4) | 0x" +
                         String.format("%02X", config.getSdmFileRead()));
        System.out.println("  Byte 2: 0x" + String.format("%02X", sdmByte2) + " = (0x" +
                         String.format("%02X", config.getSdmCounterRet()) + " << 4) | 0x0F");
        System.out.println();

        System.out.println("이전 하드코딩값 (버그):");
        System.out.println("  0x21 0xF1 (잘못된 값)");
        System.out.println();

        System.out.println("수정된 값:");
        System.out.println("  0x" + String.format("%02X", sdmByte1) + " 0x" + String.format("%02X", sdmByte2));
        System.out.println();

        // 전체 File Settings 데이터 구성 확인
        System.out.println("전체 File Settings 구조:");
        System.out.println("  FileOption: 0x40");
        System.out.println("  AccessRights: 0x00 0xE0 (Change=00, RW=00, Read=0E, Write=00)");
        System.out.println("  SDMOptions: 0xC1");
        System.out.println("  SDMAccessRights: 0x" + String.format("%02X", sdmByte1) + " 0x" + String.format("%02X", sdmByte2));
        System.out.println("  PICCDataOffset: " + config.getPiccDataOffset() + " (0x" +
                         String.format("%06X", config.getPiccDataOffset()) + ")");
        System.out.println("  SDMMACOffset: " + config.getSdmMacOffset() + " (0x" +
                         String.format("%06X", config.getSdmMacOffset()) + ")");
        System.out.println("  SDMMACInputOffset: " + config.getSdmMacInputOffset() + " (0x" +
                         String.format("%04X", config.getSdmMacInputOffset()) + ")");
    }
}