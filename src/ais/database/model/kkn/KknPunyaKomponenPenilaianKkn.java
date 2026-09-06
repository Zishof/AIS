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
import ais.database.model.Kkn;

/**
 * Entity relasi <b>many-to-one ganda</b> yang menghubungkan satu gelaran {@link Kkn} dengan satu
 * baris katalog {@link KomponenPenilaianKkn} (mis. "Nilai Pembimbing", "Nilai Laporan Akhir") pada
 * tabel {@code public.kkn_punya_komponen_penilaian_kkn}. Baris pada tabel ini adalah cara modul KKN
 * menentukan <b>komponen penilaian apa saja yang dipakai untuk gelaran KKN tertentu</b> — katalog
 * {@code KomponenPenilaianKkn} sendiri bersifat global/dipakai bersama, sedangkan tabel penghubung
 * ini yang mengikatnya ke satu {@code Kkn} beserta bobotnya (bobot sebenarnya disimpan di
 * {@code KomponenPenilaianKkn.bobot}, bukan di sini).
 *
 * <p><b>Kembaran modul PKL:</b> struktur kelas ini identik byte-demi-byte (selain penggantian nama
 * Kkn&rarr;Pkl) dengan {@link ais.database.model.pkl.PklPunyaKomponenPenilaianPkl}; keduanya
 * dipertahankan terpisah karena KKN dan PKL adalah dua modul akademik yang berbeda dengan tabel
 * basis data masing-masing, bukan karena perbedaan perilaku yang disengaja.</p>
 *
 * <p><b>Kode mati terkait (dicatat, bukan ditambal di sini):</b> method
 * {@code MahasiswaDapatKelompokKkn.reloadKknPunyaKomponenPenilaianKkn(Session)} membangun
 * {@code Criteria} atas kelas ini yang menyaring properti {@code parent}, {@code persen}, dan
 * {@code statusPertemuan} — <b>ketiganya tidak ada</b> pada entity ini (field yang benar-benar
 * dideklarasikan hanya {@code id}, {@code oleh}, {@code olehId}, {@code tanggal_dirubah},
 * {@code nama}, {@code keterangan}, {@code kkn}, dan {@code komponenPenilaianKkn} — lihat daftar di
 * bawah). Bila method itu benar-benar dijalankan, Hibernate akan melempar
 * {@code QueryException: could not resolve property}. Diverifikasi ulang dari sisi entity ini:
 * tidak ada satu pun pemanggil method tersebut di seluruh pohon sumber, sehingga method itu aman
 * sebagai kode mati — kembarannya di modul PKL ({@code PklPunyaKomponenPenilaianPkl}, lihat file
 * tersebut) mengidap cacat salin-tempel yang identik.</p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "kkn_punya_komponen_penilaian_kkn")



public class KknPunyaKomponenPenilaianKkn extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key baris relasi ini (bukan primary key {@link Kkn} maupun {@link KomponenPenilaianKkn}). */
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
	 * <b>diabaikan diam-diam</b> (early return) — nilai lama yang sudah tersimpan tetap
	 * dipertahankan, bukan ditimpa jadi kosong. Pola ini konsisten dengan seluruh entity audited
	 * lain di repo ini.
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null}/blank
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama tampilan pengubah terakhir. Nilai {@code null} atau blank diabaikan diam-diam,
	 * sama seperti {@link #setOlehId(String)}.
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
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} sehingga jejak waktu
	 * perubahan selalu akurat tanpa perlu diset manual oleh pemanggil.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * @param tanggal_dirubah stempel waktu perubahan terakhir; biasanya diset otomatis oleh
	 *                        {@link #onUpdate()}, jarang dipanggil manual.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return stempel waktu perubahan terakhir baris ini. Diinisialisasi ke waktu saat ini pada
	 *         konstruksi objek (in-memory), lalu diperbarui otomatis oleh {@link #onUpdate()}
	 *         setiap kali baris diperbarui di basis data.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @return {@link #getNama()} — representasi teks ringkas baris relasi ini. */
	public String toString() {
		return nama;
	}

	/** Nama tampilan baris relasi; boleh {@code null} — lihat {@link #getNama()} untuk fallback-nya. */
	private String nama;
	/** Catatan/keterangan bebas untuk baris relasi ini; boleh {@code null}. */
	private String keterangan;
	/** Gelaran KKN yang memakai komponen penilaian ini. Wajib diisi (kolom {@code NOT NULL}). */
	private Kkn kkn;
	/** Komponen penilaian katalog yang dipakai oleh gelaran KKN di atas. Wajib diisi. */
	private KomponenPenilaianKkn komponenPenilaianKkn;

	/**
	 * @return gelaran {@link Kkn} pemilik baris relasi ini. Sebelum dikembalikan, referensi dicek
	 *         lewat {@code check(kkn)} (proxy Hibernate basi/terputus dari sesi lama diganti dengan
	 *         entity segar bila perlu) sehingga pemanggil tidak perlu menangani
	 *         {@code LazyInitializationException} sendiri.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kkn", nullable = false)
	public Kkn getKkn() {
		kkn = check(kkn);
		return kkn;
	}

	/** @param kkn gelaran KKN yang memakai komponen penilaian ini. */
	public void setKkn(Kkn kkn) {
		this.kkn = kkn;
	}

	/**
	 * @return komponen penilaian katalog ({@link KomponenPenilaianKkn}) yang diikat oleh baris
	 *         relasi ini ke gelaran KKN pada {@link #getKkn()}. Referensi dicek lewat
	 *         {@code check(...)} sebelum dikembalikan, sama seperti {@link #getKkn()}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "komponen_penilaian_kkn", nullable = false)
	public KomponenPenilaianKkn getKomponenPenilaianKkn() {
		komponenPenilaianKkn = check(komponenPenilaianKkn);
		return komponenPenilaianKkn;
	}

	/** @param komponenPenilaianKkn komponen penilaian katalog yang diikat ke gelaran KKN ini. */
	public void setKomponenPenilaianKkn(KomponenPenilaianKkn komponenPenilaianKkn) {
		this.komponenPenilaianKkn = komponenPenilaianKkn;
	}

	/** Konstruktor kosong wajib bagi Hibernate (dipakai lewat refleksi saat memuat entity). */
	public KknPunyaKomponenPenilaianKkn() {
	}

	/**
	 * @return primary key baris relasi ini, di-generate basis data ({@code IDENTITY}); {@code null}
	 *         sebelum baris pertama kali disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * @param id primary key baris relasi ini. Kolom dipetakan {@code insertable = false} sehingga
	 *           pengisian di sini tidak berpengaruh pada {@code INSERT} — nilai sebenarnya selalu
	 *           berasal dari sequence/identity basis data.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return nama tampilan baris relasi ini. Bila field {@link #nama} belum pernah diisi eksplisit
	 *         (masih {@code null}), method ini <b>jatuh balik (fallback) ke nama komponen penilaian
	 *         terkait</b> lewat {@link #getKomponenPenilaianKkn()}{@code .getNama()} — sehingga
	 *         tampilan daftar tetap punya label bermakna walau baris relasi belum pernah diberi nama
	 *         sendiri. Perhatikan efek samping: pemanggilan ini menulis ulang field
	 *         {@link #komponenPenilaianKkn} (memperbarui referensi lewat {@code check(...)}) sebelum
	 *         nilainya dibaca.
	 * @throws NullPointerException bila {@link #getKomponenPenilaianKkn()} mengembalikan
	 *         {@code null} (seharusnya tidak terjadi karena kolomnya {@code NOT NULL}, tapi entity
	 *         yang belum pernah dikaitkan komponennya akan melempar exception ini, bukan
	 *         mengembalikan {@code null} dengan aman).
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		komponenPenilaianKkn = getKomponenPenilaianKkn();
		return this.nama == null ? komponenPenilaianKkn.getNama() : this.nama.trim();
	}

	/** @param nama nama tampilan eksplisit baris relasi; boleh dibiarkan {@code null} untuk memakai fallback. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return catatan/keterangan bebas baris relasi ini, apa adanya tanpa normalisasi. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan catatan/keterangan bebas untuk baris relasi ini. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

}
