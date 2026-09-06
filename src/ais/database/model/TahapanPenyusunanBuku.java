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
 * Entity master data <b>tahapan penyusunan buku</b> pada tabel
 * {@code public.tahapan_penyusunan_buku} (mis. "Draf", "Review", "Cetak") yang dipakai layar
 * {@code TahapanPenyusunanBukuAction} sebagai daftar tahap penulisan/penerbitan
 * {@link BukuBahanAjar}, masing-masing dengan {@link #getProsentase()} kontribusi terhadap
 * progres keseluruhan.
 *
 * <p>Berbeda dari {@link #getAktif()} (default {@code true} bila {@code null}),
 * {@link #getProsentase()} <b>tidak</b> punya nilai bawaan — dikembalikan apa adanya termasuk
 * {@code null} bila belum diisi, sehingga pemanggil yang menjumlahkan prosentase seluruh tahap
 * perlu menjaga sendiri kemungkinan {@code NullPointerException} pada unboxing.</p>
 *
 * <p>Field audit ({@code id}, {@code oleh}, {@code olehId}, {@code tanggal_dirubah})
 * dideklarasikan ulang di sini karena {@link GeneralValueObject} bukan
 * {@code @Entity}/{@code @MappedSuperclass}; kontrak umumnya didokumentasikan di kelas
 * tersebut.</p>
 *
 * @see GeneralValueObject
 * @see BukuBahanAjar
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "tahapan_penyusunan_buku")

public class TahapanPenyusunanBuku extends GeneralValueObject {

	/**
	 * Versi serialisasi Java; dibiarkan sama dengan banyak entity sejenis hasil generate
	 * {@code hbm2java} 2010.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama tabel {@code public.tahapan_penyusunan_buku} ({@code IDENTITY}). */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini (bayangan field audit). */
	private String oleh;
	/** ID pengguna terakhir yang mengubah baris ini (bayangan field audit). */
	private String olehId;

	/**
	 * @return ID pengguna yang terakhir mengubah baris ini, atau {@code null} bila belum
	 *         pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan ID pengguna pengubah terakhir. Masukan {@code null}/kosong/spasi diabaikan
	 * diam-diam (early return) sehingga nilai audit lama tidak bisa dihapus lewat setter ini.
	 *
	 * @param olehId ID pengguna; nilai kosong/{@code null} tidak berpengaruh
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama pengguna pengubah terakhir. Sama seperti {@link #setOlehId(String)},
	 * masukan kosong/{@code null} diabaikan diam-diam.
	 *
	 * @param oleh nama pengguna; nilai kosong/{@code null} tidak berpengaruh
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * @return nama pengguna yang terakhir mengubah baris ini, atau {@code null} bila belum
	 *         pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}, dipanggil Hibernate sebelum {@code UPDATE} untuk mengisi
	 * ulang {@code oleh}/{@code olehId}/{@code tanggal_dirubah} lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}. Tidak untuk
	 * dipanggil langsung dari kode aplikasi.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** @param tanggal_dirubah waktu perubahan terakhir; boleh {@code null}. */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return waktu perubahan terakhir baris ini. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @return representasi teks berbentuk {@code "<id>-<nama>"}, dipakai label combobox ZK. */
	public String toString() {
		return id + "-" + nama;
	}

	/** Nama tahapan penyusunan buku (mis. "Draf"); wajib diisi. */
	private String nama;
	/** Catatan/keterangan bebas tentang tahapan ini; boleh {@code null}. */
	private String keterangan;
	/** Kontribusi prosentase tahapan ini terhadap progres keseluruhan; boleh {@code null}, tanpa nilai bawaan. */
	private Double prosentase;
	/** Sakelar aktif/non-aktif satu-arah; {@code null} diperlakukan sebagai {@code true}. */
	private Boolean aktif;

	/** Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA. */
	public TahapanPenyusunanBuku() {
	}

	/**
	 * @return kunci utama baris ini, di-generate basis data ({@code IDENTITY}); {@code null}
	 *         sebelum baris pertama kali disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id kunci utama baris ini. */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return nama tahapan, di-trim; {@code null} bila belum pernah diisi. */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/** @param nama nama tahapan; disimpan apa adanya, trimming terjadi di {@link #getNama()}. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return keterangan bebas tahapan ini apa adanya, tanpa normalisasi. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan catatan/keterangan bebas untuk tahapan ini. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return kontribusi prosentase tahapan ini apa adanya, termasuk {@code null} bila belum
	 *         diisi — <b>tidak</b> ada nilai bawaan seperti field lain di kelas ini.
	 */
	public Double getProsentase() {
		return prosentase;
	}

	/** @param prosentase kontribusi prosentase tahapan ini terhadap progres keseluruhan. */
	public void setProsentase(Double prosentase) {
		this.prosentase = prosentase;
	}

	/**
	 * @return status aktif/non-aktif tahapan ini. <b>Efek samping:</b> {@code null} diganti
	 *         {@code true} pada nilai kembalian, tanpa menulis balik ke field.
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/** @param aktif {@code true} bila tahapan ini masih boleh dipilih. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

}
