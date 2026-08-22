package ais.action.master.sekolah;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sekolah.GelombangPendaftaranPsb;
import ais.database.model.sekolah.UjianPSB;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class UjianPSBAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267902900328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Combobox searchgelombangPendaftaranPsb;

	private Textbox nama;
	private Textbox lokasi;
	private Intbox jumlahHariUjian;
	private MyDatebox tanggalUjian1;
	private MyDatebox tanggalUjian2;
	private MyDatebox tanggalUjian3;
	private MyDatebox tanggalUjian4;
	private MyDatebox tanggalUjian5;
	private MyDatebox tanggalUjian6;
	private MyDatebox tanggalUjian7;
	private MyDatebox tanggalUjian8;
	private MyDatebox tanggalUjian9;
	private MyDatebox tanggalUjian10;
	private MyCheckboxConfig tampilkanJadwalUjianDiKartuUjian;

	private Textbox keterangan;
	private Textbox keteranganSetelahBayar;

	// private Textbox keteranganHeader;
	// private Textbox keteranganSetelahBayarHeader;

	private boolean edit = false;
	private boolean delete = false;

	private UjianPSB ujianPSB;
	private MyToolbarbuttonConfig add;
	private Row row2;
	private Row row3;
	private Row row9;
	private Row row5;
	private Row row6;
	private Row row7;
	private Row row8;
	private Row row4;
	private Row row10;
	private Combobox gelombangPendaftaranPsb;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		Common.insertCombo(searchgelombangPendaftaranPsb, "nama", GelombangPendaftaranPsb.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		if (!searchgelombangPendaftaranPsb.getChildren().isEmpty()) {
			searchgelombangPendaftaranPsb.setSelectedIndex(0);
		}
		if (searchgelombangPendaftaranPsb != null) { searchgelombangPendaftaranPsb.setReadonly(true); }

		if (execution.getParameter("gelombangPendaftaranPsb") != null) {
			GelombangPendaftaranPsb gel = (GelombangPendaftaranPsb) HibernateUtil.currentSession()
					.createCriteria(GelombangPendaftaranPsb.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("gelombangPendaftaranPsb"))))
					.uniqueResult();
			if (gel != null) {
				Common.selectComboItem(true, searchgelombangPendaftaranPsb, gel);
				searchgelombangPendaftaranPsb.setDisabled(true);
			}
		}

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	}

	class UjianPSBRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			arg0.setValign("top");
			final UjianPSB ujianPSB = (UjianPSB) arg1;

			RevisiHelper.createNewRevisi(UjianPSB.class, ujianPSB, ujianPSB.getNama()).setParent(arg0);

			new Label(ujianPSB.getTahunAkademik()).setParent(arg0);
			new MyLabelAgakKecil(ujianPSB.getGelombangPendaftaranPsb() == null ? ""
					: ujianPSB.getGelombangPendaftaranPsb().toString()).setParent(arg0);
			new Label(ujianPSB.getJumlahHariUjian().toString()).setParent(arg0);

			String dosenPengampu = "<font style='font-size:9px;'>" + "<ol>";
			if (ujianPSB.getTanggalUjian1() != null) {
				dosenPengampu += "<li>" + Common.dateFormat2.get().format(ujianPSB.getTanggalUjian1()) + "</li>";
			}
			if (ujianPSB.getTanggalUjian2() != null) {
				dosenPengampu += "<li>" + Common.dateFormat2.get().format(ujianPSB.getTanggalUjian2()) + "</li>";
			}
			if (ujianPSB.getTanggalUjian3() != null) {
				dosenPengampu += "<li>" + Common.dateFormat2.get().format(ujianPSB.getTanggalUjian3()) + "</li>";
			}
			if (ujianPSB.getTanggalUjian4() != null) {
				dosenPengampu += "<li>" + Common.dateFormat2.get().format(ujianPSB.getTanggalUjian4()) + "</li>";
			}
			if (ujianPSB.getTanggalUjian5() != null) {
				dosenPengampu += "<li>" + Common.dateFormat2.get().format(ujianPSB.getTanggalUjian5()) + "</li>";
			}
			if (ujianPSB.getTanggalUjian6() != null) {
				dosenPengampu += "<li>" + Common.dateFormat2.get().format(ujianPSB.getTanggalUjian6()) + "</li>";
			}
			if (ujianPSB.getTanggalUjian7() != null) {
				dosenPengampu += "<li>" + Common.dateFormat2.get().format(ujianPSB.getTanggalUjian7()) + "</li>";
			}
			if (ujianPSB.getTanggalUjian8() != null) {
				dosenPengampu += "<li>" + Common.dateFormat2.get().format(ujianPSB.getTanggalUjian8()) + "</li>";
			}
			if (ujianPSB.getTanggalUjian9() != null) {
				dosenPengampu += "<li>" + Common.dateFormat2.get().format(ujianPSB.getTanggalUjian9()) + "</li>";
			}
			if (ujianPSB.getTanggalUjian10() != null) {
				dosenPengampu += "<li>" + Common.dateFormat2.get().format(ujianPSB.getTanggalUjian10()) + "</li>";
			}
			dosenPengampu += "</ol>" + "</font>";

			new ais.ui.util.MyHtml(dosenPengampu).setParent(arg0);

			new Label(ujianPSB.getLokasi()).setParent(arg0);
			new Label(ujianPSB.getTampilkanJadwalUjianDiKartuUjian() ? "Ya" : "Tidak").setParent(arg0);
			new MyLabelKecil(ujianPSB.getKeterangan()).setParent(arg0);
			new MyLabelKecil(ujianPSB.getKeteranganSetelahBayar()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(ujianPSB);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
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

											Common.refreshDelete(ujianPSB);

											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e); 
											MyMessageboxConfig
													.show("Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
										}

									}

								}
							});

				}
			});
			button.setParent(toolbar);
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new UjianPSB());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(UjianPSB ujianPSB) throws Exception {
		this.ujianPSB = ujianPSB;
		addWindow.setTitle(ujianPSB.getId() == null ? "Tambah Ujian PSB" : "Ubah Ujian PSB");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
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
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Ujian PSB"));
		row.appendChild(nama = new Textbox(ujianPSB.getNama() == null ? "" : ujianPSB.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lokasi Ujian PSB"));
		row.appendChild(lokasi = new Textbox(ujianPSB.getLokasi()));
		lokasi.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Gelombang Pendaftaran"));
		row.appendChild(gelombangPendaftaranPsb = new Combobox());
		gelombangPendaftaranPsb.setWidth("90%");

		Common.insertCombo(gelombangPendaftaranPsb, new String[] { "nama", "mulai", "sampai", "jenisSeleksi" },
				"tahunAjaran", GelombangPendaftaranPsb.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		Common.selectComboItem(gelombangPendaftaranPsb, ujianPSB.getGelombangPendaftaranPsb());

		if (searchgelombangPendaftaranPsb.getSelectedItem() != null
				&& searchgelombangPendaftaranPsb.getSelectedItem().getValue() != null) {
			Common.selectComboItem(gelombangPendaftaranPsb, searchgelombangPendaftaranPsb.getSelectedItem().getValue());
			gelombangPendaftaranPsb.setDisabled(searchgelombangPendaftaranPsb.isDisabled());

		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jumlah Hari Ujian"));
		row.appendChild(jumlahHariUjian = new Intbox(ujianPSB.getJumlahHariUjian()));
		jumlahHariUjian.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tampilkan Info Ujian Di Kartu Peserta"));
		row.appendChild(tampilkanJadwalUjianDiKartuUjian = new MyCheckboxConfig());
		tampilkanJadwalUjianDiKartuUjian.setChecked(ujianPSB.getTampilkanJadwalUjianDiKartuUjian());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Ujian"));
		row.appendChild(tanggalUjian1 = new MyDatebox(ujianPSB.getTanggalUjian1()));
		tanggalUjian1.setWidth("90%");

		row2 = new MyFormRow();
		row2.setStyle("border:0px;background: transparent;");
		row2.setParent(rows);
		row2.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Ujian ke 2")));
		row2.appendChild(tanggalUjian2 = new MyDatebox(ujianPSB.getTanggalUjian2()));
		tanggalUjian2.setWidth("90%");

		row3 = new MyFormRow();
		row3.setStyle("border:0px;background: transparent;");
		row3.setParent(rows);
		row3.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Ujian ke 3")));
		row3.appendChild(tanggalUjian3 = new MyDatebox(ujianPSB.getTanggalUjian3()));
		tanggalUjian3.setWidth("90%");

		row4 = new MyFormRow();
		row4.setStyle("border:0px;background: transparent;");
		row4.setParent(rows);
		row4.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Ujian ke 4")));
		row4.appendChild(tanggalUjian4 = new MyDatebox(ujianPSB.getTanggalUjian4()));
		tanggalUjian4.setWidth("90%");

		row5 = new MyFormRow();
		row5.setStyle("border:0px;background: transparent;");
		row5.setParent(rows);
		row5.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Ujian ke 5")));
		row5.appendChild(tanggalUjian5 = new MyDatebox(ujianPSB.getTanggalUjian5()));
		tanggalUjian5.setWidth("90%");

		row6 = new MyFormRow();
		row6.setStyle("border:0px;background: transparent;");
		row6.setParent(rows);
		row6.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Ujian ke 6")));
		row6.appendChild(tanggalUjian6 = new MyDatebox(ujianPSB.getTanggalUjian6()));
		tanggalUjian6.setWidth("90%");

		row7 = new MyFormRow();
		row7.setStyle("border:0px;background: transparent;");
		row7.setParent(rows);
		row7.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Ujian ke 7")));
		row7.appendChild(tanggalUjian7 = new MyDatebox(ujianPSB.getTanggalUjian7()));
		tanggalUjian7.setWidth("90%");

		row8 = new MyFormRow();
		row8.setStyle("border:0px;background: transparent;");
		row8.setParent(rows);
		row8.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Ujian ke 8")));
		row8.appendChild(tanggalUjian8 = new MyDatebox(ujianPSB.getTanggalUjian8()));
		tanggalUjian8.setWidth("90%");

		row9 = new MyFormRow();
		row9.setStyle("border:0px;background: transparent;");
		row9.setParent(rows);
		row9.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Ujian ke 9")));
		row9.appendChild(tanggalUjian9 = new MyDatebox(ujianPSB.getTanggalUjian9()));
		tanggalUjian9.setWidth("90%");

		row10 = new MyFormRow();
		row10.setStyle("border:0px;background: transparent;");
		row10.setParent(rows);
		row10.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Ujian ke 10")));
		row10.appendChild(tanggalUjian10 = new MyDatebox(ujianPSB.getTanggalUjian10()));
		tanggalUjian10.setWidth("90%");

		EventListener rowEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				int jumlahUjian = jumlahHariUjian.getValue() == null ? 1 : jumlahHariUjian.getValue();
				jumlahHariUjian.setValue(jumlahUjian);
				row2.setVisible(jumlahUjian > 1);
				row3.setVisible(jumlahUjian > 2);
				row4.setVisible(jumlahUjian > 3);
				row5.setVisible(jumlahUjian > 4);
				row6.setVisible(jumlahUjian > 5);
				row7.setVisible(jumlahUjian > 6);
				row8.setVisible(jumlahUjian > 7);
				row9.setVisible(jumlahUjian > 8);
				row10.setVisible(jumlahUjian > 9);
			}
		};

		jumlahHariUjian.addEventListener("onChange", rowEventListener);
		rowEventListener.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Informasi ke peserta ujian pada kartu pembayaran"));
		row.appendChild(keterangan = new Textbox(ujianPSB.getKeterangan() == null ? "" : ujianPSB.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(6);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Informasi ke peserta ujian pada kartu Ujian"));
		row.appendChild(keteranganSetelahBayar = new Textbox(ujianPSB.getKeteranganSetelahBayar()));
		keteranganSetelahBayar.setWidth("90%");
		keteranganSetelahBayar.setRows(6);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Nama Ujian PSB harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (lokasi.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Lokasi Ujian PSB harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (gelombangPendaftaranPsb.getSelectedItem() == null) {
			MyMessageboxConfig.show("Gelombang Pendaftaran Ujian PSB harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (ujianPSB.getId() != null) {
			ujianPSB = (UjianPSB) session.load(UjianPSB.class, ujianPSB.getId());

		}

		ujianPSB.setKeteranganSetelahBayar(keteranganSetelahBayar.getValue());
		ujianPSB.setTampilkanJadwalUjianDiKartuUjian(tampilkanJadwalUjianDiKartuUjian.isChecked());
		ujianPSB.setNama(nama.getValue());
		ujianPSB.setLokasi(lokasi.getValue());
		ujianPSB.setKeterangan(keterangan.getValue());

		ujianPSB.setJumlahHariUjian(jumlahHariUjian.getValue());
		ujianPSB.setTanggalUjian1(tanggalUjian1.getValue());
		ujianPSB.setTanggalUjian2(tanggalUjian2.getValue());
		ujianPSB.setTanggalUjian3(tanggalUjian3.getValue());
		ujianPSB.setTanggalUjian4(tanggalUjian4.getValue());
		ujianPSB.setTanggalUjian5(tanggalUjian5.getValue());
		ujianPSB.setTanggalUjian6(tanggalUjian6.getValue());
		ujianPSB.setTanggalUjian7(tanggalUjian7.getValue());
		ujianPSB.setTanggalUjian8(tanggalUjian8.getValue());
		ujianPSB.setTanggalUjian9(tanggalUjian9.getValue());
		ujianPSB.setTanggalUjian10(tanggalUjian10.getValue());
		ujianPSB.setGelombangPendaftaranPsb(
				(GelombangPendaftaranPsb) gelombangPendaftaranPsb.getSelectedItem().getValue());

		Common.refreshSaveOrUpdate(session, ujianPSB);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(UjianPSB.class).createAlias("gelombangPendaftaranPsb",
				"gelombangPendaftaranPsb");

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchgelombangPendaftaranPsb.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("gelombangPendaftaranPsb",
								searchgelombangPendaftaranPsb.getSelectedItem().getValue()));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<UjianPSB> ujianPSB = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(ujianPSB);
		grid.setRowRenderer(new UjianPSBRenderer());
		grid.setModelCheckMobile(strset);

	}

}
