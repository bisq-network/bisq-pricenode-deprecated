package bisq.pricenode.util;

import java.util.Optional;
import java.util.function.Consumer;

public class Environment {

    public String getRequiredVar(String name) {
        return Optional.ofNullable(System.getenv(name))
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Error: required environment variable '%s' not found.", name)));
    }

    public void doWithOptionalVar(String name, Consumer<String> action) {
        Optional.ofNullable(System.getenv(name))
                .ifPresent(action);
    }
}
