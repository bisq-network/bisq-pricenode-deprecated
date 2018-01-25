package bisq.pricenode.app;

import java.util.Optional;

class Environment {

    public String getRequiredVar(String name) {
        return Optional.ofNullable(System.getenv(name))
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Error: required environment variable '%s' not found.", name)));
    }
}
