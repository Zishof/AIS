package ais.action.master.pmb.nim;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Mahasiswa;

public class JENJANG_PROFESI_PRODI_URUT_YYYY_NimGenerator implements NimGenerator {

	private static final String NILAI_KOSONG = "-";

	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa) {
		return generateNim(calonMahasiswa, new ArrayList<String>());
	}

	@Override
	public String generateNim(BiodataCalonMahasiswa calonMahasiswa, List<String> jumlahPengecualian) {
		if (calonMahasiswa == null || calonMahasiswa.getProdiLulus() == null || calonMahasiswa.getTahun() == null) {
			return NILAI_KOSONG;
		}

		if (jumlahPengecualian == null) {
			jumlahPengecualian = new ArrayList<String>();
		}

		Session session = HibernateUtil.openSession();
		try {
			Integer tahun = calonMahasiswa.getTahun();
			boolean profesi = isJenjangProfesi(calonMahasiswa);

			if (profesi) {
				return generateNimProfesi(session, calonMahasiswa, tahun, jumlahPengecualian);
			}

			return generateNimNonProfesi(session, calonMahasiswa, tahun, jumlahPengecualian);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Format non-profesi STAI:
	 * YYYY + kode fakultas + kode prodi + nomor urut
	 * Contoh: 20240101001 = 2024 + 01 + 01 + 001
	 */
	private String generateNimNonProfesi(Session session, BiodataCalonMahasiswa calonMahasiswa, Integer tahun,
			List<String> jumlahPengecualian) {
		String prefix = getKonfigurasi("prefix_pmb", "");
		String kodeFakultas = resolveKodeFakultas(calonMahasiswa);
		String kodeProdi = resolveKodeProdiNonProfesi(calonMahasiswa);
		Integer jumlahDigitUrut = getKonfigurasiInteger("jumlah_digit_gen_nim_mahasiswa_non_profesi", 3);

		Long jumlah = hitungMahasiswaAktifByTahunDanProdi(session, tahun, calonMahasiswa);
		long nomorUrut = jumlah.longValue() + jumlahPengecualian.size() + 1L;

		while (true) {
			String digitUrut = leftPad(String.valueOf(nomorUrut), jumlahDigitUrut.intValue(), '0');
			String nim = prefix + tahun.toString() + kodeFakultas + kodeProdi + digitUrut;

			if (!nimSudahDipakai(session, nim) && !jumlahPengecualian.contains(nim)) {
				logGenerate("NON-PROFESI", tahun.toString(), kodeFakultas, kodeProdi, digitUrut, nim);
				return nim;
			}

			jumlahPengecualian.add(nim);
			nomorUrut++;
		}
	}

	/**
	 * Format profesi:
	 * kode program + nomor urut + YYYY
	 * Contoh Program Bahasa Arab/PBA: 100012025 = 1 + 0001 + 2025
	 * Contoh Program SANAD:            200012025 = 2 + 0001 + 2025
	 */
	private String generateNimProfesi(Session session, BiodataCalonMahasiswa calonMahasiswa, Integer tahun,
			List<String> jumlahPengecualian) {
		String prefix = getKonfigurasi("prefix_pmb", "");
		String kodeProgramProfesi = resolveKodeProgramProfesi(calonMahasiswa);
		Integer jumlahDigitUrut = getKonfigurasiInteger("jumlah_digit_gen_nim_mahasiswa_profesi", 4);

		if (isBlank(kodeProgramProfesi)) {
			return NILAI_KOSONG;
		}

		Long jumlah = hitungMahasiswaAktifProfesi(session, tahun, prefix, kodeProgramProfesi);
		long nomorUrut = jumlah.longValue() + jumlahPengecualian.size() + 1L;

		while (true) {
			String digitUrut = leftPad(String.valueOf(nomorUrut), jumlahDigitUrut.intValue(), '0');
			String nim = prefix + kodeProgramProfesi + digitUrut + tahun.toString();

			if (!nimSudahDipakai(session, nim) && !jumlahPengecualian.contains(nim)) {
				logGenerate("PROFESI", tahun.toString(), "-", kodeProgramProfesi, digitUrut, nim);
				return nim;
			}

			jumlahPengecualian.add(nim);
			nomorUrut++;
		}
	}

	private boolean isJenjangProfesi(BiodataCalonMahasiswa calonMahasiswa) {
		Object jenjang = getObjectProperty(calonMahasiswa, "getJenjang");
		String namaJenjang = getStringProperty(jenjang, "getNama", "getName", "getKode", "getCode");
		return namaJenjang != null && namaJenjang.toLowerCase().contains("profesi");
	}

	private String resolveKodeProgramProfesi(BiodataCalonMahasiswa calonMahasiswa) {
		Object prodi = calonMahasiswa.getProdiLulus();
		String kode = getStringProperty(prodi, "getKode", "getCode");
		String nama = getStringProperty(prodi, "getNama", "getName");
		String identitas = ((kode == null ? "" : kode) + " " + (nama == null ? "" : nama)).toLowerCase();

		if (containsAny(identitas, new String[] { "program bahasa arab", "bahasa arab", "pba" })) {
			return getKonfigurasi("kode_profesi_program_bahasa_arab", "1");
		}

		if (containsAny(identitas, new String[] { "sanad" })) {
			return getKonfigurasi("kode_profesi_program_sanad", "2");
		}

		if ("1".equals(trim(kode)) || "01".equals(trim(kode))) {
			return "1";
		}

		if ("2".equals(trim(kode)) || "02".equals(trim(kode))) {
			return "2";
		}

		return trim(getKonfigurasi("kode_profesi_default", ""));
	}

	private String resolveKodeFakultas(BiodataCalonMahasiswa calonMahasiswa) {
		Object prodi = calonMahasiswa.getProdiLulus();
		Object fakultas = getObjectProperty(prodi, "getFakultas", "getFaculty");
		String kodeFakultas = getStringProperty(fakultas, "getKode", "getCode");

		if (isBlank(kodeFakultas)) {
			kodeFakultas = getKonfigurasi("kode_fakultas_default_gen_nim_stai", "01");
		}

		return normalizeNumericCode(kodeFakultas, 2);
	}

	private String resolveKodeProdiNonProfesi(BiodataCalonMahasiswa calonMahasiswa) {
		/*
		 * Kode prodi diambil langsung dari master Prodi/Jurusan, sesuai request:
		 * String kodeProdi = calonMahasiswa.getProdiLulus().getKode();
		 *
		 * Dengan begitu mapping HKI=02, PAI=03, Ekonomi Syariah=04,
		 * Psikologi Islam=05, BSA=06 cukup diatur pada master data prodi.
		 */
		String kodeProdi = calonMahasiswa.getProdiLulus().getKode();

		if (!isBlank(kodeProdi)) {
			return normalizeNumericCode(kodeProdi, 2);
		}

		return normalizeNumericCode(getKonfigurasi("kode_prodi_default_gen_nim_stai", "01"), 2);
	}

	private Long hitungMahasiswaAktifByTahunDanProdi(Session session, Integer tahun,
			BiodataCalonMahasiswa calonMahasiswa) {
		Number jumlah = (Number) session.createCriteria(Mahasiswa.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("tahunangkatan", tahun))
				.add(Restrictions.eq("jurusan", calonMahasiswa.getProdiLulus()))
				.setProjection(Projections.rowCount()).setMaxResults(1).uniqueResult();

		return Long.valueOf(jumlah == null ? 0L : jumlah.longValue());
	}

	private Long hitungMahasiswaAktifProfesi(Session session, Integer tahun, String prefix, String kodeProgramProfesi) {
		String polaNim = prefix + kodeProgramProfesi + "%" + tahun.toString();

		Number jumlah = (Number) session.createCriteria(Mahasiswa.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("tahunangkatan", tahun)).add(Restrictions.like("nim", polaNim))
				.setProjection(Projections.rowCount()).setMaxResults(1).uniqueResult();

		return Long.valueOf(jumlah == null ? 0L : jumlah.longValue());
	}

	private boolean nimSudahDipakai(Session session, String nim) {
		Number count = (Number) session.createCriteria(Mahasiswa.class).add(Restrictions.eq("nim", nim))
				.setProjection(Projections.count("nim")).setMaxResults(1).uniqueResult();
		return count != null && count.intValue() > 0;
	}

	private String getKonfigurasi(String key, String defaultValue) {
		try {
			return Common.getKonfigurasi(key, defaultValue).getNilai();
		} catch (Exception e) {
			return defaultValue;
		}
	}

	private Integer getKonfigurasiInteger(String key, Integer defaultValue) {
		try {
			return Integer.valueOf(getKonfigurasi(key, String.valueOf(defaultValue)));
		} catch (Exception e) {
			return defaultValue;
		}
	}

	private Object getObjectProperty(Object target, String... getterNames) {
		if (target == null || getterNames == null) {
			return null;
		}

		for (int i = 0; i < getterNames.length; i++) {
			try {
				Method method = target.getClass().getMethod(getterNames[i], new Class[0]);
				Object value = method.invoke(target, new Object[0]);
				if (value != null) {
					return value;
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/nim/JENJANG_PROFESI_PRODI_URUT_YYYY_NimGenerator.java:228");
				// Getter tidak tersedia pada model tertentu, lanjut cek getter berikutnya.
			}
		}

		return null;
	}

	private String getStringProperty(Object target, String... getterNames) {
		Object value = getObjectProperty(target, getterNames);
		return value == null ? "" : String.valueOf(value).trim();
	}

	private String normalizeNumericCode(String kode, int panjang) {
		String nilai = trim(kode);
		if (isBlank(nilai)) {
			return leftPad("", panjang, '0');
		}

		String digitOnly = nilai.replaceAll("[^0-9]", "");
		if (!isBlank(digitOnly)) {
			String result = leftPad(digitOnly, panjang, '0');
			if (result.length() > panjang) {
				return result.substring(result.length() - panjang);
			}
			return result;
		}

		return nilai;
	}

	private String leftPad(String nilai, int panjang, char karakter) {
		if (nilai == null) {
			nilai = "";
		}

		StringBuffer sb = new StringBuffer(nilai);
		while (sb.length() < panjang) {
			sb.insert(0, karakter);
		}

		return sb.toString();
	}

	private boolean containsAny(String text, String[] daftarKeyword) {
		if (text == null || daftarKeyword == null) {
			return false;
		}

		for (int i = 0; i < daftarKeyword.length; i++) {
			if (daftarKeyword[i] != null && text.contains(daftarKeyword[i].toLowerCase())) {
				return true;
			}
		}

		return false;
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().length() == 0;
	}

	private String trim(String value) {
		return value == null ? "" : value.trim();
	}

	private void logGenerate(String mode, String tahun, String kodeFakultas, String kodeProdiAtauProgram,
			String digitUrut, String nim) {
		if (!Boolean.valueOf(getKonfigurasi("debug_gen_nim_mahasiswa", "false")).booleanValue()) {
			return;
		}

		System.out.println("Mode NIM = " + mode);
		System.out.println("digit tahun masuk = " + tahun);
		System.out.println("digit fakultas = " + kodeFakultas);
		System.out.println("digit prodi / program = " + kodeProdiAtauProgram);
		System.out.println("digit urutan = " + digitUrut);
		System.out.println("NIM hasil generate = " + nim);
	}
}
