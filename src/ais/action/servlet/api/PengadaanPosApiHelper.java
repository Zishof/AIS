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
import ais.database.model.asset.PembayaranTerminMasterAssetDetail;
import ais.database.model.asset.PemesananPengadaanMasterAsset;
import ais.database.model.asset.PemesananPengadaanMasterAssetDetail;
import ais.database.model.asset.PenyediaAsset;
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

	/** Kunci menu tunggal utk seluruh tahap pengadaan POS (PR dulu, tahap lain menyusul). */
	private static final String KUNCI_MENU = "pengadaan_pr";

	private PengadaanPosApiHelper() {
	}

	private static void tolak(JSONObject hasil, String pesan) throws Exception {
		hasil.put("status", "91");
		hasil.put("description", pesan);
	}

	private static boolean bolehLihat(Tbmuser tbmuser) {
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
		return menu != null && menu.optBoolean(KUNCI_MENU, false);
	}

	private static boolean bolehAksi(Tbmuser tbmuser, String aksi) {
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
				EbisnisMenuKatalog.urai(role.getEbisnisMenu()), KUNCI_MENU, aksi);
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
	 * Daftar PR pada lingkup toko pemanggil. Param opsional: {@code cari} (kode/keterangan),
	 * {@code status} (DRAFT/DISETUJUI/DITOLAK/TUTUP), {@code page}, {@code pageSize}.
	 */
	public static void prDaftar(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehLihat(tbmuser)) {
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
		if (!bolehLihat(tbmuser)) {
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
				o.put("master_asset_id", d.getMasterAsset() == null ? JSONObject.NULL : d.getMasterAsset().getId());
				o.put("barang", d.getMasterAsset() == null ? "" : d.getMasterAsset().getNama());
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
		if (!bolehAksi(tbmuser, id == null ? "create" : "update")) {
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
				if (b.isNull("master_asset_id")) {
					continue;
				}
				MasterAsset barang = (MasterAsset) session.get(MasterAsset.class,
						Long.valueOf((b.get("master_asset_id") + "").trim()));
				if (barang == null) {
					continue;
				}
				double jumlah = b.optDouble("jumlah", 0);
				double harga = b.optDouble("hargaBeli", 0);
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
		if (!bolehAksi(tbmuser, "approve")) {
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
		if (!bolehAksi(tbmuser, "delete")) {
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
		if (!bolehLihat(tbmuser)) {
			tolak(hasil, "Menu Pengadaan tidak diaktifkan untuk grup pengguna Anda.");
			return;
		}
		String q = request == null ? "" : request.optString("keyword", "").trim();
		int limit = Math.min(100, Math.max(5, request == null ? 50 : request.optInt("limit", 50)));
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Criteria kriteria = session.createCriteria(MasterAsset.class);
			if (q.length() > 0) {
				kriteria.add(Restrictions.or(
						Restrictions.ilike("kode", q, MatchMode.ANYWHERE),
						Restrictions.ilike("nama", q, MatchMode.ANYWHERE)));
			}
			kriteria.addOrder(Order.asc("nama"));
			kriteria.setMaxResults(limit);
			@SuppressWarnings("unchecked")
			List<MasterAsset> daftar = kriteria.list();
			JSONArray arr = new JSONArray();
			for (MasterAsset m : daftar) {
				JSONObject o = new JSONObject();
				o.put("id", m.getId());
				o.put("kode", m.getKode() == null ? "" : m.getKode());
				o.put("nama", m.getNama() == null ? "" : m.getNama());
				o.put("merk", m.getMerk() == null ? "" : m.getMerk());
				o.put("satuan", m.getUnit() == null ? "" : m.getUnit());
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
				// Divalidasi dulu supaya isi kolom formula tidak pernah berisi tanggal ngawur.
				item.put("tanggalD", Common.dateFormat1.get().format(Common.dateFormat1.get().parse(tgl)));
			}
			hasil.put(item);
		}
		return hasil;
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
		if (!bolehLihat(tbmuser)) {
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
		if (!bolehLihat(tbmuser)) {
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
				o.put("master_asset_id", d.getMasterAsset() == null ? JSONObject.NULL : d.getMasterAsset().getId());
				o.put("barang", d.getMasterAsset() == null ? "" : d.getMasterAsset().getNama());
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
				if (b.getPembayaranTerminMasterAsset() != null
						&& Boolean.FALSE.equals(b.getPembayaranTerminMasterAsset().getAktif())) {
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
		if (!bolehAksi(tbmuser, id == null ? "create" : "update")) {
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
		Long penyediaId = (request.isNull("penyedia_id") || (request.get("penyedia_id") + "").trim().isEmpty())
				? null : Long.valueOf((request.get("penyedia_id") + "").trim());
		if (penyediaId == null) {
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
			PenyediaAsset penyedia = (PenyediaAsset) session.get(PenyediaAsset.class, penyediaId);
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
				try {
					po.setPengirimanPalingLambat(Common.dateFormat1.get().parse(kirim));
				} catch (Exception e) {
					tolak(hasil, "Tanggal pengiriman paling lambat harus berformat hh-bb-tttt.");
					return;
				}
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
				if (b == null || b.isNull("master_asset_id")) {
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
				if (b.isNull("master_asset_id")) {
					continue;
				}
				MasterAsset barang = (MasterAsset) session.get(MasterAsset.class,
						Long.valueOf((b.get("master_asset_id") + "").trim()));
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
		if (!bolehAksi(tbmuser, "approve")) {
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
		if (!bolehAksi(tbmuser, "delete")) {
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
		if (!bolehLihat(tbmuser)) {
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
				o.put("master_asset_id", d.getMasterAsset() == null ? JSONObject.NULL : d.getMasterAsset().getId());
				o.put("barang", d.getMasterAsset() == null ? "" : d.getMasterAsset().getNama());
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
		if (!bolehLihat(tbmuser)) {
			tolak(hasil, "Menu Pengadaan tidak diaktifkan untuk grup pengguna Anda.");
			return;
		}
		String q = request == null ? "" : request.optString("keyword", "").trim();
		int limit = Math.min(100, Math.max(5, request == null ? 50 : request.optInt("limit", 50)));
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Criteria kriteria = session.createCriteria(PenyediaAsset.class);
			if (q.length() > 0) {
				kriteria.add(Restrictions.or(
						Restrictions.ilike("kode", q, MatchMode.ANYWHERE),
						Restrictions.ilike("nama", q, MatchMode.ANYWHERE)));
			}
			kriteria.addOrder(Order.asc("nama"));
			kriteria.setMaxResults(limit);
			@SuppressWarnings("unchecked")
			List<PenyediaAsset> daftar = kriteria.list();
			JSONArray arr = new JSONArray();
			for (PenyediaAsset p : daftar) {
				JSONObject o = new JSONObject();
				o.put("id", p.getId());
				o.put("kode", p.getKode() == null ? "" : p.getKode());
				o.put("nama", p.getNama() == null ? "" : p.getNama());
				o.put("alamat", p.getAlamat() == null ? "" : p.getAlamat());
				arr.put(o);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
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
		return false;
	}
}
