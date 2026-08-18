package ais.action.master;

import java.util.Date;
import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.East;
import org.zkoss.zul.Image;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.West;

import ais.action.master.helper.AmbilDataNamaSekolahBanbox;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonMedia;
import ais.common.CommonOnSearchdefault;
import ais.database.dao.BiodataPegawaiDao;
import ais.database.dao.DaoFactory;
import ais.database.dao.PegawaiDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Agama;
import ais.database.model.Bank;
import ais.database.model.BiodataPegawai;
import ais.database.model.Dosen;
import ais.database.model.Pegawai;
import ais.database.model.StatusPegawai;
import ais.database.model.Tbmuser;
import ais.database.model.employ.GajiPokok;
import ais.database.model.employ.Insentif;
import ais.database.model.employ.KenaikanPangkat;
import ais.database.model.employ.Makan;
import ais.database.model.employ.Transport;
import ais.database.model.file.FotoPegawai;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

public class BiodataPegawaiAccountAction extends MyWindow {
	/**
	 * 
	 */
	private static final long serialVersionUID = 72558191307949087L;

	private CommonOnSearchdefault commonOnSearchdefault;

	private ManagingBiodataPegawai managingBiodataPegawai = new ManagingBiodataPegawai();
	private ManagingPegawai managingPegawai = new ManagingPegawai();
//	private ManagingAccountPegawai managingAccountPegawai = new ManagingAccountPegawai();

	private SatuanKerja satuanKerjaOnSession;

	private Boolean tampilBatal = true;

	public BiodataPegawaiAccountAction() throws Exception {
		super();
		init(null);
	}

	public BiodataPegawaiAccountAction(SatuanKerja satuanKerjaOnSession) throws Exception {
		super();
		this.satuanKerjaOnSession = satuanKerjaOnSession;
		init(null);
	}

	public BiodataPegawaiAccountAction(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init(null);
	}

	public BiodataPegawaiAccountAction(String title, String border, boolean closable, SatuanKerja satuanKerjaOnSession)
			throws Exception {
		super(title, border, closable);
		this.satuanKerjaOnSession = satuanKerjaOnSession;
		init(null);
	}

	public BiodataPegawaiAccountAction(Pegawai pegawai) throws Exception {
		super();
		init(pegawai);
	}

	public BiodataPegawaiAccountAction(Pegawai pegawai, SatuanKerja satuanKerjaOnSession) throws Exception {
		super();
		this.satuanKerjaOnSession = satuanKerjaOnSession;
		init(pegawai);
	}

	public BiodataPegawaiAccountAction(Pegawai pegawai, SatuanKerja satuanKerjaOnSession, Boolean tampilLogin,
			Boolean tampilBatal) throws Exception {
		super();
		this.satuanKerjaOnSession = satuanKerjaOnSession;
		this.tampilBatal = tampilBatal;
		init(pegawai);
	}

	public BiodataPegawaiAccountAction(Pegawai pegawai, String title, String border, boolean closable)
			throws Exception {
		super(title, border, closable);
		init(pegawai);

	}

