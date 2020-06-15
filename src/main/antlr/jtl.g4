
grammar jtl;

import json;

options {
    language = Java;
}

jtl 
      : value EOF?
      ;

value
    : jpath 
    | functionReference
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
    : eq_expr 
    | and_expr 'and' eq_expr
    ;

eq_expr 
	: not_expr
    | eq_expr '='  not_expr
    | eq_expr '!=' not_expr
    ;
    
    
not_expr 
    : rel_expr
    | '!' not_expr
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
   : object 
   | array
   | '(' value ')'
   | number
   | string
   | variable
   | functionCall
   | id
   | recurs
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


functionCall
	: funcprefix '(' ')'
  | funcprefix '(' value (',' value )* ')' 
  ;

funcprefix 
	: ident
	| ident '.' ident
	| INTEGER
	;
	
functionReference
     : '`'  value '`'
     | ':' funcprefix
     | ':' INTEGER
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
	| '(' value ')'
	;
	
	
id 
     : ident
     | id '.' ident
     ;
 
 
string
	: STRING
	| SSTRING
	;

