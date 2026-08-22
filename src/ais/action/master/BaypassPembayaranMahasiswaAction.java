package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.util.List;

import org.apache.commons.lang.StringUtils;
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
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
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
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.master.helper.KegiatanHelper;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.dao.BaypassPembayaranMahasiswaDao;
import ais.database.dao.DaoFactory;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BaypassPembayaranMahasiswa;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.HistoryStatusMahasiswa;
import ais.database.model.JenisKegiatan;
import ais.database.model.Jenjang;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.Konsentrasi;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.StatusMahasiswa;
import ais.database.model.Tbmuser;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

public class BaypassPembayaranMahasiswaAction extends GenericAutowireComposer
		implements DataLoader, DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3786091220301468178L;
	private MyGrid grid;
	private Paging paging;
	private MyWindow addWindow;

	private Textbox searchnim;
	private Textbox searchnama;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Decimalbox searchtahun;
	private Combobox searchkonsentrasi;
	private Combobox searchstatus;
	private Combobox searchprogram;
	private Combobox searchStatusAwalMahasiswa;
	private Combobox searchjenjang;
	private Combobox kewarganegaraan;
	private MyCheckboxConfig searchdosenPA;
	private AmbilDataDosenBanbox searchdosen;

	private AmbilDataMahasiswaBanbox mahasiswa;
	private Textbox keterangan;
	private Combobox tahunAkademik;
	private Combobox ganjilGenap;
	private MyDatebox berlakuMulai;
	private MyDatebox berlakuSampai;
	private Label lblSemester;

	private boolean edit = false;
	private boolean delete = false;

	private MyToolbarbuttonConfig add;

	private BaypassPembayaranMahasiswa baypassPembayaranMahasiswa;
	private Combobox tahap;
	private Combobox jenisKegiatan;

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

		Common.initPrograms(searchprogram);
		Common.insertCombo(searchstatus, new String[] { "nama", "kodeEpsbed" }, StatusMahasiswa.class);
		Common.insertCombo(searchStatusAwalMahasiswa, "nama", StatusAwalMahasiswa.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		kewarganegaraan = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		if (comboitem != null) { comboitem.setLabel(ais.database.model.Mahasiswa.WNI); }
		if (comboitem != null) { comboitem.setValue(ais.database.model.Mahasiswa.WNI); }
		kewarganegaraan.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(ais.database.model.Mahasiswa.WNA); }
		if (comboitem != null) { comboitem.setValue(ais.database.model.Mahasiswa.WNA); }
		kewarganegaraan.appendChild(comboitem);

		tahunAkademik = new Combobox();
		Common.generateTahunAjaran(tahunAkademik);
		ganjilGenap = new Combobox();
		comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GANJIL); }
		ganjilGenap.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GENAP); }
		ganjilGenap.appendChild(comboitem);

		class SearchJurusanEventListener implements EventListener {

			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub
				Common.clear(searchkonsentrasi);
				searchkonsentrasi.setSelectedItem(null);
				if (searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null) {
					return;
				}
				Common.insertCombo(searchkonsentrasi, "nama", Konsentrasi.class,
						CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false));
			}

		}
		searchjurusan.addEventListener("onChange", new SearchJurusanEventListener());

		Common.insertCombo(searchfakultas, new String[] { "nama", "kode" }, Fakultas.class,
				Restrictions.eq("aktif", true));

		class SearchFakultasEventListener implements EventListener {

			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub
				Common.clear(searchjurusan);
				searchjurusan.setSelectedItem(null);
				if (searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null) {
					return;
				}
				Common.insertCombo(searchjurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false));
			}

		}

		searchfakultas.addEventListener("onChange", new SearchFakultasEventListener());

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		Tbmuser tbmuser = Common.getCurrentUser();
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		Common.insertComboDanSemua(searchjenjang, "nama", Jenjang.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		if (tbmuser != null && tbmuser.ambilDosen() != null && tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")) {
			Dosen mydosen = tbmuser.ambilDosen();
			searchdosen.setValue(mydosen.getNama());
			searchdosen.setAttribute("myValue", mydosen);
			searchdosen.setAttribute("dosen", mydosen);
			searchdosen.setDisabled(true);
		}
		searchdosen.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		MyToolbarbuttonConfig cetakSksDosen = new MyToolbarbuttonConfig("Singkronkan", "/img/svg/check2.svg");
		Common.appendKeToolbar(cetakSksDosen, add, comp);
		cetakSksDosen.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.createDefaultTimer(new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event arg0) throws Exception {

						final Label label = new Label(ais.common.Common.getBahasaConfig("Proses singkronisasi bypass"));
						// Laporan rinci per baris (berhasil/gagal+penyebab teknis lengkap+langkah
						// mengatasi) - dulu SATU error di mana pun di badan loop mematikan Thread
						// TANPA jejak (label tak pernah dikosongkan lagi -> Clients.showBusy
						// menggantung selamanya). Sekarang tiap baris dibungkus try/catch sendiri.
						final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload("Sinkronisasi Bypass Pembayaran Mahasiswa");

						new Thread(new Runnable() {

							@Override
							public void run() {
								List<BaypassPembayaranMahasiswa> baypassPembayaranMahasiswas = ConstantValues
										.simpleList(initCriteria(true), BaypassPembayaranMahasiswa.class);

								int i = 0;
								int size = baypassPembayaranMahasiswas.size();
								try {
									for (BaypassPembayaranMahasiswa baypassPembayaranMahasiswa : baypassPembayaranMahasiswas) {
										String kunci = baypassPembayaranMahasiswa != null
												&& baypassPembayaranMahasiswa.getMahasiswa() != null
												&& baypassPembayaranMahasiswa.getMahasiswa().getNim() != null
														? baypassPembayaranMahasiswa.getMahasiswa().getNim()
														: String.valueOf(baypassPembayaranMahasiswa);
										try {
											if (baypassPembayaranMahasiswa != null && baypassPembayaranMahasiswa.getId() != null
													&& baypassPembayaranMahasiswa.getJenisKegiatan() != null
													&& baypassPembayaranMahasiswa.getJenisKegiatan()
															.getDigunakanSyaratKeaktifan()) {
												boolean terlambarLangsungTidakAktif = Common
														.getKonfigurasi("mhs_all_lambat_bayar_langsung_tidak_aktif", "",
																baypassPembayaranMahasiswa.getSemester(),
																baypassPembayaranMahasiswa.getMahasiswa().getTahunangkatan(),
																baypassPembayaranMahasiswa.getMahasiswa().getJurusan(),
																baypassPembayaranMahasiswa.getMahasiswa().getProgram(),
																baypassPembayaranMahasiswa.getMahasiswa()
																		.getStatusAwalMahasiswa())
														.getNilai().equals(Konfigurasi.AKTIF);
												if (terlambarLangsungTidakAktif) {

													boolean checkStatusPembayaranMahasiswa = true;

													Mahasiswa mahasiswa = baypassPembayaranMahasiswa.getMahasiswa();
													KegiatanHelper.updateBatasStudiMahasiswa(mahasiswa, null,
															baypassPembayaranMahasiswa.getSemester(),
															checkStatusPembayaranMahasiswa);

													HistoryStatusMahasiswa historyStatusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(
															baypassPembayaranMahasiswa.getMahasiswa(),
															baypassPembayaranMahasiswa.getTahunAkademik(),
															baypassPembayaranMahasiswa.getSemester());
													historyStatusMahasiswa.put(checkStatusPembayaranMahasiswa + "",
															"checkStatusPembayaranMahasiswa");
													historyStatusMahasiswa.setStatusMahasiswa(
															checkStatusPembayaranMahasiswa ? ConstantValues.AKTIF
																	: ConstantValues.TIDAK_AKTIF);

													System.out.println(
															"mahasiswa " + mahasiswa + ", checkStatusPembayaranMahasiswa "
																	+ checkStatusPembayaranMahasiswa);
												}
											}
											laporan.catatBerhasil(i, kunci, "Sinkronisasi berhasil");
										} catch (Exception eBaris) {
											ais.common.ErrorAuditUtil.record(eBaris, "auto-audit src/ais/action/master/BaypassPembayaranMahasiswaAction.java:sinkronBaris");
											laporan.catatGagalDetail(i, kunci, eBaris);
										} finally {
											HibernateUtil.closeSession();
										}
										if (label != null) {
											label.setValue("(" + (Common.numberFormat.get().format(i * 100.0 / size))
													+ " %) sinkronisasi data bypass " + baypassPembayaranMahasiswa + " ..");
										}
										i++;
									}
								} finally {
									// WAJIB selalu dikosongkan walau ada error tak terduga di luar loop,
									// supaya Timer di bawah tidak menggantung selamanya (Clients.showBusy).
									label.setValue("");
								}

							}
						}).start();

						final Timer timer = new Timer(500);
						timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						timer.setRepeats(true);
						timer.addEventListener("onTimer", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								// System.out.println("process = " +
								// label.getValue());
								Clients.showBusy(label.getValue());
								if (label.getValue().isEmpty()) {
									Clients.clearBusy();
									timer.detach();
									// Laporan rinci per baris (berhasil/gagal+penyebab teknis lengkap)
									// otomatis diunduh sbg berkas teks; menggantikan popup generik lama.
									laporan.selesaikan(null);
								}

							}
						});
						timer.start();

					}
				});
			}
		});

		String[] contents = new String[] { "id", "mahasiswa", "semester", "tahunAkademik", "ganjilGenap",
				"jenisKegiatan", "berlakuMulai", "berlakuSampai", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(BaypassPembayaranMahasiswa.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, BaypassPembayaranMahasiswa.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	        FilterLanjutHelper.setup(comp);
}

	class MahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final BaypassPembayaranMahasiswa baypassPembayaranMahasiswa = (BaypassPembayaranMahasiswa) arg1;
			final Mahasiswa mahasiswa = baypassPembayaranMahasiswa.getMahasiswa();

			RevisiHelper
					.createNewRevisi(BaypassPembayaranMahasiswa.class, baypassPembayaranMahasiswa, mahasiswa.getNim())
					.setParent(arg0);

			new Label(mahasiswa.getNama()).setParent(arg0);
			new Label(mahasiswa.getTahunangkatan() + "").setParent(arg0);

			new Label(mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama()).setParent(arg0);
			new Label(baypassPembayaranMahasiswa.getTahunAkademik()).setParent(arg0);
			new Label(
					baypassPembayaranMahasiswa.getSemester() + "" + (baypassPembayaranMahasiswa.getTahap() == null ? ""
							: " / Tahap " + baypassPembayaranMahasiswa.getTahap()))
					.setParent(arg0);

			new Label(baypassPembayaranMahasiswa.getJenisKegiatan() == null ? "Semua"
					: baypassPembayaranMahasiswa.getJenisKegiatan().getNamaKegiatan()).setParent(arg0);

			new Label((baypassPembayaranMahasiswa.getBerlakuMulai() == null ? ""
					: Common.dateFormat11.get().format(baypassPembayaranMahasiswa.getBerlakuMulai()))
					+ " s.d "
					+ (baypassPembayaranMahasiswa.getBerlakuSampai() == null ? ""
							: Common.dateFormat11.get().format(baypassPembayaranMahasiswa.getBerlakuSampai())))
					.setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(baypassPembayaranMahasiswa);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											if (baypassPembayaranMahasiswa != null
													&& baypassPembayaranMahasiswa.getId() != null
													&& baypassPembayaranMahasiswa.getJenisKegiatan() != null
													&& baypassPembayaranMahasiswa.getJenisKegiatan()
															.getDigunakanSyaratKeaktifan()) {
												boolean terlambarLangsungTidakAktif = Common
														.getKonfigurasi("mhs_all_lambat_bayar_langsung_tidak_aktif", "",
																baypassPembayaranMahasiswa.getSemester(),
																baypassPembayaranMahasiswa.getMahasiswa()
																		.getTahunangkatan(),
																baypassPembayaranMahasiswa.getMahasiswa().getJurusan(),
																baypassPembayaranMahasiswa.getMahasiswa().getProgram(),
																baypassPembayaranMahasiswa.getMahasiswa()
																		.getStatusAwalMahasiswa())
														.getNilai().equals(Konfigurasi.AKTIF);
												if (terlambarLangsungTidakAktif) {

													boolean checkStatusPembayaranMahasiswa = false;

													Mahasiswa mahasiswa = baypassPembayaranMahasiswa.getMahasiswa();
													KegiatanHelper.updateBatasStudiMahasiswa(mahasiswa, null,
															baypassPembayaranMahasiswa.getSemester(),
															checkStatusPembayaranMahasiswa);

													HistoryStatusMahasiswa historyStatusMahasiswa = Common
															.currentStatus(baypassPembayaranMahasiswa.getMahasiswa(),
																	baypassPembayaranMahasiswa.getTahunAkademik(),
																	baypassPembayaranMahasiswa.getSemester());
													historyStatusMahasiswa.put(checkStatusPembayaranMahasiswa + "",
															"checkStatusPembayaranMahasiswa");
													historyStatusMahasiswa.setStatusMahasiswa(
															checkStatusPembayaranMahasiswa ? ConstantValues.AKTIF
																	: ConstantValues.TIDAK_AKTIF);
													System.out.println("mahasiswa " + mahasiswa
															+ ", checkStatusPembayaranMahasiswa "
															+ checkStatusPembayaranMahasiswa);

													Common.refreshSaveOrUpdate(historyStatusMahasiswa);

												}
											}

											Common.refreshDelete(baypassPembayaranMahasiswa);
											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(
													"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
										}

									}

								}
							});

				}
			});
			button.setParent(toolbar);
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(arg0);
		}

	}

	public Criteria initCriteria(boolean order) {

		StatusMahasiswa statusMahasiswa = (StatusMahasiswa) (searchstatus.getSelectedItem() == null
				|| searchstatus.getSelectedItem().getValue() == null ? null
						: searchstatus.getSelectedItem().getValue());

		Criterion criteriaStatus = Restrictions.sqlRestriction("true");
		if (statusMahasiswa != null) {
			String sql = "this_.mahasiswa in (select mahasiswa from history_status_mahasiswa where status_mahasiswa="
					+ statusMahasiswa.getId() + " and tahunakademik = '" + Common.getCurrentTahunAkademik()
					+ "' and semester%2=" + (Common.isNowSemensterGanjil() ? 1 : 0) + ")";
			System.out.println("sql=>" + sql);
			criteriaStatus = Restrictions.sqlRestriction(sql);
		}

		Session session = HibernateUtil.currentSession();
		Dosen dosen = (Dosen) searchdosen.getAttribute("myValue");
		Criteria criteria = session.createCriteria(BaypassPembayaranMahasiswa.class).add(criteriaStatus)
				.createCriteria("mahasiswa")
				.add(dosen != null ? Restrictions.eq("dosen", dosen.getId()) : Restrictions.sqlRestriction("1=1"))
				.add(searchdosenPA.isChecked() ? Restrictions.isNull("dosen") : Restrictions.sqlRestriction("1=1"));
		if (order)
			criteria.addOrder(Order.desc("tahunangkatan")).addOrder(Order.desc("tahunangkatan"))
					.addOrder(Order.asc("nim"));
		criteria.add(searchnama.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
				: Restrictions.ilike("nama", searchnama.getValue(), MatchMode.ANYWHERE))
				.add(searchnim.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nim", searchnim.getValue(), MatchMode.ANYWHERE))
				.add(searchprogram.getSelectedItem() == null || searchprogram.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("program", searchprogram.getSelectedItem().getValue()))
				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

				.add(searchStatusAwalMahasiswa.getSelectedItem() == null
						|| searchStatusAwalMahasiswa.getSelectedItem().getValue() == null
						|| searchStatusAwalMahasiswa.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("statusAwalMahasiswa",
										searchStatusAwalMahasiswa.getSelectedItem().getValue()))
				.add(searchtahun.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahunangkatan", searchtahun.getValue().intValue()))

				.add(searchkonsentrasi.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("konsentrasi", searchkonsentrasi.getSelectedItem().getValue()))

				.createCriteria("jurusan", Criteria.LEFT_JOIN)

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false))
				.add(searchjenjang.getSelectedItem() == null || searchjenjang.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("jenjang", searchjenjang.getSelectedItem().getValue()));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<BaypassPembayaranMahasiswa> baypassPembayaranMahasiswas = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(baypassPembayaranMahasiswas);
		grid.setRowRenderer(new MahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void onAdd(Event event) throws Exception {
		init(new BaypassPembayaranMahasiswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(BaypassPembayaranMahasiswa baypassPembayaranMahasiswa) {
		this.baypassPembayaranMahasiswa = baypassPembayaranMahasiswa;
		addWindow.setTitle("Baypass Pembayaran Mahasiswa");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Mahasiswa *"));
		row.appendChild(mahasiswa = new AmbilDataMahasiswaBanbox());
		mahasiswa.setAttribute("mahasiswa", baypassPembayaranMahasiswa.getMahasiswa());
		mahasiswa.setValue(baypassPembayaranMahasiswa.getMahasiswa() == null ? ""
				: baypassPembayaranMahasiswa.getMahasiswa().getNama());
		mahasiswa.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Digunakan untuk pembayaran"));
		row.appendChild(jenisKegiatan = new Combobox());
		Common.insertComboDanSemua(jenisKegiatan, "namaKegiatan", JenisKegiatan.class);
		Common.selectComboItem(jenisKegiatan, baypassPembayaranMahasiswa.getJenisKegiatan());
		jenisKegiatan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik *"));
		row.appendChild(tahunAkademik);
		Common.selectComboItem(tahunAkademik,
				baypassPembayaranMahasiswa.getTahunAkademik() == null ? Common.getCurrentTahunAkademik()
						: baypassPembayaranMahasiswa.getTahunAkademik());
		tahunAkademik.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Semester *"));
		row.appendChild(ganjilGenap);
		Common.selectComboItem(ganjilGenap, baypassPembayaranMahasiswa.getGanjilGenap() == null ? Perkuliahan.GANJIL
				: baypassPembayaranMahasiswa.getGanjilGenap());
		ganjilGenap.setWidth("90%");
		ganjilGenap.setReadonly(true);

		final MyFormRow rowSemester = new MyFormRow();
		rowSemester.setStyle("border:0px;background: transparent;");
		rowSemester.setParent(rows);
		rowSemester.appendChild(new Label(ais.common.Common.getBahasaConfig("Semester")));
		rowSemester.appendChild(lblSemester = new Label(baypassPembayaranMahasiswa.getSemester() == null ? ""
				: baypassPembayaranMahasiswa.getSemester().toString()));

		class SemesterEventListener implements EventListener {

			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub
				Common.clear(lblSemester);
				Integer semesterInt = 0;
				Integer tahun = Integer
						.parseInt(StringUtils.split((String) tahunAkademik.getSelectedItem().getValue(), "/")[0]);

				if (ganjilGenap.getSelectedItem().getValue().equals(Perkuliahan.GANJIL)) {

					if (mahasiswa.getAttribute("mahasiswa") != null) {
						Mahasiswa mahasiswaSelected = (Mahasiswa) mahasiswa.getAttribute("mahasiswa");
						if (tahun.equals(mahasiswaSelected.getTahunangkatan())) {
							semesterInt = 1;
						} else {
							semesterInt = Common.getSemester(mahasiswaSelected.getTahunangkatan(), Perkuliahan.GANJIL,
									mahasiswaSelected.getPindahKeKampusIniMasukSemester(), tahun,
									mahasiswaSelected.getSemesterMulai());
						}
					}

				} else if (ganjilGenap.getSelectedItem().getValue().equals(Perkuliahan.GENAP)) {

					if (mahasiswa.getAttribute("mahasiswa") != null) {
						Mahasiswa mahasiswaSelected = (Mahasiswa) mahasiswa.getAttribute("mahasiswa");
						semesterInt = Common.getSemester(mahasiswaSelected.getTahunangkatan(), Perkuliahan.GENAP,
								mahasiswaSelected.getPindahKeKampusIniMasukSemester(), tahun,
								mahasiswaSelected.getSemesterMulai());
					}
				}
				System.out.println("tahun : " + tahun);
				System.out.println("Mhass : " + semesterInt);

				Common.clear(lblSemester);
				lblSemester.setValue(semesterInt + "");

			}
		}

		ganjilGenap.addEventListener("onChange", new SemesterEventListener());
		tahunAkademik.addEventListener("onChange", new SemesterEventListener());

		if (ConstantValues.aktifkanTahapan) {
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Tahap"));
			row.appendChild(tahap = new Combobox());
			int jumlahTahapan = 3;

			for (int i = 1; i <= (jumlahTahapan * 5); i++) {
				MyComboitemConfig comboitem = new MyComboitemConfig("Tahap " + i);
				comboitem.setValue(i);
				tahap.appendChild(comboitem);
			}
			tahap.setReadonly(true);

			Common.selectComboItem(tahap, baypassPembayaranMahasiswa.getTahap());
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Masa Berlaku"));
		Hbox hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(berlakuMulai = new MyDatebox(baypassPembayaranMahasiswa.getBerlakuMulai()));
		hbox.appendChild(new MyLabelConfig(" s.d "));
		hbox.appendChild(berlakuSampai = new MyDatebox(baypassPembayaranMahasiswa.getBerlakuSampai()));

		berlakuMulai.setCols(6);
		berlakuSampai.setCols(6);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				baypassPembayaranMahasiswa.getKeterangan() == null ? "" : baypassPembayaranMahasiswa.getKeterangan()));
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
		if (mahasiswa.getAttribute("mahasiswa") == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Data mahasiswa",
					"Kolom Data mahasiswa belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Data mahasiswa.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (tahunAkademik.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Tahun Akademik",
					"Kolom Tahun Akademik belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Tahun Akademik.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		if (ganjilGenap.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Jenis Semester",
					"Kolom Jenis Semester belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Jenis Semester.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		BaypassPembayaranMahasiswaDao baypassPembayaranMahasiswaDao = DaoFactory.getInstance()
				.getBaypassPembayaranMahasiswaDao();
		if (baypassPembayaranMahasiswa.getId() != null) {
			baypassPembayaranMahasiswa = baypassPembayaranMahasiswaDao.load(baypassPembayaranMahasiswa.getId());

		}

		baypassPembayaranMahasiswa.setSemester(lblSemester.getValue() == null || lblSemester.getValue().isEmpty() ? null
				: Integer.parseInt(lblSemester.getValue()));
		baypassPembayaranMahasiswa.setTahunAkademik((String) tahunAkademik.getSelectedItem().getValue());
		baypassPembayaranMahasiswa.setGanjilGenap((String) ganjilGenap.getSelectedItem().getValue());
		baypassPembayaranMahasiswa.setMahasiswa((Mahasiswa) mahasiswa.getAttribute("mahasiswa"));
		baypassPembayaranMahasiswa.setKeterangan(keterangan.getValue());
		baypassPembayaranMahasiswa.setTahap((Integer) (tahap == null || tahap.getSelectedItem() == null ? null
				: tahap.getSelectedItem().getValue()));
		baypassPembayaranMahasiswa.setJenisKegiatan((JenisKegiatan) (jenisKegiatan.getSelectedItem() == null ? null
				: jenisKegiatan.getSelectedItem().getValue()));

		baypassPembayaranMahasiswa.setBerlakuMulai(berlakuMulai.getValue());
		baypassPembayaranMahasiswa.setBerlakuSampai(berlakuSampai.getValue());

		if (baypassPembayaranMahasiswa.getId() != null) {
			baypassPembayaranMahasiswaDao.update(baypassPembayaranMahasiswa);
		} else {
			baypassPembayaranMahasiswaDao.save(baypassPembayaranMahasiswa);
		}

		if (baypassPembayaranMahasiswa != null && baypassPembayaranMahasiswa.getId() != null
				&& baypassPembayaranMahasiswa.getJenisKegiatan() != null
				&& baypassPembayaranMahasiswa.getJenisKegiatan().getDigunakanSyaratKeaktifan()) {
			boolean terlambarLangsungTidakAktif = Common
					.getKonfigurasi("mhs_all_lambat_bayar_langsung_tidak_aktif", "",
							baypassPembayaranMahasiswa.getSemester(),
							baypassPembayaranMahasiswa.getMahasiswa().getTahunangkatan(),
							baypassPembayaranMahasiswa.getMahasiswa().getJurusan(),
							baypassPembayaranMahasiswa.getMahasiswa().getProgram(),
							baypassPembayaranMahasiswa.getMahasiswa().getStatusAwalMahasiswa())
					.getNilai().equals(Konfigurasi.AKTIF);
			if (terlambarLangsungTidakAktif) {
				boolean checkStatusPembayaranMahasiswa = true;

				Mahasiswa mahasiswa = baypassPembayaranMahasiswa.getMahasiswa();
				KegiatanHelper.updateBatasStudiMahasiswa(mahasiswa, null, baypassPembayaranMahasiswa.getSemester(),
						checkStatusPembayaranMahasiswa);

				HistoryStatusMahasiswa historyStatusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(
						baypassPembayaranMahasiswa.getMahasiswa(), baypassPembayaranMahasiswa.getTahunAkademik(),
						baypassPembayaranMahasiswa.getSemester());
				historyStatusMahasiswa.put(checkStatusPembayaranMahasiswa + "", "checkStatusPembayaranMahasiswa");
				historyStatusMahasiswa.setStatusMahasiswa(
						checkStatusPembayaranMahasiswa ? ConstantValues.AKTIF : ConstantValues.TIDAK_AKTIF);
				System.out.println("mahasiswa " + mahasiswa + ", checkStatusPembayaranMahasiswa "
						+ checkStatusPembayaranMahasiswa);

				Common.refreshSaveOrUpdate(historyStatusMahasiswa);

			}
		}

		return true;
	}

	@Override
	public void loadData(Object value) {
		onSearchDefault(null);
	}

}
