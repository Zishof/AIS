package ais.action.master.sekolah;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Button;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Separator;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.sekolah.helper.AmbilDataSiswaBanbox;
import ais.action.master.helper.AmbilDataTbmuserBanbox;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.AktiftasHarianSiswa;
import ais.database.model.sekolah.JenisAktiftasHarianDefault;
import ais.database.model.sekolah.JenisMateriHarianDefault;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class AktiftasHarianSiswaAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault, DataInitDefault {

	private static final long serialVersionUID = 1L;
	
	// UI Halaman Depan
	private Textbox searchnama;
	private Datebox searchtanggal;
	private MyGrid grid;
	private Paging paging;
	private MyToolbarbuttonConfig add;
	
	// Tabpanels
	private Tabpanel panelDaftarSiswa;
	private Tabpanel panelDashboard;
	private Tabpanel panelJenisAktiftasHarianDefault;
	private Tabpanel panelJenisMateriHarianDefault;
	private Tabpanel panelBukuPenghubung;
	
	// Popup Form UI Components
	private MyWindow addWindow;
	private Div divSiswa, divPembina1, divPembina2, divPembina3;
	private Datebox tanggalInput;
	private Rows rowsAkt, rowsMat;
	private Textbox pesanPembina, pesanOrangTua, keterangan;
	private Button btnTambahAkt, btnTambahMat;
	
	private AmbilDataSiswaBanbox siswaBanbox;
	private AmbilDataTbmuserBanbox pembina1Banbox, pembina2Banbox, pembina3Banbox;
	
	private AktiftasHarianSiswa currentObj;
	private boolean isGuru = false, isOrtu = false, edit = false, delete = false;
	
	// Penanda untuk mode popup dari Kalender (Standalone)
	private boolean standaloneMode = false;
	private EventListener onSavedCallback;
	
	/**
	 * PEMANGGILAN DASHBOARD
	 */
	public void onDashboardAktifitasHarianSiswa(Event event) {
		if (panelDashboard != null && panelDashboard.getChildren().isEmpty()) {
			Common.clear(panelDashboard);
			new MyInclude("/pages/master/sekolah/dashboard_aktifitas_harian_siswa.zul").setParent(panelDashboard);
		}
	}

	public void onBukuPenghubung(Event event) {
		if (panelBukuPenghubung != null && panelBukuPenghubung.getChildren().isEmpty()) {
			BukuPenghubungSiswa buku = new BukuPenghubungSiswa();
			buku.setWidth("100%");
			buku.setHeight("100%");
			buku.setParent(panelBukuPenghubung);
		}
	}

	/**
	 * METHOD STATIC UTAMA: Memunculkan Window Edit/Tambah dari luar class
	 */
	public static void showPopupForm(AktiftasHarianSiswa obj, Component parent, EventListener onSavedCallback) {
		AktiftasHarianSiswaAction action = new AktiftasHarianSiswaAction();
		Tbmuser user = Common.getCurrentUser();
		if (user != null) {
			action.isGuru = (user.getGuru() != null || user.getSuperadmin());
			action.isOrtu = (user.getOrangTua() != null || user.getSiswa() != null);
		}
		action.edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		action.delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		
		action.standaloneMode = true;
		action.onSavedCallback = onSavedCallback;
		
		action.addWindow = new ais.ui.util.MyWindow();
		if (action.addWindow.getPage() == null) {
			action.addWindow.setPage(org.zkoss.zk.ui.Executions.getCurrent().getDesktop().getFirstPage());
		}
		action.addWindow.setTitle("Form Aktivitas Harian Siswa");
		action.addWindow.setWidth("950px");
		action.addWindow.setHeight("95%");
		action.addWindow.setBorder("normal");
		try {
			action.addWindow.setMode("modal");
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		
		if (parent != null) {
			action.addWindow.setParent(parent);
		}
		
		
		
		try {
			action.init(obj);
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();
		
		Tbmuser user = Common.getCurrentUser();
		if (user != null) {
			isGuru = (user.getGuru() != null || user.getSuperadmin());
			isOrtu = (user.getOrangTua() != null || user.getSiswa() != null);
		}
		
		if (add != null) {
			add.setVisible(isGuru && CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		}
		
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		
		String[] contents = new String[] { "id", "kode", "siswa.nis", "siswa.nama", "tanggal", "nama",
				"aktifitas", "materi", "pesanPembina", "pesanOrangTua", "pembina1.userNama",
				"pembina2.userNama", "pembina3.userNama", "keterangan", "aktif" };
		Common.appendDownloadButton(add, AktiftasHarianSiswa.class, this, contents);
		
		initDefaultMasterData();
		onSearchDefault(null);
		
		onDaftarAktifitasSiswa(null);
	}

	// ===================================================================================
	// NAVIGASI TAB (DYNAMIC INCLUDE)
	// ===================================================================================

	public void onDaftarAktifitasSiswa(Event event) {
		if (panelDaftarSiswa != null && panelDaftarSiswa.getChildren().isEmpty()) {
			Common.clear(panelDaftarSiswa);
			new MyInclude("/pages/master/sekolah/daftar_aktifitas_harian_siswa.zul").setParent(panelDaftarSiswa);
		}
	}

	public void onJenisAktiftasHarianDefault(Event event) {
		if (panelJenisAktiftasHarianDefault != null && panelJenisAktiftasHarianDefault.getChildren().isEmpty()) {
			Common.clear(panelJenisAktiftasHarianDefault);
			new MyInclude("/pages/master/sekolah/jenis_aktiftas_harian_default.zul").setParent(panelJenisAktiftasHarianDefault);
		}
	}

	public void onJenisMateriHarianDefault(Event event) {
		if (panelJenisMateriHarianDefault != null && panelJenisMateriHarianDefault.getChildren().isEmpty()) {
			Common.clear(panelJenisMateriHarianDefault);
			new MyInclude("/pages/master/sekolah/jenis_materi_harian_default.zul").setParent(panelJenisMateriHarianDefault);
		}
	}

	private void initDefaultMasterData() {
		Session session = null;
		Transaction tx = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			Long countAkt = (Long) session.createCriteria(JenisAktiftasHarianDefault.class).setProjection(Projections.rowCount()).uniqueResult();
			Long countMat = (Long) session.createCriteria(JenisMateriHarianDefault.class).setProjection(Projections.rowCount()).uniqueResult();
			
			if ((countAkt != null && countAkt == 0) || (countMat != null && countMat == 0)) {
				tx = session.beginTransaction();
				if (countAkt == 0) {
					String[] defAkt = {"Shalat Jamaah", "Membaca Al-Quran", "Membantu Orang Tua", "Olahraga"};
					for (String s : defAkt) {
						JenisAktiftasHarianDefault d = new JenisAktiftasHarianDefault();
						d.setNama(s); d.setAktif(true); session.save(d);
					}
				}
				if (countMat == 0) {
					String[] defMat = {"Tahfidz", "Hadits", "Bahasa Arab", "Fiqih"};
					for (String s : defMat) {
						JenisMateriHarianDefault d = new JenisMateriHarianDefault();
						d.setNama(s); d.setAktif(true); session.save(d);
					}
				}
				tx.commit();
			}
		} catch (Exception e) {
			if (tx != null && tx.isActive()) tx.rollback();
		} finally {
			if (session != null) session.close();
		}
	}

	@Override
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		return initCriteria(order, session);
	}

	public Criteria initCriteria(boolean order, Session session) {
		Criteria criteria = session.createCriteria(AktiftasHarianSiswa.class);
		if (order) criteria.addOrder(Order.desc("tanggal"));
		
		if (searchnama != null && !searchnama.getValue().trim().isEmpty()) {
			criteria.createAlias("siswa", "s");
			criteria.add(Restrictions.ilike("s.namaSiswa", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		}
		if (searchtanggal != null && searchtanggal.getValue() != null) {
			criteria.add(Restrictions.eq("tanggal", searchtanggal.getValue()));
		}
		if (isOrtu) {
			List<Long> kids = Common.getCurrentUser().getOrangTua().ambilAnakSiswa();
			if (!kids.isEmpty()) criteria.add(Restrictions.in("siswa.id", kids));
		}
		return criteria;
	}

	@Override
	public void onSearchDefault(Event event) {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Common.initPaging(initCriteria(false, session), paging);
			List<AktiftasHarianSiswa> list = ConstantValues.simpleList(
				initCriteria(true, session)
					.setFirstResult((paging == null ? 0 : paging.getActivePage()) * Common.ROWS_COUNT_ON_PAGE)
					.setMaxResults(Common.ROWS_COUNT_ON_PAGE), 
				AktiftasHarianSiswa.class);
			
			if(grid != null) {
				grid.setRowRenderer(new MyRenderer());
				grid.setModelCheckMobile(new SimpleListModel(list));
			}
		} finally {
			session.close();
		}
	}

	class MyRenderer extends ais.ui.util.MyRowRenderer {
		@SuppressWarnings("unchecked")
		@Override
		public void render(final Row row, Object data) throws Exception {
			final AktiftasHarianSiswa akt = (AktiftasHarianSiswa) data;
			row.appendChild(new Label(Common.dateFormat6.get().format(akt.getTanggal())));
			row.appendChild(new Label(akt.getSiswa() != null ? akt.getSiswa().getNamaSiswa() : ""));

			// TAMPILAN DETAIL VIA HTML COMPONENT
			StringBuilder detailHtml = new StringBuilder();
			detailHtml.append("<div style='padding:5px; font-size:11px;'>");

			if (akt.getKeterangan() != null && !akt.getKeterangan().trim().isEmpty()) {
				detailHtml.append("<span style='color:blue; font-weight:bold;'>Keterangan:</span> ")
						  .append(akt.getKeterangan()).append("<br/><br/>");
			}

			try {
				if (akt.getAktifitas() != null && !akt.getAktifitas().trim().isEmpty()) {
					JSONObject jAkt = new JSONObject(akt.getAktifitas());
					if (jAkt.length() > 0) {
						detailHtml.append("<span style='font-weight:bold;'>Aktivitas Harian:</span><ul style='margin-top:2px; margin-bottom:8px; padding-left:20px;'>");
						Iterator<String> keys = jAkt.keys();
						while (keys.hasNext()) {
							JSONObject item = jAkt.getJSONObject(keys.next());
							String nama = item.optString("nama", "");
							String nilai = item.optString("nilai", "");
							if (!nama.isEmpty() && !nilai.isEmpty()) {
								String color = nilai.equalsIgnoreCase("YA") ? "green" : "red";
								detailHtml.append("<li>").append(nama).append(" : <span style='color:").append(color).append("; font-weight:bold;'>").append(nilai).append("</span></li>");
							}
						}
						detailHtml.append("</ul>");
					}
				}
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

			try {
				if (akt.getMateri() != null && !akt.getMateri().trim().isEmpty()) {
					JSONObject jMat = new JSONObject(akt.getMateri());
					if (jMat.length() > 0) {
						detailHtml.append("<span style='font-weight:bold;'>Materi:</span><ul style='margin-top:2px; margin-bottom:8px; padding-left:20px;'>");
						Iterator<String> keys = jMat.keys();
						while (keys.hasNext()) {
							JSONObject item = jMat.getJSONObject(keys.next());
							String nama = item.optString("nama", "");
							String nilai = item.optString("nilai", "");
							if (!nama.isEmpty() && !nilai.isEmpty()) {
								detailHtml.append("<li>").append(nama).append(" : <b>").append(nilai).append("</b></li>");
							}
						}
						detailHtml.append("</ul>");
					}
				}
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
			
			detailHtml.append("</div>");
			Html htmlDetail = new Html(detailHtml.toString());
			row.appendChild(htmlDetail);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(akt.getAktif() == null ? false : akt.getAktif());
			checkbox.setParent(row);
			row.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					akt.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(akt);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, akt, AktiftasHarianSiswaAction.this).setParent(row);
		}
	}

	public void onAdd(Event event) throws Exception { 
		init(new AktiftasHarianSiswa()); 
	}

	@SuppressWarnings("deprecation")
	private void buildFormUI() {
		addWindow.getChildren().clear();
		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(addWindow);
		
		North north = new North();
		north.setBorder("none");
		north.setSize("110px");
		north.setStyle("padding:5px;");
		north.setParent(borderlayout);
		
		Grid gridNorth = new Grid();
		gridNorth.setStyle("border:none;");
		gridNorth.setParent(north);
		
		Columns colsNorth = new Columns();
		colsNorth.setParent(gridNorth);
		Column cn1 = new Column(); cn1.setWidth("100px"); cn1.setParent(colsNorth);
		Column cn2 = new Column(); cn2.setParent(colsNorth);
		Column cn3 = new Column(); cn3.setWidth("100px"); cn3.setParent(colsNorth);
		Column cn4 = new Column(); cn4.setParent(colsNorth);
		
		Rows rowsNorth = new Rows();
		rowsNorth.setParent(gridNorth);
		
		Row r1 = new Row(); r1.setStyle("background:transparent; border:none;"); r1.setParent(rowsNorth);
		r1.appendChild(new Label(ais.common.Common.getBahasaConfig("Siswa *")));
		divSiswa = new Div(); divSiswa.setWidth("100%"); r1.appendChild(divSiswa);
		r1.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal *")));
		tanggalInput = new Datebox(); tanggalInput.setFormat("dd-MM-yyyy"); r1.appendChild(tanggalInput);
		
		Row r2 = new Row(); r2.setStyle("background:transparent; border:none;"); r2.setParent(rowsNorth);
		r2.appendChild(new Label(ais.common.Common.getBahasaConfig("Pembina 1")));
		divPembina1 = new Div(); divPembina1.setWidth("100%"); r2.appendChild(divPembina1);
		r2.appendChild(new Label(ais.common.Common.getBahasaConfig("Pembina 2")));
		divPembina2 = new Div(); divPembina2.setWidth("100%"); r2.appendChild(divPembina2);
		
		Row r3 = new Row(); r3.setStyle("background:transparent; border:none;"); r3.setParent(rowsNorth);
		r3.appendChild(new Label(ais.common.Common.getBahasaConfig("Pembina 3")));
		divPembina3 = new Div(); divPembina3.setWidth("100%"); r3.appendChild(divPembina3);
		r3.appendChild(new Label(""));
		r3.appendChild(new Label(""));
		
		siswaBanbox = new AmbilDataSiswaBanbox(); siswaBanbox.setParent(divSiswa);
		pembina1Banbox = new AmbilDataTbmuserBanbox(); pembina1Banbox.setParent(divPembina1);
		pembina2Banbox = new AmbilDataTbmuserBanbox(); pembina2Banbox.setParent(divPembina2);
		pembina3Banbox = new AmbilDataTbmuserBanbox(); pembina3Banbox.setParent(divPembina3);
		
		Center center = new Center();
		center.setBorder("none"); center.setAutoscroll(true); center.setStyle("padding:10px;"); center.setParent(borderlayout);
		
		Vbox vboxCenter = new Vbox(); vboxCenter.setWidth("100%"); vboxCenter.setSpacing("10px"); vboxCenter.setParent(center);
		Hbox hboxSplit = new Hbox(); hboxSplit.setWidth("100%"); hboxSplit.setWidths("50%,50%"); hboxSplit.setParent(vboxCenter);
		
		Vbox vboxAkt = new Vbox(); vboxAkt.setWidth("98%"); vboxAkt.setParent(hboxSplit);
		Div divHAkt = new Div(); divHAkt.setStyle("background:#4A6792; padding:5px; color:white; font-weight:bold;"); divHAkt.setParent(vboxAkt);
		new Label("AKTIFITAS HARIAN (YA / TIDAK)").setParent(divHAkt);
		btnTambahAkt = new Button("Tambah Baris"); btnTambahAkt.setStyle("float:right; font-size:10px;"); btnTambahAkt.setParent(divHAkt);
		btnTambahAkt.addEventListener("onClick", new EventListener() { public void onEvent(Event e) { addRowAkt("", ""); } });
		Grid gridAkt = new Grid(); gridAkt.setEmptyMessage("Data aktivitas kosong"); gridAkt.setParent(vboxAkt);
		Columns colsAkt = new Columns(); colsAkt.setParent(gridAkt);
		Column ca1 = new Column("NO"); ca1.setWidth("40px"); ca1.setAlign("center"); ca1.setParent(colsAkt);
		Column ca2 = new Column("AKTIFITAS HARIAN"); ca2.setParent(colsAkt);
		Column ca3 = new Column("YA"); ca3.setWidth("50px"); ca3.setAlign("center"); ca3.setParent(colsAkt);
		Column ca4 = new Column("TIDAK"); ca4.setWidth("60px"); ca4.setAlign("center"); ca4.setParent(colsAkt);
		Column ca5 = new Column(); ca5.setWidth("40px"); ca5.setParent(colsAkt);
		rowsAkt = new Rows(); rowsAkt.setParent(gridAkt);
		
		Vbox vboxMat = new Vbox(); vboxMat.setWidth("98%"); vboxMat.setParent(hboxSplit);
		Div divHMat = new Div(); divHMat.setStyle("background:#4A6792; padding:5px; color:white; font-weight:bold;"); divHMat.setParent(vboxMat);
		new Label(ais.common.Common.getBahasaConfig("PENCAPAIAN MATERI")).setParent(divHMat);
		btnTambahMat = new Button("Tambah Baris"); btnTambahMat.setStyle("float:right; font-size:10px;"); btnTambahMat.setParent(divHMat);
		btnTambahMat.addEventListener("onClick", new EventListener() { public void onEvent(Event e) { addRowMat("", ""); } });
		Grid gridMat = new Grid(); gridMat.setEmptyMessage("Data materi kosong"); gridMat.setParent(vboxMat);
		Columns colsMat = new Columns(); colsMat.setParent(gridMat);
		Column cm1 = new Column("NO"); cm1.setWidth("40px"); cm1.setAlign("center"); cm1.setParent(colsMat);
		Column cm2 = new Column("MATERI"); cm2.setParent(colsMat);
		Column cm3 = new Column("PENILAIAN"); cm3.setWidth("120px"); cm3.setParent(colsMat);
		Column cm4 = new Column(); cm4.setWidth("40px"); cm4.setParent(colsMat);
		rowsMat = new Rows(); rowsMat.setParent(gridMat);
		
		Separator sep = new Separator(); sep.setBar(true); sep.setParent(vboxCenter);
		
		Grid gridPesan = new Grid(); gridPesan.setStyle("border:none;"); gridPesan.setParent(vboxCenter);
		Columns colsPesan = new Columns(); colsPesan.setParent(gridPesan);
		Column cp1 = new Column(); cp1.setWidth("150px"); cp1.setParent(colsPesan);
		Column cp2 = new Column(); cp2.setParent(colsPesan);
		Rows rowsPesan = new Rows(); rowsPesan.setParent(gridPesan);
		
		Row rp1 = new Row(); rp1.setStyle("background:transparent; border:none;"); rp1.setParent(rowsPesan);
		rp1.appendChild(new Label("Pesan Pembina / Guru"));
		pesanPembina = new Textbox(); pesanPembina.setRows(3); pesanPembina.setWidth("98%"); rp1.appendChild(pesanPembina);
		
		Row rp2 = new Row(); rp2.setStyle("background:transparent; border:none;"); rp2.setParent(rowsPesan);
		rp2.appendChild(new Label(ais.common.Common.getBahasaConfig("Pesan Orang Tua")));
		pesanOrangTua = new Textbox(); pesanOrangTua.setRows(3); pesanOrangTua.setWidth("98%"); rp2.appendChild(pesanOrangTua);
		
		Row rp3 = new Row(); rp3.setStyle("background:transparent; border:none;"); rp3.setParent(rowsPesan);
		rp3.appendChild(new Label(ais.common.Common.getBahasaConfig("Keterangan Umum")));
		keterangan = new Textbox(); keterangan.setRows(2); keterangan.setWidth("98%"); rp3.appendChild(keterangan);
		
		South south = new South(); south.setBorder("none"); south.setSize("45px"); south.setStyle("padding:5px;"); south.setParent(borderlayout);
		Toolbar tbSouth = new Toolbar(); tbSouth.setAlign("end"); tbSouth.setParent(south);
		MyToolbarbuttonConfig btnBatal = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		btnBatal.addEventListener("onClick", new EventListener() { public void onEvent(Event e) { onCancel(e); } });
		btnBatal.setParent(tbSouth);
		MyToolbarbuttonConfig btnSimpan = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		btnSimpan.addEventListener("onClick", new EventListener() { public void onEvent(Event e) throws Exception { onSave(e); } });
		btnSimpan.setParent(tbSouth);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void init(GeneralValueObject obj) throws Exception {
		currentObj = (AktiftasHarianSiswa) obj;
		buildFormUI();
		
		tanggalInput.setValue(currentObj.getTanggal() != null ? currentObj.getTanggal() : new Date());
		keterangan.setValue(currentObj.getKeterangan());
		pesanPembina.setValue(currentObj.getPesanPembina());
		pesanOrangTua.setValue(currentObj.getPesanOrangTua());
		
		siswaBanbox.setAttribute("siswa", currentObj.getSiswa());
		siswaBanbox.setValue(currentObj.getSiswa() == null ? "" : currentObj.getSiswa().getNamaSiswa());
		
		pembina1Banbox.setAttribute("tbmuser", currentObj.getPembina1());
		pembina1Banbox.setValue(currentObj.getPembina1() == null ? "" : currentObj.getPembina1().getUserNama());
		
		pembina2Banbox.setAttribute("tbmuser", currentObj.getPembina2());
		pembina2Banbox.setValue(currentObj.getPembina2() == null ? "" : currentObj.getPembina2().getUserNama());
		
		pembina3Banbox.setAttribute("tbmuser", currentObj.getPembina3());
		pembina3Banbox.setValue(currentObj.getPembina3() == null ? "" : currentObj.getPembina3().getUserNama());

		Tbmuser currentUser = Common.getCurrentUser();
		if (isGuru && currentUser.getGuru() != null) {
			if (currentObj.getId() == null) {
				pembina1Banbox.setAttribute("tbmuser", currentUser);
				pembina1Banbox.setValue(currentUser.getUserNama());
			}
			pembina1Banbox.setDisabled(true);
		} else {
			pembina1Banbox.setDisabled(!isGuru);
		}

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			if (currentObj.getId() == null) {
				// FILTER SEKOLAH PADA DATA DEFAULT (AKTIVITAS)
				Criteria critAkt = session.createCriteria(JenisAktiftasHarianDefault.class).addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("nama")).add(Restrictions.eq("aktif", true));
				if (currentObj.getSiswa() != null && currentObj.getSiswa().getSekolah() != null) {
					critAkt.add(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", currentObj.getSiswa().getSekolah())));
				}
				List<JenisAktiftasHarianDefault> defs = ConstantValues.simpleList(critAkt, JenisAktiftasHarianDefault.class);
				for (JenisAktiftasHarianDefault d : defs) addRowAkt(d.getNama(), "");

				// FILTER SEKOLAH PADA DATA DEFAULT (MATERI)
				Criteria critMat = session.createCriteria(JenisMateriHarianDefault.class).addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("nama")).add(Restrictions.eq("aktif", true));
				if (currentObj.getSiswa() != null && currentObj.getSiswa().getSekolah() != null) {
					critMat.add(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", currentObj.getSiswa().getSekolah())));
				}
				List<JenisMateriHarianDefault> mDefs = ConstantValues.simpleList(critMat, JenisMateriHarianDefault.class);
				for (JenisMateriHarianDefault d : mDefs) addRowMat(d.getNama(), "");
			} else {
				JSONObject jA = new JSONObject(currentObj.getAktifitas() != null && !currentObj.getAktifitas().isEmpty() ? currentObj.getAktifitas() : "{}");
				Iterator<String> kA = jA.keys();
				while (kA.hasNext()) {
					JSONObject item = jA.getJSONObject(kA.next());
					addRowAkt(item.getString("nama"), item.getString("nilai"));
				}

				JSONObject jM = new JSONObject(currentObj.getMateri() != null && !currentObj.getMateri().isEmpty() ? currentObj.getMateri() : "{}");
				Iterator<String> kM = jM.keys();
				while (kM.hasNext()) {
					JSONObject item = jM.getJSONObject(kM.next());
					addRowMat(item.getString("nama"), item.getString("nilai"));
				}
			}
		} finally {
			session.close();
		}

		siswaBanbox.setDisabled(!isGuru || currentObj.getId() != null);
		tanggalInput.setDisabled(!isGuru || currentObj.getId() != null);
		pembina2Banbox.setDisabled(!isGuru);
		pembina3Banbox.setDisabled(!isGuru);
		//pesanPembina.setReadonly(!isGuru);
		//pesanOrangTua.setReadonly(!isOrtu);
		btnTambahAkt.setVisible(isGuru);
		btnTambahMat.setVisible(isGuru);
		
		addWindow.setVisible(true);
		addWindow.doModal();
	}

	private void addRowAkt(String nm, String val) {
		final Row row = new Row();
		row.appendChild(new Label("" + (rowsAkt.getChildren().size() + 1)));
		Textbox tb = new Textbox(nm); tb.setWidth("95%"); tb.setReadonly(!isGuru); row.appendChild(tb);
		Radiogroup rg = new Radiogroup();
		Radio rYa = new Radio("YA"); rYa.setParent(rg); rYa.setDisabled(!isGuru);
		Radio rTdk = new Radio("TIDAK"); rTdk.setParent(rg); rTdk.setDisabled(!isGuru);
		if (val.equalsIgnoreCase("YA")) rYa.setSelected(true);
		else if (val.equalsIgnoreCase("TIDAK")) rTdk.setSelected(true);
		row.appendChild(rYa); row.appendChild(rTdk);
		MyToolbarbuttonConfig del = new MyToolbarbuttonConfig("", "/img/delete.gif");
		del.setVisible(isGuru);
		del.addEventListener("onClick", new EventListener() { public void onEvent(Event e) { rowsAkt.removeChild(row); } });
		row.appendChild(del);
		rowsAkt.appendChild(row);
	}

	private void addRowMat(String nm, String val) {
		final Row row = new Row();
		row.appendChild(new Label("" + (rowsMat.getChildren().size() + 1)));
		Textbox tbNm = new Textbox(nm); tbNm.setWidth("95%"); tbNm.setReadonly(!isGuru); row.appendChild(tbNm);
		Textbox tbV = new Textbox(val); tbV.setWidth("95%"); tbV.setReadonly(!isGuru); row.appendChild(tbV);
		MyToolbarbuttonConfig del = new MyToolbarbuttonConfig("", "/img/delete.gif");
		del.setVisible(isGuru);
		del.addEventListener("onClick", new EventListener() { public void onEvent(Event e) { rowsMat.removeChild(row); } });
		row.appendChild(del);
		rowsMat.appendChild(row);
	}

	public void onSave(Event event) throws Exception {
		if (siswaBanbox.getAttribute("siswa") == null) {
			MyMessageboxConfig.show("Mohon maaf, Siswa belum dipilih. Langkah yang dapat dilakukan: (1) klik kolom Siswa dan cari atau pilih nama siswa; (2) pastikan nama siswa ditemukan dan terpilih; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis."); return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = session.beginTransaction();
		try {
			Siswa s = (Siswa) siswaBanbox.getAttribute("siswa");
			Date tgl = tanggalInput.getValue();
			if (currentObj.getId() == null) {
				Number n = (Number) session.createCriteria(AktiftasHarianSiswa.class)
					.add(Restrictions.eq("siswa.id", s.getId())).add(Restrictions.eq("tanggal", tgl))
					.setProjection(Projections.rowCount()).uniqueResult();
				if (n != null && n.intValue() > 0) {
					MyMessageboxConfig.show("Data aktivitas siswa tanggal " + Common.dateFormat.get().format(tgl) + " sudah ada!");
					return;
				}
				currentObj.setKode(s.getNomorInduk() + "-" + Common.dateFormat8.get().format(tgl));
			}
			currentObj.setSiswa(s);
			currentObj.setTanggal(tgl);
			currentObj.setKeterangan(keterangan.getValue());
			currentObj.setPesanPembina(pesanPembina.getValue());
			currentObj.setPesanOrangTua(pesanOrangTua.getValue());
			currentObj.setPembina1((Tbmuser) pembina1Banbox.getAttribute("tbmuser"));
			currentObj.setPembina2((Tbmuser) pembina2Banbox.getAttribute("tbmuser"));
			currentObj.setPembina3((Tbmuser) pembina3Banbox.getAttribute("tbmuser"));
			
			JSONObject jA = new JSONObject(); int i=1;
			for (Object o : rowsAkt.getChildren()) {
				Component r = (Component) o; Row rw = (Row) r; 
				JSONObject item = new JSONObject();
				item.put("nama", ((Textbox) rw.getChildren().get(1)).getValue());
				String v = "";
				if (((Radio) rw.getChildren().get(2)).isSelected()) v = "YA";
				else if (((Radio) rw.getChildren().get(3)).isSelected()) v = "TIDAK";
				item.put("nilai", v); jA.put("" + (i++), item);
			}
			currentObj.setAktifitas(jA.toString());
			
			JSONObject jM = new JSONObject(); i=1;
			for (Object o : rowsMat.getChildren()) {
				Component r = (Component) o; Row rw = (Row) r; 
				JSONObject item = new JSONObject();
				item.put("nama", ((Textbox) rw.getChildren().get(1)).getValue());
				item.put("nilai", ((Textbox) rw.getChildren().get(2)).getValue());
				jM.put("" + (i++), item);
			}
			currentObj.setMateri(jM.toString());
			
			session.saveOrUpdate(currentObj);
			tx.commit();
			
			addWindow.setVisible(false);
			
			if (standaloneMode) {
				addWindow.detach();
				if (onSavedCallback != null) {
					onSavedCallback.onEvent(new Event("onSaved", null, currentObj));
				}
			} else {
				onSearchDefault(null);
			}
		} catch (Exception e) {
			if (tx != null && tx.isActive()) tx.rollback();
			ais.common.Common.tampilErrorJikaAdmin(e);
		} finally {
			session.close();
		}
	}

	public void onCancel(Event e) { 
		addWindow.setVisible(false); 
		if (standaloneMode) {
			addWindow.detach();
			if (onSavedCallback != null) {
				try { onSavedCallback.onEvent(new Event("onCancel", null, null)); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sekolah/AktiftasHarianSiswaAction.java:682");}
			}
		}
	}
}