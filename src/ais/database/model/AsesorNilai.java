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
 * Entity Hibernate untuk tabel {@code public.asesor_nilai} &mdash; master <b>jenis
 * kegiatan penilaian asesor</b> beserta bobot SKS (satuan kredit semester)-nya, dipakai
 * pada modul penilaian kinerja dosen/pegawai berbasis asesor (BKD/LKP): mis. "Bimbingan"
 * (skripsi/tesis), "Penguji", "Bimbingan KKN", "Bimbingan PKL", "Penelitian", "Pengabdian".
 *
 * <p>Konstanta {@link #BIMBINGAN}, {@link #PENGUJI}, {@link #BIMBINGAN_KKN},
 * {@link #BIMBINGAN_PKL}, {@link #PENELITIAN}, dan {@link #PENGABDIAN} mendokumentasikan
 * nilai baku {@link #getNama()} yang dipakai saat data ini di-seed, tetapi <b>tidak
 * direferensikan lagi oleh kode lain</b> di repo ini (tidak ada pemanggil
 * {@code AsesorNilai.BIMBINGAN} dkk.) &mdash; kemungkinan sisa referensi dari implementasi
 * awal atau label acuan bagi operator saat mengisi data lewat CRUD master.
 *
 * <p>Dikelola lewat CRUD master data di {@code ais.action.master.bkd.AsesorNilaiAction},
 * dengan pemeriksaan duplikasi nama ({@code checkNamaAsesorNilai()}) sebelum simpan.
 * Diturunkan dari {@link GeneralValueObject}; {@code id}, {@code oleh}, {@code olehId}, dan
 * {@link #tanggal_dirubah} dideklarasikan ulang di sini karena kelas induk adalah POJO
 * abstrak biasa (bukan {@code @Entity}/{@code @MappedSuperclass}) &mdash; keharusan teknis,
 * bukan duplikasi keliru.
 *
 * @see Asesor
 * @see AsesorPegawai
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "asesor_nilai")

public class AsesorNilai extends GeneralValueObject {

	/** Label baku jenis kegiatan "Bimbingan" (skripsi/tesis), lihat catatan kelas. */
	public static final String BIMBINGAN = "Bimbingan";
	/** Label baku jenis kegiatan "Penguji". */
	public static final String PENGUJI = "Penguji";
	/** Label baku jenis kegiatan "Bimbingan KKN". */
	public static final String BIMBINGAN_KKN = "Bimbingan KKN";
	/** Label baku jenis kegiatan "Bimbingan PKL". */
	public static final String BIMBINGAN_PKL = "Bimbingan PKL";

	/** Label baku jenis kegiatan "Penelitian". */
	public static final String PENELITIAN = "Penelitian";
	/** Label baku jenis kegiatan "Pengabdian" (pengabdian masyarakat). */
	public static final String PENGABDIAN = "Pengabdian";

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	/** Nama pelaku (audit shadow, lihat {@link GeneralValueObject}) yang membuat/mengubah baris ini. */
	private String oleh;
	/** Id pelaku (audit shadow) yang membuat/mengubah baris ini. */
	private String olehId;

	/** @return id pelaku terakhir yang mengubah baris ini, atau {@code null} bila belum pernah diisi. */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pelaku. Nilai kosong/blank diabaikan (fail-safe agar audit shadow tidak
	 * tertimpa string kosong secara tidak sengaja) &mdash; bukan validasi keamanan.
	 *
	 * @param olehId id pelaku; diabaikan jika {@code null} atau hanya berisi spasi
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pelaku. Nilai kosong/blank diabaikan, sama seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pelaku; diabaikan jika {@code null} atau hanya berisi spasi
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/** @return nama pelaku terakhir yang mengubah baris ini, atau {@code null} bila belum pernah diisi. */
	public String getOleh() {
		return oleh;
	}

	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengubah stempel waktu perubahan terakhir secara manual. Nilai default sudah
	 * di-set ke waktu saat ini pada deklarasi field dan di-refresh otomatis oleh
	 * {@link #onUpdate()} pada setiap update; setter ini jarang perlu dipanggil langsung.
	 *
	 * @param tanggal_dirubah stempel waktu perubahan baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return stempel waktu perubahan terakhir baris ini. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * @return {@link #getKeterangan()} sebagai representasi baris ini, dipakai untuk
	 *     debugging/log. Perhatikan bahwa nama jenis kegiatan ({@link #getNama()}) <b>tidak</b>
	 *     ikut ditampilkan di sini, berbeda dari kebiasaan {@code id-nama} pada banyak
	 *     master lain di paket ini.
	 */
	public String toString() {
		return keterangan;
	}

	private String nama;

	private Double sks;
	private String keterangan;

	/** Konstruktor kosong yang diperlukan Hibernate. */
	public AsesorNilai() {
	}

	/** @return id baris (primary key, auto-generated identity di database). */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id id baris; kolom tidak insertable sehingga nilai ini diabaikan saat insert. */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return nama jenis kegiatan penilaian asesor apa adanya (tidak di-trim, tidak
	 *     dijamin non-{@code null}) &mdash; berbeda dari kebiasaan {@code getNama()} pada
	 *     banyak master lain di paket ini yang men-trim hasilnya.
	 */
	public String getNama() {
		return nama;
	}

	/** @param nama nama jenis kegiatan penilaian asesor. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return keterangan tambahan, sudah di-trim; string kosong ({@code ""}) bila belum diisi. */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan == null ? "" : keterangan.trim();
	}

	/** @param keterangan keterangan tambahan untuk jenis kegiatan ini. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return bobot SKS untuk jenis kegiatan ini; {@code 0.0} bila kolom masih {@code null}
	 *     (belum pernah diisi) &mdash; nilai {@code 0.0} itu sekaligus ditulis balik ke field
	 *     {@code sks} pada pemanggilan pertama.
	 */
	@Column(name = "sks_nilai", nullable = true)
	public Double getSks() {
		if (sks == null) {
			sks = 0.0;
		}
		return sks;
	}

	/** @param sks bobot SKS baru untuk jenis kegiatan ini. */
	public void setSks(Double sks) {
		this.sks = sks;
	}

}
