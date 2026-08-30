package ais.action.master.inventory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.inventory.Produk;
import ais.database.model.inventory.StokOpname;
import ais.database.model.inventory.Toko;

/**
 * Backend bersama untuk fitur <b>Stok Opname (SO) By Scan HP / PDT</b>.
 *
 * <h2>Apa fungsi kelas ini?</h2>
 * <p>
 * Kelas utilitas ini adalah "otak" dari proses stok opname berbasis scan barcode. Ia dipakai
 * <b>bersama</b> oleh dua antarmuka yang berbeda namun harus berperilaku <b>persis sama</b>:
 * </p>
 * <ul>
 * <li>versi <b>JSP</b> ({@code webapp/.../kantin/stok/opname_scan.jsp} + {@code _service.jsp}), dan</li>
 * <li>versi <b>ZK</b> ({@link StokOpnameScanKantinAction}).</li>
 * </ul>
 * <p>
 * Dengan menaruh seluruh logika data di satu tempat, perbaikan/perawatan di kemudian hari cukup
 * dilakukan sekali dan otomatis berlaku untuk kedua tampilan — inilah inti dari prinsip
 * <i>single source of truth</i> yang menghemat biaya pemeliharaan jangka panjang.
 * </p>
 *
 * <h2>Bagaimana alur kerjanya?</h2>
 * <ol>
 * <li><b>Cari produk dari barcode.</b> Barcode hasil scan dicocokkan ke {@link Produk#getKode()}
 * dalam lingkup satu toko/kios. Lihat {@link #cariProdukByBarcode(Session, Long, String)}.</li>
 * <li><b>Hitung stok sistem.</b> Stok menurut catatan aplikasi dihitung dengan rumus yang
 * <i>identik</i> dengan {@link StokKantinUtil#recomputeStokProduk(Long)}, yaitu
 * <code>(barang masuk pengadaan) + (akumulasi selisih opname) - (barang terjual) -
 * (pemakaian bahan baku)</code>. Lihat {@link #hitungStokSistem(Session, Long)}.</li>
 * <li><b>Simpan hasil hitung fisik.</b> Saat petugas menyimpan jumlah fisik nyata, sebuah baris
 * {@link StokOpname} dibuat. Selisih (fisik − sistem) dihitung otomatis oleh getter entitas
 * {@code StokOpname.getSelisih()} pada saat disimpan, lalu stok produk di-<i>recompute</i> agar
 * langsung sinkron. Lihat {@link #simpanOpname(Session, Long, Long, double, String, String)}.</li>
 * <li><b>Ringkasan harian.</b> {@link #ringkasanHariIni(Session, Long)} menyediakan angka ringkas
 * (jumlah produk, total kelebihan, total kekurangan) untuk panel dasbor di kedua tampilan.</li>
 * </ol>
 *
 * <h2>Aturan sesi (penting untuk stabilitas)</h2>
 * <p>
 * Semua method di sini bekerja di atas {@link HibernateUtil#currentSession()} (sesi yang
 * <b>dikelola</b> kerangka kerja). Sesi jenis ini <b>tidak boleh</b> ditutup manual di sini karena
 * akan ditutup otomatis oleh kerangka kerja; menutupnya manual justru memutus transaksi pemanggil.
 * Karena itu kelas ini sengaja <b>tidak</b> membuka {@code openSession()}/{@code currentNativeSession()}
 * sendiri sehingga tidak ada sesi yang perlu ditutup di blok {@code finally}.
 * </p>
 *
 * <h2>Efisiensi memori &amp; performa</h2>
 * <p>
 * Perhitungan stok sistem dipadatkan menjadi <b>satu</b> kueri SQL (memakai sub-kueri agregat)
 * alih-alih empat kueri terpisah — menekan jumlah perjalanan ke basis data, objek sementara, dan
 * tekanan <i>garbage collector</i>. Pencarian produk memakai HQL ter-parameter dengan
 * {@code setMaxResults(1)} agar hanya satu baris yang dimuat ke memori. Tidak ada koleksi besar
 * yang ditahan; pemanggil hanya menerima entitas/nilai yang benar-benar diperlukan.
 * </p>
 *
 * <h2>Keamanan tipe &amp; kompatibilitas</h2>
 * <p>
 * Seluruh kode dijaga kompatibel dengan <b>Java 1.7</b> (tanpa lambda/stream/<i>try-with-resources</i>)
 * dan blok {@code try/catch} bergaya 1.6 (tanpa <i>multi-catch</i>). Nilai numerik dari basis data
 * dinormalkan lewat {@link #toDouble(Object)} agar aman terhadap {@link BigDecimal}/{@link Number}.
 * </p>
 *
 * @author AIS
 */
