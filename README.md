# jtl / jpath


JTL is a JSON Transformation Language designed for accepting json-formatted data as input, operating on that data and returning arbitrary json data as output.

The langauge is a superset of json which allows a jpath expression to be specified anywhere that a json value would be expected.

An extensible Module System provides to JDBC, CSV datasources, caching and an embedded http client.

## requirements
	Java 8

## installation

	to create the command line tool, ensure that you have Java 8 available on the path.

`./gradlew distZip`

generates ./build/distributions/jtl-\<`version`\>.zip

 ** If you are building from behind a VPN: I have observed a certificate mismatch when accessing the well-known maven repository https://oss.jfrog.org. I have worked around this by disconnecting from the VPN and running the gradle-wrapper command again.  Once the required artifact has been retreived (gradle integration for antlr), subsequent builds may be run with the --offline switch to avoid this error. **


unzip in the directory of your choice.  You may then either
* add JTL\_HOME/bin to your PATH
* add a softlink in a directory already under your path to `JTL_HOME/bin/jtl`
* access the jtl script directly via `JTL_HOME/bin/jtl`

## library 
To use JTL as an embedded library, it may be accessed via a maven repository.

Here is an example of accessing that respository with gradle by adding the following blocks
to build.gradle.

```
repositories {
  mavenCentral()
  maven {
    url 'https://raw.github.com/mdykman/mvn-repo/master/'
  }
}


dependencies {
   compile 'org.dykman:jtl:0.9.3'
}

```

## usage
```
$ jtl --help

 $ jtl  [options ...] arg1 arg2 ...

  JTL is a language, library, tool and service for parsing, creating and transforming JSON data
  see: https://github.com/mdykman/jtl

  -h --help	print this help message and exit
  -v --version	print jtl version
  -c --config	specify a configuration file
  -i --init	specify an init script
  -x --jtl	specify a jtl file
  -d --data	specify an input json file
  -D --dir	specify base directory (default:.)
  -e --expr	evaluate an expression against input data
  -o --output	specify an output file (cli-only)
  -s --server	run in server mode (default port:7718)
  -p --port	specify a port number (default:7718) * implies --server
  -B --binding	bind network address * implies --server (default:127.0.0.1)
  -k --canon	output canonical JSON (ordered keys)
  -n --indent	specify default indent level for output (default:3)
  -q --quote	enforce quoting of all object keys (default:false)
  -a --array	parse a sequence of json entities from the input stream, assemble them into an array and process
  -b --batch	gather n items from a sequence of JSON values from the input stream, processing them as an array
  -z --null	use null input data (cli-only)

```
If no script is specified explicitly via '-x', the first argument is assumed to be the jtl script file.

If no data is specified specified explicitly via '-d', the next argument is assumed to be the json input file, unless -a or -b has been specified.

In CLI mode, all additional arguments will be passed to the script as $1, $2, ...

These invocations will all produce the same result

    $ jtl src/test/resources/group.jtl src/test/resources/generated.json
    $ jtl -x src/test/resources/group.jtl src/test/resources/generated.json
    $ jtl -d src/test/resources/generated.json src/test/resources/group.jtl 
    $ jtl -x src/test/resources/group.jtl -d src/test/resources/generated.json
    $ cat src/test/resources/generated.json | jtl -x src/test/resources/group.jtl
    $ cat src/test/resources/generated.json | jtl src/test/resources/group.jtl
    $ jtl src/test/resources/group.jtl < src/test/resources/generated.json

After options and files have been processed, any remaining arguments are passed to the program script.

###  examples
	 # evaluate an jtl script with null input data
    $ jtl -z  test.jtl

	 # evaluate an ad-hoc expression against an input file
    $ jtl -e "/people/count()"  src/test/resources/generated.json

	
	 # launch a server bound to all interfaces based at a specified directory
    $ jtl --binding 0.0.0.0 -D src/test/resources

## JSON
The JSON parser provided by JTL supports a somewhat extended syntax
* object keys do not need to be quoted if they contain no spaces and do not begin with non-letter character 
* strings may be single-quoted or double-quoted
* line comments are allowed using `//`

The formal definition of the values allowed in an unquoted key is given below. It is based on the definition of a valid identifier as specified in Java 8.

