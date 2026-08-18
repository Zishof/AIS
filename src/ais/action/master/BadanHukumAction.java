package ais.action.master;

import org.hibernate.Session;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.dao.BadanHukumDao;
import ais.database.dao.DaoFactory;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BadanHukum;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyPanel;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class BadanHukumAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6990431715599659916L;

	private BadanHukum badanHukum;

	private Textbox kode;
	private Textbox nama;
	private Textbox alamat1;
	private Textbox alamat2;
	private Textbox kota;
	private Textbox kodePos;
	private Textbox telepon;
	private Textbox faksimil;
	private MyDatebox tanggalAkta;
	private Textbox namaAkta;
	private MyDatebox tanggalPengesahan;
	private Textbox nomorPengesahan;
	private MyDatebox tanggalAwalPendirian;
	private Textbox email;
	private Textbox alamatWebsite;
	private Textbox logo;

	MyWindow win;
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

		win = (MyWindow) comp;
		loadDataBadanHukum();

	}

	public void loadDataBadanHukum() {
		Session session = HibernateUtil.currentSession();

		badanHukum = (BadanHukum) session.createCriteria(BadanHukum.class).setMaxResults(1).uniqueResult();

		if (badanHukum == null)
			init(new BadanHukum());
		else
			init(badanHukum);

	}

	private void init(BadanHukum badanHukum) {
		this.badanHukum = badanHukum;
		win.getFellowIfAny("window");
		MyPanel panel = new MyPanel();
		panel.setParent(win);
		panel.setTitle("Data Badan Hukum");
		panel.setWidth("100%");
		panel.setHeight("100%");
		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
		row.appendChild(kode = new Textbox(badanHukum.getKode() == null ? "" : badanHukum.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new Textbox(badanHukum.getNama() == null ? "" : badanHukum.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Alamat 1"));
		row.appendChild(alamat1 = new Textbox(badanHukum.getAlamat1() == null ? "" : badanHukum.getAlamat1()));
		alamat1.setWidth("90%");
		alamat1.setRows(5);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Alamat 2"));
		row.appendChild(alamat2 = new Textbox(badanHukum.getAlamat2() == null ? "" : badanHukum.getAlamat2()));
		alamat2.setWidth("90%");
		alamat2.setRows(5);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kota"));
		row.appendChild(kota = new Textbox(badanHukum.getKota() == null ? "" : badanHukum.getKota()));
		kota.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Pos"));
		row.appendChild(kodePos = new Textbox(badanHukum.getKodePos() == null ? "" : badanHukum.getKodePos()));
		kodePos.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Telepon"));
		row.appendChild(telepon = new Textbox(badanHukum.getTelepon() == null ? "" : badanHukum.getTelepon()));
		telepon.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Faksimil"));
		row.appendChild(faksimil = new Textbox(badanHukum.getFaksimil() == null ? "" : badanHukum.getFaksimil()));
		faksimil.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Akta"));
		row.appendChild(tanggalAkta = new MyDatebox(
				badanHukum.getTanggalAkta() == null ? ais.ui.util.WaktuUtil.getDate() : badanHukum.getTanggalAkta()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Akta"));
		row.appendChild(namaAkta = new Textbox(badanHukum.getNamaAkta() == null ? "" : badanHukum.getNamaAkta()));
		namaAkta.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Pengesahan"));
		row.appendChild(tanggalPengesahan = new MyDatebox(
				badanHukum.getTanggalPengesahan() == null ? ais.ui.util.WaktuUtil.getDate() : badanHukum.getTanggalPengesahan()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor Pengesahan"));
		row.appendChild(nomorPengesahan = new Textbox(
				badanHukum.getNomorPengesahan() == null ? "" : badanHukum.getNomorPengesahan()));
		nomorPengesahan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Awal Pendirian"));
		row.appendChild(tanggalAwalPendirian = new MyDatebox(
				badanHukum.getTanggalAwalPendirian() == null ? ais.ui.util.WaktuUtil.getDate() : badanHukum.getTanggalAwalPendirian()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Email"));
		row.appendChild(email = new Textbox(badanHukum.getEmail() == null ? "" : badanHukum.getEmail()));
		email.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Website"));
		row.appendChild(alamatWebsite = new Textbox(
				badanHukum.getAlamatWebsite() == null ? "" : badanHukum.getAlamatWebsite()));
		alamatWebsite.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Logo"));
		row.appendChild(logo = new Textbox(badanHukum.getLogo() == null ? "" : badanHukum.getLogo()));
		logo.setWidth("90%");

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		/*
		 * MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("",
		 * "/img/cancel.gif");cancel.setTooltiptext("Tutup");
		 * cancel.addEventListener("onClick", new EventListener() {
		 * 
		 * @Override public void onEvent(Event event) throws Exception {
		 * 
		 * } }); cancel.setParent(toolbar);
		 */
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				// save data ke database
				onSave(event);
			}
		});
		save.setParent(toolbar);

	}

	public void onSave(Event event) throws Exception {
		try {
			if (kode.getValue().trim().equals("")) {
				PesanFormalHelper.tampilkanGagal("penyimpanan data Kode",
						"Kolom Kode belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
						new String[] {
								"Isi/pilih terlebih dahulu Kode.",
								"Ulangi proses penyimpanan setelah kolom tersebut terisi."
						});
				return;
			}

			BadanHukumDao badanHukumDao = DaoFactory.getInstance().getBadanHukumDao();

			if (badanHukum.getId() != null) {
				System.out.println("Load");
				badanHukum = badanHukumDao.load(badanHukum.getId());
			}

			badanHukum.setKode(kode.getValue());
			badanHukum.setNama(nama.getValue());
			badanHukum.setAlamat1(alamat1.getValue());
			badanHukum.setAlamat2(alamat2.getValue());
			badanHukum.setKota(kota.getValue());
			badanHukum.setKodePos(kodePos.getValue());
			badanHukum.setTelepon(telepon.getValue());
			badanHukum.setFaksimil(faksimil.getValue());
			badanHukum.setTanggalAkta(tanggalAkta.getValue());
			badanHukum.setNamaAkta(namaAkta.getValue());
			badanHukum.setTanggalPengesahan(tanggalPengesahan.getValue());
			badanHukum.setNomorPengesahan(nomorPengesahan.getValue());
			badanHukum.setTanggalAwalPendirian(tanggalAwalPendirian.getValue());
			badanHukum.setEmail(email.getValue());
			badanHukum.setAlamatWebsite(alamatWebsite.getValue());
			badanHukum.setLogo(logo.getValue());

			// badanHukumDao.beginTransaction();

			if (badanHukum.getId() != null) {
				// System.out.println("Update");
				badanHukumDao.update(badanHukum);
				// MyMessageboxConfig.show("Badan Hukum berhasil disimpan",
				// "Informasi",
				// MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			} else {
				// System.out.println("Save");
				badanHukum.setId(Long.valueOf(1));
				badanHukumDao.save(badanHukum);
				// MyMessageboxConfig.show("Badan Hukum berhasil disimpan",
				// "Informasi",
				// MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			}
			// badanHukumDao.commitTransaction();

			MyMessageboxConfig.show("Data berhasil disimpan!", "Informasi", 1, MyMessageboxConfig.INFORMATION);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			MyMessageboxConfig.show("Data gagal disimpan!");
		}
	}

}
