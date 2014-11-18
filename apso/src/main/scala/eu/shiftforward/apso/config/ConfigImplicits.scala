package eu.shiftforward.apso.config

import com.typesafe.config.Config

/**
 * Provides useful extension methods for `Config` instances.
 */
trait ConfigImplicits {

  final implicit class ApsoConfig(val conf: Config) {

    @inline private[this] def getOption[A](path: String, f: (Config, String) => A) =
      if (conf.hasPath(path)) Some(f(conf, path)) else None

    /**
     * Gets the value as a boolean wrapped in a `Some` if one is defined and `None` if not. This method throws an
     * exception if the path has a value associated but it is not of the requested type.
     *
     * @param path the path in the config
     * @return the value as a boolean wrapped in a `Some` if one is defined and `None` if not.
     */
    def getBooleanOption(path: String) = getOption(path, _.getBoolean(_))

    /**
     * Gets the value as a int wrapped in a `Some` if one is defined and `None` if not. This method throws an
     * exception if the path has a value associated but it is not of the requested type.
     *
     * @param path the path in the config
     * @return the value as a int wrapped in a `Some` if one is defined and `None` if not.
     */
    def getIntOption(path: String) = getOption(path, _.getInt(_))

    /**
     * Gets the value as a long wrapped in a `Some` if one is defined and `None` if not. This method throws an
     * exception if the path has a value associated but it is not of the requested type.
     *
     * @param path the path in the config
     * @return the value as a long wrapped in a `Some` if one is defined and `None` if not.
     */
    def getLongOption(path: String) = getOption(path, _.getLong(_))

    /**
     * Gets the value as a double wrapped in a `Some` if one is defined and `None` if not. This method throws an
     * exception if the path has a value associated but it is not of the requested type.
     *
     * @param path the path in the config
     * @return the value as a double wrapped in a `Some` if one is defined and `None` if not.
     */
    def getDoubleOption(path: String) = getOption(path, _.getDouble(_))

    /**
     * Gets the value as a string wrapped in a `Some` if one is defined and `None` if not. This method throws an
     * exception if the path has a value associated but it is not of the requested type.
     *
     * @param path the path in the config
     * @return the value as a string wrapped in a `Some` if one is defined and `None` if not.
     */
    def getStringOption(path: String) = getOption(path, _.getString(_))
  }
}
