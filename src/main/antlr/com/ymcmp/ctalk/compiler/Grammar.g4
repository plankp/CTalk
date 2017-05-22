/**
 * MIT License
 *
 * Copyright (c) 2017 Paul T.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

grammar Grammar;

WHITESPACE
    : [ \t\r\n] -> skip
    ;

LINE_COMMENT
    : '#' ~[\r\n]* -> skip
    ;

NEST_COMMENT
    : '#{' (NEST_COMMENT | .)*? '}' -> skip
    ;

fragment
DIGIT
    : [0-9]
    ;

fragment
DIGIT_WOZ
    : [1-9]
    ;

fragment
DIGIT_HEX
    : DIGIT
    | [a-fA-F]
    ;

fragment
UNICODE_ESC
    : 'u' DIGIT_HEX DIGIT_HEX DIGIT_HEX DIGIT_HEX
    | 'U' DIGIT_HEX DIGIT_HEX DIGIT_HEX DIGIT_HEX DIGIT_HEX DIGIT_HEX DIGIT_HEX DIGIT_HEX
    ;

fragment
ESCAPE
    : '\\' ([abfnrtv"'\\] | UNICODE_ESC)
    ;

L_INT
    : [+-]? ('0' | DIGIT_WOZ (DIGIT)*)
    ;

L_DOUBLE
    : L_INT '.' (DIGIT)+
    ;

L_CHAR
    : '\'' (ESCAPE | ~['\r\n\\]) '\''
    ;

L_STRING
    : '"' (ESCAPE | ~["\r\n\\])* '"'
    ;

L_SYSPATH
    : '<' (ESCAPE | ~["\r\n\\])* '>'
    ;

UNDERSCORE
    : '_'
    ;

COLON
    : ':'
    ;

NSRES
    : '::'
    ;

LSQUARE
    : '['
    ;

RSQUARE
    : ']'
    ;

LPAREN
    : '('
    ;

RPAREN
    : ')'
    ;

SEMI
    : ';'
    ;

COMMA
    : ','
    ;

MEM_VAL
    : '.'
    ;

MEM_PTR
    : '->'
    ;

ELLIPSIS
    : '...'
    ;

ASSIGN
    : '='
    ;

T_VOID
    : 'void'
    ;

T_INT
    : 'int'
    ;

T_CHAR
    : 'char'
    ;

T_BOOL
    : 'bool'
    ;

T_DOUBLE
    : 'double'
    ;

K_AS
    : 'as'
    ;

K_TYPE
    : 'typename'
    ;

K_FUNCTION
    : 'function'
    ;

K_MACRO
    : 'macro'
    ;

K_END
    : 'end'
    ;

K_STRUCT
    : 'struct'
    ;

K_UNION
    : 'union'
    ;

K_CONST
    : 'const'
    ;

K_VOLATILE
    : 'volatile'
    ;

K_MODULE
    : 'module'
    ;

K_EXPORT
    : 'export'
    ;

K_INTERNAL
    : 'internal'
    ;

K_HIDDEN
    : 'hidden'
    ;

K_EXTERN
    : 'extern'
    ;

K_IMPORT
    : 'import'
    ;

K_RETURN
    : 'return'
    ;

IDENT
    : [a-zA-Z][a-zA-Z0-9_]*
    ;

program
    : p+=programLevel+
    ;

programLevel
    : defModule SEMI
    | defDependency SEMI
    | topLevel
    ;

namespace
    : IDENT (NSRES IDENT)*
    | UNDERSCORE (NSRES IDENT)+
    ;

topLevel
    : defFunction SEMI
    | defExternal SEMI
    | defStruct SEMI
    | defUnion SEMI
    ;

defModule
    : K_MODULE ns=namespace b+=moduleBody* K_END
    ;

moduleBody
    : K_EXPORT b=topLevel   # exportEntity
    | K_INTERNAL b=topLevel # internalEntity
    | K_HIDDEN? b=topLevel  # hiddenEntity
    ;

arrayBound
    : L_INT     # intArrBound
    | namespace # varArrBound
    ;

arrayBounds
    : arrayBound
    | arrayBound COMMA arrayBounds
    ;

lesserTypeId
    : (T_INT | T_CHAR | T_BOOL | T_DOUBLE) # primTypeId
    | n=namespace # nsTypeId
    | LSQUARE c=arrayBounds? t=typeId RSQUARE # ptrTypeId
    ;

typeId
    : c=K_CONST? v=K_VOLATILE? t=lesserTypeId # basicTypeId
    | K_VOLATILE K_CONST t=lesserTypeId # vconstTypeId
    ;

defFunction
    : K_FUNCTION n=IDENT r=retType? p=defParams s+=statement* K_END
    ;

retType
    : COLON T_VOID # voidRetType
    | COLON typeId # valueRetType
    ;

defParams
    : defParam (COMMA defParam)* v=defVariadic?
    | LPAREN RPAREN
    ;

defVariadic
    : COMMA ELLIPSIS
    ;

defParam
    : IDENT (COMMA IDENT)* COLON typeId
    ;

statement // follow the $RULE SEMI format
    : funcCall SEMI
    | retVal SEMI
    | defParam SEMI
    | assignVar SEMI
    ;

lvalExpression
    : funcRef
    ;

assignVar
    : d=lvalExpression ASSIGN s=expression
    ;

retVal
    : K_RETURN e=expression?
    ;

funcCall
    : n=namespace p+=parameter+ v+=variadicParam* # paramFuncCall
    | n=namespace LPAREN RPAREN # unitFuncCall
    ;

parameter
    : IDENT COLON expression
    ;

variadicParam
    : COMMA e=expression
    ;

expression
    : lesserExpr # basicExpr
    | e=expression K_AS t=typeId # castExpr
    ;

lesserExpr
    : (L_INT | L_DOUBLE | L_CHAR | L_STRING) # primExpr
    | (funcRef | funcCall) # refExpr
    | LPAREN e=expression RPAREN # braceExpr
    ;

funcRef
    : namespace (COLON IDENT)*
    | namespace t+=memberAccess+
    ;

memberAccess
    : MEM_VAL n=IDENT
    | MEM_PTR n=IDENT
    ;

defDependency
    : K_IMPORT n=namespace # importModule
    | K_EXTERN n=L_STRING  # includeLocal
    | K_EXTERN n=L_SYSPATH # includeSys
    ;

defMParams
    : IDENT (COMMA IDENT)* v=defVariadic?
    | LPAREN RPAREN
    ;

defExternal
    : K_EXTERN K_FUNCTION n=IDENT r=retType? p=defParams e=L_STRING # defExternFunction
    | K_EXTERN K_MACRO n=IDENT p=defMParams? e=L_STRING # defExternMacro
    | K_EXTERN K_TYPE n=IDENT e=L_STRING # defExternType
    ;

defStruct
    : K_STRUCT n=IDENT (defParam SEMI)* K_END
    ;

defUnion
    : K_UNION n=IDENT (defParam SEMI)* K_END
    ;