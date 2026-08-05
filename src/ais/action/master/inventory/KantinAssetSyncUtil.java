package ais.action.master.inventory;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.master.library.util.LibraryUtil;
import ais.common.Common;
import ais.database.model.Konfigurasi;
import ais.database.model.asset.DetailTransaksiAsset;
import ais.database.model.asset.MasterAsset;
import ais.database.model.asset.PenerimaanPengadaanMasterAsset;
import ais.database.model.asset.PenerimaanPengadaanMasterAssetDetail;
import ais.database.model.asset.PenyediaAsset;
import ais.database.model.inventory.PengadaanProduk;
import ais.database.model.inventory.Produk;
import ais.database.model.koperasi.PembelianAnggotaKoperasi;
import ais.database.model.library.KodeTransaksi;
import ais.ui.util.WaktuUtil;

/**
 * <h3>Jembatan FASE 1: Penerimaan BAST (modul Aset) &rarr; stok masuk kantin</h3>
 *
 * <p>Saat sebuah baris penerimaan barang (BAST, {@link PenerimaanPengadaanMasterAssetDetail})
 * difinalisasi/di-posting menjadi stok aset, dan {@code MasterAsset}-nya tertaut ke sebuah
 * {@link Produk} kantin ({@code Produk.masterAsset}), maka stok kantin diisi otomatis dengan
 * membuat baris {@code koperasi.pengadaan_produk} (barang masuk) sejumlah yang diterima, lalu
 * stok produk dihitung ulang.</p>
 *
 * <p><b>Idempoten:</b> tiap baris BAST ditandai pada {@code keterangan} pengadaan
 * ({@link #MARKER_PREFIX}+id detail). Sebelum membuat, baris auto lama untuk detail itu dihapus —
 * jadi memposting ulang BAST tidak menggandakan stok.</p>
 *
 * <p><b>Aman/konservatif (Fase 1):</b> hanya menangani tautan TIDAK ambigu — bila sebuah
 * {@code MasterAsset} tertaut ke lebih dari satu Produk (mis. dijual di banyak toko), sinkronisasi
 * dilewati (perlu keputusan distribusi manual). Seluruhnya fail-safe: kegagalan di sini TIDAK boleh
 * menggagalkan proses penerimaan/BAST.</p>
 *
 * <p><b>PENTING — kepemilikan transaksi BERBEDA antara kedua method publik di kelas ini, sengaja:</b></p>
 * <ul>
 *   <li>{@link #konsumsiPenjualanKeAset} membuka &amp; meng-commit TRANSAKSINYA SENDIRI (pola
 *       "beberapa transaksi kecil terpisah" yang sama dipakai {@code KantinHelper.bayar()} dan
 *       {@link BahanBakuUtil#konsumsiBahanBaku} — lihat catatan invariant di sana) — cocok karena
 *       kegagalannya harus BISA di-rollback sendiri tanpa membatalkan baris penjualan yang sudah
 *       committed sebelumnya (fail-safe: sinkron aset gagal &ne; transaksi kasir gagal).</li>
 *   <li>{@link #syncPengadaanDariBast} SEBALIKNYA tidak membuka transaksi sendiri — ia numpang pada
 *       transaksi milik alur posting BAST yang sedang berjalan di {@code session} yang sama, supaya
 *       "stok masuk kantin otomatis" ini SATU KESATUAN commit-atau-rollback dengan posting BAST itu
 *       sendiri: bila posting BAST gagal/dibatalkan, baris pengadaan otomatis ini ikut batal (tidak
 *       ada baris stok yatim yang sudah ter-commit dari BAST yang sebenarnya gagal).</li>
 * </ul>
 * <p>Jangan menyamakan pola keduanya tanpa memahami perbedaan ini — mengubah salah satu method agar
 * "konsisten" dengan yang lain akan mengubah semantik fail-safe-nya.</p>
 */
public final class KantinAssetSyncUtil {

	/** Penanda pada {@code pengadaan_produk.keterangan} = kunci idempoten per baris BAST. */
	public static final String MARKER_PREFIX = "AUTO-BAST#";

	/** Penanda pada {@code detail_transaksi_asset.keterangan} = kunci idempoten per penjualan kantin. */
	public static final String MARKER_JUAL_PREFIX = "AUTO-JUAL-KANTIN#";

