package ais.action.master.sirs;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
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
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.East;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.sirs.helper.AmbilDataPasienBanbox;
import ais.action.master.sirs.helper.AmbilDataPendaftaranSemuaBanbox;
import ais.action.master.sirs.helper.AmbilDataTempatTidurBanbox;
import ais.action.master.sirs.util.CommonPendaftaranUtil;
import ais.action.report.Report;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.CommonSirs;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Ruang;
import ais.database.model.asset.Lokasi;
import ais.database.model.sirs.Bagian;
import ais.database.model.sirs.JenisPasien;
import ais.database.model.sirs.Kamar;
import ais.database.model.sirs.KelasPerawatan;
import ais.database.model.sirs.Pasien;
import ais.database.model.sirs.Pendaftaran;
import ais.database.model.sirs.Shift;
import ais.database.model.sirs.Tindakan;
import ais.database.model.sirs.TransaksiMedis;
import ais.database.model.sirs.TransaksiMedisDetail;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyTextbox;

public class TransaksiPaketAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Grid grid;
	private Paging paging;

	private Tabpanel tambahData;

	private MyTextbox searchkode;
	private MyTextbox searchmr;
	private MyTextbox searchnama;
	private Combobox searchbagian;

	private Combobox searchkelas;
	private Combobox searchruang;
	private Combobox searchkamar;
	private AmbilDataTempatTidurBanbox searchbed;

	private MyTextbox kode;
	private AmbilDataPasienBanbox pasien;
	private AmbilDataPendaftaranSemuaBanbox pendaftaran;
	private Combobox bagian;
	private MyTextbox keterangan;
	private MyDatebox tanggalTransaksi;

	private MyTextbox nama;
	private Label umur;
	private MyTextbox alamat;
	private Datebox ttl;
	private Combobox jenisPasien;
	private Combobox jenisKelamin;

	private boolean edit = false;
	private boolean delete = false;

	private TransaksiMedis transaksi;
	private Toolbarbutton add;
	private Toolbarbutton simpan;
	private Toolbarbutton validasi;

	private Center center = new Center();

	private String SUMBER = TransaksiMedis.SUMBER_LAIN;
	protected Set<Tindakan> pakets;

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
		myLokasi = Common.getCurrentLokasi();

		if (execution.getParameter("sumber") != null && !execution.getParameter("sumber").trim().equalsIgnoreCase("")) {
			SUMBER = execution.getParameter("sumber").trim();
		}

		System.out.println("SUMBER => " + SUMBER + " =============================");

		Common.insertCombo(searchkelas, "nama", "keterangan", KelasPerawatan.class);
		Common.insertCombo(searchruang, "nama", "keterangan", Ruang.class);

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

		Common.insertCombo(jenisPasien = new Combobox(), "nama", JenisPasien.class);

		Common.insertCombo(bagian = new Combobox(), "nama", "keterangan", Bagian.class);

		Common.insertCombo(searchbagian, "nama", "keterangan", Bagian.class);

		add = new ais.ui.util.MyToolbarbuttonConfig("Transaksi "
				+ (SUMBER.equals(TransaksiMedis.SUMBER_LAIN) ? "" : StringUtils.capitalize(SUMBER.toLowerCase())) + " Baru",
				"/img/user_male_add.png");
		add.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				init(new TransaksiMedis());

			}
		});
		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		init(new TransaksiMedis());
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	class TransaksiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final TransaksiMedis transaksi = (TransaksiMedis) arg1;

			if (transaksi.getValidasi() == null || !transaksi.getValidasi()) {
				arg0.setStyle("background-color:yellow;");
			} else {
				arg0.setStyle("background-color:#DBFDF3;");
			}

			Pasien pasien = transaksi.getPasien();

			new ais.action.master.sirs.detail.TransaksiTindakanDetailAction(transaksi).setParent(arg0);

			RevisiHelper.createNewRevisi(TransaksiMedis.class, transaksi, transaksi.getKode()).setParent(arg0);
			// new Label(pasien == null ? "" :
			// pasien.getKode()).setParent(arg0);
			new Label(pasien == null ? transaksi.getNama() : pasien.getNama()).setParent(arg0);
			new Label(transaksi.getTanggalTransaksi() == null ? ""
					: Common.dateFormat3.get().format(transaksi.getTanggalTransaksi())).setParent(arg0);

			new Label(pasien == null ? "" : pasien.getAlamatLengkap()).setParent(arg0);
			new Label(transaksi.getKelasPerawatan() == null ? "" : transaksi.getKelasPerawatan().getNama())
					.setParent(arg0);

			String bed = "";
			if (transaksi.getPendaftaran() != null && transaksi.getPendaftaran().getTempatTidur() != null) {
				bed = (transaksi.getPendaftaran().getRuangPerawatan() == null ? ""
						: transaksi.getPendaftaran().getRuangPerawatan().getNama())
						+ " - "
						+ (transaksi.getPendaftaran().getKamarPerawatan() == null ? ""
								: transaksi.getPendaftaran().getKamarPerawatan().getNama())
						+ " - " + (transaksi.getPendaftaran().getTempatTidur() == null ? ""
								: transaksi.getPendaftaran().getTempatTidur().getNama());
			}

			new Label(bed).setParent(arg0);

			new Label(transaksi.getBebas() ? "Ya" : "Tidak").setParent(arg0);
			new Label(transaksi.getValidasi() == null || !transaksi.getValidasi() ? "Belum" : "Ya").setParent(arg0);
			new Label(transaksi.getLunas() ? "Ya" : "Belum").setParent(arg0);

			Hbox toolbar = new Hbox();

			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak Transaksi");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {
					onCetak(transaksi);
				}

			});
			button.setParent(toolbar);

			button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/edit.gif");
			button.setTooltiptext("Rubah Data");
			button.setVisible(edit && (transaksi.getLunas() == null || !transaksi.getLunas()));
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					TransaksiMedis myTransaksi = (TransaksiMedis) HibernateUtil.currentSession().createCriteria(TransaksiMedis.class)
							.add(Restrictions.idEq(transaksi.getId())).uniqueResult();
					init(myTransaksi);
				}

			});
			button.setParent(toolbar);

			button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete && (transaksi.getLunas() == null || !transaksi.getLunas()));
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					onDelete(transaksi);

				}
			});
			button.setParent(toolbar);
			toolbar.setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new TransaksiMedis());
	}

	private EventListener perubahanPasienListener = new EventListener() {

		private void doExecute(final Pendaftaran pendaftaran) {
			Pasien pasien = pendaftaran.getPasien();

			TransaksiPaketAction.this.pasien.setValue(pasien == null ? "" : pasien.getKode().trim());
			nama.setValue(pasien == null ? "" : pasien.getNama());

			if (pasien.getTanggalLahir() != null) {
				Calendar tahunSkr = Calendar.getInstance();
				Calendar tahunLahir = Calendar.getInstance();
				tahunLahir.setTime(pasien.getTanggalLahir());
				Integer myumur = tahunSkr.get(Calendar.YEAR) - tahunLahir.get(Calendar.YEAR);
				umur.setValue(myumur + " thn");
			} else {
				umur.setValue("");
			}

			alamat.setValue(pasien == null ? "" : pasien.getAlamatLengkap());
			ttl.setValue(pasien == null ? null : pasien.getTanggalLahir());

			jenisKelamin
					.setValue(pasien == null ? "" : pasien.getJenisKelamin() == null ? "" : pasien.getJenisKelamin());

			komunitas.setValue(pendaftaran == null ? ""
					: pendaftaran.getKomunitass().toString().replaceAll("\\[", "").replaceAll("\\]", ""));

			asuransi.setValue(pendaftaran == null || pendaftaran.getAsuransi() == null ? ""
					: pendaftaran.getAsuransi().toString());

			Common.selectComboItem(kelasPerawatan, pendaftaran.getKelasPerawatan() == null ? ConstantValues.kelasNormal
					: pendaftaran.getKelasPerawatan());
			Common.selectComboItem(jenisPasien, pasien.getJenisPasien());

		}

		@Override
		public void onEvent(Event arg0) throws Exception {
			Pasien pasien = (Pasien) TransaksiPaketAction.this.pasien.getAttribute("pasien");

			Pendaftaran myPendaftaran = (Pendaftaran) pendaftaran.getAttribute("pendaftaran");
			if (myPendaftaran == null) {
				myPendaftaran = (Pendaftaran) HibernateUtil.currentSession().createCriteria(Pendaftaran.class)
						.add(Restrictions.eq("pasien", pasien)).add(Restrictions.eq("lunas", false))
						.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
				if (myPendaftaran == null) {
					MyMessageboxConfig.show(
							"Mohon maaf, pasien ini belum melakukan pendaftaran. Silakan centang pilihan Bebas apabila merupakan pembeli umum. Langkah yang dapat dilakukan: (1) lakukan pendaftaran pasien terlebih dahulu; (2) atau centang pilihan Bebas untuk pembeli umum.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}
				pendaftaran.setAttribute("pendaftaran", myPendaftaran);
				pendaftaran.setValue(myPendaftaran == null ? "" : myPendaftaran.getKode() + "-" + pasien.getNama());
				pendaftaran.setDisabled(true);
			} else {
				myPendaftaran = (Pendaftaran) HibernateUtil.currentSession().createCriteria(Pendaftaran.class)
						.add(Restrictions.idEq(myPendaftaran.getId())).uniqueResult();
			}

			if (myPendaftaran != null && myPendaftaran.getId() != null) {
				pakets = myPendaftaran.getPakets();
				System.out.println("pakets = " + pakets);
				if (!pakets.isEmpty()) {
					System.out.println("masuk pakets = " + pakets);
					CommonPendaftaranUtil.transaksiDetailPaketFinal(eastInfoPasien, pakets, myPendaftaran);
					add.setDisabled(true);
					simpan.setDisabled(false);
				}
			} else {
				MyMessageboxConfig.show("Mohon maaf, pendaftaran yang Bapak/Ibu pilih bukan merupakan pendaftaran paket. Langkah yang dapat dilakukan: (1) pilih pendaftaran yang memiliki paket perawatan; (2) atau tandai sebagai pasien Bebas apabila memang sesuai.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return;
			}

			final TransaksiMedis mytransaksi = (TransaksiMedis) HibernateUtil.currentSession().createCriteria(TransaksiMedis.class)
					.add(Restrictions.eq("sumber", SUMBER)).add(Restrictions.eq("jenisTransaksi", TransaksiMedis.TRX_PAKET))
					.add(Restrictions.eq("pendaftaran", myPendaftaran)).addOrder(Order.desc("id")).setMaxResults(1)
					.uniqueResult();

			if (mytransaksi != null && transaksi.getId() == null) {
				init(mytransaksi);
			} else {
				doExecute(myPendaftaran);
			}

		}
	};

	private Combobox kelasPerawatan;
	private East eastInfoPasien;
	private Label komunitas;
	private Label asuransi;

	@SuppressWarnings("deprecation")
	private Borderlayout createMain(final TransaksiMedis transaksi) throws Exception {
		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setStyle("border:0px;background: transparent;");
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
		ais.ui.util.ZkCompat.setSpans(row, "1,3");
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode Transaksi")));
		String mykode = transaksi.getKode();
		row.appendChild(kode = new MyTextbox(mykode));
		kode.setWidth("90%");
		kode.setReadonly(true);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Ambil dari Pendaftaran")));
		row.appendChild(pendaftaran = new AmbilDataPendaftaranSemuaBanbox(false));
		pendaftaran.setAttribute("pendaftaran", transaksi.getPendaftaran());
		pendaftaran.setValue(transaksi.getPendaftaran() == null ? "" : transaksi.getPendaftaran().getKode());
		pendaftaran.setWidth("90%");

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Pendaftaran mypendaftaran = (Pendaftaran) pendaftaran.getAttribute("pendaftaran");
				if (mypendaftaran != null) {
					pasien.setValue(mypendaftaran.getPasien() == null ? "" : mypendaftaran.getPasien().getKode());
					pasien.setAttribute("pasien", mypendaftaran.getPasien());
					pasien.setDisabled(true);
					Common.selectComboItem(kelasPerawatan,
							mypendaftaran.getKelasPerawatan() == null ? ConstantValues.kelasNormal
									: mypendaftaran.getKamarPerawatan());
					perubahanPasienListener.onEvent(arg0);
				}
			}
		};

		pendaftaran.setEventListener(eventListener);

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Pasien")));

		row.appendChild(pasien = new AmbilDataPasienBanbox());
		pasien.setValue(transaksi.getPasien() == null ? "" : transaksi.getPasien().getKode());
		pasien.setAttribute("pasien", transaksi.getPasien());
		pasien.setEventListener(perubahanPasienListener);
		pasien.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Transaksi")));
		row.appendChild(tanggalTransaksi = new MyDatebox(
				transaksi.getTanggalTransaksi() == null ? new Date() : transaksi.getTanggalTransaksi()));
		tanggalTransaksi.setFormat(Common.dateFormat3.get().toPattern());
		tanggalTransaksi.setCols(30);
		tanggalTransaksi.setDisabled(true);

		Pasien pasien = transaksi.getPasien();

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama Pasien")));
		row.appendChild(nama = new MyTextbox(pasien == null ? "" : pasien.getNama()));
		nama.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Komunitas")));
		row.appendChild(komunitas = new Label(transaksi.getPendaftaran() == null ? ""
				: transaksi.getPendaftaran().getKomunitass().toString().replaceAll("\\[", "").replaceAll("\\]", "")));

		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Asuransi")));
		row.appendChild(asuransi = new Label(
				transaksi.getPendaftaran() == null || transaksi.getPendaftaran().getAsuransi() == null ? ""
						: transaksi.getPendaftaran().getAsuransi().toString()));

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Bagian (Unit)")));
		Common.selectComboItem(bagian,
				transaksi.getBagian() == null
						? (Common.getCurrentUser() != null && Common.getCurrentUser().getPegawai() != null
								? Common.getCurrentUser().getPegawai().getBagian()
								: null)
						: transaksi.getBagian());
		bagian.setDisabled(Common.getCurrentUser() != null && Common.getCurrentUser().getPegawai() != null
				&& Common.getCurrentUser().getPegawai().getBagian() != null);
		row.appendChild(bagian);
		bagian.setWidth("90%");

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Umur")));
		row.appendChild(
				umur = new Label(transaksi == null || transaksi.getUmur() == null ? "" : transaksi.getUmur() + " thn"));

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kelas Perawatan")));
		row.appendChild(kelasPerawatan = new Combobox());
		Common.insertCombo(kelasPerawatan, "nama", KelasPerawatan.class);
		Common.selectComboItem(kelasPerawatan,
				transaksi.getKelasPerawatan() == null ? ConstantValues.kelasNormal : transaksi.getKelasPerawatan());
		kelasPerawatan.setWidth("90%");
		kelasPerawatan.setDisabled(true);

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Lahir")));
		row.appendChild(ttl = new Datebox(pasien == null ? null : pasien.getTanggalLahir()));
		ttl.setWidth("90%");
		ttl.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (ttl.getValue() == null) {
					return;
				}
				Calendar tahunSkr = Calendar.getInstance();
				Calendar tahunLahir = Calendar.getInstance();
				tahunLahir.setTime(ttl.getValue());
				Integer myumur = tahunSkr.get(Calendar.YEAR) - tahunLahir.get(Calendar.YEAR);
				umur.setValue(myumur + " thn");
			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Kelamin")));
		row.appendChild(jenisKelamin = new Combobox());
		Comboitem comboitem = new Comboitem("Laki-laki");
		comboitem.setValue("Laki-laki");
		jenisKelamin.appendChild(comboitem);
		comboitem = new Comboitem("Perempuan");
		comboitem.setValue("Perempuan");
		jenisKelamin.appendChild(comboitem);
		Common.selectComboItem(jenisKelamin, transaksi.getJenisKelamin());
		jenisKelamin.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(""));
		row.appendChild(new Label(""));

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Pasien")));
		row.appendChild(jenisPasien);
		Common.selectComboItem(jenisPasien, transaksi.getJenisPasien());
		jenisPasien.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Keterangan")));
		row.appendChild(keterangan = new MyTextbox(transaksi.getKeterangan() == null ? "" : transaksi.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Alamat")));
		row.appendChild(alamat = new MyTextbox(pasien == null ? "" : pasien.getAlamatLengkap()));
		alamat.setWidth("90%");
		alamat.setRows(3);

		CommonSirs.initLokasiDanShift(transaksi.getLokasi() == null ? myLokasi : transaksi.getLokasi(),
				transaksi.getShift(), rows, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Object[] o = (Object[]) arg0.getData();
						myLokasi = (Lokasi) o[0];
						myShift = (Shift) o[1];
					}
				});

		eventListener.onEvent(null);
		return borderlayout;
	}

	private void init(final TransaksiMedis transaksi) throws Exception {
		this.transaksi = transaksi;

		Common.clear(tambahData);
		Borderlayout borderlayout = new Borderlayout();

		Common.clear(center);
		center.setStyle("border:0px;background: transparent;");
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		center.appendChild(createMain(transaksi));
		if (transaksi.getId() != null) {
			Common.freeze(center, true);
		}

		eastInfoPasien = new East();
		ais.ui.util.ZkCompat.setFlex(eastInfoPasien, true);
		eastInfoPasien.setParent(borderlayout);
		eastInfoPasien.setWidth("65%");

		if (transaksi != null && transaksi.getId() != null) {
			pakets = transaksi.getPendaftaran() == null ? new HashSet<Tindakan>()
					: transaksi.getPendaftaran().getPakets();
			System.out.println("pakets = " + pakets);
			if (!pakets.isEmpty()) {
				System.out.println("masuk pakets = " + pakets);
				CommonPendaftaranUtil.transaksiDetailPaketFinal(eastInfoPasien, pakets, transaksi.getPendaftaran());
				add.setDisabled(true);
				simpan.setDisabled(false);
			}
		}

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(south);

		add.setParent(toolbar);
		simpan = new ais.ui.util.MyToolbarbuttonConfig(
				"Simpan Transaksi "
						+ (SUMBER.equals(TransaksiMedis.SUMBER_LAIN) ? "" : StringUtils.capitalize(SUMBER.toLowerCase())),
				"/img/save.gif");
		simpan.setTooltiptext("Simpan");
		simpan.setDisabled(true);
		add.setDisabled(false);
		simpan.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				if (myLokasi == null) {

					MyMessageboxConfig.show("Mohon Bapak/Ibu memilih salah satu lokasi terlebih dahulu karena data lokasi wajib ditentukan. Langkah yang dapat dilakukan: (1) pilih lokasi pada daftar yang tersedia; (2) lanjutkan kembali proses Anda.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}

				if (onSave(event)) {

					MyMessageboxConfig.show("Alhamdulillah, data transaksi telah berhasil disimpan. Terima kasih, Bapak/Ibu.", "Informasi", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									onCetak(transaksi);
									validasi.setVisible(true);
								}
							});
					add.setDisabled(false);
					simpan.setDisabled(true);
					add.setDisabled(false);
					onSearchDefault(null);
					Common.initPaging(paging, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							onSearchDefault(null);
						}
					});
					//
					// Common.freeze(center, true);
					// Common.freeze(transaksiDetailAction, true);
				}
			}
		});
		simpan.setParent(toolbar);

		validasi = new ais.ui.util.MyToolbarbuttonConfig(
				"Validasi Transaksi "
						+ (SUMBER.equals(TransaksiMedis.SUMBER_LAIN) ? "" : StringUtils.capitalize(SUMBER.toLowerCase())),
				"/img/Ok-icon_kecil.png");
		validasi.setTooltiptext("Validasi");
		validasi.setVisible(transaksi.getId() != null);
		validasi.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				if (myLokasi == null) {

					MyMessageboxConfig.show("Mohon Bapak/Ibu memilih salah satu lokasi terlebih dahulu karena data lokasi wajib ditentukan. Langkah yang dapat dilakukan: (1) pilih lokasi pada daftar yang tersedia; (2) lanjutkan kembali proses Anda.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}

				MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin memvalidasi transaksi ini? Setelah divalidasi, transaksi akan dianggap sah dan tidak dapat diubah kembali.", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = new Integer(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {

									if (onSave(event)) {

										if (!CommonPendaftaranUtil.validasiTransaksiDetailPaketFinal(pakets,
												transaksi)) {
											return;
										}

										MyMessageboxConfig.show("Alhamdulillah, data transaksi telah berhasil divalidasi. Terima kasih, Bapak/Ibu.", "Informasi",
												MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

													@Override
													public void onEvent(Event arg0) throws Exception {
														onCetak(transaksi);

													}
												});

										add.setDisabled(false);
										validasi.setDisabled(true);
										simpan.setDisabled(true);
										add.setDisabled(false);
										onSearchDefault(null);
										Common.freeze(center, true);
										Common.freeze(eastInfoPasien, true);

									}

								}
							}
						});
			}
		});
		validasi.setParent(toolbar);

		Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig(
				"Batalkan Transaksi "
						+ (SUMBER.equals(TransaksiMedis.SUMBER_LAIN) ? "" : StringUtils.capitalize(SUMBER.toLowerCase())),
				"/img/delete.gif");
		button.setVisible(delete);
		button.setTooltiptext("Batalkan Transaksi");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (transaksi != null && transaksi.getId() != null) {
					onDelete(transaksi);

				} else {
					MyMessageboxConfig.show("Apakah Bapak/Ibu benar-benar yakin ingin membatalkan transaksi ini? Perlu diketahui bahwa transaksi yang telah dibatalkan tidak dapat dikembalikan.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										init(new TransaksiMedis());
									}
								}
							});

				}
			}
		});
		button.setParent(toolbar);

		add.setDisabled(false);

		button.setParent(toolbar);

		borderlayout.setParent(tambahData);
		tambahData.getLinkedTab().setSelected(true);
	}

	public void onDelete(final TransaksiMedis transaksi) throws Exception {

		MyMessageboxConfig.show("Apakah Bapak/Ibu benar-benar yakin ingin membatalkan transaksi ini? Perlu diketahui bahwa transaksi yang telah dibatalkan tidak dapat dikembalikan.", "Pertanyaan",
				MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event event) throws Exception {
						int i = new Integer(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							try {
								Session session = HibernateUtil.currentSession();
								session.createSQLQuery(
										"delete from sirs.detail_transaksi_layanan where transaksi_detail in (select id from sirs.transaksi_medis_detail where transaksi = "
												+ transaksi.getId() + ");")
										.executeUpdate();

								session.createSQLQuery(
										"delete from sirs.detail_transaksi_pasien where racikan_detail in (select id from sirs.racikan_detail where racikan in (select id from sirs.racikan where transaksi_detail in (select id from sirs.transaksi_medis_detail where transaksi = "
												+ transaksi.getId() + ")));")
										.executeUpdate();

								String sql = "delete from sirs.racikan_detail where racikan in (select id from sirs.racikan where transaksi_detail in (select id from sirs.transaksi_medis_detail where transaksi = "
										+ transaksi.getId() + "));";
								session.createSQLQuery(sql).executeUpdate();

								session.createSQLQuery(
										"update sirs.transaksi_medis_detail set racikan = null where transaksi = "
												+ transaksi.getId() + ";")
										.executeUpdate();

								session.createSQLQuery(
										"delete from sirs.racikan where transaksi_detail in (select id from sirs.transaksi_medis_detail where transaksi = "
												+ transaksi.getId() + ");")
										.executeUpdate();

								session.createSQLQuery(
										"delete from sirs.transaksi_medis_detail where transaksi_detail in (select id from sirs.transaksi_medis_detail where transaksi = "
												+ transaksi.getId() + ");")
										.executeUpdate();

								List<TransaksiMedisDetail> transaksiDetails = session.createCriteria(TransaksiMedisDetail.class)
										.add(Restrictions.eq("transaksi", transaksi)).list();

								for (TransaksiMedisDetail transaksiDetail : transaksiDetails) {
									Common.refreshDelete(session, transaksiDetail);
								}

								Common.refreshDelete(session, transaksi);
								init(new TransaksiMedis());

								onSearchDefault(event);
							} catch (Exception e) {
								ais.common.Common.tampilErrorJikaAdmin(e);
								MyMessageboxConfig.show(Common.pesan(
										"Mohon maaf, data ini tidak dapat dihapus karena masih berkaitan dengan data lainnya. Rincian teknis kesalahan: {V1}. Langkah yang dapat dilakukan: (1) hapus atau pindahkan terlebih dahulu seluruh data yang berkaitan; (2) periksa kembali keterkaitan antar data; (3) hubungi administrator apabila kendala masih berlanjut.",
												e.getMessage()));
							}

						}

					}
				});
	}

	public void onCetak(TransaksiMedis transaksi) throws Exception {
		if (pakets == null || pakets.isEmpty()) {
			final Map<String, Serializable> parameters = new HashMap<String, Serializable>();
			parameters.put("id", transaksi.getId());
			Report.generateWindowReport(Report.PDF, parameters, "sirs/transaksi_layanan", transaksi.getTanggalTransaksi());
		}
	}

	public boolean onSave(Event event) throws Exception {
		if (myLokasi == null) {
			MyMessageboxConfig.show("Mohon Bapak/Ibu melengkapi data Lokasi terlebih dahulu karena data ini wajib diisi. Langkah yang dapat dilakukan: (1) pilih Lokasi yang sesuai; (2) lanjutkan kembali proses penyimpanan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (myShift == null) {
			MyMessageboxConfig.show("Mohon Bapak/Ibu melengkapi data Shift terlebih dahulu karena data ini wajib diisi. Langkah yang dapat dilakukan: (1) pilih Shift yang sesuai; (2) lanjutkan kembali proses penyimpanan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (tanggalTransaksi.getValue() == null) {
			MyMessageboxConfig.show("Mohon Bapak/Ibu melengkapi Tanggal Transaksi terlebih dahulu karena data ini wajib diisi. Langkah yang dapat dilakukan: (1) tentukan Tanggal Transaksi; (2) lanjutkan kembali proses penyimpanan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (bagian.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon Bapak/Ibu melengkapi data Bagian (Unit) terlebih dahulu karena data ini wajib diisi. Langkah yang dapat dilakukan: (1) pilih Bagian (Unit) yang sesuai; (2) lanjutkan kembali proses penyimpanan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (transaksi.getId() != null) {
			transaksi = (TransaksiMedis) session.load(TransaksiMedis.class, transaksi.getId());

		}

		if (kode.getValue().trim().equals("")) {
			kode.setValue(Common.generateCode(TransaksiMedis.class, 8));
		}

		transaksi.setJenisTransaksi(TransaksiMedis.TRX_PAKET);
		transaksi.setSumber(SUMBER);
		transaksi.setNama(nama.getValue());
		transaksi.setAlamat(alamat.getValue());
		transaksi.setJenisKelamin(
				(String) (jenisKelamin.getSelectedItem() == null ||  jenisKelamin.getSelectedItem().getValue() == null ? "" : jenisKelamin.getSelectedItem().getValue().toString()));

		transaksi.setJenisPasien((JenisPasien) (jenisPasien.getSelectedItem() == null ? null
				: jenisPasien.getSelectedItem().getValue()));
		transaksi.setKelasPerawatan((KelasPerawatan) (kelasPerawatan.getSelectedItem() == null ? null
				: kelasPerawatan.getSelectedItem().getValue()));

		transaksi.setUmur(umur.getValue());
		transaksi.setTanggalTransaksi(tanggalTransaksi.getValue());

		transaksi.setPasien((Pasien) pasien.getAttribute("pasien"));
		transaksi.setBagian((Bagian) bagian.getSelectedItem().getValue());
		transaksi.setKode(kode.getValue());
		transaksi.setKeterangan(keterangan.getValue());
		transaksi.setLokasi(myLokasi);
		transaksi.setShift(myShift);
		transaksi.setPasien((Pasien) pasien.getAttribute("pasien"));
		transaksi.setPendaftaran((Pendaftaran) pendaftaran.getAttribute("pendaftaran"));
		transaksi.setBebas(false);

		if (transaksi.getId() != null) {
			Common.refreshUpdate(session, transaksi);
		} else {
			String mykode = Common.generateCode(TransaksiMedis.class, 8, "TRX", myLokasi);
			transaksi.setIndex(Common.generateMaxByLokasi(TransaksiMedis.class, myLokasi) + 1);
			kode.setValue(mykode);
			kode.setValue(transaksi.getKode());
			session.save(transaksi);
		}

		return true;
	}

	private Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(TransaksiMedis.class).add(Restrictions.eq("sumber", SUMBER))
				.add(Restrictions.eq("jenisTransaksi", TransaksiMedis.TRX_PAKET))
				.createAlias("pendaftaran", "pendaftaran", Criteria.LEFT_JOIN)
				.createAlias("pasien", "pasien", Criteria.LEFT_JOIN)
				.add(searchbagian.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("bagian", searchbagian.getSelectedItem().getValue()))

				.add(searchkelas.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("kelasPerawatan", searchkelas.getSelectedItem().getValue()))
				.add(searchruang.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("pendaftaran.ruangPerawatan", searchruang.getSelectedItem().getValue()))
				.add(searchkamar.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("pendaftaran.kamarPerawatan", searchkamar.getSelectedItem().getValue()))
				.add((searchbed == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchbed.getAttribute("tempatTidur") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("pendaftaran.tempatTidur", searchbed.getAttribute("tempatTidur"))))
				.add((searchkode == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.ilike("kode", searchkode.getValue(), MatchMode.ANYWHERE)))
				.add((searchnama == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchnama.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.ilike("pasien.nama", searchnama.getValue(), MatchMode.ANYWHERE),
								Restrictions.ilike("nama", searchnama.getValue(), MatchMode.ANYWHERE))))
				.add((searchmr == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchmr.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("pasien.kode", searchmr.getValue(), MatchMode.ANYWHERE)));
		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<TransaksiMedis> transaksi = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(transaksi);
		grid.setRowRenderer(new TransaksiRenderer());
		grid.setModel(strset);
		grid.renderAll();

	}

	public Boolean checkKodeTransaksi() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(TransaksiMedis.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("kode", kode.getValue().trim()))
				.add(this.transaksi.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.transaksi.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
