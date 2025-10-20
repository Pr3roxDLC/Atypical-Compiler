grammar Atypical;

file: imports fileMember* EOF;

fileMember: moduleDeclaration | implDeclaration | traitDeclaration | structDeclaration | methodImplementation;

//Imports
imports: importedClass*;
importedClass: IMPORT class=typeName AS alias=typeName SEMICOLON;

//Module
moduleDeclaration: MODULE typeName LBRACE moduleMemberDeclaration* RBRACE;
moduleMemberDeclaration: moduleStructDeclaration | moduleSelfImplDeclaration | moduleImplDeclaration;
moduleStructDeclaration: STRUCT LBRACE structMemberDeclaration* RBRACE;
moduleSelfImplDeclaration: IMPL LBRACE implMemberDeclaration* RBRACE; //Shorthand for implementing methods for the current module
moduleImplDeclaration: IMPL typeName LBRACE implMemberDeclaration* RBRACE; //Shorthand for implementing methods for a
                                                                           // given trait for the current module

//Struct
structDeclaration: STRUCT typeName LBRACE structMemberDeclaration* RBRACE;
structMemberDeclaration: typeName COLON memberName SEMICOLON;

//Trait
traitDeclaration: TRAIT typeName LBRACE traitMemberDeclaration* RBRACE;
traitMemberDeclaration: methodDeclaration;

//Impl
implDeclaration: IMPL itf=typeName FOR struct=typeName LBRACE implMemberDeclaration* RBRACE;
implMemberDeclaration: methodImplementation;

//Methods
methodImplementation: methodSignature LBRACE statement* RBRACE;
methodDeclaration: methodSignature SEMICOLON;

methodSignature
    : memberName LPAREN parameterList? RPAREN methodReturnTypeDeclaration?
    ;

parameterList
    : parameterDeclaration (COMMA parameterDeclaration)*
    ;

parameterDeclaration
    : typeName COLON memberName
    ;

methodReturnTypeDeclaration: COLON typeName;

//Statements
statement:
      localVariableDeclarationExpression SEMICOLON
    | asignLocalVariableStatement SEMICOLON
    | expression SEMICOLON
    | returnStatement SEMICOLON
    | ifStatement
    | whileStatement;

localVariableDeclarationExpression: typeName COLON variableName ASSIGN expression;
asignLocalVariableStatement: variableName ASSIGN expression;
returnStatement: RETURN expression?;
ifStatement: IF LPAREN expression RPAREN LBRACE statement* RBRACE elseIfStatement* elseStatement?;
elseIfStatement: ELSE IF LPAREN expression RPAREN LBRACE statement* RBRACE;
elseStatement: ELSE LBRACE statement* RBRACE;
whileStatement: WHILE LPAREN expression RPAREN LBRACE statement* RBRACE;

// =========================
// Expression (Simplified Integration Tip)
// =========================

expression
    : postfixExpression ((ADD | SUB | MUL | DIV | MOD | CMPEQ | CMPNE | CMPGT | CMPLT | ASSIGN | LOGIC_AND | LOGIC_OR) expression)?
    ;

postfixExpression
    : primary postfixOperator*
    ;

postfixOperator
    : DOT memberAccess
    | arrayAccess
    ;

memberAccess
    : memberName
    | methodInvocation
    ;

methodInvocation
    : memberName LPAREN argList? RPAREN
    ;

arrayAccess
    : LBRACK expression RBRACK
    ;

primary
    : literal
    | memberOrVariableName
    | structInitializerExpression
    | castExpression
    | parenthesesExpression
    ;

// =========================
// Other Expression Forms
// =========================

structInitializerExpression: typeName LBRACE argList? RBRACE;
parenthesesExpression: LPAREN expression RPAREN;
castExpression: LPAREN typeName RPAREN expression;
argList: expression (COMMA expression)*;

literal
    : NUMBER
    | STRING
    | NULL
    ;

  STRING
      : SINGLE_QUOTE (~['\r\n])* SINGLE_QUOTE
      | DOUBLE_QUOTE (~["\r\n])* DOUBLE_QUOTE
      ;

//Identifiers
typeName: identifier (DOT identifier)* ARRAY_TYPE*;
identifier: LETTER LETTER_OR_DIGIT*;
memberName: LETTER LETTER_OR_DIGIT*;
variableName: LETTER LETTER_OR_DIGIT*;
memberOrVariableName: LETTER LETTER_OR_DIGIT*;

//Keywords
MODULE: 'module';
STRUCT: 'struct';
TRAIT: 'trait';
IMPL: 'impl';
FOR: 'for';
IMPORT: 'import';
AS: 'as';
RETURN: 'return';
IF: 'if';
ELSE: 'else';
WHILE: 'while';
NULL: 'null';

//Reserved Chars
LBRACE: '{';
RBRACE: '}';
LPAREN: '(';
RPAREN: ')';
LBRACK: '[';
RBRACK: ']';
COLON: ':';
SEMICOLON: ';';
ARRAY_TYPE: '[]';
COMMA: ',';
DOT: '.';
SINGLE_QUOTE: '\'';
DOUBLE_QUOTE: '"';

//Operator
ADD: '+';
SUB: '-';
MUL: '*';
DIV: '/';
MOD: '%';
ASSIGN: '=';
CMPLT: '<';
CMPGT: '>';
CMPEQ: '==';
CMPNE: '!=';
LOGIC_AND: '&&';
LOGIC_OR: '||';

NOT: '!';
INC: '++';
DEC: '--';

//REGEX
NUMBER: [0-9]+;
LETTER: [a-zA-Z]+;
LETTER_OR_DIGIT: LETTER | NUMBER;

//Ignore whitespace
WS: [ \t\r\n]+ -> skip;
