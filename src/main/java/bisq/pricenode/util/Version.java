package bisq.pricenode.util;

import bisq.pricenode.app.Pricenode;

import java.util.Optional;

public class Version {

    public static final String NONE = "0.0.0";

    private final String value;

    public Version(Class<Pricenode> clazz) {
        this(clazz.getPackage());
    }

    public Version(Package pkg) {
        this(Optional.ofNullable(pkg.getImplementationVersion()).orElse(NONE));
    }

    public Version(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
