package ais.database.model;

// Generated Dec 23, 2009 1:56:16 PM by Hibernate Tools 3.2.4.CR1

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
 * Master <b>pejabat penanda tangan dokumen akademik</b> (tabel {@code public.staff}) -- satu
 * baris = satu jabatan struktural yang namanya dan NIP-nya dicetak di blok tanda tangan
 * dokumen resmi: transkrip akademik, KHS, KRS, SK dosen, berita acara sidang skripsi, daftar
 * hadir ujian, dan surat-surat template.
 *
 * <h2>MODUL: AKADEMIK AIS, <u>BUKAN</u> ebisnis.id -- terverifikasi</h2>
 * Nama kelas yang generik ("Staff") sempat menimbulkan dugaan bahwa entity ini bagian dari
 * lini produk terpisah <i>ebisnis.id</i> (SaaS POS/ERP multi-tenant yang menumpang codebase
 * dan session factory yang sama, lihat {@link ais.database.model.Investor} dan
 * {@link ais.database.model.AkunManajemen}) -- misalnya sebagai data pegawai/staf toko milik
 * tenant. <b>Dugaan itu SALAH.</b> Bukti dari kode:
 * <ul>
 *   <li>Seluruh relasi keluar entity ini murni akademik: {@link #getJurusan()} ke
 *       {@link ais.database.model.Jurusan}, {@link #getFakultas()} ke
 *       {@link ais.database.model.Fakultas}, {@link #getJabatan()} ke
 *       {@link ais.database.model.Jabatan}. Tidak ada relasi apa pun ke
 *       {@code Pendaftar}/{@code Brand}/{@code Investor} -- berbeda total dari pola
 *       {@link ais.database.model.Investor} dan {@link ais.database.model.AkunManajemen}
 *       yang keduanya bergantung pada {@code Pendaftar} sebagai jangkar tenant.</li>
 *   <li>Layar masternya {@code ais.action.master.StaffAction} berada di paket master
 *       akademik dan hanya menampilkan kolom Nama / NIP / Prodi / Jabatan.</li>
 *   <li>Konsumen entity ini seluruhnya kelas laporan akademik
 *       ({@code ais.action.report.format1.akademik.*}, {@code ais.action.report.helper.pdf.*},
 *       {@code ais.action.report.CommonReportHelper}, {@code AmbilLaporanMahasiswa}) --
 *       sekitar 30 kelas, semuanya untuk mengisi parameter JasperReports
 *       {@code dekan}/{@code rektor}/{@code kaprodi}/{@code pudek_1}/{@code nip}.</li>
 *   <li>Konstanta {@link #KAPRODI} ("Kaprodi" = Ketua Program Studi) adalah istilah
 *       perguruan tinggi, bukan istilah ritel.</li>
 * </ul>
 *
 * <h2>KREDENSIAL: TIDAK ADA -- terverifikasi ulang</h2>
 * Karena entity dengan nama generik pernah terbukti menyimpan password plaintext di modul
 * ebisnis.id ({@code akun_manajemen.pass}, lihat {@link ais.database.model.AkunManajemen}),
 * file ini diperiksa ulang secara eksplisit. <b>Hasil: entity ini TIDAK memiliki field
 * kredensial apa pun</b> -- tidak ada {@code pass}/{@code password}/{@code userid}/
 * {@code token}/{@code pin}, dan tidak ada relasi ke entity yang menyimpannya. Seluruh
 * properti terpetakan hanyalah {@link #getStaff()}, {@link #getNama()}, {@link #getNip()},
 * ditambah tiga FK master. Pola plaintext {@code AkunManajemen} <b>TIDAK</b> terulang di sini.
 *
 * <p>Catatan sensitivitas yang tetap berlaku: {@link #getNip()} adalah nomor induk pegawai
 * pejabat (PII ringan). Seperti seluruh entity Hibernate terpetakan lain, baris tabel ini
 * terjangkau endpoint reflektif generik {@code /Api dataRinci} ({@code task_493423ef}) oleh
 * pemegang token AIS mana pun. Dampaknya jauh lebih rendah daripada {@code AkunManajemen}
 * (nama + NIP pejabat memang dicetak di dokumen publik seperti transkrip), jadi ini
 * <i>bukan</i> temuan kredensial baru -- dicatat hanya untuk kelengkapan.</p>
 *
 * <h2>Kolom {@code staff}: kunci peran, sekaligus duplikat {@code jabatan.nama}</h2>
 * Kolom {@link #getStaff()} (bukan {@link #getNama()}) adalah <b>kunci pencarian</b> yang
 * dipakai seluruh laporan untuk menemukan pejabat yang tepat. Isinya adalah <i>salinan teks</i>
 * dari {@code jabatan.nama}: baik {@code StaffAction.onSave()},
 * {@code FakultasAction} maupun {@code JurusanAction} menuliskannya dengan
 * {@code staff.setStaff(jabatan.getNama())}. Jadi ada <b>dua sumber kebenaran untuk fakta yang
 * sama</b> -- FK {@link #getJabatan()} dan teks {@link #getStaff()} -- yang bisa menyimpang
 * (drift) begitu seseorang mengganti nama baris {@link ais.database.model.Jabatan}: kolom
 * {@code staff} di baris-baris {@code Staff} lama TIDAK ikut diperbarui, sehingga laporan yang
 * mencari lewat teks mendadak berhenti menemukan pejabatnya sementara laporan yang mencari
 * lewat FK tetap berhasil.
 *
 * <h2>BUG NYATA: pencarian kunci peran sebagian case-sensitive</h2>
 * Nilai kunci peran yang dicari pemanggil semuanya <b>literal hardcoded</b>, dan cara
 * membandingkannya <b>tidak konsisten</b>:
 * <ul>
 *   <li><b>Case-INsensitive ({@code Restrictions.ilike}) -- aman:</b> keluarga transkrip
 *       ({@code LaporanTranskipAkademik}, {@code ...Beda}, {@code ...4Kolom}) mencari
 *       {@code "dekan"}, {@code "rektor"}, {@code "Pembatu Rektor"}.</li>
 *   <li><b>Case-SENSITIVE ({@code Restrictions.eq}) -- rapuh:</b> {@code LaporanKHS},
 *       {@code LaporanKHSType1}, {@code LaporanKRS}, {@code LaporanRekamanNilai(Kelompok)},
 *       {@code LaporanRuanganDosen}, {@code LaporanDaftarHadirDosenHarian},
 *       {@code LaporanTranskipAkademikWisuda}, {@code CommonReportHelper},
 *       {@code AmbilLaporanMahasiswa}, {@code LaporanKHSSemesterPendekWindow} dsb. mencari
 *       literal huruf kecil {@code "dekan"}, {@code "rektor"}, {@code "prodi"},
 *       {@code "pudek 1"} (dengan spasi), bahkan
 *       {@code "pembantu dekan bidang administrasi dan keuangan"}.</li>
 * </ul>
 * Padahal nilai yang benar-benar <i>ditulis</i> ke kolom ini berasal dari nama baris
 * {@link ais.database.model.Jabatan} yang lazimnya berkapital -- {@code FakultasAction}
 * menulis {@code Common.getBahasa("label_dekan")} yang defaultnya <b>{@code "Dekan"}</b>
 * (lihat {@code KonfigurasiNewAction} baris ~4364), dan {@code JurusanAction} menulis
 * {@code "Kaprodi"}. Akibatnya seluruh pencarian {@code eq("staff","dekan")} tidak akan pernah
 * cocok dengan baris yang dihasilkan alur otomatis tersebut. Kegagalannya <b>diam</b>: setiap
 * pemanggil menuliskan {@code staffDekan == null ? "" : staffDekan.getNama()}, sehingga
 * dokumen tetap tercetak namun blok tanda tangannya <b>kosong</b> tanpa pesan kesalahan
 * apa pun. Kunci {@code "prodi"} (dipakai KHS/KRS) bahkan tidak pernah ditulis oleh alur mana
 * pun -- hanya cocok bila admin secara manual membuat baris
 * {@link ais.database.model.Jabatan} bernama persis {@code "prodi"} huruf kecil.
 * <b>Tidak diperbaiki di sesi ini</b> (hanya Javadoc), dicatat apa adanya.
 *
 * <h2>Kuirk: merender laporan MENULIS ke tabel ini</h2>
 * {@code LaporanTranskipAkademik}, {@code ...Beda} dan {@code ...4Kolom} melakukan
 * {@code session.save(new Staff())} saat pencarian tidak menemukan apa pun -- menyisipkan
 * baris placeholder ber-{@link #getNama()} {@code "Rektor"} / {@code "Pembatu Rektor"} dengan
 * {@link #getNip()} {@code "NIP Rektor"} / {@code "NIP Pembatu Rektor"}. Jadi sekadar mencetak
 * satu transkrip bisa membuat baris master baru (pola "jalur baca menulis ke DB" yang berulang
 * di codebase ini). Salah ketik {@code "Pembatu Rektor"} (seharusnya "Pembantu Rektor") ikut
 * <b>tersimpan permanen sebagai data</b> dan direplikasi oleh literal pencariannya sendiri,
 * sehingga tidak bisa dikoreksi tanpa mengubah kode. {@code ConstantValues.checkStaff()}
 * menyediakan penyisipan serupa yang lebih rapi, tetapi satu-satunya pemanggilnya di
 * {@code InitDataHelper} (baris ~1931) dalam keadaan <b>dikomentari</b> -- praktis mati.
 *
 * <h2>Cakupan (scoping) dan strategi pencarian</h2>
 * Baris {@code Staff} boleh terikat ke {@link #getJurusan()}, ke {@link #getFakultas()}, atau
 * ke keduanya {@code null} (berlaku se-institusi). Laporan transkrip memakai kaskade tiga
 * tahap: cari yang cocok jurusan mahasiswa &rarr; bila nihil cari yang cocok fakultasnya
 * &rarr; bila masih nihil ambil baris pertama tanpa filter apa pun. Tahap ketiga inilah yang
 * membuat dokumen bisa tertanda tangani pejabat <i>fakultas lain</i> bila data master belum
 * lengkap. {@code Common.getKaprodi(Jurusan)} dan
 * {@code NewUiLaporanDaftarHadirUjianController} memakai pola berbeda: filter jurusan wajib,
 * ditambah {@code addOrder(Order.desc("id")).setMaxResults(1)} sehingga bila ada duplikat yang
 * <b>terbaru</b> yang menang.
 *
 * <h2>Sinkronisasi otomatis dari layar lain</h2>
 * Selain layar master {@code StaffAction}, baris tabel ini dibuat/diperbarui otomatis sebagai
 * efek samping penyimpanan master lain:
 * <ul>
 *   <li>{@code FakultasAction.onSave()} -- menyalin Dekan fakultas
 *       ({@code Fakultas.getDekan()}, sebuah {@code Dosen}) menjadi baris {@code Staff}
 *       ber-{@link #getFakultas()} terisi; bahkan membuat baris
 *       {@link ais.database.model.Jabatan} "Dekan" bila belum ada.</li>
 *   <li>{@code JurusanAction.onSave()} -- menyalin Kaprodi prodi
 *       ({@code Jurusan.getKaprodi()}) menjadi baris {@code Staff} ber-{@link #getJurusan()}
 *       terisi dengan {@link ais.database.model.Jabatan} {@code ConstantValues.KAPRODI}.</li>
 * </ul>
 * Perhatikan bahwa kedua alur itu menyalin <b>nama dan NIP sebagai teks</b> dari objek
 * {@code Dosen}; tidak ada FK ke {@code Dosen}. Bila nama/NIP dosen berubah kemudian, baris
 * {@code Staff} tidak ikut berubah sampai layar Fakultas/Jurusan disimpan ulang -- dokumen
 * bisa tercetak dengan data pejabat yang basi.
 *
 * <h2>Catatan hak akses</h2>
 * Berbeda dari beberapa layar master lain yang pernah ditemukan nol pemeriksaan,
 * {@code StaffAction.doAfterCompose()} <b>memeriksa {@code CommonPrivilages.READ} secara
 * eksplisit</b> (dan mem-{@code goLogoff()} bila gagal), lalu memasang gerbang
 * CREATE/UPDATE/DELETE pada tombol tambah/ubah/hapus. Untuk jalur ZK, layar ini termasuk
 * <b>contoh positif</b>. Yang tetap perlu dicatat: alur sinkronisasi otomatis di
 * {@code FakultasAction}/{@code JurusanAction} menulis ke tabel ini <i>tanpa</i> gerbang
 * CREATE/UPDATE milik layar Staff -- siapa pun yang boleh menyunting Fakultas/Jurusan
 * otomatis boleh mengubah pejabat penanda tangan.
 *
 * <h2>Hubungan dengan {@link GeneralValueObject}</h2>
 * Kelas induk {@link GeneralValueObject} <b>bukan</b> {@code @Entity} maupun
 * {@code @MappedSuperclass} -- ia POJO abstrak biasa, sehingga Hibernate <b>tidak</b>
 * memetakan properti apa pun miliknya. Karena itu {@link #id}, {@link #oleh}, {@link #olehId}
 * dan {@link #tanggal_dirubah} <b>WAJIB</b> dideklarasikan ulang di kelas ini agar ikut
 * tersimpan. Pengulangan itu <b>keharusan teknis, bukan duplikasi atau bug</b>. Sebaliknya
 * properti induk yang <i>tidak</i> dideklarasikan ulang di sini -- terutama
 * {@code keterangan} -- memang tidak terpetakan dan tidak akan pernah tersimpan.
 * Perhatikan {@code nama} adalah kasus khusus: field-nya milik induk (dipakai langsung oleh
 * {@link #toString()}), tetapi kelas ini men-{@code @Column}-kan accessor-nya sehingga
 * kolomnya tetap terpetakan.
 *
 * <h2>Pengelompokan anggota</h2>
 * <ol>
 *   <li><b>Konstanta domain:</b> {@link #KAPRODI}.</li>
 *   <li><b>Jejak audit (deklarasi ulang wajib):</b> {@link #getOleh()}/{@link #setOleh(String)},
 *       {@link #getOlehId()}/{@link #setOlehId(String)},
 *       {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, {@link #onUpdate()}.</li>
 *   <li><b>Identitas:</b> {@link #getId()}/{@link #setId(Long)}, {@link #toString()}.</li>
 *   <li><b>Data pejabat:</b> {@link #getStaff()} (kunci peran), {@link #getNama()},
 *       {@link #getNip()} beserta setter-nya.</li>
 *   <li><b>Relasi cakupan/klasifikasi:</b> {@link #getJurusan()}, {@link #getFakultas()},
 *       {@link #getJabatan()} beserta setter-nya.</li>
 *   <li><b>Konstruktor:</b> {@link #Staff()}, {@link #Staff(String, String)}.</li>
 * </ol>
 *
 * <h2>Verifikasi pola berulang (diperiksa langsung pada kode file ini)</h2>
 * <ul>
 *   <li><b>Getter yang menulis balik ke field/DB: TIDAK ADA.</b> {@link #getNama()} memang
 *       men-{@code trim()}, tetapi hasilnya dikembalikan tanpa disimpan ke field -- berbeda
 *       dari {@code Kota.getNama()} atau {@code Penghasilan.getNama()} yang menugaskan ulang
 *       ke {@code this.nama}.</li>
 *   <li><b>Getter yang menutup sesi Hibernate: TIDAK ADA.</b> Tidak ada satu pun accessor di
 *       file ini yang menyentuh {@code HibernateUtil}/{@code Session}.</li>
 *   <li><b>Getter destruktif (menghapus data saat dibaca): TIDAK ADA.</b> Tidak ada
 *       {@code delete}/{@code remove} di mana pun.</li>
 * </ul>
 * Seluruh accessor di sini murni bebas efek samping. Efek samping tulis-saat-baca yang
 * dijelaskan di atas terjadi pada <b>pemanggil</b> ({@code LaporanTranskipAkademik*}), bukan
 * pada kelas ini.
 *
 * <p><b>Catatan komentar generator.</b> Baris "Staff generated by hbm2java" adalah komentar
 * asli Hibernate Tools 3.2.4.CR1; sebagian besar entity AIS menyalinnya (kadang dengan nama
 * kelas yang keliru). Untuk file ini nama kelasnya memang cocok.</p>
 *
 * @see ais.database.model.GeneralValueObject
 * @see ais.database.model.Jabatan
 * @see ais.database.model.Jurusan
 * @see ais.database.model.Fakultas
 * @see ais.database.dao.StaffDao
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "staff")

public class Staff extends GeneralValueObject {

	/**
	 * Kunci peran untuk jabatan Ketua Program Studi, nilai literal {@code "Kaprodi"}.
	 *
	 * <p>Dipakai di tiga tempat berbeda dengan <b>arti yang berbeda pula</b>, sehingga mudah
	 * membingungkan:</p>
	 * <ul>
	 *   <li>{@code Common.getKaprodi(Jurusan)} dan
	 *       {@code NewUiLaporanDaftarHadirUjianController} memakainya sebagai nilai kolom
	 *       {@link #getStaff()} yang dicari ({@code Restrictions.eq("staff", Staff.KAPRODI)}).</li>
	 *   <li>{@code InitDataHelper} memakainya sebagai <i>nama</i> baris
	 *       {@link ais.database.model.Jabatan} yang dicari/dibuat, lalu menyimpan objek
	 *       {@code Jabatan} hasilnya ke variabel statis {@code ConstantValues.KAPRODI}.
	 *       Jadi {@code Staff.KAPRODI} bertipe {@code String} sedangkan
	 *       {@code ConstantValues.KAPRODI} bertipe {@code Jabatan} -- nama sama, tipe beda.</li>
	 *   <li>{@code StaffAction} pernah memakainya untuk menambah item combo secara manual;
	 *       kode itu kini dikomentari (combo Jabatan diisi dari tabel
	 *       {@link ais.database.model.Jabatan}).</li>
	 * </ul>
	 * <p>Karena nilainya berkapital, pencarian dengan konstanta ini konsisten dengan nilai
	 * yang ditulis {@code JurusanAction} -- berbeda dari literal huruf kecil
	 * {@code "dekan"}/{@code "rektor"}/{@code "prodi"} yang dipakai laporan lain (lihat
	 * bagian BUG di JavaDoc kelas).</p>
	 */
	public static final String KAPRODI = "Kaprodi";

	/**
	 * Versi serialisasi Java. Nilainya dipertahankan apa adanya; mengubahnya akan memutus
	 * kompatibilitas deserialisasi objek {@code Staff} yang mungkin tersimpan di sesi ZK atau
	 * cache.
	 */
	private static final long serialVersionUID = -2420181318365653420L;

	/**
	 * Primary key baris, dibangkitkan database ({@code IDENTITY}). Dideklarasikan ulang di sini
	 * karena {@link GeneralValueObject} bukan {@code @MappedSuperclass} -- keharusan teknis.
	 * Selain sebagai identitas, urutannya dipakai sebagai tie-breaker "yang terbaru menang"
	 * oleh {@code Common.getKaprodi()} ({@code Order.desc("id")}).
	 */
	private Long id;

	/**
	 * Nama pengguna yang terakhir mengubah baris ini (jejak audit). Diisi otomatis oleh
	 * {@code AuditTimestampInterceptor}, bukan oleh layar. Dideklarasikan ulang karena induknya
	 * tidak terpetakan Hibernate.
	 */
	private String oleh;

	/**
	 * Identitas (id) pengguna yang terakhir mengubah baris ini (jejak audit), pendamping
	 * {@link #oleh}. Dideklarasikan ulang karena induknya tidak terpetakan Hibernate.
	 */
	private String olehId;

	/**
	 * @return id pengguna terakhir yang mengubah baris ini, atau {@code null} bila belum pernah
	 *         terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id pengguna pengubah terakhir.
	 *
	 * <p><b>Kuirk penting:</b> setter ini <b>mengabaikan</b> nilai {@code null} maupun string
	 * kosong/spasi -- nilai lama dipertahankan tanpa peringatan. Konsekuensinya jejak audit
	 * tidak pernah bisa "dikosongkan" secara sengaja, dan pemanggil yang mengira sudah
	 * menghapusnya akan salah. Pola pelindung ini seragam di seluruh entity AIS.</p>
	 *
	 * @param olehId id pengguna pengubah; {@code null}/kosong diabaikan diam-diam
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan nama pengguna pengubah terakhir.
	 *
	 * <p><b>Kuirk sama seperti {@link #setOlehId(String)}:</b> nilai {@code null} atau
	 * kosong/spasi diabaikan diam-diam sehingga nilai lama bertahan.</p>
	 *
	 * @param oleh nama pengguna pengubah; {@code null}/kosong diabaikan diam-diam
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * @return nama pengguna terakhir yang mengubah baris ini, atau {@code null} bila belum
	 *         pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate} dan deklarasi field {@link #tanggal_dirubah} -- keduanya
	 * berada pada satu baris fisik warisan gaya penulisan repo ini, sehingga JavaDoc ini
	 * mencakup keduanya sekaligus.
	 *
	 * <p><b>{@code onUpdate()}:</b> dipanggil Hibernate tepat sebelum pernyataan UPDATE baris
	 * ini di-flush, lalu meneruskan ke {@code AuditTimestampInterceptor.ubah(this)} yang
	 * memperbarui cap waktu dan pelaku audit standar AIS ({@link #tanggal_dirubah},
	 * {@link #oleh}, {@link #olehId}). Tidak pernah dipanggil manual dari kode aplikasi.
	 * Karena entity ini juga ditulis oleh alur sinkronisasi otomatis
	 * {@code FakultasAction}/{@code JurusanAction}, jejak audit yang tercatat bisa berupa
	 * pengguna yang sedang menyunting <i>Fakultas/Jurusan</i>, bukan layar Staff.</p>
	 *
	 * <p><b>{@code tanggal_dirubah}:</b> cap waktu perubahan terakhir, dideklarasikan ulang di
	 * sini karena {@link GeneralValueObject} bukan {@code @MappedSuperclass} (keharusan
	 * teknis). Diinisialisasi ke waktu sekarang saat objek dibuat -- termasuk untuk objek
	 * yang hanya sementara dan tidak jadi disimpan -- lalu diperbarui pada tiap UPDATE.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan cap waktu perubahan terakhir secara manual. Umumnya tidak perlu dipanggil
	 * karena {@link #onUpdate()} sudah mengisinya otomatis pada tiap UPDATE.
	 *
	 * @param tanggal_dirubah cap waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return cap waktu perubahan terakhir baris ini, dipetakan sebagai
	 *         {@code TIMESTAMP}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks objek, yaitu nama pejabat.
	 *
	 * <p><b>Perhatikan dua kuirk:</b></p>
	 * <ol>
	 *   <li>Method ini membaca <b>field {@code nama} milik {@link GeneralValueObject} secara
	 *       langsung</b>, bukan lewat {@link #getNama()} -- sehingga nilainya <b>tidak
	 *       di-{@code trim()}</b> dan bisa berbeda (mengandung spasi tepi) dari yang
	 *       dikembalikan getter.</li>
	 *   <li>Method ini dapat mengembalikan {@code null} bila nama belum diisi (mis. objek
	 *       hasil {@link #Staff()} yang belum disetel). Ini melanggar kontrak umum
	 *       {@code Object.toString()} dan dapat memicu {@code NullPointerException} atau label
	 *       kosong pada komponen ZK yang memanggilnya secara implisit.</li>
	 * </ol>
	 *
	 * @return nama pejabat apa adanya, mungkin {@code null}
	 */
	public String toString() {
		return nama;
	}

	/**
	 * Kunci peran/jabatan sebagai teks -- <b>kolom terpenting entity ini</b>. Berisi salinan
	 * {@code jabatan.nama} dan menjadi satu-satunya kriteria yang dipakai laporan untuk
	 * menemukan pejabat yang tepat (mis. {@code "Dekan"}, {@code "Kaprodi"}, {@code "rektor"},
	 * {@code "pudek 1"}). Lihat bagian "Kolom {@code staff}" dan "BUG NYATA" pada JavaDoc
	 * kelas untuk masalah duplikasi sumber kebenaran dan ketidakkonsistenan
	 * case-sensitivity-nya.
	 */
	private String staff;

	/**
	 * Nama pejabat yang dicetak di blok tanda tangan. Nilainya <b>salinan teks</b> dari
	 * {@code Dosen.getNama()} bila baris dibuat lewat sinkronisasi otomatis
	 * {@code FakultasAction}/{@code JurusanAction}, atau ketikan bebas bila lewat layar
	 * {@code StaffAction}. Tidak ada FK ke {@code Dosen}, sehingga salinan ini bisa basi.
	 *
	 * <p>Field ini <b>membayangi (shadow)</b> field bernama sama milik
	 * {@link GeneralValueObject}; {@link #toString()} membaca field induk, sedangkan
	 * {@link #getNama()} membaca field ini.</p>
	 */
	private String nama;

	/**
	 * NIP (Nomor Induk Pegawai) pejabat, dicetak di bawah namanya pada blok tanda tangan.
	 * Boleh {@code null} -- laporan menanganinya dengan mengganti string kosong. Sama seperti
	 * {@link #nama}, ini salinan teks dari {@code Dosen.getCode()} pada alur sinkronisasi
	 * otomatis, bukan FK.
	 */
	private String nip;

	/**
	 * Program studi tempat pejabat ini berlaku, atau {@code null} bila cakupannya bukan
	 * per-prodi. Dipakai sebagai filter tahap pertama pada kaskade pencarian laporan dan
	 * sebagai filter wajib pada {@code Common.getKaprodi(Jurusan)}.
	 */
	private Jurusan jurusan;

	/**
	 * Fakultas tempat pejabat ini berlaku, atau {@code null} bila cakupannya se-institusi
	 * (mis. Rektor). Dipakai sebagai filter tahap kedua pada kaskade pencarian laporan.
	 */
	private Fakultas fakultas;

	/**
	 * FK ke master {@link ais.database.model.Jabatan}. Menyimpan fakta yang sama dengan teks
	 * {@link #staff} namun sebagai relasi -- lihat catatan "dua sumber kebenaran" pada JavaDoc
	 * kelas. Dipakai sebagai kriteria pencarian oleh alur sinkronisasi
	 * {@code FakultasAction}/{@code JurusanAction} (yang mencari lewat FK, bukan teks),
	 * sedangkan seluruh laporan mencari lewat teks.
	 */
	private Jabatan jabatan;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JavaBeans. Juga dipakai langsung oleh
	 * {@code StaffAction.onAdd()} (baris baru dari layar), oleh alur sinkronisasi
	 * {@code FakultasAction}/{@code JurusanAction}, serta oleh laporan transkrip yang
	 * menyisipkan baris placeholder saat pencarian nihil. Satu-satunya nilai default yang
	 * dipasang adalah {@link #tanggal_dirubah}.
	 */
	public Staff() {
	}

	/**
	 * Konstruktor kolom-wajib ({@code NOT NULL}) hasil generator hbm2java.
	 *
	 * <p><b>Catatan:</b> tidak ada pemanggil konstruktor ini di codebase saat ini -- seluruh
	 * kode memakai {@link #Staff()} lalu setter. Perhatikan juga bahwa konstruktor ini
	 * menugaskan {@code this.nama} yaitu field milik kelas ini, sehingga {@link #toString()}
	 * (yang membaca field induk) tetap mengembalikan {@code null} untuk objek yang dibuat
	 * lewat sini.</p>
	 *
	 * @param staff kunci peran/jabatan sebagai teks (kolom {@code staff}, {@code NOT NULL})
	 * @param nama  nama pejabat (kolom {@code nama}, {@code NOT NULL})
	 */
	public Staff(String staff, String nama) {
		this.staff = staff;
		this.nama = nama;
	}

	/**
	 * @return primary key baris; {@code null} untuk objek yang belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan primary key. Hanya dipanggil Hibernate; kode aplikasi memakai
	 * {@code getId() != null} untuk membedakan tambah vs ubah
	 * ({@code StaffAction.onSave()}).
	 *
	 * @param id primary key baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kunci peran/jabatan sebagai teks -- nilai yang dibandingkan seluruh laporan
	 * untuk memilih pejabat penanda tangan.
	 *
	 * <p>Tidak di-{@code trim()} dan tidak dinormalkan huruf besar/kecilnya, padahal sebagian
	 * pemanggil membandingkannya secara case-sensitive; lihat bagian "BUG NYATA" pada JavaDoc
	 * kelas.</p>
	 *
	 * @return kunci peran (mis. {@code "Dekan"}, {@code "Kaprodi"}); kolom {@code NOT NULL}
	 *         sehingga baris tersimpan selalu berisi nilai
	 */
	@Column(name = "staff", nullable = false)
	public String getStaff() {
		return this.staff;
	}

	/**
	 * Menetapkan kunci peran/jabatan.
	 *
	 * <p>Seluruh pemanggil produksi mengisinya dengan {@code jabatan.getNama()} -- yaitu
	 * menduplikasi nama master {@link ais.database.model.Jabatan} ke dalam kolom teks ini.
	 * Karena tidak ada normalisasi, nilai yang tersimpan mengikuti persis kapitalisasi nama
	 * jabatan yang dipilih pengguna.</p>
	 *
	 * @param staff kunci peran baru
	 */
	public void setStaff(String staff) {
		this.staff = staff;
	}

	/**
	 * Mengembalikan nama pejabat, sudah di-{@code trim()}.
	 *
	 * <p><b>Diverifikasi tidak destruktif:</b> hasil {@code trim()} hanya dikembalikan dan
	 * <b>tidak</b> ditugaskan kembali ke field, jadi getter ini tidak memicu {@code UPDATE}
	 * saat entity ter-flush -- berbeda dari pola "getter menulis balik" yang ditemukan di
	 * {@code Kota}/{@code Penghasilan}/{@code ItemBiayaPunyaAkun}.</p>
	 *
	 * @return nama pejabat tanpa spasi tepi, atau {@code null} bila field-nya {@code null}
	 *         (meskipun kolomnya {@code NOT NULL}, objek yang belum tersimpan bisa
	 *         mengembalikan {@code null})
	 */
	@Column(name = "nama", nullable = false)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan nama pejabat. Tidak ada validasi maupun {@code trim()} di sini; pemeriksaan
	 * "wajib diisi" hanya dilakukan di lapisan UI ({@code StaffAction.onSave()}), sehingga
	 * jalur non-ZK dapat menyimpan nama kosong.
	 *
	 * @param nama nama pejabat
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Menetapkan NIP pejabat. Tanpa validasi format apa pun.
	 *
	 * @param nip NIP baru; boleh {@code null} atau kosong
	 */
	public void setNip(String nip) {
		this.nip = nip;
	}

	/**
	 * @return NIP pejabat apa adanya (tanpa {@code trim()}), atau {@code null} bila tidak diisi
	 */
	@Column(name = "nip", nullable = true)
	public String getNip() {
		return nip;
	}

	/**
	 * Menetapkan program studi cakupan pejabat ini.
	 *
	 * @param jurusan program studi, atau {@code null} bila cakupannya bukan per-prodi
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * Mengembalikan program studi cakupan pejabat ini.
	 *
	 * <p>Relasi {@code ManyToOne} dengan {@code cascade = MERGE, PERSIST}: menyimpan sebuah
	 * {@code Staff} ikut mem-persist/merge objek {@link ais.database.model.Jurusan} yang
	 * tertaut. {@code FetchMode.SELECT} berarti dimuat lewat query terpisah saat pertama
	 * diakses (lazy), sehingga membacanya di luar sesi Hibernate yang masih terbuka dapat
	 * memicu {@code LazyInitializationException}.</p>
	 *
	 * @return program studi cakupan, atau {@code null} bila tidak dibatasi per-prodi
	 */
	@ManyToOne(cascade = { CascadeType.MERGE, CascadeType.PERSIST })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jurusan")
	public Jurusan getJurusan() {
		return jurusan;
	}

	/**
	 * Menetapkan jabatan struktural pejabat ini.
	 *
	 * <p>Perhatikan bahwa memanggil setter ini saja <b>tidak</b> memperbarui kolom teks
	 * {@link #setStaff(String)}; kedua nilai harus diset berpasangan agar tidak terjadi drift.
	 * Seluruh pemanggil produksi memang melakukannya berpasangan.</p>
	 *
	 * @param jabatan baris master jabatan
	 */
	public void setJabatan(Jabatan jabatan) {
		this.jabatan = jabatan;
	}

	/**
	 * Mengembalikan jabatan struktural pejabat ini sebagai relasi ke master
	 * {@link ais.database.model.Jabatan}.
	 *
	 * <p>Relasi {@code ManyToOne}, {@code cascade = MERGE, PERSIST}, {@code FetchMode.SELECT}
	 * (lazy, query terpisah). Dipakai sebagai kriteria pencarian oleh alur sinkronisasi
	 * {@code FakultasAction}/{@code JurusanAction}; laporan justru mengabaikannya dan memakai
	 * teks {@link #getStaff()}.</p>
	 *
	 * @return baris master jabatan, atau {@code null} untuk baris lama/placeholder yang dibuat
	 *         laporan (jalur itu hanya mengisi {@link #setStaff(String)}, tidak FK ini)
	 */
	@ManyToOne(cascade = { CascadeType.MERGE, CascadeType.PERSIST })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jabatan")
	public Jabatan getJabatan() {
		return jabatan;
	}

	/**
	 * Menetapkan fakultas cakupan pejabat ini.
	 *
	 * @param fakultas fakultas, atau {@code null} bila cakupannya se-institusi
	 */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	/**
	 * Mengembalikan fakultas cakupan pejabat ini.
	 *
	 * <p>Relasi {@code ManyToOne}, {@code cascade = MERGE, PERSIST}, {@code FetchMode.SELECT}
	 * (lazy, query terpisah). Menjadi filter tahap kedua pada kaskade pencarian laporan
	 * transkrip (jurusan &rarr; fakultas &rarr; tanpa filter).</p>
	 *
	 * <p><b>Diverifikasi tidak menulis balik:</b> berbeda dari
	 * {@code ItemBiayaPunyaAkun.getFakultas()} yang menurunkan nilainya dari {@code jurusan}
	 * lalu menimpa field, getter ini murni mengembalikan field apa adanya. Konsekuensinya
	 * baris dengan {@link #getJurusan()} terisi tetapi {@code fakultas} kosong <b>tidak</b>
	 * akan cocok pada pencarian tahap kedua meskipun prodinya jelas bernaung di fakultas
	 * tertentu -- kaskade langsung jatuh ke tahap ketiga (tanpa filter).</p>
	 *
	 * @return fakultas cakupan, atau {@code null} bila tidak dibatasi per-fakultas
	 */
	@ManyToOne(cascade = { CascadeType.MERGE, CascadeType.PERSIST })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "fakultas")
	public Fakultas getFakultas() {
		return fakultas;
	}

}
