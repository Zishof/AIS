package ais.database.model.sekolah;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.envers.Audited;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;

/**
 * Entity MASTER/KATALOG kategori kebutuhan khusus (disabilitas, keberbakatan, dan kondisi belajar
 * khusus) siswa pada modul sekolah — satu baris tabel {@code sekolah.kebutuhan_khusus_siswa}
 * mewakili SATU nama kategori, mis. "Tuna netra", "Autis", "Cerdas istimewa", "Kesulitan belajar".
 *
 * <p><b>Kelas ini BUKAN data pribadi siswa.</b> Ia hanya berisi DAFTAR PILIHAN yang ditampilkan
 * sebagai deretan {@code Checkbox} pada formulir pendaftaran. Data kebutuhan khusus milik anak yang
 * sesungguhnya — yang merupakan data kesehatan/disabilitas dan karenanya sangat sensitif secara
 * privasi — TIDAK disimpan di sini, melainkan sebagai satu kolom teks datar pada entity lain:</p>
 * <ul>
 * <li>{@code ais.database.model.sekolah.CalonSiswa#getKebutuhanKhusus()} — kolom
 * {@code text} pada tabel calon siswa (jalur PSB/PPDB);</li>
 * <li>{@code ais.database.model.sekolah.Siswa#getKebutuhanKhusus()} — kolom teks pada tabel
 * siswa aktif.</li>
 * </ul>
 * Keduanya menyimpan STRING bertanda pemisah titik-koma, bukan relasi FK ke entity ini. Tidak ada
 * tabel penghubung, tidak ada {@code @ManyToMany}, dan tidak ada satu pun FK di seluruh basis kode
 * yang menunjuk ke {@code sekolah.kebutuhan_khusus_siswa}. Konsekuensinya: menghapus atau mengubah
 * nama sebuah baris di sini TIDAK merusak integritas referensial, tetapi juga TIDAK memperbarui
 * data siswa yang sudah tersimpan — nilai lama tetap tertinggal sebagai teks bebas.
 *
 * <h3>Siapa yang memakai katalog ini (hasil penelusuran seluruh kode sumber)</h3>
 * <ol>
 * <li><b>{@code ais.action.master.sekolah.KebutuhanKhususSiswaAction}</b> — layar master CRUD
 * ({@code /pages/master/sekolah/kebutuhan_khusus_siswa.zul}, dan padanan New-UI
 * {@code WEB-INF/new/sekolah/uiux/kebutuhan_khusus_siswa.jsp}). Layar ini hanya mengelola
 * {@code nama} dan {@code keterangan}; kolom {@link #getAktif() aktif} diubah lewat centang di
 * grid, bukan lewat formulir tambah/ubah.</li>
 * <li><b>{@code ais.action.master.sekolah.psb.form.PPDB1}</b> dan
 * <b>{@code ...psb.form.PPDB2}</b> — formulir biodata calon siswa. Keduanya memuat seluruh baris
 * ber-{@code aktif} true/NULL, mengurutkannya menurut {@code nama}, lalu merender satu
 * {@code Checkbox} per baris (pertanyaan nomor 19 pada PPDB1: "Apakah peserta didik berkebutuhan
 * khusus ?").</li>
 * <li><b>{@code ais.common.InitData}</b> — dua peran: (a) memanggil {@link #reloadDefault()} sekali
 * saat aplikasi naik untuk menyemai 17 kategori bawaan, dan (b) mendaftarkan kelas ini pada
 * {@code initClasses(...)} sehingga skema tabel audit Envers ikut diselaraskan saat start-up.</li>
 * <li><b>{@code ais.action.master.sekolah.GelombangPendaftaranPsbAction}</b> — hanya menyematkan
 * layar master di atas sebagai {@code MyInclude} pada salah satu tab pengaturan gelombang PSB;
 * tidak menyentuh entity secara langsung.</li>
 * </ol>
 * Tidak ada pembaca lain: tidak ada laporan, tidak ada REST/mobile API khusus, tidak ada agregasi
 * statistik, dan tidak ada penggunaan pada rapor.
 *
 * <h3>Kembaran di modul perguruan tinggi</h3>
 * {@code ais.database.model.KebutuhanKhusus} (tabel {@code public.kebutuhan_khusus}) adalah salinan
 * struktur yang nyaris identik untuk sisi perguruan tinggi — dipakai
 * {@code ais.action.master.feeder.util.FeederConverter}/{@code FeederImporter} (sinkronisasi
 * PDDikti). Keduanya bahkan berbagi {@code serialVersionUID} yang sama persis; lihat catatan pada
 * {@link #serialVersionUID}. Kedua kelas TIDAK saling merujuk dan tidak berbagi data.
 *
 * <h3>Tidak ada cakupan multi-tenant sama sekali</h3>
 * Entity ini TIDAK memiliki field {@code sekolah}, {@code yayasan}, maupun {@code perguruanTinggi}.
 * Katalog bersifat GLOBAL untuk seluruh instalasi: satu daftar dipakai bersama oleh semua sekolah
 * dan semua yayasan yang menumpang pada basis data yang sama. Ini bukan kasus "fail-open" (filter
 * yang gagal), melainkan memang tidak ada sumbu penyaringan yang bisa dipakai. Implikasi
 * praktisnya: pengguna dengan hak {@code UPDATE}/{@code DELETE} pada menu ini di SATU sekolah dapat
 * mengganti nama, menonaktifkan, atau menghapus kategori yang sedang dipakai formulir PPDB SELURUH
 * sekolah lain pada instalasi tersebut. Karena isinya metadata katalog (bukan PII), dampaknya
 * bersifat integritas/ketersediaan data, bukan kebocoran privasi.
 *
 * <h3>BUG BESAR: nilai yang DITULIS dan nilai yang DIBACA tidak pernah cocok</h3>
 * Ini temuan terpenting bagi siapa pun yang akan menyentuh modul ini, dan berlaku SIMETRIS di
 * {@code PPDB1} maupun {@code PPDB2}:
 * <ul>
 * <li><b>Saat menyimpan</b>, {@code CalonSiswa.kebutuhanKhusus(Box)} (helper statis) menyusun
 * string dari <b>LABEL</b> setiap checkbox yang tercentang, dihuruf-kecilkan:
 * {@code info += ";" + cc.getLabel().toLowerCase() + ";"}. Hasilnya berbentuk
 * {@code ;tuna netra;;autis;}.</li>
 * <li><b>Saat memuat ulang formulir</b>, kode pemulih centang justru mencari <b>ID</b>:
 * {@code calonSiswa.getKebutuhanKhusus().contains(";" + khusus.getId() + ";")}.</li>
 * </ul>
 * Karena {@code nama} kategori tidak pernah sama dengan {@code id} numeriknya, kondisi itu
 * PRAKTIS TIDAK PERNAH bernilai benar. Akibat berantai yang nyata:
 * <ol>
 * <li>Setiap kali calon siswa (atau petugas) membuka kembali formulir yang sudah diisi, seluruh
 * centang kebutuhan khusus tampil KOSONG walaupun datanya ada di basis data.</li>
 * <li>Bila formulir itu disimpan lagi tanpa mencentang ulang, {@code setKebutuhanKhusus("")}
 * MENGHAPUS data disabilitas anak secara senyap — kehilangan data, bukan sekadar tampilan.</li>
 * <li>Validasi wajib-unggah "surat keterangan kebutuhan khusus dari rumah sakit" pada
 * {@code PPDB1} hanya aktif bila string hasil rakitan tidak kosong; setelah centang hilang,
 * validasi itu ikut terlewati.</li>
 * </ol>
 * <b>Peringatan "bom waktu":</b> memperbaiki salah satu sisi saja (mis. mengubah penulis agar
 * menyimpan {@code id}) akan membuat seluruh data historis yang tersimpan sebagai nama menjadi
 * tidak terbaca. Perbaikan yang benar harus menangani kedua format sekaligus plus migrasi data.
 *
 * <h3>Data PPDB tidak pernah menyeberang ke siswa aktif</h3>
 * {@code ais.common.CommonPSB} — jalur konversi calon siswa menjadi {@code Siswa} — tidak
 * menyebut {@code kebutuhanKhusus} sama sekali (nol kecocokan pada seluruh berkas). Jadi kategori
 * yang dicentang saat pendaftaran TIDAK terbawa ke rekam siswa aktif. Satu-satunya penulis
 * {@code Siswa#setKebutuhanKhusus(String)} adalah {@code SiswaAction} baris ~4264, yang mengambil
 * nilai dari sebuah {@code Textbox} bebas dua baris — bukan dari katalog ini. Dengan kata lain:
 * pada rekam siswa aktif, kolom kebutuhan khusus adalah teks bebas yang diketik operator, dan
 * entity ini sama sekali tidak berperan sebagai kamus penerjemahnya.
 *
 * <h3>Pengelompokan anggota</h3>
 * <ul>
 * <li><b>Jejak audit warisan</b> — {@link #getOleh()}/{@link #setOleh(String)},
 * {@link #getOlehId()}/{@link #setOlehId(String)},
 * {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, {@link #onUpdate()}.</li>
 * <li><b>Identitas</b> — {@link #getId()}/{@link #setId(Long)}, konstruktor
 * {@link #KebutuhanKhususSiswa()}, {@link #toString()}.</li>
 * <li><b>Isi katalog</b> — {@link #getNama()}/{@link #setNama(String)},
 * {@link #getKeterangan()}/{@link #setKeterangan(String)},
 * {@link #getAktif()}/{@link #setAktif(Boolean)}.</li>
 * <li><b>Utilitas statis</b> — {@link #reloadDefault()} (satu-satunya method dengan logika nyata di
 * kelas ini).</li>
 * </ul>
 * Entity ini TIDAK memiliki {@code equals}/{@code hashCode}/{@code compareTo} sendiri, tidak punya
 * relasi JPA apa pun, dan tidak punya finder statis selain penyemai di atas.
 *
 * <h3>Hal non-obvious yang WAJIB diketahui sebelum menyentuh kelas ini</h3>
 * <ul>
 * <li><b>Komentar hbm2java di atas anotasi keliru</b> — teks aslinya berbunyi "Bank generated by
 * hbm2java", sisa salin-tempel generator dari entity {@code Bank}. Kelas ini tidak ada hubungannya
 * dengan bank. Kekeliruan yang sama terbawa ke kembarannya
 * {@code ais.database.model.KebutuhanKhusus}.</li>
 * <li><b>Field warisan yang dideklarasikan ULANG bukan bug</b> — {@code id}, {@code oleh},
 * {@code olehId}, dan {@code tanggal_dirubah} sudah ada di induk
 * {@link ais.database.model.GeneralValueObject}, namun induk itu BUKAN {@code @Entity} maupun
 * {@code @MappedSuperclass} — hanya POJO abstrak biasa. Hibernate TIDAK memetakan properti induk,
 * sehingga setiap entity turunan HARUS mendeklarasikan ulang keempatnya agar tersimpan. Ini
 * keharusan teknis, jangan "dirapikan".</li>
 * <li><b>Field {@code nama}/{@code keterangan}/{@code aktif} dideklarasikan di TENGAH berkas</b>
 * (setelah {@link #reloadDefault()}), bukan di bagian atas bersama field lain. Sah secara Java —
 * {@link #toString()} di baris lebih awal tetap dapat memakainya — tetapi menyesatkan pembaca yang
 * menyangka daftar field berakhir di deklarasi {@code olehId}.</li>
 * <li><b>{@link #getAktif()} membalik kontrak</b> — nilai kolom {@code NULL} dilaporkan sebagai
 * {@code true}. Baris hasil penyemaian {@link #reloadDefault()} TIDAK PERNAH mengisi kolom
 * {@code aktif}, sehingga seluruh 17 kategori bawaan permanen ber-{@code NULL} di basis data.
 * Itulah sebabnya SETIAP query pembaca (layar master maupun PPDB) harus menulis
 * {@code Restrictions.or(isNull("aktif"), eq("aktif", true))} — melupakan cabang {@code isNull}
 * akan membuat seluruh kategori bawaan menghilang dari layar.</li>
 * <li><b>{@link #getNama()} men-{@code trim} saat dibaca, {@link #setNama(String)} tidak saat
 * ditulis</b> — asimetri ini membuat pemeriksaan duplikat di layar master
 * ({@code Restrictions.eq("nama", nilai.trim())}) meleset bila baris lama tersimpan dengan spasi
 * di ujung. Berbeda dari beberapa entity lain di repo ini, {@code getNama()} di sini TIDAK
 * destruktif: hasil {@code trim} dikembalikan tanpa ditulis balik ke field.</li>
 * <li><b>Tidak ada getter yang melakukan mutasi</b> — kelas ini bebas dari pola "getter destruktif"
 * yang tersebar di banyak entity repo ini. Namun perlu diketahui bahwa
 * {@code CalonSiswa#getKebutuhanKhusus()} — sisi data siswanya — JUSTRU destruktif: ia menormalkan
 * dan menulis balik field-nya sendiri saat dibaca, sehingga sekadar merender formulir dapat
 * memicu {@code UPDATE} dan revisi Envers semu.</li>
 * <li><b>{@code @Audited}</b> — setiap perubahan direkam Envers ke tabel audit; layar master
 * menampilkannya lewat {@code RevisiHelper.createNewRevisi(...)}.</li>
 * </ul>
 *
 * <h3>Catatan keamanan &amp; privasi (hasil audit khusus)</h3>
 * <ul>
 * <li><b>Sensitivitas isi entity ini sendiri: RENDAH.</b> Tabelnya hanya berisi nama kategori
 * generik yang identik di semua instalasi (daftar Dapodik standar). Membocorkannya tidak
 * mengungkap satu pun individu. Perlu ditegaskan agar tidak ada yang salah menyimpulkan bahwa
 * temuan-temuan di bawah adalah kebocoran data disabilitas anak — kebocoran itu terjadi di entity
 * {@code Siswa}/{@code CalonSiswa}, bukan di sini.</li>
 * <li><b>Terjangkau anonim lewat {@code /Data}.</b> Servlet {@code ais.action.servlet.Data}
 * menerima penanda {@code tanpaLogin=true} dari klien dan melewati cek login untuk seluruh aksi
 * baca; resolusi kelas entity ({@code DaftarDataService.resolveKelasEntitas}) hanya mensyaratkan
 * turunan {@link ais.database.model.GeneralValueObject} tanpa daftar putih/hitam, sehingga kelas
 * ini terbaca dan terekspor tanpa sesi. Ini instance lain dari pola yang sudah tercatat pada task
 * audit {@code task_493423ef}; keparahannya di sini rendah karena isi tabelnya bukan PII.</li>
 * <li><b>{@code /Api} rute {@code daftar}/{@code load}</b> mewajibkan token, tetapi tidak memeriksa
 * tenant maupun kepemilikan — token siswa/orang tua mana pun cukup untuk membaca katalog ini.
 * Sekali lagi, dampaknya rendah untuk tabel ini.</li>
 * <li><b>Tombol ekspor tanpa gerbang privilese.</b> {@code KebutuhanKhususSiswaAction} memasang
 * {@code Common.cetakData(...)} tanpa {@code CommonPrivilages.checkPrevilages(READ)} — berbeda dari
 * tombol tambah/ubah/hapus di layar yang sama. Konsisten dengan pola yang dicatat pada
 * {@code task_4ca32776}; berdampak rendah untuk katalog ini.</li>
 * <li><b>Impor massal dapat menimpa baris.</b> {@code Common.uploadData(...)} pada layar master
 * menyertakan kolom {@code "id"}, dan alur unggahnya berakhir pada {@code session.saveOrUpdate}.
 * Berkas Excel yang menyebut {@code id} yang sudah ada akan MENIMPA baris tersebut. Gerbangnya
 * hanya visibilitas tombol, bukan penolakan di sisi eksekusi.</li>
 * <li><b>{@code Common.doCheckSecurity()} pada layar master efektif no-op</b> — implementasinya
 * hanya memeriksa 12 path ZUL yang di-hardcode pada {@code CommonPrivilages.MUST_CHECKED}, dan
 * {@code kebutuhan_khusus_siswa.zul} tidak termasuk. Pemanggilannya memberi kesan terlindungi
 * padahal tidak.</li>
 * <li><b>Jalur New-UI justru sehat</b> — {@code WEB-INF/new/_shared/services/dispatcher.jsp}
 * menolak pemanggil tanpa sesi (401), memakai {@code NewUiRouteGuard} yang fail-closed, dan
 * mewajibkan CSRF + POST untuk aksi mutasi.</li>
 * </ul>
 *
 * @see ais.database.model.GeneralValueObject
 * @see ais.database.model.KebutuhanKhusus
 * @see ais.database.model.sekolah.CalonSiswa
 * @see ais.database.model.sekolah.Siswa
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true, 
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "kebutuhan_khusus_siswa")
public class KebutuhanKhususSiswa extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java.
	 *
	 * <p>Nilai {@code 2463821577548439808L} DIPAKAI ULANG persis oleh
	 * {@code ais.database.model.KebutuhanKhusus} (tabel {@code public.kebutuhan_khusus}, sisi
	 * perguruan tinggi) — bukti langsung bahwa salah satu berkas adalah salinan berkas lainnya.
	 * Tidak berbahaya, karena mekanisme serialisasi Java tetap memeriksa nama kelas.</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama baris, dipetakan ke kolom {@code id}. Lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris. Lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris. Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini.
	 *
	 * <p>Diisi otomatis oleh {@code ais.database.hibernate.AuditTimestampInterceptor} lewat callback
	 * {@link #onUpdate()}. Tidak dipetakan dengan {@code @Column} eksplisit, sehingga Hibernate
	 * memakai nama kolom bawaan hasil penamaan default ({@code olehid}).</p>
	 *
	 * @return id pengguna, atau {@code null} bila baris belum pernah diubah lewat jalur ber-audit
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan id pengguna terakhir yang mengubah baris ini.
	 *
	 * <p><b>Perilaku non-obvious:</b> nilai {@code null} atau kosong (setelah {@code trim})
	 * DIABAIKAN DIAM-DIAM — method langsung {@code return} tanpa menulis apa pun. Tujuannya menjaga
	 * agar jejak audit lama tidak terhapus oleh pemanggil yang kebetulan mengirim nilai kosong.
	 * Efek sampingnya: setter ini TIDAK dapat dipakai untuk mengosongkan jejak audit.</p>
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau kosong setelah di-{@code trim}
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, nilai {@code null}/kosong diabaikan diam-diam
	 * sehingga jejak audit lama tidak pernah terhapus.</p>
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau kosong setelah di-{@code trim}
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil kontainer persistence TEPAT SEBELUM pernyataan
	 * {@code UPDATE} dikirim ke basis data.
	 *
	 * <p><b>Efek samping:</b> mendelegasikan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} yang mengisi
	 * {@link #getOleh() oleh}/{@link #getOlehId() olehId} dari pengguna sesi aktif dan menyegarkan
	 * {@link #getTanggal_dirubah()}. TIDAK ada {@code @PrePersist}, sehingga baris BARU mengandalkan
	 * nilai awal field {@code tanggal_dirubah} (diinisialisasi ke
	 * {@code ais.ui.util.WaktuUtil.getDate()} saat objek dibuat) dan tidak pernah mendapat
	 * {@code oleh}/{@code olehId} dari jalur ini — termasuk 17 baris hasil {@link #reloadDefault()}
	 * yang memang tercipta tanpa pengguna sesi.</p>
	 *
	 * <p><b>Catatan tata letak:</b> baris kode ini memuat DUA deklarasi sekaligus — method
	 * {@code onUpdate()} dan field {@code tanggal_dirubah} — hasil penyisipan otomatis oleh
	 * perkakas migrasi. Jangan dipisah tanpa alasan; Javadoc ini mendokumentasikan keduanya.</p>
	 *
	 * <p>Perlu disadari bahwa satu klik centang "Aktif" pada layar daftar (lihat
	 * {@link #setAktif(Boolean)}) sudah cukup untuk memicu jalur ini, menimpa jejak audit, dan
	 * menciptakan satu revisi Envers baru.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir baris ini.
	 *
	 * <p>Dalam praktik hanya dipanggil oleh {@code AuditTimestampInterceptor} lewat
	 * {@link #onUpdate()}; tidak ada pemanggil manual di modul kebutuhan khusus.</p>
	 *
	 * @param tanggal_dirubah stempel waktu baru; nilai {@code null} diterima apa adanya
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini, dipetakan sebagai
	 * {@code TIMESTAMP}.
	 *
	 * @return waktu perubahan terakhir; untuk baris yang belum pernah di-{@code UPDATE} berisi
	 *         waktu pembuatan objek Java, bukan {@code null}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris ini dalam bentuk {@code "<id>-<nama>"}.
	 *
	 * <p><b>Non-obvious:</b> method ini membaca FIELD {@code nama} secara langsung, bukan lewat
	 * {@link #getNama()}, sehingga spasi di ujung TIDAK ikut dipangkas — hasilnya bisa berbeda dari
	 * apa yang ditampilkan grid. Untuk baris yang belum tersimpan, {@code id} masih {@code null}
	 * sehingga keluarannya berawalan {@code "null-"}.</p>
	 *
	 * <p>Tidak ada pemanggil eksplisit di modul ini; nilainya muncul hanya bila objek dirangkai ke
	 * dalam string oleh perkakas umum (log, pesan debug, atau renderer generik).</p>
	 *
	 * @return gabungan id dan nama dipisah tanda hubung
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Menyemai 17 kategori kebutuhan khusus bawaan bila tabel {@code sekolah.kebutuhan_khusus_siswa}
	 * masih KOSONG.
	 *
	 * <p><b>Dipanggil dari:</b> {@code ais.common.InitData.reloadDefaults()} — sekali saat aplikasi
	 * naik, dijalankan di dalam {@code Runnable} pada thread executor bersama belasan penyemai
	 * lain. Tidak ada pemanggil lain di seluruh basis kode; khususnya, layar master TIDAK
	 * menyediakan tombol "kembalikan ke bawaan".</p>
	 *
	 * <p><b>Alur:</b>
	 * <ol>
	 * <li>Membuka {@code Session} native lewat {@code HibernateUtil.currentNativeSession()}.</li>
	 * <li>Menghitung jumlah baris dengan {@code Projections.rowCount()}. <b>Hanya bila hasilnya
	 * TEPAT 0</b> penyemaian dijalankan; jadi ini penyemaian sekali-seumur-instalasi, bukan
	 * penyelarasan. Bila admin menghapus SEBAGIAN kategori, sisanya tidak pernah dikembalikan;
	 * bila admin menghapus SEMUANYA, seluruh 17 baris muncul lagi pada restart berikutnya
	 * <b>dengan id baru</b> — yang tidak berpengaruh apa pun karena tidak ada FK yang menunjuk ke
	 * sini.</li>
	 * <li>Memecah satu literal string berpemisah koma menjadi 17 nama, lalu untuk SETIAP nama
	 * membuka transaksi sendiri, {@code save}, dan {@code commit} — 17 transaksi terpisah, bukan
	 * satu batch. Konsekuensinya kegagalan di tengah meninggalkan tabel terisi sebagian, dan
	 * karena penjaga di langkah 2 memakai "jumlah == 0", sisa kategori TIDAK akan dilengkapi pada
	 * restart berikutnya.</li>
	 * </ol>
	 *
	 * <p><b>Isi bawaan</b> (mengikuti daftar standar Dapodik): Tuna netra, Tuna rungu, Tuna
	 * grahita ringan, Tuna grahita sedang, Tuna daksa ringan, Tuna daksa sedang, Tuna laras, Tuna
	 * wicara, Tuna ganda, Hiperaktif, Cerdas istimewa, Bakat istimewa, Kesulitan belajar, Indigo,
	 * Down syndrome, Autis, Narkoba.</p>
	 *
	 * <p><b>Kuirk isi:</b> {@code keterangan} diisi dengan nilai yang SAMA PERSIS dengan
	 * {@code nama}, sehingga kolom keterangan pada layar master menduplikasi kolom nama sampai
	 * seseorang menyuntingnya. Selain itu {@code aktif} TIDAK pernah diisi, sehingga seluruh baris
	 * bawaan permanen ber-{@code NULL} — lihat penjelasan konsekuensinya pada
	 * {@link #getAktif()}.</p>
	 *
	 * <p><b>Kuirk penamaan variabel:</b> variabel lokal bernama {@code grupLaporan}, sisa
	 * salin-tempel dari penyemai entity lain. Tidak berpengaruh pada perilaku.</p>
	 *
	 * <p><b>Penanganan galat:</b> seluruh badan method dibungkus {@code try/catch (Exception)} yang
	 * mencetak stack trace dan mencatatnya ke {@code ais.common.ErrorAuditUtil}. Kegagalan
	 * penyemaian TIDAK menghentikan start-up aplikasi — instalasi hanya berjalan tanpa kategori
	 * bawaan, dan formulir PPDB akan menampilkan daftar kosong.</p>
	 *
	 * <p><b>Efek samping pada session:</b> setelah selesai, session native di-{@code disconnect} dan
	 * di-{@code close} bila masih terbuka, lalu {@code HibernateUtil.closeSession()} dipanggil untuk
	 * membersihkan session terikat-thread. Wajib diperhatikan bila method ini pernah dipanggil dari
	 * konteks yang masih membutuhkan session aktif setelahnya.</p>
	 */
	public static void reloadDefault() {
		Session session = HibernateUtil.currentNativeSession();
		try {
			Number a = (Number) session.createCriteria(KebutuhanKhususSiswa.class).setProjection(Projections.rowCount())
					.uniqueResult();
			if (a.intValue() == 0) {

				String namas = "Tuna netra,Tuna rungu,Tuna grahita ringan,Tuna grahita sedang,"
						+ "Tuna daksa ringan,Tuna daksa sedang,Tuna laras,Tuna wicara,Tuna ganda,"
						+ "Hiperaktif,Cerdas istimewa,Bakat istimewa,Kesulitan belajar,Indigo,"
						+ "Down syndrome,Autis,Narkoba";

				for (String s : namas.split(",")) {
					KebutuhanKhususSiswa grupLaporan = new KebutuhanKhususSiswa();
					grupLaporan.setNama(s);
					grupLaporan.setKeterangan(s);
					session.getTransaction().begin();
					session.save(grupLaporan);
					session.getTransaction().commit();
				}
			}

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/sekolah/KebutuhanKhususSiswa.java:98");
		}
		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();
	}

	/** Nama kategori kebutuhan khusus. Lihat {@link #getNama()}. */
	private String nama;
	/** Keterangan bebas kategori. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Penanda kategori masih dipakai. Lihat {@link #getAktif()} untuk semantik {@code NULL}. */
	private Boolean aktif;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA.
	 *
	 * <p>Juga dipakai langsung oleh {@link #reloadDefault()} dan oleh
	 * {@code KebutuhanKhususSiswaAction.onAdd(...)} saat pengguna menekan tombol "Tambah". Tidak
	 * ada konstruktor lain; seluruh properti diisi lewat setter.</p>
	 */
	public KebutuhanKhususSiswa() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Dipetakan sebagai {@code IDENTITY} (kolom serial basis data), sehingga id bersifat
	 * BERURUTAN dan mudah ditebak — relevan bila baris ini pernah dirujuk lewat parameter
	 * permintaan.</p>
	 *
	 * <p><b>Non-obvious:</b> {@code insertable = false} berarti kolom ini TIDAK ikut dalam
	 * pernyataan {@code INSERT}; nilainya sepenuhnya ditentukan basis data lalu dibaca balik.
	 * Menyetel id secara manual sebelum {@code save} karena itu tidak berpengaruh.</p>
	 *
	 * @return id baris, atau {@code null} bila objek belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama baris ini.
	 *
	 * <p>Dipakai Hibernate saat memuat/menyimpan. Karena kolom {@code id} ber-{@code insertable
	 * = false}, pemanggilan manual tidak memengaruhi hasil {@code INSERT}.</p>
	 *
	 * @param id kunci utama baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama kategori kebutuhan khusus, sudah di-{@code trim}.
	 *
	 * <p>Ini adalah teks yang menjadi LABEL checkbox pada formulir PPDB, sekaligus — karena bug
	 * yang dijelaskan pada Javadoc kelas — teks yang benar-benar TERSIMPAN ke kolom
	 * {@code kebutuhan_khusus} milik calon siswa (dihuruf-kecilkan). Jadi mengganti nama sebuah
	 * kategori membuat data calon siswa lama yang menyimpan nama versi sebelumnya menjadi tidak
	 * lagi cocok dengan kategori mana pun.</p>
	 *
	 * <p><b>Non-obvious:</b> pemangkasan dilakukan pada SAAT BACA dan hasilnya TIDAK ditulis balik
	 * ke field (getter ini tidak destruktif), sementara {@link #setNama(String)} menyimpan apa
	 * adanya. Asimetri ini membuat pemeriksaan duplikat di layar master — yang membandingkan
	 * langsung ke kolom basis data — meleset untuk baris yang tersimpan dengan spasi di ujung.</p>
	 *
	 * <p>Kolom bersifat {@code NOT NULL} sepanjang 255 karakter; nilai {@code null} hanya mungkin
	 * pada objek yang belum tersimpan, dan getter mengembalikan {@code null} apa adanya untuk kasus
	 * itu.</p>
	 *
	 * @return nama kategori tanpa spasi di ujung, atau {@code null} bila field belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama kategori kebutuhan khusus.
	 *
	 * <p>Menyimpan nilai APA ADANYA tanpa {@code trim} dan tanpa validasi panjang; pemeriksaan
	 * "harus diisi" dan "sudah ada di database" dilakukan di layar
	 * {@code KebutuhanKhususSiswaAction.onSave(...)}, bukan di sini. Jalur impor Excel dan jalur
	 * {@link #reloadDefault()} melewati pemeriksaan tersebut sepenuhnya.</p>
	 *
	 * @param nama nama kategori baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas kategori.
	 *
	 * <p>Getter ini TIDAK membalik kontrak: kolom {@code keterangan} nyata ada dan dikembalikan apa
	 * adanya (tanpa {@code trim}, tanpa mutasi). Ditampilkan sebagai kolom kedua grid layar master
	 * dan sebagai {@code Textbox} 3 baris pada formulir tambah/ubah; TIDAK pernah ditampilkan di
	 * formulir PPDB — calon siswa hanya melihat {@link #getNama()}.</p>
	 *
	 * <p>Pada baris hasil penyemaian, isinya sama persis dengan {@code nama} — lihat
	 * {@link #reloadDefault()}.</p>
	 *
	 * @return keterangan, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas kategori.
	 *
	 * @param keterangan teks keterangan baru; {@code null} diterima (kolom {@code nullable})
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan penanda apakah kategori masih dipakai, dengan {@code NULL} DIANGGAP AKTIF.
	 *
	 * <p><b>Ini pembalikan kontrak yang wajib disadari.</b> Kolom {@code aktif} tidak pernah diisi
	 * oleh {@link #reloadDefault()}, sehingga ke-17 kategori bawaan permanen bernilai {@code NULL}
	 * di basis data. Getter menutupi hal itu dengan mengembalikan {@code true}, tetapi query
	 * berbasis {@code Criteria} membaca KOLOM, bukan getter. Itulah sebabnya setiap pembaca harus
	 * menulis {@code Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif",
	 * true))}:</p>
	 * <ul>
	 * <li>{@code KebutuhanKhususSiswaAction.initCriteria(...)} — filter "Tampilkan hanya yang
	 * aktif" pada layar master;</li>
	 * <li>{@code PPDB1} dan {@code PPDB2} — daftar checkbox formulir calon siswa.</li>
	 * </ul>
	 * Query baru yang lupa menyertakan cabang {@code isNull} akan membuat SELURUH kategori bawaan
	 * hilang dari layar, padahal barisnya ada.
	 *
	 * <p>Kolom ini tidak beranotasi {@code @Column}, jadi memakai nama kolom bawaan
	 * ({@code aktif}).</p>
	 *
	 * @return {@code true} bila kolom bernilai {@code NULL} atau {@code true}; {@code false} hanya
	 *         bila kolom benar-benar berisi {@code false}
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel penanda aktif kategori.
	 *
	 * <p><b>Dipanggil dari:</b> satu-satunya jalur UI adalah centang "Aktif" pada setiap baris grid
	 * layar master ({@code KebutuhanKhususSiswaRenderer}), yang langsung diikuti
	 * {@code Common.refreshSaveOrUpdate(...)} — jadi perubahan TERSIMPAN SEKETIKA tanpa tombol
	 * simpan dan tanpa dialog konfirmasi. Centang itu dinonaktifkan bila pengguna tidak memegang
	 * privilese {@code UPDATE}. Formulir tambah/ubah TIDAK memuat kolom ini sama sekali, sehingga
	 * kategori yang baru dibuat lewat formulir selalu lahir ber-{@code NULL}.</p>
	 *
	 * <p><b>Efek samping:</b> penyimpanan memicu {@link #onUpdate()} (menimpa jejak audit) dan
	 * menciptakan satu revisi Envers. Menonaktifkan sebuah kategori menghilangkannya dari formulir
	 * PPDB seluruh sekolah pada instalasi ini — entity tidak punya cakupan tenant.</p>
	 *
	 * @param aktif nilai baru; {@code null} akan dibaca kembali sebagai {@code true} oleh
	 *              {@link #getAktif()}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

}