```
JavaLetter
	:	[a-zA-Z_] // these are the "java letters" below 0xFF
	|	// covers all characters above 0xFF which are not a surrogate
		~[\u0000-\u00FF\uD800-\uDBFF]
		{Character.isJavaIdentifierStart(_input.LA(-1))}?
	|	// covers UTF-16 surrogate pairs encodings for U+10000 to U+10FFFF
		[\uD800-\uDBFF] [\uDC00-\uDFFF]
		{Character.isJavaIdentifierStart(Character.toCodePoint((char)_input.LA(-2), (char)_input.LA(-1)))}?
	;

JavaLetterOrDigit
	: JavaLetter
	| [0-9] 
	;
```

In CLI mode, the default output behaviour is to avoid quoting keys if they conform to item 1, listed above.  This behaviour may be over-ridden with the command line switch `-q`

In server mode, keys are always quoted.

## data types 
All data is JSON which is to say objects, arrays and scalars. Any reference to a value in this document could be refering to any of these unless specified otherwise.

### objects 
Objects are containers mapping string keys to JSON values.  Values may be of any JSON type or jpath expression

### array 
Arrays are conainers which hold a continuous sequence of JSON values.  Values may be of any JSON type or jpath expression

### scalars 
Scalars can be of severals types
* string
* number (int or real)
* boolean
* null

## literal notation 
The notation of literal data in JTL is exactly that of JSON, of which JTL is a superset. Any valid JSON notation should be a valid notation
for expressing literal data in JTL.  Any inconsistency which is found should be reported to the author as a bug.
 	
## truth values 
All JSON values have a well-defined truth value when evaluated in boolean contexts.

Evaluate as false:
* null
* false (boolean)
* 0 (int or real)
* empty string
* empty array
* empty object

All other values evaluate as true

## jpath
Jpath is a notation for navigating and manipulating json data, analogous to XPath and it's relationship with XML. 

Like XPath, jpath expressions are evaluated from left to right against implicit context data. 

Unlike XPath expressions, jpath expressions are not string-embedded allowing path components to be freely intermixed with literal data expressions. For the sake of convenience, the term __jpath__ will be used when refering to path expressions and the term __jtl__ wil be used when refering to overall program structure, but the notation is recursive.

__data.json__
```
{
	domain:"org.dykman",
	author: "michael",
	year: 2015
}
```

```
$ jtl --expr "domain" a.jtl
"dykman.org"
```
```
$ jtl --expr "author" a.jtl
"michael"
```

### path elements

symbol|meaning
------|-------
/     | path seperator. When used as a prefix, denote an absolute path into the input data
\*    | children, return the elements of an array or the values of an object
\*\*   | children, recursive
.    | self
..    | parent - if the context data is a child of an array or object, the parent is returned
...    | parent, recursive
_identifier_ | select named element from object
_identifier_() | invoke function (user or builtin)
$_identifier_ | define or reference named variable
!_identifier_ | imperitive: similar to above, but with geedy resolution. see Context Object
_jsonexpr_ | any valid json expression


### dereferencing  \[ ... \]
Any jpath element may optionally be suffixed with the dereference operator, \[ ... \].


NOTE: when square bracket notation is used without a prefix, it is direct array notation. 
The dereferening behaviour only applies when it is suffixed to another element within the same path segment.
```
# select a single element from the array denoted by 'sales'
sales[3]
#select an array of several elements
sales[1,3]
#select an array from a range
sales[1..3]
#select mix ranges and single items
sales[1..3,5]
# create a an array of numbers from 1 to 5 (inclusive) ignoring data in 'sales'
sales/[1..5]
```

When the data on the left is an array and the contents of the dereference operator is numeric, those numbers are treated as indices by which values are selected from that array.


When data on the left is an object and the contents of the dereference operator are strings, those string are interpreted as keys by which values are selected from that object.

### mixing json and jpath expressions
Json object and arrays may contain embedded jpath expressions anywhere a json value might be used.

```
{
	name: details/(first + ' ' + last)
}


details/[first,last,dob,17]
```

### special symbols
There are a few _special symbols_ in JTL where the meaning is context-dependant.

SYMBOL | CLI | HTTP | FUNCTION
-------|-----|------|---------
$\_    | script input data | script input data   | function input data
$0     | path of current script|path of current script|name of current function
$1..$n | command-line arguments|http path arguments|function arguments
$@     | array of command-line arguments|array of http path arguments|array of function arguments

### named contexts
Using the builtin `module` or `import` functions in an init block (see Init Block) may load additional
functions and/or variables into a named context. They can be accessed via:
* variables `identifier . $identifier`
* functions `identifier . identifier ( )`

examples
```
foo.$myvar
bar.myfunc()
```

### reserved words
As JTL is a superset of JSON, it inherits 3 reserved words from  that language:

	true, false, null

JTL additionally reserves

	and, or, not, nand, nor, div

