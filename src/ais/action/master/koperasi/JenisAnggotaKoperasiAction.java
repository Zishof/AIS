package ais.action.master.koperasi;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

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
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.koperasi.JenisAnggotaKoperasi;
import ais.database.model.koperasi.CaraPembayaranKoperasi;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class JenisAnggotaKoperasiAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchkode;
	private Checkbox searchaktif;

	private Textbox nama;
	private Textbox keterangan;
	private Checkbox wajibPin;
	private Checkbox wajibBiometricWajah;
	private Checkbox wajibBiometricFingerprint;
	private Map<Checkbox, CaraPembayaranKoperasi> pilihanCaraBayar = new LinkedHashMap<Checkbox, CaraPembayaranKoperasi>();
	private Map<Checkbox, CaraPembayaranKoperasi> pilihanCaraBayarWajibPin = new LinkedHashMap<Checkbox, CaraPembayaranKoperasi>();

	private boolean edit = false;
	private boolean delete = false;

	private JenisAnggotaKoperasi jenisAnggotaKoperasi;
	private MyToolbarbuttonConfig add;
	private Textbox kode;

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

		String[] contents = new String[] { "id", "kode", "nama", "keterangan", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(JenisAnggotaKoperasi.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, JenisAnggotaKoperasi.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class JenisAnggotaKoperasiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final JenisAnggotaKoperasi jenisAnggotaKoperasi = (JenisAnggotaKoperasi) arg1;
			new Label(jenisAnggotaKoperasi.getKode()).setParent(arg0);
			RevisiHelper
					.createNewRevisi(JenisAnggotaKoperasi.class, jenisAnggotaKoperasi, jenisAnggotaKoperasi.getNama())
					.setParent(arg0);
			new Label(jenisAnggotaKoperasi.getKeterangan()).setParent(arg0);
			new Label(Boolean.TRUE.equals(jenisAnggotaKoperasi.getWajibPin()) ? "Wajib" : "-").setParent(arg0);
			new Label(Boolean.TRUE.equals(jenisAnggotaKoperasi.getWajibVerifikasiBiometricWajah()) ? "Wajib" : "-").setParent(arg0);
			new Label(Boolean.TRUE.equals(jenisAnggotaKoperasi.getWajibVerifikasiBiometricFingerprint()) ? "Wajib" : "-").setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(jenisAnggotaKoperasi.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisAnggotaKoperasi.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(jenisAnggotaKoperasi);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, jenisAnggotaKoperasi, JenisAnggotaKoperasiAction.this)
					.setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new JenisAnggotaKoperasi());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		jenisAnggotaKoperasi = (JenisAnggotaKoperasi) obj;
		init(jenisAnggotaKoperasi);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(JenisAnggotaKoperasi jenisAnggotaKoperasi) {
		this.jenisAnggotaKoperasi = jenisAnggotaKoperasi;
		addWindow.setTitle(jenisAnggotaKoperasi.getId() == null ? "Tambah Jenis Anggota Koperasi" : "Ubah Jenis Anggota Koperasi");
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

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Jenis Anggota"));
		row.appendChild(kode = new Textbox(jenisAnggotaKoperasi.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Jenis Anggota *"));
		row.appendChild(nama = new Textbox(jenisAnggotaKoperasi.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(jenisAnggotaKoperasi.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Verifikasi PIN"));
		row.appendChild(wajibPin = new Checkbox("Wajib sebelum memotong saldo"));
		wajibPin.setChecked(Boolean.TRUE.equals(jenisAnggotaKoperasi.getWajibPin()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Biometric Wajah"));
		row.appendChild(wajibBiometricWajah = new Checkbox("Wajib kamera + liveness sebelum memotong saldo"));
		wajibBiometricWajah.setChecked(Boolean.TRUE.equals(jenisAnggotaKoperasi.getWajibVerifikasiBiometricWajah()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fingerprint"));
		row.appendChild(wajibBiometricFingerprint = new Checkbox("Wajib scanner USB/OTG + SDK sebelum memotong saldo"));
		wajibBiometricFingerprint.setChecked(Boolean.TRUE.equals(jenisAnggotaKoperasi.getWajibVerifikasiBiometricFingerprint()));

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Cara Bayar yang Diizinkan"));
		Vbox caraBayar = new Vbox();
		pilihanCaraBayar.clear();
		String csvCara = jenisAnggotaKoperasi.getDaftarCaraPembayaranYangBolehDiPilih();
		@SuppressWarnings("unchecked")
		List<CaraPembayaranKoperasi> semuaCara = HibernateUtil.currentSession().createCriteria(CaraPembayaranKoperasi.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.asc("nama")).list();
		for (CaraPembayaranKoperasi cara : semuaCara) {
			Checkbox pilihan = new Checkbox(cara.getNama());
			pilihan.setChecked(csvCara.contains("," + cara.getId() + ","));
			caraBayar.appendChild(pilihan);
			pilihanCaraBayar.put(pilihan, cara);
		}
		row.appendChild(caraBayar);

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("PIN wajib untuk cara bayar"));
		Vbox caraBayarPin = new Vbox();
		caraBayarPin.appendChild(new Label("Kosong = semua cara bayar saat Wajib PIN aktif"));
		pilihanCaraBayarWajibPin.clear();
		String csvWajibPin = jenisAnggotaKoperasi.getDaftarCaraPembayaranWajibPin();
		for (CaraPembayaranKoperasi cara : semuaCara) {
			Checkbox pilihanPin = new Checkbox(cara.getNama());
			pilihanPin.setChecked(csvWajibPin.contains("," + cara.getId() + ","));
			caraBayarPin.appendChild(pilihanPin);
			pilihanCaraBayarWajibPin.put(pilihanPin, cara);
		}
		row.appendChild(caraBayarPin);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
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
			MyMessageboxConfig.show("Mohon maaf, nama jenis anggota koperasi belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Nama Jenis Anggota; (2) gunakan nama yang deskriptif dan belum terpakai; (3) ulangi penyimpanan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		boolean i = checkNamaJenisAnggotaKoperasi();
		if (i) {
			MyMessageboxConfig.show("Mohon maaf, nama jenis anggota koperasi sudah ada di database. Langkah yang dapat dilakukan: (1) gunakan nama lain yang belum terdaftar; (2) cari jenis anggota yang sudah ada di daftar; (3) ulangi penyimpanan.", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (jenisAnggotaKoperasi.getId() != null) {
			jenisAnggotaKoperasi = (JenisAnggotaKoperasi) session.load(JenisAnggotaKoperasi.class,
					jenisAnggotaKoperasi.getId());

		}

		jenisAnggotaKoperasi.setKode(kode.getValue());
		jenisAnggotaKoperasi.setNama(nama.getValue());
		jenisAnggotaKoperasi.setKeterangan(keterangan.getValue());
		jenisAnggotaKoperasi.setWajibPin(Boolean.valueOf(wajibPin.isChecked()));
		jenisAnggotaKoperasi.setWajibVerifikasiBiometricWajah(Boolean.valueOf(wajibBiometricWajah.isChecked()));
		jenisAnggotaKoperasi.setWajibVerifikasiBiometricFingerprint(Boolean.valueOf(wajibBiometricFingerprint.isChecked()));
		StringBuilder csvCara = new StringBuilder();
		for (Map.Entry<Checkbox, CaraPembayaranKoperasi> entry : pilihanCaraBayar.entrySet()) {
			if (entry.getKey().isChecked()) csvCara.append(",").append(entry.getValue().getId());
		}
		if (csvCara.length() > 0) csvCara.append(",");
		jenisAnggotaKoperasi.setDaftarCaraPembayaranYangBolehDiPilih(csvCara.toString());
		StringBuilder csvWajibPin = new StringBuilder();
		for (Map.Entry<Checkbox, CaraPembayaranKoperasi> entry : pilihanCaraBayarWajibPin.entrySet()) {
			if (entry.getKey().isChecked()) csvWajibPin.append(",").append(entry.getValue().getId());
		}
		if (csvWajibPin.length() > 0) csvWajibPin.append(",");
		if (csvCara.length() > 0) {
			for (Map.Entry<Checkbox, CaraPembayaranKoperasi> entry : pilihanCaraBayarWajibPin.entrySet()) {
				if (entry.getKey().isChecked() && !csvCara.toString().contains("," + entry.getValue().getId() + ",")) {
					MyMessageboxConfig.show("Cara bayar wajib PIN harus termasuk cara bayar yang diizinkan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return false;
				}
			}
		}
		jenisAnggotaKoperasi.setDaftarCaraPembayaranWajibPin(csvWajibPin.toString());

		Common.refreshSaveOrUpdate(session, jenisAnggotaKoperasi);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(JenisAnggotaKoperasi.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		criteria.add(searchkode == null || searchkode.getValue().trim().isEmpty()
		        ? Restrictions.sqlRestriction("true")
		        : Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<JenisAnggotaKoperasi> jenisAnggotaKoperasi = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(jenisAnggotaKoperasi);
		grid.setRowRenderer(new JenisAnggotaKoperasiRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkNamaJenisAnggotaKoperasi() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(JenisAnggotaKoperasi.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.jenisAnggotaKoperasi.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.jenisAnggotaKoperasi.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
