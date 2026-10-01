package za.co.unilinkhub.shared.domain;

import za.co.unilinkhub.common.exception.BadRequestException;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * CPUT's campuses. Where a business hands over its goods matters more to a student than almost
 * anything else on a listing - a cupcake at Bellville is no use to someone in District Six - so
 * businesses record their campus, and browse can filter by it.
 */
public enum Campus {
    BELLVILLE("Bellville"),
    DISTRICT_SIX("District Six"),
    MOWBRAY("Mowbray"),
    WELLINGTON("Wellington"),
    GRANGER_BAY("Granger Bay");

    private final String label;

    Campus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Null or blank means "not given". Anything else must be one of the campuses. */
    public static Campus parseOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String wanted = value.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        return Arrays.stream(values()).filter(c -> c.name().equals(wanted)).findFirst()
                .orElseThrow(() -> new BadRequestException("\"" + value + "\" isn't a CPUT campus we know. Choose one of: "
                        + Arrays.stream(values()).map(Campus::label).collect(Collectors.joining(", ")) + "."));
    }
}
