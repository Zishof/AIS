package ais.database.model.library;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

import ais.common.Common;
import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;
import ais.ui.util.WaktuUtil;

/**
 * Entity <b>header</b> dokumen <b>koreksi stok item perpustakaan</b> (tabel
 * {@code library.koreksi_item}). Satu baris merepresentasikan satu nota penyesuaian
 * (<i>stock adjustment</i>) yang menyamakan catatan sistem dengan hasil stok opname: menambah
 * eksemplar yang ternyata ada tapi tidak tercatat, atau mengurangi eksemplar yang tercatat tapi
 * hilang/rusak. Rincian per judul berada pada {@link KoreksiItemDetail}.
 *
 * <p>Tipe ini membawa state yang dipertukarkan oleh lapisan persistence, service, dan UI; makna
 * bisnis utamanya ditentukan oleh field serta relasi yang dideklarasikan.</p>
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap
 * dimiliki {@link GeneralValueObject}. Kelas ini hanya memuat perbedaan yang benar-benar
 * spesifik untuk variasi ini; perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di
 * kelas induk agar fungsi tidak bercabang atau tumpang tindih.</p>
 *
 * <p><b>Header paling ramping di klaster pengadaan.</b> Berbeda dari
 * {@link ReturPengadaanItem} (punya {@code penyedia} + {@code penerimaanPengadaanItem}) dan
 * {@link TransferPengadaanItem} (punya {@code perpustakaanTujuan} + {@code terimaPengadaanItem}),
 * header koreksi <b>tidak memiliki dokumen sumber sama sekali</b>. Ini konsisten dengan sifatnya:
 * koreksi adalah pernyataan sepihak perpustakaan tentang stok yang sebenarnya, bukan turunan
 * transaksi dengan pihak lain. Konsekuensinya, tidak ada satu pun referensi yang bisa dipakai
 * untuk memverifikasi kewajaran angka koreksi &mdash; satu-satunya pengendalian yang tersisa
 * adalah persetujuan manusia lewat {@link #getDisetujuiOleh() disetujuiOleh} dan jejak
 * {@link Audited Envers}. Karena itu keterangan yang bermakna pada
 * {@link #getKeterangan() keterangan} bukan formalitas, melainkan satu-satunya penjelasan
 * mengapa stok berubah.</p>
 *
 * <p><b>Pola dokumen dua tahap.</b> Entity memakai pasangan {@link #getDibuatOleh() dibuatOleh}/
 * {@link #getTanggalPembuatan() tanggalPembuatan} dan {@link #getDisetujuiOleh() disetujuiOleh}/
 * {@link #getTanggalPersetujuan() tanggalPersetujuan}. Kolom persetujuan {@code nullable}:
 * {@code null} berarti dokumen masih draf. Model <b>tidak</b> menegakkan pemisahan tugas antara
 * pembuat dan penyetuju, tidak memeriksa hak akses, dan tidak mengunci baris rincian setelah
 * dokumen diposting. Mengingat dokumen ini dapat mengubah stok tanpa dokumen pembanding apa pun,
 * gerbang persetujuan di lapisan action adalah pengendalian yang paling penting untuk modul
 * ini.</p>
 *
 * <p><b>Multi-tenant.</b> Ruang lingkup dibatasi oleh {@link #getPerpustakaan() perpustakaan},
 * yang getter-nya mengisi diri sendiri dari sesi bila masih {@code null}. Pembatasan tenant yang
 * sesungguhnya tetap harus berupa kriteria query di DAO/action.</p>
 *
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Long id}, {@code Long
 * index}, {@code String oleh}, {@code String olehId}, {@code Date tanggal_dirubah}, {@code String
 * kode}, {@code String keterangan}, {@code Perpustakaan perpustakaan}; pemetaan persistence:
 * tabel {@code library.koreksi_item}; inisialisasi/lifecycle ({@code setDibuatOleh()}, {@code
 * getDibuatOleh()}, {@code setTanggalPembuatan()}, {@code getTanggalPembuatan()});
 * pembacaan/pencarian ({@code getOlehId()}, {@code getOleh()}, {@code getTanggal_dirubah()},
 * {@code getId()}, {@code getKode()}, {@code getKeterangan()}); mutasi data ({@code setOlehId()},
 * {@code setOleh()}, {@code onUpdate()}, {@code setTanggal_dirubah()}, {@code setId()}, {@code
 * setKode()}); operasi domain lain ({@code toString()}). Bagian lain dari kontrak tetap mengikuti
 * kelas induk atau interface yang disebut di atas.</p>
 *
 * <p><b>Efek samping:</b> accessor dan mutator hanya membaca atau mengubah state entity/value
 * object di memori. Persistence, transaksi, otorisasi, dan pemuatan relasi lazy tetap menjadi
 * tanggung jawab DAO/service dengan session aktif; jangan menaruh query duplikat pada model.</p>
 *
 * @see KoreksiItemDetail
 * @see KodeTransaksi
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "library", name = "koreksi_item")

public class KoreksiItem extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya sengaja disamakan di seluruh entity modul
	 * {@code library} karena kelas-kelas ini dibangkitkan dari template yang sama; jangan
	 * diubah agar sesi ZK/HTTP yang sudah terserialisasi tetap dapat dibaca.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama (surrogate key) baris ini, dibangkitkan oleh database. */
	private Long id;
	/** Nomor urut tampilan pada grid ZK; bukan kolom bisnis. */
	private Long index;
	/** Nama pengguna aplikasi yang terakhir mengubah baris ini (jejak audit ringan). */
	private String oleh;
	/** ID pengguna aplikasi yang terakhir mengubah baris ini (jejak audit ringan). */
	private String olehId;

	/**
	 * Mengembalikan ID pengguna aplikasi yang terakhir mengubah baris ini.
	 *
	 * @return ID pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel ID pengguna pengubah terakhir. Bersifat <b>no-op bila nilai baru kosong atau
	 * hanya berisi spasi</b> agar jejak audit lama tidak tertimpa oleh pemanggil tanpa konteks
	 * pengguna.
	 *
	 * @param olehId ID pengguna baru; diabaikan bila {@code null}/blank.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Representasi teks dokumen untuk combobox, listbox, dan log.
	 *
	 * <p>Membaca field {@link #kode} secara langsung sehingga tidak memicu inisialisasi proxy
	 * apa pun.</p>
	 *
	 * @return kode dokumen koreksi; dapat {@code null} untuk objek yang belum diberi kode.
	 */
	public String toString() {
		return kode;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir; no-op bila nilai baru kosong/blank.
	 *
	 * @param oleh nama pengguna baru; diabaikan bila {@code null}/blank.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna aplikasi yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}. Dipanggil Hibernate tepat sebelum {@code UPDATE},
	 * lalu mendelegasikan pengisian trio field audit kepada
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}
	/**
	 * Cap waktu perubahan terakhir. Diinisialisasi ke waktu server saat objek dibuat dan
	 * diperbarui oleh {@link #onUpdate()} pada setiap {@code UPDATE}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel cap waktu perubahan terakhir secara manual.
	 *
	 * @param tanggal_dirubah cap waktu baru.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan cap waktu perubahan terakhir baris ini.
	 *
	 * @return cap waktu perubahan terakhir.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Kode/nomor dokumen koreksi; unik pada tabel dan dipakai sebagai identitas manusiawi. */
	private String kode;
	/**
	 * Catatan bebas. Karena dokumen koreksi tidak punya dokumen sumber, teks inilah
	 * satu-satunya penjelasan mengapa stok berubah &mdash; perlakukan sebagai wajib secara
	 * prosedural meski kolomnya {@code nullable}.
	 */
	private String keterangan;
	/** Perpustakaan (tenant) pemilik dokumen koreksi ini. */
	private Perpustakaan perpustakaan;
	/** Tanggal dokumen dibuat (tahap draf). */
	private Date tanggalPembuatan;
	/** Tanggal dokumen disetujui; {@code null} selama dokumen masih draf. */
	private Date tanggalPersetujuan;
	/** Pengguna pembuat dokumen. */
	private Tbmuser dibuatOleh;
	/** Pengguna penyetuju dokumen; {@code null} selama dokumen masih draf. */
	private Tbmuser disetujuiOleh;

	/**
	 * Konstruktor kosong yang dibutuhkan Hibernate/ZK untuk instansiasi via refleksi.
	 */
	public KoreksiItem() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * @return ID baris, atau {@code null} bila objek belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama baris ini. Umumnya hanya dipanggil Hibernate setelah {@code INSERT}.
	 *
	 * @param id ID baris baru.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kode/nomor dokumen koreksi.
	 *
	 * @return kode dokumen; wajib terisi dan unik pada tabel.
	 */
	@Column(name = "kode", nullable = false, unique = true)
	public String getKode() {
		return this.kode;
	}

	/**
	 * Menyetel kode/nomor dokumen koreksi. Keunikan hanya dijaga oleh constraint database;
	 * pembangkitan nomor otomatis dilakukan oleh lapisan action.
	 *
	 * @param kode kode dokumen baru.
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan keterangan/alasan koreksi stok.
	 *
	 * <p>Berbeda dengan {@link SaldoAwal#getKeterangan()} yang menormalkan {@code null} menjadi
	 * string kosong, getter ini mengembalikan {@code null} apa adanya; pemanggil di layar dan
	 * laporan harus menanganinya sendiri.</p>
	 *
	 * @return keterangan bebas, atau {@code null} bila tidak diisi.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan/alasan koreksi stok.
	 *
	 * @param keterangan teks keterangan baru.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menyetel pengguna pembuat dokumen.
	 *
	 * @param dibuatOleh pengguna pembuat.
	 */
	public void setDibuatOleh(Tbmuser dibuatOleh) {
		this.dibuatOleh = dibuatOleh;
	}

	/**
	 * Mengembalikan pengguna pembuat dokumen. Kolom bersifat {@code NOT NULL} sehingga dokumen
	 * yang tersimpan selalu punya pembuat.
	 *
	 * @return pengguna pembuat dokumen.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "dibuat_oleh", nullable = false)
	public Tbmuser getDibuatOleh() {
		return dibuatOleh;
	}

	/**
	 * Menyetel pengguna penyetuju dokumen.
	 *
	 * <p><b>Catatan integritas:</b> setter tidak memeriksa bahwa penyetuju berbeda dari
	 * pembuat, tidak memeriksa hak akses, dan tidak menolak perubahan pada dokumen yang sudah
	 * disetujui. Pada dokumen koreksi hal ini paling terasa karena tidak ada dokumen sumber
	 * yang bisa dipakai membandingkan angka; pemisahan tugas harus ditegakkan lapisan
	 * action.</p>
	 *
	 * @param disetujuiOleh pengguna penyetuju; {@code null} mengembalikan dokumen ke status draf.
	 */
	public void setDisetujuiOleh(Tbmuser disetujuiOleh) {
		this.disetujuiOleh = disetujuiOleh;
	}

	/**
	 * Mengembalikan pengguna penyetuju dokumen.
	 *
	 * @return pengguna penyetuju, atau {@code null} bila dokumen masih berstatus draf.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "disetujui_oleh", nullable = true)
	public Tbmuser getDisetujuiOleh() {
		return disetujuiOleh;
	}

	/**
	 * Menyetel tanggal pembuatan dokumen.
	 *
	 * @param tanggalPembuatan tanggal pembuatan baru.
	 */
	public void setTanggalPembuatan(Date tanggalPembuatan) {
		this.tanggalPembuatan = tanggalPembuatan;
	}

	/**
	 * Mengembalikan tanggal pembuatan dokumen, dengan <b>fallback ke waktu server</b> bila
	 * belum diisi.
	 *
	 * <p><b>Peringatan:</b> fallback hanya dikembalikan, <em>tidak</em> ditulis balik ke field.
	 * Karena Hibernate membaca nilai lewat getter (property access), baris tanpa
	 * {@code tanggalPembuatan} tersimpan dengan waktu saat <i>flush</i> terjadi. Untuk dokumen
	 * koreksi hal ini berarti tanggal efektif penyesuaian stok bisa bergeser dari tanggal stok
	 * opname yang sesungguhnya; isilah tanggal secara eksplisit di lapisan action.</p>
	 *
	 * @return tanggal pembuatan tersimpan, atau waktu server saat ini bila belum diisi.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_pembuatan")
	public Date getTanggalPembuatan() {
		return tanggalPembuatan == null ? WaktuUtil.getDate() : tanggalPembuatan;
	}

	/**
	 * Menyetel tanggal persetujuan dokumen.
	 *
	 * @param tanggalPersetujuan tanggal persetujuan; {@code null} berarti belum disetujui.
	 */
	public void setTanggalPersetujuan(Date tanggalPersetujuan) {
		this.tanggalPersetujuan = tanggalPersetujuan;
	}

	/**
	 * Mengembalikan tanggal persetujuan dokumen. Tidak memakai fallback: nilai {@code null}
	 * adalah penanda sah bahwa dokumen belum disetujui dan karenanya belum boleh memengaruhi
	 * stok.
	 *
	 * @return tanggal persetujuan, atau {@code null} bila dokumen masih draf.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_persetujuan")
	public Date getTanggalPersetujuan() {
		return tanggalPersetujuan;
	}

	/**
	 * Menyetel perpustakaan (tenant) pemilik dokumen.
	 *
	 * @param perpustakaan perpustakaan pemilik.
	 */
	public void setPerpustakaan(Perpustakaan perpustakaan) {
		this.perpustakaan = perpustakaan;
	}

	/**
	 * Mengembalikan perpustakaan (tenant) pemilik dokumen, dengan <b>pengisian otomatis</b>
	 * dari sesi bila belum diisi.
	 *
	 * <p>Alur getter ini: (1) bila field masih {@code null}, ambil perpustakaan aktif melalui
	 * {@link Common#getCurrentPerpustakaan()}; (2) jalankan {@code check(...)} milik
	 * {@link GeneralValueObject} untuk menukar proxy Hibernate yang sudah terlepas session
	 * dengan instance yang aman dibaca. Hasilnya ditulis balik ke field, sehingga getter ini
	 * <b>mengubah state objek</b> (getter destruktif ringan).</p>
	 *
	 * <p><b>Konsekuensi keamanan:</b> dokumen yang dimuat pengguna tenant lain dan kemudian
	 * di-<i>flush</i> dapat berpindah tenant bila field-nya kebetulan {@code null}. Pembatasan
	 * tenant yang sesungguhnya harus berupa kriteria {@code Restrictions.eq("perpustakaan",
	 * ...)} pada query DAO/action.</p>
	 *
	 * @return perpustakaan pemilik dokumen; dapat {@code null} bila sesi juga tidak memilikinya.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "perpustakaan", nullable = true)
	public Perpustakaan getPerpustakaan() {
		if (perpustakaan == null) {
			perpustakaan = Common.getCurrentPerpustakaan();
		}
		perpustakaan = check(perpustakaan);
		return perpustakaan;
	}

	/**
	 * Menyetel nomor urut tampilan pada grid ZK.
	 *
	 * @param index nomor urut tampilan.
	 */
	public void setIndex(Long index) {
		this.index = index;
	}

	/**
	 * Mengembalikan nomor urut tampilan pada grid ZK. Nilai murni kosmetik dan tidak boleh
	 * dipakai sebagai identitas.
	 *
	 * @return nomor urut tampilan, atau {@code null} bila belum diisi renderer.
	 */
	public Long getIndex() {
		return index;
	}

}
