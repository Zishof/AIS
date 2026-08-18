package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.math.BigDecimal;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Decimalbox;
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
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Gedung;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.Ruang;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class RuangAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3786091220301468178L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;
	private Textbox searchnama;
	private Textbox nama;
	private MyDoublebox luas;
	// private Textbox searchkodeRuangan;
	private Textbox kodeRuangan;
	private Textbox searchkapasitasruangan;
	private Decimalbox kapasitasRuangan;
	private Combobox merupakanRuangKelas;
	private Combobox searchgedung;
	private Combobox gedung;
	private Checkbox searchaktif;

	private Combobox searchjurusan;
	private Combobox searchfakultas;
	private Combobox searchyayasan;
	private Combobox searchsekolah;
	private Label labelFakProd;
	private Label labelYaySek;
	private Hbox fakProd;
	private Hbox yaySek;

	private Combobox fakultas;
	private Combobox jurusan;

	private MyCheckboxConfig ikutiIpGedung;
	private Textbox ip;

	private MyToolbarbuttonConfig add;
	private Ruang ruang;
	private boolean edit;
	private boolean delete;

	private Tabpanel fasilitasRuangan;
	private Textbox keterangan;

//	private DetailRuangDosenHelper detailRuangDosenHelper = new DetailRuangDosenHelper();
	private boolean pt;
	private boolean ya;
	private Combobox yayasan;
	private Combobox sekolah;

	public void onFasilitasRuangan(Event event) {
		if (fasilitasRuangan.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(fasilitasRuangan);
			MyInclude iframe = new MyInclude("/pages/master/fasilitas_ruangan.zul");
			iframe.setParent(window);
		}
	}

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

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);


		boolean[] ptYa = Common.chekPtAtauSekolah();
		pt = ptYa[0];
		ya = ptYa[1];

		if (labelFakProd != null) { labelFakProd.setVisible(pt); }
		if (fakProd != null) { fakProd.setVisible(pt); }

		if (labelYaySek != null) { labelYaySek.setVisible(ya); }
		if (yaySek != null) { yaySek.setVisible(ya); }

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

		if (add != null) { add.setTooltiptext("Tambah"); }

		Tbmuser tbmuser = Common.getCurrentUser();
		Fakultas fak = tbmuser.ambilFakultas();
		Jurusan jur = tbmuser.ambilJurusan();

		Yayasan yay = tbmuser.ambilYayasan();
		Sekolah sek = tbmuser.ambilSekolah();

		Criterion criterion = fak == null ? Restrictions.sqlRestriction("true")
				: Restrictions.or(Restrictions.eq("fakultas", fak), Restrictions.isNull("fakultas"));

		criterion = Restrictions.and(criterion, jur == null ? Restrictions.sqlRestriction("true")
				: Restrictions.or(Restrictions.eq("jurusan", jur), Restrictions.isNull("jurusan")));

		criterion = Restrictions.and(criterion, yay == null ? Restrictions.sqlRestriction("true")
				: Restrictions.or(Restrictions.eq("yayasan", yay), Restrictions.isNull("yayasan")));

		criterion = Restrictions.and(criterion, sek == null ? Restrictions.sqlRestriction("true")
				: Restrictions.or(Restrictions.eq("sekolah", sek), Restrictions.isNull("sekolah")));

		criterion = Restrictions.and(criterion,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		Common.insertComboDanSemua(searchgedung, new String[] { "nama" }, "alamat", Gedung.class, "Semua Gedung",
				criterion);

		merupakanRuangKelas = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		if (comboitem != null) { comboitem.setLabel("Ya"); }
		if (comboitem != null) { comboitem.setValue(1); }
		merupakanRuangKelas.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel("Tidak"); }
		if (comboitem != null) { comboitem.setValue(0); }
		merupakanRuangKelas.appendChild(comboitem);

		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "kodeRuangan", "nama", "luas", "gedung", "kapasitasRuangan",
				"merupakanRuangKelas", "ikutiIpGedung", "ip", "yayasan", "sekolah", "fakultas", "jurusan", "aktif" };

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, Ruang.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class RuangRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Ruang ruang = (Ruang) arg1;

