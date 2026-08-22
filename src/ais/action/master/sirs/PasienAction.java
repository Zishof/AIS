package ais.action.master.sirs;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
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
import org.zkoss.zul.Caption;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Window;

import ais.action.master.helper.AmbilDataKecamatanBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.sirs.helper.AmbilDataAsuransiBanbox;
import ais.action.master.sirs.helper.AmbilDataKelurahanBanbox;
import ais.action.master.sirs.helper.AmbilDataKotaBanbox;
import ais.action.master.sirs.helper.AmbilDataSatkerBanbox;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.CommonSirs;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Agama;
import ais.database.model.Kota;
import ais.database.model.Propinsi;
import ais.database.model.employ.Pendidikan;
import ais.database.model.sirs.Asuransi;
import ais.database.model.sirs.JenisPasien;
import ais.database.model.sirs.Kecamatan;
import ais.database.model.sirs.Kelurahan;
import ais.database.model.sirs.Pasien;
import ais.database.model.sirs.PrioritasPasien;
import ais.database.model.sirs.Satker;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;

public class PasienAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Window addWindow;
	private Window window;
	private Tabpanel tambahData;
	private Grid grid;
	private Paging paging;

	private MyTextbox searchkode;
	private MyTextbox searchnama;
	private MyTextbox searchalamat;
	private MyTextbox searchtelp;

	private MyTextbox nama;
	private Combobox pekerjaan;
	private MyTextbox kode;
	private MyTextbox keterangan;
	private Combobox propinsi;
	private AmbilDataKotaBanbox kota;
	private AmbilDataKecamatanBanbox kecamatan;
	private AmbilDataKelurahanBanbox kelurahan;
	private AmbilDataAsuransiBanbox asuransi;
	private MyTextbox alamat;
	private MyTextbox rt;
	private MyTextbox rw;
	private Combobox jenisPasien;
	private Combobox statusPerkawinan;
	private Combobox jenisKelamin;
	private MyDatebox tanggalRegistrasi;
	private MyDatebox tanggalLahir;
	private MyTextbox tempatLahir;
	private Combobox agama;
	private Combobox pendidikan;
	private MyTextbox noTelp;
	private MyTextbox noHp;
	private Combobox kewarganegaraan;
	private Combobox prioritasPasien;

	private MyTextbox nama_penanggungjawab;
	private Combobox jenis_penanggungjawab;
	private MyTextbox nip;
	private MyTextbox pangkat;
	private MyTextbox golongan;
	private Combobox jenis_pasien_dinas;
	private MyTextbox sebutkan_non_kemhan;
	private AmbilDataSatkerBanbox satker;

	private boolean edit = false;
	private boolean delete = false;

	private Pasien pasien;
	private Toolbarbutton add;

	private EventListener externalCalled;
	private MyTextbox telp_penanggungjawab;
	private MyTextbox hp_penanggungjawab;
	private Checkbox aktif;

	private Boolean editRMManual = false;

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			execution.sendRedirect("/logoff");
			return;
		}

		if (tambahData != null) {
			add = new ais.ui.util.MyToolbarbuttonConfig("Pasien Baru", "/img/user_male_add.png");
			add.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(new Pasien());
				}
			});
			init(new Pasien());
		}

		editRMManual = (Boolean) (window.getAttribute("editRMManual") == null ? false
				: window.getAttribute("editRMManual"));

		System.out.println("editRMManual = " + editRMManual);

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
	}

	class PasienRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final Pasien pasien = (Pasien) arg1;

			if (pasien.getAktif() == null || !pasien.getAktif()) {
				arg0.setStyle("background-color:red;");
			}
			new Label(pasien.getKode()).setParent(arg0);

			RevisiHelper.createNewRevisi(Pasien.class, pasien, pasien.getNama()).setParent(arg0);
			new Label(pasien.getTanggalRegistrasi() == null ? ""
					: Common.dateFormat3.get().format(pasien.getTanggalRegistrasi())).setParent(arg0);
			new Label(pasien.getNoTelp() + " / " + pasien.getNoHp()).setParent(arg0);
			new Label(pasien.getJenisPasien() == null ? "" : pasien.getJenisPasien().getNama()).setParent(arg0);

			new Label(pasien.getAlamatLengkap()).setParent(arg0);
			new Label(pasien.getSatker() == null ? "" : pasien.getSatker().getNama()).setParent(arg0);
			new Label(pasien.getAsuransi() == null ? "" : pasien.getAsuransi().getNama()).setParent(arg0);

			new Label(pasien.getAktif() == null || !pasien.getAktif() ? "Tidak" : "Ya").setParent(arg0);

			Hbox toolbar = new Hbox();
			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/user_male_add.png");
			button.setTooltiptext("Copy Data Pasien menggunakan Kode baru");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					Pasien myPasien = (Pasien) pasien.clone();
					myPasien.setId(null);
					myPasien.setKode(null);
					init(myPasien);
				}

			});
			button.setParent(toolbar);

			button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/edit.gif");
			button.setTooltiptext("Rubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(pasien);
				}

			});
			button.setParent(toolbar);

			button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					onDelete(pasien);

				}
			});
			button.setParent(toolbar);
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(arg0);
		}

	}

	public void onDelete(final Pasien pasien) throws Exception {
		MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus data pasien ini? Data yang telah dihapus tidak dapat dikembalikan.", "Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
				MyMessageboxConfig.QUESTION, new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {
						int i = new Integer(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							try {
								Common.refreshDelete(pasien);
								onSearchDefault(event);
							} catch (Exception e) {
								ais.common.Common.tampilErrorJikaAdmin(e);
								MyMessageboxConfig.show(Common.pesan(
										"Mohon maaf, data pasien ini tidak dapat dihapus karena masih berelasi dengan data lain. Rincian teknis: {V1}. Langkah yang dapat dilakukan: (1) hapus terlebih dahulu data lain yang berkaitan dengan data ini; (2) pastikan data tidak sedang digunakan pada transaksi lain; (3) hubungi administrator apabila kendala masih berlanjut.",
										e.getMessage()));
							}

						}

					}
				});
	}

	public void onAdd(Event event) throws Exception {
		init(new Pasien());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	public static Window onExternalAdd(EventListener listener) throws Exception {
		PasienAction pasienAction = new PasienAction();
		pasienAction.externalCalled = listener;
		pasienAction.addWindow = new Window();
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(pasienAction.addWindow);
		pasienAction.init(new Pasien());

		pasienAction.addWindow.setWidth("750px");
		pasienAction.addWindow.setHeight("95%");
		pasienAction.addWindow.setTitle("Tambah Pasien Baru");
		pasienAction.addWindow.setVisible(true);
		pasienAction.addWindow.onModal();

		if (pasienAction.jenisPasien.getItemCount() > 0) {
			pasienAction.jenisPasien.setSelectedIndex(0);
		}
		pasienAction.generateCode();

		return pasienAction.addWindow;
	}

	private void initComponent() {
		statusPerkawinan = new Combobox();
		Comboitem comboitem = new Comboitem("Belum Menikah");
		comboitem.setValue("Belum Menikah");
		statusPerkawinan.appendChild(comboitem);
		comboitem = new Comboitem("Menikah");
		comboitem.setValue("Menikah");
		statusPerkawinan.appendChild(comboitem);
		comboitem = new Comboitem("Duda");
		comboitem.setValue("Duda");
		statusPerkawinan.appendChild(comboitem);
		comboitem = new Comboitem("Janda");
		comboitem.setValue("Janda");
		statusPerkawinan.appendChild(comboitem);

		pekerjaan = Common.initPekerjaan(pekerjaan);

		jenisKelamin = new Combobox();
		comboitem = new Comboitem("Laki-laki");
		comboitem.setValue("Laki-laki");
		jenisKelamin.appendChild(comboitem);
		comboitem = new Comboitem("Perempuan");
		comboitem.setValue("Perempuan");
		jenisKelamin.appendChild(comboitem);

		kewarganegaraan = new Combobox();
		comboitem = new Comboitem("WNI");
		comboitem.setValue("WNI");
		kewarganegaraan.appendChild(comboitem);
		comboitem = new Comboitem("WNA");
		comboitem.setValue("WNA");
		kewarganegaraan.appendChild(comboitem);

		jenis_penanggungjawab = new Combobox();
		comboitem = new Comboitem("Ayah");
		comboitem.setValue("Ayah");
		jenis_penanggungjawab.appendChild(comboitem);
		comboitem = new Comboitem("Ibu");
		comboitem.setValue("Ibu");
		jenis_penanggungjawab.appendChild(comboitem);
		comboitem = new Comboitem("Suami");
		comboitem.setValue("Suami");
		jenis_penanggungjawab.appendChild(comboitem);
		comboitem = new Comboitem("Istri");
		comboitem.setValue("Istri");
		jenis_penanggungjawab.appendChild(comboitem);
		comboitem = new Comboitem("Adik");
		comboitem.setValue("Adik");
		jenis_penanggungjawab.appendChild(comboitem);
		comboitem = new Comboitem("Kakak");
		comboitem.setValue("Kakak");
		jenis_penanggungjawab.appendChild(comboitem);
		comboitem = new Comboitem("Anak");
		comboitem.setValue("Anak");
		jenis_penanggungjawab.appendChild(comboitem);
		comboitem = new Comboitem("Tetangga");
		comboitem.setValue("Tetangga");
		jenis_penanggungjawab.appendChild(comboitem);
		comboitem = new Comboitem("Teman");
		comboitem.setValue("Teman");
		jenis_penanggungjawab.appendChild(comboitem);
		comboitem = new Comboitem("Lain-lain");
		comboitem.setValue("Lain-lain");
		jenis_penanggungjawab.appendChild(comboitem);

		jenis_pasien_dinas = new Combobox();
		comboitem = new Comboitem(Pasien.TNI_AD.getName());
		comboitem.setValue(Pasien.TNI_AD.getId());
		jenis_pasien_dinas.appendChild(comboitem);
		comboitem = new Comboitem(Pasien.TNI_AL.getName());
		comboitem.setValue(Pasien.TNI_AL.getId());
		jenis_pasien_dinas.appendChild(comboitem);
		comboitem = new Comboitem(Pasien.TNI_AU.getName());
		comboitem.setValue(Pasien.TNI_AU.getId());
		jenis_pasien_dinas.appendChild(comboitem);
		comboitem = new Comboitem(Pasien.PNS.getName());
		comboitem.setValue(Pasien.PNS.getId());
		jenis_pasien_dinas.appendChild(comboitem);

		Common.insertCombo(prioritasPasien = new Combobox(), "nama", PrioritasPasien.class);
		Common.insertCombo(pendidikan = new Combobox(), "nama", Pendidikan.class);
		Common.insertCombo(agama = new Combobox(), "nama", Agama.class);
		Common.insertCombo(jenisPasien = new Combobox(), "nama", JenisPasien.class);
		Common.insertCombo(propinsi = new Combobox(), "nama", Propinsi.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		prioritasPasien.setReadonly(true);
		pendidikan.setReadonly(true);
		agama.setReadonly(true);
		jenisPasien.setReadonly(true);
		propinsi.setReadonly(true);

		jenis_pasien_dinas.setReadonly(true);
		jenis_penanggungjawab.setReadonly(true);
		kewarganegaraan.setReadonly(true);
		jenisKelamin.setReadonly(true);
		pekerjaan.setReadonly(true);
		statusPerkawinan.setReadonly(true);
		// kota = new Combobox();
		// kecamatan = new Combobox();
		// kelurahan = new Combobox();
		//
		// propinsi.addEventListener(Events.ON_CHANGE, new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// Common.clear(kota);
		// Propinsi propinsi = (Propinsi) (PasienAction.this.propinsi
		// .getSelectedItem() == null ? null
		// : PasienAction.this.propinsi.getSelectedItem()
		// .getValue());
		// if (propinsi == null) {
		// return;
		// }
		// Common.insertCombo(kota, "nama", Kota.class,
		// Restrictions.eq("propinsi", propinsi));
		// }
		// });
		//
		// kota.addEventListener(Events.ON_CHANGE, new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// Common.clear(kecamatan);
		// Kota kota = (Kota) (PasienAction.this.kota.getSelectedItem() == null
		// ? null
		// : PasienAction.this.kota.getSelectedItem().getValue());
		// if (kota == null) {
		// return;
		// }
		// Common.insertCombo(kecamatan, "nama", Kecamatan.class,
		// Restrictions.eq("kota", kota));
		// }
		// });
		//
		// kecamatan.addEventListener(Events.ON_CHANGE, new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// Common.clear(kelurahan);
		// Kecamatan kecamatan = (Kecamatan) (PasienAction.this.kecamatan
		// .getSelectedItem() == null ? null
		// : PasienAction.this.kecamatan.getSelectedItem()
		// .getValue());
		// if (kecamatan == null) {
		// return;
		// }
		// Common.insertCombo(kelurahan, "nama", Kelurahan.class,
		// Restrictions.eq("kecamatan", kecamatan));
		// }
		// });
	}

	private Groupbox createKode(final Pasien pasien) {

		Groupbox groupbox = new Groupbox();

		groupbox.appendChild(new Caption("Data Pasien"));

		Grid grid = new Grid();
		grid.setStyle("border:0px;background: transparent;");
		grid.setParent(groupbox);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		Column column = new Column();
		column.setWidth("100px");
		column.setParent(columns);
		column = new Column();
		// column.setWidth("100px");
		column.setParent(columns);
		column = new Column();
		column.setWidth("100px");
		column.setParent(columns);
		column = new Column();
		// column.setWidth("100px");
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		Row row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("MR")));
		String mykode = pasien.getKode();
		row.appendChild(kode = new MyTextbox(mykode));
		kode.setWidth("90%");

		editRMManual = (Boolean) (window == null || window.getAttribute("editRMManual") == null ? false
				: window.getAttribute("editRMManual"));
		kode.setReadonly(!editRMManual);
		System.out.println("!editRMManual = " + (!editRMManual));

		// row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Pasien")));
		row.appendChild(jenisPasien);
		Common.selectComboItem(jenisPasien, pasien.getJenisPasien());
		jenisPasien.setWidth("90%");
		jenisPasien.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				generateCode();
			}
		});

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama Pasien")));
		row.appendChild(nama = new MyTextbox(pasien.getNama() == null ? "" : pasien.getNama()));
		nama.setWidth("90%");

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Waktu Registrasi")));
		row.appendChild(tanggalRegistrasi = new MyDatebox(pasien.getTanggalRegistrasi()));
		tanggalRegistrasi.setFormat(Common.dateFormat3.get().toPattern());
		tanggalRegistrasi.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Aktif")));
		row.appendChild(aktif = new Checkbox());
		aktif.setChecked(pasien.getAktif() == null ? false : pasien.getAktif());

		generateCode();
		return groupbox;
	}

	private Groupbox createDinas(final Pasien pasien) {

		Groupbox groupbox = new Groupbox();

		groupbox.appendChild(new Caption("Pasien Dinas"));

		Grid grid = new Grid();
		grid.setStyle("border:0px;background: transparent;");
		grid.setParent(groupbox);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		Column column = new Column();
		column.setWidth("100px");
		column.setParent(columns);
		column = new Column();
		// column.setWidth("100px");
		column.setParent(columns);
		column = new Column();
		column.setWidth("100px");
		column.setParent(columns);
		column = new Column();
		// column.setWidth("100px");
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		Row row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label("NRP / NIP (Dinas)"));
		row.appendChild(nip = new MyTextbox(pasien.getNip() == null ? "" : pasien.getNip()));
		nip.setWidth("90%");

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Satker")));
		row.appendChild(satker = new AmbilDataSatkerBanbox());
		satker.setAttribute("satker", pasien.getSatker());
		satker.setValue(pasien.getSatker() == null ? "" : pasien.getSatker().getNama());
		satker.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Pangkat (Dinas)")));
		row.appendChild(pangkat = new MyTextbox(pasien.getPangkat() == null ? "" : pasien.getPangkat()));
		pangkat.setWidth("90%");

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Golongan (Dinas)")));
		row.appendChild(golongan = new MyTextbox(pasien.getGolongan() == null ? "" : pasien.getGolongan()));
		golongan.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Dinas")));
		row.appendChild(jenis_pasien_dinas);
		Common.selectComboItem(jenis_pasien_dinas, pasien.getJenisPasienDinas());
		jenis_pasien_dinas.setWidth("90%");

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Sebuatkan jika Non Kemhan")));
		row.appendChild(sebutkan_non_kemhan = new MyTextbox(
				pasien.getSebutkan_non_kemhan() == null ? "" : pasien.getSebutkan_non_kemhan()));
		sebutkan_non_kemhan.setWidth("90%");

		return groupbox;
	}

	private Groupbox createBiodata(final Pasien pasien) {

		Groupbox groupbox = new Groupbox();

		groupbox.appendChild(new Caption("Biodata Pasien"));

		Grid grid = new Grid();
		grid.setStyle("border:0px;background: transparent;");
		grid.setParent(groupbox);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		Column column = new Column();
		column.setWidth("100px");
		column.setParent(columns);
		column = new Column();
		// column.setWidth("100px");
		column.setParent(columns);
		column = new Column();
		column.setWidth("100px");
		column.setParent(columns);
		column = new Column();
		// column.setWidth("100px");
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		Row row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama Keluarga")));

		// Hbox hbox = new Hbox();
		// row.appendChild(hbox);

		row.appendChild(nama_penanggungjawab = new MyTextbox(
				pasien.getNama_penanggungjawab() == null ? "" : pasien.getNama_penanggungjawab()));
		nama_penanggungjawab.setWidth("90%");

		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Keluarga")));
		row.appendChild(jenis_penanggungjawab);
		Common.selectComboItem(jenis_penanggungjawab, pasien.getJenis_penanggungjawab());
		jenis_penanggungjawab.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Pekerjaan Pasien")));
		row.appendChild(pekerjaan);
		Common.selectComboItem(pekerjaan, pasien.getPekerjaan());
		pekerjaan.setWidth("90%");

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Asuransi")));
		row.appendChild(asuransi = new AmbilDataAsuransiBanbox());
		asuransi.setValue(pasien.getAsuransi() == null ? "" : pasien.getAsuransi().getNama());
		asuransi.setAttribute("asuransi", pasien.getAsuransi());
		asuransi.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Propinsi")));
		row.appendChild(propinsi);
		Common.selectComboItem(propinsi, pasien.getPropinsi());
		propinsi.setWidth("90%");

		propinsi.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				kota.setAttribute("kota", null);
				kota.setValue("");
				kecamatan.setAttribute("kecamatan", null);
				kecamatan.setValue("");
				kelurahan.setAttribute("kelurahan", null);
				kelurahan.setValue("");
			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Status Perkawinan")));
		row.appendChild(statusPerkawinan);
		Common.selectComboItem(statusPerkawinan, pasien.getStatusPerkawinan());
		statusPerkawinan.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label("Kota/Kabupaten"));
		row.appendChild(kota = new AmbilDataKotaBanbox());
		kota.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				Kota myKota = (Kota) kota.getAttribute("kota");
				Common.selectComboItem(propinsi, myKota == null ? pasien.getPropinsi() : myKota.getPropinsi());
				kecamatan.setAttribute("kecamatan", null);
				kecamatan.setValue("");
				kelurahan.setAttribute("kelurahan", null);
				kelurahan.setValue("");
			}
		});
		kota.setAttribute("kota", pasien.getKota());
		kota.setValue(pasien.getKota() == null ? "" : pasien.getKota().getNama());
		kota.setWidth("90%");
		kota.setReadonly(true);

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Kelamin")));
		row.appendChild(jenisKelamin);
		Common.selectComboItem(jenisKelamin, pasien.getJenisKelamin());
		jenisKelamin.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kecamatan")));
		row.appendChild(kecamatan = new AmbilDataKecamatanBanbox());
		kecamatan.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				Kecamatan myKecamatan = (Kecamatan) kecamatan.getAttribute("kecamatan");
				Kota myKota = myKecamatan.getKota();
				Common.selectComboItem(propinsi, myKota == null ? pasien.getPropinsi() : myKota.getPropinsi());

				kota.setAttribute("kota", myKota);
				kota.setValue(myKota == null ? "" : myKota.getNama());
				kelurahan.setAttribute("kelurahan", null);
				kelurahan.setValue("");
			}

		});
		kecamatan.setAttribute("kecamatan", pasien.getKecamatan());
		kecamatan.setValue(pasien.getKecamatan() == null ? "" : pasien.getKecamatan().getNama());
		kecamatan.setWidth("90%");
		kecamatan.setReadonly(true);

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("TTL")));

		Hbox hbox = new Hbox();
		row.appendChild(hbox);

		hbox.setWidth("100%");
		hbox.appendChild(tempatLahir = new MyTextbox(pasien.getTempatLahir() == null ? "" : pasien.getTempatLahir()));
		tempatLahir.setCols(30);
		hbox.appendChild(new Label("/"));
		hbox.appendChild(
				tanggalLahir = new MyDatebox(pasien.getTanggalLahir() == null ? null : pasien.getTanggalLahir()));
		tanggalLahir.setCols(20);
		tanggalLahir.setFormat(Common.dateFormat2.get().toPattern());

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kelurahan")));
		row.appendChild(kelurahan = new AmbilDataKelurahanBanbox());
		kelurahan.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				Kelurahan myKelurahan = (Kelurahan) kelurahan.getAttribute("kelurahan");
				Kecamatan myKecamatan = myKelurahan.getKecamatan();
				Kota myKota = myKecamatan.getKota();

				Common.selectComboItem(propinsi, myKota == null ? pasien.getPropinsi() : myKota.getPropinsi());

				kota.setAttribute("kota", myKota);
				kota.setValue(myKota == null ? "" : myKota.getNama());

				kecamatan.setAttribute("kecamatan", myKecamatan);
				kecamatan.setValue(myKecamatan == null ? "" : myKecamatan.getNama());
			}
		});
		kecamatan.setAttribute("kelurahan", pasien.getKelurahan());
		kecamatan.setValue(pasien.getKelurahan() == null ? "" : pasien.getKelurahan().getNama());
		kelurahan.setWidth("90%");
		kelurahan.setReadonly(true);

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Agama")));
		row.appendChild(agama);
		Common.selectComboItem(agama, pasien.getAgama());
		agama.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label("Alamat/Jalan"));
		row.appendChild(alamat = new MyTextbox(pasien.getAlamat() == null ? "" : pasien.getAlamat()));
		alamat.setWidth("90%");
		alamat.setRows(4);

		// row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label("RT/RW"));

		hbox = new Hbox();
		row.appendChild(hbox);

		hbox.appendChild(rt = new MyTextbox(pasien.getRt() == null ? "" : pasien.getRt()));
		rt.setWidth("90%");
		hbox.appendChild(new Label("/"));
		hbox.appendChild(rw = new MyTextbox(pasien.getRw() == null ? "" : pasien.getRw()));
		rw.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("No. Telpon Pasien")));
		row.appendChild(noTelp = new MyTextbox(pasien.getNoTelp() == null ? "" : pasien.getNoTelp()));
		noTelp.setWidth("90%");

		// row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("No. HP Pasien")));
		row.appendChild(noHp = new MyTextbox(pasien.getNoHp() == null ? "" : pasien.getNoHp()));
		noHp.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("No. Telpon Keluarga")));
		row.appendChild(telp_penanggungjawab = new MyTextbox(
				pasien.getTelp_penanggungjawab() == null ? "" : pasien.getTelp_penanggungjawab()));
		telp_penanggungjawab.setWidth("90%");

		// row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("No. HP Keluarga")));
		row.appendChild(hp_penanggungjawab = new MyTextbox(
				pasien.getHp_penanggungjawab() == null ? "" : pasien.getHp_penanggungjawab()));
		hp_penanggungjawab.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kewarganegaraan")));
		row.appendChild(kewarganegaraan);
		Common.selectComboItem(kewarganegaraan, pasien.getKewarganegaraan());
		kewarganegaraan.setWidth("90%");
		// Guard: setSelectedIndex(0) melempar "Out of bound: 0 while size=0" bila combo KOSONG.
		if (kewarganegaraan.getItemCount() > 0) {
			kewarganegaraan.setSelectedIndex(0);
		}

		// row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Prioritas Pasien")));
		row.appendChild(prioritasPasien);
		Common.selectComboItem(prioritasPasien, pasien.getPrioritasPasien());
		// Guard: setSelectedIndex(1) melempar "Out of bound: 1 while size=0" bila data master
		// Prioritas Pasien KOSONG (atau <2). Default indeks-1 hanya dipakai bila item mencukupi;
		// jika tidak, biarkan hasil selectComboItem (atau tanpa pilihan) agar form tetap terbuka.
		if (prioritasPasien.getItemCount() > 1) {
			prioritasPasien.setSelectedIndex(1);
		}
		prioritasPasien.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Keterangan")));
		row.appendChild(keterangan = new MyTextbox(pasien.getKeterangan() == null ? "" : pasien.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Pendidikan")));
		row.appendChild(pendidikan);
		Common.selectComboItem(pendidikan, pasien.getPendidikan());
		pendidikan.setWidth("90%");

		return groupbox;
	}

	private void init(final Pasien pasien) throws Exception {
		this.pasien = pasien;

		initComponent();

		if (addWindow != null) {
			addWindow.setTitle(pasien.getId() == null ? "Tambah Pasien" : "Ubah Pasien");
			Common.clear(addWindow);
		} else if (tambahData != null) {
			Common.clear(tambahData);
		}
		Borderlayout borderlayout = new Borderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Grid grid = new Grid();
		grid.setStyle("border:0px;background: transparent;");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		Row row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(createKode(pasien));

		final Row rowDinas = new Row();
		rowDinas.setVisible(false);
		rowDinas.setStyle("border:0px;background: transparent;");
		rowDinas.setParent(rows);
		rowDinas.appendChild(createDinas(pasien));

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(createBiodata(pasien));

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				JenisPasien myJenisPasien = (JenisPasien) (jenisPasien.getSelectedItem() == null ? null
						: jenisPasien.getSelectedItem().getValue());
				rowDinas.setVisible(
						myJenisPasien != null && (myJenisPasien.getId().equals(ConstantValues.PASIEN_DINAS.getId())
								|| myJenisPasien.getId().equals(ConstantValues.PASIEN_SISWA.getId())));
			}
		};

		jenisPasien.addEventListener("onChange", eventListener);
		eventListener.onEvent(null);

		if (addWindow != null) {

			South south = new South();
			ais.ui.util.ZkCompat.setFlex(south, true);
			south.setParent(borderlayout);

			Toolbar toolbar = new Toolbar();
			toolbar.setHeight("30px");
			toolbar.setParent(south);

			Toolbarbutton cancel = new ais.ui.util.MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
			cancel.setTooltiptext("Tutup");
			cancel.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					// if (externalCalled != null) {
					// addWindow.setAttribute("pasien", null);
					// Event myEvent = new Event("my_event", addWindow, null);
					// externalCalled.onEvent(myEvent);
					// }
					addWindow.setVisible(false);
				}
			});
			cancel.setParent(toolbar);
			Toolbarbutton save = new ais.ui.util.MyToolbarbuttonConfig("Simpan", "/img/save.gif");
			save.setTooltiptext("Simpan");
			save.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					if (onSave(event)) {
						onSearchDefault(null);
						Common.initPaging(paging, new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								onSearchDefault(null);
							}
						});
						addWindow.setVisible(false);
					}
				}
			});
			save.setParent(toolbar);
			borderlayout.setParent(addWindow);
		} else if (tambahData != null) {

			row = new Row();
			row.setStyle("border:0px;background: transparent;");
			row.setParent(rows);

			Toolbar toolbar = new Toolbar();
			toolbar.setHeight("30px");
			toolbar.setParent(row);

			add.setParent(toolbar);
			Toolbarbutton save = new ais.ui.util.MyToolbarbuttonConfig("Simpan Data Pasien", "/img/save.gif");
			save.setTooltiptext("Simpan");
			save.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					if (onSave(event)) {
						onSearchDefault(null);
						Common.initPaging(paging, new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								onSearchDefault(null);
							}
						});
					}
				}
			});
			save.setParent(toolbar);

			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("Hapus data Pasien", "/img/delete.gif");
			button.setVisible(delete);
			button.setTooltiptext("Hapus data Pasien");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					if (pasien != null && pasien.getId() != null) {
						onDelete(pasien);

					} else {
						MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin mengosongkan formulir data pasien ini? Seluruh isian yang belum disimpan akan dikosongkan.", "Pertanyaan",
								MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = new Integer(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											init(new Pasien());
										}
									}
								});

					}
				}
			});
			button.setParent(toolbar);

			borderlayout.setParent(tambahData);
			tambahData.getLinkedTab().setSelected(true);
		}

	}

	private JenisPasien generateCode() {
		JenisPasien myJenisPasien = (JenisPasien) (jenisPasien.getSelectedItem() == null ? null
				: jenisPasien.getSelectedItem().getValue());

		if (pasien != null && pasien.getId() != null) {
			return null;
		}

		if (editRMManual) {
			return null;
		}

		if (myJenisPasien == null) {
			kode.setValue("");
			return null;
		}

		if (myJenisPasien.getId().equals(ConstantValues.PASIEN_DINAS.getId())) {
			kode.setValue(CommonSirs.generateCodePasien(8, "D", "", ConstantValues.PASIEN_DINAS));
			return ConstantValues.PASIEN_DINAS;
		} else if (myJenisPasien.getId().equals(ConstantValues.PASIEN_SISWA.getId())) {
			kode.setValue(CommonSirs.generateCodePasien(6, "D", "S", ConstantValues.PASIEN_SISWA));
			return ConstantValues.PASIEN_SISWA;
		} else {
			kode.setValue(CommonSirs.generateCodePasien(8, "", "", ConstantValues.PASIEN_UMUM));
			return ConstantValues.PASIEN_UMUM;
		}
	}

	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Pasien wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) isikan Nama Pasien pada kolom yang tersedia; (2) pastikan kolom tidak dikosongkan; (3) simpan kembali data setelah kolom terisi.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (pekerjaan.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Pekerjaan Pasien wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Pekerjaan Pasien pada daftar yang tersedia; (2) pastikan pilihan tidak dikosongkan; (3) simpan kembali data setelah Pekerjaan ditentukan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (jenisKelamin.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Jenis Kelamin Pasien wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Jenis Kelamin pada daftar yang tersedia; (2) pastikan pilihan tidak dikosongkan; (3) simpan kembali data setelah Jenis Kelamin ditentukan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (jenisPasien.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Jenis Pasien wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Jenis Pasien pada daftar yang tersedia; (2) pastikan pilihan tidak dikosongkan; (3) simpan kembali data setelah Jenis Pasien ditentukan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (kewarganegaraan.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Kewarganegaraan Pasien wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Kewarganegaraan pada daftar yang tersedia; (2) pastikan pilihan tidak dikosongkan; (3) simpan kembali data setelah Kewarganegaraan ditentukan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (statusPerkawinan.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Status Perkawinan Pasien wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Status Perkawinan pada daftar yang tersedia; (2) pastikan pilihan tidak dikosongkan; (3) simpan kembali data setelah Status Perkawinan ditentukan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (editRMManual && checkKodePasien()) {
			MyMessageboxConfig.showFormat("Mohon maaf, Nomor MR (Rekam Medis) \"{V1}\" sudah terdaftar di dalam sistem. Langkah yang dapat dilakukan: (1) gunakan Nomor MR yang berbeda; (2) periksa kembali data pasien yang telah ada melalui pencarian; (3) lakukan perubahan pada data pasien yang sudah ada apabila diperlukan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION, kode.getValue());
			return false;
		}

		/*
		 * if (keterangan.getValue().trim().equals("")) {
		 * Messagebox.show("Keterangan harus diisi", "Peringatan", Messagebox.OK,
		 * Messagebox.EXCLAMATION); return false; }
		 */

		Session session = HibernateUtil.currentSession();
		if (pasien.getId() != null) {
			pasien = (Pasien) session.load(Pasien.class, pasien.getId());

		}

		pasien.setAktif(aktif.isChecked());
		pasien.setAsuransi((Asuransi) asuransi.getAttribute("asuransi"));
		pasien.setSatker((Satker) satker.getAttribute("satker"));
		pasien.setNip(nip.getValue().trim());
		pasien.setTelp_penanggungjawab(telp_penanggungjawab.getValue());
		pasien.setHp_penanggungjawab(hp_penanggungjawab.getValue());
		pasien.setNama_penanggungjawab(nama_penanggungjawab.getValue());
		pasien.setPekerjaan((String) pekerjaan.getSelectedItem().getValue());
		pasien.setJenis_penanggungjawab((String) (jenis_penanggungjawab.getSelectedItem() == null ? null
				: jenis_penanggungjawab.getSelectedItem().getValue()));
		pasien.setPangkat(pangkat.getValue().trim());
		pasien.setGolongan(golongan.getValue().trim());
		pasien.setJenisPasienDinas((String) (jenis_pasien_dinas.getSelectedItem() == null ? null
				: jenis_pasien_dinas.getSelectedItem().getValue()));
		pasien.setSebutkan_non_kemhan(sebutkan_non_kemhan.getValue().trim());

		pasien.setNama(nama.getValue());
		pasien.setKeterangan(keterangan.getValue());
		pasien.setAgama((Agama) (agama.getSelectedItem() == null ? null : agama.getSelectedItem().getValue()));
		pasien.setAlamat(alamat.getValue());
		pasien.setJenisKelamin((String) jenisKelamin.getSelectedItem().getValue());
		JenisPasien jenisPasien = (JenisPasien) (this.jenisPasien.getSelectedItem().getValue());
		pasien.setJenisPasien(jenisPasien);
		pasien.setKecamatan((Kecamatan) (kecamatan.getAttribute("kecamatan")));
		pasien.setKelurahan((Kelurahan) (kelurahan.getAttribute("kelurahan")));
		pasien.setKeterangan(keterangan.getValue());
		pasien.setKewarganegaraan((String) kewarganegaraan.getSelectedItem().getValue());
		pasien.setKota((Kota) (kota.getAttribute("kota")));
		pasien.setNoHp(noHp.getValue());
		pasien.setNoTelp(noTelp.getValue());
		pasien.setPendidikan(
				(Pendidikan) (pendidikan.getSelectedItem() == null ? null : pendidikan.getSelectedItem().getValue()));
		pasien.setPrioritasPasien((PrioritasPasien) (prioritasPasien.getSelectedItem() == null ? null
				: prioritasPasien.getSelectedItem().getValue()));
		pasien.setPropinsi(
				(Propinsi) (propinsi.getSelectedItem() == null ? null : propinsi.getSelectedItem().getValue()));
		pasien.setRt(rt.getValue());
		pasien.setRw(rw.getValue());
		pasien.setStatusPerkawinan((String) (statusPerkawinan.getSelectedItem() == null ? null
				: statusPerkawinan.getSelectedItem().getValue()));
		pasien.setTanggalLahir(tanggalLahir.getValue());
		pasien.setTempatLahir(tempatLahir.getValue());
		pasien.setTanggalRegistrasi(tanggalRegistrasi.getValue());

		if (jenisPasien.getNama().trim().equalsIgnoreCase("")) {

		}
		pasien.setKode(kode.getValue().trim());

		if (pasien.getId() != null) {
			Common.refreshUpdate(session, pasien);
		} else {
			JenisPasien myJenisPasien = generateCode();
			if (myJenisPasien != null) {
				pasien.setKode(kode.getValue().trim());
				pasien.setIndex(CommonSirs.generateMaxByJenisPasien(myJenisPasien) + 1);
			}
			session.save(pasien);

			CommonSirs.simpanTransaksiTindakan(pasien, ConstantValues.PENDAFTARAN_PASIEN, ConstantValues.kelasNormal,
					Common.getCurrentLokasi(), 1.0);
		}

		if (externalCalled != null) {
			addWindow.setAttribute("pasien", pasien);
			Event myEvent = new Event("my_event", addWindow, pasien);
			externalCalled.onEvent(myEvent);
		}

		MyMessageboxConfig.show("Data pasien telah berhasil disimpan ke dalam sistem. Terima kasih, Bapak/Ibu.", "Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						if (tambahData != null && add != null) {
							Common.freeze(tambahData, true);
							add.setDisabled(false);
						}

					}
				});

		return true;
	}

	private Criteria initCriteria(boolean order) {
		Criterion criterion = searchalamat.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
				: Restrictions.ilike("alamat", searchalamat.getValue(), MatchMode.ANYWHERE);

		criterion = searchalamat.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
				: Restrictions.or(criterion,
						Restrictions.ilike("propinsi.nama", searchalamat.getValue(), MatchMode.ANYWHERE));

		criterion = searchalamat.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
				: Restrictions.or(criterion,
						Restrictions.ilike("kota.nama", searchalamat.getValue(), MatchMode.ANYWHERE));

		criterion = searchalamat.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
				: Restrictions.or(criterion,
						Restrictions.ilike("kecamatan.nama", searchalamat.getValue(), MatchMode.ANYWHERE));

		criterion = searchalamat.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
				: Restrictions.or(criterion,
						Restrictions.ilike("kelurahan.nama", searchalamat.getValue(), MatchMode.ANYWHERE));

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Pasien.class).createAlias("propinsi", "propinsi", Criteria.LEFT_JOIN)
				.createAlias("kota", "kota", Criteria.LEFT_JOIN)
				.createAlias("kecamatan", "kecamatan", Criteria.LEFT_JOIN)
				.createAlias("kelurahan", "kelurahan", Criteria.LEFT_JOIN)
				.add((searchnama == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchnama.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nama", searchnama.getValue(), MatchMode.ANYWHERE)))
				.add((searchkode == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchkode.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("kode", searchkode.getValue(), MatchMode.ANYWHERE)))
				.add((searchtelp == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchtelp.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.ilike("noTelp", searchtelp.getValue(), MatchMode.ANYWHERE),
								Restrictions.ilike("noHp", searchtelp.getValue(), MatchMode.ANYWHERE))));
		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		if (searchnama == null) {
			return;
		}
		Common.initPaging(initCriteria(false), paging);

		List<Pasien> pasien = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(pasien);
		grid.setRowRenderer(new PasienRenderer());
		grid.setModel(strset);
		grid.renderAll();

	}

	public Boolean checkKodePasien() {
		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(Pasien.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("kode", kode.getValue().trim()))
				.add(this.pasien.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.pasien.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}
}
