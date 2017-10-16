# CachingHttpClient
A lazy way to persist data by using the okhttp disk cache for responses.

Main purpose to use this is to 
1) limit caching of non-critical http GET calls to a db.
2) limit server over-spamming through default 1 minute max-age when online, and long max-stale when offline.
3) semi-reliable offline persistence thought okhttp disk cache.
