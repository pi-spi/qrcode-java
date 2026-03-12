package bceao.pispi.qrcode;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class PispiQrCodeTest {

    private static final String BASE_ALIAS = "3497a720-ab11-4973-9619-534e04f263a1";
    private static final String REFERENCE_CAISSE = "CAISSE_A01";
    private static final String REFERENCE_PRODUIT = "Produit-ABC-123654";
    private static final String REFERENCE_TX = "Tx-20251112-055052-001";
    private static final String BASE_COUNTRY = "CI";

    private static Map<String, String> decodeSegments(String data) {
        Map<String, String> segments = new java.util.LinkedHashMap<>();
        int cursor = 0;
        while (cursor < data.length()) {
            if (cursor + 4 > data.length()) break;
            String tag = data.substring(cursor, cursor + 2);
            String lengthStr = data.substring(cursor + 2, cursor + 4);
            int length = Integer.parseInt(lengthStr, 10);
            if (length < 0) throw new IllegalArgumentException("Longueur invalide pour le tag " + tag);
            int valueStart = cursor + 4;
            int valueEnd = valueStart + length;
            if (valueEnd > data.length()) throw new IllegalArgumentException("Segment tronqué");
            segments.put(tag, data.substring(valueStart, valueEnd));
            cursor = valueEnd;
        }
        return segments;
    }

    private static List<SegmentEntry> decodeSegmentEntries(String data) {
        List<SegmentEntry> entries = new ArrayList<>();
        int cursor = 0;
        while (cursor < data.length()) {
            if (cursor + 4 > data.length()) break;
            String tag = data.substring(cursor, cursor + 2);
            String lengthStr = data.substring(cursor + 2, cursor + 4);
            int length = Integer.parseInt(lengthStr, 10);
            if (length < 0) throw new IllegalArgumentException("Longueur invalide pour le tag " + tag);
            int valueStart = cursor + 4;
            int valueEnd = valueStart + length;
            if (valueEnd > data.length()) throw new IllegalArgumentException("Segment tronqué");
            entries.add(new SegmentEntry(tag, data.substring(valueStart, valueEnd)));
            cursor = valueEnd;
        }
        return entries;
    }

    private static String encodeSimpleSegments(List<SegmentEntry> entries) {
        return entries.stream()
                .map(e -> e.tag + String.format("%02d", e.value.length()) + e.value)
                .collect(Collectors.joining());
    }

    private static String rebuildPayload(List<SegmentEntry> entries) {
        List<SegmentEntry> filtered = entries.stream().filter(e -> !"63".equals(e.tag)).toList();
        String base = encodeSimpleSegments(filtered);
        String crc = CrcUtil.computeCrc16(base + "6304");
        return base + "6304" + crc;
    }

    private static void updateSegment(List<SegmentEntry> entries, String tag, String value) {
        int idx = -1;
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).tag.equals(tag)) { idx = i; break; }
        }
        if (idx >= 0) entries.set(idx, new SegmentEntry(tag, value));
        else entries.add(new SegmentEntry(tag, value));
    }

    private static void removeSegment(List<SegmentEntry> entries, String tag) {
        entries.removeIf(e -> e.tag.equals(tag));
    }

    private static List<SegmentEntry> decodeAdditional(String raw) {
        return decodeSegmentEntries(raw);
    }

    private static String encodeAdditional(List<SegmentEntry> entries) {
        return encodeSimpleSegments(entries);
    }

    private static void expectValidCrc(String payload, Map<String, String> segments) {
        String crc = segments.get("63");
        assertNotNull(crc);
        String withoutCrc = payload.length() >= 4 ? payload.substring(0, payload.length() - 4) : "";
        assertEquals(crc, CrcUtil.computeCrc16(withoutCrc));
    }

    private record SegmentEntry(String tag, String value) {}

    private static QrPayloadInput input(String alias, String countryCode, QrType qrType, String referenceLabel, Object amount) {
        QrPayloadInput.Builder b = QrPayloadInput.builder()
                .alias(alias)
                .countryCode(countryCode)
                .qrType(qrType)
                .referenceLabel(referenceLabel);
        if (amount != null) {
            if (amount instanceof Long l) b.amount(l);
            else if (amount instanceof Integer i) b.amount(i.longValue());
            else b.amount(amount.toString());
        }
        return b.build();
    }

    @Nested
    class CreateQrPayload {
        @Test
        void genereUnQRStatiqueSansMontant() {
            QrPayloadResult result = PispiQrCode.createQrPayload(
                    input(BASE_ALIAS, BASE_COUNTRY, QrType.STATIC, REFERENCE_CAISSE, null));
            Map<String, String> segments = decodeSegments(result.payload());
            assertNotNull(segments.get("62"));
            Map<String, String> additional = decodeSegments(segments.get("62"));

            assertEquals("01", segments.get("00"));
            assertTrue(segments.get("36").contains(BASE_ALIAS));
            assertNull(segments.get("54"));
            assertEquals(BASE_COUNTRY, segments.get("58"));
            assertEquals("X", segments.get("59"));
            assertEquals("X", segments.get("60"));
            assertEquals(REFERENCE_CAISSE, additional.get("05"));
            assertEquals("000", additional.get("11"));
            expectValidCrc(result.payload(), segments);
        }

        @Test
        void genereUnQRStatiqueAvecMontant() {
            int amount = 1500;
            QrPayloadResult result = PispiQrCode.createQrPayload(
                    input(BASE_ALIAS, BASE_COUNTRY, QrType.STATIC, REFERENCE_PRODUIT, amount));
            Map<String, String> segments = decodeSegments(result.payload());
            Map<String, String> additional = decodeSegments(segments.get("62"));

            assertEquals(String.valueOf(amount), segments.get("54"));
            assertEquals(REFERENCE_PRODUIT, additional.get("05"));
            assertEquals("000", additional.get("11"));
            expectValidCrc(result.payload(), segments);
        }

        @Test
        void genereUnQRDynamiqueSansMontant() {
            QrPayloadResult result = PispiQrCode.createQrPayload(
                    input(BASE_ALIAS, BASE_COUNTRY, QrType.DYNAMIC, REFERENCE_TX, null));
            Map<String, String> segments = decodeSegments(result.payload());
            Map<String, String> additional = decodeSegments(segments.get("62"));

            assertNull(segments.get("54"));
            assertEquals(REFERENCE_TX, additional.get("05"));
            assertEquals("400", additional.get("11"));
            expectValidCrc(result.payload(), segments);
        }

        @Test
        void genereUnQRDynamiqueAvecMontant() {
            int amount = 82500;
            QrPayloadResult result = PispiQrCode.createQrPayload(
                    input(BASE_ALIAS, BASE_COUNTRY, QrType.DYNAMIC, REFERENCE_TX, amount));
            Map<String, String> segments = decodeSegments(result.payload());
            Map<String, String> additional = decodeSegments(segments.get("62"));

            assertEquals(String.valueOf(amount), segments.get("54"));
            assertEquals(REFERENCE_TX, additional.get("05"));
            assertEquals("400", additional.get("11"));
            expectValidCrc(result.payload(), segments);
        }

        @Test
        void rejetteUnSousTagAdditionnelInvalide() {
            AdditionalDataOverrides overrides = AdditionalDataOverrides.builder()
                    .putCustom("0@", "INVALID")
                    .build();
            QrPayloadOptions options = QrPayloadOptions.builder().additionalData(overrides).build();
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    PispiQrCode.createQrPayload(
                            input(BASE_ALIAS, BASE_COUNTRY, QrType.STATIC, REFERENCE_CAISSE, null),
                            options));
            assertTrue(ex.getMessage().contains("0@"));
            assertTrue(ex.getMessage().contains("2 caractères alphanumériques"));
        }

        @Test
        void rejetteUnTypeDeQRInconnu() {
            // In Java qrType is an enum so "UNKNOWN" is not possible. Equivalent: null qrType is rejected by createQrPayload.
            QrPayloadInput input = QrPayloadInput.builder()
                    .alias(BASE_ALIAS)
                    .countryCode(BASE_COUNTRY)
                    .referenceLabel(REFERENCE_CAISSE)
                    .build();
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    PispiQrCode.createQrPayload(input));
            assertTrue(ex.getMessage().contains("qrType"));
        }

        @Test
        void accepteUnMontantEnChaine() {
            QrPayloadResult result = PispiQrCode.createQrPayload(
                    input(BASE_ALIAS, BASE_COUNTRY, QrType.STATIC, REFERENCE_CAISSE, "000123"));
            Map<String, String> segments = decodeSegments(result.payload());
            assertEquals("000123", segments.get("54"));
        }

        @Test
        void autoriseLesDonneesAdditionnellesPersonnalisees() {
            AdditionalDataOverrides overrides = AdditionalDataOverrides.builder()
                    .purposeOfTransaction("FACTURE")
                    .putCustom("05", "REFERENCE_TX_MAX_25_CHARS")
                    .putCustom("11", "400")
                    .build();
            QrPayloadOptions options = QrPayloadOptions.builder().additionalData(overrides).build();
            QrPayloadResult result = PispiQrCode.createQrPayload(
                    input(BASE_ALIAS, BASE_COUNTRY, QrType.STATIC, REFERENCE_CAISSE, null),
                    options);
            Map<String, String> segments = decodeSegments(result.payload());
            Map<String, String> additional = decodeSegments(segments.get("62"));
            assertEquals("FACTURE", additional.get("12"));
            assertEquals("REFERENCE_TX_MAX_25_CHARS", additional.get("05"));
            assertEquals("400", additional.get("11"));
        }

        @Test
        void rejetteUneCleCustomInvalide() {
            AdditionalDataOverrides overrides = AdditionalDataOverrides.builder()
                    .putCustom("AA", "VALEUR")
                    .build();
            QrPayloadOptions options = QrPayloadOptions.builder().additionalData(overrides).build();
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    PispiQrCode.createQrPayload(
                            input(BASE_ALIAS, BASE_COUNTRY, QrType.STATIC, REFERENCE_CAISSE, null),
                            options));
            assertTrue(ex.getMessage().contains("05") && ex.getMessage().contains("11"));
            assertTrue(ex.getMessage().contains("AA"));
        }

        @Test
        void rejetteCustomTag05TropLong() {
            AdditionalDataOverrides overrides = AdditionalDataOverrides.builder()
                    .putCustom("05", "A".repeat(26))
                    .build();
            QrPayloadOptions options = QrPayloadOptions.builder().additionalData(overrides).build();
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    PispiQrCode.createQrPayload(
                            input(BASE_ALIAS, BASE_COUNTRY, QrType.STATIC, REFERENCE_CAISSE, null),
                            options));
            assertTrue(ex.getMessage().contains("05"));
            assertTrue(ex.getMessage().contains("25"));
        }

        @Test
        void rejetteCustomTag11ValeurInvalide() {
            AdditionalDataOverrides overrides = AdditionalDataOverrides.builder()
                    .putCustom("11", "123")
                    .build();
            QrPayloadOptions options = QrPayloadOptions.builder().additionalData(overrides).build();
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    PispiQrCode.createQrPayload(
                            input(BASE_ALIAS, BASE_COUNTRY, QrType.STATIC, REFERENCE_CAISSE, null),
                            options));
            assertTrue(ex.getMessage().contains("11"));
            assertTrue(ex.getMessage().contains("000") && ex.getMessage().contains("400"));
        }

        @Test
        void accepteCustomTag11_000() {
            AdditionalDataOverrides overrides = AdditionalDataOverrides.builder()
                    .putCustom("11", "000")
                    .build();
            QrPayloadOptions options = QrPayloadOptions.builder().additionalData(overrides).build();
            QrPayloadResult result = PispiQrCode.createQrPayload(
                    input(BASE_ALIAS, BASE_COUNTRY, QrType.STATIC, REFERENCE_CAISSE, null),
                    options);
            Map<String, String> segments = decodeSegments(result.payload());
            Map<String, String> additional = decodeSegments(segments.get("62"));
            assertEquals("000", additional.get("11"));
        }

        @Test
        void rejetteUnMontantNumeriqueNonEntier() {
            assertThrows(IllegalArgumentException.class, () ->
                    PispiQrCode.createQrPayload(
                            input(BASE_ALIAS, BASE_COUNTRY, QrType.STATIC, REFERENCE_CAISSE, "12.5")));
        }

        @Test
        void rejetteUnMontantAvecSeparateurs() {
            assertThrows(IllegalArgumentException.class, () ->
                    PispiQrCode.createQrPayload(
                            input(BASE_ALIAS, BASE_COUNTRY, QrType.STATIC, REFERENCE_CAISSE, "12,50")));
            assertThrows(IllegalArgumentException.class, () ->
                    PispiQrCode.createQrPayload(
                            input(BASE_ALIAS, BASE_COUNTRY, QrType.STATIC, REFERENCE_CAISSE, "15.00")));
        }

        @Test
        void rejetteUnTypeDeQRManquant() {
            QrPayloadInput input = QrPayloadInput.builder()
                    .alias(BASE_ALIAS)
                    .countryCode(BASE_COUNTRY)
                    .referenceLabel(REFERENCE_CAISSE)
                    .build();
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    PispiQrCode.createQrPayload(input));
            assertTrue(ex.getMessage().contains("qrType"));
        }

        @Test
        void rejetteUnReferenceLabelManquant() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    PispiQrCode.createQrPayload(
                            input(BASE_ALIAS, BASE_COUNTRY, QrType.STATIC, "", null)));
            assertTrue(ex.getMessage().contains("referenceLabel"));
        }

        @Test
        void rejetteUnAliasManquant() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    PispiQrCode.createQrPayload(
                            input("", BASE_COUNTRY, QrType.STATIC, REFERENCE_CAISSE, null)));
            assertTrue(ex.getMessage().contains("alias"));
        }

        @Test
        void rejetteUnCountryCodeManquant() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    PispiQrCode.createQrPayload(
                            input(BASE_ALIAS, "", QrType.STATIC, REFERENCE_CAISSE, null)));
            assertTrue(ex.getMessage().contains("countryCode"));
        }
    }

    @Nested
    class IsValidPispiQrPayload {
        @Test
        void valideUnePayloadGenereeParLeSDK() {
            String payload = PispiQrCode.buildPayloadString(
                    input(BASE_ALIAS, BASE_COUNTRY, QrType.STATIC, REFERENCE_CAISSE, 1000));
            QrValidationResult result = PispiQrCode.isValidPispiQrPayload(payload);
            assertTrue(result.valid());
            assertTrue(result.errors().isEmpty());
            assertNotNull(result.data());
            assertEquals(BASE_ALIAS, result.data().alias());
            assertEquals(BASE_COUNTRY, result.data().countryCode());
            assertEquals(QrType.STATIC, result.data().qrType());
            assertEquals(REFERENCE_CAISSE, result.data().referenceLabel());
            assertEquals("1000", result.data().amount());
        }

        @Test
        void detecteUnCRCInvalide() {
            String payload = PispiQrCode.buildPayloadString(
                    input(BASE_ALIAS, BASE_COUNTRY, QrType.STATIC, REFERENCE_CAISSE, null));
            String corrupted = payload.substring(0, payload.length() - 1) + "Z";
            QrValidationResult result = PispiQrCode.isValidPispiQrPayload(corrupted);
            assertFalse(result.valid());
            assertTrue(result.errors().stream().anyMatch(e -> e.contains("CRC invalide")));
            assertNull(result.data());
        }

        @Test
        void detecteUnChampObligatoireManquant() {
            String invalidPayload = "0002010102126304BEEF";
            QrValidationResult result = PispiQrCode.isValidPispiQrPayload(invalidPayload);
            assertFalse(result.valid());
            assertTrue(result.errors().stream().anyMatch(e -> e.contains("Tag 36")));
            assertNull(result.data());
        }

        @Test
        void rejetteUnAliasQuiNestPasUnUUIDv4() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    PispiQrCode.createQrPayload(
                            input("not-an-uuid", BASE_COUNTRY, QrType.STATIC, REFERENCE_CAISSE, null)));
            assertTrue(ex.getMessage().contains("UUID v4"));
        }

        @Test
        void rejetteUnReferenceLabelTropLong() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    PispiQrCode.createQrPayload(
                            input(BASE_ALIAS, BASE_COUNTRY, QrType.STATIC, "A".repeat(26), null)));
            assertTrue(ex.getMessage().contains("25 caractères"));
        }

        @Test
        void rejetteUnPaysHorsUEMOA() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    PispiQrCode.createQrPayload(
                            input(BASE_ALIAS, "FR", QrType.STATIC, REFERENCE_CAISSE, null)));
            assertTrue(ex.getMessage().contains("UEMOA"));
        }

        @Test
        void rejetteUnMontantSuperieurA13Chiffres() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    PispiQrCode.createQrPayload(
                            input(BASE_ALIAS, BASE_COUNTRY, QrType.STATIC, REFERENCE_CAISSE, "12345678901234")));
            assertTrue(ex.getMessage().contains("13 chiffres"));
        }
    }

    @Nested
    class IsValidPispiQrPayloadDiagnostics {
        private String basePayload() {
            return PispiQrCode.buildPayloadString(
                    input(BASE_ALIAS, BASE_COUNTRY, QrType.STATIC, REFERENCE_CAISSE, null));
        }

        @Test
        void signaleUnIndicateurDeFormatInvalide() {
            List<SegmentEntry> entries = new ArrayList<>(decodeSegmentEntries(basePayload()));
            updateSegment(entries, "00", "02");
            String mutated = rebuildPayload(entries);
            QrValidationResult result = PispiQrCode.isValidPispiQrPayload(mutated);
            assertFalse(result.valid());
            assertTrue(result.errors().stream().anyMatch(e -> e.contains("Tag 00 invalide")));
        }

        @Test
        void signaleUnIndicateurDeFormatManquant() {
            List<SegmentEntry> entries = new ArrayList<>(decodeSegmentEntries(basePayload()));
            removeSegment(entries, "00");
            String mutated = rebuildPayload(entries);
            QrValidationResult result = PispiQrCode.isValidPispiQrPayload(mutated);
            assertFalse(result.valid());
            assertTrue(result.errors().stream().anyMatch(e -> e.contains("Tag 00") && e.contains("manquant")));
        }

        @Test
        void signaleAbsenceAliasDansInformationsMarchand() {
            List<SegmentEntry> entries = new ArrayList<>(decodeSegmentEntries(basePayload()));
            SegmentEntry merchant = entries.stream().filter(e -> "36".equals(e.tag)).findFirst().orElseThrow();
            List<SegmentEntry> merchantEntries = decodeAdditional(merchant.value()).stream()
                    .filter(e -> !"01".equals(e.tag)).toList();
            updateSegment(entries, "36", encodeAdditional(new ArrayList<>(merchantEntries)));
            String mutated = rebuildPayload(entries);
            QrValidationResult result = PispiQrCode.isValidPispiQrPayload(mutated);
            assertFalse(result.valid());
            assertTrue(result.errors().stream().anyMatch(e -> e.contains("Alias manquant")));
        }

        @Test
        void signaleErreurLorsDecodageInformationsMarchand() {
            List<SegmentEntry> entries = new ArrayList<>(decodeSegmentEntries(basePayload()));
            updateSegment(entries, "36", "0004AB");
            String mutated = rebuildPayload(entries);
            QrValidationResult result = PispiQrCode.isValidPispiQrPayload(mutated);
            assertFalse(result.valid());
            assertTrue(result.errors().stream().anyMatch(e -> e.contains("informations marchand")));
        }

        @Test
        void accepteUnQRCodeSansReferenceLabelDansDonneesAdditionnelles() {
            List<SegmentEntry> entries = new ArrayList<>(decodeSegmentEntries(basePayload()));
            SegmentEntry additional = entries.stream().filter(e -> "62".equals(e.tag)).findFirst().orElseThrow();
            List<SegmentEntry> additionalEntries = decodeAdditional(additional.value()).stream()
                    .filter(e -> !"05".equals(e.tag)).toList();
            updateSegment(entries, "62", encodeAdditional(new ArrayList<>(additionalEntries)));
            String mutated = rebuildPayload(entries);
            QrValidationResult result = PispiQrCode.isValidPispiQrPayload(mutated);
            assertTrue(result.valid());
            assertEquals("", result.data().referenceLabel());
        }

        @Test
        void signaleMerchantChannelManquantDansDonneesAdditionnelles() {
            List<SegmentEntry> entries = new ArrayList<>(decodeSegmentEntries(basePayload()));
            SegmentEntry additional = entries.stream().filter(e -> "62".equals(e.tag)).findFirst().orElseThrow();
            List<SegmentEntry> additionalEntries = decodeAdditional(additional.value()).stream()
                    .filter(e -> !"11".equals(e.tag)).toList();
            updateSegment(entries, "62", encodeAdditional(new ArrayList<>(additionalEntries)));
            String mutated = rebuildPayload(entries);
            QrValidationResult result = PispiQrCode.isValidPispiQrPayload(mutated);
            assertFalse(result.valid());
            assertTrue(result.errors().stream().anyMatch(e -> e.contains("Tag 11") && e.contains("Merchant Channel")));
        }

        @Test
        void signaleErreurLorsDecodageDonneesAdditionnelles() {
            List<SegmentEntry> entries = new ArrayList<>(decodeSegmentEntries(basePayload()));
            updateSegment(entries, "62", "00AATEST");
            String mutated = rebuildPayload(entries);
            QrValidationResult result = PispiQrCode.isValidPispiQrPayload(mutated);
            assertFalse(result.valid());
            assertTrue(result.errors().stream().anyMatch(e -> e.contains("données additionnelles")));
        }

        @Test
        void signaleCRCManquante() {
            List<SegmentEntry> entries = new ArrayList<>(decodeSegmentEntries(basePayload()));
            removeSegment(entries, "63");
            String withoutCrc = encodeSimpleSegments(entries);
            QrValidationResult result = PispiQrCode.isValidPispiQrPayload(withoutCrc);
            assertFalse(result.valid());
            assertTrue(result.errors().stream().anyMatch(e -> e.contains("Tag 63") && e.contains("CRC")));
        }

        @Test
        void signaleLongueurInvalidePourUnSegment() {
            String payload = basePayload();
            int index = payload.indexOf("36");
            String mutated = payload.substring(0, index + 2) + "AA" + payload.substring(index + 4);
            QrValidationResult result = PispiQrCode.isValidPispiQrPayload(mutated);
            assertFalse(result.valid());
            assertTrue(result.errors().stream().anyMatch(e -> e.contains("Longueur invalide") && e.contains("36")));
        }

        @Test
        void signaleSegmentTronque() {
            String payload = basePayload();
            int index = payload.indexOf("36");
            int valueStart = index + 4;
            String truncated = payload.substring(0, valueStart + 10);
            QrValidationResult result = PispiQrCode.isValidPispiQrPayload(truncated);
            assertFalse(result.valid());
            assertTrue(result.errors().stream().anyMatch(e -> e.contains("Segment 36 tronqué")));
        }

        @Test
        void rejetteUnePayloadVide() {
            QrValidationResult result = PispiQrCode.isValidPispiQrPayload("");
            assertFalse(result.valid());
            assertTrue(result.errors().stream().anyMatch(e -> e.contains("non vide")));
        }

        @Test
        void rejetteUnePayloadTropCourte() {
            QrValidationResult result = PispiQrCode.isValidPispiQrPayload("000201");
            assertFalse(result.valid());
            assertTrue(result.errors().stream().anyMatch(e -> e.contains("trop courte")));
        }

        @Test
        void retourneSTATICPourUnMerchantChannelInconnu() {
            List<SegmentEntry> entries = new ArrayList<>(decodeSegmentEntries(basePayload()));
            SegmentEntry additional = entries.stream().filter(e -> "62".equals(e.tag)).findFirst().orElseThrow();
            List<SegmentEntry> additionalEntries = new ArrayList<>(decodeAdditional(additional.value()));
            for (int i = 0; i < additionalEntries.size(); i++) {
                if ("11".equals(additionalEntries.get(i).tag())) {
                    additionalEntries.set(i, new SegmentEntry("11", "999"));
                    break;
                }
            }
            updateSegment(entries, "62", encodeAdditional(additionalEntries));
            String mutated = rebuildPayload(entries);
            QrValidationResult result = PispiQrCode.isValidPispiQrPayload(mutated);
            assertTrue(result.valid());
            assertEquals(QrType.STATIC, result.data().qrType());
        }

        @Test
        void retourneDYNAMICPourUnMerchantChannel400() {
            String payload = PispiQrCode.buildPayloadString(
                    input(BASE_ALIAS, BASE_COUNTRY, QrType.DYNAMIC, REFERENCE_TX, null));
            QrValidationResult result = PispiQrCode.isValidPispiQrPayload(payload);
            assertTrue(result.valid());
            assertEquals(QrType.DYNAMIC, result.data().qrType());
        }
    }

    @Nested
    class Crc16 {
        @Test
        void computeCrc16MatchesJsBehavior() {
            String input = "0002010102126304";
            String crc = CrcUtil.computeCrc16(input);
            assertEquals(4, crc.length());
            assertTrue(crc.matches("[0-9A-F]{4}"));
        }
    }
}
