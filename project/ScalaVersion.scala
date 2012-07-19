sealed abstract class MajorScalaVersion
case object Scala28 extends MajorScalaVersion
case object Scala29 extends MajorScalaVersion

object ScalaVersion {
  def v(implicit sv: String) =
    if(sv startsWith "2.8.") Scala28
    else if(sv startsWith "2.9.") Scala29
    else sys.error("Scala version unmapped: " + sv)
}
