package ais.action.master.inventory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.model.asset.JenisLokasi;
import ais.database.model.asset.Lokasi;
import ais.database.model.asset.PengirimanGudang;
import ais.database.model.asset.PengirimanGudangDetail;
import ais.database.model.inventory.Produk;
import ais.database.model.inventory.ReturBarang;
import ais.database.model.inventory.Toko;
import ais.ui.util.WaktuUtil;

/**
 * <h2>PengirimanGudangUtil — mesin alur <b>kirim → dalam perjalanan → terima</b> antar {@link Lokasi}.</h2>
 *
 * <p>
 * Melengkapi {@link StokLokasiUtil#catatTransfer} (instan-atomik) dengan jeda konfirmasi penerimaan —
 * lihat javadoc lengkap desain di {@link PengirimanGudang}. Kelas ini SAMA SEKALI TIDAK mengubah
 * {@link StokLokasiUtil}/{@link ais.database.model.asset.MutasiLokasi} — {@link #kirim} dan
 * {@link #terima} murni memanggil {@code catatKeluar}/{@code catatMasuk} yang sudah ada, dua kali
 * (asal→transit saat kirim, transit→tujuan saat terima), dengan {@link PengirimanGudang#getKode()}
 * sebagai {@code referensi} baris mutasi supaya tertelusur.
 * </p>
 *
 * <h3>Kontrak sesi</h3>
 * <p>
 * POLA A, sama seperti {@link StokLokasiUtil} — semua metode menerima {@link Session} dari pemanggil
 * dan tidak menutupnya.
 * </p>
 *
 * @author AIS e-Kantin (modul pergudangan)
 * @see PengirimanGudang
 * @see PengirimanGudangDetail
 * @see StokLokasiUtil
 */
public final class PengirimanGudangUtil {

	private PengirimanGudangUtil() {
		// util murni.
	}

	/** Satu baris produk pada permintaan kirim — DTO sederhana (Java 1.7, tanpa record/lambda). */
	public static final class DetailKirim {
		public Long produkId;
		public double qty;
		public Double hargaSatuan;

		public DetailKirim() {
		}

		public DetailKirim(Long produkId, double qty, Double hargaSatuan) {
			this.produkId = produkId;
			this.qty = qty;
			this.hargaSatuan = hargaSatuan;
		}
	}

	/**
	 * Membuat dokumen pengiriman baru: barang dicatat KELUAR dari {@code lokasiAsalId} dan MASUK ke
	 * lokasi transit otomatis (dibuat/didapat sekali per pengiriman) — stok tujuan BELUM bertambah
	 * sampai {@link #terima} dipanggil.
	 *
	 * @param session        sesi Hibernate milik pemanggil (tidak ditutup di sini).
	 * @param lokasiAsalId   lokasi asal (mis. Gudang Pusat).
	 * @param lokasiTujuanId lokasi tujuan (mis. Cabang/Outlet); wajib berbeda dari asal.
	 * @param detail         baris produk+qty yang dikirim (minimal 1 baris dgn qty &gt; 0).
	 * @param tanggal        tanggal kirim (boleh null → sekarang).
	 * @param keterangan     catatan pengiriman.
	 * @param oleh           nama pengguna (audit).
	 * @param olehId         id pengguna (audit).
	 * @return dokumen {@link PengirimanGudang} berstatus {@link PengirimanGudang#DIKIRIM}.
	 * @throws IllegalArgumentException bila asal/tujuan sama, atau tidak ada baris produk valid.
	 */
	public static PengirimanGudang kirim(Session session, Long lokasiAsalId, Long lokasiTujuanId,
			List<DetailKirim> detail, Date tanggal, String keterangan, String oleh, String olehId) {
		if (lokasiAsalId == null || lokasiTujuanId == null || lokasiAsalId.equals(lokasiTujuanId)) {
			throw new IllegalArgumentException("Lokasi asal dan tujuan harus berbeda.");
		}
		if (detail == null || detail.isEmpty()) {
			throw new IllegalArgumentException("Minimal satu baris produk wajib diisi.");
		}
		Date t = tanggal == null ? WaktuUtil.getDate() : tanggal;
		String kode = "KRM-" + Common.getGeneratedBarCode(8);

		Lokasi transit = buatLokasiTransit(session, kode, oleh, olehId);

		PengirimanGudang header = new PengirimanGudang();
		header.setKode(kode);
		header.setLokasiAsal((Lokasi) session.get(Lokasi.class, lokasiAsalId));
		header.setLokasiTujuan((Lokasi) session.get(Lokasi.class, lokasiTujuanId));
		header.setLokasiTransit(transit);
		header.setTanggalKirim(t);
		header.setStatus(PengirimanGudang.DIKIRIM);
		header.setKeterangan(keterangan);
		header.setDikirimOleh(oleh);
		header.setDikirimOlehId(olehId);
		header.setOleh(oleh);
		header.setOlehId(olehId);
		Common.refreshSaveOrUpdate(session, header);

		int baris = 0;
		for (int i = 0; i < detail.size(); i++) {
			DetailKirim d = detail.get(i);
			if (d == null || d.produkId == null || d.qty <= 0) {
				continue;
			}
			StokLokasiUtil.catatKeluar(session, lokasiAsalId, d.produkId, d.qty, d.hargaSatuan, t, keterangan, kode,
					oleh, olehId);
			StokLokasiUtil.catatMasuk(session, transit.getId(), d.produkId, d.qty, d.hargaSatuan, t, keterangan, kode,
					oleh, olehId);

			PengirimanGudangDetail row = new PengirimanGudangDetail();
			row.setPengiriman(header);
			row.setProduk((Produk) session.get(Produk.class, d.produkId));
			row.setQtyKirim(Double.valueOf(d.qty));
			row.setHargaSatuan(d.hargaSatuan);
			row.setOleh(oleh);
			row.setOlehId(olehId);
			Common.refreshSaveOrUpdate(session, row);
			baris++;
		}
		if (baris == 0) {
			throw new IllegalArgumentException("Minimal satu baris produk dengan jumlah > 0 wajib diisi.");
		}
		return header;
	}

