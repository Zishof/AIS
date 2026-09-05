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




import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;



import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;

/**
 * Kelompok (grup) form isian parameter tambahan yang dapat dipasangkan ke satu atau lebih {@link
 * KegiatanTugasJabatan} (lihat {@link KegiatanTugasJabatan#getKelompokParameterTambahanKegiatans()},
 * relasi many-to-many via tabel {@code kegiatan_has_parameter}). Satu kelompok mengelompokkan
 * sejumlah item {@link ParameterTambahanKegiatan} yang dirujuk lewat {@code
 * kelompokParameterTambahanKegiatan} pada entity tersebut; nilai aktual yang diisi pengguna untuk
 * tiap kombinasi kelompok+parameter pada satu realisasi disimpan sebagai string terserialisasi di
 * {@link ais.database.model.lkp.RealisasiKerjaPegawai#getParameterTambahan()}, dengan lampiran
 * (bila ada) diberi kunci "jenis" lewat {@link ais.database.model.file.LampiranLain#resolveJenisParameterTambahan}
 * — mekanisme namespace jenis/ref yang sama dengan yang sudah dipakai secara luas di modul lain
 * (lihat catatan arsitektur "tabrakan namespace jenis/ref parameter tambahan"); di paket LKP,
 * pemakaiannya di {@link ais.database.model.lkp.RealisasiKerjaPegawai#populateParameterTambahan}
 * sudah memakai resolver tersebut, sehingga instance ini konsisten dengan perbaikan yang sudah ada,
 * bukan instance baru yang belum ditambal.
 *
 * @see KegiatanTugasJabatan
 * @see ParameterTambahanKegiatan
 * @see ais.database.model.lkp.RealisasiKerjaPegawai
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "kelompok_parameter_tambahan_kegiatan")



public class KelompokParameterTambahanKegiatan extends GeneralValueObject {

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
	 * {@link #getNama() nama} kelompok.
	 *
	 * @return string {@code "<id>-<nama>"}.
	 */
	public String toString() {
		return id + "-" + nama;
	}

	private String nama;
	private String keterangan;
	private Boolean defaultData;
	private Boolean aktif;
	private Integer nomorUrut;

	/**
	 * Mengambil kelompok parameter tambahan yang ditandai {@code defaultData = true}, atau
	 * membuatnya bila belum ada — memastikan selalu tersedia minimal satu kelompok default bernama
	 * "Form Tambahan" untuk dipakai sebagai fallback saat kegiatan belum secara eksplisit memasang
	 * kelompok parameter tambahan mana pun.
	 *
	 * <p><b>Efek samping penting:</b> method ini membuka sesi Hibernate baru ({@link
	 * HibernateUtil#currentNativeSession()}), memulai dan meng-commit transaksinya sendiri secara
	 * mandiri (tidak berpartisipasi dalam transaksi pemanggil), lalu menutup sesi lewat {@link
	 * HibernateUtil#closeSession()} di akhir — pemanggil tidak perlu (dan tidak boleh) membungkus
	 * pemanggilan ini dalam transaksi sendiri.</p>
	 *
	 * @return kelompok parameter tambahan default yang sudah ada atau baru dibuat; tidak pernah
	 *         {@code null}.
	 */
	public static KelompokParameterTambahanKegiatan checkCreateDefault() {
		Session session = HibernateUtil.currentNativeSession();
		KelompokParameterTambahanKegiatan kelompokParameterTambahanMahasiswa = (KelompokParameterTambahanKegiatan) session
				.createCriteria(KelompokParameterTambahanKegiatan.class).add(Restrictions.eq("defaultData", true))
				.setMaxResults(1).uniqueResult();
		if (kelompokParameterTambahanMahasiswa == null) {
			kelompokParameterTambahanMahasiswa = new KelompokParameterTambahanKegiatan();
			kelompokParameterTambahanMahasiswa.setDefaultData(true);
			kelompokParameterTambahanMahasiswa.setNama("Form Tambahan");
			kelompokParameterTambahanMahasiswa.setKeterangan("Form Tambahan");
			session.getTransaction().begin();
			session.save(kelompokParameterTambahanMahasiswa);
			session.getTransaction().commit();
		}

		HibernateUtil.closeSession();
		return kelompokParameterTambahanMahasiswa;
	}

	/** Konstruktor default (dibutuhkan Hibernate/JPA). */
	public KelompokParameterTambahanKegiatan() {
	}

	/**
	 * Mengembalikan id primary key kelompok ini. Dipetakan {@code insertable = false} karena nilai
	 * dibangkitkan basis data (identity).
	 *
	 * @return id kelompok, atau {@code null} untuk instance yang belum tersimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan id kelompok ini.
	 *
	 * @param id id kelompok.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama kelompok parameter tambahan, di-trim dari whitespace di kedua ujung.
	 * Kolom wajib diisi dengan panjang maksimum 255 karakter.
	 *
	 * @return nama kelompok yang sudah di-trim, atau {@code null} bila field belum diisi.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan nama kelompok parameter tambahan.
	 *
	 * @param nama nama kelompok.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan/deskripsi bebas untuk kelompok ini.
	 *
	 * @return keterangan kelompok, dapat {@code null}.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan/deskripsi bebas untuk kelompok ini.
	 *
	 * @param keterangan keterangan kelompok.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan penanda apakah kelompok ini adalah kelompok default sistem (dipakai oleh
	 * {@link #checkCreateDefault()} untuk mencari/menjamin keberadaan satu kelompok default).
	 *
	 * @return {@code true} bila kelompok default; {@code false} bila belum diisi atau memang bukan
	 *         default.
	 */
	public Boolean getDefaultData() {
		if (defaultData == null) {
			defaultData = false;
		}
		return defaultData;
	}

	/**
	 * Menetapkan penanda kelompok default sistem.
	 *
	 * @param defaultData penanda default baru.
	 */
	public void setDefaultData(Boolean defaultData) {
		this.defaultData = defaultData;
	}

	/**
	 * Mengembalikan status aktif kelompok ini; kelompok tidak aktif umumnya disembunyikan dari
	 * picker pemasangan baru pada kegiatan.
	 *
	 * @return {@code true} bila aktif atau belum diisi (default aktif); {@code false} bila
	 *         dinonaktifkan eksplisit.
	 */
	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}
		return aktif;
	}

	/**
	 * Menetapkan status aktif kelompok ini.
	 *
	 * @param aktif status aktif baru.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan nomor urut tampil kelompok ini, dipakai juga sebagai kunci pengurutan oleh
	 * {@link #compareTo(GeneralValueObject)} dan oleh {@code @OrderBy} pada
	 * {@link KegiatanTugasJabatan#getKelompokParameterTambahanKegiatans()}.
	 *
	 * @return nomor urut; {@code 1} bila belum diisi.
	 */
	public Integer getNomorUrut() {
		if (nomorUrut == null) {
			nomorUrut = 1;
		}
		return nomorUrut == null ? 1 : nomorUrut;
	}

	/**
	 * Menetapkan nomor urut tampil kelompok ini.
	 *
	 * @param nomorUrut nomor urut baru.
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}


	/**
	 * Membandingkan urutan dengan objek {@link GeneralValueObject} lain. Bila keduanya sama-sama
	 * {@link KelompokParameterTambahanKegiatan}, pengurutan memakai {@link #getNomorUrut()}. Untuk
	 * tipe {@code GeneralValueObject} lain (perbandingan lintas-tipe yang jarang terjadi dalam
	 * praktik), method ini mencoba berturut-turut membandingkan NIM, lalu nama, lalu keterangan —
	 * exception yang timbul (mis. method {@code getNim()}/{@code getNama()} tidak relevan untuk
	 * tipe tersebut) ditelan dan dicatat lewat {@link ais.common.ErrorAuditUtil#record}, mengembalikan
	 * {@code 0} (dianggap setara) sebagai fallback aman.
	 *
	 * @param arg0 objek pembanding.
	 * @return hasil perbandingan negatif/nol/positif sesuai kontrak {@link Comparable}.
	 */
	@Override
	public int compareTo(GeneralValueObject arg0) {
		if (arg0 instanceof KelompokParameterTambahanKegiatan) {
			KelompokParameterTambahanKegiatan s = (KelompokParameterTambahanKegiatan) arg0;
			return getNomorUrut().compareTo(s.getNomorUrut());
		} else {
			try {
				if (getNim() != null && arg0.getNim() != null) {
					return getNim().compareTo(arg0.getNim());
				} else if (getNama() != null && arg0.getNama() != null) {
					return getNama().compareTo(arg0.getNama());
				} else if (getKeterangan() != null && arg0.getKeterangan() != null) {
					return getKeterangan().compareTo(arg0.getKeterangan());
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/lkp/KelompokParameterTambahanKegiatan.java:188");

			}

			return 0;
		}
	}
}
