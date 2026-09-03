package ais.database.model.asset;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;
import ais.database.model.Ruang;
import ais.database.model.Tbmuser;
import ais.database.model.rab.SatuanKerja;

/**
 * Kepemilikan satu jenis aset oleh satu satuan kerja -- tingkat TENGAH dari tiga tingkat
 * pencatatan aset tetap / inventaris AIS.
 *
 * <h3>Posisi dalam hirarki tiga tingkat</h3>
 *
 * <p>Modul aset AIS TIDAK memakai pola dua tingkat "header + detail" seperti dokumen transaksi
 * (pemesanan, penerimaan, pembayaran) di paket yang sama. Ia memakai TIGA tingkat, dan urutannya
 * dapat diverifikasi dari arah kolom FK-nya, bukan dari namanya:</p>
 *
 * <ol>
 * <li>{@link MasterAsset} -- KATALOG / definisi jenis barang. Tidak menyimpan kolom FK ke
 *     {@code Asset} maupun {@code AssetDetail}. Satu baris berarti "Laptop Dell XPS 13",
 *     lengkap dengan merk, spesifikasi, satuan, umur ekonomis default, harga beli default,
 *     dan pemetaan akun akuntansinya. Dipakai bersama SELURUH satuan kerja;</li>
 * <li>{@code Asset} (kelas ini) -- kolom {@code master_asset} menunjuk ke katalog di atas, dan
 *     kolom {@code satuan_kerja} menunjuk ke pemiliknya. Satu baris berarti "satuan kerja X
 *     memegang Laptop Dell XPS 13 (dari pengadaan / saldo awal Y)". Inilah simpul yang
 *     mengaitkan katalog global ke tenant, sekaligus jangkar dokumen asal-usulnya
 *     ({@link #getPermintaanPengadaanMasterAssetDetail()} atau
 *     {@link #getSaldoAwalMasterAssetDetail()});</li>
 * <li>{@link AssetDetail} -- kolom {@code asset} menunjuk BALIK ke kelas ini. Satu baris berarti
 *     SATU UNIT FISIK yang bisa dipegang tangan: punya barcode / nomor inventaris sendiri, harga
 *     beli sendiri, tanggal beli sendiri, lokasi dan ruang sendiri, serta jadwal penyusutannya
 *     sendiri ({@link PenyusutanAsset} ber-FK ke {@code AssetDetail}, BUKAN ke {@code Asset}).</li>
 * </ol>
 *
 * <p>Jadi "Laptop Dell XPS 13" adalah satu {@code MasterAsset}; "5 laptop XPS milik Fakultas
 * Teknik" adalah satu {@code Asset}; dan tiap laptop bernomor seri masing-masing adalah satu
 * {@code AssetDetail}. Bukti tambahan bahwa {@code Asset} adalah induk pengelompokan (bukan
 * unit fisik): {@code AssetDetail.generateBarcode} menghitung jumlah baris {@code AssetDetail}
 * yang ber-FK ke {@code Asset} yang sama untuk memperoleh nomor urut unit; nomor urut itu
 * mustahil bermakna kalau {@code Asset} sendiri sudah berupa unit tunggal.</p>
 *
 * <h3>Isolasi tenant</h3>
 *
 * <p>Entitas ini memegang kolom {@code satuan_kerja} SECARA LANGSUNG -- pola satu tingkat,
 * seperti {@code Toko} pada modul inventory, bukan pola dua tingkat tak langsung seperti
 * {@code Koperasi}. Namun nilainya BUKAN nilai otoritatif yang diisi pengguna: lihat
 * {@link #getSatuanKerja()}, yang menurunkannya ulang dari rantai dokumen pengadaan setiap kali
 * dipanggil. {@link MasterAsset} dan {@link KelompokAsset} sama sekali tidak punya kolom tenant,
 * sehingga katalog aset bersifat GLOBAL lintas satuan kerja.</p>
 *
 * <h3>Bidang audit bayangan</h3>
 *
 * <p>{@link #getOleh()}, {@link #getOlehId()}, dan {@link #getTanggal_dirubah()} diulang di
 * hampir setiap entitas AIS. Pengulangan itu KEHARUSAN TEKNIS, bukan duplikasi ceroboh:
 * {@link GeneralValueObject} adalah kelas induk biasa, bukan {@code @Entity} maupun
 * {@code @MappedSuperclass}, sehingga Hibernate tidak akan mewarisi pemetaan kolom apa pun
 * darinya. Setiap tabel harus mendeklarasikan sendiri kolom auditnya.</p>
 *
 * <h3>Riwayat versi</h3>
 *
 * <p>{@code @Audited} (Hibernate Envers) merekam tiap revisi baris ke tabel bayangan
 * {@code asset.asset_AUD}. {@code dynamicInsert}/{@code dynamicUpdate} membuat Hibernate
 * menyusun SQL hanya dari kolom yang benar-benar berubah -- penting pada entitas berkolom
 * banyak yang biasanya hanya disunting sebagian.</p>
 *
 * @see MasterAsset katalog / definisi jenis aset di atasnya
 * @see AssetDetail unit fisik individual di bawahnya
 * @see PenyusutanAsset jadwal penyusutan, melekat pada {@code AssetDetail}
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "asset", name = "asset")
public class Asset extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java.
	 *
	 * <p>Nilainya sengaja SAMA dengan hampir seluruh entitas lain di paket ini karena seluruh
	 * berkas dihasilkan satu kali oleh hbm2java dari templat yang sama. Kesamaan itu tidak
	 * menimbulkan masalah: {@code serialVersionUID} hanya dibandingkan antar-versi kelas YANG
	 * SAMA, tidak pernah antar-kelas berbeda.</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama baris, kolom {@code id}; di-generate database (IDENTITY). */
	private Long id;

	/** Nama tampil pengguna terakhir yang menyunting baris ini; lihat {@link #getOleh()}. */
	private String oleh;

	/** Id pengguna terakhir yang menyunting baris ini; lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Id pengguna terakhir yang menyunting baris ini.
	 *
	 * @return id pengguna penyunting terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna penyunting terakhir, dengan MENGABAIKAN nilai kosong.
	 *
	 * <p>Argumen {@code null} atau hanya berisi spasi tidak menimpa nilai lama. Perilaku ini
	 * disengaja: pemanggil yang tidak mengetahui pengguna aktif (mis. proses batch atau
	 * penyalinan objek) tidak boleh MENGHAPUS jejak audit yang sudah tercatat.</p>
	 *
	 * @param olehId id pengguna penyunting; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna penyunting terakhir, dengan MENGABAIKAN nilai kosong.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}: nilai kosong tidak menimpa jejak audit lama.</p>
	 *
	 * @param oleh nama pengguna penyunting; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Nama tampil pengguna terakhir yang menyunting baris ini.
	 *
	 * @return nama penyunting terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}: memperbarui stempel waktu audit tepat sebelum UPDATE dikirim.
	 *
	 * <p>Pekerjaannya didelegasikan ke {@code AuditTimestampInterceptor.ubah(this)} agar aturan
	 * penulisan stempel waktu terpusat di satu tempat untuk seluruh entitas.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu penyuntingan terakhir.
	 *
	 * <p>Diberi nilai awal waktu server saat objek dibuat, sehingga baris baru sudah bertanggal
	 * meski {@code @PreUpdate} belum pernah berjalan. {@code WaktuUtil.getDate()} dipakai
	 * (bukan {@code new Date()}) agar seluruh aplikasi memakai satu sumber waktu yang sama.</p>
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi stempel waktu penyuntingan terakhir.
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Stempel waktu penyuntingan terakhir baris ini.
	 *
	 * @return stempel waktu; tidak pernah {@code null} pada objek yang dibuat lewat konstruktor
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks "id-nama", dipakai komponen ZK untuk label combobox dan listbox.
	 *
	 * <p>Perhatikan bahwa bagian nama diambil lewat {@link #getNama()}, BUKAN dari field
	 * {@code nama} langsung. Karena {@code getNama()} menyalin nama dari katalog dan menimpa
	 * field-nya (lihat dokumentasi getter tersebut), memanggil {@code toString()} -- termasuk
	 * secara tidak sengaja lewat perangkaian teks di log -- ikut mengubah keadaan objek.</p>
	 *
	 * @return teks berbentuk {@code "<id>-<nama>"}
	 */
	public String toString() {
		return id + "-" + getNama();
	}

	/** Katalog / jenis aset yang diacu baris ini; lihat {@link #getMasterAsset()}. */
	private MasterAsset masterAsset;

	/** Satuan kerja pemilik; lihat peringatan penting di {@link #getSatuanKerja()}. */
	private SatuanKerja satuanKerja;

	/** Pihak pemilik legal aset (mis. yayasan, pihak ketiga); lihat {@link #getPemilikAsset()}. */
	private PemilikAsset pemilikAsset;

	/** Lokasi (gedung/kampus) default bagi seluruh unit di bawah baris ini. */
	private Lokasi lokasi;

	/** Ruang default bagi seluruh unit di bawah baris ini. */
	private Ruang ruang;

	/** Pengguna penanggung jawab / pemegang aset. */
	private Tbmuser tbmuser;

	/** Tanggal persetujuan pencatatan aset; lihat {@link #getTanggalPersetujuan()}. */
	private Date tanggalPersetujuan;

	/** Pengguna pembuat baris; kolomnya {@code nullable = false}. */
	private Tbmuser dibuatOleh;

	/** Pengguna penyetuju; {@code null} selama pencatatan belum disetujui. */
	private Tbmuser disetujuiOleh;

	/** Nama aset; DISALIN dari katalog oleh {@link #getNama()}, jangan diandalkan sebagai nilai bebas. */
	private String nama;

	/** Keterangan bebas yang diketik pengguna. */
	private String keterangan;

	/** Baris saldo awal asal-usul aset ini, bila aset berasal dari saldo awal / penerimaan pengadaan. */
	private SaldoAwalMasterAssetDetail saldoAwalMasterAssetDetail;

	/** Baris permintaan pengadaan asal-usul aset ini, bila aset berasal dari alur pengadaan. */
	private PermintaanPengadaanMasterAssetDetail permintaanPengadaanMasterAssetDetail;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate dan ZK data binding.
	 *
	 * <p>Tidak mengisi apa pun; seluruh nilai awal ditetapkan lewat inisialisasi field
	 * (mis. {@link #tanggal_dirubah}) atau oleh pemanggil.</p>
	 */
	public Asset() {
	}

	/**
	 * Kunci utama baris.
	 *
	 * <p>{@code insertable = false} karena nilainya di-generate database (kolom serial /
	 * auto-increment), sehingga Hibernate tidak boleh menyertakannya dalam INSERT.</p>
	 *
	 * @return id baris, atau {@code null} bila objek belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi kunci utama.
	 *
	 * <p>Umumnya hanya dipanggil Hibernate seusai INSERT; kode aplikasi jarang perlu memanggilnya.</p>
	 *
	 * @param id kunci utama baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Nama aset -- getter DESTRUKTIF yang menyalin nama dari katalog.
	 *
	 * <p><b>Efek samping.</b> Sebelum mengembalikan nilai, getter ini memanggil
	 * {@link #getMasterAsset()} dan, bila katalog terisi, MENIMPA field {@link #nama} dengan
	 * {@code masterAsset.getNama()}. Entitas ini memakai akses PROPERTI (anotasi {@code @Id}
	 * terpasang di getter), sehingga Hibernate memanggil getter ini juga saat pemeriksaan
	 * perubahan (dirty checking) pada tiap flush. Konsekuensinya nilai hasil salinan itu ikut
	 * TERTULIS PERMANEN ke kolom {@code nama} tabel {@code asset.asset}, walaupun tidak ada satu
	 * pun baris kode aplikasi yang memanggil {@link #setNama(String)}.</p>
	 *
	 * <p><b>Akibat yang perlu diketahui pemanggil.</b> Nama aset TIDAK dapat dibedakan dari nama
	 * katalognya: setiap upaya memberi nama khusus pada baris {@code Asset} akan hilang pada
	 * flush berikutnya selama {@code master_asset} terisi. Bila suatu saat nama katalog diubah
	 * (mis. koreksi ejaan pada {@code MasterAsset}), seluruh baris {@code Asset} yang menunjuk
	 * katalog itu akan ikut berubah namanya secara diam-diam begitu tersentuh sesi Hibernate
	 * mana pun -- termasuk sesi yang hanya membaca lalu menutup dengan flush otomatis. Untuk
	 * pelaporan historis, nama yang tercatat di baris aset karena itu BUKAN nama pada saat
	 * perolehan, melainkan nama katalog terkini.</p>
	 *
	 * <p><b>Jalur pemanggilan tersembunyi.</b> {@link #toString()} memanggil getter ini, sehingga
	 * merangkai objek {@code Asset} ke dalam teks log atau pesan kesalahan pun cukup untuk
	 * memicu penyalinan. Ini pola yang sama dengan getter destruktif yang telah berulang kali
	 * ditemukan pada entitas keuangan AIS lain; catatan ini ada agar pembaca berikutnya tidak
	 * menyangka field {@code nama} pada tabel aset dapat disunting bebas.</p>
	 *
	 * @return nama aset (hasil {@code trim()}), atau {@code null} bila belum terisi sama sekali
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		masterAsset = getMasterAsset();
		if (masterAsset != null) {
			nama = masterAsset.getNama();
		}
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi nama aset.
	 *
	 * <p>Perlu dicatat: nilai yang diisi di sini akan DITIMPA oleh {@link #getNama()} selama
	 * {@link #getMasterAsset()} tidak {@code null}. Setter ini praktis hanya berpengaruh pada
	 * aset tanpa katalog.</p>
	 *
	 * @param nama nama aset baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Keterangan bebas untuk baris aset ini.
	 *
	 * @return keterangan apa adanya, boleh {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi keterangan bebas.
	 *
	 * @param keterangan teks keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Pihak pemilik legal aset (mis. yayasan sendiri, hibah, atau milik pihak ketiga).
	 *
	 * <p>Dipanggil melalui {@code check(...)} agar proxy lazy Hibernate diresolusi lebih dahulu;
	 * tanpa itu pemanggil di luar sesi bisa menerima proxy yang melempar
	 * {@code LazyInitializationException}. Nilai ini menjadi warisan default bagi unit di
	 * bawahnya -- lihat {@code AssetDetail.getPemilikAsset()}.</p>
	 *
	 * @return pemilik aset, atau {@code null} bila belum ditetapkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pemilik_asset", nullable = true)
	public PemilikAsset getPemilikAsset() {
		pemilikAsset = check(pemilikAsset);
		return pemilikAsset;
	}

	/**
	 * Menetapkan pihak pemilik legal aset.
	 *
	 * @param pemilikAsset pemilik baru, boleh {@code null}
	 */
	public void setPemilikAsset(PemilikAsset pemilikAsset) {
		this.pemilikAsset = pemilikAsset;
	}

	/**
	 * Lokasi (gedung / kampus) tempat aset ini berada.
	 *
	 * <p>Berfungsi sebagai nilai default bagi unit fisik di bawahnya: bila
	 * {@code AssetDetail.lokasi} kosong, unit tersebut mewarisi lokasi dari sini.</p>
	 *
	 * @return lokasi, atau {@code null} bila belum ditetapkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "lokasi", nullable = true)
	public Lokasi getLokasi() {
		lokasi = check(lokasi);
		return lokasi;
	}

	/**
	 * Menetapkan lokasi aset.
	 *
	 * @param lokasi lokasi baru, boleh {@code null}
	 */
	public void setLokasi(Lokasi lokasi) {
		this.lokasi = lokasi;
	}

	/**
	 * Pengguna penanggung jawab / pemegang aset.
	 *
	 * @return pengguna penanggung jawab, atau {@code null} bila belum ditetapkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tbmuser", nullable = true)
	public Tbmuser getTbmuser() {
		tbmuser = check(tbmuser);
		return tbmuser;
	}

	/**
	 * Menetapkan pengguna penanggung jawab aset.
	 *
	 * @param tbmuser pengguna baru, boleh {@code null}
	 */
	public void setTbmuser(Tbmuser tbmuser) {
		this.tbmuser = tbmuser;
	}

	/**
	 * Ruang tempat aset ini ditempatkan.
	 *
	 * <p>Seperti {@link #getLokasi()}, menjadi nilai default bagi unit fisik di bawahnya.</p>
	 *
	 * @return ruang, atau {@code null} bila belum ditetapkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "ruang", nullable = true)
	public Ruang getRuang() {
		ruang = check(ruang);
		return this.ruang;
	}

	/**
	 * Menetapkan ruang penempatan aset.
	 *
	 * @param ruang ruang baru, boleh {@code null}
	 */
	public void setRuang(Ruang ruang) {
		this.ruang = ruang;
	}

	/**
	 * Menetapkan pengguna pembuat baris.
	 *
	 * @param dibuatOleh pengguna pembuat
	 */
	public void setDibuatOleh(Tbmuser dibuatOleh) {
		this.dibuatOleh = dibuatOleh;
	}

	/**
	 * Pengguna yang membuat baris aset ini.
	 *
	 * <p>Kolom {@code dibuat_oleh} dideklarasikan {@code nullable = false}, sehingga baris aset
	 * tidak dapat disimpan tanpa pembuat.</p>
	 *
	 * @return pengguna pembuat
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dibuat_oleh", nullable = false)
	public Tbmuser getDibuatOleh() {
		dibuatOleh = check(dibuatOleh);
		return dibuatOleh;
	}

	/**
	 * Menetapkan pengguna penyetuju pencatatan aset.
	 *
	 * @param disetujuiOleh pengguna penyetuju, boleh {@code null}
	 */
	public void setDisetujuiOleh(Tbmuser disetujuiOleh) {
		this.disetujuiOleh = disetujuiOleh;
	}

	/**
	 * Katalog / jenis aset yang diacu baris ini.
	 *
	 * <p>Inilah tautan ke tingkat teratas hirarki (lihat uraian di dokumentasi kelas). Dari
	 * katalog inilah {@link #getNama()} menyalin nama, dan dari sana pula unit fisik di bawahnya
	 * mewarisi harga beli default, nilai minimal, serta umur ekonomis.</p>
	 *
	 * @return katalog aset, atau {@code null} bila aset dicatat tanpa katalog
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "master_asset", nullable = true)
	public MasterAsset getMasterAsset() {
		masterAsset = check(masterAsset);
		return masterAsset;
	}

	/**
	 * Menetapkan katalog / jenis aset.
	 *
	 * @param masterAsset katalog baru, boleh {@code null}
	 */
	public void setMasterAsset(MasterAsset masterAsset) {
		this.masterAsset = masterAsset;
	}

	/**
	 * Pengguna yang menyetujui pencatatan aset ini.
	 *
	 * <p>{@code null} berarti pencatatan belum disetujui. Pasangannya adalah
	 * {@link #getTanggalPersetujuan()}; keduanya tidak saling memaksa lewat batasan basis data,
	 * sehingga kode pemanggil sebaiknya memeriksa yang ia butuhkan sendiri.</p>
	 *
	 * @return pengguna penyetuju, atau {@code null} bila belum disetujui
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disetujui_oleh", nullable = true)
	public Tbmuser getDisetujuiOleh() {
		disetujuiOleh = check(disetujuiOleh);
		return disetujuiOleh;
	}

	/**
	 * Menetapkan tanggal persetujuan pencatatan aset.
	 *
	 * @param tanggalPersetujuan tanggal persetujuan, boleh {@code null}
	 */
	public void setTanggalPersetujuan(Date tanggalPersetujuan) {
		this.tanggalPersetujuan = tanggalPersetujuan;
	}

	/**
	 * Tanggal persetujuan pencatatan aset.
	 *
	 * @return tanggal persetujuan, atau {@code null} bila belum disetujui
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_persetujuan")
	public Date getTanggalPersetujuan() {
		return tanggalPersetujuan;
	}

	/**
	 * Baris rincian saldo awal / penerimaan pengadaan yang melahirkan aset ini.
	 *
	 * <p>Terisi bila aset masuk lewat jalur saldo awal atau lewat penerimaan barang pengadaan.
	 * Rantai inilah yang dibaca {@link #getSatuanKerja()} dan
	 * {@code AssetDetail.getHargaBeli()} / {@code AssetDetail.getTanggalBeli()} untuk menurunkan
	 * pemilik dan nilai perolehan. Tidak memakai {@code check(...)}: relasi ini sengaja
	 * di-{@code Fetch(FetchMode.SELECT)} sehingga Hibernate menerbitkan SELECT terpisah alih-alih
	 * ikut dalam JOIN besar.</p>
	 *
	 * @return baris saldo awal asal-usul, atau {@code null} bila aset tidak berasal dari jalur itu
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "saldo_awal_master_asset_detail", nullable = true)
	public SaldoAwalMasterAssetDetail getSaldoAwalMasterAssetDetail() {
		return saldoAwalMasterAssetDetail;
	}

	/**
	 * Menetapkan baris saldo awal asal-usul aset.
	 *
	 * @param saldoAwalMasterAssetDetail baris saldo awal, boleh {@code null}
	 */
	public void setSaldoAwalMasterAssetDetail(SaldoAwalMasterAssetDetail saldoAwalMasterAssetDetail) {
		this.saldoAwalMasterAssetDetail = saldoAwalMasterAssetDetail;
	}

	/**
	 * Satuan kerja pemilik aset -- getter DESTRUKTIF yang MENURUNKAN ULANG pemilik dari rantai
	 * dokumen asal-usul.
	 *
	 * <h3>Apa yang sebenarnya dilakukan</h3>
	 *
	 * <p>Getter ini tidak sekadar membaca kolom {@code satuan_kerja}. Setelah meresolusi proxy
	 * dengan {@code check(...)}, ia menelusuri tiga rantai dokumen secara berurutan dan
	 * MENIMPA field {@link #satuanKerja} dengan yang pertama ditemukan:</p>
	 *
	 * <ol>
	 * <li>{@code permintaanPengadaanMasterAssetDetail -> perjanjianKerjasamaMasterAsset ->
	 *     satuanKerja} -- jalur aset yang lahir dari pengadaan berbasis perjanjian kerja sama;</li>
	 * <li>{@code saldoAwalMasterAssetDetail -> penerimaanPengadaanMasterAssetDetail ->
	 *     penerimaanPengadaanMasterAsset -> satuanKerja} -- jalur aset yang lahir dari penerimaan
	 *     barang pengadaan;</li>
	 * <li>{@code saldoAwalMasterAssetDetail -> saldoAwal -> satuanKerja} -- jalur aset yang
	 *     dicatat sebagai saldo awal murni (migrasi data lama, hibah, temuan inventarisasi).</li>
	 * </ol>
	 *
	 * <p>Bila ketiganya tidak menghasilkan apa-apa, nilai kolom yang tersimpan dipakai apa adanya.
	 * Perhatikan bahwa cabang-cabang itu ditulis sebagai {@code if / else if} SETELAH pemanggilan
	 * {@code check(...)}, sehingga urutan di atas benar-benar prioritas: jalur pengadaan
	 * mengalahkan jalur saldo awal, dan jalur penerimaan mengalahkan saldo awal murni.</p>
	 *
	 * <h3>Mengapa ini penting bagi pemanggil</h3>
	 *
	 * <p><b>Pertama, nilai kolom bukan nilai otoritatif.</b> Karena entitas ini memakai akses
	 * PROPERTI, Hibernate ikut memanggil getter ini saat pemeriksaan perubahan. Nilai hasil
	 * penurunan karena itu tertulis permanen ke kolom {@code satuan_kerja} pada flush berikutnya.
	 * Artinya {@link #setSatuanKerja(SatuanKerja)} yang dipanggil kode aplikasi akan HILANG
	 * selama salah satu rantai di atas terisi. Untuk aset yang lahir dari pengadaan, satu-satunya
	 * cara mengubah pemiliknya adalah mengubah dokumen pengadaannya, bukan barisan asetnya.</p>
	 *
	 * <p><b>Kedua, pemilik dapat berpindah tanpa ada yang menyentuh baris aset.</b> Bila satuan
	 * kerja pada perjanjian kerja sama atau pada dokumen penerimaan diperbaiki, seluruh aset
	 * turunannya ikut berpindah tenant secara diam-diam pada akses berikutnya. Ini konsisten
	 * bila dianggap sebagai koreksi, tetapi berarti riwayat kepemilikan tidak terekam pada
	 * tabel aset -- hanya tabel Envers ({@code asset.asset_AUD}) yang menyimpan jejaknya.</p>
	 *
	 * <p><b>Ketiga, biaya akses.</b> Rantai terpanjang menyentuh empat entitas. Dua di antaranya
	 * ({@code saldoAwalMasterAssetDetail} dan {@code permintaanPengadaanMasterAssetDetail})
	 * dipetakan {@code Fetch(FetchMode.SELECT)}, sehingga memanggil getter ini pada setiap baris
	 * sebuah daftar akan menerbitkan rentetan SELECT satu per baris. Pada laporan aset yang
	 * panjang, itulah sumber lambatnya -- bukan kueri utamanya.</p>
	 *
	 * <h3>Kaitan dengan isolasi tenant</h3>
	 *
	 * <p>Kolom ini adalah SATU-SATUNYA penanda tenant pada modul aset yang berdiri langsung di
	 * entitas (pola satu tingkat). {@link MasterAsset} dan {@link KelompokAsset} tidak memilikinya
	 * sama sekali, sehingga katalog dan kelompok aset dipakai bersama seluruh satuan kerja; hanya
	 * mulai dari tingkat {@code Asset} ke bawah data terpisah per tenant. Setiap penyaringan
	 * tenant pada modul aset karena itu harus dilakukan di tingkat {@code Asset} atau
	 * {@code AssetDetail}, tidak pernah di tingkat katalog.</p>
	 *
	 * <p>Perlu diketahui pula bahwa penurunan ini terjadi di dalam Java, BUKAN di dalam SQL.
	 * Kueri yang menyaring langsung pada kolom {@code satuan_kerja} akan melihat nilai yang
	 * TERSIMPAN -- yang bisa saja belum pernah diperbarui bila baris tersebut belum pernah
	 * tersentuh flush sejak dokumen asalnya berubah. Perbedaan antara nilai tersimpan dan nilai
	 * terhitung inilah yang perlu diingat saat mencocokkan hasil laporan berbasis kueri dengan
	 * hasil tampilan berbasis objek.</p>
	 *
	 * @return satuan kerja pemilik hasil penurunan, atau nilai tersimpan bila rantai dokumen
	 *         tidak menghasilkan apa-apa; boleh {@code null}
	 * @see AssetDetail#getSatuanKerja() penurunan lanjutan di tingkat unit fisik
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);

		if (getPermintaanPengadaanMasterAssetDetail() != null
				&& getPermintaanPengadaanMasterAssetDetail().getPerjanjianKerjasamaMasterAsset() != null
				&& getPermintaanPengadaanMasterAssetDetail().getPerjanjianKerjasamaMasterAsset()
						.getSatuanKerja() != null) {
			satuanKerja = getPermintaanPengadaanMasterAssetDetail().getPerjanjianKerjasamaMasterAsset()
					.getSatuanKerja();
		} else if (getSaldoAwalMasterAssetDetail() != null
				&& getSaldoAwalMasterAssetDetail().getPenerimaanPengadaanMasterAssetDetail() != null
				&& getSaldoAwalMasterAssetDetail().getPenerimaanPengadaanMasterAssetDetail()
						.getPenerimaanPengadaanMasterAsset() != null
				&& getSaldoAwalMasterAssetDetail().getPenerimaanPengadaanMasterAssetDetail()
						.getPenerimaanPengadaanMasterAsset().getSatuanKerja() != null) {
			satuanKerja = getSaldoAwalMasterAssetDetail().getPenerimaanPengadaanMasterAssetDetail()
					.getPenerimaanPengadaanMasterAsset().getSatuanKerja();
		} else if (getSaldoAwalMasterAssetDetail() != null && getSaldoAwalMasterAssetDetail().getSaldoAwal() != null
				&& getSaldoAwalMasterAssetDetail().getSaldoAwal().getSatuanKerja() != null) {
			satuanKerja = getSaldoAwalMasterAssetDetail().getSaldoAwal().getSatuanKerja();
		}

		return satuanKerja;
	}

	/**
	 * Menetapkan satuan kerja pemilik.
	 *
	 * <p>Perlu dicatat bahwa nilai yang diisi di sini akan DITIMPA oleh {@link #getSatuanKerja()}
	 * bila salah satu rantai dokumen asal-usul terisi. Setter ini efektif hanya untuk aset yang
	 * dicatat manual tanpa dokumen pengadaan maupun saldo awal.</p>
	 *
	 * @param satuanKerja satuan kerja pemilik baru, boleh {@code null}
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/**
	 * Baris permintaan pengadaan yang melahirkan aset ini.
	 *
	 * <p>Terisi bila aset masuk lewat alur pengadaan (permintaan, pemesanan, penerimaan). Dari
	 * rantai ini {@link #getSatuanKerja()} mengambil pemilik dengan prioritas TERTINGGI, dan
	 * {@code AssetDetail.getHargaBeli()} mengambil nilai perolehan dari pertanggungjawaban uang
	 * mukanya. Sama seperti {@link #getSaldoAwalMasterAssetDetail()}, tidak melewati
	 * {@code check(...)} dan dipetakan {@code Fetch(FetchMode.SELECT)}.</p>
	 *
	 * @return baris permintaan pengadaan asal-usul, atau {@code null} bila aset tidak berasal
	 *         dari alur pengadaan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "permintaan_pengadaan_master_asset_detail", nullable = true)
	public PermintaanPengadaanMasterAssetDetail getPermintaanPengadaanMasterAssetDetail() {
		return permintaanPengadaanMasterAssetDetail;
	}

	/**
	 * Menetapkan baris permintaan pengadaan asal-usul aset.
	 *
	 * @param permintaanPengadaanMasterAssetDetail baris permintaan pengadaan, boleh {@code null}
	 */
	public void setPermintaanPengadaanMasterAssetDetail(
			PermintaanPengadaanMasterAssetDetail permintaanPengadaanMasterAssetDetail) {
		this.permintaanPengadaanMasterAssetDetail = permintaanPengadaanMasterAssetDetail;
	}

}
