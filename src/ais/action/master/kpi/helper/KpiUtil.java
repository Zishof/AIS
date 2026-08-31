package ais.action.master.kpi.helper;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.LogicalUtil;
import ais.database.model.Konstanta;
import ais.database.model.kpi.ItemKpi;
import ais.database.model.kpi.Kpi;
import ais.database.model.kpi.SkorKpi;
import ais.ui.util.WaktuUtil;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

/**
 * Mesin evaluasi formula KPI (Key Performance Indicator) modul KPI. {@code formula} pada seluruh
 * method di kelas ini adalah string JSON berbentuk array objek {@code {tgl, target, <kode_skor>...}}
 * — satu entri per periode efektif; entri yang tanggalnya (dikonversi ke granularitas hari,
 * {@code Common.dateFormat8}) sudah lewat atau sama dengan tanggal evaluasi ({@code sekarang}), dan
 * merupakan yang <b>paling baru</b> di antara yang memenuhi syarat itu (lewat {@link TreeMap}
 * terurut terbalik), yang dipakai sebagai formula/target aktif — memungkinkan definisi KPI berubah
 * dari waktu ke waktu tanpa kehilangan riwayat. Kolom {@code target} berisi ekspresi matematika
 * bebas (mendukung placeholder kode skor KPI, kode {@link Konstanta} sistem, kode {@link Kpi} lain
 * yang dievaluasi rekursif, dan opsional kode {@link ItemKpi}) yang dievaluasi lewat
 * <a href="https://github.com/fasseg/exp4j">exp4j</a> ({@link ExpressionBuilder}) dengan fungsi dan
 * operator kustom dari {@link LogicalUtil}. Rekursi antar-KPI dibatasi 500 percobaan
 * ({@code coba > 500}) untuk mencegah loop tak berhingga akibat KPI yang saling merujuk.
 */
public class KpiUtil {

