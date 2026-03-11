package bceao.pispi.qrcode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Point d'entrée principal du SDK PI-SPI QR Code.
 * Génère et valide les payloads EMV conformes PI-SPI (pas de génération d'image).
 */
public final class PispiQrCode {

    private static final String DEFAULT_MERCHANT_CATEGORY_CODE = "0000";
    private static final String DEFAULT_CURRENCY = "952"; // XOF
    private static final String DEFAULT_MERCHANT_NAME = "X";
    private static final String DEFAULT_MERCHANT_CITY = "X";
    private static final String DEFAULT_REFERENCE_LABEL_TAG = "05";
    private static final String DEFAULT_MERCHANT_CHANNEL_TAG = "11";

    private static final Set<String> UEMOA_COUNTRIES = Set.of("BJ", "BF", "CI", "ML", "NE", "SN", "TG", "GW");
    private static final Pattern UUID_V4 = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SUB_TAG = Pattern.compile("^[0-9A-Za-z]{2}$");
    private static final Pattern DIGITS_ONLY = Pattern.compile("^\\d+$");

    private PispiQrCode() {}

    /**
     * Génère la payload EMV conforme PI-SPI pour un QR Code.
     */
    public static QrPayloadResult createQrPayload(QrPayloadInput input) {
        return createQrPayload(input, QrPayloadOptions.defaults());
    }

    /**
     * Génère la payload EMV conforme PI-SPI pour un QR Code avec options.
     */
    public static QrPayloadResult createQrPayload(QrPayloadInput input, QrPayloadOptions options) {
        if (options == null) {
            options = QrPayloadOptions.defaults();
        }

        String alias = input.alias();
        String countryCode = input.countryCode();
        QrType qrType = input.qrType();
        String referenceLabel = input.referenceLabel();
        String amount = input.amount();

        if (alias == null || alias.isEmpty()) {
            throw new IllegalArgumentException("Le paramètre \"alias\" est obligatoire.");
        }
        validateAlias(alias);
        if (countryCode == null || countryCode.isEmpty()) {
            throw new IllegalArgumentException("Le paramètre \"countryCode\" est obligatoire.");
        }
        validateCountryCode(countryCode);
        if (qrType == null) {
            throw new IllegalArgumentException("Le paramètre \"qrType\" est obligatoire.");
        }
        if (referenceLabel == null || referenceLabel.isEmpty()) {
            throw new IllegalArgumentException("Le paramètre \"referenceLabel\" est obligatoire.");
        }
        validateReferenceLabel(referenceLabel);
        if (amount != null && !amount.isEmpty()) {
            validateAmount(amount);
        }

        List<String> payloadSegments = new ArrayList<>();

        payloadSegments.add(EmvCodec.formatDataObject("00", "01"));

        String merchantAccountInformation = EmvCodec.formatDataObject("00", "int.bceao.pi")
                + EmvCodec.formatDataObject("01", alias);
        payloadSegments.add(EmvCodec.formatDataObject("36", merchantAccountInformation));
        payloadSegments.add(EmvCodec.formatDataObject("52", DEFAULT_MERCHANT_CATEGORY_CODE));
        payloadSegments.add(EmvCodec.formatDataObject("53", DEFAULT_CURRENCY));

        if (amount != null && !amount.isEmpty()) {
            payloadSegments.add(EmvCodec.formatDataObject("54", sanitizeAmount(amount)));
        }

        payloadSegments.add(EmvCodec.formatDataObject("58", countryCode));
        payloadSegments.add(EmvCodec.formatDataObject("59", DEFAULT_MERCHANT_NAME));
        payloadSegments.add(EmvCodec.formatDataObject("60", DEFAULT_MERCHANT_CITY));

        QrType normalizedQrType = normalizeQrType(qrType.name());
        String additionalData = buildAdditionalData(normalizedQrType, referenceLabel, options.additionalData());
        if (additionalData != null && !additionalData.isEmpty()) {
            payloadSegments.add(EmvCodec.formatDataObject("62", additionalData));
        }

        String payloadWithoutCrc = String.join("", payloadSegments);
        String crcInput = payloadWithoutCrc + "6304";
        String crc = CrcUtil.computeCrc16(crcInput);

        return new QrPayloadResult(payloadWithoutCrc + "6304" + crc);
    }

    /**
     * Raccourci : retourne directement la chaîne payload.
     */
    public static String buildPayloadString(QrPayloadInput input) {
        return buildPayloadString(input, QrPayloadOptions.defaults());
    }

    public static String buildPayloadString(QrPayloadInput input, QrPayloadOptions options) {
        return createQrPayload(input, options).payload();
    }

    /**
     * Valide une payload PI-SPI et extrait les données si elle est valide.
     */
    public static QrValidationResult isValidPispiQrPayload(String value) {
        List<String> basicErrors = validatePayloadBasics(value);
        if (!basicErrors.isEmpty()) {
            return QrValidationResult.invalid(basicErrors);
        }

        EmvCodec.ParseResult parsed = EmvCodec.parseSegments(value);
        if (!parsed.errors().isEmpty()) {
            return QrValidationResult.invalid(parsed.errors());
        }

        SegmentValidation segmentValidation = validateSegmentContent(parsed.segments(), value);
        if (!segmentValidation.errors().isEmpty()) {
            return QrValidationResult.invalid(segmentValidation.errors());
        }

        QrPayloadInput data = buildValidationData(parsed.segments(), segmentValidation);
        return QrValidationResult.valid(data);
    }

    // --- Helpers payload ---

    private static String buildAdditionalData(QrType qrType, String referenceLabel, AdditionalDataOverrides overrides) {
        StringBuilder sb = new StringBuilder();
        sb.append(EmvCodec.formatDataObject(DEFAULT_REFERENCE_LABEL_TAG, referenceLabel));
        String merchantChannelValue = qrType == QrType.DYNAMIC ? "400" : "000";
        sb.append(EmvCodec.formatDataObject(DEFAULT_MERCHANT_CHANNEL_TAG, merchantChannelValue));

        if (overrides != null) {
            if (overrides.purposeOfTransaction() != null && !overrides.purposeOfTransaction().isEmpty()) {
                sb.append(EmvCodec.formatDataObject("12", overrides.purposeOfTransaction()));
            }
            if (overrides.custom() != null && !overrides.custom().isEmpty()) {
                for (Map.Entry<String, String> e : new java.util.TreeMap<>(overrides.custom()).entrySet()) {
                    validateSubTag(e.getKey());
                    sb.append(EmvCodec.formatDataObject(e.getKey(), e.getValue()));
                }
            }
        }
        return sb.toString();
    }

    private static String sanitizeAmount(String value) {
        return value.trim();
    }

    private static void validateSubTag(String tag) {
        if (tag == null || !SUB_TAG.matcher(tag).matches()) {
            throw new IllegalArgumentException(
                    "Le sous-tag additional data \"" + tag + "\" doit contenir exactement 2 caractères alphanumériques.");
        }
    }

    private static void validateAlias(String alias) {
        if (!UUID_V4.matcher(alias).matches()) {
            throw new IllegalArgumentException("L'alias doit être un UUID v4 valide.");
        }
    }

    private static void validateReferenceLabel(String reference) {
        if (reference.length() > 25) {
            throw new IllegalArgumentException("Le referenceLabel ne doit pas dépasser 25 caractères.");
        }
    }

    private static QrType normalizeQrType(String type) {
        String normalized = type.trim().toUpperCase();
        if ("STATIC".equals(normalized)) return QrType.STATIC;
        if ("DYNAMIC".equals(normalized)) return QrType.DYNAMIC;
        throw new IllegalArgumentException("Le paramètre \"qrType\" doit être \"STATIC\" ou \"DYNAMIC\".");
    }

    private static void validateCountryCode(String code) {
        if (!UEMOA_COUNTRIES.contains(code.toUpperCase())) {
            throw new IllegalArgumentException(
                    "Le countryCode doit être l'un des codes ISO2 de l'UEMOA (BJ, BF, CI, ML, NE, SN, TG, GW).");
        }
    }

    private static void validateAmount(String amount) {
        String normalized = amount.trim();
        if (!DIGITS_ONLY.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Le montant doit contenir uniquement des chiffres.");
        }
        if (normalized.length() > 13) {
            throw new IllegalArgumentException("Le montant ne doit pas dépasser 13 chiffres.");
        }
    }

    // --- Validation payload ---

    private static List<String> validatePayloadBasics(String value) {
        List<String> errors = new ArrayList<>();
        if (value == null || value.isEmpty()) {
            errors.add("La payload doit être une chaîne non vide.");
        } else if (value.length() < 12) {
            errors.add("Payload trop courte pour contenir des segments EMV.");
        }
        return errors;
    }

    private static String mapQrTypeFromChannel(String channel) {
        if (channel != null && "400".equals(channel.trim().toUpperCase())) {
            return "DYNAMIC";
        }
        return "STATIC";
    }

    private record SegmentValidation(
            List<String> errors,
            Map<String, String> merchantInfo,
            String referenceLabel,
            String merchantChannel,
            String countryCode) {}

    private static SegmentValidation validateSegmentContent(Map<String, String> segments, String rawValue) {
        List<String> errors = new ArrayList<>();

        List<String> formatErrors = validateFormatIndicator(segments.get("00"));
        var merchantResult = extractMerchantInfo(segments.get("36"));
        var countryResult = validateCountryCodeSegment(segments.get("58"));
        var additionalResult = extractAdditionalData(segments.get("62"));
        List<String> crcErrors = validateCrcSegment(segments.get("63"), rawValue);

        errors.addAll(formatErrors);
        errors.addAll(merchantResult.errors());
        errors.addAll(countryResult.errors());
        errors.addAll(additionalResult.errors());
        errors.addAll(crcErrors);

        return new SegmentValidation(
                errors,
                merchantResult.merchantInfo(),
                additionalResult.referenceLabel(),
                additionalResult.merchantChannel(),
                countryResult.countryCode());
    }

    private static List<String> validateFormatIndicator(String formatIndicator) {
        List<String> errors = new ArrayList<>();
        if (formatIndicator == null || formatIndicator.isEmpty()) {
            errors.add("Tag 00 (format indicator) manquant.");
        } else if (!"01".equals(formatIndicator)) {
            errors.add("Tag 00 invalide (doit être 01).");
        }
        return errors;
    }

    private record MerchantResult(Map<String, String> merchantInfo, List<String> errors) {}

    private static MerchantResult extractMerchantInfo(String segment) {
        if (segment == null || segment.isEmpty()) {
            return new MerchantResult(null, List.of("Tag 36 (Merchant Account Information) manquant."));
        }
        try {
            Map<String, String> merchantInfo = EmvCodec.parseSubFields(segment);
            List<String> errors = new ArrayList<>();
            if (merchantInfo.get("01") == null || merchantInfo.get("01").isEmpty()) {
                errors.add("Alias manquant dans les informations marchand (tag 36).");
            }
            return new MerchantResult(merchantInfo, errors);
        } catch (Exception e) {
            return new MerchantResult(null,
                    List.of("Erreur lors de l'analyse des informations marchand: " + (e.getMessage() != null ? e.getMessage() : e.toString())));
        }
    }

    private record CountryResult(String countryCode, List<String> errors) {}

    private static CountryResult validateCountryCodeSegment(String countryCode) {
        if (countryCode == null || countryCode.isEmpty()) {
            return new CountryResult(null, List.of("Tag 58 (Country Code) manquant."));
        }
        return new CountryResult(countryCode, List.of());
    }

    private record AdditionalResult(String referenceLabel, String merchantChannel, List<String> errors) {}

    private static AdditionalResult extractAdditionalData(String segment) {
        if (segment == null || segment.isEmpty()) {
            return new AdditionalResult(null, null, List.of("Tag 62 (Additional Data Field) manquant."));
        }
        try {
            Map<String, String> additionalSegments = EmvCodec.parseSubFields(segment);
            String referenceLabel = additionalSegments.get("05");
            String merchantChannel = additionalSegments.get(DEFAULT_MERCHANT_CHANNEL_TAG);
            List<String> errors = new ArrayList<>();
            if (merchantChannel == null || merchantChannel.isEmpty()) {
                errors.add("Tag 11 (Merchant Channel) manquant dans les données additionnelles.");
            }
            return new AdditionalResult(referenceLabel, merchantChannel, errors);
        } catch (Exception e) {
            return new AdditionalResult(null, null,
                    List.of("Erreur lors de l'analyse des données additionnelles: " + (e.getMessage() != null ? e.getMessage() : e.toString())));
        }
    }

    private static List<String> validateCrcSegment(String crc, String rawValue) {
        List<String> errors = new ArrayList<>();
        if (crc == null || crc.isEmpty()) {
            errors.add("Tag 63 (CRC) manquant.");
            return errors;
        }
        String payloadWithoutCrc = rawValue.length() >= 4 ? rawValue.substring(0, rawValue.length() - 4) : "";
        String computedCrc = CrcUtil.computeCrc16(payloadWithoutCrc);
        if (!crc.equals(computedCrc)) {
            errors.add("CRC invalide.");
        }
        return errors;
    }

    private static QrPayloadInput buildValidationData(Map<String, String> segments, SegmentValidation context) {
        String alias = context.merchantInfo() != null ? context.merchantInfo().get("01") : null;
        String countryCode = context.countryCode();
        String qrTypeStr = mapQrTypeFromChannel(context.merchantChannel());
        String referenceLabel = context.referenceLabel() != null ? context.referenceLabel() : "";

        QrPayloadInput.Builder b = QrPayloadInput.builder()
                .alias(alias != null ? alias : "")
                .countryCode(countryCode != null ? countryCode : "")
                .qrType("DYNAMIC".equals(qrTypeStr) ? QrType.DYNAMIC : QrType.STATIC)
                .referenceLabel(referenceLabel);

        String amountValue = segments.get("54");
        if (amountValue != null && !amountValue.isEmpty()) {
            b.amount(amountValue);
        }
        return b.build();
    }
}
