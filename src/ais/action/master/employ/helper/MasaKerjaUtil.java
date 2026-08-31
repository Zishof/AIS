package ais.action.master.employ.helper;

import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.hibernate.EntityMode;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.type.Type;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.LogicalUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konstanta;
import ais.database.model.Pegawai;
import ais.database.model.employ.SkorGolongan;
import ais.ui.util.WaktuUtil;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

/**
 * Tipe khusus untuk masa kerja util. Kelas ini memberi nama dan batas tanggung jawab yang
 * eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> gunakan tipe ini hanya untuk state dan operasi yang sesuai dengan nama
 * domainnya. Logika lintas domain harus didelegasikan ke service atau helper bersama supaya tidak muncul
 * implementasi paralel dengan hasil berbeda.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah pembacaan/pencarian ({@code cariGajiPokok()});
 * validasi/perhitungan ({@code hitung()}); operasi domain lain ({@code masaKerja()}, {@code
 * efektifLebihBaru()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 */
public class MasaKerjaUtil {

	public static Period masaKerja(Pegawai pegawai) {

		Date masuk = pegawai.getAwalmasuk();

		if (masuk == null) {
			masuk = WaktuUtil.getDate();
		}

		Date keluar = WaktuUtil.getDate();
		if (pegawai.getTanggalmasuk() != null && pegawai.getTanggalkeluar() != null) {
			keluar = pegawai.getTanggalkeluar();
		} else if (pegawai.getTanggalmasuk() == null && pegawai.getTanggalmasukSemiTetap() != null
				&& pegawai.getTanggalkeluarSemiTetap() != null) {
			keluar = pegawai.getTanggalkeluarSemiTetap();
		} else if (pegawai.getTanggalmasuk() == null && pegawai.getTanggalmasukSemiTetap() == null
				&& pegawai.getTanggalmasukHonorer() != null && pegawai.getTanggalkeluarHonorer() != null) {
			keluar = pegawai.getTanggalkeluarHonorer();
		} else if (pegawai.getTanggalmasuk() == null && pegawai.getTanggalmasukSemiTetap() == null
				&& pegawai.getTanggalmasukHonorer() == null && pegawai.getTanggalMulaiPengalanKerja() != null
				&& pegawai.getTanggalSampaiPengalanKerja() != null) {
			keluar = pegawai.getTanggalSampaiPengalanKerja();
		}

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		String ActualDate = Common.databaseDateFormat.get().format(masuk);
		java.time.LocalDate dt = java.time.LocalDate.parse(ActualDate, formatter);
		java.time.LocalDate currentdate = keluar == null ? java.time.LocalDate.now()
				: java.time.LocalDate.parse(Common.databaseDateFormat.get().format(keluar));
		Period period = Period.between(dt, currentdate);

		return period;
	}