public final class StokOpnameScanUtil {

	/** Kelas utilitas — tidak untuk di-instansiasi. */
	private StokOpnameScanUtil() {
	}

	/**
	 * Cari produk berdasarkan barcode hasil scan. Dicocokkan (tidak peka huruf besar/kecil) ke
	 * {@link Produk#getKode()} ATAU {@link Produk#getBarcode()} -- scanner fisik bisa memindai kode
	 * internal toko (tercetak di label toko sendiri) MAUPUN barcode/UPC asli kemasan produk (dari
	 * pemasok), keduanya harus dikenali. Dibatasi pada satu toko serta produk yang aktif.
	 *
	 * @param session  sesi Hibernate aktif; bila {@code null} dipakai {@code currentSession()}.
	 * @param tokoId   id toko/kios pemilik produk (wajib).
	 * @param barcode  kode barcode hasil scan/ketik (wajib, akan di-<i>trim</i>).
	 * @return entitas {@link Produk} pertama yang cocok, atau {@code null} bila tidak ditemukan
	 *         atau argumen tidak lengkap.
	 */
	public static Produk cariProdukByBarcode(Session session, Long tokoId, String barcode) {
		if (tokoId == null || barcode == null) {
			return null;
		}
		String bc = barcode.trim();
		if (bc.length() == 0) {
			return null;
		}
		Session s = session == null ? HibernateUtil.currentSession() : session;
		Query q = s.createQuery("from Produk p where p.toko.id = :toko "
				+ "and (p.aktif is null or p.aktif = true) "
				+ "and (upper(p.kode) = upper(:bc) or upper(p.barcode) = upper(:bc)) order by p.id asc");
		q.setParameter("toko", tokoId);
		q.setParameter("bc", bc);
		q.setMaxResults(1);
		return (Produk) q.uniqueResult();
	}

	/**
	 * Hitung stok sistem terkini sebuah produk (hanya membaca; tidak mengubah data). Rumusnya
	 * identik dengan {@link StokKantinUtil#recomputeStokProduk(Long)} sehingga angka yang tampil
	 * saat opname konsisten dengan stok yang tersimpan.
	 *
	 * <p>
	 * Empat komponen digabung dalam satu kueri agar hemat perjalanan basis data:
	 * <code>masuk(pengadaan) + Σselisih(opname) − keluar(pembelian) − pakai(bahan baku)</code>.
	 * </p>
	 *
	 * @param session  sesi Hibernate; bila {@code null} dipakai {@code currentSession()}.
	 * @param produkId id produk (wajib).
	 * @return jumlah stok sistem; {@code 0} bila {@code produkId} null atau terjadi galat baca.
	 */
	public static double hitungStokSistem(Session session, Long produkId) {
		if (produkId == null) {
			return 0d;
		}
		Session s = session == null ? HibernateUtil.currentSession() : session;
		String sql = "SELECT "
				+ "  COALESCE((SELECT SUM(qty) FROM koperasi.pengadaan_produk WHERE produk = :p),0)"
				+ "+ COALESCE((SELECT SUM(selisih) FROM koperasi.stok_opname WHERE produk = :p),0)"
				+ "- COALESCE((SELECT SUM(qty) FROM koperasi.pembelian WHERE produk = :p),0)"
				+ "- COALESCE((SELECT SUM(qty) FROM koperasi.pemakaian_bahan_baku WHERE produk = :p),0)"
				// Produksi (Fase 0 dok. 49) -- selaras dengan baseline StokOpnameKantinAction.
				+ "+ COALESCE((SELECT SUM(COALESCE(qty_masuk,0) - COALESCE(qty_keluar,0)) FROM koperasi.mutasi_stok_produksi WHERE produk = :p),0)";
		try {
			SQLQuery q = s.createSQLQuery(sql);
			q.setParameter("p", produkId);
			return toDouble(q.uniqueResult());
		} catch (Exception e) {
			return 0d;
		}
	}

