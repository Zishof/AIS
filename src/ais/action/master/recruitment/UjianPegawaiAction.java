package ais.action.master.recruitment;

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
import ais.database.model.recruitment.GelombangPendaftaranPegawai;
import ais.database.model.recruitment.UjianPegawai;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class UjianPegawaiAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267902900328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Combobox searchgelombangPendaftaranPegawai;

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

	private UjianPegawai ujianPegawai;
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
	private Combobox gelombangPendaftaranPegawai;

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

		Common.insertCombo(searchgelombangPendaftaranPegawai, "nama", GelombangPendaftaranPegawai.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		if (!searchgelombangPendaftaranPegawai.getChildren().isEmpty()) {
			searchgelombangPendaftaranPegawai.setSelectedIndex(0);
		}
		if (searchgelombangPendaftaranPegawai != null) { searchgelombangPendaftaranPegawai.setReadonly(true); }

		if (execution.getParameter("gelombangPendaftaranPegawai") != null) {
			GelombangPendaftaranPegawai gel = (GelombangPendaftaranPegawai) HibernateUtil.currentSession()
					.createCriteria(GelombangPendaftaranPegawai.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("gelombangPendaftaranPegawai"))))
					.uniqueResult();
			if (gel != null) {
				Common.selectComboItem(true, searchgelombangPendaftaranPegawai, gel);
				searchgelombangPendaftaranPegawai.setDisabled(true);
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

	class UjianPegawaiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			arg0.setValign("top");
			final UjianPegawai ujianPegawai = (UjianPegawai) arg1;

			RevisiHelper.createNewRevisi(UjianPegawai.class, ujianPegawai, ujianPegawai.getNama()).setParent(arg0);

			new MyLabelAgakKecil(ujianPegawai.getGelombangPendaftaranPegawai() == null ? ""
					: ujianPegawai.getGelombangPendaftaranPegawai().toString()).setParent(arg0);
			new Label(ujianPegawai.getJumlahHariUjian().toString()).setParent(arg0);

			String dosenPengampu = "<font style='font-size:9px;'>" + "<ol>";
			if (ujianPegawai.getTanggalUjian1() != null) {
				dosenPengampu += "<li>" + Common.dateFormat2.get().format(ujianPegawai.getTanggalUjian1()) + "</li>";
			}
			if (ujianPegawai.getTanggalUjian2() != null) {
				dosenPengampu += "<li>" + Common.dateFormat2.get().format(ujianPegawai.getTanggalUjian2()) + "</li>";
			}
			if (ujianPegawai.getTanggalUjian3() != null) {
				dosenPengampu += "<li>" + Common.dateFormat2.get().format(ujianPegawai.getTanggalUjian3()) + "</li>";
			}
			if (ujianPegawai.getTanggalUjian4() != null) {
				dosenPengampu += "<li>" + Common.dateFormat2.get().format(ujianPegawai.getTanggalUjian4()) + "</li>";
			}
			if (ujianPegawai.getTanggalUjian5() != null) {
				dosenPengampu += "<li>" + Common.dateFormat2.get().format(ujianPegawai.getTanggalUjian5()) + "</li>";
			}
			if (ujianPegawai.getTanggalUjian6() != null) {
				dosenPengampu += "<li>" + Common.dateFormat2.get().format(ujianPegawai.getTanggalUjian6()) + "</li>";
			}
			if (ujianPegawai.getTanggalUjian7() != null) {
				dosenPengampu += "<li>" + Common.dateFormat2.get().format(ujianPegawai.getTanggalUjian7()) + "</li>";
			}
			if (ujianPegawai.getTanggalUjian8() != null) {
				dosenPengampu += "<li>" + Common.dateFormat2.get().format(ujianPegawai.getTanggalUjian8()) + "</li>";
			}
			if (ujianPegawai.getTanggalUjian9() != null) {
				dosenPengampu += "<li>" + Common.dateFormat2.get().format(ujianPegawai.getTanggalUjian9()) + "</li>";
			}
			if (ujianPegawai.getTanggalUjian10() != null) {
				dosenPengampu += "<li>" + Common.dateFormat2.get().format(ujianPegawai.getTanggalUjian10()) + "</li>";
			}
			dosenPengampu += "</ol>" + "</font>";

			new ais.ui.util.MyHtml(dosenPengampu).setParent(arg0);

			new Label(ujianPegawai.getLokasi()).setParent(arg0);
			new Label(ujianPegawai.getTampilkanJadwalUjianDiKartuUjian() ? "Ya" : "Tidak").setParent(arg0);
			new MyLabelKecil(ujianPegawai.getKeterangan()).setParent(arg0);
			new MyLabelKecil(ujianPegawai.getKeteranganSetelahBayar()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(ujianPegawai);
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

											Common.refreshDelete(ujianPegawai);

											onSearchDefault(event);
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
			button.setParent(toolbar);
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new UjianPegawai());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(UjianPegawai ujianPegawai) throws Exception {
		this.ujianPegawai = ujianPegawai;
		addWindow.setTitle(ujianPegawai.getId() == null ? "Tambah Ujian Pegawai" : "Ubah Ujian Pegawai");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Ujian Pegawai"));
		row.appendChild(nama = new Textbox(ujianPegawai.getNama() == null ? "" : ujianPegawai.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lokasi Ujian Pegawai"));
		row.appendChild(lokasi = new Textbox(ujianPegawai.getLokasi()));
		lokasi.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Gelombang Pendaftaran"));
		row.appendChild(gelombangPendaftaranPegawai = new Combobox());
		gelombangPendaftaranPegawai.setWidth("90%");

		Common.insertCombo(gelombangPendaftaranPegawai, new String[] { "nama", "mulai", "sampai", "jenisSeleksi" },
				"tahunAjaran", GelombangPendaftaranPegawai.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		Common.selectComboItem(gelombangPendaftaranPegawai, ujianPegawai.getGelombangPendaftaranPegawai());

		if (searchgelombangPendaftaranPegawai.getSelectedItem() != null
				&& searchgelombangPendaftaranPegawai.getSelectedItem().getValue() != null) {
			Common.selectComboItem(gelombangPendaftaranPegawai,
					searchgelombangPendaftaranPegawai.getSelectedItem().getValue());
			gelombangPendaftaranPegawai.setDisabled(searchgelombangPendaftaranPegawai.isDisabled());

		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jumlah Hari Ujian"));
		row.appendChild(jumlahHariUjian = new Intbox(ujianPegawai.getJumlahHariUjian()));
		jumlahHariUjian.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tampilkan Info Ujian Di Kartu Peserta"));
		row.appendChild(tampilkanJadwalUjianDiKartuUjian = new MyCheckboxConfig());
		tampilkanJadwalUjianDiKartuUjian.setChecked(ujianPegawai.getTampilkanJadwalUjianDiKartuUjian());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Ujian"));
		row.appendChild(tanggalUjian1 = new MyDatebox(ujianPegawai.getTanggalUjian1()));
		tanggalUjian1.setWidth("90%");

		row2 = new MyFormRow();
		row2.setStyle("border:0px;background: transparent;");
		row2.setParent(rows);
		row2.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Ujian ke 2")));
		row2.appendChild(tanggalUjian2 = new MyDatebox(ujianPegawai.getTanggalUjian2()));
		tanggalUjian2.setWidth("90%");

		row3 = new MyFormRow();
		row3.setStyle("border:0px;background: transparent;");
		row3.setParent(rows);
		row3.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Ujian ke 3")));
		row3.appendChild(tanggalUjian3 = new MyDatebox(ujianPegawai.getTanggalUjian3()));
		tanggalUjian3.setWidth("90%");

		row4 = new MyFormRow();
		row4.setStyle("border:0px;background: transparent;");
		row4.setParent(rows);
		row4.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Ujian ke 4")));
		row4.appendChild(tanggalUjian4 = new MyDatebox(ujianPegawai.getTanggalUjian4()));
		tanggalUjian4.setWidth("90%");

		row5 = new MyFormRow();
		row5.setStyle("border:0px;background: transparent;");
		row5.setParent(rows);
		row5.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Ujian ke 5")));
		row5.appendChild(tanggalUjian5 = new MyDatebox(ujianPegawai.getTanggalUjian5()));
		tanggalUjian5.setWidth("90%");

		row6 = new MyFormRow();
		row6.setStyle("border:0px;background: transparent;");
		row6.setParent(rows);
		row6.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Ujian ke 6")));
		row6.appendChild(tanggalUjian6 = new MyDatebox(ujianPegawai.getTanggalUjian6()));
		tanggalUjian6.setWidth("90%");

		row7 = new MyFormRow();
		row7.setStyle("border:0px;background: transparent;");
		row7.setParent(rows);
		row7.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Ujian ke 7")));
		row7.appendChild(tanggalUjian7 = new MyDatebox(ujianPegawai.getTanggalUjian7()));
		tanggalUjian7.setWidth("90%");

		row8 = new MyFormRow();
		row8.setStyle("border:0px;background: transparent;");
		row8.setParent(rows);
		row8.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Ujian ke 8")));
		row8.appendChild(tanggalUjian8 = new MyDatebox(ujianPegawai.getTanggalUjian8()));
		tanggalUjian8.setWidth("90%");

		row9 = new MyFormRow();
		row9.setStyle("border:0px;background: transparent;");
		row9.setParent(rows);
		row9.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Ujian ke 9")));
		row9.appendChild(tanggalUjian9 = new MyDatebox(ujianPegawai.getTanggalUjian9()));
		tanggalUjian9.setWidth("90%");

		row10 = new MyFormRow();
		row10.setStyle("border:0px;background: transparent;");
		row10.setParent(rows);
		row10.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Ujian ke 10")));
		row10.appendChild(tanggalUjian10 = new MyDatebox(ujianPegawai.getTanggalUjian10()));
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
		row.appendChild(
				keterangan = new Textbox(ujianPegawai.getKeterangan() == null ? "" : ujianPegawai.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(6);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Informasi ke peserta ujian pada kartu Ujian"));
		row.appendChild(keteranganSetelahBayar = new Textbox(ujianPegawai.getKeteranganSetelahBayar()));
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
			MyMessageboxConfig.show("Nama Ujian Pegawai harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (lokasi.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Lokasi Ujian Pegawai harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (gelombangPendaftaranPegawai.getSelectedItem() == null) {
			MyMessageboxConfig.show("Gelombang Pendaftaran Ujian Pegawai harus diisi", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (ujianPegawai.getId() != null) {
			ujianPegawai = (UjianPegawai) session.load(UjianPegawai.class, ujianPegawai.getId());

		}

		ujianPegawai.setKeteranganSetelahBayar(keteranganSetelahBayar.getValue());
		ujianPegawai.setTampilkanJadwalUjianDiKartuUjian(tampilkanJadwalUjianDiKartuUjian.isChecked());
		ujianPegawai.setNama(nama.getValue());
		ujianPegawai.setLokasi(lokasi.getValue());
		ujianPegawai.setKeterangan(keterangan.getValue());

		ujianPegawai.setJumlahHariUjian(jumlahHariUjian.getValue());
		ujianPegawai.setTanggalUjian1(tanggalUjian1.getValue());
		ujianPegawai.setTanggalUjian2(tanggalUjian2.getValue());
		ujianPegawai.setTanggalUjian3(tanggalUjian3.getValue());
		ujianPegawai.setTanggalUjian4(tanggalUjian4.getValue());
		ujianPegawai.setTanggalUjian5(tanggalUjian5.getValue());
		ujianPegawai.setTanggalUjian6(tanggalUjian6.getValue());
		ujianPegawai.setTanggalUjian7(tanggalUjian7.getValue());
		ujianPegawai.setTanggalUjian8(tanggalUjian8.getValue());
		ujianPegawai.setTanggalUjian9(tanggalUjian9.getValue());
		ujianPegawai.setTanggalUjian10(tanggalUjian10.getValue());
		ujianPegawai.setGelombangPendaftaranPegawai(
				(GelombangPendaftaranPegawai) gelombangPendaftaranPegawai.getSelectedItem().getValue());

		Common.refreshSaveOrUpdate(session, ujianPegawai);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(UjianPegawai.class).createAlias("gelombangPendaftaranPegawai",
				"gelombangPendaftaranPegawai");

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchgelombangPendaftaranPegawai.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("gelombangPendaftaranPegawai",
								searchgelombangPendaftaranPegawai.getSelectedItem().getValue()));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<UjianPegawai> ujianPegawai = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(ujianPegawai);
		grid.setRowRenderer(new UjianPegawaiRenderer());
		grid.setModelCheckMobile(strset);

	}

}
