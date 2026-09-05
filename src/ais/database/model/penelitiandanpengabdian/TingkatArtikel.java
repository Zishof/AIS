package ais.database.model.penelitiandanpengabdian;

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



import ais.database.model.GeneralValueObject;

/**
 * Entitas referensi/lookup tingkat publikasi (tabel
 * {@code penelitiandanpengabdian.tingkat_publikasi}) — daftar tingkat cakupan publikasi ilmiah
 * (mis. lokal, nasional, internasional, atau kategori lain sesuai kebijakan institusi) yang dapat
 * ditautkan ke sebuah {@link Artikel}. Ditautkan dari {@link Artikel} lewat relasi
 * banyak-ke-banyak {@link Artikel#getTingkatArtikeles()} (tabel penghubung
 * {@code artikel_has_tingkat}) — satu artikel dapat memiliki berapa pun tingkat publikasi
 * sekaligus, dan satu tingkat publikasi dapat menaungi berapa pun artikel.
 *
 * <p>Nama tampil (label) yang sebenarnya berasal dari data yang diinput pengelola lewat
 * {@link #getNama()} — nama kelas Java hanya menyatakan konsep umum "tingkat publikasi", bukan
 * daftar nama tingkat yang tetap/hardcoded.</p>
 *
 * <h2>Bidang audit bayangan</h2>
 * <p>{@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} beserta {@link #onUpdate()} adalah
 * keharusan teknis agar {@code AuditTimestampInterceptor} dapat bekerja, bukan duplikasi yang bisa
 * dihapus. Setternya sengaja mengabaikan masukan kosong agar jejak audit yang sudah ada tidak
 * tertimpa string kosong dari jalur salin/klon objek.</p>
 *
 * @see Artikel
 * @see ArtikelTerindeks
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "penelitiandanpengabdian", name = "tingkat_publikasi")



public class TingkatArtikel extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java. Nilai warisan cetakan hbm2java; jangan diubah tanpa alasan.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama basis data, dibangkitkan {@code IDENTITY}; {@code null} selama baris belum tersimpan. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini; diisi {@code AuditTimestampInterceptor}, bukan oleh form. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini; pasangan teknis dari {@link #oleh}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris ini.
	 *
	 * @return id pengguna terakhir, atau {@code null} bila belum pernah diubah lewat jalur yang
	 *         memasang interceptor audit
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna yang terakhir mengubah baris ini.
	 *
	 * <p><b>Setter defensif:</b> masukan {@code null} atau yang hanya berisi spasi diabaikan
	 * diam-diam sehingga nilai lama dipertahankan.</p>
	 *
	 * @param olehId id pengguna; {@code null}/kosong diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna yang terakhir mengubah baris ini.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, masukan {@code null}/kosong diabaikan.</p>
	 *
	 * @param oleh nama pengguna; {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate} yang mendelegasikan pencatatan audit ke
	 * {@code AuditTimestampInterceptor.ubah(this)} tepat sebelum baris diperbarui. Interceptor-lah
	 * yang mengisi {@link #oleh}, {@link #olehId}, dan {@link #getTanggal_dirubah()} dari konteks
	 * pengguna aktif. Method sengaja {@code protected} dan tidak boleh dipanggil manual.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Umumnya dipanggil {@code AuditTimestampInterceptor},
	 * bukan oleh form.
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * @return stempel waktu perubahan terakhir, dapat {@code null} bila belum pernah diubah lewat
	 *         jalur yang memasang interceptor audit dan field belum diinisialisasi manual
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat tingkat publikasi ini, dipakai label bawaan komponen ZK dan
	 * penelusuran log.
	 *
	 * @return nama tingkat publikasi
	 */
	public String toString() {
		return nama;
	}

	/** Nama tingkat publikasi (mis. "Nasional", "Internasional"); nilai tampil sebenarnya, ditentukan data, bukan oleh nama kelas Java. */
	private String nama;
	/** Keterangan tambahan mengenai tingkat publikasi ini. */
	private String keterangan;
	/** Flag aktif/nonaktif tingkat publikasi ini; {@code null} diperlakukan sebagai aktif oleh {@link #getAktif()}. */
	private Boolean aktif;

	/** Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate. */
	public TingkatArtikel() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * @return id baris, atau {@code null} bila baris belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama baris ini. Hanya untuk kebutuhan Hibernate dan penyalinan objek.
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama tingkat publikasi ini (mis. "Nasional", "Internasional").
	 *
	 * @return nama tingkat publikasi
	 */
	@Column(name = "nama", nullable = false, columnDefinition = "text")
	public String getNama() {
		return this.nama;
	}

	/**
	 * Menyetel nama tingkat publikasi ini.
	 *
	 * @param nama nama baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan tambahan mengenai tingkat publikasi ini.
	 *
	 * @return keterangan, dapat {@code null} bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan tambahan mengenai tingkat publikasi ini.
	 *
	 * @param keterangan keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan status aktif/nonaktif tingkat publikasi ini.
	 *
	 * <p><b>Lazy-default:</b> bila field internal masih {@code null}, method ini menetapkan
	 * {@code true} ke field tersebut sebelum mengembalikannya, sehingga panggilan berikutnya
	 * konsisten mengembalikan {@code true} tanpa perlu query ulang.</p>
	 *
	 * @return {@code true} bila aktif; default {@code true} bila belum pernah disetel
	 */
	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}
		return aktif;
	}

	/**
	 * Menyetel status aktif/nonaktif tingkat publikasi ini.
	 *
	 * @param aktif status aktif baru; {@code null} akan kembali diperlakukan sebagai aktif oleh
	 *              {@link #getAktif()}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

}
