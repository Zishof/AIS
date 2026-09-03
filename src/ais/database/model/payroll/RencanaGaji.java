package ais.database.model.payroll;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Calendar;
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
 * Dokumen <b>kepala rencana (anggaran) gaji tahunan</b> &mdash; akar dari rantai
 * <i>perencanaan</i> penggajian AIS. Tabel: {@code payroll.rencana_gaji}, di-audit penuh oleh
 * Hibernate Envers ({@code @Audited} &rarr; tabel bayangan {@code rencana_gaji_aud}).
 *
 * <h2>Struktur kelas ini &mdash; TERVERIFIKASI dari kode, bukan analogi dari kelas realisasi</h2>
 *
 * <p>Berbeda dari yang mungkin diduga dengan menyamakannya begitu saja dengan
 * {@link PembayaranGaji} (kepala rantai <i>realisasi</i>), kelas ini <b>sangat minim</b>: hanya
 * enam field &mdash; kunci utama, tiga field jejak audit, dan <b>dua</b> kolom domain saja:
 * {@link #getKeterangan()} (catatan bebas) dan {@link #getTahun()} (tahun anggaran). Tidak ada
 * {@code bulan}, tidak ada {@code caraPembayaranGaji}, tidak ada {@code satuanKerja}, tidak ada
 * alur persetujuan SOP, tidak ada {@code standingInstruction}, tidak ada {@code postingHistory} &mdash;
 * seluruh mesin realisasi (persetujuan, penomoran surat, posting jurnal, perintah transfer) yang
 * menempel pada {@link PembayaranGaji} <b>tidak punya padanan</b> di sini. Kelas ini murni
 * "map tahun ke catatan", tidak lebih.</p>
 *
 * <p>Kelas ini juga <b>tidak memegang koleksi balik</b> ke anaknya,
 * {@link ais.database.model.payroll.RencanaGajiPunyaPegawai}. Relasi induk&ndash;anak sepenuhnya
 * dipegang dari sisi anak lewat {@code @ManyToOne} bernama {@code rencanaGaji} (dipetakan ke
 * kolom fisik {@code pembayaran_gaji} &mdash; nama kolom yang menyesatkan, sisa salin-tempel dari
 * {@code PembayaranGajiPunyaPegawai}; lihat catatan di
 * {@link ais.database.model.payroll.RencanaGajiPunyaPegawai#getRencanaGaji()}). Seluruh pembacaan
 * anak dari sisi kelas ini dilakukan lewat Criteria/HQL yang menyaring properti
 * {@code rencanaGaji}, bukan lewat koleksi Hibernate.</p>
 *
 * <h2>Rantai perencanaan, lengkap</h2>
 * <pre>
 * RencanaGaji                        (dokumen per TAHUN; hanya keterangan + tahun) &lt;-- KELAS INI
 *   +-- RencanaGajiPunyaPegawai       (satu baris per pegawai per tahun)
 *         +-- RencanaItemGajiPegawai  (rincian per komponen gaji, per bulan)
 * </pre>
 *
 * <h2>Rencana &rarr; realisasi: TIDAK ADA tautan, di kedua tingkat kepala dokumen</h2>
 *
 * <p>Terverifikasi langsung dari field kelas ini: <b>tidak ada satu pun referensi</b> ke
 * {@link PembayaranGaji} maupun sebaliknya. Ini konsisten dengan temuan pada tingkat anak
 * ({@code RencanaGajiPunyaPegawai} vs {@code PembayaranGajiPunyaPegawai}, lihat Javadoc kelas
 * tersebut): kedua rantai &mdash; perencanaan dan realisasi &mdash; berdiri sepenuhnya sendiri,
 * dari kepala sampai daun. Satu-satunya jembatan data antara keduanya, dan itu pun searah
 * realisasi&rarr;rencana (bukan sebaliknya), berlangsung lewat mekanisme di luar kelas ini:
 * {@code RencanaItemGajiPegawaiTreeModel.reset(...)} mencari realisasi
 * {@link PembayaranGajiPunyaPegawai} yang <b>sudah disetujui</b> untuk pegawai/tahun/bulan yang
 * sama, dan bila ketemu memakai nominalnya sebagai nilai rencana &mdash; tetapi tautan itu hanya
 * dipakai sesaat saat penghitungan, tidak pernah disimpan sebagai FK permanen di baris manapun
 * pada rantai ini. Menyimpan/menghapus dokumen kelas ini <b>tidak pernah</b> menyentuh
 * {@link PembayaranGaji} atau anak-anaknya, dan sebaliknya.</p>
 *
 * <h2>Keunikan tahun: ditegakkan aplikasi, global lintas tenant, TANPA constraint DB</h2>
 *
 * <p>Kelas ini <b>tidak punya</b> {@code @Table(uniqueConstraints = ...)} maupun anotasi unique
 * pada kolom {@code tahun}. Satu-satunya penjaga "satu dokumen rencana per tahun" adalah
 * pemeriksaan aplikasi di {@code RencanaGajiAction.check()}: menghitung baris {@code RencanaGaji}
 * lain dengan {@code tahun} yang sama (mengecualikan id dokumen yang sedang diedit) dan menolak
 * simpan bila ditemukan. Query itu <b>tidak menyaring satuan kerja/tenant apa pun</b> &mdash;
 * karena memang tidak ada kolom tenant di kelas ini untuk disaring &mdash; sehingga satu tahun
 * anggaran hanya boleh punya satu dokumen rencana untuk <b>seluruh instalasi</b>, dipakai bersama
 * oleh semua tenant. Ini bukan penyaring tenant yang lemah/hilang; struktur datanya memang sengaja
 * tanpa dimensi tenant di tingkat kepala dokumen.</p>
 *
 * <h2>Jalur "salin rencana tahun lain"</h2>
 *
 * <p>{@code RencanaGajiAction.onSave(Event)} mendukung penyalinan seluruh baris
 * {@link ais.database.model.payroll.RencanaGajiPunyaPegawai} dari dokumen tahun lain
 * ({@code obj.getCopyDari()}) ke dokumen kelas ini yang baru dibuat/disimpan. Setelah baris kepala
 * ({@code this}) tersimpan, sebuah {@code Thread} mentah mengiterasi seluruh pegawai aktif yang
 * punya baris rencana pada dokumen sumber, membuat baris {@code RencanaGajiPunyaPegawai} baru bila
 * belum ada, lalu memanggil {@code RencanaItemGajiPegawaiTreeModel.reset(...)} untuk mengisi
 * nominal bulanannya &mdash; jalur inilah yang, lewat mekanisme pada bagian sebelumnya, bisa saja
 * mewarisi nominal dari realisasi {@link PembayaranGaji} yang sudah disetujui, bukan dari formula.
 * Thread ini berjalan di luar konteks pengguna ZK dan menutup sesi Hibernate native di setiap
 * iterasi.</p>
 *
 * <h2>Gerbang hak akses layar</h2>
 *
 * <p>{@code RencanaGajiAction.doAfterCompose()} memeriksa {@code CommonPrivilages.READ} sebelum
 * mengizinkan layar terbuka, dan menyimpan flag {@code edit}/{@code delete} (dari
 * {@code CommonPrivilages.UPDATE}/{@code DELETE}) yang dipakai untuk menampilkan atau
 * menyembunyikan tombol Ubah/Hapus per baris lewat {@code Common.copyEditDeleteButtons(...)}.
 * <b>Perhatian:</b> {@code onAdd(Event)} (menu tambah dokumen baru) dan {@code onSave(Event)}
 * (penyimpanan sesungguhnya) tidak memeriksa hak apa pun secara langsung &mdash; keduanya hanya
 * dipagari oleh ada/tidaknya tombol pemicu di sisi tampilan. Ini pola yang sama dengan temuan
 * berulang di rantai penggajian lain (gerbang hanya di satu titik render, bukan di titik mutasi);
 * lihat catatan serupa pada
 * {@link ais.database.model.payroll.RencanaGajiPunyaPegawai} dan {@link PembayaranGaji}.</p>
 *
 * <h2>Pengelompokan method</h2>
 * <ul>
 *   <li><b>Identitas:</b> {@link #getId()}/{@link #setId(Long)}.</li>
 *   <li><b>Jejak audit:</b> {@link #getOleh()}/{@link #setOleh(String)},
 *       {@link #getOlehId()}/{@link #setOlehId(String)},
 *       {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, kait
 *       {@link #onUpdate()}.</li>
 *   <li><b>Domain:</b> {@link #getKeterangan()}/{@link #setKeterangan(String)} (catatan bebas),
 *       {@link #getTahun()}/{@link #setTahun(Integer)} (tahun anggaran).</li>
 *   <li><b>Lain-lain:</b> {@link #toString()}.</li>
 * </ul>
 *
 * @see ais.database.model.payroll.RencanaGajiPunyaPegawai
 * @see ais.database.model.payroll.RencanaItemGajiPegawai
 * @see PembayaranGaji
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "payroll", name = "rencana_gaji")
public class RencanaGaji extends GeneralValueObject {

	/**
	 * Versi serialisasi warisan generator hbm2java.
	 *
	 * <p><b>Bukan sidik jari kelas ini.</b> Nilai {@code 2463821577548439808L} dipakai bersama
	 * oleh puluhan entity lain hasil generator yang sama (termasuk
	 * {@link ais.database.model.payroll.RencanaGajiPunyaPegawai} dan {@link PembayaranGaji}); jangan
	 * dijadikan penanda identitas kelas.</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama, {@code IDENTITY} pada kolom {@code id}. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini (kolom {@code oleh}). */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini (kolom {@code olehid}). */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini.
	 *
	 * @return id pengguna, atau {@code null} bila belum pernah terisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir.
	 *
	 * <p><b>Non-obvious:</b> argumen {@code null} atau string kosong <b>diabaikan diam-diam</b>
	 * (nilai lama dipertahankan) &mdash; jejak audit sengaja tidak bisa dikosongkan lewat setter
	 * ini. Diisi otomatis oleh {@code AuditTimestampInterceptor} lewat kait {@link #onUpdate()}.</p>
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null}/kosong.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Representasi teks dokumen rencana gaji: <b>hanya {@link #getKeterangan()}</b>.
	 *
	 * <p>Dipakai antara lain sebagai bagian pertama hasil
	 * {@link ais.database.model.payroll.RencanaGajiPunyaPegawai#toString()} (yang merangkai
	 * {@code "<dokumen rencana> <pegawai>"}). Nilai balik bisa {@code null} bila
	 * {@code keterangan} belum diisi &mdash; pemanggil yang menampilkannya sebagai label harus
	 * siap menerima {@code null} (tampil sebagai teks {@code "null"} bila digabung dengan
	 * operator {@code +}).</p>
	 *
	 * @return isi kolom {@code keterangan}; bisa {@code null}.
	 */
	public String toString() {
		return keterangan;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir. Sama seperti {@link #setOlehId(String)},
	 * argumen {@code null}/kosong diabaikan diam-diam.
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null}/kosong.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah terisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}: memperbarui jejak audit ({@code oleh}, {@code olehId},
	 * {@link #tanggal_dirubah}) tepat sebelum {@code UPDATE} dikirim ke basis data.
	 *
	 * <p>Dipanggil oleh provider persistence (Hibernate), <b>tidak pernah</b> dari kode aplikasi
	 * &mdash; jangan panggil manual. Mendelegasikan sepenuhnya ke
	 * {@code AuditTimestampInterceptor.ubah(this)}.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     /** Stempel waktu perubahan terakhir; diinisialisasi ke waktu server saat object dibuat. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir.
	 *
	 * @param tanggal_dirubah waktu perubahan; biasanya diisi otomatis oleh
	 *                        {@code AuditTimestampInterceptor}, tetapi boleh {@code null}.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir (presisi {@code TIMESTAMP}).
	 *
	 * <p>Diinisialisasi ke waktu server saat objek dibuat, sehingga dokumen yang belum pernah
	 * disimpan pun sudah memiliki nilai bawaan.</p>
	 *
	 * @return waktu perubahan terakhir.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Catatan bebas dokumen rencana gaji tahunan; juga menjadi hasil {@link #toString()} dan
	 * satu-satunya kriteria pencarian teks pada {@code RencanaGajiAction.initCriteria(...)}
	 * ({@code Restrictions.ilike("keterangan", ...)}).
	 */
	private String keterangan;
	/**
	 * Tahun anggaran rencana gaji ini. <b>Kunci keunikan dokumen</b>: ditegakkan hanya di
	 * aplikasi ({@code RencanaGajiAction.check()}), berlaku global lintas seluruh tenant, tanpa
	 * constraint unik di basis data &mdash; lihat pembahasan lengkap pada Javadoc kelas.
	 */
	private Integer tahun;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA.
	 *
	 * <p>Dokumen baru dibuat lewat {@code RencanaGajiAction.onAdd(Event)}, yang langsung membuka
	 * dialog pengisian tahun dan keterangan; nilainya baru benar-benar disimpan setelah
	 * {@code onSave(Event)} lolos pemeriksaan keunikan tahun.</p>
	 */
	public RencanaGaji() {
	}

	/**
	 * Mengembalikan kunci utama dokumen.
	 *
	 * <p>Kolom {@code id} ditandai {@code insertable = false}: nilainya sepenuhnya dihasilkan
	 * basis data (strategi {@code IDENTITY}), sehingga {@code null} sebelum baris tersimpan.</p>
	 *
	 * @return id dokumen, atau {@code null} bila belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama. Dipakai Hibernate saat memuat baris; dari kode aplikasi hanya
	 * relevan untuk kebutuhan migrasi/klon.
	 *
	 * @param id kunci utama; {@code null} berarti dokumen belum tersimpan.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan catatan bebas dokumen rencana gaji tahunan.
	 *
	 * @return keterangan dokumen, bisa {@code null}.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel catatan bebas dokumen. Tanpa validasi.
	 *
	 * @param keterangan keterangan baru; boleh {@code null}.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan tahun anggaran dokumen rencana gaji ini, memberi bawaan tahun berjalan bila
	 * kolomnya masih kosong.
	 *
	 * <p><b>Bukan getter write-back.</b> Berbeda dari padanan yang tampak serupa pada
	 * {@link PembayaranGaji#getBulan()}/{@link PembayaranGaji#getTahun()} (yang menuliskan hasil
	 * turunannya kembali ke field, sehingga sekadar membaca dapat memicu {@code UPDATE}), getter
	 * ini <b>murni</b>: nilai bawaan {@code Calendar.getInstance().get(Calendar.YEAR)} hanya
	 * dikembalikan, tidak pernah ditugaskan ke field {@link #tahun}. Membaca dokumen yang belum
	 * punya tahun tersimpan tidak mengubah apa pun di baris tersebut.</p>
	 *
	 * <p>Konsekuensinya: dokumen dengan kolom {@code tahun} kosong di basis data akan
	 * <b>selalu</b> tampil sebagai tahun berjalan setiap kali dibaca (bukan tahun saat pertama
	 * kali dimuat), dan nilai itu tidak pernah "mengeras" menjadi tersimpan permanen kecuali
	 * lewat {@link #setTahun(Integer)} yang benar-benar dipanggil dan disimpan (mis. oleh
	 * {@code RencanaGajiAction.onSave(Event)}).</p>
	 *
	 * @return tahun anggaran rencana gaji; tidak pernah {@code null} (bawaan tahun kalender
	 *         berjalan bila kolom kosong).
	 */
	public Integer getTahun() {
		return tahun == null ? Calendar.getInstance().get(Calendar.YEAR) : tahun;
	}

	/**
	 * Menyetel tahun anggaran dokumen.
	 *
	 * <p>Tidak ada validasi rentang di sini; keunikan per tahun ditegakkan di luar kelas ini oleh
	 * {@code RencanaGajiAction.check()} sebelum penyimpanan &mdash; lihat pembahasan pada Javadoc
	 * kelas.</p>
	 *
	 * @param tahun tahun anggaran; boleh {@code null} (getter akan memberi bawaan tahun
	 *              berjalan).
	 */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

}
