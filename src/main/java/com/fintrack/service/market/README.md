# Market Data Services Architecture

This package contains services for handling market data operations across different asset types (stocks, forex, crypto, and commodities).

## Architecture Overview

The market data services are structured using a combination of interfaces, abstract classes, and implementation classes to promote code reuse, maintainability, and separation of concerns.

### Core Interfaces

1. **MarketDataProvider** - Base interface for all services that provide market data.
   - Defines common methods for requesting market data updates.
   - Provides default implementations for convenience methods.

2. **KafkaMarketDataProvider** - Interface for market data providers that use Kafka.
   - Extends MarketDataProvider.
   - Adds Kafka-specific methods for sending messages and handling responses.

3. **WatchlistDataOperations** - Interface for watchlist-specific operations.
   - Defines methods for managing watchlist items.
   - Includes methods for requesting market data updates for watchlist items.

### Abstract Base Classes

1. **AbstractMarketDataProvider** - Base implementation of KafkaMarketDataProvider.
   - Provides common functionality for market data providers.
   - Includes error handling, logging, and Kafka message formatting.

2. **AssetMarketDataProviderBase** - Extension of AbstractMarketDataProvider for asset-specific services.
   - Adds functionality specific to asset market data handling.
   - Includes methods for processing symbols, fetching data with retry, and handling historical data.

### Service Implementations

1. **Asset-specific Services**:
   - **StockMarketDataService** - Handles stock market data.
   - **ForexMarketDataService** - Handles forex market data with currency conversion logic.
   - **CryptoMarketDataService** - Handles cryptocurrency market data.
   - **CommodityMarketDataService** - Handles commodity market data.

2. **Coordinator Services**:
   - **MarketDataService** - Coordinates market data operations across all asset types.
   - **MarketIndexDataService** - Handles market index data.
   - **WatchlistDataService** - Manages watchlist operations and triggers market data updates.

## Class Hierarchy

```
MarketDataProvider (interface)
   └── KafkaMarketDataProvider (interface)
       └── AbstractMarketDataProvider (abstract class)
           ├── MarketDataService
           ├── MarketIndexDataService
           └── AssetMarketDataProviderBase (abstract class)
               ├── StockMarketDataService
               ├── ForexMarketDataService
               ├── CryptoMarketDataService
               └── CommodityMarketDataService

WatchlistDataOperations (interface)
   └── WatchlistDataService
```

## Data Flow

1. A user adds items to their watchlist or requests market data.
2. The request is handled by the appropriate coordinator service.
3. The coordinator service delegates to asset-specific services based on asset type.
4. Asset-specific services process and format the symbols appropriately.
5. Kafka messages are sent to request updates from external data providers.
6. When updates are complete, Kafka listeners process the results.

## Benefits of This Architecture

1. **Separation of Concerns**: Each service focuses on a specific responsibility.
2. **Code Reuse**: Common functionality is defined in interfaces and abstract classes.
3. **Type Safety**: Asset-specific processing is encapsulated in dedicated services.
4. **Maintainability**: Service boundaries are clearly defined, making it easier to modify one area without affecting others.
5. **Testability**: Each component can be tested in isolation.
6. **Event-Driven**: Uses Kafka for asynchronous processing and decoupling.

## Extension Points

To add support for a new asset type:
1. Create a new service that extends AssetMarketDataProviderBase.
2. Implement the required asset-specific processing in processSymbols().
3. Implement the onMarketDataUpdateComplete method to handle asset-specific updates.
4. Update MarketDataService to use the new service for the new asset type. 