package ais.action.master.sirs;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Button;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Window;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.sirs.helper.AmbilDataBookingRegistrasiBanbox;
import ais.action.master.sirs.helper.AmbilDataPasienBanbox;
import ais.action.master.sirs.helper.AmbilDataPendaftaranRawatJalanBanbox;
import ais.action.master.sirs.helper.AmbilDataTempatTidurBanbox;
import ais.action.master.sirs.helper.MonitorDataTempatTidurHelper;
import ais.action.master.sirs.util.CommonPendaftaranUtil;
import ais.action.master.sirs.util.RawatInapCalculationProcessor;
import ais.action.report.Report;
import ais.common.BarcodeCommon;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.CommonSirs;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Ruang;
import ais.database.model.asset.Lokasi;
import ais.database.model.employ.Pendidikan;
import ais.database.model.sirs.Asuransi;
import ais.database.model.sirs.Bagian;
import ais.database.model.sirs.BookingRegistrasi;
import ais.database.model.sirs.Dokter;
import ais.database.model.sirs.JadwalDokter;
import ais.database.model.sirs.JenisPasien;
import ais.database.model.sirs.Kamar;
import ais.database.model.sirs.KelasPerawatan;
import ais.database.model.sirs.Pasien;
import ais.database.model.sirs.Pendaftaran;
import ais.database.model.sirs.Poly;
import ais.database.model.sirs.Shift;
import ais.database.model.sirs.TempatTidur;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;

