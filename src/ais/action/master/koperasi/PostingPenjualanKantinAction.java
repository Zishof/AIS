package ais.action.master.koperasi;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
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
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.PostingHistory;
import ais.database.model.koperasi.CaraPembayaranKoperasi;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.DashboardUiKit;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.WaktuUtil;

/**
 * <h3>Posting Penjualan Kantin — pendapatan penjualan ke jurnal (periodik/batch)</h3>
 *
 * <p>Melengkapi double-entry penjualan kantin: {@link PostingHppKantinAction} mencatat sisi BIAYA
 * (Dr HPP / Cr Persediaan); kelas ini mencatat sisi PENDAPATAN untuk periode terpilih, dibaca dari
 * header penjualan {@code koperasi.pembelian_anggota_koperasi} (memakai {@code tanggal_pembayaran}).
 * Satu jurnal batch:</p>
 * <ul>
 *   <li><b>Debit</b> = akun Kas/Bank/Piutang tiap <b>Cara Pembayaran Koperasi</b>
 *       ({@code CaraPembayaranKoperasi.getAkun()}), dipecah per slot bayar 1..5 sesuai nominalnya.</li>
 *   <li><b>Kredit</b> = akun <b>Pendapatan Penjualan</b> &amp; <b>PPN Keluaran</b> yang ditetapkan
 *       <b>per Jenis Produk</b> ({@code JenisProduk.akunPendapatan} / {@code akunPpnKeluaran}).
 *       Nilai penjualan dihitung per baris lalu dikelompokkan per jenis produk; total tagihan
 *       ({@code total_biaya}) &amp; PPN ({@code pajak}) header dialokasikan proporsional ke tiap jenis
 *       produk (sisa dibebankan ke baris terakhir agar Debit = Kredit persis).</li>
 * </ul>
 *
 * <h3>Aman by design</h3>
 * <ul>
 *   <li><b>Idempoten:</b> hanya header {@code posting_history IS NULL}; setelah diposting
 *       {@code PembelianAnggotaKoperasi.posting_history} diisi → tak terposting ganda.</li>
 *   <li><b>Seimbang:</b> header ditolak (tak diikutkan) bila ada slot bayar tanpa akun, atau ada
 *       jenis produk tanpa akun Pendapatan (atau PPN saat ada pajak).</li>
 *   <li><b>Sadar closing</b> (lewat {@code CommonAkunting.saveTransaksi}).</li>
 * </ul>
 */
public class PostingPenjualanKantinAction extends GenericAutowireComposer {

	private static final long serialVersionUID = 1L;

	/** Jenis posting history khusus penjualan kantin. */
	public static final String JENIS = "Penjualan Kantin";

	private static final double EPS = 0.0000001;

	private Div postingPenjualanHost;

	private Datebox dpMulai;
	private Datebox dpSampai;
	private Div previewBox;
	private Label lblStatus;

	private double totalNet = 0.0; // Σ pendapatan bersih (kredit Pendapatan)
	private double totalPpn = 0.0; // Σ PPN Keluaran (kredit PPN)
	private double totalDebit = 0.0; // Σ nominal bayar (debit Kas/Bank)
	private final Map<Long, Double> debitKasPerAkun = new LinkedHashMap<Long, Double>();
	private final Map<Long, Double> pendapatanPerAkun = new LinkedHashMap<Long, Double>();
	private final Map<Long, Double> ppnPerAkun = new LinkedHashMap<Long, Double>();
	private final Map<Long, String> akunNama = new LinkedHashMap<Long, String>();
	private final Map<Long, Double> nominalPerMetode = new LinkedHashMap<Long, Double>();
	private final Map<Long, String> metodeNama = new LinkedHashMap<Long, String>();
	private final Map<Long, String> metodeAkun = new LinkedHashMap<Long, String>();
	private final Map<Long, double[]> kategoriNilai = new LinkedHashMap<Long, double[]>(); // jpId → [net, ppn]
	private final Map<Long, String> kategoriNama = new LinkedHashMap<Long, String>();
	private final List<String> belumDipetakan = new ArrayList<String>();
	private final List<Long> headerTerposting = new ArrayList<Long>();
	/**
	 * Draf jurnal PER TRANSAKSI (bukan agregat) -- dipakai layar "analisis sebelum
	 * posting" ala Posting Cicilan Mahasiswa: tiap faktur tampil beserta baris
	 * akun/debit/kredit-nya sendiri, statusnya siap atau belum, dan alasannya bila
	 * pemetaan akun kurang. Diisi di hitungPreview(), dikirim lewat prosesApi().
	 */
	private final List<org.json.JSONObject> rincianDraft = new ArrayList<org.json.JSONObject>();

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
		if (postingPenjualanHost == null) {
			return;
		}
		DashboardUiKit.attachIntro(comp, "Posting Penjualan Kantin",
				"Mencatat pendapatan penjualan kantin ke jurnal per periode. Akun Kas/Bank diambil dari Cara "
						+ "Pembayaran Koperasi; akun Pendapatan & PPN Keluaran diambil dari master Jenis Produk.");

		Vlayout box = new Vlayout();
		box.setWidth("100%");
		box.setStyle("gap:8px;padding:6px;");
		box.setParent(postingPenjualanHost);

