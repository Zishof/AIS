package ais.action.master;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
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
import org.zkoss.zul.Vbox;

import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.helper.ParameterTambahanPengajuanPegawaiListener;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.action.report.Report;
import ais.action.report.format1.akademik.LaporanPengajuanPegawai;
import ais.action.report.helper.CommonReport;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.UIClassHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.JenisPengajuanPegawai;
import ais.database.model.KelompokParameterTambahanPengajuanPegawai;
import ais.database.model.ParameterTambahan;
import ais.database.model.ParameterTambahanPengajuanPegawai;
import ais.database.model.Pegawai;
import ais.database.model.PengajuanPegawai;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.Pejabat;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.database.model.surat.NomorSurat;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.FormSop;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelKecilBold;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

public class PengajuanPegawaiAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault, FormSop {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchkode;
	private Textbox searchnama;
	private Textbox searchpegawai;
	private AmbilDataSatuanKerjaBanbox searchparent;

	private MyDatebox start;
	private MyDatebox end;

	private boolean edit = false;
	private boolean delete = false;

	private PengajuanPegawai pengajuanPegawai;
	private MyToolbarbuttonConfig add;
	private MyDatebox waktu;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private Combobox jenisPengajuanPegawai;

	private Tabpanel tabJenisPengajuanPegawai;
	private Tabpanel tabManajemenParameter;

	private Tabpanel tabLaporan;

	private ArrayList<Row> parameterRows;
	private HashMap<String, LampiranLain> lampiranLains;
	private ParameterTambahanPengajuanPegawaiListener parameterTambahanListener;
	private MyTextbox nama;
	private Textbox keterangan;
	private DisposisiSop disposisiSop;
	private Label kode;
	private AmbilDataPegawaiBanbox pegawai;
	private Tbmuser tbmuser;

	private Checkbox searchaktif;

	private List<Long> punyaBawahan = null;
	private List<Long> punyaBawahanDosen = null;