	/**
	 * Mengambil string target/formula mentah yang efektif berlaku pada tanggal {@code sekarang} dari
	 * {@code formula} (array JSON entri {tgl, target}), memilih entri bertanggal efektif paling baru
	 * yang tidak melampaui {@code sekarang}. Mengembalikan string kosong bila parsing gagal atau
	 * tidak ada entri yang efektif (kegagalan diserap secara diam-diam, dicatat ke audit error).
	 *
	 * @param formula   array JSON entri formula ({@code [{tgl, target, ...}, ...]})
	 * @param sekarang  tanggal acuan evaluasi
	 * @return string target/ekspresi mentah yang efektif, atau string kosong bila tidak ditemukan
	 */
	public static String ambilTarget(String formula, Date sekarang) throws Exception {
		String hasil = "";
		try {
			String s = Common.dateFormat1.get().format(sekarang);
			JSONArray jsonArray = new JSONArray(formula);
			TreeMap<String, String> targets = new TreeMap<String, String>(Collections.reverseOrder());
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject jsonObject = jsonArray.getJSONObject(i);
				if (!jsonObject.isNull("tgl") && !jsonObject.isNull("target")) {
					Date tgl = Common.dateFormat1.get().parse(jsonObject.get("tgl").toString());
					targets.put(Common.dateFormat8.get().format(tgl), jsonObject.get("target") + "");
				}
			}

			for (String ss : targets.keySet()) {
				Date tanggalEfektif = Common.dateFormat8.get().parse(ss);
				if (tanggalEfektif.before(sekarang) || Common.dateFormat1.get().format(tanggalEfektif).equals(s)) {
					hasil = targets.get(ss);
					break;
				}
			}
			targets = null;
			jsonArray = null;

		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/kpi/helper/KpiUtil.java:51");
			// TODO: handle exception
		}
		return hasil;
	}

	/** Seperti {@link #ambilPoint(JSONObject, Map, boolean)} tanpa peta {@link ItemKpi} tambahan (placeholder kode item KPI pada ekspresi tidak akan tersubstitusi). */
	public static Double ambilPoint(JSONObject jsonObject, boolean refresh) throws Exception {
		return ambilPoint(jsonObject, null, refresh);
	}

	/**
	 * Menghitung nilai poin dari satu objek {@code jsonObject} (satu snapshot skor: kunci berupa
	 * kode {@link SkorKpi} aktif dan {@code target}, BUKAN array JSON seperti method
	 * {@link #ambilPoint(String, Date, Map, boolean, int)} lainnya). Ekspresi {@code target}
	 * disubstitusi berturut-turut: kode {@link Konstanta} sistem → nilai keterangannya; kode
	 * {@link Kpi} lain → hasil evaluasi rekursif formulanya; kode {@link ItemKpi} pada
	 * {@code dataItemKpi} → nilai target-nya (atau hasil evaluasi ulang bila {@code refresh}
	 * {@code true}). Bila hasil substitusi berupa angka murni, dikembalikan langsung; bila tidak,
	 * dievaluasi sebagai ekspresi matematika lewat exp4j dengan variabel = skor KPI dari
	 * {@code jsonObject}. Seluruh kegagalan ditangkap dan menghasilkan {@code 0.0} (dicatat ke audit
	 * error, tidak melempar exception ke pemanggil).
	 *
	 * @param jsonObject snapshot data skor KPI beserta {@code target} (ekspresi/formula)
	 * @param dataItemKpi peta id→{@link ItemKpi} untuk substitusi placeholder kode item KPI, boleh {@code null}
	 * @param refresh    bila {@code true}, nilai item KPI dievaluasi ulang rekursif alih-alih memakai {@code target} tersimpan
	 * @return nilai poin hasil evaluasi, {@code 0.0} bila gagal atau tidak ada target
	 */
	@SuppressWarnings("rawtypes")
	public static Double ambilPoint(JSONObject jsonObject, Map dataItemKpi, boolean refresh) throws Exception {
		Double hasil = 0.0;
		try {
			Map<String, Double> data = new HashMap<String, Double>();
			for (Object o : ConstantValues.ambilBerdasarClass(SkorKpi.class).values()) {
				SkorKpi skorKpi = (SkorKpi) o;
				if (skorKpi.getAktif()) {
					if (!jsonObject.isNull(skorKpi.getKode())) {
						data.put(skorKpi.getKode(), jsonObject.getDouble(skorKpi.getKode()));
					} else {
						data.put(skorKpi.getKode(), 0.0);
					}
				}
			}

			Date sekarang = WaktuUtil.getDate();

			if (data != null && !jsonObject.isNull("target")) {
				String target = jsonObject.get("target") + "";
				if (target != null && !target.trim().isEmpty()) {
					target = " " + target + " ";
					target = target.replaceAll("\\(", " ( ");
					target = target.replaceAll("\\)", " ) ");
					target = target.replaceAll("\\+", " + ");
					target = target.replaceAll("\\-", " - ");
					target = target.replaceAll("\\*", " * ");
					target = target.replaceAll("/", " / ");
					target = target.replaceAll("%", " % ");

					for (Object o : ConstantValues.ambilBerdasarClass(Konstanta.class).values()) {
						Konstanta konstanta = (Konstanta) o;
						if (konstanta.getAktif() && konstanta.getKode() != null) {
							if (StringUtils.contains(target, " " + konstanta.getKode() + " ")) {
								target = org.apache.commons.lang3.StringUtils.replace(target, " " + konstanta.getKode() + " ",
										" " + konstanta.getKeterangan() + " ");
							}
						}

					}

					for (Object o : ConstantValues.ambilBerdasarClass(Kpi.class).values()) {
						Kpi kpi = (Kpi) o;
						if (kpi.getAktif()) {
							if (StringUtils.contains(target, " " + kpi.getKode() + " ")) {
								Double nilai = KpiUtil.ambilPoint(kpi.getFormula(), sekarang, dataItemKpi, refresh, 0);
								target = target.replaceAll(" " + kpi.getKode() + " ", " " + nilai + " ");
							}
						}
					}

					if (dataItemKpi != null) {
						for (Object o : dataItemKpi.values()) {
							ItemKpi itemKpi = (ItemKpi) o;
							if (itemKpi.getAktif()) {
								if (StringUtils.contains(target, " " + itemKpi.getKode() + " ")) {
									Double nilai = refresh
											? KpiUtil.ambilPoint(itemKpi.getFormula(), sekarang, dataItemKpi, refresh,
													0)
											: itemKpi.getTarget();
									target = target.replaceAll(" " + itemKpi.getKode() + " ", " " + nilai + " ");
								}
							}
						}
					}

					if (Common.isNumber(target)) {
						hasil = Double.parseDouble(target.trim());
					} else {

						try {
							Expression e = new ExpressionBuilder(target).variables(data.keySet())
									.functions(LogicalUtil.ALL_FUNCTION).operator(LogicalUtil.ALL_OPERATOR).build();

							for (String kode : data.keySet()) {
								try {
									e.setVariable(kode, data.get(kode));
								} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/kpi/helper/KpiUtil.java:138");
								}
							}
							try {
								boolean valid = e.validate().isValid();
								System.out.println("formula = " + target + " valid => " + valid);
								if (!valid) {
									List<String> d = e.validate().getErrors();
									if (!d.isEmpty()) {
										String ds = "";
										for (String dd : d) {
											ds += ds.isEmpty() ? dd : ".\n" + dd;
										}
										System.out.println("error = " + ds);
									}
								}
							} catch (Exception ee) {
								ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/master/kpi/helper/KpiUtil.java:155");
							}
							hasil = e.evaluate();
						} catch (Exception ee) {
							ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/master/kpi/helper/KpiUtil.java:159");
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/kpi/helper/KpiUtil.java:165");
		}
		return hasil;

	}

	/** Seperti {@link #ambilPoint(String, Date, Map, boolean, int)} tanpa peta {@link ItemKpi} tambahan dan hitungan rekursi awal 0. */
	public static Double ambilPoint(String formula, Date sekarang, boolean refresh) throws Exception {
		return ambilPoint(formula, sekarang, null, refresh, 0);
	}

	/**
	 * Implementasi inti evaluasi poin KPI dari {@code formula} (array JSON multi-periode, lihat
	 * javadoc kelas): memilih entri skor efektif untuk {@code sekarang}, mengambil target efektif
	 * lewat {@link #ambilTarget}, mensubstitusi placeholder kode Konstanta/Kpi/ItemKpi (Kpi lain
	 * dievaluasi rekursif dengan {@code coba} bertambah, dihentikan paksa mengembalikan {@code 0.0}
	 * bila {@code coba > 500} untuk mencegah loop tak berhingga antar-KPI yang saling merujuk), lalu
	 * mengevaluasi hasil akhirnya sebagai angka murni atau ekspresi matematika (exp4j) dengan
	 * variabel = skor KPI periode efektif tersebut. Mengembalikan {@code 0.0} bila tidak ada periode
	 * efektif ditemukan atau evaluasi gagal.
	 *
	 * @param formula     array JSON entri formula multi-periode ({@code [{tgl, <kode_skor>...}, ...]})
	 * @param sekarang    tanggal acuan evaluasi
	 * @param dataItemKpi peta id→{@link ItemKpi} untuk substitusi placeholder kode item KPI, boleh {@code null}
	 * @param refresh     bila {@code true}, nilai item KPI dievaluasi ulang rekursif alih-alih memakai target tersimpan
	 * @param coba        penghitung rekursi antar-KPI, dipakai internal untuk mencegah loop tak berhingga (mulai dari 0)
	 * @return nilai poin hasil evaluasi, {@code 0.0} bila tidak ada periode efektif atau evaluasi gagal
	 */
	@SuppressWarnings({ "rawtypes" })
	public static Double ambilPoint(String formula, Date sekarang, Map dataItemKpi, boolean refresh, int coba)
			throws Exception {
		if (coba > 500) {
			return 0.0;
		}
		String s = Common.dateFormat1.get().format(sekarang);
		JSONArray jsonArray = new JSONArray(formula);
		TreeMap<String, Map<String, Double>> formulas = new TreeMap<String, Map<String, Double>>(
				Collections.reverseOrder());
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonObject = jsonArray.getJSONObject(i);
			if (!jsonObject.isNull("tgl")) {
				Date tgl = Common.dateFormat1.get().parse(jsonObject.get("tgl").toString());

				Map<String, Double> data = formulas.get(Common.dateFormat8.get().format(tgl));
				if (data == null) {
					data = new HashMap<String, Double>();
					formulas.put(Common.dateFormat8.get().format(tgl), data);
				}

				for (Object o : ConstantValues.ambilBerdasarClass(SkorKpi.class).values()) {
					SkorKpi skorKpi = (SkorKpi) o;
					if (skorKpi.getAktif()) {
						try {
							if (!jsonObject.isNull(skorKpi.getKode())) {
								data.put(skorKpi.getKode(), jsonObject.getDouble(skorKpi.getKode()));
							} else {
								data.put(skorKpi.getKode(), 0.0);
							}
						} catch (Exception e) {
							data.put(skorKpi.getKode(), 0.0);
						}
					}
				}

			}
		}

		Map<String, Double> data = null;
		for (String ss : formulas.keySet()) {
			try {
				Date tanggalEfektif = Common.dateFormat8.get().parse(ss);
				if (tanggalEfektif.before(sekarang) || Common.dateFormat1.get().format(tanggalEfektif).equals(s)) {
					data = formulas.get(ss);
					break;
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/kpi/helper/KpiUtil.java:222");
				// TODO: handle exception
			}
		}

		Double hasil = 0.0;
		if (data != null) {
			String target = ambilTarget(formula, sekarang);
			if (target != null && !target.trim().isEmpty()) {
				target = " " + target + " ";
				target = target.replaceAll("\\(", " ( ");
				target = target.replaceAll("\\)", " ) ");
				target = target.replaceAll("\\+", " + ");
				target = target.replaceAll("\\-", " - ");
				target = target.replaceAll("\\*", " * ");
				target = target.replaceAll("/", " / ");
				target = target.replaceAll("%", " % ");

				for (Object o : ConstantValues.ambilBerdasarClass(Konstanta.class).values()) {
					Konstanta konstanta = (Konstanta) o;
					if (konstanta.getAktif() && konstanta.getKode() != null) {
						if (StringUtils.contains(target, " " + konstanta.getKode() + " ")) {
							target = org.apache.commons.lang3.StringUtils.replace(target, " " + konstanta.getKode() + " ",
									" " + konstanta.getKeterangan() + " ");
						}
					}

				}

				for (Object o : ConstantValues.ambilBerdasarClass(Kpi.class).values()) {
					Kpi kpi = (Kpi) o;
					if (kpi.getAktif()) {
						if (StringUtils.contains(target, " " + kpi.getKode() + " ")) {
							Double nilai = KpiUtil.ambilPoint(kpi.getFormula(), sekarang, dataItemKpi, refresh, ++coba);
							target = target.replaceAll(" " + kpi.getKode() + " ", " " + nilai + " ");
						}
					}
				}

				if (dataItemKpi != null) {
					for (Object o : dataItemKpi.values()) {
						ItemKpi itemKpi = (ItemKpi) o;
						if (itemKpi.getAktif()) {
							if (StringUtils.contains(target, " " + itemKpi.getKode() + " ")) {
								Double nilai = refresh
										? KpiUtil.ambilPoint(itemKpi.getFormula(), sekarang, dataItemKpi, refresh,
												++coba)
										: itemKpi.getTarget();
								target = target.replaceAll(" " + itemKpi.getKode() + " ", " " + nilai + " ");
							}
						}
					}
				}

				if (Common.isNumber(target)) {
					hasil = Double.parseDouble(target.trim());
				} else {

					try {
						Expression e = new ExpressionBuilder(target).variables(data.keySet())
								.functions(LogicalUtil.ALL_FUNCTION).operator(LogicalUtil.ALL_OPERATOR).build();

						for (String kode : data.keySet()) {
							try {
								e.setVariable(kode, data.get(kode));
							} catch (Exception ee) {
								ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/master/kpi/helper/KpiUtil.java:288");
							}
						}
						try {
							boolean valid = e.validate().isValid();
							System.out.println("formula = " + target + " valid => " + valid);
							if (!valid) {
								List<String> d = e.validate().getErrors();
								if (!d.isEmpty()) {
									String ds = "";
									for (String dd : d) {
										ds += ds.isEmpty() ? dd : ".\n" + dd;
									}
									System.out.println("error = " + ds);
								}
							}
						} catch (Exception ee) {
							ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/master/kpi/helper/KpiUtil.java:305");
						}
						hasil = e.evaluate();
					} catch (Exception ee) {
						ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/master/kpi/helper/KpiUtil.java:309");
					}
				}
			}
		}
		formulas = null;
		jsonArray = null;
		return hasil;
	}

	/**
	 * Menyusun deskripsi HTML (daftar bernomor {@code <ol>}) yang merangkum setiap entri periode
	 * pada {@code formula} (array JSON multi-periode): tanggal, nilai tiap skor KPI aktif yang ada
	 * pada entri tersebut, dan ekspresi target-nya — dipakai sebagai tooltip/penjelasan formula di
	 * UI. Kegagalan parsing menghasilkan daftar kosong (elemen {@code <ol>} tetap dibuka/ditutup),
	 * tidak melempar exception ke pemanggil.
	 *
	 * @param formula array JSON entri formula multi-periode
	 * @return markup HTML {@code <ol>} berisi ringkasan tiap periode
	 */
	public static String ambilDeskripsi(String formula) throws Exception {

		String t = "<ol style='font-size:x-small;text-align: left;'>";

		try {
			JSONArray jsonArray = new JSONArray(formula);
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject jsonObject = jsonArray.getJSONObject(i);
				String d = "";
				if (!jsonObject.isNull("tgl")) {
					d = "Tgl:" + jsonObject.get("tgl");
				}

				for (Object o : ConstantValues.ambilBerdasarClass(SkorKpi.class).values()) {
					SkorKpi skorKpi = (SkorKpi) o;
					if (skorKpi.getAktif()) {
						if (!jsonObject.isNull(skorKpi.getKode())) {
							d += ", " + skorKpi.getNama() + ": " + jsonObject.get(skorKpi.getKode());
						}
					}
				}

				if (!jsonObject.isNull("target")) {
					d += ", Formula: " + jsonObject.get("target");
				}

				if (!d.isEmpty()) {
					t += "<li>" + d + "</li>";
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/kpi/helper/KpiUtil.java:349");
			// TODO: handle exception
		}

		t += "</ol>";
		return t;
	}

}
