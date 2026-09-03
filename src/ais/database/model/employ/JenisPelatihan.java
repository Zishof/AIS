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

/**
 * Model data untuk katalog <b>jenis pelatihan</b> — berbeda dari {@link JenisDiklat}, kelas ini
 * berbentuk <b>pohon</b> (self-referential lewat {@link #getParent()}/{@link #getDeep()}), bukan
 * daftar datar, dan direferensikan lewat relasi opsional {@code jenisPelatihan} pada {@code
 * RiwayatPelatihanPegawai} — bukan oleh entity {@code Diklat} yang memakai {@link JenisDiklat}.
 * Pohon jenis pelatihan ditampilkan di UI lewat {@code JenisPelatihanTreeAction}/{@code
 * JenisPelatihanTreeModel} dan dikelola lewat {@code JenisPelatihanAction} serta DAO {@code
 * JenisPelatihanDao}/{@code JenisPelatihanDaoImpl}.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap
 * dimiliki {@link GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar
 * spesifik untuk variasi ini; perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di
 * kelas induk agar fungsi tidak bercabang atau tumpang tindih.</p>
 *
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Long id}, {@code String
 * oleh}, {@code String olehId}, {@code String kode}, {@code String nama}, {@code String
 * keterangan}, {@code Boolean defaultItem}, {@code JenisPelatihan parent}, {@code Integer deep},
 * {@code Long jmlDipakai}, {@code Date tanggal_dirubah}; pemetaan persistence: tabel {@code
 * employ.jenis_pelatihan}; pembacaan/pencarian ({@code getOlehId()}, {@code getId()}, {@code
 * getOleh()}, {@code getTanggal_dirubah()}, {@code getNama()}, {@code getKeterangan()}, {@code
 * getDefaultItem()}, {@code getParent()}, {@code getDeep()}, {@code getJmlDipakai()}, {@code
 * getKode()}); mutasi data ({@code setOlehId()}, {@code onUpdate()}, {@code setId()}, {@code
 * setOleh()}, {@code setTanggal_dirubah()}, {@code setNama()}, {@code setKeterangan()}, {@code
 * setDefaultItem()}, {@code setParent()}, {@code setDeep()}, {@code setJmlDipakai()}, {@code
 * setKode()}); operasi domain lain ({@code toString()}). Bagian lain dari kontrak tetap mengikuti
 * kelas induk atau interface yang disebut di atas.</p>
 *
 * <p><b>Getter dengan efek samping:</b> {@link #getDefaultItem()} tidak murni membaca state —
 * bila field masih {@code null} (mis. baris lama yang belum pernah menyimpan nilai eksplisit,
 * berbeda dari default konstruktor {@code false}), method ini menuliskan {@code true} ke field
 * {@link #defaultItem} sebelum mengembalikannya. Pola ini sejalan dengan pola getter destruktif
 * yang berulang di paket ini (lihat query {@code Restrictions.eq("defaultItem", true)} pada
 * {@code AmbilDataJenisPelatihanBanyak}) — pemanggil yang hanya ingin membaca nilai tanpa
 * mengubah state entity tetap harus menyadari efek samping ini.</p>
 *
 * <p><b>Efek samping:</b> selain {@link #getDefaultItem()} di atas, accessor dan mutator lain
 * hanya membaca atau mengubah state entity/value object di memori. Relasi {@code @ManyToOne}
 * self-referential ({@link #getParent()}) dipetakan {@code @Fetch(FetchMode.SELECT)} dengan
 * cascade {@code PERSIST}/{@code MERGE}. Persistence, transaksi, otorisasi, dan pemuatan relasi
 * lazy tetap menjadi tanggung jawab DAO/service dengan session aktif; jangan menaruh query
 * duplikat pada model.</p>
 *
 * @see GeneralValueObject
 * @see JenisDiklat
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "employ", name = "jenis_pelatihan")
public class JenisPelatihan extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * Mengembalikan id pengguna (kolom {@code oleh_id}) yang terakhir membuat/mengubah baris jenis
	 * pelatihan ini. Bagian dari pasangan field audit {@code oleh}/{@code olehId} yang diwarisi
	 * pola generiknya dari {@link GeneralValueObject}.
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
	 * Mengembalikan nama pengguna (kolom {@code oleh}) yang terakhir membuat/mengubah baris jenis
	 * pelatihan ini.
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
	 * Representasi teks jenis pelatihan ini: mengembalikan {@link #getNama()} apa adanya (tanpa
	 * trim, berbeda dari {@link #getNama()} sendiri yang men-trim). Dipakai di
	 * treecell/combobox/label pemilihan jenis pelatihan pada UI.
	 *
	 * @return nama jenis pelatihan
	 */
	public String toString() {
		return nama;
	}

	private String kode;
	private String nama;
	private String keterangan;
	private Boolean defaultItem = false;
	private JenisPelatihan parent;
	private Integer deep;
	private Long jmlDipakai = 0L;

	/**
	 * Konstruktor tanpa argumen yang dipersyaratkan Hibernate/JPA untuk instansiasi entity lewat
	 * reflection. Tidak menginisialisasi field lain di luar default deklarasi field ({@code
	 * defaultItem = false}, {@code jmlDipakai = 0L}).
	 */
	public JenisPelatihan() {
	}

	/**
	 * Mengembalikan primary key baris jenis pelatihan ini.
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
	 * Mengembalikan nama jenis pelatihan, di-trim (whitespace di awal/akhir dibuang) setiap kali
	 * dibaca.
	 *
	 * @return nama jenis pelatihan hasil trim, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan nama jenis pelatihan. Nilai disimpan apa adanya; trim baru terjadi saat dibaca
	 * lewat {@link #getNama()}.
	 *
	 * @param nama nama baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas untuk jenis pelatihan ini.
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
	 * Mengembalikan flag default item pada pohon jenis pelatihan (dipakai mis. sebagai filter
	 * {@code Restrictions.eq("defaultItem", true)} saat memuat daftar bantuan/lookup jenis
	 * pelatihan). <b>Bukan getter murni:</b> bila field belum pernah diisi ({@code null}), method
	 * ini menuliskan {@code true} ke {@link #defaultItem} sebelum mengembalikannya — lihat catatan
	 * "Getter dengan efek samping" pada Javadoc kelas ini. Baris baru yang belum pernah disimpan
	 * membawa default konstruktor {@code false}, jadi efek samping ini hanya teramati pada baris
	 * yang dimuat dari database dengan kolom {@code default_item} bernilai {@code NULL}.
	 *
	 * @return {@code true}/{@code false} status default item; tidak pernah mengembalikan {@code
	 *         null} karena efek samping di atas
	 */
	public Boolean getDefaultItem() {
		if (defaultItem == null) {
			defaultItem = true;
		}
		return defaultItem;
	}

	/**
	 * Menetapkan flag default item.
	 *
	 * @param defaultItem status default item baru
	 */
	public void setDefaultItem(Boolean defaultItem) {
		this.defaultItem = defaultItem;
	}

	/**
	 * Mengembalikan node induk (parent) jenis pelatihan ini pada struktur pohon. Relasi
	 * self-referential ke tabel yang sama ({@code employ.jenis_pelatihan}); {@code null} berarti
	 * node ini adalah akar (root) pohon.
	 *
	 * @return jenis pelatihan induk, atau {@code null} bila node ini akar pohon
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "parent", nullable = true)
	public JenisPelatihan getParent() {
		return parent;
	}

	/**
	 * Menetapkan node induk pada struktur pohon.
	 *
	 * @param parent jenis pelatihan induk baru, atau {@code null} untuk menjadikan node ini akar
	 */
	public void setParent(JenisPelatihan parent) {
		this.parent = parent;
	}

	/**
	 * Mengembalikan kedalaman (level) node ini pada struktur pohon jenis pelatihan, dipakai
	 * {@code JenisPelatihanTreeModel}/{@code JenisPelatihanTreeAction} untuk menyusun indentasi dan
	 * urutan tampil tree. Nilai ini tidak dihitung ulang otomatis dari {@link #getParent()} oleh
	 * getter ini — pemeliharaan konsistensinya menjadi tanggung jawab kode pemanggil yang menyusun
	 * pohon.
	 *
	 * @return kedalaman node, atau {@code null} bila belum diset
	 */
	public Integer getDeep() {
		return deep;
	}

	/**
	 * Menetapkan kedalaman node pada struktur pohon.
	 *
	 * @param deep kedalaman node baru
	 */
	public void setDeep(Integer deep) {
		this.deep = deep;
	}

	/**
	 * Mengembalikan jumlah pemakaian jenis pelatihan ini (mis. berapa kali dirujuk oleh riwayat
	 * pelatihan pegawai). Default {@code 0L} bila belum pernah diset ulang lewat
	 * {@link #setJmlDipakai(Long)}; nilai ini murni state yang dibaca, tidak dihitung ulang secara
	 * live dari tabel riwayat oleh getter ini — pemanggil bertanggung jawab menjaga nilainya tetap
	 * akurat.
	 *
	 * @return jumlah pemakaian, tidak pernah {@code null} kecuali diset eksplisit ke {@code null}
	 */
	public Long getJmlDipakai() {
		return jmlDipakai;
	}

	/**
	 * Menetapkan jumlah pemakaian jenis pelatihan ini.
	 *
	 * @param jmlDipakai jumlah pemakaian baru
	 */
	public void setJmlDipakai(Long jmlDipakai) {
		this.jmlDipakai = jmlDipakai;
	}

	/**
	 * Mengembalikan kode singkat jenis pelatihan ini.
	 *
	 * @return kode jenis pelatihan, atau {@code null} bila belum diisi
	 */
	public String getKode() {
		return kode;
	}

	/**
	 * Menetapkan kode singkat jenis pelatihan.
	 *
	 * @param kode kode baru
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

}