	public static Double hitung(Pegawai pegawai) throws Exception {
		String target = pegawai.getMasaKerja() == null ? null : pegawai.getMasaKerja().getKeterangan();
		Double hasil = 0.0;
		if (target != null && !target.trim().isEmpty()) {
			target = target.replaceAll("\\(", " ( ");
			target = target.replaceAll("\\)", " ) ");
			target = target.replaceAll("\\+", " + ");
			target = target.replaceAll("\\-", " - ");
			target = target.replaceAll("\\*", " * ");
			target = target.replaceAll("/", " / ");
			target = target.replaceAll("%", " % ");
			target = " " + target + " ";

			if (target.contains(" MASA_KERJA_THN ")) {
				target = org.apache.commons.lang3.StringUtils.replace(target, " MASA_KERJA_THN ", " " + pegawai.ambilMasaKerjaTahun() + " ");
			}
			if (target.contains(" MASA_KERJA_BLN ")) {
				target = org.apache.commons.lang3.StringUtils.replace(target, " MASA_KERJA_BLN ", " " + pegawai.ambilMasaKerjaBulan() + " ");
			}

			if (target.contains(" PK_THN ")) {
				target = org.apache.commons.lang3.StringUtils.replace(target, " PK_THN ",
						" " + pegawai.ambilMasaKerjaTahunPengalamanKerja() + " ");
			}
			if (target.contains(" PK_BLN ")) {
				target = org.apache.commons.lang3.StringUtils.replace(target, " PK_BLN ",
						" " + pegawai.ambilMasaKerjaBulanPengalamanKerja() + " ");
			}

			if (target.contains(" HONOR_THN ")) {
				target = org.apache.commons.lang3.StringUtils.replace(target, " HONOR_THN ", " " + pegawai.ambilMasaKerjaTahunHonorer() + " ");
			}
			if (target.contains(" HONOR_BLN ")) {
				target = org.apache.commons.lang3.StringUtils.replace(target, " HONOR_BLN ", " " + pegawai.ambilMasaKerjaBulanHonorer() + " ");
			}

			if (target.contains(" ST_THN ")) {
				target = org.apache.commons.lang3.StringUtils.replace(target, " ST_THN ", " " + pegawai.ambilMasaKerjaTahunSemiTetap() + " ");
			}
			if (target.contains(" ST_BLN ")) {
				target = org.apache.commons.lang3.StringUtils.replace(target, " ST_BLN ", " " + pegawai.ambilMasaKerjaBulanSemiTetap() + " ");
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
			ClassMetadata classMetadata = HibernateUtil.getClassMetadata(Pegawai.class);
			String[] strings = classMetadata.getPropertyNames();
			Type[] types = classMetadata.getPropertyTypes();
			JSONArray array = new JSONArray(pegawai.getMasaKerja().getFormula());
			for (int i = 0; i < array.length(); i++) {

				JSONObject jsonObject = array.getJSONObject(i);

				if (!jsonObject.isNull("variabel")) {

					String variabel = jsonObject.get("variabel") + "";

					if (!variabel.trim().isEmpty() && StringUtils.contains(target, " " + variabel + " ")) {

						String val = jsonObject.isNull("nilai") ? "" : jsonObject.get("nilai") + "";

						if (!val.trim().isEmpty()) {
							boolean bener = true;
							String[] ss = pegawai.getMasaKerja().getSkor().split(",");
							for (String s : ss) {
								if (!s.trim().isEmpty()) {
									SkorGolongan skorGolongan = (SkorGolongan) ConstantValues
											.ambil(SkorGolongan.class.getName(), Long.parseLong(s));
									if (skorGolongan != null) {

										String valVariabel = jsonObject.isNull(skorGolongan.getKode()) ? ""
												: jsonObject.get(skorGolongan.getKode()) + "";

										if (!valVariabel.isEmpty() && skorGolongan.getParameterTambahan() != null
												&& skorGolongan.getParameterTambahan().getNilaiDataInputan() != null) {

											try {

												int ih = 0;
												for (String sh : strings) {
													Type type = types[ih++];
													if (type.getReturnedClass().getName().equals(skorGolongan
															.getParameterTambahan().getNilaiDataInputan())) {

														GeneralValueObject generalValueObject = (GeneralValueObject) classMetadata
																.getPropertyValue(pegawai, sh, EntityMode.POJO);
														bener &= generalValueObject != null
																&& generalValueObject.getId() != null
																&& generalValueObject.getId().toString()
																		.equals(valVariabel);
													}
												}

											} catch (Exception e) {
												e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/employ/helper/MasaKerjaUtil.java:166");
											}

										}

									}
								}
							}

							System.out.println("bener -> " + bener + ", variabel -> " + variabel + ", val -> " + val);

							if (bener) {
								target = org.apache.commons.lang3.StringUtils.replace(target, " " + variabel + " ", " " + val + " ");
							}
						}
					}
				}
			}

			Map<String, Double> data = new HashMap<String, Double>();

			try {
				Expression e = new ExpressionBuilder(target).variables(data.keySet())
						.functions(LogicalUtil.ALL_FUNCTION).operator(LogicalUtil.ALL_OPERATOR).build();

				for (String kode : data.keySet()) {
					try {
						e.setVariable(kode, data.get(kode));
					} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/employ/helper/MasaKerjaUtil.java:194");
					}
				}
				try {
					List<String> d = e.validate().getErrors();
					boolean valid = e.validate().isValid();
					System.out.println("formula = " + target + " valid => " + valid);
					if (!valid) {
						if (!d.isEmpty()) {
							String ds = "";
							for (String dd : d) {
								ds += ds.isEmpty() ? dd : ".\n" + dd;
							}
							System.out.println("error = " + ds);
						}
					}
				} catch (Exception ee) {
					ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/master/employ/helper/MasaKerjaUtil.java:211");
				}
				hasil = e.evaluate();
			} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/employ/helper/MasaKerjaUtil.java:214");
			}
		}

		if (pegawai.getMasaKerja() != null && pegawai.getMasaKerja().getMaksimal() < hasil) {
			hasil = pegawai.getMasaKerja().getMaksimal();
		}

