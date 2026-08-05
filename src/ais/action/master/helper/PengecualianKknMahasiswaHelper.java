package ais.action.master.helper;


import ais.common.CommonSearchFilterHelper;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.generic.AmbilDataMahasiswaBanyak;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Kkn;
import ais.database.model.Mahasiswa;
import ais.database.model.PengecualianKknMahasiswa;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyPanel;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class PengecualianKknMahasiswaHelper implements DataLoader {

	private MyGrid grid;
	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();

	private Textbox nama;
	private Kkn kkn;

	public PengecualianKknMahasiswaHelper(Kkn kkn) {
		this.kkn = kkn;
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan); 
	}

	class PengecualianKknMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		public PengecualianKknMahasiswaRenderer() {

		}

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final PengecualianKknMahasiswa pengecualianKknMahasiswa = (PengecualianKknMahasiswa) arg1;

			Mahasiswa mahasiswa = pengecualianKknMahasiswa.getMahasiswa();
			new Label(mahasiswa.getNim()).setParent(arg0);
			new Label(mahasiswa.getNama()).setParent(arg0);
			new Label(mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama()).setParent(arg0);
			new Label(mahasiswa.getJurusan() == null || mahasiswa.getJurusan().getFakultas() == null ? ""
					: mahasiswa.getJurusan().getFakultas().getNama()).setParent(arg0);

			final MyTextbox keterangan = new MyTextbox(pengecualianKknMahasiswa.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setParent(arg0);
			keterangan.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					Session session = HibernateUtil.currentSession();
					pengecualianKknMahasiswa.setKeterangan(keterangan.getValue());
					Common.refreshSaveOrUpdate(session, pengecualianKknMahasiswa);
				}
			});

			Hbox hbox = new Hbox();
			hbox.setParent(arg0);

			MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
			toolbarbutton.setOrient("vertical");
			toolbarbutton.setTooltiptext("Hapus Data");
			toolbarbutton.setParent(hbox);
			toolbarbutton.addEventListener("onClick", new EventListener() {
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
									Session session = HibernateUtil.currentSession();

									Common.refreshDelete(session, pengecualianKknMahasiswa);

									loadData(null);
								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
									PesanFormalHelper.tampilkanGagalException(
											"menghapus data pengecualian KKN mahasiswa",
											e,
											new String[] {
													"Periksa apakah data ini masih berelasi dengan data lain (misalnya data kelompok atau penilaian KKN) sehingga tidak dapat dihapus.",
													"Hapus atau lepaskan terlebih dahulu data terkait yang masih berelasi, lalu ulangi proses penghapusan.",
													"Jika data tetap tidak dapat dihapus, konfirmasikan kebutuhan penghapusan ini kepada Administrator." });
								}

							}

						}
					});

				}

			});
		}
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Session session = Common.getManualSession();
		List<PengecualianKknMahasiswa> mahasiswa = session.createCriteria(PengecualianKknMahasiswa.class)
				.add(Restrictions.eq("kkn", kkn)).addOrder(Order.desc("id")).createCriteria("mahasiswa")
				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null || searchjurusan.getSelectedItem().getValue()==null ? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

		.createCriteria("jurusan")
				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null || searchfakultas.getSelectedItem().getValue()==null ? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false))
				.setMaxResults(Common.MAX_RESULT_50).list();

		ListModel strset = new SimpleListModel(mahasiswa);
		grid.setRowRenderer(new PengecualianKknMahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void display() throws InterruptedException {

		final MyWindow window = new MyWindow("Daftar Pengecualian KKN mahasiswa", "none", true);
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);

		window.setWidth("90%");
		window.setHeight("90%");

		MyPanel panel = new MyPanel();
		panel.setParent(window);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Div div = new Div();
		div.setParent(north);

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(div);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setWidth("90%");

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(div);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Data Mahasiswa", "/img/new.gif");

		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				AmbilDataMahasiswaBanyak window = new AmbilDataMahasiswaBanyak(new ArrayList<Mahasiswa>());
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
				window.setWidth("700px");
				window.setHeight("90%");

				window.setEventListener(new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Mahasiswa> mahasiswas = (List<Mahasiswa>) arg0.getData();
						if (mahasiswas != null) {

							Session session = HibernateUtil.currentSession();

							for (Mahasiswa mahasiswa : mahasiswas) {
								PengecualianKknMahasiswa pengecualianKknMahasiswa = new PengecualianKknMahasiswa();
								pengecualianKknMahasiswa.setMahasiswa(mahasiswa);
								pengecualianKknMahasiswa.setKkn(kkn);
								pengecualianKknMahasiswa.setKeterangan("");

								session.save(pengecualianKknMahasiswa);
							}

							loadData(null);

						}
					}
				});

				window.onModal();

			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.setTooltiptext("Cari");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				loadData(null);
			}
		});
		button.setParent(toolbar);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);grid.getPagingChild().setMold("os");
		grid.setParent(center);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("NIM");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jurusan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Fakultas");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");
		column.setWidth("40%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("6%");

		loadData(null);

		South south = new South();
		south.setParent(borderlayout);

		toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);

		button = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		button.setTooltiptext("Tutup");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();
			}
		});
		button.setParent(toolbar);

		window.onModal();
	}

}
