package eu.shiftforward.apso

/**
 * Object containing reflection-related helpers.
 */
object Reflect {

  /**
   * Returns a new instance of the given class using the default constructor.
   * @param className the qualified name of the class to instantiate
   * @tparam T the type of the class to instantiate
   * @return a new instance of the given class.
   */
  def newInstance[T](className: String): T =
    Class.forName(className).newInstance.asInstanceOf[T]

  /**
   * Returns a companion object by its qualified name.
   * @param objName the name of the object
   * @param man a manifest of the object to return
   * @tparam T the type of the object to return
   * @return the companion object with the given name.
   */
  def companion[T](objName: String)(implicit man: Manifest[T]): T =
    Class.forName(objName + "$").getField("MODULE$").get(man.runtimeClass).asInstanceOf[T]
}