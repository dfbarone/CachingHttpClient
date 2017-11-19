# CachingOkHttpClient
A simple way to persist GET responses by using the okhttp disk cache.

Main purpose to use this is to 
1) Enfore the use of max-age for static http GET calls to limit network use.
2) Semi-reliable persistence of http responses through use of http disk cache.
3) Optional confiuration to save http responses to a db.
