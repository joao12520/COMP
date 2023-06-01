.class Test
.super java/lang/Object

.method <init>()V
	.limit stack 100
	.limit locals 1
	aload_0
	invokespecial java/lang/Object/<init>()V
	return
.end method

.method public static main([Ljava/lang/String;)V
	.limit stack 100
	.limit locals 2
	new Test
	astore_1
	aload_1
	invokespecial Test/<init>()V
	return
.end method


