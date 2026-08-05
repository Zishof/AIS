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
import ais.ui.util.MyDetail;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
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
import ais.action.master.recruitment.helper.AktifitasJadwalUjianPegawaiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.recruitment.GelombangPendaftaranPegawai;
import ais.database.model.recruitment.JadwalUjianPegawai;
import ais.database.model.recruitment.UjianPegawai;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class JadwalUjianPegawaiAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Combobox searchujian;
	private Combobox searchgelombangPendaftaranPegawai;

	private Combobox ujianPegawai;
	private Combobox gelombangPendaftaranPegawai;

	private MyDatebox waktuMulai;
	private MyDatebox waktuSampai;

	private Textbox nama;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private JadwalUjianPegawai jadwalUjianPegawai;
	private MyToolbarbuttonConfig add;

	protected AktifitasJadwalUjianPegawaiHelper aktifitasJadwalUjianPegawaiHelper = new AktifitasJadwalUjianPegawaiHelper();

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

	class JadwalUjianPegawaiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final JadwalUjianPegawai jadwalUjianPegawai = (JadwalUjianPegawai) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);

			detail.addEventListener("onOpen", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (detail.getChildren().size() == 0) {
						ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
						groupbox.setStyle("min-height: 200px;");
						aktifitasJadwalUjianPegawaiHelper.initDetail(jadwalUjianPegawai, groupbox);
						detail.appendChild(groupbox);
					}
				}
			});

			RevisiHelper.createNewRevisi(JadwalUjianPegawai.class, jadwalUjianPegawai, jadwalUjianPegawai.getNama())
					.setParent(arg0);
			new Label(Common.dateFormat3.get().format(jadwalUjianPegawai.getWaktuMulai())).setParent(arg0);
			new Label(Common.dateFormat3.get().format(jadwalUjianPegawai.getWaktuSampai())).setParent(arg0);
			new Label(jadwalUjianPegawai.getUjianPegawai().toString()).setParent(arg0);
			new Label(jadwalUjianPegawai.getGelombangPendaftaranPegawai() == null ? "Semua"
					: jadwalUjianPegawai.getGelombangPendaftaranPegawai().getNama()).setParent(arg0);
			new Label(jadwalUjianPegawai.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(jadwalUjianPegawai);
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
											Common.refreshDelete(HibernateUtil.currentSession(), jadwalUjianPegawai);
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
			toolbar.setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new JadwalUjianPegawai());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(JadwalUjianPegawai jadwalUjianPegawai) {
		this.jadwalUjianPegawai = jadwalUjianPegawai;
		addWindow.setTitle(jadwalUjianPegawai.getId() == null ? "Tambah Jadwal Ujian Pegawai" : "Ubah Jadwal Ujian Pegawai");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Materi Ujian"));
		row.appendChild(nama = new Textbox(jadwalUjianPegawai.getNama() == null ? "" : jadwalUjianPegawai.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Waktu Mulai"));
		row.appendChild(waktuMulai = new MyDatebox(jadwalUjianPegawai.getWaktuMulai()));
		waktuMulai.setFormat(Common.dateFormat.get().toPattern());

		waktuMulai.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Waktu Sampai"));
		row.appendChild(waktuSampai = new MyDatebox(jadwalUjianPegawai.getWaktuSampai()));
		waktuSampai.setFormat(Common.dateFormat.get().toPattern());

		waktuSampai.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Ujian"));
		row.appendChild(ujianPegawai = new Combobox());
		Common.insertCombo(ujianPegawai, "nama", UjianPegawai.class);
		Common.selectComboItem(ujianPegawai, jadwalUjianPegawai.getUjianPegawai());
		ujianPegawai.setWidth("90%");
		ujianPegawai.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Gelombang Pendaftaran"));
		row.appendChild(gelombangPendaftaranPegawai = new Combobox());
		Common.insertCombo(gelombangPendaftaranPegawai, "nama", GelombangPendaftaranPegawai.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(gelombangPendaftaranPegawai, jadwalUjianPegawai.getGelombangPendaftaranPegawai());
		gelombangPendaftaranPegawai.setWidth("90%");

		if (searchgelombangPendaftaranPegawai.getSelectedItem() != null
				&& searchgelombangPendaftaranPegawai.getSelectedItem().getValue() != null) {
			Common.selectComboItem(true, gelombangPendaftaranPegawai,
					searchgelombangPendaftaranPegawai.getSelectedItem().getValue());
			gelombangPendaftaranPegawai.setDisabled(searchgelombangPendaftaranPegawai.isDisabled());

			Common.insertCombo(ujianPegawai, "nama", UjianPegawai.class, Restrictions.eq("gelombangPendaftaranPegawai",
					searchgelombangPendaftaranPegawai.getSelectedItem().getValue()));
			Common.selectComboItem(ujianPegawai, jadwalUjianPegawai.getUjianPegawai());
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				jadwalUjianPegawai.getKeterangan() == null ? "" : jadwalUjianPegawai.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

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
			MyMessageboxConfig.show("Materi ujian harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (waktuMulai.getValue() == null) {
			MyMessageboxConfig.show("Waktu mulai ujian harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (waktuSampai.getValue() == null) {
			MyMessageboxConfig.show("Waktu sampai ujian harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (ujianPegawai.getSelectedItem() == null) {
			MyMessageboxConfig.show("Data ujian harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (jadwalUjianPegawai.getId() != null) {
			jadwalUjianPegawai = (JadwalUjianPegawai) session.load(JadwalUjianPegawai.class,
					jadwalUjianPegawai.getId());

		}

		jadwalUjianPegawai.setWaktuMulai(waktuMulai.getValue());
		jadwalUjianPegawai.setWaktuSampai(waktuSampai.getValue());
		jadwalUjianPegawai.setUjianPegawai((UjianPegawai) ujianPegawai.getSelectedItem().getValue());
		jadwalUjianPegawai.setNama(nama.getValue());
		jadwalUjianPegawai.setKeterangan(keterangan.getValue());
		jadwalUjianPegawai.setGelombangPendaftaranPegawai(
				(GelombangPendaftaranPegawai) (gelombangPendaftaranPegawai.getSelectedItem() == null ? null
						: gelombangPendaftaranPegawai.getSelectedItem().getValue()));

		if (jadwalUjianPegawai.getId() != null) {
			session.update(jadwalUjianPegawai);
		} else {
			session.save(jadwalUjianPegawai);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(JadwalUjianPegawai.class).createAlias("ujianPegawai",
				"ujianPegawai");

		if (order)
			criteria.addOrder(Order.asc("waktuMulai"));
		criteria.add(searchujian.getSelectedItem() == null ? Restrictions.sqlRestriction("true")
				: Restrictions.eq("ujianPegawai", searchujian.getSelectedItem().getValue()))
				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchgelombangPendaftaranPegawai.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("gelombangPendaftaranPegawai",
								searchgelombangPendaftaranPegawai.getSelectedItem().getValue()));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<JadwalUjianPegawai> jadwalUjianPegawai = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(jadwalUjianPegawai);
		grid.setRowRenderer(new JadwalUjianPegawaiRenderer());
		grid.setModelCheckMobile(strset);

	}

}
