play-mongo-cache
================

A mongo-based cache for Play applications


# Motivation

The default Play cache is nice, but in many real-world applications, you want a cache that lives outside the web server process, because your web app might actually be running on multiple nodes (for load-balancing and/or failover reasons). A common scenario is a Heroku app with multiple dynos.

So you'll want a cache that resides in its own service, and can be accessed by all your web server processes. Memcached could do the trick. We started with memcached (via memcachier) but found it unreliable -- we had fairly frequent timeouts from the memcachier nodes. So we moved to Mongo, which has been working fine for us.

# Project overview
This repo is a basic scala Play project (what you get when you init a new Play project) with a custom Cache plugin that uses a capped collection in Mongo as a cache. To use this in your own Play project, you should:

1. Copy the code in ```util``` and ```service.cache``` into your projcet
2. Copy the lines from ```conf/play.plugins``` into your play.plugins file
3. Add the section at the end of ```conf/application.conf``` to your application.conf file
4. Add the dependency on ```se.radley.play-plugins-salat``` to your Build.scala file

In your code, you can then

```
import play.api.cache.Cache
import play.api.Play.current
```

And use the Cache object as you would any other way:

```
Cache.getOrElse("some-key")
Cache.set("key", "some value", expiration = 100)
```

# Notes

- The starter play project was generated with Play 2.1.3. Sorry that's a bit old.
- Change the name of your database in the mongo connection string in application.conf
- Change the name of the collection to use in CachePlugin.scala
