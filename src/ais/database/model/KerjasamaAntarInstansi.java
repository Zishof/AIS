package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

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

import ais.common.ConstantValues;

/**
 * Entity Hibernate/JPA untuk tabel {@code public.kerjasama_antar_instansi} — catatan
 * <b>kerjasama institusional</b> (MoU/MoA) antara perguruan tinggi dengan instansi lain: nama
 * mitra, jenis kerjasama, negara mitra, cakupan Fakultas/Jurusan, rentang tanggal berlaku,
 * manfaat, bukti dokumen, tingkat (lokal/nasional/internasional), dan kategori kegiatan
 * (pendidikan/penelitian/pengabdian masyarakat) — umumnya dipakai untuk pelaporan akreditasi.
 *
 * @see #TINGKAT
 * @see #JENIS
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "kerjasama_antar_instansi")

public class KerjasamaAntarInstansi extends GeneralValueObject {

	/** Daftar pilihan tingkat kerjasama yang valid untuk {@link #getTingkat()}: Wilayah/Lokal, Nasional, Internasional. */
	public static List<String> TINGKAT = new ArrayList<String>();
	/** Daftar pilihan jenis kerjasama yang valid untuk {@link #getJenis()}: Pendidikan, Penelitian, Pengabdian Kepada Masyarakat. */
	public static List<String> JENIS = new ArrayList<String>();

	static {
		TINGKAT.add("Wilayah / Lokal");
		TINGKAT.add("Nasional");
		TINGKAT.add("Internasional");

		JENIS.add("Kerjasama Pendidikan");
		JENIS.add("Kerjasama Penelitian");
		JENIS.add("Kerjasama Pengabdian Kepada Masyarakat");
	}

	/**
	 * ID versi serialisasi Java untuk kompatibilitas antar build (bukan kolom database).
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key baris {@code kerjasama_antar_instansi}, kolom {@code id} (identity, auto-generate). */
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
	 * @return string ringkas identitas baris ini
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Nama mitra/judul kerjasama. */
	private String nama;
	/** Jenis kerjasama terstruktur (relasi ke master {@link JenisKerjasama}, berbeda dari kategori string {@link #jenis}). */
	private JenisKerjasama jenisKerjasama;
	/** Negara mitra kerjasama; default {@link ConstantValues#INDONESIA} bila belum diisi, lihat {@link #getNegara()}. */
	private Negara negara;
	/** Cakupan Fakultas (Institusi) kerjasama ini. */
	private Fakultas fakultas;
	/** Cakupan Jurusan (Prodi) kerjasama ini. */
	private Jurusan jurusan;
	/** Tanggal mulai berlaku kerjasama. */
	private Date mulai;
	/** Tanggal berakhirnya kerjasama. */
	private Date sampai;
	/** Deskripsi manfaat kerjasama ini. */
	private String manfaat;
	/** Tautan/deskripsi bukti dokumen kerjasama (mis. path/nama file MoU). */
	private String bukti;
	/** Keterangan bebas untuk kerjasama ini. */
	private String keterangan;
	/** Tahun kerjasama; diturunkan otomatis dari {@link #mulai} bila terisi, lihat {@link #getTahun()}. */
	private Integer tahun;
	/** Tingkat kerjasama (lihat {@link #TINGKAT} untuk pilihan valid); default "Nasional" bila belum diisi. */
	private String tingkat;
	/** Kategori kerjasama sebagai string bebas (lihat {@link #JENIS} untuk pilihan valid; berbeda dari relasi terstruktur {@link #jenisKerjasama}); default "Kerjasama Pendidikan" bila belum diisi. */
	private String jenis;

	/**
	 * Konstruktor kosong yang dibutuhkan Hibernate untuk instansiasi entity via refleksi.
	 */
	public KerjasamaAntarInstansi() {
	}

	/**
	 * @return primary key baris {@code kerjasama_antar_instansi}; {@code null} sebelum baris
	 *         di-{@code INSERT}.
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
	 * @return nama mitra/judul kerjasama, di-{@code trim()}; {@code null} bila field mentah
	 *         {@code null} (meski kolomnya {@code nullable = false} di skema).
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * @param nama nama mitra/judul kerjasama baru.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * @return keterangan bebas kerjasama ini; boleh {@code null}.
	 */
	@Column(name = "keterangan", columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * @param keterangan keterangan baru untuk kerjasama ini.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return jenis kerjasama terstruktur (proxy lazy diresolusi via {@code check()}); boleh
	 *         {@code null}. Bandingkan dengan kategori string bebas {@link #getJenis()}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_kerjasama", nullable = true)
	public JenisKerjasama getJenisKerjasama() {
		jenisKerjasama = check(jenisKerjasama);
		return jenisKerjasama;
	}

	/**
	 * @param jenisKerjasama jenis kerjasama terstruktur baru; {@code null} untuk melepas tautan.
	 */
	public void setJenisKerjasama(JenisKerjasama jenisKerjasama) {
		this.jenisKerjasama = jenisKerjasama;
	}

	/**
	 * @return tanggal mulai berlaku kerjasama; boleh {@code null}.
	 */
	@Temporal(TemporalType.DATE)
	public Date getMulai() {
		return mulai;
	}

	/**
	 * @param mulai tanggal mulai berlaku baru.
	 */
	public void setMulai(Date mulai) {
		this.mulai = mulai;
	}

	/**
	 * @return tanggal berakhirnya kerjasama; boleh {@code null}.
	 */
	@Temporal(TemporalType.DATE)
	public Date getSampai() {
		return sampai;
	}

	/**
	 * @param sampai tanggal berakhir baru.
	 */
	public void setSampai(Date sampai) {
		this.sampai = sampai;
	}

	/**
	 * @return deskripsi manfaat kerjasama ini; boleh {@code null}.
	 */
	@Column(name = "manfaat", columnDefinition = "text")
	public String getManfaat() {
		return manfaat;
	}

	/**
	 * @param manfaat deskripsi manfaat baru.
	 */
	public void setManfaat(String manfaat) {
		this.manfaat = manfaat;
	}

	/**
	 * @return negara mitra kerjasama (proxy lazy diresolusi via {@code check()}); default
	 *         {@link ConstantValues#INDONESIA} bila belum diisi (bukan {@code null}).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "negara", nullable = true)
	public Negara getNegara() {
		negara = check(negara);
		return negara == null ? ConstantValues.INDONESIA : negara;
	}

	/**
	 * @param negara negara mitra baru; {@code null} untuk memakai default {@link
	 *               ConstantValues#INDONESIA}.
	 */
	public void setNegara(Negara negara) {
		this.negara = negara;
	}

	/**
	 * @return cakupan Fakultas (Institusi) kerjasama ini (proxy lazy diresolusi via {@code
	 *         check()}); boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "fakultas", nullable = true)
	public Fakultas getFakultas() {
		fakultas = check(fakultas);
		return fakultas;
	}

	/**
	 * @param fakultas cakupan fakultas baru; {@code null} untuk melepas tautan.
	 */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	/**
	 * @return cakupan Jurusan (Prodi) kerjasama ini (proxy lazy diresolusi via {@code check()});
	 *         boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	/**
	 * @param jurusan cakupan jurusan baru; {@code null} untuk melepas tautan.
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * Tahun kerjasama.
	 *
	 * <p><b>Getter yang menulis balik (diturunkan dari tanggal mulai):</b> bila {@link
	 * #getMulai()} tidak {@code null}, field {@link #tahun} DITIMPA dengan tahun kalender dari
	 * tanggal mulai itu setiap kali getter ini dipanggil — nilai yang pernah diset manual lewat
	 * {@link #setTahun(Integer)} tertimpa selama {@link #mulai} terisi. Bila {@link #mulai}
	 * {@code null}, field {@link #tahun} dikembalikan apa adanya (termasuk {@code null} bila
	 * belum pernah diisi).</p>
	 *
	 * @return tahun kerjasama efektif; boleh {@code null} bila {@link #mulai} kosong dan
	 *         belum pernah diset manual.
	 */
	public Integer getTahun() {
		if (getMulai() != null) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(getMulai());
			tahun = calendar.get(Calendar.YEAR);
		}
		return tahun;
	}

	/**
	 * @param tahun tahun baru untuk field lokal (bisa tetap ditimpa oleh tahun {@link #mulai}
	 *              saat dibaca via {@link #getTahun()} — lihat javadoc getter).
	 */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/**
	 * @return tingkat kerjasama (lihat {@link #TINGKAT} untuk pilihan valid); default
	 *         {@code "Nasional"} bila belum diisi.
	 */
	public String getTingkat() {
		return tingkat == null ? "Nasional" : tingkat;
	}

	/**
	 * @param tingkat tingkat kerjasama baru.
	 */
	public void setTingkat(String tingkat) {
		this.tingkat = tingkat;
	}

	/**
	 * @return tautan/deskripsi bukti dokumen kerjasama; boleh {@code null}.
	 */
	@Column(name = "bukti", columnDefinition = "text")
	public String getBukti() {
		return bukti;
	}

	/**
	 * @param bukti bukti dokumen baru.
	 */
	public void setBukti(String bukti) {
		this.bukti = bukti;
	}

	/**
	 * @return kategori kerjasama sebagai string bebas (lihat {@link #JENIS} untuk pilihan
	 *         valid); default {@code "Kerjasama Pendidikan"} bila belum diisi.
	 */
	public String getJenis() {
		return jenis == null ? "Kerjasama Pendidikan" : jenis;
	}

	/**
	 * @param jenis kategori kerjasama baru.
	 */
	public void setJenis(String jenis) {
		this.jenis = jenis;
	}

}
