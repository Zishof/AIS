package ais.action.master;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.maintenance.MainAction;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.ParameterUmum;
import ais.database.model.StatusPertemuan;
import ais.database.model.Tbmuser;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class ParameterUmumAction extends GenericAutowireComposer {

	/**
	 *
	 */
	protected static final long serialVersionUID = -5779730267402400328L;

	protected Tabs tabsKonfigurasi;
	protected Tabpanels tabpanelsKonfigurasi;

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
		
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getUserId() != null) {
			Integer desktopHeight = MainAction.desktopHeights.get(tbmuser.getUserId());
			if (desktopHeight != null) {
				tabsKonfigurasi.setHeight((desktopHeight * 0.9) + "px");
			} 
		} 

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

	}

	public void doAfterComposeOri(Component comp) throws Exception {
		super.doAfterCompose(comp);
	}

	public void onSearchDefault(Event event) {

	}

	@SuppressWarnings("deprecation")
	protected Rows createSpan(String title) {

		System.out.println("title => " + title + ", tabsParameterUmum " + tabsKonfigurasi);

		if (tabsKonfigurasi == null) {
			return new Rows();
		}

		final MyTabConfig tab = new MyTabConfig(title);
		tab.setParent(tabsKonfigurasi);
		Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanelsKonfigurasi);

		boolean aktif = Common.getParameterUmum("aktifkan_parameterUmum_" + title.replaceAll(" ", "_").toLowerCase(),
				ParameterUmum.AKTIF).getNilai().trim().equalsIgnoreCase(ParameterUmum.AKTIF);

		tab.setVisible(aktif);
		tabpanel.setVisible(aktif);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setHeight("100%");
		borderlayout.setWidth("100%");
		borderlayout.setParent(tabpanel);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid grid = new MyGrid();
		grid.setParent(center);

		Rows rows = new Rows();
		rows.setParent(grid);

		Row row = new Row();row.setValign("top");
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new ais.ui.util.MyHtml("<hr><div>"
				+ Common.getBahasaConfig(title) + "</div><hr>"));

		if (tabsKonfigurasi.getChildren().size() == 1) {
			tab.setSelected(true);
		}

		return rows;
	}

	@SuppressWarnings("deprecation")
	protected void createSpan(String title, Rows rows) {
		Row row = new Row();row.setValign("top");
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new ais.ui.util.MyHtml("<hr><div>"
				+ Common.getBahasaConfig(title) + "</div><hr>"));
	}

	protected Row createRowActiveDefault(final String label, final String key, final String defaultValue) {
		return createRowActiveDefault(label, key, defaultValue, createComboActive());
	}

	protected Row createRowActiveDefault(final String label, final String key, final String defaultValue,
			final Combobox nilaiInputNilai) {
		ParameterUmum parameterUmum = Common.getParameterUmum(key, defaultValue);
		Row row = new Row();row.setValign("top");
		try {
			RevisiHelper.createNewRevisi(ParameterUmum.class, parameterUmum, Common.getBahasaConfig(label))
					.setParent(row);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		nilaiInputNilai.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				ParameterUmum parameterUmum = Common.getParameterUmum(key, defaultValue);
				parameterUmum.setNilai((String) nilaiInputNilai.getSelectedItem().getValue());
				Session session = HibernateUtil.currentNativeSession();
				session.getTransaction().begin();
				session.update(parameterUmum);
				session.getTransaction().commit();

				HibernateUtil.closeSession();
				
			}
		});

		row.appendChild(nilaiInputNilai);

		Common.selectComboItem(nilaiInputNilai, parameterUmum.getNilai());
		return row;
	}

	protected Row createRowActive(final String label, final String key) {
		return createRowActiveDefault(label, key, ParameterUmum.AKTIF);
	}

	protected Row createRowActive(final String label, final String key, final Combobox nilaiInputNilai) {
		return createRowActiveDefault(label, key, ParameterUmum.AKTIF, nilaiInputNilai);
	}

	protected Row createRowActive(final String label, final String key, final StatusPertemuan statusPertemuan) {
		Row row = new Row();row.setValign("top");
		ParameterUmum parameterUmum = Common.getParameterUmum(key,
				statusPertemuan.getAktif() ? ParameterUmum.AKTIF : ParameterUmum.TIDAK_AKTIF);
		try {
			RevisiHelper.createNewRevisi(ParameterUmum.class, parameterUmum, Common.getBahasaConfig(label))
					.setParent(row);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		final Combobox nilaiInputNilai = createComboActive();
		nilaiInputNilai.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				ParameterUmum parameterUmum = Common.getParameterUmum(key,
						statusPertemuan.getAktif() ? ParameterUmum.AKTIF : ParameterUmum.TIDAK_AKTIF);
				parameterUmum.setNilai((String) nilaiInputNilai.getSelectedItem().getValue());

				Session session = HibernateUtil.currentNativeSession();

				session.refresh(statusPertemuan);
				statusPertemuan.setAktif(parameterUmum.getNilai().equals(ParameterUmum.AKTIF));

				session.getTransaction().begin();
				session.update(statusPertemuan);
				session.update(parameterUmum);
				session.getTransaction().commit();

				HibernateUtil.closeSession();
				// Common.parameterUmums.put(parameterUmum.getNama(),
				// parameterUmum);

				if (statusPertemuan.getId().equals(ConstantValues.ABSEN)) {
					ConstantValues.ABSEN = statusPertemuan;
				} else if (statusPertemuan.getId().equals(ConstantValues.FORM)) {
					ConstantValues.FORM = statusPertemuan;
				} else if (statusPertemuan.getId().equals(ConstantValues.UTS)) {
					ConstantValues.UTS = statusPertemuan;
				} else if (statusPertemuan.getId().equals(ConstantValues.UAS)) {
					ConstantValues.UAS = statusPertemuan;
				}
			}
		});

		row.appendChild(nilaiInputNilai);

		Common.selectComboItem(nilaiInputNilai, parameterUmum.getNilai());
		return row;
	}

	protected Row createRowActive(final String label, final String key, final String info1) {
		Row row = new Row();row.setValign("top");
		ParameterUmum parameterUmum = Common.getParameterUmum(key, ParameterUmum.AKTIF, info1, "", "");
		try {
			RevisiHelper.createNewRevisi(ParameterUmum.class, parameterUmum, Common.getBahasaConfig(label))
					.setParent(row);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		final Combobox nilaiInputNilai = createComboActive();
		final Textbox info1Textbox = new Textbox();

		EventListener eventListener = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				ParameterUmum parameterUmum = Common.getParameterUmum(key, ParameterUmum.AKTIF);
				parameterUmum.setNilai((String) nilaiInputNilai.getSelectedItem().getValue());
				parameterUmum.setInfo1(info1Textbox.getValue().trim());
				Session session = HibernateUtil.currentNativeSession();
				session.getTransaction().begin();
				session.update(parameterUmum);
				session.getTransaction().commit();

				HibernateUtil.closeSession();
				// Common.parameterUmums.put(parameterUmum.getNama(),
				// parameterUmum);
			}
		};

		nilaiInputNilai.addEventListener("onChange", eventListener);
		info1Textbox.addEventListener("onChange", eventListener);

		Common.selectComboItem(nilaiInputNilai, parameterUmum.getNilai());
		info1Textbox.setValue(parameterUmum.getInfo1());

		Hbox hbox = new Hbox();
		hbox.appendChild(nilaiInputNilai);
		hbox.appendChild(info1Textbox);
		row.appendChild(hbox);

		return row;
	}

	protected Row createRowActiveWithDefault(final String label, final String key, final String info1,
			final String withDefault) {
		return createRowActiveWithDefault(label, key, info1, withDefault, createComboActive());
	}

	protected Row createRowActiveWithDefault(final String label, final String key, final String info1,
			final String withDefault, final Combobox nilaiInputNilai) {
		Row row = new Row();row.setValign("top");
		ParameterUmum parameterUmum = Common.getParameterUmum(key, withDefault, info1, "", "");
		try {
			RevisiHelper.createNewRevisi(ParameterUmum.class, parameterUmum, Common.getBahasaConfig(label))
					.setParent(row);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		final Textbox info1Textbox = new Textbox();

		EventListener eventListener = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				ParameterUmum parameterUmum = Common.getParameterUmum(key, withDefault);
				parameterUmum.setNilai((String) nilaiInputNilai.getSelectedItem().getValue());
				parameterUmum.setInfo1(info1Textbox.getValue().trim());
				Session session = HibernateUtil.currentNativeSession();
				session.getTransaction().begin();
				session.update(parameterUmum);
				session.getTransaction().commit();

				HibernateUtil.closeSession();
				// Common.parameterUmums.put(parameterUmum.getNama(),
				// parameterUmum);
			}
		};

		nilaiInputNilai.addEventListener("onChange", eventListener);
		info1Textbox.addEventListener("onChange", eventListener);

		Common.selectComboItem(nilaiInputNilai, parameterUmum.getNilai());
		info1Textbox.setValue(parameterUmum.getInfo1());

		Hbox hbox = new Hbox();
		hbox.appendChild(nilaiInputNilai);
		if (info1 != null && !info1.trim().isEmpty()) {
			hbox.appendChild(info1Textbox);
		}
		row.appendChild(hbox);

		return row;
	}

	protected Row createRowTanggal(final String label, final String key, final String withDefault) {
		Row row = new Row();row.setValign("top");
		ParameterUmum parameterUmum = Common.getParameterUmum(key, withDefault, "", "", "");
		try {
			RevisiHelper.createNewRevisi(ParameterUmum.class, parameterUmum, Common.getBahasaConfig(label))
					.setParent(row);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		final MyDatebox nilaiInputNilai = new MyDatebox();
		nilaiInputNilai.setFormat(Common.dateFormat1.get().toPattern());
		if (!parameterUmum.getNilai().trim().isEmpty()) {
			try {
				nilaiInputNilai.setValue(Common.dateFormat1.get().parse(parameterUmum.getNilai().trim()));
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		}

		EventListener eventListener = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				ParameterUmum parameterUmum = Common.getParameterUmum(key, withDefault);
				parameterUmum.setNilai(nilaiInputNilai.getValue() == null ? ""
						: Common.dateFormat1.get().format(nilaiInputNilai.getValue()));
				Session session = HibernateUtil.currentNativeSession();
				session.getTransaction().begin();
				session.update(parameterUmum);
				session.getTransaction().commit();

				HibernateUtil.closeSession();
				// Common.parameterUmums.put(parameterUmum.getNama(),
				// parameterUmum);
			}
		};

		nilaiInputNilai.addEventListener("onChange", eventListener);
		row.appendChild(nilaiInputNilai);

		return row;
	}

	protected Row createRowActiveWithDefaultCombo(final String label, final String key, final String info1,
			final String withDefault, final Combobox nilaiInputNilai,
			final List<GeneralValueObject> generalValueObjects) {
		Row row = new Row();row.setValign("top");
		ParameterUmum parameterUmum = Common.getParameterUmum(key, withDefault, info1, "", "");
		try {
			RevisiHelper.createNewRevisi(ParameterUmum.class, parameterUmum, Common.getBahasaConfig(label))
					.setParent(row);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		final Combobox info1Textbox = new Combobox();
		info1Textbox.setReadonly(true);
		for (GeneralValueObject generalValueObject : generalValueObjects) {
			MyComboitemConfig comboitem = new MyComboitemConfig(generalValueObject.getNama());
			comboitem.setDescription(generalValueObject.getKeterangan());
			comboitem.setValue(generalValueObject.getId().toString());
			info1Textbox.appendChild(comboitem);
		}

		EventListener eventListener = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				ParameterUmum parameterUmum = Common.getParameterUmum(key, withDefault);
				parameterUmum.setNilai((String) nilaiInputNilai.getSelectedItem().getValue());
				parameterUmum.setInfo1(info1Textbox.getSelectedItem() == null ? ""
						: info1Textbox.getSelectedItem().getValue().toString());
				Session session = HibernateUtil.currentNativeSession();
				session.getTransaction().begin();
				session.update(parameterUmum);
				session.getTransaction().commit();

				HibernateUtil.closeSession();
				// Common.parameterUmums.put(parameterUmum.getNama(),
				// parameterUmum);
				try {
					info1Textbox.setDisabled(
							nilaiInputNilai.getSelectedItem().getValue().equals(ParameterUmum.TIDAK_AKTIF));
				} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
			}
		};

		nilaiInputNilai.addEventListener("onChange", eventListener);
		info1Textbox.addEventListener("onChange", eventListener);

		Common.selectComboItem(nilaiInputNilai, parameterUmum.getNilai());
		Common.selectComboItem(info1Textbox, parameterUmum.getInfo1().trim());

		Hbox hbox = new Hbox();
		hbox.appendChild(nilaiInputNilai);
		hbox.appendChild(info1Textbox);
		row.appendChild(hbox);

		try {
			info1Textbox.setDisabled(nilaiInputNilai.getSelectedItem().getValue().equals(ParameterUmum.TIDAK_AKTIF));
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		hbox.appendChild(createButtonLihat(key, true));
		return row;
	}

	protected Row createRowActiveWithTwoCombo(final String label, final String key, final String info1,
			final String withDefault, final Combobox info1Textbox, final Combobox nilaiInputNilai,
			final EventListener listener) {
		Row row = new Row();row.setValign("top");
		ParameterUmum parameterUmum = Common.getParameterUmum(key + info1, withDefault, info1, "", "");

		try {
			RevisiHelper.createNewRevisi(ParameterUmum.class, parameterUmum, Common.getBahasaConfig(label))
					.setParent(row);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		EventListener eventListener = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				String info1 = info1Textbox.getSelectedItem() == null ? ""
						: info1Textbox.getSelectedItem().getValue().toString();
				ParameterUmum parameterUmum = Common.getParameterUmum(key + info1, withDefault, info1, "", "");
				if (event.getTarget() == nilaiInputNilai) {
					parameterUmum.setNilai((String) nilaiInputNilai.getSelectedItem().getValue());
				} else if (event.getTarget() == info1Textbox) {
					parameterUmum.setInfo1(info1Textbox.getSelectedItem() == null ? ""
							: info1Textbox.getSelectedItem().getValue().toString());
					Common.selectComboItem(nilaiInputNilai, parameterUmum.getNilai());
				}
				Session session = HibernateUtil.currentNativeSession();
				session.getTransaction().begin();
				session.update(parameterUmum);
				session.getTransaction().commit();

				HibernateUtil.closeSession();
				// Common.parameterUmums.put(parameterUmum.getNama(),
				// parameterUmum);

				listener.onEvent(event);
			}
		};

		nilaiInputNilai.addEventListener("onChange", eventListener);
		info1Textbox.addEventListener("onChange", eventListener);

		Common.selectComboItem(nilaiInputNilai, parameterUmum.getNilai());
		Common.selectComboItem(info1Textbox, parameterUmum.getInfo1().trim());

		Hbox hbox = new Hbox();
		hbox.appendChild(info1Textbox);
		hbox.appendChild(nilaiInputNilai);
		hbox.appendChild(createButtonLihat(key, true));
		row.appendChild(hbox);

		return row;
	}

	protected Row createRowActiveWithTreeCombo(final String label, final String key, final String info1,
			final String info2, final String withDefault, final Combobox info1Textbox, final Combobox info2Textbox,
			final Combobox nilaiInputNilai, final EventListener listener) {
		Row row = new Row();row.setValign("top");
		ParameterUmum parameterUmum = Common.getParameterUmum(key + info1 + info2, withDefault, info1, info2, "");

		try {
			RevisiHelper.createNewRevisi(ParameterUmum.class, parameterUmum, Common.getBahasaConfig(label))
					.setParent(row);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		EventListener eventListener = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				String info1 = info1Textbox.getSelectedItem() == null
						|| info1Textbox.getSelectedItem().getValue() == null ? ""
								: info1Textbox.getSelectedItem().getValue().toString();
				String info2 = info2Textbox.getSelectedItem() == null
						|| info2Textbox.getSelectedItem().getValue() == null ? ""
								: info2Textbox.getSelectedItem().getValue().toString();
				ParameterUmum parameterUmum = Common.getParameterUmum(key + info1 + info2, withDefault, info1, info2,
						"");
				if (event.getTarget() == nilaiInputNilai) {
					parameterUmum.setNilai((String) nilaiInputNilai.getSelectedItem().getValue());
				} else if (event.getTarget() == info1Textbox) {
					parameterUmum.setInfo1(
							info1Textbox.getSelectedItem() == null || info1Textbox.getSelectedItem().getValue() == null
									? "" : info1Textbox.getSelectedItem().getValue().toString());
					Common.selectComboItem(nilaiInputNilai, parameterUmum.getNilai());
				} else if (event.getTarget() == info2Textbox) {
					parameterUmum.setInfo2(
							info2Textbox.getSelectedItem() == null || info2Textbox.getSelectedItem().getValue() == null
									? "" : info2Textbox.getSelectedItem().getValue().toString());
					Common.selectComboItem(nilaiInputNilai, parameterUmum.getNilai());
				}
				Session session = HibernateUtil.currentNativeSession();
				session.getTransaction().begin();
				session.update(parameterUmum);
				session.getTransaction().commit();

				HibernateUtil.closeSession();
				// Common.parameterUmums.put(parameterUmum.getNama(),
				// parameterUmum);

				if (listener != null) {
					listener.onEvent(event);
				}
			}
		};

		nilaiInputNilai.addEventListener("onChange", eventListener);
		info1Textbox.addEventListener("onChange", eventListener);
		info2Textbox.addEventListener("onChange", eventListener);

		Common.selectComboItem(nilaiInputNilai, parameterUmum.getNilai());
		Common.selectComboItem(info1Textbox, parameterUmum.getInfo1().trim());
		Common.selectComboItem(info2Textbox, parameterUmum.getInfo2().trim());

		Hbox hbox = new Hbox();
		hbox.appendChild(info1Textbox);
		hbox.appendChild(info2Textbox);
		hbox.appendChild(nilaiInputNilai);
		hbox.appendChild(createButtonLihat(key, true));
		row.appendChild(hbox);

		return row;
	}

	protected Row createRowNilai(final String label, final String key, final String nilai) {
		return createRowNilai(label, key, nilai, 1, null, null);
	}

	protected Row createRowNilai(final String label, final String key, final String nilai,
			EventListener paramEventListener) {
		return createRowNilai(label, key, nilai, 1, null, paramEventListener);
	}

	protected Row createRowNilaiPassword(final String label, final String key, final String nilai) {
		return createRowNilaiPassword(label, key, nilai, 1, null);
	}

	protected Row createRowNilai(final String label, final String key, final String nilai, int rowCount,
			final EventListener paramEventListener) {
		return createRowNilai(label, key, nilai, rowCount, null, paramEventListener);
	}

	protected Row createRowNilai(final String label, final String key, final String nilai, int rowCount,
			final Combobox pilihan, final EventListener paramEventListener) {
		return createRowNilai(label, key, nilai, rowCount, null, null, paramEventListener);
	}

	protected Row createRowNilai(final String label, final String key, final String nilai, int rowCount,
			final Combobox pilihan, final Combobox pilihan1, final EventListener paramEventListener) {
		return createRowNilai(label, key, nilai, rowCount, pilihan1, pilihan1, null, null, paramEventListener);
	}

	protected Row createRowNilai(final String label, final String key, final String nilai, int rowCount,
			final Combobox pilihan, final Combobox pilihan1, final Combobox pilihan2, final Combobox pilihan3,
			final EventListener paramEventListener) {
		Row row = new Row();row.setValign("top");

		String newKey = key;
		if (pilihan != null && pilihan.getSelectedItem() != null && pilihan.getSelectedItem().getValue() != null) {
			GeneralValueObject generalValueObject = (GeneralValueObject) pilihan.getSelectedItem().getValue();
			newKey += "_" + generalValueObject.getId();
		}
		if (pilihan1 != null && pilihan1.getSelectedItem() != null && pilihan1.getSelectedItem().getValue() != null) {
			GeneralValueObject generalValueObject = (GeneralValueObject) pilihan1.getSelectedItem().getValue();
			newKey += "_" + generalValueObject.getId();
		}
		if (pilihan2 != null && pilihan2.getSelectedItem() != null && pilihan2.getSelectedItem().getValue() != null) {
			GeneralValueObject generalValueObject = (GeneralValueObject) pilihan2.getSelectedItem().getValue();
			newKey += "_" + generalValueObject.getId();
		}
		if (pilihan3 != null && pilihan3.getSelectedItem() != null && pilihan3.getSelectedItem().getValue() != null) {
			GeneralValueObject generalValueObject = (GeneralValueObject) pilihan3.getSelectedItem().getValue();
			newKey += "_" + generalValueObject.getId();
		}

		ParameterUmum parameterUmum = Common.getParameterUmum(newKey, nilai);

		try {
			RevisiHelper.createNewRevisi(ParameterUmum.class, parameterUmum, Common.getBahasaConfig(label))
					.setParent(row);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		final Textbox info1Textbox = new Textbox();
		info1Textbox.setWidth("90%");
		EventListener eventListener = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				String newKey = key;
				if (pilihan != null && pilihan.getSelectedItem() != null
						&& pilihan.getSelectedItem().getValue() != null) {
					GeneralValueObject generalValueObject = (GeneralValueObject) pilihan.getSelectedItem().getValue();
					newKey += "_" + generalValueObject.getId();
				}
				if (pilihan1 != null && pilihan1.getSelectedItem() != null
						&& pilihan1.getSelectedItem().getValue() != null) {
					GeneralValueObject generalValueObject = (GeneralValueObject) pilihan1.getSelectedItem().getValue();
					newKey += "_" + generalValueObject.getId();
				}
				if (pilihan2 != null && pilihan2.getSelectedItem() != null
						&& pilihan2.getSelectedItem().getValue() != null) {
					GeneralValueObject generalValueObject = (GeneralValueObject) pilihan2.getSelectedItem().getValue();
					newKey += "_" + generalValueObject.getId();
				}
				if (pilihan3 != null && pilihan3.getSelectedItem() != null
						&& pilihan3.getSelectedItem().getValue() != null) {
					GeneralValueObject generalValueObject = (GeneralValueObject) pilihan3.getSelectedItem().getValue();
					newKey += "_" + generalValueObject.getId();
				}
				ParameterUmum parameterUmum = Common.getParameterUmum(newKey, nilai);

				if ((pilihan != null && event.getTarget() == pilihan)
						|| (pilihan1 != null && event.getTarget() == pilihan1)
						|| (pilihan2 != null && event.getTarget() == pilihan2)
						|| (pilihan3 != null && event.getTarget() == pilihan3)) {
					info1Textbox.setValue(parameterUmum.getNilai());
				} else {
					parameterUmum.setNilai(info1Textbox.getValue().trim());
					Session session = HibernateUtil.currentNativeSession();
					session.getTransaction().begin();
					session.update(parameterUmum);
					session.getTransaction().commit();

					HibernateUtil.closeSession();
					// Common.parameterUmums.put(parameterUmum.getNama(),
					// parameterUmum);

					try {
						if (paramEventListener != null) {
							paramEventListener.onEvent(new Event("", info1Textbox, parameterUmum));
						}
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}
			}
		};

		info1Textbox.addEventListener("onChange", eventListener);

		info1Textbox.setValue(parameterUmum.getNilai());
		info1Textbox.setRows(rowCount);

		if (pilihan == null && pilihan1 == null && pilihan2 == null && pilihan3 == null) {
			row.appendChild(info1Textbox);
		} else {
			Hbox hbox = new Hbox();
			hbox.setParent(row);
			if (pilihan != null) {
				hbox.appendChild(pilihan);
				pilihan.addEventListener("onChange", eventListener);
			}
			if (pilihan1 != null) {
				hbox.appendChild(pilihan1);
				pilihan1.addEventListener("onChange", eventListener);
			}
			if (pilihan2 != null) {
				hbox.appendChild(pilihan2);
				pilihan2.addEventListener("onChange", eventListener);
			}
			if (pilihan3 != null) {
				hbox.appendChild(pilihan3);
				pilihan3.addEventListener("onChange", eventListener);
			}
			hbox.appendChild(info1Textbox);

			hbox.appendChild(createButtonLihat(key, true));
		}

		return row;
	}

	protected Row createRowNilaiSemesterDanAngkatanDanJurusan(final String label, final String key, final String nilai,
			int rowCount, final EventListener paramEventListener) {

		final Combobox semester = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig("Semua Semester");
		comboitem.setValue("");
		semester.appendChild(comboitem);
		semester.setSelectedItem(comboitem);

		for (int i = 1; i < 15; i++) {
			comboitem = new MyComboitemConfig("" + i);
			comboitem.setValue("" + i);
			semester.appendChild(comboitem);
		}

		semester.setReadonly(true);

		final Combobox angkatan = new Combobox();
		Common.generateTahunAngkatan(angkatan);
		comboitem = new MyComboitemConfig("Semua Angkatan");
		comboitem.setValue("");
		angkatan.appendChild(comboitem);
		angkatan.setSelectedItem(comboitem);
		angkatan.setReadonly(true);

		final Combobox jurusan = new Combobox();
		Common.insertCombo(jurusan, "nama", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		comboitem = new MyComboitemConfig("Semua " + Common.getBahasaConfig("Jurusan"));
		comboitem.setValue(null);
		jurusan.appendChild(comboitem);
		jurusan.setSelectedItem(comboitem);
		jurusan.setReadonly(true);

		final Combobox program = Common.initPrograms(null);

		Row row = new Row();row.setValign("top");
		ParameterUmum parameterUmum = Common.getParameterUmum(key, nilai);
		try {
			RevisiHelper.createNewRevisi(ParameterUmum.class, parameterUmum, Common.getBahasaConfig(label))
					.setParent(row);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		final Textbox info1Textbox = new Textbox();
		info1Textbox.setWidth("90%");
		EventListener eventListener = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				String smt = semester.getSelectedItem() == null || semester.getSelectedItem().getValue().equals("")
						? "_smt:0" : "_smt:" + semester.getSelectedItem().getValue();
				String ang = angkatan.getSelectedItem() == null || angkatan.getSelectedItem().getValue().equals("")
						? "_ang:0" : "_ang:" + angkatan.getSelectedItem().getValue();
				String jur = jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null
						? "_jur:0" : "_jur:" + ((Jurusan) jurusan.getSelectedItem().getValue()).getId();
				String pro = program.getSelectedItem() == null || program.getSelectedItem().getValue() == null
						|| program.getSelectedItem().getValue() == null ? "_pro:0"
								: "_pro:" + program.getSelectedItem().getValue();

				String semua = smt + ang + jur + pro;

				if ((semester.getSelectedItem() == null || semester.getSelectedItem().getValue().equals(""))
						&& (angkatan.getSelectedItem() == null || angkatan.getSelectedItem().getValue().equals(""))
						&& (program.getSelectedItem() == null || program.getSelectedItem().getValue() == null
								|| program.getSelectedItem().getValue().equals(""))
						&& (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null)) {
					semua = "";
				}

				ParameterUmum parameterUmum = Common.getParameterUmum(key + semua, semua.trim().isEmpty() ? nilai : "");
				if (event.getTarget() == info1Textbox) {
					parameterUmum.setNilai(info1Textbox.getValue().trim());
				} else {
					info1Textbox.setValue(parameterUmum.getNilai());
				}
				Session session = HibernateUtil.currentNativeSession();
				session.getTransaction().begin();
				session.update(parameterUmum);
				session.getTransaction().commit();

				HibernateUtil.closeSession();
				// Common.parameterUmums.put(parameterUmum.getNama(),
				// parameterUmum);

				try {
					if (paramEventListener != null) {
						paramEventListener.onEvent(new Event("", info1Textbox, parameterUmum));
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		};

		info1Textbox.addEventListener("onChange", eventListener);
		semester.addEventListener("onChange", eventListener);
		angkatan.addEventListener("onChange", eventListener);
		jurusan.addEventListener("onChange", eventListener);
		program.addEventListener("onChange", eventListener);

		info1Textbox.setValue(parameterUmum.getNilai());
		info1Textbox.setRows(rowCount);

		Vbox vbox = new Vbox();
		vbox.setWidth("90%");
		row.appendChild(vbox);

		vbox.appendChild(info1Textbox);

		Hbox hbox = new Hbox();
		hbox.setWidth("90%");
		vbox.appendChild(hbox);

		hbox.appendChild(semester);
		hbox.appendChild(angkatan);
		hbox.appendChild(jurusan);
		hbox.appendChild(program);

		semester.setCols(8);
		angkatan.setCols(8);
		jurusan.setCols(12);
		program.setCols(8);

		hbox.appendChild(createButtonLihat(key));

		return row;

	}

	protected Row createRowAktifSemesterDanAngkatanDanJurusan(final String label, final String key, final String nilai,
			final EventListener paramEventListener) {

		final Combobox semester = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig("Semua Semester");
		comboitem.setValue("");
		semester.appendChild(comboitem);
		semester.setSelectedItem(comboitem);

		for (int i = 1; i < 15; i++) {
			comboitem = new MyComboitemConfig("" + i);
			comboitem.setValue("" + i);
			semester.appendChild(comboitem);
		}

		semester.setReadonly(true);

		final Combobox angkatan = new Combobox();
		Common.generateTahunAngkatan(angkatan);
		comboitem = new MyComboitemConfig("Semua Angkatan");
		comboitem.setValue("");
		angkatan.appendChild(comboitem);
		angkatan.setSelectedItem(comboitem);
		angkatan.setReadonly(true);

		final Combobox jurusan = new Combobox();
		Common.insertCombo(jurusan, "nama", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		comboitem = new MyComboitemConfig("Semua " + Common.getBahasaConfig("Jurusan"));
		comboitem.setValue(null);
		jurusan.appendChild(comboitem);
		jurusan.setSelectedItem(comboitem);
		jurusan.setReadonly(true);

		final Combobox program = Common.initPrograms(null);

		Row row = new Row();row.setValign("top");
		ParameterUmum parameterUmum = Common.getParameterUmum(key, nilai);
		try {
			RevisiHelper.createNewRevisi(ParameterUmum.class, parameterUmum, Common.getBahasaConfig(label))
					.setParent(row);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		final Combobox info1Textbox = createComboActive();
		comboitem = new MyComboitemConfig("Tidak ada");
		comboitem.setValue("");
		info1Textbox.appendChild(comboitem);
		info1Textbox.setWidth("90%");
		EventListener eventListener = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				String smt = semester.getSelectedItem() == null || semester.getSelectedItem().getValue().equals("")
						? "_smt:0" : "_smt:" + semester.getSelectedItem().getValue();
				String ang = angkatan.getSelectedItem() == null || angkatan.getSelectedItem().getValue().equals("")
						? "_ang:0" : "_ang:" + angkatan.getSelectedItem().getValue();
				String jur = jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null
						? "_jur:0" : "_jur:" + ((Jurusan) jurusan.getSelectedItem().getValue()).getId();
				String pro = program.getSelectedItem() == null || program.getSelectedItem().getValue() == null
						|| program.getSelectedItem().getValue() == null ? "_pro:0"
								: "_pro:" + program.getSelectedItem().getValue();

				String semua = smt + ang + jur + pro;

				if ((semester.getSelectedItem() == null || semester.getSelectedItem().getValue().equals(""))
						&& (angkatan.getSelectedItem() == null || angkatan.getSelectedItem().getValue().equals(""))
						&& (program.getSelectedItem() == null || program.getSelectedItem().getValue() == null
								|| program.getSelectedItem().getValue().equals(""))
						&& (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null)) {
					semua = "";
				}

				ParameterUmum parameterUmum = Common.getParameterUmum(key + semua, semua.trim().isEmpty() ? nilai : "");
				if (event.getTarget() == info1Textbox) {
					parameterUmum.setNilai(info1Textbox.getValue().trim());
				} else {
					Common.selectComboItem(info1Textbox, parameterUmum.getNilai());
				}
				Session session = HibernateUtil.currentNativeSession();
				session.getTransaction().begin();
				session.update(parameterUmum);
				session.getTransaction().commit();

				HibernateUtil.closeSession();
				// Common.parameterUmums.put(parameterUmum.getNama(),
				// parameterUmum);

				try {
					if (paramEventListener != null) {
						paramEventListener.onEvent(new Event("", info1Textbox, parameterUmum));
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		};

		info1Textbox.addEventListener("onChange", eventListener);
		semester.addEventListener("onChange", eventListener);
		angkatan.addEventListener("onChange", eventListener);
		jurusan.addEventListener("onChange", eventListener);
		program.addEventListener("onChange", eventListener);

		Common.selectComboItem(info1Textbox, parameterUmum.getNilai());
		info1Textbox.setReadonly(true);

		Vbox vbox = new Vbox();
		vbox.setWidth("90%");
		row.appendChild(vbox);

		vbox.appendChild(info1Textbox);

		Hbox hbox = new Hbox();
		hbox.setWidth("90%");
		vbox.appendChild(hbox);

		hbox.appendChild(semester);
		hbox.appendChild(angkatan);
		hbox.appendChild(jurusan);
		hbox.appendChild(program);

		semester.setCols(8);
		angkatan.setCols(8);
		jurusan.setCols(12);
		program.setCols(8);

		hbox.appendChild(createButtonLihat(key));

		return row;

	}

	public static Row createRowNilaiProgramDanJurusan(final String label, final String key, final String nilai,
			int rowCount, final EventListener paramEventListener, final Combobox customCombo) {

		final Combobox program = Common.initPrograms(null);

		final Combobox jurusan = new Combobox();
		Common.insertCombo(jurusan, "nama", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		MyComboitemConfig comboitem = new MyComboitemConfig("Semua " + Common.getBahasaConfig("Jurusan"));
		comboitem.setValue(null);
		jurusan.appendChild(comboitem);
		jurusan.setSelectedItem(comboitem);
		jurusan.setReadonly(true);

		Row row = new Row();row.setValign("top");
		ParameterUmum parameterUmum = Common.getParameterUmum(key, nilai);
		try {
			RevisiHelper.createNewRevisi(ParameterUmum.class, parameterUmum, Common.getBahasaConfig(label))
					.setParent(row);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		final Textbox info1Textbox = new Textbox();
		info1Textbox.setWidth("90%");
		EventListener eventListener = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				String prog = program.getSelectedItem() == null || program.getSelectedItem().getValue() == null
						|| program.getSelectedItem().getValue().equals("") ? "_prog:0"
								: "_prog:" + program.getSelectedItem().getValue();
				String jur = jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null
						? "_jur:0" : "_jur:" + ((Jurusan) jurusan.getSelectedItem().getValue()).getId();

				String cust = customCombo == null || customCombo.getSelectedItem() == null
						|| customCombo.getSelectedItem().getValue() == null ? "_cust:0"
								: "_cust:" + customCombo.getSelectedItem().getValue();

				if (cust.trim().equals("_cust:")) {
					cust = "_cust:0";
				}

				String semua = prog + cust + jur;

				if ((program.getSelectedItem() == null || program.getSelectedItem().getValue() == null
						|| program.getSelectedItem().getValue().equals(""))
						&& (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null)
						&& (customCombo == null || customCombo.getSelectedItem() == null
								|| customCombo.getSelectedItem().getValue() == null)) {
					semua = "";
				}

				ParameterUmum parameterUmum = Common.getParameterUmum(key + semua, semua.trim().isEmpty() ? nilai : "");
				if (event.getTarget() == info1Textbox) {
					parameterUmum.setNilai(info1Textbox.getValue().trim());
				} else {
					info1Textbox.setValue(parameterUmum.getNilai());
				}
				Session session = HibernateUtil.currentNativeSession();
				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, parameterUmum);
				session.getTransaction().commit();

				HibernateUtil.closeSession();
				// Common.parameterUmums.put(parameterUmum.getNama(),
				// parameterUmum);

				try {
					if (paramEventListener != null) {
						paramEventListener.onEvent(new Event("", info1Textbox, parameterUmum));
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		};

		info1Textbox.addEventListener("onChange", eventListener);
		program.addEventListener("onChange", eventListener);
		jurusan.addEventListener("onChange", eventListener);

		info1Textbox.setValue(parameterUmum.getNilai());
		info1Textbox.setRows(rowCount);

		Vbox vbox = new Vbox();
		vbox.setWidth("90%");
		row.appendChild(vbox);

		vbox.appendChild(info1Textbox);

		Hbox hbox = new Hbox();
		hbox.setWidth("90%");
		vbox.appendChild(hbox);

		hbox.appendChild(program);
		hbox.appendChild(jurusan);

		program.setCols(5);
		jurusan.setCols(8);

		if (customCombo != null) {
			hbox.appendChild(customCombo);
			customCombo.setCols(5);
			customCombo.addEventListener("onChange", eventListener);
		}

		hbox.appendChild(createButtonLihatProgramJurusan(key));

		return row;

	}

	public static MyButtonConfig createButtonLihat(final String key) {
		return createButtonLihat(key, false);
	}

	public static MyButtonConfig createButtonLihat(final String key, final Boolean hanyaTampilKeyDanNilai) {
		MyButtonConfig button = new MyButtonConfig("Lihat", "/img/print.png");
		button.setWidth("60px");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));
				final Intbox intbox = new Intbox(10);
				Clients.showBusy(label.getValue());

				final String filename = Sessions.getCurrent().getWebApp()
						.getRealPath("/tmp/cetak_data_"
								+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
								+ ".xlsx");
				final File file;
				(file = new File(filename)).createNewFile();

				final Timer timer = new Timer(200);
				timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				timer.setRepeats(true);
				timer.addEventListener("onTimer", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						try {

							Clients.showBusy(label.getValue());
							System.out.println("label " + label.getValue());

							if (label.getValue().trim().equalsIgnoreCase("-")) {
								Clients.clearBusy();
								timer.detach();
							} else if (label.getValue().isEmpty()) {

								Center center = new Center();
								final MyWindow window = new MyWindow("Cetak Data", "none", true);
								window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
								window.setHeight("97%");
								window.setWidth("90%");

								Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
								borderlayout.setParent(window);

								ais.ui.util.ZkCompat.setFlex(center, true);
								center.setParent(borderlayout);

								System.out.println("loading file " + file.getAbsolutePath());
								Common.clear(center);
								Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
				Common.clear(center);spreadsheet.setParent(center);
								spreadsheet.setWidth("100%");
								spreadsheet.setHeight("100%");
								spreadsheet.setSrc("../../tmp/" + file.getName());

								spreadsheet.setMaxrows(intbox.getValue() + 1);
								spreadsheet.setMaxcolumns(7);
								ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

								South south = new South();
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

								MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Download Data",
										"/img/excel.png");
								print.addEventListener("onClick", new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {

										try {
											Filedownload.save(new FileInputStream(file), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
													file.getName());
										} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
									}
								});
								print.setParent(toolbar);

								window.setVisible(true);
								window.onModal();

								Clients.clearBusy();
								timer.detach();
							}

						} catch (Exception e) {
							Clients.clearBusy();
						}

					}
				});
				timer.start();

				try {

					Clients.showBusy(label.getValue());

					new Thread(new Runnable() {

						@Override
						public void run() {
							try {
							Session session = HibernateUtil.currentNativeSession();
							try {

								@SuppressWarnings("unchecked")
								List<ParameterUmum> parameterUmums = session.createCriteria(ParameterUmum.class)
										.add(Restrictions.ilike("nama", key, MatchMode.START))
										.add(Restrictions.ne("nilai", "")).add(Restrictions.isNotNull("nilai"))
										.addOrder(Order.asc("nama")).list();
								intbox.setValue(parameterUmums.size());

								XSSFWorkbook workbook = new XSSFWorkbook();
								XSSFSheet sheet = workbook.createSheet("KONFIGURASI");

								sheet.setDefaultColumnWidth(20);
								int rowIndex = 0;

								XSSFRow rowhead;

								if (hanyaTampilKeyDanNilai) {
									rowhead = sheet.createRow((short) 0);
									rowhead.createCell(0).setCellValue("Nilai");
									rowhead.createCell(1).setCellValue("Key");
								} else {
									rowhead = sheet.createRow((short) 0);
									rowhead.createCell(0).setCellValue("Nilai");
									rowhead.createCell(1).setCellValue("Info1");
									rowhead.createCell(2).setCellValue("Semester");
									rowhead.createCell(3).setCellValue("Tahun Angkatan");
									rowhead.createCell(4).setCellValue("Jurusan");
									rowhead.createCell(5).setCellValue("Program");
									rowhead.createCell(6).setCellValue("Key");
								}

								for (ParameterUmum parameterUmum : parameterUmums) {
									try {
										rowIndex++;
										if (parameterUmum == null) {
											continue;
										}
										label.setValue("Sedang memproses data " + parameterUmum.getNama() + " ("
												+ Common.numberFormat.get().format(rowIndex * 100.0 / parameterUmums.size())
												+ " %)");

										XSSFRow row = sheet.createRow(rowIndex);

										if (hanyaTampilKeyDanNilai) {
											row.createCell(0).setCellValue(parameterUmum.getNilai());
											row.createCell(1).setCellValue(parameterUmum.getNama());
										} else {

											String[] t = parameterUmum.getNama().split("smt:");
											String smt = t.length == 1 ? "" : t[1].split("_")[0];
											t = parameterUmum.getNama().split("ang:");
											String ang = t.length == 1 ? "" : t[1].split("_")[0];
											t = parameterUmum.getNama().split("jur:");
											String jur = t.length == 1 ? "" : t[1].split("_")[0];
											t = parameterUmum.getNama().split("pro:");
											String pro = t.length == 1 ? "" : t[1].split("_")[0];

											String namaJurusan = jur.isEmpty() || jur.equals("0") ? ""
													: (String) session.createCriteria(Jurusan.class)
															.add(Restrictions.idEq(Long.parseLong(jur)))
															.setProjection(Projections.property("nama")).uniqueResult();

											row.createCell(0).setCellValue(parameterUmum.getNilai());
											row.createCell(1).setCellValue(parameterUmum.getInfo1());
											row.createCell(2)
													.setCellValue(smt.isEmpty() || smt.equals("0") ? "Semua" : smt);
											row.createCell(3)
													.setCellValue(ang.isEmpty() || ang.equals("0") ? "Semua" : ang);
											row.createCell(4).setCellValue(namaJurusan == null || namaJurusan.isEmpty()
													? "Semua" : namaJurusan);
											row.createCell(5)
													.setCellValue(pro.isEmpty() || pro.equals("0") ? "Semua" : pro);
											row.createCell(6).setCellValue(parameterUmum.getNama());
										}

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
									}
								}

								try {
									FileOutputStream fileOut = new FileOutputStream(filename);
									workbook.write(fileOut);
									fileOut.close();
								} catch (IOException e) {
									// TODO Auto-generated catch block
									Common.tampilErrorJikaAdmin(e);
								}
								System.out.println(
										"Your excel file has been generated! " );
								parameterUmums.clear();
								parameterUmums = null;
								label.setValue("");
							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
								label.setValue("-");
							}
							HibernateUtil.closeSession();
													} finally {
								ais.database.hibernate.HibernateUtil.closeSession();
							}
						}
					}).start();

				} catch (Exception e) {
					// TODO Auto-generated catch block
					Common.tampilErrorJikaAdmin(e);
				}

			}
		});

		return button;
	}

	public static MyButtonConfig createButtonLihatProgramJurusan(final String key) {
		MyButtonConfig button = new MyButtonConfig("Lihat", "/img/print.png");
		button.setWidth("60px");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));
				final Intbox intbox = new Intbox(10);
				Clients.showBusy(label.getValue());

				final String filename = Sessions.getCurrent().getWebApp()
						.getRealPath("/tmp/cetak_data_"
								+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
								+ ".xlsx");
				final File file;
				(file = new File(filename)).createNewFile();

				final Timer timer = new Timer(200);
				timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				timer.setRepeats(true);
				timer.addEventListener("onTimer", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						try {

							Clients.showBusy(label.getValue());
							System.out.println("label " + label.getValue());

							if (label.getValue().trim().equalsIgnoreCase("-")) {
								Clients.clearBusy();
								timer.detach();
							} else if (label.getValue().isEmpty()) {

								Center center = new Center();
								final MyWindow window = new MyWindow("Cetak Data", "none", true);
								window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
								window.setHeight("97%");
								window.setWidth("90%");

								Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
								borderlayout.setParent(window);

								ais.ui.util.ZkCompat.setFlex(center, true);
								center.setParent(borderlayout);

								System.out.println("loading file " + file.getAbsolutePath());
								Common.clear(center);
								Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
				Common.clear(center);spreadsheet.setParent(center);
								spreadsheet.setWidth("100%");
								spreadsheet.setHeight("100%");
								spreadsheet.setSrc("../../tmp/" + file.getName());

								spreadsheet.setMaxrows(intbox.getValue() + 1);
								spreadsheet.setMaxcolumns(6);
								ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

								South south = new South();
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

								MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Download Data",
										"/img/excel.png");
								print.addEventListener("onClick", new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {

										try {
											Filedownload.save(new FileInputStream(file), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
													file.getName());
										} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
									}
								});
								print.setParent(toolbar);

								window.setVisible(true);
								window.onModal();

								Clients.clearBusy();
								timer.detach();
							}

						} catch (Exception e) {
							Clients.clearBusy();
						}

					}
				});
				timer.start();

				try {

					Clients.showBusy(label.getValue());

					new Thread(new Runnable() {

						@Override
						public void run() {
							try {
							Session session = HibernateUtil.currentNativeSession();
							try {

								@SuppressWarnings("unchecked")
								List<ParameterUmum> parameterUmums = session.createCriteria(ParameterUmum.class)
										.add(Restrictions.ilike("nama", key, MatchMode.START))
										.add(Restrictions.ne("nilai", "")).add(Restrictions.isNotNull("nilai"))
										.addOrder(Order.asc("nama")).list();
								intbox.setValue(parameterUmums.size());

								XSSFWorkbook workbook = new XSSFWorkbook();
								XSSFSheet sheet = workbook.createSheet("KONFIGURASI");

								sheet.setDefaultColumnWidth(20);
								int rowIndex = 0;

								XSSFRow rowhead = sheet.createRow((short) 0);
								rowhead.createCell(0).setCellValue("Nilai");
								rowhead.createCell(1).setCellValue("Info1");
								rowhead.createCell(2).setCellValue("Program");
								rowhead.createCell(3).setCellValue("Jurusan");
								rowhead.createCell(4).setCellValue("Custom");
								rowhead.createCell(5).setCellValue("Key");

								for (ParameterUmum parameterUmum : parameterUmums) {
									try {
										rowIndex++;
										if (parameterUmum == null) {
											continue;
										}
										label.setValue("Sedang memproses data " + parameterUmum.getNama() + " ("
												+ Common.numberFormat.get().format(rowIndex * 100.0 / parameterUmums.size())
												+ " %)");

										XSSFRow row = sheet.createRow(rowIndex);

										String[] t = parameterUmum.getNama().split("_prog:");
										String prog = t.length == 1 ? "" : t[1].split("_")[0];
										t = parameterUmum.getNama().split("jur:");
										String jur = t.length == 1 ? "" : t[1].split("_")[0];
										String namaJurusan = jur.isEmpty() || jur.equals("0") ? ""
												: (String) session.createCriteria(Jurusan.class)
														.add(Restrictions.idEq(Long.parseLong(jur)))
														.setProjection(Projections.property("nama")).uniqueResult();

										t = parameterUmum.getNama().split("_cust:");
										String cust = t.length == 1 ? "" : t[1].split("_")[0];

										row.createCell(0).setCellValue(parameterUmum.getNilai());
										row.createCell(1).setCellValue(parameterUmum.getInfo1());
										row.createCell(2)
												.setCellValue(prog.isEmpty() || prog.equals("0") ? "Semua" : prog);
										row.createCell(3).setCellValue(
												namaJurusan == null || namaJurusan.isEmpty() ? "Semua" : namaJurusan);
										row.createCell(4).setCellValue(
												cust == null || cust.isEmpty() || cust.equals("0") ? "Semua" : cust);
										row.createCell(5).setCellValue(parameterUmum.getNama());

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
									}
								}

								try {
									FileOutputStream fileOut = new FileOutputStream(filename);
									workbook.write(fileOut);
									fileOut.close();
								} catch (IOException e) {
									// TODO Auto-generated catch block
									Common.tampilErrorJikaAdmin(e);
								}
								System.out.println(
										"Your excel file has been generated! " );
								parameterUmums.clear();
								parameterUmums = null;
								label.setValue("");
							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
								label.setValue("-");
							}
							HibernateUtil.closeSession();
													} finally {
								ais.database.hibernate.HibernateUtil.closeSession();
							}
						}
					}).start();

				} catch (Exception e) {
					// TODO Auto-generated catch block
					Common.tampilErrorJikaAdmin(e);
				}

			}
		});

		return button;
	}

	protected Row createRowNilaiPassword(final String label, final String key, final String nilai, int rowCount,
			final EventListener paramEventListener) {
		Row row = new Row();row.setValign("top");
		ParameterUmum parameterUmum = Common.getParameterUmum(key, nilai);
		try {
			RevisiHelper.createNewRevisi(ParameterUmum.class, parameterUmum, Common.getBahasaConfig(label))
					.setParent(row);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		final Textbox info1Textbox = new Textbox();
		info1Textbox.setType("password");
		info1Textbox.setWidth("90%");
		EventListener eventListener = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				ParameterUmum parameterUmum = Common.getParameterUmum(key, nilai);
				parameterUmum.setNilai(info1Textbox.getValue().trim());
				Session session = HibernateUtil.currentNativeSession();
				session.getTransaction().begin();
				session.update(parameterUmum);
				session.getTransaction().commit();

				HibernateUtil.closeSession();
				// Common.parameterUmums.put(parameterUmum.getNama(),
				// parameterUmum);

				try {
					if (paramEventListener != null) {
						paramEventListener.onEvent(new Event("", info1Textbox, parameterUmum));
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		};

		info1Textbox.addEventListener("onChange", eventListener);

		info1Textbox.setValue(parameterUmum.getNilai());
		info1Textbox.setRows(rowCount);

		row.appendChild(info1Textbox);

		return row;
	}

	public Row createRowNilai(final String label, final String key, final String nilai, final String nilai2) {
		return createRowNilai(label, key, nilai, nilai2, 1, null);
	}

	protected Row createRowNilai(final String label, final String key, final String nilai, final String nilai2,
			int rowCount, final EventListener paramEventListener) {
		return createRowNilaiDariVo(label, key, nilai, nilai2, rowCount, null, paramEventListener);
	}

	protected Row createRowNilaiDariVo(final String label, final String mykey, final String nilai, final String nilai2,
			int rowCount, final Combobox pilihan, final EventListener paramEventListener) {
		return createRowNilaiDariVo(label, mykey, nilai, nilai2, null, rowCount, pilihan, paramEventListener);
	}

	protected Row createRowNilaiDariVo(final String label, final String mykey, final String nilai, final String nilai2,
			final String nilai3, int rowCount, final Combobox pilihan, final EventListener paramEventListener) {
		return createRowNilaiDariVo(label, mykey, nilai, nilai2, nilai3, rowCount, pilihan, null, null, null,
				paramEventListener);
	}

	protected Row createRowNilaiDariVo(final String label, final String mykey, final String nilai, final String nilai2,
			final String nilai3, int rowCount, final Combobox pilihan, final Combobox pilihan1, final Combobox pilihan2,
			final Combobox pilihan3, final EventListener paramEventListener) {
		Row row = new Row();row.setValign("top");

		String newKey = mykey;
		if (pilihan != null && pilihan.getSelectedItem() != null && pilihan.getSelectedItem().getValue() != null) {
			if (pilihan.getSelectedItem().getValue() instanceof GeneralValueObject) {
				GeneralValueObject generalValueObject = (GeneralValueObject) pilihan.getSelectedItem().getValue();
				newKey += "_" + generalValueObject.getId();
			} else {
				newKey += "_" + pilihan.getSelectedItem().getValue();
			}
		}
		if (pilihan1 != null && pilihan1.getSelectedItem() != null && pilihan1.getSelectedItem().getValue() != null) {
			if (pilihan1.getSelectedItem().getValue() instanceof GeneralValueObject) {
				GeneralValueObject generalValueObject = (GeneralValueObject) pilihan1.getSelectedItem().getValue();
				newKey += "_" + generalValueObject.getId();
			} else {
				newKey += "_" + pilihan1.getSelectedItem().getValue();
			}
		}
		if (pilihan2 != null && pilihan2.getSelectedItem() != null && pilihan2.getSelectedItem().getValue() != null) {
			if (pilihan2.getSelectedItem().getValue() instanceof GeneralValueObject) {
				GeneralValueObject generalValueObject = (GeneralValueObject) pilihan2.getSelectedItem().getValue();
				newKey += "_" + generalValueObject.getId();
			} else {
				newKey += "_" + pilihan2.getSelectedItem().getValue();
			}
		}
		if (pilihan3 != null && pilihan3.getSelectedItem() != null && pilihan3.getSelectedItem().getValue() != null) {
			if (pilihan3.getSelectedItem().getValue() instanceof GeneralValueObject) {
				GeneralValueObject generalValueObject = (GeneralValueObject) pilihan3.getSelectedItem().getValue();
				newKey += "_" + generalValueObject.getId();
			} else {
				newKey += "_" + pilihan3.getSelectedItem().getValue();
			}
		}

		ParameterUmum parameterUmum = Common.getParameterUmum(newKey, nilai, nilai2, nilai3 == null ? "" : nilai3, "");
		try {
			RevisiHelper.createNewRevisi(ParameterUmum.class, parameterUmum,
					Common.getBahasaConfig(label) + "\n(" + mykey + ")").setParent(row);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		final Textbox info1Textbox = new Textbox();
		info1Textbox.setWidth("90%");
		final Textbox info2Textbox = new Textbox();
		info2Textbox.setWidth("90%");
		final Textbox info3Textbox = new Textbox();
		info3Textbox.setWidth("90%");
		EventListener eventListener = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				String newKey = mykey;
				if (pilihan != null && pilihan.getSelectedItem() != null
						&& pilihan.getSelectedItem().getValue() != null) {
					if (pilihan.getSelectedItem().getValue() instanceof GeneralValueObject) {
						GeneralValueObject generalValueObject = (GeneralValueObject) pilihan.getSelectedItem()
								.getValue();
						newKey += "_" + generalValueObject.getId();
					} else {
						newKey += "_" + pilihan.getSelectedItem().getValue();
					}
				}
				if (pilihan1 != null && pilihan1.getSelectedItem() != null
						&& pilihan1.getSelectedItem().getValue() != null) {
					if (pilihan1.getSelectedItem().getValue() instanceof GeneralValueObject) {
						GeneralValueObject generalValueObject = (GeneralValueObject) pilihan1.getSelectedItem()
								.getValue();
						newKey += "_" + generalValueObject.getId();
					} else {
						newKey += "_" + pilihan1.getSelectedItem().getValue();
					}
				}
				if (pilihan2 != null && pilihan2.getSelectedItem() != null
						&& pilihan2.getSelectedItem().getValue() != null) {
					if (pilihan2.getSelectedItem().getValue() instanceof GeneralValueObject) {
						GeneralValueObject generalValueObject = (GeneralValueObject) pilihan2.getSelectedItem()
								.getValue();
						newKey += "_" + generalValueObject.getId();
					} else {
						newKey += "_" + pilihan2.getSelectedItem().getValue();
					}
				}
				if (pilihan3 != null && pilihan3.getSelectedItem() != null
						&& pilihan3.getSelectedItem().getValue() != null) {
					if (pilihan3.getSelectedItem().getValue() instanceof GeneralValueObject) {
						GeneralValueObject generalValueObject = (GeneralValueObject) pilihan3.getSelectedItem()
								.getValue();
						newKey += "_" + generalValueObject.getId();
					} else {
						newKey += "_" + pilihan3.getSelectedItem().getValue();
					}
				}

				ParameterUmum parameterUmum = Common.getParameterUmum(newKey, nilai, info2Textbox.getValue().trim(),
						info3Textbox.getValue().trim(), "");

				System.out.println("newKey = " + newKey + ", parameterUmum " + parameterUmum);

				if ((pilihan != null && event.getTarget() == pilihan)
						|| (pilihan1 != null && event.getTarget() == pilihan1)
						|| (pilihan2 != null && event.getTarget() == pilihan2)
						|| (pilihan3 != null && event.getTarget() == pilihan3)) {
					info1Textbox.setValue(parameterUmum.getNilai());
					info2Textbox.setValue(parameterUmum.getInfo1());
					info3Textbox.setValue(parameterUmum.getInfo2());
				} else {
					parameterUmum.setNilai(info1Textbox.getValue().trim());
					parameterUmum.setInfo1(info2Textbox.getValue().trim());
					parameterUmum.setInfo2(info3Textbox.getValue().trim());
					Session session = HibernateUtil.currentNativeSession();
					session.getTransaction().begin();
					session.update(parameterUmum);
					session.getTransaction().commit();

					HibernateUtil.closeSession();
					// Common.parameterUmums.put(parameterUmum.getNama(),
					// parameterUmum);

					try {
						if (paramEventListener != null) {
							paramEventListener.onEvent(new Event("", info1Textbox, parameterUmum));
						}
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}

			}
		};

		info1Textbox.addEventListener("onChange", eventListener);
		info2Textbox.addEventListener("onChange", eventListener);
		info3Textbox.addEventListener("onChange", eventListener);

		Hbox hbox = new Hbox();
		hbox.setWidth("100%");
		row.appendChild(hbox);

		info1Textbox.setValue(parameterUmum.getNilai());
		info1Textbox.setRows(rowCount);

		info2Textbox.setValue(parameterUmum.getInfo1());
		info2Textbox.setRows(rowCount);

		info3Textbox.setValue(parameterUmum.getInfo2());
		info3Textbox.setRows(rowCount);

		if (pilihan != null) {
			hbox.appendChild(pilihan);
			pilihan.addEventListener("onChange", eventListener);
		}
		if (pilihan1 != null) {
			hbox.appendChild(pilihan1);
			pilihan1.addEventListener("onChange", eventListener);
		}
		if (pilihan2 != null) {
			hbox.appendChild(pilihan2);
			pilihan2.addEventListener("onChange", eventListener);
		}
		if (pilihan3 != null) {
			hbox.appendChild(pilihan3);
			pilihan3.addEventListener("onChange", eventListener);
		}

		hbox.appendChild(info1Textbox);

		if (nilai2 != null) {
			hbox.appendChild(info2Textbox);
		}

		if (nilai3 != null) {
			hbox.appendChild(info3Textbox);
		}

		hbox.appendChild(createButtonLihat(mykey, true));

		return row;
	}

	protected Row createRowNilai(final String label, final String key, final String nilai, final String nilai2,
			final Combobox pilihan) {
		return createRowNilai(label, key, nilai, nilai2, 1, pilihan, null);
	}

	protected Row createRowNilai(final String label, final String key, final String nilai, final String nilai2,
			int rowCount, final Combobox pilihan, final EventListener paramEventListener) {
		Row row = new Row();row.setValign("top");
		String pil = (String) (pilihan.getSelectedItem() == null ? "_" : pilihan.getSelectedItem().getValue());
		ParameterUmum parameterUmum = Common.getParameterUmum(key + pil, nilai);
		try {
			RevisiHelper.createNewRevisi(ParameterUmum.class, parameterUmum, Common.getBahasaConfig(label))
					.setParent(row);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		final Textbox info1Textbox = new Textbox();
		info1Textbox.setWidth("90%");
		final Textbox info2Textbox = new Textbox();
		info2Textbox.setWidth("90%");
		final EventListener eventListener = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				String pil = (String) (pilihan.getSelectedItem() == null || pilihan.getSelectedItem().getValue() == null
						? "" : pilihan.getSelectedItem().getValue());

				ParameterUmum parameterUmum = Common.getParameterUmum(key + pil, nilai);
				parameterUmum.setNilai(info1Textbox.getValue().trim());
				parameterUmum.setInfo1(info2Textbox.getValue().trim());
				Session session = HibernateUtil.currentNativeSession();
				session.getTransaction().begin();
				session.update(parameterUmum);
				session.getTransaction().commit();

				HibernateUtil.closeSession();
				// Common.parameterUmums.put(parameterUmum.getNama(),
				// parameterUmum);

				try {
					if (paramEventListener != null) {
						paramEventListener.onEvent(new Event("", info1Textbox, parameterUmum));
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		};

		info1Textbox.addEventListener("onChange", eventListener);
		info2Textbox.addEventListener("onChange", eventListener);

		Hbox hbox = new Hbox();
		hbox.setWidth("100%");
		row.appendChild(hbox);

		info1Textbox.setValue(parameterUmum.getNilai());
		info1Textbox.setRows(rowCount);

		info2Textbox.setValue(parameterUmum.getInfo1());
		info2Textbox.setRows(rowCount);

		hbox.appendChild(pilihan);
		pilihan.setReadonly(true);
		pilihan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				String pil = (String) (pilihan.getSelectedItem() == null ? "_" : pilihan.getSelectedItem().getValue());
				ParameterUmum parameterUmum = Common.getParameterUmum(key + pil, nilai);
				info1Textbox.setValue(parameterUmum.getNilai());
				info2Textbox.setValue(parameterUmum.getInfo1());
			}
		});

		hbox.appendChild(info1Textbox);
		hbox.appendChild(info2Textbox);

		return row;
	}

	protected Row createRowNilaiSemesterDanAngkatanDanJurusan(final String label, final String key, final String nilai,
			final String nilai2, int rowCount, final Combobox pilihan, final EventListener paramEventListener) {
		Row row = new Row();row.setValign("top");
		String pil = (String) (pilihan.getSelectedItem() == null ? "_" : pilihan.getSelectedItem().getValue());
		ParameterUmum parameterUmum = Common.getParameterUmum(key + pil, nilai);
		try {
			RevisiHelper.createNewRevisi(ParameterUmum.class, parameterUmum, Common.getBahasaConfig(label))
					.setParent(row);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		final Combobox semester = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig("Semua Semester");
		comboitem.setValue("");
		semester.appendChild(comboitem);
		semester.setSelectedItem(comboitem);

		for (int i = 1; i < 15; i++) {
			comboitem = new MyComboitemConfig("" + i);
			comboitem.setValue("" + i);
			semester.appendChild(comboitem);
		}

		semester.setReadonly(true);

		final Combobox angkatan = new Combobox();
		Common.generateTahunAngkatan(angkatan);
		comboitem = new MyComboitemConfig("Semua Angkatan");
		comboitem.setValue("");
		angkatan.appendChild(comboitem);
		angkatan.setSelectedItem(comboitem);
		angkatan.setReadonly(true);

		final Combobox jurusan = new Combobox();
		Common.insertCombo(jurusan, "nama", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		comboitem = new MyComboitemConfig("Semua " + Common.getBahasaConfig("Jurusan"));
		comboitem.setValue(null);
		jurusan.appendChild(comboitem);
		jurusan.setSelectedItem(comboitem);
		jurusan.setReadonly(true);

		final Textbox info1Textbox = new Textbox();
		info1Textbox.setWidth("90%");
		final Textbox info2Textbox = new Textbox();
		info2Textbox.setWidth("90%");
		final EventListener eventListener = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				String pil = (String) (pilihan.getSelectedItem() == null ? "_" : pilihan.getSelectedItem().getValue());

				String smt = semester.getSelectedItem() == null || semester.getSelectedItem().getValue().equals("")
						? "_smt:0" : "_smt:" + semester.getSelectedItem().getValue();
				String ang = angkatan.getSelectedItem() == null || angkatan.getSelectedItem().getValue().equals("")
						? "_ang:0" : "_ang:" + angkatan.getSelectedItem().getValue();
				String jur = jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null
						? "_jur:0" : "_jur:" + ((Jurusan) jurusan.getSelectedItem().getValue()).getId();

				String semua = smt + ang + jur;

				if ((semester.getSelectedItem() == null || semester.getSelectedItem().getValue().equals(""))
						&& (angkatan.getSelectedItem() == null || angkatan.getSelectedItem().getValue().equals(""))
						&& (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null)) {
					semua = "";
				}

				ParameterUmum parameterUmum = Common.getParameterUmum(key + pil + semua,
						semua.trim().isEmpty() ? nilai : "");
				if (event.getTarget() == info1Textbox) {
					parameterUmum.setNilai(info1Textbox.getValue().trim());
				} else {
					info1Textbox.setValue(parameterUmum.getNilai());
				}

				if (event.getTarget() == info2Textbox) {
					parameterUmum.setInfo1(info2Textbox.getValue().trim());
				} else {
					info2Textbox.setValue(parameterUmum.getInfo1());
				}

				Session session = HibernateUtil.currentNativeSession();
				session.getTransaction().begin();
				session.update(parameterUmum);
				session.getTransaction().commit();

				HibernateUtil.closeSession();
				// Common.parameterUmums.put(parameterUmum.getNama(),
				// parameterUmum);

				try {
					if (paramEventListener != null) {
						paramEventListener.onEvent(new Event("", info1Textbox, parameterUmum));
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		};

		info1Textbox.addEventListener("onChange", eventListener);
		info2Textbox.addEventListener("onChange", eventListener);
		semester.addEventListener("onChange", eventListener);
		angkatan.addEventListener("onChange", eventListener);
		jurusan.addEventListener("onChange", eventListener);

		Vbox vbox = new Vbox();
		vbox.setWidth("90%");
		row.appendChild(vbox);

		Hbox hbox = new Hbox();
		hbox.setWidth("100%");
		vbox.appendChild(hbox);

		info1Textbox.setValue(parameterUmum.getNilai());
		info1Textbox.setRows(rowCount);

		info2Textbox.setValue(parameterUmum.getInfo1());
		info2Textbox.setRows(rowCount);

		hbox.appendChild(pilihan);
		pilihan.setReadonly(true);
		pilihan.addEventListener("onChange", eventListener);

		hbox.appendChild(info1Textbox);
		hbox.appendChild(info2Textbox);

		hbox = new Hbox();
		hbox.setWidth("90%");
		vbox.appendChild(hbox);

		hbox.appendChild(semester);
		hbox.appendChild(angkatan);
		hbox.appendChild(jurusan);

		semester.setCols(10);
		angkatan.setCols(10);
		jurusan.setCols(15);

		hbox.appendChild(createButtonLihat(key));

		return row;
	}

	protected Combobox createComboActive() {
		return createComboActive(false);
	}

	protected Combobox createComboActive(boolean tambahkanTidakWajib) {
		Combobox nilai = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig("Aktif");
		comboitem.setValue(ParameterUmum.AKTIF);
		nilai.appendChild(comboitem);
		comboitem = new MyComboitemConfig("Tidak Aktif");
		comboitem.setValue(ParameterUmum.TIDAK_AKTIF);
		nilai.appendChild(comboitem);
		if (tambahkanTidakWajib) {
			comboitem = new MyComboitemConfig("Tidak Wajib");
			comboitem.setValue(ParameterUmum.AKTIF_TIDAK_WAJIB);
			nilai.appendChild(comboitem);
		}
		nilai.setReadonly(true);
		return nilai;
	}

}
