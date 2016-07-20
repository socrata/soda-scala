sealed abstract class MajorScalaVersion
case object Scala210 extends MajorScalaVersion
case object Scala211 extends MajorScalaVersion

object ScalaVersion {
  def v(implicit sv: String) =
    if(sv startsWith "2.10.") Scala210
    else if(sv startsWith "2.11.") Scala211
    else sys.error("Scala version unmapped: " + sv)
}
