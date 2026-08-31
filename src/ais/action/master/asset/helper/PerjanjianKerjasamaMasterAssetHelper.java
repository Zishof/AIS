package ais.action.master.asset.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Columns;
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
import ais.database.model.asset.MasterAsset;
import ais.database.model.asset.PerjanjianKerjasamaMasterAsset;
import ais.database.model.asset.PerjanjianKerjasamaMasterAssetDetail;
import ais.database.model.asset.PermintaanPengadaanMasterAsset;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyGroupboxStyled;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Helper UI untuk mengelola daftar barang/jasa (dengan harga, diskon, dan total per baris) pada
 * satu {@link PerjanjianKerjasamaMasterAsset} modul asset, ditampilkan sebagai grid tambah/hapus
 * berpaging di dalam layar detail perjanjian. Menu tambah membuka dialog pemilihan
 * {@link MasterAsset} secara massal, mengecualikan asset yang sudah ada di daftar. Setiap baris
 * dapat disunting langsung di grid (jumlah, harga beli, potongan harga) dengan total baris dihitung
 * ulang otomatis dan disimpan langsung ke database saat berubah (bila detail sudah tersimpan);
 * mengubah harga beli turut memperbarui harga beli default pada {@link MasterAsset} terkait.
 * Footer grid menampilkan total keseluruhan, dihitung ulang lewat {@link #eventListenerHitungUlang}
 * setiap kali baris berubah/dihapus. Grid dikunci (read-only) begitu perjanjian sudah disetujui
 * ({@code disetujuiOleh} terisi).
 */
public class PerjanjianKerjasamaMasterAssetHelper {

	private MyGrid gridMasterAsset;

	private boolean edit = true;
	private boolean delete = true;
	private double totalSemua = 0.0;

	private Footer footerTotalSemua;

	/** Membuat helper terikat ke {@code gridMasterAsset} (grid berpaging 10 baris). */
	public PerjanjianKerjasamaMasterAssetHelper(MyGrid gridMasterAsset) {
		this.gridMasterAsset = gridMasterAsset;
	}

	/**
	 * Membangun kotak grup grid daftar barang/jasa lengkap dengan toolbar tambah + refresh
	 * (disembunyikan/dinonaktifkan begitu perjanjian disetujui), kolom gambar/kode/nama/qty/harga/
	 * diskon/total/keterangan/aksi, dan footer total keseluruhan. Langsung memuat detail
	 * {@code perjanjianKerjasamaMasterAsset} yang sudah ada.
	 *
	 * @param perjanjianKerjasamaMasterAsset induk perjanjian, boleh belum tersimpan (draft baru)
	 * @return {@link MyGroupboxStyled} siap ditempel ke komponen induk
	 */
	public MyGroupboxStyled initDetail(final PerjanjianKerjasamaMasterAsset perjanjianKerjasamaMasterAsset)
			throws Exception {
		MyGroupboxStyled myGroupboxStyled = new MyGroupboxStyled();
		myGroupboxStyled.appendChild(new MyCaptionStyled("Daftar Barang/Jasa"));

		edit = perjanjianKerjasamaMasterAsset.getDisetujuiOleh() == null;
		delete = perjanjianKerjasamaMasterAsset.getDisetujuiOleh() == null;

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(myGroupboxStyled);

		MyToolbarbuttonConfig add = new MyToolbarbuttonConfig("Ambil Data Barang/Jasa", "/img/new.gif");
		add.setVisible(perjanjianKerjasamaMasterAsset.getDisetujuiOleh() == null);
		add.setParent(toolbar);
		add.setTooltiptext("Tambah");
		add.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				List<MasterAsset> masterAssets = new ArrayList<MasterAsset>();
				List<Row> myrows = gridMasterAsset.getRows().getChildren();
				for (Row row : myrows) {
					masterAssets.add(((PerjanjianKerjasamaMasterAssetDetail) row
							.getAttribute("perjanjianKerjasamaMasterAssetDetail")).getMasterAsset());
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
							PerjanjianKerjasamaMasterAssetDetail perjanjianKerjasamaMasterAssetDetail = new PerjanjianKerjasamaMasterAssetDetail();
							perjanjianKerjasamaMasterAssetDetail.setMasterAsset(masterAsset);
							perjanjianKerjasamaMasterAssetDetail.setJumlah(1.0);
							perjanjianKerjasamaMasterAssetDetail.setKeterangan("");
							perjanjianKerjasamaMasterAssetDetail
									.setPerjanjianKerjasamaMasterAsset(perjanjianKerjasamaMasterAsset);

							if (perjanjianKerjasamaMasterAsset.getId() != null) {
								Session session = HibernateUtil.currentSession();
								session.save(perjanjianKerjasamaMasterAssetDetail);
							}

							Rows rows = gridMasterAsset.getRows() == null ? new Rows() : gridMasterAsset.getRows();
							rows.setParent(gridMasterAsset);
							Row row = new Row();row.setValign("top");
							row.setParent(rows);
							initRow(row, perjanjianKerjasamaMasterAssetDetail);
						}
						eventListenerHitungUlang.onEvent(arg0);
					}
				});

				ambilDataMasterAssetBanyak.onModal();

			}
		});

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		cari.setParent(toolbar);
		cari.setVisible(perjanjianKerjasamaMasterAsset.getId() != null);
		cari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDataDetail(perjanjianKerjasamaMasterAsset);
			}
		});

		gridMasterAsset.setParent(myGroupboxStyled);
		gridMasterAsset.setWidth("100%");
		gridMasterAsset.setHeight("100%");
		gridMasterAsset.setStyle("min-height:350px");
		gridMasterAsset.setMold("paging");
		gridMasterAsset.setPageSize(10);
		gridMasterAsset.getPagingChild().setMold("os");

		Columns columns = new Columns();
		columns.setParent(gridMasterAsset);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("120px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Qty");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Harga");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Diskon");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Total");
		column.setAlign("right");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		footerTotalSemua = new Footer(Common.numberFormat.get().format(totalSemua));

		loadDataDetail(perjanjianKerjasamaMasterAsset);

		Foot foot = new Foot();
		foot.setParent(gridMasterAsset);

		Footer footer = new Footer();
		foot.appendChild(footer);

		footer = new Footer("Total");
		foot.appendChild(footer);

		footer = new Footer();
		foot.appendChild(footer);

		footer = new Footer();
		foot.appendChild(footer);

		footer = new Footer();
		foot.appendChild(footer);

		footer = new Footer();
		foot.appendChild(footer);

		foot.appendChild(footerTotalSemua);

		footer = new Footer();
		foot.appendChild(footer);

		return myGroupboxStyled;
	}

	/** Listener yang menghitung ulang total keseluruhan (jumlah x harga beli, dikurangi persentase potongan) dari seluruh baris grid yang masih tampak, lalu memperbarui {@link #footerTotalSemua}. */
	public EventListener eventListenerHitungUlang = new EventListener() {

		@SuppressWarnings("unchecked")
		@Override
		public void onEvent(Event arg0) throws Exception {
			List<Row> rows = gridMasterAsset.getRows().getChildren();

			totalSemua = 0.0;
			for (Row row : rows) {
				if (row.isVisible()) {
					PerjanjianKerjasamaMasterAssetDetail perjanjianKerjasamaMasterAssetDetail = (PerjanjianKerjasamaMasterAssetDetail) row
							.getAttribute("perjanjianKerjasamaMasterAssetDetail");

					Double j = (perjanjianKerjasamaMasterAssetDetail.getJumlah()
							* perjanjianKerjasamaMasterAssetDetail.getHargaBeli())
							- ((perjanjianKerjasamaMasterAssetDetail.getHargaPotongan() / 100.0)
									* (perjanjianKerjasamaMasterAssetDetail.getJumlah()
											* perjanjianKerjasamaMasterAssetDetail.getHargaBeli()));

					totalSemua += j;
				}
			}

			footerTotalSemua.setLabel(Common.numberFormat.get().format(totalSemua));
		}
	};

	/** Memuat seluruh detail tersimpan milik {@code perjanjianKerjasamaMasterAsset} ke grid, lalu menjadwalkan penghitungan ulang total lewat {@link Common#createDefaultTimer}. */
	@SuppressWarnings("unchecked")
	private void loadDataDetail(final PerjanjianKerjasamaMasterAsset perjanjianKerjasamaMasterAsset) throws Exception {

		List<PerjanjianKerjasamaMasterAssetDetail> perjanjianKerjasamaMasterAssetDetails = perjanjianKerjasamaMasterAsset == null
				|| perjanjianKerjasamaMasterAsset.getId() == null
						? new ArrayList<PerjanjianKerjasamaMasterAssetDetail>()
						: HibernateUtil.currentSession().createCriteria(PerjanjianKerjasamaMasterAssetDetail.class)
								.add(Restrictions.eq("perjanjianKerjasamaMasterAsset", perjanjianKerjasamaMasterAsset))
								.list();

		Rows rows = gridMasterAsset.getRows() == null ? new Rows() : gridMasterAsset.getRows();
		rows.setParent(gridMasterAsset);
		Common.clear(rows);

		for (PerjanjianKerjasamaMasterAssetDetail perjanjianKerjasamaMasterAssetDetail : perjanjianKerjasamaMasterAssetDetails) {
			Row row = new Row();row.setValign("top");
			row.setParent(rows);
			initRow(row, perjanjianKerjasamaMasterAssetDetail);
		}

		Common.createDefaultTimer(eventListenerHitungUlang);
	}

	/**
	 * Mengisi satu baris grid dengan pratinjau gambar asset, kode, nama (dengan tautan riwayat
	 * revisi, dan tautan ke permintaan pengadaan sumbernya bila detail ini berasal dari satu),
	 * field jumlah/harga beli/potongan yang dapat disunting (langsung menyimpan perubahan ke
	 * database dan memicu {@link #eventListenerHitungUlang} bila berubah — mengubah harga beli juga
	 * memperbarui harga beli default {@link MasterAsset} terkait), total baris terhitung otomatis,
	 * keterangan, dan tombol hapus (dengan konfirmasi). Seluruh field disunting dinonaktifkan bila
	 * perjanjian sudah disetujui atau pengguna tidak punya hak edit.
	 */
	public void initRow(final Row row, final PerjanjianKerjasamaMasterAssetDetail perjanjianKerjasamaMasterAssetDetail)
			throws Exception {

		row.setValign("top");row.setAttribute("perjanjianKerjasamaMasterAssetDetail", perjanjianKerjasamaMasterAssetDetail);

		Hbox hbox = new Hbox();
		hbox.setParent(row);

		LampiranLain.createDownloadUploadFileLain(hbox, perjanjianKerjasamaMasterAssetDetail.getMasterAsset().getId(),
				LampiranLain.GAMBAR_MASTER_ASSET, "Gambar", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

					}
				}, null, false, false, false, true, null, false, true);

		final Label total = new Label(Common.numberFormat.get().format(
				(perjanjianKerjasamaMasterAssetDetail.getJumlah() * perjanjianKerjasamaMasterAssetDetail.getHargaBeli())
						- ((perjanjianKerjasamaMasterAssetDetail.getHargaPotongan() / 100.0)
								* (perjanjianKerjasamaMasterAssetDetail.getJumlah()
										* perjanjianKerjasamaMasterAssetDetail.getHargaBeli()))));

		final MyDoublebox jumlah = new MyDoublebox(perjanjianKerjasamaMasterAssetDetail.getJumlah() == null ? 0.0
				: perjanjianKerjasamaMasterAssetDetail.getJumlah());

		final MyDoublebox hargaBeli = new MyDoublebox(perjanjianKerjasamaMasterAssetDetail.getHargaBeli());

		final MyDoublebox hargaPotongan = new MyDoublebox(perjanjianKerjasamaMasterAssetDetail.getHargaPotongan());

		new Label(perjanjianKerjasamaMasterAssetDetail.getMasterAsset() == null ? ""
				: perjanjianKerjasamaMasterAssetDetail.getMasterAsset().getKode()).setParent(row);

		Vbox a;
		(a = RevisiHelper.createNewRevisi(PerjanjianKerjasamaMasterAssetDetail.class,
				perjanjianKerjasamaMasterAssetDetail, perjanjianKerjasamaMasterAssetDetail.getMasterAsset() == null ? ""
						: perjanjianKerjasamaMasterAssetDetail.getMasterAsset().getNama()))
				.setParent(row);
		if (perjanjianKerjasamaMasterAssetDetail.getPermintaanPengadaanMasterAssetDetail() != null) {
			RevisiHelper.createNewRevisi(PermintaanPengadaanMasterAsset.class,
					perjanjianKerjasamaMasterAssetDetail.getPermintaanPengadaanMasterAssetDetail()
							.getPermintaanPengadaanMasterAsset(),
					perjanjianKerjasamaMasterAssetDetail.getPermintaanPengadaanMasterAssetDetail()
							.getPermintaanPengadaanMasterAsset().getKode())
					.setParent(a);
		}

		(jumlah).setParent(row);
		jumlah.setDisabled(
				perjanjianKerjasamaMasterAssetDetail.getPerjanjianKerjasamaMasterAsset().getDisetujuiOleh() != null
						|| !edit);
		jumlah.setStyle("text-align:right");
		jumlah.setWidth("90%");
		jumlah.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Session session = HibernateUtil.currentSession();
				perjanjianKerjasamaMasterAssetDetail.setJumlah(jumlah.getValue());

				row.setValign("top");row.setAttribute("perjanjianKerjasamaMasterAssetDetail", perjanjianKerjasamaMasterAssetDetail);
				if (perjanjianKerjasamaMasterAssetDetail.getId() != null) {
					Common.refreshUpdate(session, (perjanjianKerjasamaMasterAssetDetail));
				}

				total.setValue(Common.numberFormat.get().format((perjanjianKerjasamaMasterAssetDetail.getJumlah()
						* perjanjianKerjasamaMasterAssetDetail.getHargaBeli())
						- ((perjanjianKerjasamaMasterAssetDetail.getHargaPotongan() / 100.0)
								* (perjanjianKerjasamaMasterAssetDetail.getJumlah()
										* perjanjianKerjasamaMasterAssetDetail.getHargaBeli()))));

				eventListenerHitungUlang.onEvent(null);
			}
		});

		(hargaBeli).setParent(row);
		hargaBeli.setDisabled(
				perjanjianKerjasamaMasterAssetDetail.getPerjanjianKerjasamaMasterAsset().getDisetujuiOleh() != null
						|| !edit);
		hargaBeli.setStyle("text-align:right");
		hargaBeli.setWidth("90%");
		hargaBeli.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Session session = HibernateUtil.currentSession();
				perjanjianKerjasamaMasterAssetDetail.setHargaBeli(hargaBeli.getValue());

				row.setValign("top");row.setAttribute("perjanjianKerjasamaMasterAssetDetail", perjanjianKerjasamaMasterAssetDetail);
				if (perjanjianKerjasamaMasterAssetDetail.getId() != null) {
					Common.refreshUpdate(session, (perjanjianKerjasamaMasterAssetDetail));
				}

				MasterAsset masterAsset = perjanjianKerjasamaMasterAssetDetail.getMasterAsset();
				session.refresh(masterAsset);
				masterAsset.setHargaBeliDefault(hargaBeli.getValue());
				Common.refreshUpdate(session, masterAsset);

				total.setValue(Common.numberFormat.get().format((perjanjianKerjasamaMasterAssetDetail.getJumlah()
						* perjanjianKerjasamaMasterAssetDetail.getHargaBeli())
						- ((perjanjianKerjasamaMasterAssetDetail.getHargaPotongan() / 100.0)
								* (perjanjianKerjasamaMasterAssetDetail.getJumlah()
										* perjanjianKerjasamaMasterAssetDetail.getHargaBeli()))));
				eventListenerHitungUlang.onEvent(null);
			}
		});

		(hargaPotongan).setParent(row);
		hargaPotongan.setDisabled(
				perjanjianKerjasamaMasterAssetDetail.getPerjanjianKerjasamaMasterAsset().getDisetujuiOleh() != null
						|| !edit);
		hargaPotongan.setStyle("text-align:right");
		hargaPotongan.setWidth("90%");
		hargaPotongan.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Session session = HibernateUtil.currentSession();
				perjanjianKerjasamaMasterAssetDetail.setHargaPotongan(hargaPotongan.getValue());

				row.setValign("top");row.setAttribute("perjanjianKerjasamaMasterAssetDetail", perjanjianKerjasamaMasterAssetDetail);
				if (perjanjianKerjasamaMasterAssetDetail.getId() != null) {
					Common.refreshUpdate(session, (perjanjianKerjasamaMasterAssetDetail));
				}

				total.setValue(Common.numberFormat.get().format((perjanjianKerjasamaMasterAssetDetail.getJumlah()
						* perjanjianKerjasamaMasterAssetDetail.getHargaBeli())
						- ((perjanjianKerjasamaMasterAssetDetail.getHargaPotongan() / 100.0)
								* (perjanjianKerjasamaMasterAssetDetail.getJumlah()
										* perjanjianKerjasamaMasterAssetDetail.getHargaBeli()))));

				eventListenerHitungUlang.onEvent(null);
			}
		});

		total.setStyle("text-align:right");
		total.setParent(row);

		final MyTextbox keterangan = new MyTextbox(perjanjianKerjasamaMasterAssetDetail.getKeterangan() == null ? ""
				: perjanjianKerjasamaMasterAssetDetail.getKeterangan());
		keterangan.setWidth("90%");
		keterangan.setHeight("95%");
		keterangan.setParent(row);
		keterangan.setDisabled(
				perjanjianKerjasamaMasterAssetDetail.getPerjanjianKerjasamaMasterAsset().getDisetujuiOleh() != null
						|| !edit);
		keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Session session = HibernateUtil.currentSession();
				perjanjianKerjasamaMasterAssetDetail.setKeterangan(keterangan.getValue());

				row.setValign("top");row.setAttribute("perjanjianKerjasamaMasterAssetDetail", perjanjianKerjasamaMasterAssetDetail);
				if (perjanjianKerjasamaMasterAssetDetail.getId() != null) {
					Common.refreshUpdate(session, (perjanjianKerjasamaMasterAssetDetail));
				}
			}
		});

		hbox = new Hbox();
		hbox.setParent(row);

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
									if (perjanjianKerjasamaMasterAssetDetail.getId() != null) {
										Session session = HibernateUtil.currentSession();
										session.delete(perjanjianKerjasamaMasterAssetDetail);
									}
	row.setVisible(false);row.detach();

									eventListenerHitungUlang.onEvent(null);
								}

							}
						});

			}
		});
	}

}