## regular expressions 
The general syntax for a regex match expression is:
	
`text =~ regexp`

Note the =~ operator which is only legal with a literal string to the right of it.


The regular expression itself must be represented as a literal string:

`. =~ "[a-z]*"`
	
Dynamic regular expressions are on the road map, not at high priority

Regular expression syntax is as in https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html


On match, the expression will produce an array containing at least 1 item: the matched text.  If parentheses have been used to define sub matches, those submatches will appears as subsequent elements of the array.

```
"this is my text string" =~ "m.*xt" -> ["my text"]
"this is my text string" =~ "th[^y]*(y..).*(ri.*)$" -> ["this is my text string","y t","ring"]
```

On no-match, the expression yields an empty array, which tests false.

`"not even close" =~ "th[^y]*(y..).*(ri.*)$" -> []`

## functions 

### general 
* module(name,config)  - import modules, usually called in `!init`
* name - a string selecting the name, must be defined in config.json
* config - an object containing module-specific config data
* import(name) - import scripts - not implmented
* error - define error handler for current scope - badly implemented
* rand(selector) - random numbers - 
* rand(count,selector)
 1 param returns 0->(p-1): `rand(20)`->17
 2 params returns array of size p1, selected by p2: `rand(4,20)` -> [3,17,0,8]
 0 selector returns random double 0 <= n < 1: `rand(0)` -> 0.583501
* switch(selector,object) - string based expression selector
 selector - string expression keyed to a member of object
 object - a map of expressions keyed by string with _ as default
 `switch("foo",{bar:"you selected bar",foo:"you selected foo",_:"you selected nothing useful"}) -> "you selected foo"`
* defined(name) - tests if a given function is defined
* call(name,...) call a named function with variable argument list
			

### external data 
* file(fn) - parse json data from a file
* url(u) - parse json data fetched via optomistic, remote get
* write(fn) - write context data to file

		
### string-oriented 
* split(sep) - returns an array of strings splitting the input on sep
* join(cnj) - expects an array of strings as input data, returning the joined string with cnj in between
* substr(start) - return the substring of 
* substr(start,length)
		
### list-oriented 
list-oriented functions operate on lists or frames equivalently

* sum() - sum all numeric values in an array
* unique() - return an array contain all the unique items found in the import array
* count() - count the items in an array
* sort/rsort() - sort items in an array
* sort/rsort(expr)
`dataset/sort()` - sort the items 'naturally' (nulls->boolean->numeric->string->object->array)
`dataset/sort(expr)` - evaluate expr against each item in the input array, sort 'naturally' with that value
* filter(expr) - returns an array of elements from the input array which expr evaluated as true
* contains(element) - return true if array contains element
* append($1, ...)  - return an array with each argument appended to the input array
`[1,3,5]/append(7,9) -> [1,3,5,7,9]`
* copy(n) - returna an array containing n copies of the conext data
* each(expr) - evaluate expresssion agains each element of the context data, regardless of the expressions affinity
      

### object-oriented 
* group(expr) - apply expr to each object in an array, return an object grouping them according to that result
    __see src/test/resources/group.jtl__
* map()
* collate() - collects the data from an array of objects into a single object
* omap()
* amend(expr) - modifies objects
* amend(key,expr)
`expr` is evaluated against the object to be amended
 1 param form: expects an object result which is overlaid on the input object, creating or replacing values
 2 params form: key is used to create or replace input object memeber
* keys() - returns an array of the keys in the object
`{ foo:1,bar:2 }/keys()`->["foo","bar"]

### types 
  __boolean type test only - 0 arguments__
  
    __see src/test/resources/types.jtl__

* nil()
* value()
* object()

```
		null/nil() -> true
		1/value() -> true
		"hello"/value() -> true
		{ thing: 1 }/object() -> true
```

  __boolean type test and converter - 0/1 arguments__
with 0 args, return boolean type test result
with 1 arg, attempts to coerce to the specified type
* array() - test if array or frame
* array(expr) -> if expr evaluates to
```
 array -> return self
 frame -> coerce to array
 object or scalar -> return array of that one item
```

* number()
* number(expr) -> if expr evaluates to
* int()
* int(expr) -> if expr evaluates to
* real()
* real(expr) -> if expr evaluates to
```
 number -> return self
 string -> attempt to parse, null on failure
 boolean -> 0 on false, 1 on true
 all others -> null
```

* string() - test if string
* string(expr) -> if expr evaluates to
```
string -> return self
other -> coerce to string (valid json)
```

boolean() - tests if boolean
boolean(expr) - coerce to boolean 
