sealed abstract class MajorScalaVersion
case object Scala29 extends MajorScalaVersion
case object Scala210 extends MajorScalaVersion

object ScalaVersion {
  def v(implicit sv: String) =
    if(sv startsWith "2.9.") Scala29
    else if(sv startsWith "2.10.") Scala210
    else sys.error("Scala version unmapped: " + sv)
}