	/**
	 * Mengonfirmasi penerimaan barang: memindahkan jumlah yang benar-benar diterima
	 * ({@code qtyTerimaPerDetailId}) dari lokasi transit ke {@link PengirimanGudang#getLokasiTujuan()}.
	 * Boleh dipanggil dengan qty kurang dari qty kirim (penerimaan sebagian) — sisanya TETAP mengendap
	 * di lokasi transit sebagai jejak audit, TIDAK dikoreksi/dihilangkan otomatis (perlu tindak lanjut
	 * manual, selaras prinsip stock opname §18).
	 *
	 * @param session              sesi Hibernate milik pemanggil (tidak ditutup di sini).
	 * @param pengirimanId         id {@link PengirimanGudang} yang diterima.
	 * @param qtyTerimaPerDetailId peta {@code PengirimanGudangDetail.id → qty diterima} (baris yang
	 *                             tidak ada di peta ini dianggap qty diterima = 0 pada panggilan ini).
	 * @param keteranganTerima     catatan penerimaan (mis. alasan bila sebagian/selisih).
	 * @param oleh                 nama pengguna penerima (audit).
	 * @param olehId               id pengguna penerima (audit).
	 * @return dokumen {@link PengirimanGudang} dgn status termutakhirkan.
	 * @throws IllegalArgumentException bila dokumen tidak ditemukan.
	 * @throws IllegalStateException    bila dokumen sudah {@link PengirimanGudang#DITERIMA}/
	 *                                  {@link PengirimanGudang#DIBATALKAN}, atau tidak ada qty &gt; 0
	 *                                  yang diterima pada panggilan ini.
	 */
	public static PengirimanGudang terima(Session session, Long pengirimanId, Map<Long, Double> qtyTerimaPerDetailId,
			String keteranganTerima, String oleh, String olehId) {
		return terima(session, pengirimanId, qtyTerimaPerDetailId, null, null, keteranganTerima, oleh, olehId);
	}

