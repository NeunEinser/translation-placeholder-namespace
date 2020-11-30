import java.math.BigInteger;
import java.util.ArrayList;

public class Main {
	public static void main(String[] args) {
		if (args.length != 2) {
			System.err.println("Usage: java -jar <jar file> (encode|decode) <value>");
			System.exit(-1);
		}

		switch (args[0]) {
			case "encode" -> encode(args[1]);
			case "decode" -> decode(args[1]);
			default -> System.err.printf("invalid command %s. Expected either \"encode\" or \"decode\".%n", args[0]);
		}

		final var namespace = args[0];
	}

	private static void encode(final String namespace) {
		if (!namespace.matches("^[a-z0-9_.-]+$")) {
			System.err.printf("invalid namespace %s%n", namespace);
			System.exit(-1);
		}

		final boolean fullCharSet = !namespace.matches("^[a-z_]+$");

		BigInteger value = BigInteger.ZERO;
		final int base = fullCharSet ? 39 : 27;
		final BigInteger multiplier = BigInteger.valueOf(base);

		for (char curChar : namespace.toCharArray()) {
			//The add one is here to create a consistent offset. In case a namespace starts with 'a', we don't want
			// the first digit as 0. 
			value = value.multiply(multiplier)
			             .add(BigInteger.valueOf(getCharValue(curChar) + 1));
		}

		// ceil((value.bitLength() + 1) / 30)
		// Int division always floors, but ceil can be expressed with ((n - 1) / m) +1
		// Since n = value.bitLength() + 1, the + 1 with the - 1 cancel each other out.
		// The + 1 is needed because we use an additional bit to store the value of fullCharSet
		// Each translation placeholder can hold 30 bits (positive signed int). The first bit is always 1, to ensure
		// a value > 0
		final int length = (value.bitLength()) / 30 + 1;

		final int[] placeholders = new int[length];
		final BigInteger twoPow30 = BigInteger.valueOf(0x40_00_00_00);

		for (int i = length -1; i >= 0; i--) {
			final var divMod = value.divideAndRemainder(twoPow30);

			placeholders[i] = divMod[1].intValueExact()
			                  | 0x40_00_00_00;
			value = divMod[0];

			if (i == 0 && fullCharSet) {
				placeholders[0] |= 0x20_00_00_00;
			}
		}

		final var result = new StringBuilder();
		for (int placeholder : placeholders) {
			result.append("%")
			      .append(Integer.toUnsignedString(placeholder))
			      .append("$s");
		}

		System.out.println(result);
	}

	private static int getCharValue(char c) {
		if (c >= 'a') {
			return c - 'a';
		} else if (c == '_') {
			return 26;
		} else if (c >= '0') {
			return c - '0' + 27;
		} else if (c == '-') {
			return 37;
		} else {
			return 38;
		}
	}

	private static void decode(final String encodedNamespace) {
		if (!encodedNamespace.matches("^(?:%[0-9]+\\$s)+$")) {
			System.err.printf("invalid encoded namespace %s%n", encodedNamespace);
			System.exit(-1);
		}

		final String trimmed = encodedNamespace.substring(1, encodedNamespace.length() - 2);
		final String[] placeholderStrings = trimmed.split("\\$s%");
		final int[] placeholders = new int[placeholderStrings.length];

		for (int i = 0; i < placeholders.length; i++) {
			placeholders[i] = Integer.parseInt(placeholderStrings[i]) & 0xBF_FF_FF_FF;
		}
		boolean fullCharSet = (placeholders[0] & 0x20_00_00_00) > 0;
		placeholders[0] &= 0xDF_FF_FF_FF;

		final BigInteger twoPow30 = BigInteger.valueOf(0x40_00_00_00L);
		BigInteger value = BigInteger.ZERO;

		for (int placeholder : placeholders) {
			value = value.multiply(twoPow30)
			             .add(BigInteger.valueOf(placeholder));
		}

		final int base = fullCharSet ? 39 : 27;
		final BigInteger divisor = BigInteger.valueOf(base);
		final var characters = new ArrayList<Character>();

		while (value.compareTo(BigInteger.ZERO) > 0) {
			value = value.subtract(BigInteger.ONE);
			final BigInteger[] divMod = value.divideAndRemainder(divisor);

			value = divMod[0];
			characters.add(getCharFromValue(divMod[1].intValueExact()));
		}

		final var chars = new char[characters.size()];
		for(int i = 0; i < chars.length; i++) {
			chars[i] = characters.get(chars.length - i - 1);
		}

		System.out.println(new String(chars));
	}

	private static char getCharFromValue(int value) {
		if (value < 26) {
			return (char) ('a' + value);
		} else if (value == 26) {
			return '_';
		} else if (value < 37) {
			return (char) ('0' + value - 27);
		} else if (value == 37) {
			return '-';
		} else {
			return '.';
		}
	}
}
