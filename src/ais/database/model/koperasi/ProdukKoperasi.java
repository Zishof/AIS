package ais.database.model.koperasi;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;
import org.json.JSONArray;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.model.GeneralValueObject;
import ais.database.model.surat.NomorSurat;

/**
 * Katalog produk koperasi Simpan-Pinjam (USPK) — satu baris di sini mendefinisikan SATU jenis
 * produk (mis. "Simpanan Pokok", "Simpanan Wajib", "Simpanan Sukarela", "Pinjaman Modal Usaha")
 * yang dipilih anggota saat bertransaksi lewat {@link TransaksiKoperasi#getProdukKoperasi()}.
 *
 * <p><b>Peran ganda sebagai konfigurasi produk sekaligus mesin parameter transaksi.</b> Kelas ini
 * BUKAN sekadar master data pasif: sejumlah getter ({@link #getFormula()},
 * {@link #getOtomatisTerbentukTransaksi()}, {@link #getJumlahTransaksiTerbentuk()},
 * {@link #getHanyaBolehSekaliTransaksi()}) melakukan auto-seed/auto-infer nilai default
 * berdasarkan {@link #getNama()} atau {@link #getTipeProdukKoperasi()} SAAT DIPANGGIL, dan
 * MENULIS hasilnya balik ke field instance (pola getter-yang-memutasi-state, sudah tercatat
 * berulang di domain finansial AIS lain). Karena entity ini dikelola Hibernate dengan
 * {@code dynamicUpdate = true}, pemanggilan getter tsb sebelum flush bisa membuat nilai hasil
 * inferensi ikut TERSIMPAN ke database meskipun tidak pernah di-set eksplisit lewat UI.</p>
 *
 * <p><b>Field {@link #bunga} dan {@link #jangkaWaktuBulan} dibaca LIVE, bukan snapshot, oleh
 * transaksi yang sudah dibuat.</b> {@link TransaksiKoperasi#getMargin()} menghitung ulang bunga
 * dari KONFIGURASI PRODUK SAAT INI setiap kali dipanggil (bukan dari nilai saat transaksi
 * dibuat) — mengubah {@link #getBunga()} atau {@link #getJangkaWaktuBulan()} produk akan
 * mengubah tampilan margin/total SELURUH transaksi historis yang memakai produk tsb. Lihat
 * catatan lengkap pada {@link TransaksiKoperasi#getMargin()}.</p>
 *
 * <p><b>Relasi:</b> {@link #getTipeProdukKoperasi()} menentukan arah akuntansi (PENAMBAHAN untuk
 * produk simpanan, PENGURANGAN untuk produk pinjaman) yang dipakai mesin posting Simpan-Pinjam
 * (dok 61 butir A) lewat {@link #getAkun()}; {@link #getSyaratProdukKoperasis()} adalah syarat
 * administratif (dokumen/berkas) yang harus dipenuhi anggota sebelum produk ini bisa dipilih;
 * parameter tambahan per produk (field kustom di luar kolom baku di sini) dikelola lewat klaster
 * {@code KelompokParameterTambahanProdukKoperasi}/{@code ParameterTambahanProdukKoperasi}.</p>
 *
 * <p>Dihasilkan awalnya oleh hbm2java (16 Apr 2010); struktur sudah jauh berkembang sejak itu,
 * termasuk konsolidasi parameter eks-{@code ProdukPinjaman} (lihat komentar penanda di bawah)
 * dan penambahan kolom akun untuk mesin posting (r78584).</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "produk_koperasi")
public class ProdukKoperasi extends GeneralValueObject {

	/** Nilai {@link #durasi}/{@link #penghitunganBunga}: siklus harian. */
	public static final String HARIAN = "Harian";
	/** Nilai {@link #durasi}/{@link #penghitunganBunga}: siklus mingguan. */
	public static final String MINGGUAN = "Mingguan";
	/** Nilai {@link #durasi}/{@link #penghitunganBunga}: siklus bulanan — default {@link #getDurasi()}
	 * dan {@link #getPenghitunganBunga()} bila kolom kosong. */
	public static final String BULANAN = "Bulanan";
	/** Nilai {@link #durasi}/{@link #penghitunganBunga}: siklus tahunan. */
	public static final String TAHUNAN = "Tahunan";

	/**
	 * Metode bunga <b>flat (tetap)</b>: bunga dihitung dari saldo awal pokok pinjaman. Ini adalah
	 * perilaku default historis modul (lihat {@link TransaksiKoperasi#getMargin()} yang menghitung
	 * {@code nilai * bunga% * jangkaWaktuBulan}).
	 */
	public static final String METODE_FLAT = "FLAT";
	/** Metode bunga <b>menurun (efektif)</b>: bunga dihitung dari sisa pokok pinjaman. */
	public static final String METODE_MENURUN = "MENURUN";
	/** Metode <b>anuitas</b>: total cicilan tetap, komposisi pokok-bunga berubah tiap periode. */
	public static final String METODE_ANUITAS = "ANUITAS";

	/** Metode bunga simpanan atas <b>saldo terendah</b> dalam satu bulan. */
	public static final String BUNGA_SIMPANAN_SALDO_TERENDAH = "SALDO_TERENDAH";
	/** Metode bunga simpanan atas <b>saldo harian</b> (bunga di-akru tiap hari). */
	public static final String BUNGA_SIMPANAN_SALDO_HARIAN = "SALDO_HARIAN";
	/** Metode bunga simpanan atas <b>saldo rata-rata</b> dalam satu bulan. */
	public static final String BUNGA_SIMPANAN_SALDO_RATA_RATA = "SALDO_RATA_RATA";

	/**
	 * Versi serialisasi tetap — JANGAN diubah kecuali struktur field berubah secara tidak
	 * kompatibel, karena entity ini di-{@code Audited} (Envers menyimpan snapshot ter-serialisasi
	 * pada tabel riwayat {@code produk_koperasi_aud}).
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * Id (username/kode) pengguna yang terakhir mengubah baris produk ini. Diisi otomatis oleh
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(ais.database.model.GeneralValueObject)}
	 * lewat hook {@link #onUpdate()} — bukan field yang lazim diisi manual dari layar.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah diubah lewat
	 *         jalur yang memicu interceptor (mis. baris hasil import langsung).
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id pengguna pengubah. Nilai {@code null} atau string kosong/blank DIABAIKAN
	 * (metode kembali tanpa efek) — sekali terisi, field ini tidak bisa dikosongkan lagi lewat
	 * setter ini, hanya bisa ditimpa nilai baru yang valid. Konsisten dengan {@link #setOleh(String)}.
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null}/blank.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan nama pengguna pengubah. Nilai {@code null} atau string kosong/blank DIABAIKAN
	 * (metode kembali tanpa efek), sama seperti {@link #setOlehId(String)} — mencegah audit
	 * trail ditimpa kosong oleh pemanggil yang lupa mengisi konteks pengguna.
	 *
	 * @param oleh nama/identitas pengguna; diabaikan bila {@code null}/blank.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Nama/identitas pengguna yang terakhir mengubah baris produk ini. Lihat {@link #setOleh(String)}
	 * dan {@link #getOlehId()} — field audit shadow, diisi otomatis lewat {@link #onUpdate()}.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah diubah.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate}: dipanggil Hibernate otomatis tepat sebelum UPDATE dieksekusi
	 * (setiap kali entity yang sudah persisten mengalami perubahan field dan di-flush). Mendelegasikan
	 * ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(ais.database.model.GeneralValueObject)}
	 * yang mengisi {@link #oleh}/{@link #olehId} dari konteks pengguna sesi aktif dan memperbarui
	 * {@link #tanggal_dirubah}. Tidak dipanggil manual dari kode aplikasi.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan waktu perubahan terakhir secara manual. Field ini normalnya diperbarui otomatis
	 * oleh {@link #onUpdate()} lewat interceptor audit — pemanggilan setter ini langsung dari kode
	 * aplikasi jarang diperlukan dan bisa menimpa jejak audit yang seharusnya mencerminkan waktu
	 * UPDATE sebenarnya di database.
	 *
	 * @param tanggal_dirubah waktu perubahan baru.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Waktu perubahan terakhir baris produk ini. Diinisialisasi ke waktu saat ini pada
	 * instansiasi objek (baris baru yang belum disimpan sudah punya nilai), lalu diperbarui
	 * otomatis oleh {@link #onUpdate()} setiap UPDATE.
	 *
	 * @return waktu perubahan terakhir, tidak pernah {@code null}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi ringkas untuk log/debug/dropdown: {@code "<id>-<nama>"}. Memakai field
	 * {@link #nama} apa adanya (tanpa trim seperti {@link #getNama()}) dan TIDAK null-safe —
	 * bila {@link #id} atau {@code nama} {@code null}, hasilnya memuat literal string {@code "null"}.
	 *
	 * @return string ringkas {@code id-nama}, tidak pernah {@code null} objeknya sendiri.
	 */
	public String toString() {
		return id + "-" + nama;
	}

	private String kode;

	private String nama;
	private String keterangan;
	private Boolean aktif;
	private Integer nomorUrut;
	private Double jangkaWaktuBulan;
	private String durasi;
	private String penghitunganBunga;
	private NomorSurat nomorSurat;
	private Boolean hitungShu;
	private Koperasi koperasi;
	private TipeProdukKoperasi tipeProdukKoperasi;

	private Boolean otomatisTerbentukTransaksi;
	private Boolean hanyaBolehSekaliTransaksi;
	private Integer jumlahTransaksiTerbentuk;

	private Double bunga;
	private Double nilaiMaksimal;
	private Double nilaiMinimal;
	private Double setoran;
	// Parameter tambahan pinjaman sesuai SOM USPK (hasil konsolidasi dari eks-ProdukPinjaman).
	private String metodeBunga;
	private Double provisiPersen;
	private Double biayaAdmin;
	private Boolean wajibAgunan;
	private Double dendaPersenPerHari;
	// Parameter bunga simpanan sesuai SOM USPK (untuk produk simpanan sukarela/berjangka).
	private String metodeBungaSimpanan;
	private Double bungaSimpananPersen;
	private Set<SyaratProdukKoperasi> syaratProdukKoperasis = new HashSet<SyaratProdukKoperasi>();
	private String formula;

	/**
	 * Syarat administratif (dokumen/berkas/kondisi) yang harus dipenuhi anggota sebelum produk
	 * ini bisa ditransaksikan — relasi many-to-many lewat tabel penghubung
	 * {@code koperasi.produk_koperasi_punya_syarat}. Cascade hanya {@code MERGE} (tidak
	 * {@code PERSIST}/{@code REMOVE}): baris {@link SyaratProdukKoperasi} harus sudah ada
	 * terlebih dulu, menghapus produk ini tidak menghapus syarat yang dipakai produk lain.
	 *
	 * @return himpunan syarat produk, tidak pernah {@code null} (default {@code HashSet} kosong).
	 */
	@ManyToMany(targetEntity = SyaratProdukKoperasi.class, cascade = { CascadeType.MERGE })
	@JoinTable(name = "produk_koperasi_punya_syarat", schema = "koperasi", joinColumns = @JoinColumn(name = "produk_koperasi"), inverseJoinColumns = @JoinColumn(name = "syarat"))
	public Set<SyaratProdukKoperasi> getSyaratProdukKoperasis() {
		return syaratProdukKoperasis;
	}

	/**
	 * Mengganti seluruh himpunan syarat produk. Layar edit produk lazimnya membangun ulang
	 * seluruh set dari pilihan checkbox pengguna lalu memanggil setter ini sekali (bukan
	 * menambah/menghapus satu per satu) — lihat pemakaian di {@code ProdukKoperasiAction}.
	 *
	 * @param syaratProdukKoperasis himpunan syarat baru; boleh {@code null} (menimpa referensi,
	 *                              pemanggil berikutnya ke {@link #getSyaratProdukKoperasis()}
	 *                              akan mengembalikan {@code null} sampai di-set ulang).
	 */
	public void setSyaratProdukKoperasis(Set<SyaratProdukKoperasi> syaratProdukKoperasis) {
		this.syaratProdukKoperasis = syaratProdukKoperasis;
	}

	/** Konstruktor kosong wajib Hibernate/JPA. Semua field diisi lewat setter atau di-load ORM. */
	public ProdukKoperasi() {
	}

	/**
	 * Primary key produk. {@code insertable = false} — kolom {@code id} diisi oleh sequence/serial
	 * database saat INSERT, bukan oleh Hibernate; nilai baru hanya tersedia setelah entity
	 * di-flush/reload.
	 *
	 * @return id produk, {@code null} untuk instance yang belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Kode singkat produk (opsional, dipakai pada cetakan/laporan ringkas). Null-safe: TIDAK
	 * PERNAH mengembalikan {@code null}, jatuh ke string kosong {@code ""} bila kolom kosong.
	 *
	 * @return kode ter-trim, atau {@code ""} bila kosong/{@code null}.
	 */
	public String getKode() {
		return kode == null || kode.isEmpty() ? "" : kode.trim();
	}

	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Nama produk — dipakai luas sebagai kunci pencocokan implisit oleh getter lain di kelas ini
	 * ({@link #getFormula()}, {@link #getOtomatisTerbentukTransaksi()},
	 * {@link #getJumlahTransaksiTerbentuk()}, {@link #getHanyaBolehSekaliTransaksi()} semuanya
	 * mencocokkan substring nama seperti {@code "simpanan pokok"}/{@code "simpanan wajib"} untuk
	 * auto-infer default). Konsekuensinya: mengganti nama produk existing (mis. dari "Simpanan
	 * Wajib" ke nama lain) bisa mengubah perilaku auto-infer pada pemanggilan getter berikutnya,
	 * meski konfigurasi eksplisit lain tidak disentuh.
	 *
	 * @return nama produk ter-trim, atau {@code null} bila kolom {@code null} (getter TIDAK
	 *         menjatuhkan ke {@code ""} seperti {@link #getKode()}).
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	public void setNama(String nama) {
		this.nama = nama;
	}

	/** Keterangan/deskripsi bebas produk, opsional. Tidak ada pemrosesan khusus pada getter. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Status aktif produk — mengendalikan apakah produk masih boleh dipilih untuk transaksi baru
	 * (transaksi lama yang sudah memakai produk ini tetap valid, lihat catatan live-read pada
	 * javadoc kelas). Default {@code true} bila kolom {@code null} (produk baru/legacy dianggap
	 * aktif, bukan nonaktif-diam-diam).
	 *
	 * @return {@code true} bila aktif atau kolom belum diisi.
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/** Urutan tampil produk pada daftar/dropdown UI. Default {@code 1} bila belum diisi. */
	public Integer getNomorUrut() {
		return nomorUrut == null ? 1 : nomorUrut;
	}

	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * Jangka waktu produk dalam bulan (tenor pinjaman, atau siklus untuk produk simpanan
	 * berjangka). Default {@code 1.0} bila belum diisi.
	 *
	 * <p><b>DIBACA LIVE oleh transaksi yang sudah dibuat</b> — {@link TransaksiKoperasi#getMargin()}
	 * memakai {@code produkKoperasi.getJangkaWaktuBulan()} SAAT DIPANGGIL (bukan snapshot nilai
	 * pada saat transaksi dibuat) sebagai faktor pengali margin flat, dan
	 * {@link TransaksiKoperasi#getTanggalTerakhirDiangsur()} (lihat javadoc-nya) memakainya untuk
	 * menghitung tanggal jatuh tempo angsuran terakhir. Mengubah nilai ini pada produk existing mengubah tampilan
	 * margin/jatuh tempo SELURUH transaksi historis yang memakai produk tsb — bukan bug, tapi
	 * konsekuensi arsitektur "konfigurasi produk sebagai sumber kebenaran live" yang sudah
	 * tercatat di javadoc kelas dan {@link TransaksiKoperasi#getMargin()}.
	 *
	 * @return jangka waktu dalam bulan, tidak pernah {@code null} (default {@code 1.0}).
	 */
	public Double getJangkaWaktuBulan() {
		return jangkaWaktuBulan == null ? 1.0 : jangkaWaktuBulan;
	}

	public void setJangkaWaktuBulan(Double jangkaWaktuBulan) {
		this.jangkaWaktuBulan = jangkaWaktuBulan;
	}

//	public Boolean getHitungBagiHasil() {
//		return hitungBagiHasil == null ? false : hitungBagiHasil;
//	}
//
//	public void setHitungBagiHasil(Boolean hitungBagiHasil) {
//		this.hitungBagiHasil = hitungBagiHasil;
//	}

	/**
	 * {@code true} bila produk ini ikut dihitung dalam alokasi SHU (Sisa Hasil Usaha) tahunan
	 * anggota. Default {@code true} bila kolom belum diisi (produk lama/legacy dianggap ikut
	 * SHU, bukan dikecualikan diam-diam).
	 */
	public Boolean getHitungShu() {
		return hitungShu == null ? true : hitungShu;
	}

	public void setHitungShu(Boolean hitungShu) {
		this.hitungShu = hitungShu;
	}

	/**
	 * Tipe produk (kategori PENAMBAHAN/PENGURANGAN posisi dana anggota) — menentukan arah jurnal
	 * pada mesin posting Simpan-Pinjam (dok 61 butir A, lihat {@link #getAkun()}) dan dipakai
	 * {@link #getHanyaBolehSekaliTransaksi()} untuk mendeteksi produk pinjaman. Relasi wajib
	 * ({@code nullable = false}); {@link #check(Object)} me-resolve proxy lazy Hibernate sebelum
	 * dikembalikan, konsisten dengan pola relasi lain di kelas ini.
	 *
	 * @return tipe produk, di-resolve dari proxy lazy bila perlu.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tipe_produk_koperasi", nullable = false)
	public TipeProdukKoperasi getTipeProdukKoperasi() {
		tipeProdukKoperasi = check(tipeProdukKoperasi);
		return tipeProdukKoperasi;
	}

	public void setTipeProdukKoperasi(TipeProdukKoperasi tipeProdukKoperasi) {
		this.tipeProdukKoperasi = tipeProdukKoperasi;
	}

	/** Representasi JSON kosong ({@code "[]"}) — nilai sentinel yang menandakan {@link #formula}
	 * belum pernah diisi eksplisit, dipakai sebagai pemicu auto-seed pada {@link #getFormula()}. */
	public static String DEFAULT_FORMULA = new JSONArray().toString();

	/**
	 * Formula/template baris transaksi (JSON array) yang dipakai UI untuk mem-prefill item saat
	 * anggota bertransaksi dengan produk ini — tiap elemen memuat {@code nama}/{@code harga}/
	 * {@code jumlah}/{@code jenisTransaksiKoperasi}/{@code qty}/{@code boleh} (apakah baris boleh
	 * diedit pengguna)/{@code key} (id unik acak untuk keperluan UI).
	 *
	 * <p><b>AUTO-SEED BERDASARKAN NAMA, DIEKSEKUSI DI DALAM GETTER (side effect pada baris yang
	 * sudah tersimpan).</b> Bila {@link #getId()} tidak {@code null} (baris sudah persisten) DAN
	 * {@link #formula} masih kosong/{@link #DEFAULT_FORMULA}, getter ini MENULIS formula default
	 * ke field instance berdasarkan pencocokan case-insensitive {@link #getNama()}:
	 * <ul>
	 * <li>{@code "Simpanan Pokok"} → baris {@link ConstantValues#SIMPANAN_POKOK} Rp 10.000 (tidak
	 * boleh diedit) + {@link ConstantValues#BIAYA_ADMIN} Rp 500 (tidak boleh diedit);</li>
	 * <li>{@code "Simpanan Wajib"} → baris {@link ConstantValues#SIMPANAN_WAJIB} harga tampil
	 * Rp 10.000 tetapi {@code jumlah} Rp 5.000 (lihat catatan ketidaksesuaian di bawah) + biaya
	 * admin Rp 500, keduanya tidak boleh diedit;</li>
	 * <li>nama lain → satu baris {@link ConstantValues#BIAYA_ADMIN} Rp 0 yang BOLEH diedit
	 * (template generik, jumlah diisi pengguna saat transaksi).</li>
	 * </ul>
	 * Karena entity ini {@code dynamicUpdate = true}, pemanggilan getter ini SEBELUM flush
	 * Hibernate (mis. saat menyusun tampilan cetak/laporan yang memanggil getter demi getter)
	 * bisa membuat formula auto-seed ikut TERSIMPAN ke database meskipun admin tidak pernah
	 * membuka layar edit produk untuk mengisinya — pola getter-yang-memutasi-state yang sama
	 * dengan {@link #getOtomatisTerbentukTransaksi()}/{@link #getJumlahTransaksiTerbentuk()}/
	 * {@link #getHanyaBolehSekaliTransaksi()} di kelas ini.
	 *
	 * <p><b>Ketidaksesuaian harga/jumlah pada template "Simpanan Wajib":</b> {@code harga} JSON
	 * bernilai 10.000 tetapi {@code jumlah} bernilai 5.000 — bila UI menampilkan {@code harga}
	 * sebagai label dan {@code jumlah} sebagai nominal transaksi aktual, anggota akan melihat
	 * label "Rp 10.000" untuk transaksi yang sebenarnya bernilai Rp 5.000. Perilaku warisan,
	 * TIDAK diubah di sini — perbaikan memerlukan konfirmasi apakah ini kesalahan input historis
	 * atau desain sengaja (mis. harga referensi vs jumlah minimum).
	 *
	 * <p>Bila {@link #getId()} {@code null} (entity belum tersimpan) auto-seed TIDAK dijalankan;
	 * getter tetap mengembalikan {@link #DEFAULT_FORMULA} bila {@link #formula} kosong.
	 *
	 * @return formula JSON array, tidak pernah {@code null} (jatuh ke {@link #DEFAULT_FORMULA}
	 *         string {@code "[]"} bila kosong dan tidak memenuhi kondisi auto-seed).
	 */
	@Column(name = "formula", nullable = true, columnDefinition = "text")
	public String getFormula() {

		if (getId() != null) {
			if ((formula == null || formula.trim().isEmpty() || formula.trim().equalsIgnoreCase(DEFAULT_FORMULA))
					&& getNama() != null && getNama().equalsIgnoreCase("Simpanan Pokok")) {
				formula = "[{\"nama\":\"\",\"harga\":10000,\"jumlah\":10000,\"jenisTransaksiKoperasi\":"
						+ ConstantValues.SIMPANAN_POKOK.getId() + ",\"qty\":1,\"boleh\":false,\"key\":"
						+ Math.abs(Common.randLong())
						+ "},{\"nama\":\"\",\"harga\":500,\"jumlah\":500,\"jenisTransaksiKoperasi\":"
						+ ConstantValues.BIAYA_ADMIN.getId() + ",\"qty\":1,\"boleh\":false,\"key\":"
						+ Math.abs(Common.randLong()) + "}]";
			} else if ((formula == null || formula.trim().isEmpty() || formula.trim().equalsIgnoreCase(DEFAULT_FORMULA))
					&& getNama() != null && getNama().equalsIgnoreCase("Simpanan Wajib")) {
				formula = "[{\"nama\":\"\",\"harga\":10000,\"jumlah\":5000,\"jenisTransaksiKoperasi\":"
						+ ConstantValues.SIMPANAN_WAJIB.getId() + ",\"qty\":1,\"boleh\":false,\"key\":"
						+ Math.abs(Common.randLong())
						+ "},{\"nama\":\"\",\"harga\":500,\"jumlah\":500,\"jenisTransaksiKoperasi\":"
						+ ConstantValues.BIAYA_ADMIN.getId() + ",\"qty\":1,\"boleh\":false,\"key\":"
						+ Math.abs(Common.randLong()) + "}]";
			} else if (formula == null || formula.trim().isEmpty()
					|| formula.trim().equalsIgnoreCase(DEFAULT_FORMULA)) {
				formula = "[{\"nama\":\"\",\"harga\":0,\"jumlah\":0,\"jenisTransaksiKoperasi\":"
						+ ConstantValues.BIAYA_ADMIN.getId() + ",\"qty\":1,\"boleh\":true,\"key\":"
						+ Math.abs(Common.randLong()) + "}]";
			}
		}
		return formula == null || formula.isEmpty() ? DEFAULT_FORMULA : formula;
	}

	/**
	 * Menetapkan formula secara eksplisit — dipanggil layar edit produk saat admin menyusun
	 * template baris transaksi manual. Men-set nilai apa adanya, TIDAK memvalidasi bentuk JSON;
	 * validasi/parsing dilakukan pemanggil ({@code ProdukKoperasiAction}) sebelum simpan.
	 *
	 * @param formula string JSON array baris transaksi; boleh {@code null}/kosong (akan memicu
	 *                auto-seed pada pemanggilan {@link #getFormula()} berikutnya bila entity
	 *                sudah persisten).
	 */
	public void setFormula(String formula) {
		this.formula = formula;
	}

	/**
	 * Koperasi (unit organisasi/tenant) pemilik produk ini. Relasi wajib ({@code nullable = false})
	 * — setiap produk selalu berada di bawah satu koperasi, dipakai sebagai batas lingkup saat
	 * memfilter daftar produk yang boleh dipilih anggota koperasi tsb. {@link #check(Object)}
	 * me-resolve proxy lazy Hibernate sebelum dikembalikan.
	 *
	 * @return koperasi pemilik, di-resolve dari proxy lazy bila perlu.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "koperasi", nullable = false)
	public Koperasi getKoperasi() {
		koperasi = check(koperasi);
		return koperasi;
	}

	public void setKoperasi(Koperasi koperasi) {
		this.koperasi = koperasi;
	}

	/**
	 * Nomor surat (dokumen resmi/SK) terkait penetapan produk ini, opsional. Dipakai
	 * {@code ProdukKoperasiAction} lewat atribut {@code "nomorSurat"} pada layar edit untuk
	 * mengaitkan produk dengan penomoran surat auto-generate milik {@link NomorSurat}.
	 * {@link #check(Object)} me-resolve proxy lazy Hibernate sebelum dikembalikan.
	 *
	 * @return nomor surat terkait, atau {@code null} bila tidak diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "nomor_surat", nullable = true)
	public NomorSurat getNomorSurat() {
		nomorSurat = check(nomorSurat);
		return nomorSurat;
	}

	public void setNomorSurat(NomorSurat nomorSurat) {
		this.nomorSurat = nomorSurat;
	}

	/**
	 * Setoran awal/minimal wajib saat anggota pertama kali mengambil produk ini (mis. nominal
	 * simpanan pokok). Default {@code 0.0} bila belum diisi.
	 */
	public Double getSetoran() {
		return setoran == null ? 0.0 : setoran;
	}

	public void setSetoran(Double setoran) {
		this.setoran = setoran;
	}

	/**
	 * Suku bunga/margin produk (persen). Default {@code 0.0} bila belum diisi.
	 *
	 * <p><b>DIBACA LIVE oleh transaksi yang sudah dibuat — bukan snapshot.</b>
	 * {@link TransaksiKoperasi#getMargin()} menghitung
	 * {@code nilai * (produkKoperasi.getBunga() / 100.0) * produkKoperasi.getJangkaWaktuBulan()}
	 * memakai nilai bunga produk SAAT getter dipanggil. Mengubah {@link #getBunga()} pada produk
	 * yang sudah dipakai transaksi historis akan mengubah margin/total tampilan transaksi TERSEBUT
	 * secara retroaktif, karena tidak ada snapshot bunga per transaksi — lihat catatan lengkap
	 * (termasuk implikasi pada pemecahan jadwal angsuran) di {@link TransaksiKoperasi#getMargin()}
	 * dan {@link TransaksiKoperasi#getTotal()}. Ini adalah konsekuensi arsitektur yang sudah
	 * tercatat, bukan bug baru — perubahan bunga produk SENGAJA memengaruhi seluruh transaksi
	 * yang masih merujuk produk tsb sampai ada mekanisme snapshot-per-transaksi.
	 *
	 * @return suku bunga dalam persen, tidak pernah {@code null} (default {@code 0.0}).
	 */
	public Double getBunga() {
		return bunga == null ? 0.0 : bunga;
	}

	public void setBunga(Double bunga) {
		this.bunga = bunga;
	}

	/**
	 * Batas nilai transaksi maksimal yang boleh dilayani produk ini. Default sangat besar
	 * ({@code 10.000.000.000,0} — 10 miliar) bila belum diisi, sehingga produk lama/legacy
	 * yang belum mengisi kolom ini efektif TIDAK terbatasi.
	 */
	public Double getNilaiMaksimal() {
		return nilaiMaksimal == null ? 10000000000.0 : nilaiMaksimal;
	}

	public void setNilaiMaksimal(Double nilaiMaksimal) {
		this.nilaiMaksimal = nilaiMaksimal;
	}

	/** Batas nilai transaksi minimal yang boleh dilayani produk ini. Default {@code 0.0} (tanpa
	 * batas bawah) bila belum diisi. */
	public Double getNilaiMinimal() {
		return nilaiMinimal == null ? 0.0 : nilaiMinimal;
	}

	public void setNilaiMinimal(Double nilaiMinimal) {
		this.nilaiMinimal = nilaiMinimal;
	}

	/**
	 * Siklus durasi produk: salah satu {@link #HARIAN}/{@link #MINGGUAN}/{@link #BULANAN}/
	 * {@link #TAHUNAN} (bebas string lain, tidak divalidasi terhadap konstanta). Default
	 * {@link #BULANAN} bila kolom kosong/{@code null}.
	 */
	public String getDurasi() {
		return durasi == null || durasi.trim().isEmpty() ? BULANAN : durasi;
	}

	public void setDurasi(String durasi) {
		this.durasi = durasi;
	}

	/**
	 * Siklus penghitungan bunga: salah satu {@link #HARIAN}/{@link #MINGGUAN}/{@link #BULANAN}/
	 * {@link #TAHUNAN}. Default {@link #BULANAN} bila kolom kosong/{@code null}. Dipakai
	 * {@link TransaksiKoperasi} (cabang {@code equalsIgnoreCase(ProdukKoperasi.BULANAN)}) untuk
	 * menentukan satuan penambahan tanggal saat menghitung jatuh tempo angsuran.
	 */
	public String getPenghitunganBunga() {
		return penghitunganBunga == null || penghitunganBunga.trim().isEmpty() ? BULANAN : penghitunganBunga;
	}

	public void setPenghitunganBunga(String penghitunganBunga) {
		this.penghitunganBunga = penghitunganBunga;
	}

	/**
	 * {@code true} bila transaksi produk ini dibentuk otomatis oleh sistem (mis. penjadwal
	 * simpanan wajib bulanan) alih-alih diinput manual anggota per transaksi.
	 *
	 * <p><b>Auto-infer dari nama, dieksekusi di dalam getter.</b> Bila field belum pernah di-set
	 * eksplisit ({@code null}), getter menebak {@code true} bila {@link #getNama()} mengandung
	 * substring {@code "simpanan"} (case-insensitive) — MENULIS hasil tebakan ke field instance
	 * secara permanen pada pemanggilan pertama. Pola sama seperti {@link #getFormula()}/
	 * {@link #getJumlahTransaksiTerbentuk()}/{@link #getHanyaBolehSekaliTransaksi()}: pada entity
	 * {@code dynamicUpdate = true}, pemanggilan getter sebelum flush bisa membuat tebakan ini
	 * ikut tersimpan ke database tanpa admin pernah mengisinya lewat UI. Setelah nilai eksplisit
	 * (dari database maupun tebakan sebelumnya) tersimpan di field, cabang tebakan tidak lagi
	 * dieksekusi (hanya berjalan saat field masih {@code null}).
	 *
	 * @return {@code true} bila otomatis terbentuk (eksplisit atau hasil tebakan nama), {@code false}
	 *         bila field {@code null} dan nama tidak mengandung "simpanan".
	 */
	public Boolean getOtomatisTerbentukTransaksi() {

		if (otomatisTerbentukTransaksi == null) {
			if (getNama() != null && getNama().toLowerCase().contains("simpanan")) {
				otomatisTerbentukTransaksi = true;
			}
		}

		return otomatisTerbentukTransaksi == null ? false : otomatisTerbentukTransaksi;
	}

	public void setOtomatisTerbentukTransaksi(Boolean otomatisTerbentukTransaksi) {
		this.otomatisTerbentukTransaksi = otomatisTerbentukTransaksi;
	}

	/**
	 * Jumlah transaksi yang akan dibentuk otomatis (relevan hanya bila
	 * {@link #getOtomatisTerbentukTransaksi()} {@code true}) — mis. 12 untuk simpanan wajib
	 * bulanan selama setahun.
	 *
	 * <p>Auto-infer sama seperti {@link #getOtomatisTerbentukTransaksi()}: bila field masih
	 * {@code null}, getter menebak {@code 12} bila {@link #getNama()} mengandung
	 * {@code "simpanan wajib"} (case-insensitive) dan MENULIS tebakan itu ke field instance.
	 * Fallback akhir (bila tebakan tidak berlaku) adalah {@code 1}, BUKAN {@code 0} — produk yang
	 * tidak cocok kriteria tebakan tetap dianggap membentuk minimal satu transaksi.
	 *
	 * @return jumlah transaksi yang dibentuk otomatis, tidak pernah {@code null} (default {@code 1}).
	 */
	public Integer getJumlahTransaksiTerbentuk() {
		if (jumlahTransaksiTerbentuk == null) {
			if (getNama() != null && getNama().toLowerCase().contains("simpanan wajib")) {
				jumlahTransaksiTerbentuk = 12;
			}
		}

		return jumlahTransaksiTerbentuk == null ? 1 : jumlahTransaksiTerbentuk;
	}

	public void setJumlahTransaksiTerbentuk(Integer jumlahTransaksiTerbentuk) {
		this.jumlahTransaksiTerbentuk = jumlahTransaksiTerbentuk;
	}

	/**
	 * {@code true} bila anggota hanya boleh bertransaksi SATU KALI dengan produk ini (mis.
	 * simpanan pokok yang lazimnya disetor sekali di awal keanggotaan).
	 *
	 * <p>Kombinasi dua mekanisme, keduanya dieksekusi di dalam getter:
	 * <ol>
	 * <li><b>Auto-infer dari nama</b> (hanya bila field masih {@code null}): menebak {@code true}
	 * bila {@link #getNama()} mengandung {@code "simpanan pokok"} (case-insensitive), lalu MENULIS
	 * tebakan itu ke field instance — pola sama dengan getter auto-infer lain di kelas ini.</li>
	 * <li><b>Guardrail tipe produk PINJAMAN, dieksekusi SETIAP PANGGILAN tanpa syarat null-check</b>:
	 * bila {@link #getTipeProdukKoperasi()} sama dengan {@link ConstantValues#PINJAMAN}, field
	 * DIPAKSA {@code false} — TERLEPAS dari nilai eksplisit apa pun yang sebelumnya di-set lewat
	 * {@link #setHanyaBolehSekaliTransaksi(Boolean)} (mis. dari checkbox UI). Ini disengaja:
	 * produk pinjaman secara bisnis selalu perlu bisa ditransaksikan berulang (pencairan,
	 * angsuran), jadi "hanya boleh sekali" tidak masuk akal untuk PINJAMAN. TAPI konsekuensinya,
	 * bila getter ini terpanggil sebelum flush pada entity {@code dynamicUpdate = true} (mis.
	 * Hibernate membaca dirty properties saat commit), nilai {@code false} hasil paksaan ini bisa
	 * ikut tersimpan permanen ke database, menimpa apa pun yang admin coba set.</li>
	 * </ol>
	 *
	 * @return {@code true} hanya untuk produk non-PINJAMAN yang eksplisit/tersimpulkan demikian;
	 *         selalu {@code false} untuk produk bertipe PINJAMAN.
	 */
	public Boolean getHanyaBolehSekaliTransaksi() {
		if (hanyaBolehSekaliTransaksi == null) {
			if (getNama() != null && getNama().toLowerCase().contains("simpanan pokok")) {
				hanyaBolehSekaliTransaksi = true;
			}
		}

		if (getTipeProdukKoperasi() != null && ConstantValues.PINJAMAN != null
				&& getTipeProdukKoperasi().getId().equals(ConstantValues.PINJAMAN.getId())) {
			hanyaBolehSekaliTransaksi = false;
		}

		return hanyaBolehSekaliTransaksi == null ? false : hanyaBolehSekaliTransaksi;
	}

	public void setHanyaBolehSekaliTransaksi(Boolean hanyaBolehSekaliTransaksi) {
		this.hanyaBolehSekaliTransaksi = hanyaBolehSekaliTransaksi;
	}

	// ═══════════════════════════════════════════════════════════════════════════════════════
	// Parameter pinjaman tambahan (SOM USPK) — hasil merge eks-ProdukPinjaman ke sini agar tidak
	// ada model ganda. Semua kolom nullable & getter null-safe, sehingga perilaku produk lama
	// (yang belum mengisi field ini) tetap sama persis.
	// ═══════════════════════════════════════════════════════════════════════════════════════

	/**
	 * Metode perhitungan bunga pinjaman: {@link #METODE_FLAT} atau {@link #METODE_MENURUN}. Default
	 * {@link #METODE_FLAT} agar konsisten dengan perhitungan margin historis pada
	 * {@link TransaksiKoperasi#getMargin()} (flat: pokok awal × bunga% × jangka waktu).
	 */
	@Column(name = "metode_bunga", length = 20)
	public String getMetodeBunga() {
		return metodeBunga == null || metodeBunga.trim().isEmpty() ? METODE_FLAT : metodeBunga;
	}

	public void setMetodeBunga(String metodeBunga) {
		this.metodeBunga = metodeBunga;
	}

	/** Provisi (persen dari pokok) yang dipungut di muka saat pencairan pinjaman. */
	@Column(name = "provisi_persen")
	public Double getProvisiPersen() {
		return provisiPersen == null ? 0.0 : provisiPersen;
	}

	public void setProvisiPersen(Double provisiPersen) {
		this.provisiPersen = provisiPersen;
	}

	/** Biaya administrasi tetap (rupiah) saat pencairan pinjaman. */
	@Column(name = "biaya_admin")
	public Double getBiayaAdmin() {
		return biayaAdmin == null ? 0.0 : biayaAdmin;
	}

	public void setBiayaAdmin(Double biayaAdmin) {
		this.biayaAdmin = biayaAdmin;
	}

	/** {@code true} bila produk pinjaman ini mensyaratkan agunan. */
	@Column(name = "wajib_agunan")
	public Boolean getWajibAgunan() {
		return wajibAgunan == null ? false : wajibAgunan;
	}

	public void setWajibAgunan(Boolean wajibAgunan) {
		this.wajibAgunan = wajibAgunan;
	}

	/** Denda keterlambatan per hari (persen dari nilai angsuran) untuk produk pinjaman. */
	@Column(name = "denda_persen_per_hari")
	public Double getDendaPersenPerHari() {
		return dendaPersenPerHari == null ? 0.0 : dendaPersenPerHari;
	}

	public void setDendaPersenPerHari(Double dendaPersenPerHari) {
		this.dendaPersenPerHari = dendaPersenPerHari;
	}

	/**
	 * Metode perhitungan bunga simpanan untuk produk simpanan sukarela/berjangka:
	 * {@link #BUNGA_SIMPANAN_SALDO_TERENDAH}, {@link #BUNGA_SIMPANAN_SALDO_HARIAN}, atau
	 * {@link #BUNGA_SIMPANAN_SALDO_RATA_RATA}. Default {@link #BUNGA_SIMPANAN_SALDO_RATA_RATA}
	 * karena paling adil dan lazim dipakai koperasi. Nilai selalu aman-null.
	 */
	@Column(name = "metode_bunga_simpanan", length = 20)
	public String getMetodeBungaSimpanan() {
		return metodeBungaSimpanan == null || metodeBungaSimpanan.trim().isEmpty() ? BUNGA_SIMPANAN_SALDO_RATA_RATA
				: metodeBungaSimpanan;
	}

	public void setMetodeBungaSimpanan(String metodeBungaSimpanan) {
		this.metodeBungaSimpanan = metodeBungaSimpanan;
	}

	/** Suku bunga simpanan per tahun (persen) untuk produk simpanan sukarela/berjangka. Aman-null. */
	@Column(name = "bunga_simpanan_persen")
	public Double getBungaSimpananPersen() {
		return bungaSimpananPersen == null ? 0.0 : bungaSimpananPersen;
	}

	public void setBungaSimpananPersen(Double bungaSimpananPersen) {
		this.bungaSimpananPersen = bungaSimpananPersen;
	}

	private ais.database.model.akunting.Akun akun;
	private ais.database.model.akunting.Akun akunMargin;

	/**
	 * Akun posisi dana produk pada buku besar: kewajiban simpanan anggota untuk produk
	 * bertipe PENAMBAHAN, atau piutang pembiayaan untuk produk bertipe PENGURANGAN.
	 * Dipakai mesin posting Simpan-Pinjam di dasbor Draft Jurnal (dok 61 butir A) --
	 * produk tanpa akun ini dilewati mesin dan tetap terhitung draf.
	 */
	@javax.persistence.ManyToOne(fetch = javax.persistence.FetchType.LAZY)
	@javax.persistence.JoinColumn(name = "akun", nullable = true)
	public ais.database.model.akunting.Akun getAkun() {
		akun = check(akun);
		return akun;
	}

	public void setAkun(ais.database.model.akunting.Akun akun) {
		this.akun = akun;
	}

	/**
	 * Akun pendapatan margin/bunga produk. BELUM dipakai mesin posting dokumen pengajuan
	 * (margin diakui saat angsuran diterima, keluarga PembayaranAnggotaKoperasi -- dok 61
	 * butir B); disediakan sekarang supaya master siap saat jurnal angsuran dibuat.
	 */
	@javax.persistence.ManyToOne(fetch = javax.persistence.FetchType.LAZY)
	@javax.persistence.JoinColumn(name = "akun_margin", nullable = true)
	public ais.database.model.akunting.Akun getAkunMargin() {
		akunMargin = check(akunMargin);
		return akunMargin;
	}

	public void setAkunMargin(ais.database.model.akunting.Akun akunMargin) {
		this.akunMargin = akunMargin;
	}

}
