package ais.database.model.sekolah;
// Generated 10 Okt 18 12:46:07 by Hibernate Tools 5.2.3.Final

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;
import ais.database.model.PerguruanTinggi;

/**
 * Entity MASTER "paket" tata tertib siswa — satu baris tabel
 * {@code sekolah.pelanggaran_dan_hukuman} adalah sebuah PEMETAAN BERNAMA yang mengikat
 * <b>sehimpunan</b> {@link ais.database.model.sekolah.Pelanggaran} (butir aturan yang dilanggar)
 * dengan <b>sehimpunan</b> {@link ais.database.model.sekolah.Hukuman} (sanksi yang boleh
 * dijatuhkan). Contoh isi: paket "Pelanggaran Ringan" yang memuat butir "Terlambat" dan "Atribut
 * tidak lengkap" di sisi pelanggaran, serta "Teguran lisan" dan "Membersihkan halaman" di sisi
 * hukuman.
 *
 * <p><b>Kelas ini BUKAN entity transaksi.</b> Ia tidak menyimpan kejadian pelanggaran seorang
 * siswa dan tidak punya relasi ke {@code Siswa}, {@code waktu}, maupun tahun ajaran. Catatan
 * kejadian nyata ada di entity terpisah {@code PelanggaranSiswa} (tabel
 * {@code sekolah.pelanggaran_siswa}), yang menyimpan {@code siswa}, {@code waktu}, {@code ta},
 * relasi {@code @ManyToOne} ke baris kelas ini, PLUS dua koleksi {@code @ManyToMany}-nya sendiri
 * ke {@code Pelanggaran} dan {@code Hukuman} yang berisi butir-butir yang BENAR-BENAR dicentang
 * petugas untuk kejadian itu.</p>
 *
 * <h3>Posisi dalam rantai tata tertib (4 lapis)</h3>
 * <ol>
 * <li><b>{@link ais.database.model.sekolah.Pelanggaran}</b> (tabel {@code sekolah.pelanggaran}) —
 * master JENIS pelanggaran, dengan bobot {@code kredit}.</li>
 * <li><b>{@link ais.database.model.sekolah.Hukuman}</b> (tabel {@code sekolah.hukuman}) — master
 * JENIS sanksi, dengan bobot {@code poin}.</li>
 * <li><b>{@code PelanggaranDanHukuman}</b> (kelas ini) — PAKET/KATEGORI yang menyilangkan kedua
 * master di atas lewat DUA tabel penghubung, sehingga petugas tidak perlu memilih dari daftar
 * penuh setiap kali mencatat.</li>
 * <li><b>{@code PelanggaranSiswa}</b> — transaksi per siswa per kejadian.</li>
 * </ol>
 *
 * <h3>Bagaimana paket ini dipakai petugas (alur terverifikasi dari kode)</h3>
 * Pada layar {@code ais.action.master.sekolah.PelanggaranSiswaAction} (form Tambah/Ubah catatan
 * pelanggaran siswa):
 * <ol>
 * <li>Petugas memilih siswa, lalu memilih satu baris kelas ini dari combobox berlabel "Jenis
 * Pelanggaran Dan Hukuman". Combobox itu diisi
 * {@code Common.insertCombo(pelanggaranDanHukuman, "nama", PelanggaranDanHukuman.class)} —
 * <b>tanpa satu pun {@code Criterion}</b> (lihat catatan cakupan di bawah).</li>
 * <li>Event {@code onChange} combobox memicu {@code loadPelanggaran(...)} dan
 * {@code loadHukuman(...)}. Kedua method itu melakukan
 * {@code HibernateUtil.currentSession().refresh(pelanggaranDanHukuman)} lalu membaca
 * {@link #getPelanggarans()} dan {@link #getHukumans()}, dan merender <b>satu checkbox per
 * anggota himpunan</b>.</li>
 * <li>Jadi paket ini berfungsi sebagai <b>PENYARING/menu pilihan</b>: hanya butir pelanggaran dan
 * sanksi yang terdaftar di paket terpilih yang bisa dicentang untuk kejadian tersebut. Mengganti
 * paket = mengganti seluruh daftar checkbox yang tersedia.</li>
 * <li>Hasil centang disimpan ke koleksi milik {@code PelanggaranSiswa}, BUKAN ke koleksi kelas
 * ini. Perubahan pada paket setelahnya tidak menulis ulang catatan lama, tetapi MEMPERSEMPIT
 * pilihan yang tampil saat catatan lama dibuka kembali untuk diedit.</li>
 * </ol>
 * Konsumen lain: {@code ais.action.report.format1.sekolah.LaporanRaporSiswa} mencetak
 * {@code pelanggaran.getPelanggaranDanHukuman().getNama()} sebagai label jenis pada rapor, dan
 * {@code ais.action.master.pelanggaran.DasbordPelanggaran} memakai nama paket sebagai sumbu
 * pengelompokan grafik.
 *
 * <h3>Struktur DUA relasi {@code @ManyToMany} (TERVERIFIKASI dari kode kelas ini)</h3>
 * Keduanya berbentuk simetris dan tidak punya sisi {@code mappedBy} — kelas ini adalah sisi
 * PEMILIK untuk kedua-duanya:
 * <table border="1">
 * <caption>Ringkasan pemetaan koleksi</caption>
 * <tr><th>Properti</th><th>Tabel silang</th><th>{@code joinColumns}</th>
 * <th>{@code inverseJoinColumns}</th><th>{@code cascade}</th><th>{@code @OrderBy}</th></tr>
 * <tr><td>{@link #getPelanggarans()}</td>
 * <td>{@code sekolah.pelanggaran_dan_hukuman_has_pelanggaran}</td>
 * <td>{@code pelanggaran_dan_hukuman}</td><td>{@code pelanggaran}</td>
 * <td>{@code MERGE}</td><td>{@code nama asc}</td></tr>
 * <tr><td>{@link #getHukumans()}</td>
 * <td>{@code sekolah.pelanggaran_dan_hukuman_has_hukuman}</td>
 * <td>{@code pelanggaran_dan_hukuman}</td><td>{@code hukuman}</td>
 * <td>{@code MERGE}</td><td>{@code nama asc}</td></tr>
 * </table>
 * Catatan penting yang mengikutinya:
 * <ul>
 * <li>Tidak ada {@code CascadeType.REMOVE} maupun {@code orphanRemoval} — menghapus paket TIDAK
 * menghapus master {@code Pelanggaran}/{@code Hukuman}, hanya baris tabel silang. Itu perilaku
 * yang benar untuk pemetaan.</li>
 * <li>{@code CascadeType.MERGE} berarti setiap {@code session.merge(paket)} — yang dilakukan
 * {@code Common.refreshSaveOrUpdate(...)} dari {@code onSave()} — ikut me-{@code merge} SETIAP
 * anggota kedua himpunan. Bila salah satu anggota kebetulan berupa instance basi (mis. berasal
 * dari cache {@code ConstantValues} yang di-preload {@code InitData}), keadaan basi itu bisa
 * ikut tertulis balik ke tabel master. Jangan menambahkan cascade lain tanpa memikirkan efek
 * ini.</li>
 * <li><b>{@code @OrderBy("nama asc")} praktis dekoratif.</b> Kedua koleksi bertipe
 * {@link java.util.Set} yang diinisialisasi {@link java.util.HashSet} — bukan
 * {@code SortedSet}/{@code @SortNatural}. Hibernate memang menambahkan {@code ORDER BY} pada SQL
 * pemuatan, tetapi hasilnya langsung ditumpahkan ke {@code HashSet} sehingga urutannya hilang.
 * Konsumen menanganinya sendiri dan TIDAK seragam: renderer grid master membungkus ulang dengan
 * {@code new TreeSet<>(...)}, sedangkan {@code PelanggaranSiswaAction} melakukan iterasi
 * langsung atas {@code HashSet} sehingga urutan checkbox pada form pencatatan bersifat
 * sembarang.</li>
 * <li><b>Efek samping {@code TreeSet} di grid master.</b> Karena {@code Pelanggaran} dan
 * {@code Hukuman} sama-sama tidak meng-override {@code compareTo}, kunci urut efektifnya jatuh ke
 * {@code nama} (lihat Javadoc kedua kelas itu) dan mengembalikan {@code 0} untuk nama yang sama
 * persis. Pembungkusan {@code new TreeSet<Pelanggaran>(...)} /
 * {@code new TreeSet<Hukuman>(...)} di {@code PelanggaranDanHukumanRenderer.render(...)}
 * karenanya MENCIUTKAN dua anggota ber-nama identik menjadi satu baris pada kolom ringkasan —
 * tanpa pesan apa pun. Kondisi ini bukan teoretis: tidak ada batasan {@code UNIQUE} pada kolom
 * {@code nama} kedua master, dan daftar centang pada layar ini justru menyorongkan aturan dari
 * SEMUA sekolah sekaligus (lihat butir berikutnya), sehingga nama kembar sangat mudah terjadi.
 * Form pencatatan siswa tetap menampilkan keduanya karena tidak memakai {@code TreeSet}.</li>
 * </ul>
 *
 * <h3>Cakupan multi-tenant — HASIL VERIFIKASI</h3>
 * Entity ini menyediakan tiga field cakupan: {@link #getSekolah()}, {@link #getYayasan()}, dan
 * {@link #getPerguruanTinggi()}. Grid daftar pada {@code PelanggaranDanHukumanAction} memang
 * menyaring lewat combobox {@code searchyayasan}/{@code searchsekolah} yang dipra-pilih (dan
 * di-{@code disable} untuk non-admin) oleh {@code InitComboUtil.initYayasanDanSekolahDanSemua},
 * sehingga tampilan daftar umumnya sudah terkurung pada sekolah pengguna.
 *
 * <p><b>Namun daftar centang di dalam dialog Tambah/Ubah TIDAK tersaring sama sekali</b>, dan ini
 * berlaku untuk KEDUA sisi (bukan hanya sisi {@code Pelanggaran}). Kedua query di
 * {@code PelanggaranDanHukumanAction.init(...)} hanya menyaring status aktif:</p>
 * <pre>
 * createCriteria(Pelanggaran.class).add(or(isNull("aktif"), eq("aktif", true)))
 * createCriteria(Hukuman.class)   .add(or(isNull("aktif"), eq("aktif", true)))
 * </pre>
 * Tidak ada {@code eq("sekolah", ...)}, {@code eq("yayasan", ...)}, maupun kriteria
 * {@code perguruanTinggi}. Akibatnya admin satu sekolah dapat mencentang butir pelanggaran DAN
 * butir hukuman milik sekolah/yayasan lain ke dalam paketnya sendiri. Ini kebocoran cakupan
 * BACA (nama aturan sekolah lain terlihat) sekaligus keterikatan data lintas tenant yang
 * membingungkan.
 *
 * <p>Kelemahan sejenis berlanjut di hilir: combobox pemilihan paket pada
 * {@code PelanggaranSiswaAction} diisi {@code Common.insertCombo(...)} tanpa {@code Criterion}
 * sama sekali, sehingga daftarnya memuat paket dari SEMUA sekolah <b>dan</b> paket yang sudah
 * dinonaktifkan — {@link #getAktif()} tidak dihormati satu pun pembaca di jalur pencatatan
 * siswa (nilainya hanya menggerakkan checkbox "Aktif" di grid master).</p>
 *
 * <h3>Otorisasi layar CRUD — CONTOH POSITIF (terverifikasi ulang)</h3>
 * Berbeda dari banyak layar sekawan, {@code PelanggaranDanHukumanAction} memasang guard privilege
 * dasar dengan lengkap:
 * <ul>
 * <li>{@code doBeforeCompose(...)} memanggil {@code Common.doCheckSecurity()} (gerbang READ);</li>
 * <li>tombol Tambah: {@code add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE))};</li>
 * <li>{@code edit = checkPrevilages(UPDATE)} dan {@code delete = checkPrevilages(DELETE)}, dipakai
 * pada {@code Common.copyEditDeleteButtons(edit, delete, ...)} maupun untuk men-{@code disable}
 * checkbox "Aktif" per baris;</li>
 * <li>tombol Unggah data massal disembunyikan kecuali ketiga hak (CREATE+UPDATE+DELETE) dimiliki.</li>
 * </ul>
 * Yang TIDAK dijaga: tombol Cetak/Ekspor ({@code Common.cetakData}) tidak dikaitkan ke hak apa
 * pun — pengguna READ-saja tetap bisa menarik seluruh kolom {@code id, nama, sekolah,
 * perguruanTinggi, keterangan, aktif}. Untuk entity master ini dampaknya rendah, tetapi perlu
 * dicatat karena batas cakupan sekolahnya juga longgar.
 *
 * <p><b>Peringatan konteks (bukan cacat kelas ini):</b> layar RIWAYAT pelanggaran/hukuman siswa
 * ({@code PelanggaranSiswaAction.initCriteria()}) TIDAK memfilter kepemilikan untuk peran siswa
 * maupun guru, dan filter orang tua bersifat <i>fail-open</i>. Itu menyangkut data disiplin anak
 * di bawah umur dan ditangani terpisah pada task eskalasi yang sudah ada; jangan bingung
 * membedakannya dengan layar master paket ini yang guard privilege-nya justru sudah benar.</p>
 *
 * <h3>Kuirk lain pada dialog Tambah/Ubah yang perlu diketahui</h3>
 * <ul>
 * <li><b>Centang langsung mengubah data, tombol "Batal" tidak membatalkan apa pun.</b> Method
 * {@code init(...)} menyetel {@code selectedPelanggaran = this.pelanggaranDanHukuman
 * .getPelanggarans()} (dan padanannya untuk hukuman) SETELAH
 * {@code session.refresh(pelanggaranDanHukuman)} — jadi variabel itu adalah <b>alias langsung ke
 * {@code PersistentSet} milik entity yang ter-attach</b>, bukan salinan. Listener {@code onClick}
 * setiap checkbox memanggil {@code add}/{@code remove} pada koleksi itu, sehingga perubahan sudah
 * masuk ke entity terkelola sebelum "Simpan" ditekan. Tombol "Batal" hanya melakukan
 * {@code addWindow.setVisible(false)} tanpa rollback maupun {@code evict}, sehingga perubahan
 * centang tetap ikut ter-<i>flush</i> saat session request ditutup. Jangan "merapikan"
 * {@code setPelanggarans}/{@code setHukumans} di {@code onSave()} tanpa lebih dulu memutus alias
 * ini.</li>
 * <li>Validasi {@code onSave()} hanya mewajibkan {@code nama} tidak kosong. Paket TANPA satu pun
 * pelanggaran atau tanpa satu pun hukuman boleh disimpan, dan begitu dipilih di form pencatatan
 * siswa akan menghasilkan daftar checkbox kosong tanpa penjelasan.</li>
 * </ul>
 *
 * <h3>Pengelompokan anggota</h3>
 * <ul>
 * <li><b>Jejak audit warisan</b> — {@link #getOleh()}/{@link #setOleh(String)},
 * {@link #getOlehId()}/{@link #setOlehId(String)},
 * {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, {@link #onUpdate()}.</li>
 * <li><b>Identitas</b> — {@link #getId()}/{@link #setId(Long)} dan dua konstruktor.</li>
 * <li><b>Cakupan tenant</b> — {@link #getSekolah()}, {@link #getYayasan()},
 * {@link #getPerguruanTinggi()} beserta setter-nya.</li>
 * <li><b>Isi paket (inti kelas ini)</b> — {@link #getPelanggarans()}/{@link #setPelanggarans(Set)}
 * dan {@link #getHukumans()}/{@link #setHukumans(Set)}.</li>
 * <li><b>Atribut deskriptif</b> — {@link #getNama()}, {@link #getKeterangan()},
 * {@link #getAktif()} beserta setter-nya.</li>
 * </ul>
 * Entity ini tidak memiliki method bisnis, query statis, maupun {@code compareTo()} sendiri.
 *
 * <h3>Hal non-obvious yang WAJIB diketahui sebelum menyentuh kelas ini</h3>
 * <ul>
 * <li><b>Komentar hbm2java di atas anotasi keliru</b> — teks aslinya berbunyi "JenisGuru generated
 * by hbm2java", sisa salin-tempel generator; kelas ini tidak ada hubungannya dengan jenis guru.
 * Kekeliruan yang sama muncul di {@code Pelanggaran} dan {@code PelanggaranSiswa}.</li>
 * <li><b>{@code serialVersionUID} kembar</b> — nilai {@code -7490758846785025664L} dipakai persis
 * sama di {@code Pelanggaran} dan {@code PelanggaranSiswa}. Tidak berbahaya (serialisasi Java
 * tetap memeriksa nama kelas), tetapi menegaskan ketiganya lahir dari salin-tempel.</li>
 * <li><b>Field warisan yang dideklarasikan ULANG bukan bug</b> — {@code id}, {@code oleh},
 * {@code olehId}, dan {@code tanggal_dirubah} sudah ada di induk
 * {@link ais.database.model.GeneralValueObject}, namun induk itu BUKAN {@code @Entity} maupun
 * {@code @MappedSuperclass} — hanya POJO abstrak biasa. Hibernate TIDAK memetakan properti induk,
 * sehingga setiap entity turunan HARUS mendeklarasikan ulang keempatnya agar ikut tersimpan. Ini
 * keharusan teknis, jangan "dirapikan".</li>
 * <li><b>Dua getter melakukan MUTASI saat dibaca</b> — {@link #getYayasan()} dan
 * {@link #getPerguruanTinggi()} menulis balik ke field instance. Karena entity beranotasi
 * {@code dynamicUpdate=true} dan pemetaannya <i>property access</i> ({@code @Id} ada di getter),
 * Hibernate membaca state lewat getter saat flush — sehingga sekadar MEMBACA baris yang masih
 * ter-attach dapat memicu {@code UPDATE} nyata plus revisi Envers palsu. Rinciannya pada Javadoc
 * masing-masing getter.</li>
 * <li><b>Tiga setter menolak nilai kosong secara diam-diam</b> — {@link #setOleh(String)},
 * {@link #setOlehId(String)}, {@link #setSekolah(Sekolah)}, dan {@link #setYayasan(Yayasan)}
 * mengabaikan {@code null}/blanko alih-alih menyimpannya, sehingga nilai yang sudah pernah terisi
 * TIDAK BISA dikosongkan lagi lewat setter.</li>
 * <li><b>Pengurutan mewarisi {@code compareTo} induk</b> — {@link ais.database.model.GeneralValueObject}
 * memakai urutan kunci {@code nomorUrut} &rarr; {@code nim} &rarr; {@code nama} &rarr;
 * {@code keterangan}. Entity ini tidak punya {@code nomorUrut} maupun {@code nim}, jadi kunci
 * efektifnya {@code nama}. Itulah yang dipakai {@code Collections.sort(...)} di dalam
 * {@code Common.insertCombo(...)} untuk mengurutkan combobox pemilihan paket.</li>
 * </ul>
 *
 * <h3>Pemuatan awal &amp; cache</h3>
 * {@code ais.common.InitData} mendaftarkan kelas ini pada {@code initClasses(...)} saat aplikasi
 * naik (berdampingan dengan {@code Hukuman} dan {@code Pelanggaran}), sehingga seluruh isi tabel
 * di-preload ke cache {@code ConstantValues} tingkat aplikasi — cache itu TIDAK dipartisi per
 * sekolah/yayasan.
 *
 * <h3>Layar &amp; audit</h3>
 * Layar CRUD-nya {@code ais.action.master.sekolah.PelanggaranDanHukumanAction} (lengkap dengan tab
 * dasbor {@code DasbordPelanggaran} lingkup {@code SEMUA}). Entity beranotasi {@link Audited}
 * sehingga setiap perubahan direkam Hibernate Envers ke tabel bayangan, dan grid menampilkan
 * tombol revisi lewat {@code RevisiHelper.createNewRevisi(...)}.
 *
 * @see ais.database.model.sekolah.Pelanggaran
 * @see ais.database.model.sekolah.Hukuman
 * @see ais.database.model.GeneralValueObject
 * @see ais.database.model.PerguruanTinggi
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(name = "pelanggaran_dan_hukuman", schema = "sekolah")
public class PelanggaranDanHukuman extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilai ini IDENTIK dengan milik {@code Pelanggaran} dan
	 * {@code PelanggaranSiswa} (sisa salin-tempel generator); jangan diubah karena instance
	 * entity ikut diserialisasi ke dalam state desktop ZK.
	 */
	private static final long serialVersionUID = -7490758846785025664L;
	/** Kunci utama, dibangkitkan basis data ({@code IDENTITY}). Lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini. Lihat {@link #getOleh()}. */
	private String oleh;
	/** ID pengguna terakhir yang mengubah baris ini. Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan ID pengguna yang terakhir mengubah baris ini.
	 *
	 * <p>Bagian dari jejak audit ringan yang HARUS dideklarasikan ulang di setiap entity karena
	 * {@link ais.database.model.GeneralValueObject} bukan {@code @MappedSuperclass}. Diisi otomatis
	 * oleh {@code ais.database.hibernate.AuditTimestampInterceptor} lewat {@link #onUpdate()}.</p>
	 *
	 * @return ID pengguna pengubah terakhir, atau {@code null} bila baris belum pernah diubah
	 *         melalui jalur yang memasang interceptor
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel ID pengguna pengubah terakhir.
	 *
	 * <p><b>Perhatian:</b> nilai {@code null} atau string yang hanya berisi spasi DIABAIKAN secara
	 * diam-diam (method langsung {@code return} tanpa menyentuh field). Artinya nilai lama tidak
	 * pernah bisa dikosongkan lewat setter ini — sengaja, agar jejak audit tidak terhapus oleh
	 * pemanggil yang kebetulan mengirim nilai kosong.</p>
	 *
	 * @param olehId ID pengguna pengubah; {@code null}/blanko diabaikan
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir.
	 *
	 * <p>Perilaku "abaikan nilai kosong" sama persis dengan {@link #setOlehId(String)} — lihat
	 * penjelasan di sana.</p>
	 *
	 * @param oleh nama pengguna pengubah; {@code null}/blanko diabaikan
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil Hibernate TEPAT SEBELUM setiap {@code UPDATE}
	 * baris ini, lalu meneruskan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} yang mengisi
	 * {@link #setOleh(String)}, {@link #setOlehId(String)}, dan {@link #setTanggal_dirubah(Date)}
	 * dari konteks pengguna aktif.
	 *
	 * <p><b>Jangan dipanggil manual.</b> Ini titik masuk milik provider persistence. Perlu diingat
	 * bahwa entity ini juga memiliki getter yang menulis balik ({@link #getYayasan()},
	 * {@link #getPerguruanTinggi()}), sehingga {@code UPDATE} — dan karenanya callback ini —
	 * dapat terpicu bahkan pada alur yang secara logis hanya "membaca" paket.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu perubahan terakhir. Diinisialisasi ke waktu server saat objek dibuat
	 * ({@code ais.ui.util.WaktuUtil.getDate()}), lalu diperbarui interceptor pada setiap
	 * {@code UPDATE}. Lihat {@link #getTanggal_dirubah()}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir.
	 *
	 * <p>Berbeda dari {@link #setOleh(String)}/{@link #setOlehId(String)}, setter ini TIDAK
	 * menyaring {@code null} — nilai {@code null} akan benar-benar tersimpan dan mengosongkan
	 * kolom.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * @return waktu perubahan terakhir (presisi {@code TIMESTAMP}); tidak pernah {@code null}
	 *         untuk objek yang baru dibuat di memori, tetapi bisa {@code null} untuk baris warisan
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Unit sekolah pemilik paket ini. Lihat {@link #getSekolah()}. */
	private Sekolah sekolah;
	/** Badan penyelenggara (yayasan) pemilik paket ini. Lihat {@link #getYayasan()}. */
	private Yayasan yayasan;
	/** Penjelasan bebas atas paket. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Label paket yang dilihat petugas di combobox pencatatan. Lihat {@link #getNama()}. */
	private String nama;
	/** Tenant terluar (instansi). Lihat {@link #getPerguruanTinggi()}. */
	private PerguruanTinggi perguruanTinggi;
	/** Penanda aktif/nonaktif. Lihat {@link #getAktif()} untuk kuirk nilai bawaannya. */
	private Boolean aktif;

	/**
	 * Himpunan JENIS SANKSI yang boleh dijatuhkan untuk paket ini. Diinisialisasi
	 * {@link java.util.HashSet} kosong agar tidak pernah {@code null}. Lihat
	 * {@link #getHukumans()}.
	 */
	private Set<Hukuman> hukumans = new HashSet<Hukuman>();

	/**
	 * Mengembalikan himpunan {@link Hukuman} yang tercakup dalam paket ini — separuh dari inti
	 * kelas ini.
	 *
	 * <p><b>Pemetaan:</b> {@code @ManyToMany} lewat tabel silang
	 * {@code sekolah.pelanggaran_dan_hukuman_has_hukuman}, kolom {@code pelanggaran_dan_hukuman}
	 * menunjuk baris ini dan kolom {@code hukuman} menunjuk baris master sanksi. Kelas ini adalah
	 * sisi pemilik ({@code Hukuman} tidak punya sisi {@code mappedBy} balik).</p>
	 *
	 * <p><b>Cascade:</b> hanya {@code MERGE}. Menyimpan paket ikut me-{@code merge} setiap anggota
	 * himpunan; menghapus paket TIDAK menghapus master sanksinya.</p>
	 *
	 * <p><b>Urutan tidak dijamin.</b> {@code @OrderBy("nama asc")} memang ditambahkan ke SQL
	 * pemuatan, tetapi hasilnya ditampung {@link java.util.HashSet} sehingga urutan hilang.
	 * Konsumen yang butuh urutan harus mengurutkan sendiri — dan bila memakai {@code TreeSet},
	 * ingat bahwa {@code Hukuman} tidak meng-override {@code compareTo} sehingga dua sanksi
	 * ber-nama identik akan menciut menjadi satu.</p>
	 *
	 * <p><b>Pemanggil:</b> {@code PelanggaranDanHukumanRenderer.render(...)} (kolom ringkasan grid
	 * master) dan {@code PelanggaranSiswaAction.loadHukuman(...)} — yang terakhir mendahuluinya
	 * dengan {@code session.refresh(paket)} lalu menjadikan setiap anggota sebagai satu checkbox
	 * pada form pencatatan pelanggaran siswa, berikut label pengurangan poinnya.</p>
	 *
	 * @return himpunan sanksi milik paket; tidak pernah {@code null}, bisa kosong (paket tanpa
	 *         sanksi boleh disimpan dan menghasilkan daftar checkbox kosong di form pencatatan)
	 */
	@ManyToMany(targetEntity = Hukuman.class, cascade = { CascadeType.MERGE })
	@OrderBy(value = "nama asc")
	@JoinTable(name = "pelanggaran_dan_hukuman_has_hukuman", schema = "sekolah", joinColumns = @JoinColumn(name = "pelanggaran_dan_hukuman"), inverseJoinColumns = @JoinColumn(name = "hukuman"))
	public Set<Hukuman> getHukumans() {
		return hukumans;
	}

	/**
	 * Mengganti seluruh himpunan sanksi paket ini.
	 *
	 * <p><b>Waspadai penggantian referensi.</b> {@code PelanggaranDanHukumanAction.onSave()}
	 * memanggil {@code setHukumans(selectedHukuman)} dengan {@code selectedHukuman} yang
	 * sebenarnya adalah ALIAS ke koleksi persisten yang sama (diambil dari {@link #getHukumans()}
	 * setelah {@code session.refresh}). Jadi pada alur normal ini sekadar menyetel ulang referensi
	 * yang identik; perubahan sesungguhnya sudah terjadi lebih dulu saat checkbox diklik. Bila
	 * suatu saat alias itu diputus, pastikan tidak menyodorkan koleksi biasa untuk menggantikan
	 * {@code PersistentSet} milik entity terkelola karena Hibernate akan kehilangan jejak
	 * perubahan yang sudah terekam.</p>
	 *
	 * @param hukumans himpunan sanksi baru; menyetel {@code null} akan membuat {@link #getHukumans()}
	 *                 mengembalikan {@code null} dan meruntuhkan pemanggil yang beriterasi tanpa
	 *                 pemeriksaan — tidak ada penjagaan di sini
	 */
	public void setHukumans(Set<Hukuman> hukumans) {
		this.hukumans = hukumans;
	}

	/**
	 * Himpunan JENIS PELANGGARAN yang tercakup paket ini. Diinisialisasi {@link java.util.HashSet}
	 * kosong agar tidak pernah {@code null}. Lihat {@link #getPelanggarans()}.
	 */
	private Set<Pelanggaran> pelanggarans = new HashSet<Pelanggaran>();

	/**
	 * Mengembalikan himpunan {@link Pelanggaran} yang tercakup dalam paket ini — separuh lainnya
	 * dari inti kelas ini.
	 *
	 * <p><b>Pemetaan:</b> {@code @ManyToMany} lewat tabel silang
	 * {@code sekolah.pelanggaran_dan_hukuman_has_pelanggaran}, kolom
	 * {@code pelanggaran_dan_hukuman} menunjuk baris ini dan kolom {@code pelanggaran} menunjuk
	 * baris master aturan. Strukturnya simetris persis dengan {@link #getHukumans()}; kelas ini
	 * sisi pemiliknya.</p>
	 *
	 * <p><b>Cascade:</b> hanya {@code MERGE} — sama seperti sisi hukuman, dengan konsekuensi yang
	 * sama.</p>
	 *
	 * <p><b>Urutan tidak dijamin</b> ({@code HashSet}), dan {@code Pelanggaran} juga tidak
	 * meng-override {@code compareTo} sehingga pembungkusan ke {@code TreeSet} menciutkan butir
	 * ber-nama identik. Risiko nama identik di sini NYATA karena layar pemilihan memuat aturan
	 * dari semua sekolah tanpa filter cakupan.</p>
	 *
	 * <p><b>Pemanggil:</b> {@code PelanggaranDanHukumanRenderer.render(...)} dan
	 * {@code PelanggaranSiswaAction.loadPelanggaran(...)}. Pada yang terakhir, himpunan inilah
	 * yang menjadi DAFTAR PILIHAN checkbox butir pelanggaran untuk kejadian yang sedang dicatat —
	 * butir di luar paket tidak dapat dipilih petugas.</p>
	 *
	 * @return himpunan butir pelanggaran milik paket; tidak pernah {@code null}, bisa kosong
	 */
	@ManyToMany(targetEntity = Pelanggaran.class, cascade = { CascadeType.MERGE })
	@OrderBy(value = "nama asc")
	@JoinTable(name = "pelanggaran_dan_hukuman_has_pelanggaran", schema = "sekolah", joinColumns = @JoinColumn(name = "pelanggaran_dan_hukuman"), inverseJoinColumns = @JoinColumn(name = "pelanggaran"))
	public Set<Pelanggaran> getPelanggarans() {
		return pelanggarans;
	}

	/**
	 * Mengganti seluruh himpunan butir pelanggaran paket ini.
	 *
	 * <p>Catatan alias {@code PersistentSet} pada {@link #setHukumans(Set)} berlaku sama persis di
	 * sini.</p>
	 *
	 * @param pelanggarans himpunan butir pelanggaran baru; {@code null} tidak dijaga
	 */
	public void setPelanggarans(Set<Pelanggaran> pelanggarans) {
		this.pelanggarans = pelanggarans;
	}

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate/JPA untuk instansiasi lewat refleksi.
	 *
	 * <p>Juga dipakai langsung oleh {@code PelanggaranDanHukumanAction.onAdd(...)} sebagai objek
	 * kosong untuk dialog Tambah. Kedua koleksi sudah terisi {@link java.util.HashSet} kosong dan
	 * {@link #getTanggal_dirubah()} sudah berisi waktu sekarang.</p>
	 */
	public PelanggaranDanHukuman() {
	}

	/**
	 * Konstruktor ringkas warisan hbm2java yang mengisi kolom {@code NOT NULL} saja.
	 *
	 * <p><b>Tidak ada pemanggil di dalam basis kode ini</b> — dipertahankan agar kontrak kelas
	 * hasil generator tetap utuh. Perhatikan bahwa parameter bertipe primitif {@code long}
	 * sedangkan field-nya {@link Long}; nilai akan di-<i>autobox</i>, sehingga konstruktor ini
	 * tidak dapat membuat objek ber-{@code id} {@code null} (bentuk yang dibutuhkan alur Tambah).
	 * Gunakan konstruktor kosong untuk data baru.</p>
	 *
	 * @param id   kunci utama baris yang sudah ada
	 * @param nama label paket
	 */
	public PelanggaranDanHukuman(long id, String nama) {
		this.id = id;
		this.nama = nama;
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Dibangkitkan basis data ({@code IDENTITY}) dan dipetakan {@code insertable = false}
	 * sehingga tidak pernah ikut disertakan pada {@code INSERT}. Nilai {@code null} adalah
	 * penanda "baris baru" yang dipakai layar CRUD untuk memilih judul dialog
	 * ("Tambah" vs "Ubah") serta untuk memutuskan perlu-tidaknya {@code session.load}/
	 * {@code session.refresh}.</p>
	 *
	 * <p>Karena {@code @Id} berada di getter, seluruh pemetaan entity ini memakai <i>property
	 * access</i> — itulah sebab getter lain yang punya efek samping bisa berubah menjadi
	 * {@code UPDATE} nyata saat flush.</p>
	 *
	 * @return kunci utama, atau {@code null} bila baris belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama. Umumnya hanya dipanggil Hibernate sesudah {@code INSERT}.
	 *
	 * @param id kunci utama baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan unit sekolah pemilik paket ini.
	 *
	 * <p>Relasi {@code @ManyToOne} lazy ke kolom {@code sekolah_id}. Nilai dilewatkan
	 * {@code check(...)} milik {@link ais.database.model.GeneralValueObject} yang me-resolve proxy
	 * lazy yang mungkin sudah <i>detached</i> — mencegah {@code LazyInitializationException} saat
	 * baris dibaca di luar session aslinya (mis. dari cache {@code ConstantValues} atau dari state
	 * desktop ZK).</p>
	 *
	 * <p><b>Cakupan ini bersifat informatif, bukan penegak.</b> Grid daftar menyaringnya lewat
	 * combobox pencarian, tetapi daftar centang {@code Pelanggaran}/{@code Hukuman} di dalam
	 * dialog dan combobox pemilihan paket pada layar pencatatan siswa TIDAK memakai nilai ini
	 * sebagai filter sama sekali.</p>
	 *
	 * @return sekolah pemilik, atau {@code null} bila paket berlaku lintas sekolah / kolom belum
	 *         diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id")
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return this.sekolah;
	}

	/**
	 * Menyetel sekolah pemilik paket.
	 *
	 * <p><b>Kuirk:</b> objek {@code Sekolah} yang {@code null} ATAU yang ber-{@code id}
	 * {@code null} (mis. objek konteks kosong yang dikembalikan {@code SekolahUtil.getSekolah()},
	 * atau item combobox "==Semua==") disimpan sebagai {@code null} — bukan sebagai objek transien
	 * yang akan meledak saat flush. Ini penjagaan yang disengaja terhadap
	 * {@code TransientObjectException}, bukan bug.</p>
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau objek tanpa {@code id} akan menjadi
	 *                {@code null}
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah == null || sekolah.getId() == null ? null : sekolah;
	}

	/**
	 * Mengembalikan yayasan (badan penyelenggara) pemilik paket ini.
	 *
	 * <p><b>GETTER DESTRUKTIF — baca sebelum memakai.</b> Method ini tidak sekadar membaca field.
	 * Ia memanggil {@link #getSekolah()} lebih dulu, dan bila sekolah ada, MENIMPA field
	 * {@code yayasan} dengan {@code sekolah.getYayasan()} — mengabaikan apa pun yang tersimpan di
	 * kolom {@code yayasan_id}. Karena pemetaan entity ini memakai <i>property access</i> dan
	 * {@code dynamicUpdate=true}, Hibernate membaca state lewat getter saat flush; artinya
	 * MEMBACA saja paket yang masih ter-attach pada session dapat menghasilkan {@code UPDATE}
	 * nyata pada kolom {@code yayasan_id} <b>plus satu revisi Envers palsu</b> — tanpa ada
	 * pengguna yang benar-benar mengubah apa pun.</p>
	 *
	 * <p>Konsekuensi praktis: nilai {@code yayasan} yang berbeda dari yayasan sekolahnya tidak
	 * dapat bertahan. Bila suatu paket sengaja dimiliki yayasan A tetapi sekolahnya milik yayasan
	 * B, pembacaan pertama akan "mengoreksi"-nya menjadi B secara permanen. Pola getter destruktif
	 * yang sama muncul di banyak entity lain pada basis kode ini.</p>
	 *
	 * @return yayasan pemilik — diturunkan dari {@link #getSekolah()} bila sekolah terisi, jika
	 *         tidak dari kolom {@code yayasan_id}; boleh {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan_id")
	public Yayasan getYayasan() {
		sekolah = getSekolah();
		if (sekolah != null) {
			yayasan = sekolah.getYayasan();
		}
		yayasan = check(yayasan);
		return this.yayasan;
	}

	/**
	 * Menyetel yayasan pemilik paket.
	 *
	 * <p>Penjagaan "objek tanpa {@code id} dianggap {@code null}" sama seperti
	 * {@link #setSekolah(Sekolah)}. Perlu diingat bahwa nilai yang disetel di sini akan DITIMPA
	 * oleh {@link #getYayasan()} pada pembacaan berikutnya apabila {@link #getSekolah()} tidak
	 * {@code null}.</p>
	 *
	 * @param yayasan yayasan pemilik; {@code null} atau objek tanpa {@code id} menjadi {@code null}
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan == null || yayasan.getId() == null ? null : yayasan;
	}

	/**
	 * Mengembalikan penjelasan bebas atas paket ini (mis. kapan paket dipakai, dasar aturannya).
	 *
	 * <p>Ditampilkan sebagai satu kolom tersendiri pada grid daftar
	 * {@code PelanggaranDanHukumanAction} dan diisi lewat {@code Textbox} 3 baris pada dialog
	 * Tambah/Ubah. Tidak dipakai sebagai kunci pencarian maupun pengurutan.</p>
	 *
	 * <p><b>Catatan konsistensi:</b> berbeda dari sejumlah entity penghubung sekawan yang sama
	 * sekali tidak punya field {@code keterangan} (sehingga getter warisan induk terpakai dan
	 * membalik kontrak), entity ini MEMILIKI kolomnya sendiri dan meng-override getter induk
	 * dengan benar.</p>
	 *
	 * @return keterangan paket, atau {@code null} bila tidak diisi
	 */
	@Column(name = "keterangan")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel penjelasan bebas atas paket.
	 *
	 * <p>Tidak ada penyaringan nilai kosong di sini — string kosong dari {@code Textbox} akan
	 * benar-benar tersimpan sebagai string kosong, bukan {@code null}.</p>
	 *
	 * @param keterangan teks keterangan; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan label paket — teks yang dilihat petugas saat memilih jenis pelanggaran pada
	 * form pencatatan siswa.
	 *
	 * <p>Kolom {@code NOT NULL} dan satu-satunya field yang divalidasi wajib-isi oleh
	 * {@code onSave()}. Juga menjadi:</p>
	 * <ul>
	 * <li>label item combobox pada {@code PelanggaranSiswaAction} ({@code insertCombo(..., "nama",
	 * ...)});</li>
	 * <li>label tombol revisi Envers pada grid master;</li>
	 * <li>nilai parameter {@code "pelanggaranDanHukuman"} pada rapor siswa
	 * ({@code LaporanRaporSiswa});</li>
	 * <li>sumbu pengelompokan pada {@code DasbordPelanggaran};</li>
	 * <li>penyusun {@code nama} turunan milik {@code PelanggaranSiswa}.</li>
	 * </ul>
	 *
	 * <p><b>Kunci pengurutan efektif.</b> Entity ini tidak meng-override {@code compareTo}, dan
	 * karena tidak punya {@code nomorUrut} maupun {@code nim}, implementasi induk jatuh ke
	 * {@code nama}. Itulah kunci yang dipakai {@code Collections.sort(...)} di dalam
	 * {@code Common.insertCombo(...)} saat menyusun combobox pemilihan paket. Tidak ada batasan
	 * {@code UNIQUE} pada kolom ini dan combobox tersebut tidak menyaring sekolah, sehingga dua
	 * paket ber-nama sama dari sekolah berbeda dapat tampil berdampingan tanpa pembeda apa pun di
	 * layar.</p>
	 *
	 * @return label paket; secara skema {@code NOT NULL}, tetapi baris warisan teoretis tetap bisa
	 *         mengembalikan {@code null} sehingga pemanggil sebaiknya berjaga-jaga
	 */
	@Column(name = "nama", nullable = false)
	public String getNama() {
		return this.nama;
	}

	/**
	 * Menyetel label paket.
	 *
	 * <p>Nilai berasal langsung dari {@code Textbox} pada dialog Tambah/Ubah setelah dipastikan
	 * tidak kosong oleh {@code onSave()}. Mengubahnya bersifat RETROAKTIF: seluruh catatan
	 * {@code PelanggaranSiswa} lama menampilkan nama baru, termasuk pada rapor yang dicetak
	 * ulang.</p>
	 *
	 * @param nama label paket
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan status aktif paket, dengan {@code null} DIPERLAKUKAN SEBAGAI {@code true}.
	 *
	 * <p>Perlakuan itu penting: kolom ini tidak punya nilai bawaan di skema, sehingga baris lama
	 * (dan baris yang dibuat lewat impor massal) berisi {@code null} dan tetap dianggap aktif.
	 * Karena getter tidak menulis balik ke field, tidak ada efek samping {@code UPDATE} di sini —
	 * berbeda dari {@link #getYayasan()}.</p>
	 *
	 * <p><b>TEMUAN: nilai ini nyaris tidak dihormati siapa pun.</b> Satu-satunya pembaca yang
	 * ditemukan adalah checkbox "Aktif" pada grid master ({@code PelanggaranDanHukumanRenderer}),
	 * yang menyimpannya kembali lewat {@code Common.refreshSaveOrUpdate(...)} begitu diklik.
	 * Combobox pemilihan paket pada {@code PelanggaranSiswaAction} diisi
	 * {@code Common.insertCombo(...)} <b>tanpa {@code Criterion} apa pun</b>, sehingga paket yang
	 * sudah dinonaktifkan TETAP dapat dipilih untuk mencatat pelanggaran baru. Bandingkan dengan
	 * daftar centang {@code Pelanggaran}/{@code Hukuman} di dialog paket yang justru menyaring
	 * {@code aktif} dengan benar — inkonsistensi antar layar, bukan desain yang disengaja.</p>
	 *
	 * @return {@code true} bila paket aktif atau statusnya belum pernah diisi; {@code false} hanya
	 *         bila secara eksplisit dinonaktifkan. Tidak pernah {@code null}.
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel status aktif paket.
	 *
	 * <p>Dipanggil dari listener {@code onCheck} checkbox "Aktif" di grid master, yang langsung
	 * disusul {@code Common.refreshSaveOrUpdate(...)} — jadi satu klik centang sudah menulis ke
	 * basis data (dan membuat satu revisi Envers) tanpa konfirmasi. Checkbox itu di-{@code disable}
	 * bila pengguna tidak memiliki hak {@code UPDATE}.</p>
	 *
	 * @param aktif status aktif; {@code null} berarti "belum diisi" dan akan dibaca sebagai
	 *              {@code true} oleh {@link #getAktif()}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan tenant terluar (instansi) pemilik paket, DENGAN PENGISIAN OTOMATIS.
	 *
	 * <p><b>GETTER DESTRUKTIF — baca sebelum memakai.</b> Bila field masih {@code null}, method ini
	 * mengisinya dari konteks sesi lewat
	 * {@code ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi()}. Sama seperti
	 * {@link #getYayasan()}, karena pemetaan memakai <i>property access</i> dengan
	 * {@code dynamicUpdate=true}, pengisian itu dapat ter-<i>flush</i> menjadi {@code UPDATE}
	 * nyata pada kolom {@code perguruan_tinggi} berikut revisi Envers-nya — sekadar dari membaca
	 * baris warisan yang kolomnya masih kosong.</p>
	 *
	 * <p>Bahayanya spesifik pada entity ini: nilai yang terisi berasal dari SESI PENGGUNA YANG
	 * KEBETULAN MEMBACA, bukan dari pemilik data yang sebenarnya. Pada instalasi multi-instansi,
	 * baris warisan bisa "diklaim" oleh instansi mana pun yang lebih dulu membukanya.</p>
	 *
	 * <p>Seluruh blok pengisian dibungkus {@code try/catch} yang menelan setiap {@code Exception}
	 * (kini diteruskan ke {@code ais.common.ErrorAuditUtil.record(...)} agar tidak hilang total),
	 * sehingga getter ini tidak pernah gagal — bila konteks sesi tidak tersedia, nilainya tetap
	 * {@code null}.</p>
	 *
	 * @return instansi pemilik; boleh {@code null} bila kolom kosong DAN konteks sesi tidak
	 *         tersedia (mis. dipanggil dari job latar atau dari servlet tanpa sesi)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "perguruan_tinggi")
	public PerguruanTinggi getPerguruanTinggi() {
		perguruanTinggi = check(perguruanTinggi);
		try {
			if (perguruanTinggi == null) {
				perguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/PelanggaranDanHukuman.java:197");
		}
		return perguruanTinggi;
	}

	/**
	 * Menyetel instansi pemilik paket.
	 *
	 * <p>Tidak ada penjagaan "objek tanpa {@code id}" seperti pada {@link #setSekolah(Sekolah)} dan
	 * {@link #setYayasan(Yayasan)} — nilai apa pun langsung diterima. {@code onSave()} pada layar
	 * CRUD selalu memanggilnya dengan hasil {@code PerguruanTinggiUtil.getPerguruanTinggi()} yang
	 * diambil sekali saat {@code doAfterCompose}, sehingga setiap penyimpanan menetapkan ulang
	 * instansi ke instansi pengguna yang menyimpan.</p>
	 *
	 * @param perguruanTinggi instansi pemilik; boleh {@code null}
	 */
	public void setPerguruanTinggi(PerguruanTinggi perguruanTinggi) {
		this.perguruanTinggi = perguruanTinggi;
	}

}
