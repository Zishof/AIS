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
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.East;
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
import org.zkoss.zul.Vbox;

import ais.action.master.akunting.helper.ItemBiayaPunyaAkunHelper;
import ais.action.master.akunting.helper.ItemBiayaPunyaDenda;
import ais.action.master.akunting.helper.ItemBiayaPunyaDibayarDimukaHelper;
import ais.action.master.akunting.helper.ItemBiayaPunyaDiskonHelper;
import ais.action.master.akunting.helper.ItemBiayaPunyaPiutangHelper;
import ais.action.master.helper.AmbilDataParameterTambahanBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.ItemBiaya;
import ais.database.model.ItemBiayaPunyaAkun;
import ais.database.model.ItemBiayaPunyaDibayarDimuka;
import ais.database.model.ItemBiayaPunyaDiskon;
import ais.database.model.ItemBiayaPunyaPendapatanDenda;
import ais.database.model.ItemBiayaPunyaPiutang;
import ais.database.model.JenisPembayaran;
import ais.database.model.Konfigurasi;
import ais.database.model.ParameterTambahan;
import ais.database.model.akunting.Akun;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyCombobox;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class ItemBiayaAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4124140285573733292L;

	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox serachkode;

	private Combobox searchpenghitungan;
	private Checkbox searchaktif;

	private Textbox nama;
	private Textbox kode;
	private Textbox deskripsi;
	private Textbox namaMatakuliah;
	private MyCombobox penghitungan;
	private MyCheckboxConfig nilaiBisaDiubah;
	private MyCheckboxConfig mahasiswaBolehMencicilkan;
	private MyCheckboxConfig adminBolehMencicilkan;
	private MyCheckboxConfig dendaJikaTerlambat;
	private MyCheckboxConfig ditampilkanDiSuratTagihan;
	private MyDoublebox defaultProsentaseDenda;
	private MyCombobox jenisPembayaran;

	private MyGrid gridAkun;

	public ItemBiaya itemBiaya;
	private MyToolbarbuttonConfig add;

	private boolean edit;
	private boolean delete;

	private MyColumnConfig kode_akun;

	private MyCheckboxConfig menggunakanIstilahBayarAngsuran;

	private MyGrid gridPiutang;

	private MyGrid gridDiskon;

	private Intbox minSmt;

	private Intbox maxSmt;

	private MyGrid gridDibayarDimuka;

	private MyCheckboxConfig nilaiDendaDalamPersen;

	private MyIntbox dendaAkanBerlipatTerlambaHari;

	private MyIntbox maksimalBerlipatTerlambaHari;

	private MyGrid gridPendapatanDenda;

	private MyCheckboxConfig tidakDitagihDiSmtGanjil;

	private MyCheckboxConfig tidakDitagihDiSmtGenap;

	private MyCheckboxConfig tanggalTagihanMengikutiRencanaTahunAkademik;

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

		for (String s : ItemBiaya.PENGHITUNGAN_MAP.keySet()) {
			MyComboitemConfig comboitem = new MyComboitemConfig(ItemBiaya.PENGHITUNGAN_MAP.get(s));
			comboitem.setValue(ItemBiaya.PENGHITUNGAN_MAP.get(s));
			searchpenghitungan.appendChild(comboitem);
		}

		MyComboitemConfig comboitem = new MyComboitemConfig(ItemBiaya.TIDAK_ADA_PENGHITUNGAN);
		if (comboitem != null) { comboitem.setValue(ItemBiaya.TIDAK_ADA_PENGHITUNGAN); }
		searchpenghitungan.appendChild(comboitem);

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		}

		if (!Common.bolehKonfigurasi("integrasi_modul_akuntansi", Konfigurasi.TIDAK_AKTIF)) {
			kode_akun.setWidth("0px");
			// nama_akun.setWidth("0px");
		}

		if (add != null) { add.setTooltiptext("Tambah"); }
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "kode", "nama", "deskripsi", "penghitungan", "dendaJikaTerlambat",
				"nilaiDendaDalamPersen", "dendaAkanBerlipatTerlambaHari", "maksimalBerlipatTerlambaHari",
				"defaultProsentaseDenda", "menggunakanIstilahBayarAngsuran", "nilaiBisaDiubah",
				"ditampilkanDiSuratTagihan", "terhubungKeNilaiTambahan", "parameterTambahan",
				"mahasiswaBolehMencicilkan", "adminBolehMencicilkan", "minSmt", "maxSmt", "tidakDitagihDiSmtGanjil",
				"tidakDitagihDiSmtGenap", "tanggalTagihanMengikutiRencanaTahunAkademik", "aktif", "namaMatakuliah",
				"autoCreate", "jenisPembayaran" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(ItemBiaya.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, ItemBiaya.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class ItemBiayaRenderer extends ais.ui.util.MyRowRenderer {

		@SuppressWarnings("unchecked")
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final ItemBiaya itemBiaya = (ItemBiaya) arg1;

			RevisiHelper.createNewRevisi(ItemBiaya.class, itemBiaya, itemBiaya.getKode()).setParent(arg0);

			new Label(itemBiaya.getNama()).setParent(arg0);
			new Label(itemBiaya.getPenghitungan()).setParent(arg0);
			new Label(itemBiaya.getDeskripsi()).setParent(arg0);
			new Label(itemBiaya.getNilaiBisaDiubah() ? "Ya" : "Tidak").setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(itemBiaya.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					itemBiaya.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(itemBiaya);
				}
			});

			new Label(itemBiaya.getAutoCreate() ? "Ya" : "Tidak").setParent(arg0);

			Vbox vbox = new Vbox();
			Session session = HibernateUtil.currentSession();
			List<Akun> akuns = ConstantValues.simpleList(session.createCriteria(ItemBiayaPunyaAkun.class)
					.setProjection(Projections.groupProperty("akun.id")).add(Restrictions.eq("itemBiaya", itemBiaya)),
					Akun.class, false);

			String a = akuns.isEmpty() ? "" : "Akun pendapatan : ";
			String s = "";
			for (Akun akun : akuns) {
				s += s.isEmpty() ? akun.getKode() : ", " + akun.getKode();

			}
			vbox.appendChild(new MyLabelKecil(a + s));

			akuns = ConstantValues.simpleList(session.createCriteria(ItemBiayaPunyaDenda.class)
					.setProjection(Projections.groupProperty("akun.id")).add(Restrictions.eq("itemBiaya", itemBiaya)),
					Akun.class, false);
			a = akuns.isEmpty() ? "" : "Akun denda : ";
			s = "";
			for (Akun akun : akuns) {
				s += s.isEmpty() ? akun.getKode() : ", " + akun.getKode();
			}
			vbox.appendChild(new MyLabelKecil(a + s));

			akuns = ConstantValues.simpleList(session.createCriteria(ItemBiayaPunyaPiutang.class)
					.setProjection(Projections.groupProperty("akun.id")).add(Restrictions.eq("itemBiaya", itemBiaya)),
					Akun.class, false);
			a = akuns.isEmpty() ? "" : "Akun piutang : ";
			s = "";
			for (Akun akun : akuns) {
				s += s.isEmpty() ? akun.getKode() : ", " + akun.getKode();
			}
			vbox.appendChild(new MyLabelKecil(a + s));

			akuns = ConstantValues.simpleList(session.createCriteria(ItemBiayaPunyaDiskon.class)
					.setProjection(Projections.groupProperty("akun.id")).add(Restrictions.eq("itemBiaya", itemBiaya)),
					Akun.class, false);

			a = akuns.isEmpty() ? "" : "Akun diskon : ";
			s = "";
			for (Akun akun : akuns) {
				s += s.isEmpty() ? akun.getKode() : ", " + akun.getKode();
			}
			vbox.appendChild(new MyLabelKecil(a + s));

			akuns = ConstantValues.simpleList(session.createCriteria(ItemBiayaPunyaDibayarDimuka.class)
					.setProjection(Projections.groupProperty("akun.id")).add(Restrictions.eq("itemBiaya", itemBiaya)),
					Akun.class, false);

			a = akuns.isEmpty() ? "" : "Akun dibayar dimuka : ";
			s = "";
			for (Akun akun : akuns) {
				s += s.isEmpty() ? akun.getKode() : ", " + akun.getKode();
			}
			vbox.appendChild(new MyLabelKecil(a + s));

			vbox.setParent(arg0);

			Hbox hbox = new Hbox();
			hbox.setParent(arg0);

			final AmbilDataParameterTambahanBanbox parameterTambahan = new AmbilDataParameterTambahanBanbox();
			parameterTambahan.setWidth("120px");
			parameterTambahan.setReadonly(true);
			parameterTambahan.setAttribute("parameterTambahan", itemBiaya.getParameterTambahan());
			parameterTambahan.setValue(
					itemBiaya.getParameterTambahan() == null ? "" : itemBiaya.getParameterTambahan().getNama());

			final MyCheckboxConfig terhubungKeNilaiTambahan = new MyCheckboxConfig("");
			terhubungKeNilaiTambahan.setChecked(itemBiaya.getTerhubungKeNilaiTambahan());
			terhubungKeNilaiTambahan.setParent(hbox);
			parameterTambahan.setParent(hbox);
			terhubungKeNilaiTambahan.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					itemBiaya.setTerhubungKeNilaiTambahan(terhubungKeNilaiTambahan.isChecked());
					Common.refreshSaveOrUpdate(itemBiaya);
					parameterTambahan.setVisible(itemBiaya.getTerhubungKeNilaiTambahan());
				}
			});
			parameterTambahan.setVisible(itemBiaya.getTerhubungKeNilaiTambahan());
			parameterTambahan.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					itemBiaya.setParameterTambahan(
							(ParameterTambahan) (parameterTambahan.getAttribute("parameterTambahan")));
					Common.refreshSaveOrUpdate(itemBiaya);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, itemBiaya, ItemBiayaAction.this).setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new ItemBiaya());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		itemBiaya = (ItemBiaya) obj;
		init(itemBiaya);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(ItemBiaya itemBiaya) throws Exception {
		this.itemBiaya = itemBiaya;
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(addWindow);
		addWindow.setHeight("95%");
		East east = new East();
		east.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(east, true);
		east.setWidth("65%");
		if (east.isVisible()) {
			addWindow.setWidth("95%");
		}

		ais.ui.util.MyButtonTabbox btnTab = ais.ui.util.MyButtonTabbox.buat(east, "100%", new int[] { 0 });

		{ org.zkoss.zul.Div panel = btnTab.tambahTab(0, "Akun Pendapatan", "/img/svg/money-bills.svg");
		  panel.setStyle("min-height: 200px;");
		  panel.appendChild(new ItemBiayaPunyaAkunHelper(gridAkun = new MyGrid()).initDetail(itemBiaya)); }

		{ org.zkoss.zul.Div panel = btnTab.tambahTab(1, "Akun Pendapatan Denda", "/img/svg/coin.svg");
		  panel.setStyle("min-height: 200px;");
		  panel.appendChild(new ItemBiayaPunyaDenda(gridPendapatanDenda = new MyGrid()).initDetail(itemBiaya)); }

		{ org.zkoss.zul.Div panel = btnTab.tambahTab(2, "Akun Piutang", "/img/svg/credit-card.svg");
		  panel.setStyle("min-height: 200px;");
		  panel.appendChild(new ItemBiayaPunyaPiutangHelper(gridPiutang = new MyGrid()).initDetail(itemBiaya)); }

		{ org.zkoss.zul.Div panel = btnTab.tambahTab(3, "Akun Diskon", "/img/svg/percent.svg");
		  panel.setStyle("min-height: 200px;");
		  panel.appendChild(new ItemBiayaPunyaDiskonHelper(gridDiskon = new MyGrid()).initDetail(itemBiaya)); }

		{ org.zkoss.zul.Div panel = btnTab.tambahTab(4, "Akun Pendapatan Dibayar Dimuka", "/img/svg/journal-arrow-up.svg");
		  panel.setStyle("min-height: 200px;");
		  panel.appendChild(new ItemBiayaPunyaDibayarDimukaHelper(gridDibayarDimuka = new MyGrid()).initDetail(itemBiaya)); }

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

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
		row.appendChild(kode = new Textbox((itemBiaya.getKode() == null ? "" : itemBiaya.getKode().trim())));
		kode.setWidth("90%");
		// kode.setDisabled(Common.getCurrentUser().getRoot() == null ||
		// !Common.getCurrentUser().getRoot());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new Textbox(itemBiaya.getNama() == null ? "" : itemBiaya.getNama().trim()));
		nama.setWidth("90%");
		// nama.setDisabled(Common.getCurrentUser().getRoot() == null ||
		// !Common.getCurrentUser().getRoot());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Penghitungan"));
		row.appendChild(penghitungan = new MyCombobox());
		for (String s : ItemBiaya.PENGHITUNGAN_MAP.keySet()) {
			MyComboitemConfig comboitem = new MyComboitemConfig(ItemBiaya.PENGHITUNGAN_MAP.get(s));
			comboitem.setValue(ItemBiaya.PENGHITUNGAN_MAP.get(s));
			penghitungan.appendChild(comboitem);
		}

		MyComboitemConfig comboitem = new MyComboitemConfig(ItemBiaya.TIDAK_ADA_PENGHITUNGAN);
		comboitem.setValue(ItemBiaya.TIDAK_ADA_PENGHITUNGAN);
		penghitungan.appendChild(comboitem);

		Common.selectComboItem(penghitungan, itemBiaya.getPenghitungan());
		penghitungan.setWidth("90%");
		penghitungan.setReadonly(true);

		final MyFormRow rowNamaMatakuliah = new MyFormRow();
		rowNamaMatakuliah.setStyle("border:0px;background: transparent;");
		rowNamaMatakuliah.setParent(rows);
		rowNamaMatakuliah.appendChild(new ais.ui.util.MyLabelConfig("Kode atau Nama Matakuliah"));
		rowNamaMatakuliah.appendChild(namaMatakuliah = new Textbox(itemBiaya.getNamaMatakuliah()));
		namaMatakuliah.setWidth("90%");

		final Row rowKet = Common.initKeterangan(rows,
				"Pisahkan dengan tanda semikolon (;) jika terdapat banyak nama atau kode matakuliah");

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				String p = (String) (penghitungan.getSelectedItem() == null ? ItemBiaya.TIDAK_ADA_PENGHITUNGAN
						: penghitungan.getSelectedItem().getValue());
				rowNamaMatakuliah.setVisible(p.equals(ItemBiaya.DIKALI_SATU_JIKA_AMBIL_MK_TERTENTU)
						|| p.equals(ItemBiaya.DIKALI_SATU_JIKA_AMBIL_MK_TERTENTU_DAN_SEMESTER_SEBELUMNYA));
				rowKet.setVisible(p.equals(ItemBiaya.DIKALI_SATU_JIKA_AMBIL_MK_TERTENTU)
						|| p.equals(ItemBiaya.DIKALI_SATU_JIKA_AMBIL_MK_TERTENTU_DAN_SEMESTER_SEBELUMNYA));
			}
		};

		penghitungan.addEventListener("onChange", eventListener);
		eventListener.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Deskripsi"));
		row.appendChild(deskripsi = new Textbox(itemBiaya.getDeskripsi() == null ? "" : itemBiaya.getDeskripsi()));
		deskripsi.setWidth("90%");
		deskripsi.setRows(5);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(nilaiBisaDiubah = new MyCheckboxConfig("Pada saat pembayaran nilai bisa di-ubah"));
		nilaiBisaDiubah.setChecked(itemBiaya.getNilaiBisaDiubah());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(mahasiswaBolehMencicilkan = new MyCheckboxConfig("Boleh diangsur oleh mahasiswa"));
		mahasiswaBolehMencicilkan.setChecked(itemBiaya.getMahasiswaBolehMencicilkan());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(adminBolehMencicilkan = new MyCheckboxConfig("Boleh diangsur oleh admin / keuangan"));
		adminBolehMencicilkan.setChecked(itemBiaya.getAdminBolehMencicilkan());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(ditampilkanDiSuratTagihan = new MyCheckboxConfig("Ditampilkan di surat tagihan"));
		ditampilkanDiSuratTagihan.setChecked(itemBiaya.getDitampilkanDiSuratTagihan());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(dendaJikaTerlambat = new MyCheckboxConfig("Dikenakan denda jika terlambat membayar"));
		dendaJikaTerlambat.setChecked(itemBiaya.getDendaJikaTerlambat());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(nilaiDendaDalamPersen = new MyCheckboxConfig(
				"Denda dalam persen (jika tidak dipilih dalam nilai fix)"));
		nilaiDendaDalamPersen.setChecked(itemBiaya.getNilaiDendaDalamPersen());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Denda akan berlipat jika terlambat dalam hari"));
		row.appendChild(dendaAkanBerlipatTerlambaHari = new MyIntbox(itemBiaya.getDendaAkanBerlipatTerlambaHari()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Maksimal jumlah kelipatan"));
		row.appendChild(maksimalBerlipatTerlambaHari = new MyIntbox(itemBiaya.getMaksimalBerlipatTerlambaHari()));

		final MyFormRow rowDenda = new MyFormRow();
		rowDenda.setStyle("border:0px;background: transparent;");
		rowDenda.setParent(rows);
		rowDenda.appendChild(new ais.ui.util.MyLabelConfig("Nilai Denda"));
		rowDenda.appendChild(defaultProsentaseDenda = new MyDoublebox(itemBiaya.getDefaultProsentaseDenda()));

		EventListener eventListenerDenda = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				rowDenda.setVisible(dendaJikaTerlambat.isChecked());
				nilaiDendaDalamPersen.getParent().setVisible(dendaJikaTerlambat.isChecked());

				dendaAkanBerlipatTerlambaHari.getParent().setVisible(dendaJikaTerlambat.isChecked());
				maksimalBerlipatTerlambaHari.getParent().setVisible(dendaJikaTerlambat.isChecked());
			}
		};

		dendaJikaTerlambat.addEventListener("onClick", eventListenerDenda);
		eventListenerDenda.onEvent(null);

		if (itemBiaya.getAutoCreate()) {
			kode.setDisabled(true);
			penghitungan.setDisabled(true);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Minimal Semester"));
		row.appendChild(minSmt = new Intbox(itemBiaya.getMinSmt()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Maksimal Semester"));
		row.appendChild(maxSmt = new Intbox(itemBiaya.getMaxSmt()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(tidakDitagihDiSmtGanjil = new MyCheckboxConfig("Tidak ditagih di semester ganjil"));
		tidakDitagihDiSmtGanjil.setChecked(itemBiaya.getTidakDitagihDiSmtGanjil());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(tidakDitagihDiSmtGenap = new MyCheckboxConfig("Tidak ditagih di semester genap"));
		tidakDitagihDiSmtGenap.setChecked(itemBiaya.getTidakDitagihDiSmtGenap());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(tanggalTagihanMengikutiRencanaTahunAkademik = new MyCheckboxConfig(
				"Tanggal Tagihan Mengikuti Rencana Tahun Akademik atau Gelombang Pendaftaran"));
		tanggalTagihanMengikutiRencanaTahunAkademik
				.setChecked(itemBiaya.getTanggalTagihanMengikutiRencanaTahunAkademik());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(menggunakanIstilahBayarAngsuran = new MyCheckboxConfig(
				"Menggunakan istilah angsuran unutk tagihan bulanan, contoh : Angsuran 1, Angsuran 2, dan seterusnya"));
		menggunakanIstilahBayarAngsuran.setChecked(itemBiaya.getMenggunakanIstilahBayarAngsuran());

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pembayaran"));
		row.appendChild(jenisPembayaran = new MyCombobox());
		jenisPembayaran.setWidth("90%");
		SatuanKerja satuanKerja = Common.getSatuanKerja();
		Common.insertCombo(jenisPembayaran, "nama", "akun", JenisPembayaran.class,
				Restrictions.and(
						satuanKerja == null ? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.isNull("satuanKerja"),
										Restrictions.eq("satuanKerja", satuanKerja)),
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
		Common.selectComboItem(jenisPembayaran, itemBiaya.getJenisPembayaran());

//		Common.initKeterangan(rows, "Item biaya ini akan dibayar secara default menggunakan jenis pembayaran ini");

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
				try {
					if (onSave(event)) {
						onSearchDefault(null);
						addWindow.setVisible(false);
					}
				} catch (Exception e) {
					// PERMINTAAN: setiap error saat proses Simpan WAJIB diinformasikan ke pengguna
					// secara jelas (bukan dibiarkan lolos jadi halaman error ZK generik yang
					// membingungkan), lengkap dengan saran langkah dan eskalasi ke admin/pengembang
					// (wajib lampirkan screenshot) -- lihat PesanFormalHelper.eskalasi().
					ais.common.ErrorAuditUtil.record(e,
							"auto-audit src/ais/action/master/ItemBiayaAction.java:onClick-Simpan");
					ais.common.PesanFormalHelper.tampilkanGagalException("penyimpanan data Item Biaya", e,
							new String[] {
									"Periksa kembali seluruh isian pada form ini (Kode, Nama, Akun Pendapatan/Piutang/Diskon, dsb).",
									"Pastikan koneksi jaringan Bapak/Ibu stabil, lalu ulangi proses Simpan.",
									"Bila data sudah benar namun kesalahan tetap terjadi, kemungkinan ada kendala pada sistem/basis data."
							});
				}
			}
		});
		save.setParent(toolbar);

	}

	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {
		if (kode.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Kode",
					"Kolom Kode belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Kode.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (nama.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Nama",
					"Kolom Nama belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		// Muat ulang dgn get() (bukan load()) SEBELUM checkKode()/checkNama() memakai
		// itemBiaya: load() menghasilkan lazy proxy yang baru gagal (ObjectNotFoundException)
		// saat properti diakses nanti kalau baris sudah dihapus dari DB sementara form/grid
		// masih memegang referensi lama (mis. ItemBiaya#229). get() langsung kena DB dan
		// null bila sudah tak ada, sehingga bisa dideteksi di sini tanpa crash.
		if (itemBiaya.getId() != null) {
			ItemBiaya itemBiayaDariDb = (ItemBiaya) HibernateUtil.currentSession().get(ItemBiaya.class,
					itemBiaya.getId());
			if (itemBiayaDariDb == null) {
				MyMessageboxConfig.show(
						"Item Biaya ini sudah tidak ada di database (mungkin telah dihapus). Silakan tutup form ini lalu buka kembali.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return false;
			}
			itemBiaya = itemBiayaDariDb;
		}

		boolean i = checkKode();
		if (i) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Kode Item",
					"Kode Item sudah terdaftar sebelumnya di database, sehingga tidak dapat disimpan kembali untuk menghindari duplikasi data.",
					new String[] {
							"Gunakan Kode Item yang berbeda dari data yang sudah ada.",
							"Periksa kembali daftar data yang sudah tersimpan apabila Bapak/Ibu ragu."
					});
			return false;
		}

		i = checkNama();
		if (i) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Item",
					"Nama Item sudah terdaftar sebelumnya di database, sehingga tidak dapat disimpan kembali untuk menghindari duplikasi data.",
					new String[] {
							"Gunakan nama item yang berbeda dari data yang sudah ada.",
							"Periksa kembali daftar data yang sudah tersimpan apabila Bapak/Ibu ragu."
					});
			return false;
		}

		List<Row> rowsAkun = gridAkun.getRows().getChildren();
		for (Row row : rowsAkun) {
			if (row.isVisible()) {
				ItemBiayaPunyaAkun itemBiayaPunyaAkun = (ItemBiayaPunyaAkun) row.getAttribute("itemBiayaPunyaAkun");
				if (itemBiayaPunyaAkun.getAkun() == null) {
					PesanFormalHelper.tampilkanGagal("penyimpanan data Akun",
							"Kolom Akun belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
							new String[] {
									"Isi/pilih terlebih dahulu Akun.",
									"Ulangi proses penyimpanan setelah kolom tersebut terisi."
							});
					return false;
				}
			}
		}

		List<Row> rowsPiutang = gridPiutang.getRows().getChildren();
		for (Row row : rowsPiutang) {
			if (row.isVisible()) {
				ItemBiayaPunyaPiutang itemBiayaPunyaPiutang = (ItemBiayaPunyaPiutang) row
						.getAttribute("itemBiayaPunyaPiutang");
				if (itemBiayaPunyaPiutang.getAkun() == null) {
					PesanFormalHelper.tampilkanGagal("penyimpanan data Piutang",
							"Kolom Piutang belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
							new String[] {
									"Isi/pilih terlebih dahulu Piutang.",
									"Ulangi proses penyimpanan setelah kolom tersebut terisi."
							});
					return false;
				}
			}
		}

		List<Row> rowsDiskon = gridDiskon.getRows().getChildren();
		for (Row row : rowsDiskon) {
			if (row.isVisible()) {
				ItemBiayaPunyaDiskon itemBiayaPunyaDiskon = (ItemBiayaPunyaDiskon) row
						.getAttribute("itemBiayaPunyaDiskon");
				if (itemBiayaPunyaDiskon.getAkun() == null) {
					PesanFormalHelper.tampilkanGagal("penyimpanan data Diskon",
							"Kolom Diskon belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
							new String[] {
									"Isi/pilih terlebih dahulu Diskon.",
									"Ulangi proses penyimpanan setelah kolom tersebut terisi."
							});
					return false;
				}
			}
		}

		List<Row> rowsDibayarDimuka = gridDibayarDimuka.getRows().getChildren();
		for (Row row : rowsDibayarDimuka) {
			if (row.isVisible()) {
				ItemBiayaPunyaDibayarDimuka itemBiayaPunyaDibayarDimuka = (ItemBiayaPunyaDibayarDimuka) row
						.getAttribute("itemBiayaPunyaDibayarDimuka");
				if (itemBiayaPunyaDibayarDimuka.getAkun() == null) {
					PesanFormalHelper.tampilkanGagal("penyimpanan data Dibayar Dimuka",
							"Kolom Dibayar Dimuka belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
							new String[] {
									"Isi/pilih terlebih dahulu Dibayar Dimuka.",
									"Ulangi proses penyimpanan setelah kolom tersebut terisi."
							});
					return false;
				}
			}
		}

		List<Row> rowsPendapatanDenda = gridPendapatanDenda.getRows().getChildren();
		for (Row row : rowsPendapatanDenda) {
			if (row.isVisible()) {
				ItemBiayaPunyaPendapatanDenda itemBiayaPunyaPendapatanDenda = (ItemBiayaPunyaPendapatanDenda) row
						.getAttribute("itemBiayaPunyaPendapatanDenda");
				if (itemBiayaPunyaPendapatanDenda.getAkun() == null) {
					PesanFormalHelper.tampilkanGagal("penyimpanan data Pendapatan Denda",
							"Kolom Pendapatan Denda belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
							new String[] {
									"Isi/pilih terlebih dahulu Pendapatan Denda.",
									"Ulangi proses penyimpanan setelah kolom tersebut terisi."
							});
					return false;
				}
			}
		}

		// itemBiaya sudah dimuat ulang secara aman (get(), bukan proxy load()) di atas,
		// sebelum checkKode()/checkNama() — tidak perlu reload proxy di sini lagi.
		itemBiaya.setNilaiBisaDiubah(nilaiBisaDiubah.isChecked());
		itemBiaya.setKode(kode.getValue());
		itemBiaya.setNama(nama.getValue());
		itemBiaya.setDeskripsi(deskripsi.getValue());
		itemBiaya.setPenghitungan((String) penghitungan.getSelectedItem().getValue());
		itemBiaya.setDendaJikaTerlambat(dendaJikaTerlambat.isChecked());
		itemBiaya.setDefaultProsentaseDenda(defaultProsentaseDenda.getValue());
		itemBiaya.setNamaMatakuliah(namaMatakuliah.getValue());
		itemBiaya.setJenisPembayaran((JenisPembayaran) (jenisPembayaran.getSelectedItem() == null ? null
				: jenisPembayaran.getSelectedItem().getValue()));
		itemBiaya.setDitampilkanDiSuratTagihan(ditampilkanDiSuratTagihan.isChecked());
		itemBiaya.setMenggunakanIstilahBayarAngsuran(menggunakanIstilahBayarAngsuran.isChecked());
		itemBiaya.setAdminBolehMencicilkan(adminBolehMencicilkan.isChecked());
		itemBiaya.setMahasiswaBolehMencicilkan(mahasiswaBolehMencicilkan.isChecked());

		itemBiaya.setMinSmt(minSmt.getValue());
		itemBiaya.setMaxSmt(maxSmt.getValue());

		itemBiaya.setNilaiDendaDalamPersen(nilaiDendaDalamPersen.isChecked());
		itemBiaya.setDendaAkanBerlipatTerlambaHari(dendaAkanBerlipatTerlambaHari.getValue());
		itemBiaya.setMaksimalBerlipatTerlambaHari(maksimalBerlipatTerlambaHari.getValue());

		itemBiaya.setTidakDitagihDiSmtGanjil(tidakDitagihDiSmtGanjil.isChecked());
		itemBiaya.setTidakDitagihDiSmtGenap(tidakDitagihDiSmtGenap.isChecked());

		itemBiaya.setTanggalTagihanMengikutiRencanaTahunAkademik(
				tanggalTagihanMengikutiRencanaTahunAkademik.isChecked());

		Common.refreshSaveOrUpdate(itemBiaya);

		// PERBAIKAN (Item Biaya baru tidak muncul di checklist "Tambah Jenis Biaya" pada
		// layar Setting Biaya): ConstantValues.simpleList() yang dipakai layar itu mencari ID
		// LANGSUNG dari database (selalu terbaru), TAPI mengambil ISI datanya lewat cache
		// in-JVM MemoryCacheUtil (lihat ConstantValues.ambilBanyak -> MemoryCacheUtil.get).
		// refreshSaveOrUpdate() di atas HANYA menyimpan ke database, TIDAK memperbarui cache
		// itu -- persis kelalaian yang didokumentasikan sbg akar bug serupa di javadoc
		// MemoryCacheUtil ("skor 0 / data lama" pada modul ujian). Tanpa baris ini, Item
        // Biaya yang baru disimpan tetap ada di database tapi "hilang" dari cache sampai
        // cache di-refresh oleh proses lain (mis. restart aplikasi) -- panggil
        // DataUtil.masukkanData agar cache ikut diperbarui SEKARANG, saat itu juga.
		ais.common.DataUtil.masukkanData(ItemBiaya.class, itemBiaya);

		Session session = HibernateUtil.currentSession();
		for (Row row : rowsAkun) {
			if (row.isVisible()) {
				ItemBiayaPunyaAkun itemBiayaPunyaAkun = (ItemBiayaPunyaAkun) row.getAttribute("itemBiayaPunyaAkun");
				itemBiayaPunyaAkun.setItemBiaya(itemBiaya);
				session.saveOrUpdate(itemBiayaPunyaAkun);
			}
		}

		for (Row row : rowsPiutang) {
			if (row.isVisible()) {
				ItemBiayaPunyaPiutang itemBiayaPunyaPiutang = (ItemBiayaPunyaPiutang) row
						.getAttribute("itemBiayaPunyaPiutang");
				itemBiayaPunyaPiutang.setItemBiaya(itemBiaya);
				session.saveOrUpdate(itemBiayaPunyaPiutang);
			}
		}

		for (Row row : rowsDiskon) {
			if (row.isVisible()) {
				ItemBiayaPunyaDiskon itemBiayaPunyaDiskon = (ItemBiayaPunyaDiskon) row
						.getAttribute("itemBiayaPunyaDiskon");
				itemBiayaPunyaDiskon.setItemBiaya(itemBiaya);
				session.saveOrUpdate(itemBiayaPunyaDiskon);
			}
		}

		for (Row row : rowsDibayarDimuka) {
			if (row.isVisible()) {
				ItemBiayaPunyaDibayarDimuka itemBiayaPunyaDibayarDimuka = (ItemBiayaPunyaDibayarDimuka) row
						.getAttribute("itemBiayaPunyaDibayarDimuka");
				itemBiayaPunyaDibayarDimuka.setItemBiaya(itemBiaya);
				session.saveOrUpdate(itemBiayaPunyaDibayarDimuka);
			}
		}

		for (Row row : rowsPendapatanDenda) {
			if (row.isVisible()) {
				ItemBiayaPunyaPendapatanDenda itemBiayaPunyaPendapatanDenda = (ItemBiayaPunyaPendapatanDenda) row
						.getAttribute("itemBiayaPunyaPendapatanDenda");
				itemBiayaPunyaPendapatanDenda.setItemBiaya(itemBiaya);
				session.saveOrUpdate(itemBiayaPunyaPendapatanDenda);
			}
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(ItemBiaya.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))

				.add(searchpenghitungan.getSelectedItem() != null
						? Restrictions.eq("penghitungan", searchpenghitungan.getSelectedItem().getValue())
						: Restrictions.sqlRestriction("true"))

				.add(serachkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("kode", serachkode.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		if (order)
			criteria.addOrder(Order.asc("kode"));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<ItemBiaya> itemBiaya = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(itemBiaya);
		grid.setRowRenderer(new ItemBiayaRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkKode() {

		// Ambil ID secara aman: this.itemBiaya bisa null, atau proxy yang sudah lepas
		// dari sesi (baris terhapus) bisa melempar exception saat getId() diakses.
		Long idAman = null;
		if (this.itemBiaya != null) {
			try {
				idAman = this.itemBiaya.getId();
			} catch (Exception e) {
				idAman = null;
			}
		}

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(ItemBiaya.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("kode", kode.getValue().trim()))
				.add(idAman == null ? Restrictions.sqlRestriction("1=1") : Restrictions.ne("id", idAman))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

	public Boolean checkNama() {

		// Sama seperti checkKode(): ambil ID secara aman terhadap itemBiaya null/proxy stale.
		Long idAman = null;
		if (this.itemBiaya != null) {
			try {
				idAman = this.itemBiaya.getId();
			} catch (Exception e) {
				idAman = null;
			}
		}

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(ItemBiaya.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(idAman == null ? Restrictions.sqlRestriction("1=1") : Restrictions.ne("id", idAman))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
