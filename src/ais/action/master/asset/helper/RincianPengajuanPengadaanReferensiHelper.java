package ais.action.master.asset.helper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.MasterAsset;
import ais.database.model.asset.PemesananPengadaanMasterAsset;
import ais.database.model.asset.PemesananPengadaanMasterAssetDetail;
import ais.database.model.asset.PenerimaanPengadaanMasterAsset;
import ais.database.model.asset.PenerimaanPengadaanMasterAssetDetail;
import ais.database.model.asset.PermintaanPengadaanMasterAsset;
import ais.database.model.asset.PermintaanPengadaanMasterAssetDetail;
import ais.database.model.sop.DisposisiAlurSop;
import ais.database.model.sop.DisposisiSop;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyGroupboxStyled;
import ais.ui.util.MyLabelKecil;

/**
 * Helper penampil RINCIAN PENGAJUAN sebagai referensi read-only pada layar disposisi SOP pengadaan.
 *
 * <p><b>Latar belakang:</b> Sejumlah SOP pengadaan (mis. <i>"Reimburse / Penggantian Dana"</i> yang
 * berupa <i>penerimaan langsung tanpa pemesanan</i>) menyimpan rincian barang/jasa pengaju pada entitas
 * {@link PenerimaanPengadaanMasterAsset} (atau {@link PermintaanPengadaanMasterAsset}), BUKAN pada
 * {@link PemesananPengadaanMasterAsset}. Namun langkah persetujuan tertentu (mis. "Persetujuan I" oleh
 * Bendahara Yayasan) dikonfigurasi menampilkan form <b>Pemesanan</b> — sehingga grid "Daftar Pemesanan
 * Barang/Jasa" tampil KOSONG walau pengaju sudah mengisi rincian (rincian itu ada di dokumen Penerimaan
 * yang tampil pada langkah verifikasi sebelumnya).</p>
 *
 * <p><b>Fungsi:</b> Diberi sebuah {@link DisposisiSop}, helper ini mencari dokumen pengajuan yang tertaut
 * (Penerimaan → Permintaan → Pemesanan, prioritas berurutan) lewat {@code disposisiSop.properti} /
 * {@code DisposisiAlurSop.properti} maupun FK, lalu membangun sebuah blok grid read-only berisi rincian
 * barang/jasa pengaju agar aktor persetujuan tetap dapat melihat "data pengajuan" tersebut.</p>
 *
 * <p><b>Sifat aman:</b> Semua query lookup memakai {@link FlushMode#MANUAL} (read-only, tak memicu
 * auto-flush entitas kotor di tengah transaksi tampil SOP) dan tanpa filter kolom {@code aktif} agar
 * data pengajuan tetap tampil sebagai rujukan. Mengembalikan {@code null} bila tak ada rincian ditemukan
 * sehingga pemanggil cukup melewati (tak ada blok kosong yang muncul).</p>
 */
public class RincianPengajuanPengadaanReferensiHelper {

	/**
	 * Membangun blok referensi rincian pengajuan untuk sebuah disposisi. Mengembalikan {@code null}
	 * bila disposisi tidak valid atau tak ada rincian pengajuan yang dapat ditemukan.
	 */
	public static MyGroupboxStyled bangunReferensiPengajuan(DisposisiSop disposisiSop) {
		try {
			if (disposisiSop == null || disposisiSop.getId() == null) {
				return null;
			}
			Session session = HibernateUtil.currentSession();

			// Setiap baris: { nama(String), qty(Double), harga(Double), total(Double), keterangan(String) }
			List<Object[]> baris = new ArrayList<Object[]>();

			// Prioritas: Penerimaan (kasus reimburse/penerimaan langsung) -> Permintaan -> Pemesanan.
			String sumber = kumpulkanDari(session, disposisiSop, PenerimaanPengadaanMasterAsset.class,
					PenerimaanPengadaanMasterAssetDetail.class, "penerimaanPengadaanMasterAsset",
					"Penerimaan Barang/Jasa", baris);
			if (baris.isEmpty()) {
				sumber = kumpulkanDari(session, disposisiSop, PermintaanPengadaanMasterAsset.class,
						PermintaanPengadaanMasterAssetDetail.class, "permintaanPengadaanMasterAsset",
						"Permintaan Barang/Jasa", baris);
			}
			if (baris.isEmpty()) {
				sumber = kumpulkanDari(session, disposisiSop, PemesananPengadaanMasterAsset.class,
						PemesananPengadaanMasterAssetDetail.class, "pemesananPengadaanMasterAsset",
						"Pemesanan Barang/Jasa", baris);
			}

			if (baris.isEmpty()) {
				return null;
			}

			MyGroupboxStyled gb = new MyGroupboxStyled();
			gb.appendChild(new MyCaptionStyled("Rincian Pengajuan (" + sumber + ")"));

			MyGrid grid = new MyGrid();
			grid.setWidth("100%");

			Columns cols = new Columns();
			cols.setParent(grid);
			MyColumnConfig cn = new MyColumnConfig("Nama");
			cn.setParent(cols);
			MyColumnConfig cq = new MyColumnConfig("Qty");
			cq.setParent(cols);
			cq.setWidth("70px");
			MyColumnConfig ch = new MyColumnConfig("Harga");
			ch.setParent(cols);
			ch.setWidth("120px");
			MyColumnConfig ct = new MyColumnConfig("Total");
			ct.setParent(cols);
			ct.setWidth("130px");
			MyColumnConfig ck = new MyColumnConfig("Keterangan");
			ck.setParent(cols);

			Rows rows = new Rows();
			rows.setParent(grid);

			double totalSemua = 0.0;
			for (Object[] b : baris) {
				Row r = new Row();
				r.setValign("top");
				r.setParent(rows);
				r.appendChild(new MyLabelKecil((String) b[0]));
				r.appendChild(new MyLabelKecil(Common.numberFormat1.get().format(nz((Double) b[1]))));
				r.appendChild(new MyLabelKecil(Common.numberFormat1.get().format(nz((Double) b[2]))));
				double t = nz((Double) b[3]);
				totalSemua += t;
				r.appendChild(new MyLabelKecil(Common.numberFormat1.get().format(t)));
				r.appendChild(new MyLabelKecil(b[4] == null ? "" : (String) b[4]));
			}

			gb.appendChild(grid);
			return gb;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return null;
		}
	}

