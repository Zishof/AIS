package ais.common;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
// Asumsi pakai ZK Framework
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Filedownload; // Asumsi pakai ZK Framework
import org.zkoss.zul.Messagebox;

import ais.action.master.helper.PembayaranUtilHelper;
import ais.action.ws.util.ConstantUtil;
import ais.action.ws.util.PembayaranUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.DetailBiaya;
import ais.database.model.Dosen;
import ais.database.model.ErrorLog;
import ais.database.model.FormatNilaiProposalSkripsi;
import ais.database.model.FormatNilaiSkripsi;
import ais.database.model.FormulirKegiatan;
import ais.database.model.JenisKegiatan;
import ais.database.model.Kegiatan;
import ais.database.model.Konfigurasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.MatakuliahEkivalen;
import ais.database.model.PendaftaranCutiMahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.PengecualianJadwalPengisianKRSMahasiswa;
import ais.database.model.PengecualianJadwalPenilaianDosen;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.Tbmrole;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.WaktuUtil;
// Import Hibernate & FileUtils (Apache Commons) diasumsikan sudah ada

public class CommonHelperClass {

	// 1. Menggunakan StringBuilder untuk performa lebih cepat
	public static String convertToQueryString(Map<String, Object> map) {
		if (map == null || map.isEmpty()) {
			return "";
		}

		StringBuilder sb = new StringBuilder();

		for (Map.Entry<String, Object> entry : map.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();

			// Cek null safety agar tidak NullPointerException
			if (key == null || value == null)
				continue;

			// Handle jika value adalah Array String (sesuai kode asli)
			if (value instanceof String[]) {
				String[] values = (String[]) value;
				for (String val : values) {
					appendQueryParam(sb, key, val);
				}
			}
			// IMPROVEMENT: Handle jika value cuma String biasa/Object lain (bukan array)
			// agar code lebih fleksibel
			else {
				appendQueryParam(sb, key, value.toString());
			}
		}

		return sb.toString();
	}

	// Helper kecil untuk append parameter
	private static void appendQueryParam(StringBuilder sb, String key, String val) {
		if (sb.length() > 0) {
			sb.append("&");
		}
		sb.append(key).append("=").append(encodeUTF8(val));
	}

	// Wrapper untuk URLEncoder agar tidak perlu try-catch berulang di loop
	private static String encodeUTF8(String s) {
		try {
			return s != null ? URLEncoder.encode(s, "UTF-8") : "";
		} catch (UnsupportedEncodingException e) {
			return s; // Fallback jika gagal encode
		}
	}

	public static String tampilErrorJikaAdmin(Exception ex) {
		return tampilErrorJikaAdmin(ex, "", false);
	}

	public static String tampilErrorJikaAdmin(Exception ex, String info, boolean download) {
		// 2. Mengambil stack trace menggunakan helper method (DRY)
		String stackTrace = getStackTraceAsString(ex);

		// Gabungkan info tambahan jika ada
		if (info != null && !info.isEmpty()) {
			stackTrace = info + "\n" + stackTrace;
		}

		// Fitur Download File
		if (download) {
			saveAndDownloadErrorFile(stackTrace);
		}

		// Simpan ke Database (Hibernate)
		saveErrorToDatabase(stackTrace);

		try {
			MyMessageboxConfig.showDetail(
					"Terjadi kesalahan pada sistem. Silakan klik Detail lalu copy informasi teknis untuk dikirim ke admin.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR, ex,
					(info == null || info.trim().isEmpty()
							? "Langkah perbaikan yang disarankan:\n"
									+ "1. Ulangi proses setelah memastikan data/form sudah lengkap.\n"
									+ "2. Jika error tetap muncul, klik Copy Detail lalu kirim ke admin/teknis.\n"
									+ "3. Admin dapat mencari log error berdasarkan waktu kejadian pada detail ini."
							: info));
		} catch (Exception alertEx) {
			ais.common.ErrorAuditUtil.record(alertEx,
					"auto-audit(empty-catch) CommonHelperClass.tampilErrorJikaAdmin alert-detail");
		}

		return stackTrace;
	}

