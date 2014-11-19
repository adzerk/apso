package eu.shiftforward.apso.config

import com.typesafe.config.{ ConfigException, Config }

/**
 * Provides useful extension methods for `Config` instances.
 */
object Implicits {

  @inline private[this] def getOption[A](conf: Config, path: String, f: (Config, String) => A) =
    if (conf.hasPath(path)) Some(f(conf, path)) else None

  final implicit class ApsoConfig(val conf: Config) extends AnyVal {

    /**
     * Gets the value as a boolean wrapped in a `Some` if one is defined and `None` if not. This method throws an
     * exception if the path has a value associated but it is not of the requested type.
     *
     * @param path the path in the config
     * @return the value as a boolean wrapped in a `Some` if one is defined and `None` if not.
     */
    def getBooleanOption(path: String) = getOption(conf, path, _.getBoolean(_))

    /**
     * Gets the value as a int wrapped in a `Some` if one is defined and `None` if not. This method throws an
     * exception if the path has a value associated but it is not of the requested type.
     *
     * @param path the path in the config
     * @return the value as a int wrapped in a `Some` if one is defined and `None` if not.
     */
    def getIntOption(path: String) = getOption(conf, path, _.getInt(_))

    /**
     * Gets the value as a long wrapped in a `Some` if one is defined and `None` if not. This method throws an
     * exception if the path has a value associated but it is not of the requested type.
     *
     * @param path the path in the config
     * @return the value as a long wrapped in a `Some` if one is defined and `None` if not.
     */
    def getLongOption(path: String) = getOption(conf, path, _.getLong(_))

    /**
     * Gets the value as a double wrapped in a `Some` if one is defined and `None` if not. This method throws an
     * exception if the path has a value associated but it is not of the requested type.
     *
     * @param path the path in the config
     * @return the value as a double wrapped in a `Some` if one is defined and `None` if not.
     */
    def getDoubleOption(path: String) = getOption(conf, path, _.getDouble(_))

    /**
     * Gets the value as a string wrapped in a `Some` if one is defined and `None` if not. This method throws an
     * exception if the path has a value associated but it is not of the requested type.
     *
     * @param path the path in the config
     * @return the value as a string wrapped in a `Some` if one is defined and `None` if not.
     */
    def getStringOption(path: String) = getOption(conf, path, _.getString(_))

    /**
     * Gets the percentage value as a double wrapped in a `Some` if it is defined and `None` if not.
     *
     * @param path the path in the config
     * @throws ConfigException.BadValue if the percentage does not end with '%' or if it is not a double.
     * @return the percentage value as a double wrapped in a `Some` if one is defined and `None` if not.
     */
    def getPercentageOption(path: String): Option[Double] = {
      getStringOption(path).map { value =>
        try {
          if (value.last == '%')
            value.dropRight(1).toDouble / 100.0
          else
            value.toDouble
        } catch {
          case _: NumberFormatException =>
            throw new ConfigException.BadValue(conf.origin, path, "A percentage must end with '%' or be a double.")
        }
      }
    }

    /**
     * Gets the value as a double from a percentage.
     *
     * @param path the path in the config
     * @throws ConfigException.Missing if value is absent or null
     * @throws ConfigException.BadValue if the percentage does not end with '%' or if it is not a double.
     * @return the percentage value as a double.
     */
    def getPercentage(path: String): Double = {
      getPercentageOption(path) match {
        case None => throw new ConfigException.Missing(path)
        case Some(value) => value
      }
    }

  }
}
