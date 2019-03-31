/* 
 * CRC-32 forcer (Java)
 * 
 * Copyright (c) 2018 Project Nayuki
 * https://www.nayuki.io/page/forcing-a-files-crc-to-any-value
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program (see COPYING.txt).
 * If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Objects;
import org.checkerframework.checker.signedness.qual.*;


public final class forcecrc32 {
	
	/*---- Main application ----*/
	
	public static void main(String[] args) {
		String errmsg = submain(args);
		if (errmsg != null) {
			System.err.println(errmsg);
			System.exit(1);
		}
	}
	
	
	private static String submain(String[] args) {
		// Handle arguments
		@Unsigned long offset;
		@Unsigned int newCrc;
		if (args.length != 3)
			return "Usage: java forcecrc32 FileName ByteOffset NewCrc32Value";
		try {
			//Offset will always remain positive therefore we can do this
			offset = Long.parseUnsignedLong(args[1]);
		} catch (NumberFormatException e) {
			return "Error: Invalid byte offset";
		}
		/* No use of this statement because of signedness checker
		if (offset < 0)
			return "Error: Negative byte offset";
		*/
		try {
			if (args[2].length() != 8 || args[2].startsWith("+") || args[2].startsWith("-"))
				return "Error: Invalid new CRC-32 value";
			long temp = Long.parseLong(args[2], 16);
			if ((temp & 0xFFFFFFFFL) != temp)
				return "Error: Invalid new CRC-32 value";
			//reverse of a signed value can be unsigned
			@SuppressWarnings("signedness")
			@Unsigned int k = Integer.reverse((int)temp);
			newCrc = k;
		} catch (NumberFormatException e) {
			return "Error: Invalid new CRC-32 value";
		}
		File file = new File(args[0]);
		if (!file.isFile())
			return "Error: File does not exist: " + file;
		
		// Process the file
		try {
			modifyFileCrc32(file, offset, newCrc, true);
		} catch (IOException e) {
			return "I/O error: " + e.getMessage();
		} catch (IllegalArgumentException e) {
			return "Error: " + e.getMessage();
		} catch (AssertionError e) {
			return "Assertion error: " + e.getMessage();
		}
		return null;
	}
	
	
	/*---- Main function ----*/
	
	// Public library function.
	public static void modifyFileCrc32(File file, @Unsigned long offset,@Unsigned int newCrc, boolean printStatus) throws IOException {
		Objects.requireNonNull(file);
		/* No use of this statement because of signedness checker
			if (offset < 0)
				throw new IllegalArgumentException("Negative file offset");
		*/
		try (RandomAccessFile raf = new RandomAccessFile(file, "rws")) {
			/*Length will always be positive
			  then it can be or must be unsigned.
			  Length function needs to be annotated */
			if (offset + 4 > raf.length())
				throw new IllegalArgumentException("Byte offset plus 4 exceeds file length");
			
			// Read entire file and calculate original CRC-32 value
			@Unsigned int crc = getCrc32(raf);
			//reverse of a signed value can be unsigned
			@SuppressWarnings("signedness")
			int p= Integer.reverse(crc);
			if (printStatus)
				System.out.printf("Original CRC-32: %08X%n", p);
			
			// Compute the change to make
			@Unsigned int delta = crc ^ newCrc;
			/*Annotation of reciprocalMod
			  is needed and also of length function
			  to get the return type as unsigned */
			delta = (int)multiplyMod(reciprocalMod(powMod(2, (raf.length() - offset) * 8)), delta & 0xFFFFFFFFL);
			// Patch 4 bytes in the file
			//Unable to seek unsigned value
			raf.seek(offset);
			@Unsigned byte[] bytes4 = new byte[4];
			/* Unable to read from the file until
			   the requested number of bytes are read since bytes are unsigned.
			   Annotation in jdk is needed */
			raf.readFully(bytes4);
			//reverse of a signed value can be unsigned
			@SuppressWarnings("signedness")
			@Unsigned int k=Integer.reverse(delta);
			for (int i = 0; i < bytes4.length; i++)
				bytes4[i] ^= k >>> (i * 8);
			//Unable to seek unsigned value
			raf.seek(offset);
			raf.write(bytes4);
			if (printStatus)
				System.out.println("Computed and wrote patch");
			
			// Recheck entire file
			if (getCrc32(raf) != newCrc)
				throw new AssertionError("Failed to update CRC-32 to desired value");
			else if (printStatus)
				System.out.println("New CRC-32 successfully verified");
		}
	}
	
	
	/*---- Utilities ----*/
	
	private static @Constant long POLYNOMIAL = 0x104C11DB7L;  // Generator polynomial. Do not modify, because there are many dependencies
	
	
	private static @Unsigned int getCrc32(RandomAccessFile raf) throws IOException {
		raf.seek(0);
		int crc = 0xFFFFFFFF;
		@Unsigned byte[] buffer = new byte[32 * 1024];
		while (true) {
			/* Unable to read Unsigned bytes array.
			   Annotation in jdk is needed */
			@SuppressWarnings("signedness")
			int n = raf.read(buffer);
			//negate of a signed value can be unsigned
			@SuppressWarnings("signedness")
			@Unsigned int z= ~crc;
			if (n == -1)
				return z;
			for (int i = 0; i < n; i++) {
				for (int j = 0; j < 8; j++) {
					int k= buffer[i] >>> j;
					crc ^= k << 31;
					if (crc < 0)
						crc = (crc << 1) ^ (int)POLYNOMIAL;
					else
						crc <<= 1;
				}
			}
		}
	}
	
	
	/*---- Polynomial arithmetic ----*/
	
	// Returns polynomial x multiplied by polynomial y modulo the generator polynomial.
	private static @Unsigned long multiplyMod(@Unsigned long x, @Unsigned long y) {
		// Russian peasant multiplication algorithm
		@Unsigned long z = 0;
		while (y != 0) {
			z ^= x * (y & 1);
			y >>>= 1;
			x <<= 1;
			if (((x >>> 32) & 1) != 0)
				x ^= POLYNOMIAL;
		}
		return z;
	}
	
	
	// Returns polynomial x to the power of natural number y modulo the generator polynomial.
	private static @Unsigned long powMod(@Unsigned long x, @Unsigned long y) {
		// Exponentiation by squaring
		@Unsigned long z = 1;
		while (y != 0) {
			if ((y & 1) != 0)
				z = multiplyMod(z, x);
			x = multiplyMod(x, x);
			y >>>= 1;
		}
		return z;
	}
	
	
	// Computes polynomial x divided by polynomial y, returning the quotient and remainder.
	private static @Unsigned long[] divideAndRemainder(@Unsigned long x, @Unsigned long y) {
		if (y == 0)
			throw new IllegalArgumentException("Division by zero");
		if (x == 0)
			return new long[]{0, 0};
		
		int ydeg = getDegree(y);
		long z = 0;
		for (int i = getDegree(x) - ydeg; i >= 0; i--) {
			if (((x >>> (i + ydeg)) & 1) != 0) {
				x ^= y << i;
				z |= 1 << i;
			}
		}
		return new long[]{z, x};
	}
	
	
	// Returns the reciprocal of polynomial x with respect to the generator polynomial.
	private static @Unsigned long reciprocalMod(@Unsigned long x) {
		// Based on a simplification of the extended Euclidean algorithm
		@Unsigned long y = x;
		x = POLYNOMIAL;
		@Unsigned long a = 0;
		@Unsigned long b = 1;
		while (y != 0) {
			@Unsigned long[] divRem = divideAndRemainder(x, y);
			@Unsigned long c = a ^ multiplyMod(divRem[0], b);
			x = y;
			y = divRem[1];
			a = b;
			b = c;
		}
		if (x == 1)
			return a;
		else
			throw new IllegalArgumentException("Reciprocal does not exist");
	}
	
	
	private static int getDegree(@Unsigned long x) {
		/*Only takes signed number as parameter.
		 Need to annotate in the JDK*/
		@SuppressWarnings("signedness")
		@Unsigned int k=Long.numberOfLeadingZeros(x);
		return 63 - k;
	}
	
}
 
