package co.pjrt.stags.cli

import java.io.File

import org.scalameter.api._
import org.scalameter.picklers.Implicits._
import org.scalameter.picklers.noPickler._

class TagGeneratorBench extends Bench.LocalTime {
  val sizes: Gen[Int] = Gen.range("size")(300000, 1500000, 300000)

  performance of "stags" in {
    val file = Gen.single("stags")(new File("./"))
    val config = file.map(f => Config(Seq(f), None, 0))
    // val _ = Cli.run(config)
    using(config) in { Cli.run(_)}
  }
}
