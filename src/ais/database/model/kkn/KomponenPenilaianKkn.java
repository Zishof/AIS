package ais.database.model.kkn;

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

/**
 * Entity <b>katalog master komponen penilaian akhir KKN</b> pada tabel
 * {@code public.komponen_penilaian_kkn}. Satu baris mewakili SATU komponen nilai (mis. "Nilai
 * Pembimbing", "Nilai Laporan Akhir", "Nilai Kedisiplinan") yang diikat ke satu atau lebih gelaran
 * {@link Kkn} lewat {@link KknPunyaKomponenPenilaianKkn}. Katalog ini bersifat
 * <b>global/dipakai bersama</b> antar gelaran KKN — mengubah satu baris di sini (mis. bobotnya)
 * memengaruhi seluruh gelaran yang mengaitkannya.
 *
 * <h3>Struktur hierarkis via {@link #getParent()}</h3>
 * <p>Komponen bisa disusun berjenjang: sebuah komponen dapat menunjuk komponen lain sebagai
 * {@link #parent}, memungkinkan pengelompokan (mis. komponen induk "Nilai Akhir" membawahi
 * beberapa sub-komponen dengan bobot masing-masing). Field ini {@code nullable = true} — komponen
 * tingkat teratas tidak memiliki parent.</p>
 *
 * <h3>Penanda dosen penilai {@link #getDosen1()}..{@link #getDosen5()}</h3>
 * <p>Lima flag boolean ini menandai <b>dosen pembimbing keberapa</b> (dari maksimal 10 dosen
 * pembimbing yang bisa diisi pada {@link KelompokKkn#getDosen_pembimbing1()} s.d.
 * {@code getDosen_pembimbing10()}) yang berwenang mengisi nilai untuk komponen ini — meski hanya
 * ada 5 flag sementara {@code KelompokKkn} punya slot untuk 10 dosen pembimbing, sehingga hanya
 * dosen pembimbing 1 s.d. 5 yang bisa diberi wewenang penilaian granular per komponen; dosen
 * pembimbing 6-10 tidak punya representasi di sini. Seluruhnya default {@code true} (fail-open —
 * setiap komponen baru dianggap bisa dinilai oleh dosen mana pun secara default sampai dibatasi
 * eksplisit).</p>
 *
 * <h3>Kembaran modul PKL</h3>
 * <p>Struktur kelas ini identik byte-demi-byte (selain penggantian nama Kkn&rarr;Pkl dan urutan
 * deklarasi field {@code parent}/{@code bobot} yang tertukar secara tekstual — tidak berpengaruh
 * pada perilaku) dengan {@link ais.database.model.pkl.KomponenPenilaianPkl}.</p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "komponen_penilaian_kkn")



public class KomponenPenilaianKkn extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key baris komponen ini. */
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

	/** @return {@code id + "-" + nama} — representasi teks ringkas komponen ini, mis. "3-Nilai Pembimbing". */
	public String toString() {
		return id + "-" + nama;
	}

	/** Nama komponen penilaian (mis. "Nilai Pembimbing"); wajib diisi (kolom {@code NOT NULL}). */
	private String nama;
	/** Nomor urut tampilan komponen ini relatif terhadap komponen lain. Default {@code 1} bila belum diisi. */
	private Integer nomorUrut;
	/** Menandai apakah komponen ini masih berlaku/ditampilkan. Default {@code true} bila belum diisi. */
	private Boolean aktif;
	/** Deskripsi/penjelasan bebas untuk komponen ini; boleh {@code null}. */
	private String keterangan;
	/** Komponen induk dalam struktur hierarkis (lihat javadoc kelas); {@code null} bila komponen ini tingkat teratas. */
	private KomponenPenilaianKkn parent;
	/** Bobot komponen ini dalam perhitungan nilai akhir. Default {@code 1.0} bila belum diisi. */
	private Double bobot;

	/** Penanda apakah dosen pembimbing ke-1 ({@link KelompokKkn#getDosen_pembimbing1()}) berwenang menilai komponen ini. Default {@code true}. */
	private Boolean dosen1;
	/** Penanda apakah dosen pembimbing ke-2 berwenang menilai komponen ini. Default {@code true}. */
	private Boolean dosen2;
	/** Penanda apakah dosen pembimbing ke-3 berwenang menilai komponen ini. Default {@code true}. */
	private Boolean dosen3;
	/** Penanda apakah dosen pembimbing ke-4 berwenang menilai komponen ini. Default {@code true}. */
	private Boolean dosen4;
	/** Penanda apakah dosen pembimbing ke-5 berwenang menilai komponen ini. Default {@code true}. */
	private Boolean dosen5;

	/** Konstruktor kosong wajib bagi Hibernate (dipakai lewat refleksi saat memuat entity). */
	public KomponenPenilaianKkn() {
	}

	/** @param nomorUrut nomor urut awal komponen ini, langsung diisi ke field {@link #nomorUrut} tanpa melalui setter/validasi. */
	public KomponenPenilaianKkn(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * @return primary key baris komponen ini, di-generate basis data ({@code IDENTITY});
	 *         {@code null} sebelum baris pertama kali disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * @param id primary key baris komponen ini. Kolom dipetakan {@code insertable = false}
	 *           sehingga pengisian di sini tidak berpengaruh pada {@code INSERT}.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return nama komponen, di-trim; {@code null} bila field {@link #nama} belum pernah diisi. */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/** @param nama nama komponen penilaian (mis. "Nilai Pembimbing"); disimpan apa adanya, trimming terjadi di {@link #getNama()}. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return nomor urut tampilan komponen ini. Bila field {@link #nomorUrut} belum pernah diisi, method ini mengembalikan default {@code 1} TANPA menuliskannya kembali ke field (berbeda dari pola getter lain di kelas ini yang menulis default ke field). */
	public Integer getNomorUrut() {
		return nomorUrut == null ? 1 : nomorUrut;
	}

	/** @param nomorUrut nomor urut tampilan komponen ini relatif terhadap komponen lain. */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/** @return deskripsi/penjelasan bebas komponen ini, apa adanya tanpa normalisasi. Dipetakan {@code columnDefinition = "text"} sehingga tidak dibatasi panjang varchar. */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan deskripsi/penjelasan bebas untuk komponen ini. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** @return {@code true} bila komponen ini masih berlaku/ditampilkan; default {@code true} bila field {@link #aktif} belum pernah diisi (fail-open). */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/** @param aktif {@code true} agar komponen ini tetap berlaku/ditampilkan, {@code false} untuk menonaktifkannya tanpa menghapus baris. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * @return komponen induk dalam struktur hierarkis (lihat javadoc kelas), atau {@code null} bila
	 *         komponen ini tingkat teratas. Referensi dicek lewat {@code check(parent)} sebelum
	 *         dikembalikan (proxy Hibernate basi diganti entity segar bila perlu).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "parent", nullable = true)
	public KomponenPenilaianKkn getParent() {
		parent = check(parent);
		return parent;
	}

	/** @param parent komponen induk dalam struktur hierarkis; {@code null} untuk menjadikannya komponen tingkat teratas. */
	public void setParent(KomponenPenilaianKkn parent) {
		this.parent = parent;
	}

	/** @return bobot komponen ini dalam perhitungan nilai akhir; default {@code 1.0} bila field {@link #bobot} belum pernah diisi. */
	public Double getBobot() {
		return bobot == null ? 1.0 : bobot;
	}

	/** @param bobot bobot komponen ini dalam perhitungan nilai akhir. */
	public void setBobot(Double bobot) {
		this.bobot = bobot;
	}

	/** @return {@code true} bila dosen pembimbing ke-1 berwenang menilai komponen ini; default {@code true} bila belum diisi (fail-open). */
	public Boolean getDosen1() {
		return dosen1 == null ? true : dosen1;
	}

	/** @param dosen1 {@code true} agar dosen pembimbing ke-1 berwenang menilai komponen ini. */
	public void setDosen1(Boolean dosen1) {
		this.dosen1 = dosen1;
	}

	/** @return {@code true} bila dosen pembimbing ke-2 berwenang menilai komponen ini; default {@code true} bila belum diisi (fail-open). */
	public Boolean getDosen2() {
		return dosen2 == null ? true : dosen2;
	}

	/** @param dosen2 {@code true} agar dosen pembimbing ke-2 berwenang menilai komponen ini. */
	public void setDosen2(Boolean dosen2) {
		this.dosen2 = dosen2;
	}

	/** @return {@code true} bila dosen pembimbing ke-3 berwenang menilai komponen ini; default {@code true} bila belum diisi (fail-open). */
	public Boolean getDosen3() {
		return dosen3 == null ? true : dosen3;
	}

	/** @param dosen3 {@code true} agar dosen pembimbing ke-3 berwenang menilai komponen ini. */
	public void setDosen3(Boolean dosen3) {
		this.dosen3 = dosen3;
	}

	/** @return {@code true} bila dosen pembimbing ke-4 berwenang menilai komponen ini; default {@code true} bila belum diisi (fail-open). */
	public Boolean getDosen4() {
		return dosen4 == null ? true : dosen4;
	}

	/** @param dosen4 {@code true} agar dosen pembimbing ke-4 berwenang menilai komponen ini. */
	public void setDosen4(Boolean dosen4) {
		this.dosen4 = dosen4;
	}

	/** @return {@code true} bila dosen pembimbing ke-5 berwenang menilai komponen ini; default {@code true} bila belum diisi (fail-open). */
	public Boolean getDosen5() {
		return dosen5 == null ? true : dosen5;
	}

	/** @param dosen5 {@code true} agar dosen pembimbing ke-5 berwenang menilai komponen ini. */
	public void setDosen5(Boolean dosen5) {
		this.dosen5 = dosen5;
	}
}