public class PendaftaranRawatInapAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Grid grid;
	private Paging paging;

	private Tabpanel tambahData;

	private MyTextbox searchkode;
	private AmbilDataPasienBanbox searchmr;
	private MyTextbox searchnama;
	private Combobox searchbagian;
	private Combobox searchjenisPasien;
	private MyTextbox searchtelp;
	private MyTextbox searchalamat;

	private Combobox searchkelas;
	private Combobox searchruang;
	private Combobox searchkamar;
	private AmbilDataTempatTidurBanbox searchbed;

	private Label kode;
	private Pasien pasien;
	private Asuransi asuransi;
	private Date tanggalPendaftaran;
	private String keterangan;
	private Date dilayaniTanggal;

	private AmbilDataPendaftaranRawatJalanBanbox transferDaripendaftaran;

	private Checkbox baru;
	private Combobox sumberPasien;

	private Poly poly;
	private Poly subpoly;
	private Dokter dokter;
	private JadwalDokter shiftDokter;

	private MyTextbox namaDokterPengirim;
	private MyTextbox pernahDirawatDi;
	private Datebox tanggalPernahDirawat;

	private boolean edit = false;
	private boolean delete = false;

	private Pendaftaran pendaftaran;
	private Toolbarbutton add;

	private String jenis = Pendaftaran.RAWAT_INAP;

	private Lokasi myLokasi = Common.getCurrentLokasi();
	private Shift myShift;

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			execution.sendRedirect("/logoff");
			return;
		}

		Common.insertCombo(searchkelas, "nama", "keterangan", KelasPerawatan.class);
		Common.insertCombo(searchruang, "nama", "keterangan", Ruang.class);

		searchmr.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);

			}
		});

		EventListener myEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(searchkamar);
				if (searchkelas.getSelectedItem() != null && searchruang.getSelectedItem() != null) {
					Common.insertCombo(searchkamar, "nama", "keterangan", Kamar.class,
							Restrictions.and(Restrictions.eq("ruang", searchruang.getSelectedItem().getValue()),
									Restrictions.eq("kelasPerawatan", searchkelas.getSelectedItem().getValue())));
				}
			}
		};

		searchkelas.addEventListener("onChange", myEventListener);
		searchruang.addEventListener("onChange", myEventListener);

		searchkelas.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				KelasPerawatan mykelasPerawatan = (KelasPerawatan) (searchkelas.getSelectedItem() == null ? null
						: searchkelas.getSelectedItem().getValue());
				if (mykelasPerawatan != null) {
					searchbed.setMyKelasPerawatan(mykelasPerawatan);
				}
			}
		});

		searchruang.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Ruang myRuang = (Ruang) (searchruang.getSelectedItem() == null ? null
						: searchruang.getSelectedItem().getValue());
				if (myRuang != null) {
					searchbed.setMyRuang(myRuang);
				}
			}
		});

		searchkamar.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Kamar myKamar = (Kamar) (searchkamar.getSelectedItem() == null ? null
						: searchkamar.getSelectedItem().getValue());
				if (myKamar != null) {
					searchbed.setMyKamar(myKamar);
				}
			}
		});

		Common.insertCombo(searchjenisPasien, "nama", JenisPasien.class);

		Common.insertCombo(searchbagian, "nama", "keterangan", Bagian.class);

		sumberPasien = new Combobox();
		Comboitem comboitem = new Comboitem(Pendaftaran.SUMBER_PASIEN_POLI);
		if (comboitem != null) { comboitem.setValue(Pendaftaran.SUMBER_PASIEN_POLI); }
		sumberPasien.appendChild(comboitem);
		comboitem = new Comboitem(Pendaftaran.SUMBER_PASIEN_UGD);
		if (comboitem != null) { comboitem.setValue(Pendaftaran.SUMBER_PASIEN_UGD); }
		sumberPasien.appendChild(comboitem);
		comboitem = new Comboitem(Pendaftaran.SUMBER_PASIEN_DARI_RS);
		if (comboitem != null) { comboitem.setValue(Pendaftaran.SUMBER_PASIEN_DARI_RS); }
		sumberPasien.appendChild(comboitem);
		comboitem = new Comboitem(Pendaftaran.SUMBER_PASIEN_DARI_TAMU);
		if (comboitem != null) { comboitem.setValue(Pendaftaran.SUMBER_PASIEN_DARI_TAMU); }
		sumberPasien.appendChild(comboitem);
		comboitem = new Comboitem(Pendaftaran.SUMBER_PASIEN_LUAR_DKI);
		if (comboitem != null) { comboitem.setValue(Pendaftaran.SUMBER_PASIEN_LUAR_DKI); }
		sumberPasien.appendChild(comboitem);

		add = new ais.ui.util.MyToolbarbuttonConfig("Pendaftaran Baru", "/img/user_male_add.png");
		add.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				init(new Pendaftaran());

			}
		});
		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		init(new Pendaftaran());
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	class PendaftaranRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final Pendaftaran pendaftaran = (Pendaftaran) arg1;

			Pasien pasien = pendaftaran.getPasien();

			if (pasien.getAktif() == null || !pasien.getAktif()) {
				arg0.setStyle("background-color:red;");
			}

			new Label(pendaftaran == null ? "" : pendaftaran.getKode()).setParent(arg0);
			new Label(pasien.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(Pendaftaran.class, pendaftaran, pasien == null ? "" : pasien.getNama())
					.setParent(arg0);

			new Label(Common.dateFormat3.get().format(pendaftaran.getTanggalPendaftaran())).setParent(arg0);
			new Label(pendaftaran.getStatusPendaftaran()).setParent(arg0);
			new Label(pendaftaran.getSumberPasien()).setParent(arg0);

			new Label(pasien == null ? "" : pasien.getAlamatLengkap()).setParent(arg0);
			new Label(pendaftaran.getKelasPerawatan() == null ? "" : pendaftaran.getKelasPerawatan().getNama())
					.setParent(arg0);
			String bed = (pendaftaran.getRuangPerawatan() == null ? "" : pendaftaran.getRuangPerawatan().getNama())
					+ " - " + (pendaftaran.getKamarPerawatan() == null ? "" : pendaftaran.getKamarPerawatan().getNama())
					+ " - " + (pendaftaran.getTempatTidur() == null ? "" : pendaftaran.getTempatTidur().getNama());

			new Label(bed).setParent(arg0);

			new Label(pasien == null ? "" : pasien.getNoTelp() + " / " + pasien.getNoHp()).setParent(arg0);
			new Label(pasien == null ? "" : pasien.getJenisPasien() == null ? "" : pasien.getJenisPasien().getNama())
					.setParent(arg0);

			Hbox toolbar = new Hbox();
			toolbar.setVisible(pasien.getAktif() != null && pasien.getAktif());
			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/user_male.png");
			button.setTooltiptext("Cetak Status Pasien Rawat Inap");
			button.setVisible(pendaftaran.getPasien() != null);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					onCetakStatusPasien(pendaftaran);
				}

			});
			button.setParent(toolbar);

			button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/edit.gif");
			button.setTooltiptext("Rubah Data");
			button.setVisible(edit && (pendaftaran.getLunas() == null || !pendaftaran.getLunas()));
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(pendaftaran);

				}

			});
			button.setParent(toolbar);

			button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete && (pendaftaran.getLunas() == null || !pendaftaran.getLunas()));
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					onDelete(pendaftaran);
				}
			});
			button.setParent(toolbar);
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(arg0);
		}

	}

	public void onDelete(final Pendaftaran pendaftaran) throws Exception {
		MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus data pendaftaran pasien rawat inap ini? Data pendaftaran yang telah dihapus tidak dapat dikembalikan.", "Pertanyaan",
				MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {
						int i = new Integer(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							try {
								Session session = HibernateUtil.currentSession();
								TempatTidur tempatTidur = pendaftaran.getTempatTidur();
								if (tempatTidur != null) {
									tempatTidur.setTerisi(false);
									Common.refreshUpdate(session, tempatTidur);
								}

								session.refresh(pendaftaran);
								Common.refreshDelete(session, pendaftaran);
								onSearchDefault(event);
							} catch (Exception e) {
								ais.common.Common.tampilErrorJikaAdmin(e);
								MyMessageboxConfig.show(Common.pesan(
										"Data ini tidak dapat dihapus karena masih memiliki keterkaitan dengan data lainnya. Rincian kesalahan: {V1}. Langkah yang dapat dilakukan: (1) periksa kembali data lain yang terkait dengan data ini; (2) hapus terlebih dahulu data terkait tersebut apabila memungkinkan; (3) hubungi administrator sistem apabila kendala masih berlanjut.",
												e.getMessage()));
							}

						}

					}
				});
	}

	public void onAdd(Event event) throws Exception {
		init(new Pendaftaran());
	}

	private EventListener perubahanPasienListener;
	private Combobox pendaftar;
	private MyTextbox namaPendaftar;
	private MyTextbox alamatPendaftar;
	private MyTextbox telpPendaftar;
	private Combobox biayaPerawatan;
	private MyTextbox namaPenjamin;
	private Combobox pendidikanPenjamin;
	private Combobox pekerjaanPenjamin;
	private MyTextbox alamatPenjamin;

	private Combobox kelasPerawatan;
	private Combobox ruangPerawatan;
	private Combobox kamarPerawatan;
	private AmbilDataTempatTidurBanbox tempatTidur;

	private AmbilDataBookingRegistrasiBanbox bookingRegistrasi;
	private EventListener jadwalPerawatanEventListener;

	private Borderlayout createRuangPerawatan(final Pendaftaran pendaftaran) throws Exception {
		Borderlayout borderlayout = new Borderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Grid grid = new Grid();
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();

		columns.setParent(grid);

		Column column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("30%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("70%");

		Rows rows = new Rows();
		rows.setParent(grid);

		Row row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kelas Perawatan")));
		row.appendChild(kelasPerawatan = new Combobox());
		Common.insertCombo(kelasPerawatan, "nama", KelasPerawatan.class,
				Restrictions.ne("id", ConstantValues.kelasNormalId()));
		Common.selectComboItem(kelasPerawatan, pendaftaran.getKelasPerawatan());
		kelasPerawatan.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Ruang")));
		row.appendChild(ruangPerawatan = new Combobox());
		Common.insertCombo(ruangPerawatan, "nama", Ruang.class);
		Common.selectComboItem(ruangPerawatan, pendaftaran.getRuangPerawatan());
		ruangPerawatan.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kamar")));
		row.appendChild(kamarPerawatan = new Combobox());
		kamarPerawatan.setWidth("90%");

		EventListener myEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(kamarPerawatan);
				Common.insertCombo(kamarPerawatan, "nama", "keterangan", Kamar.class, Restrictions.and(
						ruangPerawatan.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("ruang", ruangPerawatan.getSelectedItem().getValue()),
						kelasPerawatan.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("kelasPerawatan", kelasPerawatan.getSelectedItem().getValue())));
				Common.selectComboItem(kamarPerawatan, pendaftaran.getKamarPerawatan());
			}

		};

		kelasPerawatan.addEventListener("onChange", myEventListener);
		ruangPerawatan.addEventListener("onChange", myEventListener);
		myEventListener.onEvent(null);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tempat Tidur (Bed)")));
		row.appendChild(tempatTidur = new AmbilDataTempatTidurBanbox());
		tempatTidur.setAttribute("tempatTidur", pendaftaran.getTempatTidur());
		tempatTidur.setValue(pendaftaran.getTempatTidur() == null ? "" : pendaftaran.getTempatTidur().getNama());
		tempatTidur.setWidth("90%");

		kelasPerawatan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				KelasPerawatan mykelasPerawatan = (KelasPerawatan) (kelasPerawatan.getSelectedItem() == null ? null
						: kelasPerawatan.getSelectedItem().getValue());
				tempatTidur.setMyKelasPerawatan(mykelasPerawatan);
			}
		});

		ruangPerawatan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Ruang myRuang = (Ruang) (ruangPerawatan.getSelectedItem() == null ? null
						: ruangPerawatan.getSelectedItem().getValue());
				tempatTidur.setMyRuang(myRuang);
			}
		});

		kamarPerawatan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Kamar myKamar = (Kamar) (kamarPerawatan.getSelectedItem() == null ? null
						: kamarPerawatan.getSelectedItem().getValue());
				tempatTidur.setMyKamar(myKamar);
				if (myKamar != null) {

					Common.selectComboItem(kelasPerawatan, myKamar.getKelasPerawatan());

					Common.insertCombo(ruangPerawatan, "nama", "keterangan", Ruang.class);

					Common.selectComboItem(ruangPerawatan, myKamar.getRuang());
				}
			}
		});

		tempatTidur.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				TempatTidur myTempatTidur = (TempatTidur) (tempatTidur.getAttribute("tempatTidur"));
				if (myTempatTidur != null) {
					Kamar myKamar = myTempatTidur.getKamar();

					Common.selectComboItem(kelasPerawatan, myKamar == null ? null : myKamar.getKelasPerawatan());

					Common.insertCombo(ruangPerawatan, "nama", "keterangan", Ruang.class);

					Common.selectComboItem(ruangPerawatan, myKamar == null ? null : myKamar.getRuang());

					Common.insertCombo(kamarPerawatan, "nama", "keterangan", Kamar.class,
							Restrictions.and(Restrictions.eq("ruang", myTempatTidur.getRuang()),
									Restrictions.eq("kelasPerawatan", myTempatTidur.getKelasPerawatan())));
					Common.selectComboItem(kamarPerawatan, myTempatTidur.getKamar());
				}
			}
		});

		// row.setStyle("border:0px;background: transparent;");
		// row.setParent(rows);
		// row.appendChild(new Label(ais.common.Common.getBahasaConfig("Pindah Ke Kelas Perawatan")));
		// row.appendChild(pindahKeKelasPerawatan = new Combobox());
		// Common.insertCombo(pindahKeKelasPerawatan, "nama",
		// KelasPerawatan.class,
		// Restrictions.ne("id", ConstantValues.kelasNormalId()));
		// Common.selectComboItem(pindahKeKelasPerawatan,
		// pendaftaran.getPindahKeKelasPerawatan());
		// pindahKeKelasPerawatan.setWidth("90%");

		return borderlayout;
	}

	private Borderlayout createPenanggungJawab(final Pendaftaran pendaftaran) throws Exception {
		Borderlayout borderlayout = new Borderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Grid grid = new Grid();
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();

		columns.setParent(grid);

		Column column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("30%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("70%");

		Rows rows = new Rows();
		rows.setParent(grid);

		Row row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Biaya Perawatan")));
		row.appendChild(biayaPerawatan = new Combobox());
		Comboitem comboitem = new Comboitem("Pribadi");
		comboitem.setValue("Pribadi");
		biayaPerawatan.appendChild(comboitem);
		comboitem = new Comboitem("Kantor");
		comboitem.setValue("Kantor");
		biayaPerawatan.appendChild(comboitem);
		comboitem = new Comboitem("Asuransi");
		comboitem.setValue("Asuransi");
		biayaPerawatan.appendChild(comboitem);
		Common.selectComboItem(biayaPerawatan, pendaftaran.getBiayaPerawatan());
		biayaPerawatan.setWidth("90%");

		// row = new Row();
		// row.setStyle("border:0px;background: transparent;");
		// row.setParent(rows);
		// row.appendChild(new Label(ais.common.Common.getBahasaConfig("Asuransi")));
		// row.appendChild(asuransi = new AmbilDataAsuransiBanbox());
		// asuransi.setAttribute("asuransi", pendaftaran.getAsuransi());
		// asuransi.setValue(pendaftaran.getAsuransi() == null ? "" :
		// pendaftaran
		// .getAsuransi().getNama());
		// asuransi.setWidth("90%");
		// // asuransi.setDisabled(true);
		//
		// Pasien pasien = (Pasien) PendaftaranRawatInapAction.this.pasien
		// .getAttribute("pasien");
		// asuransi.setValue(pasien == null || pasien.getAsuransi() == null ? ""
		// : pasien.getAsuransi().getNama());
		// asuransi.setAttribute("asuransi",
		// pasien == null ? null : pasien.getAsuransi());
		//
		// EventListener eventListener = new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// asuransi.setDisabled(true);
		// String bp = (String) (biayaPerawatan.getSelectedItem() == null ? null
		// : biayaPerawatan.getSelectedItem().getValue());
		// if (bp != null) {
		// if (bp.equalsIgnoreCase("Asuransi")) {
		// asuransi.setDisabled(false);
		// }
		// }
		//
		// }
		// };
		// eventListener.onEvent(null);
		// biayaPerawatan.addEventListener("onChange", eventListener);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama Penanggung Jawab")));
		row.appendChild(namaPenjamin = new MyTextbox(
				pendaftaran.getNamaPenjamin() == null ? "" : pendaftaran.getNamaPenjamin()));
		namaPenjamin.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Pekerjaan Penanggung Jawab")));
		row.appendChild(pekerjaanPenjamin = new Combobox());
		comboitem = new Comboitem("BUMN");
		comboitem.setValue("BUMN");
		pekerjaanPenjamin.appendChild(comboitem);
		comboitem = new Comboitem("BURUH");
		comboitem.setValue("BURUH");
		pekerjaanPenjamin.appendChild(comboitem);
		comboitem = new Comboitem("PNS");
		comboitem.setValue("PNS");
		pekerjaanPenjamin.appendChild(comboitem);
		comboitem = new Comboitem("PROFESI");
		comboitem.setValue("PROFESI");
		pekerjaanPenjamin.appendChild(comboitem);
		comboitem = new Comboitem("PURNABAKTI");
		comboitem.setValue("PURNABAKTI");
		pekerjaanPenjamin.appendChild(comboitem);
		comboitem = new Comboitem("SWASTA");
		comboitem.setValue("SWASTA");
		pekerjaanPenjamin.appendChild(comboitem);
		comboitem = new Comboitem("TGG KELUARGA");
		comboitem.setValue("TGG KELUARGA");
		pekerjaanPenjamin.appendChild(comboitem);
		comboitem = new Comboitem("TNI");
		comboitem.setValue("TNI");
		pekerjaanPenjamin.appendChild(comboitem);
		comboitem = new Comboitem("POLRI");
		comboitem.setValue("POLRI");
		pekerjaanPenjamin.appendChild(comboitem);
		comboitem = new Comboitem("WIRASWASTA");
		comboitem.setValue("WIRASWASTA");
		pekerjaanPenjamin.appendChild(comboitem);
		comboitem = new Comboitem("IBU RUMAH TANGGA");
		comboitem.setValue("IBU RUMAH TANGGA");
		pekerjaanPenjamin.appendChild(comboitem);
		Common.selectComboItem(pekerjaanPenjamin, pendaftaran.getPekerjaanPenjamin());
		pekerjaanPenjamin.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Pendidikan Penanggunga Jawab")));
		row.appendChild(pendidikanPenjamin = new Combobox());
		Common.insertCombo(pendidikanPenjamin, "nama", Pendidikan.class);
		Common.selectComboItem(pendidikanPenjamin, pendaftaran.getPendidikanPenjamin());
		pendidikanPenjamin.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Alamat Penanggung Jawab")));
		row.appendChild(alamatPenjamin = new MyTextbox(
				pendaftaran.getAlamatPenjamin() == null ? "" : pendaftaran.getAlamatPenjamin()));
		alamatPenjamin.setWidth("90%");

		return borderlayout;
	}

	private Borderlayout createPendaftar(final Pendaftaran pendaftaran) {
		Borderlayout borderlayout = new Borderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Grid grid = new Grid();
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();

		columns.setParent(grid);

		Column column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("30%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("70%");

		Rows rows = new Rows();
		rows.setParent(grid);

		Row row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Pendaftar")));
		row.appendChild(pendaftar = new Combobox());
		Comboitem comboitem = new Comboitem("Ayah");
		comboitem.setValue("Ayah");
		pendaftar.appendChild(comboitem);
		comboitem = new Comboitem("Ibu");
		comboitem.setValue("Ibu");
		pendaftar.appendChild(comboitem);
		comboitem = new Comboitem("Suami");
		comboitem.setValue("Suami");
		pendaftar.appendChild(comboitem);
		comboitem = new Comboitem("Istri");
		comboitem.setValue("Istri");
		pendaftar.appendChild(comboitem);
		comboitem = new Comboitem("Adik");
		comboitem.setValue("Adik");
		pendaftar.appendChild(comboitem);
		comboitem = new Comboitem("Kakak");
		comboitem.setValue("Kakak");
		pendaftar.appendChild(comboitem);
		comboitem = new Comboitem("Anak");
		comboitem.setValue("Anak");
		pendaftar.appendChild(comboitem);
		comboitem = new Comboitem("Tetangga");
		comboitem.setValue("Tetangga");
		pendaftar.appendChild(comboitem);
		comboitem = new Comboitem("Teman");
		comboitem.setValue("Teman");
		pendaftar.appendChild(comboitem);
		comboitem = new Comboitem("Lain-lain");
		comboitem.setValue("Lain-lain");
		pendaftar.appendChild(comboitem);
		Common.selectComboItem(pendaftar, pendaftaran.getPendaftar());
		pendaftar.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama Jelas Pendaftar")));
		row.appendChild(namaPendaftar = new MyTextbox(
				pendaftaran.getNamaPendaftar() == null ? "" : pendaftaran.getNamaPendaftar()));
		namaPendaftar.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Alamat Pendaftar")));
		row.appendChild(alamatPendaftar = new MyTextbox(
				pendaftaran.getAlamatPendaftar() == null ? "" : pendaftaran.getAlamatPendaftar()));
		alamatPendaftar.setWidth("90%");
		alamatPendaftar.setRows(4);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Telpon Pendaftar")));
		row.appendChild(telpPendaftar = new MyTextbox(
				pendaftaran.getTelpPendaftar() == null ? "" : pendaftaran.getTelpPendaftar()));
		telpPendaftar.setWidth("90%");

		return borderlayout;
	}

	@SuppressWarnings("deprecation")
	private Borderlayout createMain(final Pendaftaran pendaftaran) throws Exception {
		Borderlayout borderlayout = new Borderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Grid grid = new Grid();
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();

		columns.setParent(grid);

		Column column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("20%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("30%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("20%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("30%");

		Rows rows = new Rows();
		rows.setParent(grid);

		Row row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Ambil dari Rawat Jalan")));
		row.appendChild(transferDaripendaftaran = new AmbilDataPendaftaranRawatJalanBanbox(false));
		transferDaripendaftaran.setAttribute("pendaftaran", pendaftaran.getTransferDaripendaftaran());
		transferDaripendaftaran.setValue(pendaftaran.getTransferDaripendaftaran() == null ? ""
				: pendaftaran.getTransferDaripendaftaran().getKode());
		transferDaripendaftaran.setWidth("90%");

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Pendaftaran pendaftaranRajal = (Pendaftaran) transferDaripendaftaran.getAttribute("pendaftaran");
				if (pendaftaranRajal != null) {
					perubahanPasienListener
							.onEvent(new Event("", transferDaripendaftaran, pendaftaranRajal.getPasien()));
				}
			}
		};

		transferDaripendaftaran.setEventListener(eventListener);

		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Sumber Pasien")));
		row.appendChild(sumberPasien);
		Common.selectComboItem(sumberPasien, pendaftaran.getSumberPasien());
		sumberPasien.setWidth("90%");

		final Button tambahPasienBaru = new ais.ui.util.MyToolbarbuttonConfig("Pasien Baru", "/img/user_male.png");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Pasien Baru")));

		Hbox hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(baru = new Checkbox());
		baru.setChecked(pendaftaran.getBaru() == null ? null : pendaftaran.getBaru());
		baru.addEventListener("onCheck", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				tambahPasienBaru.setDisabled(!baru.isChecked());
			}
		});

		tambahPasienBaru.setDisabled(true);
		hbox.appendChild(tambahPasienBaru);
		tambahPasienBaru.addEventListener(Events.ON_CLICK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				PasienAction.onExternalAdd(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Window window = (Window) arg0.getTarget();
						Pasien pasien = (Pasien) arg0.getData();

						perubahanPasienListener.onEvent(new Event("", tambahPasienBaru, pasien));

						window.detach();
					}
				});

			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Booking")));
		row.appendChild(bookingRegistrasi = new AmbilDataBookingRegistrasiBanbox());
		bookingRegistrasi.setValue(
				pendaftaran.getBookingRegistrasi() == null ? "" : pendaftaran.getBookingRegistrasi().getKode());
		bookingRegistrasi.setAttribute("bookingRegistrasi", pendaftaran.getBookingRegistrasi());
		bookingRegistrasi.setWidth("90%");
		bookingRegistrasi.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				BookingRegistrasi myBookingRegistrasi = (BookingRegistrasi) bookingRegistrasi
						.getAttribute("bookingRegistrasi");
				if (myBookingRegistrasi != null) {
					perubahanPasienListener.onEvent(new Event("", bookingRegistrasi, myBookingRegistrasi.getPasien()));

					pendaftaran.setBookingRegistrasi(myBookingRegistrasi);
					myBookingRegistrasi.setPendaftaran(pendaftaran);

					Object[] datas = new Object[] { myBookingRegistrasi.getJadwalDokter(),
							myBookingRegistrasi.getDilayaniTanggal(), true, pendaftaran };
					jadwalPerawatanEventListener.onEvent(new Event("", bookingRegistrasi, datas));

				}
			}
		});

		kode = new Label(pendaftaran.getKode());
		perubahanPasienListener = CommonPendaftaranUtil.initPendaftaran(rows, kode, pendaftaran, new EventListener() {

			@SuppressWarnings("rawtypes")
			@Override
			public void onEvent(Event arg0) throws Exception {
				Map data = (Map) arg0.getData();
				Pendaftaran pendaftaran = (Pendaftaran) data.get("pendaftaran");
				pasien = pendaftaran.getPasien();
				tanggalPendaftaran = pendaftaran.getTanggalPendaftaran();
				asuransi = pendaftaran.getAsuransi();
				keterangan = pendaftaran.getKeterangan();
			}
		});

		jadwalPerawatanEventListener = CommonPendaftaranUtil.initJadwalPemeriksaan(rows, pendaftaran, jenis,
				new EventListener() {

					@SuppressWarnings("rawtypes")
					@Override
					public void onEvent(Event arg0) throws Exception {
						Map data = (Map) arg0.getData();
						if (data != null) {
							poly = (Poly) data.get("poly");
							subpoly = (Poly) data.get("subpoly");
							shiftDokter = (JadwalDokter) data.get("jadwalDokter");
							dokter = (Dokter) data.get("dokter");
							dilayaniTanggal = (Date) data.get("dilayaniTanggal");
						} else {
							poly = null;
							subpoly = null;
							shiftDokter = null;
							dokter = null;
							dilayaniTanggal = null;
						}
					}
				});

		row = new Row();
		row.setAttribute("hide", "no");
		ais.ui.util.ZkCompat.setSpans(row, "4");
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Html("<hr>"));

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Dokter Pengirim")));
		row.appendChild(namaDokterPengirim = new MyTextbox(
				pendaftaran.getNamaDokterPengirim() == null ? "" : "" + pendaftaran.getNamaDokterPengirim()));
		namaDokterPengirim.setWidth("90%");

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label("Pernah dirawat di / tanggal"));
		pernahDirawatDi = new MyTextbox(
				pendaftaran.getPernahDirawatDi() == null ? "" : pendaftaran.getPernahDirawatDi());

		tanggalPernahDirawat = new Datebox(
				pendaftaran.getTanggalPernahDirawat() == null ? null : pendaftaran.getTanggalPernahDirawat());

		row.appendChild(new Hbox(new Component[] { pernahDirawatDi, tanggalPernahDirawat }));

		CommonSirs.initLokasiDanShift(pendaftaran.getLokasi() == null ? myLokasi : pendaftaran.getLokasi(),
				pendaftaran.getShift(), rows, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Object[] o = (Object[]) arg0.getData();
						myLokasi = (Lokasi) o[0];
						myShift = (Shift) o[1];

						if (kode.getValue().trim().equals("") && myLokasi != null) {
							String mykode = Common.generateCode(Pendaftaran.class, 10, "REG-RANAP", myLokasi);
							kode.setValue(mykode);
						}
					}
				});

		eventListener.onEvent(null);

		if (kode.getValue().trim().equals("") && myLokasi != null) {
			kode.setValue(Common.generateCode(Pendaftaran.class, 10, "REG-RANAP", myLokasi));
		}

		return borderlayout;
	}

	private void init(final Pendaftaran pendaftaran) throws Exception {

		pasien = pendaftaran.getPasien();
		tanggalPendaftaran = pendaftaran.getTanggalPendaftaran();
		asuransi = pendaftaran.getAsuransi();
		keterangan = pendaftaran.getKeterangan();
		poly = pendaftaran.getPoly();
		subpoly = pendaftaran.getSubpoly();
		shiftDokter = pendaftaran.getJadwalDokter();
		dokter = pendaftaran.getDokter();
		dilayaniTanggal = pendaftaran.getDilayaniTanggal();

		myLokasi = pendaftaran.getLokasi();
		myShift = pendaftaran.getShift();

		this.pendaftaran = pendaftaran;
		Common.clear(tambahData);
		Borderlayout borderlayout = new Borderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Tabbox tabbox = new Tabbox();
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");
		tabbox.setParent(center);

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		Tab tabPasien = new Tab("Data Pasien");
		tabPasien.setParent(tabs);
		Tab tabPendaftar = new Tab("Data Pendaftar");
		tabPendaftar.setParent(tabs);
		Tab tabPenanggungjawab = new Tab("Data Penanggungjawab");
		tabPenanggungjawab.setParent(tabs);
		Tab tabPerawatan = new Tab("Ruang Perawatan");
		tabPerawatan.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		final Tabpanel tabpanelPasien = new ais.ui.util.MyTabpanel();
		tabpanelPasien.setParent(tabpanels);
		tabpanelPasien.appendChild(createMain(pendaftaran));

		final Tabpanel tabpanelPendaftar = new ais.ui.util.MyTabpanel();
		tabpanelPendaftar.setParent(tabpanels);
		tabPendaftar.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelPendaftar.getChildren().size() == 0) {
					tabpanelPendaftar.appendChild(createPendaftar(pendaftaran));
				}

			}
		});
		// Fileupload
		final Tabpanel tabpanelPenanggungJawab = new ais.ui.util.MyTabpanel();
		tabpanelPenanggungJawab.setParent(tabpanels);
		tabPenanggungjawab.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelPenanggungJawab.getChildren().size() == 0) {
					tabpanelPenanggungJawab.appendChild(createPenanggungJawab(pendaftaran));
				}

			}
		});

		final Tabpanel tabpanelRuangPerawatan = new ais.ui.util.MyTabpanel();
		tabpanelRuangPerawatan.setParent(tabpanels);
		tabpanelRuangPerawatan.appendChild(createRuangPerawatan(pendaftaran));
		tabPerawatan.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelPenanggungJawab.getChildren().size() == 0) {
					tabpanelPenanggungJawab.appendChild(createPenanggungJawab(pendaftaran));
				}

			}
		});

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(south);

		add.setParent(toolbar);
		Toolbarbutton save = new ais.ui.util.MyToolbarbuttonConfig("Simpan Pendaftaran Rawat Inap", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
				}
			}
		});
		save.setParent(toolbar);

		Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("Batalkan Pendaftaran Rawat Inap",
				"/img/delete.gif");
		button.setVisible(delete);
		button.setTooltiptext("Batalkan Pendaftaran Pasien Rawat Inap");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (pendaftaran != null && pendaftaran.getId() != null) {
					onDelete(pendaftaran);

				} else {
					MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin membatalkan pendaftaran rawat inap ini? Data yang telah dimasukkan tidak akan tersimpan.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										init(new Pendaftaran());
									}
								}
							});

				}
			}
		});
		button.setParent(toolbar);

		Toolbarbutton monitor = new ais.ui.util.MyToolbarbuttonConfig("Monitor Tempat Tidur", "/img/monitor sims.png");
		monitor.setTooltiptext("Simpan");
		monitor.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onMonitorKeberadaanTempatTidur(event);
			}
		});
		monitor.setParent(toolbar);

		borderlayout.setParent(tambahData);
		tambahData.getLinkedTab().setSelected(true);
	}

	public boolean onSave(Event event) throws Exception {

		if (myLokasi == null) {
			MyMessageboxConfig.show("Lokasi belum diisi. Mohon lengkapi data lokasi terlebih dahulu. Langkah yang dapat dilakukan: (1) periksa kolom lokasi; (2) pilih lokasi yang sesuai; (3) simpan kembali data.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (myShift == null) {
			MyMessageboxConfig.show("Shift belum diisi. Mohon lengkapi data shift terlebih dahulu. Langkah yang dapat dilakukan: (1) periksa kolom shift; (2) pilih shift yang sesuai; (3) simpan kembali data.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (tanggalPendaftaran == null) {
			MyMessageboxConfig.show("Tanggal pendaftaran belum diisi. Mohon lengkapi tanggal pendaftaran terlebih dahulu. Langkah yang dapat dilakukan: (1) periksa kolom tanggal pendaftaran; (2) isi tanggal pendaftaran yang sesuai; (3) simpan kembali data.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (pasien == null) {
			MyMessageboxConfig.show("Data pasien belum diisi. Mohon lengkapi data pasien terlebih dahulu. Langkah yang dapat dilakukan: (1) periksa kolom pasien; (2) pilih pasien yang sesuai; (3) simpan kembali data.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (kelasPerawatan == null || kelasPerawatan.getSelectedItem() == null) {
			MyMessageboxConfig.show("Kelas perawatan belum diisi. Mohon lengkapi kelas perawatan terlebih dahulu. Langkah yang dapat dilakukan: (1) periksa kolom kelas perawatan; (2) pilih kelas perawatan yang sesuai; (3) simpan kembali data.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (tempatTidur == null || tempatTidur.getAttribute("tempatTidur") == null) {
			MyMessageboxConfig.show("Tempat tidur belum diisi. Mohon lengkapi data tempat tidur terlebih dahulu. Langkah yang dapat dilakukan: (1) periksa kolom tempat tidur; (2) pilih tempat tidur yang tersedia; (3) simpan kembali data.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (poly == null) {
			MyMessageboxConfig.show("Poli belum diisi. Mohon lengkapi data poli terlebih dahulu. Langkah yang dapat dilakukan: (1) periksa kolom poli; (2) pilih poli yang sesuai; (3) simpan kembali data.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (shiftDokter == null) {
			MyMessageboxConfig.show("Shift tenaga medis belum diisi. Mohon lengkapi shift tenaga medis terlebih dahulu. Langkah yang dapat dilakukan: (1) periksa kolom shift tenaga medis; (2) pilih shift tenaga medis yang sesuai; (3) simpan kembali data.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (dilayaniTanggal == null) {
			MyMessageboxConfig.show("Tanggal pelayanan belum diisi. Mohon lengkapi tanggal pelayanan terlebih dahulu. Langkah yang dapat dilakukan: (1) periksa kolom tanggal pelayanan; (2) isi tanggal pelayanan yang sesuai; (3) simpan kembali data.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (pendaftaran.getId() != null) {
			pendaftaran = (Pendaftaran) session.load(Pendaftaran.class, pendaftaran.getId());

		}

		if (kode.getValue().trim().equals("")) {
			kode.setValue(Common.generateCode(Pendaftaran.class, 8));
		}

		pendaftaran.setDilayaniTanggal(dilayaniTanggal);

		pendaftaran.setBookingRegistrasi((BookingRegistrasi) bookingRegistrasi.getAttribute("bookingRegistrasi"));

		pendaftaran.setDokter((Dokter) dokter);

		pendaftaran.setTransferDaripendaftaran((Pendaftaran) (transferDaripendaftaran == null ? null
				: transferDaripendaftaran.getAttribute("pendaftaran")));

		pendaftaran.setAsuransi((Asuransi) (asuransi));

		pendaftaran.setAlamatPendaftar(alamatPendaftar == null ? "" : alamatPendaftar.getValue());
		pendaftaran.setTelpPendaftar(telpPendaftar == null ? "" : telpPendaftar.getValue());
		pendaftaran.setNamaPendaftar(namaPendaftar == null ? "" : namaPendaftar.getValue());
		pendaftaran.setNamaDokterPengirim(namaDokterPengirim == null ? "" : namaDokterPengirim.getValue());
		pendaftaran.setPendaftar((String) (pendaftar == null || pendaftar.getSelectedItem() == null ? null
				: pendaftar.getSelectedItem().getValue()));
		pendaftaran.setPernahDirawatDi(pernahDirawatDi == null ? "" : pernahDirawatDi.getValue());
		pendaftaran.setTanggalPernahDirawat(tanggalPernahDirawat == null ? null : tanggalPernahDirawat.getValue());
		pendaftaran.setSumberPasien((String) (sumberPasien == null || sumberPasien.getSelectedItem() == null ? null
				: sumberPasien.getSelectedItem().getValue()));
		pendaftaran.setNamaPenjamin(namaPenjamin == null ? "" : namaPenjamin.getValue());
		pendaftaran.setAlamatPenjamin(alamatPenjamin == null ? "" : alamatPenjamin.getValue());
		pendaftaran.setPekerjaanPenjamin(
				(String) (pekerjaanPenjamin == null || pekerjaanPenjamin.getSelectedItem() == null ? null
						: pekerjaanPenjamin.getSelectedItem().getValue()));
		pendaftaran.setPendidikanPenjamin(
				(Pendidikan) (pendidikanPenjamin == null || pendidikanPenjamin.getSelectedItem() == null ? null
						: pendidikanPenjamin.getSelectedItem().getValue()));
		pendaftaran
				.setTempatTidur((TempatTidur) (tempatTidur == null ? null : tempatTidur.getAttribute("tempatTidur")));
		pendaftaran
				.setBiayaPerawatan((String) (biayaPerawatan == null || biayaPerawatan.getSelectedItem() == null ? null
						: biayaPerawatan.getSelectedItem().getValue()));
		pendaftaran.setKamarPerawatan((Kamar) (kamarPerawatan == null || kamarPerawatan.getSelectedItem() == null ? null
				: kamarPerawatan.getSelectedItem().getValue()));
		pendaftaran.setKelasPerawatan(
				(KelasPerawatan) (kelasPerawatan == null || kelasPerawatan.getSelectedItem() == null ? null
						: kelasPerawatan.getSelectedItem().getValue()));
		pendaftaran.setRuangPerawatan((Ruang) (ruangPerawatan == null || ruangPerawatan.getSelectedItem() == null ? null
				: ruangPerawatan.getSelectedItem().getValue()));

		pendaftaran.setNomorAntrian(null);

		pendaftaran.setPoly(null);
		pendaftaran.setBaru(baru == null ? null : baru.isChecked());
		pendaftaran.setJenis(jenis);
		pendaftaran.setTanggalPendaftaran(tanggalPendaftaran);

		pendaftaran.setPasien((Pasien) (pasien));
		pendaftaran.setPasienKomunitas(pendaftaran.getPasien());

		pendaftaran.setKode(kode.getValue());
		pendaftaran.setKeterangan(keterangan);
		pendaftaran.setTbmuser(Common.getCurrentUser());

		pendaftaran.setLokasi(myLokasi);
		pendaftaran.setShift(myShift);

		pendaftaran.setPoly((Poly) poly);
		pendaftaran.setSubpoly((Poly) (subpoly));
		pendaftaran.setJadwalDokter(shiftDokter);

		Integer antrian = CommonPendaftaranUtil.generateNomorAntrian(pendaftaran, pendaftaran.getJadwalDokter());
		pendaftaran.setNomorAntrian(antrian);

		if (pendaftaran.getId() != null) {
			Common.refreshUpdate(session, pendaftaran);
		} else {
			pendaftaran.setIndex(Common.generateMaxByLokasi(Pendaftaran.class, myLokasi) + 1);
			String mykode = Common.generateCode(Pendaftaran.class, 10, "REG-RANAP", myLokasi);
			kode.setValue(mykode);
			pendaftaran.setKode(mykode);
			session.save(pendaftaran);
		}

		TempatTidur tempatTidur = pendaftaran.getTempatTidur();
		if (tempatTidur != null) {
			tempatTidur.setTerisi(true);
			Common.refreshUpdate(session, tempatTidur);
		}

		BookingRegistrasi bookingRegistrasi = pendaftaran.getBookingRegistrasi();
		if (bookingRegistrasi != null) {
			session.refresh(bookingRegistrasi);
			bookingRegistrasi.setPendaftaran(pendaftaran);
			Common.refreshUpdate(session, bookingRegistrasi);
		}

		MyMessageboxConfig.show("Data pendaftaran rawat inap telah berhasil disimpan.", "Informasi", MyMessageboxConfig.OK,
				MyMessageboxConfig.INFORMATION, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.freeze(tambahData, true);
						add.setDisabled(false);
						onCetakStatusPasien(pendaftaran);
						RawatInapCalculationProcessor.checkPendaftaran(pendaftaran);
					}
				});

		return true;
	}

	private Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Pasien pasien = (Pasien) searchmr.getAttribute("pasien");

		Criterion criterion = searchalamat.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
				: Restrictions.ilike("pasien.alamat", searchalamat.getValue(), MatchMode.ANYWHERE);

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

		Criteria criteria = session.createCriteria(Pendaftaran.class);

		if (order)
			criteria.addOrder(Order.desc("tanggalPendaftaran"));

		criteria.createAlias("pasien", "pasien", Criteria.LEFT_JOIN)

				.createAlias("pasien.propinsi", "propinsi", Criteria.LEFT_JOIN)
				.createAlias("pasien.kota", "kota", Criteria.LEFT_JOIN)
				.createAlias("pasien.kecamatan", "kecamatan", Criteria.LEFT_JOIN)
				.createAlias("pasien.kelurahan", "kelurahan", Criteria.LEFT_JOIN)

				.add(criterion)

				.add((searchtelp == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.or(Restrictions.ilike("pasien.noTelp", searchtelp.getValue(), MatchMode.ANYWHERE),
						Restrictions.ilike("pasien.noHp", searchtelp.getValue(), MatchMode.ANYWHERE))))

				.add(Restrictions.eq("jenis", jenis))
				.add(searchbagian.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("bagian", searchbagian.getSelectedItem().getValue()))

				.add(searchkelas.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("kelasPerawatan", searchkelas.getSelectedItem().getValue()))
				.add(searchruang.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("ruangPerawatan", searchruang.getSelectedItem().getValue()))
				.add(searchkamar.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("kamarPerawatan", searchkamar.getSelectedItem().getValue()))
				.add(searchjenisPasien.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("jenisPasien", searchjenisPasien.getSelectedItem().getValue()))
				.add((searchbed == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchbed.getAttribute("tempatTidur") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tempatTidur", searchbed.getAttribute("tempatTidur"))))
				.add((searchkode == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.ilike("kode", searchkode.getValue(), MatchMode.ANYWHERE)))
				.add((searchnama == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.ilike("pasien.nama", searchnama.getValue(), MatchMode.ANYWHERE)))
				.add(pasien == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("pasien", pasien));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Pendaftaran> pendaftaran = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(pendaftaran);
		grid.setRowRenderer(new PendaftaranRenderer());
		grid.setModel(strset);

		grid.renderAll();

	}

	public Boolean checkKodePendaftaran() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(Pendaftaran.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("kode", kode.getValue().trim()))
				.add(this.pendaftaran.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.pendaftaran.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void onCetakStatusPasien(Pendaftaran pendaftaran) {

		try {
			Pasien pasien = pendaftaran.getPasien();
			File myfile = new File(Sessions.getCurrent().getWebApp().getRealPath("/report/temp") + "/barcode_"
					+ pasien.getKode() + ".png");
			myfile.getParentFile().mkdirs();
			myfile.createNewFile();

			BarcodeCommon.generateCRCode(pasien.getKode(), myfile);

			String barcode = myfile.getAbsolutePath();

			Map parameters = new HashMap();
			Common.insertProperty(Pasien.class, pasien, parameters, "");
			Common.insertProperty(Pendaftaran.class, pendaftaran, parameters, "pendaftaran");
			parameters.put("mybarcode", barcode);
			parameters.put("rm", pasien.getKode());
			parameters.put("keluarga", pasien.getNama_penanggungjawab());
			parameters.put("kesatuan", pasien.getJenisPasienDinas() == null ? ""
					: pasien.getJenisPasienDinas().trim().equals(Pasien.TNI_AD.getId()) ? Pasien.TNI_AD.getName()
							: pasien.getJenisPasienDinas().trim().equals(Pasien.TNI_AL.getId())
									? Pasien.TNI_AL.getName()
									: pasien.getJenisPasienDinas().trim().equals(Pasien.TNI_AU.getId())
											? Pasien.TNI_AU.getName()
											: pasien.getJenisPasienDinas().trim().equals(Pasien.PNS.getId())
													? Pasien.PNS.getName()
													: "");
			parameters.put("pangkat", pasien.getPangkat() == null ? "" : pasien.getPangkat());
			parameters.put("nip", pasien.getNip() == null ? "" : pasien.getNip());
			parameters.put("telp", (pasien.getNoTelp() == null ? "" : pasien.getNoTelp()) + " / "
					+ (pasien.getNoHp() == null ? "" : pasien.getNoHp()));
			parameters.put("status_perkawinan", pasien.getStatusPerkawinan());
			parameters.put("jenis_kelamin", pasien.getJenisKelamin());
			parameters.put("agama", pasien.getAgama() == null ? "" : pasien.getAgama().getNama());
			parameters.put("pendidikan", pasien.getPendidikan() == null ? "" : pasien.getPendidikan().getNama());
			parameters.put("pekerjaan", pasien.getPekerjaan());

			Date tangggalKunjunganpertama = (Date) HibernateUtil.currentSession().createCriteria(Pendaftaran.class)
					.add(Restrictions.eq("pasien", pasien)).setProjection(Projections.min("tanggalPendaftaran"))
					.setMaxResults(1).uniqueResult();

			parameters.put("kunjungan",
					tangggalKunjunganpertama == null ? "" : Common.dateFormat3.get().format(tangggalKunjunganpertama));
			parameters.put("ttd", "Jakarta, " + Common.dateFormat2.get().format(new Date()));
			parameters.put("nama", pasien.getNama() == null ? "" : pasien.getNama().trim());
			parameters.put("ttl", (pasien.getTempatLahir() == null ? "" : pasien.getTempatLahir()) + " / "
					+ (pasien.getTanggalLahir() == null ? "" : Common.dateFormat2.get().format(pasien.getTanggalLahir())));
			parameters.put("alamat", pasien.getAlamatLengkap());
			parameters.put("wkt_reg", pendaftaran.getTanggalPendaftaran() == null ? ""
					: Common.dateFormat3.get().format(pendaftaran.getTanggalPendaftaran()));

			parameters.put("noreg", pendaftaran.getKode());
			parameters.put("kelas",
					pendaftaran.getKelasPerawatan() == null ? "" : pendaftaran.getKelasPerawatan().getNama());
			parameters.put("ruang",
					pendaftaran.getRuangPerawatan() == null ? "" : pendaftaran.getRuangPerawatan().getNama());
			parameters.put("kamar",
					pendaftaran.getKamarPerawatan() == null ? "" : pendaftaran.getKamarPerawatan().getNama());
			parameters.put("bed", pendaftaran.getTempatTidur() == null ? "" : pendaftaran.getTempatTidur().getNama());

			File file = Report.generateFileReport("sirs/data_identitas_pasien_ranap", Report.PDF, parameters,
					"sirs/data_identitas_pasien_ranap", new Date(), Sessions.getCurrent().getWebApp());

			Report.tampil(file, parameters, "sirs/data_identitas_pasien_ranap");

		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}

	public void onMonitorKeberadaanTempatTidur(Event event) throws Exception {
		MonitorDataTempatTidurHelper monitorDataTempatTidurHelper = new MonitorDataTempatTidurHelper();
		monitorDataTempatTidurHelper.setTitle("Monitor Keberadaan Tempat Tidur");
		monitorDataTempatTidurHelper.setClosable(true);
		monitorDataTempatTidurHelper.setWidth("750px");
		monitorDataTempatTidurHelper.setHeight("95%");
		monitorDataTempatTidurHelper.setParent(page.getFirstRoot());
		monitorDataTempatTidurHelper.onModal();
	}

}
