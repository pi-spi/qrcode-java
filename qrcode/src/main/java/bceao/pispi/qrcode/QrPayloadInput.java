package bceao.pispi.qrcode;

/**
 * Paramètres d'entrée pour générer une payload EMV PI-SPI.
 */
public final class QrPayloadInput {

    private final String alias;
    private final String countryCode;
    private final QrType qrType;
    private final String referenceLabel;
    private final String amount; // null ou vide = pas de montant

    private QrPayloadInput(Builder b) {
        this.alias = b.alias;
        this.countryCode = b.countryCode;
        this.qrType = b.qrType;
        this.referenceLabel = b.referenceLabel;
        this.amount = b.amount;
    }

    public String alias() {
        return alias;
    }

    public String countryCode() {
        return countryCode;
    }

    public QrType qrType() {
        return qrType;
    }

    public String referenceLabel() {
        return referenceLabel;
    }

    /**
     * Montant optionnel (sera normalisé au format EMV). null ou chaîne vide = pas de montant.
     */
    public String amount() {
        return amount;
    }

    public boolean hasAmount() {
        return amount != null && !amount.isEmpty();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String alias;
        private String countryCode;
        private QrType qrType;
        private String referenceLabel;
        private String amount;

        public Builder alias(String alias) {
            this.alias = alias;
            return this;
        }

        public Builder countryCode(String countryCode) {
            this.countryCode = countryCode;
            return this;
        }

        public Builder qrType(QrType qrType) {
            this.qrType = qrType;
            return this;
        }

        public Builder referenceLabel(String referenceLabel) {
            this.referenceLabel = referenceLabel;
            return this;
        }

        /**
         * Montant optionnel. Peut être un nombre ou une chaîne de chiffres.
         */
        public Builder amount(long amount) {
            this.amount = String.valueOf(amount);
            return this;
        }

        public Builder amount(String amount) {
            this.amount = amount;
            return this;
        }

        public QrPayloadInput build() {
            return new QrPayloadInput(this);
        }
    }
}
