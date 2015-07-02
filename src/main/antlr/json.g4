

grammar json;

options {
    language = Java;
}


json
    :   value EOF?;

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
    : string ':' value
    | id ':' value 
    ; 
    
array
    :   '[' value (',' value)* ']' 
    |   '[' ']' // empty array
    ;
   
number
      : INTEGER 
      | FLOAT 
      ;

id 
     : ID 
     ;
    
string 
	: STRING
	| SSTRING
    ;


//lstr
//	: '"' strbod* '"'
//	;

//strbod 
//	:  ESC 
//	|  STRBIT
//	| '\''
//	;


fragment INT :   '0' | [1-9] [0-9]* ; // no leading zeros
fragment EXP :   [Ee] [+\-]? INT ; // \- since - means "range" inside [...]

fragment ESC :   '\\' ([\\/bfnrt] | UNICODE) ;
fragment UNICODE : 'u' HEX HEX HEX HEX ;
fragment HEX : [0-9a-fA-F] ;

fragment STRBOD :  (ESC | ~[\'\"\\]+);


/* 
fragment
StringCharacters
	:	StringCharacter+
	;

fragment StringCharacter
	:	~["\\]
	|	EscapeSequence
	;

fragment
OctalDigit
	:	[0-7]
	;

fragment
HexDigit
	:	[0-9a-fA-F]
	;


fragment
EscapeSequence
	:	'\\' [btnfr"'\\]
	|	OctalEscape
    |   UnicodeEscape // This is not in the spec but prevents having to preprocess the input
	;

fragment
OctalEscape
	:	'\\' OctalDigit
	|	'\\' OctalDigit OctalDigit
	|	'\\' ZeroToThree OctalDigit OctalDigit
	;

fragment
ZeroToThree
	:	[0-3]
	;

// This is not in the spec but prevents having to preprocess the input
fragment
UnicodeEscape
    :   '\\' 'u' HexDigit HexDigit HexDigit HexDigit
    ;
fragment
StringLiteral
	:	'"' StringCharacters? '"'
	;

*/
STRBIT
	: ~[\'\"\\]+
	;

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
