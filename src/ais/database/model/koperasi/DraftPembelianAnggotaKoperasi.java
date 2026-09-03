package ais.database.model.koperasi;

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

import org.hibernate.Session;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.PostingHistory;
import ais.database.model.asset.Lokasi;
import ais.database.model.inventory.DraftPembelian;
import ais.database.model.inventory.Produk;
import ais.database.model.inventory.Toko;

/**
 * Dokumen draft pesanan koperasi/kantin -- jalur "pesan dulu, bayar belakangan" (mis. pesanan
 * kantin/meja yang ditahan sebelum dibayar) yang, begitu lunas, <b>difinalisasi</b> menjadi header
 * {@link PembelianAnggotaKoperasi} definitif. Lihat {@link #simpanRinci} untuk cara rincian barang
 * (dan produk ekstra) dicatat ke draft ini, dan {@link #getLunas()} untuk gerbang yang mencegah
 * draft yang sama difinalisasi/dibayar dua kali.
 *
 * <h3>Relasi dengan {@code PembelianAnggotaKoperasi}</h3>
 * <p>
 * Kelas ini adalah <b>sumber kebenaran sementara</b> sebelum pembayaran definitif terjadi. Header
 * {@link PembelianAnggotaKoperasi} punya properti {@code draftPembelianAnggotaKoperasi} yang, bila
 * terisi, membuat TUJUH getter konteksnya ({@code getAnggotaKoperasi}, {@code getLokasi},
 * {@code getToko}, {@code getTbmuser}, {@code getMejaKantin}, {@code getTanggalPembayaran},
 * {@code getKodePembayaranOnline}) berhenti membaca kolomnya sendiri dan menyalin ulang nilai dari
 * draft ini pada tiap pemanggilan -- lihat JavaDoc {@link PembelianAnggotaKoperasi#getDraftPembelianAnggotaKoperasi()}
 * untuk rincian lengkap pola getter destruktif tersebut. Konsekuensinya, mengubah field konteks di
 * draft ini SETELAH header terkait dibuat akan ikut mengubah apa yang dibaca dari header itu pada
 * penyimpanan berikutnya.
 * </p>
 *
 * <h3>Gerbang anti-bayar-dobel</h3>
 * <p>
 * {@link #getLunas()} menunjuk header {@link PembelianAnggotaKoperasi} yang melunasi draft ini;
 * {@code null} berarti belum dibayar. Pemanggil finalisasi (mis.
 * {@code KantinHelper#sinkronkanRincianDraftUntukFinalisasi}) memeriksa
 * {@code draft.getLunas() != null} di awal dan langsung keluar bila sudah terisi -- draft yang
 * sudah lunas TIDAK diproses ulang. Pemeriksaan ini murni tanggung jawab pemanggil; entity ini
 * sendiri tidak menolak {@link #setLunas(PembelianAnggotaKoperasi)} dipanggil dua kali.
 * </p>
 *
 * <p>{@code @Audited} (Hibernate Envers) -- setiap perubahan baris draft (termasuk saat
 * {@code lunas} terpasang) tersimpan sebagai revisi.</p>
 *
 * @see PembelianAnggotaKoperasi dokumen definitif hasil finalisasi draft ini
 * @see DraftPembelian baris rincian barang milik draft ini
 * @see #simpanRinci(Session, JSONArray, String, Date, Toko, KodePembayaranOnline)
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "draft_pembelian_anggota_koperasi")
public class DraftPembelianAnggotaKoperasi extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java. Nilainya sengaja disamakan dengan {@link PembelianAnggotaKoperasi}
	 * dan beberapa entity koperasi lain hasil pembangkitan Hibernate Tools; jangan diubah.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama (identity/auto-increment). Lihat {@link #getId()}. */
	private Long id;
	/** Nomor urut tampilan sementara, tidak dipetakan ke kolom (tanpa {@code @Column}) -- pola sama dgn {@link PembelianAnggotaKoperasi#getIndex()}. Lihat {@link #getIndex()}. */
	private Long index;
	/** Id pengguna yang terakhir mengubah baris ini (field bayangan audit). Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Id pengguna yang tercatat terakhir kali mengubah baris draft ini.
	 *
	 * <p>Field bayangan audit generik -- diisi otomatis lewat
	 * {@code AuditTimestampInterceptor.ubah(Object)} dari {@link #onUpdate()} berdasarkan sesi
	 * web/ZK yang sedang berjalan. TIDAK ADA kolom {@code @Column} eksplisit di getter ini (dipetakan
	 * lewat konvensi penamaan default Hibernate).</p>
	 */
	public String getOlehId() {
		return olehId;
	}

	/** Mengabaikan nilai kosong/blank -- lihat catatan di {@link #getOlehId()}. */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/** Nama pengguna yang terakhir mengubah baris ini (field bayangan audit). Lihat {@link #getOleh()}. */
	private String oleh;

	/** Representasi teks draft ini, yaitu {@link #getKode()} apa adanya. */
	public String toString() {
		return kode;
	}

	/** Mengabaikan nilai kosong/blank -- lihat catatan di {@link #getOleh()}. */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/** Nama pengguna yang tercatat terakhir kali mengubah baris draft ini (field bayangan audit, pasangan {@link #getOlehId()}). */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait daur hidup JPA yang berjalan tepat sebelum baris ini di-{@code UPDATE}.
	 *
	 * <p>Mendelegasikan pengisian {@link #getOleh()}, {@link #getOlehId()}, dan
	 * {@link #getTanggal_dirubah()} ke {@code AuditTimestampInterceptor.ubah(Object)} sehingga tiga
	 * field bayangan audit itu selalu konsisten tanpa perlu diisi manual di tiap pemanggil. Hanya
	 * dipicu pada pembaruan, bukan penyisipan baris baru.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** Field bayangan audit "terakhir diubah" -- lihat {@link #onUpdate()}. Nilai inisialisasi ({@code new Date()}) dipakai sampai baris ini pertama kali di-{@code UPDATE}. */
	private Date tanggal_dirubah = new Date();

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Nomor/kode draft (tampil di struk/label pesanan). Lihat {@link #getKode()}. */
	private String kode;
	/** Catatan bebas opsional. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Anggota koperasi pemesan (boleh {@code null} untuk pembeli umum). Lihat {@link #getAnggotaKoperasi()}. */
	private AnggotaKoperasi anggotaKoperasi;
	/** Meja kantin asal pesanan (bila berlaku). Lihat {@link #getMejaKantin()}. */
	private MejaKantin mejaKantin;
	/** Lokasi/toko fisik asal pesanan. Lihat {@link #getLokasi()}. */
	private Lokasi lokasi;
	/** Tanggal pembayaran direncanakan/tercatat. Lihat {@link #getTanggalPembayaran()}. */
	private Date tanggalPembayaran = new Date();
	/** Metode pembayaran yang dipilih. Lihat {@link #getCaraPembayaranKoperasi()}. */
	private CaraPembayaranKoperasi caraPembayaranKoperasi;
	/** Kode transaksi pembayaran online terkait (bila non-tunai daring). Lihat {@link #getKodePembayaranOnline()}. */
	private KodePembayaranOnline kodePembayaranOnline;
	/** Biaya/ongkos tambahan di luar harga barang. Lihat {@link #getBiaya()}. */
	private Double biaya = 0.0;
	/** Nilai retur atas draft ini. Lihat {@link #getRetur()}. */
	private Double retur = 0.0;
	/** Nilai diskon header (nominal atau persen, lihat {@link #getDiskonDalamPersen()}). Lihat {@link #getDiskon()}. */
	private Double diskon = 0.0;
	/** Penanda apakah {@link #getDiskon()} dalam bentuk persen ({@code true}) atau nominal ({@code false}). Lihat {@link #getDiskonDalamPersen()}. */
	private Boolean diskonDalamPersen = false;
	/** Total diskon akumulasi seluruh baris. Lihat {@link #getTotalDiskon()}. */
	private Double totalDiskon = 0.0;
	/** Total cashback akumulasi seluruh baris. Lihat {@link #getTotalCashback()}. */
	private Double totalCashback = 0.0;
	/** Persentase PPN yang berlaku. Lihat {@link #getPpn()}. */
	private Double ppn = 0.0;
	/** Nominal PPN hasil hitung. Lihat {@link #getHargaPpn()}. */
	private Double hargaPpn = 0.0;
	/** Toko asal pesanan -- lihat {@link #getToko()} untuk cara nilainya diturunkan dari {@link #getLokasi()}. */
	private Toko toko;
	/** Total tagihan draft ini (dasar perhitungan lunas/belum). Lihat {@link #getTotalBiaya()}. */
	private Double totalBiaya = 0.0;

	/** Bagian pembayaran tunai. Lihat {@link #getBayarTunai()}. */
	private Double bayarTunai = 0.0;
	/** Bagian pembayaran non-tunai. Lihat {@link #getBayarNonTunai()}. */
	private Double bayarNonTunai = 0.0;
	/** Kembalian yang diberikan ke pembeli. Lihat {@link #getKembalian()}. */
	private Double kembalian = 0.0;

	/** Total uang diterima ({@link #getBayarTunai()} + {@link #getBayarNonTunai()}) -- lihat {@link #getBayar()}, GETTER DESTRUKTIF. */
	private Double bayar = 0.0;

	/** Pengguna (kasir) yang membuat/menahan draft ini. Lihat {@link #getTbmuser()}. */
	private Tbmuser tbmuser;

	private String kasirLoginNama;

	private String namaMesin;

	/** Header {@link PembelianAnggotaKoperasi} yang melunasi draft ini; {@code null} = belum dibayar. Lihat {@link #getLunas()}. */
	private PembelianAnggotaKoperasi lunas;

	/** Jejak posting akunting atas draft ini (bila draft ikut diposting sebelum finalisasi). Lihat {@link #getPostingHistory()}. */
	private PostingHistory postingHistory;

	/**
	 * Mengubah rincian keranjang mentah (JSON dari klien POS/kantin) menjadi baris {@link DraftPembelian}
	 * yang menempel ke draft ini ({@code this}), lengkap dengan sub-baris "Produk Ekstra" (mis. topping
	 * tambahan pada satu item menu). Method ini analog persis dengan
	 * {@link PembelianAnggotaKoperasi#simpanRinci(Session, JSONArray, String, Date, Toko, KodePembayaranOnline,
	 * DraftPembelianAnggotaKoperasi)} cabang checkout-langsung, tetapi menulis ke tabel
	 * {@code koperasi.draft_pembelian} (bukan {@code koperasi.pembelian}) karena pesanan di sini BELUM
	 * dibayar -- baris-baris ini kelak "dipromosikan" menjadi {@code Pembelian} definitif saat draft
	 * difinalisasi (lihat {@link PembelianAnggotaKoperasi#simpanRinci} cabang
	 * {@code draftPembelianAnggotaKoperasi != null}, yang membaca balik baris {@link DraftPembelian}
	 * ini lewat query {@code draftPembelianAnggotaKoperasi = this}).
	 *
	 * <h3>Format {@code transaksi}</h3>
	 * <p>
	 * Setiap elemen array adalah satu baris item dengan field opsional yang dibaca dengan penjagaan
	 * {@code isNull} dan nilai cadangan seragam: {@code id}/{@code kode}/{@code nama} (identitas
	 * produk, cadangan {@code "_"} bila kode/nama kosong), {@code harga} (cadangan {@code 0.0}),
	 * {@code jumlah} (cadangan {@code 1.0}), {@code diskon} dan {@code cashback} (cadangan
	 * {@code 0.0}), serta {@code aturanDiskon} opsional (id {@link AturanDiskon} yang dimuat lewat
	 * {@link GeneralValueObject#ambilData(Class, String)}). Produk yang belum punya baris {@code Produk}
	 * cocok (berdasar {@code id}/{@code kode}) dibuat otomatis lewat
	 * {@link PembelianAnggotaKoperasi#resolveOrCreateProduk(Session, String, String, String, Double, Toko)}
	 * -- kode produk dipakai BERSAMA di dua kelas ini, bukan diduplikasi.
	 * </p>
	 *
	 * <h3>Gap-closure "Produk Ekstra"</h3>
	 * <p>
	 * Elemen bisa membawa array {@code ekstra} berisi item tambahan yang menumpang satu baris dasar
	 * (mis. tambahan telur pada satu porsi nasi goreng). Tiap item ekstra disimpan sebagai baris
	 * {@link DraftPembelian} TERPISAH dengan {@code indukId} menunjuk id baris DASAR (bukan ekstra
	 * lain) yang baru saja disimpan pada iterasi ini. <b>Beda krusial dari padanannya di
	 * {@link PembelianAnggotaKoperasi#simpanRinci}:</b> di sana {@code indukId} langsung menunjuk id
	 * {@code Pembelian} definitif (final), sedangkan di sini {@code indukId} menunjuk id
	 * {@link DraftPembelian} INDUK yang MASIH DRAFT -- nilai itu di-REMAP dua-pass ke id
	 * {@code Pembelian} yang benar saat draft ini difinalisasi (lihat cabang remap 2-pass di
	 * {@link PembelianAnggotaKoperasi#simpanRinci}). Kegagalan menyimpan satu item ekstra tidak
	 * menggagalkan baris dasarnya maupun item ekstra lain -- ditangkap lokal, dicatat lewat
	 * {@link ais.common.ErrorAuditUtil#record}, dan transaksi Hibernate-nya di-rollback secara
	 * terisolasi.
	 * </p>
	 *
	 * <h3>Isolasi kegagalan per baris</h3>
	 * <p>
	 * Bila pemanggil BELUM mengelola transaksinya sendiri ({@code transaksiDikelolaPemanggil == false}
	 * -- terdeteksi dari {@code session.getTransaction()} yang belum {@code isActive()}), method ini
	 * membuka dan meng-commit satu transaksi PENDEK per baris (produk lalu draft-pembelian). Bila
	 * penyimpanan satu baris gagal, exception ditangkap lokal (TIDAK dilempar ke pemanggil), dicatat
	 * ke {@link ais.common.ErrorAuditUtil}, dan transaksi yang sempat di-{@code begin()} untuk baris
	 * itu di-{@code rollback()} EKSPLISIT sebelum lanjut ke baris berikutnya. Rollback eksplisit ini
	 * BUKAN kosmetik: tanpanya, transaksi yang sudah {@code begin()} tapi gagal tetap berstatus
	 * "aktif" di mata Hibernate, sehingga panggilan {@code begin()} pada iterasi SELANJUTNYA dianggap
	 * no-op (transaksi dianggap sudah berjalan) -- akibatnya SEMUA item sesudah item yang bermasalah
	 * ikut gagal tersimpan secara diam-diam juga (satu item rusak "meracuni" seluruh sisa keranjang,
	 * termasuk yang datanya valid). Ini adalah perbaikan gap-closure "Muat ke Keranjang kosong walau
	 * toast sukses" -- sebelum rollback eksplisit ditambahkan, klien menerima respons sukses padahal
	 * sebagian besar/seluruh keranjang gagal tersimpan.
	 * </p>
	 *
	 * <p>Sebaliknya bila pemanggil SUDAH mengelola transaksinya sendiri
	 * ({@code transaksiDikelolaPemanggil == true}), method ini TIDAK membuka/meng-commit/me-rollback
	 * transaksi sendiri -- kegagalan pada baris apa pun langsung dilempar sebagai
	 * {@code RuntimeException} ke pemanggil (all-or-nothing, konsisten dengan ekspektasi transaksi
	 * besar yang dikelola di luar).</p>
	 *
	 * @param session          sesi Hibernate aktif.
	 * @param transaksi        array baris keranjang mentah dari klien, lihat format di atas.
	 * @param kodeUnik         awalan kode unik dipakai membentuk {@code kode} tiap baris
	 *                         {@link DraftPembelian} ({@code kodeUnik + "-" + kodeBarang}, dan
	 *                         {@code kodeUnik + "-" + kodeBarang + "-" + kodeEkstra} untuk baris ekstra).
	 * @param currentWaktu     waktu transaksi dicatat ke tiap baris {@link DraftPembelian#getWaktu()}.
	 * @param toko             toko konteks penyimpanan (dipakai resolusi/pembuatan produk dan baris draft).
	 * @param kodePembayaranOnline kode pembayaran online yang berlaku (boleh {@code null}), disalin ke
	 *                         tiap baris {@link DraftPembelian}.
	 * @return array JSON representasi baris {@link DraftPembelian} (termasuk baris ekstra) yang
	 *         berhasil disimpan -- baris yang gagal tersimpan (isolasi per baris di atas) TIDAK muncul
	 *         di array hasil ini, walau responsnya tetap berstatus "sukses" bagi pemanggil yang tidak
	 *         mengelola transaksinya sendiri.
	 * @see PembelianAnggotaKoperasi#simpanRinci(Session, JSONArray, String, Date, Toko, KodePembayaranOnline, DraftPembelianAnggotaKoperasi)
	 * @see PembelianAnggotaKoperasi#resolveOrCreateProduk(Session, String, String, String, Double, Toko)
	 */
	public JSONArray simpanRinci(Session session, JSONArray transaksi, String kodeUnik, Date currentWaktu, Toko toko,
			KodePembayaranOnline kodePembayaranOnline) {
		JSONArray arrayTransaksi = new JSONArray();
		final boolean transaksiDikelolaPemanggil = session.getTransaction() != null
				&& session.getTransaction().isActive();
		for (int i = 0; i < transaksi.length(); i++) {
			try {
				JSONObject objectTransaksi = transaksi.getJSONObject(i);
				String idBarang = objectTransaksi.isNull("id") ? null : objectTransaksi.get("id") + "";
				String kodeBarang = objectTransaksi.isNull("kode") ? "_" : objectTransaksi.get("kode") + "";
				String namaBarang = objectTransaksi.isNull("nama") ? "_" : objectTransaksi.get("nama") + "";
				Double hargaBarang = objectTransaksi.isNull("harga") ? 0.0
						: Double.parseDouble((objectTransaksi.get("harga") + "").trim());
				Double jumlahBarang = objectTransaksi.isNull("jumlah") ? 1.0
						: Double.parseDouble((objectTransaksi.get("jumlah") + "").trim());
				Double diskonBarang = objectTransaksi.isNull("diskon") ? 0.0
						: Double.parseDouble((objectTransaksi.get("diskon") + "").trim());
				Double cashbackBarang = objectTransaksi.isNull("cashback") ? 0.0
						: Double.parseDouble((objectTransaksi.get("cashback") + "").trim());
				Produk produk = PembelianAnggotaKoperasi.resolveOrCreateProduk(session, idBarang, kodeBarang,
						namaBarang, hargaBarang, toko);

				AturanDiskon aturanDiskon = (objectTransaksi.isNull("aturanDiskon") ? null
						: (AturanDiskon) GeneralValueObject.ambilData(AturanDiskon.class,
								(objectTransaksi.get("aturanDiskon") + "").trim()));

				DraftPembelian pembelian = new DraftPembelian();
				pembelian.setDraftPembelianAnggotaKoperasi(this);
				pembelian.setAnggotaKoperasi(this.getAnggotaKoperasi());
				pembelian.setKode(kodeUnik + "-" + kodeBarang);
				pembelian.setNama(namaBarang);
				pembelian.setKodePembayaranOnline(kodePembayaranOnline);
				pembelian.setQty(jumlahBarang);
				pembelian.setDiskon(diskonBarang);
				pembelian.setAturanDiskon(aturanDiskon);
				pembelian.setCashback(cashbackBarang);
				pembelian.setHargaSatuan(hargaBarang);
				pembelian.setProduk(produk);
				pembelian.setToko(toko);
				pembelian.setWaktu(currentWaktu);
				if (!transaksiDikelolaPemanggil) session.getTransaction().begin();
				session.save(pembelian);
				if (!transaksiDikelolaPemanggil) session.getTransaction().commit();

				JSONObject data = new JSONObject();
				Common.insertProperty(DraftPembelian.class, pembelian, data, "", 1, "siswa", "calonSiswa", "mahasiswa",
						"biodataCalonMahasiswa", "pembelianAnggotaKoperasi", "tbmuser", "kodePembayaranOnline", "toko");
				arrayTransaksi.put(data);

				// Gap-closure "Produk Ekstra" -- sama pola persis dgn PembelianAnggotaKoperasi.simpanRinci
				// (lihat JavaDoc di sana), TAPI indukId di sini menunjuk ke id DraftPembelian INDUK ini
				// (bukan Pembelian) -- di-remap 2-pass jadi id Pembelian yg benar saat draft difinalisasi
				// (lihat PembelianAnggotaKoperasi.simpanRinci cabang draft-finalization).
				if (!objectTransaksi.isNull("ekstra")) {
					JSONArray ekstraArr = objectTransaksi.optJSONArray("ekstra");
					for (int k = 0; ekstraArr != null && k < ekstraArr.length(); k++) {
						try {
							JSONObject e = ekstraArr.getJSONObject(k);
							String idEkstra = e.isNull("id") ? null : e.get("id") + "";
							String kodeEkstra = e.isNull("kode") ? "_" : e.get("kode") + "";
							String namaEkstra = e.isNull("nama") ? "_" : e.get("nama") + "";
							Double hargaEkstra = e.isNull("harga") ? 0.0
									: Double.parseDouble((e.get("harga") + "").trim());
							Double jumlahEkstra = e.isNull("jumlah") ? 1.0
									: Double.parseDouble((e.get("jumlah") + "").trim());

							Produk produkEkstra = PembelianAnggotaKoperasi.resolveOrCreateProduk(session, idEkstra,
									kodeEkstra, namaEkstra, hargaEkstra, toko);

							DraftPembelian pembelianEkstra = new DraftPembelian();
							pembelianEkstra.setDraftPembelianAnggotaKoperasi(this);
							pembelianEkstra.setAnggotaKoperasi(this.getAnggotaKoperasi());
							pembelianEkstra.setKode(kodeUnik + "-" + kodeBarang + "-" + kodeEkstra);
							pembelianEkstra.setNama(namaEkstra);
							pembelianEkstra.setKodePembayaranOnline(kodePembayaranOnline);
							pembelianEkstra.setQty(jumlahBarang * jumlahEkstra);
							pembelianEkstra.setDiskon(0.0);
							pembelianEkstra.setCashback(0.0);
							pembelianEkstra.setHargaSatuan(hargaEkstra);
							pembelianEkstra.setProduk(produkEkstra);
							pembelianEkstra.setToko(toko);
							pembelianEkstra.setWaktu(currentWaktu);
							pembelianEkstra.setIndukId(pembelian.getId());
							if (!transaksiDikelolaPemanggil) session.getTransaction().begin();
							session.save(pembelianEkstra);
							if (!transaksiDikelolaPemanggil) session.getTransaction().commit();

							JSONObject dataEkstra = new JSONObject();
							Common.insertProperty(DraftPembelian.class, pembelianEkstra, dataEkstra, "", 1, "siswa",
									"calonSiswa", "mahasiswa", "biodataCalonMahasiswa", "pembelianAnggotaKoperasi",
									"tbmuser", "kodePembayaranOnline", "toko");
							arrayTransaksi.put(dataEkstra);
						} catch (Exception eEkstra) {
							if (transaksiDikelolaPemanggil) {
								throw new RuntimeException("Gagal menyimpan produk ekstra draft baris " + (k + 1), eEkstra);
							}
							ais.common.ErrorAuditUtil.record(eEkstra,
									"auto-audit src/ais/database/model/koperasi/DraftPembelianAnggotaKoperasi.java:simpanRinci:ekstra");
							try {
								if (session.getTransaction() != null && session.getTransaction().isActive()) {
									session.getTransaction().rollback();
								}
							} catch (Exception eRollback) {
								ais.common.ErrorAuditUtil.record(eRollback,
										"auto-audit(rollback) src/ais/database/model/koperasi/DraftPembelianAnggotaKoperasi.java:simpanRinci:ekstra");
							}
						}
					}
				}

			} catch (Exception e) {
				if (transaksiDikelolaPemanggil) {
					throw new RuntimeException("Gagal menyimpan rincian draft baris " + (i + 1), e);
				}
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/koperasi/DraftPembelianAnggotaKoperasi.java:200");
				// Gap-closure "Muat ke Keranjang kosong walau toast sukses": tanpa rollback di sini,
				// transaction Hibernate yg SUDAH di-begin() di atas (session.save(produk)/session.save
				// (pembelian)) tertinggal aktif-tapi-gagal begitu item ini gagal -- begin() berikutnya di
				// iterasi SELANJUTNYA jadi no-op (transaction dianggap sudah aktif), sehingga SEMUA item
				// sesudahnya ikut gagal tersimpan diam-diam juga (satu item bermasalah meracuni seluruh
				// keranjang). Rollback eksplisit memastikan tiap item benar-benar terisolasi.
				try {
					if (session.getTransaction() != null && session.getTransaction().isActive()) {
						session.getTransaction().rollback();
					}
				} catch (Exception eRollback) {
					ais.common.ErrorAuditUtil.record(eRollback, "auto-audit(rollback) src/ais/database/model/koperasi/DraftPembelianAnggotaKoperasi.java:simpanRinci");
				}
			}
		}

		return arrayTransaksi;
	}

	/** Konstruktor kosong wajib JPA/Hibernate. */
	public DraftPembelianAnggotaKoperasi() {
	}

	/** Kunci utama (identity/auto-increment); {@code insertable = false} karena nilainya diserahkan sepenuhnya ke DB. */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	/** Nomor/kode draft, mis. dipakai membentuk awalan {@code kodeUnik} pada {@link #simpanRinci}. */
	@Column(name = "kode", nullable = false, length = 255)
	public String getKode() {
		return this.kode;
	}

	public void setKode(String kode) {
		this.kode = kode;
	}

	/** Catatan bebas opsional atas draft ini. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	public void setAnggotaKoperasi(AnggotaKoperasi anggotaKoperasi) {
		this.anggotaKoperasi = anggotaKoperasi;
	}

	/**
	 * Anggota koperasi pemesan (boleh {@code null} untuk pembeli umum non-anggota).
	 *
	 * <p>{@code check(anggotaKoperasi)} meresolusi proxy lazy Hibernate lewat cache identitas entity
	 * (lihat {@link GeneralValueObject#check(Object)}) sebelum dikembalikan -- pola berulang di semua
	 * relasi {@code @ManyToOne} kelas ini, bukan hanya di sini. Bila draft ini kelak dipakai
	 * memfinalisasi header {@link PembelianAnggotaKoperasi}, nilai inilah yang disalin ulang oleh
	 * {@code PembelianAnggotaKoperasi.getAnggotaKoperasi()} (lihat JavaDoc kelas).</p>
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "anggota_koperasi", nullable = true)
	public AnggotaKoperasi getAnggotaKoperasi() {
		anggotaKoperasi = check(anggotaKoperasi);
		return anggotaKoperasi;
	}

	public void setIndex(Long index) {
		this.index = index;
	}

	/** Nomor urut tampilan sementara, TIDAK dipetakan ke kolom DB (tanpa {@code @Column}) -- murni bantu UI, hilang setelah entity dimuat ulang. */
	public Long getIndex() {
		return index;
	}

	/** Lokasi/toko fisik asal pesanan. Nilai inilah yang menentukan {@link #getToko()} bila terisi. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "lokasi", nullable = true)
	public Lokasi getLokasi() {
		lokasi = check(lokasi);
		return lokasi;
	}

	public void setLokasi(Lokasi lokasi) {
		this.lokasi = lokasi;
	}

	/** Biaya/ongkos tambahan di luar harga barang. TIDAK beranotasi {@code @Column} eksplisit -- dipetakan lewat konvensi penamaan default Hibernate. */
	public Double getBiaya() {
		return biaya;
	}

	public void setBiaya(Double biaya) {
		this.biaya = biaya;
	}

	/** Nilai diskon header; bentuknya (nominal/persen) ditentukan {@link #getDiskonDalamPersen()}. Tanpa {@code @Column} eksplisit. */
	public Double getDiskon() {
		return diskon;
	}

	public void setDiskon(Double diskon) {
		this.diskon = diskon;
	}

	/** {@code true} bila {@link #getDiskon()} dalam persen, {@code false} (default) bila nominal rupiah langsung. */
	@Column(name = "diskon_dalam_persen")
	public Boolean getDiskonDalamPersen() {
		return diskonDalamPersen;
	}

	public void setDiskonDalamPersen(Boolean diskonDalamPersen) {
		this.diskonDalamPersen = diskonDalamPersen;
	}

	/** Total diskon akumulasi seluruh baris {@link DraftPembelian} milik draft ini. */
	@Column(name = "total_diskon")
	public Double getTotalDiskon() {
		return totalDiskon;
	}

	public void setTotalDiskon(Double totalDiskon) {
		this.totalDiskon = totalDiskon;
	}

	/** Persentase PPN yang berlaku atas draft ini. Tanpa {@code @Column} eksplisit. */
	public Double getPpn() {
		return ppn;
	}

	public void setPpn(Double ppn) {
		this.ppn = ppn;
	}

	/** Bagian pembayaran tunai; getter menormalkan {@code null} jadi {@code 0.0} (field diisi balik -- BUKAN getter destruktif krn hasil normalisasi identik dgn semantik "belum dibayar tunai"). */
	@Column(name = "bayar_tunai")
	public Double getBayarTunai() {
		if (bayarTunai == null) {
			bayarTunai = 0.0;
		}
		return bayarTunai;
	}

	public void setBayarTunai(Double bayarTunai) {
		this.bayarTunai = bayarTunai;
	}

	/** Bagian pembayaran non-tunai; sama pola normalisasi {@code null}→{@code 0.0} dgn {@link #getBayarTunai()}. */
	@Column(name = "bayar_non_tunai")
	public Double getBayarNonTunai() {
		if (bayarNonTunai == null) {
			bayarNonTunai = 0.0;
		}
		return bayarNonTunai;
	}

	public void setBayarNonTunai(Double bayarNonTunai) {
		this.bayarNonTunai = bayarNonTunai;
	}

	public void setTanggalPembayaran(Date tanggalPembayaran) {
		this.tanggalPembayaran = tanggalPembayaran;
	}

	/** Tanggal pembayaran direncanakan/tercatat. Ikut disalin oleh {@code PembelianAnggotaKoperasi.getTanggalPembayaran()} bila draft ini terpasang di header (lihat JavaDoc kelas). */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_pembayaran")
	public Date getTanggalPembayaran() {
		return tanggalPembayaran;
	}

	public void setTbmuser(Tbmuser tbmuser) {
		this.tbmuser = tbmuser;
	}

	/** Pengguna (kasir) yang membuat/menahan draft ini. Ikut disalin oleh {@code PembelianAnggotaKoperasi.getTbmuser()} bila draft ini terpasang di header. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tbmuser", nullable = true)
	public Tbmuser getTbmuser() {
		tbmuser = check(tbmuser);
		return tbmuser;
	}

	/**
	 * Nama kasir yang BENAR-BENAR login di mesin POS saat pesanan/keranjang ditahan -- field baru
	 * terpisah dari {@link #getOleh()}/{@link #getOlehId()} (audit generik, selalu jatuh ke
	 * {@code "external_update"} utk permintaan lewat PosApi). Pola IDENTIK dgn
	 * {@link PembelianAnggotaKoperasi#getKasirLoginNama()} -- lihat JavaDoc di sana utk alasan
	 * lengkap. {@code null} utk pesanan online (lihat {@code PosApi.dariPembeliOnline} -- member yg
	 * checkout sendiri, bukan kasir).
	 */
	@Column(name = "kasir_login_nama", nullable = true)
	public String getKasirLoginNama() {
		return kasirLoginNama;
	}

	public void setKasirLoginNama(String kasirLoginNama) {
		this.kasirLoginNama = kasirLoginNama;
	}

	/**
	 * Nama mesin POS fisik asal pesanan ini -- pola IDENTIK dgn
	 * {@link PembelianAnggotaKoperasi#getNamaMesin()}. {@code null} utk pesanan online (tak ada
	 * "mesin POS" -- pembeli checkout sendiri lewat kanal lain).
	 */
	@Column(name = "nama_mesin", nullable = true, length = 100)
	public String getNamaMesin() {
		return namaMesin;
	}

	public void setNamaMesin(String namaMesin) {
		this.namaMesin = namaMesin;
	}

	public void setRetur(Double retur) {
		this.retur = retur;
	}

	/** Nilai retur atas draft ini. Tanpa {@code @Column} eksplisit. */
	public Double getRetur() {
		return retur;
	}

	public void setTotalBiaya(Double totalBiaya) {
		this.totalBiaya = totalBiaya;
	}

	/** Total tagihan draft ini; getter menormalkan {@code null} jadi {@code 0.0} (field diisi balik). Basis perbandingan {@link #getBayarTunai()}+{@link #getBayarNonTunai()} di alur kelunasan (lihat {@code KantinHelper#sinkronkanRincianDraftUntukFinalisasi}). */
	@Column(name = "total_biaya")
	public Double getTotalBiaya() {
		if (totalBiaya == null) {
			totalBiaya = 0.0;
		}
		return totalBiaya;
	}

	public void setKembalian(Double kembalian) {
		this.kembalian = kembalian;
	}

	/** Kembalian yang diberikan ke pembeli. Tanpa {@code @Column} eksplisit. */
	public Double getKembalian() {
		return kembalian;
	}

	public void setHargaPpn(Double hargaPpn) {
		this.hargaPpn = hargaPpn;
	}

	/** Nominal PPN hasil hitung atas draft ini. */
	@Column(name = "harga_ppn")
	public Double getHargaPpn() {
		return hargaPpn;
	}

	/** Jejak posting akunting atas draft ini, bila ada (mis. pesanan yang diposting sebagai piutang sebelum resmi dibayar/difinalisasi). */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "posting_history", nullable = true)
	public PostingHistory getPostingHistory() {
		return postingHistory;
	}

	public void setPostingHistory(PostingHistory postingHistory) {
		this.postingHistory = postingHistory;
	}



	/**
	 * Total uang diterima untuk draft ini.
	 *
	 * <p><b>Getter destruktif</b> -- pola berulang di domain finansial AIS (lihat juga
	 * {@code PembelianAnggotaKoperasi.getLunas()}): field {@code bayar} DITIMPA hasil penjumlahan
	 * {@link #getBayarNonTunai()} + {@link #getBayarTunai()} pada SETIAP pemanggilan, sehingga
	 * {@link #setBayar(Double)} tidak pernah berpengaruh terhadap nilai yang dibaca kembali -- setter
	 * itu ada semata melengkapi bentuk JavaBean. Karena Hibernate memakai getter yang sama saat
	 * menulis kolom (bila {@code bayar} pernah dipetakan sebagai kolom), nilai turunan ini pun ikut
	 * tersimpan.</p>
	 */
	public Double getBayar() {
		bayar = getBayarNonTunai() + getBayarTunai();
		return bayar;
	}

	/** Tidak berpengaruh -- lihat catatan getter destruktif di {@link #getBayar()}. */
	public void setBayar(Double bayar) {
		this.bayar = bayar;
	}

	/** Metode pembayaran yang dipilih untuk draft ini. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "cara_pembayaran_koperasi", nullable = true)
	public CaraPembayaranKoperasi getCaraPembayaranKoperasi() {
		caraPembayaranKoperasi = check(caraPembayaranKoperasi);
		return caraPembayaranKoperasi;
	}

	public void setCaraPembayaranKoperasi(CaraPembayaranKoperasi caraPembayaranKoperasi) {
		this.caraPembayaranKoperasi = caraPembayaranKoperasi;
	}

	/** Kode transaksi pembayaran online terkait (boleh {@code null} untuk tunai/non-daring). Ikut disalin oleh {@code PembelianAnggotaKoperasi.getKodePembayaranOnline()} bila draft ini terpasang di header. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "kode_pembayaran_online", nullable = true)
	public KodePembayaranOnline getKodePembayaranOnline() {
		return kodePembayaranOnline;
	}

	public void setKodePembayaranOnline(KodePembayaranOnline kodePembayaranOnline) {
		this.kodePembayaranOnline = kodePembayaranOnline;
	}

	/**
	 * Toko asal pesanan draft ini.
	 *
	 * <p>Diturunkan (bukan sekadar dibaca) dari {@link #getLokasi()} bila lokasi tersebut sudah punya
	 * {@code Toko}-nya sendiri -- baris {@code toko} milik draft ini HANYA dipakai sebagai cadangan
	 * ({@code check(toko)}) saat {@link #getLokasi()} kosong atau lokasinya belum terhubung ke toko
	 * mana pun. Konsekuensinya, mengubah {@link #setLokasi(Lokasi)} lalu menyimpan ulang draft ini
	 * bisa mengubah nilai {@code toko} yang tersimpan tanpa {@link #setToko(Toko)} pernah dipanggil.
	 * Ikut disalin oleh {@code PembelianAnggotaKoperasi.getToko()} bila draft ini terpasang di
	 * header.</p>
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "toko", nullable = true)
	public Toko getToko() {
		if (getLokasi() != null && getLokasi().getToko() != null) {
			toko = getLokasi().getToko();
		} else {
			toko = check(toko);
		}
		return toko;
	}

	public void setToko(Toko toko) {
		this.toko = toko;
	}

	/** Total cashback akumulasi seluruh baris; getter menormalkan {@code null} jadi {@code 0.0} tanpa mengisi balik field. Tanpa {@code @Column} eksplisit. */
	public Double getTotalCashback() {
		return totalCashback == null ? 0.0 : totalCashback;
	}

	public void setTotalCashback(Double totalCashback) {
		this.totalCashback = totalCashback;
	}

	/** Meja kantin asal pesanan (bila berlaku, mis. pesanan dine-in). Ikut disalin oleh {@code PembelianAnggotaKoperasi.getMejaKantin()} bila draft ini terpasang di header. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "meja_kantin", nullable = true)
	public MejaKantin getMejaKantin() {
		mejaKantin = check(mejaKantin);
		return mejaKantin;
	}

	public void setMejaKantin(MejaKantin mejaKantin) {
		this.mejaKantin = mejaKantin;
	}

	/**
	 * Header {@link PembelianAnggotaKoperasi} yang melunasi draft ini; {@code null} berarti draft
	 * belum dibayar.
	 *
	 * <p><b>Gerbang anti-bayar-dobel.</b> Pemanggil finalisasi (mis.
	 * {@code KantinHelper#sinkronkanRincianDraftUntukFinalisasi}) memeriksa nilai ini di AWAL dan
	 * langsung berhenti bila sudah terisi -- draft yang sudah lunas tidak diproses ulang menjadi
	 * header baru. Setelah {@link #simpanRinci} baris {@link DraftPembelian} draft ini "dipromosikan"
	 * menjadi baris {@code Pembelian} definitif oleh {@code PembelianAnggotaKoperasi.simpanRinci},
	 * pemanggil-lah yang memasang field ini lewat {@link #setLunas(PembelianAnggotaKoperasi)} --
	 * entity ini sendiri TIDAK menolak dipanggil dua kali; kedisiplinan gerbang sepenuhnya ada di
	 * pemanggil.</p>
	 *
	 * <p>Jangan bingung dengan {@code PembelianAnggotaKoperasi.getLunas()} yang berbeda sama sekali:
	 * milik header itu mengembalikan {@code Boolean} hasil hitung (apakah uang diterima menutup
	 * total struk), bukan referensi ke draft/header lain.</p>
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "lunas", nullable = true)
	public PembelianAnggotaKoperasi getLunas() {
		return lunas;
	}

	/** Menandai draft ini lunas dengan menautkan header hasil pembayarannya -- lihat gerbang anti-bayar-dobel di {@link #getLunas()}. */
	public void setLunas(PembelianAnggotaKoperasi lunas) {
		this.lunas = lunas;
	}
}
