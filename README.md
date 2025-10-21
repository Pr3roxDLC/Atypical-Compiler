# Atypical-Compiler
## A proof of concept programming language
Atypical is a proof of concept language that brings rust-like trait and implementations to the standard JVM.
## Features
- Transform rust-like ```struct/trait/impl``` into JVM readeable classes.
- Basic Integer Arithmatics
- Method Invocations / Field Access in expressions
- Class casting
## TODOS
- instanceof/implements for traits
- Operator functions (c++ style defining behavior for arithmatic operators)


## Composite Types / Traits Implementation
### Structs
```
struct C = A | B;
```
=>
```
class C { 
  A a;
  B b;

  boolean instanceOf(Class<?> clazz);
  A a();
  B b();
}
```

### Traits
```
trait X = Y + Z;
```
=>
```
interface X extends Y,Z { 
  
}
```
```
impl X for A {
  
}
```
=>
```
class X$A implements X {}
```
