package ais.action.master.sirs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.East;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.North;
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
import ais.action.master.sirs.helper.AmbilDataTransaksiBanbox;
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
import ais.database.model.sirs.DetailTransaksiPasien;
import ais.database.model.sirs.Kamar;
import ais.database.model.sirs.KelasPerawatan;
import ais.database.model.sirs.Pasien;
import ais.database.model.sirs.Pendaftaran;
import ais.database.model.sirs.Shift;
import ais.database.model.sirs.TransaksiMedis;
import ais.database.model.sirs.TransaksiMedisDetail;
import ais.database.model.sirs.TransaksiRetur;
import ais.database.model.sirs.TransaksiReturDetail;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyTextbox;

public class TransaksiReturAction extends GenericAutowireComposer {

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

	private MyTextbox kodeRtr;
	private MyTextbox kode;
	private AmbilDataPasienBanbox pasien;
	private AmbilDataPendaftaranSemuaBanbox pendaftaran;
	private Combobox bagian;
	private MyTextbox keterangan;
	private MyDatebox tanggalTransaksi;
	private Label bebas;

	private MyTextbox nama;
	private Label umur;
	private Label alamat;
	private Label ttl;
	private Label jenisPasien;
	private Label jenisKelamin;

	private boolean edit = false;
	private boolean delete = false;

	private TransaksiMedis transaksi;
	private Toolbarbutton add;
	private Toolbarbutton simpan;
	private Toolbarbutton validasi;

	private Center center = new Center();

	private AmbilDataTransaksiBanbox ambilDataTransaksiBanbox;

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

		Common.insertCombo(bagian = new Combobox(), "nama", "keterangan", Bagian.class);

		Common.insertCombo(searchbagian, "nama", "keterangan", Bagian.class);

