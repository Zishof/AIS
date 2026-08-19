package ais.action.master.inventory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.database.model.inventory.Produk;
import ais.database.model.inventory.Toko;
import ais.database.model.koperasi.PembelianAnggotaKoperasi;

/**
 * Util bersama untuk resep/bahan baku (Bill of Materials) produk kantin.
 *
 * <p>Saat produk ber-resep terjual, stok bahan bakunya ikut berkurang otomatis (praktik baku POS
 * manufaktur). Karena stok di sistem ini DITURUNKAN dari transaksi
 * (<code>stok = &Sigma;pengadaan.qty + &Sigma;opname.selisih &minus; &Sigma;pembelian.qty &minus; &Sigma;pemakaian.qty</code>,
 * lihat {@link StokKantinUtil}), pemakaian bahan baku dicatat ke <b>buku besar khusus</b>
 * {@code koperasi.pemakaian_bahan_baku} — BUKAN ke {@code pembelian} — supaya:</p>
 * <ul>
 *   <li>tidak mencemari laporan penjualan/omzet maupun kueri operasional (mis. hitungan pesanan
 *       belum dilayani berbasis {@code terlayani});</li>
 *   <li>idempoten: baris pemakaian untuk satu bill dihapus dulu sebelum dibuat ulang, jadi
 *       memproses ulang sebuah pesanan tidak menggandakan pemotongan stok;</li>
 *   <li>punya jejak audit pemakaian bahan baku yang bisa dijadikan laporan tersendiri.</li>
 * </ul>
 *
 * <p>Semua dibungkus fail-safe: kegagalan di sini TIDAK boleh menggagalkan transaksi penjualan.</p>
 */
public final class BahanBakuUtil {

	/**
	 * {@link SimpleDateFormat} TIDAK thread-safe (state internal kalender dibagi antar-panggilan) --
	 * karena itu dibungkus {@link ThreadLocal} sehingga tiap thread kasir memakai instans sendiri
	 * (aman dipanggil dari banyak thread sekaligus saat server memproses beberapa request checkout
	 * paralel), tanpa perlu blok {@code synchronized} maupun alokasi objek baru tiap panggilan.
	 */
	private static final ThreadLocal<SimpleDateFormat> TS = new ThreadLocal<SimpleDateFormat>() {
		@Override
		protected SimpleDateFormat initialValue() {
			return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		}
	};

	private BahanBakuUtil() {
	}

	/**
	 * Catat pemakaian bahan baku untuk tiap produk ber-resep pada {@code transaksi} ke buku besar
	 * {@code koperasi.pemakaian_bahan_baku} (hapus dulu milik bill ini lalu sisipkan ulang).
	 *
	 * @param session session aktif tempat bill disimpan.
	 * @param transaksi array item terjual ({id, jumlah, ...}) — sama dengan payload kasir.
	 * @param toko     toko transaksi.
	 * @param waktu    waktu transaksi.
	 * @param bill     header pembelian (bill) acuan idempoten.
	 * @return himpunan id produk bahan baku yang stoknya perlu dihitung ulang (boleh kosong).
	 */
	public static Set<Long> konsumsiBahanBaku(Session session, JSONArray transaksi, Toko toko, Date waktu,
			PembelianAnggotaKoperasi bill) {
		Set<Long> touched = new HashSet<Long>();
		if (session == null || bill == null || bill.getId() == null) {
			return touched;
		}

		// Akumulasi pemakaian per bahan baku: id -> qty total.
		Map<Long, Double> akumulasi = new HashMap<Long, Double>();
		if (transaksi != null) {
			for (int i = 0; i < transaksi.length(); i++) {
				try {
					JSONObject t = transaksi.getJSONObject(i);
					if (t.isNull("id")) {
						continue;
					}
					Long produkId = Long.valueOf(Long.parseLong((t.get("id") + "").trim()));
					double jual = t.isNull("jumlah") ? 1.0 : Double.parseDouble((t.get("jumlah") + "").trim());
					if (jual <= 0) {
						continue;
					}
					Produk p = (Produk) session.get(Produk.class, produkId);
					if (p == null || p.getBahanBaku() == null || p.getBahanBaku().trim().isEmpty()) {
						continue;
					}
					JSONArray bahan = new JSONArray(p.getBahanBaku().trim());
					for (int j = 0; j < bahan.length(); j++) {
						JSONObject b = bahan.getJSONObject(j);
						long bid = b.optLong("produk", 0);
						double qtyResep = b.optDouble("qty", 0);
						if (bid <= 0 || qtyResep <= 0) {
							continue;
						}
						Long key = Long.valueOf(bid);
						Double prev = akumulasi.get(key);
						akumulasi.put(key, Double.valueOf((prev == null ? 0.0 : prev.doubleValue()) + qtyResep * jual));
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/inventory/BahanBakuUtil.java:92");
				}
			}
		}

		long billId = bill.getId().longValue();
		String waktuStr = TS.get().format(waktu == null ? new Date() : waktu);
		String tokoSql = (toko == null || toko.getId() == null) ? "null" : String.valueOf(toko.getId());

		try {
			// Aman memulai transaksi baru di sini karena pemanggil (KantinHelper.bayar(), lewat
			// PembelianAnggotaKoperasi.simpanRinci() tepat sebelum ini) SELALU meng-commit
			// transaksinya sendiri sebelum memanggil method ini -- pola "beberapa transaksi kecil
			// terpisah per langkah" yang dipakai konsisten di seluruh alur checkout (lihat JavaDoc
			// KantinHelper.bayar()). Bila pemanggil di masa depan berubah jadi satu transaksi atomik
			// tanpa commit di antara langkah, begin() di baris ini akan melempar
			// IllegalStateException("Transaction already active") -- sinyal jelas bahwa asumsi ini
			// perlu ditinjau ulang, bukan gagal senyap.
			session.getTransaction().begin();
			// Idempoten: bersihkan pemakaian lama bill ini sebelum menyisipkan ulang.
			session.createSQLQuery(
					"DELETE FROM koperasi.pemakaian_bahan_baku WHERE pembelian_anggota_koperasi = " + billId)
					.executeUpdate();
			for (Map.Entry<Long, Double> en : akumulasi.entrySet()) {
				long bid = en.getKey().longValue();
				double qty = en.getValue().doubleValue();
				session.createSQLQuery("INSERT INTO koperasi.pemakaian_bahan_baku "
						+ "(produk, qty, toko, waktu, pembelian_anggota_koperasi) VALUES (" + bid + ", " + qty + ", "
						+ tokoSql + ", '" + waktuStr + "', " + billId + ")").executeUpdate();
				touched.add(Long.valueOf(bid));
			}
			session.getTransaction().commit();
		} catch (Exception e) {
			try {
				session.getTransaction().rollback();
			} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/inventory/BahanBakuUtil.java:122");
			}
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/inventory/BahanBakuUtil.java:124");
			return new HashSet<Long>();
		}
		return touched;
	}
}
