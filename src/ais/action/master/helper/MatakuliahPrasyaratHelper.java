package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Matakuliah;
import ais.database.model.MatakuliahPrasyarat;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyPanel;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class MatakuliahPrasyaratHelper implements DataLoader {

	private MyGrid grid;
	private Matakuliah matakuliah;

	class DetailMatakuliahRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final MatakuliahPrasyarat matakuliahPrasyarat = (MatakuliahPrasyarat) data;

			// Prasyarat kini OPSIONAL: bisa null (baris hanya berisi Minimal Nilai Lulus/SKS/IPK).
			final Matakuliah matakuliah = matakuliahPrasyarat.getMatakuliahPrasyarat();
			new Label(matakuliah == null ? "-" : matakuliah.getKode()).setParent(row);
			new Label(matakuliah == null ? "(tanpa prasyarat)" : matakuliah.getNama()).setParent(row);
			new Label(matakuliah == null ? "" : matakuliah.getSks() + "").setParent(row);
			new Label(matakuliah == null || matakuliah.getJurusan() == null ? "" : matakuliah.getJurusan().getNama())
					.setParent(row);
			new Label(matakuliah == null ? "" : matakuliah.getJenisMatakuliah()).setParent(row);

			final MyDoublebox minimalNilaiLulus = new MyDoublebox(matakuliahPrasyarat.getMinimalNilaiLulus());
			minimalNilaiLulus.setParent(row);
			minimalNilaiLulus.setWidth("90%");

			minimalNilaiLulus.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					matakuliahPrasyarat.setMinimalNilaiLulus(minimalNilaiLulus.getValue());
					Common.refreshUpdate(matakuliahPrasyarat);
				}
			});

			Hbox toolbar = new Hbox();
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
									Common.refreshDelete(matakuliahPrasyarat); 

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
			button.setParent(toolbar);
			toolbar.setParent(row);

		}

	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Session session = HibernateUtil.currentSession();
		List<MatakuliahPrasyarat> matakuliahPrasyarat = session.createCriteria(MatakuliahPrasyarat.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.asc("id")).add(Restrictions.eq("matakuliah", matakuliah)).list();

		ListModel strset = new SimpleListModel(matakuliahPrasyarat);
		grid.setRowRenderer(new DetailMatakuliahRenderer());
		grid.setModelCheckMobile(strset);

	}

	private DataLoader getDataloader() {
		return this;
	}

	public void display(final Matakuliah matakuliah, final Component component, final MyWindow window) {
		this.matakuliah = matakuliah;
		Common.clear(component);

		MyPanel panel = new MyPanel();
		panel.setParent(component);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar matakuliah prasyarat");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(panel);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Matakuliah Prasyarat", "/img/new.gif");
		button.addEventListener("onClick", new EventListener() {

			private AmbilDataMatakuliahPrasyaratHelper ambilDataMatakuliahHelper = new AmbilDataMatakuliahPrasyaratHelper();

			@Override
			public void onEvent(Event event) throws Exception {
				ambilDataMatakuliahHelper.display(matakuliah, getDataloader(), window);
			}

		});
		button.setParent(toolbar);

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);grid.getPagingChild().setMold("os");
		grid.setParent(panelchildren);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("SKS");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jurusan");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keberadaan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nilai Lulus");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("10%");
		loadData(null);

	}

}
