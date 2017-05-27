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

L_TRUE
    : 'true'
    ;

L_FALSE
    : 'false'
    ;

L_NULL
    : 'null'
    ;

L_INT
    : '0'
    | DIGIT_WOZ (DIGIT)*
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

ADDRESS_OF
    : '@'
    ;

ASSIGN
    : '='
    ;

ADD
    : '+'
    ;

SUB
    : '-'
    ;

MUL
    : '*'
    ;

DIV
    : '/'
    ;

MOD
    : '%'
    ;

EXCLAIM
    : '!'
    ;

LESS_THAN
    : '<'
    ;

GREATER_THAN
    : '>'
    ;

LESS_EQUAL
    : '<='
    ;

GREATER_EQUAL
    : '>='
    ;

EQUAL
    : '=='
    ;

NOT_EQUAL
    : '!='
    ;

SHIFT_LEFT
    : '<<'
    ;

SHIFT_RIGHT
    : '>>'
    ;

BIT_AND
    : '&'
    ;

BIT_XOR
    : '^'
    ;

BIT_OR
    : '|'
    ;

K_AND
    : 'and'
    ;

K_OR
    : 'or'
    ;

K_NOT
    : 'not'
    ;

SPECIAL_ASSIGN
    : '+='
    | '-='
    | '*='
    | '/='
    | '%='
    | '<<='
    | '>>='
    | '&='
    | '^='
    | '|='
    ;

LABEL
    : '$'
    ;

T_VOID
    : 'void'
    ;

T_LONG
    : 'long'
    ;

T_INT
    : 'int'
    ;

T_SHORT
    : 'short'
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

T_FLOAT
    : 'float'
    ;

T_SIZE
    : 'size_t'
    ;

T_SIGNED
    : 'signed'
    ;

T_UNSIGNED
    : 'unsigned'
    ;

T_COMPLEX
    : 'complex'
    ;

T_IMAGINARY
    : 'imaginary'
    ;

T_ANY
    : 'any_t'
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

K_SIZEOF
    : 'sizeof'
    ;

K_SWITCH
    : 'switch'
    ;

K_CASE
    : 'case'
    ;

K_DEFAULT
    : 'default'
    ;

K_FOR
    : 'for'
    ;

K_IF
    : 'if'
    ;

K_ELSEIF
    : 'elseif'
    ;

K_ELSE
    : 'else'
    ;

K_BREAK
    : 'break'
    ;

K_CONTINUE
    : 'continue'
    ;

K_DO
    : 'do'
    ;

K_GOTO
    : 'goto'
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
    | defModuleVar SEMI
    ;

defModule
    : K_MODULE ns=namespace b+=moduleBody* K_END
    ;

moduleBody
    : K_EXPORT b=topLevel   # exportEntity
    | K_INTERNAL b=topLevel # internalEntity
    | K_HIDDEN? b=topLevel  # hiddenEntity
    ;

arrayBounds
    : expression
    | expression COMMA arrayBounds
    ;

integral
    : (T_SIGNED | T_UNSIGNED)? i=lesserIntegral
    ;

lesserIntegral
    : T_CHAR
    | T_SHORT
    | T_INT
    | T_LONG T_INT?
    | T_LONG T_LONG T_INT?
    ;

floatPoint
    : (T_COMPLEX | T_IMAGINARY)? f=lesserFloatPoint
    ;

lesserFloatPoint
    : T_FLOAT
    | T_DOUBLE
    | T_LONG T_DOUBLE
    ;

lesserTypeId
    : (T_BOOL | T_SIZE | T_ANY) # primTypeId
    | (integral | floatPoint) # stdTypeId
    | n=namespace # nsTypeId
    | LSQUARE c=arrayBounds? t=typeId RSQUARE # ptrTypeId
    | p=defParams MEM_PTR r=typeId # funcTypeId
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

defModuleVar
    : localInit (COMMA localInit)* COLON typeId
    ;

