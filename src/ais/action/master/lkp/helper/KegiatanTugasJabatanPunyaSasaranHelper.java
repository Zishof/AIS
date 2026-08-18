package ais.action.master.lkp.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;

import ais.action.master.rab.helper.AmbilDataSasaranBanyak;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.lkp.KegiatanTugasJabatan;
import ais.database.model.lkp.KegiatanTugasJabatanPunyaSasaran;
import ais.database.model.rab.Sasaran;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

public class KegiatanTugasJabatanPunyaSasaranHelper {

	private MyGrid gridSasaran;
	private boolean add = false;
	// private boolean edit = false;
	private boolean delete = false;

	public KegiatanTugasJabatanPunyaSasaranHelper(MyGrid gridSasaran) {
		this.gridSasaran = gridSasaran;
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		// edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
	}

	public Borderlayout initDetail(final KegiatanTugasJabatan kegiatanTugasJabatan) {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(north);

		MyToolbarbuttonConfig add = new MyToolbarbuttonConfig("Tambah Sasaran", "/img/new.gif");
		add.setVisible(KegiatanTugasJabatanPunyaSasaranHelper.this.add);
		add.setParent(toolbar);
		add.setTooltiptext("Tambah");
		add.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				List<Sasaran> sasarans = new ArrayList<Sasaran>();
				List<Row> myrows = gridSasaran.getRows().getChildren();
				for (Row row : myrows) {
					sasarans.add(((KegiatanTugasJabatanPunyaSasaran) row
							.getAttribute("kegiatanTugasJabatanPunyaSasaran"))
							.getSasaran());
				}
				AmbilDataSasaranBanyak ambilDataSasaranBanyak = new AmbilDataSasaranBanyak(
						sasarans);
				ambilDataSasaranBanyak.setHeight("95%");
				ambilDataSasaranBanyak.setWidth("90%");
				ambilDataSasaranBanyak.setParent(ExecutionsCtrl
						.getCurrentCtrl().getCurrentPage().getFirstRoot());
				ambilDataSasaranBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Sasaran> sasarans = (List<Sasaran>) arg0.getData();
						for (Sasaran sasaran : sasarans) {
							KegiatanTugasJabatanPunyaSasaran kegiatanTugasJabatanPunyaSasaran = new KegiatanTugasJabatanPunyaSasaran();
							kegiatanTugasJabatanPunyaSasaran.setKegiatanTugasJabatan(kegiatanTugasJabatan);
							kegiatanTugasJabatanPunyaSasaran.setSasaran(sasaran);

							Rows rows = gridSasaran.getRows() == null ? new Rows()
									: gridSasaran.getRows();
							rows.setParent(gridSasaran);
							Row row = new Row();row.setValign("top");
							row.setParent(rows);
							initRow(row, kegiatanTugasJabatanPunyaSasaran);
						}
					}
				});

				ambilDataSasaranBanyak.onModal();

			}
		});

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Common.clear(gridSasaran);
		gridSasaran.setParent(center);
		gridSasaran.setWidth("100%");
		gridSasaran.setHeight("100%");
		Columns columns = new Columns();
		columns.setParent(gridSasaran);

		MyColumnConfig column = new MyColumnConfig("Kode");
		column.setParent(columns);
		column.setWidth("10%");

		column = new MyColumnConfig("Isi Sasaran");
		column.setParent(columns);

		column = new MyColumnConfig("Hapus");
		column.setParent(columns);
		column.setWidth("10%");

		loadDataDetail(kegiatanTugasJabatan);

		return borderlayout;
	}

	@SuppressWarnings("unchecked")
	private void loadDataDetail(final KegiatanTugasJabatan kegiatanTugasJabatan) {

		List<KegiatanTugasJabatanPunyaSasaran> kegiatanTugasJabatanPunyaSasarans = kegiatanTugasJabatan == null
				|| kegiatanTugasJabatan.getId() == null ? new ArrayList<KegiatanTugasJabatanPunyaSasaran>()
				: HibernateUtil.currentSession()
						.createCriteria(KegiatanTugasJabatanPunyaSasaran.class)
						.add(Restrictions.eq("kegiatanTugasJabatan", kegiatanTugasJabatan)).list();

		Rows rows = gridSasaran.getRows() == null ? new Rows() : gridSasaran
				.getRows();
		rows.setParent(gridSasaran);

		for (KegiatanTugasJabatanPunyaSasaran kegiatanTugasJabatanPunyaSasaran : kegiatanTugasJabatanPunyaSasarans) {
			Row row = new Row();row.setValign("top");
			row.setParent(rows);
			initRow(row, kegiatanTugasJabatanPunyaSasaran);
		}
	}

	public void initRow(final Row row,
			final KegiatanTugasJabatanPunyaSasaran kegiatanTugasJabatanPunyaSasaran) {
		row.setValign("top");row.setAttribute("kegiatanTugasJabatanPunyaSasaran", kegiatanTugasJabatanPunyaSasaran);

		new Label(kegiatanTugasJabatanPunyaSasaran.getSasaran() == null ? ""
				: kegiatanTugasJabatanPunyaSasaran.getSasaran().getKode()).setParent(row);

		new Label(kegiatanTugasJabatanPunyaSasaran.getSasaran() == null ? ""
				: kegiatanTugasJabatanPunyaSasaran.getSasaran().getNama()).setParent(row);

		Hbox hbox = new Hbox();
		hbox.setParent(row);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
		button.setTooltiptext("Hapus Data");
		button.setVisible(delete);
		button.setParent(hbox);

		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?",
						"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
						MyMessageboxConfig.QUESTION, new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									if (kegiatanTugasJabatanPunyaSasaran.getId() != null) {
										Session session = HibernateUtil
												.currentSession();
										session.delete(kegiatanTugasJabatanPunyaSasaran);
									}
	row.setVisible(false);row.detach();
								}

							}
						});

			}
		});
	}

}
