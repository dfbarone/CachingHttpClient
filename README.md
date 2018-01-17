# CachingOkHttp
A simple way to persist HTTP GET responses by using the okhttp disk cache.

Main purpose to use this is to 
1) Enforce the use of max-age (via network interceptor) for (static URL) HTTP GET calls to limit network use.
2) Use http disk cache when offline (via interceptor).
3) Optionally implement CachingInterface to persist all HTTP responses.
