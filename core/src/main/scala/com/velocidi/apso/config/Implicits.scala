package com.velocidi.apso.config

import com.typesafe.config._
import com.github.nscala_time.time.Imports.{ Duration => _, _ }

import scala.annotation.implicitNotFound
import scala.concurrent.duration._
import scala.collection.JavaConverters._
import scala.util.Try

import com.velocidi.apso.io.LocalFileDescriptor
import ConfigReader.{ BasicConfigReaders, ExtendedConfigReaders }

/**
 * Provides useful extension methods for `Config` instances.
 */
@deprecated("Use https://github.com/pureconfig/pureconfig instead", "2017/07/13")
object Implicits extends BasicConfigReaders with ExtendedConfigReaders {

  @inline private[this] def extractOption[A](conf: Config, path: String, f: (Config, String) => A) =
    if (conf.hasPath(path)) Some(f(conf, path)) else None

  final implicit class ApsoConfig(val conf: Config) extends AnyVal {

    /**
     * Gets a value from this `Config`.
     *
     * @param path the path in the config
     * @tparam T the type of the value to extract
     * @return the value.
     */
    def get[T](path: String)(implicit configReader: ConfigReader[T]) = configReader(conf, path)

    /**
     * Gets the value wrapped in a `Some` if one is defined and `None` if not. This method throws an exception if the
     * path has a value associated but it is not of the requested type.
     *
     * @param path the path in the config
     * @tparam T the type of the value to extract
     * @return the value as a boolean wrapped in a `Some` if one is defined and `None` if not.
     */
    def getOption[T](path: String)(implicit configReader: ConfigReader[T]) = extractOption(conf, path, configReader)

    /**
     * Gets the value as a boolean wrapped in a `Some` if one is defined and `None` if not. This method throws an
     * exception if the path has a value associated but it is not of the requested type.
     *
     * @param path the path in the config
     * @return the value as a boolean wrapped in a `Some` if one is defined and `None` if not.
     */
    def getBooleanOption(path: String) = extractOption(conf, path, _.getBoolean(_))

    /**
     * Gets the value as a int wrapped in a `Some` if one is defined and `None` if not. This method throws an
     * exception if the path has a value associated but it is not of the requested type.
     *
     * @param path the path in the config
     * @return the value as a int wrapped in a `Some` if one is defined and `None` if not.
     */
    def getIntOption(path: String) = extractOption(conf, path, _.getInt(_))

    /**
     * Gets the value as a long wrapped in a `Some` if one is defined and `None` if not. This method throws an
     * exception if the path has a value associated but it is not of the requested type.
     *
     * @param path the path in the config
     * @return the value as a long wrapped in a `Some` if one is defined and `None` if not.
     */
    def getLongOption(path: String) = extractOption(conf, path, _.getLong(_))

    /**
     * Gets the value as a double wrapped in a `Some` if one is defined and `None` if not. This method throws an
     * exception if the path has a value associated but it is not of the requested type.
     *
     * @param path the path in the config
     * @return the value as a double wrapped in a `Some` if one is defined and `None` if not.
     */
    def getDoubleOption(path: String) = extractOption(conf, path, _.getDouble(_))

    /**
     * Gets the value as a string wrapped in a `Some` if one is defined and `None` if not. This method throws an
     * exception if the path has a value associated but it is not of the requested type.
     *
     * @param path the path in the config
     * @return the value as a string wrapped in a `Some` if one is defined and `None` if not.
     */
    def getStringOption(path: String) = extractOption(conf, path, _.getString(_))

    /**
     * Gets the value as a duration wrapped in a `Some` if one is defined and `None` if not. This method throws an
     * exception if the path has a value associated but it is not of the requested type.
     *
     * @param path the path in the config
     * @return the value as a duration wrapped in a `Some` if one is defined and `None` if not.
     */
    def getDurationOption(path: String) = extractOption(conf, path, _.getDuration(_, SECONDS).seconds)

    /**
     * Gets the value as a `Config` wrapped in a `Some` if one is defined and `None` if not. This method throws an
     * exception if the path has a value associated but it is not of the requested type.
     *
     * @param path the path in the config
     * @return the value as a `Config` wrapped in a `Some` if one is defined and `None` if not.
     */
    def getConfigOption(path: String) = extractOption(conf, path, _.getConfig(_))

    /**
     * Gets the value as a list wrapped in a `Some` if one is defined and `None` if not. This method throws an
     * exception if the path has a value associated but it is not of the requested type.
     *
     * @param path the path in the config
     * @return the value as a list wrapped in a `Some` if one is defined and `None` if not.
     */
    def getListOption(path: String) = extractOption(conf, path, _.getList(_).asScala.toList)

    /**
     * Gets the value as a boolean list wrapped in a `Some` if one is defined and `None` if not. This method throws an
     * exception if the path has a value associated but it is not of the requested type.
     *
     * @param path the path in the config
     * @return the value as a boolean list wrapped in a `Some` if one is defined and `None` if not.
     */
    def getBooleanListOption(path: String) = extractOption(conf, path, _.getBooleanList(_).asScala.toList)

    /**
     * Gets the value as a string list wrapped in a `Some` if one is defined and `None` if not. This method throws an
     * exception if the path has a value associated but it is not of the requested type.
     *
     * @param path the path in the config
     * @return the value as a string list wrapped in a `Some` if one is defined and `None` if not.
     */
    def getStringListOption(path: String) = extractOption(conf, path, _.getStringList(_).asScala.toList)

    /**
     * Gets the value as a int list wrapped in a `Some` if one is defined and `None` if not. This method throws an
     * exception if the path has a value associated but it is not of the requested type.
     *
     * @param path the path in the config
     * @return the value as a int list wrapped in a `Some` if one is defined and `None` if not.
     */
    def getIntListOption(path: String) = extractOption(conf, path, _.getIntList(_).asScala.toList)

    /**
     * Gets the value as a double list wrapped in a `Some` if one is defined and `None` if not. This method throws an
     * exception if the path has a value associated but it is not of the requested type.
     *
     * @param path the path in the config
     * @return the value as a double list wrapped in a `Some` if one is defined and `None` if not.
     */
    def getDoubleListOption(path: String) = extractOption(conf, path, _.getDoubleList(_).asScala.toList)

    /**
     * Gets the value as a long list wrapped in a `Some` if one is defined and `None` if not. This method throws an
     * exception if the path has a value associated but it is not of the requested type.
     *
     * @param path the path in the config
     * @return the value as a long list wrapped in a `Some` if one is defined and `None` if not.
     */
    def getLongListOption(path: String) = extractOption(conf, path, _.getLongList(_).asScala.toList)

    /**
     * Gets the value as a duration list wrapped in a `Some` if one is defined and `None` if not. This method throws an
     * exception if the path has a value associated but it is not of the requested type.
     *
     * @param path the path in the config
     * @return the value as a duration list wrapped in a `Some` if one is defined and `None` if not.
     */
    def getDurationListOption(path: String) = extractOption(conf, path, _.getDurationList(_, SECONDS).asScala.map(_.toLong.seconds).toList)

    /**
     * Gets the value as a config list wrapped in a `Some` if one is defined and `None` if not. This method throws an
     * exception if the path has a value associated but it is not of the requested type.
     *
     * @param path the path in the config
     * @return the value as a config list wrapped in a `Some` if one is defined and `None` if not.
     */
    def getConfigListOption(path: String) = extractOption(conf, path, _.getConfigList(_).asScala.toList)

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
    def getMapOption[T](path: String)(implicit configReader: ConfigReader[T]): Option[Map[String, T]] =
      extractOption(conf, path, _.getMap[T](_))

    /**
     * Gets the value as a deep flattened Map[String, T] wrapped in a `Some` if it is defined and `None` if not.
     *
     * @param path the path in the config
     * @tparam T the return type of the Map
     * @return the Map wrapped in a `Some` if one is defined and `None` if not
     */
    def getFlattenedMapOption[T](path: String)(implicit configReader: ConfigReader[T]): Option[Map[String, T]] =
      extractOption(conf, path, _.getFlattenedMap[T](_))

    /**
     * Gets the value as a Map[String, T]
     *
     * @param path the path in the config
     * @tparam T the return type of the Map
     * @return the Map value
     */
    def getMap[T](path: String)(implicit configReader: ConfigReader[T]): Map[String, T] = conf.getConfig(path).toMap[T]

    /**
     * Gets the value as a deep flattened Map[String, T]
     *
     * @param path the path in the config
     * @tparam T the return type of the Map
     * @return the Map value
     */
    def getFlattenedMap[T](path: String)(implicit configReader: ConfigReader[T]): Map[String, T] =
      conf.getConfig(path).toFlattenedMap[T]

    /**
     * Gets the value as a Map[String, Config] wrapped in a `Some` if it is defined and `None` if not.
     *
     * @param path the path in the config
     * @return the Map wrapped in a `Some` if one is defined and `None` if not
     */
    def getConfigMapOption(path: String): Option[Map[String, Config]] =
      extractOption(conf, path, _.getConfigMap(_))

    /**
     * Gets the value as a Map[String, Config]
     *
     * @param path the path in the config
     * @return the Map value
     */
    def getConfigMap(path: String): Map[String, Config] = conf.getConfig(path).toConfigMap

    /**
     * Gets the value as a List[T] wrapped in a `Some` if it is defined and `None` if not
     *
     * @param path the path in the config
     * @tparam T the return type of the List
     * @return the List value  wrapped in a `Some` if it is defined and `None` if not
     */
    def getTypedListOption[T](path: String): Option[List[T]] =
      extractOption(conf, path, _.getTypedList[T](_))

    /**
     * Gets the value as a List[T]
     *
     * @param path the path in the config
     * @tparam T the return type of the List
     * @return the List value
     */
    def getTypedList[T](path: String): List[T] =
      (for (
        entry <- conf.getList(path).asScala
      ) yield entry.unwrapped.asInstanceOf[T]).toList

    /**
     * Converts the config into a Map[String, T]
     *
     * @tparam T the return type of the Map
     * @return the Map value
     * @throws ConfigException if the value cannot be read as type `T`
     */
    def toMap[T](implicit configReader: ConfigReader[T]): Map[String, T] = {
      conf.root.keySet.asScala.map { k =>
        val quotedKey = ConfigUtil.joinPath(k)
        quotedKey -> configReader(conf, quotedKey)
      }.toMap
    }

    /**
     * Converts the config into a Map[String, T] with all the config keys flattened into one.
     *
     * @tparam T the return type of the Map
     * @return the Map value
     */
    def toFlattenedMap[T](implicit configReader: ConfigReader[T]): Map[String, T] = {
      (for (
        entry <- conf.entrySet.asScala
      ) yield (entry.getKey, configReader(conf, entry.getKey))).toMap
    }

    /**
     * Converts the config into a Map[String, Config]
     *
     * @return the Map value
     */
    def toConfigMap: Map[String, Config] = {
      val configRegex = "(\"[^\"]*\")|([^\\.]+)".r
      (for {
        entry <- conf.entrySet.asScala
        key <- configRegex.findFirstMatchIn(entry.getKey).flatMap(_.subgroups.flatMap(Option(_)).headOption)
        value <- Try(conf.getConfig(key)).toOption
      } yield { key -> value }).toMap
    }
  }
}