	private void init(Pegawai pegawai) throws Exception {

		if (pegawai == null) {
			pegawai = Common.getCurrentUser().ambilPegawai();
		}
		if (pegawai == null) {
			Dosen dosen = Common.getCurrentUser().getDosen();
			if (dosen != null) {
				pegawai = (Pegawai) HibernateUtil.currentSession().createCriteria(Pegawai.class)
						.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
						.add(Restrictions.eq("dosen", dosen)).setMaxResults(1).uniqueResult();
			}

		}

		if (pegawai == null) {
			MyMessageboxConfig.show("Anda harus login sebagai pegawai", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		Tabpanel tabpanelPegawai = managingPegawai.init(pegawai);
		Tabpanel tabpanelBiodataPegawai = managingBiodataPegawai.preInit(pegawai);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);
		borderlayout.setStyle("border:0px;");

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(center);
		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);
		final MyTabConfig tab1;
		final MyTabConfig tab2;
		tabs.appendChild(tab1 = new MyTabConfig("Data Pegawai"));
		tabs.appendChild(tab2 = new MyTabConfig("Rincian Data Pegawai"));
		tab2.setVisible(false);

		tab2.addEventListener(Events.ON_CLICK, new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				Pegawai pegawai = managingPegawai.onSave(event);
				if (pegawai == null || pegawai.getId() == null) {
					tab1.setSelected(true);
				} else {
					managingBiodataPegawai.pegawai = pegawai;

				}
			}
		});

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);
		tabpanels.appendChild(tabpanelPegawai);
		tabpanels.appendChild(tabpanelBiodataPegawai);

		ais.ui.util.MyButtonTabbox.gantiTabboxNative(tabbox, new int[] { 1 });

		South south = new South();
		south.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(south, true);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.setVisible(tampilBatal);
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				BiodataPegawaiAccountAction.this.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				Pegawai pegawai = managingPegawai.onSave(event);
				if (pegawai != null && pegawai.getId() != null) {
					managingBiodataPegawai.pegawai = pegawai;

					boolean result = managingBiodataPegawai.onSave(event);
					if (result) {
						if (commonOnSearchdefault != null)
							commonOnSearchdefault.onSearchDefault(event);
					} else {
						MyMessageboxConfig.show("Data gagal disimpan!", "Peringatan", MyMessageboxConfig.OK,
								MyMessageboxConfig.EXCLAMATION);
					}

				}
			}
		});
		save.setParent(toolbar);

	}

	public void setCommonOnSearchdefault(CommonOnSearchdefault commonOnSearchdefault) {
		this.commonOnSearchdefault = commonOnSearchdefault;
	}

	public CommonOnSearchdefault getCommonOnSearchdefault() {
		return commonOnSearchdefault;
	}

	private class ManagingPegawai {
		private Textbox code;
		private Textbox mycode;
		private Textbox nama;
		private Textbox ktp;
		// private Combobox unitKerja;
		private Textbox alamat;
		private Textbox email;
		private Intbox usiaPensiun;
		private Textbox telp;
		private Combobox kelamin;
		private Textbox tempatlahir;
		private MyDatebox tanggallahir;
		private Textbox pangkat;
		private Textbox jabatan;
		private Textbox spesialisasi1;
		private Textbox spesialisasi2;
		private Textbox spesialisasi3;

		private Combobox agama;
		private Combobox statusPerkawinan;
		private Textbox alamatJalan;
		private Textbox alamatKelurahan;
		private Textbox alamatKecamatan;
		private Textbox alamatKabupaten;
		private Textbox alamatPropinsi;

		// private Textbox panggilan;
		// private Textbox gelarDepan;
		// private Textbox gelarBelakang;
		// private Textbox darah;
		// private Textbox hp;
		private Combobox bank;
		private Textbox norek;
		private Textbox karis;
		private Textbox askes;
		private Textbox taspen;
//		private Textbox karpeg;
		private Textbox npwp;

		private Textbox keteranganBadanTinggi;
		private Textbox keteranganBadanBerat;
		private Textbox keteranganBadanRambut;
		private Textbox keteranganBadanBentukMuka;
		private Textbox keteranganBadanWarnaKulit;
		private Textbox keteranganBadanCiriKhas;
		private Textbox keteranganBadanCacat;
		private MyDatebox tanggalmasuk;

		private Textbox hobi;

		private Pegawai pegawai;
		private Image foto;
		private Combobox statusPegawai;
		private AmbilDataSatuanKerjaBanbox satuanKerja;
		private Textbox keterangan;

		public ManagingPegawai() {

			kelamin = new Combobox();
			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel("Laki-laki");
			comboitem.setValue("Laki-laki");
			kelamin.appendChild(comboitem);
			comboitem = new MyComboitemConfig();
			comboitem.setLabel("Perempuan");
			comboitem.setValue("Perempuan");
			kelamin.appendChild(comboitem);

			statusPerkawinan = new Combobox();
			comboitem = new MyComboitemConfig();
			comboitem.setLabel("Belum kawin");
			comboitem.setValue("Belum kawin");
			statusPerkawinan.appendChild(comboitem);

			comboitem = new MyComboitemConfig();
			comboitem.setLabel("Kawin");
			comboitem.setValue("Kawin");
			statusPerkawinan.appendChild(comboitem);

			comboitem = new MyComboitemConfig();
			comboitem.setLabel("Janda");
			comboitem.setValue("Janda");
			statusPerkawinan.appendChild(comboitem);

			comboitem = new MyComboitemConfig();
			comboitem.setLabel("Duda");
			comboitem.setValue("Duda");
			statusPerkawinan.appendChild(comboitem);

		}

		public Tabpanel init(final Pegawai pegawai) throws Exception {
			this.pegawai = pegawai;
			Tabpanel panel = new ais.ui.util.MyTabpanel();
			panel.setWidth("100%");
			panel.setHeight("100%");
			Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
			borderlayout.setParent(panel);
			borderlayout.setStyle("border:0px;");

			East east = new East();
			east.setStyle("border:0px;");
			ais.ui.util.ZkCompat.setFlex(east, true);
			east.setWidth("370px");
			east.setParent(borderlayout);

			MyGrid grid = new MyGrid();
			grid.setWidth("100%");
			grid.setParent(east);
			grid.setWidth("100%");
			grid.setHeight("100%");

			Columns columns = new Columns();
			columns.setParent(grid);
			MyColumnConfig column = new MyColumnConfig();
			column.setWidth("40%");
			columns.appendChild(column);
			column = new MyColumnConfig();
			columns.appendChild(column);

			Rows rows = new Rows();
			rows.setParent(grid);

			Date sekarang = WaktuUtil.getDate();
			List<KenaikanPangkat> kenaikanPangkats = pegawai.ambilKenaikanPangkatData(sekarang);

			for (KenaikanPangkat kenaikanPangkat : kenaikanPangkats) {
				if (kenaikanPangkat.getKenaikanPangkatFungsional()) {
					MyFormRow jabatanfungsionalrow = new MyFormRow();
					jabatanfungsionalrow.setParent(rows);
					jabatanfungsionalrow.appendChild(new MyLabelConfig("Jabatan Fungsional"));
					Label jabatanFungsional;
					jabatanfungsionalrow.appendChild(jabatanFungsional = new Label());
					jabatanFungsional
							.setValue(kenaikanPangkat == null || kenaikanPangkat.getJabatanFungsional() == null ? ""
									: kenaikanPangkat.getJabatanFungsional().getNama());

					MyFormRow jabatanfungsionalrowtgl = new MyFormRow();
					jabatanfungsionalrowtgl.setParent(rows);
					jabatanfungsionalrowtgl.appendChild(new MyLabelConfig("Mulai Menjabat Fungsional"));
					jabatanfungsionalrowtgl
							.appendChild(new Label(kenaikanPangkat == null || kenaikanPangkat.getMulai() == null ? ""
									: Common.dateFormat6.get().format(kenaikanPangkat.getMulai())));

					MyFormRow row = new MyFormRow();row.setValign("top");
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig("Tunjangan Fungsional"));
					row.appendChild(
							new Label(kenaikanPangkat == null || kenaikanPangkat.getJabatanFungsional() == null ? ""
									: (kenaikanPangkat.getJabatanFungsional().ambilTunjangan(sekarang))));

					if (kenaikanPangkat != null && kenaikanPangkat.getKenaikanPangkatFungsional()) {
						Common.initKeterangan(rows, "Parameter tunjangan fungsional adalah TUNJ_FUNG");
					}
				}
			}

			for (KenaikanPangkat kenaikanPangkat : kenaikanPangkats) {
				if (kenaikanPangkat.getKenaikanPangkatGolongan()) {
					final MyFormRow jabatanstrukturalrow = new MyFormRow();
					jabatanstrukturalrow.setParent(rows);
					jabatanstrukturalrow.appendChild(new MyLabelConfig("Jabatan Struktural"));
					Label jabatanStruktural;
					jabatanstrukturalrow.appendChild(jabatanStruktural = new Label());
					jabatanStruktural
							.setValue(kenaikanPangkat == null || kenaikanPangkat.getJabatanStruktural() == null ? ""
									: kenaikanPangkat.getJabatanStruktural().getNama());
					jabatanStruktural.setWidth("90%");

					MyFormRow jabatanstrukturalrowtgl = new MyFormRow();
					jabatanstrukturalrowtgl.setParent(rows);
					jabatanstrukturalrowtgl.appendChild(new MyLabelConfig("Mulai Menjabat Struktural"));
					jabatanstrukturalrowtgl
							.appendChild(new Label(kenaikanPangkat == null || kenaikanPangkat.getMulai() == null ? ""
									: Common.dateFormat6.get().format(kenaikanPangkat.getMulai())));

					MyFormRow row = new MyFormRow();row.setValign("top");
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig("Tunjangan Struktural"));
					row.appendChild(
							new Label(kenaikanPangkat == null || kenaikanPangkat.getJabatanStruktural() == null ? ""
									: (kenaikanPangkat.getJabatanStruktural().ambilTunjangan(sekarang))));

					if (kenaikanPangkat != null && kenaikanPangkat.getKenaikanPangkatGolongan()) {
						Common.initKeterangan(rows, "Parameter tunjangan struktural adalah TUNJ_SRTK");
					}
				}
			}

			for (KenaikanPangkat kenaikanPangkat : kenaikanPangkats) {
				if (kenaikanPangkat.getJabatan() != null) {
					MyFormRow jabatanrow = new MyFormRow();
					jabatanrow.setParent(rows);
					jabatanrow.appendChild(new MyLabelConfig("Jabatan Lain"));
					jabatanrow
							.appendChild(new Label(kenaikanPangkat == null || kenaikanPangkat.getJabatan() == null ? ""
									: kenaikanPangkat.getJabatan().getNama()));

					MyFormRow row = new MyFormRow();row.setValign("top");
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig("Tunjangan Jabatan"));
					row.appendChild(new Label(kenaikanPangkat == null || kenaikanPangkat.getJabatan() == null ? ""
							: (kenaikanPangkat.getJabatan().ambilTunjangan(sekarang))));

					if (kenaikanPangkat != null && kenaikanPangkat.getKenaikanJabatan()) {
						Common.initKeterangan(rows, "Parameter tunjangan struktural adalah TUNJ_JAB");
					}
				}
			}

			MyFormRow row = new MyFormRow();row.setValign("top");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Deskripsi Jabatan"));
			row.appendChild(jabatan = new Textbox(pegawai.getJabatan() == null ? "" : pegawai.getJabatan()));
			jabatan.setRows(2);
			jabatan.setWidth("90%");

			KenaikanPangkat kenaikanPangkat = kenaikanPangkats.isEmpty() ? null : kenaikanPangkats.get(0);
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Golongan"));
			row.appendChild(new Label(kenaikanPangkat == null || kenaikanPangkat.getGolongan() == null ? ""
					: kenaikanPangkat.getGolongan().getNama()));

			GajiPokok gajiPokok = pegawai.ambilGajiPokok(sekarang);
			Insentif insentif = pegawai.ambilInsentif(sekarang);
			Makan makan = pegawai.ambilMakan(sekarang);
			Transport transport = pegawai.ambilTransport(sekarang);

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Gaji Pokok"));
			row.appendChild(new Label(gajiPokok == null ? "" : Common.numberFormat.get().format(gajiPokok.getGaji())));

			Common.initKeterangan(rows, "Parameter gaji pokok adalah GAPOK");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Insentif"));
			row.appendChild(new Label(insentif == null ? "" : Common.numberFormat.get().format(insentif.getInsentif())));

			Common.initKeterangan(rows, "Parameter insentif adalah INSENTIF");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Makan"));
			row.appendChild(new Label(makan == null ? "" : Common.numberFormat.get().format(makan.getMakan())));

			Common.initKeterangan(rows, "Parameter makan adalah MAKAN");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Transportasi"));
			row.appendChild(new Label(transport == null ? "" : Common.numberFormat.get().format(transport.getTransport())));

			Common.initKeterangan(rows, "Parameter transportasi adalah TRANSPORT");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Lain-lain"));
			row.appendChild(new Label(gajiPokok == null ? "" : Common.numberFormat.get().format(gajiPokok.getLain())));

			Common.initKeterangan(rows, "Parameter transportasi adalah LAIN_LAIN");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Pangkat"));
			row.appendChild(pangkat = new Textbox(pegawai.getPangkat() == null ? "" : pegawai.getPangkat()));
			pangkat.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
			row.appendChild(satuanKerja = new AmbilDataSatuanKerjaBanbox(true));
			satuanKerja.setValue(pegawai.getSatuanKerja() == null
					? (satuanKerjaOnSession != null ? satuanKerjaOnSession.toString()
							: (Common.getCurrentUser().ambilSatuanKerja() == null ? ""
									: Common.getCurrentUser().ambilSatuanKerja().toString()))
					: pegawai.getSatuanKerja().getNama());
			satuanKerja.setAttribute("satuanKerja",
					pegawai.getSatuanKerja() == null
							? (satuanKerjaOnSession != null ? satuanKerjaOnSession
									: Common.getCurrentUser() == null ? null
											: Common.getCurrentUser().ambilSatuanKerja())
							: pegawai.getSatuanKerja());
			satuanKerja.setWidth("90%");
			satuanKerja.setDisabled(satuanKerjaOnSession != null);

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Usia Pensiun"));
			row.appendChild(usiaPensiun = new Intbox(pegawai.getUsiaPensiun()));
			usiaPensiun.setWidth("90%");

			West west = new West();
			west.setStyle("border:0px;");
			ais.ui.util.ZkCompat.setFlex(west, true);
			west.setWidth("270px");
			west.setParent(borderlayout);

			Vbox vbox = new Vbox();
			vbox.setPack("center");
			vbox.setAlign("center");
			vbox.setHeight("100%");
			vbox.setWidth("100%");
			vbox.setParent(west);
			vbox.appendChild(foto = new Image("/img/administrator-icon_default.png"));
			// foto.setHeight("300px");
			foto.setWidth("220px");
			MyToolbarbuttonConfig fileupload = new MyToolbarbuttonConfig("Ganti Foto" + Common.ukuranLabelFileUpload(),
					"/img/File-Upload-icon.png");
			fileupload.setUpload(Common.ukuranFileUpload());
			vbox.appendChild(fileupload);
			EventListener eventListener = new EventListener() {

				@SuppressWarnings("deprecation")
				@Override
				public void onEvent(Event event) throws Exception {
					try {
						UploadEvent uploadEvent = (UploadEvent) event;
						if (uploadEvent != null) {
							if (!ais.action.master.helper.generic.AmbilDataTugasFileContent
									.validasiFoto(uploadEvent.getMedia())) return;

							if (pegawai.getId() == null) {
								if (ManagingPegawai.this.onSave(event) == null) {
									return;
								}
							}

							Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
							FotoPegawai fotoPegawai = (FotoPegawai) streamingSession.createCriteria(FotoPegawai.class)
									.add(Restrictions.eq("pegawai", pegawai.getId())).setMaxResults(1).uniqueResult();
							if (fotoPegawai != null) {
								streamingSession.getTransaction().begin();
								streamingSession.delete(fotoPegawai);
								streamingSession.getTransaction().commit();
							}

							fotoPegawai = new FotoPegawai();
							fotoPegawai.setNama(uploadEvent.getMedia().getName());
							fotoPegawai.setKeterangan(uploadEvent.getMedia().getContentType());
							fotoPegawai.setPegawai(pegawai.getId());

							try {
								fotoPegawai.setFoto(new javax.sql.rowset.serial.SerialBlob(uploadEvent.getMedia().getByteData()));
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/BiodataPegawaiAccountAction.java:594");

							}

							streamingSession.getTransaction().begin();
							streamingSession.save(fotoPegawai);
							streamingSession.getTransaction().commit();

							StreamingHibernateUtil.getInstance().closeSession();

							foto.setSrc(CommonMedia.getUrlFotoPengguna(new Tbmuser(pegawai)));
						} else {
							if (pegawai.getId() != null) {
								foto.setSrc(CommonMedia.getUrlFotoPengguna(new Tbmuser(pegawai)));
							}
						}
					} catch (Exception e) {
						StreamingHibernateUtil.getInstance().rollbackTransaction();
					}

				}
			};
			fileupload.addEventListener("onUpload", eventListener);

			eventListener.onEvent(null);

			Center center = new Center();
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);

			grid = new MyGrid();// grid.setOddRowSclass("non-odd");
			grid.setWidth("100%");
			grid.setParent(center);
			grid.setWidth("100%");
			grid.setHeight("100%");

			columns = new Columns();
			columns.setParent(grid);
			column = new MyColumnConfig();
			column.setWidth("20%");
			columns.appendChild(column);
			column = new MyColumnConfig();
			columns.appendChild(column);

			column = new MyColumnConfig();
			column.setWidth("20%");
			columns.appendChild(column);
			column = new MyColumnConfig();
			columns.appendChild(column);

			rows = new Rows();
			rows.setParent(grid);

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("NIP"));
			row.appendChild(mycode = new Textbox(pegawai.getMycode() == null ? "" : pegawai.getMycode()));
			mycode.setWidth("90%");

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Bank"));
			row.appendChild(bank = new Combobox());
			Common.insertCombo(bank, "nama", Bank.class);
			Common.selectComboItem(bank, pegawai.getBank());
			bank.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("NIP Baru"));
			row.appendChild(code = new Textbox(pegawai.getCode() == null ? "" : pegawai.getCode()));
			code.setWidth("90%");

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("No Rekening"));
			row.appendChild(norek = new Textbox(pegawai.getNorek()));
			norek.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("No. KTP"));
			row.appendChild(ktp = new Textbox(pegawai.getKtp()));
			ktp.setWidth("90%");

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("KARIS"));
			row.appendChild(karis = new Textbox(pegawai.getKaris()));
			karis.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
			row.appendChild(nama = new Textbox(pegawai.getNama() == null ? "" : pegawai.getNama()));
			nama.setWidth("90%");

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("ASKES"));
			row.appendChild(askes = new Textbox(pegawai.getAskes()));
			askes.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Terhitung Mulai Tanggal"));
			row.appendChild(tanggalmasuk = new MyDatebox(pegawai.getTanggalmasuk()));

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("TASPEN"));
			row.appendChild(taspen = new Textbox(pegawai.getTaspen()));
			taspen.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Status Pegawai"));
			statusPegawai = new Combobox();
			Common.insertCombo(statusPegawai, "nama", StatusPegawai.class, Restrictions.eq("aktif", true));
			Common.selectComboItem(statusPegawai, pegawai.getStatusPegawai());
			row.appendChild(statusPegawai);
			statusPegawai.setWidth("90%");

