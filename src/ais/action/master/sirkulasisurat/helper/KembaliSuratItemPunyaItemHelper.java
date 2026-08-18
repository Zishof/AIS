package ais.action.master.sirkulasisurat.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.file.FotoGambarSuratMasuk;
import ais.database.model.library.Perpustakaan;
import ais.database.model.sirkulasisurat.KembaliSuratItem;
import ais.database.model.sirkulasisurat.KembaliSuratItemDetail;
import ais.database.model.sirkulasisurat.PeminjamanSuratItem;
import ais.database.model.sirkulasisurat.PeminjamanSuratItemDetail;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyGroupboxStyled;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

public class KembaliSuratItemPunyaItemHelper {

	private boolean edit = false;
	private boolean delete = false;
	// private Textbox barcode;

	private Perpustakaan perpustakaan;
	private PeminjamanSuratItem peminjamanSuratItem;
	private KembaliSuratItem kembaliSuratItem;
//	private Textbox barcode;
	private boolean persetujuan;
	private String tipe;
	private MyGrid gridItem;

	public KembaliSuratItemPunyaItemHelper(String tipe, boolean persetujuan) {

		this.persetujuan = persetujuan;
		this.tipe = tipe;
		CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
	}

	public Groupbox initDetail(final MyGrid gridItem, final KembaliSuratItem kembaliSuratItem, final String barcodeItem)
			throws Exception {
		this.kembaliSuratItem = kembaliSuratItem;
		this.gridItem = gridItem;

		MyGroupboxStyled myGroupboxStyled = new MyGroupboxStyled();
		myGroupboxStyled.appendChild(new MyCaptionStyled("Daftar " + tipe));

		if (!persetujuan) {
			Toolbar toolbar = new Toolbar();
			toolbar.setHeight("30px");
			toolbar.setParent(myGroupboxStyled);

//			new Label("Scan Nomor Surat / Nomor Agenda yang dikembalikan disini : ").setParent(toolbar);
//			new Space().setParent(toolbar);
//			barcode = new Textbox();
//			barcode.setStyle("font-size:xx-large");
//			barcode.setParent(toolbar);
//			barcode.addEventListener("onOK", new EventListener() {
//
//				@Override
//				public void onEvent(Event arg0) throws Exception {
//
//					loadBarcode(kembaliSuratItem);
//
//					Common.createDefaultTimerNoBusy(new EventListener() {
//
//						@Override
//						public void onEvent(Event arg0) throws Exception {
//							barcode.focus();
//							barcode.select();
//						}
//					});
//				}
//			});
//
//			barcode.addEventListener("onFocus", new EventListener() {
//
//				@Override
//				public void onEvent(Event arg0) throws Exception {
//					barcode.select();
//				}
//			});

			MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
			cari.setParent(toolbar);
			cari.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(gridItem.getRows());
					loadDataDetail(gridItem, kembaliSuratItem, true);
					loadDataDetailFromPeminjaman(gridItem);
				}
			});
		}

		Common.clear(gridItem);
		gridItem.setParent(myGroupboxStyled);
		gridItem.setWidth("100%");
		gridItem.setStyle("min-height:350px");
		gridItem.setHeight("100%");
		Columns columns = new Columns();
		columns.setParent(gridItem);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nomor Surat / Perihal");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nomor Agenda");
		column.setWidth("10%");

