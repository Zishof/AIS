package ais.action.master.sirs;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Bandbox;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Button;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Window;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.sirs.detail.TransaksiAlatMedisDetailHelper;
import ais.action.master.sirs.detail.TransaksiItemDetailHelper;
import ais.action.master.sirs.detail.TransaksiLayananDetailHelper;
import ais.action.master.sirs.util.CommonPendaftaranUtil;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.CommonSirs;
import ais.common.ConstantValues;
import ais.common.listener.GetTransaksi;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.Lokasi;
import ais.database.model.sirs.Asuransi;
import ais.database.model.sirs.Dokter;
import ais.database.model.sirs.JadwalDokter;
import ais.database.model.sirs.JenisPasien;
import ais.database.model.sirs.KelasPerawatan;
import ais.database.model.sirs.Pasien;
import ais.database.model.sirs.Pendaftaran;
import ais.database.model.sirs.Poly;
import ais.database.model.sirs.Shift;
import ais.database.model.sirs.TransaksiMedis;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;

public class PendaftaranRawatUgdAction extends GenericAutowireComposer implements GetTransaksi {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Tabpanel tambahData;
	private Grid grid;
	private Paging paging;

	private MyTextbox searchkode;
	private MyTextbox searchnama;
	private MyTextbox searchdokter;
	private MyTextbox searchmr;
	private MyTextbox searchtelp;
	private MyTextbox searchalamat;

	private Combobox searchjenisPasien;

	private Label kode;
	private Pasien pasien;
	private Asuransi asuransi;
	private Date tanggalPendaftaran;
	private String keterangan;
	private Date dilayaniTanggal;
	private Poly poly;
	private Poly subpoly;
	private Dokter dokter;

	private Checkbox baru;

	private boolean edit = false;
	private boolean delete = false;

	private Pendaftaran pendaftaran;
	private TransaksiMedis transaksi;
	private Toolbarbutton add;
	private String jenis = Pendaftaran.RAWAT_UGD;

	private TransaksiItemDetailHelper transaksiItemDetailHelper;
	private TransaksiLayananDetailHelper transaksiLayananDetailHelper;
	private TransaksiAlatMedisDetailHelper transaksiAlatMedisAction;

	private Lokasi myLokasi = Common.getCurrentLokasi();
	private Shift myShift;

	private String SUMBER = TransaksiMedis.SUMBER_UGD;

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
		System.out.println("jenis = " + jenis);

		Common.insertCombo(searchjenisPasien, "nama", JenisPasien.class);

		add = new ais.ui.util.MyToolbarbuttonConfig("Pasien  Baru", "/img/user_male_add.png");
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
			if (pendaftaran.getPasien() == null) {
				arg0.detach();
				return;
			}

			Pasien pasien = pendaftaran.getPasien();
			if (pasien.getAktif() == null || !pasien.getAktif()) {
				arg0.setStyle("background-color:red;");
			}