	/**
	 * Simpan satu hasil scan opname lalu segarkan stok produk.
	 *
	 * <p>
	 * Stok sistem historis diambil dari {@link #hitungStokSistem(Session, Long)} saat opname terjadi
	 * (bukan stok terkini setelahnya). Selisih (fisik − sistem) dihitung otomatis di getter entitas
	 * {@code StokOpname.getSelisih()} ketika disimpan, dan {@link StokKantinUtil#recomputeStokProduk(Long)}
	 * dipanggil agar kolom stok produk langsung sinkron.
	 * </p>
	 *
	 * @param session    sesi Hibernate; bila {@code null} dipakai {@code currentSession()}.
	 * @param tokoId     id toko/kios (wajib).
	 * @param produkId   id produk (wajib).
	 * @param stokFisik  jumlah fisik hasil hitung nyata.
	 * @param keterangan alasan selisih (opsional, mis. "barang basi"/"hilang").
	 * @param oleh       userId petugas yang melakukan opname (opsional).
	 * @return entitas {@link StokOpname} yang tersimpan (selisih sudah terisi).
	 * @throws Exception bila toko/produk tidak valid atau penyimpanan gagal.
	 */
	public static StokOpname simpanOpname(Session session, Long tokoId, Long produkId, double stokFisik,
			String keterangan, String oleh) throws Exception {
		if (tokoId == null) {
			throw new Exception("Toko/kios wajib dipilih.");
		}
		if (produkId == null) {
			throw new Exception("Produk tidak ditemukan.");
		}
		Session s = session == null ? HibernateUtil.currentSession() : session;
		Toko toko = (Toko) s.get(Toko.class, tokoId);
		Produk produk = (Produk) s.get(Produk.class, produkId);
		if (toko == null) {
			throw new Exception("Toko/kios tidak ditemukan.");
		}
		if (produk == null) {
			throw new Exception("Produk tidak ditemukan.");
		}
		if (produk.getToko() == null || produk.getToko().getId() == null
				|| !tokoId.equals(produk.getToko().getId())) {
			throw new Exception("Produk tidak termasuk toko yang sedang dipilih. Pilih toko yang benar lalu ulangi.");
		}

		StokOpname so = new StokOpname();
		so.setToko(toko);
		so.setProduk(produk);
		so.setStokSistem(Double.valueOf(hitungStokSistem(s, produkId)));
		so.setStokFisik(Double.valueOf(stokFisik));
		// WAJIB ditulis eksplisit di sini -- JANGAN andalkan getSelisih() (computed getter, hanya
		// memutasi field in-memory saat DIPANGGIL) utk mengisi kolom ini secara "otomatis" saat
		// disimpan. Pola getter-terkomputasi-tanpa-dipanggil-sebelum-save ini SUDAH berulang kali
		// jadi sumber bug data basi di modul lain (dashboard/SOP) -- di sini konsekuensinya kolom
		// selisih di DB tetap NULL/0 walau stokFisik yang diunggah benar, sehingga
		// StokKantinUtil.recomputeStokProduk() (baca kolom mentah via SQL native, BUKAN lewat getter)
		// menghitung ulang stok TANPA kontribusi opname ini sama sekali.
		so.setSelisih(Double.valueOf(so.getStokFisik() - so.getStokSistem()));
		so.setWaktuOpname(new Date());
		so.setKeterangan(keterangan);
		so.setOleh(oleh);
		Common.refreshSaveOrUpdate(s, so);

		// Stok produk dihitung ulang otomatis (selisih opname memengaruhi stok). WAJIB pakai varian
		// "Native(session, ...)" -- BUKAN recomputeStokProduk(Long) polos -- krn method itu membaca
		// lewat HibernateUtil.currentSession() (session ZK/JSP thread-bound), yg BEDA dgn sesi eksplisit
		// `s` di atas ketika simpanOpname() dipanggil dari konteks Servlet API (KantinHelper.
		// produkImporExcelKomit/soSimpan membuka sesi sendiri via openSession(), bukan ZK/JSP). Recompute
		// via currentSession() lalu membaca stok_opname di SESI/KONEKSI LAIN yg belum melihat baris `so`
		// yg BARU disimpan (belum commit) -- kontribusi opname ini jadi dianggap 0, stok produk TETAP
		// nilai lama walau selisih sudah benar tersimpan. Native(s, ...) memakai SESI YANG SAMA (satu
		// UPDATE SQL dlm transaksi yg sama) sehingga selalu melihat tulisannya sendiri, di ZK/JSP maupun
		// Servlet API. Efek samping "sinkron harga beli dari pengadaan terbaru" pada recomputeStokProduk
		// polos SENGAJA tidak ikut dipakai di sini -- opname adalah koreksi KUANTITAS, bukan alasan utk
		// mengubah harga beli/jual produk.
		StokKantinUtil.recomputeStokProdukNative(s, produkId);
		return so;
	}

