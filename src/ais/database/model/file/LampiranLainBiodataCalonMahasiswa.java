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

import ais.common.Common;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Konfigurasi;
import ais.ui.util.MyMessageboxConfig;

/**
 * Entity <b>lampiran berkas pendaftaran calon mahasiswa</b> &mdash; satu baris tabel
 * {@code public.lampiran_lain_biodata_calon_mahasiswa} mewakili satu berkas dokumen pendaftaran
 * yang diunggah CALON mahasiswa (belum diterima/belum menjadi mahasiswa aktif) selama proses
 * Penerimaan Mahasiswa Baru (PMB/PPDB): ijazah, transkrip nilai, KTP/kartu pelajar, bukti bayar
 * pendaftaran/daftar ulang, serta hingga sepuluh "lampiran custom" tambahan yang jenis dan
 * labelnya dikonfigurasi lewat {@code Konfigurasi} ({@code calon_mahasiswa_upload_lampiran_1..10}).
 * Satu baris di tabel ini berkorespondensi dengan satu
 * {@link ais.database.model.BiodataCalonMahasiswa} lewat kolom {@link #getBiodataCalonMahasiswa()}
 * dan satu nilai {@code jenis} (salah satu konstanta {@link #IJAZAH}, {@link #KTP}, dst.).
 *
 * <h2>Verifikasi arsitektur: entity MANDIRI, BUKAN wrapper/view {@code LampiranLain}</h2>
 * <p>Meski namanya diawali "LampiranLain" seperti {@code ais.database.model.file.LampiranLain}
 * (entity generik paket ini yang dipakai fitur lampiran umum lainnya), kelas ini adalah
 * <b>entity mandiri dan berbeda</b>, BUKAN view/wrapper di atas {@code LampiranLain} &mdash;
 * pola "nama class menyesatkan" yang sebelumnya berulang kali ditemukan di codebase ini TIDAK
 * berlaku di sini setelah diverifikasi dari kode, bukan dari nama:</p>
 * <ul>
 * <li>Tabel fisiknya sendiri, {@code lampiran_lain_biodata_calon_mahasiswa}, terpisah total dari
 * tabel {@code lampiran_lain} milik {@code LampiranLain}.</li>
 * <li>Relasi kepemilikannya adalah kolom {@code Long biodataCalonMahasiswa} milik kelas ini
 * sendiri ({@link #getBiodataCalonMahasiswa()}) yang menunjuk langsung ke
 * {@link ais.database.model.BiodataCalonMahasiswa#getId()} &mdash; BUKAN kolom {@code ref}
 * generik milik {@code LampiranLain}.</li>
 * <li>Kelas ini terdaftar sebagai entri TERPISAH di {@code FileFotoLain.RELASI_MAP}
 * ({@code LampiranLainBiodataCalonMahasiswa.class -> "biodataCalonMahasiswa"}, berbeda dari
 * entri {@code LampiranLain.class -> "ref"}), sehingga mesin generik
 * {@code FileFotoLain.ambil()}/{@code createFileFotoLain()} memperlakukannya sebagai kelas lampiran
 * sendiri, independen dari {@code LampiranLain}.</li>
 * </ul>
 * <p>Kesamaan nama semata-mata konvensi penamaan historis ("LampiranLain" + entitas pemilik),
 * bukan indikasi hubungan pewarisan/pembungkusan apa pun.</p>
 *
 * <h2>Relasi dengan {@link LampiranLainMahasiswa} (file 9 paket ini): migrasi saat diterima</h2>
 * <p>Saat seorang calon mahasiswa DITERIMA dan datanya dikonversi menjadi mahasiswa aktif, method
 * migrasi di {@code ais.common.CommonPMB} (bagian "PROSES LAMPIRAN LAIN BIODATA -&gt; MAHASISWA")
 * MENYALIN setiap baris kelas ini milik satu {@code BiodataCalonMahasiswa} menjadi baris
 * {@link LampiranLainMahasiswa} baru (atau memperbarui baris yang sudah ada) milik
 * {@link ais.database.model.Mahasiswa} hasil konversi, DIPASANGKAN lewat kesamaan nilai
 * {@code jenis} (mis. baris {@code jenis="Ijazah"} di sini disalin ke baris
 * {@code jenis="Ijazah"} di {@link LampiranLainMahasiswa}). Isi blob disalin dalam transaksi
 * yang sama (bukan dibagi lewat {@code copyDari}), sehingga kedua baris punya blob independen
 * setelah migrasi &mdash; berkas calon mahasiswa yang lama TETAP ada di tabel ini walau
 * mahasiswa sudah aktif, sekadar tidak lagi menjadi sumber tampilan utama.</p>
 *
 * <h2>Celah akses yang diwarisi (bukan sesuatu yang ditambahkan kelas ini)</h2>
 * <p>Sama seperti seluruh subclass {@link FileFotoLain} lain di paket ini, berkas entity ini
 * diunduh lewat servlet {@code /al} ({@code AmbilLampiran.process()}) yang TIDAK memverifikasi
 * sesi login maupun kepemilikan baris &mdash; instance dari pola IDOR generik
 * {@code task_b82b25d2}. Kelas ini sempat dicurigai berpotensi menjadi instance TAMBAHAN dari
 * {@code task_1f9c66d3} (dokumen pendaftaran anak di bawah umur PPDB yang bocor lewat
 * {@code LampiranLain.createLinkUri()} + rute publik-anonim {@code /ppdb?hanya_tampil_jsp=true}):
 * setelah ditelusuri, rute {@code Ppdb} servlet TERSEBUT hanya menyajikan halaman JSP shell,
 * TIDAK mengambil baris {@link LampiranLainBiodataCalonMahasiswa} secara langsung di lapisan
 * servlet-nya; pengambilan datanya (termasuk lampiran ini, lewat {@code CommonPMB}/
 * {@code BiodataCalonMahasiswaAction}) terjadi di lapisan Action/ZK yang TIDAK diverifikasi tuntas
 * dalam sesi dokumentasi ini apakah dapat diakses tanpa login. Catatan ini disengaja TIDAK
 * dijadikan task baru karena akar penyebab (bila memang ada) akan berupa kekurangan otorisasi di
 * {@code BiodataCalonMahasiswaAction}/rute PMB publik &mdash; perluasan area yang sama dengan
 * {@code task_1f9c66d3}, bukan pola baru. Bagian yang SUDAH pasti berlaku untuk kelas ini adalah
 * pola generik {@code task_b82b25d2} di atas.</p>
 *
 * <h2>Kerangka umum keluarga FileFotoLain</h2>
 * <p>Subclass {@link FileFotoLain}: seluruh mekanisme unggah/unduh, penyimpanan blob-atau-Google-
 * Drive/link eksternal, dan pembuatan tautan {@code /al?d=...} (lihat
 * {@link FileFotoLain#createLinkUri()}) diwariskan apa adanya.</p>
 *
 * <h2>Diaudit Envers</h2>
 * <p>{@code @Audited}: setiap INSERT/UPDATE/DELETE direkam ke tabel bayangan
 * {@code lampiran_lain_biodata_calon_mahasiswa_AUD}, kecuali {@link #getFoto()} yang
 * {@code @NotAudited}.</p>
 *
 * @see FileFotoLain
 * @see LampiranLainMahasiswa
 * @see ais.database.model.BiodataCalonMahasiswa
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "lampiran_lain_biodata_calon_mahasiswa")
public class LampiranLainBiodataCalonMahasiswa extends FileFotoLain {
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
	/** Nilai kolom {@code jenis} untuk lampiran ijazah/surat keterangan lulus. */
	public static final String IJAZAH = "Ijazah";
	/** Nilai kolom {@code jenis} untuk lampiran KTP/kartu pelajar calon mahasiswa. */
	public static final String KTP = "Ktp";

	/** Nilai kolom {@code jenis} untuk lampiran bukti pembayaran biaya pendaftaran. */
	public static final String BUKTI_BAYAR_PENDAFTARAN = "Bukti Bayar Pendaftaran";
	/** Nilai kolom {@code jenis} untuk lampiran bukti pembayaran biaya daftar ulang. */
	public static final String BUKTI_BAYAR_DAFTAR_ULANG = "Bukti Bayar Daftar Ulang";

	/**
	 * Sepuluh konstanta {@link #LAMPIRAN_1}&ndash;{@link #LAMPIRAN_10} adalah nilai kolom
	 * {@code jenis} untuk "lampiran custom" tambahan yang keberadaan dan labelnya diatur lewat
	 * konfigurasi {@code calon_mahasiswa_upload_lampiran_1..10} (lihat {@link #checkLampiran}).
	 * Berbeda dari {@link #IJAZAH}/{@link #KTP}/dst yang maknanya tetap di seluruh instalasi AIS,
	 * makna sesungguhnya butir-butir ini (label yang tampil ke calon mahasiswa) dikonfigurasi per
	 * instalasi lewat {@code Konfigurasi.getInfo1()} pada baris konfigurasi terkait.
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
	 * Nilai kolom {@code jenis} apa adanya, sesuai kontrak {@link FileFoto#ambilJenis()}.
	 *
	 * @return nilai field {@code jenis} baris ini, bisa {@code null}
	 */
	@Override
	public String ambilJenis() {
		return jenis;
	}

	/**
	 * Memvalidasi bahwa seorang calon mahasiswa SUDAH mengunggah seluruh berkas pendaftaran yang
	 * diwajibkan konfigurasi instalasi, sebelum diizinkan melanjutkan alur pendaftaran (mis.
	 * submit formulir PMB/PPDB, atau membayar). Dipanggil dari lapisan Action (mis.
	 * {@code ais.action.master.pmb.BiodataCalonMahasiswaAction}) tepat sebelum baris
	 * {@code BiodataCalonMahasiswa} dianggap "lengkap".
	 *
	 * <h4>Alur kerja</h4>
	 * <p>Method ini membuka satu {@link Session} streaming
	 * ({@code StreamingHibernateUtil.getInstance().currentSession()}) dan, untuk SETIAP jenis
	 * berkas yang berpotensi wajib, menjalankan query {@code COUNT(*)} terpisah (Criteria +
	 * {@code Projections.rowCount()}) terhadap tabel ini (atau tabel {@code FotoBiodataCalonMahasiswa}
	 * untuk foto) guna memastikan setidaknya SATU baris dengan {@code jenis} dan
	 * {@code biodataCalonMahasiswa} yang sesuai sudah ada. Urutan pemeriksaannya:</p>
	 * <ol>
	 * <li><b>Foto</b> &mdash; wajib KECUALI paket pendaftaran ({@code biodataCalonMahasiswa.getPaket()})
	 * eksplisit menyetel {@code getWajibUploadFoto() == false}. Bila paket {@code null}, foto tetap
	 * dianggap wajib (fail-closed terhadap ketiadaan konfigurasi paket).</li>
	 * <li><b>Ijazah, Transkrip Nilai, KTP</b> &mdash; masing-masing wajib KECUALI konfigurasi terkait
	 * ({@code calon_mahasiswa_tidak_perlu_upload_ijazah/nilai/ktp}) diset {@code AKTIF} (nama
	 * konfigurasi berpolaritas terbalik: nilai {@code AKTIF} berarti "TIDAK WAJIB", sesuai makna
	 * literalnya "tidak perlu upload"). Bila konfigurasi belum pernah ditulis ke DB,
	 * {@code Common.getKonfigurasi(..., Konfigurasi.AKTIF)} memakai {@code AKTIF} sebagai default
	 * sehingga secara default BERKAS INI TIDAK DIWAJIBKAN &mdash; berbeda dari foto yang defaultnya
	 * justru WAJIB.</li>
	 * <li><b>Sepuluh lampiran custom ({@link #LAMPIRAN_1}&ndash;{@link #LAMPIRAN_10})</b> &mdash;
	 * KEBALIKAN dari tiga butir di atas: masing-masing baru diperiksa (dan baru wajib) bila
	 * konfigurasi {@code calon_mahasiswa_upload_lampiran_1..10} secara eksplisit diset
	 * {@code AKTIF} (default {@code TIDAK_AKTIF}, artinya secara default tidak ada satu pun dari sepuluh
	 * butir ini yang wajib kecuali diaktifkan admin per instalasi). Label pesan error untuk tiap
	 * butir diambil dari {@code konfigurasiUpload.getInfo1()} pada baris konfigurasi yang sama,
	 * BUKAN hardcode &mdash; sehingga administrator dapat mengatur teks yang tampil ke calon
	 * mahasiswa tanpa mengubah kode.</li>
	 * </ol>
	 * <p>Session ditutup SEGERA setelah seluruh COUNT selesai dijalankan (SEBELUM satu pun
	 * pemeriksaan {@code == 0} dievaluasi) &mdash; pola yang sama dipakai
	 * {@link LampiranLainMahasiswa#checkLampiran}, membebaskan koneksi database secepat mungkin
	 * karena sisa method ini murni logika di memori (pesan dialog), tidak lagi butuh akses DB.</p>
	 *
	 * <h4>Efek samping &amp; nilai balik</h4>
	 * <p>Pada kegagalan validasi PERTAMA yang ditemukan (method berhenti pada pemeriksaan
	 * {@code == 0} pertama yang bernilai true, TIDAK mengumpulkan seluruh daftar kekurangan
	 * sekaligus), method menampilkan {@link MyMessageboxConfig#show(String, String, int, int)}
	 * berisi pesan "&lt;label butir&gt; harus di upload" ke pengguna (efek samping UI langsung,
	 * bukan hanya nilai balik) dan mengembalikan {@code false}. Bila SELURUH butir wajib sudah
	 * terpenuhi, mengembalikan {@code true} tanpa menampilkan apa pun. Urutan pemeriksaan (dan
	 * karenanya urutan pesan yang mungkin tampil bila beberapa butir sama-sama kurang) SELALU:
	 * foto &rarr; ijazah &rarr; nilai &rarr; ktp &rarr; lampiran 1..10.</p>
	 *
	 * <h4>Perbedaan dari {@link LampiranLainMahasiswa#checkLampiran}</h4>
	 * <p>Versi mahasiswa (file 9 paket ini) menerima parameter tambahan {@code EventListener}
	 * yang diteruskan ke {@code MyMessageboxConfig.show(...)} sebagai callback setelah dialog
	 * ditutup, dan membungkus SELURUH badan method dalam {@code try/catch} yang melakukan
	 * rollback eksplisit bila terjadi exception. Method versi calon mahasiswa ini TIDAK memiliki
	 * keduanya: tidak ada parameter callback, dan TIDAK ada blok {@code try/catch} pembungkus di
	 * sini &mdash; exception apa pun (mis. dari query Hibernate) akan menjalar keluar ke
	 * pemanggil apa adanya, ditangani (atau tidak) oleh lapisan Action yang memanggilnya.</p>
	 *
	 * @param biodataCalonMahasiswa baris pendaftaran calon mahasiswa yang berkasnya diperiksa
	 * @return {@code true} bila seluruh berkas wajib (sesuai konfigurasi aktif) sudah diunggah;
	 *         {@code false} bila ada yang kurang (pesan sudah ditampilkan ke pengguna)
	 * @throws Exception bila query Hibernate gagal (tidak ditangkap secara lokal)
	 */
	public static boolean checkLampiran(BiodataCalonMahasiswa biodataCalonMahasiswa) throws Exception {
		Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();

		int foto = (biodataCalonMahasiswa.getPaket() == null || biodataCalonMahasiswa.getPaket().getWajibUploadFoto())
				? (((Number) streamingSession.createCriteria(FotoBiodataCalonMahasiswa.class)
						.setProjection(Projections.rowCount())
						.add(Restrictions.eq("biodataCalonMahasiswa", biodataCalonMahasiswa.getId())).setMaxResults(1)
						.uniqueResult()).intValue())
				: 1;

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

		if (Common.getKonfigurasi("calon_mahasiswa_tidak_perlu_upload_ijazah", Konfigurasi.AKTIF).getNilai()
				.equals(Konfigurasi.TIDAK_AKTIF)) {
			ijazah = ((Number) streamingSession.createCriteria(LampiranLainBiodataCalonMahasiswa.class)
					.setProjection(Projections.rowCount())
					.add(Restrictions.eq("jenis", LampiranLainBiodataCalonMahasiswa.IJAZAH))
					.add(Restrictions.eq("biodataCalonMahasiswa", biodataCalonMahasiswa.getId())).setMaxResults(1)
					.uniqueResult()).intValue();
		}

		if (Common.getKonfigurasi("calon_mahasiswa_tidak_perlu_upload_nilai", Konfigurasi.AKTIF).getNilai()
				.equals(Konfigurasi.TIDAK_AKTIF)) {
			nilai = ((Number) streamingSession.createCriteria(LampiranLainBiodataCalonMahasiswa.class)
					.setProjection(Projections.rowCount())
					.add(Restrictions.eq("jenis", LampiranLainBiodataCalonMahasiswa.TRANSKRIP_NILAI))
					.add(Restrictions.eq("biodataCalonMahasiswa", biodataCalonMahasiswa.getId())).setMaxResults(1)
					.uniqueResult()).intValue();
		}

		if (Common.getKonfigurasi("calon_mahasiswa_tidak_perlu_upload_ktp", Konfigurasi.AKTIF).getNilai()
				.equals(Konfigurasi.TIDAK_AKTIF)) {
			ktp = ((Number) streamingSession.createCriteria(LampiranLainBiodataCalonMahasiswa.class)
					.setProjection(Projections.rowCount())
					.add(Restrictions.eq("jenis", LampiranLainBiodataCalonMahasiswa.KTP))
					.add(Restrictions.eq("biodataCalonMahasiswa", biodataCalonMahasiswa.getId())).setMaxResults(1)
					.uniqueResult()).intValue();
		}

		Konfigurasi konfigurasiUpload = Common.getKonfigurasi("calon_mahasiswa_upload_lampiran_1",
				Konfigurasi.TIDAK_AKTIF);
		String keteranganInfo1 = "";
		if (konfigurasiUpload.getNilai().equals(Konfigurasi.AKTIF)) {
			lampiran_1 = ((Number) streamingSession.createCriteria(LampiranLainBiodataCalonMahasiswa.class)
					.setProjection(Projections.rowCount())
					.add(Restrictions.eq("jenis", LampiranLainBiodataCalonMahasiswa.LAMPIRAN_1))
					.add(Restrictions.eq("biodataCalonMahasiswa", biodataCalonMahasiswa.getId())).setMaxResults(1)
					.uniqueResult()).intValue();
			keteranganInfo1 = konfigurasiUpload.getInfo1();
		}

		konfigurasiUpload = Common.getKonfigurasi("calon_mahasiswa_upload_lampiran_2", Konfigurasi.TIDAK_AKTIF);
		String keteranganInfo2 = "";
		if (konfigurasiUpload.getNilai().equals(Konfigurasi.AKTIF)) {
			lampiran_2 = ((Number) streamingSession.createCriteria(LampiranLainBiodataCalonMahasiswa.class)
					.setProjection(Projections.rowCount())
					.add(Restrictions.eq("jenis", LampiranLainBiodataCalonMahasiswa.LAMPIRAN_2))
					.add(Restrictions.eq("biodataCalonMahasiswa", biodataCalonMahasiswa.getId())).setMaxResults(1)
					.uniqueResult()).intValue();
			keteranganInfo2 = konfigurasiUpload.getInfo1();
		}

		konfigurasiUpload = Common.getKonfigurasi("calon_mahasiswa_upload_lampiran_3", Konfigurasi.TIDAK_AKTIF);
		String keteranganInfo3 = "";
		if (konfigurasiUpload.getNilai().equals(Konfigurasi.AKTIF)) {
			lampiran_3 = ((Number) streamingSession.createCriteria(LampiranLainBiodataCalonMahasiswa.class)
					.setProjection(Projections.rowCount())
					.add(Restrictions.eq("jenis", LampiranLainBiodataCalonMahasiswa.LAMPIRAN_3))
					.add(Restrictions.eq("biodataCalonMahasiswa", biodataCalonMahasiswa.getId())).setMaxResults(1)
					.uniqueResult()).intValue();
			keteranganInfo3 = konfigurasiUpload.getInfo1();
		}

		konfigurasiUpload = Common.getKonfigurasi("calon_mahasiswa_upload_lampiran_4", Konfigurasi.TIDAK_AKTIF);
		String keteranganInfo4 = "";
		if (konfigurasiUpload.getNilai().equals(Konfigurasi.AKTIF)) {
			lampiran_4 = ((Number) streamingSession.createCriteria(LampiranLainBiodataCalonMahasiswa.class)
					.setProjection(Projections.rowCount())
					.add(Restrictions.eq("jenis", LampiranLainBiodataCalonMahasiswa.LAMPIRAN_4))
					.add(Restrictions.eq("biodataCalonMahasiswa", biodataCalonMahasiswa.getId())).setMaxResults(1)
					.uniqueResult()).intValue();
			keteranganInfo4 = konfigurasiUpload.getInfo1();
		}

		konfigurasiUpload = Common.getKonfigurasi("calon_mahasiswa_upload_lampiran_5", Konfigurasi.TIDAK_AKTIF);
		String keteranganInfo5 = "";
		if (konfigurasiUpload.getNilai().equals(Konfigurasi.AKTIF)) {
			lampiran_5 = ((Number) streamingSession.createCriteria(LampiranLainBiodataCalonMahasiswa.class)
					.setProjection(Projections.rowCount())
					.add(Restrictions.eq("jenis", LampiranLainBiodataCalonMahasiswa.LAMPIRAN_5))
					.add(Restrictions.eq("biodataCalonMahasiswa", biodataCalonMahasiswa.getId())).setMaxResults(1)
					.uniqueResult()).intValue();
			keteranganInfo5 = konfigurasiUpload.getInfo1();
		}

		konfigurasiUpload = Common.getKonfigurasi("calon_mahasiswa_upload_lampiran_6", Konfigurasi.TIDAK_AKTIF);
		String keteranganInfo6 = "";
		if (konfigurasiUpload.getNilai().equals(Konfigurasi.AKTIF)) {
			lampiran_6 = ((Number) streamingSession.createCriteria(LampiranLainBiodataCalonMahasiswa.class)
					.setProjection(Projections.rowCount())
					.add(Restrictions.eq("jenis", LampiranLainBiodataCalonMahasiswa.LAMPIRAN_6))
					.add(Restrictions.eq("biodataCalonMahasiswa", biodataCalonMahasiswa.getId())).setMaxResults(1)
					.uniqueResult()).intValue();
			keteranganInfo6 = konfigurasiUpload.getInfo1();
		}

		konfigurasiUpload = Common.getKonfigurasi("calon_mahasiswa_upload_lampiran_7", Konfigurasi.TIDAK_AKTIF);
		String keteranganInfo7 = "";
		if (konfigurasiUpload.getNilai().equals(Konfigurasi.AKTIF)) {
			lampiran_7 = ((Number) streamingSession.createCriteria(LampiranLainBiodataCalonMahasiswa.class)
					.setProjection(Projections.rowCount())
					.add(Restrictions.eq("jenis", LampiranLainBiodataCalonMahasiswa.LAMPIRAN_7))
					.add(Restrictions.eq("biodataCalonMahasiswa", biodataCalonMahasiswa.getId())).setMaxResults(1)
					.uniqueResult()).intValue();
			keteranganInfo7 = konfigurasiUpload.getInfo1();
		}

		konfigurasiUpload = Common.getKonfigurasi("calon_mahasiswa_upload_lampiran_8", Konfigurasi.TIDAK_AKTIF);
		String keteranganInfo8 = "";
		if (konfigurasiUpload.getNilai().equals(Konfigurasi.AKTIF)) {
			lampiran_8 = ((Number) streamingSession.createCriteria(LampiranLainBiodataCalonMahasiswa.class)
					.setProjection(Projections.rowCount())
					.add(Restrictions.eq("jenis", LampiranLainBiodataCalonMahasiswa.LAMPIRAN_8))
					.add(Restrictions.eq("biodataCalonMahasiswa", biodataCalonMahasiswa.getId())).setMaxResults(1)
					.uniqueResult()).intValue();
			keteranganInfo8 = konfigurasiUpload.getInfo1();
		}

		konfigurasiUpload = Common.getKonfigurasi("calon_mahasiswa_upload_lampiran_9", Konfigurasi.TIDAK_AKTIF);
		String keteranganInfo9 = "";
		if (konfigurasiUpload.getNilai().equals(Konfigurasi.AKTIF)) {
			lampiran_9 = ((Number) streamingSession.createCriteria(LampiranLainBiodataCalonMahasiswa.class)
					.setProjection(Projections.rowCount())
					.add(Restrictions.eq("jenis", LampiranLainBiodataCalonMahasiswa.LAMPIRAN_9))
					.add(Restrictions.eq("biodataCalonMahasiswa", biodataCalonMahasiswa.getId())).setMaxResults(1)
					.uniqueResult()).intValue();
			keteranganInfo9 = konfigurasiUpload.getInfo1();
		}

		konfigurasiUpload = Common.getKonfigurasi("calon_mahasiswa_upload_lampiran_10", Konfigurasi.TIDAK_AKTIF);
		String keteranganInfo10 = "";
		if (konfigurasiUpload.getNilai().equals(Konfigurasi.AKTIF)) {
			lampiran_10 = ((Number) streamingSession.createCriteria(LampiranLainBiodataCalonMahasiswa.class)
					.setProjection(Projections.rowCount())
					.add(Restrictions.eq("jenis", LampiranLainBiodataCalonMahasiswa.LAMPIRAN_10))
					.add(Restrictions.eq("biodataCalonMahasiswa", biodataCalonMahasiswa.getId())).setMaxResults(1)
					.uniqueResult()).intValue();
			keteranganInfo10 = konfigurasiUpload.getInfo1();
		}

		StreamingHibernateUtil.getInstance().closeSession();

		if (foto == 0) {
			MyMessageboxConfig.show("Foto harus di upload", "Informasi", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (ijazah == 0) {
			MyMessageboxConfig.show("Ijazah/surat keterangan lulus harus di upload", "Informasi", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (nilai == 0) {
			MyMessageboxConfig.show("Transkrip nilai harus di upload", "Informasi", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (ktp == 0) {
			MyMessageboxConfig.show("KTP / Kartu Pelajar harus di upload", "Informasi", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (lampiran_1 == 0) {
			MyMessageboxConfig.show(keteranganInfo1 + " harus di upload", "Informasi", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (lampiran_2 == 0) {
			MyMessageboxConfig.show(keteranganInfo2 + " harus di upload", "Informasi", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (lampiran_3 == 0) {
			MyMessageboxConfig.show(keteranganInfo3 + " harus di upload", "Informasi", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (lampiran_4 == 0) {
			MyMessageboxConfig.show(keteranganInfo4 + " harus di upload", "Informasi", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (lampiran_5 == 0) {
			MyMessageboxConfig.show(keteranganInfo5 + " harus di upload", "Informasi", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (lampiran_6 == 0) {
			MyMessageboxConfig.show(keteranganInfo6 + " harus di upload", "Informasi", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (lampiran_7 == 0) {
			MyMessageboxConfig.show(keteranganInfo7 + " harus di upload", "Informasi", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (lampiran_8 == 0) {
			MyMessageboxConfig.show(keteranganInfo8 + " harus di upload", "Informasi", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (lampiran_9 == 0) {
			MyMessageboxConfig.show(keteranganInfo9 + " harus di upload", "Informasi", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (lampiran_10 == 0) {
			MyMessageboxConfig.show(keteranganInfo10 + " harus di upload", "Informasi", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
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
	 * Identitas (biasanya userid akun PMB) pihak yang terakhir mengunggah/mengubah baris ini.
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
	private Long biodataCalonMahasiswa;
	private Blob foto;
	private String jenis;
	private LampiranLainBiodataCalonMahasiswa copyDari;
	private String link;

	/** Konstruktor kosong wajib bagi entity JPA/Hibernate; field diisi lewat setter/reflection. */
	public LampiranLainBiodataCalonMahasiswa() {
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
	 * Nama berkas asli (termasuk ekstensi) sebagaimana diunggah calon mahasiswa.
	 *
	 * <p><b>Dua fallback berurutan (berbeda dari {@link LampiranPklMahasiswa#getNama()} yang
	 * hanya punya satu):</b> bila baris ini "salinan" ({@link #copyDari} != null), nama diambil
	 * dari baris sumber; JIKA TIDAK salinan tetapi {@link #getLink()} tidak kosong (lampiran
	 * berupa tautan eksternal, bukan berkas unggahan), field {@code nama} DITIMPA dengan literal
	 * {@code "Berupa link file"} &mdash; supaya UI menampilkan label yang bermakna alih-alih nama
	 * berkas yang mungkin tidak relevan/kosong untuk lampiran bertipe tautan.</p>
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
	 * Isi berkas biner baris ini. <b>Urutan prioritas berbeda dari
	 * {@link LampiranPklMahasiswa#getFoto()}:</b> {@code null} bila disimpan di Google Drive;
	 * JIKA TIDAK, field lokal {@code foto} diutamakan BILA TERISI (baris ini punya blob sendiri);
	 * hanya bila field lokal kosong barulah blob {@link #copyDari} dipakai sebagai fallback.
	 * Perbedaan urutan ini relevan untuk baris hasil migrasi (lihat javadoc kelas): baris yang
	 * pernah disalin blobnya sendiri (bukan sekadar menunjuk {@code copyDari}) akan tetap
	 * memakai blob miliknya sendiri, bukan blob sumbernya, walau {@code copyDari} masih terisi.
	 * Ditandai {@code @NotAudited} agar isi biner tidak digandakan ke tabel bayangan Envers.
	 *
	 * @return blob isi berkas, atau {@code null} bila disimpan di Google Drive
	 */
	@NotAudited
	public Blob getFoto() {
		return gdrive != null && !gdrive.trim().isEmpty() ? null : (foto != null ? foto : (copyDari == null ? null : copyDari.foto));
	}

	/**
	 * Menyetel id baris {@link ais.database.model.BiodataCalonMahasiswa} pemilik lampiran ini.
	 *
	 * @param biodataCalonMahasiswa id {@code BiodataCalonMahasiswa} pemilik lampiran
	 */
	public void setBiodataCalonMahasiswa(Long biodataCalonMahasiswa) {
		this.biodataCalonMahasiswa = biodataCalonMahasiswa;
	}

	/**
	 * Id baris {@link ais.database.model.BiodataCalonMahasiswa} pemilik lampiran ini. Lihat
	 * javadoc kelas mengenai verifikasi bahwa kelas ini adalah entity mandiri: field ini
	 * {@code Long} polos (bukan {@code @ManyToOne}), tetapi TETAP menjadi kunci kepemilikan
	 * satu-satunya untuk kelas ini (didaftarkan sebagai field relasi di
	 * {@code FileFotoLain.RELASI_MAP}).
	 *
	 * @return id {@code BiodataCalonMahasiswa} pemilik lampiran, bisa {@code null}
	 */
	@Column(name = "biodata_calon_mahasiswa", nullable = true)
	public Long getBiodataCalonMahasiswa() {
		return biodataCalonMahasiswa;
	}

	/**
	 * Nilai kolom {@code jenis} apa adanya. <b>Catatan:</b> berbeda dari
	 * {@link LampiranPklMahasiswa#getJenis()}, getter ini TIDAK menimpa nilai dengan konstanta
	 * default apa pun &mdash; kelas ini memang tidak punya satu {@code DEFAULT_JENIS} tunggal
	 * (satu {@code BiodataCalonMahasiswa} punya BANYAK baris lampiran dengan {@code jenis}
	 * berbeda-beda, lihat {@link #IJAZAH}, {@link #KTP}, dst), sehingga nilai kolom sesungguhnya
	 * harus selalu dipertahankan apa adanya.
	 *
	 * @return nilai field {@code jenis}, salah satu konstanta {@link #IJAZAH}/{@link #KTP}/dst
	 *         atau nilai bebas lain, bisa {@code null}
	 */
	public String getJenis() {
		return jenis;
	}

	/**
	 * Menyetel jenis dokumen yang dipenuhi lampiran ini.
	 *
	 * @param jenis nilai jenis, idealnya salah satu konstanta {@link #IJAZAH}/{@link #KTP}/dst
	 */
	public void setJenis(String jenis) {
		this.jenis = jenis;
	}

	/**
	 * Baris sumber bila lampiran ini "salinan" &mdash; berbagi nama/gdrive fisik milik baris lain
	 * (lihat catatan urutan prioritas berbeda pada {@link #getFoto()}). {@code @NotFound(IGNORE)}:
	 * bila baris sumber sudah terhapus, dikembalikan {@code null} alih-alih melempar exception.
	 *
	 * @return baris sumber salinan, atau {@code null} bila bukan salinan / sumbernya sudah tak ada
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@NotFound(action = NotFoundAction.IGNORE)
	@JoinColumn(name = "copy_dari", nullable = true)
	public LampiranLainBiodataCalonMahasiswa getCopyDari() {
		return copyDari;
	}

	/**
	 * Menandai baris ini sebagai salinan dari baris {@code copyDari} yang diberikan.
	 *
	 * @param copyDari baris sumber yang nama/gdrive-nya akan dibagikan
	 */
	public void setCopyDari(LampiranLainBiodataCalonMahasiswa copyDari) {
		this.copyDari = copyDari;
	}

	private String gdrive;
	private String gdriveUsername;

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
	 * Implementasi kontrak {@link FileFotoLain#ambilRef()}: mengembalikan
	 * {@link #getBiodataCalonMahasiswa()} sebagai "acuan" generik dipakai mesin
	 * {@code FileFotoLain.ambil()}/{@code hapusAtauUpdate()}.
	 *
	 * @return id {@code BiodataCalonMahasiswa} pemilik lampiran
	 */
	@Override
	public Long ambilRef() {
		// TODO Auto-generated method stub
		return biodataCalonMahasiswa;
	}

	/**
	 * Implementasi kontrak {@link FileFotoLain#ambilClazz()}. Berbeda dari
	 * {@link LampiranPklMahasiswa#ambilClazz()} yang mengembalikan {@code this.getClass()}
	 * (menangani proxy Hibernate dengan benar), method ini mengembalikan literal
	 * {@code LampiranLainBiodataCalonMahasiswa.class} &mdash; pada entity yang dimuat lewat proxy
	 * Hibernate, nilai ini tetap benar secara kebetulan (subclass proxy sama-sama merujuk
	 * assignable ke class ini), namun tidak mengikuti pola aman umum di kelas sejenis lainnya.
	 *
	 * @return selalu literal {@code LampiranLainBiodataCalonMahasiswa.class}
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClazz() {
		// TODO Auto-generated method stub
		return LampiranLainBiodataCalonMahasiswa.class;
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
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/file/LampiranLainBiodataCalonMahasiswa.java:519");
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
