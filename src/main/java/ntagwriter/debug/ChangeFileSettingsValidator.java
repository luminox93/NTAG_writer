package ntagwriter.debug;

import ntagwriter.service.CryptoService;
import ntagwriter.util.HexUtils;

import java.security.GeneralSecurityException;

/**
 * Utility that reproduces AN12196 Table 18 calculations to validate our implementation.
 */
public class ChangeFileSettingsValidator {

    public static void main(String[] args) throws GeneralSecurityException {
        ChangeFileSettingsExample example = ChangeFileSettingsExample.docExample();
        CryptoService cryptoService = new CryptoService();

        byte[] paddedData = cryptoService.addPadding(example.getCmdData(), 16);
        byte[] ivInput = buildIvInput(example.getTi(), example.getCmdCtr());
        byte[] ivc = cryptoService.encryptECB(example.getKSesAuthEnc(), ivInput);
        byte[] encryptedSettings = cryptoService.encryptCBC(example.getKSesAuthEnc(), ivc, paddedData);

        byte[] cmacInput = buildCmacInput(example.getCmdCtr(), example.getTi(), example.getFileNumber(), encryptedSettings);
        byte[] cmac = cryptoService.calculateCmac(example.getKSesAuthMac(), cmacInput);
        byte[] macT = truncateMac(cmac);

        System.out.println("--- ChangeFileSettings Doc Example Validation ---");
        System.out.println("Expected EncryptedSettings: " + HexUtils.bytesToHex(example.getExpectedEncryptedSettings()));
        System.out.println("Actual   EncryptedSettings: " + HexUtils.bytesToHex(encryptedSettings));
        System.out.println("Match: " + java.util.Arrays.equals(example.getExpectedEncryptedSettings(), encryptedSettings));

        System.out.println();
        System.out.println("Expected MACt: " + HexUtils.bytesToHex(example.getExpectedMacT()));
        System.out.println("Actual   MACt: " + HexUtils.bytesToHex(macT));
        System.out.println("Match: " + java.util.Arrays.equals(example.getExpectedMacT(), macT));
    }

    private static byte[] buildIvInput(byte[] ti, byte[] cmdCtr) {
        byte[] ivInput = new byte[16];
        ivInput[0] = (byte) 0xA5;
        ivInput[1] = (byte) 0x5A;
        System.arraycopy(ti, 0, ivInput, 2, 4);
        System.arraycopy(cmdCtr, 0, ivInput, 6, 2);
        return ivInput;
    }

    private static byte[] buildCmacInput(byte[] cmdCtr, byte[] ti, byte fileNumber, byte[] encryptedSettings) {
        byte[] cmacInput = new byte[1 + cmdCtr.length + ti.length + 1 + encryptedSettings.length];
        int idx = 0;
        cmacInput[idx++] = (byte) 0x5F;
        System.arraycopy(cmdCtr, 0, cmacInput, idx, cmdCtr.length); idx += cmdCtr.length;
        System.arraycopy(ti, 0, cmacInput, idx, ti.length); idx += ti.length;
        cmacInput[idx++] = fileNumber;
        System.arraycopy(encryptedSettings, 0, cmacInput, idx, encryptedSettings.length);
        return cmacInput;
    }

    private static byte[] truncateMac(byte[] cmac) {
        byte[] macT = new byte[8];
        for (int i = 0; i < 8; i++) {
            macT[i] = cmac[i * 2 + 1];
        }
        return macT;
    }
}
