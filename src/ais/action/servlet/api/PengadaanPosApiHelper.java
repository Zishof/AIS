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
		java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyyMM");
		String periode = fmt.format(ais.ui.util.WaktuUtil.getDate());
		String prefiks = "PR/" + (tokoId == null ? "0" : tokoId) + "/" + periode + "/";
		for (int percobaan = 0; percobaan < 50; percobaan++) {
			Number jml = (Number) session.createCriteria(PermintaanPengadaanMasterAsset.class)
					.setProjection(Projections.rowCount())
					.add(Restrictions.ilike("kode", prefiks, MatchMode.START)).uniqueResult();
			long urut = (jml == null ? 0 : jml.longValue()) + 1 + percobaan;
			String kandidat = prefiks + (urut < 10 ? "000" : urut < 100 ? "00" : urut < 1000 ? "0" : "") + urut;
			Number bentrok = (Number) session.createCriteria(PermintaanPengadaanMasterAsset.class)
					.setProjection(Projections.rowCount())
					.add(Restrictions.eq("kode", kandidat)).uniqueResult();
			if (bentrok == null || bentrok.intValue() == 0) {
				return kandidat;
			}
		}
		return prefiks + System.currentTimeMillis();
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
		return false;
	}
}
