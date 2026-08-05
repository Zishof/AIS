package ais.action.master.inventory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.SQLQuery;
import org.hibernate.Session;

import ais.common.Common;
import ais.database.model.asset.Lokasi;
import ais.database.model.asset.MutasiLokasi;
import ais.database.model.inventory.Produk;
import ais.ui.util.WaktuUtil;

/**
 * <h2>StokLokasiUtil — mesin stok &amp; mutasi barang <b>per lokasi/gudang</b> untuk e-Kantin.</h2>
 *
 * <p>
 * Kelas util ini adalah <b>satu pintu</b> untuk mencatat pergerakan stok pada setiap {@link Lokasi}
 * (gudang, outlet, dsb) dan menghitung stok berjalan berdasarkan pendekatan <i>buku besar</i>
 * ({@link MutasiLokasi}). Tujuan utamanya: <b>pemakaian ulang maksimal</b> — halaman input mutasi
 * (JSP maupun ZKoss), proses transfer antar-gudang, serta seluruh <b>laporan &amp; dasbor pergudangan
 * </b> (Laporan Outlet &amp; Laporan Gudang) memanggil metode yang sama di sini. Dengan memusatkan
 * aturan tanda ({@code +}/{@code -}), pembentukan pasangan transfer, dan agregasi stok, logika stok
 * tidak tersebar dan mudah dipelihara.
 * </p>
 *
 * <h3>Model data &amp; konvensi tanda</h3>
 * <p>
 * Stok tidak disimpan sebagai satu angka melainkan diturunkan dari penjumlahan seluruh baris
 * {@link MutasiLokasi}. Setiap baris menyimpan {@code qty} <b>bertanda</b>: barang masuk positif,
 * keluar negatif, sehingga stok sebuah (lokasi, produk) = {@code SUM(qty)}. Pendekatan ini akurat,
 * dapat diaudit (riwayat lengkap "mengapa stok segini"), dan menghindari kolom cache yang rawan
 * melenceng. Metode {@code catat...} di kelas ini bertanggung jawab memasang tanda yang benar
 * sehingga pemanggil cukup memberi jumlah positif beserta jenisnya.
 * </p>
 *
 * <h3>Transfer antar lokasi</h3>
 * <p>
 * {@link #catatTransfer} mencatat perpindahan sebagai <b>dua baris atomik</b>: satu KELUAR (negatif)
 * di lokasi asal dan satu MASUK (positif) di lokasi tujuan, keduanya berbagi kode
 * {@code referensi} yang sama dan saling menunjuk lewat {@code lokasiPasangan}. Dengan begitu stok
 * tiap lokasi tetap sekadar {@code SUM(qty)} baris miliknya sendiri — agregasi laporan menjadi
 * sederhana dan efisien (tanpa CASE bercabang).
 * </p>
 *
 * <h3>Kontrak sesi &amp; kinerja</h3>
 * <p>
 * Semua metode <b>menerima {@link Session} dari pemanggil dan tidak menutupnya</b>. Ini menjaga
 * aturan framework: {@code currentSession()} tak boleh ditutup manual; bila pemanggil memakai
 * {@code openSession()}/{@code currentNativeSession()} ialah pemanggil yang menutup di
 * {@code finally}. Penulisan memakai
 * {@link Common#refreshSaveOrUpdate(Session, ais.database.model.GeneralValueObject)} agar konsisten
 * dengan modul lain. Perhitungan stok dan rekap laporan memakai agregasi SQL langsung
 * ({@code SUM(qty)}) yang ringan; pemanggil dianjurkan memanggil {@link #rekapStokLokasi} sekali
 * untuk mengisi tabel sekaligus dasar grafik, bukan query berulang dalam perulangan. Semua kode
 * hemat objek, mengembalikan {@link List} kosong (bukan {@code null}), kompatibel <b>Java 1.7</b>
 * (tanpa lambda), dan aman thread selama tiap pemanggil memakai sesinya sendiri.
 * </p>
 *
 * <h3>Ringkasan metode</h3>
 * <ul>
 *   <li>{@link #catatMasuk} / {@link #catatKeluar} — barang masuk/keluar satu lokasi.</li>
 *   <li>{@link #catatTransfer} — pindah barang antar lokasi (buat pasangan otomatis).</li>
 *   <li>{@link #catatPenyesuaian} — koreksi hasil opname (selisih bertanda).</li>
 *   <li>{@link #qtyStok} — stok berjalan satu (lokasi, produk).</li>
 *   <li>{@link #rekapStokLokasi} — rekap stok+nilai untuk laporan/dasbor (filter jenis/lokasi/cari).</li>
 * </ul>
 *
 * @author AIS e-Kantin (modul pergudangan)
 * @see MutasiLokasi
 * @see ais.action.master.koperasi.helper.LokasiKantinUtil
 */
