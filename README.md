This repo contains the Annotated and unannotated version of code which force a file’s CRC to any value
i.e. on https://www.nayuki.io/page/forcing-a-files-crc-to-any-value

To compile from terminal, following command needs to be passed :

javac -processor signedness forcecrc32.java

In this case study I learnt that signedness checker is weak in some classes and needs enhancements such as:-

1.)java.lang.Integer- reverse function

3.)java.io.RandomAccessFile

and I think many more in the future too.

When you will compile this file using signedness checker these errors will come-
```
java:102: error: [comparison.unsignedlhs] comparison has an unsigned LHS
			if (offset + 4 > raf.length())
			           ^
java:114: error: [operation.mixed.unsignedrhs] MINUS operation on signed LHS and unsigned RHS values
      delta = (int)multiplyMod(reciprocalMod(powMod(2, (raf.length() - offset) * 8)), delta & 0xFFFFFFFFL);
			                                                               ^
java:114: error: [argument.type.incompatible] incompatible types in argument.
      delta = (int)multiplyMod(reciprocalMod(powMod(2, (raf.length() - offset) * 8)), delta & 0xFFFFFFFFL);
			                                                                         ^
  found   : @UnknownSignedness long
  required: @Unsigned long
java:118: error: [argument.type.incompatible] incompatible types in argument.
	    raf.seek(offset);
			         ^
  found   : @Unsigned long
  required: @Signed long
java:123: error: [argument.type.incompatible] incompatible types in argument.
	    raf.readFully(bytes4);
			              ^
  found   : @Unsigned byte @UnknownSignedness []
  required: @Signed byte @UnknownSignedness []
java:128: error: [argument.type.incompatible] incompatible types in argument.
			raf.seek(offset);
			         ^
  found   : @Unsigned long
  required: @Signed long
```
These errors shows the weakness in signedness checker.

Due to this errors came and in someplaces I had to @SuppressWarnings.
I would not have found these problems in signedness checker without doing this case study.
Will further analyze this checker where it needs more enhancements.
