package ais.database.model.sirs;

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

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;
import ais.database.model.asset.Lokasi;

/**
 * Entitas <b>Produksi</b> item medis internal pada schema {@code sirs} (tabel
 * {@code produksi}). Merepresentasikan satu kegiatan pembuatan/peracikan item
 * medis di dalam rumah sakit sendiri — mis. meracik obat komposit dari
 * bahan-bahan bakunya — sehingga stok bahan baku BERKURANG dan stok item hasil
 * BERTAMBAH dalam satu dokumen.
 *
 * <h2>Bentuk dokumen yang tidak lazim: hasil di header, bahan di detail</h2>
 * <p>
 * Berbeda dari dokumen header-detail lain di klaster ini yang headernya hanya
 * berisi metadata, entitas ini menyimpan HASIL produksinya sendiri di tingkat
 * header: {@link #getItem()} adalah item yang dihasilkan dan
 * {@link #getQty()} adalah jumlah yang dihasilkan. Baris-baris
 * {@link ProduksiDetail} berisi BAHAN BAKU yang dikonsumsi.
 * </p>
 * <p>
 * Konsekuensi langsung dari bentuk ini: satu dokumen produksi hanya bisa
 * menghasilkan SATU jenis item. Produksi yang menghasilkan beberapa keluaran
 * sekaligus tidak dapat diwakili oleh struktur ini dan harus dipecah menjadi
 * beberapa dokumen. Konsekuensi lain, arah mutasi stok bergantung pada TEMPAT
 * data berada, bukan pada tanda angkanya: yang di header menambah, yang di
 * detail mengurangi. Kode yang memproses dokumen ini harus memakai kedua sisi
 * itu dengan arah yang benar, karena tertukarnya arah tidak akan terdeteksi
 * oleh apa pun di level model.
 * </p>
 *
 * <h2>Hubungan dengan resep bahan baku</h2>
 * <p>
 * Komposisi teoretis suatu item komposit dicatat terpisah di
 * {@link BahanBakuItem} (item induk, bahan baku, takaran per satu unit induk).
 * Entitas ini TIDAK menautkan diri ke sana: baris-baris
 * {@link ProduksiDetail} diisi bebas dan tidak diverifikasi terhadap resep
 * mana pun. Skema karena itu tidak menjamin bahwa bahan yang dikonsumsi sesuai
 * jenis maupun takarannya dengan komposisi item yang dihasilkan — untuk bahan
 * medis, ketidaksesuaian semacam itu bermakna risiko klinis, bukan sekadar
 * selisih persediaan.
 * </p>
 *
 * <h2>Catatan integritas</h2>
 * <p>
 * Tidak ada penjaga keseimbangan stok di level skema: tidak ada pemeriksaan
 * bahwa stok bahan baku di {@link #getLokasi()} mencukupi sebelum produksi
 * disetujui, dan tidak ada pemeriksaan kewajaran antara jumlah hasil dengan
 * jumlah bahan yang dikonsumsi. Nilai biaya
 * ({@link #getBiaya()}, {@link #getBiayaSatuan()},
 * {@link #getBiayaTambahan()}) adalah angka TERSIMPAN yang tidak diturunkan
 * ulang dari harga bahan bakunya saat dibaca.
 * </p>
 * <p>
 * Seperti {@link PermintaanPembelian} dan berbeda dari
 * {@link PesananPembelian}/{@link PenerimaanOrder}, entitas ini TIDAK memiliki
 * jejak pembatalan ({@code tanggalPembatalan}/{@code dibatalkanOleh}). Dokumen
 * produksi yang keliru karena itu tidak punya jalur pembatalan yang
 * meninggalkan jejak — padahal ia sudah menggerakkan stok di dua arah
 * sekaligus. Entitas ini juga tidak punya relasi ke {@code PostingHistory}.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "produksi")
public class Produksi extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java untuk entitas ini, dipatok agar object
	 * yang sudah terserialisasi tetap kompatibel meski struktur kelas berubah.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private Long index;
	private String olehId;

	/**
	 * Mengambil ID pengguna yang terakhir mengubah dokumen produksi ini. Field
	 * audit shadow yang diisi lewat {@link #setOlehId(String)}.
	 *
	 * @return ID pengguna terakhir yang mengubah, atau {@code null} jika belum
	 *         pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan ID pengguna yang mengubah dokumen produksi ini. Nilai
	 * kosong/blank SENGAJA diabaikan (early return) agar field audit ini tidak
	 * pernah ditimpa jadi kosong — pola field audit shadow yang merupakan
	 * KEHARUSAN TEKNIS berulang di paket model AIS, bukan bug.
	 *
	 * @param olehId ID pengguna; nilai {@code null} atau string
	 *               kosong/whitespace-saja tidak akan mengubah nilai tersimpan.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	private String oleh;

	/**
	 * Representasi ringkas dokumen produksi ini untuk tampilan
	 * combobox/listbox ZK dan log, memakai {@link #getKode()} sebagai label.
	 * Akan mengembalikan {@code null} bila kode belum di-generate.
	 *
	 * @return kode dokumen produksi ini.
	 */
	public String toString() {
		return kode;
	}

	/**
	 * Menetapkan nama pengguna yang mengubah dokumen produksi ini. Nilai
	 * kosong/blank diabaikan agar field audit ini tidak pernah ditimpa kosong.
	 *
	 * @param oleh nama pengguna; nilai {@code null} atau kosong/whitespace-saja
	 *             tidak akan mengubah nilai tersimpan.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengambil nama pengguna yang terakhir mengubah dokumen produksi ini.
	 *
	 * @return nama pengguna terakhir yang mengubah, atau {@code null} jika
	 *         belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate} yang memperbarui {@link #tanggal_dirubah}
	 * otomatis sebelum baris ini di-UPDATE, lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = new Date();

	/**
	 * Menetapkan timestamp perubahan terakhir dokumen ini secara manual.
	 *
	 * @param tanggal_dirubah timestamp perubahan baru.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengambil timestamp perubahan terakhir dokumen ini, diperbarui otomatis
	 * oleh {@link #onUpdate()} setiap kali baris di-UPDATE.
	 *
	 * @return timestamp perubahan terakhir.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	private String kode;
	private ItemMedis item;
	private Double qty = 0.0;
	private Double biayaSatuan = 0.0;
	private Double biaya = 0.0;
	private Double biayaTambahan = 0.0;
	private String keterangan;
	private Date tanggalPembuatan;
	private Date tanggalPersetujuan;
	private Lokasi lokasi;
	private Tbmuser dibuatOleh;
	private Tbmuser disetujuiOleh;

	/**
	 * Konstruktor default (wajib untuk entitas JPA/Hibernate).
	 */
	public Produksi() {
	}

	/**
	 * Primary key dokumen produksi ini, auto-increment (IDENTITY) dan diisi
	 * database.
	 *
	 * @return ID unik dokumen produksi ini, atau {@code null} untuk baris yang
	 *         belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan ID dokumen produksi ini.
	 *
	 * @param id ID dokumen produksi.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil kode/nomor dokumen produksi ini. Kolom {@code NOT NULL} dan
	 * {@code UNIQUE} di database sehingga berperan sebagai identitas bisnis
	 * dokumen, di samping {@link #getId()} yang merupakan identitas teknis.
	 *
	 * @return kode dokumen produksi, atau {@code null} bila belum di-generate.
	 */
	@Column(name = "kode", nullable = false, unique = true)
	public String getKode() {
		return this.kode;
	}

	/**
	 * Menetapkan kode/nomor dokumen produksi ini. Tidak ada validasi format
	 * maupun pengecekan keunikan di level entitas — keunikan ditegakkan oleh
	 * constraint {@code UNIQUE} database.
	 *
	 * @param kode kode dokumen produksi.
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengambil keterangan bebas dokumen produksi ini.
	 *
	 * @return teks keterangan, atau {@code null} jika belum diisi.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas dokumen produksi ini.
	 *
	 * @param keterangan teks keterangan.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menetapkan pengguna pembuat dokumen produksi ini.
	 *
	 * @param dibuatOleh pengguna pembuat dokumen.
	 */
	public void setDibuatOleh(Tbmuser dibuatOleh) {
		this.dibuatOleh = dibuatOleh;
	}

	/**
	 * Mengambil pengguna pembuat dokumen produksi ini. Relasi WAJIB
	 * ({@code nullable = false}) ke {@link Tbmuser} — setiap dokumen produksi
	 * harus punya pembuat yang teridentifikasi. Untuk peracikan bahan medis,
	 * jejak siapa yang meracik adalah informasi yang bernilai klinis, bukan
	 * sekadar administratif.
	 *
	 * <p>
	 * Getter ini memanggil {@code check(...)} milik
	 * {@link ais.database.model.GeneralValueObject} untuk meresolusi proxy lazy
	 * dan menugaskan hasilnya kembali ke field — sehingga bukan getter murni:
	 * ia bisa mengubah state object dan membuka koneksi database sendiri saat
	 * sesi Hibernate asalnya sudah tertutup.
	 * </p>
	 *
	 * @return pengguna pembuat dokumen.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dibuat_oleh", nullable = false)
	public Tbmuser getDibuatOleh() {
		dibuatOleh = check(dibuatOleh);
		return dibuatOleh;
	}

	/**
	 * Menetapkan pengguna yang menyetujui dokumen produksi ini.
	 *
	 * @param disetujuiOleh pengguna penyetuju dokumen.
	 */
	public void setDisetujuiOleh(Tbmuser disetujuiOleh) {
		this.disetujuiOleh = disetujuiOleh;
	}

	/**
	 * Mengambil pengguna yang menyetujui dokumen produksi ini — relasi
	 * OPSIONAL, kosong selama produksi belum disetujui. Persetujuanlah yang
	 * memicu mutasi stok dua arah (bahan berkurang, hasil bertambah). Entitas
	 * TIDAK memaksakan penyetuju berbeda dari {@link #getDibuatOleh()}.
	 * Getter ini memanggil {@code check(...)} sehingga bukan getter murni
	 * (lihat {@link #getDibuatOleh()}).
	 *
	 * @return pengguna penyetuju, atau {@code null} bila belum disetujui.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disetujui_oleh", nullable = true)
	public Tbmuser getDisetujuiOleh() {
		disetujuiOleh = check(disetujuiOleh);
		return disetujuiOleh;
	}

	/**
	 * Menetapkan tanggal pembuatan dokumen produksi ini.
	 *
	 * @param tanggalPembuatan timestamp pembuatan dokumen.
	 */
	public void setTanggalPembuatan(Date tanggalPembuatan) {
		this.tanggalPembuatan = tanggalPembuatan;
	}

	/**
	 * Mengambil tanggal pembuatan dokumen produksi ini. Diisi sekali oleh
	 * lapisan action dan tidak berubah lagi.
	 *
	 * @return timestamp pembuatan dokumen, atau {@code null} bila belum diisi.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_pembuatan")
	public Date getTanggalPembuatan() {
		return tanggalPembuatan;
	}

	/**
	 * Menetapkan tanggal persetujuan dokumen produksi ini.
	 *
	 * @param tanggalPersetujuan timestamp persetujuan dokumen.
	 */
	public void setTanggalPersetujuan(Date tanggalPersetujuan) {
		this.tanggalPersetujuan = tanggalPersetujuan;
	}

	/**
	 * Mengambil tanggal persetujuan dokumen produksi ini — satu-satunya
	 * penanda status pada entitas ini, sekaligus penanda bahwa mutasi stok dua
	 * arah sudah dituliskan. Nilai {@code null} berarti produksi masih draft.
	 *
	 * <p>
	 * Karena entitas ini tidak punya kolom pembatalan (berbeda dari
	 * {@link PesananPembelian} dan {@link PenerimaanOrder}), tidak ada nilai
	 * yang bisa menyatakan bahwa dokumen yang sudah disetujui kemudian
	 * dibatalkan. Membatalkan produksi berarti mengosongkan kembali kolom ini,
	 * yang menghapus jejak bahwa dokumen pernah disetujui dan pernah
	 * menggerakkan stok. Rekonsiliasi persediaan karena itu tidak dapat
	 * bersandar pada dokumen produksi untuk menjelaskan mutasi yang sudah
	 * terlanjur tertulis.
	 * </p>
	 *
	 * @return timestamp persetujuan, atau {@code null} bila belum disetujui.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_persetujuan")
	public Date getTanggalPersetujuan() {
		return tanggalPersetujuan;
	}

	/**
	 * Menetapkan lokasi/gudang tempat produksi berlangsung.
	 *
	 * @param lokasi lokasi gudang produksi.
	 */
	public void setLokasi(Lokasi lokasi) {
		this.lokasi = lokasi;
	}

	/**
	 * Mengambil lokasi/gudang tempat produksi berlangsung — relasi ke
	 * {@link Lokasi} pada paket {@code asset}. Inilah lokasi yang stok bahan
	 * bakunya BERKURANG sekaligus stok item hasilnya BERTAMBAH; kedua arah
	 * mutasi terjadi di gudang yang sama, karena entitas ini tidak punya kolom
	 * lokasi terpisah untuk hasil produksi.
	 *
	 * <p>
	 * Relasi OPSIONAL, sehingga dokumen produksi tanpa lokasi tetap sah secara
	 * skema — kondisi berbahaya karena tidak jelas gudang mana yang stoknya
	 * bergerak, padahal dokumen ini menggerakkan dua arah sekaligus. Ini juga
	 * satu-satunya sumbu pembatas lingkup data yang tersedia (modul
	 * {@code sirs} tidak punya sumbu tenant/satuan kerja). Getter ini memanggil
	 * {@code check(...)} sehingga bukan getter murni (lihat
	 * {@link #getDibuatOleh()}).
	 * </p>
	 *
	 * @return lokasi gudang produksi, atau {@code null} bila belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "lokasi", nullable = true)
	public Lokasi getLokasi() {
		lokasi = check(lokasi);
		return lokasi;
	}

	/**
	 * Menetapkan nomor urut tampilan baris ini.
	 *
	 * @param index nomor urut tampilan.
	 */
	public void setIndex(Long index) {
		this.index = index;
	}

	/**
	 * Mengambil nomor urut tampilan baris ini. Dipakai grid/listbox ZK untuk
	 * penomoran baris; bukan bagian dari identitas dokumen.
	 *
	 * @return nomor urut tampilan, atau {@code null} bila belum diisi.
	 */
	public Long getIndex() {
		return index;
	}

	/**
	 * Menetapkan item medis yang DIHASILKAN oleh produksi ini.
	 *
	 * @param item item medis hasil produksi.
	 */
	public void setItem(ItemMedis item) {
		this.item = item;
	}

	/**
	 * Mengambil item medis yang DIHASILKAN oleh dokumen produksi ini — inilah
	 * keluaran produksi, bukan bahan bakunya. Bahan baku yang dikonsumsi ada di
	 * baris-baris {@link ProduksiDetail}.
	 *
	 * <p>
	 * Penempatan hasil produksi di tingkat header inilah yang membatasi satu
	 * dokumen hanya boleh punya satu jenis keluaran. Relasinya OPSIONAL
	 * ({@code nullable = true}), sehingga dokumen produksi tanpa item hasil
	 * tetap tersimpan — dokumen semacam itu akan mengonsumsi bahan baku tanpa
	 * menghasilkan apa pun, yaitu penyusutan persediaan murni yang berkedok
	 * produksi.
	 * </p>
	 * <p>
	 * Skema tidak memeriksa apakah item ini benar-benar merupakan item komposit
	 * yang punya resep di {@link BahanBakuItem}, dan tidak memeriksa apakah
	 * bahan-bahan pada {@link ProduksiDetail} sesuai dengan resep tersebut.
	 * Getter ini memanggil {@code check(...)} sehingga bukan getter murni
	 * (lihat {@link #getDibuatOleh()}).
	 * </p>
	 *
	 * @return item medis hasil produksi, atau {@code null} bila belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item", nullable = true)
	public ItemMedis getItem() {
		item = check(item);
		return item;
	}

	/**
	 * Menetapkan jumlah item hasil yang diproduksi. Tidak ada penolakan nilai
	 * negatif maupun nol di level entitas.
	 *
	 * @param qty jumlah hasil produksi.
	 */
	public void setQty(Double qty) {
		this.qty = qty;
	}

	/**
	 * Mengambil jumlah item hasil yang diproduksi — angka yang MENAMBAH stok
	 * {@link #getItem()} di gudang {@link #getLokasi()} saat dokumen
	 * disetujui.
	 *
	 * <p>
	 * Skema tidak memasang batas apa pun pada angka ini, dan yang khas untuk
	 * dokumen produksi: tidak ada pemeriksaan kewajaran antara jumlah hasil di
	 * sini dengan jumlah bahan baku yang dikonsumsi di
	 * {@link ProduksiDetail#getJumlah()}. Satu dokumen bisa mengklaim
	 * menghasilkan seribu unit dari satu unit bahan baku tanpa satu pun
	 * mekanisme yang mempertanyakannya. Bila resep di {@link BahanBakuItem}
	 * tersedia, kewajaran itu sebenarnya bisa dihitung — tetapi entitas ini
	 * tidak menautkan diri ke sana.
	 * </p>
	 * <p>
	 * Nilai NEGATIF perlu diwaspadai khusus: ia akan menjadi penambahan atas
	 * angka negatif, yaitu pengurangan stok item hasil lewat dokumen yang
	 * bentuknya produksi. Nilainya di-default {@code 0.0} sehingga jarang
	 * {@code null}, tetapi setter-nya menerima {@code null} tanpa keberatan.
	 * </p>
	 *
	 * @return jumlah hasil produksi, default {@code 0.0}.
	 */
	public Double getQty() {
		return qty;
	}

	/**
	 * Menetapkan total biaya produksi.
	 *
	 * @param biaya total biaya produksi.
	 */
	public void setBiaya(Double biaya) {
		this.biaya = biaya;
	}

	/**
	 * Mengambil total biaya produksi — nilai TERSIMPAN yang dituliskan lapisan
	 * action, bukan hasil penjumlahan harga bahan baku pada
	 * {@link ProduksiDetail} saat dibaca. Bila baris bahan berubah setelah
	 * angka ini terisi, keduanya akan menyimpang diam-diam. Angka inilah yang
	 * lazimnya menjadi dasar nilai persediaan item hasil yang masuk.
	 *
	 * @return total biaya produksi, default {@code 0.0}.
	 */
	public Double getBiaya() {
		return biaya;
	}

	/**
	 * Menetapkan biaya tambahan produksi.
	 *
	 * @param biayaTambahan biaya tambahan di luar bahan baku.
	 */
	public void setBiayaTambahan(Double biayaTambahan) {
		this.biayaTambahan = biayaTambahan;
	}

	/**
	 * Mengambil biaya tambahan produksi — komponen biaya di luar nilai bahan
	 * baku (mis. upah peracikan, kemasan). Skema tidak mendefinisikan apakah
	 * angka ini sudah termasuk di dalam {@link #getBiaya()} atau merupakan
	 * tambahan di atasnya, sehingga risiko hitung ganda nyata dan hanya bisa
	 * dicegah oleh kesepakatan di lapisan action.
	 *
	 * @return biaya tambahan, default {@code 0.0}.
	 */
	@Column(name = "biaya_tambahan")
	public Double getBiayaTambahan() {
		return biayaTambahan;
	}

	/**
	 * Menetapkan biaya per satuan hasil produksi.
	 *
	 * @param biayaSatuan biaya per satuan hasil.
	 */
	public void setBiayaSatuan(Double biayaSatuan) {
		this.biayaSatuan = biayaSatuan;
	}

	/**
	 * Mengambil biaya per satuan hasil produksi — angka yang secara logis
	 * merupakan {@link #getBiaya()} dibagi {@link #getQty()}, tetapi TERSIMPAN
	 * terpisah dan tidak diturunkan ulang saat dibaca. Skema tidak menjamin
	 * ketiganya konsisten: mengubah salah satu tidak menyesuaikan yang lain,
	 * sehingga nilai persediaan yang dihitung dari {@code biayaSatuan} bisa
	 * berbeda dari yang dihitung dari {@code biaya} dibagi {@code qty}. Kode
	 * yang menilai persediaan hasil produksi sebaiknya memilih satu sumber
	 * secara konsisten dan tidak berpindah-pindah antar keduanya.
	 *
	 * @return biaya per satuan hasil, default {@code 0.0}.
	 */
	@Column(name = "biaya_satuan")
	public Double getBiayaSatuan() {
		return biayaSatuan;
	}

}