public final class StokLokasiUtil {

	private StokLokasiUtil() {
		// util murni.
	}

	/**
	 * Mencatat satu baris mutasi (dipakai internal &amp; boleh dipakai ulang oleh pemanggil lanjutan).
	 *
	 * @param session       sesi Hibernate milik pemanggil (tidak ditutup di sini).
	 * @param lokasiId      id lokasi (wajib &gt; 0).
	 * @param produkId      id produk (wajib &gt; 0).
	 * @param jenis         label jenis (lihat konstanta {@link MutasiLokasi}).
	 * @param qtyBertanda   jumlah <b>sudah bertanda</b> (+/-).
	 * @param hargaSatuan   harga satuan saat pergerakan (boleh null → 0).
	 * @param lokasiPasanganId lokasi lawan untuk transfer (boleh null).
	 * @param tanggal       tanggal pergerakan (boleh null → sekarang).
	 * @param keterangan    catatan bebas.
	 * @param referensi     kode penghubung/dokumen sumber.
	 * @param oleh          nama pengguna (audit).
	 * @param olehId        id pengguna (audit).
	 * @return baris {@link MutasiLokasi} tersimpan.
	 */
	public static MutasiLokasi catat(Session session, Long lokasiId, Long produkId, String jenis, double qtyBertanda,
			Double hargaSatuan, Long lokasiPasanganId, Date tanggal, String keterangan, String referensi, String oleh,
			String olehId) {
		if (lokasiId == null || lokasiId.longValue() <= 0) {
			throw new IllegalArgumentException("Lokasi wajib dipilih.");
		}
		if (produkId == null || produkId.longValue() <= 0) {
			throw new IllegalArgumentException("Produk wajib dipilih.");
		}
		MutasiLokasi m = new MutasiLokasi();
		m.setLokasi((Lokasi) session.get(Lokasi.class, lokasiId));
		m.setProduk((Produk) session.get(Produk.class, produkId));
		if (lokasiPasanganId != null && lokasiPasanganId.longValue() > 0) {
			m.setLokasiPasangan((Lokasi) session.get(Lokasi.class, lokasiPasanganId));
		}
		m.setJenis(jenis);
		m.setQty(Double.valueOf(qtyBertanda));
		m.setHargaSatuan(hargaSatuan == null ? Double.valueOf(0d) : hargaSatuan);
		m.setTanggal(tanggal == null ? WaktuUtil.getDate() : tanggal);
		m.setKeterangan(keterangan);
		m.setReferensi(referensi);
		m.setOleh(oleh);
		m.setOlehId(olehId);
		Common.refreshSaveOrUpdate(session, m);
		return m;
	}

	/**
	 * Mencatat barang <b>masuk</b> (qty dipaksa positif).
	 *
	 * @param qty jumlah positif (nilai negatif akan diambil nilai mutlaknya).
	 * @return baris mutasi tersimpan.
	 * @see #catat
	 */
	public static MutasiLokasi catatMasuk(Session session, Long lokasiId, Long produkId, double qty, Double hargaSatuan,
			Date tanggal, String keterangan, String referensi, String oleh, String olehId) {
		return catat(session, lokasiId, produkId, MutasiLokasi.MASUK, Math.abs(qty), hargaSatuan, null, tanggal,
				keterangan, referensi, oleh, olehId);
	}

