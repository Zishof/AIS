package ais.database.model.employ;

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

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;

/**
 * Entitas Hibernate kelompok/tab parameter tambahan untuk pengajuan cuti &amp; izin kepegawaian
 * AIS — dipetakan ke tabel {@code kelompok_parameter_tambahan_pengajuan_pegawai}. Satu kelompok
 * menaungi banyak {@link ParameterTambahanCutiDanIzin} (yang masing-masing merujuk satu
 * {@link ais.database.model.ParameterTambahan}), dan satu/lebih kelompok dirujuk oleh
 * {@link JenisCutiDanIzin#getKelompokParameterTambahanCutiDanIzins()} untuk menentukan parameter
 * tambahan apa saja yang tampil pada form pengajuan cuti/izin jenis tersebut.
 *
 * <h2>Catatan skema tabel</h2>
 * <p>
 * Class ini berada di paket {@code ais.database.model.employ}, namun anotasi {@link Table} di
 * bawah menunjuk skema {@code payroll} (BUKAN {@code employ}) — {@code @Table(schema = "payroll",
 * name = "kelompok_parameter_tambahan_pengajuan_pegawai")}. Ini konsisten dengan nama kolom join
 * {@code kelompok_parameter_tambahan_pengajuan_pegawai} yang dipakai
 * {@link ParameterTambahanCutiDanIzin#getKelompokParameterTambahanCutiDanIzin()} — bukan
 * kesalahan penamaan, melainkan penempatan tabel fisik di skema {@code payroll} sementara
 * class Java-nya dikelompokkan bersama entitas {@code employ} lain karena kedekatan domain
 * (cuti/izin kepegawaian).
 * </p>
 *
 * <h2>Verifikasi pola kunci atribut ZK (dibandingkan dengan {@code task_fe6517bf})</h2>
 * <p>
 * Ditemukan sebelumnya pada dua pasangan parameter-tambahan serupa di paket {@code payroll}
 * (pengajuan-transaksi &amp; gaji-pegawai) bahwa salah satunya menulis/membaca row-attribute ZK
 * dengan kunci milik modul lain (bug {@code task_fe6517bf}). Untuk pasangan cuti-izin ini,
 * listener penulis ({@code ais.action.master.employ.helper.ParameterTambahanCutiDanIzinListener},
 * baris {@code row.setAttribute("kelompokParameterTambahanCutiDanIzin", ...)} dengan tipe
 * {@link KelompokParameterTambahanCutiDanIzin}) dan pembaca
 * ({@link ais.database.model.payroll.CutiDanIzin#populateParameterTambahan(java.util.List)}, yang
 * meng-cast {@code row.getAttribute("kelompokParameterTambahanCutiDanIzin")} ke
 * {@link KelompokParameterTambahanCutiDanIzin} juga) memakai KUNCI DAN TIPE YANG SAMA — TIDAK
 * ditemukan bug serupa {@code task_fe6517bf} pada pasangan ini (verifikasi negatif).
 * </p>
 *
 * @see ParameterTambahanCutiDanIzin
 * @see JenisCutiDanIzin
 * @see ais.database.model.payroll.CutiDanIzin
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "payroll", name = "kelompok_parameter_tambahan_pengajuan_pegawai")
public class KelompokParameterTambahanCutiDanIzin extends GeneralValueObject {

	/**
	 * Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable}.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
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

	/** @return representasi teks baris ini: {@link #id} digabung dengan {@link #nama}. */
	public String toString() {
		return id + "-" + nama;
	}

	/** Nama kelompok parameter tambahan (mis. "Form Tambahan", judul tab pada form pengajuan). */
	private String nama;
	/** Keterangan/deskripsi bebas untuk kelompok ini, boleh {@code null}. */
	private String keterangan;
	/** Menandai apakah baris ini adalah kelompok bawaan/default sistem — lihat {@link #checkCreateDefault()}. */
	private Boolean defaultData;
	/** Menandai apakah kelompok ini masih aktif/dipakai; {@code null} diperlakukan sebagai aktif (lihat {@link #getAktif()}). */
	private Boolean aktif;
	/** Nomor urut tampilan kelompok ini relatif terhadap kelompok lain; dipakai juga oleh {@link #compareTo(GeneralValueObject)}. */
	private Integer nomorUrut;

	/**
	 * Mencari kelompok parameter tambahan bawaan ({@code defaultData = true}) lewat sesi Hibernate
	 * native baru, membuatnya ("Form Tambahan") bila belum ada di database, lalu menutup sesi yang
	 * dibuka. Dipanggil oleh kode pemanggil (mis. layar admin) untuk memastikan selalu tersedia
	 * minimal satu kelompok default tanpa perlu migrasi data manual.
	 *
	 * @return kelompok parameter tambahan bawaan yang sudah ada, atau baris baru yang baru saja
	 *         dibuat &amp; disimpan bila belum ada sebelumnya
	 */
	public static KelompokParameterTambahanCutiDanIzin checkCreateDefault() {
		Session session = HibernateUtil.currentNativeSession();
		KelompokParameterTambahanCutiDanIzin kelompokParameterTambahanCatatanAdministrasi = (KelompokParameterTambahanCutiDanIzin) session
				.createCriteria(KelompokParameterTambahanCutiDanIzin.class).add(Restrictions.eq("defaultData", true))
				.setMaxResults(1).uniqueResult();
		if (kelompokParameterTambahanCatatanAdministrasi == null) {
			kelompokParameterTambahanCatatanAdministrasi = new KelompokParameterTambahanCutiDanIzin();
			kelompokParameterTambahanCatatanAdministrasi.setDefaultData(true);
			kelompokParameterTambahanCatatanAdministrasi.setNama("Form Tambahan");
			kelompokParameterTambahanCatatanAdministrasi.setKeterangan("Form Tambahan");
			session.getTransaction().begin();
			session.save(kelompokParameterTambahanCatatanAdministrasi);
			session.getTransaction().commit();
		}

		HibernateUtil.closeSession();
		return kelompokParameterTambahanCatatanAdministrasi;
	}

	/** Konstruktor default (dibutuhkan Hibernate); tidak menginisialisasi field apa pun secara eksplisit selain default deklarasi field. */
	public KelompokParameterTambahanCutiDanIzin() {
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

	/** @return {@link #nama} yang sudah di-trim; {@code null} bila {@link #nama} {@code null}. */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/** @param nama nama kelompok parameter tambahan baru. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return {@link #keterangan} — keterangan/deskripsi bebas kelompok ini, boleh {@code null}. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan keterangan/deskripsi bebas baru. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** @return {@link #defaultData}; {@code false} bila belum pernah di-set ({@code null}). */
	public Boolean getDefaultData() {
		if (defaultData == null) {
			defaultData = false;
		}
		return defaultData;
	}

	/** @param defaultData status kelompok bawaan/default baru. */
	public void setDefaultData(Boolean defaultData) {
		this.defaultData = defaultData;
	}

	/** @return {@link #aktif}; {@code true} bila belum pernah di-set ({@code null}) — default aktif. */
	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}
		return aktif;
	}

	/** @param aktif status aktif/nonaktif baru untuk kelompok ini. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/** @return {@link #nomorUrut}; {@code 1} bila belum pernah di-set ({@code null}). */
	public Integer getNomorUrut() {
		if (nomorUrut == null) {
			nomorUrut = 1;
		}
		return nomorUrut == null ? 1 : nomorUrut;
	}

	/** @param nomorUrut nomor urut tampilan baru untuk kelompok ini. */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * Membandingkan urutan tampilan dua kelompok berdasarkan {@link #getNomorUrut()} saja (tidak
	 * memakai {@link #nama} sebagai pembanding sekunder) — dipakai oleh koleksi terurut seperti
	 * {@link java.util.TreeSet} pada {@link JenisCutiDanIzin#getKelompokParameterTambahanCutiDanIzins()}.
	 *
	 * @param arg0 objek pembanding, di-cast ke {@link KelompokParameterTambahanCutiDanIzin}
	 * @return hasil {@link Integer#compareTo(Integer)} antara {@link #getNomorUrut()} kedua objek
	 * @throws ClassCastException bila {@code arg0} bukan instance {@link KelompokParameterTambahanCutiDanIzin}
	 */
	@Override
	public int compareTo(GeneralValueObject arg0) {
		return getNomorUrut().compareTo(((KelompokParameterTambahanCutiDanIzin) arg0).getNomorUrut());
	}
}
