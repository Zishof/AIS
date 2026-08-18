package ais.action.master.koperasi;

import java.util.ArrayList;
import java.util.Date;
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
		Session session = HibernateUtil.currentSession();
		String mStr = Common.databaseDateFormat.get().format(mulai);
		String sStr = Common.databaseDateFormat.get().format(sampai);

		// Rincian per barang (untuk tampilan seperti tab posting lain).
		StringBuilder detailRows = new StringBuilder();
		int barisTampil = 0;

		// (1) Rata-rata & (1b) biaya kulakan TERAKHIR per produk s.d tanggal "sampai" (bahan metode HPP).
		Map<Long, double[]> rataKulakan = rataRataKulakanPerProduk(session, sStr);
		Map<Long, Double> kulakanTerakhir = kulakanTerakhirPerProduk(session, sStr);

		// (2) Penjualan per produk — DIBACA LEWAT header pembelian_anggota_koperasi (penjualan ke anggota),
		//     memakai tanggal_pembayaran header sebagai tanggal jual. Hanya baris aktif & produk tertaut aset.
		//     pr.metode_hpp = metode biaya per-produk (kosong = default rata-rata kulakan → fallback harga beli).
		@SuppressWarnings("unchecked")
		List<Object[]> rows = session.createSQLQuery(
				"SELECT pb.produk, pr.master_asset, COALESCE(pr.hargabeli,0), COALESCE(SUM(pb.qty),0), pr.metode_hpp, "
						+ "jp.akun_hpp "
						+ "FROM koperasi.pembelian pb "
						+ "INNER JOIN koperasi.produk pr ON pr.id = pb.produk "
						+ "LEFT JOIN koperasi.jenis_produk jp ON jp.id = pr.jenis_produk "
						+ "INNER JOIN koperasi.pembelian_anggota_koperasi h ON h.id = pb.pembelian_anggota_koperasi "
						+ "WHERE pr.master_asset IS NOT NULL AND pb.aktif = true "
						+ "AND date(h.tanggal_pembayaran) BETWEEN date('" + mStr + "') AND date('" + sStr + "') "
						+ "GROUP BY pb.produk, pr.master_asset, pr.hargabeli, pr.metode_hpp, jp.akun_hpp "
						+ "ORDER BY pb.produk").list();

		for (Object[] r : rows) {
			if (r[0] == null || r[1] == null) {
				continue;
			}
			Long produkId = ((Number) r[0]).longValue();
			Long masterAssetId = ((Number) r[1]).longValue();
			double hargaBeliProduk = r[2] == null ? 0 : ((Number) r[2]).doubleValue();
			double qty = r[3] == null ? 0 : ((Number) r[3]).doubleValue();
			String metode = r.length > 4 && r[4] != null ? r[4].toString().trim() : null;
			if (qty <= 0) {
				continue;
			}

			// Harga pokok/satuan mengikuti METODE HPP per-produk (pr.metode_hpp):
			//  - HARGA_BELI_PRODUK        → harga beli produk (statis)
			//  - KULAKAN_TERAKHIR         → biaya kulakan terakhir (fallback harga beli produk)
			//  - RATA_RATA_KULAKAN/kosong → DEFAULT: rata-rata biaya kulakan (fallback harga beli produk)
			double[] k = rataKulakan.get(produkId);
			double avgKulakan = (k != null && k[1] > 0) ? (k[0] / k[1]) : 0.0;
			Double lastObj = kulakanTerakhir.get(produkId);
			double lastKulakan = lastObj == null ? 0.0 : lastObj.doubleValue();
			double hargaPokok;
			String sumberBiaya;
			if ("HARGA_BELI_PRODUK".equals(metode)) {
				hargaPokok = hargaBeliProduk;
				sumberBiaya = "Harga beli produk";
			} else if ("KULAKAN_TERAKHIR".equals(metode)) {
				if (lastKulakan > 0) {
					hargaPokok = lastKulakan;
					sumberBiaya = "Kulakan terakhir";
				} else {
					hargaPokok = hargaBeliProduk;
					sumberBiaya = "Harga beli produk (fallback)";
				}
			} else {
				// default / RATA_RATA_KULAKAN
				if (avgKulakan > 0) {
					hargaPokok = avgKulakan;
					sumberBiaya = "Rata-rata kulakan";
				} else if (hargaBeliProduk > 0) {
					hargaPokok = hargaBeliProduk;
					sumberBiaya = "Harga beli produk (fallback)";
				} else {
					hargaPokok = 0;
					sumberBiaya = "-";
				}
			}
			double hpp = qty * hargaPokok;

			MasterAsset ma = (MasterAsset) session.get(MasterAsset.class, masterAssetId);
			String namaBarang = ma == null ? ("#" + masterAssetId) : ma.getNama();

			Long jpAkunHppId = r.length > 5 && r[5] != null ? Long.valueOf(((Number) r[5]).longValue()) : null;

			Akun akunHpp = null;
			Akun akunPersediaan = null;
			// Prioritas akun HPP: dari Jenis Produk (bila diisi), lalu fallback ke Kelompok Aset.
			if (jpAkunHppId != null) {
				try {
					akunHpp = (Akun) ConstantValues.ambil(Akun.class.getName(), jpAkunHppId);
				} catch (Exception ex) {
					akunHpp = null;
				}
			}
			if (ma != null) {
				KelompokAsset kelompok = ma.getKelompokAsset();
				try {
					if (akunHpp == null && kelompok != null && kelompok.getAkunBebanPokokPenjualan() != null) {
						akunHpp = AssetUtil.ambilDataAkun(kelompok.getAkunBebanPokokPenjualan(), satker);
					}
				} catch (Exception ex) {
					// biarkan akunHpp apa adanya (mungkin sudah dari Jenis Produk)
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
			}

			boolean akunLengkap = hpp > 0 && akunHpp != null && akunPersediaan != null;

			// Baris rincian per barang (selalu ditampilkan agar transparan; yang tak lengkap ditandai).
			barisTampil++;
			detailRows.append("<tr style='border-bottom:1px solid #eef2f7;").append(akunLengkap ? "" : "background:#fff7ed;")
					.append("'>")
					.append("<td style='padding:6px;'>").append(DashboardUiKit.esc(namaBarang)).append("</td>")
					.append("<td style='text-align:right;padding:6px;'>").append(DashboardUiKit.money(qty)).append("</td>")
					.append("<td style='text-align:right;padding:6px;'>Rp ").append(DashboardUiKit.money(hargaPokok))
					.append("<div style='font-size:9px;color:#64748b;'>").append(DashboardUiKit.esc(sumberBiaya))
					.append("</div></td>")
					.append("<td style='text-align:right;padding:6px;font-weight:700;'>Rp ").append(DashboardUiKit.money(hpp))
					.append("</td>")
					.append("<td style='padding:6px;font-size:11px;'>")
					.append(akunHpp == null ? "<span style='color:#b45309;'>(akun HPP belum diatur)</span>"
							: DashboardUiKit.esc(namaAkun(akunHpp)))
					.append("</td>")
					.append("<td style='padding:6px;font-size:11px;'>")
					.append(akunPersediaan == null ? "<span style='color:#b45309;'>(akun persediaan belum diatur)</span>"
							: DashboardUiKit.esc(namaAkun(akunPersediaan)))
					.append("</td></tr>");

			if (!akunLengkap) {
				if (hpp > 0) {
					belumDipetakan.add(namaBarang);
				}
				continue;
			}

			tambah(debitPerAkun, akunHpp, hpp);
			tambah(kreditPerAkun, akunPersediaan, hpp);
			akunNama.put(akunHpp.getId(), namaAkun(akunHpp));
			akunNama.put(akunPersediaan.getId(), namaAkun(akunPersediaan));
			totalCogs += hpp;
		}

		StringBuilder sb = new StringBuilder();
		sb.append("<div style='font-size:13px;font-weight:800;color:#0f172a;margin:6px 0;'>Pratinjau Jurnal HPP — ")
				.append(Common.dateFormat.get().format(mulai)).append(" s.d ").append(Common.dateFormat.get().format(sampai))
				.append("</div>");

		if (barisTampil == 0) {
			sb.append("<div style='font-size:12px;color:#64748b;padding:8px;'>Tidak ada penjualan barang tertaut aset "
					+ "pada periode ini (dibaca dari Pembelian Anggota Koperasi). Pastikan ada transaksi kasir pada rentang "
					+ "tanggal, dan produk sudah ditautkan ke Barang/Aset persediaan.</div>");
		} else {
			// Rincian per barang (mirip tampilan tab posting lain: memperlihatkan asal data & perhitungan).
			sb.append("<div style='font-size:12px;font-weight:700;color:#334155;margin:8px 0 4px;'>Rincian per barang</div>");
			sb.append("<div style='overflow-x:auto;'><table style='width:100%;border-collapse:collapse;font-size:12px;'>");
			sb.append("<thead><tr style='background:#f1f5f9;'>"
					+ "<th style='text-align:left;padding:6px;'>Barang</th>"
					+ "<th style='text-align:right;padding:6px;'>Qty Terjual</th>"
					+ "<th style='text-align:right;padding:6px;'>Harga Pokok/Sat</th>"
					+ "<th style='text-align:right;padding:6px;'>HPP</th>"
					+ "<th style='text-align:left;padding:6px;'>Akun HPP (Debit)</th>"
					+ "<th style='text-align:left;padding:6px;'>Akun Persediaan (Kredit)</th></tr></thead><tbody>");
			sb.append(detailRows);
			sb.append("</tbody></table></div>");

			// Ringkasan jurnal (yang akan diposting) — akun terkelompok.
			sb.append("<div style='font-size:12px;font-weight:700;color:#334155;margin:12px 0 4px;'>Ringkasan Jurnal (akan diposting)</div>");
			if (debitPerAkun.isEmpty()) {
				sb.append("<div style='font-size:12px;color:#b45309;padding:8px;'>Belum ada baris yang siap diposting "
						+ "(akun HPP/persediaan pada Kelompok Aset belum lengkap).</div>");
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
		}
		if (!belumDipetakan.isEmpty()) {
			sb.append("<div style='font-size:12px;color:#b45309;margin-top:8px;'>⚠ ").append(belumDipetakan.size())
					.append(" barang persediaan belum lengkap akunnya di Kelompok Aset (akun HPP/persediaan) — tidak diikutkan: ")
					.append(DashboardUiKit.esc(ringkas(belumDipetakan))).append("</div>");
		}
		previewBox.appendChild(DashboardUiKit.html(sb.toString()));
	}

	/**
	 * Rata-rata biaya kulakan (pengadaan/barang masuk) per produk s.d tanggal {@code sampaiStr}
	 * (weighted-average: Σ(qty×hargaBeliSatuan) / Σqty). Dipakai sebagai metode HPP default;
	 * bila sebuah produk tak punya kulakan, pemanggil jatuh ke {@code Produk.hargaBeli} (biaya terakhir).
	 * Map: produkId → [totalBiaya, totalQty].
	 */
	private static Map<Long, double[]> rataRataKulakanPerProduk(Session session, String sampaiStr) {
		Map<Long, double[]> map = new java.util.HashMap<Long, double[]>();
		try {
			@SuppressWarnings("unchecked")
			List<Object[]> rows = session.createSQLQuery(
					"SELECT produk, COALESCE(SUM(qty*COALESCE(hargabelisatuan,0)),0), COALESCE(SUM(qty),0) "
							+ "FROM koperasi.pengadaan_produk "
							+ "WHERE produk IS NOT NULL AND date(waktupengadaan) <= date('" + sampaiStr + "') "
							+ "GROUP BY produk").list();
			for (Object[] r : rows) {
				if (r[0] == null) {
					continue;
				}
				Long pid = ((Number) r[0]).longValue();
				double biaya = r[1] == null ? 0 : ((Number) r[1]).doubleValue();
				double q = r[2] == null ? 0 : ((Number) r[2]).doubleValue();
				map.put(pid, new double[] { biaya, q });
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit rataRataKulakanPerProduk");
		}
		return map;
	}

	/**
	 * Biaya kulakan TERAKHIR (per tanggal) tiap produk s.d {@code sampaiStr}. Memakai
	 * {@code DISTINCT ON (produk)} PostgreSQL (urut waktupengadaan &amp; id turun) sehingga tiap produk
	 * mengambil satu baris kulakan paling akhir. Map: produkId → hargaBeliSatuan.
	 */
	private static Map<Long, Double> kulakanTerakhirPerProduk(Session session, String sampaiStr) {
		Map<Long, Double> map = new java.util.HashMap<Long, Double>();
		try {
			@SuppressWarnings("unchecked")
			List<Object[]> rows = session.createSQLQuery(
					"SELECT DISTINCT ON (produk) produk, COALESCE(hargabelisatuan,0) "
							+ "FROM koperasi.pengadaan_produk "
							+ "WHERE produk IS NOT NULL AND date(waktupengadaan) <= date('" + sampaiStr + "') "
							+ "ORDER BY produk, waktupengadaan DESC, id DESC").list();
			for (Object[] r : rows) {
				if (r[0] == null) {
					continue;
				}
				Long pid = ((Number) r[0]).longValue();
				double biaya = r[1] == null ? 0 : ((Number) r[1]).doubleValue();
				map.put(pid, Double.valueOf(biaya));
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit kulakanTerakhirPerProduk");
		}
		return map;
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

	/**
	 * Adapter headless untuk POS Desktop/Android. Perhitungan tetap memakai satu
	 * sumber yang sama dengan layar ZK sehingga pratinjau dan jurnal tidak dapat
	 * menyimpang. Tidak membuat komponen halaman atau melakukan redirect.
	 */
	public JSONObject prosesApi(Date mulai, Date sampai, boolean posting) throws Exception {
		if (mulai == null || sampai == null || mulai.after(sampai)) {
			throw new IllegalArgumentException("Periode posting HPP tidak valid.");
		}
		dpMulai = new Datebox();
		dpMulai.setValue(mulai);
		dpSampai = new Datebox();
		dpSampai.setValue(sampai);
		previewBox = new Div();
		hitungPreview();

		JSONObject hasil = new JSONObject();
		hasil.put("jenis", "hpp");
		hasil.put("total", totalCogs);
		hasil.put("siap", totalCogs > 0 && !debitPerAkun.isEmpty() && belumDipetakan.isEmpty());
		hasil.put("belumDipetakan", new JSONArray(belumDipetakan));
		hasil.put("jurnal", jurnalApi(debitPerAkun, kreditPerAkun, "HPP", "Persediaan"));
		Date terakhir = lastPostedEnd();
		hasil.put("terakhir", terakhir == null ? JSONObject.NULL : Common.databaseDateFormat.get().format(terakhir));
		if (!posting) {
			return hasil;
		}
		if (terakhir != null && !mulai.after(terakhir)) {
			throw new IllegalStateException("Periode tumpang tindih; posting terakhir sampai "
					+ Common.dateFormat.get().format(terakhir) + ".");
		}
		if (totalCogs <= 0 || debitPerAkun.isEmpty()) {
			throw new IllegalStateException("Tidak ada HPP yang siap diposting pada periode ini.");
		}
		if (!belumDipetakan.isEmpty()) {
			throw new IllegalStateException("Masih ada " + belumDipetakan.size()
					+ " barang tanpa pemetaan akun HPP/persediaan.");
		}

		Session session = HibernateUtil.currentSession();
		List<Akun> akunDebet = akunApi(debitPerAkun);
		List<Double> nilaiDebet = nilaiApi(debitPerAkun);
		List<Akun> akunKredit = akunApi(kreditPerAkun);
		List<Double> nilaiKredit = nilaiApi(kreditPerAkun);
		if (akunDebet.isEmpty() || akunKredit.isEmpty()) {
			throw new IllegalStateException("Akun HPP/persediaan tidak lengkap.");
		}
		String ket = "HPP Penjualan Kantin " + Common.dateFormat.get().format(mulai) + " s.d "
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
				session.getTransaction().commit();
			} else {
				session.getTransaction().rollback();
			}
		} catch (Exception ex) {
			try {
				session.getTransaction().rollback();
			} catch (Exception rollbackError) {
				ais.common.ErrorAuditUtil.record(rollbackError, "rollback PostingHppKantinAction.prosesApi");
			}
			throw ex;
		}
		if (!ok) {
			throw new IllegalStateException("Posting HPP dibatalkan oleh aturan periode closing.");
		}
		hasil.put("diposting", true);
		return hasil;
	}

	private JSONArray jurnalApi(Map<Long, Double> debit, Map<Long, Double> kredit, String labelDebit,
			String labelKredit) throws Exception {
		JSONArray rows = new JSONArray();
		for (Map.Entry<Long, Double> en : debit.entrySet()) {
			JSONObject row = new JSONObject();
			row.put("akunId", en.getKey());
			row.put("akun", akunNama.get(en.getKey()));
			row.put("posisi", "DEBIT " + labelDebit);
			row.put("nominal", en.getValue());
			rows.put(row);
		}
		for (Map.Entry<Long, Double> en : kredit.entrySet()) {
			JSONObject row = new JSONObject();
			row.put("akunId", en.getKey());
			row.put("akun", akunNama.get(en.getKey()));
			row.put("posisi", "KREDIT " + labelKredit);
			row.put("nominal", en.getValue());
			rows.put(row);
		}
		return rows;
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
			// Jumlah produk tertaut aset yang terjual (via header Pembelian Anggota Koperasi) pada periode pending.
			// Konsisten dengan sumber penjualan di hitungPreview (tanggal_pembayaran header).
			Number n = (Number) session.createSQLQuery(
					"SELECT COUNT(DISTINCT pb.produk) "
							+ "FROM koperasi.pembelian pb "
							+ "INNER JOIN koperasi.produk pr ON pr.id = pb.produk "
							+ "INNER JOIN koperasi.pembelian_anggota_koperasi h ON h.id = pb.pembelian_anggota_koperasi "
							+ "WHERE pr.master_asset IS NOT NULL AND pb.aktif = true "
							+ "AND date(h.tanggal_pembayaran) BETWEEN date('" + mStr + "') AND date('" + sStr + "')")
					.uniqueResult();
			return n == null ? 0 : n.intValue();
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
