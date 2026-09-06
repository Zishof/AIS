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
 * Entity referensi <b>jenis layanan akademik kepada mahasiswa</b> (tabel
 * {@code public.jenis_layanan_kepada_mahasiswa}) — master daftar jenis layanan yang tersedia untuk
 * mahasiswa (mis. bimbingan &amp; konseling, revisi nilai, layanan umum), dipakai sebagai lookup pada
 * fitur permintaan/pengajuan layanan mahasiswa. Beberapa flag ({@link #getPa()},
 * {@link #getBimbingan()}, {@link #getRevisi()}, {@link #getUmum()}) menandai kategori khusus yang
 * memengaruhi alur/penanganan permintaan.
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "jenis_layanan_kepada_mahasiswa")

public class JenisLayananKepadaMahasiswa extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * @return id akun yang membuat/mengubah baris ini.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan id pembuat/pengubah. Nilai kosong/null diabaikan (tidak menimpa nilai lama) —
	 * write-guard satu-arah.
	 *
	 * @param olehId id akun pembuat/pengubah.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama pembuat/pengubah. Nilai kosong/null diabaikan (tidak menimpa nilai lama) —
	 * write-guard satu-arah.
	 *
	 * @param oleh nama akun pembuat/pengubah.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * @return nama akun yang membuat/mengubah baris ini.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook Envers/JPA: memperbarui timestamp audit shadow {@link #tanggal_dirubah} setiap kali baris
	 * ini di-update.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * @param tanggal_dirubah waktu perubahan terakhir (audit shadow field).
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return waktu perubahan terakhir baris ini, diisi otomatis oleh {@link #onUpdate()}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * @return representasi ringkas "{id}-{nama}", dipakai untuk keperluan log/debug.
	 */
	public String toString() {
		return id + "-" + nama;
	}

	private String nama;
	private String keterangan;
	private Integer nomorUrut;

	private Boolean aktif;
	private Boolean pa;
	private Boolean bimbingan;
	private Boolean revisi;
	private Boolean umum;

	/**
	 * Konstruktor kosong (dipakai Hibernate untuk instansiasi via reflection).
	 */
	public JenisLayananKepadaMahasiswa() {
	}

	/**
	 * @return id unik baris (surrogate key, auto-increment).
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * @param id id unik baris.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return nama jenis layanan, di-trim saat dibaca.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * @param nama nama jenis layanan.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * @return keterangan/deskripsi jenis layanan ini.
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * @param keterangan keterangan/deskripsi jenis layanan ini.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return nomor urut tampil jenis layanan ini pada daftar pilihan; default {@code 1} bila belum
	 *         diisi.
	 */
	public Integer getNomorUrut() {
		return nomorUrut == null ? 1 : nomorUrut;
	}

	/**
	 * @param nomorUrut nomor urut tampil jenis layanan ini.
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * @return {@code true} bila jenis layanan ini aktif/boleh dipilih; default {@code true} bila
	 *         belum diisi — getter-mutasi (menuliskan default ke field saat pertama dipanggil).
	 */
	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}
		return aktif;
	}

	/**
	 * @param aktif status aktif/nonaktif jenis layanan ini.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * @return {@code true} bila jenis layanan ini tergolong layanan Penasihat Akademik (PA); ditebak
	 *         otomatis {@code true} bila belum diisi DAN {@link #nama} persis "Bimbingan dan
	 *         konseling" (getter-mutasi berbasis nama, bukan flag independen); default {@code false}
	 *         selain itu.
	 */
	public Boolean getPa() {
		if (pa == null && nama != null && nama.equalsIgnoreCase("Bimbingan dan konseling")) {
			pa = true;
		}
		return pa == null ? false : pa;
	}

	/**
	 * @param pa status kategori PA jenis layanan ini.
	 */
	public void setPa(Boolean pa) {
		this.pa = pa;
	}

	/**
	 * @return {@code true} bila jenis layanan ini tergolong layanan bimbingan; ditebak otomatis
	 *         {@code true} bila belum diisi DAN {@link #nama} persis "Bimbingan dan konseling"
	 *         (getter-mutasi berbasis nama, sama seperti {@link #getPa()}); default {@code false}
	 *         selain itu.
	 */
	public Boolean getBimbingan() {
		if (bimbingan == null && nama != null && nama.equalsIgnoreCase("Bimbingan dan konseling")) {
			bimbingan = true;
		}
		return bimbingan == null ? false : bimbingan;
	}

	/**
	 * @param bimbingan status kategori bimbingan jenis layanan ini.
	 */
	public void setBimbingan(Boolean bimbingan) {
		this.bimbingan = bimbingan;
	}

	/**
	 * @return {@code true} bila jenis layanan ini tergolong layanan revisi (nilai/berkas); default
	 *         {@code false} bila belum diisi.
	 */
	public Boolean getRevisi() {
		return revisi == null ? false : revisi;
	}

	/**
	 * @param revisi status kategori revisi jenis layanan ini.
	 */
	public void setRevisi(Boolean revisi) {
		this.revisi = revisi;
	}

	/**
	 * @return {@code true} bila jenis layanan ini tergolong layanan umum; default {@code true} bila
	 *         belum diisi.
	 */
	public Boolean getUmum() {
		return umum == null ? true : umum;
	}

	/**
	 * @param umum status kategori umum jenis layanan ini.
	 */
	public void setUmum(Boolean umum) {
		this.umum = umum;
	}
}
