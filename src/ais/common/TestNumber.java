package ais.common;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.apache.commons.lang.StringUtils;

public class TestNumber {

	public static double withMathRound(double value, int places) {
		double scale = Math.pow(10, places);
		return Math.round(value * scale) / scale;
	}

	public static DecimalFormat df = (DecimalFormat) NumberFormat.getNumberInstance(Common.locale);

	public static void main(String[] args) {

		double number = 0.5;
//		number = withMathRound(number, 2);
		String string = df.format(number);
		string = org.apache.commons.lang3.StringUtils.replace(string, ".", "");
		string = org.apache.commons.lang3.StringUtils.replace(string, ",", ".");
		System.out.println("string -> " + string);

		String rowData = "1. Do'a Sebelum Belajar->A;A-;B+;B;C+;C;D";
		String[] colAtauRow = rowData.split("->");

		String[] colAtauRowOld = rowData.split("->");

		String colsOld = colAtauRowOld.length > 1 ? colAtauRowOld[1] : "";
		System.out.println("colsOld => " + colsOld);

		String cols = colAtauRow.length > 1 ? colAtauRow[1] : "";

		System.out.println("cols => " + cols);
	}

}
