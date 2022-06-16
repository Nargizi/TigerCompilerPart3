grammar IR;

ID: [a-zA-Z][0-9a-zA-Z_]*;
NUM: [0-9]+;
WHITESPACE: [ \t\n] -> skip;
alnum: ID | NUM;
COMMA: ',';

program: START_PROGRAM ID static_int_list static_float_list function* END_PROGRAM ID;
START_PROGRAM: 'start_program';
END_PROGRAM: 'end_program';

static_int_list: STATIC_INT_LIST SEMICOLON (ID (COMMA ID)*)?;
static_float_list: STATIC_FLOAT_LIST SEMICOLON (ID (COMMA ID)*)?;

STATIC_INT_LIST: 'static-int-list';
STATIC_FLOAT_LIST: 'static-float-list';

function: START_FUNC ID body END_FUNC ID;
START_FUNC: 'start_function';
END_FUNC: 'end_function';

body: type_dec int_list float_list operators*;

type_dec: ID ID LP RP;

LP: '(';
RP: ')';

int_list: INT_LIST SEMICOLON (ID (COMMA ID)*)?;
float_list: FLOAT_LIST SEMICOLON (ID (COMMA ID)*)?;

INT_LIST: 'int-list';
FLOAT_LIST: 'float-list';

operators: branch_operators | return_operators | branch_operators |
 binary_operators | func_calls | array_operators | assignment_operator | label;



branch_operators: (GOTO | BNE | BEQ | BLE | BLT | BGE | BGT) COMMA alnum COMMA alnum COMMA ID;

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

func_calls: (CALL | CALLR) (COMMA alnum)*;

CALL: 'call';
CALLR: 'callr';

array_operators: ARRAY_LOAD COMMA ID COMMA alnum COMMA alnum | ARRAY_STORE ID COMMA ID alnum;

ARRAY_STORE: 'array_store';
ARRAY_LOAD: 'array_load';


assignment_operator: ASSIGN COMMA ID COMMA alnum COMMA alnum?;

ASSIGN: 'assign';

label: ID SEMICOLON;

SEMICOLON: ':';