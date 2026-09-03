package ais.common;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.sql.Blob;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

import ais.action.master.KonfigurasiTampilanBiodataCalonMahasiswaAction;
import ais.action.master.helper.KegiatanPersistenceHelper;
import ais.action.master.helper.PembayaranUtilHelper;
import ais.action.master.pmb.VerifikasiPMBHelper;
import ais.action.master.pmb.nim.NimGenerator;
import ais.action.master.pmb.noreg.NoRegGenerator;
import ais.action.master.pmb.noujian.NoUjianGenerator;
import ais.action.master.sekolah.psb.noreg.NoRegGeneratorPsb;
import ais.action.report.Report;
import ais.action.ws.util.PembayaranUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailBiaya;
import ais.database.model.JenisKegiatan;
import ais.database.model.Jurusan;
import ais.database.model.Kegiatan;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.RuangPMB;
import ais.database.model.RuangPaketPMB;
import ais.database.model.Tbmuser;
import ais.database.model.UploadVirtualAccount;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.FotoBiodataCalonMahasiswa;
import ais.database.model.file.FotoMahasiswa;
import ais.database.model.file.LampiranLainBiodataCalonMahasiswa;
import ais.database.model.file.LampiranLainMahasiswa;
import ais.database.model.sekolah.CalonSiswa;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Utilitas bersama AIS untuk common pmb. Kelas ini mengonsolidasikan operasi lintas layar/service
 * yang benar-benar satu domain agar pemanggil tidak membuat helper dengan fungsi paralel.
 *
 * <p><b>Batas tanggung jawab:</b> gunakan tipe ini hanya untuk state dan operasi yang sesuai dengan nama
 * domainnya. Logika lintas domain harus didelegasikan ke service atau helper bersama supaya tidak muncul
 * implementasi paralel dengan hasil berbeda.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code PembayaranUtil pembayaranUtil};
 * pembacaan/pencarian ({@code getProdiPilihan()}, {@code getTotalTagihan()}, {@code uploadFoto()}, {@code
 * ambilNoUjianDariObject()}, {@code ambilIdBiodataCalonMahasiswa()}, {@code ambilNoUjianDariDatabase()});
 * validasi/perhitungan ({@code isNimPmbValid()}, {@code isNimPmbValidAtauDiizinkan()}); mutasi data ({@code
 * setTimeoutTransaksiPmb()}, {@code simpanNoUjianLangsung()}, {@code saveMahasiswa()}, {@code saveMahasiswa()},
 * {@code saveMahasiswa()}, {@code saveMahasiswa()}); penghapusan/pembatalan ({@code
 * isErrorTimeoutAtauCancelDatabase()}); operasi domain lain ({@code generateNoRegistrasi()}, {@code
 * generateNoRegistrasi()}, {@code generateNoUjian()}, {@code isBlankString()}, {@code
 * isNimPmbMengandungTandaHubung()}, {@code konfirmasiNimMengandungTandaJikaPerlu()}). Bagian lain dari kontrak
 * tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> sesuai operasi yang dipanggil, utilitas dapat mengubah komponen UI, membaca/menulis
 * persistence atau berkas, dan memanggil layanan lain. Gunakan method kanonik di kelas ini melalui konteks
 * request/transaksi yang tepat, bukan menyalin implementasinya.</p>
 */
public class CommonPMB {

	public static PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	public static String getProdiPilihan(BiodataCalonMahasiswa calonMahasiswa) {
		String str = "";
		if (calonMahasiswa.getProdi1() != null) {
			str += (calonMahasiswa.getProdi1().getNama()) + ", ";
		}
		if (calonMahasiswa.getProdi2() != null) {
			str += (calonMahasiswa.getProdi2().getNama()) + ", ";
		}
		if (calonMahasiswa.getProdi3() != null) {
			str += (calonMahasiswa.getProdi3().getNama()) + ", ";
		}
		if (calonMahasiswa.getProdi4() != null) {
			str += (calonMahasiswa.getProdi4().getNama()) + ", ";
		}
		if (calonMahasiswa.getProdi5() != null) {
			str += (calonMahasiswa.getProdi5().getNama());
		}
		return str;
	}

	public static Double getTotalTagihan(BiodataCalonMahasiswa biodataCalonMahasiswa, JenisKegiatan jenisKegiatan) {
		Jurusan prodiLulus = biodataCalonMahasiswa.getProdiLulus();
		List<DetailBiaya> detailBiayas = new ArrayList<DetailBiaya>();
		if (prodiLulus == null || prodiLulus.getId() == null) {
			Jurusan myjurusan1 = biodataCalonMahasiswa.getProdi1() == null ? biodataCalonMahasiswa.getProdi2()
					: biodataCalonMahasiswa.getProdi1();
			java.util.Collection<DetailBiaya> detailBiayas1 = PembayaranUtilHelper
					.getDetailBiayaCalonMahasiswa(biodataCalonMahasiswa, jenisKegiatan, myjurusan1, false);
			detailBiayas.addAll(detailBiayas1);
		} else {
			java.util.Collection<DetailBiaya> detailBiayas1 = PembayaranUtilHelper
					.getDetailBiayaCalonMahasiswa(biodataCalonMahasiswa, jenisKegiatan, prodiLulus, false);
			detailBiayas.addAll(detailBiayas1);
		}

		Double total = 0.0;
		for (DetailBiaya biaya : detailBiayas) {
			Double nilai = biaya.hitungTotal();
			total += (nilai).longValue();
		}
		return total;
	}

