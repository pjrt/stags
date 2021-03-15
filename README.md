# stags: Scala tags generator

## Installation

## Using graalvm: Recommended

Requires `native-image`.

Clone this repository and call `sbt cli/graalvm-native-image:packageBin`. This will create a native image in `cli/target/graalvm-native-image/stags`.
Copy this file somewhere in your `$PATH`.

### Using Coursier:

```bash
coursier bootstrap co.pjrt:stags-cli_2.12:0.5.0 -o stags
```

If you want to use `stags` tag generation as a library, you can add it to sbt with:

```
libraryDependencies += "co.pjrt" % "stags_2.12" % "0.5.0"
```

### Using Nailgun:

You can use Coursier to create a standalone cli for starting Stags with Nailgun like this:

```
coursier bootstrap --standalone co.pjrt:stags-cli_2.12:0.5.0 \
  -o stags_ng -f --main com.martiansoftware.nailgun.NGServer
stags_ng & // start nailgun in background
ng ng-alias stags co.pjrt.stags.cli.Main
ng stags --version
```

You can then create an alias for `ng stags` if that's still too much typing.

Caveats and tips:

* You must call `ng ng-alias` after every restart of the nailgun server. You could create
  a script to do this
  * You could also simply make an alias in your terminal (ie: `alias stags=ng co.pjrt.stags.cli.Main`).
* If you are running multiple Nailgun instances (for example, one for `stags` and one for `scalafmt`)
  you must run one of them in a different port.
  * In the above example, simply call `stags_ng $new_port` to run the stags ng instance in a
    different port. Then all `ng` calls need to have the flag `--nailgun-port $new_port` in them.
  * You could in theory simply feed both jars (ie: stags and scalafmt) into the same ng instance
    but beware this could cause classpath conflicts between the two (or more) jars.

## Usage

```bash
stags ./
```

This will fetch all Scala files under the current directory. The tags file
will be generated in `./tags`. To place the tags file somewhere else, do:

```bash
stags ./ -o path/to/tags
```

## Features

