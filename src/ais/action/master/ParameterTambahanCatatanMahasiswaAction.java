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
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
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
import ais.common.PesanFormalHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.KelompokParameterTambahanCatatanMahasiswa;
import ais.database.model.ParameterTambahan;
import ais.database.model.ParameterTambahanCatatanMahasiswa;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class ParameterTambahanCatatanMahasiswaAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;

	private Combobox searchkelompokParameterTambahanCatatanMahasiswa;

	private Combobox kelompokParameterTambahanCatatanMahasiswa;
	private Combobox parameterTambahan;

	private boolean edit = true;
	private boolean delete = true;

	private MyToolbarbuttonConfig find;
	private ParameterTambahanCatatanMahasiswa parameterTambahanCatatanMahasiswa;

	public void onResetParameter(Event event) {

		Common.insertCombo(searchkelompokParameterTambahanCatatanMahasiswa, "nama",
				KelompokParameterTambahanCatatanMahasiswa.class);
		if (!searchkelompokParameterTambahanCatatanMahasiswa.getChildren().isEmpty()) {
			searchkelompokParameterTambahanCatatanMahasiswa.setSelectedIndex(0);
		}
		if (!searchkelompokParameterTambahanCatatanMahasiswa.getChildren().isEmpty()) {
			searchkelompokParameterTambahanCatatanMahasiswa.setSelectedIndex(0);
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
			MyInclude iframe = new MyInclude("/pages/master/kelompok_parameter_tambahan_catatan_mahasiswa.zul");
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

		KelompokParameterTambahanCatatanMahasiswa.checkCreateDefault();

		final EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.insertCombo(searchkelompokParameterTambahanCatatanMahasiswa, "nama",
						KelompokParameterTambahanCatatanMahasiswa.class);
				if (!searchkelompokParameterTambahanCatatanMahasiswa.getChildren().isEmpty()) {
					searchkelompokParameterTambahanCatatanMahasiswa.setSelectedIndex(0);
				}
			}
		};

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "parameterTambahan", "kelompokParameterTambahanCatatanMahasiswa",
				"nomorUrut", "yayasan", "sekolah" };

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, find, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, ParameterTambahanCatatanMahasiswa.class, contents);
		Common.appendKeToolbar(upload, find, comp);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				eventListener.onEvent(arg0);

				onSearchDefault(null);

			}
		});
	}

	class ParameterTambahanCatatanMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final ParameterTambahanCatatanMahasiswa parameterTambahanCatatanMahasiswa = (ParameterTambahanCatatanMahasiswa) arg1;
			new Label(parameterTambahanCatatanMahasiswa.getKelompokParameterTambahanCatatanMahasiswa().getNama())
					.setParent(arg0);

			RevisiHelper.createNewRevisi(ParameterTambahanCatatanMahasiswa.class, parameterTambahanCatatanMahasiswa,
					parameterTambahanCatatanMahasiswa.getParameterTambahan().getLabelInputan()).setParent(arg0);
			new Label(parameterTambahanCatatanMahasiswa.getParameterTambahan().getHarusMenyertakanLampiran() ? "Ya"
					: "Tidak").setParent(arg0);

			new Label(parameterTambahanCatatanMahasiswa.getParameterTambahan().getTipeDataInputan()).setParent(arg0);
			new Label(parameterTambahanCatatanMahasiswa.getParameterTambahan().getNilaiDataInputan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(parameterTambahanCatatanMahasiswa);
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

											Common.refreshDelete(parameterTambahanCatatanMahasiswa);

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

	@SuppressWarnings("unchecked")
	public void onAdd(Event event) throws Exception {
		if (searchkelompokParameterTambahanCatatanMahasiswa.getSelectedItem() == null
				|| searchkelompokParameterTambahanCatatanMahasiswa.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Sebelum bisa menambah data, kelompok harus dipilih", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							searchkelompokParameterTambahanCatatanMahasiswa.focus();
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

						ParameterTambahanCatatanMahasiswa parameterTambahanCatatanMahasiswa = new ParameterTambahanCatatanMahasiswa();
						parameterTambahanCatatanMahasiswa.setParameterTambahan(parameterTambahan);
						parameterTambahanCatatanMahasiswa.setKelompokParameterTambahanCatatanMahasiswa(
								(KelompokParameterTambahanCatatanMahasiswa) (searchkelompokParameterTambahanCatatanMahasiswa
										.getSelectedItem() == null ? null
												: searchkelompokParameterTambahanCatatanMahasiswa.getSelectedItem()
														.getValue()));

						session.save(parameterTambahanCatatanMahasiswa);

					}

					onSearchDefault(arg0);

				}

			}
		});

		window.onModal();

	}

	private void init(ParameterTambahanCatatanMahasiswa parameterTambahanCatatanMahasiswa) {
		this.parameterTambahanCatatanMahasiswa = parameterTambahanCatatanMahasiswa;
		addWindow.setTitle(parameterTambahanCatatanMahasiswa.getId() == null ? "Tambah Parameter" : "Ubah Parameter");
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
		Common.insertCombo(kelompokParameterTambahanCatatanMahasiswa = new Combobox(), "nama", "keterangan",
				KelompokParameterTambahanCatatanMahasiswa.class);
		Common.selectComboItem(kelompokParameterTambahanCatatanMahasiswa,
				parameterTambahanCatatanMahasiswa.getKelompokParameterTambahanCatatanMahasiswa());
		row.appendChild(kelompokParameterTambahanCatatanMahasiswa);
		kelompokParameterTambahanCatatanMahasiswa.setWidth("90%");
		kelompokParameterTambahanCatatanMahasiswa.setReadonly(true);

		Common.insertCombo(parameterTambahan = new Combobox(),
				new String[] { "labelInputan", "tipeDataInputan", "nilaiDataInputan" }, ParameterTambahan.class);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Parameter"));
		row.appendChild(parameterTambahan);
		parameterTambahan.setWidth("90%");
		Common.selectComboItem(parameterTambahan, parameterTambahanCatatanMahasiswa.getParameterTambahan());

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

		if (kelompokParameterTambahanCatatanMahasiswa.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Kelompok Parameter",
					"Kolom Kelompok Parameter belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Kelompok Parameter.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (parameterTambahan.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Parameter",
					"Kolom Nama Parameter belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama Parameter.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (parameterTambahanCatatanMahasiswa.getId() != null) {
			parameterTambahanCatatanMahasiswa = (ParameterTambahanCatatanMahasiswa) session
					.load(ParameterTambahanCatatanMahasiswa.class, parameterTambahanCatatanMahasiswa.getId());

		}
		parameterTambahanCatatanMahasiswa.setKelompokParameterTambahanCatatanMahasiswa(
				(KelompokParameterTambahanCatatanMahasiswa) (kelompokParameterTambahanCatatanMahasiswa
						.getSelectedItem() == null ? null
								: kelompokParameterTambahanCatatanMahasiswa.getSelectedItem().getValue()));

		parameterTambahanCatatanMahasiswa
				.setParameterTambahan((ParameterTambahan) parameterTambahan.getSelectedItem().getValue());

		Common.refreshSaveOrUpdate(session, parameterTambahanCatatanMahasiswa);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(ParameterTambahanCatatanMahasiswa.class)
				.createAlias("parameterTambahan", "parameterTambahan");

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.add(searchkelompokParameterTambahanCatatanMahasiswa.getSelectedItem() == null
				|| searchkelompokParameterTambahanCatatanMahasiswa.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("kelompokParameterTambahanCatatanMahasiswa",
								searchkelompokParameterTambahanCatatanMahasiswa.getSelectedItem().getValue()))
				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("parameterTambahan.nama", searchnama.getValue().trim(),
								MatchMode.ANYWHERE))

		;

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<ParameterTambahanCatatanMahasiswa> parameterTambahanCatatanMahasiswa = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(parameterTambahanCatatanMahasiswa);
		grid.setRowRenderer(new ParameterTambahanCatatanMahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
