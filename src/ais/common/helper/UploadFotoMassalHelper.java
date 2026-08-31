package ais.common.helper;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.Blob;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.image.AImage;
import org.zkoss.util.media.Media;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Dosen;
import ais.database.model.Mahasiswa;
import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;
import ais.database.model.file.FotoAdmin;
import ais.database.model.file.FotoBiodataCalonMahasiswa;
import ais.database.model.file.FotoCalonSiswa;
import ais.database.model.file.FotoDosen;
import ais.database.model.file.FotoGuru;
import ais.database.model.file.FotoMahasiswa;
import ais.database.model.file.FotoPegawai;
import ais.database.model.file.FotoSiswa;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.Siswa;

/**
 * <h1>UploadFotoMassalHelper — unggah FOTO MASSAL berdasarkan nama berkas = NIM/No. Induk</h1>
 *
 * <p><b>Untuk apa.</b> Admin memilih banyak berkas foto sekaligus (mis. isi satu folder) yang
 * <b>nama berkasnya adalah NIM/No. Induk</b> (mis. {@code 12345.jpg}, {@code 45666.png},
 * {@code 108036101300.jpeg}). Setiap foto langsung dipasang sebagai foto profil mahasiswa/siswa
 * yang NIM/No. Induk-nya cocok. Memudahkan pemasangan foto ratusan mahasiswa/siswa dalam sekali
 * unggah tanpa membuka profil satu per satu.</p>
 *
 * <p><b>Cara kerja tiap berkas.</b>
 * <ol>
 *   <li>Ambil nama berkas, buang ekstensi ({@code 12345.jpg} → {@code 12345}) sebagai kunci NIM.</li>
 *   <li>Cari mahasiswa (kolom {@code nim}) / siswa (kolom {@code nomorInduk}, lalu {@code nim}).</li>
 *   <li>Bila ketemu &amp; berkas berupa gambar: hapus foto lama (bila ada) lalu simpan foto baru
 *       sebagai {@link FotoMahasiswa}/{@link FotoSiswa} (BLOB via {@link Common#getBlobFromMedia}).</li>
 * </ol>
 * Semua memakai {@code StreamingHibernateUtil} (sesi streaming untuk BLOB, ditutup di akhir) dan
 * dibungkus try/catch sehingga satu berkas gagal tidak menggagalkan berkas lain.</p>
 *
 * <p><b>Hasil.</b> Mengembalikan {@code int[]{berhasil, tidakDitemukan, gagal}} untuk ditampilkan
 * sebagai ringkasan ke admin.</p>
 */
public final class UploadFotoMassalHelper {

	private UploadFotoMassalHelper() {
	}

	// ------------------------------------------------------------- pencatatan laporan

	/** Nama berkas apa adanya (termasuk folder di dalam ZIP) untuk ditampilkan di laporan. */
	private static String namaUntukLaporan(Media media) {
		String n = media == null ? null : media.getName();
		return n == null || n.trim().isEmpty() ? "(tanpa nama)" : n.trim();
	}

	/** Mencatat satu berkas ke laporan; aman bila {@code laporan} null. */
	private static void catat(ais.common.LaporanUpload laporan, int nomor, Media media, String status,
			String keterangan) {
		if (laporan == null) {
			return;
		}
		if (ais.common.LaporanUpload.BERHASIL.equals(status)) {
			laporan.catatBerhasil(nomor, namaUntukLaporan(media), keterangan);
		} else if (ais.common.LaporanUpload.DILEWATI.equals(status)) {
			laporan.catatDilewati(nomor, namaUntukLaporan(media), keterangan);
		} else {
			laporan.catatGagal(nomor, namaUntukLaporan(media), keterangan);
		}
	}

	private static void catatGagal(ais.common.LaporanUpload laporan, int nomor, Media media, Throwable t) {
		if (laporan != null) {
			laporan.catatGagal(nomor, namaUntukLaporan(media), t);
		}
	}