	private static double nz(Double d) {
		return d == null ? 0.0 : d;
	}

	/**
	 * Mengumpulkan baris rincian dari satu jenis entitas pengajuan (Penerimaan/Permintaan/Pemesanan)
	 * yang tertaut ke {@code disposisiSop}, melalui id di properti maupun FK.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static String kumpulkanDari(Session session, DisposisiSop disposisiSop, Class kelasEntitas,
			Class kelasDetail, String propAsosiasi, String labelSumber, List<Object[]> baris) {
		try {
			LinkedHashSet<Long> ids = new LinkedHashSet<Long>();
			String key = kelasEntitas.getName();

			// id dari properti tingkat DisposisiSop
			ambilIdDariProperti(disposisiSop.getProperti(), key, ids);

			// id dari properti tiap DisposisiAlurSop pada SOP ini
			try {
				List<DisposisiAlurSop> alurs = session.createCriteria(DisposisiAlurSop.class)
						.add(Restrictions.eq("disposisiSop", disposisiSop)).setFlushMode(FlushMode.MANUAL).list();
				for (DisposisiAlurSop a : alurs) {
					ambilIdDariProperti(a.getProperti(), key, ids);
				}
			} catch (Exception ig) {
			}

			List<Object> entitasFound = new ArrayList<Object>();
			for (Long id : ids) {
				Object e = session.get(kelasEntitas, id);
				if (e != null && !entitasFound.contains(e)) {
					entitasFound.add(e);
				}
			}

			// Tautan FK langsung (tanpa filter aktif)
			try {
				List<Object> byFk = session.createCriteria(kelasEntitas)
						.add(Restrictions.eq("disposisiSop", disposisiSop)).addOrder(Order.asc("id"))
						.setFlushMode(FlushMode.MANUAL).list();
				for (Object e : byFk) {
					if (e != null && !entitasFound.contains(e)) {
						entitasFound.add(e);
					}
				}
			} catch (Exception ig) {
			}

			for (Object e : entitasFound) {
				List<Object> detail = session.createCriteria(kelasDetail)
						.add(Restrictions.eq(propAsosiasi, e)).setFlushMode(FlushMode.MANUAL).list();
				for (Object d : detail) {
					tambahBaris(d, baris);
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return labelSumber;
	}

	private static void ambilIdDariProperti(String properti, String key, LinkedHashSet<Long> ids) {
		if (properti == null || properti.trim().isEmpty()) {
			return;
		}
		try {
			JSONObject o = new JSONObject(properti);
			// bentuk peta { classname: {id,...} }
			if (!o.isNull(key)) {
				tambahId(o.getJSONObject(key), ids);
			}
			// bentuk langsung { id,... } (properti per-DisposisiAlurSop)
			tambahId(o, ids);
		} catch (Exception ig) {
		}
	}

	private static void tambahId(JSONObject o, LinkedHashSet<Long> ids) {
		try {
			if (o != null && !o.isNull("id")) {
				ids.add(Long.parseLong(o.get("id") + ""));
			}
		} catch (Exception ig) {
		}
	}

	private static void tambahBaris(Object d, List<Object[]> baris) {
		try {
			MasterAsset ma = null;
			Double qty = 0.0, harga = 0.0, total = 0.0;
			String ket = "";
			if (d instanceof PenerimaanPengadaanMasterAssetDetail) {
				PenerimaanPengadaanMasterAssetDetail x = (PenerimaanPengadaanMasterAssetDetail) d;
				ma = x.getMasterAsset();
				qty = x.getJumlah();
				harga = x.getHargaBeli();
				total = x.getHargaTotal();
				ket = x.getKeterangan();
			} else if (d instanceof PermintaanPengadaanMasterAssetDetail) {
				PermintaanPengadaanMasterAssetDetail x = (PermintaanPengadaanMasterAssetDetail) d;
				ma = x.getMasterAsset();
				qty = x.getJumlah();
				harga = x.getHargaBeli();
				total = x.getHargaTotal();
				ket = x.getKeterangan();
			} else if (d instanceof PemesananPengadaanMasterAssetDetail) {
				PemesananPengadaanMasterAssetDetail x = (PemesananPengadaanMasterAssetDetail) d;
				ma = x.getMasterAsset();
				qty = x.getJumlah();
				harga = x.getHargaBeli();
				total = x.getHargaTotal();
				ket = x.getKeterangan();
			} else {
				return;
			}
			String nama = ma == null ? "" : ma.getNama();
			baris.add(new Object[] { nama, qty, harga, total, ket });
		} catch (Exception ig) {
		}
	}
}
