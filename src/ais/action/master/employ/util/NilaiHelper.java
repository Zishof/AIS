package ais.action.master.employ.util;

import ais.common.Common;

public class NilaiHelper {

	public static String[] nilais = {

	"Nol", "Satu", "Dua", "Tiga", "Empat", "Lima", "Enam", "Tujuh", "Delapan",
			"Sembilan",

			"Sepuluh", "Sebelas", "Dua Belas", "Tiga Belas", "Empat Belas",
			"Lima Belas", "Enam Belas", "Tujuh Belas", "Delapan Belas",
			"Sembilan Belas",

			"Dua Puluh", "Dua Puluh Satu", "Dua Puluh Dua", "Dua Puluh Tiga",
			"Dua Puluh Empat", "Dua Puluh Lima", "Dua Puluh Enam",
			"Dua Puluh Tujuh", "Dua Puluh Delapan", "Dua Puluh Sembilan",

			"Tiga Puluh", "Tiga Puluh Satu", "Tiga Puluh Dua",
			"Tiga Puluh Tiga", "Tiga Puluh Empat", "Tiga Puluh Lima",
			"Tiga Puluh Enam", "Tiga Puluh Tujuh", "Tiga Puluh Delapan",
			"Tiga Puluh Sembilan",

			"Empat Puluh", "Empat Puluh Satu", "Empat Puluh Empat",
			"Empat Puluh Tiga", "Empat Puluh Empat", "Empat Puluh Lima",
			"Empat Puluh Enam", "Empat Puluh Tujuh", "Empat Puluh Delapan",
			"Empat Puluh Sembilan",

			"Lima Puluh", "Lima Puluh Satu", "Lima Puluh Dua",
			"Lima Puluh Tiga", "Lima Puluh Empat", "Lima Puluh Lima",
			"Lima Puluh Enam", "Lima Puluh Tujuh", "Lima Puluh Delapan",
			"Lima Puluh Sembilan",

			"Enam Puluh", "Enam Puluh Satu", "Enam Puluh Dua",
			"Enam Puluh Tiga", "Enam Puluh Empat", "Enam Puluh Lima",
			"Enam Puluh Enam", "Enam Puluh Tujuh", "Enam Puluh Delapan",
			"Enam Puluh Sembilan",

			"Tujuh Puluh", "Tujuh Puluh Satu", "Tujuh Puluh Dua",
			"Tujuh Puluh Tiga", "Tujuh Puluh Empat", "Tujuh Puluh Lima",
			"Tujuh Puluh Enam", "Tujuh Puluh Tujuh", "Tujuh Puluh Delapan",
			"Tujuh Puluh Sembilan",

			"Delapan Puluh", "Delapan Puluh Satu", "Delapan Puluh Dua",
			"Delapan Puluh Tiga", "Delapan Puluh Empat", "Delapan Puluh Lima",
			"Delapan Puluh Enam", "Delapan Puluh Tujuh",
			"Delapan Puluh Delapan", "Delapan Puluh Sembilan",

			"Sembilan Puluh", "Sembilan Puluh Satu", "Sembilan Puluh Dua",
			"Sembilan Puluh Tiga", "Sembilan Puluh Empat",
			"Sembilan Puluh Lima", "Sembilan Puluh Enam",
			"Sembilan Puluh Tujuh", "Sembilan Puluh Delapan",
			"Sembilan Puluh Sembilan",

			"Seratus" };

	public static String[] nilaisRibuan = { "Seribu", "Dua Ribu", "Tiga Ribu",
			"Empat Ribu", "Lima Ribu", "Enam Ribu", "Tujuh Ribu",
			"Delapan Ribu", "Sembilan Ribu", "Sepuluh Ribu", "Sebelas Ribu",
			"Dua Belas Ribu", "Tiga Belas Ribu", "Empat Belas Ribu",
			"Lima Belas Ribu", "Enam Belas Ribu", "Tujuh Belas Ribu" };

	public static String getNilai(Double nilai) {
		String ss = Common.numberFormat.get().format(nilai);
		String[] sss = ss.split("\\.");
		if (sss.length == 1) {
			sss = ss.split(",");
		}

		String result = "";
		if (sss.length == 1) {
			if (sss[0].length() == 3) {
				result = "";
			}
		}

		return result;
	}

}
