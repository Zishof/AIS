package ais.database.model.sirs;

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
 * Entitas katalog master komunitas pada schema {@code sirs} (tabel
 * {@code komunitas}). Diverifikasi dari javadoc kelas
 * {@code KomunitasAction}: merepresentasikan kelompok/program layanan
 * (mis. program kesehatan komunitas atau penjaminan tertentu) dengan
 * periode berlaku ({@link #getMulai()}&ndash;{@link #getSampai()}) dan
 * flag {@link #getAktif()}.
 *
 * <p>
 * Anggotanya dikelola lewat entitas junction terpisah
 * {@link KomunitasPunyaPasien} (relasi many-to-many ke {@link Pasien}).
 * Selain sebagai pengelompokan keanggotaan, komunitas juga dipakai
 * sebagai salah satu SUMBU KUALIFIKASI TARIF KHUSUS ({@code tarifKhusus})
 * bersama {@link Dokter}, {@link Asuransi}, dan {@link Pasien} — lihat
 * {@code ais.action.master.sirs.util.CommonTarif#getTarif} dan pemakaian
 * serupa di {@code CommonTarifTindakan}/{@code CommonTarifItem}/
 * {@code CommonTarifAlatMedis} — sehingga tarif suatu layanan bisa
 * berbeda untuk pasien anggota komunitas tertentu, dalam rentang tanggal
 * berlaku komunitas tersebut.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "komunitas")
public class Komunitas extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String olehId;

	/**
	 * Mengambil ID pengguna (username/oleh-id) yang terakhir mengubah baris
	 * ini. Field audit shadow yang diisi lewat {@link #setOlehId(String)}.
	 *
	 * @return ID pengguna terakhir yang mengubah, atau {@code null} jika
	 *         belum pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan ID pengguna yang mengubah baris ini. Nilai kosong/blank
	 * SENGAJA diabaikan (early return) agar field audit ini tidak pernah
	 * ditimpa jadi kosong — pola field audit shadow yang merupakan
	 * KEHARUSAN TEKNIS berulang di paket model AIS, bukan bug.
	 *
	 * @param olehId ID pengguna; nilai {@code null} atau string
	 *               kosong/whitespace-saja tidak akan mengubah nilai
	 *               tersimpan.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	private String oleh;

	/**
	 * Representasi ringkas komunitas ini untuk keperluan tampilan/log.
	 *
	 * @return nama komunitas.
	 */
	public String toString() {
		return nama;
	}

	/**
	 * Menetapkan nama pengguna yang mengubah baris ini. Nilai kosong/blank
	 * diabaikan agar field audit ini tidak pernah ditimpa kosong.
	 *
	 * @param oleh nama pengguna; nilai {@code null} atau
	 *             kosong/whitespace-saja tidak akan mengubah nilai
	 *             tersimpan.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengambil nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna terakhir yang mengubah, atau {@code null} jika
	 *         belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate} yang memperbarui {@link #tanggal_dirubah}
	 * otomatis sebelum baris ini di-UPDATE, lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = new Date();

	/**
	 * Menetapkan timestamp perubahan terakhir baris ini secara manual.
	 *
	 * @param tanggal_dirubah timestamp perubahan baru.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengambil timestamp perubahan terakhir baris ini, diperbarui
	 * otomatis oleh {@link #onUpdate()} setiap kali baris di-UPDATE.
	 *
	 * @return timestamp perubahan terakhir.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	private String nama;
	private Date mulai = new Date();
	private Date sampai;
	private String keterangan;
	private Boolean aktif;

	/**
	 * Konstruktor default (wajib untuk entitas JPA/Hibernate). Field
	 * {@link #mulai} diinisialisasi ke tanggal saat objek dibuat
	 * (bukan {@code null}), berbeda dari {@link #sampai} yang dibiarkan
	 * kosong sampai eksplisit diisi.
	 */
	public Komunitas() {
	}

	/**
	 * Primary key baris komunitas, auto-increment (IDENTITY) dan diisi
	 * database.
	 *
	 * @return ID unik komunitas ini.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan ID komunitas.
	 *
	 * @param id ID komunitas.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil nama komunitas.
	 *
	 * @return nama komunitas.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama;
	}

	/**
	 * Menetapkan nama komunitas.
	 *
	 * @param nama nama komunitas.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengambil keterangan bebas komunitas ini.
	 *
	 * @return teks keterangan, atau {@code null} jika belum diisi.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas komunitas ini.
	 *
	 * @param keterangan teks keterangan.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengambil flag aktif/tidak-aktif komunitas ini. {@code null}
	 * otomatis dibaca sebagai {@code true} (aktif) lewat lazy-init yang
	 * ditulis-balik ke field {@link #aktif} — baris lama yang belum
	 * pernah eksplisit diset akan otomatis dianggap aktif begitu getter
	 * ini dipanggil sekali.
	 *
	 * @return {@code true} jika komunitas aktif; default {@code true}
	 *         bila belum pernah diset.
	 */
	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}
		return aktif;
	}

	/**
	 * Menetapkan flag aktif/tidak-aktif komunitas ini.
	 *
	 * @param aktif {@code true} jika komunitas aktif dipakai.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengambil tanggal mulai berlaku komunitas ini. Field ini
	 * diinisialisasi ke tanggal pembuatan objek sejak deklarasi
	 * ({@code = new Date()}), sehingga tidak pernah {@code null} kecuali
	 * eksplisit diset demikian.
	 *
	 * @return tanggal mulai berlaku.
	 */
	public Date getMulai() {
		return mulai;
	}

	/**
	 * Menetapkan tanggal mulai berlaku komunitas ini.
	 *
	 * @param mulai tanggal mulai berlaku.
	 */
	public void setMulai(Date mulai) {
		this.mulai = mulai;
	}

	/**
	 * Mengambil tanggal berakhir berlaku komunitas ini.
	 *
	 * @return tanggal berakhir berlaku, atau {@code null} jika belum
	 *         diisi (dianggap tanpa batas akhir).
	 */
	public Date getSampai() {
		return sampai;
	}

	/**
	 * Menetapkan tanggal berakhir berlaku komunitas ini.
	 *
	 * @param sampai tanggal berakhir berlaku.
	 */
	public void setSampai(Date sampai) {
		this.sampai = sampai;
	}

}
