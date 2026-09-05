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
 * Seksi/bab kurikulum sebuah {@link ais.database.model.kursus.ProdukKursus}. Menampung urutan
 * {@link MateriKursus} di dalamnya lewat {@link MateriKursus#getSeksiKursus()} (relasi
 * one-to-many yang diakses dari sisi banyak, tidak ada koleksi eksplisit di kelas ini).
 * Urutan tampil seksi di dalam kursus diatur lewat {@link #getUrutan()}, sama seperti pola
 * {@link MateriKursus#getUrutan()} di dalam satu seksi.
 * <p>
 * Otorisasi "hanya instruktur pemilik {@link ais.database.model.kursus.ProdukKursus} boleh
 * menambah/mengubah/menghapus seksi" ditegakkan di pemanggil (aksi simpan_seksi/hapus_seksi
 * pada webapp/WEB-INF/baru/modul/kursus/_kursus_service.jsp, membandingkan
 * {@link #getProdukKursus()}.getInstruktur() dengan peserta yang login), BUKAN di entity ini.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "seksi_kursus")
public class SeksiKursus extends GeneralValueObject {

	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * Mengambil id pengguna pembuat/pengubah terakhir baris ini. Field audit "shadow" -- diisi
	 * lewat {@link ais.database.hibernate.AuditTimestampInterceptor}, bukan bagian dari data
	 * bisnis seksi kursus.
	 *
	 * @return id pengguna pembuat/pengubah terakhir, atau {@code null} jika belum pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna pembuat/pengubah terakhir. Nilai kosong/blank diabaikan (fail-safe).
	 *
	 * @param olehId id pengguna; diabaikan jika {@code null} atau hanya berisi spasi.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna pembuat/pengubah terakhir. Nilai kosong/blank diabaikan (fail-safe).
	 *
	 * @param oleh nama pengguna; diabaikan jika {@code null} atau hanya berisi spasi.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengambil nama pengguna yang terakhir membuat/mengubah baris ini.
	 *
	 * @return nama pengguna pembuat/pengubah terakhir, atau {@code null} jika belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: memperbarui {@link #tanggal_dirubah} otomatis sesaat
	 * sebelum UPDATE dieksekusi. Method audit "shadow", keharusan teknis infrastruktur.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi tanggal terakhir baris ini diubah. Biasanya diisi otomatis lewat {@link #onUpdate()}.
	 *
	 * @param tanggal_dirubah tanggal/waktu perubahan terakhir.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengambil tanggal terakhir baris ini diubah.
	 *
	 * @return tanggal/waktu perubahan terakhir.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi ringkas untuk log/debug/dropdown: {@code id + "-" + nama}.
	 *
	 * @return string gabungan id dan nama seksi.
	 */
	public String toString() {
		return id + "-" + nama;
	}

	private String kode;
	private String nama;
	private String keterangan;
	private ProdukKursus produkKursus;
	private Integer urutan;
	private Boolean aktif;

	/** Konstruktor default -- dibutuhkan Hibernate untuk instansiasi lewat reflection. */
	public SeksiKursus() {
	}

	/**
	 * Mengambil id unik (primary key) baris seksi ini, di-generate DB lewat
	 * strategi {@code IDENTITY}.
	 *
	 * @return id seksi, atau {@code null} untuk entity yang belum pernah disimpan.
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
	 * Mengambil kode seksi (belum dipetakan lewat {@code @Column} eksplisit). Nilai selalu
	 * di-trim, dan {@code null} dikembalikan sebagai string kosong.
	 *
	 * @return kode seksi ter-trim, atau string kosong jika belum diisi.
	 */
	public String getKode() {
		return kode == null ? "" : kode.trim();
	}

	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengambil nama/judul seksi, selalu di-trim. Kolom {@code NOT NULL} di DB.
	 *
	 * @return nama seksi ter-trim, atau {@code null} jika belum diisi.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi nama/judul seksi. Tidak ada validasi kosong/blank di layer entity ini -- validasi
	 * "judul harus diisi" ditegakkan di pemanggil (aksi simpan_seksi pada
	 * {@code _kursus_service.jsp}).
	 *
	 * @param nama nama seksi.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengambil keterangan/deskripsi bebas untuk seksi ini (kolom {@code text}).
	 *
	 * @return keterangan seksi, bisa {@code null}.
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengambil {@link ais.database.model.kursus.ProdukKursus} (kursus induk) tempat seksi ini
	 * berada.
	 *
	 * @return kursus induk seksi ini; kolom {@code NOT NULL} di DB.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "produk_kursus", nullable = false)
	public ProdukKursus getProdukKursus() {
		produkKursus = check(produkKursus);
		return produkKursus;
	}

	/**
	 * Mengisi kursus induk seksi ini. Tidak ada verifikasi kepemilikan di setter ini --
	 * pengecekan "hanya instruktur pemilik kursus yang boleh menambah seksi" ditegakkan oleh
	 * pemanggil (lihat catatan otorisasi di Javadoc kelas).
	 *
	 * @param produkKursus kursus induk baru.
	 */
	public void setProdukKursus(ProdukKursus produkKursus) {
		this.produkKursus = produkKursus;
	}

	/**
	 * Mengambil urutan tampil seksi ini di dalam kurikulum kursusnya (ascending, dipakai saat
	 * query kurikulum -- lihat pemanggil di {@code _kursus_service.jsp} yang selalu
	 * {@code addOrder(Order.asc("urutan"))}).
	 *
	 * @return urutan tampil, default 0 bila belum diisi.
	 */
	public Integer getUrutan() {
		return urutan == null ? 0 : urutan;
	}

	public void setUrutan(Integer urutan) {
		this.urutan = urutan;
	}

	/**
	 * Mengambil penanda aktif/nonaktif seksi ini (soft-disable, seksi tidak dihapus dari DB
	 * tapi disembunyikan dari kurikulum yang ditampilkan ke peserta). Default {@code true} bila
	 * kolom belum diisi -- pola flag aktif "fail-open by default" yang sama dipakai
	 * {@link MateriKursus#getAktif()}.
	 *
	 * @return {@code true} jika aktif; default {@code true} bila belum diisi.
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

}
