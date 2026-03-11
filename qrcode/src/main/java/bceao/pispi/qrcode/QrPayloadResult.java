package bceao.pispi.qrcode;

/**
 * Résultat de la génération d'une payload EMV PI-SPI.
 *
 * @param payload la chaîne payload EMV complète (avec CRC)
 */
public record QrPayloadResult(String payload) {}
