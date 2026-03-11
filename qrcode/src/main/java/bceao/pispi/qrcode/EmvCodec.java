package bceao.pispi.qrcode;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Encodage / décodage des segments EMV (tag + longueur 2 chiffres + valeur).
 */
public final class EmvCodec {

    private EmvCodec() {}

    /**
     * Formate un objet de données EMV : id (2) + length (2 chiffres) + value.
     */
    public static String formatDataObject(String id, String value) {
        String length = value.length() <= 9
                ? "0" + value.length()
                : String.valueOf(value.length());
        return id + length + value;
    }

    /**
     * Parse les segments EMV de haut niveau (tags 00, 36, 52, ...).
     * En cas d'erreur, les messages sont ajoutés à errors et le parsing s'arrête.
     */
    public static ParseResult parseSegments(String value) {
        Map<String, String> segments = new LinkedHashMap<>();
        java.util.List<String> errors = new java.util.ArrayList<>();
        int cursor = 0;

        try {
            while (cursor < value.length()) {
                if (cursor + 4 > value.length()) {
                    break;
                }
                String tag = value.substring(cursor, cursor + 2);
                String lengthStr = value.substring(cursor + 2, cursor + 4);
                int length;
                try {
                    length = Integer.parseInt(lengthStr, 10);
                } catch (NumberFormatException e) {
                    errors.add("Longueur invalide pour le tag " + tag + ".");
                    break;
                }
                if (length < 0) {
                    errors.add("Longueur invalide pour le tag " + tag + ".");
                    break;
                }

                int valueStart = cursor + 4;
                int valueEnd = valueStart + length;
                if (valueEnd > value.length()) {
                    errors.add("Segment " + tag + " tronqué.");
                    break;
                }

                segments.put(tag, value.substring(valueStart, valueEnd));
                cursor = valueEnd;
            }
        } catch (Exception e) {
            errors.add("Erreur lors de l'analyse de la payload: " + (e.getMessage() != null ? e.getMessage() : e.toString()));
        }

        return new ParseResult(segments, errors);
    }

    /**
     * Parse les sous-champs d'un segment (ex. contenu du tag 36 ou 62).
     * @throws IllegalArgumentException si format invalide
     */
    public static Map<String, String> parseSubFields(String data) {
        Map<String, String> segments = new LinkedHashMap<>();
        int cursor = 0;

        while (cursor < data.length()) {
            if (cursor + 4 > data.length()) {
                throw new IllegalArgumentException("Sous-segment tronqué");
            }
            String tag = data.substring(cursor, cursor + 2);
            String lengthStr = data.substring(cursor + 2, cursor + 4);
            int length;
            try {
                length = Integer.parseInt(lengthStr, 10);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Longueur invalide pour le sous-tag " + tag);
            }
            if (length < 0) {
                throw new IllegalArgumentException("Longueur invalide pour le sous-tag " + tag);
            }

            int valueStart = cursor + 4;
            int valueEnd = valueStart + length;
            if (valueEnd > data.length()) {
                throw new IllegalArgumentException("Sous-segment " + tag + " tronqué");
            }

            segments.put(tag, data.substring(valueStart, valueEnd));
            cursor = valueEnd;
        }

        return segments;
    }

    public record ParseResult(Map<String, String> segments, java.util.List<String> errors) {}
}
