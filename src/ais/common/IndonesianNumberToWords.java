package ais.common;

import java.text.DecimalFormat;

public class IndonesianNumberToWords {

	private static final String[] tensNames = { "", " sepuluh", " dua puluh", " tiga puluh", " empat puluh",
			" lima puluh", " enam puluh", " tujuh puluh", " delapan puluh", " sembilan puluh" };

	private static final String[] numNames = { "", " satu", " dua", " tiga", " empat", " lima", " enam", " tujuh",
			" delapan", " sembilan", " sepuluh", " sebelas", " dua belas", " tiga belas", " empat belas", " lima belas",
			" enam belas", " tujuh belas", " delapan belas", " sembilan belas" };

	private IndonesianNumberToWords() {
	}

	private static String convertLessThanOneThousand(int number) {
		String soFar;

		if (number % 100 < 20) {
			soFar = numNames[number % 100];
			number /= 100;
		} else {
			soFar = numNames[number % 10];
			number /= 10;

			soFar = tensNames[number % 10] + soFar;
			number /= 10;
		}
		if (number == 0)
			return soFar;
		else if (number == 1)
			return "seratus" + soFar;
		return numNames[number] + " ratus" + soFar;
	}

	public static String convert(String number) {
		String[] spit = number.split(",");
		if (spit.length == 1) {
			return convert(Long.parseLong(number));
		} else if (spit.length > 1) {
			return convert(Long.parseLong(spit[0])) + " koma " + convert(Long.parseLong(spit[1]));
		} else {
			return "";
		}
	}

	public static String convert(long number) {
		// 0 to 999 999 999 999
		if (number == 0) {
			return "nol";
		}

		// Bilangan NEGATIF (mis. tagihan minus akibat diskon/beasiswa atau nilai overflow
		// -2147483648) tidak boleh masuk ke parsing substring di bawah: tanda '-' menggeser
		// posisi digit sehingga salah satu segmen ter-parse negatif -> numNames[indeks negatif]
		// melempar ArrayIndexOutOfBoundsException. Tangani dengan awalan "minus" + nilai mutlak.
		if (number < 0) {
			// Long.MIN_VALUE tak punya nilai mutlak positif; amankan agar tidak overflow.
			if (number == Long.MIN_VALUE) {
				return "minus " + convert(Long.MAX_VALUE);
			}
			return "minus " + convert(-number);
		}

		String snumber = Long.toString(number);

		// pad with "0"
		String mask = "000000000000";
		DecimalFormat df = new DecimalFormat(mask);
		snumber = df.format(number);

		// XXXnnnnnnnnn
		int billions = Integer.parseInt(snumber.substring(0, 3));
		// nnnXXXnnnnnn
		int millions = Integer.parseInt(snumber.substring(3, 6));
		// nnnnnnXXXnnn
		int hundredThousands = Integer.parseInt(snumber.substring(6, 9));
		// nnnnnnnnnXXX
		int thousands = Integer.parseInt(snumber.substring(9, 12));

		String tradBillions;
		switch (billions) {
		case 0:
			tradBillions = "";
			break;
		case 1:
			tradBillions = convertLessThanOneThousand(billions) + " milyar ";
			break;
		default:
			tradBillions = convertLessThanOneThousand(billions) + " milyar ";
		}
		String result = tradBillions;

		String tradMillions;
		switch (millions) {
		case 0:
			tradMillions = "";
			break;
		case 1:
			tradMillions = convertLessThanOneThousand(millions) + " juta ";
			break;
		default:
			tradMillions = convertLessThanOneThousand(millions) + " juta ";
		}
		result = result + tradMillions;

		String tradHundredThousands;
		switch (hundredThousands) {
		case 0:
			tradHundredThousands = "";
			break;
		case 1:
			tradHundredThousands = "seribu ";
			break;
		default:
			tradHundredThousands = convertLessThanOneThousand(hundredThousands) + " ribu ";
		}
		result = result + tradHundredThousands;

		String tradThousand;
		tradThousand = convertLessThanOneThousand(thousands);
		result = result + tradThousand;

		// remove extra spaces!
		return result.replaceAll("^\\s+", "").replaceAll("\\b\\s{2,}\\b", " ");
	}

	/**
	 * testing
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("*** " + IndonesianNumberToWords.convert(0));
		System.out.println("*** " + IndonesianNumberToWords.convert(1));
		System.out.println("*** " + IndonesianNumberToWords.convert(16));
		System.out.println("*** " + IndonesianNumberToWords.convert(100));
		System.out.println("*** " + IndonesianNumberToWords.convert(118));
		System.out.println("*** " + IndonesianNumberToWords.convert(200));
		System.out.println("*** " + IndonesianNumberToWords.convert(219));
		System.out.println("*** " + IndonesianNumberToWords.convert(800));
		System.out.println("*** " + IndonesianNumberToWords.convert(801));
		System.out.println("*** " + IndonesianNumberToWords.convert(1316));
		System.out.println("*** " + IndonesianNumberToWords.convert(1000000));
		System.out.println("*** " + IndonesianNumberToWords.convert(2000000));
		System.out.println("*** " + IndonesianNumberToWords.convert(3000200));
		System.out.println("*** " + IndonesianNumberToWords.convert(700000));
		System.out.println("*** " + IndonesianNumberToWords.convert(9000000));
		System.out.println("*** " + IndonesianNumberToWords.convert(9001000));
		System.out.println("*** " + IndonesianNumberToWords.convert(123456789));
		System.out.println("*** " + IndonesianNumberToWords.convert(2147483647));
		System.out.println("*** " + IndonesianNumberToWords.convert(3000000010L));

		/*
		 *** zero one sixteen one hundred one hundred eighteen two hundred two hundred
		 * nineteen eight hundred eight hundred one one thousand three hundred sixteen
		 * one million two millions three millions two hundred seven hundred thousand
		 * nine millions nine millions one thousand one hundred twenty three millions
		 * four hundred fifty six thousand seven hundred eighty nine two billion one
		 * hundred forty seven millions four hundred eighty three thousand six hundred
		 * forty seven three billion ten
		 **/
	}

}
