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


//ID   : [a-zA-Z_][a-zA-Z0-9_]* ;


//Identifier
ID
	:	JavaLetter JavaLetterOrDigit*
	;

fragment
JavaLetter
	:	[a-zA-Z_] // these are the "java letters" below 0xFF
	|	// covers all characters above 0xFF which are not a surrogate
		~[\u0000-\u00FF\uD800-\uDBFF]
		{Character.isJavaIdentifierStart(_input.LA(-1))}?
	|	// covers UTF-16 surrogate pairs encodings for U+10000 to U+10FFFF
		[\uD800-\uDBFF] [\uDC00-\uDFFF]
		{Character.isJavaIdentifierStart(Character.toCodePoint((char)_input.LA(-2), (char)_input.LA(-1)))}?
	;

fragment
JavaLetterOrDigit
	:	[a-zA-Z0-9_] // these are the "java letters or digits" below 0xFF
	|	// covers all characters above 0xFF which are not a surrogate
		~[\u0000-\u00FF\uD800-\uDBFF]
		{Character.isJavaIdentifierPart(_input.LA(-1))}?
	|	// covers UTF-16 surrogate pairs encodings for U+10000 to U+10FFFF
		[\uD800-\uDBFF] [\uDC00-\uDFFF]
		{Character.isJavaIdentifierPart(Character.toCodePoint((char)_input.LA(-2), (char)_input.LA(-1)))}?
	;


        
WS  :   [ \t\n\r\f]+ -> skip ;

COMMENT : '/' '/' .*? '\n' -> skip;