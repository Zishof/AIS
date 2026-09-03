package ais.database.model.sekolah;
// Generated 10 Okt 18 12:46:07 by Hibernate Tools 5.2.3.Final

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;
import ais.database.model.PerguruanTinggi;

/**
 * <h2>Master jenis hukuman/sanksi tata tertib siswa (tabel {@code sekolah.hukuman})</h2>
 *
 * <p><b>Peran.</b> Entity ini adalah <b>daftar master</b> bentuk hukuman/sanksi yang boleh
 * dijatuhkan kepada siswa atas pelanggaran tata tertib — misalnya "Teguran lisan",
 * "Panggilan orang tua", "Skorsing 3 hari", "Membersihkan halaman sekolah". Isinya
 * <b>bukan</b> catatan hukuman seorang siswa, melainkan katalog pilihan yang dipakai
 * layar entri pelanggaran. Satu baris = satu jenis sanksi.</p>
 *
 * <p><b>Posisi dalam rantai modul tata tertib.</b> Modul ini terdiri dari empat lapis yang
 * saling terkait (nama kelas ditulis biasa, bukan {@code @link}, karena sebagian belum
 * didokumentasikan pada saat berkas ini ditulis):</p>
 * <ol>
 *   <li><b>{@code Pelanggaran}</b> — master jenis pelanggaran (apa yang dilanggar siswa).
 *       Strukturnya kembar persis dengan kelas ini.</li>
 *   <li><b>{@code Hukuman}</b> (kelas ini) — master jenis sanksi (apa akibatnya).</li>
 *   <li><b>{@code PelanggaranDanHukuman}</b> — <b>paket/kategori</b> yang MENGIKAT sekumpulan
 *       {@code Pelanggaran} dengan sekumpulan {@code Hukuman} lewat dua tabel silang
 *       {@code sekolah.pelanggaran_dan_hukuman_has_pelanggaran} dan
 *       {@code sekolah.pelanggaran_dan_hukuman_has_hukuman}. Meski namanya mengandung kata
 *       "dan", entity itu <b>bukan</b> catatan transaksi per siswa melainkan tetap master:
 *       ia mendefinisikan "untuk pelanggaran kelompok X, sanksi yang boleh dipilih adalah
 *       himpunan Y".</li>
 *   <li><b>{@code PelanggaranSiswa}</b> — barulah ini <b>transaksi</b>: satu kejadian
 *       pelanggaran oleh satu siswa pada satu waktu. Menunjuk satu
 *       {@code PelanggaranDanHukuman} (kolom {@code pelanggaran_dan_hukuman}, {@code NOT
 *       NULL}) dan menyimpan sanksi yang benar-benar dijatuhkan di tabel silang
 *       {@code sekolah.pelanggaran_siswa_has_hukuman} — yaitu himpunan bagian dari
 *       {@code Hukuman} yang ditawarkan paket di lapis 3.</li>
 * </ol>
 * <p>Konsekuensi praktisnya: baris di sini <b>dirujuk dari dua tabel silang berbeda</b>
 * ({@code pelanggaran_dan_hukuman_has_hukuman} dan {@code pelanggaran_siswa_has_hukuman}),
 * dan relasi itu dideklarasikan <b>hanya di sisi seberang</b> — kelas ini sama sekali tidak
 * punya koleksi balik. Menghapus satu baris hukuman berarti memutus rujukan pada catatan
 * pelanggaran siswa yang sudah terlanjur menyimpannya (riwayat disipliner historis), tanpa
 * ada peringatan apa pun dari sisi entity ini.</p>
 *
 * <h3>Kolom bisnis</h3>
 * <ul>
 *   <li>{@code nama} — label sanksi, satu-satunya kolom wajib ({@code nullable=false}) dan
 *       satu-satunya yang divalidasi layar. Juga menjadi kunci pengurutan efektif (lihat
 *       "Pengurutan" di bawah).</li>
 *   <li>{@code poin} — <b>bobot pengurangan poin</b> tata tertib siswa. Bukan sekadar
 *       metadata: nilai ini dijumlahkan menjadi total poin pelanggaran siswa di
 *       {@code LaporanPelanggaranSiswa} dan di rapor ({@code LaporanRaporSiswa}), serta
 *       ditampilkan di label pilihan sanksi ({@code PelanggaranSiswaAction}) sebagai
 *       "pengurangan poin : ..." bila lebih besar dari 0,1.</li>
 *   <li>{@code keterangan} — teks bebas penjelas.</li>
 *   <li>{@code aktif} — penanda baris masih dipakai. Perhatikan bahwa penanda ini
 *       <b>tidak</b> disaring di query manapun yang ditemukan (baik layar master, kombo
 *       pilihan, maupun laporan); efeknya sejauh ini murni informatif.</li>
 *   <li>{@code sekolah} / {@code yayasan} / {@code perguruanTinggi} — cakupan kepemilikan
 *       data pada instalasi multi-tenant.</li>
 * </ul>
 *
 * <h3>Pengelompokan method</h3>
 * <ul>
 *   <li><b>Jejak audit manual:</b> {@link #getOleh()}/{@link #setOleh(String)},
 *       {@link #getOlehId()}/{@link #setOlehId(String)},
 *       {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, dan callback
 *       {@code onUpdate()}.</li>
 *   <li><b>Identitas:</b> {@link #getId()}/{@link #setId(Long)} dan dua konstruktor.</li>
 *   <li><b>Cakupan kepemilikan (relasi):</b> {@link #getSekolah()}, {@link #getYayasan()},
 *       {@link #getPerguruanTinggi()} beserta setter-nya — ketiganya punya perilaku
 *       tambahan yang <b>tidak</b> sekadar mengembalikan field (lihat "Getter destruktif").</li>
 *   <li><b>Data bisnis murni:</b> {@link #getNama()}, {@link #getKeterangan()},
 *       {@link #getPoin()}, {@link #getAktif()} dan setter-nya.</li>
 * </ul>
 *
 * <h3>Hal non-obvious yang wajib diketahui</h3>
 *
 * <p><b>1. Javadoc bawaan generator salah.</b> Komentar asli hasil hbm2java berbunyi
 * "JenisGuru generated by hbm2java" — sisa salin-tempel dari entity lain. Nama itu tidak
 * ada hubungannya dengan isi kelas ini; jangan dipakai sebagai petunjuk domain.</p>
 *
 * <p><b>2. Deklarasi ulang {@code id}/{@code oleh}/{@code olehId}/{@code tanggal_dirubah}
 * BUKAN bug.</b> {@link ais.database.model.GeneralValueObject} adalah POJO abstrak biasa —
 * <b>bukan</b> {@code @Entity} dan <b>bukan</b> {@code @MappedSuperclass} — sehingga
 * Hibernate tidak memetakan properti milik induk sama sekali. Setiap subclass yang butuh
 * kolom-kolom itu <b>harus</b> mendeklarasikannya ulang. Efek sampingnya: field {@code nama}
 * dan {@code keterangan} di kelas ini <b>membayangi</b> (shadow) field bernama sama di induk,
 * sedangkan {@code nomorUrut} dan {@code nim} milik induk tetap ada tapi <b>selalu
 * {@code null}</b> untuk entity ini karena tidak pernah dipetakan maupun diisi.</p>
 *
 * <p><b>3. Pengurutan &amp; risiko penciutan senyap {@code TreeSet}.</b> Kelas ini tidak
 * meng-override {@code compareTo}, jadi berlaku implementasi induk yang mencoba berturut-turut
 * {@code nomorUrut} → {@code nim} → {@code nama} → {@code keterangan}. Karena dua kunci
 * pertama selalu {@code null} di sini (lihat butir 2), <b>pengurutan efektif selalu jatuh ke
 * {@code nama}</b>. Konsekuensinya: {@code compareTo} mengembalikan {@code 0} untuk dua baris
 * hukuman ber-{@code nama} sama walaupun {@code id}-nya berbeda. {@code PelanggaranSiswaAction}
 * membungkus koleksi sanksi seorang siswa ke dalam {@code new TreeSet<Hukuman>(...)} sebelum
 * menampilkannya di grid, sehingga <b>dua sanksi berbeda dengan nama identik akan menciut
 * menjadi satu baris di layar</b> — satu sanksi lenyap dari tampilan riwayat tanpa jejak.
 * Ini varian pola yang sama seperti yang berulang kali ditemukan di keluarga
 * {@code KelompokParameterTambahan*}. Tidak ada batasan {@code unique} pada kolom
 * {@code nama}, jadi kondisi ini bisa terjadi dan sepenuhnya di tangan operator.</p>
 *
 * <p><b>4. Getter destruktif (write-back diam-diam).</b> Pemetaan memakai <i>property
 * access</i> ({@code @Id} ada di getter) dengan {@code dynamicUpdate=true}, artinya Hibernate
 * membaca state entity <b>lewat getter</b> saat flush. Dua getter di sini punya efek samping
 * yang karenanya bisa berubah menjadi {@code UPDATE} nyata ke database — plus revisi Envers
 * palsu — hanya karena barisnya kebetulan dibaca dalam sesi aktif:</p>
 * <ul>
 *   <li>{@link #getYayasan()} <b>menimpa</b> {@code yayasan} dengan
 *       {@code getSekolah().getYayasan()} setiap kali dipanggil, sehingga nilai
 *       {@code yayasan} yang disimpan operator tidak pernah bertahan bila {@code sekolah}
 *       terisi.</li>
 *   <li>{@link #getPerguruanTinggi()} <b>mengisi</b> {@code perguruanTinggi} dari
 *       {@code PerguruanTinggiUtil.getPerguruanTinggi()} bila masih kosong.</li>
 * </ul>
 *
 * <p><b>5. Pra-muat cache saat startup.</b> {@code Hukuman.class} terdaftar di daftar
 * {@code initClasses(...)} milik {@code ais.common.InitData}, sehingga seluruh isi tabel ini
 * dibaca sekali ke cache memori ({@code MemoryDbUtil}/{@code ConstantValues}) pada saat
 * aplikasi dinyalakan, di luar konteks request pengguna.</p>
 *
 * @see ais.database.model.GeneralValueObject
 * @see ais.database.model.PerguruanTinggi
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(name = "hukuman", schema = "sekolah")
public class Hukuman extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilai ini <b>identik</b> dengan milik
	 * {@code PelanggaranDanHukuman} dan {@code Pelanggaran} — sisa salin-tempel antar entity
	 * satu modul; tidak berbahaya karena serialisasi Java hanya membandingkan nilai ini
	 * antar versi kelas yang sama.
	 */
	private static final long serialVersionUID = -7490758846785025664L;

	/** Kunci utama, dibangkitkan database ({@code IDENTITY}). Lihat {@link #getId()}. */
	private Long id;

	/**
	 * Nama pengguna terakhir yang mengubah baris ini; diisi otomatis oleh
	 * {@code AuditTimestampInterceptor}. Lihat {@link #getOleh()}.
	 */
	private String oleh;

	/**
	 * ID pengguna terakhir yang mengubah baris ini; pendamping {@link #oleh}. Lihat
	 * {@link #getOlehId()}.
	 */
	private String olehId;

	/**
	 * Mengembalikan ID pengguna yang terakhir mengubah baris ini.
	 *
	 * @return ID pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel ID pengguna pengubah terakhir, dengan penjagaan "tolak nilai kosong".
	 *
	 * <p><b>Perhatikan:</b> nilai {@code null} maupun string kosong/spasi <b>diabaikan
	 * diam-diam</b> (method langsung {@code return} tanpa menulis apa pun). Jadi jejak audit
	 * yang sudah pernah terisi <b>tidak bisa dikosongkan kembali</b> lewat setter ini —
	 * disengaja, agar interceptor audit tidak menghapus jejak lama ketika konteks pengguna
	 * tidak tersedia (mis. proses batch/startup tanpa sesi login).</p>
	 *
	 * @param olehId ID pengguna pengubah; diabaikan bila {@code null} atau kosong.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir, dengan penjagaan "tolak nilai kosong" yang
	 * sama persis dengan {@link #setOlehId(String)} — nilai {@code null}/kosong diabaikan
	 * diam-diam sehingga jejak audit lama tidak terhapus.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null} atau kosong.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dijalankan otomatis oleh provider persistence tepat
	 * sebelum pernyataan {@code UPDATE} baris ini dikirim ke database.
	 *
	 * <p>Mendelegasikan seluruh pekerjaan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)}, yang mengisi
	 * {@link #oleh}, {@link #olehId}, dan {@link #tanggal_dirubah} dari konteks pengguna yang
	 * sedang aktif.</p>
	 *
	 * <p><b>Jangan dipanggil manual.</b> Method ini {@code protected} dan sepenuhnya milik
	 * lifecycle Hibernate/JPA. Perhatikan juga bahwa hanya {@code UPDATE} yang tercakup —
	 * tidak ada {@code @PrePersist}, jadi pada {@code INSERT} jejak {@code oleh}/{@code olehId}
	 * hanya terisi bila pemanggil (atau interceptor sesi) mengisinya sendiri.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Waktu perubahan terakhir. Diberi nilai awal "sekarang" ({@code WaktuUtil.getDate()})
	 * pada saat objek dibuat, sehingga baris baru selalu punya cap waktu meski tidak ada
	 * {@code @PrePersist}. Lihat {@link #getTanggal_dirubah()}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel cap waktu perubahan terakhir. Umumnya dipanggil oleh
	 * {@code AuditTimestampInterceptor}, bukan oleh kode aplikasi.
	 *
	 * @param tanggal_dirubah cap waktu perubahan; boleh {@code null} (tidak ada penjagaan).
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan cap waktu perubahan terakhir, dipetakan sebagai {@code TIMESTAMP}.
	 *
	 * @return cap waktu perubahan terakhir; tidak pernah {@code null} untuk objek yang baru
	 *         dibuat di memori, tetapi bisa {@code null} bila kolomnya kosong di database.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Sekolah pemilik baris ini (cakupan tenant paling sempit). Lihat {@link #getSekolah()}. */
	private Sekolah sekolah;

	/**
	 * Yayasan pemilik baris ini. <b>Bukan</b> nilai independen: {@link #getYayasan()} selalu
	 * menurunkannya kembali dari {@link #sekolah} bila sekolah terisi.
	 */
	private Yayasan yayasan;

	/** Keterangan bebas penjelas sanksi. Lihat {@link #getKeterangan()}. */
	private String keterangan;

	/** Label jenis hukuman/sanksi; satu-satunya kolom wajib. Lihat {@link #getNama()}. */
	private String nama;

	/**
	 * Bobot pengurangan poin tata tertib akibat sanksi ini. Lihat {@link #getPoin()} untuk
	 * perilaku nilai bawaan dan daftar pemakainya.
	 */
	private Double poin;

	/** Penanda baris masih dipakai. Lihat {@link #getAktif()}. */
	private Boolean aktif;

	/**
	 * Perguruan tinggi/institusi pemilik baris. Diisi otomatis oleh
	 * {@link #getPerguruanTinggi()} bila masih kosong.
	 */
	private PerguruanTinggi perguruanTinggi;

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate/JPA untuk membuat instance saat memuat
	 * baris dari database. Dipakai juga oleh layar "Tambah Hukuman"
	 * ({@code HukumanAction.onAdd}) untuk menyiapkan formulir kosong.
	 */
	public Hukuman() {
	}

	/**
	 * Konstruktor ringkas berisi kolom wajib saja ({@code id} + {@code nama}), warisan
	 * template hbm2java.
	 *
	 * <p><b>Catatan:</b> tidak ditemukan pemanggil di dalam basis kode — kolom {@code id}
	 * dipetakan {@code insertable=false} dan dibangkitkan database, sehingga menyetelnya
	 * manual di konstruktor tidak berpengaruh pada {@code INSERT}.</p>
	 *
	 * @param id   kunci utama yang sudah diketahui.
	 * @param nama label jenis hukuman.
	 */
	public Hukuman(long id, String nama) {
		this.id = id;
		this.nama = nama;
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Kolom dipetakan {@code insertable=false} dan dibangkitkan database
	 * ({@code IDENTITY}), jadi nilainya baru terisi setelah baris benar-benar tersimpan.
	 * {@code HukumanAction} memakai {@code getId() == null} sebagai pembeda mode
	 * "Tambah" vs "Ubah" pada judul dialog.</p>
	 *
	 * @return kunci utama, atau {@code null} bila entity belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama secara manual.
	 *
	 * @param id kunci utama.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan sekolah pemilik baris ini, setelah proxy lazy-nya diselesaikan.
	 *
	 * <p><b>Cara kerja.</b> Memanggil {@code check(sekolah)} milik
	 * {@link ais.database.model.GeneralValueObject} yang menyelesaikan proxy Hibernate yang
	 * mungkin sudah <i>detached</i> (memuat ulang lewat identifier bila perlu), sehingga
	 * pemanggil menerima entity asli dan tidak meledak dengan
	 * {@code LazyInitializationException}. Hasilnya ditulis balik ke field, jadi resolusi
	 * hanya terjadi sekali per instance.</p>
	 *
	 * <p><b>Dipanggil dari.</b> Renderer grid {@code HukumanAction.HukumanRenderer} (kolom
	 * "Sekolah"), dialog Ubah ({@code Common.pilihSekolah}), dan secara tidak langsung oleh
	 * {@link #getYayasan()}.</p>
	 *
	 * @return sekolah pemilik, atau {@code null} bila baris berlaku lintas sekolah.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id")
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return this.sekolah;
	}

	/**
	 * Menyetel sekolah pemilik baris ini.
	 *
	 * <p><b>Penjagaan penting:</b> objek {@code Sekolah} yang belum tersimpan (ber-{@code id}
	 * {@code null}) <b>dinormalisasi menjadi {@code null}</b>, bukan disimpan apa adanya.
	 * Ini mencegah {@code CascadeType.PERSIST} tanpa sengaja membuat baris {@code Sekolah}
	 * baru hanya karena combobox mengembalikan objek kosong. Efek sampingnya: pemanggil yang
	 * mengira sudah menyetel sekolah bisa mendapati nilainya hilang tanpa pesan kesalahan.</p>
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau objek tanpa {@code id} disimpan
	 *                sebagai {@code null}.
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah == null || sekolah.getId() == null ? null : sekolah;
	}

	/**
	 * Mengembalikan yayasan pemilik baris ini — <b>getter dengan efek samping</b>.
	 *
	 * <p><b>Cara kerja.</b> Bukan pembacaan murni: method ini lebih dulu memanggil
	 * {@link #getSekolah()}, dan bila sekolah terisi maka field {@link #yayasan}
	 * <b>ditimpa</b> dengan {@code sekolah.getYayasan()}. Barulah hasilnya dilewatkan
	 * {@code check(...)} untuk resolusi proxy lazy.</p>
	 *
	 * <p><b>Konsekuensi yang mudah terlewat.</b> Karena pemetaan memakai <i>property
	 * access</i> dengan {@code dynamicUpdate=true}, Hibernate membaca state lewat getter saat
	 * flush. Bila baris ini kebetulan berada dalam sesi aktif dan yayasan tersimpan berbeda
	 * dari yayasan milik sekolahnya, sekadar <b>membaca</b> baris sudah cukup memicu
	 * {@code UPDATE} nyata plus revisi Envers palsu. Nilai yayasan yang disetel manual lewat
	 * {@link #setYayasan(Yayasan)} praktis tidak pernah bertahan selama {@code sekolah}
	 * terisi — kolom {@code yayasan_id} efektif hanya turunan (denormalisasi) dari
	 * {@code sekolah_id}. Pola getter destruktif yang sama dijumpai berulang di banyak
	 * subclass {@link ais.database.model.GeneralValueObject}.</p>
	 *
	 * @return yayasan pemilik (turunan dari sekolah bila sekolah terisi), atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan_id")
	public Yayasan getYayasan() {
		sekolah = getSekolah();
		if (sekolah != null) {
			yayasan = sekolah.getYayasan();
		}
		yayasan = check(yayasan);
		return this.yayasan;
	}

	/**
	 * Menyetel yayasan pemilik baris ini, dengan normalisasi "objek tanpa {@code id} menjadi
	 * {@code null}" yang sama seperti {@link #setSekolah(Sekolah)}.
	 *
	 * <p><b>Perhatikan:</b> nilai yang disetel di sini akan ditimpa lagi oleh
	 * {@link #getYayasan()} bila {@link #sekolah} terisi — lihat penjelasan di getter.</p>
	 *
	 * @param yayasan yayasan pemilik; {@code null} atau objek tanpa {@code id} disimpan
	 *                sebagai {@code null}.
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan == null || yayasan.getId() == null ? null : yayasan;
	}

	/**
	 * Mengembalikan keterangan bebas penjelas sanksi.
	 *
	 * <p>Berbeda dengan sebagian besar subclass {@link ais.database.model.GeneralValueObject}
	 * yang membalik kontrak {@code keterangan} milik induk, di sini kolom {@code keterangan}
	 * benar-benar dipetakan ke database dan dipakai apa adanya: ditampilkan sebagai kolom
	 * grid di {@code HukumanAction} dan diisi lewat {@code Textbox} tiga baris di dialog
	 * Tambah/Ubah.</p>
	 *
	 * @return keterangan sanksi, atau {@code null} bila kosong.
	 */
	@Column(name = "keterangan")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas penjelas sanksi.
	 *
	 * @param keterangan teks keterangan; boleh {@code null}.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan label jenis hukuman/sanksi.
	 *
	 * <p><b>Peran ganda.</b> Selain sebagai label tampilan (grid master, checkbox pilihan
	 * sanksi di layar Pelanggaran Siswa, kolom "hukumans" di rapor), nilai ini juga menjadi
	 * <b>kunci pengurutan efektif</b> entity ini karena {@code compareTo} milik induk jatuh
	 * ke {@code nama} — lihat catatan "penciutan senyap {@code TreeSet}" pada Javadoc kelas.</p>
	 *
	 * @return label jenis hukuman; secara skema {@code NOT NULL}, tetapi baris warisan
	 *         teoretis bisa mengembalikan {@code null} sehingga pemanggil tetap perlu
	 *         berjaga-jaga.
	 */
	@Column(name = "nama", nullable = false)
	public String getNama() {
		return this.nama;
	}

	/**
	 * Menyetel label jenis hukuman/sanksi.
	 *
	 * <p>Validasi "harus diisi" dilakukan di lapis UI ({@code HukumanAction.onSave}), bukan di
	 * sini; setter ini menerima nilai apa pun termasuk {@code null}.</p>
	 *
	 * @param nama label jenis hukuman.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan penanda apakah jenis hukuman ini masih aktif dipakai, dengan bawaan
	 * {@code true}.
	 *
	 * <p><b>Perilaku bawaan.</b> Bila field masih {@code null} (baris lama sebelum kolom ini
	 * ada, atau baris baru yang belum pernah dicentang), method mengembalikan {@code true} —
	 * jadi "belum diisi" diperlakukan sebagai "aktif". Nilai {@code null} itu sendiri
	 * <b>tidak</b> ditulis balik ke field, sehingga getter ini aman dari efek samping
	 * write-back.</p>
	 *
	 * <p><b>Cakupan pemakaian sebenarnya.</b> Penanda ini hanya dipakai untuk mengisi
	 * checkbox "Aktif" di grid master {@code HukumanAction} (yang menyimpan perubahannya
	 * langsung via {@code Common.refreshSaveOrUpdate}). Tidak ada satu pun query yang
	 * menyaring baris berdasarkan kolom ini — termasuk kombo pilihan sanksi di layar
	 * Pelanggaran Siswa dan penjumlahan poin di laporan. Artinya <b>menonaktifkan sebuah
	 * jenis hukuman tidak menyembunyikannya dari mana pun</b>; efeknya murni informatif.</p>
	 *
	 * @return {@code true} bila aktif atau belum pernah diisi; {@code false} hanya bila
	 *         eksplisit dinonaktifkan.
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel penanda aktif/tidaknya jenis hukuman ini.
	 *
	 * @param aktif {@code true} aktif, {@code false} nonaktif, {@code null} diperlakukan
	 *              sebagai aktif oleh {@link #getAktif()}.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan bobot pengurangan poin tata tertib untuk sanksi ini, dengan bawaan
	 * {@code 0.0}.
	 *
	 * <p><b>Perilaku bawaan.</b> Field {@code null} dikembalikan sebagai {@code 0.0} tanpa
	 * ditulis balik ke field — sehingga pemanggil aman melakukan aritmetika langsung
	 * ({@code point += hukuman.getPoin()}) tanpa memeriksa {@code null}, dan getter ini tidak
	 * memicu {@code UPDATE} tak terduga.</p>
	 *
	 * <p><b>Dipanggil dari (semua konsumen nyata bobot ini).</b></p>
	 * <ul>
	 *   <li>{@code HukumanAction} — kolom "Poin" pada grid master dan isian
	 *       {@code MyDoublebox} pada dialog Tambah/Ubah.</li>
	 *   <li>{@code PelanggaranSiswaAction.loadHukuman} — label checkbox pilihan sanksi
	 *       ditambahi ", pengurangan poin : ..." hanya bila nilainya {@code > 0.1}; ambang
	 *       ini sekaligus berfungsi sebagai penjagaan terhadap galat pembulatan
	 *       {@code double}, tetapi juga berarti sanksi dengan bobot kecil (mis. 0,05) tidak
	 *       pernah menampilkan poinnya di layar meski tetap dijumlahkan di laporan.</li>
	 *   <li>{@code LaporanPelanggaranSiswa} — dua tempat penjumlahan total poin pelanggaran
	 *       per siswa.</li>
	 *   <li>{@code LaporanRaporSiswa} — bobot per sanksi dan subtotal per kejadian pada rapor
	 *       siswa.</li>
	 *   <li>{@code PelanggaranMahasiswaAction} — layar padanan versi perguruan tinggi.</li>
	 * </ul>
	 *
	 * @return bobot pengurangan poin; {@code 0.0} bila belum pernah diisi.
	 */
	public Double getPoin() {
		return poin == null ? 0.0 : poin;
	}

	/**
	 * Menyetel bobot pengurangan poin untuk sanksi ini.
	 *
	 * <p>Tidak ada validasi rentang: nilai negatif maupun sangat besar diterima apa adanya
	 * dan akan ikut terjumlah di laporan poin siswa.</p>
	 *
	 * @param poin bobot pengurangan poin; {@code null} diperlakukan sebagai {@code 0.0} oleh
	 *             {@link #getPoin()}.
	 */
	public void setPoin(Double poin) {
		this.poin = poin;
	}

	/**
	 * Mengembalikan perguruan tinggi/institusi pemilik baris ini — <b>getter dengan efek
	 * samping</b>.
	 *
	 * <p><b>Cara kerja.</b> Pertama menyelesaikan proxy lazy lewat {@code check(...)}. Bila
	 * hasilnya masih {@code null}, field diisi dari
	 * {@code ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi()} —
	 * institusi tunggal yang sedang aktif pada instalasi ini.</p>
	 *
	 * <p><b>Efek samping.</b> Sama seperti {@link #getYayasan()}: karena pemetaan memakai
	 * <i>property access</i> dengan {@code dynamicUpdate=true}, pengisian otomatis ini bisa
	 * berubah menjadi {@code UPDATE} nyata (plus revisi Envers) untuk baris lama yang
	 * kolom {@code perguruan_tinggi}-nya masih kosong, cukup dengan membacanya di dalam sesi
	 * aktif. Berbeda dari {@link #getYayasan()}, isian ini bersifat <i>self-healing</i>
	 * (hanya mengisi yang kosong, tidak menimpa nilai yang sudah ada).</p>
	 *
	 * <p><b>Penanganan galat.</b> Kegagalan {@code PerguruanTinggiUtil} (mis. dipanggil di
	 * luar konteks request, saat pra-muat startup) ditelan dan hanya dicatat ke
	 * {@code ErrorAuditUtil}; method tetap mengembalikan {@code null} alih-alih melempar,
	 * agar pembacaan entity tidak pernah gagal karena konteks institusi belum siap.</p>
	 *
	 * @return perguruan tinggi pemilik; {@code null} bila belum terisi dan resolusi otomatis
	 *         gagal.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "perguruan_tinggi")
	public PerguruanTinggi getPerguruanTinggi() {
		perguruanTinggi = check(perguruanTinggi);
		try {
			if (perguruanTinggi == null) {
				perguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/Hukuman.java:175");
		}
		return perguruanTinggi;
	}

	/**
	 * Menyetel perguruan tinggi/institusi pemilik baris ini.
	 *
	 * <p>Berbeda dari {@link #setSekolah(Sekolah)}/{@link #setYayasan(Yayasan)}, setter ini
	 * <b>tidak</b> menormalisasi objek tanpa {@code id} menjadi {@code null}. Dipanggil
	 * eksplisit oleh {@code HukumanAction.onSave} dengan institusi aktif hasil
	 * {@code PerguruanTinggiUtil}.</p>
	 *
	 * @param perguruanTinggi institusi pemilik; boleh {@code null}.
	 */
	public void setPerguruanTinggi(PerguruanTinggi perguruanTinggi) {
		this.perguruanTinggi = perguruanTinggi;
	}
}
