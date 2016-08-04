package eu.shiftforward.apso.config

import java.io.File
import java.net.{ MalformedURLException, URL }

import com.typesafe.config._
import com.typesafe.config.impl.Parseable

import eu.shiftforward.apso.config.Implicits._
import scala.collection.convert.WrapAsJava._

/**
 * Contains static methods for creating `Config` instances in a lazy way.
 *
 * The loading process resolves variables lazily - configurations are first completely loaded and merged
 * (reference.conf, the application file and default overrides) and only then are variables resolved. This
 * `ConfigFactory` also considers a third standard configuration file, `overrides.conf`, which has priority over the
 * application file and can be used to specify keys that should always be overriden, e.g. by environment variables if
 * they are defined.
 */
object LazyConfigFactory {

  lazy val load: Config = load(ConfigParseOptions.defaults, ConfigResolveOptions.defaults)

  def load(resourceBasename: String): Config =
    load(ConfigFactory.parseResourcesAnySyntax(resourceBasename),
      ConfigParseOptions.defaults, ConfigResolveOptions.defaults)

  def load(parseOptions: ConfigParseOptions, resolveOptions: ConfigResolveOptions): Config =
    load(defaultApplication(parseOptions), parseOptions, resolveOptions)

  def load(config: Config, parseOptions: ConfigParseOptions, resolveOptions: ConfigResolveOptions): Config =
    defaultOverrides(parseOptions).
      withFallback(config).
      withFallback(defaultReference(parseOptions)).
      resolve(resolveOptions)

  def defaultOverrides(parseOptions: ConfigParseOptions): Config =
    ConfigFactory.systemProperties.
      withFallback(Parseable.newResources("overrides.conf", parseOptions).parse.toConfig)

  def defaultApplication(parseOptions: ConfigParseOptions): Config = {
    val loader = parseOptions.getClassLoader
    if (loader == null) throw new ConfigException.BugOrBroken("ClassLoader should have been set here; bug in ConfigFactory. " + "(You can probably work around this bug by passing in a class loader or calling currentThread().setContextClassLoader() though.)")
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
      throw new ConfigException.Generic("You set more than one of config.file='" + file + "', config.url='" + url +
        "', config.resource='" + resource + "'; don't know which one to use!")
    } else {
      val overrideOptions: ConfigParseOptions = parseOptions.setAllowMissing(false)
      if (resource != null) {
        if (resource.startsWith("/")) resource = resource.substring(1)
        ConfigFactory.parseResources(loader, resource, overrideOptions)
      } else if (file != null) {
        ConfigFactory.parseFile(new File(file), overrideOptions)
      } else {
        try {
          ConfigFactory.parseURL(new URL(url), overrideOptions)
        } catch {
          case e: MalformedURLException =>
            throw new ConfigException.Generic("Bad URL in config.url system property: '" + url + "': " + e.getMessage, e)
        }
      }
    }
  }

  def defaultReference(parseOptions: ConfigParseOptions) =
    Parseable.newResources("reference.conf", parseOptions).parse.toConfig

  private[this] implicit def stringAsConfig(str: String): Config =
    ConfigFactory.parseString(str)

  private[this] implicit def seqKeyValueConfig(str: Seq[(String, Any)]): Config =
    str.foldLeft(ConfigFactory.empty) {
      case (acc, (path, v)) => ConfigFactory.parseString(s"$path = $v").withFallback(acc)
    }

  /**
   * A `Config` builder providing a DSL for retireving and modifying configs.
   */
  trait Builder {

    /**
     * The built config.
     */
    def config: Config

    /**
     * Returns a new builder with some settings overridden. The provided settings must exist in the currently built
     * config; otherwise, a `ConfigFactory.ValidationFailed` is thrown.
     *
     * @param overrides the settings to override in the current config
     * @return a new builder with the given settings overridden.
     */
    def withOverrides(overrides: Config): Builder = new WithOverridesBuilder(config, overrides)

    /**
     * Returns a new builder with some settings overridden. The provided settings must exist in the currently built
     * config; otherwise, a `ConfigFactory.ValidationFailed` is thrown.
     *
     * @param overrides the settings to override in the current config as a parseable config
     * @return a new builder with the given settings overridden.
     */
    def withOverrides(overrides: String): Builder = new WithOverridesBuilder(config, overrides)

    /**
     * Returns a new builder with some settings overridden. The provided settings must exist in the currently built
     * config; otherwise, a `ConfigFactory.ValidationFailed` is thrown.
     *
     * @param overrides a setting to override in the current config as a key-value pair
     * @param moreOverrides additional settings to override in the current config as key-value pairs
     * @return a new builder with the given settings overridden.
     */
    def withOverrides(overrides: (String, Any), moreOverrides: (String, Any)*): Builder =
      new WithOverridesBuilder(config, overrides +: moreOverrides)

    /**
     * Returns a new builder with some additional settings. The provided settings must not exist in the currently built
     * config; otherwise, a `ConfigFactory.ValidationFailed` is thrown.
     *
     * @param settings the settings to add to the current config
     * @return a new builder with the given settings added.
     */
    def withSettings(settings: Config): Builder = new WithSettingsBuilder(config, settings)

    /**
     * Returns a new builder with some additional settings. The provided settings must not exist in the currently built
     * config; otherwise, a `ConfigFactory.ValidationFailed` is thrown.
     *
     * @param settings the settings to add to the current config as a parseable config
     * @return a new builder with the given settings added.
     */
    def withSettings(settings: String): Builder = new WithSettingsBuilder(config, settings)

    /**
     * Returns a new builder with some additional settings. The provided settings must not exist in the currently built
     * config; otherwise, a `ConfigFactory.ValidationFailed` is thrown.
     *
     * @param settings a setting to add to the current config as a key-value pair
     * @param moreSettings additional settings to add to the current config as key-value pairs
     * @return a new builder with the given settings added.
     */
    def withSettings(settings: (String, Any), moreSettings: (String, Any)*): Builder =
      new WithSettingsBuilder(config, settings +: moreSettings)
  }

  /**
   * Returns a `LazyConfigFactory.Builder` targeted at a path of the default config.
   *
   * @param path the target path of the default config
   * @return a `LazyConfigFactory.Builder` targeted at the given path of the default config.
   */
  def loadAt(path: String): Builder = new AtPathBuilder(path)

  private[this] class AtPathBuilder(path: String) extends Builder {
    lazy val config = load.getConfig(path)
  }

  private[this] class WithOverridesBuilder(baseConfig: Config, overrides: Config) extends Builder {
    baseConfig.checkValid(overrides)
    lazy val config = overrides.withFallback(baseConfig)
  }

  private[this] class WithSettingsBuilder(baseConfig: Config, settings: Config) extends Builder {
    val validationProblems = settings.toMap[Unit].keys.toSeq.collect {
      case path if baseConfig.hasPath(path) =>
        new ConfigException.ValidationProblem(path, settings.origin(),
          "Settings added with .withSettings() must not override existing ones")
    }

    if (validationProblems.nonEmpty)
      throw new ConfigException.ValidationFailed(validationProblems)

    lazy val config = baseConfig.withFallback(settings)
  }
}
