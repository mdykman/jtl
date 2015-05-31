

lexer grammar jtllex;

// END_BLOCK : '}' -> popMode;

END_BLOCK : '}' -> popMode;

COMMENT : '/' '/' [^\n]* -> skip;

EMPTY_STR: '""';

SSTR : '"' ->pushMode(STR);


fragment ESC :   '\\' ([\\/bfnrt] | UNICODE) ;
fragment UNICODE : 'u' HEX HEX HEX HEX ;
fragment HEX : [0-9a-fA-F] ;

mode STR;

SS  : ~["\\$]+ 
	| ESC
	;

ESTR : '"' -> popMode;
START_BLOCK : '${' ->pushMode(DEFAULT_MODE);
//JTLESC : 
