# CTalk

A pun on Smalltalk and C

## How does this work?

When you build this project, you are building a trans-compiler from CTalk to C.
You can use stuff from C in CTalk, not the other way around though (as of now).

To build, run `gradlew build` and the trans-compiler will be created under
`build/distributions`. Extract the one of them and done!

## Show me code

```
#{ Just your average day "Hello, world!" program }

import std::io;

function main:int argc: int, argv:[[char]]
    std::io::println str:"Hello, world!";
    return 0;
end;
```

## Differences from C

### File name

* Yes, CTalk source files *must* have an extension of `.ct` (see modules and imports).

### Comments

* Single lined ones start with `#` instead of `//`.
* Multi-lined ones start with `#{` and end with `}`.
* Multi-lined ones are nested by matching `{` and `}`.

### Operators

* There are no `++` or `--` operators.
* `()` does not automatically convert to a function call.
* `[3 arr]` instead of `arr[3]`; order is not fixed.
* Compound literals are not a thing (yet?).
* `!` is a bitwise not and `not` is a logical not.
* Casting is done via `expression as type` instead of `(type) expression`.
* Dereferencing with `*` is replaced with `[expression]`.
* `@` replaces `&` when getting the address of a value.
* `&&` is replaced with `and` and `||` is replaced with `or`.
* No ternary operator `?:` (yet).
* No comma in expressions, and this will never happen!

### Data type

* `char`, `int`, `bool`, `double` are predefined (more coming).
* Pointer of a type is written as `[type]` instead of `type *`.
* Arrays are written as `[size type]` instead of `type[size]`.
* To get (for example) `int[2][4]`, write `[2, 4 int]`.
* Type comes after the name: `a, b, c : int`.

### Prototypes and Dependencies

* All functions and types are automatically forward declared.
* `import` is used instead of `#include` (see interfacing with C).
* Modules are used instead of header files (see modules and imports).

## Modules and Imports

* When importing a module, the compiler will search in this order:
	1. The CTalk standard library (only std::arg and std::io at the moment).
	2. The directory of the current source code file.
* Importing does *not* pollute the global namespace. Referencing its exported entities still requires the full name.
* If the definitions are not done in a module block, they are considered to be in the *nameless module*.
* To reference other definitions within the current module, it is necessary to start with an underscore. For example: `_::foo`.
* Definitions inside a module block can have these visibilities:
	* `export` It is visible to all modules.
	* `internal` It is visible to itself and its child namespaces.
	* `hidden` It is visible to only itself.
* Omitting the visibility --that includes everything in the *nameless module*-- have the visibility of `hidden`.

Consider the following project layout:

```
project/
|- main.ct
|- foo/
|  |- a.ct
|- bar/
|  |- b.ct
|  |- baz/
|  |  |- c.ct
```

Assume `main.ct` needs to import all three files:

```
# File main.ct

import foo::a;
import bar::b;
import bar::baz::c;

function main:int argc:int argv:[[char]]
    # Do stuff here!
    return 0;
end;
```

```
# File foo/a.ct

module foo::a
end;
```

```
# File bar/b.ct
module bar::b
end;
```

```
# File bar/baz/c.ct
module bar::baz::c
end;
```

## Interfacing with C

* To use a C header, use `extern "path"` or `extern <path>` at the beginning of the file accordingly.
* CTalk mangles its symbol names. Proper `externs` will need to be done:
	* `extern typename` translates to `typedef`
	* `extern macro` translates to `#define`
	* `extern function` creates a delegate call.