	/** Baca stok akhir yang sudah tersimpan pada master produk di sesi/transaksi yang sama. */
	public static double stokProdukTerkini(Session session, Long produkId) {
		if (produkId == null) return 0d;
		Session s = session == null ? HibernateUtil.currentSession() : session;
		SQLQuery q = s.createSQLQuery("SELECT COALESCE(stok,0) FROM koperasi.produk WHERE id = :produk");
		q.setParameter("produk", produkId);
		return toDouble(q.uniqueResult());
	}

	/**
	 * Membatalkan satu hasil opname dengan jurnal kompensasi, bukan menghapus riwayat asli.
	 * Cara ini membuat audit tetap utuh dan aman bila sesudah opname sudah ada mutasi lain:
	 * hanya kontribusi selisih opname asal yang dibalik.
	 */
	public static StokOpname batalkanOpname(Session session, Long tokoId, Long opnameId,
			String alasan, String oleh) throws Exception {
		if (tokoId == null || opnameId == null) throw new Exception("Catatan Stok Opname tidak ditemukan.");
		Session s = session == null ? HibernateUtil.currentSession() : session;
		StokOpname asal = (StokOpname) s.get(StokOpname.class, opnameId);
		if (asal == null || asal.getToko() == null || !tokoId.equals(asal.getToko().getId())) {
			throw new Exception("Catatan Stok Opname tidak ditemukan pada toko yang dipilih.");
		}
		String ketAsal = asal.getKeterangan() == null ? "" : asal.getKeterangan();
		if (ketAsal.startsWith("[BATAL_SO:")) throw new Exception("Jurnal pembatalan tidak dapat dibatalkan kembali.");
		if (asal.getPostingHistory() != null) {
			throw new Exception("Stok Opname sudah diposting ke jurnal. Batalkan posting akuntansi terlebih dahulu.");
		}
		String penanda = "[BATAL_SO:" + opnameId + "]";
		SQLQuery sudah = s.createSQLQuery("SELECT id FROM koperasi.stok_opname WHERE toko=:toko "
				+ "AND keterangan LIKE :penanda ORDER BY id DESC LIMIT 1");
		sudah.setParameter("toko", tokoId);
		sudah.setParameter("penanda", penanda + "%");
		Object idAda = sudah.uniqueResult();
		if (idAda != null) return (StokOpname) s.get(StokOpname.class, ((Number) idAda).longValue());

		double stokSekarang = hitungStokSistem(s, asal.getProduk().getId());
		StokOpname balik = new StokOpname();
		balik.setToko(asal.getToko());
		balik.setProduk(asal.getProduk());
		balik.setStokSistem(Double.valueOf(stokSekarang));
		balik.setStokFisik(Double.valueOf(stokSekarang - asal.getSelisih()));
		balik.setSelisih(Double.valueOf(-asal.getSelisih()));
		balik.setWaktuOpname(new Date());
		balik.setKeterangan(penanda + " " + (alasan == null ? "" : alasan.trim()));
		balik.setOleh(oleh);
		Common.refreshSaveOrUpdate(s, balik);
		StokKantinUtil.recomputeStokProdukNative(s, asal.getProduk().getId());
		return balik;
	}

