package eu.shiftforward.apso

object Reflect {
	def newInstance[T](className: String): T =
		Class.forName(className).newInstance.asInstanceOf[T]

	def companion[T](objName: String)(implicit man: Manifest[T]): T = 
	    Class.forName(objName + "$").getField("MODULE$").get(man.runtimeClass).asInstanceOf[T]
}