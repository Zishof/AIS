package ais.action.servlet.api;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.EbisnisMenuKatalog;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.asset.MasterAsset;
import ais.database.model.asset.PembayaranTerminMasterAsset;
import ais.database.model.asset.PembayaranTerminMasterAssetDetail;
import ais.database.model.asset.PemesananPengadaanMasterAsset;
import ais.database.model.asset.PemesananPengadaanMasterAssetDetail;
import ais.database.model.asset.PenerimaanPengadaanMasterAsset;
import ais.database.model.asset.PenerimaanPengadaanMasterAssetDetail;
import ais.database.model.asset.PenyediaAsset;
import ais.database.model.inventory.Produk;
import ais.database.model.asset.PermintaanPengadaanMasterAsset;
import ais.database.model.asset.PermintaanPengadaanMasterAssetDetail;
import ais.database.model.inventory.Toko;

/**
 * <h3>API JSON Modul Pengadaan POS -- tahap 1: Permintaan Pembelian (PR).</h3>
 *
 * <p>Dipakai bersama oleh JSP e-Kantin dan POS Desktop/Android, sehingga aturan bisnisnya
 * SATU tempat (pola {@code GrupProdukApiHelper}/{@code KantinHelper}). Alur & semantik status
 * MENGIKUTI versi ZKoss {@code PermintaanPengadaanMasterAssetAction}, tetapi lingkupnya
 * disederhanakan ke {@link Toko} + {@link Produk} sesuai keputusan produk 2026-08-19.</p>
 *
 * <p><b>Status PR</b>: {@code DRAFT} (belum ada keputusan), {@code DISETUJUI}
 * ({@code tanggalPersetujuan} terisi), {@code DITOLAK} ({@code tanggalDitolak} terisi), dan
 * {@code TUTUP} (tidak dapat diproses lebih lanjut). Sama persis dengan versi umum agar
 * pengguna yang sudah terbiasa tidak perlu belajar ulang.</p>
 *
 * <p><b>Gerbang</b>: seluruh handler self-guard kunci menu {@code pengadaan_pr} + aksi granular
 * ({@code view}/{@code create}/{@code update}/{@code delete}/{@code approve}). Kunci ini
 * terdaftar di {@code KUNCI_DEFAULT_NONAKTIF} sehingga role existing TIDAK mendadak bisa
 * membuat komitmen pembelian; admin menyalakannya per-role lewat grid CRUD TbmroleAction.</p>
 */
public final class PengadaanPosApiHelper {

	/** Kunci menu per tahap pengadaan -- tiap tahap punya hak akses sendiri di TbmroleAction. */
	private static final String KUNCI_PR = "pengadaan_pr";
	private static final String KUNCI_PO = "pengadaan_po";
	private static final String KUNCI_BAST = "pengadaan_bast";
	private static final String KUNCI_TAGIHAN = "pengadaan_tagihan";
	private static final String KUNCI_DPC = "pengadaan_dpc";
	private static final String KUNCI_BDP = "pengadaan_bdp";
	private static final String KUNCI_SINKRON = "pengadaan_sinkron";

	private PengadaanPosApiHelper() {
	}

	private static void tolak(JSONObject hasil, String pesan) throws Exception {
		hasil.put("status", "91");
		hasil.put("description", pesan);
	}

	private static boolean bolehLihat(Tbmuser tbmuser, String kunci) {
		if (tbmuser == null) {
			return false;
		}
		if (Common.getApakahAdminLain(tbmuser)) {
			return true;
		}
		Tbmrole role = tbmuser.hakAkses();
		if (role == null) {
			return true;
		}
		JSONObject menu = EbisnisMenuKatalog.urai(role.getEbisnisMenu()).optJSONObject("menu");
		// Bawaan TAMPIL (2026-08-20, permintaan pemilik produk): peran lama yang JSON menunya
		// belum memuat kunci ini tetap dapat memakai modul Pengadaan; admin dapat mematikannya
		// per-peran lewat grid CRUD TbmroleAction.
		return menu == null || menu.optBoolean(kunci, true);
	}

	private static boolean bolehAksi(Tbmuser tbmuser, String kunci, String aksi) {
		if (tbmuser == null) {
			return false;
		}
		if (Common.getApakahAdminLain(tbmuser)) {
			return true;
		}
		Tbmrole role = tbmuser.hakAkses();
		if (role == null) {
			return true;
		}
		return EbisnisMenuKatalog.bolehAksi(
				EbisnisMenuKatalog.urai(role.getEbisnisMenu()), kunci, aksi);
	}

	/** Toko lingkup pemanggil: pedagang dikunci ke tokonya, admin boleh memilih lewat payload. */
	private static Long tokoLingkup(Tbmuser tbmuser, JSONObject request) {
		if (tbmuser != null && tbmuser.getPedagang() != null) {
			if (tbmuser.getPedagang().getToko() != null) {
				return tbmuser.getPedagang().getToko().getId();
			}
			if (tbmuser.getTokoAktifMultiToko() != null) {
				return tbmuser.getTokoAktifMultiToko();
			}
		}
		if (request != null && !request.isNull("toko_id")) {
			try {
				return Long.valueOf((request.get("toko_id") + "").trim());
			} catch (Exception e) {
				return null;
			}
		}
		return null;
	}

	/** Label status baris PR -- dihitung server supaya SEMUA kanal menampilkan istilah sama. */
	private static String statusPr(PermintaanPengadaanMasterAsset pr) {
		if (Boolean.TRUE.equals(pr.getTutup())) {
			return "TUTUP";
		}
		if (pr.getTanggalDitolak() != null) {
			return "DITOLAK";
		}
		if (pr.getTanggalPersetujuan() != null) {
			return "DISETUJUI";
		}
		return "DRAFT";
	}

	/**
	 * Nomor PR otomatis: {@code PR/<toko>/<yyyyMM>/<urut>} dengan urut per toko per bulan.
	 * Dicoba beberapa kali bila bentrok (aman terhadap dua kasir menyimpan bersamaan).
	 */
	private static String buatKode(Session session, Long tokoId) {
		return buatKodeUmum(session, PermintaanPengadaanMasterAsset.class, "PR", tokoId);
	}


	/**
	 * Jembatan Produk POS ke MasterAsset.
	 *
	 * <p>Tabel pengadaan dipakai BERSAMA dengan JSP/ZKoss dan ber-FK ke {@code master_asset},
	 * sedangkan pengguna toko memilih dari daftar Produk POS. Fungsi ini mencarikan padanan
	 * MasterAsset untuk sebuah produk dan MENYIMPAN tautannya pada {@code produk.master_asset},
	 * sehingga pemetaan terisi sendiri seiring pemakaian -- sekaligus menjadi jembatan yang
	 * dibutuhkan sinkronisasi penerimaan ke Kulakan.</p>
	 *
	 * <p>Urutan pencarian: tautan yang sudah ada, lalu MasterAsset berkode sama, baru dibuat
	 * baru. Pencocokan lewat kode dipakai supaya produk yang sudah punya padanan aset tidak
	 * menghasilkan duplikat.</p>
	 */
	private static MasterAsset masterAssetUntukProduk(Session session, Produk produk) {
		if (produk == null) {
			return null;
		}
		if (produk.getMasterAsset() != null) {
			return produk.getMasterAsset();
		}
		String kode = produk.getKode() == null ? "" : produk.getKode().trim();
		MasterAsset padanan = null;
		if (!kode.isEmpty()) {
			padanan = (MasterAsset) session.createCriteria(MasterAsset.class)
					.add(Restrictions.eq("kode", kode)).setMaxResults(1).uniqueResult();
		}
		if (padanan == null) {
			padanan = new MasterAsset();
			padanan.setKode(kode.isEmpty() ? null : kode);
			padanan.setNama(produk.getNama() == null ? "(tanpa nama)" : produk.getNama());
			// Produk POS tidak menyimpan satuan tersendiri, jadi unit dibiarkan kosong.
			session.save(padanan);
			session.flush();
		}
		produk.setMasterAsset(padanan);
		session.saveOrUpdate(produk);
		return padanan;
	}

	/**
	 * Tentukan barang sebuah baris dokumen. Klien POS mengirim {@code produk_id};
	 * {@code master_asset_id} tetap diterima sebagai jalur langsung untuk data lama
	 * maupun barang inventaris yang memang tidak berpadanan produk toko.
	 */
	private static MasterAsset barangBaris(Session session, JSONObject baris) throws Exception {
		if (baris == null) {
			return null;
		}
		if (!baris.isNull("produk_id") && !(baris.get("produk_id") + "").trim().isEmpty()) {
			Produk produk = (Produk) session.get(Produk.class,
					Long.valueOf((baris.get("produk_id") + "").trim()));
			return masterAssetUntukProduk(session, produk);
		}
		if (!baris.isNull("master_asset_id") && !(baris.get("master_asset_id") + "").trim().isEmpty()) {
			return (MasterAsset) session.get(MasterAsset.class,
					Long.valueOf((baris.get("master_asset_id") + "").trim()));
		}
		return null;
	}

	/**
	 * Tentukan penyedia sebuah dokumen. Vendor pengadaan memakai master PENYEDIA ASET
	 * ({@code asset.penyedia_asset}) -- daftar yang sama dengan versi ZKoss, sesuai keputusan
	 * pemilik produk 2026-08-20. {@code penyedia_asset_id} diterima sebagai alias eksplisit.
	 */
	private static PenyediaAsset penyediaDokumen(Session session, JSONObject request) throws Exception {
		if (request == null) {
			return null;
		}
		String kunci = null;
		if (!request.isNull("penyedia_id") && !(request.get("penyedia_id") + "").trim().isEmpty()) {
			kunci = (request.get("penyedia_id") + "").trim();
		} else if (!request.isNull("penyedia_asset_id")
				&& !(request.get("penyedia_asset_id") + "").trim().isEmpty()) {
			kunci = (request.get("penyedia_asset_id") + "").trim();
		}
		if (kunci == null) {
			return null;
		}
		return (PenyediaAsset) session.get(PenyediaAsset.class, Long.valueOf(kunci));
	}

	/** Produk POS yang menunjuk sebuah MasterAsset -- dipakai agar layar dapat memuat ulang pilihan. */
	private static Produk produkDariMasterAsset(Session session, MasterAsset aset) {
		if (aset == null) {
			return null;
		}
		return (Produk) session.createCriteria(Produk.class)
				.add(Restrictions.eq("masterAsset.id", aset.getId()))
				.setMaxResults(1).uniqueResult();
	}

