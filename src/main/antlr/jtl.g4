/** Taken from "The Definitive ANTLR 4 Reference" by Terence Parr */

// Derived from http://json.org

grammar jtl;

import json;


//tokens {S1CONTENT, S2CONTENT}

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
    : object 
    | array
    | jpath 
    | '(' value ')'
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
    ;

and_expr 
    : eq_expr 
    | and_expr 'and' eq_expr
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
	: '[' indexlist ']'
	;
	
indexlist
	: indexl
	| indexlist ',' indexl
	;

indexl
	: value
	| value '..' value
	;
	
pathstep 
	: '.'
	| '..'
	| '*'
    | 'true'  
    | 'false' 
    | 'null' 
	| id
	| recurs
	| func
	| variable
	| number
	| jstring
	;
            
recurs 
	: '**'
    | '...'
    ;
            
func
     : ID '(' ')'
     | ID '(' value (',' value )* ')' 
     ;

variable
      : '$' ID 
      | '$' INTEGER
      | '!' ID 
      ;

number
      : INTEGER 
      | FLOAT 
      ;

ident 
	: ID
	;
	
id 
     : ident
     | id '.' ident
     ;
 
string
	: STRING
	| SSTRING
	;

jstring : SSTR strc ESTR
     | string
     ;
     
strc : SS
	| START_BLOCK jtl END_BLOCK
	| strc strc
	; 
