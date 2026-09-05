package ais.database.model.lkp;

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
 * Master data satuan ukur (mis. "Dokumen", "Laporan", "Kegiatan", "Kali") yang dipakai untuk
 * menyatakan kuantitas pada modul LKP (Laporan Kinerja Pegawai) — dirujuk oleh {@link
 * KegiatanTugasJabatan#getSatuanKuantitas()} (satuan kuantitas default kegiatan) dan {@link
 * KegiatanTugasJabatanPunyaIndikator#getSatuan()} (satuan nilai target indikator). Entity ini murni
 * lookup/kamus (nama + keterangan), tidak membawa logika konversi antar satuan.
 *
 * @see KegiatanTugasJabatan
 * @see KegiatanTugasJabatanPunyaIndikator
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "satuan_kegiatan_tugas_jabatan")
public class SatuanKegiatanTugasJabatan extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir menyimpan/mengubah baris ini (field audit shadow,
	 * pasangan {@link #getOleh()}, diisi manual).
	 *
	 * @return id pengguna terakhir, dapat {@code null}.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id pengguna yang melakukan perubahan. Nilai {@code null} atau kosong/blank
	 * diabaikan secara diam-diam.
	 *
	 * @param olehId id pengguna; diabaikan jika {@code null} atau kosong setelah di-trim.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan nama/label pengguna yang melakukan perubahan (pasangan {@link #setOlehId(String)}).
	 * Nilai {@code null} atau kosong/blank diabaikan secara diam-diam.
	 *
	 * @param oleh nama pengguna; diabaikan jika {@code null} atau kosong setelah di-trim.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama/label pengguna yang terakhir menyimpan/mengubah baris ini.
	 *
	 * @return nama pengguna terakhir, dapat {@code null}.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: memperbarui {@link #tanggal_dirubah} melalui {@link
	 * ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} pada setiap update baris ini.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan timestamp perubahan terakhir secara eksplisit.
	 *
	 * @param tanggal_dirubah timestamp perubahan terakhir.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan timestamp perubahan terakhir baris ini, diperbarui otomatis oleh {@link
	 * #onUpdate()}.
	 *
	 * @return timestamp perubahan terakhir.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi ringkas untuk keperluan tampilan/log/debug, berupa gabungan {@code id} dan
	 * {@link #getNama() nama} satuan.
	 *
	 * @return string {@code "<id>-<nama>"}.
	 */
	public String toString() {
		return id + "-" + nama;
	}


	private String nama;
	private String keterangan;

	/** Konstruktor default (dibutuhkan Hibernate/JPA). */
	public SatuanKegiatanTugasJabatan() {
	}

	/**
	 * Mengembalikan id primary key satuan ini. Dipetakan {@code insertable = false} karena nilai
	 * dibangkitkan basis data (identity).
	 *
	 * @return id satuan, atau {@code null} untuk instance yang belum tersimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan id satuan ini.
	 *
	 * @param id id satuan.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama satuan ukur (mis. "Dokumen", "Laporan"), di-trim dari whitespace di kedua
	 * ujung. Kolom wajib diisi dengan panjang maksimum 255 karakter.
	 *
	 * @return nama satuan yang sudah di-trim, atau {@code null} bila field belum diisi.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan nama satuan ukur.
	 *
	 * @param nama nama satuan.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan/deskripsi bebas untuk satuan ini.
	 *
	 * @return keterangan satuan, dapat {@code null}.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan/deskripsi bebas untuk satuan ini.
	 *
	 * @param keterangan keterangan satuan.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}


}