	/**
	 * Mencatat barang <b>keluar</b> (qty dipaksa negatif).
	 *
	 * @param qty jumlah positif yang akan disimpan sebagai negatif.
	 * @return baris mutasi tersimpan.
	 * @see #catat
	 */
	public static MutasiLokasi catatKeluar(Session session, Long lokasiId, Long produkId, double qty, Double hargaSatuan,
			Date tanggal, String keterangan, String referensi, String oleh, String olehId) {
		return catat(session, lokasiId, produkId, MutasiLokasi.KELUAR, -Math.abs(qty), hargaSatuan, null, tanggal,
				keterangan, referensi, oleh, olehId);
	}

	/**
	 * Mencatat <b>transfer</b> antar lokasi sebagai dua baris atomik (KELUAR di asal, MASUK di tujuan)
	 * yang berbagi satu kode referensi.
	 *
	 * @param session        sesi Hibernate milik pemanggil (tidak ditutup di sini).
	 * @param lokasiAsalId   lokasi asal.
	 * @param lokasiTujuanId lokasi tujuan.
	 * @param produkId       produk yang dipindah.
	 * @param qty            jumlah positif yang dipindah.
	 * @param hargaSatuan    harga satuan (boleh null).
	 * @param tanggal        tanggal (boleh null → sekarang).
	 * @param keterangan     catatan.
	 * @param oleh           nama pengguna (audit).
	 * @param olehId         id pengguna (audit).
	 * @throws IllegalArgumentException bila asal &amp; tujuan sama atau jumlah &le; 0.
	 */
	public static void catatTransfer(Session session, Long lokasiAsalId, Long lokasiTujuanId, Long produkId, double qty,
			Double hargaSatuan, Date tanggal, String keterangan, String oleh, String olehId) {
		if (lokasiAsalId == null || lokasiTujuanId == null || lokasiAsalId.equals(lokasiTujuanId)) {
			throw new IllegalArgumentException("Lokasi asal dan tujuan harus berbeda.");
		}
		if (qty <= 0) {
			throw new IllegalArgumentException("Jumlah transfer harus lebih dari 0.");
		}
		Date t = tanggal == null ? WaktuUtil.getDate() : tanggal;
		String ref = "TRF-" + Common.getGeneratedBarCode(8);
		double abs = Math.abs(qty);
		// KELUAR dari asal (menunjuk tujuan sebagai pasangan)
		catat(session, lokasiAsalId, produkId, MutasiLokasi.TRANSFER, -abs, hargaSatuan, lokasiTujuanId, t, keterangan,
				ref, oleh, olehId);
		// MASUK ke tujuan (menunjuk asal sebagai pasangan)
		catat(session, lokasiTujuanId, produkId, MutasiLokasi.TRANSFER, abs, hargaSatuan, lokasiAsalId, t, keterangan,
				ref, oleh, olehId);
	}

	/**
	 * Mencatat <b>penyesuaian/opname</b> berupa selisih bertanda agar stok tercatat menyamai fisik.
	 *
	 * @param selisih selisih (fisik − sistem); positif menambah, negatif mengurangi.
	 * @return baris mutasi tersimpan.
	 * @see #catat
	 */
	public static MutasiLokasi catatPenyesuaian(Session session, Long lokasiId, Long produkId, double selisih,
			Double hargaSatuan, Date tanggal, String keterangan, String oleh, String olehId) {
		return catat(session, lokasiId, produkId, MutasiLokasi.PENYESUAIAN, selisih, hargaSatuan, null, tanggal,
				keterangan, null, oleh, olehId);
	}

