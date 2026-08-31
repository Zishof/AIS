package ais.action.master.employ.helper;

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
import ais.database.model.employ.Golongan;
import ais.database.model.employ.SkorGolongan;
import ais.ui.util.WaktuUtil;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

/**
 * Mesin evaluasi formula kenaikan {@link Golongan} pegawai (kepegawaian): menghitung "poin"
 * seorang pegawai pada tanggal tertentu dari sebuah {@code formula} tersimpan berformat JSON
 * (array objek {@code {"tgl":..., "target":"...", <kodeSkorGolongan>:nilai, ...}}) memakai
 * pustaka evaluasi ekspresi matematika exp4j.
 *
 * <h2>Struktur formula</h2>
 * Setiap {@code formula} adalah riwayat aturan yang berlaku EFEKTIF sejak tanggal
 * ({@code tgl}) tertentu — mendukung perubahan kebijakan kenaikan golongan dari waktu ke waktu
 * tanpa kehilangan riwayat lama. Untuk menghitung poin pada {@code sekarang}, aturan yang dipakai
 * adalah entri dengan {@code tgl} EFEKTIF TERBARU yang tidak melewati {@code sekarang (diurutkan
 * menurun lewat {@link TreeMap} dengan {@link Collections#reverseOrder()}). Field {@code target}
 * berisi ekspresi matematis (mis. {@code "SK1 + SK2 * 2"}) yang variabelnya adalah kode
 * {@link SkorGolongan} aktif dan dapat juga merujuk kode {@link Konstanta} (disubstitusi jadi
 * nilai konstanta) atau kode {@link Golongan} lain (disubstitusi jadi HASIL EVALUASI REKURSIF
 * formula golongan tersebut — memungkinkan satu golongan mensyaratkan poin golongan sebelumnya).
 *
 * <h2>Pengaman rekursi</h2>
 * Karena formula golongan dapat merujuk golongan lain yang mungkin (secara keliru) merujuk balik,
 * {@link #ambilPoint(String, Date, int)} membawa parameter {@code coba} yang dinaikkan setiap
 * evaluasi bersarang dan dipotong paksa (mengembalikan 0.0) setelah 25 tingkat rekursi, mencegah
 * {@link StackOverflowError} akibat referensi melingkar antar golongan.
 */
public class GolonganUtil {

