package ais.database.model.rab;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;




import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

import ais.common.Common;
import ais.database.model.GeneralValueObject;



/**
 * Entity JPA/Hibernate untuk tabel {@code rab.informasi_rab} — satu pengumuman/berita RAB (Rencana
 * Anggaran Belanja), lengkap dengan jenis, jendela tanggal berlaku, isi HTML, lampiran foto
 * ({@link ais.database.model.file.FotoInformasiRab}), dan komentar ({@link InformasiRabKomentar}).
 * Komentar bersifat KHUSUS untuk kelas ini (relasi {@code ManyToOne} langsung, bukan infrastruktur
 * komentar polymorphic generik seperti {@code ais.database.model.employ.KomunikasiPegawai}); lihat
 * javadoc {@link InformasiRabKomentar} untuk detail.
 *
 * <p>
 * <b>Dua jalur akses yang sudah diverifikasi dari kode, dengan perilaku filter yang BERBEDA:</b>
 * </p>
 * <ul>
 * <li><b>Layar admin ZK</b> {@link ais.action.master.rab.InformasiRabAction} — dijaga
 * {@code Common.doCheckSecurity()} dan {@code CommonPrivilages} (login + hak modul wajib).
 * {@code initCriteria()} di sana HANYA memfilter teks {@code content} (pencarian), TANPA filter
 * satuan kerja maupun jendela {@link #mulai}/{@link #sampai} — pengguna bermodul ini melihat
 * SEMUA pengumuman lintas satuan kerja pada satu grid. Ini mengikuti pola "filter tenant
 * lemah/hilang" yang sudah tercatat berulang di paket ini, bukan temuan baru.</li>
 * <li><b>REST feed</b> {@code ais.action.master.resources.WorkspaceResource#daftarInformasiRab} —
 * dipakai portal/aplikasi klien untuk papan pengumuman satuan kerja. Method ini MEMFILTER jendela
 * aktif secara benar lewat SQL mentah ({@code date(mulai) <= today <= date(sampai)} atau
 * {@code sampai} kosong) dan mendukung filter {@link #satuanKerja} opsional. Berdasarkan javadoc
 * kelas tersebut, endpoint ini (bersama {@code daftarInformasiRabKomentar} dan
 * {@code daftarInformasiRabJumlahKomentar}) sebelumnya TIDAK memvalidasi kredensial sama sekali —
 * sudah DITAMBAL 2026-09-01 dengan mewajibkan {@code username}/{@code password} yang valid. Catatan
 * ini bukan temuan baru dari sesi ini, hanya konfirmasi silang dari sisi model.</li>
 * </ul>
 *
 * <p>
 * <b>Catatan arsitektur — {@link #getAktif()} adalah logika mati/tidak terpakai:</b> getter ini
 * TIDAK direferensikan oleh kode pemanggil mana pun di seluruh basis kode (baik layar admin maupun
 * REST feed menghitung status "berlaku" mereka SENDIRI secara independen, seperti dijelaskan di
 * atas). Lihat javadoc {@link #getAktif()} untuk detail bug logika dan efek samping penulisan
 * ulang field di dalamnya — pola "getter destruktif" + "flag aktif dua-arah" yang sudah tercatat
 * berulang di basis kode ini, dicatat di sini untuk kelengkapan, bukan sebagai eskalasi baru.
 * </p>
 *
 * @see InformasiRabKomentar
 * @see JenisInformasiRab
 * @see ais.action.master.rab.util.RabUtil#INFORMASI
 * @see ais.action.master.rab.util.RabUtil#PENGUMUMAN
 * @see ais.action.master.rab.util.RabUtil#PERINGATAN
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "rab", name = "informasi_rab")



public class InformasiRab extends GeneralValueObject {

	/**
	 * Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable}. Nilai ini diwariskan
	 * dari template hbm2java yang sama dipakai di seluruh paket {@code ais.database.model.rab}
	 * (identik pada banyak entity lain) — bukan kesalahan salin-tempel yang perlu diperbaiki, karena
	 * Hibernate hanya memerlukan nilai ini stabil per-kelas, bukan unik lintas kelas.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key auto-increment (identitas baris pengumuman). */
	private Long id;
	/** Nama tampilan pembuat/pengubah terakhir — field audit "siapa" (bukan referensi FK). */
	private String oleh;
	/**
	 * Id pengguna pembuat/pengubah terakhir — pasangan shadow dari {@link #oleh} untuk audit "siapa"
	 * berbasis id. Kedua field ini adalah KEHARUSAN TEKNIS pola audit di basis kode ini (bukan bug):
	 * {@link #oleh} menyimpan nama tampilan, field ini menyimpan id, keduanya diisi terpisah oleh
	 * pemanggil (tidak ada mekanisme yang menjaga konsistensi otomatis antara keduanya).
	 */
	private String olehId;

	/**
	 * Mengambil id pengguna pembuat/pengubah terakhir.
	 *
	 * @return id pengguna audit, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {return olehId;}

	/**
	 * Mengisi id pengguna pembuat/pengubah terakhir. Guard fail-safe: nilai {@code null} atau string
	 * kosong/spasi diabaikan diam-diam (tidak menimpa nilai lama) — pola yang sama dipakai
	 * {@link #setOleh(String)} di bawah, mencegah audit trail "siapa" tertimpa kosong oleh pemanggil
	 * yang lupa mengisi.
	 *
	 * @param olehId id pengguna audit baru; diabaikan bila kosong/{@code null}
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}this.olehId = olehId;}

	/**
	 * Mengisi nama tampilan pembuat/pengubah terakhir. Guard fail-safe yang sama seperti
	 * {@link #setOlehId(String)}: nilai {@code null}/kosong/spasi-saja diabaikan diam-diam sehingga
	 * nama audit yang sudah tersimpan tidak pernah ditimpa kosong.
	 *
	 * @param oleh nama tampilan audit baru; diabaikan bila kosong/{@code null}
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengambil nama tampilan pembuat/pengubah terakhir.
	 *
	 * @return nama audit, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil otomatis oleh Hibernate sesaat sebelum setiap
	 * {@code UPDATE} baris ini, mendelegasikan ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} untuk menyegarkan
	 * {@link #tanggal_dirubah} ke waktu saat ini. Pola audit-timestamp yang identik dipakai di
	 * seluruh entity {@code Audited} pada paket ini — bukan logika khusus kelas ini.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}
	/**
	 * Timestamp perubahan terakhir. Diinisialisasi ke waktu saat objek dibuat di memori (bukan waktu
	 * simpan pertama ke DB) dan disegarkan otomatis oleh {@link #onUpdate()} pada setiap
	 * {@code UPDATE} berikutnya.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi timestamp perubahan terakhir secara manual. Biasanya tidak perlu dipanggil pemanggil
	 * biasa karena {@link #onUpdate()} sudah menyegarkannya otomatis pada setiap update; tersedia
	 * untuk kasus di mana pemanggil perlu menetapkan nilai eksplisit (mis. saat {@code INSERT}
	 * pertama, seperti dilakukan {@code InformasiRabAction#onSave}).
	 *
	 * @param tanggal_dirubah timestamp baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengambil timestamp perubahan terakhir.
	 *
	 * @return timestamp perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Satuan kerja (tenant) pemilik pengumuman ini, atau {@code null} bila pengumuman berlaku untuk
	 * SEMUA satuan kerja (kolom DB {@code nullable = true}; lihat juga filter {@code satuanKerja}
	 * "kosong/-1/_" = semua tenant pada {@code WorkspaceResource#daftarInformasiRab}).
	 */
	private SatuanKerja satuanKerja;
	/** Jenis/kategori pengumuman — referensi ke katalog {@link JenisInformasiRab} (mis. Informasi/Pengumuman/Peringatan, lihat {@code RabUtil}). */
	private JenisInformasiRab jenisInformasiRab;
	/**
	 * Awal jendela tanggal berlaku pengumuman. Diinisialisasi ke waktu saat objek dibuat di memori,
	 * sehingga form "Tambah" di {@code InformasiRabAction} sudah terisi "sekarang" secara default
	 * sebelum pengguna mengubahnya. Kolom DB {@code nullable = false} — wajib diisi.
	 */
	private Date mulai = ais.ui.util.WaktuUtil.getDate();
	/**
	 * Akhir jendela tanggal berlaku pengumuman, atau {@code null} untuk tanpa batas akhir (berlaku
	 * selamanya sejak {@link #mulai}) — dikonfirmasi dari logika {@code WorkspaceResource#daftarInformasiRab}
	 * yang memperlakukan {@code sampai IS NULL} sebagai "belum berakhir".
	 */
	private Date sampai;
	/** Isi pengumuman berformat HTML (diisi lewat editor WYSIWYG {@code MyCkEditor} pada layar admin), ditampilkan apa adanya baik di grid admin maupun feed REST. */
	private String content;
	/**
	 * Field status "aktif" yang HANYA ditulis/dibaca oleh pasangan {@link #getAktif()}/
	 * {@link #setAktif(Boolean)} di bawah — lihat javadoc {@link #getAktif()} untuk bug logika dan
	 * efek sampingnya. Tidak ada pemanggil lain di basis kode yang membaca field ini secara langsung.
	 */
	private Boolean aktif;

	/** Konstruktor default (wajib untuk Hibernate); juga dipakai {@code InformasiRabAction#onAdd} untuk membuat entri baru kosong pada form "Tambah". */
	public InformasiRab() {
	}

	/**
	 * Konstruktor kenyamanan untuk membuat referensi ber-id tanpa memuat seluruh baris dari DB
	 * (pola umum entity Hibernate untuk dipakai sebagai FK proxy ringan). Tidak ada pemanggil
	 * konstruktor ini di seluruh basis kode saat ini (hanya konstruktor tanpa argumen yang dipakai) —
	 * konsisten dengan pola konstruktor yatim yang berulang ditemukan di paket entity ini.
	 *
	 * @param id id baris yang akan direferensikan
	 */
	public InformasiRab(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil id baris (primary key).
	 *
	 * @return id baris, atau {@code null} untuk entity baru yang belum disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi id baris secara manual. Kolom dipetakan {@code insertable = false} (nilai sesungguhnya
	 * berasal dari {@code IDENTITY} auto-increment DB saat {@code INSERT}); setter ini terutama
	 * relevan untuk membangun objek referensi FK ringan tanpa query (lihat
	 * {@link #InformasiRab(Long)}).
	 *
	 * @param id id baris baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil satuan kerja (tenant) pemilik pengumuman ini, dengan AUTO-POPULATE untuk entity BARU:
	 * bila {@link #satuanKerja} belum diisi DAN {@link #id} masih {@code null} (baris belum pernah
	 * disimpan), method ini otomatis mengisinya dari satuan kerja pengguna yang sedang login
	 * ({@code Common.getCurrentUser().ambilSatuanKerja()}) — pola default lazy yang sama dipakai
	 * banyak entity lain di paket ini. Untuk baris yang SUDAH tersimpan ({@link #id} != null),
	 * auto-populate ini TIDAK berlaku lagi; getter hanya mengembalikan apa pun yang benar-benar
	 * tersimpan (termasuk {@code null}, yang berarti "berlaku untuk semua satuan kerja").
	 *
	 * <p>
	 * Kegagalan mengambil satuan kerja pengguna saat ini (mis. tidak ada sesi login aktif — jalur
	 * yang bisa terjadi bila objek dibuat di luar konteks request ZK) ditelan diam-diam lewat
	 * {@code catch (Exception e)} yang hanya mencatat ke {@code ErrorAuditUtil}, sehingga getter tetap
	 * mengembalikan {@code null} tanpa melempar error ke pemanggil.
	 * </p>
	 *
	 * @return satuan kerja pemilik, atau {@code null} bila berlaku untuk semua satuan kerja atau
	 *         gagal ditentukan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		if (this.satuanKerja == null && this.id == null) {
			try {
				SatuanKerja satuanKerja = Common.getCurrentUser()
						.ambilSatuanKerja();
				this.satuanKerja = satuanKerja;
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/rab/InformasiRab.java:107");
			}
		}
		return satuanKerja;
	}

	/**
	 * Mengisi satuan kerja pemilik secara eksplisit, menimpa kandidat auto-populate yang mungkin
	 * dihasilkan {@link #getSatuanKerja()}.
	 *
	 * @param satuanKerja satuan kerja pemilik baru, atau {@code null} untuk "semua satuan kerja"
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/**
	 * Menghitung DAN menuliskan-ulang status "aktif" berdasarkan perbandingan {@link #mulai}/
	 * {@link #sampai} terhadap waktu saat ini, lalu mengembalikan hasilnya.
	 *
	 * <p>
	 * <b>Bug logika (dicatat, bukan diperbaiki di sesi ini karena tidak ada pemanggil nyata):</b>
	 * kondisi yang dipakai adalah {@code sampai.before(now) && mulai.after(now)} — yaitu "tanggal
	 * akhir sudah lewat" DAN "tanggal mulai belum tiba" SEKALIGUS. Untuk data valid di mana
	 * {@code mulai <= sampai} (kasus normal), dua syarat ini SALING KONTRADIKTIF dan tidak pernah
	 * bisa true bersamaan (agar keduanya true, dibutuhkan {@code sampai < now < mulai}, yang hanya
	 * mungkin bila data korup dengan {@code mulai > sampai}). Akibatnya getter ini nyaris SELALU
	 * mengembalikan {@code false} untuk seluruh data valid, terlepas dari apakah pengumuman
	 * sesungguhnya sedang berlaku atau tidak. Logika yang tampaknya dimaksud (rentang
	 * {@code mulai <= now <= sampai}) sudah diimplementasikan secara TERPISAH dan BENAR di
	 * {@code WorkspaceResource#daftarInformasiRab} lewat SQL mentah — getter ini tidak dipakai di
	 * sana maupun di layar admin {@code InformasiRabAction} manapun.
	 * </p>
	 *
	 * <p>
	 * <b>Efek samping "getter destruktif":</b> setiap pemanggilan method ini MENULIS ULANG field
	 * {@link #aktif} (baik dipanggil eksplisit oleh kode Java, maupun secara implisit oleh Hibernate
	 * saat property-access flush/dirty-check, karena getter ini TIDAK ditandai {@code @Transient})
	 * sebelum mengembalikan nilainya — nilai apa pun yang sebelumnya ditulis lewat
	 * {@link #setAktif(Boolean)} akan tertimpa oleh hasil kalkulasi (yang, karena bug di atas, hampir
	 * selalu {@code false}) pada pembacaan berikutnya. Pola "getter destruktif" + "flag aktif
	 * dua-arah yang saling menimpa" ini sudah tercatat berulang di basis kode; karena tidak ada
	 * pemanggil yang benar-benar bergantung pada nilai ini untuk keputusan otorisasi/tampilan,
	 * dampaknya saat ini murni kosmetik/mati, bukan celah keamanan aktif.
	 * </p>
	 *
	 * @return {@code false} untuk hampir semua data valid (lihat catatan bug di atas); jarang/tidak
	 *         pernah {@code true} kecuali data korup
	 */
	public Boolean getAktif() {
		if (mulai != null && sampai != null && sampai.before(ais.ui.util.WaktuUtil.getDate())
				&& mulai.after(ais.ui.util.WaktuUtil.getDate())) {
			aktif = true;
		} else {
			aktif = false;
		}
		return aktif;
	}

	/**
	 * Mengisi field {@link #aktif} secara langsung TANPA melalui kalkulasi. Perlu diingat bahwa nilai
	 * yang diisi lewat setter ini akan tertimpa pada pembacaan berikutnya lewat {@link #getAktif()}
	 * (lihat catatan bug/efek samping di javadoc getter tersebut).
	 *
	 * @param aktif nilai status aktif yang ingin diset secara manual
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengambil awal jendela tanggal berlaku pengumuman.
	 *
	 * @return tanggal mulai berlaku (tidak pernah {@code null} untuk baris valid — kolom DB
	 *         {@code nullable = false})
	 */
	@Column(name = "mulai", nullable = false)
	@Temporal(TemporalType.TIMESTAMP)
	public Date getMulai() {
		return mulai;
	}

	/**
	 * Mengisi awal jendela tanggal berlaku pengumuman.
	 *
	 * @param mulai tanggal mulai berlaku baru
	 */
	public void setMulai(Date mulai) {
		this.mulai = mulai;
	}

	/**
	 * Mengambil akhir jendela tanggal berlaku pengumuman.
	 *
	 * @return tanggal akhir berlaku, atau {@code null} bila tanpa batas akhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getSampai() {
		return sampai;
	}

	/**
	 * Mengisi akhir jendela tanggal berlaku pengumuman.
	 *
	 * @param sampai tanggal akhir berlaku baru, atau {@code null} untuk tanpa batas akhir
	 */
	public void setSampai(Date sampai) {
		this.sampai = sampai;
	}

	/**
	 * Mengambil isi pengumuman berformat HTML.
	 *
	 * @return isi HTML pengumuman
	 */
	@Column(name = "content", nullable = false, columnDefinition = "text")
	public String getContent() {
		return content;
	}

	/**
	 * Mengisi isi pengumuman berformat HTML (biasanya berasal dari editor WYSIWYG {@code MyCkEditor}
	 * pada layar admin).
	 *
	 * @param content isi HTML pengumuman baru
	 */
	public void setContent(String content) {
		this.content = content;
	}

	/**
	 * Mengambil jenis/kategori pengumuman ini.
	 *
	 * @return jenis informasi RAB, atau {@code null} bila belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jenis_informasi_rab", nullable = true)
	public JenisInformasiRab getJenisInformasiRab() {
		return jenisInformasiRab;
	}

	/**
	 * Mengisi jenis/kategori pengumuman ini.
	 *
	 * @param jenisInformasiRab jenis informasi RAB baru
	 */
	public void setJenisInformasiRab(JenisInformasiRab jenisInformasiRab) {
		this.jenisInformasiRab = jenisInformasiRab;
	}

}
