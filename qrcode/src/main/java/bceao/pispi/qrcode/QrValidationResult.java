package bceao.pispi.qrcode;

import java.util.List;

/**
 * Résultat de la validation d'un payload PI-SPI.
 *
 * @param valid  true si la payload respecte la structure PI-SPI
 * @param errors liste des erreurs détectées (vide si valid est true)
 * @param data   champs extraits de la payload lorsqu'elle est valide, ou null
 */
public record QrValidationResult(
        boolean valid,
        List<String> errors,
        QrPayloadInput data
) {
    /**
     * Crée un résultat invalide avec des erreurs.
     */
    public static QrValidationResult invalid(List<String> errors) {
        return new QrValidationResult(false, errors, null);
    }

    /**
     * Crée un résultat valide avec les données extraites.
     */
    public static QrValidationResult valid(QrPayloadInput data) {
        return new QrValidationResult(true, List.of(), data);
    }
}