	/**
	 * Menghitung stok berjalan satu (lokasi, produk) = {@code SUM(qty)} seluruh mutasinya.
	 *
	 * @param session  sesi Hibernate milik pemanggil (tidak ditutup di sini).
	 * @param lokasiId id lokasi.
	 * @param produkId id produk.
	 * @return stok berjalan (0 bila belum ada pergerakan).
	 */
	public static double qtyStok(Session session, Long lokasiId, Long produkId) {
		if (session == null || lokasiId == null || produkId == null) {
			return 0d;
		}
		try {
			SQLQuery q = session.createSQLQuery(
					"select coalesce(sum(qty),0) from asset.mutasi_lokasi where lokasi=:l and produk=:p");
			q.setParameter("l", lokasiId);
			q.setParameter("p", produkId);
			Object o = q.uniqueResult();
			return o == null ? 0d : ((Number) o).doubleValue();
		} catch (Exception e) {
			return 0d;
		}
	}

	/**
	 * Rekap stok + nilai persediaan untuk laporan/dasbor, dikelompokkan per (lokasi, produk).
	 * Hanya baris berstok tidak nol yang dikembalikan.
	 *
	 * <p>
	 * Tiap elemen hasil adalah {@code Object[]} berisi:
	 * {@code [0]=lokasiId (Number), [1]=lokasiNama (String), [2]=jenisNama (String),
	 * [3]=produkId (Number), [4]=produkKode (String), [5]=produkNama (String),
	 * [6]=qty (Number), [7]=nilai (Number = qty × harga beli)}.
	 * </p>
	 *
	 * @param session       sesi Hibernate milik pemanggil (tidak ditutup di sini).
	 * @param jenisLokasiId bila &gt; 0, hanya lokasi berjenis tsb (mis. semua "Gudang" atau "Outlet").
	 * @param lokasiId      bila &gt; 0, hanya satu lokasi.
	 * @param cari          bila tidak kosong, cocokkan {@code ilike} nama/kode produk atau nama lokasi.
	 * @return daftar baris rekap (tidak pernah {@code null}).
	 */
	@SuppressWarnings("unchecked")
	public static List<Object[]> rekapStokLokasi(Session session, Long jenisLokasiId, Long lokasiId, String cari) {
		if (session == null) {
			return new ArrayList<Object[]>();
		}
		try {
			StringBuilder sb = new StringBuilder();
			sb.append("select l.id, l.nama, coalesce(jl.nama,'(tanpa jenis)'), p.id, p.kode, p.nama, ");
			sb.append("  coalesce(sum(m.qty),0) as qty, ");
			sb.append("  coalesce(sum(m.qty),0) * coalesce(p.hargabeli,0) as nilai ");
			sb.append("from asset.mutasi_lokasi m ");
			sb.append("join asset.lokasi l on l.id = m.lokasi ");
			sb.append("left join asset.jenis_lokasi jl on jl.id = l.jenis_lokasi ");
			sb.append("join koperasi.produk p on p.id = m.produk ");
			sb.append("where 1=1 ");
			if (jenisLokasiId != null && jenisLokasiId.longValue() > 0) {
				sb.append("and l.jenis_lokasi = :jenis ");
			}
			if (lokasiId != null && lokasiId.longValue() > 0) {
				sb.append("and m.lokasi = :lokasi ");
			}
			boolean adaCari = cari != null && cari.trim().length() > 0;
			if (adaCari) {
				sb.append("and (p.nama ilike :cari or p.kode ilike :cari or l.nama ilike :cari) ");
			}
			sb.append("group by l.id, l.nama, jl.nama, p.id, p.kode, p.nama ");
			sb.append("having coalesce(sum(m.qty),0) <> 0 ");
			sb.append("order by l.nama, p.nama");

			SQLQuery q = session.createSQLQuery(sb.toString());
			if (jenisLokasiId != null && jenisLokasiId.longValue() > 0) {
				q.setParameter("jenis", jenisLokasiId);
			}
			if (lokasiId != null && lokasiId.longValue() > 0) {
				q.setParameter("lokasi", lokasiId);
			}
			if (adaCari) {
				q.setParameter("cari", "%" + cari.trim() + "%");
			}
			List<Object[]> hasil = q.list();
			return hasil == null ? new ArrayList<Object[]>() : hasil;
		} catch (Exception e) {
			return new ArrayList<Object[]>();
		}
	}