//		column = new MyColumnConfig();
//		column.setParent(columns);
//		column.setLabel("Status");
//		column.setWidth("10%");
//
//		column = new MyColumnConfig();
//		column.setParent(columns);
//		column.setLabel("Kelengkapan");
//		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Deskripsi");
//		column.setWidth("22%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tgl. Kembali");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("10%");

		loadDataDetail(gridItem, kembaliSuratItem, false);

		return myGroupboxStyled;
	}

	@SuppressWarnings("unchecked")
	private void loadDataDetail(final MyGrid gridItem, final KembaliSuratItem kembaliSuratItem, boolean refresh)
			throws Exception {

		List<KembaliSuratItemDetail> kembaliSuratItemDetails = kembaliSuratItem == null
				|| kembaliSuratItem.getId() == null ? new ArrayList<KembaliSuratItemDetail>()
						: HibernateUtil.currentSession().createCriteria(KembaliSuratItemDetail.class)
								.add(Restrictions.eq("kembaliSuratItem", kembaliSuratItem)).list();

		Rows rows = gridItem.getRows() == null ? new Rows() : gridItem.getRows();
		rows.setParent(gridItem);

		for (KembaliSuratItemDetail kembaliSuratItemDetail : kembaliSuratItemDetails) {

			Row row = new Row();
			row.setValign("top");
			row.setParent(rows);
			initRow(gridItem, row, kembaliSuratItemDetail);
		}
	}

	public void initRow(final MyGrid gridItem, final Row row, final KembaliSuratItemDetail kembaliSuratItemDetail)
			throws Exception {
		row.setValign("top");
		row.setAttribute("kembaliSuratItemDetail", kembaliSuratItemDetail);

		final PeminjamanSuratItemDetail peminjamanSuratItemDetail = kembaliSuratItemDetail
				.getPeminjamanSuratItemDetail();
		Integer jumlahMaksimalPerpanjanganPeminjaman = 2;
		peminjamanSuratItemDetail.setJumlahMaxPerpanjangan(jumlahMaksimalPerpanjanganPeminjaman);

		RevisiHelper.createNewRevisi(KembaliSuratItemDetail.class, kembaliSuratItemDetail,
				(kembaliSuratItemDetail.getSuratMasuk().getNoSurat()) + " \n"
						+ (kembaliSuratItemDetail.getSuratMasuk().getPerihal()))
				.setParent(row);

		new Label(kembaliSuratItemDetail.getSuratMasuk().getKode()).setParent(row);

//		final MyCheckboxConfig status = new MyCheckboxConfig("Status");
//		status.setChecked(kembaliSuratItemDetail.getStatus());
//		status.setParent(row);
//		status.addEventListener("onCheck", new EventListener() {
//
//			@Override
//			public void onEvent(Event arg0) throws Exception {
//				kembaliSuratItemDetail.setStatus(status.isChecked());
//				row.setAttribute("kembaliSuratItemDetail", kembaliSuratItemDetail);
//
//				if (kembaliSuratItemDetail.getId() != null) {
//					Common.refreshUpdate(kembaliSuratItemDetail);
//				}
//			}
//		});
//
//		final MyCheckboxConfig kelengkapan = new MyCheckboxConfig("Kelengkapan");
//		kelengkapan.setParent(row);
//		kelengkapan.setChecked(kembaliSuratItemDetail.getKelengkapan());
//		kelengkapan.addEventListener("onCheck", new EventListener() {
//
//			@Override
//			public void onEvent(Event arg0) throws Exception {
//				kembaliSuratItemDetail.setKelengkapan(kelengkapan.isChecked());
//				row.setAttribute("kembaliSuratItemDetail", kembaliSuratItemDetail);
//
//				if (kembaliSuratItemDetail.getId() != null) {
//					Common.refreshUpdate(kembaliSuratItemDetail);
//				}
//			}
//		});

		Vbox vbox = new Vbox();
		vbox.appendChild(new MyLabelAgakKecil("Tgl Pinjam: "
				+ Common.dateFormat4.get().format(peminjamanSuratItemDetail.getPeminjamanSuratItem().getTanggalPembuatan())));
		final MyLabelAgakKecil batasPengembalian;
		vbox.appendChild(batasPengembalian = new MyLabelAgakKecil(
				"Hrs kembali: " + Common.dateFormat4.get().format(peminjamanSuratItemDetail.getBatasWaktupengembalian())));
		final MyLabelAgakKecil batas;
		vbox.appendChild(
				batas = new MyLabelAgakKecil("Batas: " + peminjamanSuratItemDetail.getJumlahHariBatas() + " hari"));

		final MyLabelAgakKecil lama;
		vbox.appendChild(lama = new MyLabelAgakKecil(
				"Lama pinjam: " + peminjamanSuratItemDetail.getJumlahSelisihHari() + " hari"));

		final MyLabelAgakKecil terlambat;
		vbox.appendChild(terlambat = new MyLabelAgakKecil(
				"Terlambat: " + peminjamanSuratItemDetail.getJumlahHariTerlambat() + " hari"));
		vbox.setParent(row);

		vbox = new Vbox();
		vbox.setParent(row);
		final ais.ui.util.MyDatebox tanggal = new ais.ui.util.MyDatebox(kembaliSuratItemDetail.getTanggal());
		tanggal.setDisabled((kembaliSuratItemDetail.getKembaliSuratItem().getDisetujuiOleh() != null || !edit));
		tanggal.setWidth("90%");
		tanggal.setParent(vbox);

		row.setAttribute("tanggal", tanggal);

		if (persetujuan) {
			new Label(peminjamanSuratItemDetail.getKeterangan()).setParent(row);
		} else {

			final MyTextbox keterangan = new MyTextbox(
					kembaliSuratItemDetail.getKeterangan() == null ? "" : kembaliSuratItemDetail.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setHeight("95%");
			keterangan.setRows(3);
			keterangan.setParent(row);
			keterangan.setDisabled(kembaliSuratItemDetail.getKembaliSuratItem().getDisetujuiOleh() != null || !edit);
			keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					kembaliSuratItemDetail.setKeterangan(keterangan.getValue());

					row.setValign("top");
					row.setAttribute("kembaliSuratItemDetail", kembaliSuratItemDetail);
					if (kembaliSuratItemDetail.getId() != null) {
						Common.refreshUpdate(kembaliSuratItemDetail);
					}
				}
			});
		}
		final EventListener tanggalEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (tanggal.getValue() == null) {
					tanggal.setValue(ais.ui.util.WaktuUtil.getDate());
				}

				kembaliSuratItemDetail.setTanggal(tanggal.getValue());

				PeminjamanSuratItemDetail peminjamanSuratItemDetail = kembaliSuratItemDetail
						.getPeminjamanSuratItemDetail();

				peminjamanSuratItemDetail.setTanggalKembali(tanggal.getValue());

				peminjamanSuratItemDetail.setKembaliSuratItemDetail(kembaliSuratItemDetail);

				terlambat.setValue("Terlambat " + peminjamanSuratItemDetail.getJumlahHariTerlambat() + " hari");

				kembaliSuratItemDetail.setPeminjamanSuratItemDetail(peminjamanSuratItemDetail);

				row.setValign("top");
				row.setAttribute("kembaliSuratItemDetail", kembaliSuratItemDetail);
				lama.setValue("Lama pinjam " + peminjamanSuratItemDetail.getJumlahSelisihHari() + " hari");
				batasPengembalian.setValue("Hrs kembali: "
						+ Common.dateFormat4.get().format(peminjamanSuratItemDetail.getBatasWaktupengembalian()));

				if (peminjamanSuratItemDetail.getJumlahHariTerlambat() > 0) {
					terlambat.setStyle("color:red");
				} else {
					terlambat.setStyle("color:black");
				}

				batas.setValue("Batas: " + peminjamanSuratItemDetail.getJumlahHariBatas() + " hari");
			}
		};

		tanggalEventListener.onEvent(null);
		tanggal.addEventListener("onChange", tanggalEventListener);

		Vbox toolbar = new Vbox();
		toolbar.setParent(row);

		if (kembaliSuratItemDetail.getPeminjamanSuratItemDetail() != null
				&& kembaliSuratItemDetail.getPeminjamanSuratItemDetail().getPeminjamanSuratItem() != null) {
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig(
					"Lihat " + kembaliSuratItemDetail.getPeminjamanSuratItemDetail().getPeminjamanSuratItem().getTipe(),
					"/img/eye-icon.png");
			button.setDisabled(kembaliSuratItemDetail.getPeminjamanSuratItemDetail().getPeminjamanSuratItem()
					.getDisetujuiOleh() == null
					|| kembaliSuratItemDetail.getPeminjamanSuratItemDetail().getPeminjamanSuratItem().getMulai()
							.after(WaktuUtil.getDate())
					|| kembaliSuratItemDetail.getPeminjamanSuratItemDetail().getPeminjamanSuratItem().getSampai()
							.before(WaktuUtil.getDate()));
			button.setAttribute("janganDisabled", true);
			button.setOrient("vertical");
			button.setTooltiptext("Lihat Data");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {
					final MyWindow window = new MyWindow("Tampilan Surat", "none", true);
					window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					window.setHeight("95%");
					window.setWidth("95%");

					Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
					borderlayout.setParent(window);

					Center center = new Center();
					center.setParent(borderlayout);

					MyGrid grid = new MyGrid();
					grid.setWidth("100%");
					grid.setParent(center);
					grid.setHeight("100%");

					Session session = StreamingHibernateUtil.getInstance().currentSession();
					List<FotoGambarSuratMasuk> fotoGambarSuratMasuks = kembaliSuratItemDetail
							.getPeminjamanSuratItemDetail().getSuratMasuk() == null
							|| kembaliSuratItemDetail.getPeminjamanSuratItemDetail().getSuratMasuk().getId() == null
									? new ArrayList<FotoGambarSuratMasuk>()
									: session.createCriteria(FotoGambarSuratMasuk.class)
											.add(Restrictions.eq("suratMasuk",
													kembaliSuratItemDetail.getPeminjamanSuratItemDetail()
															.getSuratMasuk().getId()))
											.addOrder(Order.desc("id")).list();

					Rows rows = new Rows();
					rows.setParent(grid);

					for (FotoGambarSuratMasuk fotoGambarSuratMasuk : fotoGambarSuratMasuks) {
						Row row = new Row();
						row.setValign("top");
						row.setParent(rows);
						CommonMedia.preview(fotoGambarSuratMasuk, row);
					}
					// session.disconnect();
					if (session.isOpen()) {session.disconnect();session.close();}
					StreamingHibernateUtil.getInstance().closeSession();

					South south = new South();
					ais.ui.util.ZkCompat.setFlex(south, true);
					south.setParent(borderlayout);

					Toolbar toolbar = new Toolbar();
					toolbar.setParent(south);
					MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai", "/img/cancel.gif");
					cancel.setTooltiptext("Tutup");
					cancel.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							window.detach();
						}
					});
					cancel.setParent(toolbar);
					window.onModal();

				}
			});
			button.setParent(toolbar);
		}

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Batalkan", "/img/svg/trash.svg");
		button.setTooltiptext("Hapus Data");
		button.setOrient("vertical");
		button.setVisible(delete && kembaliSuratItem.getDisetujuiOleh() == null);
		button.setParent(toolbar);

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
									if (kembaliSuratItemDetail.getId() != null) {
										Session session = HibernateUtil.currentSession();
										session.delete(kembaliSuratItemDetail);
									}

									Common.createDefaultTimer(new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											Common.clear(gridItem.getRows());
											loadDataDetail(gridItem, kembaliSuratItem, false);
											loadDataDetailFromPeminjaman(gridItem);
										}
									});
								}

							}
						});

			}
		});
	}

	public Perpustakaan getPerpustakaan() {
		return perpustakaan;
	}

	public void setPerpustakaan(Perpustakaan perpustakaan) {
		this.perpustakaan = perpustakaan;
	}

	public PeminjamanSuratItem getPeminjamanSuratItem() {
		return peminjamanSuratItem;
	}

	public void setPeminjamanSuratItem(PeminjamanSuratItem peminjamanSuratItem) {
		this.peminjamanSuratItem = peminjamanSuratItem;
		try {
			loadDataDetailFromPeminjaman(gridItem);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public void setPeminjamanSuratItem(MyGrid gridItem, PeminjamanSuratItem peminjamanSuratItem) {
		this.peminjamanSuratItem = peminjamanSuratItem;
		try {
			loadDataDetailFromPeminjaman(gridItem);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	@SuppressWarnings("unchecked")
	public void loadDataDetailFromPeminjaman(final MyGrid gridItem) throws Exception {

		List<PeminjamanSuratItemDetail> peminjamanSuratItemDetails = HibernateUtil.currentSession()
				.createCriteria(PeminjamanSuratItemDetail.class)
				.createAlias("peminjamanSuratItem", "peminjamanSuratItem")
				.add(Restrictions.eq("peminjamanSuratItem.peminjamSurat", peminjamanSuratItem.getPeminjamSurat()))
				.list();

		kembaliSuratItem.setPeminjamanSuratItem(peminjamanSuratItem);

		Rows rows = gridItem.getRows() == null ? new Rows() : gridItem.getRows();
		rows.setParent(gridItem);

		Common.clear(rows);

		for (PeminjamanSuratItemDetail peminjamanSuratItemDetail : peminjamanSuratItemDetails) {

			KembaliSuratItemDetail kembaliSuratItemDetail = peminjamanSuratItemDetail.getKembaliSuratItemDetail();

			if (kembaliSuratItemDetail == null) {
				kembaliSuratItemDetail = new KembaliSuratItemDetail();

				kembaliSuratItemDetail.setDikembali(peminjamanSuratItemDetail.getJumlah());
				kembaliSuratItemDetail.setKeterangan("");
				kembaliSuratItemDetail.setKembaliSuratItem(kembaliSuratItem);
				kembaliSuratItemDetail.setSuratMasuk(peminjamanSuratItemDetail.getSuratMasuk());
				kembaliSuratItemDetail.setPeminjamanSuratItemDetail(peminjamanSuratItemDetail);

			}

			Row row = new Row();
			row.setValign("top");
			row.setParent(rows);
			initRow(gridItem, row, kembaliSuratItemDetail);
		}
	}

//	@SuppressWarnings("unchecked")
//	public void loadBarcode(KembaliSuratItem kembaliSuratItem) throws Exception {
//		String barcode = this.barcode.getText().trim();
//		if (barcode.trim().equals("")) {
//			MyMessageboxConfig.show("Barcode harus diisi", "Peringatan", MyMessageboxConfig.OK,
//					MyMessageboxConfig.EXCLAMATION);
//			return;
//		}
//
//		Session session = HibernateUtil.currentSession();
//		SuratMasuk suratMasuk = (SuratMasuk) session.createCriteria(SuratMasuk.class)
//				.add(Restrictions.or(Restrictions.ilike("noSurat", barcode, MatchMode.EXACT),
//						Restrictions.ilike("kode", barcode, MatchMode.EXACT)))
//				.setMaxResults(1).uniqueResult();
//
//		if (suratMasuk == null) {
//			MyMessageboxConfig.show("Nomor Surat/Nomor Agenda " + barcode + " tidak ditemukan", "Peringatan",
//					MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, new EventListener() {
//
//						@Override
//						public void onEvent(Event arg0) throws Exception {
//							KembaliSuratItemPunyaItemHelper.this.barcode.focus();
//							KembaliSuratItemPunyaItemHelper.this.barcode.select();
//						}
//					});
//			return;
//		}
//
//		List<Row> rowsItem = gridItem.getRows().getChildren();
//		for (Row row : rowsItem) {
//			try {
//				KembaliSuratItemDetail kembaliSuratItemDetail = (KembaliSuratItemDetail) row
//						.getAttribute("kembaliSuratItemDetail");
//				if (kembaliSuratItemDetail.getSuratMasuk() != null
//						&& suratMasuk.getId().equals(kembaliSuratItemDetail.getSuratMasuk().getId())) {
//					try {
//						MyCheckboxConfig checkbox = (MyCheckboxConfig) row.getAttribute("checkbox");
//						checkbox.setChecked(true);
//						EventListener checkEventListener = (EventListener) checkbox.getAttribute("checkEventListener");
//						checkEventListener.onEvent(null);
//						break;
//					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirkulasisurat/helper/KembaliSuratItemPunyaItemHelper.java:578");
//						e.printStackTrace();
//					}
//
//				}
//
//			} catch (Exception e) {
//				Common.tampilErrorJikaAdmin(e);
//			}
//		}
//
//		Common.createDefaultTimer(new EventListener() {
//
//			@Override
//			public void onEvent(Event arg0) throws Exception {
//				KembaliSuratItemPunyaItemHelper.this.barcode.focus();
//				KembaliSuratItemPunyaItemHelper.this.barcode.select();
//			}
//		});
//	}
}