	/**
	 * Memilih entri {@code target} (teks ekspresi) yang berlaku efektif pada {@code sekarang} dari
	 * riwayat {@code formula} JSON, yaitu entri dengan {@code tgl} terbaru yang tidak melewati
	 * {@code sekarang}. Kegagalan parsing formula ditelan dan mengembalikan string kosong.
	 *
	 * @param formula  riwayat formula (JSON array {@code {"tgl", "target"}})
	 * @param sekarang tanggal acuan
	 * @return ekspresi target yang berlaku, atau string kosong bila tidak ada entri yang cocok/formula tidak valid
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
					targets.put(Common.dateFormat8.get().format(tgl), jsonObject.get("target")+"");
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

		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/employ/helper/GolonganUtil.java:50");
			// TODO: handle exception
		}
		return hasil;
	}

	/**
	 * Menghitung poin langsung dari SATU objek data skor (bukan riwayat formula tersimpan): nilai
	 * setiap {@link SkorGolongan} aktif diambil dari {@code jsonObject} (0.0 bila tidak ada), lalu
	 * ekspresi {@code target} pada {@code jsonObject} yang sama dievaluasi setelah substitusi kode
	 * {@link Konstanta} dan kode {@link Golongan} lain (rekursif, lihat javadoc kelas).
	 *
	 * @param jsonObject objek berisi nilai skor per kode {@link SkorGolongan} plus field {@code target}
	 * @return hasil evaluasi ekspresi, atau 0.0 bila {@code target} kosong/evaluasi gagal
	 */
	public static Double ambilPoint(JSONObject jsonObject) throws Exception {
		Double hasil = 0.0;
		try {
			Map<String, Double> data = new HashMap<String, Double>();
			for (Object o : ConstantValues.ambilBerdasarClass(SkorGolongan.class).values()) {
				SkorGolongan skorGolongan = (SkorGolongan) o;
				if (skorGolongan.getAktif()) {
					if (!jsonObject.isNull(skorGolongan.getKode())) {
						data.put(skorGolongan.getKode(), jsonObject.getDouble(skorGolongan.getKode()));
					} else {
						data.put(skorGolongan.getKode(), 0.0);
					}
				}
			}

			Date sekarang = WaktuUtil.getDate();

			if (data != null && !jsonObject.isNull("target")) {
				String target = jsonObject.get("target")+"";
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

					for (Object o : ConstantValues.ambilBerdasarClass(Golongan.class).values()) {
						Golongan golongan = (Golongan) o;
						if (golongan.getAktif()) {
							if (StringUtils.contains(target, " " + golongan.getKode() + " ")) {
								Double nilai = GolonganUtil.ambilPoint(golongan.getFormula(), sekarang, 0);
								target = target.replaceAll(" " + golongan.getKode() + " ", " " + nilai + " ");
							}
						}
					}

					try {
						Expression e = new ExpressionBuilder(target).variables(data.keySet())
								.functions(LogicalUtil.ALL_FUNCTION).operator(LogicalUtil.ALL_OPERATOR).build();

						for (String kode : data.keySet()) {
							try {
								e.setVariable(kode, data.get(kode));
							} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/employ/helper/GolonganUtil.java:114");
							}
						}
						try {
							boolean valid = e.validate().isValid();
							System.out.println("formula = " + target + " valid => " + valid+" data -> "+data);
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
							ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/master/employ/helper/GolonganUtil.java:131");
						}

						hasil = e.evaluate();
					} catch (Exception ee) {
						ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/master/employ/helper/GolonganUtil.java:136");
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/employ/helper/GolonganUtil.java:141");
		}
		return hasil;

	}

	/** Menghitung poin dari riwayat {@code formula} pada {@code sekarang}, mulai dari kedalaman rekursi 0. */
	public static Double ambilPoint(String formula, Date sekarang) throws Exception {
		return ambilPoint(formula, sekarang, 0);
	}

	/**
	 * Implementasi kanonik penghitungan poin dari riwayat {@code formula} JSON: memilih set data
	 * skor yang efektif pada {@code sekarang} (entri {@code tgl} terbaru yang tidak melewati
	 * {@code sekarang}), menentukan ekspresi target yang berlaku lewat {@link #ambilTarget}, lalu
	 * mendelegasikan evaluasi ke {@link #hitung}.
	 *
	 * @param formula  riwayat formula (JSON array berisi {@code tgl} dan nilai per kode {@link SkorGolongan})
	 * @param sekarang tanggal acuan
	 * @param coba     kedalaman rekursi saat ini (lihat javadoc kelas soal pengaman rekursi); hentikan dan kembalikan 0.0 bila melebihi 25
	 * @return hasil evaluasi ekspresi, atau 0.0 bila tidak ada data efektif atau rekursi terlalu dalam
	 */
	@SuppressWarnings({})
	public static Double ambilPoint(String formula, Date sekarang, int coba) throws Exception {
		if (coba > 25) {
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

				for (Object o : ConstantValues.ambilBerdasarClass(SkorGolongan.class).values()) {
					try {
						SkorGolongan skorGolongan = (SkorGolongan) o;
						if (skorGolongan.getAktif()) {
							if (!jsonObject.isNull(skorGolongan.getKode())) {
								data.put(skorGolongan.getKode(), jsonObject.getDouble(skorGolongan.getKode()));
							} else {
								data.put(skorGolongan.getKode(), 0.0);
							}
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/employ/helper/GolonganUtil.java:181");
						// TODO: handle exception
					}
				}

			}
		}

		Map<String, Double> data = null;
		for (String ss : formulas.keySet()) {
			Date tanggalEfektif = Common.dateFormat8.get().parse(ss);
			if (tanggalEfektif.before(sekarang) || Common.dateFormat1.get().format(tanggalEfektif).equals(s)) {
				data = formulas.get(ss);
				break;
			}
		}

		Double hasil = 0.0;
		if (data != null) {
			String target = ambilTarget(formula, sekarang);
			hasil = GolonganUtil.hitung(data, target, sekarang, coba);
		}
		formulas = null;
		jsonArray = null;
		return hasil;
	}

	/**
	 * Mengevaluasi ekspresi matematis {@code target} terhadap {@code data} (nilai skor per kode)
	 * memakai exp4j: menormalkan spasi di sekitar operator, mensubstitusi kode {@link Konstanta}
	 * aktif menjadi nilainya, mensubstitusi kode {@link Golongan} aktif menjadi hasil evaluasi
	 * REKURSIF formula golongan tersebut (kedalaman {@code coba} dinaikkan satu setiap rekursi —
	 * lihat javadoc kelas soal batas 25 tingkat), lalu membangun dan mengevaluasi
	 * {@link Expression} dengan variabel dari {@code data.keySet()}.
	 *
	 * @param data     nilai skor per kode {@link SkorGolongan}, dipakai sebagai variabel ekspresi
	 * @param target   ekspresi matematis yang akan dievaluasi
	 * @param sekarang tanggal acuan, diteruskan ke evaluasi rekursif formula golongan lain
	 * @param coba     kedalaman rekursi saat ini
	 * @return hasil evaluasi, atau 0.0 bila {@code target} kosong atau evaluasi gagal
	 */
	public static Double hitung(Map<String, Double> data, String target, Date sekarang, int coba) throws Exception {
		Double hasil = 0.0;
		if (target != null && !target.trim().isEmpty()) {
			target = " " + target + " ";
			target = target.replaceAll("\\(", " ( ");
			target = target.replaceAll("\\)", " ) ");
			target = target.replaceAll("\\+", " + ");
			target = target.replaceAll("\\-", " - ");
			target = target.replaceAll("\\*", " * ");
			target = target.replaceAll("/", " / ");
			target = target.replaceAll("%", " % ");
			target = " " + target + " ";
			for (Object o : ConstantValues.ambilBerdasarClass(Konstanta.class).values()) {
				Konstanta konstanta = (Konstanta) o;
				if (konstanta.getAktif() && konstanta.getKode() != null) {
					if (StringUtils.contains(target, " " + konstanta.getKode() + " ")) {
						target = org.apache.commons.lang3.StringUtils.replace(target, " " + konstanta.getKode() + " ",
								" " + konstanta.getKeterangan() + " ");
					}
				}

			}

			for (Object o : ConstantValues.ambilBerdasarClass(Golongan.class).values()) {
				Golongan golongan = (Golongan) o;
				if (golongan.getAktif()) {
					if (StringUtils.contains(target, " " + golongan.getKode() + " ")) {
						Double nilai = GolonganUtil.ambilPoint(golongan.getFormula(), sekarang, ++coba);
						target = org.apache.commons.lang3.StringUtils.replace(target, " " + golongan.getKode() + " ", " " + nilai + " ");
					}
				}
			}

			try {
				Expression e = new ExpressionBuilder(target).variables(data.keySet())
						.functions(LogicalUtil.ALL_FUNCTION).operator(LogicalUtil.ALL_OPERATOR).build();

				for (String kode : data.keySet()) {
					try {
						e.setVariable(kode, data.get(kode));
					} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/employ/helper/GolonganUtil.java:248");
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
					ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/master/employ/helper/GolonganUtil.java:265");
				}
				hasil = e.evaluate();
			} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/employ/helper/GolonganUtil.java:268");
			}
		}
		return hasil;
	}

