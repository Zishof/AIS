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
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
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
import ais.action.master.helper.generic.AmbilDataParameterTambahanBanyak;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.ParameterTambahan;
import ais.database.model.sekolah.GelombangPendaftaranPsb;
import ais.database.model.sekolah.KelompokParameterTambahanCalonSiswa;
import ais.database.model.sekolah.ParameterTambahanGelombangPendaftaranPsb;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class ParameterTambahanGelombangPendaftaranPsbAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Combobox searchgelombangPendaftaranPsb;
	private Combobox searchkelompokParameterTambahanCalonSiswa;

	private Combobox kelompokParameterTambahanCalonSiswa;
	private Combobox gelombangPendaftaranPsb;
	private Combobox parameterTambahan;

	private boolean edit = true;
	private boolean delete = true;

	private MyToolbarbuttonConfig find;
	private ParameterTambahanGelombangPendaftaranPsb parameterTambahanGelombangPendaftaranPsb;

	private GelombangPendaftaranPsb selectedGelombangPendaftaranPsb;
	private Label labelsearchgelombangPendaftaranPsb;

	public void onResetParameter(Event event) {
		Common.insertCombo(searchkelompokParameterTambahanCalonSiswa, "nama",
				KelompokParameterTambahanCalonSiswa.class);
		if (!searchkelompokParameterTambahanCalonSiswa.getChildren().isEmpty()) {
			searchkelompokParameterTambahanCalonSiswa.setSelectedIndex(0);
		}
		onSearchDefault(null);
	}

	private Tabpanel manajemenKelompok;

	public void onManajemenKelompok(Event event) {
		if (manajemenKelompok.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenKelompok);
			MyInclude iframe = new MyInclude("/pages/psb/kelompok_parameter_tambahan_calon_siswa.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel manajemenParameter;

	public void onManajemenParameter(Event event) {
		if (manajemenParameter.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenParameter);
			MyInclude iframe = new MyInclude("/pages/master/parameter_tambahan.zul");
			iframe.setParent(window);
		}
	}

	// private MyToolbarbuttonConfig add;

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

		KelompokParameterTambahanCalonSiswa kelompokParameterTambahanCalonSiswa = KelompokParameterTambahanCalonSiswa
				.checkCreateDefault();
		HibernateUtil.currentSession().createSQLQuery(
				"update sekolah.parameter_tambahan_gelombang_pendaftaran_psb set kelompok_parameter_tambahan_calon_siswa="
						+ kelompokParameterTambahanCalonSiswa.getId()
						+ " where kelompok_parameter_tambahan_calon_siswa is null;")
				.executeUpdate();

		Common.insertCombo(gelombangPendaftaranPsb = new Combobox(), "nama", "keterangan",
				GelombangPendaftaranPsb.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		Common.insertCombo(searchgelombangPendaftaranPsb, "nama", "keterangan", GelombangPendaftaranPsb.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		if (execution.getParameter("gelombangPendaftaranPsb") != null) {
			selectedGelombangPendaftaranPsb = (GelombangPendaftaranPsb) HibernateUtil.currentSession()
					.createCriteria(GelombangPendaftaranPsb.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("gelombangPendaftaranPsb"))))
					.uniqueResult();
			Common.selectComboItem(searchgelombangPendaftaranPsb, selectedGelombangPendaftaranPsb);
			searchgelombangPendaftaranPsb.setDisabled(true);
		} else {
			labelsearchgelombangPendaftaranPsb.setVisible(false);
			searchgelombangPendaftaranPsb.setVisible(false);
		}

		Common.insertCombo(searchkelompokParameterTambahanCalonSiswa, "nama",
				KelompokParameterTambahanCalonSiswa.class);
		if (!searchkelompokParameterTambahanCalonSiswa.getChildren().isEmpty()) {
			searchkelompokParameterTambahanCalonSiswa.setSelectedIndex(0);
		}

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "parameterTambahan", "tampilDiSemuaGelombang", "gelombangs",
				"kelompokParameterTambahanCalonSiswa", "gelombangPendaftaranPsb", "tampilDiFromSebelumLogin",
				"tampilDiFromSetelahLogin" };

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, find, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, ParameterTambahanGelombangPendaftaranPsb.class,
				contents);
		Common.appendKeToolbar(upload, find, comp);
	}

	class ParameterTambahanGelombangPendaftaranPsbRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final ParameterTambahanGelombangPendaftaranPsb parameterTambahanGelombangPendaftaranPsb = (ParameterTambahanGelombangPendaftaranPsb) arg1;
			new Label(parameterTambahanGelombangPendaftaranPsb.getKelompokParameterTambahanCalonSiswa().getNama())
					.setParent(arg0);

			RevisiHelper
					.createNewRevisi(ParameterTambahanGelombangPendaftaranPsb.class,
							parameterTambahanGelombangPendaftaranPsb,
							parameterTambahanGelombangPendaftaranPsb.getParameterTambahan().getLabelInputan())
					.setParent(arg0);
			new Label(
					parameterTambahanGelombangPendaftaranPsb.getParameterTambahan().getHarusMenyertakanLampiran() ? "Ya"
							: "Tidak")
					.setParent(arg0);

			new Label(parameterTambahanGelombangPendaftaranPsb.getParameterTambahan().getTipeDataInputan())
					.setParent(arg0);
			new Label(parameterTambahanGelombangPendaftaranPsb.getParameterTambahan().getNilaiDataInputan())
					.setParent(arg0);

			final MyCheckboxConfig tampilDiFromSebelumLogin = new MyCheckboxConfig("Tampil Sebelum Login");
			tampilDiFromSebelumLogin.setDisabled(!edit);
			tampilDiFromSebelumLogin.setChecked(parameterTambahanGelombangPendaftaranPsb.getTampilDiFromSebelumLogin());
			tampilDiFromSebelumLogin.setParent(arg0);
			tampilDiFromSebelumLogin.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					parameterTambahanGelombangPendaftaranPsb
							.setTampilDiFromSebelumLogin(tampilDiFromSebelumLogin.isChecked());
					Common.refreshSaveOrUpdate(parameterTambahanGelombangPendaftaranPsb);
				}
			});

			final MyCheckboxConfig tampilDiFromSetelahLogin = new MyCheckboxConfig("Tampil Setelah Login");
			tampilDiFromSetelahLogin.setDisabled(!edit);
			tampilDiFromSetelahLogin.setChecked(parameterTambahanGelombangPendaftaranPsb.getTampilDiFromSetelahLogin());
			tampilDiFromSetelahLogin.setParent(arg0);
			tampilDiFromSetelahLogin.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					parameterTambahanGelombangPendaftaranPsb
							.setTampilDiFromSetelahLogin(tampilDiFromSetelahLogin.isChecked());
					Common.refreshSaveOrUpdate(parameterTambahanGelombangPendaftaranPsb);
				}
			});

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(parameterTambahanGelombangPendaftaranPsb);
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

											Common.refreshDelete(parameterTambahanGelombangPendaftaranPsb);

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

	@SuppressWarnings("unchecked")
	public void onAdd(Event event) throws Exception {
		if (searchkelompokParameterTambahanCalonSiswa.getSelectedItem() == null
				|| searchkelompokParameterTambahanCalonSiswa.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Sebelum bisa menambah data, kelompok harus dipilih", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							searchkelompokParameterTambahanCalonSiswa.focus();
						}
					});
			return;
		}

		List<ParameterTambahan> parameterTambahans = initCriteria(false)
				.setProjection(Projections.groupProperty("parameterTambahan")).list();

		AmbilDataParameterTambahanBanyak window = new AmbilDataParameterTambahanBanyak(parameterTambahans);

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
		window.setWidth("90%");
		window.setHeight("90%");

		window.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				List<ParameterTambahan> parameterTambahans = (List<ParameterTambahan>) arg0.getData();

				if (parameterTambahans != null) {
					Session session = HibernateUtil.currentSession();
					for (ParameterTambahan parameterTambahan : parameterTambahans) {

						ParameterTambahanGelombangPendaftaranPsb parameterTambahanGelombangPendaftaranPsb = new ParameterTambahanGelombangPendaftaranPsb();
						parameterTambahanGelombangPendaftaranPsb.setParameterTambahan(parameterTambahan);
						parameterTambahanGelombangPendaftaranPsb
								.setGelombangPendaftaranPsb(selectedGelombangPendaftaranPsb);
						parameterTambahanGelombangPendaftaranPsb.setKelompokParameterTambahanCalonSiswa(
								(KelompokParameterTambahanCalonSiswa) (searchkelompokParameterTambahanCalonSiswa
										.getSelectedItem() == null ? null
												: searchkelompokParameterTambahanCalonSiswa.getSelectedItem()
														.getValue()));

						session.save(parameterTambahanGelombangPendaftaranPsb);

					}

					onSearchDefault(arg0);

				}

			}
		});

		window.onModal();

	}

	private void init(ParameterTambahanGelombangPendaftaranPsb parameterTambahanGelombangPendaftaranPsb) {
		this.parameterTambahanGelombangPendaftaranPsb = parameterTambahanGelombangPendaftaranPsb;
		addWindow.setTitle(parameterTambahanGelombangPendaftaranPsb.getId() == null ? "Tambah Parameter" : "Ubah Parameter");
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
		column.setWidth("40%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelompok"));
		Common.insertCombo(kelompokParameterTambahanCalonSiswa = new Combobox(), "nama", "keterangan",
				KelompokParameterTambahanCalonSiswa.class);
		Common.selectComboItem(kelompokParameterTambahanCalonSiswa,
				parameterTambahanGelombangPendaftaranPsb.getKelompokParameterTambahanCalonSiswa());
		row.appendChild(kelompokParameterTambahanCalonSiswa);
		kelompokParameterTambahanCalonSiswa.setWidth("90%");
		kelompokParameterTambahanCalonSiswa.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("GelombangPendaftaranPsb"));
		Common.selectComboItem(gelombangPendaftaranPsb,
				parameterTambahanGelombangPendaftaranPsb.getGelombangPendaftaranPsb() == null ? null
						: parameterTambahanGelombangPendaftaranPsb.getGelombangPendaftaranPsb());
		row.appendChild(gelombangPendaftaranPsb);
		gelombangPendaftaranPsb.setWidth("90%");

		if (selectedGelombangPendaftaranPsb != null) {
			Common.selectComboItem(gelombangPendaftaranPsb, selectedGelombangPendaftaranPsb);
			gelombangPendaftaranPsb.setDisabled(true);
		}

		Common.insertCombo(parameterTambahan = new Combobox(),
				new String[] { "labelInputan", "tipeDataInputan", "nilaiDataInputan" }, ParameterTambahan.class);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Parameter"));
		row.appendChild(parameterTambahan);
		parameterTambahan.setWidth("90%");
		Common.selectComboItem(parameterTambahan, parameterTambahanGelombangPendaftaranPsb.getParameterTambahan());

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

		if (kelompokParameterTambahanCalonSiswa.getSelectedItem() == null) {
			MyMessageboxConfig.show("Kelompok Parameter harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (parameterTambahan.getSelectedItem() == null) {
			MyMessageboxConfig.show("Nama Parameter harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (parameterTambahanGelombangPendaftaranPsb.getId() != null) {
			parameterTambahanGelombangPendaftaranPsb = (ParameterTambahanGelombangPendaftaranPsb) session.load(
					ParameterTambahanGelombangPendaftaranPsb.class, parameterTambahanGelombangPendaftaranPsb.getId());

		}
		parameterTambahanGelombangPendaftaranPsb.setKelompokParameterTambahanCalonSiswa(
				(KelompokParameterTambahanCalonSiswa) (kelompokParameterTambahanCalonSiswa.getSelectedItem() == null
						? null
						: kelompokParameterTambahanCalonSiswa.getSelectedItem().getValue()));

		parameterTambahanGelombangPendaftaranPsb.setGelombangPendaftaranPsb(
				(GelombangPendaftaranPsb) (gelombangPendaftaranPsb.getSelectedItem() == null ? null
						: gelombangPendaftaranPsb.getSelectedItem().getValue()));
		parameterTambahanGelombangPendaftaranPsb
				.setParameterTambahan((ParameterTambahan) parameterTambahan.getSelectedItem().getValue());

		Common.refreshSaveOrUpdate(session, parameterTambahanGelombangPendaftaranPsb);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(ParameterTambahanGelombangPendaftaranPsb.class)
				.createAlias("parameterTambahan", "parameterTambahan");

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.add(searchkelompokParameterTambahanCalonSiswa.getSelectedItem() == null
				|| searchkelompokParameterTambahanCalonSiswa.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("kelompokParameterTambahanCalonSiswa",
								searchkelompokParameterTambahanCalonSiswa.getSelectedItem().getValue()))
				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("parameterTambahan.nama", searchnama.getValue().trim(),
								MatchMode.ANYWHERE))
				.add(searchgelombangPendaftaranPsb.getSelectedItem() == null
						? Restrictions.isNull("gelombangPendaftaranPsb")
						: Restrictions.eq("gelombangPendaftaranPsb",
								searchgelombangPendaftaranPsb.getSelectedItem().getValue()));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<ParameterTambahanGelombangPendaftaranPsb> parameterTambahanGelombangPendaftaranPsb = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(parameterTambahanGelombangPendaftaranPsb);
		grid.setRowRenderer(new ParameterTambahanGelombangPendaftaranPsbRenderer());
		grid.setModelCheckMobile(strset);

	}

}
