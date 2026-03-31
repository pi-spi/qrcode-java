package bceao.pispi.qrcode;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Surcharges des données additionnelles (template 62).
 * Données personnalisées : seules les clés {@code "05"} et {@code "11"} sont autorisées.
 * <ul>
 *   <li>{@code "05"} : référence transaction, max 25 caractères</li>
 *   <li>{@code "11"} : canal marchand, longueur 3, valeurs autorisées {@code "000"} ou {@code "400"}</li>
 * </ul>
 */
public final class AdditionalDataOverrides {

    private final String merchantChannel;
    private final Map<String, String> custom;

    private AdditionalDataOverrides(Builder b) {
        this.merchantChannel = b.merchantChannel;
        this.custom = b.custom == null ? Map.of() : Collections.unmodifiableMap(new TreeMap<>(b.custom));
    }

    public String merchantChannel() {
        return merchantChannel;
    }

    public Map<String, String> custom() {
        return custom;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String merchantChannel;
        private Map<String, String> custom;

        public Builder merchantChannel(String merchantChannel) {
            this.merchantChannel = merchantChannel;
            return this;
        }

        public Builder custom(Map<String, String> custom) {
            this.custom = custom;
            return this;
        }

        public Builder putCustom(String tag, String value) {
            if (this.custom == null) {
                this.custom = new TreeMap<>();
            }
            this.custom.put(tag, value);
            return this;
        }

        public AdditionalDataOverrides build() {
            return new AdditionalDataOverrides(this);
        }
    }
}
