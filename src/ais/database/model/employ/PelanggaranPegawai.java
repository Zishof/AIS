package ais.database.model.employ;
// Generated 10 Okt 18 12:46:07 by Hibernate Tools 5.2.3.Final

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
 * Entitas Hibernate katalog jenis pelanggaran kepegawaian AIS — dipetakan ke tabel
 * {@code employ.pelanggaran_pegawai}. Baris entitas ini adalah master (mis. "Terlambat Masuk
 * Kerja", "Tidak Memakai Seragam") berikut bobot poin ({@link #getPoint()}) yang dipakainya;
 * entitas ini sendiri TIDAK menyimpan catatan kejadian pelanggaran oleh pegawai tertentu. Struktur
 * field, konstruktor, dan pola getter/setternya identik dengan {@link HukumanPegawai} (pasangan
 * katalog pelanggaran/hukuman) — lihat "Rantai disiplin pegawai" pada Javadoc kelas tersebut untuk
 * gambaran alur lengkap.
 *
 * @see HukumanPegawai
 * @see PelanggaranDanHukumanPegawai
 * @see PendataanPelanggaranPegawai
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(name = "pelanggaran_pegawai", schema = "employ")
public class PelanggaranPegawai extends GeneralValueObject {

	/**
	 * Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable}.
	 */
	private static final long serialVersionUID = -7490758846785025664L;
	/** Primary key baris ini, di-generate database (IDENTITY). */
	private Long id;
	/** Nama pengguna audit terakhir yang mengubah baris ini. */
	private String oleh;
	/** Id pengguna audit terakhir yang mengubah baris ini (pasangan {@link #oleh}). */
	private String olehId;

	/** @return {@link #olehId} — id pengguna audit terakhir yang mengubah baris ini. */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Meng-set {@link #olehId}; dilewati (no-op) bila {@code olehId} {@code null} atau kosong/hanya
	 * berisi spasi, sehingga nilai audit lama tidak pernah tertimpa nilai kosong.
	 *
	 * @param olehId id pengguna yang melakukan perubahan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Meng-set {@link #oleh}; dilewati (no-op) bila {@code oleh} {@code null} atau kosong/hanya
	 * berisi spasi, sehingga nilai audit lama tidak pernah tertimpa nilai kosong.
	 *
	 * @param oleh nama pengguna yang melakukan perubahan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/** @return {@link #oleh} — nama pengguna audit terakhir yang mengubah baris ini. */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}, dipanggil otomatis oleh Hibernate tepat sebelum baris ini
	 * di-UPDATE; mendelegasikan pembaruan {@link #tanggal_dirubah} ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}. Tidak dipanggil manual
	 * dari kode aplikasi.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** @param tanggal_dirubah nilai timestamp audit baru; dipanggil manual maupun otomatis oleh {@link #onUpdate()}. */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return {@link #tanggal_dirubah} — timestamp terakhir baris ini diubah; nilai awal saat konstruksi objek adalah waktu sekarang. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Keterangan/deskripsi bebas untuk jenis pelanggaran ini, boleh {@code null}. */
	private String keterangan;
	/** Nama jenis pelanggaran (mis. "Terlambat Masuk Kerja"). */
	private String nama;

	/** Menandai apakah jenis pelanggaran ini masih aktif/boleh dipilih; {@code null} diperlakukan sebagai aktif (lihat {@link #getAktif()}). */
	private Boolean aktif;
	/** Bobot poin pelanggaran ini, dipakai untuk akumulasi/penilaian disiplin pegawai. */
	private Double point;

	/** Konstruktor default (dibutuhkan Hibernate); tidak menginisialisasi field apa pun secara eksplisit selain default deklarasi field. */
	public PelanggaranPegawai() {
	}

	/**
	 * Konstruktor kenyamanan untuk membuat baris dengan {@link #id} dan {@link #nama} langsung
	 * terisi (mis. dipakai untuk data referensi/lookup ringan tanpa harus memanggil setter satu per
	 * satu). Field lain (poin, keterangan, status aktif) TIDAK ikut diisi oleh konstruktor ini.
	 *
	 * @param id   primary key yang akan di-set langsung (bukan menunggu generate database)
	 * @param nama nama jenis pelanggaran
	 */
	public PelanggaranPegawai(long id, String nama) {
		this.id = id;
		this.nama = nama;
	}

	/** @return {@link #id} — primary key baris ini. */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id primary key baru; normalnya di-generate database, jarang di-set manual dari kode aplikasi. */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return {@link #keterangan} — keterangan/deskripsi bebas jenis pelanggaran ini, boleh {@code null}. */
	@Column(name = "keterangan")
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan keterangan/deskripsi bebas baru. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** @return {@link #nama} — nama jenis pelanggaran ini (tidak di-trim, berbeda dengan beberapa entitas lain di paket ini). */
	@Column(name = "nama", nullable = false)
	public String getNama() {
		return this.nama;
	}

	/** @param nama nama jenis pelanggaran baru. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return {@link #aktif}; {@code true} bila belum pernah di-set ({@code null}) — default aktif. */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/** @param aktif status aktif/nonaktif baru untuk jenis pelanggaran ini. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/** @return {@link #point}; {@code 0.0} bila belum pernah di-set ({@code null}). */
	public Double getPoint() {
		return point == null ? 0.0 : point;
	}

	/** @param point bobot poin pelanggaran baru. */
	public void setPoint(Double point) {
		this.point = point;
	}
}
