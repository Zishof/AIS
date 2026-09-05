package ais.database.model.recruitment;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;




import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;



import ais.database.model.GeneralValueObject;

/**
 * Entity JPA/Hibernate untuk tabel {@code public.gelombang_punya_parameter_verifikasi_calon_pegawai}:
 * pengelompokan parameter verifikasi calon pegawai per {@link GelombangPendaftaranPegawai} pada
 * modul rekrutmen. Satu baris mewakili satu "grup" parameter verifikasi (dengan nama & judul
 * tampilan sendiri, mis. untuk dikelompokkan menjadi beberapa tab/bagian formulir verifikasi) yang
 * terikat ke satu gelombang tertentu, dan menaungi himpunan {@link ParameterVerifikasiCalonPegawai}
 * lewat relasi many-to-many.
 *
 * <p>Perlu dibedakan dari {@link GelombangPendaftaranPegawai#getVerifikasiKelengkapanCalonPegawais()}:
 * relasi tersebut menaungi item verifikasi <b>kelengkapan berkas</b> ({@link
 * VerifikasiKelengkapanCalonPegawai}) langsung dari gelombang, sedangkan kelas ini adalah entity
 * perantara terpisah yang menaungi <b>parameter verifikasi</b> ({@link
 * ParameterVerifikasiCalonPegawai}) dan dikelompokkan lebih lanjut dengan nama/judul sendiri —
 * keduanya adalah dua mekanisme verifikasi berbeda yang hidup berdampingan di modul ini, jangan
 * disamakan meski nama kelasnya mirip.</p>
 *
 * <p><b>Relasi:</b></p>
 * <ul>
 * <li>{@link #getGelombangPendaftaranPegawai()} — {@code @ManyToOne} opsional ({@code nullable}
 * tidak diset eksplisit, default JPA {@code true}) ke {@link GelombangPendaftaranPegawai} dengan
 * {@code FetchMode.SELECT}.</li>
 * <li>{@link #getParameterVerifikasiCalonPegawais()} — {@code @ManyToMany} dengan {@code cascade =
 * {MERGE, PERSIST}} lewat tabel pivot {@code gelombang_verifikasi_calon_pegawai_punya_parameter}
 * ke {@link ParameterVerifikasiCalonPegawai}; berbeda dari relasi many-to-many pada {@link
 * GelombangPendaftaranPegawai}, di sini {@code CascadeType.PERSIST} disertakan, sehingga entity
 * {@link ParameterVerifikasiCalonPegawai} baru yang belum pernah dipersist bisa langsung ikut
 * dipersist saat baris grup ini disimpan (tidak harus dipersist terpisah lebih dulu).</li>
 * </ul>
 *
 * <p>Diaudit oleh Hibernate Envers ({@code @Audited}); setiap INSERT/UPDATE/DELETE tercatat ke
 * tabel revisi historis terpisah.</p>
 *
 * @see GelombangPendaftaranPegawai
 * @see ParameterVerifikasiCalonPegawai
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "gelombang_punya_parameter_verifikasi_calon_pegawai")



public class GelombangPendaftaranPegawaiPunyaParameterVerifikasiCalonPegawai extends GeneralValueObject {

	/**
	 * Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable} lintas deployment.
	 */
	private static final long serialVersionUID = 1463822577548439808L;
	/**
	 * Primary key baris ini pada tabel {@code gelombang_punya_parameter_verifikasi_calon_pegawai},
	 * dihasilkan otomatis oleh database ({@code IDENTITY}). Lihat {@link #getId()}.
	 */
	private Long id;
	/**
	 * Nama tampilan pengguna yang terakhir membuat/mengubah baris ini. Lihat {@link #getOleh()}/
	 * {@link #setOleh(String)}.
	 */
	private String oleh;
	/**
	 * ID pengguna yang terakhir membuat/mengubah baris ini, pasangan dari {@link #oleh}. Lihat
	 * {@link #getOlehId()}/{@link #setOlehId(String)}.
	 */
	private String olehId;

	/**
	 * Mengambil ID pengguna audit terakhir.
	 *
	 * @return ID pengguna, atau {@code null} bila belum pernah diset.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengeset ID pengguna audit. Menolak (no-op) nilai {@code null}/kosong-whitespace — pola
	 * audit-shadow-field yang berulang di seluruh entity AIS agar jejak "olehId" tidak pernah
	 * tertimpa kosong.
	 *
	 * @param olehId ID pengguna; diabaikan bila {@code null}/kosong.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengeset nama pengguna audit. Guard yang sama seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null}/kosong.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengambil nama pengguna audit terakhir.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diset.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: mendelegasikan pembaruan timestamp audit ke {@link
	 * ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} tepat sebelum UPDATE
	 * dijalankan. Tidak dipanggil manual dari kode aplikasi.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengeset timestamp perubahan terakhir secara manual.
	 *
	 * @param tanggal_dirubah timestamp baru.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengambil timestamp terakhir baris ini diubah.
	 *
	 * @return timestamp perubahan terakhir; tidak pernah {@code null}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi string entity ini untuk keperluan tampilan/log, berupa gabungan ID, gelombang
	 * induk, dan nama grup.
	 *
	 * @return string berformat {@code "<id>-<gelombangPendaftaranPegawai>_<nama>"}.
	 */
	public String toString() {
		return id + "-" + gelombangPendaftaranPegawai + "_" + nama;
	}

	/**
	 * Gelombang pendaftaran yang menaungi grup parameter verifikasi ini. Lihat {@link
	 * #getGelombangPendaftaranPegawai()}.
	 */
	private GelombangPendaftaranPegawai gelombangPendaftaranPegawai;

	/**
	 * Nama internal grup parameter verifikasi ini. Lihat {@link #getNama()}.
	 */
	private String nama;
	/**
	 * Judul tampilan grup ini (mis. label tab/bagian formulir). Lihat {@link #getJudul()}.
	 */
	private String judul;
	/**
	 * Catatan/keterangan bebas grup ini. Lihat {@link #getKeterangan()}.
	 */
	private String keterangan;

	/**
	 * Himpunan parameter verifikasi calon pegawai yang tergabung dalam grup ini. Diinisialisasi ke
	 * {@link TreeSet} kosong (tidak pernah {@code null}). Lihat {@link
	 * #getParameterVerifikasiCalonPegawais()}.
	 */
	private Set<ParameterVerifikasiCalonPegawai> parameterVerifikasiCalonPegawais = new TreeSet<ParameterVerifikasiCalonPegawai>();

	/**
	 * Mengambil himpunan parameter verifikasi calon pegawai yang tergabung dalam grup ini. Relasi
	 * many-to-many lewat tabel pivot {@code gelombang_verifikasi_calon_pegawai_punya_parameter}
	 * (kolom {@code gelombang}/{@code parameter}) dengan {@code cascade = {MERGE, PERSIST}} —
	 * entity {@link ParameterVerifikasiCalonPegawai} baru bisa ikut dipersist otomatis saat grup
	 * ini disimpan, tapi tidak ada {@code CascadeType.REMOVE}: menghapus dari set ini hanya
	 * melepas baris pivot, tidak menghapus entity parameter itu sendiri (parameter tetap bisa
	 * dipakai grup lain).
	 *
	 * @return set (tidak pernah {@code null}) parameter verifikasi dalam grup ini.
	 */
	@ManyToMany(targetEntity = ParameterVerifikasiCalonPegawai.class, cascade = { CascadeType.MERGE,
			CascadeType.PERSIST })
	@JoinTable(name = "gelombang_verifikasi_calon_pegawai_punya_parameter", schema = "public", joinColumns = @JoinColumn(name = "gelombang"), inverseJoinColumns = @JoinColumn(name = "parameter"))
	public Set<ParameterVerifikasiCalonPegawai> getParameterVerifikasiCalonPegawais() {
		return parameterVerifikasiCalonPegawais;
	}

	public void setParameterVerifikasiCalonPegawais(Set<ParameterVerifikasiCalonPegawai> parameterVerifikasiCalonPegawais) {
		this.parameterVerifikasiCalonPegawais = parameterVerifikasiCalonPegawais;
	}

	/**
	 * Konstruktor kosong yang disyaratkan Hibernate/JPA untuk instansiasi lewat refleksi. Field
	 * lain (nama, judul, gelombang induk, parameter verifikasi) harus diisi terpisah lewat setter.
	 */
	public GelombangPendaftaranPegawaiPunyaParameterVerifikasiCalonPegawai() {
	}

	/**
	 * Mengambil primary key baris ini.
	 *
	 * @return ID grup, atau {@code null} untuk instance transient.
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
	 * Mengambil gelombang pendaftaran yang menaungi grup ini. Relasi {@code @ManyToOne} dengan
	 * {@code FetchMode.SELECT} (query terpisah saat diakses); kolom FK tidak menyatakan {@code
	 * nullable} secara eksplisit sehingga mengikuti default JPA ({@code true}, opsional).
	 *
	 * @return {@link GelombangPendaftaranPegawai} induk, atau {@code null} bila grup belum/tidak
	 * terikat gelombang manapun.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "gelombang_pendaftaran_pegawai")
	public GelombangPendaftaranPegawai getGelombangPendaftaranPegawai() {
		return gelombangPendaftaranPegawai;
	}

	public void setGelombangPendaftaranPegawai(GelombangPendaftaranPegawai gelombangPendaftaranPegawai) {
		this.gelombangPendaftaranPegawai = gelombangPendaftaranPegawai;
	}

	/**
	 * Mengambil nama internal grup parameter verifikasi ini.
	 *
	 * @return nama grup, atau {@code null} bila belum diisi.
	 */
	public String getNama() {
		return nama;
	}

	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengambil judul tampilan grup ini.
	 *
	 * @return judul grup, atau {@code null} bila belum diisi.
	 */
	public String getJudul() {
		return judul;
	}

	public void setJudul(String judul) {
		this.judul = judul;
	}

	/**
	 * Mengambil catatan/keterangan bebas grup ini.
	 *
	 * @return keterangan, atau {@code null} bila belum diisi.
	 */
	public String getKeterangan() {
		return keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

}
