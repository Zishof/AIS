package ais.action.master;

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
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import ais.ui.util.MyInclude;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonSearchFilterHelper;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.GrupChecklistPenilaianDosenDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.AngketPenilaianDosen;
import ais.database.model.GrupChecklistPenilaianDosen;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

public class GrupChecklistPenilaianDosenAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchkodeangket;
	private Textbox searchnamaangket;
	private Combobox searchjurusan;
	private Combobox searchprogram;
	private Combobox searchfakultas;
	private MyCheckboxConfig searchhanyaAktif;

	private Textbox isi;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private GrupChecklistPenilaianDosen grupChecklistPenilaianDosen;
	private MyToolbarbuttonConfig add;

	private Tabpanel grupAngketUmum;
	private Tabpanel grupKuosioner;
	private Tabpanel jadwalAngketUmum;
	private AngketPenilaianDosen angket;
	private Combobox angketPenilaianDosen;

	public void onGrupAngketUmum(Event event) {
		if (grupAngketUmum == null) {
			return;
		}
		if (grupAngketUmum.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(grupAngketUmum);
			window.setContentStyle("overflow:auto;");
			MyInclude iframe = new MyInclude("/pages/master/grup_checklist_penilaian_umum.zul");
			iframe.setParent(window);
		}
	}

	public void onGrupKuosioner(Event event) {
		if (grupKuosioner == null) {
			return;
		}
		if (grupKuosioner.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(grupKuosioner);
			window.setContentStyle("overflow:auto;");
			MyInclude iframe = new MyInclude("/pages/master/grup_kuesioner_umum.zul");
			iframe.setParent(window);
		}
	}

	public void onJadwalAngketUmum(Event event) {
		if (jadwalAngketUmum == null) {
			return;
		}
		if (jadwalAngketUmum.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(jadwalAngketUmum);
			window.setContentStyle("overflow:auto;");
			MyInclude iframe = new MyInclude("/pages/master/jadwal_checklist_penilaian_umum.zul");
			iframe.setParent(window);
		}
	}

	public static String[] contents = new String[] { "id", "angketPenilaianDosen", "isi", "aktif", "keterangan" };

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		Session session = HibernateUtil.currentSession();
		int count = ((Number) session.createCriteria(AngketPenilaianDosen.class).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();
		if (count == 0) {
			angket = new AngketPenilaianDosen();
			angket.setKode("001.000");
			angket.setIsi("EVALUASI PENILAIAN PEMBELAJARAN");
			Common.refreshSaveOrUpdate(session, angket);
		} else {
			angket = (AngketPenilaianDosen) session.createCriteria(AngketPenilaianDosen.class).setMaxResults(1)
					.uniqueResult();
		}

		Common.initPrograms(searchprogram);

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

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

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(GrupChecklistPenilaianDosen.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, GrupChecklistPenilaianDosen.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	        FilterLanjutHelper.setup(comp);
}

	class GrupChecklistPenilaianDosenRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
				final GrupChecklistPenilaianDosen grupChecklistPenilaianDosen = (GrupChecklistPenilaianDosen) arg1;
			if (grupChecklistPenilaianDosen.getAngketPenilaianDosen() == null && angket != null) {
				grupChecklistPenilaianDosen.setAngketPenilaianDosen(angket);
			}
			new Label(grupChecklistPenilaianDosen.getAngketPenilaianDosen() == null ? ""
					: grupChecklistPenilaianDosen.getAngketPenilaianDosen().getIsi()).setParent(arg0);
			RevisiHelper.createNewRevisi(GrupChecklistPenilaianDosen.class, grupChecklistPenilaianDosen,
					grupChecklistPenilaianDosen.getIsi()).setParent(arg0);

			AngketPenilaianDosen angketPenilaianDosen = grupChecklistPenilaianDosen.getAngketPenilaianDosen();
			new Label(angketPenilaianDosen == null || angketPenilaianDosen.getFakultas() == null ? "Semua"
					: angketPenilaianDosen.getFakultas().getNama()).setParent(arg0);
			new Label(angketPenilaianDosen == null || angketPenilaianDosen.getJurusan() == null ? "Semua"
					: angketPenilaianDosen.getJurusan().getNama()).setParent(arg0);

			new Label(angketPenilaianDosen == null || angketPenilaianDosen.getProgram() == null
					|| angketPenilaianDosen.getProgram().trim().isEmpty() ? "Semua" : angketPenilaianDosen.getProgram())
					.setParent(arg0);

			new Label(grupChecklistPenilaianDosen.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(grupChecklistPenilaianDosen.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					grupChecklistPenilaianDosen.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(grupChecklistPenilaianDosen);
				}
			});

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(grupChecklistPenilaianDosen);
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

											Common.refreshDelete(grupChecklistPenilaianDosen);

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
		init(new GrupChecklistPenilaianDosen());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(GrupChecklistPenilaianDosen grupChecklistPenilaianDosen) {
		this.grupChecklistPenilaianDosen = grupChecklistPenilaianDosen;
		addWindow.setTitle(grupChecklistPenilaianDosen.getId() == null ? "Tambah Grup Penilaian Dosen" : "Ubah Grup Penilaian Dosen");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Angket"));
		row.appendChild(angketPenilaianDosen = new Combobox());
		angketPenilaianDosen.setReadonly(true);
		Common.insertCombo(angketPenilaianDosen, new String[] { "kode", "isi", "fakultas", "jurusan", "program" },
				AngketPenilaianDosen.class);
		Common.selectComboItem(angketPenilaianDosen, grupChecklistPenilaianDosen.getAngketPenilaianDosen());
		angketPenilaianDosen.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Grup"));
		row.appendChild(isi = new Textbox(
				grupChecklistPenilaianDosen.getIsi() == null ? "" : grupChecklistPenilaianDosen.getIsi()));
		isi.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(grupChecklistPenilaianDosen.getKeterangan() == null ? ""
				: grupChecklistPenilaianDosen.getKeterangan()));
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
		if (angketPenilaianDosen.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Angket",
					"Kolom Nama Angket belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama Angket.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (isi.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Grup",
					"Kolom Nama Grup belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama Grup.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		GrupChecklistPenilaianDosenDao grupChecklistPenilaianDosenDao = DaoFactory.getInstance()
				.getChecklistPenilaianDosenDao();
		if (grupChecklistPenilaianDosen.getId() != null) {
			grupChecklistPenilaianDosen = grupChecklistPenilaianDosenDao.load(grupChecklistPenilaianDosen.getId());

		}
		grupChecklistPenilaianDosen
				.setAngketPenilaianDosen((AngketPenilaianDosen) angketPenilaianDosen.getSelectedItem().getValue());
		grupChecklistPenilaianDosen.setIsi(isi.getValue());
		grupChecklistPenilaianDosen.setKeterangan(keterangan.getValue());

		if (grupChecklistPenilaianDosen.getId() != null) {
			grupChecklistPenilaianDosenDao.update(grupChecklistPenilaianDosen);
		} else {
			grupChecklistPenilaianDosenDao.save(grupChecklistPenilaianDosen);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(GrupChecklistPenilaianDosen.class)
				.add(searchhanyaAktif.isChecked() ? Restrictions.eq("aktif", true)
						: Restrictions.sqlRestriction("true"))
				.createAlias("angketPenilaianDosen", "angketPenilaianDosen", Criteria.LEFT_JOIN);

		if (order)
			criteria.addOrder(Order.asc("isi"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("isi", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchkodeangket.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("angketPenilaianDosen.kode", searchkodeangket.getValue().trim(),
								MatchMode.ANYWHERE))
				.add(searchnamaangket.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("angketPenilaianDosen.isi", searchnamaangket.getValue().trim(),
								MatchMode.ANYWHERE));

		criteria.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
				? Restrictions.sqlRestriction("1=1")
				: Restrictions.or(Restrictions.isNull("angketPenilaianDosen.jurusan"),
						CommonSearchFilterHelper.eqSelectedWithId("angketPenilaianDosen.jurusan", searchjurusan, false)))

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("angketPenilaianDosen.fakultas"),
								CommonSearchFilterHelper.eqSelectedWithId("angketPenilaianDosen.fakultas", searchfakultas, false)))

				.add(searchprogram.getSelectedItem() == null || searchprogram.getSelectedItem().getValue() == null
						|| searchprogram.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("angketPenilaianDosen.program"), Restrictions.eq(
										"angketPenilaianDosen.program", searchprogram.getSelectedItem().getValue())));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<GrupChecklistPenilaianDosen> grupChecklistPenilaianDosen = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(grupChecklistPenilaianDosen);
		grid.setRowRenderer(new GrupChecklistPenilaianDosenRenderer());
		grid.setModelCheckMobile(strset);

	}

}