	/**
	 * Membangun ringkasan HTML (daftar bernomor) yang menampilkan setiap entri riwayat
	 * {@code formula}: tanggal efektif, nilai tiap {@link SkorGolongan} aktif yang diisi, dan
	 * ekspresi {@code target}-nya. Dipakai untuk menampilkan riwayat formula secara manusiawi di
	 * UI (bukan untuk perhitungan).
	 *
	 * @param formula riwayat formula (JSON array)
	 * @return markup HTML {@code <ol>...</ol>} berisi ringkasan tiap entri
	 */
	public static String ambilDeskripsi(String formula) throws Exception {
		JSONArray jsonArray = new JSONArray(formula);
		String t = "<ol>";
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonObject = jsonArray.getJSONObject(i);
			String d = "";
			if (!jsonObject.isNull("tgl")) {
				d = "Tgl:" + jsonObject.get("tgl");
			}

			for (Object o : ConstantValues.ambilBerdasarClass(SkorGolongan.class).values()) {
				SkorGolongan skorGolongan = (SkorGolongan) o;
				if (skorGolongan.getAktif()) {
					if (!jsonObject.isNull(skorGolongan.getKode())) {
						d += ", " + skorGolongan.getNama() + ": " + jsonObject.get(skorGolongan.getKode());
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

		t += "</ol>";
		return t;
	}

}
