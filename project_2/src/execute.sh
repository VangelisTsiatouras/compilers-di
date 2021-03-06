#!/bin/bash

### CORRECT
printf "\n-----CORRECT FILES-----\n"
java Main ../inputs/BinaryTree.java ../inputs/BubbleSort.java ../inputs/Factorial.java ../inputs/LinearSearch.java ../inputs/LinkedList.java ../inputs/MoreThan4.java ../inputs/QuickSort.java ../inputs/TreeVisitor.java 

### ERROR
printf "\n-----ERROR FILES-----\n"
java Main ../inputs/BubbleSort-error.java ../inputs/Factorial-error.java ../inputs/LinearSearch-error.java ../inputs/LinkedList-error.java ../inputs/MoreThan4-error.java ../inputs/QuickSort-error.java ../inputs/TreeVisitor-error.java 

### CORRECT
printf "\n-----CORRECT FILES-----\n"
java Main ../inputs/minijava-extra/Add.java ../inputs/minijava-extra/ArrayTest.java ../inputs/minijava-extra/CallFromSuper.java ../inputs/minijava-extra/Classes.java ../inputs/minijava-extra/DerivedCall.java ../inputs/minijava-extra/Example1.java ../inputs/minijava-extra/FieldAndClassConflict.java ../inputs/minijava-extra/Main.java ../inputs/minijava-extra/ManyClasses.java ../inputs/minijava-extra/OutOfBounds1.java ../inputs/minijava-extra/Overload2.java ../inputs/minijava-extra/ShadowBaseField.java ../inputs/minijava-extra/ShadowField.java ../inputs/minijava-extra/test06.java ../inputs/minijava-extra/test07.java ../inputs/minijava-extra/test15.java ../inputs/minijava-extra/test17.java ../inputs/minijava-extra/test20.java ../inputs/minijava-extra/test62.java ../inputs/minijava-extra/test73.java ../inputs/minijava-extra/test82.java ../inputs/minijava-extra/test93.java ../inputs/minijava-extra/test99.java

## ERROR
printf "\n-----ERROR FILES-----\n"
java Main ../inputs/minijava-error-extra/BadAssign.java ../inputs/minijava-error-extra/BadAssign2.java ../inputs/minijava-error-extra/Classes-error.java ../inputs/minijava-error-extra/DoubleDeclaration1.java ../inputs/minijava-error-extra/DoubleDeclaration4.java ../inputs/minijava-error-extra/DoubleDeclaration6.java ../inputs/minijava-error-extra/NoMatchingMethod.java ../inputs/minijava-error-extra/NoMethod.java ../inputs/minijava-error-extra/Overload1.java ../inputs/minijava-error-extra/test18.java ../inputs/minijava-error-extra/test21.java ../inputs/minijava-error-extra/test35.java ../inputs/minijava-error-extra/test52.java ../inputs/minijava-error-extra/test68.java ../inputs/minijava-error-extra/UseArgs.java