//			final Detail mydetail = new Detail();
//			mydetail.setParent(arg0);
//			mydetail.addEventListener("onOpen", new EventListener() {
//				@Override
//				public void onEvent(Event event) throws Exception {
//					Common.clear(mydetail);
//					if (mydetail.isOpen()) {
//						detailRuangDosenHelper.displayDetailDosen(ruang, mydetail);
//					}
//				}
//			});

			RevisiHelper.createNewRevisi(Ruang.class, ruang, ruang.getNama()).setParent(arg0);

			new Label(ruang.getKodeRuangan()).setParent(arg0);
			new Label(ruang.getGedung() == null ? "" : ruang.getGedung().getNama()).setParent(arg0);
			new Label(Common.numberFormat.get().format(ruang.getLuas()) + " m2").setParent(arg0);
			new Label((ruang == null ? Ruang.getDefaultKapasitas() : ruang.getKapasitasRuangan()) == null ? ""
					: ruang.getKapasitasRuangan().toString()).setParent(arg0);
			new Label(ruang.getMerupakanRuangKelas().equals(1) ? "Ya" : "Tidak").setParent(arg0);
			new Label(((ruang.getFakultas() == null ? "" : ruang.getFakultas().getNama())
					+ (ruang.getJurusan() == null ? "" : " / " + ruang.getJurusan().getNama()))
					+ (ruang.getYayasan() == null ? "" : ruang.getYayasan().getNama())
					+ (ruang.getSekolah() == null ? "" : " / " + ruang.getSekolah().getNama())).setParent(arg0);
			new Label(ruang.getIp()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(ruang.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					ruang.setAktif(checkbox.isChecked());
					Common.refreshUpdate(ruang);
				}
			});

			new Label(ruang.getKeterangan()).setParent(arg0);

			Common.copyEditDeleteButtons(edit, delete, ruang, RuangAction.this).setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new Ruang());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		ruang = (Ruang) obj;
		init(ruang);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings({})
	private void init(final Ruang ruang) throws Exception {
		this.ruang = ruang;
		addWindow.setTitle(ruang.getId() == null ? "Tambah Ruang" : "Ubah Ruang");
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

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Ruangan"));
		row.appendChild(nama = new Textbox(ruang.getNama() == null ? "" : ruang.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Ruangan"));
		row.appendChild(kodeRuangan = new Textbox(ruang.getKodeRuangan() == null ? "" : ruang.getKodeRuangan()));
		kodeRuangan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Luas Ruangan (m2)"));
		row.appendChild(luas = new MyDoublebox(ruang.getLuas()));
		luas.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kapasitas Ruangan"));
		row.appendChild(kapasitasRuangan = new Decimalbox(new BigDecimal(
				ruang.getKapasitasRuangan() == null ? Ruang.getDefaultKapasitas() : ruang.getKapasitasRuangan())));
		kapasitasRuangan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Merupakan Ruang Kelas"));
		Common.selectComboItem(merupakanRuangKelas, ruang.getMerupakanRuangKelas());
		row.appendChild(merupakanRuangKelas);
		merupakanRuangKelas.setWidth("90%");

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.ambilJurusan() != null) {
			ruang.setJurusan(tbmuser.ambilJurusan());
		}
		if (tbmuser != null && tbmuser.ambilFakultas() != null) {
			ruang.setFakultas(tbmuser.ambilFakultas());
		}

		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.initFakultasDanJurusanDanSemua(fakultas, jurusan, null, null);

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));
		Common.selectComboItem(yayasan, ruang.getYayasan() == null ? tbmuser.ambilYayasan() : ruang.getYayasan());
		row.appendChild(yayasan);
		yayasan.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));
		Common.pilihSekolah(sekolah, ruang.getSekolah() == null ? tbmuser.ambilSekolah() : ruang.getSekolah());
		row.appendChild(sekolah);
		sekolah.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		Common.selectComboItem(fakultas, ruang.getFakultas() == null ? tbmuser.ambilFakultas() : ruang.getFakultas());
		row.appendChild(fakultas);
		fakultas.setWidth("90%");

		if (fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() != null) {
			Common.insertComboDanSemua(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
		}

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		Common.pilihJurusan(jurusan, ruang.getJurusan() == null ? tbmuser.ambilJurusan() : ruang.getJurusan());
		row.appendChild(jurusan);
		jurusan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Gedung"));
		row.appendChild(gedung = new Combobox());
		gedung.setWidth("90%");

		EventListener isiGedung = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Fakultas fak = (Fakultas) (fakultas.getSelectedItem() == null ? null
						: fakultas.getSelectedItem().getValue());
				Jurusan jur = (Jurusan) (jurusan.getSelectedItem() == null ? null
						: jurusan.getSelectedItem().getValue());

				Yayasan yay = (Yayasan) (yayasan.getSelectedItem() == null ? null
						: yayasan.getSelectedItem().getValue());
				Sekolah sek = (Sekolah) (sekolah.getSelectedItem() == null ? null
						: sekolah.getSelectedItem().getValue());

				Criterion criterion = fak == null ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.eq("fakultas", fak), Restrictions.isNull("fakultas"));

				criterion = Restrictions.and(criterion, jur == null ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.eq("jurusan", jur), Restrictions.isNull("jurusan")));

				criterion = Restrictions.and(criterion, yay == null ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.eq("yayasan", yay), Restrictions.isNull("yayasan")));

				criterion = Restrictions.and(criterion, sek == null ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.eq("sekolah", sek), Restrictions.isNull("sekolah")));

				criterion = Restrictions.and(criterion,
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

				Common.clear(gedung);
				Common.insertComboDanSemua(gedung, new String[] { "nama" }, "alamat", Gedung.class, "Tanpa Gedung",
						criterion);
				Common.selectComboItem(gedung, ruang.getGedung());
			}
		};

		isiGedung.onEvent(null);
		fakultas.addEventListener("onChange", isiGedung);
		jurusan.addEventListener("onChange", isiGedung);
		yayasan.addEventListener("onChange", isiGedung);
		sekolah.addEventListener("onChange", isiGedung);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(ikutiIpGedung = new MyCheckboxConfig("Ikuti Ip Gedung"));
		ikutiIpGedung.setChecked(ruang.getIkutiIpGedung());

		final MyFormRow rowIp = new MyFormRow();
		rowIp.setStyle("border:0px;background: transparent;");
		rowIp.setParent(rows);
		rowIp.appendChild(new ais.ui.util.MyLabelConfig("Alamat IP Gedung"));
		rowIp.appendChild(ip = new Textbox(ruang.getIp()));
		ip.setWidth("90%");
		ip.setRows(2);
		final Row r = Common.initKeterangan(rows,
				"Pisahkan dengan tanda semikolon (;) jika terdapat banyak alamat IP. Kosongkan jika berlaku untuk semua alamat IP");

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				rowIp.setVisible(!ikutiIpGedung.isChecked());
				r.setVisible(!ikutiIpGedung.isChecked());
			}
		};

		ikutiIpGedung.addEventListener("onClick", eventListener);
		eventListener.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(ruang.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(2);

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
			PesanFormalHelper.tampilkanGagal("penyimpanan data Nama",
					"Kolom Nama belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		if (kodeRuangan.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Kode Ruangan",
					"Kolom Kode Ruangan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Kode Ruangan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (gedung.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Gedung",
					"Kolom Gedung belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Gedung.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (kapasitasRuangan.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Kapasitas Ruangan",
					"Kolom Kapasitas Ruangan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Kapasitas Ruangan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (merupakanRuangKelas.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Merupakan Ruang Kelas",
					"Kolom Merupakan Ruang Kelas belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Merupakan Ruang Kelas.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		ruang.setKeterangan(keterangan.getValue());
		ruang.setLuas(luas.getValue());
		ruang.setNama(nama.getValue());
		ruang.setKodeRuangan(kodeRuangan.getValue());
		ruang.setGedung((Gedung) (gedung.getSelectedItem() == null ? null : gedung.getSelectedItem().getValue()));
		ruang.setKapasitasRuangan(
				kapasitasRuangan.getValue() == null ? null : Integer.parseInt(kapasitasRuangan.getValue().toString()));
		ruang.setMerupakanRuangKelas(merupakanRuangKelas.getSelectedItem() == null ? null
				: Integer.parseInt(merupakanRuangKelas.getSelectedItem().getValue().toString()));
		ruang.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
						: fakultas.getSelectedItem().getValue()));
		ruang.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
						: jurusan.getSelectedItem().getValue()));

		ruang.setYayasan((Yayasan) (yayasan.getSelectedItem() == null ? null : yayasan.getSelectedItem().getValue()));
		ruang.setSekolah((Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue()));

		ruang.setIkutiIpGedung(ikutiIpGedung.isChecked());
		ruang.setIp(ip.getValue());
//		ruang.setFasilitasRuangans(selectedFasilitasRuangan);
		Common.refreshSaveOrUpdate(ruang);
		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Ruang.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));
		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.or(Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE),
						Restrictions.ilike("kodeRuangan", searchnama.getValue().trim(), MatchMode.ANYWHERE)))

				.add(searchkapasitasruangan.getValue().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("kapasitasRuangan",
								Integer.parseInt(searchkapasitasruangan.getValue().toString())))

				.add(CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

				.add(CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false))

				.add(CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))

				.add(CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false))

				.add(searchgedung.getSelectedItem() == null || searchgedung.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("gedung", searchgedung.getSelectedItem().getValue()));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Ruang> ruang = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(ruang);
		grid.setRowRenderer(new RuangRenderer());
		grid.setModelCheckMobile(strset);

	}

}
