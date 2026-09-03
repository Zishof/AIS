package ais.database.model.employ;

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
import ais.database.model.Pegawai;



/**
 * Model data untuk <b>unit kerja</b> — node dalam struktur organisasi berbentuk pohon (tree)
 * internal AIS, dipakai untuk mengelompokkan pegawai secara hierarkis (mis. Fakultas &gt; Jurusan
 * &gt; Program Studi, atau Biro &gt; Bagian &gt; Subbagian) terlepas dari struktur satuan kerja
 * anggaran/tenant yang dipakai modul lain. Tipe ini membawa state yang dipertukarkan oleh lapisan
 * persistence, service, dan UI; makna bisnis utamanya ditentukan oleh field serta relasi yang
 * dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap
 * dimiliki {@link GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar
 * spesifik untuk variasi ini; perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di
 * kelas induk agar fungsi tidak bercabang atau tumpang tindih.</p>
 *
 * <p><b>Tiga entity "satuan kerja" yang mirip nama tapi berbeda tujuan &mdash; jangan tertukar:</b></p>
 * <ul>
 * <li>{@code UnitKerja} (kelas ini, tabel {@code employ.unit_kerja}) — pohon organisasi
 * kepegawaian dengan self-reference {@link #getParent()}, {@link #getLevel()}, dan
 * {@link #getDeep()} eksplisit; dipakai {@code UnitKerjaAction}, {@code UnitSatkerTreeAction}, dan
 * rekap dashboard kepegawaian ({@code RekapJumlahPegawaiBaseUnitKerja},
 * {@code DasboardKepegawaianUnitKerja}) untuk menampilkan/menghitung pegawai per simpul
 * organisasi.</li>
 * <li>{@link SatuanKerjaEmploy} (tabel {@code employ.satuan_kerja_employ}) — daftar satuan kerja
 * <b>datar</b> (tanpa hierarki parent/level) khusus modul employ, dikelola lewat
 * {@code SatuanKerjaAction} dan DAO {@code getSatuanKerjaEmployDao()}. Struktur fieldnya
 * (nama/keterangan/jenisPimpinan/pimpinan/jabatanStruktural/prioritas) mirip {@code UnitKerja}
 * minus field pohon — kemungkinan besar predecessor historis {@code UnitKerja} yang masih
 * dipertahankan untuk kompatibilitas data lama, BUKAN alias.</li>
 * <li>{@code ais.database.model.rab.SatuanKerja} (paket {@code rab}, di luar cakupan berkas ini)
 * — entity satuan kerja anggaran/tenant lintas-modul yang jauh lebih luas pemakaiannya (dipakai
 * mis. {@link JamKerjaPegawai#getSatuanKerja()}); sama sekali kelas berbeda dari dua di atas
 * meski secara konsep serupa "unit organisasi".</li>
 * </ul>
 *
 * <p><b>Struktur pohon.</b> {@link #getParent()} menunjuk ke {@code UnitKerja} induk (self-
 * reference); {@link #getLevel()} dan {@link #getDeep()} menyimpan kedalaman node secara eksplisit
 * sebagai kolom terpisah (bukan dihitung on-the-fly dari rantai {@code parent}), sehingga kedua
 * nilai ini <b>harus dijaga konsisten manual</b> oleh kode yang menulis/memindah node — bila
 * {@code parent} diubah tanpa memperbarui {@code level}/{@code deep}, tampilan pohon bisa salah
 * tanpa exception apa pun. {@link #getDefaultItem()} menandai unit kerja yang dipakai sebagai
 * pilihan default pada form-form terkait.</p>
 *
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Long id}, {@code String
 * oleh}, {@code String olehId}, {@code String nama}, {@code String keterangan}, {@code
 * JenisPimpinan jenisPimpinan}, {@code Pegawai pimpinan}, {@code JabatanStruktural
 * jabatanStruktural}, {@code Integer prioritas}, {@code Integer level}, {@code Boolean
 * defaultItem}, {@code UnitKerja parent}, {@code Integer deep}, {@code Boolean aktif}, {@code
 * Date tanggal_dirubah}; pemetaan persistence: tabel {@code employ.unit_kerja}; pembacaan/pencarian
 * ({@code getOlehId()}, {@code getId()}, {@code getOleh()}, {@code getTanggal_dirubah()}, {@code
 * getNama()}, {@code getKeterangan()}, {@code getJenisPimpinan()}, {@code getPimpinan()}, {@code
 * getJabatanStruktural()}, {@code getPrioritas()}, {@code getParent()}, {@code getDeep()}, {@code
 * getLevel()}, {@code getDefaultItem()}, {@code getAktif()}); mutasi data ({@code setOlehId()},
 * {@code onUpdate()}, {@code setId()}, {@code setOleh()}, {@code setTanggal_dirubah()}, {@code
 * setNama()}, {@code setKeterangan()}, {@code setJenisPimpinan()}, {@code setPimpinan()}, {@code
 * setJabatanStruktural()}, {@code setPrioritas()}, {@code setParent()}, {@code setDeep()}, {@code
 * setLevel()}, {@code setDefaultItem()}, {@code setAktif()}); operasi domain lain ({@code
 * toString()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut
 * di atas.</p>
 *
 * <p><b>Flag aktif satu-arah.</b> {@link #getAktif()} mengembalikan {@code true} secara default
 * bila field belum pernah diset ({@code null}), bukan {@code false} — pola "flag aktif dengan
 * default permisif" yang berulang di banyak entity AIS: baris lama yang dibuat sebelum kolom
 * {@code aktif} ada otomatis tampil sebagai aktif tanpa migrasi data eksplisit.</p>
 *
 * <p><b>Efek samping:</b> accessor dan mutator hanya membaca atau mengubah state entity/value
 * object di memori. Relasi {@code @ManyToOne} ({@link #getJenisPimpinan()}, {@link #getPimpinan()},
 * {@link #getJabatanStruktural()}, {@link #getParent()}) dipetakan {@code @Fetch(FetchMode.SELECT)}
 * tanpa panggilan {@code check(...)} eksplisit di getter — berbeda dari pola lazy-resolve yang
 * dipakai field serupa di {@link GajiPokok}/{@link Insentif}/{@link Makan}/{@link Transport}, jadi
 * getter di sini <b>tidak</b> menjamin proxy lazy yang sudah detached bisa diakses aman; lihat
 * dokumentasi {@link GeneralValueObject} bagian mekanisme {@code check()} untuk konteks pola
 * standarnya. Persistence, transaksi, otorisasi, dan pemuatan relasi lazy tetap menjadi tanggung
 * jawab DAO/service dengan session aktif; jangan menaruh query duplikat pada model.</p>
 *
 * @see GeneralValueObject
 * @see SatuanKerjaEmploy
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "employ", name = "unit_kerja")



public class UnitKerja extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * Mengembalikan id pengguna (kolom {@code oleh_id}) yang terakhir membuat/mengubah baris unit
	 * kerja ini. Bagian dari pasangan field audit {@code oleh}/{@code olehId} yang diwarisi pola
	 * generiknya dari {@link GeneralValueObject}.
	 *
	 * @return id pengguna pencatat, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id pengguna pencatat. Nilai kosong atau hanya berisi whitespace diabaikan secara
	 * diam-diam (tidak melempar exception, tidak mengubah state).
	 *
	 * @param olehId id pengguna pencatat; {@code null} atau string kosong/whitespace diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan nama pengguna pencatat. Nilai kosong atau hanya berisi whitespace diabaikan
	 * secara diam-diam, sama seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna pencatat; {@code null} atau string kosong/whitespace diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna (kolom {@code oleh}) yang terakhir membuat/mengubah baris unit
	 * kerja ini.
	 *
	 * @return nama pengguna pencatat, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil otomatis oleh Hibernate tepat sebelum baris ini
	 * di-{@code UPDATE}, mendelegasikan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} untuk memutakhirkan
	 * {@link #tanggal_dirubah}. Tidak dipanggil manual oleh kode aplikasi.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan tanggal terakhir baris ini dirubah. Biasanya diisi otomatis oleh
	 * {@link #onUpdate()}, bukan dipanggil manual.
	 *
	 * @param tanggal_dirubah tanggal perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan tanggal terakhir baris ini dirubah. Nilai awalnya (sebelum pernah di-update)
	 * diinisialisasi ke waktu saat object dibuat, lewat {@code WaktuUtil.getDate()}.
	 *
	 * @return tanggal perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks unit kerja ini: mengembalikan {@link #getNama()} apa adanya. Dipakai di
	 * combobox/label pemilihan unit kerja dan node pohon organisasi pada UI.
	 *
	 * @return nama unit kerja
	 */
	public String toString() {
		return nama;
	}

	private String nama;
	private String keterangan;
	private JenisPimpinan jenisPimpinan;
	private Pegawai pimpinan;
	private JabatanStruktural jabatanStruktural;
	private Integer prioritas;
	private Integer level;
	private Boolean defaultItem = false;
	private UnitKerja parent;
	private Integer deep;
	private Boolean aktif;



	/**
	 * Konstruktor tanpa argumen yang dipersyaratkan Hibernate/JPA untuk instansiasi entity lewat
	 * reflection. {@link #defaultItem} tetap {@code false} sesuai inisialisasi field.
	 */
	public UnitKerja() {
	}

	/**
	 * Mengembalikan primary key unit kerja ini.
	 *
	 * @return id baris, atau {@code null} bila belum persisten
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan primary key. Kolom dipetakan {@code insertable = false} (nilai dihasilkan
	 * database via {@code IDENTITY}), jadi setter ini praktis hanya dipakai saat memuat ulang
	 * entity dari hasil query.
	 *
	 * @param id id baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama unit kerja, di-trim (whitespace di awal/akhir dibuang) setiap kali
	 * dibaca.
	 *
	 * @return nama unit kerja hasil trim, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan nama unit kerja. Nilai disimpan apa adanya (trim baru terjadi saat dibaca lewat
	 * {@link #getNama()}), sehingga whitespace mentah masih ada di field {@link #nama} sampai
	 * baris ini di-reload dari database.
	 *
	 * @param nama nama baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas untuk unit kerja ini.
	 *
	 * @return teks keterangan, atau {@code null} bila tidak diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas.
	 *
	 * @param keterangan teks keterangan
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan jenis pimpinan (mis. Dekan/Ketua Jurusan/Kepala Biro) yang berlaku untuk unit
	 * kerja ini.
	 *
	 * @return jenis pimpinan, atau {@code null} bila tidak diset
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jenis_pimpinan", nullable = true)
	public JenisPimpinan getJenisPimpinan() {
		return jenisPimpinan;
	}

	/**
	 * Menetapkan jenis pimpinan.
	 *
	 * @param jenisPimpinan jenis pimpinan baru
	 */
	public void setJenisPimpinan(JenisPimpinan jenisPimpinan) {
		this.jenisPimpinan = jenisPimpinan;
	}

	/**
	 * Mengembalikan pegawai yang menjabat sebagai pimpinan unit kerja ini.
	 *
	 * @return pegawai pimpinan, atau {@code null} bila belum ditunjuk
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pimpinan", nullable = true)
	public Pegawai getPimpinan() {
		return pimpinan;
	}

	/**
	 * Menetapkan pegawai pimpinan unit kerja.
	 *
	 * @param pimpinan pegawai pimpinan baru
	 */
	public void setPimpinan(Pegawai pimpinan) {
		this.pimpinan = pimpinan;
	}

	/**
	 * Mengembalikan jabatan struktural yang melekat pada posisi pimpinan unit kerja ini.
	 *
	 * @return jabatan struktural, atau {@code null} bila tidak diset
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jabatan_struktural", nullable = true)
	public JabatanStruktural getJabatanStruktural() {
		return jabatanStruktural;
	}

	/**
	 * Menetapkan jabatan struktural.
	 *
	 * @param jabatanStruktural jabatan struktural baru
	 */
	public void setJabatanStruktural(JabatanStruktural jabatanStruktural) {
		this.jabatanStruktural = jabatanStruktural;
	}

	/**
	 * Mengembalikan urutan prioritas tampil unit kerja ini di antara unit kerja sejawat (dipakai
	 * untuk mengurutkan node pada pohon organisasi).
	 *
	 * @return prioritas, atau {@code null} bila tidak diset
	 */
	@Column(name = "prioritas")
	public Integer getPrioritas() {
		return prioritas;
	}

	/**
	 * Menetapkan urutan prioritas tampil.
	 *
	 * @param prioritas prioritas baru
	 */
	public void setPrioritas(Integer prioritas) {
		this.prioritas = prioritas;
	}

	/**
	 * Mengembalikan unit kerja induk (self-reference) dalam pohon organisasi. {@code null} berarti
	 * baris ini adalah node akar (root).
	 *
	 * @return unit kerja induk, atau {@code null} bila node akar
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "parent", nullable = true)
	public UnitKerja getParent() {
		return parent;
	}

	/**
	 * Menetapkan unit kerja induk. <b>Perhatian:</b> mengubah {@code parent} tidak otomatis
	 * memutakhirkan {@link #getLevel()}/{@link #getDeep()} — kedua kolom itu harus disinkronkan
	 * manual oleh pemanggil (lihat catatan struktur pohon di Javadoc kelas).
	 *
	 * @param parent unit kerja induk baru
	 */
	public void setParent(UnitKerja parent) {
		this.parent = parent;
	}

	/**
	 * Mengembalikan kedalaman (deep) node ini pada pohon organisasi, disimpan sebagai kolom
	 * eksplisit dan tidak dihitung otomatis dari rantai {@link #getParent()}.
	 *
	 * @return kedalaman node, atau {@code null} bila belum diset
	 */
	public Integer getDeep() {
		return deep;
	}

	/**
	 * Menetapkan kedalaman node secara manual.
	 *
	 * @param deep kedalaman baru
	 */
	public void setDeep(Integer deep) {
		this.deep = deep;
	}

	/**
	 * Mengembalikan level node ini pada pohon organisasi. Sama seperti {@link #getDeep()},
	 * disimpan sebagai kolom eksplisit, tidak dihitung otomatis.
	 *
	 * @return level node, atau {@code null} bila belum diset
	 */
	public Integer getLevel() {
		return level;
	}

	/**
	 * Menetapkan level node secara manual.
	 *
	 * @param level level baru
	 */
	public void setLevel(Integer level) {
		this.level = level;
	}

	/**
	 * Mengembalikan penanda apakah unit kerja ini adalah pilihan default pada form-form yang
	 * memakai daftar unit kerja.
	 *
	 * @return {@code true} bila menjadi pilihan default; field diinisialisasi {@code false}
	 */
	public Boolean getDefaultItem() {
		return defaultItem;
	}

	/**
	 * Menetapkan penanda pilihan default.
	 *
	 * @param defaultItem penanda pilihan default baru
	 */
	public void setDefaultItem(Boolean defaultItem) {
		this.defaultItem = defaultItem;
	}

	/**
	 * Mengembalikan status aktif unit kerja ini. <b>Default permisif:</b> {@code null}
	 * dikembalikan sebagai {@code true} — baris yang belum pernah mengisi kolom {@code aktif}
	 * (mis. data lama sebelum kolom ini ada) otomatis dianggap aktif tanpa migrasi data eksplisit.
	 *
	 * @return {@code true} bila aktif atau belum diset, {@code false} bila secara eksplisit
	 *         dinonaktifkan
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menetapkan status aktif unit kerja.
	 *
	 * @param aktif status aktif baru
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}
}
