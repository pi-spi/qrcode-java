package bceao.pispi.qrcode;

/**
 * Options pour la génération de la payload (surcharge des données additionnelles).
 */
public final class QrPayloadOptions {

    private final AdditionalDataOverrides additionalData;

    private QrPayloadOptions(Builder b) {
        this.additionalData = b.additionalData;
    }

    public AdditionalDataOverrides additionalData() {
        return additionalData;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static QrPayloadOptions defaults() {
        return builder().build();
    }

    public static final class Builder {
        private AdditionalDataOverrides additionalData;

        public Builder additionalData(AdditionalDataOverrides additionalData) {
            this.additionalData = additionalData;
            return this;
        }

        public QrPayloadOptions build() {
            return new QrPayloadOptions(this);
        }
    }
}
