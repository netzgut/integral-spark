# ∫ Integral Spark

A Servlet Filter for using the awesome [Tapestry IoC](https://tapestry.apache.org/ioc.html)
in combination with the [Spark Framework](http://sparkjava.com/).

Right now it's more a "proof of concept" than a battle-tested library, so proceed with care ;-)


## Why?

We love Tapestry at Netzgut, and IMHO the IoC container is totally awesome. Lately we want
do some micro-services stuff and we wanted to try out Spark, but the lack of an IoC container
was the first hurdle to take. So we decided to do it ourselves!

This project won't be helpful for many people, but we're releasing it anyway ;-)


## How?

To hook ourselves into Spark we needed to extend the `SparkFilter` class and intercept
application loading.


## Usage

NOTE: This library isn't released yet on maven, jcenter etc.!

### `build.gradle`:
```groovy
respositories {
  jcenter()
}

dependencies {
    compile "net.netzgut.integral:integral-spark:0.0.1"
}
```


### Custom web.xml / server

We need a custom `web.xml` to load our servlet filter, and we need to run it on
a server because we can't use the embedded Jetty in this setup. You can use the
`web.xml` file in the examples folder as a starter.

### Autodiscovery
Spark supports specifying the Spark Application classes via the init-param
`applicationClass` and won't be happy if no applications are defined. We changed
this behaviour and added an autodiscovery option if no `applicationClass` is
specificed in the filter config.

The autodiscovery leverages [Fast Classpath Scanner](https://github.com/lukehutch/fast-classpath-scanner)
to scan for classes implementing the interface `SparkApplication`. Because it scans
ALL available classloaders and only blacklists some system packages this might impact
startup time, so you can specify a search spec to white-/black-list packages and more.
See [this documentation](https://github.com/lukehutch/fast-classpath-scanner/wiki/2.-Constructor)
for all options available.


## Gradle task uploadArchives

To upload the archives you need to set some project properties:

- snapshot_repository
- snapshot_repository_username
- snapshot_repository_password

The fallbacks are empty strings, so you can build etc. without gradle failing instantly.


## Contribute

If you want to contribute feel free to open issues or provide pull requests.


## License

Apache 2.0 license, see `LICENSE.txt` and `NOTICE.txt` for more details.
