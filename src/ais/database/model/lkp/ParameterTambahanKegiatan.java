package ais.database.model.lkp;

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




import org.hibernate.envers.Audited;



import ais.database.model.GeneralValueObject;
import ais.database.model.ParameterTambahan;

/**
 * Baris keanggotaan yang menyatakan satu {@link ParameterTambahan} (definisi field form tambahan
 * generik milik {@code ais.database.model}, dipakai lintas modul) menjadi anggota satu {@link
 * KelompokParameterTambahanKegiatan} (kelompok form tambahan modul LKP). Entity ini sendiri tidak
 * menyimpan nilai isian pengguna — nilai aktual per realisasi disimpan terserialisasi di {@link
 * ais.database.model.lkp.RealisasiKerjaPegawai#getParameterTambahan()}; baris ini hanya
 * mendefinisikan parameter mana saja yang tersedia untuk diisi dalam satu kelompok, beserta
 * urutan tampilnya.
 *
 * @see ParameterTambahan
 * @see KelompokParameterTambahanKegiatan
 * @see ais.database.model.lkp.RealisasiKerjaPegawai
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "parameter_tambahan_kegiatan")



public class ParameterTambahanKegiatan extends GeneralValueObject {

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

	private String nama;

	/**
	 * Mengembalikan nama tampilan parameter ini. <b>Efek samping saat baca:</b> bila {@link
	 * #parameterTambahan} sudah termuat, field {@code nama} lokal ditimpa dengan {@link
	 * ParameterTambahan#getNama()} milik parameter tersebut sebelum dikembalikan — sehingga nama
	 * yang pernah di-{@code set} manual pada objek ini akan selalu kalah oleh nama definisi
	 * {@link ParameterTambahan} begitu relasinya termuat.
	 *
	 * @return nama parameter (mengikuti {@link ParameterTambahan#getNama()} bila relasi termuat),
	 *         atau nilai yang di-set manual bila relasi belum/tidak termuat.
	 */
	public String getNama() {
		if (parameterTambahan != null) {
			nama = parameterTambahan.getNama();
		}
		return nama;
	}

	/**
	 * Menetapkan nama tampilan parameter secara manual. Perhatikan bahwa nilai ini dapat ditimpa
	 * saat berikutnya {@link #getNama()} dipanggil, jika {@link #parameterTambahan} sudah termuat
	 * (lihat catatan pada {@link #getNama()}).
	 *
	 * @param nama nama parameter.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	private ParameterTambahan parameterTambahan;
	private KelompokParameterTambahanKegiatan kelompokParameterTambahanKegiatan;

	private Integer nomorUrut;

	/**
	 * Mengembalikan nomor urut tampil parameter ini di dalam kelompoknya. <b>Efek samping saat
	 * baca:</b> method ini secara aktif memanggil {@link #getParameterTambahan()} (memuat/refresh
	 * relasi lazy) dan, bila hasilnya tidak {@code null}, menimpa field {@code nomorUrut} lokal
	 * dengan {@link ParameterTambahan#getNomorUrut()} milik definisi parameter tersebut — pola yang
	 * sama seperti {@link #getNama()}: nomor urut definisi {@link ParameterTambahan} selalu
	 * mengalahkan nilai yang pernah di-{@code set} manual pada baris keanggotaan ini, selama
	 * relasinya berhasil dimuat.
	 *
	 * @return nomor urut tampil; {@code 1} bila baik nomor urut lokal maupun definisi parameter
	 *         belum diisi.
	 */
	public Integer getNomorUrut() {
		parameterTambahan = getParameterTambahan();
		if (parameterTambahan != null) {
			nomorUrut = parameterTambahan.getNomorUrut();
		}
		return nomorUrut == null ? 1 : nomorUrut;
	}

	/**
	 * Menetapkan nomor urut tampil parameter secara manual. Perhatikan bahwa nilai ini dapat
	 * ditimpa saat berikutnya {@link #getNomorUrut()} dipanggil (lihat catatan pada method
	 * tersebut).
	 *
	 * @param nomorUrut nomor urut baru.
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/** Konstruktor default (dibutuhkan Hibernate/JPA). */
	public ParameterTambahanKegiatan() {
	}

	/**
	 * Mengembalikan id primary key baris keanggotaan ini. Dipetakan {@code insertable = false}
	 * karena nilai dibangkitkan basis data (identity).
	 *
	 * @return id baris, atau {@code null} untuk instance yang belum tersimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan id baris keanggotaan ini.
	 *
	 * @param id id baris.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan {@link ParameterTambahan} (definisi field form tambahan generik) yang menjadi
	 * anggota kelompok ini. Kolom wajib diisi; relasi lazy, di-refresh lewat {@link
	 * #check(Object)}.
	 *
	 * @return definisi parameter tambahan terkait.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "parameter_tambahan", nullable = false)
	public ParameterTambahan getParameterTambahan() {
		parameterTambahan = check(parameterTambahan);
		return parameterTambahan;
	}

	/**
	 * Menetapkan definisi parameter tambahan yang menjadi anggota kelompok ini.
	 *
	 * @param parameterTambahan definisi parameter tambahan baru.
	 */
	public void setParameterTambahan(ParameterTambahan parameterTambahan) {
		this.parameterTambahan = parameterTambahan;
	}

	/**
	 * Mengembalikan {@link KelompokParameterTambahanKegiatan} (kelompok) pemilik keanggotaan
	 * parameter ini. Kolom wajib diisi; relasi lazy, di-refresh lewat {@link #check(Object)}.
	 *
	 * @return kelompok pemilik.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelompok_parameter_tambahan_kegiatan", nullable = false)
	public KelompokParameterTambahanKegiatan getKelompokParameterTambahanKegiatan() {
		kelompokParameterTambahanKegiatan = check(kelompokParameterTambahanKegiatan);
		return kelompokParameterTambahanKegiatan;
	}

	/**
	 * Menetapkan kelompok pemilik keanggotaan parameter ini.
	 *
	 * @param kelompokParameterTambahanKegiatan kelompok pemilik baru.
	 */
	public void setKelompokParameterTambahanKegiatan(
			KelompokParameterTambahanKegiatan kelompokParameterTambahanKegiatan) {
		this.kelompokParameterTambahanKegiatan = kelompokParameterTambahanKegiatan;
	}

}
