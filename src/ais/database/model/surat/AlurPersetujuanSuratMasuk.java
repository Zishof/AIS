package ais.database.model.surat;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;
import java.util.Set;
import java.util.TreeSet;

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
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.employ.JenisJabatan;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;

/**
 * Entitas <b>definisi</b> satu simpul (tahapan/jenjang) pada alur persetujuan &mdash; sering disebut
 * "alur disposisi" &mdash; untuk <b>surat masuk</b>. Dipetakan ke tabel
 * {@code surat.alur_persetujuan_surat_masuk}.
 *
 * <h2>Peran entitas ini dalam modul persuratan</h2>
 * Modul persuratan AIS memisahkan dua hal yang mudah tertukar:
 * <ol>
 * <li><b>Definisi alur</b> (kelas ini): "template" jenjang persetujuan &mdash; siapa (dalam arti
 * {@link JenisJabatan}, bukan orang tertentu) harus menyetujui, dan pada urutan/tingkat keberapa.
 * Satu baris kelas ini adalah satu simpul pohon; hubungan urutan dibentuk lewat {@link #getParent()}
 * (self-referencing tree), sehingga "jenjang berikutnya" dari sebuah simpul adalah seluruh baris
 * yang {@code parent}-nya menunjuk ke simpul tersebut. Pohon ini dikelola lewat
 * {@code AlurPersetujuanSuratMasukAction} dan {@code AlurPersetujuanSuratMasukTreeAction}.</li>
 * <li><b>Status/riwayat alur</b> ({@link AlurPersetujuanSuratMasukStatus}): realisasi aktual per
 * surat per pejabat &mdash; menyimpan flag {@code disetujui}/{@code ditolak}, waktu keputusan,
 * catatan, dan pejabat yang mengeksekusi. Baris status dibuat "malas" (lazy) saat jenjang
 * sebelumnya disetujui, bukan dibuat sekaligus di awal.</li>
 * </ol>
 * Dengan kata lain kelas ini adalah <i>skema</i>, sedangkan {@code *Status} adalah <i>jejak
 * eksekusi</i>. Membaca kelas ini saja TIDAK pernah memberi tahu apakah sebuah surat sudah disetujui.
 *
 * <h2>Bagaimana simpul ini dipakai saat runtime</h2>
 * Pemakai utamanya:
 * <ul>
 * <li>{@code ais.action.servlet.api.SuratApi#disposisi_surat_masuk} &mdash; setelah satu baris
 * status ditandai disetujui, seluruh {@code AlurPersetujuanSuratMasuk} yang {@code parent}-nya sama
 * dengan simpul baris status tersebut <b>dan</b> memiliki {@link #getJenisJabatan()} tidak
 * {@code null} dicari; untuk tiap jenis jabatan itu dicari daftar {@link ais.database.model.rab.Pejabat}
 * dan dibuatkan baris status baru.</li>
 * <li>{@code ais.action.master.surat.SuratMasukAction} &mdash; membentuk baris status jenjang
 * pertama saat surat disimpan.</li>
 * <li>{@code ais.action.master.surat.helper.DasboardSurat} dan
 * {@code ais.action.master.surat.helper.DasboardAlurSurat} &mdash; merender rantai alur pada dasbor.</li>
 * </ul>
 * Perhatikan konsekuensi penyaringan {@code isNotNull("jenisJabatan")} di {@code SuratApi}: simpul
 * yang hanya mengisi {@link #getJenisJabatans()} (koleksi many-to-many) tanpa mengisi
 * {@link #getJenisJabatan()} (relasi tunggal) TIDAK akan pernah memunculkan jenjang berikutnya lewat
 * jalur API mobile. Kedua field itu bukan sinonim dan tidak saling mengisi.
 *
 * <h2>Cakupan/tenant</h2>
 * Simpul alur dapat dilingkupi ke {@link Yayasan}, {@link Sekolah}, {@link SatuanKerja},
 * {@link Fakultas}, atau {@link Jurusan}. Semua bersifat opsional ({@code nullable = true}) dan
 * TIDAK ada satupun yang divalidasi wajib pada level entitas; penyaringan berdasarkan cakupan
 * sepenuhnya diserahkan ke query di lapisan Action. Baris dengan seluruh field cakupan {@code null}
 * bersifat global dan akan terlihat/terpakai lintas tenant.
 *
 * <h2>Catatan arsitektur yang perlu diwaspadai pembaca</h2>
 * <ul>
 * <li><b>Getter destruktif.</b> Hampir seluruh getter relasi memanggil
 * {@link GeneralValueObject#check(Object)} dan <i>menulis balik</i> hasilnya ke field. Getter di
 * kelas ini karena itu bukan operasi baca murni; memanggilnya dapat mengubah state objek (lihat
 * {@link #getParent()} yang bahkan dapat mengosongkan {@code parent}).</li>
 * <li><b>Dua bendera "aktif".</b> {@link #getAktif()} dan {@link #getDefaultItem()} sama-sama
 * bertipe {@code Boolean} dengan default {@code true}, namun {@code AlurPersetujuanSuratMasukTreeAction}
 * merender kolom berlabel "Aktif" dari {@code defaultItem}, bukan dari {@code aktif}. Jangan asumsikan
 * keduanya sinkron.</li>
 * <li><b>Field audit bayangan.</b> {@link #getOleh()}, {@link #getOlehId()}, dan
 * {@link #getTanggal_dirubah()} diisi oleh {@code AuditTimestampInterceptor} &mdash; keharusan teknis
 * infrastruktur audit, bukan duplikasi yang perlu "dibersihkan". Setter {@code oleh}/{@code olehId}
 * sengaja menolak nilai kosong agar jejak lama tidak terhapus oleh binding form yang kosong.</li>
 * <li><b>Nama tabel gabungan yang menyesatkan.</b> {@link #getJenisJabatans()} pada kelas SURAT
 * MASUK memakai tabel gabungan {@code surat.alur_keluar_punya_jenis_jabatan} &mdash; mengandung kata
 * "keluar". Ini tetap tabel yang berbeda dari milik {@link AlurPersetujuanSuratKeluar}
 * ({@code surat.alur_punya_jenis_jabatan}), jadi tidak ada tabrakan data, tetapi penamaannya
 * berlawanan intuisi saat menelusuri skema database.</li>
 * <li><b>Field tidur.</b> {@link #getJmlDipakai()} dan {@link #getDeep()} tidak memiliki pemanggil
 * nyata di luar kelas ini (berbeda dengan entitas pohon lain yang mengisi {@code deep} lewat
 * {@code *TreeModel}); keduanya tetap dipertahankan demi kompatibilitas skema.</li>
 * </ul>
 *
 * <h2>Peringatan keamanan (integritas persetujuan)</h2>
 * Definisi alur di kelas ini bersifat <b>deklaratif saja</b>. Tidak ada satupun method di sini yang
 * menegakkan urutan jenjang, dan tidak ada mekanisme di entitas ini yang mencegah sebuah baris
 * {@link AlurPersetujuanSuratMasukStatus} ditandai disetujui tanpa jenjang induknya disetujui lebih
 * dulu. Penegakan urutan sepenuhnya bergantung pada lapisan Action/API, yang saat ini tidak
 * melakukannya (lihat catatan pada {@link AlurPersetujuanSuratMasukStatus}).
 *
 * @see AlurPersetujuanSuratMasukStatus
 * @see AlurPersetujuanSuratKeluar
 * @see SuratMasuk
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "surat", name = "alur_persetujuan_surat_masuk")
public class AlurPersetujuanSuratMasuk extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya sengaja disamakan dengan entitas alur persuratan lain
	 * (hasil salin-tempel generator), sehingga TIDAK dapat dipakai untuk membedakan tipe.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama (identity, di-generate database). */
	private Long id;

	/** Nama/identitas pengguna terakhir yang mengubah baris ini; diisi interseptor audit. */
	private String oleh;

	/** Id pengguna terakhir yang mengubah baris ini; diisi interseptor audit. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang menyentuh baris ini (field audit bayangan).
	 *
	 * @return id pengguna, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir. Nilai {@code null} atau kosong <b>diabaikan</b>
	 * (bukan disimpan) agar jejak audit yang sudah ada tidak terhapus oleh binding form yang
	 * mengirim string kosong.
	 *
	 * @param olehId id pengguna; {@code null}/kosong tidak berpengaruh
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Representasi teks simpul alur untuk komponen daftar/kombo ZK: nama simpul diikuti nama jenis
	 * jabatan dalam kurung bila ada.
	 * <p>
	 * Perhatikan bahwa method ini memanggil {@link #getJenisJabatan()} dan <i>menulis balik</i>
	 * hasilnya ke field {@code jenisJabatan}; jadi {@code toString()} di kelas ini punya efek
	 * samping (dapat memicu inisialisasi proxy Hibernate dan mengganti isi field).
	 *
	 * @return teks tampilan simpul alur
	 */
	public String toString() {
		jenisJabatan = getJenisJabatan();
		return nama + (jenisJabatan == null ? "" : " (" + jenisJabatan.getNama() + ") ");
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir; nilai {@code null}/kosong diabaikan dengan alasan
	 * yang sama seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang menyentuh baris ini (field audit bayangan).
	 *
	 * @return nama pengguna, atau {@code null}
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}: mendelegasikan pengisian stempel audit ke
	 * {@code AuditTimestampInterceptor.ubah(this)} tepat sebelum {@code UPDATE} dieksekusi.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu perubahan terakhir. Diinisialisasi ke waktu pembuatan objek agar baris baru
	 * tidak pernah memiliki kolom {@code NULL}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir (dipakai interseptor audit).
	 *
	 * @param tanggal_dirubah waktu perubahan
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir.
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Nama simpul alur (wajib, maksimum 255 karakter). */
	private String nama;

	/** Keterangan bebas mengenai simpul alur. */
	private String keterangan;
	// private SatuanKerja satuanKerja;

	/**
	 * Jenis jabatan tunggal yang menjadi "pemilik" jenjang ini. Field inilah yang dipakai mesin
	 * alur ({@code SuratApi}, {@code SuratMasukAction}) untuk mencari pejabat jenjang berikutnya.
	 */
	private JenisJabatan jenisJabatan;

	/**
	 * Koleksi jenis jabatan tambahan yang boleh menangani jenjang ini (dipakai form pemilihan
	 * multi-jabatan di {@code AlurPersetujuanSuratMasukAction}). Terpisah dari {@link #jenisJabatan}
	 * dan tidak dipakai oleh mesin pembentuk jenjang berikutnya.
	 */
	private Set<JenisJabatan> jenisJabatans = new TreeSet<JenisJabatan>();

	/**
	 * Mengembalikan himpunan jenis jabatan tambahan untuk simpul alur ini.
	 * <p>
	 * Relasi many-to-many ini dipetakan ke tabel gabungan {@code surat.alur_keluar_punya_jenis_jabatan}
	 * &mdash; perhatikan bahwa nama tabelnya mengandung kata "keluar" meskipun kelas ini melayani
	 * surat MASUK. Tabel tersebut tetap berbeda dari tabel gabungan milik
	 * {@link AlurPersetujuanSuratKeluar} ({@code surat.alur_punya_jenis_jabatan}), sehingga data
	 * kedua modul tidak bercampur; namun penamaan ini kerap menyesatkan saat menelusuri skema.
	 * <p>
	 * Koleksi dikembalikan sebagai referensi langsung (bukan salinan), sehingga pemanggil dapat
	 * memodifikasinya dan perubahan akan ikut tersimpan lewat {@code CascadeType.MERGE}. Wadahnya
	 * {@link TreeSet}, jadi elemen {@link JenisJabatan} harus memiliki urutan alami yang konsisten;
	 * elemen {@code null} akan melempar {@code NullPointerException} saat dimasukkan.
	 * <p>
	 * <b>Penting untuk alur persetujuan:</b> mesin alur pada {@code SuratApi.disposisi_surat_masuk}
	 * mencari jenjang berikutnya dengan {@code Restrictions.isNotNull("jenisJabatan")} &mdash;
	 * yakni memakai relasi tunggal {@link #getJenisJabatan()}, BUKAN koleksi ini. Simpul yang hanya
	 * mengisi koleksi ini akan tampil rapi di form namun tidak pernah menghasilkan baris status
	 * jenjang berikutnya, sehingga rantai persetujuan berhenti secara diam-diam.
	 *
	 * @return himpunan jenis jabatan tambahan (tidak pernah {@code null})
	 */
	@ManyToMany(targetEntity = JenisJabatan.class, cascade = { CascadeType.MERGE })
	@JoinTable(name = "alur_keluar_punya_jenis_jabatan", joinColumns = @JoinColumn(name = "alur"), inverseJoinColumns = @JoinColumn(name = "jenis_jabatan"), schema = "surat")
	public Set<JenisJabatan> getJenisJabatans() {
		return jenisJabatans;
	}

	/**
	 * Mengganti himpunan jenis jabatan tambahan. Nilai {@code null} diterima apa adanya dan akan
	 * membuat {@link #getJenisJabatans()} mengembalikan {@code null} (tidak ada penjagaan).
	 *
	 * @param jenisJabatans himpunan pengganti
	 */
	public void setJenisJabatans(Set<JenisJabatan> jenisJabatans) {
		this.jenisJabatans = jenisJabatans;
	}

	/** Simpul induk pada pohon alur; {@code null} berarti simpul akar (jenjang pertama). */
	private AlurPersetujuanSuratMasuk parent;

	/** Penanda item bawaan; di layar pohon dirender sebagai kolom berlabel "Aktif". */
	private Boolean defaultItem;

	/** Kedalaman simpul pada pohon. Tidak ada pengisi otomatis di modul surat (field tidur). */
	private Integer deep;

	/** Pencacah pemakaian simpul. Tidak memiliki pemanggil nyata di codebase (field tidur). */
	private Long jmlDipakai = 0L;

	/** Tautan silang ke definisi alur surat keluar (memicu alur balasan/tindak lanjut). */
	private AlurPersetujuanSuratKeluar alurPersetujuanSuratKeluar;

	/** Klasifikasi surat keluar yang dipakai saat simpul ini menurunkan surat keluar balasan. */
	private KlasifikasiSuratKeluar klasifikasiSuratKeluar;

	/** Cakupan yayasan (opsional). */
	private Yayasan yayasan;

	/** Cakupan sekolah (opsional). */
	private Sekolah sekolah;

	/** Cakupan satuan kerja (opsional). */
	private SatuanKerja satuanKerja;

	/** Cakupan fakultas (opsional). */
	private Fakultas fakultas;

	/** Cakupan jurusan/program studi (opsional). */
	private Jurusan jurusan;

	/** Bendera aktif simpul; {@code null} diperlakukan sebagai aktif. */
	private Boolean aktif;

	/** Penanda tipe/varian alur, diisi konstanta oleh Action pengelola. */
	private String tipe;

	/** Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate. */
	public AlurPersetujuanSuratMasuk() {
	}

	/**
	 * Mengembalikan kunci utama simpul alur.
	 *
	 * @return id, atau {@code null} bila entitas belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama (dipakai Hibernate dan kode pemuatan manual).
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama simpul alur, sudah dipangkas spasi tepi.
	 *
	 * @return nama simpul, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama simpul alur. Tidak ada validasi panjang/kekosongan di sini meskipun kolom
	 * dideklarasikan {@code nullable = false}; pelanggaran baru muncul sebagai galat database.
	 *
	 * @param nama nama simpul
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas simpul alur.
	 *
	 * @return keterangan, atau {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas simpul alur.
	 *
	 * @param keterangan teks keterangan
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan jenis jabatan tunggal pemilik jenjang ini, setelah dinormalisasi lewat
	 * {@link GeneralValueObject#check(Object)} (getter destruktif: hasil {@code check} ditulis
	 * balik ke field, sehingga referensi lepas/basi diganti {@code null}).
	 * <p>
	 * Inilah field kunci mesin alur: {@code SuratApi.disposisi_surat_masuk} dan
	 * {@code SuratMasukAction} menyaring simpul jenjang berikutnya dengan syarat field ini tidak
	 * {@code null}, lalu mencari seluruh {@link ais.database.model.rab.Pejabat} aktif dengan jenis
	 * jabatan tersebut untuk dibuatkan baris {@link AlurPersetujuanSuratMasukStatus}.
	 *
	 * @return jenis jabatan pemilik jenjang, atau {@code null} bila simpul tidak menargetkan jabatan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_jabatan", nullable = true)
	public JenisJabatan getJenisJabatan() {
		jenisJabatan = check(jenisJabatan);
		return jenisJabatan;
	}

	/**
	 * Menyetel jenis jabatan pemilik jenjang.
	 *
	 * @param jenisJabatan jenis jabatan
	 */
	public void setJenisJabatan(JenisJabatan jenisJabatan) {
		this.jenisJabatan = jenisJabatan;
	}

	/**
	 * Mengembalikan penanda item bawaan, dengan <b>materialisasi default</b>: bila field masih
	 * {@code null}, field diisi {@code true} lalu dikembalikan. Pemanggilan getter ini karena itu
	 * mengubah state objek dan &mdash; pada entitas terkelola &mdash; dapat menyebabkan kolom
	 * ikut tertulis pada {@code UPDATE} berikutnya.
	 * <p>
	 * Waspadai pemakaiannya: pada {@code AlurPersetujuanSuratMasukTreeAction} kolom pohon yang
	 * berlabel "Aktif" sebenarnya membaca dan menulis field ini, bukan {@link #getAktif()}.
	 * Kedua bendera itu hidup berdampingan tanpa sinkronisasi, sehingga sebuah simpul bisa
	 * "tidak aktif" menurut satu layar dan "aktif" menurut layar lain.
	 *
	 * @return {@code true} bila item bawaan (default bila belum pernah diisi)
	 */
	public Boolean getDefaultItem() {
		if (defaultItem == null) {
			defaultItem = true;
		}
		return defaultItem;
	}

	/**
	 * Menyetel penanda item bawaan.
	 *
	 * @param defaultItem nilai penanda
	 */
	public void setDefaultItem(Boolean defaultItem) {
		this.defaultItem = defaultItem;
	}

	/**
	 * Mengembalikan simpul induk pada pohon alur &mdash; relasi yang menentukan <b>urutan</b>
	 * jenjang persetujuan surat masuk.
	 * <p>
	 * Semantik pohon di modul ini: sebuah simpul dengan {@code parent == null} adalah jenjang
	 * pertama; "jenjang berikutnya" dari simpul X adalah seluruh baris yang {@code parent}-nya
	 * menunjuk ke X. Mesin alur menelusuri hubungan ini satu tingkat setiap kali sebuah baris
	 * {@link AlurPersetujuanSuratMasukStatus} ditandai disetujui: ia mengambil anak-anak simpul
	 * dari baris status tersebut, mencari pejabat untuk tiap {@link JenisJabatan} anak, lalu
	 * membuat baris status baru. Karena penelusuran hanya satu tingkat dan hanya dipicu oleh
	 * peristiwa persetujuan, pohon ini tidak pernah "dievaluasi utuh" di manapun.
	 * <p>
	 * Getter ini melakukan dua hal yang membuatnya bukan operasi baca murni:
	 * <ol>
	 * <li>Menormalisasi referensi lewat {@link GeneralValueObject#check(Object)} dan menulis
	 * hasilnya balik ke field &mdash; pola getter destruktif yang lazim di seluruh model AIS.
	 * Referensi ke baris yang sudah terhapus atau proxy yang tidak dapat dipulihkan akan berubah
	 * menjadi {@code null} tanpa peringatan.</li>
	 * <li><b>Menjaga siklus tingkat pertama</b>: bila {@code parent} ternyata menunjuk ke dirinya
	 * sendiri (id induk sama dengan id simpul ini), field {@code parent} dikosongkan menjadi
	 * {@code null}. Tujuannya mencegah rekursi tak berujung pada perender pohon ZK dan pada mesin
	 * alur yang akan selamanya menemukan dirinya sendiri sebagai "jenjang berikutnya".</li>
	 * </ol>
	 * <b>Batas penjagaan siklus.</b> Pemeriksaan hanya menutup kasus paling sepele (A &rarr; A).
	 * Siklus yang lebih panjang &mdash; A &rarr; B &rarr; A, atau A &rarr; B &rarr; C &rarr; A
	 * &mdash; sama sekali tidak terdeteksi di sini, dan tidak ada validasi lain di entitas ini yang
	 * mencegah pembentukannya saat simpul disimpan lewat form pohon. Konsekuensinya bukan sekadar
	 * kosmetik: rantai persetujuan yang membentuk lingkaran akan terus-menerus melahirkan baris
	 * status baru setiap kali satu jenjang disetujui, karena simpul yang sudah dilewati muncul lagi
	 * sebagai anak. Dampaknya adalah alur yang tidak pernah bisa "selesai" plus pembengkakan tabel
	 * status. Penjagaan yang benar memerlukan penelusuran ke atas sampai akar (atau batas kedalaman)
	 * pada saat simpan, dan itu memang tidak ada di lapisan model.
	 * <p>
	 * Perlu dicatat juga bahwa penjagaan ini hanya berlaku bila kedua id sudah terisi. Pada entitas
	 * yang belum tersimpan ({@code getId() == null}) pemeriksaan dilewati sepenuhnya, sehingga
	 * pengosongan baru terjadi pada pembacaan berikutnya setelah entitas punya id. Efek sampingnya:
	 * relasi yang terlihat "hilang sendiri" antara satu pembacaan dan pembacaan berikutnya, yang
	 * mudah disalahartikan sebagai kegagalan penyimpanan.
	 * <p>
	 * Terakhir, karena pengosongan dilakukan pada <i>field</i> dan bukan sekadar pada nilai
	 * kembalian, entitas terkelola yang di-flush setelah getter ini dipanggil akan benar-benar
	 * menuliskan {@code parent = NULL} ke database. Getter ini karena itu dapat mengubah struktur
	 * pohon secara permanen, bukan hanya menyembunyikan siklus dari tampilan.
	 *
	 * @return simpul induk, atau {@code null} bila simpul ini adalah akar (atau induknya sama
	 *         dengan dirinya sendiri sehingga baru saja dikosongkan)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "parent", nullable = true)
	public AlurPersetujuanSuratMasuk getParent() {
		parent = check(parent);
		if (parent != null && parent.getId() != null && getId() != null && getId().equals(parent.getId())) {
			parent = null;
		}
		return parent;
	}

	/**
	 * Menyetel simpul induk. Tidak ada validasi anti-siklus di sini &mdash; lihat catatan pada
	 * {@link #getParent()}.
	 *
	 * @param parent simpul induk
	 */
	public void setParent(AlurPersetujuanSuratMasuk parent) {
		this.parent = parent;
	}

	/**
	 * Mengembalikan kedalaman simpul pada pohon. Field ini tidak diisi otomatis oleh modul surat
	 * (tidak ada {@code TreeModel} yang memanggil {@link #setDeep(Integer)} untuk entitas ini),
	 * sehingga umumnya bernilai {@code null}.
	 *
	 * @return kedalaman, atau {@code null}
	 */
	public Integer getDeep() {
		return deep;
	}

	/**
	 * Menyetel kedalaman simpul pada pohon.
	 *
	 * @param deep kedalaman
	 */
	public void setDeep(Integer deep) {
		this.deep = deep;
	}

	/**
	 * Mengembalikan pencacah pemakaian simpul. Tidak ada kode di luar kelas ini yang membaca atau
	 * memutakhirkannya (field tidur); nilainya tetap pada default {@code 0}.
	 *
	 * @return jumlah pemakaian
	 */
	public Long getJmlDipakai() {
		return jmlDipakai;
	}

	/**
	 * Menyetel pencacah pemakaian simpul.
	 *
	 * @param jmlDipakai jumlah pemakaian
	 */
	public void setJmlDipakai(Long jmlDipakai) {
		this.jmlDipakai = jmlDipakai;
	}

	/**
	 * Mengembalikan tautan silang ke definisi alur <b>surat keluar</b> (getter destruktif lewat
	 * {@link GeneralValueObject#check(Object)}).
	 * <p>
	 * Tautan ini dipakai pada skenario tindak lanjut: sebuah simpul alur surat masuk dapat
	 * menunjuk alur surat keluar yang harus dijalankan ketika surat masuk tersebut dibalas.
	 * Pembacanya antara lain {@code AlurPersetujuanSuratMasukAction} dan
	 * {@code AlurPersetujuanSuratMasukTreeAction} (untuk menampilkan nama alur terkait).
	 *
	 * @return definisi alur surat keluar terkait, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "alur_persetujuan_surat_keluar", nullable = true)
	public AlurPersetujuanSuratKeluar getAlurPersetujuanSuratKeluar() {
		alurPersetujuanSuratKeluar = check(alurPersetujuanSuratKeluar);
		return alurPersetujuanSuratKeluar;
	}

	/**
	 * Menyetel tautan silang ke definisi alur surat keluar.
	 *
	 * @param alurPersetujuanSuratKeluar definisi alur surat keluar
	 */
	public void setAlurPersetujuanSuratKeluar(AlurPersetujuanSuratKeluar alurPersetujuanSuratKeluar) {
		this.alurPersetujuanSuratKeluar = alurPersetujuanSuratKeluar;
	}

	/**
	 * Mengembalikan klasifikasi surat keluar yang dipakai bila simpul ini menurunkan surat keluar
	 * (getter destruktif lewat {@link GeneralValueObject#check(Object)}).
	 *
	 * @return klasifikasi surat keluar, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "klasifikasi_surat_keluar", nullable = true)
	public KlasifikasiSuratKeluar getKlasifikasiSuratKeluar() {
		klasifikasiSuratKeluar = check(klasifikasiSuratKeluar);
		return klasifikasiSuratKeluar;
	}

	/**
	 * Menyetel klasifikasi surat keluar terkait.
	 *
	 * @param klasifikasiSuratKeluar klasifikasi surat keluar
	 */
	public void setKlasifikasiSuratKeluar(KlasifikasiSuratKeluar klasifikasiSuratKeluar) {
		this.klasifikasiSuratKeluar = klasifikasiSuratKeluar;
	}

	/**
	 * Mengembalikan cakupan jurusan/program studi simpul alur (getter destruktif lewat
	 * {@link GeneralValueObject#check(Object)}). {@code null} berarti tidak dibatasi jurusan.
	 *
	 * @return jurusan, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	/**
	 * Menyetel cakupan jurusan simpul alur.
	 *
	 * @param jurusan jurusan
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * Mengembalikan cakupan fakultas simpul alur (getter destruktif lewat
	 * {@link GeneralValueObject#check(Object)}). {@code null} berarti tidak dibatasi fakultas.
	 *
	 * @return fakultas, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "fakultas", nullable = true)
	public Fakultas getFakultas() {
		fakultas = check(fakultas);
		return fakultas;
	}

	/**
	 * Menyetel cakupan fakultas simpul alur.
	 *
	 * @param fakultas fakultas
	 */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	/**
	 * Mengembalikan cakupan satuan kerja simpul alur (getter destruktif lewat
	 * {@link GeneralValueObject#check(Object)}).
	 * <p>
	 * Field inilah yang biasanya dipakai sebagai pembatas tenant di modul lain. Di sini nilainya
	 * boleh {@code null} dan entitas tidak menegakkan apa pun; penyaringan berdasarkan satuan kerja
	 * sepenuhnya bergantung pada query di lapisan Action. Simpul alur tanpa satuan kerja bersifat
	 * global dan dapat terpakai oleh surat dari satuan kerja mana pun.
	 *
	 * @return satuan kerja, atau {@code null} bila simpul bersifat global
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);
		return satuanKerja;
	}

	/**
	 * Menyetel cakupan satuan kerja simpul alur.
	 *
	 * @param satuanKerja satuan kerja
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/**
	 * Mengembalikan cakupan sekolah simpul alur (getter destruktif lewat
	 * {@link GeneralValueObject#check(Object)}).
	 *
	 * @return sekolah, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah", nullable = true)
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return sekolah;
	}

	/**
	 * Menyetel cakupan sekolah, dengan normalisasi: objek {@link Sekolah} yang belum tersimpan
	 * (ber-{@code id} {@code null}) diperlakukan sama dengan tidak ada cakupan dan disimpan sebagai
	 * {@code null}. Ini mencegah Hibernate mencoba mem-persist entitas sekolah kosong yang berasal
	 * dari kombo ZK yang belum dipilih.
	 *
	 * @param sekolah sekolah; objek tanpa id akan menjadi {@code null}
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah == null || sekolah.getId() == null ? null : sekolah;
	}

	/**
	 * Mengembalikan cakupan yayasan simpul alur (getter destruktif lewat
	 * {@link GeneralValueObject#check(Object)}).
	 *
	 * @return yayasan, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan", nullable = true)
	public Yayasan getYayasan() {
		yayasan = check(yayasan);
		return yayasan;
	}

	/**
	 * Menyetel cakupan yayasan, dengan normalisasi yang sama seperti {@link #setSekolah(Sekolah)}:
	 * objek tanpa id disimpan sebagai {@code null}.
	 *
	 * @param yayasan yayasan; objek tanpa id akan menjadi {@code null}
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan == null || yayasan.getId() == null ? null : yayasan;
	}

	/**
	 * Mengembalikan bendera aktif simpul alur dengan default aman: {@code null} dibaca sebagai
	 * {@code true} (aktif). Berbeda dari {@link #getDefaultItem()}, getter ini TIDAK menulis balik
	 * default ke field &mdash; jadi baris lama tetap menyimpan {@code NULL} di database sementara
	 * aplikasi memperlakukannya sebagai aktif.
	 * <p>
	 * Karena default-nya "aktif", menonaktifkan sebuah simpul alur harus dilakukan secara eksplisit;
	 * simpul yang baru dibuat dan belum pernah disentuh form akan otomatis ikut dipertimbangkan
	 * oleh query yang menyaring simpul aktif.
	 *
	 * @return {@code true} bila simpul aktif (termasuk saat nilai belum pernah diisi)
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel bendera aktif simpul alur.
	 *
	 * @param aktif {@code true} untuk mengaktifkan
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan penanda tipe/varian alur. Diisi konstanta oleh Action pengelola
	 * ({@code AlurPersetujuanSuratMasukAction}/{@code AlurPersetujuanSuratMasukTreeAction}) untuk
	 * memisahkan kelompok alur pada layar yang sama.
	 *
	 * @return penanda tipe, atau {@code null}
	 */
	public String getTipe() {
		return tipe;
	}

	/**
	 * Menyetel penanda tipe/varian alur.
	 *
	 * @param tipe penanda tipe
	 */
	public void setTipe(String tipe) {
		this.tipe = tipe;
	}
}
