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

import org.hibernate.envers.Audited;

import ais.database.model.rab.SatuanKerja;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;

/**
 * Entity Hibernate/JPA untuk tabel {@code public.penjelasan_bank_soal} — <b>penjelasan/rubrik
 * koreksi</b> untuk satu {@link BankSoal} (bank soal ujian): jenis koreksi ({@link
 * #getJenisKoreksi()}, mis. {@link BankSoal#PILIHAN_GANDA}) beserta cakupan siapa yang berhak
 * (Fakultas/Jurusan/Dosen untuk perguruan tinggi, atau Yayasan/Sekolah/Guru untuk modul
 * sekolah) dan {@link SatuanKerja} terkait — entity ini mendukung DUA populasi berbeda
 * (perguruan tinggi dan sekolah) sekaligus, mirip pola pada {@link PengeluaranMahasiswa}.
 *
 * @see BankSoalDetail
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "penjelasan_bank_soal")
public class PenjelasanBankSoal extends GeneralValueObject {

	/**
	 * Nilai penanda "jenis koreksi otomatis". Perhatikan salah eja pada nilai string literalnya
	 * ({@code "otomtais"}, bukan "otomatis") — dipertahankan apa adanya karena kemungkinan
	 * sudah dipakai sebagai nilai pembanding tersimpan; mengubah literalnya berisiko memutus
	 * kecocokan dengan data yang sudah ada tanpa migrasi.
	 */
	public static final String KOREKSI_OTOMATIS = "Hasil dikoreksi otomtais";
	/** Nilai penanda "jenis koreksi manual". */
	public static final String KOREKSI_MANUAL = "Hasil dikoreksi manual";

	/**
	 * ID versi serialisasi Java untuk kompatibilitas antar build (bukan kolom database).
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key baris {@code penjelasan_bank_soal}, kolom {@code id} (identity, auto-generate). */
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

	/** Nama/judul penjelasan bank soal ini. */
	private String nama;
	/** Keterangan bebas penjelasan ini. */
	private String keterangan;
	/** Flag aktif baris ini; default {@code true} bila belum diisi. */
	private Boolean aktif;

	/** Cakupan Fakultas (Institusi, perguruan tinggi). */
	private Fakultas fakultas;
	/** Cakupan Jurusan (Prodi, perguruan tinggi). */
	private Jurusan jurusan;
	/** Cakupan Yayasan (modul sekolah); diturunkan dari {@link #sekolah} bila terisi, lihat {@link #getYayasan()}. */
	private Yayasan yayasan;
	/** Cakupan Sekolah (modul sekolah). */
	private Sekolah sekolah;
	/** Dosen terkait (perguruan tinggi). */
	private Dosen dosen;
	/** Guru terkait (modul sekolah). */
	private Guru guru;
	/** Satuan kerja terkait; diturunkan dari {@link #sekolah}/{@link #fakultas} bila terisi, lihat {@link #getSatuanKerja()}. */
	private SatuanKerja satuanKerja;

	/** Jenis koreksi (lihat {@link #KOREKSI_OTOMATIS}/{@link #KOREKSI_MANUAL}); default {@link BankSoal#PILIHAN_GANDA} bila belum diisi. */
	private String jenisKoreksi;

	/**
	 * Konstruktor kosong yang dibutuhkan Hibernate untuk instansiasi entity via refleksi.
	 */
	public PenjelasanBankSoal() {
	}

	/**
	 * @return primary key baris {@code penjelasan_bank_soal}; {@code null} sebelum baris
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
	 * @return nama/judul penjelasan bank soal ini, di-{@code trim()}; {@code null} bila field
	 *         mentah {@code null} (meski kolomnya {@code nullable = false} di skema).
	 */
	@Column(name = "nama", nullable = false, columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * @param nama nama/judul baru.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * @return keterangan bebas penjelasan ini; boleh {@code null}.
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * @param keterangan keterangan baru untuk baris ini.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return status aktif baris ini; default {@code true} bila belum diisi.
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * @param aktif status aktif baru.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * @param fakultas cakupan fakultas baru; {@code null} untuk melepas tautan.
	 */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	/**
	 * @return cakupan Fakultas (Institusi) baris ini (proxy lazy diresolusi via {@code
	 *         check()}); boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "fakultas", nullable = true)
	public Fakultas getFakultas() {
		fakultas = check(fakultas);
		return fakultas;
	}

	/**
	 * @param jurusan cakupan jurusan baru; {@code null} untuk melepas tautan.
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * @return cakupan Jurusan (Prodi) baris ini (proxy lazy diresolusi via {@code check()});
	 *         boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	/**
	 * @param dosen dosen terkait baru; {@code null} untuk melepas tautan.
	 */
	public void setDosen(Dosen dosen) {
		this.dosen = dosen;
	}

	/**
	 * @return dosen terkait baris ini (proxy lazy diresolusi via {@code check()}); boleh
	 *         {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen", nullable = true)
	public Dosen getDosen() {
		dosen = check(dosen);
		return dosen;
	}

	/**
	 * @return guru terkait baris ini (proxy lazy diresolusi via {@code check()}); boleh
	 *         {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "guru", nullable = true)
	public Guru getGuru() {
		guru = check(guru);
		return guru;
	}

	/**
	 * @param guru guru terkait baru; {@code null} untuk melepas tautan.
	 */
	public void setGuru(Guru guru) {
		this.guru = guru;
	}

	/**
	 * Satuan kerja terkait baris ini.
	 *
	 * <p><b>Getter yang menulis balik (diturunkan dari relasi):</b> bila {@link
	 * #getSekolah()} tidak {@code null} dan sekolah itu punya satuan kerja, field {@link
	 * #satuanKerja} DITIMPA dengan satuan kerja sekolah itu; bila tidak, tapi {@link
	 * #getFakultas()} tidak {@code null} dan fakultas itu punya satuan kerja, ditimpa dengan
	 * satuan kerja fakultas. Nilai yang pernah diset manual lewat {@link
	 * #setSatuanKerja(SatuanKerja)} tertimpa selama salah satu relasi itu tersedia.</p>
	 *
	 * @return satuan kerja efektif (dari sekolah, dari fakultas, atau field lokal — sesuai
	 *         prioritas di atas); boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);

		if (getSekolah() != null && getSekolah().getSatuanKerja() != null) {
			satuanKerja = getSekolah().getSatuanKerja();
		} else if (getFakultas() != null && getFakultas().getSatuanKerja() != null) {
			satuanKerja = getFakultas().getSatuanKerja();
		}

		return satuanKerja;
	}

	/**
	 * @param satuanKerja satuan kerja baru untuk field lokal (bisa tetap ditimpa oleh satuan
	 *                    kerja sekolah/fakultas saat dibaca via {@link #getSatuanKerja()}).
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/**
	 * Cakupan Yayasan (modul sekolah).
	 *
	 * <p><b>Getter yang menulis balik (diturunkan dari relasi):</b> bila {@link #getSekolah()}
	 * tidak {@code null}, field {@link #yayasan} DITIMPA dengan {@code
	 * getSekolah().getYayasan()} — nilai yang pernah diset manual lewat {@link
	 * #setYayasan(Yayasan)} tertimpa selama {@link #sekolah} terisi.</p>
	 *
	 * @return yayasan efektif (dari sekolah bila tersedia, atau field lokal bila tidak); boleh
	 *         {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan", nullable = true)
	public Yayasan getYayasan() {
		yayasan = check(yayasan);
		if (getSekolah() != null) {
			yayasan = getSekolah().getYayasan();
		}
		return yayasan;
	}

	/**
	 * Menautkan yayasan cakupan baris ini. Menormalkan input: yayasan {@code null} ATAU yang
	 * ID-nya {@code null} (belum tersimpan/transient) sama-sama disimpan sebagai {@code null}.
	 *
	 * @param yayasan yayasan baru; entity tanpa ID diperlakukan sama seperti {@code null}. Bisa
	 *                tetap ditimpa oleh yayasan dari {@link #sekolah} saat dibaca via {@link
	 *                #getYayasan()}.
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan==null||yayasan.getId()==null?null:yayasan;
	}

	/**
	 * @return cakupan Sekolah (modul sekolah) baris ini (proxy lazy diresolusi via {@code
	 *         check()}); boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah", nullable = true)
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return sekolah;
	}

	/**
	 * Menautkan sekolah cakupan baris ini. Normalisasi input sama seperti {@link
	 * #setYayasan(Yayasan)}: sekolah {@code null} atau tanpa ID disimpan sebagai {@code null}.
	 *
	 * @param sekolah sekolah baru; entity tanpa ID diperlakukan sama seperti {@code null}.
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah==null||sekolah.getId()==null?null:sekolah;
	}

	/**
	 * @return jenis koreksi baris ini (lihat {@link #KOREKSI_OTOMATIS}/{@link
	 *         #KOREKSI_MANUAL}); default {@link BankSoal#PILIHAN_GANDA} bila kosong/{@code
	 *         null}.
	 */
	public String getJenisKoreksi() {
		return jenisKoreksi == null || jenisKoreksi.trim().isEmpty() ? BankSoal.PILIHAN_GANDA : jenisKoreksi;
	}

	/**
	 * @param jenisKoreksi jenis koreksi baru.
	 */
	public void setJenisKoreksi(String jenisKoreksi) {
		this.jenisKoreksi = jenisKoreksi;
	}
}
