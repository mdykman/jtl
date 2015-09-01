# jtl / jpath


JTL is a JSON Transformation Language designed for accepting json-formatted data as input, operating on thata data and returning arbitrary json data as output.

The langauge is a superset of json which allows a jpath expression to be specified anywhere a json value would be expected.

## installation

initialize with gradle..   details, details



## jpath
Jpath is a notation for navigating and manipulating json data, analogous to XPath and it's relationship with XML. Unlike XPath expressions, jpath is not string-embedded 
allowing path components to be freely intermixed with literal data expressions.

## data types 
All data is JSON which is to say objects, arrays and scalars. Any reference to a value in this document could be refering to any of these unless specified otherwise.

### objects 
Objects are conainers mapping string keys to JSON values.  Values may be of any JSON type

### array 
Arrays are conainers which hold a continuous sequence of JSON values.  Values may be of any type

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