	/** Konfigurasi gerbang Fase 2 (default mati): aktifkan sinkron penjualan kantin → stok keluar aset. */
	public static final String KONFIG_SINKRON_JUAL = "aktifkan_sinkron_jual_kantin_ke_aset";

	private KantinAssetSyncUtil() {
	}

	/**
	 * <h3>FASE 2: penjualan kantin → stok KELUAR aset (sumber tunggal stok)</h3>
	 *
	 * <p>Untuk tiap item terjual yang produknya tertaut ke {@code MasterAsset}, dibuat baris
	 * {@code asset.detail_transaksi_asset} bertanda keluar ({@link LibraryUtil#PEMAKAIAN}, kode "PEM")
	 * sebanyak yang terjual — sehingga stok persediaan aset ikut berkurang dan tetap sinkron dengan
	 * penjualan kantin. Idempoten per bill (hapus-lalu-buat berdasarkan {@link #MARKER_JUAL_PREFIX}),
	 * gagal-aman (tidak menggagalkan transaksi penjualan).</p>
	 *
	 * <p><b>Gerbang konfigurasi:</b> hanya aktif bila {@code aktifkan_sinkron_jual_kantin_ke_aset = aktif}
	 * (default mati → tidak ada efek). Catatan: ini hanya menyinkronkan STOK; pencatatan jurnal
	 * (HPP/persediaan) memerlukan keputusan akuntansi tersendiri dan TIDAK diposting otomatis di sini.</p>
	 *
	 * @param session   session aktif alur penjualan ({@code KantinHelper.bayar}).
	 * @param transaksi array item terjual ({id, jumlah, ...}).
	 * @param bill      header penjualan (acuan idempoten).
	 * @param waktu     waktu transaksi.
	 */
	public static void konsumsiPenjualanKeAset(Session session, JSONArray transaksi, PembelianAnggotaKoperasi bill,
			Date waktu) {
		try {
			if (session == null || bill == null || bill.getId() == null || transaksi == null) {
				return;
			}
			// Gerbang konfigurasi (default mati).
			try {
				String mode = Common.getKonfigurasi(KONFIG_SINKRON_JUAL, Konfigurasi.TIDAK_AKTIF).getNilai();
				if (mode == null || !mode.equalsIgnoreCase(Konfigurasi.AKTIF)) {
					return;
				}
			} catch (Exception e) {
				return;
			}
			if (LibraryUtil.PEMAKAIAN == null || LibraryUtil.PEMAKAIAN.getId() == null) {
				return;
			}
			// Ambil ULANG lewat session.get(id) alih-alih memakai LibraryUtil.PEMAKAIAN langsung: field
			// statis itu bisa saja dimuat di session/thread LAIN sebelumnya (mis. saat startup), dan
			// menempelkan entity dari session asing ke DetailTransaksiAsset baru di bawah bisa memicu
			// error lintas-session Hibernate. Mengambil ulang by-id di session AKTIF ini menjamin
			// entity yang dipakai selalu terikat ke session yang benar.
			KodeTransaksi pemakaian = (KodeTransaksi) session.get(KodeTransaksi.class, LibraryUtil.PEMAKAIAN.getId());
			if (pemakaian == null) {
				return;
			}

			// Akumulasi qty terjual per master asset (hanya produk yang tertaut).
			Map<Long, Double> akumulasi = new HashMap<Long, Double>();
			Map<Long, MasterAsset> masterRef = new HashMap<Long, MasterAsset>();
			for (int i = 0; i < transaksi.length(); i++) {
				try {
					JSONObject t = transaksi.getJSONObject(i);
					if (t.isNull("id")) {
						continue;
					}
					Long produkId = Long.valueOf(Long.parseLong((t.get("id") + "").trim()));
					double jual = t.isNull("jumlah") ? 0 : Double.parseDouble((t.get("jumlah") + "").trim());
					if (jual <= 0) {
						continue;
					}
					Produk p = (Produk) session.get(Produk.class, produkId);
					if (p == null || p.getMasterAsset() == null || p.getMasterAsset().getId() == null) {
						continue;
					}
					Long maId = p.getMasterAsset().getId();
					Double prev = akumulasi.get(maId);
					akumulasi.put(maId, Double.valueOf((prev == null ? 0.0 : prev.doubleValue()) + jual));
					if (!masterRef.containsKey(maId)) {
						masterRef.put(maId, p.getMasterAsset());
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/inventory/KantinAssetSyncUtil.java:125");
				}
			}

			String marker = MARKER_JUAL_PREFIX + bill.getId();
			session.getTransaction().begin();
			// Idempoten: hapus stok-keluar auto sebelumnya untuk bill ini.
			session.createSQLQuery("DELETE FROM asset.detail_transaksi_asset WHERE keterangan = :k")
					.setString("k", marker).executeUpdate();
			for (Map.Entry<Long, Double> en : akumulasi.entrySet()) {
				double qty = en.getValue().doubleValue();
				if (qty <= 0) {
					continue;
				}
				DetailTransaksiAsset d = new DetailTransaksiAsset();
				d.setMasterAsset(masterRef.get(en.getKey()));
				d.setKodeTransaksi(pemakaian);
				d.setQty(Double.valueOf(qty));
				d.setQtyBonus(Double.valueOf(0.0));
				d.setKeterangan(marker);
				d.setTanggal(waktu == null ? WaktuUtil.getDate() : waktu);
				session.save(d);
			}
			session.getTransaction().commit();
		} catch (Exception e) {
			try {
				session.getTransaction().rollback();
			} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/inventory/KantinAssetSyncUtil.java:152");
			}
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/inventory/KantinAssetSyncUtil.java:154");
		}
	}

