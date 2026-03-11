package bceao.pispi.qrcode;

/**
 * Calcul CRC-16 (CCITT, polynôme 0x1021, valeur initiale 0xFFFF).
 * CCITT (Consultative Committee for International Telephony and Telegraphy)
 */
public final class CrcUtil {

    private static final int INITIAL = 0xFFFF;
    private static final int POLYNOMIAL = 0x1021;

    private CrcUtil() {}

    /**
     * Calcule le CRC-16 de la chaîne d'entrée et retourne une chaîne hexadécimale
     * de 4 caractères en majuscules (ex. "BEEF").
     */
    public static String computeCrc16(String input) {
        int crc = INITIAL;
        for (int i = 0; i < input.length(); i++) {
            int codePoint = input.charAt(i);
            crc ^= codePoint << 8;
            for (int j = 0; j < 8; j++) {
                boolean hasHighBit = (crc & 0x8000) == 0;
                crc = hasHighBit ? crc << 1 : (crc << 1) ^ POLYNOMIAL;
                crc &= 0xFFFF;
            }
        }
        return String.format("%04X", crc);
    }
}