	/**
	 * Daftar PR pada lingkup toko pemanggil. Param opsional: {@code cari} (kode/keterangan),
	 * {@code status} (DRAFT/DISETUJUI/DITOLAK/TUTUP), {@code page}, {@code pageSize}.
	 */
	public static void prDaftar(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehLihat(tbmuser, KUNCI_PR)) {
			tolak(hasil, "Menu Pengadaan tidak diaktifkan untuk grup pengguna Anda.");
			return;
		}
		Long tokoId = tokoLingkup(tbmuser, request);
		int page = Math.max(1, request == null ? 1 : request.optInt("page", 1));
		int pageSize = Math.min(100, Math.max(5, request == null ? 15 : request.optInt("pageSize", 15)));
		String cari = request == null ? "" : request.optString("cari", "").trim();
		String status = request == null ? "" : request.optString("status", "").trim().toUpperCase();
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Criteria kriteria = session.createCriteria(PermintaanPengadaanMasterAsset.class);
			kriteria.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)));
			if (tokoId != null) {
				kriteria.add(Restrictions.eq("toko.id", tokoId));
			}
			if (cari.length() > 0) {
				kriteria.add(Restrictions.or(
						Restrictions.ilike("kode", cari, MatchMode.ANYWHERE),
						Restrictions.ilike("keterangan", cari, MatchMode.ANYWHERE)));
			}
			// Status difilter di Java (bukan SQL) karena merupakan turunan dari kombinasi
			// tanggal persetujuan/penolakan/tutup -- satu definisi, dipakai bersama statusPr().
			kriteria.addOrder(Order.desc("id"));
			@SuppressWarnings("unchecked")
			List<PermintaanPengadaanMasterAsset> semua = kriteria.list();
			JSONArray arr = new JSONArray();
			int cocok = 0;
			int mulai = (page - 1) * pageSize;
			for (PermintaanPengadaanMasterAsset pr : semua) {
				String st = statusPr(pr);
				if (status.length() > 0 && !status.equals(st)) {
					continue;
				}
				cocok++;
				if (cocok <= mulai || arr.length() >= pageSize) {
					continue;
				}
				JSONObject o = new JSONObject();
				o.put("id", pr.getId());
				o.put("kode", pr.getKode() == null ? "" : pr.getKode());
				o.put("keterangan", pr.getKeterangan() == null ? "" : pr.getKeterangan());
				o.put("tanggal", pr.getTanggalPembuatan() == null ? JSONObject.NULL
						: Common.dateFormat3.get().format(pr.getTanggalPembuatan()));
				o.put("nilai", pr.getNilai() == null ? 0 : pr.getNilai());
				o.put("status", st);
				o.put("toko", pr.getToko() == null ? "" : pr.getToko().getNama());
				o.put("dibuatOleh", pr.getDibuatOleh() == null ? "" : pr.getDibuatOleh().getUserNama());
				o.put("disetujuiOleh", pr.getDisetujuiOleh() == null ? "" : pr.getDisetujuiOleh().getUserNama());
				o.put("alasanDitolak", pr.getAlasanDitolak() == null ? "" : pr.getAlasanDitolak());
				arr.put(o);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("total", cocok);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Detail satu PR: header + baris barang. Param: {@code id}. */
	public static void prDetail(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehLihat(tbmuser, KUNCI_PR)) {
			tolak(hasil, "Menu Pengadaan tidak diaktifkan untuk grup pengguna Anda.");
			return;
		}
		Long id = (request == null || request.isNull("id")) ? null
				: Long.valueOf((request.get("id") + "").trim());
		if (id == null) {
			tolak(hasil, "Parameter id wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			PermintaanPengadaanMasterAsset pr = (PermintaanPengadaanMasterAsset) session.get(PermintaanPengadaanMasterAsset.class, id);
			if (pr == null) {
				tolak(hasil, "Permintaan Pembelian tidak ditemukan.");
				return;
			}
			Long tokoId = tokoLingkup(tbmuser, request);
			if (tokoId != null && pr.getToko() != null && !tokoId.equals(pr.getToko().getId())) {
				tolak(hasil, "Permintaan Pembelian ini milik toko lain.");
				return;
			}
			JSONObject h = new JSONObject();
			h.put("id", pr.getId());
			h.put("kode", pr.getKode() == null ? "" : pr.getKode());
			h.put("keterangan", pr.getKeterangan() == null ? "" : pr.getKeterangan());
			h.put("tanggal", pr.getTanggalPembuatan() == null ? JSONObject.NULL
					: Common.dateFormat3.get().format(pr.getTanggalPembuatan()));
			h.put("status", statusPr(pr));
			h.put("nilai", pr.getNilai() == null ? 0 : pr.getNilai());
			h.put("tutup", Boolean.TRUE.equals(pr.getTutup()));
			h.put("alasanDitolak", pr.getAlasanDitolak() == null ? "" : pr.getAlasanDitolak());
			h.put("toko_id", pr.getToko() == null ? JSONObject.NULL : pr.getToko().getId());
			h.put("toko", pr.getToko() == null ? "" : pr.getToko().getNama());
			h.put("dibuatOleh", pr.getDibuatOleh() == null ? "" : pr.getDibuatOleh().getUserNama());
			h.put("disetujuiOleh", pr.getDisetujuiOleh() == null ? "" : pr.getDisetujuiOleh().getUserNama());
			h.put("ditolakOleh", pr.getDitolakOleh() == null ? "" : pr.getDitolakOleh().getUserNama());

			@SuppressWarnings("unchecked")
			List<PermintaanPengadaanMasterAssetDetail> baris = session
					.createCriteria(PermintaanPengadaanMasterAssetDetail.class)
					.add(Restrictions.eq("permintaanPengadaanMasterAsset.id", pr.getId()))
					.addOrder(Order.asc("id")).list();
			JSONArray arr = new JSONArray();
			for (PermintaanPengadaanMasterAssetDetail d : baris) {
				JSONObject o = new JSONObject();
				o.put("id", d.getId());
				Produk produkBaris = produkDariMasterAsset(session, d.getMasterAsset());
				o.put("produk_id", produkBaris == null ? JSONObject.NULL : produkBaris.getId());
				o.put("master_asset_id", d.getMasterAsset() == null ? JSONObject.NULL : d.getMasterAsset().getId());
				o.put("barang", d.getMasterAsset() == null ? "" : d.getMasterAsset().getNama());
				// Alias "produk": layar JSP menamai kolom ini demikian. Disediakan server agar
				// kedua penamaan sah dan nama barang tidak pernah tampil kosong.
				o.put("produk", d.getMasterAsset() == null ? "" : d.getMasterAsset().getNama());
				o.put("kodeBarang", d.getMasterAsset() == null ? "" : (d.getMasterAsset().getKode() == null ? "" : d.getMasterAsset().getKode()));
				o.put("jumlah", d.getJumlah() == null ? 0 : d.getJumlah());
				o.put("hargaBeli", d.getHargaBeli() == null ? 0 : d.getHargaBeli());
				o.put("hargaTotal", d.getHargaTotal() == null ? 0 : d.getHargaTotal());
				o.put("jumlahDatang", d.getJumlahDatang());
				o.put("keterangan", d.getKeterangan() == null ? "" : d.getKeterangan());
				arr.put(o);
			}
			hasil.put("status", "00");
			hasil.put("header", h);
			hasil.put("detail", arr);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Tambah/ubah PR beserta seluruh baris barangnya dalam SATU transaksi.
	 *
	 * <p>Baris detail memakai pola REPLACE (seluruh baris lama dihapus lalu ditulis ulang dari
	 * payload) -- sama dengan perilaku layar ZKoss, dan membuat klien cukup mengirim kondisi
	 * akhir tanpa melacak baris mana yang dihapus. PR yang SUDAH DISETUJUI atau DITUTUP tidak
	 * boleh diubah, mengikuti aturan versi umum: dokumen yang sudah menjadi komitmen tidak
	 * disunting diam-diam.</p>
	 *
	 * <p>{@code nilai} header dihitung ULANG dari baris (bukan diambil dari klien) supaya total
	 * PR tidak pernah berbeda dengan rinciannya.</p>
	 */
	public static void prSimpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Long id = (request == null || request.isNull("id") || (request.get("id") + "").trim().isEmpty())
				? null : Long.valueOf((request.get("id") + "").trim());
		if (!bolehAksi(tbmuser, KUNCI_PR, id == null ? "create" : "update")) {
			tolak(hasil, "Grup pengguna Anda tidak memiliki hak "
					+ (id == null ? "membuat" : "mengubah") + " Permintaan Pembelian.");
			return;
		}
		JSONArray detail = request == null ? null : request.optJSONArray("detail");
		if (detail == null || detail.length() == 0) {
			tolak(hasil, "Permintaan Pembelian harus memiliki minimal satu baris barang.");
			return;
		}
		Long tokoId = tokoLingkup(tbmuser, request);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			PermintaanPengadaanMasterAsset pr;
			if (id != null) {
				pr = (PermintaanPengadaanMasterAsset) session.get(PermintaanPengadaanMasterAsset.class, id);
				if (pr == null) {
					tolak(hasil, "Permintaan Pembelian tidak ditemukan.");
					return;
				}
				if (tokoId != null && pr.getToko() != null && !tokoId.equals(pr.getToko().getId())) {
					tolak(hasil, "Permintaan Pembelian ini milik toko lain.");
					return;
				}
				if (pr.getTanggalPersetujuan() != null) {
					tolak(hasil, "Permintaan Pembelian yang sudah disetujui tidak dapat diubah. "
							+ "Batalkan persetujuan terlebih dahulu bila memang perlu dikoreksi.");
					return;
				}
				if (Boolean.TRUE.equals(pr.getTutup())) {
					tolak(hasil, "Permintaan Pembelian sudah ditutup dan tidak dapat diubah.");
					return;
				}
			} else {
				pr = new PermintaanPengadaanMasterAsset();
				pr.setTanggalPembuatan(ais.ui.util.WaktuUtil.getDate());
				pr.setDibuatOleh(tbmuser);
				pr.setAktif(Boolean.TRUE);
			}
			if (tokoId != null) {
				pr.setToko((Toko) session.get(Toko.class, tokoId));
			}
			pr.setKeterangan(request.optString("keterangan", "").trim());
			if (!request.isNull("tanggal")) {
				try {
					pr.setTanggalPembuatan(Common.dateFormat3.get().parse((request.get("tanggal") + "").trim()));
				} catch (Exception e) {
					// Format tanggal tidak dikenali -> pertahankan nilai yang sudah ada.
				}
			}
			if (pr.getKode() == null || pr.getKode().trim().isEmpty()) {
				pr.setKode(buatKode(session, tokoId));
			}
			if (tbmuser != null) {
				pr.setOleh(tbmuser.getUserNama());
				pr.setOlehId(tbmuser.getUserId());
			}

			session.beginTransaction();
			session.saveOrUpdate(pr);
			session.flush();

			@SuppressWarnings("unchecked")
			List<PermintaanPengadaanMasterAssetDetail> lama = session
					.createCriteria(PermintaanPengadaanMasterAssetDetail.class)
					.add(Restrictions.eq("permintaanPengadaanMasterAsset.id", pr.getId())).list();
			for (PermintaanPengadaanMasterAssetDetail d : lama) {
				session.delete(d);
			}
			session.flush();

			double total = 0;
			for (int i = 0; i < detail.length(); i++) {
				JSONObject b = detail.getJSONObject(i);
				MasterAsset barang = barangBaris(session, b);
				if (barang == null) {
					continue;
				}
				double jumlah = angkaAman(b, "jumlah");
				double harga = angkaAman(b, "hargaBeli");
				double sub = jumlah * harga;
				PermintaanPengadaanMasterAssetDetail d = new PermintaanPengadaanMasterAssetDetail();
				d.setPermintaanPengadaanMasterAsset(pr);
				d.setMasterAsset(barang);
				d.setJumlah(Double.valueOf(jumlah));
				d.setHargaBeli(Double.valueOf(harga));
				d.setHargaTotal(Double.valueOf(sub));
				d.setKeterangan(b.optString("keterangan", "").trim());
				if (tbmuser != null) {
					d.setOleh(tbmuser.getUserNama());
					d.setOlehId(tbmuser.getUserId());
				}
				session.save(d);
				total += sub;
			}
			pr.setNilai(Double.valueOf(total));
			session.saveOrUpdate(pr);
			session.getTransaction().commit();

			hasil.put("status", "00");
			hasil.put("id", pr.getId());
			hasil.put("kode", pr.getKode());
			hasil.put("nilai", total);
		} catch (Exception e) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception eRollback) {
				ais.common.ErrorAuditUtil.record(eRollback, "PengadaanPosApiHelper.prSimpan rollback");
			}
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Keputusan atas PR: setujui / tolak / batalkan keputusan -- padanan tombol persetujuan di
	 * layar ZKoss. Param: {@code id}, {@code keputusan} (SETUJUI|TOLAK|BATAL), {@code alasan}
	 * (wajib bila menolak). Butuh aksi granular {@code approve}.
	 */
	public static void prPutusan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehAksi(tbmuser, KUNCI_PR, "approve")) {
			tolak(hasil, "Grup pengguna Anda tidak memiliki hak menyetujui atau menolak Permintaan Pembelian.");
			return;
		}
		Long id = (request == null || request.isNull("id")) ? null
				: Long.valueOf((request.get("id") + "").trim());
		String keputusan = request == null ? "" : request.optString("keputusan", "").trim().toUpperCase();
		if (id == null || keputusan.isEmpty()) {
			tolak(hasil, "Parameter id dan keputusan wajib diisi.");
			return;
		}
		String alasan = request.optString("alasan", "").trim();
		if ("TOLAK".equals(keputusan) && alasan.length() < 5) {
			tolak(hasil, "Alasan penolakan wajib diisi, minimal 5 karakter, agar pembuat PR tahu langkah perbaikannya.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			PermintaanPengadaanMasterAsset pr = (PermintaanPengadaanMasterAsset) session.get(PermintaanPengadaanMasterAsset.class, id);
			if (pr == null) {
				tolak(hasil, "Permintaan Pembelian tidak ditemukan.");
				return;
			}
			Long tokoId = tokoLingkup(tbmuser, request);
			if (tokoId != null && pr.getToko() != null && !tokoId.equals(pr.getToko().getId())) {
				tolak(hasil, "Permintaan Pembelian ini milik toko lain.");
				return;
			}
			if (Boolean.TRUE.equals(pr.getTutup())) {
				tolak(hasil, "Permintaan Pembelian sudah ditutup; keputusan tidak dapat diubah.");
				return;
			}
			session.beginTransaction();
			if ("SETUJUI".equals(keputusan)) {
				pr.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());
				pr.setDisetujuiOleh(tbmuser);
				pr.setTanggalDitolak(null);
				pr.setDitolakOleh(null);
				pr.setAlasanDitolak(null);
			} else if ("TOLAK".equals(keputusan)) {
				pr.setTanggalDitolak(ais.ui.util.WaktuUtil.getDate());
				pr.setDitolakOleh(tbmuser);
				pr.setAlasanDitolak(alasan);
				pr.setTanggalPersetujuan(null);
				pr.setDisetujuiOleh(null);
			} else if ("BATAL".equals(keputusan)) {
				// Padanan reset keputusan di ZKoss (mengosongkan persetujuan & penolakan).
				pr.setTanggalPersetujuan(null);
				pr.setDisetujuiOleh(null);
				pr.setTanggalDitolak(null);
				pr.setDitolakOleh(null);
				pr.setAlasanDitolak(null);
			} else {
				session.getTransaction().rollback();
				tolak(hasil, "Keputusan tidak dikenali. Gunakan SETUJUI, TOLAK, atau BATAL.");
				return;
			}
			if (tbmuser != null) {
				pr.setOleh(tbmuser.getUserNama());
				pr.setOlehId(tbmuser.getUserId());
			}
			session.saveOrUpdate(pr);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("id", pr.getId());
			hasil.put("statusPr", statusPr(pr));
		} catch (Exception e) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception eRollback) {
				ais.common.ErrorAuditUtil.record(eRollback, "PengadaanPosApiHelper.prPutusan rollback");
			}
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Hapus PR -- hanya yang masih DRAFT. PR yang sudah disetujui/ditolak/ditutup adalah jejak
	 * keputusan dan TIDAK dihapus (pola referential-guard sama dengan master lain).
	 */
	public static void prHapus(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehAksi(tbmuser, KUNCI_PR, "delete")) {
			tolak(hasil, "Grup pengguna Anda tidak memiliki hak menghapus Permintaan Pembelian.");
			return;
		}
		Long id = (request == null || request.isNull("id")) ? null
				: Long.valueOf((request.get("id") + "").trim());
		if (id == null) {
			tolak(hasil, "Parameter id wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			PermintaanPengadaanMasterAsset pr = (PermintaanPengadaanMasterAsset) session.get(PermintaanPengadaanMasterAsset.class, id);
			if (pr == null) {
				tolak(hasil, "Permintaan Pembelian tidak ditemukan.");
				return;
			}
			Long tokoId = tokoLingkup(tbmuser, request);
			if (tokoId != null && pr.getToko() != null && !tokoId.equals(pr.getToko().getId())) {
				tolak(hasil, "Permintaan Pembelian ini milik toko lain.");
				return;
			}
			if (!"DRAFT".equals(statusPr(pr))) {
				tolak(hasil, "Hanya Permintaan Pembelian berstatus DRAFT yang dapat dihapus. "
						+ "Dokumen yang sudah disetujui/ditolak disimpan sebagai jejak keputusan.");
				return;
			}
			session.beginTransaction();
			@SuppressWarnings("unchecked")
			List<PermintaanPengadaanMasterAssetDetail> baris = session
					.createCriteria(PermintaanPengadaanMasterAssetDetail.class)
					.add(Restrictions.eq("permintaanPengadaanMasterAsset.id", pr.getId())).list();
			for (PermintaanPengadaanMasterAssetDetail d : baris) {
				session.delete(d);
			}
			session.delete(pr);
			session.getTransaction().commit();
			hasil.put("status", "00");
		} catch (Exception e) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception eRollback) {
				ais.common.ErrorAuditUtil.record(eRollback, "PengadaanPosApiHelper.prHapus rollback");
			}
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}


	/**
	 * Pencarian barang (MasterAsset) untuk mengisi baris PR. Param: {@code keyword} (kode/nama),
	 * {@code limit} (maks 100). Sengaja memakai MasterAsset -- bukan katalog produk POS --
	 * karena sejak 2026-08-20 modul ini memakai TABEL PENGADAAN BERSAMA dengan JSP/ZKoss,
	 * dibedakan lewat kolom toko. Pemetaan barang ke produk POS baru diperlukan pada tahap
	 * sinkronisasi BAST ke Kulakan.
	 */
	public static void cariBarang(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		// Pencarian barang dipakai layar PR, PO, maupun BAST, jadi cukup salah satu menu aktif.
		if (!bolehLihat(tbmuser, KUNCI_PR) && !bolehLihat(tbmuser, KUNCI_PO)
				&& !bolehLihat(tbmuser, KUNCI_BAST)) {
			tolak(hasil, "Menu Pengadaan tidak diaktifkan untuk grup pengguna Anda.");
			return;
		}
		String q = request == null ? "" : request.optString("keyword", "").trim();
		int limit = Math.min(200, Math.max(5, request == null ? 50 : request.optInt("limit", 50)));
		Long tokoId = tokoLingkup(tbmuser, request);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			// Pengguna toko memilih dari PRODUK POS; padanan MasterAsset dibuat/ditautkan
			// server saat dokumen disimpan (lihat masterAssetUntukProduk).
			Criteria kriteria = session.createCriteria(Produk.class);
			kriteria.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)));
			if (tokoId != null) {
				kriteria.add(Restrictions.eq("toko.id", tokoId));
			}
			if (q.length() > 0) {
				kriteria.add(Restrictions.disjunction()
						.add(Restrictions.ilike("kode", q, MatchMode.ANYWHERE))
						.add(Restrictions.ilike("nama", q, MatchMode.ANYWHERE)));
			}
			kriteria.addOrder(Order.asc("nama"));
			kriteria.setMaxResults(limit);
			@SuppressWarnings("unchecked")
			List<Produk> daftar = kriteria.list();
			JSONArray arr = new JSONArray();
			for (Produk pr : daftar) {
				JSONObject o = new JSONObject();
				o.put("produk_id", pr.getId());
				o.put("id", pr.getId());
				o.put("kode", pr.getKode() == null ? "" : pr.getKode());
				o.put("nama", pr.getNama() == null ? "" : pr.getNama());
				o.put("hargaBeli", pr.getHargaBeli() == null ? 0 : pr.getHargaBeli());
				o.put("master_asset_id", pr.getMasterAsset() == null ? JSONObject.NULL
						: pr.getMasterAsset().getId());
				arr.put(o);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Nomor dokumen otomatis {@code <JENIS>/<toko>/<yyyyMM>/<urut>}, urut per toko per bulan.
	 * Dicoba beberapa kali bila bentrok (aman bila dua pengguna menyimpan bersamaan).
	 */
	private static String buatKodeUmum(Session session, Class<?> kelas, String jenis, Long tokoId) {
		java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyyMM");
		String periode = fmt.format(ais.ui.util.WaktuUtil.getDate());
		String prefiks = jenis + "/" + (tokoId == null ? "0" : tokoId) + "/" + periode + "/";
		for (int percobaan = 0; percobaan < 50; percobaan++) {
			Number jml = (Number) session.createCriteria(kelas)
					.setProjection(Projections.rowCount())
					.add(Restrictions.ilike("kode", prefiks, MatchMode.START)).uniqueResult();
			long urut = (jml == null ? 0 : jml.longValue()) + 1 + percobaan;
			String kandidat = prefiks + (urut < 10 ? "000" : urut < 100 ? "00" : urut < 1000 ? "0" : "") + urut;
			Number bentrok = (Number) session.createCriteria(kelas)
					.setProjection(Projections.rowCount())
					.add(Restrictions.eq("kode", kandidat)).uniqueResult();
			if (bentrok == null || bentrok.intValue() == 0) {
				return kandidat;
			}
		}
		return prefiks + System.currentTimeMillis();
	}

	/**
	 * Label status baris PO. Berbeda dengan PR, PO punya dimensi pembayaran: kolom
	 * {@code dibayar}/{@code lunas} diisi oleh modul pembayaran vendor. Status pembayaran
	 * diutamakan supaya pengguna langsung melihat kondisi terkini dokumen.
	 *
	 * <p>Pembayaran SEBAGIAN sengaja TIDAK dijadikan status tersendiri agar daftar status tetap
	 * sedikit dan dapat difilter; besaran terbayar dikirim terpisah lewat {@code dibayar} dan
	 * {@code sisa} sehingga layar tetap dapat menampilkannya.</p>
	 */
	private static String statusPo(PemesananPengadaanMasterAsset po) {
		double nilai = po.getNilai() == null ? 0 : po.getNilai().doubleValue();
		double dibayar = po.getDibayar() == null ? 0 : po.getDibayar().doubleValue();
		if (Boolean.TRUE.equals(po.getLunas()) || (nilai > 0 && dibayar >= nilai - TOLERANSI)) {
			return "LUNAS";
		}
		if (po.getTanggalDitolak() != null) {
			return "DITOLAK";
		}
		if (po.getTanggalPersetujuan() != null) {
			return "DISETUJUI";
		}
		return "DRAFT";
	}

	/** Selisih rupiah yang masih dianggap sama (menyerap pembulatan sen pada pembagian termin). */
	private static final double TOLERANSI = 1.0;

	/**
	 * Jadwal termin PO tersimpan sebagai JSON pada kolom {@code formula}.
	 *
	 * <p>Kolom ini dipakai BERSAMA layar ZKoss dan modul pembayaran vendor -- modul pembayaran
	 * menuliskan kembali kunci tambahan pada item yang sama (mis. {@code pajak} per termin).
	 * Karena itu pembacaan dibuat toleran terhadap isi yang tidak dikenal, dan penulisan WAJIB
	 * lewat {@link #gabungTermin} yang mempertahankan kunci milik modul lain.</p>
	 */
	private static JSONArray terminDari(PemesananPengadaanMasterAsset po) {
		if (po == null || po.getFormula() == null || po.getFormula().trim().isEmpty()) {
			return new JSONArray();
		}
		try {
			return new JSONArray(po.getFormula());
		} catch (Exception e) {
			// Isi lama tidak terbaca -> perlakukan sebagai belum ada jadwal, jangan gagalkan layar.
			return new JSONArray();
		}
	}

	/** Cari item termin lama berdasarkan {@code key} supaya kunci milik modul lain tidak hilang. */
	private static JSONObject terminLamaBerkunci(JSONArray lama, String key) throws Exception {
		if (lama == null || key == null || key.trim().isEmpty()) {
			return null;
		}
		for (int i = 0; i < lama.length(); i++) {
			JSONObject o = lama.optJSONObject(i);
			if (o != null && !o.isNull("key") && key.equals(o.get("key") + "")) {
				return o;
			}
		}
		return null;
	}

	/**
	 * Susun ulang jadwal termin dari payload klien.
	 *
	 * <p>Setiap item hasil adalah SALINAN item lama dengan kunci yang sama (bila ada), lalu
	 * kunci milik POS ditimpa. Dengan begitu data yang ditulis modul pembayaran vendor
	 * -- yang memakai kolom {@code formula} yang sama -- tidak terhapus saat PO disunting
	 * dari kasir. Tanggal ditulis memakai pola {@code dd-MM-yyyy}, sama dengan yang dibaca
	 * layar ZKoss, supaya satu dokumen tetap terbaca di kedua versi.</p>
	 */
	private static JSONArray gabungTermin(JSONArray payload, JSONArray lama) throws Exception {
		JSONArray hasil = new JSONArray();
		if (payload == null) {
			return hasil;
		}
		for (int i = 0; i < payload.length(); i++) {
			JSONObject src = payload.optJSONObject(i);
			if (src == null) {
				continue;
			}
			String key = src.isNull("key") ? "" : (src.get("key") + "").trim();
			JSONObject dasar = terminLamaBerkunci(lama, key);
			JSONObject item = new JSONObject();
			if (dasar != null) {
				java.util.Iterator<String> it = dasar.keys();
				while (it.hasNext()) {
					String k = it.next();
					item.put(k, dasar.get(k));
				}
			}
			if (key.isEmpty()) {
				key = Math.abs(Common.randLong()) + "";
			}
			item.put("key", key);
			item.put("nomor", (i + 1) + "");
			String nama = src.optString("nama", "").trim();
			item.put("nama", nama.isEmpty() ? "Termin " + (i + 1) : nama);
			item.put("penagihan", angkaAman(src, "penagihan"));
			if (!src.isNull("pekerjaan")) {
				item.put("pekerjaan", angkaAman(src, "pekerjaan"));
			} else if (item.isNull("pekerjaan")) {
				item.put("pekerjaan", 0.0);
			}
			String tgl = src.optString("tanggalD", "").trim();
			if (!tgl.isEmpty()) {
				// Divalidasi KETAT supaya kolom formula tidak pernah berisi tanggal ngawur.
				java.util.Date jatuhTempo = tanggalKetat(tgl);
				if (jatuhTempo == null) {
					throw new IllegalArgumentException("Tanggal jatuh tempo termin \"" + tgl
							+ "\" harus berformat hh-bb-tttt, mis. 31-12-2026.");
				}
				item.put("tanggalD", Common.dateFormat1.get().format(jatuhTempo));
			}
			hasil.put(item);
		}
		return hasil;
	}

	/**
	 * Baca tanggal berpola {@code dd-MM-yyyy} secara KETAT.
	 *
	 * <p>{@code Common.dateFormat1} bersifat lenient: "2026-08-10" ikut terbaca dan diam-diam
	 * berubah menjadi tanggal lain (hari 2026, bulan 08, tahun 10 digulung). Untuk dokumen
	 * pengadaan hal itu berbahaya karena tanggal faktur dan batas kirim dipakai menghitung
	 * jatuh tempo. Karena itu hasil parse ditulis ulang dan dibandingkan dengan masukan;
	 * bila tidak sama persis, masukan ditolak.</p>
	 *
	 * @return tanggal hasil parse, atau {@code null} bila masukan tidak berpola benar.
	 */
	private static java.util.Date tanggalKetat(String teks) {
		if (teks == null || teks.trim().isEmpty()) {
			return null;
		}
		String rapi = teks.trim();
		try {
			java.util.Date tgl = Common.dateFormat1.get().parse(rapi);
			return rapi.equals(Common.dateFormat1.get().format(tgl)) ? tgl : null;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Baca angka dari payload klien dengan aman.
	 *
	 * <p>String kosong dari kolom input yang belum diisi pernah menjatuhkan transaksi produksi
	 * (NumberFormatException), jadi di sini kosong, tanda hubung, atau teks bukan angka diperlakukan sebagai 0
	 * alih-alih melempar galat.</p>
	 */
	private static double angkaAman(JSONObject o, String kunci) throws Exception {
		if (o == null || o.isNull(kunci)) {
			return 0;
		}
		String s = (o.get(kunci) + "").trim().replace(",", "");
		if (s.isEmpty()) {
			return 0;
		}
		try {
			return Double.parseDouble(s);
		} catch (Exception e) {
			return 0;
		}
	}

	/**
	 * Daftar PO pada lingkup toko pemanggil. Param opsional: {@code cari} (kode/keterangan/
	 * nomor invoice), {@code status} (DRAFT/DISETUJUI/DITOLAK/LUNAS), {@code page},
	 * {@code pageSize}.
	 */
	public static void poDaftar(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehLihat(tbmuser, KUNCI_PO)) {
			tolak(hasil, "Menu Pengadaan tidak diaktifkan untuk grup pengguna Anda.");
			return;
		}
		Long tokoId = tokoLingkup(tbmuser, request);
		int page = Math.max(1, request == null ? 1 : request.optInt("page", 1));
		int pageSize = Math.min(100, Math.max(5, request == null ? 15 : request.optInt("pageSize", 15)));
		String cari = request == null ? "" : request.optString("cari", "").trim();
		String status = request == null ? "" : request.optString("status", "").trim().toUpperCase();
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Criteria kriteria = session.createCriteria(PemesananPengadaanMasterAsset.class);
			kriteria.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)));
			if (tokoId != null) {
				kriteria.add(Restrictions.eq("toko.id", tokoId));
			}
			if (cari.length() > 0) {
				// Restrictions.or pada Hibernate versi ini hanya menerima dua kriteria,
				// jadi tiga kolom pencarian disusun sebagai disjunction.
				kriteria.add(Restrictions.disjunction()
						.add(Restrictions.ilike("kode", cari, MatchMode.ANYWHERE))
						.add(Restrictions.ilike("keterangan", cari, MatchMode.ANYWHERE))
						.add(Restrictions.ilike("kodeInvoice", cari, MatchMode.ANYWHERE)));
			}
			// Status disaring di Java karena merupakan turunan tanggal persetujuan/penolakan dan
			// kondisi pelunasan -- satu definisi saja, dipakai bersama lewat statusPo().
			kriteria.addOrder(Order.desc("id"));
			@SuppressWarnings("unchecked")
			List<PemesananPengadaanMasterAsset> semua = kriteria.list();
			JSONArray arr = new JSONArray();
			int cocok = 0;
			int mulai = (page - 1) * pageSize;
			for (PemesananPengadaanMasterAsset po : semua) {
				String st = statusPo(po);
				if (status.length() > 0 && !status.equals(st)) {
					continue;
				}
				cocok++;
				if (cocok <= mulai || arr.length() >= pageSize) {
					continue;
				}
				double nilai = po.getNilai() == null ? 0 : po.getNilai().doubleValue();
				double dibayar = po.getDibayar() == null ? 0 : po.getDibayar().doubleValue();
				JSONObject o = new JSONObject();
				o.put("id", po.getId());
				o.put("kode", po.getKode() == null ? "" : po.getKode());
				o.put("kodeInvoice", po.getKodeInvoice() == null ? "" : po.getKodeInvoice());
				o.put("keterangan", po.getKeterangan() == null ? "" : po.getKeterangan());
				o.put("tanggal", po.getTanggalPembuatan() == null ? JSONObject.NULL
						: Common.dateFormat3.get().format(po.getTanggalPembuatan()));
				o.put("penyedia", po.getPenyedia() == null ? "" : po.getPenyedia().getNama());
				o.put("nilai", nilai);
				o.put("dibayar", dibayar);
				o.put("sisa", Math.max(0, nilai - dibayar));
				o.put("byTermin", Boolean.TRUE.equals(po.getByTermin()));
				o.put("jumlahTermin", terminDari(po).length());
				o.put("status", st);
				o.put("toko", po.getToko() == null ? "" : po.getToko().getNama());
				o.put("dibuatOleh", po.getDibuatOleh() == null ? "" : po.getDibuatOleh().getUserNama());
				o.put("disetujuiOleh", po.getDisetujuiOleh() == null ? "" : po.getDisetujuiOleh().getUserNama());
				o.put("alasanDitolak", po.getAlasanDitolak() == null ? "" : po.getAlasanDitolak());
				arr.put(o);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("total", cocok);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Detail satu PO: header, baris barang, dan jadwal termin.
	 *
	 * <p>Nilai terbayar per termin dijumlahkan dari dokumen pembayaran vendor yang menunjuk PO
	 * ini; item termin dikenali lewat kunci {@code key} pada kolom {@code tagihan} baris
	 * pembayaran -- cara pengenalan yang sama dipakai layar ZKoss.</p>
	 */
	public static void poDetail(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehLihat(tbmuser, KUNCI_PO)) {
			tolak(hasil, "Menu Pengadaan tidak diaktifkan untuk grup pengguna Anda.");
			return;
		}
		Long id = (request == null || request.isNull("id")) ? null
				: Long.valueOf((request.get("id") + "").trim());
		if (id == null) {
			tolak(hasil, "Parameter id wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			PemesananPengadaanMasterAsset po = (PemesananPengadaanMasterAsset) session
					.get(PemesananPengadaanMasterAsset.class, id);
			if (po == null) {
				tolak(hasil, "Pemesanan Pembelian tidak ditemukan.");
				return;
			}
			Long tokoId = tokoLingkup(tbmuser, request);
			if (tokoId != null && po.getToko() != null && !tokoId.equals(po.getToko().getId())) {
				tolak(hasil, "Pemesanan Pembelian ini milik toko lain.");
				return;
			}
			double nilai = po.getNilai() == null ? 0 : po.getNilai().doubleValue();
			double dibayar = po.getDibayar() == null ? 0 : po.getDibayar().doubleValue();
			JSONObject h = new JSONObject();
			h.put("id", po.getId());
			h.put("kode", po.getKode() == null ? "" : po.getKode());
			h.put("kodeInvoice", po.getKodeInvoice() == null ? "" : po.getKodeInvoice());
			h.put("keterangan", po.getKeterangan() == null ? "" : po.getKeterangan());
			h.put("catatanKesepakatan", po.getCatatanKesepakatan() == null ? "" : po.getCatatanKesepakatan());
			h.put("tanggal", po.getTanggalPembuatan() == null ? JSONObject.NULL
					: Common.dateFormat3.get().format(po.getTanggalPembuatan()));
			h.put("pengirimanPalingLambat", po.getPengirimanPalingLambat() == null ? ""
					: Common.dateFormat1.get().format(po.getPengirimanPalingLambat()));
			h.put("status", statusPo(po));
			h.put("nilai", nilai);
			h.put("dp", po.getDp() == null ? 0 : po.getDp().doubleValue());
			h.put("dibayar", dibayar);
			h.put("sisa", Math.max(0, nilai - dibayar));
			h.put("lunas", Boolean.TRUE.equals(po.getLunas()));
			h.put("byTermin", Boolean.TRUE.equals(po.getByTermin()));
			h.put("alasanDitolak", po.getAlasanDitolak() == null ? "" : po.getAlasanDitolak());
			h.put("penyedia_id", po.getPenyedia() == null ? JSONObject.NULL : po.getPenyedia().getId());
			h.put("penyedia", po.getPenyedia() == null ? "" : po.getPenyedia().getNama());
			h.put("toko_id", po.getToko() == null ? JSONObject.NULL : po.getToko().getId());
			h.put("toko", po.getToko() == null ? "" : po.getToko().getNama());
			h.put("permintaan", po.getPermintaanPengadaanMasterAssets() == null ? ""
					: po.getPermintaanPengadaanMasterAssets());
			h.put("dibuatOleh", po.getDibuatOleh() == null ? "" : po.getDibuatOleh().getUserNama());
			h.put("disetujuiOleh", po.getDisetujuiOleh() == null ? "" : po.getDisetujuiOleh().getUserNama());
			h.put("ditolakOleh", po.getDitolakOleh() == null ? "" : po.getDitolakOleh().getUserNama());

			@SuppressWarnings("unchecked")
			List<PemesananPengadaanMasterAssetDetail> baris = session
					.createCriteria(PemesananPengadaanMasterAssetDetail.class)
					.add(Restrictions.eq("pemesananPengadaanMasterAsset.id", po.getId()))
					.addOrder(Order.asc("id")).list();
			JSONArray arr = new JSONArray();
			for (PemesananPengadaanMasterAssetDetail d : baris) {
				JSONObject o = new JSONObject();
				o.put("id", d.getId());
				Produk produkBaris = produkDariMasterAsset(session, d.getMasterAsset());
				o.put("produk_id", produkBaris == null ? JSONObject.NULL : produkBaris.getId());
				o.put("master_asset_id", d.getMasterAsset() == null ? JSONObject.NULL : d.getMasterAsset().getId());
				o.put("barang", d.getMasterAsset() == null ? "" : d.getMasterAsset().getNama());
				// Alias "produk": layar JSP menamai kolom ini demikian. Disediakan server agar
				// kedua penamaan sah dan nama barang tidak pernah tampil kosong.
				o.put("produk", d.getMasterAsset() == null ? "" : d.getMasterAsset().getNama());
				o.put("kodeBarang", d.getMasterAsset() == null || d.getMasterAsset().getKode() == null ? ""
						: d.getMasterAsset().getKode());
				o.put("jumlah", d.getJumlah() == null ? 0 : d.getJumlah());
				o.put("hargaBeli", d.getHargaBeli() == null ? 0 : d.getHargaBeli());
				o.put("hargaTotal", d.getHargaTotal() == null ? 0 : d.getHargaTotal());
				o.put("keterangan", d.getKeterangan() == null ? "" : d.getKeterangan());
				o.put("pr_detail_id", d.getPermintaanPengadaanMasterAssetDetail() == null ? JSONObject.NULL
						: d.getPermintaanPengadaanMasterAssetDetail().getId());
				arr.put(o);
			}
			hasil.put("status", "00");
			hasil.put("header", h);
			hasil.put("detail", arr);
			hasil.put("termin", terminUntukKlien(session, po));
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Susun jadwal termin untuk klien, lengkap dengan nilai yang sudah terbayar per termin.
	 *
	 * <p>Terbayar dihitung dari baris pembayaran vendor milik PO ini. Baris yang dokumen
	 * induknya dinonaktifkan tidak ikut dihitung supaya angka di layar sama dengan angka yang
	 * diakui pembukuan.</p>
	 */
	private static JSONArray terminUntukKlien(Session session, PemesananPengadaanMasterAsset po) throws Exception {
		JSONArray sumber = terminDari(po);
		java.util.Map<String, Double> terbayar = new java.util.HashMap<String, Double>();
		try {
			@SuppressWarnings("unchecked")
			List<PembayaranTerminMasterAssetDetail> bayar = session
					.createCriteria(PembayaranTerminMasterAssetDetail.class)
					.add(Restrictions.eq("pemesananPengadaanMasterAsset.id", po.getId())).list();
			for (PembayaranTerminMasterAssetDetail b : bayar) {
				// HANYA pembayaran yang SUDAH DISETUJUI yang diakui -- definisi yang sama
				// dipakai PemesananPengadaanMasterAsset.hitungDibayar(), sehingga angka di
				// layar termin tidak pernah berbeda dengan kolom "dibayar" pada PO.
				if (b.getPembayaranTerminMasterAsset() == null
						|| b.getPembayaranTerminMasterAsset().getDisetujuiOleh() == null
						|| Boolean.FALSE.equals(b.getPembayaranTerminMasterAsset().getAktif())) {
					continue;
				}
				String kunci = "";
				if (b.getTagihan() != null && !b.getTagihan().trim().isEmpty()) {
					try {
						JSONObject t = new JSONObject(b.getTagihan());
						kunci = t.isNull("key") ? "" : (t.get("key") + "").trim();
					} catch (Exception e) {
						kunci = "";
					}
				}
				double n = b.getDibayar() == null ? 0 : b.getDibayar().doubleValue();
				Double sebelum = terbayar.get(kunci);
				terbayar.put(kunci, (sebelum == null ? 0 : sebelum.doubleValue()) + n);
			}
		} catch (Exception e) {
			// Kegagalan membaca pembayaran tidak boleh menutup layar PO; jadwal tetap tampil.
			ais.common.ErrorAuditUtil.record(e, "PengadaanPosApiHelper.terminUntukKlien");
		}
		JSONArray arr = new JSONArray();
		for (int i = 0; i < sumber.length(); i++) {
			JSONObject src = sumber.optJSONObject(i);
			if (src == null) {
				continue;
			}
			String kunci = src.isNull("key") ? "" : (src.get("key") + "").trim();
			double tagih = angkaAman(src, "penagihan");
			Double sudah = terbayar.get(kunci);
			double bayar = sudah == null ? 0 : sudah.doubleValue();
			JSONObject o = new JSONObject();
			o.put("key", kunci);
			o.put("nomor", src.isNull("nomor") ? (i + 1) + "" : src.get("nomor") + "");
			o.put("nama", src.isNull("nama") ? "" : src.get("nama") + "");
			o.put("penagihan", tagih);
			o.put("pekerjaan", angkaAman(src, "pekerjaan"));
			o.put("tanggalD", src.isNull("tanggalD") ? "" : src.get("tanggalD") + "");
			o.put("dibayar", bayar);
			o.put("sisa", Math.max(0, tagih - bayar));
			o.put("lunas", tagih > 0 && bayar >= tagih - TOLERANSI);
			arr.put(o);
		}
		return arr;
	}

	/**
	 * Tambah/ubah PO beserta baris barang dan jadwal terminnya dalam SATU transaksi.
	 *
	 * <p>Aturan yang ditegakkan server (bukan layar), supaya keempat kanal berperilaku sama:</p>
	 * <ul>
	 * <li>{@code nilai} header dihitung ULANG dari baris, tidak pernah diambil dari klien.</li>
	 * <li>PO yang sudah DISETUJUI atau sudah menerima pembayaran tidak dapat diubah -- dokumen
	 * yang sudah menjadi komitmen kepada vendor tidak boleh disunting diam-diam.</li>
	 * <li>Bila memakai termin, jumlah seluruh penagihan harus sama dengan nilai PO. Jadwal yang
	 * tidak menutup nilai PO adalah kesalahan input yang baru ketahuan saat menagih, jadi ditolak
	 * sejak awal dengan pesan yang menyebutkan kedua angkanya.</li>
	 * <li>DP dan termin saling meniadakan, mengikuti model pengadaan yang sudah ada -- uang muka
	 * pada PO bertermin ditulis sebagai termin pertama.</li>
	 * </ul>
	 */
	public static void poSimpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Long id = (request == null || request.isNull("id") || (request.get("id") + "").trim().isEmpty())
				? null : Long.valueOf((request.get("id") + "").trim());
		if (!bolehAksi(tbmuser, KUNCI_PO, id == null ? "create" : "update")) {
			tolak(hasil, "Grup pengguna Anda tidak memiliki hak "
					+ (id == null ? "membuat" : "mengubah") + " Pemesanan Pembelian.");
			return;
		}
		if (tbmuser == null) {
			tolak(hasil, "Sesi pengguna tidak dikenali, silakan masuk ulang.");
			return;
		}
		JSONArray detail = request == null ? null : request.optJSONArray("detail");
		if (detail == null || detail.length() == 0) {
			tolak(hasil, "Pemesanan Pembelian harus memiliki minimal satu baris barang.");
			return;
		}
		boolean adaPenyedia = (!request.isNull("penyedia_id") && !(request.get("penyedia_id") + "").trim().isEmpty())
				|| (!request.isNull("penyedia_asset_id") && !(request.get("penyedia_asset_id") + "").trim().isEmpty());
		if (!adaPenyedia) {
			tolak(hasil, "Penyedia/vendor wajib dipilih pada Pemesanan Pembelian.");
			return;
		}
		boolean byTermin = request.optBoolean("byTermin", false);
		JSONArray terminPayload = request.optJSONArray("termin");
		Long tokoId = tokoLingkup(tbmuser, request);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			PemesananPengadaanMasterAsset po;
			if (id != null) {
				po = (PemesananPengadaanMasterAsset) session.get(PemesananPengadaanMasterAsset.class, id);
				if (po == null) {
					tolak(hasil, "Pemesanan Pembelian tidak ditemukan.");
					return;
				}
				if (tokoId != null && po.getToko() != null && !tokoId.equals(po.getToko().getId())) {
					tolak(hasil, "Pemesanan Pembelian ini milik toko lain.");
					return;
				}
				if (po.getTanggalPersetujuan() != null) {
					tolak(hasil, "Pemesanan Pembelian yang sudah disetujui tidak dapat diubah. "
							+ "Batalkan persetujuan terlebih dahulu bila memang perlu dikoreksi.");
					return;
				}
				if (po.getDibayar() != null && po.getDibayar().doubleValue() > 0) {
					tolak(hasil, "Pemesanan Pembelian ini sudah menerima pembayaran sebesar "
							+ Common.numberFormat.get().format(po.getDibayar())
							+ " sehingga tidak dapat diubah.");
					return;
				}
			} else {
				po = new PemesananPengadaanMasterAsset();
				po.setTanggalPembuatan(ais.ui.util.WaktuUtil.getDate());
				po.setDibuatOleh(tbmuser);
				po.setAktif(Boolean.TRUE);
				po.setDibayar(Double.valueOf(0));
				po.setLunas(Boolean.FALSE);
			}
			PenyediaAsset penyedia = penyediaDokumen(session, request);
			if (penyedia == null) {
				tolak(hasil, "Penyedia/vendor tidak ditemukan.");
				return;
			}
			po.setPenyedia(penyedia);
			if (tokoId != null) {
				po.setToko((Toko) session.get(Toko.class, tokoId));
			}
			po.setKeterangan(request.optString("keterangan", "").trim());
			po.setKodeInvoice(request.optString("kodeInvoice", "").trim());
			po.setCatatanKesepakatan(request.optString("catatanKesepakatan", "").trim());
			po.setDp(Double.valueOf(angkaAman(request, "dp")));
			po.setByTermin(Boolean.valueOf(byTermin));
			if (!request.isNull("tanggal")) {
				try {
					po.setTanggalPembuatan(Common.dateFormat3.get().parse((request.get("tanggal") + "").trim()));
				} catch (Exception e) {
					// Format tanggal tidak dikenali -> pertahankan nilai yang sudah ada.
				}
			}
			String kirim = request.optString("pengirimanPalingLambat", "").trim();
			if (kirim.isEmpty()) {
				po.setPengirimanPalingLambat(null);
			} else {
				java.util.Date parsed = tanggalKetat(kirim);
				if (parsed == null) {
					tolak(hasil, "Tanggal pengiriman paling lambat harus berformat hh-bb-tttt, mis. 31-12-2026.");
					return;
				}
				po.setPengirimanPalingLambat(parsed);
			}
			if (po.getKode() == null || po.getKode().trim().isEmpty()) {
				po.setKode(buatKodeUmum(session, PemesananPengadaanMasterAsset.class, "PO", tokoId));
			}
			po.setOleh(tbmuser.getUserNama());
			po.setOlehId(tbmuser.getUserId());

			// Nilai dihitung dulu dari payload supaya jadwal termin dapat divalidasi SEBELUM
			// satu baris pun ditulis -- PO yang gagal validasi tidak boleh meninggalkan jejak.
			double totalRencana = 0;
			for (int i = 0; i < detail.length(); i++) {
				JSONObject b = detail.optJSONObject(i);
				if (b == null || (b.isNull("master_asset_id") && b.isNull("produk_id"))) {
					continue;
				}
				totalRencana += angkaAman(b, "jumlah") * angkaAman(b, "hargaBeli");
			}
			// Pada model pengadaan yang sudah ada, DP dan termin adalah dua cara bayar yang
			// SALING MENIADAKAN: PemesananPengadaanMasterAsset.getDp() memaksa DP nol begitu
			// byTermin menyala. Aturan itu ditegakkan di sini juga -- bukan didiamkan -- supaya
			// pengguna tahu uang mukanya harus ditulis sebagai termin pertama, bukan hilang.
			double dpDiminta = angkaAman(request, "dp");
			if (byTermin && dpDiminta > 0) {
				tolak(hasil, "Pemesanan Pembelian bertermin tidak memakai uang muka (DP) terpisah. "
						+ "Tuliskan uang muka sebagai termin pertama.");
				return;
			}
			if (byTermin) {
				po.setDp(Double.valueOf(0));
			}
			double dp = byTermin ? 0 : (po.getDp() == null ? 0 : po.getDp().doubleValue());
			if (dpDiminta < 0) {
				tolak(hasil, "Uang muka (DP) tidak boleh bernilai negatif.");
				return;
			}
			if (dp > totalRencana + TOLERANSI) {
				tolak(hasil, "Uang muka (DP) " + Common.numberFormat.get().format(dp)
						+ " melebihi nilai Pemesanan Pembelian " + Common.numberFormat.get().format(totalRencana) + ".");
				return;
			}
			JSONArray terminBaru = new JSONArray();
			if (byTermin) {
				if (terminPayload == null || terminPayload.length() == 0) {
					tolak(hasil, "Pemesanan Pembelian bertermin harus memiliki minimal satu baris termin.");
					return;
				}
				terminBaru = gabungTermin(terminPayload, terminDari(po));
				double totalTermin = 0;
				for (int i = 0; i < terminBaru.length(); i++) {
					double n = angkaAman(terminBaru.getJSONObject(i), "penagihan");
					if (n <= 0) {
						tolak(hasil, "Nilai penagihan termin ke-" + (i + 1) + " harus lebih besar dari nol.");
						return;
					}
					totalTermin += n;
				}
				if (Math.abs(totalTermin - totalRencana) > TOLERANSI) {
					tolak(hasil, "Jumlah penagihan seluruh termin "
							+ Common.numberFormat.get().format(totalTermin)
							+ " belum sama dengan nilai Pemesanan Pembelian "
							+ Common.numberFormat.get().format(totalRencana)
							+ ". Sesuaikan pembagian terminnya.");
					return;
				}
			}
			po.setFormula(byTermin ? terminBaru.toString() : null);

			session.beginTransaction();
			session.saveOrUpdate(po);
			session.flush();

			@SuppressWarnings("unchecked")
			List<PemesananPengadaanMasterAssetDetail> lama = session
					.createCriteria(PemesananPengadaanMasterAssetDetail.class)
					.add(Restrictions.eq("pemesananPengadaanMasterAsset.id", po.getId())).list();
			for (PemesananPengadaanMasterAssetDetail d : lama) {
				session.delete(d);
			}
			session.flush();

			java.util.List<String> prDetailIds = new java.util.ArrayList<String>();
			double total = 0;
			for (int i = 0; i < detail.length(); i++) {
				JSONObject b = detail.getJSONObject(i);
				MasterAsset barang = barangBaris(session, b);
				if (barang == null) {
					continue;
				}
				double jumlah = angkaAman(b, "jumlah");
				double harga = angkaAman(b, "hargaBeli");
				double sub = jumlah * harga;
				PemesananPengadaanMasterAssetDetail d = new PemesananPengadaanMasterAssetDetail();
				d.setPemesananPengadaanMasterAsset(po);
				d.setMasterAsset(barang);
				d.setJumlah(Double.valueOf(jumlah));
				d.setHargaBeli(Double.valueOf(harga));
				d.setHargaTotal(Double.valueOf(sub));
				d.setKeterangan(b.optString("keterangan", "").trim());
				if (!b.isNull("pr_detail_id") && !(b.get("pr_detail_id") + "").trim().isEmpty()) {
					PermintaanPengadaanMasterAssetDetail asal = (PermintaanPengadaanMasterAssetDetail) session
							.get(PermintaanPengadaanMasterAssetDetail.class,
									Long.valueOf((b.get("pr_detail_id") + "").trim()));
					if (asal != null) {
						d.setPermintaanPengadaanMasterAssetDetail(asal);
						prDetailIds.add(asal.getId() + "");
					}
				}
				d.setOleh(tbmuser.getUserNama());
				d.setOlehId(tbmuser.getUserId());
				session.save(d);
				total += sub;
			}
			// Jejak PR asal disimpan pada header dengan format yang sama dengan versi ZKoss
			// (daftar id baris PR dipisah koma) supaya kedua versi membaca dokumen yang sama.
			StringBuilder jejakPr = new StringBuilder();
			for (String satu : prDetailIds) {
				jejakPr.append(jejakPr.length() == 0 ? "" : ",").append(satu);
			}
			po.setPermintaanPengadaanMasterAssets(jejakPr.length() == 0 ? null : jejakPr.toString());
			po.setNilai(Double.valueOf(total));
			session.saveOrUpdate(po);
			session.getTransaction().commit();

			hasil.put("status", "00");
			hasil.put("id", po.getId());
			hasil.put("kode", po.getKode());
			hasil.put("nilai", total);
		} catch (Exception e) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception eRollback) {
				ais.common.ErrorAuditUtil.record(eRollback, "PengadaanPosApiHelper.poSimpan rollback");
			}
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Keputusan atas PO: setujui / tolak / batalkan keputusan. Param: {@code id},
	 * {@code keputusan} (SETUJUI|TOLAK|BATAL), {@code alasan} (wajib bila menolak).
	 * Butuh aksi granular {@code approve}.
	 */
	public static void poPutusan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehAksi(tbmuser, KUNCI_PO, "approve")) {
			tolak(hasil, "Grup pengguna Anda tidak memiliki hak menyetujui atau menolak Pemesanan Pembelian.");
			return;
		}
		Long id = (request == null || request.isNull("id")) ? null
				: Long.valueOf((request.get("id") + "").trim());
		if (id == null) {
			tolak(hasil, "Parameter id wajib diisi.");
			return;
		}
		String keputusan = request.optString("keputusan", "").trim().toUpperCase();
		String alasan = request.optString("alasan", "").trim();
		if (!"SETUJUI".equals(keputusan) && !"TOLAK".equals(keputusan) && !"BATAL".equals(keputusan)) {
			tolak(hasil, "Keputusan harus salah satu dari SETUJUI, TOLAK, atau BATAL.");
			return;
		}
		if ("TOLAK".equals(keputusan) && alasan.length() < 5) {
			tolak(hasil, "Alasan penolakan wajib diisi minimal 5 karakter agar pembuat tahu "
					+ "apa yang harus diperbaiki.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			PemesananPengadaanMasterAsset po = (PemesananPengadaanMasterAsset) session
					.get(PemesananPengadaanMasterAsset.class, id);
			if (po == null) {
				tolak(hasil, "Pemesanan Pembelian tidak ditemukan.");
				return;
			}
			Long tokoId = tokoLingkup(tbmuser, request);
			if (tokoId != null && po.getToko() != null && !tokoId.equals(po.getToko().getId())) {
				tolak(hasil, "Pemesanan Pembelian ini milik toko lain.");
				return;
			}
			if ("BATAL".equals(keputusan) && po.getDibayar() != null && po.getDibayar().doubleValue() > 0) {
				tolak(hasil, "Persetujuan tidak dapat dibatalkan karena Pemesanan Pembelian ini "
						+ "sudah menerima pembayaran sebesar "
						+ Common.numberFormat.get().format(po.getDibayar()) + ".");
				return;
			}
			session.beginTransaction();
			if ("SETUJUI".equals(keputusan)) {
				po.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());
				po.setDisetujuiOleh(tbmuser);
				po.setTanggalDitolak(null);
				po.setDitolakOleh(null);
				po.setAlasanDitolak(null);
			} else if ("TOLAK".equals(keputusan)) {
				po.setTanggalDitolak(ais.ui.util.WaktuUtil.getDate());
				po.setDitolakOleh(tbmuser);
				po.setAlasanDitolak(alasan);
				po.setTanggalPersetujuan(null);
				po.setDisetujuiOleh(null);
			} else {
				po.setTanggalPersetujuan(null);
				po.setDisetujuiOleh(null);
				po.setTanggalDitolak(null);
				po.setDitolakOleh(null);
				po.setAlasanDitolak(null);
			}
			if (tbmuser != null) {
				po.setOleh(tbmuser.getUserNama());
				po.setOlehId(tbmuser.getUserId());
			}
			session.saveOrUpdate(po);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("id", po.getId());
			hasil.put("statusDokumen", statusPo(po));
		} catch (Exception e) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception eRollback) {
				ais.common.ErrorAuditUtil.record(eRollback, "PengadaanPosApiHelper.poPutusan rollback");
			}
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Hapus PO. Penghapusan bersifat lunak ({@code aktif=false}) mengikuti pola dokumen lain,
	 * sehingga jejak audit dan dokumen turunan tidak putus. PO yang sudah disetujui atau sudah
	 * menerima pembayaran tidak boleh dihapus.
	 */
	public static void poHapus(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehAksi(tbmuser, KUNCI_PO, "delete")) {
			tolak(hasil, "Grup pengguna Anda tidak memiliki hak menghapus Pemesanan Pembelian.");
			return;
		}
		Long id = (request == null || request.isNull("id")) ? null
				: Long.valueOf((request.get("id") + "").trim());
		if (id == null) {
			tolak(hasil, "Parameter id wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			PemesananPengadaanMasterAsset po = (PemesananPengadaanMasterAsset) session
					.get(PemesananPengadaanMasterAsset.class, id);
			if (po == null) {
				tolak(hasil, "Pemesanan Pembelian tidak ditemukan.");
				return;
			}
			Long tokoId = tokoLingkup(tbmuser, request);
			if (tokoId != null && po.getToko() != null && !tokoId.equals(po.getToko().getId())) {
				tolak(hasil, "Pemesanan Pembelian ini milik toko lain.");
				return;
			}
			if (po.getTanggalPersetujuan() != null) {
				tolak(hasil, "Pemesanan Pembelian yang sudah disetujui tidak dapat dihapus. "
						+ "Batalkan persetujuannya terlebih dahulu.");
				return;
			}
			if (po.getDibayar() != null && po.getDibayar().doubleValue() > 0) {
				tolak(hasil, "Pemesanan Pembelian yang sudah menerima pembayaran tidak dapat dihapus.");
				return;
			}
			session.beginTransaction();
			po.setAktif(Boolean.FALSE);
			if (tbmuser != null) {
				po.setOleh(tbmuser.getUserNama());
				po.setOlehId(tbmuser.getUserId());
			}
			session.saveOrUpdate(po);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("id", po.getId());
		} catch (Exception e) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception eRollback) {
				ais.common.ErrorAuditUtil.record(eRollback, "PengadaanPosApiHelper.poHapus rollback");
			}
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Siapkan isian PO dari sebuah PR yang sudah disetujui -- padanan alur "buat PO dari
	 * permintaan" pada versi ZKoss. Tidak menulis apa pun; klien menerima payload siap sunting
	 * lalu menyimpannya lewat {@code pengadaan_po_simpan}.
	 *
	 * <p>Sisa yang belum dipesan dihitung per baris PR: jumlah pada PR dikurangi jumlah yang
	 * sudah masuk PO lain yang masih aktif. Dengan begitu satu PR dapat dipecah menjadi
	 * beberapa PO tanpa terjadi pemesanan berlebih.</p>
	 */
	public static void poDariPr(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehLihat(tbmuser, KUNCI_PO)) {
			tolak(hasil, "Menu Pengadaan tidak diaktifkan untuk grup pengguna Anda.");
			return;
		}
		Long prId = (request == null || request.isNull("pr_id")) ? null
				: Long.valueOf((request.get("pr_id") + "").trim());
		if (prId == null) {
			tolak(hasil, "Parameter pr_id wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			PermintaanPengadaanMasterAsset pr = (PermintaanPengadaanMasterAsset) session
					.get(PermintaanPengadaanMasterAsset.class, prId);
			if (pr == null) {
				tolak(hasil, "Permintaan Pembelian tidak ditemukan.");
				return;
			}
			Long tokoId = tokoLingkup(tbmuser, request);
			if (tokoId != null && pr.getToko() != null && !tokoId.equals(pr.getToko().getId())) {
				tolak(hasil, "Permintaan Pembelian ini milik toko lain.");
				return;
			}
			if (pr.getTanggalPersetujuan() == null) {
				tolak(hasil, "Hanya Permintaan Pembelian yang sudah disetujui yang dapat dijadikan "
						+ "Pemesanan Pembelian.");
				return;
			}
			@SuppressWarnings("unchecked")
			List<PermintaanPengadaanMasterAssetDetail> baris = session
					.createCriteria(PermintaanPengadaanMasterAssetDetail.class)
					.add(Restrictions.eq("permintaanPengadaanMasterAsset.id", pr.getId()))
					.addOrder(Order.asc("id")).list();
			JSONArray arr = new JSONArray();
			double total = 0;
			for (PermintaanPengadaanMasterAssetDetail d : baris) {
				double diminta = d.getJumlah() == null ? 0 : d.getJumlah().doubleValue();
				double sudah = jumlahSudahDipesan(session, d.getId());
				double sisa = diminta - sudah;
				if (sisa <= 0) {
					continue;
				}
				double harga = d.getHargaBeli() == null ? 0 : d.getHargaBeli().doubleValue();
				JSONObject o = new JSONObject();
				o.put("pr_detail_id", d.getId());
				Produk produkBaris = produkDariMasterAsset(session, d.getMasterAsset());
				o.put("produk_id", produkBaris == null ? JSONObject.NULL : produkBaris.getId());
				o.put("master_asset_id", d.getMasterAsset() == null ? JSONObject.NULL : d.getMasterAsset().getId());
				o.put("barang", d.getMasterAsset() == null ? "" : d.getMasterAsset().getNama());
				// Alias "produk": layar JSP menamai kolom ini demikian. Disediakan server agar
				// kedua penamaan sah dan nama barang tidak pernah tampil kosong.
				o.put("produk", d.getMasterAsset() == null ? "" : d.getMasterAsset().getNama());
				o.put("kodeBarang", d.getMasterAsset() == null || d.getMasterAsset().getKode() == null ? ""
						: d.getMasterAsset().getKode());
				o.put("jumlahDiminta", diminta);
				o.put("jumlahSudahDipesan", sudah);
				o.put("jumlah", sisa);
				o.put("hargaBeli", harga);
				o.put("hargaTotal", sisa * harga);
				o.put("keterangan", d.getKeterangan() == null ? "" : d.getKeterangan());
				arr.put(o);
				total += sisa * harga;
			}
			hasil.put("status", "00");
			hasil.put("pr_id", pr.getId());
			hasil.put("pr_kode", pr.getKode() == null ? "" : pr.getKode());
			hasil.put("keterangan", pr.getKeterangan() == null ? "" : pr.getKeterangan());
			hasil.put("toko_id", pr.getToko() == null ? JSONObject.NULL : pr.getToko().getId());
			hasil.put("detail", arr);
			hasil.put("nilai", total);
			if (arr.length() == 0) {
				hasil.put("catatan", "Seluruh baris Permintaan Pembelian ini sudah dipesan.");
			}
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Jumlah sebuah baris PR yang sudah tercantum pada PO aktif mana pun. */
	private static double jumlahSudahDipesan(Session session, Long prDetailId) {
		if (prDetailId == null) {
			return 0;
		}
		@SuppressWarnings("unchecked")
		List<PemesananPengadaanMasterAssetDetail> daftar = session
				.createCriteria(PemesananPengadaanMasterAssetDetail.class)
				.add(Restrictions.eq("permintaanPengadaanMasterAssetDetail.id", prDetailId)).list();
		double jml = 0;
		for (PemesananPengadaanMasterAssetDetail d : daftar) {
			PemesananPengadaanMasterAsset induk = d.getPemesananPengadaanMasterAsset();
			if (induk == null || Boolean.FALSE.equals(induk.getAktif())) {
				continue;
			}
			jml += d.getJumlah() == null ? 0 : d.getJumlah().doubleValue();
		}
		return jml;
	}

	/** Pencarian penyedia/vendor untuk pemilih pada layar PO. */
	public static void cariPenyedia(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehLihat(tbmuser, KUNCI_PO) && !bolehLihat(tbmuser, KUNCI_BAST)) {
			tolak(hasil, "Menu Pengadaan tidak diaktifkan untuk grup pengguna Anda.");
			return;
		}
		String q = request == null ? "" : request.optString("keyword", "").trim();
		int limit = Math.min(200, Math.max(5, request == null ? 50 : request.optInt("limit", 50)));
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Criteria kriteria = session.createCriteria(PenyediaAsset.class);
			if (q.length() > 0) {
				kriteria.add(Restrictions.disjunction()
						.add(Restrictions.ilike("kode", q, MatchMode.ANYWHERE))
						.add(Restrictions.ilike("nama", q, MatchMode.ANYWHERE)));
			}
			kriteria.addOrder(Order.asc("nama"));
			kriteria.setMaxResults(limit);
			@SuppressWarnings("unchecked")
			List<PenyediaAsset> daftar = kriteria.list();
			JSONArray arr = new JSONArray();
			for (PenyediaAsset v : daftar) {
				JSONObject o = new JSONObject();
				o.put("id", v.getId());
				o.put("penyedia_id", v.getId());
				o.put("kode", v.getKode() == null ? "" : v.getKode());
				o.put("nama", v.getNama() == null ? "" : v.getNama());
				o.put("alamat", v.getAlamat() == null ? "" : v.getAlamat());
				arr.put(o);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}


	/**
	 * Label status BAST. Berbeda dengan PR/PO, model penerimaan yang sudah ada TIDAK
	 * menyediakan kolom penolakan -- BAST keliru diperbaiki lewat sunting atau dihapus
	 * (nonaktif), bukan ditolak. Statusnya karena itu hanya DRAFT dan DISETUJUI.
	 */
	private static String statusBast(PenerimaanPengadaanMasterAsset bast) {
		return bast.getTanggalPersetujuan() != null ? "DISETUJUI" : "DRAFT";
	}

	/**
	 * Jumlah sebuah baris PO yang SUDAH diterima lewat BAST aktif mana pun.
	 * Dipakai untuk menghitung sisa yang masih boleh diterima, sehingga penerimaan
	 * tidak pernah melebihi yang dipesan.
	 *
	 * @param kecualiBastId BAST yang sedang disunting -- barisnya tidak ikut dihitung
	 *                      supaya penyuntingan tidak menuduh dirinya sendiri berlebih.
	 */
	private static double jumlahSudahDiterima(Session session, Long poDetailId, Long kecualiBastId) {
		if (poDetailId == null) {
			return 0;
		}
		@SuppressWarnings("unchecked")
		List<PenerimaanPengadaanMasterAssetDetail> daftar = session
				.createCriteria(PenerimaanPengadaanMasterAssetDetail.class)
				.add(Restrictions.eq("pemesananPengadaanMasterAssetDetail.id", poDetailId)).list();
		double jml = 0;
		for (PenerimaanPengadaanMasterAssetDetail d : daftar) {
			PenerimaanPengadaanMasterAsset induk = d.getPenerimaanPengadaanMasterAsset();
			if (induk == null || Boolean.FALSE.equals(induk.getAktif())) {
				continue;
			}
			if (kecualiBastId != null && kecualiBastId.equals(induk.getId())) {
				continue;
			}
			jml += d.getDiterima() == null ? 0 : d.getDiterima().doubleValue();
		}
		return jml;
	}

	/**
	 * Daftar BAST pada lingkup toko pemanggil. Param opsional: {@code cari}
	 * (kode/keterangan/nomor tagihan), {@code status} (DRAFT/DISETUJUI), {@code page},
	 * {@code pageSize}.
	 */
	public static void bastDaftar(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehLihat(tbmuser, KUNCI_BAST)) {
			tolak(hasil, "Menu Pengadaan tidak diaktifkan untuk grup pengguna Anda.");
			return;
		}
		Long tokoId = tokoLingkup(tbmuser, request);
		int page = Math.max(1, request == null ? 1 : request.optInt("page", 1));
		int pageSize = Math.min(100, Math.max(5, request == null ? 15 : request.optInt("pageSize", 15)));
		String cari = request == null ? "" : request.optString("cari", "").trim();
		String status = request == null ? "" : request.optString("status", "").trim().toUpperCase();
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Criteria kriteria = session.createCriteria(PenerimaanPengadaanMasterAsset.class);
			kriteria.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)));
			if (tokoId != null) {
				kriteria.add(Restrictions.eq("toko.id", tokoId));
			}
			if (cari.length() > 0) {
				kriteria.add(Restrictions.disjunction()
						.add(Restrictions.ilike("kode", cari, MatchMode.ANYWHERE))
						.add(Restrictions.ilike("keterangan", cari, MatchMode.ANYWHERE))
						.add(Restrictions.ilike("kodeTagihan", cari, MatchMode.ANYWHERE)));
			}
			kriteria.addOrder(Order.desc("id"));
			@SuppressWarnings("unchecked")
			List<PenerimaanPengadaanMasterAsset> semua = kriteria.list();
			JSONArray arr = new JSONArray();
			int cocok = 0;
			int mulai = (page - 1) * pageSize;
			for (PenerimaanPengadaanMasterAsset bast : semua) {
				String st = statusBast(bast);
				if (status.length() > 0 && !status.equals(st)) {
					continue;
				}
				cocok++;
				if (cocok <= mulai || arr.length() >= pageSize) {
					continue;
				}
				JSONObject o = new JSONObject();
				o.put("id", bast.getId());
				o.put("kode", bast.getKode() == null ? "" : bast.getKode());
				o.put("kodeTagihan", bast.getKodeTagihan() == null ? "" : bast.getKodeTagihan());
				o.put("keterangan", bast.getKeterangan() == null ? "" : bast.getKeterangan());
				o.put("tanggal", bast.getTanggalPembuatan() == null ? JSONObject.NULL
						: Common.dateFormat3.get().format(bast.getTanggalPembuatan()));
				o.put("penyedia", bast.getPenyedia() == null ? "" : bast.getPenyedia().getNama());
				o.put("po_id", bast.getPemesananPengadaanMasterAsset() == null ? JSONObject.NULL
						: bast.getPemesananPengadaanMasterAsset().getId());
				o.put("po", bast.getPemesananPengadaanMasterAsset() == null ? ""
						: (bast.getPemesananPengadaanMasterAsset().getKode() == null ? ""
								: bast.getPemesananPengadaanMasterAsset().getKode()));
				o.put("tanpaPemesanan", Boolean.TRUE.equals(bast.getTampaPemesanan()));
				o.put("sudahSinkron", bast.getPengadaanFaktur() != null);
				o.put("nomorFakturKulakan", bast.getPengadaanFaktur() == null ? ""
						: (bast.getPengadaanFaktur().getNomorFaktur() == null ? ""
								: bast.getPengadaanFaktur().getNomorFaktur()));
				o.put("nilai", bast.getNilai() == null ? 0 : bast.getNilai());
				o.put("status", st);
				o.put("toko", bast.getToko() == null ? "" : bast.getToko().getNama());
				o.put("dibuatOleh", bast.getDibuatOleh() == null ? "" : bast.getDibuatOleh().getUserNama());
				o.put("disetujuiOleh", bast.getDisetujuiOleh() == null ? "" : bast.getDisetujuiOleh().getUserNama());
				arr.put(o);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("total", cocok);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Detail satu BAST: header + baris barang beserta sisa yang masih boleh diterima. */
	public static void bastDetail(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehLihat(tbmuser, KUNCI_BAST)) {
			tolak(hasil, "Menu Pengadaan tidak diaktifkan untuk grup pengguna Anda.");
			return;
		}
		Long id = (request == null || request.isNull("id")) ? null
				: Long.valueOf((request.get("id") + "").trim());
		if (id == null) {
			tolak(hasil, "Parameter id wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			PenerimaanPengadaanMasterAsset bast = (PenerimaanPengadaanMasterAsset) session
					.get(PenerimaanPengadaanMasterAsset.class, id);
			if (bast == null) {
				tolak(hasil, "Penerimaan Barang tidak ditemukan.");
				return;
			}
			Long tokoId = tokoLingkup(tbmuser, request);
			if (tokoId != null && bast.getToko() != null && !tokoId.equals(bast.getToko().getId())) {
				tolak(hasil, "Penerimaan Barang ini milik toko lain.");
				return;
			}
			PemesananPengadaanMasterAsset po = bast.getPemesananPengadaanMasterAsset();
			JSONObject h = new JSONObject();
			h.put("id", bast.getId());
			h.put("kode", bast.getKode() == null ? "" : bast.getKode());
			h.put("kodeTagihan", bast.getKodeTagihan() == null ? "" : bast.getKodeTagihan());
			h.put("keterangan", bast.getKeterangan() == null ? "" : bast.getKeterangan());
			h.put("kurir", bast.getKurir() == null ? "" : bast.getKurir());
			h.put("tanggal", bast.getTanggalPembuatan() == null ? JSONObject.NULL
					: Common.dateFormat3.get().format(bast.getTanggalPembuatan()));
			h.put("tanggalTagihan", bast.getTanggalTagihan() == null ? ""
					: Common.dateFormat1.get().format(bast.getTanggalTagihan()));
			h.put("status", statusBast(bast));
			h.put("nilai", bast.getNilai() == null ? 0 : bast.getNilai());
			h.put("tanpaPemesanan", Boolean.TRUE.equals(bast.getTampaPemesanan()));
			h.put("sudahSinkron", bast.getPengadaanFaktur() != null);
			h.put("nomorFakturKulakan", bast.getPengadaanFaktur() == null ? ""
					: (bast.getPengadaanFaktur().getNomorFaktur() == null ? ""
							: bast.getPengadaanFaktur().getNomorFaktur()));
			h.put("penyedia_id", bast.getPenyedia() == null ? JSONObject.NULL : bast.getPenyedia().getId());
			h.put("penyedia", bast.getPenyedia() == null ? "" : bast.getPenyedia().getNama());
			h.put("po_id", po == null ? JSONObject.NULL : po.getId());
			h.put("po", po == null || po.getKode() == null ? "" : po.getKode());
			h.put("toko_id", bast.getToko() == null ? JSONObject.NULL : bast.getToko().getId());
			h.put("toko", bast.getToko() == null ? "" : bast.getToko().getNama());
			h.put("dibuatOleh", bast.getDibuatOleh() == null ? "" : bast.getDibuatOleh().getUserNama());
			h.put("disetujuiOleh", bast.getDisetujuiOleh() == null ? "" : bast.getDisetujuiOleh().getUserNama());

			@SuppressWarnings("unchecked")
			List<PenerimaanPengadaanMasterAssetDetail> baris = session
					.createCriteria(PenerimaanPengadaanMasterAssetDetail.class)
					.add(Restrictions.eq("penerimaanPengadaanMasterAsset.id", bast.getId()))
					.addOrder(Order.asc("id")).list();
			JSONArray arr = new JSONArray();
			for (PenerimaanPengadaanMasterAssetDetail d : baris) {
				PemesananPengadaanMasterAssetDetail asal = d.getPemesananPengadaanMasterAssetDetail();
				double dipesan = asal == null ? 0 : (asal.getJumlah() == null ? 0 : asal.getJumlah().doubleValue());
				double lain = asal == null ? 0 : jumlahSudahDiterima(session, asal.getId(), bast.getId());
				JSONObject o = new JSONObject();
				o.put("id", d.getId());
				Produk produkBaris = produkDariMasterAsset(session, d.getMasterAsset());
				o.put("produk_id", produkBaris == null ? JSONObject.NULL : produkBaris.getId());
				o.put("master_asset_id", d.getMasterAsset() == null ? JSONObject.NULL : d.getMasterAsset().getId());
				o.put("barang", d.getMasterAsset() == null ? "" : d.getMasterAsset().getNama());
				// Alias "produk": layar JSP menamai kolom ini demikian. Disediakan server agar
				// kedua penamaan sah dan nama barang tidak pernah tampil kosong.
				o.put("produk", d.getMasterAsset() == null ? "" : d.getMasterAsset().getNama());
				o.put("kodeBarang", d.getMasterAsset() == null || d.getMasterAsset().getKode() == null ? ""
						: d.getMasterAsset().getKode());
				o.put("jumlahDipesan", dipesan);
				o.put("diterimaDokumenLain", lain);
				o.put("sisaBolehDiterima", asal == null ? JSONObject.NULL : Math.max(0, dipesan - lain));
				o.put("diterima", d.getDiterima() == null ? 0 : d.getDiterima());
				o.put("hargaBeli", d.getHargaBeli() == null ? 0 : d.getHargaBeli());
				o.put("hargaTotal", d.getHargaTotal() == null ? 0 : d.getHargaTotal());
				o.put("kondisi", d.getKondisi() == null ? "" : d.getKondisi());
				o.put("keterangan", d.getKeterangan() == null ? "" : d.getKeterangan());
				o.put("po_detail_id", asal == null ? JSONObject.NULL : asal.getId());
				arr.put(o);
			}
			hasil.put("status", "00");
			hasil.put("header", h);
			hasil.put("detail", arr);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Tambah/ubah BAST beserta baris barangnya dalam SATU transaksi.
	 *
	 * <p>Aturan yang ditegakkan server:</p>
	 * <ul>
	 * <li>Penerimaan atas PO hanya boleh dari PO yang SUDAH DISETUJUI.</li>
	 * <li>Jumlah diterima per baris tidak boleh melebihi sisa yang dipesan (jumlah PO
	 * dikurangi yang sudah diterima BAST aktif lain) -- penerimaan berlebih adalah
	 * kesalahan yang mahal dikoreksi setelah stok terlanjur bertambah.</li>
	 * <li>BAST yang sudah disetujui tidak dapat diubah.</li>
	 * <li>{@code nilai} header dihitung ULANG dari baris memakai perhitungan milik entitas
	 * (diterima x harga, dikurangi potongan, ditambah PPN, dikurangi PPh bila dikonfigurasi)
	 * sehingga sama persis dengan angka yang dipakai versi ZKoss.</li>
	 * </ul>
	 */
	public static void bastSimpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Long id = (request == null || request.isNull("id") || (request.get("id") + "").trim().isEmpty())
				? null : Long.valueOf((request.get("id") + "").trim());
		if (!bolehAksi(tbmuser, KUNCI_BAST, id == null ? "create" : "update")) {
			tolak(hasil, "Grup pengguna Anda tidak memiliki hak "
					+ (id == null ? "membuat" : "mengubah") + " Penerimaan Barang.");
			return;
		}
		if (tbmuser == null) {
			tolak(hasil, "Sesi pengguna tidak dikenali, silakan masuk ulang.");
			return;
		}
		JSONArray detail = request == null ? null : request.optJSONArray("detail");
		if (detail == null || detail.length() == 0) {
			tolak(hasil, "Penerimaan Barang harus memiliki minimal satu baris barang.");
			return;
		}
		Long poId = (request.isNull("po_id") || (request.get("po_id") + "").trim().isEmpty())
				? null : Long.valueOf((request.get("po_id") + "").trim());
		Long tokoId = tokoLingkup(tbmuser, request);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			PenerimaanPengadaanMasterAsset bast;
			if (id != null) {
				bast = (PenerimaanPengadaanMasterAsset) session.get(PenerimaanPengadaanMasterAsset.class, id);
				if (bast == null) {
					tolak(hasil, "Penerimaan Barang tidak ditemukan.");
					return;
				}
				if (tokoId != null && bast.getToko() != null && !tokoId.equals(bast.getToko().getId())) {
					tolak(hasil, "Penerimaan Barang ini milik toko lain.");
					return;
				}
				if (bast.getTanggalPersetujuan() != null) {
					tolak(hasil, "Penerimaan Barang yang sudah disetujui tidak dapat diubah. "
							+ "Batalkan persetujuan terlebih dahulu bila memang perlu dikoreksi.");
					return;
				}
			} else {
				bast = new PenerimaanPengadaanMasterAsset();
				bast.setTanggalPembuatan(ais.ui.util.WaktuUtil.getDate());
				bast.setDibuatOleh(tbmuser);
				bast.setAktif(Boolean.TRUE);
			}
			PemesananPengadaanMasterAsset po = null;
			if (poId != null) {
				po = (PemesananPengadaanMasterAsset) session.get(PemesananPengadaanMasterAsset.class, poId);
				if (po == null) {
					tolak(hasil, "Pemesanan Pembelian tidak ditemukan.");
					return;
				}
				if (tokoId != null && po.getToko() != null && !tokoId.equals(po.getToko().getId())) {
					tolak(hasil, "Pemesanan Pembelian ini milik toko lain.");
					return;
				}
				if (po.getTanggalPersetujuan() == null) {
					tolak(hasil, "Hanya Pemesanan Pembelian yang sudah disetujui yang dapat diterima barangnya.");
					return;
				}
			}
			bast.setPemesananPengadaanMasterAsset(po);
			bast.setTampaPemesanan(Boolean.valueOf(po == null));
			if (po != null && po.getPenyedia() != null) {
				bast.setPenyedia(po.getPenyedia());
			} else {
				PenyediaAsset vendor = penyediaDokumen(session, request);
				if (vendor != null) {
					bast.setPenyedia(vendor);
				}
			}
			if (tokoId != null) {
				bast.setToko((Toko) session.get(Toko.class, tokoId));
			}
			bast.setKeterangan(request.optString("keterangan", "").trim());
			bast.setKodeTagihan(request.optString("kodeTagihan", "").trim());
			bast.setKurir(request.optString("kurir", "").trim());
			String tglTagihan = request.optString("tanggalTagihan", "").trim();
			if (tglTagihan.isEmpty()) {
				bast.setTanggalTagihan(null);
			} else {
				java.util.Date parsed = tanggalKetat(tglTagihan);
				if (parsed == null) {
					tolak(hasil, "Tanggal tagihan harus berformat hh-bb-tttt, mis. 10-08-2026.");
					return;
				}
				bast.setTanggalTagihan(parsed);
			}
			if (!request.isNull("tanggal")) {
				try {
					bast.setTanggalPembuatan(Common.dateFormat3.get().parse((request.get("tanggal") + "").trim()));
				} catch (Exception e) {
					// Format tanggal tidak dikenali -> pertahankan nilai yang sudah ada.
				}
			}
			if (bast.getKode() == null || bast.getKode().trim().isEmpty()) {
				bast.setKode(buatKodeUmum(session, PenerimaanPengadaanMasterAsset.class, "BAST", tokoId));
			}
			bast.setOleh(tbmuser.getUserNama());
			bast.setOlehId(tbmuser.getUserId());

			// Validasi penerimaan berlebih dilakukan SEBELUM satu baris pun ditulis, supaya
			// BAST yang gagal validasi tidak meninggalkan jejak separuh jadi.
			for (int i = 0; i < detail.length(); i++) {
				JSONObject b = detail.optJSONObject(i);
				if (b == null || b.isNull("po_detail_id") || (b.get("po_detail_id") + "").trim().isEmpty()) {
					continue;
				}
				PemesananPengadaanMasterAssetDetail asal = (PemesananPengadaanMasterAssetDetail) session
						.get(PemesananPengadaanMasterAssetDetail.class,
								Long.valueOf((b.get("po_detail_id") + "").trim()));
				if (asal == null) {
					continue;
				}
				double dipesan = asal.getJumlah() == null ? 0 : asal.getJumlah().doubleValue();
				double lain = jumlahSudahDiterima(session, asal.getId(), bast.getId());
				double diminta = angkaAman(b, "diterima");
				if (diminta > dipesan - lain + TOLERANSI) {
					String namaBarang = asal.getMasterAsset() == null ? "barang"
							: asal.getMasterAsset().getNama();
					tolak(hasil, "Jumlah diterima untuk " + namaBarang + " (" + hapusNolEkor(diminta)
							+ ") melebihi sisa yang dipesan (" + hapusNolEkor(Math.max(0, dipesan - lain))
							+ " dari " + hapusNolEkor(dipesan) + " yang dipesan).");
					return;
				}
			}

			session.beginTransaction();
			session.saveOrUpdate(bast);
			session.flush();

			@SuppressWarnings("unchecked")
			List<PenerimaanPengadaanMasterAssetDetail> lama = session
					.createCriteria(PenerimaanPengadaanMasterAssetDetail.class)
					.add(Restrictions.eq("penerimaanPengadaanMasterAsset.id", bast.getId())).list();
			for (PenerimaanPengadaanMasterAssetDetail d : lama) {
				session.delete(d);
			}
			session.flush();

			double total = 0;
			for (int i = 0; i < detail.length(); i++) {
				JSONObject b = detail.getJSONObject(i);
				MasterAsset barang = barangBaris(session, b);
				if (barang == null) {
					continue;
				}
				PemesananPengadaanMasterAssetDetail asal = null;
				if (!b.isNull("po_detail_id") && !(b.get("po_detail_id") + "").trim().isEmpty()) {
					asal = (PemesananPengadaanMasterAssetDetail) session.get(
							PemesananPengadaanMasterAssetDetail.class,
							Long.valueOf((b.get("po_detail_id") + "").trim()));
				}
				double diterima = angkaAman(b, "diterima");
				double harga = angkaAman(b, "hargaBeli");
				PenerimaanPengadaanMasterAssetDetail d = new PenerimaanPengadaanMasterAssetDetail();
				d.setPenerimaanPengadaanMasterAsset(bast);
				d.setMasterAsset(barang);
				d.setPemesananPengadaanMasterAssetDetail(asal);
				// jumlah = yang dipesan, diterima = yang benar-benar datang. Keduanya diisi
				// TEGAS: getter entitas mengisi sendiri bila null, dan nilai turunan itu
				// dapat menyesatkan pembacaan berikutnya.
				d.setJumlah(Double.valueOf(asal == null ? diterima
						: (asal.getJumlah() == null ? diterima : asal.getJumlah().doubleValue())));
				d.setDiterima(Double.valueOf(diterima));
				d.setHargaBeli(Double.valueOf(harga));
				d.setHargaPotongan(Double.valueOf(angkaAman(b, "hargaPotongan")));
				d.setDiskonDalamBentukPersen(Boolean.valueOf(b.optBoolean("diskonPersen", false)));
				d.setPersenPpn(Double.valueOf(angkaAman(b, "persenPpn")));
				d.setPersenPph(Double.valueOf(angkaAman(b, "persenPph")));
				d.setKondisi(b.optString("kondisi", "").trim());
				d.setKeterangan(b.optString("keterangan", "").trim());
				d.setOleh(tbmuser.getUserNama());
				d.setOlehId(tbmuser.getUserId());
				session.save(d);
				// Total dihitung lewat getHargaTotal() milik entitas -- satu rumus dipakai
				// bersama layar ZKoss, termasuk perlakuan PPN/PPh dan potongan.
				Double sub = d.getHargaTotal();
				total += sub == null ? 0 : sub.doubleValue();
			}
			bast.setNilai(Double.valueOf(total));
			session.saveOrUpdate(bast);
			session.getTransaction().commit();

			hasil.put("status", "00");
			hasil.put("id", bast.getId());
			hasil.put("kode", bast.getKode());
			hasil.put("nilai", total);
		} catch (Exception e) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception eRollback) {
				ais.common.ErrorAuditUtil.record(eRollback, "PengadaanPosApiHelper.bastSimpan rollback");
			}
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Tampilkan jumlah tanpa nol di belakang koma bila memang bulat (mis. "3", bukan "3.0"). */
	private static String hapusNolEkor(double n) {
		if (Math.abs(n - Math.rint(n)) < 0.0001) {
			return String.valueOf((long) Math.rint(n));
		}
		return Common.numberFormat.get().format(n);
	}

	/**
	 * Keputusan atas BAST: setujui atau batalkan persetujuan. Model penerimaan yang sudah ada
	 * tidak menyediakan kolom penolakan, jadi keputusan yang tersedia hanya SETUJUI dan BATAL.
	 */
	public static void bastPutusan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehAksi(tbmuser, KUNCI_BAST, "approve")) {
			tolak(hasil, "Grup pengguna Anda tidak memiliki hak menyetujui Penerimaan Barang.");
			return;
		}
		Long id = (request == null || request.isNull("id")) ? null
				: Long.valueOf((request.get("id") + "").trim());
		if (id == null) {
			tolak(hasil, "Parameter id wajib diisi.");
			return;
		}
		String keputusan = request.optString("keputusan", "").trim().toUpperCase();
		if (!"SETUJUI".equals(keputusan) && !"BATAL".equals(keputusan)) {
			tolak(hasil, "Keputusan untuk Penerimaan Barang hanya SETUJUI atau BATAL.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			PenerimaanPengadaanMasterAsset bast = (PenerimaanPengadaanMasterAsset) session
					.get(PenerimaanPengadaanMasterAsset.class, id);
			if (bast == null) {
				tolak(hasil, "Penerimaan Barang tidak ditemukan.");
				return;
			}
			Long tokoId = tokoLingkup(tbmuser, request);
			if (tokoId != null && bast.getToko() != null && !tokoId.equals(bast.getToko().getId())) {
				tolak(hasil, "Penerimaan Barang ini milik toko lain.");
				return;
			}
			session.beginTransaction();
			if ("SETUJUI".equals(keputusan)) {
				bast.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());
				bast.setDisetujuiOleh(tbmuser);
			} else {
				bast.setTanggalPersetujuan(null);
				bast.setDisetujuiOleh(null);
			}
			bast.setOleh(tbmuser.getUserNama());
			bast.setOlehId(tbmuser.getUserId());
			session.saveOrUpdate(bast);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("id", bast.getId());
			hasil.put("statusDokumen", statusBast(bast));
		} catch (Exception e) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception eRollback) {
				ais.common.ErrorAuditUtil.record(eRollback, "PengadaanPosApiHelper.bastPutusan rollback");
			}
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Hapus BAST secara lunak ({@code aktif=false}). BAST yang sudah disetujui tidak boleh
	 * dihapus karena sudah menjadi dasar tagihan dan pembayaran vendor.
	 */
	public static void bastHapus(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehAksi(tbmuser, KUNCI_BAST, "delete")) {
			tolak(hasil, "Grup pengguna Anda tidak memiliki hak menghapus Penerimaan Barang.");
			return;
		}
		Long id = (request == null || request.isNull("id")) ? null
				: Long.valueOf((request.get("id") + "").trim());
		if (id == null) {
			tolak(hasil, "Parameter id wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			PenerimaanPengadaanMasterAsset bast = (PenerimaanPengadaanMasterAsset) session
					.get(PenerimaanPengadaanMasterAsset.class, id);
			if (bast == null) {
				tolak(hasil, "Penerimaan Barang tidak ditemukan.");
				return;
			}
			Long tokoId = tokoLingkup(tbmuser, request);
			if (tokoId != null && bast.getToko() != null && !tokoId.equals(bast.getToko().getId())) {
				tolak(hasil, "Penerimaan Barang ini milik toko lain.");
				return;
			}
			if (bast.getTanggalPersetujuan() != null) {
				tolak(hasil, "Penerimaan Barang yang sudah disetujui tidak dapat dihapus. "
						+ "Batalkan persetujuannya terlebih dahulu.");
				return;
			}
			session.beginTransaction();
			bast.setAktif(Boolean.FALSE);
			bast.setOleh(tbmuser.getUserNama());
			bast.setOlehId(tbmuser.getUserId());
			session.saveOrUpdate(bast);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("id", bast.getId());
		} catch (Exception e) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception eRollback) {
				ais.common.ErrorAuditUtil.record(eRollback, "PengadaanPosApiHelper.bastHapus rollback");
			}
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Siapkan isian BAST dari sebuah PO yang sudah disetujui. Tidak menulis apa pun; klien
	 * menerima payload siap sunting lalu menyimpannya lewat {@code pengadaan_bast_simpan}.
	 *
	 * <p>Sisa per baris = jumlah dipesan dikurangi yang sudah diterima BAST aktif lain,
	 * sehingga satu PO dapat diterima bertahap (kirim sebagian) tanpa penerimaan berlebih.</p>
	 */
	public static void bastDariPo(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehLihat(tbmuser, KUNCI_BAST)) {
			tolak(hasil, "Menu Pengadaan tidak diaktifkan untuk grup pengguna Anda.");
			return;
		}
		Long poId = (request == null || request.isNull("po_id")) ? null
				: Long.valueOf((request.get("po_id") + "").trim());
		if (poId == null) {
			tolak(hasil, "Parameter po_id wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			PemesananPengadaanMasterAsset po = (PemesananPengadaanMasterAsset) session
					.get(PemesananPengadaanMasterAsset.class, poId);
			if (po == null) {
				tolak(hasil, "Pemesanan Pembelian tidak ditemukan.");
				return;
			}
			Long tokoId = tokoLingkup(tbmuser, request);
			if (tokoId != null && po.getToko() != null && !tokoId.equals(po.getToko().getId())) {
				tolak(hasil, "Pemesanan Pembelian ini milik toko lain.");
				return;
			}
			if (po.getTanggalPersetujuan() == null) {
				tolak(hasil, "Hanya Pemesanan Pembelian yang sudah disetujui yang dapat diterima barangnya.");
				return;
			}
			@SuppressWarnings("unchecked")
			List<PemesananPengadaanMasterAssetDetail> baris = session
					.createCriteria(PemesananPengadaanMasterAssetDetail.class)
					.add(Restrictions.eq("pemesananPengadaanMasterAsset.id", po.getId()))
					.addOrder(Order.asc("id")).list();
			JSONArray arr = new JSONArray();
			double total = 0;
			for (PemesananPengadaanMasterAssetDetail d : baris) {
				double dipesan = d.getJumlah() == null ? 0 : d.getJumlah().doubleValue();
				double sudah = jumlahSudahDiterima(session, d.getId(), null);
				double sisa = dipesan - sudah;
				if (sisa <= 0) {
					continue;
				}
				double harga = d.getHargaBeli() == null ? 0 : d.getHargaBeli().doubleValue();
				JSONObject o = new JSONObject();
				o.put("po_detail_id", d.getId());
				Produk produkBaris = produkDariMasterAsset(session, d.getMasterAsset());
				o.put("produk_id", produkBaris == null ? JSONObject.NULL : produkBaris.getId());
				o.put("master_asset_id", d.getMasterAsset() == null ? JSONObject.NULL : d.getMasterAsset().getId());
				o.put("barang", d.getMasterAsset() == null ? "" : d.getMasterAsset().getNama());
				// Alias "produk": layar JSP menamai kolom ini demikian. Disediakan server agar
				// kedua penamaan sah dan nama barang tidak pernah tampil kosong.
				o.put("produk", d.getMasterAsset() == null ? "" : d.getMasterAsset().getNama());
				o.put("kodeBarang", d.getMasterAsset() == null || d.getMasterAsset().getKode() == null ? ""
						: d.getMasterAsset().getKode());
				o.put("jumlahDipesan", dipesan);
				o.put("diterimaDokumenLain", sudah);
				o.put("sisaBolehDiterima", sisa);
				o.put("diterima", sisa);
				o.put("hargaBeli", harga);
				o.put("hargaTotal", sisa * harga);
				o.put("keterangan", d.getKeterangan() == null ? "" : d.getKeterangan());
				arr.put(o);
				total += sisa * harga;
			}
			hasil.put("status", "00");
			hasil.put("po_id", po.getId());
			hasil.put("po_kode", po.getKode() == null ? "" : po.getKode());
			hasil.put("penyedia_id", po.getPenyedia() == null ? JSONObject.NULL : po.getPenyedia().getId());
			hasil.put("penyedia", po.getPenyedia() == null ? "" : po.getPenyedia().getNama());
			hasil.put("keterangan", po.getKeterangan() == null ? "" : po.getKeterangan());
			hasil.put("toko_id", po.getToko() == null ? JSONObject.NULL : po.getToko().getId());
			hasil.put("detail", arr);
			hasil.put("nilai", total);
			if (arr.length() == 0) {
				hasil.put("catatan", "Seluruh barang pada Pemesanan Pembelian ini sudah diterima.");
			}
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}


	/**
	 * Daftar tagihan vendor. Pada model pengadaan yang sudah ada, "terima tagihan" BUKAN
	 * dokumen tersendiri melainkan tahap di atas BAST: nomor dan tanggal faktur vendor
	 * dicapkan pada penerimaan yang sudah disetujui, lalu menjadi dasar pembayaran vendor.
	 *
	 * <p>Karena itu daftar ini menampilkan BAST yang SUDAH DISETUJUI saja -- barang yang
	 * belum diakui diterima tidak boleh ditagihkan. Param opsional: {@code cari},
	 * {@code status} (BELUM/SUDAH), {@code page}, {@code pageSize}.</p>
	 */
	public static void tagihanDaftar(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehLihat(tbmuser, KUNCI_TAGIHAN)) {
			tolak(hasil, "Menu Pengadaan tidak diaktifkan untuk grup pengguna Anda.");
			return;
		}
		Long tokoId = tokoLingkup(tbmuser, request);
		int page = Math.max(1, request == null ? 1 : request.optInt("page", 1));
		int pageSize = Math.min(100, Math.max(5, request == null ? 15 : request.optInt("pageSize", 15)));
		String cari = request == null ? "" : request.optString("cari", "").trim();
		String status = request == null ? "" : request.optString("status", "").trim().toUpperCase();
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Criteria kriteria = session.createCriteria(PenerimaanPengadaanMasterAsset.class);
			kriteria.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)));
			kriteria.add(Restrictions.isNotNull("tanggalPersetujuan"));
			if (tokoId != null) {
				kriteria.add(Restrictions.eq("toko.id", tokoId));
			}
			if (cari.length() > 0) {
				kriteria.add(Restrictions.disjunction()
						.add(Restrictions.ilike("kode", cari, MatchMode.ANYWHERE))
						.add(Restrictions.ilike("keterangan", cari, MatchMode.ANYWHERE))
						.add(Restrictions.ilike("kodeTagihan", cari, MatchMode.ANYWHERE)));
			}
			kriteria.addOrder(Order.desc("id"));
			@SuppressWarnings("unchecked")
			List<PenerimaanPengadaanMasterAsset> semua = kriteria.list();
			JSONArray arr = new JSONArray();
			int cocok = 0;
			int mulai = (page - 1) * pageSize;
			for (PenerimaanPengadaanMasterAsset bast : semua) {
				boolean sudah = sudahDitagih(bast);
				String st = sudah ? "SUDAH" : "BELUM";
				if (status.length() > 0 && !status.equals(st)) {
					continue;
				}
				cocok++;
				if (cocok <= mulai || arr.length() >= pageSize) {
					continue;
				}
				PemesananPengadaanMasterAsset po = bast.getPemesananPengadaanMasterAsset();
				JSONObject o = new JSONObject();
				o.put("id", bast.getId());
				o.put("kode", bast.getKode() == null ? "" : bast.getKode());
				o.put("keterangan", bast.getKeterangan() == null ? "" : bast.getKeterangan());
				o.put("tanggal", bast.getTanggalPembuatan() == null ? JSONObject.NULL
						: Common.dateFormat3.get().format(bast.getTanggalPembuatan()));
				o.put("penyedia", bast.getPenyedia() == null ? "" : bast.getPenyedia().getNama());
				o.put("po", po == null || po.getKode() == null ? "" : po.getKode());
				o.put("nilai", bast.getNilai() == null ? 0 : bast.getNilai());
				o.put("kodeTagihan", bast.getKodeTagihan() == null ? "" : bast.getKodeTagihan());
				o.put("tanggalTagihan", bast.getTanggalTagihan() == null ? ""
						: Common.dateFormat1.get().format(bast.getTanggalTagihan()));
				o.put("status", st);
				o.put("dibayarPo", po == null || po.getDibayar() == null ? 0 : po.getDibayar());
				arr.put(o);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("total", cocok);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Sebuah BAST dianggap sudah ditagihkan bila nomor DAN tanggal fakturnya lengkap. */
	private static boolean sudahDitagih(PenerimaanPengadaanMasterAsset bast) {
		return bast.getKodeTagihan() != null && !bast.getKodeTagihan().trim().isEmpty()
				&& bast.getTanggalTagihan() != null;
	}

	/**
	 * Terima tagihan vendor atas sebuah BAST: mencapkan nomor dan tanggal faktur.
	 *
	 * <p>Keduanya WAJIB -- sama dengan syarat tombol "Simpan dan Terima Tagihan" pada layar
	 * ZKoss, karena nomor faktur adalah rujukan pembayaran dan penagihan ulang. BAST yang
	 * belum disetujui tidak dapat ditagihkan: barang yang belum diakui diterima tidak boleh
	 * menimbulkan kewajiban bayar.</p>
	 */
	public static void tagihanTerima(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehAksi(tbmuser, KUNCI_TAGIHAN, "update")) {
			tolak(hasil, "Grup pengguna Anda tidak memiliki hak menerima tagihan vendor.");
			return;
		}
		if (tbmuser == null) {
			tolak(hasil, "Sesi pengguna tidak dikenali, silakan masuk ulang.");
			return;
		}
		Long id = (request == null || request.isNull("id")) ? null
				: Long.valueOf((request.get("id") + "").trim());
		if (id == null) {
			tolak(hasil, "Parameter id wajib diisi.");
			return;
		}
		String kodeTagihan = request.optString("kodeTagihan", "").trim();
		String tanggalTagihan = request.optString("tanggalTagihan", "").trim();
		if (kodeTagihan.isEmpty()) {
			tolak(hasil, "Nomor tagihan/faktur vendor wajib diisi sesuai dokumen yang diterima.");
			return;
		}
		if (tanggalTagihan.isEmpty()) {
			tolak(hasil, "Tanggal tagihan wajib diisi sesuai tanggal pada faktur vendor.");
			return;
		}
		java.util.Date tglTagihan = tanggalKetat(tanggalTagihan);
		if (tglTagihan == null) {
			tolak(hasil, "Tanggal tagihan harus berformat hh-bb-tttt, mis. 10-08-2026.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			PenerimaanPengadaanMasterAsset bast = (PenerimaanPengadaanMasterAsset) session
					.get(PenerimaanPengadaanMasterAsset.class, id);
			if (bast == null) {
				tolak(hasil, "Penerimaan Barang tidak ditemukan.");
				return;
			}
			Long tokoId = tokoLingkup(tbmuser, request);
			if (tokoId != null && bast.getToko() != null && !tokoId.equals(bast.getToko().getId())) {
				tolak(hasil, "Penerimaan Barang ini milik toko lain.");
				return;
			}
			if (bast.getTanggalPersetujuan() == null) {
				tolak(hasil, "Tagihan hanya dapat diterima atas Penerimaan Barang yang sudah disetujui.");
				return;
			}
			// Nomor faktur yang sama pada penyedia yang sama menandakan tagihan ganda --
			// kesalahan yang baru ketahuan saat rekonsiliasi pembayaran bila didiamkan.
			Criteria kembar = session.createCriteria(PenerimaanPengadaanMasterAsset.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
					.add(Restrictions.eq("kodeTagihan", kodeTagihan))
					.add(Restrictions.ne("id", bast.getId()));
			if (bast.getPenyedia() != null) {
				kembar.add(Restrictions.eq("penyedia.id", bast.getPenyedia().getId()));
			}
			@SuppressWarnings("unchecked")
			List<PenerimaanPengadaanMasterAsset> bentrok = kembar.list();
			if (!bentrok.isEmpty()) {
				tolak(hasil, "Nomor tagihan " + kodeTagihan + " sudah dipakai pada "
						+ bentrok.get(0).getKode() + " untuk penyedia yang sama. "
						+ "Periksa kembali agar tidak terjadi tagihan ganda.");
				return;
			}
			session.beginTransaction();
			bast.setKodeTagihan(kodeTagihan);
			bast.setTanggalTagihan(tglTagihan);
			bast.setOleh(tbmuser.getUserNama());
			bast.setOlehId(tbmuser.getUserId());
			session.saveOrUpdate(bast);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("id", bast.getId());
			hasil.put("kode", bast.getKode());
			hasil.put("kodeTagihan", kodeTagihan);
			hasil.put("statusTagihan", "SUDAH");
		} catch (Exception e) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception eRollback) {
				ais.common.ErrorAuditUtil.record(eRollback, "PengadaanPosApiHelper.tagihanTerima rollback");
			}
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Batalkan tagihan vendor: mengosongkan nomor dan tanggal faktur pada BAST.
	 *
	 * <p>Ditolak bila Pemesanan Pembelian induknya SUDAH menerima pembayaran -- membuang
	 * rujukan faktur atas pesanan yang sudah dibayar akan memutus jejak rekonsiliasi.
	 * Penerimaan langsung (tanpa PO) tidak terkena pagar ini karena tidak ada pembayaran
	 * termin yang menunjuknya.</p>
	 */
	public static void tagihanBatal(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehAksi(tbmuser, KUNCI_TAGIHAN, "delete")) {
			tolak(hasil, "Grup pengguna Anda tidak memiliki hak membatalkan tagihan vendor.");
			return;
		}
		Long id = (request == null || request.isNull("id")) ? null
				: Long.valueOf((request.get("id") + "").trim());
		if (id == null) {
			tolak(hasil, "Parameter id wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			PenerimaanPengadaanMasterAsset bast = (PenerimaanPengadaanMasterAsset) session
					.get(PenerimaanPengadaanMasterAsset.class, id);
			if (bast == null) {
				tolak(hasil, "Penerimaan Barang tidak ditemukan.");
				return;
			}
			Long tokoId = tokoLingkup(tbmuser, request);
			if (tokoId != null && bast.getToko() != null && !tokoId.equals(bast.getToko().getId())) {
				tolak(hasil, "Penerimaan Barang ini milik toko lain.");
				return;
			}
			if (!sudahDitagih(bast)) {
				tolak(hasil, "Penerimaan Barang ini belum memiliki tagihan yang dapat dibatalkan.");
				return;
			}
			PemesananPengadaanMasterAsset po = bast.getPemesananPengadaanMasterAsset();
			if (po != null && po.getDibayar() != null && po.getDibayar().doubleValue() > 0) {
				tolak(hasil, "Tagihan tidak dapat dibatalkan karena Pemesanan Pembelian "
						+ (po.getKode() == null ? "" : po.getKode()) + " sudah menerima pembayaran sebesar "
						+ Common.numberFormat.get().format(po.getDibayar())
						+ ". Batalkan pembayarannya terlebih dahulu bila memang perlu dikoreksi.");
				return;
			}
			session.beginTransaction();
			bast.setKodeTagihan(null);
			bast.setTanggalTagihan(null);
			if (tbmuser != null) {
				bast.setOleh(tbmuser.getUserNama());
				bast.setOlehId(tbmuser.getUserId());
			}
			session.saveOrUpdate(bast);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("id", bast.getId());
			hasil.put("statusTagihan", "BELUM");
		} catch (Exception e) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception eRollback) {
				ais.common.ErrorAuditUtil.record(eRollback, "PengadaanPosApiHelper.tagihanBatal rollback");
			}
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}


	/**
	 * Cocokkan baris tempelan/Excel ke Produk POS secara massal -- penopang bulk entry
	 * PR/PO/BAST, sepadan dengan "Cek Produk Existing" pada Bulk Entry Kulakan.
	 *
	 * <p>Param: {@code baris} berisi array objek dengan {@code kode} dan/atau {@code nama}.
	 * Setiap baris dikembalikan apa adanya beserta hasil pencocokan, sehingga klien dapat
	 * menandai mana yang sudah dikenal dan mana yang perlu diperbaiki SEBELUM menyimpan.</p>
	 *
	 * <p>Urutan pencocokan: kode/barcode persis, lalu nama persis, lalu nama mengandung.
	 * Bila lebih dari satu produk cocok pada tahap terakhir, baris ditandai
	 * {@code ganda} agar pengguna memilih sendiri -- menebak diam-diam berisiko salah barang.</p>
	 */
	public static void barangResolve(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehLihat(tbmuser, KUNCI_PR) && !bolehLihat(tbmuser, KUNCI_PO)
				&& !bolehLihat(tbmuser, KUNCI_BAST)) {
			tolak(hasil, "Menu Pengadaan tidak diaktifkan untuk grup pengguna Anda.");
			return;
		}
		JSONArray baris = request == null ? null : request.optJSONArray("baris");
		if (baris == null || baris.length() == 0) {
			tolak(hasil, "Tidak ada baris yang dapat dicocokkan.");
			return;
		}
		if (baris.length() > 2000) {
			tolak(hasil, "Terlalu banyak baris sekaligus (maksimal 2000). Bagi menjadi beberapa unggahan.");
			return;
		}
		Long tokoId = tokoLingkup(tbmuser, request);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			JSONArray arr = new JSONArray();
			int ketemu = 0;
			int ganda = 0;
			for (int i = 0; i < baris.length(); i++) {
				JSONObject src = baris.optJSONObject(i);
				if (src == null) {
					continue;
				}
				String kode = src.optString("kode", "").trim();
				String nama = src.optString("nama", "").trim();
				JSONObject o = new JSONObject();
				o.put("baris", i + 1);
				o.put("kode", kode);
				o.put("nama", nama);
				o.put("jumlah", angkaAman(src, "jumlah"));
				o.put("hargaBeli", angkaAman(src, "hargaBeli"));
				o.put("keterangan", src.optString("keterangan", "").trim());

				java.util.List<Produk> cocok = cariProdukCocok(session, tokoId, kode, nama);
				if (cocok.size() == 1) {
					Produk p = cocok.get(0);
					ketemu++;
					o.put("produk_id", p.getId());
					o.put("kodeProduk", p.getKode() == null ? "" : p.getKode());
					o.put("namaProduk", p.getNama() == null ? "" : p.getNama());
					if (angkaAman(src, "hargaBeli") <= 0 && p.getHargaBeli() != null) {
						o.put("hargaBeli", p.getHargaBeli());
					}
					o.put("statusCocok", "COCOK");
				} else if (cocok.size() > 1) {
					ganda++;
					o.put("statusCocok", "GANDA");
					o.put("catatan", "Ada " + cocok.size() + " produk yang cocok, pilih sendiri barangnya.");
				} else {
					o.put("statusCocok", "TIDAK ADA");
					o.put("catatan", "Produk tidak ditemukan pada toko ini.");
				}
				arr.put(o);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("jumlahBaris", arr.length());
			hasil.put("jumlahCocok", ketemu);
			hasil.put("jumlahGanda", ganda);
			hasil.put("jumlahTidakAda", arr.length() - ketemu - ganda);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Cari produk yang cocok untuk satu baris tempelan; lihat {@link #barangResolve}. */
	private static java.util.List<Produk> cariProdukCocok(Session session, Long tokoId, String kode, String nama) {
		if ((kode == null || kode.isEmpty()) && (nama == null || nama.isEmpty())) {
			return new java.util.ArrayList<Produk>();
		}
		if (kode != null && !kode.isEmpty()) {
			@SuppressWarnings("unchecked")
			java.util.List<Produk> lewatKode = kriteriaProduk(session, tokoId)
					.add(Restrictions.eq("kode", kode)).setMaxResults(5).list();
			if (!lewatKode.isEmpty()) {
				return lewatKode;
			}
		}
		if (nama != null && !nama.isEmpty()) {
			@SuppressWarnings("unchecked")
			java.util.List<Produk> persis = kriteriaProduk(session, tokoId)
					.add(Restrictions.eq("nama", nama).ignoreCase()).setMaxResults(5).list();
			if (!persis.isEmpty()) {
				return persis;
			}
			@SuppressWarnings("unchecked")
			java.util.List<Produk> mengandung = kriteriaProduk(session, tokoId)
					.add(Restrictions.ilike("nama", nama, MatchMode.ANYWHERE)).setMaxResults(5).list();
			return mengandung;
		}
		return new java.util.ArrayList<Produk>();
	}

	/** Kriteria dasar produk aktif pada lingkup toko. */
	private static Criteria kriteriaProduk(Session session, Long tokoId) {
		Criteria k = session.createCriteria(Produk.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)));
		if (tokoId != null) {
			k.add(Restrictions.eq("toko.id", tokoId));
		}
		return k;
	}


	/**
	 * Sisa kewajiban sebuah PO: nilai dikurangi yang SUDAH DIBAYAR menurut perhitungan
	 * kanonik entitas ({@code hitungDibayar}), yang hanya mengakui dokumen pembayaran
	 * ber-persetujuan. Dipakai bersama oleh daftar tagihan terbuka dan pagar penyimpanan
	 * agar keduanya tidak pernah berbeda definisi.
	 */
	private static double sisaTagihanPo(Session session, PemesananPengadaanMasterAsset po) {
		if (po == null) {
			return 0;
		}
		double nilai = po.getNilai() == null ? 0 : po.getNilai().doubleValue();
		double dibayar;
		try {
			Double d = po.hitungDibayar(session);
			dibayar = d == null ? 0 : d.doubleValue();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "PengadaanPosApiHelper.sisaTagihanPo");
			dibayar = po.getDibayar() == null ? 0 : po.getDibayar().doubleValue();
		}
		return nilai - dibayar;
	}

	/**
	 * Selaraskan kolom {@code dibayar} dan {@code lunas} pada PO dengan perhitungan
	 * kanonik entitas. Dipanggil setiap kali dokumen pembayaran disetujui, dibatalkan,
	 * atau dihapus -- sehingga kolom ringkas pada PO tidak pernah tertinggal dari
	 * kenyataan pembayarannya.
	 */
	private static void selaraskanPembayaranPo(Session session, PemesananPengadaanMasterAsset po) {
		if (po == null) {
			return;
		}
		Double d = po.hitungDibayar(session);
		double dibayar = d == null ? 0 : d.doubleValue();
		double nilai = po.getNilai() == null ? 0 : po.getNilai().doubleValue();
		po.setDibayar(Double.valueOf(dibayar));
		po.setLunas(Boolean.valueOf(nilai > 0 && dibayar >= nilai - TOLERANSI));
		session.saveOrUpdate(po);
	}

	/** Nilai yang sudah dibayar untuk satu termin tertentu pada sebuah PO. */
	private static double terbayarTermin(Session session, Long poId, String kunciTermin, Long kecualiBayarId) {
		@SuppressWarnings("unchecked")
		List<PembayaranTerminMasterAssetDetail> daftar = session
				.createCriteria(PembayaranTerminMasterAssetDetail.class)
				.add(Restrictions.eq("pemesananPengadaanMasterAsset.id", poId)).list();
		double jml = 0;
		for (PembayaranTerminMasterAssetDetail b : daftar) {
			PembayaranTerminMasterAsset induk = b.getPembayaranTerminMasterAsset();
			if (induk == null || induk.getDisetujuiOleh() == null
					|| Boolean.FALSE.equals(induk.getAktif())) {
				continue;
			}
			if (kecualiBayarId != null && kecualiBayarId.equals(induk.getId())) {
				continue;
			}
			String kunci = "";
			if (b.getTagihan() != null && !b.getTagihan().trim().isEmpty()) {
				try {
					JSONObject t = new JSONObject(b.getTagihan());
					kunci = t.isNull("key") ? "" : (t.get("key") + "").trim();
				} catch (Exception e) {
					kunci = "";
				}
			}
			if (kunciTermin == null ? kunci.isEmpty() : kunciTermin.equals(kunci)) {
				jml += b.getDibayar() == null ? 0 : b.getDibayar().doubleValue();
			}
		}
		return jml;
	}

	/** Label status dokumen pembayaran vendor. */
	private static String statusBayar(PembayaranTerminMasterAsset bayar) {
		return bayar.getDisetujuiOleh() != null ? "DISETUJUI" : "DRAFT";
	}

	/**
	 * Daftar dokumen pembayaran vendor pada lingkup toko. Param opsional: {@code cari}
	 * (kode/keterangan), {@code status} (DRAFT/DISETUJUI), {@code page}, {@code pageSize}.
	 */
	public static void bayarDaftar(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehLihat(tbmuser, KUNCI_DPC)) {
			tolak(hasil, "Menu Pengadaan tidak diaktifkan untuk grup pengguna Anda.");
			return;
		}
		Long tokoId = tokoLingkup(tbmuser, request);
		int page = Math.max(1, request == null ? 1 : request.optInt("page", 1));
		int pageSize = Math.min(100, Math.max(5, request == null ? 15 : request.optInt("pageSize", 15)));
		String cari = request == null ? "" : request.optString("cari", "").trim();
		String status = request == null ? "" : request.optString("status", "").trim().toUpperCase();
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Criteria kriteria = session.createCriteria(PembayaranTerminMasterAsset.class);
			kriteria.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)));
			if (tokoId != null) {
				kriteria.add(Restrictions.eq("toko.id", tokoId));
			}
			if (cari.length() > 0) {
				kriteria.add(Restrictions.disjunction()
						.add(Restrictions.ilike("kode", cari, MatchMode.ANYWHERE))
						.add(Restrictions.ilike("keterangan", cari, MatchMode.ANYWHERE)));
			}
			kriteria.addOrder(Order.desc("id"));
			@SuppressWarnings("unchecked")
			List<PembayaranTerminMasterAsset> semua = kriteria.list();
			JSONArray arr = new JSONArray();
			int cocok = 0;
			int mulai = (page - 1) * pageSize;
			for (PembayaranTerminMasterAsset b : semua) {
				String st = statusBayar(b);
				if (status.length() > 0 && !status.equals(st)) {
					continue;
				}
				cocok++;
				if (cocok <= mulai || arr.length() >= pageSize) {
					continue;
				}
				JSONObject o = new JSONObject();
				o.put("id", b.getId());
				o.put("kode", b.getKode() == null ? "" : b.getKode());
				o.put("keterangan", b.getKeterangan() == null ? "" : b.getKeterangan());
				o.put("tanggal", b.getTanggalPembuatan() == null ? JSONObject.NULL
						: Common.dateFormat3.get().format(b.getTanggalPembuatan()));
				o.put("penyedia", b.getPenyedia() == null ? "" : b.getPenyedia().getNama());
				o.put("nilai", b.getNilaiDibayar() == null ? 0 : b.getNilaiDibayar());
				o.put("status", st);
				o.put("dibuatOleh", b.getDibuatOleh() == null ? "" : b.getDibuatOleh().getUserNama());
				o.put("disetujuiOleh", b.getDisetujuiOleh() == null ? "" : b.getDisetujuiOleh().getUserNama());
				arr.put(o);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("total", cocok);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Tagihan terbuka seorang penyedia: PO yang sudah disetujui dan masih menyisakan
	 * kewajiban bayar. Untuk PO bertermin, sisa dirinci per termin sehingga pembayaran
	 * dapat menunjuk termin yang mana -- rujukan itulah yang kemudian dibaca layar PO.
	 *
	 * <p>Tidak menulis apa pun; klien memakai hasilnya sebagai isian dokumen pembayaran.</p>
	 */
	public static void bayarTagihanTerbuka(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehLihat(tbmuser, KUNCI_DPC)) {
			tolak(hasil, "Menu Pengadaan tidak diaktifkan untuk grup pengguna Anda.");
			return;
		}
		PenyediaAsset penyedia = null;
		Long kecuali = (request == null || request.isNull("kecuali_bayar_id")) ? null
				: Long.valueOf((request.get("kecuali_bayar_id") + "").trim());
		Long tokoId = tokoLingkup(tbmuser, request);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			penyedia = penyediaDokumen(session, request);
			if (penyedia == null) {
				tolak(hasil, "Penyedia/vendor wajib dipilih untuk melihat tagihan terbukanya.");
				return;
			}
			Criteria kriteria = session.createCriteria(PemesananPengadaanMasterAsset.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
					.add(Restrictions.isNotNull("tanggalPersetujuan"))
					.add(Restrictions.eq("penyedia.id", penyedia.getId()));
			if (tokoId != null) {
				kriteria.add(Restrictions.eq("toko.id", tokoId));
			}
			kriteria.addOrder(Order.asc("id"));
			@SuppressWarnings("unchecked")
			List<PemesananPengadaanMasterAsset> daftar = kriteria.list();
			JSONArray arr = new JSONArray();
			double totalSisa = 0;
			for (PemesananPengadaanMasterAsset po : daftar) {
				double sisaPo = sisaTagihanPo(session, po);
				if (sisaPo <= TOLERANSI) {
					continue;
				}
				if (Boolean.TRUE.equals(po.getByTermin())) {
					JSONArray termin = terminDari(po);
					for (int i = 0; i < termin.length(); i++) {
						JSONObject t = termin.optJSONObject(i);
						if (t == null) {
							continue;
						}
						String kunci = t.isNull("key") ? "" : (t.get("key") + "").trim();
						double tagih = angkaAman(t, "penagihan");
						double sudah = terbayarTermin(session, po.getId(), kunci, kecuali);
						double sisa = tagih - sudah;
						if (sisa <= TOLERANSI) {
							continue;
						}
						JSONObject o = new JSONObject();
						o.put("po_id", po.getId());
						o.put("po", po.getKode() == null ? "" : po.getKode());
						o.put("termin_key", kunci);
						o.put("termin", t.isNull("nama") ? "" : t.get("nama") + "");
						o.put("jatuhTempo", t.isNull("tanggalD") ? "" : t.get("tanggalD") + "");
						o.put("nilaiTagih", tagih);
						o.put("sudahDibayar", sudah);
						o.put("sisa", sisa);
						o.put("dibayar", sisa);
						arr.put(o);
						totalSisa += sisa;
					}
				} else {
					JSONObject o = new JSONObject();
					o.put("po_id", po.getId());
					o.put("po", po.getKode() == null ? "" : po.getKode());
					o.put("termin_key", "");
					o.put("termin", "Tanpa termin");
					o.put("jatuhTempo", po.getPengirimanPalingLambat() == null ? ""
							: Common.dateFormat1.get().format(po.getPengirimanPalingLambat()));
					o.put("nilaiTagih", po.getNilai() == null ? 0 : po.getNilai());
					o.put("sudahDibayar", (po.getNilai() == null ? 0 : po.getNilai().doubleValue()) - sisaPo);
					o.put("sisa", sisaPo);
					o.put("dibayar", sisaPo);
					arr.put(o);
					totalSisa += sisaPo;
				}
			}
			hasil.put("status", "00");
			hasil.put("penyedia_id", penyedia.getId());
			hasil.put("penyedia", penyedia.getNama() == null ? "" : penyedia.getNama());
			hasil.put("data", arr);
			hasil.put("totalSisa", totalSisa);
			if (arr.length() == 0) {
				hasil.put("catatan", "Tidak ada tagihan terbuka untuk penyedia ini.");
			}
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Detail satu dokumen pembayaran vendor: header + baris tagihan yang dibayar. */
	public static void bayarDetail(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehLihat(tbmuser, KUNCI_DPC)) {
			tolak(hasil, "Menu Pengadaan tidak diaktifkan untuk grup pengguna Anda.");
			return;
		}
		Long id = (request == null || request.isNull("id")) ? null
				: Long.valueOf((request.get("id") + "").trim());
		if (id == null) {
			tolak(hasil, "Parameter id wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			PembayaranTerminMasterAsset bayar = (PembayaranTerminMasterAsset) session
					.get(PembayaranTerminMasterAsset.class, id);
			if (bayar == null) {
				tolak(hasil, "Dokumen pembayaran tidak ditemukan.");
				return;
			}
			Long tokoId = tokoLingkup(tbmuser, request);
			if (tokoId != null && bayar.getToko() != null && !tokoId.equals(bayar.getToko().getId())) {
				tolak(hasil, "Dokumen pembayaran ini milik toko lain.");
				return;
			}
			JSONObject h = new JSONObject();
			h.put("id", bayar.getId());
			h.put("kode", bayar.getKode() == null ? "" : bayar.getKode());
			h.put("keterangan", bayar.getKeterangan() == null ? "" : bayar.getKeterangan());
			h.put("tanggal", bayar.getTanggalPembuatan() == null ? JSONObject.NULL
					: Common.dateFormat3.get().format(bayar.getTanggalPembuatan()));
			h.put("status", statusBayar(bayar));
			h.put("nilai", bayar.getNilaiDibayar() == null ? 0 : bayar.getNilaiDibayar());
			h.put("penyedia_id", bayar.getPenyedia() == null ? JSONObject.NULL : bayar.getPenyedia().getId());
			h.put("penyedia", bayar.getPenyedia() == null ? "" : bayar.getPenyedia().getNama());
			h.put("toko_id", bayar.getToko() == null ? JSONObject.NULL : bayar.getToko().getId());
			h.put("dibuatOleh", bayar.getDibuatOleh() == null ? "" : bayar.getDibuatOleh().getUserNama());
			h.put("disetujuiOleh", bayar.getDisetujuiOleh() == null ? "" : bayar.getDisetujuiOleh().getUserNama());

			@SuppressWarnings("unchecked")
			List<PembayaranTerminMasterAssetDetail> baris = session
					.createCriteria(PembayaranTerminMasterAssetDetail.class)
					.add(Restrictions.eq("pembayaranTerminMasterAsset.id", bayar.getId()))
					.addOrder(Order.asc("id")).list();
			JSONArray arr = new JSONArray();
			for (PembayaranTerminMasterAssetDetail d : baris) {
				PemesananPengadaanMasterAsset po = d.getPemesananPengadaanMasterAsset();
				String kunci = "";
				if (d.getTagihan() != null && !d.getTagihan().trim().isEmpty()) {
					try {
						JSONObject t = new JSONObject(d.getTagihan());
						kunci = t.isNull("key") ? "" : (t.get("key") + "").trim();
					} catch (Exception e) {
						kunci = "";
					}
				}
				JSONObject o = new JSONObject();
				o.put("id", d.getId());
				o.put("po_id", po == null ? JSONObject.NULL : po.getId());
				o.put("po", po == null || po.getKode() == null ? "" : po.getKode());
				o.put("termin_key", kunci);
				o.put("termin", namaTermin(po, kunci));
				o.put("dibayar", d.getDibayar() == null ? 0 : d.getDibayar());
				o.put("keterangan", d.getKeterangan() == null ? "" : d.getKeterangan());
				// Sisa dihitung ulang tanpa memperhitungkan dokumen ini sendiri, supaya
				// penyuntingan tidak menuduh dirinya sendiri melebihi tagihan.
				double tagih = nilaiTagihanTermin(po, kunci);
				double lain = po == null ? 0 : terbayarTermin(session, po.getId(), kunci, bayar.getId());
				o.put("nilaiTagih", tagih);
				o.put("sudahDibayar", lain);
				o.put("sisa", Math.max(0, tagih - lain));
				arr.put(o);
			}
			hasil.put("status", "00");
			hasil.put("header", h);
			hasil.put("detail", arr);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Nama termin sebuah PO berdasarkan kuncinya; kosong berarti PO tanpa termin. */
	private static String namaTermin(PemesananPengadaanMasterAsset po, String kunci) throws Exception {
		if (po == null) {
			return "";
		}
		if (kunci == null || kunci.isEmpty()) {
			return "Tanpa termin";
		}
		JSONArray termin = terminDari(po);
		for (int i = 0; i < termin.length(); i++) {
			JSONObject t = termin.optJSONObject(i);
			if (t != null && !t.isNull("key") && kunci.equals((t.get("key") + "").trim())) {
				return t.isNull("nama") ? "" : t.get("nama") + "";
			}
		}
		return "";
	}

	/** Nilai tagihan sebuah termin; untuk PO tanpa termin dipakai nilai PO. */
	private static double nilaiTagihanTermin(PemesananPengadaanMasterAsset po, String kunci) throws Exception {
		if (po == null) {
			return 0;
		}
		if (kunci == null || kunci.isEmpty()) {
			return po.getNilai() == null ? 0 : po.getNilai().doubleValue();
		}
		JSONArray termin = terminDari(po);
		for (int i = 0; i < termin.length(); i++) {
			JSONObject t = termin.optJSONObject(i);
			if (t != null && !t.isNull("key") && kunci.equals((t.get("key") + "").trim())) {
				return angkaAman(t, "penagihan");
			}
		}
		return 0;
	}

	/**
	 * Tambah/ubah dokumen pembayaran vendor beserta barisnya dalam SATU transaksi.
	 *
	 * <p>Aturan yang ditegakkan server:</p>
	 * <ul>
	 * <li>Hanya PO yang SUDAH DISETUJUI yang dapat dibayar.</li>
	 * <li>Nilai bayar per baris tidak boleh melebihi sisa tagihannya; sisa dihitung tanpa
	 * memperhitungkan dokumen ini sendiri sehingga penyuntingan tidak menuduh diri sendiri.</li>
	 * <li>Setiap baris disimpan DICENTANG ({@code pilih}) -- entitas memaksa nilai bayar nol
	 * pada baris yang tidak dicentang, sehingga baris tanpa centang akan diam-diam hilang.</li>
	 * <li>Dokumen yang sudah disetujui tidak dapat diubah.</li>
	 * </ul>
	 */
	public static void bayarSimpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Long id = (request == null || request.isNull("id") || (request.get("id") + "").trim().isEmpty())
				? null : Long.valueOf((request.get("id") + "").trim());
		if (!bolehAksi(tbmuser, KUNCI_DPC, id == null ? "create" : "update")) {
			tolak(hasil, "Grup pengguna Anda tidak memiliki hak "
					+ (id == null ? "membuat" : "mengubah") + " pembayaran vendor.");
			return;
		}
		if (tbmuser == null) {
			tolak(hasil, "Sesi pengguna tidak dikenali, silakan masuk ulang.");
			return;
		}
		JSONArray detail = request == null ? null : request.optJSONArray("detail");
		if (detail == null || detail.length() == 0) {
			tolak(hasil, "Pembayaran harus memiliki minimal satu baris tagihan.");
			return;
		}
		Long tokoId = tokoLingkup(tbmuser, request);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			PembayaranTerminMasterAsset bayar;
			if (id != null) {
				bayar = (PembayaranTerminMasterAsset) session.get(PembayaranTerminMasterAsset.class, id);
				if (bayar == null) {
					tolak(hasil, "Dokumen pembayaran tidak ditemukan.");
					return;
				}
				if (tokoId != null && bayar.getToko() != null && !tokoId.equals(bayar.getToko().getId())) {
					tolak(hasil, "Dokumen pembayaran ini milik toko lain.");
					return;
				}
				if (bayar.getDisetujuiOleh() != null) {
					tolak(hasil, "Pembayaran yang sudah disetujui tidak dapat diubah. "
							+ "Batalkan persetujuannya terlebih dahulu bila memang perlu dikoreksi.");
					return;
				}
			} else {
				bayar = new PembayaranTerminMasterAsset();
				bayar.setTanggalPembuatan(ais.ui.util.WaktuUtil.getDate());
				bayar.setDibuatOleh(tbmuser);
				bayar.setAktif(Boolean.TRUE);
			}
			PenyediaAsset penyedia = penyediaDokumen(session, request);
			if (penyedia == null) {
				tolak(hasil, "Penyedia/vendor wajib dipilih pada dokumen pembayaran.");
				return;
			}
			bayar.setPenyedia(penyedia);
			if (tokoId != null) {
				bayar.setToko((Toko) session.get(Toko.class, tokoId));
			}
			bayar.setKeterangan(request.optString("keterangan", "").trim());
			if (!request.isNull("tanggal")) {
				try {
					bayar.setTanggalPembuatan(Common.dateFormat3.get().parse((request.get("tanggal") + "").trim()));
				} catch (Exception e) {
					// Format tanggal tidak dikenali -> pertahankan nilai yang sudah ada.
				}
			}
			if (bayar.getKode() == null || bayar.getKode().trim().isEmpty()) {
				bayar.setKode(buatKodeUmum(session, PembayaranTerminMasterAsset.class, "BYR", tokoId));
			}
			bayar.setOleh(tbmuser.getUserNama());
			bayar.setOlehId(tbmuser.getUserId());

			// Validasi seluruh baris SEBELUM menulis, agar dokumen yang ditolak tidak
			// meninggalkan jejak separuh jadi.
			for (int i = 0; i < detail.length(); i++) {
				JSONObject b = detail.optJSONObject(i);
				if (b == null || b.isNull("po_id")) {
					continue;
				}
				PemesananPengadaanMasterAsset po = (PemesananPengadaanMasterAsset) session
						.get(PemesananPengadaanMasterAsset.class, Long.valueOf((b.get("po_id") + "").trim()));
				if (po == null) {
					tolak(hasil, "Pemesanan Pembelian pada baris ke-" + (i + 1) + " tidak ditemukan.");
					return;
				}
				if (po.getTanggalPersetujuan() == null) {
					tolak(hasil, "Pemesanan Pembelian " + (po.getKode() == null ? "" : po.getKode())
							+ " belum disetujui sehingga belum dapat dibayar.");
					return;
				}
				if (penyedia != null && po.getPenyedia() != null
						&& !penyedia.getId().equals(po.getPenyedia().getId())) {
					tolak(hasil, "Pemesanan Pembelian " + (po.getKode() == null ? "" : po.getKode())
							+ " bukan milik penyedia yang dipilih.");
					return;
				}
				String kunci = b.optString("termin_key", "").trim();
				double nilaiBayar = angkaAman(b, "dibayar");
				if (nilaiBayar <= 0) {
					tolak(hasil, "Nilai bayar pada baris ke-" + (i + 1) + " harus lebih besar dari nol.");
					return;
				}
				double tagih = nilaiTagihanTermin(po, kunci);
				double lain = terbayarTermin(session, po.getId(), kunci, bayar.getId());
				double sisa = tagih - lain;
				if (nilaiBayar > sisa + TOLERANSI) {
					tolak(hasil, "Nilai bayar " + Common.numberFormat.get().format(nilaiBayar)
							+ " pada " + (po.getKode() == null ? "PO" : po.getKode())
							+ " melebihi sisa tagihannya " + Common.numberFormat.get().format(Math.max(0, sisa)) + ".");
					return;
				}
			}

			session.beginTransaction();
			session.saveOrUpdate(bayar);
			session.flush();

			@SuppressWarnings("unchecked")
			List<PembayaranTerminMasterAssetDetail> lama = session
					.createCriteria(PembayaranTerminMasterAssetDetail.class)
					.add(Restrictions.eq("pembayaranTerminMasterAsset.id", bayar.getId())).list();
			java.util.Set<Long> poTersentuh = new java.util.HashSet<Long>();
			for (PembayaranTerminMasterAssetDetail d : lama) {
				if (d.getPemesananPengadaanMasterAsset() != null) {
					poTersentuh.add(d.getPemesananPengadaanMasterAsset().getId());
				}
				session.delete(d);
			}
			session.flush();

			double total = 0;
			for (int i = 0; i < detail.length(); i++) {
				JSONObject b = detail.getJSONObject(i);
				if (b.isNull("po_id")) {
					continue;
				}
				PemesananPengadaanMasterAsset po = (PemesananPengadaanMasterAsset) session
						.get(PemesananPengadaanMasterAsset.class, Long.valueOf((b.get("po_id") + "").trim()));
				if (po == null) {
					continue;
				}
				String kunci = b.optString("termin_key", "").trim();
				double nilaiBayar = angkaAman(b, "dibayar");
				PembayaranTerminMasterAssetDetail d = new PembayaranTerminMasterAssetDetail();
				d.setPembayaranTerminMasterAsset(bayar);
				d.setPemesananPengadaanMasterAsset(po);
				// WAJIB dicentang: getDibayar() pada entitas memaksa nol bila pilih=false,
				// sehingga baris tanpa centang tersimpan tetapi bernilai nol.
				d.setPilih(Boolean.TRUE);
				d.setDibayar(Double.valueOf(nilaiBayar));
				d.setPinalti(Double.valueOf(angkaAman(b, "pinalti")));
				d.setKeterangan(b.optString("keterangan", "").trim());
				d.setTanggalDibayar(bayar.getTanggalPembuatan());
				if (!kunci.isEmpty()) {
					JSONObject tagihan = new JSONObject();
					tagihan.put("key", kunci);
					d.setTagihan(tagihan.toString());
				}
				d.setOleh(tbmuser.getUserNama());
				d.setOlehId(tbmuser.getUserId());
				session.save(d);
				poTersentuh.add(po.getId());
				total += nilaiBayar;
			}
			bayar.setNilaiDibayar(Double.valueOf(total));
			session.saveOrUpdate(bayar);
			session.flush();

			// Dokumen DRAF belum diakui pembayaran, tetapi PO yang barisnya dilepas saat
			// penyuntingan tetap perlu diselaraskan supaya kolom ringkasnya tidak basi.
			for (Long poId : poTersentuh) {
				selaraskanPembayaranPo(session,
						(PemesananPengadaanMasterAsset) session.get(PemesananPengadaanMasterAsset.class, poId));
			}
			session.getTransaction().commit();

			hasil.put("status", "00");
			hasil.put("id", bayar.getId());
			hasil.put("kode", bayar.getKode());
			hasil.put("nilai", total);
		} catch (Exception e) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception eRollback) {
				ais.common.ErrorAuditUtil.record(eRollback, "PengadaanPosApiHelper.bayarSimpan rollback");
			}
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Keputusan atas dokumen pembayaran: setujui atau batalkan. Persetujuanlah yang membuat
	 * pembayaran DIAKUI -- {@code hitungDibayar} pada PO hanya menghitung dokumen yang sudah
	 * disetujui -- karena itu kolom {@code dibayar}/{@code lunas} PO diselaraskan di sini.
	 */
	public static void bayarPutusan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehAksi(tbmuser, KUNCI_DPC, "approve")) {
			tolak(hasil, "Grup pengguna Anda tidak memiliki hak menyetujui pembayaran vendor.");
			return;
		}
		Long id = (request == null || request.isNull("id")) ? null
				: Long.valueOf((request.get("id") + "").trim());
		if (id == null) {
			tolak(hasil, "Parameter id wajib diisi.");
			return;
		}
		String keputusan = request.optString("keputusan", "").trim().toUpperCase();
		if (!"SETUJUI".equals(keputusan) && !"BATAL".equals(keputusan)) {
			tolak(hasil, "Keputusan untuk pembayaran hanya SETUJUI atau BATAL.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			PembayaranTerminMasterAsset bayar = (PembayaranTerminMasterAsset) session
					.get(PembayaranTerminMasterAsset.class, id);
			if (bayar == null) {
				tolak(hasil, "Dokumen pembayaran tidak ditemukan.");
				return;
			}
			Long tokoId = tokoLingkup(tbmuser, request);
			if (tokoId != null && bayar.getToko() != null && !tokoId.equals(bayar.getToko().getId())) {
				tolak(hasil, "Dokumen pembayaran ini milik toko lain.");
				return;
			}
			// Pengajuan transfer bank bersifat OPSIONAL dan diputuskan saat menyetujui:
			// pembayaran tunai tidak perlu masuk antrean pencairan.
			boolean ajukanTransfer = request.optBoolean("ajukanTransfer", false);
			int transferDibuat = 0;
			int transferDitarik = 0;
			session.beginTransaction();
			if ("SETUJUI".equals(keputusan)) {
				bayar.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());
				bayar.setDisetujuiOleh(tbmuser);
			} else {
				bayar.setTanggalPersetujuan(null);
				bayar.setDisetujuiOleh(null);
			}
			bayar.setOleh(tbmuser.getUserNama());
			bayar.setOlehId(tbmuser.getUserId());
			session.saveOrUpdate(bayar);
			session.flush();
			if ("SETUJUI".equals(keputusan)) {
				if (ajukanTransfer) {
					transferDibuat = buatPengajuanTransfer(session, bayar, tbmuser);
				}
			} else {
				transferDitarik = tarikPengajuanTransfer(session, bayar);
			}
			selaraskanPoDokumenBayar(session, bayar);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("id", bayar.getId());
			hasil.put("statusDokumen", statusBayar(bayar));
			hasil.put("transferDibuat", transferDibuat);
			hasil.put("transferDitarik", transferDitarik);
		} catch (Exception e) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception eRollback) {
				ais.common.ErrorAuditUtil.record(eRollback, "PengadaanPosApiHelper.bayarPutusan rollback");
			}
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}


	/**
	 * Buat pengajuan transfer bank untuk setiap baris sebuah dokumen pembayaran yang
	 * disetujui -- padanan jalur DPC/ProsesTransfer pada versi ZKoss.
	 *
	 * <p>Pengajuan dibuat pada SAAT PERSETUJUAN, bukan saat dokumen disimpan, karena
	 * persetujuanlah yang menjadikan pembayaran sebuah kewajiban yang harus dicairkan.
	 * Rekening sumber sengaja dibiarkan kosong: pemilihan bank adalah wewenang keuangan,
	 * dan memaksa kasir menebaknya justru menghasilkan data yang harus dikoreksi.</p>
	 *
	 * <p>Baris yang sudah punya pengajuan dilewati, sehingga persetujuan berulang tidak
	 * menggandakan antrean transfer.</p>
	 */
	private static int buatPengajuanTransfer(Session session, PembayaranTerminMasterAsset bayar,
			Tbmuser tbmuser) {
		@SuppressWarnings("unchecked")
		List<PembayaranTerminMasterAssetDetail> baris = session
				.createCriteria(PembayaranTerminMasterAssetDetail.class)
				.add(Restrictions.eq("pembayaranTerminMasterAsset.id", bayar.getId())).list();
		int dibuat = 0;
		int urut = 0;
		for (PembayaranTerminMasterAssetDetail d : baris) {
			urut++;
			if (d.getDaftarPengajuanTransfer() != null) {
				continue;
			}
			double nilai = d.getDibayar() == null ? 0 : d.getDibayar().doubleValue();
			if (nilai <= 0) {
				continue;
			}
			PemesananPengadaanMasterAsset po = d.getPemesananPengadaanMasterAsset();
			String namaVendor = bayar.getPenyedia() == null ? "vendor" : bayar.getPenyedia().getNama();
			ais.database.model.akunting.DaftarPengajuanTransfer trf =
					new ais.database.model.akunting.DaftarPengajuanTransfer();
			trf.setKode((bayar.getKode() == null ? "BYR" : bayar.getKode()) + "/" + urut);
			trf.setNama("Pembayaran " + namaVendor
					+ (po == null || po.getKode() == null ? "" : " - " + po.getKode()));
			trf.setKeterangan("Diajukan dari pembayaran vendor "
					+ (bayar.getKode() == null ? "" : bayar.getKode())
					+ (bayar.getKeterangan() == null || bayar.getKeterangan().trim().isEmpty()
							? "" : " (" + bayar.getKeterangan().trim() + ")"));
			trf.setNominal(Double.valueOf(nilai));
			trf.setAktif(Boolean.TRUE);
			trf.setTransfer(Boolean.TRUE);
			trf.setTransitori(Boolean.FALSE);
			trf.setWaktu(ais.ui.util.WaktuUtil.getDate());
			trf.setPembayaranTerminMasterAssetDetail(d);
			if (tbmuser != null) {
				trf.setOleh(tbmuser.getUserNama());
				trf.setOlehId(tbmuser.getUserId());
			}
			session.save(trf);
			d.setDaftarPengajuanTransfer(trf);
			session.saveOrUpdate(d);
			dibuat++;
		}
		if (dibuat > 0) {
			session.flush();
		}
		return dibuat;
	}

	/**
	 * Tarik kembali pengajuan transfer sebuah dokumen pembayaran. Dipakai saat persetujuan
	 * dibatalkan atau dokumen dihapus: antrean transfer tidak boleh menyimpan permintaan
	 * pencairan atas pembayaran yang sudah tidak berlaku.
	 *
	 * <p>Pengajuan dinonaktifkan, bukan dihapus, supaya jejaknya tetap terbaca keuangan.</p>
	 */
	private static int tarikPengajuanTransfer(Session session, PembayaranTerminMasterAsset bayar) {
		@SuppressWarnings("unchecked")
		List<PembayaranTerminMasterAssetDetail> baris = session
				.createCriteria(PembayaranTerminMasterAssetDetail.class)
				.add(Restrictions.eq("pembayaranTerminMasterAsset.id", bayar.getId())).list();
		int ditarik = 0;
		for (PembayaranTerminMasterAssetDetail d : baris) {
			ais.database.model.akunting.DaftarPengajuanTransfer trf = d.getDaftarPengajuanTransfer();
			if (trf == null) {
				continue;
			}
			trf.setAktif(Boolean.FALSE);
			session.saveOrUpdate(trf);
			d.setDaftarPengajuanTransfer(null);
			session.saveOrUpdate(d);
			ditarik++;
		}
		if (ditarik > 0) {
			session.flush();
		}
		return ditarik;
	}

	/** Selaraskan seluruh PO yang disebut sebuah dokumen pembayaran. */
	private static void selaraskanPoDokumenBayar(Session session, PembayaranTerminMasterAsset bayar) {
		@SuppressWarnings("unchecked")
		List<PembayaranTerminMasterAssetDetail> baris = session
				.createCriteria(PembayaranTerminMasterAssetDetail.class)
				.add(Restrictions.eq("pembayaranTerminMasterAsset.id", bayar.getId())).list();
		java.util.Set<Long> po = new java.util.HashSet<Long>();
		for (PembayaranTerminMasterAssetDetail d : baris) {
			if (d.getPemesananPengadaanMasterAsset() != null) {
				po.add(d.getPemesananPengadaanMasterAsset().getId());
			}
		}
		for (Long poId : po) {
			selaraskanPembayaranPo(session,
					(PemesananPengadaanMasterAsset) session.get(PemesananPengadaanMasterAsset.class, poId));
		}
	}

	/** Hapus lunak dokumen pembayaran; PO yang disebutnya diselaraskan kembali. */
	public static void bayarHapus(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehAksi(tbmuser, KUNCI_DPC, "delete")) {
			tolak(hasil, "Grup pengguna Anda tidak memiliki hak menghapus pembayaran vendor.");
			return;
		}
		Long id = (request == null || request.isNull("id")) ? null
				: Long.valueOf((request.get("id") + "").trim());
		if (id == null) {
			tolak(hasil, "Parameter id wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			PembayaranTerminMasterAsset bayar = (PembayaranTerminMasterAsset) session
					.get(PembayaranTerminMasterAsset.class, id);
			if (bayar == null) {
				tolak(hasil, "Dokumen pembayaran tidak ditemukan.");
				return;
			}
			Long tokoId = tokoLingkup(tbmuser, request);
			if (tokoId != null && bayar.getToko() != null && !tokoId.equals(bayar.getToko().getId())) {
				tolak(hasil, "Dokumen pembayaran ini milik toko lain.");
				return;
			}
			if (bayar.getDisetujuiOleh() != null) {
				tolak(hasil, "Pembayaran yang sudah disetujui tidak dapat dihapus. "
						+ "Batalkan persetujuannya terlebih dahulu.");
				return;
			}
			session.beginTransaction();
			bayar.setAktif(Boolean.FALSE);
			bayar.setOleh(tbmuser.getUserNama());
			bayar.setOlehId(tbmuser.getUserId());
			session.saveOrUpdate(bayar);
			session.flush();
			selaraskanPoDokumenBayar(session, bayar);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("id", bayar.getId());
		} catch (Exception e) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception eRollback) {
				ais.common.ErrorAuditUtil.record(eRollback, "PengadaanPosApiHelper.bayarHapus rollback");
			}
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}


	/**
	 * Barang Dalam Proses: barang yang SUDAH DIPESAN tetapi BELUM DITERIMA.
	 *
	 * <p>Bukan dokumen tersendiri melainkan pandangan yang diturunkan dari selisih
	 * PO dan BAST -- persis definisi yang dipakai {@link #bastDariPo} untuk membatasi
	 * penerimaan, sehingga angka pada layar pemantauan tidak pernah berbeda dengan
	 * angka yang menjadi pagar saat menerima barang.</p>
	 *
	 * <p>Param opsional: {@code cari} (kode PO/nama barang), {@code penyedia_id},
	 * {@code hanyaTerlambat}, {@code page}, {@code pageSize}.</p>
	 */
	public static void bdpDaftar(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehLihat(tbmuser, KUNCI_BDP)) {
			tolak(hasil, "Menu Pengadaan tidak diaktifkan untuk grup pengguna Anda.");
			return;
		}
		Long tokoId = tokoLingkup(tbmuser, request);
		int page = Math.max(1, request == null ? 1 : request.optInt("page", 1));
		int pageSize = Math.min(200, Math.max(5, request == null ? 25 : request.optInt("pageSize", 25)));
		String cari = request == null ? "" : request.optString("cari", "").trim().toLowerCase();
		boolean hanyaTerlambat = request != null && request.optBoolean("hanyaTerlambat", false);
		Long penyediaId = (request == null || request.isNull("penyedia_id")
				|| (request.get("penyedia_id") + "").trim().isEmpty())
						? null : Long.valueOf((request.get("penyedia_id") + "").trim());
		java.util.Date hariIni = ais.ui.util.WaktuUtil.getDate();
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Criteria kriteria = session.createCriteria(PemesananPengadaanMasterAsset.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
					.add(Restrictions.isNotNull("tanggalPersetujuan"));
			if (tokoId != null) {
				kriteria.add(Restrictions.eq("toko.id", tokoId));
			}
			if (penyediaId != null) {
				kriteria.add(Restrictions.eq("penyedia.id", penyediaId));
			}
			kriteria.addOrder(Order.asc("id"));
			@SuppressWarnings("unchecked")
			List<PemesananPengadaanMasterAsset> daftarPo = kriteria.list();

			JSONArray arr = new JSONArray();
			int cocok = 0;
			int mulai = (page - 1) * pageSize;
			double totalNilai = 0;
			int jumlahTerlambat = 0;
			for (PemesananPengadaanMasterAsset po : daftarPo) {
				@SuppressWarnings("unchecked")
				List<PemesananPengadaanMasterAssetDetail> baris = session
						.createCriteria(PemesananPengadaanMasterAssetDetail.class)
						.add(Restrictions.eq("pemesananPengadaanMasterAsset.id", po.getId()))
						.addOrder(Order.asc("id")).list();
				for (PemesananPengadaanMasterAssetDetail d : baris) {
					double dipesan = d.getJumlah() == null ? 0 : d.getJumlah().doubleValue();
					double diterima = jumlahSudahDiterima(session, d.getId(), null);
					double sisa = dipesan - diterima;
					if (sisa <= 0) {
						continue;
					}
					String namaBarang = d.getMasterAsset() == null ? "" : d.getMasterAsset().getNama();
					boolean terlambat = po.getPengirimanPalingLambat() != null
							&& po.getPengirimanPalingLambat().before(hariIni);
					if (hanyaTerlambat && !terlambat) {
						continue;
					}
					if (cari.length() > 0) {
						String kodePo = po.getKode() == null ? "" : po.getKode().toLowerCase();
						if (kodePo.indexOf(cari) < 0
								&& (namaBarang == null ? "" : namaBarang.toLowerCase()).indexOf(cari) < 0) {
							continue;
						}
					}
					double harga = d.getHargaBeli() == null ? 0 : d.getHargaBeli().doubleValue();
					cocok++;
					totalNilai += sisa * harga;
					if (terlambat) {
						jumlahTerlambat++;
					}
					if (cocok <= mulai || arr.length() >= pageSize) {
						continue;
					}
					Produk produkBaris = produkDariMasterAsset(session, d.getMasterAsset());
					JSONObject o = new JSONObject();
					o.put("po_id", po.getId());
					o.put("po", po.getKode() == null ? "" : po.getKode());
					o.put("po_detail_id", d.getId());
					o.put("penyedia", po.getPenyedia() == null ? "" : po.getPenyedia().getNama());
					o.put("tanggalPo", po.getTanggalPembuatan() == null ? ""
							: Common.dateFormat1.get().format(po.getTanggalPembuatan()));
					o.put("kirimPalingLambat", po.getPengirimanPalingLambat() == null ? ""
							: Common.dateFormat1.get().format(po.getPengirimanPalingLambat()));
					o.put("terlambat", terlambat);
					o.put("umurHari", umurHari(po.getTanggalPembuatan(), hariIni));
					o.put("produk_id", produkBaris == null ? JSONObject.NULL : produkBaris.getId());
					o.put("master_asset_id", d.getMasterAsset() == null ? JSONObject.NULL : d.getMasterAsset().getId());
					o.put("barang", namaBarang == null ? "" : namaBarang);
					o.put("produk", namaBarang == null ? "" : namaBarang);
					o.put("dipesan", dipesan);
					o.put("diterima", diterima);
					o.put("sisa", sisa);
					o.put("hargaBeli", harga);
					o.put("nilaiSisa", sisa * harga);
					arr.put(o);
				}
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("total", cocok);
			hasil.put("totalNilai", totalNilai);
			hasil.put("jumlahTerlambat", jumlahTerlambat);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Umur dokumen dalam hari; dipakai memantau pesanan yang lama tidak datang. */
	private static long umurHari(java.util.Date awal, java.util.Date akhir) {
		if (awal == null || akhir == null) {
			return 0;
		}
		long selisih = akhir.getTime() - awal.getTime();
		return selisih <= 0 ? 0 : selisih / (1000L * 60L * 60L * 24L);
	}

	/**
	 * Sinkronkan sebuah BAST yang sudah disetujui menjadi faktur Kulakan, sehingga barang
	 * yang diterima benar-benar menambah stok POS.
	 *
	 * <p>Penambahan stok, pembaruan harga beli, dan seluruh pagar kebijakan harga TIDAK
	 * ditulis ulang di sini melainkan didelegasikan ke {@code KantinHelper.kulakanFakturSimpan}
	 * -- satu-satunya jalur resmi Kulakan. Dengan begitu barang masuk dari Pengadaan
	 * diperlakukan persis sama dengan entri Kulakan biasa, termasuk perhitungan HPP-nya.</p>
	 *
	 * <p>Sinkronisasi menuntut setiap baris BAST berpadanan Produk POS. Padanan itu terisi
	 * sendiri ketika dokumen dibuat dari daftar Produk POS; baris warisan yang menunjuk
	 * barang inventaris tanpa padanan dilaporkan agar diperbaiki, bukan dilewati diam-diam.</p>
	 */
	public static void bastSinkronKulakan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehAksi(tbmuser, KUNCI_SINKRON, "create")) {
			tolak(hasil, "Grup pengguna Anda tidak memiliki hak menyinkronkan penerimaan ke Kulakan.");
			return;
		}
		Long id = (request == null || request.isNull("id")) ? null
				: Long.valueOf((request.get("id") + "").trim());
		if (id == null) {
			tolak(hasil, "Parameter id wajib diisi.");
			return;
		}
		Long tokoId = tokoLingkup(tbmuser, request);
		JSONObject payload = new JSONObject();
		JSONArray items = new JSONArray();
		String nomorFaktur;
		Long bastId;
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			PenerimaanPengadaanMasterAsset bast = (PenerimaanPengadaanMasterAsset) session
					.get(PenerimaanPengadaanMasterAsset.class, id);
			if (bast == null) {
				tolak(hasil, "Penerimaan Barang tidak ditemukan.");
				return;
			}
			if (tokoId != null && bast.getToko() != null && !tokoId.equals(bast.getToko().getId())) {
				tolak(hasil, "Penerimaan Barang ini milik toko lain.");
				return;
			}
			if (bast.getTanggalPersetujuan() == null) {
				tolak(hasil, "Hanya Penerimaan Barang yang sudah disetujui yang dapat "
						+ "disinkronkan ke stok Kulakan.");
				return;
			}
			if (bast.getPengadaanFaktur() != null) {
				tolak(hasil, "Penerimaan Barang ini sudah pernah disinkronkan ke Kulakan "
						+ "(faktur " + (bast.getPengadaanFaktur().getNomorFaktur() == null ? ""
								: bast.getPengadaanFaktur().getNomorFaktur())
						+ "). Sinkronisasi ulang akan menggandakan stok.");
				return;
			}
			bastId = bast.getId();
			nomorFaktur = bast.getKodeTagihan() == null || bast.getKodeTagihan().trim().isEmpty()
					? (bast.getKode() == null ? "" : bast.getKode())
					: bast.getKodeTagihan().trim();

			@SuppressWarnings("unchecked")
			List<PenerimaanPengadaanMasterAssetDetail> baris = session
					.createCriteria(PenerimaanPengadaanMasterAssetDetail.class)
					.add(Restrictions.eq("penerimaanPengadaanMasterAsset.id", bast.getId()))
					.addOrder(Order.asc("id")).list();
			if (baris.isEmpty()) {
				tolak(hasil, "Penerimaan Barang ini tidak memiliki baris barang.");
				return;
			}
			StringBuilder tanpaPadanan = new StringBuilder();
			StringBuilder tanpaHarga = new StringBuilder();
			for (PenerimaanPengadaanMasterAssetDetail d : baris) {
				double qty = d.getDiterima() == null ? 0 : d.getDiterima().doubleValue();
				if (qty <= 0) {
					continue;
				}
				Produk produk = produkDariMasterAsset(session, d.getMasterAsset());
				String nama = d.getMasterAsset() == null ? "barang" : d.getMasterAsset().getNama();
				if (produk == null) {
					tanpaPadanan.append(tanpaPadanan.length() == 0 ? "" : ", ").append(nama);
					continue;
				}
				double harga = d.getHargaBeli() == null ? 0 : d.getHargaBeli().doubleValue();
				if (harga <= 0) {
					tanpaHarga.append(tanpaHarga.length() == 0 ? "" : ", ").append(nama);
					continue;
				}
				JSONObject it = new JSONObject();
				it.put("produk_id", produk.getId());
				it.put("qty", qty);
				it.put("harga_beli_satuan", harga);
				items.put(it);
			}
			if (tanpaPadanan.length() > 0) {
				tolak(hasil, "Barang berikut belum berpadanan produk toko sehingga stoknya tidak "
						+ "dapat ditambahkan: " + tanpaPadanan
						+ ". Buat dokumen pengadaan dari daftar Produk POS, atau petakan barangnya lebih dulu.");
				return;
			}
			if (tanpaHarga.length() > 0) {
				tolak(hasil, "Harga beli belum diisi pada barang berikut: " + tanpaHarga
						+ ". Kulakan menuntut harga beli lebih besar dari nol.");
				return;
			}
			if (items.length() == 0) {
				tolak(hasil, "Tidak ada baris dengan jumlah diterima lebih dari nol.");
				return;
			}
			payload.put("nomor_faktur", nomorFaktur);
			payload.put("items", items);
			payload.put("keterangan", "Sinkron dari penerimaan " + (bast.getKode() == null ? "" : bast.getKode()));
			if (bast.getToko() != null) {
				payload.put("toko_id", bast.getToko().getId());
			}
			if (bast.getTanggalTagihan() != null) {
				payload.put("tanggal_faktur", Common.dateFormatInput.get().format(bast.getTanggalTagihan()));
			}
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}

		// Delegasi ke jalur resmi Kulakan -- di luar session di atas supaya helper itu
		// mengelola transaksinya sendiri seperti saat dipanggil layar Kulakan.
		JSONObject hasilKulakan = new JSONObject();
		ais.action.servlet.api.KantinHelper.kulakanFakturSimpan(tbmuser, payload, hasilKulakan);
		if (!"00".equals(hasilKulakan.optString("status"))
				&& !"success".equals(hasilKulakan.optString("status"))) {
			tolak(hasil, "Kulakan menolak sinkronisasi: "
					+ hasilKulakan.optString("description", "sebab tidak dijelaskan") + ".");
			return;
		}

		// Tandai BAST dengan faktur yang terbentuk supaya tidak tersinkron dua kali.
		Session tandai = HibernateUtil.getSessionFactory().openSession();
		try {
			PenerimaanPengadaanMasterAsset bast = (PenerimaanPengadaanMasterAsset) tandai
					.get(PenerimaanPengadaanMasterAsset.class, bastId);
			ais.database.model.inventory.PengadaanFaktur faktur = null;
			// Kulakan mengembalikan kunci "fakturId"; dibaca apa adanya agar penandaan
			// tidak bergantung pada pencarian nomor faktur yang bisa saja kembar.
			Long fakturId = hasilKulakan.isNull("fakturId") ? null
					: Long.valueOf((hasilKulakan.get("fakturId") + "").trim());
			if (fakturId != null) {
				faktur = (ais.database.model.inventory.PengadaanFaktur) tandai
						.get(ais.database.model.inventory.PengadaanFaktur.class, fakturId);
			}
			if (faktur == null) {
				faktur = (ais.database.model.inventory.PengadaanFaktur) tandai
						.createCriteria(ais.database.model.inventory.PengadaanFaktur.class)
						.add(Restrictions.eq("nomorFaktur", nomorFaktur))
						.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
			}
			if (bast != null && faktur != null) {
				tandai.beginTransaction();
				bast.setPengadaanFaktur(faktur);
				tandai.saveOrUpdate(bast);
				tandai.getTransaction().commit();
			}
			hasil.put("status", "00");
			hasil.put("id", bastId);
			hasil.put("nomorFaktur", nomorFaktur);
			hasil.put("jumlahBaris", items.length());
			hasil.put("faktur_id", faktur == null ? JSONObject.NULL : faktur.getId());
		} catch (Exception e) {
			try {
				if (tandai.getTransaction() != null && tandai.getTransaction().isActive()) {
					tandai.getTransaction().rollback();
				}
			} catch (Exception eRollback) {
				ais.common.ErrorAuditUtil.record(eRollback, "PengadaanPosApiHelper.bastSinkronKulakan tandai");
			}
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(tandai);
		}
	}

	/** Dipakai dispatcher: aksi berawalan {@code pengadaan_} diarahkan ke sini. */
	public static boolean proses(String action, Tbmuser tbmuser, JSONObject request, JSONObject hasil)
			throws Exception {
		if ("pengadaan_pr_daftar".equals(action) || "pengadaan_pr_list".equals(action)) {
			prDaftar(tbmuser, request, hasil);
			return true;
		}
		if ("pengadaan_pr_detail".equals(action)) {
			prDetail(tbmuser, request, hasil);
			return true;
		}
		if ("pengadaan_pr_simpan".equals(action)) {
			prSimpan(tbmuser, request, hasil);
			return true;
		}
		if ("pengadaan_pr_putusan".equals(action)) {
			prPutusan(tbmuser, request, hasil);
			return true;
		}
		if ("pengadaan_barang_cari".equals(action)) {
			cariBarang(tbmuser, request, hasil);
			return true;
		}
		if ("pengadaan_pr_hapus".equals(action)) {
			prHapus(tbmuser, request, hasil);
			return true;
		}
		if ("pengadaan_po_daftar".equals(action) || "pengadaan_po_list".equals(action)) {
			poDaftar(tbmuser, request, hasil);
			return true;
		}
		if ("pengadaan_po_detail".equals(action)) {
			poDetail(tbmuser, request, hasil);
			return true;
		}
		if ("pengadaan_po_simpan".equals(action)) {
			poSimpan(tbmuser, request, hasil);
			return true;
		}
		if ("pengadaan_po_putusan".equals(action)) {
			poPutusan(tbmuser, request, hasil);
			return true;
		}
		if ("pengadaan_po_hapus".equals(action)) {
			poHapus(tbmuser, request, hasil);
			return true;
		}
		if ("pengadaan_po_dari_pr".equals(action)) {
			poDariPr(tbmuser, request, hasil);
			return true;
		}
		if ("pengadaan_penyedia_cari".equals(action)) {
			cariPenyedia(tbmuser, request, hasil);
			return true;
		}
		if ("pengadaan_bast_daftar".equals(action) || "pengadaan_bast_list".equals(action)) {
			bastDaftar(tbmuser, request, hasil);
			return true;
		}
		if ("pengadaan_bast_detail".equals(action)) {
			bastDetail(tbmuser, request, hasil);
			return true;
		}
		if ("pengadaan_bast_simpan".equals(action)) {
			bastSimpan(tbmuser, request, hasil);
			return true;
		}
		if ("pengadaan_bast_putusan".equals(action)) {
			bastPutusan(tbmuser, request, hasil);
			return true;
		}
		if ("pengadaan_bast_hapus".equals(action)) {
			bastHapus(tbmuser, request, hasil);
			return true;
		}
		if ("pengadaan_bast_dari_po".equals(action)) {
			bastDariPo(tbmuser, request, hasil);
			return true;
		}
		if ("pengadaan_tagihan_daftar".equals(action) || "pengadaan_tagihan_list".equals(action)) {
			tagihanDaftar(tbmuser, request, hasil);
			return true;
		}
		if ("pengadaan_tagihan_terima".equals(action)) {
			tagihanTerima(tbmuser, request, hasil);
			return true;
		}
		if ("pengadaan_tagihan_batal".equals(action)) {
			tagihanBatal(tbmuser, request, hasil);
			return true;
		}
		if ("pengadaan_barang_resolve".equals(action)) {
			barangResolve(tbmuser, request, hasil);
			return true;
		}
		if ("pengadaan_bayar_daftar".equals(action) || "pengadaan_bayar_list".equals(action)) {
			bayarDaftar(tbmuser, request, hasil);
			return true;
		}
		if ("pengadaan_bayar_detail".equals(action)) {
			bayarDetail(tbmuser, request, hasil);
			return true;
		}
		if ("pengadaan_bayar_simpan".equals(action)) {
			bayarSimpan(tbmuser, request, hasil);
			return true;
		}
		if ("pengadaan_bayar_putusan".equals(action)) {
			bayarPutusan(tbmuser, request, hasil);
			return true;
		}
		if ("pengadaan_bayar_hapus".equals(action)) {
			bayarHapus(tbmuser, request, hasil);
			return true;
		}
		if ("pengadaan_bayar_tagihan_terbuka".equals(action)) {
			bayarTagihanTerbuka(tbmuser, request, hasil);
			return true;
		}
		if ("pengadaan_bdp_daftar".equals(action) || "pengadaan_bdp_list".equals(action)) {
			bdpDaftar(tbmuser, request, hasil);
			return true;
		}
		if ("pengadaan_bast_sinkron_kulakan".equals(action)) {
			bastSinkronKulakan(tbmuser, request, hasil);
			return true;
		}
		return false;
	}
}