defLocal
    : localInit (COMMA localInit)* COLON typeId
    ;

localInit
    : n=IDENT ASSIGN v=expression
    | n=IDENT
    ;

statement
    : funcCall SEMI
    | retVal SEMI
    | defLocal SEMI
    | assignVar SEMI
    | ifFlow SEMI
    | forFlow SEMI
    | switchFlow SEMI
    | alterFlow SEMI
    | blockScope SEMI
    | labelFlow       // No SEMI here! (rule itself already ends with SEMI)
    | gotoFlow SEMI
    ;

gotoFlow
    : K_GOTO LABEL n=IDENT
    ;

labelFlow
    : LABEL n=IDENT COLON s=statement
    ;

blockScope
    : K_DO s+=statement* K_END
    ;

alterFlow
    : K_BREAK
    | K_CONTINUE
    ;

switchFlow
    : K_SWITCH e=expression c1+=caseFlow* d=defaultFlow? c2+=caseFlow* K_END
    ;

defaultFlow
    : K_DEFAULT COLON s+=statement*
    ;

caseFlow
    : K_CASE e=expression COLON s+=statement*
    ;

forFlow
    : K_FOR c=forCondition s+=statement* K_END
    ;

forCondition
    : c=expression
    | i=expression? COMMA c=expression? COMMA f=expression?
    ;

ifFlow
    : K_IF c=expression s+=statement* a+=elseIfFlow* e=elseFlow? K_END
    ;

elseIfFlow
    : K_ELSEIF c=expression s+=statement*
    ;

elseFlow
    : K_ELSE s+=statement*
    ;

lvalExpression
    : funcRef
    | dereference
    ;

assignVar
    : d=lvalExpression (ASSIGN | SPECIAL_ASSIGN) s=expression
    ;

retVal
    : K_RETURN e=expression?
    ;

funcCall
    : n=namespace p+=parameter+ v+=variadicParam* # paramFuncCall
    | n=namespace LPAREN RPAREN # unitFuncCall
    | n=IDENT MEM_VAL s=IDENT LPAREN RPAREN # extUnitCall
    | n=IDENT MEM_VAL s=IDENT p+=parameter+ v+=variadicParam* # extFuncCall
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
    | (ADDRESS_OF | ADD | SUB | K_SIZEOF | EXCLAIM | K_NOT) e=expression # unaryPrefixExpr
    | e1=expression (MUL | DIV| MOD) e2=expression # mulLikeExpr
    | e1=expression (ADD | SUB) e2=expression # addLikeExpr
    | e1=expression (SHIFT_LEFT | SHIFT_RIGHT) e2=expression # shiftLikeExpr
    | e1=expression (LESS_THAN | GREATER_THAN | LESS_EQUAL | GREATER_EQUAL) e2=expression # relLikeExpr
    | e1=expression (EQUAL | NOT_EQUAL) e2=expression # eqlLikeExpr
    | e1=expression BIT_AND e2=expression # bitAndExpr
    | e1=expression BIT_XOR e2=expression # bitXorExpr
    | e1=expression BIT_OR e2=expression # bitOrExpr
    | e1=expression K_AND e2=expression # logAndExpr
    | e1=expression K_OR e2=expression # logOrExpr
    | assignVar # assignVarExpr
    ;

lesserExpr
    : (L_INT | L_DOUBLE | L_CHAR | L_STRING | L_TRUE | L_FALSE | L_NULL) # primExpr
    | (funcRef | funcCall) # refExpr
    | K_SIZEOF t=typeId # typeSizeExpr
    | LPAREN e=expression RPAREN # braceExpr
    | dereference # derefExpr
    ;

dereference
    : LSQUARE off=expression? e=expression RSQUARE
    ;

funcRef
    : n=namespace t+=memberAccess* s+=funcSel*
    ;

funcSel
    : COLON s=IDENT
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