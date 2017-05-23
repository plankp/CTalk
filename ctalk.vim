" Vim syntax file
" Language: CTalk
" Filenames: *.ct
" Maintainer: Paul Teng <plankp@outlook.com>
" Latest Revision: 23 May 2017

if exists("b:current_syntax")
	finish
endif

" Keywords
syn keyword ctalkKeywords export function do end goto export internal hidden return break continue
syn keyword ctalkInclude import extern
syn keyword ctalkStruct struct union module
syn keyword ctalkDefine macro
syn keyword ctalkTypedef typename
syn keyword ctalkCond if elseif else switch
syn keyword ctalkRepeat for
syn keyword ctalkLabel case default
syn keyword ctalkOp sizeof and not or as
syn keyword ctalkType bool any_t size_t char short int long float double unsigned signed imaginary complex void
syn keyword ctalkStoClass const volatile

syn match ctalkLabel "$\w\+"

syn match ctalkIdent "\w\+"

syn match ctalkSpecial display contained "\\\(.\|$\)"
syn match ctalkSpecial display contained "\\\(u\x\{4}\|U\x\{8}\)"

syn region ctalkString start=+"+ skip=+\\\\\|\\"+ end=+"+ contains=ctalkSpecial extend
syn region ctalkString start=+<+ skip=+\\\\+ end=+>+ contains=ctalkSpecial extend

syn match ctalkChar "'[^\\]'"
syn match ctalkChar "'[^']*'" contains=ctalkSpecial

" Boolean literals
syn keyword ctalkLBool true false null

" Integer with - + or nothing in front
syn match ctalkNumber '\d\+'
syn match ctalkNumber '[-+]\d\+'

" Floating point number with decimal no E or e 
syn match ctalkNumber '[-+]\d\+\.\d*'

syn region ctalkBlock start="do" end="end" fold transparent contains=ALL

syn keyword ctalkTodo contained TODO FIXME XXX NOTE
syn match ctalkSComment "#.*$" contains=ctalkTodo
syn region ctalkMComment start="#{" end="}" contains=ctalkTodo,ctalkMComment

let b:current_syntax = "ctalk"

hi def link ctalkType Type
hi def link ctalkStoClass StorageClass
hi def link ctalkOp Operator
hi def link ctalkIdent Identifier
hi def link ctalkCond Conditional
hi def link ctalkRepeat Repeat
hi def link ctalkLabel Label
hi def link ctalkStruct Structure
hi def link ctalkTypedef Typedef
hi def link ctalkInclude Include
hi def link ctalkTodo Todo
hi def link ctalkSComment Comment
hi def link ctalkMComment Comment
hi def link ctalkDefine Define
hi def link ctalkKeywords Keyword
hi def link ctalkNumber Number
hi def link ctalkLBool Boolean
hi def link ctalkString String
hi def link ctalkChar Character