/**
 * Represents a function that given a config and the config key string, will return the given type.
 *
 * @tparam T the type to be returned
 */
@deprecated("Use https://github.com/pureconfig/pureconfig instead", "2017/07/13")
@implicitNotFound(msg = "Could not find a way to read a ${T} from a Config. You might want to import or implement a ConfigReader[${T}]")
trait ConfigReader[+T] extends ((Config, String) => T)

@deprecated("Use https://github.com/pureconfig/pureconfig instead", "2017/07/13")
object ConfigReader {
  def configReader[T](f: (Config, String) => T): ConfigReader[T] =
    new ConfigReader[T] {
      def apply(config: Config, key: String): T = f(config, key)
    }

  trait BasicConfigReaders {

    // ConfigReaders implicits
    implicit val boolConfigReader = configReader[Boolean](_.getBoolean(_))
    implicit val stringConfigReader = configReader[String](_.getString(_))
    implicit val intConfigReader = configReader[Int](_.getInt(_))
    implicit val doubleConfigReader = configReader[Double](_.getDouble(_))
    implicit val longConfigReader = configReader[Long](_.getLong(_))
    implicit val unitConfigReader = configReader[Unit]((_, _) => ())
    implicit val durationConfigReader = configReader[Duration](_.getDuration(_))
    implicit val finiteDurationConfigReader = configReader[FiniteDuration](_.getDuration(_))
    implicit val javaDurationConfigReader = configReader[java.time.Duration](_.getDuration(_))
    implicit val configValueConfigReader = configReader[ConfigValue](_.getValue(_))
    implicit val configConfigReader = configReader[Config](_.getConfig(_))

