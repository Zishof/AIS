package ais.action.master.sekolah.util;

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
import ais.database.model.sekolah.DetailGrupKategoriItemPenilaianSiswa;
import ais.database.model.sekolah.DetailGrupPenilaian;
import ais.database.model.sekolah.GrupPenilaian;
import ais.database.model.sekolah.JenisItemPenilaianSiswa;
import ais.database.model.sekolah.Matapelajaran;
import ais.ui.util.WaktuUtil;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

/**
 * Mesin evaluasi formula "Grup Penilaian" modul Sekolah: menghitung nilai akhir siswa dari formula
 * matematis bebas yang dikonfigurasi admin (mis. {@code (uh1+uh2)/2*0.4+uts*0.3+uas*0.3}), memakai
 * pustaka ekspresi {@code exp4j} ({@link Expression}/{@link ExpressionBuilder}). Formula disimpan
 * sebagai {@link JSONArray} berisi beberapa versi ber-tanggal ("riwayat formula") — {@code target}
 * (untuk nilai)/{@code target_min}/{@code target_max} (untuk validasi rentang nilai) — sehingga
 * perubahan formula di masa depan tidak mengubah perhitungan nilai historis; versi yang dipakai
 * dipilih berdasarkan tanggal efektif terbaru yang tidak melebihi tanggal evaluasi ({@code sekarang}).
 *
 * <p>
 * Sebelum dievaluasi, string formula diberi spasi di sekitar operator/tanda kurung agar exp4j
 * dapat mem-parsing token dengan benar, lalu placeholder {@code kkm}/{@code KKM} diganti nilai KKM
 * mata pelajaran, kode {@link Konstanta} aktif diganti nilai keterangannya, dan kode
 * {@link JenisItemPenilaianSiswa} (mis. {@code uh1}, {@code uts}) diganti nilai input siswa
 * (default {@code "1"} bila belum diisi) — untuk kode yang termasuk kategori penilaian yang
 * dipilih ({@code selectedJenisItemPenilaianSiswa}) atau seluruh kategori grup penilaian
 * ({@code grupPenilaianData}), substitusi dilakukan rekursif lewat {@link #hitung} agar formula
 * bertingkat (formula yang merujuk komponen bernilai formula lain) dapat dihitung. Grup penilaian
 * lain yang dirujuk lewat kodenya (dalam {@link #ambilPoint(String, Date, Map, int)}) juga
 * dievaluasi rekursif, dibatasi 25 level ({@code coba > 25}) untuk mencegah rekursi tak terbatas
 * pada formula yang saling merujuk.
 * </p>
 */
public class GrupPenilaianUtil {

