package co.pjrt

import scala.meta._

package object stags {

  type TagPosition = (Int, Int)

  implicit class DefnClassOps(val cls: Defn.Class) extends AnyVal {

    private def containsMod(m: Mod): Boolean =
      cls.mods.exists((c: Mod) => c.structure == m.structure)

    def isCaseClass: Boolean =
      containsMod(Mod.Case())

    def isImplicitClass: Boolean =
      containsMod(Mod.Implicit())
  }
}