////			row.setParent(rows);
//			row.appendChild(new ais.ui.util.MyLabelConfig("KARPEG"));
//			row.appendChild(karpeg = new Textbox(pegawai.getKarpeg()));
//			karpeg.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Status Perkawinan"));
			Common.selectComboItem(statusPerkawinan, pegawai.getStatusPerkawinan());
			row.appendChild(statusPerkawinan);
			statusPerkawinan.setWidth("90%");
			statusPerkawinan.setDisabled(pegawai.getDosen() != null);

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("NPWP"));
			row.appendChild(npwp = new Textbox(pegawai.getNpwp()));
			npwp.setWidth("90%");
			npwp.setDisabled(pegawai.getDosen() != null);

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Kelamin"));
			Common.selectComboItem(kelamin, pegawai.getKelamin());
			row.appendChild(kelamin);
			kelamin.setWidth("90%");
			kelamin.setDisabled(pegawai.getDosen() != null);

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Telp."));
			row.appendChild(telp = new Textbox(pegawai.getTelp() == null ? "" : pegawai.getTelp()));
			telp.setWidth("90%");
			telp.setDisabled(pegawai.getDosen() != null);

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Email"));
			row.appendChild(email = new Textbox(pegawai.getEmail() == null ? "" : pegawai.getEmail()));
			email.setWidth("90%");
			email.setDisabled(pegawai.getDosen() != null);

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Tempat Lahir"));
			row.appendChild(
					tempatlahir = new Textbox(pegawai.getTempatlahir() == null ? "" : pegawai.getTempatlahir()));
			tempatlahir.setWidth("90%");
			tempatlahir.setDisabled(pegawai.getDosen() != null);

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Agama"));
			row.appendChild(agama = new Combobox());
			Common.insertCombo(agama, "nama", Agama.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
			Common.selectComboItem(agama, pegawai.getAgama());
			agama.setWidth("90%");
			agama.setDisabled(pegawai.getDosen() != null);

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Lahir"));
			row.appendChild(tanggallahir = new MyDatebox(
					pegawai.getTanggallahir() == null ? ais.ui.util.WaktuUtil.getDate() : pegawai.getTanggallahir()));

			tanggallahir.setDisabled(pegawai.getDosen() != null);

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Alamat"));

			MyGrid alamatGrid = new MyGrid();
			alamatGrid.setStyle("border:0px;background: transparent;");
			row.appendChild(alamatGrid);

			Columns alamatcolumns = new Columns();
			alamatcolumns.setParent(alamatGrid);
			MyColumnConfig alamatcolumn = new MyColumnConfig();
			alamatcolumn.setWidth("40%");
			alamatcolumns.appendChild(alamatcolumn);
			alamatcolumn = new MyColumnConfig();
			alamatcolumns.appendChild(alamatcolumn);

			Rows alamatrows = new Rows();
			alamatrows.setParent(alamatGrid);

			MyFormRow alamatrow = new MyFormRow();
			alamatrow.setParent(alamatrows);
			alamatrow.appendChild(new MyLabelConfig("Alamat/Jalan"));
			alamatrow.appendChild(alamatJalan = new Textbox(pegawai.getAlamatJalan()));
			alamatJalan.setWidth("90%");
			alamatJalan.setDisabled(pegawai.getDosen() != null);

			alamatrow = new MyFormRow();
			alamatrow.setParent(alamatrows);
			alamatrow.appendChild(new MyLabelConfig("Kelurahan / Desa"));
			alamatrow.appendChild(alamatKelurahan = new Textbox(pegawai.getAlamatKelurahan()));
			alamatKelurahan.setWidth("90%");
			alamatKelurahan.setDisabled(pegawai.getDosen() != null);

			alamatrow = new MyFormRow();
			alamatrow.setParent(alamatrows);
			alamatrow.appendChild(new MyLabelConfig("Kecamatan"));
			alamatrow.appendChild(alamatKecamatan = new Textbox(pegawai.getAlamatKecamatan()));
			alamatKecamatan.setWidth("90%");
			alamatKecamatan.setDisabled(pegawai.getDosen() != null);

			alamatrow = new MyFormRow();
			alamatrow.setParent(alamatrows);
			alamatrow.appendChild(new MyLabelConfig("Kabupaten / Kota"));
			alamatrow.appendChild(alamatKabupaten = new Textbox(pegawai.getAlamatKabupaten()));
			alamatKabupaten.setWidth("90%");
			alamatKabupaten.setDisabled(pegawai.getDosen() != null);

			alamatrow = new MyFormRow();
			alamatrow.setParent(alamatrows);
			alamatrow.appendChild(new MyLabelConfig("Propinsi"));
			alamatrow.appendChild(alamatPropinsi = new Textbox(pegawai.getAlamatPropinsi()));
			alamatPropinsi.setWidth("90%");
			alamatPropinsi.setDisabled(pegawai.getDosen() != null);

			alamat = new Textbox(pegawai.getAlamat() == null ? "" : pegawai.getAlamat());
			alamat.setWidth("90%");
			alamat.setRows(5);

			row.setValign("top");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Hobi"));
			row.appendChild(hobi = new Textbox(pegawai.getHobi()));
			hobi.setWidth("90%");
			hobi.setRows(5);

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan Badan"));

			MyGrid keteranganBadanGrid = new MyGrid();
			keteranganBadanGrid.setStyle("border:0px;background: transparent;");
			row.appendChild(keteranganBadanGrid);

			Columns keteranganBadancolumns = new Columns();
			keteranganBadancolumns.setParent(keteranganBadanGrid);
			MyColumnConfig keteranganBadancolumn = new MyColumnConfig();
			keteranganBadancolumn.setWidth("40%");
			keteranganBadancolumns.appendChild(keteranganBadancolumn);
			keteranganBadancolumn = new MyColumnConfig();
			keteranganBadancolumns.appendChild(keteranganBadancolumn);

			Rows keteranganBadanrows = new Rows();
			keteranganBadanrows.setParent(keteranganBadanGrid);

			MyFormRow keteranganBadanrow = new MyFormRow();
			keteranganBadanrow.setParent(keteranganBadanrows);
			keteranganBadanrow.appendChild(new MyLabelConfig("Tinggi (cm)"));
			keteranganBadanrow.appendChild(keteranganBadanTinggi = new Textbox(pegawai.getKeteranganBadanTinggi()));
			keteranganBadanTinggi.setWidth("90%");

			keteranganBadanrow = new MyFormRow();
			keteranganBadanrow.setParent(keteranganBadanrows);
			keteranganBadanrow.appendChild(new MyLabelConfig("Berat badan (kg)"));
			keteranganBadanrow.appendChild(keteranganBadanBerat = new Textbox(pegawai.getKeteranganBadanBerat()));
			keteranganBadanBerat.setWidth("90%");

			keteranganBadanrow = new MyFormRow();
			keteranganBadanrow.setParent(keteranganBadanrows);
			keteranganBadanrow.appendChild(new MyLabelConfig("Rambut"));
			keteranganBadanrow.appendChild(keteranganBadanRambut = new Textbox(pegawai.getKeteranganBadanRambut()));
			keteranganBadanRambut.setWidth("90%");

			keteranganBadanrow = new MyFormRow();
			keteranganBadanrow.setParent(keteranganBadanrows);
			keteranganBadanrow.appendChild(new MyLabelConfig("Bentuk muka"));
			keteranganBadanrow
					.appendChild(keteranganBadanBentukMuka = new Textbox(pegawai.getKeteranganBadanBentukMuka()));
			keteranganBadanBentukMuka.setWidth("90%");

			keteranganBadanrow = new MyFormRow();
			keteranganBadanrow.setParent(keteranganBadanrows);
			keteranganBadanrow.appendChild(new MyLabelConfig("Warna kulit"));
			keteranganBadanrow
					.appendChild(keteranganBadanWarnaKulit = new Textbox(pegawai.getKeteranganBadanWarnaKulit()));
			keteranganBadanWarnaKulit.setWidth("90%");

			keteranganBadanrow = new MyFormRow();
			keteranganBadanrow.setParent(keteranganBadanrows);
			keteranganBadanrow.appendChild(new MyLabelConfig("Ciri-ciri khas"));
			keteranganBadanrow.appendChild(keteranganBadanCiriKhas = new Textbox(pegawai.getKeteranganBadanCiriKhas()));
			keteranganBadanCiriKhas.setWidth("90%");

			keteranganBadanrow = new MyFormRow();
			keteranganBadanrow.setParent(keteranganBadanrows);
			keteranganBadanrow.appendChild(new MyLabelConfig("Cacat tubuh"));
			keteranganBadanrow.appendChild(keteranganBadanCacat = new Textbox(pegawai.getKeteranganBadanCacat()));
			keteranganBadanCacat.setWidth("90%");

			row.setValign("top");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
			row.appendChild(keterangan = new Textbox(pegawai.getKeterangan()));
			keterangan.setWidth("90%");
			keterangan.setRows(3);

			row = new MyFormRow();
			row.setVisible(false);
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Spesialisasi 1"));
			row.appendChild(
					spesialisasi1 = new Textbox(pegawai.getSpesialisasi1() == null ? "" : pegawai.getSpesialisasi1()));
			spesialisasi1.setWidth("90%");
			spesialisasi1.setRows(2);

			row = new MyFormRow();
			row.setVisible(false);
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Spesialisasi 2"));
			row.appendChild(
					spesialisasi2 = new Textbox(pegawai.getSpesialisasi2() == null ? "" : pegawai.getSpesialisasi2()));
			spesialisasi2.setWidth("90%");
			spesialisasi2.setRows(2);

			row = new MyFormRow();
			row.setVisible(false);
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Spesialisasi 3"));
			row.appendChild(
					spesialisasi3 = new Textbox(pegawai.getSpesialisasi3() == null ? "" : pegawai.getSpesialisasi3()));
			spesialisasi3.setWidth("90%");
			spesialisasi3.setRows(2);

			return panel;
		}

		public Pegawai onSave(Event event) throws Exception {
			if (nama.getValue().trim().equals("")) {
				PesanFormalHelper.tampilkanGagal("penyimpanan data Nama",
						"Kolom Nama belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
						new String[] {
								"Isi/pilih terlebih dahulu Nama.",
								"Ulangi proses penyimpanan setelah kolom tersebut terisi."
						});
				return null;
			}
			if (statusPerkawinan.getSelectedItem() == null) {
				PesanFormalHelper.tampilkanGagal("penyimpanan data Status perkawinan",
						"Kolom Status perkawinan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
						new String[] {
								"Isi/pilih terlebih dahulu Status perkawinan.",
								"Ulangi proses penyimpanan setelah kolom tersebut terisi."
						});
				return null;
			}
			PegawaiDao pegawaiDao = DaoFactory.getInstance().getPegawaiDao();
			if (pegawai.getId() != null) {
				pegawai = pegawaiDao.load(pegawai.getId());
			}

			pegawai.setTanggalmasuk(tanggalmasuk.getValue());
			pegawai.setHobi(hobi.getValue());

			pegawai.setStatusPerkawinan((String) (statusPerkawinan.getSelectedItem() == null ? null
					: statusPerkawinan.getSelectedItem().getValue()));
			pegawai.setAgama((Agama) (agama.getSelectedItem() == null ? null : agama.getSelectedItem().getValue()));

			String almt = Common.removeDuplicateWords("Jln. " + alamatJalan.getValue() + ", Kelurahan / Desa "
					+ alamatKelurahan.getValue() + ", Kecamatan " + alamatKecamatan.getValue() + ", Kabupaten / Kota "
					+ alamatKabupaten.getValue() + ", Provinsi " + alamatPropinsi.getValue());

			alamat.setValue(almt);

			pegawai.setUsiaPensiun(usiaPensiun.getValue());
			pegawai.setAlamatJalan(alamatJalan.getValue());
			pegawai.setAlamatKecamatan(alamatKecamatan.getValue());
			pegawai.setAlamatKabupaten(alamatKabupaten.getValue());
			pegawai.setAlamatKelurahan(alamatKelurahan.getValue());
			pegawai.setAlamatPropinsi(alamatPropinsi.getValue());
			pegawai.setKeteranganBadanBentukMuka(keteranganBadanBentukMuka.getValue());
			pegawai.setKeteranganBadanBerat(keteranganBadanBerat.getValue());
			pegawai.setKeteranganBadanCacat(keteranganBadanCacat.getValue());
			pegawai.setKeteranganBadanCiriKhas(keteranganBadanCiriKhas.getValue());
			pegawai.setKeteranganBadanRambut(keteranganBadanRambut.getValue());
			pegawai.setKeteranganBadanTinggi(keteranganBadanTinggi.getValue());
			pegawai.setKeteranganBadanWarnaKulit(keteranganBadanWarnaKulit.getValue());

			pegawai.setKtp(ktp.getValue());
			pegawai.setSpesialisasi1(spesialisasi1.getValue());
			pegawai.setSpesialisasi2(spesialisasi2.getValue());
			pegawai.setSpesialisasi3(spesialisasi3.getValue());
			pegawai.setJabatan(jabatan.getValue());
			pegawai.setMycode(mycode.getValue());
			pegawai.setCode(code.getValue().trim());
			pegawai.setAlamat(alamat.getValue());
			pegawai.setEmail(email.getValue());
			pegawai.setKelamin(
					kelamin.getSelectedItem() == null ? null : kelamin.getSelectedItem().getValue().toString());
			pegawai.setNama(nama.getValue());
			pegawai.setTanggallahir(tanggallahir.getValue());
			pegawai.setTelp(telp.getValue());
			pegawai.setTempatlahir(tempatlahir.getValue());
			pegawai.setPangkat(pangkat.getValue().trim());
			pegawai.setStatusPegawai((StatusPegawai) (statusPegawai.getSelectedItem() == null ? null
					: statusPegawai.getSelectedItem().getValue()));

			pegawai.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));

			pegawai.setTetap(1);
			pegawai.setKeterangan(keterangan.getValue());
			pegawai.setNorek(norek.getValue());
			pegawai.setKaris(karis.getValue());
			pegawai.setAskes(askes.getValue());
			pegawai.setTaspen(taspen.getValue());