		Hlayout filter = new Hlayout();
		filter.setStyle("gap:8px;align-items:center;flex-wrap:wrap;");
		filter.setParent(box);
		filter.appendChild(new Label(Common.getBahasaConfig("Periode penjualan:")));
		dpMulai = new Datebox();
		dpMulai.setFormat("dd-MM-yyyy");
		dpMulai.setWidth("130px");
		filter.appendChild(dpMulai);
		filter.appendChild(new Label(Common.getBahasaConfig("s.d")));
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
		Button btnPosting = new Button("Posting Jurnal Penjualan");
		btnPosting.setImage("/img/save.gif");
		btnPosting.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				onPosting();
			}
		});
		aksi.appendChild(btnPosting);
		// "Posting Semua yang Siap" -- memakai mode PER transaksi (pola Posting Cicilan
		// Mahasiswa): setiap baris siap dijurnal sendiri-sendiri, sehingga baris yang
		// pemetaan akunnya belum lengkap dilewati TANPA memblokir yang lain. Berbeda
		// dari tombol di sebelah kiri yang memakai mode agregat satu-jurnal-per-periode.
		Button btnPostingSiap = new Button("Posting Semua yang Siap");
		btnPostingSiap.setImage("/img/svg/check2-circle.svg");
		btnPostingSiap.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				JSONObject ringkas = postingPerTransaksi(java.util.Collections.<Long>emptyList());
				MyMessageboxConfig.show(
						"Diposting: " + ringkas.optInt("diposting", 0) + " transaksi; dilewati (belum siap): "
								+ ringkas.optInt("dilewati", 0) + "; gagal: " + ringkas.optInt("gagal", 0)
								+ (ringkas.optString("pesan", "").isEmpty() ? ""
										: ". " + ringkas.optString("pesan", "")),
						"Hasil Posting", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				hitungPreview();
			}
		});
		aksi.appendChild(btnPostingSiap);

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
		sb.append(lastEnd == null ? "Belum pernah ada posting Penjualan kantin."
				: ("Terakhir diposting s.d " + Common.dateFormat.get().format(lastEnd) + "."));
		sb.append("  Akun Kas/Bank dari Cara Pembayaran Koperasi; akun Pendapatan & PPN Keluaran dari master"
				+ " Jenis Produk. Transaksi yang akunnya belum lengkap tidak diikutkan.");
		if (lblStatus != null) {
			lblStatus.setValue(sb.toString());
		}
	}

	// ── Baris kategori penjualan per header (dari query level baris) ──────────────────────────
	private static final class KategoriBaris {
		Long jpId;
		String jpNama;
		Long akunPendapatanId;
		Long akunPpnId;
		double lineTotal;
	}

	/** Hitung pratinjau pendapatan untuk periode terpilih. */
	@SuppressWarnings("unchecked")
	private void hitungPreview() {
		totalNet = 0.0;
		totalPpn = 0.0;
		totalDebit = 0.0;
		debitKasPerAkun.clear();
		pendapatanPerAkun.clear();
		ppnPerAkun.clear();
		akunNama.clear();
		nominalPerMetode.clear();
		metodeNama.clear();
		metodeAkun.clear();
		kategoriNilai.clear();
		kategoriNama.clear();
		belumDipetakan.clear();
		headerTerposting.clear();
		rincianDraft.clear();
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

		Session session = HibernateUtil.currentSession();
		String mStr = Common.databaseDateFormat.get().format(mulai);
		String sStr = Common.databaseDateFormat.get().format(sampai);
		String periode = "h.posting_history IS NULL AND COALESCE(h.total_biaya,0) > 0 "
				+ "AND date(h.tanggal_pembayaran) BETWEEN date('" + mStr + "') AND date('" + sStr + "') "
				+ "AND EXISTS (SELECT 1 FROM koperasi.pembelian pb WHERE pb.pembelian_anggota_koperasi = h.id AND pb.aktif = true)";

		// (Q1) Header: total, pajak, slot bayar 1..5.
		List<Object[]> hdr = session.createSQLQuery(
				"SELECT h.id, COALESCE(h.total_biaya,0), COALESCE(h.pajak,0), "
						+ "h.cara_pembayaran_koperasi, h.cara_pembayaran_koperasi_2, h.cara_pembayaran_koperasi_3, "
						+ "h.cara_pembayaran_koperasi_4, h.cara_pembayaran_koperasi_5, "
						+ "COALESCE(h.nominal_bayar_2,0), COALESCE(h.nominal_bayar_3,0), COALESCE(h.nominal_bayar_4,0), "
						+ "COALESCE(h.nominal_bayar_5,0) "
						+ "FROM koperasi.pembelian_anggota_koperasi h WHERE " + periode + " ORDER BY h.id").list();

		// (Q2) Baris per jenis produk: nilai penjualan (OMZET) per (header, jenis produk).
		List<Object[]> cats = session.createSQLQuery(
				"SELECT h.id, pr.jenis_produk, jp.nama, jp.akun_pendapatan, jp.akun_ppn_keluaran, "
						+ "COALESCE(SUM(COALESCE(pb.hargasatuan, pr.hargajual, 0)*COALESCE(pb.qty,0) - COALESCE(pb.diskon,0)),0) "
						+ "FROM koperasi.pembelian_anggota_koperasi h "
						+ "JOIN koperasi.pembelian pb ON pb.pembelian_anggota_koperasi = h.id AND pb.aktif = true "
						+ "JOIN koperasi.produk pr ON pr.id = pb.produk "
						+ "LEFT JOIN koperasi.jenis_produk jp ON jp.id = pr.jenis_produk "
						+ "WHERE " + periode
						+ " GROUP BY h.id, pr.jenis_produk, jp.nama, jp.akun_pendapatan, jp.akun_ppn_keluaran "
						+ "ORDER BY h.id").list();

		Map<Long, List<KategoriBaris>> perHeaderCats = new LinkedHashMap<Long, List<KategoriBaris>>();
		for (Object[] r : cats) {
			Long hid = ((Number) r[0]).longValue();
			KategoriBaris kb = new KategoriBaris();
			kb.jpId = asLong(r[1]);
			kb.jpNama = r[2] == null ? "(tanpa jenis produk)" : r[2].toString();
			kb.akunPendapatanId = asLong(r[3]);
			kb.akunPpnId = asLong(r[4]);
			kb.lineTotal = num(r[5]);
			List<KategoriBaris> list = perHeaderCats.get(hid);
			if (list == null) {
				list = new ArrayList<KategoriBaris>();
				perHeaderCats.put(hid, list);
			}
			list.add(kb);
		}

		Map<Long, CaraPembayaranKoperasi> cacheCp = new HashMap<Long, CaraPembayaranKoperasi>();

		for (Object[] r : hdr) {
			Long headerId = ((Number) r[0]).longValue();
			double total = num(r[1]);
			double pajak = num(r[2]);
			Long[] mids = new Long[] { asLong(r[3]), asLong(r[4]), asLong(r[5]), asLong(r[6]), asLong(r[7]) };
			double nb2 = num(r[8]), nb3 = num(r[9]), nb4 = num(r[10]), nb5 = num(r[11]);
			double nb1 = total - (nb2 + nb3 + nb4 + nb5);
			if (nb1 < 0) {
				nb1 = 0;
			}
			double[] amts = new double[] { nb1, nb2, nb3, nb4, nb5 };

			List<KategoriBaris> kbs = perHeaderCats.get(headerId);
			double headerLineTotal = 0;
			if (kbs != null) {
				for (KategoriBaris kb : kbs) {
					headerLineTotal += kb.lineTotal;
				}
			}
			if (kbs == null || headerLineTotal <= EPS) {
				continue; // tak ada baris bernilai untuk dialokasi
			}

			// --- Validasi + susun DEBIT (kas/bank per slot) ---
			Map<Long, Double> debitHeader = new LinkedHashMap<Long, Double>();
			boolean valid = true;
			String alasan = null;
			for (int i = 0; i < 5 && valid; i++) {
				double amt = amts[i];
				if (amt <= EPS) {
					continue;
				}
				Long mid = mids[i];
				CaraPembayaranKoperasi cp = null;
				if (mid != null) {
					cp = cacheCp.get(mid);
					if (cp == null) {
						cp = (CaraPembayaranKoperasi) session.get(CaraPembayaranKoperasi.class, mid);
						if (cp != null) {
							cacheCp.put(mid, cp);
						}
					}
				}
				Akun ak = cp == null ? null : cp.getAkun();
				if (ak == null || ak.getId() == null) {
					valid = false;
					alasan = cp == null ? "cara pembayaran tak dikenal" : (cp.getNama() + " belum punya akun");
					break;
				}
				add(debitHeader, ak.getId(), amt);
				akunNama.put(ak.getId(), namaAkun(ak));
				add(nominalPerMetode, mid, amt);
				metodeNama.put(mid, cp.getNama());
				metodeAkun.put(mid, namaAkun(ak));
			}

			// --- Validasi + alokasi KREDIT (pendapatan + ppn per jenis produk) ---
			Map<Long, Double> pendHeader = new LinkedHashMap<Long, Double>();
			Map<Long, Double> ppnHeader = new LinkedHashMap<Long, Double>();
			Map<Long, double[]> katHeader = new LinkedHashMap<Long, double[]>();
			double sisaShare = total, sisaPpn = pajak;
			for (int i = 0; i < kbs.size() && valid; i++) {
				KategoriBaris kb = kbs.get(i);
				boolean last = (i == kbs.size() - 1);
				double frac = kb.lineTotal / headerLineTotal;
				double catShare = last ? sisaShare : total * frac;
				double catPpn = last ? sisaPpn : pajak * frac;
				sisaShare -= catShare;
				sisaPpn -= catPpn;
				double catNet = catShare - catPpn;

				if (kb.akunPendapatanId == null) {
					valid = false;
					alasan = "Jenis produk \"" + kb.jpNama + "\" belum punya akun Pendapatan";
					break;
				}
				if (catPpn > EPS && kb.akunPpnId == null) {
					valid = false;
					alasan = "Jenis produk \"" + kb.jpNama + "\" belum punya akun PPN Keluaran";
					break;
				}
				add(pendHeader, kb.akunPendapatanId, catNet);
				if (catPpn > EPS) {
					add(ppnHeader, kb.akunPpnId, catPpn);
				}
				double[] kv = katHeader.get(kb.jpId == null ? Long.valueOf(-1L) : kb.jpId);
				if (kv == null) {
					kv = new double[2];
					katHeader.put(kb.jpId == null ? Long.valueOf(-1L) : kb.jpId, kv);
					kategoriNama.put(kb.jpId == null ? Long.valueOf(-1L) : kb.jpId, kb.jpNama);
				}
				kv[0] += catNet;
				kv[1] += catPpn;
			}

			if (!valid) {
				belumDipetakan.add("Faktur #" + headerId + " — " + alasan);
				rincianDraft.add(barisDraft(headerId, total, alasan, null, null, null));
				continue;
			}

			rincianDraft.add(barisDraft(headerId, total, null, debitHeader, pendHeader, ppnHeader));
			// --- Commit ke akumulator global ---
			for (Map.Entry<Long, Double> en : debitHeader.entrySet()) {
				add(debitKasPerAkun, en.getKey(), en.getValue());
				totalDebit += en.getValue();
			}
			for (Map.Entry<Long, Double> en : pendHeader.entrySet()) {
				add(pendapatanPerAkun, en.getKey(), en.getValue());
				akunNama.put(en.getKey(), namaAkunById(en.getKey()));
				totalNet += en.getValue();
			}
			for (Map.Entry<Long, Double> en : ppnHeader.entrySet()) {
				add(ppnPerAkun, en.getKey(), en.getValue());
				akunNama.put(en.getKey(), namaAkunById(en.getKey()));
				totalPpn += en.getValue();
			}
			for (Map.Entry<Long, double[]> en : katHeader.entrySet()) {
				double[] g = kategoriNilai.get(en.getKey());
				if (g == null) {
					g = new double[2];
					kategoriNilai.put(en.getKey(), g);
				}
				g[0] += en.getValue()[0];
				g[1] += en.getValue()[1];
			}
			headerTerposting.add(headerId);
		}

		bangunTampilan(mulai, sampai);
	}

	private void bangunTampilan(Date mulai, Date sampai) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='font-size:13px;font-weight:800;color:#0f172a;margin:6px 0;'>Pratinjau Jurnal Penjualan — ")
				.append(Common.dateFormat.get().format(mulai)).append(" s.d ").append(Common.dateFormat.get().format(sampai))
				.append("</div>");

		if (headerTerposting.isEmpty() && belumDipetakan.isEmpty()) {
			sb.append("<div style='font-size:12px;color:#64748b;padding:8px;'>Tidak ada transaksi penjualan yang belum "
					+ "diposting pada periode ini (dibaca dari Pembelian Anggota Koperasi).</div>");
			previewBox.appendChild(DashboardUiKit.html(sb.toString()));
		tampilkanGridDraf();
			return;
		}

		// Rincian pendapatan per jenis produk.
		if (!kategoriNilai.isEmpty()) {
			sb.append("<div style='font-size:12px;font-weight:700;color:#334155;margin:8px 0 4px;'>Pendapatan per Jenis Produk</div>");
			sb.append("<div style='overflow-x:auto;'><table style='width:100%;border-collapse:collapse;font-size:12px;'>");
			sb.append("<thead><tr style='background:#f1f5f9;'>"
					+ "<th style='text-align:left;padding:6px;'>Jenis Produk</th>"
					+ "<th style='text-align:right;padding:6px;'>Pendapatan (Net)</th>"
					+ "<th style='text-align:right;padding:6px;'>PPN Keluaran</th></tr></thead><tbody>");
			for (Map.Entry<Long, double[]> en : kategoriNilai.entrySet()) {
				sb.append("<tr style='border-bottom:1px solid #eef2f7;'><td style='padding:6px;'>")
						.append(DashboardUiKit.esc(kategoriNama.get(en.getKey())))
						.append("</td><td style='text-align:right;padding:6px;font-weight:700;'>Rp ")
						.append(DashboardUiKit.money(en.getValue()[0]))
						.append("</td><td style='text-align:right;padding:6px;'>Rp ")
						.append(DashboardUiKit.money(en.getValue()[1])).append("</td></tr>");
			}
			sb.append("</tbody></table></div>");
		}

		// Penerimaan per cara pembayaran (debit).
		if (!nominalPerMetode.isEmpty()) {
			sb.append("<div style='font-size:12px;font-weight:700;color:#334155;margin:12px 0 4px;'>Penerimaan per Cara Pembayaran</div>");
			sb.append("<div style='overflow-x:auto;'><table style='width:100%;border-collapse:collapse;font-size:12px;'>");
			sb.append("<thead><tr style='background:#f1f5f9;'>"
					+ "<th style='text-align:left;padding:6px;'>Cara Pembayaran</th>"
					+ "<th style='text-align:left;padding:6px;'>Akun Kas/Bank/Piutang</th>"
					+ "<th style='text-align:right;padding:6px;'>Nominal</th></tr></thead><tbody>");
			for (Map.Entry<Long, Double> en : nominalPerMetode.entrySet()) {
				sb.append("<tr style='border-bottom:1px solid #eef2f7;'><td style='padding:6px;'>")
						.append(DashboardUiKit.esc(metodeNama.get(en.getKey())))
						.append("</td><td style='padding:6px;font-size:11px;'>")
						.append(DashboardUiKit.esc(metodeAkun.get(en.getKey())))
						.append("</td><td style='text-align:right;padding:6px;font-weight:700;'>Rp ")
						.append(DashboardUiKit.money(en.getValue())).append("</td></tr>");
			}
			sb.append("</tbody></table></div>");
		}

		// Ringkasan jurnal (yang akan diposting).
		sb.append("<div style='font-size:12px;font-weight:700;color:#334155;margin:12px 0 4px;'>Ringkasan Jurnal (akan diposting)</div>");
		sb.append("<table style='width:100%;border-collapse:collapse;font-size:12px;'>");
		sb.append("<thead><tr style='background:#f1f5f9;'><th style='text-align:left;padding:6px;'>Akun</th>"
				+ "<th style='text-align:left;padding:6px;'>Posisi</th><th style='text-align:right;padding:6px;'>Nominal</th></tr></thead><tbody>");
		for (Map.Entry<Long, Double> en : debitKasPerAkun.entrySet()) {
			barisJurnal(sb, akunNama.get(en.getKey()), "DEBIT (Kas/Bank)", "#16a34a", en.getValue(), true);
		}
		for (Map.Entry<Long, Double> en : pendapatanPerAkun.entrySet()) {
			barisJurnal(sb, akunNama.get(en.getKey()), "Kredit (Pendapatan)", "#dc2626", en.getValue(), false);
		}
		for (Map.Entry<Long, Double> en : ppnPerAkun.entrySet()) {
			barisJurnal(sb, akunNama.get(en.getKey()), "Kredit (PPN Keluaran)", "#dc2626", en.getValue(), false);
		}
		sb.append("</tbody><tfoot><tr style='border-top:2px solid #e2e8f0;font-weight:800;'>"
				+ "<td style='padding:6px;' colspan='2'>TOTAL</td><td style='text-align:right;padding:6px;'>Rp ")
				.append(DashboardUiKit.money(totalDebit)).append("</td></tr></tfoot></table>");

		if (!belumDipetakan.isEmpty()) {
			sb.append("<div style='font-size:12px;color:#b45309;margin-top:8px;'>⚠ ").append(belumDipetakan.size())
					.append(" transaksi tidak diikutkan karena akun belum lengkap: ")
					.append(DashboardUiKit.esc(ringkas(belumDipetakan))).append("</div>");
		}
		previewBox.appendChild(DashboardUiKit.html(sb.toString()));
	}

	private void barisJurnal(StringBuilder sb, String akun, String posisi, String warna, double nilai, boolean tebal) {
		sb.append("<tr><td style='padding:6px;'>").append(DashboardUiKit.esc(akun == null ? "" : akun))
				.append("</td><td style='padding:6px;color:").append(warna).append(tebal ? ";font-weight:700;'>" : ";'>")
				.append(posisi).append("</td><td style='text-align:right;padding:6px;").append(tebal ? "font-weight:700;" : "")
				.append("'>Rp ").append(DashboardUiKit.money(nilai)).append("</td></tr>");
	}

	private void onPosting() throws Exception {
		Date mulai = dpMulai == null ? null : dpMulai.getValue();
		Date sampai = dpSampai == null ? null : dpSampai.getValue();
		if (mulai == null || sampai == null || mulai.after(sampai)) {
			MyMessageboxConfig.show("Mohon maaf, periode yang dipilih tidak valid. Pastikan tanggal Mulai tidak lebih besar dari tanggal Sampai lalu ulangi.", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}

		hitungPreview();
		if (headerTerposting.isEmpty() || totalDebit <= 0) {
			MyMessageboxConfig.show("Tidak ada penjualan untuk diposting pada periode ini. Pastikan ada transaksi kasir pada rentang tanggal, cara pembayaran sudah punya akun, dan Jenis Produk sudah punya akun Pendapatan.", "Informasi",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		}

		Session session = HibernateUtil.currentSession();

		List<Akun> akunDebetList = new ArrayList<Akun>();
		List<Double> nilaiDebetList = new ArrayList<Double>();
		for (Map.Entry<Long, Double> en : debitKasPerAkun.entrySet()) {
			Akun ak = (Akun) ConstantValues.ambil(Akun.class.getName(), en.getKey());
			if (ak != null) {
				akunDebetList.add(ak);
				nilaiDebetList.add(en.getValue());
			}
		}
		List<Akun> akunKreditList = new ArrayList<Akun>();
		List<Double> nilaiKreditList = new ArrayList<Double>();
		for (Map.Entry<Long, Double> en : pendapatanPerAkun.entrySet()) {
			if (en.getValue() <= EPS) {
				continue;
			}
			Akun ak = (Akun) ConstantValues.ambil(Akun.class.getName(), en.getKey());
			if (ak != null) {
				akunKreditList.add(ak);
				nilaiKreditList.add(en.getValue());
			}
		}
		for (Map.Entry<Long, Double> en : ppnPerAkun.entrySet()) {
			if (en.getValue() <= EPS) {
				continue;
			}
			Akun ak = (Akun) ConstantValues.ambil(Akun.class.getName(), en.getKey());
			if (ak != null) {
				akunKreditList.add(ak);
				nilaiKreditList.add(en.getValue());
			}
		}
		if (akunDebetList.isEmpty() || akunKreditList.isEmpty()) {
			MyMessageboxConfig.show("Mohon maaf, akun debit/kredit tidak lengkap sehingga jurnal tidak seimbang. Periksa akun Cara Pembayaran & akun Jenis Produk lalu ulangi.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		String ket = "Penjualan Kantin " + Common.dateFormat.get().format(mulai) + " s.d "
				+ Common.dateFormat.get().format(sampai);

		PostingHistory ph = new PostingHistory(JENIS);
		ph.setTanggal(sampai);
		ph.setTbmuser(Common.getCurrentUser());
		ph.setKeterangan(ket);

		boolean ok;
		session.getTransaction().begin();
		try {
			session.save(ph);
			ok = CommonAkunting.saveTransaksi(akunDebetList.toArray(new Akun[] {}),
					akunKreditList.toArray(new Akun[] {}), null, null, ph, true, ket, sampai,
					nilaiDebetList.toArray(new Double[] {}), nilaiKreditList.toArray(new Double[] {}),
					Double.valueOf(0.0), null, satkerKantin(), session);
			if (ok) {
				tandaiHeaderTerposting(session, ph);
				session.getTransaction().commit();
			} else {
				session.getTransaction().rollback();
			}
		} catch (Exception ex) {
			try {
				session.getTransaction().rollback();
			} catch (Exception ignore) {
				ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) PostingPenjualanKantinAction.onPosting");
			}
			Common.tampilErrorJikaAdmin(ex);
			MyMessageboxConfig.show("Gagal memposting jurnal: " + ex.getMessage(), "Gagal", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		if (ok) {
			MyMessageboxConfig.show("Jurnal Penjualan berhasil diposting (Rp " + DashboardUiKit.money(totalDebit) + ").",
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

	/** Adapter headless POS; memakai kalkulasi dan transaksi jurnal yang sama dengan layar ZK. */
	/**
	 * Satu baris draf jurnal untuk SATU faktur: identitas, nilai, status siap/belum
	 * beserta alasannya, dan baris akun debit/kredit-nya. Bentuk ini yang dirender
	 * layar analisis sebelum posting (Desktop/Android/ZK) sehingga pengguna dapat
	 * memeriksa jurnal tiap transaksi, lalu memposting satu per satu.
	 */
	private org.json.JSONObject barisDraft(Long headerId, double total, String alasan,
			Map<Long, Double> debit, Map<Long, Double> pendapatan, Map<Long, Double> ppn) {
		org.json.JSONObject o = new org.json.JSONObject();
		try {
		o.put("id", headerId);
		o.put("ref", "Faktur #" + headerId);
		o.put("nilai", total);
		o.put("siap", alasan == null);
		o.put("alasan", alasan == null ? "" : alasan);
		org.json.JSONArray baris = new org.json.JSONArray();
		if (debit != null) {
			for (Map.Entry<Long, Double> en : debit.entrySet()) {
				baris.put(barisJurnal(en.getKey(), en.getValue().doubleValue(), 0.0));
			}
		}
		if (pendapatan != null) {
			for (Map.Entry<Long, Double> en : pendapatan.entrySet()) {
				baris.put(barisJurnal(en.getKey(), 0.0, en.getValue().doubleValue()));
			}
		}
		if (ppn != null) {
			for (Map.Entry<Long, Double> en : ppn.entrySet()) {
				baris.put(barisJurnal(en.getKey(), 0.0, en.getValue().doubleValue()));
			}
		}
		o.put("jurnal", baris);
		} catch (Exception e) {
			// Draf hanya untuk DITAMPILKAN; kegagalan menyusunnya tidak boleh
			// menggagalkan perhitungan posting itu sendiri (gagal-aman).
			ais.common.ErrorAuditUtil.record(e, "auto-audit PostingPenjualanKantinAction.barisDraft");
		}
		return o;
	}

	private org.json.JSONObject barisJurnal(Long akunId, double debit, double kredit) {
		org.json.JSONObject j = new org.json.JSONObject();
		try {
			j.put("akunId", akunId);
			j.put("akun", namaAkunById(akunId));
			j.put("debit", debit);
			j.put("kredit", kredit);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit PostingPenjualanKantinAction.barisJurnal");
		}
		return j;
	}

	/**
	 * Posting PER TRANSAKSI mengikuti pola layar Posting Cicilan Mahasiswa: tiap faktur
	 * menjadi SATU entri jurnal sendiri (bukan satu jurnal gabungan seperti mode lama),
	 * sehingga pengguna dapat menganalisis lalu memposting satu per satu -- dan satu
	 * transaksi bermasalah tidak lagi memblokir seluruh periode.
	 *
	 * @param idsDipilih daftar id faktur yang hendak diposting; kosong/null = SEMUA
	 *                   faktur yang berstatus siap pada periode ini.
	 * @return ringkasan {@code {diposting, dilewati, gagal, pesan}}.
	 */
	public JSONObject postingPerTransaksi(java.util.Collection<Long> idsDipilih) throws Exception {
		Session session = HibernateUtil.currentSession();
		int diposting = 0, dilewati = 0, gagal = 0;
		StringBuilder pesan = new StringBuilder();
		for (int i = 0; i < rincianDraft.size(); i++) {
			org.json.JSONObject baris = rincianDraft.get(i);
			Long id = baris.isNull("id") ? null : Long.valueOf(baris.get("id").toString());
			if (id == null) {
				continue;
			}
			if (idsDipilih != null && !idsDipilih.isEmpty() && !idsDipilih.contains(id)) {
				continue;
			}
			if (!baris.optBoolean("siap", false)) {
				dilewati++;
				continue;
			}
			try {
				if (postingSatuFaktur(session, id, baris)) {
					diposting++;
				} else {
					gagal++;
					if (pesan.length() < 300) {
						pesan.append(pesan.length() == 0 ? "" : "; ").append("Faktur #").append(id)
								.append(" gagal ditulis ke jurnal");
					}
				}
			} catch (Exception ex) {
				gagal++;
				ais.common.ErrorAuditUtil.record(ex,
						"auto-audit PostingPenjualanKantinAction.postingPerTransaksi faktur " + id);
				if (pesan.length() < 300) {
					pesan.append(pesan.length() == 0 ? "" : "; ").append("Faktur #").append(id).append(": ")
							.append(ex.getMessage());
				}
			}
		}
		JSONObject out = new JSONObject();
		out.put("diposting", diposting);
		out.put("dilewati", dilewati);
		out.put("gagal", gagal);
		out.put("pesan", pesan.toString());
		return out;
	}

	/**
	 * Tulis SATU faktur menjadi satu entri jurnal + tandai fakturnya. Setiap faktur
	 * memakai transaksi database sendiri: kegagalan pada satu faktur tidak membatalkan
	 * faktur lain yang sudah berhasil (pola yang sama dipakai Posting Cicilan Mahasiswa).
	 */
	private boolean postingSatuFaktur(Session session, Long headerId, org.json.JSONObject baris) throws Exception {
		org.json.JSONArray jurnal = baris.optJSONArray("jurnal");
		if (jurnal == null || jurnal.length() == 0) {
			return false;
		}
		List<Akun> akunDebet = new ArrayList<Akun>();
		List<Double> nilaiDebet = new ArrayList<Double>();
		List<Akun> akunKredit = new ArrayList<Akun>();
		List<Double> nilaiKredit = new ArrayList<Double>();
		for (int i = 0; i < jurnal.length(); i++) {
			org.json.JSONObject j = jurnal.getJSONObject(i);
			Long akunId = j.isNull("akunId") ? null : Long.valueOf(j.get("akunId").toString());
			if (akunId == null) {
				continue;
			}
			Akun akun = (Akun) ConstantValues.ambil(Akun.class.getName(), akunId);
			if (akun == null) {
				continue;
			}
			double d = j.optDouble("debit", 0);
			double k = j.optDouble("kredit", 0);
			if (d > EPS) {
				akunDebet.add(akun);
				nilaiDebet.add(Double.valueOf(d));
			}
			if (k > EPS) {
				akunKredit.add(akun);
				nilaiKredit.add(Double.valueOf(k));
			}
		}
		if (akunDebet.isEmpty() || akunKredit.isEmpty()) {
			return false;
		}
		String ket = "Penjualan Kantin Faktur #" + headerId;
		PostingHistory ph = new PostingHistory(JENIS);
		ph.setTanggal(dpSampai == null ? new Date() : dpSampai.getValue());
		ph.setTbmuser(Common.getCurrentUser());
		ph.setKeterangan(ket);
		session.getTransaction().begin();
		try {
			session.save(ph);
			boolean ok = CommonAkunting.saveTransaksi(akunDebet.toArray(new Akun[] {}),
					akunKredit.toArray(new Akun[] {}), null, null, ph, true, ket,
					ph.getTanggal(), nilaiDebet.toArray(new Double[] {}), nilaiKredit.toArray(new Double[] {}),
					Double.valueOf(0.0), null, satkerKantin(), session);
			if (!ok) {
				session.getTransaction().rollback();
				return false;
			}
			session.createSQLQuery("UPDATE koperasi.pembelian_anggota_koperasi SET posting_history = "
					+ ph.getId().longValue() + " WHERE id = " + headerId.longValue()).executeUpdate();
			session.getTransaction().commit();
			return true;
		} catch (Exception ex) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception ignored) {
				ais.common.ErrorAuditUtil.record(ignored, "auto-audit postingSatuFaktur rollback");
			}
			throw ex;
		}
	}

	/**
	 * Grid DRAF JURNAL PER TRANSAKSI di layar ZK -- meniru layar Posting Cicilan
	 * Mahasiswa: tiap transaksi tampil beserta baris akun debit/kreditnya sehingga
	 * dapat dianalisis dulu, punya status sendiri, dan tombol "Posting" per baris.
	 * Transaksi yang pemetaan akunnya belum lengkap ditandai beserta alasannya dan
	 * TIDAK memblokir transaksi lain.
	 */
	private void tampilkanGridDraf() {
		if (previewBox == null || rincianDraft.isEmpty()) {
			return;
		}
		org.zkoss.zul.Grid grid = new org.zkoss.zul.Grid();
		grid.setWidth("100%");
		grid.setSpan("true");
		org.zkoss.zul.Columns cols = new org.zkoss.zul.Columns();
		String[] judulKolom = new String[] { "Referensi", "Nilai", "Draf Jurnal (Akun / Debit / Kredit)",
				"Status", "Aksi" };
		String[] lebarKolom = new String[] { "18%", "12%", "44%", "16%", "10%" };
		for (int i = 0; i < judulKolom.length; i++) {
			org.zkoss.zul.Column c = new org.zkoss.zul.Column(judulKolom[i]);
			c.setWidth(lebarKolom[i]);
			cols.appendChild(c);
		}
		grid.appendChild(cols);
		org.zkoss.zul.Rows rows = new org.zkoss.zul.Rows();
		grid.appendChild(rows);
		for (int i = 0; i < rincianDraft.size(); i++) {
			final org.json.JSONObject baris = rincianDraft.get(i);
			final boolean siap = baris.optBoolean("siap", false);
			org.zkoss.zul.Row row = new org.zkoss.zul.Row();
			row.setValign("top");
			if (!siap) {
				row.setStyle("background:#fff7ed;");
			}
			row.appendChild(new Label(baris.optString("ref", "-")));
			row.appendChild(new Label("Rp " + DashboardUiKit.money(baris.optDouble("nilai", 0))));
			StringBuilder j = new StringBuilder("<table style='width:100%;font-size:11px;'>");
			org.json.JSONArray jr = baris.optJSONArray("jurnal");
			for (int k = 0; jr != null && k < jr.length(); k++) {
				org.json.JSONObject b = jr.optJSONObject(k);
				if (b == null) {
					continue;
				}
				double d = b.optDouble("debit", 0);
				double kr = b.optDouble("kredit", 0);
				j.append("<tr><td style='padding:1px 4px;'>")
						.append(DashboardUiKit.esc(b.optString("akun", "-")))
						.append("</td><td style='padding:1px 4px;text-align:right;'>")
						.append(d > 0 ? DashboardUiKit.money(d) : "")
						.append("</td><td style='padding:1px 4px;text-align:right;'>")
						.append(kr > 0 ? DashboardUiKit.money(kr) : "").append("</td></tr>");
			}
			double totD = 0, totK = 0;
			for (int k = 0; jr != null && k < jr.length(); k++) {
				org.json.JSONObject b2 = jr.optJSONObject(k);
				if (b2 == null) {
					continue;
				}
				totD += b2.optDouble("debit", 0);
				totK += b2.optDouble("kredit", 0);
			}
			j.append("<tr style='border-top:1px solid #cbd5e1;font-weight:700;'>")
					.append("<td style='padding:1px 4px;'>Total</td>")
					.append("<td style='padding:1px 4px;text-align:right;'>").append(DashboardUiKit.money(totD))
					.append("</td><td style='padding:1px 4px;text-align:right;'>").append(DashboardUiKit.money(totK))
					.append("</td></tr>");
			j.append("</table>");
			row.appendChild(DashboardUiKit.html(j.toString()));
			row.appendChild(DashboardUiKit.html(siap
					? "<span style='color:#166534;font-size:11px;'>Siap diposting</span>"
					: "<span style='color:#b45309;font-size:11px;'>Belum siap: "
							+ DashboardUiKit.esc(baris.optString("alasan", "")) + "</span>"));
			if (siap) {
				org.zkoss.zul.Toolbarbutton tombol = new org.zkoss.zul.Toolbarbutton("Posting");
				tombol.setStyle("color:#166534;font-weight:600;cursor:pointer;");
				tombol.addEventListener("onClick", new EventListener() {
					public void onEvent(Event ev) throws Exception {
						postingBarisIni(baris);
					}
				});
				row.appendChild(tombol);
			} else {
				row.appendChild(new Label("-"));
			}
			rows.appendChild(row);
		}
		previewBox.appendChild(DashboardUiKit.html(
				"<div style='font-weight:700;font-size:12px;margin:10px 0 4px;'>Draf Jurnal per Transaksi"
						+ " (analisis dulu, posting bisa satu per satu)</div>"));
		previewBox.appendChild(grid);
	}

	/** Posting SATU baris draf dari grid, lalu segarkan pratinjau. */
	private void postingBarisIni(org.json.JSONObject baris) throws Exception {
		Long id = baris.isNull("id") ? null : Long.valueOf(baris.get("id").toString());
		if (id == null) {
			return;
		}
		JSONObject hasil = postingPerTransaksi(java.util.Collections.singletonList(id));
		int diposting = hasil.optInt("diposting", 0);
		int gagal = hasil.optInt("gagal", 0);
		MyMessageboxConfig.show(
				gagal == 0 ? (diposting + " transaksi berhasil diposting ke jurnal.")
						: ("Gagal memposting transaksi ini. " + hasil.optString("pesan", "")),
				gagal == 0 ? "Berhasil" : "Gagal", MyMessageboxConfig.OK,
				gagal == 0 ? MyMessageboxConfig.INFORMATION : MyMessageboxConfig.EXCLAMATION);
		hitungPreview();
	}

	public JSONObject prosesApi(Date mulai, Date sampai, boolean posting) throws Exception {
		if (mulai == null || sampai == null || mulai.after(sampai)) {
			throw new IllegalArgumentException("Periode posting penjualan tidak valid.");
		}
		dpMulai = new Datebox();
		dpMulai.setValue(mulai);
		dpSampai = new Datebox();
		dpSampai.setValue(sampai);
		previewBox = new Div();
		hitungPreview();

		JSONObject hasil = new JSONObject();
		hasil.put("jenis", "penjualan");
		hasil.put("total", totalDebit);
		hasil.put("totalPendapatan", totalNet);
		hasil.put("totalPpn", totalPpn);
		hasil.put("siap", !headerTerposting.isEmpty() && totalDebit > 0 && belumDipetakan.isEmpty());
		hasil.put("jumlahTransaksi", headerTerposting.size());
		hasil.put("belumDipetakan", new JSONArray(belumDipetakan));
		hasil.put("jurnal", jurnalApi());
		hasil.put("rincian", new JSONArray(rincianDraft));
		Date terakhir = lastPostedEnd();
		hasil.put("terakhir", terakhir == null ? JSONObject.NULL : Common.databaseDateFormat.get().format(terakhir));
		if (!posting) {
			return hasil;
		}
		if (headerTerposting.isEmpty() || totalDebit <= 0) {
			throw new IllegalStateException("Tidak ada penjualan yang siap diposting pada periode ini.");
		}
		if (!belumDipetakan.isEmpty()) {
			throw new IllegalStateException("Masih ada " + belumDipetakan.size()
					+ " transaksi dengan pemetaan akun yang belum lengkap.");
		}

		Session session = HibernateUtil.currentSession();
		List<Akun> akunDebet = akunApi(debitKasPerAkun);
		List<Double> nilaiDebet = nilaiApi(debitKasPerAkun);
		Map<Long, Double> kreditGabungan = new LinkedHashMap<Long, Double>();
		gabungApi(kreditGabungan, pendapatanPerAkun);
		gabungApi(kreditGabungan, ppnPerAkun);
		List<Akun> akunKredit = akunApi(kreditGabungan);
		List<Double> nilaiKredit = nilaiApi(kreditGabungan);
		if (akunDebet.isEmpty() || akunKredit.isEmpty()) {
			throw new IllegalStateException("Akun debit/kredit penjualan tidak lengkap.");
		}
		String ket = "Penjualan Kantin " + Common.dateFormat.get().format(mulai) + " s.d "
				+ Common.dateFormat.get().format(sampai);
		PostingHistory ph = new PostingHistory(JENIS);
		ph.setTanggal(sampai);
		ph.setTbmuser(Common.getCurrentUser());
		ph.setKeterangan(ket);
		boolean ok = false;
		session.getTransaction().begin();
		try {
			session.save(ph);
			ok = CommonAkunting.saveTransaksi(akunDebet.toArray(new Akun[] {}), akunKredit.toArray(new Akun[] {}),
					null, null, ph, true, ket, sampai, nilaiDebet.toArray(new Double[] {}),
					nilaiKredit.toArray(new Double[] {}), Double.valueOf(0.0), null, satkerKantin(), session);
			if (ok) {
				tandaiHeaderTerposting(session, ph);
				session.getTransaction().commit();
			} else {
				session.getTransaction().rollback();
			}
		} catch (Exception ex) {
			try {
				session.getTransaction().rollback();
			} catch (Exception rollbackError) {
				ais.common.ErrorAuditUtil.record(rollbackError, "rollback PostingPenjualanKantinAction.prosesApi");
			}
			throw ex;
		}
		if (!ok) {
			throw new IllegalStateException("Posting penjualan dibatalkan oleh aturan periode closing.");
		}
		hasil.put("diposting", true);
		return hasil;
	}

	private JSONArray jurnalApi() throws Exception {
		JSONArray rows = new JSONArray();
		tambahJurnalApi(rows, debitKasPerAkun, "DEBIT Kas/Bank");
		tambahJurnalApi(rows, pendapatanPerAkun, "KREDIT Pendapatan");
		tambahJurnalApi(rows, ppnPerAkun, "KREDIT PPN Keluaran");
		return rows;
	}

	private void tambahJurnalApi(JSONArray rows, Map<Long, Double> sumber, String posisi) throws Exception {
		for (Map.Entry<Long, Double> en : sumber.entrySet()) {
			JSONObject row = new JSONObject();
			row.put("akunId", en.getKey());
			row.put("akun", akunNama.get(en.getKey()));
			row.put("posisi", posisi);
			row.put("nominal", en.getValue());
			rows.put(row);
		}
	}

	private List<Akun> akunApi(Map<Long, Double> sumber) {
		List<Akun> list = new ArrayList<Akun>();
		for (Long id : sumber.keySet()) {
			Akun akun = (Akun) ConstantValues.ambil(Akun.class.getName(), id);
			if (akun != null) {
				list.add(akun);
			}
		}
		return list;
	}

	private List<Double> nilaiApi(Map<Long, Double> sumber) {
		return new ArrayList<Double>(sumber.values());
	}

	private void gabungApi(Map<Long, Double> target, Map<Long, Double> sumber) {
		for (Map.Entry<Long, Double> en : sumber.entrySet()) {
			add(target, en.getKey(), en.getValue().doubleValue());
		}
	}

	/** Tandai posting_history seluruh header yang ikut batch (idempoten anti-dobel). */
	private void tandaiHeaderTerposting(Session session, PostingHistory ph) {
		if (headerTerposting.isEmpty() || ph == null || ph.getId() == null) {
			return;
		}
		StringBuilder ids = new StringBuilder();
		for (Long id : headerTerposting) {
			if (id == null) {
				continue;
			}
			ids.append(ids.length() == 0 ? "" : ",").append(id.longValue());
		}
		if (ids.length() == 0) {
			return;
		}
		session.createSQLQuery("UPDATE koperasi.pembelian_anggota_koperasi SET posting_history = "
				+ ph.getId().longValue() + " WHERE id IN (" + ids + ") AND posting_history IS NULL").executeUpdate();
	}

	// ---------------- util ----------------

	private static double num(Object o) {
		return o == null ? 0.0 : ((Number) o).doubleValue();
	}

	private static Long asLong(Object o) {
		return o == null ? null : Long.valueOf(((Number) o).longValue());
	}

	private void add(Map<Long, Double> map, Long key, double nilai) {
		if (key == null) {
			return;
		}
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

	/** Tanggal akhir batch penjualan terakhir yang sudah diposting (atau {@code null}). */
	public static Date lastPostedEnd(Session session) {
		try {
			return (Date) session.createCriteria(PostingHistory.class).add(Restrictions.eq("jenis", JENIS))
					.setProjection(Projections.max("tanggal")).uniqueResult();
		} catch (Exception e) {
			return null;
		}
	}

	/** Jumlah transaksi penjualan (header) belum diposting sejak batch terakhir sampai hari ini. */
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
			Number n = (Number) session.createSQLQuery(
					"SELECT COUNT(*) FROM koperasi.pembelian_anggota_koperasi h "
							+ "WHERE h.posting_history IS NULL AND COALESCE(h.total_biaya,0) > 0 "
							+ "AND date(h.tanggal_pembayaran) BETWEEN date('" + mStr + "') AND date('" + sStr + "') "
							+ "AND EXISTS (SELECT 1 FROM koperasi.pembelian pb "
							+ "            WHERE pb.pembelian_anggota_koperasi = h.id AND pb.aktif = true)")
					.uniqueResult();
			return n == null ? 0 : n.intValue();
		} catch (Exception e) {
			return 0;
		}
	}

	/** Jumlah batch penjualan yang sudah pernah diposting sepanjang waktu. */
	public static int hitungTerposting(Session session) {
		try {
			Number n = (Number) session.createCriteria(PostingHistory.class).add(Restrictions.eq("jenis", JENIS))
					.setProjection(Projections.rowCount()).uniqueResult();
			return n == null ? 0 : n.intValue();
		} catch (Exception e) {
			return 0;
		}
	}

	private String namaAkunById(Long id) {
		try {
			Akun a = (Akun) ConstantValues.ambil(Akun.class.getName(), id);
			return namaAkun(a);
		} catch (Exception e) {
			return "";
		}
	}

	private String namaAkun(Akun a) {
		if (a == null) {
			return "";
		}
		try {
			String kode = a.getKode() == null ? "" : a.getKode();
			String nama = a.getNama() == null ? "" : a.getNama();
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
