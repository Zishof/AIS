package ais.database.model.employ;
// Generated 10 Okt 18 12:46:07 by Hibernate Tools 5.2.3.Final

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.Pegawai;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.ui.util.WaktuUtil;

/**
 * Entitas Hibernate catatan kejadian pelanggaran &amp; hukuman AKTUAL yang dikenakan kepada
 * seorang pegawai — dipetakan ke tabel {@code employ.pendataan_pelanggaran_pegawai}. Berbeda dari
 * {@link PelanggaranDanHukumanPegawai} (template/aturan master), baris entitas ini terikat pada
 * {@link #getPegawai() pegawai} tertentu dan {@link #getWaktu() waktu} kejadian tertentu, merujuk
 * satu {@link #getPelanggaranDanHukumanPegawai() template} sebagai dasar SEKALIGUS menyimpan
 * salinan himpunan {@link #getPelanggaranPegawais() pelanggaran} dan
 * {@link #getHukumanPegawais() hukuman} miliknya sendiri (independen dari isi template saat ini —
 * boleh disesuaikan berbeda per kejadian, dan tidak otomatis berubah bila template diedit
 * kemudian). Subclass dari {@link DataSop}, sehingga tunduk pada alur persetujuan (approval
 * workflow) SOP/disposisi generik AIS yang sama seperti
 * {@link ais.database.model.payroll.CutiDanIzin} — lihat {@link #getDisposisiSop()} dan
 * {@link #getAktif()}. Lihat "Rantai disiplin pegawai" pada Javadoc {@link HukumanPegawai} untuk
 * gambaran alur lengkap empat entitas terkait.
 *
 * <h2>Siapa boleh mencatat pelanggaran terhadap siapa</h2>
 * <p>
 * Entitas ini sendiri TIDAK memiliki field yang membatasi hubungan antara pengguna yang membuat
 * baris (audit {@link #oleh}/{@link #olehId}) dengan {@link #pegawai} yang dicatat — pemilihan
 * pegawai target sepenuhnya bebas dari sisi model data ini (kontrol otorisasi, bila ada, berada di
 * lapisan Action/ZK pemanggil, di luar cakupan class ini). Status berlaku ({@link #getAktif()})
 * dan proses persetujuannya sepenuhnya didelegasikan ke {@link #disposisiSop} generik, pola yang
 * sama seperti dipakai modul pengajuan cuti/izin — tidak ada mekanisme self-approval/self-review
 * khusus yang terlihat berbeda di level entitas ini dibanding modul SOP/disposisi lain yang sudah
 * diaudit sebelumnya.
 * </p>
 *
 * @see PelanggaranDanHukumanPegawai
 * @see PelanggaranPegawai
 * @see HukumanPegawai
 * @see DataSop
 * @see DisposisiSop
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(name = "pendataan_pelanggaran_pegawai", schema = "employ")
public class PendataanPelanggaranPegawai extends DataSop {

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

	/** Pegawai yang dikenai catatan pelanggaran/hukuman ini. */
	private Pegawai pegawai;
	/** Template aturan pelanggaran-dan-hukuman yang menjadi dasar catatan ini. */
	private PelanggaranDanHukumanPegawai pelanggaranDanHukumanPegawai;
	/** Waktu kejadian/pencatatan pelanggaran ini. */
	private Date waktu;
	/** Keterangan/kronologi bebas untuk catatan ini, boleh {@code null}. */
	private String keterangan;
	/** Cache; nilai efektif selalu dihitung ulang di {@link #getNama()} dari kombinasi {@link #pegawai}, {@link #pelanggaranDanHukumanPegawai}, dan {@link #waktu}. */
	private String nama;
	/** Alur disposisi SOP yang menjadi sumber status persetujuan/berlaku catatan ini. */
	private DisposisiSop disposisiSop;
	/** Cache lokal status aktif; nilai efektif bisa ditimpa oleh {@link #getAktif()} berdasarkan {@link #disposisiSop} — lihat catatan pada getter tsb. */
	private Boolean aktif;

	/** Himpunan jenis hukuman AKTUAL yang diterapkan pada kejadian ini (independen dari isi {@link #pelanggaranDanHukumanPegawai} saat ini). */
	private Set<HukumanPegawai> hukumanPegawais = new HashSet<HukumanPegawai>();

	/**
	 * @return {@link #hukumanPegawais} — himpunan jenis hukuman aktual milik catatan kejadian ini,
	 *         terurut menurut {@code nama}, lewat join table
	 *         {@code pelanggaran_pegawai_dan_hukuman_has_hukuman}. Perhatikan nama join table ini
	 *         (serta kolom joinnya, {@code pelanggaran_dan_hukuman_pegawai}) MIRIP TAPI BUKAN sama
	 *         dengan join table {@link PelanggaranDanHukumanPegawai#getHukumanPegawais()}
	 *         ({@code pelanggaran_dan_hukuman_pegawai_has_hukuman}) — keduanya adalah relasi
	 *         many-to-many yang BERBEDA (template vs kejadian aktual), sengaja terpisah agar
	 *         perubahan pada satu tidak memengaruhi yang lain.
	 */
	@ManyToMany(targetEntity = HukumanPegawai.class, cascade = { CascadeType.MERGE })
	@OrderBy(value = "nama asc")
	@JoinTable(name = "pelanggaran_pegawai_dan_hukuman_has_hukuman", schema = "employ", joinColumns = @JoinColumn(name = "pelanggaran_dan_hukuman_pegawai"), inverseJoinColumns = @JoinColumn(name = "hukuman_pegawai"))
	public Set<HukumanPegawai> getHukumanPegawais() {
		return hukumanPegawais;
	}

	/** @param hukumanPegawais himpunan jenis hukuman aktual baru untuk catatan kejadian ini. */
	public void setHukumanPegawais(Set<HukumanPegawai> hukumanPegawais) {
		this.hukumanPegawais = hukumanPegawais;
	}

	/** Himpunan jenis pelanggaran AKTUAL yang tercatat pada kejadian ini (independen dari isi {@link #pelanggaranDanHukumanPegawai} saat ini). */
	private Set<PelanggaranPegawai> pelanggaranPegawais = new HashSet<PelanggaranPegawai>();

	/**
	 * @return {@link #pelanggaranPegawais} — himpunan jenis pelanggaran aktual milik catatan
	 *         kejadian ini, terurut menurut {@code nama}, lewat join table
	 *         {@code pelanggaran_pegawai_dan_hukuman_has_pelanggaran}. Sama seperti
	 *         {@link #getHukumanPegawais()}, ini adalah relasi many-to-many TERPISAH dari
	 *         {@link PelanggaranDanHukumanPegawai#getPelanggaranPegawais()} milik template.
	 */
	@ManyToMany(targetEntity = PelanggaranPegawai.class, cascade = { CascadeType.MERGE })
	@OrderBy(value = "nama asc")
	@JoinTable(name = "pelanggaran_pegawai_dan_hukuman_has_pelanggaran", schema = "employ", joinColumns = @JoinColumn(name = "pelanggaran_pegawai_dan_hukuman"), inverseJoinColumns = @JoinColumn(name = "pelanggaran_pegawai"))
	public Set<PelanggaranPegawai> getPelanggaranPegawais() {
		return pelanggaranPegawais;
	}

	/** @param pelanggaranPegawais himpunan jenis pelanggaran aktual baru untuk catatan kejadian ini. */
	public void setPelanggaranPegawais(Set<PelanggaranPegawai> pelanggaranPegawais) {
		this.pelanggaranPegawais = pelanggaranPegawais;
	}

	/** Konstruktor default (dibutuhkan Hibernate); tidak menginisialisasi field apa pun secara eksplisit selain default deklarasi field dan kedua himpunan kosong. */
	public PendataanPelanggaranPegawai() {
	}

	/**
	 * Konstruktor kenyamanan untuk membuat baris dengan {@link #id} dan {@link #nama} langsung
	 * terisi. Field lain (pegawai, template, waktu, keterangan, dsb.) TIDAK ikut diisi oleh
	 * konstruktor ini — perlu diperhatikan bahwa {@link #getNama()} akan MENIMPA nilai {@code nama}
	 * ini pada pemanggilan berikutnya begitu {@link #getPegawai()}/{@link #getPelanggaranDanHukumanPegawai()}/{@link #getWaktu()}
	 * ikut terisi.
	 *
	 * @param id   primary key yang akan di-set langsung (bukan menunggu generate database)
	 * @param nama nama/label awal catatan (akan ditimpa ulang oleh {@link #getNama()})
	 */
	public PendataanPelanggaranPegawai(long id, String nama) {
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

	/** @return {@link #pegawai} — pegawai yang dikenai catatan ini, dilewatkan {@link #check(Object)} (pola lazy-init standar {@code GeneralValueObject}). Kolom join {@code pegawai_id} wajib terisi ({@code nullable = false}). */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai_id", nullable = false)
	public Pegawai getPegawai() {
		pegawai = check(pegawai);
		return this.pegawai;
	}

	/** @param pegawai pegawai target baru. */
	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	/** @return {@link #keterangan} — keterangan/kronologi bebas catatan ini, boleh {@code null}. */
	@Column(name = "keterangan")
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan keterangan/kronologi bebas baru. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return label baris ini, SELALU dihitung ulang &amp; menimpa field {@link #nama} sebagai efek
	 *         samping setiap kali dipanggil: gabungan {@link #getPegawai()},
	 *         {@link #getPelanggaranDanHukumanPegawai()}, dan {@link #getWaktu()} (dipisah
	 *         {@code "_"}), memakai representasi {@code toString()} masing-masing objek. Getter ini
	 *         BUKAN getter murni — nilai yang di-set manual lewat {@link #setNama(String)} akan
	 *         tertimpa begitu ketiga sumbernya terisi.
	 */
	@Column(name = "nama", nullable = false)
	public String getNama() {
		nama = getPegawai() + "_" + getPelanggaranDanHukumanPegawai() + "_" + getWaktu();
		return this.nama;
	}

	/**
	 * @param nama nilai cache/awal lokal; TIDAK bertahan sebagai label akhir, karena
	 *              {@link #getNama()} akan menghitung ulang &amp; menimpa field ini dari
	 *              {@link #pegawai}/{@link #pelanggaranDanHukumanPegawai}/{@link #waktu} pada
	 *              pemanggilan berikutnya.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * @return status aktif/berlaku catatan ini. Bila {@link #disposisiSop} terisi dan TIDAK aktif
	 *         ({@code !disposisiSop.getAktif()}), atau disposisi akhirnya
	 *         ({@code disposisiSop.getDisposisiEnd()}) berada pada langkah alur SOP yang menandai
	 *         penolakan ({@code alurSop.getPenolakanAdaDiSini()}), maka status DIPAKSA menjadi
	 *         {@code false} — menimpa field {@link #aktif} sebagai efek samping. Selain kedua
	 *         kondisi tsb, nilai field apa adanya dikembalikan ({@code true} bila belum pernah
	 *         di-set/{@code null}).
	 */
	public Boolean getAktif() {

		disposisiSop = getDisposisiSop();
		if (disposisiSop != null && !disposisiSop.getAktif()) {
			aktif = false;
		}
		if (disposisiSop != null && disposisiSop.getDisposisiEnd() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop().getPenolakanAdaDiSini()) {
			aktif = false;
		}

		return aktif == null ? true : aktif;
	}

	/**
	 * @param aktif nilai cache lokal; bisa ditimpa oleh {@link #getAktif()} pada pemanggilan
	 *               berikutnya bila {@link #disposisiSop} menunjukkan kondisi tidak aktif/ditolak
	 *               (lihat catatan pada getter tsb).
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/** @return {@link #pelanggaranDanHukumanPegawai} — template aturan yang menjadi dasar catatan ini, dilewatkan {@link #check(Object)} (pola lazy-init standar {@code GeneralValueObject}). Kolom join wajib terisi ({@code nullable = false}). */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pelanggaran_dan_hukuman", nullable = false)
	public PelanggaranDanHukumanPegawai getPelanggaranDanHukumanPegawai() {
		pelanggaranDanHukumanPegawai = check(pelanggaranDanHukumanPegawai);
		return pelanggaranDanHukumanPegawai;
	}

	/** @param pelanggaranDanHukumanPegawai template aturan baru. */
	public void setPelanggaranDanHukumanPegawai(PelanggaranDanHukumanPegawai pelanggaranDanHukumanPegawai) {
		this.pelanggaranDanHukumanPegawai = pelanggaranDanHukumanPegawai;
	}

	/** @return {@link #waktu} — waktu kejadian/pencatatan; bila belum pernah di-set ({@code null}), dikembalikan waktu SEKARANG ({@link WaktuUtil#getDate()}) — BUKAN tanggal tetap yang sudah tersimpan, sehingga nilai ini bisa berbeda antar-pemanggilan sebelum baris disimpan. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktu() {
		return waktu == null ? WaktuUtil.getDate() : waktu;
	}

	/** @param waktu waktu kejadian/pencatatan baru. */
	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

	/** @return {@link #disposisiSop} — alur disposisi SOP yang menjadi sumber status persetujuan/berlaku catatan ini, dilewatkan {@link #check(Object)} (pola lazy-init standar {@code GeneralValueObject}). */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	/**
	 * Menolak meng-set {@link #disposisiSop} ke {@code null} atau ke entitas belum tersimpan
	 * (id {@code null}) — dipertahankan hingga diganti dengan disposisi lain yang valid. Pola ini
	 * identik dengan {@link ais.database.model.payroll.CutiDanIzin#setDisposisiSop(DisposisiSop)}.
	 *
	 * @param disposisiSop disposisi SOP baru; diabaikan bila {@code null} atau belum punya id
	 */
	public void setDisposisiSop(DisposisiSop disposisiSop) {if(disposisiSop==null||disposisiSop.getId()==null) {return;}
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null)) ? this.disposisiSop : disposisiSop;
	}

}
