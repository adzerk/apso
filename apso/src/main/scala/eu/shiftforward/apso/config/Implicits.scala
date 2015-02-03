package eu.shiftforward.apso.config

import com.typesafe.config.{ ConfigException, Config }

import scala.concurrent.duration._
import scala.collection.JavaConversions._

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
     * Gets the value as a duration wrapped in a `Some` if one is defined and `None` if not. This method throws an
     * exception if the path has a value associated but it is not of the requested type.
     *
     * @param path the path in the config
     * @return the value as a duration wrapped in a `Some` if one is defined and `None` if not.
     */
    def getDurationOption(path: String) = getOption(conf, path, _.getDuration(_, SECONDS).seconds)

    /**
     * Gets the value as a `Config` wrapped in a `Some` if one is defined and `None` if not. This method throws an
     * exception if the path has a value associated but it is not of the requested type.
     *
     * @param path the path in the config
     * @return the value as a `Config` wrapped in a `Some` if one is defined and `None` if not.
     */
    def getConfigOption(path: String) = getOption(conf, path, _.getConfig(_))

    /**
     * Gets the value as a list wrapped in a `Some` if one is defined and `None` if not. This method throws an
     * exception if the path has a value associated but it is not of the requested type.
     *
     * @param path the path in the config
     * @return the value as a list wrapped in a `Some` if one is defined and `None` if not.
     */
    def getListOption(path: String) = getOption(conf, path, _.getList(_).toList)

    /**
     * Gets the value as a boolean list wrapped in a `Some` if one is defined and `None` if not. This method throws an
     * exception if the path has a value associated but it is not of the requested type.
     *
     * @param path the path in the config
     * @return the value as a boolean list wrapped in a `Some` if one is defined and `None` if not.
     */
    def getBooleanListOption(path: String) = getOption(conf, path, _.getBooleanList(_).toList)

    /**
     * Gets the value as a string list wrapped in a `Some` if one is defined and `None` if not. This method throws an
     * exception if the path has a value associated but it is not of the requested type.
     *
     * @param path the path in the config
     * @return the value as a string list wrapped in a `Some` if one is defined and `None` if not.
     */
    def getStringListOption(path: String) = getOption(conf, path, _.getStringList(_).toList)

    /**
     * Gets the value as a int list wrapped in a `Some` if one is defined and `None` if not. This method throws an
     * exception if the path has a value associated but it is not of the requested type.
     *
     * @param path the path in the config
     * @return the value as a int list wrapped in a `Some` if one is defined and `None` if not.
     */
    def getIntListOption(path: String) = getOption(conf, path, _.getIntList(_).toList)

    /**
     * Gets the value as a double list wrapped in a `Some` if one is defined and `None` if not. This method throws an
     * exception if the path has a value associated but it is not of the requested type.
     *
     * @param path the path in the config
     * @return the value as a double list wrapped in a `Some` if one is defined and `None` if not.
     */
    def getDoubleListOption(path: String) = getOption(conf, path, _.getDoubleList(_).toList)

    /**
     * Gets the value as a long list wrapped in a `Some` if one is defined and `None` if not. This method throws an
     * exception if the path has a value associated but it is not of the requested type.
     *
     * @param path the path in the config
     * @return the value as a long list wrapped in a `Some` if one is defined and `None` if not.
     */
    def getLongListOption(path: String) = getOption(conf, path, _.getLongList(_).toList)

    /**
     * Gets the value as a duration list wrapped in a `Some` if one is defined and `None` if not. This method throws an
     * exception if the path has a value associated but it is not of the requested type.
     *
     * @param path the path in the config
     * @return the value as a duration list wrapped in a `Some` if one is defined and `None` if not.
     */
    def getDurationListOption(path: String) = getOption(conf, path, _.getDurationList(_, SECONDS).map(_.toLong.seconds).toList)

    /**
     * Gets the value as a config list wrapped in a `Some` if one is defined and `None` if not. This method throws an
     * exception if the path has a value associated but it is not of the requested type.
     *
     * @param path the path in the config
     * @return the value as a config list wrapped in a `Some` if one is defined and `None` if not.
     */
    def getConfigListOption(path: String) = getOption(conf, path, _.getConfigList(_).toList)

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

    /**
     * Gets the value as a Map[String, T] wrapped in a `Some` if it is defined and `None` if not.
     *
     * @param path the path in the config
     * @tparam T the return type of the Map
     * @return the Map wrapped in a `Some` if one is defined and `None` if not
     */
    def getMapOption[T](path: String): Option[Map[String, T]] =
      getOption(conf, path, _.getMap[T](_))

    /**
     * Gets the value as a Map[String, T]
     *
     * @param path the path in the config
     * @tparam T the return type of the Map
     * @return the Map value
     */
    def getMap[T](path: String): Map[String, T] = conf.getConfig(path).toMap[T]

    /**
     * Gets the value as a List[T] wrapped in a `Some` if it is defined and `None` if not
     *
     * @param path the path in the config
     * @tparam T the return type of the List
     * @return the List value  wrapped in a `Some` if it is defined and `None` if not
     */
    def getTypedListOption[T](path: String): Option[List[T]] =
      getOption(conf, path, _.getTypedList[T](_))

    /**
     * Gets the value as a List[T]
     *
     * @param path the path in the config
     * @tparam T the return type of the List
     * @return the List value
     */
    def getTypedList[T](path: String): List[T] =
      (for (
        entry <- conf.getList(path)
      ) yield entry.unwrapped().asInstanceOf[T]).toList

    /**
     * Converts a config into a Map[String,T]
     *
     * @tparam T the return type of the Map
     * @return the Map value
     */
    def toMap[T]: Map[String, T] =
      (for (
        entry <- conf.entrySet()
      ) yield (entry.getKey, entry.getValue.unwrapped().asInstanceOf[T])).toMap

  }
}
