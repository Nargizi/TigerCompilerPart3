grammar IR;


program: START_PROGRAM ID static_int_list static_float_list function* END_PROGRAM ID;
START_PROGRAM: 'start_program';
END_PROGRAM: 'end_program';

static_int_list: STATIC_INT_LIST COLON (var_dec (COMMA var_dec)*)?;
static_float_list: STATIC_FLOAT_LIST COLON (var_dec (COMMA var_dec)*)?;

LB: '[';
RB: ']';

var_dec: ID (LB INT RB)?;

STATIC_INT_LIST: 'static-int-list';
STATIC_FLOAT_LIST: 'static-float-list';

function: START_FUNC ID body END_FUNC ID;
START_FUNC: 'start_function';
END_FUNC: 'end_function';

// TODO: arrays
body: type_dec int_list float_list operators*;

type_dec: ID ID LP args_list RP;

LP: '(';
RP: ')';

args_list: (ID ID (COMMA ID ID)*)?;

int_list: INT_LIST COLON (var_dec (COMMA var_dec)*)?;
float_list: FLOAT_LIST COLON (var_dec (COMMA var_dec)*)?;

INT_LIST: 'int-list';
FLOAT_LIST: 'float-list';

operators: goto_branch_operator | cond_branch_operators | return_operators |
 binary_operators | func_calls | array_operators | assignment_operator | label;


goto_branch_operator: GOTO COMMA ID COMMA COMMA;
cond_branch_operators: (BNE | BEQ | BLE | BLT | BGE | BGT) COMMA alnum COMMA alnum COMMA ID;

GOTO: 'goto';
BNE: 'brneq';
BEQ: 'breq';
BLE: 'brleq';
BLT: 'brlt';
BGE: 'brgeq';
BGT: 'brgt';

return_operators: RETURN COMMA alnum? COMMA COMMA;

RETURN: 'return';

binary_operators: (ADD | SUB | MUL | DIV | AND | OR) COMMA alnum COMMA alnum COMMA ID;

ADD: 'add';
SUB: 'sub';
MUL: 'mult';
DIV: 'div';
AND: 'and';
OR: 'or';

func_calls: call | callr;
call: CALL COMMA ID (COMMA alnum)*;
callr: CALLR COMMA ID COMMA ID (COMMA alnum)*;

CALL: 'call';
CALLR: 'callr';

array_operators: array_load | array_store;
array_load: ARRAY_LOAD COMMA ID COMMA ID COMMA alnum;
array_store: ARRAY_STORE COMMA ID COMMA alnum COMMA alnum;

ARRAY_STORE: 'array_store';
ARRAY_LOAD: 'array_load';

assignment_operator: ASSIGN COMMA ID COMMA alnum COMMA alnum?;

ASSIGN: 'assign';

label: ID COLON;

COLON: ':';

ID: [_a-zA-Z][_0-9a-zA-Z]*;
INT: [0-9]+;
FLOAT: INT '.' INT;

WHITESPACE: [ \t\n] -> skip;
alnum: ID | INT | FLOAT;
COMMA: ',';