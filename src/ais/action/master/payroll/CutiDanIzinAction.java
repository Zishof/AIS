package ais.action.master.payroll;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.employ.helper.ParameterTambahanCutiDanIzinListener;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.action.report.format1.akademik.LaporanCutiDanIzin;
import ais.action.report.format1.payroll.LaporanCutiPegawai;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.UIClassHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.CutiBersama;
import ais.database.model.GeneralValueObject;
import ais.database.model.Pegawai;
import ais.database.model.Statusabsensi;
import ais.database.model.StatuskehadiranKaryawanHarian;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.employ.JenisCutiDanIzin;
import ais.database.model.employ.KelompokParameterTambahanCutiDanIzin;
import ais.database.model.file.LampiranLain;
import ais.database.model.payroll.CutiDanIzin;
import ais.database.model.payroll.LiburNasional;
import ais.database.model.rab.Pejabat;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.FormSop;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.WaktuUtil;

public class CutiDanIzinAction extends GenericAutowireComposer
		implements DataInitDefault, DataSearchDefault, DataCriteria, FormSop {

	private static final long serialVersionUID = -5779730267402400328L;

	private Window addWindow;
	private MyGrid grid;
	private Paging paging;
	private Textbox searchpegawai;
	private MyDatebox start;
	private MyDatebox end;
	private MyLabelConfig labelSatker;
	private AmbilDataSatuanKerjaBanbox searchparent;
	private AmbilDataPegawaiBanbox pegawai;
	private Combobox statusabsensi;
	private MyDatebox mulai;
	private MyDatebox sampai;
	private MyTextbox keterangan;

	private boolean edit = false;
	private boolean delete = false;
	private boolean persetujuan = false;
	private boolean admin = false;
	private boolean bolehPilihanPotong = false;
	private String jenis = "";

	private CutiDanIzin cutiDanIzin;
	private MyToolbarbuttonConfig add;
	private Tbmuser tbmuser = null;

	// Gunakan HashSet untuk memori efisien & akses O(1) sangat cepat (menghindari
	// duplikat)
	private Set<Long> punyaBawahan = new HashSet<Long>();
	private Set<Long> punyaBawahanDosen = new HashSet<Long>();

	private Checkbox searchaktif;
	private Tabpanel cutiTab;
	private Tabpanel jenisKehadiran;
	private Tabpanel pengajuan;
	private DisposisiSop disposisiSop;
	private Tabpanel laporanCutiTab;
	protected LampiranLain lainMahasiswa = null;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private Combobox jenisCutiDanIzin;
	protected ArrayList<Row> parameterRows;
	protected HashMap<String, LampiranLain> lampiranLains;
	protected ParameterTambahanCutiDanIzinListener parameterTambahanListener;

	private Label sisaCuti;
	private Label jumlahCuti;
	private Label jumlahHariCuti;
	private Label jumlahCutiBersama;
	private Vbox masaKerja;
	private Label masaKerjaCuti;
	private Label masaKerjaCutiMax;
	private Label jumlahCutiDiambil;

	private Vbox containerPengecualian;

	public CutiDanIzinAction() {
		this.persetujuan = false;
	}

	public CutiDanIzinAction(boolean persetujuan, String jenis) {
		this.persetujuan = persetujuan;
		this.jenis = jenis;
	}

	public void onCutiBersama(Event event) {
		if (cutiTab.getChildren().size() == 0) {
			MyInclude include = new MyInclude("/pages/master/payroll/cuti_bersama.zul");
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(cutiTab);
		}
	}

	public void onJenisCuti(Event event) {
		if (jenisKehadiran.getChildren().size() == 0) {
			MyInclude include = new MyInclude("/pages/master/status_absensi.zul");
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(jenisKehadiran);
		}
	}

	public void onLaporanCuti(Event event) {
		if (laporanCutiTab.getChildren().size() == 0) {
			LaporanCutiPegawai include = new LaporanCutiPegawai();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(laporanCutiTab);
		}
	}

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);

		jenis = execution.getParameter("jenis") == null ? "" : execution.getParameter("jenis").trim();
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
		admin = Common.getApakahAdminBolehLihatSemuaCuti();
		tbmuser = Common.getCurrentUser();
		bolehPilihanPotong = Common.getApakahAdminBolehLihatSemuaPegawai();

		initDatesFilter();
		setupPrivilegesAndVisibility();

		if (!admin) {
			siapkanDataBawahan();
		}

		searchparent.setEventListener(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		}); 

		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		setupToolbars();
	}

	private void initDatesFilter() {
		if (start != null) start.setReadonly(true);
		if (end != null) end.setReadonly(true);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - 1);
		if (start != null) start.setValue(calendar.getTime());

		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) + 1);
		if (end != null) end.setValue(calendar.getTime());
	}

	private void setupPrivilegesAndVisibility() {
		if (tbmuser != null && tbmuser.ambilPegawai() != null) {
			add.setVisible(true);
			add.setTooltiptext("Tambah");
			edit = true;
			delete = true;

			if (cutiTab != null) {
				cutiTab.setVisible(false);
				cutiTab.getLinkedTab().setVisible(false);
			}
			if (jenisKehadiran != null) {
				jenisKehadiran.setVisible(false);
				jenisKehadiran.getLinkedTab().setVisible(false);
			}
			if (pengajuan != null) {
				pengajuan.setVisible(false);
				pengajuan.getLinkedTab().setVisible(false);
			}
		} else {
			if (add != null) {
			add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
			add.setTooltiptext("Tambah");
			}
			edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
			delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		}
	}

	private void setupToolbars() throws Exception {
		String[] contents = new String[] { "id", "pegawai", "mulai", "sampai", "statusabsensi", "memotongJatahCuti",
				"setujui", "disetujiOleh", "setujuiTanggal", "jenisCutiDanIzin", "keterangan", "parameterTambahan",
				"parameterTambahanInds", "jumlahHariCuti", "jumlahCuti", "sisaCuti" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(CutiDanIzin.class, this, contents);
		if (add != null) {
		add.getParent().appendChild(cetakToolbarbutton);
		}

		if (admin) {
			MyToolbarbuttonConfig upload = Common.uploadData(this, CutiDanIzin.class, contents);
			upload.setVisible((add != null && add.isVisible()) && edit && delete);
			if (add != null) {
			add.getParent().appendChild(upload);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void siapkanDataBawahan() {
		if (tbmuser == null)
			return;

		Session session = null;
		try {
			session = HibernateUtil.currentSession();

			if (tbmuser.getPegawai() != null) {
				Criterion criterion = Restrictions.or(Restrictions.eq("atasanlangsung", tbmuser.getPegawai()),
						Restrictions.or(Restrictions.eq("atasanlangsung3", tbmuser.getPegawai()),
								Restrictions.eq("atasanlangsung2", tbmuser.getPegawai())));

				Criteria criteria = session.createCriteria(Pegawai.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

				if (tbmuser.getDosen() != null) {
					criteria.createAlias("atasanlangsung", "atasanlangsung", Criteria.LEFT_JOIN)
							.createAlias("atasanlangsung2", "atasanlangsung2", Criteria.LEFT_JOIN)
							.createAlias("atasanlangsung3", "atasanlangsung3", Criteria.LEFT_JOIN);

					criterion = Restrictions.or(criterion,
							Restrictions.or(Restrictions.eq("atasanlangsung.dosen", tbmuser.getDosen()),
									Restrictions.or(Restrictions.eq("atasanlangsung3.dosen", tbmuser.getDosen()),
											Restrictions.eq("atasanlangsung2.dosen", tbmuser.getDosen()))));
				} else if (tbmuser.getGuru() != null) {
					criteria.createAlias("atasanlangsung", "atasanlangsung", Criteria.LEFT_JOIN)
							.createAlias("atasanlangsung2", "atasanlangsung2", Criteria.LEFT_JOIN)
							.createAlias("atasanlangsung3", "atasanlangsung3", Criteria.LEFT_JOIN);

					criterion = Restrictions.or(criterion,
							Restrictions.or(Restrictions.eq("atasanlangsung.guru", tbmuser.getGuru()),
									Restrictions.or(Restrictions.eq("atasanlangsung3.guru", tbmuser.getGuru()),
											Restrictions.eq("atasanlangsung2.guru", tbmuser.getGuru()))));
				}

				List<Long> bwhn = criteria.add(criterion).setProjection(Projections.property("id")).list();
				if (bwhn != null)
					punyaBawahan.addAll(bwhn);

				if (tbmuser.hakAkses() != null && tbmuser.hakAkses().getJenisJabatan() != null) {
					List<Long> bawahanJabatan = session.createCriteria(Pegawai.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.or(
									Restrictions.eq("atasan.id", tbmuser.hakAkses().getJenisJabatan().getId()),
									Restrictions.or(
											Restrictions.eq("atasanPendukung.id",
													tbmuser.hakAkses().getJenisJabatan().getId()),
											Restrictions.eq("atasanPendukungCadangan.id",
													tbmuser.hakAkses().getJenisJabatan().getId()))))
							.setProjection(Projections.property("id")).list();
					if (bawahanJabatan != null && !bawahanJabatan.isEmpty()) {
						punyaBawahan.addAll(bawahanJabatan);
					}
				} else {
					List<Tbmrole> roles = tbmuser.ambilRoles();
					boolean ada = false;
					for (Tbmrole tbmrole : roles) {
						if (tbmrole != null && tbmrole.getJenisJabatan() != null) {
							ada = true;
							break;
						}
					}

					if (!ada) {
						List<Long> pejabats = session
								.createCriteria(
										Pejabat.class)
								.setProjection(Projections.groupProperty("jenisJabatan.id"))
								.add(Restrictions.or(
										Restrictions.or(
												Restrictions.ilike("jenisPengguna",
														"," + tbmuser.hakAkses().getRoleId() + ",", MatchMode.ANYWHERE),
												Restrictions.ilike("usernamePengguna", "," + tbmuser.getUserId() + ",",
														MatchMode.ANYWHERE)),
										Restrictions.and(
												Restrictions.or(
														Restrictions.isNotNull("pegawai"),
														Restrictions.or(
																Restrictions.isNotNull("guru"),
																Restrictions.isNotNull("dosen"))),
												Restrictions.or(Restrictions.eq("pegawai", tbmuser.getPegawai()),
														Restrictions.or(Restrictions.eq("dosen", tbmuser.getDosen()),
																Restrictions.eq("guru", tbmuser.getGuru()))))))
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.list();

						if (pejabats != null && !pejabats.isEmpty()) {
							List<Long> bawahanJabatan = session.createCriteria(Pegawai.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(Restrictions.or(Restrictions.in("atasan.id", pejabats),
											Restrictions.or(Restrictions.in("atasanPendukung.id", pejabats),
													Restrictions.in("atasanPendukungCadangan.id", pejabats))))
									.setProjection(Projections.property("id")).list();
							if (bawahanJabatan != null && !bawahanJabatan.isEmpty()) {
								punyaBawahan.addAll(bawahanJabatan);
							}
						}
					}
				}

				if (!tbmuser.hakAkses().getMelihatDataPegawaiLain()) {
					punyaBawahan.add(tbmuser.getPegawai().getId());
				}
			}

			if (tbmuser.getDosen() != null) {
				List<Long> bwhnDosen = session.createCriteria(Pegawai.class).createAlias("dosen", "dosen")
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.or(Restrictions.eq("atasanlangsung", tbmuser.getPegawai()),
								Restrictions.or(Restrictions.eq("atasanlangsung3", tbmuser.getPegawai()),
										Restrictions.eq("atasanlangsung2", tbmuser.getPegawai()))))
						.setProjection(Projections.groupProperty("dosen.id")).list();
				if (bwhnDosen != null)
					punyaBawahanDosen.addAll(bwhnDosen);
				punyaBawahanDosen.add(tbmuser.getDosen().getId());
			}

			if ((punyaBawahan != null && !punyaBawahan.isEmpty())
					|| (punyaBawahanDosen != null && !punyaBawahanDosen.isEmpty())) {
				searchparent.setValue("");
				searchparent.setAttribute("satuanKerja", null);
				searchparent.setAttribute("myValue", null);
				searchparent.setVisible(false);
				labelSatker.setVisible(false);
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}

	class CutiDanIzinRenderer extends ais.ui.util.MyRowRenderer {
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final CutiDanIzin cutiData = (CutiDanIzin) arg1;

			CommonMedia.tampilkanGambarKecil(cutiData.getPegawai()).setParent(arg0);

			Vbox a = RevisiHelper.createNewRevisi(CutiDanIzin.class, cutiData, cutiData.getPegawai().getNama());
			a.setParent(arg0);

			if (cutiData.getDiajukanOleh() != null) {
				new Label("Diajukan oleh : " + cutiData.getDiajukanOleh().getUserNama()).setParent(a);
			}
			if (cutiData.getDisetujuiOleh() != null) {
				new Label("Disetujui oleh : " + cutiData.getDisetujuiOleh().getUserNama()).setParent(a);
			}
			if (cutiData.getSetujuiTanggal() != null) {
				new Label("Disetujui tgl : " + Common.dateFormat1.get().format(cutiData.getSetujuiTanggal()))
						.setParent(a);
			}

			Vbox myvbox = new Vbox();
			myvbox.setParent(a);
			Hbox hbox = new Hbox();
			hbox.setParent(myvbox);

			LampiranLain.createDownloadUploadFileLain(hbox, cutiData.getId(), CutiDanIzin.class.getName(), "Lampiran",
					false, null, null, false, false, false, false);

			myvbox = new Vbox();
			myvbox.setParent(arg0);
			new Label(cutiData.getStatusabsensi() == null ? "" : cutiData.getStatusabsensi().getNama())
					.setParent(myvbox);
			new Label(cutiData.getJenisCutiDanIzin() == null ? "" : cutiData.getJenisCutiDanIzin().getNama())
					.setParent(myvbox);
			new Label(cutiData.getMulai() == null ? "" : Common.dateFormat4.get().format(cutiData.getMulai()))
					.setParent(arg0);
			new Label(cutiData.getSampai() == null ? "" : Common.dateFormat4.get().format(cutiData.getSampai()))
					.setParent(arg0);

			if (cutiData.getMulai() != null && cutiData.getSampai() != null) {
				new Label(Common.numberFormat.get().format(cutiData.getJumlahHariCuti()) + " hari").setParent(arg0);
			} else {
				new Label().setParent(arg0);
			}

			new Label(
					cutiData.getJumlahCuti() == null ? "" : Common.numberFormat.get().format(cutiData.getJumlahCuti()))
					.setParent(arg0);
			new Label(cutiData.getJumlahCutiBersama() == null ? ""
					: Common.numberFormat.get().format(cutiData.getJumlahCutiBersama())).setParent(arg0);
			new Label(cutiData.getSisaCuti() == null ? "" : Common.numberFormat.get().format(cutiData.getSisaCuti()))
					.setParent(arg0);

			Vbox vbox1 = new Vbox();
			vbox1.setParent(arg0);

			if (cutiData.getDisposisiSop() != null) {
				new MyLabelKecil(Common.simpleString(cutiData.getKeterangan())).setParent(vbox1);
				A aa = new A();
				aa.setParent(vbox1);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa, "SOP " + cutiData.getDisposisiSop().getKeterangan() + " ("
						+ cutiData.getDisposisiSop().getSop().getNama() + ")");
				aa.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event ev) throws Exception {
						TampilanAlurSopAction.prosess(cutiData.getDisposisiSop().getId(), null, null, true,
								ev.getTarget());
					}
				});
			} else {
				new MyLabelKecil(cutiData.getKeterangan()).setParent(vbox1);
			}

			boolean isBawahan = isBawahanChecker(cutiData.getPegawai());

			if (tbmuser != null && tbmuser.getPegawai() != null && cutiData.getPegawai() != null
					&& !cutiData.getPegawai().getId().equals(tbmuser.getPegawai().getId()) && isBawahan) {

				final MyCheckboxConfig checkbox = new MyCheckboxConfig("Setujui");
				checkbox.setChecked(cutiData.getSetujui());
				checkbox.setParent(arg0);
				arg0.setAttribute("checkbox", checkbox);
				checkbox.addEventListener("onCheck", new EventListener() {
					@Override
					public void onEvent(Event a) throws Exception {
						cutiData.setSetujui(checkbox.isChecked());
						if (checkbox.isChecked()) {
							cutiData.setDisetujuiOleh(tbmuser);
							cutiData.setSetujuiTanggal(WaktuUtil.getDate());
						} else {
							cutiData.setDisetujuiOleh(null);
							cutiData.setSetujuiTanggal(null);
						}
						Common.refreshSaveOrUpdate(cutiData);
						updateStatusAbsensi(cutiData, cutiData.getStatusabsensi());
						Common.clear(arg0);
						render(arg0, cutiData);
					}
				});

				if (bolehPilihanPotong) {
					final MyCheckboxConfig memotongJatahCuti = new MyCheckboxConfig("Dipotong");
					memotongJatahCuti.setDisabled(!edit);
					memotongJatahCuti.setChecked(cutiData.getMemotongJatahCuti());
					memotongJatahCuti.setParent(arg0);
					arg0.setAttribute("checkbox", memotongJatahCuti);
					memotongJatahCuti.addEventListener("onCheck", new EventListener() {
						@Override
						public void onEvent(Event ev) throws Exception {
							cutiData.setMemotongJatahCuti(memotongJatahCuti.isChecked());
							Common.refreshSaveOrUpdate(cutiData);
						}
					});
				} else {
					new Label(cutiData.getMemotongJatahCuti() ? "Ya" : "Tidak").setParent(arg0);
				}
			} else {
				new Label(cutiData.getSetujui() ? "Ya" : "Tidak").setParent(arg0);
				new Label(cutiData.getMemotongJatahCuti() ? "Ya" : "Tidak").setParent(arg0);
			}

			// kebab popup (⋯) via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			if (!cutiData.getSetujui()) {
				Hbox hb = Common.copyEditDeleteButtons(edit, delete, cutiData, CutiDanIzinAction.this);
				aksiButtons.addAll(ais.ui.util.UIHelper.ambilItemAksi(hb));
			}

			if (cutiData.getJenisCutiDanIzin() != null) {
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
				button.setStyle("font-size:9px;");
				button.setOrient("vertical");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event ev) throws Exception {
						LaporanCutiDanIzin.cetak(cutiData);
					}
				});
				aksiButtons.add(button);
			}

			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);
		}

		private boolean isBawahanChecker(Pegawai p) {
			if (p == null || tbmuser == null || tbmuser.getPegawai() == null)
				return false;

			// Karena punyaBawahan berbentuk Set, method "contains" adalah komputasi Instan
			// O(1)
			if (punyaBawahan != null && punyaBawahan.contains(p.getId()))
				return true;
			if (p.getAtasanlangsung() != null && tbmuser.getPegawai().getId().equals(p.getAtasanlangsung().getId()))
				return true;
			if (p.getAtasanlangsung2() != null && tbmuser.getPegawai().getId().equals(p.getAtasanlangsung2().getId()))
				return true;
			if (p.getAtasanlangsung3() != null && tbmuser.getPegawai().getId().equals(p.getAtasanlangsung3().getId()))
				return true;

			Tbmrole tbmrole = tbmuser.hakAkses();
			if (tbmrole != null && tbmrole.getRoleId() != null) {
				try {
					if (p.getAtasan() != null && p.getAtasan().getJenisPengguna() != null
							&& p.getAtasan().getJenisPengguna().toLowerCase()
									.contains("," + tbmrole.getRoleId().toLowerCase() + ","))
						return true;
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/payroll/CutiDanIzinAction.java:596");
					// TODO: handle exception
				}
				try {
					if (p.getAtasanPendukung() != null && p.getAtasanPendukung().getJenisPengguna() != null
							&& p.getAtasanPendukung().getJenisPengguna().toLowerCase()
									.contains("," + tbmrole.getRoleId().toLowerCase() + ","))
						return true;
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/payroll/CutiDanIzinAction.java:603");
					// TODO: handle exception
				}
				try {
					if (p.getAtasanPendukungCadangan() != null && p.getAtasanPendukungCadangan().getJenisPengguna() != null
							&& p.getAtasanPendukungCadangan().getJenisPengguna()
									.toLowerCase().contains("," + tbmrole.getRoleId().toLowerCase() + ","))
						return true;
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/payroll/CutiDanIzinAction.java:610");
					// TODO: handle exception
				}
			}

			return false;
		}
	}

	public void onAdd(Event event) throws Exception {
		init(new CutiDanIzin());
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		CutiDanIzin cdi = (CutiDanIzin) obj;

		addWindow.setTitle(obj.getId() == null ? "Tambah Cuti dan Izin" : "Ubah Cuti dan Izin");
		Common.clear(addWindow);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setAutoscroll(true);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		disposisiSop = null;
		center.appendChild(form(cdi, disposisiSop, save, null));

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(south);

		Toolbarbutton cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);

		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
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
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings({ "unchecked" })
	@Override
	public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop dSop, final MyToolbarbuttonConfig saveBtn,
			EventListener setujui) throws Exception {
		this.cutiDanIzin = (CutiDanIzin) generalValueObject;
		this.disposisiSop = (this.disposisiSop != null && (dSop == null || dSop.getId() == null)) ? this.disposisiSop
				: dSop;

		tbmuser = Common.getCurrentUser();
		MyGrid myGrid = new MyGrid();
		myGrid.setWidth("100%");

		Columns columns = new Columns();
		columns.setParent(myGrid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(myGrid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Pegawai *")));

		Tbmrole tbmrole = tbmuser == null ? null : tbmuser.hakAkses();
		boolean editdata = tbmrole != null && tbmrole.getMengajukanPengajuanPegawaiLain();
		pegawai = new AmbilDataPegawaiBanbox(editdata);

		if (persetujuan) {
			row.appendChild(new Label(cutiDanIzin.getPegawai() == null ? "" : cutiDanIzin.getPegawai().getNama()));
		} else {
			row.appendChild(pegawai);
		}

		pegawai.setAttribute("pegawai", cutiDanIzin.getPegawai());
		pegawai.setValue(cutiDanIzin.getPegawai() == null ? "" : cutiDanIzin.getPegawai().getNama());
		pegawai.setWidth("90%");

		if (!editdata) {
			if (cutiDanIzin.getPegawai() == null && tbmuser != null && tbmuser.getPegawai() != null) {
				pegawai.setAttribute("pegawai", tbmuser.getPegawai());
				pegawai.setValue(tbmuser.getPegawai() == null ? "" : tbmuser.getPegawai().getNama());
				pegawai.setDisabled(true);
			}
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Status Izin *")));
		statusabsensi = new Combobox();

		if (persetujuan) {
			row.appendChild(
					new Label(cutiDanIzin.getStatusabsensi() == null ? "" : cutiDanIzin.getStatusabsensi().getNama()));
		} else {
			row.appendChild(statusabsensi);
		}

		Map<Long, GeneralValueObject> cutiMap = ConstantValues.ambilBerdasarClass(Statusabsensi.class);
		TreeMap<String, Statusabsensi> ss = new TreeMap<String, Statusabsensi>();

		for (Long cutiId : cutiMap.keySet()) {
			Statusabsensi m = (Statusabsensi) ConstantValues.ambil(Statusabsensi.class.getName(), cutiId);
			if (m != null && m.getAktif()) {
				if (jenis.trim().isEmpty()
						|| (m.getNama() != null && m.getNama().toLowerCase().contains(jenis.toLowerCase()))) {
					ss.put(m.getNama(), m);
				}
			}
		}

		for (Statusabsensi m : ss.values()) {
			Comboitem comboitem = new Comboitem(m.getNama());
			comboitem.setValue(m);
			statusabsensi.appendChild(comboitem);
		}

		if (ss.size() == 1) {
			statusabsensi.setDisabled(true);
			if (cutiDanIzin.getId() == null) {
				cutiDanIzin.setStatusabsensi(ss.values().iterator().next());
			}
		}

		statusabsensi.setWidth("90%");
		Common.selectComboItem(true, statusabsensi, cutiDanIzin.getStatusabsensi());
		statusabsensi.setReadonly(true);

		setupDateAndStatsFields(rows);
		setupEventListeners(rows);

		return myGrid;
	}

	private void setupDateAndStatsFields(Rows rows) {
		MyFormRow row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Berlaku mulai *")));
		mulai = new MyDatebox(cutiDanIzin.getMulai());
		if (persetujuan)
			row.appendChild(new Label(
					cutiDanIzin.getMulai() == null ? "" : Common.dateFormat4.get().format(cutiDanIzin.getMulai())));
		else
			row.appendChild(mulai);
		mulai.setWidth("90%");
		mulai.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Berlaku sampai *")));
		sampai = new MyDatebox(cutiDanIzin.getSampai());
		if (persetujuan)
			row.appendChild(new Label(
					cutiDanIzin.getSampai() == null ? "" : Common.dateFormat4.get().format(cutiDanIzin.getSampai())));
		else
			row.appendChild(sampai);
		sampai.setWidth("90%");
		sampai.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kecualikan Tanggal"));
		containerPengecualian = new Vbox();
		row.appendChild(containerPengecualian);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jumlah Hari Diambil"));
		row.appendChild(jumlahHariCuti = new Label());

		boolean isCutiView = jenis.isEmpty() || jenis.toLowerCase().contains("cuti");

		row = new MyFormRow();
		row.setVisible(isCutiView);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jatah"));
		row.appendChild(jumlahCuti = new Label(cutiDanIzin.getJumlahCuti() == null ? "0"
				: Common.numberFormat.get().format(cutiDanIzin.getJumlahCuti())));

		row = new MyFormRow();
		row.setVisible(isCutiView);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Cuti yang telah diambil"));
		row.appendChild(jumlahCutiDiambil = new Label());

		row = new MyFormRow();
		row.setVisible(isCutiView);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Cuti Bersama"));
		row.appendChild(jumlahCutiBersama = new Label(cutiDanIzin.getJumlahCutiBersama() == null ? "0"
				: Common.numberFormat.get().format(cutiDanIzin.getJumlahCutiBersama())));

		row = new MyFormRow();
		row.setVisible(isCutiView);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sisa Cuti"));
		row.appendChild(sisaCuti = new Label(
				cutiDanIzin.getSisaCuti() == null ? "0" : Common.numberFormat.get().format(cutiDanIzin.getSisaCuti())));

		masaKerjaCuti = new Label();
		row = new MyFormRow();
		row.setVisible(isCutiView);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Cuti ini bisa diambil minimal"));
		row.appendChild(masaKerjaCuti);

		masaKerjaCutiMax = new Label();
		row = new MyFormRow();
		row.setVisible(isCutiView);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Cuti ini bisa diambil maksimal"));
		row.appendChild(masaKerjaCutiMax);

		masaKerja = new Vbox();
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Masa Kerja Pegawai"));
		row.appendChild(masaKerja);
	}

	@SuppressWarnings("deprecation")
	private void setupEventListeners(final Rows rows) {
		EventListener eventListenerMulaiSampai = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				rebuildCheckboxes();
				kalkulasiCuti(false);
			}
		};

		EventListener eventListenerLainnya = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				kalkulasiCuti(false);
			}
		};

		pegawai.setEventListener(eventListenerLainnya);
		statusabsensi.addEventListener("onChange", eventListenerLainnya);

		mulai.addEventListener("onChange", eventListenerMulaiSampai);
		sampai.addEventListener("onChange", eventListenerMulaiSampai);

		try {
			Common.createDefaultTimer(new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					rebuildCheckboxes();
					kalkulasiCuti(false);
				}
			});
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		MyFormRow row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Form Pengajuan"));
		row.appendChild(jenisCutiDanIzin = new Combobox());
		jenisCutiDanIzin.setWidth("90%");
		jenisCutiDanIzin.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Keterangan *")));
		row.appendChild(
				keterangan = new MyTextbox(cutiDanIzin.getKeterangan() == null ? "" : cutiDanIzin.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lampiran Cuti"));
		Hbox hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, cutiDanIzin.getId(), CutiDanIzin.class.getName(),
				"Lampiran Cuti", false, new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						lainMahasiswa = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(row);

		Common.initKeterangan(rows, "Jika file lampiran cuti lebih dari satu file, zip dulu semua file tersebut");

		MyFormRow rowLampiran = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(rowLampiran, "2");
		rowLampiran.setParent(rows);

		final Grid gridLampiran = new Grid();
		gridLampiran.setSclass("fgrid");
		gridLampiran.setParent(rowLampiran);

		Columns columnsLamp = new Columns();
		columnsLamp.setParent(gridLampiran);

		MyColumnConfig columnLamp = new MyColumnConfig();
		columnLamp.setParent(columnsLamp);
		columnLamp.setWidth("30%");

		columnLamp = new MyColumnConfig();
		columnLamp.setParent(columnsLamp);

		final Rows rowsLampiran = new Rows();
		rowsLampiran.setParent(gridLampiran);

		final EventListener eventListenerJenisCutiDanIzin = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(rowsLampiran);
				JenisCutiDanIzin j = (JenisCutiDanIzin) (jenisCutiDanIzin.getSelectedItem() == null ? null
						: jenisCutiDanIzin.getSelectedItem().getValue());
				parameterTambahanListener = null;
				if (j != null) {
					parameterRows = new ArrayList<Row>();
					lampiranLains = new HashMap<String, LampiranLain>();

					try {
						HibernateUtil.currentSession().refresh(j);
					} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

					Set<KelompokParameterTambahanCutiDanIzin> kelompokParameterTambahanCutiDanIzins = new TreeSet<KelompokParameterTambahanCutiDanIzin>();
					for (KelompokParameterTambahanCutiDanIzin kp : j.getKelompokParameterTambahanCutiDanIzins()) {
						kelompokParameterTambahanCutiDanIzins.add(kp);
					}

					parameterTambahanListener = new ParameterTambahanCutiDanIzinListener(cutiDanIzin,
							kelompokParameterTambahanCutiDanIzins, parameterRows, lampiranLains, rowsLampiran);
					parameterTambahanListener.onEvent(null);
				}
			}
		};

		EventListener eventListener = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Statusabsensi status = (Statusabsensi) (statusabsensi.getSelectedItem() == null ? null
						: statusabsensi.getSelectedItem().getValue());
				Common.insertComboDanSemua(jenisCutiDanIzin, new String[] { "nama", "kode" }, "keterangan",
						JenisCutiDanIzin.class, "Tanpa Form Pengajuan",
						Restrictions.and(status == null ? Restrictions.isNull("statusabsensi")
								: Restrictions.eq("statusabsensi", status), Restrictions.eq("aktif", true)));
				Common.selectComboItem(jenisCutiDanIzin, cutiDanIzin.getJenisCutiDanIzin());
				eventListenerJenisCutiDanIzin.onEvent(arg0);
			}
		};

		jenisCutiDanIzin.addEventListener("onChange", eventListenerJenisCutiDanIzin);
		statusabsensi.addEventListener("onChange", eventListener);

		try {
			Common.createDefaultTimer(eventListener);
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}

	public boolean onSave(Event event) throws Exception {
		if (pegawai.getAttribute("pegawai") == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, data Pegawai/karyawan belum dipilih. Kolom Pegawai/karyawan wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) mohon Bapak/Ibu memilih data pegawai pada kolom Pegawai; (2) pastikan pegawai telah terpilih; (3) kemudian tekan tombol Simpan kembali.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (statusabsensi.getSelectedItem() == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, Status Izin/cuti belum dipilih. Kolom Status Izin/cuti wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) mohon Bapak/Ibu memilih Status Izin/cuti yang sesuai; (2) pastikan pilihan telah ditentukan; (3) kemudian tekan tombol Simpan kembali.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (mulai.getValue() == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, tanggal Mulai berlaku belum diisi. Kolom Mulai berlaku wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) mohon Bapak/Ibu memilih tanggal Mulai berlaku; (2) pastikan tanggal telah ditentukan; (3) kemudian tekan tombol Simpan kembali.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (sampai.getValue() == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, tanggal Sampai berlaku belum diisi. Kolom Sampai berlaku wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) mohon Bapak/Ibu memilih tanggal Sampai berlaku; (2) pastikan tanggal telah ditentukan; (3) kemudian tekan tombol Simpan kembali.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (keterangan.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show(
					"Mohon maaf, kolom Keterangan belum diisi. Kolom Keterangan wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) mohon Bapak/Ibu mengisi kolom Keterangan; (2) pastikan kolom tersebut tidak dikosongkan; (3) kemudian tekan tombol Simpan kembali.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (parameterTambahanListener != null && !parameterTambahanListener.validate()) {
			return false;
		}

		Pegawai peg = (Pegawai) pegawai.getAttribute("pegawai");
		if (!peg.getAktif()) {
			MyMessageboxConfig.show(
					"Mohon maaf, data pegawai yang dipilih berstatus tidak aktif sehingga pengajuan tidak dapat diproses. Langkah yang dapat dilakukan: (1) mohon Bapak/Ibu memilih pegawai yang berstatus aktif; (2) periksa kembali status kepegawaian pada data pegawai; (3) hubungi bagian kepegawaian bila status pegawai perlu diperbarui.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Statusabsensi m = (Statusabsensi) statusabsensi.getSelectedItem().getValue();

		/*
		 * ATURAN CUTI PEGAWAI (semua gerbang DEFAULT NONAKTIF — lihat AturanCutiHelper).
		 * onSave() dipanggil pada DUA momen: saat pegawai MENGAJUKAN dan saat atasan MENYETUJUI
		 * (dibedakan oleh field `persetujuan`), sehingga tiap aturan dipasang pada momen yang tepat.
		 */
		try {
			String pelanggaran = null;

			if (!persetujuan) {
				// Aturan 2 — pengajuan paling lambat H-x sebelum tanggal mulai.
				pelanggaran = ais.action.master.payroll.helper.AturanCutiHelper
						.validasiBatasPengajuan(mulai.getValue());
			}

			if (pelanggaran == null && persetujuan) {
				// Aturan 3 — tidak disetujui bila berada di sekitar libur panjang (H-x s.d H+y).
				pelanggaran = ais.action.master.payroll.helper.AturanCutiHelper
						.validasiLiburPanjang(mulai.getValue(), sampai.getValue());
			}

			if (pelanggaran == null) {
				// Aturan 4 — cuti khusus tidak boleh melebihi durasi bakunya (hari kalender).
				int hariKalender = 0;
				try {
					hariKalender = Common.getBetweenTwoDates(mulai.getValue(), sampai.getValue()) + 1;
				} catch (Exception exHitung) {
					ais.common.ErrorAuditUtil.record(exHitung,
							"auto-audit src/ais/action/master/payroll/CutiDanIzinAction.java:hitungHariKalender");
				}
				if (hariKalender > 0) {
					pelanggaran = ais.action.master.payroll.helper.AturanCutiHelper.validasiDurasiBaku(m,
							hariKalender);
				}
			}

			if (pelanggaran != null) {
				MyMessageboxConfig.show(pelanggaran, "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		} catch (Exception exAturan) {
			// Aturan tambahan TIDAK boleh menggagalkan alur lama bila terjadi kesalahan tak terduga.
			ais.common.ErrorAuditUtil.record(exAturan,
					"auto-audit src/ais/action/master/payroll/CutiDanIzinAction.java:aturanCuti");
		}

		if (m != null && m.getMemotongJatahCuti() != null && m.getMemotongJatahCuti()) {
			int bln = peg.ambilMasaKerjaBulan(peg.getTanggalmasuk(), mulai.getValue());
			int thn = peg.ambilMasaKerjaTahun(peg.getTanggalmasuk(), mulai.getValue());
			int totalBln = (thn * 12) + bln;

			int faktor = m.getBolehDiambilMinimalBulanKerja() == null || m.getBolehDiambilMinimalBulanKerja() == 0 ? 0
					: (totalBln / m.getBolehDiambilMinimalBulanKerja());
			if (faktor == 0)
				faktor = 1;

			int maksimal = (m.getBolehDiambilMinimalBulanKerja() != null ? m.getBolehDiambilMinimalBulanKerja() * faktor
					: 0) + (m.getBolehDiambilMaksimalBulanKerja() != null ? m.getBolehDiambilMaksimalBulanKerja() : 0);
			int minimal = (m.getBolehDiambilMinimalBulanKerja() != null ? m.getBolehDiambilMinimalBulanKerja() * faktor
					: 0) + (m.getBolehDiambilSetelahBulanKerja() != null ? m.getBolehDiambilSetelahBulanKerja() : 0);

			if (minimal > 1 && minimal > totalBln) {
				MyMessageboxConfig.showFormat(
						"Mohon maaf, pengajuan \"{V1}\" hanya dapat dilakukan setelah masa kerja minimal {V2} tahun {V3} bulan, sedangkan masa kerja Bapak/Ibu sebagai pegawai tetap saat ini baru {V4} tahun {V5} bulan. Langkah yang dapat dilakukan: (1) mohon periksa kembali tanggal pengajuan; (2) ajukan kembali setelah masa kerja mencukupi; (3) hubungi bagian kepegawaian bila memerlukan penjelasan lebih lanjut.",
						"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, m.getNama(), (minimal / 12),
						(minimal % 12), (totalBln / 12), (totalBln % 12));
				return false;
			}

			if (maksimal > 1 && maksimal < totalBln) {
				MyMessageboxConfig.showFormat(
						"Mohon maaf, pengajuan \"{V1}\" hanya dapat dilakukan hingga masa kerja maksimal {V2} tahun {V3} bulan, sedangkan masa kerja Bapak/Ibu sebagai pegawai tetap saat ini telah mencapai {V4} tahun {V5} bulan. Langkah yang dapat dilakukan: (1) mohon periksa kembali tanggal pengajuan; (2) sesuaikan periode pengajuan dengan ketentuan yang berlaku; (3) hubungi bagian kepegawaian bila memerlukan penjelasan lebih lanjut.",
						"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, m.getNama(), (maksimal / 12),
						(maksimal % 12), (totalBln / 12), (totalBln % 12));
				return false;
			}

			int sisa = 12;
			try {
				sisa = Common.numberFormat.get().parse(sisaCuti.getValue()).intValue();
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

			int libur = Common.getWorkingDaysBetweenTwoDates(mulai.getValue(), sampai.getValue()) + 1;
			if (sisa < libur) {
				MyMessageboxConfig.showFormat(
						"Mohon maaf, sisa cuti tidak mencukupi untuk pengajuan ini. Sisa cuti Bapak/Ibu saat ini {V1} hari, sedangkan jumlah hari cuti yang diajukan adalah {V2} hari. Langkah yang dapat dilakukan: (1) mohon kurangi rentang tanggal cuti yang diajukan; (2) periksa kembali sisa cuti yang tersedia; (3) hubungi bagian kepegawaian bila memerlukan penjelasan lebih lanjut.",
						"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, sisa, libur);
				return false;
			}
		}

		Session session = null;
		try {
			session = HibernateUtil.currentSession();
			if (cutiDanIzin.getId() != null) {
				cutiDanIzin = (CutiDanIzin) session.load(CutiDanIzin.class, cutiDanIzin.getId());
				updateStatusAbsensi(cutiDanIzin, ConstantValues.BELUM_ABSEN);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/payroll/CutiDanIzinAction.java:1109");
			// Abaikan error lazy loading
		}

		cutiDanIzin.setPegawai((Pegawai) pegawai.getAttribute("pegawai"));
		cutiDanIzin.setMulai(mulai.getValue());
		cutiDanIzin.setSampai(sampai.getValue());
		cutiDanIzin.setStatusabsensi((Statusabsensi) statusabsensi.getSelectedItem().getValue());
		cutiDanIzin.setKeterangan(keterangan.getValue());
		cutiDanIzin.setJenisCutiDanIzin((JenisCutiDanIzin) (jenisCutiDanIzin.getSelectedItem() == null ? null
				: jenisCutiDanIzin.getSelectedItem().getValue()));

		// Model cutiDanIzin.setKecualiTanggals(...) sekarang otomatis ditangani di
		// dalam kalkulasiCuti()
		// Pastikan pemanggilan terakhir fungsi berjalan
		if (cutiDanIzin.getKecualiTanggals() == null)
			cutiDanIzin.setKecualiTanggals("");

		if (disposisiSop != null && disposisiSop.getId() != null)
			cutiDanIzin.setDisposisiSop(disposisiSop);

		try {
			cutiDanIzin
					.setJumlahCutiBersama(Common.numberFormat.get().parse(jumlahCutiBersama.getValue()).doubleValue());
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try {
			cutiDanIzin.setSisaCuti(Common.numberFormat.get().parse(sisaCuti.getValue()).doubleValue());
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try {
			cutiDanIzin.setJumlahCuti(Common.numberFormat.get().parse(jumlahCuti.getValue()).doubleValue());
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		if (parameterTambahanListener != null)
			parameterTambahanListener.onSave(cutiDanIzin);
		if (cutiDanIzin.getId() == null)
			cutiDanIzin.setDiajukanOleh(tbmuser);

		Common.refreshSaveOrUpdate(session, cutiDanIzin);

		if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
			Session streamSession = null;
			try {
				streamSession = StreamingHibernateUtil.getInstance().currentSession();
				streamSession.refresh(lainMahasiswa);
				lainMahasiswa.setRef(cutiDanIzin.getId());
				streamSession.getTransaction().begin();
				streamSession.update(lainMahasiswa);
				streamSession.getTransaction().commit();
			} catch (Exception e) {
				try {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
				} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/payroll/CutiDanIzinAction.java:1166");
				}
				Common.tampilErrorJikaAdmin(e);
			} finally {
				// Tutup session khusus stream session karena tidak otomatis ditarik dari flow
				// ZK thread filter
				try {
					StreamingHibernateUtil.getInstance().closeSession();
				} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/payroll/CutiDanIzinAction.java:1174");
				}
			}
		}

		updateStatusAbsensi(cutiDanIzin, cutiDanIzin.getStatusabsensi());
		return true;
	}

	private void updateStatusAbsensi(CutiDanIzin cutiData, Statusabsensi statsAbsensi) {
		if (cutiData == null || statsAbsensi == null)
			return;

		Session session = null;
		try {
			session = HibernateUtil.currentSession();
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(cutiData.getMulai());

			Calendar sampaiCal = ais.ui.util.WaktuUtil.getCalendar();
			sampaiCal.setTime(cutiData.getSampai());
			sampaiCal.add(Calendar.DATE, 1);

			long timeSampai = sampaiCal.getTimeInMillis();

			// Menggunakan instance kalender tanpa me-reset 'Calendar.getInstance()' terus
			// menerus
			while (calendar.getTimeInMillis() < timeSampai) {
				Date tanggal = calendar.getTime();
				final Integer bln = calendar.get(Calendar.MONTH) + 1;
				final Integer thn = calendar.get(Calendar.YEAR);
				final Integer tgl = calendar.get(Calendar.DATE);
				final Integer hari = calendar.get(Calendar.DAY_OF_WEEK);

				LiburNasional liburNasional = LiburNasional.ambilLiburNasional(tanggal);
				StatuskehadiranKaryawanHarian statHarian = (StatuskehadiranKaryawanHarian) session
						.createCriteria(StatuskehadiranKaryawanHarian.class).add(Restrictions.eq("tanggal", tanggal))
						.add(Restrictions.eq("pegawai", cutiData.getPegawai())).setMaxResults(1).uniqueResult();

				if (statHarian == null) {
					statHarian = new StatuskehadiranKaryawanHarian();
					statHarian.setBulan(bln);
					statHarian.setTahun(thn);
					statHarian.setTgl(tgl);
					statHarian.setPegawai(cutiData.getPegawai());
					statHarian.setKeterangan("");
					statHarian.setMasukjam(null);
					statHarian.setPulangJam(null);
					statHarian.setStatusabsensi(ConstantValues.BELUM_ABSEN);
					statHarian.setTanggal(tanggal);
					statHarian.setMinggu(hari);
					statHarian.setLiburNasional(liburNasional);
				}

				boolean isBelumAbsen = (statsAbsensi.getId() != null
						&& statsAbsensi.getId().equals(ConstantValues.BELUM_ABSEN.getId()));
				statHarian.setCutiDanIzin(isBelumAbsen ? null : cutiData);

				session.saveOrUpdate(statHarian);
				calendar.add(Calendar.DATE, 1);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/payroll/CutiDanIzinAction.java:1235");
			// Tangkap aman jika koneksi db putus di proses batch ini.
		}
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Tbmrole tbmrole = tbmuser == null ? null : tbmuser.hakAkses();
		List<JenisCutiDanIzin> jenisCutiDanIzins = new ArrayList<JenisCutiDanIzin>();

		for (Object o : ConstantValues.ambilBerdasarClass(JenisCutiDanIzin.class).values()) {
			try {
				JenisCutiDanIzin jCdi = (JenisCutiDanIzin) o;
				if ((tbmrole != null && jCdi.getJenisPengguna() != null
						&& jCdi.getJenisPengguna().toLowerCase()
								.contains("," + tbmrole.getRoleId().toLowerCase() + ","))
						|| (tbmuser != null && jCdi.getUsernamePengguna() != null && jCdi.getUsernamePengguna()
								.toLowerCase().contains("," + tbmuser.getUserId().toLowerCase() + ","))) {
					jenisCutiDanIzins.add(jCdi);
				}
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		}

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Criterion cc = satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
				: Restrictions.or(Restrictions.isNull("pegawai.satuanKerja"),
						Restrictions.or(
								parent == null ? Restrictions.isNull("pegawai.satuanKerja")
										: Restrictions.sqlRestriction("false"),
								Restrictions.in("pegawai.satuanKerja", satuanKerjas)));

		Criteria criteria = session.createCriteria(CutiDanIzin.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("setujui"), Restrictions.eq("setujui", false))
						: Restrictions.sqlRestriction("true"))
				.add((start == null || end == null || start.getValue() == null || end.getValue() == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.or(
						Restrictions.sqlRestriction("date(this_.mulai) between date('"
								+ Common.databaseDateFormat.get().format(start.getValue()) + "') and date('"
								+ Common.databaseDateFormat.get().format(end.getValue()) + "')"),
						Restrictions.sqlRestriction("date(this_.sampai) between date('"
								+ Common.databaseDateFormat.get().format(start.getValue()) + "') and date('"
								+ Common.databaseDateFormat.get().format(end.getValue()) + "')"))))
				.createAlias("pegawai", "pegawai", Criteria.LEFT_JOIN)
				.add(searchpegawai.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("pegawai.nama", searchpegawai.getValue().trim(), MatchMode.ANYWHERE));

		if (!jenis.trim().isEmpty()) {
			criteria.createAlias("statusabsensi", "statusabsensi")
					.add(Restrictions.ilike("statusabsensi.nama", jenis, MatchMode.ANYWHERE));
		}
		if (order) {
			criteria.addOrder(Order.desc("mulai")).addOrder(Order.desc("sampai"));
		}

		if (punyaBawahan != null && !punyaBawahan.isEmpty() && punyaBawahanDosen != null
				&& !punyaBawahanDosen.isEmpty()) {
			criteria.createAlias("diajukanOleh", "diajukanOleh", Criteria.LEFT_JOIN)
					.createAlias("pegawai.dosen", "dosen", Criteria.LEFT_JOIN).add(
							Restrictions.or(
									jenisCutiDanIzins.isEmpty() ? Restrictions.sqlRestriction("false")
											: Restrictions.and(cc,
													Restrictions.in("jenisCutiDanIzin", jenisCutiDanIzins)),
									Restrictions.or(
											Restrictions.or(Restrictions.in("diajukanOleh.pegawai.id", punyaBawahan),
													Restrictions.in("diajukanOleh.dosen.id", punyaBawahanDosen)),
											Restrictions.or(Restrictions.in("pegawai.id", punyaBawahan),
													Restrictions.in("dosen.id", punyaBawahanDosen)))));
			searchparent.setDisabled(true);
		} else if (punyaBawahan != null && !punyaBawahan.isEmpty()) {
			criteria.createAlias("diajukanOleh", "diajukanOleh",
					Criteria.LEFT_JOIN).add(
							Restrictions.or(
									jenisCutiDanIzins.isEmpty() ? Restrictions.sqlRestriction("false")
											: Restrictions.and(cc,
													Restrictions.in("jenisCutiDanIzin", jenisCutiDanIzins)),
									Restrictions.or(Restrictions.in("diajukanOleh.pegawai.id", punyaBawahan),
											Restrictions.in("pegawai.id", punyaBawahan))));
			searchparent.setDisabled(true);
		} else if (punyaBawahanDosen != null && !punyaBawahanDosen.isEmpty()) {
			criteria.createAlias("diajukanOleh", "diajukanOleh", Criteria.LEFT_JOIN)
					.createAlias("pegawai.dosen", "dosen", Criteria.LEFT_JOIN).add(
							Restrictions.or(
									jenisCutiDanIzins.isEmpty() ? Restrictions.sqlRestriction("false")
											: Restrictions.and(cc,
													Restrictions.in("jenisCutiDanIzin", jenisCutiDanIzins)),
									Restrictions.or(Restrictions.in("diajukanOleh.dosen.id", punyaBawahanDosen),
											Restrictions.in("dosen.id", punyaBawahanDosen))));
			searchparent.setDisabled(true);
		} else {
			Criterion c = Restrictions.sqlRestriction("false");
			if (!admin) {
				if (tbmuser != null && tbmuser.getDosen() != null && tbmuser.getPegawai() != null) {
					punyaBawahan = new HashSet<Long>();
					punyaBawahanDosen = new HashSet<Long>();
					punyaBawahan.add(tbmuser.getPegawai().getId());
					punyaBawahanDosen.add(tbmuser.getDosen().getId());
					c = Restrictions.or(
							jenisCutiDanIzins.isEmpty() ? Restrictions.sqlRestriction("false")
									: Restrictions.and(cc, Restrictions.in("jenisCutiDanIzin", jenisCutiDanIzins)),
							Restrictions.or(
									Restrictions.or(Restrictions.in("diajukanOleh.pegawai.id", punyaBawahan),
											Restrictions.in("diajukanOleh.dosen.id", punyaBawahanDosen)),
									Restrictions.or(Restrictions.in("pegawai.id", punyaBawahan),
											Restrictions.in("dosen.id", punyaBawahanDosen))));
				} else if (tbmuser != null && tbmuser.getPegawai() != null) {
					punyaBawahan = new HashSet<Long>();
					punyaBawahanDosen = new HashSet<Long>();
					punyaBawahan.add(tbmuser.getPegawai().getId());
					punyaBawahanDosen.add(-1L);
					c = Restrictions.or(
							jenisCutiDanIzins.isEmpty() ? Restrictions.sqlRestriction("false")
									: Restrictions.and(cc, Restrictions.in("jenisCutiDanIzin", jenisCutiDanIzins)),
							Restrictions.or(
									Restrictions.or(Restrictions.in("diajukanOleh.pegawai.id", punyaBawahan),
											Restrictions.in("diajukanOleh.dosen.id", punyaBawahanDosen)),
									Restrictions.or(Restrictions.in("pegawai.id", punyaBawahan),
											Restrictions.in("dosen.id", punyaBawahanDosen))));
				} else if (tbmuser != null && tbmuser.getDosen() != null) {
					punyaBawahan = new HashSet<Long>();
					punyaBawahanDosen = new HashSet<Long>();
					punyaBawahan.add(-1L);
					punyaBawahanDosen.add(tbmuser.getDosen().getId());
					c = Restrictions.or(
							jenisCutiDanIzins.isEmpty() ? Restrictions.sqlRestriction("false")
									: Restrictions.and(cc, Restrictions.in("jenisCutiDanIzin", jenisCutiDanIzins)),
							Restrictions.or(
									Restrictions.or(Restrictions.in("diajukanOleh.pegawai.id", punyaBawahan),
											Restrictions.in("diajukanOleh.dosen.id", punyaBawahanDosen)),
									Restrictions.or(Restrictions.in("pegawai.id", punyaBawahan),
											Restrictions.in("dosen.id", punyaBawahanDosen))));
				}
			}

			criteria.createAlias("diajukanOleh", "diajukanOleh", Criteria.LEFT_JOIN)
					.createAlias("pegawai.dosen", "dosen", Criteria.LEFT_JOIN)
					.add(satuanKerjas.size() == 0 ? (admin ? Restrictions.sqlRestriction("1=1") : c)
							: Restrictions.or(c, Restrictions.or(Restrictions.isNull("pegawai.satuanKerja"),
									Restrictions.in("pegawai.satuanKerja", satuanKerjas))));
		}

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);
		List<CutiDanIzin> cdiList = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(cdiList);
		grid.setRowRenderer(new CutiDanIzinRenderer());
		grid.setModelCheckMobile(strset);
		grid.renderAll();
	}

	@Override
	public String istilah() throws Exception {
		return "Pengajuan Cuti Dan Izin Pegawai";
	}

	@Override
	public DataSop ambil() throws Exception {
		return cutiDanIzin;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClass() throws Exception {
		return CutiDanIzin.class;
	}

	@Override
	public void setPersetujuan(boolean persetujuan) {
		this.persetujuan = persetujuan;
	}

	@Override
	public File cetakData(GeneralValueObject generalValueObject) throws Exception {
		return null;
	}

	private void rebuildCheckboxes() {
		Common.clear(containerPengecualian);
		if (mulai.getValue() == null || sampai.getValue() == null) {
			return;
		}

		Calendar calStart = Calendar.getInstance();
		calStart.setTime(mulai.getValue());
		Calendar calEnd = Calendar.getInstance();
		calEnd.setTime(sampai.getValue());

		// Menggunakan HashSet untuk optimasi pengecekan contain string JSON dengan
		// kompleksitas O(1)
		Set<String> existingKecuali = new HashSet<String>();
		try {
			if (cutiDanIzin.getKecualiTanggals() != null && !cutiDanIzin.getKecualiTanggals().trim().isEmpty()) {
				org.json.JSONArray exArr = new org.json.JSONArray(cutiDanIzin.getKecualiTanggals());
				for (int i = 0; i < exArr.length(); i++) {
					existingKecuali.add(exArr.getString(i));
				}
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		List<Checkbox> listCheckboxes = new ArrayList<Checkbox>();
		long endTime = calEnd.getTimeInMillis();

		while (calStart.getTimeInMillis() <= endTime) {
			Date tgl = calStart.getTime();
			String tglStr = Common.dateFormat4.get().format(tgl);

			final Checkbox cb = new Checkbox(tglStr);
			cb.setAttribute("tanggal", tgl);

			if (existingKecuali.contains(tglStr)) {
				cb.setChecked(true);
			}

			cb.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					kalkulasiCuti(true);
				}
			});

			listCheckboxes.add(cb);
			containerPengecualian.appendChild(cb);

			calStart.add(Calendar.DATE, 1);
		}

		containerPengecualian.setAttribute("listCheckboxes", listCheckboxes);
	}

	@SuppressWarnings("unchecked")
	private void kalkulasiCuti(boolean debug) throws Exception {
		Pegawai peg = (Pegawai) pegawai.getAttribute("pegawai");
		if (peg == null)
			return;

		if (!peg.getAktif()) {
			// Messagebox.show("Data pegawai ini tidak aktif", "Peringatan", Messagebox.OK, Messagebox.EXCLAMATION);
			return;
		}

		Statusabsensi m = (Statusabsensi) (statusabsensi.getSelectedItem() == null ? null
				: statusabsensi.getSelectedItem().getValue());
		if (m == null) {
			// Messagebox.show("Status Izin harus diisi", "Peringatan", Messagebox.OK, Messagebox.EXCLAMATION);
			return;
		}

		if (mulai.getValue() != null && sampai.getValue() != null) {
			int libur = m.getHariLiburDihitung() != null && m.getHariLiburDihitung()
					? Common.getWorkingDaysBetweenTwoDates(mulai.getValue(), sampai.getValue(), peg) + 1
					: Common.getBetweenTwoDates(mulai.getValue(), sampai.getValue()) + 1;

			List<Checkbox> cbs = (List<Checkbox>) containerPengecualian.getAttribute("listCheckboxes");

			// Variabel lokal JSONArray
			JSONArray localArr = new JSONArray();
			int potong = 0;

			if (cbs != null) {
				for (Checkbox cb : cbs) {
					if (cb.isChecked()) {
						localArr.put(cb.getLabel());

						Date tglCb = (Date) cb.getAttribute("tanggal");
						boolean dihitungOlehSystem = true;

						if (m.getHariLiburDihitung() != null && m.getHariLiburDihitung()) {
							if (Common.isHolidayMerahDanAtauHariLibur(tglCb, peg)) {
								dihitungOlehSystem = false;
							}
						}

						if (dihitungOlehSystem)
							potong++;
					}
				}
			}

			// Simpan langsung state terbaru ke CutiDanIzin model.
			cutiDanIzin.setKecualiTanggals(localArr.toString());
			
			if (debug) {
				System.out.println("localArr -> "+localArr);
			}

			libur = libur - potong;
			if (libur < 0)
				libur = 0;
			jumlahHariCuti.setValue(Common.numberFormat.get().format(libur));

			int blnPeg = peg.ambilMasaKerjaBulan(peg.getTanggalmasuk(), mulai.getValue());
			int thnPeg = peg.ambilMasaKerjaTahun(peg.getTanggalmasuk(), mulai.getValue());

			int totalBln = (thnPeg * 12) + blnPeg;
			int faktor = m.getBolehDiambilMinimalBulanKerja() == null || m.getBolehDiambilMinimalBulanKerja() == 0 ? 0
					: (totalBln / m.getBolehDiambilMinimalBulanKerja());
			if (faktor == 0)
				faktor = 1;

			int minimal = (m.getBolehDiambilMinimalBulanKerja() != null ? m.getBolehDiambilMinimalBulanKerja() * faktor
					: 0) + (m.getBolehDiambilSetelahBulanKerja() != null ? m.getBolehDiambilSetelahBulanKerja() : 0);
			masaKerjaCuti.setValue((minimal / 12) + " tahun " + (minimal % 12) + " bulan");
			masaKerjaCuti.getParent().setVisible(
					m.getBolehDiambilMinimalBulanKerja() != null && m.getBolehDiambilMinimalBulanKerja() > 0);

			int maksimal = (m.getBolehDiambilMinimalBulanKerja() != null ? m.getBolehDiambilMinimalBulanKerja() * faktor
					: 0) + (m.getBolehDiambilMaksimalBulanKerja() != null ? m.getBolehDiambilMaksimalBulanKerja() : 0);
			masaKerjaCutiMax.setValue((maksimal / 12) + " tahun " + (maksimal % 12) + " bulan");
			masaKerjaCutiMax.getParent().setVisible(
					m.getBolehDiambilMaksimalBulanKerja() != null && m.getBolehDiambilMaksimalBulanKerja() > 0);

			Session session = null;
			Transaction tx = null;
			int jmlCuti = 0;
			CutiBersama cutiBersama = null;

			try {
				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.setTime(mulai.getValue());
				int selectedtahun = calendar.get(Calendar.YEAR);

				// Buka session secara aman
				session = HibernateUtil.getSessionFactory().openSession();
				tx = session.beginTransaction();

				// Skenario BARU: Jika id tidak null, langsung lakukan update setelah perubahan data di atas
				if (cutiDanIzin.getId() != null) {
					session.update(cutiDanIzin);
				}

				cutiBersama = (CutiBersama) session.createCriteria(CutiBersama.class)
						.add(Restrictions.eq("tahun", selectedtahun)).setMaxResults(1).uniqueResult();
				if (cutiBersama == null)
					cutiBersama = new CutiBersama();

				Calendar mul = Calendar.getInstance();
				mul.set(Calendar.YEAR, selectedtahun);
				mul.set(Calendar.MONTH, 0);
				mul.set(Calendar.DATE, 1);

				Calendar smp = Calendar.getInstance();
				smp.set(Calendar.YEAR, selectedtahun);
				smp.set(Calendar.MONTH, 11);
				smp.set(Calendar.DATE, 31);

				List<CutiDanIzin> cutiDanIzinsSemua = session.createCriteria(CutiDanIzin.class)
						.add(Restrictions.or(Restrictions.between("mulai", mul.getTime(), smp.getTime()),
								Restrictions.between("sampai", mul.getTime(), smp.getTime())))
						.addOrder(Order.asc("mulai")).add(Restrictions.eq("pegawai", peg))
						.add(Restrictions.eq("setujui", true)).list();

				for (CutiDanIzin cdi : cutiDanIzinsSemua) {
					if (cdi.getMemotongJatahCuti() != null && cdi.getMemotongJatahCuti()) {
						long timeMulaiVal = cdi.getMulai().getTime();
						long timeSampaiVal = cdi.getSampai().getTime();

						// Reuse temp cal
						Calendar tempCal = Calendar.getInstance();
						tempCal.setTimeInMillis(timeMulaiVal);

						while (tempCal.getTimeInMillis() <= timeSampaiVal) {
							if (Common.isHolidayMerahDanAtauHariLibur(tempCal.getTime(), peg)) {
								tempCal.add(Calendar.DATE, 1);
								continue;
							}
							if (tempCal.get(Calendar.YEAR) == selectedtahun) {
								jmlCuti++;
							}
							tempCal.add(Calendar.DATE, 1);
						}
					}
				}

				tx.commit();
			} catch (Exception e) {
				if (tx != null && tx.isActive()) {
					tx.rollback();
				}
				if (debug) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
			} finally {
				// Penutupan session wajib untuk mencegah memory leak dan DB lock
				if (session != null) {
					session.clear();
					session.disconnect();
					session.close();
				}
			}

			Common.clear(masaKerja);
			boolean isMemotong = m.getMemotongJatahCuti() != null ? m.getMemotongJatahCuti() : false;
			jumlahCuti.getParent().setVisible(isMemotong);
			jumlahCutiBersama.getParent().setVisible(isMemotong);
			sisaCuti.getParent().setVisible(isMemotong);
			jumlahCutiDiambil.getParent().setVisible(isMemotong);

			jumlahCutiDiambil.setValue(Common.numberFormat.get().format(jmlCuti));

			int jatahAwal = (peg.getJatahCutiTahunan() == null)
					? (cutiBersama != null && cutiBersama.getJumlahCuti() != null ? cutiBersama.getJumlahCuti() : 0)
					: peg.getJatahCutiTahunan();
			int cutiBersamaJml = (cutiBersama != null && cutiBersama.getJumlahCutiBersama() != null)
					? cutiBersama.getJumlahCutiBersama()
					: 0;
			int jumlahCutiYangBisaDiambil = ((jatahAwal - cutiBersamaJml) - jmlCuti);

			CutiDanIzinAction.this.jumlahCuti.setValue(Common.numberFormat.get().format(jatahAwal));
			CutiDanIzinAction.this.jumlahCutiBersama.setValue(Common.numberFormat.get().format(cutiBersamaJml));
			CutiDanIzinAction.this.sisaCuti.setValue(Common.numberFormat.get().format(jumlahCutiYangBisaDiambil));

			if (peg.getTanggalMulaiPengalanKerja() != null) {
				new Label("Pengalaman Kerja : "
						+ peg.ambilMasaKerjaTahunPengalamanKerja(peg.getTanggalMulaiPengalanKerja(), mulai.getValue())
						+ " thn, "
						+ peg.ambilMasaKerjaBulanPengalamanKerja(peg.getTanggalMulaiPengalanKerja(), mulai.getValue())
						+ " bln").setParent(masaKerja);
			}
			if (peg.getTanggalmasukHonorer() != null) {
				new Label("Honor : " + peg.ambilMasaKerjaTahunHonorer(peg.getTanggalmasukHonorer(), mulai.getValue())
						+ " thn, " + peg.ambilMasaKerjaBulanHonorer(peg.getTanggalmasukHonorer(), mulai.getValue())
						+ " bln").setParent(masaKerja);
			}
			if (peg.getTanggalmasukSemiTetap() != null) {
				new Label("Semi Tetap : "
						+ peg.ambilMasaKerjaTahunSemiTetap(peg.getTanggalmasukSemiTetap(), mulai.getValue()) + " thn, "
						+ peg.ambilMasaKerjaBulanSemiTetap(peg.getTanggalmasukSemiTetap(), mulai.getValue()) + " bln")
						.setParent(masaKerja);
			}
			if (peg.getTanggalmasuk() != null) {
				new Label("Tetap : " + peg.ambilMasaKerjaTahun(peg.getTanggalmasuk(), mulai.getValue()) + " thn, "
						+ peg.ambilMasaKerjaBulan(peg.getTanggalmasuk(), mulai.getValue()) + " bln")
						.setParent(masaKerja);
			}
		}
	}
}