	public void onLaporan(Event event) {
		if (tabLaporan.getChildren().size() == 0) {
			LaporanPengajuanPegawai window = new LaporanPengajuanPegawai(jenis);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabLaporan);
		}
	}

	public void onJenisPengajuanPegawai(Event event) {
		if (tabJenisPengajuanPegawai.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabJenisPengajuanPegawai);
			MyInclude iframe = new MyInclude("/pages/master/jenis_pengajuan_pegawai.zul");
			iframe.setParent(window);
		}
	}

	public void onManajemenParameter(Event event) {
		if (tabManajemenParameter.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabManajemenParameter);
			MyInclude iframe = new MyInclude("/pages/master/parameter_tambahan_pengajuan_pegawai.zul");
			iframe.setParent(window);
		}
	}

	private JenisPengajuanPegawai jenis = null;
	private AmbilDataSatuanKerjaBanbox satuanKerjaPengaju;
	private MyDatebox waktuSampai;
	private JSONObject keteranganBanyak;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	@SuppressWarnings("unchecked")
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();

		jenis = execution.getParameter("jenis") == null ? null
				: (JenisPengajuanPegawai) ConstantValues.ambil(JenisPengajuanPegawai.class.getName(),
						Long.parseLong(execution.getParameter("jenis")));

		if (!Common.getApakahAdmin()) {
			tabManajemenParameter.setVisible(false);
			tabManajemenParameter.getLinkedTab().setVisible(false);

			tabJenisPengajuanPegawai.setVisible(false);
			tabJenisPengajuanPegawai.getLinkedTab().setVisible(false);
		}

		tbmuser = Common.getCurrentUser();

		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		if (add != null) {
			add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
			add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		if (start != null && end != null) {
			if (start != null) start.setReadonly(true);
			if (end != null) end.setReadonly(true);
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 6);
			if (start != null) start.setValue(calendar.getTime());
			calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
			if (end != null) end.setValue(calendar.getTime());
		}

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		String[] contents = new String[] { "id", "pegawai", "kode", "nama", "waktu", "satuanKerja",
				"jenisPengajuanPegawai", "parameterTambahan", "parameterTambahanInds" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(PengajuanPegawai.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, PengajuanPegawai.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		if (tbmuser != null && tbmuser.getPegawai() != null) {
			try {
			Session session = HibernateUtil.currentSession();
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

			punyaBawahan = criteria.add(criterion).setProjection(Projections.property("id")).list();

			punyaBawahan.add(tbmuser.getPegawai().getId());
			System.out.println("punyaBawahan -> " + punyaBawahan);

			if (tbmuser.hakAkses() != null && tbmuser.hakAkses().getJenisJabatan() != null) {
				List<Long> bawahanJabatan = session.createCriteria(Pegawai.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.or(Restrictions.eq("atasan.id", tbmuser.hakAkses().getJenisJabatan().getId()),
								Restrictions.or(
										Restrictions.eq("atasanPendukung.id",
												tbmuser.hakAkses().getJenisJabatan().getId()),
										Restrictions.eq("atasanPendukungCadangan.id",
												tbmuser.hakAkses().getJenisJabatan().getId()))))
						.setProjection(Projections.property("id")).list();
				System.out.println("bawahanJabatan -> " + bawahanJabatan);
				if (!bawahanJabatan.isEmpty()) {
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
					List<Long> pejabats = session.createCriteria(Pejabat.class)

							.setProjection(Projections.groupProperty("jenisJabatan.id"))

							.add(Restrictions.or(
									Restrictions.or(
											Restrictions.ilike("jenisPengguna",
													"," + tbmuser.hakAkses().getRoleId() + ",", MatchMode.ANYWHERE),
											Restrictions.ilike("usernamePengguna", "," + tbmuser.getUserId() + ",",
													MatchMode.ANYWHERE)),
									Restrictions.and(
											Restrictions.or(Restrictions.isNotNull("pegawai"),
													Restrictions.or(Restrictions.isNotNull("guru"),
															Restrictions.isNotNull("dosen"))),
											Restrictions.or(Restrictions.eq("pegawai", tbmuser.getPegawai()),
													Restrictions.or(Restrictions.eq("dosen", tbmuser.getDosen()),
															Restrictions.eq("guru", tbmuser.getGuru()))))))

							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();

					System.out.println("pejabats -> " + pejabats);
					if (!pejabats.isEmpty()) {
						List<Long> bawahanJabatan = session.createCriteria(Pegawai.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(Restrictions.or(Restrictions.in("atasan.id", pejabats),
										Restrictions.or(Restrictions.in("atasanPendukung.id", pejabats),
												Restrictions.in("atasanPendukungCadangan.id", pejabats))))
								.setProjection(Projections.property("id")).list();
						System.out.println("bawahanJabatan -> " + bawahanJabatan);
						if (!bawahanJabatan.isEmpty()) {
							punyaBawahan.addAll(bawahanJabatan);
						}
					}
				}
			}
			if (tbmuser != null && tbmuser.ambilDosen() != null
					&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")) {
				punyaBawahanDosen = session.createCriteria(Pegawai.class).createAlias("dosen", "dosen")
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.or(Restrictions.eq("atasanlangsung", tbmuser.getPegawai()),
								Restrictions.or(Restrictions.eq("atasanlangsung3", tbmuser.getPegawai()),
										Restrictions.eq("atasanlangsung2", tbmuser.getPegawai()))))
						.setProjection(Projections.groupProperty("dosen.id")).list();

				punyaBawahanDosen.add(tbmuser.getDosen().getId());
				System.out.println("punyaBawahanDosen -> " + punyaBawahanDosen);
			}
			} catch (Exception eAkses) {
				eAkses.printStackTrace(); ais.common.ErrorAuditUtil.record(eAkses, "auto-audit src/ais/action/master/PengajuanPegawaiAction.java:361");
			}
		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	class PengajuanPegawaiRenderer extends ais.ui.util.MyRowRenderer {

		@SuppressWarnings("unchecked")
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PengajuanPegawai pengajuanPegawai = (PengajuanPegawai) arg1;

			Pegawai pegawai = pengajuanPegawai.getPegawai();
			if (pegawai != null) {
				Hbox hbox = new Hbox();
				hbox.setParent(arg0);
				CommonMedia.tampilkanGambarKecil(pegawai).setParent(hbox);

				Vbox vbox = new Vbox();
				vbox.setParent(hbox);
				new Label(pegawai.getMycode() == null ? "" : pegawai.getMycode()).setParent(vbox);

				Vbox vbox1 = RevisiHelper.createNewRevisi(Pegawai.class, pegawai,
						pegawai.getDosen() == null ? pegawai.getNama() : pegawai.getDosen().getNama());
				vbox1.setParent(vbox);

				if ((pengajuanPegawai.getKode() == null || pengajuanPegawai.getKode().isEmpty())
						&& pengajuanPegawai.getJenisPengajuanPegawai() != null) {
					String noAgenda = generateCode(pengajuanPegawai.getJenisPengajuanPegawai(), true);
					pengajuanPegawai.setKode(noAgenda);
					Long currentIndex = getindex(pengajuanPegawai.getJenisPengajuanPegawai());
					pengajuanPegawai.setIndex(++currentIndex);
					Common.refreshUpdate(pengajuanPegawai);
				}

				Vbox a;
				(a = RevisiHelper.createNewRevisi(PengajuanPegawai.class, pengajuanPegawai,
						Common.dateFormat5.get().format(pengajuanPegawai.getWaktu()))).setParent(arg0);
				a.appendChild(new Label(pengajuanPegawai.getKode()));

				if (pengajuanPegawai.getDiajukanOleh() != null) {
					new Label("Diajukan oleh : " + pengajuanPegawai.getDiajukanOleh().getUserNama()).setParent(a);
				}

				if (pengajuanPegawai.getDisetujuiOleh() != null) {
					new Label("Disetujui oleh : " + pengajuanPegawai.getDisetujuiOleh().getUserNama()).setParent(a);
				}
				if (pengajuanPegawai.getSetujuiTanggal() != null) {
					new Label("Disetujui tgl : " + Common.dateFormat1.get().format(pengajuanPegawai.getSetujuiTanggal()))
							.setParent(a);
				}

				vbox = new Vbox();
				vbox.setParent(arg0);
				new Label(pengajuanPegawai.getJenisPengajuanPegawai() == null ? ""
						: pengajuanPegawai.getJenisPengajuanPegawai().getNama()).setParent(vbox);

				new Label(pengajuanPegawai.getSatuanKerjaPengaju() == null ? ""
						: pengajuanPegawai.getSatuanKerjaPengaju().getNama()).setParent(vbox);

				JenisPengajuanPegawai j = pengajuanPegawai.getJenisPengajuanPegawai();
				Session session = HibernateUtil.currentSession();
				session.refresh(j);

				Vbox vbox2 = new Vbox();
				vbox2.setParent(arg0);

				for (KelompokParameterTambahanPengajuanPegawai kelompokParameterTambahanPengajuanPegawai : j
						.getKelompokParameterTambahanPengajuanPegawais()) {

					List<ParameterTambahan> parameterTambahans = ConstantValues.simpleList(
							session.createCriteria(ParameterTambahanPengajuanPegawai.class)
									.add(Restrictions.eq("kelompokParameterTambahanPengajuanPegawai",
											kelompokParameterTambahanPengajuanPegawai))
									.createAlias("parameterTambahan", "parameterTambahan")
									.createAlias("kelompokParameterTambahanPengajuanPegawai",
											"kelompokParameterTambahanPengajuanPegawai")
									.add(Restrictions.eq("parameterTambahan.aktif", true))
									.add(Restrictions.eq("kelompokParameterTambahanPengajuanPegawai.aktif", true))
									.setProjection(Projections.groupProperty("parameterTambahan.id")),
							ParameterTambahan.class, false);
					Collections.sort(parameterTambahans);

					for (ParameterTambahan parameterTambahan : parameterTambahans) {
						String jenis = kelompokParameterTambahanPengajuanPegawai.getId() + "->"
								+ parameterTambahan.getId();

						String val = "";
						String[] spl = pengajuanPegawai.getParameterTambahanInds().split("\n");
						for (String d : spl) {
							String[] value = d.split("<=>");
							if (value[0].trim().equalsIgnoreCase(jenis)) {
								val = value.length > 1 ? value[1].trim() : "";
							}
						}
						vbox2.appendChild(new MyLabelKecilBold(parameterTambahan.getLabelInputan()));
						LampiranLain lampiranLain = LampiranLain.ambil(pengajuanPegawai.getId(), jenis);
						ParameterTambahan.tampil(vbox2, parameterTambahan, lampiranLain, val);
					}

				}

				if (pengajuanPegawai.getDisposisiSop() != null) {
					A aa;
					(aa = new A()).setParent(vbox2);
					aa.setStyle("font-size:9px;");
					UIClassHelper.applyReadMore(aa, "SOP " + pengajuanPegawai.getDisposisiSop().getKeterangan() + " ("
							+ pengajuanPegawai.getDisposisiSop().getSop().getNama() + ")");
					aa.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							TampilanAlurSopAction.prosess(pengajuanPegawai.getDisposisiSop().getId(), null, null, true,
									arg0.getTarget());
						}
					});
				}

				if (tbmuser != null && pengajuanPegawai.getDisposisiSop() == null && tbmuser.getPegawai() != null
						&& pengajuanPegawai.getPegawai() != null
						&& !pengajuanPegawai.getPegawai().getId().equals(tbmuser.getPegawai().getId()) && (

						(punyaBawahan != null && punyaBawahan.contains(pengajuanPegawai.getPegawai().getId())) ||

								(pengajuanPegawai.getPegawai().getAtasanlangsung() != null && tbmuser.getPegawai()
										.getId().equals(pengajuanPegawai.getPegawai().getAtasanlangsung().getId()))

								||

								(pengajuanPegawai.getPegawai().getAtasanlangsung2() != null && tbmuser.getPegawai()
										.getId().equals(pengajuanPegawai.getPegawai().getAtasanlangsung2().getId()))

								||

								(pengajuanPegawai.getPegawai().getAtasanlangsung3() != null && tbmuser.getPegawai()
										.getId().equals(pengajuanPegawai.getPegawai().getAtasanlangsung3().getId()))

						)

				) {

					final MyCheckboxConfig checkbox = new MyCheckboxConfig("Setujui");
					checkbox.setChecked(pengajuanPegawai.getSetujui());
					checkbox.setParent(arg0);
					arg0.setAttribute("checkbox", checkbox);
					checkbox.addEventListener("onCheck", new EventListener() {

						@Override
						public void onEvent(Event a) throws Exception {
							pengajuanPegawai.setSetujui(checkbox.isChecked());

							if (checkbox.isChecked()) {
								pengajuanPegawai.setDisetujuiOleh(tbmuser);
								pengajuanPegawai.setSetujuiTanggal(WaktuUtil.getDate());
							} else {
								pengajuanPegawai.setDisetujuiOleh(null);
								pengajuanPegawai.setSetujuiTanggal(null);
							}

							Common.refreshSaveOrUpdate(pengajuanPegawai);

							// (B.2) Saat disetujui: beri tahu pengaju hasilnya. (B.3/B.4/B.5) Bila
							// jenis berupa lembur/masuk hari libur/dinas luar, beri tahu pelaksana
							// beserta arahan tugasnya.
							if (checkbox.isChecked()) {
								final PengajuanPegawai pp = pengajuanPegawai;
								Common.createDefaultTimer(new EventListener() {
									@Override
									public void onEvent(Event ev) throws Exception {
										try {
											java.util.List<Tbmuser> pengajuUsers = pp.getPegawai() == null ? null
													: resolveUsersDariPegawai(pp.getPegawai());
											Tbmuser pengaju = (pengajuUsers != null && !pengajuUsers.isEmpty())
													? pengajuUsers.get(0)
													: null;
											String jenisN = pp.getJenisPengajuanPegawai() == null
													? "Pengajuan Kepegawaian"
													: pp.getJenisPengajuanPegawai().getNama();
											String periode = periodePengajuan(pp);
											ais.common.CommonNotifikasi.pengajuanPegawaiHasil(pengaju, jenisN, true,
													periode, "", pp, HAL_ZK_PENGAJUAN_PEGAWAI, null);
											String low = jenisN.toLowerCase();
											if (low.contains("lembur") || low.contains("libur")
													|| low.contains("dinas")) {
												ais.common.CommonNotifikasi.penugasanPegawai(pengajuUsers, jenisN,
														periode, pp.getKeterangan(), pp, HAL_ZK_PENGAJUAN_PEGAWAI,
														null);
											}
										} catch (Exception e) {
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/PengajuanPegawaiAction.java:560");
										}
									}
								});
							}

							Common.clear(arg0);
							render(arg0, pengajuanPegawai);
						}
					});

				} else {
					new Label(pengajuanPegawai.getSetujui() ? "Ya" : "Tidak").setParent(arg0);
				}

				// kebab popup (⋯) via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten.
				final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
						new java.util.ArrayList<org.zkoss.zk.ui.Component>();

				Hbox d = Common.copyEditDeleteButtons(edit, delete, pengajuanPegawai, PengajuanPegawaiAction.this);
				aksiButtons.addAll(ais.ui.util.UIHelper.ambilItemAksi(d));

				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
				button.setStyle("font-size:9px;");
				button.setOrient("vertical");
				button.addEventListener("onClick", new EventListener() {
					@SuppressWarnings({})
					@Override
					public void onEvent(Event event) throws Exception {
						onKHS(pengajuanPegawai);
					}

				});
				aksiButtons.add(button);

				ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);
			}
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new PengajuanPegawai());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		pengajuanPegawai = (PengajuanPegawai) obj;
		init(pengajuanPegawai);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings("deprecation")
	@Override
	public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop disposisiSop,
			final MyToolbarbuttonConfig save, EventListener setujui) throws Exception {
		this.pengajuanPegawai = (PengajuanPegawai) generalValueObject;
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null))
				? this.disposisiSop
				: disposisiSop;

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		if (pengajuanPegawai.getPegawai() == null && tbmuser != null && tbmuser.getPegawai() != null) {
			pengajuanPegawai.setPegawai(tbmuser.getPegawai());
		}

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pegawai (*)"));
		row.appendChild(pegawai = new AmbilDataPegawaiBanbox());
		pegawai.setAttribute("myValue", pengajuanPegawai.getPegawai());
		pegawai.setAttribute("pegawai", pengajuanPegawai.getPegawai());
		pegawai.setValue(pengajuanPegawai.getPegawai() == null ? "" : pengajuanPegawai.getPegawai().getNama());
		pegawai.setWidth("90%");

		Tbmuser tbmuser = Common.getCurrentUser();

		Pegawai pegawaiTerpilih = tbmuser == null ? null : tbmuser.ambilPegawai();
		if (pegawaiTerpilih != null) {
			pegawai.setAttribute("myValue", pegawaiTerpilih);
			pegawai.setAttribute("pegawai", pegawaiTerpilih);
			pegawai.setValue(pegawaiTerpilih.getNama());
			pegawai.setDisabled(true);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No. Agenda"));
		row.appendChild(kode = new Label(pengajuanPegawai.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul Pengajuan *"));
		row.appendChild(nama = new MyTextbox(pengajuanPegawai.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal dan Waktu *"));

		if (pengajuanPegawai.getId() == null) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.set(Calendar.HOUR_OF_DAY, 8);
			calendar.set(Calendar.MINUTE, 0);
			calendar.set(Calendar.SECOND, 0);
			pengajuanPegawai.setWaktu(calendar.getTime());
		}

		Hbox hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(waktu = new MyDatebox(pengajuanPegawai.getWaktu()));
		waktu.setFormat(Common.dateFormat3.get().toPattern());
		waktu.setCols(6);
		waktu.setReadonly(true);

		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig(" sd ")));

		hbox.appendChild(waktuSampai = new MyDatebox(pengajuanPegawai.getWaktuSampai()));
		waktuSampai.setFormat(Common.dateFormat3.get().toPattern());
		waktuSampai.setCols(6);
		waktuSampai.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		satuanKerja = new AmbilDataSatuanKerjaBanbox(true);
		satuanKerja
				.setValue(pengajuanPegawai.getSatuanKerja() == null ? "" : pengajuanPegawai.getSatuanKerja().getNama());
		satuanKerja.setAttribute("satuanKerja", pengajuanPegawai.getSatuanKerja());
		row.appendChild(satuanKerja);
		satuanKerja.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja Pengaju"));
		satuanKerjaPengaju = new AmbilDataSatuanKerjaBanbox(true);
		satuanKerjaPengaju.setValue(pengajuanPegawai.getSatuanKerjaPengaju() == null ? ""
				: pengajuanPegawai.getSatuanKerjaPengaju().getNama());
		satuanKerjaPengaju.setAttribute("satuanKerja", pengajuanPegawai.getSatuanKerjaPengaju());
		row.appendChild(satuanKerjaPengaju);
		satuanKerjaPengaju.setWidth("90%");

		EventListener eventListenerPegawai = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				satuanKerja.setDisabled(false);
				Pegawai peg = (Pegawai) pegawai.getAttribute("pegawai");
				if (peg != null && peg.getSatuanKerja() != null) {
					SatuanKerja s = peg.getSatuanKerja();
					satuanKerja.setAttribute("satuanKerja", s);
					satuanKerja.setValue(s.getNama());
					satuanKerja.setDisabled(true);

					if (pengajuanPegawai.getId() == null) {
						satuanKerjaPengaju.setAttribute("satuanKerja", s);
						satuanKerjaPengaju.setValue(s.getNama());
						satuanKerjaPengaju.setDisabled(false);
					}
				}
			}
		};

		pegawai.setEventListener(eventListenerPegawai);
		Common.createDefaultTimer(eventListenerPegawai);

		if (jenis != null) {
			pengajuanPegawai.setJenisPengajuanPegawai(jenis);
		}

		jenisPengajuanPegawai = new Combobox();

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pengajuan *"));

		if (jenis != null) {
			row.appendChild(new Label(jenis.getNama()));
		} else {
			row.appendChild(jenisPengajuanPegawai);
		}
		jenisPengajuanPegawai.setWidth("90%");
		jenisPengajuanPegawai.setReadonly(true);

		final MyFormRow rowUsernameDisposisi = new MyFormRow();
		rowUsernameDisposisi.setParent(rows);
		rowUsernameDisposisi.appendChild(new ais.ui.util.MyLabelConfig("Keterangan *"));
		rowUsernameDisposisi.appendChild(keterangan = new Textbox(pengajuanPegawai.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(2);

		final MyFormRow rowBanyak = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(rowBanyak, "2");
		rowBanyak.setVisible(true);
		rowBanyak.setParent(rows);

		EventListener eventListenerKet = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				pengajuanPegawai.setWaktu(waktu.getValue());
				pengajuanPegawai.setWaktuSampai(waktuSampai.getValue());
				rowBanyak.setVisible(false);
				rowUsernameDisposisi.setVisible(false);

				if (pengajuanPegawai.getJumlahHari() > 1) {
					rowBanyak.setVisible(true);
				} else {
					rowUsernameDisposisi.setVisible(true);
				}

				Common.clear(rowBanyak);

				Grid gridLampiran = new Grid();
				gridLampiran.setSclass("fgrid");
				gridLampiran.setParent(rowBanyak);

				Columns columns = new Columns();
				columns.setParent(gridLampiran);

				MyColumnConfig column = new MyColumnConfig();
				column.setParent(columns);
				column.setWidth("30%");

				column = new MyColumnConfig();
				column.setParent(columns);

				Rows rowsLampiran = new Rows();
				rowsLampiran.setParent(gridLampiran);

				keteranganBanyak = new JSONObject(pengajuanPegawai.getKeteranganBanyak());

				for (int i = 0; i < pengajuanPegawai.getJumlahHari(); i++) {
					final String key = "" + i;
					String ket = keteranganBanyak.isNull(key) ? "" : keteranganBanyak.get(key) + "";

					if (i == 0 && ket.trim().isEmpty()) {
						ket = keterangan.getValue().trim();
					}

					MyFormRow rowUsernameDisposisi = new MyFormRow();
					rowUsernameDisposisi.setParent(rowsLampiran);
					rowUsernameDisposisi.appendChild(new Label("Keterangan kegiatan hari ke-" + (i + 1) + " *"));
					final Textbox keterangan;
					rowUsernameDisposisi.appendChild(keterangan = new Textbox(ket));
					keterangan.setWidth("90%");
					keterangan.setRows(2);

					keterangan.addEventListener("onChange", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							keteranganBanyak.put(key, keterangan.getValue().trim());
							pengajuanPegawai.setKeteranganBanyak(keteranganBanyak.toString());

							if (key.equalsIgnoreCase("0")) {
								PengajuanPegawaiAction.this.keterangan.setValue(keterangan.getValue().trim());
							}
						}
					});
				}

			}
		};

		eventListenerKet.onEvent(null);

		waktu.addEventListener("onChange", eventListenerKet);
		waktuSampai.addEventListener("onChange", eventListenerKet);

		MyFormRow rowLampiran = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(rowLampiran, "2");
		rowLampiran.setParent(rows);

		final Grid gridLampiran = new Grid();
		gridLampiran.setSclass("fgrid");
		gridLampiran.setParent(rowLampiran);

		columns = new Columns();
		columns.setParent(gridLampiran);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		final Rows rowsLampiran = new Rows();
		rowsLampiran.setParent(gridLampiran);

		final EventListener eventListenerJenisPengajuanPegawai = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.clear(rowsLampiran);

				JenisPengajuanPegawai j = jenis != null ? jenis
						: (JenisPengajuanPegawai) (jenisPengajuanPegawai.getSelectedItem() == null ? null
								: jenisPengajuanPegawai.getSelectedItem().getValue());

				if (j != null) {

					if (pengajuanPegawai.getId() == null) {
						String noAgenda = generateCode(j, false);
						kode.setValue(noAgenda);
					}

					parameterRows = new ArrayList<Row>();
					lampiranLains = new HashMap<String, LampiranLain>();
					HibernateUtil.currentSession().refresh(j);

					Set<KelompokParameterTambahanPengajuanPegawai> kelompokParameterTambahanPengajuanPegawais = new TreeSet<KelompokParameterTambahanPengajuanPegawai>();
					for (KelompokParameterTambahanPengajuanPegawai kelompokParameterTambahanPengajuanPegawai : j
							.getKelompokParameterTambahanPengajuanPegawais()) {
						kelompokParameterTambahanPengajuanPegawais.add(kelompokParameterTambahanPengajuanPegawai);
					}

					parameterTambahanListener = new ParameterTambahanPengajuanPegawaiListener(pengajuanPegawai,
							kelompokParameterTambahanPengajuanPegawais, parameterRows, lampiranLains, rowsLampiran);

					parameterTambahanListener.onEvent(null);
				}
			}

		};

		if (jenis != null) {
			pengajuanPegawai.setJenisPengajuanPegawai(jenis);
		}

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.insertCombo(jenisPengajuanPegawai, new String[] { "nama", "kode" }, "keterangan",
						JenisPengajuanPegawai.class, Restrictions.eq("aktif", true));
				Common.selectComboItem(jenisPengajuanPegawai, pengajuanPegawai.getJenisPengajuanPegawai());

				if (jenis != null) {
					jenisPengajuanPegawai.setDisabled(true);
				}

				eventListenerJenisPengajuanPegawai.onEvent(arg0);
			}

		};

		jenisPengajuanPegawai.addEventListener("onChange", eventListenerJenisPengajuanPegawai);
		Common.createDefaultTimer(eventListener);

		return grid;
	}

	private String generateCode(JenisPengajuanPegawai j, boolean tambah) {

		try {
			if (j == null || j.getNomorSurat() == null) {
				return "";
			}

			Long index = j.getNomorSurat().getGunakanIndexUrut() ? j.getNomorSurat().getNomorIndex() : getindex(j);
			if (tambah) {
				NomorSurat.tambahIndexNomorSurat(j.getNomorSurat());
			}
			String noAgenda = j.getNomorSurat().format(index, waktu.getValue());
			return noAgenda;
		} catch (Exception e) {
			return "";
		}
	}

	private Long getindex(JenisPengajuanPegawai jenisPengajuanPegawai) {
		if (jenisPengajuanPegawai.getNomorSurat() == null) {
			return 0L;
		}
		Session session = HibernateUtil.currentSession();
		int tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		int bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		Date sekarang = WaktuUtil.getDate();
		Number indexO = (Number) session.createCriteria(PengajuanPegawai.class)
				.createAlias("jenisPengajuanPegawai", "jenisPengajuanPegawai", Criteria.LEFT_JOIN)
				.createAlias("jenisPengajuanPegawai.nomorSurat", "nomorSurat", Criteria.LEFT_JOIN)

				.add(jenisPengajuanPegawai.getNomorSurat().getUrutBerdasarkanNomor()
						? Restrictions.eq("jenisPengajuanPegawai.nomorSurat", jenisPengajuanPegawai.getNomorSurat())

						: (jenisPengajuanPegawai.getNomorSurat().getUrutBerdasarkanKelompok()
								&& jenisPengajuanPegawai.getNomorSurat().getKelompokNomorSurat() != null
										? Restrictions.eq("nomorSurat.kelompokNomorSurat",
												jenisPengajuanPegawai.getNomorSurat().getKelompokNomorSurat())
										: Restrictions.sqlRestriction("true")))

				.add(jenisPengajuanPegawai.getNomorSurat().getResetUrutanTiapTahun() ? Restrictions.eq("tahun", tahun)
						: Restrictions.sqlRestriction("true"))

				.add(jenisPengajuanPegawai.getNomorSurat().getResetUrutanTiapBulan()
						? Restrictions.and(Restrictions.eq("tahun", tahun), Restrictions.eq("bulan", bulan))
						: Restrictions.sqlRestriction("true"))

				.add(jenisPengajuanPegawai.getNomorSurat().getResetTiap() != null
						&& (Common.dateFormat8.get().format(jenisPengajuanPegawai.getNomorSurat().getResetTiap())
								.equals(Common.dateFormat8.get().format(sekarang))
								|| jenisPengajuanPegawai.getNomorSurat().getResetTiap().before(sekarang))
										? Restrictions.ge("waktu", jenisPengajuanPegawai.getNomorSurat().getResetTiap())
										: Restrictions.sqlRestriction("true"))

				.setProjection(Projections.rowCount()).uniqueResult();

		Long index = indexO == null ? null : indexO.longValue();
		if (index == null) {
			index = 0L;
		}
		return ++index;
	}

	private void init(final PengajuanPegawai pengajuanPegawai) throws Exception {
		this.pengajuanPegawai = pengajuanPegawai;
		addWindow.setTitle(pengajuanPegawai.getId() == null ? "Tambah Pengajuan Pegawai" : "Ubah Pengajuan Pegawai");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		disposisiSop = null;
		center.appendChild(form(pengajuanPegawai, disposisiSop, save, null));

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
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

		if (pegawai.getAttribute("pegawai") == null) {
			MyMessageboxConfig.show(
					"Mohon Bapak/Ibu melengkapi isian Pegawai terlebih dahulu. Langkah yang dapat dilakukan: (1) klik kolom pemilihan Pegawai; (2) pilih pegawai yang bersangkutan; (3) simpan kembali data pengajuan.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (jenisPengajuanPegawai.getSelectedItem() == null && jenis == null) {
			MyMessageboxConfig.show(
					"Mohon Bapak/Ibu memilih Jenis Pengajuan terlebih dahulu. Langkah yang dapat dilakukan: (1) klik kolom pilihan Jenis Pengajuan; (2) pilih jenis pengajuan yang sesuai; (3) simpan kembali data pengajuan.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}
		Pegawai peg = (Pegawai) pegawai.getAttribute("pegawai");
		if (!peg.getAktif()) {
			MyMessageboxConfig.show(
					"Mohon maaf, data pegawai yang dipilih berstatus tidak aktif sehingga pengajuan tidak dapat diproses. Langkah yang dapat dilakukan: (1) pastikan pegawai yang dipilih sudah benar; (2) aktifkan kembali data pegawai pada menu data pegawai apabila diperlukan; (3) ulangi kembali proses pengajuan.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (parameterTambahanListener != null && !parameterTambahanListener.validate()) {
			return false;
		}

		pengajuanPegawai.setWaktu(waktu.getValue());
		pengajuanPegawai.setWaktuSampai(waktuSampai.getValue());

		for (int i = 0; i < pengajuanPegawai.getJumlahHari(); i++) {
			String key = "" + i;
			String ket = keteranganBanyak.isNull(key) ? "" : keteranganBanyak.get(key) + "";

			if (i == 0 && ket.trim().isEmpty()) {
				ket = keterangan.getValue().trim();
			}

			if (ket.trim().isEmpty()) {
				MyMessageboxConfig.show(
						"Mohon Bapak/Ibu melengkapi isian Keterangan terlebih dahulu. Langkah yang dapat dilakukan: (1) isi kolom Keterangan untuk setiap hari kegiatan; (2) pastikan tidak ada keterangan yang dikosongkan; (3) simpan kembali data pengajuan.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return false;
			}
		}

		boolean baru = false;
		Session session = HibernateUtil.currentSession();
		if (pengajuanPegawai.getId() != null) {
			pengajuanPegawai = (PengajuanPegawai) session.load(PengajuanPegawai.class, pengajuanPegawai.getId());

		}
		pengajuanPegawai.setPegawai((Pegawai) pegawai.getAttribute("pegawai"));
		pengajuanPegawai.setWaktu(waktu.getValue());
		pengajuanPegawai.setWaktuSampai(waktuSampai.getValue());
		pengajuanPegawai.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
		pengajuanPegawai.setSatuanKerjaPengaju((SatuanKerja) satuanKerjaPengaju.getAttribute("satuanKerja"));
		pengajuanPegawai.setJenisPengajuanPegawai(jenis != null ? jenis
				: (JenisPengajuanPegawai) (jenisPengajuanPegawai.getSelectedItem() == null ? null
						: jenisPengajuanPegawai.getSelectedItem().getValue()));
		pengajuanPegawai.setKeteranganBanyak(keteranganBanyak == null ? null : keteranganBanyak.toString());
		pengajuanPegawai.setNama(nama.getValue());

		pengajuanPegawai.setKeterangan(keterangan.getValue());

		if (disposisiSop != null && disposisiSop.getId() != null) {
			pengajuanPegawai.setDisposisiSop(disposisiSop);
		}

		parameterTambahanListener.onSave(pengajuanPegawai);

		if (pengajuanPegawai.getId() != null) {

			if (pengajuanPegawai.getIndex() == null) {
				String noAgenda = generateCode(pengajuanPegawai.getJenisPengajuanPegawai(), true);
				kode.setValue(noAgenda);
				pengajuanPegawai.setKode(noAgenda);
				Long currentIndex = getindex(pengajuanPegawai.getJenisPengajuanPegawai());
				pengajuanPegawai.setIndex(++currentIndex);
			}

			Common.refreshUpdate(session, pengajuanPegawai);
		} else {
			if (pengajuanPegawai.getKode() == null) {
				String noAgenda = generateCode(pengajuanPegawai.getJenisPengajuanPegawai(), true);
				kode.setValue(noAgenda);
				pengajuanPegawai.setKode(noAgenda);
			}
			pengajuanPegawai.setDiajukanOleh(tbmuser);
			Long currentIndex = getindex(pengajuanPegawai.getJenisPengajuanPegawai());
			pengajuanPegawai.setIndex(++currentIndex);
			session.save(pengajuanPegawai);
			baru = true;
		}

		if (!lampiranLains.isEmpty()) {
			Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
			streamingSession.getTransaction().begin();
			for (LampiranLain lampiranLain : lampiranLains.values()) {
				streamingSession.refresh(lampiranLain);
				lampiranLain.setRef(pengajuanPegawai.getId());
				streamingSession.update(lampiranLain);
			}
			streamingSession.getTransaction().commit();
			StreamingHibernateUtil.getInstance().closeSession();
		}

		// (B.1) Pengajuan BARU -> beri tahu atasan penyetuju agar segera menindaklanjuti.
		if (baru) {
			final PengajuanPegawai pp = pengajuanPegawai;
			Common.createDefaultTimer(new EventListener() {
				@Override
				public void onEvent(Event a) throws Exception {
					try {
						Pegawai peg = pp.getPegawai();
						java.util.List<Tbmuser> penyetuju = peg == null ? null
								: resolveUsersDariPegawai(peg.getAtasanlangsung(), peg.getAtasanlangsung2(),
										peg.getAtasanlangsung3());
						String jenisN = pp.getJenisPengajuanPegawai() == null ? "Pengajuan Kepegawaian"
								: pp.getJenisPengajuanPegawai().getNama();
						ais.common.CommonNotifikasi.pengajuanPegawaiPerluPersetujuan(penyetuju, jenisN,
								peg == null ? "" : peg.getNama(), periodePengajuan(pp), pp.getKeterangan(), pp,
								HAL_ZK_PENGAJUAN_PEGAWAI, null);
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/PengajuanPegawaiAction.java:1163");
					}
				}
			});
		}

		return true;
	}

	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	/** Halaman ZKoss tujuan klik notifikasi kepegawaian. */
	private static final String HAL_ZK_PENGAJUAN_PEGAWAI = "/pages/master/pengajuan_pegawai.zul";

	/**
	 * Resolusi akun pengguna ({@link Tbmuser}) dari satu/beberapa {@link Pegawai}.
	 * Dipakai untuk menentukan penerima notifikasi (atasan penyetuju maupun pengaju)
	 * berdasarkan relasi pegawai, sehingga notifikasi tepat sasaran.
	 */
	@SuppressWarnings("unchecked")
	private static java.util.List<Tbmuser> resolveUsersDariPegawai(Pegawai... pegawais) {
		java.util.List<Tbmuser> hasil = new java.util.ArrayList<Tbmuser>();
		java.util.List<Pegawai> ps = new java.util.ArrayList<Pegawai>();
		if (pegawais != null) {
			for (Pegawai p : pegawais) {
				if (p != null) {
					ps.add(p);
				}
			}
		}
		if (ps.isEmpty()) {
			return hasil;
		}
		try {
			Session s = HibernateUtil.currentSession();
			java.util.List<Tbmuser> us = s.createCriteria(Tbmuser.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.in("pegawai", ps)).list();
			if (us != null) {
				hasil.addAll(us);
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		return hasil;
	}

	/** Teks periode pengajuan (waktu s.d waktuSampai) untuk ditampilkan di notifikasi. */
	private static String periodePengajuan(PengajuanPegawai pp) {
		try {
			String a = pp.getWaktu() == null ? "" : Common.dateFormat1.get().format(pp.getWaktu());
			String b = pp.getWaktuSampai() == null ? "" : Common.dateFormat1.get().format(pp.getWaktuSampai());
			if (a.isEmpty() && b.isEmpty()) {
				return "-";
			}
			if (b.isEmpty() || a.equals(b)) {
				return a;
			}
			return a + " s.d " + b;
		} catch (Exception e) {
			return "-";
		}
	}

	public Criteria initCriteria(boolean order) {

		Tbmuser tbmuser = Common.getCurrentUser();
		Pegawai existing = null;
		if (tbmuser != null && tbmuser.ambilPegawai() != null) {
			existing = tbmuser.ambilPegawai();
		}

		Tbmrole tbmrole = tbmuser == null ? null : tbmuser.hakAkses();

		List<JenisPengajuanPegawai> jenisPengajuanPegawais = new ArrayList<JenisPengajuanPegawai>();
		for (Object o : ConstantValues.ambilBerdasarClass(JenisPengajuanPegawai.class).values()) {
			try {
				JenisPengajuanPegawai jenisPengajuanPegawai = (JenisPengajuanPegawai) o;
				if ((tbmrole != null && jenisPengajuanPegawai.getJenisPengguna().toLowerCase()
						.contains("," + tbmrole.getRoleId().toLowerCase() + ","))
						|| (tbmuser != null && jenisPengajuanPegawai.getUsernamePengguna().toLowerCase()
								.contains("," + tbmuser.getUserId().toLowerCase() + ","))) {
					jenisPengajuanPegawais.add(jenisPengajuanPegawai);
				}
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
		}

		Session session = HibernateUtil.currentSession();
		// User punya akses lebih luas? (atasan/punya bawahan, atau role/username terdaftar
		// di Jenis Pengajuan). Jika ya, JANGAN paksa hanya pengajuan miliknya sendiri —
		// biar filter bawahan/jenis di bawah yang menentukan, supaya pengajuan bawahan tampil.
		boolean punyaAksesLebih = (punyaBawahan != null && punyaBawahan.size() > 1)
				|| (punyaBawahanDosen != null && !punyaBawahanDosen.isEmpty())
				|| (jenisPengajuanPegawais != null && !jenisPengajuanPegawais.isEmpty());

		Criteria criteria = session.createCriteria(PengajuanPegawai.class)

				.add(start == null || start.getValue() == null || end == null || end.getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.sqlRestriction(
								"date(this_.waktu) between date('" + Common.databaseDateFormat.get().format(start.getValue())
										+ "') and date('" + Common.databaseDateFormat.get().format(end.getValue()) + "')"))

				.createAlias("pegawai", "pegawai", Criteria.LEFT_JOIN)
				.add(jenis == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("jenisPengajuanPegawai", jenis))

				.add(existing == null || punyaAksesLebih ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("pegawai", existing))

				// "Tampilkan hanya yang aktif": saat DICENTANG -> hanya record aktif (aktif = true
				// atau null, sesuai definisi "aktif" di seluruh kelas ini); saat TIDAK dicentang ->
				// tampilkan semua. (Sebelumnya keliru memfilter kolom "setujui" sehingga data yang
				// sudah disetujui ikut tersembunyi -> tampak terbalik / kosong.)
				.add(searchaktif != null && searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))

				.add(searchpegawai.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("pegawai.nama", searchpegawai.getValue().trim(), MatchMode.ANYWHERE))

		;

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria

				.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("keterangan", searchnama.getValue().trim(), MatchMode.ANYWHERE))

		;

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Criterion c = satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
				: Restrictions.or(Restrictions.isNull("satuanKerja"),
						Restrictions.or(
								parent == null ? Restrictions.isNull("satuanKerja")
										: Restrictions.sqlRestriction("false"),
								Restrictions.in("satuanKerja", satuanKerjas)));

		if (punyaBawahan != null && !punyaBawahan.isEmpty() && punyaBawahanDosen != null
				&& !punyaBawahanDosen.isEmpty()) {
			criteria

					.createAlias("diajukanOleh", "diajukanOleh", Criteria.LEFT_JOIN).createAlias("pegawai.dosen",
							"dosen", Criteria.LEFT_JOIN)
					.add(

							Restrictions.or(
									jenisPengajuanPegawais.isEmpty() ? Restrictions.sqlRestriction("false")
											: Restrictions.and(c,
													Restrictions.in("jenisPengajuanPegawai", jenisPengajuanPegawais)),

									Restrictions.or(
											Restrictions.or(Restrictions.in("diajukanOleh.pegawai.id", punyaBawahan),
													Restrictions.in("diajukanOleh.dosen.id", punyaBawahanDosen)),
											Restrictions.or(Restrictions.in("pegawai.id", punyaBawahan),
													Restrictions.in("dosen.id", punyaBawahanDosen))))

					);

			searchparent.setDisabled(true);

		} else if (punyaBawahan != null && !punyaBawahan.isEmpty()) {
			criteria.createAlias("diajukanOleh", "diajukanOleh", Criteria.LEFT_JOIN)
					.add(Restrictions.or(
							jenisPengajuanPegawais.isEmpty() ? Restrictions.sqlRestriction("false")
									: Restrictions.and(c,
											Restrictions.in("jenisPengajuanPegawai", jenisPengajuanPegawais)),
							Restrictions.or(Restrictions.in("diajukanOleh.pegawai.id", punyaBawahan),
									Restrictions.in("pegawai.id", punyaBawahan))));
			searchparent.setDisabled(true);
		} else if (punyaBawahanDosen != null && !punyaBawahanDosen.isEmpty()) {
			criteria.createAlias("diajukanOleh", "diajukanOleh", Criteria.LEFT_JOIN)
					.createAlias("pegawai.dosen", "dosen",
							Criteria.LEFT_JOIN)
					.add(Restrictions.or(
							jenisPengajuanPegawais.isEmpty() ? Restrictions.sqlRestriction("false")
									: Restrictions.and(c,
											Restrictions.in("jenisPengajuanPegawai", jenisPengajuanPegawais)),
							Restrictions.or(Restrictions.in("diajukanOleh.dosen.id", punyaBawahanDosen),
									Restrictions.in("dosen.id", punyaBawahanDosen))));
			searchparent.setDisabled(true);
		} else {

			criteria.add(c);
		}

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PengajuanPegawai> pengajuanPegawai = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(pengajuanPegawai);
		grid.setRowRenderer(new PengajuanPegawaiRenderer());
		grid.setModelCheckMobile(strset);

	}

	@Override
	public String istilah() throws Exception {
		// TODO Auto-generated method stub
		return "Pengajuan Pegawai";
	}

	@Override
	public DataSop ambil() throws Exception {
		// TODO Auto-generated method stub
		return pengajuanPegawai;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClass() throws Exception {
		// TODO Auto-generated method stub
		return PengajuanPegawai.class;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public File cetakData(GeneralValueObject generalValueObject) throws Exception {
		PengajuanPegawai pengaduan = (PengajuanPegawai) generalValueObject;
		JenisPengajuanPegawai j = pengaduan.getJenisPengajuanPegawai();
		if (j == null) {
			return null;
		}

		LampiranLain lainMahaadministrasi = LampiranLain.ambil(j.getId(),
				LampiranLain.FILE_JRXML_LAYOUT_JENIS_PENGAJUAN);

		if (lainMahaadministrasi == null) {
			return null;
		}

		Map parameters = LaporanPengajuanPegawai.generateParameter(j, null, null, pengaduan.getPegawai(), pengaduan,
				null);

		File file = Report.generateCompileFileReport(Report.PDF, parameters,
				lainMahaadministrasi.ambilFile().getAbsolutePath(), ais.ui.util.WaktuUtil.getDate());
		return file;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void onKHS(PengajuanPegawai pengajuanPegawai) throws Exception {

		try {

			JenisPengajuanPegawai j = pengajuanPegawai.getJenisPengajuanPegawai();
			if (j == null) {
				return;
			}

			LampiranLain lainMahaadministrasi = LampiranLain.ambil(j.getId(),
					LampiranLain.FILE_JRXML_LAYOUT_JENIS_PENGAJUAN);

			if (lainMahaadministrasi == null) {
				MyMessageboxConfig.show(
						"Mohon maaf, berkas (file) template laporan untuk jenis Pengajuan Pegawai ini belum diunggah sehingga laporan belum dapat ditampilkan. Langkah yang dapat dilakukan: (1) buka menu Jenis Pengajuan Pegawai; (2) unggah berkas template laporan (jrxml atau jasper) pada jenis pengajuan yang bersangkutan; (3) ulangi kembali proses pencetakan laporan.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return;
			}

			MyWindow window = new MyWindow("Laporan", "none", true);
			window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
			window.setHeight("90%");
			window.setWidth("900px");

			Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
			borderlayout.setParent(window);

			final Center center = new Center();
			ais.ui.util.ZkCompat.setFlex(center, true);
			center.setParent(borderlayout);

			Map parameters = LaporanPengajuanPegawai.generateParameter(j, null, null, pengajuanPegawai.getPegawai(),
					pengajuanPegawai, null);

			File file = Report.generateCompileFileReport(Report.PDF, parameters,
					lainMahaadministrasi.ambilFile().getAbsolutePath(), ais.ui.util.WaktuUtil.getDate());

			CommonReport.tampilkanReportPDF(center, file);

			window.onModal();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

	}

	@Override
	public void setPersetujuan(boolean persetujuan) {
//		this.persetujuan = persetujuan;
	}
}
