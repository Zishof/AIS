package ais.action.master.koperasi;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Label;
import org.zkoss.zul.Vlayout;

import ais.action.master.akunting.util.CommonAkunting;
import ais.action.master.asset.util.AssetUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.PostingHistory;
import ais.database.model.asset.KelompokAsset;
import ais.database.model.asset.MasterAsset;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.DashboardUiKit;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.WaktuUtil;

/**
 * <h3>Posting HPP (Beban Pokok Penjualan) Penjualan Kantin — FASE 2B</h3>
 *
 * <p>Mencatat HPP penjualan produk kantin yang tertaut ke barang persediaan (modul Aset) ke jurnal
 * akuntansi secara <b>periodik/batch</b>. Untuk periode terpilih: HPP = Σ(qty terjual ×
 * {@code Produk.hargaBeli}) per barang, lalu diposting 1 jurnal lewat {@link CommonAkunting#saveTransaksi}.</p>
 *
 * <p><b>Akun diambil dari Kelompok Aset</b> tiap barang (bukan dari konfigurasi global):</p>
 * <ul>
 *   <li><b>Debit</b> = {@code KelompokAsset.akunBebanPokokPenjualan} (akun HPP),</li>
 *   <li><b>Kredit</b> = {@code MasterAsset.akunTransaksi} (akun persediaan; cadangan: KelompokAsset.akunTransaksi),</li>
 * </ul>
 * keduanya di-resolusi per satuan kerja via {@link AssetUtil#ambilDataAkun}. Jadi pengaturan akun cukup
 * sekali di master Kelompok Aset, tanpa setting konfigurasi terpisah.
 *
 * <h3>Aman by design</h3>
 * <ul>
 *   <li><b>Maju-saja (idempoten):</b> "Mulai" wajib setelah periode terakhir yang sudah diposting
 *       (MAX tanggal {@link PostingHistory} jenis {@link #JENIS}).</li>
 *   <li><b>Pratinjau dulu;</b> barang yang akunnya belum lengkap di Kelompok Aset diblokir agar jurnal seimbang.</li>
 *   <li><b>Sadar closing</b> (lewat {@code saveTransaksi}).</li>
 * </ul>
 */
public class PostingHppKantinAction extends GenericAutowireComposer {

	private static final long serialVersionUID = 1L;

	/** Jenis posting history khusus HPP kantin (juga kunci idempoten maju-saja). */
	public static final String JENIS = "HPP Penjualan Kantin";

	private Div postingHppHost;

	private Datebox dpMulai;
	private Datebox dpSampai;
	private Div previewBox;
	private Label lblStatus;

	private double totalCogs = 0.0;
	private final Map<Long, Double> debitPerAkun = new LinkedHashMap<Long, Double>();
	private final Map<Long, Double> kreditPerAkun = new LinkedHashMap<Long, Double>();
	private final Map<Long, String> akunNama = new LinkedHashMap<Long, String>();
	private final List<String> belumDipetakan = new ArrayList<String>();

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page, Component parent,
			org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (postingHppHost == null) {
			return;
		}
		DashboardUiKit.attachIntro(comp, "Posting HPP Penjualan Kantin",
				"Mencatat beban pokok penjualan barang kantin (yang tertaut ke persediaan aset) ke jurnal, per periode. "
						+ "Akun HPP & persediaan diambil dari Kelompok Aset tiap barang.");

		Vlayout box = new Vlayout();
		box.setWidth("100%");
		box.setStyle("gap:8px;padding:6px;");
		box.setParent(postingHppHost);