The two main differences between stags and a general ctags generator like
[Universal CTags](https://github.com/universal-ctags/ctags) is its ability to
understand Scala code (with all its intricacies) and the ability to produce
qualified tags.

### Understanding Scala intricacies and static tagging them

What are static tags? Static tags are tags for "static functions". In the C
world this means functions that can only be used in the file where they
are defined; you could think of them as "private". Vim understand static tags
and will match them first before anything else.

Static tags lend themselves nicely to private field and functions, so `stags`
marks private statements and fields as static, while taking care of some Scala
intricacies.

If a def/val/class/ect is `private` within its file, then it is static. If
it is private for some large scope, then it isn't static. This means that
if it is `private[X]` then we check if `X` is an enclosing object within the file.
However, if X isn't an enclosing object in this file, then we mark it as
non-static. For example

```scala
package org.example.somepackage.test

object X {
  object Y {
    private[X] def f = …
  }
}

object K {
  private[somepackage] def g = …
}
```

In this example, `f` would be static, but `g` isn't because `g` might be
accessed from outside the file.

Other cases that are marked as static are:

* constructor fields in classes (ie: in `class X(a: Int, b: String, c: Boolean)`, `a`, `b` and `c` will all be static)
  * But non-static for the **first** parameter group of `case` classes (since those are accessible by default)
    * `case class X(a: Int)(b: Int)` <- `a` will be non-static, but `b` will be static
  * Any that are marked as "private" are static
* the single field in an implicit class/case class
  * `implicit class X(val x: Int)` <- `x` is static
  * this is done because chances are that `x` will never be accessed anywhere but this file
* all implicit things (val, defs, class, etc)
  * these things are rarely, if ever, accessed via their tokens

### Qualified tags

A common pattern found when importing conflicting fields is to use them in a qualified form. For example:

```scala
import org.example.SomeObject
import org.example.OtherObject

SomeObject.foo(...)
OtherObject.foo(...)
```

In order to differentiate between the two, `stags` generates tags for all
fields along with an extra tag that combines their parent with the tag itself.
Note that `stags` never generates qualified tags for fields/methods in `trait`
and `class` (only objects and package objects) since said fields/methods cannot be
qualifiedly referenced.

Following code, by default, would produce three tags: `Example`, `foo` and
`Example.foo`:

```scala
package object test {
  object Example {
    def foo(...)
  }
}
```

The depth of the qualified tags is controlled by `--qualified-depth`. Setting it
to three (3) would produce a third tag `test.Example.foo`.

## Vim support for qualified tags

Vim won't understand such a tag right off the bat. The following
modification is required:

```viml
function! QualifiedTagJump() abort
  let l:plain_tag = expand("<cword>")
  let l:orig_keyword = &iskeyword
  set iskeyword+=\.
  let l:word = expand("<cword>")
  let &iskeyword = l:orig_keyword

  let l:splitted = split(l:word, '\.')
  let l:acc = []
  for wo in l:splitted
    let l:acc = add(l:acc, wo)
    if wo ==# l:plain_tag
      break
    endif
  endfor

  let l:combined = join(l:acc, ".")
  try
    execute "ta " . l:combined
  catch /.*E426.*/ " Tag not found
    execute "ta " . l:plain_tag
  endtry
endfunction

nnoremap <silent> <C-]> :<C-u>call QualifiedTagJump()<CR>
```
## Tips for tagging jars
### Vim support for jars
Vim supports the ability to look at the contents of a zip file by default in the following way:

```
vim zipfile:/path/to/zip::/path/inside/zip
```

This will open contents of the path inside of the zip in a new buffer. Since jars are just zip files,
we can use that to create tags for jar files. This is specially useful when wanting to tag external sources.

Before we can use that though, the following settings in vim need to be disabled:

```viml
set notagrelative " Required due to the way it tries to search for the tag relative to the tags file

au BufEnter zipfile:/*.scala set nomodifiable " This isn't required, but nice if you want it to behave like an IDE
```

You could get away with keeping `tagrelative` on IFF your `set tags` is set to to `./tags` and nothing else. If you search for the tags
file outsise of the current directly, then you MUST disable `tagrelative`.

With that set, we can now call `stags`, but we must pass `--absolute-files`. Since we disabled `tagrelative` we must now create tags as
absolute tags (path to files are absolute, not relative).

```bash
stags --absolute-files -o /path/to/output ./
```

### Tagging source files: Global cache

Since the `tags` setting in vim allows for multiple tag files (in order of preference), we can have a local tags files and a global one for all sources like so:

```viml
set tags=./tags,/home/user/globalTags
```

```bash
$ cd /home/user
$ stags --absolute-files -o globalTags /home/user/.cache/coursier/ /home/user/.ivy2/cache/
```

Some caveats:
* This will create a massive tags file.
* Since you can have different derisions of libraries in the cache, this will create a lot of duplicate tags (one for each version). This can lead to confusion in the future.

## Tagging source files: Downloading source files to local project

Another strategy is to instead use sbt to copy over the source jar that the project requires, and tagging only those files. For this to happen you will need a small plugin:

```scala
import sbt.Keys._
import sbt._
import java.nio.file.{FileAlreadyExistsException, Files, Path, Paths}
import scala.util.{Failure, Success, Try}

object DownloadSourcesPlugin {
  val downloadSources = taskKey[Unit]("Download sources")
  val downloadSourcesLocation = settingKey[File]("Download sounds location")
  val downloadSourcesTypes = settingKey[List[String]](
    "Types of sources to download (default: javadoc, source)"
  )

  def downloadSettings =
    Seq(
      // We store all sources in one directory for all projects.
      // This allows us to remove duplicates below, which are common in multi-projects.
      // Change this to `target.value / "externalSources"` if you want to have one per project.
      downloadSourcesLocation := file(".") / "target" / "externalSources",
      cleanFiles += baseDirectory.value / downloadSourcesLocation.value.toString,
      downloadSourcesTypes := List("sources"),
      downloadSources := {
        val report = updateClassifiers.value
        val log = streams.value.log
        val dir = downloadSourcesLocation.value
        val types = downloadSourcesTypes.value
        def matchesTypes(f: File) =
          types.exists(t => f.getName().endsWith(t + ".jar"))
        dir.mkdirs()
        report.allFiles.map {
          case target if matchesTypes(target) =>
            val newLink = (dir / target.getName).toPath
            Try(Files.createLink(newLink, target.toPath)) match {
              case Success(_)                             => ()
              case Failure(e: FileAlreadyExistsException) => ()
              case Failure(e)                             => log.error(e.getMessage)
            }
          case _ => ()
        }
      }
    )
}
```

After adding this plugin, we can call `sbt downloadSources`.

This plugin will copy the source files from the cache into `target/externalSources` for a project. `stags` can then
pick them up from there (alongside the source files). We can then tag sources and jars separately:

```bash
stags --file-types jar -o tags-external target/externalSources
stags --file-types scala -o tags ./
```

For this to work we need to also change our tags setting like so:

```vimscript
set tags=./tags,tags-external
```

One way to add this plugin to all projects without committing anything into your repos is to:
* Place the file above in `~/.sbt/1.0/plugins/`
* Create a GlobalPlugin in `~/.sbt/1.0/plugins/` with the following contents

```scala
import sbt._

object GlobalPlugin extends AutoPlugin {
  override def requires = sbt.plugins.JvmPlugin
  override def trigger = allRequirements

  override def projectSettings = DownloadSourcesPlugin.downloadSettings
}
```