	public static String generateNoRegistrasi(BiodataCalonMahasiswa biodataCalonMahasiswa) throws Exception {

		try {
			NoRegGenerator noRegGenerator = (NoRegGenerator) Class.forName(Common
					.getKonfigurasi("class_untuk_generate_no_reg", "ais.action.master.pmb.noreg.DefaultNoRegGenerator")
					.getNilai()).newInstance();
			return noRegGenerator.generateNoReg(biodataCalonMahasiswa);
		} catch (Exception e) {
			MyMessageboxConfig.show(
					"Mohon maaf, Bapak/Ibu. Nomor Registrasi tidak dapat dibuat secara otomatis untuk saat ini. Langkah yang dapat dilakukan: (1) coba ulangi proses beberapa saat lagi; (2) periksa konfigurasi generator nomor registrasi; (3) apabila masalah masih berlanjut, mohon segera menghubungi Administrator.",
					"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			Common.tampilErrorJikaAdmin(e);
		}

		return "";
	}

	public static String generateNoRegistrasi(CalonSiswa calonSiswa) throws Exception {

		try {
			NoRegGeneratorPsb noRegGenerator = (NoRegGeneratorPsb) Class
					.forName(Common.getKonfigurasi("class_untuk_generate_no_reg_psb",
							"ais.action.master.sekolah.psb.DefaultNoRegGeneratorPsb").getNilai())
					.newInstance();
			return noRegGenerator.generateNoReg(calonSiswa);
		} catch (Exception e) {
			MyMessageboxConfig.show(
					"Mohon maaf, Bapak/Ibu. Nomor Registrasi tidak dapat dibuat secara otomatis untuk saat ini. Langkah yang dapat dilakukan: (1) coba ulangi proses beberapa saat lagi; (2) periksa konfigurasi generator nomor registrasi; (3) apabila masalah masih berlanjut, mohon segera menghubungi Administrator.",
					"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			Common.tampilErrorJikaAdmin(e);
		}

		return "";
	}

	public static void uploadFoto(Media media, BiodataCalonMahasiswa calonMahasiswa, EventListener eventListener)
			throws Exception {

		if (calonMahasiswa.getGelombangPendaftaran().getHarusBayarSebelumBisaLogin()) {
			Kegiatan kegiatan = calonMahasiswa.getPembayaranRegistrasi();

			if (!isPembayaranRegistrasiTerpenuhi(kegiatan)) {
				String infoBelumbayarSaatLogincalonMahasiswa = Common.getKonfigurasi(
						"infoBelumbayarSaatProsescalonMahasiswa",
						"Calon Mahasiswa dengan nomor pendaftaran [noreg] belum dapat diproses karena belum melakukan proses pembayaran.")
						.getNilai();
				infoBelumbayarSaatLogincalonMahasiswa = org.apache.commons.lang.StringUtils
						.replace(infoBelumbayarSaatLogincalonMahasiswa, "[noreg]", calonMahasiswa.getNoRegistrasi());
				MyMessageboxConfig.show(infoBelumbayarSaatLogincalonMahasiswa, "PERINGATAN", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return;
			}
		}

		if (media instanceof org.zkoss.image.AImage) {

			try {
				Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();

				FotoBiodataCalonMahasiswa fotoBiodataCalonMahasiswa = (FotoBiodataCalonMahasiswa) streamingSession
						.createCriteria(FotoBiodataCalonMahasiswa.class)
						.add(Restrictions.eq("biodataCalonMahasiswa", calonMahasiswa.getId())).setMaxResults(1)
						.uniqueResult();
				if (fotoBiodataCalonMahasiswa != null) {
					streamingSession.getTransaction().begin();
					streamingSession.delete(fotoBiodataCalonMahasiswa);
					streamingSession.getTransaction().commit();
				}

				fotoBiodataCalonMahasiswa = new FotoBiodataCalonMahasiswa();
				fotoBiodataCalonMahasiswa.setNama(media.getName());
				fotoBiodataCalonMahasiswa.setKeterangan(media.getContentType());
				fotoBiodataCalonMahasiswa.setBiodataCalonMahasiswa(calonMahasiswa.getId());
				Blob blob = Common.getBlobFromMedia(media, streamingSession);
				fotoBiodataCalonMahasiswa.setFoto(blob);

				streamingSession.getTransaction().begin();
				streamingSession.save(fotoBiodataCalonMahasiswa);
				streamingSession.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}

			MyMessageboxConfig.show("Foto berhasil diunggah, Bapak/Ibu.", "Pemberitahuan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, eventListener);

		}
	}

	@SuppressWarnings({})
	public static String generateNoUjian(Tbmuser tbmuser, BiodataCalonMahasiswa biodataCalonMahasiswa)
			throws Exception {
		List<String> warnings = new ArrayList<String>();
		String s = generateNoUjian(tbmuser, biodataCalonMahasiswa, warnings);
		if (!warnings.isEmpty()) {
			String err = "";
			for (String ss : warnings) {
				err += ss + "\n\n";
			}
			MyMessageboxConfig.show(err, "Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		}

		return s;
	}

	private static boolean isBlankString(String value) {
		return value == null || value.trim().length() == 0;
	}

	private static boolean isNimPmbValid(String nim) {
		if (isBlankString(nim)) {
			return false;
		}
		String value = nim.trim();
		return value.indexOf("--") < 0 && value.indexOf('_') < 0 && value.indexOf('-') < 0;
	}

	/**
	 * Menentukan apakah syarat pembayaran registrasi telah terpenuhi.
	 * Tagihan netto Rp0 (misalnya karena diskon/potongan 100%) tidak memerlukan
	 * transaksi pembayaran dan harus tetap dapat melanjutkan proses PMB.
	 * Data kegiatan tetap wajib tersedia agar kondisi data tagihan yang belum
	 * terbentuk tidak keliru dianggap gratis.
	 */
	public static boolean isPembayaranRegistrasiTerpenuhi(Kegiatan kegiatan) {
		if (kegiatan == null || kegiatan.getId() == null) {
			return false;
		}
		if (Boolean.TRUE.equals(kegiatan.getLunas())) {
			return true;
		}
		Double persentasePemenuhan = KegiatanPersistenceHelper
				.hitungPersentasePemenuhanTagihan(kegiatan);
		if (persentasePemenuhan != null && persentasePemenuhan.doubleValue() >= 99.0) {
			return true;
		}
		// Fallback untuk instalasi yang menonaktifkan perhitungan tagihan segar.
		Double tagihan = kegiatan.getTagihan();
		return tagihan != null && Math.abs(tagihan.doubleValue()) < 0.01;
	}

	public static boolean isNimPmbMengandungTandaHubung(String nim) {
		return nim != null && nim.trim().indexOf('-') >= 0;
	}

	public static boolean konfirmasiNimMengandungTandaJikaPerlu(String nim, BiodataCalonMahasiswa calonMahasiswa)
			throws Exception {
		if (!isNimPmbMengandungTandaHubung(nim)) {
			return true;
		}
		String nama = calonMahasiswa == null || calonMahasiswa.getNama() == null ? "" : calonMahasiswa.getNama();
		int hasil = MyMessageboxConfig.showFormat(
				"NIM hasil generate mengandung tanda '-' yaitu \"{V1}\"{V2}. Apakah proses tetap dilanjutkan?",
				"Peringatan", MyMessageboxConfig.YES | MyMessageboxConfig.NO, MyMessageboxConfig.EXCLAMATION,
				nim, nama.trim().isEmpty() ? "" : " untuk " + nama);
		return hasil == MyMessageboxConfig.YES;
	}

	private static boolean isNimPmbValidAtauDiizinkan(String nim, boolean izinkanTandaHubung) {
		if (isNimPmbValid(nim)) {
			return true;
		}
		if (!izinkanTandaHubung || isBlankString(nim)) {
			return false;
		}
		String value = nim.trim();
		return value.indexOf('_') < 0;
	}

	private static String safeTrim(String value) {
		return value == null ? "" : value.trim();
	}

	private static String firstNotBlank(String value1, String value2) {
		if (!isBlankString(value1)) {
			return value1.trim();
		}
		return safeTrim(value2);
	}

	private static String ambilNoUjianDariObject(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		try {
			return biodataCalonMahasiswa == null ? "" : safeTrim(biodataCalonMahasiswa.getNoUjian());
		} catch (Exception e) {
			return "";
		}
	}

	private static Long ambilIdBiodataCalonMahasiswa(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		try {
			return biodataCalonMahasiswa == null ? null : biodataCalonMahasiswa.getId();
		} catch (Exception e) {
			return null;
		}
	}

	private static String ambilNoUjianDariDatabase(Long biodataCalonMahasiswaId) {
		if (biodataCalonMahasiswaId == null) {
			return "";
		}

		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			Object value = session
					.createSQLQuery("select no_ujian from public.biodata_calon_mahasiswa where id = :id")
					.setParameter("id", biodataCalonMahasiswaId).uniqueResult();
			return value == null ? "" : value.toString().trim();
		} catch (Exception e) {
			return "";
		} finally {
			tutupSessionLokal(session);
		}
	}

	public static String ambilNimTersimpanDariRiwayatPmb(Session sessionAktif,
			BiodataCalonMahasiswa biodataCalonMahasiswa, Mahasiswa mahasiswa) {
		if (mahasiswa != null && isNimPmbValid(mahasiswa.getNim())) {
			return mahasiswa.getNim().trim();
		}
		Session session = sessionAktif;
		boolean tutupSession = false;
		try {
			if (session == null) {
				session = HibernateUtil.getSessionFactory().openSession();
				tutupSession = true;
			}
			Long biodataId = biodataCalonMahasiswa == null ? null : biodataCalonMahasiswa.getId();
			Long mahasiswaId = mahasiswa == null ? null : mahasiswa.getId();
			if (biodataId == null && mahasiswa != null && mahasiswa.getBiodataCalonMahasiswa() != null) {
				biodataId = mahasiswa.getBiodataCalonMahasiswa();
			}
			String noReg = biodataCalonMahasiswa == null ? "" : safeTrim(biodataCalonMahasiswa.getNoRegistrasi());
			String noUjian = biodataCalonMahasiswa == null ? "" : ambilNoUjianDariObject(biodataCalonMahasiswa);

			String nim = ambilNimBiodataAktif(session, biodataId);
			if (isNimPmbValid(nim)) {
				return nim.trim();
			}
			nim = ambilNimBiodataAudit(session, "public", "biodata_calon_mahasiswa_aud",
					biodataId, mahasiswaId, noReg, noUjian);
			if (isNimPmbValid(nim)) {
				return nim.trim();
			}
			nim = ambilNimBiodataAudit(session, "new_audit", "biodata_calon_mahasiswa__audit",
					biodataId, mahasiswaId, noReg, noUjian);
			return isNimPmbValid(nim) ? nim.trim() : "";
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit CommonPMB.ambilNimTersimpanDariRiwayatPmb");
			return "";
		} finally {
			if (tutupSession) {
				tutupSessionLokal(session);
			}
		}
	}

	private static String ambilNimBiodataAktif(Session session, Long biodataId) {
		if (session == null || biodataId == null) {
			return "";
		}
		try {
			Object value = session.createSQLQuery(
					"select nim from public.biodata_calon_mahasiswa "
					+ "where id = :id and nim is not null and trim(nim) <> ''")
					.setParameter("id", biodataId).setMaxResults(1).uniqueResult();
			return value == null ? "" : value.toString().trim();
		} catch (Exception e) {
			return "";
		}
	}

	private static String ambilNimBiodataAudit(Session session, String schema, String table, Long biodataId,
			Long mahasiswaId, String noReg, String noUjian) {
		if (session == null || isBlankString(schema) || isBlankString(table)) {
			return "";
		}
		try {
			Object ada = session.createSQLQuery(
					"select count(*) from information_schema.tables "
					+ "where table_schema = :schema and table_name = :table")
					.setParameter("schema", schema).setParameter("table", table).uniqueResult();
			if (!(ada instanceof Number) || ((Number) ada).longValue() < 1L) {
				return "";
			}
			StringBuilder sql = new StringBuilder();
			sql.append("select nim from ").append(schema).append(".").append(table)
					.append(" where nim is not null and trim(nim) <> '' and (1=0 ");
			if (biodataId != null) {
				sql.append("or id = :biodataId ");
			}
			if (mahasiswaId != null) {
				sql.append("or mahasiswa = :mahasiswaId ");
			}
			if (!isBlankString(noReg)) {
				sql.append("or trim(coalesce(no_registrasi,'')) = :noReg ");
			}
			if (!isBlankString(noUjian)) {
				sql.append("or trim(coalesce(no_ujian,'')) = :noUjian ");
			}
			sql.append(") order by rev desc limit 1");
			org.hibernate.SQLQuery query = session.createSQLQuery(sql.toString());
			if (biodataId != null) {
				query.setParameter("biodataId", biodataId);
			}
			if (mahasiswaId != null) {
				query.setParameter("mahasiswaId", mahasiswaId);
			}
			if (!isBlankString(noReg)) {
				query.setParameter("noReg", noReg);
			}
			if (!isBlankString(noUjian)) {
				query.setParameter("noUjian", noUjian);
			}
			Object value = query.uniqueResult();
			return value == null ? "" : value.toString().trim();
		} catch (Exception e) {
			return "";
		}
	}

	private static void tutupSessionLokal(Session session) {
		if (session == null) {
			return;
		}
		try {
			session.clear();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonPMB.java:280");
		}
		try {
			session.disconnect();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonPMB.java:284");
		}
		try {
			session.close();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonPMB.java:288");
		}
	}

	private static void rollbackCurrentSessionNoUjianQuietly() {
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			if (session != null) {
				try {
					Transaction tx = session.getTransaction();
					if (tx != null && tx.isActive()) {
						tx.rollback();
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonPMB.java:302");
				}
				tutupSessionLokal(session);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonPMB.java:306");
		}

		try {
			HibernateUtil.closeSession();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonPMB.java:311");
		}
	}

	private static void setTimeoutTransaksiPmb(Session session) {
		if (session == null) {
			return;
		}

		try {
			session.createSQLQuery("set local lock_timeout = '15s'").executeUpdate();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonPMB.java:322");
		}
		try {
			session.createSQLQuery("set local statement_timeout = '120s'").executeUpdate();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonPMB.java:326");
		}
	}

	private static void lockTransaksiNomorUjian(Session session, Long biodataCalonMahasiswaId, String noUjian) {
		if (session == null) {
			return;
		}
		try {
			// FIX MappingException "No Dialect mapping for JDBC type: 1111": pg_advisory_xact_lock()
			// mengembalikan tipe PostgreSQL "void", yang oleh JDBC dipetakan ke java.sql.Types.OTHER
			// (1111) -- Hibernate CustomLoader gagal auto-discover nama tipe utk kolom ini krn Dialect
			// tak punya pemetaan utk OTHER, sehingga query SELALU gagal & advisory lock TIDAK PERNAH
			// benar-benar didapat (fungsi pencegah race-condition nomor ujian jadi tak berfungsi, walau
			// tak sampai crash krn sudah try/catch). Cast hasilnya ke text + deklarasikan tipe kolom
			// eksplisit via addScalar() supaya Hibernate tidak perlu menebak tipe dari metadata JDBC.
			String lockKey = "PMB_NO_UJIAN_SAVE_" + biodataCalonMahasiswaId + "_" + safeTrim(noUjian);
			// KE-FIX QueryException "Not all named parameters have been set: [:text]":
			// Hibernate memindai string SQL untuk parameter bernama dengan pola ":nama", sehingga
			// operator cast PostgreSQL "::text" ikut TERBACA sebagai parameter bernama ":text".
			// Parameter itu tak pernah di-set -> query SELALU gagal -> advisory lock TIDAK PERNAH
			// didapat, artinya pengaman race-condition nomor ujian sebenarnya MATI (tak terlihat
			// karena tertelan try/catch). Cast ANSI "cast(... as text)" memberi hasil yang sama
			// persis tanpa tanda ":" sehingga tidak lagi bentrok dengan sintaks parameter Hibernate.
			session.createSQLQuery("with kunci_transaksi as "
					+ "(select pg_advisory_xact_lock(hashtext(:lockKey))) "
					+ "select cast(1 as bigint) as lock_result from kunci_transaksi")
					.addScalar("lock_result", org.hibernate.Hibernate.LONG)
					.setParameter("lockKey", lockKey)
					.uniqueResult();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonPMB.java:338");
		}
	}

	private static String simpanNoUjianLangsung(BiodataCalonMahasiswa biodataCalonMahasiswa, String noUjian)
			throws Exception {
		Long id = ambilIdBiodataCalonMahasiswa(biodataCalonMahasiswa);
		if (id == null || isBlankString(noUjian)) {
			return "";
		}

		Session session = null;
		Transaction tx = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();
			setTimeoutTransaksiPmb(session);
			lockTransaksiNomorUjian(session, id, noUjian);

			int updated = session.createSQLQuery("update public.biodata_calon_mahasiswa "
					+ "set no_ujian = case when no_ujian is null or trim(no_ujian) = '' then :noUjian else no_ujian end, "
					+ "    cetak_kartu = 1, "
					+ "    tanggal_dirubah = now() "
					+ "where id = :id").setParameter("noUjian", noUjian).setParameter("id", id).executeUpdate();

			if (updated <= 0) {
				throw new Exception("Data calon mahasiswa tidak ditemukan untuk update nomor ujian. ID=" + id);
			}

			tx.commit();

			String existing = ambilNoUjianDariDatabase(id);
			return isBlankString(existing) ? safeTrim(noUjian) : existing;
		} catch (Exception e) {
			if (tx != null && tx.isActive()) {
				try {
					tx.rollback();
				} catch (Exception rollback) { ais.common.ErrorAuditUtil.record(rollback, "auto-audit(empty-catch) src/ais/common/CommonPMB.java:375");
				}
			}
			throw e;
		} finally {
			tutupSessionLokal(session);
		}
	}

	private static boolean isErrorTimeoutAtauCancelDatabase(Throwable throwable) {
		Throwable current = throwable;
		while (current != null) {
			String className = current.getClass().getName();
			String message = current.getMessage();
			String lowerMessage = message == null ? "" : message.toLowerCase(java.util.Locale.ENGLISH);

			if (className.indexOf("PSQLException") >= 0 || className.indexOf("BatchUpdateException") >= 0
					|| className.indexOf("JDBCException") >= 0) {
				if (lowerMessage.indexOf("statement timeout") >= 0
						|| lowerMessage.indexOf("canceling statement due to user request") >= 0
						|| lowerMessage.indexOf("while updating tuple") >= 0
						|| lowerMessage.indexOf("could not execute jdbc batch update") >= 0
						|| lowerMessage.indexOf("could not execute update query") >= 0) {
					return true;
				}
			}

			if (lowerMessage.indexOf("biodata_calon_mahasiswa") >= 0
					&& (lowerMessage.indexOf("timeout") >= 0 || lowerMessage.indexOf("canceling statement") >= 0
							|| lowerMessage.indexOf("while updating tuple") >= 0)) {
				return true;
			}

			current = current.getCause();
		}
		return false;
	}

	private static String pesanErrorSingkat(Throwable throwable) {
		if (throwable == null) {
			return "";
		}
		String message = throwable.getMessage();
		if (!isBlankString(message)) {
			return message.trim();
		}
		return throwable.getClass().getName();
	}

	@SuppressWarnings({})
	public static String generateNoUjian(Tbmuser tbmuser, BiodataCalonMahasiswa biodataCalonMahasiswa,
			List<String> warnings) throws Exception {

		if (warnings == null) {
			warnings = new ArrayList<String>();
		}
		if (biodataCalonMahasiswa == null || biodataCalonMahasiswa.getId() == null) {
			warnings.add(Common.getBahasaConfig("Data calon mahasiswa tidak ditemukan."));
			return "";
		}

		try {
			String noUjianTersimpan = firstNotBlank(ambilNoUjianDariObject(biodataCalonMahasiswa),
					ambilNoUjianDariDatabase(biodataCalonMahasiswa.getId()));
			if (!isBlankString(noUjianTersimpan)) {
				String noUjianResult = simpanNoUjianLangsung(biodataCalonMahasiswa, noUjianTersimpan);
				daftarkanKeRuangUjianOtomatis(biodataCalonMahasiswa);
				return noUjianResult;
			}

			if (!Common.getApakahAdminLain(tbmuser)) {
				if (biodataCalonMahasiswa.getGelombangPendaftaran() != null && biodataCalonMahasiswa
						.getGelombangPendaftaran().getDokumenHarusDiverivikasiSebelumBisaCetakKartuUjian()) {
					if (!VerifikasiPMBHelper.checkVerifikasi(biodataCalonMahasiswa)) {
						return "";
					}
				}

				List<String> daftarWajibDiisi = KonfigurasiTampilanBiodataCalonMahasiswaAction
						.dataYangWajibDiisi(tbmuser);
				for (String key : daftarWajibDiisi) {
					if (Common.checkIsNull(BiodataCalonMahasiswa.class, biodataCalonMahasiswa, key)) {
						warnings.add(Common.getBahasaConfig("Biodata Anda harus dilengkapi. Data \""
								+ KonfigurasiTampilanBiodataCalonMahasiswaAction.keyDesc(key)
								+ "\" masih belum terisi dengan benar"));
						return "";
					}
				}
			}

			NoUjianGenerator noUjianGenerator = (NoUjianGenerator) Class
					.forName(Common.getKonfigurasi("class_untuk_generate_no_ujian",
							"ais.action.master.pmb.noujian.DefaultNoUjianGenerator").getNilai())
					.newInstance();

			String noUjian = noUjianGenerator.generateNoUjian(biodataCalonMahasiswa);
			noUjian = firstNotBlank(noUjian, ambilNoUjianDariObject(biodataCalonMahasiswa));
			if (!isBlankString(noUjian)) {
				daftarkanKeRuangUjianOtomatis(biodataCalonMahasiswa);
				return noUjian.trim();
			}
		} catch (Exception e) {
			String noUjianTersimpan = ambilNoUjianDariDatabase(biodataCalonMahasiswa.getId());
			if (!isBlankString(noUjianTersimpan)) {
				rollbackCurrentSessionNoUjianQuietly();
				return noUjianTersimpan;
			}

			String kandidatNoUjian = ambilNoUjianDariObject(biodataCalonMahasiswa);
			if (isErrorTimeoutAtauCancelDatabase(e) && !isBlankString(kandidatNoUjian)) {
				rollbackCurrentSessionNoUjianQuietly();
				try {
					String noUjianDisimpan = simpanNoUjianLangsung(biodataCalonMahasiswa, kandidatNoUjian);
					if (!isBlankString(noUjianDisimpan)) {
						return noUjianDisimpan;
					}
				} catch (Exception fallbackError) {
					warnings.add(Common.getBahasaConfig(
							"Nomor ujian sudah terbentuk, tetapi belum dapat disimpan karena data masih dipakai proses lain. Silakan coba cetak kartu ujian kembali beberapa saat lagi.")
							+ " \n\nError: " + pesanErrorSingkat(fallbackError));
					fallbackError.printStackTrace(); ais.common.ErrorAuditUtil.record(fallbackError, "auto-audit src/ais/common/CommonPMB.java:495");
					return "";
				}
			}

			warnings.add(
					Common.getBahasaConfig("Nomor Ujian tidak bisa di generate. Harap segera menghubungi Administrator")
							+ " \n\nError: " + pesanErrorSingkat(e));
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonPMB.java:503");
		}

		return "";

	}

	public static void uploadKelulusan(final File file, final EventListener eventListener) throws Exception {

		final boolean wajibbayar = Common.bolehKonfigurasi("calon_mahasiswa_wajib_melakukan_pembayaran_daftar_ulang_mahasiswa_baru");
		final boolean wajibLunas = Common.bolehKonfigurasi("calon_mahasiswa_wajib_melakukan_pembayaran_lunas_daftar_ulang_mahasiswa_baru", Konfigurasi.TIDAK_AKTIF);
		final Label peringatan = new Label("");

		final Label label = new Label(ais.common.Common.getBahasaConfig("Proses upload data data .."));
		final Label downloadPath = new Label("");
		final Label isiLaporan = new Label("");
		// FIX compile "cannot find symbol: report": report dipakai di dalam closure onTimer di
		// bawah, jadi HARUS dideklarasikan sebelum timer.addEventListener(...) dibuat, bukan setelahnya.
		final ais.common.UploadReportHelper report = new ais.common.UploadReportHelper("Upload Kelulusan PMB");
		Clients.showBusy(label.getValue());
		final Timer timer = new Timer(200);
		timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		timer.setRepeats(true);
		timer.addEventListener("onTimer", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Clients.showBusy(label.getValue());
				if (label.getValue().isEmpty()) {
					System.out.println("loading file " + file.getAbsolutePath());
					if (!downloadPath.getValue().isEmpty()) {
						try { org.zkoss.zul.Filedownload.save(new java.io.File(downloadPath.getValue()), "text/plain"); } catch (Exception eD) { ais.common.ErrorAuditUtil.record(eD, "auto-audit(empty-catch) CommonPMB-uploadKelulusan download-laporan"); }
					}
					tampilkanHasilUploadKelulusan(report, peringatan.getValue(), isiLaporan.getValue(),
							downloadPath.getValue(), eventListener);
					Clients.clearBusy();
					timer.detach();
				}

			}
		});
		timer.start();
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

				try {
					NimGenerator nimGenerator = (NimGenerator) Class.forName(Common
							.getKonfigurasi("class_untuk_generate_nim", "ais.action.master.pmb.nim.DefaultNimGenerator")
							.getNilai().trim()).newInstance();

					XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
					XSSFSheet sheet = workbook.getSheetAt(0);

					int rowCount = (sheet.getLastRowNum() + 1);
					for (int i = 1; i < rowCount; i++) {
						Session session = HibernateUtil.currentNativeSession();
						String identitasBaris = "Baris " + i;
						try {

							Long code = -1L;
							try {
								String content = Common.getCellContent(Common.getCell(sheet, 0, i));
								System.out.println("content = " + content);
								code = Long.parseLong(content);
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonPMB.java:564");

							}
							if (code.equals(-1L)) {
								try {

									String content = Common.getCellContent(Common.getCell(sheet, 1, i));
									System.out.println("noRegistrasi = " + content);
									code = (Long) session.createCriteria(BiodataCalonMahasiswa.class)
											.add(Restrictions.or(Restrictions.isNull("aktif"),
													Restrictions.eq("aktif", true)))
											.add(Restrictions.eq("noRegistrasi", content))
											.setProjection(Projections.property("id")).setMaxResults(1).uniqueResult();
								} catch (Exception e) {
									report.gagal(i, identitasBaris, e,
											"Gagal membaca/mencari No. Registrasi pada baris " + i + ".");
									continue;
								}

							}

							System.out.println("code = " + code);

							if (code == null || code.equals(-1L)) {
								report.gagal(i, identitasBaris, "ID/no registrasi calon mahasiswa tidak ditemukan.",
										"Pastikan kolom ID atau No. Registrasi pada file Excel sesuai dengan data PMB.");
								continue;
							}

							Jurusan prodiLulus = (Jurusan) Common.getSheetContentAsObject(sheet, 5, i, Jurusan.class,
									Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

							Boolean generateNimOtomatis = true;
							try {

								String content = Common.getCellContent(Common.getCell(sheet, 6, i));
								generateNimOtomatis = Boolean.parseBoolean(content);
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonPMB.java:597");

							}

							String nim = "";
							try {

								nim = Common.getCellContent(Common.getCell(sheet, 3, i));
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonPMB.java:605");

							}
							// Pada mode otomatis, kolom NIM di Excel memang tidak wajib diisi.
							// Abaikan nilai sel (termasuk placeholder lama) agar hasil generator
							// menjadi sumber NIM utama.
							if (generateNimOtomatis) {
								nim = "";
							}

							if (!generateNimOtomatis && nim.trim().isEmpty()) {
								report.gagal(i, identitasBaris,
										"NIM kosong, sedangkan Generate NIM Otomatis tidak dipilih.",
										"Isi kolom NIM atau ubah kolom Generate NIM Otomatis menjadi TRUE.");
								continue;
							}

							BiodataCalonMahasiswa biodataCalonMahasiswa = (BiodataCalonMahasiswa) ConstantValues
									.ambil(BiodataCalonMahasiswa.class.getName(), code);
							if (biodataCalonMahasiswa == null) {
								report.gagal(i, identitasBaris, "Data calon mahasiswa tidak ditemukan untuk ID " + code + ".",
										"Pastikan ID pada file Excel masih aktif dan belum terhapus.");
								continue;
							}
							identitasBaris = biodataCalonMahasiswa.getNoRegistrasi() + " - "
									+ biodataCalonMahasiswa.getNama();

							label.setValue("Upload data \"" + biodataCalonMahasiswa.getNama() + "\" ("
									+ Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");

							Kegiatan pembayaranDaftarUlang = biodataCalonMahasiswa.getPembayaranDaftarUlang();
							boolean tagihanDaftarUlangNol = isTagihanDaftarUlangNol(pembayaranDaftarUlang);

							if (!tagihanDaftarUlangNol && wajibbayar && (pembayaranDaftarUlang != null
									&& pembayaranDaftarUlang.getPersentaseLunas() != null
									&& pembayaranDaftarUlang.getPersentaseLunas() < 0.01)) {

								String my = "Calon mahasiswa \"" + biodataCalonMahasiswa
										+ "\" belum melakukan pembayaran daftar ulang.\n";

								peringatan.setValue(peringatan.getValue() + my);

								continue;
							} else if (!tagihanDaftarUlangNol && wajibLunas && (pembayaranDaftarUlang == null
									|| pembayaranDaftarUlang.getPersentaseLunas() == null
									|| pembayaranDaftarUlang.getPersentaseLunas() < 99.9)) {
								String my = "Calon mahasiswa ini belum melunasi biaya-biaya perkuliahan!";
								peringatan.setValue(peringatan.getValue() + my);

								continue;
							}

							// Pembaca sel Excel dapat memakai lalu menutup native session thread-local.
							// Ambil ulang sebelum query koreksi kelulusan agar tidak memakai referensi
							// session lama yang sudah tertutup.
							session = HibernateUtil.currentNativeSession();
							if (prodiLulus == null && biodataCalonMahasiswa.getMahasiswa() != null) {
								try {
									Mahasiswa mahasiswa = (Mahasiswa) session.createCriteria(Mahasiswa.class)
											.add(Restrictions.or(Restrictions.isNull("aktif"),
													Restrictions.eq("aktif", true)))
											.add(Restrictions.idEq(biodataCalonMahasiswa.getMahasiswa().getId()))
											.uniqueResult();
									biodataCalonMahasiswa.setMahasiswa(null);
									Common.refreshSaveOrUpdate(session, biodataCalonMahasiswa);
									Common.refreshDelete(session, mahasiswa);
								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
								}
								continue;
							}

							if (prodiLulus != null) {
								biodataCalonMahasiswa.setProdiLulus(prodiLulus);
							}

							if (prodiLulus != null && generateNimOtomatis
									&& (biodataCalonMahasiswa.getMahasiswa() == null
											|| biodataCalonMahasiswa.getMahasiswa().getNim() == null
											|| biodataCalonMahasiswa.getMahasiswa().getNim().trim().isEmpty()
											|| !isNimPmbValid(biodataCalonMahasiswa.getMahasiswa().getNim()))) {
								nim = nimGenerator.generateNim(biodataCalonMahasiswa);
							} else if (nim.trim().isEmpty() && biodataCalonMahasiswa.getMahasiswa() != null
									&& biodataCalonMahasiswa.getMahasiswa().getNim() != null
									&& !biodataCalonMahasiswa.getMahasiswa().getNim().trim().isEmpty()
									&& isNimPmbValid(biodataCalonMahasiswa.getMahasiswa().getNim())) {
								nim = biodataCalonMahasiswa.getMahasiswa().getNim();
							}

//							int count = ((Number) session.createCriteria(Mahasiswa.class)
//									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.eq("nim", nim))
//									.add(Restrictions.ne("biodataCalonMahasiswa", biodataCalonMahasiswa.getId()))
//									.setProjection(Projections.rowCount()).uniqueResult()).intValue();
//							if (count > 0) {
//								System.out.println("nim = " + nim + " sudah ada");
//								String my = "Calon mahasiswa \"" + biodataCalonMahasiswa + "\" nim = " + nim
//										+ " sudah ada di database.\n";
//								peringatan.setValue(peringatan.getValue() + my);
//
//								continue;
//							}

							System.out.println("biodataCalonMahasiswa = " + biodataCalonMahasiswa + ", prodiLulus = "
									+ prodiLulus + ", generateNimOtomatis = " + generateNimOtomatis + ", nim = " + nim);

							biodataCalonMahasiswa.setProdiLulus(prodiLulus);
							biodataCalonMahasiswa.setStatusLulus(prodiLulus == null ? BiodataCalonMahasiswa.TIDAK_LULUS
									: BiodataCalonMahasiswa.LULUS);

							Mahasiswa mahasiswa = null;
							// Sebagian implementasi NimGenerator memanggil HibernateUtil.closeSession()
							// setelah mengecek keunikan NIM. Karena itu referensi session di awal baris
							// tidak boleh dipakai lagi untuk transaksi penyimpanan hasil upload.
							session = HibernateUtil.currentNativeSession();
							session.getTransaction().begin();
							if (prodiLulus != null && !nim.trim().isEmpty()) {
								biodataCalonMahasiswa.setNim(nim);
								biodataCalonMahasiswa.setGenerateNimOtomatis(generateNimOtomatis);
								mahasiswa = CommonPMB.saveMahasiswa(session, biodataCalonMahasiswa, nim.trim(), false,
										false, !generateNimOtomatis);
							} else {
								Common.refreshUpdate(session, biodataCalonMahasiswa);
							}
							session.getTransaction().commit();

							report.sukses(i, biodataCalonMahasiswa.getNoRegistrasi() + " – " + biodataCalonMahasiswa.getNama(), "");
							if (mahasiswa != null) {
								CommonPMB.copyLampiran(biodataCalonMahasiswa, mahasiswa);
							}
							// session.disconnect();
							if (session.isOpen()) {
								session.disconnect();
								session.close();
							}
						} catch (Exception e) {

							try {
								// session.disconnect();
								if (session.isOpen()) {
									session.disconnect();
									session.close();
								}
							} catch (Exception es) { ais.common.ErrorAuditUtil.record(es, "auto-audit(empty-catch) src/ais/common/CommonPMB.java:719");
							}

							report.gagal(i, identitasBaris, e, "Periksa data baris " + i
									+ ", terutama Prodi Lulus, Jenjang, Kode Prodi, dan konfigurasi generate NIM.");
							Common.tampilErrorJikaAdmin(e);

						}
						HibernateUtil.closeSession();
					}

				} catch (Exception e1) {
					// TODO Auto-generated catch block
					Common.tampilErrorJikaAdmin(e1);
				}

				try {
					java.io.File rptFile = report.simpanLaporan();
					downloadPath.setValue(rptFile.getAbsolutePath());
					isiLaporan.setValue(report.getIsiLaporan());
				} catch (Exception eR) { ais.common.ErrorAuditUtil.record(eR, "auto-audit(empty-catch) CommonPMB-uploadKelulusan laporan"); }
				label.setValue("");
							} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();

	}

	private static void tampilkanHasilUploadKelulusan(final UploadReportHelper report, String peringatan,
			final String isiLaporan, final String downloadPath, final EventListener eventListener) throws Exception {
		String pesan = "Proses unggah data kelulusan telah berhasil dilakukan, Bapak/Ibu.";
		if (peringatan != null && !peringatan.trim().isEmpty()) {
			pesan += "\n" + peringatan;
		}
		pesan += "\n\n" + report.getRingkasan();
		if (report.getGagal() <= 0) {
			MyMessageboxConfig.show(pesan, "Pemberitahuan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, eventListener);
			return;
		}

		final Window win = new Window();
		win.setTitle("Pemberitahuan");
		win.setBorder("normal");
		win.setClosable(true);
		win.setWidth("430px");
		win.setSizable(false);
		win.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

		Vbox body = new Vbox();
		body.setSpacing("12px");
		body.setStyle("padding:18px");
		body.setParent(win);

		Label labelPesan = new Label(pesan + "\n\nKlik Rinci untuk melihat alasan baris yang gagal.");
		labelPesan.setMultiline(true);
		labelPesan.setParent(body);

		Hbox tombol = new Hbox();
		tombol.setSpacing("8px");
		tombol.setStyle("justify-content:center;width:100%;padding-top:8px");
		tombol.setParent(body);

		Button rinci = new Button("Rinci");
		rinci.setParent(tombol);
		rinci.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				tampilkanRincianUploadKelulusan(isiLaporan == null || isiLaporan.trim().isEmpty()
						? report.getIsiLaporan() : isiLaporan, downloadPath);
			}
		});

		Button ok = new Button("OK");
		ok.setParent(tombol);
		ok.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				win.detach();
				if (eventListener != null) {
					eventListener.onEvent(event);
				}
			}
		});

