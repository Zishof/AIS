package ais.database.model.sekolah;
// Generated 30 Sep 18 6:57:43 by Hibernate Tools 5.2.3.Final

import static javax.persistence.GenerationType.IDENTITY;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import org.apache.commons.lang.StringUtils;
import org.hibernate.envers.Audited;
import org.json.JSONObject;

import ais.action.master.sekolah.util.GrupPenilaianUtil;
import ais.common.ConstantValues;
import ais.ui.util.WaktuUtil;

/**
 * Baris keanggotaan seorang peserta di dalam satu <b>kelas les</b> — <b>satu baris roster kelas
 * les</b> (tabel {@code sekolah.kelas_les_punya_siswa}).
 *
 * <h2>Peran dalam arsitektur</h2>
 * <p>Entity ini adalah tabel penghubung yang <i>diperkaya</i> antara
 * {@link ais.database.model.sekolah.KelasLesSiswa} (kelas les/ekstrakurikuler, tabel
 * {@code sekolah.kelas_les}) dan pesertanya. Arah relasinya sudah <b>diverifikasi dari kode</b>:
 * FK {@code kelas_id} ({@link #getKelasLesSiswa()}) berada di sisi entity ini, dan
 * {@code KelasLesSiswa} <b>tidak</b> mendeklarasikan koleksi balik ke sini sama sekali — relasi
 * murni <b>satu arah anak &rarr; induk</b>, tanpa {@code cascade} dari induk. Konsekuensinya:
 * (1) setiap pemanggil yang butuh daftar peserta harus mem-query sendiri dengan
 * {@code createCriteria(KelasLesSiswaPunyaSiswa.class).add(Restrictions.eq("kelasLesSiswa", kelas))};
 * (2) menghapus satu {@code KelasLesSiswa} <b>tidak</b> menghapus rosternya — baris yatim mungkin
 * tertinggal di tabel.</p>
 *
 * <h2>Pesertanya bisa {@code Siswa} ATAU {@code CalonSiswa} (verifikasi ulang)</h2>
 * <p>Berbeda dari roster kelas reguler, entity ini punya <b>dua</b> FK peserta yang terpisah:</p>
 * <ul>
 *   <li>{@link #getSiswa()} &rarr; kolom {@code siswa_id}, dideklarasikan
 *       {@code nullable = false};</li>
 *   <li>{@link #getCalonSiswa()} &rarr; kolom {@code calon_siswa}, dideklarasikan
 *       {@code nullable = true}.</li>
 * </ul>
 * <p>Hasil penelusuran seluruh delapan titik pembuatan baris di repo
 * ({@code DetailKelasLesSiswaHelper} 3x, {@code KelasLesSiswaAction},
 * {@code SiswaAction}, {@code TagihanSiswa}, {@code CommonPSB.masukkanKelasLes},
 * {@code CalonSiswa.populatePembayaran}) menunjukkan pola yang konsisten: <b>{@code siswa} selalu
 * diisi</b>, sedangkan {@code calonSiswa} diisi <i>sebagai tambahan</i> untuk merekam asal-usul
 * PPDB. Jadi "peserta bisa {@code CalonSiswa}" berarti <b>calon siswa yang sudah punya baris
 * {@code Siswa} bayangan</b>, bukan calon siswa tanpa {@code Siswa} — lihat
 * {@code CommonPSB.masukkanKelasLes(calonSiswa, siswa)} yang mengisi keduanya sekaligus. Meski
 * begitu {@link #ada()} dan {@link #ambilBelumBayar()} tetap ditulis defensif: keduanya menerima
 * kemungkinan salah satu sisi {@code null} dan menggabungkan riwayat pembayaran dari kedua sisi.</p>
 *
 * <h2>Kelas les adalah kelas BERBAYAR — entity inilah penegaknya</h2>
 * <p>Bagian paling non-obvious dari berkas ini. {@link ais.database.model.sekolah.KelasLesSiswa}
 * hanya <i>menyimpan</i> syarat pembayaran; yang <i>menegakkan</i>-nya adalah entity ini, lewat
 * tiga method yang saling berkaitan:</p>
 * <ol>
 *   <li>{@link #ada()} — memeriksa apakah seluruh syarat CSV
 *       ({@code KelasLesSiswa.getSyaratPengaturanPembayaran()}) sudah tercatat pada riwayat
 *       pembayaran peserta ({@code Siswa.getRiwayatPengaturanPembayaran()} dan padanan
 *       {@code CalonSiswa}), dengan pengecualian untuk syarat bulanan yang belum jatuh tempo.</li>
 *   <li>{@link #getAktif()} — <b>menimpa</b> kolom {@code aktif} dengan hasil {@link #ada()}.
 *       Praktisnya: peserta yang menunggak otomatis berubah menjadi tidak aktif, tanpa ada yang
 *       menekan tombol apa pun; dan di layar detail checkbox "Aktif"-nya sekalian
 *       di-{@code disable}.</li>
 *   <li>{@link #ambilBelumBayar()} — bahan tampilan: daftar kewajiban yang <i>belum</i> terbayar,
 *       dirender sebagai blok merah "Belum membayar" pada grid peserta dan pada respons API
 *       {@code TagihanSiswa}.</li>
 * </ol>
 * <p><b>Celah yang perlu diketahui:</b> {@link #getAktif()} hanya menegakkan mekanisme
 * <i>CSV syarat</i>. Mekanisme alternatif {@code KelasLesSiswa.getJenisBiayaSekolah()} hanya
 * dibaca oleh {@link #ambilBelumBayar()} (tampilan), <b>tidak</b> oleh {@link #ada()} maupun
 * {@link #getAktif()}. Pada kelas les yang memakai mekanisme itu, peserta yang menunggak tetap
 * <b>berstatus aktif</b> walaupun layarnya sendiri menampilkan "Belum membayar" berwarna merah.</p>
 *
 * <h2>Pengelompokan anggota</h2>
 * <ol>
 *   <li><b>Identitas &amp; kunci urut</b> — {@link #getId()}, {@link #keyUrut()},
 *       {@link #getNomorUrut()}.</li>
 *   <li><b>Jejak audit</b> — {@link #getOleh()}, {@link #getOlehId()},
 *       {@link #getTanggal_dirubah()}, {@link #onUpdate()}. Lihat catatan "deklarasi ulang".</li>
 *   <li><b>Relasi inti</b> — {@link #getKelasLesSiswa()} (induk kelas les),
 *       {@link #getSiswa()} (peserta), {@link #getCalonSiswa()} (asal-usul PPDB, opsional).</li>
 *   <li><b>Gerbang pembayaran</b> — {@link #ada()}, {@link #getAktif()},
 *       {@link #ambilBelumBayar()}, {@link #setAktif(Boolean)}.</li>
 *   <li><b>Status kelulusan</b> — {@link #getAcc()}/{@link #setAcc(Boolean)}: gerbang cetak
 *       sertifikat kelas les.</li>
 *   <li><b>Payload nilai per peserta</b> — {@link #getDetailNilai()} (nilai per item penilaian),
 *       {@link #getDetailNilaiTotal()} (nilai agregat per kategori), beserta tujuh method mesin
 *       nilainya: {@link #retreiveDetailNilai(JenisItemPenilaianSiswa, GrupKategoriItemPenilaianSiswa, Matapelajaran, Integer, Boolean)},
 *       {@link #retreiveDetailVerify(JenisItemPenilaianSiswa, GrupKategoriItemPenilaianSiswa, Matapelajaran, Integer)},
 *       {@link #retreiveTotalNilai(List, String, Matapelajaran, GrupPenilaian, GrupKategoriItemPenilaianSiswa, Integer, Boolean)},
 *       {@link #populateDetailNilai(JenisItemPenilaianSiswa, Matapelajaran, GrupKategoriItemPenilaianSiswa, String, Boolean, Integer)},
 *       {@link #retreiveDetailNilaiTotal(GrupKategoriItemPenilaianSiswa, Matapelajaran, Integer)},
 *       {@link #retreiveTotalNilaiTotal(String, Matapelajaran, GrupPenilaian, Integer, List)},
 *       {@link #populateDetailNilaiTotal(Matapelajaran, GrupKategoriItemPenilaianSiswa, Double, Boolean, Integer)}.</li>
 *   <li><b>Catatan bebas</b> — {@link #getKeterangan()}, {@link #getKeterangan1()},
 *       {@link #getKeterangan2()}.</li>
 *   <li><b>Stub kontrak induk &amp; kolom mati</b> — {@link #ambilMk()},
 *       {@link #ambilKelasSiswa()}, {@link #getNoUts()}, {@link #getNoUas()}.</li>
 * </ol>
 *
 * <h2>Catatan penting soal deklarasi ulang {@code id}/{@code oleh}/{@code olehId}/{@code tanggal_dirubah}</h2>
 * <p>Rantai pewarisannya {@code KelasLesSiswaPunyaSiswa} &rarr;
 * {@link ais.database.model.sekolah.VoKelasPunyaSiswa} &rarr;
 * {@link ais.database.model.GeneralValueObject}. {@code GeneralValueObject} <b>bukan</b>
 * {@code @Entity} maupun {@code @MappedSuperclass} — ia POJO abstrak biasa, sehingga Hibernate
 * <b>tidak memetakan satu pun properti induknya</b>. Karena itu deklarasi ulang {@code id},
 * {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} di berkas ini <b>bukan duplikasi yang
 * keliru</b>, melainkan keharusan teknis agar kolom-kolom tersebut benar-benar ada di tabel.
 * Jangan "membersihkannya".</p>
 *
 * <h2>MESIN NILAI DI SINI ADALAH SALINAN LAMA YANG MEMBAYANGI INDUKNYA — paling penting</h2>
 * <p>{@link ais.database.model.sekolah.VoKelasPunyaSiswa} sudah menyediakan implementasi
 * <b>konkret</b> untuk ketujuh method mesin nilai, dan versi induk itu sudah <b>diperkeras</b>
 * dengan dua penjaga: {@code splitNilai()} (memakai
 * {@code StringUtils.splitPreserveAllTokens} plus pemeriksaan panjang minimal 8 ruas) dan
 * {@code nilaiUntukFormula()} (mengubah nilai non-numerik seperti {@code "A:80"} menjadi angka
 * sebelum masuk formula). Berkas ini <b>menimpa ketujuhnya dengan salinan versi lama</b> yang
 * tidak punya kedua penjaga itu, sehingga perbaikan di induk <b>tidak pernah sampai ke kelas
 * les</b>. Tiga akibat yang bisa diamati:</p>
 * <ul>
 *   <li>{@code StringUtils.split(nn, "|")} <b>membuang ruas kosong</b>. Begitu satu nilai
 *       dikosongkan lewat
 *       {@link #populateDetailNilai(JenisItemPenilaianSiswa, Matapelajaran, GrupKategoriItemPenilaianSiswa, String, Boolean, Integer)}
 *       (yang menulis {@code "id|matpel||0|0|false|smt|grup"}), seluruh indeks entri itu bergeser
 *       satu; pembacaan berikutnya salah kolom atau melempar
 *       {@code ArrayIndexOutOfBoundsException} yang langsung ditelan {@code catch} — <b>entri
 *       nilai itu lenyap diam-diam</b> dari rapor kelas les. Pada kelas reguler bug ini sudah
 *       tidak ada.</li>
 *   <li>Nilai bertipe pilihan ({@code "A:80"}) masuk ke {@code GrupPenilaianUtil.hitung()} apa
 *       adanya, bukan lewat {@code nilaiUntukFormula()}; formula total kelas les karenanya bisa
 *       gagal/menghasilkan 0 pada kasus yang sudah tertangani di kelas reguler.</li>
 *   <li>{@link #populateDetailNilai(JenisItemPenilaianSiswa, Matapelajaran, GrupKategoriItemPenilaianSiswa, String, Boolean, Integer)}
 *       kehilangan baris {@code jumlah = jumlah == null ? "" : jumlah;} milik induk, sehingga
 *       {@code jumlah} {@code null} tersimpan sebagai teks literal {@code "null"}.</li>
 * </ul>
 * <p>Bila suatu saat ketujuh override ini dihapus, kelas les otomatis mewarisi versi induk yang
 * sudah diperkeras — tetapi <b>perlu pengujian</b>: versi induk memakai {@link #ambilKelasSiswa()}
 * (yang di sini {@code null}) untuk penjaga verifikasi nilai, sementara override lokal memakai
 * {@link #getKelasLesSiswa()}, sehingga perilaku saklar "Publikasi Nilai Harus Telah Diverifikasi"
 * <b>akan berubah</b>.</p>
 *
 * <h2>Hal-hal non-obvious lain</h2>
 * <ul>
 *   <li><b>{@link #getAktif()} adalah getter DESTRUKTIF berantai.</b> Ia menulis hasil
 *       {@link #ada()} ke field {@code aktif} yang terpetakan; karena entity ini
 *       {@code dynamicUpdate} dan memakai <i>property access</i>, nilai itu ikut ter-{@code flush}
 *       ke kolom {@code aktif} hanya karena baris <b>dibaca</b> (mis. saat grid peserta dirender).
 *       Lebih jauh lagi, ia memanggil {@code KelasLesSiswa.getSyaratPengaturanPembayaran()} yang
 *       <b>juga</b> destruktif: pada kelas les yang memakai {@code jenisBiayaSekolah}, getter induk
 *       itu <b>mengosongkan permanen</b> konfigurasi CSV syarat pembayaran. Jadi sekadar membuka
 *       daftar peserta dapat menghapus konfigurasi finansial kelas les.</li>
 *   <li><b>{@link #getSiswa()} juga menulis balik.</b> Bila {@code calonSiswa} terisi dan calon
 *       siswa itu sudah punya {@code Siswa}, getter <b>menimpa</b> field {@code siswa} dengan
 *       {@code calonSiswa.getSiswa()} — penugasan ulang manual lewat {@link #setSiswa(Siswa)} akan
 *       dikembalikan diam-diam begitu baris tersentuh. Instance keluarga "getter write-back" yang
 *       sudah dikenal, di sini pada FK identitas roster.</li>
 *   <li><b>{@link #getNomorUrut()} tidak pernah {@code null}.</b> {@code compareTo()} di
 *       {@link ais.database.model.GeneralValueObject} memakai {@code getNomorUrut()} sebagai kunci
 *       PERTAMA dan hanya melewatinya bila kedua sisi {@code null}. Karena override di sini
 *       meng-<i>coalesce</i> {@code null} &rarr; {@code 0}, cabang itu <b>selalu</b> dipakai;
 *       pada instalasi yang tidak mengisi nomor urut semua baris jadi "setara", sehingga
 *       {@code TreeSet}/{@code TreeMap} berkunci entity ini akan menciut jadi satu elemen.
 *       <b>Verifikasi berkas ini: bug penciutan TIDAK aktif</b> — penelusuran seluruh repo tidak
 *       menemukan satu pun {@code TreeSet<KelasLesSiswaPunyaSiswa>},
 *       {@code TreeMap} berkunci entity ini, maupun
 *       {@code Collections.sort(List<KelasLesSiswaPunyaSiswa>)}; pengurutan peserta selalu
 *       dilakukan di sisi SQL ({@code addOrder(Order.asc("nomorUrut"))} pada
 *       {@code DetailKelasLesSiswaHelper}/{@code DetailPenilaianLesSiswaHelper}). Risikonya laten,
 *       bukan aktual.</li>
 *   <li><b>{@link #keyUrut()} adalah KODE MATI di berkas ini.</b> Kembarannya
 *       {@link ais.database.model.sekolah.KelasSiswaPunyaSiswa} memakainya sebagai
 *       {@code toString()}; berkas ini <b>tidak</b> meng-override {@code toString()}, dan tidak
 *       ada pemanggil {@code keyUrut()} di mana pun. Efek sampingnya nyata: {@code toString()}
 *       jatuh ke {@link ais.database.model.GeneralValueObject#toString()} yang memakai
 *       {@code kode}/{@code nama} — dua properti induk yang <b>tidak dipetakan</b> dan tidak
 *       pernah diisi — sehingga baris roster kelas les selalu tampil sebagai teks
 *       {@code "null"} di komponen apa pun yang mengandalkan {@code toString()}.</li>
 *   <li><b>{@link #getNoUts()}/{@link #getNoUas()} adalah kolom mati.</b> Tidak ada satu pun
 *       pemanggil {@code setNoUts()}/{@code setNoUas()} di repo; keduanya sisa salinan dari
 *       padanan modul perguruan tinggi yang logika pembangkit nomor pesertanya dibuang.</li>
 *   <li><b>{@link #ambilKelasSiswa()} dan {@link #ambilMk()} adalah stub.</b> Keduanya wajib ada
 *       karena kontrak {@link ais.database.model.sekolah.VoKelasPunyaSiswa}, tetapi
 *       mengembalikan {@code null} dan daftar kosong. Dampaknya diuraikan pada masing-masing
 *       method — yang terpenting, saklar "Publikasi Nilai Harus Telah Diverifikasi" milik induk
 *       tidak dapat dievaluasi lewat jalur itu.</li>
 *   <li><b>{@link #getKeterangan1()}/{@link #getKeterangan2()} mengembalikan {@code "{}"} bila
 *       kosong</b>, mengikuti konstanta {@link #D}; nilai pengganti itu <b>tidak</b> ditulis balik
 *       ke field, jadi kolomnya tetap {@code NULL} di basis data.</li>
 * </ul>
 *
 * <h2>Catatan hak akses (hasil audit menyertai dokumentasi ini) — VERIFIKASI ULANG</h2>
 * <p>Layar pengelola roster ini, {@code DetailKelasLesSiswaHelper}, diperiksa ulang dari sisi
 * entity ini dan <b>mengulang persis pola {@code KelasSiswaPunyaSiswa}</b>: hanya sebagian kontrol
 * yang digerbangi. Yang bergerbang: tombol "Ambil Siswa" ({@code CREATE}) dan tombol "Hapus" per
 * baris ({@code DELETE}). Yang <b>NOL GERBANG</b> — cukup hak BACA menu kelas les:</p>
 * <ul>
 *   <li><b>"Bersihkan"</b> — menghapus SELURUH peserta kelas les satu per satu
 *       ({@code session.delete} + {@code flush} dalam loop);</li>
 *   <li><b>"Copy siswa dari kelas lain"</b> — menyisipkan massal peserta dari kelas les lain.
 *       Combo sumbernya sendiri <b>fail-open tenant</b>: bila
 *       {@code Common.getCurrentUser().ambilSekolah()} mengembalikan {@code null}, pembatas
 *       sekolah diganti {@code Restrictions.sqlRestriction("true")} sehingga daftar kelas les
 *       <b>SELURUH instalasi</b> muncul sebagai sumber salinan — instance baru dari pola
 *       fail-open cakupan tenant yang berulang sepanjang audit ini;</li>
 *   <li><b>unggah Excel massal</b> penempatan siswa ke kelas les;</li>
 *   <li><b>checkbox "Aktif"</b> dan <b>checkbox "Lulus"</b> per baris — keduanya langsung
 *       {@code refreshSaveOrUpdate} saat dicentang. Ini yang paling berdampak: mencentang "Lulus"
 *       menyetel {@link #setAcc(Boolean)} sehingga tombol cetak <b>sertifikat kelas les</b>
 *       muncul (dan tombol Hapus justru disembunyikan), padahal pencentangnya sama sekali tidak
 *       diperiksa haknya;</li>
 *   <li><b>{@code Intbox} nomor urut</b> per baris — {@code refreshUpdate} pada setiap perubahan;</li>
 *   <li><b>tombol cetak/ekspor</b> daftar peserta beserta NIS/NISN/nama/sekolah.</li>
 * </ul>
 * <p>Layar penilaian kelas les, {@code DetailPenilaianLesSiswaHelper} (2.782 baris) — yang menulis
 * {@link #getDetailNilai()}/{@link #getDetailNilaiTotal()} entity ini pada setiap sel yang diedit
 * dan lewat unggah Excel massal — <b>tidak punya gerbang sama sekali</b>: kedua baris
 * {@code CommonPrivilages.checkPrevilages(...)} pada constructor-nya <b>dikomentari</b>.
 * Sebaliknya, layar MASTER {@code KelasLesSiswaAction} bergerbang benar
 * ({@code CREATE}/{@code UPDATE}/{@code DELETE}) — sekali lagi menguatkan pola berulang audit ini:
 * kerusakan terkonsentrasi pada panel/helper DETAIL, bukan pada layar master. Baris roster ini
 * mewarisi hak dari menu induknya (kelas les), jadi pengguna yang hanya diberi hak baca kelas les
 * tetap dapat mengubah nilai, status kelulusan, dan mengosongkan roster.</p>
 * <p><b>Jalur API mandiri:</b> {@code TagihanSiswa.subscribeKelasLes} membolehkan pemilik token
 * siswa mendaftarkan dirinya sendiri ke kelas les. Sisi <i>unsubscribe</i>-nya aman terhadap
 * peserta lain (kunci {@code siswa_id} diambil dari token), namun ia memakai
 * {@code createSQLQuery("delete from sekolah.kelas_les_punya_siswa ...")} sehingga
 * penghapusan <b>melewati Envers</b> — tidak ada jejak {@code @Audited} untuk baris yang hilang.
 * Sisi <i>subscribe</i>-nya lebih serius: {@code kelasLes} diresolusi langsung dari id yang
 * dikirim klien lewat {@code ConstantValues.ambil()} <b>tanpa memeriksa</b> bahwa kelas les itu
 * milik sekolah siswa tersebut, lalu barisnya disimpan dengan {@code aktif = true} — satu siswa
 * dapat mendaftarkan diri ke kelas les <b>sekolah/yayasan mana pun di instalasi yang sama</b> dan
 * langsung berstatus aktif, melewati gerbang pembayaran.</p>
 *
 * @see ais.database.model.sekolah.KelasLesSiswa
 * @see ais.database.model.sekolah.VoKelasPunyaSiswa
 * @see ais.database.model.sekolah.KelasSiswaPunyaSiswa
 * @see ais.database.model.sekolah.Siswa
 * @see ais.database.model.sekolah.CalonSiswa
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(name = "kelas_les_punya_siswa", schema = "sekolah")
public class KelasLesSiswaPunyaSiswa extends VoKelasPunyaSiswa {

	/**
	 *
	 */
	private static final long serialVersionUID = -9157912161411433979L;
	/**
	 * Primary key {@code sekolah.kelas_les_punya_siswa.id}. Dideklarasikan ulang di sini karena
	 * {@link ais.database.model.GeneralValueObject} bukan {@code @MappedSuperclass}.
	 */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris; diisi {@code AuditTimestampInterceptor}. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris; diisi {@code AuditTimestampInterceptor}. */
	private String olehId;

	/**
	 * Membangun kunci pengurutan/identitas yang stabil untuk satu baris roster kelas les,
	 * berbentuk <code>&lt;nomorUrut ter-pad&gt;_&lt;nomorInduk siswa&gt;_&lt;id&gt;</code>.
	 *
	 * <p>Nomor urut di-<i>pad</i> dengan menempelkan dua belas karakter {@code '0'} di depan lalu
	 * memangkas lima karakter pertama, sehingga untuk nomor urut satu digit hasilnya delapan digit
	 * ({@code "00000012"}) dan urutan leksikografisnya sama dengan urutan numerik. Karena
	 * {@link #getNomorUrut()} tidak pernah {@code null}, method ini tidak pernah menghasilkan teks
	 * {@code "null"} pada bagian nomor urut dan tidak pernah melempar
	 * {@code StringIndexOutOfBoundsException}.</p>
	 *
	 * <p><b>KODE MATI.</b> Berbeda dari kembarannya di
	 * {@link ais.database.model.sekolah.KelasSiswaPunyaSiswa} — yang memakai method identik ini
	 * sebagai {@code toString()} — berkas ini <b>tidak</b> meng-override {@code toString()} dan
	 * tidak ada satu pun pemanggil {@code keyUrut()} di seluruh repo. Method ini dipertahankan
	 * demi kesejajaran dengan kembarannya; bila suatu saat dipakai, kombinasi nomor
	 * urut + nomor induk + {@code id} membuat kuncinya tetap <b>unik</b> walaupun seluruh baris
	 * ber-nomor urut kosong, sehingga kebal terhadap pola penciutan {@code TreeSet} akibat
	 * {@link #getNomorUrut()} yang non-null.</p>
	 *
	 * <p><b>Peringatan:</b> memanggil {@link #getSiswa()} berarti method ini ikut memicu penulisan
	 * balik field {@code siswa} (lihat {@link #getSiswa()}), dan akan melempar
	 * {@code NullPointerException} bila baris berada dalam keadaan tak wajar tanpa siswa (kolom
	 * {@code siswa_id} sendiri {@code nullable = false}).</p>
	 *
	 * @return kunci urut unik untuk baris roster kelas les ini
	 */
	public String keyUrut() {
		String urut = "000000000000" + getNomorUrut();
		urut = urut.substring(5);

		return urut + "_" + getSiswa().getNomorInduk() + "_" + getId();
	}

	/** @return id pengguna terakhir yang mengubah baris ini, atau {@code null} bila belum pernah terisi. */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah. Nilai {@code null} atau berisi spasi saja <b>diabaikan</b>
	 * (nilai lama dipertahankan) agar jejak audit tidak terhapus oleh pemanggil yang lalai —
	 * misalnya proses batch/penjadwal yang berjalan tanpa sesi login.
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah, dengan aturan pengabaian yang sama seperti
	 * {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/** @return nama pengguna terakhir yang mengubah baris ini, atau {@code null} bila belum pernah terisi. */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback lifecycle JPA yang dijalankan tepat sebelum {@code UPDATE} baris ini, mendelegasikan
	 * ke {@code AuditTimestampInterceptor.ubah(this)} untuk mengisi {@link #getOleh()},
	 * {@link #getOlehId()}, dan {@link #getTanggal_dirubah()} dari sesi pengguna yang sedang aktif.
	 *
	 * <p><b>Efek samping:</b> mengubah tiga properti audit di atas. Tidak dipanggil pada
	 * {@code INSERT} — hanya {@code @PreUpdate}.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu perubahan terakhir. Diinisialisasi ke waktu sekarang saat object dibuat
	 * sehingga baris baru selalu punya nilai, lalu diperbarui {@link #onUpdate()}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Tanpa validasi — nilai {@code null} diterima.
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return stempel waktu perubahan terakhir baris ini (presisi {@code TIMESTAMP}). */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Induk kelas les tempat baris roster ini bernaung; FK {@code kelas_id}, wajib terisi. */
	private KelasLesSiswa kelasLesSiswa;
	/** Peserta berupa siswa aktif; FK {@code siswa_id}, wajib terisi pada seluruh jalur pembuatan. */
	private Siswa siswa;
	/** Asal-usul PPDB peserta; FK {@code calon_siswa}, opsional. */
	private CalonSiswa calonSiswa;
	/** Catatan bebas per baris roster; terpetakan tetapi tidak pernah ditulis pemanggil mana pun. */
	private String keterangan;
	/** Catatan/JSON semester 1 per peserta; lihat {@link #getKeterangan1()}. */
	private String keterangan1;
	/** Catatan/JSON semester 2 per peserta; lihat {@link #getKeterangan2()}. */
	private String keterangan2;
	/** Status keikutsertaan aktif; DITIMPA {@link #getAktif()} berdasarkan status pembayaran. */
	private Boolean aktif;

	/** Nomor peserta UTS — kolom mati, tidak ada pemanggil {@link #setNoUts(String)}. */
	private String noUts;
	/** Nomor peserta UAS — kolom mati, tidak ada pemanggil {@link #setNoUas(String)}. */
	private String noUas;

	/** Nomor urut tampil peserta di dalam kelas les; di-coalesce ke {@code 0} oleh getternya. */
	private Integer nomorUrut;

	/** Constructor default tanpa argumen. WAJIB ada untuk hidrasi entity oleh Hibernate. */
	public KelasLesSiswaPunyaSiswa() {
	}

	/**
	 * @return primary key baris roster ini, atau {@code null} bila belum tersimpan. Kolom
	 *         {@code insertable = false} karena nilainya dibangkitkan basis data
	 *         ({@code IDENTITY}).
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key. Dipakai Hibernate saat hidrasi dan oleh pembuat object "penunjuk".
	 *
	 * @param id nilai primary key
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return catatan bebas baris roster, atau {@code null}. Praktis selalu {@code null}: tidak
	 *         ada pemanggil {@link #setKeterangan(String)} di repo.
	 */
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menyetel catatan bebas baris roster.
	 *
	 * @param keterangan teks catatan
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Memeriksa apakah peserta ini <b>sudah memenuhi seluruh syarat pembayaran</b> kelas les yang
	 * diikutinya — inti gerbang pembayaran kelas les.
	 *
	 * <p><b>Algoritma (terverifikasi dari kode):</b></p>
	 * <ol>
	 *   <li>Bila baris tidak punya induk kelas les, atau kelas lesnya tidak punya CSV
	 *       {@code syaratPengaturanPembayaran}, hasilnya langsung {@code true} (tidak ada
	 *       syarat &rarr; dianggap memenuhi).</li>
	 *   <li>Riwayat pembayaran peserta digabung dari <b>kedua</b> sisi:
	 *       {@code calonSiswa.getRiwayatPengaturanPembayaran()} dan
	 *       {@code siswa.getRiwayatPengaturanPembayaran()}, dipisah koma. Keduanya sudah
	 *       ternormalkan berbungkus koma di kedua ujung, sehingga pencocokan
	 *       {@code contains("," + syarat + ",")} tidak keliru mencocokkan {@code "12"} dengan
	 *       {@code "112"}.</li>
	 *   <li>Setiap entri CSV syarat diperiksa. Entri berperiode bulanan berbentuk
	 *       {@code id_bulan_tahun}; bila periodenya <b>belum jatuh tempo</b> (hasil
	 *       {@code PembayaranSiswa.convert(tahun, bulan)} lebih besar dari
	 *       {@code PembayaranSiswa.sekarang()}), entri itu <b>dilewati</b> — tunggakan masa depan
	 *       tidak menonaktifkan peserta.</li>
	 *   <li>Entri pertama yang belum tercatat di riwayat langsung membuat hasil {@code false} dan
	 *       menghentikan iterasi.</li>
	 * </ol>
	 *
	 * <p><b>Efek samping:</b> memanggil {@link #getKelasLesSiswa()} (resolusi proxy + penulisan
	 * balik) dan {@code KelasLesSiswa.getSyaratPengaturanPembayaran()} yang <b>destruktif</b> —
	 * getter induk itu mengosongkan permanen konfigurasi CSV bila kelas les memakai
	 * {@code jenisBiayaSekolah}. Juga menyentuh {@link #getSiswa()}/{@link #getCalonSiswa()},
	 * sehingga {@link #getSiswa()} ikut menulis balik FK-nya.</p>
	 *
	 * <p><b>Batasan yang perlu diketahui:</b> mekanisme syarat berbasis
	 * {@code KelasLesSiswa.getJenisBiayaSekolah()} <b>tidak</b> diperiksa di sini sama sekali —
	 * hanya {@link #ambilBelumBayar()} yang membacanya. Pada kelas les bermekanisme itu, method ini
	 * selalu {@code true}. Selain itu {@code Integer.parseInt} atas bulan/tahun tidak dibungkus
	 * {@code try}: entri CSV yang cacat akan melempar {@code NumberFormatException} keluar dari
	 * method ini.</p>
	 *
	 * <p><b>Dipanggil dari:</b> {@link #getAktif()} (satu-satunya pemanggil), yang menjalankannya
	 * setiap kali status aktif baris dibaca — termasuk saat grid peserta
	 * {@code DetailKelasLesSiswaHelper} dirender dan saat API {@code TagihanSiswa} menyusun
	 * respons {@code sudah_diterima}.</p>
	 *
	 * @return {@code true} bila seluruh syarat pembayaran yang sudah jatuh tempo terpenuhi
	 */
	public Boolean ada() {
		Boolean ada = true;
		if (getKelasLesSiswa() != null) {
			if (!kelasLesSiswa.getSyaratPengaturanPembayaran().isEmpty()) {

				Integer pembayaranSekarang = PembayaranSiswa.sekarang();

				String c1 = getCalonSiswa() == null ? "" : getCalonSiswa().getRiwayatPengaturanPembayaran();
				String c2 = getSiswa() == null ? "" : getSiswa().getRiwayatPengaturanPembayaran();
				String c = c1 + "," + c2;
				for (String s : kelasLesSiswa.getSyaratPengaturanPembayaran().split(",")) {

					String[] aa = StringUtils.split(s, "_");
					String bulan = aa.length > 1 ? aa[1] : "";
					String tahun = aa.length > 2 ? aa[2] : "";

					if (!bulan.isEmpty() && !tahun.isEmpty()) {
						Integer kapan = PembayaranSiswa.convert(Integer.parseInt(tahun), Integer.parseInt(bulan));
						if (kapan > pembayaranSekarang) {
							continue;
						}
					}

					if (!s.trim().isEmpty() && !StringUtils.contains(c, "," + s.trim() + ",")) {
						ada = false;
						break;
					}
				}
			}
		}
		return ada;
	}

	/**
	 * Mengembalikan status keikutsertaan aktif peserta pada kelas les ini — <b>bukan sekadar
	 * pembaca kolom</b>.
	 *
	 * <p>Bila kelas lesnya memiliki CSV syarat pembayaran, method ini <b>menimpa field
	 * {@code aktif}</b> dengan hasil {@link #ada()}. Karena entity ini memakai <i>property
	 * access</i> dan {@code dynamicUpdate}, nilai hasil timpa itu ikut ter-{@code flush} ke kolom
	 * {@code aktif} begitu session ditutup — <b>hanya karena baris dibaca</b>. Setelan manual
	 * checkbox "Aktif" karenanya tidak pernah bertahan pada kelas les berbayar, dan layar detail
	 * memang sengaja men-{@code disable} checkbox itu untuk kasus tersebut.</p>
	 *
	 * <p><b>Getter destruktif berantai.</b> Selain menulis {@code aktif}, jalur ini memanggil
	 * {@code KelasLesSiswa.getSyaratPengaturanPembayaran()} yang <b>mengosongkan permanen</b>
	 * konfigurasi CSV syarat bila kelas les memakai {@code jenisBiayaSekolah}. Merender daftar
	 * peserta dapat menghapus konfigurasi finansial kelas lesnya.</p>
	 *
	 * <p><b>Fail-open pada kegagalan.</b> Seluruh badan method dibungkus {@code try/catch}
	 * (mis. {@code kelasLesSiswa} {@code null} &rarr; {@code NullPointerException}, atau entri CSV
	 * cacat &rarr; {@code NumberFormatException} dari {@link #ada()}); exception ditelan dan
	 * dicatat {@code ErrorAuditUtil}, lalu nilai kolom apa adanya yang dikembalikan. Bila kolom itu
	 * pun {@code null}, hasilnya <b>{@code true}</b> — peserta dianggap aktif. Jadi kegagalan
	 * evaluasi syarat <b>melonggarkan</b> gerbang, bukan menutupnya.</p>
	 *
	 * <p><b>Dipanggil dari:</b> renderer baris {@code DetailKelasLesSiswaHelper} (mengisi checkbox
	 * "Aktif"), {@code TagihanSiswa} (field {@code sudah_diterima} pada API kelas les siswa), dan
	 * kontrak abstrak {@link ais.database.model.sekolah.VoKelasPunyaSiswa#getAktif()}.</p>
	 *
	 * @return {@code true} bila peserta masih aktif; tidak pernah {@code null}
	 */
	public Boolean getAktif() {
		try {
			kelasLesSiswa = getKelasLesSiswa();
			if (!kelasLesSiswa.getSyaratPengaturanPembayaran().isEmpty()) {
				aktif = ada();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/KelasLesSiswaPunyaSiswa.java:168");
			// TODO: handle exception
		}
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyusun daftar kewajiban pembayaran kelas les yang <b>belum</b> dilunasi peserta ini —
	 * bahan tampilan blok merah "Belum membayar".
	 *
	 * <p>Berbeda dari {@link #ada()}, method ini mengevaluasi <b>kedua</b> mekanisme syarat, dengan
	 * urutan prioritas yang saling meniadakan:</p>
	 * <ol>
	 *   <li>Bila {@code KelasLesSiswa.getJenisBiayaSekolah()} terisi, hanya jenis biaya itu yang
	 *       diperiksa terhadap {@code getRiwayatJenisPembayaran()} gabungan calon siswa + siswa.
	 *       Hasilnya satu entri {@code Object[]{ JenisBiayaSekolah, null, null }}.</li>
	 *   <li>Bila tidak, CSV {@code getSyaratPengaturanPembayaran()} diperiksa terhadap
	 *       {@code getRiwayatPengaturanPembayaran()} gabungan. Setiap entri yang belum terbayar
	 *       diresolusi menjadi {@link PengaturanBiaya} lewat cache {@code ConstantValues.ambil()};
	 *       hanya pengaturan biaya yang <b>aktif</b> yang dimasukkan, sebagai
	 *       {@code Object[]{ PengaturanBiaya, bulan, tahun }}. Entri bulanan yang belum jatuh tempo
	 *       dilewati, sama seperti {@link #ada()}.</li>
	 * </ol>
	 *
	 * <p><b>Bentuk kembalian sengaja heterogen:</b> elemen {@code [0]} bisa
	 * {@link JenisBiayaSekolah} <i>atau</i> {@link PengaturanBiaya}, sehingga setiap pembaca wajib
	 * memakai {@code instanceof} — pola yang dipakai {@code DetailKelasLesSiswaHelper} dan
	 * {@code TagihanSiswa}. Elemen {@code [1]}/{@code [2]} berisi bulan/tahun sebagai
	 * {@code String} (bisa kosong) untuk biaya berperiode bulanan.</p>
	 *
	 * <p><b>Efek samping &amp; peringatan:</b> memanggil {@link #getKelasLesSiswa()},
	 * {@link #getSiswa()} (penulisan balik FK), dan {@code getSyaratPengaturanPembayaran()} yang
	 * destruktif. Tidak ada {@code try/catch} sama sekali di dalam method ini: entri CSV yang cacat
	 * membuat {@code Long.parseLong}/{@code Integer.parseInt} melempar
	 * {@code NumberFormatException} yang merambat keluar dan menggagalkan render satu baris grid.
	 * {@code ConstantValues.ambil()} adalah cache global tanpa pemisahan tenant, tetapi karena
	 * pencariannya berbasis id primer hal itu tidak memperluas cakupan data.</p>
	 *
	 * <p><b>Dipanggil dari:</b> renderer baris {@code DetailKelasLesSiswaHelper} (kolom "Syarat
	 * Pembayaran") dan {@code TagihanSiswa} (field {@code infoBayar} pada API kelas les siswa).</p>
	 *
	 * @return daftar kewajiban belum terbayar; kosong bila peserta sudah memenuhi seluruh syarat
	 *         atau kelas lesnya tidak menetapkan syarat apa pun. Tidak pernah {@code null}.
	 */
	public List<Object[]> ambilBelumBayar() {
		List<Object[]> pengaturanBiayas = new ArrayList<Object[]>();
		if (getKelasLesSiswa() != null) {
			Integer pembayaranSekarang = PembayaranSiswa.sekarang();
			if (kelasLesSiswa.getJenisBiayaSekolah() != null) {

				String s = kelasLesSiswa.getJenisBiayaSekolah().getId().toString();
				String c1 = getCalonSiswa() == null ? "" : getCalonSiswa().getRiwayatJenisPembayaran();
				String c2 = getSiswa() == null ? "" : getSiswa().getRiwayatJenisPembayaran();
				String c = c1 + "," + c2;

				if (!s.trim().isEmpty() && !StringUtils.contains(c, "," + s.trim() + ",")) {
					pengaturanBiayas.add(new Object[] { kelasLesSiswa.getJenisBiayaSekolah(), null, null });
				}
			}

			else if (!kelasLesSiswa.getSyaratPengaturanPembayaran().isEmpty()) {

				String c1 = getCalonSiswa() == null ? "" : getCalonSiswa().getRiwayatPengaturanPembayaran();
				String c2 = getSiswa() == null ? "" : getSiswa().getRiwayatPengaturanPembayaran();
				String c = c1 + "," + c2;
				for (String s : kelasLesSiswa.getSyaratPengaturanPembayaran().split(",")) {
					if (!s.trim().isEmpty() && !StringUtils.contains(c, "," + s.trim() + ",")) {

						String[] aa = StringUtils.split(s, "_");
						String id = aa[0];
						String bulan = aa.length > 1 ? aa[1] : "";
						String tahun = aa.length > 2 ? aa[2] : "";

						if (!bulan.isEmpty() && !tahun.isEmpty()) {
							Integer kapan = PembayaranSiswa.convert(Integer.parseInt(tahun), Integer.parseInt(bulan));
							if (kapan > pembayaranSekarang) {
								continue;
							}
						}

						PengaturanBiaya pengaturanBiaya = (PengaturanBiaya) ConstantValues
								.ambil(PengaturanBiaya.class.getName(), Long.parseLong(id));
						if (pengaturanBiaya != null && pengaturanBiaya.getAktif()) {
							pengaturanBiayas.add(new Object[] { pengaturanBiaya, bulan, tahun });
						}
					}
				}

			}
		}
		return pengaturanBiayas;
	}

	/**
	 * Menyetel status keikutsertaan aktif secara manual.
	 *
	 * <p><b>Perhatian:</b> nilai yang disetel di sini hanya bertahan pada kelas les yang
	 * <b>tidak</b> memakai CSV syarat pembayaran. Pada kelas les berbayar,
	 * {@link #getAktif()} akan menimpanya kembali dengan hasil {@link #ada()} pada pembacaan
	 * berikutnya.</p>
	 *
	 * <p>Pemanggilnya: checkbox "Aktif" pada {@code DetailKelasLesSiswaHelper} (tanpa gerbang hak
	 * — lihat catatan hak akses di dokumentasi kelas), {@code SiswaAction} (mendaftarkan siswa ke
	 * kelas les pilihannya dengan {@code false}), serta jalur PPDB/API
	 * ({@code CalonSiswa.populatePembayaran}, {@code TagihanSiswa.subscribeKelasLes}) yang
	 * menyetel {@code true}.</p>
	 *
	 * @param aktif status aktif baru; {@code null} berarti "belum ditentukan" dan dibaca sebagai
	 *              {@code true} oleh {@link #getAktif()}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan kelas les induk tempat baris roster ini bernaung.
	 *
	 * <p>Relasi {@code @ManyToOne} {@code LAZY} dengan {@code cascade} {@code PERSIST}+{@code MERGE}
	 * (tanpa {@code REMOVE}), FK {@code kelas_id} yang {@code nullable = false}. Proxy lazy
	 * diresolusi lewat {@code check()} milik {@link ais.database.model.GeneralValueObject} dan
	 * hasilnya ditulis balik ke field — penulisan balik yang benign (hanya mengganti proxy dengan
	 * object nyata), berbeda dari penulisan balik destruktif pada {@link #getSiswa()}.</p>
	 *
	 * <p><b>Ini satu-satunya jalur menuju tenant.</b> Entity ini tidak punya kolom
	 * {@code sekolah}/{@code yayasan} sendiri; seluruh pembatasan tenant harus dilakukan tidak
	 * langsung lewat {@code kelasLesSiswa.sekolah}. Perhatikan bahwa
	 * {@code KelasLesSiswa.getSekolah()} sendiri boleh {@code null} (kelas les "global"), dan
	 * beberapa pemanggil menambahkan {@code Restrictions.isNull("sekolah")} sebagai alternatif —
	 * jalur inilah yang membuat pola fail-open cakupan tenant bisa muncul di layar kelas les.</p>
	 *
	 * @return kelas les induk; secara skema tidak boleh {@code null}, tetapi bisa {@code null} pada
	 *         object yang belum lengkap terisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelas_id", nullable = false)
	public KelasLesSiswa getKelasLesSiswa() {
		kelasLesSiswa = check(kelasLesSiswa);
		return kelasLesSiswa;
	}

	/**
	 * Menyetel kelas les induk baris roster ini.
	 *
	 * @param kelasLesSiswa kelas les induk; wajib terisi sebelum baris disimpan
	 */
	public void setKelasLesSiswa(KelasLesSiswa kelasLesSiswa) {
		this.kelasLesSiswa = kelasLesSiswa;
	}

	/**
	 * Mengembalikan siswa peserta kelas les ini.
	 *
	 * <p><b>PERINGATAN — getter DESTRUKTIF.</b> Selain meresolusi proxy lazy lewat {@code check()},
	 * getter ini <b>menimpa field {@code siswa}</b> dengan {@code calonSiswa.getSiswa()} apabila
	 * {@link #getCalonSiswa()} terisi dan calon siswa itu sudah punya {@code Siswa}. Karena
	 * Hibernate memakai <i>property access</i> pada entity {@code dynamicUpdate}, nilai hasil timpa
	 * ikut ter-{@code flush} ke kolom {@code siswa_id}. Efeknya: setiap penugasan ulang manual
	 * lewat {@link #setSiswa(Siswa)} pada baris yang punya {@code calonSiswa} akan dikembalikan
	 * diam-diam begitu baris tersentuh. Perilaku ini identik dengan kembarannya
	 * {@link ais.database.model.sekolah.KelasSiswaPunyaSiswa#getSiswa()}.</p>
	 *
	 * <p>Sisi baiknya, mekanisme inilah yang membuat baris roster peninggalan PPDB otomatis
	 * "menyusul" ke {@code Siswa} resmi begitu calon siswa dikonversi, tanpa migrasi data.</p>
	 *
	 * @return siswa peserta; secara skema ({@code siswa_id nullable = false}) tidak boleh
	 *         {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa_id", nullable = false)
	public Siswa getSiswa() {
		siswa = check(siswa);
		if (getCalonSiswa() != null && getCalonSiswa().getSiswa() != null) {
			siswa = getCalonSiswa().getSiswa();
		}
		return siswa;
	}

	/**
	 * Menyetel siswa peserta kelas les.
	 *
	 * <p><b>Perhatian:</b> pada baris yang punya {@code calonSiswa} ber-{@code Siswa}, nilai yang
	 * disetel di sini akan ditimpa kembali oleh {@link #getSiswa()} — lihat peringatan pada getter
	 * tersebut.</p>
	 *
	 * @param siswa siswa peserta
	 */
	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
	}

	/**
	 * Payload nilai per item penilaian untuk peserta ini, dalam satu kolom {@code text}.
	 *
	 * <p><b>Format:</b> entri dipisah titik koma ({@code ;}), tiap entri berisi delapan ruas dipisah
	 * pipa ({@code |}):
	 * {@code jenisItemId|matapelajaranId|nilai|0|0|verified|semester|grupKategoriId}. Ruas ke-4 dan
	 * ke-5 selalu {@code "0"} (cadangan yang tidak pernah dipakai).</p>
	 *
	 * <p>Ditulis {@link #populateDetailNilai(JenisItemPenilaianSiswa, Matapelajaran, GrupKategoriItemPenilaianSiswa, String, Boolean, Integer)}
	 * dan dibaca ketiga method {@code retreive*} di berkas ini. Lihat catatan
	 * "mesin nilai adalah salinan lama" pada dokumentasi kelas: ruas {@code nilai} yang kosong
	 * membuat entri tidak dapat dibaca kembali dengan benar di kelas les.</p>
	 *
	 * @return payload nilai mentah; string kosong bila belum ada nilai (diinisialisasi {@code ""})
	 */
	@Column(columnDefinition = "text", name = "detail_nilai")
	public String getDetailNilai() {
		return detailNilai;
	}

	/**
	 * Menyetel payload nilai per item penilaian secara utuh. Umumnya <b>tidak</b> dipanggil
	 * langsung — pakai
	 * {@link #populateDetailNilai(JenisItemPenilaianSiswa, Matapelajaran, GrupKategoriItemPenilaianSiswa, String, Boolean, Integer)}
	 * agar entri lain tidak ikut terhapus.
	 *
	 * @param detailNilai payload nilai mentah dalam format yang dijelaskan di {@link #getDetailNilai()}
	 */
	public void setDetailNilai(String detailNilai) {
		this.detailNilai = detailNilai;
	}

	/** Payload nilai per item penilaian; lihat {@link #getDetailNilai()}. */
	private String detailNilai = "";

	/**
	 * Membaca satu nilai dari payload {@link #getDetailNilai()} untuk kombinasi
	 * item penilaian + kategori + mata pelajaran + semester tertentu.
	 *
	 * <p><b>Menimpa</b> implementasi {@link ais.database.model.sekolah.VoKelasPunyaSiswa} — lihat
	 * peringatan "salinan lama" pada dokumentasi kelas: versi ini memakai
	 * {@code StringUtils.split(nn, "|")} yang membuang ruas kosong (bukan
	 * {@code splitPreserveAllTokens} + pemeriksaan panjang milik induk), sehingga entri yang
	 * nilainya pernah dikosongkan akan salah indeks atau dilewati diam-diam.</p>
	 *
	 * <p><b>Perlakuan {@code hanyaValid}:</b> bila kelas les induk <b>tidak</b> mensyaratkan
	 * verifikasi ({@code getPublikasiNilaiHarusTelahDiverifikasi()} bernilai {@code false}),
	 * parameter ini dipaksa {@code null} sehingga filter validitas dimatikan dan nilai yang belum
	 * diverifikasi ikut tampil. Perhatikan bahwa penjaga versi induk memakai
	 * {@link #ambilKelasSiswa()} (yang di sini selalu {@code null}) sehingga tidak pernah menyala —
	 * override lokal inilah yang membuat saklar tersebut benar-benar berfungsi untuk kelas les.</p>
	 *
	 * <p>Setiap entri yang gagal di-parse ditelan {@code catch} dan dicatat
	 * {@code ErrorAuditUtil}, lalu iterasi berlanjut ke entri berikutnya.</p>
	 *
	 * @param jenisItemPenilaianSiswa      item penilaian yang dicari; {@code null} atau tanpa id
	 *                                     langsung menghasilkan string kosong
	 * @param grupKategoriItemPenilaianSiswa kategori/grup penilaian yang harus cocok
	 * @param matapelajaran                mata pelajaran yang harus cocok
	 * @param smt                          semester yang harus cocok
	 * @param hanyaValid                   {@code true}/{@code false} menyaring berdasarkan status
	 *                                     verifikasi; {@code null} berarti tanpa filter
	 * @return nilai sebagai teks apa adanya, atau string kosong bila tidak ditemukan
	 */
	public String retreiveDetailNilai(JenisItemPenilaianSiswa jenisItemPenilaianSiswa,
			GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa, Matapelajaran matapelajaran, Integer smt,
			Boolean hanyaValid) {

		if (getKelasLesSiswa() != null && !getKelasLesSiswa().getPublikasiNilaiHarusTelahDiverifikasi()) {
			hanyaValid = null;
		}

		if (jenisItemPenilaianSiswa != null && jenisItemPenilaianSiswa.getId() != null) {
			String[] nilais = detailNilai == null ? new String[] {} : detailNilai.split(";");
			for (String nn : nilais) {
				try {
					String[] s = StringUtils.split(nn, "|");
					Long formatId = Long.parseLong(s[0]);
					Long matpelId = Long.parseLong(s[1]);
					Integer smtId = Integer.parseInt(s[6]);
					Boolean valid = Boolean.parseBoolean(s[5]);
					Long grupId = Long.parseLong(s[7]);
					if ((hanyaValid == null || hanyaValid.equals(valid))
							&& jenisItemPenilaianSiswa.getId().equals(formatId)
							&& matapelajaran.getId().equals(matpelId) && smtId.equals(smt)
							&& grupId.equals(grupKategoriItemPenilaianSiswa.getId())) {
						return s[2];
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/KelasLesSiswaPunyaSiswa.java:287");

				}
			}
		}

		return "";
	}

	/**
	 * Membaca status <b>verifikasi</b> (ruas ke-6 payload) untuk satu kombinasi item penilaian +
	 * kategori + mata pelajaran + semester.
	 *
	 * <p><b>Menimpa</b> implementasi induk dengan salinan lama tanpa penjaga {@code splitNilai()};
	 * lihat dokumentasi kelas. Berbeda dari
	 * {@link #retreiveDetailNilai(JenisItemPenilaianSiswa, GrupKategoriItemPenilaianSiswa, Matapelajaran, Integer, Boolean)},
	 * method ini <b>tidak</b> membaca saklar {@code publikasiNilaiHarusTelahDiverifikasi}: ia murni
	 * pelapor status.</p>
	 *
	 * <p>Dipakai layar penilaian kelas les untuk menampilkan penanda "sudah diverifikasi", dan
	 * dipanggil ulang dari
	 * {@link #retreiveTotalNilai(List, String, Matapelajaran, GrupPenilaian, GrupKategoriItemPenilaianSiswa, Integer, Boolean)}
	 * untuk mengisi variabel formula bersufiks {@code _s}.</p>
	 *
	 * @param jenisItemPenilaianSiswa      item penilaian yang dicari; {@code null}/tanpa id
	 *                                     menghasilkan {@code false}
	 * @param grupKategoriItemPenilaianSiswa kategori/grup penilaian yang harus cocok
	 * @param matapelajaran                mata pelajaran yang harus cocok
	 * @param smt                          semester yang harus cocok
	 * @return {@code true} bila entri ditemukan dan bertanda terverifikasi; {@code false} bila
	 *         tidak ditemukan atau belum diverifikasi (kedua kondisi tidak dapat dibedakan)
	 */
	public Boolean retreiveDetailVerify(JenisItemPenilaianSiswa jenisItemPenilaianSiswa,
			GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa, Matapelajaran matapelajaran, Integer smt) {

		if (jenisItemPenilaianSiswa != null && jenisItemPenilaianSiswa.getId() != null) {
			String[] nilais = detailNilai == null ? new String[] {} : detailNilai.split(";");
			for (String nn : nilais) {
				try {
					String[] s = StringUtils.split(nn, "|");
					Long formatId = Long.parseLong(s[0]);
					Long matpelId = Long.parseLong(s[1]);
					Integer smtId = Integer.parseInt(s[6]);
					Long grupId = Long.parseLong(s[7]);
					if (jenisItemPenilaianSiswa.getId().equals(formatId) && matapelajaran.getId().equals(matpelId)
							&& smtId.equals(smt) && grupId.equals(grupKategoriItemPenilaianSiswa.getId())) {
						return Boolean.parseBoolean(s[5]);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/KelasLesSiswaPunyaSiswa.java:312");

				}
			}
		}

		return false;
	}

	/**
	 * Menghitung nilai agregat satu kategori penilaian untuk peserta ini, dengan menjalankan
	 * formula {@link GrupPenilaianUtil#hitung} atas seluruh item penilaian yang relevan.
	 *
	 * <p><b>Cara kerja:</b> seluruh item pada {@code jenisItemPenilaianSiswas} lebih dulu
	 * didaftarkan ke peta variabel formula dengan nilai awal {@code "0.0"} (beserta pasangan
	 * bersufiks {@code _s} untuk status verifikasi), sehingga formula tetap dapat dievaluasi
	 * walaupun sebagian item belum dinilai. Payload {@link #getDetailNilai()} lalu ditelusuri;
	 * setiap entri yang cocok mata pelajaran + semester + kategori (dan lolos filter
	 * {@code hanyaValid}) menimpa variabel bersangkutan. Terakhir formula target dijalankan dengan
	 * stempel waktu {@code WaktuUtil.getDate()}.</p>
	 *
	 * <p><b>Menimpa</b> implementasi induk. Dua perbedaan yang berdampak: (1) tanpa penjaga
	 * {@code splitNilai()}; (2) nilai dimasukkan ke peta <b>apa adanya</b> ({@code s[2]}), bukan
	 * lewat {@code nilaiUntukFormula()} milik induk — sehingga nilai bertipe pilihan seperti
	 * {@code "A:80"} masuk ke mesin formula sebagai teks non-numerik. Lihat dokumentasi kelas.</p>
	 *
	 * <p><b>Perlakuan {@code hanyaValid}</b> sama seperti
	 * {@link #retreiveDetailNilai(JenisItemPenilaianSiswa, GrupKategoriItemPenilaianSiswa, Matapelajaran, Integer, Boolean)}:
	 * dipaksa {@code null} bila kelas les tidak mensyaratkan verifikasi.</p>
	 *
	 * @param jenisItemPenilaianSiswas daftar item penilaian yang menjadi variabel formula
	 * @param target                   nama/target formula yang dievaluasi {@link GrupPenilaianUtil}
	 * @param matapelajaran            mata pelajaran; {@code null} atau tanpa id menghasilkan
	 *                                 {@code 0.0}
	 * @param grupPenilaian            grup penilaian pemilik formula
	 * @param grupKategoriItemPenilaianSiswa kategori yang entri-entrinya diperhitungkan
	 * @param smt                      semester yang diperhitungkan
	 * @param hanyaValid               filter status verifikasi; {@code null} berarti tanpa filter
	 * @return hasil formula; {@code 0.0} bila mata pelajaran tidak valid
	 * @throws Exception diteruskan apa adanya dari {@link GrupPenilaianUtil#hitung}
	 */
	public Double retreiveTotalNilai(List<JenisItemPenilaianSiswa> jenisItemPenilaianSiswas, String target,
			Matapelajaran matapelajaran, GrupPenilaian grupPenilaian,
			GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa, Integer smt, Boolean hanyaValid)
			throws Exception {

		if (getKelasLesSiswa() != null && !getKelasLesSiswa().getPublikasiNilaiHarusTelahDiverifikasi()) {
			hanyaValid = null;
		}

		Double total = 0.0;
		if (matapelajaran != null && matapelajaran.getId() != null) {
			String[] nilais = detailNilai == null ? new String[] {} : detailNilai.split(";");
			Map<String, String> data = new HashMap<String, String>();

			for (JenisItemPenilaianSiswa jenisItemPenilaianSiswa : jenisItemPenilaianSiswas) {
				if (jenisItemPenilaianSiswa != null) {
					data.put(jenisItemPenilaianSiswa.getKode(), "0.0");
					data.put(jenisItemPenilaianSiswa.getKode() + "_s", "0.0");
				}
			}

			for (String nn : nilais) {
				try {
					String[] s = StringUtils.split(nn, "|");

					Long matpelId = Long.parseLong(s[1]);

					Integer smtId = Integer.parseInt(s[6]);
					Boolean valid = Boolean.parseBoolean(s[5]);
					Long grupId = Long.parseLong(s[7]);

					if ((hanyaValid == null || hanyaValid.equals(valid)) && matapelajaran.getId().equals(matpelId)
							&& smtId.equals(smt) && grupId.equals(grupKategoriItemPenilaianSiswa.getId())) {
						String n = s[2];
						JenisItemPenilaianSiswa jenisItemPenilaianSiswa = (JenisItemPenilaianSiswa) ConstantValues
								.ambil(JenisItemPenilaianSiswa.class.getName(), Long.parseLong(s[0]));
						data.put(jenisItemPenilaianSiswa.getKode(), n);

						boolean sesuai = retreiveDetailVerify(jenisItemPenilaianSiswa, grupKategoriItemPenilaianSiswa,
								matapelajaran, smt);
						data.put(jenisItemPenilaianSiswa.getKode() + "_s", sesuai ? "1.0" : "0.0");
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/KelasLesSiswaPunyaSiswa.java:363");
//					e.printStackTrace();
				}
			}

			Date sekarang = WaktuUtil.getDate();

//			System.out.println("data -> " + data + " " + target);
			total = GrupPenilaianUtil.hitung(data, matapelajaran, target, grupPenilaian, null, sekarang, null);
		}

		return total;
	}

	/**
	 * Menyisipkan atau memperbarui <b>satu</b> nilai di dalam payload {@link #getDetailNilai()},
	 * dengan menulis ulang seluruh payload secara utuh.
	 *
	 * <p><b>Cara kerja:</b> payload lama diuraikan per entri; entri yang cocok kombinasi item +
	 * mata pelajaran + semester + kategori diganti dengan entri baru, entri lain disalin apa
	 * adanya. Bila tidak ada yang cocok, entri baru <b>ditambahkan</b> di akhir. Hasilnya
	 * ditugaskan langsung ke field {@code detailNilai} (bukan lewat
	 * {@link #setDetailNilai(String)}).</p>
	 *
	 * <p><b>Sanitasi masukan:</b> karakter {@code |} pada {@code jumlah} diganti spasi dan
	 * {@code ;} diganti koma agar tidak merusak pemisah format. Bila {@code jumlah} berisi string
	 * kosong, {@code verify} dipaksa {@code false} (nilai kosong tidak boleh berstatus
	 * terverifikasi).</p>
	 *
	 * <p><b>Menimpa</b> implementasi induk, dan salinan ini <b>kehilangan</b> baris
	 * {@code jumlah = jumlah == null ? "" : jumlah;} milik induk. Akibatnya {@code jumlah}
	 * {@code null} lolos melewati {@code StringUtils.replace} (yang mengembalikan {@code null})
	 * dan berakhir tersimpan sebagai teks literal {@code "null"} pada ruas nilai. Lihat juga
	 * peringatan "ruas kosong" pada dokumentasi kelas — menulis nilai kosong membuat entri itu
	 * tidak dapat dibaca kembali dengan benar.</p>
	 *
	 * <p><b>Efek samping:</b> mengubah state entity; pemanggil bertanggung jawab menyimpannya
	 * ({@code DetailPenilaianLesSiswaHelper} memanggil {@code Common.refreshUpdate} sesudahnya).
	 * Dipanggil dari setiap sel nilai yang diedit di layar penilaian kelas les dan dari unggah
	 * Excel massal — <b>keduanya tanpa gerbang hak</b>, lihat catatan hak akses pada dokumentasi
	 * kelas.</p>
	 *
	 * @param jenisItemPenilaianSiswa      item penilaian yang nilainya ditulis; {@code null}
	 *                                     membuat method tidak melakukan apa pun
	 * @param matapelajaran                mata pelajaran terkait
	 * @param grupKategoriItemPenilaianSiswa kategori/grup penilaian terkait
	 * @param jumlah                       nilai yang ditulis (teks bebas, disanitasi)
	 * @param verify                       status verifikasi yang ikut disimpan
	 * @param smt                          semester terkait
	 */
	public void populateDetailNilai(JenisItemPenilaianSiswa jenisItemPenilaianSiswa, Matapelajaran matapelajaran,
			GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa, String jumlah, Boolean verify, Integer smt) {
		if (jumlah != null && jumlah.trim().isEmpty()) {
			verify = false;
		}
		jumlah = org.apache.commons.lang3.StringUtils.replace(jumlah, "|", " ");
		jumlah = org.apache.commons.lang3.StringUtils.replace(jumlah, ";", ",");
		if (jenisItemPenilaianSiswa != null) {
			String formatBaru = "";
			String[] nilais = detailNilai == null ? new String[] {} : detailNilai.split(";");
			Boolean ada = false;
			for (String nn : nilais) {
				try {
					String aformatBaru = "";
					String[] s = StringUtils.split(nn, "|");
					if (!s[0].trim().isEmpty()) {
						Long formatId = Long.parseLong(s[0]);
						Long matpelId = Long.parseLong(s[1]);
						Integer smtId = Integer.parseInt(s[6]);
						Long grupId = Long.parseLong(s[7]);
						if (jenisItemPenilaianSiswa.getId().equals(formatId) && matapelajaran.getId().equals(matpelId)
								&& smtId.equals(smt) && grupId.equals(grupKategoriItemPenilaianSiswa.getId())) {
							aformatBaru = jenisItemPenilaianSiswa.getId() + "|" + matapelajaran.getId() + "|" + jumlah
									+ "|0|0|" + verify + "|" + smt + "|" + grupKategoriItemPenilaianSiswa.getId();
							ada = true;
						} else {
							aformatBaru = nn;
						}
						if (!aformatBaru.trim().isEmpty()) {
							formatBaru += formatBaru.isEmpty() ? aformatBaru : ";" + aformatBaru;
						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/KelasLesSiswaPunyaSiswa.java:409");
				}
			}

			if (!ada) {
				String aformatBaru = jenisItemPenilaianSiswa.getId() + "|" + matapelajaran.getId() + "|" + jumlah
						+ "|0|0|" + verify + "|" + smt + "|" + grupKategoriItemPenilaianSiswa.getId();
				formatBaru += formatBaru.isEmpty() ? aformatBaru : ";" + aformatBaru;
			}

			detailNilai = formatBaru;
		}
	}

	/**
	 * @return nomor peserta UTS. <b>Kolom mati</b>: tidak ada pemanggil {@link #setNoUts(String)}
	 *         di repo, sehingga selalu {@code null}. Sisa salinan dari padanan modul perguruan
	 *         tinggi yang logika pembangkit nomornya tidak ikut disalin.
	 */
	public String getNoUts() {
		return noUts;
	}

	/**
	 * Menyetel nomor peserta UTS. Tidak dipanggil dari mana pun.
	 *
	 * @param noUts nomor peserta UTS
	 */
	public void setNoUts(String noUts) {
		this.noUts = noUts;
	}

	/**
	 * @return nomor peserta UAS. <b>Kolom mati</b>, sama seperti {@link #getNoUts()}.
	 */
	public String getNoUas() {
		return noUas;
	}

	/**
	 * Menyetel nomor peserta UAS. Tidak dipanggil dari mana pun.
	 *
	 * @param noUas nomor peserta UAS
	 */
	public void setNoUas(String noUas) {
		this.noUas = noUas;
	}

	/**
	 * Payload nilai <b>agregat per kategori</b> untuk peserta ini, dalam satu kolom {@code text}.
	 *
	 * <p><b>Format:</b> sama seperti {@link #getDetailNilai()} — delapan ruas dipisah pipa, entri
	 * dipisah titik koma — kecuali ruas pertama selalu {@code "0"} (tidak ada id item penilaian
	 * pada tingkat agregat):
	 * {@code 0|matapelajaranId|nilai|0|0|verified|semester|grupKategoriId}.</p>
	 *
	 * <p>Ditulis
	 * {@link #populateDetailNilaiTotal(Matapelajaran, GrupKategoriItemPenilaianSiswa, Double, Boolean, Integer)}
	 * dan dibaca
	 * {@link #retreiveDetailNilaiTotal(GrupKategoriItemPenilaianSiswa, Matapelajaran, Integer)}
	 * serta {@link #retreiveTotalNilaiTotal(String, Matapelajaran, GrupPenilaian, Integer, List)}.</p>
	 *
	 * @return payload nilai agregat; string kosong bila belum ada (diinisialisasi {@code ""})
	 */
	@Column(columnDefinition = "text", name = "detail_nilai_total")
	public String getDetailNilaiTotal() {
		return detailNilaiTotal;
	}

	/**
	 * Menyetel payload nilai agregat secara utuh. Umumnya <b>tidak</b> dipanggil langsung — pakai
	 * {@link #populateDetailNilaiTotal(Matapelajaran, GrupKategoriItemPenilaianSiswa, Double, Boolean, Integer)}.
	 *
	 * @param detailNilaiTotal payload agregat dalam format {@link #getDetailNilaiTotal()}
	 */
	public void setDetailNilaiTotal(String detailNilaiTotal) {
		this.detailNilaiTotal = detailNilaiTotal;
	}

	/** Payload nilai agregat per kategori; lihat {@link #getDetailNilaiTotal()}. */
	private String detailNilaiTotal = "";
	/**
	 * Status kelulusan/persetujuan peserta dari kelas les — <b>gerbang cetak sertifikat</b>.
	 * Lihat {@link #getAcc()}.
	 */
	private Boolean acc;

	/**
	 * Membaca satu nilai agregat dari payload {@link #getDetailNilaiTotal()} untuk kombinasi
	 * kategori + mata pelajaran + semester tertentu.
	 *
	 * <p><b>Menimpa</b> implementasi induk dengan salinan lama tanpa penjaga {@code splitNilai()};
	 * lihat dokumentasi kelas. Entri yang gagal di-parse dilewati diam-diam (dicatat
	 * {@code ErrorAuditUtil}).</p>
	 *
	 * @param grupKategoriItemPenilaianSiswa kategori yang dicari; {@code null}/tanpa id
	 *                                       menghasilkan string kosong
	 * @param matapelajaran                  mata pelajaran yang harus cocok
	 * @param smt                            semester yang harus cocok
	 * @return nilai agregat sebagai teks, atau string kosong bila tidak ditemukan
	 */
	public String retreiveDetailNilaiTotal(GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa,
			Matapelajaran matapelajaran, Integer smt) {

		if (grupKategoriItemPenilaianSiswa != null && grupKategoriItemPenilaianSiswa.getId() != null) {
			String[] nilais = detailNilaiTotal == null ? new String[] {} : detailNilaiTotal.split(";");
			for (String nn : nilais) {
				try {
					String[] s = StringUtils.split(nn, "|");
					Long matpelId = Long.parseLong(s[1]);
					Integer smtId = Integer.parseInt(s[6]);
					Long grupId = Long.parseLong(s[7]);
					if (matapelajaran.getId().equals(matpelId) && smtId.equals(smt)
							&& grupId.equals(grupKategoriItemPenilaianSiswa.getId())) {
						return s[2];
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/KelasLesSiswaPunyaSiswa.java:466");

				}
			}
		}

		return "";
	}

	/**
	 * Menghitung nilai akhir satu grup penilaian dengan menjalankan formula
	 * {@link GrupPenilaianUtil#hitung} atas nilai-nilai <b>agregat per kategori</b> —
	 * lapis kedua mesin nilai, di atas
	 * {@link #retreiveTotalNilai(List, String, Matapelajaran, GrupPenilaian, GrupKategoriItemPenilaianSiswa, Integer, Boolean)}.
	 *
	 * <p>Seluruh kategori pada {@code grupKategoriItemPenilaianSiswas} lebih dulu didaftarkan ke
	 * peta variabel formula dengan nilai awal {@code "0.0"}, lalu ditimpa entri payload yang cocok.
	 * Kategori pada tiap entri diresolusi lewat cache {@code ConstantValues.ambil()} memakai id
	 * dari ruas ke-8, sehingga kategori di luar daftar parameter pun ikut menambah variabel.</p>
	 *
	 * <p><b>Perhatikan asimetri filter:</b> berbeda dari
	 * {@link #retreiveDetailNilaiTotal(GrupKategoriItemPenilaianSiswa, Matapelajaran, Integer)},
	 * pencocokan di sini hanya memakai mata pelajaran + semester dan <b>tidak</b> menyaring
	 * kategori — memang disengaja, karena seluruh kategori dibutuhkan sebagai variabel formula.</p>
	 *
	 * <p><b>Menimpa</b> implementasi induk; sama seperti kerabatnya, salinan ini tanpa
	 * {@code splitNilai()} dan tanpa {@code nilaiUntukFormula()}.</p>
	 *
	 * @param target                         nama/target formula yang dievaluasi
	 * @param matapelajaran                  mata pelajaran; {@code null}/tanpa id menghasilkan
	 *                                       {@code 0.0}
	 * @param grupPenilaian                  grup penilaian pemilik formula
	 * @param smt                            semester yang diperhitungkan
	 * @param grupKategoriItemPenilaianSiswas daftar kategori yang menjadi variabel formula
	 * @return hasil formula; {@code 0.0} bila mata pelajaran tidak valid
	 * @throws Exception diteruskan apa adanya dari {@link GrupPenilaianUtil#hitung}
	 */
	public Double retreiveTotalNilaiTotal(String target, Matapelajaran matapelajaran, GrupPenilaian grupPenilaian,
			Integer smt, List<GrupKategoriItemPenilaianSiswa> grupKategoriItemPenilaianSiswas) throws Exception {
		Double total = 0.0;
		if (matapelajaran != null && matapelajaran.getId() != null) {
			String[] nilais = detailNilaiTotal == null ? new String[] {} : detailNilaiTotal.split(";");
			Map<String, String> data = new HashMap<String, String>();

			for (GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa : grupKategoriItemPenilaianSiswas) {
				if (grupKategoriItemPenilaianSiswa != null) {
					data.put(grupKategoriItemPenilaianSiswa.getKode(), "0.0");
				}
			}

			for (String nn : nilais) {
				try {
					String[] s = StringUtils.split(nn, "|");

					Long matpelId = Long.parseLong(s[1]);

					Integer smtId = Integer.parseInt(s[6]);
					Long grupId = Long.parseLong(s[7]);

					if (matapelajaran.getId().equals(matpelId) && smtId.equals(smt)) {
						String n = s[2];
						GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa = (GrupKategoriItemPenilaianSiswa) ConstantValues
								.ambil(GrupKategoriItemPenilaianSiswa.class.getName(), grupId);
						data.put(grupKategoriItemPenilaianSiswa.getKode(), n);

					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/KelasLesSiswaPunyaSiswa.java:504");
//					e.printStackTrace();
				}
			}

			Date sekarang = WaktuUtil.getDate();

//			System.out.println("retreiveTotalNilaiTotal data -> " + data + " " + target);
			total = GrupPenilaianUtil.hitung(data, matapelajaran, target, grupPenilaian, null, sekarang, null);
		}

		return total;
	}

	/**
	 * Menyisipkan atau memperbarui <b>satu</b> nilai agregat kategori di dalam payload
	 * {@link #getDetailNilaiTotal()}, dengan menulis ulang seluruh payload secara utuh.
	 *
	 * <p>Mekanismenya sejajar dengan
	 * {@link #populateDetailNilai(JenisItemPenilaianSiswa, Matapelajaran, GrupKategoriItemPenilaianSiswa, String, Boolean, Integer)}:
	 * entri yang cocok mata pelajaran + semester + kategori diganti, sisanya disalin, dan bila tak
	 * ada yang cocok entri baru ditambahkan. Ruas pertama entri agregat selalu ditulis {@code "0"}.</p>
	 *
	 * <p><b>Kuirk yang perlu diketahui:</b> penjaga di awal method berbunyi
	 * {@code if (jumlah != null) { verify = false; }} — kebalikan dari kerabatnya yang memaksa
	 * {@code verify = false} hanya ketika nilainya <i>kosong</i>. Praktisnya, setiap kali nilai
	 * agregat ditulis dengan angka, status verifikasinya <b>selalu dipaksa {@code false}</b>;
	 * parameter {@code verify} hanya berpengaruh saat {@code jumlah} bernilai {@code null}.
	 * Perilaku ini identik dengan versi di
	 * {@link ais.database.model.sekolah.VoKelasPunyaSiswa}, jadi bukan divergensi lokal —
	 * melainkan kuirk yang dibawa kedua salinan.</p>
	 *
	 * <p><b>Efek samping:</b> mengubah field {@code detailNilaiTotal} langsung; pemanggil
	 * ({@code DetailPenilaianLesSiswaHelper}, tanpa gerbang hak) bertanggung jawab menyimpannya.</p>
	 *
	 * @param matapelajaran                  mata pelajaran terkait
	 * @param grupKategoriItemPenilaianSiswa kategori yang nilai agregatnya ditulis; {@code null}
	 *                                       membuat method tidak melakukan apa pun
	 * @param jumlah                         nilai agregat yang ditulis
	 * @param verify                         status verifikasi — lihat kuirk di atas
	 * @param smt                            semester terkait
	 */
	public void populateDetailNilaiTotal(Matapelajaran matapelajaran,
			GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa, Double jumlah, Boolean verify, Integer smt) {
		if (jumlah != null) {
			verify = false;
		}
		if (grupKategoriItemPenilaianSiswa != null) {
			String formatBaru = "";
			String[] nilais = detailNilaiTotal == null ? new String[] {} : detailNilaiTotal.split(";");
			Boolean ada = false;
			for (String nn : nilais) {
				try {
					String aformatBaru = "";
					String[] s = StringUtils.split(nn, "|");
					if (!s[0].trim().isEmpty()) {
						Long matpelId = Long.parseLong(s[1]);
						Integer smtId = Integer.parseInt(s[6]);
						Long grupId = Long.parseLong(s[7]);
						if (matapelajaran.getId().equals(matpelId) && smtId.equals(smt)
								&& grupId.equals(grupKategoriItemPenilaianSiswa.getId())) {
							aformatBaru = "0|" + matapelajaran.getId() + "|" + jumlah + "|0|0|" + verify + "|" + smt
									+ "|" + grupKategoriItemPenilaianSiswa.getId();
							ada = true;
						} else {
							aformatBaru = nn;
						}
						if (!aformatBaru.trim().isEmpty()) {
							formatBaru += formatBaru.isEmpty() ? aformatBaru : ";" + aformatBaru;
						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/KelasLesSiswaPunyaSiswa.java:547");
				}
			}

			if (!ada) {
				String aformatBaru = "0|" + matapelajaran.getId() + "|" + jumlah + "|0|0|" + verify + "|" + smt + "|"
						+ grupKategoriItemPenilaianSiswa.getId();
				formatBaru += formatBaru.isEmpty() ? aformatBaru : ";" + aformatBaru;
			}

			detailNilaiTotal = formatBaru;
		}
	}

	/**
	 * Nilai pengganti untuk {@link #getKeterangan1()}/{@link #getKeterangan2()} yang masih kosong:
	 * representasi teks dari {@code JSONObject} hampa, yaitu {@code "{}"}. Dihitung sekali saat
	 * kelas dimuat agar pembaca sisi klien selalu menerima JSON yang sah alih-alih {@code null}.
	 */
	final static String D = new JSONObject().toString();

	/**
	 * Catatan/JSON semester 1 untuk peserta ini.
	 *
	 * <p>Mengembalikan {@link #D} ({@code "{}"}) bila kolomnya masih {@code null}, sehingga
	 * pembaca JSON tidak perlu menangani nilai kosong. <b>Nilai pengganti itu tidak ditulis balik
	 * ke field</b> — kolomnya tetap {@code NULL} di basis data, jadi getter ini bukan getter
	 * destruktif.</p>
	 *
	 * @return isi kolom, atau {@code "{}"} bila kosong; tidak pernah {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getKeterangan1() {
		return keterangan1 == null ? D : keterangan1;
	}

	/**
	 * Menyetel catatan/JSON semester 1. Tanpa validasi format — pemanggil bertanggung jawab
	 * mengirim JSON yang sah bila isinya akan dibaca sebagai JSON.
	 *
	 * @param keterangan1 isi catatan semester 1
	 */
	public void setKeterangan1(String keterangan1) {
		this.keterangan1 = keterangan1;
	}

	/**
	 * Catatan/JSON semester 2 untuk peserta ini; perilakunya identik dengan
	 * {@link #getKeterangan1()}, termasuk pengembalian {@code "{}"} untuk kolom kosong.
	 *
	 * @return isi kolom, atau {@code "{}"} bila kosong; tidak pernah {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getKeterangan2() {
		return keterangan2 == null ? D : keterangan2;
	}

	/**
	 * Menyetel catatan/JSON semester 2. Tanpa validasi format.
	 *
	 * @param keterangan2 isi catatan semester 2
	 */
	public void setKeterangan2(String keterangan2) {
		this.keterangan2 = keterangan2;
	}

	/**
	 * Mengembalikan calon siswa asal-usul PPDB peserta ini, bila ada.
	 *
	 * <p>Relasi {@code @ManyToOne} {@code LAZY} opsional (FK {@code calon_siswa},
	 * {@code nullable = true}); proxy diresolusi lewat {@code check()} dan hasilnya ditulis balik
	 * ke field — penulisan balik benign.</p>
	 *
	 * <p>Diisi oleh jalur PPDB ({@code CommonPSB.masukkanKelasLes},
	 * {@code CalonSiswa.populatePembayaran}) dan oleh layar/API yang mendaftarkan siswa ke kelas
	 * les ({@code SiswaAction}, {@code TagihanSiswa}) lewat {@code siswa.ambilCalonSiswa()}.
	 * Perannya bukan sekadar jejak: nilai ini <b>menggerakkan penulisan balik</b> pada
	 * {@link #getSiswa()}, dan riwayat pembayarannya ikut diperhitungkan
	 * {@link #ada()}/{@link #ambilBelumBayar()} sehingga tagihan yang dilunasi semasa masih
	 * berstatus calon siswa tetap diakui setelah menjadi siswa resmi.</p>
	 *
	 * @return calon siswa asal, atau {@code null} bila peserta tidak berasal dari jalur PPDB
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "calon_siswa", nullable = true)
	public CalonSiswa getCalonSiswa() {
		calonSiswa = check(calonSiswa);
		return calonSiswa;
	}

	/**
	 * Menyetel calon siswa asal-usul PPDB peserta.
	 *
	 * <p><b>Perhatian:</b> mengisi nilai ini mengaktifkan penulisan balik pada {@link #getSiswa()}
	 * — sejak saat itu FK {@code siswa_id} akan selalu mengikuti {@code calonSiswa.getSiswa()}.</p>
	 *
	 * @param calonSiswa calon siswa asal, boleh {@code null}
	 */
	public void setCalonSiswa(CalonSiswa calonSiswa) {
		this.calonSiswa = calonSiswa;
	}

	/**
	 * Nomor urut tampil peserta di dalam kelas les, dipakai sebagai kunci pengurutan grid peserta
	 * dan grid penilaian ({@code addOrder(Order.asc("nomorUrut"))}).
	 *
	 * <p><b>Tidak pernah mengembalikan {@code null}</b> — {@code null} di-<i>coalesce</i> menjadi
	 * {@code 0}. Konsekuensinya {@code compareTo()} milik
	 * {@link ais.database.model.GeneralValueObject}, yang memakai nomor urut sebagai kunci PERTAMA
	 * dan hanya melewatinya bila kedua sisi {@code null}, <b>selalu</b> memakai cabang tersebut:
	 * pada instalasi yang tidak mengisi nomor urut, seluruh baris menjadi "setara" sehingga sebuah
	 * {@code TreeSet}/{@code TreeMap} berkunci entity ini akan menciut jadi satu elemen. Penelusuran
	 * repo menunjukkan pola berbahaya itu <b>tidak dipakai</b> untuk entity ini (pengurutan selalu
	 * dilakukan di sisi SQL), jadi risikonya laten — tetapi jangan memperkenalkan koleksi terurut
	 * berkunci entity ini tanpa comparator eksplisit; pakai {@link #keyUrut()} bila butuh kunci
	 * string yang unik.</p>
	 *
	 * @return nomor urut; {@code 0} bila belum diisi
	 */
	public Integer getNomorUrut() {
		return nomorUrut == null ? 0 : nomorUrut;
	}

	/**
	 * Menyetel nomor urut tampil peserta.
	 *
	 * <p>Satu-satunya pemanggil adalah {@code Intbox} nomor urut pada grid
	 * {@code DetailKelasLesSiswaHelper}, yang langsung menyimpan perubahan lewat
	 * {@code Common.refreshUpdate} — <b>tanpa gerbang hak sama sekali</b>, lihat catatan hak akses
	 * pada dokumentasi kelas.</p>
	 *
	 * @param nomorUrut nomor urut baru; {@code null} dibaca sebagai {@code 0}
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * Implementasi kontrak {@link ais.database.model.sekolah.VoKelasPunyaSiswa#ambilMk()} —
	 * <b>stub</b>.
	 *
	 * <p>Pada roster kelas reguler, method sejenis mengembalikan daftar id mata pelajaran yang
	 * <i>tidak</i> diambil peserta sehingga layar absensi/penilaian dapat menyaringnya. Kelas les
	 * hanya punya satu mata pelajaran, jadi konsep pengecualian itu tidak berlaku dan method ini
	 * selalu mengembalikan daftar kosong (artinya: tidak ada yang dikecualikan).</p>
	 *
	 * @return selalu {@link ArrayList} kosong yang baru; tidak pernah {@code null}
	 */
	@Override
	public List<Long> ambilMk() {
		// TODO Auto-generated method stub
		return new ArrayList<Long>();
	}

	/**
	 * Implementasi kontrak {@link ais.database.model.sekolah.VoKelasPunyaSiswa#ambilKelasSiswa()} —
	 * <b>stub yang selalu {@code null}</b>.
	 *
	 * <p>Kontrak induk bertipe kembalian {@link KelasSiswa} (kelas reguler), sedangkan induk baris
	 * ini adalah {@link KelasLesSiswa} — dua tipe yang tidak sekerabat, sehingga tidak ada nilai
	 * yang bisa dikembalikan.</p>
	 *
	 * <p><b>Dampak yang perlu diketahui:</b> mesin nilai versi induk memakai method ini sebagai
	 * penjaga saklar "Publikasi Nilai Harus Telah Diverifikasi"
	 * ({@code ambilKelasSiswa() != null && !ambilKelasSiswa().getPublikasiNilaiHarusTelahDiverifikasi()}).
	 * Karena selalu {@code null}, penjaga itu <b>tidak pernah menyala</b> — versi induk akan selalu
	 * gagal-tertutup untuk kelas les. Override lokal di berkas ini menghindarinya dengan memakai
	 * {@link #getKelasLesSiswa()} secara langsung; itulah alasan ketujuh method mesin nilai
	 * ditimpa di sini tidak dapat dihapus begitu saja.</p>
	 *
	 * @return selalu {@code null}
	 */
	@Override
	public KelasSiswa ambilKelasSiswa() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Status <b>kelulusan/persetujuan</b> peserta dari kelas les — gerbang cetak sertifikat.
	 *
	 * <p>Nilai {@code null} dibaca sebagai {@code false} (belum lulus). Konsumennya:</p>
	 * <ul>
	 *   <li>{@code DetailKelasLesSiswaHelper} — mengendalikan visibilitas tombol cetak sertifikat
	 *       (muncul bila {@code acc} dan kelas lesnya punya template
	 *       {@code KelasLesSiswa.getSertifikat()}) sekaligus <b>menyembunyikan tombol Hapus</b>
	 *       (peserta yang sudah lulus tidak boleh dihapus);</li>
	 *   <li>{@code SertifikatAction.cetakSertifikat(KelasLesSiswaPunyaSiswa)} — mencetak sertifikat
	 *       kelulusan;</li>
	 *   <li>{@code SiswaAction}/{@code CalonSiswaAction}/{@code LaporanApi}/{@code TagihanSiswa} —
	 *       menandai status {@code sudah_lulus} pada layar dan respons API.</li>
	 * </ul>
	 *
	 * <p><b>Catatan hak akses:</b> checkbox "Lulus" yang menyetel nilai ini dirender <b>tanpa
	 * gerbang hak</b> pada {@code DetailKelasLesSiswaHelper} dan langsung menyimpan perubahannya —
	 * hak BACA saja cukup untuk meluluskan peserta mana pun sekaligus membuka jalur cetak
	 * sertifikat resminya.</p>
	 *
	 * @return {@code true} bila peserta sudah dinyatakan lulus; tidak pernah {@code null}
	 */
	public Boolean getAcc() {
		return acc == null ? false : acc;
	}

	/**
	 * Menyetel status kelulusan/persetujuan peserta dari kelas les.
	 *
	 * @param acc status kelulusan; {@code null} dibaca sebagai {@code false} oleh {@link #getAcc()}
	 */
	public void setAcc(Boolean acc) {
		this.acc = acc;
	}
}
