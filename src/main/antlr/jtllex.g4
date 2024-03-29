

lexer grammar jtllex;

END_BLOCK : '}' -> popMode;

COMMENT : '/' '/' [^\n]* -> skip;

fragment EMPTY_STR: '""';

fragment DD: '$' '$';
fragment DSTRBOD :  (DD | ESC | ~[$\'\"\\]+);

SSTRM : '"""' -> pushMode(MULTI);



fragment ESC :   '\\' ([\\/bfnrt] | UNICODE) ;
fragment UNICODE : 'u' HEX HEX HEX HEX ;
fragment HEX : [0-9a-fA-F] ;

mode MULTI;
EMULTI : '"""' -> popMode;
SSM  : ~["\\$]+ 
	| ESC
	| '"'
	;

START_BLOCKM : '${' ->pushMode(DEFAULT_MODE);



mode MSTR;

ESTR : '"' -> popMode;

fragment
SS  : ~["\\$\n]+ 
	| ESC
	;

START_BLOCK : '${' ->pushMode(DEFAULT_MODE);
