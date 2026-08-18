package ais.action.master.sisdes;

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
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Window;

import ais.action.master.helper.AmbilDataKecamatanBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.sirs.helper.AmbilDataKelurahanBanbox;
import ais.action.master.sirs.helper.AmbilDataKotaBanbox;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Agama;
import ais.database.model.Kota;
import ais.database.model.Propinsi;
import ais.database.model.Tbmuser;
import ais.database.model.employ.Pendidikan;
import ais.database.model.sirs.Kecamatan;
import ais.database.model.sirs.Kelurahan;
import ais.database.model.sisdes.Penduduk;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;

public class PendudukAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Window addWindow;
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
	private MyTextbox alamat;
	private MyTextbox rt;
	private MyTextbox rw;

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

	private MyTextbox nama_penanggungjawab;
	private Combobox jenis_penanggungjawab;

	private boolean edit = false;
	private boolean delete = false;

	private Penduduk penduduk;
	private Toolbarbutton add;

	private EventListener externalCalled;
	private MyTextbox telp_penanggungjawab;
	private MyTextbox hp_penanggungjawab;
	private Checkbox aktif;
	private Textbox pass;

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
			add = new ais.ui.util.MyToolbarbuttonConfig("Penduduk Baru", "/img/user_male_add.png");
			add.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(new Penduduk());
				}
			});
