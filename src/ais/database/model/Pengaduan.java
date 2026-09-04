package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
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
import org.zkoss.zul.Row;
import org.zkoss.zul.Textbox;

import ais.common.Common;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.ui.util.WaktuUtil;

/**
 * Entity <b>pengaduan</b> (keluhan/aduan/pertanyaan) yang diajukan seorang pengguna — mahasiswa,
 * siswa, pegawai, atau kontak WhatsApp anonim yang belum punya akun — kepada institusi, beserta
 * seluruh jejak penanganannya sampai disetujui/ditutup. Dipetakan ke tabel {@code public.pengaduan}
 * lewat anotasi (tidak ada berkas {@code .hbm.xml}; pendaftarannya ada di {@code hibernate.cfg.xml}
 * baris <i>mapping class</i> {@code ais.database.model.Pengaduan}), dengan {@code dynamicInsert} +
 * {@code dynamicUpdate} dan {@link org.hibernate.envers.Audited @Audited} (Envers merekam setiap
 * versi ke tabel revisi).
 *
 * <h2>Posisi dalam alur penanganan</h2>
 *
 * <p>Satu baris {@code Pengaduan} lahir dari salah satu dari <b>tiga</b> pintu masuk yang sangat
 * berbeda karakternya:</p>
 *
 * <ol>
 *   <li><b>Layar ZK</b> {@code ais.action.master.PengaduanAction} ({@code
 *       /pages/master/pengaduan.zul}; terdaftar di menu sebagai "Pengaduan-Pengaduan", "Pengaduan
 *       Mahasiswa", dan "Pengaduan Siswa"). Ini jalur terlengkap: operator/pelapor memilih
 *       {@link JenisPengaduan}, mengisi keterangan, mengisi <i>parameter tambahan</i> dinamis
 *       (termasuk lampiran), lalu dokumen mengalir lewat SOP.</li>
 *   <li><b>API native mobile</b> {@code ais.action.servlet.api.PengaduanMahasiswaApi} (operasi
 *       {@code list}/{@code simpan}/{@code hapus}). Hanya untuk mahasiswa yang sedang login dan
 *       hanya menyentuh baris miliknya sendiri ({@code mahasiswa.id} + {@code diajukan.userId}
 *       harus cocok dua-duanya). "Hapus" di sini adalah <i>soft delete</i>: {@code aktif = false}.</li>
 *   <li><b>Webhook WhatsApp</b> {@code ais.action.servlet.Wa#simpanPesan}. Setiap pesan masuk dari
 *       nomor yang tidak masuk daftar cekal dijadikan satu {@code Pengaduan}: {@code nama} diisi
 *       <b>nama profil WhatsApp pengirim</b>, {@code keterangan} diisi isi pesan, {@code
 *       jenisPengaduan} diisi jenis aktif ber-id terkecil, {@code diajukan} diisi {@link Tbmuser}
 *       yang dibuat otomatis dengan role {@code "pengadu"}, {@code req} diisi <b>payload webhook
 *       mentah</b>, dan setelah bot menjawab {@code res} diisi respons mentah Graph API sedangkan
 *       {@code tanggapan} diisi teks balasan bot.</li>
 * </ol>
 *
 * <p>Setelah tersimpan, penanganan berjalan lewat {@link DisposisiSop} (lihat {@link
 * #getDisposisiSop()}); status akhir tercermin pada {@link #getSetujui()}, {@link
 * #getSetujuiTanggal()}, {@link #getDisetujuiOleh()}, dan {@link #getAktif()}. Rekapitulasinya
 * dibaca oleh {@code ais.action.report.format1.akademik.LaporanPengaduan}.</p>
 *
 * <h2>Pengelompokan anggota</h2>
 *
 * <ul>
 *   <li><b>Audit/identitas warisan</b> — {@link #getId()}, {@link #getOleh()}, {@link
 *       #getOlehId()}, {@link #getTanggal_dirubah()}, {@code onUpdate()}. Lihat catatan
 *       "deklarasi ulang" di bawah.</li>
 *   <li><b>Identitas pelapor</b> — {@link #getDiajukan()} (akun), {@link #getPegawai()},
 *       {@link #getMahasiswa()}, {@link #getSiswa()}. Keempatnya <b>tidak saling eksklusif</b> dan
 *       tidak ada satu pun yang wajib; kombinasi yang terisi menentukan bentuk baris di layar dan
 *       jalur persetujuannya.</li>
 *   <li><b>Isi aduan</b> — {@link #getNama()} (judul), {@link #getKeterangan()} (uraian),
 *       {@link #getJenisPengaduan()}, {@link #getWaktu()}.</li>
 *   <li><b>Parameter tambahan dinamis</b> — {@link #getParameterTambahan()}, {@link
 *       #getParameterTambahanInds()}, {@link #populateParameterTambahan(List)}, {@link
 *       #ambilDataParameterTambahan()}. Dua kolom teks berisi banyak baris; format lengkapnya
 *       diuraikan pada Javadoc {@link #populateParameterTambahan(List)}.</li>
 *   <li><b>Penomoran agenda</b> — {@link #getKode()}, {@link #getIndex()}, {@link #getTahun()},
 *       {@link #getBulan()}. Nomor dirakit di luar entity oleh {@code
 *       PengaduanAction#generateCode(JenisPengaduan, boolean)} memakai {@link
 *       ais.database.model.surat.NomorSurat} milik {@link JenisPengaduan}.</li>
 *   <li><b>Penanganan/persetujuan</b> — {@link #getDisposisiSop()}, {@link #getSetujui()},
 *       {@link #getSetujuiTanggal()}, {@link #getDisetujuiOleh()}, {@link #getTanggapan()},
 *       {@link #getAktif()}.</li>
 *   <li><b>Jejak integrasi WhatsApp</b> — {@link #getReq()}, {@link #getRes()}.</li>
 * </ul>
 *
 * <p>Class ini <b>tidak punya satu pun method utilitas/query statis</b>: seluruh pencarian,
 * penomoran, dan penyaringan hak akses berada di {@code PengaduanAction} dan {@code
 * PengaduanMahasiswaApi}. Yang ada hanyalah getter/setter properti plus dua method bisnis
 * ({@link #populateParameterTambahan(List)} dan {@link #ambilDataParameterTambahan()}).</p>
 *
 * <h2>Kenapa {@code id}/{@code oleh}/{@code olehId}/{@code tanggal_dirubah} dideklarasikan ulang</h2>
 *
 * <p>{@link GeneralValueObject} — lewat {@link DataSop} — <b>bukan</b> {@code @Entity} maupun
 * {@code @MappedSuperclass}, melainkan POJO abstrak biasa. Hibernate karena itu <b>tidak</b>
 * memetakan properti apa pun milik induk. Deklarasi ulang field {@code id}, {@code oleh}, {@code
 * olehId}, {@code tanggal_dirubah} beserta getter/setter-nya di sini <b>bukan duplikasi yang
 * keliru</b>, melainkan keharusan teknis supaya kolom-kolom itu benar-benar ada di tabel. Pola yang
 * sama muncul di hampir seluruh entity turunan {@link GeneralValueObject}.</p>
 *
 * <h2>Getter yang menulis balik (pola berulang — sudah diverifikasi)</h2>
 *
 * <p>Sebagian besar getter di sini <b>tidak murni membaca</b>. Karena tidak ada satu pun properti
 * yang ditandai {@code @Transient}, semua nilai hasil perhitungan getter ikut ter-{@code UPDATE}
 * <b>permanen</b> ke basis data begitu entity di-flush pada session yang mengelolanya. Daftar
 * lengkapnya:</p>
 *
 * <ul>
 *   <li>{@link #getNama()} — mengisi {@code nama} dari nama {@link JenisPengaduan} bila kosong.</li>
 *   <li>{@link #getTahun()} dan {@link #getBulan()} — mengisi dari jam sistem bila {@code null}.</li>
 *   <li>{@link #getParameterTambahan()} dan {@link #getParameterTambahanInds()} — mengubah {@code
 *       null} menjadi string kosong.</li>
 *   <li>{@link #getSetujui()} — menurunkan status setuju dari {@link DisposisiSop}.</li>
 *   <li>{@link #getAktif()} — memaksa {@code false} bila disposisi mati atau berhenti di simpul
 *       penolakan.</li>
 *   <li>{@link #getPegawai()} — <b>menimpa</b> {@code pegawai} dengan pegawai milik {@link
 *       #getDiajukan()}, sekaligus menulis ulang field {@code diajukan}.</li>
 *   <li>{@link #getJenisPengaduan()}, {@link #getDisposisiSop()}, {@link #getMahasiswa()},
 *       {@link #getSiswa()}, {@link #getDisetujuiOleh()} — menugaskan kembali hasil {@link
 *       GeneralValueObject#check(Object)} (resolusi proxy lazy), sesuai pola standar repo ini.</li>
 * </ul>
 *
 * <p><b>Getter yang menutup session Hibernate: tidak ada di file ini.</b> Semua pengelolaan session
 * berada di dalam {@link GeneralValueObject#check(Object)}, yang membuka dan menutup session
 * penyelamatnya sendiri; file ini tidak pernah memanggil {@code HibernateUtil.openSession()}.</p>
 *
 * <h2>Catatan privasi (hasil pemeriksaan)</h2>
 *
 * <p>Karena ini entity pengaduan, identitas pelapor diperiksa khusus. Temuannya:</p>
 *
 * <ul>
 *   <li><b>Tidak ada mekanisme anonimasi sama sekali.</b> Tidak ada field "anonim"/"rahasia", tidak
 *       ada penyamaran nama, dan tidak ada jalur penyimpanan yang membuang identitas. Identitas
 *       pelapor selalu ikut tersimpan dan selalu ikut ditampilkan pada baris daftar
 *       ({@code PengaduanAction.PengaduanRenderer} menampilkan foto + NIM/NIS/kode pegawai/user id
 *       pelapor).</li>
 *   <li><b>Tidak ada field "pihak yang diadukan".</b> Entity ini tidak menyimpan siapa yang
 *       dilaporkan, jadi tidak ada relasi langsung yang bisa membocorkan identitas pelapor kepada
 *       terlapor.</li>
 *   <li><b>Namun rute persetujuan menyalurkan aduan pegawai ke atasan langsung pelapor.</b> Kolom
 *       {@code pegawai} dipaksa berisi pegawai <i>pelapor</i> oleh {@link #getPegawai()}, dan
 *       {@code PengaduanAction} menampilkan kendali persetujuan kepada pemilik akun yang menjadi
 *       {@code atasanlangsung}/{@code atasanlangsung2}/{@code atasanlangsung3} pegawai tersebut.
 *       Konsekuensinya: bila seorang pegawai mengadukan atasannya sendiri, aduan itu — lengkap
 *       dengan identitas pelapor — muncul di layar orang yang diadukan. Ini konsekuensi rancangan
 *       alur, bukan cacat kode di entity ini, tapi perlu diketahui siapa pun yang menyentuh
 *       {@link #getPegawai()}.</li>
 *   <li><b>{@code req} menyimpan payload WhatsApp mentah</b> (lihat {@link #getReq()}), yang berisi
 *       nomor telepon dan nama profil pengirim tanpa penyamaran apa pun. Kolom {@code req} dan
 *       {@code res} ikut dalam daftar kolom ekspor/impor massal di {@code PengaduanAction}, jadi
 *       siapa pun yang punya tombol Cetak/Upload di layar pengaduan dapat menarik keluar nomor HP
 *       para pelapor.</li>
 *   <li><b>Isi aduan tidak pernah benar-benar hilang.</b> {@code @Audited} menyalin setiap versi ke
 *       tabel revisi Envers, dan "hapus" lewat API hanya menyetel {@code aktif = false}.</li>
 * </ul>
 *
 * @see GeneralValueObject
 * @see DataSop
 * @see JenisPengaduan
 * @see DisposisiSop
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "pengaduan")
public class Pengaduan extends DataSop {

	/**
	 * Versi serialisasi Java. Nilainya sengaja disamakan dengan {@link JenisPengaduan} (keduanya
	 * hasil <i>generate</i> dari cetakan yang sama) dan tidak boleh diubah tanpa alasan, karena
	 * instance entity ini ikut diserialisasi ke dalam desktop/session ZK.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama tabel {@code pengaduan}; lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna pembuat baris (audit); lihat {@link #getOleh()}. */
	private String oleh;
	/** User id pengguna pembuat baris (audit); lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan user id pembuat baris ini (kolom audit warisan pola {@link
	 * GeneralValueObject}). Nilainya diisi otomatis oleh lapisan penyimpanan umum, bukan oleh
	 * pelapor.
	 *
	 * @return user id pembuat, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel user id pembuat baris. <b>Perhatikan:</b> nilai {@code null}, kosong, atau hanya
	 * berisi spasi <b>diabaikan diam-diam</b> — field lama dipertahankan. Akibatnya kolom audit ini
	 * tidak pernah bisa dikosongkan kembali setelah terisi; itu memang disengaja supaya jejak
	 * pembuat tidak hilang saat entity di-<i>merge</i> dari form yang tidak membawa kolom audit.
	 *
	 * @param olehId user id pembuat; diabaikan bila {@code null}/kosong/hanya spasi
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pembuat baris. Sama seperti {@link #setOlehId(String)}, nilai
	 * {@code null}/kosong/hanya spasi <b>diabaikan diam-diam</b> sehingga nilai lama bertahan.
	 *
	 * @param oleh nama pembuat; diabaikan bila {@code null}/kosong/hanya spasi
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna pembuat baris ini (kolom audit warisan pola {@link
	 * GeneralValueObject}).
	 *
	 * @return nama pembuat, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Dua deklarasi yang sengaja dibiarkan menyatu pada satu baris (bentuk aslinya hasil
	 * penyisipan otomatis; <b>jangan dipecah</b> supaya diff terhadap entity lain tetap seragam):
	 *
	 * <ol>
	 *   <li>{@code onUpdate()} — kait JPA {@link javax.persistence.PreUpdate @PreUpdate} yang
	 *       dijalankan Hibernate tepat sebelum setiap {@code UPDATE}. Isinya melimpahkan pekerjaan
	 *       ke {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)}, yang
	 *       memperbarui stempel waktu perubahan. Tidak untuk dipanggil manual.</li>
	 *   <li>field {@code tanggal_dirubah} — diinisialisasi ke waktu sekarang lewat {@link
	 *       WaktuUtil#getDate()} supaya baris baru sudah punya stempel walau belum pernah
	 *       di-{@code UPDATE}. Lihat {@link #getTanggal_dirubah()}.</li>
	 * </ol>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Umumnya tidak perlu dipanggil manual — nilainya
	 * diurus kait {@code onUpdate()} di atas.
	 *
	 * @param tanggal_dirubah stempel waktu perubahan yang baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini (kolom {@code timestamp}).
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} untuk object yang dibuat lewat
	 *         konstruktor karena field-nya diinisialisasi ke waktu sekarang
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks ringkas untuk keperluan log dan komponen ZK, berbentuk {@code "id-nama"}.
	 *
	 * <p><b>Membaca field {@code nama} langsung</b>, bukan lewat {@link #getNama()}, sehingga
	 * <i>tidak</i> memicu pengisian otomatis judul dari {@link JenisPengaduan}. Untuk baris yang
	 * judulnya belum pernah dibaca, hasilnya bisa berbentuk {@code "12-null"}.</p>
	 *
	 * @return gabungan id dan judul aduan yang dipisah tanda hubung
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Judul/ringkasan aduan; lihat {@link #getNama()}. */
	private String nama;
	/** Uraian lengkap aduan (kolom {@code text}); lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Balasan/tanggapan penanganan (kolom {@code text}); lihat {@link #getTanggapan()}. */
	private String tanggapan;
	/** Payload permintaan mentah dari integrasi WhatsApp; lihat {@link #getReq()}. */
	private String req;
	/** Respons mentah dari integrasi WhatsApp; lihat {@link #getRes()}. */
	private String res;
	/** Waktu pengaduan diajukan; lihat {@link #getWaktu()}. */
	private Date waktu;
	/** Master jenis/kategori aduan; lihat {@link #getJenisPengaduan()}. */
	private JenisPengaduan jenisPengaduan;
	/** Akun pengguna yang mengajukan; lihat {@link #getDiajukan()}. */
	private Tbmuser diajukan;
	/** Pegawai pelapor (diturunkan otomatis dari {@link #diajukan}); lihat {@link #getPegawai()}. */
	private Pegawai pegawai;
	/** Mahasiswa pelapor; lihat {@link #getMahasiswa()}. */
	private Mahasiswa mahasiswa;
	/** Siswa pelapor; lihat {@link #getSiswa()}. */
	private Siswa siswa;
	/** Jawaban parameter tambahan versi label manusia; lihat {@link #getParameterTambahan()}. */
	private String parameterTambahan;
	/** Jawaban parameter tambahan versi id; lihat {@link #getParameterTambahanInds()}. */
	private String parameterTambahanInds;
	/** Simpul disposisi SOP yang sedang/terakhir menangani; lihat {@link #getDisposisiSop()}. */
	private DisposisiSop disposisiSop;
	/** Tahun agenda untuk penomoran; lihat {@link #getTahun()}. */
	private Integer tahun;
	/** Bulan agenda (1-12) untuk penomoran; lihat {@link #getBulan()}. */
	private Integer bulan;
	/** Nomor agenda hasil format {@code NomorSurat}; lihat {@link #getKode()}. */
	private String kode;
	/** Nomor urut mentah di balik {@link #kode}; lihat {@link #getIndex()}. */
	private Long index;
	/** Tanggal persetujuan; lihat {@link #getSetujuiTanggal()}. */
	private Date setujuiTanggal;
	/** Akun penyetuju (ejaan field sengaja dipertahankan); lihat {@link #getDisetujuiOleh()}. */
	private Tbmuser disetujiOleh;
	/** Penanda sudah disetujui; lihat {@link #getSetujui()}. */
	private Boolean setujui;
	/** Penanda baris masih berlaku; lihat {@link #getAktif()}. */
	private Boolean aktif;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA dan dipakai semua pintu masuk
	 * ({@code PengaduanAction}, {@code PengaduanMahasiswaApi}, dan {@code Wa}) untuk membuat aduan
	 * baru. Tidak mengisi apa pun; seluruh nilai bawaan justru muncul belakangan lewat getter yang
	 * mengisi sendiri ({@link #getWaktu()}, {@link #getTahun()}, {@link #getBulan()}, {@link
	 * #getAktif()}, {@link #getSetujui()}).
	 */
	public Pengaduan() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * @return id pengaduan, atau {@code null} bila belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama. Hanya untuk dipakai Hibernate dan kode yang sengaja memasang id
	 * (mis. impor data massal); jangan diubah pada entity yang sudah tersimpan.
	 *
	 * @param id kunci utama yang baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nomor agenda pengaduan yang sudah diformat.
	 *
	 * <p>Nilainya dirakit di luar entity oleh {@code PengaduanAction#generateCode(JenisPengaduan,
	 * boolean)} memakai {@link ais.database.model.surat.NomorSurat} milik {@link JenisPengaduan},
	 * lalu dipasang saat baris pertama kali dirender. Getter ini <b>mengembalikan {@code null}
	 * bila kode kosong/hanya spasi</b> (bukan string kosong) — itu penting, karena pemanggilnya
	 * memakai kondisi "kode masih kosong" sebagai pemicu pembuatan nomor.</p>
	 *
	 * @return nomor agenda yang sudah dipangkas spasi, atau {@code null} bila belum ada
	 */
	@Column(name = "kode")
	public String getKode() {
		return this.kode == null || kode.trim().isEmpty() ? null : kode.trim();
	}

	/**
	 * Menyetel nomor agenda pengaduan.
	 *
	 * @param kode nomor agenda hasil format {@code NomorSurat}
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan judul/ringkasan aduan.
	 *
	 * <p><b>Getter ini menulis balik.</b> Bila {@code nama} masih {@code null}/kosong <i>dan</i>
	 * {@link #getJenisPengaduan()} terisi, field {@code nama} diisi dengan nama jenis pengaduan.
	 * Karena {@code nama} adalah kolom terpetakan sungguhan (bahkan {@code nullable = false} pada
	 * anotasi, walau {@code cascade.sql} pernah melepas batasan itu di basis data), nilai hasil
	 * pengisian otomatis ini ikut ter-{@code UPDATE} permanen saat entity di-flush.</p>
	 *
	 * <p>Pada jalur WhatsApp isi kolom ini bukan judul aduan melainkan <b>nama profil WhatsApp
	 * pengirim</b> (lihat {@code Wa#simpanPesan}); pada jalur layar dan API mobile isinya judul
	 * yang diketik pelapor.</p>
	 *
	 * @return judul aduan yang sudah dipangkas spasi, atau {@code null} bila tetap kosong
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		if ((nama == null || nama.isEmpty()) && getJenisPengaduan() != null) {
			nama = getJenisPengaduan().getNama();
		}
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel judul/ringkasan aduan.
	 *
	 * @param nama judul aduan
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Menyetel nomor urut mentah di balik nomor agenda.
	 *
	 * @param index nomor urut baru
	 * @see #getIndex()
	 */
	public void setIndex(Long index) {
		this.index = index;
	}

	/**
	 * Mengembalikan nomor urut mentah yang mendasari {@link #getKode()}.
	 *
	 * <p>Diisi oleh {@code PengaduanAction.PengaduanRenderer} bersamaan dengan pembuatan kode.
	 * <b>Kuirk yang perlu diketahui:</b> di sana nilainya berasal dari {@code
	 * PengaduanAction#getindex(JenisPengaduan)} — yang <i>sudah</i> mengembalikan "jumlah baris + 1"
	 * — lalu masih dinaikkan sekali lagi ({@code setIndex(++currentIndex)}). Jadi angka yang
	 * tersimpan di kolom ini bisa berselisih satu terhadap angka yang benar-benar tercetak di
	 * {@link #getKode()}. Jangan pakai kolom ini sebagai sumber kebenaran nomor agenda; pakai
	 * {@link #getKode()}.</p>
	 *
	 * @return nomor urut mentah, atau {@code null} bila belum pernah diisi
	 */
	public Long getIndex() {
		return index;
	}

	/**
	 * Mengembalikan uraian lengkap aduan (kolom {@code text}).
	 *
	 * <p>Mengembalikan <b>string kosong</b>, bukan {@code null}, bila belum diisi — supaya
	 * pemanggil di layar bisa langsung memanggil {@code .trim().isEmpty()} tanpa penjagaan
	 * {@code null}. Berbeda dengan getter lain di class ini, nilai substitusi itu <b>tidak</b>
	 * ditulis balik ke field.</p>
	 *
	 * @return uraian aduan, atau string kosong bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan == null ? "" : keterangan;
	}

	/**
	 * Menyetel uraian lengkap aduan.
	 *
	 * @param keterangan uraian aduan; pada jalur WhatsApp diisi isi pesan mentah pengirim
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan waktu pengaduan diajukan.
	 *
	 * <p>Bila field masih {@code null}, getter mengembalikan <b>waktu sekarang</b> ({@link
	 * WaktuUtil#getDate()}) sebagai pengganti. Berbeda dengan {@link #getNama()} atau {@link
	 * #getTahun()}, nilai pengganti ini <b>tidak</b> ditulis balik ke field, sehingga dua
	 * pemanggilan berturut-turut pada entity yang belum punya waktu akan menghasilkan nilai yang
	 * sedikit berbeda dan penyimpanan tetap menulis {@code null} kalau setter tak pernah dipanggil.
	 * Pemanggil yang butuh nilai tetap harus memanggil {@link #setWaktu(Date)} secara eksplisit —
	 * itulah yang dilakukan {@code PengaduanMahasiswaApi} saat membuat aduan baru.</p>
	 *
	 * @return waktu pengajuan; tidak pernah {@code null}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktu() {
		return waktu == null ? WaktuUtil.getDate() : waktu;
	}

	/**
	 * Menyetel waktu pengaduan diajukan.
	 *
	 * @param waktu waktu pengajuan
	 */
	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

	/**
	 * Mengembalikan master jenis/kategori pengaduan.
	 *
	 * <p>Relasi lazy; getter menugaskan kembali hasil {@link GeneralValueObject#check(Object)} ke
	 * field, sesuai pola standar resolusi proxy di repo ini. {@link JenisPengaduan} membawa
	 * {@code NomorSurat} untuk penomoran agenda dan himpunan {@code
	 * KelompokParameterTambahanPengaduan} yang menentukan formulir parameter tambahan mana yang
	 * ditampilkan.</p>
	 *
	 * @return jenis pengaduan, atau {@code null} bila belum dipilih
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_pengaduan", nullable = false)
	public JenisPengaduan getJenisPengaduan() {
		jenisPengaduan = check(jenisPengaduan);
		return jenisPengaduan;
	}

	/**
	 * Menyetel master jenis/kategori pengaduan.
	 *
	 * @param jenisPengaduan jenis pengaduan yang dipilih
	 */
	public void setJenisPengaduan(JenisPengaduan jenisPengaduan) {
		this.jenisPengaduan = jenisPengaduan;
	}

	/**
	 * Mengembalikan jawaban parameter tambahan dalam <b>versi id</b> (kolom {@code text}).
	 *
	 * <p>Format: satu baris per parameter, dipisah {@code \n}, tiap baris berbentuk</p>
	 * <pre>{@code kelompokId->parameterId<=>nilai<=>urlLampiran<=>keterangan}</pre>
	 * <p>Versi inilah yang dibaca ulang saat formulir dibuka kembali (lihat {@code
	 * ParameterTambahanPengaduanListener} dan {@code PengaduanAction.PengaduanRenderer}), karena
	 * pencocokannya memakai id sehingga tahan terhadap perubahan label.</p>
	 *
	 * <p><b>Getter ini menulis balik:</b> {@code null} diubah menjadi string kosong pada field,
	 * dan karena kolomnya terpetakan, perubahan itu ikut tersimpan saat flush.</p>
	 *
	 * @return jawaban parameter tambahan versi id; tidak pernah {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getParameterTambahanInds() {
		if (parameterTambahanInds == null) {
			parameterTambahanInds = "";
		}

		return parameterTambahanInds;
	}

	/**
	 * Menyetel jawaban parameter tambahan versi id.
	 *
	 * @param parameterTambahanInds teks banyak baris berformat
	 *        {@code kelompokId->parameterId<=>nilai<=>urlLampiran<=>keterangan}
	 * @see #getParameterTambahanInds()
	 */
	public void setParameterTambahanInds(String parameterTambahanInds) {
		this.parameterTambahanInds = parameterTambahanInds;
	}

	/**
	 * Mengurai {@link #getParameterTambahan()} (versi label manusia) menjadi daftar {@link CommonVO}
	 * yang siap ditampilkan atau direkap.
	 *
	 * <p>Setiap baris dipecah dengan pemisah {@code <=>} dan dipetakan sebagai berikut:</p>
	 * <ul>
	 *   <li>potongan ke-0 &rarr; {@code name} — utuh berbentuk {@code "namaKelompok->labelInputan"};
	 *       bagian sebelum {@code ->} juga disalin ke {@code name5} sebagai kunci pengelompokan</li>
	 *   <li>potongan ke-1 &rarr; {@code name1} — nilai jawaban</li>
	 *   <li>potongan ke-2 &rarr; {@code name2} — URL lampiran (kosong bila parameter tidak
	 *       mewajibkan lampiran)</li>
	 *   <li>potongan ke-3 &rarr; {@code nomorUrut} — gagal urai jatuh ke {@code 1}</li>
	 *   <li>potongan ke-4 &rarr; {@code id} — id {@code ParameterTambahan}; gagal urai jatuh ke
	 *       {@code 1}</li>
	 * </ul>
	 *
	 * <p>Potongan ke-5 (id kelompok) dan ke-6 (keterangan) yang ditulis {@link
	 * #populateParameterTambahan(List)} <b>tidak dibaca</b> di sini. Hasilnya diurutkan memakai
	 * {@link CommonVO#compareTo(CommonVO)}, yang karena {@code name5} terisi akan mengurutkan
	 * berdasarkan {@code name5 + " " + nomorUrut} sebagai teks — jadi urutannya leksikografis per
	 * kelompok, bukan numerik.</p>
	 *
	 * <p><b>Kuirk:</b> bila {@code parameterTambahan} kosong, {@code "".split("\n")} tetap
	 * menghasilkan satu elemen kosong, sehingga method ini mengembalikan daftar berisi <b>satu
	 * {@link CommonVO} kosong</b> (id {@code "1"}, seluruh nama kosong), bukan daftar kosong.
	 * Kedua blok {@code try} penangkap kegagalan {@code parseInt}/{@code parseLong} sengaja
	 * senyap dan sudah ditandai audit blok-catch-kosong.</p>
	 *
	 * <p><b>Catatan pemakaian:</b> di seluruh sumber, method bernama sama dipanggil untuk entity
	 * lain ({@code IsiAngketParameterUmum}, {@code KegiatanSiswa}) oleh layar dasbor rekap; salinan
	 * milik {@code Pengaduan} ini <b>tidak dipanggil dari mana pun</b> saat ini — layar pengaduan
	 * membaca versi id lewat {@link #getParameterTambahanInds()}. Tetap dipertahankan karena
	 * bentuknya seragam dengan entity sekeluarga dan dipakai bila rekap pengaduan ditambahkan.</p>
	 *
	 * @return daftar {@link CommonVO} terurut hasil penguraian; tidak pernah {@code null}, minimal
	 *         berisi satu elemen (lihat kuirk di atas)
	 * @see #populateParameterTambahan(List)
	 */
	public List<CommonVO> ambilDataParameterTambahan() {
		List<CommonVO> commonVOs = new ArrayList<CommonVO>();
		String[] splNama = getParameterTambahan().split("\n");
		for (int j = 0; j < splNama.length; j++) {
			CommonVO commonVO = new CommonVO();
			String namaCol = splNama.length > j ? splNama[j] : "";

			String[] value = namaCol.split("<=>");
			String lbl = value.length > 0 ? value[0].trim() : "";
			String url = value.length > 2 ? value[2].trim() : "";
			String val = value.length > 1 ? value[1].trim() : "";
			Integer nomorUrut = 1;
			try {
				nomorUrut = value.length > 3 ? Integer.parseInt(value[3].trim()) : 1;
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pengaduan.java:209");

			}
			Long id = 1L;
			try {
				id = value.length > 4 ? Long.parseLong(value[4].trim()) : 1L;
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pengaduan.java:215");

			}

			// System.out.println("namaCol=> " + namaCol + ", lbl=> " + lbl + ", val=> " +
			// val + ", url=>" + url);

			String[] param = lbl.split("->");

			commonVO.setId(id.toString());
			commonVO.setName(lbl);
			commonVO.setName1(val);
			commonVO.setName2(url);
			commonVO.setName5(param[0]);
			commonVO.setNomorUrut(nomorUrut);
			commonVOs.add(commonVO);
		}
		Collections.sort(commonVOs);
		return commonVOs;
	}

	/**
	 * Memanen nilai seluruh field parameter tambahan yang sedang tampil di formulir ZK, lalu
	 * menuliskannya kembali ke kedua kolom teks entity ini.
	 *
	 * <p>Dipanggil dari {@code ais.action.master.helper.ParameterTambahanPengaduanListener} — sekali
	 * setiap kali salah satu input parameter berubah, dan sekali lagi lewat {@code onSave(Pengaduan)}
	 * tepat sebelum pengaduan disimpan. Karena itu method ini boleh dijalankan berkali-kali dan
	 * selalu membangun ulang isi kolom dari nol (tidak menambah di belakang isi lama).</p>
	 *
	 * <p>Setiap {@link Row} yang diperiksa harus membawa dua atribut yang dipasang pembuat formulir:
	 * {@code "parameterTambahan"} ({@link ParameterTambahan}) dan {@code
	 * "kelompokParameterTambahanPengaduan"} ({@link KelompokParameterTambahanPengaduan}); baris yang
	 * salah satunya kosong dilewati diam-diam. Nilai jawaban diambil lewat {@code
	 * ParameterTambahan.ambilVal(row, parameterTambahan)}, sedangkan catatan tambahan diambil dari
	 * atribut {@code "keterangan"} bila berupa {@link Textbox}.</p>
	 *
	 * <p>Untuk parameter yang {@code getHarusMenyertakanLampiran()}-nya benar, method mencari
	 * {@link LampiranLain} dengan referensi {@link #getId()} dan jenis {@code
	 * "kelompokId->parameterId"}, lalu menyimpan tautan unduhnya. <b>Kuirk penting:</b> pada
	 * pengaduan yang <i>belum pernah disimpan</i>, {@link #getId()} masih {@code null} sehingga
	 * lampiran tidak ketemu dan URL yang tersimpan kosong; URL baru terisi pada penyuntingan
	 * berikutnya.</p>
	 *
	 * <p>Dua kolom dihasilkan sekaligus, dengan {@code \n} sebagai pemisah baris:</p>
	 * <ul>
	 *   <li>{@link #setParameterTambahan(String)} — versi label manusia:
	 *       <pre>{@code namaKelompok->labelInputan<=>nilai<=>url<=>nomorUrut<=>parameterId<=>kelompokId<=>keterangan}</pre></li>
	 *   <li>{@link #setParameterTambahanInds(String)} — versi id:
	 *       <pre>{@code kelompokId->parameterId<=>nilai<=>url<=>keterangan}</pre></li>
	 * </ul>
	 *
	 * <p><b>Efek samping:</b> hanya kedua setter di atas; tidak menyentuh basis data selain
	 * pembacaan {@link LampiranLain}. Seluruh kegagalan per baris ditelan dan hanya ditampilkan
	 * kepada admin lewat {@code Common.tampilErrorJikaAdmin(e)}, sehingga satu baris rusak tidak
	 * membatalkan pemanenan baris lain — konsekuensinya kegagalan diam-diam bisa membuat jawaban
	 * suatu parameter hilang tanpa peringatan bagi pengguna biasa.</p>
	 *
	 * @param parameterRows daftar baris ZK berisi input parameter tambahan; bila {@code null} atau
	 *        kosong method langsung keluar tanpa mengubah apa pun (isi kolom lama dipertahankan)
	 * @see #getParameterTambahan()
	 * @see #getParameterTambahanInds()
	 * @see #ambilDataParameterTambahan()
	 */
	public void populateParameterTambahan(List<Row> parameterRows) {
		if (parameterRows == null || parameterRows.isEmpty()) {
			return;
		}

		String parameterTambahanStr = "";
		String parameterTambahanInds = "";
		for (Row row : parameterRows) {
			try {
				ParameterTambahan parameterTambahan = (ParameterTambahan) row.getAttribute("parameterTambahan");
				KelompokParameterTambahanPengaduan kelompokParameterTambahanPengaduan = (KelompokParameterTambahanPengaduan) row
						.getAttribute("kelompokParameterTambahanPengaduan");
				if (parameterTambahan != null && kelompokParameterTambahanPengaduan != null) {
					String jenis = LampiranLain.resolveJenisParameterTambahan(Pengaduan.class, getId(),
							kelompokParameterTambahanPengaduan.getId() + "->" + parameterTambahan.getId());

					String val = ParameterTambahan.ambilVal(row, parameterTambahan);
					Textbox keterangan = (Textbox) ((row.getAttribute("keterangan") != null
							&& row.getAttribute("keterangan") instanceof Textbox) ? row.getAttribute("keterangan")
									: null);
					String url = "";
					if (parameterTambahan.getHarusMenyertakanLampiran()) {

						LampiranLain lam = LampiranLain.ambil(getId(), jenis);
						if (lam != null) {
							try {
								url = lam.createLinkUri();
							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
							}
						}

					}

					String s = kelompokParameterTambahanPengaduan.getNama() + "->" + parameterTambahan.getLabelInputan()
							+ "<=>" + val + "<=>" + url + "<=>" + parameterTambahan.getNomorUrut() + "<=>"
							+ parameterTambahan.getId() + "<=>" + kelompokParameterTambahanPengaduan.getId() + "<=>"
							+ (keterangan == null ? "" : keterangan.getValue().trim());

					parameterTambahanStr += parameterTambahanStr.isEmpty() ? s : "\n" + s;

					String sIds = kelompokParameterTambahanPengaduan.getId() + "->" + parameterTambahan.getId() + "<=>"
							+ val + "<=>" + url + "<=>" + (keterangan == null ? "" : keterangan.getValue().trim());

					parameterTambahanInds += parameterTambahanInds.isEmpty() ? sIds : "\n" + sIds;
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}
		// System.out.println("parameterTambahanStr => " + parameterTambahanStr);
		// System.out.println("parameterTambahanInds => " + parameterTambahanInds);
		setParameterTambahanInds(parameterTambahanInds);
		setParameterTambahan(parameterTambahanStr);
	}

	/**
	 * Mengembalikan jawaban parameter tambahan dalam <b>versi label manusia</b> (kolom {@code text}).
	 *
	 * <p>Format: satu baris per parameter, dipisah {@code \n}, tiap baris berbentuk</p>
	 * <pre>{@code namaKelompok->labelInputan<=>nilai<=>url<=>nomorUrut<=>parameterId<=>kelompokId<=>keterangan}</pre>
	 * <p>Versi ini dipakai untuk pencetakan/rekap (lihat {@link #ambilDataParameterTambahan()});
	 * untuk memuat ulang formulir dipakai {@link #getParameterTambahanInds()} karena label bisa
	 * berubah sedangkan id tidak.</p>
	 *
	 * <p><b>Getter ini menulis balik:</b> {@code null} diubah menjadi string kosong pada field, dan
	 * perubahan itu ikut tersimpan saat flush.</p>
	 *
	 * @return jawaban parameter tambahan versi label; tidak pernah {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getParameterTambahan() {
		if (parameterTambahan == null) {
			parameterTambahan = "";
		}

		return parameterTambahan;
	}

	/**
	 * Menyetel jawaban parameter tambahan versi label manusia.
	 *
	 * @param parameterTambahan teks banyak baris berformat
	 *        {@code namaKelompok->labelInputan<=>nilai<=>url<=>nomorUrut<=>parameterId<=>kelompokId<=>keterangan}
	 * @see #getParameterTambahan()
	 */
	public void setParameterTambahan(String parameterTambahan) {
		this.parameterTambahan = parameterTambahan;
	}

	/**
	 * Mengembalikan simpul disposisi SOP yang sedang/terakhir menangani pengaduan ini —
	 * implementasi kontrak abstrak {@link DataSop#getDisposisiSop()}.
	 *
	 * <p>Relasi lazy; getter menugaskan kembali hasil {@link GeneralValueObject#check(Object)} ke
	 * field. Object inilah sumber kebenaran status penanganan: {@link #getSetujui()} dan {@link
	 * #getAktif()} sama-sama menurunkan nilainya dari sini.</p>
	 *
	 * @return simpul disposisi SOP, atau {@code null} bila pengaduan belum masuk alur SOP (kondisi
	 *         normal untuk aduan yang baru dibuat, termasuk semua aduan dari WhatsApp)
	 * @see DataSop
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	/**
	 * Menyetel simpul disposisi SOP — implementasi kontrak abstrak {@link
	 * DataSop#setDisposisiSop(DisposisiSop)}.
	 *
	 * <p><b>Bersifat menjaga, bukan sekadar menugaskan.</b> Argumen {@code null} atau yang
	 * {@code getId()}-nya {@code null} (disposisi yang belum tersimpan) <b>diabaikan diam-diam</b>
	 * lewat penjagaan di awal method, sehingga tautan SOP yang sudah terpasang tidak bisa hilang
	 * karena {@code merge} dari form yang tidak membawa kolom ini. Ekspresi ternary pada baris
	 * berikutnya adalah sisa penjagaan yang sama dan tidak pernah lagi bernilai selain
	 * {@code disposisiSop} karena kasus {@code null} sudah tersaring lebih dulu; dibiarkan apa
	 * adanya (tidak ada perubahan logika pada pekerjaan dokumentasi ini).</p>
	 *
	 * @param disposisiSop simpul disposisi baru; diabaikan bila {@code null} atau belum punya id
	 */
	public void setDisposisiSop(DisposisiSop disposisiSop) {if(disposisiSop==null||disposisiSop.getId()==null) {return;}
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null)) ? this.disposisiSop : disposisiSop;
	}

	/**
	 * Mengembalikan tahun agenda pengaduan.
	 *
	 * <p><b>Getter ini menulis balik:</b> bila field masih {@code null}, diisi tahun berjalan dari
	 * {@link ais.ui.util.WaktuUtil#getCalendar()} dan nilai itu ikut tersimpan saat flush. Kolom ini
	 * bukan hiasan — {@code PengaduanAction#getindex(JenisPengaduan)} memakainya sebagai penyaring
	 * saat {@code NomorSurat} disetel "reset urutan tiap tahun".</p>
	 *
	 * <p>Perhatikan bahwa nilainya diambil dari <b>jam saat getter pertama kali dipanggil</b>, bukan
	 * dari {@link #getWaktu()}. Untuk data yang diimpor mundur, keduanya bisa tidak sinkron.</p>
	 *
	 * @return tahun agenda; tidak pernah {@code null}
	 */
	public Integer getTahun() {
		if (tahun == null) {
			tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		}
		return tahun;
	}

	/**
	 * Menyetel tahun agenda pengaduan.
	 *
	 * @param tahun tahun agenda
	 */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/**
	 * Mengembalikan bulan agenda pengaduan dalam rentang <b>1-12</b> (sudah ditambah satu dari
	 * {@link Calendar#MONTH} yang berbasis nol).
	 *
	 * <p><b>Getter ini menulis balik</b>, persis seperti {@link #getTahun()}, dan nilainya dipakai
	 * bersama {@code tahun} sebagai penyaring saat {@code NomorSurat} disetel "reset urutan tiap
	 * bulan".</p>
	 *
	 * @return bulan agenda 1-12; tidak pernah {@code null}
	 */
	public Integer getBulan() {
		if (bulan == null) {
			bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		}
		return bulan;
	}

	/**
	 * Menyetel bulan agenda pengaduan.
	 *
	 * @param bulan bulan agenda dalam rentang 1-12
	 */
	public void setBulan(Integer bulan) {
		this.bulan = bulan;
	}

	/**
	 * Mengembalikan pegawai yang terkait pengaduan ini — dalam praktiknya <b>pegawai pelapor</b>.
	 *
	 * <p><b>Getter paling berefek samping di class ini.</b> Urutannya: (1) resolusi proxy lazy lewat
	 * {@link GeneralValueObject#check(Object)}; (2) field {@code diajukan} <i>ditulis ulang</i>
	 * dengan hasil {@link #getDiajukan()}; (3) bila akun pengaju punya {@link Pegawai}, field
	 * {@code pegawai} <b>ditimpa</b> dengan pegawai milik akun itu — menimpa apa pun yang sudah
	 * disetel operator lewat {@link #setPegawai(Pegawai)}. Karena {@code pegawai} adalah kolom
	 * terpetakan sungguhan, penimpaan ini ikut ter-{@code UPDATE} permanen ke basis data saat entity
	 * di-flush.</p>
	 *
	 * <p><b>Konsekuensi yang perlu disadari:</b> selama akun pengaju terhubung ke seorang pegawai,
	 * kolom {@code pegawai} tidak dapat dipakai untuk mencatat pegawai lain (mis. pegawai yang
	 * ditunjuk sebagai penanggung jawab, atau pegawai yang diadukan) — nilainya akan kembali ke
	 * pegawai pelapor pada pembacaan berikutnya. Kolom ini juga menjadi dasar penyaluran
	 * persetujuan: {@code PengaduanAction} menampilkan kendali "setujui" kepada pemilik akun yang
	 * menjadi {@code atasanlangsung}/{@code atasanlangsung2}/{@code atasanlangsung3} pegawai ini,
	 * sehingga aduan seorang pegawai otomatis mendarat di meja atasan langsungnya. Lihat bagian
	 * "Catatan privasi" pada Javadoc class.</p>
	 *
	 * @return pegawai pelapor, atau {@code null} bila pelapor bukan pegawai
	 * @see #getDiajukan()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai")
	public Pegawai getPegawai() {
		pegawai = check(pegawai);
		diajukan = getDiajukan();

		if (diajukan != null && diajukan.getPegawai() != null) {
			pegawai = diajukan.getPegawai();
		}

		return pegawai;
	}

	/**
	 * Menyetel pegawai yang terkait pengaduan.
	 *
	 * <p><b>Nilai yang disetel di sini tidak selalu bertahan:</b> {@link #getPegawai()} akan
	 * menimpanya dengan pegawai milik {@link #getDiajukan()} bila akun pengaju punya pegawai.</p>
	 *
	 * @param pegawai pegawai pelapor
	 */
	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	/**
	 * Mengembalikan akun pengguna yang menyetujui pengaduan ini.
	 *
	 * <p>Relasi lazy dengan resolusi proxy lewat {@link GeneralValueObject#check(Object)}.
	 * Ditampilkan pada baris daftar sebagai "Disetujui oleh : ...". Perhatikan ejaan field
	 * penyimpannya, {@code disetujiOleh} (tanpa "u"), yang berbeda dari nama method {@code
	 * getDisetujuiOleh}; ejaan itu sengaja dipertahankan agar nama kolom hasil pemetaan tidak
	 * berubah.</p>
	 *
	 * @return akun penyetuju, atau {@code null} bila belum ada yang menyetujui
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disetuji_oleh", nullable = true)
	public Tbmuser getDisetujuiOleh() {
		disetujiOleh = check(disetujiOleh);
		return disetujiOleh;
	}

	/**
	 * Menyetel akun pengguna yang menyetujui pengaduan.
	 *
	 * @param disetujiOleh akun penyetuju
	 */
	public void setDisetujuiOleh(Tbmuser disetujiOleh) {
		this.disetujiOleh = disetujiOleh;
	}

	/**
	 * Mengembalikan tanggal persetujuan pengaduan.
	 *
	 * <p>Salah satu dari sedikit getter murni di class ini: tidak ada nilai pengganti dan tidak ada
	 * penulisan balik. {@code null} berarti belum pernah disetujui.</p>
	 *
	 * @return tanggal persetujuan, atau {@code null}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getSetujuiTanggal() {
		return setujuiTanggal;
	}

	/**
	 * Menyetel tanggal persetujuan pengaduan.
	 *
	 * @param setujuiTanggal tanggal persetujuan
	 */
	public void setSetujuiTanggal(Date setujuiTanggal) {
		this.setujuiTanggal = setujuiTanggal;
	}

	/**
	 * Mengembalikan penanda apakah pengaduan sudah disetujui/ditutup.
	 *
	 * <p><b>Getter ini menulis balik dan menurunkan nilainya dari SOP:</b> bila {@link
	 * #getDisposisiSop()} terisi, field {@code setujui} <b>selalu</b> ditimpa dengan hasil
	 * {@code disposisiSop.getDisposisiSetuju() != null} — jadi nilai yang disetel manual lewat
	 * {@link #setSetujui(Boolean)} akan tergusur begitu pengaduan sudah masuk alur SOP, termasuk
	 * tergusur kembali menjadi {@code false} bila simpul setujunya dibatalkan. Penimpaan ini ikut
	 * tersimpan permanen karena kolomnya terpetakan.</p>
	 *
	 * <p>Selama {@code disposisiSop} masih {@code null} (aduan baru, aduan WhatsApp), nilai manual
	 * dipertahankan dan {@code null} dibaca sebagai {@code false}.</p>
	 *
	 * <p>Dipakai sebagai kunci pengaman di {@code PengaduanMahasiswaApi}: aduan yang sudah
	 * disetujui tidak boleh lagi diubah maupun dihapus oleh pelapornya.</p>
	 *
	 * @return {@code true} bila sudah disetujui; tidak pernah {@code null}
	 */
	public Boolean getSetujui() {
		if (getDisposisiSop() != null) {
			setujui = getDisposisiSop().getDisposisiSetuju() != null;
		}
		return setujui == null ? false : setujui;
	}

	/**
	 * Menyetel penanda persetujuan secara manual.
	 *
	 * <p>Hanya berpengaruh selama pengaduan belum punya {@link DisposisiSop}; setelah itu {@link
	 * #getSetujui()} menimpanya dari SOP.</p>
	 *
	 * @param setujui penanda persetujuan
	 */
	public void setSetujui(Boolean setujui) {
		this.setujui = setujui;
	}

	/**
	 * Mengembalikan mahasiswa pelapor.
	 *
	 * <p>Relasi lazy dengan resolusi proxy lewat {@link GeneralValueObject#check(Object)}. Diisi
	 * pada jalur layar dan jalur API mobile ({@code PengaduanMahasiswaApi} mengisinya dari akun yang
	 * sedang login dan memakainya sebagai penyaring kepemilikan bersama {@code diajukan}).</p>
	 *
	 * @return mahasiswa pelapor, atau {@code null} bila pelapor bukan mahasiswa
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa")
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);
		return mahasiswa;
	}

	/**
	 * Menyetel mahasiswa pelapor.
	 *
	 * @param mahasiswa mahasiswa pelapor
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * Mengembalikan siswa pelapor (jenjang sekolah).
	 *
	 * <p>Relasi lazy dengan resolusi proxy lewat {@link GeneralValueObject#check(Object)}. Setara
	 * dengan {@link #getMahasiswa()} untuk institusi berjenjang sekolah; keduanya bisa sama-sama
	 * {@code null} bila pelapor hanya berupa akun/pegawai.</p>
	 *
	 * @return siswa pelapor, atau {@code null} bila pelapor bukan siswa
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa")
	public Siswa getSiswa() {
		siswa = check(siswa);
		return siswa;
	}

	/**
	 * Menyetel siswa pelapor.
	 *
	 * @param siswa siswa pelapor
	 */
	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
	}

	/**
	 * Mengembalikan penanda apakah baris pengaduan ini masih berlaku.
	 *
	 * <p><b>Getter ini menulis balik dan bersifat satu arah ke {@code false}.</b> Dua kondisi
	 * memaksa {@code aktif = false}, dan tidak ada satu pun jalur di dalam class ini yang
	 * mengembalikannya ke {@code true}:</p>
	 * <ol>
	 *   <li>{@link DisposisiSop} yang menangani sudah tidak aktif; atau</li>
	 *   <li>disposisi berhenti pada simpul akhir yang alur SOP-nya ditandai {@code
	 *       getPenolakanAdaDiSini()} — artinya pengaduan <b>ditolak</b>.</li>
	 * </ol>
	 * <p>Karena {@code aktif} kolom terpetakan sungguhan, sekali salah satu kondisi terpenuhi,
	 * nilai {@code false} tertulis permanen ke basis data pada flush berikutnya; memulihkannya harus
	 * lewat {@link #setAktif(Boolean)} dari luar <i>dan</i> memastikan kedua kondisi di atas sudah
	 * tidak berlaku, kalau tidak akan langsung dimatikan lagi pada pembacaan berikutnya. Pola
	 * "flag aktif satu arah" ini sama dengan yang terdokumentasi pada entity akunting keluarga
	 * {@code DataSop} lainnya.</p>
	 *
	 * <p>Perhatikan juga bahwa nilai bawaan untuk {@code null} adalah <b>{@code true}</b> — baris
	 * lama yang kolomnya kosong dianggap masih berlaku. {@code PengaduanMahasiswaApi} memanfaatkan
	 * ini: penyaring daftarnya menerima {@code aktif IS NULL OR aktif = true}, dan "hapus" hanya
	 * menyetel {@code false} (<i>soft delete</i>).</p>
	 *
	 * @return {@code true} bila baris masih berlaku; tidak pernah {@code null}
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
	 * Menyetel penanda baris masih berlaku.
	 *
	 * <p>Menyetel {@code false} adalah cara resmi menghapus pengaduan (<i>soft delete</i>);
	 * menyetel {@code true} hanya bertahan bila kedua kondisi pemaksa di {@link #getAktif()} tidak
	 * terpenuhi.</p>
	 *
	 * @param aktif penanda berlaku
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan akun pengguna yang mengajukan pengaduan ini.
	 *
	 * <p>Ini <b>identitas pelapor yang paling dasar</b> dan satu-satunya yang terisi pada jalur
	 * WhatsApp — di sana {@code Wa#simpanPesan} membuat {@link Tbmuser} baru berperan {@code
	 * "pengadu"} dengan {@code userId} berupa nomor telepon pengirim bila nomor itu belum dikenal.
	 * {@code PengaduanMahasiswaApi} memakainya bersama {@code mahasiswa} sebagai penyaring
	 * kepemilikan, dan {@code PengaduanAction#initCriteria} membatasi daftar pengguna non-admin
	 * dengan {@code diajukan = pengguna aktif}.</p>
	 *
	 * <p><b>Berbeda dari relasi lazy lain di class ini, getter ini TIDAK memanggil {@code check()}
	 * dan tidak menulis balik</b> — ia mengembalikan field apa adanya. Karena relasinya {@code
	 * FetchType.LAZY}, pemanggilan di luar session yang mengelola entity dapat mengembalikan proxy
	 * yang belum terinisialisasi. Perhatikan bahwa {@link #getPegawai()} justru memanggil getter ini
	 * dan menugaskan hasilnya kembali ke field.</p>
	 *
	 * @return akun pengaju, atau {@code null} bila pengaduan dibuat tanpa akun
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "diajukan", nullable = true)
	public Tbmuser getDiajukan() {
		diajukan = check(diajukan);
		return diajukan;
	}

	/**
	 * Menyetel akun pengguna yang mengajukan pengaduan.
	 *
	 * @param diajukan akun pengaju
	 */
	public void setDiajukan(Tbmuser diajukan) {
		this.diajukan = diajukan;
	}

	/**
	 * Mengembalikan tanggapan/balasan atas pengaduan (kolom {@code text}).
	 *
	 * <p>Diisi petugas lewat kotak isian di layar disposisi, atau diisi otomatis oleh chatbot
	 * WhatsApp dengan teks balasan yang benar-benar dikirim ke pelapor. Mengembalikan <b>string
	 * kosong</b>, bukan {@code null}, bila belum ada tanggapan — supaya pemanggil di layar bisa
	 * langsung memakai {@code .trim().isEmpty()}. Nilai pengganti itu tidak ditulis balik ke
	 * field.</p>
	 *
	 * @return teks tanggapan, atau string kosong bila belum ada
	 */
	@Column(name = "tanggapan", nullable = true, columnDefinition = "text")
	public String getTanggapan() {
		return tanggapan == null ? "" : tanggapan;
	}

	/**
	 * Menyetel tanggapan/balasan atas pengaduan.
	 *
	 * @param tanggapan teks tanggapan
	 */
	public void setTanggapan(String tanggapan) {
		this.tanggapan = tanggapan;
	}

	/**
	 * Mengembalikan <b>payload permintaan mentah</b> dari integrasi WhatsApp (kolom {@code text}).
	 *
	 * <p>Diisi hanya pada jalur webhook: {@code Wa#simpanPesan} menyimpan seluruh badan JSON yang
	 * dikirim Meta ke sini apa adanya. Isinya karena itu memuat <b>nomor telepon dan nama profil
	 * pengirim tanpa penyamaran</b>, di samping metadata nomor bisnis penerima. Pada pengaduan yang
	 * dibuat lewat layar ZK atau API mobile kolom ini tetap {@code null}.</p>
	 *
	 * <p><b>Catatan privasi:</b> kolom ini (bersama {@link #getRes()}) termasuk dalam daftar kolom
	 * ekspor/impor massal di {@code PengaduanAction}, sehingga ikut terbawa saat pengguna menekan
	 * tombol Cetak/Upload data pada layar pengaduan. Tidak ada penyaringan atau penyamaran isi.</p>
	 *
	 * <p>Berbeda dari getter teks lain di class ini, getter ini mengembalikan field apa adanya —
	 * bisa {@code null}.</p>
	 *
	 * @return payload webhook mentah, atau {@code null} bila pengaduan bukan berasal dari WhatsApp
	 */
	@Column(name = "req", nullable = true, columnDefinition = "text")
	public String getReq() {
		return req;
	}

	/**
	 * Menyetel payload permintaan mentah dari integrasi WhatsApp.
	 *
	 * @param req badan JSON webhook apa adanya
	 * @see #getReq()
	 */
	public void setReq(String req) {
		this.req = req;
	}

	/**
	 * Mengembalikan <b>respons mentah</b> pengiriman balasan ke WhatsApp (kolom {@code text}).
	 *
	 * <p>Diisi hanya pada jalur webhook, pada utas terpisah setelah bot benar-benar mengirim
	 * jawaban: isinya keluaran mentah panggilan {@code curl} ke Graph API Meta (identifikasi pesan
	 * bila berhasil, atau badan galat bila gagal). Bila pengiriman balasan dimatikan lewat
	 * konfigurasi, yang tersimpan adalah teks penanda bahwa pesan tidak dikirimkan.</p>
	 *
	 * <p>Sama seperti {@link #getReq()}, kolom ini ikut dalam ekspor/impor massal; lihat catatan
	 * privasi di sana. Mengembalikan field apa adanya — bisa {@code null}.</p>
	 *
	 * @return respons Graph API mentah, atau {@code null} bila belum/tidak pernah ada balasan
	 */
	@Column(name = "res", nullable = true, columnDefinition = "text")
	public String getRes() {
		return res;
	}

	/**
	 * Menyetel respons mentah pengiriman balasan ke WhatsApp.
	 *
	 * @param res keluaran mentah panggilan Graph API
	 * @see #getRes()
	 */
	public void setRes(String res) {
		this.res = res;
	}
}
