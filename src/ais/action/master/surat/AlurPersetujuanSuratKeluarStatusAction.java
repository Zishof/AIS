package ais.action.master.surat;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.pdfbox.util.PDFMergerUtility;
import org.hibernate.Criteria;
import org.hibernate.Session;
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
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import ais.ui.util.MyDetail;
import org.zkoss.zul.East;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Iframe;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.BroadcastHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataPejabatBanbox;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.surat.helper.DasboardSurat;
import ais.action.master.surat.helper.SuratKeluarPunyaGambarFotoHelper;
import ais.action.master.surat.util.SuratUtil;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.employ.JenisJabatan;
import ais.database.model.file.FotoGambarSuratKeluar;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.Pejabat;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.surat.AlurPersetujuanSuratKeluar;
import ais.database.model.surat.AlurPersetujuanSuratKeluarStatus;
import ais.database.model.surat.AlurPersetujuanSuratMasuk;
import ais.database.model.surat.AlurPersetujuanSuratMasukStatus;
import ais.database.model.surat.KlasifikasiSuratKeluarParemeterValue;
import ais.database.model.surat.OpsiSuratKeluarValue;
import ais.database.model.surat.SuratKeluar;
import ais.database.model.surat.SuratMasuk;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelStyled;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class AlurPersetujuanSuratKeluarStatusAction extends GenericAutowireComposer implements DataCriteria {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchkode;
	private Combobox searchjenisjabatan;
	private MyCheckboxConfig searchbelumsayaajukan;
	private AmbilDataSatuanKerjaBanbox searchparent;
	private Iframe template;

	private AmbilDataPejabatBanbox pejabat;
	private MyCheckboxConfig disetujui;
	private Textbox keterangan;

	private boolean edit = false;
	private AlurPersetujuanSuratKeluarStatus alurPersetujuanSuratKeluarStatus;
	private MyToolbarbuttonConfig add;
	private MyGrid gridGambar;
	private HashSet<JenisJabatan> selectedJenisJabatan;
	private HashSet<JenisJabatan> removedJenisJabatan;
	private Vbox vboxAlur;

	private Boolean ubahLangsungA = false;
	private MyDatebox waktuPersetujuan;
	protected LampiranLain lainMahasiswa;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	private List<JenisJabatan> jenisJabatans = null;
	private JSONObject jenisSurats;
	private EventListener eventListener = null;
	private boolean ubah = true;
	private MyCheckboxConfig ditolak;
	private MyDatebox waktuDitolak;
	private Tbmuser tbmuser;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private SuratKeluar suratKeluar = null;
	private MyCheckboxConfig blmDisetujui;
	private MyCheckboxConfig telahDisetujui;
	private MyCheckboxConfig selesai;

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();

		if (execution.getParameter("ubahLangsung") != null) {
			ubahLangsungA = true;
		}

		tbmuser = Common.getCurrentUser();

		if (tbmuser.hakAkses() != null && tbmuser.hakAkses().getRoleId() != null
				&& !tbmuser.hakAkses().getMelihatSemuaSurat()) {

		} else {
			searchbelumsayaajukan.setVisible(false);
		}

		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
		List<Pejabat> pejabats = null;
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.hakAkses() != null && tbmuser.hakAkses().getJenisJabatan() != null) {

			Comboitem comboitem = new Comboitem();
			comboitem.setLabel(tbmuser.hakAkses().getJenisJabatan().getNama());
			comboitem.setValue(tbmuser.hakAkses().getJenisJabatan());
			searchjenisjabatan.appendChild(comboitem);
			searchjenisjabatan.setSelectedItem(comboitem);
			searchjenisjabatan.setDisabled(true);
			pejabats = new ArrayList<Pejabat>();
		} else {

			pejabats = Common.getCurrentPejabat(true);
			if (pejabats != null && !pejabats.isEmpty()) {
				jenisJabatans = new ArrayList<JenisJabatan>();

				for (Pejabat pejabat : pejabats) {
					jenisJabatans.add(pejabat.getJenisJabatan());
				}
				Common.insertComboItems(searchjenisjabatan, "nama", jenisJabatans);

				Comboitem comboitem = new Comboitem();
				comboitem.setLabel("Semua");
				comboitem.setValue(null);
				searchjenisjabatan.appendChild(comboitem);
				searchjenisjabatan.setSelectedItem(comboitem);
				searchjenisjabatan.setReadonly(true);
			} else {
				Common.insertComboDanSemua(searchjenisjabatan, "nama", JenisJabatan.class,
						Restrictions.eq("aktif", true));
			}
		}

		if (!Common.getApakahAdmin() && pejabats == null) {
			return;
		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				searchparent.setDisabled(false);
				searchparent.setAttribute("satuanKerja", null);
				searchparent.setValue("");
				onSearchDefault(null);
			}
		});

		if (add != null) { add.setTooltiptext("Tambah"); }

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		blmDisetujui = new MyCheckboxConfig("Belum Disetujui");
		if (blmDisetujui != null) { blmDisetujui.setChecked(true); }
		telahDisetujui = new MyCheckboxConfig("Telah Disetujui");
		if (telahDisetujui != null) { telahDisetujui.setChecked(true); }

		blmDisetujui.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		telahDisetujui.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		Common.appendKeToolbar(blmDisetujui, add, comp);
		Common.appendKeToolbar(telahDisetujui, add, comp);

		String[] contents = new String[] { "id", "alurPersetujuanSuratKeluar", "disetujui", "ditolak", "pejabat",
				"waktuPersetujuan", "jenisJabatan", "telahDirevisi", "catatanDisposisi", "waktuDitolak",
				"suratKeluar.kode", "suratKeluar.agenda", "suratKeluar.nama", "suratKeluar.tanggal",
				"suratKeluar.waktu", "suratKeluar.konseptor", "suratKeluar.perihal",
				"suratKeluar.klasifikasiSuratKeluar", "suratKeluar.alurPersetujuanSuratKeluar", "suratKeluar.kepada",
				"keterangan", "konseptor", "siswa", "mahasiswa" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(AlurPersetujuanSuratKeluarStatus.class, this,
				contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);
	}

	class AlurPersetujuanSuratKeluarStatusRenderer extends ais.ui.util.MyRowRenderer {

		@SuppressWarnings("unchecked")
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final AlurPersetujuanSuratKeluarStatus alurPersetujuanSuratKeluarStatus = (AlurPersetujuanSuratKeluarStatus) arg1;

			final SuratKeluar suratKeluar = alurPersetujuanSuratKeluarStatus.getSuratKeluar();

			if (suratKeluar == null) {
				arg0.detach();
				return;
			}

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {

						Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
						borderlayout.setHeight("300px");
						borderlayout.setParent(detail);

						Center center = new Center();
						center.setParent(borderlayout);
						ais.ui.util.ZkCompat.setFlex(center, true);
						center.setAutoscroll(true);

						Iframe iframe = new Iframe();
						iframe.setHeight("100%");
						iframe.setWidth("100%");
						iframe.setScrolling("auto");
						iframe.setParent(center);
						Map<String, Object> parameters = SuratUtil.ubahIsiSuratKeluar(suratKeluar);
						PDFMergerUtility ut = new PDFMergerUtility();
						for (int index = 1; index <= 15; index++) {
							try {
								LampiranLain lampiranLain = LampiranLain.ambil(
										suratKeluar.getKlasifikasiSuratKeluar().getId(),
										LampiranLain.FILE_JRXML_LAYOUT_SURAT + (index == 1 ? "" : "_" + index));
								if (lampiranLain != null && lampiranLain.getId() != null) {
									String t = lampiranLain.ambilFile().getAbsolutePath();
									File myfile = Report.generateCompileFileReport(Report.PDF, parameters, t,
											ais.ui.util.WaktuUtil.getDate());
									if (myfile != null && myfile.exists()) {
										ut.addSource(myfile);
									}
								}
							} catch (Exception e) {
								ais.common.Common.tampilErrorJikaAdmin(e);
							}
						}

						File filePdfBaru = new File(
								Common.ambilREAL_PATH_REPORT() + "/" + Common.getGeneratedBarCode() + ".pdf");
						ut.setDestinationStream(new FileOutputStream(filePdfBaru));
						ut.mergeDocuments();
						CommonReport.tampilkanReportPDF(iframe, filePdfBaru, parameters);

					}
				}
			});

			Component parent = arg0;
			if (ubahLangsungA) {
				parent = new Vbox();
				parent.setParent(arg0);
			}

			Vbox a;
			(a = RevisiHelper.createNewRevisi(AlurPersetujuanSuratKeluarStatus.class, alurPersetujuanSuratKeluarStatus,
					alurPersetujuanSuratKeluarStatus.getSuratKeluar() == null ? ""
							: alurPersetujuanSuratKeluarStatus.getSuratKeluar().getKode()))
					.setParent(parent);

			new Label(
					suratKeluar.getTanggal() == null ? "" : Common.dateFormat41.get().format(suratKeluar.getTanggal()))
					.setParent(a);

			new Label(alurPersetujuanSuratKeluarStatus.getSuratKeluar() == null
					|| alurPersetujuanSuratKeluarStatus.getSuratKeluar().getKlasifikasiSuratKeluar() == null ? ""
							: alurPersetujuanSuratKeluarStatus.getSuratKeluar().getKlasifikasiSuratKeluar().getNama())
					.setParent(parent);

			String nama = "";
			if (alurPersetujuanSuratKeluarStatus.getMahasiswa() != null) {
				nama = alurPersetujuanSuratKeluarStatus.getMahasiswa().getNim() + " - "
						+ alurPersetujuanSuratKeluarStatus.getMahasiswa().getNama();
			} else if (alurPersetujuanSuratKeluarStatus.getPejabat() != null) {
				nama = alurPersetujuanSuratKeluarStatus.getPejabat().getNama();
			} else if (alurPersetujuanSuratKeluarStatus.getSiswa() != null) {
				nama = alurPersetujuanSuratKeluarStatus.getSiswa().getNomorInduk() + " - "
						+ alurPersetujuanSuratKeluarStatus.getSiswa().getNama();
			}

			new Label(alurPersetujuanSuratKeluarStatus.getJenisJabatan() == null ? ""
					: alurPersetujuanSuratKeluarStatus.getJenisJabatan().getNama() + (" " + nama)).setParent(parent);

			Session session = HibernateUtil.currentSession();

			List<String> suratKeluarValues = session.createCriteria(OpsiSuratKeluarValue.class)
					.setProjection(Projections.groupProperty("nama")).add(Restrictions.eq("suratKeluar", suratKeluar))
					.list();
			new ais.ui.util.MyHtml(DasboardSurat.buildOpsiChipsHtmlV20(suratKeluarValues)).setParent(parent);

			Vbox myVbox = new Vbox();
			myVbox.setParent(parent);
			List<KlasifikasiSuratKeluarParemeterValue> klasifikasiSuratKeluarParemeterValues = session
					.createCriteria(KlasifikasiSuratKeluarParemeterValue.class)
					.createAlias("klasifikasiSuratKeluarParemeter", "klasifikasiSuratKeluarParemeter")
					.addOrder(Order.asc("klasifikasiSuratKeluarParemeter.nama"))
					.add(Restrictions.eq("suratKeluar", suratKeluar)).list();
			int size = 0;
			boolean tampilSemua = false;
			String htmlIsiAwal = "";
			for (KlasifikasiSuratKeluarParemeterValue p : klasifikasiSuratKeluarParemeterValues) {
				if (size == 1) { tampilSemua = true; break; }
				htmlIsiAwal += DasboardSurat.buildParamRowHtmlV20(
						p.getKlasifikasiSuratKeluarParemeter().getNama(), p.getNama());
				size++;
			}
			final Html isi;
			(isi = new ais.ui.util.MyHtml(DasboardSurat.buildIsiWrapperHtmlV20(htmlIsiAwal, suratKeluar.getKeterangan()))).setParent(myVbox);

			if (tampilSemua) {
				MyToolbarbuttonConfig tdpOnline = new MyToolbarbuttonConfig("Tampilkan semua isi", "/img/received.png");
				tdpOnline.setParent(myVbox);
				tdpOnline.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						arg0.getTarget().setVisible(false);
						StringBuilder htmlSemua = new StringBuilder();
						Session session = HibernateUtil.currentSession();
						List<KlasifikasiSuratKeluarParemeterValue> all = session
								.createCriteria(KlasifikasiSuratKeluarParemeterValue.class)
								.createAlias("klasifikasiSuratKeluarParemeter", "klasifikasiSuratKeluarParemeter")
								.addOrder(Order.asc("klasifikasiSuratKeluarParemeter.nama"))
								.add(Restrictions.eq("suratKeluar", suratKeluar)).list();
						for (KlasifikasiSuratKeluarParemeterValue p : all) {
							htmlSemua.append(DasboardSurat.buildParamRowHtmlV20(
									p.getKlasifikasiSuratKeluarParemeter().getNama(), p.getNama()));
						}
						isi.setContent(DasboardSurat.buildIsiWrapperHtmlV20(htmlSemua.toString(), suratKeluar.getKeterangan()));
					}
				});
			}

			Vbox myvbox = new Vbox();
			myvbox.setParent(myVbox);

			Hbox hbox = new Hbox();
			hbox.setParent(myvbox);
			LampiranLain.createDownloadUploadFileLain(hbox, alurPersetujuanSuratKeluarStatus.getId(),
					AlurPersetujuanSuratKeluarStatus.class.getName(), "tindak lanjut Disposisi", false, null, null,
					false, false, false, false);

			Vbox hbox21 = new Vbox();
			hbox21.setParent(parent);
			new ais.ui.util.MyHtml(DasboardSurat.buildAlurKeluarStatusHtmlV20(alurPersetujuanSuratKeluarStatus))
					.setParent(hbox21);
			Hbox aa = new Hbox();
			aa.setParent(hbox21);
			LampiranLain.createDownloadUploadFileLain(aa, alurPersetujuanSuratKeluarStatus.getId(),
					AlurPersetujuanSuratKeluarStatus.class.getName(), "tindak lanjut Disposisi", false,
					new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							lainMahasiswa = (LampiranLain) arg0.getData();
						}
					});

			Hbox hbox2 = new Hbox();
			hbox2.setParent(arg0);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Catatan Disposisi", "/img/print.png");
			button.setOrient("vertical");
			button.setTooltiptext("Lihat catatan disposisi (tabel)");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					ais.action.master.surat.helper.CatatanDisposisiPopupHelper.showKeluar(
							alurPersetujuanSuratKeluarStatus, tbmuser, (org.zkoss.zk.ui.Component) event.getTarget());
				}

			});
			button.setParent(hbox2);

			JenisJabatan jenisJabatan = alurPersetujuanSuratKeluarStatus.getJenisJabatan();

			boolean boleh = false;
			if (jenisJabatan != null) {
				List<Pejabat> jab = Common.getCurrentPejabat(false);
				if (jab != null && !jab.isEmpty()) {
					for (Pejabat pejabat : jab) {
						if (pejabat.getJenisJabatan() != null
								&& pejabat.getJenisJabatan().getId().equals(jenisJabatan.getId())) {
							boleh = true;
							break;
						}
					}
				}
			}

			if (Common.getApakahAdmin()) {
				boleh = true;
			}

			if (!alurPersetujuanSuratKeluarStatus.getDisetujui()) {

				if (boleh) {

					button = new MyToolbarbuttonConfig("Tindak Lanjuti", "/img/Check-icon.png");
					button.setOrient("vertical");
					button.setTooltiptext("Ubah Data");
					button.setVisible(edit);
					button.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							init(alurPersetujuanSuratKeluarStatus);
							addWindow.setVisible(true);
							addWindow.onModal();
						}

					});
					button.setParent(hbox2);

					button = new MyToolbarbuttonConfig("Batalkan", "/img/Check-icon.png");
					button.setOrient("vertical");
					button.setTooltiptext("Ubah Data");
					button.setVisible(edit);
					button.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {

							MyMessageboxConfig.show("Apakah yakin ingin membatalkan disposisi data ini ?", "Pertanyaan",
									MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
									new EventListener() {

										@Override
										public void onEvent(Event event) throws Exception {
											int i = Integer.parseInt(event.getData().toString());
											if (i == MyMessageboxConfig.OK) {
												try {

													Common.refreshDelete(alurPersetujuanSuratKeluarStatus);

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
					button.setParent(hbox2);
				}

			} else {
				if (boleh) {
					button = new MyToolbarbuttonConfig("Ubah", "/img/Check-icon.png");
					button.setOrient("vertical");
					button.setTooltiptext("Ubah Data");
					button.setVisible(edit);
					button.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							init(alurPersetujuanSuratKeluarStatus);
							addWindow.setVisible(true);
							addWindow.onModal();
						}

					});
					button.setParent(hbox2);
				}

				button = new MyToolbarbuttonConfig("Lihat", "/img/eye-icon.png");
				button.setOrient("vertical");
				button.setTooltiptext("Lihat Data");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						preview(alurPersetujuanSuratKeluarStatus);
						addWindow.setVisible(true);
						addWindow.onModal();
					}
				});
				button.setParent(hbox2);

			}
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new AlurPersetujuanSuratKeluarStatus());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	public static void onPreview(AlurPersetujuanSuratKeluarStatus alurPersetujuanSuratKeluarStatus) throws Exception {
		AlurPersetujuanSuratKeluarStatusAction skripsiAction = new AlurPersetujuanSuratKeluarStatusAction();
		skripsiAction.addWindow = new MyWindow();
		skripsiAction.tbmuser = Common.getCurrentUser();
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(skripsiAction.addWindow);
		skripsiAction.addWindow.setHeight("95%");
		skripsiAction.addWindow.setWidth("90%");

		skripsiAction.preview(alurPersetujuanSuratKeluarStatus);

		skripsiAction.addWindow.setVisible(true);
		skripsiAction.addWindow.setClosable(true);
		skripsiAction.addWindow.onModal();
	}

	public static void onAddExternal(EventListener eventListener,
			AlurPersetujuanSuratKeluarStatus alurPersetujuanSuratKeluarStatus) throws Exception {
		AlurPersetujuanSuratKeluarStatusAction skripsiAction = new AlurPersetujuanSuratKeluarStatusAction();
		skripsiAction.tbmuser = Common.getCurrentUser();
		skripsiAction.eventListener = eventListener;
		skripsiAction.addWindow = new MyWindow();

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(skripsiAction.addWindow);
		skripsiAction.addWindow.setHeight("95%");
		skripsiAction.addWindow.setWidth("90%");

		skripsiAction.init(alurPersetujuanSuratKeluarStatus);

		skripsiAction.addWindow.setVisible(true);
		skripsiAction.addWindow.setClosable(true);
		skripsiAction.addWindow.onModal();
	}

	protected void initDetail(SuratKeluar suratKeluar, Component component, boolean preview) throws Exception {

		gridGambar = new MyGrid();
		template = new Iframe();

		AlurPersetujuanSuratKeluarStatusAction.initDetail(suratKeluar, component, template, gridGambar, false,
				jenisSurats, alurPersetujuanSuratKeluarStatus, preview);

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void initDetail(final SuratKeluar suratKeluar, Component component, Iframe template,
			MyGrid gridGambar, boolean tampilEdit, JSONObject jenisSurats,
			AlurPersetujuanSuratKeluarStatus alurPersetujuanSuratKeluarStatus, boolean preview) throws Exception {
		Tabbox tabbox = new Tabbox();
		tabbox.setParent(component);
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tabPreview = new MyTabConfig("Preview Surat Keluar");
		tabPreview.setParent(tabs);

		MyTabConfig tabGambar = new MyTabConfig("Lampiran Surat");
		tabGambar.setParent(tabs);

		if (preview) {
			if (alurPersetujuanSuratKeluarStatus == null
					|| alurPersetujuanSuratKeluarStatus.getAlurPersetujuanSuratKeluar() == null
					|| !alurPersetujuanSuratKeluarStatus.getAlurPersetujuanSuratKeluar().getHarusMengikutiAlur()) {
				MyTabConfig tabDisposisi = new MyTabConfig("Disposisi ke");
				tabDisposisi.setParent(tabs);
			}
		}

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanelPreview = new ais.ui.util.MyTabpanel();
		tabpanelPreview.setParent(tabpanels);
		tabpanelPreview.setHeight("100%");
		tabpanelPreview.appendChild(template);
		// Iframe pratinjau MENGISI tinggi modal secara PROPORSIONAL. Sebelumnya tinggi di-set FIX 5000px
		// sehingga surat A4 tampil tidak proporsional (iframe sangat tinggi, isi kecil di atas + banyak
		// ruang kosong/scroll). 100% mengikuti tinggi panel (window 95% viewport) → viewer PDF menskalakan
		// halaman agar pas lebar. min-height:70vh mencegah kolaps ke 0 bila rantai tinggi tak terdefinisi.
		template.setHeight("100%");
		template.setWidth("100%");
		template.setStyle("min-height:70vh;");
		template.setScrolling("auto");

		try {
			Map parameters = SuratUtil.ubahIsiSuratKeluar(suratKeluar);

			PDFMergerUtility ut = new PDFMergerUtility();
			java.util.List<File> htmlParts = new java.util.ArrayList<File>();

			for (int index = 1; index <= 15; index++) {
				try {
					LampiranLain lampiranLain = LampiranLain.ambil(suratKeluar.getKlasifikasiSuratKeluar().getId(),
							LampiranLain.FILE_JRXML_LAYOUT_SURAT + (index == 1 ? "" : "_" + index));
					if (lampiranLain != null && lampiranLain.getId() != null) {
						try {

							File file = Report.generateCompileFileReport(Report.PDF, parameters,
									lampiranLain.ambilFile().getAbsolutePath(), ais.ui.util.WaktuUtil.getDate(), false);
							if (file != null && file.exists()) {
								ut.addSource(file);
								// Kumpulkan pendamping HTML tiap bagian agar pratinjau bisa tampil HTML.
								File sib = new File(file.getAbsolutePath() + ".html");
								if (sib.exists() && sib.length() > 0) {
									htmlParts.add(sib);
								}
							}

						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						}
					}
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}

			}
			try {
				File filePdfBaru = new File(
						Common.ambilREAL_PATH_REPORT() + "/" + Common.getGeneratedBarCode() + ".pdf");
				ut.setDestinationStream(new FileOutputStream(filePdfBaru));
				ut.mergeDocuments();
				// Satukan pendamping HTML semua bagian → sibling utk berkas merge agar pratinjau surat
				// keluar tampil HTML (mirip PDF) sebagai default. Best-effort (gagal → tetap PDF).
				try {
					Report.gabungHtmlMandiri(htmlParts, new File(filePdfBaru.getAbsolutePath() + ".html"));
				} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/action/master/surat/AlurPersetujuanSuratKeluarStatusAction.java:712");
				}
				CommonReport.tampilkanReportPDF(template, filePdfBaru);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		Tabpanel tabpanelGambar = new ais.ui.util.MyTabpanel();
		tabpanelGambar.setParent(tabpanels);
		tabpanelGambar
				.appendChild(new SuratKeluarPunyaGambarFotoHelper(gridGambar).initDetail(suratKeluar, tampilEdit));

		if (preview) {
			Tabpanel tabpanelDisposisi = new ais.ui.util.MyTabpanel();
			tabpanelDisposisi.setParent(tabpanels);
			if (alurPersetujuanSuratKeluarStatus == null
					|| alurPersetujuanSuratKeluarStatus.getAlurPersetujuanSuratKeluar() == null
					|| !alurPersetujuanSuratKeluarStatus.getAlurPersetujuanSuratKeluar().getHarusMengikutiAlur()) {
				tabpanelDisposisi.appendChild(SuratKeluarAction.initJenisJabatan(suratKeluar, jenisSurats));
			}
		}

	}

	private void preview(AlurPersetujuanSuratKeluarStatus alurPersetujuanSuratKeluarStatus) throws Exception {
		this.alurPersetujuanSuratKeluarStatus = alurPersetujuanSuratKeluarStatus;
		addWindow.setTitle("Preview Surat Keluar");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		SuratKeluar suratKeluar = alurPersetujuanSuratKeluarStatus.getSuratKeluar();

		if (suratKeluar != null) {
			Center center = new Center();
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);
			initDetail(suratKeluar, center, false);
		}

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		borderlayout.setParent(addWindow);
	}

	@SuppressWarnings("deprecation")
	private void init(final AlurPersetujuanSuratKeluarStatus alurPersetujuanSuratKeluarStatus) throws Exception {
		this.suratKeluar = alurPersetujuanSuratKeluarStatus.getSuratKeluar();
		this.alurPersetujuanSuratKeluarStatus = alurPersetujuanSuratKeluarStatus;

		selectedJenisJabatan = new HashSet<JenisJabatan>();
		removedJenisJabatan = new HashSet<JenisJabatan>();
		addWindow.setTitle(alurPersetujuanSuratKeluarStatus.getId() == null ? "Tambah Alur Persetujuan Surat Keluar" : "Ubah Alur Persetujuan Surat Keluar");
		Common.clear(addWindow);

		try {
			jenisSurats = new JSONObject(alurPersetujuanSuratKeluarStatus.getJenisSurats());
		} catch (Exception e) {
			jenisSurats = new JSONObject();
		}

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		SuratKeluar suratKeluar = alurPersetujuanSuratKeluarStatus.getSuratKeluar();

		if (suratKeluar != null) {
			East east = new East();
			east.setWidth("65%");
			east.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(east, true);
			east.setAutoscroll(true);
			initDetail(suratKeluar, east, true);
		}

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("40%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Disposisi oleh"));

		JenisJabatan jenisJabatan = alurPersetujuanSuratKeluarStatus.getAlurPersetujuanSuratKeluar() == null ? null
				: alurPersetujuanSuratKeluarStatus.getJenisJabatan() != null
						? alurPersetujuanSuratKeluarStatus.getJenisJabatan()
						: alurPersetujuanSuratKeluarStatus.getAlurPersetujuanSuratKeluar().getJenisJabatan();

		if (jenisJabatan != null) {
			jenisJabatan = alurPersetujuanSuratKeluarStatus.getJenisJabatan();
		}

		row.appendChild(pejabat = new AmbilDataPejabatBanbox(jenisJabatan));
		if (alurPersetujuanSuratKeluarStatus.getPejabat() != null) {
			pejabat.setAttribute("pejabat", alurPersetujuanSuratKeluarStatus.getPejabat());
			pejabat.setValue(alurPersetujuanSuratKeluarStatus.getPejabat() == null ? ""
					: alurPersetujuanSuratKeluarStatus.getPejabat().toString());
		}
		pejabat.setWidth("90%");

		if (jenisJabatan != null) {
			Pejabat pejabatData = Common.getCurrentPejabat(jenisJabatan);
			if (pejabatData != null) {
				pejabat.setAttribute("pejabat", pejabatData);
				pejabat.setValue(pejabatData.toString());

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig(""));
				Hbox hbox = new Hbox();
				row.appendChild(hbox);

				Vbox vbox1 = new Vbox();
				vbox1.setParent(hbox);

				try {
					CommonMedia.tampilkanGambarKecil(tbmuser).setParent(vbox1);
				} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

				vbox1.appendChild(new Label(tbmuser.getUserNama()));

			}

		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(disetujui = new MyCheckboxConfig(
				"Persetujuan / diterima pejabat \"" + (jenisJabatan == null ? "" : jenisJabatan.getNama()) + "\""));

		selesai = new MyCheckboxConfig("Selesai sampai di sini");

		ditolak = new MyCheckboxConfig(
				"Ditolak pejabat \"" + (jenisJabatan == null ? "" : jenisJabatan.getNama()) + "\"");
		waktuPersetujuan = new MyDatebox(alurPersetujuanSuratKeluarStatus.getWaktuPersetujuan());

		waktuDitolak = new MyDatebox(alurPersetujuanSuratKeluarStatus.getWaktuDitolak());

		disetujui.setChecked(alurPersetujuanSuratKeluarStatus.getDisetujui());

		disetujui.addEventListener("onCheck", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (pejabat.getAttribute("pejabat") == null) {
					MyMessageboxConfig.show("Mohon maaf, Pejabat belum dipilih. Langkah yang dapat dilakukan: (1) pilih pejabat yang akan menyetujui pada kolom Pejabat; (2) pastikan data pejabat tersedia di master data; (3) ulangi proses persetujuan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									disetujui.setChecked(false);
								}

							});
					return;
				}

				if (alurPersetujuanSuratKeluarStatus.getId() != null) {
					alurPersetujuanSuratKeluarStatus.setDisetujui(disetujui.isChecked());
					alurPersetujuanSuratKeluarStatus.setPejabat((Pejabat) pejabat.getAttribute("pejabat"));

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							SuratKeluar suratKeluar = alurPersetujuanSuratKeluarStatus.getSuratKeluar();
							if (suratKeluar != null) {
								Map<String, Object> parameters = SuratUtil.ubahIsiSuratKeluar(suratKeluar);

								PDFMergerUtility ut = new PDFMergerUtility();

								for (int index = 1; index <= 15; index++) {
									try {
										LampiranLain lampiranLain = LampiranLain.ambil(
												suratKeluar.getKlasifikasiSuratKeluar().getId(),
												LampiranLain.FILE_JRXML_LAYOUT_SURAT + (index == 1 ? "" : "_" + index));
										if (lampiranLain != null && lampiranLain.getId() != null) {
											try {

												File file = Report.generateCompileFileReport(Report.PDF, parameters,
														lampiranLain.ambilFile().getAbsolutePath(),
														ais.ui.util.WaktuUtil.getDate(), false);
												ut.addSource(file);

											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
											}
										}
									} catch (Exception e) {
										ais.common.Common.tampilErrorJikaAdmin(e);
									}

								}
								try {
									File filePdfBaru = new File(Common.ambilREAL_PATH_REPORT() + "/"
											+ Common.getGeneratedBarCode() + ".pdf");
									ut.setDestinationStream(new FileOutputStream(filePdfBaru));
									ut.mergeDocuments();
									CommonReport.tampilkanReportPDF(template, filePdfBaru);
								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
								}

							}
						}
					});
				}

				selesai.setDisabled(ditolak.isChecked());
				disetujui.setDisabled(selesai.isChecked());
				ditolak.setDisabled(disetujui.isChecked());
				ditolak.setChecked(!disetujui.isChecked());

				waktuPersetujuan.setDisabled(!disetujui.isChecked());
				alurPersetujuanSuratKeluarStatus.setDisetujui(disetujui.isChecked());
				waktuPersetujuan.setValue(alurPersetujuanSuratKeluarStatus.getWaktuPersetujuan());

				waktuDitolak.setDisabled(!ditolak.isChecked());
				alurPersetujuanSuratKeluarStatus.setDitolak(ditolak.isChecked());
				waktuDitolak.setValue(alurPersetujuanSuratKeluarStatus.getWaktuDitolak());
			}
		});

		row = new MyFormRow();
		row.setVisible(alurPersetujuanSuratKeluarStatus.getAlurPersetujuanSuratKeluar() != null
				&& alurPersetujuanSuratKeluarStatus.getAlurPersetujuanSuratKeluar().getTerdapatPilihanSelesai());
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(selesai);
		selesai.setChecked(alurPersetujuanSuratKeluarStatus.getSelesai());
		selesai.addEventListener("onCheck", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (pejabat.getAttribute("pejabat") == null) {
					MyMessageboxConfig.show("Mohon maaf, Pejabat belum dipilih. Langkah yang dapat dilakukan: (1) pilih pejabat yang akan menyetujui pada kolom Pejabat; (2) pastikan data pejabat tersedia di master data; (3) ulangi proses persetujuan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									selesai.setChecked(false);
								}

							});
					return;
				}

				disetujui.setChecked(true);
				disetujui.setDisabled(selesai.isChecked());
				waktuPersetujuan.setDisabled(!disetujui.isChecked());
				alurPersetujuanSuratKeluarStatus.setDisetujui(disetujui.isChecked());
				waktuPersetujuan.setValue(alurPersetujuanSuratKeluarStatus.getWaktuPersetujuan());

				ditolak.setDisabled(disetujui.isChecked());
				ditolak.setChecked(!disetujui.isChecked());

				waktuPersetujuan.setDisabled(!disetujui.isChecked());
				alurPersetujuanSuratKeluarStatus.setDisetujui(disetujui.isChecked());
				waktuPersetujuan.setValue(alurPersetujuanSuratKeluarStatus.getWaktuPersetujuan());

				waktuDitolak.setDisabled(!ditolak.isChecked());
				alurPersetujuanSuratKeluarStatus.setDitolak(ditolak.isChecked());
				waktuDitolak.setValue(alurPersetujuanSuratKeluarStatus.getWaktuDitolak());

			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal/Waktu Disetujui"));

		if (waktuPersetujuan.getValue() != null && (alurPersetujuanSuratKeluarStatus.getDisetujui() || alurPersetujuanSuratKeluarStatus.getDitolak())) {
			row.appendChild(new Label(Common.dateFormat.get().format(waktuPersetujuan.getValue())));
		} else {
			row.appendChild(waktuPersetujuan);
		}
		
		waktuPersetujuan.setReadonly(true);
		waktuPersetujuan.setFormat(Common.dateFormat.get().toPattern());
		waktuPersetujuan.setDisabled(!disetujui.isChecked());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(ditolak);
		ditolak.setChecked(alurPersetujuanSuratKeluarStatus.getDitolak());

		ditolak.addEventListener("onCheck", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (pejabat.getAttribute("pejabat") == null) {
					MyMessageboxConfig.show("Mohon maaf, Pejabat belum dipilih. Langkah yang dapat dilakukan: (1) pilih pejabat yang akan menyetujui pada kolom Pejabat; (2) pastikan data pejabat tersedia di master data; (3) ulangi proses persetujuan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									disetujui.setChecked(false);
								}

							});
					return;
				}

				if (alurPersetujuanSuratKeluarStatus.getId() != null) {
					alurPersetujuanSuratKeluarStatus.setDitolak(ditolak.isChecked());
					alurPersetujuanSuratKeluarStatus.setPejabat((Pejabat) pejabat.getAttribute("pejabat"));

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							SuratKeluar suratKeluar = alurPersetujuanSuratKeluarStatus.getSuratKeluar();
							if (suratKeluar != null) {
								Map<String, Object> parameters = SuratUtil.ubahIsiSuratKeluar(suratKeluar);

								PDFMergerUtility ut = new PDFMergerUtility();

								for (int index = 1; index <= 15; index++) {
									try {
										LampiranLain lampiranLain = LampiranLain.ambil(
												suratKeluar.getKlasifikasiSuratKeluar().getId(),
												LampiranLain.FILE_JRXML_LAYOUT_SURAT + (index == 1 ? "" : "_" + index));
										if (lampiranLain != null && lampiranLain.getId() != null) {
											try {

												File file = Report.generateCompileFileReport(Report.PDF, parameters,
														lampiranLain.ambilFile().getAbsolutePath(),
														ais.ui.util.WaktuUtil.getDate(), false);
												ut.addSource(file);

											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
											}
										}
									} catch (Exception e) {
										ais.common.Common.tampilErrorJikaAdmin(e);
									}

								}
								try {
									File filePdfBaru = new File(Common.ambilREAL_PATH_REPORT() + "/"
											+ Common.getGeneratedBarCode() + ".pdf");
									ut.setDestinationStream(new FileOutputStream(filePdfBaru));
									ut.mergeDocuments();
									CommonReport.tampilkanReportPDF(template, filePdfBaru);
								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
								}
							}
						}
					});
				}
				disetujui.setDisabled(selesai.isChecked());
				disetujui.setDisabled(ditolak.isChecked());
				disetujui.setChecked(!ditolak.isChecked());
				selesai.setDisabled(ditolak.isChecked());

				waktuPersetujuan.setDisabled(!disetujui.isChecked());
				alurPersetujuanSuratKeluarStatus.setDisetujui(disetujui.isChecked());
				waktuPersetujuan.setValue(alurPersetujuanSuratKeluarStatus.getWaktuPersetujuan());

				waktuDitolak.setDisabled(!ditolak.isChecked());
				alurPersetujuanSuratKeluarStatus.setDitolak(ditolak.isChecked());
				waktuDitolak.setValue(alurPersetujuanSuratKeluarStatus.getWaktuDitolak());
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Alur Surat"));
		row.appendChild(new Label(alurPersetujuanSuratKeluarStatus == null
				|| alurPersetujuanSuratKeluarStatus.getAlurPersetujuanSuratKeluar() == null ? ""
						: alurPersetujuanSuratKeluarStatus.getAlurPersetujuanSuratKeluar().getNama()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal/Waktu Ditolak"));
		row.appendChild(waktuDitolak);
		waktuDitolak.setReadonly(true);
		waktuDitolak.setFormat(Common.dateFormat.get().toPattern());
		waktuDitolak.setDisabled(!ditolak.isChecked());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Uraian *"));
		row.appendChild(keterangan = new Textbox(alurPersetujuanSuratKeluarStatus.getKeterangan() == null ? ""
				: alurPersetujuanSuratKeluarStatus.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(15);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lampiran"));
		Hbox hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, alurPersetujuanSuratKeluarStatus.getId(),
				AlurPersetujuanSuratKeluarStatus.class.getName(), "tindak lanjut Disposisi", false,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lainMahasiswa = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(row);

		Common.initKeterangan(rows, "Jika file lampiran catatan lebih dari satu file, zip dulu semua file tersebut");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(vboxAlur = new Vbox());

		if (alurPersetujuanSuratKeluarStatus.getMasihLanjut()) {
			initKelengkapanBerkas(vboxAlur, alurPersetujuanSuratKeluarStatus.getAlurPersetujuanSuratKeluar());
		}

		ubah = true;
		if (alurPersetujuanSuratKeluarStatus.getId() != null && suratKeluar != null && suratKeluar.getId() != null) {

			Session session = HibernateUtil.currentSession();
			AlurPersetujuanSuratKeluarStatus alurPersetujuanSuratKeluarStatusNext = (AlurPersetujuanSuratKeluarStatus) session
					.createCriteria(AlurPersetujuanSuratKeluarStatus.class).add(Restrictions.isNotNull("kodeUnik"))
					.setMaxResults(1).add(Restrictions.gt("id", alurPersetujuanSuratKeluarStatus.getId()))
					.add(Restrictions.eq("suratKeluar", suratKeluar)).addOrder(Order.asc("id")).uniqueResult();

			if (alurPersetujuanSuratKeluarStatus.getDisetujui() && alurPersetujuanSuratKeluarStatusNext != null
					&& alurPersetujuanSuratKeluarStatusNext.getDisetujui()) {
				Common.freezeGanti(grid, true);
				ubah = false;
			}
		}

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new MyLabelStyled("Informasi Disposisi"));

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		String html = SuratKeluarAction.infoDisposisiBagan(suratKeluar);
		new ais.ui.util.MyHtml(html).setParent(row);

		// Footer aksi: South WAJIB bertinggi tetap (bukan flex) agar tidak kolaps & menumpuk konten
		// form di atasnya. Diberi bingkai atas + latar lembut + tombol rata kanan agar rapi.
		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, false);
		south.setHeight("56px");
		south.setBorder("none");
		south.setStyle("border-top:1px solid #e5e7eb; background:#f8fafc;");
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setWidth("100%");
		toolbar.setStyle("display:flex; gap:10px; align-items:center; justify-content:flex-end;"
				+ " padding:10px 16px; box-sizing:border-box;");
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

					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

		if (!ubah) {
			save.setVisible(false);
			cancel.setLabel("Tutup");
		}

	}

	private void initKelengkapanBerkas(Vbox vbox, AlurPersetujuanSuratKeluar alurPersetujuanSuratKeluar) {
		Common.clear(vbox);
		if (alurPersetujuanSuratKeluar == null || alurPersetujuanSuratKeluar.getId() == null) {
			return;
		}

		final MyGrid subGrid = new MyGrid();
		vbox.appendChild(subGrid);

		Columns subColumns = new Columns();
		subColumns.setParent(subGrid);
		Column c = new Column("Kepada Yth :");
		subColumns.appendChild(c);

		Rows subRows = new Rows();
		subRows.setParent(subGrid);

		MyFormRow subRow = new MyFormRow();
		subRow.setStyle("border:0px;background: transparent;");
		subRow.setParent(subRows);
		subRow.setValign("top");
		Session session = HibernateUtil.currentSession();
		if (alurPersetujuanSuratKeluar.getId() != null) {
			alurPersetujuanSuratKeluar = (AlurPersetujuanSuratKeluar) session
					.createCriteria(AlurPersetujuanSuratKeluar.class)
					.add(Restrictions.idEq(alurPersetujuanSuratKeluar.getId())).uniqueResult();
		}
		selectedJenisJabatan = new HashSet<JenisJabatan>();
		removedJenisJabatan = new HashSet<JenisJabatan>();

		Vbox vboxSkala = new Vbox();
		vboxSkala.setPack("top");
		vboxSkala.setParent(subRow);

		List<JenisJabatan> alurPersetujuanSuratKeluars = ConstantValues.simpleList(session
				.createCriteria(AlurPersetujuanSuratKeluar.class)
				.setProjection(Projections.groupProperty("jenisJabatan.id")).add(Restrictions.isNotNull("jenisJabatan"))
				.add(Restrictions.eq("parent", alurPersetujuanSuratKeluar)), JenisJabatan.class, false);
		System.out.println("alurPersetujuanSuratKeluars -> " + alurPersetujuanSuratKeluars.size());
		for (JenisJabatan jenisJabatan : alurPersetujuanSuratKeluars) {

			new Label(jenisJabatan.getNama()).setParent(vboxSkala);
		}

		TreeMap<String, JenisJabatan> data = new TreeMap<String, JenisJabatan>();
		for (JenisJabatan jenisJabatan : alurPersetujuanSuratKeluar.getJenisJabatans()) {
			data.put(jenisJabatan.getNama(), jenisJabatan);
		}

		for (final JenisJabatan jenisJabatan : data.values()) {

			AlurPersetujuanSuratKeluarStatus alurPersetujuanSuratKeluarStatus = (AlurPersetujuanSuratKeluarStatus) session
					.createCriteria(AlurPersetujuanSuratKeluarStatus.class).add(Restrictions.isNotNull("kodeUnik"))
					.add(Restrictions.eq("alurPersetujuanSuratKeluar", alurPersetujuanSuratKeluar))
					.add(Restrictions.eq("suratKeluar",
							AlurPersetujuanSuratKeluarStatusAction.this.alurPersetujuanSuratKeluarStatus
									.getSuratKeluar()))
					.add(Restrictions.eq("jenisJabatan", jenisJabatan)).setMaxResults(1).uniqueResult();

//			System.out.println("alurPersetujuanSuratKeluarStatus => " + alurPersetujuanSuratKeluarStatus);

			final Checkbox checkbox = new Checkbox(jenisJabatan.getNama());
			checkbox.setParent(vboxSkala);
			checkbox.setChecked(alurPersetujuanSuratKeluarStatus != null);
			checkbox.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						selectedJenisJabatan.add(jenisJabatan);
						removedJenisJabatan.remove(jenisJabatan);
					} else {
						selectedJenisJabatan.remove(jenisJabatan);
						removedJenisJabatan.add(jenisJabatan);
					}
				}
			});

		}
		data = null;
	}

	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {
		if (pejabat.getAttribute("pejabat") == null) {
			MyMessageboxConfig.show("Mohon maaf, Pejabat belum dipilih. Langkah yang dapat dilakukan: (1) pilih pejabat yang akan menyetujui pada kolom Pejabat; (2) pastikan data pejabat tersedia di master data; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (keterangan.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show("Mohon maaf, Uraian Disposisi belum diisi. Langkah yang dapat dilakukan: (1) klik kolom Uraian pada formulir disposisi; (2) isi uraian atau keterangan yang diperlukan secara jelas; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		List<Row> rowsFotoGambar = gridGambar.getRows().getChildren();
		for (Row row : rowsFotoGambar) {
			FotoGambarSuratKeluar fotoGambarSuratKeluar = (FotoGambarSuratKeluar) row
					.getAttribute("fotoGambarSuratKeluar");
			if (fotoGambarSuratKeluar.getSuratKeluar() == null) {
				MyMessageboxConfig.show("Mohon maaf, terdapat baris gambar yang belum diunggah. Langkah yang dapat dilakukan: (1) periksa daftar gambar pada formulir; (2) hapus baris yang kosong atau unggah file gambar yang sesuai; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		Session session = HibernateUtil.currentSession();
		if (alurPersetujuanSuratKeluarStatus.getId() != null) {
			alurPersetujuanSuratKeluarStatus = (AlurPersetujuanSuratKeluarStatus) session
					.load(AlurPersetujuanSuratKeluarStatus.class, alurPersetujuanSuratKeluarStatus.getId());

		}

		// FIX duplicate key "alur_persetujuan_surat_keluar_status_kodeunik_key": getKodeUnik() bersifat
		// TURUNAN (@Column unique) = "P_<pejabat>_<surat>" dst. Saat TAMBAH maupun UBAH pejabat, kombinasi
		// (pejabat, surat) TARGET bisa SUDAH dimiliki baris LAIN -> insert/update menabrak unique constraint.
		// Cari baris existing untuk kodeUnik target; bila ADA & BEDA dari yang sedang diproses (atau entitas
		// masih baru), REUSE baris itu supaya tidak menduplikasi. Bila TIDAK ada, entity yang sedang diproses
		// dipertahankan (baru -> insert; ubah -> update ke kodeUnik baru yang dijamin belum dipakai). Dedup
		// ini menyeragamkan pola yang sudah dipakai di alur jenisSurats/jabatan, sekaligus menutup NPE lama
		// (dulu untuk entitas baru yang tak menemukan kodeUnik, referensi di-set null lalu NPE di setter).
		{
			Pejabat pejabatDipilih = (Pejabat) pejabat.getAttribute("pejabat");
			String kodeUnikTarget = AlurPersetujuanSuratKeluarStatus.kodeUnik(pejabatDipilih, suratKeluar,
					pejabatDipilih == null ? null : pejabatDipilih.getJenisJabatan(), tbmuser,
					tbmuser == null ? null : tbmuser.getMahasiswa(), tbmuser == null ? null : tbmuser.getSiswa());
			System.out.println("suratKeluar -> kodeUnik " + kodeUnikTarget);
			if (kodeUnikTarget != null) {
				AlurPersetujuanSuratKeluarStatus existingKodeUnik = (AlurPersetujuanSuratKeluarStatus) session
						.createCriteria(AlurPersetujuanSuratKeluarStatus.class)
						.add(Restrictions.eq("kodeUnik", kodeUnikTarget)).setMaxResults(1).uniqueResult();
				if (existingKodeUnik != null && (alurPersetujuanSuratKeluarStatus.getId() == null
						|| !existingKodeUnik.getId().equals(alurPersetujuanSuratKeluarStatus.getId()))) {
					alurPersetujuanSuratKeluarStatus = existingKodeUnik;
				}
			}
		}

		alurPersetujuanSuratKeluarStatus.setDisetujui(disetujui.isChecked());
		alurPersetujuanSuratKeluarStatus.setPejabat((Pejabat) pejabat.getAttribute("pejabat"));
		alurPersetujuanSuratKeluarStatus.setKeterangan(keterangan.getValue());
		alurPersetujuanSuratKeluarStatus.setWaktuPersetujuan(waktuPersetujuan.getValue());
		alurPersetujuanSuratKeluarStatus.setJenisSurats(jenisSurats.toString());

		alurPersetujuanSuratKeluarStatus.setWaktuDitolak(waktuDitolak.getValue());
		alurPersetujuanSuratKeluarStatus.setDitolak(ditolak.isChecked());
		alurPersetujuanSuratKeluarStatus.setSelesai(selesai.isChecked());

		if (suratKeluar != null)
			alurPersetujuanSuratKeluarStatus.setSuratKeluar(suratKeluar);

		Common.refreshSaveOrUpdate(session, alurPersetujuanSuratKeluarStatus);

		if (alurPersetujuanSuratKeluarStatus.getDitolak()) {
			DasboardSurat.tolak(session, alurPersetujuanSuratKeluarStatus);
		}

		if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
			try {
				Session sessions = StreamingHibernateUtil.getInstance().currentSession();

				sessions.refresh(lainMahasiswa);
				lainMahasiswa.setRef(alurPersetujuanSuratKeluarStatus.getId());

				sessions.getTransaction().begin();
				sessions.update(lainMahasiswa);
				sessions.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}
		}

		SuratKeluar suratKeluar = alurPersetujuanSuratKeluarStatus.getSuratKeluar();
		if (!selesai.isChecked()) {
			Iterator<String> enumeration = jenisSurats.keys();
			while (enumeration.hasNext()) {
				try {
					Long idJenis = Long.parseLong(enumeration.next());

					if (idJenis != null && !idJenis.equals(-1L)) {
						Pejabat pejabat = (Pejabat) ConstantValues.ambil(Pejabat.class.getName(), idJenis);
						if (pejabat != null) {
							AlurPersetujuanSuratKeluarStatus alurPersetujuanSuratKeluarStatus = (AlurPersetujuanSuratKeluarStatus) session
									.createCriteria(AlurPersetujuanSuratKeluarStatus.class)
									.add(Restrictions.isNotNull("kodeUnik"))
									.add(Restrictions.eq("suratKeluar", suratKeluar))
									.add(Restrictions.eq("pejabat", pejabat))
									.add(Restrictions.eq("jenisJabatan", pejabat.getJenisJabatan())).setMaxResults(1)
									.uniqueResult();

							if (alurPersetujuanSuratKeluarStatus == null) {
								String kodeUnik = AlurPersetujuanSuratKeluarStatus.kodeUnik(pejabat, suratKeluar,
										pejabat.getJenisJabatan(), tbmuser,
										tbmuser == null ? null : tbmuser.getMahasiswa(),
										tbmuser == null ? null : tbmuser.getSiswa());
								if (kodeUnik != null) {
									alurPersetujuanSuratKeluarStatus = (AlurPersetujuanSuratKeluarStatus) session
											.createCriteria(AlurPersetujuanSuratKeluarStatus.class)
											.add(Restrictions.isNotNull("kodeUnik"))
											.add(Restrictions.eq("kodeUnik", kodeUnik)).setMaxResults(1).uniqueResult();
								}
							}

							if (alurPersetujuanSuratKeluarStatus == null && !ditolak.isChecked()
									&& disetujui.isChecked()) {
								alurPersetujuanSuratKeluarStatus = new AlurPersetujuanSuratKeluarStatus();

								alurPersetujuanSuratKeluarStatus.setKonseptor(tbmuser);
								alurPersetujuanSuratKeluarStatus
										.setMahasiswa(tbmuser == null ? null : tbmuser.getMahasiswa());
								alurPersetujuanSuratKeluarStatus.setSiswa(tbmuser == null ? null : tbmuser.getSiswa());

								alurPersetujuanSuratKeluarStatus.setSuratKeluar(suratKeluar);
								alurPersetujuanSuratKeluarStatus.setJenisJabatan(pejabat.getJenisJabatan());
								alurPersetujuanSuratKeluarStatus.setPejabat(pejabat);
								alurPersetujuanSuratKeluarStatus.setJenisSurats(jenisSurats.toString());
								session.save(alurPersetujuanSuratKeluarStatus);
								session.flush();
							}

//						else if (ditolak.isChecked() && alurPersetujuanSuratKeluarStatus != null) {
//							session.delete(alurPersetujuanSuratKeluarStatus);
//							session.flush();
//						}
						}

					}
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
			}

			for (JenisJabatan jenisJabatan : selectedJenisJabatan) {
				List<Pejabat> pejabats = ConstantValues.simpleList(session.createCriteria(Pejabat.class)

						.add(Restrictions.or(
								Restrictions.or(
										Restrictions.ilike("jenisPengguna", "," + tbmuser.hakAkses().getRoleId() + ",",
												MatchMode.ANYWHERE),
										Restrictions.ilike("usernamePengguna", "," + tbmuser.getUserId() + ",",
												MatchMode.ANYWHERE)),
								Restrictions.and(
										Restrictions.or(Restrictions.isNotNull("pegawai"),
												Restrictions.or(Restrictions.isNotNull("guru"),
														Restrictions.isNotNull("dosen"))),
										Restrictions.or(Restrictions.eq("pegawai", tbmuser.getPegawai()),
												Restrictions.or(Restrictions.eq("dosen", tbmuser.getDosen()),
														Restrictions.eq("guru", tbmuser.getGuru()))))))

						.add(Restrictions.eq("jenisJabatan", jenisJabatan))
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.setMaxResults(1), Pejabat.class);

				if (pejabats.isEmpty()) {
					pejabats = ConstantValues.simpleList(
							session.createCriteria(Pejabat.class).add(Restrictions.eq("jenisJabatan", jenisJabatan))
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.setMaxResults(1),
							Pejabat.class);
				}
				for (Pejabat pejabat : pejabats) {

					AlurPersetujuanSuratKeluarStatus alurPersetujuanSuratKeluarStatusLocal = (AlurPersetujuanSuratKeluarStatus) session
							.createCriteria(AlurPersetujuanSuratKeluarStatus.class)
							.add(Restrictions.isNotNull("kodeUnik"))
							.add(Restrictions.eq("suratKeluar", alurPersetujuanSuratKeluarStatus.getSuratKeluar()))
							.add(Restrictions.eq("jenisJabatan", jenisJabatan)).add(Restrictions.eq("pejabat", pejabat))
							.setMaxResults(1).uniqueResult();

					if (alurPersetujuanSuratKeluarStatusLocal == null) {
						String kodeUnik = AlurPersetujuanSuratKeluarStatus.kodeUnik(pejabat, suratKeluar, jenisJabatan,
								tbmuser, tbmuser == null ? null : tbmuser.getMahasiswa(),
								tbmuser == null ? null : tbmuser.getSiswa());
						if (kodeUnik != null) {
							alurPersetujuanSuratKeluarStatusLocal = (AlurPersetujuanSuratKeluarStatus) session
									.createCriteria(AlurPersetujuanSuratKeluarStatus.class)
									.add(Restrictions.isNotNull("kodeUnik")).add(Restrictions.eq("kodeUnik", kodeUnik))
									.setMaxResults(1).uniqueResult();
						}
					}

					if (alurPersetujuanSuratKeluarStatusLocal == null && !ditolak.isChecked()
							&& disetujui.isChecked()) {
						alurPersetujuanSuratKeluarStatusLocal = new AlurPersetujuanSuratKeluarStatus();

						alurPersetujuanSuratKeluarStatusLocal.setKonseptor(tbmuser);
						alurPersetujuanSuratKeluarStatusLocal
								.setMahasiswa(tbmuser == null ? null : tbmuser.getMahasiswa());
						alurPersetujuanSuratKeluarStatusLocal.setSiswa(tbmuser == null ? null : tbmuser.getSiswa());

						alurPersetujuanSuratKeluarStatusLocal
								.setSuratKeluar(alurPersetujuanSuratKeluarStatus.getSuratKeluar());
						alurPersetujuanSuratKeluarStatusLocal.setJenisJabatan(jenisJabatan);
						alurPersetujuanSuratKeluarStatusLocal.setPejabat(pejabat);

						alurPersetujuanSuratKeluarStatusLocal.setMasihLanjut(false);

						session.save(alurPersetujuanSuratKeluarStatusLocal);
						session.flush();
					} else if (ditolak.isChecked() && alurPersetujuanSuratKeluarStatusLocal != null) {
						session.delete(alurPersetujuanSuratKeluarStatusLocal);
						session.flush();
					}

				}
			}

			List<AlurPersetujuanSuratKeluar> alurPersetujuanSuratKeluarsNext = ConstantValues.simpleList(
					session.createCriteria(AlurPersetujuanSuratKeluar.class).add(Restrictions.isNotNull("jenisJabatan"))
							.add(Restrictions.eq("parent",
									alurPersetujuanSuratKeluarStatus.getAlurPersetujuanSuratKeluar())),
					AlurPersetujuanSuratKeluar.class);

			for (AlurPersetujuanSuratKeluar alurPersetujuanSuratKeluar : alurPersetujuanSuratKeluarsNext) {
				JenisJabatan jenisJabatan = alurPersetujuanSuratKeluar.getJenisJabatan();

				List<Pejabat> pejabats = ConstantValues.simpleList(
						session.createCriteria(Pejabat.class)
								.add(Restrictions.or(Restrictions.isNotNull("pegawai"),
										Restrictions.or(Restrictions.isNotNull("guru"),
												Restrictions.isNotNull("dosen"))))
								.add(tbmuser.getDosen() != null ? Restrictions.eq("dosen", tbmuser.getDosen())
										: Restrictions.sqlRestriction("true"))

								.add(tbmuser.getGuru() != null ? Restrictions.eq("guru", tbmuser.getGuru())
										: Restrictions.sqlRestriction("true"))

								.add(tbmuser.getPegawai() != null ? Restrictions.eq("pegawai", tbmuser.getPegawai())
										: Restrictions.sqlRestriction("true"))

								.add(Restrictions.eq("jenisJabatan", jenisJabatan))
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
						Pejabat.class);

				if (pejabats.isEmpty()) {
					pejabats =

							ConstantValues.simpleList(
									session.createCriteria(Pejabat.class)
											.add(Restrictions.eq("jenisJabatan", jenisJabatan)).add(Restrictions
													.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
									Pejabat.class);
				}
				for (Pejabat pejabat : pejabats) {

					AlurPersetujuanSuratKeluarStatus alurPersetujuanSuratKeluarStatusLocal = (AlurPersetujuanSuratKeluarStatus) session
							.createCriteria(AlurPersetujuanSuratKeluarStatus.class)
							.add(Restrictions.isNotNull("kodeUnik"))
							.add(Restrictions.eq("alurPersetujuanSuratKeluar", alurPersetujuanSuratKeluar))
							.add(Restrictions.eq("suratKeluar", alurPersetujuanSuratKeluarStatus.getSuratKeluar()))
							.add(Restrictions.eq("jenisJabatan", jenisJabatan)).add(Restrictions.eq("pejabat", pejabat))
							.setMaxResults(1).uniqueResult();

					if (alurPersetujuanSuratKeluarStatusLocal == null) {
						String kodeUnik = AlurPersetujuanSuratKeluarStatus.kodeUnik(pejabat, suratKeluar, jenisJabatan,
								tbmuser, tbmuser == null ? null : tbmuser.getMahasiswa(),
								tbmuser == null ? null : tbmuser.getSiswa());
						if (kodeUnik != null) {
							alurPersetujuanSuratKeluarStatusLocal = (AlurPersetujuanSuratKeluarStatus) session
									.createCriteria(AlurPersetujuanSuratKeluarStatus.class)
									.add(Restrictions.isNotNull("kodeUnik")).add(Restrictions.eq("kodeUnik", kodeUnik))
									.setMaxResults(1).uniqueResult();
						}
					}

					if (alurPersetujuanSuratKeluarStatusLocal == null && !ditolak.isChecked()
							&& disetujui.isChecked()) {
						alurPersetujuanSuratKeluarStatusLocal = new AlurPersetujuanSuratKeluarStatus();

						alurPersetujuanSuratKeluarStatusLocal.setKonseptor(tbmuser);
						alurPersetujuanSuratKeluarStatusLocal
								.setMahasiswa(tbmuser == null ? null : tbmuser.getMahasiswa());
						alurPersetujuanSuratKeluarStatusLocal.setSiswa(tbmuser == null ? null : tbmuser.getSiswa());

						alurPersetujuanSuratKeluarStatusLocal.setAlurPersetujuanSuratKeluar(alurPersetujuanSuratKeluar);
						alurPersetujuanSuratKeluarStatusLocal
								.setSuratKeluar(alurPersetujuanSuratKeluarStatus.getSuratKeluar());
						alurPersetujuanSuratKeluarStatusLocal.setJenisJabatan(jenisJabatan);
						alurPersetujuanSuratKeluarStatusLocal.setPejabat(pejabat);

						alurPersetujuanSuratKeluarStatusLocal.setMasihLanjut(true);
						session.save(alurPersetujuanSuratKeluarStatusLocal);
						session.flush();
					} else if (ditolak.isChecked() && alurPersetujuanSuratKeluarStatusLocal != null) {
						session.delete(alurPersetujuanSuratKeluarStatusLocal);
						session.flush();
					}

				}

			}

			if (removedJenisJabatan != null) {
				for (JenisJabatan jenisJabatan : removedJenisJabatan) {
					AlurPersetujuanSuratKeluarStatus alurPersetujuanSuratKeluarStatusLocal = (AlurPersetujuanSuratKeluarStatus) session
							.createCriteria(AlurPersetujuanSuratKeluarStatus.class)
							.add(Restrictions.isNotNull("kodeUnik"))
							.add(Restrictions.eq("alurPersetujuanSuratKeluar",
									alurPersetujuanSuratKeluarStatus.getAlurPersetujuanSuratKeluar()))
							.add(Restrictions.eq("suratKeluar", alurPersetujuanSuratKeluarStatus.getSuratKeluar()))
							.add(Restrictions.eq("jenisJabatan", jenisJabatan)).setMaxResults(1).uniqueResult();
					if (alurPersetujuanSuratKeluarStatusLocal != null) {
						session.delete(alurPersetujuanSuratKeluarStatusLocal);
					}

				}
			}

			session.flush();

			if (disetujui.isChecked()) {

				AlurPersetujuanSuratMasuk alurPersetujuanSuratMasuk = alurPersetujuanSuratKeluarStatus
						.getAlurPersetujuanSuratKeluar() == null ? null
								: alurPersetujuanSuratKeluarStatus.getAlurPersetujuanSuratKeluar()
										.getAlurPersetujuanSuratMasuk();

				if (alurPersetujuanSuratMasuk != null) {

					SuratMasuk suratMasuk = (SuratMasuk) session.createCriteria(SuratMasuk.class)
							.add(Restrictions.eq("suratKeluar", alurPersetujuanSuratKeluarStatus.getSuratKeluar()))
							.setMaxResults(1).uniqueResult();
					if (suratMasuk == null) {
						suratMasuk = new SuratMasuk();
						suratMasuk.setAlurPersetujuanSuratMasuk(alurPersetujuanSuratMasuk);
						suratMasuk.setAsal("Surat keluar dengan nomor "
								+ alurPersetujuanSuratKeluarStatus.getSuratKeluar().getKode());
						suratMasuk.setFakultas(alurPersetujuanSuratKeluarStatus.getSuratKeluar().getFakultas());
						suratMasuk.setJurusan(alurPersetujuanSuratKeluarStatus.getSuratKeluar().getJurusan());
						suratMasuk.setSuratKeluar(alurPersetujuanSuratKeluarStatus.getSuratKeluar());
						suratMasuk.setKlasifikasiSuratMasuk(alurPersetujuanSuratKeluarStatus
								.getAlurPersetujuanSuratKeluar().getKlasifikasiSuratMasuk());
						suratMasuk.setSatuanKerja(alurPersetujuanSuratKeluarStatus.getSuratKeluar().getSatuanKerja());
						suratMasuk.setSekolah(alurPersetujuanSuratKeluarStatus.getSuratKeluar().getSekolah());
						suratMasuk.setTanggal(alurPersetujuanSuratKeluarStatus.getWaktuDitolak());
						suratMasuk.setYayasan(alurPersetujuanSuratKeluarStatus.getSuratKeluar().getYayasan());
						suratMasuk.setKode(alurPersetujuanSuratKeluarStatus.getSuratKeluar().getAgenda());
						session.save(suratMasuk);
						session.flush();
					}

					JenisJabatan jenisJabatan = alurPersetujuanSuratMasuk.getJenisJabatan();

					AlurPersetujuanSuratMasukStatus alurPersetujuanSuratMasukStatusBaru = (AlurPersetujuanSuratMasukStatus) session
							.createCriteria(AlurPersetujuanSuratMasukStatus.class)
							.add(Restrictions.isNotNull("kodeUnik")).add(Restrictions.isNotNull("jenisJabatan"))
							.add(Restrictions.eq("alurPersetujuanSuratMasuk", alurPersetujuanSuratMasuk))
							.add(Restrictions.eq("suratMasuk", suratMasuk))
							.add(Restrictions.eq("jenisJabatan", jenisJabatan)).setMaxResults(1).uniqueResult();

					if (alurPersetujuanSuratMasukStatusBaru == null) {
						String kodeUnik = AlurPersetujuanSuratMasukStatus.kodeUnik(null, suratMasuk, jenisJabatan,
								tbmuser, tbmuser == null ? null : tbmuser.getMahasiswa(),
								tbmuser == null ? null : tbmuser.getSiswa());
						if (kodeUnik != null) {
							alurPersetujuanSuratMasukStatusBaru = (AlurPersetujuanSuratMasukStatus) session
									.createCriteria(AlurPersetujuanSuratMasukStatus.class)
									.add(Restrictions.isNotNull("kodeUnik")).add(Restrictions.eq("kodeUnik", kodeUnik))
									.setMaxResults(1).uniqueResult();
						}
					}

					if (alurPersetujuanSuratMasukStatusBaru == null) {
						alurPersetujuanSuratMasukStatusBaru = new AlurPersetujuanSuratMasukStatus();
					}

					alurPersetujuanSuratMasukStatusBaru.setKonseptor(tbmuser);
					alurPersetujuanSuratMasukStatusBaru.setMahasiswa(tbmuser == null ? null : tbmuser.getMahasiswa());
					alurPersetujuanSuratMasukStatusBaru.setSiswa(tbmuser == null ? null : tbmuser.getSiswa());

					alurPersetujuanSuratMasukStatusBaru.setAlurPersetujuanSuratMasuk(alurPersetujuanSuratMasuk);
					alurPersetujuanSuratMasukStatusBaru.setSuratMasuk(suratMasuk);
					alurPersetujuanSuratMasukStatusBaru.setJenisJabatan(alurPersetujuanSuratMasuk.getJenisJabatan());

					if (alurPersetujuanSuratMasukStatusBaru.getAlurPersetujuanSuratMasuk().getJenisJabatan() != null
							&& jenisJabatan != null && jenisJabatan.getId().equals(alurPersetujuanSuratMasukStatusBaru
									.getAlurPersetujuanSuratMasuk().getJenisJabatan().getId())) {
						alurPersetujuanSuratMasukStatusBaru.setMasihLanjut(true);
					} else {
						alurPersetujuanSuratMasukStatusBaru.setMasihLanjut(false);
					}

					Common.refreshSaveOrUpdate(session, alurPersetujuanSuratMasukStatusBaru);

				}
			}
		} else if (selesai.isChecked()) {

			String sql = "delete from surat.alur_persetujuan_surat_keluar_status where id>"
					+ alurPersetujuanSuratKeluarStatus.getId() + " and surat_keluar=" + suratKeluar.getId();

			int i = session.createSQLQuery(sql).executeUpdate();

			System.out.println(sql + " -> " + i);
		}

		Session mysession = StreamingHibernateUtil.getInstance().currentSession();
		try {
			mysession.getTransaction().begin();
			for (Row row : rowsFotoGambar) {
				FotoGambarSuratKeluar fotoGambarSuratKeluar = (FotoGambarSuratKeluar) row
						.getAttribute("fotoGambarSuratKeluar");
				if (fotoGambarSuratKeluar.getId() == null || fotoGambarSuratKeluar.getSuratKeluar() == null
						|| !fotoGambarSuratKeluar.getSuratKeluar()
								.equals(alurPersetujuanSuratKeluarStatus.getSuratKeluar().getId())) {
					fotoGambarSuratKeluar.setSuratKeluar(alurPersetujuanSuratKeluarStatus.getSuratKeluar().getId());
					mysession.saveOrUpdate(fotoGambarSuratKeluar);
				}
			}
			mysession.getTransaction().commit();
		} catch (Exception e) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			Common.tampilErrorJikaAdmin(e);
		}

		StreamingHibernateUtil.getInstance().closeSession();

		Map<String, Object> parameters = SuratUtil.ubahIsiSuratKeluar(suratKeluar, null);
		SuratKeluarAction.cetakDisposisi(parameters, alurPersetujuanSuratKeluarStatus, tbmuser);

		Common.createDefaultTimerNoBusy((new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				BroadcastHelper.kirimEmailSuratKeluar(alurPersetujuanSuratKeluarStatus.getSuratKeluar(),
						alurPersetujuanSuratKeluarStatus, tbmuser);
			}
		}));

		return true;
	}

	public Criteria initCriteria(boolean order) {

		SatuanKerja parent = (SatuanKerja) (searchparent == null ? null : searchparent.getAttribute("satuanKerja"));
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		JenisJabatan jenisJabatan = (JenisJabatan) (searchjenisjabatan == null
				|| searchjenisjabatan.getSelectedItem() == null ? null
						: searchjenisjabatan.getSelectedItem().getValue());

		// Null-safe untuk komponen filter yang mungkin belum ter-compose (autowire null),
		// agar initCriteria tidak melempar NullPointerException saat onSearchDefault.
		boolean blmDisetujuiCk = blmDisetujui != null && blmDisetujui.isChecked();
		boolean telahDisetujuiCk = telahDisetujui != null && telahDisetujui.isChecked();
		boolean belumSayaAjukanCk = searchbelumsayaajukan != null && searchbelumsayaajukan.isChecked();
		String namaVal = searchnama == null || searchnama.getValue() == null ? "" : searchnama.getValue().trim();
		String kodeVal = searchkode == null || searchkode.getValue() == null ? "" : searchkode.getValue().trim();

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(AlurPersetujuanSuratKeluarStatus.class)
				.add(Restrictions.isNotNull("kodeUnik"));

		Tbmrole tbmrole = tbmuser == null ? null : tbmuser.hakAkses();

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria

				.add(!blmDisetujuiCk && !telahDisetujuiCk ? Restrictions.sqlRestriction("false")
						: blmDisetujuiCk && telahDisetujuiCk ? Restrictions.sqlRestriction("true")
								: telahDisetujuiCk ? Restrictions.eq("disetujui", true)
										: blmDisetujuiCk ? Restrictions.eq("disetujui", false)
												: Restrictions.sqlRestriction("true"))

				.createAlias("suratKeluar", "suratKeluar")

				.add(Restrictions.or(Restrictions.isNull("suratKeluar.satuanKerja"),
						satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
								: Restrictions.in("suratKeluar.satuanKerja", satuanKerjas)))

				.add(!belumSayaAjukanCk
						? (jenisJabatan == null || (tbmrole != null && tbmrole.getMelihatSemuaSurat())
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("jenisJabatan", jenisJabatan))
						: tbmrole != null && tbmrole.getRoleId() != null && !tbmrole.getMelihatSemuaSurat() ?

								Restrictions.eq("suratKeluar.konseptor", tbmuser) : Restrictions.sqlRestriction("true"))

				.add(!belumSayaAjukanCk
						? (jenisJabatans == null || jenisJabatans.isEmpty()
								|| (tbmrole != null && tbmrole.getMelihatSemuaSurat())
										? Restrictions.sqlRestriction("1=1")
										: Restrictions.in("jenisJabatan", jenisJabatans))
						: tbmrole != null && tbmrole.getRoleId() != null && !tbmrole.getMelihatSemuaSurat()
								? Restrictions.eq("suratKeluar.konseptor", tbmuser)
								: Restrictions.sqlRestriction("true"))

				.add(namaVal.equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								Restrictions.ilike("suratKeluar.nama", namaVal,
										MatchMode.ANYWHERE),
								Restrictions.ilike("suratKeluar.perihal", namaVal,
										MatchMode.ANYWHERE)))

				.add(kodeVal.equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("suratKeluar.kode", kodeVal, MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		if (searchjenisjabatan == null) {
			return;
		}

		Common.initPaging(initCriteria(false), paging);

		List<AlurPersetujuanSuratKeluarStatus> alurPersetujuanSuratKeluarStatus = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(alurPersetujuanSuratKeluarStatus);
		grid.setRowRenderer(new AlurPersetujuanSuratKeluarStatusRenderer());
		if (ubahLangsungA) {
			grid.setModel(strset);
		} else {
			grid.setModelCheckMobile(strset);
		}

	}

}