	/** Mengambil nilai {@code target} (formula nilai) yang berlaku pada tanggal {@code sekarang} dari riwayat formula ber-tanggal ({@code formula} JSON), memilih versi bertanggal efektif terbaru yang tidak melebihi {@code sekarang}. */
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

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/util/GrupPenilaianUtil.java:54");
		}
		return hasil;
	}
	
	/** Sama seperti {@link #ambilTarget}, tetapi mengambil field {@code target_min} (batas bawah nilai valid) yang berlaku pada {@code sekarang}. */
	public static String ambilTargetMin(String formula, Date sekarang) throws Exception {
		String hasil = "";
		try {
			String s = Common.dateFormat1.get().format(sekarang);
			JSONArray jsonArray = new JSONArray(formula);
			TreeMap<String, String> targets = new TreeMap<String, String>(Collections.reverseOrder());
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject jsonObject = jsonArray.getJSONObject(i);
				if (!jsonObject.isNull("tgl") && !jsonObject.isNull("target_min")) {
					Date tgl = Common.dateFormat1.get().parse(jsonObject.get("tgl").toString());
					targets.put(Common.dateFormat8.get().format(tgl), jsonObject.getString("target_min"));
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

		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/util/GrupPenilaianUtil.java:83");
			// TODO: handle exception
		}
		return hasil;
	}
	
	
	/** Sama seperti {@link #ambilTarget}, tetapi mengambil field {@code target_max} (batas atas nilai valid) yang berlaku pada {@code sekarang}. */
	public static String ambilTargetMax(String formula, Date sekarang) throws Exception {
		String hasil = "";
		try {
			String s = Common.dateFormat1.get().format(sekarang);
			JSONArray jsonArray = new JSONArray(formula);
			TreeMap<String, String> targets = new TreeMap<String, String>(Collections.reverseOrder());
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject jsonObject = jsonArray.getJSONObject(i);
				if (!jsonObject.isNull("tgl") && !jsonObject.isNull("target_max")) {
					Date tgl = Common.dateFormat1.get().parse(jsonObject.get("tgl").toString());
					targets.put(Common.dateFormat8.get().format(tgl), jsonObject.getString("target_max"));
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

		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/util/GrupPenilaianUtil.java:114");
			// TODO: handle exception
		}
		return hasil;
	}

	/** Menghitung nilai dari satu baris input {@code jsonObject} (kode item penilaian → nilai mentah, plus {@code target}) tanpa data grup penilaian tambahan; lihat overload lengkap. */
	public static Double ambilPoint(JSONObject jsonObject, Matapelajaran matapelajaran, GrupPenilaian grupPenilaian,
			HashMap<Long, DetailGrupPenilaian> selectedJenisItemPenilaianSiswa) throws Exception {
		return ambilPoint(jsonObject, matapelajaran, grupPenilaian, selectedJenisItemPenilaianSiswa, null);
	}

	/**
	 * Menghitung nilai akhir dari satu baris input {@code jsonObject} (nilai mentah tiap
	 * {@link JenisItemPenilaianSiswa} aktif) memakai formula {@code target} pada objek yang sama,
	 * lewat {@link #hitung}.
	 *
	 * @param jsonObject                       objek berisi nilai mentah per kode item penilaian dan field {@code target}
	 * @param matapelajaran                    mata pelajaran (sumber nilai KKM), boleh {@code null} (default KKM 70)
	 * @param grupPenilaianData                grup penilaian konteks (dipakai bila {@code selectedJenisItemPenilaianSiswa} kosong)
	 * @param selectedJenisItemPenilaianSiswa  peta kategori item penilaian yang dipilih untuk substitusi rekursif
	 * @param dataItemGrupPenilaian            data tambahan diteruskan apa adanya ke pemanggilan rekursif
	 * @return nilai hasil evaluasi formula, atau {@code 0.0} bila {@code target} tidak ada
	 */
	@SuppressWarnings("rawtypes")
	public static Double ambilPoint(JSONObject jsonObject, Matapelajaran matapelajaran, GrupPenilaian grupPenilaianData,
			HashMap<Long, DetailGrupPenilaian> selectedJenisItemPenilaianSiswa, Map dataItemGrupPenilaian)
			throws Exception {
		Double hasil = 0.0;
		try {
			Map<String, String> data = new HashMap<String, String>();
			for (Object o : ConstantValues.ambilBerdasarClass(JenisItemPenilaianSiswa.class).values()) {
				JenisItemPenilaianSiswa jenisItemPenilaianSiswa = (JenisItemPenilaianSiswa) o;
				if (jenisItemPenilaianSiswa.getAktif()) {
					if (!jsonObject.isNull(jenisItemPenilaianSiswa.getKode())) {
						data.put(jenisItemPenilaianSiswa.getKode(),
								jsonObject.get(jenisItemPenilaianSiswa.getKode()) + "");
					}
				}
			}

			Date sekarang = WaktuUtil.getDate();

			if (data != null && !jsonObject.isNull("target")) {
				String target = jsonObject.get("target")+"";
				hasil = hitung(data, matapelajaran, target, grupPenilaianData, dataItemGrupPenilaian, sekarang,
						selectedJenisItemPenilaianSiswa);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/util/GrupPenilaianUtil.java:150");
		}
		return hasil;

	}

	/**
	 * Inti mesin evaluasi formula: menormalkan spasi di sekitar operator/tanda kurung, mengganti
	 * placeholder {@code kkm}/{@code KKM} dengan KKM mata pelajaran, mengganti kode
	 * {@link Konstanta} aktif dengan nilainya, lalu — bila {@code target} sudah berupa angka murni,
	 * mengembalikannya langsung. Selain itu, untuk setiap kode {@link JenisItemPenilaianSiswa} yang
	 * termasuk kategori penilaian yang relevan (dari {@code selectedJenisItemPenilaianSiswa} atau
   	 * {@code grupPenilaianData}), nilainya dihitung rekursif (memanggil {@code hitung} lagi dengan
	 * formula = nilai input kode tersebut, default {@code "1"} bila kosong) dan disubstitusikan ke
	 * {@code target} sebelum akhirnya dievaluasi lewat {@link ExpressionBuilder}. Kegagalan
	 * evaluasi (formula tidak valid) dicatat ke log/audit dan mengembalikan {@code 0.0}.
	 *
	 * @param data                             peta kode item penilaian → nilai mentah (string)
	 * @param matapelajaran                    sumber nilai KKM, boleh {@code null}
	 * @param target                           string formula yang akan dievaluasi
	 * @param grupPenilaianData                grup penilaian konteks untuk resolusi kategori item
	 * @param dataItemGrupPenilaian            diteruskan apa adanya ke pemanggilan rekursif
	 * @param sekarang                         tanggal evaluasi, dipakai pemanggilan rekursif
	 * @param selectedJenisItemPenilaianSiswa  peta kategori item penilaian terpilih
	 * @return nilai hasil evaluasi, atau {@code 0.0} bila {@code target} kosong atau evaluasi gagal
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Double hitung(Map<String, String> data, Matapelajaran matapelajaran, String target,
			GrupPenilaian grupPenilaianData, Map dataItemGrupPenilaian, Date sekarang,
			HashMap<Long, DetailGrupPenilaian> selectedJenisItemPenilaianSiswa) throws Exception {
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
			target = target.replaceAll(",", " , ");

			target = org.apache.commons.lang3.StringUtils.replace(target, "!=", " != ");
			target = org.apache.commons.lang3.StringUtils.replace(target, ">=", " >= ");
			target = org.apache.commons.lang3.StringUtils.replace(target, "<=", " <= ");
			if (!StringUtils.contains(target, ">=")) {
				target = org.apache.commons.lang3.StringUtils.replace(target, ">", " > ");
			}
			if (!StringUtils.contains(target, "<=")) {
				target = org.apache.commons.lang3.StringUtils.replace(target, "<", " < ");
			}

			if (!StringUtils.contains(target, "<=") && !StringUtils.contains(target, ">=")
					&& !StringUtils.contains(target, "!=")) {
				target = org.apache.commons.lang3.StringUtils.replace(target, "=", " = ");
			}

			Double kkm = 70.0;
			if (matapelajaran != null) {
				kkm = matapelajaran.getKkm();
			}

			if (StringUtils.contains(target, " kkm ")) {
				target = org.apache.commons.lang3.StringUtils.replace(target, " kkm ", " " + kkm + " ");
			}

			if (StringUtils.contains(target, " KKM ")) {
				target = org.apache.commons.lang3.StringUtils.replace(target, " KKM ", " " + kkm + " ");
			}

			if (Common.isNumber(target.trim())) {
				return Double.parseDouble(target.trim());
			}

			for (Object o : ConstantValues.ambilBerdasarClass(Konstanta.class).values()) {
				Konstanta konstanta = (Konstanta) o;
				if (konstanta.getAktif() && konstanta.getKode() != null) {
					if (StringUtils.contains(target, " " + konstanta.getKode() + " ")) {
						target = org.apache.commons.lang3.StringUtils.replace(target, " " + konstanta.getKode() + " ",
								" " + konstanta.getKeterangan() + " ");
					}
				}

			}

			Map<Long, JenisItemPenilaianSiswa> mapJenisItemPenilaianSiswa = ConstantValues
					.ambilBerdasarClass(JenisItemPenilaianSiswa.class);
			Map<Long, DetailGrupKategoriItemPenilaianSiswa> mapGrupKategoriItemPenilaianSiswa = ConstantValues
					.ambilBerdasarClass(DetailGrupKategoriItemPenilaianSiswa.class);

			if (selectedJenisItemPenilaianSiswa != null && !selectedJenisItemPenilaianSiswa.isEmpty()) {
				for (DetailGrupPenilaian detailGrupPenilaian : selectedJenisItemPenilaianSiswa.values()) {

					for (DetailGrupKategoriItemPenilaianSiswa detailGrupKategoriItemPenilaianSiswa : mapGrupKategoriItemPenilaianSiswa
							.values()) {
						try {
							if (detailGrupPenilaian.getGrupKategoriItemPenilaianSiswa().getId().equals(
									detailGrupKategoriItemPenilaianSiswa.getGrupKategoriItemPenilaianSiswa().getId())) {

								for (JenisItemPenilaianSiswa jenisItemPenilaianSiswa : mapJenisItemPenilaianSiswa
										.values()) {

									try {
										if (jenisItemPenilaianSiswa.getKategoriItemPenilaianSiswa().getId()
												.equals(detailGrupKategoriItemPenilaianSiswa
														.getKategoriItemPenilaianSiswa().getId())) {

											if (StringUtils.contains(target,
													" " + jenisItemPenilaianSiswa.getKode() + " ")) {

												String inputNilai = data.get(jenisItemPenilaianSiswa.getKode());

												Double nilai = hitung(data, matapelajaran,
														inputNilai == null ? "1" : inputNilai, grupPenilaianData,
														dataItemGrupPenilaian, sekarang,
														selectedJenisItemPenilaianSiswa);
												target = target.replaceAll(
														" " + jenisItemPenilaianSiswa.getKode() + " ",
														" " + nilai + " ");
											}
										}
									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/util/GrupPenilaianUtil.java:253");
									}
								}

							}
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/util/GrupPenilaianUtil.java:259");
						}
					}
				}
			} else if (grupPenilaianData != null && grupPenilaianData.getId() != null) {
				for (Object o : ConstantValues.ambilBerdasarClass(DetailGrupPenilaian.class).values()) {
					DetailGrupPenilaian detailGrupPenilaian = (DetailGrupPenilaian) o;
					if (detailGrupPenilaian.getAktif()
							&& detailGrupPenilaian.getGrupPenilaian().getId().equals(grupPenilaianData.getId())) {

						for (DetailGrupKategoriItemPenilaianSiswa detailGrupKategoriItemPenilaianSiswa : mapGrupKategoriItemPenilaianSiswa
								.values()) {
							try {
								if (detailGrupPenilaian.getGrupKategoriItemPenilaianSiswa().getId()
										.equals(detailGrupKategoriItemPenilaianSiswa.getGrupKategoriItemPenilaianSiswa()
												.getId())) {

									for (JenisItemPenilaianSiswa jenisItemPenilaianSiswa : mapJenisItemPenilaianSiswa
											.values()) {

										try {
											if (jenisItemPenilaianSiswa.getKategoriItemPenilaianSiswa().getId()
													.equals(detailGrupKategoriItemPenilaianSiswa
															.getKategoriItemPenilaianSiswa().getId())) {

												if (StringUtils.contains(target,
														" " + jenisItemPenilaianSiswa.getKode() + " ")) {

													String inputNilai = data.get(jenisItemPenilaianSiswa.getKode());

													Double nilai = hitung(data, matapelajaran,
															inputNilai == null ? "1" : inputNilai, grupPenilaianData,
															dataItemGrupPenilaian, sekarang,
															selectedJenisItemPenilaianSiswa);
													target = target.replaceAll(
															" " + jenisItemPenilaianSiswa.getKode() + " ",
															" " + nilai + " ");
												}
											}
										} catch (Exception e) {
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/util/GrupPenilaianUtil.java:299");
										}
									}

								}
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/util/GrupPenilaianUtil.java:305");
							}
						}

					}
				}
			}

			try {
				Expression e = new ExpressionBuilder(target).variables(data.keySet())
						.functions(LogicalUtil.ALL_FUNCTION).operator(LogicalUtil.ALL_OPERATOR).build();

				for (String kode : data.keySet()) {
					try {
						e.setVariable(kode, Double.parseDouble(data.get(kode)));
					} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/sekolah/util/GrupPenilaianUtil.java:320");
					}
				}
				try {
					boolean valid = e.validate().isValid();
//					System.out.println("formula = " + target + " valid => " + valid);
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
				} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/sekolah/util/GrupPenilaianUtil.java:336");
//					ee.printStackTrace();
				}
				hasil = e.evaluate();
			} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/sekolah/util/GrupPenilaianUtil.java:340");
//				ee.printStackTrace();
			}
		}
		return hasil;
	}

	/** Menghitung nilai dari riwayat formula {@link GrupPenilaian} ber-tanggal (versi yang berlaku pada {@code sekarang}), tanpa data tambahan dan level rekursi 0; lihat overload lengkap. */
	public static Double ambilPoint(String formula, Date sekarang) throws Exception {
		return ambilPoint(formula, sekarang, null, 0);
	}

	/**
	 * Menghitung nilai dari riwayat formula {@link GrupPenilaian} ber-tanggal ({@code formula}
	 * JSON, format sama dengan {@link #ambilTarget}): memilih versi bertanggal efektif terbaru,
	 * mengambil {@code target}-nya lewat {@link #ambilTarget}, mengganti placeholder kode
	 * {@link Konstanta} dan kode {@link GrupPenilaian} lain (memicu evaluasi rekursif grup
	 * tersebut lewat pemanggilan diri sendiri, {@code coba} bertambah tiap level, dibatasi 25 level
	 * untuk mencegah rekursi tak terbatas antar-grup yang saling merujuk), lalu mengevaluasi hasil
	 * akhir lewat {@link ExpressionBuilder} dengan variabel dari nilai item penilaian pada versi
	 * formula terpilih.
	 *
	 * @param formula                riwayat formula {@link GrupPenilaian} dalam bentuk JSON
	 * @param sekarang               tanggal evaluasi
	 * @param dataItemGrupPenilaian  data tambahan diteruskan apa adanya ke pemanggilan rekursif
	 * @param coba                   penghitung kedalaman rekursi antar-grup; berhenti dan mengembalikan {@code 0.0} bila melebihi 25
	 * @return nilai hasil evaluasi, atau {@code 0.0} bila tidak ada versi formula yang berlaku atau evaluasi gagal
	 */
	@SuppressWarnings({ "rawtypes" })
	public static Double ambilPoint(String formula, Date sekarang, Map dataItemGrupPenilaian, int coba)
			throws Exception {
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

				for (Object o : ConstantValues.ambilBerdasarClass(JenisItemPenilaianSiswa.class).values()) {
					JenisItemPenilaianSiswa jenisItemPenilaianSiswa = (JenisItemPenilaianSiswa) o;
					if (jenisItemPenilaianSiswa.getAktif()) {
						if (!jsonObject.isNull(jenisItemPenilaianSiswa.getKode())) {
							data.put(jenisItemPenilaianSiswa.getKode(),
									jsonObject.getDouble(jenisItemPenilaianSiswa.getKode()));
						} else {
							data.put(jenisItemPenilaianSiswa.getKode(), 0.0);
						}
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

				for (Object o : ConstantValues.ambilBerdasarClass(GrupPenilaian.class).values()) {
					GrupPenilaian grupPenilaian = (GrupPenilaian) o;
					if (grupPenilaian.getAktif()) {
						if (StringUtils.contains(target, " " + grupPenilaian.getKode() + " ")) {
							Double nilai = GrupPenilaianUtil.ambilPoint(grupPenilaian.getFormula(), sekarang,
									dataItemGrupPenilaian, ++coba);
							target = target.replaceAll(" " + grupPenilaian.getKode() + " ", " " + nilai + " ");
						}
					}
				}

				try {
					Expression e = new ExpressionBuilder(target).variables(data.keySet())
							.functions(LogicalUtil.ALL_FUNCTION).operator(LogicalUtil.ALL_OPERATOR).build();

					for (String kode : data.keySet()) {
						try {
							e.setVariable(kode, data.get(kode));
						} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/sekolah/util/GrupPenilaianUtil.java:438");
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
						ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/master/sekolah/util/GrupPenilaianUtil.java:455");
					}
					hasil = e.evaluate();
				} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/sekolah/util/GrupPenilaianUtil.java:458");
				}
			}
		}
		formulas = null;
		jsonArray = null;
		return hasil;
	}

	/** Menghasilkan deskripsi HTML (daftar bernomor) dari seluruh versi riwayat formula: tiap versi ditampilkan sebagai tanggal berlaku, nilai tiap item penilaian aktif, dan formula ({@code target})-nya — dipakai sebagai tooltip/penjelasan formula ke pengguna. */
	public static String ambilDeskripsi(String formula) throws Exception {
		JSONArray jsonArray = new JSONArray(formula);
		String t = "<ol style='font-size:x-small;text-align: left;'>";
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonObject = jsonArray.getJSONObject(i);
			String d = "";
			if (!jsonObject.isNull("tgl")) {
				d = "Tgl:" + jsonObject.get("tgl");
			}

			for (Object o : ConstantValues.ambilBerdasarClass(JenisItemPenilaianSiswa.class).values()) {
				JenisItemPenilaianSiswa jenisItemPenilaianSiswa = (JenisItemPenilaianSiswa) o;
				if (jenisItemPenilaianSiswa.getAktif()) {
					if (!jsonObject.isNull(jenisItemPenilaianSiswa.getKode())) {
						d += ", " + jenisItemPenilaianSiswa.getNama() + ": "
								+ jsonObject.get(jenisItemPenilaianSiswa.getKode());
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
