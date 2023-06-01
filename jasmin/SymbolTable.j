.class SymbolTable
.super Quicksort

.field public intField I
.field public boolField Z
.method <init>()V
	.limit stack 100
	.limit locals 1
	aload_0
	invokespecial Quicksort/<init>()V
	return
.end method

.method public method1()I
	.limit stack 100
	.limit locals 3
	iconst_0
	istore_1
	iconst_1
	istore_2
	iconst_0
	ireturn
.end method

.method public method2(IZ)Z
	.limit stack 100
	.limit locals 3
	iload_2
	ireturn
.end method

.method public static main([Ljava/lang/String;)V
	.limit stack 100
	.limit locals 1
	return
.end method


