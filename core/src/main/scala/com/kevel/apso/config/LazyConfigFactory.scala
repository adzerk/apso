package com.kevel.apso.config

import java.io.File
import java.net.{MalformedURLException, URI}

import com.typesafe.config._
import com.typesafe.config.impl.Parseable

/** Contains static methods for creating `Config` instances in a lazy way.
  *
  * The loading process resolves variables lazily - configurations are first completely loaded and merged
  * (reference.conf, the application file and default overrides) and only then are variables resolved. This
  * `ConfigFactory` also considers a third standard configuration file, `overrides.conf`, which has priority over the
  * application file and can be used to specify keys that should always be overriden, e.g. by environment variables if
  * they are defined.
  */
object LazyConfigFactory {

  lazy val load: Config = load(ConfigParseOptions.defaults, ConfigResolveOptions.defaults)

  def load(resourceBasename: String): Config = load(
    ConfigFactory.parseResourcesAnySyntax(resourceBasename),
    ConfigParseOptions.defaults,
    ConfigResolveOptions.defaults
  )

  def load(parseOptions: ConfigParseOptions, resolveOptions: ConfigResolveOptions): Config =
    load(defaultApplication(parseOptions), parseOptions, resolveOptions)

  def load(config: Config, parseOptions: ConfigParseOptions, resolveOptions: ConfigResolveOptions): Config =
    defaultOverrides(parseOptions)
      .withFallback(config)
      .withFallback(defaultEnvReference(parseOptions))
      .withFallback(defaultReference(parseOptions))
      .resolve(resolveOptions)

  def defaultOverrides(parseOptions: ConfigParseOptions): Config =
    ConfigFactory.systemProperties.withFallback(Parseable.newResources("overrides.conf", parseOptions).parse.toConfig)

  def defaultApplication(parseOptions: ConfigParseOptions): Config = {
    val loader = parseOptions.getClassLoader
    if (loader == null)
      throw new ConfigException.BugOrBroken(
        "ClassLoader should have been set here; bug in ConfigFactory. " + "(You can probably work around this bug by passing in a class loader or calling currentThread().setContextClassLoader() though.)"
      )
    var specified: Int = 0
    var resource = System.getProperty("config.resource")
    if (resource != null) specified += 1
    val file = System.getProperty("config.file")
    if (file != null) specified += 1
    val url = System.getProperty("config.url")
    if (url != null) specified += 1
    if (specified == 0) {
      ConfigFactory.parseResourcesAnySyntax(loader, "application", parseOptions)
    } else if (specified > 1) {
      throw new ConfigException.Generic(
        "You set more than one of config.file='" + file + "', config.url='" + url +
          "', config.resource='" + resource + "'; don't know which one to use!"
      )
    } else {
      val overrideOptions: ConfigParseOptions = parseOptions.setAllowMissing(false)
      if (resource != null) {
        if (resource.startsWith("/")) resource = resource.substring(1)
        ConfigFactory.parseResources(loader, resource, overrideOptions)
      } else if (file != null) {
        ConfigFactory.parseFile(new File(file), overrideOptions)
      } else {
        try {
          ConfigFactory.parseURL(new URI(url).toURL, overrideOptions)
        } catch {
          case e: MalformedURLException =>
            throw new ConfigException.Generic(
              "Bad URL in config.url system property: '" + url + "': " + e.getMessage,
              e
            )
        }
      }
    }
  }

  def defaultReference(parseOptions: ConfigParseOptions) =
    Parseable.newResources("reference.conf", parseOptions).parse.toConfig

  def defaultEnvReference(parseOptions: ConfigParseOptions) = {
    val configEnv = Option(System.getProperty("apso.configloader.env"))
      .orElse(Option(System.getenv("CONFIG_ENV")))
      .getOrElse("development")

    Parseable.newResources(s"$configEnv.conf", parseOptions).parse.toConfig
  }
}
