package ais.database.model.sekolah;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

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
import ais.database.model.CommonVO;
import ais.database.model.GeneralValueObject;
import ais.database.model.ParameterTambahan;
import ais.database.model.file.LampiranLain;
import ais.ui.util.WaktuUtil;

/**
 * Baris <b>catatan siswa</b> &mdash; satu pengamatan/observasi yang ditulis guru atau wali kelas
 * mengenai seorang siswa, di luar nilai akademik.
 *
 * <p>Tabel: {@code sekolah.catatan_siswa}. Satu baris = satu catatan untuk SATU siswa pada satu
 * tahun ajaran + semester, dikategorikan oleh {@link JenisCatatanSiswa} dan dapat dilengkapi
 * sejumlah <i>field kustom</i> (&quot;parameter tambahan&quot;) yang bentuknya ditentukan oleh
 * jenis catatan tersebut.</p>
 *
 * <h2>Domain (TERVERIFIKASI dari kode dan panduan aplikasi)</h2>
 * <p>Panduan resmi halaman ini ({@code webapp/WEB-INF/bantuan/catatan_siswa.html}, ditampilkan
 * tombol &quot;Bantuan&quot; pada {@code catatan_siswa.zul}) menyatakan halaman ini dipakai
 * &quot;untuk mencatat catatan mengenai siswa, baik catatan positif maupun catatan yang memerlukan
 * perhatian dan pembinaan&quot;, yaitu <b>perilaku, sikap, kepemimpinan, pelanggaran ringan, dan
 * hal yang perlu dibina</b> &mdash; secara eksplisit sebagai bahan &quot;komunikasi dengan orang
 * tua&quot;. Dengan kata lain isi kolom {@link #getKeterangan()} dan parameter tambahannya adalah
 * <b>data pribadi bersifat sensitif tentang anak di bawah umur</b> (catatan perilaku/pembinaan),
 * bukan sekadar catatan administratif. Ini penting untuk menilai dampak temuan kontrol akses yang
 * dirangkum di bawah.</p>
 * <p>Tidak ada daftar jenis bawaan: {@link JenisCatatanSiswa} tidak punya auto-seed, sehingga
 * seluruh nama kategori (&quot;Prestasi&quot;, &quot;Pelanggaran&quot;, &quot;Konseling&quot;,
 * &hellip;) diketik sendiri oleh admin tiap sekolah. Entity ini karena itu bersifat serbaguna:
 * satu instalasi bisa memakainya untuk apresiasi, instalasi lain untuk catatan pelanggaran atau
 * catatan bimbingan konseling.</p>
 *
 * <h2>Layar &amp; jalur pemakai (TERVERIFIKASI)</h2>
 * <ul>
 *   <li><b>Layar utama:</b> {@code ais.action.master.sekolah.CatatanSiswaAction} +
 *       {@code /pages/master/sekolah/catatan_siswa.zul}. Punya 5 tab: Dasbor, Catatan Siswa
 *       (daftar/CRUD), Jenis Catatan Siswa, Manajemen Parameter, dan Laporan Catatan Siswa.</li>
 *   <li><b>Form isian dinamis:</b>
 *       {@code ais.action.master.sekolah.helper.ParameterTambahanCatatanSiswaListener} &mdash;
 *       membangun baris input parameter tambahan dan memanggil
 *       {@link #populateParameterTambahan(List)} saat simpan.</li>
 *   <li><b>Dasbor:</b> {@code ais.action.master.catatan.DasbordCatatan} dengan
 *       {@code Lingkup.SISWA} (dimuat otomatis oleh {@code CatatanSiswaAction.onDasbor()}).</li>
 *   <li><b>Laporan/cetak:</b> {@code ais.action.report.format1.sekolah.LaporanCatatanSiswa}
 *       (cetak per-catatan lewat tombol printer di grid, dan cetak otomatis setelah simpan) serta
 *       {@code ...LaporanRaporSiswa} (blok catatan pada rapor, aktif bila
 *       {@code JenisRaporSiswa.getAmbilCatatanSiswa()} bernilai {@code true}).</li>
 *   <li><b>REST/mobile:</b> {@code ais.action.servlet.api.CatatanApi} &mdash; rute
 *       {@code catatan_siswa_daftar}, {@code catatan_siswa_detail}, {@code catatan_siswa_simpan},
 *       {@code catatan_siswa_hapus}, {@code catatan_siswa_jenis}, {@code catatan_siswa_parameter}.</li>
 *   <li><b>Dasbor agregat:</b> {@code ais.action.master.dashboard.admin.DasborAkademikSekolah}
 *       (kartu &quot;Catatan Siswa&quot; dan tabel &quot;Top Catatan Per Kelas&quot;),
 *       {@code DasboardJadwalPelajaran}, {@code ProfileSekolahLanjutanDashboard}.</li>
 * </ul>
 *
 * <h2>Warisan {@link GeneralValueObject}</h2>
 * <p>{@link GeneralValueObject} <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} &mdash;
 * ia POJO abstrak biasa sehingga Hibernate <b>tidak memetakan properti induknya sama sekali</b>.
 * Karena itu {@link #getId()}, {@link #getOleh()}, {@link #getOlehId()}, dan
 * {@link #getTanggal_dirubah()} <b>harus</b> dideklarasikan ulang di kelas ini, begitu pula field
 * {@code nama} dan {@code keterangan} yang menutupi (<i>shadow</i>) field senama milik induk.
 * Duplikasi tersebut adalah KEHARUSAN TEKNIS, bukan bug &mdash; jangan &quot;dirapikan&quot; dengan
 * menghapusnya.</p>
 * <p>Kelas ini juga <b>tidak</b> meng-{@code override} {@link GeneralValueObject#compareTo}.
 * Implementasi induk memanggil <i>getter</i> (bukan field) sehingga tetap bekerja secara virtual;
 * kunci {@code nomorUrut} dan {@code nim} milik induk selalu {@code null} pada instance ini,
 * sehingga pengurutan alami praktis selalu jatuh ke kunci ketiga, yaitu {@link #getNama()} &mdash;
 * yang (lihat kuirk di bawah) berisi <b>nama siswa</b>, bukan judul catatan.</p>
 *
 * <h2>Model data &amp; relasi</h2>
 * <ul>
 *   <li>{@link #getSiswa()} &rarr; {@link Siswa} (FK {@code siswa}) &mdash; subjek catatan; sumber
 *       turunan untuk {@link #getKode()}, {@link #getNama()}, {@link #getSekolah()},
 *       {@link #getKelasSiswa()}.</li>
 *   <li>{@link #getGuru()} &rarr; {@link Guru} (FK {@code guru}) &mdash; penulis/pembina catatan.</li>
 *   <li>{@link #getJenisCatatanSiswa()} &rarr; {@link JenisCatatanSiswa} (FK
 *       {@code jenis_catatan_siswa}) &mdash; kategori catatan sekaligus penentu formulir parameter
 *       tambahan yang muncul.</li>
 *   <li>{@link #getKelasSiswa()} &rarr; {@link KelasSiswa} (FK {@code kelas_siswa}) &mdash; rombel
 *       siswa; lihat peringatan getter destruktif.</li>
 *   <li>{@link #getSekolah()}/{@link #getYayasan()} (FK {@code sekolah_id}/{@code yayasan_id})
 *       &mdash; kolom cakupan multi-tenant.</li>
 *   <li>Lampiran berkas TIDAK punya relasi langsung: berkas disimpan sebagai
 *       {@link LampiranLain} dan ditemukan lewat pasangan
 *       {@code (ref = id catatan, jenis = "{kelompokId}-&gt;{parameterId}")}
 *       &mdash; lihat {@link #populateParameterTambahan(List)}.</li>
 * </ul>
 *
 * <h2>Parameter tambahan (field kustom) &mdash; dua kolom teks terdenormalisasi</h2>
 * <p>Nilai field kustom tidak disimpan dalam tabel terpisah, melainkan diserialkan ke DUA kolom
 * teks pada baris ini:</p>
 * <ul>
 *   <li>{@link #getParameterTambahan()} &mdash; versi <b>siap-baca manusia</b>; per baris:
 *       <code>namaKelompok-&gt;labelInputan&lt;=&gt;nilai&lt;=&gt;urlLampiran&lt;=&gt;nomorUrut&lt;=&gt;idParameter&lt;=&gt;idKelompok&lt;=&gt;keterangan</code>.</li>
 *   <li>{@link #getParameterTambahanInds()} &mdash; versi <b>berkunci id</b> (dipakai untuk memuat
 *       ulang nilai ke form); per baris:
 *       <code>idKelompok-&gt;idParameter&lt;=&gt;nilai&lt;=&gt;urlLampiran&lt;=&gt;keterangan</code>.</li>
 * </ul>
 * <p>Antarbaris dipisah {@code "\n"}, antar-ruas {@code "<=>"}. Keduanya ditulis sekaligus oleh
 * {@link #populateParameterTambahan(List)}. Konsekuensi bentuk denormalisasi ini: mengubah label
 * atau nomor urut sebuah {@link ParameterTambahan} <b>tidak</b> memperbarui catatan lama &mdash;
 * catatan lama tetap menyimpan salinan label pada saat disimpan.</p>
 *
 * <h2>Kelompok method</h2>
 * <ul>
 *   <li><b>Jejak audit (deklarasi ulang wajib):</b> {@link #getOleh()}, {@link #setOleh(String)},
 *       {@link #getOlehId()}, {@link #setOlehId(String)}, {@link #onUpdate()},
 *       {@link #getTanggal_dirubah()}, {@link #setTanggal_dirubah(Date)}.</li>
 *   <li><b>Identitas &amp; representasi:</b> {@link #CatatanSiswa()}, {@link #getId()},
 *       {@link #setId(Long)}, {@link #toString()}.</li>
 *   <li><b>Isi catatan:</b> {@link #getNama()}, {@link #setNama(String)},
 *       {@link #getKeterangan()}, {@link #setKeterangan(String)}, {@link #getWaktu()},
 *       {@link #setWaktu(Date)}, {@link #getKode()}, {@link #setKode(String)}.</li>
 *   <li><b>Periode akademik:</b> {@link #getTahunAjaran()}, {@link #setTahunAjaran(String)},
 *       {@link #getSemester()}, {@link #setSemester(Integer)}.</li>
 *   <li><b>Relasi subjek &amp; kategori:</b> {@link #getSiswa()}, {@link #setSiswa(Siswa)},
 *       {@link #getGuru()}, {@link #setGuru(Guru)}, {@link #getJenisCatatanSiswa()},
 *       {@link #setJenisCatatanSiswa(JenisCatatanSiswa)}, {@link #getKelasSiswa()},
 *       {@link #setKelasSiswa(KelasSiswa)}.</li>
 *   <li><b>Cakupan multi-tenant:</b> {@link #getSekolah()}, {@link #setSekolah(Sekolah)},
 *       {@link #getYayasan()}, {@link #setYayasan(Yayasan)}.</li>
 *   <li><b>Parameter tambahan:</b> {@link #getParameterTambahan()},
 *       {@link #setParameterTambahan(String)}, {@link #getParameterTambahanInds()},
 *       {@link #setParameterTambahanInds(String)}, {@link #populateParameterTambahan(List)},
 *       {@link #ambilDataParameterTambahan()}.</li>
 * </ul>
 *
 * <h2>Kuirk &amp; catatan penting (semua TERVERIFIKASI dari kode)</h2>
 * <ol>
 *   <li><b>{@code nama} BUKAN judul catatan.</b> {@link #getNama()} menimpa dirinya dengan
 *       {@code getSiswa().getNama()} setiap kali dibaca &mdash; kolom {@code nama} pada praktiknya
 *       adalah <i>nama siswa</i> yang didenormalisasi, bukan judul. {@code CatatanApi.simpan()}
 *       tetap mewajibkan klien mobile mengirim &quot;nama/judul catatan&quot;, tetapi nilai itu
 *       akan tertimpa nama siswa pada pembacaan berikutnya. Isi catatan yang sebenarnya ada di
 *       {@link #getKeterangan()}.</li>
 *   <li><b>Lima getter melakukan write-back (menulis balik ke field).</b> {@link #getKode()},
 *       {@link #getNama()}, {@link #getSekolah()}, {@link #getYayasan()}, dan
 *       {@link #getKelasSiswa()} bukan getter murni: sekadar MEMBACA baris (mis. merender grid)
 *       sudah mengubah state entity, dan karena entity dalam keadaan <i>managed</i> perubahan itu
 *       ikut ter-<i>flush</i> ke DB tanpa ada tombol Simpan yang ditekan. Yang paling merusak
 *       adalah {@link #getKelasSiswa()} &mdash; lihat butir berikut.</li>
 *   <li><b>{@link #getKelasSiswa()} bisa MENGHAPUS PERMANEN FK kelas.</b> Nilai kolom
 *       {@code kelas_siswa} selalu dibuang dan diganti hasil
 *       {@code Siswa.ambilKelas(siswa, tahunAjaran)}. Bila siswa sudah naik kelas/pindah rombel,
 *       catatan lama akan &quot;pindah&quot; ke kelas terbaru; dan bila {@code ambilKelas}
 *       mengembalikan {@code null} (mis. roster tahun ajaran itu sudah dihapus), FK ditulis
 *       {@code null} secara permanen. Riwayat &quot;catatan ini dibuat saat siswa di kelas apa&quot;
 *       karenanya tidak dapat diandalkan.</li>
 *   <li><b>{@link #getSekolah()} menulis ulang kolom tenant.</b> Sekolah catatan selalu diambil
 *       ulang dari {@code getSiswa().getSekolah()}; memindahkan siswa ke sekolah lain dalam satu
 *       yayasan akan <b>secara retroaktif memindahkan seluruh catatan lamanya</b> ke sekolah baru,
 *       dan {@link #getYayasan()} ikut mengekor. Filter tenant pada laporan/dasbor karenanya tidak
 *       mencerminkan keadaan historis.</li>
 *   <li><b>{@link #setOleh(String)}/{@link #setOlehId(String)} menolak nilai kosong secara
 *       senyap</b> (pola warisan {@link GeneralValueObject}) &mdash; jejak audit tidak bisa
 *       dikosongkan lagi setelah terisi, dan pemanggil tidak diberi tahu bahwa set-nya diabaikan.</li>
 *   <li><b>{@link #ambilDataParameterTambahan()} adalah kode mati</b> untuk kelas ini &mdash; tidak
 *       ada satu pun pemanggil di repositori (yang dipanggil adalah method senama milik entity
 *       lain seperti {@code KegiatanSiswa}/{@code IsiAngketParameterUmum}). Ia juga tidak pernah
 *       mengembalikan daftar kosong; lihat Javadoc method tersebut.</li>
 *   <li><b>{@link #toString()} membaca field {@code nama} langsung, bukan getter</b>, sehingga
 *       untuk entity yang baru dimuat ia bisa mencetak {@code null} padahal
 *       {@link #getNama()} akan mengembalikan nama siswa.</li>
 * </ol>
 *
 * <h2>Kontrol akses pada jalur pemakai (TERVERIFIKASI &mdash; PERLU PERHATIAN)</h2>
 * <p>Ringkasan hasil audit jalur yang membaca/menulis entity ini. Dicatat di sini karena entity
 * ini memuat data perilaku anak di bawah umur.</p>
 * <ul>
 *   <li><b>Role SISWA di-seed dengan hak CRUD PENUH atas menu &quot;Catatan Siswa&quot;.</b>
 *       {@code MenuInitializer.ensureSiswaRoleAndPrivileges()} memberi role {@code Tbmrole.SISWA}
 *       {@code RolePrivilage} dengan {@code create=1, read=1, update=1, delete=1} untuk menu
 *       {@code 48916} yang menunjuk {@code /pages/master/sekolah/catatan_siswa.zul}.</li>
 *   <li><b>{@code CatatanSiswaAction.initCriteria()} sama sekali tidak menyaring berdasarkan
 *       pemilik data.</b> Filter yang ada hanya guru pembina, jenis, semester, tahun ajaran,
 *       sekolah, yayasan, dan kata kunci nama/NIS &mdash; tidak ada pembatas
 *       &quot;siswa = siswa milik akun&quot;. {@code doAfterCompose()} hanya menyembunyikan dua tab
 *       master bagi akun guru/siswa, tanpa menyentuh data. Gabungan dua butir ini berarti akun
 *       siswa dapat <b>melihat, mengubah, dan menghapus catatan perilaku SELURUH siswa di
 *       instalasi</b> &mdash; termasuk menghapus catatan pelanggaran dirinya sendiri.</li>
 *   <li><b>Tidak ada pembatas anak untuk akun wali murid.</b> Berbeda dari entity kesiswaan
 *       sejenis, TIDAK ADA pemanggilan {@code OrangTua.ambilAnakSiswa()} sama sekali pada seluruh
 *       jalur {@code CatatanSiswa} &mdash; jadi bukan sekadar pola <i>fail-open</i>, melainkan
 *       ketiadaan pembatas anak sepenuhnya.</li>
 *   <li><b>Fail-open pada dasbor.</b> {@code DasbordCatatan.muatCatatanSiswa()} hanya memfilter
 *       {@code siswa} bila akun punya {@code Tbmuser.getSiswa()}, atau {@code guru} bila punya
 *       {@code ambilGuru()}; untuk akun lain (wali murid, staf, admin) TIDAK ADA filter, sehingga
 *       hingga {@code MAX_ROWS = 600} catatan lintas siswa/sekolah dirender apa adanya.</li>
 *   <li><b>Fail-open cakupan tenant.</b>
 *       {@code DasborAkademikSekolah.applySekolahFilter()} langsung {@code return} tanpa menambah
 *       pembatas apa pun ketika {@code currentSekolah == null} &mdash; agregat &quot;Catatan
 *       Siswa&quot; dan &quot;Top Catatan Per Kelas&quot; kemudian dihitung lintas seluruh
 *       instalasi.</li>
 *   <li><b>IDOR pada REST {@code CatatanApi}.</b> {@code detail}, {@code simpan}, dan
 *       {@code hapus} hanya memverifikasi token valid lalu memuat baris langsung dari
 *       {@code id} kiriman klien tanpa cek kepemilikan; {@code daftar} menghormati parameter
 *       {@code siswa} apa adanya (Javadoc method itu sendiri menyatakan &quot;id siswa yang dikirim
 *       dihormati&quot;). Token siswa mana pun cukup untuk membaca, mengubah, atau menghapus
 *       catatan siswa lain.</li>
 *   <li><b>Laporan cetak tidak terkunci untuk non-siswa.</b>
 *       {@code LaporanCatatanSiswa} mengunci bandbox pilihan siswa HANYA bila
 *       {@code Common.getCurrentUser().getSiswa() != null}; akun wali murid ({@code getSiswa()}
 *       {@code null}) tetap bebas memilih siswa mana pun untuk dicetak.</li>
 *   <li><b>Pewarisan hak lewat menu induk.</b> Tab &quot;Jenis Catatan Siswa&quot; dan
 *       &quot;Manajemen Parameter&quot; me-{@code MyInclude} halaman master
 *       {@code jenis_catatan_siswa.zul}/{@code parameter_tambahan_catatan_siswa.zul} ke dalam
 *       jendela ini. Karena {@code CommonPrivilages.checkPrevilages()} memutuskan berdasarkan
 *       {@code Common.getCurrentMenu()} (menu terakhir yang diklik), hak atas menu
 *       &quot;Catatan Siswa&quot; efektif menjadi hak CRUD atas kedua master tersebut. Menu yang
 *       sama juga terdaftar rangkap tiga pada tiga induk berbeda
 *       ({@code 73021}, {@code 43116}, {@code 570140} di {@code MenuSnapshotData}), sehingga
 *       tingkat hak yang berlaku bergantung pada dari mana halaman dibuka.</li>
 *   <li><b>Contoh POSITIF (fail-closed) sebagai pembanding:</b> {@code LaporanRaporSiswa}
 *       memakai {@code Restrictions.sqlRestriction("false")} ketika daftar siswa kosong, sehingga
 *       daftar kosong menghasilkan NOL baris, bukan seluruh tabel.</li>
 * </ul>
 *
 * @see JenisCatatanSiswa
 * @see KelompokParameterTambahanCatatanSiswa
 * @see ParameterTambahanCatatanSiswa
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "sekolah", name = "catatan_siswa")
public class CatatanSiswa extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Dipertahankan tetap agar sesi ZK/objek ter-serialisasi dari rilis
	 * sebelumnya tetap dapat dibaca; JANGAN diubah saat menambah field.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama; dideklarasikan ulang karena {@link GeneralValueObject} tidak dipetakan Hibernate. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini (jejak audit, deklarasi ulang). */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini (jejak audit, deklarasi ulang). */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id pengguna pengubah terakhir.
	 *
	 * <p><b>Perhatian:</b> nilai {@code null} atau string kosong <b>diabaikan secara senyap</b>
	 * (pola warisan {@link GeneralValueObject}) &mdash; jejak audit yang sudah terisi tidak dapat
	 * dikosongkan kembali lewat setter ini, dan pemanggil tidak menerima indikasi apa pun.</p>
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
	 * Menetapkan nama pengguna pengubah terakhir.
	 *
	 * <p><b>Perhatian:</b> sama seperti {@link #setOlehId(String)}, nilai {@code null}/kosong
	 * diabaikan secara senyap.</p>
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null}/kosong
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
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}: dipanggil Hibernate tepat sebelum baris ini di-{@code UPDATE},
	 * meneruskan ke {@code AuditTimestampInterceptor.ubah(this)} untuk mengisi
	 * {@link #getOleh()}/{@link #getOlehId()}/{@link #getTanggal_dirubah()} dari pengguna sesi
	 * aktif.
	 *
	 * <p><b>Efek samping:</b> mengubah state entity. Tidak untuk dipanggil manual dari kode
	 * aplikasi.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu perubahan terakhir. Diinisialisasi ke waktu pembuatan objek sehingga baris
	 * baru tidak pernah bernilai {@code null}, lalu diperbarui {@link #onUpdate()} tiap
	 * {@code UPDATE}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan stempel waktu perubahan terakhir.
	 *
	 * <p>Umumnya diisi otomatis oleh {@link #onUpdate()}; pemanggilan manual biasanya hanya pada
	 * jalur impor/migrasi data.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * @return waktu perubahan terakhir (tidak pernah {@code null} untuk objek yang baru dibuat)
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi ringkas {@code "id-nama"} untuk keperluan log/debug.
	 *
	 * <p><b>Kuirk:</b> membaca <i>field</i> {@code nama} langsung, bukan {@link #getNama()},
	 * sehingga tidak memicu pengisian dari {@link #getSiswa()}; untuk entity yang baru dimuat dari
	 * DB dan belum pernah dibaca lewat getter, bagian nama bisa tampil {@code null}.</p>
	 *
	 * @return string {@code "<id>-<nama tersimpan>"}
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Salinan NISN siswa (didenormalisasi); lihat {@link #getKode()}. */
	private String kode;
	/** Siswa subjek catatan (FK {@code siswa}). */
	private Siswa siswa;
	/** Rombel siswa (FK {@code kelas_siswa}); ditulis ulang tiap pembacaan, lihat {@link #getKelasSiswa()}. */
	private KelasSiswa kelasSiswa;
	/** Guru penulis/pembina catatan (FK {@code guru}). */
	private Guru guru;
	/** Salinan nama siswa (didenormalisasi), BUKAN judul catatan; lihat {@link #getNama()}. */
	private String nama;
	/** Isi/uraian catatan (kolom {@code text}) &mdash; inilah muatan sensitif baris ini. */
	private String keterangan;
	/** Tanggal &amp; jam kejadian yang dicatat. */
	private Date waktu;
	/** Kategori catatan sekaligus penentu formulir parameter tambahan (FK {@code jenis_catatan_siswa}). */
	private JenisCatatanSiswa jenisCatatanSiswa;
	/** Sekolah pemilik data (FK {@code sekolah_id}); diturunkan dari siswa, lihat {@link #getSekolah()}. */
	private Sekolah sekolah;
	/** Yayasan pemilik data (FK {@code yayasan_id}); diturunkan dari sekolah, lihat {@link #getYayasan()}. */
	private Yayasan yayasan;
	/** Serialisasi parameter tambahan versi siap-baca manusia; lihat {@link #getParameterTambahan()}. */
	private String parameterTambahan;
	/** Serialisasi parameter tambahan versi berkunci id; lihat {@link #getParameterTambahanInds()}. */
	private String parameterTambahanInds;
	/** Tahun ajaran catatan, format {@code "YYYY/YYYY"}; default tahun akademik berjalan. */
	private String tahunAjaran;
	/** Semester catatan: {@code 1} = ganjil, {@code 2} = genap; default semester berjalan. */
	private Integer semester;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA. Juga dipakai langsung oleh
	 * {@code CatatanSiswaAction.onAdd()} dan {@code CatatanApi.simpan()} untuk membuat catatan
	 * baru yang masih kosong.
	 */
	public CatatanSiswa() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Nilai dihasilkan database ({@code IDENTITY}); kolom ditandai {@code insertable = false}
	 * sehingga tidak pernah dikirim pada {@code INSERT}. Id ini juga dipakai sebagai
	 * {@code ref} saat mencari {@link LampiranLain} milik catatan.</p>
	 *
	 * @return id catatan, atau {@code null} bila entity belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci utama. Umumnya hanya dipakai Hibernate atau jalur impor data.
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kode catatan &mdash; pada praktiknya <b>NISN siswa</b> yang didenormalisasi.
	 *
	 * <p><b>Getter dengan write-back:</b> bila {@link #getSiswa()} tidak {@code null}, field
	 * {@code kode} DITIMPA dengan {@code getSiswa().getNomorIndukNasional()} setiap kali method ini
	 * dipanggil. Nilai apa pun yang pernah diisi lewat {@link #setKode(String)} akan hilang begitu
	 * baris dibaca, dan karena entity dalam keadaan <i>managed</i> perubahan itu ikut ter-flush ke
	 * DB tanpa aksi simpan eksplisit.</p>
	 * <p>Juga mengubah {@code null} menjadi string kosong, sehingga kolom {@code kode} praktis
	 * tidak pernah bernilai {@code NULL} setelah baris pernah dibaca.</p>
	 *
	 * @return NISN siswa (di-{@code trim}), atau {@code ""} bila belum ada
	 */
	public String getKode() {
		if (getSiswa() != null) {
			kode = getSiswa().getNomorIndukNasional();
		}
		return kode == null ? "" : kode.trim();
	}

	/**
	 * Menetapkan kode catatan.
	 *
	 * <p><b>Perhatian:</b> nilai yang diset akan tertimpa NISN siswa pada pemanggilan
	 * {@link #getKode()} berikutnya selama {@link #getSiswa()} tidak {@code null}.</p>
	 *
	 * @param kode kode catatan
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan isi kolom {@code nama} &mdash; pada praktiknya <b>nama siswa</b>, BUKAN judul
	 * catatan.
	 *
	 * <p><b>Getter dengan write-back:</b> bila {@link #getSiswa()} tidak {@code null}, field
	 * {@code nama} DITIMPA dengan {@code getSiswa().getNama()}. Karena kolom dipetakan
	 * {@code nullable = false}, catatan tanpa siswa DAN tanpa nama yang pernah diset akan gagal
	 * pada {@code INSERT}.</p>
	 * <p>Implikasi lain: {@code CatatanApi.simpan()} mewajibkan klien mengirim
	 * &quot;nama/judul catatan&quot; dan menyimpannya ke sini, tetapi judul tersebut akan hilang
	 * pada pembacaan berikutnya. Judul/isi yang bertahan hanya {@link #getKeterangan()}. Method
	 * ini juga menjadi kunci pengurutan alami efektif (lihat catatan {@code compareTo} pada
	 * dokumentasi kelas) dan dipakai {@code LaporanRaporSiswa} sebagai
	 * {@code Order.asc("nama")}.</p>
	 *
	 * @return nama siswa (di-{@code trim}), atau {@code null} bila belum ada nilai sama sekali
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		if (getSiswa() != null) {
			nama = getSiswa().getNama();
		}
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan isi kolom {@code nama}.
	 *
	 * <p><b>Perhatian:</b> nilai akan tertimpa nama siswa pada pemanggilan {@link #getNama()}
	 * berikutnya selama {@link #getSiswa()} tidak {@code null}.</p>
	 *
	 * @param nama nilai kolom {@code nama}
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan isi/uraian catatan &mdash; muatan utama entity ini.
	 *
	 * <p>Dipetakan sebagai kolom {@code text} (tanpa batas panjang). Inilah teks bebas berisi
	 * pengamatan perilaku/sikap/pembinaan siswa yang ditulis guru, ditampilkan pada grid daftar,
	 * dasbor, rapor, dan dikirim apa adanya oleh {@code CatatanApi}. Perlakukan sebagai data
	 * pribadi sensitif.</p>
	 *
	 * @return isi catatan, atau {@code null} bila tidak diisi
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan isi/uraian catatan.
	 *
	 * @param keterangan teks catatan (boleh {@code null})
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan semester catatan: {@code 1} = ganjil, {@code 2} = genap.
	 *
	 * <p><b>Isi-otomatis saat kosong:</b> bila field masih {@code null}, diisi dari
	 * {@code Common.isNowSemensterGanjil()} &mdash; yakni semester yang sedang berjalan
	 * <i>pada saat pembacaan</i>, bukan pada saat kejadian {@link #getWaktu()}. Untuk baris lama
	 * yang semesternya belum pernah terisi, pembacaan pertama akan melabelinya dengan semester
	 * berjalan saat itu dan nilai tersebut ikut ter-flush.</p>
	 * <p>Dipakai sebagai filter oleh {@code CatatanSiswaAction.initCriteria()} dan sebagai syarat
	 * pengambilan blok catatan pada {@code LaporanRaporSiswa}.</p>
	 *
	 * @return {@code 1} (ganjil) atau {@code 2} (genap)
	 */
	@Column(name = "semester", nullable = true)
	public Integer getSemester() {
		if (semester == null) {
			semester = Common.isNowSemensterGanjil() ? 1 : 2;
		}
		return this.semester;
	}

	/**
	 * Menetapkan semester catatan.
	 *
	 * @param semester {@code 1} untuk ganjil, {@code 2} untuk genap
	 */
	public void setSemester(Integer semester) {
		this.semester = semester;
	}

	/**
	 * Mengembalikan tahun ajaran catatan dalam format {@code "YYYY/YYYY"}.
	 *
	 * <p><b>Isi-otomatis saat kosong:</b> bila field masih {@code null}, diisi
	 * {@code Common.getCurrentTahunAkademik()} &mdash; tahun akademik yang berjalan saat
	 * pembacaan. Berlaku catatan yang sama seperti {@link #getSemester()} untuk baris lama.</p>
	 * <p>Nilai ini juga menjadi argumen {@code ta} pada {@code Siswa.ambilKelas()} di dalam
	 * {@link #getKelasSiswa()}, sehingga tahun ajaran yang salah dapat menyebabkan FK kelas
	 * ditimpa {@code null}.</p>
	 *
	 * @return tahun ajaran, mis. {@code "2025/2026"}
	 */
	@Column(name = "tahun_ajaran", nullable = true, length = 9)
	public String getTahunAjaran() {

		if (tahunAjaran == null) {
			tahunAjaran = Common.getCurrentTahunAkademik();
		}
		return this.tahunAjaran;
	}

	/**
	 * Menetapkan tahun ajaran catatan.
	 *
	 * @param tahunAjaran tahun ajaran format {@code "YYYY/YYYY"}
	 */
	public void setTahunAjaran(String tahunAjaran) {
		this.tahunAjaran = tahunAjaran;
	}

	/**
	 * Mengembalikan siswa subjek catatan ini.
	 *
	 * <p>Relasi {@code @ManyToOne} malas (FK {@code siswa}). Nilai dilewatkan
	 * {@link GeneralValueObject#check(Object)} lebih dulu untuk memulihkan proxy yang sesinya sudah
	 * tertutup, sehingga aman dipanggil dari renderer ZK di luar transaksi asal.</p>
	 * <p>Getter ini menjadi sumber turunan bagi {@link #getKode()}, {@link #getNama()},
	 * {@link #getSekolah()}, dan {@link #getKelasSiswa()}.</p>
	 *
	 * @return siswa subjek catatan, atau {@code null} bila belum dipilih
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa")
	public Siswa getSiswa() {
		siswa = check(siswa);
		return siswa;
	}

	/**
	 * Menetapkan siswa subjek catatan.
	 *
	 * <p><b>Efek lanjutan:</b> mengganti siswa akan ikut mengubah nilai yang dikembalikan
	 * {@link #getKode()}, {@link #getNama()}, {@link #getSekolah()}, {@link #getYayasan()}, dan
	 * {@link #getKelasSiswa()} pada pembacaan berikutnya &mdash; termasuk menulis ulang kolom
	 * tenant baris ini.</p>
	 *
	 * @param siswa siswa subjek catatan
	 */
	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
	}

	/**
	 * Mengembalikan tanggal &amp; jam kejadian yang dicatat.
	 *
	 * <p><b>Kuirk:</b> bila field masih {@code null}, method mengembalikan
	 * {@code WaktuUtil.getDate()} (waktu sekarang) tetapi <b>tidak</b> menuliskannya ke field.
	 * Karena pemetaan Hibernate kelas ini berbasis properti, nilai &quot;sekarang&quot; itulah yang
	 * dibaca saat {@code INSERT}/{@code UPDATE} &mdash; jadi kolom tetap tersimpan terisi, namun
	 * dua pembacaan berturut-turut pada objek yang sama dapat mengembalikan waktu yang berbeda
	 * selama field belum diset.</p>
	 *
	 * @return waktu kejadian catatan; waktu sekarang bila belum pernah diisi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktu() {
		return waktu == null ? WaktuUtil.getDate() : waktu;
	}

	/**
	 * Menetapkan tanggal &amp; jam kejadian catatan.
	 *
	 * @param waktu waktu kejadian
	 */
	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

	/**
	 * Mengembalikan jenis/kategori catatan.
	 *
	 * <p>Relasi {@code @ManyToOne} malas (FK {@code jenis_catatan_siswa}), dipulihkan lewat
	 * {@link GeneralValueObject#check(Object)}. Selain menjadi label kategori, baris
	 * {@link JenisCatatanSiswa} inilah yang <b>menentukan formulir parameter tambahan</b> yang
	 * dirender: hanya {@link KelompokParameterTambahanCatatanSiswa} yang dicentang padanya yang
	 * muncul sebagai isian.</p>
	 *
	 * @return jenis catatan, atau {@code null} bila belum dipilih
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_catatan_siswa")
	public JenisCatatanSiswa getJenisCatatanSiswa() {
		jenisCatatanSiswa = check(jenisCatatanSiswa);
		return jenisCatatanSiswa;
	}

	/**
	 * Menetapkan jenis/kategori catatan.
	 *
	 * <p><b>Perhatian:</b> mengganti jenis setelah parameter tambahan terisi TIDAK membersihkan
	 * {@link #getParameterTambahan()}/{@link #getParameterTambahanInds()}; nilai lama tetap
	 * tersimpan dan bisa menjadi baris &quot;yatim&quot; yang tidak lagi punya definisi field
	 * padanan.</p>
	 *
	 * @param jenisCatatanSiswa jenis catatan
	 */
	public void setJenisCatatanSiswa(JenisCatatanSiswa jenisCatatanSiswa) {
		this.jenisCatatanSiswa = jenisCatatanSiswa;
	}

	/**
	 * Mengembalikan guru penulis/pembina catatan.
	 *
	 * <p>Relasi {@code @ManyToOne} malas (FK {@code guru}), dipulihkan lewat
	 * {@link GeneralValueObject#check(Object)}. Dipakai sebagai filter pencarian &quot;Guru&quot;
	 * pada {@code CatatanSiswaAction.initCriteria()} (dikombinasikan {@code OR} dengan guru
	 * pembina kelas) dan sebagai pembatas dasbor untuk akun guru pada
	 * {@code DasbordCatatan.muatCatatanSiswa()}.</p>
	 *
	 * @return guru penulis catatan, atau {@code null} bila belum dipilih
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "guru")
	public Guru getGuru() {
		guru = check(guru);
		return guru;
	}

	/**
	 * Menetapkan guru penulis/pembina catatan.
	 *
	 * @param guru guru penulis catatan
	 */
	public void setGuru(Guru guru) {
		this.guru = guru;
	}

	/**
	 * Mengembalikan sekolah pemilik catatan (kolom cakupan multi-tenant, FK {@code sekolah_id}).
	 *
	 * <p><b>Getter dengan write-back:</b> bila {@link #getSiswa()} tidak {@code null}, field
	 * {@code sekolah} DITIMPA dengan {@code getSiswa().getSekolah()}; hanya bila siswa
	 * {@code null} nilai tersimpan dipertahankan (lewat {@link GeneralValueObject#check(Object)}).
	 * Artinya sekolah catatan selalu mengikuti sekolah siswa <i>saat ini</i>: memindahkan siswa
	 * antarsekolah dalam satu yayasan akan secara retroaktif memindahkan seluruh catatan lamanya,
	 * dan perubahan itu ikut ter-flush ke DB tanpa aksi simpan eksplisit.</p>
	 *
	 * @return sekolah pemilik catatan, atau {@code null} bila tidak dapat ditentukan
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id")
	public Sekolah getSekolah() {
		if (getSiswa() != null) {
			sekolah = getSiswa().getSekolah();
		} else {
			sekolah = check(sekolah);
		}
		return this.sekolah;
	}

	/**
	 * Menetapkan sekolah pemilik catatan.
	 *
	 * <p>Objek {@link Sekolah} yang ber-{@code id} {@code null} (mis. pilihan
	 * &quot;Semua&quot; pada combobox) dinormalkan menjadi {@code null} agar tidak tersimpan
	 * sebagai entity transien. Nilai yang diset tetap dapat tertimpa oleh
	 * {@link #getSekolah()} bila catatan punya siswa.</p>
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau ber-id {@code null} disimpan sebagai {@code null}
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah == null || sekolah.getId() == null ? null : sekolah;
	}

	/**
	 * Mengembalikan yayasan pemilik catatan (kolom cakupan multi-tenant, FK {@code yayasan_id}).
	 *
	 * <p><b>Getter dengan write-back berantai:</b> method memanggil {@link #getSekolah()} lebih
	 * dulu (yang sendirinya sudah menulis balik field {@code sekolah}), lalu bila sekolah tidak
	 * {@code null} field {@code yayasan} DITIMPA dengan {@code sekolah.getYayasan()}. Jadi satu
	 * pembacaan yayasan berpotensi menulis ulang DUA kolom tenant sekaligus.</p>
	 *
	 * @return yayasan pemilik catatan, atau {@code null} bila tidak dapat ditentukan
	 */
	@ManyToOne(fetch = FetchType.LAZY)
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
	 * Menetapkan yayasan pemilik catatan.
	 *
	 * <p>Sama seperti {@link #setSekolah(Sekolah)}, objek ber-{@code id} {@code null} dinormalkan
	 * menjadi {@code null}. Nilai yang diset tetap dapat tertimpa oleh {@link #getYayasan()}.</p>
	 *
	 * @param yayasan yayasan pemilik; {@code null} atau ber-id {@code null} disimpan sebagai {@code null}
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan == null || yayasan.getId() == null ? null : yayasan;
	}

	/**
	 * Mengembalikan serialisasi parameter tambahan versi <b>berkunci id</b> &mdash; bentuk yang
	 * dipakai untuk memuat ulang nilai ke dalam form.
	 *
	 * <p>Format per baris (antarbaris dipisah {@code "\n"}, antar-ruas {@code "<=>"}):
	 * <code>idKelompok-&gt;idParameter&lt;=&gt;nilai&lt;=&gt;urlLampiran&lt;=&gt;keterangan</code>.
	 * Karena kuncinya berupa id (bukan label), bentuk ini tahan terhadap penggantian nama
	 * kelompok/parameter. Dipakai antara lain oleh renderer daftar
	 * {@code CatatanSiswaAction.CatatanSiswaRenderer} (link &quot;Lihat Parameter Tambahan&quot;),
	 * {@code ParameterTambahanCatatanSiswaListener}, dan dikirim apa adanya oleh
	 * {@code CatatanApi}.</p>
	 * <p>Mengembalikan string kosong (bukan {@code null}) bila belum pernah diisi, dan menuliskan
	 * {@code ""} itu ke field.</p>
	 *
	 * @return serialisasi parameter tambahan berkunci id; {@code ""} bila kosong
	 */
	@Column(columnDefinition = "text")
	public String getParameterTambahanInds() {
		if (parameterTambahanInds == null) {
			parameterTambahanInds = "";
		}

		return parameterTambahanInds;
	}

	/**
	 * Menetapkan serialisasi parameter tambahan versi berkunci id.
	 *
	 * <p>Biasanya diisi oleh {@link #populateParameterTambahan(List)}; pengisian manual (mis. dari
	 * {@code CatatanApi.simpan()}) harus mengikuti format yang dijelaskan pada
	 * {@link #getParameterTambahanInds()} karena tidak ada validasi bentuk di sini.</p>
	 *
	 * @param parameterTambahanInds serialisasi berkunci id
	 */
	public void setParameterTambahanInds(String parameterTambahanInds) {
		this.parameterTambahanInds = parameterTambahanInds;
	}

	/**
	 * Mengurai {@link #getParameterTambahan()} menjadi daftar {@link CommonVO} siap tampil,
	 * terurut menurut {@code nomorUrut}.
	 *
	 * <p>Setiap baris teks dipecah dengan pemisah {@code "<=>"} dan dipetakan ke {@link CommonVO}
	 * sebagai berikut:</p>
	 * <ul>
	 *   <li>ruas 0 &rarr; {@code name} (label lengkap {@code "namaKelompok->labelInputan"}) dan,
	 *       setelah dipecah lagi dengan {@code "->"}, bagian pertamanya &rarr; {@code name5}
	 *       (nama kelompok);</li>
	 *   <li>ruas 1 &rarr; {@code name1} (nilai isian);</li>
	 *   <li>ruas 2 &rarr; {@code name2} (URL lampiran);</li>
	 *   <li>ruas 3 &rarr; {@code nomorUrut} (default {@code 1} bila gagal diurai);</li>
	 *   <li>ruas 4 &rarr; {@code id} (id {@link ParameterTambahan}, <b>bukan</b> id catatan;
	 *       default {@code 1} bila gagal diurai).</li>
	 * </ul>
	 * <p>Kegagalan penguraian angka pada ruas 3/4 ditelan (dicatat ke {@code ErrorAuditUtil}) dan
	 * diganti nilai default, sehingga method ini tidak pernah melempar karena data rusak.</p>
	 *
	 * <p><b>Kuirk 1 &mdash; tidak pernah mengembalikan daftar kosong.</b>
	 * {@link #getParameterTambahan()} mengembalikan {@code ""} saat kosong, dan
	 * {@code "".split("\n")} menghasilkan larik berisi satu string kosong. Akibatnya catatan yang
	 * sama sekali tidak punya parameter tambahan tetap menghasilkan SATU {@link CommonVO} dengan
	 * label/nilai/URL kosong dan {@code id = 1}. Pemanggil wajib menyaring entri kosong sendiri.</p>
	 * <p><b>Kuirk 2 &mdash; kode mati.</b> Tidak ada pemanggil method ini untuk kelas
	 * {@code CatatanSiswa} di seluruh repositori; yang benar-benar dipanggil adalah method senama
	 * milik entity lain ({@code KegiatanSiswa}, {@code IsiAngketParameterUmum}) dari
	 * {@code DashboardRekapKegiatanSiswaData}/{@code DashboardRekapParameterTambahanUmumData}.
	 * Method ini dipertahankan demi keseragaman kontrak antar-entity bercorak &quot;catatan&quot;.</p>
	 * <p><b>Kuirk 3.</b> Ekspresi {@code splNama.length > j ? splNama[j] : ""} selalu bernilai
	 * benar karena {@code j} adalah indeks perulangan atas larik itu sendiri &mdash; cabang
	 * {@code ""} tidak pernah tercapai.</p>
	 *
	 * @return daftar {@link CommonVO} terurut menurut {@code nomorUrut}; minimal berisi satu entri
	 *         (kosong) meskipun catatan tidak punya parameter tambahan
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
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/CatatanSiswa.java:278");

			}
			Long id = 1L;
			try {
				id = value.length > 4 ? Long.parseLong(value[4].trim()) : 1L;
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/CatatanSiswa.java:284");

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
	 * Memanen nilai seluruh baris input parameter tambahan dari form ZK, lalu menuliskannya ke
	 * kolom {@link #getParameterTambahan()} dan {@link #getParameterTambahanInds()} entity ini.
	 *
	 * <p><b>Dipanggil dari:</b> {@code ParameterTambahanCatatanSiswaListener.onSave(CatatanSiswa)},
	 * yang sendirinya dipanggil {@code CatatanSiswaAction.onSave()} SETELAH validasi field wajib
	 * lulus dan SEBELUM {@code session.save()}/{@code Common.refreshUpdate()}. Jadi method ini
	 * adalah jembatan tunggal dari komponen ZK ke kolom teks terdenormalisasi.</p>
	 *
	 * <p><b>Cara kerja.</b> Untuk setiap {@link Row} pada {@code parameterRows}, method membaca
	 * tiga atribut yang sebelumnya dipasang listener: {@code "parameterTambahan"}
	 * ({@link ParameterTambahan}), {@code "kelompokParameterTambahanCatatanSiswa"}
	 * ({@link KelompokParameterTambahanCatatanSiswa}), dan {@code "keterangan"} (sebuah
	 * {@link Textbox} opsional). Nilai isian diambil lewat
	 * {@code ParameterTambahan.ambilVal(row, parameterTambahan)}. Bila definisi parameter
	 * mewajibkan lampiran ({@code getHarusMenyertakanLampiran()}), method mencari
	 * {@link LampiranLain} milik catatan ini dengan kunci
	 * {@code (ref = getId(), jenis = "{idKelompok}->{idParameter}")} dan menyertakan
	 * {@code createLinkUri()}-nya sebagai ruas URL.</p>
	 *
	 * <p>Dua string dibangun sekaligus lalu ditulis lewat
	 * {@link #setParameterTambahanInds(String)} dan {@link #setParameterTambahan(String)}:</p>
	 * <ul>
	 *   <li>versi siap-baca:
	 *       <code>namaKelompok-&gt;labelInputan&lt;=&gt;nilai&lt;=&gt;url&lt;=&gt;nomorUrut&lt;=&gt;idParameter&lt;=&gt;idKelompok&lt;=&gt;keterangan</code>;</li>
	 *   <li>versi berkunci id:
	 *       <code>idKelompok-&gt;idParameter&lt;=&gt;nilai&lt;=&gt;url&lt;=&gt;keterangan</code>.</li>
	 * </ul>
	 *
	 * <p><b>Efek samping:</b> mengubah dua field entity (dan karenanya dua kolom DB pada flush
	 * berikutnya). Tidak membuka/menutup sesi Hibernate sendiri, tetapi {@code LampiranLain.ambil}
	 * dan {@code createLinkUri()} mengakses persistence &mdash; panggil dari event thread ZK dengan
	 * sesi aktif.</p>
	 *
	 * <p><b>Kuirk penting.</b></p>
	 * <ul>
	 *   <li><b>Tidak melakukan apa-apa bila daftar baris kosong atau {@code null}</b> &mdash;
	 *       method langsung {@code return} TANPA mengosongkan kolom. Jadi bila pengguna mengganti
	 *       jenis catatan ke jenis yang tidak punya parameter apa pun, nilai parameter lama
	 *       <b>tetap tersimpan</b> di DB dan menjadi data yatim.</li>
	 *   <li><b>Penulisan bersifat mengganti total</b> untuk baris yang memang dirender: string
	 *       dibangun dari nol, sehingga parameter yang barisnya tidak ikut ditampilkan pada sesi
	 *       edit ini akan hilang dari kolom.</li>
	 *   <li><b>Kegagalan per-baris ditelan</b> lewat {@code Common.tampilErrorJikaAdmin(e)}:
	 *       hanya admin yang melihat pesan; bagi pengguna biasa satu baris parameter bisa hilang
	 *       secara senyap tanpa proses simpan gagal.</li>
	 *   <li><b>Pencarian lampiran memakai {@link #getId()}</b>, sehingga pada catatan BARU (id
	 *       masih {@code null}) ruas URL selalu kosong. Inilah alasan
	 *       {@code CatatanSiswaAction.onSave()} menautkan ulang {@link LampiranLain} ke
	 *       {@code catatanSiswa.getId()} sesudah entity tersimpan.</li>
	 * </ul>
	 *
	 * @param parameterRows daftar baris ZK hasil render {@code ParameterTambahanCatatanSiswaListener};
	 *                      bila {@code null} atau kosong, method tidak melakukan apa pun
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
				KelompokParameterTambahanCatatanSiswa kelompokParameterTambahanCatatanSiswa = (KelompokParameterTambahanCatatanSiswa) row
						.getAttribute("kelompokParameterTambahanCatatanSiswa");
				if (parameterTambahan != null && kelompokParameterTambahanCatatanSiswa != null) {
					String jenis = kelompokParameterTambahanCatatanSiswa.getId() + "->" + parameterTambahan.getId();

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

					String s = kelompokParameterTambahanCatatanSiswa.getNama() + "->"
							+ parameterTambahan.getLabelInputan() + "<=>" + val + "<=>" + url + "<=>"
							+ parameterTambahan.getNomorUrut() + "<=>" + parameterTambahan.getId() + "<=>"
							+ kelompokParameterTambahanCatatanSiswa.getId() + "<=>"
							+ (keterangan == null ? "" : keterangan.getValue().trim());

					parameterTambahanStr += parameterTambahanStr.isEmpty() ? s : "\n" + s;

					String sIds = kelompokParameterTambahanCatatanSiswa.getId() + "->" + parameterTambahan.getId()
							+ "<=>" + val + "<=>" + url + "<=>"
							+ (keterangan == null ? "" : keterangan.getValue().trim());
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
	 * Mengembalikan serialisasi parameter tambahan versi <b>siap-baca manusia</b>.
	 *
	 * <p>Format per baris (antarbaris dipisah {@code "\n"}, antar-ruas {@code "<=>"}):
	 * <code>namaKelompok-&gt;labelInputan&lt;=&gt;nilai&lt;=&gt;urlLampiran&lt;=&gt;nomorUrut&lt;=&gt;idParameter&lt;=&gt;idKelompok&lt;=&gt;keterangan</code>.
	 * Karena label ikut disalin, mengganti nama kelompok/parameter di master TIDAK memperbarui
	 * catatan lama &mdash; nilai di sini adalah cuplikan (<i>snapshot</i>) saat catatan disimpan.</p>
	 * <p>Dibaca oleh {@link #ambilDataParameterTambahan()}, oleh {@code DasbordCatatan} (ditampilkan
	 * pada baris dasbor), dan dikirim apa adanya oleh {@code CatatanApi.detail()}. Ikut serta pula
	 * pada daftar kolom ekspor &quot;Cetak Data&quot; di {@code CatatanSiswaAction}.</p>
	 * <p>Mengembalikan string kosong (bukan {@code null}) bila belum pernah diisi, dan menuliskan
	 * {@code ""} itu ke field.</p>
	 *
	 * @return serialisasi parameter tambahan siap-baca; {@code ""} bila kosong
	 */
	@Column(columnDefinition = "text")
	public String getParameterTambahan() {
		if (parameterTambahan == null) {
			parameterTambahan = "";
		}

		return parameterTambahan;
	}

	/**
	 * Menetapkan serialisasi parameter tambahan versi siap-baca.
	 *
	 * <p>Biasanya diisi oleh {@link #populateParameterTambahan(List)}; pengisian manual harus
	 * mengikuti format pada {@link #getParameterTambahan()} karena tidak ada validasi bentuk.</p>
	 *
	 * @param parameterTambahan serialisasi siap-baca
	 */
	public void setParameterTambahan(String parameterTambahan) {
		this.parameterTambahan = parameterTambahan;
	}

	/**
	 * Mengembalikan rombel (kelas) siswa untuk catatan ini.
	 *
	 * <p><b>GETTER PALING DESTRUKTIF PADA KELAS INI.</b> Alurnya: nilai tersimpan dipulihkan lebih
	 * dulu lewat {@link GeneralValueObject#check(Object)}, TETAPI bila {@link #getSiswa()} tidak
	 * {@code null} nilai itu langsung DIBUANG dan diganti hasil
	 * {@code Siswa.ambilKelas(getSiswa(), getTahunAjaran())}. Karena entity dalam keadaan
	 * <i>managed</i>, sekadar merender baris ini (grid daftar, dasbor, laporan) sudah cukup untuk
	 * menulis ulang kolom {@code kelas_siswa} di DB tanpa aksi simpan eksplisit.</p>
	 * <p>Dua konsekuensi nyata:</p>
	 * <ul>
	 *   <li>Setelah siswa naik kelas/pindah rombel, catatan lama ikut &quot;pindah&quot; ke rombel
	 *       terbaru &mdash; informasi &quot;catatan ini dibuat saat siswa di kelas apa&quot; hilang
	 *       permanen.</li>
	 *   <li>Bila {@code ambilKelas} tidak menemukan rombel yang cocok untuk tahun ajaran tersebut
	 *       (mis. roster {@code KelasSiswaPunyaSiswa} tahun itu sudah dibersihkan), ia
	 *       mengembalikan {@code null} dan FK kelas DIHAPUS permanen. Sesudah itu kolom filter
	 *       &quot;guru pembina kelas&quot; pada {@code CatatanSiswaAction.initCriteria()} (yang
	 *       mengandalkan alias {@code kelasSiswa}) tidak lagi menemukan baris tersebut.</li>
	 * </ul>
	 *
	 * @return rombel siswa untuk tahun ajaran catatan, atau {@code null} bila tidak ditemukan
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "kelas_siswa")
	public KelasSiswa getKelasSiswa() {
		kelasSiswa = check(kelasSiswa);
		if (getSiswa() != null) {
			kelasSiswa = Siswa.ambilKelas(getSiswa(), getTahunAjaran());
		}
		return kelasSiswa;
	}

	/**
	 * Menetapkan rombel (kelas) siswa untuk catatan ini.
	 *
	 * <p><b>Perhatian:</b> nilai yang diset akan dibuang pada pemanggilan
	 * {@link #getKelasSiswa()} berikutnya selama catatan punya siswa &mdash; setter ini hanya
	 * efektif untuk catatan tanpa siswa.</p>
	 *
	 * @param kelasSiswa rombel siswa
	 */
	public void setKelasSiswa(KelasSiswa kelasSiswa) {
		this.kelasSiswa = kelasSiswa;
	}
}
