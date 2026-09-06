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
 * Entity master data <b>skala kegiatan kemahasiswaan</b> pada tabel
 * {@code public.skala_kegiatan_kemahasiswaan} (mis. "Lokal", "Regional", "Nasional",
 * "Internasional") yang menyatakan tingkat/cakupan penyelenggaraan sebuah
 * {@link KegiatanKemahasiswaan}. Skala ini lazim ikut menentukan bobot penilaian/poin
 * kemahasiswaan pada layar {@code SkalaKegiatanKemahasiswaanAction} dan dipakai sebagai daftar
 * pilihan pada {@code KegiatanKemahasiswaanAction}/{@code NilaiKegiatanKemahasiswaanAction}.
 *
 * <p>Selain {@link #getNama()} dan {@link #getKeterangan()}, kelas ini punya sakelar
 * satu-arah {@link #getAktif()} (default {@code true} bila {@code null}) dan
 * {@link #getNomorUrut()} (default {@code 1} bila {@code null}) untuk urutan tampil.</p>
 *
 * <p>Field audit ({@code id}, {@code oleh}, {@code olehId}, {@code tanggal_dirubah})
 * dideklarasikan ulang di sini karena {@link GeneralValueObject} bukan
 * {@code @Entity}/{@code @MappedSuperclass}; kontrak umumnya didokumentasikan di kelas
 * tersebut.</p>
 *
 * @see GeneralValueObject
 * @see KegiatanKemahasiswaan
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "skala_kegiatan_kemahasiswaan")

public class SkalaKegiatanKemahasiswaan extends GeneralValueObject {

	/**
	 * Versi serialisasi Java; dibiarkan sama dengan banyak entity sejenis hasil generate
	 * {@code hbm2java} 2010.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama tabel {@code public.skala_kegiatan_kemahasiswaan} ({@code IDENTITY}). */
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

	/** Nama skala kegiatan (mis. "Nasional"); wajib diisi. */
	private String nama;
	/** Catatan/keterangan bebas tentang skala ini; boleh {@code null}. */
	private String keterangan;
	/** Sakelar aktif/non-aktif satu-arah; {@code null} diperlakukan sebagai {@code true}. */
	private Boolean aktif;
	/** Nomor urut tampil pada daftar pilihan; {@code null} diperlakukan sebagai {@code 1}. */
	private Integer nomorUrut;

	/** Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA. */
	public SkalaKegiatanKemahasiswaan() {
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

	/** @return nama skala, di-trim; {@code null} bila belum pernah diisi. */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/** @param nama nama skala; disimpan apa adanya, trimming terjadi di {@link #getNama()}. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return keterangan bebas skala ini apa adanya, tanpa normalisasi. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan catatan/keterangan bebas untuk skala ini. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return status aktif/non-aktif skala ini. <b>Efek samping:</b> {@code null} diganti
	 *         {@code true} pada nilai kembalian, tanpa menulis balik ke field.
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/** @param aktif {@code true} bila skala ini masih boleh dipilih. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * @return nomor urut tampil skala ini pada daftar pilihan. <b>Efek samping:</b>
	 *         {@code null} diganti {@code 1} pada nilai kembalian, tanpa menulis balik ke
	 *         field (bukan getter-mutasi).
	 */
	public Integer getNomorUrut() {
		return nomorUrut == null ? 1 : nomorUrut;
	}

	/** @param nomorUrut nomor urut tampil; {@code null} akan tampil sebagai 1 saat dibaca. */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

}
