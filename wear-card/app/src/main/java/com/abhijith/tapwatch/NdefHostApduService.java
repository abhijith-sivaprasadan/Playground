package com.abhijith.tapwatch;

import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;

import java.util.Arrays;

/**
 * Minimal NFC Forum Type 4 Tag / NDEF URI emulator.
 *
 * If the watch exposes android.hardware.nfc.hce, a phone reading the watch as a
 * Type 4 tag can retrieve CardConfig.CARD_URL as an NDEF URI. The app still
 * provides a QR fallback because NFC reader behaviour varies by device/vendor.
 */
public class NdefHostApduService extends HostApduService {

    private static final byte[] SW_SUCCESS = hex("9000");
    private static final byte[] SW_FILE_NOT_FOUND = hex("6A82");
    private static final byte[] SW_CONDITIONS_NOT_SATISFIED = hex("6986");
    private static final byte[] SW_WRONG_P1P2 = hex("6B00");
    private static final byte[] SW_INS_NOT_SUPPORTED = hex("6D00");

    private static final byte[] NDEF_AID = hex("D2760000850101");
    private static final int FILE_NONE = 0;
    private static final int FILE_CC = 1;
    private static final int FILE_NDEF = 2;

    // NFC Forum Type 4 Tag Capability Container (15 bytes), mapping v2.0.
    // NDEF file E104, maximum size 1024 bytes, read allowed, write denied.
    private static final byte[] CC_FILE = hex("000F2000FF00FF0406E104040000FF");

    private int selectedFile = FILE_NONE;
    private byte[] ndefFile;

    @Override
    public void onCreate() {
        super.onCreate();
        NdefRecord uri = NdefRecord.createUri(CardConfig.CARD_URL);
        byte[] payload = new NdefMessage(new NdefRecord[]{uri}).toByteArray();
        ndefFile = new byte[payload.length + 2];
        ndefFile[0] = (byte) ((payload.length >> 8) & 0xFF);
        ndefFile[1] = (byte) (payload.length & 0xFF);
        System.arraycopy(payload, 0, ndefFile, 2, payload.length);
    }

    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {
        if (commandApdu == null || commandApdu.length < 4) {
            return SW_INS_NOT_SUPPORTED;
        }

        if (isSelectNdefApplication(commandApdu)) {
            selectedFile = FILE_NONE;
            return SW_SUCCESS;
        }

        if (isSelectFile(commandApdu, 0xE103)) {
            selectedFile = FILE_CC;
            return SW_SUCCESS;
        }

        if (isSelectFile(commandApdu, 0xE104)) {
            selectedFile = FILE_NDEF;
            return SW_SUCCESS;
        }

        // READ BINARY: 00 B0 P1 P2 Le
        if ((commandApdu[0] & 0xFF) == 0x00 && (commandApdu[1] & 0xFF) == 0xB0) {
            byte[] source;
            if (selectedFile == FILE_CC) {
                source = CC_FILE;
            } else if (selectedFile == FILE_NDEF) {
                source = ndefFile;
            } else {
                return SW_CONDITIONS_NOT_SATISFIED;
            }

            if (commandApdu.length < 5) {
                return SW_WRONG_P1P2;
            }
            int offset = ((commandApdu[2] & 0xFF) << 8) | (commandApdu[3] & 0xFF);
            int requested = commandApdu[4] & 0xFF;
            if (requested == 0) requested = 256;
            if (offset > source.length) return SW_WRONG_P1P2;

            int end = Math.min(source.length, offset + requested);
            return concat(Arrays.copyOfRange(source, offset, end), SW_SUCCESS);
        }

        if ((commandApdu[1] & 0xFF) == 0xA4) {
            return SW_FILE_NOT_FOUND;
        }
        return SW_INS_NOT_SUPPORTED;
    }

    private static boolean isSelectNdefApplication(byte[] command) {
        // 00 A4 04 00 07 D2760000850101 [Le]
        if (command.length < 12) return false;
        if ((command[0] & 0xFF) != 0x00 || (command[1] & 0xFF) != 0xA4 ||
                (command[2] & 0xFF) != 0x04 || (command[4] & 0xFF) != 0x07) {
            return false;
        }
        for (int i = 0; i < NDEF_AID.length; i++) {
            if (command[5 + i] != NDEF_AID[i]) return false;
        }
        return true;
    }

    private static boolean isSelectFile(byte[] command, int fileId) {
        // Accept SELECT FILE with P2=0C or P2=00.
        if (command.length < 7) return false;
        if ((command[0] & 0xFF) != 0x00 || (command[1] & 0xFF) != 0xA4 ||
                (command[2] & 0xFF) != 0x00 || (command[4] & 0xFF) != 0x02) {
            return false;
        }
        int selected = ((command[5] & 0xFF) << 8) | (command[6] & 0xFF);
        return selected == fileId;
    }

    @Override
    public void onDeactivated(int reason) {
        selectedFile = FILE_NONE;
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    private static byte[] hex(String value) {
        int len = value.length();
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            out[i / 2] = (byte) Integer.parseInt(value.substring(i, i + 2), 16);
        }
        return out;
    }
}
