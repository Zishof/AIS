package ais.database.model.sekolah;
// Generated 30 Sep 18 6:57:43 by Hibernate Tools 5.2.3.Final

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
 * Daftar peserta (<i>whitelist</i>) sebuah pengaturan biaya sekolah yang ditandai
 * &quot;khusus buat siswa tertentu&quot;: satu baris = satu pasangan
 * <i>(pengaturan biaya, siswa)</i> atau <i>(pengaturan biaya, calon siswa)</i>.
 *
 * <h2>Domain yang TERVERIFIKASI</h2>
 * <p>Entity ini adalah tabel penghubung murni antara {@link ais.database.model.sekolah.PengaturanBiaya}
 * (satu paket tarif pada mesin billing sekolah &mdash; lihat
 * {@link ais.database.model.sekolah.PengaturanBiayaItemBiaya} untuk sisi item/nominalnya) dan
 * {@link ais.database.model.sekolah.Siswa} / {@link ais.database.model.sekolah.CalonSiswa}.</p>
 *
 * <p><b>Ini BUKAN penetapan tarif individual, BUKAN diskon, dan BUKAN pengecualian per siswa.</b>
 * Tidak ada satu pun kolom nominal, persentase, atau potongan pada tabel ini. Besaran uang tetap
 * berada di {@code PengaturanBiayaItemBiaya} &rarr; {@link ais.database.model.sekolah.NominalBiaya}
 * &rarr; {@link ais.database.model.sekolah.Tagihan}. Yang ditentukan tabel ini hanyalah
 * <b>SIAPA yang termasuk cakupan</b> sebuah pengaturan biaya. Diskon per siswa dikelola entity lain
 * ({@code DiskonSiswa}, lewat {@code AmbilDataSiswaForDiskonSiswaHelper}).</p>
 *
 * <p>Baris di sini hanya bermakna bila {@code PengaturanBiaya.getKhususBuatSiswaTertentu()} bernilai
 * {@code true}. Pada mode normal, cakupan pengaturan biaya ditentukan secara <i>deklaratif</i> oleh
 * kombinasi kelas / kelas les / asrama / jurusan ({@link ais.database.model.sekolah.PenjurusanSekolah})
 * / status awal siswa / tahun angkatan / gelombang PSB. Mencentang &quot;Khusus buat siswa tertentu&quot;
 * mematikan seluruh penyaring deklaratif itu dan menggantinya dengan daftar eksplisit pada tabel ini.
 * Konsekuensinya bersifat finansial langsung: <b>terdaftar di sini = ditagih</b>, tidak terdaftar =
 * tidak ditagih (dengan pengecualian penting yang dirinci pada <a href="#kuirk4">kuirk 4</a>).</p>
 *
 * <h2>Dua kaki relasi, satu terisi per baris</h2>
 * <p>{@link #getSiswa()} dan {@link #getCalonSiswa()} keduanya <i>nullable</i>. Satu baris dipakai
 * untuk siswa terdaftar <b>atau</b> calon siswa PPDB, tidak pernah keduanya sekaligus: seluruh
 * penulis hanya mengisi salah satu, dan seluruh pembaca menyaring dengan
 * {@code isNotNull("siswa")} atau {@code isNotNull("calonSiswa")}. Pemisahan itu mengikuti
 * {@code JenisBiayaSekolah.gunakanCalonSiswa} &mdash; satu jenis biaya melayani siswa saja atau
 * calon siswa saja. Tidak ada <i>constraint</i> basis data yang menegakkan aturan ini; ia hanya
 * konvensi kode.</p>
 *
 * <h2>Jalur baris ini DIBUAT (empat, semuanya menambah, tidak ada yang menghapus)</h2>
 * <ol>
 *   <li><b>Picker massal siswa.</b> Tombol &quot;Ambil Siswa&quot; pada panel detail pengaturan
 *   biaya ({@code DetailTagihanSiswaHelper}) &rarr;
 *   {@code AmbilDataSiswaForPengaturanBiayaHelper#save()}. Centang bertahan lintas halaman dan
 *   lintas pencarian.</li>
 *   <li><b>Picker massal calon siswa.</b> Kembarannya,
 *   {@code AmbilDataCalonSiswaForPengaturanBiayaHelper#save()}, dipanggil dari
 *   {@code DetailTagihanCalonSiswaHelper}.</li>
 *   <li><b>Impor Excel.</b> Unggahan berkas pada kedua panel detail di atas: begitu satu baris
 *   Excel berhasil dicocokkan ke siswa/calon siswa (lewat nomor induk, lalu nama), keanggotaan
 *   dibuat <i>otomatis</i> bila belum ada, lalu tagihannya langsung dibentuk.</li>
 *   <li><b>Efek samping entri pembayaran manual.</b>
 *   {@code PembayaranSiswaAction} &mdash; saat kasir menyimpan pembayaran atas sebuah pengaturan
 *   biaya bermode khusus, siswa yang bersangkutan <b>otomatis dimasukkan</b> ke daftar ini bila
 *   belum terdaftar. Jadi keanggotaan bisa lahir tanpa ada yang pernah membuka layar pengelolaan
 *   daftar peserta.</li>
 * </ol>
 * <p>Ketiadaan jalur penghapusan bukan kelalaian dokumentasi: penelusuran seluruh codebase tidak
 * menemukan satu pun {@code delete}/{@code refreshDelete} atas kelas ini. Lihat
 * <a href="#kuirk1">kuirk 1</a>.</p>
 *
 * <h2>Jalur baris ini DIBACA</h2>
 * <ul>
 *   <li>{@code DetailTagihanSiswaHelper#initCriteriaDenganNama(...)} dan
 *   {@code DetailTagihanCalonSiswaHelper#initCriteria(...)} &mdash; mempersempit daftar siswa/calon
 *   siswa sebuah pengaturan biaya menjadi tepat anggota daftar ini (bila daftar kosong, kriteria
 *   dipaksa {@code false} sehingga tidak ada siapa pun yang ditagih).</li>
 *   <li>{@code DetailTagihanSiswaHelper#apakahAda(...)} /
 *   {@code DetailTagihanCalonSiswaHelper#apakahAda(...)} &mdash; penjaga per-siswa yang dipakai
 *   layar/laporan/API pembayaran: {@code TagihanSiswa} (API mobile), {@code PsbCalonApi},
 *   {@code NewUiPemOnlineController}, {@code PembayaranOnline}, {@code WizardPembayaranSiswaHelper},
 *   {@code CommonReportHelper}, dan {@code TampilanPengumumanAkademisAction}.</li>
 *   <li>{@code AnalisisTagihanSekolahHelper} &mdash; alat diagnosis &quot;mengapa tagihan ini
 *   muncul/tidak muncul&quot;; menampilkan keanggotaan sebagai salah satu tahap penelusuran.</li>
 *   <li>{@code CommonUiFactoryHelper} &mdash; menghitung referensi ke sebuah siswa dan
 *   <b>memblokir penghapusan siswa</b> selama masih ada baris di tabel ini (FK {@code siswa} memang
 *   tidak ber-{@code ON DELETE CASCADE}, berbeda dari FK {@code calon_siswa} yang di
 *   {@code cascade.sql} justru diberi {@code ON DELETE CASCADE}).</li>
 * </ul>
 *
 * <h2>Pengelompokan method</h2>
 * <ul>
 *   <li><b>Identitas:</b> {@link #getId()}, {@link #setId(Long)}, dan konstruktor
 *   {@link #PengaturanBiayaPunyaSiswa()}.</li>
 *   <li><b>Relasi inti (kunci penghubung):</b> {@link #getPengaturanBiaya()},
 *   {@link #setPengaturanBiaya(PengaturanBiaya)}, {@link #getSiswa()}, {@link #setSiswa(Siswa)},
 *   {@link #getCalonSiswa()}, {@link #setCalonSiswa(CalonSiswa)}.</li>
 *   <li><b>Jejak audit (warisan yang dideklarasikan ulang):</b> {@link #getOleh()},
 *   {@link #setOleh(String)}, {@link #getOlehId()}, {@link #setOlehId(String)},
 *   {@link #getTanggal_dirubah()}, {@link #setTanggal_dirubah(Date)}, {@link #onUpdate()}.</li>
 *   <li><b>Kolom sisa salin-tempel (kode mati):</b> {@link #getKeterangan()},
 *   {@link #setKeterangan(String)} &mdash; lihat <a href="#kuirk3">kuirk 3</a>.</li>
 * </ul>
 *
 * <h2>Catatan teknis: kenapa {@code id}/{@code oleh}/{@code olehId}/{@code tanggal_dirubah}
 * dideklarasikan ulang</h2>
 * <p>{@link ais.database.model.GeneralValueObject} <b>bukan</b> {@code @Entity} maupun
 * {@code @MappedSuperclass} &mdash; ia POJO abstrak biasa, sehingga Hibernate tidak memetakan satu
 * pun properti induknya. Pengulangan deklarasi keempat anggota itu di sini adalah
 * <b>keharusan teknis</b> agar kolom bersangkutan benar-benar terpetakan, bukan duplikasi yang
 * perlu &quot;dibersihkan&quot;. Rinciannya ada di Javadoc kelas induk; lihat
 * {@link ais.database.model.GeneralValueObject}.</p>
 *
 * <h2>Kuirk &amp; temuan</h2>
 * <ol>
 *   <li id="kuirk1"><b>Keanggotaan hanya bisa DITAMBAH, tidak pernah bisa DICABUT lewat UI.</b>
 *   Pada picker massal, checkbox baris yang sudah terdaftar dirender
 *   {@code setChecked(true)} sekaligus {@code setDisabled(true)}, sehingga secara harfiah tidak
 *   bisa dilepas; panel detail pun tidak menyediakan tombol hapus untuk baris ini. Salah centang
 *   pada satu siswa karena itu bersifat permanen dari sisi aplikasi: siswa tersebut selamanya
 *   ditagih oleh pengaturan biaya itu, dan koreksinya hanya mungkin lewat SQL manual (atau dengan
 *   menghapus siswanya &mdash; yang justru diblokir {@code CommonUiFactoryHelper}, lihat di atas).
 *   Karena sifat finansialnya, ini kelemahan operasional yang layak diperbaiki.</li>
 *
 *   <li id="kuirk2"><b>Anti-duplikat hanya di lapisan aplikasi, dan rawan balapan.</b> Keempat
 *   jalur penulis memakai pola &quot;hitung dulu, simpan bila nol&quot;. Indeks basis data yang
 *   dibuat {@code ais.common.InitIndex} atas {@code (pengaturan_biaya, siswa)} dan
 *   {@code (pengaturan_biaya, calon_siswa)} adalah indeks <b>biasa, bukan {@code UNIQUE}</b>.
 *   Dua operator yang menyimpan bersamaan &mdash; atau satu impor Excel yang memuat NIS yang sama
 *   dua kali &mdash; dapat menghasilkan baris kembar. Dampaknya bukan tagihan ganda (pembaca
 *   memakai {@code groupProperty}/{@code rowCount() &gt; 0}), melainkan sekadar kotoran data.</li>
 *
 *   <li id="kuirk3"><b>{@code keterangan} adalah kolom mati.</b> Penelusuran seluruh codebase tidak
 *   menemukan satu pun pemanggilan {@link #setKeterangan(String)} maupun {@link #getKeterangan()}
 *   atas objek bertipe kelas ini; tidak ada layar yang menampilkannya. Kolom ini sisa salin-tempel
 *   dari keluarga entity {@code ...PunyaSiswa} lain (bandingkan
 *   {@link ais.database.model.sekolah.AsramaSiswaPunyaSiswa} yang punya lima kolom sisa serupa).
 *   Padahal justru kolom inilah yang seharusnya dipakai mencatat <i>alasan</i> seorang siswa
 *   dimasukkan ke tarif khusus &mdash; informasi yang saat ini hilang sama sekali.</li>
 *
 *   <li id="kuirk4"><b>Daftar ini TIDAK ditegakkan pada seluruh jalur pembentukan tagihan.</b>
 *   {@code Tagihan.getBoleh(...)} &mdash; penjaga yang menentukan boleh-tidaknya sebuah tagihan
 *   dibentuk/diaktifkan &mdash; sama sekali tidak membaca tabel ini; sebaliknya, ketika
 *   {@code khususBuatSiswaTertentu} bernilai {@code true} ia justru <i>melewati</i> penyaring
 *   jurusan dan status awal siswa. Hal yang sama berlaku pada kriteria
 *   {@code PengaturanBiaya.terapkanFilterPembayaran(...)} yang meng-OR-kan
 *   {@code khususBuatSiswaTertentu = true} sehingga pengaturan biaya bermode khusus lolos dari
 *   penyaring kelas/angkatan/status. Penegakan daftar peserta seluruhnya bergantung pada pemanggil
 *   yang menambahkan {@code apakahAda(...)} secara manual. Enam pemanggil melakukannya; dua
 *   <b>tidak</b>:
 *   <ul>
 *     <li>{@code TagihanUtilCalonSiswa#doGenerateTagihanInsendentil(CalonSiswa, JenisBiayaSekolah,
 *     boolean)} hanya menyaring dengan {@code Tagihan.getBoleh(...)}, sehingga
 *     <b>membentuk tagihan nyata untuk calon siswa yang tidak terdaftar di daftar peserta</b>.
 *     Jalur ini dipicu dari {@code PsbApi}, {@code SiswaBaruApi}, dan
 *     {@link ais.database.model.sekolah.CalonSiswa} sendiri.</li>
 *     <li>{@code GelombangPendaftaranPsb#chekSyaratBayar(...)} memakai daftar pengaturan biaya
 *     hasil {@code terapkanFilterPembayaran} apa adanya, sehingga tagihan &quot;hantu&quot; di atas
 *     dapat <b>memblokir kemajuan calon siswa</b> pada gerbang syarat lunas PPDB.</li>
 *   </ul>
 *   Akibatnya tagihan hantu tersebut tetap {@code aktif} (karena {@code Tagihan.getAktif()} juga
 *   bersandar pada {@code getBoleh(...)}), namun tidak terlihat pada layar/API yang menyaring benar
 *   &mdash; sehingga muncul sebagai tunggakan yang tidak bisa dijelaskan. Ini bug integritas data
 *   finansial, bukan kerentanan akses.</li>
 *
 *   <li id="kuirk5"><b>Bom waktu {@code getKhususBuatSiswaTertentu()} yang destruktif.</b> Getter
 *   itu pada {@code PengaturanBiaya} memaksa nilainya menjadi {@code false} setiap kali
 *   {@code kelasLesSiswa} tidak {@code null}. Karena Hibernate memetakan {@code PengaturanBiaya}
 *   dengan <i>property access</i>, penulisan balik itu bisa ikut ter-flush ke basis data. Bila
 *   sebuah pengaturan biaya khusus kemudian dikaitkan ke satu kelas les, seluruh baris tabel ini
 *   menjadi yatim dalam senyap dan pengaturan biaya berubah menjadi tarif massal &mdash; menagih
 *   jauh lebih banyak siswa daripada yang pernah dicentang operator.</li>
 *
 *   <li id="kuirk6"><b>Jejak audit terisi hanya separuh.</b> Dari empat jalur penulis, hanya kedua
 *   picker massal yang memanggil {@link #setOleh(String)}. Impor Excel dan efek samping
 *   {@code PembayaranSiswaAction} menyimpan baris tanpa {@code oleh}/{@code olehId} sama sekali,
 *   sehingga kolom &quot;siapa yang memasukkan siswa ini ke tarif khusus&quot; kosong justru pada
 *   jalur massal yang paling berdampak. Yang tersisa hanyalah revisi Envers ({@code @Audited}).</li>
 *
 *   <li id="kuirk7"><b>{@code toString()} tidak di-override.</b> Yang berlaku adalah
 *   {@code GeneralValueObject#toString()} yang merangkai {@code "kode - nama"}; kedua properti itu
 *   tidak dideklarasikan ulang di sini sehingga tidak pernah dipetakan dan selalu {@code null} &mdash;
 *   {@code toString()} entity ini selalu menghasilkan string {@code "null"}. Tidak berdampak hari ini
 *   (seluruh label UI diambil dari {@code getSiswa().getNama()}), tetapi membuat log tidak informatif.</li>
 *
 *   <li id="kuirk8"><b>Terdaftar sebagai kandidat CRUD generik.</b> Berkas manifest
 *   {@code WEB-INF/generic-crud/manifests/general_value_object_inventory.csv} menandai kelas ini
 *   {@code ELIGIBLE_METADATA_FIRST} dengan label layar &quot;Pengaturan Biaya Punya Siswa&quot;.
 *   Bila layar itu benar-benar dibangkitkan tanpa gerbang hak akses dan tanpa penyaring tenant, ia
 *   menjadi jalur langsung untuk menambah/menghapus keanggotaan tarif lintas sekolah. Statusnya
 *   saat ini masih <i>disabled</i>; catat sebagai risiko masa depan.</li>
 * </ol>
 *
 * <h2 id="akses">Hasil verifikasi kontrol akses (data keuangan siswa)</h2>
 * <ol>
 *   <li><b>Layar induk bergerbang benar.</b> {@code PengaturanBiayaAction} memanggil
 *   {@code CommonPrivilages.checkPrevilages(CREATE/UPDATE/APPROVE/DELETE)} dan menurunkan hasilnya
 *   sebagai {@code edit}/{@code approve} ke panel detail. Panel detail pun memakai {@code edit} itu
 *   secara konsisten pada belasan komponen sel grid ({@code setDisabled(!edit)}).</li>
 *   <li><b>TAPI tombol &quot;Ambil Siswa&quot; TIDAK ikut digerbangi.</b> Pada
 *   {@code DetailTagihanSiswaHelper} (dan kembarannya untuk calon siswa) tombol itu dibuat hanya
 *   dengan syarat {@code pengaturanBiaya.getKhususBuatSiswaTertentu()}, tanpa memeriksa
 *   {@code edit}. Hal yang sama berlaku untuk tombol massal &quot;Sinkronkan&quot;,
 *   &quot;Recovery&quot;, dan &quot;Upload&quot; (impor Excel). Artinya pengguna berhak BACA saja
 *   tetap dapat memasukkan siswa ke tarif khusus dan memicu pembentukan tagihan &mdash; menciptakan
 *   kewajiban finansial atas nama siswa. Ini <i>broken access control</i> pada data keuangan
 *   personal, bukan sekadar kosmetik.</li>
 *   <li><b>Fail-open cakupan tenant.</b> {@code PengaturanBiayaAction.initCriteria(...)} tidak
 *   memiliki penyaring sekolah/yayasan yang wajib &mdash; keduanya hanya filter pencarian opsional
 *   ({@code 1=1} bila kosong), sehingga pengaturan biaya milik seluruh sekolah/yayasan pada satu
 *   instalasi terlihat oleh siapa pun yang bisa membuka menu. Selaras dengan itu,
 *   {@code AmbilDataSiswaForPengaturanBiayaHelper#initCriteria(boolean)} mengunci combo
 *   yayasan/sekolah <b>hanya bila</b> pengaturan biaya sudah menetapkannya; pada pengaturan biaya
 *   tanpa sekolah, dialog menelusuri SELURUH siswa instalasi (NIS, nama, tahun angkatan) dan dapat
 *   menuliskan keanggotaan atas siswa sekolah lain. Satu-satunya penyempit berbasis pengguna adalah
 *   pembatasan khusus akun orang tua ke daftar anaknya sendiri.</li>
 *   <li><b>Pembacaan nilai tarif lewat endpoint umum: TIDAK ditemukan celah pada jalur ini
 *   (temuan POSITIF).</b> {@code ais.action.servlet.api.TagihanSiswa} &mdash; endpoint yang
 *   membacakan nominal tagihan &mdash; menolak permintaan tanpa token sesi valid, memetakan token
 *   ke entitas siswa/calon siswa milik token itu sendiri, dan menyaring tiap pengaturan biaya
 *   dengan {@code apakahAda(...)}. Pola &quot;nilai keuangan terbaca lewat {@code /Data}/{@code /Api}
 *   tanpa otorisasi&quot; yang berulang di modul keuangan lain ({@code task_493423ef})
 *   <b>tidak</b> terkonfirmasi untuk data tabel ini.</li>
 *   <li><b>Pewarisan hak lewat menu induk:</b> tidak berlaku di sini &mdash; layar pengelola daftar
 *   peserta bukan {@code .zul} tersendiri, melainkan panel yang selalu dibuka dari menu Pengaturan
 *   Biaya Sekolah. Yang bermasalah bukan menu induknya, melainkan tombol tanpa gerbang di butir 2.</li>
 * </ol>
 *
 * @see ais.database.model.sekolah.PengaturanBiaya
 * @see ais.database.model.sekolah.PengaturanBiayaItemBiaya
 * @see ais.database.model.sekolah.Siswa
 * @see ais.database.model.sekolah.CalonSiswa
 * @see ais.database.model.sekolah.Tagihan
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(name = "pengaturan_biaya_punya_siswa", schema = "sekolah")
public class PengaturanBiayaPunyaSiswa extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya identik dengan sejumlah entity modul sekolah lain karena
	 * seluruh berkas itu lahir dari salin-tempel yang sama &mdash; bukan penanda kompatibilitas
	 * serialisasi yang dirancang. Jangan diubah: objek entity AIS diserialkan ke cache dan ke sesi
	 * ZK, sehingga mengubahnya membuat data ter-cache lama tidak terbaca.
	 */
	private static final long serialVersionUID = -9157912161411433979L;
	/** Kunci primer {@code sekolah.pengaturan_biaya_punya_siswa.id}; lihat {@link #getId()}. */
	private Long id;
	/** Nama/user id pengguna pengubah terakhir; lihat {@link #getOleh()} dan <a href="#kuirk6">kuirk 6</a>. */
	private String oleh;
	/** Id teknis pengguna pengubah terakhir; lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan id teknis pengguna pengubah terakhir.
	 *
	 * @return nilai {@code olehId}, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id teknis pengguna pengubah terakhir.
	 *
	 * <p>Nilai {@code null} atau kosong diabaikan diam-diam sehingga jejak audit lama tidak bisa
	 * terhapus tanpa sengaja.</p>
	 *
	 * @param olehId id pengguna baru; diabaikan bila {@code null} atau kosong setelah di-{@code trim}
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama/user id pengguna pengubah terakhir.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, nilai {@code null} atau kosong diabaikan
	 * diam-diam. Dipanggil eksplisit oleh {@code AmbilDataSiswaForPengaturanBiayaHelper#save()} dan
	 * {@code AmbilDataCalonSiswaForPengaturanBiayaHelper#save()}; dua jalur penulis lainnya (impor
	 * Excel dan {@code PembayaranSiswaAction}) tidak memanggilnya &mdash; lihat
	 * <a href="#kuirk6">kuirk 6</a>.</p>
	 *
	 * @param oleh nama/user id baru; diabaikan bila {@code null} atau kosong setelah di-{@code trim}
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama/user id pengguna pengubah terakhir.
	 *
	 * @return nilai {@code oleh}, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil Hibernate tepat sebelum baris ini di-{@code UPDATE}.
	 *
	 * <p><b>Efek samping:</b> mendelegasikan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} yang mengisi
	 * {@link #getOleh()}/{@link #getOlehId()} dari pengguna sesi aktif dan menyegarkan
	 * {@link #getTanggal_dirubah()}. Tidak ada {@code @PrePersist}, sehingga baris baru hanya
	 * mengandalkan nilai awal field {@link #tanggal_dirubah}.</p>
	 *
	 * <p>Dalam praktiknya callback ini nyaris tidak pernah berjalan: baris tabel ini hanya pernah
	 * di-{@code INSERT}, tidak ada satu pun jalur aplikasi yang mengubah atau menghapusnya (lihat
	 * <a href="#kuirk1">kuirk 1</a>). Kelas ini {@code @Audited}, jadi setiap {@code UPDATE} yang
	 * memang terjadi juga melahirkan satu revisi Envers.</p>
	 *
	 * <p>Perhatikan bahwa deklarasi field {@link #tanggal_dirubah} sengaja berbagi baris dengan
	 * method ini persis seperti pada seluruh entity turunan lain (hasil penyuntingan massal); itu
	 * bukan kekeliruan sintaks.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     /**
	 * Waktu perubahan terakhir. Diinisialisasi ke waktu server ({@code ais.ui.util.WaktuUtil}) saat
	 * instance dibuat, sehingga baris yang baru di-{@code INSERT} pun sudah punya stempel waktu
	 * meski {@link #onUpdate()} belum pernah berjalan &mdash; pada tabel ini nilai itulah yang
	 * praktis selalu bertahan, karena baris tidak pernah diubah lagi.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir.
	 *
	 * <p>Berbeda dari {@link #setOleh(String)}/{@link #setOlehId(String)}, setter ini menerima
	 * {@code null} apa adanya.</p>
	 *
	 * @param tanggal_dirubah stempel waktu baru; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir (kolom {@code tanggal_dirubah}, dipetakan sebagai
	 * {@code TIMESTAMP}).
	 *
	 * @return stempel waktu perubahan terakhir; tidak pernah {@code null} pada objek yang baru
	 *         dibuat di memori, dapat {@code null} pada baris lama hasil impor langsung
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Pengaturan biaya yang cakupannya dibatasi oleh baris ini; wajib terisi. Lihat {@link #getPengaturanBiaya()}. */
	private PengaturanBiaya pengaturanBiaya;
	/** Siswa terdaftar yang termasuk cakupan, atau {@code null} bila baris ini untuk calon siswa. Lihat {@link #getSiswa()}. */
	private Siswa siswa;
	/** Calon siswa PPDB yang termasuk cakupan, atau {@code null} bila baris ini untuk siswa terdaftar. Lihat {@link #getCalonSiswa()}. */
	private CalonSiswa calonSiswa;
	/** Kolom sisa salin-tempel yang tidak pernah dibaca maupun ditulis; lihat <a href="#kuirk3">kuirk 3</a>. */
	private String keterangan;

	/**
	 * Konstruktor kosong wajib Hibernate/JPA.
	 *
	 * <p>Dipakai juga langsung oleh keempat jalur penulis, yang selalu melanjutkannya dengan
	 * {@link #setPengaturanBiaya(PengaturanBiaya)} plus salah satu dari {@link #setSiswa(Siswa)} /
	 * {@link #setCalonSiswa(CalonSiswa)} sebelum {@code session.save(...)}.</p>
	 */
	public PengaturanBiayaPunyaSiswa() {
	}

	/**
	 * Mengembalikan kunci primer baris ini.
	 *
	 * <p>Kolom {@code id} dipetakan {@code insertable = false} dan dibangkitkan basis data
	 * ({@code IDENTITY}/sekuens), jadi nilainya baru terisi setelah {@code flush}.</p>
	 *
	 * @return id baris, atau {@code null} bila entity belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci primer baris ini.
	 *
	 * <p>Hanya dipakai Hibernate; kode aplikasi tidak boleh menetapkannya sendiri.</p>
	 *
	 * @param id kunci primer baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan keterangan bebas baris ini.
	 *
	 * <p><b>Kode mati:</b> tidak ada layar, laporan, maupun API yang membaca kolom ini &mdash; lihat
	 * <a href="#kuirk3">kuirk 3</a>.</p>
	 *
	 * @return isi kolom {@code keterangan}; dalam praktiknya selalu {@code null}
	 */
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menyetel keterangan bebas baris ini.
	 *
	 * <p><b>Kode mati:</b> tidak pernah dipanggil dari mana pun (lihat
	 * <a href="#kuirk3">kuirk 3</a>).</p>
	 *
	 * @param keterangan teks keterangan baru; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan pengaturan biaya yang cakupannya dipersempit oleh baris ini.
	 *
	 * <p>Kolom {@code pengaturan_biaya} bersifat {@code nullable = false}, jadi kaki relasi ini
	 * selalu terisi pada baris yang tersimpan. Relasi {@code LAZY}; nilainya dilewatkan
	 * {@code GeneralValueObject.check(...)} lebih dulu sehingga proxy Hibernate yang sudah lepas
	 * dari sesi tetap dapat dipakai (materialisasi ulang lewat cache/{@code EntityIdentityMap}).
	 * Pemanggilan pertama karena itu dapat memicu satu kueri tambahan.</p>
	 *
	 * @return pengaturan biaya pemilik baris ini; tidak pernah {@code null} pada baris tersimpan
	 * @see ais.database.model.GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pengaturan_biaya", nullable = false)
	public PengaturanBiaya getPengaturanBiaya() {
		pengaturanBiaya = check(pengaturanBiaya);
		return pengaturanBiaya;
	}

	/**
	 * Menyetel pengaturan biaya pemilik baris ini.
	 *
	 * <p>Wajib dipanggil sebelum penyimpanan. {@code CascadeType.PERSIST}/{@code MERGE} membuat
	 * pengaturan biaya yang belum tersimpan ikut tersimpan &mdash; keempat jalur penulis selalu
	 * meneruskan pengaturan biaya yang sudah ada, jadi kaskade itu praktis tidak pernah terpakai.</p>
	 *
	 * @param pengaturanBiaya pengaturan biaya pemilik baris ini
	 */
	public void setPengaturanBiaya(PengaturanBiaya pengaturanBiaya) {
		this.pengaturanBiaya = pengaturanBiaya;
	}

	/**
	 * Mengembalikan siswa terdaftar yang termasuk cakupan pengaturan biaya ini.
	 *
	 * <p>{@code null} bila baris ini mewakili calon siswa (lihat {@link #getCalonSiswa()}); seluruh
	 * pembaca sisi siswa karena itu menambahkan {@code isNotNull("siswa")}. Sama seperti
	 * {@link #getPengaturanBiaya()}, nilainya dilewatkan {@code GeneralValueObject.check(...)}
	 * sehingga proxy lepas-sesi tetap aman dipakai.</p>
	 *
	 * <p>FK {@code siswa} sengaja <b>tidak</b> ber-{@code ON DELETE CASCADE}, dan
	 * {@code CommonUiFactoryHelper} memblokir penghapusan siswa selama baris ini masih ada.</p>
	 *
	 * @return siswa anggota cakupan, atau {@code null} bila baris ini untuk calon siswa
	 * @see ais.database.model.GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa", nullable = true)
	public Siswa getSiswa() {
		siswa = check(siswa);
		return siswa;
	}

	/**
	 * Menyetel siswa terdaftar yang termasuk cakupan pengaturan biaya ini.
	 *
	 * <p>Diisi oleh jalur penulis sisi siswa; jangan diisi bersamaan dengan
	 * {@link #setCalonSiswa(CalonSiswa)}. Perhatikan
	 * {@code AmbilDataSiswaForPengaturanBiayaHelper#save()} pada cabang &quot;centang lintas
	 * halaman&quot; meneruskan {@code new Siswa(id)} &mdash; entity dangkal berisi id saja, cukup
	 * karena hanya kolom FK-nya yang ditulis.</p>
	 *
	 * @param siswa siswa anggota cakupan; boleh {@code null}
	 */
	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
	}

	/**
	 * Mengembalikan calon siswa PPDB yang termasuk cakupan pengaturan biaya ini.
	 *
	 * <p>{@code null} bila baris ini mewakili siswa terdaftar; seluruh pembaca sisi calon siswa
	 * menambahkan {@code isNotNull("calonSiswa")}. Nilainya dilewatkan
	 * {@code GeneralValueObject.check(...)} lebih dulu, sama seperti dua relasi lainnya.</p>
	 *
	 * <p>Berbeda dari FK {@code siswa}, FK {@code calon_siswa} diberi {@code ON DELETE CASCADE} oleh
	 * {@code cascade.sql}: menghapus calon siswa ikut membuang keanggotaan tarif khususnya.</p>
	 *
	 * @return calon siswa anggota cakupan, atau {@code null} bila baris ini untuk siswa terdaftar
	 * @see ais.database.model.GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "calon_siswa")
	public CalonSiswa getCalonSiswa() {
		calonSiswa = check(calonSiswa);
		return calonSiswa;
	}

	/**
	 * Menyetel calon siswa PPDB yang termasuk cakupan pengaturan biaya ini.
	 *
	 * <p>Diisi oleh jalur penulis sisi calon siswa
	 * ({@code AmbilDataCalonSiswaForPengaturanBiayaHelper#save()} dan impor Excel pada
	 * {@code DetailTagihanCalonSiswaHelper}); jangan diisi bersamaan dengan
	 * {@link #setSiswa(Siswa)}.</p>
	 *
	 * @param calonSiswa calon siswa anggota cakupan; boleh {@code null}
	 */
	public void setCalonSiswa(CalonSiswa calonSiswa) {
		this.calonSiswa = calonSiswa;
	}

}
