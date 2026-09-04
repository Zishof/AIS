package ais.database.model.koperasi;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * Kategori biaya sales lapangan (ERD &sect;3.8) -- CONFIGURABLE (bukan enum tertutup yang
 * butuh rilis aplikasi). Seed awal idempoten by kode di ApiEBisnis.init: BBM, TOL, PARKIR,
 * MAKAN, BONGKAR_MUAT, PENGINAPAN, SERVIS, ADMIN, LAINNYA.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "kategori_biaya_sales")
public class KategoriBiayaSales extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	private Long id;
	private String kode;
	private String nama;
	private Boolean aktif;

	/**
	 * Hook JPA {@code @PreUpdate}: mendelegasikan pencatatan {@link #tanggal_dirubah} (dan field
	 * audit sejenis) ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah}. Dipanggil
	 * otomatis oleh provider JPA setiap {@code UPDATE}, tidak untuk dipanggil manual.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Konstruktor bawaan (dipakai JPA/Hibernate dan seed idempoten {@code ApiEBisnis.init}). */
	public KategoriBiayaSales() {
	}

	/** @return id baris (identity, dibuat DB). */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id id baris; biasanya tidak diset manual, dibuat DB saat {@code save}. */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return kode kategori (unik), mis. {@code BBM}, {@code TOL}, {@code PARKIR}, {@code MAKAN},
	 *         {@code BONGKAR_MUAT}, {@code PENGINAPAN}, {@code SERVIS}, {@code ADMIN},
	 *         {@code LAINNYA} -- daftar seed awal, bukan enum tertutup (lihat catatan kelas);
	 *         pengurus dapat menambah kategori baru lewat master tanpa rilis aplikasi.
	 */
	@Column(name = "kode", length = 30, unique = true)
	public String getKode() {
		return kode;
	}

	/** @param kode kode kategori biaya (unik). */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/** @return nama tampilan kategori biaya. */
	@Column(name = "nama")
	public String getNama() {
		return nama;
	}

	/** @param nama nama tampilan kategori biaya. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return status aktif kategori. Fallback ke {@link Boolean#TRUE} bila kolom {@code null}. */
	@Column(name = "aktif")
	public Boolean getAktif() {
		return aktif == null ? Boolean.TRUE : aktif;
	}

	/** @param aktif status aktif kategori; kategori nonaktif tidak lagi dipilihkan di transaksi baru
	 *               tapi tetap tampak di riwayat biaya lama yang sudah memakainya. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/** @return waktu baris terakhir diubah; diperbarui otomatis lewat {@link #onUpdate()}. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @param tanggal_dirubah waktu perubahan terakhir (biasanya tidak diset manual). */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	private ais.database.model.akunting.Akun akun;

	/**
	 * Akun BEBAN untuk kategori biaya ini (dok 61 butir E). Diisi lewat master Kategori Biaya
	 * Sales; selama kosong, biaya berkategori ini tidak dapat dijurnal dan tetap tampil sebagai
	 * draf di dasbor Draft Jurnal — pola yang sama dengan pasangan akun jenis penghapusan aset.
	 */
	@javax.persistence.ManyToOne(fetch = javax.persistence.FetchType.LAZY)
	@javax.persistence.JoinColumn(name = "akun", nullable = true)
	public ais.database.model.akunting.Akun getAkun() {
		akun = check(akun);
		return akun;
	}

	/** @param akun akun BEBAN untuk kategori biaya ini; boleh {@code null} selama belum diisi pengurus. */
	public void setAkun(ais.database.model.akunting.Akun akun) {
		this.akun = akun;
	}

}
