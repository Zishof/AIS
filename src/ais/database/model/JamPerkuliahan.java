package ais.database.model;

// Generated Apr 12, 2010 11:30:55 AM by Hibernate Tools 3.2.4.CR1

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

import ais.common.Common;

/**
 * Entity <b>master slot jam perkuliahan</b> — satu baris mewakili satu "jam ke-N" pada papan
 * jadwal, misalnya <i>"Jam 1"</i> mulai {@code 07.00} sampai {@code 08.40}. Dipetakan ke tabel
 * {@code public.jam_perkuliahan} (kelas ini memang benar-benar bernama {@code JamPerkuliahan},
 * jadi komentar generator hbm2java di atas <b>tepat</b> untuk berkas ini — lihat catatan
 * "Komentar generator" di bawah).
 *
 * <p>Master ini adalah kamus waktu yang dipakai ulang oleh penjadwalan kuliah: object jadwal
 * ({@link Perkuliahan}, {@link TemplatePerkuliahanDetail}) hanya menyimpan <i>referensi</i> ke
 * salah satu slot di sini, sehingga perubahan jam di satu tempat langsung terlihat di semua
 * kelas yang memakainya.</p>
 *
 * <h2>Yang TIDAK ada di entity ini (sering disalahpahami)</h2>
 * <ul>
 *   <li><b>Tidak ada properti hari.</b> Slot ini murni rentang jam; harinya disimpan di sisi
 *   jadwal ({@code Perkuliahan.hari}), bukan di sini. Satu baris {@code JamPerkuliahan} berlaku
 *   untuk semua hari.</li>
 *   <li><b>Tidak ada properti urutan/nomor slot.</b> Urutan slot tidak disimpan sebagai angka,
 *   melainkan <i>diturunkan</i> dari waktu: setiap pemakai mengurutkan dengan
 *   {@code Order.asc("mulai")} lalu {@code Order.asc("sampai")}. Nomor slot hanya hidup di dalam
 *   teks {@link #getNama() nama} (mis. "Jam ke 1"), sehingga urutan yang tampil bisa berbeda dari
 *   penomoran di nama bila jam mulai disunting.</li>
 *   <li><b>Tidak ada relasi ke tahun akademik/semester.</b> Slot bersifat abadi lintas semester.</li>
 * </ul>
 *
 * <h2>Ruang lingkup slot: fakultas / prodi / program</h2>
 * <p>Tiga properti membatasi berlakunya sebuah slot: {@link #getFakultas() fakultas},
 * {@link #getJurusan() jurusan/prodi}, dan {@link #getProgram() program} (mis. Reguler/Karyawan).
 * Ketiganya <b>boleh kosong</b>, dan kosong berarti <i>berlaku umum</i>. Karena itu semua penyaring
 * pemakai selalu berbentuk {@code Restrictions.or(Restrictions.isNull("jurusan"), eq(...))} —
 * baris tanpa prodi tetap muncul untuk prodi mana pun (lihat
 * {@code AmbilDataJamPerkuliahanBanbox} dan {@code JamPerkuliahanAction.initCriteria}).</p>
 * <p>Perhatikan bahwa {@code fakultas} bukan data mandiri: {@link #getFakultas()} akan
 * <b>menimpanya</b> dari {@code jurusan.getFakultas()} setiap kali dipanggil bila prodi terisi.
 * Baca javadoc method tersebut sebelum mengandalkan isi kolom {@code fakultas} di database.</p>
 *
 * <h2>Dua representasi waktu yang sama-sama disimpan</h2>
 * <p>Ini kekhasan terpenting entity ini. Rentang jam disimpan <b>dua kali</b> dalam kolom yang
 * berbeda:</p>
 * <ol>
 *   <li>{@link #getMulai() mulai} / {@link #getSampai() sampai} — kolom {@code mulai} dan
 *   {@code sampai} bertipe {@link TemporalType#TIME} (hanya jam-menit-detik, tanggalnya tidak
 *   bermakna).</li>
 *   <li>{@link #getWaktuMulai() waktuMulai} / {@link #getWaktuSelesai() waktuSelesai} — kolom teks
 *   {@code waktu_mulai} dan {@code waktu_selesai} ({@code varchar(20)}) berisi hasil format
 *   {@code Common.timeFormat2} yaitu pola <b>{@code "HH.mm"}</b> (pemisah <b>titik</b>, bukan
 *   titik dua), mis. {@code "07.00"}.</li>
 * </ol>
 * <p>Keduanya dijaga sinkron oleh setter/getter, bukan oleh basis data:
 * {@link #setMulai(Date)} menulis ulang {@code waktuMulai}, {@link #setWaktuMulai(String)} mengurai
 * balik ke {@code mulai}, dan {@link #getWaktuMulai()} mengisi teks yang masih kosong dari
 * {@code mulai}. Sinkronisasi ini <b>tidak dijamin</b> pada dua kondisi: (a) penguraian teks gagal
 * — exception ditelan, {@code mulai} tetap nilai lama sementara teks tersimpan apa adanya; dan
 * (b) baris yang diubah lewat SQL langsung. Bila keduanya berbeda, pemakai yang berbeda akan
 * membaca jam yang berbeda pula — {@code TimetablePerkuliahanWindow.matchJam(...)} mencocokkan
 * memakai <b>teks</b> {@code waktuMulai} ({@code String.equals}), sedangkan formulir penjadwalan
 * ({@code PenjadwalanUtil}) mengisi kotak waktu dari <b>{@code Date}</b> {@code mulai}/
 * {@code sampai}.</p>
 *
 * <h2>Pengelompokan anggota</h2>
 * <ul>
 *   <li><b>Jejak audit</b> — {@link #getOleh()}/{@link #setOleh(String)},
 *   {@link #getOlehId()}/{@link #setOlehId(String)},
 *   {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, dan kait
 *   {@link #onUpdate()} ({@code @PreUpdate}). Entity juga ber-{@code @Audited} (Hibernate Envers),
 *   sehingga setiap perubahan direkam ke tabel revisi.</li>
 *   <li><b>Identitas</b> — {@link #getId()}/{@link #setId(Long)}, {@link #getNama()}/
 *   {@link #setNama(String)}, {@link #toString()}.</li>
 *   <li><b>Waktu</b> — {@link #getMulai()}/{@link #setMulai(Date)},
 *   {@link #getSampai()}/{@link #setSampai(Date)}, {@link #getWaktuMulai()}/
 *   {@link #setWaktuMulai(String)}, {@link #getWaktuSelesai()}/{@link #setWaktuSelesai(String)}.</li>
 *   <li><b>Ruang lingkup</b> — {@link #getFakultas()}/{@link #setFakultas(Fakultas)},
 *   {@link #getJurusan()}/{@link #setJurusan(Jurusan)}, {@link #getProgram()}/
 *   {@link #setProgram(String)}.</li>
 *   <li><b>Pelengkap</b> — {@link #getSks()}/{@link #setSks(Integer)},
 *   {@link #getKeterangan()}/{@link #setKeterangan(String)},
 *   {@link #getAktif()}/{@link #setAktif(Boolean)}.</li>
 * </ul>
 *
 * <h2>Pola pemakaian di aplikasi</h2>
 * <p>Sekitar 30 berkas menyebut kelas ini. Titik-titik utamanya:</p>
 * <ul>
 *   <li><b>{@link Perkuliahan}</b> — properti {@code jamPerkuliahan}. Bila terisi, jam master
 *   <b>menimpa</b> teks bebas: {@code Perkuliahan.getWaktuMulai()}/{@code getWaktuSelesai()}
 *   mengambil nilainya dari {@link #getWaktuMulai()}/{@link #getWaktuSelesai()} slot ini.</li>
 *   <li><b>{@link TemplatePerkuliahanDetail}</b> — cetakan jadwal yang nanti disalin menjadi
 *   {@code Perkuliahan}; menyimpan referensi slot yang sama (getter di sana <i>tidak</i>
 *   memanggil {@code check()}, berbeda dengan {@code Perkuliahan}).</li>
 *   <li><b>{@code ais.action.master.JamPerkuliahanAction}</b> — layar master CRUD "Jam
 *   Perkuliahan" (label kolom: Jam Perkuliahan, Mulai, Sampai, SKS, Fakultas, Prodi, Program,
 *   Keterangan). Penghapusan di sana <b>tidak</b> mengandalkan cascade: {@code onDelete(...)}
 *   menjalankan dua {@code UPDATE ... SET jam_perkuliahan = NULL} manual ke tabel
 *   {@code perkuliahan} dan {@code template_perkuliahan_detail} lebih dulu.</li>
 *   <li><b>{@code AmbilDataJamPerkuliahanBanbox}</b> — bandbox pemilih slot yang dipasang di
 *   {@code PenjadwalanUtil}, {@code CalendarPerkuliahanSemesterComposer}, dan
 *   {@code CalendarPerkuliahanDosenComposer}. Saat slot dipilih, kotak waktu mulai/selesai diisi
 *   dari {@link #getMulai()}/{@link #getSampai()} lalu <i>dikunci</i> (di-{@code disable}).
 *   Konfigurasi {@code jam_perkuliahan_wajib_dipilih} memaksa penguncian itu bahkan ketika slot
 *   belum dipilih.</li>
 *   <li><b>{@code TimetablePerkuliahanWindow}</b> — papan jadwal berbentuk grid: baris = slot
 *   jam hasil {@code loadJamList(...)} (dibatasi {@code setMaxResults(16)} pada jalur tanpa
 *   prodi), kolom = hari. Juga menyediakan pembuatan 10 slot bawaan untuk sebuah prodi.</li>
 *   <li><b>Cache</b> — {@code InitData} memuat kelas ini di awal aplikasi dan
 *   {@code DataUtil.CLASS_JANGAN_DIBERSIHKAN} melindunginya dari pembersihan cache, jadi
 *   instance-nya berumur panjang di memori (lihat catatan proxy lazy di bawah).</li>
 *   <li>Akses basis data generik lewat {@code JamPerkuliahanDao}/{@code JamPerkuliahanDaoImpl}
 *   yang tidak menambah method apa pun di atas {@code GenericDao}.</li>
 * </ul>
 *
 * <h2>Hubungan dengan {@link GeneralValueObject}</h2>
 * <p>Induknya adalah POJO abstrak biasa — <b>bukan</b> {@code @Entity} maupun
 * {@code @MappedSuperclass} — sehingga Hibernate <b>tidak</b> memetakan properti induk. Karena itu
 * {@code id}, {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} sengaja
 * <b>dideklarasikan ulang</b> di sini. Itu <b>bukan duplikasi keliru</b>, melainkan keharusan
 * teknis; menghapusnya membuat kolom-kolom tersebut hilang dari pemetaan.</p>
 * <p>Dari induk, entity ini memakai {@code check(...)} pada getter relasinya. Perlu diingat
 * {@code check()} bisa membuka <i>session</i> Hibernate baru sebagai upaya terakhir dan
 * menutupnya sendiri di {@code finally} — jadi getter relasi di sini tidak pernah "bocor"
 * session, tetapi juga tidak gratis.</p>
 *
 * <h2>Kuirk yang perlu diketahui sebelum menyunting</h2>
 * <ul>
 *   <li>{@link #getMulai()} dan {@link #getSampai()} <b>tidak pernah mengembalikan {@code null}</b>:
 *   bila field kosong, keduanya mengembalikan <i>waktu server saat ini</i>. Pemeriksaan
 *   {@code jam.getMulai() != null} karena itu selalu benar dan tidak berguna.</li>
 *   <li>Digabung dengan poin di atas, {@link #getWaktuMulai()}/{@link #getWaktuSelesai()} adalah
 *   <b>getter yang menulis balik ke field</b>; pada baris yang kolom waktunya kosong, sekadar
 *   membaca getter tersebut dapat mengisi teks dengan jam sekarang dan — bila object sedang
 *   ter-attach — ikut tersimpan pada flush berikutnya.</li>
 *   <li>{@link #setOleh(String)} dan {@link #setOlehId(String)} <b>mengabaikan</b> nilai
 *   {@code null}/kosong secara diam-diam, jadi jejak audit tidak bisa dikosongkan lewat setter.</li>
 *   <li>{@link #getFakultas()} menimpa field {@code fakultas} dari prodi bila prodi terisi.</li>
 *   <li>{@link #toString()} punya efek samping (memanggil {@link #getJurusan()}).</li>
 *   <li>{@link #getSks()} hanya ditampilkan di layar master; tidak ada satu pun perhitungan di
 *   basis kode yang membacanya.</li>
 * </ul>
 *
 * <h2>Komentar generator</h2>
 * <p>Baris {@code "JamPerkuliahan generated by hbm2java"} di berkas ini <b>benar</b>. Baris yang
 * sama muncul salah salin-tempel di empat berkas lain yang bukan {@code JamPerkuliahan}:
 * {@code MasaPerkuliahan}, {@code JadwalSidangTugasAkhir}, {@code JadwalSeminarTugasAkhir}, dan
 * {@code GelombangPendaftaranSidangTugasAkhir}. Jangan tertukar saat menelusuri kode.</p>
 *
 * @see GeneralValueObject
 * @see Perkuliahan
 * @see TemplatePerkuliahanDetail
 * @see Jurusan
 * @see Fakultas
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "jam_perkuliahan")
public class JamPerkuliahan extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya dipertahankan agar object yang tersimpan di cache
	 * (MapDB/berkas sementara) dari rilis lama tetap bisa dibaca; jangan diubah.
	 */
	private static final long serialVersionUID = -8842945307087672400L;

	/**
	 * Primary key tabel {@code jam_perkuliahan}, dibangkitkan basis data ({@code IDENTITY}).
	 * Dideklarasikan ulang di sini karena {@link GeneralValueObject} tidak dipetakan Hibernate.
	 */
	private Long id;

	/**
	 * Nama pengguna terakhir yang mengubah baris ini (jejak audit ringan, di luar Envers).
	 * Diisi lewat {@link #setOleh(String)} yang mengabaikan nilai kosong.
	 */
	private String oleh;

	/**
	 * Identitas (NIP/NIM/user id) pengguna terakhir yang mengubah baris ini, pendamping
	 * {@link #oleh}. Diisi lewat {@link #setOlehId(String)} yang mengabaikan nilai kosong.
	 */
	private String olehId;

	/**
	 * Mengembalikan identitas pengguna terakhir yang mengubah slot jam ini.
	 *
	 * @return identitas pengguna (NIP/NIM/user id), atau {@code null} bila belum pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel identitas pengguna terakhir yang mengubah slot jam ini.
	 *
	 * <p><b>Perhatian:</b> nilai {@code null}, string kosong, atau string yang hanya berisi spasi
	 * <b>diabaikan diam-diam</b> — field lama dipertahankan. Jadi setter ini hanya bisa mengisi
	 * atau mengganti, tidak bisa mengosongkan jejak audit.</p>
	 *
	 * @param olehId identitas pengguna; nilai kosong/hanya-spasi tidak berpengaruh.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna terakhir yang mengubah slot jam ini.
	 *
	 * <p><b>Perhatian:</b> sama seperti {@link #setOlehId(String)}, nilai {@code null}/kosong/
	 * hanya-spasi <b>diabaikan diam-diam</b> sehingga nama lama tetap bertahan.</p>
	 *
	 * @param oleh nama pengguna; nilai kosong/hanya-spasi tidak berpengaruh.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah slot jam ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait daur hidup JPA yang dijalankan tepat sebelum baris ini di-{@code UPDATE}; meneruskan
	 * object ke {@code AuditTimestampInterceptor.ubah(this)} untuk memperbarui stempel waktu
	 * audit. Dipanggil oleh Hibernate, bukan oleh kode aplikasi.
	 *
	 * <p>Pada baris kode yang sama juga dideklarasikan field {@code tanggal_dirubah}: waktu
	 * perubahan terakhir, dipetakan ke kolom {@code timestamp} dan <b>diinisialisasi ke waktu
	 * server saat object dibuat</b> (lewat {@code ais.ui.util.WaktuUtil.getDate()}), sehingga
	 * baris baru selalu punya stempel meski belum pernah disimpan. Tata letak satu baris ini
	 * adalah gaya bawaan repo; jangan diurai ulang tanpa alasan.</p>
	 *
	 * @see #getTanggal_dirubah()
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir baris ini.
	 *
	 * <p>Umumnya tidak perlu dipanggil manual — kait {@link #onUpdate()} sudah mengurusnya saat
	 * penyimpanan.</p>
	 *
	 * @param tanggal_dirubah stempel waktu perubahan; boleh {@code null}.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini (kolom {@code timestamp}).
	 *
	 * @return stempel waktu perubahan; tidak pernah {@code null} untuk object yang baru dibuat
	 *         karena field-nya diinisialisasi ke waktu server.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Ringkasan teks slot jam untuk keperluan log/debug, berformat
	 * {@code "<id>-<nama>-<mulai>_<sampai>_<jurusan>"}.
	 *
	 * <p><b>Punya efek samping.</b> Baris pertamanya memanggil {@link #getJurusan()} dan menugaskan
	 * hasilnya kembali ke field {@code jurusan}, sehingga method ini ikut memicu resolusi proxy
	 * lazy — yang pada kasus terburuk membuka session Hibernate baru (dan menutupnya sendiri).
	 * Hindari memanggilnya di dalam loop besar atas object detached.</p>
	 *
	 * <p><b>Dua ketidakkonsistenan yang disengaja/terlanjur:</b> (1) bagian jam memakai
	 * <b>field mentah</b> {@code mulai}/{@code sampai}, bukan {@link #getMulai()}/
	 * {@link #getSampai()}, jadi slot yang jamnya kosong dicetak sebagai {@code "null"} alih-alih
	 * disubstitusi waktu server; (2) {@code Date} dirender dengan {@code Date.toString()} bawaan
	 * (mis. {@code "Thu Jan 01 07:00:00 WIB 1970"}), bukan dengan pola {@code "HH.mm"} yang dipakai
	 * di layar. Format induk {@code "kode - nama"} milik {@link GeneralValueObject} sengaja tidak
	 * dipakai di sini.</p>
	 *
	 * @return ringkasan teks slot jam; komponen {@code jurusan} bernilai {@code "null"} bila slot
	 *         berlaku umum.
	 */
	public String toString() {
		jurusan = getJurusan();
		return getId() + "-" + getNama() + "-" + mulai + "_" + sampai + "_" + jurusan;
	}


	/**
	 * Nama/label slot sebagaimana ditampilkan di layar dan di bandbox pemilih, mis.
	 * {@code "Jam ke 1"}. Wajib diisi di layar master. Inilah satu-satunya tempat nomor urut slot
	 * "hidup" — tidak ada kolom urutan tersendiri.
	 */
	private String nama;

	/**
	 * Jam mulai slot, kolom {@code mulai} bertipe {@link TemporalType#TIME} (komponen tanggal
	 * tidak bermakna). Kembarannya dalam bentuk teks adalah {@link #waktuMulai}.
	 */
	private Date mulai;

	/**
	 * Jam selesai slot, kolom {@code sampai} bertipe {@link TemporalType#TIME}. Kembarannya dalam
	 * bentuk teks adalah {@link #waktuSelesai}.
	 */
	private Date sampai;

	/**
	 * Prodi/jurusan pemilik slot; {@code null} berarti slot berlaku untuk semua prodi.
	 * Relasi {@code @ManyToOne} lazy — akses selalu lewat {@link #getJurusan()}.
	 */
	private Jurusan jurusan;

	/**
	 * Fakultas pemilik slot; {@code null} berarti berlaku untuk semua fakultas. Nilainya
	 * <b>diturunkan ulang dari {@link #jurusan}</b> setiap kali {@link #getFakultas()} dipanggil
	 * bila prodi terisi, jadi jangan diperlakukan sebagai data mandiri.
	 */
	private Fakultas fakultas;

	/**
	 * Program penyelenggaraan tempat slot berlaku (mis. Reguler/Karyawan), disimpan sebagai teks.
	 * {@code null}/kosong berarti "Semua" — begitulah layar master dan bandbox menampilkannya.
	 */
	private String program;

	/** Catatan bebas operator tentang slot ini; ditampilkan sebagai kolom grid di layar master. */
	private String keterangan;

	/**
	 * Bentuk teks {@link #mulai} dengan pola {@code "HH.mm"} ({@code Common.timeFormat2}), kolom
	 * {@code waktu_mulai}. <b>Ikut disimpan ke basis data</b>, bukan sekadar nilai turunan.
	 */
	private String waktuMulai;

	/**
	 * Bentuk teks {@link #sampai} dengan pola {@code "HH.mm"} ({@code Common.timeFormat2}), kolom
	 * {@code waktu_selesai}. <b>Ikut disimpan ke basis data</b>, bukan sekadar nilai turunan.
	 */
	private String waktuSelesai;

	/**
	 * Bobot SKS yang setara dengan panjang slot ini. Bersifat <b>informasional saja</b>: hanya
	 * ditampilkan di formulir dan grid layar master, tidak dibaca oleh perhitungan mana pun di
	 * basis kode.
	 */
	private Integer sks;

	/**
	 * Penanda slot masih dipakai. {@code null} diperlakukan sama dengan {@code true} — baik oleh
	 * {@link #getAktif()} maupun oleh penyaring query pemakai yang berbentuk
	 * {@code or(isNull("aktif"), eq("aktif", true))}.
	 */
	private Boolean aktif;

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate/JPA dan dipakai layar master saat menambah slot
	 * baru ({@code JamPerkuliahanAction.onAdd}). Tidak melakukan inisialisasi apa pun selain
	 * bawaan field — perhatikan bahwa {@code tanggal_dirubah} sudah terisi waktu server dari
	 * inisialisasi field-nya.
	 */
	public JamPerkuliahan() {

	}

	/**
	 * Mengembalikan primary key slot jam ini.
	 *
	 * @return id baris, atau {@code null} bila object belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key slot jam ini. Praktis hanya dipanggil Hibernate; kolomnya
	 * {@code insertable = false} dan diisi oleh basis data.
	 *
	 * @param id id baris.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama/label slot, mis. {@code "Jam ke 1"}.
	 *
	 * @return nama slot, atau {@code null} bila belum diisi.
	 */
	public String getNama() {
		return nama;
	}

	/**
	 * Menyetel nama/label slot.
	 *
	 * @param nama nama slot; layar master mewajibkannya terisi sebelum menyimpan.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Menyetel prodi/jurusan pemilik slot.
	 *
	 * <p>Menyetel {@code null} berarti slot berlaku untuk semua prodi. Perhatikan bahwa
	 * {@link #getFakultas()} akan menurunkan fakultas dari prodi ini, sehingga mengganti prodi
	 * secara tidak langsung juga mengganti fakultas yang terbaca.</p>
	 *
	 * @param jurusan prodi pemilik slot; boleh {@code null} (berlaku umum).
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * Mengembalikan prodi/jurusan pemilik slot, setelah meresolusi proxy lazy.
	 *
	 * <p>Mengikuti pola standar entity AIS: hasil {@code check(...)} <b>ditugaskan kembali ke
	 * field</b> karena object yang dikembalikan bisa berupa instance lain (kanonik dari identity
	 * map, dari cache, atau hasil muat ulang). Pada jalur terakhir {@code check(...)} membuka
	 * session Hibernate baru dan menutupnya sendiri di {@code finally} — jadi getter ini bisa
	 * menyentuh basis data, tetapi tidak meninggalkan session terbuka. Relevan di sini karena
	 * instance {@code JamPerkuliahan} berumur panjang di cache ({@code InitData},
	 * {@code DataUtil.CLASS_JANGAN_DIBERSIHKAN}) sehingga sering sudah <i>detached</i>.</p>
	 *
	 * @return prodi pemilik slot, atau {@code null} bila slot berlaku untuk semua prodi.
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	/**
	 * Menyetel fakultas pemilik slot.
	 *
	 * <p><b>Nilai yang disetel di sini tidak selalu bertahan.</b> Bila {@link #getJurusan()} terisi,
	 * {@link #getFakultas()} akan menimpanya dengan fakultas milik prodi tersebut. Setter ini baru
	 * benar-benar menentukan hasil untuk slot yang prodinya kosong.</p>
	 *
	 * @param fakultas fakultas pemilik slot; boleh {@code null} (berlaku umum).
	 */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	/**
	 * Mengembalikan fakultas pemilik slot — <b>diturunkan dari prodi bila prodi terisi</b>.
	 *
	 * <p>Alurnya dua langkah: (1) resolusi proxy lazy lewat {@code check(...)} dengan penugasan
	 * balik ke field, lalu (2) bila {@link #getJurusan()} tidak {@code null}, field {@code fakultas}
	 * <b>ditimpa</b> oleh {@code getJurusan().getFakultas()}. Karena penimpaan itu mengubah field —
	 * bukan sekadar nilai kembalian — object yang sedang ter-<i>attach</i> akan ikut menuliskan
	 * fakultas turunan itu ke kolom {@code fakultas} pada flush berikutnya. Dengan kata lain kolom
	 * {@code fakultas} berperilaku seperti data <i>slave</i> dari kolom {@code jurusan}, dan nilai
	 * lama yang tidak cocok akan hilang begitu getter ini terpanggil.</p>
	 *
	 * <p>Efek samping tambahan: method ini memanggil {@link #getJurusan()} sehingga bisa memicu
	 * pembukaan session Hibernate sementara oleh {@code check(...)}.</p>
	 *
	 * @return fakultas prodi bila prodi terisi; selain itu fakultas yang tersimpan langsung, atau
	 *         {@code null} bila slot berlaku untuk semua fakultas.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "fakultas", nullable = true)
	public Fakultas getFakultas() {
		fakultas = check(fakultas);
		if (getJurusan() != null) {
			fakultas = getJurusan().getFakultas();
		}
		return fakultas;
	}

	/**
	 * Mengembalikan catatan bebas tentang slot ini apa adanya (tanpa normalisasi kosong→{@code null}).
	 *
	 * @return keterangan slot, atau {@code null} bila belum diisi.
	 */
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menyetel catatan bebas tentang slot ini.
	 *
	 * @param keterangan teks keterangan; boleh {@code null}.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan jam mulai slot sebagai {@link Date} (kolom {@code TIME}).
	 *
	 * <p><b>Tidak pernah mengembalikan {@code null}.</b> Bila field {@code mulai} kosong, method ini
	 * mengembalikan <i>waktu server saat ini</i> ({@code WaktuUtil.getDate()}) sebagai pengganti.
	 * Konsekuensinya:</p>
	 * <ul>
	 *   <li>pemeriksaan {@code jam.getMulai() != null} di kode pemanggil selalu benar dan tidak
	 *   menyaring apa pun;</li>
	 *   <li>nilai substitusi itu <b>tidak</b> ditulis balik ke field oleh method ini sendiri —
	 *   tetapi {@link #getWaktuMulai()} memanggil getter ini dan <i>menulis hasilnya ke field</i>
	 *   {@code waktuMulai}, sehingga secara tidak langsung jam sekarang bisa mendarat di basis
	 *   data untuk baris yang jamnya kosong;</li>
	 *   <li>tidak ada peringatan/log ketika substitusi terjadi.</li>
	 * </ul>
	 *
	 * @return jam mulai slot, atau waktu server saat ini bila slot belum punya jam mulai.
	 */
	@Temporal(TemporalType.TIME)
	@Column(name = "mulai")
	public Date getMulai() {
		return mulai == null ? ais.ui.util.WaktuUtil.getDate() : mulai;
	}

	/**
	 * Menyetel jam mulai slot <b>sekaligus menyegarkan bentuk teksnya</b>.
	 *
	 * <p>Bila argumen tidak {@code null}, field {@link #waktuMulai} langsung ditulis ulang dengan
	 * hasil format {@code Common.timeFormat2} (pola {@code "HH.mm"}). Bila argumen {@code null},
	 * field {@code mulai} dikosongkan tetapi {@code waktuMulai} <b>dibiarkan apa adanya</b> —
	 * inilah salah satu celah yang membuat dua representasi waktu bisa berbeda.</p>
	 *
	 * @param mulai jam mulai slot; {@code null} mengosongkan {@code mulai} tanpa menyentuh teksnya.
	 * @see #setWaktuMulai(String)
	 */
	public void setMulai(Date mulai) {
		this.mulai = mulai;
		if (mulai != null) {
			waktuMulai = Common. timeFormat2.get().format(mulai);
		}
	}

	/**
	 * Mengembalikan jam selesai slot sebagai {@link Date} (kolom {@code TIME}).
	 *
	 * <p>Berperilaku sama persis dengan {@link #getMulai()}: <b>tidak pernah {@code null}</b>, dan
	 * mengembalikan waktu server saat ini bila field {@code sampai} kosong. Baca peringatan di
	 * {@link #getMulai()} — semuanya berlaku di sini.</p>
	 *
	 * @return jam selesai slot, atau waktu server saat ini bila slot belum punya jam selesai.
	 */
	@Temporal(TemporalType.TIME)
	@Column(name = "sampai")
	public Date getSampai() {
		return sampai == null ? ais.ui.util.WaktuUtil.getDate() : sampai;
	}

	/**
	 * Menyetel jam selesai slot <b>sekaligus menyegarkan bentuk teksnya</b>.
	 *
	 * <p>Cerminan {@link #setMulai(Date)}: argumen non-{@code null} memicu penulisan ulang
	 * {@link #waktuSelesai} dengan pola {@code "HH.mm"}, sedangkan argumen {@code null} hanya
	 * mengosongkan {@code sampai} dan meninggalkan teks lama.</p>
	 *
	 * @param sampai jam selesai slot; {@code null} mengosongkan {@code sampai} tanpa menyentuh
	 *               teksnya.
	 * @see #setWaktuSelesai(String)
	 */
	public void setSampai(Date sampai) {
		this.sampai = sampai;
		if (sampai != null) {
			waktuSelesai = Common.timeFormat2.get().format(sampai);
		}
	}

	/**
	 * Mengembalikan program penyelenggaraan tempat slot berlaku, dengan string kosong dinormalkan
	 * menjadi {@code null}.
	 *
	 * <p>Tidak menulis balik ke field — normalisasi hanya berlaku pada nilai kembalian. Pemanggil
	 * (layar master dan bandbox) menerjemahkan {@code null} menjadi label {@code "Semua"}.
	 * Catatan: hanya string benar-benar kosong yang dinormalkan; string berisi spasi saja tetap
	 * dikembalikan apa adanya (berbeda dari {@link #getWaktuMulai()} yang mem-{@code trim}).</p>
	 *
	 * @return nama program, atau {@code null} bila slot berlaku untuk semua program.
	 */
	public String getProgram() {
		return program == null || program.isEmpty() ? null : program;
	}

	/**
	 * Menyetel program penyelenggaraan tempat slot berlaku.
	 *
	 * @param program nama program; {@code null}/kosong berarti berlaku untuk semua program.
	 */
	public void setProgram(String program) {
		this.program = program;
	}

	/**
	 * Menyetel bentuk teks jam mulai <b>sekaligus mengurainya kembali menjadi {@link Date}</b>.
	 *
	 * <p>Alurnya: (1) bila teks berisi sesuatu, teks diurai dengan {@code Common.timeFormat2}
	 * (pola {@code "HH.mm"}) dan hasilnya ditulis ke field {@link #mulai}; (2) teksnya sendiri
	 * disimpan setelah di-{@code trim}, dengan kosong/hanya-spasi dinormalkan menjadi
	 * {@code null}.</p>
	 *
	 * <p><b>Kegagalan penguraian ditelan diam-diam</b> (hanya dicatat lewat
	 * {@code ErrorAuditUtil.record}, blok {@code catch}-nya kosong dan masih menyisakan
	 * {@code // TODO: handle exception}). Akibatnya, teks yang tidak sesuai pola — misalnya
	 * {@code "07:00"} dengan titik dua, bukan titik — <b>tetap tersimpan</b> sementara field
	 * {@code mulai} bertahan pada nilai lamanya. Sejak titik itu kolom {@code waktu_mulai} dan
	 * kolom {@code mulai} menunjuk jam yang berbeda, dan pemakai yang membaca lewat jalur berbeda
	 * akan melihat jadwal yang berbeda pula.</p>
	 *
	 * @param waktuMulai jam mulai berbentuk teks pola {@code "HH.mm"}; {@code null}/kosong
	 *                   disimpan sebagai {@code null} dan tidak mengubah {@link #mulai}.
	 * @see #setMulai(Date)
	 */
	public void setWaktuMulai(String waktuMulai) {

		if (waktuMulai != null && !waktuMulai.trim().isEmpty()) {
			try {
				mulai = Common.timeFormat2.get().parse(waktuMulai);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/JamPerkuliahan.java:185");
				// TODO: handle exception
			}
		}

		this.waktuMulai = waktuMulai == null || waktuMulai.trim().equals("") ? null : waktuMulai.trim();
	}

	/**
	 * Mengembalikan bentuk teks jam mulai (kolom {@code waktu_mulai}, pola {@code "HH.mm"}).
	 *
	 * <p><b>Getter dengan efek samping — menulis balik ke field.</b> Bila {@link #waktuMulai} masih
	 * {@code null}, method ini mengisinya dari hasil format {@link #getMulai()}. Karena
	 * {@link #getMulai()} sendiri <b>tidak pernah mengembalikan {@code null}</b> (jatuh ke waktu
	 * server saat ini), gabungan keduanya berarti: <i>membaca</i> getter ini pada slot yang kolom
	 * {@code mulai} dan {@code waktu_mulai}-nya sama-sama kosong akan mengisi field dengan
	 * <b>jam sekarang</b>. Bila object sedang ter-<i>attach</i> ke session Hibernate, nilai itu
	 * ikut tersimpan pada flush berikutnya ({@code dynamicUpdate} aktif) — tanpa ada satu pun
	 * pemanggil yang bermaksud menyunting data.</p>
	 *
	 * <p>Seluruh blok pengisian dibungkus {@code try/catch} yang mencetak stack trace dan mencatat
	 * ke {@code ErrorAuditUtil}, sehingga getter ini tidak pernah melempar exception.</p>
	 *
	 * <p>Nilai kembalian dinormalkan: {@code null} atau hanya-spasi menjadi {@code null}, selain itu
	 * hasil {@code trim()}. Bandingkan dengan {@link #getWaktuSelesai()} yang normalisasinya
	 * <b>tidak</b> setara.</p>
	 *
	 * @return jam mulai berbentuk teks, atau {@code null} bila kosong.
	 */
	@Column(name = "waktu_mulai", length = 20)
	public String getWaktuMulai() {

		try {
			if (waktuMulai == null) {

				if (getMulai() != null) {
					waktuMulai = Common.timeFormat2.get().format(getMulai());
				}
			}

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/JamPerkuliahan.java:205");
		}
		return waktuMulai == null || waktuMulai.trim().equals("") ? null : waktuMulai.trim();
	}

	/**
	 * Menyetel bentuk teks jam selesai <b>sekaligus mengurainya kembali menjadi {@link Date}</b>.
	 *
	 * <p>Cerminan {@link #setWaktuMulai(String)}: teks non-kosong diurai dengan pola
	 * {@code "HH.mm"} ke field {@link #sampai}, lalu teksnya disimpan setelah {@code trim} dengan
	 * kosong dinormalkan menjadi {@code null}. Kegagalan penguraian juga <b>ditelan diam-diam</b>
	 * (hanya dicatat ke {@code ErrorAuditUtil}), dengan akibat yang sama: teks tersimpan sementara
	 * {@code sampai} bertahan pada nilai lama.</p>
	 *
	 * @param waktuSelesai jam selesai berbentuk teks pola {@code "HH.mm"}; {@code null}/kosong
	 *                     disimpan sebagai {@code null} dan tidak mengubah {@link #sampai}.
	 * @see #setSampai(Date)
	 */
	public void setWaktuSelesai(String waktuSelesai) {

		if (waktuSelesai != null && !waktuSelesai.trim().isEmpty()) {
			try {
				sampai = Common.timeFormat2.get().parse(waktuSelesai);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/JamPerkuliahan.java:215");
				// TODO: handle exception
			}
		}

		this.waktuSelesai = waktuSelesai == null || waktuSelesai.trim().equals("") ? null : waktuSelesai.trim();
	}

	/**
	 * Mengembalikan bentuk teks jam selesai (kolom {@code waktu_selesai}, pola {@code "HH.mm"}).
	 *
	 * <p><b>Getter dengan efek samping — menulis balik ke field</b>, persis seperti
	 * {@link #getWaktuMulai()}: field yang masih {@code null} diisi dari hasil format
	 * {@link #getSampai()}, yang sendirinya tidak pernah {@code null} sehingga jam server bisa
	 * mendarat di data. Baca peringatan lengkapnya di {@link #getWaktuMulai()}.</p>
	 *
	 * <p><b>Ketidaksetaraan dengan kembarannya (dicatat, bukan diperbaiki):</b> normalisasi
	 * kembalian di sini memakai {@code waktuSelesai.equals("")} <i>tanpa</i> {@code trim()},
	 * sedangkan {@link #getWaktuMulai()} memakai {@code waktuMulai.trim().equals("")}. Untuk nilai
	 * tersimpan yang hanya berisi spasi, {@code getWaktuMulai()} mengembalikan {@code null}
	 * sementara method ini mengembalikan <b>string kosong</b>. Pemanggil yang memeriksa
	 * {@code != null} karena itu bisa berperilaku berbeda untuk jam mulai dan jam selesai pada
	 * baris yang sama.</p>
	 *
	 * @return jam selesai berbentuk teks, {@code null} bila tersimpan {@code null} atau benar-benar
	 *         string kosong, atau string kosong bila yang tersimpan hanya spasi.
	 */
	@Column(name = "waktu_selesai", length = 20)
	public String getWaktuSelesai() {

		try {
			if (waktuSelesai == null) {
				if (getSampai() != null) {
					waktuSelesai = Common.timeFormat2.get().format(getSampai());
				}
			}

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/JamPerkuliahan.java:234");
		}
		return waktuSelesai == null || waktuSelesai.equals("") ? null : waktuSelesai.trim();
	}

	/**
	 * Mengembalikan status aktif slot, dengan {@code null} diperlakukan sebagai {@code true}.
	 *
	 * <p>Normalisasi ini hanya pada nilai kembalian — field tidak ditulis balik. Perlakuan yang
	 * sama diulang di sisi query oleh setiap pemakai dalam bentuk
	 * {@code Restrictions.or(isNull("aktif"), eq("aktif", true))}, sehingga slot lama yang kolom
	 * {@code aktif}-nya masih {@code NULL} tetap ikut terpakai.</p>
	 *
	 * @return {@code true} bila slot aktif atau statusnya belum pernah diisi; {@code false} bila
	 *         dinonaktifkan.
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel status aktif slot. Menonaktifkan slot menyembunyikannya dari bandbox pemilih dan
	 * dari daftar master (kecuali saat penyaring "tampilkan nonaktif" dinyalakan), tetapi
	 * <b>tidak</b> memutus jadwal yang sudah terlanjur menunjuk slot ini.
	 *
	 * @param aktif {@code true}/{@code null} berarti aktif, {@code false} berarti nonaktif.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan bobot SKS yang setara dengan panjang slot ini.
	 *
	 * <p>Nilai ini <b>tidak pernah dipakai untuk perhitungan apa pun</b> di basis kode — satu-satunya
	 * pembacanya adalah layar master {@code JamPerkuliahanAction} (kolom grid, ditampilkan
	 * {@code "-"} bila kosong, dan kotak isian pada formulir). Jangan berasumsi total SKS jadwal
	 * dihitung dari sini.</p>
	 *
	 * @return bobot SKS slot, atau {@code null} bila tidak diisi.
	 */
	public Integer getSks() {
		return sks;
	}

	/**
	 * Menyetel bobot SKS yang setara dengan panjang slot ini.
	 *
	 * @param sks bobot SKS; boleh {@code null}. Layar master mengisinya dari sebuah
	 *            {@code Decimalbox}, jadi nilai pecahan yang diketik operator akan terpotong
	 *            menjadi bilangan bulat.
	 */
	public void setSks(Integer sks) {
		this.sks = sks;
	}

}
