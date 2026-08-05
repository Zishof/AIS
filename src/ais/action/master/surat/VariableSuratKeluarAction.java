package ais.action.master.surat;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import ais.ui.util.MyCkEditor;
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
import ais.database.model.GeneralValueObject;
import ais.database.model.file.LampiranLain;
import ais.database.model.surat.VariableSuratKeluar;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class VariableSuratKeluarAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox nama;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private VariableSuratKeluar variableSuratKeluar;
	private MyToolbarbuttonConfig add;

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

		String[] contents = new String[] { "id", "nama", "key", "nilai", "tipe", "nomorUrut", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, VariableSuratKeluar.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class VariableSuratKeluarRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row row, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final VariableSuratKeluar variableSuratKeluar = (VariableSuratKeluar) arg1;
			final Combobox tipe = new Combobox();
			RevisiHelper.createNewRevisi(VariableSuratKeluar.class, variableSuratKeluar, variableSuratKeluar.getNama())
					.setParent(row);
			new Label(variableSuratKeluar.getKey()).setParent(row);

			final Intbox nomorUrut = new Intbox(variableSuratKeluar.getNomorUrut());
			nomorUrut.setWidth("90%");
			nomorUrut.setParent(row);
			nomorUrut.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					variableSuratKeluar.setNomorUrut(nomorUrut.getValue());
					row.setValign("top");row.setAttribute("variableSuratKeluar", variableSuratKeluar);
					if (variableSuratKeluar.getId() != null) {
						Session session = HibernateUtil.currentSession();
						session.update(variableSuratKeluar);
					}
				}
			});

			Hbox hbox = new Hbox();
			hbox.setParent(row);

			final Textbox nilai = new Textbox(variableSuratKeluar.getNilai());
			nilai.setWidth("90%");
			nilai.setParent(hbox);
			nilai.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					variableSuratKeluar.setNilai(nilai.getValue().trim());
					row.setValign("top");row.setAttribute("variableSuratKeluar", variableSuratKeluar);
					if (variableSuratKeluar.getId() != null) {
						Session session = HibernateUtil.currentSession();
						session.update(variableSuratKeluar);
					}
				}
			});

			final Hbox gambarHbox = new Hbox();
			gambarHbox.setParent(hbox);
			LampiranLain.createDownloadUploadFileLain(gambarHbox, variableSuratKeluar.getId(),
					VariableSuratKeluar.class.getName(), "Gambar", false, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							LampiranLain lainMahasiswa = (LampiranLain) arg0.getData();
							variableSuratKeluar.setLampiranId(lainMahasiswa.getId());
							variableSuratKeluar.setNilai(lainMahasiswa.ambilFile().getAbsolutePath());
							row.setValign("top");row.setAttribute("variableSuratKeluar", variableSuratKeluar);
							if (variableSuratKeluar.getId() != null) {
								Session session = HibernateUtil.currentSession();
								session.update(variableSuratKeluar);
							}
						}
					});
			gambarHbox.setParent(hbox);

			final MyButtonConfig tombol = new MyButtonConfig("Ubah Nilai");
			tombol.setWidth("90%");
			tombol.setParent(hbox);
			tombol.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					final MyWindow window = new MyWindow("Nilai", "none", true);
					window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					window.setHeight("440px");
					window.setWidth("850px");

					Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
					borderlayout.setParent(window);
					Center center = new Center();
					center.setParent(borderlayout);
					ais.ui.util.ZkCompat.setFlex(center, true);

					final MyCkEditor nilai = new MyCkEditor();
					nilai.setValue(variableSuratKeluar.getNilai());
					nilai.setParent(center);

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
							window.detach();
						}
					});
					cancel.setParent(toolbar);
					MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
					save.setTooltiptext("Simpan");
					save.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							if (nilai.getValue().trim().equals("")) {
								MyMessageboxConfig.show("Mohon maaf, Nilai Parameter belum diisi. Langkah yang dapat dilakukan: (1) klik kolom Nilai Parameter; (2) isikan nilai yang sesuai; (3) klik tombol Simpan kembali. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
										MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
								return;
							}

							variableSuratKeluar.setNilai(nilai.getValue().trim());
							row.setValign("top");row.setAttribute("variableSuratKeluar", variableSuratKeluar);
							if (variableSuratKeluar.getId() != null) {
								Session session = HibernateUtil.currentSession();
								session.update(variableSuratKeluar);
							}

							window.detach();
						}
					});
					save.setParent(toolbar);

					window.onModal();

				}
			});

			final MyButtonConfig tombolData = new MyButtonConfig("Ubah Nilai");
			tombolData.setWidth("90%");
			tombolData.setParent(hbox);
			tombolData.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					final MyWindow window = new MyWindow("Nilai Default", "none", true);
					window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					window.setHeight("440px");
					window.setWidth("90%");

					Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
					borderlayout.setParent(window);
					Center center = new Center();
					center.setParent(borderlayout);
					ais.ui.util.ZkCompat.setFlex(center, true);

					String nilai = variableSuratKeluar.getNilai();
					String[] rrr = new String[100];
					if (nilai != null && !nilai.trim().isEmpty()) {
						String[] s = StringUtils.split(nilai, "||");
						for (int i = 0; i < s.length; i++) {
							try {
								rrr[i] = s[i];
							} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
						}
					}

					MyGrid grid = new MyGrid();
					grid.setParent(center);
					final Rows rows = new Rows();
					rows.setParent(grid);
					for (int r = 0; r < 100; r++) {
						MyFormRow row = new MyFormRow();row.setValign("top");
						row.setParent(rows);
						String nil = rrr[r];
						String[] val = nil == null || nil.trim().isEmpty() ? new String[15]
								: StringUtils.split(nil, "<->");
						for (int col = 0; col < 15; col++) {
							String v = val.length > col ? val[col] : "";
							Textbox textbox = new Textbox(v);
							textbox.setWidth("90%");
							textbox.setParent(row);
						}
					}

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
							window.detach();
						}
					});
					cancel.setParent(toolbar);
					MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
					save.setTooltiptext("Simpan");
					save.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {

							String hasil = "";
							for (Object o : rows.getChildren()) {
								if (o instanceof Row) {
									Row row = (Row) o;
									String ss = "";
									for (Object c : row.getChildren()) {
										if (c instanceof Textbox) {
											Textbox d = (Textbox) c;
											ss += ss.isEmpty() ? d.getValue() : "<->" + d.getValue();
										}
									}
									hasil += hasil.isEmpty() ? ss : "||" + ss;
								}
							}

							variableSuratKeluar.setNilai(hasil);
							row.setValign("top");row.setAttribute("variableSuratKeluar", variableSuratKeluar);
							if (variableSuratKeluar.getId() != null) {
								Session session = HibernateUtil.currentSession();
								session.update(variableSuratKeluar);
							}

							window.detach();
						}
					});
					save.setParent(toolbar);

					window.onModal();

				}
			});

			tipe.setWidth("90%");
			tipe.setParent(row);
			MyComboitemConfig comboitem = new MyComboitemConfig(String.class.getSimpleName());
			comboitem.setValue(String.class.getName());
			tipe.appendChild(comboitem);

			comboitem = new MyComboitemConfig(Integer.class.getSimpleName());
			comboitem.setValue(Integer.class.getName());
			tipe.appendChild(comboitem);

			comboitem = new MyComboitemConfig(Double.class.getSimpleName());
			comboitem.setValue(Double.class.getName());
			tipe.appendChild(comboitem);

			comboitem = new MyComboitemConfig(Date.class.getSimpleName());
			comboitem.setValue(Date.class.getName());
			tipe.appendChild(comboitem);

			comboitem = new MyComboitemConfig(VariableSuratKeluar.GAMBAR);
			comboitem.setValue(VariableSuratKeluar.GAMBAR);
			tipe.appendChild(comboitem);

			comboitem = new MyComboitemConfig(VariableSuratKeluar.TEXT);
			comboitem.setValue(VariableSuratKeluar.TEXT);
			tipe.appendChild(comboitem);

			comboitem = new MyComboitemConfig(VariableSuratKeluar.DATA);
			comboitem.setValue(VariableSuratKeluar.DATA);
			tipe.appendChild(comboitem);
			tipe.setReadonly(true);

			Common.selectComboItem(tipe, variableSuratKeluar.getTipe());

			tipe.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					variableSuratKeluar.setTipe(
							(String) (tipe.getSelectedItem() == null ? null : tipe.getSelectedItem().getValue()));
					row.setValign("top");row.setAttribute("variableSuratKeluar", variableSuratKeluar);
					if (variableSuratKeluar.getId() != null) {
						Session session = HibernateUtil.currentSession();
						session.update(variableSuratKeluar);
					}

					nilai.setValue(variableSuratKeluar.getNilai());

					nilai.setVisible(!tipe.getValue().equalsIgnoreCase(VariableSuratKeluar.GAMBAR)
							&& !tipe.getValue().equalsIgnoreCase(VariableSuratKeluar.TEXT)
							&& !tipe.getValue().equalsIgnoreCase(VariableSuratKeluar.DATA));
					gambarHbox.setVisible(tipe.getValue().equalsIgnoreCase(VariableSuratKeluar.GAMBAR));
					tombol.setVisible(tipe.getValue().equalsIgnoreCase(VariableSuratKeluar.TEXT));
					tombolData.setVisible(tipe.getValue().equalsIgnoreCase(VariableSuratKeluar.DATA));
				}
			});

			nilai.setVisible(!tipe.getValue().equalsIgnoreCase(VariableSuratKeluar.GAMBAR)
					&& !tipe.getValue().equalsIgnoreCase(VariableSuratKeluar.TEXT)
					&& !tipe.getValue().equalsIgnoreCase(VariableSuratKeluar.DATA));
			gambarHbox.setVisible(tipe.getValue().equalsIgnoreCase(VariableSuratKeluar.GAMBAR));
			tombol.setVisible(tipe.getValue().equalsIgnoreCase(VariableSuratKeluar.TEXT));
			tombolData.setVisible(tipe.getValue().equalsIgnoreCase(VariableSuratKeluar.DATA));

			new Label(variableSuratKeluar.getKeterangan()).setParent(row);

			Common.copyEditDeleteButtons(edit, delete, variableSuratKeluar, VariableSuratKeluarAction.this)
					.setParent(row);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new VariableSuratKeluar());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		variableSuratKeluar = (VariableSuratKeluar) obj;
		init(variableSuratKeluar);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(VariableSuratKeluar variableSuratKeluar) {
		this.variableSuratKeluar = variableSuratKeluar;
		addWindow.setTitle(variableSuratKeluar.getId() == null ? "Tambah Variabel" : "Ubah Variabel");
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
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Variabel"));
		row.appendChild(nama = new Textbox(variableSuratKeluar.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(variableSuratKeluar.getKeterangan()));
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
			MyMessageboxConfig.show("Mohon maaf, Nama Variabel Surat Keluar belum diisi. Langkah yang dapat dilakukan: (1) klik kolom Nama Variabel; (2) isikan nama variabel secara lengkap; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		boolean i = checkNamaVariableSuratKeluar();
		if (i) {
			MyMessageboxConfig.show("Mohon maaf, Nama Variabel Surat Keluar sudah ada di database. Langkah yang dapat dilakukan: (1) periksa daftar variabel yang sudah terdaftar; (2) gunakan nama yang berbeda dan belum terdaftar; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (variableSuratKeluar.getId() != null) {
			variableSuratKeluar = (VariableSuratKeluar) session.load(VariableSuratKeluar.class,
					variableSuratKeluar.getId());

		}

		variableSuratKeluar.setNama(nama.getValue());
		variableSuratKeluar.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, variableSuratKeluar);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(VariableSuratKeluar.class);

		if (order)
			criteria.addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<VariableSuratKeluar> variableSuratKeluar = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(variableSuratKeluar);
		grid.setRowRenderer(new VariableSuratKeluarRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkNamaVariableSuratKeluar() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(VariableSuratKeluar.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.variableSuratKeluar.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.variableSuratKeluar.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