//			init(new Penduduk());
		}

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

	class PendudukRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final Penduduk penduduk = (Penduduk) arg1;

			if (penduduk.getAktif() == null || !penduduk.getAktif()) {
				arg0.setStyle("background-color:red;");
			}
			new Label(penduduk.getKode()).setParent(arg0);

			RevisiHelper.createNewRevisi(Penduduk.class, penduduk, penduduk.getNama()).setParent(arg0);
			new Label(penduduk.getTempatLahir() + (penduduk.getTanggalLahir() == null ? ""
					: ", " + Common.dateFormat2.get().format(penduduk.getTanggalLahir()))).setParent(arg0);
			new Label(penduduk.getNoTelp() + " / " + penduduk.getNoHp()).setParent(arg0);

			new Label(penduduk.getAlamatLengkap()).setParent(arg0);

			new Label(penduduk.getAktif() == null || !penduduk.getAktif() ? "Tidak" : "Ya").setParent(arg0);

			Hbox toolbar = new Hbox();
			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/user_male_add.png");
			button.setTooltiptext("Copy Data Penduduk menggunakan Kode baru");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					Penduduk myPenduduk = (Penduduk) penduduk.clone();
					myPenduduk.setId(null);
					myPenduduk.setKode(null);
					init(myPenduduk);
				}

			});
			button.setParent(toolbar);

			button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/edit.gif");
			button.setTooltiptext("Rubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(penduduk);
				}

			});
			button.setParent(toolbar);

			button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					onDelete(penduduk);

				}
			});
			button.setParent(toolbar);
			toolbar.setParent(arg0);
		}

	}

	public void onDelete(final Penduduk penduduk) throws Exception {
		MyMessageboxConfig.show(
				"Apakah Bapak/Ibu yakin ingin menghapus data ini? Perlu diperhatikan bahwa tindakan ini bersifat permanen dan data yang telah dihapus tidak dapat dikembalikan.",
				"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
				MyMessageboxConfig.QUESTION, new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {
						int i = new Integer(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							try {
								Common.refreshDelete(penduduk);
								onSearchDefault(event);
							} catch (Exception e) {
								ais.common.Common.tampilErrorJikaAdmin(e);
								MyMessageboxConfig.showFormat(
										"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Rincian teknis: {V1}. Langkah yang dapat dilakukan: (1) periksa data lain yang masih terkait dengan data ini; (2) hapus atau lepaskan keterkaitan tersebut terlebih dahulu; (3) hubungi admin apabila memerlukan bantuan.",
										"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
										e.getMessage());
							}

						}

					}
				});
	}

	public void onAdd(Event event) throws Exception {
		init(new Penduduk());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	public static Window onExternalAdd(EventListener listener) throws Exception {
		PendudukAction pendudukAction = new PendudukAction();
		pendudukAction.externalCalled = listener;
		pendudukAction.addWindow = new Window();
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(pendudukAction.addWindow);
		pendudukAction.init(new Penduduk());

		pendudukAction.addWindow.setWidth("750px");
		pendudukAction.addWindow.setHeight("95%");
		pendudukAction.addWindow.setTitle("Tambah Penduduk Baru");
		pendudukAction.addWindow.setVisible(true);
		pendudukAction.addWindow.onModal();

		return pendudukAction.addWindow;
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
		statusPerkawinan.setReadonly(true);
		pekerjaan = Common.initPekerjaan(pekerjaan);

		jenisKelamin = new Combobox();
		comboitem = new Comboitem("Laki-laki");
		comboitem.setValue("Laki-laki");
		jenisKelamin.appendChild(comboitem);
		comboitem = new Comboitem("Perempuan");
		comboitem.setValue("Perempuan");
		jenisKelamin.appendChild(comboitem);
		jenisKelamin.setReadonly(true);

		kewarganegaraan = new Combobox();
		comboitem = new Comboitem("WNI");
		comboitem.setValue("WNI");
		kewarganegaraan.appendChild(comboitem);
		comboitem = new Comboitem("WNA");
		comboitem.setValue("WNA");
		kewarganegaraan.appendChild(comboitem);
		kewarganegaraan.setReadonly(true);

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
		jenis_penanggungjawab.setReadonly(true);
		Common.insertCombo(pendidikan = new Combobox(), "nama", Pendidikan.class);
		Common.insertCombo(agama = new Combobox(), "nama", Agama.class);
		Common.insertCombo(propinsi = new Combobox(), "nama", Propinsi.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		pendidikan.setReadonly(true);
		agama.setReadonly(true);
		propinsi.setReadonly(true);

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
		// Propinsi propinsi = (Propinsi) (PendudukAction.this.propinsi
		// .getSelectedItem() == null ? null
		// : PendudukAction.this.propinsi.getSelectedItem()
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
		// Kota kota = (Kota) (PendudukAction.this.kota.getSelectedItem() == null
		// ? null
		// : PendudukAction.this.kota.getSelectedItem().getValue());
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
		// Kecamatan kecamatan = (Kecamatan) (PendudukAction.this.kecamatan
		// .getSelectedItem() == null ? null
		// : PendudukAction.this.kecamatan.getSelectedItem()
		// .getValue());
		// if (kecamatan == null) {
		// return;
		// }
		// Common.insertCombo(kelurahan, "nama", Kelurahan.class,
		// Restrictions.eq("kecamatan", kecamatan));
		// }
		// });
	}

	private Groupbox createKode(final Penduduk penduduk) {

		Groupbox groupbox = new Groupbox();

		groupbox.appendChild(new Caption("Data Penduduk"));

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
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("NIK")));
		String mykode = penduduk.getKode();
		row.appendChild(kode = new MyTextbox(mykode));
		kode.setWidth("90%");

		Tbmuser tbmuser = Common.getCurrentUser();

		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Password")));
		pass = new Textbox(penduduk.getPass() == null || penduduk.getPass().trim().equals("") ? ""
				: Common.desEncrypter.get().decrypt(penduduk.getPass()));
		if (tbmuser != null && tbmuser.getSiswa() != null) {
			row.appendChild(new Label(ais.common.Common.getBahasaConfig("* Untuk mengubah password, klik menu Ganti Password")));
		} else {
			row.appendChild(pass);
			pass.setDisabled(!Common.getApakahAdmin());
		}
		pass.setWidth("90%");
		pass.setType("password");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama Penduduk")));
		row.appendChild(nama = new MyTextbox(penduduk.getNama() == null ? "" : penduduk.getNama()));
		nama.setWidth("90%");

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Waktu Registrasi")));
		row.appendChild(tanggalRegistrasi = new MyDatebox(penduduk.getTanggalRegistrasi()));
		tanggalRegistrasi.setFormat(Common.dateFormat3.get().toPattern());
		tanggalRegistrasi.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Aktif")));
		row.appendChild(aktif = new Checkbox());
		aktif.setChecked(penduduk.getAktif() == null ? false : penduduk.getAktif());

		return groupbox;
	}

	private Groupbox createBiodata(final Penduduk penduduk) {

		Groupbox groupbox = new Groupbox();

		groupbox.appendChild(new Caption("Biodata Penduduk"));

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
				penduduk.getNama_penanggungjawab() == null ? "" : penduduk.getNama_penanggungjawab()));
		nama_penanggungjawab.setWidth("90%");

		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Keluarga")));
		row.appendChild(jenis_penanggungjawab);
		Common.selectComboItem(jenis_penanggungjawab, penduduk.getJenis_penanggungjawab());
		jenis_penanggungjawab.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Pekerjaan Penduduk")));
		row.appendChild(pekerjaan);
		Common.selectComboItem(pekerjaan, penduduk.getPekerjaan());
		pekerjaan.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Propinsi")));
		row.appendChild(propinsi);
		Common.selectComboItem(propinsi, penduduk.getPropinsi());
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
		Common.selectComboItem(statusPerkawinan, penduduk.getStatusPerkawinan());
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
				Common.selectComboItem(propinsi, myKota == null ? penduduk.getPropinsi() : myKota.getPropinsi());
				kecamatan.setAttribute("kecamatan", null);
				kecamatan.setValue("");
				kelurahan.setAttribute("kelurahan", null);
				kelurahan.setValue("");
			}
		});
		kota.setAttribute("kota", penduduk.getKota());
		kota.setValue(penduduk.getKota() == null ? "" : penduduk.getKota().getNama());
		kota.setWidth("90%");
		kota.setReadonly(true);

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Kelamin")));
		row.appendChild(jenisKelamin);
		Common.selectComboItem(jenisKelamin, penduduk.getJenisKelamin());
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
				Common.selectComboItem(propinsi, myKota == null ? penduduk.getPropinsi() : myKota.getPropinsi());

				kota.setAttribute("kota", myKota);
				kota.setValue(myKota == null ? "" : myKota.getNama());
				kelurahan.setAttribute("kelurahan", null);
				kelurahan.setValue("");
			}

		});
		kecamatan.setAttribute("kecamatan", penduduk.getKecamatan());
		kecamatan.setValue(penduduk.getKecamatan() == null ? "" : penduduk.getKecamatan().getNama());
		kecamatan.setWidth("90%");
		kecamatan.setReadonly(true);

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("TTL")));

		Hbox hbox = new Hbox();
		row.appendChild(hbox);

		hbox.setWidth("100%");
		hbox.appendChild(
				tempatLahir = new MyTextbox(penduduk.getTempatLahir() == null ? "" : penduduk.getTempatLahir()));
		tempatLahir.setCols(30);
		hbox.appendChild(new Label("/"));
		hbox.appendChild(
				tanggalLahir = new MyDatebox(penduduk.getTanggalLahir() == null ? null : penduduk.getTanggalLahir()));
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

				Common.selectComboItem(propinsi, myKota == null ? penduduk.getPropinsi() : myKota.getPropinsi());

				kota.setAttribute("kota", myKota);
				kota.setValue(myKota == null ? "" : myKota.getNama());

				kecamatan.setAttribute("kecamatan", myKecamatan);
				kecamatan.setValue(myKecamatan == null ? "" : myKecamatan.getNama());
			}
		});
		kecamatan.setAttribute("kelurahan", penduduk.getKelurahan());
		kecamatan.setValue(penduduk.getKelurahan() == null ? "" : penduduk.getKelurahan().getNama());
		kelurahan.setWidth("90%");
		kelurahan.setReadonly(true);

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Agama")));
		row.appendChild(agama);
		Common.selectComboItem(agama, penduduk.getAgama());
		agama.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label("Alamat/Jalan"));
		row.appendChild(alamat = new MyTextbox(penduduk.getAlamat() == null ? "" : penduduk.getAlamat()));
		alamat.setWidth("90%");
		alamat.setRows(4);

		// row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label("RT/RW"));

		hbox = new Hbox();
		row.appendChild(hbox);

		hbox.appendChild(rt = new MyTextbox(penduduk.getRt() == null ? "" : penduduk.getRt()));
		rt.setWidth("90%");
		hbox.appendChild(new Label("/"));
		hbox.appendChild(rw = new MyTextbox(penduduk.getRw() == null ? "" : penduduk.getRw()));
		rw.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("No. Telpon Penduduk")));
		row.appendChild(noTelp = new MyTextbox(penduduk.getNoTelp() == null ? "" : penduduk.getNoTelp()));
		noTelp.setWidth("90%");

		// row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("No. HP Penduduk")));
		row.appendChild(noHp = new MyTextbox(penduduk.getNoHp() == null ? "" : penduduk.getNoHp()));
		noHp.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("No. Telpon Keluarga")));
		row.appendChild(telp_penanggungjawab = new MyTextbox(
				penduduk.getTelp_penanggungjawab() == null ? "" : penduduk.getTelp_penanggungjawab()));
		telp_penanggungjawab.setWidth("90%");

		// row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("No. HP Keluarga")));
		row.appendChild(hp_penanggungjawab = new MyTextbox(
				penduduk.getHp_penanggungjawab() == null ? "" : penduduk.getHp_penanggungjawab()));
		hp_penanggungjawab.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kewarganegaraan")));
		row.appendChild(kewarganegaraan);
		Common.selectComboItem(kewarganegaraan, penduduk.getKewarganegaraan());
		kewarganegaraan.setWidth("90%");
		kewarganegaraan.setSelectedIndex(0);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Keterangan")));
		row.appendChild(keterangan = new MyTextbox(penduduk.getKeterangan() == null ? "" : penduduk.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Pendidikan")));
		row.appendChild(pendidikan);
		Common.selectComboItem(pendidikan, penduduk.getPendidikan());
		pendidikan.setWidth("90%");

		return groupbox;
	}

	private void init(final Penduduk penduduk) throws Exception {
		this.penduduk = penduduk;

		initComponent();

		if (addWindow != null) {
			addWindow.setTitle(penduduk.getId() == null ? "Tambah Penduduk" : "Ubah Penduduk");
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
		row.appendChild(createKode(penduduk));

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(createBiodata(penduduk));

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
					// addWindow.setAttribute("penduduk", null);
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
			Toolbarbutton save = new ais.ui.util.MyToolbarbuttonConfig("Simpan Data Penduduk", "/img/save.gif");
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

			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("Hapus data Penduduk", "/img/delete.gif");
			button.setVisible(delete);
			button.setTooltiptext("Hapus data Penduduk");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					if (penduduk != null && penduduk.getId() != null) {
						onDelete(penduduk);

					} else {
						MyMessageboxConfig.show(
								"Apakah Bapak/Ibu yakin ingin menghapus data penduduk ini? Perlu diperhatikan bahwa tindakan ini bersifat permanen dan data yang telah dihapus tidak dapat dikembalikan.",
								"Pertanyaan",
								MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = new Integer(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											init(new Penduduk());
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

	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show(
					"Mohon Bapak/Ibu terlebih dahulu mengisi Nama Penduduk. Langkah yang dapat dilakukan: (1) klik kolom Nama; (2) isikan nama penduduk secara lengkap; (3) tekan tombol Simpan kembali.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (pekerjaan.getSelectedItem() == null) {
			MyMessageboxConfig.show(
					"Mohon Bapak/Ibu terlebih dahulu memilih Pekerjaan Penduduk. Langkah yang dapat dilakukan: (1) buka pilihan Pekerjaan; (2) pilih pekerjaan yang sesuai; (3) tekan tombol Simpan kembali.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (jenisKelamin.getSelectedItem() == null) {
			MyMessageboxConfig.show(
					"Mohon Bapak/Ibu terlebih dahulu memilih Jenis Kelamin Penduduk. Langkah yang dapat dilakukan: (1) buka pilihan Jenis Kelamin; (2) pilih Laki-laki atau Perempuan; (3) tekan tombol Simpan kembali.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (kewarganegaraan.getSelectedItem() == null) {
			MyMessageboxConfig.show(
					"Mohon Bapak/Ibu terlebih dahulu memilih Kewarganegaraan Penduduk. Langkah yang dapat dilakukan: (1) buka pilihan Kewarganegaraan; (2) pilih kewarganegaraan yang sesuai; (3) tekan tombol Simpan kembali.",
					"Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (statusPerkawinan.getSelectedItem() == null) {
			MyMessageboxConfig.show(
					"Mohon Bapak/Ibu terlebih dahulu memilih Status Perkawinan Penduduk. Langkah yang dapat dilakukan: (1) buka pilihan Status Perkawinan; (2) pilih status yang sesuai; (3) tekan tombol Simpan kembali.",
					"Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (penduduk.getId() != null) {
			penduduk = (Penduduk) session.load(Penduduk.class, penduduk.getId());

		}

		penduduk.setAktif(aktif.isChecked());

		penduduk.setTelp_penanggungjawab(telp_penanggungjawab.getValue());
		penduduk.setHp_penanggungjawab(hp_penanggungjawab.getValue());
		penduduk.setNama_penanggungjawab(nama_penanggungjawab.getValue());
		penduduk.setPekerjaan((String) pekerjaan.getSelectedItem().getValue());
		penduduk.setJenis_penanggungjawab((String) (jenis_penanggungjawab.getSelectedItem() == null ? null
				: jenis_penanggungjawab.getSelectedItem().getValue()));

		penduduk.setNama(nama.getValue());
		penduduk.setKeterangan(keterangan.getValue());
		penduduk.setAgama((Agama) (agama.getSelectedItem() == null ? null : agama.getSelectedItem().getValue()));
		penduduk.setAlamat(alamat.getValue());
		penduduk.setJenisKelamin((String) jenisKelamin.getSelectedItem().getValue());

		penduduk.setKecamatan((Kecamatan) (kecamatan.getAttribute("kecamatan")));
		penduduk.setKelurahan((Kelurahan) (kelurahan.getAttribute("kelurahan")));
		penduduk.setKeterangan(keterangan.getValue());
		penduduk.setKewarganegaraan((String) kewarganegaraan.getSelectedItem().getValue());
		penduduk.setKota((Kota) (kota.getAttribute("kota")));
		penduduk.setNoHp(noHp.getValue());
		penduduk.setNoTelp(noTelp.getValue());
		penduduk.setPendidikan(
				(Pendidikan) (pendidikan.getSelectedItem() == null ? null : pendidikan.getSelectedItem().getValue()));

		penduduk.setPropinsi(
				(Propinsi) (propinsi.getSelectedItem() == null ? null : propinsi.getSelectedItem().getValue()));
		penduduk.setRt(rt.getValue());
		penduduk.setRw(rw.getValue());
		penduduk.setStatusPerkawinan((String) (statusPerkawinan.getSelectedItem() == null ? null
				: statusPerkawinan.getSelectedItem().getValue()));
		penduduk.setTanggalLahir(tanggalLahir.getValue());
		penduduk.setTempatLahir(tempatLahir.getValue());
		penduduk.setTanggalRegistrasi(tanggalRegistrasi.getValue());

		penduduk.setKode(kode.getValue().trim());
		try {
			penduduk.setPass(Common.desEncrypter.get().encrypt(pass.getValue().trim()));
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		if (penduduk.getId() != null) {
			Common.refreshUpdate(session, penduduk);
		} else {
			session.save(penduduk);

		}

		if (externalCalled != null) {
			addWindow.setAttribute("penduduk", penduduk);
			Event myEvent = new Event("my_event", addWindow, penduduk);
			externalCalled.onEvent(myEvent);
		}

		MyMessageboxConfig.show(
				"Data penduduk telah berhasil disimpan. Terima kasih atas kesediaan Bapak/Ibu melengkapi data.",
				"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
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
		Criteria criteria = session.createCriteria(Penduduk.class)
				.createAlias("propinsi", "propinsi", Criteria.LEFT_JOIN).createAlias("kota", "kota", Criteria.LEFT_JOIN)
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

		List<Penduduk> penduduk = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(penduduk);
		grid.setRowRenderer(new PendudukRenderer());
		grid.setModel(strset);
		grid.renderAll();

	}

}
