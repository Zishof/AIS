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
 * Entitas <b>definisi</b> satu simpul (tahapan/jenjang) pada alur persetujuan untuk <b>surat
 * keluar</b>. Dipetakan ke tabel {@code surat.alur_persetujuan_surat_keluar}.
 *
 * <h2>Peran entitas ini</h2>
 * Kelas ini adalah kembaran {@link AlurPersetujuanSuratMasuk} untuk sisi surat keluar, dan
 * memisahkan dua hal yang sama:
 * <ol>
 * <li><b>Definisi alur</b> (kelas ini): pohon jenjang persetujuan berbasis {@link JenisJabatan}
 * &mdash; siapa (dalam arti jabatan, bukan orang) harus menyetujui, dan pada tingkat keberapa.
 * Urutan dibentuk lewat {@link #getParent()} (self-referencing tree). Dikelola oleh
 * {@code AlurPersetujuanSuratKeluarAction} dan {@code AlurPersetujuanSuratKeluarTreeAction}.</li>
 * <li><b>Status/riwayat alur</b> ({@link AlurPersetujuanSuratKeluarStatus}): realisasi aktual per
 * surat per pejabat, lengkap dengan bendera {@code disetujui}/{@code ditolak}/{@code selesai},
 * waktu keputusan, dan catatan. Baris status dibuat "malas" saat jenjang sebelumnya disetujui.</li>
 * </ol>
 * Membaca kelas ini saja tidak pernah memberi tahu apakah sebuah surat sudah disetujui.
 *
 * <h2>Perbedaan dari sisi surat masuk</h2>
 * Selain seluruh field yang juga dimiliki {@link AlurPersetujuanSuratMasuk}, kelas ini menambahkan
 * dua bendera kebijakan yang tidak ada di sisi surat masuk:
 * <ul>
 * <li>{@link #getHarusMengikutiAlur()} &mdash; melarang disposisi bebas (ad hoc) sehingga pengguna
 * hanya boleh mengikuti pohon jenjang yang sudah didefinisikan;</li>
 * <li>{@link #getTerdapatPilihanSelesai()} &mdash; mengizinkan sebuah tahapan menutup rantai lebih
 * awal lewat bendera {@code selesai} pada baris status.</li>
 * </ul>
 * Keduanya <b>hanya ditegakkan sebagai pengaturan tampilan</b>; lihat catatan pada masing-masing
 * getter. Perbedaan lain: tabel gabungan {@link #getJenisJabatans()} di sini bernama
 * {@code surat.alur_punya_jenis_jabatan}, sedangkan padanannya di sisi surat masuk justru bernama
 * {@code surat.alur_keluar_punya_jenis_jabatan} &mdash; penamaan yang berlawanan intuisi, meski
 * keduanya tetap tabel yang berbeda sehingga data tidak bercampur.
 *
 * <h2>Bagaimana simpul ini dipakai saat runtime</h2>
 * <ul>
 * <li>{@code ais.action.servlet.api.SuratApi#disposisi_surat_keluar} &mdash; setelah satu baris
 * status disetujui, seluruh simpul yang {@code parent}-nya sama dengan simpul baris tersebut
 * <b>dan</b> memiliki {@link #getJenisJabatan()} tidak {@code null} dicari, lalu untuk tiap jenis
 * jabatan itu dibuatkan baris status baru untuk setiap {@link ais.database.model.rab.Pejabat}
 * terkait.</li>
 * <li>{@code ais.action.master.surat.SuratKeluarAction} &mdash; membentuk baris status jenjang
 * pertama saat surat keluar disimpan, dan menyembunyikan tab "Disposisi ke" bila
 * {@link #getHarusMengikutiAlur()} menyala.</li>
 * <li>{@code DasboardSurat}/{@code DasboardAlurSurat} &mdash; merender rantai alur pada dasbor.</li>
 * </ul>
 * Sama seperti pada sisi surat masuk, penyaringan {@code isNotNull("jenisJabatan")} berarti simpul
 * yang hanya mengisi koleksi {@link #getJenisJabatans()} tanpa mengisi relasi tunggal
 * {@link #getJenisJabatan()} tidak akan pernah melahirkan jenjang berikutnya.
 *
 * <h2>Cakupan/tenant</h2>
 * Simpul dapat dilingkupi ke {@link Yayasan}, {@link Sekolah}, {@link SatuanKerja},
 * {@link Fakultas}, atau {@link Jurusan}; semuanya opsional dan tidak satu pun divalidasi wajib di
 * level entitas. Simpul tanpa cakupan bersifat global dan dapat terpakai lintas tenant &mdash;
 * penyaringan sepenuhnya bergantung pada query di lapisan Action.
 *
 * <h2>Catatan arsitektur</h2>
 * <ul>
 * <li><b>Getter destruktif</b> pada hampir seluruh relasi (pola {@code field = check(field)});
 * {@link #getParent()} bahkan dapat mengosongkan {@code parent} secara permanen.</li>
 * <li><b>Dua bendera "aktif" yang tidak sinkron</b>: {@link #getAktif()} dan
 * {@link #getDefaultItem()}. Layar pohon merender kolom berlabel "Aktif" dari {@code defaultItem}.</li>
 * <li><b>Field audit bayangan</b> {@code oleh}/{@code olehId}/{@code tanggal_dirubah} &mdash;
 * keharusan teknis interseptor audit, bukan duplikasi yang perlu dibersihkan.</li>
 * <li><b>Field tidur</b>: {@link #getJmlDipakai()} dan {@link #getDeep()} tidak memiliki pemanggil
 * nyata di codebase.</li>
 * </ul>
 *
 * <h2>Peringatan keamanan</h2>
 * Definisi alur di kelas ini bersifat deklaratif. Tidak ada method di sini yang menegakkan urutan
 * jenjang, dan tidak ada mekanisme yang mencegah sebuah {@link AlurPersetujuanSuratKeluarStatus}
 * ditandai disetujui tanpa jenjang induknya lolos lebih dulu. Penegakan sepenuhnya bergantung pada
 * lapisan Action/API, yang saat ini tidak melakukannya &mdash; lihat dokumentasi
 * {@link AlurPersetujuanSuratKeluarStatus}.
 *
 * @see AlurPersetujuanSuratKeluarStatus
 * @see AlurPersetujuanSuratMasuk
 * @see SuratKeluar
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "surat", name = "alur_persetujuan_surat_keluar")
public class AlurPersetujuanSuratKeluar extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya identik dengan entitas alur persuratan lainnya (warisan
	 * salin-tempel generator) sehingga tidak dapat dipakai membedakan tipe.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama (identity, di-generate database). */
	private Long id;

	/** Nama pengguna terakhir yang mengubah baris ini; diisi interseptor audit. */
	private String oleh;

	/** Id pengguna terakhir yang mengubah baris ini; diisi interseptor audit. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang menyentuh baris ini (field audit bayangan).
	 *
	 * @return id pengguna, atau {@code null}
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir; nilai {@code null}/kosong diabaikan agar jejak audit
	 * lama tidak terhapus oleh binding form yang kosong.
	 *
	 * @param olehId id pengguna
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Representasi teks simpul alur untuk komponen daftar/kombo ZK: nama simpul diikuti nama jenis
	 * jabatan dalam kurung bila ada. Memanggil {@link #getJenisJabatan()} sehingga ikut menormalisasi
	 * (dan berpotensi mengubah) field {@code jenisJabatan} &mdash; {@code toString()} di sini punya
	 * efek samping.
	 *
	 * @return teks tampilan simpul alur
	 */
	public String toString() {
		jenisJabatan = getJenisJabatan();
		return nama + (jenisJabatan == null ? "" : " (" + jenisJabatan.getNama() + ") ");
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir; nilai {@code null}/kosong diabaikan.
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
	 * {@code AuditTimestampInterceptor.ubah(this)} tepat sebelum {@code UPDATE}.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** Stempel waktu perubahan terakhir; diinisialisasi ke waktu pembuatan objek. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir.
	 *
	 * @param tanggal_dirubah waktu perubahan
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir. Berbeda dari padanannya di
	 * {@link AlurPersetujuanSuratKeluarStatus}, getter ini tidak mematerialkan nilai default bila
	 * field masih {@code null}.
	 *
	 * @return waktu perubahan terakhir, atau {@code null}
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
	 * Jenis jabatan tunggal pemilik jenjang ini &mdash; field yang dipakai mesin alur untuk mencari
	 * pejabat jenjang berikutnya.
	 */
	private JenisJabatan jenisJabatan;

	/**
	 * Koleksi jenis jabatan tambahan (dipakai form pemilihan multi-jabatan). Terpisah dari
	 * {@link #jenisJabatan} dan tidak dipakai mesin pembentuk jenjang berikutnya.
	 */
	private Set<JenisJabatan> jenisJabatans = new TreeSet<JenisJabatan>();

	/**
	 * Mengembalikan himpunan jenis jabatan tambahan untuk simpul alur ini, dipetakan ke tabel
	 * gabungan {@code surat.alur_punya_jenis_jabatan}.
	 * <p>
	 * Koleksi dikembalikan sebagai referensi langsung (bukan salinan) sehingga modifikasi pemanggil
	 * ikut tersimpan lewat {@code CascadeType.MERGE}. Wadahnya {@link TreeSet}, jadi elemen
	 * {@link JenisJabatan} harus memiliki urutan alami yang konsisten dan elemen {@code null} akan
	 * melempar {@code NullPointerException} saat dimasukkan.
	 * <p>
	 * <b>Penting untuk alur persetujuan:</b> mesin alur pada {@code SuratApi.disposisi_surat_keluar}
	 * dan {@code SuratKeluarAction} mencari jenjang berikutnya dengan
	 * {@code Restrictions.isNotNull("jenisJabatan")} &mdash; memakai relasi tunggal
	 * {@link #getJenisJabatan()}, BUKAN koleksi ini. Simpul yang hanya mengisi koleksi ini akan
	 * tampil wajar di form namun tidak pernah menghasilkan baris status jenjang berikutnya, sehingga
	 * rantai persetujuan berhenti secara diam-diam. Koleksi ini dipakai
	 * {@code AlurPersetujuanSuratKeluarStatusAction} untuk menyusun daftar pilihan pejabat pada form
	 * disposisi, bukan untuk menentukan jenjang.
	 *
	 * @return himpunan jenis jabatan tambahan (tidak pernah {@code null} pada objek baru)
	 */
	@ManyToMany(targetEntity = JenisJabatan.class, cascade = { CascadeType.MERGE })
	@JoinTable(name = "alur_punya_jenis_jabatan", joinColumns = @JoinColumn(name = "alur"), inverseJoinColumns = @JoinColumn(name = "jenis_jabatan"), schema = "surat")
	public Set<JenisJabatan> getJenisJabatans() {
		return jenisJabatans;
	}

	/**
	 * Mengganti himpunan jenis jabatan tambahan. Nilai {@code null} diterima apa adanya (tidak ada
	 * penjagaan).
	 *
	 * @param jenisJabatans himpunan pengganti
	 */
	public void setJenisJabatans(Set<JenisJabatan> jenisJabatans) {
		this.jenisJabatans = jenisJabatans;
	}

	/** Simpul induk pada pohon alur; {@code null} berarti simpul akar (jenjang pertama). */
	private AlurPersetujuanSuratKeluar parent;

	/** Penanda item bawaan; di layar pohon dirender sebagai kolom berlabel "Aktif". */
	private Boolean defaultItem;

	/** Kedalaman simpul pada pohon. Tidak ada pengisi otomatis di modul surat (field tidur). */
	private Integer deep;

	/** Pencacah pemakaian simpul. Tidak memiliki pemanggil nyata di codebase (field tidur). */
	private Long jmlDipakai = 0L;

	/** Tautan silang ke definisi alur surat masuk (skenario surat keluar sebagai tindak lanjut). */
	private AlurPersetujuanSuratMasuk alurPersetujuanSuratMasuk;

	/** Klasifikasi surat masuk terkait, dipakai saat simpul ini menurunkan surat masuk. */
	private KlasifikasiSuratMasuk klasifikasiSuratMasuk;

	/** Cakupan jurusan/program studi (opsional). */
	private Jurusan jurusan;

	/** Cakupan fakultas (opsional). */
	private Fakultas fakultas;

	/** Cakupan satuan kerja (opsional). */
	private SatuanKerja satuanKerja;

	/** Cakupan sekolah (opsional). */
	private Sekolah sekolah;

	/** Cakupan yayasan (opsional). */
	private Yayasan yayasan;

	/** Bendera aktif simpul; {@code null} diperlakukan sebagai aktif. */
	private Boolean aktif;

	/** Kebijakan: tahapan boleh menutup rantai lebih awal lewat bendera {@code selesai}. */
	private Boolean terdapatPilihanSelesai;

	/** Kebijakan: disposisi bebas (ad hoc) dilarang, pengguna harus mengikuti pohon jenjang. */
	private Boolean harusMengikutiAlur;

	/** Penanda tipe/varian alur, diisi konstanta oleh Action pengelola. */
	private String tipe;

	/** Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate. */
	public AlurPersetujuanSuratKeluar() {
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
	 * dideklarasikan {@code nullable = false}.
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
	 * {@link GeneralValueObject#check(Object)} (getter destruktif: hasil ditulis balik ke field).
	 * <p>
	 * Inilah field kunci mesin alur: {@code SuratApi.disposisi_surat_keluar} dan
	 * {@code SuratKeluarAction} menyaring simpul jenjang berikutnya dengan syarat field ini tidak
	 * {@code null}, lalu mencari seluruh {@link ais.database.model.rab.Pejabat} aktif berjenis
	 * jabatan tersebut untuk dibuatkan baris {@link AlurPersetujuanSuratKeluarStatus}.
	 *
	 * @return jenis jabatan pemilik jenjang, atau {@code null}
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
	 * mengubah state objek dan pada entitas terkelola dapat menyebabkan kolom ikut tertulis pada
	 * {@code UPDATE} berikutnya.
	 * <p>
	 * Waspadai pemakaiannya: {@code AlurPersetujuanSuratKeluarTreeAction} merender kolom pohon
	 * berlabel "Aktif" dari field ini, bukan dari {@link #getAktif()}. Kedua bendera hidup
	 * berdampingan tanpa sinkronisasi, sehingga satu simpul bisa terbaca "tidak aktif" pada satu
	 * layar dan "aktif" pada layar lain.
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
	 * jenjang persetujuan surat keluar.
	 * <p>
	 * Semantik pohon: simpul dengan {@code parent == null} adalah jenjang pertama; "jenjang
	 * berikutnya" dari simpul X adalah seluruh baris yang {@code parent}-nya menunjuk X. Mesin alur
	 * menelusuri hubungan ini satu tingkat setiap kali sebuah baris
	 * {@link AlurPersetujuanSuratKeluarStatus} ditandai disetujui: anak-anak simpul baris tersebut
	 * diambil, pejabat untuk tiap {@link JenisJabatan} anak dicari, lalu baris status baru dibuat.
	 * Karena penelusuran hanya satu tingkat dan hanya dipicu oleh peristiwa persetujuan, pohon ini
	 * tidak pernah dievaluasi secara utuh di manapun &mdash; tidak ada satu pun kode yang
	 * memverifikasi bahwa seluruh jalur dari akar sampai daun benar-benar sudah dilalui.
	 * <p>
	 * Getter ini bukan operasi baca murni karena melakukan dua hal:
	 * <ol>
	 * <li>Menormalisasi referensi lewat {@link GeneralValueObject#check(Object)} dan menulis
	 * hasilnya balik ke field &mdash; pola getter destruktif yang lazim di model AIS. Referensi ke
	 * baris terhapus atau proxy yang tidak dapat dipulihkan berubah menjadi {@code null} tanpa
	 * peringatan.</li>
	 * <li><b>Menjaga siklus tingkat pertama</b>: bila {@code parent} menunjuk ke dirinya sendiri (id
	 * induk sama dengan id simpul ini), field {@code parent} dikosongkan menjadi {@code null}, demi
	 * mencegah rekursi tak berujung pada perender pohon ZK dan pada mesin alur yang akan selamanya
	 * menemukan dirinya sendiri sebagai jenjang berikutnya.</li>
	 * </ol>
	 * <b>Batas penjagaan siklus.</b> Hanya kasus paling sepele (A &rarr; A) yang tertutup. Siklus
	 * yang lebih panjang &mdash; A &rarr; B &rarr; A, atau lebih &mdash; sama sekali tidak terdeteksi
	 * di sini, dan tidak ada validasi lain di entitas ini yang mencegah pembentukannya lewat form
	 * pohon. Dampaknya bukan kosmetik: rantai persetujuan berbentuk lingkaran akan terus melahirkan
	 * baris status baru setiap kali satu jenjang disetujui, karena simpul yang sudah dilewati muncul
	 * lagi sebagai anak. Hasilnya adalah alur yang tidak pernah dapat mencapai akhir plus
	 * pembengkakan tabel status. Penjagaan yang benar menuntut penelusuran ke atas sampai akar (atau
	 * batas kedalaman) pada saat simpan, dan itu tidak ada di lapisan model.
	 * <p>
	 * Penjagaan juga hanya berlaku bila kedua id sudah terisi; pada entitas yang belum tersimpan
	 * ({@code getId() == null}) pemeriksaan dilewati, sehingga pengosongan baru terjadi pada
	 * pembacaan berikutnya setelah entitas punya id &mdash; relasi terlihat "hilang sendiri" antara
	 * dua pembacaan, yang mudah disalahartikan sebagai kegagalan penyimpanan.
	 * <p>
	 * Karena pengosongan dilakukan pada field dan bukan sekadar pada nilai kembalian, entitas
	 * terkelola yang di-flush setelah getter ini dipanggil akan benar-benar menuliskan
	 * {@code parent = NULL} ke database. Getter ini dapat mengubah struktur pohon secara permanen.
	 *
	 * @return simpul induk, atau {@code null} bila simpul ini akar (atau induknya sama dengan
	 *         dirinya sendiri sehingga baru saja dikosongkan)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "parent", nullable = true)
	public AlurPersetujuanSuratKeluar getParent() {
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
	public void setParent(AlurPersetujuanSuratKeluar parent) {
		this.parent = parent;
	}

	/**
	 * Mengembalikan kedalaman simpul pada pohon. Tidak diisi otomatis oleh modul surat (tidak ada
	 * {@code TreeModel} yang memanggil {@link #setDeep(Integer)} untuk entitas ini), sehingga
	 * umumnya {@code null}.
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
	 * Mengembalikan tautan silang ke definisi alur <b>surat masuk</b> (getter destruktif lewat
	 * {@link GeneralValueObject#check(Object)}).
	 * <p>
	 * Dipakai pada skenario surat keluar yang lahir sebagai tindak lanjut sebuah surat masuk; dibaca
	 * antara lain oleh {@code AlurPersetujuanSuratKeluarAction} untuk menampilkan nama alur terkait.
	 *
	 * @return definisi alur surat masuk terkait, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "alur_persetujuan_surat_masuk", nullable = true)
	public AlurPersetujuanSuratMasuk getAlurPersetujuanSuratMasuk() {
		alurPersetujuanSuratMasuk = check(alurPersetujuanSuratMasuk);
		return alurPersetujuanSuratMasuk;
	}

	/**
	 * Menyetel tautan silang ke definisi alur surat masuk.
	 *
	 * @param alurPersetujuanSuratMasuk definisi alur surat masuk
	 */
	public void setAlurPersetujuanSuratMasuk(AlurPersetujuanSuratMasuk alurPersetujuanSuratMasuk) {
		this.alurPersetujuanSuratMasuk = alurPersetujuanSuratMasuk;
	}

	/**
	 * Mengembalikan klasifikasi surat masuk terkait (getter destruktif lewat
	 * {@link GeneralValueObject#check(Object)}).
	 * <p>
	 * Dipakai {@code AlurPersetujuanSuratKeluarStatusAction} ketika sebuah tahapan surat keluar
	 * menurunkan surat masuk baru: klasifikasi surat masuk yang dibuat diambil dari sini.
	 *
	 * @return klasifikasi surat masuk, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "klasifikasi_surat_masuk", nullable = true)
	public KlasifikasiSuratMasuk getKlasifikasiSuratMasuk() {
		klasifikasiSuratMasuk = check(klasifikasiSuratMasuk);
		return klasifikasiSuratMasuk;
	}

	/**
	 * Menyetel klasifikasi surat masuk terkait.
	 *
	 * @param klasifikasiSuratMasuk klasifikasi surat masuk
	 */
	public void setKlasifikasiSuratMasuk(KlasifikasiSuratMasuk klasifikasiSuratMasuk) {
		this.klasifikasiSuratMasuk = klasifikasiSuratMasuk;
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
	 * Field ini biasanya menjadi pembatas tenant di modul lain. Di sini nilainya boleh {@code null}
	 * dan entitas tidak menegakkan apa pun; penyaringan bergantung sepenuhnya pada query di lapisan
	 * Action. Simpul alur tanpa satuan kerja bersifat global dan dapat terpakai oleh surat dari
	 * satuan kerja mana pun.
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
	 * (ber-{@code id} {@code null}) disimpan sebagai {@code null}. Ini mencegah Hibernate mencoba
	 * mem-persist entitas sekolah kosong yang berasal dari kombo ZK yang belum dipilih.
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
	 * Mengembalikan kebijakan "wajib mengikuti alur", dengan default aman {@code false} bila belum
	 * pernah diisi (tanpa menulis balik ke field).
	 * <p>
	 * Ketika bernilai {@code true}, maksud kebijakannya adalah: pengguna tidak boleh menentukan
	 * sendiri tujuan disposisi berikutnya secara bebas, melainkan harus mengikuti pohon jenjang yang
	 * sudah didefinisikan. Namun perlu dipahami bahwa <b>penegakannya murni pada tampilan</b>:
	 * <ul>
	 * <li>{@code SuratKeluarAction} menyembunyikan tab "Disposisi ke"
	 * ({@code tabDisposisi.setVisible(!getHarusMengikutiAlur())}), dan pemanggilan itu bahkan
	 * dibungkus {@code try/catch} yang menelan galat &mdash; bila komponen tab belum ada, kebijakan
	 * ini gagal diterapkan tanpa jejak selain audit galat otomatis;</li>
	 * <li>{@code AlurPersetujuanSuratKeluarStatusAction} tidak membuat tab/panel "Disposisi ke" pada
	 * layar pratinjau bila bendera menyala.</li>
	 * </ul>
	 * Tidak ada satu pun pemeriksaan bendera ini pada saat penyimpanan. Nilai
	 * {@code jenisSurats} &mdash; peta JSON pejabat tujuan disposisi bebas pada
	 * {@link AlurPersetujuanSuratKeluarStatus#getJenisSurats()} &mdash; tetap diproses apa adanya
	 * oleh {@code onSave()} dan oleh {@code BroadcastHelper}, sehingga disposisi bebas yang dikirim
	 * lewat jalur non-UI (mis. API, atau permintaan yang menyertakan data tab yang seharusnya
	 * tersembunyi) tidak tertahan oleh kebijakan ini. Perlakukan bendera ini sebagai preferensi
	 * antarmuka, bukan sebagai kontrol keamanan.
	 * <p>
	 * Perhatikan juga bahwa kebijakan ini tidak memiliki padanan di
	 * {@link AlurPersetujuanSuratMasuk}: alur surat masuk selalu mengizinkan disposisi bebas.
	 *
	 * @return {@code true} bila disposisi bebas dilarang (default {@code false})
	 */
	public Boolean getHarusMengikutiAlur() {
		return harusMengikutiAlur == null ? false : harusMengikutiAlur;
	}

	/**
	 * Menyetel kebijakan "wajib mengikuti alur".
	 *
	 * @param harusMengikutiAlur nilai kebijakan
	 */
	public void setHarusMengikutiAlur(Boolean harusMengikutiAlur) {
		this.harusMengikutiAlur = harusMengikutiAlur;
	}

	/**
	 * Mengembalikan bendera aktif simpul alur dengan default aman: {@code null} dibaca sebagai
	 * {@code true} (aktif). Berbeda dari {@link #getDefaultItem()}, getter ini TIDAK menulis balik
	 * default ke field &mdash; baris lama tetap menyimpan {@code NULL} di database sementara
	 * aplikasi memperlakukannya sebagai aktif.
	 * <p>
	 * Karena default-nya aktif, menonaktifkan sebuah simpul harus dilakukan eksplisit; simpul baru
	 * yang belum pernah disentuh form otomatis ikut dipertimbangkan query yang menyaring simpul
	 * aktif.
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
	 * ({@code AlurPersetujuanSuratKeluarAction}/{@code AlurPersetujuanSuratKeluarTreeAction}) untuk
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

	/**
	 * Mengembalikan kebijakan "terdapat pilihan selesai", dengan default aman {@code false} bila
	 * belum pernah diisi (tanpa menulis balik ke field).
	 * <p>
	 * Bendera ini mengendalikan apakah sebuah tahapan boleh <b>menutup rantai lebih awal</b>: bila
	 * menyala, form persetujuan menampilkan kotak centang "Selesai sampai di sini" yang menulis
	 * {@link AlurPersetujuanSuratKeluarStatus#setSelesai(Boolean)}. Tahapan yang ditandai selesai
	 * dianggap mengakhiri alur meskipun pohon jenjang masih memiliki simpul anak yang belum pernah
	 * disetujui &mdash; jadi bendera inilah satu-satunya mekanisme "final lebih awal" yang resmi ada
	 * di modul ini.
	 * <p>
	 * Hubungannya dengan {@link AlurPersetujuanSuratKeluarStatus#getSelesai()} bersifat dua arah dan
	 * perlu diperhatikan: getter status tersebut memaksa {@code selesai = false} bila simpul alurnya
	 * ({@link AlurPersetujuanSuratKeluarStatus#getAlurPersetujuanSuratKeluar()}) tidak lagi
	 * mengizinkan pilihan selesai. Mematikan bendera ini pada definisi alur karena itu <b>membatalkan
	 * secara surut</b> seluruh penandaan "selesai" pada baris status yang sudah ada &mdash; dan
	 * karena getter status menulis balik ke field, pembatalan itu ikut tersimpan saat baris berikutnya
	 * di-flush. Perubahan konfigurasi alur dengan demikian dapat mengubah status historis surat.
	 * <p>
	 * Sebaliknya, menyalakan kembali bendera ini tidak memulihkan nilai yang sudah terlanjur
	 * tertulis {@code false}. Bendera ini juga tidak memiliki padanan di
	 * {@link AlurPersetujuanSuratMasuk}, sehingga alur surat masuk tidak mengenal konsep penutupan
	 * dini sama sekali.
	 *
	 * @return {@code true} bila tahapan boleh menutup rantai lebih awal (default {@code false})
	 */
	public Boolean getTerdapatPilihanSelesai() {
		return terdapatPilihanSelesai == null ? false : terdapatPilihanSelesai;
	}

	/**
	 * Menyetel kebijakan "terdapat pilihan selesai". Mematikannya berdampak surut terhadap baris
	 * status yang sudah bertanda selesai &mdash; lihat {@link #getTerdapatPilihanSelesai()}.
	 *
	 * @param terdapatPilihanSelesai nilai kebijakan
	 */
	public void setTerdapatPilihanSelesai(Boolean terdapatPilihanSelesai) {
		this.terdapatPilihanSelesai = terdapatPilihanSelesai;
	}
}
