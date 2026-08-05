package ais.action.master.sekolah;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
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
import ais.action.master.sekolah.helper.AktifitasJadwalPertemuanPSBHelper;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.GelombangPendaftaranPsb;
import ais.database.model.sekolah.JadwalPertemuanPSB;
import ais.database.model.sekolah.Sekolah;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

public class JadwalPertemuanPSBAction extends GenericAutowireComposer implements DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Combobox searchgelombangPendaftaranPsb;

	private Combobox gelombangPendaftaranPsb;
	private Checkbox sembunyikanTanggalTerlewat;
	private Checkbox searchaktif;

	private MyDatebox waktuMulai;
	private MyDatebox waktuSampai;

	private Textbox nama;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private JadwalPertemuanPSB jadwalPertemuanPSB;
	private MyToolbarbuttonConfig add;

	protected AktifitasJadwalPertemuanPSBHelper aktifitasJadwalPertemuanPSBHelper = new AktifitasJadwalPertemuanPSBHelper();
	private MyIntbox kuota;
	private MyCheckboxConfig bolehDipilihSendiriOlehCalonSiswa;

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

		String[] contents = new String[] { "noRegistrasi", "nama", "jadwalPertemuanPSB.keterangan",
				"jadwalPertemuanPSB.kuota", "jadwalPertemuanPSB.aktif",
				"jadwalPertemuanPSB.bolehDipilihSendiriOlehCalonSiswa", "jadwalPertemuanPSB.gelombangPendaftaranPsb",
				"jadwalPertemuanPSB.waktuMulai", "jadwalPertemuanPSB.waktuSampai" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(CalonSiswa.class, new DataCriteria() {

			@SuppressWarnings("unchecked")
			@Override
			public Object initCriteria(boolean order) {
				List<Long> jadwalPertemuanPSB = JadwalPertemuanPSBAction.this.initCriteria(true)
						.setProjection(Projections.property("id")).list();

				return HibernateUtil.currentSession().createCriteria(CalonSiswa.class)
						.add(jadwalPertemuanPSB.isEmpty() ? Restrictions.sqlRestriction("false")
								: Restrictions.in("jadwalPertemuanPSB.id", jadwalPertemuanPSB));
			}
		}, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

	}

	class JadwalPertemuanPSBRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final JadwalPertemuanPSB jadwalPertemuanPSB = (JadwalPertemuanPSB) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);

			detail.addEventListener("onOpen", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (detail.getChildren().size() == 0) {

						MyWindow window = new MyWindow("", "none", false);
						window.setHeight("100%");
						window.setWidth("100%");
						window.setStyle("min-height: 3000px;");
						window.setParent(detail);
						MyInclude iframe = new MyInclude("/pages/master/sekolah/calon_siswa.zul?jadwalPertemuanPSB="
								+ jadwalPertemuanPSB.getId());
						iframe.setParent(window);

					}
				}
			});

			RevisiHelper.createNewRevisi(JadwalPertemuanPSB.class, jadwalPertemuanPSB, jadwalPertemuanPSB.getNama())
					.setParent(arg0);
			new Label(Common.dateFormat3.get().format(jadwalPertemuanPSB.getWaktuMulai())).setParent(arg0);
			new Label(Common.dateFormat3.get().format(jadwalPertemuanPSB.getWaktuSampai())).setParent(arg0);

			new Label(Common.numberFormat.get().format(jadwalPertemuanPSB.getKuota())).setParent(arg0);

			new Label(jadwalPertemuanPSB.getBolehDipilihSendiriOlehCalonSiswa() ? "Ya" : "Tidak").setParent(arg0);

			new Label(jadwalPertemuanPSB.getGelombangPendaftaranPsb() == null ? "Semua"
					: jadwalPertemuanPSB.getGelombangPendaftaranPsb().getNama()).setParent(arg0);
			new Label(jadwalPertemuanPSB.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(jadwalPertemuanPSB.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jadwalPertemuanPSB.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(jadwalPertemuanPSB);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, jadwalPertemuanPSB, JadwalPertemuanPSBAction.this)
					.setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new JadwalPertemuanPSB());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		jadwalPertemuanPSB = (JadwalPertemuanPSB) obj;
		init(jadwalPertemuanPSB);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(JadwalPertemuanPSB jadwalPertemuanPSB) {
		this.jadwalPertemuanPSB = jadwalPertemuanPSB;
		addWindow.setTitle(jadwalPertemuanPSB.getId() == null ? "Tambah Jadwal Pertemuan" : "Ubah Jadwal Pertemuan");
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

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Materi Pertemuan *"));
		row.appendChild(nama = new Textbox(jadwalPertemuanPSB.getNama() == null ? "" : jadwalPertemuanPSB.getNama()));
		nama.setWidth("90%");
		nama.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Waktu Mulai *"));
		row.appendChild(waktuMulai = new MyDatebox(jadwalPertemuanPSB.getWaktuMulai()));
		waktuMulai.setFormat(Common.dateFormat.get().toPattern());
		waktuMulai.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Waktu Sampai *"));
		row.appendChild(waktuSampai = new MyDatebox(jadwalPertemuanPSB.getWaktuSampai()));
		waktuSampai.setFormat(Common.dateFormat.get().toPattern());
		waktuSampai.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kuota"));
		row.appendChild(kuota = new MyIntbox(jadwalPertemuanPSB.getKuota()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(
				bolehDipilihSendiriOlehCalonSiswa = new MyCheckboxConfig("Boleh dipilih sendiri oleh calon siswa"));
		bolehDipilihSendiriOlehCalonSiswa.setChecked(jadwalPertemuanPSB.getBolehDipilihSendiriOlehCalonSiswa());

		Sekolah sekolah = SekolahUtil.getSekolah();

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Gelombang Pendaftaran"));
		row.appendChild(gelombangPendaftaranPsb = new Combobox());
		Common.insertCombo(gelombangPendaftaranPsb, "nama", GelombangPendaftaranPsb.class,
				Restrictions.and(
						sekolah == null ? Restrictions.eq("sekolah", sekolah) : Restrictions.sqlRestriction("true"),
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
		Common.selectComboItem(gelombangPendaftaranPsb, jadwalPertemuanPSB.getGelombangPendaftaranPsb());
		gelombangPendaftaranPsb.setWidth("90%");

		if (searchgelombangPendaftaranPsb.getSelectedItem() != null
				&& searchgelombangPendaftaranPsb.getSelectedItem().getValue() != null) {
			Common.selectComboItem(true, gelombangPendaftaranPsb,
					searchgelombangPendaftaranPsb.getSelectedItem().getValue());
			gelombangPendaftaranPsb.setDisabled(searchgelombangPendaftaranPsb.isDisabled());

		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				jadwalPertemuanPSB.getKeterangan() == null ? "" : jadwalPertemuanPSB.getKeterangan()));
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
			MyMessageboxConfig.show("Materi pertemuan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (waktuMulai.getValue() == null) {
			MyMessageboxConfig.show("Waktu mulai pertemuan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (waktuSampai.getValue() == null) {
			MyMessageboxConfig.show("Waktu sampai pertemuan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (jadwalPertemuanPSB.getId() != null) {
			jadwalPertemuanPSB = (JadwalPertemuanPSB) session.load(JadwalPertemuanPSB.class,
					jadwalPertemuanPSB.getId());

		}

		jadwalPertemuanPSB.setWaktuMulai(waktuMulai.getValue());
		jadwalPertemuanPSB.setWaktuSampai(waktuSampai.getValue());
		jadwalPertemuanPSB.setNama(nama.getValue());
		jadwalPertemuanPSB.setKeterangan(keterangan.getValue());
		jadwalPertemuanPSB.setGelombangPendaftaranPsb(
				(GelombangPendaftaranPsb) (gelombangPendaftaranPsb.getSelectedItem() == null ? null
						: gelombangPendaftaranPsb.getSelectedItem().getValue()));

		jadwalPertemuanPSB.setKuota(kuota.getValue());
		jadwalPertemuanPSB.setBolehDipilihSendiriOlehCalonSiswa(bolehDipilihSendiriOlehCalonSiswa.isChecked());

		if (jadwalPertemuanPSB.getId() != null) {
			session.update(jadwalPertemuanPSB);
		} else {
			session.save(jadwalPertemuanPSB);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(JadwalPertemuanPSB.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))

				.add(sembunyikanTanggalTerlewat.isChecked() ? Restrictions.gt("waktuSampai", WaktuUtil.getDate())
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.asc("waktuMulai"));
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

		List<JadwalPertemuanPSB> jadwalPertemuanPSB = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(jadwalPertemuanPSB);
		grid.setRowRenderer(new JadwalPertemuanPSBRenderer());
		grid.setModelCheckMobile(strset);

	}

}