	/**
	 * Varian dgn cek kondisi barang per baris -- fitur "Pengiriman Antar Gudang: cek kondisi barang +
	 * retur vendor" (gap analisis PDF klien 2026-07-26). Dari {@code qtyTerimaPerDetailId} (total
	 * dikonfirmasi diterima), sebagian boleh ditandai RUSAK lewat {@code qtyRusakPerDetailId} -- porsi
	 * itu TETAP keluar dari lokasi transit (baris selesai diproses) tapi TIDAK ditambahkan ke stok
	 * lokasi tujuan; sebagai gantinya otomatis dicatat sbg {@link ReturBarang}. Hanya porsi BAIK
	 * ({@code qtyTerima - qtyRusak}) yang menambah stok lokasi tujuan seperti biasa.
	 *
	 * @param qtyRusakPerDetailId    peta {@code detailId → qty rusak} (boleh {@code null}/kosong =
	 *                               tidak ada yang rusak, perilaku identik overload lama).
	 * @param alasanRusakPerDetailId peta {@code detailId → alasan rusak} (opsional).
	 */
	public static PengirimanGudang terima(Session session, Long pengirimanId, Map<Long, Double> qtyTerimaPerDetailId,
			Map<Long, Double> qtyRusakPerDetailId, Map<Long, String> alasanRusakPerDetailId,
			String keteranganTerima, String oleh, String olehId) {
		PengirimanGudang header = (PengirimanGudang) session.get(PengirimanGudang.class, pengirimanId);
		if (header == null) {
			throw new IllegalArgumentException("Dokumen pengiriman tidak ditemukan.");
		}
		if (PengirimanGudang.DITERIMA.equals(header.getStatus()) || PengirimanGudang.DIBATALKAN.equals(header.getStatus())) {
			throw new IllegalStateException("Pengiriman ini berstatus " + header.getStatus() + ", tidak bisa diterima lagi.");
		}
		List<PengirimanGudangDetail> baris = detailPengiriman(session, pengirimanId);
		Long lokasiTransitId = header.getLokasiTransit().getId();
		Long lokasiTujuanId = header.getLokasiTujuan().getId();
		Toko tokoTujuan = header.getLokasiTujuan().getToko();
		Date sekarang = WaktuUtil.getDate();

		boolean semuaPenuh = true;
		boolean adaYangDiterima = false;
		for (int i = 0; i < baris.size(); i++) {
			PengirimanGudangDetail d = baris.get(i);
			Double qtyTerimaWrap = qtyTerimaPerDetailId == null ? null : qtyTerimaPerDetailId.get(d.getId());
			double qt = qtyTerimaWrap == null ? 0d : qtyTerimaWrap.doubleValue();
			if (qt < 0) {
				qt = 0;
			}
			double qtyKirim = d.getQtyKirim().doubleValue();
			if (qt > qtyKirim) {
				qt = qtyKirim; // jaga: tak bisa terima lebih banyak dari yang dikirim
			}

			Double qtyRusakWrap = qtyRusakPerDetailId == null ? null : qtyRusakPerDetailId.get(d.getId());
			double qtyRusak = qtyRusakWrap == null ? 0d : qtyRusakWrap.doubleValue();
			if (qtyRusak < 0) {
				qtyRusak = 0;
			}
			if (qtyRusak > qt) {
				qtyRusak = qt; // jaga: tak bisa rusak lebih banyak dari yang diterima
			}
			double qtyBaik = qt - qtyRusak;

			if (qt > 0) {
				// Seluruh qt (baik + rusak) keluar dari transit -- baris ini SELESAI diproses, tidak ada
				// yg mengendap lagi utk porsi yg sudah dipastikan kondisinya (baik ATAU rusak).
				StokLokasiUtil.catatKeluar(session, lokasiTransitId, d.getProduk().getId(), qt, d.getHargaSatuan(),
						sekarang, keteranganTerima, header.getKode(), oleh, olehId);
				if (qtyBaik > 0) {
					StokLokasiUtil.catatMasuk(session, lokasiTujuanId, d.getProduk().getId(), qtyBaik,
							d.getHargaSatuan(), sekarang, keteranganTerima, header.getKode(), oleh, olehId);
				}
				adaYangDiterima = true;
			}
			if (qtyRusak > 0) {
				String alasan = alasanRusakPerDetailId == null ? null : alasanRusakPerDetailId.get(d.getId());
				ReturBarang retur = new ReturBarang();
				retur.setProduk(d.getProduk());
				retur.setToko(tokoTujuan);
				retur.setQty(Double.valueOf(qtyRusak));
				retur.setHargaSatuan(d.getHargaSatuan());
				retur.setWaktu(sekarang);
				retur.setOleh(oleh);
				retur.setKeterangan("Otomatis dari penerimaan Pengiriman Antar Gudang " + header.getKode()
						+ " (kondisi rusak saat terima)" + (alasan == null || alasan.trim().isEmpty() ? "" : ": " + alasan));
				Common.refreshSaveOrUpdate(session, retur);
				d.setAlasanRusak(alasan);
			}
			d.setQtyTerima(Double.valueOf(qt));
			d.setQtyRusak(Double.valueOf(qtyRusak));
			Common.refreshSaveOrUpdate(session, d);
			if (qt < qtyKirim) {
				semuaPenuh = false;
			}
		}

		if (!adaYangDiterima) {
			throw new IllegalStateException("Isi jumlah diterima minimal satu baris dengan nilai lebih dari 0.");
		}

		header.setStatus(semuaPenuh ? PengirimanGudang.DITERIMA : PengirimanGudang.DITERIMA_SEBAGIAN);
		header.setTanggalTerima(sekarang);
		header.setKeteranganTerima(keteranganTerima);
		header.setDiterimaOleh(oleh);
		header.setDiterimaOlehId(olehId);
		Common.refreshSaveOrUpdate(session, header);
		return header;
	}

	/** @return baris detail milik satu dokumen pengiriman (tidak pernah {@code null}). */
	@SuppressWarnings("unchecked")
	public static List<PengirimanGudangDetail> detailPengiriman(Session session, Long pengirimanId) {
		if (session == null || pengirimanId == null) {
			return new ArrayList<PengirimanGudangDetail>();
		}
		List<PengirimanGudangDetail> hasil = session.createCriteria(PengirimanGudangDetail.class)
				.add(Restrictions.eq("pengiriman.id", pengirimanId)).list();
		return hasil == null ? new ArrayList<PengirimanGudangDetail>() : hasil;
	}

