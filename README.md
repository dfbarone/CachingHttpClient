# CachingOkHttp
A wrapper around okhttp to help persist HTTP GET responses by using the okhttp disk cache.

Main purpose to use this is to 
1) Enforce the use of max-age (via network interceptor) for (static URL) HTTP GET calls to limit network use.
2) Use http disk cache when offline (via interceptor).
3) Optionally implement CachingInterface to persist all HTTP responses.

```groovy
    // Initialize a CachingOkHttpClient
    CachingOkHttpClient httpClient = new CachingOkHttpClient.Builder(context/*to check network availability*/)
                .okHttpClient(new OkHttpClient.Builder().build())
                .maxAge(5*60/*5 minutes default*/)
                .maxStale(60*60*24/*1 day default*/)
                .cache("cache_name", SIZE_IN_MB)
                .dataStore(new CachingInterfaceImpl())
                .build();
```

```groovy
    // Build a CachingRequest
    CachingRequest request = new CachingRequest.Builder(
                new okhttp3.Request.Builder()
                    .url(url)
                    .get()
                    .addHeader(..., ...)
                    .build())
                .maxAge(60/*1 minute for this request*/)
                .build();
```

```groovy
    // Fetch your data as a string
    httpClient.getString(request);
    
    // Fetch your data as a string
    httpClient.getStringAsync(request)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe();
```