	/**
	 * Menjelaskan SEBAB sebuah berkas ditolak sebelum sempat dicocokkan ke data. Penjelasan dibuat
	 * spesifik supaya pengguna tahu tindakan perbaikannya (mis. mengganti nama berkas menjadi NIM,
	 * atau membuang berkas non-gambar dari dalam ZIP).
	 */
	private static String sebabBukanGambar(Media media, String nim) {
		if (media == null) {
			return "Berkas tidak dapat dibaca";
		}
		if (nim == null || nim.isEmpty()) {
			return "Nama berkas kosong; nama berkas HARUS berupa NIM, mis. 26454201001.jpg";
		}
		if (!media.isBinary()) {
			return "Berkas bukan biner (kemungkinan berkas teks), bukan berkas gambar";
		}
		if (!isGambar(media)) {
			return "Bukan berkas gambar (ekstensi/tipe " + (media.getContentType() == null ? "tidak dikenal"
					: media.getContentType()) + "); yang diterima: jpg, jpeg, png, gif, bmp";
		}
		return "Berkas tidak memenuhi syarat";
	}

	/** Ambil NIM dari nama berkas (buang path &amp; ekstensi, mis. {@code 12345.jpg} → {@code 12345}). */
	public static String nimDariNamaBerkas(String namaBerkas) {
		if (namaBerkas == null) {
			return null;
		}
		String nama = namaBerkas.replace('\\', '/');
		int slash = nama.lastIndexOf('/');
		if (slash >= 0) {
			nama = nama.substring(slash + 1);
		}
		int dot = nama.lastIndexOf('.');
		if (dot > 0) {
			nama = nama.substring(0, dot);
		}
		return nama.trim();
	}

