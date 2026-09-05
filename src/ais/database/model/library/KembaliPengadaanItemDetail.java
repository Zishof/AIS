package ais.database.model.library;

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

/**
 * Entity <b>baris rincian</b> dokumen pengembalian buku oleh anggota (tabel
 * {@code library.kembali_pengadaan_item_detail}). Satu baris menyatakan satu eksemplar yang
 * dikembalikan, lengkap dengan tanggal pengembaliannya, denda keterlambatan yang dikenakan, dan
 * status pelunasan denda tersebut. Baris ini menunjuk balik ke header
 * {@link KembaliPengadaanItem} sekaligus ke baris peminjaman asalnya
 * ({@link PeminjamanPengadaanItemDetail}).
 *
 * <h3>Peran ganda: bukti pengembalian sekaligus buku besar denda</h3>
 * <p>Berbeda dari baris rincian dokumen lain di modul {@code library} yang hanya mencatat barang
 * dan kuantitas, entity ini juga berfungsi sebagai satu-satunya tempat penyimpanan denda
 * keterlambatan. Empat field terkait denda bekerja bersama:</p>
 * <ul>
 *   <li>{@link #getDenda() denda} &mdash; besaran denda yang <em>dikenakan</em>, dihitung
 *       {@code LibraryUtil.hitungDendaItem(...)} dari {@link DendaKeterlambatanItem} yang cocok
 *       dengan profil anggota lalu dikalikan kuantitas, ditambah biaya penggantian bila buku
 *       hilang/rusak;</li>
 *   <li>{@link #getDibayarSejumlah() dibayarSejumlah} &mdash; nominal yang sudah
 *       <em>dibayarkan</em> anggota;</li>
 *   <li>{@link #getTelahDibayar() telahDibayar} &mdash; penanda bahwa denda dianggap
 *       <em>selesai</em>, baik karena lunas maupun karena dibebaskan petugas;</li>
 *   <li>{@link #getKetDenda() ketDenda} &mdash; catatan riwayat denda; jalur API
 *       ({@code LibraryOperationsApi}) menambahkan baris log berisi jenis peristiwa
 *       (pembayaran/pembebasan/pembatalan), nominal, tanggal, dan pelaku.</li>
 * </ul>
 *
 * <p><b>Peringatan penting: {@code dibayarSejumlah} tidak menyimpan pembayaran sebagian.</b>
 * Getter {@link #getDibayarSejumlah()} <b>tidak</b> mengembalikan nilai yang tersimpan apa
 * adanya. Ia menerapkan dua aturan sekaligus: bila {@code telahDibayar} bernilai salah, ia
 * mengembalikan {@code 0.0} berapa pun nilai field sesungguhnya; dan bila {@code telahDibayar}
 * benar sedangkan nominalnya kosong atau di bawah {@code 0.01}, ia mengembalikan seluruh
 * {@link #getDenda() denda}. Karena Hibernate memetakan entity ini dengan <i>property access</i>
 * (anotasi terpasang pada getter), nilai yang dikembalikan getter itulah yang ditulis ke basis
 * data pada setiap <i>flush</i>. Akibatnya pembayaran sebagian &mdash; anggota membayar sebagian
 * denda dan sisanya belum lunas, sehingga {@code telahDibayar} masih salah &mdash; akan
 * <b>tertulis nol</b> ke kolomnya dan hilang tanpa jejak. Konsekuensi lanjutannya: laporan
 * pembayaran denda ({@code LibraryUtil} menyaring {@code dibayarSejumlah &gt; 0.1}) dan
 * penjumlahan pada dasbor perpustakaan sama-sama kehilangan angka tersebut. Rinciannya
 * dijelaskan pada Javadoc {@link #getDibayarSejumlah()}.</p>
 *
 * <p><b>Penegakan denda: ada, tetapi hanya pada perhitungan.</b> Modul ini benar-benar
 * menghitung dan menyimpan denda (bukan sekadar tabel referensi deskriptif): tarif diambil dari
 * {@link DendaKeterlambatanItem}, tenggat dari {@link BatasWaktuPeminjamanItem}, dan hasilnya
 * dituliskan ke kolom {@link #getDenda() denda} pada baris ini. Yang <b>tidak</b> ada adalah
 * penegakan di sisi berikutnya: tidak ada mekanisme di model maupun di alur peminjaman yang
 * memblokir anggota meminjam lagi selama masih ada baris dengan {@code denda &gt; 0} dan
 * {@code telahDibayar} salah. Pemblokiran anggota dilakukan lewat entity terpisah
 * {@code AnggotaYangDiblokir} yang diisi manual petugas, bukan otomatis dari tunggakan denda.</p>
 *
 * <p><b>Efek samping.</b> Accessor dan mutator hanya membaca atau mengubah state di memori,
 * kecuali {@link #getItem()} dan {@link #getTanggal()} yang menulis balik hasilnya ke field.
 * Persistence, transaksi, otorisasi, dan pemuatan relasi lazy tetap menjadi tanggung jawab
 * DAO/service dengan session aktif; jangan menaruh query duplikat pada model.</p>
 *
 * @see KembaliPengadaanItem
 * @see PeminjamanPengadaanItemDetail
 * @see DendaKeterlambatanItem
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "library", name = "kembali_pengadaan_item_detail")
public class KembaliPengadaanItemDetail extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya sengaja disamakan di seluruh entity modul
	 * {@code library} karena kelas-kelas ini dibangkitkan dari template yang sama; jangan
	 * diubah agar sesi ZK/HTTP yang sudah terserialisasi tetap dapat dibaca.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama (surrogate key) baris ini, dibangkitkan oleh database. */
	private Long id;
	/** Nama pengguna aplikasi yang terakhir mengubah baris ini (jejak audit ringan). */
	private String oleh;
	/** ID pengguna aplikasi yang terakhir mengubah baris ini (jejak audit ringan). */
	private String olehId;

	/**
	 * Mengembalikan ID pengguna aplikasi yang terakhir mengubah baris ini.
	 *
	 * @return ID pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel ID pengguna pengubah terakhir. Bersifat <b>no-op bila nilai baru kosong atau
	 * hanya berisi spasi</b> agar jejak audit lama tidak tertimpa oleh pemanggil tanpa konteks
	 * pengguna.
	 *
	 * @param olehId ID pengguna baru; diabaikan bila {@code null}/blank.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Representasi teks baris untuk grid, combobox, dan log.
	 *
	 * <p>Membaca field {@link #item} secara langsung dan merangkainya dengan {@code ""}
	 * sehingga aman terhadap {@code null}. Denda dan status pelunasan tidak ikut tampil.</p>
	 *
	 * @return representasi teks item pada baris ini.
	 */
	public String toString() {
		return item + "";
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir; no-op bila nilai baru kosong/blank.
	 *
	 * @param oleh nama pengguna baru; diabaikan bila {@code null}/blank.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna aplikasi yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}. Dipanggil Hibernate tepat sebelum {@code UPDATE},
	 * lalu mendelegasikan pengisian trio field audit kepada
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Cap waktu perubahan terakhir. Diinisialisasi ke waktu server saat objek dibuat dan
	 * diperbarui oleh {@link #onUpdate()} pada setiap {@code UPDATE}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel cap waktu perubahan terakhir secara manual.
	 *
	 * @param tanggal_dirubah cap waktu baru.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan cap waktu perubahan terakhir baris ini.
	 *
	 * @return cap waktu perubahan terakhir.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Judul/koleksi yang dikembalikan pada baris ini (denormalisasi dari eksemplarnya). */
	private Item item;
	/** Kuantitas yang dikembalikan pada baris ini. */
	private Double dikembali;
	/** Header dokumen pengembalian pemilik baris ini. */
	private KembaliPengadaanItem kembaliPengadaanItem;
	/** Baris peminjaman asal yang ditutup oleh baris pengembalian ini. */
	private PeminjamanPengadaanItemDetail peminjamanPengadaanItemDetail;
	/** Catatan bebas pada tingkat baris, umumnya kondisi fisik buku saat dikembalikan. */
	private String keterangan;
	/** Tanggal efektif pengembalian eksemplar ini; dasar perhitungan hari keterlambatan. */
	private Date tanggal = ais.ui.util.WaktuUtil.getDate();
	/** Eksemplar fisik (barcode) yang dikembalikan. */
	private ItemPunyaBarcode itemPunyaBarcode;

	/** Besaran denda yang dikenakan pada baris ini (keterlambatan + biaya penggantian). */
	private Double denda = 0.0;
	/** Catatan/riwayat denda; jalur API menambahkan baris log peristiwa ke sini. */
	private String ketDenda;
	/** Nominal denda yang sudah dibayarkan anggota; lihat peringatan pada getter-nya. */
	private Double dibayarSejumlah = 0.0;
	/** Penanda denda sudah dianggap selesai (lunas atau dibebaskan). */
	private Boolean telahDibayar;

	/**
	 * Konstruktor kosong yang dibutuhkan Hibernate/ZK untuk instansiasi via refleksi.
	 * {@code denda} dan {@code dibayarSejumlah} sudah terinisialisasi {@code 0.0}, dan
	 * {@code tanggal} terisi waktu server saat objek dibuat.
	 */
	public KembaliPengadaanItemDetail() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * @return ID baris, atau {@code null} bila objek belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama baris ini. Umumnya hanya dipanggil Hibernate setelah {@code INSERT}.
	 *
	 * @param id ID baris baru.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan keterangan baris (umumnya kondisi fisik buku saat dikembalikan).
	 *
	 * @return keterangan bebas, atau {@code null} bila tidak diisi.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan baris.
	 *
	 * @param keterangan teks keterangan baru.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menyetel judul/koleksi yang dikembalikan pada baris ini.
	 *
	 * @param item judul yang dikembalikan.
	 */
	public void setItem(Item item) {
		this.item = item;
	}

	/**
	 * Mengembalikan judul/koleksi yang dikembalikan pada baris ini.
	 *
	 * <p>Getter menjalankan {@code check(...)} milik {@link GeneralValueObject} untuk menukar
	 * proxy Hibernate yang sudah terlepas session dengan instance yang aman dibaca, lalu
	 * <b>menulis hasilnya balik ke field</b> (getter destruktif ringan).</p>
	 *
	 * @return judul yang dikembalikan, atau {@code null} bila belum ditentukan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item", nullable = true)
	public Item getItem() {
		item = check(item);
		return item;
	}

	/**
	 * Menyetel kuantitas yang dikembalikan pada baris ini.
	 *
	 * <p><b>Catatan integritas:</b> setter tidak membandingkan nilai baru dengan kuantitas pada
	 * {@link #getPeminjamanPengadaanItemDetail() baris peminjaman asal}. Mengembalikan lebih
	 * banyak daripada yang dipinjam tidak ditolak model.</p>
	 *
	 * @param dikembali kuantitas yang dikembalikan.
	 */
	public void setDikembali(Double dikembali) {
		this.dikembali = dikembali;
	}

	/**
	 * Mengembalikan kuantitas yang dikembalikan pada baris ini, dinormalkan ke {@code 0.0}
	 * bila belum diisi.
	 *
	 * <p>Normalisasi ditulis balik ke field, sehingga getter ini mengubah state objek. Berbeda
	 * dari {@link ReturPengadaanItemDetail#getJumlah()} yang membiarkan {@code null} lolos,
	 * getter ini aman langsung dipakai dalam aritmetika.</p>
	 *
	 * @return kuantitas yang dikembalikan; tidak pernah {@code null}.
	 */
	public Double getDikembali() {
		if (dikembali == null) {
			dikembali = 0.0;
		}
		return dikembali;
	}

	/**
	 * Menyetel header dokumen pengembalian pemilik baris ini.
	 *
	 * @param kembaliPengadaanItem header dokumen pengembalian.
	 */
	public void setKembaliPengadaanItem(KembaliPengadaanItem kembaliPengadaanItem) {
		this.kembaliPengadaanItem = kembaliPengadaanItem;
	}

	/**
	 * Mengembalikan header dokumen pengembalian pemilik baris ini.
	 *
	 * <p>Relasi dipetakan {@link FetchMode#SELECT} (eager per baris) sehingga aman dibaca dari
	 * renderer. Header ini juga menjadi sumber tanggal cadangan bagi {@link #getTanggal()} dan
	 * penentu apakah baris masih boleh disunting (layar mengunci input bila
	 * {@code disetujuiOleh} sudah terisi).</p>
	 *
	 * @return header dokumen pengembalian, atau {@code null} bila baris belum dikaitkan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "kembali_pengadaan_item", nullable = true)
	public KembaliPengadaanItem getKembaliPengadaanItem() {
		return kembaliPengadaanItem;
	}

	/**
	 * Mengembalikan baris peminjaman asal yang ditutup oleh baris pengembalian ini.
	 *
	 * <p>Inilah pasangan yang benar-benar dipakai menghitung keterlambatan:
	 * {@link PeminjamanPengadaanItemDetail#getJumlahSelisihHari()} membaca
	 * {@link #getTanggal()} dari sisi ini untuk menentukan berapa lama eksemplar ditahan.
	 * Relasi pada tingkat header ({@link KembaliPengadaanItem#getPeminjamanPengadaanItem()})
	 * terlalu kasar untuk pengembalian sebagian.</p>
	 *
	 * @return baris peminjaman asal, atau {@code null} bila belum dikaitkan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "peminjaman_pengadaan_item_detail", nullable = true)
	public PeminjamanPengadaanItemDetail getPeminjamanPengadaanItemDetail() {
		return peminjamanPengadaanItemDetail;
	}

	/**
	 * Menyetel baris peminjaman asal yang ditutup oleh baris pengembalian ini.
	 *
	 * <p>Pemanggil bertanggung jawab mengisi pula sisi sebaliknya
	 * ({@code peminjamanPengadaanItemDetail.setKembaliPengadaanItemDetail(this)}) karena kedua
	 * penunjuk adalah kolom {@code ManyToOne} mandiri yang tidak disinkronkan Hibernate. Bila
	 * sisi peminjaman tidak diisi, eksemplar akan tetap terhitung sebagai belum kembali dan
	 * dendanya terus bertambah setiap hari.</p>
	 *
	 * @param peminjamanPengadaanItemDetail baris peminjaman asal.
	 */
	public void setPeminjamanPengadaanItemDetail(PeminjamanPengadaanItemDetail peminjamanPengadaanItemDetail) {
		this.peminjamanPengadaanItemDetail = peminjamanPengadaanItemDetail;
	}

	/**
	 * Mengembalikan besaran denda yang dikenakan pada baris ini, dinormalkan ke {@code 0.0}
	 * bila belum diisi.
	 *
	 * <p>Nilai ini dihitung dan dituliskan oleh lapisan action, bukan oleh model. Rumusnya:
	 * tarif dari {@link DendaKeterlambatanItem} yang cocok dengan profil anggota (jenis
	 * anggota, tipe anggota, fakultas, jurusan, perpustakaan, ambang jumlah hari terlambat)
	 * dikalikan kuantitas baris peminjaman, ditambah biaya penggantian bila buku dinyatakan
	 * hilang atau rusak. Karena hasil perhitungan dibekukan ke kolom ini, mengubah tarif
	 * {@link DendaKeterlambatanItem} di kemudian hari <b>tidak</b> mengubah denda yang sudah
	 * tercatat &mdash; perilaku yang memang diinginkan agar riwayat tetap stabil.</p>
	 *
	 * <p>Normalisasi {@code null} ditulis balik ke field, sehingga getter ini mengubah state
	 * objek.</p>
	 *
	 * @return besaran denda yang dikenakan; tidak pernah {@code null}.
	 */
	public Double getDenda() {
		if (denda == null) {
			denda = 0.0;
		}
		return denda;
	}

	/**
	 * Menyetel besaran denda yang dikenakan pada baris ini.
	 *
	 * <p><b>Catatan integritas:</b> setter tidak memverifikasi nilai terhadap tarif
	 * {@link DendaKeterlambatanItem} mana pun dan tidak menolak nilai negatif. Ia juga tidak
	 * menyesuaikan {@link #getDibayarSejumlah() dibayarSejumlah} yang sudah tercatat, sehingga
	 * menurunkan denda setelah pembayaran dapat menghasilkan kondisi "dibayar melebihi
	 * denda".</p>
	 *
	 * @param denda besaran denda baru.
	 */
	public void setDenda(Double denda) {
		this.denda = denda;
	}

	/**
	 * Mengembalikan tanggal efektif pengembalian eksemplar ini, dengan dua tingkat cadangan.
	 *
	 * <p>Urutan pengambilan nilai: (1) tanggal yang tersimpan pada baris; bila kosong,
	 * (2) {@link KembaliPengadaanItem#getTanggalPersetujuan() tanggal persetujuan} header;
	 * bila masih kosong juga, (3) waktu server saat ini. Nilai hasilnya <b>ditulis balik ke
	 * field</b>, sehingga getter ini mengubah state objek dan dapat menandai entity kotor.</p>
	 *
	 * <p><b>Bobotnya besar.</b> Tanggal inilah yang dibaca
	 * {@link PeminjamanPengadaanItemDetail#getJumlahSelisihHari()} untuk menghitung berapa hari
	 * eksemplar ditahan, dan dari situ berapa hari keterlambatan serta berapa denda yang
	 * dikenakan. Cadangan tingkat ketiga (waktu server) berarti baris pengembalian pada dokumen
	 * yang belum disetujui akan dinilai seolah-olah dikembalikan hari ini &mdash; nilai yang
	 * terus bergeser setiap hari selama dokumen dibiarkan berstatus draf.</p>
	 *
	 * @return tanggal efektif pengembalian; tidak pernah {@code null} setelah pemanggilan
	 *         pertama.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal() {
		if (tanggal == null) {
			if (kembaliPengadaanItem != null && kembaliPengadaanItem.getTanggalPersetujuan() != null) {
				tanggal = kembaliPengadaanItem.getTanggalPersetujuan();
			}
		}

		if (tanggal == null) {
			tanggal = ais.ui.util.WaktuUtil.getDate();
		}

		return tanggal;
	}

	/**
	 * Menyetel tanggal efektif pengembalian eksemplar ini.
	 *
	 * <p><b>Berdampak langsung pada denda:</b> memundurkan tanggal ini mengurangi hari
	 * keterlambatan dan karenanya mengurangi denda. Model tidak membatasi rentangnya sama
	 * sekali &mdash; tanggal sebelum tanggal peminjaman maupun di masa depan sama-sama
	 * diterima.</p>
	 *
	 * @param tanggal tanggal efektif pengembalian.
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * Mengembalikan eksemplar fisik (barcode) yang dikembalikan pada baris ini.
	 *
	 * <p>Relasi dipetakan {@link FetchMode#SELECT} sehingga aman dibaca dari renderer ZK.
	 * Inilah data yang dipakai petugas saat memindai buku di meja sirkulasi.</p>
	 *
	 * @return eksemplar yang dikembalikan, atau {@code null} bila belum ditentukan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "item_punya_barcode", nullable = true)
	public ItemPunyaBarcode getItemPunyaBarcode() {
		return itemPunyaBarcode;
	}

	/**
	 * Menyetel eksemplar fisik (barcode) yang dikembalikan pada baris ini.
	 *
	 * <p><b>Catatan integritas:</b> setter tidak memeriksa bahwa eksemplar ini sama dengan yang
	 * tercatat pada {@link #getPeminjamanPengadaanItemDetail() baris peminjaman asal}.
	 * Mengembalikan eksemplar yang berbeda dari yang dipinjam akan lolos tersimpan.</p>
	 *
	 * @param itemPunyaBarcode eksemplar yang dikembalikan.
	 */
	public void setItemPunyaBarcode(ItemPunyaBarcode itemPunyaBarcode) {
		this.itemPunyaBarcode = itemPunyaBarcode;
	}

	/**
	 * Mengembalikan penanda bahwa denda pada baris ini sudah dianggap selesai.
	 *
	 * <p>Bernilai benar untuk dua situasi yang berbeda maknanya namun tidak dibedakan oleh
	 * kolom ini: denda benar-benar lunas dibayar anggota, atau denda dibebaskan petugas
	 * (<i>waiver</i>). Yang membedakan keduanya hanyalah teks pada {@link #getKetDenda()}.
	 * Getter menormalkan {@code null} menjadi {@code false} tanpa menulis balik ke field.</p>
	 *
	 * <p><b>Penanda ini adalah gerbang bagi {@link #getDibayarSejumlah()}</b> &mdash; selama ia
	 * salah, nominal pembayaran yang tersimpan tidak akan pernah terbaca. Lihat peringatan
	 * pada getter tersebut.</p>
	 *
	 * @return {@code true} bila denda sudah dianggap selesai; tidak pernah {@code null}.
	 */
	public Boolean getTelahDibayar() {
		return telahDibayar == null ? false : telahDibayar;
	}

	/**
	 * Menyetel penanda bahwa denda pada baris ini sudah dianggap selesai.
	 *
	 * <p>Layar ZK ({@code helper/KembaliPengadaanItemPunyaItemHelper}) memasang pendengar
	 * {@code onChange} pada kotak nominal yang <b>mencentang penanda ini secara otomatis untuk
	 * nominal berapa pun di atas {@code 0.1}</b>. Artinya, lewat layar, pembayaran sebagian
	 * langsung tercatat sebagai denda selesai &mdash; sisa tagihan lenyap. Sebaliknya jalur API
	 * ({@code LibraryOperationsApi.fineAction}) menyetel penanda ini hanya bila nominal
	 * terbayar sudah mencapai denda penuh, sehingga pembayaran sebagian meninggalkan penanda
	 * bernilai salah. Kedua jalur bersama-sama membuat pembayaran sebagian tidak pernah
	 * tersimpan dengan benar; lihat {@link #getDibayarSejumlah()}.</p>
	 *
	 * @param telahDibayar penanda penyelesaian denda.
	 */
	public void setTelahDibayar(Boolean telahDibayar) {
		this.telahDibayar = telahDibayar;
	}

	/**
	 * Mengembalikan nominal denda yang sudah dibayarkan anggota &mdash; <b>bukan nilai mentah
	 * yang tersimpan</b>.
	 *
	 * <p>Getter ini menerapkan dua aturan turunan sebelum mengembalikan nilai:</p>
	 * <ol>
	 *   <li>bila {@link #getTelahDibayar()} bernilai salah, hasilnya selalu {@code 0.0},
	 *       berapa pun isi field {@code dibayarSejumlah};</li>
	 *   <li>bila {@link #getTelahDibayar()} benar tetapi {@code dibayarSejumlah} kosong atau
	 *       lebih kecil dari {@code 0.01}, hasilnya adalah seluruh {@link #getDenda() denda}
	 *       &mdash; asumsinya penanda "selesai" berarti dibayar penuh.</li>
	 * </ol>
	 *
	 * <p><b>Mengapa ini berbahaya.</b> Entity dipetakan Hibernate dengan <i>property access</i>
	 * (anotasi {@code @Id} dan {@code @Column} terpasang pada getter, bukan field), sehingga
	 * nilai yang <em>dikembalikan getter inilah</em> yang dibaca Hibernate saat menyusun
	 * pernyataan {@code INSERT}/{@code UPDATE}. Getter yang menurunkan nilai secara kondisional
	 * karenanya bukan sekadar kenyamanan tampilan: ia ikut menentukan isi basis data. Untuk
	 * baris dengan pembayaran sebagian &mdash; anggota membayar sebagian denda sehingga
	 * {@code telahDibayar} masih salah &mdash; aturan pertama membuat nominal yang sudah
	 * disetel lewat {@link #setDibayarSejumlah(Double)} tertulis sebagai {@code 0.0} pada
	 * <i>flush</i> berikutnya. Pembayaran itu hilang tanpa jejak di kolomnya sendiri; satu-satunya
	 * sisa adalah teks log pada {@link #getKetDenda()} bila pembayaran dilakukan lewat jalur
	 * API.</p>
	 *
	 * <p>Dampak yang dapat diamati:</p>
	 * <ul>
	 *   <li>jalur API {@code LibraryOperationsApi.fineAction} menghitung sisa tagihan sebagai
	 *       {@code denda - dibayarSejumlah}. Karena angka terbayar selalu terbaca {@code 0.0}
	 *       untuk tagihan yang belum lunas, sisa tagihan tidak pernah berkurang dan anggota
	 *       dapat diminta membayar cicilan yang sama berulang kali;</li>
	 *   <li>laporan penerimaan denda ({@code LibraryUtil} menyaring
	 *       {@code dibayarSejumlah &gt; 0.1}) melewatkan seluruh pembayaran sebagian;</li>
	 *   <li>penjumlahan pada dasbor perpustakaan
	 *       ({@code select sum(k.dibayarSejumlah) from KembaliPengadaanItemDetail k})
	 *       melaporkan penerimaan denda lebih rendah dari kenyataan.</li>
	 * </ul>
	 *
	 * <p>Aturan kedua punya efek berlawanan namun sama-sama diam: begitu petugas mencentang
	 * "telah dibayar" tanpa mengisi nominal, sistem menganggap denda penuh sudah diterima dan
	 * mencatatnya sebagai penerimaan &mdash; termasuk untuk kasus pembebasan denda yang
	 * seharusnya bernilai nol.</p>
	 *
	 * <p>Pemanggil yang memerlukan nilai mentah yang benar-benar tersimpan tidak dapat
	 * memperolehnya lewat kelas ini; bacalah kolomnya langsung lewat query proyeksi.</p>
	 *
	 * @return nominal yang dianggap sudah dibayar menurut aturan di atas; tidak pernah
	 *         {@code null}.
	 */
	public Double getDibayarSejumlah() {
		return getTelahDibayar() ? (dibayarSejumlah == null || dibayarSejumlah < 0.01 ? getDenda() : dibayarSejumlah)
				: 0.0;
	}

	/**
	 * Menyetel nominal denda yang sudah dibayarkan anggota.
	 *
	 * <p><b>Perhatian:</b> nilai yang disetel di sini belum tentu bertahan. Bila
	 * {@link #getTelahDibayar()} bernilai salah, {@link #getDibayarSejumlah()} akan
	 * mengembalikan {@code 0.0} dan itulah yang ditulis Hibernate ke basis data. Untuk mencatat
	 * pembayaran secara andal, setel {@code telahDibayar} bernilai benar pada transaksi yang
	 * sama &mdash; dengan konsekuensi tagihan dianggap selesai meski baru terbayar
	 * sebagian.</p>
	 *
	 * @param dibayarSejumlah nominal yang dibayarkan.
	 */
	public void setDibayarSejumlah(Double dibayarSejumlah) {
		this.dibayarSejumlah = dibayarSejumlah;
	}

	/**
	 * Mengembalikan catatan/riwayat denda baris ini, dinormalkan ke string kosong bila belum
	 * diisi.
	 *
	 * <p>Kolom dipetakan sebagai {@code text} sehingga dapat menampung riwayat panjang. Jalur
	 * API menambahkan satu baris log per peristiwa berisi jenis tindakan (pembayaran,
	 * pembebasan, pembatalan), nominal, tanggal, pelaku, dan alasan &mdash; dengan pemotongan
	 * pada sekitar 3.900 karakter. Karena {@link #getDibayarSejumlah()} tidak dapat dipercaya
	 * untuk pembayaran sebagian, teks pada kolom inilah satu-satunya jejak yang tersisa untuk
	 * merekonstruksi riwayat pembayaran tersebut.</p>
	 *
	 * <p>Normalisasi tidak ditulis balik ke field, sehingga getter ini tidak mengubah state.</p>
	 *
	 * @return catatan denda; tidak pernah {@code null}.
	 */
	@Column(columnDefinition = "text")
	public String getKetDenda() {
		return ketDenda == null ? "" : ketDenda;
	}

	/**
	 * Menyetel catatan/riwayat denda baris ini.
	 *
	 * @param ketDenda teks catatan denda baru.
	 */
	public void setKetDenda(String ketDenda) {
		this.ketDenda = ketDenda;
	}

}
