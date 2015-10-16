package eu.shiftforward.apso.config

import com.typesafe.config.Config
import eu.shiftforward.apso.config.Implicits._

/**
 * Trait that describes how a `FileDescriptor` extracts credentials from a config.
 *
 * The config file must follow the following structure:
 *
 *     {{protocol}}.default = $protocolCredentials
 *     {{protocol}}.credentials = [{
 *       id = id1
 *       creds = $protocolCredentials
 *     },{
 *       ids = [id2, id3]
 *       creds = $protocolCredentials
 *     }]
 *
 * The {{protocol}}.default is an optional and is used as a fallback when the
 * file descriptor id matches none of the credentials ids.
 *
 * Inside the {{protocol}}.credentials list there must be a `creds` key which
 * contains the all required keys for the pertaining protocol, and either an
 * `id` or a `ids` key which represent an unique id or a list of unique ids
 * respectively for which the credentials are valid.
 *
 * @tparam T The type of credential object that is extracted
 */
trait FileDescriptorCredentials[T] {

  /**
   * Reads from the config file and given the file descriptor path converted to
   * an unique id by the `ìd` function, extracts a credentials object `T`.
   *
   * @param config the config file with the credentials keys
   * @param path the file descriptor path to be converted to an unique id
   * @return the credential object `T`
   */
  def read(config: Config, path: String): Option[T] = {
    val fdId = id(path)

    config.getConfigListOption(s"$protocol.credentials")
      .flatMap { protocolCreds =>
        protocolCreds.find {
          case protocolCred if protocolCred.hasPath("ids") =>
            protocolCred.getStringList("ids").contains(fdId)
          case protocolCred =>
            protocolCred.getStringOption("id").filter(_ == fdId).isDefined
        }.map(_.getConfig("creds"))
      }
      .orElse(config.getConfigOption(s"$protocol.default"))
      .map(createCredentials(fdId, _))
  }

  /**
   * The protocol key to use when accessing the config
   * @return the protocol key to use when accessing the config
   */
  def protocol: String

  /**
   * Creates a credential object `T` from the file descriptor specific config
   * @param fdConfig the config containing specific keys required to create a
   *                 credential object `T` for the file descriptor
   * @return the credential object `T`
   */
  protected def createCredentials(fdId: String, fdConfig: Config): T

  /**
   * Returns an unique id from the given file descriptor path to use when
   * accessing the config
   * @param path the file descriptor path
   * @return an unique id from the given file descriptor path
   */
  protected def id(path: String): String
}