	/**
	 * true bila pengguna aktif memiliki hak akses <b>READ, UPDATE (ubah), dan DELETE (hapus)</b> sekaligus
	 * pada menu yang sedang dibuka. Dipakai membatasi fungsi Upload Foto Massal: hanya boleh dijalankan bila
	 * KETIGA hak akses tersebut aktif (sesuai permintaan).
	 */
	public static boolean bolehUploadMassal() {
		try {
			return ais.common.CommonPrivilages.checkPrevilages(ais.common.CommonPrivilages.READ)
					&& ais.common.CommonPrivilages.checkPrevilages(ais.common.CommonPrivilages.UPDATE)
					&& ais.common.CommonPrivilages.checkPrevilages(ais.common.CommonPrivilages.DELETE);
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Menyembunyikan tombol "Upload Foto (NIM)" (komponen ber-{@code id=uploadFotoMassal}) ketika hak akses
	 * tak lengkap ({@link #bolehUploadMassal()} == false). {@code acuan} = komponen mana pun di halaman yang
	 * sama (mis. {@code self} composer) yang dipakai menemukan tombol via {@code getFellowIfAny}.
	 */
	public static void terapkanGateTombolUpload(org.zkoss.zk.ui.Component acuan) {
		if (acuan == null) {
			return;
		}
		try {
			org.zkoss.zk.ui.Component tombol = acuan.getFellowIfAny("uploadFotoMassal");
			if (tombol != null) {
				tombol.setVisible(bolehUploadMassal());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/helper/UploadFotoMassalHelper.java:108");
			// tombol tak ditemukan / konteks lain — abaikan diam-diam.
		}
	}

	/**
	 * Unggah foto massal MAHASISWA. Untuk tiap {@code media}: NIM = nama berkas tanpa ekstensi,
	 * dicocokkan ke {@code Mahasiswa.nim}, lalu foto disimpan sebagai {@link FotoMahasiswa}.
	 *
	 * @param medias daftar berkas foto (boleh berisi non-gambar; akan dilewati)
	 * @return {@code int[]{berhasil, tidakDitemukan, gagal}}
	 */
	public static int[] uploadFotoMahasiswaByNim(List<Media> mediasAsli) {
		return uploadFotoMahasiswaByNim(mediasAsli, null);
	}

	/**
	 * Varian yang MENCATAT hasil tiap berkas ke {@code laporan} (boleh null): nama berkas, status,
	 * dan sebab kegagalannya. Dipakai supaya pengguna tahu foto mana yang masuk dan mana yang tidak,
	 * lengkap dengan alasannya, bukan sekadar angka ringkasan.
	 */
	public static int[] uploadFotoMahasiswaByNim(List<Media> mediasAsli, ais.common.LaporanUpload laporan) {
		return uploadFotoMahasiswaByNim(mediasAsli, laporan, null);
	}

	/** Headless New UI: NIM di luar scope PT aktif selalu dilewati. */
	public static int[] uploadFotoMahasiswaByNim(List<Media> mediasAsli, ais.common.LaporanUpload laporan,
			java.util.Set<String> allowedNims) {
		int berhasil = 0, tidakDitemukan = 0, gagal = 0;
		int nomor = -1;
		if (mediasAsli == null || mediasAsli.isEmpty()) {
			return new int[] { 0, 0, 0 };
		}
		// Bila salah satu berkas adalah ZIP, buka & jadikan tiap entri gambar sebagai Media tersendiri
		// (mendukung "upload folder": admin cukup zip folder foto lalu unggah 1 berkas .zip).
		List<Media> medias = ekspandZip(mediasAsli);
		for (Media media : medias) {
			if (media == null) {
				continue;
			}
			nomor++;
			String nim = nimDariNamaBerkas(media.getName());
			if (nim == null || nim.isEmpty() || !media.isBinary() || !isGambar(media)) {
				gagal++;
				catat(laporan, nomor, media, ais.common.LaporanUpload.GAGAL, sebabBukanGambar(media, nim));
				continue;
			}
			if (allowedNims != null && !allowedNims.contains(nim)) {
				tidakDitemukan++;
				catat(laporan, nomor, media, ais.common.LaporanUpload.DILEWATI,
						"NIM tidak ditemukan pada scope perguruan tinggi aktif");
				continue;
			}
			Session streamingSession = null;
			try {
				// Mahasiswa WAJIB dicari di sesi UTAMA (HibernateUtil), bukan streaming.
				// Sesi streaming (StreamingHibernateUtil) terhubung ke DB file/BLOB terpisah
				// (hibernate.streaming.cfg.xml) yang TIDAK memetakan entitas Mahasiswa —
				// query Mahasiswa ke sana selalu mengembalikan null → semua baris "DILEWATI".
				Mahasiswa mhs = (Mahasiswa) HibernateUtil.currentSession()
						.createCriteria(Mahasiswa.class)
						.add(Restrictions.eq("nim", nim)).setMaxResults(1).uniqueResult();
				if (mhs == null || mhs.getId() == null) {
					tidakDitemukan++;
					catat(laporan, nomor, media, ais.common.LaporanUpload.DILEWATI,
							"NIM \"" + nim + "\" tidak ditemukan pada data mahasiswa");
					continue;
				}
				// Buka sesi streaming HANYA setelah Mahasiswa ditemukan (untuk operasi BLOB)
				streamingSession = StreamingHibernateUtil.getInstance().currentSession();

				FotoMahasiswa lama = (FotoMahasiswa) streamingSession.createCriteria(FotoMahasiswa.class)
						.add(Restrictions.eq("mahasiswa", mhs.getId())).setMaxResults(1).uniqueResult();
				if (lama != null) {
					streamingSession.getTransaction().begin();
					streamingSession.delete(lama);
					streamingSession.getTransaction().commit();
				}

				FotoMahasiswa baru = new FotoMahasiswa();
				baru.setNama(media.getName());
				baru.setKeterangan(media.getContentType());
				baru.setMahasiswa(mhs.getId());
				Blob blob = Common.getBlobFromMedia(media, streamingSession);
				baru.setFoto(blob);

				streamingSession.getTransaction().begin();
				streamingSession.save(baru);
				streamingSession.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
				berhasil++;
				catat(laporan, nomor, media, ais.common.LaporanUpload.BERHASIL,
						"NIM " + nim + " - " + mhs.getNama());
			} catch (Exception e) {
				try {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/helper/UploadFotoMassalHelper.java:172");
				}
				try {
					StreamingHibernateUtil.getInstance().closeSession();
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/helper/UploadFotoMassalHelper.java:176");
				}
				Common.tampilErrorJikaAdmin(e);
				gagal++;
				catatGagal(laporan, nomor, media, e);
			}
		}
		return new int[] { berhasil, tidakDitemukan, gagal };
	}

	/**
	 * Unggah foto massal SISWA. NIM = nama berkas tanpa ekstensi, dicocokkan ke
	 * {@code Siswa.nomorInduk} lalu {@code Siswa.nim}, foto disimpan sebagai {@link FotoSiswa}.
	 *
	 * @param medias daftar berkas foto
	 * @return {@code int[]{berhasil, tidakDitemukan, gagal}}
	 */
	public static int[] uploadFotoSiswaByNim(List<Media> mediasAsli) {
		int berhasil = 0, tidakDitemukan = 0, gagal = 0;
		if (mediasAsli == null || mediasAsli.isEmpty()) {
			return new int[] { 0, 0, 0 };
		}
		// Bila salah satu berkas adalah ZIP, buka & jadikan tiap entri gambar sebagai Media tersendiri.
		List<Media> medias = ekspandZip(mediasAsli);
		for (Media media : medias) {
			if (media == null) {
				continue;
			}
			String nim = nimDariNamaBerkas(media.getName());
			if (nim == null || nim.isEmpty() || !media.isBinary() || !isGambar(media)) {
				gagal++;
				continue;
			}
			Session streamingSession = null;
			try {
				// Siswa WAJIB dicari di sesi UTAMA — streaming DB tidak memetakan entitas Siswa.
				Siswa siswa = (Siswa) HibernateUtil.currentSession()
						.createCriteria(Siswa.class)
						.add(Restrictions.eq("nomorInduk", nim)).setMaxResults(1).uniqueResult();
				if (siswa == null || siswa.getId() == null) {
					siswa = (Siswa) HibernateUtil.currentSession()
							.createCriteria(Siswa.class)
							.add(Restrictions.eq("nim", nim)).setMaxResults(1).uniqueResult();
				}
				if (siswa == null || siswa.getId() == null) {
					tidakDitemukan++;
					continue;
				}
				// Buka sesi streaming HANYA setelah Siswa ditemukan (untuk operasi BLOB)
				streamingSession = StreamingHibernateUtil.getInstance().currentSession();

				FotoSiswa lama = (FotoSiswa) streamingSession.createCriteria(FotoSiswa.class)
						.add(Restrictions.eq("siswa", siswa.getId())).setMaxResults(1).uniqueResult();
				if (lama != null) {
					streamingSession.getTransaction().begin();
					streamingSession.delete(lama);
					streamingSession.getTransaction().commit();
				}

				FotoSiswa baru = new FotoSiswa();
				baru.setNama(media.getName());
				baru.setKeterangan(media.getContentType());
				baru.setSiswa(siswa.getId());
				Blob blob = Common.getBlobFromMedia(media, streamingSession);
				baru.setFoto(blob);

				streamingSession.getTransaction().begin();
				streamingSession.save(baru);
				streamingSession.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
				berhasil++;
			} catch (Exception e) {
				try {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/helper/UploadFotoMassalHelper.java:247");
				}
				try {
					StreamingHibernateUtil.getInstance().closeSession();
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/helper/UploadFotoMassalHelper.java:251");
				}
				Common.tampilErrorJikaAdmin(e);
				gagal++;
			}
		}
		return new int[] { berhasil, tidakDitemukan, gagal };
	}

	// ────────────────────────────────────────────────────────────────────────────────────────
	// Ekstensi ke entitas lain: Dosen, Pegawai, Guru, CalonSiswa, BiodataCalonMahasiswa, Tbmuser.
	// Loop unggah + sesi/BLOB/transaksi ditulis SEKALI (uploadByNim) via kontrak ResolverFoto,
	// sehingga tiap jenis pemilik cukup menyediakan: cari id pemilik dari NIM, cari foto lama,
	// dan bangun entitas foto baru. Mendukung id pemilik Long (mahasiswa dsb.) maupun String (Tbmuser).
	// ────────────────────────────────────────────────────────────────────────────────────────

	/**
	 * Kontrak callback/strategi bersarang milik {@link UploadFotoMassalHelper}. Tipe ini memisahkan satu variasi
	 * perilaku lokal tanpa membuat service atau interface global yang tumpang tindih.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link UploadFotoMassalHelper} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p> Tipe ini merupakan detail
	 * implementasi privat; pemanggil luar harus memakai API kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code cariOwnerId()}, {@code
	 * cariFotoLama()}, {@code buatFoto}(). Aturan bisnis bersama tetap berada pada kelas induk atau service yang
	 * dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
	 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
	 * tambahkan perilaku lintas domain pada service bersama.</p>
	 *
	 * @see UploadFotoMassalHelper
	 */
	private interface ResolverFoto {
		/**
		 * id pemilik (Long/String) yang cocok dgn NIM, atau null bila tak ditemukan.
		 * WAJIB pakai {@link HibernateUtil#currentSession()} di dalam implementasi —
		 * sesi streaming (BLOB) TIDAK memetakan entitas seperti Dosen/Pegawai/Guru/dsb.
		 */
		Object cariOwnerId(String nim) throws Exception;

		/** entitas foto LAMA milik pemilik ini (untuk dihapus), atau null. */
		Object cariFotoLama(Session session, Object ownerId) throws Exception;

		/** bangun entitas foto BARU (belum disimpan) dari media + id pemilik + blob. */
		Object buatFoto(String nama, String keterangan, Object ownerId, Blob blob) throws Exception;
	}

	/**
	 * Loop unggah foto massal generik: expand ZIP, lalu tiap gambar → NIM (nama berkas) → pemilik →
	 * hapus foto lama → simpan foto baru (BLOB). Menangani sesi streaming + transaksi + error per
	 * berkas (satu gagal tak menggagalkan lain). Dipakai semua method {@code uploadFoto<Entitas>ByNim}.
	 */
	private static int[] uploadByNim(List<Media> mediasAsli, ResolverFoto resolver) {
		int berhasil = 0, tidakDitemukan = 0, gagal = 0;
		if (mediasAsli == null || mediasAsli.isEmpty()) {
			return new int[] { 0, 0, 0 };
		}
		List<Media> medias = ekspandZip(mediasAsli);
		for (Media media : medias) {
			if (media == null) {
				continue;
			}
			String nim = nimDariNamaBerkas(media.getName());
			if (nim == null || nim.isEmpty() || !media.isBinary() || !isGambar(media)) {
				gagal++;
				continue;
			}
			try {
				// Cari owner di DB UTAMA dulu; streaming session dibuka SETELAH ditemukan.
				Object ownerId = resolver.cariOwnerId(nim);
				if (ownerId == null) {
					tidakDitemukan++;
					continue;
				}
				Session session = StreamingHibernateUtil.getInstance().currentSession();
				Object lama = resolver.cariFotoLama(session, ownerId);
				if (lama != null) {
					session.getTransaction().begin();
					session.delete(lama);
					session.getTransaction().commit();
				}
				Blob blob = Common.getBlobFromMedia(media, session);
				Object baru = resolver.buatFoto(media.getName(), media.getContentType(), ownerId, blob);
				session.getTransaction().begin();
				session.save(baru);
				session.getTransaction().commit();
				StreamingHibernateUtil.getInstance().closeSession();
				berhasil++;
			} catch (Exception e) {
				try {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/helper/UploadFotoMassalHelper.java:322");
				}
				try {
					StreamingHibernateUtil.getInstance().closeSession();
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/helper/UploadFotoMassalHelper.java:326");
				}
				Common.tampilErrorJikaAdmin(e);
				gagal++;
			}
		}
		return new int[] { berhasil, tidakDitemukan, gagal };
	}

	/** Unggah foto massal DOSEN — nama berkas = NIDN (fallback: code). Simpan {@link FotoDosen}. */
	public static int[] uploadFotoDosenByNim(List<Media> medias) {
		return uploadByNim(medias, new ResolverFoto() {
			@Override
			public Object cariOwnerId(String nim) throws Exception {
				Dosen d = (Dosen) HibernateUtil.currentSession().createCriteria(Dosen.class)
						.add(Restrictions.eq("nidn", nim)).setMaxResults(1).uniqueResult();
				if (d == null || d.getId() == null) {
					d = (Dosen) HibernateUtil.currentSession().createCriteria(Dosen.class)
							.add(Restrictions.eq("code", nim)).setMaxResults(1).uniqueResult();
				}
				return d == null ? null : d.getId();
			}

			@Override
			public Object cariFotoLama(Session session, Object ownerId) throws Exception {
				return session.createCriteria(FotoDosen.class).add(Restrictions.eq("dosen", (Long) ownerId))
						.setMaxResults(1).uniqueResult();
			}

			@Override
			public Object buatFoto(String nama, String keterangan, Object ownerId, Blob blob) throws Exception {
				FotoDosen f = new FotoDosen();
				f.setNama(nama);
				f.setKeterangan(keterangan);
				f.setDosen((Long) ownerId);
				f.setFoto(blob);
				return f;
			}
		});
	}

	/** Unggah foto massal PEGAWAI — nama berkas = code (NIP/kode pegawai). Simpan {@link FotoPegawai}. */
	public static int[] uploadFotoPegawaiByNim(List<Media> medias) {
		return uploadByNim(medias, new ResolverFoto() {
			@Override
			public Object cariOwnerId(String nim) throws Exception {
				Pegawai p = (Pegawai) HibernateUtil.currentSession().createCriteria(Pegawai.class)
						.add(Restrictions.eq("code", nim)).setMaxResults(1).uniqueResult();
				return p == null ? null : p.getId();
			}

			@Override
			public Object cariFotoLama(Session session, Object ownerId) throws Exception {
				return session.createCriteria(FotoPegawai.class).add(Restrictions.eq("pegawai", (Long) ownerId))
						.setMaxResults(1).uniqueResult();
			}

			@Override
			public Object buatFoto(String nama, String keterangan, Object ownerId, Blob blob) throws Exception {
				FotoPegawai f = new FotoPegawai();
				f.setNama(nama);
				f.setKeterangan(keterangan);
				f.setPegawai((Long) ownerId);
				f.setFoto(blob);
				return f;
			}
		});
	}

	/** Unggah foto massal GURU — nama berkas = NIP (fallback: NUPTK). Simpan {@link FotoGuru}. */
	public static int[] uploadFotoGuruByNim(List<Media> medias) {
		return uploadByNim(medias, new ResolverFoto() {
			@Override
			public Object cariOwnerId(String nim) throws Exception {
				Guru g = (Guru) HibernateUtil.currentSession().createCriteria(Guru.class)
						.add(Restrictions.eq("nip", nim)).setMaxResults(1).uniqueResult();
				if (g == null || g.getId() == null) {
					g = (Guru) HibernateUtil.currentSession().createCriteria(Guru.class)
							.add(Restrictions.eq("nuptk", nim)).setMaxResults(1).uniqueResult();
				}
				return g == null ? null : g.getId();
			}

			@Override
			public Object cariFotoLama(Session session, Object ownerId) throws Exception {
				return session.createCriteria(FotoGuru.class).add(Restrictions.eq("guru", (Long) ownerId))
						.setMaxResults(1).uniqueResult();
			}

			@Override
			public Object buatFoto(String nama, String keterangan, Object ownerId, Blob blob) throws Exception {
				FotoGuru f = new FotoGuru();
				f.setNama(nama);
				f.setKeterangan(keterangan);
				f.setGuru((Long) ownerId);
				f.setFoto(blob);
				return f;
			}
		});
	}

	/** Unggah foto massal CALON SISWA — nama berkas = No. Registrasi/Induk (fallback: NIS). Simpan {@link FotoCalonSiswa}. */
	public static int[] uploadFotoCalonSiswaByNim(List<Media> medias) {
		return uploadByNim(medias, new ResolverFoto() {
			@Override
			public Object cariOwnerId(String nim) throws Exception {
				CalonSiswa c = (CalonSiswa) HibernateUtil.currentSession().createCriteria(CalonSiswa.class)
						.add(Restrictions.eq("noRegistrasi", nim)).setMaxResults(1).uniqueResult();
				if (c == null || c.getId() == null) {
					c = (CalonSiswa) HibernateUtil.currentSession().createCriteria(CalonSiswa.class)
							.add(Restrictions.eq("nis", nim)).setMaxResults(1).uniqueResult();
				}
				return c == null ? null : c.getId();
			}

			@Override
			public Object cariFotoLama(Session session, Object ownerId) throws Exception {
				return session.createCriteria(FotoCalonSiswa.class).add(Restrictions.eq("calonSiswa", (Long) ownerId))
						.setMaxResults(1).uniqueResult();
			}

			@Override
			public Object buatFoto(String nama, String keterangan, Object ownerId, Blob blob) throws Exception {
				FotoCalonSiswa f = new FotoCalonSiswa();
				f.setNama(nama);
				f.setKeterangan(keterangan);
				f.setCalonSiswa((Long) ownerId);
				f.setFoto(blob);
				return f;
			}
		});
	}

	/** Unggah foto massal BIODATA CALON MAHASISWA — nama berkas = No. Registrasi. Simpan {@link FotoBiodataCalonMahasiswa}. */
	public static int[] uploadFotoBiodataCalonMahasiswaByNim(List<Media> medias) {
		return uploadByNim(medias, new ResolverFoto() {
			@Override
			public Object cariOwnerId(String nim) throws Exception {
				BiodataCalonMahasiswa b = (BiodataCalonMahasiswa) HibernateUtil.currentSession()
						.createCriteria(BiodataCalonMahasiswa.class)
						.add(Restrictions.eq("noRegistrasi", nim)).setMaxResults(1).uniqueResult();
				return b == null ? null : b.getId();
			}

			@Override
			public Object cariFotoLama(Session session, Object ownerId) throws Exception {
				return session.createCriteria(FotoBiodataCalonMahasiswa.class)
						.add(Restrictions.eq("biodataCalonMahasiswa", (Long) ownerId)).setMaxResults(1).uniqueResult();
			}

			@Override
			public Object buatFoto(String nama, String keterangan, Object ownerId, Blob blob) throws Exception {
				FotoBiodataCalonMahasiswa f = new FotoBiodataCalonMahasiswa();
				f.setNama(nama);
				f.setKeterangan(keterangan);
				f.setBiodataCalonMahasiswa((Long) ownerId);
				f.setFoto(blob);
				return f;
			}
		});
	}

	/**
	 * Unggah foto massal PENGGUNA (Tbmuser) — nama berkas = User ID. Simpan {@link FotoAdmin}.
	 * <b>Catatan:</b> pemilik foto admin memakai kunci STRING ({@code Tbmuser.getUserId()}), bukan
	 * id numerik seperti entitas lain — {@link FotoAdmin#setTbmuser(String)} menerima String.
	 */
	public static int[] uploadFotoTbmuserByNim(List<Media> medias) {
		return uploadByNim(medias, new ResolverFoto() {
			@Override
			public Object cariOwnerId(String nim) throws Exception {
				Tbmuser t = (Tbmuser) HibernateUtil.currentSession().createCriteria(Tbmuser.class)
						.add(Restrictions.eq("userId", nim)).setMaxResults(1).uniqueResult();
				return t == null ? null : t.getUserId();
			}

			@Override
			public Object cariFotoLama(Session session, Object ownerId) throws Exception {
				return session.createCriteria(FotoAdmin.class).add(Restrictions.eq("tbmuser", (String) ownerId))
						.setMaxResults(1).uniqueResult();
			}

			@Override
			public Object buatFoto(String nama, String keterangan, Object ownerId, Blob blob) throws Exception {
				FotoAdmin f = new FotoAdmin();
				f.setNama(nama);
				f.setKeterangan(keterangan);
				f.setTbmuser((String) ownerId);
				f.setFoto(blob);
				return f;
			}
		});
	}

	/** Ringkasan hasil untuk ditampilkan ke admin. */
	public static String ringkasan(int[] hasil) {
		if (hasil == null || hasil.length < 3) {
			return "Tidak ada berkas diproses.";
		}
		return "Unggah foto massal selesai.\n\n" + "Berhasil dipasang : " + hasil[0] + " foto\n"
				+ "NIM tidak ditemukan : " + hasil[1] + " berkas\n" + "Gagal/bukan gambar : " + hasil[2] + " berkas";
	}

	/** true bila media kemungkinan besar gambar (content-type image/* atau ekstensi umum). */
	private static boolean isGambar(Media media) {
		try {
			String ct = media.getContentType();
			if (ct != null && ct.toLowerCase().startsWith("image")) {
				return true;
			}
			return isNamaGambar(media.getName());
		} catch (Exception e) {
			return false;
		}
	}

	/** true bila NAMA berkas berekstensi gambar umum (jpg/jpeg/png/gif/bmp/tif/tiff/webp). */
	private static boolean isNamaGambar(String nama) {
		if (nama == null) {
			return false;
		}
		String n = nama.toLowerCase();
		return n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png") || n.endsWith(".gif")
				|| n.endsWith(".bmp") || n.endsWith(".tif") || n.endsWith(".tiff") || n.endsWith(".webp");
	}

	/** true bila media adalah berkas ZIP (berdasar content-type atau ekstensi .zip). */
	private static boolean isZip(Media media) {
		try {
			String ct = media.getContentType();
			if (ct != null) {
				String c = ct.toLowerCase();
				if (c.contains("zip") || c.contains("compressed")) {
					return true;
				}
			}
			String nama = media.getName() == null ? "" : media.getName().toLowerCase();
			return nama.endsWith(".zip");
		} catch (Exception e) {
			return false;
		}
	}

	/** Kedalaman rekursi maksimum saat membuka ZIP di dalam ZIP (cegah zip-bomb/rekursi tak henti). */
	private static final int MAKS_KEDALAMAN_ZIP = 8;

	/**
	 * Membuka setiap berkas ZIP menjadi Media gambar per-entri; berkas non-ZIP diteruskan apa adanya.
	 * Mendukung "unggah folder" secara praktis: admin cukup meng-ZIP folder foto (nama berkas =
	 * NIM/No.Induk) lalu mengunggah SATU berkas {@code .zip}. <b>REKURSIF</b>: bila di dalam ZIP masih
	 * ada SUB-FOLDER (berapa pun dalamnya) MAUPUN <b>ZIP di dalam ZIP</b> (mis. sub-folder yang di-ZIP
	 * lagi), semuanya ditelusuri sampai kedalaman {@value #MAKS_KEDALAMAN_ZIP} sehingga SEMUA berkas foto
	 * yang bernama NIM ikut terbaca. Entri direktori &amp; non-gambar dilewati; nama entri (mis.
	 * {@code foto/kelas-a/12345.jpg}) tetap dibawa agar {@link #nimDariNamaBerkas} bisa mengambil NIM.
	 * Aman: kegagalan membuka satu ZIP/entri tidak menggagalkan berkas lain.
	 *
	 * @param medias daftar berkas terunggah (campuran gambar &amp; zip)
	 * @return daftar Media gambar (entri ZIP jadi {@code AImage}, berkas gambar biasa diteruskan)
	 */
	public static List<Media> ekspandZip(List<Media> medias) {
		List<Media> hasil = new ArrayList<Media>();
		if (medias == null) {
			return hasil;
		}
		for (Media m : medias) {
			if (m == null) {
				continue;
			}
			if (!isZip(m)) {
				hasil.add(m);
				continue;
			}
			try {
				InputStream in = m.isBinary() ? m.getStreamData()
						: new java.io.ByteArrayInputStream(m.getStringData().getBytes("UTF-8"));
				ekspandSatuZip(in, 0, hasil);
			} catch (Exception eZip) {
				Common.tampilErrorJikaAdmin(eZip);
			}
		}
		return hasil;
	}

	/**
	 * Menelusuri SATU aliran ZIP secara REKURSIF (lihat {@link #ekspandZip}). Karena {@code ZipInputStream}
	 * mengembalikan tiap entri dengan path lengkapnya (mis. {@code a/b/12345.jpg}), sub-folder biasa otomatis
	 * ikut terbaca; TAMBAHAN di sini: bila entri itu sendiri berkas {@code .zip} (sub-folder yang di-ZIP lagi)
	 * maka aliran-nya DIBUKA ULANG (rekursi, {@code kedalaman+1}). Entri gambar → {@link AImage}; entri
	 * direktori &amp; lainnya dilewati. Berhenti bila {@code kedalaman} melebihi {@value #MAKS_KEDALAMAN_ZIP}.
	 */
	private static void ekspandSatuZip(InputStream in, int kedalaman, List<Media> hasil) {
		if (in == null || kedalaman > MAKS_KEDALAMAN_ZIP) {
			return;
		}
		ZipInputStream zis = null;
		try {
			zis = new ZipInputStream(in);
			ZipEntry entry;
			byte[] buf = new byte[8192];
			while ((entry = zis.getNextEntry()) != null) {
				try {
					if (entry.isDirectory()) {
						continue;
					}
					String namaEntri = entry.getName();
					boolean gambar = isNamaGambar(namaEntri);
					boolean zipBersarang = !gambar && namaEntri != null
							&& namaEntri.toLowerCase().endsWith(".zip");
					if (!gambar && !zipBersarang) {
						continue;
					}
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					int n;
					while ((n = zis.read(buf)) != -1) {
						baos.write(buf, 0, n);
					}
					byte[] bytes = baos.toByteArray();
					if (bytes.length == 0) {
						continue;
					}
					if (zipBersarang) {
						// Sub-folder yang di-ZIP lagi → buka rekursif agar semua foto di dalamnya ikut terbaca.
						ekspandSatuZip(new java.io.ByteArrayInputStream(bytes), kedalaman + 1, hasil);
					} else {
						hasil.add(new AImage(namaEntri, bytes));
					}
				} catch (Exception eEntri) { ais.common.ErrorAuditUtil.record(eEntri, "auto-audit(empty-catch) src/ais/common/helper/UploadFotoMassalHelper.java:651");
					// entri rusak / bukan gambar valid — dilewati
				} finally {
					try {
						zis.closeEntry();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/helper/UploadFotoMassalHelper.java:656");
					}
				}
			}
		} catch (Exception eZip) {
			Common.tampilErrorJikaAdmin(eZip);
		} finally {
			if (zis != null) {
				try {
					zis.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/helper/UploadFotoMassalHelper.java:666");
				}
			}
		}
	}
}