	/**
	 * Sinkronkan satu baris penerimaan BAST menjadi stok masuk kantin (bila tertaut ke Produk).
	 *
	 * @param session session aktif milik alur BAST (baris dibuat di session yang sama).
	 * @param detail  baris penerimaan pengadaan (BAST) yang baru difinalisasi.
	 */
	@SuppressWarnings("unchecked")
	public static void syncPengadaanDariBast(Session session, PenerimaanPengadaanMasterAssetDetail detail) {
		try {
			if (session == null || detail == null || detail.getMasterAsset() == null
					|| detail.getMasterAsset().getId() == null) {
				return;
			}

			// Cari produk kantin yang tertaut ke master asset ini.
			List<Produk> produks = session.createCriteria(Produk.class)
					.add(Restrictions.eq("masterAsset", detail.getMasterAsset())).list();
			if (produks == null || produks.size() != 1) {
				return; // tidak tertaut / ambigu -> lewati (Fase 1)
			}
			Produk produk = produks.get(0);
			if (produk.getToko() == null) {
				return; // pengadaan_produk wajib memiliki toko
			}

			String marker = MARKER_PREFIX + detail.getId();

			// Idempoten: hapus baris auto sebelumnya untuk baris BAST ini (anti dobel saat re-posting).
			session.createSQLQuery("DELETE FROM koperasi.pengadaan_produk WHERE keterangan = :k")
					.setString("k", marker).executeUpdate();

			double diterima = detail.getDiterima() == null ? 0 : detail.getDiterima().doubleValue();
			if (diterima > 0) {
				double harga = detail.getHargaBeli() == null ? 0 : detail.getHargaBeli().doubleValue();
				PenerimaanPengadaanMasterAsset header = detail.getPenerimaanPengadaanMasterAsset();
				Date waktu = (header != null && header.getTanggalPembuatan() != null) ? header.getTanggalPembuatan()
						: WaktuUtil.getDate();
				String namaSupplier = "";
				try {
					PenyediaAsset py = header == null ? null : header.getPenyedia();
					if (py != null && py.getNama() != null) {
						namaSupplier = py.getNama();
					}
				} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/inventory/KantinAssetSyncUtil.java:201");
				}
				String nomorFaktur = (header != null && header.getKode() != null) ? header.getKode()
						: (detail.getKodeUnik() == null ? "BAST" : detail.getKodeUnik());

				PengadaanProduk pp = new PengadaanProduk();
				pp.setProduk(produk);
				pp.setToko(produk.getToko());
				pp.setQty(Double.valueOf(diterima));
				pp.setHargaBeliSatuan(Double.valueOf(harga));
				pp.setTotalHarga(Double.valueOf(diterima * harga));
				pp.setWaktuPengadaan(waktu);
				pp.setNamaSupplier(namaSupplier);
				pp.setNomorFaktur(nomorFaktur);
				pp.setKeterangan(marker);
				session.save(pp);
				session.flush();
			}

			// Hitung ulang stok kantin (mencakup pengadaan baru ini), aman dalam transaksi BAST.
			StokKantinUtil.recomputeStokProdukNative(session, produk.getId());
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/inventory/KantinAssetSyncUtil.java:223");
		}
	}
}
