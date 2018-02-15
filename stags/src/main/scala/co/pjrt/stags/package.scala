package co.pjrt

import scala.meta._

package object stags {

  implicit class DefnClassOps(val cls: Defn.Class) extends AnyVal {

    def containsMod(m: Mod): Boolean =
      cls.mods.exists((c: Mod) => c.structure == m.structure)

    def isCaseClass: Boolean =
      containsMod(Mod.Case())

    def isImplicitClass: Boolean =
      containsMod(Mod.Implicit())
  }
}
