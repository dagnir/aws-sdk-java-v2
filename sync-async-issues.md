## Issues with current Sync and Async code paths

### ExecutionInterceptor does not have access to streaming content for async responses

### Two separate code paths for sync and async: AmazonSyncHttpClient, AmazonAsyncHttpClient

### Previous item means inevitible divergence in client behavior (e.g. retry behavior for streaming responses)

- What's fundamentally different between sync and async from the SDK pov?

  - return type?

## Issues with going adapted sync from Async

### HTTP implementations that are sync must be adapted to sync
This will take extra resources (memory, threads) to fake an asynchronous interface over the synchronous http implementation. Furthermore, if the customer is using a "sync" service client, then even more resources will be needed to adapt the async client BACK to async.

