package eu.shiftforward.apso.io

/**
 * Trait that describes how a `FileDescriptor` extracts credentials from a config.
 *
 * The config file must follow the following structure:
 *
 * {
 *   default = $protocolCredentials
 *    ids {
 *      id1 = $protocolCredentials
 *     id2 = $protocolCredentials
 *    }
 *
 * The `default` is optional and is used as a fallback when the
 * file descriptor id matches none of the credentials ids.
 *
 * @tparam Conf The type of credential config that is extracted
 * @tparam CredObj The type of credential object that is extracted
 */
trait FileDescriptorCredentials[Conf, CredObj] {

  /**
   * Returns an unique id from the given file descriptor path to use when
   * accessing the config
   * @param path the file descriptor path
   * @return an unique id from the given file descriptor path
   */
  protected def id(path: String): String

  /**
   * Creates a credential object `T` from the file descriptor specific config
   * @param fdConfig the config containing specific keys required to create a
   *                 credential object `T` for the file descriptor
   * @return the credential object `T`
   */
  protected def createCredentials(fdId: String, fdConfig: Conf): CredObj

  /**
   * Reads from the config file and given the file descriptor path converted to
   * an unique id by the `ìd` function, extracts a credentials object `T`.
   *
   * @param conf the config file with the credentials keys
   * @param path the file descriptor path to be converted to an unique id
   * @return the credential object `T`
   */
  def read(conf: config.Credentials.Protocol[Conf], path: String): Option[CredObj] = {
    val fdId = id(path)
    conf.ids.get(fdId).orElse(conf.default).map(createCredentials(fdId, _))
  }
}
