package ais.database.model.rab;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

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




import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;



/**
 * Entity JPA/Hibernate untuk tabel {@code rab.informasi_rab_komentar} — satu komentar atas satu
 * {@link InformasiRab} (pengumuman/berita RAB).
 *
 * <p>
 * <b>Bukan infrastruktur komentar polymorphic generik.</b> Berbeda dari
 * {@code ais.database.model.employ.KomunikasiPegawai} (didokumentasikan batch 94 — thread komentar
 * lintas-entity via pasangan jenis+id polymorphic), kelas ini HANYA bisa berelasi dengan
 * {@link InformasiRab} lewat satu {@code ManyToOne} langsung ({@link #informasiRab}); tidak ada
 * kolom "jenis target" atau id polymorphic. Satu baris {@code InformasiRabKomentar} = satu komentar
 * untuk satu {@link InformasiRab}, titik.
 * </p>
 *
 * <p>
 * <b>Bentuk data menunjukkan komentator BUKAN pengguna internal ber-akun.</b> Field yang tersedia —
 * {@link #nama}, {@link #alamat}, {@link #kontak}, {@link #email} — adalah data kontak bebas-teks
 * yang diketik langsung oleh penulis komentar, bukan referensi ke entity pengguna/pegawai
 * bersistem-akun (tidak ada FK ke user/pegawai). Ini konsisten dengan sifat {@link InformasiRab}
 * sebagai papan pengumuman yang dibaca lewat feed REST publik-ke-tenant (lihat javadoc
 * {@link InformasiRab}); siapa pun yang berhasil mengirim komentar (jalur pembuatan baris baru
 * TIDAK ditemukan di action ZK manapun dalam paket ini — kemungkinan besar berasal dari sisi klien
 * portal/aplikasi yang mengonsumsi REST feed, di luar cakupan paket model ini) hanya perlu mengetik
 * identitas kontaknya sendiri, tanpa login.
 * </p>
 *
 * <p>
 * <b>Siapa bisa membaca:</b> dari kode yang ditemukan, komentar dibaca lewat dua jalur:
 * {@link ais.action.master.rab.helper.InformasiRabPunyaKomentarHelper} (grid admin ZK, login-gated,
 * hanya memfilter berdasarkan {@link #informasiRab} yang sedang dibuka — tanpa filter satuan kerja
 * tambahan karena satu {@link InformasiRab} sudah menentukan tenant pemiliknya) dan
 * {@code ais.action.master.resources.WorkspaceResource#daftarInformasiRabKomentar} (REST, filter
 * hanya berdasarkan {@code item} = id {@link InformasiRab}, TANPA verifikasi bahwa
 * {@link InformasiRab} tersebut milik satuan kerja pengguna yang mengautentikasi permintaan —
 * pengguna satuan kerja mana pun yang lolos login bisa membaca komentar, termasuk
 * {@link #nama}/{@link #kontak}/{@link #email} pribadi penulis, untuk id {@link InformasiRab} MILIK
 * SATUAN KERJA LAIN sekalipun. Pola "filter tenant lemah/hilang" ini sudah tercatat berulang di
 * paket ini; dicatat di sini untuk kelengkapan konteks komentar, bukan sebagai eskalasi baru karena
 * berada di luar tiga berkas yang menjadi tanggung jawab dokumentasi sesi ini).
 * </p>
 *
 * @see InformasiRab
 * @see ais.action.master.rab.helper.InformasiRabPunyaKomentarHelper
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "rab", name = "informasi_rab_komentar")



public class InformasiRabKomentar extends GeneralValueObject {

	/**
	 * Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable}. Nilai ini identik
	 * dengan entity lain hasil template hbm2java yang sama di paket {@code ais.database.model.rab} —
	 * bukan kesalahan salin-tempel yang perlu diperbaiki.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key auto-increment (identitas baris komentar). */
	private Long id;
	/** Nama tampilan pembuat/pengubah terakhir (audit sistem, BUKAN nama penulis komentar — lihat {@link #nama}). */
	private String oleh;
	/** Id pengguna pembuat/pengubah terakhir — pasangan shadow audit dari {@link #oleh} (pola sama seperti {@link InformasiRab#getOlehId()}). */
	private String olehId;

	/**
	 * Mengambil id pengguna pembuat/pengubah terakhir (audit sistem).
	 *
	 * @return id pengguna audit, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {return olehId;}

	/**
	 * Mengisi id pengguna pembuat/pengubah terakhir. Guard fail-safe: nilai {@code null}/kosong/spasi
	 * diabaikan diam-diam agar audit trail "siapa" tidak tertimpa kosong — pola sama seperti
	 * {@link InformasiRab#setOlehId(String)}.
	 *
	 * @param olehId id pengguna audit baru; diabaikan bila kosong/{@code null}
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}this.olehId = olehId;}

	/**
	 * Mengisi nama tampilan pembuat/pengubah terakhir (audit sistem). Guard fail-safe yang sama
	 * seperti {@link #setOlehId(String)}: nilai kosong/{@code null} diabaikan diam-diam.
	 *
	 * @param oleh nama tampilan audit baru; diabaikan bila kosong/{@code null}
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengambil nama tampilan pembuat/pengubah terakhir (audit sistem).
	 *
	 * @return nama audit, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil otomatis oleh Hibernate sesaat sebelum setiap
	 * {@code UPDATE} baris ini, mendelegasikan ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} untuk menyegarkan
	 * {@link #tanggal_dirubah} ke waktu saat ini — pola audit-timestamp identik dengan
	 * {@link InformasiRab#onUpdate()} dan entity lain di paket ini.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}
	/**
	 * Timestamp perubahan terakhir. Diinisialisasi ke waktu saat objek dibuat di memori dan
	 * disegarkan otomatis oleh {@link #onUpdate()} pada setiap {@code UPDATE} berikutnya; dipakai
	 * {@code WorkspaceResource#daftarInformasiRabKomentar} sebagai kunci pengurutan
	 * (terbaru-dahulu) daftar komentar yang dikembalikan.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi timestamp perubahan terakhir secara manual. Biasanya tidak perlu dipanggil pemanggil
	 * biasa karena {@link #onUpdate()} sudah menyegarkannya otomatis pada setiap update.
	 *
	 * @param tanggal_dirubah timestamp baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengambil timestamp perubahan terakhir.
	 *
	 * @return timestamp perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi string singkat objek ini, dipakai mis. oleh komponen ZK generik yang menampilkan
	 * objek lewat {@code toString()} (combobox/label default).
	 *
	 * @return nama penulis komentar ({@link #nama} mentah, TANPA {@code trim()} — berbeda dari
	 *         {@link #getNama()} yang men-trim; lihat catatan pada {@link #getNama()})
	 */
	public String toString() {
		return nama;
	}

	/** Nama penulis komentar, diketik bebas oleh penulis (bukan referensi ke entity pengguna/pegawai). */
	private String nama;
	/** Alamat penulis komentar, diketik bebas — field opsional (tidak ada {@code @Column} eksplisit, DB memperbolehkan {@code NULL} secara default). */
	private String alamat;
	/** Kontak (mis. nomor telepon) penulis komentar, diketik bebas. */
	private String kontak;
	/** Alamat email penulis komentar, diketik bebas — TIDAK divalidasi format di level entity. */
	private String email;
	/** Pengumuman/berita RAB ({@link InformasiRab}) yang menjadi target komentar ini. */
	private InformasiRab informasiRab;

	/** Konstruktor default (wajib untuk Hibernate). */
	public InformasiRabKomentar() {
	}

	/**
	 * Mengambil id baris (primary key).
	 *
	 * @return id baris, atau {@code null} untuk entity baru yang belum disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi id baris secara manual. Kolom dipetakan {@code insertable = false} (nilai sesungguhnya
	 * berasal dari {@code IDENTITY} auto-increment DB saat {@code INSERT}).
	 *
	 * @param id id baris baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil nama penulis komentar, di-trim dari spasi di awal/akhir.
	 *
	 * @return nama penulis komentar yang sudah di-trim, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi nama penulis komentar. Nilai disimpan APA ADANYA tanpa {@code trim()} di sini — proses
	 * pemangkasan spasi hanya terjadi saat dibaca lewat {@link #getNama()}, sehingga
	 * {@link #toString()} (yang membaca field {@link #nama} langsung) dapat mengembalikan nilai yang
	 * belum di-trim.
	 *
	 * @param nama nama penulis komentar baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengambil alamat penulis komentar.
	 *
	 * @return alamat penulis komentar, atau {@code null} bila belum diisi
	 */
	public String getAlamat() {
		return alamat;
	}

	/**
	 * Mengisi alamat penulis komentar.
	 *
	 * @param alamat alamat penulis komentar baru
	 */
	public void setAlamat(String alamat) {
		this.alamat = alamat;
	}

	/**
	 * Mengambil kontak penulis komentar.
	 *
	 * @return kontak penulis komentar, atau {@code null} bila belum diisi
	 */
	public String getKontak() {
		return kontak;
	}

	/**
	 * Mengisi kontak penulis komentar.
	 *
	 * @param kontak kontak penulis komentar baru
	 */
	public void setKontak(String kontak) {
		this.kontak = kontak;
	}

	/**
	 * Mengambil alamat email penulis komentar.
	 *
	 * @return email penulis komentar, atau {@code null} bila belum diisi
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * Mengisi alamat email penulis komentar. Tidak ada validasi format di level entity ini.
	 *
	 * @param email email penulis komentar baru
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * Mengambil pengumuman/berita RAB yang menjadi target komentar ini.
	 *
	 * @return {@link InformasiRab} target, atau {@code null} bila belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "informasi_rab", nullable = true)
	public InformasiRab getInformasiRab() {
		return informasiRab;
	}

	/**
	 * Mengisi pengumuman/berita RAB yang menjadi target komentar ini.
	 *
	 * @param informasiRab {@link InformasiRab} target baru
	 */
	public void setInformasiRab(
			InformasiRab informasiRab) {
		this.informasiRab = informasiRab;
	}

}
