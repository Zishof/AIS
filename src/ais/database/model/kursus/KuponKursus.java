package ais.database.model.kursus;

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

/**
 * Kode kupon diskon untuk checkout {@link ProdukKursus}. {@code produkKursus} {@code null} =
 * berlaku untuk semua kursus.
 *
 * <h3>Verifikasi terhadap pola bug kupon/diskon yang tercatat di domain lain</h3>
 * <p>Modul lain (SIRS/Apotik, lihat {@code ais.database.model.sirs.Diskon}) pernah ditemukan
 * mengalami "diskon mustahil diberikan" akibat nilai awal field mendahului nilai cadangan getter
 * (mis. {@code jumlahMaksimal} berawal {@code 0} padahal getternya bermaksud {@code 100}) serta
 * batas tanggal berlaku yang tidak ditandai {@code @Temporal(TemporalType.DATE)} sehingga langsung
 * kedaluwarsa begitu tersimpan. Kelas ini sudah diverifikasi <b>TIDAK</b> mengulang kedua pola
 * tersebut: {@link #nilai}, {@link #berlakuMulai}, dan {@link #berlakuSampai} semuanya TANPA
 * inisialisasi field (default {@code null} Java biasa, bukan {@code 0}/{@code new Date()} yang
 * mendahului nilai cadangan getter), dan {@link #getBerlakuMulai()}/{@link #getBerlakuSampai()}
 * sama-sama ditandai {@link TemporalType#DATE}.</p>
 *
 * <h3>Namun {@link #berlakuUntuk(Date)} punya bug batas akhir off-by-one-hari yang berbeda</h3>
 * <p>{@link #berlakuSampai} dipetakan {@code DATE} sehingga nilainya (baik yang baru dimuat dari
 * basis data maupun yang di-set lewat form tanggal) secara efektif berkomponen jam
 * {@code 00:00:00} — tengah malam awal hari itu. {@link #berlakuUntuk(Date)} membandingkannya
 * langsung terhadap {@code sekarang} (stempel waktu PENUH, mis. jam 14:00) memakai
 * {@code sekarang.after(berlakuSampai)}. Karena {@code 14:00} pada tanggal akhir masa berlaku
 * sudah "setelah" tengah malam tanggal yang sama, method ini menyatakan kupon SUDAH kedaluwarsa
 * sepanjang hari terakhir masa berlakunya (kecuali sesaat tepat pukul {@code 00:00:00}) — padahal
 * maksud kolom {@code berlakuSampai} ("berlaku sampai [tanggal]") secara wajar berarti kupon masih
 * sah SEPANJANG hari itu. Efeknya peserta yang mencoba memakai kupon pada hari terakhir yang
 * dijanjikan akan ditolak lebih awal dari yang diharapkan. Ini pola "kedaluwarsa" yang berbeda
 * mekanismenya dari bug {@code Diskon} (di sana penyebabnya anotasi {@code @Temporal} yang hilang;
 * di sini anotasinya justru ada dan benar, tetapi logika perbandingannya tidak menoleransi
 * komponen jam pada {@code sekarang}). Sisi awal masa berlaku tidak mengalami masalah simetris:
 * {@code sekarang.before(berlakuMulai)} pada tanggal mulai yang sama (jam berapa pun setelah tengah
 * malam) tetap {@code false}, sehingga kupon sudah sah sejak awal hari mulainya.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "kupon_kursus")
public class KuponKursus extends GeneralValueObject {

	/** Jenis diskon: potongan berupa persentase dari harga; nilai cadangan {@link #getTipeDiskon()}. */
	public final static String PERSEN = "Persen";
	/** Jenis diskon: potongan berupa nominal rupiah tetap. */
	public final static String NOMINAL = "Nominal";

	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key baris {@code kupon_kursus}, dibangkitkan basis data (IDENTITY). */
	private Long id;
	/** Nama pengguna yang terakhir mengubah baris ini — field audit bayangan. */
	private String oleh;
	/** Id pengguna yang terakhir mengubah baris ini — field audit bayangan. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah kupon ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna pengubah terakhir. Nilai {@code null}/kosong/spasi diabaikan diam-diam
	 * agar jejak audit yang sudah terisi tidak terhapus oleh jalur simpan tanpa identitas pengguna.
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna pengubah terakhir, dengan validasi non-trivial yang sama seperti
	 * {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah kupon ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate}: mendelegasikan pencatatan stempel audit ke
	 * {@code AuditTimestampInterceptor.ubah(this)} setiap kali baris ini diperbarui.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu perubahan terakhir. Diinisialisasi ke waktu pembuatan object memakai
	 * {@code WaktuUtil.getDate()}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi stempel waktu perubahan terakhir. Tanpa validasi.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir, dipetakan sebagai {@code TIMESTAMP}.
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks ringkas kupon: {@code "id-nama"}.
	 *
	 * @return gabungan id dan nama kupon
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Kode kupon yang dimasukkan peserta saat checkout; lihat {@link #getKode()} untuk normalisasi kapitalisasi. */
	private String kode;
	/** Nama/label kupon untuk tampilan admin. */
	private String nama;
	/** Keterangan bebas kupon (kolom bertipe {@code text}). */
	private String keterangan;
	/** Jenis diskon; lihat {@link #PERSEN}/{@link #NOMINAL} dan {@link #getTipeDiskon()} untuk nilai cadangan. */
	private String tipeDiskon;
	/**
	 * Besaran diskon: persentase (0-100, tanpa validasi rentang di kelas ini) bila
	 * {@link #tipeDiskon} = {@link #PERSEN}, atau nominal rupiah bila {@link #NOMINAL}. TANPA
	 * inisialisasi field (default {@code null} Java) — lihat javadoc kelas soal verifikasi pola bug.
	 */
	private Double nilai;
	/**
	 * Awal masa berlaku kupon, dipetakan {@code DATE}. TANPA inisialisasi field — {@code null}
	 * berarti tidak ada batas awal (lihat {@link #berlakuUntuk(Date)}).
	 */
	private Date berlakuMulai;
	/**
	 * Akhir masa berlaku kupon, dipetakan {@code DATE}. TANPA inisialisasi field — {@code null}
	 * berarti tidak ada batas akhir. Lihat javadoc kelas soal bug off-by-one-hari pada
	 * {@link #berlakuUntuk(Date)} yang timbul dari perbandingan field ini terhadap stempel waktu
	 * penuh.
	 */
	private Date berlakuSampai;
	/** Batas jumlah pemakaian kupon secara keseluruhan; {@code null} berarti tanpa batas. */
	private Integer batasPemakaian;
	/** Jumlah pemakaian kupon sejauh ini; {@code null} dianggap {@code 0} oleh {@link #getJumlahDipakai()}. */
	private Integer jumlahDipakai;
	/** Produk kursus yang dicakup kupon ini; {@code null} berarti berlaku untuk semua produk kursus. */
	private ProdukKursus produkKursus;
	/** Status aktif/nonaktif kupon; {@code null} dianggap aktif oleh {@link #getAktif()}. */
	private Boolean aktif;

	/** Konstruktor kosong yang diwajibkan JPA/Hibernate. */
	public KuponKursus() {
	}

	/**
	 * Mengembalikan primary key kupon.
	 *
	 * @return primary key, atau {@code null} bila belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key. Tanpa validasi; normalnya diisi otomatis oleh Hibernate.
	 *
	 * @param id primary key baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kode kupon, menormalkan {@code null} menjadi string kosong, memangkas spasi
	 * tepi, dan MENGUBAH KE HURUF KAPITAL. Normalisasi kapital ini membuat pencocokan kode kupon
	 * dari input peserta (apa pun kapitalisasinya) konsisten selama pemanggil juga
	 * membandingkannya lewat getter ini (atau ikut meng-kapital-kan input sebelum query); kolom
	 * {@code kode} sendiri {@code unique = true} pada basis data, jadi keunikan ditegakkan pada
	 * nilai APA ADANYA yang tersimpan — dua kode yang hanya berbeda kapitalisasi (mis. "DISKON10"
	 * vs "diskon10") tetap dapat tersimpan sebagai dua baris berbeda bila disimpan tanpa melalui
	 * normalisasi getter ini terlebih dahulu (mis. lewat SQL langsung atau import massal).
	 *
	 * @return kode kupon (dipangkas, huruf kapital), tidak pernah {@code null}
	 */
	@Column(name = "kode", nullable = false, unique = true, length = 100)
	public String getKode() {
		return kode == null ? "" : kode.trim().toUpperCase();
	}

	/**
	 * Mengisi kode kupon. Nilai yang diisi di sini belum dikapitalkan — kapitalisasi baru terjadi
	 * saat dibaca lewat {@link #getKode()}.
	 *
	 * @param kode kode kupon baru
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan nama/label kupon, dipangkas spasi tepi.
	 *
	 * @return nama kupon (dipangkas), atau {@code null} bila belum pernah diisi
	 */
	@Column(name = "nama", nullable = true, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi nama/label kupon.
	 *
	 * @param nama nama kupon baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas kupon. Getter murni-baca, tanpa normalisasi.
	 *
	 * @return keterangan, atau {@code null} bila tidak diisi
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi keterangan bebas kupon.
	 *
	 * @param keterangan keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan jenis diskon kupon, menormalkan {@code null}/kosong menjadi {@link #PERSEN}.
	 *
	 * @return jenis diskon ({@link #PERSEN} atau {@link #NOMINAL}), tidak pernah {@code null}/kosong
	 */
	@Column(name = "tipe_diskon", nullable = true, length = 50)
	public String getTipeDiskon() {
		return tipeDiskon == null || tipeDiskon.isEmpty() ? PERSEN : tipeDiskon;
	}

	/**
	 * Mengisi jenis diskon kupon. Tanpa validasi bahwa nilainya {@link #PERSEN} atau {@link #NOMINAL}.
	 *
	 * @param tipeDiskon jenis diskon baru
	 */
	public void setTipeDiskon(String tipeDiskon) {
		this.tipeDiskon = tipeDiskon;
	}

	/**
	 * Mengembalikan besaran diskon kupon, menormalkan {@code null} menjadi {@code 0.0}. Maknanya
	 * bergantung {@link #getTipeDiskon()}: persentase (idealnya 0-100, TANPA validasi rentang di
	 * kelas ini — nilai di atas 100 atau negatif dapat tersimpan) bila {@link #PERSEN}, atau
	 * nominal rupiah bila {@link #NOMINAL}.
	 *
	 * @return besaran diskon, tidak pernah {@code null}
	 */
	public Double getNilai() {
		return nilai == null ? 0.0 : nilai;
	}

	/**
	 * Mengisi besaran diskon kupon.
	 *
	 * @param nilai besaran diskon baru; tidak divalidasi rentangnya
	 */
	public void setNilai(Double nilai) {
		this.nilai = nilai;
	}

	/**
	 * Mengembalikan awal masa berlaku kupon, dipetakan {@code DATE}. Getter murni-baca tanpa nilai
	 * cadangan; {@code null} berarti tanpa batas awal (kupon sah sejak kapan pun) — lihat
	 * {@link #berlakuUntuk(Date)}.
	 *
	 * @return awal masa berlaku, atau {@code null} bila tanpa batas awal
	 */
	@Temporal(TemporalType.DATE)
	public Date getBerlakuMulai() {
		return berlakuMulai;
	}

	/**
	 * Mengisi awal masa berlaku kupon.
	 *
	 * @param berlakuMulai awal masa berlaku baru, atau {@code null} untuk tanpa batas awal
	 */
	public void setBerlakuMulai(Date berlakuMulai) {
		this.berlakuMulai = berlakuMulai;
	}

	/**
	 * Mengembalikan akhir masa berlaku kupon, dipetakan {@code DATE}. Getter murni-baca tanpa nilai
	 * cadangan; {@code null} berarti tanpa batas akhir. Lihat javadoc kelas soal bug
	 * off-by-one-hari pada {@link #berlakuUntuk(Date)}: nilai {@code DATE} (berkomponen jam tengah
	 * malam) dari field ini dibandingkan langsung terhadap stempel waktu penuh, sehingga kupon
	 * tampak kedaluwarsa sepanjang hari terakhir masa berlakunya, bukan baru keesokan harinya.
	 *
	 * @return akhir masa berlaku, atau {@code null} bila tanpa batas akhir
	 */
	@Temporal(TemporalType.DATE)
	public Date getBerlakuSampai() {
		return berlakuSampai;
	}

	/**
	 * Mengisi akhir masa berlaku kupon.
	 *
	 * @param berlakuSampai akhir masa berlaku baru, atau {@code null} untuk tanpa batas akhir
	 */
	public void setBerlakuSampai(Date berlakuSampai) {
		this.berlakuSampai = berlakuSampai;
	}

	/**
	 * Mengembalikan batas jumlah pemakaian kupon secara keseluruhan. Getter murni-baca; {@code null}
	 * berarti tanpa batas jumlah pemakaian.
	 *
	 * @return batas pemakaian, atau {@code null} bila tanpa batas
	 */
	public Integer getBatasPemakaian() {
		return batasPemakaian;
	}

	/**
	 * Mengisi batas jumlah pemakaian kupon secara keseluruhan.
	 *
	 * @param batasPemakaian batas pemakaian baru, atau {@code null} untuk tanpa batas
	 */
	public void setBatasPemakaian(Integer batasPemakaian) {
		this.batasPemakaian = batasPemakaian;
	}

	/**
	 * Mengembalikan jumlah pemakaian kupon sejauh ini, menormalkan {@code null} menjadi {@code 0}.
	 * Kelas ini sendiri tidak menaikkan nilai ini secara otomatis saat kupon dipakai — pemanggil
	 * (jalur checkout) bertanggung jawab memanggil {@link #setJumlahDipakai(Integer)} dengan nilai
	 * bertambah setiap kali kupon berhasil dipakai.
	 *
	 * @return jumlah pemakaian, tidak pernah {@code null}
	 */
	public Integer getJumlahDipakai() {
		return jumlahDipakai == null ? 0 : jumlahDipakai;
	}

	/**
	 * Mengisi jumlah pemakaian kupon sejauh ini. Tanpa penjaga konkurensi di kelas ini — dua
	 * pemakaian kupon bersamaan yang membaca-lalu-menulis nilai ini tanpa lock/increment atomik di
	 * lapisan pemanggil berisiko kehilangan salah satu kenaikan (lost update).
	 *
	 * @param jumlahDipakai jumlah pemakaian baru
	 */
	public void setJumlahDipakai(Integer jumlahDipakai) {
		this.jumlahDipakai = jumlahDipakai;
	}

	/**
	 * Mengembalikan produk kursus yang dicakup kupon ini. <b>Getter destruktif</b>
	 * ({@code check(...)}). Nilai {@code null} bermakna khusus dan disengaja: kupon berlaku untuk
	 * SEMUA produk kursus, bukan "belum diisi/tidak berlaku" — lihat javadoc kelas.
	 *
	 * @return produk kursus yang dicakup, atau {@code null} bila kupon berlaku untuk semua produk
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "produk_kursus", nullable = true)
	public ProdukKursus getProdukKursus() {
		produkKursus = check(produkKursus);
		return produkKursus;
	}

	/**
	 * Menetapkan produk kursus yang dicakup kupon ini, atau melepasnya ({@code null}) agar kupon
	 * berlaku untuk semua produk kursus.
	 *
	 * @param produkKursus produk kursus yang dicakup, atau {@code null} untuk semua produk
	 */
	public void setProdukKursus(ProdukKursus produkKursus) {
		this.produkKursus = produkKursus;
	}

	/**
	 * Mengembalikan status aktif/nonaktif kupon, menormalkan {@code null} menjadi {@code true}.
	 * Dipakai langsung oleh {@link #berlakuUntuk(Date)} sebagai syarat terakhir.
	 *
	 * @return {@code true} bila kupon aktif, tidak pernah {@code null}
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyalakan atau mematikan kupon.
	 *
	 * @param aktif {@code true} bila kupon aktif
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Menentukan apakah kupon ini sah dipakai pada waktu {@code sekarang}, memeriksa empat syarat
	 * berurutan: (1) belum melewati {@link #berlakuMulai} bila diisi, (2) belum melewati
	 * {@link #berlakuSampai} bila diisi, (3) belum mencapai {@link #batasPemakaian} bila diisi
	 * (dibandingkan terhadap {@link #getJumlahDipakai()} dengan {@code >=}, sehingga pemakaian
	 * ke-{@code batasPemakaian} yang ke-N SUDAH ditolak begitu {@code jumlahDipakai} mencapai N,
	 * bukan N+1 — batas dihitung inklusif dari sisi jumlah pemakaian yang SUDAH terjadi), dan (4)
	 * {@link #getAktif()}. Argumen {@code null} dinormalkan menjadi {@code new Date()} (waktu
	 * pemanggilan).
	 *
	 * <p><b>Peringatan — bug batas akhir off-by-one-hari:</b> syarat (2) membandingkan
	 * {@code sekarang} (stempel waktu penuh) langsung terhadap {@link #berlakuSampai} (efektif
	 * tengah malam tanggal akhir masa berlaku, karena dipetakan {@code @Temporal(DATE)}) memakai
	 * {@code sekarang.after(berlakuSampai)}. Akibatnya method ini mengembalikan {@code false} untuk
	 * SELURUH hari kalender terakhir masa berlaku kupon (kecuali persis pukul {@code 00:00:00.000}),
	 * bukan baru mengembalikan {@code false} keesokan harinya seperti yang secara wajar diharapkan
	 * dari makna "berlaku sampai [tanggal]". Peserta yang mencoba memakai kupon pada hari terakhir
	 * yang dijanjikan akan ditolak. Perbaikan yang benar perlu membandingkan {@code sekarang} yang
	 * dipangkas ke tanggal saja (tanpa komponen jam) terhadap {@link #berlakuSampai}, atau
	 * menggeser {@link #berlakuSampai} maju satu hari sebelum dibandingkan.</p>
	 *
	 * @param sekarang waktu yang diperiksa; {@code null} dinormalkan menjadi waktu saat ini
	 * @return {@code true} bila kupon sah dipakai pada waktu tersebut
	 */
	public boolean berlakuUntuk(Date sekarang) {
		if (sekarang == null) {
			sekarang = new Date();
		}
		if (berlakuMulai != null && sekarang.before(berlakuMulai)) {
			return false;
		}
		if (berlakuSampai != null && sekarang.after(berlakuSampai)) {
			return false;
		}
		if (batasPemakaian != null && getJumlahDipakai() >= batasPemakaian) {
			return false;
		}
		return getAktif();
	}

}
