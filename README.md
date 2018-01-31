# stags: Scala tags generator

## Installation

Using Coursier:

```bash
coursier bootstrap co.pjrt:stags-cli_2.12:0.3.1 -o stags
```

If you want to use `stags` tag generation as a library, you can add it to sbt with:

```
libraryDependencies += "co.pjrt" % "stags_2.12" % "0.3.1"
```

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
