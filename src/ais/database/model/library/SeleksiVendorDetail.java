package ais.database.model.library;

// Detail per-vendor untuk pengajuan Seleksi Vendor.
// Setiap baris = satu vendor (berelasi ke Penyedia) + data manual + 9 skor kriteria (1..5).

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
import javax.persistence.Transient;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * Entitas detail <b>per-vendor</b> pada satu pengajuan {@link SeleksiVendor} (tabel
 * {@code library.seleksi_vendor_detail}) — satu baris mewakili satu vendor pembanding
 * (I/II/III/...) beserta sembilan skor kriteria (1..5) yang diberikan padanya.
 *
 * <h2>Relasi</h2>
 * <p>{@link #getSeleksiVendor()} menautkan detail ini ke header pengajuannya (banyak-ke-satu,
 * fetch <i>eager</i> bawaan JPA sehingga tidak memerlukan resolusi {@code check(...)}), dan
 * {@link #getPenyedia()} menautkannya ke {@link Penyedia} yang sesungguhnya bila vendor ini sudah
 * terdaftar sebagai master data (opsional — lihat di bawah).</p>
 *
 * <h2>Data manual boleh menimpang dari {@link Penyedia}</h2>
 * <p>{@link #getNamaVendor()}, {@link #getAlamatKontak()}, dan {@link #getPicVendor()} masing-masing
 * mengembalikan nilai manual yang tersimpan langsung pada baris ini bila terisi, dan hanya jatuh ke
 * data {@link #getPenyedia()} sebagai <b>bawaan</b> bila kolom manualnya kosong. Dengan kata lain
 * detail seleksi <b>tidak wajib</b> menunjuk {@link Penyedia} yang sudah terdaftar — staf pengadaan
 * dapat mengisi data vendor pembanding secara manual (mis. vendor yang belum pernah dipakai
 * sebelumnya dan belum masuk master data) tanpa membuat baris {@link Penyedia} baru terlebih
 * dahulu. Ini konsisten dengan sifat proses seleksi sebagai tahap <b>sebelum</b> vendor resmi
 * dipakai/didaftarkan.</p>
 *
 * <h2>Perhitungan skor</h2>
 * <p>{@link #getTotalNilai()} menjumlahkan kesembilan skor mentah (maksimum 45), sedangkan
 * {@link #getSkorTertimbang()} mengalikan tiap skor dengan bobot kriteria yang bersangkutan dari
 * {@link #getSeleksiVendor()} (lihat javadoc {@link SeleksiVendor} bagian bobot best-practice) dan
 * menyekalakannya ke rentang 0..100. Kedua method ini {@code @Transient} — tidak pernah tersimpan
 * ke basis data, selalu dihitung ulang dari skor dan bobot saat ini.</p>
 *
 * @see SeleksiVendor
 * @see Penyedia
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "library", name = "seleksi_vendor_detail")
public class SeleksiVendorDetail extends GeneralValueObject {

	/** Penanda versi serialisasi Java. Nilai warisan cetakan hbm2java; jangan diubah tanpa alasan. */
	private static final long serialVersionUID = 2463821577548439811L;

	/** Kunci utama basis data, dibangkitkan {@code IDENTITY}; {@code null} selama baris belum tersimpan. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini; diisi {@code AuditTimestampInterceptor}, bukan oleh form. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini; pasangan teknis dari {@link #oleh}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris detail ini.
	 *
	 * @return id pengguna terakhir, atau {@code null} bila baris belum pernah diubah lewat jalur
	 *         yang memasang interceptor audit
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna yang terakhir mengubah baris detail ini.
	 *
	 * <p><b>Setter defensif:</b> masukan {@code null}/kosong diabaikan diam-diam.</p>
	 *
	 * @param olehId id pengguna; {@code null}/kosong diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna yang terakhir mengubah baris detail ini.
	 *
	 * @param oleh nama pengguna; {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris detail ini.
	 *
	 * @return nama pengguna terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate} yang mendelegasikan pencatatan audit ke
	 * {@code AuditTimestampInterceptor.ubah(this)} tepat sebelum baris diperbarui. Method sengaja
	 * {@code protected} dan tidak boleh dipanggil manual dari kode aplikasi.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Umumnya dipanggil {@code AuditTimestampInterceptor}.
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris detail ini.
	 *
	 * @return stempel waktu perubahan terakhir; tidak pernah {@code null} untuk objek baru di memori
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat: nama vendor (lihat {@link #getNamaVendor()} untuk logikanya,
	 * tetapi method ini membaca bidang mentah {@link #namaVendor} langsung tanpa fallback ke
	 * {@link Penyedia}).
	 *
	 * @return nama vendor mentah; dapat {@code null} bila kolom manual belum diisi
	 */
	public String toString() {
		return namaVendor;
	}

	/** Header pengajuan seleksi vendor tempat baris detail ini berada. */
	private SeleksiVendor seleksiVendor;
	/** Vendor master data yang ditunjuk (opsional) — lihat javadoc kelas soal data manual. */
	private Penyedia penyedia;
	/** Nomor urut vendor pada pengajuan (Vendor I/II/III &rarr; 1,2,3). */
	private Integer urutan;         // Vendor I/II/III -> 1,2,3

	// Section A - data vendor (manual, boleh override dari Penyedia)
	/** Nama vendor, isian manual; jatuh ke {@link Penyedia#getNama()} bila kosong — lihat {@link #getNamaVendor()}. */
	private String namaVendor;
	/** Alamat+kontak vendor, isian manual; jatuh ke gabungan alamat/telp {@link Penyedia} bila kosong. */
	private String alamatKontak;
	/** Jenis barang/jasa yang ditawarkan vendor ini. */
	private String jenisBarangJasa;
	/** PIC vendor, isian manual; jatuh ke {@link Penyedia#getKontak()} bila kosong — lihat {@link #getPicVendor()}. */
	private String picVendor;

	// Section B - 9 skor kriteria (1..5)
	/** Skor kriteria harga (1..5). */
	private Integer nilaiHarga;
	/** Skor kriteria kesesuaian spesifikasi (1..5). */
	private Integer nilaiSpesifikasi;
	/** Skor kriteria ketersediaan (1..5). */
	private Integer nilaiKetersediaan;
	/** Skor kriteria kejelasan penawaran (1..5). */
	private Integer nilaiKejelasan;
	/** Skor kriteria legalitas vendor (1..5). */
	private Integer nilaiLegalitas;
	/** Skor kriteria pengalaman vendor (1..5). */
	private Integer nilaiPengalaman;
	/** Skor kriteria responsivitas vendor (1..5). */
	private Integer nilaiResponsif;
	/** Skor kriteria kemudahan pembayaran (1..5). */
	private Integer nilaiPembayaran;
	/** Skor kriteria reputasi vendor (1..5). */
	private Integer nilaiReputasi;

	/** Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate. */
	public SeleksiVendorDetail() {
	}

	/**
	 * Mengembalikan kunci utama baris detail ini.
	 *
	 * @return id detail, atau {@code null} bila baris belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama baris detail ini. Hanya untuk kebutuhan Hibernate/penyalinan objek.
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan header pengajuan seleksi vendor yang menaungi baris detail ini.
	 *
	 * <p>Berbeda dari kebanyakan relasi lain pada entitas paket ini, method ini <b>tidak</b>
	 * memanggil {@code check(...)} karena {@code fetch} tidak dinyatakan {@code LAZY} pada anotasi
	 * (bawaan JPA untuk {@code @ManyToOne} adalah <i>eager</i>), sehingga relasi ini tidak pernah
	 * berupa proksi lazim yang perlu diselesaikan.</p>
	 *
	 * @return header pengajuan seleksi vendor; boleh {@code null} bila belum ditautkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "seleksi_vendor", nullable = true)
	public SeleksiVendor getSeleksiVendor() {
		return seleksiVendor;
	}

	/**
	 * Menyetel header pengajuan seleksi vendor untuk baris detail ini.
	 *
	 * @param seleksiVendor header pengajuan; boleh {@code null}
	 */
	public void setSeleksiVendor(SeleksiVendor seleksiVendor) {
		this.seleksiVendor = seleksiVendor;
	}

	/**
	 * Mengembalikan vendor master data yang ditunjuk baris detail ini, setelah proksi malasnya
	 * diselesaikan {@code check(...)}.
	 *
	 * <p>Boleh {@code null} — lihat javadoc kelas: detail seleksi tidak wajib menunjuk
	 * {@link Penyedia} yang sudah terdaftar, staf pengadaan dapat mengisi data vendor secara
	 * manual pada {@link #namaVendor}/{@link #alamatKontak}/{@link #picVendor}.</p>
	 *
	 * @return vendor master data, atau {@code null} bila vendor ini murni data manual
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "penyedia", nullable = true)
	public Penyedia getPenyedia() {
		penyedia = check(penyedia);
		return penyedia;
	}

	/**
	 * Menyetel vendor master data yang ditunjuk baris detail ini.
	 *
	 * @param penyedia vendor master data; boleh {@code null}
	 */
	public void setPenyedia(Penyedia penyedia) {
		this.penyedia = penyedia;
	}

	/**
	 * Mengembalikan nomor urut vendor pada pengajuan (Vendor I/II/III &rarr; 1,2,3), dengan bawaan
	 * 0 bila belum diisi.
	 *
	 * @return nomor urut; tidak pernah {@code null}
	 */
	@Column(name = "urutan")
	public Integer getUrutan() {
		return urutan == null ? 0 : urutan;
	}

	/**
	 * Menyetel nomor urut vendor pada pengajuan.
	 *
	 * @param urutan nomor urut; boleh {@code null} untuk kembali ke bawaan 0
	 */
	public void setUrutan(Integer urutan) {
		this.urutan = urutan;
	}

	/**
	 * Mengembalikan nama vendor, dengan fallback ke {@link Penyedia#getNama()} bila kolom manual
	 * kosong dan {@link #getPenyedia()} tidak {@code null}.
	 *
	 * <p>Fallback ini hanya berlaku saat <b>dibaca</b>; nilai manual yang kosong tidak diisi ulang
	 * ke bidang {@link #namaVendor} (method ini bebas efek samping, berbeda dari banyak getter
	 * relasi lazim di kelas lain paket ini).</p>
	 *
	 * @return nama vendor manual bila terisi; bila tidak, nama {@link Penyedia} bila tertaut;
	 *         atau {@code null} bila keduanya kosong
	 */
	@Column(name = "nama_vendor", length = 255)
	public String getNamaVendor() {
		if ((namaVendor == null || namaVendor.trim().isEmpty()) && getPenyedia() != null) {
			return getPenyedia().getNama();
		}
		return namaVendor;
	}

	/**
	 * Menyetel nama vendor secara manual, menimpang fallback ke {@link Penyedia}.
	 *
	 * @param namaVendor nama vendor manual; boleh {@code null}/kosong untuk mengaktifkan fallback
	 */
	public void setNamaVendor(String namaVendor) {
		this.namaVendor = namaVendor;
	}

	/**
	 * Mengembalikan alamat dan kontak vendor, dengan fallback ke gabungan alamat dan telepon
	 * {@link Penyedia} bila kolom manual kosong.
	 *
	 * <p><b>Format fallback.</b> Bila {@link #getPenyedia()} tertaut, alamat dan telepon vendor
	 * digabung dengan pemisah {@code " / "} (hanya bila telepon terisi), lalu dipangkas spasi
	 * tepinya. Bila hasil gabungan kosong (keduanya kosong), method mengembalikan {@code null}
	 * alih-alih string kosong. Sama seperti {@link #getNamaVendor()}, fallback ini tidak menulis
	 * kembali ke bidang instans.</p>
	 *
	 * @return alamat+kontak manual bila terisi; bila tidak, gabungan alamat/telepon
	 *         {@link Penyedia} bila tertaut dan tidak keduanya kosong; atau {@code null}
	 */
	@Column(name = "alamat_kontak")
	public String getAlamatKontak() {
		if ((alamatKontak == null || alamatKontak.trim().isEmpty()) && getPenyedia() != null) {
			String a = getPenyedia().getAlamat();
			String t = getPenyedia().getTelp();
			String gabung = (a == null ? "" : a) + (t == null || t.trim().isEmpty() ? "" : " / " + t);
			return gabung.trim().isEmpty() ? null : gabung.trim();
		}
		return alamatKontak;
	}

	/**
	 * Menyetel alamat dan kontak vendor secara manual, menimpang fallback ke {@link Penyedia}.
	 *
	 * @param alamatKontak alamat+kontak manual; boleh {@code null}/kosong untuk mengaktifkan fallback
	 */
	public void setAlamatKontak(String alamatKontak) {
		this.alamatKontak = alamatKontak;
	}

	/**
	 * Mengembalikan jenis barang/jasa yang ditawarkan vendor ini. Tidak ada fallback ke
	 * {@link Penyedia} — kolom ini murni isian manual per pengajuan.
	 *
	 * @return jenis barang/jasa; boleh {@code null}
	 */
	@Column(name = "jenis_barang_jasa")
	public String getJenisBarangJasa() {
		return jenisBarangJasa;
	}

	/**
	 * Menyetel jenis barang/jasa yang ditawarkan vendor ini.
	 *
	 * @param jenisBarangJasa jenis barang/jasa; boleh {@code null}
	 */
	public void setJenisBarangJasa(String jenisBarangJasa) {
		this.jenisBarangJasa = jenisBarangJasa;
	}

	/**
	 * Mengembalikan nama PIC (person in charge) vendor, dengan fallback ke
	 * {@link Penyedia#getKontak()} bila kolom manual kosong.
	 *
	 * @return PIC manual bila terisi; bila tidak, kontak {@link Penyedia} bila tertaut; atau
	 *         {@code null} bila keduanya kosong
	 */
	@Column(name = "pic_vendor")
	public String getPicVendor() {
		if ((picVendor == null || picVendor.trim().isEmpty()) && getPenyedia() != null) {
			return getPenyedia().getKontak();
		}
		return picVendor;
	}

	/**
	 * Menyetel nama PIC vendor secara manual, menimpang fallback ke {@link Penyedia}.
	 *
	 * @param picVendor PIC manual; boleh {@code null}/kosong untuk mengaktifkan fallback
	 */
	public void setPicVendor(String picVendor) {
		this.picVendor = picVendor;
	}

	/** @return skor kriteria harga (1..5); boleh {@code null} bila belum dinilai */
	@Column(name = "nilai_harga") public Integer getNilaiHarga() { return nilaiHarga; }
	/** @param v skor kriteria harga (1..5) */
	public void setNilaiHarga(Integer v) { this.nilaiHarga = v; }
	/** @return skor kriteria kesesuaian spesifikasi (1..5); boleh {@code null} bila belum dinilai */
	@Column(name = "nilai_spesifikasi") public Integer getNilaiSpesifikasi() { return nilaiSpesifikasi; }
	/** @param v skor kriteria kesesuaian spesifikasi (1..5) */
	public void setNilaiSpesifikasi(Integer v) { this.nilaiSpesifikasi = v; }
	/** @return skor kriteria ketersediaan (1..5); boleh {@code null} bila belum dinilai */
	@Column(name = "nilai_ketersediaan") public Integer getNilaiKetersediaan() { return nilaiKetersediaan; }
	/** @param v skor kriteria ketersediaan (1..5) */
	public void setNilaiKetersediaan(Integer v) { this.nilaiKetersediaan = v; }
	/** @return skor kriteria kejelasan penawaran (1..5); boleh {@code null} bila belum dinilai */
	@Column(name = "nilai_kejelasan") public Integer getNilaiKejelasan() { return nilaiKejelasan; }
	/** @param v skor kriteria kejelasan penawaran (1..5) */
	public void setNilaiKejelasan(Integer v) { this.nilaiKejelasan = v; }
	/** @return skor kriteria legalitas vendor (1..5); boleh {@code null} bila belum dinilai */
	@Column(name = "nilai_legalitas") public Integer getNilaiLegalitas() { return nilaiLegalitas; }
	/** @param v skor kriteria legalitas vendor (1..5) */
	public void setNilaiLegalitas(Integer v) { this.nilaiLegalitas = v; }
	/** @return skor kriteria pengalaman vendor (1..5); boleh {@code null} bila belum dinilai */
	@Column(name = "nilai_pengalaman") public Integer getNilaiPengalaman() { return nilaiPengalaman; }
	/** @param v skor kriteria pengalaman vendor (1..5) */
	public void setNilaiPengalaman(Integer v) { this.nilaiPengalaman = v; }
	/** @return skor kriteria responsivitas vendor (1..5); boleh {@code null} bila belum dinilai */
	@Column(name = "nilai_responsif") public Integer getNilaiResponsif() { return nilaiResponsif; }
	/** @param v skor kriteria responsivitas vendor (1..5) */
	public void setNilaiResponsif(Integer v) { this.nilaiResponsif = v; }
	/** @return skor kriteria kemudahan pembayaran (1..5); boleh {@code null} bila belum dinilai */
	@Column(name = "nilai_pembayaran") public Integer getNilaiPembayaran() { return nilaiPembayaran; }
	/** @param v skor kriteria kemudahan pembayaran (1..5) */
	public void setNilaiPembayaran(Integer v) { this.nilaiPembayaran = v; }
	/** @return skor kriteria reputasi vendor (1..5); boleh {@code null} bila belum dinilai */
	@Column(name = "nilai_reputasi") public Integer getNilaiReputasi() { return nilaiReputasi; }
	/** @param v skor kriteria reputasi vendor (1..5) */
	public void setNilaiReputasi(Integer v) { this.nilaiReputasi = v; }

	/**
	 * Menormalkan skor {@code null} menjadi 0 untuk keperluan penjumlahan.
	 *
	 * @param v skor mentah, boleh {@code null}
	 * @return {@code v} sebagai {@code int}, atau 0 bila {@code v} adalah {@code null}
	 */
	private static int n(Integer v) { return v == null ? 0 : v.intValue(); }

	/**
	 * Menghitung jumlah skor mentah kesembilan kriteria (maksimum 45 bila seluruhnya bernilai 5).
	 *
	 * <p>Skor yang belum dinilai ({@code null}) diperlakukan sebagai 0 lewat {@link #n(Integer)},
	 * sehingga vendor yang baru sebagian dinilai akan tampak memiliki total lebih rendah — bukan
	 * karena kinerjanya buruk, melainkan karena penilaiannya belum lengkap. Method ini
	 * {@code @Transient}, tidak pernah tersimpan ke basis data dan selalu dihitung ulang dari
	 * kesembilan bidang skor saat ini.</p>
	 *
	 * @return jumlah skor mentah, 0..45
	 */
	@Transient
	public Integer getTotalNilai() {
		return n(nilaiHarga) + n(nilaiSpesifikasi) + n(nilaiKetersediaan) + n(nilaiKejelasan)
				+ n(nilaiLegalitas) + n(nilaiPengalaman) + n(nilaiResponsif) + n(nilaiPembayaran)
				+ n(nilaiReputasi);
	}

	/**
	 * Menghitung skor tertimbang best-practice vendor ini pada skala 0..100, memakai bobot
	 * kriteria dari header {@link #getSeleksiVendor()}.
	 *
	 * <p><b>Rumus.</b> Untuk tiap kriteria, skor (0..5, {@code null} dianggap 0) dikalikan bobot
	 * persennya (lihat {@code getBobotHarga()} dkk. pada {@link SeleksiVendor}, yang totalnya
	 * bawaan 100). Kesembilan hasil kali dijumlahkan, dibagi 5 (skor maksimum per kriteria) untuk
	 * menyekalakan ke rentang 0..100 (karena bobot totalnya ~100 dan skor maksimum 5, hasil kali
	 * maksimum teoretis adalah {@code 100 * 5 = 500}, dibagi 5 menjadi 100), lalu dibulatkan ke
	 * dua desimal lewat {@code Math.round(x * 100.0) / 100.0}.</p>
	 *
	 * <p><b>Tanpa header.</b> Bila {@link #getSeleksiVendor()} bernilai {@code null} (detail belum
	 * ditautkan ke pengajuan manapun), method jatuh ke rata-rata sederhana:
	 * {@code getTotalNilai() * 100.0 / 45.0} — memperlakukan seluruh kriteria seakan berbobot sama
	 * rata, tanpa pembulatan dua desimal seperti cabang utama.</p>
	 *
	 * <p><b>Konsekuensi bobot tidak tepat 100.</b> Bila staf pengadaan mengubah bobot sembilan
	 * kriteria sedemikian rupa sehingga totalnya tidak persis 100 (tidak ada validasi yang
	 * memastikan totalnya 100 — lihat {@code SeleksiVendorAction}), skor tertimbang yang
	 * dihasilkan tidak lagi benar-benar berskala 0..100; ia bisa melebihi 100 atau kurang dari
	 * skala yang diharapkan tanpa peringatan apa pun dari method ini.</p>
	 *
	 * @return skor tertimbang pada skala nominal 0..100, dibulatkan dua desimal; atau rata-rata
	 *         sederhana tanpa pembulatan khusus bila belum ditautkan ke header pengajuan manapun
	 * @see SeleksiVendor
	 */
	@Transient
	public Double getSkorTertimbang() {
		SeleksiVendor h = getSeleksiVendor();
		if (h == null) {
			// tanpa header: rata-rata sederhana skala 0..100
			return getTotalNilai() * 100.0 / 45.0;
		}
		double total = n(nilaiHarga) * h.getBobotHarga() + n(nilaiSpesifikasi) * h.getBobotSpesifikasi()
				+ n(nilaiKetersediaan) * h.getBobotKetersediaan() + n(nilaiKejelasan) * h.getBobotKejelasan()
				+ n(nilaiLegalitas) * h.getBobotLegalitas() + n(nilaiPengalaman) * h.getBobotPengalaman()
				+ n(nilaiResponsif) * h.getBobotResponsif() + n(nilaiPembayaran) * h.getBobotPembayaran()
				+ n(nilaiReputasi) * h.getBobotReputasi();
		// bobot dalam persen (total ~100), nilai maks 5 -> bagi 5 untuk skala 0..100
		return Math.round((total / 5.0) * 100.0) / 100.0;
	}

}
