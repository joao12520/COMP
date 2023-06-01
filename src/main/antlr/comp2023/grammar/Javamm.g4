grammar Javamm;

@header {
    package pt.up.fe.comp2023;
}

INTEGER : [0] | [1-9][0-9]* ;
ID : [a-zA-Z_$][a-zA-Z_0-9$]* ;
COMMENT : '/*' .*? '*/' -> skip ;
LINE_COMMENT: '//' ~[\r\n]* -> skip ;

WS : [ \t\n\r\f]+ -> skip ;

program
    : importDeclaration* classDeclaration EOF
    ;

classDeclaration
    : 'class' value=ID ('extends' superClassName=ID )? '{' (varDeclaration)* (methodDeclaration)* '}' #ClassStmt
    ;

varDeclaration
    : type value=ID ';' #VarStmt
    ;

methodDeclaration locals[boolean isStatic=false]
    : ('public')? ('static' {$isStatic=true;})? methodSignature '(' ( parameter (',' parameter)*)? ')' '{'
    (varDeclaration)* (statement)* returnStatement '}' #MethodStmt
    | ('public')? 'static' 'void' 'main' '(' 'String' '[' ']' value=ID ')' '{' (
    varDeclaration)* (statement)* '}' #MainMethodStmt
    ;

returnStatement
 : 'return' expression ';' #ReturnStmt
 ;

type locals[boolean isArray=false]
    : value=('int' | 'boolean' | 'String') ('[' ']' {$isArray=true;})? #BaseType
    | value=ID #IdentificationType
    ;

parameter
    : type value=ID
    ;

methodSignature
    : type value=ID
    ;

statement
    : '{' (statement)* '}' #Block
    | 'if' '(' expression ')' statement 'else' statement #IfStmt
    | 'while' '(' expression ')' statement #WhileStmt
    | expression ';' #ExprStmt
    | value=ID '=' expression ';' #AssignStmt
    | value=ID '[' expression ']' '=' expression ';' #ArrayAssignStmt
    | value=ID '=' INTEGER ';' #AssignLiteralStmt
    ;

expression
    : '(' expression ')' #Parenthesis
    | '!' expression #Negation
    | expression op=('*' | '/') expression #BinaryOp
    | expression op=('+' | '-') expression #BinaryOp
    | expression op=('&&' | '<') expression #BinaryOp
    | value=INTEGER #Integer
    | value=ID #Identifier
    | value='true' #Boolean
    | value='false' #Boolean
    | value='this'  #Object
    | value='new' type '[' expression ']' #NewArray
    | 'new' value=ID '(' ')' #NewObject
    | expression (type)? '[' (expression)* ']' #ArrayAccessExpr
    | expression '.' value=ID'(' (expression (',' expression)*)? ')' #MethodCallExpr
    | value=ID ('.' expression)* #MethodCallExpr
    ;

importDeclaration
    : 'import' names+=ID ('.' names+=ID)* ';' #ImportStmt
    ;
