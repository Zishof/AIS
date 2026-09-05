package ais.database.model.file;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.sql.Blob;
import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.hibernate.Session;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.zkoss.zk.ui.event.EventListener;

import ais.common.Common;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.ui.util.MyMessageboxConfig;

/**
 * Entity <b>lampiran berkas umum mahasiswa aktif</b> &mdash; satu baris tabel
 * {@code public.lampiran_lain_mahasiswa} mewakili satu berkas dokumen yang diunggah MAHASISWA
 * AKTIF (sudah diterima, berbeda dari {@link LampiranLainBiodataCalonMahasiswa} yang menampung
 * dokumen CALON mahasiswa): ijazah, transkrip nilai, KTP/akte kelahiran/KK, NPWP, KTP orang
 * tua/wali, surat penunjukan pengurus organisasi, hingga sepuluh "lampiran custom" umum
 * ({@link #LAMPIRAN_1}&ndash;{@link #LAMPIRAN_10}) DAN lima "lampiran pengajuan skripsi"
 * ({@link #LAMPIRAN_PENGAJUAN_SKRIPSI_1}&ndash;{@link #LAMPIRAN_PENGAJUAN_SKRIPSI_5}) yang
 * masing-masing dikonfigurasi terpisah. Ini adalah file TERBESAR di klaster lampiran akademik
 * paket ini (639 baris), mencerminkan cakupannya yang paling luas: satu tabel menampung SELURUH
 * jenis dokumen umum sepanjang masa studi mahasiswa, bukan hanya satu program/kegiatan seperti
 * PKL/KKN/Beasiswa.
 *
 * <h2>Verifikasi arsitektur: entity MANDIRI, BUKAN wrapper/view {@code LampiranLain}</h2>
 * <p>Sama seperti {@link LampiranLainBiodataCalonMahasiswa} (lihat javadoc kelas tersebut untuk
 * penjelasan lengkap pola verifikasi), kelas ini SETELAH ditelusuri dari kode adalah entity
 * mandiri: tabel fisik {@code lampiran_lain_mahasiswa} terpisah dari {@code lampiran_lain} milik
 * {@code ais.database.model.file.LampiranLain}, kepemilikan diwakili kolom {@code Long mahasiswa}
 * milik kelas ini sendiri ({@link #getMahasiswa()}) yang menunjuk langsung ke
 * {@link ais.database.model.Mahasiswa#getId()}, dan kelas ini terdaftar sebagai entri TERPISAH
 * di {@code FileFotoLain.RELASI_MAP} ({@code LampiranLainMahasiswa.class -> "mahasiswa"}).
 * Kesamaan awalan nama "LampiranLain" murni konvensi penamaan historis, bukan relasi pewarisan
 * atau pembungkusan.</p>
 *
 * <h2>Relasi dengan {@link LampiranLainBiodataCalonMahasiswa} (file 8 paket ini)</h2>
 * <p>Lihat javadoc {@link LampiranLainBiodataCalonMahasiswa} untuk penjelasan lengkap: saat
 * seorang calon mahasiswa DITERIMA, {@code ais.common.CommonPMB} menyalin setiap baris lampiran
 * milik {@code BiodataCalonMahasiswa}-nya menjadi baris BARU di kelas ini (dipasangkan lewat
 * kesamaan nilai {@code jenis}), dengan blob disalin independen (bukan lewat {@code copyDari}).
 * Dengan kata lain, kelas ini adalah "kelanjutan" siklus hidup dokumen yang SEBELUMNYA berada di
 * {@link LampiranLainBiodataCalonMahasiswa} selama status masih calon mahasiswa.</p>
 *
 * <h2>Celah akses yang diwarisi (bukan sesuatu yang ditambahkan kelas ini)</h2>
 * <p>Sama seperti seluruh subclass {@link FileFotoLain} lain di paket ini, berkas entity ini
 * diunduh lewat servlet {@code /al} ({@code AmbilLampiran.process()}) yang TIDAK memverifikasi
 * sesi login maupun kepemilikan baris &mdash; instance dari pola IDOR generik
 * {@code task_b82b25d2}, bukan sesuatu yang spesifik ditambahkan kelas ini. Karena kelas ini
 * menyimpan dokumen paling sensitif dalam siklus hidup mahasiswa (akte kelahiran, KTP orang
 * tua/wali, NPWP, KK &mdash; lihat konstanta {@link #AKTE}, {@link #KTP_AYAH}, {@link #KTP_IBU},
 * {@link #KTP_WALI}, {@link #NPWP}, {@link #KK}), dampak celah tersebut BILA dieksploitasi
 * terhadap kelas ini tergolong tinggi; namun akar penyebabnya tetap sama (mekanisme bersama
 * {@code FileFotoLain}/servlet {@code /al}), sehingga perbaikannya harus dilakukan satu kali di
 * sana untuk seluruh ~50 subclass, bukan per-kelas.</p>
 *
 * <h2>Kerangka umum keluarga FileFotoLain</h2>
 * <p>Subclass {@link FileFotoLain}: seluruh mekanisme unggah/unduh, penyimpanan blob-atau-Google-
 * Drive/link eksternal, dan pembuatan tautan {@code /al?d=...} (lihat
 * {@link FileFotoLain#createLinkUri()}) diwariskan apa adanya.</p>
 *
 * <h2>Diaudit Envers</h2>
 * <p>{@code @Audited}: setiap INSERT/UPDATE/DELETE direkam ke tabel bayangan
 * {@code lampiran_lain_mahasiswa_AUD}, kecuali {@link #getFoto()} yang {@code @NotAudited}.</p>
 *
 * @see FileFotoLain
 * @see LampiranLainBiodataCalonMahasiswa
 * @see ais.database.model.Mahasiswa
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "lampiran_lain_mahasiswa")
public class LampiranLainMahasiswa extends FileFotoLain {
	private String lokasiSimpan;

	/**
	 * Lokasi simpan mentah cadangan; lihat catatan pada {@link LampiranPklMahasiswa#getLokasiSimpan()}
	 * mengenai field yang di-shadow dari {@link FileFoto} dan tidak dipetakan ke kolom database.
	 *
	 * @return path lokasi simpan sementara, atau {@code null} bila belum pernah diisi
	 */
	public String getLokasiSimpan() {
		return lokasiSimpan;
	}

	/**
	 * Menyetel lokasi simpan sementara di memori (tidak persisten ke basis data).
	 *
	 * @param lokasiSimpan path lokasi simpan yang akan disimpan sementara
	 */
	public void setLokasiSimpan(String lokasiSimpan) {
		this.lokasiSimpan = lokasiSimpan;
	}

	/** Nilai kolom {@code jenis} untuk lampiran transkrip nilai pendidikan sebelumnya. */
	public static final String TRANSKRIP_NILAI = "Transkrip Nilai";
	/** Nilai kolom {@code jenis} untuk lampiran surat penunjukan sebagai pengurus organisasi kemahasiswaan. */
	public static final String SURAT_PENUNJUKAN_PENGURUS_ORGANISASI = "surat penunjukan sebagai pengurus organisasi";
	/** Nilai kolom {@code jenis} untuk lampiran ijazah/surat keterangan lulus. */
	public static final String IJAZAH = "Ijazah";
	/** Nilai kolom {@code jenis} untuk lampiran KTP/kartu pelajar mahasiswa sendiri. */
	public static final String KTP = "Ktp";
	/** Nilai kolom {@code jenis} untuk lampiran akte kelahiran mahasiswa. */
	public static final String AKTE = "Akte Kelahiran";
	/** Nilai kolom {@code jenis} untuk lampiran NPWP (mahasiswa atau orang tua/wali). */
	public static final String NPWP = "NPWP";
	/** Nilai kolom {@code jenis} untuk lampiran KTP ayah kandung/wali laki-laki mahasiswa. */
	public static final String KTP_AYAH = "Ktp Ayah";
	/** Nilai kolom {@code jenis} untuk lampiran KTP ibu kandung/wali perempuan mahasiswa. */
	public static final String KTP_IBU = "Ktp Ibu";
	/** Nilai kolom {@code jenis} untuk lampiran KTP wali (bila bukan orang tua kandung). */
	public static final String KTP_WALI = "Ktp Wali";
	/** Nilai kolom {@code jenis} untuk lampiran Kartu Keluarga (KK). */
	public static final String KK = "Kartu Keluarga (KK)";

	/**
	 * Sepuluh konstanta {@link #LAMPIRAN_1}&ndash;{@link #LAMPIRAN_10} adalah nilai kolom
	 * {@code jenis} untuk "lampiran custom" tambahan yang keberadaan dan labelnya diatur lewat
	 * konfigurasi {@code mahasiswa_upload_lampiran_1..10} (lihat {@link #checkLampiran}). Lihat
	 * juga {@link #LAMPIRAN_PENGAJUAN_SKRIPSI_1} untuk kelompok konfigurasi TERPISAH yang
	 * konteksnya khusus pengajuan skripsi, bukan berkas umum mahasiswa.
	 */
	public static final String LAMPIRAN_1 = "Lampiran ke-1";
	/** Lihat {@link #LAMPIRAN_1}. */
	public static final String LAMPIRAN_2 = "Lampiran ke-2";
	/** Lihat {@link #LAMPIRAN_1}. */
	public static final String LAMPIRAN_3 = "Lampiran ke-3";
	/** Lihat {@link #LAMPIRAN_1}. */
	public static final String LAMPIRAN_4 = "Lampiran ke-4";
	/** Lihat {@link #LAMPIRAN_1}. */
	public static final String LAMPIRAN_5 = "Lampiran ke-5";
	/** Lihat {@link #LAMPIRAN_1}. */
	public static final String LAMPIRAN_6 = "Lampiran ke-6";
	/** Lihat {@link #LAMPIRAN_1}. */
	public static final String LAMPIRAN_7 = "Lampiran ke-7";
	/** Lihat {@link #LAMPIRAN_1}. */
	public static final String LAMPIRAN_8 = "Lampiran ke-8";
	/** Lihat {@link #LAMPIRAN_1}. */
	public static final String LAMPIRAN_9 = "Lampiran ke-9";
	/** Lihat {@link #LAMPIRAN_1}. */
	public static final String LAMPIRAN_10 = "Lampiran ke-10";

	/**
	 * Lima konstanta {@link #LAMPIRAN_PENGAJUAN_SKRIPSI_1}&ndash;{@link #LAMPIRAN_PENGAJUAN_SKRIPSI_5}
	 * adalah nilai kolom {@code jenis} KHUSUS untuk berkas yang dipersyaratkan saat mahasiswa
	 * mengajukan skripsi, diatur lewat konfigurasi TERPISAH
	 * {@code mahasiswa_upload_lampiran_pengajuan_skripsi_1..5} dan diperiksa lewat method
	 * TERPISAH {@link #checkLampiranPengajuanSkripsi} &mdash; BUKAN bagian dari pemeriksaan
	 * {@link #checkLampiran} (yang memeriksa {@link #LAMPIRAN_1}&ndash;{@link #LAMPIRAN_10}).
	 * Meski sama-sama disimpan di tabel yang sama (dibedakan hanya lewat nilai {@code jenis}),
	 * kedua kelompok "lampiran custom" ini independen satu sama lain dari sisi konfigurasi dan
	 * validasi.
	 */
	public static final String LAMPIRAN_PENGAJUAN_SKRIPSI_1 = "Lampiran Pengajuan Skripsi ke-1";
	/** Lihat {@link #LAMPIRAN_PENGAJUAN_SKRIPSI_1}. */
	public static final String LAMPIRAN_PENGAJUAN_SKRIPSI_2 = "Lampiran Pengajuan Skripsi ke-2";
	/** Lihat {@link #LAMPIRAN_PENGAJUAN_SKRIPSI_1}. */
	public static final String LAMPIRAN_PENGAJUAN_SKRIPSI_3 = "Lampiran Pengajuan Skripsi ke-3";
	/** Lihat {@link #LAMPIRAN_PENGAJUAN_SKRIPSI_1}. */
	public static final String LAMPIRAN_PENGAJUAN_SKRIPSI_4 = "Lampiran Pengajuan Skripsi ke-4";
	/** Lihat {@link #LAMPIRAN_PENGAJUAN_SKRIPSI_1}. */
	public static final String LAMPIRAN_PENGAJUAN_SKRIPSI_5 = "Lampiran Pengajuan Skripsi ke-5";

	/**
	 * Nilai kolom {@code jenis} apa adanya, sesuai kontrak {@link FileFoto#ambilJenis()}.
	 *
	 * @return nilai field {@code jenis} baris ini, bisa {@code null}
	 */
	@Override
	public String ambilJenis() {
		return jenis;
	}

	/**
	 * Memvalidasi bahwa seorang mahasiswa aktif SUDAH mengunggah seluruh berkas umum yang
	 * diwajibkan konfigurasi instalasi, sebelum diizinkan melanjutkan suatu alur (mis. pengajuan
	 * cuti, her-registrasi, atau proses administratif lain yang mensyaratkan kelengkapan berkas).
	 * Padanan langsung {@link LampiranLainBiodataCalonMahasiswa#checkLampiran} untuk mahasiswa
	 * AKTIF (bukan calon mahasiswa) &mdash; lihat javadoc method itu untuk pola umum dan
	 * perbedaannya dengan versi ini.
	 *
	 * <h4>Alur kerja</h4>
	 * <p>Membuka satu {@link Session} streaming
	 * ({@code StreamingHibernateUtil.getInstance().currentSession()}) dan, untuk SETIAP jenis
	 * berkas yang berpotensi wajib, menjalankan query {@code COUNT(*)} terpisah (Criteria +
	 * {@code Projections.rowCount()}) terhadap tabel ini (atau {@code FotoMahasiswa} untuk foto)
	 * guna memastikan setidaknya SATU baris dengan {@code jenis} dan {@code mahasiswa} yang
	 * sesuai sudah ada. Urutan pemeriksaannya:</p>
	 * <ol>
	 * <li><b>Foto</b> &mdash; SELALU wajib tanpa syarat konfigurasi apa pun (berbeda dari versi
	 * calon mahasiswa yang bisa dilonggarkan lewat {@code paket.getWajibUploadFoto()}).</li>
	 * <li><b>Ijazah, Transkrip Nilai, KTP (pendidikan sebelumnya)</b> &mdash; masing-masing wajib
	 * KECUALI konfigurasi {@code mahasiswa_tidak_perlu_upload_ijazah/nilai/ktp} diset {@code AKTIF}
	 * (default {@code AKTIF} milik parameter fallback berarti secara default TIDAK wajib bila
	 * konfigurasi belum pernah ditulis ke DB &mdash; sama seperti versi calon mahasiswa).</li>
	 * <li><b>Sepuluh lampiran custom ({@link #LAMPIRAN_1}&ndash;{@link #LAMPIRAN_10})</b> &mdash;
	 * baru diperiksa (dan baru wajib) bila konfigurasi {@code mahasiswa_upload_lampiran_1..10}
	 * secara eksplisit diset {@code AKTIF} (default {@code TIDAK_AKTIF}). Label pesan error
	 * diambil dari {@code konfigurasiUpload.getInfo1()}, bukan hardcode.</li>
	 * </ol>
	 * <p>Session ditutup SEGERA setelah seluruh COUNT selesai, sebelum satu pun pemeriksaan
	 * {@code == 0} dievaluasi.</p>
	 *
	 * <h4>Efek samping &amp; nilai balik</h4>
	 * <p>Pada kegagalan validasi PERTAMA yang ditemukan, method menampilkan
	 * {@link MyMessageboxConfig#show(String, String, int, int, EventListener)} berisi pesan
	 * "&lt;label butir&gt; harus di upload" (dengan label yang disesuaikan konteks mahasiswa
	 * aktif, mis. "Ijazah/surat keterangan lulus (pendidikan sebelumnya) harus di upload") dan
	 * mengembalikan {@code false}; bila seluruh butir wajib terpenuhi, mengembalikan {@code true}.
	 * Urutan pemeriksaan SELALU: foto &rarr; ijazah &rarr; nilai &rarr; ktp &rarr; lampiran 1..10.</p>
	 *
	 * <h4>Perbedaan dari {@link LampiranLainBiodataCalonMahasiswa#checkLampiran}</h4>
	 * <p>Method ini MENERIMA parameter {@code eventListener} yang diteruskan sebagai callback ke
	 * {@code MyMessageboxConfig.show(...)} (dipanggil setelah dialog ditutup pengguna), dan
	 * SELURUH badannya dibungkus {@code try/catch}: exception apa pun mengakibatkan
	 * {@code StreamingHibernateUtil.getInstance().rollbackTransaction()} dipanggil eksplisit lalu
	 * {@code Common.tampilErrorJikaAdmin(e)} sebelum method mengembalikan {@code true} secara
	 * default (perhatikan: path exception TETAP mengembalikan {@code true}, bukan {@code false}
	 * &mdash; kegagalan teknis pada pemeriksaan diperlakukan sebagai "lolos validasi", BUKAN
	 * "gagal validasi"; ini fail-open yang berbeda arah dari filter otorisasi, tetapi konsisten
	 * dengan tujuan method ini yaitu tidak memblokir pengguna karena error infrastruktur).
	 * Versi calon mahasiswa TIDAK memiliki keduanya (tidak ada parameter callback, tidak ada
	 * pembungkus {@code try/catch}).</p>
	 *
	 * @param mahasiswa baris mahasiswa aktif yang berkasnya diperiksa
	 * @param eventListener callback yang diteruskan ke setiap dialog {@code MyMessageboxConfig.show}
	 *        yang mungkin ditampilkan; boleh {@code null}
	 * @return {@code true} bila seluruh berkas wajib (sesuai konfigurasi aktif) sudah diunggah
	 *         ATAU bila terjadi exception internal (fail-open); {@code false} bila ada yang
	 *         kurang (pesan sudah ditampilkan ke pengguna)
	 * @throws Exception dideklarasikan pada signature tetapi tidak pernah benar-benar menjalar
	 *         keluar karena seluruh badan method dibungkus {@code try/catch} yang menangkap
	 *         {@link Exception}
	 */
	public static boolean checkLampiran(Mahasiswa mahasiswa, EventListener eventListener) throws Exception {
		try {
			Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();

			int foto = ((Number) streamingSession.createCriteria(FotoMahasiswa.class)
					.setProjection(Projections.rowCount()).add(Restrictions.eq("mahasiswa", mahasiswa.getId()))
					.setMaxResults(1).uniqueResult()).intValue();

			int ijazah = 1;
			int nilai = 1;
			int ktp = 1;
			int lampiran_1 = 1;
			int lampiran_2 = 1;
			int lampiran_3 = 1;
			int lampiran_4 = 1;
			int lampiran_5 = 1;
			int lampiran_6 = 1;
			int lampiran_7 = 1;
			int lampiran_8 = 1;
			int lampiran_9 = 1;
			int lampiran_10 = 1;

			if (Common.getKonfigurasi("mahasiswa_tidak_perlu_upload_ijazah", Konfigurasi.AKTIF).getNilai()
					.equals(Konfigurasi.TIDAK_AKTIF)) {
				ijazah = ((Number) streamingSession.createCriteria(LampiranLainMahasiswa.class)
						.setProjection(Projections.rowCount())
						.add(Restrictions.eq("jenis", LampiranLainMahasiswa.IJAZAH))
						.add(Restrictions.eq("mahasiswa", mahasiswa.getId())).setMaxResults(1).uniqueResult())
						.intValue();
			}

			if (Common.getKonfigurasi("mahasiswa_tidak_perlu_upload_nilai", Konfigurasi.AKTIF).getNilai()
					.equals(Konfigurasi.TIDAK_AKTIF)) {
				nilai = ((Number) streamingSession.createCriteria(LampiranLainMahasiswa.class)
						.setProjection(Projections.rowCount())
						.add(Restrictions.eq("jenis", LampiranLainMahasiswa.TRANSKRIP_NILAI))
						.add(Restrictions.eq("mahasiswa", mahasiswa.getId())).setMaxResults(1).uniqueResult())
						.intValue();
			}

			if (Common.getKonfigurasi("mahasiswa_tidak_perlu_upload_ktp", Konfigurasi.AKTIF).getNilai()
					.equals(Konfigurasi.TIDAK_AKTIF)) {
				ktp = ((Number) streamingSession.createCriteria(LampiranLainMahasiswa.class)
						.setProjection(Projections.rowCount()).add(Restrictions.eq("jenis", LampiranLainMahasiswa.KTP))
						.add(Restrictions.eq("mahasiswa", mahasiswa.getId())).setMaxResults(1).uniqueResult())
						.intValue();
			}

			Konfigurasi konfigurasiUpload = Common.getKonfigurasi("mahasiswa_upload_lampiran_1",
					Konfigurasi.TIDAK_AKTIF);
			String keteranganInfo1 = "";
			if (konfigurasiUpload.getNilai().equals(Konfigurasi.AKTIF)) {
				lampiran_1 = ((Number) streamingSession.createCriteria(LampiranLainMahasiswa.class)
						.setProjection(Projections.rowCount())
						.add(Restrictions.eq("jenis", LampiranLainMahasiswa.LAMPIRAN_1))
						.add(Restrictions.eq("mahasiswa", mahasiswa.getId())).setMaxResults(1).uniqueResult())
						.intValue();
				keteranganInfo1 = konfigurasiUpload.getInfo1();
			}

			konfigurasiUpload = Common.getKonfigurasi("mahasiswa_upload_lampiran_2", Konfigurasi.TIDAK_AKTIF);
			String keteranganInfo2 = "";
			if (konfigurasiUpload.getNilai().equals(Konfigurasi.AKTIF)) {
				lampiran_2 = ((Number) streamingSession.createCriteria(LampiranLainMahasiswa.class)
						.setProjection(Projections.rowCount())
						.add(Restrictions.eq("jenis", LampiranLainMahasiswa.LAMPIRAN_2))
						.add(Restrictions.eq("mahasiswa", mahasiswa.getId())).setMaxResults(1).uniqueResult())
						.intValue();
				keteranganInfo2 = konfigurasiUpload.getInfo1();
			}

			konfigurasiUpload = Common.getKonfigurasi("mahasiswa_upload_lampiran_3", Konfigurasi.TIDAK_AKTIF);
			String keteranganInfo3 = "";
			if (konfigurasiUpload.getNilai().equals(Konfigurasi.AKTIF)) {
				lampiran_3 = ((Number) streamingSession.createCriteria(LampiranLainMahasiswa.class)
						.setProjection(Projections.rowCount())
						.add(Restrictions.eq("jenis", LampiranLainMahasiswa.LAMPIRAN_3))
						.add(Restrictions.eq("mahasiswa", mahasiswa.getId())).setMaxResults(1).uniqueResult())
						.intValue();
				keteranganInfo3 = konfigurasiUpload.getInfo1();
			}

			konfigurasiUpload = Common.getKonfigurasi("mahasiswa_upload_lampiran_4", Konfigurasi.TIDAK_AKTIF);
			String keteranganInfo4 = "";
			if (konfigurasiUpload.getNilai().equals(Konfigurasi.AKTIF)) {
				lampiran_4 = ((Number) streamingSession.createCriteria(LampiranLainMahasiswa.class)
						.setProjection(Projections.rowCount())
						.add(Restrictions.eq("jenis", LampiranLainMahasiswa.LAMPIRAN_4))
						.add(Restrictions.eq("mahasiswa", mahasiswa.getId())).setMaxResults(1).uniqueResult())
						.intValue();
				keteranganInfo4 = konfigurasiUpload.getInfo1();
			}

			konfigurasiUpload = Common.getKonfigurasi("mahasiswa_upload_lampiran_5", Konfigurasi.TIDAK_AKTIF);
			String keteranganInfo5 = "";
			if (konfigurasiUpload.getNilai().equals(Konfigurasi.AKTIF)) {
				lampiran_5 = ((Number) streamingSession.createCriteria(LampiranLainMahasiswa.class)
						.setProjection(Projections.rowCount())
						.add(Restrictions.eq("jenis", LampiranLainMahasiswa.LAMPIRAN_5))
						.add(Restrictions.eq("mahasiswa", mahasiswa.getId())).setMaxResults(1).uniqueResult())
						.intValue();
				keteranganInfo5 = konfigurasiUpload.getInfo1();
			}

			konfigurasiUpload = Common.getKonfigurasi("mahasiswa_upload_lampiran_6", Konfigurasi.TIDAK_AKTIF);
			String keteranganInfo6 = "";
			if (konfigurasiUpload.getNilai().equals(Konfigurasi.AKTIF)) {
				lampiran_6 = ((Number) streamingSession.createCriteria(LampiranLainMahasiswa.class)
						.setProjection(Projections.rowCount())
						.add(Restrictions.eq("jenis", LampiranLainMahasiswa.LAMPIRAN_6))
						.add(Restrictions.eq("mahasiswa", mahasiswa.getId())).setMaxResults(1).uniqueResult())
						.intValue();
				keteranganInfo6 = konfigurasiUpload.getInfo1();
			}

			konfigurasiUpload = Common.getKonfigurasi("mahasiswa_upload_lampiran_7", Konfigurasi.TIDAK_AKTIF);
			String keteranganInfo7 = "";
			if (konfigurasiUpload.getNilai().equals(Konfigurasi.AKTIF)) {
				lampiran_7 = ((Number) streamingSession.createCriteria(LampiranLainMahasiswa.class)
						.setProjection(Projections.rowCount())
						.add(Restrictions.eq("jenis", LampiranLainMahasiswa.LAMPIRAN_7))
						.add(Restrictions.eq("mahasiswa", mahasiswa.getId())).setMaxResults(1).uniqueResult())
						.intValue();
				keteranganInfo7 = konfigurasiUpload.getInfo1();
			}

			konfigurasiUpload = Common.getKonfigurasi("mahasiswa_upload_lampiran_8", Konfigurasi.TIDAK_AKTIF);
			String keteranganInfo8 = "";
			if (konfigurasiUpload.getNilai().equals(Konfigurasi.AKTIF)) {
				lampiran_8 = ((Number) streamingSession.createCriteria(LampiranLainMahasiswa.class)
						.setProjection(Projections.rowCount())
						.add(Restrictions.eq("jenis", LampiranLainMahasiswa.LAMPIRAN_8))
						.add(Restrictions.eq("mahasiswa", mahasiswa.getId())).setMaxResults(1).uniqueResult())
						.intValue();
				keteranganInfo8 = konfigurasiUpload.getInfo1();
			}

			konfigurasiUpload = Common.getKonfigurasi("mahasiswa_upload_lampiran_9", Konfigurasi.TIDAK_AKTIF);
			String keteranganInfo9 = "";
			if (konfigurasiUpload.getNilai().equals(Konfigurasi.AKTIF)) {
				lampiran_9 = ((Number) streamingSession.createCriteria(LampiranLainMahasiswa.class)
						.setProjection(Projections.rowCount())
						.add(Restrictions.eq("jenis", LampiranLainMahasiswa.LAMPIRAN_9))
						.add(Restrictions.eq("mahasiswa", mahasiswa.getId())).setMaxResults(1).uniqueResult())
						.intValue();
				keteranganInfo9 = konfigurasiUpload.getInfo1();
			}

			konfigurasiUpload = Common.getKonfigurasi("mahasiswa_upload_lampiran_10", Konfigurasi.TIDAK_AKTIF);
			String keteranganInfo10 = "";
			if (konfigurasiUpload.getNilai().equals(Konfigurasi.AKTIF)) {
				lampiran_10 = ((Number) streamingSession.createCriteria(LampiranLainMahasiswa.class)
						.setProjection(Projections.rowCount())
						.add(Restrictions.eq("jenis", LampiranLainMahasiswa.LAMPIRAN_10))
						.add(Restrictions.eq("mahasiswa", mahasiswa.getId())).setMaxResults(1).uniqueResult())
						.intValue();
				keteranganInfo10 = konfigurasiUpload.getInfo1();
			}

			StreamingHibernateUtil.getInstance().closeSession();

			if (foto == 0) {
				MyMessageboxConfig.show("Foto harus di upload", "Informasi", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION, eventListener);
				return false;
			}

			if (ijazah == 0) {
				MyMessageboxConfig.show("Ijazah/surat keterangan lulus (pendidikan sebelumnya) harus di upload",
						"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, eventListener);
				return false;
			}

			if (nilai == 0) {
				MyMessageboxConfig.show("Transkrip nilai (pendidikan sebelumnya) harus di upload", "Informasi",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, eventListener);
				return false;
			}

			if (ktp == 0) {
				MyMessageboxConfig.show("KTP / Kartu Pelajar (pendidikan sebelumnya) harus di upload", "Informasi",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, eventListener);
				return false;
			}

			if (lampiran_1 == 0) {
				MyMessageboxConfig.show(keteranganInfo1 + " harus di upload", "Informasi", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION, eventListener);
				return false;
			}

			if (lampiran_2 == 0) {
				MyMessageboxConfig.show(keteranganInfo2 + " harus di upload", "Informasi", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION, eventListener);
				return false;
			}

			if (lampiran_3 == 0) {
				MyMessageboxConfig.show(keteranganInfo3 + " harus di upload", "Informasi", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION, eventListener);
				return false;
			}

			if (lampiran_4 == 0) {
				MyMessageboxConfig.show(keteranganInfo4 + " harus di upload", "Informasi", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION, eventListener);
				return false;
			}

			if (lampiran_5 == 0) {
				MyMessageboxConfig.show(keteranganInfo5 + " harus di upload", "Informasi", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION, eventListener);
				return false;
			}

			if (lampiran_6 == 0) {
				MyMessageboxConfig.show(keteranganInfo6 + " harus di upload", "Informasi", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION, eventListener);
				return false;
			}

			if (lampiran_7 == 0) {
				MyMessageboxConfig.show(keteranganInfo7 + " harus di upload", "Informasi", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION, eventListener);
				return false;
			}

			if (lampiran_8 == 0) {
				MyMessageboxConfig.show(keteranganInfo8 + " harus di upload", "Informasi", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION, eventListener);
				return false;
			}

			if (lampiran_9 == 0) {
				MyMessageboxConfig.show(keteranganInfo9 + " harus di upload", "Informasi", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION, eventListener);
				return false;
			}

			if (lampiran_10 == 0) {
				MyMessageboxConfig.show(keteranganInfo10 + " harus di upload", "Informasi", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION, eventListener);
				return false;
			}
		} catch (Exception e) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			Common.tampilErrorJikaAdmin(e);
		}

		return true;
	}

	/**
	 * Memvalidasi bahwa seorang mahasiswa SUDAH mengunggah seluruh "lampiran pengajuan skripsi"
	 * ({@link #LAMPIRAN_PENGAJUAN_SKRIPSI_1}&ndash;{@link #LAMPIRAN_PENGAJUAN_SKRIPSI_5}) yang
	 * diaktifkan konfigurasi instalasi, sebelum diizinkan mengajukan skripsi. Method ini SENGAJA
	 * TERPISAH dari {@link #checkLampiran} (yang memeriksa foto/ijazah/nilai/ktp/lampiran umum
	 * 1-10) karena konteks bisnisnya berbeda: seorang mahasiswa bisa saja sudah lolos pemeriksaan
	 * {@link #checkLampiran} jauh-jauh hari (saat awal masuk kuliah), tetapi baru perlu memenuhi
	 * lampiran pengajuan skripsi ini di semester akhir &mdash; menggabungkan keduanya dalam satu
	 * pemeriksaan akan salah secara bisnis (memaksa mahasiswa mengunggah ulang berkas skripsi
	 * setiap kali melakukan aksi yang sebenarnya hanya butuh berkas umum, atau sebaliknya).
	 *
	 * <h4>Alur kerja</h4>
	 * <p>Sama seperti {@link #checkLampiran}, membuka satu {@link Session} streaming dan
	 * menjalankan query {@code COUNT(*)} terpisah untuk MASING-MASING dari lima butir lampiran
	 * pengajuan skripsi, tetapi HANYA bila konfigurasi
	 * {@code mahasiswa_upload_lampiran_pengajuan_skripsi_1..5} terkait diset {@code AKTIF}
	 * (default {@code TIDAK_AKTIF} &mdash; secara default TIDAK ADA satu pun butir ini yang
	 * wajib, harus diaktifkan eksplisit oleh admin per instalasi). Tidak ada pemeriksaan
	 * foto/ijazah/nilai/ktp di method ini sama sekali &mdash; murni lima butir skripsi. Label
	 * pesan error diambil dari {@code konfigurasiUpload.getInfo1()} pada baris konfigurasi
	 * terkait, sama seperti method {@link #checkLampiran}. Session ditutup segera setelah seluruh
	 * COUNT selesai, sebelum pemeriksaan {@code == 0} dievaluasi.</p>
	 *
	 * <h4>Efek samping, nilai balik, dan perbedaan penanganan exception</h4>
	 * <p>Pada kegagalan validasi PERTAMA yang ditemukan (urutan tetap: lampiran 1 &rarr; 2 &rarr;
	 * 3 &rarr; 4 &rarr; 5), method menampilkan
	 * {@link MyMessageboxConfig#show(String, String, int, int, EventListener)} lalu mengembalikan
	 * {@code false}; bila seluruh butir yang diaktifkan terpenuhi, mengembalikan {@code true}.
	 * <b>Berbeda dari {@link #checkLampiran}</b>, method ini TIDAK membungkus badannya dengan
	 * {@code try/catch} &mdash; exception dari query Hibernate akan menjalar keluar apa adanya ke
	 * pemanggil (perilaku ini justru SAMA dengan
	 * {@link LampiranLainBiodataCalonMahasiswa#checkLampiran}, bukan dengan
	 * {@link #checkLampiran} di kelas yang sama), meski signature-nya tetap menerima parameter
	 * {@code eventListener} seperti {@link #checkLampiran}.</p>
	 *
	 * @param mahasiswa mahasiswa yang mengajukan skripsi, berkasnya diperiksa
	 * @param eventListener callback yang diteruskan ke setiap dialog {@code MyMessageboxConfig.show}
	 *        yang mungkin ditampilkan; boleh {@code null}
	 * @return {@code true} bila seluruh lampiran pengajuan skripsi yang diaktifkan konfigurasi
	 *         sudah diunggah; {@code false} bila ada yang kurang (pesan sudah ditampilkan)
	 * @throws Exception bila query Hibernate gagal (TIDAK ditangkap secara lokal, berbeda dari
	 *         {@link #checkLampiran} pada kelas yang sama)
	 */
	public static boolean checkLampiranPengajuanSkripsi(Mahasiswa mahasiswa, EventListener eventListener)
			throws Exception {
		Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();

		int lampiran_1 = 1;
		int lampiran_2 = 1;
		int lampiran_3 = 1;
		int lampiran_4 = 1;
		int lampiran_5 = 1;

		Konfigurasi konfigurasiUpload = Common.getKonfigurasi("mahasiswa_upload_lampiran_pengajuan_skripsi_1",
				Konfigurasi.TIDAK_AKTIF);
		String keteranganInfo1 = "";
		if (konfigurasiUpload.getNilai().equals(Konfigurasi.AKTIF)) {
			lampiran_1 = ((Number) streamingSession.createCriteria(LampiranLainMahasiswa.class)
					.setProjection(Projections.rowCount())
					.add(Restrictions.eq("jenis", LampiranLainMahasiswa.LAMPIRAN_PENGAJUAN_SKRIPSI_1))
					.add(Restrictions.eq("mahasiswa", mahasiswa.getId())).setMaxResults(1).uniqueResult()).intValue();
			keteranganInfo1 = konfigurasiUpload.getInfo1();
		}

		konfigurasiUpload = Common.getKonfigurasi("mahasiswa_upload_lampiran_pengajuan_skripsi_2",
				Konfigurasi.TIDAK_AKTIF);
		String keteranganInfo2 = "";
		if (konfigurasiUpload.getNilai().equals(Konfigurasi.AKTIF)) {
			lampiran_2 = ((Number) streamingSession.createCriteria(LampiranLainMahasiswa.class)
					.setProjection(Projections.rowCount())
					.add(Restrictions.eq("jenis", LampiranLainMahasiswa.LAMPIRAN_PENGAJUAN_SKRIPSI_2))
					.add(Restrictions.eq("mahasiswa", mahasiswa.getId())).setMaxResults(1).uniqueResult()).intValue();
			keteranganInfo2 = konfigurasiUpload.getInfo1();
		}

		konfigurasiUpload = Common.getKonfigurasi("mahasiswa_upload_lampiran_pengajuan_skripsi_3",
				Konfigurasi.TIDAK_AKTIF);
		String keteranganInfo3 = "";
		if (konfigurasiUpload.getNilai().equals(Konfigurasi.AKTIF)) {
			lampiran_3 = ((Number) streamingSession.createCriteria(LampiranLainMahasiswa.class)
					.setProjection(Projections.rowCount())
					.add(Restrictions.eq("jenis", LampiranLainMahasiswa.LAMPIRAN_PENGAJUAN_SKRIPSI_3))
					.add(Restrictions.eq("mahasiswa", mahasiswa.getId())).setMaxResults(1).uniqueResult()).intValue();
			keteranganInfo3 = konfigurasiUpload.getInfo1();
		}

		konfigurasiUpload = Common.getKonfigurasi("mahasiswa_upload_lampiran_pengajuan_skripsi_4",
				Konfigurasi.TIDAK_AKTIF);
		String keteranganInfo4 = "";
		if (konfigurasiUpload.getNilai().equals(Konfigurasi.AKTIF)) {
			lampiran_4 = ((Number) streamingSession.createCriteria(LampiranLainMahasiswa.class)
					.setProjection(Projections.rowCount())
					.add(Restrictions.eq("jenis", LampiranLainMahasiswa.LAMPIRAN_PENGAJUAN_SKRIPSI_4))
					.add(Restrictions.eq("mahasiswa", mahasiswa.getId())).setMaxResults(1).uniqueResult()).intValue();
			keteranganInfo4 = konfigurasiUpload.getInfo1();
		}

		konfigurasiUpload = Common.getKonfigurasi("mahasiswa_upload_lampiran_pengajuan_skripsi_5",
				Konfigurasi.TIDAK_AKTIF);
		String keteranganInfo5 = "";
		if (konfigurasiUpload.getNilai().equals(Konfigurasi.AKTIF)) {
			lampiran_5 = ((Number) streamingSession.createCriteria(LampiranLainMahasiswa.class)
					.setProjection(Projections.rowCount())
					.add(Restrictions.eq("jenis", LampiranLainMahasiswa.LAMPIRAN_PENGAJUAN_SKRIPSI_5))
					.add(Restrictions.eq("mahasiswa", mahasiswa.getId())).setMaxResults(1).uniqueResult()).intValue();
			keteranganInfo5 = konfigurasiUpload.getInfo1();
		}

		StreamingHibernateUtil.getInstance().closeSession();

		if (lampiran_1 == 0) {
			MyMessageboxConfig.show(keteranganInfo1 + " harus di upload", "Informasi", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, eventListener);
			return false;
		}

		if (lampiran_2 == 0) {
			MyMessageboxConfig.show(keteranganInfo2 + " harus di upload", "Informasi", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, eventListener);
			return false;
		}

		if (lampiran_3 == 0) {
			MyMessageboxConfig.show(keteranganInfo3 + " harus di upload", "Informasi", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, eventListener);
			return false;
		}

		if (lampiran_4 == 0) {
			MyMessageboxConfig.show(keteranganInfo4 + " harus di upload", "Informasi", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, eventListener);
			return false;
		}

		if (lampiran_5 == 0) {
			MyMessageboxConfig.show(keteranganInfo5 + " harus di upload", "Informasi", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, eventListener);
			return false;
		}

		return true;
	}

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * Identitas (biasanya userid) pihak yang terakhir mengunggah/mengubah baris ini.
	 *
	 * @return id pihak pengunggah/pengubah, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel identitas pihak pengunggah/pengubah. Nilai kosong/{@code null} SENGAJA diabaikan
	 * agar simpan-ulang yang tidak membawa nilai baru tidak menimpa identitas asli.
	 *
	 * @param olehId id pihak pengunggah/pengubah; nilai {@code null}/kosong diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama tampilan pihak pengunggah/pengubah; nilai kosong/{@code null} diabaikan
	 * (lihat {@link #setOlehId(String)}).
	 *
	 * @param oleh nama pihak pengunggah/pengubah; nilai {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Nama tampilan pihak yang terakhir mengunggah/mengubah baris ini.
	 *
	 * @return nama pengunggah/pengubah, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback lifecycle JPA {@code @PreUpdate}, memperbarui {@link #tanggal_dirubah} otomatis
	 * sebelum UPDATE lewat {@code AuditTimestampInterceptor.ubah(this)}.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir secara manual.
	 *
	 * @param tanggal_dirubah stempel waktu perubahan yang baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Stempel waktu perubahan terakhir baris ini.
	 *
	 * @return tanggal &amp; waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi string ringkas baris ini untuk log/debug: nama berkas apa adanya (field
	 * {@code nama} langsung, tanpa fallback {@code copyDari}/link milik getter).
	 *
	 * @return nama berkas apa adanya, bisa {@code null}
	 */
	public String toString() {
		return nama;
	}

	private String nama;
	private String keterangan;
	private Long mahasiswa;
	private Blob foto;
	private String jenis;
	private LampiranLainMahasiswa copyDari;

	/** Konstruktor kosong wajib bagi entity JPA/Hibernate; field diisi lewat setter/reflection. */
	public LampiranLainMahasiswa() {
	}

	/**
	 * Primary key baris ini. {@code insertable = false} karena nilainya SELALU berasal dari
	 * {@code GenerationType.IDENTITY} (kolom serial PostgreSQL).
	 *
	 * @return id baris, {@code null} sebelum baris pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel id baris; jarang dipanggil manual karena id dibangkitkan basis data.
	 *
	 * @param id id baris yang akan disetel
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Nama berkas asli (termasuk ekstensi) sebagaimana diunggah mahasiswa. Sama seperti
	 * {@link LampiranLainBiodataCalonMahasiswa#getNama()}, method ini punya dua fallback
	 * berurutan: {@link #copyDari} diprioritaskan bila baris ini salinan; jika tidak, dan
	 * {@link #getLink()} tidak kosong, field {@code nama} ditimpa literal
	 * {@code "Berupa link file"}.
	 *
	 * @return nama berkas (di-trim), literal {@code "Berupa link file"} bila lampiran berupa
	 *         tautan, atau {@code null} bila keduanya tidak berlaku dan field lokal kosong
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		if (copyDari != null) {
			nama = copyDari.nama;
		} else if (!getLink().isEmpty()) {
			nama = "Berupa link file";
		}
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama berkas asli. Lihat {@link #getNama()} untuk perilaku fallback yang dapat
	 * menimpa nilai ini saat dibaca kembali.
	 *
	 * @param nama nama berkas (termasuk ekstensi)
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Keterangan bebas terkait berkas ini (mis. label butir dokumen yang dipenuhi).
	 *
	 * @return keterangan berkas, bisa {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan berkas.
	 *
	 * @param keterangan keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menyetel isi berkas biner (Large Object PostgreSQL).
	 *
	 * @param foto isi berkas biner
	 */
	public void setFoto(Blob foto) {
		this.foto = foto;
	}

	/**
	 * Isi berkas biner baris ini. Sama seperti
	 * {@link LampiranLainBiodataCalonMahasiswa#getFoto()}: {@code null} bila disimpan di Google
	 * Drive; jika tidak, field lokal {@code foto} diutamakan BILA TERISI, dan hanya bila kosong
	 * barulah blob {@link #copyDari} dipakai sebagai fallback (urutan ini berbeda dari
	 * {@link LampiranPklMahasiswa#getFoto()} yang selalu memprioritaskan {@code copyDari} lebih
	 * dulu). Ditandai {@code @NotAudited} agar isi biner tidak digandakan ke tabel bayangan
	 * Envers.
	 *
	 * @return blob isi berkas, atau {@code null} bila disimpan di Google Drive
	 */
	@NotAudited
	public Blob getFoto() {
		return gdrive != null && !gdrive.trim().isEmpty() ? null : (foto != null ? foto : (copyDari == null ? null : copyDari.foto));
	}

	/**
	 * Menyetel id baris {@link ais.database.model.Mahasiswa} pemilik lampiran ini.
	 *
	 * @param mahasiswa id {@code Mahasiswa} pemilik lampiran
	 */
	public void setMahasiswa(Long mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * Id baris {@link ais.database.model.Mahasiswa} pemilik lampiran ini. Lihat javadoc kelas
	 * mengenai verifikasi bahwa kelas ini adalah entity mandiri: field ini {@code Long} polos
	 * (bukan {@code @ManyToOne}), tetapi tetap menjadi kunci kepemilikan satu-satunya untuk
	 * kelas ini (didaftarkan sebagai field relasi di {@code FileFotoLain.RELASI_MAP}).
	 *
	 * @return id {@code Mahasiswa} pemilik lampiran, bisa {@code null}
	 */
	@Column(name = "mahasiswa", nullable = true)
	public Long getMahasiswa() {
		return mahasiswa;
	}

	/**
	 * Nilai kolom {@code jenis} apa adanya. Sama seperti
	 * {@link LampiranLainBiodataCalonMahasiswa#getJenis()}, getter ini TIDAK menimpa nilai dengan
	 * konstanta default apa pun &mdash; satu {@code Mahasiswa} punya BANYAK baris lampiran dengan
	 * {@code jenis} berbeda-beda (lihat konstanta {@link #IJAZAH}, {@link #KTP}, {@link #AKTE},
	 * dst).
	 *
	 * @return nilai field {@code jenis}, salah satu konstanta di kelas ini atau nilai bebas lain,
	 *         bisa {@code null}
	 */
	public String getJenis() {
		return jenis;
	}

	/**
	 * Menyetel jenis dokumen yang dipenuhi lampiran ini.
	 *
	 * @param jenis nilai jenis, idealnya salah satu konstanta yang dideklarasikan kelas ini
	 */
	public void setJenis(String jenis) {
		this.jenis = jenis;
	}

	/**
	 * Baris sumber bila lampiran ini "salinan" &mdash; berbagi nama/gdrive fisik milik baris lain
	 * (lihat catatan urutan prioritas pada {@link #getFoto()}). {@code @NotFound(IGNORE)}: bila
	 * baris sumber sudah terhapus, dikembalikan {@code null} alih-alih melempar exception.
	 *
	 * @return baris sumber salinan, atau {@code null} bila bukan salinan / sumbernya sudah tak ada
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@NotFound(action = NotFoundAction.IGNORE)
	@JoinColumn(name = "copy_dari", nullable = true)
	public LampiranLainMahasiswa getCopyDari() {
		return copyDari;
	}

	/**
	 * Menandai baris ini sebagai salinan dari baris {@code copyDari} yang diberikan.
	 *
	 * @param copyDari baris sumber yang nama/gdrive-nya akan dibagikan
	 */
	public void setCopyDari(LampiranLainMahasiswa copyDari) {
		this.copyDari = copyDari;
	}

	private String gdrive;
	private String gdriveUsername;

	private String link;

	/**
	 * Id berkas Google Drive tempat isi berkas sesungguhnya disimpan. Lihat penjelasan lengkap
	 * pada {@link LampiranPklMahasiswa#getGdrive()} mengenai fallback {@code copyDari} dan
	 * {@code retreive("gdrive")}.
	 *
	 * @return id berkas Google Drive, atau {@code null}/kosong bila berkas disimpan sebagai blob
	 */
	public String getGdrive() {
		if (copyDari != null) {
			gdrive = copyDari.gdrive;
		}
		String s = gdrive == null || gdrive.trim().isEmpty() ? retreive("gdrive") : gdrive;
		return s != null && !s.trim().isEmpty() ? s : gdrive;
	}

	/**
	 * Menyetel id berkas Google Drive; nilai tidak kosong juga disimpan lewat {@code put()} agar
	 * konsisten dengan jalur baca fallback di {@link #getGdrive()}.
	 *
	 * @param gdrive id berkas Google Drive; nilai kosong tidak dipropagasi ke {@code put()}
	 */
	public void setGdrive(String gdrive) {
		if (gdrive != null && !gdrive.trim().isEmpty()) {
			put(gdrive, "gdrive");
		}
		this.gdrive = gdrive;
	}

	/**
	 * Username akun Google yang dipakai mengunggah ke Drive. Diwarisi dari {@link #copyDari} bila
	 * baris ini salinan.
	 *
	 * @return username akun Google pengunggah, bisa {@code null}
	 */
	public String getGdriveUsername() {
		if (copyDari != null) {
			gdriveUsername = copyDari.gdriveUsername;
		}
		return gdriveUsername;
	}

	/**
	 * Menyetel username akun Google Drive pengunggah.
	 *
	 * @param gdriveUsername username akun Google
	 */
	public void setGdriveUsername(String gdriveUsername) {
		this.gdriveUsername = gdriveUsername;
	}

	/**
	 * Implementasi kontrak {@link FileFotoLain#ambilRef()}: mengembalikan {@link #getMahasiswa()}
	 * sebagai "acuan" generik dipakai mesin {@code FileFotoLain.ambil()}/{@code hapusAtauUpdate()}.
	 *
	 * @return id {@code Mahasiswa} pemilik lampiran (sama dengan {@link #getMahasiswa()})
	 */
	@Override
	public Long ambilRef() {
		return getMahasiswa();
	}

	/**
	 * Implementasi kontrak {@link FileFotoLain#ambilClazz()}. Sama seperti
	 * {@link LampiranLainBiodataCalonMahasiswa#ambilClazz()}, mengembalikan literal
	 * {@code LampiranLainMahasiswa.class} (bukan {@code this.getClass()} seperti pola aman pada
	 * {@link LampiranPklMahasiswa#ambilClazz()}); lihat catatan yang sama pada javadoc method
	 * tersebut mengenai proxy Hibernate.
	 *
	 * @return selalu literal {@code LampiranLainMahasiswa.class}
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClazz() {
		// TODO Auto-generated method stub
		return LampiranLainMahasiswa.class;
	}

	/**
	 * Tautan eksternal (mis. URL Google Photos/Drive berbagi) bila lampiran ini berupa link
	 * alih-alih berkas unggahan biasa. Dipakai {@link #getNama()} untuk menandai nama tampilan
	 * {@code "Berupa link file"}.
	 *
	 * @return tautan (di-trim), string kosong bila tidak ada (tidak pernah {@code null})
	 */
	public String getLink() {
		return link == null ? "" : link.trim();
	}

	/**
	 * Menyetel tautan eksternal berkas.
	 *
	 * @param link tautan eksternal yang akan disimpan
	 */
	public void setLink(String link) {
		this.link = link;
	}

	/**
	 * Implementasi kontrak {@link FileFoto#ambilLink()}: tautan eksternal berkas ini bila
	 * lampiran berupa link. Lihat {@link #getLink()}.
	 *
	 * @return tautan eksternal berkas, string kosong bila tidak ada
	 */
	@Override
	public String ambilLink() {
		return getLink();
	}

	private String url;

	/**
	 * URL siap pakai untuk mengunduh/menampilkan berkas ini lewat servlet {@code /al}, dibangun
	 * sekali lewat {@link FileFotoLain#createLinkUri()} lalu di-cache di field {@link #url}.
	 * Memiliki guard {@code url == null && getId() != null} (sama seperti
	 * {@link LampiranBeasiswaMahasiswa#getUrl()}) sehingga tautan hanya dibangun sekali dan hanya
	 * untuk baris yang sudah tersimpan. Exception dicatat ke {@code ErrorAuditUtil} dan ditelan
	 * supaya kegagalan satu baris tidak menghentikan render grid lampiran.
	 *
	 * @return URL unduhan/tampilan berkas, atau {@code null} bila {@code getId() == null} atau
	 *         gagal membangun tautan
	 */
	@Transient
	public String getUrl() {
		try {
			if (url == null && getId() != null) {
				url = createLinkUri();
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/file/LampiranLainMahasiswa.java:631");
		}
		return url;
	}

	/**
	 * Menyetel URL secara langsung; nilai ini akan DIPERTAHANKAN oleh {@link #getUrl()} pada
	 * pemanggilan berikutnya (tidak ditimpa) karena guard {@code url == null} pada getter.
	 *
	 * @param url URL yang akan disetel
	 */
	public void setUrl(String url) {
		this.url = url;
	}
}
