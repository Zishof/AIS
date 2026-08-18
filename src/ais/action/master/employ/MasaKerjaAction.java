package ais.action.master.employ;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.employ.helper.MasaKerjaDetailAction;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.ParameterTambahan;
import ais.database.model.employ.MasaKerja;
import ais.database.model.employ.SkorGolongan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class MasaKerjaAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private TreeSet<Long> skors = new TreeSet<Long>();

	private Textbox searchnama;
	private Checkbox searchaktif;

	private Textbox nama;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private MasaKerja masaKerja;
	private MyToolbarbuttonConfig add;
	protected JSONArray array;
	private MyDoublebox maksimal;

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

		String[] contents = new String[] { "id", "kode", "nama", "keterangan", "skor", "aktif", "maksimal", "feeder" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(MasaKerja.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, MasaKerja.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class MasaKerjaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final MasaKerja masaKerja = (MasaKerja) arg1;

			new MasaKerjaDetailAction(masaKerja).setParent(arg0);

			RevisiHelper.createNewRevisi(MasaKerja.class, masaKerja, masaKerja.getNama()).setParent(arg0);

			List<SkorGolongan> skorGolongans = new ArrayList<SkorGolongan>();
			Vbox hbox = new Vbox();
			hbox.setParent(arg0);
			try {
				String[] ss = masaKerja.getSkor().split(",");

				for (String s : ss) {
					if (!s.trim().isEmpty()) {
						SkorGolongan skorGolongan = (SkorGolongan) ConstantValues.ambil(SkorGolongan.class.getName(),
								Long.parseLong(s));
						if (skorGolongan != null) {
							skorGolongans.add(skorGolongan);

						}
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/employ/MasaKerjaAction.java:159");
				// TODO: handle exception
			}
			Collections.sort(skorGolongans);

			for (SkorGolongan skorGolongan : skorGolongans) {
				hbox.appendChild(new MyLabelAgakKecil(skorGolongan.getNama()));
			}

			new Label(masaKerja.getKeterangan()).setParent(arg0);
			new Label(Common.numberFormat.get().format(masaKerja.getMaksimal())).setParent(arg0);
			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(masaKerja.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					masaKerja.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(masaKerja);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, masaKerja, MasaKerjaAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new MasaKerja());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		masaKerja = (MasaKerja) obj;
		init(masaKerja);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(final MasaKerja masaKerja) throws Exception {
		this.masaKerja = masaKerja;
		addWindow.setTitle(masaKerja.getId() == null ? "Tambah Masa Kerja" : "Ubah Masa Kerja");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		array = new JSONArray(masaKerja.getFormula());

		MyGrid grid = new MyGrid();
		if (masaKerja.getId() != null) {
			Tabbox tabbox = new Tabbox();
			tabbox.setParent(center);
			Tabs tabs = new Tabs();
			tabs.setParent(tabbox);

			MyTabConfig tabData = new MyTabConfig("Masa Kerja");
			tabData.setParent(tabs);

			MyTabConfig tabDataVariabel = new MyTabConfig("Variabel");
			tabDataVariabel.setParent(tabs);

			Tabpanels tabpanels = new Tabpanels();
			tabpanels.setParent(tabbox);

			Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
			tabpanel.setParent(tabpanels);
			grid.setParent(Common.tampilanScroll(tabpanel));

			final Tabpanel tabpanelVariabel = new ais.ui.util.MyTabpanel();
			tabpanelVariabel.setParent(tabpanels);

			tabDataVariabel.addEventListener("onClick", new EventListener() {

				@SuppressWarnings("deprecation")
				@Override
				public void onEvent(Event arg0) throws Exception {
					if (tabpanelVariabel.getChildren().isEmpty()) {
						Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
						borderlayout.setParent(tabpanelVariabel);

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
						column.setWidth("90%");

						Rows rows = new Rows();
						rows.setParent(grid);

						MyFormRow row = new MyFormRow();row.setValign("top");
						ais.ui.util.ZkCompat.setSpans(row, "2");
						row.setParent(rows);
						row.appendChild(new ais.ui.util.MyLabelConfig("Variable"));

						row = new MyFormRow();
						ais.ui.util.ZkCompat.setSpans(row, "2");
						row.setParent(rows);
						Row rowFormula = Common.tampilanScroll1(row);

						reloadFormula(rowFormula, masaKerja, array, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								masaKerja.setFormula(array.toString());
							}
						});
					}
				}
			});

		} else {
			grid.setParent(center);
		}

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
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Masa Kerja *"));
		row.appendChild(nama = new Textbox(masaKerja.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Maksimal Tahun *"));
		row.appendChild(maksimal = new MyDoublebox(masaKerja.getMaksimal()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Formula"));
		row.appendChild(keterangan = new Textbox(masaKerja.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		try {
			try {
				String[] ss = masaKerja.getSkor().split(",");

				skors.clear();
				for (String s : ss) {
					if (!s.trim().isEmpty()) {
						skors.add(Long.parseLong(s));
					}
				}
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

			List<SkorGolongan> skorGolongans = new ArrayList<SkorGolongan>();
			for (Object o : ConstantValues.ambilBerdasarClass(SkorGolongan.class).values()) {
				SkorGolongan skorGolongan = (SkorGolongan) o;
				if (skorGolongan.getAktif()) {
					skorGolongans.add(skorGolongan);
				}
			}
			Collections.sort(skorGolongans);

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Skor"));

			Vbox hbox = new Vbox();
			hbox.setParent(row);

			for (SkorGolongan skorGolongan : skorGolongans) {
				final Long id = skorGolongan.getId();
				Checkbox c;
				hbox.appendChild(c = new Checkbox(skorGolongan.getNama()));
				c.setChecked(skors.contains(skorGolongan.getId()));
				c.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (((Checkbox) arg0.getTarget()).isChecked()) {
							skors.add(id);
						} else {
							skors.remove(id);
						}
					}
				});
			}

		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
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

	public static void reloadDataFormula(final Row rowU, final MasaKerja u, final JSONArray array,
			final EventListener eventListenerUbah) throws Exception {
		Common.clear(rowU);

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(rowU);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig("Variabel");
		column.setParent(columns);
		column.setWidth("15%");

		List<SkorGolongan> skorGolongans = new ArrayList<SkorGolongan>();

		if (u == null || u.getSkor().isEmpty()) {
			for (Object o : ConstantValues.ambilBerdasarClass(SkorGolongan.class).values()) {
				SkorGolongan skorGolongan = (SkorGolongan) o;
				if (skorGolongan.getAktif()) {
					skorGolongans.add(skorGolongan);
				}
			}
		} else {
			String[] ss = u.getSkor().split(",");
			for (String s : ss) {
				if (!s.trim().isEmpty()) {
					SkorGolongan skorGolongan = (SkorGolongan) ConstantValues.ambil(SkorGolongan.class.getName(),
							Long.parseLong(s));
					if (skorGolongan != null) {
						skorGolongans.add(skorGolongan);
					}
				}
			}
		}
		Collections.sort(skorGolongans);

		int lebar = skorGolongans.size() == 0 ? 60 : 60 / skorGolongans.size();

		for (SkorGolongan skorGolongan : skorGolongans) {
			column = new MyColumnConfig(skorGolongan.getKode());
			column.setTooltiptext(skorGolongan.getNama());
			column.setParent(columns);
			column.setWidth(lebar + "%");
		}

		column = new MyColumnConfig("Nilai");
		column.setAlign("right");
		column.setParent(columns);
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		Rows rows = new Rows();
		rows.setParent(grid);

		for (int i = 0; i < array.length(); i++) {
			final int index = i;
			final JSONObject jsonObject = array.getJSONObject(i);

			if (!jsonObject.isNull("variabel")) {

				String variabel = jsonObject.get("variabel") + "";

				MyFormRow row = new MyFormRow();row.setValign("top");
				row.setParent(rows);

				final MyDoublebox nilai = new MyDoublebox(
						!jsonObject.isNull("nilai") ? jsonObject.getDouble("nilai") : 0.0);

				final MyTextbox variabelText = new MyTextbox(variabel);

				variabelText.setWidth("90%");
				row.appendChild(variabelText);

				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						if (arg0 != null && arg0.getTarget() instanceof MyDoublebox
								&& arg0.getTarget().getAttribute("skorGolongan") != null) {
							SkorGolongan skorGolongan = (SkorGolongan) arg0.getTarget().getAttribute("skorGolongan");
							MyDoublebox doublebox = (MyDoublebox) arg0.getTarget();
							jsonObject.put(skorGolongan.getKode(),
									doublebox.getValue() == null ? "" : doublebox.getValue());
						}

						String variabel = variabelText.getValue() == null ? "" : variabelText.getValue();
						jsonObject.put("variabel", variabel);
						jsonObject.put("nilai", nilai.getValue() == null ? "0.0" : nilai.getValue());

						eventListenerUbah.onEvent(arg0);
					}
				};

				for (final SkorGolongan skorGolongan : skorGolongans) {

					String val = "";
					if (!jsonObject.isNull(skorGolongan.getKode())) {
						val = jsonObject.get(skorGolongan.getKode()).toString();
					}

					if (skorGolongan.getParameterTambahan() != null) {
						Component component = ParameterTambahan.ambilComponent(val, skorGolongan.getParameterTambahan(),
								new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										String val = ParameterTambahan.ambilValComponent(arg0.getTarget(),
												skorGolongan.getParameterTambahan());
										
										try {
											val = val.split(":")[1];
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/employ/MasaKerjaAction.java:531");
											// TODO: handle exception
										}
										
										jsonObject.put(skorGolongan.getKode(), val);

										String variabel = variabelText.getValue() == null ? ""
												: variabelText.getValue();
										jsonObject.put("variabel", variabel);
										jsonObject.put("nilai", nilai.getValue() == null ? "0.0" : nilai.getValue());
										eventListenerUbah.onEvent(arg0);
									}
								});
						component.setAttribute("skorGolongan", skorGolongan);
						row.appendChild(component);
					} else {
						Double point = 0.0;
						if (!jsonObject.isNull(skorGolongan.getKode())) {
							point = jsonObject.getDouble(skorGolongan.getKode());
						}
						MyDoublebox doublebox = new MyDoublebox(point);
						doublebox.setAttribute("skorGolongan", skorGolongan);
						doublebox.setWidth("85%");
						row.appendChild(doublebox);
						doublebox.addEventListener("onChange", eventListener);
					}
				}

				variabelText.addEventListener("onChange", eventListener);
				nilai.addEventListener("onChange", eventListener);

				nilai.setWidth("90%");
				nilai.setParent(row);

				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
				button.setTooltiptext("Hapus Data");
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
												array.put(index, new JSONObject());

												reloadDataFormula(rowU, u, array, eventListenerUbah);

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
				button.setParent(row);
			}
		}
	}

	public static void reloadFormula(final Row rowFormula, final MasaKerja u, final JSONArray array,
			final EventListener eventListenerUbah) throws Exception {
		final MyFormRow rowU = new MyFormRow();

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Variabel", "/img/svg/addthis.svg");
		button.setTooltiptext("Hapus Data");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("tgl", Common.dateFormat1.get().format(new Date()));
				jsonObject.put("variabel", "");
				array.put(jsonObject);

				reloadDataFormula(rowU, u, array, eventListenerUbah);
			}
		});
		button.setParent(rowFormula);

		rowU.setParent(rowFormula.getParent());

		reloadDataFormula(rowU, u, array, eventListenerUbah);

	}

	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Masa Kerja belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Nama Masa Kerja pada form; (2) pastikan nama tidak kosong atau hanya spasi; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (masaKerja.getId() != null) {
			masaKerja = (MasaKerja) session.load(MasaKerja.class, masaKerja.getId());

		}
		masaKerja.setNama(nama.getValue());
		masaKerja.setKeterangan(keterangan.getValue());

		String s = "";
		for (Long sk : skors) {
			s += s.isEmpty() ? sk.toString() : "," + sk.toString();
		}
		masaKerja.setSkor(s);
		masaKerja.setFormula(array.toString());
		masaKerja.setMaksimal(maksimal.getValue());
		Common.refreshSaveOrUpdate(session, masaKerja);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(MasaKerja.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<MasaKerja> masaKerja = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(masaKerja);
		grid.setRowRenderer(new MasaKerjaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
