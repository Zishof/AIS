package ais.database.model;

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

/**
 * Entity <b>formulir pengajuan beasiswa oleh mahasiswa</b> &mdash; satu baris tabel
 * {@code public.pengajuan_beasiswa} mewakili satu berkas permohonan: mahasiswa <i>X</i> memohon
 * program beasiswa <i>Y</i>, disertai potret keadaan sosial-ekonomi keluarganya (nama dan
 * pekerjaan orang tua, alamat, jarak tempuh ke kampus, kelas penghasilan, kondisi rumah tinggal)
 * serta alasan naratif mengapa ia layak dibantu.
 *
 * <p>Kelas ini <b>hanya menampung isian formulir</b>. Ia tidak menyimpan skor seleksi, tidak
 * menyimpan keputusan diterima/ditolak, dan tidak punya kolom status apa pun. Satu-satunya
 * penanda hasil adalah relasi opsional {@link #getMahasiswaDapatBeasiswa()} &mdash; lihat bagian
 * "Kuirk" di bawah, karena relasi itu <b>tidak pernah diisi oleh kode Java mana pun</b>.</p>
 *
 * <h2>Posisi dalam alur beasiswa</h2>
 * <p>Master programnya adalah {@link Beasiswa} (lihat Javadoc kelas tersebut untuk definisi
 * jendela pendaftaran, sasaran, dan ambang syarat; jangan duplikasi penjelasannya di sini). Dari
 * master itu ada <b>dua jalur yang berjalan sendiri-sendiri</b> dan &mdash; ini penting &mdash;
 * <b>tidak saling tersambung di dalam kode</b>:</p>
 * <ol>
 * <li><b>Jalur pendaftaran &amp; seleksi (jalur yang benar-benar dipakai untuk memutuskan).</b>
 * {@link ais.database.model.beasiswa.MahasiswaDaftarBeasiswa} menampung baris pendaftaran
 * (kolom {@code terima} sebagai hasil seleksi dan {@code totalSkor} sebagai nilainya), jawaban
 * per butir syarat di {@link ais.database.model.beasiswa.MahasiswaBeasiswaPersyaratan}, lalu
 * penetapan penerima final di {@link MahasiswaDapatBeasiswa}. Layar pendukungnya
 * {@code ais.action.master.helper.AmbilDataMahasiswaSeleksiBeasiswaHelper} dan
 * {@code ais.action.master.helper.AmbilDataMahasiswaBeasiswaHelper}.</li>
 * <li><b>Jalur formulir rinci (kelas ini).</b> Layar {@code pengajuan_beasiswa.zul} yang
 * dikendalikan {@code ais.action.master.PengajuanBeasiswaAction}: mahasiswa (atau petugas)
 * mengisi profil sosial-ekonomi keluarga. Data ini murni menjadi <b>bahan pertimbangan manual</b>
 * bagi panitia; tidak ada kode yang menghitung skor darinya, dan tidak ada kode yang
 * mempromosikan sebuah {@code PengajuanBeasiswa} menjadi {@code MahasiswaDaftarBeasiswa} atau
 * {@code MahasiswaDapatBeasiswa}.</li>
 * </ol>
 * <p><b>Anak langsung</b> kelas ini adalah {@link KeadaanKeluargaPengajuanBeasiswa} (daftar
 * anggota keluarga: nama, hubungan, pekerjaan, umur). Relasinya <b>searah dari sisi anak</b>
 * &mdash; kelas ini tidak mendeklarasikan satu pun koleksi {@code @OneToMany}, sehingga untuk
 * mengambil "semua anggota keluarga pengajuan X" kode harus menjalankan Criteria/HQL sendiri
 * dengan {@code Restrictions.eq("pengajuanBeasiswa", pengajuan)}. Perlu dicatat: entity anak itu
 * <b>yatim di sisi kode</b> &mdash; tidak punya DAO, tidak punya Action, dan tidak dirujuk satu
 * pun berkas ZUL, jadi tabelnya (beserta tabel bayangan Envers-nya) memang terbentuk tapi tidak
 * pernah diisi lewat aplikasi.</p>
 *
 * <h2>Pemetaan Hibernate</h2>
 * <p>{@code @Entity} + {@code @Table(schema = "public", name = "pengajuan_beasiswa")},
 * {@code dynamicInsert}/{@code dynamicUpdate} aktif (hanya kolom yang benar-benar berubah ikut
 * dalam {@code INSERT}/{@code UPDATE}), dan {@code @Audited} sehingga setiap perubahan direkam
 * Hibernate Envers ke tabel bayangan {@code pengajuan_beasiswa_AUD}.</p>
 * <p>Pemetaan memakai <b>property access</b> (anotasi menempel pada getter), sehingga
 * <b>setiap pasangan getter/setter yang tidak dianotasi {@code @Transient} tetap dipetakan</b>.
 * Karena {@code ais.database.hibernate.MyNamingStrategy} adalah turunan
 * {@code DefaultNamingStrategy} (nama kolom = nama properti apa adanya, tanpa konversi ke
 * {@code under_score}), mayoritas properti di kelas ini &mdash; yang memang tidak diberi
 * {@code @Column} &mdash; jatuh ke kolom bernama persis seperti propertinya dan tetap
 * <i>camelCase</i>: {@code namaBapak}, {@code pekerjaanIbu}, {@code kodePos},
 * {@code jarakKotaKecamatan}, {@code jarakKampus}, {@code alatTransportasi},
 * {@code rumahTinggal}, {@code luasBangunanRumah}, {@code peneranganRumah},
 * {@code sumberAirBersih}, {@code tanggalPengajuan}, {@code tanggal_dirubah}, {@code oleh},
 * {@code olehId}. Hanya {@code id}, {@code nama}, {@code keterangan}, dan ketiga kolom relasi
 * yang namanya ditentukan eksplisit.</p>
 * <p>Ketiga relasi ({@link #getMahasiswa()}, {@link #getBeasiswa()},
 * {@link #getMahasiswaDapatBeasiswa()}) memakai {@code @ManyToOne} dengan
 * {@code cascade = {PERSIST, MERGE}} dan {@code @Fetch(FetchMode.SELECT)}: fetch tetap
 * <i>eager</i> (bawaan {@code @ManyToOne}) tetapi lewat {@code SELECT} terpisah, bukan
 * {@code JOIN}. Konsekuensi praktisnya, menampilkan satu halaman grid pengajuan menghasilkan
 * beberapa query tambahan per baris (pola N+1 yang disengaja oleh generator aslinya).</p>
 *
 * <h2>Hubungan dengan {@link GeneralValueObject}</h2>
 * <p>Kelas induk <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass}, melainkan POJO
 * abstrak biasa; Hibernate <b>tidak memetakan properti milik induk</b>. Karena itu deklarasi
 * ulang {@code id}, {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} di kelas ini
 * <b>bukan duplikasi yang keliru, melainkan keharusan teknis</b> &mdash; tanpa deklarasi ulang,
 * kolom-kolom tersebut tidak akan pernah dipetakan. Pola yang sama muncul di hampir semua entity
 * repo ini.</p>
 * <p>Berbeda dengan {@link Beasiswa}, kelas ini <b>tidak memakai</b>
 * {@link GeneralValueObject#check(Object)} pada getter relasinya; ketiga getter relasi
 * mengembalikan field apa adanya. Akibatnya <b>tidak ada satu pun method di kelas ini yang
 * menyentuh {@code Session} Hibernate</b> &mdash; tidak membuka, tidak menutup, tidak
 * me-resolve proxy. Semua akses database terjadi di luar kelas ini (DAO/Action).</p>
 *
 * <h2>Pengelompokan method</h2>
 * <ol>
 * <li><b>Jejak audit</b> &mdash; {@link #getOleh()}/{@link #setOleh(String)},
 * {@link #getOlehId()}/{@link #setOlehId(String)},
 * {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, dan hook {@code @PreUpdate}
 * {@link #onUpdate()}.</li>
 * <li><b>Identitas &amp; deskripsi</b> &mdash; {@link #getId()}, {@link #getNama()} (turunan,
 * lihat kuirk), {@link #getKeterangan()}, {@link #toString()}.</li>
 * <li><b>Relasi inti</b> &mdash; {@link #getMahasiswa()} (pemohon),
 * {@link #getBeasiswa()} (program yang dimohon), {@link #getMahasiswaDapatBeasiswa()}
 * (penanda persetujuan, opsional dan praktis mati).</li>
 * <li><b>Waktu</b> &mdash; {@link #getTanggalPengajuan()} (turunan bila {@code null}).</li>
 * <li><b>Profil orang tua</b> &mdash; {@link #getNamaBapak()}, {@link #getNamaIbu()},
 * {@link #getPekerjaanBapak()}, {@link #getPekerjaanIbu()}, {@link #getPenghasilan()}.</li>
 * <li><b>Alamat &amp; keterjangkauan</b> &mdash; {@link #getKelurahan()},
 * {@link #getKecamatan()}, {@link #getKabupaten()}, {@link #getProvinsi()},
 * {@link #getKodePos()}, {@link #getJarakKotaKecamatan()}, {@link #getJarakKampus()},
 * {@link #getAlatTransportasi()}.</li>
 * <li><b>Kondisi rumah</b> &mdash; {@link #getRumahTinggal()},
 * {@link #getLuasBangunanRumah()}, {@link #getPeneranganRumah()},
 * {@link #getSumberAirBersih()}.</li>
 * <li><b>Narasi</b> &mdash; {@link #getPenjelasanAlasanBeasiswa()} (kolom {@code text}).</li>
 * </ol>
 * <p>Tidak ada method statis, tidak ada helper query, dan tidak ada method bisnis lain di kelas
 * ini: seluruh isinya adalah aksesor properti ditambah {@link #toString()} dan hook audit.</p>
 *
 * <h2>Pola "getter yang menulis balik"</h2>
 * <p>Seperti banyak entity lain di repo ini, tidak semua getter di sini polos. Karena entity
 * yang dibaca dari session Hibernate bersifat <i>managed</i>, perubahan yang terjadi saat
 * <i>membaca</i> ikut ter-{@code UPDATE} ke database pada flush berikutnya <b>meskipun tidak ada
 * layar yang secara sadar menyimpan apa pun</b>. Di kelas ini ada dua:</p>
 * <ul>
 * <li><b>Selalu menimpa</b> (bukan hanya saat {@code null}): {@link #getNama()}.</li>
 * <li><b>Mengisi hanya saat {@code null}</b>: {@link #getTanggalPengajuan()} (diisi
 * "sekarang").</li>
 * </ul>
 * <p>Keduanya adalah <b>kolom terpetakan sungguhan</b>, bukan {@code @Transient}, jadi hasil
 * logika getter benar-benar tersimpan permanen.</p>
 *
 * <h2>Verifikasi pola "flag {@code aktif} satu arah"</h2>
 * <p>Pola penonaktifan satu arah yang ditemukan di banyak entity lain (mis. {@code KasKecil},
 * {@code KasBesar}, {@code DaftarPengajuanTransfer}) <b>tidak berlaku di sini</b>:
 * {@code PengajuanBeasiswa} <b>sama sekali tidak punya field {@code aktif}</b>, tidak punya
 * getter/setter-nya, dan tidak punya kolom untuknya. Penghapusan berkas pengajuan dilakukan
 * secara fisik lewat {@code Common.refreshDelete(...)} dari tombol "Hapus Data" di renderer
 * {@code PengajuanBeasiswaAction}, bukan lewat penandaan nonaktif. Jadi tidak ada pertanyaan
 * "satu arah atau dua arah" untuk entity ini.</p>
 *
 * <h2>Kuirk yang perlu diketahui sebelum menyunting</h2>
 * <ul>
 * <li><b>{@code nama} adalah kolom nyata, tapi isinya selalu ditimpa.</b>
 * {@link #getNama()} menghitung ulang {@code mahasiswa + "-" + beasiswa} pada setiap pembacaan.
 * Karena pemetaan memakai property access, Hibernate memanggil getter itu saat menyusun
 * {@code INSERT} maupun saat <i>dirty checking</i>, sehingga kolomnya selalu konvergen ke hasil
 * perhitungan. Efek sampingnya: bila {@code Mahasiswa.toString()} atau
 * {@code Beasiswa.toString()} berubah (mahasiswa berganti nama, tanggal program disunting),
 * sekadar <b>membuka daftar pengajuan</b> dapat menghasilkan {@code UPDATE} sekaligus
 * <b>revisi Envers baru</b> tanpa ada pengguna yang menekan Simpan. Jangan pernah menulis
 * {@code setNama(...)} dan berharap nilainya bertahan.</li>
 * <li><b>Justru karena kuirk di atas, pencarian layar bisa bekerja.</b>
 * {@code PengajuanBeasiswaAction.initCriteria()} memfilter dengan
 * {@code Restrictions.ilike("nama", ..., ANYWHERE)}; berkat isi kolom yang berupa gabungan
 * "{@code id-nim - nama mahasiswa}-{@code nama-tanggalBuka-tanggalTutup-instansi beasiswa}",
 * satu kotak pencarian bisa menemukan berdasarkan NIM, nama mahasiswa, nama program, maupun
 * nama instansi pemberi dana sekaligus. {@code PengajuanBeasiswaAction.onSave()} memang tidak
 * pernah mengisi {@code nama} secara eksplisit &mdash; dan tidak perlu.</li>
 * <li><b>Pengurutan {@code Order.asc("nama")} sebenarnya mengurutkan menurut id mahasiswa
 * sebagai teks</b>, karena {@code Mahasiswa.toString()} diawali {@code id}. Jadi "10-..."
 * mendahului "2-...". Kolom grid yang menampilkan sort ini berlabel "Beasiswa", yang menyesatkan
 * &mdash; catat saja, jangan diubah tanpa permintaan.</li>
 * <li><b>Risiko panjang kolom.</b> {@code nama} dipetakan {@code length = 255} dan
 * {@code nullable = false}, sementara isinya adalah gabungan dua {@code toString()} yang
 * masing-masing sudah panjang. Untuk program beasiswa dengan nama dan nama instansi yang
 * panjang, hasil gabungan bisa melewati 255 karakter dan menyebabkan kegagalan
 * {@code INSERT}/{@code UPDATE} di tingkat basis data.</li>
 * <li><b>Penjaga {@code null} di {@link #getNama()} tidak pernah aktif.</b> Rangkaian
 * {@code mahasiswa + "-" + beasiswa} selalu menghasilkan {@link String} bukan {@code null}
 * (relasi kosong menghasilkan teks {@code "null-null"}), jadi cabang
 * {@code this.nama == null ? null : ...} praktis mati.</li>
 * <li><b>{@link #getMahasiswaDapatBeasiswa()} tidak pernah diisi.</b> Penelusuran seluruh pohon
 * sumber menunjukkan {@link #setMahasiswaDapatBeasiswa(MahasiswaDapatBeasiswa)} <b>tidak
 * dipanggil dari mana pun</b>. Padahal renderer {@code PengajuanBeasiswaAction} memakai relasi
 * itu untuk menampilkan kolom "Status": {@code null} &rarr; "Belum mensetujui", selain itu
 * &rarr; "Sudah disetujui". Praktisnya kolom Status <b>selalu</b> berbunyi "Belum mensetujui"
 * kecuali barisnya diisi langsung lewat SQL. Jangan menyimpulkan dari layar bahwa tidak ada
 * pengajuan yang pernah disetujui.</li>
 * <li><b>{@link #getKeterangan()} juga tidak pernah diisi lewat UI.</b> {@code onSave()} tidak
 * menyentuhnya; kolom ini warisan template generator.</li>
 * <li><b>{@link #getPenghasilan()} adalah {@link String} kelas rentang, bukan angka.</b>
 * Nilainya berasal dari combobox berisi teks tetap seperti {@code "< Rp. 250.000"} atau
 * {@code "Rp. 250.000 s.d Rp. 500.000"}. Jangan disamakan atau dibandingkan dengan
 * {@code Beasiswa.getPenghasilanOrangTua()} yang bertipe {@link Long} rupiah &mdash; keduanya
 * hidup di jalur yang berbeda dan tidak pernah dipertemukan oleh kode.</li>
 * <li><b>Satuan jarak tidak didefinisikan di mana pun.</b> {@link #getJarakKotaKecamatan()} dan
 * {@link #getJarakKampus()} hanya {@link Double} polos; label layar pun tidak menyebut satuan.
 * Konvensi de facto adalah kilometer, tapi tidak ada yang memvalidasinya.</li>
 * <li><b>Tidak ada satu pun validasi wajib-isi di layar.</b> Semua baris
 * {@code setConstraint("no empty")} di {@code PengajuanBeasiswaAction.init()} sengaja
 * dikomentari, sehingga hampir seluruh properti di kelas ini bisa tersimpan kosong. Hanya
 * {@code mahasiswa} dan {@code beasiswa} yang benar-benar diperiksa sebelum simpan.</li>
 * <li><b>{@link #toString()} memanggil {@link #getNama()}</b>, sehingga sekadar mencetak object
 * ini ke log atau memakainya sebagai label komponen <b>ikut memicu penulisan balik</b> ke field
 * {@code nama}. Bandingkan dengan {@code Beasiswa.toString()} yang sengaja membaca field
 * langsung justru untuk menghindari efek samping semacam ini.</li>
 * <li><b>{@code DashboardStatistikPengajuanBeasiswaPerJurusan} tidak membaca entity ini.</b>
 * Meski namanya menjanjikan statistik pengajuan beasiswa, isinya adalah salinan dasbor KKN yang
 * mengagregasi {@code MahasiswaDapatKelompokKkn}. Jangan memakai dasbor itu sebagai rujukan
 * perilaku kelas ini.</li>
 * </ul>
 *
 * @see GeneralValueObject
 * @see Beasiswa
 * @see MahasiswaDapatBeasiswa
 * @see KeadaanKeluargaPengajuanBeasiswa
 * @see ais.database.model.beasiswa.MahasiswaDaftarBeasiswa
 * @see ais.database.dao.PengajuanBeasiswaDao
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "pengajuan_beasiswa")

public class PengajuanBeasiswa extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya dipatok tetap supaya sesi ZK yang di-passivate lalu
	 * diaktifkan kembali (atau data yang dikirim antar-node) tetap kompatibel walau field kelas
	 * bertambah. Jangan diubah tanpa alasan kuat.
	 *
	 * <p>Catatan: nilai ini <b>sama persis</b> dengan milik
	 * {@link KeadaanKeluargaPengajuanBeasiswa} karena keduanya lahir dari template generator yang
	 * sama. Kebetulan itu tidak berbahaya (serialVersionUID hanya dibandingkan per kelas), tapi
	 * jangan dijadikan patokan bahwa kedua kelas sekerabat.</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama; dipetakan pada {@link #getId()}. */
	private Long id;

	/** Nama pengguna terakhir yang mengubah baris ini; dipetakan pada {@link #getOleh()}. */
	private String oleh;

	/** Id pengguna terakhir yang mengubah baris ini; dipetakan pada {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Id pengguna yang terakhir menyunting baris ini (kolom {@code olehId}).
	 *
	 * @return id pengguna, atau {@code null} bila baris belum pernah disunting lewat jalur yang
	 *         mengisi jejak audit.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna penyunting terakhir. <b>Nilai {@code null} atau yang hanya berisi
	 * spasi diabaikan diam-diam</b> (nilai lama dipertahankan), supaya jejak audit tidak terhapus
	 * oleh pemanggil yang kebetulan menyalin object kosong.
	 *
	 * @param olehId id pengguna; {@code null}/kosong &rarr; tidak ada perubahan.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna penyunting terakhir. Sama seperti
	 * {@link #setOlehId(String)}, <b>nilai {@code null} atau kosong diabaikan diam-diam</b>.
	 *
	 * @param oleh nama pengguna; {@code null}/kosong &rarr; tidak ada perubahan.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Nama pengguna yang terakhir menyunting baris ini (kolom {@code oleh}).
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah terisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate}: dipanggil otomatis oleh Hibernate tepat sebelum
	 * {@code UPDATE} baris ini dieksekusi, lalu meneruskan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} yang mengisi
	 * {@link #setOleh(String)}/{@link #setOlehId(String)} dari pengguna sesi aktif dan
	 * memutakhirkan {@link #setTanggal_dirubah(Date)}.
	 *
	 * <p><b>Jangan panggil manual</b> dan jangan ubah tanda tangannya; hook ini hanya berjalan
	 * pada jalur {@code UPDATE} (bukan {@code INSERT}), karena itu nilai awal
	 * {@code tanggal_dirubah} diinisialisasi langsung pada deklarasi field di baris yang sama.</p>
	 *
	 * <p><b>Catatan gaya:</b> deklarasi field {@code tanggal_dirubah} sengaja dibiarkan menyatu
	 * di baris yang sama seperti aslinya di seluruh repo ini; jangan dipecah saat merapikan
	 * format.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Normalnya diisi otomatis oleh
	 * {@link #onUpdate()}; pemanggilan manual hanya untuk migrasi/perbaikan data.
	 *
	 * @param tanggal_dirubah stempel waktu baru.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Stempel waktu perubahan terakhir (kolom {@code tanggal_dirubah}, presisi
	 * {@code TIMESTAMP}). Untuk baris yang belum pernah di-{@code UPDATE}, nilainya adalah waktu
	 * object dibuat di memori.
	 *
	 * @return stempel waktu perubahan terakhir; tidak pernah {@code null} pada object yang dibuat
	 *         lewat konstruktor.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi ringkas untuk log dan label komponen ZK: <code>{id}-{nama}</code>, dengan
	 * {@code nama} diambil dari {@link #getNama()}.
	 *
	 * <p><b>Awas efek samping:</b> karena memanggil {@link #getNama()}, memanggil method ini
	 * <b>menulis ulang field {@code nama}</b> dengan gabungan {@code mahasiswa + "-" + beasiswa}.
	 * Pada object yang sedang <i>managed</i> oleh session Hibernate, perubahan itu bisa ikut
	 * ter-{@code UPDATE} saat flush. Bandingkan dengan {@code Beasiswa.toString()} yang sengaja
	 * membaca field langsung untuk menghindari hal ini.</p>
	 *
	 * @return teks "{id}-{nama gabungan mahasiswa dan beasiswa}".
	 */
	public String toString() {
		return id + "-" + getNama();
	}

	/** Tanggal berkas diajukan; dipetakan pada {@link #getTanggalPengajuan()}. */
	private Date tanggalPengajuan;

	/** Label gabungan hasil perhitungan; dipetakan pada {@link #getNama()}. Jangan diisi manual. */
	private String nama;

	/** Keterangan bebas; dipetakan pada {@link #getKeterangan()}. Tidak pernah diisi lewat UI. */
	private String keterangan;

	/** Mahasiswa pemohon; dipetakan pada {@link #getMahasiswa()}. Wajib terisi. */
	private Mahasiswa mahasiswa;

	/** Program beasiswa yang dimohon; dipetakan pada {@link #getBeasiswa()}. Wajib terisi. */
	private Beasiswa beasiswa;

	/**
	 * Penanda persetujuan; dipetakan pada {@link #getMahasiswaDapatBeasiswa()}. Tidak pernah
	 * diisi oleh kode Java mana pun &mdash; lihat Javadoc kelas.
	 */
	private MahasiswaDapatBeasiswa mahasiswaDapatBeasiswa;

	/** Nama ayah pemohon; dipetakan pada {@link #getNamaBapak()}. */
	private String namaBapak;

	/** Nama ibu pemohon; dipetakan pada {@link #getNamaIbu()}. */
	private String namaIbu;

	/** Pekerjaan ayah pemohon; dipetakan pada {@link #getPekerjaanBapak()}. */
	private String pekerjaanBapak;

	/** Pekerjaan ibu pemohon; dipetakan pada {@link #getPekerjaanIbu()}. */
	private String pekerjaanIbu;

	/** Kelurahan/desa alamat keluarga; dipetakan pada {@link #getKelurahan()}. */
	private String kelurahan;

	/** Kode pos alamat keluarga; dipetakan pada {@link #getKodePos()}. */
	private String kodePos;

	/** Kecamatan alamat keluarga; dipetakan pada {@link #getKecamatan()}. */
	private String kecamatan;

	/** Kabupaten/kota alamat keluarga; dipetakan pada {@link #getKabupaten()}. */
	private String kabupaten;

	/** Provinsi alamat keluarga; dipetakan pada {@link #getProvinsi()}. */
	private String provinsi;

	/** Jarak rumah ke kota kecamatan; dipetakan pada {@link #getJarakKotaKecamatan()}. */
	private Double jarakKotaKecamatan;

	/** Jarak rumah ke kampus; dipetakan pada {@link #getJarakKampus()}. */
	private Double jarakKampus;

	/** Alat transportasi harian ke kampus; dipetakan pada {@link #getAlatTransportasi()}. */
	private String alatTransportasi;

	/** Kelas rentang penghasilan orang tua (teks); dipetakan pada {@link #getPenghasilan()}. */
	private String penghasilan;

	/** Status kepemilikan rumah tinggal; dipetakan pada {@link #getRumahTinggal()}. */
	private String rumahTinggal;

	/** Luas bangunan rumah; dipetakan pada {@link #getLuasBangunanRumah()}. */
	private Double luasBangunanRumah;

	/** Sumber penerangan rumah; dipetakan pada {@link #getPeneranganRumah()}. */
	private String peneranganRumah;

	/** Sumber air bersih rumah; dipetakan pada {@link #getSumberAirBersih()}. */
	private String sumberAirBersih;

	/** Alasan naratif pemohon; dipetakan pada {@link #getPenjelasanAlasanBeasiswa()}. */
	private String penjelasanAlasanBeasiswa;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate (dan dipakai
	 * {@code PengajuanBeasiswaAction.onAdd()} untuk membuat formulir kosong).
	 *
	 * <p>Semua properti dibiarkan {@code null} kecuali {@code tanggal_dirubah} yang diisi waktu
	 * sekarang lewat inisialisasi field. Perhatikan bahwa {@code tanggalPengajuan} <b>belum</b>
	 * terisi di sini &mdash; pengisiannya baru terjadi pada pembacaan pertama
	 * {@link #getTanggalPengajuan()}.</p>
	 */
	public PengajuanBeasiswa() {
	}

	/**
	 * Kunci utama baris ini (kolom {@code id}, {@code IDENTITY}, {@code insertable = false}
	 * sehingga nilainya sepenuhnya ditentukan basis data).
	 *
	 * @return id baris, atau {@code null} bila object belum pernah disimpan &mdash; kondisi
	 *         {@code null} inilah yang dipakai {@code PengajuanBeasiswaAction} untuk membedakan
	 *         "Tambah" dari "Ubah".
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama. Hanya untuk Hibernate dan keperluan migrasi; jangan disetel manual
	 * pada alur normal.
	 *
	 * @param id kunci utama baru.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Label gabungan berkas pengajuan ini (kolom {@code nama}, {@code nullable = false},
	 * {@code length = 255}).
	 *
	 * <p><b>Ini bukan getter polos.</b> Setiap pemanggilan <b>menghitung ulang dan menimpa</b>
	 * field {@code nama} dengan {@code mahasiswa + "-" + beasiswa}, yaitu gabungan
	 * {@code Mahasiswa.toString()} ("{id}-{nim} - {nama}") dan {@code Beasiswa.toString()}
	 * ("{nama}-{tanggalBuka}-{tanggalTutup}-{instansi}"). Karena pemetaan memakai property
	 * access, Hibernate memanggil method ini saat menyusun {@code INSERT} dan saat
	 * <i>dirty checking</i>, sehingga nilai kolom <b>selalu konvergen ke hasil hitungan</b> dan
	 * apa pun yang disetel lewat {@link #setNama(String)} akan tertimpa.</p>
	 *
	 * <p><b>Kenapa ini dipertahankan:</b> kolom hasil perhitungan inilah yang membuat satu kotak
	 * pencarian di {@code PengajuanBeasiswaAction.initCriteria()}
	 * ({@code Restrictions.ilike("nama", ..., ANYWHERE)}) bisa menemukan berkas berdasarkan NIM,
	 * nama mahasiswa, nama program, maupun nama instansi sekaligus.</p>
	 *
	 * <p><b>Jebakan:</b> (1) relasi yang masih kosong menghasilkan teks {@code "null-null"},
	 * bukan {@code null}, sehingga cabang {@code == null} di baris {@code return} praktis mati;
	 * (2) gabungan dua {@code toString()} yang panjang bisa melewati batas 255 karakter dan
	 * menggagalkan {@code INSERT}/{@code UPDATE}; (3) perubahan nama mahasiswa atau tanggal
	 * program membuat sekadar membaca daftar pengajuan memicu {@code UPDATE} plus revisi Envers
	 * baru.</p>
	 *
	 * @return label gabungan yang sudah di-{@code trim()}; tidak pernah {@code null} pada praktik
	 *         nyata.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		nama = mahasiswa + "-" + beasiswa;
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel label gabungan secara manual.
	 *
	 * <p><b>Praktis tidak berguna:</b> nilai apa pun yang disetel di sini akan ditimpa pada
	 * pembacaan {@link #getNama()} berikutnya &mdash; termasuk pembacaan yang dilakukan Hibernate
	 * sendiri sebelum menyimpan. Setter ini hanya ada karena syarat kontrak JavaBean untuk
	 * property access. Tidak ada pemanggil di seluruh pohon sumber.</p>
	 *
	 * @param nama label yang akan (sementara) disimpan di field.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Keterangan bebas atas berkas pengajuan ini (kolom {@code keterangan}, boleh {@code null}).
	 *
	 * <p>Warisan template generator: {@code PengajuanBeasiswaAction.onSave()} tidak pernah
	 * mengisinya, dan tidak ada layar yang menampilkannya.</p>
	 *
	 * @return keterangan, atau {@code null}.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas.
	 *
	 * @param keterangan teks keterangan; boleh {@code null}.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mahasiswa pemohon beasiswa (kolom {@code mahasiswa}, {@code nullable = false}).
	 *
	 * <p>Relasi {@code @ManyToOne} eager lewat {@code SELECT} terpisah, dengan cascade
	 * {@code PERSIST}/{@code MERGE}. Berbeda dengan {@link Beasiswa}, getter ini
	 * <b>tidak</b> memanggil {@link GeneralValueObject#check(Object)}: nilainya dikembalikan apa
	 * adanya tanpa upaya resolusi proxy dan tanpa menyentuh {@code Session}.</p>
	 *
	 * <p>Diisi {@code PengajuanBeasiswaAction.onSave()} dari komponen pencarian
	 * {@code AmbilDataMahasiswaBanbox}. Bila pengguna yang login adalah seorang mahasiswa,
	 * {@code init()} memaksakan pemohon menjadi dirinya sendiri dan mengunci komponennya.</p>
	 *
	 * @return mahasiswa pemohon; secara praktis tidak pernah {@code null} untuk baris yang sudah
	 *         tersimpan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "mahasiswa", nullable = false)
	public Mahasiswa getMahasiswa() {
		return mahasiswa;
	}

	/**
	 * Menyetel mahasiswa pemohon. Ikut menentukan hasil {@link #getNama()} pada pembacaan
	 * berikutnya.
	 *
	 * @param mahasiswa mahasiswa pemohon; kolomnya {@code NOT NULL}, jadi menyimpan dengan nilai
	 *                  {@code null} akan gagal di tingkat basis data.
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * Program beasiswa yang dimohon (kolom {@code beasiswa}, {@code nullable = false}).
	 *
	 * <p>Relasi {@code @ManyToOne} eager lewat {@code SELECT} terpisah, tanpa resolusi proxy
	 * eksplisit (lihat {@link #getMahasiswa()}).</p>
	 *
	 * <p>Pilihan program di layar dibatasi {@code Restrictions.eq("dibukaUtkMahasiswa", 1)},
	 * dan tombol Ubah/Hapus pada satu baris hanya muncul selama program terkait masih
	 * {@code dibukaUtkMahasiswa} dan {@code Beasiswa.getMasihBuka()} bernilai {@code true}
	 * &mdash; jadi berkas otomatis terkunci begitu jendela pendaftaran programnya tutup.</p>
	 *
	 * @return program beasiswa yang dimohon; secara praktis tidak pernah {@code null} untuk baris
	 *         yang sudah tersimpan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "beasiswa", nullable = false)
	public Beasiswa getBeasiswa() {
		return beasiswa;
	}

	/**
	 * Menyetel program beasiswa yang dimohon. Ikut menentukan hasil {@link #getNama()} pada
	 * pembacaan berikutnya.
	 *
	 * @param beasiswa program beasiswa; kolomnya {@code NOT NULL}.
	 */
	public void setBeasiswa(Beasiswa beasiswa) {
		this.beasiswa = beasiswa;
	}

	/**
	 * Penetapan penerima beasiswa yang terkait dengan berkas ini (kolom
	 * {@code mahasiswa_dapat_beasiswa}, boleh {@code null}) &mdash; secara desain berperan
	 * sebagai penanda "pengajuan ini sudah disetujui".
	 *
	 * <p><b>Relasi ini praktis mati.</b> Penelusuran seluruh pohon sumber menunjukkan
	 * {@link #setMahasiswaDapatBeasiswa(MahasiswaDapatBeasiswa)} tidak dipanggil dari mana pun,
	 * sehingga kolomnya selalu {@code NULL} kecuali diisi langsung lewat SQL. Akibatnya kolom
	 * "Status" pada grid {@code PengajuanBeasiswaAction} &mdash; yang dihitung persis dari
	 * {@code getMahasiswaDapatBeasiswa() == null ? "Belum mensetujui" : "Sudah disetujui"}
	 * &mdash; selalu menampilkan "Belum mensetujui".</p>
	 *
	 * <p>Penetapan penerima yang sesungguhnya berjalan di jalur lain, lewat
	 * {@link ais.database.model.beasiswa.MahasiswaDaftarBeasiswa} dan
	 * {@link MahasiswaDapatBeasiswa} secara langsung.</p>
	 *
	 * @return baris penetapan penerima, atau {@code null} (kondisi normal).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "mahasiswa_dapat_beasiswa", nullable = true)
	public MahasiswaDapatBeasiswa getMahasiswaDapatBeasiswa() {
		return mahasiswaDapatBeasiswa;
	}

	/**
	 * Menyetel penetapan penerima yang terkait dengan berkas ini. Tidak ada pemanggil di pohon
	 * sumber saat ini; disediakan bila kelak jalur formulir disambungkan ke jalur seleksi.
	 *
	 * @param mahasiswaDapatBeasiswa baris penetapan penerima; boleh {@code null}.
	 */
	public void setMahasiswaDapatBeasiswa(MahasiswaDapatBeasiswa mahasiswaDapatBeasiswa) {
		this.mahasiswaDapatBeasiswa = mahasiswaDapatBeasiswa;
	}

	/**
	 * Nama ayah pemohon (kolom {@code namaBapak}). Tidak divalidasi wajib isi.
	 *
	 * @return nama ayah, atau {@code null}.
	 */
	public String getNamaBapak() {
		return namaBapak;
	}

	/**
	 * Menyetel nama ayah pemohon.
	 *
	 * @param namaBapak nama ayah; boleh {@code null}/kosong.
	 */
	public void setNamaBapak(String namaBapak) {
		this.namaBapak = namaBapak;
	}

	/**
	 * Nama ibu pemohon (kolom {@code namaIbu}). Tidak divalidasi wajib isi.
	 *
	 * @return nama ibu, atau {@code null}.
	 */
	public String getNamaIbu() {
		return namaIbu;
	}

	/**
	 * Menyetel nama ibu pemohon.
	 *
	 * @param namaIbu nama ibu; boleh {@code null}/kosong.
	 */
	public void setNamaIbu(String namaIbu) {
		this.namaIbu = namaIbu;
	}

	/**
	 * Pekerjaan ayah pemohon (kolom {@code pekerjaanBapak}), teks bebas tanpa daftar acuan.
	 *
	 * @return pekerjaan ayah, atau {@code null}.
	 */
	public String getPekerjaanBapak() {
		return pekerjaanBapak;
	}

	/**
	 * Menyetel pekerjaan ayah pemohon.
	 *
	 * @param pekerjaanBapak teks pekerjaan; boleh {@code null}/kosong.
	 */
	public void setPekerjaanBapak(String pekerjaanBapak) {
		this.pekerjaanBapak = pekerjaanBapak;
	}

	/**
	 * Pekerjaan ibu pemohon (kolom {@code pekerjaanIbu}), teks bebas tanpa daftar acuan.
	 *
	 * @return pekerjaan ibu, atau {@code null}.
	 */
	public String getPekerjaanIbu() {
		return pekerjaanIbu;
	}

	/**
	 * Menyetel pekerjaan ibu pemohon.
	 *
	 * @param pekerjaanIbu teks pekerjaan; boleh {@code null}/kosong.
	 */
	public void setPekerjaanIbu(String pekerjaanIbu) {
		this.pekerjaanIbu = pekerjaanIbu;
	}

	/**
	 * Kelurahan/desa pada alamat keluarga pemohon (kolom {@code kelurahan}).
	 *
	 * <p>Alamat di sini adalah <b>teks lepas</b>, sama sekali tidak terhubung ke master wilayah
	 * mana pun dan tidak disinkronkan dengan alamat pada entity {@link Mahasiswa}.</p>
	 *
	 * @return nama kelurahan/desa, atau {@code null}.
	 */
	public String getKelurahan() {
		return kelurahan;
	}

	/**
	 * Menyetel kelurahan/desa alamat keluarga.
	 *
	 * @param kelurahan nama kelurahan/desa; boleh {@code null}/kosong.
	 */
	public void setKelurahan(String kelurahan) {
		this.kelurahan = kelurahan;
	}

	/**
	 * Kode pos alamat keluarga pemohon (kolom {@code kodePos}). Disimpan sebagai teks, tanpa
	 * validasi format.
	 *
	 * @return kode pos, atau {@code null}.
	 */
	public String getKodePos() {
		return kodePos;
	}

	/**
	 * Menyetel kode pos alamat keluarga.
	 *
	 * @param kodePos kode pos; boleh {@code null}/kosong.
	 */
	public void setKodePos(String kodePos) {
		this.kodePos = kodePos;
	}

	/**
	 * Kecamatan pada alamat keluarga pemohon (kolom {@code kecamatan}).
	 *
	 * @return nama kecamatan, atau {@code null}.
	 */
	public String getKecamatan() {
		return kecamatan;
	}

	/**
	 * Menyetel kecamatan alamat keluarga.
	 *
	 * @param kecamatan nama kecamatan; boleh {@code null}/kosong.
	 */
	public void setKecamatan(String kecamatan) {
		this.kecamatan = kecamatan;
	}

	/**
	 * Kabupaten/kota pada alamat keluarga pemohon (kolom {@code kabupaten}).
	 *
	 * @return nama kabupaten/kota, atau {@code null}.
	 */
	public String getKabupaten() {
		return kabupaten;
	}

	/**
	 * Menyetel kabupaten/kota alamat keluarga.
	 *
	 * @param kabupaten nama kabupaten/kota; boleh {@code null}/kosong.
	 */
	public void setKabupaten(String kabupaten) {
		this.kabupaten = kabupaten;
	}

	/**
	 * Provinsi pada alamat keluarga pemohon (kolom {@code provinsi}).
	 *
	 * @return nama provinsi, atau {@code null}.
	 */
	public String getProvinsi() {
		return provinsi;
	}

	/**
	 * Menyetel provinsi alamat keluarga.
	 *
	 * @param provinsi nama provinsi; boleh {@code null}/kosong.
	 */
	public void setProvinsi(String provinsi) {
		this.provinsi = provinsi;
	}

	/**
	 * Jarak dari rumah keluarga ke kota kecamatan (kolom {@code jarakKotaKecamatan}), salah satu
	 * indikator keterpencilan yang dinilai panitia secara manual.
	 *
	 * <p><b>Satuannya tidak didefinisikan di mana pun</b> &mdash; label layar hanya berbunyi
	 * "Jarak Kota Kecamatan" tanpa satuan, dan tidak ada validasi rentang. Konvensi de facto:
	 * kilometer.</p>
	 *
	 * @return jarak sebagai angka desimal, atau {@code null} bila tidak diisi.
	 */
	public Double getJarakKotaKecamatan() {
		return jarakKotaKecamatan;
	}

	/**
	 * Menyetel jarak rumah ke kota kecamatan.
	 *
	 * @param jarakKotaKecamatan angka jarak (satuan tidak ditentukan sistem); boleh {@code null}.
	 */
	public void setJarakKotaKecamatan(Double jarakKotaKecamatan) {
		this.jarakKotaKecamatan = jarakKotaKecamatan;
	}

	/**
	 * Jarak dari rumah keluarga ke kampus (kolom {@code jarakKampus}). Sama seperti
	 * {@link #getJarakKotaKecamatan()}, satuannya tidak ditentukan sistem.
	 *
	 * @return jarak sebagai angka desimal, atau {@code null} bila tidak diisi.
	 */
	public Double getJarakKampus() {
		return jarakKampus;
	}

	/**
	 * Menyetel jarak rumah ke kampus.
	 *
	 * @param jarakKampus angka jarak (satuan tidak ditentukan sistem); boleh {@code null}.
	 */
	public void setJarakKampus(Double jarakKampus) {
		this.jarakKampus = jarakKampus;
	}

	/**
	 * Kelas rentang penghasilan orang tua (kolom {@code penghasilan}).
	 *
	 * <p><b>Bertipe teks, bukan angka.</b> Nilainya berasal dari combobox berisi lima pilihan
	 * tetap yang dibangun {@code PengajuanBeasiswaAction.init()}: {@code "< Rp. 250.000"},
	 * {@code "Rp. 250.000 s.d Rp. 500.000"}, {@code "Rp. 500.000 s.d Rp. 1.000.000"},
	 * {@code "Rp. 1.000.000 s.d Rp. 2.500.000"}, {@code "Rp. 2.500.000 s.d Rp. 5.000.000"}.
	 * Pilihan itu ditulis langsung di dalam kode (bukan master data), combobox-nya tidak
	 * dikunci {@code readonly}, dan tidak ada rentang di atas Rp. 5.000.000.</p>
	 *
	 * <p><b>Jangan disamakan</b> dengan {@code Beasiswa.getPenghasilanOrangTua()} yang bertipe
	 * {@link Long} rupiah dan dipakai sebagai ambang syarat pada jalur seleksi; tidak ada kode
	 * yang mempertemukan keduanya.</p>
	 *
	 * @return teks kelas penghasilan, atau {@code null}.
	 */
	public String getPenghasilan() {
		return penghasilan;
	}

	/**
	 * Menyetel kelas rentang penghasilan orang tua.
	 *
	 * @param penghasilan teks kelas penghasilan; tidak divalidasi terhadap daftar pilihan, jadi
	 *                    nilai di luar kelima pilihan pun akan tersimpan apa adanya.
	 */
	public void setPenghasilan(String penghasilan) {
		this.penghasilan = penghasilan;
	}

	/**
	 * Status kepemilikan rumah tinggal keluarga (kolom {@code rumahTinggal}).
	 *
	 * <p>Diisi dari combobox dua pilihan tetap yang ditulis langsung di kode Action:
	 * {@code "Milik Sendiri"} dan {@code "Menyewa"}.</p>
	 *
	 * @return teks status kepemilikan, atau {@code null}.
	 */
	public String getRumahTinggal() {
		return rumahTinggal;
	}

	/**
	 * Menyetel status kepemilikan rumah tinggal.
	 *
	 * @param rumahTinggal teks status; tidak divalidasi terhadap daftar pilihan.
	 */
	public void setRumahTinggal(String rumahTinggal) {
		this.rumahTinggal = rumahTinggal;
	}

	/**
	 * Sumber penerangan rumah keluarga (kolom {@code peneranganRumah}).
	 *
	 * <p>Diisi dari combobox tiga pilihan tetap yang ditulis langsung di kode Action:
	 * {@code "Listrik dari PLN"}, {@code "Listrik swadaya masyarakat"},
	 * {@code "Lampu minyak tanah"}.</p>
	 *
	 * @return teks sumber penerangan, atau {@code null}.
	 */
	public String getPeneranganRumah() {
		return peneranganRumah;
	}

	/**
	 * Menyetel sumber penerangan rumah.
	 *
	 * @param peneranganRumah teks sumber penerangan; tidak divalidasi terhadap daftar pilihan.
	 */
	public void setPeneranganRumah(String peneranganRumah) {
		this.peneranganRumah = peneranganRumah;
	}

	/**
	 * Sumber air bersih rumah keluarga (kolom {@code sumberAirBersih}).
	 *
	 * <p>Diisi dari combobox tiga pilihan tetap yang ditulis langsung di kode Action:
	 * {@code "PAM"}, {@code "Sumur"}, {@code "Sumber lainnya"}.</p>
	 *
	 * @return teks sumber air bersih, atau {@code null}.
	 */
	public String getSumberAirBersih() {
		return sumberAirBersih;
	}

	/**
	 * Menyetel sumber air bersih rumah.
	 *
	 * @param sumberAirBersih teks sumber air; tidak divalidasi terhadap daftar pilihan.
	 */
	public void setSumberAirBersih(String sumberAirBersih) {
		this.sumberAirBersih = sumberAirBersih;
	}

	/**
	 * Alasan naratif pemohon mengapa ia layak menerima beasiswa (kolom bertipe {@code text},
	 * sehingga panjangnya tidak dibatasi 255 karakter seperti kolom teks lain di kelas ini).
	 *
	 * <p>Ditampilkan apa adanya sebagai kolom "Penjelasan" pada grid
	 * {@code PengajuanBeasiswaAction}, dan disunting lewat {@code Textbox} empat baris.</p>
	 *
	 * @return teks alasan, atau {@code null}.
	 */
	@Column(columnDefinition = "text")
	public String getPenjelasanAlasanBeasiswa() {
		return penjelasanAlasanBeasiswa;
	}

	/**
	 * Menyetel alasan naratif pemohon.
	 *
	 * @param penjelasanAlasanBeasiswa teks alasan; boleh {@code null}/kosong (tidak ada validasi
	 *                                 wajib isi).
	 */
	public void setPenjelasanAlasanBeasiswa(String penjelasanAlasanBeasiswa) {
		this.penjelasanAlasanBeasiswa = penjelasanAlasanBeasiswa;
	}

	/**
	 * Tanggal berkas ini diajukan (kolom {@code tanggalPengajuan}, presisi {@code DATE} sehingga
	 * bagian jamnya tidak ikut disimpan).
	 *
	 * <p><b>Ini bukan getter polos.</b> Bila field masih {@code null}, method ini
	 * <b>mengisinya dengan waktu sekarang</b> ({@code ais.ui.util.WaktuUtil.getDate()}) lalu
	 * mengembalikannya. Pada object yang <i>managed</i> oleh session Hibernate, pengisian itu
	 * ikut ter-{@code UPDATE} permanen saat flush &mdash; artinya <b>membaca</b> sebuah baris
	 * lama yang tanggalnya kosong sudah cukup untuk menstempelnya dengan tanggal hari ini,
	 * beserta satu revisi Envers baru.</p>
	 *
	 * <p>Untuk berkas baru, inilah yang membuat kotak tanggal di layar langsung terisi hari ini.
	 * Kotak tersebut selalu {@code setDisabled(true)}, jadi pengguna tidak pernah bisa mengubah
	 * tanggal pengajuan lewat UI; {@code onSave()} sekadar menyetel kembali nilai yang sama.</p>
	 *
	 * @return tanggal pengajuan; tidak pernah {@code null} setelah method ini dipanggil sekali.
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalPengajuan() {
		if (tanggalPengajuan == null) {
			tanggalPengajuan = ais.ui.util.WaktuUtil.getDate();
		}
		return tanggalPengajuan;
	}

	/**
	 * Menyetel tanggal pengajuan.
	 *
	 * <p>Menyetel {@code null} di sini tidak "mengosongkan" nilai secara efektif: pembacaan
	 * berikutnya lewat {@link #getTanggalPengajuan()} akan langsung mengisinya kembali dengan
	 * waktu sekarang.</p>
	 *
	 * @param tanggalPengajuan tanggal pengajuan baru.
	 */
	public void setTanggalPengajuan(Date tanggalPengajuan) {
		this.tanggalPengajuan = tanggalPengajuan;
	}

	/**
	 * Alat transportasi yang dipakai pemohon menuju kampus (kolom {@code alatTransportasi}),
	 * teks bebas &mdash; berbeda dengan properti profil lain di sekitarnya, yang ini disunting
	 * lewat {@code Textbox}, bukan combobox berdaftar tetap.
	 *
	 * @return teks alat transportasi, atau {@code null}.
	 */
	public String getAlatTransportasi() {
		return alatTransportasi;
	}

	/**
	 * Menyetel alat transportasi menuju kampus.
	 *
	 * @param alatTransportasi teks bebas; boleh {@code null}/kosong.
	 */
	public void setAlatTransportasi(String alatTransportasi) {
		this.alatTransportasi = alatTransportasi;
	}

	/**
	 * Luas bangunan rumah keluarga (kolom {@code luasBangunanRumah}), indikator kondisi ekonomi
	 * yang dinilai panitia secara manual.
	 *
	 * <p>Seperti kedua properti jarak, <b>satuannya tidak didefinisikan sistem</b>; label layar
	 * berbunyi "Luas bangunan rumah adalah" tanpa satuan. Konvensi de facto: meter persegi.</p>
	 *
	 * @return luas bangunan sebagai angka desimal, atau {@code null} bila tidak diisi.
	 */
	public Double getLuasBangunanRumah() {
		return luasBangunanRumah;
	}

	/**
	 * Menyetel luas bangunan rumah.
	 *
	 * @param luasBangunanRumah angka luas (satuan tidak ditentukan sistem); boleh {@code null}.
	 */
	public void setLuasBangunanRumah(Double luasBangunanRumah) {
		this.luasBangunanRumah = luasBangunanRumah;
	}

}