		win.doModal();
	}

	private static void tampilkanRincianUploadKelulusan(final String isiLaporan, final String downloadPath)
			throws Exception {
		final Window win = new Window();
		win.setTitle("Rincian Gagal Upload Kelulusan");
		win.setBorder("normal");
		win.setClosable(true);
		win.setWidth("720px");
		win.setHeight("520px");
		win.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

		Vbox body = new Vbox();
		body.setSpacing("8px");
		body.setStyle("padding:12px;width:100%;height:100%");
		body.setParent(win);

		Textbox detail = new Textbox(isiLaporan == null || isiLaporan.trim().isEmpty()
				? "Rincian belum tersedia." : isiLaporan);
		detail.setReadonly(true);
		detail.setMultiline(true);
		detail.setRows(20);
		detail.setWidth("98%");
		detail.setHeight("400px");
		detail.setParent(body);

		Hbox tombol = new Hbox();
		tombol.setSpacing("8px");
		tombol.setParent(body);

		Button download = new Button("Download Rincian");
		download.setParent(tombol);
		download.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (downloadPath != null && !downloadPath.trim().isEmpty()) {
					Filedownload.save(new java.io.File(downloadPath), "text/plain");
				}
			}
		});

		Button tutup = new Button("Tutup");
		tutup.setParent(tombol);
		tutup.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				win.detach();
			}
		});

		win.doModal();
	}

	public static void uploadDataCalonMahasiswa(final File file, final EventListener eventListener,
			final String[] contents) throws Exception {

		final Label peringatan = new Label("");

		final Label label = new Label(ais.common.Common.getBahasaConfig("Proses upload data data .."));
		final Label downloadPath = new Label("");
		// FIX compile "cannot find symbol: report": harus dideklarasikan sebelum
		// timer.addEventListener(...) karena dipakai di dalam closure onTimer di bawah.
		final ais.common.UploadReportHelper report = new ais.common.UploadReportHelper("Upload Data Calon Mahasiswa PMB");
		Clients.showBusy(label.getValue());
		final Timer timer = new Timer(200);
		timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		timer.setRepeats(true);
		timer.addEventListener("onTimer", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Clients.showBusy(label.getValue());
				if (label.getValue().isEmpty()) {
					System.out.println("loading file " + file.getAbsolutePath());
					if (!downloadPath.getValue().isEmpty()) {
						try { org.zkoss.zul.Filedownload.save(new java.io.File(downloadPath.getValue()), "text/plain"); } catch (Exception eD) { ais.common.ErrorAuditUtil.record(eD, "auto-audit(empty-catch) CommonPMB-uploadData download-laporan"); }
					}
					MyMessageboxConfig.showFormatCb(
							"Proses unggah data calon mahasiswa telah berhasil dilakukan, Bapak/Ibu.{V1}\n\n" + report.getRingkasan(),
							"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, eventListener,
							(peringatan.getValue().isEmpty() ? "" : "\n" + peringatan.getValue()));
					Clients.clearBusy();
					timer.detach();
				}

			}
		});
		timer.start();
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

				try {

					XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
					XSSFSheet sheet = workbook.getSheetAt(0);

					ClassMetadata classMetadata = HibernateUtil.getClassMetadata(BiodataCalonMahasiswa.class);
					Session session = HibernateUtil.currentNativeSession();
					int rowCount = (sheet.getLastRowNum() + 1);
					for (int i = 1; i < rowCount; i++) {
						try {

							String nama = Common.getSheetContentAsString(sheet, 3, i);

							System.out.println("memproses nama = " + nama);

							if (nama == null || nama.trim().isEmpty()) {
								break;
							}

							Long id = Common.getSheetContentAsLong(sheet, 0, i);
							// getSheetContentAsString/Long menutup session native thread-local; ambil ulang
							// sebelum createCriteria agar tidak "Session is closed!".
							session = HibernateUtil.currentNativeSession();
							BiodataCalonMahasiswa biodataCalonMahasiswa = id == null || id.equals(-1L) ? null
									: (BiodataCalonMahasiswa) session.createCriteria(BiodataCalonMahasiswa.class)
											.add(Restrictions.or(Restrictions.isNull("aktif"),
													Restrictions.eq("aktif", true)))
											.add(Restrictions.idEq(id)).uniqueResult();
							String noRegistrasi = Common.getSheetContentAsString(sheet, 1, i);

							if (biodataCalonMahasiswa == null) {
								biodataCalonMahasiswa = noRegistrasi == null || noRegistrasi.equals("-----")
										|| noRegistrasi.trim().isEmpty()
												? null
												: (BiodataCalonMahasiswa) session
														.createCriteria(BiodataCalonMahasiswa.class)
														.add(Restrictions.or(Restrictions.isNull("aktif"),
																Restrictions.eq("aktif", true)))
														.add(Restrictions.eq("noRegistrasi", noRegistrasi))
														.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
							}

							String noUjian = Common.getSheetContentAsString(sheet, 2, i);

							if (biodataCalonMahasiswa == null) {
								biodataCalonMahasiswa = noUjian == null || noUjian.equals("-----")
										|| noUjian.trim().isEmpty()
												? null
												: (BiodataCalonMahasiswa) session
														.createCriteria(BiodataCalonMahasiswa.class)
														.add(Restrictions.or(Restrictions.isNull("aktif"),
																Restrictions.eq("aktif", true)))
														.add(Restrictions.eq("noUjian", noUjian))
														.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
							}

							if (biodataCalonMahasiswa == null) {
								biodataCalonMahasiswa = new BiodataCalonMahasiswa();
							}

							if (biodataCalonMahasiswa.getNoRegistrasi() == null
									|| biodataCalonMahasiswa.getNoRegistrasi().trim().isEmpty()) {
								biodataCalonMahasiswa
										.setNoRegistrasi(CommonPMB.generateNoRegistrasi(biodataCalonMahasiswa));
							}

							Common.setObjectValues(classMetadata, biodataCalonMahasiswa, contents, 3, sheet, i);

							// Paksa email, hp, teleponRumah, noTelpOrtu dari sel Excel secara eksplisit
							// — hindari side-effect getter (misal getHp() fallback ke teleponRumah, atau
							// getEmail() fallback ke mahasiswa.email). Cari indeks dinamis agar kompatibel
							// bila contents berubah urutan.
							int emailIdx = -1, hpIdx = -1, teleponRumahIdx = -1, noTelpOrtuIdx = -1;
							for (int ci = 0; ci < contents.length; ci++) {
								if ("email".equals(contents[ci]) && emailIdx < 0) emailIdx = ci;
								if ("hp".equals(contents[ci]) && hpIdx < 0) hpIdx = ci;
								if ("teleponRumah".equals(contents[ci]) && teleponRumahIdx < 0) teleponRumahIdx = ci;
								if ("noTelpOrtu".equals(contents[ci]) && noTelpOrtuIdx < 0) noTelpOrtuIdx = ci;
							}
							if (emailIdx >= 0) {
								String emailVal = Common.getSheetContentAsString(sheet, emailIdx, i);
								if (emailVal != null && !emailVal.trim().isEmpty()) {
									emailVal = emailVal.trim();
									// Strip tanda petik (') di depan — konvensi format teks Excel
									while (emailVal.startsWith("'")) emailVal = emailVal.substring(1).trim();
									biodataCalonMahasiswa.setEmail(emailVal);
								}
							}
							if (hpIdx >= 0) {
								String hpVal = Common.getSheetContentAsString(sheet, hpIdx, i);
								if (hpVal != null && !hpVal.trim().isEmpty()) {
									hpVal = hpVal.trim();
									while (hpVal.startsWith("'")) hpVal = hpVal.substring(1).trim();
									biodataCalonMahasiswa.setHp(hpVal);
								}
							}
							if (teleponRumahIdx >= 0) {
								String telRumahVal = Common.getSheetContentAsString(sheet, teleponRumahIdx, i);
								if (telRumahVal != null && !telRumahVal.trim().isEmpty()) {
									telRumahVal = telRumahVal.trim();
									while (telRumahVal.startsWith("'")) telRumahVal = telRumahVal.substring(1).trim();
									biodataCalonMahasiswa.setTeleponRumah(telRumahVal);
								}
							}
							if (noTelpOrtuIdx >= 0) {
								String noTelpOrtuVal = Common.getSheetContentAsString(sheet, noTelpOrtuIdx, i);
								if (noTelpOrtuVal != null && !noTelpOrtuVal.trim().isEmpty()) {
									noTelpOrtuVal = noTelpOrtuVal.trim();
									while (noTelpOrtuVal.startsWith("'")) noTelpOrtuVal = noTelpOrtuVal.substring(1).trim();
									biodataCalonMahasiswa.setNoTelpOrtu(noTelpOrtuVal);
								}
							}

							biodataCalonMahasiswa.setNoUjian(noUjian);
							biodataCalonMahasiswa.setNoRegistrasi(noRegistrasi);

							// Ambil ulang session native (getSheetContent* menutupnya) sebelum transaksi
							// simpan agar getTransaction()/saveOrUpdate tidak kena "Session is closed!".
							session = HibernateUtil.currentNativeSession();
							session.getTransaction().begin();
							session.saveOrUpdate(biodataCalonMahasiswa);
							session.getTransaction().commit();

							label.setValue("Upload data \"" + biodataCalonMahasiswa.getNama() + "\" ("
									+ Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");
							report.sukses(i, biodataCalonMahasiswa.getNoRegistrasi() + " – " + biodataCalonMahasiswa.getNama(), "");

						} catch (Exception e) {
							report.gagal(i, String.valueOf(i), e, "Periksa data baris " + i);
							Common.tampilErrorJikaAdmin(e);
						}

					}
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/common/CommonPMB.java:910");
				}

				HibernateUtil.closeSession();

				try {
					java.io.File rptFile = report.simpanLaporan();
					downloadPath.setValue(rptFile.getAbsolutePath());
				} catch (Exception eR) { ais.common.ErrorAuditUtil.record(eR, "auto-audit(empty-catch) CommonPMB-uploadData laporan"); }
				label.setValue("");
							} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();
	}

	public static Mahasiswa saveMahasiswa(Session session, BiodataCalonMahasiswa calonMahasiswa, String nim) {
		return saveMahasiswa(session, calonMahasiswa, nim, false);
	}

	public static Mahasiswa saveMahasiswa(Session session, BiodataCalonMahasiswa calonMahasiswa, String nim,
			boolean commitMaual) {
		return saveMahasiswa(session, calonMahasiswa, nim, commitMaual, false);
	}

	public static synchronized Mahasiswa saveMahasiswa(Session session, BiodataCalonMahasiswa calonMahasiswa, String nim,
			boolean commitMaual, boolean izinkanNimDenganTandaHubung) {
		return saveMahasiswa(session, calonMahasiswa, nim, commitMaual,
				izinkanNimDenganTandaHubung, true);
	}

	public static synchronized Mahasiswa saveMahasiswa(Session session, BiodataCalonMahasiswa calonMahasiswa, String nim,
			boolean commitMaual, boolean izinkanNimDenganTandaHubung, boolean gunakanNimRiwayat) {

		Mahasiswa mahasiswa = calonMahasiswa.getMahasiswa();
		// Cek dulu lewat FK biodataCalonMahasiswa (lebih presisi & tak rawan salah tangkap
		// dibanding pencocokan nama/tanggal lahir di bawah) -- root cause ConstraintViolationException
		// "biodata_calon_mahasiswa_long": bila calonMahasiswa ini SUDAH pernah di-generate NIM-nya
		// (mis. via genNim di thread lain / percobaan sebelumnya) tapi calonMahasiswa.getMahasiswa()
		// belum ter-refresh, pencocokan fuzzy nama/tanggallahir bisa saja meleset (typo, format beda)
		// sehingga kode di bawah membuat Mahasiswa BARU dengan biodataCalonMahasiswa yang SAMA -> unique
		// constraint violation saat insert.
		if (mahasiswa == null && calonMahasiswa != null && calonMahasiswa.getId() != null) {
			mahasiswa = (Mahasiswa) ConstantValues.simpleObject(
					session.createCriteria(Mahasiswa.class)
							.add(Restrictions.eq("biodataCalonMahasiswa", calonMahasiswa.getId())).setMaxResults(1),
					Mahasiswa.class);
		}
		if (mahasiswa == null && calonMahasiswa != null && calonMahasiswa.getProdiLulus() != null) {
			mahasiswa = (Mahasiswa) ConstantValues.simpleObject(
					session.createCriteria(Mahasiswa.class)
							.add(Restrictions.ilike("nama", calonMahasiswa.getNama(), MatchMode.EXACT))
							.add(Restrictions.eq("tanggallahir", calonMahasiswa.getTanggalLahir()))
							.add(Restrictions.eq("tahunangkatan", calonMahasiswa.getTahun()))
							.add(Restrictions.eq("jurusan", calonMahasiswa.getProdiLulus())).setMaxResults(1),
					Mahasiswa.class);
		}
		if (gunakanNimRiwayat) {
			String nimRiwayat = ambilNimTersimpanDariRiwayatPmb(session, calonMahasiswa, mahasiswa);
			if (!isBlankString(nimRiwayat)) {
				nim = nimRiwayat;
			}
		}
		if (!isNimPmbValidAtauDiizinkan(nim, izinkanNimDenganTandaHubung)) {
			throw new IllegalArgumentException("NIM PMB tidak valid: " + nim
					+ ". Generate ulang NIM diperlukan karena NIM masih mengandung placeholder '-' atau '_'.");
		}
		Mahasiswa pemilikNim = (Mahasiswa) ConstantValues.simpleObject(
				session.createCriteria(Mahasiswa.class).add(Restrictions.eq("nimKey", nim.trim())).setMaxResults(1),
				Mahasiswa.class);
		if (pemilikNim != null && (mahasiswa == null || mahasiswa.getId() == null
				|| !pemilikNim.getId().equals(mahasiswa.getId()))) {
			Long biodataPemilik = pemilikNim.getBiodataCalonMahasiswa();
			if (calonMahasiswa.getId() != null && calonMahasiswa.getId().equals(biodataPemilik)) {
				// Request paralel untuk calon yang sama: gunakan baris yang sudah lebih dulu dibuat.
				mahasiswa = pemilikNim;
			} else {
				throw new IllegalArgumentException("NIM " + nim
						+ " sudah digunakan mahasiswa lain. Generate ulang NIM untuk calon mahasiswa ini.");
			}
		}
		if (mahasiswa == null) {
			mahasiswa = new Mahasiswa();
			mahasiswa.setPass(Common.desEncrypter.get().encrypt(nim));
			mahasiswa.setIs_encripted(true);
		}

//		else {
//			nim = mahasiswa.getNim();
//		}

		mahasiswa.setKonsentrasi(calonMahasiswa.getKonsentrasi());
		mahasiswa.setBiodataCalonMahasiswa(calonMahasiswa.getId());
		mahasiswa.setAlamat(calonMahasiswa.getAlamat());
		mahasiswa.setEmail(calonMahasiswa.getEmail());
		mahasiswa.setKelamin(calonMahasiswa.getJenisKelamin());
		mahasiswa.setNama(calonMahasiswa.getNama());
		mahasiswa.setNim(nim);
		mahasiswa.setSemesterMulai(calonMahasiswa.getGelombangPendaftaran() == null ? Perkuliahan.GANJIL
				: calonMahasiswa.getGelombangPendaftaran().getJenisSemester());
		mahasiswa.setTahunangkatan(calonMahasiswa.getTahun());
		mahasiswa.setTanggallahir(calonMahasiswa.getTanggalLahir());
		mahasiswa.setTelp(calonMahasiswa.getTeleponRumah());
		mahasiswa.setTempatlahir(calonMahasiswa.getTempatLahir());
		mahasiswa.setJenjang(calonMahasiswa.getJenjang());
		mahasiswa.setJurusan(calonMahasiswa.getProdiLulus());
		mahasiswa.setWarganegara(
				calonMahasiswa.getKewarganegaraan() == null ? "WNI" : calonMahasiswa.getKewarganegaraan());

		mahasiswa.setStatusAwalMahasiswa(calonMahasiswa.getMerupakanPindahan() ? ConstantValues.PINDAHAN
				: calonMahasiswa.getStatusAwalMahasiswa());
		mahasiswa.setMerupakanPindahan(calonMahasiswa.getMerupakanPindahan());
		mahasiswa.setPindahanDariKampus(calonMahasiswa.getPindahanDariKampus());
		mahasiswa.setNimLamaSebelumPindah(calonMahasiswa.getNimLamaSebelumPindah());
		mahasiswa.setNamaProdiPindah(calonMahasiswa.getPindahanDariProdi());
		mahasiswa.setPindahanPerguruanTinggi(calonMahasiswa.getPindahanDariKampus());
		mahasiswa.setPindahDariKampusLamaDiSemester(calonMahasiswa.getPindahDariKampusLamaDiSemester());
		mahasiswa.setPindahKeKampusIniMasukSemester(calonMahasiswa.getPindahDariKampusLamaDiSemester());
		mahasiswa.setKeteranganPindah(calonMahasiswa.getKeteranganPindah());
		mahasiswa.setTanggalPindah(calonMahasiswa.getTanggalPindah());

		mahasiswa.setProgram(calonMahasiswa.getProgram());
		mahasiswa.setJenisSeleksi(calonMahasiswa.getJenisSeleksi());

		mahasiswa.setNegara(calonMahasiswa.getKewarganegaraan_asli() != null
				&& calonMahasiswa.getKewarganegaraan_asli().equals(Mahasiswa.WNI) ? ConstantValues.INDONESIA : null);
		mahasiswa.setSemesterMulai(
				calonMahasiswa.getSemesterMulai() == null ? Perkuliahan.GANJIL : calonMahasiswa.getSemesterMulai());
		mahasiswa.setTanggalMasuk(ais.ui.util.WaktuUtil.getDate());
		mahasiswa.setPindahanDari(calonMahasiswa.getPindahanDari());
		mahasiswa.setAgama(calonMahasiswa.getAgama());

		boolean mahasiswaBaru = mahasiswa.getId() == null;
		boolean transaksiAktifSebelumSimpan = session.getTransaction() != null
				&& session.getTransaction().isActive();
		if (commitMaual && !transaksiAktifSebelumSimpan) {
			session.getTransaction().begin();
		}
		try {
			Common.refreshSaveOrUpdate(session, mahasiswa);
		} catch (RuntimeException eSimpanMahasiswa) {
			// FIX ConstraintViolationException "biodata_calon_mahasiswa_long": bila mahasiswa BARU
			// (belum punya id) gagal insert karena UNIQUE constraint pada kolom biodataCalonMahasiswa,
			// itu artinya proses LAIN sudah lebih dulu berhasil membuat Mahasiswa untuk calonMahasiswa
			// yang SAMA (race condition, mis. genNim dipanggil bersamaan dari dua request/thread).
			// Alih-alih meledak, pakai baris yang SUDAH dibuat proses lain tsb.
			boolean konstraintBiodataCalonMahasiswa = mahasiswaBaru
					&& eSimpanMahasiswa.getMessage() != null
					&& eSimpanMahasiswa.getMessage().toLowerCase().indexOf("biodata_calon_mahasiswa_long") >= 0;
			if (!konstraintBiodataCalonMahasiswa) {
				Throwable penyebab = eSimpanMahasiswa.getCause();
				while (penyebab != null && !konstraintBiodataCalonMahasiswa) {
					konstraintBiodataCalonMahasiswa = penyebab.getMessage() != null
							&& penyebab.getMessage().toLowerCase().indexOf("biodata_calon_mahasiswa_long") >= 0;
					penyebab = penyebab.getCause();
				}
			}
			if (!konstraintBiodataCalonMahasiswa) {
				throw eSimpanMahasiswa;
			}
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception eRollback) { ais.common.ErrorAuditUtil.record(eRollback, "auto-audit(empty-catch) src/ais/common/CommonPMB.java:saveMahasiswa-rollback"); }
			try { session.clear(); } catch (Exception eClear) { ais.common.ErrorAuditUtil.record(eClear, "auto-audit(empty-catch) src/ais/common/CommonPMB.java:saveMahasiswa-clear"); }
			System.out.println("CommonPMB.saveMahasiswa: request paralel biodataCalonMahasiswa id="
					+ (calonMahasiswa.getId() == null ? "null" : calonMahasiswa.getId())
					+ " -- menggunakan Mahasiswa yang sudah dibuat request lain");
			if ((transaksiAktifSebelumSimpan || commitMaual)
					&& (session.getTransaction() == null || !session.getTransaction().isActive())) {
				session.beginTransaction();
			}
			Mahasiswa mahasiswaSudahAda = (Mahasiswa) ConstantValues.simpleObject(
					session.createCriteria(Mahasiswa.class)
							.add(Restrictions.eq("biodataCalonMahasiswa", calonMahasiswa.getId())).setMaxResults(1),
					Mahasiswa.class);
			if (mahasiswaSudahAda == null) {
				// Belum ketemu (proses lain belum commit) -- tak ada yang bisa dipakai, lempar lagi.
				throw eSimpanMahasiswa;
			}
			mahasiswa = mahasiswaSudahAda;
		}
		if (commitMaual) {
			session.getTransaction().commit();
		}

		if (commitMaual) {
			session.getTransaction().begin();
		}
		CommonPMB.saveBiodataMahasiswa(session, mahasiswa, calonMahasiswa);
		if (commitMaual) {
			session.getTransaction().commit();
		}

		calonMahasiswa.setMahasiswa(mahasiswa);

		calonMahasiswa.setNimGenerated(1);

		if (commitMaual) {
			session.getTransaction().begin();
		}
		Common.refreshUpdate(session, calonMahasiswa);
		if (commitMaual) {
			session.getTransaction().commit();
		}

		return mahasiswa;
	}

	public static void saveBiodataMahasiswa(Session session, Mahasiswa mahasiswa,
			BiodataCalonMahasiswa calonMahasiswa) {

		BiodataMahasiswa biodataMahasiswa = mahasiswa == null || mahasiswa.getId() == null ? new BiodataMahasiswa()
				: (BiodataMahasiswa) session.createCriteria(BiodataMahasiswa.class)
						.add(Restrictions.eq("mahasiswa", mahasiswa)).setMaxResults(1).uniqueResult();

		if (biodataMahasiswa == null) {
			biodataMahasiswa = new BiodataMahasiswa();
		}
		biodataMahasiswa.setMahasiswa(mahasiswa);
		biodataMahasiswa.setAlamat(calonMahasiswa.getAlamat());
		biodataMahasiswa.setNamaAyah(calonMahasiswa.getNamaAyah());
		biodataMahasiswa.setNamaIbu(calonMahasiswa.getNamaIbu());

		biodataMahasiswa.setNamaUntukIjazah(calonMahasiswa.getNamaUntukIjazah());
		biodataMahasiswa.setNoIjazah(calonMahasiswa.getNoIjazah());
		biodataMahasiswa.setUkuranJaket(calonMahasiswa.getUkuranJaket());
		biodataMahasiswa.setTinggiBadan(calonMahasiswa.getTinggiBadan());
		biodataMahasiswa.setPernahMenetapDiLuarNegeri(calonMahasiswa.getPernahMenetapDiLuarNegeri());
		biodataMahasiswa.setBeratBadan(calonMahasiswa.getBeratBadan());
		biodataMahasiswa.setTeleponRumah(calonMahasiswa.getTeleponRumah());
		biodataMahasiswa.setHp(calonMahasiswa.getHp());
		biodataMahasiswa.setSuratIzinMengemudi(calonMahasiswa.getSuratIzinMengemudi());
		biodataMahasiswa.setKendaraanKuliah(calonMahasiswa.getKendaraanKuliah());
		biodataMahasiswa.setPernahMemimpinOrganisasi(calonMahasiswa.getPernahMemimpinOrganisasi());
		biodataMahasiswa.setNamaOrganisasi(calonMahasiswa.getNamaOrganisasi());
		biodataMahasiswa.setHobi(calonMahasiswa.getHobi());
		biodataMahasiswa.setMinatSeni(calonMahasiswa.getMinatSeni());
		biodataMahasiswa.setKemampuanBahasa1(calonMahasiswa.getKemampuanBahasa1());
		biodataMahasiswa.setKemampuanBahasa2(calonMahasiswa.getKemampuanBahasa2());
		biodataMahasiswa.setKemampuanBahasa3(calonMahasiswa.getKemampuanBahasa3());
		biodataMahasiswa.setAsalSma(calonMahasiswa.getAsalSma());
		biodataMahasiswa.setNamaSekolahAsal(calonMahasiswa.getNamaSekolahAsal());
		biodataMahasiswa.setAlamatAsalSma(calonMahasiswa.getAlamatAsalSma());
		biodataMahasiswa.setAsalSmp(calonMahasiswa.getAsalSmp());
		biodataMahasiswa.setAlamatAsalSmp(calonMahasiswa.getAlamatAsalSmp());
		biodataMahasiswa.setAsalSd(calonMahasiswa.getAsalSd());
		biodataMahasiswa.setAlamatAsalSd(calonMahasiswa.getAlamatAsalSd());
		biodataMahasiswa.setGolonganDarah(calonMahasiswa.getGolonganDarah());
		biodataMahasiswa.setStatusNikah(calonMahasiswa.getStatusNikah());
		biodataMahasiswa.setAgama(calonMahasiswa.getAgama());
		biodataMahasiswa.setDusun(calonMahasiswa.getDusunCalon());
		biodataMahasiswa.setKelurahan(calonMahasiswa.getKelurahanCalon());
		biodataMahasiswa.setKecamatan(calonMahasiswa.getKecamatanCalon());
		biodataMahasiswa.setKota(calonMahasiswa.getKotaCalon());
		biodataMahasiswa.setPropinsi(calonMahasiswa.getPropinsiCalon());
		biodataMahasiswa.setNoIdentitas(calonMahasiswa.getNoIdentitas());
		biodataMahasiswa.setPendapatanOrtu(calonMahasiswa.getPendapatanOrtu());
		biodataMahasiswa.setPendapatanOrtuIbu(calonMahasiswa.getPendapatanOrtuIbu());
		biodataMahasiswa.setPendapatanWali(calonMahasiswa.getPendapatanOrtuWali());
		biodataMahasiswa.setNoIdentitas(calonMahasiswa.getNoIdentitas());
		biodataMahasiswa.setJenisSekolah(calonMahasiswa.getJenisSekolah());
		biodataMahasiswa.setPendidikanAyah(calonMahasiswa.getPendidikanOrtu());
		biodataMahasiswa.setPendidikanIbu(calonMahasiswa.getPendidikanOrtuIbu());
		biodataMahasiswa.setPendidikanWali(calonMahasiswa.getPendidikanOrtuWali());
		biodataMahasiswa.setPekerjaanAyah(calonMahasiswa.getPekerjaanAyah());
		biodataMahasiswa.setPekerjaanIbu(calonMahasiswa.getPekerjaanAyahIbu());
		biodataMahasiswa.setPekerjaanWali(calonMahasiswa.getPekerjaanAyahWali());

		Common.refreshSaveOrUpdate(session, biodataMahasiswa);

	}

	@SuppressWarnings("unchecked")
	public static void copyLampiran(BiodataCalonMahasiswa biodataCalonMahasiswa, Mahasiswa mahasiswa) {
		// Proteksi null untuk mencegah NullPointerException
		if (biodataCalonMahasiswa == null || mahasiswa == null) {
			return;
		}

		Session session = null;
		org.hibernate.Transaction tx = null;

		try {
			// Membuka session secara lokal (Thread-Safe)
			session = StreamingHibernateUtil.getInstance().openSession();
			tx = session.beginTransaction();

			// =========================================================================
			// 1. PROSES FOTO BIODATA -> MAHASISWA
			// =========================================================================
			FotoBiodataCalonMahasiswa fotobiodataCalonMahasiswa = (FotoBiodataCalonMahasiswa) session
					.createCriteria(FotoBiodataCalonMahasiswa.class)
					.add(Restrictions.eq("biodataCalonMahasiswa", biodataCalonMahasiswa.getId())).setMaxResults(1)
					.uniqueResult();

			if (fotobiodataCalonMahasiswa != null) {
				FotoMahasiswa fotoMahasiswa = (FotoMahasiswa) session.createCriteria(FotoMahasiswa.class)
						.addOrder(Order.desc("id")).add(Restrictions.eq("mahasiswa", mahasiswa.getId()))
						.setMaxResults(1).uniqueResult();

				if (fotoMahasiswa == null) {
					fotoMahasiswa = new FotoMahasiswa();
				}
				fotoMahasiswa.setNama(fotobiodataCalonMahasiswa.getNama());
				fotoMahasiswa.setKeterangan(fotobiodataCalonMahasiswa.getKeterangan());
				fotoMahasiswa.setMahasiswa(mahasiswa.getId());
				fotoMahasiswa.setGdrive(fotobiodataCalonMahasiswa.getGdrive());
				fotoMahasiswa.setGdriveUsername(fotobiodataCalonMahasiswa.getGdriveUsername());
				fotoMahasiswa.setLink(fotobiodataCalonMahasiswa.getLink());
				fotoMahasiswa.setFoto(salinBlobDalamTransaksi(fotobiodataCalonMahasiswa.getFoto()));

				session.saveOrUpdate(fotoMahasiswa);
			}

			// =========================================================================
			// 2. PROSES LAMPIRAN LAIN BIODATA -> MAHASISWA
			// =========================================================================
			List<Long> lampiranLainBiodataCalonMahasiswas = session
					.createCriteria(LampiranLainBiodataCalonMahasiswa.class)
					.add(Restrictions.eq("biodataCalonMahasiswa", biodataCalonMahasiswa.getId()))
					.setProjection(Projections.property("id")).addOrder(Order.asc("id")).list();

			for (Long idLampiran : lampiranLainBiodataCalonMahasiswas) {
				// Menggunakan session.get() jauh lebih efisien dan cepat daripada membuat
				// criteria baru untuk pencarian ID
				LampiranLainBiodataCalonMahasiswa lampiranLainBiodataCalonMahasiswa = (LampiranLainBiodataCalonMahasiswa) session
						.get(LampiranLainBiodataCalonMahasiswa.class, idLampiran);

				if (lampiranLainBiodataCalonMahasiswa != null) {
					LampiranLainMahasiswa lampiranLainMahasiswa = (LampiranLainMahasiswa) session
							.createCriteria(LampiranLainMahasiswa.class).addOrder(Order.desc("id"))
							.add(Restrictions.eq("mahasiswa", mahasiswa.getId()))
							.add(Restrictions.eq("jenis", lampiranLainBiodataCalonMahasiswa.getJenis()))
							.setMaxResults(1).uniqueResult();

					if (lampiranLainMahasiswa == null) {
						lampiranLainMahasiswa = new LampiranLainMahasiswa();
					}
					lampiranLainMahasiswa.setNama(lampiranLainBiodataCalonMahasiswa.getNama());
					lampiranLainMahasiswa.setKeterangan(lampiranLainBiodataCalonMahasiswa.getKeterangan());
					lampiranLainMahasiswa.setMahasiswa(mahasiswa.getId());
					lampiranLainMahasiswa.setFoto(
							salinBlobDalamTransaksi(lampiranLainBiodataCalonMahasiswa.getFoto()));
					lampiranLainMahasiswa.setJenis(lampiranLainBiodataCalonMahasiswa.getJenis());
					lampiranLainMahasiswa.setGdrive(lampiranLainBiodataCalonMahasiswa.getGdrive());
					lampiranLainMahasiswa.setGdriveUsername(lampiranLainBiodataCalonMahasiswa.getGdriveUsername());
					lampiranLainMahasiswa.setLink(lampiranLainBiodataCalonMahasiswa.getLink());

					session.saveOrUpdate(lampiranLainMahasiswa);
				}
			}

			// Simpan semua perubahan database dalam satu kali commit
			tx.commit();
		} catch (Exception e1) {
			if (tx != null && tx.isActive()) {
				tx.rollback();
			}
			e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/common/CommonPMB.java:1175");
		} finally {
			// WAJIB: Membersihkan dan menutup koneksi database agar tidak terjadi
			// memory/connection leak
			if (session != null) {
				try {
					session.clear();
					session.disconnect();
					session.close();
				} catch (Exception ex) {
					ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/common/CommonPMB.java:1185");
				}
			}
		}

		// =========================================================================
		// 3. GENERATE REPORT & KIRIM EMAIL
		// =========================================================================
		try {
			boolean broadcast_ketika_dapat_nim = Common.bolehKonfigurasi("broadcast_ketika_dapat_nim", Konfigurasi.TIDAK_AKTIF);
			if (broadcast_ketika_dapat_nim) {
				java.util.Map<String, java.io.Serializable> parameters = ais.common.HashMapGenerator
						.getRandStringSerializable();
				parameters.put("nim", biodataCalonMahasiswa.getNim());

				biodataCalonMahasiswa.putPhoto(parameters);

				File file = null;
				if (org.zkoss.zk.ui.Sessions.getCurrent() != null
						&& org.zkoss.zk.ui.Sessions.getCurrent().getWebApp() != null) {
					file = Report.generateFileReport(Report.PDF, parameters, "Biodata_Nim",
							ais.ui.util.WaktuUtil.getDate(), Common.locale);
				} else {
					file = Report.generateFileReportSimple(Report.PDF, parameters, "Biodata_Nim");
				}

				if (file != null) {
					CommonEmail.infoNimMahasiswa(biodataCalonMahasiswa, file);
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonPMB.java:1216");
		}
	}

	public static String onGenerateNim(final BiodataCalonMahasiswa calonMahasiswa, NimGenerator nimGenerator) throws Exception {

	    if (calonMahasiswa.getProdiLulus() == null) {
	        MyMessageboxConfig.show("Mohon maaf, Bapak/Ibu. Calon mahasiswa ini belum dinyatakan lulus sehingga NIM belum dapat dibuat. Langkah yang dapat dilakukan: (1) periksa kembali status kelulusan calon mahasiswa; (2) pastikan calon mahasiswa telah dinyatakan lulus pada program studi tujuan; (3) ulangi proses setelah status kelulusan tersedia.", "Peringatan", MyMessageboxConfig.OK,
	                MyMessageboxConfig.EXCLAMATION);
	        return "";
	    }

	    boolean wajibBayarPersen = Common.bolehKonfigurasi("calon_mahasiswa_wajib_melakukan_pembayaran_daftar_ulang_mahasiswa_baru", Konfigurasi.TIDAK_AKTIF);

	    boolean wajibLunas = Common.bolehKonfigurasi("calon_mahasiswa_wajib_melakukan_pembayaran_lunas_daftar_ulang_mahasiswa_baru", Konfigurasi.TIDAK_AKTIF);

	    // Default jika tidak ada aturan wajib bayar, maka boleh generate
	    boolean shouldGenNim = (!wajibBayarPersen && !wajibLunas); 
	    Kegiatan kegiatan = calonMahasiswa.getPembayaranDaftarUlang();
	    // Tagihan Rp0 tidak memerlukan transaksi pembayaran dan tetap berhak memperoleh NIM.
	    if (isTagihanDaftarUlangNol(kegiatan)) {
	        shouldGenNim = true;
	    }
	    
	    // Evaluasi jika aturan wajib bayar/lunas aktif
	    if (!shouldGenNim) {
	        if (kegiatan == null) {
	            MyMessageboxConfig.show("Mohon maaf, Bapak/Ibu. Calon mahasiswa ini belum memiliki data pembayaran daftar ulang. Langkah yang dapat dilakukan: (1) periksa data pembayaran daftar ulang calon mahasiswa; (2) pastikan pembayaran daftar ulang telah tercatat pada sistem; (3) ulangi proses setelah data pembayaran tersedia.", "Peringatan",
	                    MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
	            return "";
	        }

	        if (wajibBayarPersen) {
	            Double minPersen = 10.0;
	            try {
	                minPersen = Double.parseDouble(Common.getKonfigurasi(
	                        "minimal_jumlah_persen_pembayaran_mahasiswa_otomatis_mendapatkan_nim", "0",
	                        calonMahasiswa.getProgram(), calonMahasiswa.getProdiLulus(),
	                        calonMahasiswa.getTahun().toString()).getNilai().trim());
	            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonPMB.java:1251");
	                // Gunakan default
	            }

	            Double persentaseLunas = kegiatan.getPersentaseLunas() != null ? kegiatan.getPersentaseLunas() : 0.0;
	            if (persentaseLunas >= minPersen) {
	                shouldGenNim = true;
	            } else {
	                MyMessageboxConfig.showFormat(
	                        "Mohon maaf, Bapak/Ibu. Calon mahasiswa ini belum memenuhi syarat persentase pembayaran daftar ulang (minimal {V1}%). Langkah yang dapat dilakukan: (1) periksa persentase pembayaran yang telah dilakukan; (2) lengkapi pembayaran hingga memenuhi persentase minimal; (3) ulangi proses setelah persyaratan terpenuhi.",
	                        "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, minPersen);
	                return "";
	            }
	        }

	        if (!shouldGenNim && wajibLunas) {
	            String kode = Common.getKonfigurasi(
	                    "kode_item_biaya_untuk_pembayaran_mahasiswa_baru_otomatis_dapat_nim", "",
	                    calonMahasiswa.getProgram(), calonMahasiswa.getProdiLulus(),
	                    calonMahasiswa.getTahun().toString()).getNilai().trim();

	            if (!kode.isEmpty()) {
	                Session session = null;
	                try {
	                    session = HibernateUtil.getSessionFactory().openSession();
	                    int countCicilan = ((Number) session.createCriteria(CicilanPembayaran.class)
	                            .createAlias("itemBiaya", "itemBiaya").add(Restrictions.eq("kegiatan", kegiatan))
	                            .add(Restrictions.ilike("itemBiaya.kode", kode, MatchMode.EXACT))
	                            .setProjection(Projections.rowCount()).uniqueResult()).intValue();

	                    if (countCicilan > 0) {
	                        shouldGenNim = true;
	                    } else {
	                         MyMessageboxConfig.showFormat(
	                                "Mohon maaf, Bapak/Ibu. Calon mahasiswa ini belum membayar item biaya yang disyaratkan ({V1}). Langkah yang dapat dilakukan: (1) periksa item biaya yang masih harus dibayar; (2) lakukan pembayaran atas item biaya tersebut; (3) ulangi proses setelah pembayaran diselesaikan.",
	                                "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, kode);
	                         return "";
	                    }
	                } catch (Exception e) {
	                    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonPMB.java:1290");
	                } finally {
	                    if (session != null) {
	                        session.close();
	                    }
	                }
	            } else {
	                Double minNominal = 10.0;
	                try {
	                    minNominal = Double.parseDouble(Common.getKonfigurasi(
	                            "minimal_jumlah_pembayaran_mahasiswa_otomatis_mendapatkan_nim", "0",
	                            calonMahasiswa.getProgram(), calonMahasiswa.getProdiLulus(),
	                            calonMahasiswa.getTahun().toString()).getNilai().trim());
	                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonPMB.java:1303");
	                    // Gunakan default
	                }

	                Double dibayar = kegiatan.getDibayar() != null ? kegiatan.getDibayar() : 0.0;
	                if (dibayar >= minNominal) {
	                    shouldGenNim = true;
	                } else {
	                     MyMessageboxConfig.showFormat(
	                            "Mohon maaf, Bapak/Ibu. Calon mahasiswa ini belum memenuhi syarat nominal pembayaran (minimal {V1}). Langkah yang dapat dilakukan: (1) periksa nominal pembayaran yang telah dilakukan; (2) lengkapi pembayaran hingga memenuhi nominal minimal; (3) ulangi proses setelah persyaratan terpenuhi.",
	                            "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, minNominal);
	                     return "";
	                }
	            }
	        }
	    }

	    // Jika sampai tahap ini, berarti memenuhi syarat untuk generate NIM atau sekadar cetak ulang
	    if (shouldGenNim) {
	        String nim = "";
	        
	        // Kasus 1: Mahasiswa sudah punya NIM, sekadar generate ulang dokumen (PDF)
	        if (calonMahasiswa.getNim() != null && !calonMahasiswa.getNim().trim().isEmpty()) {
	            try {
	                Map<String, Serializable> parameters = ais.common.HashMapGenerator.getRandStringSerializable();
	                parameters.put("nim", calonMahasiswa.getNim());
	                calonMahasiswa.putPhoto(parameters);
	                Report.generatePDFReport(Report.PDF, parameters, "Biodata_Nim", ais.ui.util.WaktuUtil.getDate());
	            } catch (Exception e) {
	                e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonPMB.java:1332");
	            }
	            return calonMahasiswa.getNim();
	        } 
	        
	        // Kasus 2: Mahasiswa belum punya NIM, proses generate NIM baru
	        else {
	            nim = ambilNimTersimpanDariRiwayatPmb(null, calonMahasiswa, calonMahasiswa.getMahasiswa());
	            if (isBlankString(nim)) {
	                nim = nimGenerator.generateNim(calonMahasiswa);
	            }
	            boolean izinkanNimDenganTandaHubung = konfirmasiNimMengandungTandaJikaPerlu(nim, calonMahasiswa);
	            if (!izinkanNimDenganTandaHubung) {
	            	return "";
	            }
	            String nimSebelum = calonMahasiswa.getNim();
	            Mahasiswa mahasiswaSebelum = calonMahasiswa.getMahasiswa();
	            Integer nimGeneratedSebelum = calonMahasiswa.getNimGenerated();
	            calonMahasiswa.setNim(nim);

	            Session session = null;
	            Transaction tx = null;
	            Mahasiswa mahasiswa = null;
	            try {
	                // Hindari currentSession() untuk mencegah tumpang tindih state hibernate jika dipanggil berulang.
	                session = HibernateUtil.getSessionFactory().openSession();
	                tx = session.beginTransaction();

	                mahasiswa = CommonPMB.saveMahasiswa(session, calonMahasiswa, nim, false,
	                		izinkanNimDenganTandaHubung);
	                tx.commit();
	                tx = null;
	            } catch (Exception e) {
	                if (tx != null && tx.isActive()) {
	                    try {
	                        tx.rollback();
	                    } catch (Exception rollbackError) {
	                        ais.common.ErrorAuditUtil.record(rollbackError,
	                                "CommonPMB.onGenerateNim rollback");
	                    }
	                }
	                calonMahasiswa.setNim(nimSebelum);
	                calonMahasiswa.setMahasiswa(mahasiswaSebelum);
	                calonMahasiswa.setNimGenerated(nimGeneratedSebelum);
	                ais.common.ErrorAuditUtil.record(e, "CommonPMB.onGenerateNim simpan NIM");
	                throw e;
	            } finally {
	                tutupSessionLokal(session);
	            }

	            // Salin lampiran hanya setelah mahasiswa sudah committed, agar session terpisah
	            // dapat melihat baris mahasiswa dan tidak menabrak foreign-key/transaksi yang belum selesai.
	            CommonPMB.copyLampiran(calonMahasiswa, mahasiswa);

	            try {
	                Map<String, Serializable> parameters = ais.common.HashMapGenerator.getRandStringSerializable();
	                parameters.put("nim", mahasiswa.getNim());
	                mahasiswa.putPhoto(parameters);
	                Report.generatePDFReport(Report.PDF, parameters, "Biodata_Nim", ais.ui.util.WaktuUtil.getDate());
	            } catch (Exception reportError) {
	                // NIM sudah sah tersimpan; kegagalan PDF tidak boleh membatalkan atau menyamarkan hasil tersebut.
	                ais.common.ErrorAuditUtil.record(reportError, "CommonPMB.onGenerateNim cetak PDF");
	            }
	            return nim;
	        }
	    }

	    return "";
	}

	private static Blob salinBlobDalamTransaksi(Blob sumber) throws Exception {
		if (sumber == null) {
			return null;
		}
		InputStream input = null;
		try {
			input = sumber.getBinaryStream();
			return org.hibernate.Hibernate.createBlob(org.apache.commons.io.IOUtils.toByteArray(input));
		} finally {
			if (input != null) {
				try { input.close(); } catch (Exception ignored) { }
			}
		}
	}

	/**
	 * Tagihan daftar ulang Rp0 dianggap telah memenuhi syarat pembayaran untuk
	 * proses generate NIM. Data kegiatan tetap harus tersedia agar kondisi
	 * "belum memiliki data pembayaran" tidak keliru dianggap sebagai tagihan nol.
	 */
	public static boolean isTagihanDaftarUlangNol(Kegiatan kegiatan) {
		return kegiatan != null && kegiatan.getTagihan() != null
				&& Math.abs(kegiatan.getTagihan().doubleValue()) < 0.01;
	}

	@SuppressWarnings("deprecation")
	public static void createDownloadUploadFileLampiran(Rows rows, final BiodataCalonMahasiswa biodataCalonMahasiswa,
			final String jenis, final String keterangan, String style) {
		Row row = new Row();
		row.setValign("top");
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setStyle(style);

		row.setParent(rows);
		row.setValign("top");
		row.setAttribute("jenis", true);

		Row parentPreview = new Row();
		ais.ui.util.ZkCompat.setSpans(parentPreview, "2");
		parentPreview.setStyle(style);

		parentPreview.setParent(rows);

		Hbox hbox = new Hbox();
		hbox.setAlign("center");
		hbox.setPack("center");
		hbox.setWidth("100%");
		hbox.setParent(parentPreview);

		createDownloadUploadFileLampiran(row, hbox, biodataCalonMahasiswa, jenis, keterangan);

		parentPreview = new Row();
		ais.ui.util.ZkCompat.setSpans(parentPreview, "2");
		parentPreview.setStyle(style);
		parentPreview.appendChild(new ais.ui.util.MyHtml("<hr>"));
	}

	public static void createDownloadUploadFileLampiran(Row row, Hbox parentPreview,
			final BiodataCalonMahasiswa biodataCalonMahasiswa, final String jenis, final String keterangan) {
		boolean harusPdf = false;
		Long myref = biodataCalonMahasiswa == null ? null : biodataCalonMahasiswa.getId();
		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				MyMessageboxConfig.showFormat("Berkas \"{V1}\" telah berhasil diunggah, Bapak/Ibu.", "Informasi",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, keterangan);
			}
		};
		Map<String, FileFotoLain> lampiranLains = null;
		boolean tidakTampilJurusan = false;
		boolean hanyaIcon = false;
		boolean usingId = false;
		boolean tampilUpload = true;

		Integer cutomUkuranUpload = null;
		boolean vertical = false;
		boolean janganPreviewDiLayarUtama = false;
		Hbox hbox = new Hbox();
		hbox.setParent(row);
		hbox.setAlign("center");
		hbox.setPack("center");
		hbox.setWidth("100%");
		FileFotoLain.createDownloadUpload(hbox, myref, jenis, keterangan, harusPdf, eventListener, lampiranLains,
				tidakTampilJurusan, hanyaIcon, usingId, tampilUpload, cutomUkuranUpload, vertical,
				janganPreviewDiLayarUtama, parentPreview, LampiranLainBiodataCalonMahasiswa.class);

	}

	public static MyToolbarbuttonConfig createDownloadBRIFormat(final GetFile getFile) {
		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Ambil CSV Format BRI", "/img/excel.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

				String Brivano = (String) HibernateUtil.currentSession().createCriteria(UploadVirtualAccount.class)
						.setProjection(Projections.property("kode")).setMaxResults(1).addOrder(Order.desc("id"))
						.uniqueResult();

				File file = getFile.getFile();

				if (file == null) {
					MyMessageboxConfig.show("Mohon maaf, Bapak/Ibu. Mohon menekan tombol \"Tampilkan Data\" terlebih dahulu sebelum melanjutkan proses ini. Langkah yang dapat dilakukan: (1) tekan tombol \"Tampilkan Data\"; (2) tunggu hingga data selesai dimuat; (3) ulangi proses yang dikehendaki.", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return;
				}

				// System.out.println("file = " + file.getAbsolutePath());

				XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
				XSSFSheet sheet = workbook.getSheetAt(0);
				String csv = "No|Brivano|CustCode|CustName|Type|OverwriteAddRemove|Amount|LastPeriode|Keteranga";
				for (int row = 1; row < (sheet.getLastRowNum() + 1); row++) {
					String colCellVal = Common.getCellContent(Common.getCell(sheet, 0, row));
					if (colCellVal != null && !colCellVal.isEmpty()) {
						String No = Common.getCellContent(Common.getCell(sheet, 0, row));
						String CustCode = Common.getCellContent(Common.getCell(sheet, 1, row));
						String CustName = Common.getCellContent(Common.getCell(sheet, 2, row));
						String Type = "K";
						String OverwriteAddRemove = "O";
						String Amount = Common.getCellContent(Common.getCell(sheet, 4, row));
						String LastPeriode = dateFormat.format(ais.ui.util.WaktuUtil.getDate());
						String Keteranga = Common.getCellContent(Common.getCell(sheet, 3, row));
						csv += "\n" + No + "|" + Brivano + "|" + CustCode + "|" + CustName + "|" + Type + "|"
								+ OverwriteAddRemove + "|" + Amount + "|" + LastPeriode + "|" + Keteranga;
					}
				}

				InputStream stream = new ByteArrayInputStream(csv.getBytes("UTF-8"));
				try {
					Filedownload.save(stream, "text/csv",
							"TAGIHAN_" + Common.dateFormat5.get().format(ais.ui.util.WaktuUtil.getDate()) + ".csv");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonPMB.java:1482");

				}
			}
		});
		return print;
	}

	private static void daftarkanKeRuangUjianOtomatis(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		if (biodataCalonMahasiswa == null || biodataCalonMahasiswa.getId() == null) {
			return;
		}
		try {
			boolean aktif = Common.bolehKonfigurasi("auto_daftarkan_ruang_ujian_pmb");
			if (!aktif) {
				return;
			}
			RuangPaketPMB ruang = dapatkanRuangUjian(biodataCalonMahasiswa);
			if (ruang != null) {
				System.out.println("INFO PMB: Calon " + biodataCalonMahasiswa.getId()
						+ " didaftarkan ke ruang ujian ID=" + ruang.getId());
			} else {
				System.out.println("INFO PMB: Tidak ada ruang ujian tersedia untuk calon "
						+ biodataCalonMahasiswa.getId() + " (ruang mungkin belum dikonfigurasi)");
			}
		} catch (Exception e) {
			System.err.println("Gagal mendaftarkan ke ruang ujian otomatis untuk calon "
					+ biodataCalonMahasiswa.getId() + ": " + e.getMessage());
		}
	}

	public static RuangPaketPMB dapatkanRuangUjian(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		// 1. Fail-fast: Cek null di awal untuk mencegah NullPointerException
		if (biodataCalonMahasiswa == null || biodataCalonMahasiswa.getId() == null) {
			System.err.println("PMB dapatkanRuangUjian: biodata null atau belum tersimpan.");
			return null;
		}
		if (biodataCalonMahasiswa.getGelombangPendaftaran() == null) {
			System.err.println("PMB dapatkanRuangUjian: gelombangPendaftaran null untuk calon ID="
					+ biodataCalonMahasiswa.getId());
			return null;
		}

		Session session = null;
		Transaction tx = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();

			// 2. Cek apakah calon sudah punya entri RuangPaketPMB — cegah duplikasi
			RuangPaketPMB ruangPaketPMB = (RuangPaketPMB) session.createCriteria(RuangPaketPMB.class)
					.add(Restrictions.eq("biodataCalonMahasiswa.id", biodataCalonMahasiswa.getId()))
					.setMaxResults(1).uniqueResult();

			if (ruangPaketPMB != null && ruangPaketPMB.getRuangPMB() != null) {
				// Sudah punya ruang — commit dan kembalikan agar caller tahu sudah ada
				tx.commit();
				tx = null;
				System.out.println("PMB dapatkanRuangUjian: calon ID=" + biodataCalonMahasiswa.getId()
						+ " sudah di ruang ID=" + ruangPaketPMB.getRuangPMB().getId());
				return ruangPaketPMB;
			}

			// 3. Cari Ruang PMB yang tersedia (belum penuh, sesuai gelombang & paket)
			RuangPMB ruangSelected = (RuangPMB) session.createCriteria(RuangPMB.class)
					.createAlias("ujianPMB", "ujianPMB")
					.add(Restrictions.or(Restrictions.isNull("paket"),
							Restrictions.eq("paket", biodataCalonMahasiswa.getPaket())))
					.add(Restrictions.eq("penuh", 0))
					.add(Restrictions.eq("ujianPMB.gelombangPendaftaran",
							biodataCalonMahasiswa.getGelombangPendaftaran()))
					.setMaxResults(1).addOrder(Order.asc("kodeRuangan")).addOrder(Order.asc("id")).uniqueResult();

			if (ruangSelected == null) {
				// Tidak ada ruang dikonfigurasi/tersedia — bukan error, hanya belum setup
				tx.commit();
				tx = null;
				return null;
			}

			// 4. Hitung peserta aktif di ruangan (yang sudah punya noUjian)
			Number t = (Number) session.createCriteria(RuangPaketPMB.class)
					.createAlias("biodataCalonMahasiswa", "bcm")
					.add(Restrictions.ne("bcm.noUjian", ""))
					.add(Restrictions.isNotNull("bcm.noUjian"))
					.add(Restrictions.eq("ruangPMB", ruangSelected))
					.setProjection(Projections.rowCount()).uniqueResult();

			int isiRuang = (t == null) ? 0 : t.intValue();

			// 5. Siapkan atau buat entri RuangPaketPMB
			if (ruangPaketPMB == null) {
				ruangPaketPMB = new RuangPaketPMB();
			}
			ruangPaketPMB.setBiodataCalonMahasiswa(
					(BiodataCalonMahasiswa) session.load(BiodataCalonMahasiswa.class,
							biodataCalonMahasiswa.getId()));
			ruangPaketPMB.setRuangPMB(ruangSelected);
			Common.refreshSaveOrUpdate(session, ruangPaketPMB);
			session.flush();

			// 6. Tandai ruang penuh jika kapasitas sudah tercapai setelah penambahan ini
			if (ruangSelected.getKapasitasRuangan() <= isiRuang + 1) {
				ruangSelected.setPenuh(1);
				Common.refreshUpdate(session, ruangSelected);
				session.flush();
			}

			// 7. Commit — data harus tersimpan agar sesi lain bisa membacanya
			tx.commit();
			tx = null;

			System.out.println("PMB dapatkanRuangUjian: calon ID=" + biodataCalonMahasiswa.getId()
					+ " → ruangPaketPMB ID=" + ruangPaketPMB.getId()
					+ " (ruang=" + ruangSelected.getKodeRuangan() + ", isi=" + (isiRuang + 1)
					+ "/" + ruangSelected.getKapasitasRuangan() + ")");
			return ruangPaketPMB;

		} catch (Exception e) {
			System.err.println("PMB dapatkanRuangUjian error untuk calon ID="
					+ biodataCalonMahasiswa.getId() + ": " + e.getMessage());
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonPMB.java:1603");
			if (tx != null) {
				try { tx.rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/CommonPMB.java:1605"); /* ignore */ }
				tx = null;
			}
			return null;
		} finally {
			if (session != null && session.isOpen()) {
				try { session.clear(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/CommonPMB.java:1611"); /* ignore */ }
				try { session.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/CommonPMB.java:1612"); /* ignore */ }
			}
		}
	}
}