	/**
	 * Ringkasan hasil opname <b>hari ini</b> untuk sebuah toko — bahan panel dasbor di JSP &amp; ZK.
	 *
	 * @param session sesi Hibernate; bila {@code null} dipakai {@code currentSession()}.
	 * @param tokoId  id toko/kios (wajib); bila {@code null} dikembalikan ringkasan kosong.
	 * @return objek {@link RingkasanOpname} berisi jumlah catatan, jumlah produk berbeda, total
	 *         kelebihan, total kekurangan, dan selisih bersih; tidak pernah {@code null}.
	 */
	public static RingkasanOpname ringkasanHariIni(Session session, Long tokoId) {
		RingkasanOpname r = new RingkasanOpname();
		if (tokoId == null) {
			return r;
		}
		Session s = session == null ? HibernateUtil.currentSession() : session;
		String sql = "SELECT COUNT(*) AS jml, COUNT(DISTINCT produk) AS prd, "
				+ "COALESCE(SUM(CASE WHEN selisih > 0 THEN selisih ELSE 0 END),0) AS lebih, "
				+ "COALESCE(SUM(CASE WHEN selisih < 0 THEN -selisih ELSE 0 END),0) AS kurang "
				+ "FROM koperasi.stok_opname WHERE toko = :toko AND CAST(waktuopname AS date) = CURRENT_DATE";
		try {
			SQLQuery q = s.createSQLQuery(sql);
			q.setParameter("toko", tokoId);
			Object row = q.uniqueResult();
			if (row instanceof Object[]) {
				Object[] c = (Object[]) row;
				r.jumlahCatatan = (long) toDouble(c[0]);
				r.jumlahProduk = (long) toDouble(c[1]);
				r.totalLebih = toDouble(c[2]);
				r.totalKurang = toDouble(c[3]);
				r.selisihBersih = r.totalLebih - r.totalKurang;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/inventory/StokOpnameScanUtil.java:225");
			// kembalikan ringkasan kosong bila gagal baca
		}
		return r;
	}

	/**
	 * Daftar catatan Stok Opname <b>hari ini</b> untuk sebuah toko, terbaru dulu — pelengkap
	 * {@link #ringkasanHariIni(Session, Long)} (yang cuma memberi angka ringkas) agar layar Stok
	 * Opname (JSP/ZK/Desktop/Android) bisa menampilkan RINCIAN baris per baris juga, bukan cuma total.
	 *
	 * <p>
	 * SEBELUMNYA layar Desktop/Android hanya menyimpan "riwayat" di memori sesi layar itu sendiri
	 * (baris yang baru saja discan pengguna, hilang begitu layar dimuat ulang) — kartu ringkasan
	 * (dari {@link #ringkasanHariIni}) memang tetap benar (baca dari server), tapi daftar rincian di
	 * bawahnya tampak kosong padahal catatannya ada di database, membingungkan pengguna. Method ini
	 * membaca LANGSUNG dari {@code koperasi.stok_opname} (bukan dari memori sesi mana pun) sehingga
	 * daftar tetap terisi walau layar baru dibuka/dimuat ulang, atau dibuka dari perangkat lain.
	 * </p>
	 *
	 * @param session sesi Hibernate; bila {@code null} dipakai {@code currentSession()}.
	 * @param tokoId  id toko/kios (wajib); bila {@code null} dikembalikan daftar kosong.
	 * @param limit   maksimum baris dikembalikan (dipaksa ke rentang 1..200; dipakai {@code 50} bila
	 *                di luar rentang wajar atau {@code <= 0}).
	 * @return daftar {@link CatatanOpname}, terbaru dulu; tidak pernah {@code null} (kosong bila gagal
	 *         baca atau memang belum ada catatan).
	 */
	public static List<CatatanOpname> daftarHariIni(Session session, Long tokoId, int limit) {
		List<CatatanOpname> hasil = new ArrayList<CatatanOpname>();
		if (tokoId == null) {
			return hasil;
		}
		int batas = (limit <= 0 || limit > 200) ? 50 : limit;
		Session s = session == null ? HibernateUtil.currentSession() : session;
		String sql = "SELECT so.id, so.waktuopname, p.kode, p.nama, so.stoksistem, so.stokfisik, so.selisih, "
				+ "so.keterangan, so.oleh, so.produk, COALESCE(p.stok,0), "
				+ "EXISTS(SELECT 1 FROM koperasi.stok_opname b WHERE b.toko=so.toko AND b.keterangan LIKE ('[BATAL_SO:' || so.id || ']%')) "
				+ "FROM koperasi.stok_opname so JOIN koperasi.produk p ON p.id = so.produk "
				+ "WHERE so.toko = :toko AND CAST(so.waktuopname AS date) = CURRENT_DATE "
				+ "ORDER BY so.waktuopname DESC LIMIT :batas";
		try {
			SQLQuery q = s.createSQLQuery(sql);
			q.setParameter("toko", tokoId);
			q.setParameter("batas", batas);
			@SuppressWarnings("unchecked")
			List<Object[]> baris = q.list();
			for (Object[] c : baris) {
				CatatanOpname k = new CatatanOpname();
				k.id = ((Number) c[0]).longValue();
				k.waktuOpname = (Date) c[1];
				k.kodeProduk = c[2] == null ? "" : c[2].toString();
				k.namaProduk = c[3] == null ? "" : c[3].toString();
				k.stokSistem = toDouble(c[4]);
				k.stokFisik = toDouble(c[5]);
				k.selisih = toDouble(c[6]);
				k.keterangan = c[7] == null ? "" : c[7].toString();
				k.oleh = c[8] == null ? "" : c[8].toString();
				k.produkId = c[9] == null ? null : ((Number) c[9]).longValue();
				k.stokAkhir = toDouble(c[10]);
				k.dibatalkan = Boolean.TRUE.equals(c[11]);
				k.jurnalPembatalan = k.keterangan.startsWith("[BATAL_SO:");
				hasil.add(k);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/inventory/StokOpnameScanUtil.java:daftarHariIni");
			// kembalikan daftar kosong bila gagal baca
		}
		return hasil;
	}

	/** Satu baris catatan Stok Opname, hasil {@link #daftarHariIni(Session, Long, int)}. */
	public static final class CatatanOpname {
		public long id;
		public Date waktuOpname;
		public String kodeProduk;
		public String namaProduk;
		public double stokSistem;
		public double stokFisik;
		public double selisih;
		public String keterangan;
		public String oleh;
		public Long produkId;
		public double stokAkhir;
		public boolean dibatalkan;
		public boolean jurnalPembatalan;
	}

	/** Konversi aman nilai numerik basis data ({@link BigDecimal}/{@link Number}/teks) ke double. */
	private static double toDouble(Object o) {
		if (o == null) {
			return 0d;
		}
		if (o instanceof BigDecimal) {
			return ((BigDecimal) o).doubleValue();
		}
		if (o instanceof Number) {
			return ((Number) o).doubleValue();
		}
		try {
			return Double.parseDouble(o.toString());
		} catch (Exception e) {
			return 0d;
		}
	}

	/**
	 * Wadah ringkasan opname harian (nilai sederhana, siap ditampilkan di panel dasbor).
	 * Dibuat sebagai kelas statis ringan agar mudah dipakai ulang lintas tampilan.
	 */
	public static final class RingkasanOpname {
		/** Banyaknya baris/catatan opname hari ini. */
		public long jumlahCatatan;
		/** Banyaknya produk berbeda yang diopname hari ini. */
		public long jumlahProduk;
		/** Total kelebihan barang (Σ selisih positif). */
		public double totalLebih;
		/** Total kekurangan barang (Σ |selisih negatif|). */
		public double totalKurang;
		/** Selisih bersih = totalLebih − totalKurang. */
		public double selisihBersih;
	}
}