	/**
	 * Inbox "Perlu Diterima" untuk satu lokasi tujuan (mis. Cabang/Outlet mengecek kiriman masuk).
	 *
	 * @param statusFilter bila diisi, hanya dokumen berstatus tsb; bila kosong/null, semua status.
	 * @return dokumen terbaru dulu (tidak pernah {@code null}).
	 */
	@SuppressWarnings("unchecked")
	public static List<PengirimanGudang> daftarUntukLokasiTujuan(Session session, Long lokasiTujuanId, String statusFilter) {
		if (session == null || lokasiTujuanId == null) {
			return new ArrayList<PengirimanGudang>();
		}
		Criteria c = session.createCriteria(PengirimanGudang.class)
				.add(Restrictions.eq("lokasiTujuan.id", lokasiTujuanId)).addOrder(Order.desc("tanggalKirim"));
		if (statusFilter != null && statusFilter.trim().length() > 0) {
			c.add(Restrictions.eq("status", statusFilter.trim()));
		}
		List<PengirimanGudang> hasil = c.list();
		return hasil == null ? new ArrayList<PengirimanGudang>() : hasil;
	}

	/** @return riwayat 200 pengiriman terbaru dari satu lokasi asal (tidak pernah {@code null}). */
	@SuppressWarnings("unchecked")
	public static List<PengirimanGudang> daftarUntukLokasiAsal(Session session, Long lokasiAsalId) {
		if (session == null || lokasiAsalId == null) {
			return new ArrayList<PengirimanGudang>();
		}
		List<PengirimanGudang> hasil = session.createCriteria(PengirimanGudang.class)
				.add(Restrictions.eq("lokasiAsal.id", lokasiAsalId)).addOrder(Order.desc("tanggalKirim"))
				.setMaxResults(200).list();
		return hasil == null ? new ArrayList<PengirimanGudang>() : hasil;
	}

	/**
	 * Membuat {@link Lokasi} transit virtual milik satu dokumen pengiriman: {@code aktif=false} supaya
	 * tidak muncul di picker Lokasi manual manapun, tapi tetap query-able lewat
	 * {@link StokLokasiUtil#qtyStok}/{@link StokLokasiUtil#rekapStokLokasi} seperti Lokasi biasa.
	 */
	private static Lokasi buatLokasiTransit(Session session, String kode, String oleh, String olehId) {
		Lokasi l = new Lokasi();
		l.setNama("Transit " + kode);
		l.setKeterangan("Lokasi transit otomatis untuk pengiriman " + kode + " -- jangan dipilih manual.");
		l.setAktif(Boolean.FALSE);
		l.setJenisLokasi(ambilAtauBuatJenisLokasiTransit(session));
		l.setOleh(oleh);
		l.setOlehId(olehId);
		Common.refreshSaveOrUpdate(session, l);
		return l;
	}

	/**
	 * Get-or-create {@link JenisLokasi} "Dalam Perjalanan" — didaftarkan juga di
	 * {@code LokasiKantinUtil.JENIS_LOKASI_DEFAULT} (seed saat startup) supaya tampil rapi di layar
	 * admin Jenis Lokasi; pencarian di sini murni jaga-jaga bila startup seeding belum sempat berjalan
	 * (mis. baru deploy) sehingga fitur ini tetap langsung bisa dipakai tanpa restart server.
	 */
	@SuppressWarnings("unchecked")
	private static JenisLokasi ambilAtauBuatJenisLokasiTransit(Session session) {
		try {
			List<Object> rows = session.createSQLQuery(
					"select id from asset.jenis_lokasi where lower(nama)='dalam perjalanan'").list();
			if (rows != null && !rows.isEmpty() && rows.get(0) != null) {
				return (JenisLokasi) session.get(JenisLokasi.class, ((Number) rows.get(0)).longValue());
			}
		} catch (Exception ignore) {
			// jatuh ke pembuatan baru di bawah.
		}
		JenisLokasi jl = new JenisLokasi();
		jl.setNama("Dalam Perjalanan");
		jl.setWarna("#94a3b8");
		jl.setIkon("fas fa-truck-fast");
		jl.setAktif(Boolean.TRUE);
		jl.setUrutan(Integer.valueOf(99));
		jl.setKeterangan("Bawaan sistem -- lokasi transit otomatis fitur Pengiriman Antar Gudang.");
		jl.setOleh("SYSTEM");
		jl.setOlehId("SYSTEM");
		Common.refreshSaveOrUpdate(session, jl);
		return jl;
	}
}
