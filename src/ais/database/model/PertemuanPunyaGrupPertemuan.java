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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;
import org.json.JSONObject;

/**
 * Entity <b>penghubung (junction)</b> antara satu sesi konsultasi
 * {@link GrupPertemuan} dan satu baris {@link Pertemuan} milik <b>seorang</b>
 * {@link Mahasiswa}. Tabel: {@code public.pertemuan_punya_grup_pertemuan},
 * {@code @Audited} (Envers), {@code dynamicInsert}/{@code dynamicUpdate}.
 *
 * <h2>Peran: inilah entity penghubung yang dimaksud {@link GrupPertemuan}</h2>
 * Javadoc {@link GrupPertemuan} menyimpulkan bahwa relasi {@link Pertemuan} &harr;
 * {@link GrupPertemuan} <b>tidak langsung</b>. Kelas ini adalah bukti dan pelaksana kesimpulan
 * itu: {@link GrupPertemuan} sama sekali tidak punya field koleksi maupun field {@code pertemuan},
 * dan {@link Pertemuan} tidak punya field {@code grupPertemuan}. Yang ada hanyalah baris-baris
 * kelas ini, masing-masing memegang <b>tiga</b> penunjuk sekaligus:
 * <ul>
 *   <li>{@link #getGrupPertemuan() grupPertemuan} — agenda/undangan konsultasinya (kolom
 *       {@code grup_pertemuan});</li>
 *   <li>{@link #getMahasiswa() mahasiswa} — peserta yang diundang (kolom {@code mahasiswa});</li>
 *   <li>{@link #getPertemuan() pertemuan} — baris {@link Pertemuan} milik <b>konteks akademik
 *       mahasiswa itu sendiri</b> ({@link KrsMahasiswa}, {@link MahasiswaRequestTugasAkhir}, atau
 *       {@link Skripsi}), bukan milik grupnya (kolom {@code pertemuan}).</li>
 * </ul>
 * Jadi kardinalitasnya: satu {@link GrupPertemuan} yang diikuti 20 mahasiswa menghasilkan
 * <b>20 baris kelas ini</b> dan 20 baris {@link Pertemuan} terpisah. Arah FK-nya pun dua arah
 * secara fisik — kelas ini menyimpan kolom {@code pertemuan}, sementara {@link Pertemuan}
 * menyimpan kolom {@code pertemuan_punya_grup_pertemuan}
 * ({@code Pertemuan#getPertemuanPunyaGrupPertemuan()}); {@link #getPertemuan()} bertugas menjaga
 * kedua sisi tetap sinkron (lihat di bawah).
 *
 * <h2>Siapa yang membuat/menghapus baris ini</h2>
 * Satu-satunya penulis adalah {@code GrupPertemuanAction#saveDetail(GrupPertemuan)}, yang untuk
 * tiap mahasiswa tercentang:
 * <ol>
 *   <li>mencari/menyiapkan {@link Pertemuan} sesuai {@code jenis} grup
 *       ({@link GrupPertemuan#SIDANG} &rarr; {@link Skripsi};
 *       {@link GrupPertemuan#BIMBINGAN} &rarr; {@link MahasiswaRequestTugasAkhir};
 *       {@link GrupPertemuan#KRS_MAHASISWA} &rarr; {@link KrsMahasiswa};
 *       {@link GrupPertemuan#LAINNYA} &rarr; <b>tidak ada</b> konteks akademik, {@link Pertemuan}
 *       baru yang "yatim"), menyalin tanggal/jam/ruang dari grup ke pertemuan itu;</li>
 *   <li>mencari baris kelas ini untuk pasangan ({@code pertemuan}, {@code grupPertemuan}); bila
 *       tidak ada, membuat baris baru;</li>
 *   <li>mengisi ketiga penunjuk, memasang balik {@code pertemuan.setPertemuanPunyaGrupPertemuan()},
 *       lalu {@code saveOrUpdate} keduanya.</li>
 * </ol>
 * Mahasiswa yang centangnya <b>dilepas</b> menyebabkan baris kelas ini dihapus lewat
 * {@code Common.refreshDelete(...)} — baris {@link Pertemuan}-nya sendiri <b>tidak</b> ikut
 * dihapus, sehingga bisa tertinggal sebagai pertemuan tanpa induk grup.
 *
 * <h2>Pembaca utama</h2>
 * <ul>
 *   <li>{@code AbsensiGrupPertemuanHelper} — presensi kehadiran per mahasiswa dalam satu grup;
 *       kueri-nya memakai {@code createAlias("mahasiswa", ...)} sehingga baris dengan kolom
 *       {@code mahasiswa} kosong <b>hilang diam-diam</b> dari daftar presensi.</li>
 *   <li>{@code PenjadwalanGrupPertemuanHelper} — grid peserta + pencarian NIM/nama.</li>
 *   <li>{@code AktifitasGrupPertemuanHelper}, {@code TampilanELearningAction},
 *       {@code LinimasaApi}, {@code RekapHasilMahasiswa}, {@code DashboardTimelinePertemuan},
 *       serta cache linimasa e-learning di {@link Mahasiswa} dan {@link Dosen}
 *       (kunci {@code KONSULTASI}).</li>
 *   <li>{@code ais.database.hibernate.AuditListener} — memetakan perubahan baris ini ke
 *       {@link Mahasiswa} pemiliknya dan ke daftar dosen ({@code populateDosenBuNama()}).</li>
 *   <li>{@code ais.common.DataUtil} — terdaftar di {@code CLASS_IZINKAN}, jadi baris kelas ini
 *       BOLEH masuk cache MapDB; ingat ini saat menilai efek samping getter di bawah.</li>
 * </ul>
 *
 * <h2>Posisi dalam hierarki dan konsekuensinya</h2>
 * {@code PertemuanPunyaGrupPertemuan} &rarr; {@link VOPembelajaran} &rarr; {@link VoKunci} &rarr;
 * {@code ais.database.model.sop.DataSop} &rarr; {@link ais.database.model.GeneralValueObject}.
 * Karena {@link ais.database.model.GeneralValueObject} <b>bukan</b> {@code @Entity} maupun
 * {@code @MappedSuperclass} — hanya POJO abstrak biasa — Hibernate tidak memetakan satu pun
 * properti milik induk. Itulah sebabnya {@link #getId() id}, {@link #getOleh() oleh},
 * {@link #getOlehId() olehId}, dan {@link #getTanggal_dirubah() tanggal_dirubah} <b>harus</b>
 * dideklarasikan ulang di kelas ini; pengulangan itu <b>keharusan teknis, bukan duplikasi yang
 * perlu "dibersihkan"</b>. Lihat {@link ais.database.model.GeneralValueObject} untuk penjelasan
 * lengkap mekanisme {@code check()}, cache {@code ambilData}/{@code masukkanData}, dan pola getter
 * di paket ini.
 *
 * <p>Berbeda dengan {@link GrupPertemuan} — yang mewarisi "mesin pertemuan" {@link VOPembelajaran}
 * tapi <b>tidak</b> terdaftar di rantai {@code instanceof} sehingga mesin itu mati baginya —
 * kelas inilah yang <b>terdaftar sebagai subtipe sah</b>. Rantai {@code instanceof} di
 * {@link VOPembelajaran} mengenali {@code PertemuanPunyaGrupPertemuan} pada
 * {@code populatePertemuan()}, {@code reInitPertemuan/Tugas/Ujian(Session)},
 * {@code populateDosenBuNama()}, {@code populateDosenBuId()}, {@code infoDosen()},
 * {@code infoSimple()}, {@code ambilTahunAjaran()}, {@code ambilJenis()}, dan
 * {@code ambilMerupakanSP()} — hampir semuanya mendelegasikan jawaban ke
 * {@link #getGrupPertemuan()}. Jadi <b>grup adalah pembawa metadata, penghubung inilah yang
 * dipakai mesin pembelajaran</b>.</p>
 *
 * <h2>Kuirk yang perlu diwaspadai</h2>
 * <ul>
 *   <li><b>Getter yang menulis balik ke field.</b> {@link #getNama()}, {@link #getKodeUnik()}, dan
 *       {@link #getMahasiswa()} bukan getter murni: ketiganya <i>menghitung ulang</i> nilainya dari
 *       relasi lalu <b>menimpa field</b>. Karena Hibernate memakai akses properti, nilai hasil
 *       hitung ulang itulah yang benar-benar ditulis ke kolom saat flush — nilai yang di-{@code set}
 *       dari luar bisa "hilang" tanpa jejak.</li>
 *   <li><b>Getter yang memutasi entity lain.</b> {@link #getPertemuan()} memanggil
 *       {@code pertemuan.setPertemuanPunyaGrupPertemuan(this)}. Ini memperbaiki sisi balik relasi,
 *       tetapi juga bisa <b>mengotori</b> ({@code dirty}) baris {@link Pertemuan} yang sedang
 *       attached sehingga memicu {@code UPDATE} tak terduga — dan karena
 *       {@link #getKodeUnik()}/{@link #getMahasiswa()} ikut memanggilnya, efek itu merambat.</li>
 *   <li><b>Tidak ada getter destruktif yang menge-null-kan data.</b> Diverifikasi dari kode kelas
 *       ini: tidak ada getter yang mengosongkan field atau menghapus baris (bandingkan pola
 *       destruktif pada {@code CicilanPembayaranGagal#getTanggal()}).</li>
 *   <li><b>Tidak ada getter yang membuka/menutup sesi Hibernate.</b> Diverifikasi: kelas ini tidak
 *       memanggil {@code HibernateUtil}/{@code openSession()}/{@code closeSession()} sama sekali.
 *       Resolusi lazy sepenuhnya diserahkan ke {@code check()} milik
 *       {@link ais.database.model.GeneralValueObject} (dipakai di {@link #getDikunci()} dan
 *       {@link #getMahasiswa()}).</li>
 *   <li><b>{@code ambilMahasiswaById()} warisan mengembalikan daftar KOSONG.</b> Cabang
 *       {@code instanceof PertemuanPunyaGrupPertemuan} di {@link VOPembelajaran} berisi badan
 *       kosong (variabel di-cast lalu ditandai {@code @SuppressWarnings("unused")}), padahal kelas
 *       ini justru punya penunjuk {@link #getMahasiswa() mahasiswa} langsung. Akibatnya loop
 *       {@code for (Long id : getPertemuanPunyaGrupPertemuan().ambilMahasiswaById())} di
 *       {@link Pertemuan} tidak pernah berjalan. Dicatat apa adanya, tidak diperbaiki di sini.</li>
 *   <li><b>Empat {@code TODO Auto-generated method stub}</b> yang tertinggal di
 *       {@link #ambilVOPembelajaran()}, {@link #ambilJumlahDetailperkuliahanLangsung()},
 *       {@link #getCourse()}, dan {@link #getUrutkanotomatis()} — <b>menyesatkan</b>: keempatnya
 *       sebenarnya sudah berisi implementasi yang dipakai produksi.</li>
 * </ul>
 *
 * <h2>Pengelompokan method</h2>
 * <ul>
 *   <li><b>Jejak audit (deklarasi ulang wajib):</b> {@link #getOleh()}/{@link #setOleh(String)},
 *       {@link #getOlehId()}/{@link #setOlehId(String)},
 *       {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, {@link #onUpdate()}.</li>
 *   <li><b>Identitas:</b> {@link #getId()}/{@link #setId(Long)}, {@link #toString()},
 *       {@link #getNama()}/{@link #setNama(String)},
 *       {@link #getKodeUnik()}/{@link #setKodeUnik(String)}.</li>
 *   <li><b>Tiga penunjuk penghubung:</b> {@link #getPertemuan()}/{@link #setPertemuan(Pertemuan)},
 *       {@link #getGrupPertemuan()}/{@link #setGrupPertemuan(GrupPertemuan)},
 *       {@link #getMahasiswa()}/{@link #setMahasiswa(Mahasiswa)}.</li>
 *   <li><b>Isi/administrasi:</b> {@link #getKeterangan()}/{@link #setKeterangan(String)},
 *       {@link #getDikunci()}/{@link #setDikunci(Tbmuser)}.</li>
 *   <li><b>Kontrak e-learning ({@link VOPembelajaran}):</b> {@link #getCourse()}/
 *       {@link #setCourse(String)}, {@link #getUrutkanotomatis()}/
 *       {@link #setUrutkanotomatis(Boolean)}, {@link #ambilJumlahDetailperkuliahanLangsung()}.</li>
 *   <li><b>Kontrak peserta ({@link VOPesertaPembelajaran}):</b>
 *       {@link #ambilVOPembelajaran()}.</li>
 * </ul>
 *
 * <p><b>Catatan komentar generator.</b> Baris {@code "Bank generated by hbm2java"} pada Javadoc
 * asli adalah salin-tempel keliru dari generator Hibernate Tools — kelas ini tidak ada
 * hubungannya dengan entity {@code Bank}. Komentar tersebut kini digantikan dokumentasi di
 * atas.</p>
 *
 * @see GrupPertemuan
 * @see Pertemuan
 * @see VOPembelajaran
 * @see VOPesertaPembelajaran
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "pertemuan_punya_grup_pertemuan")
public class PertemuanPunyaGrupPertemuan extends VOPembelajaran implements VOPesertaPembelajaran {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key {@code id} (IDENTITY). Dideklarasikan ulang karena induk tidak dipetakan. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini; diisi otomatis oleh interceptor audit. */
	private String oleh;
	/** ID pengguna terakhir yang mengubah baris ini; diisi otomatis oleh interceptor audit. */
	private String olehId;

	/**
	 * Baca ID pengguna terakhir yang mengubah baris ini.
	 *
	 * @return ID pengguna, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Setel ID pengguna pengubah terakhir.
	 *
	 * <p><b>Guard:</b> argumen {@code null} atau berisi spasi saja <b>diabaikan diam-diam</b> —
	 * nilai lama dipertahankan. Ini disengaja agar jejak audit tidak terhapus oleh alur yang
	 * menyalin objek tanpa membawa konteks pengguna.</p>
	 *
	 * @param olehId ID pengguna; {@code null}/kosong tidak berpengaruh
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Setel nama pengguna pengubah terakhir.
	 *
	 * <p><b>Guard:</b> sama seperti {@link #setOlehId(String)} — {@code null}/kosong diabaikan
	 * diam-diam sehingga nilai audit sebelumnya tetap utuh.</p>
	 *
	 * @param oleh nama pengguna; {@code null}/kosong tidak berpengaruh
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Baca nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait daur hidup JPA {@code @PreUpdate}: mendelegasikan pembaruan stempel waktu/pengguna ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} tepat sebelum
	 * {@code UPDATE} dijalankan. Tidak dipanggil manual dari kode aplikasi.
	 *
	 * <p><b>Perhatikan bentuk barisnya:</b> deklarasi field {@code tanggal_dirubah} berada pada
	 * baris fisik yang sama dengan method ini (pola yang dipakai konsisten di seluruh entity AIS),
	 * dengan nilai awal {@code ais.ui.util.WaktuUtil.getDate()} sehingga baris baru sudah punya
	 * stempel waktu meski belum pernah di-update.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Setel stempel waktu perubahan terakhir.
	 *
	 * @param tanggal_dirubah waktu perubahan; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Baca stempel waktu perubahan terakhir (kolom {@code TIMESTAMP}).
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} untuk objek yang baru dibuat
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat berbentuk {@code "<id>-<nama>"}.
	 *
	 * <p><b>Kuirk:</b> memakai <b>field</b> {@code nama} secara langsung, bukan {@link #getNama()}.
	 * Selama {@link #getNama()} belum pernah dipanggil pada instance yang baru dibuat, hasilnya
	 * berbentuk {@code "null-null"} walau {@link #getGrupPertemuan()} dan {@link #getPertemuan()}
	 * sudah terisi.</p>
	 *
	 * @return teks {@code id} digabung {@code nama} dengan pemisah tanda hubung
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Pengguna yang "mengunci" baris ini (kontrak {@link VoKunci}); kolom {@code dikunci}. */
	private Tbmuser dikunci;

	/**
	 * Baca pengguna yang mengunci baris ini, sesuai kontrak {@link VoKunci}.
	 *
	 * <p>Relasi {@code LAZY}, sehingga nilainya dilewatkan
	 * {@code GeneralValueObject#check(Object)} lebih dulu dan <b>hasilnya ditugaskan kembali ke
	 * field</b> — objek yang dikembalikan bisa instance lain (kanonik dari identity map, cache,
	 * atau hasil reload). Ini pola getter relasi standar di paket ini, bukan efek samping tak
	 * sengaja; method tersebut tidak pernah melempar exception dan tidak membuka sesi Hibernate
	 * baru kecuali sebagai upaya terakhir di dalam dirinya sendiri.</p>
	 *
	 * @return pengguna pengunci, atau {@code null} bila baris tidak terkunci
	 * @see ais.database.model.GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dikunci")
	public Tbmuser getDikunci() {
		dikunci = check(dikunci);
		return dikunci;
	}

	/**
	 * Setel pengguna yang mengunci baris ini.
	 *
	 * @param dikunci pengguna pengunci; {@code null} berarti tidak terkunci
	 */
	public void setDikunci(Tbmuser dikunci) {
		this.dikunci = dikunci;
	}


	/** Label baris, dihitung ulang oleh {@link #getNama()}; kolom {@code nama} ({@code NOT NULL}). */
	private String nama;
	/** Catatan bebas untuk keanggotaan ini; kolom {@code keterangan} (boleh {@code null}). */
	private String keterangan;

	/** Pertemuan milik konteks akademik mahasiswa yang diikat ke grup; kolom {@code pertemuan}. */
	private Pertemuan pertemuan;
	/** Sesi konsultasi induk; kolom {@code grup_pertemuan}. */
	private GrupPertemuan grupPertemuan;
	/** Peserta konsultasi; kolom {@code mahasiswa}, dihitung ulang oleh {@link #getMahasiswa()}. */
	private Mahasiswa mahasiswa;
	/** Kunci alami {@code "<idPertemuan>-<idGrup>"}, dihitung ulang oleh {@link #getKodeUnik()}. */
	private String kodeUnik;

	/**
	 * Konstruktor default tanpa argumen — disyaratkan Hibernate/JPA dan dipakai
	 * {@code GrupPertemuanAction#saveDetail(GrupPertemuan)} saat pasangan
	 * ({@code pertemuan}, {@code grupPertemuan}) belum punya baris. Semua penunjuk harus diisi
	 * lewat setter sebelum {@code saveOrUpdate}.
	 */
	public PertemuanPunyaGrupPertemuan() {
	}

	/**
	 * Baca primary key baris ini.
	 *
	 * <p>Dihasilkan {@code IDENTITY} oleh basis data; kolomnya {@code insertable = false} sehingga
	 * nilai yang di-{@code set} manual tidak ikut dikirim pada {@code INSERT}.</p>
	 *
	 * @return ID baris, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Setel primary key baris ini.
	 *
	 * @param id ID baris; umumnya hanya diisi Hibernate
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Baca label baris, sekaligus <b>menghitung ulang dan menimpa</b> field {@code nama}.
	 *
	 * <p><b>Bukan getter murni.</b> Bila {@link #getGrupPertemuan() grupPertemuan} dan
	 * {@link #getPertemuan() pertemuan} sama-sama terisi, field {@code nama} ditimpa dengan
	 * {@code grupPertemuan.getNama() + "-" + pertemuan.getId()}. Karena Hibernate membaca properti
	 * (bukan field) saat flush, <b>nilai hasil hitung ulang inilah yang benar-benar tersimpan</b>
	 * ke kolom {@code nama} — apa pun yang sebelumnya dipasang lewat {@link #setNama(String)}.</p>
	 *
	 * <p>Perhatikan bahwa method ini membaca <b>field</b> {@code grupPertemuan}/{@code pertemuan}
	 * langsung, bukan getter-nya, sehingga tidak memicu efek samping sisi-balik pada
	 * {@link #getPertemuan()}. Bila {@code pertemuan} belum pernah disimpan, {@code getId()}-nya
	 * masih {@code null} dan label yang tersimpan berbentuk {@code "<namaGrup>-null"}. Kolomnya
	 * {@code NOT NULL}, jadi menyimpan baris tanpa grup maupun pertemuan (dan tanpa
	 * {@link #setNama(String)}) akan ditolak basis data.</p>
	 *
	 * @return label yang sudah di-{@code trim}, atau {@code null} bila field {@code nama} kosong
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		if (grupPertemuan != null && pertemuan != null) {
			nama = grupPertemuan.getNama() + "-" + pertemuan.getId();
		}
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Setel label baris.
	 *
	 * <p>Nilai ini <b>hanya bertahan</b> selama grup atau pertemuan masih {@code null}; begitu
	 * keduanya terisi, {@link #getNama()} menimpanya pada pembacaan berikutnya.</p>
	 *
	 * @param nama label baris
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Baca catatan bebas keanggotaan ini.
	 *
	 * @return keterangan, atau {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Setel catatan bebas keanggotaan ini.
	 *
	 * @param keterangan keterangan; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Baca {@link Pertemuan} milik konteks akademik mahasiswa yang diikat ke grup ini, sekaligus
	 * <b>memperbaiki sisi balik relasi</b>.
	 *
	 * <p><b>Efek samping penting.</b> Bila field {@code pertemuan} terisi, method ini memanggil
	 * {@code pertemuan.setPertemuanPunyaGrupPertemuan(this)} — jadi <b>membaca</b> properti ini
	 * <b>memutasi objek {@link Pertemuan}</b>. Manfaatnya: kolom
	 * {@code pertemuan.pertemuan_punya_grup_pertemuan} ikut terisi sehingga
	 * {@code Pertemuan#untuk()}, {@code Pertemuan#info()}, dan seluruh rantai
	 * {@code instanceof}/{@code getPertemuanPunyaGrupPertemuan() != null} di {@link Pertemuan} dan
	 * {@link VOPembelajaran} mengenali pertemuan itu sebagai "konsultasi". Risikonya: bila
	 * {@link Pertemuan} tersebut sedang <i>attached</i> pada sesi Hibernate, pemutasian ini bisa
	 * menandainya kotor dan memicu {@code UPDATE} yang tidak diminta pemanggil. Efek ini merambat
	 * karena {@link #getKodeUnik()} dan {@link #getMahasiswa()} memanggil method ini.</p>
	 *
	 * <p>Relasi ini <b>EAGER</b> (default {@code @ManyToOne}, tidak ada {@code fetch = LAZY})
	 * dengan {@code FetchMode.SELECT} — dimuat lewat {@code SELECT} terpisah, bukan {@code JOIN}.
	 * Karena itu tidak ada panggilan {@code check()} di sini.</p>
	 *
	 * @return pertemuan terkait, atau {@code null} bila belum ditautkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pertemuan")
	public Pertemuan getPertemuan() {
		if (pertemuan != null) {
			pertemuan.setPertemuanPunyaGrupPertemuan(this);
		}
		return pertemuan;
	}

	/**
	 * Tautkan baris ini ke satu {@link Pertemuan}.
	 *
	 * <p>Setter murni — sisi balik pada {@link Pertemuan} <b>tidak</b> diisi di sini, melainkan
	 * saat {@link #getPertemuan()} dibaca (atau secara eksplisit oleh
	 * {@code GrupPertemuanAction#saveDetail(GrupPertemuan)}).</p>
	 *
	 * @param pertemuan pertemuan milik konteks akademik mahasiswa; boleh {@code null}
	 */
	public void setPertemuan(Pertemuan pertemuan) {
		this.pertemuan = pertemuan;
	}

	/**
	 * Baca sesi konsultasi ({@link GrupPertemuan}) induk baris ini.
	 *
	 * <p>Getter murni: tidak menghitung ulang, tidak memanggil {@code check()}. Relasi
	 * <b>EAGER</b> dengan {@code FetchMode.SELECT}, jadi nilainya sudah termuat begitu baris
	 * dibaca dari basis data. Inilah sumber jawaban bagi hampir semua cabang
	 * {@code instanceof PertemuanPunyaGrupPertemuan} di {@link VOPembelajaran}
	 * ({@code ambilJenis()}, {@code ambilTahunAjaran()}, {@code ambilMerupakanSP()},
	 * {@code infoSimple()}, {@code populateDosenBuNama()}, dan {@code infoDosen()} semuanya
	 * mendelegasikan ke objek ini) — sehingga <b>{@code null} di sini akan menyebabkan
	 * {@code NullPointerException} di dalam mesin {@link VOPembelajaran}</b>, yang sebagian
	 * ditangkap diam-diam dan berubah menjadi hasil {@code "-"}.</p>
	 *
	 * @return grup pertemuan induk, atau {@code null} bila belum ditautkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "grup_pertemuan")
	public GrupPertemuan getGrupPertemuan() {
		return grupPertemuan;
	}

	/**
	 * Tautkan baris ini ke satu sesi konsultasi {@link GrupPertemuan}.
	 *
	 * @param grupPertemuan grup pertemuan induk; boleh {@code null}
	 */
	public void setGrupPertemuan(GrupPertemuan grupPertemuan) {
		this.grupPertemuan = grupPertemuan;
	}

	/**
	 * Baca kunci alami baris ini, sekaligus <b>menghitung ulang dan menimpa</b> field
	 * {@code kodeUnik}.
	 *
	 * <p>Formatnya {@code "<idPertemuan>-<idGrupPertemuan>"}. Kolomnya {@code unique = true},
	 * sehingga inilah yang menegakkan aturan "satu pertemuan hanya boleh terikat sekali pada satu
	 * grup" di tingkat basis data — pelengkap pencarian
	 * {@code Restrictions.eq("pertemuan", ...) + Restrictions.eq("grupPertemuan", ...)} yang
	 * dipakai {@code GrupPertemuanAction#saveDetail(GrupPertemuan)} sebelum membuat baris baru.</p>
	 *
	 * <p><b>Bukan getter murni, dan berefek samping ganda:</b> (1) field {@code pertemuan}
	 * ditugaskan ulang dari {@link #getPertemuan()}, yang berarti sisi balik pada {@link Pertemuan}
	 * ikut dimutasi; (2) field {@code kodeUnik} ditimpa. Nilai apa pun yang dipasang lewat
	 * {@link #setKodeUnik(String)} akan hilang begitu {@code pertemuan} terisi.</p>
	 *
	 * <p><b>Kuirk/bug potensial:</b> hanya {@code pertemuan} yang diperiksa {@code null}, sedangkan
	 * {@code grupPertemuan.getId()} langsung didereferensi. Baris dengan {@code pertemuan} terisi
	 * tetapi {@code grup_pertemuan} kosong akan melempar {@code NullPointerException} saat dibaca
	 * atau di-flush. Selain itu bila {@code pertemuan}/{@code grupPertemuan} belum tersimpan,
	 * kode yang dihasilkan mengandung teks {@code "null"} dan bisa bertabrakan dengan baris lain
	 * yang senasib pada indeks {@code unique}.</p>
	 *
	 * @return kunci alami {@code "<idPertemuan>-<idGrup>"}, atau {@code null} bila belum ada
	 *         pertemuan yang ditautkan
	 */
	@Column(unique = true)
	public String getKodeUnik() {
		pertemuan = getPertemuan();
		if (pertemuan != null) {
			kodeUnik = pertemuan.getId() + "-" + grupPertemuan.getId();
			return kodeUnik;
		} else {
			return null;
		}
	}

	/**
	 * Setel kunci alami secara manual.
	 *
	 * <p>Praktis hanya berguna untuk baris yang belum punya {@code pertemuan}; setelah itu
	 * {@link #getKodeUnik()} akan menimpanya.</p>
	 *
	 * @param kodeUnik kunci alami
	 */
	public void setKodeUnik(String kodeUnik) {
		this.kodeUnik = kodeUnik;
	}

	/**
	 * Baca peserta konsultasi, sekaligus <b>menurunkan ulang dan menimpa</b> field
	 * {@code mahasiswa} dari konteks akademik {@link #getPertemuan() pertemuan}.
	 *
	 * <p><b>Bukan getter murni.</b> Urutan penurunannya: bila {@code pertemuan} terisi, mahasiswa
	 * diambil dari cabang pertama yang tidak {@code null} —
	 * {@code pertemuan.getKrsMahasiswa().getMahasiswa()}, lalu
	 * {@code pertemuan.getSkripsi().getMahasiswa()}, lalu
	 * {@code pertemuan.getMahasiswaRequestTugasAkhir().getMahasiswa()}. Nilai hasil penurunan itu
	 * <b>menimpa</b> apa pun yang dipasang lewat {@link #setMahasiswa(Mahasiswa)}, dan karena
	 * Hibernate memakai akses properti, itulah yang tersimpan ke kolom {@code mahasiswa}.
	 * Konsekuensinya: <b>memindahkan {@link #setPertemuan(Pertemuan)} ke pertemuan milik mahasiswa
	 * lain akan diam-diam memindahkan kepemilikan baris konsultasi ini</b>.</p>
	 *
	 * <p>Untuk grup berjenis {@link GrupPertemuan#LAINNYA} ketiga cabang di atas selalu
	 * {@code null} (pertemuan yang dibuat {@code GrupPertemuanAction#saveDetail} sengaja tidak
	 * punya KRS/skripsi/TA), sehingga nilai yang dipasang eksplisit lewat
	 * {@link #setMahasiswa(Mahasiswa)} bertahan — di situlah field ini benar-benar menjadi satu-
	 * satunya sumber kebenaran.</p>
	 *
	 * <p>Method ini memanggil {@link #getPertemuan()}, jadi <b>ikut membawa efek samping sisi-balik
	 * ke {@link Pertemuan}</b>. Relasinya {@code LAZY}, sehingga hasil akhir dilewatkan
	 * {@code GeneralValueObject#check(Object)} dan ditugaskan kembali ke field.</p>
	 *
	 * @return mahasiswa peserta, atau {@code null} bila tidak dapat diturunkan maupun dipasang
	 * @see ais.database.model.GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa")
	public Mahasiswa getMahasiswa() {
		pertemuan = getPertemuan();
		if (pertemuan != null) {
			if (pertemuan.getKrsMahasiswa() != null) {
				mahasiswa = pertemuan.getKrsMahasiswa().getMahasiswa();
			} else if (pertemuan.getSkripsi() != null) {
				mahasiswa = pertemuan.getSkripsi().getMahasiswa();
			} else if (pertemuan.getMahasiswaRequestTugasAkhir() != null) {
				mahasiswa = pertemuan.getMahasiswaRequestTugasAkhir().getMahasiswa();
			}
		}
		mahasiswa = check(mahasiswa);
		return mahasiswa;
	}

	/**
	 * Setel peserta konsultasi secara eksplisit.
	 *
	 * <p>Dipakai {@code GrupPertemuanAction#saveDetail(GrupPertemuan)} untuk setiap mahasiswa yang
	 * dicentang. Nilai ini akan <b>ditimpa</b> oleh {@link #getMahasiswa()} bila pertemuan yang
	 * ditautkan punya konteks KRS/skripsi/tugas akhir.</p>
	 *
	 * @param mahasiswa peserta; boleh {@code null}
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * Implementasi {@link VOPesertaPembelajaran#ambilVOPembelajaran()}.
	 *
	 * <p>Baris ini <b>merangkap dua peran</b>: ia adalah peserta pembelajaran sekaligus objek
	 * pembelajaran itu sendiri (turunan {@link VOPembelajaran}), sehingga mengembalikan
	 * {@code this}. Perhatikan komentar {@code TODO Auto-generated method stub} yang tertinggal —
	 * <b>menyesatkan</b>, implementasinya memang sudah final dan dipakai produksi.</p>
	 *
	 * @return {@code this}
	 */
	@Override
	public VOPembelajaran ambilVOPembelajaran() {
		// TODO Auto-generated method stub
		return this;
	}

	/**
	 * Jumlah peserta langsung untuk satu objek pembelajaran — selalu {@code 1}.
	 *
	 * <p>Konstanta ini benar secara struktural: satu baris kelas ini memang mewakili tepat satu
	 * mahasiswa (berbeda dengan {@link Perkuliahan} yang menghitung baris
	 * {@code Detailperkuliahan}). Nilainya dipakai UI e-learning generik
	 * ({@code TampilanELearningAction}, {@code CommonUiFactoryHelper},
	 * {@code ElearningApiUtil}) untuk menghitung tinggi baris/jumlah peserta. Komentar
	 * {@code TODO Auto-generated method stub} di dalamnya <b>menyesatkan</b>.</p>
	 *
	 * @return selalu {@code 1}
	 */
	@Override
	public Integer ambilJumlahDetailperkuliahanLangsung() {
		// TODO Auto-generated method stub
		return 1;
	}

	/** Konfigurasi e-learning berbentuk teks JSON; kolom {@code course} bertipe {@code text}. */
	private String course;
	/** Preferensi pengurutan otomatis materi e-learning; kolom {@code urutkanotomatis}. */
	private Boolean urutkanotomatis;

	/**
	 * Baca konfigurasi e-learning baris ini sebagai teks JSON (kontrak {@link VOPembelajaran}).
	 *
	 * <p><b>Tidak destruktif:</b> bila field {@code course} {@code null} atau berisi spasi saja,
	 * method mengembalikan JSON objek kosong {@code "{}"} <b>tanpa</b> menulis balik ke field —
	 * jadi kolom di basis data tetap {@code NULL} sampai ada {@link #setCourse(String)} sungguhan.
	 * Ini menjamin pemanggil selalu menerima teks yang aman di-{@code parse}
	 * {@code org.json.JSONObject}. Komentar {@code TODO Auto-generated method stub}
	 * <b>menyesatkan</b>.</p>
	 *
	 * @return teks JSON konfigurasi, atau {@code "{}"} bila belum diisi; tidak pernah {@code null}
	 */
	@Override
	@Column(columnDefinition = "text")
	public String getCourse() {
		// TODO Auto-generated method stub
		return course == null || course.trim().isEmpty() ? new JSONObject().toString() : course;
	}

	/**
	 * Setel konfigurasi e-learning berbentuk teks JSON.
	 *
	 * <p>Tidak ada validasi bentuk JSON di sini; teks yang tidak valid baru akan gagal saat
	 * di-{@code parse} oleh pemanggil.</p>
	 *
	 * @param course teks JSON konfigurasi; boleh {@code null}/kosong
	 */
	@Override
	public void setCourse(String course) {
		this.course = course;
	}

	/**
	 * Baca preferensi pengurutan otomatis materi e-learning (kontrak {@link VOPembelajaran}).
	 *
	 * <p><b>Tidak destruktif:</b> {@code null} diperlakukan sebagai {@code true} (default aktif)
	 * tanpa menulis balik ke field, sehingga kolom tetap {@code NULL} di basis data. Komentar
	 * {@code TODO Auto-generated method stub} <b>menyesatkan</b>.</p>
	 *
	 * @return {@code true} bila pengurutan otomatis aktif; tidak pernah {@code null}
	 */
	@Override
	public Boolean getUrutkanotomatis() {
		// TODO Auto-generated method stub
		return urutkanotomatis == null ? true : urutkanotomatis;
	}

	/**
	 * Setel preferensi pengurutan otomatis materi e-learning.
	 *
	 * @param urutkanotomatis {@code true}/{@code false}; {@code null} berarti kembali ke default
	 *                        aktif saat dibaca
	 */
	@Override
	public void setUrutkanotomatis(Boolean urutkanotomatis) {
		this.urutkanotomatis = urutkanotomatis;
	}
}
