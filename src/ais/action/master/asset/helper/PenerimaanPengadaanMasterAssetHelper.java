package ais.action.master.asset.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.JenisPajakBarang;
import ais.database.model.asset.JenisPajakPpn;
import ais.database.model.asset.MasterAsset;
import ais.database.model.asset.PemesananPengadaanMasterAsset;
import ais.database.model.asset.PenerimaanPengadaanMasterAsset;
import ais.database.model.asset.PenerimaanPengadaanMasterAssetDetail;
import ais.database.model.asset.PermintaanPengadaanMasterAsset;
import ais.database.model.file.LampiranLain;
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

public class PenerimaanPengadaanMasterAssetHelper {

	private MyGrid gridMasterAsset;
	private boolean add = false;
	private boolean edit = false;
	private boolean delete = false;
	private Footer footerTotalSemua;
	private double totalSemua = 0.0;
	private boolean persetujuan = false;
	private boolean beliLangsung = false;

	public PenerimaanPengadaanMasterAssetHelper(MyGrid gridMasterAsset) {
		this.gridMasterAsset = gridMasterAsset;

	}

	public MyGroupboxStyled initDetail(final PenerimaanPengadaanMasterAsset penerimaanPengadaanMasterAsset,
			boolean persetujuan, final MyCheckboxConfig tampaPemesanan) throws Exception {
		this.beliLangsung = (penerimaanPengadaanMasterAsset.getPemesananPengadaanMasterAsset() != null
				&& penerimaanPengadaanMasterAsset.getPemesananPengadaanMasterAsset().getPembelianLangsung());
		this.persetujuan = persetujuan || beliLangsung;
		MyGroupboxStyled myGroupboxStyled = new MyGroupboxStyled();
		myGroupboxStyled.appendChild(new MyCaptionStyled("Daftar Penerimaan Barang/Jasa"));

		edit = penerimaanPengadaanMasterAsset.getDisetujuiOleh() == null;
		add = penerimaanPengadaanMasterAsset.getDisetujuiOleh() == null;
		delete = penerimaanPengadaanMasterAsset.getDisetujuiOleh() == null;

		Toolbar toolbar = new Toolbar();
		toolbar.setVisible(!this.persetujuan);
		toolbar.setHeight("30px");
		toolbar.setParent(myGroupboxStyled);

		final MyToolbarbuttonConfig add = new MyToolbarbuttonConfig("Ambil Data Barang/Jasa", "/img/new.gif");
		add.setVisible(PenerimaanPengadaanMasterAssetHelper.this.add && tampaPemesanan.isChecked());
		add.setParent(toolbar);
		add.setTooltiptext("Tambah");
		add.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				List<MasterAsset> masterAssets = new ArrayList<MasterAsset>();
				List<Row> myrows = gridMasterAsset.getRows().getChildren();
				for (Row row : myrows) {
					masterAssets.add(((PenerimaanPengadaanMasterAssetDetail) row
							.getAttribute("penerimaanPengadaanMasterAssetDetail")).getMasterAsset());
				}
				AmbilDataMasterAssetBanyak ambilDataMasterAssetBanyak = new AmbilDataMasterAssetBanyak(masterAssets,
						null);
				ambilDataMasterAssetBanyak.setHeight("95%");
				ambilDataMasterAssetBanyak.setWidth("90%");
				ambilDataMasterAssetBanyak.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				ambilDataMasterAssetBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<MasterAsset> masterAssets = (List<MasterAsset>) arg0.getData();
						for (MasterAsset masterAsset : masterAssets) {
							PenerimaanPengadaanMasterAssetDetail penerimaanPengadaanMasterAssetDetail = new PenerimaanPengadaanMasterAssetDetail();
							penerimaanPengadaanMasterAssetDetail.setMasterAsset(masterAsset);
							penerimaanPengadaanMasterAssetDetail.setJumlah(1.0);
							penerimaanPengadaanMasterAssetDetail.setKeterangan("");
							penerimaanPengadaanMasterAssetDetail
									.setPenerimaanPengadaanMasterAsset(penerimaanPengadaanMasterAsset);

							if (penerimaanPengadaanMasterAsset.getId() != null) {
								Session session = HibernateUtil.currentSession();
								session.save(penerimaanPengadaanMasterAssetDetail);
							}

							Rows rows = gridMasterAsset.getRows() == null ? new Rows() : gridMasterAsset.getRows();
							rows.setParent(gridMasterAsset);
							Row row = new Row();
							row.setValign("top");
							row.setParent(rows);
							initRow(row, penerimaanPengadaanMasterAssetDetail);
						}
						eventListenerHitungUlang.onEvent(arg0);
					}
				});

				ambilDataMasterAssetBanyak.onModal();

			}
		});

		tampaPemesanan.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				add.setVisible(PenerimaanPengadaanMasterAssetHelper.this.add && tampaPemesanan.isChecked());
			}
		});

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		cari.setParent(toolbar);
		cari.setVisible(penerimaanPengadaanMasterAsset.getId() != null);
		cari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDataDetail(penerimaanPengadaanMasterAsset);
			}
		});

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("History", "/img/jadwal.png");
		button.setVisible(penerimaanPengadaanMasterAsset.getId() != null);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				RevisiPenerimaanPengadaanMasterAssetDetailHelper revisiHelper = new RevisiPenerimaanPengadaanMasterAssetDetailHelper(
						penerimaanPengadaanMasterAsset, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								loadDataDetail(penerimaanPengadaanMasterAsset);
							}
						});
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(revisiHelper);
				revisiHelper.setVisible(true);
				revisiHelper.onModal();

			}

		});
		button.setParent(toolbar);

		// === Tombol PULIHKAN item dari tabel AUDIT (Envers) ===
		// Saat disposisi/persetujuan, toolbar utama (berisi tombol History) DISEMBUNYIKAN, sehingga
		// bila item "Daftar Penerimaan Barang/Jasa" tidak sengaja terhapus, pengguna tidak punya
		// jalan mengembalikannya. Toolbar audit ini SELALU tampil di mode persetujuan/disposisi dan
		// membuka jendela riwayat audit yang sudah mendukung restore (per revisi & "Restore Terbaru"
		// massal) — termasuk mengembalikan baris yang sudah dihapus ke kondisi sebelum dihapus.
		Toolbar toolbarAudit = new Toolbar();
		toolbarAudit.setHeight("30px");
		toolbarAudit.setVisible(this.persetujuan && penerimaanPengadaanMasterAsset.getId() != null);
		toolbarAudit.setParent(myGroupboxStyled);

		MyToolbarbuttonConfig restoreBtn = new MyToolbarbuttonConfig("Pulihkan Barang/Jasa (Audit)",
				"/img/refresh.gif");
		restoreBtn.setTooltiptext(
				"Tampilkan riwayat audit & pulihkan item Barang/Jasa yang tidak sengaja terhapus dari daftar ini");
		restoreBtn.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				RevisiPenerimaanPengadaanMasterAssetDetailHelper revisiHelper = new RevisiPenerimaanPengadaanMasterAssetDetailHelper(
						penerimaanPengadaanMasterAsset, new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								loadDataDetail(penerimaanPengadaanMasterAsset);
							}
						});
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(revisiHelper);
				revisiHelper.setVisible(true);
				revisiHelper.onModal();
			}
		});
		restoreBtn.setParent(toolbarAudit);

		gridMasterAsset.setParent(myGroupboxStyled);
		gridMasterAsset.setWidth("100%");
		gridMasterAsset.setHeight("100%");
		gridMasterAsset.setStyle("min-height:350px");
		gridMasterAsset.setMold("paging");
		gridMasterAsset.getPagingChild().setMold("os");

		columnsData(penerimaanPengadaanMasterAsset == null ? null
				: penerimaanPengadaanMasterAsset.getPemesananPengadaanMasterAsset(), persetujuan);

		loadDataDetail(penerimaanPengadaanMasterAsset);

		return myGroupboxStyled;
	}

	public void columnsData(PemesananPengadaanMasterAsset pemesananPengadaanMasterAsset, boolean persetujuan)
			throws Exception {

		this.beliLangsung = (pemesananPengadaanMasterAsset != null
				&& pemesananPengadaanMasterAsset.getPembelianLangsung());
		this.persetujuan = persetujuan || beliLangsung;

		Columns columns = gridMasterAsset.getColumns() == null ? new Columns() : gridMasterAsset.getColumns();
		Common.clear(columns);
		columns.setParent(gridMasterAsset);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode/Nama/Gambar");
		column.setWidth("120px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Qty");
		column.setAlign("right");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Diterima");
		column.setAlign("right");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Selisih");
		column.setWidth("5%");
		column.setAlign("right");

		if (!beliLangsung) {
			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Harga");
			column.setAlign("right");
			column.setWidth("8%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Diskon");
			column.setAlign("right");
			column.setWidth("8%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("PPN");
			column.setAlign("right");
			column.setWidth("10%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Nilai PPN");
			column.setAlign("right");
			column.setWidth("8%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("PPH");
			column.setAlign("right");
			column.setWidth("5%");

		}

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Total");
		column.setAlign("right");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan / Kondisi");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		footerTotalSemua = new Footer(Common.numberFormat.get().format(totalSemua));

		Foot foot = gridMasterAsset.getFoot() == null ? new Foot() : gridMasterAsset.getFoot();
		Common.clear(foot);
		foot.setParent(gridMasterAsset);

		Footer footer = new Footer("Total");
		foot.appendChild(footer);

		footer = new Footer();
		foot.appendChild(footer);

		footer = new Footer();
		foot.appendChild(footer);

		footer = new Footer();
		foot.appendChild(footer);

		if (!beliLangsung) {

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

		}

		foot.appendChild(footerTotalSemua);

		footer = new Footer();
		foot.appendChild(footer);

		footer = new Footer();
		foot.appendChild(footer);
	}

	@SuppressWarnings("unchecked")
	public void setTermin(JSONObject termin, PenerimaanPengadaanMasterAsset penerimaanPengadaanMasterAsset) {
		List<Row> rows = gridMasterAsset.getRows().getChildren();
		for (final Row row : rows) {
			if (row.isVisible()) {
				try {
					final PenerimaanPengadaanMasterAssetDetail penerimaanPengadaanMasterAssetDetail = (PenerimaanPengadaanMasterAssetDetail) row
							.getAttribute("penerimaanPengadaanMasterAssetDetail");
					penerimaanPengadaanMasterAssetDetail
							.setPenerimaanPengadaanMasterAsset(penerimaanPengadaanMasterAsset);

					final MyDoublebox hargaBeli = (MyDoublebox) row.getAttribute("hargaBeli");

					JSONObject jsonObject = new JSONObject(penerimaanPengadaanMasterAsset.getJsonTermin());
					Double penagihan = 0.0;
					if (!jsonObject.isNull("penagihan")) {
						penagihan = jsonObject.getDouble("penagihan");
					}

					hargaBeli.setValue(penagihan);

					penerimaanPengadaanMasterAssetDetail.setHargaBeli(hargaBeli.getValue());

					row.setValign("top");
					row.setAttribute("penerimaanPengadaanMasterAssetDetail", penerimaanPengadaanMasterAssetDetail);
					if (penerimaanPengadaanMasterAssetDetail.getId() != null) {
						Session session = HibernateUtil.currentSession();
						Common.refreshUpdate(session, (penerimaanPengadaanMasterAssetDetail));
					}

					Label total = (Label) row.getAttribute("total");
					total.setValue(Common.numberFormat.get().format(penerimaanPengadaanMasterAssetDetail.getHargaTotal()));

					eventListenerHitungUlang.onEvent(null);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/asset/helper/PenerimaanPengadaanMasterAssetHelper.java:385");
				}
			}
		}
	}

	public EventListener eventListenerHitungUlang = new EventListener() {

		@SuppressWarnings("unchecked")
		@Override
		public void onEvent(Event arg0) throws Exception {
			List<Row> rows = gridMasterAsset.getRows().getChildren();

			totalSemua = 0.0;
			for (Row row : rows) {
				if (row.isVisible()) {
					PenerimaanPengadaanMasterAssetDetail penerimaanPengadaanMasterAssetDetail = (PenerimaanPengadaanMasterAssetDetail) row
							.getAttribute("penerimaanPengadaanMasterAssetDetail");

					Double j = penerimaanPengadaanMasterAssetDetail.getHargaTotal();

					totalSemua += j;
				}
			}

			footerTotalSemua.setLabel(Common.numberFormat.get().format(totalSemua));
		}
	};

	@SuppressWarnings("unchecked")
	private void loadDataDetail(final PenerimaanPengadaanMasterAsset penerimaanPengadaanMasterAsset) throws Exception {

		List<PenerimaanPengadaanMasterAssetDetail> penerimaanPengadaanMasterAssetDetails = penerimaanPengadaanMasterAsset == null
				|| penerimaanPengadaanMasterAsset.getId() == null
						? new ArrayList<PenerimaanPengadaanMasterAssetDetail>()
						: HibernateUtil.currentSession().createCriteria(PenerimaanPengadaanMasterAssetDetail.class)
								.add(Restrictions.eq("penerimaanPengadaanMasterAsset", penerimaanPengadaanMasterAsset))
								.list();

		Rows rows = gridMasterAsset.getRows() == null ? new Rows() : gridMasterAsset.getRows();
		rows.setParent(gridMasterAsset);
		Common.clear(rows);
		for (PenerimaanPengadaanMasterAssetDetail penerimaanPengadaanMasterAssetDetail : penerimaanPengadaanMasterAssetDetails) {
			Row row = new Row();
			row.setValign("top");
			row.setParent(rows);
			initRow(row, penerimaanPengadaanMasterAssetDetail);
		}

		Common.createDefaultTimer(eventListenerHitungUlang);
	}

	public void initRow(final Row row, final PenerimaanPengadaanMasterAssetDetail penerimaanPengadaanMasterAssetDetail)
			throws Exception {
		row.setValign("top");
		row.setValign("top");
		row.setAttribute("penerimaanPengadaanMasterAssetDetail", penerimaanPengadaanMasterAssetDetail);

		Vbox vbox = new Vbox();
		vbox.setParent(row);

		new Label(penerimaanPengadaanMasterAssetDetail.getMasterAsset() == null ? ""
				: penerimaanPengadaanMasterAssetDetail.getMasterAsset().getKode()).setParent(vbox);

		Hbox hbox = new Hbox();
		hbox.setParent(vbox);

		LampiranLain.createDownloadUploadFileLain(hbox, penerimaanPengadaanMasterAssetDetail.getMasterAsset().getId(),
				LampiranLain.GAMBAR_MASTER_ASSET, "Gambar", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

					}
				}, null, false, false, false, !persetujuan, null, true, true);

		Vbox a;
		(a = RevisiHelper.createNewRevisi(PenerimaanPengadaanMasterAssetDetail.class,
				penerimaanPengadaanMasterAssetDetail, penerimaanPengadaanMasterAssetDetail.getMasterAsset() == null ? ""
						: penerimaanPengadaanMasterAssetDetail.getMasterAsset().getNama()))
				.setParent(vbox);

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
							.getPermintaanPengadaanMasterAssetDetail().getPermintaanPengadaanMasterAsset().getKode())
					.setParent(a);
		}

		final Label jumlah = new MyLabelKecil(
				Common.numberFormat.get().format(penerimaanPengadaanMasterAssetDetail.getJumlah()));

		final MyDoublebox diterima = new MyDoublebox(penerimaanPengadaanMasterAssetDetail.getDiterima() == null ? 0.0
				: penerimaanPengadaanMasterAssetDetail.getDiterima());

		final Label sisa = new Label(penerimaanPengadaanMasterAssetDetail.getJumlah() == null
				|| penerimaanPengadaanMasterAssetDetail.getDiterima() == null ? ""
						: Common.numberFormat.get().format(penerimaanPengadaanMasterAssetDetail.getJumlah()
								- penerimaanPengadaanMasterAssetDetail.getDiterima()));

		final Label total = new MyLabelKecil(
				Common.numberFormat.get().format(penerimaanPengadaanMasterAssetDetail.getHargaTotal()));
		row.setValign("top");
		row.setAttribute("total", total);

		Double dpp = (penerimaanPengadaanMasterAssetDetail.getDiterima()
				* penerimaanPengadaanMasterAssetDetail.getHargaBeli());
		Double potongan = penerimaanPengadaanMasterAssetDetail.getDiskonDalamBentukPersen()
				? ((penerimaanPengadaanMasterAssetDetail.getHargaPotongan() / 100.0) * dpp)
				: penerimaanPengadaanMasterAssetDetail.getHargaPotongan();
		dpp = dpp - potongan;
		Double ppn = ((penerimaanPengadaanMasterAssetDetail.getPersenPpn() / 100.0) * dpp);
		final Label ppnNilai = new MyLabelKecil(Common.numberFormat.get().format(ppn));
		row.setValign("top");
		row.setAttribute("ppnNilai", ppnNilai);

		final MyDoublebox hargaPotongan = new MyDoublebox(penerimaanPengadaanMasterAssetDetail.getHargaPotongan());
		final MyCheckboxConfig diskonDalamBentukPersen = new MyCheckboxConfig("Persen");
		diskonDalamBentukPersen.setChecked(penerimaanPengadaanMasterAssetDetail.getDiskonDalamBentukPersen());
		final Combobox persenPpn = new Combobox();
		Common.insertComboDanSemua(persenPpn, new String[] { "nama" }, "keterangan", JenisPajakPpn.class, "Tanpa PPN",
				Restrictions.eq("aktif", true));
		Common.selectComboItem(persenPpn, penerimaanPengadaanMasterAssetDetail.getJenisPajakPpn());

		final Combobox persenPph = new Combobox();
		Common.insertComboDanSemua(persenPph, new String[] { "nama", "persen" }, "keterangan", JenisPajakBarang.class,
				"Tanpa Pajak", Restrictions.eq("aktif", true));
		Common.selectComboItem(persenPph, penerimaanPengadaanMasterAssetDetail.getJenisPajakBarang());

		final MyDoublebox hargaBeli = new MyDoublebox(penerimaanPengadaanMasterAssetDetail.getHargaBeli() == null ? 0.0
				: penerimaanPengadaanMasterAssetDetail.getHargaBeli());

		row.setValign("top");
		row.setAttribute("hargaBeli", hargaBeli);

		try {
			if (persetujuan) {
				new MyLabelKecil(Common.numberFormat.get().format(penerimaanPengadaanMasterAssetDetail.getJumlah()))
						.setParent(row);
			} else {
				(jumlah).setParent(row);
			}
		} catch (Exception e) {
			new Label(penerimaanPengadaanMasterAssetDetail.getJumlah() + "").setParent(row);
		}

		try {
			if (persetujuan) {
				new MyLabelKecil(Common.numberFormat.get().format(penerimaanPengadaanMasterAssetDetail.getDiterima()))
						.setParent(row);
			} else {
				(diterima).setParent(row);
			}
		} catch (Exception e) {
			new Label(penerimaanPengadaanMasterAssetDetail.getDiterima() + "").setParent(row);
		}

		diterima.setDisabled((penerimaanPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset() != null
				&& penerimaanPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset().getDisetujuiOleh() != null)
				|| !edit);
		diterima.setStyle("text-align:right");
		diterima.setWidth("90%");
		diterima.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Session session = HibernateUtil.currentSession();
				penerimaanPengadaanMasterAssetDetail.setDiterima(diterima.getValue());

				row.setValign("top");
				row.setAttribute("penerimaanPengadaanMasterAssetDetail", penerimaanPengadaanMasterAssetDetail);
				if (penerimaanPengadaanMasterAssetDetail.getId() != null) {
					Common.refreshUpdate(session, (penerimaanPengadaanMasterAssetDetail));
				}

				String mysisa = penerimaanPengadaanMasterAssetDetail.getJumlah() == null
						|| penerimaanPengadaanMasterAssetDetail.getDiterima() == null ? ""
								: Common.numberFormat.get().format(penerimaanPengadaanMasterAssetDetail.getJumlah()
										- penerimaanPengadaanMasterAssetDetail.getDiterima());
				sisa.setValue(mysisa);

				total.setValue(Common.numberFormat.get().format(penerimaanPengadaanMasterAssetDetail.getHargaTotal()));

				Double dpp = (penerimaanPengadaanMasterAssetDetail.getDiterima()
						* penerimaanPengadaanMasterAssetDetail.getHargaBeli());

				Double potongan = penerimaanPengadaanMasterAssetDetail.getDiskonDalamBentukPersen()
						? ((penerimaanPengadaanMasterAssetDetail.getHargaPotongan() / 100.0) * dpp)
						: penerimaanPengadaanMasterAssetDetail.getHargaPotongan();
				dpp = dpp - potongan;

				Double ppn = ((penerimaanPengadaanMasterAssetDetail.getPersenPpn() / 100.0) * dpp);
				ppnNilai.setValue(Common.numberFormat.get().format(ppn));

				eventListenerHitungUlang.onEvent(null);
			}
		});

		(sisa).setParent(row);

		if (!beliLangsung) {
			if (penerimaanPengadaanMasterAssetDetail != null
					&& !penerimaanPengadaanMasterAssetDetail.getMasterAsset().getHargaBolehDiubah()) {
				new MyLabelKecil(Common.numberFormat.get().format(penerimaanPengadaanMasterAssetDetail.getHargaBeli()))
						.setParent(row);
			} else if (persetujuan) {
				new MyLabelKecil(Common.numberFormat.get().format(penerimaanPengadaanMasterAssetDetail.getHargaBeli()))
						.setParent(row);
			} else {
				(hargaBeli).setParent(row);
			}
			hargaBeli.setDisabled((penerimaanPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset() != null
					&& penerimaanPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset()
							.getDisetujuiOleh() != null)
					|| !edit);
			hargaBeli.setStyle("text-align:right");
			hargaBeli.setWidth("90%");
			hargaBeli.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					penerimaanPengadaanMasterAssetDetail.setHargaBeli(hargaBeli.getValue());
					penerimaanPengadaanMasterAssetDetail.setHargaBeliDiEntry(hargaBeli.getValue());

					row.setValign("top");
					row.setAttribute("penerimaanPengadaanMasterAssetDetail", penerimaanPengadaanMasterAssetDetail);
					if (penerimaanPengadaanMasterAssetDetail.getId() != null) {
						Common.refreshUpdate(session, (penerimaanPengadaanMasterAssetDetail));
					}

					MasterAsset masterAsset = penerimaanPengadaanMasterAssetDetail.getMasterAsset();
					session.refresh(masterAsset);
					masterAsset.setHargaBeliDefault(hargaBeli.getValue());
					Common.refreshUpdate(session, masterAsset);

					total.setValue(Common.numberFormat.get().format(penerimaanPengadaanMasterAssetDetail.getHargaTotal()));

					Double dpp = (penerimaanPengadaanMasterAssetDetail.getDiterima()
							* penerimaanPengadaanMasterAssetDetail.getHargaBeli());

					Double potongan = penerimaanPengadaanMasterAssetDetail.getDiskonDalamBentukPersen()
							? ((penerimaanPengadaanMasterAssetDetail.getHargaPotongan() / 100.0) * dpp)
							: penerimaanPengadaanMasterAssetDetail.getHargaPotongan();
					dpp = dpp - potongan;

					Double ppn = ((penerimaanPengadaanMasterAssetDetail.getPersenPpn() / 100.0) * dpp);
					ppnNilai.setValue(Common.numberFormat.get().format(ppn));

					eventListenerHitungUlang.onEvent(null);
				}
			});

			vbox = new Vbox();
			vbox.setWidth("99%");
			vbox.setParent(row);

			if (persetujuan) {
				new Label(penerimaanPengadaanMasterAssetDetail.getDiskonDalamBentukPersen() ? "Ya" : "Tidak")
						.setParent(vbox);
			} else {
				(diskonDalamBentukPersen).setParent(vbox);
			}

			diskonDalamBentukPersen
					.setDisabled((penerimaanPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset() != null
							&& penerimaanPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset()
									.getDisetujuiOleh() != null)
							|| !edit);
			diskonDalamBentukPersen.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					penerimaanPengadaanMasterAssetDetail
							.setDiskonDalamBentukPersen(diskonDalamBentukPersen.isChecked());
					// Samakan dgn handler diterima/hargaBeli: hanya simpan bila detail SUDAH punya id.
					// Detail baru (id null) yg parent-nya belum tersimpan akan memicu
					// TransientObjectException saat flush; nilainya sudah ter-set di objek dan ikut
					// tersimpan saat header disimpan.
					if (penerimaanPengadaanMasterAssetDetail.getId() != null) {
						Common.refreshUpdate(session, (penerimaanPengadaanMasterAssetDetail));
					}

					total.setValue(Common.numberFormat.get().format(penerimaanPengadaanMasterAssetDetail.getHargaTotal()));

					Double dpp = (penerimaanPengadaanMasterAssetDetail.getDiterima()
							* penerimaanPengadaanMasterAssetDetail.getHargaBeli());

					Double potongan = penerimaanPengadaanMasterAssetDetail.getDiskonDalamBentukPersen()
							? ((penerimaanPengadaanMasterAssetDetail.getHargaPotongan() / 100.0) * dpp)
							: penerimaanPengadaanMasterAssetDetail.getHargaPotongan();
					dpp = dpp - potongan;

					Double ppn = ((penerimaanPengadaanMasterAssetDetail.getPersenPpn() / 100.0) * dpp);
					ppnNilai.setValue(Common.numberFormat.get().format(ppn));

					eventListenerHitungUlang.onEvent(null);
				}
			});

			if (persetujuan) {
				new MyLabelKecil(Common.numberFormat.get().format(penerimaanPengadaanMasterAssetDetail.getHargaPotongan()))
						.setParent(vbox);
			} else {
				(hargaPotongan).setParent(vbox);
			}
			hargaPotongan.setDisabled((penerimaanPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset() != null
					&& penerimaanPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset()
							.getDisetujuiOleh() != null)
					|| !edit);
			hargaPotongan.setStyle("text-align:right");
			hargaPotongan.setWidth("90%");
			hargaPotongan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					penerimaanPengadaanMasterAssetDetail.setHargaPotongan(hargaPotongan.getValue());

					row.setValign("top");
					row.setAttribute("penerimaanPengadaanMasterAssetDetail", penerimaanPengadaanMasterAssetDetail);
					if (penerimaanPengadaanMasterAssetDetail.getId() != null) {
						Common.refreshUpdate(session, (penerimaanPengadaanMasterAssetDetail));
					}

					total.setValue(Common.numberFormat.get().format(penerimaanPengadaanMasterAssetDetail.getHargaTotal()));
					Double dpp = (penerimaanPengadaanMasterAssetDetail.getDiterima()
							* penerimaanPengadaanMasterAssetDetail.getHargaBeli());

					Double potongan = penerimaanPengadaanMasterAssetDetail.getDiskonDalamBentukPersen()
							? ((penerimaanPengadaanMasterAssetDetail.getHargaPotongan() / 100.0) * dpp)
							: penerimaanPengadaanMasterAssetDetail.getHargaPotongan();
					dpp = dpp - potongan;

					Double ppn = ((penerimaanPengadaanMasterAssetDetail.getPersenPpn() / 100.0) * dpp);
					ppnNilai.setValue(Common.numberFormat.get().format(ppn));

					eventListenerHitungUlang.onEvent(null);
				}
			});

			if (persetujuan) {
				new Label(penerimaanPengadaanMasterAssetDetail.getJenisPajakPpn() == null ? ""
						: penerimaanPengadaanMasterAssetDetail.getJenisPajakPpn().getNama()).setParent(row);
			} else {
				(persenPpn).setParent(row);
			}

			persenPpn.setDisabled((penerimaanPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset() != null
					&& penerimaanPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset()
							.getDisetujuiOleh() != null)
					|| !edit);
			persenPpn.setStyle("text-align:right");
			persenPpn.setWidth("90%");
			persenPpn.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					penerimaanPengadaanMasterAssetDetail
							.setJenisPajakPpn((JenisPajakPpn) (persenPpn.getSelectedItem() == null ? null
									: persenPpn.getSelectedItem().getValue()));
					if (penerimaanPengadaanMasterAssetDetail.getId() != null) {
						Common.refreshUpdate(session, (penerimaanPengadaanMasterAssetDetail));
					}

					total.setValue(Common.numberFormat.get().format(penerimaanPengadaanMasterAssetDetail.getHargaTotal()));
					Double dpp = (penerimaanPengadaanMasterAssetDetail.getDiterima()
							* penerimaanPengadaanMasterAssetDetail.getHargaBeli());

					Double potongan = penerimaanPengadaanMasterAssetDetail.getDiskonDalamBentukPersen()
							? ((penerimaanPengadaanMasterAssetDetail.getHargaPotongan() / 100.0) * dpp)
							: penerimaanPengadaanMasterAssetDetail.getHargaPotongan();
					dpp = dpp - potongan;

					Double ppn = ((penerimaanPengadaanMasterAssetDetail.getPersenPpn() / 100.0) * dpp);
					ppnNilai.setValue(Common.numberFormat.get().format(ppn));

					eventListenerHitungUlang.onEvent(null);
				}
			});

			ppnNilai.setStyle("text-align:right");
			ppnNilai.setParent(row);

			if (persetujuan) {
				new Label(penerimaanPengadaanMasterAssetDetail.getJenisPajakBarang() == null ? ""
						: penerimaanPengadaanMasterAssetDetail.getJenisPajakBarang().getNama()).setParent(row);
			} else {
				(persenPph).setParent(row);
			}

			persenPph.setDisabled((penerimaanPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset() != null
					&& penerimaanPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset()
							.getDisetujuiOleh() != null)
					|| !edit);
			persenPph.setStyle("text-align:right");
			persenPph.setWidth("90%");
			persenPph.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					penerimaanPengadaanMasterAssetDetail
							.setJenisPajakBarang((JenisPajakBarang) (persenPph.getSelectedItem() == null ? null
									: persenPph.getSelectedItem().getValue()));
					Common.refreshUpdate(session, (penerimaanPengadaanMasterAssetDetail));

					total.setValue(Common.numberFormat.get().format(penerimaanPengadaanMasterAssetDetail.getHargaTotal()));
					Double dpp = (penerimaanPengadaanMasterAssetDetail.getDiterima()
							* penerimaanPengadaanMasterAssetDetail.getHargaBeli());

					Double potongan = penerimaanPengadaanMasterAssetDetail.getDiskonDalamBentukPersen()
							? ((penerimaanPengadaanMasterAssetDetail.getHargaPotongan() / 100.0) * dpp)
							: penerimaanPengadaanMasterAssetDetail.getHargaPotongan();
					dpp = dpp - potongan;

					Double ppn = ((penerimaanPengadaanMasterAssetDetail.getPersenPpn() / 100.0) * dpp);
					ppnNilai.setValue(Common.numberFormat.get().format(ppn));

					eventListenerHitungUlang.onEvent(null);
				}
			});

		}

		total.setStyle("text-align:right");
		total.setParent(row);

		vbox = new Vbox();
		vbox.setParent(row);
		vbox.setWidth("98%");

		final MyTextbox keterangan = new MyTextbox(penerimaanPengadaanMasterAssetDetail.getKeterangan() == null ? ""
				: penerimaanPengadaanMasterAssetDetail.getKeterangan());
		keterangan.setWidth("90%");
		keterangan.setHeight("95%");

		if (persetujuan) {
			new MyLabelKecil(penerimaanPengadaanMasterAssetDetail.getKeterangan()).setParent(vbox);
		} else {
			keterangan.setParent(vbox);
		}
		keterangan.setDisabled((penerimaanPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset() != null
				&& penerimaanPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset().getDisetujuiOleh() != null)
				|| !edit);
		keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Session session = HibernateUtil.currentSession();
				penerimaanPengadaanMasterAssetDetail.setKeterangan(keterangan.getValue());

				row.setValign("top");
				row.setAttribute("penerimaanPengadaanMasterAssetDetail", penerimaanPengadaanMasterAssetDetail);
				if (penerimaanPengadaanMasterAssetDetail.getId() != null) {
					Common.refreshUpdate(session, (penerimaanPengadaanMasterAssetDetail));
				}
			}
		});

		final MyTextbox kondisi = new MyTextbox(penerimaanPengadaanMasterAssetDetail.getKondisi() == null ? ""
				: penerimaanPengadaanMasterAssetDetail.getKondisi());
		kondisi.setWidth("90%");
		kondisi.setHeight("95%");
		if (persetujuan) {
			new Label(penerimaanPengadaanMasterAssetDetail.getKondisi()).setParent(vbox);
		} else {
			kondisi.setParent(vbox);
		}
		kondisi.setDisabled((penerimaanPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset() != null
				&& penerimaanPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset().getDisetujuiOleh() != null)
				|| !edit);
		kondisi.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Session session = HibernateUtil.currentSession();
				penerimaanPengadaanMasterAssetDetail.setKondisi(kondisi.getValue());

				row.setValign("top");
				row.setAttribute("penerimaanPengadaanMasterAssetDetail", penerimaanPengadaanMasterAssetDetail);
				if (penerimaanPengadaanMasterAssetDetail.getId() != null) {
					Common.refreshUpdate(session, (penerimaanPengadaanMasterAssetDetail));
				}
			}
		});

		final MyCheckboxConfig kartuGaransi = new MyCheckboxConfig("Ada garansi ?");
		kartuGaransi.setChecked(penerimaanPengadaanMasterAssetDetail.getKartuGaransi());

		if (persetujuan) {
			new Label("Ada garansi ?" + (penerimaanPengadaanMasterAssetDetail.getKartuGaransi() ? "Ya" : "Tidak"))
					.setParent(vbox);
		} else {
			kartuGaransi.setParent(vbox);
		}

		final MyCheckboxConfig boxDus = new MyCheckboxConfig("Ada boks / dus ?");
		boxDus.setChecked(penerimaanPengadaanMasterAssetDetail.getBoxDus());
		if (persetujuan) {
			new Label("Ada boks / dus ?" + (penerimaanPengadaanMasterAssetDetail.getBoxDus() ? "Ya" : "Tidak"))
					.setParent(vbox);
		} else {
			boxDus.setParent(vbox);
		}

		final MyCheckboxConfig manualBook = new MyCheckboxConfig("Ada manual book ?");
		manualBook.setChecked(penerimaanPengadaanMasterAssetDetail.getManualBook());

		if (persetujuan) {
			new Label("Ada manual book ?" + (penerimaanPengadaanMasterAssetDetail.getManualBook() ? "Ya" : "Tidak"))
					.setParent(vbox);
		} else {
			manualBook.setParent(vbox);
		}
		kartuGaransi.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Session session = HibernateUtil.currentSession();
				penerimaanPengadaanMasterAssetDetail.setKartuGaransi(kartuGaransi.isChecked());

				row.setValign("top");
				row.setAttribute("penerimaanPengadaanMasterAssetDetail", penerimaanPengadaanMasterAssetDetail);
				if (penerimaanPengadaanMasterAssetDetail.getId() != null) {
					Common.refreshUpdate(session, (penerimaanPengadaanMasterAssetDetail));
				}
			}
		});

		boxDus.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Session session = HibernateUtil.currentSession();
				penerimaanPengadaanMasterAssetDetail.setBoxDus(boxDus.isChecked());

				row.setValign("top");
				row.setAttribute("penerimaanPengadaanMasterAssetDetail", penerimaanPengadaanMasterAssetDetail);
				if (penerimaanPengadaanMasterAssetDetail.getId() != null) {
					Common.refreshUpdate(session, (penerimaanPengadaanMasterAssetDetail));
				}
			}
		});

		manualBook.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Session session = HibernateUtil.currentSession();
				penerimaanPengadaanMasterAssetDetail.setManualBook(manualBook.isChecked());

				row.setValign("top");
				row.setAttribute("penerimaanPengadaanMasterAssetDetail", penerimaanPengadaanMasterAssetDetail);
				if (penerimaanPengadaanMasterAssetDetail.getId() != null) {
					Common.refreshUpdate(session, (penerimaanPengadaanMasterAssetDetail));
				}
			}
		});

		hbox = new Hbox();
		hbox.setParent(row);

		if (!persetujuan) {

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
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
										if (penerimaanPengadaanMasterAssetDetail.getId() != null) {
											Session session = HibernateUtil.currentSession();
											session.delete(penerimaanPengadaanMasterAssetDetail);
										}
										row.setVisible(false);
										row.detach();

										eventListenerHitungUlang.onEvent(null);
									}

								}
							});

				}
			});
		}
	}

}