    // ConfigReaders implicits for lists
    implicit val boolListConfigReader = configReader[List[Boolean]](_.getBooleanList(_).asScala.toList.map(Boolean.unbox))
    implicit val stringListConfigReader = configReader[List[String]](_.getStringList(_).asScala.toList)
    implicit val intListConfigReader = configReader[List[Int]](_.getIntList(_).asScala.toList.map(Int.unbox))
    implicit val doubleListConfigReader = configReader[List[Double]](_.getDoubleList(_).asScala.toList.map(Double.unbox))
    implicit val longListConfigReader = configReader[List[Long]](_.getLongList(_).asScala.toList.map(Long.unbox))
    implicit val durationListConfigReader =
      configReader[List[Duration]](_.getDurationList(_).asScala.map(durationToFiniteDuration).toList)
    implicit val finiteDurationListConfigReader =
      configReader[List[FiniteDuration]](_.getDurationList(_).asScala.map(durationToFiniteDuration).toList)
    implicit val javaDurationListConfigReader = configReader[List[java.time.Duration]](_.getDurationList(_).asScala.toList)
    implicit val configListConfigReader = configReader[List[Config]](_.getConfigList(_).asScala.toList)

    implicit def durationToFiniteDuration(d: java.time.Duration): FiniteDuration = Duration.fromNanos(d.toNanos)
  }

  object BasicConfigReaders extends BasicConfigReaders

  trait ExtendedConfigReaders {
    /**
     * A `ConfigReader` for reading a `LocalDate` from a `Config`.
     */
    implicit val localDateConfReader = configReader[LocalDate](_.getString(_).toLocalDate)

    /**
     * A `ConfigReader` for reading a `DateTime` from a `Config`.
     */
    implicit val dateTimeConfReader = configReader[DateTime](_.getString(_).toDateTime)

    /**
     * A `ConfigReader` for reading a `LocalFileDescriptor` from a `Config`.
     */
    implicit val localFileDescriptorConfReader = configReader[LocalFileDescriptor] {
      (config, key) => LocalFileDescriptor(config.getString(key))
    }
  }

  object ExtendedConfigReaders extends ExtendedConfigReaders
}