		add = new ais.ui.util.MyToolbarbuttonConfig("Retur Baru", "/img/user_male_add.png");
		add.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				init(new TransaksiRetur());

			}
		});
		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		init(new TransaksiRetur());
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
			final TransaksiRetur transaksiRetur = (TransaksiRetur) arg1;

			if (transaksiRetur.getValidasi() == null || !transaksiRetur.getValidasi()) {
				arg0.setStyle("background-color:yellow;");
			} else {
				arg0.setStyle("background-color:#DBFDF3;");
			}

			final TransaksiMedis transaksi = transaksiRetur.getTransaksi();

			Pasien pasien = transaksi.getPasien();

			new ais.action.master.sirs.detail.TransaksiReturDetailAction(transaksiRetur).setParent(arg0);

			RevisiHelper.createNewRevisi(TransaksiMedis.class, transaksi, transaksiRetur.getKode()).setParent(arg0);

			new Label(transaksiRetur.getTanggal() == null ? "" : Common.dateFormat3.get().format(transaksiRetur.getTanggal()))
					.setParent(arg0);

			new Label(pasien == null ? transaksi.getNama() : pasien.getNama()).setParent(arg0);

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

			new Label(transaksi.getBebas() ? "Ya" : "Belum").setParent(arg0);
			new Label(transaksiRetur.getValidasi() == null || !transaksiRetur.getValidasi() ? "Belum" : "Ya")
					.setParent(arg0);
			new Label(transaksiRetur.getLunas() ? "Ya" : "Belum").setParent(arg0);

			Hbox toolbar = new Hbox();

			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak Transaksi Retur");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {
					onCetak(transaksiRetur);
				}

			});
			button.setParent(toolbar);

			button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/edit.gif");
			button.setTooltiptext("Rubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(transaksiRetur);

				}

			});
			button.setParent(toolbar);

			button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					onDelete(transaksiRetur);

				}
			});
			button.setParent(toolbar);
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new TransaksiRetur());
	}

	private EventListener perubahanPasienListener = new EventListener() {

		@Override
		public void onEvent(Event arg0) throws Exception {
			Pasien pasien = (Pasien) TransaksiReturAction.this.pasien.getAttribute("pasien");
			TransaksiReturAction.this.pasien.setValue(pasien == null ? "" : pasien.getKode().trim());
			nama.setValue(pasien == null ? "" : pasien.getNama());

			umur.setValue(pasien == null ? "" : pasien.getUmur().toString() + " thn");

			alamat.setValue(pasien == null ? "" : pasien.getAlamatLengkap());
			ttl.setValue(pasien == null ? null : Common.dateFormat2.get().format(pasien.getTanggalLahir()));

			jenisKelamin
					.setValue(pasien == null ? "" : pasien.getJenisKelamin() == null ? "" : pasien.getJenisKelamin());

			jenisPasien.setValue(transaksi == null || transaksi.getJenisPasien() == null ? ""
					: transaksi.getJenisPasien().getNama());

			bebas.setValue(transaksi != null && transaksi.getBebas() ? "Ya" : "Tidak");

		}
	};

	private TransaksiDetailAction transaksiDetailAction;
	private TransaksiRetur transaksiRetur;
	private Label kelasPerawatan;

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
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nomor Transaksi")));
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
		row.appendChild(kelasPerawatan = new Label(
				transaksi.getKelasPerawatan() == null ? "" : transaksi.getKelasPerawatan().getNama()));

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Lahir")));
		row.appendChild(ttl = new Label(pasien == null ? null : Common.dateFormat2.get().format(pasien.getTanggalLahir())));
		ttl.setWidth("90%");

		row = new Row();
		ais.ui.util.ZkCompat.setSpans(row, "1,3");
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Alamat")));
		row.appendChild(alamat = new Label(pasien == null ? "" : pasien.getAlamatLengkap()));

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Kelamin")));
		row.appendChild(jenisKelamin = new Label(transaksi.getJenisKelamin()));

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Pasien")));
		row.appendChild(jenisPasien = new Label(
				transaksi.getJenisPasien() == null ? "" : transaksi.getJenisPasien().toString()));
		jenisPasien.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Keterangan")));
		row.appendChild(keterangan = new MyTextbox(transaksi.getKeterangan() == null ? "" : transaksi.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Pasien Bebas")));
		row.appendChild(bebas = new Label(transaksi != null && transaksi.getBebas() ? "Ya" : "Tidak"));

		CommonSirs.initLokasiDanShift(transaksiRetur.getLokasi() == null ? myLokasi : transaksiRetur.getLokasi(),
				transaksiRetur.getShift(), rows, new EventListener() {

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

	public class TransaksiDetailAction extends Borderlayout {

		/**
		 * 
		 */
		private static final long serialVersionUID = 5086031585928643232L;

		private Grid grid;
		private Footer total;
		private Footer totalRtr;

		public TransaksiDetailAction() throws Exception {
			super();
			display();
		}

		class TransaksiDetailRenderer extends ais.ui.util.MyRowRenderer {

			private Session session = HibernateUtil.currentSession();

			@Override
			public void render(final Row row, Object data) throws Exception {row.setValign("top");
				final TransaksiMedisDetail transaksiDetail = (TransaksiMedisDetail) data;
				final TransaksiReturDetail transaksiReturDetail = (TransaksiReturDetail) session
						.createCriteria(TransaksiReturDetail.class)
						.add(Restrictions.eq("transaksiDetail", transaksiDetail)).setMaxResults(1).uniqueResult();

				if (transaksiDetail.getRacikan() == null) {
					RevisiHelper
							.createNewRevisi(TransaksiMedisDetail.class, transaksiDetail,
									transaksiDetail.getItem() == null ? "" : transaksiDetail.getItem().getNama())
							.setParent(row);
					new Label(
							transaksiDetail.getItem() == null || transaksiDetail.getItem().getSatuanItem() == null ? ""
									: transaksiDetail.getItem().getSatuanItem().getNama())
							.setParent(row);
				} else {
					RevisiHelper
							.createNewRevisi(TransaksiMedisDetail.class, transaksiDetail,
									transaksiDetail.getRacikan() == null ? "" : transaksiDetail.getRacikan().getNama())
							.setParent(row);

					new Label(ais.common.Common.getBahasaConfig("Racikan")).setParent(row);

				}

				new Label(Common.numberFormat.get().format(transaksiDetail.getQty() == null ? 0.0 : transaksiDetail.getQty()))
						.setParent(row);

				final MyDoublebox jumlah;
				(jumlah = new MyDoublebox(transaksiReturDetail == null ? 0.0 : transaksiReturDetail.getQty()))
						.setParent(row);
				jumlah.setStyle("text-align:right");
				jumlah.setWidth("90%");
				jumlah.setDisabled(transaksiDetail.getRacikan() != null);
				jumlah.addEventListener(Events.ON_CHANGE, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						if (!TransaksiReturAction.this.onSave(arg0)) {
							return;
						}

						Session session = HibernateUtil.currentSession();

						TransaksiReturDetail myTransaksiReturDetail = transaksiReturDetail;
						if (myTransaksiReturDetail == null) {
							myTransaksiReturDetail = new TransaksiReturDetail();
							myTransaksiReturDetail.setItem(transaksiDetail.getItem());
							myTransaksiReturDetail.setKeterangan("Transaksi retur");
							myTransaksiReturDetail.setTanggal(new Date());
							myTransaksiReturDetail.setTransaksiDetail(transaksiDetail);
							myTransaksiReturDetail.setTransaksiRetur(transaksiRetur);
						}
						myTransaksiReturDetail.setAmount(transaksiDetail.getAmount());
						myTransaksiReturDetail.setQty(jumlah.getValue());
						session.saveOrUpdate((myTransaksiReturDetail));
						simpan.setDisabled(false);
						loadTotal();
					}
				});

			}
		}

		@SuppressWarnings("unchecked")
		public void loadData(Object value) {
			Session session = HibernateUtil.currentSession();
			List<TransaksiMedisDetail> transaksiDetails = transaksi == null || transaksi.getId() == null
					? new ArrayList<TransaksiMedisDetail>()
					: session.createCriteria(TransaksiMedisDetail.class).addOrder(Order.desc("id"))
							.add(Restrictions.eq("transaksi", transaksi)).list();

			ListModel strset = new SimpleListModel(transaksiDetails);
			grid.setRowRenderer(new TransaksiDetailRenderer());
			grid.setModel(strset);
			grid.renderAll();

			loadTotal();
		}

		public void loadTotal() {
			Session session = HibernateUtil.currentSession();
			Double mytotal = (Double) (transaksi == null || transaksi.getId() == null ? 0.0
					: session.createCriteria(TransaksiMedisDetail.class).setProjection(Projections.sum("qty"))
							.add(Restrictions.eq("transaksi", transaksi)).uniqueResult());

			Double mytotalRtr = (Double) (transaksiRetur == null || transaksiRetur.getId() == null ? 0.0
					: session.createCriteria(TransaksiReturDetail.class).setProjection(Projections.sum("qty"))
							.add(Restrictions.eq("transaksiRetur", transaksiRetur)).uniqueResult());

			if (mytotal == null) {
				mytotal = 0.0;
			}

			if (mytotalRtr == null) {
				mytotalRtr = 0.0;
			}
			System.out.println("mytotal => " + mytotal + ", mytotalRtr => " + mytotalRtr);

			total.setLabel(Common.numberFormat.get().format(mytotal));
			totalRtr.setLabel(Common.numberFormat.get().format(mytotalRtr));
		}

		private void display() throws Exception {

			setHeight("100%");
			setWidth("100%");
			setStyle("border:0px;background: transparent;");

			Center center = new Center();
			center.setParent(this);
			ais.ui.util.ZkCompat.setFlex(center, true);

			grid = new Grid();
			grid.setMold("paging");
			grid.setPageSize(25);
			grid.setParent(center);

			Columns columns = new Columns();

			columns.setParent(grid);

			Column column = new Column();
			column.setParent(columns);
			column.setLabel("Nama Item");
			column.setWidth("40%");

			column = new Column();
			column.setParent(columns);
			column.setLabel("Satuan");
			column.setWidth("30%");

			column = new Column();
			column.setParent(columns);
			column.setLabel("Qty Trns");
			column.setAlign("right");
			column.setWidth("10%");

			column = new Column();
			column.setParent(columns);
			column.setLabel("Qty Retur");
			column.setAlign("right");
			column.setWidth("20%");

			Foot foot = new Foot();
			foot.setParent(grid);

			foot.appendChild(new Footer("Total Qty"));
			foot.appendChild(new Footer());

			total = new Footer();
			total.setParent(foot);
			total.setStyle("font-weight:bold;font-size:15px;text-align:right;");

			totalRtr = new Footer();
			totalRtr.setParent(foot);
			totalRtr.setStyle("font-weight:bold;font-size:15px;text-align:right;");

			loadData(null);
		}
	}

	public void onCetak(TransaksiRetur transaksiRetur) throws Exception {
		final Map<String, Serializable> parameters = new HashMap<String, Serializable>();
		parameters.put("id", transaksiRetur.getId());
		Report.generateWindowReport(Report.PDF, parameters, "sirs/transaksi_retur_item", transaksiRetur.getTanggal());
	}

	private void init(final TransaksiRetur transaksiRetur) throws Exception {
		this.transaksiRetur = transaksiRetur;
		this.transaksi = transaksiRetur.getTransaksi();

		Common.clear(tambahData);
		final Borderlayout borderlayout = new Borderlayout();
		center.setParent(borderlayout);
		final East east = new East();
		east.setParent(borderlayout);

		final North north = new North();
		north.setStyle("border:0px;background: transparent;");
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Grid grid = new Grid();
		grid.setStyle("border:0px;background: transparent;");
		grid.setParent(north);
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
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Ambil data transaksi")));
		ambilDataTransaksiBanbox = new AmbilDataTransaksiBanbox(true, TransaksiMedis.SUMBER_APOTIK, TransaksiMedis.TRX_ITEM,
				false, false, null);
		ambilDataTransaksiBanbox.setAttribute("transaksi", transaksi);
		ambilDataTransaksiBanbox.setValue(transaksi == null || transaksi.getId() == null ? "" : transaksi.getKode());
		row.appendChild(ambilDataTransaksiBanbox);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nomor Retur")));
		String mykode = transaksiRetur.getKode();
		row.appendChild(kodeRtr = new MyTextbox(mykode));
		kodeRtr.setWidth("90%");
		kodeRtr.setReadonly(true);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.clear(center);
				Common.clear(east);
				transaksi = (TransaksiMedis) ambilDataTransaksiBanbox.getAttribute("transaksi");
				if (transaksi == null) {
					return;
				}

				center.setStyle("border:0px;background: transparent;");
				ais.ui.util.ZkCompat.setFlex(center, true);

				center.appendChild(createMain(transaksi));

				jenisPasien.setValue(transaksi == null || transaksi.getJenisPasien() == null ? ""
						: transaksi.getJenisPasien().getNama());

				kelasPerawatan
						.setValue(transaksi.getKelasPerawatan() == null ? "" : transaksi.getKelasPerawatan().getNama());

				bebas.setValue(transaksi != null && transaksi.getBebas() ? "Ya" : "Tidak");

				transaksiDetailAction = new TransaksiDetailAction();
				east.setStyle("border:0px;background: transparent;");
				ais.ui.util.ZkCompat.setFlex(east, true);
				east.appendChild(transaksiDetailAction);
				east.setWidth("500px");

				if (transaksiRetur.getId() != null) {
					Common.freeze(north, true);
				}
			}
		};

		ambilDataTransaksiBanbox.setEventListener(eventListener);
		eventListener.onEvent(null);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(south);

		add.setParent(toolbar);
		simpan = new ais.ui.util.MyToolbarbuttonConfig("Simpan Retur Transaksi", "/img/save.gif");
		simpan.setTooltiptext("Simpan");
		simpan.setDisabled(true);
		simpan.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				if (myLokasi == null) {

					MyMessageboxConfig.show("Mohon Bapak/Ibu memilih salah satu lokasi terlebih dahulu karena data lokasi wajib ditentukan. Langkah yang dapat dilakukan: (1) pilih lokasi pada daftar yang tersedia; (2) lanjutkan kembali proses Anda.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}

				if (onSave(event)) {

					MyMessageboxConfig.show("Alhamdulillah, data transaksi retur telah berhasil disimpan. Terima kasih, Bapak/Ibu.", "Informasi", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									onCetak(transaksiRetur);
									validasi.setVisible(true);
								}
							});
					add.setDisabled(false);
					simpan.setDisabled(true);
					onSearchDefault(null);
					Common.initPaging(paging, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							onSearchDefault(null);
						}
					});
					//
					// Common.freeze(north, true);
					// Common.freeze(center, true);
					// Common.freeze(transaksiDetailAction, true);
				}
			}
		});
		simpan.setParent(toolbar);

		validasi = new ais.ui.util.MyToolbarbuttonConfig("Validasi Retur Penjualan", "/img/Ok-icon_kecil.png");
		validasi.setTooltiptext("Validasi");
		validasi.setVisible(transaksiRetur.getId() != null);
		validasi.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				if (myLokasi == null) {

					MyMessageboxConfig.show("Mohon Bapak/Ibu memilih salah satu lokasi terlebih dahulu karena data lokasi wajib ditentukan. Langkah yang dapat dilakukan: (1) pilih lokasi pada daftar yang tersedia; (2) lanjutkan kembali proses Anda.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}

				MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin memvalidasi transaksi ini? Setelah divalidasi, transaksi akan dianggap sah dan tidak dapat diubah kembali.", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

							@SuppressWarnings("unchecked")
							@Override
							public void onEvent(Event event) throws Exception {

								if (onSave(event)) {

									Session session = HibernateUtil.currentSession();

									transaksiRetur.setValidasi(true);
									Common.refreshUpdate(session, (transaksiRetur));

									List<TransaksiReturDetail> transaksiReturDetails = session
											.createCriteria(TransaksiReturDetail.class)
											.add(Restrictions.eq("transaksiRetur", transaksiRetur)).list();
									session.createSQLQuery(
											"delete from sirs.transaksi_medis_detail where transaksi_retur_detail in (select id from sirs.transaksi_retur_detail where transaksi_retur = "
													+ transaksiRetur.getId() + ");")
											.executeUpdate();
									for (TransaksiReturDetail transaksiReturDetail : transaksiReturDetails) {

										if (transaksiReturDetail.getTransaksiDetail() == null
												|| transaksiReturDetail.getTransaksiDetail().getRacikan() == null
												|| transaksiReturDetail.getTransaksiDetail().getRacikan()
														.getId() == null) {
											DetailTransaksiPasien detailTransaksi = new DetailTransaksiPasien();
											detailTransaksi.setQtyBonus(0.0);
											detailTransaksi.setPasien(transaksiRetur.getTransaksi().getPasien());
											detailTransaksi
													.setTransaksiDetail(transaksiReturDetail.getTransaksiDetail());
											detailTransaksi.setTransaksiReturDetail(transaksiReturDetail);
											detailTransaksi.setItem(transaksiReturDetail.getItem());
											detailTransaksi.setAmount(transaksiReturDetail.getAmount() == null ? 0.0
													: transaksiReturDetail.getAmount());
											detailTransaksi.setKeterangan(
													"Retur: " + transaksiReturDetail.getItem().getNama());
											detailTransaksi.setKodeTransaksi(ConstantValues.apotikRetur);
											detailTransaksi.setLokasi(transaksiRetur.getLokasi());
											detailTransaksi.setQty(transaksiReturDetail.getQty() == null ? 0.0
													: transaksiReturDetail.getQty());
											detailTransaksi.setTanggal(new Date());

											detailTransaksi.setDiskonPersen(
													transaksiReturDetail.getTransaksiDetail().getDiskonPersen());
											detailTransaksi.setPajakPersen(
													transaksiReturDetail.getTransaksiDetail().getPajakPersen());

											CommonPendaftaranUtil.setDetailBiaya(detailTransaksi,
													transaksi.getKelasPerawatan());
										}

									}

									MyMessageboxConfig.show("Alhamdulillah, validasi retur telah berhasil disimpan. Terima kasih, Bapak/Ibu.", "Informasi", MyMessageboxConfig.OK,
											MyMessageboxConfig.INFORMATION, new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													onCetak(transaksiRetur);
													validasi.setVisible(true);
												}
											});
									add.setDisabled(false);
									simpan.setDisabled(true);
									validasi.setDisabled(true);
									onSearchDefault(null);
									Common.initPaging(paging, new EventListener() {
										@Override
										public void onEvent(Event arg0) throws Exception {
											onSearchDefault(null);
										}
									});

									Common.freeze(north, true);
									Common.freeze(center, true);
									Common.freeze(transaksiDetailAction, true);
								}

							}

						});
			}
		});
		validasi.setParent(toolbar);

		Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("Batalkan Retur Transaksi", "/img/delete.gif");
		button.setTooltiptext("Batalkan Transaksi");
		button.setVisible(delete);
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (transaksiRetur != null && transaksiRetur.getId() != null) {
					onDelete(transaksiRetur);

				} else {
					MyMessageboxConfig.show("Apakah Bapak/Ibu benar-benar yakin ingin membatalkan retur transaksi ini? Perlu diketahui bahwa retur yang telah dibatalkan tidak dapat dikembalikan.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										init(new TransaksiRetur());
									}
								}
							});

				}
			}
		});
		button.setParent(toolbar);

		button.setParent(toolbar);

		borderlayout.setParent(tambahData);
		tambahData.getLinkedTab().setSelected(true);
	}

	public void onDelete(final TransaksiRetur transaksiRetur) throws Exception {

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
										"delete from sirs.transaksi_medis_detail where transaksi_retur_detail in (select id from sirs.transaksi_retur_detail where transaksi_retur = "
												+ transaksiRetur.getId() + ");")
										.executeUpdate();
								List<TransaksiReturDetail> transaksiReturDetails = session
										.createCriteria(TransaksiReturDetail.class)
										.add(Restrictions.eq("transaksiRetur", transaksiRetur)).list();

								for (TransaksiReturDetail transaksiReturDetail : transaksiReturDetails) {
									session.delete(transaksiReturDetail);
								}

								Common.refreshDelete(session, transaksiRetur);
								init(new TransaksiRetur());

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

	public boolean onSave(Event event) throws Exception {

		if (myLokasi == null) {
			MyMessageboxConfig.show("Mohon Bapak/Ibu melengkapi data Lokasi terlebih dahulu karena data ini wajib diisi. Langkah yang dapat dilakukan: (1) pilih Lokasi yang sesuai; (2) lanjutkan kembali proses penyimpanan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (myShift == null) {
			MyMessageboxConfig.show("Mohon Bapak/Ibu melengkapi data Shift terlebih dahulu karena data ini wajib diisi. Langkah yang dapat dilakukan: (1) pilih Shift yang sesuai; (2) lanjutkan kembali proses penyimpanan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (ambilDataTransaksiBanbox.getAttribute("transaksi") == null) {
			MyMessageboxConfig.show("Mohon Bapak/Ibu melengkapi data Transaksi terlebih dahulu karena data ini wajib diisi. Langkah yang dapat dilakukan: (1) pilih data Transaksi yang akan diretur; (2) lanjutkan kembali proses penyimpanan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
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
		if (transaksiRetur.getId() != null) {
			transaksiRetur = (TransaksiRetur) session.load(TransaksiRetur.class, transaksiRetur.getId());

		}

		if (kode.getValue().trim().equals("")) {
			kode.setValue(Common.generateCode(TransaksiRetur.class, 8));
		}

		transaksi = (TransaksiMedis) ambilDataTransaksiBanbox.getAttribute("transaksi");

		transaksiRetur.setTransaksi(transaksi);
		transaksiRetur.setBagian((Bagian) bagian.getSelectedItem().getValue());
		transaksiRetur.setKode(kodeRtr.getValue());
		transaksiRetur.setKeterangan(keterangan.getValue());
		transaksiRetur.setLokasi(myLokasi);
		transaksiRetur.setShift(myShift);

		if (transaksiRetur.getId() != null) {
			Common.refreshUpdate(session, transaksiRetur);
		} else {
			String mykode = Common.generateCode(TransaksiRetur.class, 8, "TRX", myLokasi);
			transaksiRetur.setIndex(Common.generateMaxByLokasi(TransaksiRetur.class, myLokasi) + 1);
			kodeRtr.setValue(mykode);
			kodeRtr.setValue(transaksiRetur.getKode());
			transaksiRetur.setTanggal(new Date());
			session.save(transaksiRetur);
		}

		return true;
	}

	private Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(TransaksiRetur.class)
				.createCriteria("transaksi", Criteria.INNER_JOIN)
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