		Hlayout filter = new Hlayout();
		filter.setStyle("gap:8px;align-items:center;flex-wrap:wrap;");
		filter.setParent(box);
		filter.appendChild(new Label(ais.common.Common.getBahasaConfig("Periode penjualan:")));
		dpMulai = new Datebox();
		dpMulai.setFormat("dd-MM-yyyy");
		dpMulai.setWidth("130px");
		filter.appendChild(dpMulai);
		filter.appendChild(new Label(ais.common.Common.getBahasaConfig("s.d")));
		dpSampai = new Datebox();
		dpSampai.setFormat("dd-MM-yyyy");
		dpSampai.setWidth("130px");
		dpSampai.setValue(WaktuUtil.getDate());
		filter.appendChild(dpSampai);
		MyToolbarbuttonConfig btnTampil = new MyToolbarbuttonConfig("Tampilkan", "/img/search.gif");
		btnTampil.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				hitungPreview();
			}
		});
		filter.appendChild(btnTampil);

		lblStatus = new Label();
		lblStatus.setMultiline(true);
		lblStatus.setStyle("font-size:12px;color:#475569;");
		box.appendChild(lblStatus);

		previewBox = new Div();
		previewBox.setWidth("100%");
		box.appendChild(previewBox);

		Hlayout aksi = new Hlayout();
		aksi.setStyle("gap:8px;margin-top:6px;");
		aksi.setParent(box);
		Button btnPosting = new Button("Posting Jurnal HPP");
		btnPosting.setImage("/img/save.gif");
		btnPosting.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				onPosting();
			}
		});
		aksi.appendChild(btnPosting);

		Date lastEnd = lastPostedEnd();
		java.util.Calendar c = java.util.Calendar.getInstance();
		if (lastEnd != null) {
			c.setTime(lastEnd);
			c.add(java.util.Calendar.DAY_OF_MONTH, 1);
		} else {
			c.set(java.util.Calendar.DAY_OF_MONTH, 1);
		}
		dpMulai.setValue(c.getTime());

		refreshStatus();
		hitungPreview();
	}

	private void refreshStatus() {
		StringBuilder sb = new StringBuilder();
		Date lastEnd = lastPostedEnd();
		sb.append(lastEnd == null ? "Belum pernah ada posting HPP kantin."
				: ("Terakhir diposting s.d " + Common.dateFormat.get().format(lastEnd) + "."));
		sb.append("  Akun HPP (debit) & persediaan (kredit) diambil dari Kelompok Aset tiap barang"
				+ " — atur di master Kelompok Aset, bukan di konfigurasi.");
		if (lblStatus != null) {
			lblStatus.setValue(sb.toString());
		}
	}

	/** Hitung pratinjau HPP yang akan diposting untuk periode terpilih. */
	private void hitungPreview() {
		totalCogs = 0.0;
		debitPerAkun.clear();
		kreditPerAkun.clear();
		akunNama.clear();
		belumDipetakan.clear();
		if (previewBox != null) {
			previewBox.getChildren().clear();
		}

		Date mulai = dpMulai == null ? null : dpMulai.getValue();
		Date sampai = dpSampai == null ? null : dpSampai.getValue();
		if (mulai == null || sampai == null) {
			previewBox.appendChild(DashboardUiKit.html(
					"<div style='font-size:12px;color:#64748b;padding:8px;'>Pilih periode lalu klik Tampilkan.</div>"));
			return;
		}

		SatuanKerja satker = satkerKantin();
		String mStr = Common.databaseDateFormat.get().format(mulai);
		String sStr = Common.databaseDateFormat.get().format(sampai);

		@SuppressWarnings("unchecked")
		List<Object[]> rows = HibernateUtil.currentSession().createSQLQuery(
				"SELECT pr.master_asset, COALESCE(SUM(pb.qty*COALESCE(pr.hargabeli,0)),0) "
						+ "FROM koperasi.pembelian pb INNER JOIN koperasi.produk pr ON pr.id = pb.produk "
						+ "WHERE pr.master_asset IS NOT NULL AND (pb.aktif = true) "
						+ "AND date(pb.waktu) BETWEEN date('" + mStr + "') AND date('" + sStr + "') "
						+ "GROUP BY pr.master_asset").list();

		for (Object[] r : rows) {
			if (r[0] == null) {
				continue;
			}
			Long masterAssetId = ((Number) r[0]).longValue();
			double nilai = r[1] == null ? 0 : ((Number) r[1]).doubleValue();
			if (nilai <= 0) {
				continue;
			}
			MasterAsset ma = (MasterAsset) HibernateUtil.currentSession().get(MasterAsset.class, masterAssetId);
			if (ma == null) {
				continue;
			}
			KelompokAsset kelompok = ma.getKelompokAsset();
			Akun akunHpp = null;
			Akun akunPersediaan = null;
			try {
				if (kelompok != null && kelompok.getAkunBebanPokokPenjualan() != null) {
					akunHpp = AssetUtil.ambilDataAkun(kelompok.getAkunBebanPokokPenjualan(), satker);
				}
			} catch (Exception ex) {
				akunHpp = null;
			}
			try {
				if (ma.getAkunTransaksi() != null && !ma.getAkunTransaksi().trim().isEmpty()) {
					akunPersediaan = AssetUtil.ambilDataAkun(ma.getAkunTransaksi(), satker);
				}
				if (akunPersediaan == null && kelompok != null) {
					akunPersediaan = AssetUtil.ambilDataAkun(kelompok.getAkunTransaksi(), satker);
				}
			} catch (Exception ex) {
				akunPersediaan = null;
			}
			if (akunHpp == null || akunPersediaan == null) {
				belumDipetakan.add(ma.getNama());
				continue;
			}
			tambah(debitPerAkun, akunHpp, nilai);
			tambah(kreditPerAkun, akunPersediaan, nilai);
			akunNama.put(akunHpp.getId(), namaAkun(akunHpp));
			akunNama.put(akunPersediaan.getId(), namaAkun(akunPersediaan));
			totalCogs += nilai;
		}

		StringBuilder sb = new StringBuilder();
		sb.append("<div style='font-size:13px;font-weight:800;color:#0f172a;margin:6px 0;'>Pratinjau Jurnal HPP — ")
				.append(Common.dateFormat.get().format(mulai)).append(" s.d ").append(Common.dateFormat.get().format(sampai))
				.append("</div>");
		if (debitPerAkun.isEmpty()) {
			sb.append("<div style='font-size:12px;color:#64748b;padding:8px;'>Tidak ada HPP penjualan barang tertaut pada periode ini.</div>");
		} else {
			sb.append("<table style='width:100%;border-collapse:collapse;font-size:12px;'>");
			sb.append("<thead><tr style='background:#f1f5f9;'><th style='text-align:left;padding:6px;'>Akun</th>"
					+ "<th style='text-align:left;padding:6px;'>Posisi</th><th style='text-align:right;padding:6px;'>Nominal</th></tr></thead><tbody>");
			for (Map.Entry<Long, Double> en : debitPerAkun.entrySet()) {
				sb.append("<tr><td style='padding:6px;'>").append(DashboardUiKit.esc(akunNama.get(en.getKey())))
						.append("</td><td style='padding:6px;color:#16a34a;font-weight:700;'>DEBIT (HPP)</td>")
						.append("<td style='text-align:right;padding:6px;font-weight:700;'>Rp ")
						.append(DashboardUiKit.money(en.getValue())).append("</td></tr>");
			}
			for (Map.Entry<Long, Double> en : kreditPerAkun.entrySet()) {
				sb.append("<tr><td style='padding:6px;'>").append(DashboardUiKit.esc(akunNama.get(en.getKey())))
						.append("</td><td style='padding:6px;color:#dc2626;'>Kredit (Persediaan)</td>")
						.append("<td style='text-align:right;padding:6px;'>Rp ")
						.append(DashboardUiKit.money(en.getValue())).append("</td></tr>");
			}
			sb.append("</tbody><tfoot><tr style='border-top:2px solid #e2e8f0;font-weight:800;'>"
					+ "<td style='padding:6px;' colspan='2'>TOTAL</td><td style='text-align:right;padding:6px;'>Rp ")
					.append(DashboardUiKit.money(totalCogs)).append("</td></tr></tfoot></table>");
		}
		if (!belumDipetakan.isEmpty()) {
			sb.append("<div style='font-size:12px;color:#b45309;margin-top:8px;'>⚠ ").append(belumDipetakan.size())
					.append(" barang persediaan belum lengkap akunnya di Kelompok Aset (akun HPP/persediaan) — tidak diikutkan: ")
					.append(DashboardUiKit.esc(ringkas(belumDipetakan))).append("</div>");
		}
		previewBox.appendChild(DashboardUiKit.html(sb.toString()));
	}

	private void onPosting() throws Exception {
		Date mulai = dpMulai == null ? null : dpMulai.getValue();
		Date sampai = dpSampai == null ? null : dpSampai.getValue();
		if (mulai == null || sampai == null || mulai.after(sampai)) {
			MyMessageboxConfig.show("Mohon maaf, periode yang dipilih tidak valid. Langkah yang dapat dilakukan: (1) pastikan tanggal Mulai tidak lebih besar dari tanggal Sampai; (2) perbaiki rentang tanggal; (3) ulangi proses posting HPP.", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}
		Date lastEnd = lastPostedEnd();
		if (lastEnd != null && !mulai.after(lastEnd)) {
			MyMessageboxConfig.show("Periode tumpang tindih dengan yang sudah diposting (terakhir s.d "
					+ Common.dateFormat.get().format(lastEnd) + "). 'Mulai' harus setelah tanggal itu.", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}

		hitungPreview();
		if (totalCogs <= 0 || debitPerAkun.isEmpty()) {
			MyMessageboxConfig.show("Tidak ada HPP untuk diposting pada periode ini. Langkah yang dapat dilakukan: (1) pastikan terdapat transaksi penjualan pada periode yang dipilih; (2) periksa apakah resep/produk sudah memiliki harga pokok; (3) coba perluas rentang periode.", "Informasi",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		}
		if (!belumDipetakan.isEmpty()) {
			MyMessageboxConfig.show("Ada " + belumDipetakan.size()
					+ " barang persediaan tanpa akun lengkap di Kelompok Aset (HPP/persediaan). Lengkapi dulu agar jurnal seimbang.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}

		org.hibernate.Session session = HibernateUtil.currentSession();

		List<Akun> akunDebetList = new ArrayList<Akun>();
		List<Double> nilaiDebetList = new ArrayList<Double>();
		for (Map.Entry<Long, Double> en : debitPerAkun.entrySet()) {
			Akun ak = (Akun) ConstantValues.ambil(Akun.class.getName(), en.getKey());
			if (ak != null) {
				akunDebetList.add(ak);
				nilaiDebetList.add(en.getValue());
			}
		}
		List<Akun> akunKreditList = new ArrayList<Akun>();
		List<Double> nilaiKreditList = new ArrayList<Double>();
		for (Map.Entry<Long, Double> en : kreditPerAkun.entrySet()) {
			Akun ak = (Akun) ConstantValues.ambil(Akun.class.getName(), en.getKey());
			if (ak != null) {
				akunKreditList.add(ak);
				nilaiKreditList.add(en.getValue());
			}
		}
		if (akunDebetList.isEmpty() || akunKreditList.isEmpty()) {
			MyMessageboxConfig.show("Mohon maaf, akun HPP/persediaan tidak dapat ditemukan. Langkah yang dapat dilakukan: (1) pastikan akun HPP dan persediaan sudah dikonfigurasi di Chart of Account; (2) hubungi Administrator untuk verifikasi pemetaan akun; (3) ulangi proses posting.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		String ket = "HPP Penjualan Kantin " + Common.dateFormat.get().format(mulai) + " s.d "
				+ Common.dateFormat.get().format(sampai);

		PostingHistory ph = new PostingHistory(JENIS);
		ph.setTanggal(sampai);
		ph.setTbmuser(Common.getCurrentUser());
		ph.setKeterangan(ket);

		// ATOMIK: simpan PostingHistory DAN jurnal dalam SATU transaksi. Commit hanya bila jurnal
		// benar-benar tersimpan (ok). Bila jurnal gagal (exception) ATAU dibatalkan periode closing
		// (ok=false), transaksi di-rollback sehingga PostingHistory TIDAK ikut tersimpan — mencegah
		// guard idempoten (lastPostedEnd) memblokir posting ulang periode itu secara permanen.
		boolean ok;
		session.getTransaction().begin();
		try {
			session.save(ph);
			ok = CommonAkunting.saveTransaksi(akunDebetList.toArray(new Akun[] {}),
					akunKreditList.toArray(new Akun[] {}), null, null, ph, true, ket, sampai,
					nilaiDebetList.toArray(new Double[] {}), nilaiKreditList.toArray(new Double[] {}),
					Double.valueOf(0.0), null, satkerKantin(), session);
			if (ok) {
				session.getTransaction().commit();
			} else {
				session.getTransaction().rollback();
			}
		} catch (Exception ex) {
			try {
				session.getTransaction().rollback();
			} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/koperasi/PostingHppKantinAction.java:366");
			}
			Common.tampilErrorJikaAdmin(ex);
			MyMessageboxConfig.show("Gagal memposting jurnal: " + ex.getMessage(), "Gagal", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		if (ok) {
			MyMessageboxConfig.show("Jurnal HPP berhasil diposting (Rp " + DashboardUiKit.money(totalCogs) + ").",
					"Berhasil", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			refreshStatus();
			java.util.Calendar c = java.util.Calendar.getInstance();
			c.setTime(sampai);
			c.add(java.util.Calendar.DAY_OF_MONTH, 1);
			dpMulai.setValue(c.getTime());
			hitungPreview();
		} else {
			MyMessageboxConfig.show("Posting dibatalkan (kemungkinan tanggal melewati periode closing).", "Informasi",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		}
	}

	// ---------------- util ----------------

	private void tambah(Map<Long, Double> map, Akun akun, double nilai) {
		Long key = akun.getId();
		Double prev = map.get(key);
		map.put(key, Double.valueOf((prev == null ? 0.0 : prev.doubleValue()) + nilai));
	}

	private SatuanKerja satkerKantin() {
		try {
			String v = Common.getKonfigurasi("satuan_kerja_kantin", "").getNilai();
			if (v == null || v.trim().isEmpty()) {
				return null;
			}
			return (SatuanKerja) ConstantValues.ambil(SatuanKerja.class.getName(), Long.valueOf(Long.parseLong(v.trim())));
		} catch (Exception e) {
			return null;
		}
	}

	private Date lastPostedEnd() {
		return lastPostedEnd(HibernateUtil.currentSession());
	}

	/**
	 * Tanggal akhir batch HPP terakhir yang sudah diposting (atau {@code null} bila belum pernah).
	 * Static &amp; menerima {@code session} eksplisit supaya bisa dipanggil dari luar dengan sesi
	 * dedikasi sendiri (lihat {@link #hitungDraftPending} dan pemanggil di
	 * {@code DrafJurnalAction} yang menjalankan tiap jenis jurnal di thread/session terpisah).
	 */
	public static Date lastPostedEnd(Session session) {
		try {
			return (Date) session.createCriteria(PostingHistory.class)
					.add(Restrictions.eq("jenis", JENIS)).setProjection(Projections.max("tanggal")).uniqueResult();
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Jumlah barang persediaan (master_asset) yang HPP penjualannya BELUM diposting -- dari hari
	 * setelah batch terakhir ({@link #lastPostedEnd}) sampai hari ini. Dipakai baris "Posting HPP"
	 * di dasbor Draft Jurnal ({@code DrafJurnalAction}) sebagai angka "Draft", tanpa duplikasi query
	 * (pola grouping sama persis dgn {@link #hitungPreview}).
	 */
	public static int hitungDraftPending(Session session) {
		try {
			Date lastEnd = lastPostedEnd(session);
			java.util.Calendar c = java.util.Calendar.getInstance();
			if (lastEnd != null) {
				c.setTime(lastEnd);
				c.add(java.util.Calendar.DAY_OF_MONTH, 1);
			} else {
				c.set(java.util.Calendar.DAY_OF_MONTH, 1);
			}
			String mStr = Common.databaseDateFormat.get().format(c.getTime());
			String sStr = Common.databaseDateFormat.get().format(WaktuUtil.getDate());
			@SuppressWarnings("unchecked")
			List<Object[]> rows = session.createSQLQuery(
					"SELECT pr.master_asset, COALESCE(SUM(pb.qty*COALESCE(pr.hargabeli,0)),0) "
							+ "FROM koperasi.pembelian pb INNER JOIN koperasi.produk pr ON pr.id = pb.produk "
							+ "WHERE pr.master_asset IS NOT NULL AND (pb.aktif = true) "
							+ "AND date(pb.waktu) BETWEEN date('" + mStr + "') AND date('" + sStr + "') "
							+ "GROUP BY pr.master_asset").list();
			int n = 0;
			for (Object[] r : rows) {
				if (r[0] == null) {
					continue;
				}
				double nilai = r[1] == null ? 0 : ((Number) r[1]).doubleValue();
				if (nilai > 0) {
					n++;
				}
			}
			return n;
		} catch (Exception e) {
			return 0;
		}
	}

	/** Jumlah batch HPP yang sudah pernah diposting sepanjang waktu (baris {@code PostingHistory} jenis HPP). */
	public static int hitungTerposting(Session session) {
		try {
			Number n = (Number) session.createCriteria(PostingHistory.class)
					.add(Restrictions.eq("jenis", JENIS)).setProjection(Projections.rowCount()).uniqueResult();
			return n == null ? 0 : n.intValue();
		} catch (Exception e) {
			return 0;
		}
	}

	private String namaAkun(Akun a) {
		if (a == null) {
			return "";
		}
		try {
			String kode = "";
			String nama = "";
			try {
				kode = a.getKode() == null ? "" : a.getKode();
			} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/koperasi/PostingHppKantinAction.java:427");
			}
			try {
				nama = a.getNama() == null ? "" : a.getNama();
			} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/koperasi/PostingHppKantinAction.java:431");
			}
			return (kode.isEmpty() ? "" : kode + " - ") + nama;
		} catch (Exception e) {
			return a.toString();
		}
	}

	private String ringkas(List<String> list) {
		StringBuilder sb = new StringBuilder();
		int n = Math.min(list.size(), 5);
		for (int i = 0; i < n; i++) {
			sb.append(i == 0 ? "" : ", ").append(list.get(i));
		}
		if (list.size() > n) {
			sb.append(", …");
		}
		return sb.toString();
	}
}
