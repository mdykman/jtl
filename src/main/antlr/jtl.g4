/** Taken from "The Definitive ANTLR 4 Reference" by Terence Parr */

// Derived from http://json.org

grammar jtl;

import json;

options {
    language = Java;
    tokenVocab=jtllex;
}

@header {
}


jtl 
      : value EOF?
      ;

value
    : jpath 
    ;


jpath
	:  re_expr
	;

re_expr
	: tern_expr
	| re_expr '=~' string
	;
	
tern_expr 
	: or_expr
	| tern_expr '?' value ':' value
	; 
	
or_expr 
    : and_expr
    | or_expr 'or' and_expr
    | or_expr 'nor' and_expr
    ;

and_expr 
    : match_expr 
    | and_expr 'and' match_expr
    ;

match_expr
   : eq_expr
   | match_expr '==' eq_expr
   ;
   
eq_expr 
	: rel_expr
    | eq_expr '=' rel_expr
    | eq_expr '!=' rel_expr
    ;
        
rel_expr
	: add_expr
    | rel_expr '<' add_expr
    | rel_expr '>' add_expr
    | rel_expr '<=' add_expr
    | rel_expr '>=' add_expr
    ;

add_expr   
	: mul_expr
    | add_expr '+' mul_expr
    | add_expr '-' mul_expr
    ;
            
mul_expr
	: unary_expr
    | mul_expr '*' unary_expr
    | mul_expr 'div' unary_expr
    | mul_expr '%' unary_expr
    ;
            
unary_expr
	: union_expr
    | '-' unary_expr
    ; 

            
union_expr  
	: filter_path ('|' filter_path )*
    ;

filter_path
	: path
    | filter_path '{' value '}' 
    ;

path 
	: abs_path
	;

abs_path 
	: '/' rel_path
	| rel_path
	;
			
rel_path 
	: pathelement
    | rel_path '/' pathelement 
    ;
                        
pathelement 
	: pathstep
    | pathelement pathindex 
    ;
    
pathindex
	: '[' indexlist2 ']'
	;
	
indexlist2
	: indexl (',' indexl)*
	;
	
indexlist
	: indexl (',' indexl)*
	;

indexl
	: value ('..' value)?
	;

array
//  :   '[' value (',' value)* ']' 
  : '[' indexlist ']'
  |   '[' ']' // empty array
  ;

pathstep 
   : id
   | recurs
   | func
   | funcderef
   | variable
   | number
//   | jstring
   | string
   | object 
   | array
   | '(' value ')'
	| '.'
	| '..'
	| '*'
    | 'true'  
    | 'false' 
    | 'null' 
	
	;
            
recurs 
	: '**'
    | '...'
    ;


func
	: ff
	| ident '.' ff
	;
            
ff
     : ident '(' ')'
     | ident '(' value (',' value )* ')' 
     ;


funcderef
     : '$' ident '(' ')'
     | '$' ident '(' value (',' value )* ')'
     | '$' INTEGER '(' ')' 
     | '$' INTEGER '(' value (',' value )* ')'
     ;
	
variable
      : '$' ident 
      |  ident '.' '$' ident 
      | '$' INTEGER
      | '$' '@'
      | '$' '#'
      | '$' ':'
      | '$' '$'
      ;


ident 
	: ID
	;


key
	: ident
	| '!' ident
	| '$' ident
	| string
	;
	
	
id 
     : ident
     | id '.' ident
     ;
 
 
string
	: STRING
	| SSTRING
	;

