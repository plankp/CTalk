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

Syntax highlighting for vim is supported: `ctalk.vim`.

For more info, see the [wiki](https://github.com/plankp/CTalk/wiki).
