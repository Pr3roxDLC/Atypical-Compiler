grammar Atypical;

file: fileMember* EOF;

fileMember: moduleDeclaration | implDeclaration | traitDeclaration | structDeclaration | methodImplementation;


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
methodSignature: memberName LPAREN parameterDeclaration* RPAREN methodReturnTypeDeclaration?;
methodReturnTypeDeclaration: COLON typeName;
parameterDeclaration: typeName COLON memberName;

//Statements
statement: localVariableDeclarationExpression SEMICOLON
    | asignLocalVariableStatement SEMICOLON
    | expression SEMICOLON;
localVariableDeclarationExpression: typeName COLON variableName ASIGN expression;
asignLocalVariableStatement: variableName ASIGN expression;

//Expression
expression:
      unaryExpression
    | left=expression binaryExpression
    | terminalExpression
    | left= expression DOT memberAccessExpression   //didnt find a better way to do this we need this rule to allow
    | memberAccessExpression;                       //things like "abc".replace();

memberAccessExpression: primaryMemberAccess (DOT (fieldAccessExpression | methodInvocationExpression))*;
primaryMemberAccess: fieldAccessExpression | methodInvocationExpression;
fieldAccessExpression: memberName;
methodInvocationExpression: memberName LPAREN argList?  RPAREN;
argList: expression (COMMA expression)*;
binaryExpression: op=binaryOperator right=expression;
unaryExpression: unaryOperator right = expression;
terminalExpression: literal | memberOrVariableName;
literal: NUMBER;


//Operators
unaryOperator: NOT | INC | DEC;
binaryOperator: ADD | SUB | MUL | DIV | MOD | ASIGN | CMPEQ | CMPNE | CMPGT | CMPLT;


//Identifiers
typeName: identifier ('.' identifier)* ARRAY_TYPE?;
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

//Reserved Chars
LBRACE: '{';
RBRACE: '}';
LPAREN: '(';
RPAREN: ')';
COLON: ':';
SEMICOLON: ';';
ARRAY_TYPE: '[]';
COMMA: ',';
DOT: '.';

//Operator
ADD: '+';
SUB: '-';
MUL: '*';
DIV: '/';
MOD: '%';
ASIGN: '=';
CMPLT: '<';
CMPGT: '>';
CMPEQ: '==';
CMPNE: '!=';


NOT: '!';
INC: '++';
DEC: '--';


//REGEX
LETTER : [a-zA-Z]+ ;
LETTER_OR_DIGIT: LETTER | [0-9];
NUMBER : [0-9]+;

//Ignore whitespace
WS : [ \t\r\n]+ -> skip ;

