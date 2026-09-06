package ais.database.model.kkn;

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
 * Entity <b>katalog master syarat pendaftaran KKN</b> pada tabel {@code public.persyaratan_kkn}.
 * Satu baris mewakili SATU butir syarat (mis. "Fotokopi KTM", "Minimal SKS 100", "Minimal IPK
 * 2.75", atau syarat bebas/"Syarat Lain") yang lalu diikat ke satu atau lebih gelaran {@link Kkn}
 * lewat {@link KknPunyaPersyaratan}, dan dijawab per mahasiswa di {@link MahasiswaKknPersyaratan}.
 * Katalog ini bersifat <b>global/dipakai bersama</b> antar gelaran KKN — mengubah satu baris di
 * sini memengaruhi seluruh gelaran yang mengaitkannya.
 *
 * <h3>Konstanta {@link #getTipeDataInputan()}</h3>
 * <p>Ketujuh konstanta {@code String} publik di kelas ini ({@link #TIDAK_ADA}, {@link #TEXT},
 * {@link #ANGKA}, {@link #TEXT_ANGKA}, {@link #TANGGAL}, {@link #PILIHAN_YA_TIDAK},
 * {@link #PILIHAN_CUSTOM}) adalah nilai-nilai valid yang disimpan pada field
 * {@link #tipeDataInputan}, menentukan jenis input yang harus diisi mahasiswa untuk memenuhi syarat
 * ini (dipetakan ke field {@code nilaiString}/{@code nilaiTanggal}/{@code nilaiNumber}/
 * {@code nilaiBoolean} pada {@link MahasiswaKknPersyaratan}).</p>
 *
 * <h3>Presisi bug default SKS/IPK pada "Syarat Lain" (dicatat, TIDAK ditambal di sini)</h3>
 * <p>Diverifikasi ulang dari sisi entity ini: {@link #getNilaiDataInputan()} mengembalikan string
 * kosong {@code ""} (bukan {@code null}) saat field {@link #nilaiDataInputan} belum pernah diisi —
 * ini adalah ambang batas mentah (mis. angka minimal SKS/IPK) yang dipakai kode perbandingan syarat
 * di lapisan lain (di luar 14 file paket {@code kkn}/{@code pkl}). Bila operator mengaktifkan syarat
 * "Syarat Lain" (memilih {@link #TEXT_ANGKA}/{@link #ANGKA} sebagai {@link #getTipeDataInputan()})
 * tanpa pernah mengisi angka ambang batas lewat {@link #setNilaiDataInputan(String)}, ambang batas
 * efektifnya adalah string kosong — bukan angka yang gagal-aman (fail-closed) menolak semua
 * pendaftar, melainkan nilai yang dapat ditafsirkan kode pemanggil sebagai "tidak ada batas" dan
 * meloloskan semua pendaftar. Bug identik ada di kembaran {@link ais.database.model.pkl.PersyaratanPkl};
 * ini KONFIRMASI ULANG dari sisi model, bukan temuan baru — sudah tercatat di memori proyek
 * sebelumnya dan sengaja TIDAK ditambal pada sesi dokumentasi ini.</p>
 *
 * <h3>Kembaran modul PKL</h3>
 * <p>Struktur kelas ini identik byte-demi-byte (selain nama tabel dan javadoc) dengan
 * {@link ais.database.model.pkl.PersyaratanPkl} — tidak ada satu pun divergensi field/method yang
 * ditemukan antara keduanya.</p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "persyaratan_kkn")



public class PersyaratanKkn extends GeneralValueObject {

	/** Nilai {@link #tipeDataInputan}: syarat ini tidak meminta input apa pun dari mahasiswa. */
	public static final String TIDAK_ADA = "Tidak ada data yang diinput";
	/** Nilai {@link #tipeDataInputan}: syarat ini meminta input berupa teks bebas. */
	public static final String TEXT = "Berupa teks";
	/** Nilai {@link #tipeDataInputan}: syarat ini meminta input berupa angka/numerik (mis. ambang SKS/IPK). */
	public static final String ANGKA = "Berupa numerik / angka";
	/** Nilai {@link #tipeDataInputan}: syarat ini meminta input berupa teks ATAU angka (paling longgar, dipakai untuk "Syarat Lain"). */
	public static final String TEXT_ANGKA = "Berupa teks / angka";
	/** Nilai {@link #tipeDataInputan}: syarat ini meminta input berupa tanggal. */
	public static final String TANGGAL = "Berupa tanggal";
	/** Nilai {@link #tipeDataInputan}: syarat ini meminta input berupa pilihan ya/tidak (boolean). */
	public static final String PILIHAN_YA_TIDAK = "Berupa pilihan ya/tidak";
	/** Nilai {@link #tipeDataInputan}: syarat ini meminta input berupa pilihan custom yang didefinisikan operator. */
	public static final String PILIHAN_CUSTOM = "Berupa pilihan custom";

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key baris syarat ini. */
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

	/** @return {@link #getNama()} langsung dari field mentah — representasi teks ringkas syarat ini. */
	public String toString() {
		return nama;
	}

	/** Nama syarat (mis. "Minimal SKS", "Fotokopi KTM"); wajib diisi (kolom {@code NOT NULL}). */
	private String nama;
	/** Deskripsi/penjelasan bebas untuk syarat ini; boleh {@code null}. */
	private String keterangan;
	/** Jenis input yang diminta ke mahasiswa; lihat konstanta {@link #TEXT} dkk. Default {@code String.class.getName()} bila belum diisi. */
	private String tipeDataInputan;
	/**
	 * Nilai/ambang batas mentah syarat ini (mis. angka minimal SKS/IPK sebagai teks). Default string
	 * kosong bila belum diisi — lihat catatan bug pada javadoc kelas.
	 */
	private String nilaiDataInputan;
	/** Menandai apakah mahasiswa wajib menyertakan berkas lampiran untuk syarat ini. Default {@code false}. */
	private Boolean harusMenyertakanLampiran;
	/** Label tampilan kustom untuk kolom input syarat ini di formulir pendaftaran; boleh {@code null}. */
	private String labelInputan;
	/** Filter jenis kelamin pendaftar yang berlaku untuk syarat ini (string bebas); default string kosong. */
	private String jenisKelamin;
	/** Menandai apakah syarat ini wajib diisi (berbeda dari {@link #harusMenyertakanLampiran}, ini soal keharusan mengisi nilai). Default {@code false}. */
	private Boolean harusDiisi;
	/** Menandai apakah syarat ini masih berlaku/ditampilkan. Default {@code true} bila belum diisi. */
	private Boolean aktif;

	/** Konstruktor kosong wajib bagi Hibernate (dipakai lewat refleksi saat memuat entity). */
	public PersyaratanKkn() {
	}

	/**
	 * @return primary key baris syarat ini, di-generate basis data ({@code IDENTITY}); {@code null}
	 *         sebelum baris pertama kali disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * @param id primary key baris syarat ini. Kolom dipetakan {@code insertable = false} sehingga
	 *           pengisian di sini tidak berpengaruh pada {@code INSERT}.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return nama syarat, di-trim; {@code null} bila field {@link #nama} belum pernah diisi. */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/** @param nama nama syarat (mis. "Minimal SKS 100"); disimpan apa adanya, trimming terjadi di {@link #getNama()}. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return deskripsi/penjelasan bebas syarat ini, apa adanya tanpa normalisasi. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan deskripsi/penjelasan bebas untuk syarat ini. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return jenis input yang diminta ke mahasiswa untuk memenuhi syarat ini — salah satu dari
	 *         konstanta {@link #TEXT}, {@link #ANGKA}, {@link #TEXT_ANGKA}, {@link #TANGGAL},
	 *         {@link #PILIHAN_YA_TIDAK}, {@link #PILIHAN_CUSTOM}, atau {@link #TIDAK_ADA}. Bila
	 *         field {@link #tipeDataInputan} belum pernah diisi, method ini <b>menuliskannya</b>
	 *         (efek samping) dengan default {@code String.class.getName()} — nilai literal ini
	 *         BUKAN salah satu dari ketujuh konstanta di atas, sehingga kode pemanggil yang
	 *         membandingkan hasil getter ini dengan konstanta-konstanta tersebut harus menangani
	 *         kemungkinan nilai default "asing" ini secara eksplisit.
	 */
	public String getTipeDataInputan() {
		if (tipeDataInputan == null) {
			tipeDataInputan = String.class.getName();
		}
		return tipeDataInputan;
	}

	/** @param tipeDataInputan salah satu konstanta {@link #TEXT} dkk yang menentukan jenis input syarat ini. */
	public void setTipeDataInputan(String tipeDataInputan) {
		this.tipeDataInputan = tipeDataInputan;
	}

	/**
	 * @return {@code true} bila mahasiswa wajib menyertakan berkas lampiran untuk memenuhi syarat
	 *         ini. Bila field {@link #harusMenyertakanLampiran} belum pernah diisi, method ini
	 *         menuliskannya (efek samping) dengan default {@code false} lalu mengembalikannya —
	 *         bukan sekadar membaca nilai secara pasif.
	 */
	public Boolean getHarusMenyertakanLampiran() {
		if (harusMenyertakanLampiran == null) {
			harusMenyertakanLampiran = false;
		}
		return harusMenyertakanLampiran;
	}

	/** @param harusMenyertakanLampiran {@code true} bila syarat ini mewajibkan lampiran berkas. */
	public void setHarusMenyertakanLampiran(Boolean harusMenyertakanLampiran) {
		this.harusMenyertakanLampiran = harusMenyertakanLampiran;
	}

	/** @return label tampilan kustom untuk kolom input syarat ini di formulir pendaftaran, atau {@code null} bila belum diisi (formulir memakai label default). */
	public String getLabelInputan() {
		return labelInputan;
	}

	/** @param labelInputan label tampilan kustom untuk kolom input syarat ini. */
	public void setLabelInputan(String labelInputan) {
		this.labelInputan = labelInputan;
	}

	/**
	 * @return nilai/ambang batas mentah syarat ini sebagai teks (mis. angka minimal SKS/IPK). Bila
	 *         field {@link #nilaiDataInputan} belum pernah diisi, method ini menuliskannya (efek
	 *         samping) dengan default string kosong {@code ""} lalu mengembalikannya. <b>Lihat
	 *         catatan bug pada javadoc kelas</b>: default string kosong ini — bukan nilai yang
	 *         gagal-aman menolak pendaftar — adalah akar presisi bug "Syarat Lain" yang sudah
	 *         tercatat di memori proyek: bila operator mengaktifkan tipe input numerik/teks-angka
	 *         tanpa pernah mengisi ambang batas lewat {@link #setNilaiDataInputan(String)}, kode
	 *         pembanding di lapisan lain menerima string kosong sebagai ambang batas efektif.
	 */
	public String getNilaiDataInputan() {
		if (nilaiDataInputan == null) {
			nilaiDataInputan = "";
		}
		return nilaiDataInputan;
	}

	/** @param nilaiDataInputan nilai/ambang batas mentah syarat ini sebagai teks (mis. "100" untuk minimal SKS). */
	public void setNilaiDataInputan(String nilaiDataInputan) {
		this.nilaiDataInputan = nilaiDataInputan;
	}

	/**
	 * @return filter jenis kelamin pendaftar yang berlaku untuk syarat ini (string bebas, bukan
	 *         enum). Bila field {@link #jenisKelamin} belum pernah diisi, method ini menuliskannya
	 *         (efek samping) dengan default string kosong {@code ""} lalu mengembalikannya.
	 */
	public String getJenisKelamin() {
		if (jenisKelamin == null) {
			jenisKelamin = "";
		}
		return jenisKelamin;
	}

	/** @param jenisKelamin filter jenis kelamin pendaftar untuk syarat ini. */
	public void setJenisKelamin(String jenisKelamin) {
		this.jenisKelamin = jenisKelamin;
	}

	/**
	 * @return {@code true} bila syarat ini wajib diisi mahasiswa (berbeda dari
	 *         {@link #getHarusMenyertakanLampiran()}: field ini soal keharusan mengisi NILAI,
	 *         bukan soal keharusan melampirkan BERKAS). Bila field {@link #harusDiisi} belum pernah
	 *         diisi, method ini menuliskannya (efek samping) dengan default {@code false}.
	 */
	public Boolean getHarusDiisi() {
		if (harusDiisi == null) {
			harusDiisi = false;
		}
		return harusDiisi;
	}

	/** @param harusDiisi {@code true} bila syarat ini wajib diisi nilainya oleh mahasiswa. */
	public void setHarusDiisi(Boolean harusDiisi) {
		this.harusDiisi = harusDiisi;
	}

	/** @return {@code true} bila syarat ini masih berlaku/ditampilkan; default {@code true} bila field {@link #aktif} belum pernah diisi (fail-open — syarat baru dianggap aktif secara default). */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/** @param aktif {@code true} agar syarat ini tetap berlaku/ditampilkan, {@code false} untuk menonaktifkannya tanpa menghapus baris. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}
}
