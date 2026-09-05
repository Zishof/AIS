package ais.database.model.surat;

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

import ais.database.model.GeneralValueObject;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.StatusMahasiswa;

/**
 * <b>Entity JPA — "peruntukan" sebuah klasifikasi surat keluar: untuk siapa surat jenis ini dibuat.</b>
 *
 * <p>Setiap {@link KlasifikasiSuratKeluar} menunjuk satu baris kelas ini lewat
 * {@link KlasifikasiSuratKeluar#getKlasifikasiSuratKeluarUntuk()}. Nilainya menentukan
 * <i>subjek</i> surat — mahasiswa, siswa, dosen, guru, pegawai, atau umum — dan dari situ mengalir
 * sejumlah keputusan UI dan validasi pada {@code SuratKeluarAction}: baris form mana yang
 * ditampilkan (pencarian mahasiswa vs pencarian pegawai), apakah pemilih semester muncul, serta
 * prasyarat apa yang harus dipenuhi subjek sebelum surat boleh dibuat.</p>
 *
 * <h2>PERINGATAN: nama tabel menyesatkan</h2>
 * <p>Kelas bernama {@code KlasifikasiSuratKeluarUntuk} tetapi dipetakan ke tabel
 * <b>{@code surat.klasifikasi_surat_masuk_untuk}</b> — perhatikan kata <b>masuk</b>. Ini warisan
 * salin-tempel dari modul surat masuk. Tidak ada tabrakan pemetaan (tidak ada entity lain di
 * seluruh basis kode yang memetakan tabel tersebut, dan tidak ada kelas
 * {@code KlasifikasiSuratMasukUntuk}), sehingga secara fungsional aman. Namun konsekuensinya
 * nyata bagi siapa pun yang membaca skema langsung dari basis data: tabel bernama
 * "…surat_masuk_untuk" sebenarnya berisi konfigurasi <b>surat keluar</b>. Query SQL manual,
 * skrip migrasi, dan laporan ad-hoc mudah salah sasaran karena ini. Mengganti nama tabel
 * memerlukan migrasi DB dan karenanya belum dilakukan.</p>
 *
 * <h2>Baris-barisnya adalah enum yang di-seed otomatis, bukan data bebas</h2>
 * <p>Walaupun tabel ini punya layar CRUD sendiri
 * ({@code ais.action.master.surat.KlasifikasiSuratKeluarUntukAction}), isinya diperlakukan kode
 * lain sebagai <b>enumerasi tetap</b>. Blok {@code static} pada
 * {@code ais.action.master.surat.util.SuratUtil} menjalankan {@code initKlasifikasi(session, nama)}
 * untuk enam nama baku saat kelas itu pertama kali dimuat: {@code "Mahasiswa"}, {@code "Siswa"},
 * {@code "Dosen"}, {@code "Guru"}, {@code "Pegawai"}, dan {@code "Umum"}. Method tersebut mencari
 * baris ber-{@code nama} persis itu; <b>bila tidak ditemukan, baris baru langsung dibuat dan
 * disimpan ke database</b>, lalu referensinya dipegang di field statis {@code SuratUtil.MAHASISWA},
 * {@code SuratUtil.SISWA}, {@code SuratUtil.DOSEN}, {@code SuratUtil.GURU},
 * {@code SuratUtil.PEGAWAI}, dan {@code SuratUtil.UMUM}.</p>
 *
 * <p>Ini adalah pola <b>auto-seed</b> yang sama seperti di modul konfigurasi, dan membawa jebakan
 * yang sama:</p>
 * <ul>
 *   <li><b>Mengganti nama sebuah baris lewat layar CRUD berbahaya.</b> Misalkan baris
 *   {@code "Mahasiswa"} diubah menjadi {@code "Mahasiswa Aktif"}. Baris tersebut tetap dirujuk oleh
 *   seluruh {@link KlasifikasiSuratKeluar} yang sudah ada. Tetapi pada restart JVM berikutnya
 *   {@code SuratUtil} tidak lagi menemukan baris bernama {@code "Mahasiswa"}, sehingga ia
 *   <b>membuat baris baru</b> dengan nama itu. Sejak saat itu ada dua baris yang bermakna sama,
 *   {@code SuratUtil.MAHASISWA} menunjuk baris baru yang tidak dipakai klasifikasi mana pun,
 *   sedangkan perbandingan {@code getId().equals(SuratUtil.MAHASISWA.getId())} pada
 *   {@code SuratKeluarAction} berhenti cocok untuk surat-surat lama. Akibatnya baris form
 *   pencarian mahasiswa diam-diam tidak muncul lagi. Tidak ada pesan galat apa pun.</li>
 *   <li><b>Menghapus baris</b> punya efek serupa: baris akan muncul kembali sendiri pada restart
 *   berikutnya, dengan id baru dan seluruh flag kebijakan kembali ke default.</li>
 *   <li>Pencocokan berbasis {@code nama} ini juga dilakukan lewat perbandingan teks
 *   <i>case-insensitive</i> di {@link KlasifikasiSuratKeluar#getTampilkanSemester()} terhadap
 *   {@code "mahasiswa"}, {@code "siswa"}, {@code "dosen"}, dan {@code "guru"} — jadi nama baris
 *   berfungsi ganda: sebagai kunci pencarian eksak untuk auto-seed, dan sebagai penanda
 *   longgar untuk logika tampilan semester.</li>
 * </ul>
 * <p>Kesimpulan praktis: perlakukan {@code nama} pada tabel ini sebagai <b>kunci teknis</b>, bukan
 * label yang boleh diperindah. Perubahan kosmetik sebaiknya lewat {@code keterangan}.</p>
 *
 * <h2>Flag kebijakan dan seberapa jauh ditegakkan</h2>
 * <p>Tiga flag boolean pada kelas ini punya derajat penegakan yang berbeda-beda, dan perbedaan itu
 * penting saat menilai apakah sebuah kebijakan benar-benar berlaku:</p>
 * <ul>
 *   <li>{@link #getStatusMahasiswa()} / {@link #getStatusAwalMahasiswa()} — <b>ditegakkan</b> di
 *   {@code SuratKeluarAction} dengan membandingkan status mahasiswa saat ini terhadap status yang
 *   disyaratkan, dan membatalkan pemilihan klasifikasi bila tidak cocok.</li>
 *   <li>{@link #getMahasiswaHarusTelahMembayar()} — <b>ditegakkan</b> lewat
 *   {@code Common.checkStatusPembayaranMahasiswa(...)}, juga pada saat klasifikasi dipilih.</li>
 *   <li>{@link #getTanggalSuratTidakBisaDiubah()} — ditegakkan hanya sebagai
 *   {@code tanggal.setDisabled(true)} pada komponen tanggal, yaitu <b>penonaktifan kendali UI</b>,
 *   bukan validasi ulang di jalur simpan.</li>
 *   <li>{@link #getMahasiswaHanyaBisaCetakSuratSekali()} — <b>TIDAK ditegakkan sama sekali</b>.
 *   Lihat Javadoc getter tersebut.</li>
 * </ul>
 *
 * <h2>Basis data dan audit</h2>
 * <p>Skema {@code surat}, {@code dynamicInsert}/{@code dynamicUpdate}, dan
 * {@link org.hibernate.envers.Audited}. Field {@code oleh}/{@code olehId}/{@code tanggal_dirubah}
 * adalah <b>audit bayangan</b> pendamping Envers — keharusan teknis agar grid ZK dapat membaca
 * kolom "Diubah oleh" lewat Criteria biasa, karena tabel revisi Envers hanya terbaca lewat API
 * Envers.</p>
 *
 * <h2>Catatan pembangkitan</h2>
 * Bank generated by hbm2java
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "surat", name = "klasifikasi_surat_masuk_untuk")
public class KlasifikasiSuratKeluarUntuk extends GeneralValueObject {

	/**
	 * 
	 * Versi serialisasi. Nilainya identik dengan hampir seluruh entity lain di paket
	 * {@code ais.database.model.surat} karena berasal dari template hbm2java yang sama; jangan
	 * dipakai sebagai penanda tipe.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/**
	 * Kunci utama baris peruntukan. Di-generate database ({@code IDENTITY}).
	 *
	 * <p>Id inilah yang dibandingkan {@code SuratKeluarAction} terhadap
	 * {@code SuratUtil.MAHASISWA.getId()} dan kawan-kawannya untuk memutuskan baris form mana yang
	 * ditampilkan. Karena itu id baris ini <b>bermakna secara fungsional</b>: memindahkan data
	 * antar-instalasi dengan id berbeda, atau menghapus lalu membiarkan baris di-seed ulang dengan
	 * id baru, akan memutus pencocokan tersebut.</p>
	 */
	private Long id;
	/**
	 * Nama pengguna terakhir yang mengubah baris ini (field audit bayangan). Diisi otomatis oleh
	 * {@link ais.database.hibernate.AuditTimestampInterceptor} lewat {@link #onUpdate()}.
	 */
	private String oleh;
	/**
	 * Id/username pengguna terakhir yang mengubah baris ini (field audit bayangan, pasangan dari
	 * {@link #oleh}).
	 */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini. Getter murni tanpa normalisasi.
	 *
	 * @return id/username pengubah terakhir, atau {@code null} bila belum pernah tercatat.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan id pengguna pengubah terakhir, dengan <b>penjaga anti-penghapusan</b>: argumen
	 * {@code null} atau berisi spasi saja diabaikan (langsung {@code return}) sehingga nilai audit
	 * lama tidak tertimpa nilai hampa oleh pemanggil tanpa konteks pengguna, mis. blok
	 * {@code static} {@code SuratUtil} yang melakukan auto-seed dari luar sesi ZK.
	 *
	 * @param olehId id/username pengubah; diabaikan bila kosong.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Representasi teks baris peruntukan, yaitu nilai {@code nama} apa adanya (mis.
	 * {@code "Mahasiswa"}).
	 *
	 * <p>Membaca <b>field</b> {@code nama} secara langsung, bukan lewat {@link #getNama()},
	 * sehingga hasilnya <b>tidak</b> ter-{@code trim} dan dapat mengandung spasi tepi yang
	 * tersimpan di database. Perbedaan halus ini terlihat ketika nilai dipakai sebagai label
	 * combobox ZK: label bisa memuat spasi ekstra padahal perbandingan logika yang memakai
	 * {@link #getNama()} sudah ter-{@code trim}. Dapat pula mengembalikan {@code null} apa adanya
	 * bila nama belum diisi.</p>
	 *
	 * @return nama peruntukan, atau {@code null}.
	 */
	public String toString() {
		return nama;
	}

	/**
	 * Menyimpan nama pengguna pengubah terakhir, dengan penjaga anti-penghapusan yang sama seperti
	 * {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengubah; diabaikan bila kosong.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengubah terakhir, atau {@code null}.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: meneruskan ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} agar {@code oleh},
	 * {@code olehId}, dan {@code tanggal_dirubah} terisi tepat sebelum {@code UPDATE} dieksekusi.
	 * Tidak berjalan pada INSERT.
	 *
	 * <p><b>Perhatian:</b> deklarasi field {@code tanggal_dirubah} berada pada BARIS FISIK YANG
	 * SAMA dengan method ini, sehingga Javadoc ini sekaligus mendokumentasikan field tersebut:
	 * stempel waktu perubahan terakhir, diinisialisasi ke waktu sekarang lewat
	 * {@code ais.ui.util.WaktuUtil.getDate()} saat object dibuat.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan waktu perubahan terakhir. Umumnya dipanggil interceptor audit.
	 *
	 * @param tanggal_dirubah stempel waktu perubahan.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir, dipetakan {@code TIMESTAMP} (tanggal + jam).
	 *
	 * @return waktu perubahan terakhir.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Nama peruntukan; berfungsi sebagai <b>kunci enumerasi</b>, lihat {@link #getNama()} dan
	 * dokumentasi kelas.
	 */
	private String nama;
	/**
	 * Keterangan bebas untuk operator. Tidak dipakai logika mana pun, sehingga inilah tempat yang
	 * aman untuk penjelasan yang bisa diubah kapan saja — berbeda dengan {@link #nama}.
	 */
	private String keterangan;
	/**
	 * Status awal mahasiswa yang disyaratkan agar surat jenis ini boleh dibuat; {@code null}
	 * berarti tidak disyaratkan. Lihat {@link #getStatusAwalMahasiswa()}.
	 */
	private StatusAwalMahasiswa statusAwalMahasiswa;
	/**
	 * Status mahasiswa saat ini yang disyaratkan agar surat jenis ini boleh dibuat; {@code null}
	 * berarti tidak disyaratkan. Lihat {@link #getStatusMahasiswa()}.
	 */
	private StatusMahasiswa statusMahasiswa;
	/**
	 * Bila {@code true}, tanggal surat dikunci mengikuti default dan tidak dapat diubah operator.
	 * Lihat {@link #getTanggalSuratTidakBisaDiubah()} untuk batas penegakannya.
	 */
	private Boolean tanggalSuratTidakBisaDiubah;
	/**
	 * Bila {@code true}, seharusnya membatasi mahasiswa hanya boleh mencetak surat jenis ini satu
	 * kali. <b>Tidak ada kode yang membacanya.</b> Lihat
	 * {@link #getMahasiswaHanyaBisaCetakSuratSekali()}.
	 */
	private Boolean mahasiswaHanyaBisaCetakSuratSekali;
	/**
	 * Bila {@code true}, mahasiswa harus sudah melunasi biaya perkuliahan semester berjalan sebelum
	 * surat jenis ini boleh dibuat. Lihat {@link #getMahasiswaHarusTelahMembayar()}.
	 */
	private Boolean mahasiswaHarusTelahMembayar;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate. Seluruh flag boolean dibiarkan
	 * {@code null} dan baru dinormalisasi menjadi {@code false} oleh getter masing-masing.
	 */
	public KlasifikasiSuratKeluarUntuk() {
	}

	/**
	 * Mengembalikan kunci utama baris peruntukan.
	 *
	 * @return id baris, atau {@code null} bila belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci utama. Praktis hanya dipakai Hibernate.
	 *
	 * @param id kunci utama baris.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama peruntukan, sudah ter-{@code trim}.
	 *
	 * <h2>Ini kunci enumerasi, bukan sekadar label</h2>
	 * <p>Nilai kembalian method ini dipakai di dua tempat yang menentukan perilaku aplikasi, dan
	 * keduanya membandingkannya sebagai teks:</p>
	 * <ol>
	 *   <li>{@code SuratUtil.initKlasifikasi(session, nama)} mencari baris dengan
	 *   {@code Restrictions.eq("nama", nama)} untuk enam nilai baku — {@code "Mahasiswa"},
	 *   {@code "Siswa"}, {@code "Dosen"}, {@code "Guru"}, {@code "Pegawai"}, {@code "Umum"} —
	 *   dan <b>membuat baris baru bila tidak ketemu</b>. Perbandingan ini <i>case-sensitive</i> dan
	 *   eksak.</li>
	 *   <li>{@link KlasifikasiSuratKeluar#getTampilkanSemester()} membandingkan nilai ini secara
	 *   <i>case-insensitive</i> terhadap {@code "mahasiswa"}, {@code "siswa"}, {@code "dosen"},
	 *   {@code "guru"} untuk memutuskan apakah pemilih semester ditampilkan pada form surat.</li>
	 * </ol>
	 *
	 * <p>Dua pembacaan dengan aturan pencocokan berbeda atas satu kolom yang sama menciptakan celah
	 * halus. Baris bernama {@code "MAHASISWA"} (huruf besar semua) akan tetap memicu tampilan
	 * semester pada butir 2, tetapi <b>tidak</b> dikenali pada butir 1 — sehingga {@code SuratUtil}
	 * akan menambahkan baris kedua bernama {@code "Mahasiswa"} pada pemuatan kelas berikutnya, dan
	 * sejak itu ada dua baris yang bermakna sama dengan id berbeda. Karena {@code SuratKeluarAction}
	 * membandingkan <b>id</b> ({@code getId().equals(SuratUtil.MAHASISWA.getId())}), klasifikasi
	 * lama yang menunjuk baris {@code "MAHASISWA"} berhenti dikenali sebagai peruntukan mahasiswa
	 * dan baris form pencarian mahasiswa tidak lagi muncul — tanpa pesan galat.</p>
	 *
	 * <p>Perhatikan juga bahwa {@code trim()} di sini <b>tidak</b> dituliskan kembali ke field
	 * (tidak destruktif), sehingga spasi tepi yang tersimpan di database tetap ada dan tetap membuat
	 * pencocokan eksak {@code Restrictions.eq("nama", "Mahasiswa")} gagal. Dengan kata lain, spasi
	 * tepi yang tak sengaja terketik operator dapat memicu duplikasi baris enum dengan cara yang
	 * sama sekali tidak kasatmata di layar.</p>
	 *
	 * <p>Kolom dipetakan {@code nullable = false} dengan panjang 255. Meski begitu, method ini tetap
	 * memeriksa {@code null} karena object yang belum tersimpan dapat memilikinya.</p>
	 *
	 * @return nama peruntukan tanpa spasi tepi, atau {@code null} bila belum diisi.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan nama peruntukan. Setter polos: tidak melakukan {@code trim}, tidak menormalkan
	 * kapitalisasi, dan tidak memeriksa apakah nama tersebut sudah dipakai baris lain. Mengingat
	 * nama berfungsi sebagai kunci enumerasi (lihat {@link #getNama()}), pemanggil sebaiknya
	 * memastikan nilainya persis salah satu dari enam nama baku bila baris dimaksudkan sebagai
	 * peruntukan standar.
	 *
	 * @param nama nama peruntukan.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas baris ini. Getter murni tanpa normalisasi; dapat {@code null}.
	 *
	 * @return keterangan, atau {@code null}.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas. Tidak dipakai logika mana pun sehingga aman diubah kapan saja.
	 *
	 * @param keterangan keterangan bebas.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan status awal mahasiswa yang disyaratkan untuk peruntukan ini, atau {@code null}
	 * bila tidak ada syarat.
	 *
	 * <p>Getter murni: mengembalikan field apa adanya <b>tanpa</b> memanggil {@code check(...)},
	 * berbeda dengan mayoritas getter relasi di paket ini. Dipadukan dengan {@code @ManyToOne}
	 * tanpa {@code fetch = LAZY} dan {@code @Fetch(FetchMode.SELECT)}, relasi ini pada praktiknya
	 * di-fetch eager lewat query terpisah, sehingga ketiadaan {@code check(...)} jarang bermasalah.</p>
	 *
	 * <p>Bila terisi, {@code SuratKeluarAction} membandingkan status awal mahasiswa yang dipilih
	 * terhadap nilai ini dan membatalkan pemilihan klasifikasi bila tidak cocok — jadi ini adalah
	 * salah satu flag kebijakan yang benar-benar ditegakkan. Penegakannya terjadi pada saat
	 * klasifikasi dipilih di form, yaitu di jalur UI, bukan sebagai batasan basis data.</p>
	 *
	 * @return status awal mahasiswa yang disyaratkan, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "status_awal_mahasiswa", nullable = true)
	public StatusAwalMahasiswa getStatusAwalMahasiswa() {
		return statusAwalMahasiswa;
	}

	/**
	 * Menetapkan status awal mahasiswa yang disyaratkan. {@code null} berarti tidak ada syarat.
	 *
	 * @param statusAwalMahasiswa status awal yang disyaratkan.
	 */
	public void setStatusAwalMahasiswa(StatusAwalMahasiswa statusAwalMahasiswa) {
		this.statusAwalMahasiswa = statusAwalMahasiswa;
	}

	/**
	 * Mengembalikan status mahasiswa saat ini yang disyaratkan untuk peruntukan ini, atau
	 * {@code null} bila tidak ada syarat.
	 *
	 * <p>Sama seperti {@link #getStatusAwalMahasiswa()}: getter murni tanpa {@code check(...)},
	 * relasi ter-fetch lewat {@code @Fetch(FetchMode.SELECT)}. Bila terisi,
	 * {@code SuratKeluarAction} mengambil status berjalan mahasiswa lewat
	 * {@code HistoryStatusMahasiswaUtil.currentStatus(mhs)} dan menolak pemilihan klasifikasi
	 * ketika id-nya berbeda dari nilai ini.</p>
	 *
	 * @return status mahasiswa yang disyaratkan, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "status_mahasiswa", nullable = true)
	public StatusMahasiswa getStatusMahasiswa() {
		return statusMahasiswa;
	}

	/**
	 * Menetapkan status mahasiswa saat ini yang disyaratkan. {@code null} berarti tidak ada syarat.
	 *
	 * @param statusMahasiswa status yang disyaratkan.
	 */
	public void setStatusMahasiswa(StatusMahasiswa statusMahasiswa) {
		this.statusMahasiswa = statusMahasiswa;
	}

	/**
	 * Menyatakan apakah tanggal surat dikunci untuk peruntukan ini, dengan normalisasi {@code null}
	 * menjadi {@code false} (default: tanggal boleh diubah).
	 *
	 * <p>Normalisasi ini tidak destruktif — nilai {@code null} tidak ditulis balik ke field —
	 * sehingga kolom di database tetap {@code NULL} dan tidak berubah hanya karena getter dipanggil.</p>
	 *
	 * <p><b>Batas penegakan.</b> Satu-satunya penegakan flag ini adalah pemanggilan
	 * {@code tanggal.setDisabled(...)} pada komponen {@code MyDatebox} di {@code SuratKeluarAction},
	 * yaitu menonaktifkan kendali tanggal di form. Jalur simpan tetap membaca
	 * {@code tanggal.getValue()} tanpa memeriksa ulang flag ini. Pada ZK, komponen yang
	 * di-{@code setDisabled} memang tidak menerima nilai baru dari klien pada alur normal, sehingga
	 * dalam praktik sehari-hari kunci ini efektif. Namun perlu dicatat bahwa ini adalah
	 * <b>pembatasan kendali UI, bukan validasi sisi server</b>: jalur penyimpanan lain yang tidak
	 * melewati form tersebut — misalnya API atau proses batch yang membangun {@link SuratKeluar}
	 * secara langsung — tidak melihat flag ini sama sekali.</p>
	 *
	 * @return {@code true} bila tanggal surat dikunci; {@code false} bila boleh diubah.
	 */
	public Boolean getTanggalSuratTidakBisaDiubah() {
		return tanggalSuratTidakBisaDiubah == null ? false : tanggalSuratTidakBisaDiubah;
	}

	/**
	 * Menetapkan apakah tanggal surat dikunci untuk peruntukan ini.
	 *
	 * @param tanggalSuratTidakBisaDiubah {@code true} untuk mengunci tanggal.
	 */
	public void setTanggalSuratTidakBisaDiubah(Boolean tanggalSuratTidakBisaDiubah) {
		this.tanggalSuratTidakBisaDiubah = tanggalSuratTidakBisaDiubah;
	}

	/**
	 * Menyatakan apakah mahasiswa hanya boleh mencetak surat jenis ini satu kali, dengan
	 * normalisasi {@code null} menjadi {@code false}.
	 *
	 * <h2>PERINGATAN: flag ini tidak pernah ditegakkan</h2>
	 * <p>Penelusuran seluruh pohon sumber menunjukkan bahwa {@code getMahasiswaHanyaBisaCetakSuratSekali()}
	 * hanya dipanggil di satu tempat: {@code KlasifikasiSuratKeluarUntukAction} baris 142, yaitu
	 * untuk <b>menampilkan kembali</b> nilai tersimpan pada checkbox form CRUD-nya sendiri.
	 * Pasangannya {@code setMahasiswaHanyaBisaCetakSuratSekali(...)} hanya dipanggil di baris 217
	 * dari kelas yang sama, saat form disimpan. Tidak ada satu pun pembacaan dari
	 * {@code SuratKeluarAction}, dari {@link SuratKeluar}, dari
	 * {@code ais.action.master.surat.util.SuratUtil}, maupun dari endpoint API cetak
	 * ({@code ais.action.servlet.api.SuratApi}).</p>
	 *
	 * <p>Dengan kata lain: administrator dapat mencentang "Mahasiswa Hanya Bisa Cetak Surat Sekali"
	 * pada layar konfigurasi, nilainya tersimpan rapi ke database dan terekam Envers, tetapi
	 * <b>tidak ada apa pun yang membatasi pencetakan ulang</b>. Mahasiswa tetap dapat mencetak surat
	 * yang sama berkali-kali. Ini bukan sekadar kode mati yang tidak berbahaya: flag yang tampil di
	 * UI memberi kesan kepada operator bahwa sebuah kendali sudah aktif padahal tidak — kondisi yang
	 * lebih buruk daripada tidak menyediakan opsinya sama sekali, karena mendorong rasa aman
	 * yang keliru.</p>
	 *
	 * <p>Konteks tambahan: mekanisme pembatasan jumlah cetak memang tidak ada di jalur cetak mana
	 * pun. {@link SuratKeluar#cetak(ais.database.model.Tbmuser)} tidak menghitung berapa kali surat
	 * telah dicetak, dan tidak ada kolom pencacah cetak pada {@link SuratKeluar}. Menegakkan flag
	 * ini karenanya memerlukan penambahan mekanisme baru, bukan sekadar menyisipkan satu
	 * pemeriksaan.</p>
	 *
	 * <p>Sampai penegakan itu ada, jangan mengandalkan flag ini untuk kepatuhan; dan bila
	 * disunting, pertimbangkan menonaktifkan atau memberi label peringatan pada checkbox-nya di
	 * {@code KlasifikasiSuratKeluarUntukAction} agar tidak menyesatkan operator.</p>
	 *
	 * @return {@code true} bila dikonfigurasi hanya boleh cetak sekali — nilai yang saat ini
	 *         tidak berpengaruh apa pun pada perilaku sistem.
	 */
	public Boolean getMahasiswaHanyaBisaCetakSuratSekali() {
		return mahasiswaHanyaBisaCetakSuratSekali == null ? false : mahasiswaHanyaBisaCetakSuratSekali;
	}

	/**
	 * Menetapkan flag "hanya boleh cetak sekali". Nilai tersimpan dan terekam Envers, tetapi tidak
	 * memengaruhi perilaku sistem — lihat {@link #getMahasiswaHanyaBisaCetakSuratSekali()}.
	 *
	 * @param mahasiswaHanyaBisaCetakSuratSekali {@code true} untuk membatasi cetak sekali.
	 */
	public void setMahasiswaHanyaBisaCetakSuratSekali(Boolean mahasiswaHanyaBisaCetakSuratSekali) {
		this.mahasiswaHanyaBisaCetakSuratSekali = mahasiswaHanyaBisaCetakSuratSekali;
	}

	/**
	 * Menyatakan apakah mahasiswa harus sudah melunasi biaya perkuliahan sebelum surat jenis ini
	 * boleh dibuat, dengan normalisasi {@code null} menjadi {@code false}.
	 *
	 * <p><b>Flag ini benar-benar ditegakkan.</b> {@code SuratKeluarAction} memanggil
	 * {@code Common.checkStatusPembayaranMahasiswa(mhs.currentSemester(), mhs.currentTahapan(), mhs,
	 * false, false)} ketika flag aktif dan seorang mahasiswa terpilih; bila pembayaran belum lunas,
	 * pemilihan klasifikasi dibatalkan dan operator diberi pesan peringatan yang menyebut
	 * semesternya.</p>
	 *
	 * <p>Sama seperti syarat status mahasiswa, penegakan terjadi pada <b>saat klasifikasi dipilih
	 * di form</b> — bukan pada saat penyimpanan dan bukan pada saat pencetakan. Konsekuensinya,
	 * surat yang sudah terlanjur dibuat ketika mahasiswa masih lunas tetap dapat dicetak ulang
	 * kemudian meskipun statusnya berubah, dan jalur pembuatan surat yang tidak melewati form ini
	 * tidak memeriksa pelunasan sama sekali. Bandingkan dengan
	 * {@link #getMahasiswaHanyaBisaCetakSuratSekali()} yang bahkan tidak diperiksa di mana pun.</p>
	 *
	 * <p>Perhatikan juga bahwa flag ini beririsan tetapi tidak identik dengan
	 * {@link KlasifikasiSuratKeluar#getHarusBayarLunasSmtLalu()} dan
	 * {@link KlasifikasiSuratKeluar#getHarusBayarLunasSmtSaatIni()} yang berada di entity
	 * klasifikasi, bukan di peruntukan. Ketiganya dapat aktif bersamaan.</p>
	 *
	 * @return {@code true} bila pelunasan disyaratkan.
	 */
	public Boolean getMahasiswaHarusTelahMembayar() {
		return mahasiswaHarusTelahMembayar == null ? false : mahasiswaHarusTelahMembayar;
	}

	/**
	 * Menetapkan syarat pelunasan biaya perkuliahan bagi mahasiswa.
	 *
	 * @param mahasiswaHarusTelahMembayar {@code true} untuk mensyaratkan pelunasan.
	 */
	public void setMahasiswaHarusTelahMembayar(Boolean mahasiswaHarusTelahMembayar) {
		this.mahasiswaHarusTelahMembayar = mahasiswaHarusTelahMembayar;
	}

}