	/**
	 * Rekap stok lengkap untuk dasbor laporan Outlet/Gudang, dengan <b>dua versi penilaian</b> nilai
	 * persediaan: (a) berbasis <i>harga beli acuan</i> pada master Produk, dan (b) berbasis
	 * <i>rata-rata biaya masuk</i> (weighted average dari harga_satuan mutasi MASUK per lokasi/produk).
	 * Hanya baris berstok tidak nol yang dikembalikan.
	 *
	 * <p>Tiap {@code Object[]} berisi: {@code [0]=lokasiId, [1]=lokasiNama, [2]=jenisId, [3]=jenisNama,
	 * [4]=jenisWarna, [5]=produkId, [6]=kode, [7]=nama, [8]=qty, [9]=hargaBeli, [10]=nilaiBeli,
	 * [11]=avgCost}. Nilai versi rata-rata (qty × avgCost) dihitung pemanggil agar SQL tetap ringan.</p>
	 *
	 * @param session       sesi Hibernate milik pemanggil (tidak ditutup di sini).
	 * @param jenisLokasiId bila &gt; 0, hanya lokasi berjenis tsb.
	 * @param lokasiId      bila &gt; 0, hanya satu lokasi.
	 * @param cari          bila tidak kosong, cocokkan {@code ilike} nama/kode produk atau nama lokasi.
	 * @return daftar baris rekap (tidak pernah {@code null}).
	 */
	@SuppressWarnings("unchecked")
	public static List<Object[]> rekapStokLokasiLengkap(Session session, Long jenisLokasiId, Long lokasiId, String cari) {
		if (session == null) {
			return new ArrayList<Object[]>();
		}
		try {
			StringBuilder sb = new StringBuilder();
			sb.append("select l.id, l.nama, jl.id, coalesce(jl.nama,'(tanpa jenis)'), coalesce(jl.warna,''), ");
			sb.append("  p.id, p.kode, p.nama, coalesce(sum(m.qty),0) as qty, coalesce(p.hargabeli,0) as hbeli, ");
			sb.append("  coalesce(sum(m.qty),0) * coalesce(p.hargabeli,0) as nilai_beli, ");
			sb.append("  case when coalesce(sum(case when m.qty>0 then m.qty else 0 end),0) > 0 ");
			sb.append("       then coalesce(sum(case when m.qty>0 then m.qty*coalesce(m.harga_satuan,0) else 0 end),0) ");
			sb.append("            / sum(case when m.qty>0 then m.qty else 0 end) else 0 end as avg_cost ");
			sb.append("from asset.mutasi_lokasi m ");
			sb.append("join asset.lokasi l on l.id = m.lokasi ");
			sb.append("left join asset.jenis_lokasi jl on jl.id = l.jenis_lokasi ");
			sb.append("join koperasi.produk p on p.id = m.produk where 1=1 ");
			if (jenisLokasiId != null && jenisLokasiId.longValue() > 0) {
				sb.append("and l.jenis_lokasi = :jenis ");
			}
			if (lokasiId != null && lokasiId.longValue() > 0) {
				sb.append("and m.lokasi = :lokasi ");
			}
			boolean adaCari = cari != null && cari.trim().length() > 0;
			if (adaCari) {
				sb.append("and (p.nama ilike :cari or p.kode ilike :cari or l.nama ilike :cari) ");
			}
			sb.append("group by l.id, l.nama, jl.id, jl.nama, jl.warna, p.id, p.kode, p.nama, p.hargabeli ");
			sb.append("having coalesce(sum(m.qty),0) <> 0 order by l.nama, p.nama");

			SQLQuery q = session.createSQLQuery(sb.toString());
			if (jenisLokasiId != null && jenisLokasiId.longValue() > 0) {
				q.setParameter("jenis", jenisLokasiId);
			}
			if (lokasiId != null && lokasiId.longValue() > 0) {
				q.setParameter("lokasi", lokasiId);
			}
			if (adaCari) {
				q.setParameter("cari", "%" + cari.trim() + "%");
			}
			List<Object[]> hasil = q.list();
			return hasil == null ? new ArrayList<Object[]>() : hasil;
		} catch (Exception e) {
			return new ArrayList<Object[]>();
		}
	}

