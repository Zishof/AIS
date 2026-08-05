package ais.action.master.penelitiandanpengabdian.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Space;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.penelitiandanpengabdian.PenelitianDanPengabdian;
import ais.database.model.penelitiandanpengabdian.PengajuanPenelitianDanPengabdian;
import ais.database.model.penelitiandanpengabdian.TahapanPelaporanPenelitianDanPengabdian;
import ais.database.model.penelitiandanpengabdian.TipePenelitianDanPengabdian;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyToolbarbutton;
import ais.ui.util.MyCkEditor;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyHboxToolbar;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class TahapanPelaporanPenelitianDanPengabdianHelper implements DataCriteria {

	private MyGrid gridTahapanPelaporan;
	private Combobox searchPenelitianDanPengabdian;

	private Paging paging;
	private PengajuanPenelitianDanPengabdian pengajuanPenelitianDanPengabdian;

	public void displayWindowTahapanPelaporan(final PenelitianDanPengabdian penelitianDanPengabdianData,
			final TahapanPelaporanPenelitianDanPengabdian tahapanPelaporanPenelitianDanPengabdianData)
			throws Exception {
		final MyWindow window = new MyWindow();
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		window.setHeight("99%");
		window.setWidth("800px");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pilih penelitian atau pengabdian"));
		final Combobox penelitianDanPengabdian = new Combobox();
		row.appendChild(penelitianDanPengabdian);
		penelitianDanPengabdian.setWidth("90%");

		Common.insertCombo(penelitianDanPengabdian, "judul", "jenisPenelitianDanPengabdian",
				PenelitianDanPengabdian.class, Restrictions.eq("aktif", true));
		Common.selectComboItem(penelitianDanPengabdian, penelitianDanPengabdianData);
		penelitianDanPengabdian.setReadonly(true);
		if (penelitianDanPengabdianData != null) {
			penelitianDanPengabdian.setDisabled(true);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Tahapan"));
		final Textbox nama;
		row.appendChild(nama = new Textbox(tahapanPelaporanPenelitianDanPengabdianData.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Mulai"));
		final MyDatebox mulai;
		row.appendChild(mulai = new MyDatebox(tahapanPelaporanPenelitianDanPengabdianData.getMulai()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Sampai"));
		final MyDatebox sampai;
		row.appendChild(sampai = new MyDatebox(tahapanPelaporanPenelitianDanPengabdianData.getSampai()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan Tahapan"));
		final MyCkEditor keterangan = new MyCkEditor();
		row.appendChild(keterangan);
		keterangan.setValue(tahapanPelaporanPenelitianDanPengabdianData == null ? ""
				: tahapanPelaporanPenelitianDanPengabdianData.getKeterangan());
		keterangan.setWidth("90%");
		keterangan.setHeight("200px");

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unused")
			@Override
			public void onEvent(Event event) throws Exception {
				Session session = Common.getManualSession();

				if (nama.getValue().trim().isEmpty()) {
					MyMessageboxConfig.show("Nama Tahapan harus diisi", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}
				if (keterangan.getValue().trim().isEmpty()) {
					MyMessageboxConfig.show("Keterangan Tahapan harus diisi", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}
				if (penelitianDanPengabdian.getSelectedItem() == null) {
					MyMessageboxConfig.show("Penelitian dan Pengabdian harus diisi", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}

				TahapanPelaporanPenelitianDanPengabdian tahapanPelaporanPenelitianDanPengabdian = tahapanPelaporanPenelitianDanPengabdianData;
				if (tahapanPelaporanPenelitianDanPengabdian == null) {
					tahapanPelaporanPenelitianDanPengabdian = new TahapanPelaporanPenelitianDanPengabdian();
				}
				tahapanPelaporanPenelitianDanPengabdian.setPenelitianDanPengabdian(
						(PenelitianDanPengabdian) penelitianDanPengabdian.getSelectedItem().getValue());

				tahapanPelaporanPenelitianDanPengabdian.setNama(nama.getValue());
				tahapanPelaporanPenelitianDanPengabdian.setMulai(mulai.getValue());
				tahapanPelaporanPenelitianDanPengabdian.setSampai(sampai.getValue());
				tahapanPelaporanPenelitianDanPengabdian.setKeterangan(keterangan.getValue());

				Common.refreshSaveOrUpdate(session, tahapanPelaporanPenelitianDanPengabdian);

				loadDataTahapanPelaporan();
				window.detach();
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(window);

		window.onModal();
	}

	public void displayTahapanPelaporan(final Boolean ases, final TipePenelitianDanPengabdian jenis,
			final PenelitianDanPengabdian penelitianDanPengabdianData,
			final PengajuanPenelitianDanPengabdian pengajuanPenelitianDanPengabdian, Component component)
			throws Exception {
		Tbmuser tbmuser = Common.getCurrentUser();
		this.pengajuanPenelitianDanPengabdian = pengajuanPenelitianDanPengabdian;

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setParent(component);
		groupbox.setStyle("min-height: 300px;");
		groupbox.setWidth("95%");

		MyHboxToolbar toolbar = new MyHboxToolbar();
		// toolbar.setHeight("25px");
		toolbar.setVisible(tbmuser != null && tbmuser.ambilDosen() == null && tbmuser.getMahasiswa() == null);
		toolbar.setParent(groupbox);
		MyToolbarbutton button = new MyToolbarbutton("fa-plus-circle",
				"Tambah Tahapan Pelaporan " + (jenis == null ? "Penelitian/Pengabdian" : jenis));
		button.setVisible(!ases);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				TahapanPelaporanPenelitianDanPengabdian tahapanPelaporanPenelitianDanPengabdianData = new TahapanPelaporanPenelitianDanPengabdian();
				tahapanPelaporanPenelitianDanPengabdianData.setPenelitianDanPengabdian(penelitianDanPengabdianData);
				displayWindowTahapanPelaporan(penelitianDanPengabdianData, tahapanPelaporanPenelitianDanPengabdianData);

			}

		});
		button.setParent(toolbar);

		String[] contents = new String[] { "id", "penelitianDanPengabdian", "nama", "mulai", "sampai", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(
				TahapanPelaporanPenelitianDanPengabdian.class, this, "Download", "/img/print.png", contents);
		toolbar.appendChild(cetakToolbarbutton);

		toolbar.appendChild(new Space());
		toolbar.appendChild(new Space());
		toolbar.appendChild(new Space());

		
		searchPenelitianDanPengabdian = new Combobox();

		Criteria criteria = HibernateUtil.currentSession().createCriteria(PenelitianDanPengabdian.class)

				.add(jenis == null ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.isNull("tipePenelitianDanPengabdian"),
								Restrictions.eq("tipePenelitianDanPengabdian", jenis)));

		List<?> list = criteria.list();

		Common.insertComboItems(searchPenelitianDanPengabdian, "judul", "jenisPenelitianDanPengabdian", list);

		Common.selectComboItem(searchPenelitianDanPengabdian, penelitianDanPengabdianData);
		if (penelitianDanPengabdianData != null) {
			searchPenelitianDanPengabdian.setDisabled(true);
		}
		searchPenelitianDanPengabdian.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDataTahapanPelaporan();
			}
		});

		if (pengajuanPenelitianDanPengabdian == null) {
			toolbar.appendChild(new Label((jenis == null ? "Penelitian/Pengabdian" : jenis) + " : "));
			toolbar.appendChild(searchPenelitianDanPengabdian);

			MyToolbarbutton cari = new MyToolbarbutton("fa-search", "Cari");
			cari.setParent(toolbar);
			cari.setTooltiptext("Cari");
			cari.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					loadDataTahapanPelaporan();
				}
			});
		}

		gridTahapanPelaporan = new MyGrid();
		gridTahapanPelaporan.setMold("paging");
		gridTahapanPelaporan.setPageSize(1000);
		gridTahapanPelaporan.setParent(groupbox);

		Columns columns = new Columns();
		columns.setParent(gridTahapanPelaporan);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("40px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel((jenis == null ? "Penelitian/Pengabdian" : jenis.toString()));
		column.setWidth(penelitianDanPengabdianData == null ? "20%" : "0%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama Tahapan");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Mulai");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Sampai");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tahap");
		column.setWidth(ases ? "5%" : "8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan Tahapan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth(ases ? "0%" : "10%");

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDataTahapanPelaporan();
			}
		});
		paging.setParent(groupbox);

		loadDataTahapanPelaporan();

		System.out.println("jenis -> " + jenis);

	}

	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(TahapanPelaporanPenelitianDanPengabdian.class)
				.add(searchPenelitianDanPengabdian.getSelectedItem() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("penelitianDanPengabdian",
								searchPenelitianDanPengabdian.getSelectedItem().getValue()));

		if (order) {
			criteria.addOrder(Order.asc("mulai"));
		}

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void loadDataTahapanPelaporan() {
		Common.initPaging(initCriteria(false), paging);
		List<TahapanPelaporanPenelitianDanPengabdian> tahapanPelaporanPenelitianDanPengabdian = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(tahapanPelaporanPenelitianDanPengabdian);

		gridTahapanPelaporan.setRowRenderer(new DetailTahapanPelaporanPenelitianDanPengabdianRenderer());
		gridTahapanPelaporan.setModelCheckMobile(strset);
		gridTahapanPelaporan.renderAll();

	}

	class DetailTahapanPelaporanPenelitianDanPengabdianRenderer extends ais.ui.util.MyRowRenderer {

		private PengajuanTahapanPelaporanPenelitianDanPengabdianHelper pengajuanTahapanPelaporanPenelitianDanPengabdianHelper = new PengajuanTahapanPelaporanPenelitianDanPengabdianHelper();

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			arg0.setValign("top");
			final TahapanPelaporanPenelitianDanPengabdian tahapanPelaporanPenelitianDanPengabdian = (TahapanPelaporanPenelitianDanPengabdian) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {
						
						pengajuanTahapanPelaporanPenelitianDanPengabdianHelper.displayPengajuan(
								tahapanPelaporanPenelitianDanPengabdian, pengajuanPenelitianDanPengabdian, detail);
					}
				}
			});

			new Label(tahapanPelaporanPenelitianDanPengabdian.getPenelitianDanPengabdian().getNama()).setParent(arg0);
			new Label(tahapanPelaporanPenelitianDanPengabdian.getNama()).setParent(arg0);
			new Label(Common.dateFormat4.get().format(tahapanPelaporanPenelitianDanPengabdian.getMulai())).setParent(arg0);
			new Label(Common.dateFormat4.get().format(tahapanPelaporanPenelitianDanPengabdian.getSampai())).setParent(arg0);

			Tbmuser tbmuser = Common.getCurrentUser();

			List<String> koresponden = new ArrayList<String>();
			if (tahapanPelaporanPenelitianDanPengabdian.getPenelitianDanPengabdian() != null) {
				for (String s : tahapanPelaporanPenelitianDanPengabdian.getPenelitianDanPengabdian().getKorespondensi()
						.split(",")) {
					if (!s.trim().isEmpty()) {
						koresponden.add(s.trim());
					}
				}
			}

			List<String> korespondenGrup = new ArrayList<String>();
			if (tahapanPelaporanPenelitianDanPengabdian.getPenelitianDanPengabdian() != null) {

				for (String s : tahapanPelaporanPenelitianDanPengabdian.getPenelitianDanPengabdian()
						.getKorespondensiGrupPengguna().split(",")) {
					if (!s.trim().isEmpty()) {
						korespondenGrup.add(s.trim());
					}
				}
			}

			if (koresponden.contains(tbmuser.getUserId())
					|| korespondenGrup.contains(tbmuser.hakAkses().getRoleId())) {
				final Combobox tahapPengajuan = new Combobox();
				tahapPengajuan.setParent(arg0);
				tahapPengajuan.setWidth("90%");
				MyComboitemConfig comboitem = new MyComboitemConfig(PengajuanPenelitianDanPengabdian.TAHAP_PROPOSAL);
				comboitem.setValue(PengajuanPenelitianDanPengabdian.TAHAP_PROPOSAL);
				tahapPengajuan.appendChild(comboitem);

				comboitem = new MyComboitemConfig(PengajuanPenelitianDanPengabdian.TAHAP_PENGUMPULAN_DATA);
				comboitem.setValue(PengajuanPenelitianDanPengabdian.TAHAP_PENGUMPULAN_DATA);
				tahapPengajuan.appendChild(comboitem);

				comboitem = new MyComboitemConfig(PengajuanPenelitianDanPengabdian.TAHAP_ANALISIS_DATA);
				comboitem.setValue(PengajuanPenelitianDanPengabdian.TAHAP_ANALISIS_DATA);
				tahapPengajuan.appendChild(comboitem);

				comboitem = new MyComboitemConfig(PengajuanPenelitianDanPengabdian.TAHAP_LAPORAN_AKHIR);
				comboitem.setValue(PengajuanPenelitianDanPengabdian.TAHAP_LAPORAN_AKHIR);
				tahapPengajuan.appendChild(comboitem);

				Common.selectComboItem(tahapPengajuan, tahapanPelaporanPenelitianDanPengabdian.getTahapPengajuan());
				tahapPengajuan.setReadonly(true);

				tahapPengajuan.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						tahapanPelaporanPenelitianDanPengabdian
								.setTahapPengajuan((String) (tahapPengajuan.getSelectedItem() == null ? null
										: tahapPengajuan.getSelectedItem().getValue()));
						Common.refreshUpdate(tahapanPelaporanPenelitianDanPengabdian);
					}
				});
			} else {
				new Label(tahapanPelaporanPenelitianDanPengabdian.getTahapPengajuan()).setParent(arg0);
			}

			new ais.ui.util.MyHtml(tahapanPelaporanPenelitianDanPengabdian.getKeterangan()).setParent(arg0);

			Hbox hbox = new Hbox();
			hbox.setParent(arg0);
			MyToolbarbutton toolbarbutton = new MyToolbarbutton("fa-pencil-square-o", "Ubah");
			toolbarbutton.setParent(hbox);
			toolbarbutton.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					displayWindowTahapanPelaporan(tahapanPelaporanPenelitianDanPengabdian.getPenelitianDanPengabdian(),
							tahapanPelaporanPenelitianDanPengabdian);
				}

			});

			toolbarbutton = new MyToolbarbutton("fa-trash", "Hapus");
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
											Common.refreshDelete(tahapanPelaporanPenelitianDanPengabdian);

											loadDataTahapanPelaporan();
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(
													"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
										}

									}

								}
							});

				}

			});
		}
	}

}