//			pegawai.setKarpeg(karpeg.getValue());
			pegawai.setNpwp(npwp.getValue());

			pegawai.setBank(
					(Bank) (bank == null || bank.getSelectedItem() == null ? null : bank.getSelectedItem().getValue()));

			if (pegawai.getId() != null) {
				pegawaiDao.update(pegawai);
			} else {
				pegawaiDao.save(pegawai);
			}
			return pegawai;
		}

	}

	private class ManagingBiodataPegawai {
		private Textbox alamat;
		private Textbox namaAyah;
		private Textbox pekerjaanAyah;
		private Textbox namaIbu;
		private Textbox pekerjaanIbu;

		private Combobox pernahMenetapDiLuarNegeri;
		private Textbox tinggiBadan;
		private Textbox beratBadan;
		private Textbox teleponRumah;
		private Textbox hp;
		private Textbox suratIzinMengemudi;
		private Textbox kendaraanKuliah;
		private Combobox pernahMemimpinOrganisasi;
		private Textbox namaOrganisasi;
		private Textbox hobi;
		private Textbox minatSeni;
		private Textbox kemampuanBahasa1;
		private Textbox kemampuanBahasa2;
		private Textbox kemampuanBahasa3;
		private Textbox asalS1;
		private Textbox alamatAsalS1;
		private Textbox asalS2;
		private Textbox alamatAsalS2;
		private Textbox asalS3;
		private Textbox alamatAsalS3;
		private Textbox keahlian1;
		private Textbox keahlian2;
		private Textbox keahlian3;
		private Textbox keahlian4;
		private Textbox keahlian5;
		private AmbilDataNamaSekolahBanbox asalSma;
		private Textbox alamatAsalSma;
		private AmbilDataNamaSekolahBanbox asalSmp;
		private Textbox alamatAsalSmp;
		private AmbilDataNamaSekolahBanbox asalSd;
		private Textbox alamatAsalSd;
		private Textbox golonganDarah;
		private Combobox statusNikah;
		private Textbox kewarganegaraan;
		private Textbox agama;

		private BiodataPegawai biodataPegawai;

		private Pegawai pegawai;

		private Tabpanel preInit(Pegawai pegawai) throws Exception {
			this.pegawai = pegawai;
			// if (Sessions.getCurrent().getAttribute("usersTemp") == null
			// || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			// Sessions.getCurrent().removeAttribute("usersTemp");
			// Common.goLogoff();
			// return null;
			// }

			pernahMenetapDiLuarNegeri = new Combobox();
			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel("Ya");
			comboitem.setValue(1);
			pernahMenetapDiLuarNegeri.appendChild(comboitem);
			comboitem = new MyComboitemConfig();
			comboitem.setLabel("Tidak");
			comboitem.setValue(0);
			pernahMenetapDiLuarNegeri.appendChild(comboitem);

			pernahMemimpinOrganisasi = new Combobox();
			comboitem = new MyComboitemConfig();
			comboitem.setLabel("Ya");
			comboitem.setValue(1);
			pernahMemimpinOrganisasi.appendChild(comboitem);
			comboitem = new MyComboitemConfig();
			comboitem.setLabel("Tidak");
			comboitem.setValue(0);
			pernahMemimpinOrganisasi.appendChild(comboitem);

			statusNikah = new Combobox();
			comboitem = new MyComboitemConfig();
			comboitem.setLabel("Belum Nikah");
			comboitem.setValue(0);
			statusNikah.appendChild(comboitem);
			comboitem = new MyComboitemConfig();
			comboitem.setLabel("Nikah");
			comboitem.setValue(1);
			statusNikah.appendChild(comboitem);
			comboitem = new MyComboitemConfig();
			comboitem.setLabel("Janda");
			comboitem.setValue(2);
			statusNikah.appendChild(comboitem);
			comboitem = new MyComboitemConfig();
			comboitem.setLabel("Duda");
			comboitem.setValue(3);
			statusNikah.appendChild(comboitem);

			return loadDataPegawai();
		}

		public Tabpanel loadDataPegawai() throws Exception {
			Session session = HibernateUtil.currentSession();
			if (pegawai != null && pegawai.getId() != null) {
				biodataPegawai = (BiodataPegawai) session.createCriteria(BiodataPegawai.class)
						.add(Restrictions.eq("pegawai", pegawai)).addOrder(Order.desc("id")).setMaxResults(1)
						.uniqueResult();
			}

			Tabpanel tabpanelBiodata;
			if (biodataPegawai == null) {
				tabpanelBiodata = initBiodataPegawai(new BiodataPegawai());
			} else {
				tabpanelBiodata = initBiodataPegawai(biodataPegawai);
			}
			return tabpanelBiodata;
		}

		private Tabpanel initBiodataPegawai(final BiodataPegawai biodataPegawai) throws Exception {
			this.biodataPegawai = biodataPegawai;
			Tabpanel panel = new ais.ui.util.MyTabpanel();
			panel.setWidth("100%");
			panel.setHeight("100%");
			Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
			borderlayout.setParent(panel);
			borderlayout.setStyle("border:0px;");

			Center center = new Center();
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);

			MyGrid grid = new MyGrid();
			grid.setWidth("100%");
			grid.setParent(center);
			grid.setWidth("100%");
			grid.setHeight("100%");
			// grid.setOddRowSclass("non-odd");

			org.zkoss.zul.Columns columns = new org.zkoss.zul.Columns();
			columns.setParent(grid);
			MyColumnConfig column = new MyColumnConfig();
			column.setWidth("25%");
			columns.appendChild(column);
			column = new MyColumnConfig();
			column.setWidth("75%");
			columns.appendChild(column);

			Rows rows = new Rows();
			rows.setParent(grid);

			MyFormRow row = new MyFormRow();row.setValign("top");
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
			row.appendChild(new ais.ui.util.MyLabelConfig(pegawai.getNama()));

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Kode/NIP"));
			row.appendChild(new ais.ui.util.MyLabelConfig(pegawai.getCode()));

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Alamat"));
			row.appendChild(alamat = new Textbox(biodataPegawai.getAlamat() == null ? "" : biodataPegawai.getAlamat()));
			alamat.setWidth("90%");
			alamat.setRows(3);

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Nama Ayah"));
			row.appendChild(
					namaAyah = new Textbox(biodataPegawai.getNamaAyah() == null ? "" : biodataPegawai.getNamaAyah()));
			namaAyah.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Pekerjaan Ayah"));
			row.appendChild(pekerjaanAyah = new Textbox(
					biodataPegawai.getPekerjaanAyah() == null ? "" : biodataPegawai.getPekerjaanAyah()));
			pekerjaanAyah.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Nama Ibu"));
			row.appendChild(
					namaIbu = new Textbox(biodataPegawai.getNamaIbu() == null ? "" : biodataPegawai.getNamaIbu()));
			namaIbu.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Pekerjaan Ibu"));
			row.appendChild(pekerjaanIbu = new Textbox(
					biodataPegawai.getPekerjaanIbu() == null ? "" : biodataPegawai.getPekerjaanIbu()));
			pekerjaanIbu.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Pernah Menetap di Luar Negeri"));
			Common.selectComboItem(pernahMenetapDiLuarNegeri, biodataPegawai.getPernahMenetapDiLuarNegeri());
			row.appendChild(pernahMenetapDiLuarNegeri);
			pernahMenetapDiLuarNegeri.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Tinggi Badan"));
			row.appendChild(tinggiBadan = new Textbox(
					biodataPegawai.getTinggiBadan() == null ? "" : biodataPegawai.getTinggiBadan().toString()));
			tinggiBadan.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Berat Badan"));
			row.appendChild(beratBadan = new Textbox(
					biodataPegawai.getBeratBadan() == null ? "" : biodataPegawai.getBeratBadan().toString()));
			beratBadan.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Telepon Rumah"));
			row.appendChild(teleponRumah = new Textbox(
					biodataPegawai.getTeleponRumah() == null ? "" : biodataPegawai.getTeleponRumah()));
			teleponRumah.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("HP"));
			row.appendChild(hp = new Textbox(biodataPegawai.getHp() == null ? "" : biodataPegawai.getHp()));
			hp.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Nomor Surat Ijin Mengemudi"));
			row.appendChild(suratIzinMengemudi = new Textbox(
					biodataPegawai.getSuratIzinMengemudi() == null ? "" : biodataPegawai.getSuratIzinMengemudi()));
			suratIzinMengemudi.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Kendaraan Kuliah"));
			row.appendChild(kendaraanKuliah = new Textbox(
					biodataPegawai.getKendaraanKuliah() == null ? "" : biodataPegawai.getKendaraanKuliah()));
			kendaraanKuliah.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Pernah Memimpin Organisasi"));
			Common.selectComboItem(pernahMemimpinOrganisasi, biodataPegawai.getPernahMemimpinOrganisasi());
			row.appendChild(pernahMemimpinOrganisasi);
			pernahMemimpinOrganisasi.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Nama Organisasi"));
			row.appendChild(namaOrganisasi = new Textbox(
					biodataPegawai.getNamaOrganisasi() == null ? "" : biodataPegawai.getNamaOrganisasi()));
			namaOrganisasi.setWidth("90%");
			namaOrganisasi.setMaxlength(49);

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Hobi"));
			row.appendChild(hobi = new Textbox(biodataPegawai.getHobi() == null ? "" : biodataPegawai.getHobi()));
			hobi.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Minat Seni"));
			row.appendChild(minatSeni = new Textbox(
					biodataPegawai.getMinatSeni() == null ? "" : biodataPegawai.getMinatSeni()));
			minatSeni.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Kemampuan Bahasa 1"));
			row.appendChild(kemampuanBahasa1 = new Textbox(
					biodataPegawai.getKemampuanBahasa1() == null ? "" : biodataPegawai.getKemampuanBahasa1()));
			kemampuanBahasa1.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Kemampuan Bahasa 2"));
			row.appendChild(kemampuanBahasa2 = new Textbox(
					biodataPegawai.getKemampuanBahasa2() == null ? "" : biodataPegawai.getKemampuanBahasa2()));
			kemampuanBahasa2.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Kemampuan Bahasa 3"));
			row.appendChild(kemampuanBahasa3 = new Textbox(
					biodataPegawai.getKemampuanBahasa3() == null ? "" : biodataPegawai.getKemampuanBahasa3()));
			kemampuanBahasa3.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Asal S1"));
			row.appendChild(asalS1 = new Textbox(biodataPegawai.getAsalS1() == null ? "" : biodataPegawai.getAsalS1()));
			asalS1.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Alamat Asal S1"));
			row.appendChild(alamatAsalS1 = new Textbox(
					biodataPegawai.getAlamatAsalS1() == null ? "" : biodataPegawai.getAlamatAsalS1()));
			alamatAsalS1.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Asal S2"));
			row.appendChild(asalS2 = new Textbox(biodataPegawai.getAsalS2() == null ? "" : biodataPegawai.getAsalS2()));
			asalS2.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Alamat Asal S2"));
			row.appendChild(alamatAsalS2 = new Textbox(
					biodataPegawai.getAlamatAsalS2() == null ? "" : biodataPegawai.getAlamatAsalS2()));
			alamatAsalS2.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Asal S3"));
			row.appendChild(asalS3 = new Textbox(biodataPegawai.getAsalS3() == null ? "" : biodataPegawai.getAsalS3()));
			asalS3.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Alamat Asal S3"));
			row.appendChild(alamatAsalS3 = new Textbox(
					biodataPegawai.getAlamatAsalS3() == null ? "" : biodataPegawai.getAlamatAsalS3()));
			alamatAsalS3.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Rincian Keahlian 1"));
			row.appendChild(keahlian1 = new Textbox(
					biodataPegawai.getKeahliah1() == null ? "" : biodataPegawai.getKeahliah1()));
			keahlian1.setWidth("90%");
			keahlian1.setRows(4);

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Rincian Keahlian 2"));
			row.appendChild(keahlian2 = new Textbox(
					biodataPegawai.getKeahlian2() == null ? "" : biodataPegawai.getKeahlian2()));
			keahlian2.setWidth("90%");
			keahlian2.setRows(4);

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Rincian Keahlian 3"));
			row.appendChild(keahlian3 = new Textbox(
					biodataPegawai.getKeahlian3() == null ? "" : biodataPegawai.getKeahlian3()));
			keahlian3.setWidth("90%");
			keahlian3.setRows(4);

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Rincian Keahlian 4"));
			row.appendChild(keahlian4 = new Textbox(
					biodataPegawai.getKeahlian4() == null ? "" : biodataPegawai.getKeahlian4()));
			keahlian4.setWidth("90%");
			keahlian4.setRows(4);

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Rincian Keahlian 5"));
			row.appendChild(keahlian5 = new Textbox(
					biodataPegawai.getKeahlian5() == null ? "" : biodataPegawai.getKeahlian5()));
			keahlian5.setWidth("90%");
			keahlian5.setRows(4);

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Asal SMA"));
			row.appendChild(asalSma = new AmbilDataNamaSekolahBanbox(
					biodataPegawai.getAsalSma() == null ? "" : biodataPegawai.getAsalSma()));
			asalSma.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Alamat Asal SMA"));
			row.appendChild(alamatAsalSma = new Textbox(
					biodataPegawai.getAlamatAsalSma() == null ? "" : biodataPegawai.getAlamatAsalSma()));
			alamatAsalSma.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Asal SMP"));
			row.appendChild(asalSmp = new AmbilDataNamaSekolahBanbox(
					biodataPegawai.getAsalSmp() == null ? "" : biodataPegawai.getAsalSmp()));
			asalSmp.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Alamat Asal SMP"));
			row.appendChild(alamatAsalSmp = new Textbox(
					biodataPegawai.getAlamatAsalSmp() == null ? "" : biodataPegawai.getAlamatAsalSmp()));
			alamatAsalSmp.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Asal SD"));
			row.appendChild(asalSd = new AmbilDataNamaSekolahBanbox(
					biodataPegawai.getAsalSd() == null ? "" : biodataPegawai.getAsalSd()));
			asalSd.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Alamat Asal SD"));
			row.appendChild(alamatAsalSd = new Textbox(
					biodataPegawai.getAlamatAsalSd() == null ? "" : biodataPegawai.getAlamatAsalSd()));
			alamatAsalSd.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Golongan Darah"));
			row.appendChild(golonganDarah = new Textbox(
					biodataPegawai.getGolonganDarah() == null ? "" : biodataPegawai.getGolonganDarah()));
			golonganDarah.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Status Nikah"));
			Common.selectComboItem(statusNikah, biodataPegawai.getStatusNikah());
			row.appendChild(statusNikah);
			statusNikah.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Kewarganegaraan"));
			row.appendChild(kewarganegaraan = new Textbox(
					biodataPegawai.getKewarganegaraan() == null ? "" : biodataPegawai.getKewarganegaraan()));
			kewarganegaraan.setWidth("90%");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Agama"));
			row.appendChild(agama = new Textbox(biodataPegawai.getAgama() == null ? "" : biodataPegawai.getAgama()));
			agama.setWidth("90%");

			if (pegawai != null && pegawai.getDosen() != null) {
				Common.freeze(rows, true);
			}

			return panel;
		}

		public boolean onSave(Event event) throws Exception {
			try {

				BiodataPegawaiDao biodataPegawaiDao = DaoFactory.getInstance().getBiodataPegawaiDao();

				if (biodataPegawai.getId() != null) {
					System.out.println("Load");
					biodataPegawai = biodataPegawaiDao.load(biodataPegawai.getId());
				}
				biodataPegawai.setPegawai(pegawai);
				biodataPegawai.setAlamat(alamat.getValue());
				biodataPegawai.setNamaAyah(namaAyah.getValue());
				biodataPegawai.setPekerjaanAyah(pekerjaanAyah.getValue());
				biodataPegawai.setNamaIbu(namaIbu.getValue());
				biodataPegawai.setPekerjaanIbu(pekerjaanIbu.getValue());
				biodataPegawai.setTinggiBadan(
						tinggiBadan.getValue().trim().equals("") ? null : Integer.parseInt(tinggiBadan.getValue()));
				biodataPegawai.setPernahMenetapDiLuarNegeri(
						(Integer) (pernahMenetapDiLuarNegeri.getSelectedItem() == null ? null
								: pernahMenetapDiLuarNegeri.getSelectedItem().getValue()));
				biodataPegawai.setBeratBadan(
						beratBadan.getValue().trim().equals("") ? null : Integer.parseInt(beratBadan.getValue()));
				biodataPegawai.setTeleponRumah(teleponRumah.getValue());
				biodataPegawai.setHp(hp.getValue());
				biodataPegawai.setSuratIzinMengemudi(suratIzinMengemudi.getValue());
				biodataPegawai.setKendaraanKuliah(kendaraanKuliah.getValue());
				biodataPegawai.setPernahMemimpinOrganisasi(
						(Integer) (pernahMemimpinOrganisasi.getSelectedItem() == null ? null
								: pernahMemimpinOrganisasi.getSelectedItem().getValue()));
				biodataPegawai.setNamaOrganisasi(namaOrganisasi.getValue());
				biodataPegawai.setHobi(hobi.getValue());
				biodataPegawai.setMinatSeni(minatSeni.getValue());
				biodataPegawai.setKemampuanBahasa1(kemampuanBahasa1.getValue());
				biodataPegawai.setKemampuanBahasa2(kemampuanBahasa2.getValue());
				biodataPegawai.setKemampuanBahasa3(kemampuanBahasa3.getValue());
				biodataPegawai.setAsalS1(asalS1.getValue());
				biodataPegawai.setAlamatAsalS1(alamatAsalS1.getValue());
				biodataPegawai.setAsalS2(asalS2.getValue());
				biodataPegawai.setAlamatAsalS2(alamatAsalS2.getValue());
				biodataPegawai.setAsalS3(asalS3.getValue());
				biodataPegawai.setAlamatAsalS3(alamatAsalS3.getValue());
				biodataPegawai.setKeahliah1(keahlian1.getValue());
				biodataPegawai.setKeahlian2(keahlian2.getValue());
				biodataPegawai.setKeahlian3(keahlian3.getValue());
				biodataPegawai.setKeahlian4(keahlian4.getValue());
				biodataPegawai.setKeahlian5(keahlian5.getValue());
				biodataPegawai.setAsalSma(asalSma.getValue());
				biodataPegawai.setAlamatAsalSma(alamatAsalSma.getValue());
				biodataPegawai.setAsalSmp(asalSmp.getValue());
				biodataPegawai.setAlamatAsalSmp(alamatAsalSmp.getValue());
				biodataPegawai.setAsalSd(asalSd.getValue());
				biodataPegawai.setAlamatAsalSd(alamatAsalSd.getValue());
				biodataPegawai.setGolonganDarah(golonganDarah.getValue());
				biodataPegawai.setStatusNikah((Integer) (statusNikah.getSelectedItem() == null ? null
						: statusNikah.getSelectedItem().getValue()));
				biodataPegawai.setKewarganegaraan(kewarganegaraan.getValue());
				biodataPegawai.setAgama(agama.getValue());
				if (biodataPegawai.getId() != null) {
					biodataPegawaiDao.update(biodataPegawai);
				} else {
					biodataPegawaiDao.save(biodataPegawai);
				}
				return true;
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
				return false;
			}
		}
	}

}