	public static EventListener downloadError(final Exception ex) {
		// Java 1.7 belum support Lambda, jadi pakai Anonymous Class
		return new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				String stackTrace = getStackTraceAsString(ex);
				saveAndDownloadErrorFile(stackTrace);
			}
		};
	}

	// --- PRIVATE HELPER METHODS (Agar kode utama lebih bersih) ---

	public static String getStackTraceAsString(Throwable t) {
		if (t == null)
			return "";
		// Try-with-resources (Java 1.7+) menutup writer otomatis
		try {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			t.printStackTrace(pw);
			return sw.toString();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonHelperClass.java:159");
			return "Error getting stack trace: " + e.getMessage();
		}
	}

	private static void saveAndDownloadErrorFile(String content) {
		try {
			// Gunakan File.separator agar aman di Windows/Linux
			String fileName = "error_" + Common.randLong() + ".txt";
			File fileErr = new File(Common.REAL_PATH + File.separator + "tmp" + File.separator + fileName);

			if (!fileErr.getParentFile().exists()) {
				fileErr.getParentFile().mkdirs();
			}

			FileUtils.writeStringToFile(fileErr, content); // Apache Commons IO
			Filedownload.save(fileErr, "text/plain"); // ZK Framework

		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonHelperClass.java:177");
			// e.printStackTrace(); // Log error jika gagal download, jangan sampai
			// menghentikan flow utama
		}
	}

	private static void saveErrorToDatabase(String errContent) {
		// 1. Buka Session Baru (Isolated Session)
		// Menggunakan openSession() agar terpisah dari session HTTP request
		Session session = HibernateUtil.getSessionFactory().openSession();

		try {
			session.getTransaction().begin();

			ErrorLog errorLog = new ErrorLog();
			// Batasi panjang karakter jika kolom database terbatas, opsional
			// if (errContent.length() > 5000) errContent = errContent.substring(0, 5000);
			errorLog.setKeterangan(errContent);

			session.save(errorLog);
			session.getTransaction().commit();

		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonHelperClass.java:199");
			// e.printStackTrace();
		} finally {
			// 2. WAJIB Tutup Session
			if (session != null && session.isOpen()) {
				try {
					session.disconnect();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonHelperClass.java:206");
					// TODO: handle exception
				}
				try {
					session.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonHelperClass.java:211");
					// TODO: handle exception
				}

			}
		}
	}

	public static Matakuliah[] getMatakuliahApakahEkivalen(Matakuliah matakuliah, String nim, boolean refresh) {
		if (matakuliah != null) {
			if (refresh) {
				matakuliah.reInitEkivalen();
			}

			List<MatakuliahEkivalen> matakuliahEkivalens = matakuliah.ambilEkivalen(nim);

			return new Matakuliah[] { (matakuliahEkivalens != null && !matakuliahEkivalens.isEmpty()
					? matakuliahEkivalens.get(0).getMatakuliahEkivalen()
					: matakuliah), matakuliah };
		} else {
			return new Matakuliah[] { null, null };
		}
	}

	public static TreeSet<JenisKegiatan> jenisKegiatansTanpaDaftarUlang = null;
	public static TreeSet<JenisKegiatan> jenisKegiatansAktif = null;
	public static TreeSet<JenisKegiatan> jenisKegiatansUntukKrs = null;
	public static TreeSet<JenisKegiatan> jenisKegiatansUntukNilai = null;
	public static TreeSet<JenisKegiatan> jenisKegiatansUntukSyaratAktif = null;
	public static TreeSet<JenisKegiatan> jenisKegiatansUntukSyaratUjian = null;

	public static void reloadJenisKegiatans() {
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			reloadJenisKegiatans(session);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonHelperClass.java:248");
		} finally {
			HibernateUtil.closeSession();
		}
	}

	@SuppressWarnings("unchecked")
	public static void reloadJenisKegiatans(Session session) {
		try {
			if (session == null || !session.isOpen())
				return;

			ConstantUtil.initIstilahPendaftaran(session);

			jenisKegiatansTanpaDaftarUlang = new TreeSet<JenisKegiatan>((Collection<JenisKegiatan>) session
					.createCriteria(JenisKegiatan.class).addOrder(Order.asc("namaKegiatan"))
					.add(Restrictions.isNotNull("namaKegiatan")).add(Restrictions.ne("namaKegiatan", ""))
					.add(Restrictions.not(Restrictions.in("id",
							new Long[] {
									ConstantValues.PENDAFTARAN_CALON_MAHASISWA == null ? -1L
											: ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId(),
									ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU == null ? -1L
											: ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId() })))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list());

			jenisKegiatansAktif = new TreeSet<JenisKegiatan>((Collection<JenisKegiatan>) session
					.createCriteria(JenisKegiatan.class).addOrder(Order.asc("namaKegiatan"))
					.add(Restrictions.isNotNull("namaKegiatan")).add(Restrictions.ne("namaKegiatan", ""))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions
							.or(Restrictions.isNull("defaultKegiatan"), Restrictions.eq("defaultKegiatan", false)))
					.list());

			if (ConstantValues.PENDAFTARAN_MAHASISWA_LAMA != null
					&& ConstantValues.PENDAFTARAN_MAHASISWA_LAMA.getId() != null) {
				session.refresh(ConstantValues.PENDAFTARAN_MAHASISWA_LAMA);
				if (!jenisKegiatansAktif.contains(ConstantValues.PENDAFTARAN_MAHASISWA_LAMA)) {
					jenisKegiatansAktif.add(ConstantValues.PENDAFTARAN_MAHASISWA_LAMA);
				}
			}

			if (ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null
					&& ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId() != null) {
				session.refresh(ConstantValues.PENDAFTARAN_CALON_MAHASISWA);
				if (!jenisKegiatansAktif.contains(ConstantValues.PENDAFTARAN_CALON_MAHASISWA)) {
					jenisKegiatansAktif.add(ConstantValues.PENDAFTARAN_CALON_MAHASISWA);
				}
			}

			if (ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU != null
					&& ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId() != null) {
				session.refresh(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU);
				if (!jenisKegiatansAktif.contains(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU)) {
					jenisKegiatansAktif.add(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU);
				}
			}

			if (ConstantValues.PENDAFTARAN_WISUDA != null && ConstantValues.PENDAFTARAN_WISUDA.getId() != null) {
				session.refresh(ConstantValues.PENDAFTARAN_WISUDA);
			}

			jenisKegiatansUntukKrs = new TreeSet<JenisKegiatan>((Collection<JenisKegiatan>) session
					.createCriteria(JenisKegiatan.class).add(Restrictions.isNotNull("namaKegiatan"))
					.add(Restrictions.ne("namaKegiatan", "")).addOrder(Order.asc("namaKegiatan"))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("digunakanUntukPengecekanKrs", true)).list());

			if (jenisKegiatansUntukKrs.isEmpty() && ConstantValues.PENDAFTARAN_MAHASISWA_LAMA != null)
				jenisKegiatansUntukKrs.add(ConstantValues.PENDAFTARAN_MAHASISWA_LAMA);

			jenisKegiatansUntukNilai = new TreeSet<JenisKegiatan>(
					(Collection<JenisKegiatan>) session.createCriteria(JenisKegiatan.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.eq("digunakanUntukPengecekanNilai", true)).list());

			if (jenisKegiatansUntukNilai.isEmpty() && ConstantValues.PENDAFTARAN_MAHASISWA_LAMA != null)
				jenisKegiatansUntukNilai.add(ConstantValues.PENDAFTARAN_MAHASISWA_LAMA);

			jenisKegiatansUntukSyaratAktif = new TreeSet<JenisKegiatan>((Collection<JenisKegiatan>) session
					.createCriteria(JenisKegiatan.class).addOrder(Order.asc("namaKegiatan"))
					.add(Restrictions.isNotNull("namaKegiatan")).add(Restrictions.ne("namaKegiatan", ""))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.or(Restrictions.isNull("digunakanSyaratKeaktifan"),
							Restrictions.eq("digunakanSyaratKeaktifan", true)))
					.list());

			if (jenisKegiatansUntukSyaratAktif.isEmpty() && ConstantValues.PENDAFTARAN_MAHASISWA_LAMA != null) {
				jenisKegiatansUntukSyaratAktif.add(ConstantValues.PENDAFTARAN_MAHASISWA_LAMA);
			}

			jenisKegiatansUntukSyaratUjian = new TreeSet<JenisKegiatan>((Collection<JenisKegiatan>) session
					.createCriteria(JenisKegiatan.class).add(Restrictions.eq("digunakanUntukPengecekanUjian", true))
					.add(Restrictions.isNotNull("namaKegiatan")).add(Restrictions.ne("namaKegiatan", ""))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list());

			if (jenisKegiatansUntukSyaratUjian.isEmpty() && ConstantValues.PENDAFTARAN_MAHASISWA_LAMA != null) {
				jenisKegiatansUntukSyaratUjian.add(ConstantValues.PENDAFTARAN_MAHASISWA_LAMA);
			}

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonHelperClass.java:347");
		}
	}

	public static Combobox initJenisPembayaranMahasiswa(Combobox jenisPembayaranMahasiswa) {
		if (jenisPembayaranMahasiswa == null) {
			jenisPembayaranMahasiswa = new Combobox();
		}

		if (jenisKegiatansTanpaDaftarUlang == null) {
			reloadJenisKegiatans();
		}

		if (jenisKegiatansTanpaDaftarUlang != null) {
			for (JenisKegiatan jenisKegiatan : jenisKegiatansTanpaDaftarUlang) {
				Comboitem comboitem = new Comboitem(jenisKegiatan.getNamaKegiatan());
				comboitem.setValue(jenisKegiatan);
				jenisPembayaranMahasiswa.appendChild(comboitem);
			}
		}

		Common.selectComboItem(jenisPembayaranMahasiswa, ConstantValues.PENDAFTARAN_MAHASISWA_LAMA);
		return jenisPembayaranMahasiswa;
	}

	public static Combobox initJenisPembayaranBiodataCalonMahasiswa(Combobox jenisPembayaranMahasiswa) {
		if (jenisPembayaranMahasiswa == null) {
			jenisPembayaranMahasiswa = new Combobox();
		}

		MyComboitemConfig comboitem = new MyComboitemConfig(ConstantUtil.PENDAFTARAN_CALON_MAHASISWA);
		comboitem.setValue(ConstantValues.PENDAFTARAN_CALON_MAHASISWA);
		jenisPembayaranMahasiswa.appendChild(comboitem);
		jenisPembayaranMahasiswa.setSelectedItem(comboitem);

		comboitem = new MyComboitemConfig(ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU);
		comboitem.setValue(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU);
		jenisPembayaranMahasiswa.appendChild(comboitem);

		if (jenisKegiatansAktif == null) {
			reloadJenisKegiatans();
		}

		return jenisPembayaranMahasiswa;
	}

	public static Combobox initJenisPembayaranMahasiswaDanBiodataCalonMahasiswa(Combobox jenisPembayaranMahasiswa) {
		if (jenisPembayaranMahasiswa == null) {
			jenisPembayaranMahasiswa = new Combobox();
		}

		if (jenisKegiatansTanpaDaftarUlang == null) {
			reloadJenisKegiatans();
		}

		if (jenisKegiatansTanpaDaftarUlang != null) {
			for (JenisKegiatan jenisKegiatan : jenisKegiatansTanpaDaftarUlang) {
				Comboitem comboitem = new Comboitem(jenisKegiatan.getNamaKegiatan());
				comboitem.setValue(jenisKegiatan);
				jenisPembayaranMahasiswa.appendChild(comboitem);
			}
		}

		MyComboitemConfig comboitem = new MyComboitemConfig(ConstantUtil.PENDAFTARAN_CALON_MAHASISWA);
		comboitem.setValue(ConstantValues.PENDAFTARAN_CALON_MAHASISWA);
		jenisPembayaranMahasiswa.appendChild(comboitem);
		jenisPembayaranMahasiswa.setSelectedItem(comboitem);

		comboitem = new MyComboitemConfig(ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU);
		comboitem.setValue(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU);
		jenisPembayaranMahasiswa.appendChild(comboitem);

		Common.selectComboItem(jenisPembayaranMahasiswa, ConstantValues.PENDAFTARAN_MAHASISWA_LAMA);

		return jenisPembayaranMahasiswa;
	}

	public static Combobox initJenisSemester(Combobox jenisSemester) {
		return initJenisSemester(jenisSemester, false);
	}

	public static Combobox initJenisSemester(Combobox jenisSemester, boolean sp) {
		if (jenisSemester == null) {
			jenisSemester = new Combobox();
		}
		jenisSemester.setReadonly(true);

		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		jenisSemester.appendChild(comboitem);

		comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		jenisSemester.appendChild(comboitem);

		if (sp) {
			comboitem = new MyComboitemConfig(Perkuliahan.SP);
			comboitem.setValue(Perkuliahan.SP);
			jenisSemester.appendChild(comboitem);
		}
		Boolean ganjil = Common.isNowSemensterGanjil();
		Common.selectComboItem(jenisSemester, ganjil != null && ganjil ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

		return jenisSemester;
	}

	public static String getSemesterString() {
		Boolean ganjil = Common.isNowSemensterGanjil();
		return (ganjil != null && ganjil) ? Perkuliahan.GANJIL : Perkuliahan.GENAP;
	}

	public static TreeMap<Integer, String[]> generateStatusSemester(Mahasiswa mahasiswa) {
		TreeMap<Integer, String[]> treeMap = new TreeMap<Integer, String[]>();
		if (mahasiswa == null || mahasiswa.getSemesterMulai() == null || mahasiswa.getTahunangkatan() == null) {
			return treeMap;
		}

		boolean mulaiGanjil = mahasiswa.getSemesterMulai().equals(Perkuliahan.GANJIL);
		int semester = mulaiGanjil ? 1 : 0;
		int tahunAkademik = 8;
		boolean berhenti = false;

		String ta = Common.getCurrentTahunAkademik();
		Integer tahunAsli = 0;

		if (ta != null && ta.contains("/")) {
			try {
				tahunAsli = Integer.parseInt(StringUtils.split(ta, "/")[0]);
			} catch (NumberFormatException e) {
				// Fallback value if parse fails
				tahunAsli = Calendar.getInstance().get(Calendar.YEAR);
			}
		}

		Integer tahun = (tahunAsli + tahunAkademik);
		int startIndex = (mahasiswa.getTahunangkatan() - ((int) (mahasiswa.getPindahKeKampusIniMasukSemester() / 2.0)));

		for (int i = startIndex; i < tahun; i++) {
			if (semester >= 50 || berhenti)
				break;

			String status = "";
			String tahunAkademikIndex = i + "/" + (i + 1);
			Integer thp = ConstantValues.getJumlahTahapan(mahasiswa.getProgram(), mahasiswa.getJurusan());

			KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, null, null);

			// Loop dieksekusi 2x untuk Ganjil & Genap per tahun akademik
			for (int j = 0; j < 2; j++) {
				if (semester > 0) {
					String namaSmt = mulaiGanjil ? (semester % 2 == 0 ? Perkuliahan.GANJIL : Perkuliahan.GENAP)
							: (semester % 2 == 1 ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

					status = (thp != null && thp == 3) ? ""
							: (krsMahasiswa != null && ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(krsMahasiswa) != null
									? ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(krsMahasiswa).getStatusMahasiswa().getNama()
									: "");

					treeMap.put(semester, new String[] { tahunAkademikIndex, String.valueOf(semester),
							(status == null ? "" : status), "", namaSmt });

					if (status != null && (status.equalsIgnoreCase("lulus") || status.equalsIgnoreCase("keluar"))) {
						berhenti = true;
						break;
					}
				}
				semester++;
			}
		}
		return treeMap;
	}

	public static List<String[]> generateSemestersForGrid(Mahasiswa mahasiswa, int mulai, int sampai,
			Integer semesterPendek) {
		List<String[]> data = new ArrayList<String[]>();
		try {
			if (mahasiswa == null || mahasiswa.getTahunangkatan() == null) {
				return data;
			}

			if (ConstantValues.jumlahTahapan == null || ConstantValues.jumlahTahapan.isEmpty()) {
				ConstantValues.initJumlahTahapan();
			}

			int tahap = 1;
			Integer thp = ConstantValues.getJumlahTahapan(mahasiswa.getProgram(), mahasiswa.getJurusan());
			TreeMap<Integer, String[]> treeMap = generateStatusSemester(mahasiswa);

			if (!ConstantValues.aktifkanTahapan || (thp != null && thp == 2)) {
				for (Integer semester : treeMap.keySet()) {
					if (semester >= mulai && semester <= sampai) {
						String[] d = Arrays.copyOf(treeMap.get(semester), 5);
						d[3] = "0";
						data.add(d);
					}
					tahap++;
				}
			} else if (thp != null && thp == 3) {
				// (Logika disederhanakan tanpa mengubah tujuan aslinya)
				for (Integer semester : treeMap.keySet()) {
					// Konfigurasi ini dibiarkan serupa agar tidak memutus legacy UI grid
					if (mahasiswa.getSemesterMulai().equals(Perkuliahan.GANJIL)) {
						if (semester % 2 == 1) {
							if (semester >= mulai && semester <= sampai) {
								String[] d = Arrays.copyOf(treeMap.get(semester), 5);
								d[3] = String.valueOf(tahap);
								d[2] = ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(mahasiswa, tahap).getStatusMahasiswa().getNama();
								data.add(d);
							}
							tahap++;
						} else if (semester % 2 == 0) {
							if (semester >= mulai && semester <= sampai) {
								String[] d1 = Arrays.copyOf(treeMap.get(semester), 5);
								d1[1] = (semester - 1) + "," + semester;
								d1[3] = String.valueOf(tahap);
								d1[2] = ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(mahasiswa, tahap).getStatusMahasiswa().getNama();
								data.add(d1);

								tahap++;

								String[] d2 = Arrays.copyOf(treeMap.get(semester), 5);
								d2[3] = String.valueOf(tahap);
								d2[2] = ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(mahasiswa, tahap).getStatusMahasiswa().getNama();
								data.add(d2);
							}
							tahap++;
						}
					} else {
						if (semester.equals(1)) {
							for (int k = 0; k < 3; k++) {
								if (semester >= mulai && semester <= sampai) {
									String[] d = Arrays.copyOf(treeMap.get(semester), 5);
									d[3] = String.valueOf(tahap);
									d[2] = ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(mahasiswa, tahap).getStatusMahasiswa().getNama();
									data.add(d);
								}
								if (k < 2)
									tahap++;
							}
						} else if (semester % 2 == 1) {
							tahap++;
							if (semester >= mulai && semester <= sampai) {
								String[] d1 = Arrays.copyOf(treeMap.get(semester), 5);
								d1[1] = (semester - 1) + "," + semester;
								d1[3] = String.valueOf(tahap);
								d1[2] = ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(mahasiswa, tahap).getStatusMahasiswa().getNama();
								data.add(d1);
							}
							tahap++;
							if (semester >= mulai && semester <= sampai) {
								String[] d2 = Arrays.copyOf(treeMap.get(semester), 5);
								d2[3] = String.valueOf(tahap);
								d2[2] = ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(mahasiswa, tahap).getStatusMahasiswa().getNama();
								data.add(d2);
							}
						} else if (semester % 2 == 0) {
							tahap++;
							if (semester >= mulai && semester <= sampai) {
								String[] d = Arrays.copyOf(treeMap.get(semester), 5);
								d[3] = String.valueOf(tahap);
								d[2] = ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(mahasiswa, tahap).getStatusMahasiswa().getNama();
								data.add(d);
							}
						}
					}
				}
				data.add(new String[] { "Tanpa Tahap", "", "", "-1" });

			} else if ((thp != null && thp == 4)) {
				for (Integer semester : treeMap.keySet()) {
					if (semester >= mulai && semester <= sampai) {
						String[] d1 = Arrays.copyOf(treeMap.get(semester), 4);
						d1[3] = String.valueOf(tahap);
						data.add(d1);
					}
					tahap++;

					if (semester >= mulai && semester <= sampai) {
						String[] d2 = Arrays.copyOf(treeMap.get(semester), 4);
						d2[3] = String.valueOf(tahap);
						data.add(d2);
					}
					tahap++;
				}
				data.add(new String[] { "Tanpa Tahap", "", "", "-1" });
			}

			if (semesterPendek == null) {
				data.add(new String[] { "Konversi", "" });
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonHelperClass.java:638");
		}
		return data;
	}

	public static List<String[]> generateSemestersForGridTahapan(Mahasiswa mahasiswa, int jumlahTahapan) {
		List<String[]> data = new ArrayList<String[]>();
		if (mahasiswa == null || mahasiswa.getTahunangkatan() == null) {
			return data;
		}

		int semester = 1;
		String ta = Common.getCurrentTahunAkademik();
		Integer tahun = 0;
		if (ta != null && ta.contains("/")) {
			try {
				tahun = Integer.parseInt(StringUtils.split(ta, "/")[0]);
			} catch (NumberFormatException e) {
				tahun = Calendar.getInstance().get(Calendar.YEAR);
			}
		}
		tahun = (tahun + 1);

		int startIndex = (mahasiswa.getTahunangkatan() - ((int) (mahasiswa.getPindahKeKampusIniMasukSemester() / 2.0)));
		for (int i = startIndex; i < tahun; i++) {
			if (semester >= 20)
				break;
			semester += 2; // Naik 2 sekaligus

			for (int tahap = 1; tahap <= jumlahTahapan; tahap++) {
				data.add(new String[] { i + "/" + (i + 1), String.valueOf(tahap), "" });
			}
		}

		data.add(new String[] { "Konversi", "" });
		return data;
	}

	public static Combobox createComboKonfigurasi(Combobox conf) {
		if (conf == null) {
			conf = new Combobox();
		}
		if (Konfigurasi.konfigurasi != null) {
			for (String h : Konfigurasi.konfigurasi.keySet()) {
				org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
				comboitem.setLabel(Konfigurasi.konfigurasi.get(h));
				comboitem.setValue(h);
				conf.appendChild(comboitem);
			}
		}
		return conf;
	}

	public static Boolean checkApakahDosenBolehMenilai(Dosen dosen, Tbmuser tbmuser, String tahunAkademik,
			String jenisSemester) {
		Session session = null;
		try {
			if (tahunAkademik == null || tahunAkademik.trim().isEmpty() || jenisSemester == null
					|| jenisSemester.trim().isEmpty()) {
				return false;
			}
			tahunAkademik = tahunAkademik.trim();
			jenisSemester = jenisSemester.trim();
			Calendar hariIni = Calendar.getInstance();
			hariIni.setTime(WaktuUtil.getDate());
			hariIni.set(Calendar.HOUR_OF_DAY, 0);
			hariIni.set(Calendar.MINUTE, 0);
			hariIni.set(Calendar.SECOND, 0);
			hariIni.set(Calendar.MILLISECOND, 0);
			session = HibernateUtil.currentNativeSession();
			/*
			 * Satu akun petugas/admin dapat sekaligus terhubung ke master Dosen. Identitas
			 * pengecualian harus mengikuti ROLE yang sedang login: role Dosen memakai kolom
			 * dosen, sedangkan seluruh role pengelola memakai kolom tbmuser. Pemeriksaan lama
			 * hanya melihat keberadaan relasi dosen sehingga izin Admin Prodi tersimpan pada
			 * tbmuser tetapi dibaca dari kolom dosen dan selalu dianggap tidak ada.
			 */
			boolean hakAksesDosen = tbmuser != null && tbmuser.hakAkses() != null
					&& Tbmrole.DOSEN.equalsIgnoreCase(tbmuser.hakAkses().getRoleId());
			Criterion identitas = null;
			if (hakAksesDosen || (tbmuser != null && tbmuser.hakAkses() == null && dosen != null)) {
				identitas = dosen == null || dosen.getId() == null ? Restrictions.sqlRestriction("1=0")
						: Restrictions.eq("dosen", dosen);
			}
			Criteria izin = session.createCriteria(PengecualianJadwalPenilaianDosen.class)
					/*
					 * Data versi lama menyimpan status NULL dan getStatus() menampilkannya sebagai
					 * "Disetujui". Tetap baca data lama tersebut, sedangkan data baru selalu
					 * disimpan eksplisit sebagai "Pengajuan" sampai disetujui role Admin (am).
					 */
					.add(Restrictions.or(Restrictions.eq("status", PengecualianJadwalPenilaianDosen.DISETUJU),
							Restrictions.isNull("status")))
					.add(Restrictions.eq("tahunAkademik", tahunAkademik))
					.add(Restrictions.eq("jenisSemester", jenisSemester))
					.add(Restrictions.le("tanggalMulai", hariIni.getTime()))
					.add(Restrictions.ge("tanggalSampai", hariIni.getTime()));
			if (identitas != null) {
				izin.add(identitas);
			} else if (tbmuser != null && tbmuser.getUserId() != null
					&& !tbmuser.getUserId().trim().isEmpty()) {
				/*
				 * Cocokkan akun pengelola dengan userid, bukan instance entity dari HttpSession.
				 * Ini menangani referensi Tbmuser lama/berbeda yang masih menyimpan userid sama.
				 */
				izin.createAlias("tbmuser", "izinUser");
				izin.add(Restrictions.eq("izinUser.userId", tbmuser.getUserId().trim()));
			} else {
				izin.add(Restrictions.sqlRestriction("1=0"));
			}
			Integer count = ((Number) izin.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			return count > 0;
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonHelperClass.java:708");
			return false;
		} finally {
			Common.closeNativeSessionQuietly(session);
		}
	}

	public static Boolean checkApakahMahasiswaBolehAmbilKrsLewatPengecualian(Mahasiswa mahasiswa, String tahunAkademik,
			String jenisSemester) {
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			Integer count = ((Number) session.createCriteria(PengecualianJadwalPengisianKRSMahasiswa.class)
					.add(Restrictions.eq("tahunAkademik", tahunAkademik))
					.add(Restrictions.eq("jenisSemester", jenisSemester)).add(Restrictions.eq("mahasiswa", mahasiswa))
					.add(Restrictions.sqlRestriction("date('" + Common.databaseDateFormat1.get().format(WaktuUtil.getDate())
							+ "') between this_.tanggal_mulai and this_.tanggal_sampai"))
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			return count > 0;
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonHelperClass.java:732");
			return false;
		} finally {
			HibernateUtil.closeSession();
		}
	}

	public static Boolean checkUsername(String username, String userId, Long id) {
		if (username == null || username.trim().isEmpty()) {
			return false;
		}

		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			String trimmedUsername = username.trim();
			String trimmedUserId = (userId != null) ? userId.trim() : null;

			// Cek secara berurutan, jika salah satu ada, langsung kembalikan true
			// (menghemat query database)
			Integer userCount = ((Number) session.createCriteria(Tbmuser.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.setProjection(Projections.rowCount()).add(Restrictions.eq("userId", trimmedUsername))
					.add(userId == null ? Restrictions.sqlRestriction("1=1") : Restrictions.ne("userId", trimmedUserId))
					.uniqueResult()).intValue();
			if (userCount > 0)
				return true;

			Integer userOrtuCount = ((Number) session.createCriteria(Mahasiswa.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.setProjection(Projections.rowCount()).add(Restrictions.eq("userOrtu", trimmedUsername))
					.add(id == null ? Restrictions.sqlRestriction("1=1") : Restrictions.ne("id", id)).uniqueResult())
					.intValue();
			if (userOrtuCount > 0)
				return true;

			Integer userNimCount = ((Number) session.createCriteria(Mahasiswa.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.setProjection(Projections.rowCount()).add(Restrictions.eq("nim", trimmedUsername))
					.add(id == null ? Restrictions.sqlRestriction("1=1") : Restrictions.ne("id", id)).uniqueResult())
					.intValue();
			if (userNimCount > 0)
				return true;

			Integer userCalonMhsCount = ((Number) session.createCriteria(BiodataCalonMahasiswa.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.setProjection(Projections.rowCount()).add(Restrictions.eq("username", trimmedUsername))
					.add(id == null ? Restrictions.sqlRestriction("1=1") : Restrictions.ne("id", id)).uniqueResult())
					.intValue();
			if (userCalonMhsCount > 0)
				return true;

			return false;
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonHelperClass.java:786");
			return false; // Anggap false jika terjadi exception, atau bisa disesuaikan dengan kebutuhan
		} finally {
			HibernateUtil.closeSession();
		}
	}

	public static Combobox createComboJenisPembayaranDanSemua(Combobox jenisPembayaran) {
		if (jenisPembayaran == null) {
			jenisPembayaran = new Combobox();
		}

		MyComboitemConfig comboitem = new MyComboitemConfig("Semua");
		comboitem.setValue(null);
		jenisPembayaran.appendChild(comboitem);
		jenisPembayaran.setSelectedItem(comboitem);

		comboitem = new MyComboitemConfig(ConstantUtil.PENDAFTARAN_MAHASISWA_LAMA);
		comboitem.setValue(ConstantValues.PENDAFTARAN_MAHASISWA_LAMA);
		jenisPembayaran.appendChild(comboitem);

		comboitem = new MyComboitemConfig(ConstantUtil.PENDAFTARAN_CALON_MAHASISWA);
		comboitem.setValue(ConstantValues.PENDAFTARAN_CALON_MAHASISWA);
		jenisPembayaran.appendChild(comboitem);

		comboitem = new MyComboitemConfig(ConstantUtil.PENDAFTARAN_WISUDA);
		comboitem.setValue(ConstantValues.PENDAFTARAN_WISUDA);
		jenisPembayaran.appendChild(comboitem);

		comboitem = new MyComboitemConfig(ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU);
		comboitem.setValue(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU);
		jenisPembayaran.appendChild(comboitem);

		if (jenisKegiatansAktif == null) {
			reloadJenisKegiatans();
		}
		for (JenisKegiatan jenisKegiatan : jenisKegiatansAktif) {
			comboitem = new MyComboitemConfig(jenisKegiatan.getNamaKegiatan());
			comboitem.setValue(jenisKegiatan);
			jenisPembayaran.appendChild(comboitem);
		}
		return jenisPembayaran;
	}

	public static Combobox createComboJenisPembayaran(Combobox jenisPembayaran) {
		if (jenisPembayaran == null) {
			jenisPembayaran = new Combobox();
		}
		MyComboitemConfig comboitem = new MyComboitemConfig(ConstantUtil.PENDAFTARAN_MAHASISWA_LAMA);
		comboitem.setValue(ConstantValues.PENDAFTARAN_MAHASISWA_LAMA);
		jenisPembayaran.appendChild(comboitem);
		jenisPembayaran.setSelectedItem(comboitem);

		comboitem = new MyComboitemConfig(ConstantUtil.PENDAFTARAN_CALON_MAHASISWA);
		comboitem.setValue(ConstantValues.PENDAFTARAN_CALON_MAHASISWA);
		jenisPembayaran.appendChild(comboitem);

		comboitem = new MyComboitemConfig(ConstantUtil.PENDAFTARAN_WISUDA);
		comboitem.setValue(ConstantValues.PENDAFTARAN_WISUDA);
		jenisPembayaran.appendChild(comboitem);

		comboitem = new MyComboitemConfig(ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU);
		comboitem.setValue(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU);
		jenisPembayaran.appendChild(comboitem);

		if (jenisKegiatansAktif == null) {
			reloadJenisKegiatans();
		}
		for (JenisKegiatan jenisKegiatan : jenisKegiatansAktif) {
			comboitem = new MyComboitemConfig(jenisKegiatan.getNamaKegiatan());
			comboitem.setValue(jenisKegiatan);
			jenisPembayaran.appendChild(comboitem);
		}
		return jenisPembayaran;
	}

	@SuppressWarnings({ "rawtypes" })
	public static Double hitungTagihanMahasiswaSebagaiSyaratKrs(Session session, Mahasiswa mahasiswa,
			Integer semester) {

		if (jenisKegiatansUntukKrs == null) {
			reloadJenisKegiatans();
		}
		Double totaltagihan = 0.0;
		for (JenisKegiatan jenisKegiatan : jenisKegiatansUntukKrs) {
			PembayaranUtil.getInstance();
			Collection detailBiayas = PembayaranUtilHelper.getDetailBiayaMahasiswa(mahasiswa, semester,
					jenisKegiatan, false);
			PembayaranUtil.getInstance();
			int countPengaturanBulanan = PembayaranUtilHelper.countBulanan(session, mahasiswa, jenisKegiatan,
					semester, detailBiayas, false, false);

			if (countPengaturanBulanan > 0) {
				PembayaranUtil.getInstance();
				detailBiayas = PembayaranUtilHelper.getDetailBiayaMahasiswa(mahasiswa, semester, jenisKegiatan,
						"-1", true, false);
			}

			for (Object o : detailBiayas) {
				if (o instanceof DetailBiaya) {
					DetailBiaya biaya = (DetailBiaya) o;
					totaltagihan += biaya.getNilaiBiaya();
				} else if (o instanceof PengaturanPembayaranBulanan) {
					PengaturanPembayaranBulanan biaya = (PengaturanPembayaranBulanan) o;
					totaltagihan += biaya.getNominal();
				}
			}
			detailBiayas = null;
		}
		System.out
				.println("totaltagihan untuk pengambilan KRS => " + totaltagihan + ", " + mahasiswa + ", " + semester);
		return totaltagihan;
	}

	public static Boolean checkPembayaranSebelumKRSSudahMemenuhi(Mahasiswa mahasiswa, Integer semester, Integer tahap,
			boolean persetujuan) {

		if (jenisKegiatansUntukKrs == null) {
			reloadJenisKegiatans();
		}

		if (Common.checkBaypassStatusPembayaranMahasiswa(semester, tahap, mahasiswa, jenisKegiatansUntukKrs)) {
			return true;
		}

		Integer tahunAngkatanMhs = mahasiswa.getTahunangkatan();
		Integer semesterMulai = mahasiswa.getPindahKeKampusIniMasukSemester();
		Integer tahunAkademikMulai = Common.getTahunAkademik(semester, tahunAngkatanMhs, semesterMulai,
				mahasiswa.getSemesterMulai());

		String tahunAkademik = tahunAkademikMulai + "/" + (tahunAkademikMulai + 1);
		if (Common.checkApakahMahasiswaBolehAmbilKrsLewatPengecualian(mahasiswa, tahunAkademik,
				semester % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL)) {
			return true;
		}

		String batasTerendahPersen = Common.getKonfigurasi(
				persetujuan ? "batas_terendah_persen_pembayaran_boleh_persetujuan_krs"
						: "batas_terendah_persen_pembayaran_boleh_ambil_krs",
				"0", semester, mahasiswa.getTahunangkatan(), mahasiswa.getJurusan(), mahasiswa.getProgram(),
				mahasiswa.getStatusAwalMahasiswa()).getNilai();

		Double batas = 0.0;
		try {
			batas = Double.parseDouble(batasTerendahPersen.trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonHelperClass.java:931");

		}

		// System.out.println(
		// "Check Pembayaran KRS -> batasTerendahPersen = " +
		// batasTerendahPersen + "%, batas = " + batas);

		if (batas.intValue() == 0) {
			return true;
		}

		Session session = HibernateUtil.currentNativeSession();

		Double tagihanSyaratKrs = hitungTagihanMahasiswaSebagaiSyaratKrs(session, mahasiswa, semester);
		if (tagihanSyaratKrs < 0.01) {
			// session.disconnect();
			if (session.isOpen()) {
				session.disconnect();
				session.close();
			}
			HibernateUtil.closeSession();
			return true;
		}

		Double prosenYangSudahDibayar = 0.0;
		Boolean hasil = false;
		Kegiatan kegiatan = null;
		for (JenisKegiatan jenisKegiatan : jenisKegiatansUntukKrs) {
			kegiatan = mahasiswa.ambilKegiatans(semester, jenisKegiatan);

			prosenYangSudahDibayar = kegiatan == null ? 0.0 : kegiatan.getPersentaseLunas();
			hasil = prosenYangSudahDibayar >= batas;

			// System.out.println("Check Pembayaran KRS -> Mahasiswa " +
			// mahasiswa + ", jenisKegiatan => " + jenisKegiatan
			// + ", kegiatan = " + kegiatan + ", semester " + semester + ",
			// batasTerendahPersen = "
			// + batasTerendahPersen + "%, prosenYangSudahDibayar = " +
			// prosenYangSudahDibayar + "%, hasil = "
			// + hasil + ", tagihanSyaratKrs = " + tagihanSyaratKrs);

			if (hasil) {
				break;
			}
		}

		if (!hasil) {

			boolean mahasiswabaruMengikutipersyaratanKrsSptMahasiswa = Common.bolehKonfigurasi("mahasiswa_baru_mengikuti_persyaratan_krs_spt_mahasiswa", Konfigurasi.TIDAK_AKTIF);

			if (!mahasiswabaruMengikutipersyaratanKrsSptMahasiswa && semester <= 1) {

				BiodataCalonMahasiswa calonMahasiswa = (BiodataCalonMahasiswa) ConstantValues
						.simpleObject(
								session.createCriteria(BiodataCalonMahasiswa.class)
										.add(Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)))
										.add(Restrictions.eq("mahasiswa", mahasiswa)).setMaxResults(1),
								BiodataCalonMahasiswa.class);

				if (calonMahasiswa != null) {
					jenisKegiatansUntukKrs.add(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU);
					kegiatan = calonMahasiswa.ambilKegiatans(semester, ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU);
					prosenYangSudahDibayar = kegiatan == null ? 0.0 : kegiatan.getPersentaseLunas();
					hasil = prosenYangSudahDibayar >= batas;
				}
				// System.out.println("Check Pembayaran KRS -> calonMahasiswa "
				// + calonMahasiswa + ", semester " + semester
				// + ", batasTerendahPersen = " + batasTerendahPersen + "%,
				// prosenYangSudahDibayar = "
				// + prosenYangSudahDibayar + "%, hasil = " + hasil + ",
				// tagihanSyaratKrs = " + tagihanSyaratKrs);

			}

			if (!hasil) {
				String n = "";
				for (JenisKegiatan jenisKegiatan : jenisKegiatansUntukKrs) {
					n += n.isEmpty() ? jenisKegiatan.getNamaKegiatan() : ", atau " + jenisKegiatan.getNamaKegiatan();
				}

				try {
					MyMessageboxConfig.showFormat(
							"Mohon maaf, Bapak/Ibu. Mahasiswa dengan NIM {V1} belum dapat {V2} KRS, karena belum melakukan pembayaran pada{V3}. Jenis pembayaran yang harus diselesaikan antara lain {V4}. Langkah yang dapat dilakukan: (1) periksa kembali kewajiban pembayaran mahasiswa; (2) lakukan pembayaran sesuai jenis yang disyaratkan; (3) ulangi proses setelah pembayaran diselesaikan.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, mahasiswa.getNim(),
							(persetujuan ? "disetujui" : "mengambil"),
							((ConstantValues.aktifkanTahapanTerhubungKeKeuangan && tahap != null && tahap > 0)
									? " tahap " + tahap
									: " semester " + semester),
							n);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonHelperClass.java:1022");

				}
			}

		}
		// session.disconnect();
		if (session.isOpen()) {
			session.disconnect();
			session.close();
		}
		HibernateUtil.closeSession();
		return hasil;
	}

	public static boolean checkStatusPembayaranMahasiswaSebelumnya(Integer semester, Integer tahap,
			Mahasiswa mahasiswa) {
		return checkStatusPembayaranMahasiswaSebelumnya(semester, tahap, mahasiswa, false);
	}

	@SuppressWarnings("unchecked")
	public static boolean checkStatusPembayaranMahasiswaSebelumnya(Integer semester, Integer tahap, Mahasiswa mahasiswa,
			boolean persetujuan) {
		if (semester == null || semester.intValue() <= 1
				|| (semester.equals(1) || semester.equals(mahasiswa.getPindahKeKampusIniMasukSemester() + 1)
						|| semester.equals(mahasiswa.getPindahKeKampusIniMasukSemester() + 2)
						|| semester.equals(mahasiswa.getPindahKeKampusIniMasukSemester()))) {
			return true;
		}

		if (ConstantValues.aktifkanTahapanTerhubungKeKeuangan && tahap != null && tahap.equals(1)) {
			return true;
		}

		Konfigurasi konfigurasi = Common
				.getKonfigurasi(
						persetujuan ? "mahasiswa_harus_lunas_semester_sebelumnya_sebelum_persetujuan_krs"
								: "mahasiswa_harus_lunas_semester_sebelumnya_sebelum_mengambil_krs",
						Konfigurasi.TIDAK_AKTIF);

		if (konfigurasi.getNilai().equals(Konfigurasi.AKTIF)) {

			semester = semester - 1;
			if (ConstantValues.aktifkanTahapanTerhubungKeKeuangan && tahap != null && tahap > 1) {
				tahap = tahap - 1;
			}

			Integer tahunAngkatanMhs = mahasiswa.getTahunangkatan();
			Integer semesterMulai = mahasiswa.getPindahKeKampusIniMasukSemester();
			Integer tahunAkademikMulai = Common.getTahunAkademik(semester, tahunAngkatanMhs, semesterMulai,
					mahasiswa.getSemesterMulai());

			String tahunAkademik = tahunAkademikMulai + "/" + (tahunAkademikMulai + 1);
			if (Common.checkApakahMahasiswaBolehAmbilKrsLewatPengecualian(mahasiswa, tahunAkademik,
					semester % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL)) {
				return true;
			}

			PendaftaranCutiMahasiswa pendaftaranCutiMahasiswa = mahasiswa.ambilCuti(semester, tahap, false);
			int countCuti = pendaftaranCutiMahasiswa != null && pendaftaranCutiMahasiswa.getPersetujuan() ? 1 : 0;

			if (countCuti > 0) {
				return true;
			}
			Session session = HibernateUtil.currentNativeSession();
			try {
			Double tagihanSyaratKrs = hitungTagihanMahasiswaSebagaiSyaratKrs(session, mahasiswa, semester);
			if (tagihanSyaratKrs < 0.01) {
				return true;
			}

			Double harusLunas = 90.0;
			try {
				harusLunas = Double.parseDouble(Common
						.getKonfigurasi(
								persetujuan ? "batas_terendah_persen_pembayaran_semester_yang_lalu_boleh_disetujui_krs"
										: "batas_terendah_persen_pembayaran_semester_yang_lalu_boleh_mengisi_krs",
								"90")
						.getNilai().trim().replace(',', '.'));
				if (harusLunas.doubleValue() < 0.0 || harusLunas.doubleValue() > 100.0) {
					harusLunas = 90.0;
				}
			} catch (Exception e) {
				// Nilai konfigurasi tidak valid: gunakan default historis tanpa membanjiri audit.
				harusLunas = 90.0;
			}

			if (jenisKegiatansUntukKrs == null) {
				reloadJenisKegiatans();
			}

			boolean hasil = true;
			if (tahap == null || tahap.equals(0)) {
				if (!Common.checkBaypassStatusPembayaranMahasiswa(semester, tahap, mahasiswa, jenisKegiatansUntukKrs)) {
					List<Kegiatan> kegiatanDibayars = mahasiswa.ambilKegiatans(semester, jenisKegiatansUntukKrs);
					for (Kegiatan kegiatanDibayar : kegiatanDibayars) {
						boolean lunas = (kegiatanDibayar != null && kegiatanDibayar.hitungPersentaseLunasAktual() >= harusLunas);
						System.out.println("kegiatanDibayar -> " + kegiatanDibayar + ", lunas " + lunas);
						hasil &= lunas;
					}
				}
			} else {

				if (!Common.checkBaypassStatusPembayaranMahasiswa(semester, tahap, mahasiswa, jenisKegiatansUntukKrs)) {

					PembayaranUtil.getInstance();
					Collection<DetailBiaya> detailBiayas = PembayaranUtilHelper.getDetailBiayaMahasiswa(
							mahasiswa, semester, ConstantValues.PENDAFTARAN_MAHASISWA_LAMA, null, true);

					PembayaranUtil.getInstance();
					int countPengaturanBulanan = PembayaranUtilHelper.countBulanan(session, mahasiswa,
							ConstantValues.PENDAFTARAN_MAHASISWA_LAMA, semester, detailBiayas, false, true);
					if (countPengaturanBulanan > 0) {
						Collection<PengaturanPembayaranBulanan> pengaturanPembayaranBulanans = PembayaranUtil
								.getInstance().getPengaturanPembayaranSemua(mahasiswa, session, semester,
										ConstantValues.PENDAFTARAN_MAHASISWA_LAMA, detailBiayas, false, true);
						Double tagihan = 0.0;
						for (PengaturanPembayaranBulanan pengaturanPembayaranBulanan : pengaturanPembayaranBulanans) {
							if (pengaturanPembayaranBulanan.hitungTahap(mahasiswa, semester).equals(tahap)
									|| pengaturanPembayaranBulanan.hitungTahap(mahasiswa, semester + 1).equals(tahap)) {
								tagihan += pengaturanPembayaranBulanan.ambilNominalModifikasi(mahasiswa, semester);
							}
						}

						Double sumCiciclan = mahasiswa.hitungTotalCicilanPembayaranPengecekanKrs(semester, tahap);

						Number pengurangan = 0.0;

						if (sumCiciclan == null) {
							sumCiciclan = 0.0;
						}

						Double prosenYangSudahDibayar = (sumCiciclan.doubleValue()
								/ (tagihan - Math.abs(pengurangan.doubleValue())));
						hasil = prosenYangSudahDibayar >= (harusLunas / 100.0);
						// System.out.println(
						// "mahasiswa " + mahasiswa + " semester " + semester +
						// " tahap " + tahap + ", tagihan = "
						// + tagihan + ", harusLunas = " + harusLunas + ",
						// prosenYangSudahDibayar "
						// + prosenYangSudahDibayar + ", sumCiciclan = " +
						// sumCiciclan);
					}
				}
			}

			return hasil;
			} finally {
				HibernateUtil.closeSession();
			}
		} else {
			return true;
		}

	}

	public static boolean checkStatusPembayaranKegiatanMahasiswa(FormulirKegiatan formulirKegiatan,
			Mahasiswa mahasiswa) {
		// Integer semester = mahasiswa.currentSemester();

		String jenisSemester = formulirKegiatan.getSemester();
		Integer semester = Common.getSemester(mahasiswa.getTahunangkatan(), jenisSemester,
				mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());

		Integer tahap = null;
		boolean hasil = true;

		if (formulirKegiatan.getHarusBayarLunasSmtLalu()) {

			Double harusLunas = 99.0;

			semester = semester - 1;

			if (semester < 1) {
				return true;
			}

			// Session session = HibernateUtil.currentNativeSession();

			if (jenisKegiatansUntukKrs == null) {
				reloadJenisKegiatans();
			}

			PendaftaranCutiMahasiswa pendaftaranCutiMahasiswa = mahasiswa.ambilCuti(semester, tahap, false);
			int countCuti = pendaftaranCutiMahasiswa != null && pendaftaranCutiMahasiswa.getPersetujuan() ? 1 : 0;

			if (countCuti == 0) {

				// if (tahap == null || tahap.equals(0)) {
				if (!Common.checkBaypassStatusPembayaranMahasiswa(semester, tahap, mahasiswa, jenisKegiatansUntukKrs)) {
					List<Kegiatan> kegiatanDibayars = mahasiswa.ambilKegiatans(semester, jenisKegiatansUntukKrs);
					for (Kegiatan kegiatanDibayar : kegiatanDibayars) {
						hasil &= (kegiatanDibayar != null && kegiatanDibayar.hitungPersentaseLunasAktual() >= harusLunas);
					}
				}

				if (!hasil) {
					try {
						MyMessageboxConfig.showFormat(
								"Mohon maaf, Bapak/Ibu. Mahasiswa dengan NIM {V1} atas nama {V2} harus melunasi biaya semester {V3} terlebih dahulu. Langkah yang dapat dilakukan: (1) periksa tagihan pembayaran mahasiswa yang bersangkutan; (2) lakukan pelunasan biaya semester yang masih tertunggak; (3) ulangi proses setelah pembayaran diselesaikan.",
								"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, mahasiswa.getNim(),
								mahasiswa.getNama(), semester);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						Common.tampilErrorJikaAdmin(e);
					}
				}

			}

			// HibernateUtil.closeSession();

		}

		if (!hasil) {
			return hasil;
		}

		semester = Common.getSemester(mahasiswa.getTahunangkatan(), jenisSemester,
				mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());
		if (formulirKegiatan.getHarusBayarLunasSmtSaatIni()) {

			if (semester < 1) {
				return true;
			}

			Double harusLunas = 99.0;

			// Session session = HibernateUtil.currentNativeSession();

			if (jenisKegiatansUntukKrs == null) {
				reloadJenisKegiatans();
			}

			PendaftaranCutiMahasiswa pendaftaranCutiMahasiswa = mahasiswa.ambilCuti(semester, tahap, false);
			int countCuti = pendaftaranCutiMahasiswa != null && pendaftaranCutiMahasiswa.getPersetujuan() ? 1 : 0;

			if (countCuti == 0) {

				// if (tahap == null || tahap.equals(0)) {
				if (!Common.checkBaypassStatusPembayaranMahasiswa(semester, tahap, mahasiswa, jenisKegiatansUntukKrs)) {
					List<Kegiatan> kegiatanDibayars = mahasiswa.ambilKegiatans(semester, jenisKegiatansUntukKrs);
					for (Kegiatan kegiatanDibayar : kegiatanDibayars) {
						hasil &= (kegiatanDibayar != null && kegiatanDibayar.hitungPersentaseLunasAktual() >= harusLunas);
					}
				}

				if (!hasil) {
					try {
						MyMessageboxConfig.showFormat(
								"Mohon maaf, Bapak/Ibu. Mahasiswa dengan NIM {V1} atas nama {V2} harus melunasi biaya semester {V3} terlebih dahulu. Langkah yang dapat dilakukan: (1) periksa tagihan pembayaran mahasiswa yang bersangkutan; (2) lakukan pelunasan biaya semester yang masih tertunggak; (3) ulangi proses setelah pembayaran diselesaikan.",
								"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, mahasiswa.getNim(),
								mahasiswa.getNama(), semester);
					} catch (Exception e) {

						Common.tampilErrorJikaAdmin(e);
					}
				}

			}

			// HibernateUtil.closeSession();

		}

		return hasil;

	}

	public static boolean checkStatusPembayaranMahasiswaSebelumnyaUntukPenilaian(Integer semester, Integer tahap,
			Mahasiswa mahasiswa, Double harusLunas, boolean termasukSmt1) {
		if (semester == null || semester.intValue() <= 0
				|| (termasukSmt1
						&& (semester.equals(1) || semester.equals(mahasiswa.getPindahKeKampusIniMasukSemester() + 1)
								|| semester.equals(mahasiswa.getPindahKeKampusIniMasukSemester() + 2)
								|| semester.equals(mahasiswa.getPindahKeKampusIniMasukSemester())))) {
			return true;
		}
		Konfigurasi konfigurasi = Common.getKonfigurasi(
				"mahasiswa_harus_lunas_semester_sebelumnya_sebelum_melihat_nilai", Konfigurasi.TIDAK_AKTIF);

		if (konfigurasi.getNilai().equals(Konfigurasi.AKTIF)) {

			semester = semester - 1;
			if (ConstantValues.aktifkanTahapanTerhubungKeKeuangan && tahap != null && tahap > 1) {
				tahap = tahap - 1;
			}

			PendaftaranCutiMahasiswa pendaftaranCutiMahasiswa = mahasiswa.ambilCuti(semester, tahap, false);
			int countCuti = pendaftaranCutiMahasiswa != null && pendaftaranCutiMahasiswa.getPersetujuan() ? 1 : 0;
			if (countCuti > 0) {
				return true;
			}

			if (jenisKegiatansUntukNilai == null) {
				reloadJenisKegiatans();
			}

			boolean hasil = true;

			if (!Common.checkBaypassStatusPembayaranMahasiswa(semester, tahap, mahasiswa, jenisKegiatansUntukNilai)) {
				List<Kegiatan> kegiatanDibayars = mahasiswa.ambilKegiatans(semester, jenisKegiatansUntukNilai);

				if (kegiatanDibayars.isEmpty()) {
					hasil = true;
				} else {
					for (Kegiatan kegiatanDibayar : kegiatanDibayars) {
						if (kegiatanDibayar != null) {
							hasil &= (kegiatanDibayar != null && kegiatanDibayar.hitungPersentaseLunasAktual() >= harusLunas);

							System.out.println("mahasiswa " + mahasiswa + " semester " + semester + " tahap " + tahap
									+ ", kegiatanDibayar = " + kegiatanDibayar + ", harusLunas = " + harusLunas
									+ ", prosenYangSudahDibayar " + kegiatanDibayar.hitungPersentaseLunasAktual() + ", hasil "
									+ hasil);
						}
					}
				}

			}

			return hasil;
		} else {
			return true;
		}

	}

	public static boolean checkStatusPembayaranMahasiswaPengajuanSkripsi(
			FormatNilaiProposalSkripsi formatNilaiProposalSkripsi, Integer semester, Mahasiswa mahasiswa) {
		if (semester == null || semester.intValue() <= 1
				|| (semester.equals(1) || semester.equals(mahasiswa.getPindahKeKampusIniMasukSemester() + 1)
						|| semester.equals(mahasiswa.getPindahKeKampusIniMasukSemester() + 2)
						|| semester.equals(mahasiswa.getPindahKeKampusIniMasukSemester()))) {
			return true;
		}

		if (formatNilaiProposalSkripsi.getHarusLunas()) {

			Double harusLunas = formatNilaiProposalSkripsi.getProsentaseLunas();

			boolean hasil = true;

			if (jenisKegiatansUntukKrs == null) {
				reloadJenisKegiatans();
			}
			if (!Common.checkBaypassStatusPembayaranMahasiswa(semester, null, mahasiswa, jenisKegiatansUntukKrs)) {
				List<Kegiatan> kegiatanDibayars = mahasiswa.ambilKegiatans(semester, jenisKegiatansUntukKrs);
				for (Kegiatan kegiatanDibayar : kegiatanDibayars) {
					hasil &= (kegiatanDibayar != null && kegiatanDibayar.hitungPersentaseLunasAktual() >= harusLunas);
				}
			}

			return hasil;
		} else {
			return true;
		}

	}

	public static boolean checkStatusPembayaranMahasiswaPengajuanSidang(FormatNilaiSkripsi formatNilaiSkripsi,
			Integer semester, Mahasiswa mahasiswa) {
		if (semester == null || semester.intValue() <= 1
				|| (semester.equals(1) || semester.equals(mahasiswa.getPindahKeKampusIniMasukSemester() + 1)
						|| semester.equals(mahasiswa.getPindahKeKampusIniMasukSemester() + 2)
						|| semester.equals(mahasiswa.getPindahKeKampusIniMasukSemester()))) {
			return true;
		}

		if (formatNilaiSkripsi.getHarusLunas()) {

			Double harusLunas = formatNilaiSkripsi.getProsentaseLunas();

			boolean hasil = true;

			if (jenisKegiatansUntukKrs == null) {
				reloadJenisKegiatans();
			}
			if (!Common.checkBaypassStatusPembayaranMahasiswa(semester, null, mahasiswa, jenisKegiatansUntukKrs)) {
				List<Kegiatan> kegiatanDibayars = mahasiswa.ambilKegiatans(semester, jenisKegiatansUntukKrs);
				for (Kegiatan kegiatanDibayar : kegiatanDibayars) {
					hasil &= (kegiatanDibayar != null && kegiatanDibayar.hitungPersentaseLunasAktual() >= harusLunas);
				}
			}

			return hasil;
		} else {
			return true;
		}

	}

	public static boolean checkStatusPembayaranMahasiswaPengajuanWisuda(Integer semester, Mahasiswa mahasiswa) {
		if (semester == null || semester.intValue() <= 1
				|| (semester.equals(1) || semester.equals(mahasiswa.getPindahKeKampusIniMasukSemester() + 1)
						|| semester.equals(mahasiswa.getPindahKeKampusIniMasukSemester() + 2)
						|| semester.equals(mahasiswa.getPindahKeKampusIniMasukSemester()))) {
			return true;
		}
		Konfigurasi konfigurasi = Common.getKonfigurasi("mahasiswa_harus_lunas_sebelum_wisuda",
				Konfigurasi.TIDAK_AKTIF);

		if (konfigurasi.getNilai().equals(Konfigurasi.AKTIF)) {

			Double harusLunas = 90.0;
			try {
				harusLunas = Double.parseDouble(Common
						.getKonfigurasi("batas_terendah_persen_pembayaran_sebelum_wisuda", "90").getNilai().trim());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonHelperClass.java:1424");

			}

			boolean hasil = true;
			if (jenisKegiatansUntukKrs == null) {
				reloadJenisKegiatans();
			}
			if (!Common.checkBaypassStatusPembayaranMahasiswa(semester, null, mahasiswa, jenisKegiatansUntukKrs)) {
				List<Kegiatan> kegiatanDibayars = mahasiswa.ambilKegiatans(semester, jenisKegiatansUntukKrs);
				for (Kegiatan kegiatanDibayar : kegiatanDibayars) {
					hasil &= (kegiatanDibayar != null && kegiatanDibayar.hitungPersentaseLunasAktual() >= harusLunas);
				}
			}

			return hasil;
		} else {
			return true;
		}

	}

}