	/**
	 * <h3>Fase 2: rekap NILAI stok dikelompokkan per Gudang (hierarki pusat/cabang).</h3>
	 *
	 * <p>Tambahan di samping {@link #rekapStokLokasiLengkap} yang flat per {@link Lokasi} — BUKAN
	 * pengganti. Mengelompokkan lewat {@code Lokasi.gudang} (opsional, baru bisa diisi lewat picker
	 * "Gudang" pada form Lokasi Kantin sejak Fase 2) dan menampilkan {@code Gudang.gudangInduk} sebagai
	 * kolom terpisah agar hierarki pusat/cabang tetap terlihat tanpa perlu rekursi SQL (hierarki bisnis
	 * di sini pada praktiknya dangkal — Pusat/Cabang, 2 tingkat — jadi satu join ke induk sudah cukup;
	 * tidak memakai {@code WITH RECURSIVE} demi kompatibilitas PostgreSQL lama).</p>
	 *
	 * <p>Baris {@code "(Tanpa Gudang)"} (gudangId {@code null}) menampung seluruh stok pada Lokasi yang
	 * belum ditautkan ke Gudang manapun — mayoritas data existing sebelum picker Gudang ada — supaya
	 * total rollup ini tetap bisa direkonsiliasi ke total laporan lama (tidak ada stok yang "hilang"
	 * dari rollup hanya karena belum ditandai gudangnya).</p>
	 *
	 * <p><b>Catatan penting:</b> kolom qty TIDAK dijumlah lintas produk di sini (menjumlah kuantitas
	 * barang dengan satuan berbeda-beda tidak bermakna) — hanya jumlah lokasi/produk berbeda dan nilai
	 * (Rp, aman dijumlah lintas produk karena tiap suku sudah dikalikan harga masing-masing) yang
	 * dirollup. Untuk rincian per-produk, pakai {@link #rekapStokLokasiLengkap} seperti biasa.</p>
	 *
	 * <p>Tiap {@code Object[]} berisi: {@code [0]=gudangId (Number, null bila "Tanpa Gudang"),
	 * [1]=gudangNama, [2]=gudangIndukNama (kosong bila gudang pusat/tanpa gudang), [3]=jumlahLokasi,
	 * [4]=jumlahProduk, [5]=nilaiTotal}.</p>
	 *
	 * @param session       sesi Hibernate milik pemanggil (tidak ditutup di sini).
	 * @param jenisLokasiId bila &gt; 0, hanya lokasi berjenis tsb.
	 * @param cari          bila tidak kosong, cocokkan {@code ilike} nama/kode produk.
	 * @return daftar baris rollup per gudang, terurut nama (tidak pernah {@code null}).
	 */
	@SuppressWarnings("unchecked")
	public static List<Object[]> rekapStokPerGudang(Session session, Long jenisLokasiId, String cari) {
		if (session == null) {
			return new ArrayList<Object[]>();
		}
		try {
			StringBuilder sb = new StringBuilder();
			sb.append("select g.id, coalesce(g.nama,'(Tanpa Gudang)') as gudang_nama, coalesce(gi.nama,'') as induk_nama, ");
			sb.append("  count(distinct l.id) as jumlah_lokasi, count(distinct p.id) as jumlah_produk, ");
			sb.append("  coalesce(sum(m.qty * coalesce(p.hargabeli,0)),0) as nilai_total ");
			sb.append("from asset.mutasi_lokasi m ");
			sb.append("join asset.lokasi l on l.id = m.lokasi ");
			sb.append("left join asset.gudang g on g.id = l.gudang ");
			sb.append("left join asset.gudang gi on gi.id = g.gudang_induk ");
			sb.append("join koperasi.produk p on p.id = m.produk where 1=1 ");
			if (jenisLokasiId != null && jenisLokasiId.longValue() > 0) {
				sb.append("and l.jenis_lokasi = :jenis ");
			}
			boolean adaCari = cari != null && cari.trim().length() > 0;
			if (adaCari) {
				sb.append("and (p.nama ilike :cari or p.kode ilike :cari) ");
			}
			sb.append("group by g.id, g.nama, gi.nama ");
			sb.append("having coalesce(sum(m.qty * coalesce(p.hargabeli,0)),0) <> 0 order by gudang_nama");

			SQLQuery q = session.createSQLQuery(sb.toString());
			if (jenisLokasiId != null && jenisLokasiId.longValue() > 0) {
				q.setParameter("jenis", jenisLokasiId);
			}
			if (adaCari) {
				q.setParameter("cari", "%" + cari.trim() + "%");
			}
			List<Object[]> hasil = q.list();
			return hasil == null ? new ArrayList<Object[]>() : hasil;
		} catch (Exception e) {
			return new ArrayList<Object[]>();
		}
	}