			new Label(pendaftaran.getKode()).setParent(arg0);
			new Label(pasien.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(Pendaftaran.class, pendaftaran, pasien.getNama()).setParent(arg0);
			new Label(Common.dateFormat3.get().format(pendaftaran.getTanggalPendaftaran())).setParent(arg0);
			new Label(pendaftaran.getStatusPendaftaran()).setParent(arg0);
			new Label(pendaftaran.getPoly() == null ? ""
					: pendaftaran.getPoly().getNama()
							+ (pendaftaran.getSubpoly() == null ? "" : " (" + pendaftaran.getSubpoly().getNama() + ")"))
					.setParent(arg0);
			
			new Label(pendaftaran.ambilAlamat()).setParent(arg0);
			new Label(pendaftaran.getNomorAntrian() == null ? ""
					: Common.numberFormat.get().format(pendaftaran.getNomorAntrian())).setParent(arg0);
			new Label(pendaftaran.getDokter() == null ? "" : pendaftaran.getDokter().getNama()).setParent(arg0);
			new Label(Common.dateFormat4.get().format(pendaftaran.getDilayaniTanggal())).setParent(arg0);
			new Label(pasien == null ? "" : pasien.getNoTelp() + " / " + pasien.getNoHp()).setParent(arg0);
			new Label(pasien == null ? "" : pasien.getJenisPasien() == null ? "" : pasien.getJenisPasien().getNama())
					.setParent(arg0);

			Hbox toolbar = new Hbox();
			toolbar.setVisible(pasien.getAktif() != null && pasien.getAktif());
			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/credit_card_small.png");
			button.setTooltiptext("Cetak Kartu Pasien");
			button.setVisible(pendaftaran.getPasien() != null);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					CommonSirs.onCetakKartuPasien(pendaftaran);
				}

			});
			button.setParent(toolbar);

			button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/user_male.png");
			button.setTooltiptext("Cetak Status Pasien ");
			button.setVisible(pendaftaran.getPasien() != null);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					if (pendaftaran.getPasien() != null)
						CommonSirs.onCetakStatusPasien(pendaftaran.getPasien());
				}

			});
			button.setParent(toolbar);

			button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak Tracer");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					CommonSirs.onCetakTracer(pendaftaran);
				}

			});
			button.setParent(toolbar);

			button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/edit.gif");
			button.setTooltiptext("Rubah Data");
			button.setVisible(edit && (pendaftaran.getLunas() == null || !pendaftaran.getLunas()));
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					Pendaftaran pendaftaranEdit = (Pendaftaran) HibernateUtil.currentSession()
							.createCriteria(Pendaftaran.class).add(Restrictions.idEq(pendaftaran.getId()))
							.uniqueResult();
					init(pendaftaranEdit);
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
			toolbar.setParent(arg0);
		}

	}

	public void onDelete(final Pendaftaran pendaftaran) throws Exception {
		MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus data pendaftaran ini? Data pendaftaran yang telah dihapus tidak dapat dikembalikan.", "Pertanyaan",
				MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {
						int i = new Integer(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							try {
								Common.refreshDelete(pendaftaran);
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

	private Toolbarbutton save;
	private Toolbarbutton validasi;
	protected JadwalDokter shiftDokter;
	private Row myEaes;

	private void initUgd(final Row myEaes, Pendaftaran pendaftaran) throws Exception {
		Common.clear(myEaes);
		Tabbox tabbox = new Tabbox();
		tabbox.setStyle("border:0px;background: transparent;");
		tabbox.setHeight("400px");
		tabbox.setWidth("100%");
		tabbox.setParent(myEaes);

		Tabs tabs = new Tabs();
		tabs.setStyle("border:0px;background: transparent;");
		tabs.setParent(tabbox);

		Tab tabItem = new Tab("Penggunaan Obat");
		tabItem.setParent(tabs);

		Tab tabTindakan = new Tab("Layanan Tindakan dan Perawatan");
		tabTindakan.setParent(tabs);

		Tab tabAlatMedis = new Tab("Penggunaan Alat Medis dan Kesehatan");
		tabAlatMedis.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setStyle("border:0px;background: transparent;");
		tabpanels.setParent(tabbox);

		Tabpanel tabpanelItem = new ais.ui.util.MyTabpanel();
		tabpanelItem.setParent(tabpanels);
		transaksiItemDetailHelper = new TransaksiItemDetailHelper(PendaftaranRawatUgdAction.this);
		tabpanelItem.appendChild(transaksiItemDetailHelper);
		if (transaksi.getId() != null) {
			transaksiItemDetailHelper.loadData(null);
		}

		final Tabpanel tabpanelTindakan = new ais.ui.util.MyTabpanel();
		tabpanelTindakan.setParent(tabpanels);
		transaksiLayananDetailHelper = new TransaksiLayananDetailHelper(PendaftaranRawatUgdAction.this);
		tabpanelTindakan.appendChild(transaksiLayananDetailHelper);
		if (transaksi.getId() != null) {
			transaksiLayananDetailHelper.loadData(null);
		}

		final Tabpanel tabpanelAlatMedis = new ais.ui.util.MyTabpanel();
		tabpanelAlatMedis.setParent(tabpanels);
		transaksiAlatMedisAction = new TransaksiAlatMedisDetailHelper(PendaftaranRawatUgdAction.this);
		tabpanelAlatMedis.appendChild(transaksiAlatMedisAction);
		if (transaksi.getId() != null) {
			transaksiAlatMedisAction.loadData(null);
		}
	}

	@SuppressWarnings("deprecation")
	private void init(final Pendaftaran pendaftaran) throws Exception {
		this.pendaftaran = pendaftaran;
		this.transaksi = pendaftaran.getTransaksiUgd() == null ? new TransaksiMedis() : pendaftaran.getTransaksiUgd();
		Common.clear(tambahData);
		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setWidth("100%");
		borderlayout.setHeight("100%");

		final Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setTitle("Input Data Pasien UGD");

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

		final Button tambahPasienBaru = new ais.ui.util.MyToolbarbuttonConfig("Pasien Baru", "/img/user_male.png");

		Row row = new Row();
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
						final Window window = (Window) arg0.getTarget();
						final Pasien pasien = (Pasien) arg0.getData();

						perubahanPasienListener.onEvent(new Event("", tambahPasienBaru, pasien));

						window.detach();
					}
				});

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

		EventListener jadwalPemeriksaanEventListener = CommonPendaftaranUtil.initJadwalPemeriksaan(rows, pendaftaran,
				jenis, new EventListener() {

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

		CommonSirs.initLokasiDanShift(pendaftaran.getLokasi() == null ? myLokasi : pendaftaran.getLokasi(),
				pendaftaran.getShift(), rows, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Object[] o = (Object[]) arg0.getData();
						myLokasi = (Lokasi) o[0];
						myShift = (Shift) o[1];

						if (kode.getValue().trim().equals("") && myLokasi != null) {
							String mykode = Common.generateCode(Pendaftaran.class, 10, "REG-UGD", myLokasi);
							kode.setValue(mykode);
						}
					}
				});

		myEaes = new Row();
		myEaes.setAttribute("hide", "no");
		myEaes.setStyle("border:0px;background: transparent;");
		myEaes.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(myEaes, "4");
		initUgd(myEaes, pendaftaran);

		row = new Row();
		row.setAttribute("hide", "no");
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "4");

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(row);

		add.setParent(toolbar);
		save = new ais.ui.util.MyToolbarbuttonConfig("Simpan Pasien UGD", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.setDisabled(transaksi.getId() != null);
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					MyMessageboxConfig.show("Data transaksi UGD telah berhasil disimpan.", "Informasi", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									validasi.setVisible(true);
								}
							});
					add.setDisabled(false);
					save.setDisabled(true);
					add.setDisabled(false);
					onSearchDefault(null);
				}
			}
		});
		save.setParent(toolbar);

		validasi = new ais.ui.util.MyToolbarbuttonConfig("Validasi Transaksi UGD ", "/img/Ok-icon_kecil.png");
		validasi.setTooltiptext("Validasi");
		validasi.setVisible(transaksi.getId() != null);
		validasi.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				if (myLokasi == null) {

					MyMessageboxConfig.show("Bapak/Ibu belum memilih lokasi. Mohon pilih salah satu lokasi terlebih dahulu. Langkah yang dapat dilakukan: (1) buka daftar pilihan lokasi; (2) pilih salah satu lokasi yang sesuai; (3) ulangi proses validasi.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}

				MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin memvalidasi transaksi ini? Transaksi yang telah divalidasi tidak dapat diubah kembali.", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = new Integer(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {

									if (onSave(event)) {

										CommonPendaftaranUtil.validasiTransaksiItem(transaksi, SUMBER, true);

										CommonPendaftaranUtil.validasiTransaksiLayanan(transaksi, SUMBER, false);

										CommonPendaftaranUtil.validasiTransaksiAlatMedis(transaksi, SUMBER, false);

										MyMessageboxConfig.show("Data transaksi telah berhasil divalidasi.", "Informasi",
												MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

													@Override
													public void onEvent(Event arg0) throws Exception {

													}
												});

										add.setDisabled(false);
										validasi.setDisabled(true);
										save.setDisabled(true);
										add.setDisabled(false);
										onSearchDefault(null);
										Common.freeze(center, true);
										Common.freeze(myEaes, true);

									}

								}
							}
						});
			}
		});
		validasi.setParent(toolbar);

		Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("Batalkan Pasien UGD", "/img/delete.gif");
		button.setVisible(delete);
		button.setTooltiptext("Batalkan Pendaftaran Pasien ");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (pendaftaran != null && pendaftaran.getId() != null) {
					onDelete(pendaftaran);

				} else {
					MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin membatalkan pendaftaran pasien ini? Data yang telah dimasukkan tidak akan tersimpan.", "Pertanyaan",
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

		borderlayout.setParent(tambahData);
		tambahData.getLinkedTab().setSelected(true);

		if (kode.getValue().trim().equals("") && myLokasi != null) {
			kode.setValue(Common.generateCode(Pendaftaran.class, 10, "REG-UGD", myLokasi));
		}

		if (pendaftaran.getId() != null) {
			perubahanPasienListener.onEvent(null);

			Object[] datas = new Object[] { pendaftaran.getJadwalDokter(), pendaftaran.getDilayaniTanggal(), true,
					pendaftaran };
			jadwalPemeriksaanEventListener.onEvent(new Event("", null, datas));
		}

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

		if (poly == null) {
			MyMessageboxConfig.show("Poli belum diisi. Mohon lengkapi data poli terlebih dahulu. Langkah yang dapat dilakukan: (1) periksa kolom poli; (2) pilih poli yang sesuai; (3) simpan kembali data.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (dokter == null) {
			MyMessageboxConfig.show("Dokter belum diisi. Mohon lengkapi data dokter terlebih dahulu. Langkah yang dapat dilakukan: (1) periksa kolom dokter; (2) pilih dokter yang sesuai; (3) simpan kembali data.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
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
		pendaftaran.setJadwalDokter(shiftDokter);
		pendaftaran.setBookingRegistrasi(null);
		pendaftaran.setAsuransi((Asuransi) asuransi);
		pendaftaran.setSubpoly((Poly) (subpoly));

		pendaftaran.setDokter((Dokter) dokter);
		pendaftaran.setPoly((Poly) poly);
		pendaftaran.setBaru(baru.isChecked());
		pendaftaran.setJenis(jenis);
		pendaftaran.setTanggalPendaftaran(tanggalPendaftaran);
		pendaftaran.setMerupakanPaket(false);
		pendaftaran.setPasien((Pasien) pasien);
		pendaftaran.setPasienKomunitas(pendaftaran.getPasien());
		pendaftaran.setKode(kode.getValue());
		pendaftaran.setKeterangan(keterangan);
		pendaftaran.setTbmuser(Common.getCurrentUser());

		pendaftaran.setLokasi(myLokasi);
		pendaftaran.setShift(myShift);

		Integer antrian = CommonPendaftaranUtil.generateNomorAntrian(pendaftaran, pendaftaran.getJadwalDokter());
		pendaftaran.setNomorAntrian(antrian);

		if (pendaftaran.getId() != null) {

			Common.refreshUpdate(session, pendaftaran);
		} else {
			pendaftaran.setIndex(Common.generateMaxByLokasi(Pendaftaran.class, myLokasi) + 1);
			String mykode = Common.generateCode(Pendaftaran.class, 10, "REG-UGD", myLokasi);
			kode.setValue(mykode);
			pendaftaran.setKode(mykode);
			session.save(pendaftaran);
		}

		return onSaveTransaksi(pendaftaran);
	}

	public boolean onSaveTransaksi(Pendaftaran pendaftaran) throws Exception {

		if (myLokasi == null) {
			MyMessageboxConfig.show("Lokasi belum diisi. Mohon lengkapi data lokasi terlebih dahulu. Langkah yang dapat dilakukan: (1) periksa kolom lokasi; (2) pilih lokasi yang sesuai; (3) simpan kembali data.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (myShift == null) {
			MyMessageboxConfig.show("Shift belum diisi. Mohon lengkapi data shift terlebih dahulu. Langkah yang dapat dilakukan: (1) periksa kolom shift; (2) pilih shift yang sesuai; (3) simpan kembali data.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (transaksi.getId() != null) {
			transaksi = (TransaksiMedis) session.load(TransaksiMedis.class, transaksi.getId());

		}

		if (kode.getValue().trim().equals("")) {
			kode.setValue(Common.generateCode(TransaksiMedis.class, 8));
		}

		transaksi.setJenisTransaksi(TransaksiMedis.TRX_UGD);
		transaksi.setNama(pendaftaran.getPasien().getNama());
		transaksi.setAlamat(pendaftaran.getPasien().getAlamatLengkap());
		transaksi.setJenisKelamin(pendaftaran.getPasien().getJenisKelamin());

		transaksi.setResep(null);
		transaksi.setJenisPasien(pendaftaran.getJenisPasien());
		transaksi.setKelasPerawatan(ConstantValues.kelasNormal);

		transaksi.setUmur(pendaftaran.getUmur().toString() + " thn");
		transaksi.setTanggalTransaksi(pendaftaran.getTanggalPendaftaran());

		transaksi.setPasien(pendaftaran.getPasien());
		transaksi.setBagian(pendaftaran.getBagian());
		transaksi.setKode(kode.getValue());
		transaksi.setKeterangan(pendaftaran.getKeterangan());
		transaksi.setLokasi(myLokasi);
		transaksi.setShift(myShift);
		transaksi.setPendaftaran(pendaftaran);
		transaksi.setBebas(false);
		transaksi.setSumber(SUMBER);

		if (transaksi.getId() != null) {
			Common.refreshUpdate(session, transaksi);
		} else {
			String mykode = Common.generateCode(TransaksiMedis.class, 8, "TRX", myLokasi);
			transaksi.setIndex(Common.generateMaxByLokasi(TransaksiMedis.class, myLokasi) + 1);
			kode.setValue(mykode);
			kode.setValue(transaksi.getKode());
			session.save(transaksi);
		}

		pendaftaran.setTransaksiUgd(transaksi);
		Common.refreshUpdate(session, pendaftaran);

		return true;
	}

	private Criteria initCriteria(boolean order) {

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

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Pendaftaran.class);

		if (order)
			criteria.addOrder(Order.desc("tanggalPendaftaran"));

		criteria.createAlias("pasien", "pasien", Criteria.LEFT_JOIN).createAlias("dokter", "dokter", Criteria.LEFT_JOIN)

				.createAlias("pasien.propinsi", "propinsi", Criteria.LEFT_JOIN)
				.createAlias("pasien.kota", "kota", Criteria.LEFT_JOIN)
				.createAlias("pasien.kecamatan", "kecamatan", Criteria.LEFT_JOIN)
				.createAlias("pasien.kelurahan", "kelurahan", Criteria.LEFT_JOIN)

				.add(criterion)

				.add((searchtelp == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.or(Restrictions.ilike("pasien.noTelp", searchtelp.getValue(), MatchMode.ANYWHERE),
						Restrictions.ilike("pasien.noHp", searchtelp.getValue(), MatchMode.ANYWHERE))))

				.add((searchmr == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.ilike("pasien.kode", searchmr.getValue(), MatchMode.ANYWHERE)))
				.add((searchnama == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.ilike("pasien.nama", searchnama.getValue(), MatchMode.ANYWHERE)))
				.add((searchdokter == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.ilike("dokter.nama", searchdokter.getValue(), MatchMode.ANYWHERE)))

				.add(Restrictions.eq("jenis", jenis))
				.add(searchjenisPasien.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("jenisPasien", searchjenisPasien.getSelectedItem().getValue()))

				.add((searchkode == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.ilike("kode", searchkode.getValue(), MatchMode.ANYWHERE)));
		if (order)
			criteria.addOrder(Order.desc("id"));

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

	@Override
	public TransaksiMedis getTransaksi() {
		return transaksi;
	}

	@Override
	public Lokasi getLokasi() {
		return myLokasi;
	}

	@Override
	public Button getAdd() {
		// TODO Auto-generated method stub
		return add;
	}

	@Override
	public Button getSimpan() {
		// TODO Auto-generated method stub
		return save;
	}

	@Override
	public KelasPerawatan getKelasPerawatan() {
		KelasPerawatan mykelasPerawatan = ConstantValues.kelasNormal;
		return mykelasPerawatan;
	}

	@Override
	public Bandbox getResep() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSumber() {
		// TODO Auto-generated method stub
		return SUMBER;
	}

}
