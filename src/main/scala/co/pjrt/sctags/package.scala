package co.pjrt

import scala.meta._

package object stags {

  type TagPosition = (Int, Int)

  implicit class DefnClassOps(val cls: Defn.Class) extends AnyVal {

    def isCaseClass: Boolean =
      cls.mods.exists((m: Mod) => m.structure == Mod.Case().structure)
  }
}
