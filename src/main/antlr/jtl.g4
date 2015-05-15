/** Taken from "The Definitive ANTLR 4 Reference" by Terence Parr */

// Derived from http://json.org

grammar jtl;

tokens {S1CONTENT, S2CONTENT}

options {
    language = Java;
}

@header {
import java.util.*;
import java.util.concurrent.*;
import com.google.common.util.concurrent.*;
import static com.google.common.util.concurrent.Futures.*;
import org.dykman.jtl.core.*;
}

@body {
	JSON getContext() { return null; }
}

jtl returns [ ListenableFuture<JSON> jval ]
      : json EOF 
      ;

json returns [ ListenableFuture<JSON> jval ]
    :   value 
    ; 

object returns [ ListenableFuture<JSON> jval ]
    : '{' pair  (',' pair )* '}' 
    |   '{' '}' // empty object
    ;
    
pair returns [ListenableFuture<Duo<String,JSON>> pp]
    : string ':' value
    | id ':' value 
    ; 
    
array returns [ ListenableFuture<JSON> jval ]
    :   '[' value (',' value)* ']' 
    |   '[' ']' // empty array
    ;

value returns [ ListenableFuture<JSON> jval ]
    : object 
    | array
    | jpath 
/*
    |   b=func 
    |   c=variable 
    |   ss=string  
    |   n=number 
    |   'true'  
    |   'false' 
    |   'null' 
    
    */
    ;


   
s_expr returns [  ListenableFuture<JSON> jval  ]
       : '$' '{' json '}' 
       ;

jpath returns [ ListenableFuture<JSON> jval ]
	:  or_expr
    | '(' jpath ')';


or_expr returns [ ListenableFuture<JSON> jval ]
    : and_expr
    | or_expr 'or' and_expr;

and_expr returns [ ListenableFuture<JSON> jval ]
    : eq_expr 
    | and_expr 'and' eq_expr;

eq_expr  returns [ ListenableFuture<JSON> jval ]    
	: rel_expr
    | eq_expr '=' rel_expr
    | eq_expr '!=' rel_expr;
        
rel_expr returns [ ListenableFuture<JSON> jval ]   
	: add_expr
    | rel_expr '<' add_expr
    | rel_expr '>' add_expr
    | rel_expr '<=' add_expr
    | rel_expr '>=' add_expr;

add_expr   returns [ ListenableFuture<JSON> jval ]   
	: mul_expr
    | add_expr '+' mul_expr
    | add_expr '-' mul_expr;
            
mul_expr   returns [ ListenableFuture<JSON> jval ]
	: unary_expr
    | mul_expr '*' unary_expr
    | mul_expr 'div' unary_expr
    | mul_expr '%' unary_expr;
            
unary_expr  returns [ ListenableFuture<JSON> jval ]
	: union_expr
    | '-' unary_expr; 
            
union_expr returns [ ListenableFuture<JSON> jval ]  
	: filter_path
    | union_expr '|' filter_path;

filter_path returns [ ListenableFuture<JSON> jval ]
	: path
    | filter_path '{' jpath '}' ;

path  returns [ ListenableFuture<JSON> jval ]
	: abs_path;

abs_path returns [ ListenableFuture<JSON> jval ]
	: '/' rel_path
	| rel_path;
			
rel_path  returns [ ListenableFuture<JSON> jval ]
	: pathelement
    | rel_path '/' pathelement ;
                        
pathelement returns [ ListenableFuture<JSON> jval ]
	: pathstep
    | pathelement '[' jpath ']' ;

pathstep returns [ ListenableFuture<JSON> jval ]    
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
	| string;
            
recurs returns [ ListenableFuture<JSON> jval ]
	: '**'
    | '...';
            

func returns [ ListenableFuture<JSON> jval ]
     : ID '(' ')'
     | ID '(' json (',' json )* ')' 
     ;

variable returns [ ListenableFuture<JSON> jval ]
      : '$' i=ID 
      | '$' i=INTEGER
      ;

number returns [ Number num]
      : INTEGER 
      | FLOAT 
      ;

id returns [ String str ]
     : ID 
     ;
     
string returns [String str]
        : STRING
  //     : '"' ss1=s1 '"'
//       | '\'' t=S2CONTENT '\'' 
       ;

//////////////////%%
INTEGER
    :   '-'? INT                 // -3, 45
    |   '-'? INT EXP 
    ;

FLOAT 
    :   '-'? INT '.' [0-9]+ EXP? // 1.35, 1.35E-9, 0.3, -4.5
    ;

fragment INT :   '0' | [1-9] [0-9]* ; // no leading zeros
fragment EXP :   [Ee] [+\-]? INT ; // \- since - means "range" inside [...]

fragment ESC :   '\\' ([\\/bfnrt] | UNICODE) ;
fragment UNICODE : 'u' HEX HEX HEX HEX ;
fragment HEX : [0-9a-fA-F] ;


STRING : '"' (ESC | ~[\"\\]+)* '"';
ID   : [a-zA-Z_][a-zA-Z0-9_]* ;        
WS  :   [ \t\n\r]+ -> skip ;