		return hasil;
	}

	/**
	 * Mencari baris <b>Gaji Pokok</b> dari tabel master (entity {@link ais.database.model.employ.GajiPokok})
	 * yang sesuai dengan <b>golongan/penggajian berdasarkan</b> dan <b>masa kerja (tahun)</b> pegawai.
	 *
	 * <p>Masa kerja diambil dari data kepegawaian via {@link #masaKerja(Pegawai)} (tahun penuh).
	 * Pemilihan baris (mengikuti pengertian "tabel gaji pokok per masa kerja"):</p>
	 * <ol>
	 *   <li>kecocokan PERSIS masa kerja tahun = MK baris; bila ada beberapa, ambil yang tanggal
	 *       efektifnya paling baru namun sudah berlaku (&le; {@code tanggal});</li>
	 *   <li>bila tidak ada yang persis, ambil baris dengan MK TERTINGGI yang masih &le; masa kerja
	 *       pegawai (bracket yang berlaku);</li>
	 *   <li>bila masa kerja pegawai lebih kecil dari MK terendah pada tabel, pakai baris MK terendah
	 *       sebagai cadangan.</li>
	 * </ol>
	 *
	 * @param pegawai pegawai (untuk menghitung masa kerja); boleh null → hasil null
	 * @param golongan golongan / penggajian berdasarkan yang dipilih; boleh null → hasil null
	 * @param tanggal  tanggal acuan keberlakuan tanggal efektif; null = hari ini
	 * @return baris GajiPokok yang cocok, atau null bila tak ada
	 */
	public static ais.database.model.employ.GajiPokok cariGajiPokok(Pegawai pegawai,
			ais.database.model.employ.Golongan golongan, Date tanggal) {
		if (pegawai == null || golongan == null || golongan.getId() == null) {
			return null;
		}
		if (tanggal == null) {
			tanggal = WaktuUtil.getDate();
		}

		int masaKerjaTahun = 0;
		try {
			Period p = masaKerja(pegawai);
			masaKerjaTahun = p == null ? 0 : p.getYears();
		} catch (Exception e) {
			masaKerjaTahun = 0;
		}

		String sTgl = Common.dateFormat1.get().format(tanggal);
		ais.database.model.employ.GajiPokok exact = null;
		ais.database.model.employ.GajiPokok floor = null; // MK tertinggi yang <= masa kerja pegawai
		ais.database.model.employ.GajiPokok lowest = null; // MK terendah (cadangan)

		for (Object o : ConstantValues.ambilBerdasarClass(ais.database.model.employ.GajiPokok.class).values()) {
			ais.database.model.employ.GajiPokok g = (ais.database.model.employ.GajiPokok) o;
			if (g == null || g.getMasaKerja() == null || g.getGolongan() == null
					|| g.getGolongan().getId() == null) {
				continue;
			}
			if (!g.getGolongan().getId().equals(golongan.getId())) {
				continue;
			}
			// Hanya baris yang tanggal efektifnya sudah berlaku (<= tanggal acuan).
			boolean sudahEfektif = g.getTanggalEfektif() != null && (g.getTanggalEfektif().before(tanggal)
					|| Common.dateFormat1.get().format(g.getTanggalEfektif()).equals(sTgl));
			if (!sudahEfektif) {
				continue;
			}

			int mk = g.getMasaKerja().intValue();

			if (mk == masaKerjaTahun && (exact == null || efektifLebihBaru(g, exact))) {
				exact = g;
			}
			if (mk <= masaKerjaTahun) {
				if (floor == null || mk > floor.getMasaKerja().intValue()
						|| (mk == floor.getMasaKerja().intValue() && efektifLebihBaru(g, floor))) {
					floor = g;
				}
			}
			if (lowest == null || mk < lowest.getMasaKerja().intValue()
					|| (mk == lowest.getMasaKerja().intValue() && efektifLebihBaru(g, lowest))) {
				lowest = g;
			}
		}

		if (exact != null) {
			return exact;
		}
		if (floor != null) {
			return floor;
		}
		return lowest;
	}

	/** True bila tanggal efektif {@code a} lebih baru (atau sama) dibanding {@code b}. */
	private static boolean efektifLebihBaru(ais.database.model.employ.GajiPokok a,
			ais.database.model.employ.GajiPokok b) {
		try {
			Date ta = a == null ? null : a.getTanggalEfektif();
			Date tb = b == null ? null : b.getTanggalEfektif();
			if (ta == null) {
				return false;
			}
			if (tb == null) {
				return true;
			}
			return !ta.before(tb);
		} catch (Exception e) {
			return false;
		}
	}

}
