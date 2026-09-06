package ais.database.model.pkl;

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




import org.hibernate.envers.Audited;



import ais.database.model.GeneralValueObject;
import ais.database.model.Mahasiswa;
import ais.database.model.Pkl;

/**
 * Entity <b>pendaftaran mahasiswa</b> ke satu program {@link Pkl} pada tabel
 * {@code public.mahasiswa_daftar_pkl}. Satu baris = satu mahasiswa mendaftar ke satu program PKL,
 * dengan status seleksi ({@link #getTerima()}: {@link #BELUM_DIPROSES}/{@link #DITERIMA}/
 * {@link #DITOLAK}), penanda pemenuhan syarat ({@link #getMemenuhiSyarat()}), dan skor gabungan
 * hasil penilaian syarat ({@link #getTotalSkor()}). Jawaban per butir syarat pendaftaran (bukan
 * hasil akhirnya) disimpan terpisah di {@link MahasiswaPklPersyaratan}, terhubung lewat
 * {@code mahasiswa}+{@code pkl} yang sama, bukan lewat foreign key langsung ke baris pendaftaran
 * ini.
 *
 * <h3>Alur pemakaian</h3>
 * <p>Mahasiswa mendaftar (membuat baris ini dengan {@link #getTerima()} default
 * {@link #BELUM_DIPROSES}) &rarr; panitia menyeleksi berkas syarat lewat
 * {@code ais.action.master.pkl.SeleksiPenerimaPklAction}, mengisi {@link #setMemenuhiSyarat}
 * dan {@link #setTotalSkor} &rarr; panitia mengubah {@link #setTerima} menjadi
 * {@link #DITERIMA}/{@link #DITOLAK} &rarr; mahasiswa yang diterima dipecah ke
 * {@link KelompokPkl} lewat entity terpisah {@code MahasiswaDapatKelompokPkl} (di luar paket ini).
 * Field {@link #getNo_SKTM()}/{@link #getPejabatSktm()} menampung data Surat Keterangan Tidak
 * Mampu bagi pendaftar yang mengajukan keringanan biaya.</p>
 *
 * <h3>Kembaran modul KKN</h3>
 * <p>Struktur kelas ini identik dengan {@link ais.database.model.kkn.MahasiswaDaftarKkn} —
 * satu-satunya beda tekstual adalah field relasi {@code pkl}/{@code kkn} dan bahwa kelas ini
 * menuliskan literal {@code 0} langsung pada default {@link #getTerima()} alih-alih memakai
 * konstanta {@link #BELUM_DIPROSES} secara eksplisit (perilaku run-time identik karena
 * {@code BELUM_DIPROSES == 0}).</p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "mahasiswa_daftar_pkl")



public class MahasiswaDaftarPkl extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key baris pendaftaran ini. */
	private Long id;
	/** Nama/username pengubah terakhir; diisi lewat {@link #setOleh(String)} oleh lapisan audit. */
	private String oleh;
	/** Id pengguna pengubah terakhir; diisi lewat {@link #setOlehId(String)} oleh lapisan audit. */
	private String olehId;

	/**
	 * @return id pengguna (bukan nama tampilan) yang terakhir mengubah baris ini, atau {@code null}
	 *         bila belum pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan id pengguna pengubah terakhir. Nilai {@code null} atau string kosong/blank
	 * diabaikan diam-diam (early return) — nilai lama tetap dipertahankan.
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null}/blank
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama tampilan pengubah terakhir. Nilai {@code null} atau blank diabaikan diam-diam.
	 *
	 * @param oleh nama pengubah; diabaikan bila {@code null}/blank
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * @return nama tampilan pengguna yang terakhir mengubah baris ini, atau {@code null} bila belum
	 *         pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil otomatis oleh provider persistence tepat sebelum
	 * {@code UPDATE} dikirim ke basis data, memperbarui {@link #tanggal_dirubah} lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * @param tanggal_dirubah stempel waktu perubahan terakhir; biasanya diset otomatis oleh
	 *                        {@link #onUpdate()}.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return stempel waktu perubahan terakhir baris ini; diperbarui otomatis oleh
	 *         {@link #onUpdate()} setiap kali baris diperbarui di basis data.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @return {@link #getNama()} langsung dari field mentah — representasi teks ringkas pendaftaran ini. */
	public String toString() {
		return nama;
	}

	/** Nama pendaftaran (biasanya nama mahasiswa); wajib diisi (kolom {@code NOT NULL}). */
	private String nama;
	/** Catatan/keterangan bebas untuk pendaftaran ini; boleh {@code null}. */
	private String keterangan;
	/** Program PKL yang didaftar. Wajib diisi (kolom {@code NOT NULL}). */
	private Pkl pkl;
	/** Mahasiswa pendaftar. Wajib diisi (kolom {@code NOT NULL}). */
	private Mahasiswa mahasiswa;
	/** Status seleksi pendaftaran; lihat konstanta {@link #BELUM_DIPROSES}/{@link #DITERIMA}/{@link #DITOLAK}. Default {@link #BELUM_DIPROSES}. */
	private Integer terima;
	/** Tanggal &amp; waktu mahasiswa mendaftar; boleh {@code null} bila belum diset oleh kode pemanggil. */
	private Date tanggalDaftar;
	/** Nilai {@link #terima}: pendaftaran belum diproses/diseleksi panitia. */
	public static final Integer BELUM_DIPROSES = 0;
	/** Nilai {@link #terima}: pendaftaran diterima/lolos seleksi. */
	public static final Integer DITERIMA = 1;
	/** Nilai {@link #terima}: pendaftaran ditolak. */
	public static final Integer DITOLAK = 2;
	/** Nomor Surat Keterangan Tidak Mampu (SKTM), bila pendaftar mengajukan keringanan biaya; boleh {@code null}. */
	private String no_SKTM;
	/** Nama pejabat penandatangan SKTM terkait; boleh {@code null}. */
	private String pejabatSktm;
	/** Penanda hasil evaluasi seluruh syarat pendaftaran (bukan status seleksi akhir). Default {@code false}. */
	private Boolean memenuhiSyarat;
	/** Skor gabungan hasil penilaian seluruh syarat pendaftaran. Default {@code 0}. */
	private Integer totalSkor;

	/**
	 * @return program {@link Pkl} yang didaftar mahasiswa ini. Referensi dicek lewat
	 *         {@code check(pkl)} sebelum dikembalikan (proxy Hibernate basi diganti entity segar
	 *         bila perlu).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pkl", nullable = false)
	public Pkl getPkl() {
		pkl = check(pkl);
		return pkl;
	}

	/** @param pkl program PKL yang didaftar. */
	public void setPkl(Pkl pkl) {
		this.pkl = pkl;
	}

	/** Konstruktor kosong wajib bagi Hibernate (dipakai lewat refleksi saat memuat entity). */
	public MahasiswaDaftarPkl() {
	}

	/**
	 * @return primary key baris pendaftaran ini, di-generate basis data ({@code IDENTITY});
	 *         {@code null} sebelum baris pertama kali disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * @param id primary key baris pendaftaran ini. Kolom dipetakan {@code insertable = false}
	 *           sehingga pengisian di sini tidak berpengaruh pada {@code INSERT}.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return nama pendaftaran, di-trim; {@code null} bila field {@link #nama} belum pernah diisi. */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/** @param nama nama pendaftaran (biasanya nama mahasiswa); disimpan apa adanya, trimming terjadi di {@link #getNama()}. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return catatan/keterangan bebas pendaftaran ini, apa adanya tanpa normalisasi. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan catatan/keterangan bebas untuk pendaftaran ini. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return mahasiswa pendaftar. Referensi dicek lewat {@code check(mahasiswa)} sebelum
	 *         dikembalikan, sama seperti {@link #getPkl()}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa", nullable = false)
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);
		return mahasiswa;
	}

	/** @param mahasiswa mahasiswa pendaftar. */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * @return status seleksi pendaftaran — salah satu dari {@link #BELUM_DIPROSES},
	 *         {@link #DITERIMA}, {@link #DITOLAK}. Bila field {@link #terima} belum pernah diisi,
	 *         method ini menuliskannya (efek samping) dengan default literal {@code 0} (numerik,
	 *         setara nilainya dengan {@link #BELUM_DIPROSES} tapi ditulis sebagai angka mentah,
	 *         bukan referensi konstanta — beda gaya penulisan dari kembaran
	 *         {@code MahasiswaDaftarKkn.getTerima()} yang memakai {@code BELUM_DIPROSES} langsung)
	 *         lalu mengembalikannya.
	 */
	@Column(name = "terima")
	public Integer getTerima() {
		if (terima == null) {
			terima = 0;
		}
		return terima;
	}

	/** @param terima status seleksi pendaftaran; gunakan salah satu konstanta {@link #BELUM_DIPROSES}/{@link #DITERIMA}/{@link #DITOLAK}. */
	public void setTerima(Integer terima) {
		this.terima = terima;
	}

	/** @return tanggal &amp; waktu mahasiswa mendaftar, atau {@code null} bila belum diset. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_daftar")
	public Date getTanggalDaftar() {
		return tanggalDaftar;
	}

	/** @param tanggalDaftar tanggal &amp; waktu mahasiswa mendaftar. */
	public void setTanggalDaftar(Date tanggalDaftar) {
		this.tanggalDaftar = tanggalDaftar;
	}

	/** @return nomor Surat Keterangan Tidak Mampu (SKTM) pendaftar, atau {@code null} bila tidak mengajukan/tidak diisi. */
	@Column(name = "no_sktm")
	public String getNo_SKTM() {
		return no_SKTM;
	}

	/** @param no_SKTM nomor Surat Keterangan Tidak Mampu (SKTM) pendaftar. */
	public void setNo_SKTM(String no_SKTM) {
		this.no_SKTM = no_SKTM;
	}

	/** @return nama pejabat penandatangan SKTM terkait, atau {@code null} bila tidak diisi. */
	@Column(name = "pejabat_sktm")
	public String getPejabatSktm() {
		return pejabatSktm;
	}

	/** @param pejabatSktm nama pejabat penandatangan SKTM terkait. */
	public void setPejabatSktm(String pejabatSktm) {
		this.pejabatSktm = pejabatSktm;
	}

	/**
	 * @return penanda hasil evaluasi keseluruhan syarat pendaftaran (bukan status seleksi akhir
	 *         {@link #getTerima()} itu sendiri — ini indikator pendukung yang dipakai panitia saat
	 *         memutuskan status seleksi). Bila field {@link #memenuhiSyarat} belum pernah diisi,
	 *         method ini menuliskannya (efek samping) dengan default {@code false} — pendaftar baru
	 *         dianggap BELUM memenuhi syarat sampai dievaluasi eksplisit (fail-closed).
	 */
	public Boolean getMemenuhiSyarat() {
		if (memenuhiSyarat == null) {
			memenuhiSyarat = false;
		}
		return memenuhiSyarat;
	}

	/** @param memenuhiSyarat {@code true} bila pendaftar dinilai memenuhi keseluruhan syarat pendaftaran. */
	public void setMemenuhiSyarat(Boolean memenuhiSyarat) {
		this.memenuhiSyarat = memenuhiSyarat;
	}

	/**
	 * @return skor gabungan hasil penilaian syarat pendaftaran mahasiswa ini, dipakai panitia untuk
	 *         mengurutkan/menyeleksi pendaftar. Bila field {@link #totalSkor} belum pernah diisi,
	 *         method ini menuliskannya (efek samping) dengan default {@code 0}.
	 */
	public Integer getTotalSkor() {
		if (totalSkor == null) {
			totalSkor = 0;
		}
		return totalSkor;
	}

	/** @param totalSkor skor gabungan hasil penilaian syarat pendaftaran. */
	public void setTotalSkor(Integer totalSkor) {
		this.totalSkor = totalSkor;
	}
}
