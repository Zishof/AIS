package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Columns;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.dao.DaoFactory;
import ais.database.dao.DetailBiayaDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DetailBiaya;
import ais.database.model.JenisKegiatanDetail;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class DetailBiayaHelper implements DataLoader {

	private MyGrid grid;
	private JenisKegiatanDetail jenisKegiatanDetail;
	private DetailBiaya[] detailBiayas;

	class DetailBiayaRenderer extends ais.ui.util.MyRowRenderer {

		public DetailBiayaRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final DetailBiaya detailBiaya = (DetailBiaya) data;

			new Label(detailBiaya.getItemBiaya().getNama()).setParent(row);
			new Label(detailBiaya.getJenisKegiatan().getNamaKegiatan()).setParent(row);
			new Label((detailBiaya.getNilaiBiaya() == null ? new Double(0.0) : detailBiaya.getNilaiBiaya()).toString())
					.setParent(row);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

						@Override
						public void onEvent(Event event) throws Exception {
							int i = Integer.parseInt(event.getData().toString());
							if (i == MyMessageboxConfig.OK) {
								try {
									DetailBiayaDao detailBiayaDao = DaoFactory.getInstance().getDetailBiayaDao();
									// detailBiayaDao.beginTransaction();
									detailBiayaDao.delete(detailBiayaDao.merge(detailBiaya));
									// detailBiayaDao.commitTransaction();

									loadData(null);
								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e); 
									PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
								}

							}

						}
					});

				}

			});
			// button.setParent(toolbar);
			// toolbar.setParent(south);

		}

	}

	public void loadData(Object value) {

		ListModel strset = new SimpleListModel(detailBiayas);
		grid.setRowRenderer(new DetailBiayaRenderer());
		grid.setModelCheckMobile(strset);

	}

	private DataLoader getDataloader() {
		return this;
	}

	public void displayDetailBiaya(final JenisKegiatanDetail jenisKegiatanDetail, final Component component,
			final MyWindow window) {
		this.jenisKegiatanDetail = (JenisKegiatanDetail) HibernateUtil.currentSession().load(JenisKegiatanDetail.class,
				jenisKegiatanDetail.getId());
		Common.clear(component);

		detailBiayas = this.jenisKegiatanDetail.getDetailBiayas().toArray(new DetailBiaya[] {});
		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 300px;");
		groupbox.setParent(component);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Data", "/img/new.gif");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				AmbilDetailBiayaHelper ambilDetailBiayaHelper = new AmbilDetailBiayaHelper(jenisKegiatanDetail);
				ambilDetailBiayaHelper.display(jenisKegiatanDetail.getJenisKegiatan(), getDataloader(), window);
			}
		});
		button.setParent(toolbar);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);grid.getPagingChild().setMold("os");
		grid.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Item Biaya");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jenis Biaya");
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nilai");
		column.setWidth("35%");

		// column = new MyColumnConfig();
		// column.setParent(columns);
		// column.setLabel("");
		// column.setWidth("5%");

		loadData(null);
		// borderlayout.setParent(component);

	}

}