	/**
	 * Tren mutasi harian (jumlah masuk vs keluar per tanggal) untuk grafik garis dasbor.
	 *
	 * <p>Tiap {@code Object[]} berisi: {@code [0]=tanggal (Date), [1]=totalMasuk (Number),
	 * [2]=totalKeluar (Number, nilai positif)}. Memakai {@code cast(... as date)} (bukan {@code ::date})
	 * demi menghindari salah-tafsir parameter bernama oleh Hibernate.</p>
	 *
	 * @param session       sesi Hibernate milik pemanggil (tidak ditutup di sini).
	 * @param jenisLokasiId bila &gt; 0, hanya lokasi berjenis tsb; selain itu semua.
	 * @param hari          jumlah hari ke belakang (mis. 30).
	 * @return daftar baris tren terurut tanggal (tidak pernah {@code null}).
	 */
	@SuppressWarnings("unchecked")
	public static List<Object[]> trenMutasiHarian(Session session, Long jenisLokasiId, int hari) {
		if (session == null) {
			return new ArrayList<Object[]>();
		}
		try {
			java.util.Calendar cal = java.util.Calendar.getInstance();
			cal.add(java.util.Calendar.DAY_OF_MONTH, -(hari <= 0 ? 30 : hari - 1));
			String sejak = new java.text.SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());

			StringBuilder sb = new StringBuilder();
			sb.append("select cast(m.tanggal as date) as tgl, ");
			sb.append("  coalesce(sum(case when m.qty>0 then m.qty else 0 end),0) as masuk, ");
			sb.append("  coalesce(sum(case when m.qty<0 then -m.qty else 0 end),0) as keluar ");
			sb.append("from asset.mutasi_lokasi m join asset.lokasi l on l.id = m.lokasi ");
			sb.append("where cast(m.tanggal as date) >= cast(:sejak as date) ");
			if (jenisLokasiId != null && jenisLokasiId.longValue() > 0) {
				sb.append("and l.jenis_lokasi = :jenis ");
			}
			sb.append("group by cast(m.tanggal as date) order by tgl");

			SQLQuery q = session.createSQLQuery(sb.toString());
			q.setParameter("sejak", sejak);
			if (jenisLokasiId != null && jenisLokasiId.longValue() > 0) {
				q.setParameter("jenis", jenisLokasiId);
			}
			List<Object[]> hasil = q.list();
			return hasil == null ? new ArrayList<Object[]>() : hasil;
		} catch (Exception e) {
			return new ArrayList<Object[]>();
		}
	}
}
