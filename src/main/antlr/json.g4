grammar json;

options {
    language = Java;
}


json
    :   value EOF?
    ;

jsonseq
	: json+
	;
	
value
    : object 
    | array
    | number
    | string
    | 'true'  
    | 'false' 
    | 'null' 
    ;

object
    : '{' pair  (',' pair )* '}' 
    |   '{' '}' // empty object
    ;
    
pair
    : key ':' value
    ; 

key 
	: ID
	| string
	;
	
array
    :   '[' value (',' value)* ']' 
    |   '[' ']' // empty array
    ;
 
  
number
 : '-' ? pnum
 ;
 
pnum
      : INTEGER 
      | FLOAT 
      ;

    
string 
	: STRING
	| SSTRING
    ;


fragment INT :   '0' | [1-9] [0-9]* ; // no leading zeros
fragment EXP :   [Ee] [+\-]? INT ; // \- since - means "range" inside [...]

fragment ESC :   '\\' ([\\/bfnrt"] | UNICODE) ;
fragment UNICODE : 'u' HEX HEX HEX HEX ;
fragment HEX : [0-9a-fA-F] ;

fragment STRBOD :  (ESC | ~[\'\"\\]+);
INTEGER
    :   '-'? INT                 // -3, 45
    |   '-'? INT EXP             // 7e6
    ;
FLOAT 
    :   '-'? INT '.' [0-9]+ EXP? // 1.35, 1.35E-9, 0.3, -4.5
    ;
STRING : '"' ('\'' | STRBOD)* '"';
SSTRING : '\'' ('"' | STRBOD)* '\'';


ID   : [a-zA-Z_][a-zA-Z0-9_]* ;
        
WS  :   [ \t\n\r\f]+ -> skip ;

COMMENT : '/' '/' .*? '\n' -> skip;