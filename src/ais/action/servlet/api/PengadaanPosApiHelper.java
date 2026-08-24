package ais.action.servlet.api;

import java.util.Calendar;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.EbisnisMenuKatalog;
import ais.action.master.asset.SaldoAwalMasterAssetAction;
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
import ais.database.model.asset.SaldoAwalMasterAsset;
import ais.database.model.asset.SaldoAwalMasterAssetDetail;
import ais.database.model.akunting.DaftarPengajuanTransfer;
import ais.database.model.inventory.Produk;
import ais.database.model.asset.PermintaanPengadaanMasterAsset;
import ais.database.model.asset.PermintaanPengadaanMasterAssetDetail;
import ais.database.model.inventory.Toko;
import ais.database.model.rab.Workspace;

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
	private static final String KONFIG_TAGIHAN_RUTIN_ANGGARAN_WAJIB =
			"pengadaan_tagihan_rutin_anggaran_wajib";
	private static final String KUNCI_DPC = "pengadaan_dpc";
	private static final String KUNCI_BDP = "pengadaan_bdp";
	private static final String KUNCI_SINKRON = "pengadaan_sinkron";
	private static final String KUNCI_PAJAK = "pengadaan_pajak";

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
				o.put("tanpaAnggaran", Boolean.TRUE.equals(pr.getTanpaAnggaran()));
				o.put("anggaran", pr.getWorkspace() == null ? ""
						: ((pr.getWorkspace().getKode() == null ? "" : pr.getWorkspace().getKode() + " ")
								+ (pr.getWorkspace().getNama() == null ? "" : pr.getWorkspace().getNama())).trim());
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
	 * Mencatat tagihan rutin yang memang tidak mempunyai BAST, misalnya listrik,
	 * air, internet, sewa, dan jasa berlangganan. Dokumen yang dipakai sama dengan
	 * versi ZKoss ({@link SaldoAwalMasterAsset}): setiap rincian boleh menunjuk satu
	 * {@link Workspace} sehingga realisasi tetap masuk ke anggaran yang tepat.
	 *
	 * <p>Anggaran sengaja opsional secara bawaan. Instalasi yang mewajibkannya dapat
	 * mengaktifkan konfigurasi {@code pengadaan_tagihan_rutin_anggaran_wajib}. Pagar
	 * ini dijalankan di server, bukan hanya di antarmuka.</p>
	 */
	public static void tagihanRutinSimpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil)
			throws Exception {
		if (!bolehAksi(tbmuser, KUNCI_TAGIHAN, "create")) {
			tolak(hasil, "Grup pengguna Anda tidak memiliki hak menerima tagihan rutin vendor.");
			return;
		}
		if (tbmuser == null) {
			tolak(hasil, "Sesi pengguna tidak dikenali, silakan masuk ulang.");
			return;
		}
		String penyediaTeks = request == null ? "" : request.optString("penyediaId", "").trim();
		String kodeTagihan = request == null ? "" : request.optString("kodeTagihan", "").trim();
		String tanggalTeks = request == null ? "" : request.optString("tanggalTagihan", "").trim();
		String keterangan = request == null ? "" : request.optString("keterangan", "").trim();
		JSONArray rincian = request == null ? null : request.optJSONArray("rincian");
		if (penyediaTeks.length() == 0) {
			tolak(hasil, "Penyedia tagihan rutin wajib dipilih.");
			return;
		}
		if (kodeTagihan.length() == 0) {
			tolak(hasil, "Nomor tagihan/faktur vendor wajib diisi.");
			return;
		}
		java.util.Date tanggalTagihan = tanggalKetat(tanggalTeks);
		if (tanggalTagihan == null) {
			tolak(hasil, "Tanggal tagihan harus berformat hh-bb-tttt, mis. 10-08-2026.");
			return;
		}
		if (rincian == null || rincian.length() == 0) {
			tolak(hasil, "Minimal satu rincian tagihan wajib diisi.");
			return;
		}

		boolean anggaranWajib = Common.bolehKonfigurasi(
				KONFIG_TAGIHAN_RUTIN_ANGGARAN_WAJIB);
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction transaksi = null;
		SaldoAwalMasterAsset tagihan = null;
		try {
			Long penyediaId = Long.valueOf(penyediaTeks);
			PenyediaAsset penyedia = (PenyediaAsset) session.get(PenyediaAsset.class, penyediaId);
			if (penyedia == null) {
				tolak(hasil, "Penyedia tagihan rutin tidak ditemukan.");
				return;
			}
			Long tokoId = tokoLingkup(tbmuser, request);
			Toko toko = tokoId == null ? null : (Toko) session.get(Toko.class, tokoId);
			if (tokoId != null && toko == null) {
				tolak(hasil, "Toko tagihan rutin tidak ditemukan.");
				return;
			}

			Criteria kembar = session.createCriteria(SaldoAwalMasterAsset.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
					.add(Restrictions.eq("penyedia.id", penyediaId))
					.add(Restrictions.eq("kodeTagihan", kodeTagihan));
			if (tokoId != null) {
				kembar.add(Restrictions.eq("toko.id", tokoId));
			}
			if (!kembar.list().isEmpty()) {
				tolak(hasil, "Nomor tagihan " + kodeTagihan
						+ " sudah tercatat untuk penyedia dan toko yang sama."
						+ " Gunakan nomor faktur yang benar agar tidak terjadi tagihan ganda.");
				return;
			}

			double total = 0;
			for (int i = 0; i < rincian.length(); i++) {
				JSONObject baris = rincian.getJSONObject(i);
				String uraian = baris.optString("keterangan", "").trim();
				double nominal = angkaAman(baris, "nominal");
				String workspaceTeks = baris.optString("workspaceId", "").trim();
				if (uraian.length() == 0) {
					tolak(hasil, "Uraian rincian ke-" + (i + 1) + " wajib diisi.");
					return;
				}
				if (nominal <= 0) {
					tolak(hasil, "Nominal rincian ke-" + (i + 1) + " harus lebih besar dari nol.");
					return;
				}
				if (anggaranWajib && workspaceTeks.length() == 0) {
					tolak(hasil, "Anggaran rincian ke-" + (i + 1)
							+ " wajib dipilih sesuai konfigurasi sistem.");
					return;
				}
				if (workspaceTeks.length() > 0) {
					Workspace workspace = (Workspace) session.get(Workspace.class,
							Long.valueOf(workspaceTeks));
					if (workspace == null || Boolean.FALSE.equals(workspace.getAktif())) {
						tolak(hasil, "Anggaran rincian ke-" + (i + 1)
								+ " tidak ditemukan atau sudah tidak aktif.");
						return;
					}
				}
				total += nominal;
			}

			transaksi = session.beginTransaction();
			Calendar kalender = Calendar.getInstance();
			kalender.setTime(tanggalTagihan);
			tagihan = new SaldoAwalMasterAsset();
			tagihan.setKodeUnik(Common.getGeneratedBarCode());
			tagihan.setKode(SaldoAwalMasterAssetAction.generateCode(tanggalTagihan, true));
			tagihan.setKeterangan(keterangan.length() == 0 ? kodeTagihan : keterangan);
			tagihan.setToko(toko);
			tagihan.setPenyedia(penyedia);
			tagihan.setDibuatOleh(tbmuser);
			tagihan.setDisetujuiOleh(tbmuser);
			tagihan.setTanggalPembuatan(tanggalTagihan);
			tagihan.setTanggalPersetujuan(tanggalTagihan);
			tagihan.setNilai(Double.valueOf(total));
			tagihan.setTahun(Integer.valueOf(kalender.get(Calendar.YEAR)));
			tagihan.setBulan(Integer.valueOf(kalender.get(Calendar.MONTH) + 1));
			tagihan.setDibayar(Double.valueOf(0));
			tagihan.setLunas(Boolean.FALSE);
			tagihan.setAktif(Boolean.TRUE);
			tagihan.setKodeTagihan(kodeTagihan);
			tagihan.setTanggalTagihan(tanggalTagihan);
			session.save(tagihan);

			for (int i = 0; i < rincian.length(); i++) {
				JSONObject baris = rincian.getJSONObject(i);
				double nominal = angkaAman(baris, "nominal");
				String workspaceTeks = baris.optString("workspaceId", "").trim();
				SaldoAwalMasterAssetDetail detail = new SaldoAwalMasterAssetDetail();
				detail.setKodeUnik(Common.getGeneratedBarCode());
				detail.setSaldoAwal(tagihan);
				detail.setKeterangan(baris.optString("keterangan", "").trim());
				detail.setJumlah(Double.valueOf(1));
				detail.setHarga(Double.valueOf(nominal));
				detail.setHargaTotal(Double.valueOf(nominal));
				if (workspaceTeks.length() > 0) {
					detail.setWorkspace((Workspace) session.get(Workspace.class,
							Long.valueOf(workspaceTeks)));
				}
				session.save(detail);
			}
			transaksi.commit();

			// Setelah transaksi utama aman, bentuk DPC melalui helper resmi versi ZKoss.
			// Helper tersebut idempoten dan mengelola currentNativeSession-nya sendiri.
			DaftarPengajuanTransfer.simpanSaldoAwalMasterAsset(tagihan);
			hasil.put("status", "00");
			hasil.put("id", tagihan.getId());
			hasil.put("kode", tagihan.getKode());
			hasil.put("kodeTagihan", kodeTagihan);
			hasil.put("nilai", total);
			hasil.put("anggaranWajib", anggaranWajib);
		} catch (NumberFormatException e) {
			if (transaksi != null && transaksi.isActive()) {
				transaksi.rollback();
			}
			tolak(hasil, "Penyedia atau anggaran yang dipilih tidak valid.");
		} catch (Exception e) {
			try {
				if (transaksi != null && transaksi.isActive()) {
					transaksi.rollback();
				}
			} catch (Exception eRollback) {
				ais.common.ErrorAuditUtil.record(eRollback,
						"PengadaanPosApiHelper.tagihanRutinSimpan rollback");
			}
			throw e;
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
			h.put("tanpaAnggaran", Boolean.TRUE.equals(pr.getTanpaAnggaran()));
			h.put("workspace_id", pr.getWorkspace() == null ? JSONObject.NULL : pr.getWorkspace().getId());
			h.put("anggaran", pr.getWorkspace() == null ? ""
					: ((pr.getWorkspace().getKode() == null ? "" : pr.getWorkspace().getKode() + " ")
							+ (pr.getWorkspace().getNama() == null ? "" : pr.getWorkspace().getNama())).trim());
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
			// --- Anggaran ---------------------------------------------------------
			// Permintaan Pembelian memotong anggaran, jadi anggarannya HARUS jelas sejak
			// awal. Satu-satunya jalan keluar adalah menyatakan permintaan ini memang
			// tanpa anggaran -- pilihan yang eksplisit, bukan akibat kolom yang lupa diisi.
			boolean tanpaAnggaran = request.optBoolean("tanpaAnggaran", false);
			pr.setTanpaAnggaran(Boolean.valueOf(tanpaAnggaran));
			if (tanpaAnggaran) {
				pr.setWorkspace(null);
			} else if (!request.isNull("workspace_id")
					&& !(request.get("workspace_id") + "").trim().isEmpty()) {
				ais.database.model.rab.Workspace anggaran = (ais.database.model.rab.Workspace) session
						.get(ais.database.model.rab.Workspace.class,
								Long.valueOf((request.get("workspace_id") + "").trim()));
				if (anggaran == null) {
					tolak(hasil, "Anggaran yang dipilih tidak ditemukan.");
					return;
				}
				pr.setWorkspace(anggaran);
			} else if (pr.getWorkspace() == null) {
				tolak(hasil, "Anggaran wajib dipilih. Bila permintaan ini memang tidak "
						+ "membebani anggaran, centang \"Tanpa anggaran\".");
				return;
			}
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

			/* KE-FIX (ConstraintViolationException "could not delete:
			 * [PermintaanPengadaanMasterAssetDetail#..]" -> foreign key
			 * fkbdd96d7c2b2dbf8f pada pemesanan_pengadaan_master_asset_detail).
			 *
			 * Penyimpanan PR memakai pola HAPUS-SEMUA lalu BUAT-ULANG baris detail.
			 * Begitu PR-nya sudah dipesan, baris Pemesanan (PO) menunjuk ke baris
			 * detail PR tersebut, sehingga penghapusan ditolak database. Pengguna
			 * hanya melihat pesan generik "Data belum berubah, muat ulang halaman"
			 * yang sama sekali tidak menjelaskan sebabnya, dan menyunting ulang
			 * berapa kali pun tidak akan pernah berhasil.
			 *
			 * Memaksa hapus juga BUKAN pilihan: itu akan memutus jejak PO ke PR
			 * asalnya. Jadi penyuntingan ditolak lebih awal dengan pesan yang
			 * menyebutkan nomor PO pemakainya, supaya pengguna tahu harus membatalkan
			 * atau merevisi PO itu dulu. PR yang belum dipesan sama sekali tidak
			 * terpengaruh dan tetap dapat disunting seperti biasa. */
			if (!lama.isEmpty()) {
				java.util.List<Long> idDetailLama = new java.util.ArrayList<Long>();
				for (PermintaanPengadaanMasterAssetDetail d : lama) {
					if (d.getId() != null) {
						idDetailLama.add(d.getId());
					}
				}
				if (!idDetailLama.isEmpty()) {
					@SuppressWarnings("unchecked")
					List<ais.database.model.asset.PemesananPengadaanMasterAssetDetail> dipakai = session
							.createCriteria(ais.database.model.asset.PemesananPengadaanMasterAssetDetail.class)
							.add(Restrictions.in("permintaanPengadaanMasterAssetDetail.id", idDetailLama))
							.list();
					if (!dipakai.isEmpty()) {
						java.util.LinkedHashSet<String> kodePo = new java.util.LinkedHashSet<String>();
						for (int i = 0; i < dipakai.size(); i++) {
							ais.database.model.asset.PemesananPengadaanMasterAssetDetail dd = dipakai.get(i);
							if (dd.getPemesananPengadaanMasterAsset() != null) {
								String kode = dd.getPemesananPengadaanMasterAsset().getKode();
								if (kode != null && kode.trim().length() > 0) {
									kodePo.add(kode.trim());
								}
							}
						}
						StringBuilder daftar = new StringBuilder();
						for (java.util.Iterator<String> it = kodePo.iterator(); it.hasNext();) {
							if (daftar.length() > 0) {
								daftar.append(", ");
							}
							daftar.append(it.next());
						}
						try {
							if (session.getTransaction() != null && session.getTransaction().isActive()) {
								session.getTransaction().rollback();
							}
						} catch (Exception eRb) {
							ais.common.ErrorAuditUtil.record(eRb,
									"auto-audit(empty-catch) PengadaanPosApiHelper.prSimpan:rollback-terpakai");
						}
						hasil.put("status", "91");
						hasil.put("description",
								"Permintaan ini sudah dipesan"
										+ (daftar.length() > 0 ? " pada PO " + daftar : "")
										+ ", jadi rincian barangnya tidak dapat diubah lagi."
										+ " Batalkan atau revisi pemesanan tersebut terlebih dahulu.");
						return;
					}
				}
			}

			for (PermintaanPengadaanMasterAssetDetail d : lama) {
				session.delete(d);
			}
			session.flush();

			double total = 0;
			java.util.List<Long> barisBaru = new java.util.ArrayList<Long>();
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
				session.flush();
				barisBaru.add(d.getId());
				total += sub;
			}
			pr.setNilai(Double.valueOf(total));
			session.saveOrUpdate(pr);
			session.flush();
			// Catat pemakaian anggaran memakai PABRIK milik versi ZKoss sendiri, bukan
			// salinan logikanya. PenggunaanAnggaran.prosesSimpan bersifat idempoten (dicari
			// lewat ref) dan mengembalikan null bila PR tidak berAnggaran, sehingga aman
			// dipanggil pada setiap penyimpanan -- termasuk saat PR diubah menjadi tanpa
			// anggaran. Dengan begitu tabel penggunaan_anggaran terisi persis seperti bila
			// PR-nya dibuat dari layar ZKoss.
			for (Object idBaris : barisBaru) {
				try {
					PermintaanPengadaanMasterAssetDetail acuan = (PermintaanPengadaanMasterAssetDetail) session
							.get(PermintaanPengadaanMasterAssetDetail.class, (Long) idBaris);
					if (acuan != null) {
						ais.database.model.rab.PenggunaanAnggaran.prosesSimpan(acuan, session);
					}
				} catch (Exception eAnggaran) {
					// Kegagalan pencatatan anggaran TIDAK boleh membatalkan PR yang sah.
					ais.common.ErrorAuditUtil.record(eAnggaran,
							"PengadaanPosApiHelper.prSimpan penggunaanAnggaran baris=" + idBaris);
				}
			}
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
		/* Pesanan yang sisanya ditutup lewat Back Order TIDAK lagi berlabel DITUTUP.
		 *
		 * <p>Label itu keliru menggambarkan keadaannya: dokumennya sah, sudah disetujui, dan
		 * barangnya sebagian sudah diterima -- yang berhenti hanyalah SISA kiriman, dan sisa itu
		 * pindah ke pesanan susulan. "DITUTUP" terbaca seperti pesanan yang dibatalkan, padahal
		 * tidak ada yang batal. Label itu juga tidak pernah ada pada penyaring status di layar
		 * mana pun, sehingga pesanan yang menyandangnya justru tidak dapat ditemukan lewat
		 * penyaring.</p>
		 *
		 * <p>Keadaan "ditutup" tetap terkirim ke layar lewat medan {@code tutup} dan
		 * {@code alasanTutup} yang terpisah, jadi tidak ada keterangan yang hilang.</p> */
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
	private static JSONArray gabungTermin(Session session, JSONArray payload, JSONArray lama)
			throws Exception {
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
			double dpp = angkaAman(src, "penagihan");
			item.put("penagihan", dpp);
			// PPh dan PPN per termin. Kunci "pajak" dan "ppn" adalah kunci yang SAMA dengan
			// yang dibaca layar ZKoss, sehingga satu dokumen tetap terbaca di kedua versi;
			// "pajakPpn" adalah tambahan POS yang menyimpan SUMBER tarif PPN, sementara
			// nominalnya tetap ditulis ke "ppn" agar ZKoss melihat angka yang sama.
			String idPph = src.isNull("pajak") ? "" : (src.get("pajak") + "").trim();
			if (!idPph.isEmpty()) {
				item.put("pajak", idPph);
			} else if (!src.isNull("pajak")) {
				item.remove("pajak");
			}
			String idPpn = src.isNull("pajakPpn") ? "" : (src.get("pajakPpn") + "").trim();
			if (!idPpn.isEmpty()) {
				item.put("pajakPpn", idPpn);
				item.put("ppn", Math.rint((persenPpn(session, idPpn) / 100.0) * dpp));
			} else if (!src.isNull("ppn")) {
				item.put("ppn", angkaAman(src, "ppn"));
			}
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
				o.put("tutup", Boolean.TRUE.equals(po.getTutup()));
				o.put("alasanTutup", po.getAlasanTutup() == null ? "" : po.getAlasanTutup());
				o.put("po_induk_id", po.getPoInduk() == null ? JSONObject.NULL : po.getPoInduk().getId());
				o.put("poInduk", po.getPoInduk() == null || po.getPoInduk().getKode() == null ? ""
						: po.getPoInduk().getKode());
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
			// Rincian pajak per termin. DPP = penagihan; PPN ditambahkan ke tagihan vendor,
			// sedangkan PPh DIPOTONG dari kas yang keluar dan disetorkan sendiri ke negara.
			String idPph = src.isNull("pajak") ? "" : (src.get("pajak") + "").trim();
			String idPpn = src.isNull("pajakPpn") ? "" : (src.get("pajakPpn") + "").trim();
			double tarifPph = persenPph(session, idPph);
			double nilaiPpn = angkaAman(src, "ppn");
			double nilaiPph = Math.rint((tarifPph / 100.0) * tagih);
			o.put("penagihan", tagih);
			o.put("dpp", tagih);
			o.put("pajak", idPph);
			o.put("pajakPpn", idPpn);
			o.put("namaPajak", namaJenisPajak(session, idPph, idPpn));
			o.put("persenPph", tarifPph);
			o.put("pph", nilaiPph);
			o.put("ppn", nilaiPpn);
			o.put("tagihanVendor", tagih + nilaiPpn);
			o.put("kasKeluar", tagih + nilaiPpn - nilaiPph);
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
				terminBaru = gabungTermin(session, terminPayload, terminDari(po));
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
			/* Pesanan yang sisanya sudah ditutup lewat Back Order tidak boleh dibatalkan
			 * persetujuannya.
			 *
			 * <p>Sejak statusPo berhenti melaporkan DITUTUP, pesanan seperti ini tampil sebagai
			 * DISETUJUI -- dan layar menyalakan tombol "Batalkan keputusan" untuk status itu.
			 * Tanpa penjagaan di sini, persetujuan dapat ditarik pada pesanan yang barangnya
			 * TELAH diterima dan yang sisanya sudah berpindah ke pesanan susulan, sehingga
			 * penerimaan yang sah menggantung pada pesanan yang tidak lagi disetujui.</p>
			 *
			 * <p>Jalan yang benar adalah merevisi atau membatalkan keputusan Back Order-nya
			 * lebih dulu; sesudah sisa pesanan terbuka kembali, persetujuan dapat ditarik
			 * seperti biasa. JANGAN hilangkan penjagaan ini.</p> */
			if ("BATAL".equals(keputusan) && Boolean.TRUE.equals(po.getTutup())) {
				tolak(hasil, "Persetujuan tidak dapat dibatalkan karena sisa Pemesanan Pembelian ini "
						+ "sudah ditutup lewat Back Order. Revisi atau batalkan dahulu keputusan "
						+ "Back Order-nya, lalu ulangi.");
				return;
			}
			session.beginTransaction();
			if ("SETUJUI".equals(keputusan)) {
				po.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());
				po.setDisetujuiOleh(tbmuser);
				po.setTanggalDitolak(null);
				po.setDitolakOleh(null);
				po.setAlasanDitolak(null);
				// Sisi lain dari aturan di cabang TOLAK: susulan yang hidup kembali
				// berarti sisa induknya memang tidak lagi menunggu kiriman.
				if (po.getPoInduk() != null) {
					PemesananPengadaanMasterAsset induk = po.getPoInduk();
					if (!Boolean.TRUE.equals(induk.getTutup())) {
						induk.setTutup(Boolean.TRUE);
						session.saveOrUpdate(induk);
					}
				}
			} else if ("TOLAK".equals(keputusan)) {
				po.setTanggalDitolak(ais.ui.util.WaktuUtil.getDate());
				po.setDitolakOleh(tbmuser);
				po.setAlasanDitolak(alasan);
				po.setTanggalPersetujuan(null);
				po.setDisetujuiOleh(null);
				/* KE-FIX: pesanan SUSULAN yang ditolak harus MEMBUKA KEMBALI pesanan
				 * induknya.
				 *
				 * Back Order menutup sisa pesanan lama (tutup=true) lalu menerbitkan
				 * pesanan susulan. Bila susulan itu kemudian DITOLAK, kekurangannya tidak
				 * lagi dipesan di mana pun -- tetapi pesanan induknya tetap berstatus
				 * DITUTUP selamanya. Akibatnya penerimaan barang atas pesanan itu tidak
				 * dapat disunting lagi ("Sisa Pemesanan Pembelian ini sudah ditutup lewat
				 * Back Order"), dan tidak ada satu pun jalan untuk memulihkannya dari
				 * layar mana pun -- pengguna terkunci.
				 *
				 * Aturannya dijadikan tegas: induk tertutup HANYA selama susulannya masih
				 * hidup. Susulan yang ditolak berarti keputusan menutup sisa itu batal,
				 * jadi induknya dibuka kembali. */
				if (po.getPoInduk() != null) {
					PemesananPengadaanMasterAsset induk = po.getPoInduk();
					if (Boolean.TRUE.equals(induk.getTutup())) {
						induk.setTutup(Boolean.FALSE);
						session.saveOrUpdate(induk);
						hasil.put("po_induk_dibuka", induk.getKode() == null ? "" : induk.getKode());
					}
				}
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
			if (Boolean.TRUE.equals(induk.getTutup())) {
				// Sisa pesanan ini sudah ditutup (back order / short close), jadi yang masih
				// membebani Permintaan Pembelian hanyalah yang benar-benar diterima. Tanpa
				// pengecualian ini, permintaan akan tampak dipesan melebihi yang diminta dan
				// pesanan susulan tidak akan pernah bisa dibuat.
				jml += jumlahSudahDiterima(session, d.getId(), null);
				continue;
			}
			jml += d.getJumlah() == null ? 0 : d.getJumlah().doubleValue();
		}
		return jml;
	}

	/**
	 * Jumlah sebuah baris PR yang sudah benar-benar DITERIMA (lewat BAST mana pun),
	 * ditelusuri melalui baris-baris PO yang menunjuk baris PR ini. Padanan kolom
	 * "Qty BAST" pada layar Ambil Barang PR versi ZKoss.
	 */
	private static double jumlahSudahDiterimaPrBaris(Session session, Long prDetailId) {
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
			jml += jumlahSudahDiterima(session, d.getId(), null);
		}
		return jml;
	}

	/**
	 * Daftar BARANG Permintaan Pembelian yang masih boleh dipesan, dikelompokkan per nomor PR --
	 * padanan layar "Ambil Barang PR" versi ZKoss
	 * ({@code AmbilDataPermintaanPengadaanMasterAssetBanyak}).
	 *
	 * <p>Berbeda dengan {@link #poDariPr} yang bekerja per DOKUMEN, aksi ini bekerja per BARIS.
	 * Dengan begitu satu Pemesanan Pembelian boleh menggabungkan barang dari beberapa PR
	 * sekaligus, persis seperti versi ZKoss yang mengumpulkan pilihan ke dalam
	 * {@code PemesananPengadaanMasterAsset.permintaanPengadaanMasterAssets} (daftar id baris PR
	 * dipisah koma).</p>
	 *
	 * <p>Syarat sebuah PR ikut tampil disamakan dengan versi ZKoss: aktif, sudah disetujui, dan
	 * belum ditutup. Baris yang sisanya nol tidak ditampilkan sama sekali -- di ZKoss barisnya
	 * tampil tanpa kotak centang; di sini disembunyikan supaya layar sempit (Android) tidak penuh
	 * oleh baris yang memang tidak bisa dipilih.</p>
	 *
	 * <p>Tidak menulis apa pun. Klien mengumpulkan {@code pr_detail_id} yang dicentang lalu
	 * menyimpannya lewat {@code pengadaan_po_simpan} yang memang sudah menerima
	 * {@code pr_detail_id} per baris.</p>
	 */
	public static void prBarangTersedia(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehLihat(tbmuser, KUNCI_PO)) {
			tolak(hasil, "Menu Pengadaan tidak diaktifkan untuk grup pengguna Anda.");
			return;
		}
		Long tokoId = tokoLingkup(tbmuser, request);
		String cari = request == null ? "" : request.optString("cari", "").trim();
		int maksPr = Math.min(100, Math.max(5, request == null ? 30 : request.optInt("limit", 30)));
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Criteria kriteria = session.createCriteria(PermintaanPengadaanMasterAsset.class);
			kriteria.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)));
			// Hanya PR yang SUDAH DISETUJUI dan BELUM DITUTUP -- dua syarat yang sama dipakai
			// versi ZKoss pada layar Ambil Barang PR.
			kriteria.add(Restrictions.isNotNull("tanggalPersetujuan"));
			kriteria.add(Restrictions.or(Restrictions.isNull("tutup"), Restrictions.eq("tutup", Boolean.FALSE)));
			if (tokoId != null) {
				kriteria.add(Restrictions.eq("toko.id", tokoId));
			}
			if (cari.length() > 0) {
				kriteria.add(Restrictions.or(
						Restrictions.ilike("kode", cari, MatchMode.ANYWHERE),
						Restrictions.ilike("keterangan", cari, MatchMode.ANYWHERE)));
			}
			kriteria.addOrder(Order.desc("id"));
			@SuppressWarnings("unchecked")
			List<PermintaanPengadaanMasterAsset> daftarPr = kriteria.setMaxResults(maksPr * 3).list();

			JSONArray grup = new JSONArray();
			int jumlahBarisTotal = 0;
			for (PermintaanPengadaanMasterAsset pr : daftarPr) {
				if (grup.length() >= maksPr) {
					break;
				}
				@SuppressWarnings("unchecked")
				List<PermintaanPengadaanMasterAssetDetail> baris = session
						.createCriteria(PermintaanPengadaanMasterAssetDetail.class)
						.add(Restrictions.eq("permintaanPengadaanMasterAsset.id", pr.getId()))
						.addOrder(Order.asc("id")).list();
				JSONArray arr = new JSONArray();
				double nilaiSisa = 0;
				for (PermintaanPengadaanMasterAssetDetail d : baris) {
					double diminta = d.getJumlah() == null ? 0 : d.getJumlah().doubleValue();
					double sudah = jumlahSudahDipesan(session, d.getId());
					double sisa = diminta - sudah;
					if (sisa <= TOLERANSI) {
						continue;
					}
					double harga = d.getHargaBeli() == null ? 0 : d.getHargaBeli().doubleValue();
					JSONObject o = new JSONObject();
					o.put("pr_detail_id", d.getId());
					Produk produkBaris = produkDariMasterAsset(session, d.getMasterAsset());
					o.put("produk_id", produkBaris == null ? JSONObject.NULL : produkBaris.getId());
					o.put("master_asset_id",
							d.getMasterAsset() == null ? JSONObject.NULL : d.getMasterAsset().getId());
					o.put("kodeBarang", d.getMasterAsset() == null || d.getMasterAsset().getKode() == null ? ""
							: d.getMasterAsset().getKode());
					o.put("barang", d.getMasterAsset() == null ? "" : d.getMasterAsset().getNama());
					// Alias "produk": sebagian layar menamai kolom ini demikian.
					o.put("produk", d.getMasterAsset() == null ? "" : d.getMasterAsset().getNama());
					o.put("jumlahDiminta", diminta);
					o.put("jumlahSudahDipesan", sudah);
					o.put("jumlahDatang", jumlahSudahDiterimaPrBaris(session, d.getId()));
					o.put("sisa", sisa);
					// "jumlah" = usulan isian PO (sisa penuh), boleh dikurangi pengguna.
					o.put("jumlah", sisa);
					o.put("hargaBeli", harga);
					o.put("hargaTotal", sisa * harga);
					o.put("keterangan", d.getKeterangan() == null ? "" : d.getKeterangan());
					arr.put(o);
					nilaiSisa += sisa * harga;
				}
				if (arr.length() == 0) {
					continue;
				}
				JSONObject h = new JSONObject();
				h.put("pr_id", pr.getId());
				h.put("kode", pr.getKode() == null ? "" : pr.getKode());
				h.put("keterangan", pr.getKeterangan() == null ? "" : pr.getKeterangan());
				h.put("tanggal", pr.getTanggalPembuatan() == null ? JSONObject.NULL
						: Common.dateFormat3.get().format(pr.getTanggalPembuatan()));
				h.put("tanggalPersetujuan", pr.getTanggalPersetujuan() == null ? JSONObject.NULL
						: Common.dateFormat3.get().format(pr.getTanggalPersetujuan()));
				h.put("disetujuiOleh", pr.getDisetujuiOleh() == null ? ""
						: (pr.getDisetujuiOleh().getUserNama() == null ? "" : pr.getDisetujuiOleh().getUserNama()));
				h.put("toko_id", pr.getToko() == null ? JSONObject.NULL : pr.getToko().getId());
				h.put("nilaiSisa", nilaiSisa);
				h.put("detail", arr);
				grup.put(h);
				jumlahBarisTotal += arr.length();
			}
			hasil.put("status", "00");
			hasil.put("data", grup);
			hasil.put("jumlahBaris", jumlahBarisTotal);
			if (grup.length() == 0) {
				hasil.put("catatan", cari.length() > 0
						? "Tidak ada barang PR yang cocok dengan pencarian dan masih boleh dipesan."
						: "Belum ada barang Permintaan Pembelian yang menunggu dipesan. "
								+ "Pastikan PR sudah disetujui dan belum ditutup.");
			}
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Ragam lampiran pada Terima Tagihan, meniru daftar dokumen yang diminta versi ZKoss.
	 * Kolom: kunci, nama tampil, wajib?, harus gambar?
	 *
	 * <p>Berkasnya disimpan pada tabel {@code LampiranLain} yang SAMA dengan versi ZKoss
	 * (basis data streaming), dengan {@code ref} = id BAST dan {@code jenis} =
	 * {@link #JENIS_LAMPIRAN_TAGIHAN} + kunci. Karena itu berkas yang diunggah dari POS
	 * langsung terbaca di ZKoss, dan sebaliknya -- tanpa tabel baru.</p>
	 */
	private static final String[][] SLOT_LAMPIRAN_TAGIHAN = {
			{ "INVOICE", "Invoice", "wajib", "gambar" },
			{ "FAKTUR", "Faktur Pajak", "opsional", "bebas" },
			{ "SURAT_JALAN", "Surat Jalan", "opsional", "bebas" },
			{ "KWITANSI", "Kwitansi", "opsional", "bebas" },
			{ "LAINNYA", "Dokumen Lain", "opsional", "bebas" },
	};

	/** Awalan {@code jenis} pada LampiranLain untuk dokumen tagihan pengadaan. */
	private static final String JENIS_LAMPIRAN_TAGIHAN = "Dokumen Tagihan Pengadaan - ";

	/**
	 * Templat bukti setor pajak. BARU (2026-08-21) -- versi ZKoss tidak memiliki dokumen
	 * per-baris untuk pajak, hanya ekspor daftar. Nama templatnya diletakkan di sini agar
	 * POS dan ZKoss merujuk berkas yang sama.
	 */
	public static final String TEMPLAT_BUKTI_SETOR_PAJAK = "asset/bukti_setor_pajak";

	/** Batas isi PDF cetak yang ikut dikirim sebagai base64 (8 MB). */
	private static final long MAKS_BYTE_CETAK = 8L * 1024 * 1024;

	/** Batas ukuran satu berkas lampiran (5 MB), cukup untuk foto invoice dari ponsel. */
	private static final int MAKS_BYTE_LAMPIRAN = 5 * 1024 * 1024;

	private static String[] slotLampiran(String kunci) {
		String rapi = kunci == null ? "" : kunci.trim().toUpperCase();
		for (String[] slot : SLOT_LAMPIRAN_TAGIHAN) {
			if (slot[0].equals(rapi)) {
				return slot;
			}
		}
		return null;
	}

	/**
	 * Tebak jenis berkas dari byte awalnya (magic number), bukan dari nama berkas.
	 * Nama berkas mudah dipalsukan; isi berkas tidak. Mengembalikan mis. "image/jpeg",
	 * "application/pdf", atau "" bila tidak dikenali.
	 */
	private static String jenisBerkas(byte[] isi) {
		if (isi == null || isi.length < 4) {
			return "";
		}
		int b0 = isi[0] & 0xFF, b1 = isi[1] & 0xFF, b2 = isi[2] & 0xFF, b3 = isi[3] & 0xFF;
		if (b0 == 0xFF && b1 == 0xD8 && b2 == 0xFF) {
			return "image/jpeg";
		}
		if (b0 == 0x89 && b1 == 0x50 && b2 == 0x4E && b3 == 0x47) {
			return "image/png";
		}
		if (b0 == 0x47 && b1 == 0x49 && b2 == 0x46 && b3 == 0x38) {
			return "image/gif";
		}
		if (b0 == 0x42 && b1 == 0x4D) {
			return "image/bmp";
		}
		if (isi.length >= 12 && b0 == 0x52 && b1 == 0x49 && b2 == 0x46 && b3 == 0x46
				&& (isi[8] & 0xFF) == 0x57 && (isi[9] & 0xFF) == 0x45 && (isi[10] & 0xFF) == 0x42
				&& (isi[11] & 0xFF) == 0x50) {
			return "image/webp";
		}
		if (b0 == 0x25 && b1 == 0x50 && b2 == 0x44 && b3 == 0x46) {
			return "application/pdf";
		}
		return "";
	}

	/** Sesi ke basis data streaming, tempat seluruh berkas lampiran/foto disimpan. */
	private static Session sesiBerkas() {
		return ais.database.hibernate.StreamingHibernateUtil.getInstance().openSession();
	}

	/** Benar bila penyimpanan berkas dapat dipakai. Dipanggil sebelum aksi lampiran
	 *  supaya kegagalannya menjadi pesan yang jelas, bukan galat mentah. */
	/**
	 * Hasil pemeriksaan disimpan setelah percobaan pertama. Membuka lalu menutup sesi
	 * pada SETIAP aksi lampiran bukan sekadar boros -- pemeriksaan itu menginisialisasi
	 * pabrik sesi kedua beserta benang cache-nya. Sekali per proses sudah cukup.
	 */
	private static volatile Boolean berkasSiapTersimpan = null;

	private static boolean berkasSiap() {
		Boolean tersimpan = berkasSiapTersimpan;
		if (tersimpan != null) {
			return tersimpan.booleanValue();
		}
		Session s = null;
		try {
			s = sesiBerkas();
			berkasSiapTersimpan = Boolean.valueOf(s != null);
			return s != null;
		} catch (Throwable e) {
			ais.common.ErrorAuditUtil.record(e instanceof Exception ? (Exception) e
					: new Exception(e), "PengadaanPosApiHelper.berkasSiap");
			berkasSiapTersimpan = Boolean.FALSE;
			return false;
		} finally {
			HibernateUtil.closeSessionQuietly(s);
		}
	}

	private static ais.database.model.file.LampiranLain lampiranTagihan(Session sesi, Long bastId, String kunci) {
		return (ais.database.model.file.LampiranLain) sesi
				.createCriteria(ais.database.model.file.LampiranLain.class)
				.add(Restrictions.eq("ref", bastId))
				.add(Restrictions.eq("jenis", JENIS_LAMPIRAN_TAGIHAN + kunci))
				.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
	}

	/**
	 * Daftar slot lampiran sebuah BAST beserta status terisinya. Selalu mengembalikan SELURUH
	 * slot -- termasuk yang masih kosong -- supaya layar dapat menampilkan tombol unggah untuk
	 * masing-masing tanpa perlu menghafal daftarnya sendiri.
	 */
	public static void lampiranDaftar(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!berkasSiap()) {
			tolak(hasil, "Penyimpanan berkas sedang tidak dapat dihubungi, sehingga lampiran belum dapat diproses.");
			return;
		}
		if (!bolehLihat(tbmuser, KUNCI_TAGIHAN) && !bolehLihat(tbmuser, KUNCI_BAST)) {
			tolak(hasil, "Menu Pengadaan tidak diaktifkan untuk grup pengguna Anda.");
			return;
		}
		if (request == null || request.isNull("bast_id")) {
			tolak(hasil, "Parameter bast_id wajib diisi.");
			return;
		}
		Long bastId = Long.valueOf((request.get("bast_id") + "").trim());
		Session sesi = sesiBerkas();
		try {
			JSONArray arr = new JSONArray();
			int terisi = 0;
			boolean wajibLengkap = true;
			for (String[] slot : SLOT_LAMPIRAN_TAGIHAN) {
				ais.database.model.file.LampiranLain berkas = lampiranTagihan(sesi, bastId, slot[0]);
				boolean wajib = "wajib".equals(slot[2]);
				JSONObject o = new JSONObject();
				o.put("kunci", slot[0]);
				o.put("nama", slot[1]);
				o.put("wajib", wajib);
				o.put("harusGambar", "gambar".equals(slot[3]));
				o.put("ada", berkas != null);
				o.put("lampiran_id", berkas == null ? JSONObject.NULL : berkas.getId());
				o.put("namaFile", berkas == null || berkas.getNama() == null ? "" : berkas.getNama());
				o.put("keterangan", berkas == null || berkas.getKeterangan() == null ? ""
						: berkas.getKeterangan());
				arr.put(o);
				if (berkas != null) {
					terisi++;
				} else if (wajib) {
					wajibLengkap = false;
				}
			}
			hasil.put("status", "00");
			hasil.put("bast_id", bastId);
			hasil.put("data", arr);
			hasil.put("terisi", terisi);
			hasil.put("wajibLengkap", wajibLengkap);
		} finally {
			HibernateUtil.closeSessionQuietly(sesi);
		}
	}

	/**
	 * Unggah satu berkas lampiran ke sebuah slot. Berkas dikirim sebagai base64 di badan JSON --
	 * pola yang sama dengan {@code KantinHelper.produkFotoUpload}; tidak ada endpoint multipart
	 * terpisah di basis kode ini.
	 *
	 * <p>Slot yang ditandai "harus gambar" (Invoice) menolak berkas yang isinya bukan gambar.
	 * Pemeriksaan memakai magic number, bukan ekstensi nama berkas, karena nama mudah diubah
	 * sedangkan isi tidak.</p>
	 *
	 * <p>Satu slot menampung satu berkas. Mengunggah ulang MENGGANTI berkas sebelumnya, sesuai
	 * cara kerja {@code LampiranLain.ambil(ref, jenis)} di versi ZKoss yang juga mengambil satu
	 * berkas per pasangan ref+jenis.</p>
	 */
	public static void lampiranUnggah(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!berkasSiap()) {
			tolak(hasil, "Penyimpanan berkas sedang tidak dapat dihubungi, sehingga lampiran belum dapat diproses.");
			return;
		}
		if (!bolehAksi(tbmuser, KUNCI_TAGIHAN, "update") && !bolehAksi(tbmuser, KUNCI_BAST, "update")) {
			tolak(hasil, "Grup pengguna Anda tidak memiliki hak mengunggah lampiran tagihan.");
			return;
		}
		if (request == null || request.isNull("bast_id")) {
			tolak(hasil, "Parameter bast_id wajib diisi.");
			return;
		}
		Long bastId = Long.valueOf((request.get("bast_id") + "").trim());
		String[] slot = slotLampiran(request.optString("kunci", ""));
		if (slot == null) {
			tolak(hasil, "Jenis lampiran tidak dikenali.");
			return;
		}
		String base64 = request.optString("file_base64", "").trim();
		if (base64.isEmpty()) {
			tolak(hasil, "Berkas lampiran wajib diisi.");
			return;
		}
		// Panjang diperiksa SEBELUM didekode -- mendekode dulu berarti muatan sebesar apa pun
		// sudah terlanjur dialokasikan di memori sebelum ditolak.
		String tolakPanjang = ais.common.PenjagaLampiranGambar.periksaPanjangBase64(base64,
				MAKS_BYTE_LAMPIRAN);
		if (tolakPanjang != null) {
			tolak(hasil, tolakPanjang);
			return;
		}
		byte[] isi;
		try {
			isi = java.util.Base64.getDecoder().decode(base64);
		} catch (IllegalArgumentException e) {
			tolak(hasil, "Data berkas tidak valid (base64 gagal diurai).");
			return;
		}
		if (isi.length == 0) {
			tolak(hasil, "Berkas lampiran kosong.");
			return;
		}
		if (isi.length > MAKS_BYTE_LAMPIRAN) {
			tolak(hasil, "Ukuran berkas melebihi batas " + (MAKS_BYTE_LAMPIRAN / 1024 / 1024)
					+ " MB. Perkecil dahulu gambarnya.");
			return;
		}
		String tipe = jenisBerkas(isi);
		if ("gambar".equals(slot[3]) && !tipe.startsWith("image/")) {
			tolak(hasil, slot[1] + " harus berupa gambar (JPG, PNG, GIF, BMP, atau WebP). "
					+ (tipe.isEmpty() ? "Berkas yang dikirim tidak dikenali sebagai gambar."
							: "Berkas yang dikirim bertipe " + tipe + "."));
			return;
		}
		// Batas 5 MB di atas berlaku untuk lampiran apa pun. Yang berupa GAMBAR dibatasi lebih
		// ketat: 500 KB, sama dengan yang dikecilkan klien. Slot "bebas" pun ikut -- foto yang
		// dikirim sebagai Surat Jalan tidak lebih pantas besar daripada foto Invoice.
		String tolakGambar = ais.common.PenjagaLampiranGambar.periksaBilaGambar(isi);
		if (tolakGambar != null) {
			tolak(hasil, tolakGambar);
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			PenerimaanPengadaanMasterAsset bast = (PenerimaanPengadaanMasterAsset) session
					.get(PenerimaanPengadaanMasterAsset.class, bastId);
			if (bast == null) {
				tolak(hasil, "Penerimaan barang (BAST) tidak ditemukan.");
				return;
			}
			Long tokoId = tokoLingkup(tbmuser, request);
			if (tokoId != null && bast.getToko() != null && !tokoId.equals(bast.getToko().getId())) {
				tolak(hasil, "Penerimaan barang ini milik toko lain.");
				return;
			}
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
		String namaFile = request.optString("nama_file", "").trim();
		if (namaFile.isEmpty()) {
			namaFile = slot[0].toLowerCase() + "-" + bastId + ekstensiDari(tipe);
		}
		Session sesi = sesiBerkas();
		try {
			sesi.beginTransaction();
			ais.database.model.file.LampiranLain lama = lampiranTagihan(sesi, bastId, slot[0]);
			if (lama != null) {
				sesi.delete(lama);
				sesi.flush();
			}
			ais.database.model.file.LampiranLain berkas = new ais.database.model.file.LampiranLain();
			berkas.setRef(bastId);
			berkas.setJenis(JENIS_LAMPIRAN_TAGIHAN + slot[0]);
			berkas.setNama(namaFile);
			berkas.setKeterangan(request.optString("keterangan", slot[1]).trim());
			berkas.setDeskripsi(tipe);
			berkas.setFoto(org.hibernate.Hibernate.createBlob(isi));
			berkas.setOleh(tbmuser == null ? null : tbmuser.getUserNama());
			berkas.setOlehId(tbmuser == null ? null : tbmuser.getUserId());
			sesi.save(berkas);
			sesi.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("lampiran_id", berkas.getId());
			hasil.put("kunci", slot[0]);
			hasil.put("nama", slot[1]);
			hasil.put("namaFile", namaFile);
			hasil.put("tipe", tipe);
			hasil.put("ukuran", isi.length);
		} catch (Exception e) {
			try {
				if (sesi.getTransaction() != null && sesi.getTransaction().isActive()) {
					sesi.getTransaction().rollback();
				}
			} catch (Exception eRollback) {
				ais.common.ErrorAuditUtil.record(eRollback, "PengadaanPosApiHelper.lampiranUnggah rollback");
			}
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(sesi);
		}
	}

	private static String ekstensiDari(String tipe) {
		if ("image/jpeg".equals(tipe)) {
			return ".jpg";
		}
		if ("image/png".equals(tipe)) {
			return ".png";
		}
		if ("image/gif".equals(tipe)) {
			return ".gif";
		}
		if ("image/bmp".equals(tipe)) {
			return ".bmp";
		}
		if ("image/webp".equals(tipe)) {
			return ".webp";
		}
		if ("application/pdf".equals(tipe)) {
			return ".pdf";
		}
		return ".bin";
	}

	/** Ambil isi satu lampiran sebagai base64, untuk ditampilkan atau diunduh klien. */
	public static void lampiranUnduh(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!berkasSiap()) {
			tolak(hasil, "Penyimpanan berkas sedang tidak dapat dihubungi, sehingga lampiran belum dapat diproses.");
			return;
		}
		if (!bolehLihat(tbmuser, KUNCI_TAGIHAN) && !bolehLihat(tbmuser, KUNCI_BAST)) {
			tolak(hasil, "Menu Pengadaan tidak diaktifkan untuk grup pengguna Anda.");
			return;
		}
		if (request == null || request.isNull("lampiran_id")) {
			tolak(hasil, "Parameter lampiran_id wajib diisi.");
			return;
		}
		Long id = Long.valueOf((request.get("lampiran_id") + "").trim());
		Session sesi = sesiBerkas();
		try {
			ais.database.model.file.LampiranLain berkas = (ais.database.model.file.LampiranLain) sesi
					.get(ais.database.model.file.LampiranLain.class, id);
			if (berkas == null || berkas.getFoto() == null) {
				tolak(hasil, "Lampiran tidak ditemukan.");
				return;
			}
			// Kolom foto memakai PostgreSQL Large Object (oid), yang HANYA boleh dibaca
			// di dalam transaksi. Memanggil getFoto().getBinaryStream() langsung -- yang
			// dilakukan versi sebelumnya -- selalu gagal dengan "Large Objects may not be
			// used in auto-commit mode", sehingga tombol Lihat lampiran tidak pernah
			// berfungsi. Aturannya sudah dipecahkan di FileFotoLain; dipakai ulang lewat
			// ambilIsiBlob supaya tidak ada dua salinan aturan yang bisa menyimpang.
			byte[] isi = ais.database.model.file.FileFotoLain.ambilIsiBlob(berkas);
			if (isi == null) {
				tolak(hasil, "Isi lampiran tidak dapat dibaca dari penyimpanan berkas.");
				return;
			}
			hasil.put("status", "00");
			hasil.put("lampiran_id", id);
			hasil.put("namaFile", berkas.getNama() == null ? "" : berkas.getNama());
			hasil.put("tipe", berkas.getDeskripsi() == null ? "" : berkas.getDeskripsi());
			hasil.put("fileBase64", java.util.Base64.getEncoder().encodeToString(isi));
		} finally {
			HibernateUtil.closeSessionQuietly(sesi);
		}
	}

	/**
	 * Hapus satu lampiran. Slot yang wajib boleh dikosongkan, tetapi tagihannya tidak dapat
	 * diterima sampai slot itu diisi kembali (dijaga pada {@code tagihanTerima}).
	 */
	public static void lampiranHapus(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!berkasSiap()) {
			tolak(hasil, "Penyimpanan berkas sedang tidak dapat dihubungi, sehingga lampiran belum dapat diproses.");
			return;
		}
		if (!bolehAksi(tbmuser, KUNCI_TAGIHAN, "delete") && !bolehAksi(tbmuser, KUNCI_BAST, "delete")) {
			tolak(hasil, "Grup pengguna Anda tidak memiliki hak menghapus lampiran tagihan.");
			return;
		}
		if (request == null || request.isNull("lampiran_id")) {
			tolak(hasil, "Parameter lampiran_id wajib diisi.");
			return;
		}
		Long id = Long.valueOf((request.get("lampiran_id") + "").trim());
		Session sesi = sesiBerkas();
		try {
			ais.database.model.file.LampiranLain berkas = (ais.database.model.file.LampiranLain) sesi
					.get(ais.database.model.file.LampiranLain.class, id);
			if (berkas == null) {
				tolak(hasil, "Lampiran tidak ditemukan.");
				return;
			}
			sesi.beginTransaction();
			sesi.delete(berkas);
			sesi.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("lampiran_id", id);
		} catch (Exception e) {
			try {
				if (sesi.getTransaction() != null && sesi.getTransaction().isActive()) {
					sesi.getTransaction().rollback();
				}
			} catch (Exception eRollback) {
				ais.common.ErrorAuditUtil.record(eRollback, "PengadaanPosApiHelper.lampiranHapus rollback");
			}
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(sesi);
		}
	}

	/**
	 * Nama slot lampiran WAJIB yang belum terisi untuk sebuah BAST. Dipakai
	 * {@code tagihanTerima} sebagai pagar sebelum tagihan dinyatakan diterima.
	 *
	 * <p>Bila basis data berkas gagal dibaca, kekurangan dianggap NIHIL: gangguan pada
	 * penyimpanan berkas tidak boleh ikut memblokir alur tagihan. Kegagalannya dicatat.</p>
	 */
	private static java.util.List<String> lampiranWajibKurang(Long bastId) {
		java.util.List<String> kurang = new java.util.ArrayList<String>();
		Session sesi = null;
		try {
			sesi = sesiBerkas();
			for (String[] slot : SLOT_LAMPIRAN_TAGIHAN) {
				if (!"wajib".equals(slot[2])) {
					continue;
				}
				if (lampiranTagihan(sesi, bastId, slot[0]) == null) {
					kurang.add(slot[1]);
				}
			}
		} catch (Throwable e) {
			// Sengaja Throwable, bukan Exception. Bila basis data berkas gagal DIINISIALISASI
			// (mis. konfigurasinya tidak ikut terpasang), yang dilempar adalah
			// ExceptionInInitializerError/NoClassDefFoundError -- keduanya Error, bukan
			// Exception, sehingga akan lolos dari catch biasa dan mematikan seluruh alur
			// terima tagihan. Gangguan pada penyimpanan berkas tidak boleh sejauh itu
			// akibatnya: dicatat, lalu kekurangan dianggap nihil.
			ais.common.ErrorAuditUtil.record(e instanceof Exception ? (Exception) e
					: new Exception(e), "PengadaanPosApiHelper.lampiranWajibKurang bast=" + bastId);
			return new java.util.ArrayList<String>();
		} finally {
			HibernateUtil.closeSessionQuietly(sesi);
		}
		return kurang;
	}

	/**
	 * Pilihan Cara Transfer untuk layar Pembayaran Vendor, meniru combo "Cara Transfer *" pada
	 * {@code ProsesTransferAction} versi ZKoss.
	 *
	 * <p>Penyaringnya disamakan dengan versi ZKoss: hanya {@code CaraPembayaranTransfer} yang
	 * AKTIF dan sudah memiliki {@code akun}. Syarat akun bukan formalitas -- akun itulah yang
	 * dipakai saat jurnal dibentuk, sehingga cara transfer tanpa akun akan menghasilkan
	 * pembayaran yang tidak dapat dijurnal.</p>
	 */
	public static void caraBayarOpsi(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehLihat(tbmuser, KUNCI_DPC)) {
			tolak(hasil, "Menu Pengadaan tidak diaktifkan untuk grup pengguna Anda.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			@SuppressWarnings("unchecked")
			List<ais.database.model.akunting.CaraPembayaranTransfer> daftar = session
					.createCriteria(ais.database.model.akunting.CaraPembayaranTransfer.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
					.add(Restrictions.isNotNull("akun"))
					.addOrder(Order.asc("nama")).list();
			JSONArray arr = new JSONArray();
			Long bawaan = null;
			for (ais.database.model.akunting.CaraPembayaranTransfer c : daftar) {
				JSONObject o = new JSONObject();
				o.put("id", c.getId());
				o.put("kode", c.getKode() == null ? "" : c.getKode());
				o.put("nama", c.getNama() == null ? "" : c.getNama());
				o.put("keterangan", c.getKeterangan() == null ? "" : c.getKeterangan());
				o.put("akun", c.getAkun() == null || c.getAkun().getNama() == null ? ""
						: c.getAkun().getNama());
				boolean isBawaan = Boolean.TRUE.equals(c.getDefaultPembayaran());
				o.put("bawaan", isBawaan);
				if (isBawaan && bawaan == null) {
					bawaan = c.getId();
				}
				arr.put(o);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("bawaan_id", bawaan == null ? JSONObject.NULL : bawaan);
			if (arr.length() == 0) {
				hasil.put("catatan", "Belum ada Cara Transfer yang aktif dan memiliki akun. "
						+ "Atur dahulu pada master Cara Pembayaran Transfer agar pembayaran "
						+ "dapat dijurnal.");
			}
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Rincian KEKURANGAN kiriman sebuah Pemesanan Pembelian: per baris, berapa yang dipesan,
	 * berapa yang sudah diterima lewat seluruh BAST, dan berapa yang kurang.
	 *
	 * <p>Dipakai layar Penerimaan Barang untuk menyiapkan tombol "Back Order / Pesan Kembali"
	 * ketika barang datang tidak sesuai pesanan. Tidak menulis apa pun.</p>
	 */
	public static void poKekurangan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehLihat(tbmuser, KUNCI_PO) && !bolehLihat(tbmuser, KUNCI_BAST)) {
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
			@SuppressWarnings("unchecked")
			List<PemesananPengadaanMasterAssetDetail> baris = session
					.createCriteria(PemesananPengadaanMasterAssetDetail.class)
					.add(Restrictions.eq("pemesananPengadaanMasterAsset.id", po.getId()))
					.addOrder(Order.asc("id")).list();
			JSONArray arr = new JSONArray();
			double nilaiKurang = 0;
			for (PemesananPengadaanMasterAssetDetail d : baris) {
				double dipesan = d.getJumlah() == null ? 0 : d.getJumlah().doubleValue();
				double diterima = jumlahSudahDiterima(session, d.getId(), null);
				double kurang = dipesan - diterima;
				double harga = d.getHargaBeli() == null ? 0 : d.getHargaBeli().doubleValue();
				JSONObject o = new JSONObject();
				o.put("po_detail_id", d.getId());
				Produk produkBaris = produkDariMasterAsset(session, d.getMasterAsset());
				o.put("produk_id", produkBaris == null ? JSONObject.NULL : produkBaris.getId());
				o.put("master_asset_id", d.getMasterAsset() == null ? JSONObject.NULL
						: d.getMasterAsset().getId());
				o.put("kodeBarang", d.getMasterAsset() == null || d.getMasterAsset().getKode() == null ? ""
						: d.getMasterAsset().getKode());
				o.put("barang", d.getMasterAsset() == null ? "" : d.getMasterAsset().getNama());
				o.put("produk", d.getMasterAsset() == null ? "" : d.getMasterAsset().getNama());
				o.put("dipesan", dipesan);
				o.put("diterima", diterima);
				o.put("kurang", Math.max(0, kurang));
				o.put("hargaBeli", harga);
				o.put("nilaiKurang", Math.max(0, kurang) * harga);
				o.put("pr_detail_id", d.getPermintaanPengadaanMasterAssetDetail() == null ? JSONObject.NULL
						: d.getPermintaanPengadaanMasterAssetDetail().getId());
				arr.put(o);
				if (kurang > 0) {
					nilaiKurang += kurang * harga;
				}
			}
			hasil.put("status", "00");
			hasil.put("po_id", po.getId());
			hasil.put("po", po.getKode() == null ? "" : po.getKode());
			hasil.put("penyedia_id", po.getPenyedia() == null ? JSONObject.NULL : po.getPenyedia().getId());
			hasil.put("penyedia", po.getPenyedia() == null ? "" : po.getPenyedia().getNama());
			hasil.put("tutup", Boolean.TRUE.equals(po.getTutup()));
			hasil.put("alasanTutup", po.getAlasanTutup() == null ? "" : po.getAlasanTutup());

			/* Pesanan susulan yang MASIH HIDUP disertakan supaya layar dapat membuka
			 * kembali keputusan Back Order untuk DIREVISI, bukan menampilkan alert
			 * buntu "sudah ditutup". Yang sudah ditolak sengaja tidak dihitung: ia
			 * bukan lagi pesanan yang berlaku, dan induknya pun sudah dibuka kembali. */
			{
				@SuppressWarnings("unchecked")
				java.util.List<PemesananPengadaanMasterAsset> susulanList = session
						.createCriteria(PemesananPengadaanMasterAsset.class)
						.add(Restrictions.eq("poInduk.id", po.getId()))
						.add(Restrictions.isNull("tanggalDitolak"))
						.addOrder(Order.desc("id")).setMaxResults(1).list();
				if (!susulanList.isEmpty()) {
					PemesananPengadaanMasterAsset sus = susulanList.get(0);
					JSONObject js = new JSONObject();
					js.put("id", sus.getId());
					js.put("kode", sus.getKode() == null ? "" : sus.getKode());
					js.put("status", statusPo(sus));
					js.put("penyedia_id", sus.getPenyedia() == null ? JSONObject.NULL
							: sus.getPenyedia().getId());
					js.put("penyedia", sus.getPenyedia() == null ? "" : sus.getPenyedia().getNama());
					js.put("keterangan", sus.getKeterangan() == null ? "" : sus.getKeterangan());
					js.put("pengirimanPalingLambat", sus.getPengirimanPalingLambat() == null ? ""
							: Common.dateFormat.get().format(sus.getPengirimanPalingLambat()));
					@SuppressWarnings("unchecked")
					java.util.List<PemesananPengadaanMasterAssetDetail> barisSus = session
							.createCriteria(PemesananPengadaanMasterAssetDetail.class)
							.add(Restrictions.eq("pemesananPengadaanMasterAsset.id", sus.getId()))
							.list();
					JSONArray arrSus = new JSONArray();
					for (PemesananPengadaanMasterAssetDetail ds : barisSus) {
						JSONObject od = new JSONObject();
						od.put("master_asset_id", ds.getMasterAsset() == null ? JSONObject.NULL
								: ds.getMasterAsset().getId());
						od.put("jumlah", ds.getJumlah() == null ? 0 : ds.getJumlah().doubleValue());
						arrSus.put(od);
					}
					js.put("detail", arrSus);
					hasil.put("susulan", js);
				}
			}
			hasil.put("detail", arr);
			hasil.put("nilaiKurang", nilaiKurang);
			hasil.put("adaKekurangan", nilaiKurang > 0);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Back Order / Pesan Kembali: menutup sisa pesanan yang tidak jadi dikirim, dan -- bila
	 * barangnya masih dibutuhkan -- menerbitkan pesanan susulan atas kekurangan itu.
	 *
	 * <p><b>Mengapa dua langkah sekaligus.</b> Ini praktik baku pengadaan di lapangan. Sisa
	 * pesanan lama harus DITUTUP lebih dulu, kalau tidak jumlah yang sama akan terhitung dua kali
	 * -- sekali pada pesanan lama yang masih terbuka, sekali lagi pada pesanan susulan -- dan
	 * Permintaan Pembelian asalnya akan tampak dipesan melebihi yang diminta. Karena itu
	 * {@link #jumlahSudahDipesan} menghitung pesanan yang sudah ditutup sebatas yang benar-benar
	 * diterima saja.</p>
	 *
	 * <p><b>Pilihan tindakan</b> ({@code tindakan}):</p>
	 * <ul>
	 * <li>{@code pesan_kembali} (bawaan) -- tutup sisa lama, lalu terbitkan PO susulan berisi
	 * baris-baris yang kurang. Penyedia bawaannya sama, tetapi boleh dialihkan ke penyedia lain
	 * lewat {@code penyedia_id} -- hal yang lazim ketika vendor lama tidak sanggup memenuhi.</li>
	 * <li>{@code tutup_saja} -- sisa dibatalkan tanpa pesanan susulan.</li>
	 * </ul>
	 *
	 * <p>PO susulan sengaja diterbitkan sebagai DRAF yang belum disetujui: pesanan baru tetap
	 * harus melewati persetujuan seperti pesanan lain. Ia juga dibuat TANPA termin, karena
	 * nilainya berbeda dari pesanan asal sehingga jadwal termin lama tidak lagi berlaku.</p>
	 *
	 * @param request {@code po_id} (wajib), {@code alasan} (wajib), {@code tindakan},
	 *                {@code penyedia_id} (opsional), {@code pengirimanPalingLambat} (opsional,
	 *                dd-MM-yyyy), {@code detail} (opsional: daftar {@code po_detail_id} +
	 *                {@code jumlah} bila hanya sebagian yang dipesan ulang).
	 */
	public static void poBackOrder(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehAksi(tbmuser, KUNCI_PO, "create")) {
			tolak(hasil, "Grup pengguna Anda tidak memiliki hak menerbitkan pesanan susulan.");
			return;
		}
		if (tbmuser == null) {
			tolak(hasil, "Sesi pengguna tidak dikenali, silakan masuk ulang.");
			return;
		}
		Long poId = (request == null || request.isNull("po_id")) ? null
				: Long.valueOf((request.get("po_id") + "").trim());
		if (poId == null) {
			tolak(hasil, "Parameter po_id wajib diisi.");
			return;
		}
		String alasan = request.optString("alasan", "").trim();
		if (alasan.isEmpty()) {
			tolak(hasil, "Alasan wajib diisi -- keputusan menutup sisa pesanan harus dapat ditelusuri.");
			return;
		}
		/* Penerimaan yang memicu keputusan ini. Opsional: pemanggil lama (ZKoss/JSP) tidak
		 * mengirimnya, dan untuk mereka SEMUA penerimaan draf atas pesanan ini yang diselesaikan. */
		Long bastIdPemicu = (request == null || request.isNull("bast_id")) ? null
				: Long.valueOf((request.get("bast_id") + "").trim());
		String tindakan = request.optString("tindakan", "pesan_kembali").trim().toLowerCase();
		if (!"pesan_kembali".equals(tindakan) && !"tutup_saja".equals(tindakan)) {
			tolak(hasil, "Tindakan hanya boleh pesan_kembali atau tutup_saja.");
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
			/* REVISI. Keputusan Back Order kerap perlu diperbaiki -- jumlah salah
			 * ketik, penyedia berubah, batas kirim bergeser. Sebelumnya tidak ada
			 * jalan sama sekali: pesanan sudah tertutup dan permintaan kedua ditolak,
			 * sehingga satu-satunya pilihan adalah menolak pesanan susulannya lalu
			 * mengulang dari awal -- itu pun baru mungkin setelah r77947.
			 *
			 * Dengan `revisi`, pesanan susulan yang masih hidup DIBATALKAN lebih dulu
			 * (ditandai ditolak, bukan dihapus -- jejaknya harus tetap ada), lalu
			 * keputusan baru diterbitkan seperti biasa. Pesanan susulan yang sudah
			 * menerima barang TIDAK boleh dibatalkan begitu saja. */
			boolean revisi = request.optBoolean("revisi", false);
			if (Boolean.TRUE.equals(po.getTutup())) {
				if (!revisi) {
					tolak(hasil, "Sisa Pemesanan Pembelian " + (po.getKode() == null ? "" : po.getKode())
							+ " sudah pernah ditutup.");
					return;
				}
				@SuppressWarnings("unchecked")
				java.util.List<PemesananPengadaanMasterAsset> hidup = session
						.createCriteria(PemesananPengadaanMasterAsset.class)
						.add(Restrictions.eq("poInduk.id", po.getId()))
						.add(Restrictions.isNull("tanggalDitolak")).list();
				session.beginTransaction();
				for (int i = 0; i < hidup.size(); i++) {
					PemesananPengadaanMasterAsset lamaSus = hidup.get(i);
					// Susulan yang sudah menerima barang tidak boleh dibatalkan begitu saja:
					// penerimaannya akan menggantung tanpa pesanan yang sah.
					Number jmlBast = (Number) session
							.createCriteria(PenerimaanPengadaanMasterAsset.class)
							.add(Restrictions.eq("pemesananPengadaanMasterAsset.id", lamaSus.getId()))
							.setProjection(org.hibernate.criterion.Projections.rowCount()).uniqueResult();
					if (jmlBast != null && jmlBast.intValue() > 0) {
						try { session.getTransaction().rollback(); } catch (Exception abaikan) {
							ais.common.ErrorAuditUtil.record(abaikan,
									"auto-audit(empty-catch) PengadaanPosApiHelper.poBackOrder:revisi-rollback");
						}
						tolak(hasil, "Pesanan susulan " + (lamaSus.getKode() == null ? "" : lamaSus.getKode())
								+ " sudah menerima barang, sehingga keputusan Back Order tidak dapat direvisi."
								+ " Buat penerimaan atau retur pada pesanan tersebut.");
						return;
					}
					lamaSus.setTanggalDitolak(ais.ui.util.WaktuUtil.getDate());
					lamaSus.setDitolakOleh(tbmuser);
					lamaSus.setAlasanDitolak("Dibatalkan karena keputusan Back Order direvisi.");
					lamaSus.setTanggalPersetujuan(null);
					lamaSus.setDisetujuiOleh(null);
					session.saveOrUpdate(lamaSus);
				}
				po.setTutup(Boolean.FALSE);
				session.saveOrUpdate(po);
				session.flush();
				session.getTransaction().commit();
				hasil.put("revisiDariSusulan", hidup.size());
			}
			if (po.getTanggalPersetujuan() == null) {
				tolak(hasil, "Pemesanan Pembelian yang belum disetujui tidak perlu di-back order; "
						+ "cukup sunting atau batalkan pesanannya.");
				return;
			}

			// Kekurangan per baris, dan penyesuaian bila pengguna hanya memesan ulang sebagian.
			java.util.Map<Long, Double> mintaUlang = new java.util.HashMap<Long, Double>();
			JSONArray pilihan = request.optJSONArray("detail");
			boolean adaPilihan = pilihan != null && pilihan.length() > 0;
			if (adaPilihan) {
				for (int i = 0; i < pilihan.length(); i++) {
					JSONObject b = pilihan.optJSONObject(i);
					if (b == null || b.isNull("po_detail_id")) {
						continue;
					}
					mintaUlang.put(Long.valueOf((b.get("po_detail_id") + "").trim()),
							Double.valueOf(angkaAman(b, "jumlah")));
				}
			}

			@SuppressWarnings("unchecked")
			List<PemesananPengadaanMasterAssetDetail> baris = session
					.createCriteria(PemesananPengadaanMasterAssetDetail.class)
					.add(Restrictions.eq("pemesananPengadaanMasterAsset.id", po.getId()))
					.addOrder(Order.asc("id")).list();
			java.util.List<PemesananPengadaanMasterAssetDetail> kandidat =
					new java.util.ArrayList<PemesananPengadaanMasterAssetDetail>();
			java.util.List<Double> jumlahUlang = new java.util.ArrayList<Double>();
			double totalKurang = 0;
			for (PemesananPengadaanMasterAssetDetail d : baris) {
				double dipesan = d.getJumlah() == null ? 0 : d.getJumlah().doubleValue();
				double diterima = jumlahSudahDiterima(session, d.getId(), null);
				double kurang = dipesan - diterima;
				if (kurang <= TOLERANSI) {
					continue;
				}
				totalKurang += kurang;
				double minta = kurang;
				if (adaPilihan) {
					Double dipilih = mintaUlang.get(d.getId());
					minta = dipilih == null ? 0 : dipilih.doubleValue();
					if (minta > kurang + TOLERANSI) {
						tolak(hasil, "Jumlah pesan ulang untuk "
								+ (d.getMasterAsset() == null ? "barang" : d.getMasterAsset().getNama())
								+ " (" + hapusNolEkor(minta) + ") melebihi kekurangannya ("
								+ hapusNolEkor(kurang) + ").");
						return;
					}
				}
				if (minta > TOLERANSI) {
					kandidat.add(d);
					jumlahUlang.add(Double.valueOf(minta));
				}
			}
			if (totalKurang <= TOLERANSI) {
				tolak(hasil, "Tidak ada kekurangan pada " + (po.getKode() == null ? "pesanan ini" : po.getKode())
						+ " -- seluruh barang sudah diterima lengkap.");
				return;
			}
			if ("pesan_kembali".equals(tindakan) && kandidat.isEmpty()) {
				tolak(hasil, "Tidak ada barang yang dipilih untuk dipesan ulang.");
				return;
			}
			// Nilai pesanan setelah ditutup = sebatas yang benar-benar diterima. Tanpa ini
			// penyedia masih dapat menagih barang yang tidak pernah dikirim, karena daftar
			// tagihan terbuka membaca PemesananPengadaanMasterAsset.nilai.
			double nilaiDiterima = 0;
			for (PemesananPengadaanMasterAssetDetail d : baris) {
				double harga = d.getHargaBeli() == null ? 0 : d.getHargaBeli().doubleValue();
				nilaiDiterima += jumlahSudahDiterima(session, d.getId(), null) * harga;
			}
			boolean nilaiDisesuaikan = false;

			session.beginTransaction();
			// Langkah 1 -- tutup sisa pesanan lama. Selalu dilakukan, baik dengan maupun tanpa
			// pesanan susulan, supaya sisa yang batal tidak terhitung dua kali.
			po.setTutup(Boolean.TRUE);
			po.setAlasanTutup(alasan);
			// Pesanan BERTERMIN tidak disesuaikan otomatis: jadwal terminnya disusun manusia
			// dan penjumlahannya harus tetap sama dengan nilai pesanan. Menyesuaikan nilai
			// tanpa menyusun ulang termin justru membuat dokumennya tidak konsisten.
			if (!Boolean.TRUE.equals(po.getByTermin())) {
				po.setNilai(Double.valueOf(nilaiDiterima));
				nilaiDisesuaikan = true;
			}
			session.saveOrUpdate(po);
			session.flush();

			PemesananPengadaanMasterAsset baru = null;
			double nilaiBaru = 0;
			if ("pesan_kembali".equals(tindakan)) {
				PenyediaAsset penyediaBaru = penyediaDokumen(session, request);
				baru = new PemesananPengadaanMasterAsset();
				baru.setPoInduk(po);
				baru.setPenyedia(penyediaBaru == null ? po.getPenyedia() : penyediaBaru);
				baru.setToko(po.getToko());
				baru.setSatuanKerja(po.getSatuanKerja());
				baru.setKeterangan("Pesanan susulan atas kekurangan kiriman "
						+ (po.getKode() == null ? "" : po.getKode()) + ". Alasan: " + alasan);
				baru.setTanggalPembuatan(ais.ui.util.WaktuUtil.getDate());
				baru.setDibuatOleh(tbmuser);
				baru.setAktif(Boolean.TRUE);
				// TANPA termin: nilai pesanan susulan berbeda dari pesanan asal, sehingga jadwal
				// termin lama tidak lagi berlaku dan harus disusun ulang bila memang diperlukan.
				baru.setByTermin(Boolean.FALSE);
				baru.setDp(Double.valueOf(0));
				java.util.Date batasKirim = tanggalKetat(request.optString("pengirimanPalingLambat", ""));
				if (batasKirim != null) {
					baru.setPengirimanPalingLambat(batasKirim);
				}
				baru.setKode(buatKodeUmum(session, PemesananPengadaanMasterAsset.class, "PO",
						po.getToko() == null ? null : po.getToko().getId()));
				baru.setOleh(tbmuser.getUserNama());
				baru.setOlehId(tbmuser.getUserId());
				session.save(baru);
				session.flush();

				java.util.LinkedHashSet<String> prDetailIds = new java.util.LinkedHashSet<String>();
				for (int i = 0; i < kandidat.size(); i++) {
					PemesananPengadaanMasterAssetDetail asal = kandidat.get(i);
					double jumlah = jumlahUlang.get(i).doubleValue();
					double harga = asal.getHargaBeli() == null ? 0 : asal.getHargaBeli().doubleValue();
					PemesananPengadaanMasterAssetDetail d = new PemesananPengadaanMasterAssetDetail();
					d.setPemesananPengadaanMasterAsset(baru);
					d.setMasterAsset(asal.getMasterAsset());
					d.setJumlah(Double.valueOf(jumlah));
					d.setHargaBeli(Double.valueOf(harga));
					d.setHargaTotal(Double.valueOf(jumlah * harga));
					d.setKeterangan(asal.getKeterangan());
					// Tautan ke baris Permintaan Pembelian asal DIPERTAHANKAN supaya perhitungan
					// sisa permintaan tetap utuh sepanjang rantai pesanan susulan.
					if (asal.getPermintaanPengadaanMasterAssetDetail() != null) {
						d.setPermintaanPengadaanMasterAssetDetail(
								asal.getPermintaanPengadaanMasterAssetDetail());
						prDetailIds.add(asal.getPermintaanPengadaanMasterAssetDetail().getId() + "");
					}
					d.setOleh(tbmuser.getUserNama());
					d.setOlehId(tbmuser.getUserId());
					session.save(d);
					nilaiBaru += jumlah * harga;
				}
				StringBuilder jejakPr = new StringBuilder();
				for (String satu : prDetailIds) {
					jejakPr.append(jejakPr.length() == 0 ? "" : ",").append(satu);
				}
				baru.setPermintaanPengadaanMasterAssets(jejakPr.length() == 0 ? null : jejakPr.toString());
				baru.setNilai(Double.valueOf(nilaiBaru));
				session.saveOrUpdate(baru);
			}
			List<String> bastDisetujui = new java.util.ArrayList<String>();
			/* Langkah terakhir -- penerimaan yang memicu Back Order ikut DISETUJUI.
			 *
			 * <p><b>Mengapa.</b> Sesudah sisa pesanan ditutup, tidak ada lagi barang yang akan
			 * menyusul ke penerimaan itu: isinya sudah final. Membiarkannya DRAF memaksa petugas
			 * menyetujui sekali lagi dokumen yang keputusannya baru saja ia ambil, dan bila lupa,
			 * penerimaan yang sah menggantung sebagai draf sementara pesanannya sudah tertutup.</p>
			 *
			 * <p><b>Tetap dijaga hak akses.</b> Hanya dijalankan bila pengguna memang berhak
			 * menyetujui penerimaan. Petugas gudang yang tidak berhak TIDAK menyetujui dokumennya
			 * sendiri lewat jalan belakang ini -- baginya dokumen tetap DRAF menunggu atasan,
			 * persis seperti sebelumnya. JANGAN hilangkan penjagaan ini.</p>
			 *
			 * <p>Penerimaan yang sudah disetujui tidak disentuh, sehingga tanggal dan penyetuju
			 * aslinya tidak tertimpa ketika keputusan Back Order direvisi.</p> */
			if (bolehAksi(tbmuser, KUNCI_BAST, "approve")) {
				@SuppressWarnings("unchecked")
				List<PenerimaanPengadaanMasterAsset> bastPesanan = session
						.createCriteria(PenerimaanPengadaanMasterAsset.class)
						.add(Restrictions.eq("pemesananPengadaanMasterAsset.id", po.getId())).list();
				for (PenerimaanPengadaanMasterAsset satuBast : bastPesanan) {
					if (satuBast == null || Boolean.FALSE.equals(satuBast.getAktif())
							|| satuBast.getTanggalPersetujuan() != null) {
						continue;
					}
					if (bastIdPemicu != null && !bastIdPemicu.equals(satuBast.getId())) {
						continue;
					}
					satuBast.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());
					satuBast.setDisetujuiOleh(tbmuser);
					satuBast.setOleh(tbmuser.getUserNama());
					satuBast.setOlehId(tbmuser.getUserId());
					session.saveOrUpdate(satuBast);
					bastDisetujui.add(satuBast.getKode() == null ? (satuBast.getId() + "") : satuBast.getKode());
				}
			}
			session.getTransaction().commit();

			hasil.put("status", "00");
			hasil.put("po_id", po.getId());
			hasil.put("po", po.getKode() == null ? "" : po.getKode());
			hasil.put("tindakan", tindakan);
			hasil.put("ditutup", true);
			hasil.put("bastDisetujui", new JSONArray(bastDisetujui));
			hasil.put("nilaiDisesuaikan", nilaiDisesuaikan);
			hasil.put("nilaiSetelahTutup", nilaiDisesuaikan ? nilaiDiterima
					: (po.getNilai() == null ? 0 : po.getNilai().doubleValue()));
			hasil.put("po_baru_id", baru == null ? JSONObject.NULL : baru.getId());
			hasil.put("po_baru", baru == null ? "" : (baru.getKode() == null ? "" : baru.getKode()));
			hasil.put("nilaiBaru", nilaiBaru);
			String catatanTermin = Boolean.TRUE.equals(po.getByTermin())
					? " Pesanan ini bertermin, jadi nilai dan jadwal terminnya perlu Anda sesuaikan sendiri."
					: "";
			hasil.put("description", catatanTermin + (baru == null
					? "Sisa pesanan ditutup. Tidak ada pesanan susulan yang diterbitkan."
					: "Sisa pesanan ditutup dan pesanan susulan "
							+ (baru.getKode() == null ? "" : baru.getKode())
							+ " diterbitkan sebagai draf. Setujui dahulu sebelum dikirim ke penyedia."));
		} catch (Exception e) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception eRollback) {
				ais.common.ErrorAuditUtil.record(eRollback, "PengadaanPosApiHelper.poBackOrder rollback");
			}
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ================= Dasbor Pengadaan =================
	//
	// Satu aksi melayani seluruh tahap; bentuk balasannya SERAGAM (kpi, tren,
	// komposisi, peringkat, daftar, corong) sehingga Desktop, Android, dan JSP
	// cukup punya SATU perender untuk enam dasbor. Isi tiap tahap meniru dasbor
	// yang sudah ada di versi ZKoss: TraceStatusPengadaanAssetDashboard (kartu
	// ringkasan tahapan + tabel proses) dan DasboardPajak (kartu + tren bulanan +
	// komposisi jenis pajak).

	/** Berapa bulan ke belakang yang ditarik dasbor bila klien tidak menentukan. */
	private static final int BULAN_DASBOR_BAWAAN = 12;

	private static java.util.Date awalPeriodeDasbor(int bulan) {
		java.util.Calendar kal = java.util.Calendar.getInstance();
		kal.setTime(ais.ui.util.WaktuUtil.getDate());
		kal.add(java.util.Calendar.MONTH, -(Math.max(1, bulan) - 1));
		kal.set(java.util.Calendar.DAY_OF_MONTH, 1);
		kal.set(java.util.Calendar.HOUR_OF_DAY, 0);
		kal.set(java.util.Calendar.MINUTE, 0);
		kal.set(java.util.Calendar.SECOND, 0);
		kal.set(java.util.Calendar.MILLISECOND, 0);
		return kal.getTime();
	}

	private static final String[] NAMA_BULAN_SINGKAT = { "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
			"Jul", "Ags", "Sep", "Okt", "Nov", "Des" };

	private static String kunciBulan(java.util.Date tgl) {
		if (tgl == null) {
			return "";
		}
		java.util.Calendar kal = java.util.Calendar.getInstance();
		kal.setTime(tgl);
		int b = kal.get(java.util.Calendar.MONTH) + 1;
		return kal.get(java.util.Calendar.YEAR) + "-" + (b < 10 ? "0" + b : "" + b);
	}

	private static String labelBulan(String kunci) {
		if (kunci == null || kunci.length() < 7) {
			return kunci == null ? "" : kunci;
		}
		int bln = Integer.parseInt(kunci.substring(5, 7));
		return NAMA_BULAN_SINGKAT[Math.max(0, Math.min(11, bln - 1))] + " " + kunci.substring(2, 4);
	}

	/**
	 * Kerangka deret bulanan yang SELALU lengkap sepanjang periode, termasuk bulan
	 * yang nihil. Grafik tren yang bolong-bolong menyesatkan pembacanya -- naik
	 * turunnya jadi tampak lebih tajam daripada kenyataannya.
	 */
	private static java.util.LinkedHashMap<String, double[]> kerangkaBulan(int bulan) {
		java.util.LinkedHashMap<String, double[]> peta = new java.util.LinkedHashMap<String, double[]>();
		java.util.Calendar kal = java.util.Calendar.getInstance();
		kal.setTime(awalPeriodeDasbor(bulan));
		for (int i = 0; i < Math.max(1, bulan); i++) {
			int b = kal.get(java.util.Calendar.MONTH) + 1;
			peta.put(kal.get(java.util.Calendar.YEAR) + "-" + (b < 10 ? "0" + b : "" + b),
					new double[] { 0, 0 });
			kal.add(java.util.Calendar.MONTH, 1);
		}
		return peta;
	}

	private static void tambahBulan(java.util.LinkedHashMap<String, double[]> peta,
			java.util.Date tgl, double nilai) {
		String k = kunciBulan(tgl);
		double[] sel = peta.get(k);
		if (sel == null) {
			return;
		}
		sel[0] += 1;
		sel[1] += nilai;
	}

	private static JSONArray trenDari(java.util.LinkedHashMap<String, double[]> peta) throws Exception {
		JSONArray arr = new JSONArray();
		for (java.util.Map.Entry<String, double[]> e : peta.entrySet()) {
			JSONObject o = new JSONObject();
			o.put("kunci", e.getKey());
			o.put("label", labelBulan(e.getKey()));
			o.put("jumlah", e.getValue()[0]);
			o.put("nilai", e.getValue()[1]);
			arr.put(o);
		}
		return arr;
	}

	private static JSONObject kpi(String label, String nilai, String catatan, String warna) throws Exception {
		JSONObject o = new JSONObject();
		o.put("label", label);
		o.put("nilai", nilai);
		o.put("catatan", catatan == null ? "" : catatan);
		o.put("warna", warna);
		return o;
	}

	private static JSONObject titik(String label, double nilai) throws Exception {
		JSONObject o = new JSONObject();
		o.put("label", label);
		o.put("nilai", nilai);
		return o;
	}

	/** Urutkan peta label->nilai menurun, ambil beberapa teratas. */
	private static JSONArray peringkatDari(java.util.Map<String, Double> peta, int maks) throws Exception {
		java.util.List<java.util.Map.Entry<String, Double>> daftar =
				new java.util.ArrayList<java.util.Map.Entry<String, Double>>(peta.entrySet());
		java.util.Collections.sort(daftar, new java.util.Comparator<java.util.Map.Entry<String, Double>>() {
			public int compare(java.util.Map.Entry<String, Double> a, java.util.Map.Entry<String, Double> b) {
				return Double.compare(b.getValue().doubleValue(), a.getValue().doubleValue());
			}
		});
		JSONArray arr = new JSONArray();
		for (int i = 0; i < daftar.size() && i < maks; i++) {
			arr.put(titik(daftar.get(i).getKey(), daftar.get(i).getValue().doubleValue()));
		}
		return arr;
	}

	private static void tambahPeta(java.util.Map<String, Double> peta, String kunci, double nilai) {
		if (kunci == null || kunci.trim().isEmpty()) {
			kunci = "(tanpa nama)";
		}
		Double lama = peta.get(kunci);
		peta.put(kunci, Double.valueOf((lama == null ? 0 : lama.doubleValue()) + nilai));
	}

	/**
	 * Dasbor satu tahap pengadaan. Parameter {@code tahap}: pr, po, bast, tagihan,
	 * dpc, atau pajak. Opsional {@code bulan} (bawaan 12) dan {@code toko_id}.
	 *
	 * <p>Tidak menulis apa pun. Hak aksesnya mengikuti menu tahap yang diminta,
	 * sehingga pengguna tidak dapat melihat ringkasan tahap yang menunya memang
	 * tidak diaktifkan untuk grup-nya.</p>
	 */
	public static void dasbor(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		String tahap = request == null ? "" : request.optString("tahap", "").trim().toLowerCase();
		String kunciMenu = "pr".equals(tahap) ? KUNCI_PR
				: "po".equals(tahap) ? KUNCI_PO
				: "bast".equals(tahap) ? KUNCI_BAST
				: "tagihan".equals(tahap) ? KUNCI_TAGIHAN
				: "dpc".equals(tahap) ? KUNCI_DPC
				: "pajak".equals(tahap) ? KUNCI_PAJAK : null;
		if (kunciMenu == null) {
			tolak(hasil, "Tahap dasbor tidak dikenali. Pilih salah satu: pr, po, bast, tagihan, dpc, pajak.");
			return;
		}
		if (!bolehLihat(tbmuser, kunciMenu)) {
			tolak(hasil, "Menu Pengadaan tidak diaktifkan untuk grup pengguna Anda.");
			return;
		}
		int bulan = Math.min(36, Math.max(3, request.optInt("bulan", BULAN_DASBOR_BAWAAN)));
		Long tokoId = tokoLingkup(tbmuser, request);
		java.util.Date sejak = awalPeriodeDasbor(bulan);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			hasil.put("status", "00");
			hasil.put("tahap", tahap);
			hasil.put("bulan", bulan);
			hasil.put("sejak", Common.dateFormat1.get().format(sejak));
			if ("pr".equals(tahap)) {
				dasborPr(session, tokoId, sejak, bulan, hasil);
			} else if ("po".equals(tahap)) {
				dasborPo(session, tokoId, sejak, bulan, hasil);
			} else if ("bast".equals(tahap)) {
				dasborBast(session, tokoId, sejak, bulan, hasil);
			} else if ("tagihan".equals(tahap)) {
				dasborTagihan(session, tokoId, sejak, bulan, hasil);
			} else if ("dpc".equals(tahap)) {
				dasborDpc(session, tokoId, sejak, bulan, hasil);
			} else {
				dasborPajak(session, tbmuser, tokoId, sejak, bulan, hasil);
			}
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Dasbor Permintaan Pembelian, lengkap dengan CORONG tahapan
	 * PR -> PO -> BAST -> Tagihan -> Bayar. Corong ini padanan "Ringkasan Tahapan"
	 * pada TraceStatusPengadaanAssetDashboard versi ZKoss: satu pandangan yang
	 * memperlihatkan di tahap mana permintaan tersendat.
	 */
	private static void dasborPr(Session session, Long tokoId, java.util.Date sejak, int bulan,
			JSONObject hasil) throws Exception {
		Criteria kriteria = session.createCriteria(PermintaanPengadaanMasterAsset.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
				.add(Restrictions.ge("tanggalPembuatan", sejak));
		if (tokoId != null) {
			kriteria.add(Restrictions.eq("toko.id", tokoId));
		}
		@SuppressWarnings("unchecked")
		List<PermintaanPengadaanMasterAsset> daftar = kriteria.addOrder(Order.desc("id"))
				.setMaxResults(5000).list();

		java.util.LinkedHashMap<String, double[]> tren = kerangkaBulan(bulan);
		java.util.Map<String, Double> komposisi = new java.util.LinkedHashMap<String, Double>();
		java.util.Map<String, Double> barangTeratas = new java.util.HashMap<String, Double>();
		int draf = 0, disetujui = 0, ditolak = 0, tutup = 0;
		double nilaiTotal = 0, nilaiDisetujui = 0;
		int capaiPo = 0, capaiBast = 0, capaiTagihan = 0, capaiBayar = 0;
		JSONArray menunggu = new JSONArray();
		java.util.Date hariIni = ais.ui.util.WaktuUtil.getDate();

		for (PermintaanPengadaanMasterAsset pr : daftar) {
			double nilai = nilaiPr(session, pr, barangTeratas);
			nilaiTotal += nilai;
			tambahBulan(tren, pr.getTanggalPembuatan(), nilai);
			String status = statusPr(pr);
			tambahPeta(komposisi, status, 1);
			if ("DISETUJUI".equals(status)) {
				disetujui++;
				nilaiDisetujui += nilai;
			} else if ("DITOLAK".equals(status)) {
				ditolak++;
			} else if ("DRAFT".equals(status)) {
				draf++;
			}
			if (Boolean.TRUE.equals(pr.getTutup())) {
				tutup++;
			}
			int capai = tahapTercapaiPr(session, pr);
			if (capai >= 1) capaiPo++;
			if (capai >= 2) capaiBast++;
			if (capai >= 3) capaiTagihan++;
			if (capai >= 4) capaiBayar++;
			if (pr.getTanggalPersetujuan() == null && pr.getTanggalDitolak() == null
					&& menunggu.length() < 10) {
				JSONObject o = new JSONObject();
				o.put("kode", pr.getKode() == null ? "" : pr.getKode());
				o.put("keterangan", pr.getKeterangan() == null ? "" : pr.getKeterangan());
				o.put("nilai", nilai);
				o.put("umurHari", umurHari(pr.getTanggalPembuatan(), hariIni));
				menunggu.put(o);
			}
		}

		JSONArray kartu = new JSONArray();
		kartu.put(kpi("Total PR", "" + daftar.size(), "permintaan dalam periode", "#1d4ed8"));
		kartu.put(kpi("Nilai Diajukan", Common.numberFormat.get().format(nilaiTotal),
				"seluruh permintaan", "#0ea5e9"));
		kartu.put(kpi("Disetujui", "" + disetujui,
				Common.numberFormat.get().format(nilaiDisetujui) + " nilai disetujui", "#15803d"));
		kartu.put(kpi("Menunggu Persetujuan", "" + draf, "belum diputuskan", "#b45309"));
		kartu.put(kpi("Ditolak", "" + ditolak, "tidak dilanjutkan", "#dc2626"));
		kartu.put(kpi("Ditutup", "" + tutup, "tidak dipesan lagi", "#64748b"));

		JSONArray corong = new JSONArray();
		corong.put(titik("PR", daftar.size()));
		corong.put(titik("Sudah PO", capaiPo));
		corong.put(titik("Sudah BAST", capaiBast));
		corong.put(titik("Sudah Tagihan", capaiTagihan));
		corong.put(titik("Sudah Dibayar", capaiBayar));

		hasil.put("kpi", kartu);
		hasil.put("tren", trenDari(tren));
		hasil.put("komposisi", peringkatDari(komposisi, 8));
		hasil.put("peringkat", peringkatDari(barangTeratas, 8));
		hasil.put("peringkatJudul", "Barang Paling Sering Diminta");
		hasil.put("corong", corong);
		hasil.put("daftar", menunggu);
		hasil.put("daftarJudul", "Menunggu Persetujuan Paling Lama");
		hasil.put("catatanKosong", "Belum ada Permintaan Pembelian pada periode ini.");
	}

	/**
	 * Nilai sebuah PR (jumlah x harga seluruh barisnya). Sekalian mengisi peta
	 * peringkat barang bila peta-nya disediakan, supaya tabel PR hanya dibaca sekali.
	 */
	private static double nilaiPr(Session session, PermintaanPengadaanMasterAsset pr,
			java.util.Map<String, Double> barangTeratas) {
		@SuppressWarnings("unchecked")
		List<PermintaanPengadaanMasterAssetDetail> baris = session
				.createCriteria(PermintaanPengadaanMasterAssetDetail.class)
				.add(Restrictions.eq("permintaanPengadaanMasterAsset.id", pr.getId())).list();
		double total = 0;
		for (PermintaanPengadaanMasterAssetDetail d : baris) {
			double jml = d.getJumlah() == null ? 0 : d.getJumlah().doubleValue();
			double harga = d.getHargaBeli() == null ? 0 : d.getHargaBeli().doubleValue();
			total += jml * harga;
			if (barangTeratas != null && d.getMasterAsset() != null) {
				tambahPeta(barangTeratas, d.getMasterAsset().getNama(), jml);
			}
		}
		return total;
	}

	/**
	 * Sejauh mana sebuah PR sudah berjalan: 0 belum dipesan, 1 sudah PO,
	 * 2 sudah diterima (BAST), 3 sudah bertagihan, 4 sudah dibayar.
	 *
	 * <p>Ditelusuri lewat rantai baris: PR detail -> PO detail -> BAST detail ->
	 * BAST (kodeTagihan) -> pembayaran termin. Cara yang sama dipakai
	 * TraceStatusPengadaanAssetDashboard versi ZKoss untuk menyusun Ringkasan Tahapan.</p>
	 */
	private static int tahapTercapaiPr(Session session, PermintaanPengadaanMasterAsset pr) {
		@SuppressWarnings("unchecked")
		List<PermintaanPengadaanMasterAssetDetail> baris = session
				.createCriteria(PermintaanPengadaanMasterAssetDetail.class)
				.add(Restrictions.eq("permintaanPengadaanMasterAsset.id", pr.getId())).list();
		int tertinggi = 0;
		for (PermintaanPengadaanMasterAssetDetail d : baris) {
			@SuppressWarnings("unchecked")
			List<PemesananPengadaanMasterAssetDetail> po = session
					.createCriteria(PemesananPengadaanMasterAssetDetail.class)
					.add(Restrictions.eq("permintaanPengadaanMasterAssetDetail.id", d.getId())).list();
			for (PemesananPengadaanMasterAssetDetail dp : po) {
				PemesananPengadaanMasterAsset induk = dp.getPemesananPengadaanMasterAsset();
				if (induk == null || Boolean.FALSE.equals(induk.getAktif())) {
					continue;
				}
				if (tertinggi < 1) tertinggi = 1;
				@SuppressWarnings("unchecked")
				List<PenerimaanPengadaanMasterAssetDetail> bast = session
						.createCriteria(PenerimaanPengadaanMasterAssetDetail.class)
						.add(Restrictions.eq("pemesananPengadaanMasterAssetDetail.id", dp.getId())).list();
				for (PenerimaanPengadaanMasterAssetDetail db : bast) {
					PenerimaanPengadaanMasterAsset ib = db.getPenerimaanPengadaanMasterAsset();
					if (ib == null || Boolean.FALSE.equals(ib.getAktif())) {
						continue;
					}
					if (tertinggi < 2) tertinggi = 2;
					if (ib.getKodeTagihan() != null && !ib.getKodeTagihan().trim().isEmpty()
							&& tertinggi < 3) {
						tertinggi = 3;
					}
				}
				if (tertinggi >= 3 && terpakaiPembayaranPo(session, induk.getId(), null, true) > 0
						&& tertinggi < 4) {
					tertinggi = 4;
				}
			}
		}
		return tertinggi;
	}

	/** Dasbor Pemesanan Pembelian: nilai pesanan, pembayaran, dan kiriman tertunda. */
	private static void dasborPo(Session session, Long tokoId, java.util.Date sejak, int bulan,
			JSONObject hasil) throws Exception {
		Criteria kriteria = session.createCriteria(PemesananPengadaanMasterAsset.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
				.add(Restrictions.ge("tanggalPembuatan", sejak));
		if (tokoId != null) {
			kriteria.add(Restrictions.eq("toko.id", tokoId));
		}
		@SuppressWarnings("unchecked")
		List<PemesananPengadaanMasterAsset> daftar = kriteria.addOrder(Order.desc("id"))
				.setMaxResults(5000).list();

		java.util.LinkedHashMap<String, double[]> tren = kerangkaBulan(bulan);
		java.util.Map<String, Double> komposisi = new java.util.LinkedHashMap<String, Double>();
		java.util.Map<String, Double> penyediaTeratas = new java.util.HashMap<String, Double>();
		int draf = 0, disetujui = 0, lunas = 0, ditutup = 0, terlambat = 0;
		double nilaiTotal = 0, dibayarTotal = 0;
		JSONArray jatuhTempo = new JSONArray();
		java.util.Date hariIni = ais.ui.util.WaktuUtil.getDate();

		for (PemesananPengadaanMasterAsset po : daftar) {
			double nilai = po.getNilai() == null ? 0 : po.getNilai().doubleValue();
			double dibayar = terpakaiPembayaranPo(session, po.getId(), null, true);
			nilaiTotal += nilai;
			dibayarTotal += dibayar;
			tambahBulan(tren, po.getTanggalPembuatan(), nilai);
			String status = statusPo(po);
			tambahPeta(komposisi, status, 1);
			if ("LUNAS".equals(status)) lunas++;
			else if ("DISETUJUI".equals(status)) disetujui++;
			else if ("DRAFT".equals(status)) draf++;
			if (Boolean.TRUE.equals(po.getTutup())) ditutup++;
			if (po.getPenyedia() != null) {
				tambahPeta(penyediaTeratas, po.getPenyedia().getNama(), nilai);
			}
			boolean lewat = po.getPengirimanPalingLambat() != null
					&& po.getPengirimanPalingLambat().before(hariIni)
					&& !Boolean.TRUE.equals(po.getTutup());
			if (lewat) {
				terlambat++;
				if (jatuhTempo.length() < 10) {
					JSONObject o = new JSONObject();
					o.put("kode", po.getKode() == null ? "" : po.getKode());
					o.put("keterangan", po.getPenyedia() == null ? "" : po.getPenyedia().getNama());
					o.put("nilai", nilai);
					o.put("umurHari", umurHari(po.getPengirimanPalingLambat(), hariIni));
					jatuhTempo.put(o);
				}
			}
		}

		JSONArray kartu = new JSONArray();
		kartu.put(kpi("Total PO", "" + daftar.size(), "pesanan dalam periode", "#1d4ed8"));
		kartu.put(kpi("Nilai Pesanan", Common.numberFormat.get().format(nilaiTotal),
				"seluruh pesanan", "#0ea5e9"));
		kartu.put(kpi("Sudah Dibayar", Common.numberFormat.get().format(dibayarTotal),
				"pembayaran yang sudah disetujui", "#15803d"));
		kartu.put(kpi("Sisa Kewajiban", Common.numberFormat.get().format(Math.max(0, nilaiTotal - dibayarTotal)),
				"belum dibayar", "#dc2626"));
		kartu.put(kpi("Menunggu Persetujuan", "" + draf, "belum diputuskan", "#b45309"));
		kartu.put(kpi("Lewat Batas Kirim", "" + terlambat, "perlu ditagih ke penyedia", "#ea580c"));
		kartu.put(kpi("Lunas", "" + lunas, "pembayaran selesai", "#4338ca"));
		kartu.put(kpi("Ditutup", "" + ditutup, "sisa dibatalkan lewat back order", "#64748b"));

		hasil.put("kpi", kartu);
		hasil.put("tren", trenDari(tren));
		hasil.put("komposisi", peringkatDari(komposisi, 8));
		hasil.put("peringkat", peringkatDari(penyediaTeratas, 8));
		hasil.put("peringkatJudul", "Penyedia dengan Nilai Pesanan Terbesar");
		hasil.put("daftar", jatuhTempo);
		hasil.put("daftarJudul", "Pesanan Lewat Batas Kirim");
		hasil.put("catatanKosong", "Belum ada Pemesanan Pembelian pada periode ini.");
	}

	/** Dasbor Penerimaan Barang: nilai yang diterima dan yang sudah menjadi stok. */
	private static void dasborBast(Session session, Long tokoId, java.util.Date sejak, int bulan,
			JSONObject hasil) throws Exception {
		Criteria kriteria = session.createCriteria(PenerimaanPengadaanMasterAsset.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
				.add(Restrictions.ge("tanggalPembuatan", sejak));
		if (tokoId != null) {
			kriteria.add(Restrictions.eq("toko.id", tokoId));
		}
		@SuppressWarnings("unchecked")
		List<PenerimaanPengadaanMasterAsset> daftar = kriteria.addOrder(Order.desc("id"))
				.setMaxResults(5000).list();

		java.util.LinkedHashMap<String, double[]> tren = kerangkaBulan(bulan);
		java.util.Map<String, Double> komposisi = new java.util.LinkedHashMap<String, Double>();
		java.util.Map<String, Double> penyediaTeratas = new java.util.HashMap<String, Double>();
		int disetujui = 0, masukStok = 0;
		double nilaiTotal = 0, nilaiDisetujui = 0;
		JSONArray belumSetuju = new JSONArray();
		java.util.Date hariIni = ais.ui.util.WaktuUtil.getDate();

		for (PenerimaanPengadaanMasterAsset bast : daftar) {
			double nilai = bast.getNilai() == null ? 0 : bast.getNilai().doubleValue();
			nilaiTotal += nilai;
			tambahBulan(tren, bast.getTanggalPembuatan(), nilai);
			tambahPeta(komposisi, statusBast(bast), 1);
			if (bast.getPenyedia() != null) {
				tambahPeta(penyediaTeratas, bast.getPenyedia().getNama(), nilai);
			}
			if (bast.getDisetujuiOleh() != null) {
				disetujui++;
				nilaiDisetujui += nilai;
			} else if (belumSetuju.length() < 10) {
				JSONObject o = new JSONObject();
				o.put("kode", bast.getKode() == null ? "" : bast.getKode());
				o.put("keterangan", bast.getPenyedia() == null ? "" : bast.getPenyedia().getNama());
				o.put("nilai", nilai);
				o.put("umurHari", umurHari(bast.getTanggalPembuatan(), hariIni));
				belumSetuju.put(o);
			}
			if (bast.getPengadaanFaktur() != null) {
				masukStok++;
			}
		}

		JSONArray kartu = new JSONArray();
		kartu.put(kpi("Total BAST", "" + daftar.size(), "penerimaan dalam periode", "#1d4ed8"));
		kartu.put(kpi("Nilai Diterima", Common.numberFormat.get().format(nilaiTotal),
				"seluruh penerimaan", "#0ea5e9"));
		kartu.put(kpi("Sudah Disetujui", "" + disetujui,
				Common.numberFormat.get().format(nilaiDisetujui) + " nilai", "#15803d"));
		kartu.put(kpi("Belum Disetujui", "" + (daftar.size() - disetujui), "menunggu putusan", "#b45309"));
		kartu.put(kpi("Sudah Masuk Stok", "" + masukStok, "tersalin ke faktur Kulakan", "#4338ca"));
		kartu.put(kpi("Belum Masuk Stok", "" + (daftar.size() - masukStok), "perlu disinkronkan", "#ea580c"));

		hasil.put("kpi", kartu);
		hasil.put("tren", trenDari(tren));
		hasil.put("komposisi", peringkatDari(komposisi, 8));
		hasil.put("peringkat", peringkatDari(penyediaTeratas, 8));
		hasil.put("peringkatJudul", "Penyedia dengan Nilai Penerimaan Terbesar");
		hasil.put("daftar", belumSetuju);
		hasil.put("daftarJudul", "Penerimaan Menunggu Persetujuan Paling Lama");
		hasil.put("catatanKosong", "Belum ada Penerimaan Barang pada periode ini.");
	}

	/** Dasbor Terima Tagihan: seberapa cepat penerimaan berubah menjadi tagihan. */
	private static void dasborTagihan(Session session, Long tokoId, java.util.Date sejak, int bulan,
			JSONObject hasil) throws Exception {
		Criteria kriteria = session.createCriteria(PenerimaanPengadaanMasterAsset.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
				.add(Restrictions.isNotNull("tanggalPersetujuan"))
				.add(Restrictions.ge("tanggalPembuatan", sejak));
		if (tokoId != null) {
			kriteria.add(Restrictions.eq("toko.id", tokoId));
		}
		@SuppressWarnings("unchecked")
		List<PenerimaanPengadaanMasterAsset> daftar = kriteria.addOrder(Order.desc("id"))
				.setMaxResults(5000).list();

		java.util.LinkedHashMap<String, double[]> tren = kerangkaBulan(bulan);
		java.util.Map<String, Double> komposisi = new java.util.LinkedHashMap<String, Double>();
		java.util.Map<String, Double> penyediaTeratas = new java.util.HashMap<String, Double>();
		int sudah = 0;
		double nilaiSudah = 0, nilaiBelum = 0, totalUmur = 0;
		JSONArray belum = new JSONArray();
		java.util.Date hariIni = ais.ui.util.WaktuUtil.getDate();

		for (PenerimaanPengadaanMasterAsset bast : daftar) {
			double nilai = bast.getNilai() == null ? 0 : bast.getNilai().doubleValue();
			boolean adaTagihan = bast.getKodeTagihan() != null && !bast.getKodeTagihan().trim().isEmpty();
			tambahPeta(komposisi, adaTagihan ? "SUDAH BERTAGIHAN" : "BELUM BERTAGIHAN", 1);
			if (adaTagihan) {
				sudah++;
				nilaiSudah += nilai;
				tambahBulan(tren, bast.getTanggalTagihan() == null ? bast.getTanggalPembuatan()
						: bast.getTanggalTagihan(), nilai);
				if (bast.getPenyedia() != null) {
					tambahPeta(penyediaTeratas, bast.getPenyedia().getNama(), nilai);
				}
			} else {
				nilaiBelum += nilai;
				totalUmur += umurHari(bast.getTanggalPersetujuan(), hariIni);
				if (belum.length() < 10) {
					JSONObject o = new JSONObject();
					o.put("kode", bast.getKode() == null ? "" : bast.getKode());
					o.put("keterangan", bast.getPenyedia() == null ? "" : bast.getPenyedia().getNama());
					o.put("nilai", nilai);
					o.put("umurHari", umurHari(bast.getTanggalPersetujuan(), hariIni));
					belum.put(o);
				}
			}
		}
		int jumlahBelum = daftar.size() - sudah;
		long rataUmur = jumlahBelum == 0 ? 0 : Math.round(totalUmur / jumlahBelum);

		JSONArray kartu = new JSONArray();
		kartu.put(kpi("Siap Ditagih", "" + daftar.size(), "penerimaan yang sudah disetujui", "#1d4ed8"));
		kartu.put(kpi("Sudah Bertagihan", "" + sudah,
				Common.numberFormat.get().format(nilaiSudah) + " nilai", "#15803d"));
		kartu.put(kpi("Belum Bertagihan", "" + jumlahBelum,
				Common.numberFormat.get().format(nilaiBelum) + " nilai", "#b45309"));
		kartu.put(kpi("Rata-rata Menunggu", rataUmur + " hari",
				"sejak penerimaan disetujui", "#ea580c"));

		hasil.put("kpi", kartu);
		hasil.put("tren", trenDari(tren));
		hasil.put("trenJudul", "Nilai Tagihan Diterima per Bulan");
		hasil.put("komposisi", peringkatDari(komposisi, 8));
		hasil.put("peringkat", peringkatDari(penyediaTeratas, 8));
		hasil.put("peringkatJudul", "Penyedia dengan Nilai Tagihan Terbesar");
		hasil.put("daftar", belum);
		hasil.put("daftarJudul", "Belum Bertagihan Paling Lama");
		hasil.put("catatanKosong", "Belum ada penerimaan yang siap ditagih pada periode ini.");
	}

	/** Dasbor Pembayaran Vendor: nilai yang dibayar dan kewajiban yang masih terbuka. */
	private static void dasborDpc(Session session, Long tokoId, java.util.Date sejak, int bulan,
			JSONObject hasil) throws Exception {
		Criteria kriteria = session.createCriteria(PembayaranTerminMasterAsset.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
				.add(Restrictions.ge("tanggalPembuatan", sejak));
		if (tokoId != null) {
			kriteria.add(Restrictions.eq("toko.id", tokoId));
		}
		@SuppressWarnings("unchecked")
		List<PembayaranTerminMasterAsset> daftar = kriteria.addOrder(Order.desc("id"))
				.setMaxResults(5000).list();

		java.util.LinkedHashMap<String, double[]> tren = kerangkaBulan(bulan);
		java.util.Map<String, Double> komposisi = new java.util.LinkedHashMap<String, Double>();
		java.util.Map<String, Double> caraTeratas = new java.util.LinkedHashMap<String, Double>();
		java.util.Map<String, Double> penyediaTeratas = new java.util.HashMap<String, Double>();
		int draf = 0, disetujui = 0, tanpaCara = 0;
		double nilaiDisetujui = 0, nilaiDraf = 0;
		JSONArray drafTerlama = new JSONArray();
		java.util.Date hariIni = ais.ui.util.WaktuUtil.getDate();

		for (PembayaranTerminMasterAsset b : daftar) {
			double nilai = b.getNilaiDibayar() == null ? 0 : b.getNilaiDibayar().doubleValue();
			tambahPeta(komposisi, statusBayar(b), 1);
			if (b.getDisetujuiOleh() != null) {
				disetujui++;
				nilaiDisetujui += nilai;
				tambahBulan(tren, b.getTanggalPembuatan(), nilai);
				tambahPeta(caraTeratas, b.getCaraPembayaranTransfer() == null ? "(belum ditentukan)"
						: b.getCaraPembayaranTransfer().getNama(), nilai);
				if (b.getPenyedia() != null) {
					tambahPeta(penyediaTeratas, b.getPenyedia().getNama(), nilai);
				}
			} else {
				draf++;
				nilaiDraf += nilai;
				if (drafTerlama.length() < 10) {
					JSONObject o = new JSONObject();
					o.put("kode", b.getKode() == null ? "" : b.getKode());
					o.put("keterangan", b.getPenyedia() == null ? "" : b.getPenyedia().getNama());
					o.put("nilai", nilai);
					o.put("umurHari", umurHari(b.getTanggalPembuatan(), hariIni));
					drafTerlama.put(o);
				}
			}
			if (b.getCaraPembayaranTransfer() == null) {
				tanpaCara++;
			}
		}

		JSONArray kartu = new JSONArray();
		kartu.put(kpi("Dokumen Pembayaran", "" + daftar.size(), "dalam periode", "#1d4ed8"));
		kartu.put(kpi("Sudah Dibayar", Common.numberFormat.get().format(nilaiDisetujui),
				disetujui + " dokumen disetujui", "#15803d"));
		kartu.put(kpi("Masih Draf", "" + draf,
				Common.numberFormat.get().format(nilaiDraf) + " menunggu persetujuan", "#b45309"));
		kartu.put(kpi("Tanpa Cara Transfer", "" + tanpaCara,
				"belum dapat dijurnal", tanpaCara > 0 ? "#dc2626" : "#64748b"));

		hasil.put("kpi", kartu);
		hasil.put("tren", trenDari(tren));
		hasil.put("trenJudul", "Nilai Pembayaran Disetujui per Bulan");
		hasil.put("komposisi", peringkatDari(komposisi, 8));
		hasil.put("peringkat", peringkatDari(penyediaTeratas, 8));
		hasil.put("peringkatJudul", "Penyedia dengan Pembayaran Terbesar");
		hasil.put("caraBayar", peringkatDari(caraTeratas, 8));
		hasil.put("daftar", drafTerlama);
		hasil.put("daftarJudul", "Draf Pembayaran Paling Lama");
		hasil.put("catatanKosong", "Belum ada pembayaran vendor pada periode ini.");
	}

	/**
	 * Dasbor Bayar Pajak. Meniru DasboardPajak versi ZKoss: kartu ringkasan,
	 * tren bulanan, dan komposisi per jenis pajak -- ditambah pemisahan sumber
	 * (BAST atau pembayaran vendor) yang khas modul POS.
	 */
	private static void dasborPajak(Session session, Tbmuser tbmuser, Long tokoId,
			java.util.Date sejak, int bulan,
			JSONObject hasil) throws Exception {
		// --- Yang masih terutang, dari kedua sumber -------------------------------
		JSONObject terutang = new JSONObject();
		JSONObject permintaan = new JSONObject();
		if (tokoId != null) {
			permintaan.put("toko_id", tokoId);
		}
		// Sengaja memakai aksi yang sama dengan layar Bayar Pajak, bukan menyalin
		// perhitungannya: satu definisi "terutang" untuk dasbor dan daftar.
		pajakTerutang(tbmuser, permintaan, terutang);
		JSONArray barisTerutang = terutang.optJSONArray("data");
		double pphTerutang = 0, ppnTerutang = 0;
		int dariBast = 0, dariBayar = 0;
		java.util.Map<String, Double> komposisi = new java.util.LinkedHashMap<String, Double>();
		JSONArray terbesar = new JSONArray();
		if (barisTerutang != null) {
			java.util.List<JSONObject> urut = new java.util.ArrayList<JSONObject>();
			for (int i = 0; i < barisTerutang.length(); i++) {
				JSONObject x = barisTerutang.getJSONObject(i);
				pphTerutang += x.optDouble("pph", 0);
				ppnTerutang += x.optDouble("ppn", 0);
				if ("BAST".equals(x.optString("sumber"))) {
					dariBast++;
				} else {
					dariBayar++;
				}
				urut.add(x);
			}
			java.util.Collections.sort(urut, new java.util.Comparator<JSONObject>() {
				public int compare(JSONObject a, JSONObject b) {
					return Double.compare(b.optDouble("pph", 0) + b.optDouble("ppn", 0),
							a.optDouble("pph", 0) + a.optDouble("ppn", 0));
				}
			});
			for (int i = 0; i < urut.size() && i < 10; i++) {
				JSONObject x = urut.get(i);
				JSONObject o = new JSONObject();
				o.put("kode", x.optString("dokumen", ""));
				o.put("keterangan", x.optString("penyedia", ""));
				o.put("nilai", x.optDouble("pph", 0) + x.optDouble("ppn", 0));
				o.put("umurHari", 0);
				terbesar.put(o);
			}
		}
		tambahPeta(komposisi, "Dari BAST", dariBast);
		tambahPeta(komposisi, "Dari Pembayaran", dariBayar);

		// --- Yang sudah disetor ---------------------------------------------------
		Criteria kriteria = session.createCriteria(ais.database.model.akunting.Pajak.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
				.add(Restrictions.isNotNull("tanggalStor"))
				.add(Restrictions.ge("tanggalStor", sejak));
		@SuppressWarnings("unchecked")
		List<ais.database.model.akunting.Pajak> setoran = kriteria.addOrder(Order.desc("id"))
				.setMaxResults(5000).list();

		java.util.LinkedHashMap<String, double[]> tren = kerangkaBulan(bulan);
		java.util.Map<String, Double> jenisTeratas = new java.util.HashMap<String, Double>();
		double nilaiSetor = 0, dppSetor = 0;
		for (ais.database.model.akunting.Pajak p : setoran) {
			double nilai = p.getNilai() == null ? 0 : p.getNilai().doubleValue();
			nilaiSetor += nilai;
			dppSetor += p.getDpp() == null ? 0 : p.getDpp().doubleValue();
			tambahBulan(tren, p.getTanggalStor(), nilai);
			String jenis = p.getJenisPajakBarang() != null && p.getJenisPajakBarang().getNama() != null
					? p.getJenisPajakBarang().getNama()
					: (p.getJenisPajakPpn() != null ? "PPN" : "(tanpa jenis)");
			tambahPeta(jenisTeratas, jenis, nilai);
		}

		JSONArray kartu = new JSONArray();
		kartu.put(kpi("PPh Terutang", Common.numberFormat.get().format(pphTerutang),
				"belum disetor ke negara", pphTerutang > 0 ? "#dc2626" : "#64748b"));
		kartu.put(kpi("PPN Tercatat", Common.numberFormat.get().format(ppnTerutang),
				"belum disetor", "#ea580c"));
		kartu.put(kpi("Baris Terutang", "" + (barisTerutang == null ? 0 : barisTerutang.length()),
				dariBast + " dari BAST, " + dariBayar + " dari pembayaran", "#b45309"));
		kartu.put(kpi("Sudah Disetor", Common.numberFormat.get().format(nilaiSetor),
				setoran.size() + " setoran dalam periode", "#15803d"));
		kartu.put(kpi("Total DPP Disetor", Common.numberFormat.get().format(dppSetor),
				"dasar pengenaan pajak", "#7c3aed"));

		hasil.put("kpi", kartu);
		hasil.put("tren", trenDari(tren));
		hasil.put("trenJudul", "Nilai Setoran Pajak per Bulan");
		hasil.put("komposisi", peringkatDari(komposisi, 8));
		hasil.put("komposisiJudul", "Sumber Pajak Terutang");
		hasil.put("peringkat", peringkatDari(jenisTeratas, 8));
		hasil.put("peringkatJudul", "Komposisi per Jenis Pajak (sudah disetor)");
		hasil.put("daftar", terbesar);
		hasil.put("daftarJudul", "Pajak Terutang Terbesar");
		hasil.put("catatanKosong", "Belum ada catatan pajak pada periode ini.");
	}

	/**
	 * Pencarian Anggaran (Workspace) untuk pemilih pada layar Permintaan Pembelian.
	 *
	 * <p>Mengembalikan pagu, realisasi, dan sisanya supaya pengaju melihat kemampuan
	 * anggaran SEBELUM permintaan diajukan -- bukan setelah ditolak penyetuju.</p>
	 *
	 * <p>Sisa dihitung dari kolom yang sudah dipelihara modul Anggaran sendiri
	 * ({@code hargaTotal} dikurangi {@code realisasiTotal} dan {@code realisasiProses}),
	 * bukan dihitung ulang di sini. Menghitung ulang berarti membuat definisi kedua
	 * yang cepat atau lambat berbeda dengan angka pada layar Anggaran.</p>
	 */
	public static void cariAnggaran(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehLihat(tbmuser, KUNCI_PR) && !bolehLihat(tbmuser, KUNCI_PO)
				&& !bolehLihat(tbmuser, KUNCI_TAGIHAN)) {
			tolak(hasil, "Menu Pengadaan tidak diaktifkan untuk grup pengguna Anda.");
			return;
		}
		String q = request == null ? "" : request.optString("keyword", "").trim();
		if (q.isEmpty() && request != null) {
			q = request.optString("cari", "").trim();
		}
		int limit = Math.min(200, Math.max(5, request == null ? 50 : request.optInt("limit", 50)));
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Criteria kriteria = session.createCriteria(ais.database.model.rab.Workspace.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)));
			if (q.length() > 0) {
				kriteria.add(Restrictions.or(
						Restrictions.ilike("kode", q, MatchMode.ANYWHERE),
						Restrictions.ilike("nama", q, MatchMode.ANYWHERE)));
			}
			@SuppressWarnings("unchecked")
			List<ais.database.model.rab.Workspace> daftar = kriteria.addOrder(Order.asc("kode"))
					.setMaxResults(limit).list();
			JSONArray arr = new JSONArray();
			for (ais.database.model.rab.Workspace w : daftar) {
				double pagu = w.getHargaTotal() == null ? 0 : w.getHargaTotal().doubleValue();
				double realisasi = w.getRealisasiTotal() == null ? 0 : w.getRealisasiTotal().doubleValue();
				double proses = w.getRealisasiProses() == null ? 0 : w.getRealisasiProses().doubleValue();
				JSONObject o = new JSONObject();
				o.put("id", w.getId());
				o.put("kode", w.getKode() == null ? "" : w.getKode());
				o.put("nama", w.getNama() == null ? "" : w.getNama());
				o.put("tahun", w.getTahunWorkspace() == null ? JSONObject.NULL : w.getTahunWorkspace());
				o.put("pagu", pagu);
				o.put("realisasi", realisasi);
				o.put("dalamProses", proses);
				o.put("sisa", pagu - realisasi - proses);
				arr.put(o);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
			if (arr.length() == 0) {
				hasil.put("catatan", "Tidak ada anggaran aktif yang cocok. Periksa menu Anggaran, "
						+ "atau centang \"Tanpa anggaran\" bila permintaan ini memang tidak "
						+ "membebani anggaran.");
			}
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Cetak satu dokumen pengadaan menjadi PDF, lalu kembalikan URL-nya supaya klien
	 * dapat MENAMPILKAN PRATINJAU lebih dulu sebelum benar-benar dicetak.
	 *
	 * <p><b>Templatnya sama persis dengan versi ZKoss</b> -- berkas JasperReports di
	 * {@code /report/asset/*} -- dan parameternya dibangun oleh metode yang sama pula
	 * ({@code PermintaanPengadaanMasterAssetAction.parameter}, dan seterusnya). Menyalin
	 * pembangun parameter ke sini akan melahirkan dokumen kedua yang lambat laun berbeda
	 * isinya dari cetakan ZKoss tanpa ada yang menyadarinya.</p>
	 *
	 * <p>Cara penyajian mengikuti pola yang sudah dipakai {@code LaporanApi}: berkas PDF
	 * dihasilkan di sisi server, lalu yang dikirim adalah URL-nya -- bukan base64 -- supaya
	 * dokumen besar tidak membebani muatan JSON dan peramban/pembaca PDF bisa langsung
	 * menampilkannya.</p>
	 *
	 * @param request {@code tahap} (pr, po, bast, tagihan, dpc) dan {@code id} dokumen.
	 */
	public static void cetakDokumen(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		String tahap = request == null ? "" : request.optString("tahap", "").trim().toLowerCase();
		String kunciMenu = "pr".equals(tahap) ? KUNCI_PR
				: "po".equals(tahap) ? KUNCI_PO
				: "bast".equals(tahap) ? KUNCI_BAST
				: "tagihan".equals(tahap) ? KUNCI_TAGIHAN
				: "dpc".equals(tahap) ? KUNCI_DPC
				: "pajak".equals(tahap) ? KUNCI_PAJAK : null;
		if (kunciMenu == null) {
			tolak(hasil, "Tahap cetak tidak dikenali. Pilih salah satu: pr, po, bast, tagihan, dpc, pajak.");
			return;
		}
		if (!bolehLihat(tbmuser, kunciMenu)) {
			tolak(hasil, "Menu Pengadaan tidak diaktifkan untuk grup pengguna Anda.");
			return;
		}
		Long id = (request.isNull("id") || (request.get("id") + "").trim().isEmpty()) ? null
				: Long.valueOf((request.get("id") + "").trim());
		if (id == null) {
			tolak(hasil, "Parameter id dokumen wajib diisi.");
			return;
		}
		Long tokoId = tokoLingkup(tbmuser, request);
		Session session = HibernateUtil.getSessionFactory().openSession();
		java.util.Map<?, ?> parameter = null;
		String templat = null;
		java.util.Date tanggal = null;
		String kode = "";
		try {
			if ("pr".equals(tahap)) {
				PermintaanPengadaanMasterAsset d = (PermintaanPengadaanMasterAsset) session
						.get(PermintaanPengadaanMasterAsset.class, id);
				if (!milikToko(hasil, d == null ? null : (d.getToko() == null ? null : d.getToko().getId()),
						tokoId, d == null)) {
					return;
				}
				parameter = ais.action.master.asset.PermintaanPengadaanMasterAssetAction.parameter(d);
				templat = "asset/permintaan_pengadaan";
				tanggal = d.getTanggalPembuatan();
				kode = d.getKode() == null ? "" : d.getKode();
			} else if ("po".equals(tahap)) {
				PemesananPengadaanMasterAsset d = (PemesananPengadaanMasterAsset) session
						.get(PemesananPengadaanMasterAsset.class, id);
				if (!milikToko(hasil, d == null ? null : (d.getToko() == null ? null : d.getToko().getId()),
						tokoId, d == null)) {
					return;
				}
				parameter = ais.action.master.asset.PemesananPengadaanMasterAssetAction.parameter(d);
				templat = "asset/pemesanan_pengadaan";
				tanggal = d.getTanggalPembuatan();
				kode = d.getKode() == null ? "" : d.getKode();
			} else if ("bast".equals(tahap) || "tagihan".equals(tahap)) {
				PenerimaanPengadaanMasterAsset d = (PenerimaanPengadaanMasterAsset) session
						.get(PenerimaanPengadaanMasterAsset.class, id);
				if (!milikToko(hasil, d == null ? null : (d.getToko() == null ? null : d.getToko().getId()),
						tokoId, d == null)) {
					return;
				}
				parameter = ais.action.master.asset.PenerimaanPengadaanMasterAssetAction.parameter(d);
				templat = "asset/penerimaan_pengadaan";
				tanggal = d.getTanggalPembuatan();
				kode = "tagihan".equals(tahap)
						? (d.getKodeTagihan() == null || d.getKodeTagihan().trim().isEmpty()
								? (d.getKode() == null ? "" : d.getKode()) : d.getKodeTagihan())
						: (d.getKode() == null ? "" : d.getKode());
			} else if ("pajak".equals(tahap)) {
				ais.database.model.akunting.Pajak d = (ais.database.model.akunting.Pajak) session
						.get(ais.database.model.akunting.Pajak.class, id);
				if (!milikToko(hasil, null, null, d == null)) {
					return;
				}
				parameter = parameterCetakPajak(d, tbmuser);
				templat = TEMPLAT_BUKTI_SETOR_PAJAK;
				tanggal = d.getTanggalStor() == null ? d.getTanggal() : d.getTanggalStor();
				kode = d.getKode() == null ? "" : d.getKode();
			} else {
				PembayaranTerminMasterAsset d = (PembayaranTerminMasterAsset) session
						.get(PembayaranTerminMasterAsset.class, id);
				if (!milikToko(hasil, d == null ? null : (d.getToko() == null ? null : d.getToko().getId()),
						tokoId, d == null)) {
					return;
				}
				parameter = ais.action.master.asset.PembayaranTerminMasterAssetAction.parameter(d);
				templat = "asset/pembayaran_termin";
				tanggal = d.getTanggalPembuatan();
				kode = d.getKode() == null ? "" : d.getKode();
			}
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}

		java.io.File berkas = ais.action.report.Report.generateFileReport(
				ais.action.report.Report.PDF, parameter, templat,
				tanggal == null ? ais.ui.util.WaktuUtil.getDate() : tanggal,
				(java.util.List) null, Common.locale);
		if (berkas == null) {
			tolak(hasil, "Dokumen gagal dicetak. Periksa apakah templat laporan tersedia di server.");
			return;
		}
		hasil.put("status", "00");
		hasil.put("tahap", tahap);
		hasil.put("kode", kode);
		hasil.put("namaFile", berkas.getName());
		hasil.put("url", urlBerkasLaporan(berkas.getName()));
		// Isi berkas ikut dikirim sebagai base64 supaya Desktop/Android dapat langsung
		// menampilkan PRATINJAU tanpa unduhan terpisah. JSP cukup memakai url di atas.
		// Dibatasi ukurannya: dokumen yang tidak wajar besar tetap dapat diambil lewat url,
		// tanpa membuat muatan JSON membengkak.
		try {
			if (berkas.length() > 0 && berkas.length() <= MAKS_BYTE_CETAK) {
				java.io.InputStream masuk = new java.io.FileInputStream(berkas);
				java.io.ByteArrayOutputStream keluar = new java.io.ByteArrayOutputStream();
				byte[] penyangga = new byte[8192];
				int n;
				while ((n = masuk.read(penyangga)) > 0) {
					keluar.write(penyangga, 0, n);
				}
				masuk.close();
				hasil.put("fileBase64",
						java.util.Base64.getEncoder().encodeToString(keluar.toByteArray()));
			}
		} catch (Exception eBaca) {
			// Gagal membaca isi berkas tidak membatalkan pencetakan -- url tetap sah.
			ais.common.ErrorAuditUtil.record(eBaca,
					"PengadaanPosApiHelper.cetakDokumen baca=" + berkas.getName());
		}
	}

	/**
	 * Parameter cetak Bukti Setor Pajak.
	 *
	 * <p>Berbeda dengan lima dokumen pengadaan lain, tahap ini TIDAK memiliki padanan
	 * di versi ZKoss -- di sana pencetakan pajak berupa ekspor daftar, bukan dokumen
	 * per baris. Karena itu templatnya baru ({@code asset/bukti_setor_pajak.jrxml})
	 * dan pembangun parameternya ada di sini, bukan dipinjam dari aksi ZKoss.</p>
	 */
	// Akses publik: dipakai juga oleh tombol cetak pada layar Pertanggungjawaban Pajak
	// versi ZKoss, supaya bukti setor yang tercetak dari kedua versi benar-benar sama.
	public static java.util.Map<String, Object> parameterCetakPajak(
			ais.database.model.akunting.Pajak p, Tbmuser tbmuser) {
		java.util.Map<String, Object> m = new java.util.HashMap<String, Object>();
		m.put("judul", "BUKTI SETOR PAJAK");
		m.put("nama_instansi", Common.getBahasaConfig("Nama Instansi"));
		m.put("kode", p.getKode() == null ? "-" : p.getKode());
		m.put("nama", p.getNama() == null ? "-" : p.getNama());
		String jenis = p.getJenisPajakBarang() != null && p.getJenisPajakBarang().getNama() != null
				? p.getJenisPajakBarang().getNama()
				: (p.getJenisPajakPpn() != null ? "PPN" : "-");
		m.put("jenis_pajak", jenis);
		m.put("ntpn", p.getNtpn() == null || p.getNtpn().trim().isEmpty() ? "-" : p.getNtpn());
		m.put("npwp", p.getNpwp() == null || p.getNpwp().trim().isEmpty() ? "-" : p.getNpwp());
		m.put("nama_wp", p.getNamaWp() == null || p.getNamaWp().trim().isEmpty() ? "-" : p.getNamaWp());
		m.put("tanggal_setor", p.getTanggalStor() == null ? "-"
				: Common.dateFormat1.get().format(p.getTanggalStor()));
		m.put("dpp", Common.numberFormat.get().format(p.getDpp() == null ? 0 : p.getDpp().doubleValue()));
		m.put("nilai", Common.numberFormat.get().format(p.getNilai() == null ? 0 : p.getNilai().doubleValue()));
		m.put("keterangan", p.getKeterangan() == null ? "" : p.getKeterangan());
		m.put("dicetak_oleh", tbmuser == null || tbmuser.getUserNama() == null ? "" : tbmuser.getUserNama());
		m.put("dicetak_pada", Common.dateFormat1.get().format(ais.ui.util.WaktuUtil.getDate()));
		return m;
	}

	/** Pagar kepemilikan toko yang dipakai bersama seluruh cabang {@link #cetakDokumen}. */
	private static boolean milikToko(JSONObject hasil, Long tokoDokumen, Long tokoId, boolean tidakAda)
			throws Exception {
		if (tidakAda) {
			tolak(hasil, "Dokumen tidak ditemukan.");
			return false;
		}
		if (tokoId != null && tokoDokumen != null && !tokoId.equals(tokoDokumen)) {
			tolak(hasil, "Dokumen ini milik toko lain.");
			return false;
		}
		return true;
	}

	/**
	 * URL berkas laporan, mengikuti cara {@code LaporanApi} menyajikannya: lewat direktori
	 * report langsung, atau lewat servlet /pdf bila pemasangan memakai direktori tergabung.
	 */
	private static String urlBerkasLaporan(String namaFile) throws Exception {
		if (!Common.pakaiDirReportTergabung()) {
			return Common.CURRENT_URL + "/report/"
					+ java.net.URLEncoder.encode(namaFile, "UTF-8");
		}
		return Common.CURRENT_URL + "/pdf?p="
				+ java.net.URLEncoder.encode(Common.desEncrypter.get().encrypt(namaFile), "UTF-8");
	}

	/** Pencarian penyedia/vendor untuk pemilih pada layar PO. */
	public static void cariPenyedia(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehLihat(tbmuser, KUNCI_PO) && !bolehLihat(tbmuser, KUNCI_BAST)
				&& !bolehLihat(tbmuser, KUNCI_TAGIHAN)) {
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
				o.put("terminKey", bast.getKodeTermin() == null ? "" : bast.getKodeTermin());
				o.put("termin", bast.getKeteranganTermin() == null ? "" : bast.getKeteranganTermin());
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
			h.put("terminKey", bast.getKodeTermin() == null ? "" : bast.getKodeTermin());
			h.put("termin", bast.getKeteranganTermin() == null ? "" : bast.getKeteranganTermin());
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
				// Potongan dan pajak WAJIB ikut dikembalikan. Tanpa ini layar sunting membuka
				// dokumen lama dengan PPN/PPh 0 sehingga nilainya terlihat "tidak tersimpan"
				// padahal tersimpan (laporan pemilik produk, 2026-08-21).
				o.put("hargaPotongan", d.getHargaPotongan() == null ? 0 : d.getHargaPotongan());
				o.put("diskonPersen", Boolean.TRUE.equals(d.getDiskonDalamBentukPersen()));
				o.put("persenPpn", d.getPersenPpn() == null ? 0 : d.getPersenPpn());
				o.put("persenPph", d.getPersenPph() == null ? 0 : d.getPersenPph());
				o.put("jenis_pajak_ppn_id", d.getJenisPajakPpn() == null ? JSONObject.NULL
						: d.getJenisPajakPpn().getId());
				o.put("jenis_pajak_barang_id", d.getJenisPajakBarang() == null ? JSONObject.NULL
						: d.getJenisPajakBarang().getId());
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
				if (Boolean.TRUE.equals(po.getTutup())) {
					tolak(hasil, "Sisa Pemesanan Pembelian ini sudah ditutup lewat Back Order, "
							+ "sehingga tidak menerima barang lagi. Terima barangnya pada pesanan susulan.");
					return;
				}
			}
			bast.setPemesananPengadaanMasterAsset(po);
			bast.setTampaPemesanan(Boolean.valueOf(po == null));

			/* ================== SATU PENERIMAAN UNTUK SATU TERMIN ==================
			 *
			 * Pesanan bertermin ditagih PER TERMIN, bukan sekaligus. Karena itu setiap
			 * penerimaan menunjuk satu termin, dan pesanan dengan tiga termin melahirkan
			 * tiga penerimaan -- masing-masing dengan fakturnya sendiri. Aturan ini sama
			 * dengan versi ZKoss ({@code PenerimaanPengadaanMasterAssetAction}), yang
			 * menyediakan combo pilihan termin dan menyimpannya ke tiga kolom yang sama:
			 * kodeTermin (kunci termin), jsonTermin (isi terminnya), keteranganTermin
			 * (namanya, untuk ditampilkan tanpa mengurai JSON lagi).
			 *
			 * Ketiga kolom itu SUDAH ADA sejak lama dan dipakai ZKoss; POS-lah yang
			 * selama ini tidak pernah mengisinya, sehingga penerimaan dari POS tidak
			 * dapat dibedakan termin-nya dan hanya bisa ditagih sekali.
			 *
			 * JANGAN longgarkan pemeriksaan ganda di bawah: dua penerimaan aktif pada
			 * termin yang sama membuat satu termin tertagih dua kali. */
			String kunciTermin = request.optString("termin_key", "").trim();
			if (po != null) {
				JSONArray terminPo = terminDari(po);
				if (terminPo.length() > 0) {
					if (kunciTermin.isEmpty()) {
						tolak(hasil, "Pemesanan " + (po.getKode() == null ? "" : po.getKode())
								+ " dibayar bertermin, jadi termin yang diterima wajib dipilih.");
						return;
					}
					JSONObject terminTerpilih = null;
					for (int i = 0; i < terminPo.length(); i++) {
						JSONObject t = terminPo.optJSONObject(i);
						if (t != null && !t.isNull("key") && kunciTermin.equals((t.get("key") + "").trim())) {
							terminTerpilih = t;
							break;
						}
					}
					if (terminTerpilih == null) {
						tolak(hasil, "Termin yang dipilih tidak ada pada pemesanan ini.");
						return;
					}
					@SuppressWarnings("unchecked")
					List<PenerimaanPengadaanMasterAsset> seterminan = session
							.createCriteria(PenerimaanPengadaanMasterAsset.class)
							.add(Restrictions.eq("pemesananPengadaanMasterAsset.id", po.getId()))
							.add(Restrictions.eq("kodeTermin", kunciTermin)).list();
					for (PenerimaanPengadaanMasterAsset lainnya : seterminan) {
						if (lainnya == null || Boolean.FALSE.equals(lainnya.getAktif())) {
							continue;
						}
						if (bast.getId() != null && bast.getId().equals(lainnya.getId())) {
							continue;
						}
						tolak(hasil, "Termin ini sudah diterima lewat "
								+ (lainnya.getKode() == null ? "penerimaan lain" : lainnya.getKode())
								+ ". Satu termin hanya diterima sekali.");
						return;
					}
					bast.setKodeTermin(kunciTermin);
					bast.setJsonTermin(terminTerpilih.toString());
					bast.setKeteranganTermin(terminTerpilih.isNull("nama") ? ""
							: (terminTerpilih.get("nama") + ""));
				} else {
					bast.setKodeTermin(null);
					bast.setJsonTermin(null);
					bast.setKeteranganTermin(null);
				}
			}
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
			if (Boolean.TRUE.equals(po.getTutup())) {
				tolak(hasil, "Sisa Pemesanan Pembelian ini sudah ditutup lewat Back Order, sehingga tidak "
						+ "menerima barang lagi. Terima barangnya pada pesanan susulan.");
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
			/* Jadwal termin pesanan ini, berikut penanda termin mana yang SUDAH diterima.
			 * Layar memerlukan keduanya: yang sudah diterima ditampilkan tetapi tidak dapat
			 * dipilih lagi, sehingga petugas melihat kemajuannya tanpa bisa menagih ulang. */
			JSONArray terminPo = terminDari(po);
			JSONArray daftarTermin = new JSONArray();
			if (terminPo.length() > 0) {
				@SuppressWarnings("unchecked")
				List<PenerimaanPengadaanMasterAsset> bastPo = session
						.createCriteria(PenerimaanPengadaanMasterAsset.class)
						.add(Restrictions.eq("pemesananPengadaanMasterAsset.id", po.getId())).list();
				java.util.Map<String, String> sudahDiterima = new java.util.HashMap<String, String>();
				for (PenerimaanPengadaanMasterAsset b : bastPo) {
					if (b == null || Boolean.FALSE.equals(b.getAktif()) || b.getKodeTermin() == null) {
						continue;
					}
					sudahDiterima.put(b.getKodeTermin().trim(),
							b.getKode() == null ? "" : b.getKode());
				}
				for (int i = 0; i < terminPo.length(); i++) {
					JSONObject t = terminPo.optJSONObject(i);
					if (t == null || t.isNull("key")) {
						continue;
					}
					String kunci = (t.get("key") + "").trim();
					JSONObject o = new JSONObject();
					o.put("key", kunci);
					o.put("nomor", t.isNull("nomor") ? "" : t.get("nomor") + "");
					o.put("nama", t.isNull("nama") ? "" : t.get("nama") + "");
					o.put("penagihan", angkaAman(t, "penagihan"));
					o.put("jatuhTempo", t.isNull("tanggalD") ? "" : t.get("tanggalD") + "");
					o.put("sudahDiterima", sudahDiterima.containsKey(kunci));
					o.put("diterimaLewat", sudahDiterima.containsKey(kunci)
							? sudahDiterima.get(kunci) : "");
					daftarTermin.put(o);
				}
			}
			hasil.put("termin", daftarTermin);
			hasil.put("bertermin", daftarTermin.length() > 0);
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
				o.put("statusBarang", statusBast(bast));
				o.put("terminKey", bast.getKodeTermin() == null ? "" : bast.getKodeTermin());
				o.put("termin", bast.getKeteranganTermin() == null ? "" : bast.getKeteranganTermin());
				o.put("dibayarPo", po == null || po.getDibayar() == null ? 0 : po.getDibayar());
				arr.put(o);
			}
			lengkapiLampiranTagihan(arr);
			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("total", cocok);
			hasil.put("anggaranWajib", Common.bolehKonfigurasi(
					KONFIG_TAGIHAN_RUTIN_ANGGARAN_WAJIB));
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Melengkapi baris daftar tagihan dengan ringkasan lampiran yang SUDAH diunggah.
	 *
	 * <p><b>Mengapa satu kueri borongan.</b> Lampiran tersimpan di basis data BERBEDA
	 * ({@code StreamingHibernateUtil}), sehingga tidak dapat digabung lewat join dengan kueri
	 * daftar tagihan. Menanyakannya per baris per slot berarti lima kueri lintas basis data
	 * untuk setiap baris layar. Karena itu seluruh id BAST pada halaman ini ditanyakan
	 * SEKALIGUS dalam satu kueri.</p>
	 *
	 * <p><b>Kegagalannya sengaja ditelan.</b> Penyimpanan berkas adalah basis data terpisah
	 * yang bisa saja sedang tidak dapat dihubungi. Bila itu terjadi, daftar tagihan HARUS
	 * tetap tampil -- hanya kolom lampirannya yang kosong. Membiarkan galatnya naik akan
	 * mematikan seluruh layar hanya karena keterangan tambahan tidak terbaca.</p>
	 */
	private static void lengkapiLampiranTagihan(JSONArray arr) {
		if (arr == null || arr.length() == 0 || !berkasSiap()) {
			return;
		}
		Session sesi = null;
		try {
			java.util.List<Long> idBast = new java.util.ArrayList<Long>();
			for (int i = 0; i < arr.length(); i++) {
				Object id = arr.getJSONObject(i).opt("id");
				if (id instanceof Number) {
					idBast.add(Long.valueOf(((Number) id).longValue()));
				}
			}
			if (idBast.isEmpty()) {
				return;
			}
			java.util.List<String> jenis = new java.util.ArrayList<String>();
			java.util.Map<String, String> namaSlot = new java.util.HashMap<String, String>();
			for (int i = 0; i < SLOT_LAMPIRAN_TAGIHAN.length; i++) {
				String penuh = JENIS_LAMPIRAN_TAGIHAN + SLOT_LAMPIRAN_TAGIHAN[i][0];
				jenis.add(penuh);
				namaSlot.put(penuh, SLOT_LAMPIRAN_TAGIHAN[i][1]);
			}
			sesi = sesiBerkas();
			@SuppressWarnings("unchecked")
			java.util.List<ais.database.model.file.LampiranLain> berkas = sesi
					.createCriteria(ais.database.model.file.LampiranLain.class)
					.add(Restrictions.in("ref", idBast))
					.add(Restrictions.in("jenis", jenis)).list();
			// Berkasnya sendiri disimpan juga, dikunci jenis, supaya rinciannya dapat
			// dikirim tanpa kueri kedua. Kuerinya sudah menarik baris ini seluruhnya.
			java.util.Map<Long, java.util.Map<String, ais.database.model.file.LampiranLain>> petaBerkas
					= new java.util.HashMap<Long, java.util.Map<String, ais.database.model.file.LampiranLain>>();
			java.util.Map<Long, java.util.List<String>> peta = new java.util.HashMap<Long, java.util.List<String>>();
			for (ais.database.model.file.LampiranLain b : berkas) {
				if (b == null || b.getRef() == null) {
					continue;
				}
				Long ref = Long.valueOf(b.getRef() + "");
				java.util.Map<String, ais.database.model.file.LampiranLain> perSlot = petaBerkas.get(ref);
				if (perSlot == null) {
					perSlot = new java.util.HashMap<String, ais.database.model.file.LampiranLain>();
					petaBerkas.put(ref, perSlot);
				}
				if (b.getJenis() != null) {
					perSlot.put(b.getJenis(), b);
				}
				java.util.List<String> daftar = peta.get(ref);
				if (daftar == null) {
					daftar = new java.util.ArrayList<String>();
					peta.put(ref, daftar);
				}
				String nama = namaSlot.get(b.getJenis());
				if (nama != null && !daftar.contains(nama)) {
					daftar.add(nama);
				}
			}
			for (int i = 0; i < arr.length(); i++) {
				JSONObject o = arr.getJSONObject(i);
				Object id = o.opt("id");
				java.util.List<String> daftar = (id instanceof Number)
						? peta.get(Long.valueOf(((Number) id).longValue())) : null;
				if (daftar == null) {
					daftar = new java.util.ArrayList<String>();
				}
				java.util.Map<String, ais.database.model.file.LampiranLain> berkasBaris = (id instanceof Number)
						? petaBerkas.get(Long.valueOf(((Number) id).longValue())) : null;
				JSONArray rinci = new JSONArray();
				if (berkasBaris != null) {
					for (int s = 0; s < SLOT_LAMPIRAN_TAGIHAN.length; s++) {
						ais.database.model.file.LampiranLain lb = berkasBaris
								.get(JENIS_LAMPIRAN_TAGIHAN + SLOT_LAMPIRAN_TAGIHAN[s][0]);
						if (lb == null) {
							continue;
						}
						JSONObject l = new JSONObject();
						l.put("lampiran_id", lb.getId());
						l.put("kunci", SLOT_LAMPIRAN_TAGIHAN[s][0]);
						l.put("nama", SLOT_LAMPIRAN_TAGIHAN[s][1]);
						l.put("namaFile", lb.getNama() == null ? "" : lb.getNama());
						l.put("tipe", lb.getDeskripsi() == null ? "" : lb.getDeskripsi());
						rinci.put(l);
					}
				}
				// Dipakai layar daftar untuk mengunduh berkas tanpa membuka dialog ubah.
				o.put("lampiranRinci", rinci);
				o.put("lampiran", new JSONArray(daftar));
				o.put("lampiranTerisi", daftar.size());
				o.put("lampiranTotal", SLOT_LAMPIRAN_TAGIHAN.length);
				// Invoice satu-satunya slot wajib; layar memakainya untuk menandai yang belum lengkap.
				o.put("lampiranWajibLengkap", daftar.contains(SLOT_LAMPIRAN_TAGIHAN[0][1]));
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "PengadaanPosApiHelper.lengkapiLampiranTagihan");
		} finally {
			HibernateUtil.closeSessionQuietly(sesi);
		}
	}

	/**
	 * <h3>Daftar transitori pembayaran pengadaan</h3>
	 *
	 * <p>Transitori adalah pembayaran yang uangnya TIDAK langsung ditransfer ke penyedia,
	 * melainkan ditampung dahulu pada akun perantara. Baris yang ditandai transitori
	 * memperoleh satu record {@code Transitori}; record itulah yang dikumpulkan di sini
	 * untuk kemudian direalisasikan.</p>
	 *
	 * <p>Penyaring status: MENUNGGU (belum masuk batch), DIREALISASIKAN (batchnya sudah
	 * disetujui), atau kosong untuk semuanya. Bawaannya menunggu, karena itulah yang perlu
	 * ditindaklanjuti.</p>
	 */
	public static void transitoriDaftar(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehLihat(tbmuser, KUNCI_DPC)) {
			tolak(hasil, "Menu Pengadaan tidak diaktifkan untuk grup pengguna Anda.");
			return;
		}
		Long tokoId = tokoLingkup(tbmuser, request);
		String status = request == null ? "" : request.optString("status", "MENUNGGU").trim().toUpperCase();
		String cari = request == null ? "" : request.optString("cari", "").trim().toLowerCase();
		int page = Math.max(1, request == null ? 1 : request.optInt("page", 1));
		int pageSize = Math.min(100, Math.max(5, request == null ? 20 : request.optInt("pageSize", 20)));
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			@SuppressWarnings("unchecked")
			List<ais.database.model.akunting.Transitori> semua = session
					.createCriteria(ais.database.model.akunting.Transitori.class)
					.addOrder(Order.desc("id")).list();
			JSONArray arr = new JSONArray();
			int cocok = 0;
			int mulai = (page - 1) * pageSize;
			double totalMenunggu = 0;
			for (ais.database.model.akunting.Transitori tr : semua) {
				if (tr == null || Boolean.FALSE.equals(tr.getAktif())) {
					continue;
				}
				ais.database.model.akunting.DaftarPengajuanTransfer dpt = tr.getDaftarPengajuanTransfer();
				if (dpt == null) {
					continue;
				}
				PembayaranTerminMasterAssetDetail detail = dpt.getPembayaranTerminMasterAssetDetail();
				PembayaranTerminMasterAsset bayar = detail == null ? null
						: detail.getPembayaranTerminMasterAsset();
				/* Hanya transitori milik pembayaran pengadaan POS yang ditampilkan. Transitori
				 * dari modul lain (uang muka, kas kecil, reimbursement) memang berbagi tabel
				 * yang sama, tetapi bukan urusan layar ini dan alur persetujuannya berbeda. */
				if (bayar == null) {
					continue;
				}
				if (tokoId != null && bayar.getToko() != null && !tokoId.equals(bayar.getToko().getId())) {
					continue;
				}
				ais.database.model.akunting.ProsesTransitori batch = tr.getProsesTransitori();
				boolean direalisasikan = batch != null && batch.getDisetujuiOleh() != null;
				String st = direalisasikan ? "DIREALISASIKAN" : "MENUNGGU";
				if (status.length() > 0 && !status.equals(st)) {
					continue;
				}
				String judul = (bayar.getKode() == null ? "" : bayar.getKode()) + " "
						+ (dpt.getNama() == null ? "" : dpt.getNama());
				if (cari.length() > 0 && judul.toLowerCase().indexOf(cari) < 0) {
					continue;
				}
				double nominal = dpt.getNominal() == null ? 0 : dpt.getNominal().doubleValue();
				if (!direalisasikan) {
					totalMenunggu += nominal;
				}
				cocok++;
				if (cocok <= mulai || arr.length() >= pageSize) {
					continue;
				}
				JSONObject o = new JSONObject();
				o.put("id", tr.getId());
				o.put("nama", dpt.getNama() == null ? "" : dpt.getNama());
				o.put("nominal", nominal);
				o.put("bayar_id", bayar.getId());
				o.put("bayarKode", bayar.getKode() == null ? "" : bayar.getKode());
				o.put("penyedia", bayar.getPenyedia() == null ? "" : bayar.getPenyedia().getNama());
				o.put("caraBayar", bayar.getCaraPembayaranTransfer() == null ? ""
						: bayar.getCaraPembayaranTransfer().getNama());
				o.put("tanggalBayar", bayar.getTanggalPembuatan() == null ? ""
						: Common.dateFormat3.get().format(bayar.getTanggalPembuatan()));
				o.put("po", detail.getPemesananPengadaanMasterAsset() == null
						|| detail.getPemesananPengadaanMasterAsset().getKode() == null ? ""
								: detail.getPemesananPengadaanMasterAsset().getKode());
				o.put("statusBayar", statusBayar(bayar));
				o.put("status", st);
				o.put("batch", batch == null || batch.getNama() == null ? "" : batch.getNama());
				o.put("tanggalRealisasi", batch == null || batch.getTanggalPersetujuan() == null ? ""
						: Common.dateFormat3.get().format(batch.getTanggalPersetujuan()));
				o.put("direalisasikanOleh", batch == null || batch.getDisetujuiOleh() == null ? ""
						: batch.getDisetujuiOleh().getUserNama());
				o.put("sudahDiposting", tr.getPostingHistory() != null);
				arr.put(o);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("total", cocok);
			hasil.put("nilaiMenunggu", totalMenunggu);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * <h3>Realisasi transitori</h3>
	 *
	 * <p>Mengikuti {@code ProsesTransitoriAction} versi ZKoss: beberapa transitori
	 * dikumpulkan ke dalam satu {@code ProsesTransitori}, nilainya dijumlahkan, lalu batch
	 * itu disetujui (disetujuiOleh + tanggalPersetujuan). Itulah yang dimaksud realisasi.</p>
	 *
	 * <p><b>Realisasi BUKAN posting jurnal.</b> Di ZKoss keduanya memang dua langkah
	 * terpisah: jurnalnya diterbitkan belakangan lewat layar Posting Proses Transitori,
	 * yang membaca batch yang sudah disetujui, dan pembatalan posting pun dikerjakan di
	 * sana. Metode ini SENGAJA tidak menerbitkan jurnal sendiri. Menerbitkannya di sini
	 * akan melahirkan jurnal kedua yang lambat laun berbeda dari versi ZKoss tanpa ada
	 * yang menyadarinya, sekaligus melewati layar pembatalan posting yang sudah ada.
	 * JANGAN tambahkan posting jurnal di sini.</p>
	 *
	 * @param request {@code ids} (wajib, array id transitori), {@code nama}, {@code keterangan}.
	 */
	public static void transitoriRealisasi(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehAksi(tbmuser, KUNCI_DPC, "approve")) {
			tolak(hasil, "Grup pengguna Anda tidak memiliki hak merealisasikan transitori.");
			return;
		}
		if (tbmuser == null) {
			tolak(hasil, "Sesi pengguna tidak dikenali, silakan masuk ulang.");
			return;
		}
		JSONArray ids = (request == null || request.isNull("ids")) ? null : request.getJSONArray("ids");
		if (ids == null || ids.length() == 0) {
			tolak(hasil, "Pilih minimal satu transitori yang akan direalisasikan.");
			return;
		}
		Long tokoId = tokoLingkup(tbmuser, request);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			session.beginTransaction();
			ais.database.model.akunting.ProsesTransitori batch = new ais.database.model.akunting.ProsesTransitori();
			String namaBatch = request.optString("nama", "").trim();
			batch.setNama(namaBatch.isEmpty()
					? ("Realisasi transitori " + Common.dateFormat3.get().format(ais.ui.util.WaktuUtil.getDate()))
					: namaBatch);
			batch.setKeterangan(request.optString("keterangan", "").trim());
			batch.setTanggalPembuatan(ais.ui.util.WaktuUtil.getDate());
			batch.setAktif(Boolean.TRUE);
			batch.setDisetujuiOleh(tbmuser);
			batch.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());
			batch.setOleh(tbmuser.getUserNama());
			batch.setOlehId(tbmuser.getUserId());
			session.save(batch);
			session.flush();

			double total = 0;
			int jumlah = 0;
			for (int i = 0; i < ids.length(); i++) {
				Long id = Long.valueOf((ids.get(i) + "").trim());
				ais.database.model.akunting.Transitori tr = (ais.database.model.akunting.Transitori) session
						.get(ais.database.model.akunting.Transitori.class, id);
				if (tr == null) {
					continue;
				}
				if (tr.getProsesTransitori() != null) {
					tolak(hasil, "Transitori " + (tr.getNama() == null ? (id + "") : tr.getNama())
							+ " sudah pernah direalisasikan.");
					session.getTransaction().rollback();
					return;
				}
				ais.database.model.akunting.DaftarPengajuanTransfer dpt = tr.getDaftarPengajuanTransfer();
				PembayaranTerminMasterAssetDetail detail = dpt == null ? null
						: dpt.getPembayaranTerminMasterAssetDetail();
				PembayaranTerminMasterAsset bayar = detail == null ? null
						: detail.getPembayaranTerminMasterAsset();
				if (bayar == null) {
					tolak(hasil, "Transitori ini bukan milik pembayaran pengadaan; "
							+ "realisasinya dikerjakan pada modulnya sendiri.");
					session.getTransaction().rollback();
					return;
				}
				if (tokoId != null && bayar.getToko() != null && !tokoId.equals(bayar.getToko().getId())) {
					tolak(hasil, "Transitori " + (tr.getNama() == null ? "" : tr.getNama()) + " milik toko lain.");
					session.getTransaction().rollback();
					return;
				}
				/* Pembayarannya harus sudah disetujui. Merealisasikan uang atas dokumen yang
				 * masih draf berarti mencairkan sesuatu yang belum sah, dan dokumen draf
				 * masih dapat disunting sehingga barisnya bisa berubah setelah dicairkan. */
				if (bayar.getTanggalPersetujuan() == null) {
					tolak(hasil, "Pembayaran " + (bayar.getKode() == null ? "" : bayar.getKode())
							+ " belum disetujui, jadi transitorinya belum dapat direalisasikan.");
					session.getTransaction().rollback();
					return;
				}
				tr.setProsesTransitori(batch);
				tr.setOleh(tbmuser.getUserNama());
				tr.setOlehId(tbmuser.getUserId());
				session.saveOrUpdate(tr);
				total += dpt.getNominal() == null ? 0 : dpt.getNominal().doubleValue();
				jumlah++;
			}
			if (jumlah == 0) {
				tolak(hasil, "Tidak ada transitori yang dapat direalisasikan dari pilihan itu.");
				session.getTransaction().rollback();
				return;
			}
			batch.setNilai(Double.valueOf(total));
			session.saveOrUpdate(batch);
			session.getTransaction().commit();

			hasil.put("status", "00");
			hasil.put("id", batch.getId());
			hasil.put("nama", batch.getNama());
			hasil.put("jumlah", jumlah);
			hasil.put("nilai", total);
			hasil.put("description", jumlah + " transitori direalisasikan senilai "
					+ Common.numberFormat.get().format(total)
					+ ". Jurnalnya diterbitkan lewat Posting Proses Transitori.");
		} catch (Exception e) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception eRollback) {
				ais.common.ErrorAuditUtil.record(eRollback, "PengadaanPosApiHelper.transitoriRealisasi rollback");
			}
			throw e;
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
			// Lampiran wajib (Invoice) harus sudah terunggah. Pagar ini ada di SERVER supaya
			// berlaku sama di Desktop, Android, dan JSP -- bukan sekadar penjaga di layar.
			java.util.List<String> lampiranKurang = lampiranWajibKurang(bast.getId());
			if (!lampiranKurang.isEmpty()) {
				StringBuilder daftarKurang = new StringBuilder();
				for (String nama : lampiranKurang) {
					daftarKurang.append(daftarKurang.length() == 0 ? "" : ", ").append(nama);
				}
				tolak(hasil, "Lampiran wajib belum diunggah: " + daftarKurang
						+ ". Unggah dahulu sebelum tagihan diterima.");
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
	/**
	 * Nilai sebuah PO yang sudah TERPAKAI oleh dokumen pembayaran, dijumlahkan lintas termin.
	 *
	 * <p>Parameter {@code hanyaDisetujui} memisahkan dua pertanyaan yang sengaja TIDAK boleh
	 * dicampur:</p>
	 * <ul>
	 * <li>{@code true} -- "berapa yang benar-benar sudah DIBAYAR?" Sama persis dengan definisi
	 * {@code PemesananPengadaanMasterAsset.hitungDibayar()}: hanya dokumen yang sudah disetujui
	 * yang diakui. Dipakai untuk status lunas, sisa utang, dan pelaporan.</li>
	 * <li>{@code false} -- "berapa yang sudah DIAJUKAN?" Draf yang belum disetujui pun ikut
	 * dihitung. Dipakai saat menyusun daftar tagihan yang boleh diajukan, supaya satu tagihan
	 * tidak bisa diajukan dua kali (permintaan pemilik produk, 2026-08-21).</li>
	 * </ul>
	 */
	private static double terpakaiPembayaranPo(Session session, Long poId, Long kecualiBayarId,
			boolean hanyaDisetujui) {
		return terpakaiPembayaran(session, poId, null, false, kecualiBayarId, hanyaDisetujui);
	}

	/** Sama dengan {@link #terpakaiPembayaranPo} tetapi dibatasi pada satu termin. */
	private static double terpakaiPembayaranTermin(Session session, Long poId, String kunciTermin,
			Long kecualiBayarId, boolean hanyaDisetujui) {
		return terpakaiPembayaran(session, poId, kunciTermin, true, kecualiBayarId, hanyaDisetujui);
	}

	private static double terpakaiPembayaran(Session session, Long poId, String kunciTermin,
			boolean saringTermin, Long kecualiBayarId, boolean hanyaDisetujui) {
		@SuppressWarnings("unchecked")
		List<PembayaranTerminMasterAssetDetail> daftar = session
				.createCriteria(PembayaranTerminMasterAssetDetail.class)
				.add(Restrictions.eq("pemesananPengadaanMasterAsset.id", poId)).list();
		double jml = 0;
		for (PembayaranTerminMasterAssetDetail b : daftar) {
			PembayaranTerminMasterAsset induk = b.getPembayaranTerminMasterAsset();
			if (induk == null || Boolean.FALSE.equals(induk.getAktif())) {
				continue;
			}
			if (hanyaDisetujui && induk.getDisetujuiOleh() == null) {
				continue;
			}
			if (kecualiBayarId != null && kecualiBayarId.equals(induk.getId())) {
				continue;
			}
			if (saringTermin) {
				String kunci = "";
				if (b.getTagihan() != null && !b.getTagihan().trim().isEmpty()) {
					try {
						JSONObject t = new JSONObject(b.getTagihan());
						kunci = t.isNull("key") ? "" : (t.get("key") + "").trim();
					} catch (Exception e) {
						kunci = "";
					}
				}
				if (kunciTermin == null ? !kunci.isEmpty() : !kunciTermin.equals(kunci)) {
					continue;
				}
			}
			jml += b.getDibayar() == null ? 0 : b.getDibayar().doubleValue();
		}
		return jml;
	}

	/**
	 * Nilai satu termin yang sudah benar-benar DIBAYAR (dokumen pembayaran sudah disetujui).
	 * Delegasi ke {@link #terpakaiPembayaranTermin} supaya definisi "dibayar" hanya ada di
	 * satu tempat.
	 */
	private static double terbayarTermin(Session session, Long poId, String kunciTermin, Long kecualiBayarId) {
		return terpakaiPembayaranTermin(session, poId, kunciTermin, kecualiBayarId, true);
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
				o.put("caraBayar", b.getCaraPembayaranTransfer() == null
						|| b.getCaraPembayaranTransfer().getNama() == null ? ""
								: b.getCaraPembayaranTransfer().getNama());
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
				// Dua angka berbeda, sengaja dipisah:
				//   dibayarPo  = sudah disetujui -> benar-benar dibayar (dipakai utk info).
				//   diajukanPo = termasuk draf   -> dipakai utk menentukan apa yang MASIH boleh
				//                diajukan, supaya tagihan yang sudah diajukan tidak muncul lagi.
				double nilaiPo = po.getNilai() == null ? 0 : po.getNilai().doubleValue();
				double dibayarPo = terpakaiPembayaranPo(session, po.getId(), kecuali, true);
				double diajukanPo = terpakaiPembayaranPo(session, po.getId(), kecuali, false);
				double sisaPo = nilaiPo - diajukanPo;
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
						double sudah = terpakaiPembayaranTermin(session, po.getId(), kunci, kecuali, true);
						double diajukan = terpakaiPembayaranTermin(session, po.getId(), kunci, kecuali, false);
						double sisa = tagih - diajukan;
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
						o.put("sedangDiajukan", Math.max(0, diajukan - sudah));
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
					o.put("sudahDibayar", dibayarPo);
					o.put("sedangDiajukan", Math.max(0, diajukanPo - dibayarPo));
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
			h.put("judul", bayar.getJudul() == null ? "" : bayar.getJudul());
			h.put("cara_bayar_id", bayar.getCaraPembayaranTransfer() == null ? JSONObject.NULL
					: bayar.getCaraPembayaranTransfer().getId());
			h.put("caraBayar", bayar.getCaraPembayaranTransfer() == null
					|| bayar.getCaraPembayaranTransfer().getNama() == null ? ""
							: bayar.getCaraPembayaranTransfer().getNama());
			h.put("tanggalRealisasi", bayar.getTanggalRealisasi() == null ? ""
					: Common.dateFormat1.get().format(bayar.getTanggalRealisasi()));
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
				/* Pilihan transfer/transitori disimpan pada DaftarPengajuanTransfer milik
				 * baris ini, sama seperti versi ZKoss. Bawaannya transfer, sehingga
				 * dokumen lama yang belum pernah punya pilihan tetap terbaca wajar. */
				o.put("transitori", d.getDaftarPengajuanTransfer() != null
						&& Boolean.TRUE.equals(d.getDaftarPengajuanTransfer().getTransitori()));
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
			bayar.setJudul(request.optString("judul", "").trim());
			// Cara transfer -- mengikuti form Proses Transfer versi ZKoss. Pada tahap DRAF
			// nilainya boleh kosong; yang menuntutnya adalah persetujuan (lihat bayarPutusan),
			// sebab di sanalah jurnal dibentuk dari akun yang melekat pada cara transfer.
			ais.database.model.akunting.CaraPembayaranTransfer caraBayar = null;
			if (!request.isNull("cara_bayar_id") && !(request.get("cara_bayar_id") + "").trim().isEmpty()) {
				caraBayar = (ais.database.model.akunting.CaraPembayaranTransfer) session.get(
						ais.database.model.akunting.CaraPembayaranTransfer.class,
						Long.valueOf((request.get("cara_bayar_id") + "").trim()));
				if (caraBayar == null) {
					tolak(hasil, "Cara transfer yang dipilih tidak ditemukan.");
					return;
				}
				if (caraBayar.getAkun() == null) {
					tolak(hasil, "Cara transfer " + (caraBayar.getNama() == null ? "" : caraBayar.getNama())
							+ " belum memiliki akun, sehingga pembayarannya tidak dapat dijurnal. "
							+ "Lengkapi dahulu akunnya pada master Cara Pembayaran Transfer.");
					return;
				}
				bayar.setCaraPembayaranTransfer(caraBayar);
			}
			java.util.Date tglRealisasi = tanggalKetat(request.optString("tanggalRealisasi", ""));
			if (tglRealisasi != null) {
				bayar.setTanggalRealisasi(tglRealisasi);
			}
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
				// Batas pengajuan memakai angka TERMASUK draf yang belum disetujui: sekali sebuah
				// tagihan diajukan, sisanya tidak boleh diajukan lagi lewat dokumen lain.
				double lain = terpakaiPembayaranTermin(session, po.getId(), kunci, bayar.getId(), false);
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

			/* Menyunting dokumen berarti membuang seluruh barisnya lalu membuatnya ulang.
			 * Itu tidak boleh dilakukan bila salah satu barisnya sudah masuk batch
			 * realisasi transitori: record transitorinya sudah punya arti akuntansi, dan
			 * membuangnya akan meninggalkan batch yang menunjuk baris yang tidak ada.
			 * Ditolak di muka dengan pesan yang jelas, BUKAN dilewati diam-diam --
			 * melewatinya akan menyisakan baris pengajuan yang menunjuk detail terhapus. */
			for (PembayaranTerminMasterAssetDetail dCek : lama) {
				ais.database.model.akunting.DaftarPengajuanTransfer cek = dCek.getDaftarPengajuanTransfer();
				if (cek != null && cek.getTransitoriData() != null
						&& cek.getTransitoriData().getProsesTransitori() != null) {
					tolak(hasil, "Dokumen ini tidak dapat diubah karena salah satu barisnya "
							+ "sudah masuk batch realisasi transitori. Batalkan dahulu "
							+ "realisasinya, lalu ulangi.");
					return;
				}
			}
			java.util.Set<Long> poTersentuh = new java.util.HashSet<Long>();
			for (PembayaranTerminMasterAssetDetail d : lama) {
				if (d.getPemesananPengadaanMasterAsset() != null) {
					poTersentuh.add(d.getPemesananPengadaanMasterAsset().getId());
				}
				/* Baris pengajuan transfer milik detail ini ikut dibuang, kalau tidak ia
				 * menggantung menunjuk detail yang sudah tidak ada. Aman dilakukan karena
				 * dokumen yang sudah disetujui ditolak lebih awal, sehingga yang sampai ke
				 * sini pasti masih DRAF. Penjagaan tambahan tetap dipasang: pengajuan yang
				 * sudah masuk proses transfer, sudah menjadi transitori, atau sudah
				 * diposting TIDAK disentuh. */
				ais.database.model.akunting.DaftarPengajuanTransfer pengajuanLama = d.getDaftarPengajuanTransfer();
				session.delete(d);
				if (pengajuanLama != null && pengajuanLama.getProsesTransfer() == null
						&& pengajuanLama.getPostingHistory() == null) {
					/* Record transitori miliknya ikut dibuang, KECUALI bila sudah masuk
					 * batch realisasi -- yang begitu sudah punya arti akuntansi dan tidak
					 * boleh lenyap hanya karena dokumennya disunting ulang. */
					ais.database.model.akunting.Transitori trLama = pengajuanLama.getTransitoriData();
					if (trLama != null) {
						pengajuanLama.setTransitoriData(null);
						session.saveOrUpdate(pengajuanLama);
						session.flush();
						session.delete(trLama);
					}
					session.delete(pengajuanLama);
				}
			}
			session.flush();

			double total = 0;
			java.util.List<Long> detailBaru = new java.util.ArrayList<Long>();
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
				session.flush();

				/* Setiap baris yang akan ditransfer memperoleh DaftarPengajuanTransfer
				 * sendiri, persis seperti versi ZKoss -- di situlah pilihan
				 * transfer/transitori hidup, dan dari situ pula PostingPembayaranAction
				 * menentukan akun kreditnya: CaraPembayaranTransfer.akun untuk transfer,
				 * CaraPembayaranTransfer.akunTransitori untuk transitori.
				 *
				 * Dibuat langsung di sini, BUKAN lewat
				 * DaftarPengajuanTransfer.simpanPembayaranTerminMasterAssetDetail, karena
				 * pabrik itu membuka dan menutup transaksinya sendiri sedangkan seluruh
				 * penyimpanan ini berjalan dalam satu transaksi. */
				boolean transitoriBaris = b.optBoolean("transitori", false);
				ais.database.model.akunting.DaftarPengajuanTransfer pengajuan = new ais.database.model.akunting.DaftarPengajuanTransfer();
				pengajuan.setPembayaranTerminMasterAssetDetail(d);
				pengajuan.setNama("Pembayaran pengadaan "
						+ (po.getKode() == null ? "" : po.getKode())
						+ (kunci.isEmpty() ? "" : " - " + namaTermin(po, kunci)));
				pengajuan.setKeterangan(d.getKeterangan());
				pengajuan.setNominal(Double.valueOf(nilaiBayar));
				pengajuan.setTransitori(Boolean.valueOf(transitoriBaris));
				pengajuan.setTransfer(Boolean.valueOf(!transitoriBaris));
				pengajuan.setAktif(Boolean.TRUE);
				pengajuan.setOleh(tbmuser.getUserNama());
				pengajuan.setOlehId(tbmuser.getUserId());
				session.save(pengajuan);
				d.setDaftarPengajuanTransfer(pengajuan);
				session.saveOrUpdate(d);
				session.flush();

				/* Menandai transitori TIDAK cukup dengan menyetel flag-nya. Versi ZKoss
				 * (ProsesTransferAction) juga menerbitkan satu record Transitori
				 * berpasangan, ditaut dua arah, dan menghapusnya kembali ketika tandanya
				 * dilepas. Record itulah yang muncul pada daftar realisasi; tanpa dia,
				 * baris yang ditandai transitori tidak akan pernah dapat direalisasikan
				 * dan uangnya menggantung di akun transitori selamanya. */
				if (transitoriBaris) {
					ais.database.model.akunting.Transitori tr = new ais.database.model.akunting.Transitori();
					tr.setDaftarPengajuanTransfer(pengajuan);
					tr.setNama(pengajuan.getNama());
					tr.setKode(pengajuan.getKode());
					tr.setAktif(Boolean.TRUE);
					tr.setOleh(tbmuser.getUserNama());
					tr.setOlehId(tbmuser.getUserId());
					session.save(tr);
					session.flush();
					pengajuan.setTransitoriData(tr);
					session.saveOrUpdate(pengajuan);
					session.flush();
				}

				detailBaru.add(d.getId());
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

			// Terbitkan baris Pajak (PPh termin) memakai pabrik milik versi ZKoss sendiri,
			// BUKAN salinan logika di sini. Dengan begitu pajak dari pembayaran POS muncul di
			// layar Pertanggungjawaban Pajak ZKoss dan ikut terdorong ke DPC lewat
			// DaftarPengajuanTransfer.simpanPajak, persis seperti pembayaran yang dibuat di
			// ZKoss. buatDariTermin bersifat idempoten dan menghapus sendiri barisnya bila
			// PPh-nya nol, jadi aman dipanggil pada setiap penyimpanan.
			//
			// Dipanggil SETELAH commit dan dibungkus try/catch: kegagalan di sisi pajak tidak
			// boleh membatalkan pembayaran yang sudah sah tersimpan.
			for (Long idDetail : detailBaru) {
				try {
					PembayaranTerminMasterAssetDetail acuan = new PembayaranTerminMasterAssetDetail();
					acuan.setId(idDetail);
					ais.database.model.akunting.Pajak.buatDariTermin(acuan);
				} catch (Exception ePajak) {
					ais.common.ErrorAuditUtil.record(ePajak,
							"PengadaanPosApiHelper.bayarSimpan buatDariTermin detail=" + idDetail);
				}
			}

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
			// Cara transfer TIDAK memblokir persetujuan.
			//
			// Semula syarat ini ditegakkan keras -- mula-mula saat menyimpan draf, lalu
			// dipindah ke persetujuan. Keduanya mengunci alur pembayaran yang sudah
			// berjalan: dokumen lama tidak punya cara transfer, dan setiap persetujuan
			// langsung ditolak. Yang dibutuhkan pemilik produk adalah cara transfer
			// TERCATAT untuk pembentukan jurnal, bukan pintu yang menutup pekerjaan
			// hari ini. Karena itu di sini hanya diterbitkan PERINGATAN yang dibawa
			// balasan, dan layar menampilkannya; penegakan keras layak ditambahkan
			// nanti di titik jurnal benar-benar dibentuk, ketika akun yang kosong
			// memang membuat pekerjaan mustahil diselesaikan.
			String peringatanCaraTransfer = null;
			if ("SETUJUI".equals(keputusan) && bayar.getCaraPembayaranTransfer() == null) {
				peringatanCaraTransfer = "Pembayaran disetujui tanpa cara transfer. "
						+ "Lengkapi cara transfernya agar pembayaran ini dapat dijurnal.";
			}
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
			if (peringatanCaraTransfer != null) {
				hasil.put("peringatan", peringatanCaraTransfer);
			}
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
	 * EXISTS: penerimaan ini punya minimal satu baris yang kelompok asetnya ditandai
	 * "Pekerjaan Dalam Pelaksanaan" (CIP). Disalin apa adanya dari versi ZKoss
	 * ({@code BarangDalamProsesDashboard.SQL_EXISTS_KELOMPOK_CIP}) supaya kedua versi
	 * menyaring baris yang sama persis.
	 */
	private static final String SQL_ADA_KELOMPOK_CIP =
			"exists (select 1 from asset.penerimaan_pengadaan_master_asset_detail d "
			+ "join asset.master_asset m on d.masterasset = m.id "
			+ "join asset.kelompok_asset k on m.kelompok_asset = k.id "
			+ "where d.penerimaan_pengadaan_master_asset = this_.id "
			+ "and coalesce(k.merupakanpekerjaandalampelaksanaan, false) = true)";

	/**
	 * Barang Dalam Proses (CIP -- <i>Construction in Progress</i>): rekap seluruh PENERIMAAN
	 * (BAST) beserta status persetujuan dan nilainya.
	 *
	 * <p><b>Riwayat perbaikan (2026-08-21).</b> Versi pertama modul ini keliru mengartikan
	 * "Barang Dalam Proses" sebagai barang yang sudah dipesan tetapi belum diterima. Akibatnya
	 * barang yang sudah dibeli justru HILANG dari layar tepat setelah BAST-nya dibuat. Acuan yang
	 * benar adalah {@code BarangDalamProsesDashboard} pada versi ZKoss: sumbernya BAST, bukan PO.
	 * Pandangan lama tetap berguna untuk memantau pengiriman yang tertunda, karena itu tidak
	 * dibuang melainkan dipindahkan ke {@link #bdpBelumDatang} dan dapat dipanggil dengan
	 * {@code mode=belum_datang}.</p>
	 *
	 * <p>Penyaring CIP versi ZKoss ({@link #SQL_ADA_KELOMPOK_CIP}) disediakan lewat parameter
	 * {@code hanyaCip} dan <b>mati secara bawaan</b>. Alasannya: pada pemasangan POS/kantin
	 * umumnya belum ada kelompok aset yang ditandai CIP, sehingga bila penyaring itu dipaksakan
	 * diam-diam layarnya akan selalu kosong -- persis keluhan yang memunculkan perbaikan ini.
	 * Penyaringnya ditampilkan sebagai pilihan yang terlihat, bukan aturan tersembunyi.</p>
	 *
	 * @param request {@code mode} ("bast" bawaan, atau "belum_datang"), {@code cari},
	 *                {@code tanggalMulai}/{@code tanggalSampai} (dd-MM-yyyy), {@code hanyaCip},
	 *                {@code hanyaBelumDisetujui}, {@code page}, {@code pageSize}.
	 */
	public static void bdpDaftar(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehLihat(tbmuser, KUNCI_BDP)) {
			tolak(hasil, "Menu Pengadaan tidak diaktifkan untuk grup pengguna Anda.");
			return;
		}
		String mode = request == null ? "" : request.optString("mode", "").trim().toLowerCase();
		if ("belum_datang".equals(mode)) {
			bdpBelumDatang(tbmuser, request, hasil);
			return;
		}
		Long tokoId = tokoLingkup(tbmuser, request);
		int page = Math.max(1, request == null ? 1 : request.optInt("page", 1));
		int pageSize = Math.min(200, Math.max(5, request == null ? 25 : request.optInt("pageSize", 25)));
		String cari = request == null ? "" : request.optString("cari", "").trim().toLowerCase();
		boolean hanyaCip = request != null && request.optBoolean("hanyaCip", false);
		boolean hanyaBelumDisetujui = request != null && request.optBoolean("hanyaBelumDisetujui", false);
		java.util.Date dariTgl = tanggalKetat(request == null ? null : request.optString("tanggalMulai", ""));
		java.util.Date sampaiTgl = tanggalKetat(request == null ? null : request.optString("tanggalSampai", ""));
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Criteria kriteria = session.createCriteria(PenerimaanPengadaanMasterAsset.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)));
			if (tokoId != null) {
				kriteria.add(Restrictions.eq("toko.id", tokoId));
			}
			if (hanyaCip) {
				kriteria.add(Restrictions.sqlRestriction(SQL_ADA_KELOMPOK_CIP));
			}
			if (hanyaBelumDisetujui) {
				kriteria.add(Restrictions.isNull("disetujuiOleh"));
			}
			if (dariTgl != null) {
				kriteria.add(Restrictions.ge("tanggalPembuatan", dariTgl));
			}
			if (sampaiTgl != null) {
				// Batas atas dibuat inklusif: tanggal yang diketik pengguna ikut terhitung.
				java.util.Calendar kal = java.util.Calendar.getInstance();
				kal.setTime(sampaiTgl);
				kal.add(java.util.Calendar.DATE, 1);
				kriteria.add(Restrictions.lt("tanggalPembuatan", kal.getTime()));
			}
			kriteria.addOrder(Order.desc("id"));
			@SuppressWarnings("unchecked")
			List<PenerimaanPengadaanMasterAsset> daftar = kriteria.setMaxResults(3000).list();

			JSONArray arr = new JSONArray();
			int cocok = 0;
			int mulai = (page - 1) * pageSize;
			double totalNilai = 0;
			int jumlahDisetujui = 0;
			double nilaiDisetujui = 0;
			for (PenerimaanPengadaanMasterAsset bast : daftar) {
				String kode = bast.getKode() == null ? "" : bast.getKode();
				String vendor = bast.getPenyedia() == null || bast.getPenyedia().getNama() == null ? ""
						: bast.getPenyedia().getNama();
				String uraian = bast.getKeterangan() == null ? "" : bast.getKeterangan();
				String kodePo = bast.getPemesananPengadaanMasterAsset() == null
						|| bast.getPemesananPengadaanMasterAsset().getKode() == null ? ""
								: bast.getPemesananPengadaanMasterAsset().getKode();
				if (cari.length() > 0 && kode.toLowerCase().indexOf(cari) < 0
						&& vendor.toLowerCase().indexOf(cari) < 0
						&& uraian.toLowerCase().indexOf(cari) < 0
						&& kodePo.toLowerCase().indexOf(cari) < 0) {
					continue;
				}
				double nilai = bast.getNilai() == null ? 0 : bast.getNilai().doubleValue();
				boolean disetujui = bast.getDisetujuiOleh() != null;
				cocok++;
				totalNilai += nilai;
				if (disetujui) {
					jumlahDisetujui++;
					nilaiDisetujui += nilai;
				}
				if (cocok <= mulai || arr.length() >= pageSize) {
					continue;
				}
				java.util.Date tanggal = bast.getTanggalPersetujuan() != null ? bast.getTanggalPersetujuan()
						: bast.getTanggalPembuatan();
				JSONObject o = new JSONObject();
				o.put("bast_id", bast.getId());
				o.put("kode", kode);
				o.put("vendor", vendor);
				o.put("penyedia", vendor);
				o.put("lokasi", bast.getLokasi() == null || bast.getLokasi().getNama() == null ? ""
						: bast.getLokasi().getNama());
				o.put("po_id", bast.getPemesananPengadaanMasterAsset() == null ? JSONObject.NULL
						: bast.getPemesananPengadaanMasterAsset().getId());
				o.put("po", kodePo);
				o.put("uraian", uraian);
				o.put("nilai", nilai);
				o.put("tanggal", tanggal == null ? "" : Common.dateFormat1.get().format(tanggal));
				o.put("disetujui", disetujui);
				o.put("status", disetujui ? "DISETUJUI" : "BELUM DISETUJUI");
				// Khas POS: penanda apakah penerimaan ini sudah disalin menjadi faktur Kulakan
				// (stok + HPP). Selama belum, barangnya memang masih "dalam proses" di gudang.
				o.put("sudahMasukStok", bast.getPengadaanFaktur() != null);
				arr.put(o);
			}
			hasil.put("status", "00");
			hasil.put("mode", "bast");
			hasil.put("data", arr);
			hasil.put("total", cocok);
			hasil.put("totalNilai", totalNilai);
			hasil.put("jumlahDisetujui", jumlahDisetujui);
			hasil.put("nilaiDisetujui", nilaiDisetujui);
			hasil.put("jumlahBelumDisetujui", cocok - jumlahDisetujui);
			hasil.put("nilaiBelumDisetujui", totalNilai - nilaiDisetujui);
			hasil.put("hanyaCip", hanyaCip);
			if (cocok == 0) {
				hasil.put("catatan", hanyaCip
						? "Tidak ada penerimaan dengan kelompok aset \"Pekerjaan Dalam Pelaksanaan\". "
								+ "Matikan penyaring CIP untuk melihat seluruh penerimaan."
						: "Belum ada penerimaan barang (BAST) yang tercatat.");
			}
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Pandangan pendamping Barang Dalam Proses: barang yang SUDAH DIPESAN tetapi BELUM DITERIMA.
	 *
	 * <p>Dahulu inilah isi menu Barang Dalam Proses, sampai ketahuan bahwa istilah itu di versi
	 * ZKoss berarti CIP dan bersumber dari BAST (lihat {@link #bdpDaftar}). Pandangan ini tetap
	 * dipertahankan karena berguna memantau pengiriman yang tertunda, dan dipanggil dengan
	 * {@code mode=belum_datang}.</p>
	 *
	 * <p>Selisih PO dan BAST di sini memakai definisi yang sama dengan {@link #bastDariPo},
	 * sehingga angka pada layar pemantauan tidak pernah berbeda dengan angka yang menjadi pagar
	 * saat menerima barang.</p>
	 *
	 * <p>Param opsional: {@code cari} (kode PO/nama barang), {@code penyedia_id},
	 * {@code hanyaTerlambat}, {@code page}, {@code pageSize}.</p>
	 */
	public static void bdpBelumDatang(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
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
			hasil.put("mode", "belum_datang");
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
			Long fakturId = ais.common.Common.angkaAtauNull(hasilKulakan, "fakturId");
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


	/**
	 * Pilihan jenis pajak untuk editor termin: PPh (JenisPajakBarang) dan PPN (JenisPajakPpn).
	 * Tarifnya dikirim apa adanya supaya layar dapat menghitung pratinjau tanpa menebak.
	 */
	public static void pajakOpsi(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehLihat(tbmuser, KUNCI_PO) && !bolehLihat(tbmuser, KUNCI_DPC)
				&& !bolehLihat(tbmuser, KUNCI_PAJAK)) {
			tolak(hasil, "Menu Pengadaan tidak diaktifkan untuk grup pengguna Anda.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			JSONArray pph = new JSONArray();
			@SuppressWarnings("unchecked")
			List<ais.database.model.asset.JenisPajakBarang> daftarPph = session
					.createCriteria(ais.database.model.asset.JenisPajakBarang.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
					.addOrder(Order.asc("nama")).list();
			for (ais.database.model.asset.JenisPajakBarang j : daftarPph) {
				JSONObject o = new JSONObject();
				o.put("id", j.getId());
				o.put("kode", j.getKode() == null ? "" : j.getKode());
				o.put("nama", j.getNama() == null ? "" : j.getNama());
				o.put("persen", j.getPersen() == null ? 0 : j.getPersen());
				pph.put(o);
			}
			JSONArray ppn = new JSONArray();
			@SuppressWarnings("unchecked")
			List<ais.database.model.asset.JenisPajakPpn> daftarPpn = session
					.createCriteria(ais.database.model.asset.JenisPajakPpn.class)
					.addOrder(Order.asc("nama")).list();
			for (ais.database.model.asset.JenisPajakPpn j : daftarPpn) {
				JSONObject o = new JSONObject();
				o.put("id", j.getId());
				o.put("kode", j.getKode() == null ? "" : j.getKode());
				o.put("nama", j.getNama() == null ? "" : j.getNama());
				o.put("persen", j.getPersen() == null ? 0 : j.getPersen());
				ppn.put(o);
			}
			hasil.put("status", "00");
			hasil.put("pph", pph);
			hasil.put("ppn", ppn);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Tarif sebuah jenis PPh; 0 bila tidak dikenali. */
	private static double persenPph(Session session, String idPajak) {
		if (idPajak == null || idPajak.trim().isEmpty()) {
			return 0;
		}
		try {
			ais.database.model.asset.JenisPajakBarang j = (ais.database.model.asset.JenisPajakBarang) session
					.get(ais.database.model.asset.JenisPajakBarang.class, Long.valueOf(idPajak.trim()));
			return j == null || j.getPersen() == null ? 0 : j.getPersen().doubleValue();
		} catch (Exception e) {
			return 0;
		}
	}

	/** Tarif sebuah jenis PPN; 0 bila tidak dikenali. */
	private static double persenPpn(Session session, String idPajak) {
		if (idPajak == null || idPajak.trim().isEmpty()) {
			return 0;
		}
		try {
			ais.database.model.asset.JenisPajakPpn j = (ais.database.model.asset.JenisPajakPpn) session
					.get(ais.database.model.asset.JenisPajakPpn.class, Long.valueOf(idPajak.trim()));
			return j == null || j.getPersen() == null ? 0 : j.getPersen().doubleValue();
		} catch (Exception e) {
			return 0;
		}
	}

	/** Nama jenis pajak untuk ditampilkan; kosong bila tidak dikenali. */
	private static String namaJenisPajak(Session session, String idPph, String idPpn) {
		try {
			if (idPph != null && !idPph.trim().isEmpty()) {
				ais.database.model.asset.JenisPajakBarang j = (ais.database.model.asset.JenisPajakBarang) session
						.get(ais.database.model.asset.JenisPajakBarang.class, Long.valueOf(idPph.trim()));
				if (j != null) {
					return j.getNama() == null ? "" : j.getNama();
				}
			}
			if (idPpn != null && !idPpn.trim().isEmpty()) {
				ais.database.model.asset.JenisPajakPpn j = (ais.database.model.asset.JenisPajakPpn) session
						.get(ais.database.model.asset.JenisPajakPpn.class, Long.valueOf(idPpn.trim()));
				if (j != null) {
					return j.getNama() == null ? "" : j.getNama();
				}
			}
		} catch (Exception e) {
			return "";
		}
		return "";
	}


	/**
	 * Pajak TERUTANG: PPh dan PPN yang melekat pada baris pembayaran vendor yang sudah
	 * disetujui tetapi belum tercakup rekaman setoran mana pun.
	 *
	 * <p>PPh dipotong dari kas yang keluar dan menjadi kewajiban kita kepada negara,
	 * sedangkan PPN dibayarkan kepada vendor sebagai pajak masukan; keduanya ditampilkan
	 * agar petugas melihat gambaran utuh sebelum menyetor.</p>
	 */
	/**
	 * Dasar Pengenaan Pajak satu baris penerimaan (BAST), memakai rumus yang sama persis dengan
	 * {@code PenerimaanPengadaanMasterAssetDetail.getHargaTotal()}: (diterima x harga) dikurangi
	 * potongan, sebelum PPN/PPh dikenakan. Ditulis sebagai satu fungsi agar layar, penyimpanan,
	 * dan daftar pajak tidak pernah memakai angka yang berbeda.
	 */
	private static double dppBarisBast(PenerimaanPengadaanMasterAssetDetail d) {
		double diterima = d.getDiterima() == null ? 0 : d.getDiterima().doubleValue();
		double harga = d.getHargaBeli() == null ? 0 : d.getHargaBeli().doubleValue();
		double dpp = diterima * harga;
		double potongan = d.getHargaPotongan() == null ? 0 : d.getHargaPotongan().doubleValue();
		dpp -= Boolean.TRUE.equals(d.getDiskonDalamBentukPersen()) ? (potongan / 100.0) * dpp : potongan;
		return dpp;
	}

	/**
	 * Daftar pajak yang masih terutang, dari DUA sumber:
	 *
	 * <ol>
	 * <li><b>Pembayaran vendor</b> -- PPh termin, mengikuti definisi
	 * {@code PembayaranTerminMasterAssetDetail.getNilaiPphTermin()} yang juga dipakai
	 * {@code Pajak.buatDariTermin} pada versi ZKoss.</li>
	 * <li><b>Penerimaan barang (BAST)</b> -- PPN dan PPh yang diketik per baris pada layar
	 * penerimaan. Sebelumnya sumber ini terlewat sama sekali, sehingga PPN yang sudah diisi di
	 * BAST tidak pernah muncul di layar Bayar Pajak (laporan pemilik produk, 2026-08-21).</li>
	 * </ol>
	 *
	 * <p>Baris BAST yang dokumennya belum disetujui tetap ditampilkan -- lengkap dengan penanda
	 * {@code dokumenDisetujui=false} -- supaya pajaknya terlihat sejak awal, tetapi
	 * {@link #pajakSetor} menolak menyetorkannya sampai dokumennya sah. Menyembunyikannya justru
	 * membuat pengguna mengira pajaknya hilang.</p>
	 */
	public static void pajakTerutang(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehLihat(tbmuser, KUNCI_PAJAK)) {
			tolak(hasil, "Menu Pengadaan tidak diaktifkan untuk grup pengguna Anda.");
			return;
		}
		Long tokoId = tokoLingkup(tbmuser, request);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			JSONArray arr = new JSONArray();
			double totalPph = 0;
			double totalPpn = 0;

			// --- Sumber 1: pembayaran vendor (PPh termin) ---------------------------------
			@SuppressWarnings("unchecked")
			List<PembayaranTerminMasterAssetDetail> baris = session
					.createCriteria(PembayaranTerminMasterAssetDetail.class)
					.addOrder(Order.asc("id")).list();
			for (PembayaranTerminMasterAssetDetail d : baris) {
				PembayaranTerminMasterAsset induk = d.getPembayaranTerminMasterAsset();
				if (induk == null || induk.getDisetujuiOleh() == null
						|| Boolean.FALSE.equals(induk.getAktif())) {
					continue;
				}
				if (tokoId != null && induk.getToko() != null && !tokoId.equals(induk.getToko().getId())) {
					continue;
				}
				if (d.getPajak() != null) {
					continue;
				}
				JSONObject rincian = rincianPajakBaris(session, d);
				double pph = rincian.optDouble("pph", 0);
				double ppn = rincian.optDouble("ppn", 0);
				if (pph <= 0 && ppn <= 0) {
					continue;
				}
				PemesananPengadaanMasterAsset po = d.getPemesananPengadaanMasterAsset();
				JSONObject o = new JSONObject();
				o.put("sumber", "PEMBAYARAN");
				o.put("detail_id", d.getId());
				o.put("bayar_id", induk.getId());
				o.put("bayar", induk.getKode() == null ? "" : induk.getKode());
				o.put("dokumen", induk.getKode() == null ? "" : induk.getKode());
				o.put("dokumenDisetujui", true);
				o.put("tanggal", induk.getTanggalPembuatan() == null ? ""
						: Common.dateFormat1.get().format(induk.getTanggalPembuatan()));
				o.put("penyedia", induk.getPenyedia() == null ? "" : induk.getPenyedia().getNama());
				o.put("po", po == null || po.getKode() == null ? "" : po.getKode());
				o.put("termin", rincian.optString("termin", ""));
				o.put("dpp", rincian.optDouble("dpp", 0));
				o.put("namaPajak", rincian.optString("namaPajak", ""));
				o.put("persenPph", rincian.optDouble("persenPph", 0));
				o.put("persenPpn", 0);
				o.put("pph", pph);
				o.put("ppn", ppn);
				arr.put(o);
				totalPph += pph;
				totalPpn += ppn;
			}

			// --- Sumber 2: penerimaan barang (BAST) ---------------------------------------
			@SuppressWarnings("unchecked")
			List<PenerimaanPengadaanMasterAssetDetail> barisBast = session
					.createCriteria(PenerimaanPengadaanMasterAssetDetail.class)
					// Hanya baris yang benar-benar berpajak yang perlu diperiksa. Tanpa saringan
					// ini seluruh tabel penerimaan ikut terbaca -- mahal pada basis data besar.
					.add(Restrictions.or(Restrictions.gt("persenPpn", Double.valueOf(0)),
							Restrictions.gt("persenPph", Double.valueOf(0))))
					.add(Restrictions.isNull("pajak"))
					.addOrder(Order.asc("id")).setMaxResults(3000).list();
			for (PenerimaanPengadaanMasterAssetDetail d : barisBast) {
				PenerimaanPengadaanMasterAsset induk = d.getPenerimaanPengadaanMasterAsset();
				if (induk == null || Boolean.FALSE.equals(induk.getAktif())) {
					continue;
				}
				if (tokoId != null && induk.getToko() != null && !tokoId.equals(induk.getToko().getId())) {
					continue;
				}
				if (d.getPajak() != null) {
					continue;
				}
				double persenPpn = d.getPersenPpn() == null ? 0 : d.getPersenPpn().doubleValue();
				double persenPph = d.getPersenPph() == null ? 0 : d.getPersenPph().doubleValue();
				if (persenPpn <= 0 && persenPph <= 0) {
					continue;
				}
				double dpp = dppBarisBast(d);
				double ppn = Math.rint((persenPpn / 100.0) * dpp);
				double pph = Math.rint((persenPph / 100.0) * dpp);
				if (ppn <= 0 && pph <= 0) {
					continue;
				}
				PemesananPengadaanMasterAsset po = induk.getPemesananPengadaanMasterAsset();
				JSONObject o = new JSONObject();
				o.put("sumber", "BAST");
				o.put("bast_detail_id", d.getId());
				o.put("bast_id", induk.getId());
				o.put("bast", induk.getKode() == null ? "" : induk.getKode());
				o.put("dokumen", induk.getKode() == null ? "" : induk.getKode());
				o.put("dokumenDisetujui", induk.getDisetujuiOleh() != null);
				o.put("tanggal", induk.getTanggalPembuatan() == null ? ""
						: Common.dateFormat1.get().format(induk.getTanggalPembuatan()));
				o.put("penyedia", induk.getPenyedia() == null ? "" : induk.getPenyedia().getNama());
				o.put("po", po == null || po.getKode() == null ? "" : po.getKode());
				o.put("termin", "");
				o.put("barang", d.getMasterAsset() == null ? "" : d.getMasterAsset().getNama());
				o.put("dpp", dpp);
				o.put("namaPajak", d.getJenisPajakBarang() == null ? "" : d.getJenisPajakBarang().getNama());
				o.put("persenPph", persenPph);
				o.put("persenPpn", persenPpn);
				o.put("pph", pph);
				o.put("ppn", ppn);
				arr.put(o);
				totalPph += pph;
				totalPpn += ppn;
			}

			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("total", arr.length());
			hasil.put("totalPph", totalPph);
			hasil.put("totalPpn", totalPpn);
			if (arr.length() == 0) {
				hasil.put("catatan", "Tidak ada pajak terutang. Pajak muncul di sini bila PPN/PPh diisi "
						+ "pada penerimaan barang (BAST), atau bila pembayaran vendor bertermin "
						+ "yang sudah disetujui memotong PPh.");
			}
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Rincian pajak sebuah baris pembayaran, dibaca dari item termin PO yang ditunjuknya.
	 * Perhitungannya sengaja sama dengan yang dipakai layar PO agar angka pada kedua
	 * tempat tidak pernah berselisih.
	 */
	private static JSONObject rincianPajakBaris(Session session, PembayaranTerminMasterAssetDetail d)
			throws Exception {
		JSONObject hasil = new JSONObject();
		hasil.put("dpp", 0);
		hasil.put("pph", 0);
		hasil.put("ppn", 0);
		hasil.put("termin", "");
		hasil.put("namaPajak", "");
		hasil.put("persenPph", 0);
		PemesananPengadaanMasterAsset po = d.getPemesananPengadaanMasterAsset();
		if (po == null) {
			return hasil;
		}
		String kunci = "";
		if (d.getTagihan() != null && !d.getTagihan().trim().isEmpty()) {
			try {
				JSONObject t = new JSONObject(d.getTagihan());
				kunci = t.isNull("key") ? "" : (t.get("key") + "").trim();
			} catch (Exception e) {
				kunci = "";
			}
		}
		JSONArray termin = terminDari(po);
		for (int i = 0; i < termin.length(); i++) {
			JSONObject t = termin.optJSONObject(i);
			if (t == null) {
				continue;
			}
			String k = t.isNull("key") ? "" : (t.get("key") + "").trim();
			if (!kunci.equals(k)) {
				continue;
			}
			String idPph = t.isNull("pajak") ? "" : (t.get("pajak") + "").trim();
			String idPpn = t.isNull("pajakPpn") ? "" : (t.get("pajakPpn") + "").trim();
			double dpp = angkaAman(t, "penagihan");
			double tarif = persenPph(session, idPph);
			// PPh dan PPN dihitung SEBANDING dengan porsi yang benar-benar dibayar, supaya
			// pembayaran sebagian tidak menyetorkan pajak atas nilai yang belum dibayar.
			double dibayar = d.getDibayar() == null ? 0 : d.getDibayar().doubleValue();
			double porsi = dpp <= 0 ? 0 : Math.min(1.0, dibayar / dpp);
			hasil.put("dpp", Math.rint(dpp * porsi));
			hasil.put("pph", Math.rint((tarif / 100.0) * dpp * porsi));
			hasil.put("ppn", Math.rint(angkaAman(t, "ppn") * porsi));
			hasil.put("termin", t.isNull("nama") ? "" : t.get("nama") + "");
			hasil.put("namaPajak", namaJenisPajak(session, idPph, idPpn));
			hasil.put("persenPph", tarif);
			return hasil;
		}
		return hasil;
	}

	/**
	 * Catat setoran pajak atas baris-baris pembayaran terpilih.
	 *
	 * <p>Satu rekaman {@code Pajak} mewakili SATU jenis setoran (PPH atau PPN), mengikuti
	 * bentuk yang dipakai layar Pertanggungjawaban Pajak versi ZKoss: kode, nama, jenis
	 * pajak, DPP, nilai, NPWP, nama wajib pajak, NTPN, dan tanggal setor.</p>
	 *
	 * <p>Baris yang sudah tercakup setoran lain ditolak, sehingga satu kewajiban tidak
	 * pernah disetorkan dua kali.</p>
	 */
	public static void pajakSetor(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehAksi(tbmuser, KUNCI_PAJAK, "create")) {
			tolak(hasil, "Grup pengguna Anda tidak memiliki hak mencatat setoran pajak.");
			return;
		}
		if (tbmuser == null) {
			tolak(hasil, "Sesi pengguna tidak dikenali, silakan masuk ulang.");
			return;
		}
		JSONArray detail = request == null ? null : request.optJSONArray("detail");
		if (detail == null || detail.length() == 0) {
			tolak(hasil, "Pilih minimal satu baris pajak yang akan disetor.");
			return;
		}
		String jenis = request.optString("jenis", "PPH").trim().toUpperCase();
		if (!"PPH".equals(jenis) && !"PPN".equals(jenis)) {
			tolak(hasil, "Jenis setoran hanya PPH atau PPN.");
			return;
		}
		String ntpn = request.optString("ntpn", "").trim();
		if (ntpn.isEmpty()) {
			tolak(hasil, "NTPN (Nomor Transaksi Penerimaan Negara) wajib diisi sebagai bukti setor.");
			return;
		}
		String tglSetor = request.optString("tanggalSetor", "").trim();
		java.util.Date tanggalSetor = tanggalKetat(tglSetor);
		if (tanggalSetor == null) {
			tolak(hasil, "Tanggal setor wajib diisi dan berformat hh-bb-tttt, mis. 20-08-2026.");
			return;
		}
		Long tokoId = tokoLingkup(tbmuser, request);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			java.util.List<PembayaranTerminMasterAssetDetail> terpilih =
					new java.util.ArrayList<PembayaranTerminMasterAssetDetail>();
			java.util.List<PenerimaanPengadaanMasterAssetDetail> terpilihBast =
					new java.util.ArrayList<PenerimaanPengadaanMasterAssetDetail>();
			double totalDpp = 0;
			double totalNilai = 0;
			String idJenisPph = "";
			String idJenisPpn = "";
			for (int i = 0; i < detail.length(); i++) {
				JSONObject b = detail.optJSONObject(i);
				if (b == null) {
					continue;
				}
				// Baris bersumber BAST: PPN/PPh yang diketik langsung pada penerimaan barang.
				if (!b.isNull("bast_detail_id") && !(b.get("bast_detail_id") + "").trim().isEmpty()) {
					PenerimaanPengadaanMasterAssetDetail db = (PenerimaanPengadaanMasterAssetDetail) session
							.get(PenerimaanPengadaanMasterAssetDetail.class,
									Long.valueOf((b.get("bast_detail_id") + "").trim()));
					if (db == null) {
						tolak(hasil, "Baris penerimaan ke-" + (i + 1) + " tidak ditemukan.");
						return;
					}
					PenerimaanPengadaanMasterAsset indukBast = db.getPenerimaanPengadaanMasterAsset();
					if (indukBast == null || indukBast.getDisetujuiOleh() == null) {
						tolak(hasil, "Pajak dari penerimaan barang hanya dapat disetor setelah BAST-nya "
								+ "disetujui. Setujui dahulu "
								+ (indukBast == null || indukBast.getKode() == null ? "BAST tersebut"
										: indukBast.getKode())
								+ ".");
						return;
					}
					if (tokoId != null && indukBast.getToko() != null
							&& !tokoId.equals(indukBast.getToko().getId())) {
						tolak(hasil, "Baris penerimaan ke-" + (i + 1) + " milik toko lain.");
						return;
					}
					if (db.getPajak() != null) {
						tolak(hasil, "Baris penerimaan pada "
								+ (indukBast.getKode() == null ? "" : indukBast.getKode())
								+ " sudah tercakup setoran pajak sebelumnya.");
						return;
					}
					double persen = "PPH".equals(jenis)
							? (db.getPersenPph() == null ? 0 : db.getPersenPph().doubleValue())
							: (db.getPersenPpn() == null ? 0 : db.getPersenPpn().doubleValue());
					double dppBast = dppBarisBast(db);
					double nilaiBast = Math.rint((persen / 100.0) * dppBast);
					if (nilaiBast <= 0) {
						tolak(hasil, "Baris penerimaan ke-" + (i + 1) + " tidak memiliki " + jenis
								+ " untuk disetor.");
						return;
					}
					totalDpp += dppBast;
					totalNilai += nilaiBast;
					terpilihBast.add(db);
					if ("PPH".equals(jenis) && idJenisPph.isEmpty() && db.getJenisPajakBarang() != null) {
						idJenisPph = db.getJenisPajakBarang().getId() + "";
					}
					if ("PPN".equals(jenis) && idJenisPpn.isEmpty() && db.getJenisPajakPpn() != null) {
						idJenisPpn = db.getJenisPajakPpn().getId() + "";
					}
					continue;
				}
				if (b.isNull("detail_id")) {
					continue;
				}
				PembayaranTerminMasterAssetDetail d = (PembayaranTerminMasterAssetDetail) session
						.get(PembayaranTerminMasterAssetDetail.class,
								Long.valueOf((b.get("detail_id") + "").trim()));
				if (d == null) {
					tolak(hasil, "Baris pembayaran ke-" + (i + 1) + " tidak ditemukan.");
					return;
				}
				PembayaranTerminMasterAsset induk = d.getPembayaranTerminMasterAsset();
				if (induk == null || induk.getDisetujuiOleh() == null) {
					tolak(hasil, "Pajak hanya dapat disetor dari pembayaran vendor yang sudah disetujui.");
					return;
				}
				if (tokoId != null && induk.getToko() != null && !tokoId.equals(induk.getToko().getId())) {
					tolak(hasil, "Baris pembayaran ke-" + (i + 1) + " milik toko lain.");
					return;
				}
				if (d.getPajak() != null) {
					tolak(hasil, "Baris pembayaran pada " + (induk.getKode() == null ? "" : induk.getKode())
							+ " sudah tercakup setoran pajak sebelumnya.");
					return;
				}
				JSONObject rincian = rincianPajakBaris(session, d);
				double nilai = "PPH".equals(jenis) ? rincian.optDouble("pph", 0) : rincian.optDouble("ppn", 0);
				if (nilai <= 0) {
					tolak(hasil, "Baris pembayaran ke-" + (i + 1) + " tidak memiliki " + jenis + " untuk disetor.");
					return;
				}
				totalDpp += rincian.optDouble("dpp", 0);
				totalNilai += nilai;
				terpilih.add(d);
				if (idJenisPph.isEmpty() || idJenisPpn.isEmpty()) {
					PemesananPengadaanMasterAsset po = d.getPemesananPengadaanMasterAsset();
					JSONArray termin = terminDari(po);
					for (int j = 0; j < termin.length(); j++) {
						JSONObject t = termin.optJSONObject(j);
						if (t == null) {
							continue;
						}
						if (idJenisPph.isEmpty() && !t.isNull("pajak")) {
							idJenisPph = (t.get("pajak") + "").trim();
						}
						if (idJenisPpn.isEmpty() && !t.isNull("pajakPpn")) {
							idJenisPpn = (t.get("pajakPpn") + "").trim();
						}
					}
				}
			}
			if (terpilih.isEmpty() && terpilihBast.isEmpty()) {
				tolak(hasil, "Tidak ada baris pajak yang dapat disetor.");
				return;
			}

			session.beginTransaction();
			ais.database.model.akunting.Pajak pajak = new ais.database.model.akunting.Pajak();
			pajak.setKode(buatKodeUmum(session, ais.database.model.akunting.Pajak.class, "PJK", tokoId));
			pajak.setNama("Setoran " + jenis + " pengadaan");
			pajak.setKeterangan(request.optString("keterangan", "").trim());
			pajak.setDpp(Double.valueOf(totalDpp));
			pajak.setNilai(Double.valueOf(totalNilai));
			pajak.setJumlah(Double.valueOf(totalNilai));
			pajak.setNtpn(ntpn);
			pajak.setNpwp(request.optString("npwp", "").trim());
			pajak.setNamaWp(request.optString("namaWp", "").trim());
			pajak.setTanggal(tanggalSetor);
			pajak.setTanggalStor(tanggalSetor);
			pajak.setTanggalTransaksi(tanggalSetor);
			pajak.setAktif(Boolean.TRUE);
			if ("PPH".equals(jenis) && !idJenisPph.isEmpty()) {
				pajak.setJenisPajakBarang((ais.database.model.asset.JenisPajakBarang) session
						.get(ais.database.model.asset.JenisPajakBarang.class, Long.valueOf(idJenisPph)));
			}
			if ("PPN".equals(jenis) && !idJenisPpn.isEmpty()) {
				pajak.setJenisPajakPpn((ais.database.model.asset.JenisPajakPpn) session
						.get(ais.database.model.asset.JenisPajakPpn.class, Long.valueOf(idJenisPpn)));
			}
			pajak.setOleh(tbmuser.getUserNama());
			pajak.setOlehId(tbmuser.getUserId());
			session.save(pajak);
			session.flush();
			for (PembayaranTerminMasterAssetDetail d : terpilih) {
				d.setPajak(pajak);
				session.saveOrUpdate(d);
			}
			for (PenerimaanPengadaanMasterAssetDetail db : terpilihBast) {
				db.setPajak(pajak);
				session.saveOrUpdate(db);
			}
			session.getTransaction().commit();

			hasil.put("status", "00");
			hasil.put("id", pajak.getId());
			hasil.put("kode", pajak.getKode());
			hasil.put("jenis", jenis);
			hasil.put("nilai", totalNilai);
			hasil.put("jumlahBaris", terpilih.size() + terpilihBast.size());
		} catch (Exception e) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception eRollback) {
				ais.common.ErrorAuditUtil.record(eRollback, "PengadaanPosApiHelper.pajakSetor rollback");
			}
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Daftar setoran pajak yang berasal dari pembayaran vendor pada lingkup toko.
	 * Kolomnya mengikuti layar Pertanggungjawaban Pajak versi ZKoss supaya petugas
	 * yang sudah terbiasa tidak perlu belajar bentuk baru.
	 */
	public static void pajakDaftar(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehLihat(tbmuser, KUNCI_PAJAK)) {
			tolak(hasil, "Menu Pengadaan tidak diaktifkan untuk grup pengguna Anda.");
			return;
		}
		Long tokoId = tokoLingkup(tbmuser, request);
		String cari = request == null ? "" : request.optString("cari", "").trim().toLowerCase();
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			@SuppressWarnings("unchecked")
			List<PembayaranTerminMasterAssetDetail> baris = session
					.createCriteria(PembayaranTerminMasterAssetDetail.class)
					.add(Restrictions.isNotNull("pajak")).addOrder(Order.desc("id")).list();
			java.util.Map<Long, JSONObject> peta = new java.util.LinkedHashMap<Long, JSONObject>();
			for (PembayaranTerminMasterAssetDetail d : baris) {
				PembayaranTerminMasterAsset induk = d.getPembayaranTerminMasterAsset();
				if (induk == null) {
					continue;
				}
				if (tokoId != null && induk.getToko() != null && !tokoId.equals(induk.getToko().getId())) {
					continue;
				}
				ais.database.model.akunting.Pajak pj = d.getPajak();
				if (pj == null) {
					continue;
				}
				String kode = pj.getKode() == null ? "" : pj.getKode();
				if (cari.length() > 0 && kode.toLowerCase().indexOf(cari) < 0
						&& (pj.getNtpn() == null ? "" : pj.getNtpn().toLowerCase()).indexOf(cari) < 0) {
					continue;
				}
				JSONObject o = peta.get(pj.getId());
				if (o == null) {
					o = new JSONObject();
					o.put("id", pj.getId());
					o.put("kode", kode);
					o.put("nama", pj.getNama() == null ? "" : pj.getNama());
					o.put("jenisPajak", pj.getJenisPajakBarang() != null
							? (pj.getJenisPajakBarang().getNama() == null ? "" : pj.getJenisPajakBarang().getNama())
							: (pj.getJenisPajakPpn() == null || pj.getJenisPajakPpn().getNama() == null
									? "" : pj.getJenisPajakPpn().getNama()));
					o.put("jenis", pj.getJenisPajakPpn() != null ? "PPN" : "PPH");
					o.put("dpp", pj.getDpp() == null ? 0 : pj.getDpp());
					o.put("nilai", pj.getNilai() == null ? 0 : pj.getNilai());
					o.put("ntpn", pj.getNtpn() == null ? "" : pj.getNtpn());
					o.put("npwp", pj.getNpwp() == null ? "" : pj.getNpwp());
					o.put("namaWp", pj.getNamaWp() == null ? "" : pj.getNamaWp());
					o.put("keterangan", pj.getKeterangan() == null ? "" : pj.getKeterangan());
					o.put("tanggalSetor", pj.getTanggalStor() == null ? ""
							: Common.dateFormat1.get().format(pj.getTanggalStor()));
					o.put("aktif", !Boolean.FALSE.equals(pj.getAktif()));
					o.put("jumlahBaris", 0);
					peta.put(pj.getId(), o);
				}
				o.put("jumlahBaris", o.optInt("jumlahBaris") + 1);
			}
			JSONArray arr = new JSONArray();
			for (JSONObject o : peta.values()) {
				arr.put(o);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("total", arr.length());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Batalkan sebuah setoran pajak: rekamannya dinonaktifkan (bukan dihapus, agar jejak
	 * bukti setor tetap terbaca) dan baris pembayaran yang ditanggungnya kembali menjadi
	 * terutang sehingga dapat disetor ulang dengan bukti yang benar.
	 */
	public static void pajakBatal(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehAksi(tbmuser, KUNCI_PAJAK, "delete")) {
			tolak(hasil, "Grup pengguna Anda tidak memiliki hak membatalkan setoran pajak.");
			return;
		}
		Long id = (request == null || request.isNull("id")) ? null
				: Long.valueOf((request.get("id") + "").trim());
		if (id == null) {
			tolak(hasil, "Parameter id wajib diisi.");
			return;
		}
		Long tokoId = tokoLingkup(tbmuser, request);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ais.database.model.akunting.Pajak pajak = (ais.database.model.akunting.Pajak) session
					.get(ais.database.model.akunting.Pajak.class, id);
			if (pajak == null) {
				tolak(hasil, "Setoran pajak tidak ditemukan.");
				return;
			}
			@SuppressWarnings("unchecked")
			List<PembayaranTerminMasterAssetDetail> baris = session
					.createCriteria(PembayaranTerminMasterAssetDetail.class)
					.add(Restrictions.eq("pajak.id", id)).list();
			for (PembayaranTerminMasterAssetDetail d : baris) {
				PembayaranTerminMasterAsset induk = d.getPembayaranTerminMasterAsset();
				if (tokoId != null && induk != null && induk.getToko() != null
						&& !tokoId.equals(induk.getToko().getId())) {
					tolak(hasil, "Setoran pajak ini menyangkut toko lain.");
					return;
				}
			}
			session.beginTransaction();
			for (PembayaranTerminMasterAssetDetail d : baris) {
				d.setPajak(null);
				session.saveOrUpdate(d);
			}
			pajak.setAktif(Boolean.FALSE);
			pajak.setOleh(tbmuser == null ? "" : tbmuser.getUserNama());
			pajak.setOlehId(tbmuser == null ? "" : tbmuser.getUserId());
			session.saveOrUpdate(pajak);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("id", id);
			hasil.put("barisDilepas", baris.size());
		} catch (Exception e) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception eRollback) {
				ais.common.ErrorAuditUtil.record(eRollback, "PengadaanPosApiHelper.pajakBatal rollback");
			}
			throw e;
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
		if ("pengadaan_pr_barang_tersedia".equals(action)) {
			prBarangTersedia(tbmuser, request, hasil);
			return true;
		}
		if ("pengadaan_lampiran_daftar".equals(action)) {
			lampiranDaftar(tbmuser, request, hasil);
			return true;
		}
		if ("pengadaan_lampiran_unggah".equals(action)) {
			lampiranUnggah(tbmuser, request, hasil);
			return true;
		}
		if ("pengadaan_lampiran_unduh".equals(action)) {
			lampiranUnduh(tbmuser, request, hasil);
			return true;
		}
		if ("pengadaan_lampiran_hapus".equals(action)) {
			lampiranHapus(tbmuser, request, hasil);
			return true;
		}
		if ("pengadaan_cara_bayar_opsi".equals(action)) {
			caraBayarOpsi(tbmuser, request, hasil);
			return true;
		}
		if ("pengadaan_po_kekurangan".equals(action)) {
			poKekurangan(tbmuser, request, hasil);
			return true;
		}
		if ("pengadaan_po_back_order".equals(action)) {
			poBackOrder(tbmuser, request, hasil);
			return true;
		}
		if ("pengadaan_dasbor".equals(action)) {
			dasbor(tbmuser, request, hasil);
			return true;
		}
		if ("pengadaan_anggaran_cari".equals(action)) {
			cariAnggaran(tbmuser, request, hasil);
			return true;
		}
		if ("pengadaan_cetak".equals(action)) {
			cetakDokumen(tbmuser, request, hasil);
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
		if ("pengadaan_tagihan_rutin_simpan".equals(action)) {
			tagihanRutinSimpan(tbmuser, request, hasil);
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
		if ("pengadaan_transitori_daftar".equals(action) || "pengadaan_transitori_list".equals(action)) {
			transitoriDaftar(tbmuser, request, hasil);
			return true;
		}
		if ("pengadaan_transitori_realisasi".equals(action)) {
			transitoriRealisasi(tbmuser, request, hasil);
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
		if ("pengadaan_pajak_opsi".equals(action)) {
			pajakOpsi(tbmuser, request, hasil);
			return true;
		}
		if ("pengadaan_pajak_terutang".equals(action)) {
			pajakTerutang(tbmuser, request, hasil);
			return true;
		}
		if ("pengadaan_pajak_setor".equals(action)) {
			pajakSetor(tbmuser, request, hasil);
			return true;
		}
		if ("pengadaan_pajak_daftar".equals(action)) {
			pajakDaftar(tbmuser, request, hasil);
			return true;
		}
		if ("pengadaan_pajak_batal".equals(action)) {
			pajakBatal(tbmuser, request, hasil);
			return true;
		}
		return false;
	}
}
