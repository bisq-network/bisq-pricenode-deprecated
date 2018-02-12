# Refactorings

The list of stuff remaining to complete the PR at https://github.com/bisq-network/pricenode/pull/7

 - Improve error handling: make sure failures at startup kill the JVM, and that failures on scheduled requests do not
 - Replace our own caching infrastructure with Spring's @Cacheable
 - Refactor bisq.price.app.Pricenode into individual Spring controllers
 - Replace our own Environment implementation with Spring's Environment
 - Replace use of bisq-core's HttpClient with Spring's RestTemplate
 - Finish refactoring 'mining' package, esp FeeEstimationService
 - Document provider implementations w/ links to API docs, etc
 
## Non-refactorings

Most or all of these will become individual issues / PRs. Just capturing them here for convenience now. Not all may make sense.

 - Deprecate existing get* endpoints (e.g. /getAllMarketPrices) in favor of '/exchange-rates', '/fee-estimate; 
 - Eliminate dependency on bisq-core (only real need now is CurrencyUtil for list of supported coins)
 - Remove command line args for fee estimation params; hard-code these values and update them via commits,
   not via one-off changes by each operator
 - Remove custom Version management in favor of Boot / Gradle version integration (incl. commit hash) exposed via Boot
   actuator endpoint
 - Remove 'getParams' in favor of Boot actuator endpoint
 - Update bisq-network/exchange to refer to 'provider' as 'pricenode'
 - Invert the dependency arrangement. Move 'ProviderRepository' et al from bisq-network/exchange here into
   bisq-network/pricenode and have bisq-network/exchange depend on it as a client lib
