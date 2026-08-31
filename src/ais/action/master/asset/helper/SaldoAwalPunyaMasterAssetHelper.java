package ais.action.master.asset.helper;
/* ENHANCED_PENGGUNAAN_ANGGARAN_MEMORY_SAFE_2026_06_03 - Java 1.6/1.7 compatible. */

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Space;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.akunting.JenisUangMukaAction;
import ais.action.master.asset.helper.BreakdownTagihanVendorHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataWorkspaceBanbox;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.akunting.Pajak;
import ais.database.model.asset.JenisPajakBarang;
import ais.database.model.asset.JenisPajakPpn;
import ais.database.model.asset.MasterAsset;
import ais.database.model.asset.PemesananPengadaanMasterAsset;
import ais.database.model.asset.PenerimaanPengadaanMasterAsset;
import ais.database.model.asset.PenerimaanPengadaanMasterAssetDetail;
import ais.database.model.asset.PermintaanPengadaanMasterAsset;
import ais.database.model.asset.SaldoAwalMasterAsset;
import ais.database.model.asset.SaldoAwalMasterAssetDetail;
import ais.database.model.rab.Workspace;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyGroupboxStyled;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Helper terfokus untuk saldo awal punya master asset. Tipe ini membungkus satu variasi kecil dari
 * alur yang lebih umum agar pemanggil memakai nama domain yang jelas dan tidak menggandakan
 * implementasi.
 *
 * <p><b>Batas tanggung jawab:</b> gunakan tipe ini hanya untuk state dan operasi yang sesuai dengan nama
 * domainnya. Logika lintas domain harus didelegasikan ke service atau helper bersama supaya tidak muncul
 * implementasi paralel dengan hasil berbeda.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyGrid gridMasterAsset}, {@code boolean
 * edit}, {@code boolean delete}, {@code Footer footerTotalSemua}, {@code SaldoAwalMasterAsset
 * saldoAwalMasterAsset}; inisialisasi/lifecycle ({@code initDetail()}, {@code initRow()}); pembacaan/pencarian
 * ({@code reloadNilaiTimer()}, {@code reloadNilai()}, {@code loadDataDetail()}); mutasi data ({@code
 * simpanDetailAman()}); operasi domain lain ({@code doubleValue()}, {@code safeText()}, {@code formatNumber()},
 * {@code isReadonly()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 */
public class SaldoAwalPunyaMasterAssetHelper {

	private MyGrid gridMasterAsset;
	private boolean edit = false;
	private boolean delete = false;
	private Footer footerTotalSemua;
	private SaldoAwalMasterAsset saldoAwalMasterAsset;

	public SaldoAwalPunyaMasterAssetHelper(MyGrid gridMasterAsset) {
		this.gridMasterAsset = gridMasterAsset;

	}


	private static double doubleValue(Double value) {
		return value == null ? 0.0 : value.doubleValue();
	}

	private static String safeText(String value) {
		return value == null ? "" : value.trim();
	}

	private static String formatNumber(Double value) {
		return Common.numberFormat.get().format(doubleValue(value));
	}

	private static boolean isReadonly(SaldoAwalMasterAssetDetail detail, boolean edit) {
		return detail == null || detail.getSaldoAwal() == null || detail.getSaldoAwal().getDisetujuiOleh() != null || !edit;
	}

	/**
	 * Menyimpan satu detail saldo awal dengan aman. Bila induk {@link SaldoAwalMasterAsset} masih
	 * transient (belum punya id, mis. saldo awal baru yang belum disimpan), induk disimpan lebih dulu
	 * supaya flush detail tidak melempar {@code TransientObjectException} ("object references an
	 * unsaved transient instance"). Memakai {@code currentSession} (Common.refreshUpdate) sehingga
	 * tidak perlu menutup sesi secara manual.
	 */
	private static void simpanDetailAman(Session session, SaldoAwalMasterAssetDetail detail) {
		if (detail == null) {
			return;
		}
		if (detail.getSaldoAwal() != null && detail.getSaldoAwal().getId() == null) {
			Common.refreshUpdate(session, detail.getSaldoAwal());
		}
		Common.refreshUpdate(session, detail);
	}

	private void reloadNilaiTimer() {
		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				reloadNilai();
			}
		});
	}

	public Groupbox initDetail(final SaldoAwalMasterAsset saldoAwalMasterAsset, final boolean persetujuan,
			final MyCheckboxConfig tanpaPenerimaan) throws Exception {
		this.saldoAwalMasterAsset = saldoAwalMasterAsset;
		MyGroupboxStyled groupbox = new MyGroupboxStyled();

		groupbox.appendChild(new MyCaptionStyled("Daftar Barang / Jasa Tagihan Vendor"));
		groupbox.setStyle("width:100%; padding:6px; border-radius:10px; overflow:auto;");

		edit = saldoAwalMasterAsset.getDisetujuiOleh() == null;
		delete = saldoAwalMasterAsset.getDisetujuiOleh() == null;

		if (saldoAwalMasterAsset.getDisetujuiOleh() == null) {
			Toolbar toolbar = new Toolbar();
			toolbar.setHeight("42px");
			toolbar.setStyle("border:0; background:#f8fafc; padding:6px; border-radius:8px;");

			final MyToolbarbuttonConfig add = new MyToolbarbuttonConfig("Ambil Data Barang/Jasa", "/img/new.gif");
			add.setVisible(saldoAwalMasterAsset.getDisetujuiOleh() == null && tanpaPenerimaan.isChecked());
			add.setParent(toolbar);
			add.setTooltiptext("Tambah");
			add.addEventListener("onClick", new EventListener() {
				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {

					List<MasterAsset> masterAssets = new ArrayList<MasterAsset>();
					List<Row> myrows = gridMasterAsset.getRows().getChildren();
					for (Row row : myrows) {
						masterAssets.add(((SaldoAwalMasterAssetDetail) row.getAttribute("saldoAwalMasterAssetDetail"))
								.getMasterAsset());
					}
					AmbilDataMasterAssetBanyak ambilDataMasterAssetBanyak = new AmbilDataMasterAssetBanyak(masterAssets,
							null);
					ambilDataMasterAssetBanyak.setHeight("95%");
					ambilDataMasterAssetBanyak.setWidth("90%");
					ambilDataMasterAssetBanyak
							.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					ambilDataMasterAssetBanyak.setEventListener(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							List<MasterAsset> masterAssets = (List<MasterAsset>) arg0.getData();
							for (MasterAsset masterAsset : masterAssets) {
								SaldoAwalMasterAssetDetail saldoAwalMasterAssetDetail = new SaldoAwalMasterAssetDetail();
								saldoAwalMasterAssetDetail.setMasterAsset(masterAsset);
								saldoAwalMasterAssetDetail.setJumlah(1.0);
								saldoAwalMasterAssetDetail.setKeterangan("");
								saldoAwalMasterAssetDetail.setSaldoAwal(saldoAwalMasterAsset);

								if (saldoAwalMasterAsset.getId() != null) {
									Session session = HibernateUtil.currentSession();
									session.save(saldoAwalMasterAssetDetail);
								}

								Rows rows = gridMasterAsset.getRows() == null ? new Rows() : gridMasterAsset.getRows();
								rows.setParent(gridMasterAsset);
								Row row = new Row();
								row.setValign("top");
								row.setParent(rows);
								initRow(row, saldoAwalMasterAssetDetail, persetujuan);
							}
						}
					});

					ambilDataMasterAssetBanyak.onModal();

				}
			});

			tanpaPenerimaan.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					add.setVisible(saldoAwalMasterAsset.getDisetujuiOleh() == null && tanpaPenerimaan.isChecked());
				}
			});

			new Space().setParent(toolbar);
			new Space().setParent(toolbar);
			new Space().setParent(toolbar);

			MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
			cari.setParent(toolbar);
			cari.setDisabled(saldoAwalMasterAsset.getId() != null);
			cari.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					loadDataDetail(saldoAwalMasterAsset, saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset(),
							persetujuan);
				}
			});

			// ── Pilihan mode PPh: "Sesuai PO" (default) atau "Breakdown" (saling ekslusif) ──
			new Space().setParent(toolbar);
			final MyCheckboxConfig cbSesuaiPO = new MyCheckboxConfig("Sesuai PO");
			final MyCheckboxConfig cbBreakdown = new MyCheckboxConfig("Breakdown");
			boolean modeBreakdownAwal = Boolean.TRUE.equals(saldoAwalMasterAsset.getBreakdownAktif());
			cbSesuaiPO.setChecked(!modeBreakdownAwal);
			cbBreakdown.setChecked(modeBreakdownAwal);
			cbSesuaiPO.setTooltiptext("PPh mengikuti nilai per detail PO (default)");
			cbBreakdown.setTooltiptext("Input item manual; PPh mengikuti nilai Bukti Potong");
			cbSesuaiPO.setParent(toolbar);
			cbBreakdown.setParent(toolbar);
			cbSesuaiPO.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event e) throws Exception {
					cbSesuaiPO.setChecked(true);
					cbBreakdown.setChecked(false);
					saldoAwalMasterAsset.setBreakdownAktif(false);
					Common.refreshSaveOrUpdate(saldoAwalMasterAsset);
					BreakdownTagihanVendorHelper.sinkronPajakBreakdown(saldoAwalMasterAsset);
				}
			});
			cbBreakdown.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event e) throws Exception {
					cbBreakdown.setChecked(true);
					cbSesuaiPO.setChecked(false);
					saldoAwalMasterAsset.setBreakdownAktif(true);
					Common.refreshSaveOrUpdate(saldoAwalMasterAsset);
					BreakdownTagihanVendorHelper.sinkronPajakBreakdown(saldoAwalMasterAsset);
					BreakdownTagihanVendorHelper.tampilkanPopup(saldoAwalMasterAsset, e.getTarget());
				}
			});

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("History", "/img/jadwal.png");
			button.setVisible(saldoAwalMasterAsset.getId() != null);
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					RevisiSaldoAwalMasterAssetDetailHelper revisiHelper = new RevisiSaldoAwalMasterAssetDetailHelper(
							saldoAwalMasterAsset, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									loadDataDetail(saldoAwalMasterAsset,
											saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset(), persetujuan);
								}
							});
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(revisiHelper);
					revisiHelper.setVisible(true);
					revisiHelper.onModal();

				}

			});
			button.setParent(toolbar);

			if (add.isVisible() || cari.isVisible() || button.isVisible()) {
				toolbar.setParent(groupbox);
			}
		}

		// Tombol Cetak Breakdown — selalu tampil bila SaldoAwal sudah tersimpan
		if (saldoAwalMasterAsset.getId() != null) {
			Toolbar tbBreakdown = new Toolbar();
			tbBreakdown.setHeight("36px");
			tbBreakdown.setStyle("border:0; background:#f1f5f9; padding:4px;");
			MyToolbarbuttonConfig btnBreakdown = new MyToolbarbuttonConfig("Cetak Breakdown",
					"/img/svg/list-box-line.svg");
			btnBreakdown.setTooltiptext("Cetak detail breakdown item tagihan vendor");
			btnBreakdown.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					BreakdownTagihanVendorHelper.cetakBreakdown(saldoAwalMasterAsset);
				}
			});
			btnBreakdown.setParent(tbBreakdown);
			tbBreakdown.setParent(groupbox);
		}

		Common.clear(gridMasterAsset);
		// Bungkus grid dalam Div ber-scroll agar daftar item barang/jasa BISA DI-SCROLL.
		// Sebelumnya grid dipasang langsung ke MyGroupboxStyled yang (a) overflow:hidden dari
		// konstruktor dan (b) setStyle()-nya no-op (overflow:auto yang diminta diabaikan), sehingga
		// setHeight/overflow pada grid tidak menghasilkan body-scroll yang andal → daftar terpotong.
		// Div ber-max-height + overflow:auto memberi scroll yang pasti (grid dibiarkan tinggi natural).
		org.zkoss.zul.Div gridScroll = new org.zkoss.zul.Div();
		gridScroll.setStyle("width:100%; max-height:520px; overflow:auto; box-sizing:border-box;");
		gridScroll.setParent(groupbox);
		gridMasterAsset.setParent(gridScroll);
		gridMasterAsset.setStyle("min-height:120px; border:0; background:#ffffff;");
		gridMasterAsset.setWidth("100%");
		Columns columns = new Columns();
		columns.setParent(gridMasterAsset);
		columns.setSizable(true);
		columns.setStyle("background:#f8fafc; border-bottom:1px solid #e5e7eb; font-weight:bold;");

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama Barang / Jasa");
		column.setWidth("13%");

		if (saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset() == null) {
			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Anggaran");
			column.setWidth("7%");
		}

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Qty");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Harga");
		column.setWidth("7%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Persen");
		column.setWidth("4%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Diskon");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("PPN");
		column.setWidth("6%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nilai PPN");
		column.setAlign("right");
		column.setWidth("6%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("PPH");
		column.setWidth("6%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("N.PPN");
		column.setAlign("right");
		column.setWidth("6%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("N.PPH");
		column.setAlign("right");
		column.setWidth("6%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Total");
		column.setAlign("right");
		column.setWidth("7%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("10%");

		loadDataDetail(saldoAwalMasterAsset, saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset(), persetujuan);

		Foot foot = new Foot();
		foot.setParent(gridMasterAsset);

		Footer footer = new Footer("Total");
		foot.appendChild(footer);

		footer = new Footer();
		foot.appendChild(footer);

		footer = new Footer();
		foot.appendChild(footer);

		footer = new Footer();
		foot.appendChild(footer);

		footer = new Footer();
		foot.appendChild(footer);

		footer = new Footer();
		foot.appendChild(footer);

		footer = new Footer();
		foot.appendChild(footer);

		footer = new Footer();
		foot.appendChild(footer);

		footer = new Footer();
		foot.appendChild(footer);

		footer = new Footer();
		foot.appendChild(footer);

		footerTotalSemua = new Footer(Common.numberFormat.get().format(0.0));
		foot.appendChild(footerTotalSemua);

		footer = new Footer();
		foot.appendChild(footer);

		footer = new Footer();
		foot.appendChild(footer);

		reloadNilaiTimer();

		// Mode BREAKDOWN: ganti tampilan tabel PPN/PPH per-item dengan RINGKASAN breakdown
		// read-only (daftar item + Subtotal Barang/Jasa, PPN, Jumlah Total, Jenis PPh, Bukti
		// Potong, Total Transfer) — sama layout dgn popup/CETAK. Toolbar pilihan mode tetap tampil.
		try {
			if (Boolean.TRUE.equals(saldoAwalMasterAsset.getBreakdownAktif())) {
				String htmlBreakdown = ais.action.master.asset.helper.BreakdownTagihanVendorHelper
						.buildHtmlRingkasanEmbed(saldoAwalMasterAsset);
				if (htmlBreakdown != null && htmlBreakdown.length() > 0) {
					gridMasterAsset.setVisible(false);
					ais.ui.util.MyHtml htmlView = new ais.ui.util.MyHtml();
					htmlView.setContent(htmlBreakdown);
					// Tempatkan ringkasan breakdown (Tabel 2) PERSIS di posisi Tabel 1 (sebelum
					// gridMasterAsset yang disembunyikan) agar TIDAK ada ruang kosong saat Tabel 1
					// di-hide. (breakdown=Tidak: blok ini dilewati → hanya Tabel 1 yang tampil.)
					groupbox.insertBefore(htmlView, gridMasterAsset);
				}
			}
		} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/action/master/asset/helper/SaldoAwalPunyaMasterAssetHelper.java:441");
			// gagal render ringkasan breakdown -> biarkan tabel standar tetap tampil.
		}

		return groupbox;
	}

	@SuppressWarnings("unchecked")
	public void reloadNilai() {
		List<Row> rows = gridMasterAsset == null || gridMasterAsset.getRows() == null ? new ArrayList<Row>() : (java.util.List) gridMasterAsset.getRows().getChildren();

		Double totalSemua = 0.0;
		for (Row row : rows) {
			if (row.isVisible()) {
				SaldoAwalMasterAssetDetail saldoAwalMasterAssetDetail = (SaldoAwalMasterAssetDetail) row
						.getAttribute("saldoAwalMasterAssetDetail");

				Double j = saldoAwalMasterAssetDetail.getHargaTotal();

				totalSemua += doubleValue(j);
			}
		}

		if (footerTotalSemua != null) {
			footerTotalSemua.setLabel(Common.numberFormat.get().format(totalSemua));
		}

		if (saldoAwalMasterAsset != null && Double.valueOf(doubleValue(saldoAwalMasterAsset.getNilai())).intValue() != totalSemua.intValue()) {
			saldoAwalMasterAsset.setNilai(totalSemua);
			if (saldoAwalMasterAsset.getId() != null) {
				Common.refreshUpdate(saldoAwalMasterAsset);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void loadDataDetail(final SaldoAwalMasterAsset saldoAwalMasterAsset,
			PenerimaanPengadaanMasterAsset penerimaanPengadaanMasterAsset, boolean persetujuan) throws Exception {

		Rows rows = gridMasterAsset.getRows() == null ? new Rows() : gridMasterAsset.getRows();
		Common.clear(rows);
		rows.setParent(gridMasterAsset);
		Session session = HibernateUtil.currentSession();
		if (penerimaanPengadaanMasterAsset != null && penerimaanPengadaanMasterAsset.getId() != null) {

			List<PenerimaanPengadaanMasterAssetDetail> penerimaanPengadaanMasterAssetDetails = session
					.createCriteria(PenerimaanPengadaanMasterAssetDetail.class).addOrder(Order.desc("id"))
					.add(Restrictions.eq("penerimaanPengadaanMasterAsset", penerimaanPengadaanMasterAsset)).list();

			for (PenerimaanPengadaanMasterAssetDetail penerimaanPengadaanMasterAssetDetail : penerimaanPengadaanMasterAssetDetails) {
				SaldoAwalMasterAssetDetail saldoAwalMasterAssetDetail = (SaldoAwalMasterAssetDetail) session
						.createCriteria(SaldoAwalMasterAssetDetail.class).add(Restrictions
								.eq("penerimaanPengadaanMasterAssetDetail", penerimaanPengadaanMasterAssetDetail))
						.setMaxResults(1).uniqueResult();

				if (saldoAwalMasterAssetDetail == null) {
					saldoAwalMasterAssetDetail = new SaldoAwalMasterAssetDetail();
					saldoAwalMasterAssetDetail
							.setPenerimaanPengadaanMasterAssetDetail(penerimaanPengadaanMasterAssetDetail);

					if (saldoAwalMasterAsset != null && saldoAwalMasterAsset.getId() != null) {
						saldoAwalMasterAssetDetail.setSaldoAwal(saldoAwalMasterAsset);
						session.save(saldoAwalMasterAssetDetail);
						session.flush();

						penerimaanPengadaanMasterAssetDetail.setSaldoAwalMasterAssetDetail(saldoAwalMasterAssetDetail);
						session.update(penerimaanPengadaanMasterAssetDetail);
						session.flush();
					} else {
						Row row = new Row();
						row.setValign("top");
						row.setParent(rows);
						initRow(row, saldoAwalMasterAssetDetail, persetujuan);
					}
				}
			}
		}

		List<SaldoAwalMasterAssetDetail> saldoAwalMasterAssetDetails = new ArrayList<SaldoAwalMasterAssetDetail>();
		if (saldoAwalMasterAsset != null && saldoAwalMasterAsset.getId() != null) {
			Criteria criteriaDetail = session.createCriteria(SaldoAwalMasterAssetDetail.class)
					.createAlias("penerimaanPengadaanMasterAssetDetail", "penerimaanPengadaanMasterAssetDetail",
							Criteria.LEFT_JOIN);
			if (penerimaanPengadaanMasterAsset != null) {
				criteriaDetail.add(Restrictions.or(
						Restrictions.eq("penerimaanPengadaanMasterAssetDetail.penerimaanPengadaanMasterAsset",
								penerimaanPengadaanMasterAsset),
						Restrictions.eq("saldoAwal", saldoAwalMasterAsset)));
			} else {
				criteriaDetail.add(Restrictions.eq("saldoAwal", saldoAwalMasterAsset));
			}
			saldoAwalMasterAssetDetails = criteriaDetail.list();
		}

		for (SaldoAwalMasterAssetDetail saldoAwalMasterAssetDetail : saldoAwalMasterAssetDetails) {
			saldoAwalMasterAssetDetail.setSaldoAwal(saldoAwalMasterAsset);
			Row row = new Row();
			row.setValign("top");
			row.setParent(rows);
			initRow(row, saldoAwalMasterAssetDetail, persetujuan);
		}
	}

	public void initRow(final Row row, final SaldoAwalMasterAssetDetail saldoAwalMasterAssetDetail, boolean persetujuan)
			throws Exception {

		row.setValign("top");
		row.setAttribute("saldoAwalMasterAssetDetail", saldoAwalMasterAssetDetail);

		final MyDoublebox jumlah = new MyDoublebox(
				saldoAwalMasterAssetDetail.getJumlah() == null ? 0.0 : saldoAwalMasterAssetDetail.getJumlah());
		final MyDoublebox harga = new MyDoublebox(saldoAwalMasterAssetDetail.getHarga());

		final MyCheckboxConfig diskonDalamBentukPersen = new MyCheckboxConfig("Persen");
		diskonDalamBentukPersen.setChecked(saldoAwalMasterAssetDetail.getDiskonDalamBentukPersen());

		final MyDoublebox hargaPotongan = new MyDoublebox(saldoAwalMasterAssetDetail.getHargaPotongan());

		final Combobox persenPpn = new Combobox();
		Common.insertComboDanSemua(persenPpn, new String[] { "nama" }, "keterangan", JenisPajakPpn.class, "Tanpa PPN",
				Restrictions.eq("aktif", true));
		Common.selectComboItem(persenPpn, saldoAwalMasterAssetDetail.getJenisPajakPpn());
		Double dpp = Double.valueOf(doubleValue(saldoAwalMasterAssetDetail.getJumlah()) * doubleValue(saldoAwalMasterAssetDetail.getHarga()));
		Double ppn = Double.valueOf((doubleValue(saldoAwalMasterAssetDetail.getPersenPpn()) / 100.0) * doubleValue(dpp));
		final Label ppnNilai = new MyLabelKecil(Common.numberFormat.get().format(ppn));
		row.setValign("top");
		row.setAttribute("ppnNilai", ppnNilai);

		final Combobox persenPph = new Combobox();
		Common.insertComboDanSemua(persenPph, new String[] { "nama", "persen" }, "keterangan", JenisPajakBarang.class,
				"Tanpa Pajak", Restrictions.eq("aktif", true));
		Common.selectComboItem(persenPph, saldoAwalMasterAssetDetail.getJenisPajakBarang());

		if (saldoAwalMasterAssetDetail.getId() != null && saldoAwalMasterAssetDetail.getJenisPajakBarang() != null) {
			Pajak.buat(null, null, null, saldoAwalMasterAssetDetail);
		}

		final Label total = new MyLabelKecil(
				formatNumber(saldoAwalMasterAssetDetail.getHargaTotal()));

		Vbox a;
		(a = RevisiHelper.createNewRevisi(SaldoAwalMasterAssetDetail.class, saldoAwalMasterAssetDetail,
				saldoAwalMasterAssetDetail.getMasterAsset() == null ? ""
						: saldoAwalMasterAssetDetail.getMasterAsset().getNama()))
				.setParent(row);

		PenerimaanPengadaanMasterAssetDetail penerimaanPengadaanMasterAssetDetail = saldoAwalMasterAssetDetail
				.getPenerimaanPengadaanMasterAssetDetail();

		if (penerimaanPengadaanMasterAssetDetail != null) {

			RevisiHelper
					.createNewRevisi(PenerimaanPengadaanMasterAsset.class,
							penerimaanPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset(),
							penerimaanPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset().getKode())
					.setParent(a);

			if (penerimaanPengadaanMasterAssetDetail.getPemesananPengadaanMasterAssetDetail() != null) {
				RevisiHelper.createNewRevisi(PemesananPengadaanMasterAsset.class,
						penerimaanPengadaanMasterAssetDetail.getPemesananPengadaanMasterAssetDetail()
								.getPemesananPengadaanMasterAsset(),
						penerimaanPengadaanMasterAssetDetail.getPemesananPengadaanMasterAssetDetail()
								.getPemesananPengadaanMasterAsset().getKode())
						.setParent(a);
			}

			if (penerimaanPengadaanMasterAssetDetail.getPemesananPengadaanMasterAssetDetail() != null
					&& penerimaanPengadaanMasterAssetDetail.getPemesananPengadaanMasterAssetDetail()
							.getPermintaanPengadaanMasterAssetDetail() != null) {
				RevisiHelper.createNewRevisi(PermintaanPengadaanMasterAsset.class,
						penerimaanPengadaanMasterAssetDetail.getPemesananPengadaanMasterAssetDetail()
								.getPermintaanPengadaanMasterAssetDetail().getPermintaanPengadaanMasterAsset(),
						penerimaanPengadaanMasterAssetDetail.getPemesananPengadaanMasterAssetDetail()
								.getPermintaanPengadaanMasterAssetDetail().getPermintaanPengadaanMasterAsset()
								.getKode())
						.setParent(a);
			}

		} else {

			if (persetujuan) {
				new MyLabelKecil(saldoAwalMasterAssetDetail.getWorkspace() == null ? ""
						: saldoAwalMasterAssetDetail.getWorkspace().getNama()).setParent(row);
			} else {

				final AmbilDataWorkspaceBanbox workspace = new AmbilDataWorkspaceBanbox(false);
				workspace.setAttribute("workspace", saldoAwalMasterAssetDetail.getWorkspace());
				workspace.setValue(saldoAwalMasterAssetDetail.getWorkspace() == null ? ""
						: saldoAwalMasterAssetDetail.getWorkspace().getNama());
				workspace.setParent(row);
				workspace.setWidth("90%");

				workspace.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						Workspace work = (Workspace) workspace.getAttribute("workspace");

						Double saldo = JenisUangMukaAction.hitungSaldo(null, null, null,
								saldoAwalMasterAssetDetail.getId(), work,
								(saldoAwalMasterAssetDetail.getSaldoAwal() == null ? ais.ui.util.WaktuUtil.getDate() : saldoAwalMasterAssetDetail.getSaldoAwal().getTanggalPembuatan()));

						if (Common.bolehKonfigurasi("saldo_harus_cukup_sebelum_mengajukan_realisasi_anggaran")) {

							if (saldoAwalMasterAssetDetail.getHargaTotal().doubleValue() > saldo.doubleValue()) {

								workspace.setAttribute("workspace", null);
								workspace.setValue("");

								MyMessageboxConfig.show("Saldo anggaran tidak mencukupi. Nilai tagihan melebihi sisa saldo", "Peringatan",
										MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
								return;
							}
						}

						saldoAwalMasterAssetDetail.setWorkspace(work);
						row.setValign("top");
						row.setAttribute("saldoAwalMasterAssetDetail", saldoAwalMasterAssetDetail);
						if (saldoAwalMasterAssetDetail.getId() != null) {
							Session session = HibernateUtil.currentSession();
							simpanDetailAman(session, saldoAwalMasterAssetDetail);
						}

					}
				});
			}

		}

		MasterAsset masterAssetInfo = saldoAwalMasterAssetDetail.getMasterAsset();
		new Label(masterAssetInfo == null ? "" : safeText(masterAssetInfo.getMerk())).setParent(a);
		new Label(masterAssetInfo == null || masterAssetInfo.getJenisAsset() == null ? "" : safeText(masterAssetInfo.getJenisAsset().getNama())).setParent(a);
		new Label(masterAssetInfo == null || masterAssetInfo.getKelompokAsset() == null ? "" : safeText(masterAssetInfo.getKelompokAsset().getNama())).setParent(a);

		if (saldoAwalMasterAsset != null && saldoAwalMasterAsset.getJsonTermin() != null) {
			new MyLabelKecil(Common.numberFormat.get().format(saldoAwalMasterAssetDetail.getJumlah())).setParent(row);
			new MyLabelKecil(Common.numberFormat.get().format(saldoAwalMasterAssetDetail.getHarga())).setParent(row);
		} else {

			if (persetujuan) {
				new MyLabelKecil(Common.numberFormat.get().format(saldoAwalMasterAssetDetail.getJumlah()))
						.setParent(row);
			} else {
				(jumlah).setParent(row);
			}
			jumlah.setDisabled(isReadonly(saldoAwalMasterAssetDetail, edit));
			jumlah.setStyle("text-align:right");
			jumlah.setWidth("90%");
			jumlah.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Double saldo = Math.abs(jumlah.getValue() == null ? 0.0 : jumlah.getValue());
					jumlah.setValue(saldo);
					saldoAwalMasterAssetDetail.setJumlah(saldo);
					row.setValign("top");
					row.setAttribute("saldoAwalMasterAssetDetail", saldoAwalMasterAssetDetail);
					if (saldoAwalMasterAssetDetail.getId() != null) {
						Session session = HibernateUtil.currentSession();
						simpanDetailAman(session, saldoAwalMasterAssetDetail);
					}

					total.setValue(formatNumber(saldoAwalMasterAssetDetail.getHargaTotal()));
					Double dpp = Double.valueOf(doubleValue(saldoAwalMasterAssetDetail.getJumlah()) * doubleValue(saldoAwalMasterAssetDetail.getHarga()));
					Double ppn = Double.valueOf((doubleValue(saldoAwalMasterAssetDetail.getPersenPpn()) / 100.0) * doubleValue(dpp));
					ppnNilai.setValue(Common.numberFormat.get().format(ppn));
					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							reloadNilai();
						}
					});
				}
			});
			if (persetujuan) {
				new MyLabelKecil(Common.numberFormat.get().format(saldoAwalMasterAssetDetail.getHarga()))
						.setParent(row);
			} else {
				(harga).setParent(row);
			}
			harga.setDisabled(isReadonly(saldoAwalMasterAssetDetail, edit)
					|| (saldoAwalMasterAssetDetail.getDataPerMasterAsset() != null
							&& saldoAwalMasterAssetDetail.getDataPerMasterAsset()));
			harga.setStyle("text-align:right");
			harga.setWidth("90%");
			harga.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					Double saldo = Math.abs(harga.getValue() == null ? 0.0 : harga.getValue());
					harga.setValue(saldo);

					saldoAwalMasterAssetDetail.setHarga(saldo);
					if (saldoAwalMasterAssetDetail.getId() != null) {
						Session session = HibernateUtil.currentSession();
						simpanDetailAman(session, saldoAwalMasterAssetDetail);
					}

					total.setValue(formatNumber(saldoAwalMasterAssetDetail.getHargaTotal()));
					Double dpp = Double.valueOf(doubleValue(saldoAwalMasterAssetDetail.getJumlah()) * doubleValue(saldoAwalMasterAssetDetail.getHarga()));
					Double ppn = Double.valueOf((doubleValue(saldoAwalMasterAssetDetail.getPersenPpn()) / 100.0) * doubleValue(dpp));
					ppnNilai.setValue(Common.numberFormat.get().format(ppn));
					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							reloadNilai();
						}
					});
				}
			});
		}

		if (saldoAwalMasterAsset != null && saldoAwalMasterAsset.getJsonTermin() != null) {
			new Label(saldoAwalMasterAssetDetail.getDiskonDalamBentukPersen() ? "Ya" : "Tidak").setParent(row);
		} else if (persetujuan) {
			new Label(saldoAwalMasterAssetDetail.getDiskonDalamBentukPersen() ? "Ya" : "Tidak").setParent(row);
		} else {
			(diskonDalamBentukPersen).setParent(row);
		}

		diskonDalamBentukPersen.setDisabled(persetujuan);
		diskonDalamBentukPersen.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Session session = HibernateUtil.currentSession();
				saldoAwalMasterAssetDetail.setDiskonDalamBentukPersen(diskonDalamBentukPersen.isChecked());
				simpanDetailAman(session, saldoAwalMasterAssetDetail);

				total.setValue(formatNumber(saldoAwalMasterAssetDetail.getHargaTotal()));

				reloadNilaiTimer();

			}
		});

		if (saldoAwalMasterAsset != null && saldoAwalMasterAsset.getJsonTermin() != null) {
			new MyLabelKecil(Common.numberFormat.get().format(saldoAwalMasterAssetDetail.getHargaPotongan()))
					.setParent(row);
		} else if (persetujuan) {
			new MyLabelKecil(Common.numberFormat.get().format(saldoAwalMasterAssetDetail.getHargaPotongan()))
					.setParent(row);
		} else {
			(hargaPotongan).setParent(row);
		}

		hargaPotongan.setDisabled(persetujuan);
		hargaPotongan.setStyle("text-align:right");
		hargaPotongan.setWidth("90%");
		hargaPotongan.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Session session = HibernateUtil.currentSession();
				saldoAwalMasterAssetDetail.setHargaPotongan(hargaPotongan.getValue());
				simpanDetailAman(session, saldoAwalMasterAssetDetail);

				total.setValue(formatNumber(saldoAwalMasterAssetDetail.getHargaTotal()));

				reloadNilaiTimer();
			}
		});

		if (saldoAwalMasterAsset != null && saldoAwalMasterAsset.getJsonTermin() != null) {
			new Label(saldoAwalMasterAssetDetail.getJenisPajakPpn() == null ? ""
					: saldoAwalMasterAssetDetail.getJenisPajakPpn().getNama()).setParent(row);
		} else if (persetujuan) {
			new Label(saldoAwalMasterAssetDetail.getJenisPajakPpn() == null ? ""
					: saldoAwalMasterAssetDetail.getJenisPajakPpn().getNama()).setParent(row);
		} else {
			(persenPpn).setParent(row);
		}

		persenPpn.setDisabled(persetujuan);
		persenPpn.setStyle("text-align:right");
		persenPpn.setWidth("90%");
		persenPpn.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Session session = HibernateUtil.currentSession();
				saldoAwalMasterAssetDetail.setJenisPajakPpn((JenisPajakPpn) (persenPpn.getSelectedItem() == null ? null
						: persenPpn.getSelectedItem().getValue()));
				if (saldoAwalMasterAssetDetail.getId() != null) {
					simpanDetailAman(session, saldoAwalMasterAssetDetail);
				}

				total.setValue(formatNumber(saldoAwalMasterAssetDetail.getHargaTotal()));
				Double dpp = Double.valueOf(doubleValue(saldoAwalMasterAssetDetail.getJumlah()) * doubleValue(saldoAwalMasterAssetDetail.getHarga()));
				Double ppn = Double.valueOf((doubleValue(saldoAwalMasterAssetDetail.getPersenPpn()) / 100.0) * doubleValue(dpp));
				ppnNilai.setValue(Common.numberFormat.get().format(ppn));
				reloadNilaiTimer();
			}
		});

		ppnNilai.setStyle("text-align:right");
		ppnNilai.setParent(row);

		if (saldoAwalMasterAsset != null && saldoAwalMasterAsset.getJsonTermin() != null) {
			new Label(saldoAwalMasterAssetDetail.getJenisPajakBarang() == null ? ""
					: saldoAwalMasterAssetDetail.getJenisPajakBarang().getNama()).setParent(row);
		} else if (persetujuan) {
			new Label(saldoAwalMasterAssetDetail.getJenisPajakBarang() == null ? ""
					: saldoAwalMasterAssetDetail.getJenisPajakBarang().getNama()).setParent(row);
		} else {
			(persenPph).setParent(row);
		}

		persenPph.setDisabled(persetujuan);
		persenPph.setStyle("text-align:right");
		persenPph.setWidth("90%");
		persenPph.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Session session = HibernateUtil.currentSession();
				saldoAwalMasterAssetDetail
						.setJenisPajakBarang((JenisPajakBarang) (persenPph.getSelectedItem() == null ? null
								: persenPph.getSelectedItem().getValue()));
				// Guard: hanya update jika detail DAN induk saldoAwalMasterAsset sudah tersimpan
				// (menghindari TransientObjectException saat flush referensi ke induk yang belum ada di DB)
				if (saldoAwalMasterAssetDetail.getId() != null
						&& saldoAwalMasterAsset != null && saldoAwalMasterAsset.getId() != null) {
					simpanDetailAman(session, saldoAwalMasterAssetDetail);
				}

				if (saldoAwalMasterAssetDetail.getId() != null
						&& saldoAwalMasterAssetDetail.getJenisPajakBarang() != null) {
					Pajak.buat(null, null, null, saldoAwalMasterAssetDetail);
				}

				total.setValue(formatNumber(saldoAwalMasterAssetDetail.getHargaTotal()));

				reloadNilaiTimer();
			}
		});

		new MyLabelKecil(Common.numberFormat.get().format(saldoAwalMasterAssetDetail.hitungPpn())).setParent(row);
		new MyLabelKecil(Common.numberFormat.get().format(saldoAwalMasterAssetDetail.hitungPph())).setParent(row);

		total.setStyle("text-align:right");
		total.setParent(row);

		final MyTextbox keterangan = new MyTextbox(
				saldoAwalMasterAssetDetail.getKeterangan() == null ? "" : saldoAwalMasterAssetDetail.getKeterangan());
		keterangan.setWidth("90%");
		keterangan.setHeight("95%");

		if (saldoAwalMasterAsset != null && saldoAwalMasterAsset.getJsonTermin() != null) {
			new Label(saldoAwalMasterAssetDetail.getKeterangan()).setParent(row);
		} else if (persetujuan) {
			new Label(saldoAwalMasterAssetDetail.getKeterangan()).setParent(row);
		} else {
			keterangan.setParent(row);
		}

		keterangan.setDisabled(isReadonly(saldoAwalMasterAssetDetail, edit));
		keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				saldoAwalMasterAssetDetail.setKeterangan(keterangan.getValue());
				row.setValign("top");
				row.setAttribute("saldoAwalMasterAssetDetail", saldoAwalMasterAssetDetail);
				if (saldoAwalMasterAssetDetail.getId() != null) {
					Session session = HibernateUtil.currentSession();
					simpanDetailAman(session, saldoAwalMasterAssetDetail);
				}
			}
		});

		Vbox hbox = new Vbox();
		hbox.setParent(row);

		// Tombol CETAK breakdown per-baris (di samping Hapus). SELALU tampil (tak digerbang
		// "delete") agar breakdown yang sudah dibuat tetap bisa dicetak walau tagihan sudah
		// disetujui. Mencetak breakdown milik SaldoAwalMasterAsset terkait (reuse method lama).
		MyToolbarbuttonConfig btnCetakBreakdown = new MyToolbarbuttonConfig("Cetak", "/img/svg/list-box-line.svg");
		btnCetakBreakdown.setOrient("vertical");
		btnCetakBreakdown.setTooltiptext("Cetak breakdown item tagihan vendor yang telah dibuat");
		btnCetakBreakdown.setParent(hbox);
		btnCetakBreakdown.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				BreakdownTagihanVendorHelper.cetakBreakdown(saldoAwalMasterAsset);
			}
		});


		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
		button.setOrient("vertical");
		button.setTooltiptext("Hapus Data");
		button.setVisible(delete);
		button.setParent(hbox);

		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									if (saldoAwalMasterAssetDetail.getId() != null) {
										Session session = HibernateUtil.currentSession();
										session.delete(saldoAwalMasterAssetDetail);
									}
									row.setVisible(false);
									row.detach();
								}

							}
						});

			}
		});
	}

}
