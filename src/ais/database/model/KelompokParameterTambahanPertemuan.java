package ais.database.model;

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

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;

/**
 * Entity Hibernate/JPA untuk tabel {@code public.kelompok_parameter_tambahan_pertemuan} —
 * <b>kelompok tampilan</b> yang mengelompokkan sejumlah {@link ParameterTambahanPertemuan}
 * (definisi field kustom/dinamis untuk konteks Pertemuan) ke dalam satu form/tab, dengan urutan
 * tampil ({@link #getNomorUrut()}), cakupan Fakultas/Jurusan/Yayasan/Sekolah, penanda audiens
 * ({@link #getUntukDosenDanAdmin()}), dan penanda apakah field-fieldnya diisi per-peserta
 * ({@link #getDiisiPerPeserta()}) atau sekali untuk seluruh pertemuan.
 *
 * @see ParameterTambahanPertemuan
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "kelompok_parameter_tambahan_pertemuan")

public class KelompokParameterTambahanPertemuan extends GeneralValueObject {

	/**
	 * ID versi serialisasi Java untuk kompatibilitas antar build (bukan kolom database).
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key baris {@code kelompok_parameter_tambahan_pertemuan}, kolom {@code id} (identity, auto-generate). */
	private Long id;
	/** Nama/username aktor yang membuat/terakhir mengubah baris ini (field audit longgar, bukan FK). */
	private String oleh;
	/** ID aktor yang membuat/terakhir mengubah baris ini (pasangan {@link #oleh}, bukan FK). */
	private String olehId;

	/**
	 * @return ID aktor ({@link #olehId}) yang tercatat membuat/mengubah baris ini; boleh {@code null}.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel ID aktor audit. Setter ini <b>fail-closed diam-diam</b>: nilai {@code null} atau
	 * string kosong/berspasi diabaikan sepenuhnya (nilai lama tetap dipertahankan), tanpa
	 * exception maupun log.
	 *
	 * @param olehId ID aktor baru; nilai kosong/{@code null} tidak berefek
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama aktor audit. Sama seperti {@link #setOlehId(String)}: nilai {@code null} atau
	 * kosong/berspasi diabaikan diam-diam, nilai lama dipertahankan.
	 *
	 * @param oleh nama aktor baru; nilai kosong/{@code null} tidak berefek
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * @return nama aktor ({@link #oleh}) yang tercatat membuat/mengubah baris ini; boleh {@code null}.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil otomatis oleh Hibernate tepat sebelum {@code
	 * UPDATE} dieksekusi, mendelegasikan ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah}
	 * untuk memperbarui jejak audit "terakhir diubah" milik entity ini.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu "terakhir diubah" secara manual. Field ini juga diinisialisasi ke
	 * waktu sekarang saat instance dibuat, dan ditulis ulang otomatis oleh {@link #onUpdate()}
	 * setiap kali baris di-{@code UPDATE}.
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return stempel waktu terakhir baris ini diubah (kolom timestamp), diisi otomatis oleh
	 *         {@link #onUpdate()} pada setiap {@code UPDATE}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi ringkas untuk log/debug: {@code "<id>-<nama>"}.
	 *
	 * @return string ringkas identitas kelompok ini
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Nama kelompok tampilan ini. */
	private String nama;
	/** Keterangan bebas kelompok ini. */
	private String keterangan;
	/** Flag penanda kelompok default/bawaan sistem; hanya satu baris yang seharusnya bernilai {@code true} (dijaga oleh {@link #checkCreateDefault()}). */
	private Boolean defaultData;
	/** Flag aktif kelompok ini; default {@code true} bila belum diisi. */
	private Boolean aktif;
	/** Flag: kelompok ini khusus ditujukan untuk dosen dan admin (bukan mahasiswa/peserta biasa). */
	private Boolean untukDosenDanAdmin;
	/** Urutan tampil kelompok ini, juga dipakai {@link #compareTo(GeneralValueObject)}; default 1 bila kosong. */
	private Integer nomorUrut;
	/** Flag: field-field dalam kelompok ini diisi per-peserta (bukan sekali untuk seluruh pertemuan); default {@code true} bila belum diisi. */
	private Boolean diisiPerPeserta;
	/** Cakupan Jurusan (Prodi) kelompok ini. */
	private Jurusan jurusan;
	/** Cakupan Fakultas (Institusi) kelompok ini. */
	private Fakultas fakultas;
	/** Cakupan Yayasan (modul sekolah) kelompok ini; lihat {@link #setYayasan(Yayasan)} untuk normalisasi ID kosong. */
	private Yayasan yayasan;
	/** Cakupan Sekolah (modul sekolah) kelompok ini; lihat {@link #setSekolah(Sekolah)} untuk normalisasi ID kosong. */
	private Sekolah sekolah;
	/** Posisi kolom tampilan kelompok ini; default 1 bila kosong. */
	private Integer kolomKe;

	/**
	 * Mengambil kelompok tampilan default/bawaan sistem ({@link #defaultData} bernilai {@code
	 * true}), MEMBUATNYA bila belum ada satu pun (nama dan keterangan "Form Tambahan").
	 *
	 * <p><b>Efek samping — manajemen sesi Hibernate manual:</b> method ini memakai {@link
	 * HibernateUtil#currentNativeSession()} (sesi Hibernate nativ, bukan lewat pola
	 * DAO/service biasa), dan pada cabang "belum ada" memulai serta meng-commit transaksi
	 * SECARA MANUAL ({@code session.getTransaction().begin()/commit()}) — <b>tidak dibungkus
	 * try/finally</b>, sehingga bila {@code session.save(...)} melempar exception, transaksi
	 * yang sudah dimulai tidak akan pernah di-commit maupun di-rollback secara eksplisit oleh
	 * method ini. Di akhir method — pada KEDUA cabang (baik baris ditemukan maupun baru dibuat)
	 * — {@link HibernateUtil#closeSession()} selalu dipanggil tanpa syarat; bila dipanggil dari
	 * konteks yang sedang berbagi sesi ambient dengan kode pemanggil, ini berisiko menutup
	 * sesi yang masih dibutuhkan pemanggil.</p>
	 *
	 * @return kelompok default yang sudah ada, atau yang baru dibuat dan disimpan
	 */
	public static KelompokParameterTambahanPertemuan checkCreateDefault() {
		Session session = HibernateUtil.currentNativeSession();
		KelompokParameterTambahanPertemuan kelompokParameterTambahanMahasiswa = (KelompokParameterTambahanPertemuan) session
				.createCriteria(KelompokParameterTambahanPertemuan.class).add(Restrictions.eq("defaultData", true))
				.setMaxResults(1).uniqueResult();
		if (kelompokParameterTambahanMahasiswa == null) {
			kelompokParameterTambahanMahasiswa = new KelompokParameterTambahanPertemuan();
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

	/**
	 * Konstruktor kosong yang dibutuhkan Hibernate untuk instansiasi entity via refleksi.
	 */
	public KelompokParameterTambahanPertemuan() {
	}

	/**
	 * @return primary key baris {@code kelompok_parameter_tambahan_pertemuan}; {@code null}
	 *         sebelum baris di-{@code INSERT}.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * @param id primary key; biasanya tidak perlu diset manual karena kolomnya {@code
	 *           insertable = false} (identity, dibangkitkan database).
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return nama kelompok tampilan ini, di-{@code trim()}; {@code null} bila field mentah
	 *         {@code null} (meski kolomnya {@code nullable = false} di skema).
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * @param nama nama kelompok baru.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * @return keterangan bebas kelompok ini; boleh {@code null}.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * @param keterangan keterangan baru untuk kelompok ini.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Flag penanda kelompok default/bawaan sistem.
	 *
	 * <p><b>Getter yang menulis balik (lazy-default):</b> bila field mentah {@code null},
	 * ditulis dan disimpan permanen menjadi {@code false} pada pembacaan pertama.</p>
	 *
	 * @return status flag ini; {@code false} bila belum diisi.
	 * @see #checkCreateDefault()
	 */
	public Boolean getDefaultData() {
		if (defaultData == null) {
			defaultData = false;
		}
		return defaultData;
	}

	/**
	 * @param defaultData nilai flag baru.
	 */
	public void setDefaultData(Boolean defaultData) {
		this.defaultData = defaultData;
	}

	/**
	 * Status aktif kelompok ini.
	 *
	 * <p><b>Getter yang menulis balik (lazy-default):</b> bila field mentah {@code null},
	 * ditulis dan disimpan permanen menjadi {@code true} pada pembacaan pertama.</p>
	 *
	 * @return status aktif; {@code true} bila belum diisi.
	 */
	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}
		return aktif;
	}

	/**
	 * @param aktif status aktif baru.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Urutan tampil kelompok ini, juga dipakai sebagai basis pengurutan pada {@link
	 * #compareTo(GeneralValueObject)}.
	 *
	 * <p><b>Getter yang menulis balik (lazy-default):</b> bila field mentah {@code null},
	 * ditulis dan disimpan permanen menjadi {@code 1} pada pembacaan pertama (pengecekan
	 * {@code null} kedua pada baris {@code return} setelahnya adalah kode mati/tidak pernah
	 * tercapai).</p>
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
	 * @param nomorUrut nomor urut baru.
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * Membandingkan urutan tampil dua {@link GeneralValueObject}.
	 *
	 * <p>Bila {@code arg0} juga {@link KelompokParameterTambahanPertemuan}, perbandingan
	 * memakai {@link #getNomorUrut()} keduanya. Selain itu, dicoba berturutan: NIM, lalu nama,
	 * lalu keterangan (perbandingan pertama yang kedua sisinya tidak {@code null} yang dipakai)
	 * — exception apa pun ditelan dan dicatat ke {@link ais.common.ErrorAuditUtil}, membuat
	 * method mengembalikan {@code 0} (dianggap setara).</p>
	 *
	 * @param arg0 object pembanding
	 * @return hasil perbandingan negatif/nol/positif sesuai {@link Comparable}; {@code 0} bila
	 *         tidak ada dasar perbandingan yang valid atau terjadi error
	 */
	@Override
	public int compareTo(GeneralValueObject arg0) {
		if (arg0 instanceof KelompokParameterTambahanPertemuan) {
			KelompokParameterTambahanPertemuan s = (KelompokParameterTambahanPertemuan) arg0;
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
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/KelompokParameterTambahanPertemuan.java:192");

			}

			return 0;
		}
	}

	/**
	 * @return {@code true} bila field-field dalam kelompok ini diisi per-peserta (bukan sekali
	 *         untuk seluruh pertemuan); default {@code true} bila belum diisi.
	 */
	public Boolean getDiisiPerPeserta() {
		return diisiPerPeserta == null ? true : diisiPerPeserta;
	}

	/**
	 * @param diisiPerPeserta nilai flag baru.
	 */
	public void setDiisiPerPeserta(Boolean diisiPerPeserta) {
		this.diisiPerPeserta = diisiPerPeserta;
	}

	/**
	 * @param jurusan cakupan jurusan baru; {@code null} untuk melepas tautan.
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * @return cakupan Jurusan (Prodi) kelompok ini (proxy lazy diresolusi via {@code check()});
	 *         boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	/**
	 * @param fakultas cakupan fakultas baru; {@code null} untuk melepas tautan.
	 */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	/**
	 * @return cakupan Fakultas (Institusi) kelompok ini (proxy lazy diresolusi via {@code
	 *         check()}); boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "fakultas", nullable = true)
	public Fakultas getFakultas() {
		fakultas = check(fakultas);
		return fakultas;
	}

	/**
	 * Menautkan yayasan cakupan kelompok ini. Menormalkan input: yayasan {@code null} ATAU
	 * yang ID-nya {@code null} (belum tersimpan/transient) sama-sama disimpan sebagai {@code
	 * null}, mencegah tautan ke entity yayasan yang belum ter-{@code persist}.
	 *
	 * @param yayasan yayasan baru; entity tanpa ID diperlakukan sama seperti {@code null}.
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan == null || yayasan.getId() == null ? null : yayasan;
	}

	/**
	 * @return cakupan Yayasan (modul sekolah) kelompok ini (proxy lazy diresolusi via {@code
	 *         check()}); boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan", nullable = true)
	public Yayasan getYayasan() {
		yayasan = check(yayasan);
		return yayasan;
	}

	/**
	 * @return cakupan Sekolah (modul sekolah) kelompok ini (proxy lazy diresolusi via {@code
	 *         check()}); boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah", nullable = true)
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return sekolah;
	}

	/**
	 * Menautkan sekolah cakupan kelompok ini. Normalisasi input sama seperti {@link
	 * #setYayasan(Yayasan)}: sekolah {@code null} atau tanpa ID disimpan sebagai {@code null}.
	 *
	 * @param sekolah sekolah baru; entity tanpa ID diperlakukan sama seperti {@code null}.
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah == null || sekolah.getId() == null ? null : sekolah;
	}

	/**
	 * @return {@code true} bila kelompok ini khusus ditujukan untuk dosen dan admin; default
	 *         {@code false} bila belum diisi.
	 */
	public Boolean getUntukDosenDanAdmin() {
		return untukDosenDanAdmin == null ? false : untukDosenDanAdmin;
	}

	/**
	 * @param untukDosenDanAdmin nilai flag baru.
	 */
	public void setUntukDosenDanAdmin(Boolean untukDosenDanAdmin) {
		this.untukDosenDanAdmin = untukDosenDanAdmin;
	}

	/**
	 * @return posisi kolom tampilan kelompok ini; {@code 1} bila belum diisi.
	 */
	public Integer getKolomKe() {
		return kolomKe == null ? 1 : kolomKe;
	}

	/**
	 * @param kolomKe posisi kolom baru.
	 */
	public void setKolomKe(Integer kolomKe) {
		this.kolomKe = kolomKe;
	}

}
