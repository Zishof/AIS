package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

/**
 * Model data untuk satu KATEGORI kebutuhan khusus/disabilitas peserta didik/pegawai, dipetakan
 * dari 16 kolom boolean {@code a_kk_*} yang masing-masing merepresentasikan satu JENIS
 * kebutuhan khusus resmi Kemdikbud/PDDIKTI (mis. tuna netra, tuna rungu, tuna daksa, dst. --
 * kode huruf {@code a}-{@code q} mengikuti urutan resmi feeder, dengan lompatan huruf {@code g},
 * {@code l}, {@code m} yang memang tidak dipakai serta varian {@code c1}/{@code d1} untuk
 * sub-kategori). Satu baris entity ini adalah satu OPSI kategori pada daftar pilihan (bukan satu
 * peserta didik/pegawai) -- pemakai memilih baris yang boolean-nya {@code true} sesuai kondisinya.
 * Tipe ini membawa state yang dipertukarkan oleh lapisan persistence, service, dan UI; makna
 * bisnis utamanya ditentukan oleh field serta relasi yang dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Long id}, {@code String oleh}, {@code
 * String olehId}, {@code Date tanggal_dirubah}, enam belas flag {@code Boolean a_kk_*}, {@code Long feeder};
 * pemetaan persistence: tabel {@code public.kebutuhan_khusus}; pembacaan/pencarian ({@code getOlehId()},
 * {@code getId()}, {@code getOleh()}, {@code getTanggal_dirubah()}, {@code getA_kk_a()} dst., {@code
 * getFeeder()}); mutasi data ({@code setOlehId()}, {@code setId()}, {@code setOleh()}, {@code onUpdate()},
 * {@code setTanggal_dirubah()}, {@code setA_kk_a()} dst.). Bagian lain dari kontrak tetap mengikuti kelas
 * induk atau interface yang disebut di atas.</p>
 * <p><b>Catatan struktur data:</b> keenam belas flag {@code a_kk_*} TIDAK memiliki {@code @Column} eksplisit
 * (dipetakan otomatis oleh nama field), tidak punya nilai default (semuanya {@code null}-able, bukan
 * {@code false} secara default di level getter), dan tidak dikelompokkan dalam struktur/koleksi -- pola
 * "kolom boolean bernomor/berkode" yang sama seperti field {@code jenisPekerjaanPenyedia1..5} pada
 * {@code PenyediaAsset} (didokumentasikan di {@link ais.database.model.ParameterTambahanAstract}).</p>
 * <p><b>Efek samping:</b> accessor dan mutator hanya membaca atau mengubah state entity/value object di memori.
 * Persistence, transaksi, otorisasi, dan pemuatan relasi lazy tetap menjadi tanggung jawab DAO/service dengan
 * session aktif; jangan menaruh query duplikat pada model.</p>
 *
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "kebutuhan_khusus")
public class KebutuhanKhusus extends GeneralValueObject {

	/** Versi serialisasi Java untuk kompatibilitas {@code Serializable}. */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key entity (kolom {@code id}, identity/auto-increment). */
	private Long id;
	/**
	 * Nama pengguna pengubah terakhir. Field ini MENIMPA (shadow) field bernama sama pada
	 * {@link GeneralValueObject}; getter/setter di bawah beroperasi pada field lokal ini.
	 */
	private String oleh;
	/** Id pengguna pengubah terakhir; shadow dari field sama pada {@link GeneralValueObject}. */
	private String olehId;

	/**
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi.
	 * @see GeneralValueObject#getOlehId()
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir. Nilai {@code null}/kosong diabaikan diam-diam,
	 * sama seperti {@link GeneralValueObject#setOlehId(String)}.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir. Nilai {@code null}/kosong diabaikan diam-diam,
	 * sama seperti {@link #setOlehId(String)}.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/** @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi. */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA sebelum UPDATE: memperbarui {@link #tanggal_dirubah} lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     /** Stempel waktu perubahan terakhir; diinisialisasi ke saat object dibuat. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** @param tanggal_dirubah stempel waktu perubahan terakhir yang baru. */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return stempel waktu perubahan terakhir. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @return representasi ringkas untuk debug/log: {@code "<id>-<nama>"}. */
	public String toString() {
		return id + "-" + nama;
	}

	/** Nama kategori kebutuhan khusus. */
	private String nama;
	/** Keterangan bebas. */
	private String keterangan;

	/** Flag kategori kebutuhan khusus kode "a" (kode resmi feeder), boleh {@code null}. */
	private Boolean a_kk_a;
	/** Flag kategori kebutuhan khusus kode "b". */
	private Boolean a_kk_b;
	/** Flag kategori kebutuhan khusus kode "c". */
	private Boolean a_kk_c;
	/** Flag kategori kebutuhan khusus kode "c1" (sub-kategori dari "c"). */
	private Boolean a_kk_c1;
	/** Flag kategori kebutuhan khusus kode "d". */
	private Boolean a_kk_d;
	/** Flag kategori kebutuhan khusus kode "d1" (sub-kategori dari "d"). */
	private Boolean a_kk_d1;
	/** Flag kategori kebutuhan khusus kode "e". */
	private Boolean a_kk_e;
	/** Flag kategori kebutuhan khusus kode "f". */
	private Boolean a_kk_f;
	/** Flag kategori kebutuhan khusus kode "h" (kode "g" sengaja tidak dipakai feeder). */
	private Boolean a_kk_h;
	/** Flag kategori kebutuhan khusus kode "i". */
	private Boolean a_kk_i;
	/** Flag kategori kebutuhan khusus kode "j". */
	private Boolean a_kk_j;
	/** Flag kategori kebutuhan khusus kode "k". */
	private Boolean a_kk_k;
	/** Flag kategori kebutuhan khusus kode "n" (kode "l"/"m" sengaja tidak dipakai feeder). */
	private Boolean a_kk_n;
	/** Flag kategori kebutuhan khusus kode "o". */
	private Boolean a_kk_o;
	/** Flag kategori kebutuhan khusus kode "p". */
	private Boolean a_kk_p;
	/** Flag kategori kebutuhan khusus kode "q". */
	private Boolean a_kk_q;

	/** Id/kode integrasi dari sistem feeder (PDDIKTI/Neo Feeder), bila baris ini disinkronkan dari sana. */
	private Long feeder;

	/** Konstruktor kosong, dipakai Hibernate. */
	public KebutuhanKhusus() {
	}

	/** @return primary key entity, atau {@code null} bila belum tersimpan. */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id primary key baru. */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return nama kategori kebutuhan khusus, sudah di-{@code trim}; {@code null} bila belum diisi. */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/** @param nama nama kategori kebutuhan khusus baru. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return keterangan bebas, boleh {@code null}. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan keterangan bebas yang baru. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** @return id/kode integrasi feeder, boleh {@code null}. */
	public Long getFeeder() {
		return feeder;
	}

	/** @param feeder id/kode integrasi feeder yang baru. */
	public void setFeeder(Long feeder) {
		this.feeder = feeder;
	}

	/** @return flag kebutuhan khusus kode "a", boleh {@code null}. */
	public Boolean getA_kk_a() {
		return a_kk_a;
	}

	/** @param a_kk_a flag kode "a" yang baru. */
	public void setA_kk_a(Boolean a_kk_a) {
		this.a_kk_a = a_kk_a;
	}

	/** @return flag kebutuhan khusus kode "b", boleh {@code null}. */
	public Boolean getA_kk_b() {
		return a_kk_b;
	}

	/** @param a_kk_b flag kode "b" yang baru. */
	public void setA_kk_b(Boolean a_kk_b) {
		this.a_kk_b = a_kk_b;
	}

	/** @return flag kebutuhan khusus kode "c", boleh {@code null}. */
	public Boolean getA_kk_c() {
		return a_kk_c;
	}

	/** @param a_kk_c flag kode "c" yang baru. */
	public void setA_kk_c(Boolean a_kk_c) {
		this.a_kk_c = a_kk_c;
	}

	/** @return flag kebutuhan khusus kode "c1" (sub-kategori "c"), boleh {@code null}. */
	public Boolean getA_kk_c1() {
		return a_kk_c1;
	}

	/** @param a_kk_c1 flag kode "c1" yang baru. */
	public void setA_kk_c1(Boolean a_kk_c1) {
		this.a_kk_c1 = a_kk_c1;
	}

	/** @return flag kebutuhan khusus kode "d", boleh {@code null}. */
	public Boolean getA_kk_d() {
		return a_kk_d;
	}

	/** @param a_kk_d flag kode "d" yang baru. */
	public void setA_kk_d(Boolean a_kk_d) {
		this.a_kk_d = a_kk_d;
	}

	/** @return flag kebutuhan khusus kode "d1" (sub-kategori "d"), boleh {@code null}. */
	public Boolean getA_kk_d1() {
		return a_kk_d1;
	}

	/** @param a_kk_d1 flag kode "d1" yang baru. */
	public void setA_kk_d1(Boolean a_kk_d1) {
		this.a_kk_d1 = a_kk_d1;
	}

	/** @return flag kebutuhan khusus kode "e", boleh {@code null}. */
	public Boolean getA_kk_e() {
		return a_kk_e;
	}

	/** @param a_kk_e flag kode "e" yang baru. */
	public void setA_kk_e(Boolean a_kk_e) {
		this.a_kk_e = a_kk_e;
	}

	/** @return flag kebutuhan khusus kode "f", boleh {@code null}. */
	public Boolean getA_kk_f() {
		return a_kk_f;
	}

	/** @param a_kk_f flag kode "f" yang baru. */
	public void setA_kk_f(Boolean a_kk_f) {
		this.a_kk_f = a_kk_f;
	}

	/** @return flag kebutuhan khusus kode "h", boleh {@code null}. */
	public Boolean getA_kk_h() {
		return a_kk_h;
	}

	/** @param a_kk_h flag kode "h" yang baru. */
	public void setA_kk_h(Boolean a_kk_h) {
		this.a_kk_h = a_kk_h;
	}

	/** @return flag kebutuhan khusus kode "i", boleh {@code null}. */
	public Boolean getA_kk_i() {
		return a_kk_i;
	}

	/** @param a_kk_i flag kode "i" yang baru. */
	public void setA_kk_i(Boolean a_kk_i) {
		this.a_kk_i = a_kk_i;
	}

	/** @return flag kebutuhan khusus kode "j", boleh {@code null}. */
	public Boolean getA_kk_j() {
		return a_kk_j;
	}

	/** @param a_kk_j flag kode "j" yang baru. */
	public void setA_kk_j(Boolean a_kk_j) {
		this.a_kk_j = a_kk_j;
	}

	/** @return flag kebutuhan khusus kode "k", boleh {@code null}. */
	public Boolean getA_kk_k() {
		return a_kk_k;
	}

	/** @param a_kk_k flag kode "k" yang baru. */
	public void setA_kk_k(Boolean a_kk_k) {
		this.a_kk_k = a_kk_k;
	}

	/** @return flag kebutuhan khusus kode "n", boleh {@code null}. */
	public Boolean getA_kk_n() {
		return a_kk_n;
	}

	/** @param a_kk_n flag kode "n" yang baru. */
	public void setA_kk_n(Boolean a_kk_n) {
		this.a_kk_n = a_kk_n;
	}

	/** @return flag kebutuhan khusus kode "o", boleh {@code null}. */
	public Boolean getA_kk_o() {
		return a_kk_o;
	}

	/** @param a_kk_o flag kode "o" yang baru. */
	public void setA_kk_o(Boolean a_kk_o) {
		this.a_kk_o = a_kk_o;
	}

	/** @return flag kebutuhan khusus kode "p", boleh {@code null}. */
	public Boolean getA_kk_p() {
		return a_kk_p;
	}

	/** @param a_kk_p flag kode "p" yang baru. */
	public void setA_kk_p(Boolean a_kk_p) {
		this.a_kk_p = a_kk_p;
	}

	/** @return flag kebutuhan khusus kode "q", boleh {@code null}. */
	public Boolean getA_kk_q() {
		return a_kk_q;
	}

	/** @param a_kk_q flag kode "q" yang baru. */
	public void setA_kk_q(Boolean a_kk_q) {
		this.a_kk_q = a_kk_q;
	}

}
