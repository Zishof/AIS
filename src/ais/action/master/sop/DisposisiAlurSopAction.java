package ais.action.master.sop;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Caption;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.BroadcastHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.sop.helper.ParameterTambahanDisposisiAlurSopListener;
import ais.action.master.sop.helper.SopUtil;
import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.database.model.ParameterTambahan;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.UangMuka;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.LampiranLain;
import ais.database.model.sop.AlurSop;
import ais.database.model.sop.AktorSop;
import ais.database.model.sop.DisposisiAlurSop;
import ais.database.model.sop.DisposisiSop;
import ais.database.model.sop.DokumenAlurSop;
import ais.database.model.sop.KelompokParameterTambahanAlurSop;
import ais.database.model.sop.ParameterTambahanAlurSop;
import ais.database.model.sop.Sop;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.FormSop;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelAgakKecilBoldBiru;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyLabelStyled;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

public class DisposisiAlurSopAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox keterangan;

	private DisposisiAlurSop disposisiAlurSop;
	private MyToolbarbuttonConfig find;

	private MyDatebox waktu;
	protected Map<Long, LampiranLain> lainMahasiswa;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Combobox searchyayasan;
	private Combobox searchsekolah;

	private Hbox hbFakultasLabel;
	private Hbox hbFakultas;

	private Hbox hbYayasanLabel;
	private Hbox hbYayasan;
	private Tbmuser tbmuser;
	private EventListener eventListener = null;
	private AlurSop alurSop;
	private DisposisiSop disposisiSop;
	private ArrayList<Row> parameterRows;
	private HashMap<String, LampiranLain> lampiranLains;
	private ParameterTambahanDisposisiAlurSopListener parameterTambahanListener;
	private long ref;

	private MyDatebox start;
	private MyDatebox end;

	private Radiogroup radiogroup = null;
	private Boolean setujuiData = null;
	private Boolean kembaliData = null;
	private Vbox vboxPilihan = null;
	private List<AlurSop> alurSops = null;
	private boolean editPilihan;
	private Set<DokumenAlurSop> dokumen;
	private MyDatebox waktuMaksimal;
	protected LampiranLain lampiranCatatanDisposisi = null;

	private FormSop formSop = null;
	private List<Long> idsSelected = null;
	private Row rowProses;
	private Row rowBerikutnya;
	private ArrayList<AlurSop> selanjutnya;
	private boolean ubah = true;
	private MyGrid griddata;
	private List<String> opsiAlurSops;
	private Hbox hboxAktor;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();
		tbmuser = Common.getCurrentUser();


		if (start != null) start.setReadonly(true);
		if (end != null) end.setReadonly(true);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 1);
		if (start != null) start.setValue(calendar.getTime());
		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
		if (end != null) end.setValue(calendar.getTime());

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah, true, false);

		if (searchyayasan != null) {
			searchyayasan.getParent().setVisible(Common.bolehKonfigurasi("user_yayasan", Konfigurasi.TIDAK_AKTIF));
		}
		if (hbFakultasLabel != null) {
			hbFakultasLabel.setVisible(Common.bolehKonfigurasi("user_fakultas"));
		}
		if (hbFakultas != null) {
			hbFakultas.setVisible(Common.bolehKonfigurasi("user_fakultas"));
		}
		if (hbYayasanLabel != null) {
			hbYayasanLabel.setVisible(Common.bolehKonfigurasi("user_yayasan", Konfigurasi.TIDAK_AKTIF));
		}
		if (hbYayasan != null) {
			hbYayasan.setVisible(Common.bolehKonfigurasi("user_yayasan", Konfigurasi.TIDAK_AKTIF));
		}

		String[] contents = new String[] { "diajukanOleh.userNama", "mahasiswa.nama", "siswa.nama", "waktu",
				"waktuMaksimal", "keterangan", "alurSop.kode", "alurSop.nama", "alurSop.sop.kode", "alurSop.sop.nama",
				"alurSop.sop.jurusan", "alurSop.sop.fakultas", "alurSop.sop.yayasan", "alurSop.sop.sekolah",
				"alurSop.sop.satuanKerja", "keyword", "sebelumnya", "setelahnya", "properti" };

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(DisposisiAlurSop.class, this, contents);
		if (find != null && find.getParent() != null) {
			Common.appendKeToolbar(cetakToolbarbutton, find, comp);
		}
	        FilterLanjutHelper.setup(comp);
}

	// =======================================================================================================
	// HELPER UI: Menghindari Kode Berulang untuk Render Aktor (Siswa/Mahasiswa/User)
	// =======================================================================================================
	private void renderAktorUI(Tbmuser user, ais.database.model.Mahasiswa mhs, ais.database.model.sekolah.Siswa siswa, Component parentContainer) {
		Vbox vbox1 = new Vbox();
		vbox1.setParent(parentContainer);
		try {
			if (user != null) {
				CommonMedia.tampilkanGambarKecil(user).setParent(vbox1);
				vbox1.appendChild(new Label(user.getUserNama()));
			} else if (mhs != null) {
				CommonMedia.tampilkanGambarKecil(mhs).setParent(vbox1);
				vbox1.appendChild(new Label(mhs.getNama()));
			} else if (siswa != null) {
				CommonMedia.tampilkanGambarKecil(siswa).setParent(vbox1);
				vbox1.appendChild(new Label(siswa.getNama()));
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		if (parentContainer.getParent() != null) {
			parentContainer.getParent().setVisible(true);
		}
	}

	class DisposisiAlurSopRenderer extends ais.ui.util.MyRowRenderer {
		@SuppressWarnings({ "unchecked", "deprecation" })
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");

			MyDetail detail = new MyDetail();
			arg0.appendChild(detail);

			final DisposisiAlurSop disposisiAlurSop = (DisposisiAlurSop) arg1;

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			vbox.setWidth("100%");

			Tbmuser tbmuser = disposisiAlurSop.getDiajukanOleh();
			if (tbmuser != null) {
				if (disposisiAlurSop.getMahasiswa() != null) {
					tbmuser = new Tbmuser(disposisiAlurSop.getMahasiswa());
				} else if (disposisiAlurSop.getSiswa() != null) {
					tbmuser = new Tbmuser(disposisiAlurSop.getSiswa());
				}
				try {
					CommonMedia.tampilkanGambarKecil(tbmuser).setParent(vbox);
				} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
				vbox.appendChild(new Label(tbmuser.getUserNama()));
			}

			if (disposisiAlurSop.getAlurSop() != null) {
				vbox.appendChild(new Label(disposisiAlurSop.getAlurSop().getAktor()));

				Vbox a;
				(a = RevisiHelper.createNewRevisi(DisposisiAlurSop.class, disposisiAlurSop, disposisiAlurSop.getAlurSop().getSop().getNama())).setParent(arg0);

				new Label(disposisiAlurSop.getAlurSop().getSop().getKode()).setParent(a);

				String properti = disposisiAlurSop.getProperti();
				if (properti != null && !properti.trim().isEmpty()) {
					try {
						JSONObject jsonObject = new JSONObject(properti);
						Iterator<String> keys = jsonObject.keys();
						if (keys.hasNext()) {
							String key = keys.next();
							jsonObject = jsonObject.getJSONObject(key);
							if (jsonObject != null && !jsonObject.isNull("kode")) {
								String kode = jsonObject.get("kode") + "";
								if (!kode.isEmpty()) {
									a.appendChild(new MyLabelBold(kode));
								}
							}
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/DisposisiAlurSopAction.java:292");
//						ais.common.Common.tampilErrorJikaAdmin(e);
					}
				}

				Vbox myvbox = new Vbox();
				myvbox.setParent(a);

				AlurSop alurSop = disposisiAlurSop.getAlurSop();

				Session session = null;
				try {
					session = HibernateUtil.getSessionFactory().openSession();
					alurSop = (AlurSop) session.get(AlurSop.class, alurSop.getId()); // Refresh aman

					Set<KelompokParameterTambahanAlurSop> kelompokParameterTambahanAlurSops = alurSop.getKelompokParameterTambahanAlurSops();

					Groupbox groupbox = new Groupbox();
					groupbox.setParent(detail);
					groupbox.appendChild(new Caption("Parameter, Dokumen dan Aktor"));

					if (!kelompokParameterTambahanAlurSops.isEmpty()) {
						Grid parameterGrid = new Grid();
						parameterGrid.setMold("paging");
						parameterGrid.setParent(groupbox);
						parameterGrid.setSclass("fgrid");

						Columns columnsparameter = new Columns();
						columnsparameter.setParent(parameterGrid);

						MyColumnConfig columnparameter = new MyColumnConfig("");
						columnparameter.setParent(columnsparameter);
						columnparameter.setWidth("30%");

						columnparameter = new MyColumnConfig("");
						columnparameter.setParent(columnsparameter);

						Rows rowsParameter = new Rows();
						rowsParameter.setParent(parameterGrid);

						for (KelompokParameterTambahanAlurSop kelompokParameterTambahanAlurSop : kelompokParameterTambahanAlurSops) {
							MyFormRow rowParameterTambahan = new MyFormRow();
							rowParameterTambahan.setVisible(false);
							rowParameterTambahan.setParent(rowsParameter);
							ais.ui.util.ZkCompat.setSpans(rowParameterTambahan, "2");
							rowParameterTambahan.appendChild(new MyLabelStyled(kelompokParameterTambahanAlurSop.getNama() + ""));

							List<ParameterTambahan> parameterTambahans = ConstantValues.simpleList(
									session.createCriteria(ParameterTambahanAlurSop.class)
											.add(Restrictions.eq("kelompokParameterTambahanAlurSop", kelompokParameterTambahanAlurSop))
											.createAlias("parameterTambahan", "parameterTambahan")
											.createAlias("kelompokParameterTambahanAlurSop", "kelompokParameterTambahanAlurSop")
											.add(Restrictions.eq("parameterTambahan.aktif", true))
											.add(Restrictions.eq("kelompokParameterTambahanAlurSop.aktif", true))
											.setProjection(Projections.groupProperty("parameterTambahan.id")),
									ParameterTambahan.class, false);
							Collections.sort(parameterTambahans);

							rowParameterTambahan.setVisible(!parameterTambahans.isEmpty());
							if (!parameterTambahans.isEmpty()) {
								for (final ParameterTambahan parameterTambahan : parameterTambahans) {
									String jenis = kelompokParameterTambahanAlurSop.getId() + "->" + parameterTambahan.getId();
									MyFormRow rowParameter = new MyFormRow();
									rowParameter.setValign("top");
									rowParameter.setParent(rowsParameter);
									rowParameter.appendChild(new Label(parameterTambahan.getLabelInputan() + (parameterTambahan.getWajibDiisi() ? " (*)" : " ")));

									String val = "";
									if (disposisiAlurSop.getParameterTambahanInds() != null) {
										String[] spl = disposisiAlurSop.getParameterTambahanInds().split("\n");
										for (String d : spl) {
											String[] value = d.split("<=>");
											if (value.length > 0 && value[0].trim().equalsIgnoreCase(jenis)) {
												val = value.length > 1 ? value[1].trim() : "";
											}
										}
									}

									String[] ss = val.split("->");
									if (ss.length > 1) {
										val = ss[1];
									}

									rowParameterTambahan.setVisible(!val.isEmpty());

									if (parameterTambahan.getHarusMenyertakanLampiran()) {
										Vbox vbox1 = new Vbox();
										rowParameter.appendChild(vbox1);
										vbox1.appendChild(new Label(val));

										Hbox hbox = new Hbox();
										hbox.setWidth("100%");

										LampiranLain.createDownloadUploadFileLain(hbox,
												disposisiAlurSop.getId() == null ? -Common.randLong() : disposisiAlurSop.getId(),
												jenis,
												parameterTambahan.getLabelInputan() + (parameterTambahan.getLampiranWajibDiisi() ? " (*)" : " "),
												false, new EventListener() {
													@Override
													public void onEvent(Event arg0) throws Exception {}
												}, null, false, false, false, Common.getApakahAdmin(), null);
										hbox.setParent(vbox1);
									} else {
										rowParameter.appendChild(new Label(val));
									}
								}
							}
						}
					}

					groupbox.appendChild(new MyLabelStyled("Dokumen"));

					Grid dokumenGrid = new Grid();
					dokumenGrid.setMold("paging");
					dokumenGrid.setParent(groupbox);
					dokumenGrid.setSclass("fgrid");

					Columns columnsdokumen = new Columns();
					columnsdokumen.setParent(dokumenGrid);

					MyColumnConfig columndokumen = new MyColumnConfig("Kode");
					columndokumen.setParent(columnsdokumen);
					columndokumen.setWidth("10%");

					columndokumen = new MyColumnConfig("Nama Dokumen");
					columndokumen.setParent(columnsdokumen);

					Rows rowsdokumen = new Rows();
					rowsdokumen.setParent(dokumenGrid);

					Set<DokumenAlurSop> dokumenList = alurSop.getDokumenAlurSops();
					if (!dokumenList.isEmpty()) {
						detail.setOpen(true);

						for (DokumenAlurSop dokumenAlurSop : dokumenList) {
							if (dokumenAlurSop.getAktif()) {
								MyFormRow rowdokumen = new MyFormRow();
								rowdokumen.setValign("top");
								rowdokumen.setParent(rowsdokumen);
								rowdokumen.appendChild(new Label(dokumenAlurSop.getKode()));

								final LampiranLain lampiranLain;
								if (disposisiAlurSop.getAlurSop().getStart()) {
									lampiranLain = LampiranLain.ambil(disposisiAlurSop.getDisposisiSop().getId(), DokumenAlurSop.class.getName() + "_" + dokumenAlurSop.getId());
								} else {
									lampiranLain = LampiranLain.ambil(disposisiAlurSop.getId(), DokumenAlurSop.class.getName() + "_alur_" + dokumenAlurSop.getId());
								}

								if (lampiranLain != null) {
									A aa = new A(dokumenAlurSop.getNama() + (" -> " + lampiranLain.getNama()));
									aa.setParent(rowdokumen);
									aa.setWidth("95%");
									aa.addEventListener("onClick", new EventListener() {
										@Override
										public void onEvent(Event arg0) throws Exception {
											Common.display(lampiranLain);
										}
									});
								} else {
									rowdokumen.appendChild(new Label(dokumenAlurSop.getNama()));
								}
							}
						}
					}
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				} finally {
					if (session != null) {
						try { session.clear(); session.disconnect(); session.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sop/DisposisiAlurSopAction.java:460");}
					}
				}

				Vbox vbox2 = new Vbox();
				vbox2.setParent(arg0);

				Label l = new Label(disposisiAlurSop.getAlurSop().getSop().getJenisSop().getNama());
				l.setParent(arg0);
				l.setStyle("background-color:" + disposisiAlurSop.getAlurSop().getSop().getJenisSop().getWarna() + ";color:" + disposisiAlurSop.getAlurSop().getSop().getJenisSop().getWarnatext() + ";");
				new Label(disposisiAlurSop.getWaktu() == null ? "" : Common.dateFormat5.get().format(disposisiAlurSop.getWaktu())).setParent(vbox2);
				new Label(disposisiAlurSop.getKeterangan()).setParent(vbox2);

				vbox2 = new Vbox();
				vbox2.setParent(arg0);
				new Label(disposisiAlurSop.getAlurSop().getKode()).setParent(vbox2);
				new Label(disposisiAlurSop.getAlurSop().getNama()).setParent(vbox2);

				vbox2 = new Vbox();
				vbox2.setParent(arg0);

				if (disposisiAlurSop.getSebelumnya() != null && disposisiAlurSop.getSebelumnya().getAlurSop() != null) {
					new MyLabelAgakKecilBoldBiru("Sebelumya").setParent(vbox2);
					new MyLabelAgakKecil(disposisiAlurSop.getSebelumnya().getAlurSop().getKode()).setParent(vbox2);
					new MyLabelAgakKecil(disposisiAlurSop.getSebelumnya().getAlurSop().getNama()).setParent(vbox2);
					new MyLabelAgakKecil(disposisiAlurSop.getSebelumnya().getAlurSop().getOpsi()).setParent(vbox2);
					new MyLabelAgakKecil(disposisiAlurSop.getSebelumnya().getKeterangan()).setParent(vbox2);
				}

				if (disposisiAlurSop.getSetelahnya() != null && disposisiAlurSop.getSetelahnya().getAlurSop() != null) {
					new MyLabelAgakKecilBoldBiru("Setelahnya").setParent(vbox2);
					new MyLabelAgakKecil(disposisiAlurSop.getSetelahnya().getAlurSop().getKode()).setParent(vbox2);
					new MyLabelAgakKecil(disposisiAlurSop.getSetelahnya().getAlurSop().getNama()).setParent(vbox2);
					new MyLabelAgakKecil(disposisiAlurSop.getSetelahnya().getAlurSop().getOpsi()).setParent(vbox2);
					new MyLabelAgakKecil(disposisiAlurSop.getSetelahnya().getKeterangan()).setParent(vbox2);
				}
			}
		}
	}

	public static void onAddExternal(EventListener eventListener, DisposisiAlurSop disposisiAlurSop, AlurSop alurSop,
			DisposisiSop disposisiSop, Set<DokumenAlurSop> dokumen, boolean editPilihan, boolean ubah)
			throws Exception {
		DisposisiAlurSopAction disposisiAlurSopAction = new DisposisiAlurSopAction();
		disposisiAlurSopAction.eventListener = eventListener;
		disposisiAlurSopAction.addWindow = new MyWindow();
		disposisiAlurSopAction.ubah = ubah;
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(disposisiAlurSopAction.addWindow);
		disposisiAlurSopAction.addWindow.setHeight("99%");
		disposisiAlurSopAction.addWindow.setWidth("850px");

		disposisiAlurSopAction.init(disposisiAlurSop, alurSop, disposisiSop, dokumen, editPilihan);

		disposisiAlurSopAction.addWindow.setVisible(true);
		disposisiAlurSopAction.addWindow.setClosable(true);
		disposisiAlurSopAction.addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		disposisiAlurSop = (DisposisiAlurSop) obj;
		init(disposisiAlurSop, alurSop, disposisiSop, dokumen, editPilihan);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings({ "deprecation", "unchecked" })
	private void init(final DisposisiAlurSop disposisiAlurSop, final AlurSop alurSop, final DisposisiSop disposisiSop,
			final Set<DokumenAlurSop> dokumen, boolean editPilihan) throws Exception {
		idsSelected = null;
		this.disposisiAlurSop = disposisiAlurSop;
		this.alurSop = alurSop;
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null))
				? this.disposisiSop
				: disposisiSop;
		this.editPilihan = editPilihan;
		this.dokumen = dokumen;

		addWindow.setTitle("Disposisi SOP \"" + (alurSop != null && alurSop.getSop() != null ? alurSop.getSop().getNama() : "") + "\"");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(addWindow);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
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

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("SOP (Workflow) *"));
		row.appendChild(new Label(alurSop.getSop().getKode() + " " + alurSop.getSop().getNama()));

		final MyFormRow rowFile = new MyFormRow();
		rowFile.setParent(rows);

		final EventListener eventListener1 = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(rowFile);
				rowFile.appendChild(new ais.ui.util.MyLabelConfig("Lampiran/Informasi SOP"));
				rowFile.setVisible(false);
				Sop s = DisposisiAlurSopAction.this.alurSop.getSop();
				if (s != null) {
					FileFotoLain fileFotoLain = FileFotoLain.ambil(false, s.getId(), Sop.class.getName(), LampiranLain.class);
					rowFile.setVisible(fileFotoLain != null);
					Vbox myvbox = new Vbox();
					myvbox.setParent(rowFile);

					Hbox hbox = new Hbox();
					hbox.setParent(myvbox);
					LampiranLain.createDownloadUploadFileLain(hbox, s.getId(), Sop.class.getName(),
							"Lampiran/Informasi SOP", false, null, null, false, false, false, Common.getApakahAdmin());
				}
			}
		};
		eventListener1.onEvent(null);

		alurSops = alurSop.ambilAlurSetelahnya();
		opsiAlurSops = alurSop.ambilOpsiAlurSetelahnya();

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		row.appendChild(new MyLabelStyled("Proses Alur Sebelumnya"));

		if (disposisiAlurSop.getSebelumnya() != null && disposisiAlurSop.getSebelumnya().getAlurSop() != null) {
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Aktor"));

			Vbox vbox = new Vbox();
			vbox.setParent(row);
			vbox.setWidth("100%");

			Tbmuser tbUserSblm = disposisiAlurSop.getSebelumnya().getDiajukanOleh();
			ais.database.model.Mahasiswa mhsSblm = disposisiAlurSop.getSebelumnya().getMahasiswa();
			ais.database.model.sekolah.Siswa siswaSblm = disposisiAlurSop.getSebelumnya().getSiswa();

			renderAktorUI(tbUserSblm, mhsSblm, siswaSblm, vbox);
			vbox.appendChild(new Label(disposisiAlurSop.getSebelumnya().getAlurSop().getAktor()));

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Proses"));
			row.appendChild(new Label(disposisiAlurSop.getSebelumnya().getAlurSop().getKode() + " - " + disposisiAlurSop.getSebelumnya().getAlurSop().getNama()));

			if (disposisiAlurSop.getSebelumnya().getAlurSop().getBolehDiisiCatatan()) {
				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Catatan"));
				row.appendChild(new Label(disposisiAlurSop.getSebelumnya().getKeterangan()));

				row = new MyFormRow();
				row.setVisible(Common.bolehKonfigurasi("tampilkan_lampiran_catatan_disposisi"));
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig(""));
				Vbox vboxLamp = new Vbox();
				vboxLamp.setParent(row);
				Hbox hboxLamp = new Hbox();
				hboxLamp.setParent(vboxLamp);
				LampiranLain.createDownloadUploadFileLain(hboxLamp, disposisiAlurSop.getSebelumnya().getId(),
						"Lampiran Catatan Disposisi", "Lampiran Catatan Disposisi Sebelumnya", false,
						new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {}
						}, null, false, false, false, Common.getApakahAdmin());
			}

			Session sessionDoc = null;
			try {
				sessionDoc = HibernateUtil.getSessionFactory().openSession();
				AlurSop alurSopSebelumya = (AlurSop) sessionDoc.createCriteria(AlurSop.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.idEq(disposisiAlurSop.getSebelumnya().getAlurSop().getId())).uniqueResult();

				if (alurSopSebelumya != null) {
					Set<DokumenAlurSop> dokumenAlurSops = alurSopSebelumya.getDokumenAlurSops();

					if (!dokumenAlurSops.isEmpty()) {
						row = new MyFormRow();
						row.setParent(rows);
						row.appendChild(new ais.ui.util.MyLabelConfig("Dokumen"));
						Vbox rowdokumen = new Vbox();
						row.appendChild(rowdokumen);

						for (DokumenAlurSop dokumenAlurSop : dokumenAlurSops) {
							if (dokumenAlurSop.getAktif()) {
								Hbox hboxDoc = new Hbox();
								hboxDoc.setParent(rowdokumen);
								if (disposisiAlurSop.getSebelumnya().getAlurSop().getStart()) {
									LampiranLain.createDownloadUploadFileLain(hboxDoc, disposisiSop.getId(),
											DokumenAlurSop.class.getName() + "_" + dokumenAlurSop.getId(),
											dokumenAlurSop.getNama(), false, null, null, false, false, false, Common.getApakahAdmin());
								} else {
									LampiranLain.createDownloadUploadFileLain(hboxDoc, disposisiAlurSop.getSebelumnya().getId(),
											DokumenAlurSop.class.getName() + "_alur_" + dokumenAlurSop.getId(),
											dokumenAlurSop.getNama(), false, null, null, false, false, false, Common.getApakahAdmin());
								}
							}
						}
					}
				}
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			} finally {
				if (sessionDoc != null) { try { sessionDoc.clear(); sessionDoc.disconnect(); sessionDoc.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sop/DisposisiAlurSopAction.java:682");} }
			}
		}

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		row.appendChild(new MyLabelStyled("Alur Disposisi Anda"));

		if (disposisiAlurSop.getAlurSop() != null) {
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Aktor Disposisi *"));

			Vbox vbox = new Vbox();
			vbox.setParent(row);
			vbox.setWidth("100%");

			Tbmuser currUser = Common.getCurrentUser();
			try {
				CommonMedia.tampilkanGambarKecil(currUser).setParent(vbox);
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

			vbox.appendChild(new Label(currUser == null ? "" : currUser.getUserNama()));
			vbox.appendChild(new Label(disposisiAlurSop.getAlurSop().getAktor()));

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Proses Disposisi *"));
			row.appendChild(new Label(disposisiAlurSop.getAlurSop().getNama()));
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal dan Waktu Disposisi *"));

		waktu = new MyDatebox(disposisiAlurSop.getWaktu() == null ? new Date() : disposisiAlurSop.getWaktu());
		if (disposisiAlurSop.getAlurSop() != null && disposisiAlurSop.getAlurSop().getTanggalDisposisiBolehDiubah()) {
			row.appendChild(waktu);
		} else {
			row.appendChild(new Label(Common.dateFormat3.get().format(waktu.getValue())));
		}
		waktu.setFormat(Common.dateFormat3.get().toPattern());
		waktu.setReadonly(true);
		waktu.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(disposisiAlurSop.getAlurSop() != null && disposisiAlurSop.getAlurSop().getBolehDiisiCatatan());
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Catatan Disposisi " + (disposisiAlurSop.getAlurSop() != null && disposisiAlurSop.getAlurSop().getCatatanWajibDiisi() ? "*" : "")));
		row.appendChild(keterangan = new Textbox(disposisiAlurSop.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		lampiranCatatanDisposisi = null;
		row = new MyFormRow();
		row.setVisible(disposisiAlurSop.getAlurSop() != null && disposisiAlurSop.getAlurSop().getBolehDiisiCatatan());
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		Hbox hboxInfo = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hboxInfo, disposisiAlurSop.getId() == null ? -Common.randLong() : disposisiAlurSop.getId(), "Lampiran Catatan Disposisi",
				"Lampiran Catatan Disposisi " + (disposisiAlurSop.getAlurSop() != null && disposisiAlurSop.getAlurSop().getLampiranCatatanWajibDiisi() ? "*" : ""),
				false, new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						lampiranCatatanDisposisi = (LampiranLain) arg0.getData();
					}
				});
		hboxInfo.setParent(row);

		if (disposisiAlurSop.getAlurSop() != null && disposisiAlurSop.getAlurSop().getBolehDiisiCatatan()) {
			Common.initKeterangan(rows, "Jika file lampiran catatan disposisi lebih dari satu file, zip dulu semua file tersebut");
		}

		final MyToolbarbuttonConfig save1 = new MyToolbarbuttonConfig(("Simpan / Disposisi"), "/img/svg/save-2-fill.svg");

		hboxAktor = new Hbox();
		setujuiData = null;

		final Radio radioSetujuidanSelesai = new Radio("Setujui dan Selesai");

		EventListener setujui = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				try {
					if (griddata != null && griddata.getAttribute("eventListenerSetuju") != null) {
						((EventListener) griddata.getAttribute("eventListenerSetuju")).onEvent(arg0);
					}

					setujuiData = null;
					kembaliData = null;
					if (alurSop.getJikaProsesDisetujuiMakaSelesai()) {

						if (arg0 != null && ((arg0.getTarget() instanceof Checkbox))) {
							Checkbox pilihanSetujui = (Checkbox) arg0.getTarget();
							setujuiData = pilihanSetujui.isChecked();
						} else if (arg0 != null && (arg0.getTarget() instanceof Radiogroup)) {
							Radiogroup pilihanSetujui = (Radiogroup) arg0.getTarget();
							setujuiData = pilihanSetujui.getSelectedItem() == null ? false : pilihanSetujui.getSelectedItem().getAttribute("value").equals(UangMuka.DISETUJU);
						} else if (arg0 != null && (arg0.getData() instanceof Boolean)) {
							setujuiData = (Boolean) arg0.getData();
						}

						DisposisiAlurSopAction.this.disposisiAlurSop.setSelesai(setujuiData);
						radioSetujuidanSelesai.setDisabled(false);

						if(hboxAktor != null && hboxAktor.getParent() != null) hboxAktor.getParent().setVisible(true);
						if(waktuMaksimal != null && waktuMaksimal.getParent() != null) waktuMaksimal.getParent().setVisible(true);

						boolean adaPerubahan = false;
						if (setujuiData != null && arg0 != null && arg0.getTarget() != null && arg0.getTarget() != radioSetujuidanSelesai) {
							if (radiogroup != null) {
								if (setujuiData) {
									List<Radio> radios = radiogroup.getChildren();
									for (Radio radio : radios) {
										if (!radio.getLabel().toLowerCase().contains("setuju")) {
											adaPerubahan = true;
										}
									}
									radioSetujuidanSelesai.setChecked(true);
									radioSetujuidanSelesai.setDisabled(true);
									if(hboxAktor.getParent() != null) hboxAktor.getParent().setVisible(false);
									if(waktuMaksimal != null && waktuMaksimal.getParent() != null) waktuMaksimal.getParent().setVisible(false);
								} else {
									List<Radio> radios = radiogroup.getChildren();
									for (Radio radio : radios) {
										if (radio.getLabel().toLowerCase().contains("setuju")) {
											adaPerubahan = true;
										}
									}
									radioSetujuidanSelesai.setChecked(false);
								}
							}
						}

						if (!adaPerubahan) {
							if (setujuiData) {
								if(hboxAktor.getParent() != null) hboxAktor.getParent().setVisible(false);
								if(waktuMaksimal != null && waktuMaksimal.getParent() != null) waktuMaksimal.getParent().setVisible(true);
								if(rowBerikutnya != null) rowBerikutnya.setVisible(true);
								if (vboxPilihan != null && vboxPilihan.getParent() != null) vboxPilihan.getParent().setVisible(true);
							} else {
								radioSetujuidanSelesai.setSelected(false);
								if(rowBerikutnya != null) rowBerikutnya.setVisible(true);
								if (vboxPilihan != null && vboxPilihan.getParent() != null) vboxPilihan.getParent().setVisible(true);
								if(hboxAktor != null && hboxAktor.getParent() != null) hboxAktor.getParent().setVisible(true);
								if(waktuMaksimal != null && waktuMaksimal.getParent() != null) waktuMaksimal.getParent().setVisible(true);
							}
						}
					}
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
			}
		};

		kembaliData = null;
		EventListener kembali = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				try {
					if (griddata != null && griddata.getAttribute("eventListenerSetuju") != null) {
						((EventListener) griddata.getAttribute("eventListenerSetuju")).onEvent(arg0);
					}

					kembaliData = null;

					/*
					 * PERBAIKAN (disposisi revisi & berulang saat memilih "Setujui dan Selesai"):
					 * kembaliData adalah penanda "KEMBALI/REVISI ke alur sebelumnya" — dipakai di
					 * onSave lewat disposisiAlurSop.setKembali(), yang memicu jalur pembuatan
					 * disposisi MUNDUR ke aktor sebelumnya.
					 *
					 * BUG lama: isi listener ini adalah salin-tempel dari listener "setuju" —
					 * digerbangi getJikaProsesDisetujuiMakaSelesai() dan nilainya diambil dari
					 * PILIHAN PERSETUJUAN (dibandingkan dengan UangMuka.DISETUJU). Akibatnya saat
					 * pengguna memilih "Setujui dan Selesai", kembaliData ikut bernilai true →
					 * setKembali(true) → onSave menempuh jalur REVISI dan membuat disposisi mundur,
					 * berulang setiap kali disetujui.
					 *
					 * Sekarang nilainya ditentukan HANYA dari kontrol "Kembali ke alur disposisi
					 * sebelumnya" (ditandai atribut "kembaliKeSebelumnya"), dan TIDAK lagi
					 * digerbangi flag persetujuan — sehingga pilihan kembali selalu terbaca.
					 * setSelesai() sengaja TIDAK disentuh di sini; itu urusan listener "setuju"
					 * (setujuiData) dan penetapan akhir di onSave.
					 */
					if (arg0 != null && (arg0.getTarget() instanceof Radiogroup)) {
						Radiogroup grup = (Radiogroup) arg0.getTarget();
						Radio terpilih = grup.getSelectedItem();
						kembaliData = terpilih != null
								&& Boolean.TRUE.equals(terpilih.getAttribute("kembaliKeSebelumnya"));
					} else if (arg0 != null && (arg0.getTarget() instanceof Radio)) {
						Radio r = (Radio) arg0.getTarget();
						kembaliData = r.isChecked() && Boolean.TRUE.equals(r.getAttribute("kembaliKeSebelumnya"));
					} else if (arg0 != null && (arg0.getTarget() instanceof Checkbox)) {
						Checkbox c = (Checkbox) arg0.getTarget();
						kembaliData = c.isChecked() && Boolean.TRUE.equals(c.getAttribute("kembaliKeSebelumnya"));
					}

					// Memilih "kembali" berarti BUKAN menyetujui — bersihkan pilihan setujui.
					// (Hanya saat benar-benar memilih kembali, agar pilihan "Setujui dan Selesai"
					// yang memicu listener ini lewat Radiogroup tidak ikut terhapus.)
					if (Boolean.TRUE.equals(kembaliData)) {
						setujuiData = null;
					}

					// Saat memilih "kembali", tidak ada alur berikutnya yang perlu dipilih.
					if (Boolean.TRUE.equals(kembaliData)) {
						if(rowBerikutnya != null) rowBerikutnya.setVisible(false);
						if (vboxPilihan != null && vboxPilihan.getParent() != null) vboxPilihan.getParent().setVisible(false);
						if(hboxAktor != null && hboxAktor.getParent() != null) hboxAktor.getParent().setVisible(false);
						if(waktuMaksimal != null && waktuMaksimal.getParent() != null) waktuMaksimal.getParent().setVisible(false);
					} else {
						if(rowBerikutnya != null) rowBerikutnya.setVisible(true);
						if (vboxPilihan != null && vboxPilihan.getParent() != null) vboxPilihan.getParent().setVisible(true);
						if(hboxAktor != null && hboxAktor.getParent() != null) hboxAktor.getParent().setVisible(true);
						if(waktuMaksimal != null && waktuMaksimal.getParent() != null) waktuMaksimal.getParent().setVisible(true);
					}
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
			}
		};

		formSop = null;
		if (alurSop.getFormInputan() != null && !alurSop.getFormInputan().isEmpty()) {
			try {
				formSop = (FormSop) Class.forName(alurSop.getFormInputan()).newInstance();

				MyFormRow rowLampiran = new MyFormRow();
				ais.ui.util.ZkCompat.setSpans(rowLampiran, "2");
				rowLampiran.appendChild(new MyLabelStyled(formSop.istilah()));
				rowLampiran.setParent(rows);

				String key = formSop.ambilClass().getName();

				GeneralValueObject generalValueObject = null;
				JSONObject o = null;
				if (disposisiSop != null && disposisiSop.getProperti() != null) {
					try { o = new JSONObject(disposisiSop.getProperti()); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
				}

				JSONObject jsonObject = (o != null && !o.isNull(key)) ? o.getJSONObject(key) : null;

				if (disposisiSop != null && disposisiSop.getId() != null) {
					generalValueObject = (GeneralValueObject) (jsonObject == null || jsonObject.isNull("id")
							? formSop.ambilClass().newInstance()
							: GeneralValueObject.ambilData(formSop.ambilClass(), (jsonObject.get("id") + ""), true));
				} else {
					generalValueObject = (GeneralValueObject) formSop.ambilClass().newInstance();
				}

				if ((generalValueObject == null || generalValueObject.getId() == null) && disposisiSop != null && disposisiSop.getId() != null) {
					Session session = null;
					try {
						session = HibernateUtil.getSessionFactory().openSession();
						generalValueObject = disposisiSop.ambil(session, formSop);
					} catch (Exception e) {
						ais.common.Common.tampilErrorJikaAdmin(e);
					} finally {
						if (session != null) { try { session.clear(); session.disconnect(); session.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sop/DisposisiAlurSopAction.java:921");} }
					}
				}

				if (generalValueObject == null) {
					generalValueObject = (GeneralValueObject) formSop.ambilClass().newInstance();
				}

				rowLampiran = new MyFormRow();
				ais.ui.util.ZkCompat.setSpans(rowLampiran, "2");
				rowLampiran.appendChild(griddata = formSop.form(generalValueObject, disposisiSop, save1, setujui));
				rowLampiran.setParent(rows);

				if (alurSop.getBekukanFormTampilan() && !Common.getApakahAdmin()) {
					Common.freeze(rowLampiran, true);
				}

			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
		}

		rowBerikutnya = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(rowBerikutnya, "2");
		rowBerikutnya.setParent(rows);
		rowBerikutnya.appendChild(new MyLabelStyled("Alur Disposisi Berikutnya"));

		if (alurSops.isEmpty() && !alurSop.getKembaliKeAktorSebelumnya()) {
			row = new MyFormRow();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			row.appendChild(new ais.ui.util.MyLabelAgakKecilBoldMerah("Tidak ada disposisi berikutnya"));
		}

		rowProses = new MyFormRow();
		rowProses.setParent(rows);
		rowProses.appendChild(new ais.ui.util.MyLabelConfig("Proses SOP *"));

		Session sessionIds = null;
		try {
			sessionIds = HibernateUtil.getSessionFactory().openSession();
			idsSelected = sessionIds.createCriteria(DisposisiAlurSop.class)
					.add(Restrictions.isNotNull("alurSop"))
					.add(Restrictions.eq("sebelumnya", disposisiAlurSop))
					.setProjection(Projections.groupProperty("alurSop.id")).list();
		} finally {
			if (sessionIds != null) { try { sessionIds.clear(); sessionIds.disconnect(); sessionIds.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sop/DisposisiAlurSopAction.java:967");} }
		}

		if (idsSelected == null) idsSelected = new ArrayList<Long>();

		radiogroup = null;
		vboxPilihan = null;

		if (editPilihan) {
			if (alurSop.getAlurSetelahnyaBerupaPilihan()) {
				radiogroup = new Radiogroup();
				radiogroup.setOrient("vertical");
				rowProses.appendChild(radiogroup);

				int i = 0;
				for (final AlurSop alurSop2 : alurSops) {

					String opsi = (opsiAlurSops != null && opsiAlurSops.size() > i) ? opsiAlurSops.get(i) : "";
					if (opsi.trim().isEmpty()) {
						opsi = alurSop2.getOpsi() != null ? alurSop2.getOpsi() : "";
					}
					if (!opsi.trim().isEmpty()) opsi += " - ";
					i++;

					Radio radio = new Radio(opsi + "" + alurSop2.getAktor() + " - " + alurSop2.getNama());
					radio.setSelected(idsSelected.contains(alurSop2.getId()));
					radio.setAttribute("alurSop", alurSop2);
					radiogroup.appendChild(radio);

					radio.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							if (griddata != null && griddata.getAttribute("eventListenerSetuju") != null) {
								((EventListener) griddata.getAttribute("eventListenerSetuju")).onEvent(arg0);
							}

							kembaliData = null;
							setujuiData = null;

							SopUtil.resetAktor(hboxAktor);
							if(hboxAktor.getParent() != null) hboxAktor.getParent().setVisible(false);

							if (((Radio) arg0.getTarget()).isChecked()) {
								if (alurSop2.getKembaliKePengaju()) {
									Tbmuser tbPengaju = disposisiSop.getDiajukanOleh();
									SopUtil.renderAktorTunggal(tbPengaju, disposisiSop.getMahasiswa(), disposisiSop.getSiswa(), hboxAktor);
								} else {
									// FIX MULTI-USER: sebelumnya hanya khususUsername yang dirender —
									// aktor berbasis ROLE/jabatan/atasan tidak tampil sama sekali dan
									// baris Aktor SOP tetap tersembunyi. tampilAktor mencakup khusus
									// username DAN seluruh resolusi role (sama dengan jalur checkbox).
									SopUtil.tampilAktor(null, alurSop2.getKhususUsername(),
											alurSop2.getAktorSop() != null ? alurSop2.getAktorSop().getJenisPengguna() : "",
											disposisiAlurSop.getDisposisiSop(), alurSop2, hboxAktor);
								}
								if (hboxAktor.getParent() != null) hboxAktor.getParent().setVisible(true);
							}
						}
					});
				}

				if (alurSop.getKembaliKeAktorSebelumnya() && disposisiAlurSop.getSebelumnya() != null) {
					Radio radio = new Radio("Kembali ke alur disposisi sebelumnya");
					// Penanda: HANYA kontrol inilah yang boleh menyalakan flag kembali/revisi.
					radio.setAttribute("kembaliKeSebelumnya", Boolean.TRUE);
					radio.setChecked(Boolean.TRUE.equals(DisposisiAlurSopAction.this.disposisiAlurSop.getKembali()));
					radio.addEventListener("onClick", kembali);
					radiogroup.appendChild(radio);
				}

				if (alurSop.getJikaProsesDisetujuiMakaSelesai()) {
					radioSetujuidanSelesai.setAttribute("selesai", true);
					radioSetujuidanSelesai.setChecked(DisposisiAlurSopAction.this.disposisiAlurSop.getSelesai());
					radioSetujuidanSelesai.addEventListener("onClick", setujui);
					radiogroup.appendChild(radioSetujuidanSelesai);
				}

			} else {
				vboxPilihan = new Vbox();
				rowProses.appendChild(vboxPilihan);
				int i = 0;
				for (final AlurSop alurSop2 : alurSops) {

					String opsi = (opsiAlurSops != null && opsiAlurSops.size() > i) ? opsiAlurSops.get(i) : "";
					if (opsi.trim().isEmpty()) opsi = alurSop2.getOpsi() != null ? alurSop2.getOpsi() : "";
					if (!opsi.trim().isEmpty()) opsi += " - ";
					i++;

					Checkbox checkbox = new Checkbox(opsi + "" + alurSop2.getAktor() + " - " + alurSop2.getNama());
					checkbox.setChecked(idsSelected.contains(alurSop2.getId()));
					checkbox.setAttribute("alurSop", alurSop2);
					vboxPilihan.appendChild(checkbox);

					checkbox.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							if (griddata != null && griddata.getAttribute("eventListenerSetuju") != null) {
								((EventListener) griddata.getAttribute("eventListenerSetuju")).onEvent(arg0);
							}

							kembaliData = null;
							setujuiData = null;

							SopUtil.resetAktor(hboxAktor);
							if(hboxAktor.getParent() != null) hboxAktor.getParent().setVisible(false);

							List<Component> components = vboxPilihan.getChildren();
							for (Component component : components) {
								if (component instanceof Checkbox && ((Checkbox) component).isChecked()) {
									// FIX MULTI-USER: dahulu cabang kembali-ke-pengaju memeriksa
									// alurSop2 (checkbox yang DIKLIK), bukan alur milik MASING-MASING
									// checkbox tercentang — saat multi-pilih, preview penerima salah
									// (semua ikut cabang milik checkbox terakhir diklik). Kini setiap
									// checkbox dievaluasi berdasarkan alur-nya sendiri. Kontrol tanpa
									// alurSop ("Kembali ke sebelumnya"/"Setujui dan Selesai") dilewati.
									AlurSop alur = (AlurSop) component.getAttribute("alurSop");
									if (alur == null) {
										continue;
									}
									if (alur.getKembaliKePengaju()) {
										Tbmuser tbPengaju = disposisiSop.getDiajukanOleh();
										SopUtil.renderAktorTunggal(tbPengaju, disposisiSop.getMahasiswa(), disposisiSop.getSiswa(), hboxAktor);
									} else {
										SopUtil.tampilAktor(null, alur.getKhususUsername(),
												alur.getAktorSop() != null ? alur.getAktorSop().getJenisPengguna() : "",
												disposisiAlurSop.getDisposisiSop(), alur, hboxAktor);
									}
									if(hboxAktor != null && hboxAktor.getParent() != null) hboxAktor.getParent().setVisible(true);
								}
							}
						}
					});
				}

				if (alurSop.getKembaliKeAktorSebelumnya() && disposisiAlurSop.getSebelumnya() != null) {
					Checkbox radio = new Checkbox("Kembali ke alur disposisi sebelumnya");
					// Penanda: HANYA kontrol inilah yang boleh menyalakan flag kembali/revisi.
					radio.setAttribute("kembaliKeSebelumnya", Boolean.TRUE);
					radio.setChecked(Boolean.TRUE.equals(DisposisiAlurSopAction.this.disposisiAlurSop.getKembali()));
					radio.addEventListener("onClick", kembali);
					vboxPilihan.appendChild(radio);
				}

				if (alurSop.getJikaProsesDisetujuiMakaSelesai()) {
					Checkbox radio = new Checkbox("Setujui dan Selesai");
					radio.setAttribute("selesai", true);
					radio.setChecked(DisposisiAlurSopAction.this.disposisiAlurSop.getSelesai());
					radio.addEventListener("onClick", setujui);
					vboxPilihan.appendChild(radio);
				}
			}
		} else {
			Vbox vbox = new Vbox();
			rowProses.appendChild(vbox);
			for (AlurSop alurSop2 : alurSops) {
				vbox.appendChild(new Label(alurSop2.getAktor() + " - " + alurSop2.getNama()));
			}
		}

		row = new MyFormRow();
		row.setVisible(!alurSops.isEmpty() || alurSop.getKembaliKeAktorSebelumnya());
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Batas Waktu / Deadline *"));
		row.appendChild(waktuMaksimal = new MyDatebox(disposisiAlurSop.getWaktuMaksimal()));
		waktuMaksimal.setFormat(Common.dateFormat3.get().toPattern());
		waktuMaksimal.setReadonly(true);
		waktuMaksimal.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Aktor SOP *"));
		row.appendChild(hboxAktor);

		if(hboxAktor.getParent() != null) hboxAktor.getParent().setVisible(!idsSelected.isEmpty());

		for (Long id : idsSelected) {
			AlurSop alurSopData = (AlurSop) ConstantValues.ambil(AlurSop.class.getName(), id);
			if (alurSopData != null) {
				if (alurSopData.getKembaliKePengaju()) {
					Tbmuser tbPengaju = disposisiSop.getDiajukanOleh();
					SopUtil.renderAktorTunggal(tbPengaju, disposisiSop.getMahasiswa(), disposisiSop.getSiswa(), hboxAktor);
				} else {
					// FIX MULTI-USER: pre-select dahulu hanya merender khususUsername —
					// aktor berbasis ROLE/jabatan tidak tampil saat form dibuka ulang.
					// tampilAktor mencakup keduanya (konsisten dengan listener checkbox).
					SopUtil.tampilAktor(null, alurSopData.getKhususUsername(),
							alurSopData.getAktorSop() != null ? alurSopData.getAktorSop().getJenisPengguna() : "",
							disposisiSop, alurSopData, hboxAktor);
				}
			}
		}

		parameterRows = new ArrayList<Row>();
		lampiranLains = new HashMap<String, LampiranLain>();
		parameterTambahanListener = new ParameterTambahanDisposisiAlurSopListener(disposisiAlurSop, parameterRows,
				lampiranLains, rows, alurSop.getBekukanFormTampilan() && !Common.getApakahAdmin());

		parameterTambahanListener.onEvent(null);
		lainMahasiswa = new HashMap<Long, LampiranLain>();

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

		ref = DisposisiAlurSopAction.this.disposisiAlurSop.getId() == null ? -Common.randLong() : DisposisiAlurSopAction.this.disposisiAlurSop.getId();

		EventListener eventListener = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.clear(rowsLampiran);
				DisposisiSopAction.reloadDataMenu(rowsLampiran, new JSONArray(alurSop.getHalamanMenu() != null ? alurSop.getHalamanMenu() : "[]"), eventListener1);

				if (dokumen != null && !dokumen.isEmpty()) {
					MyFormRow rowLampiranHeader = new MyFormRow();
					ais.ui.util.ZkCompat.setSpans(rowLampiranHeader, "2");
					rowLampiranHeader.appendChild(new MyLabelStyled("Dokumen"));
					rowLampiranHeader.setParent(rowsLampiran);

					for (final DokumenAlurSop dokumenAlurSop : dokumen) {
						if (dokumenAlurSop.getAktif()) {
							MyFormRow rowDoc = new MyFormRow();
							rowDoc.appendChild(new Label(dokumenAlurSop.getNama()));
							rowDoc.setParent(rowsLampiran);

							final Hbox hbox = new Hbox();
							hbox.setParent(rowDoc);

							if (alurSop.getBekukanDokumen()) {
								final LampiranLain lampiranLain = LampiranLain.ambil(ref, DokumenAlurSop.class.getName() + "_alur_" + dokumenAlurSop.getId());

								if (lampiranLain != null) {
									A aa = new A(lampiranLain.getNama());
									aa.setParent(hbox);
									aa.setWidth("95%");
									aa.addEventListener("onClick", new EventListener() {
										@Override
										public void onEvent(Event arg0) throws Exception {
											Common.display(lampiranLain);
										}
									});
								} else {
									hbox.appendChild(new Label(dokumenAlurSop.getNama()));
								}

							} else {
								EventListener eventListenerDokumen = new EventListener() {
									private EventListener getThis() {
										return this;
									}

									@Override
									public void onEvent(Event arg0) throws Exception {
										Common.clear(hbox);

										LampiranLain.createDownloadUploadFileLain(hbox, ref,
												DokumenAlurSop.class.getName() + "_alur_" + dokumenAlurSop.getId(),
												dokumenAlurSop.getNama(), false, new EventListener() {
													@Override
													public void onEvent(Event arg0) throws Exception {
														LampiranLain lain = (LampiranLain) arg0.getData();
														if (lain == null) {
															lainMahasiswa.remove(dokumenAlurSop.getId());
														} else {
															lainMahasiswa.put(dokumenAlurSop.getId(), lain);
														}
														Common.createDefaultTimer(getThis());
													}
												});

										LampiranLain fileSaatIni = LampiranLain.ambil(ref, DokumenAlurSop.class.getName() + "_alur_" + dokumenAlurSop.getId());
										if (fileSaatIni == null) {
											if (DisposisiAlurSopAction.this.disposisiAlurSop.getSebelumnya() != null) {
												LampiranLain fileSebelumnya;
												if (DisposisiAlurSopAction.this.disposisiAlurSop.getSebelumnya().getAlurSop().getStart()) {
													fileSebelumnya = LampiranLain.ambil(
															DisposisiAlurSopAction.this.disposisiAlurSop.getSebelumnya().getDisposisiSop().getId(),
															DokumenAlurSop.class.getName() + "_" + dokumenAlurSop.getId());
												} else {
													fileSebelumnya = LampiranLain.ambil(
															DisposisiAlurSopAction.this.disposisiAlurSop.getSebelumnya().getId(),
															DokumenAlurSop.class.getName() + "_alur_" + dokumenAlurSop.getId());
												}

												if (fileSebelumnya != null) {
													MyCheckboxConfig myCheckboxConfig = new MyCheckboxConfig("Samakan dokumen dengan alur sebelumnya");
													myCheckboxConfig.setParent(hbox);
													myCheckboxConfig.addEventListener("onClick", new EventListener() {
														@Override
														public void onEvent(Event arg0) throws Exception {
															if (((MyCheckboxConfig) arg0.getTarget()).isChecked()) {
																LampiranLain fileSblm = null;
																if (DisposisiAlurSopAction.this.disposisiAlurSop.getSebelumnya().getAlurSop().getStart()) {
																	fileSblm = LampiranLain.ambil(DisposisiAlurSopAction.this.disposisiAlurSop.getSebelumnya().getDisposisiSop().getId(), DokumenAlurSop.class.getName() + "_" + dokumenAlurSop.getId());
																} else {
																	fileSblm = LampiranLain.ambil(DisposisiAlurSopAction.this.disposisiAlurSop.getSebelumnya().getId(), DokumenAlurSop.class.getName() + "_alur_" + dokumenAlurSop.getId());
																}

																if (fileSblm != null) {
																	LampiranLain newLampiran = new LampiranLain();
																	newLampiran.setCopyDari(fileSblm);
																	newLampiran.setJenis(DokumenAlurSop.class.getName() + "_alur_" + dokumenAlurSop.getId());

																	Session sessionStr = null;
																	try {
																		sessionStr = StreamingHibernateUtil.getInstance().currentSession();
																		newLampiran.setRef(ref);
																		sessionStr.getTransaction().begin();
																		sessionStr.save(newLampiran);
																		sessionStr.getTransaction().commit();
																	} catch (Exception e) {
																		StreamingHibernateUtil.getInstance().rollbackTransaction();
																		Common.tampilErrorJikaAdmin(e);
																	} finally {
																		StreamingHibernateUtil.getInstance().closeSession();
																	}
																	Common.createDefaultTimer(getThis());
																}
															}
														}
													});
												}
											}
										}
									}
								};
								eventListenerDokumen.onEvent(arg0);
							}
						}
					}
					Common.initKeterangan(rowsLampiran, "Jika file dokumen lebih dari satu file, zip dulu semua file tersebut");
				}
			}
		};
		eventListener.onEvent(null);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(south);

		if (!ubah) {
			MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
			cancel.setTooltiptext("Tutup");
			cancel.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					addWindow.setVisible(false);
				}
			});
			cancel.setParent(toolbar);
			Common.freezeGanti(grid, true);
		} else {
			MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
			cancel.setTooltiptext("Tutup");
			cancel.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					addWindow.setVisible(false);
				}
			});
			cancel.setParent(toolbar);

			if (formSop != null) {
				save1.setLabel(Common.getBahasaConfig("Simpan / Disposisi sebagai") + " \"" + formSop.istilah() + "\"");
			}

			save1.setTooltiptext("Simpan");
			save1.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					if (!check()) {
						return;
					}

					if (formSop != null && formSop.onSave(event)) {
						Common.createDefaultTimer(new EventListener() {
							@Override
							public void onEvent(Event arg0) throws Exception {
								if (onSave(arg0)) {
									onSearchDefault(null);
									addWindow.setVisible(false);
									if (DisposisiAlurSopAction.this.eventListener != null) {
										DisposisiAlurSopAction.this.eventListener.onEvent(arg0);
									}
								}
							}
						});
					} else if (formSop == null) {
						if (onSave(event)) {
							onSearchDefault(null);
							addWindow.setVisible(false);
							if (DisposisiAlurSopAction.this.eventListener != null) {
								DisposisiAlurSopAction.this.eventListener.onEvent(event);
							}
						}
					}
				}
			});
			save1.setParent(toolbar);
		}
	}

	@SuppressWarnings("unchecked")
	public boolean check() throws Exception {
		if (alurSop != null && alurSop.getCatatanWajibDiisi() && keterangan.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show(
					"Mohon Bapak/Ibu terlebih dahulu mengisi Catatan. Langkah yang dapat dilakukan: (1) klik kolom Catatan; (2) isikan catatan disposisi secara jelas; (3) lanjutkan menyimpan disposisi.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (Common.bolehKonfigurasi("tampilkan_lampiran_catatan_disposisi")) {
			if (alurSop != null && alurSop.getLampiranCatatanWajibDiisi()) {
				LampiranLain lampiranLain = disposisiAlurSop.getId() == null ? null : LampiranLain.ambil(disposisiAlurSop.getId(), "Lampiran Catatan Disposisi");
				if (lampiranCatatanDisposisi == null && lampiranLain == null) {
					MyMessageboxConfig.show(
							"Mohon Bapak/Ibu terlebih dahulu mengunggah Lampiran Catatan. Langkah yang dapat dilakukan: (1) klik tombol unggah Lampiran Catatan; (2) pilih berkas lampiran yang sesuai; (3) lanjutkan menyimpan disposisi.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}
		}

		if (waktu.getValue() == null) {
			MyMessageboxConfig.show(
					"Mohon Bapak/Ibu terlebih dahulu mengisi Waktu. Langkah yang dapat dilakukan: (1) klik kolom Waktu; (2) pilih tanggal dan waktu yang sesuai; (3) lanjutkan menyimpan disposisi.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		selanjutnya = new ArrayList<AlurSop>();

		if (alurSops != null && !alurSops.isEmpty()) {
			if (radiogroup != null) {
				if (radiogroup.getSelectedItem() != null && radiogroup.getSelectedItem().getAttribute("alurSop") != null) {
					AlurSop a = (AlurSop) radiogroup.getSelectedItem().getAttribute("alurSop");
					if (a != null) {
						selanjutnya.add(a);
					}
				}
			} else if (vboxPilihan != null) {
				List<Component> components = vboxPilihan.getChildren();
				for (Component component : components) {
					if (component instanceof Checkbox && ((Checkbox) component).isChecked()) {
						AlurSop a = (AlurSop) component.getAttribute("alurSop");
						if (a != null) {
							selanjutnya.add(a);
						}
					}
				}
			}

			if (idsSelected != null && selanjutnya.isEmpty() && alurSops.size() > 1) {
				for (Long id : idsSelected) {
					AlurSop a = (AlurSop) ConstantValues.ambil(AlurSop.class.getName(), id);
					if (a != null) {
						selanjutnya.add(a);
					}
				}
			}
		}

		if (alurSop == null || !alurSop.getAlurSetelahnyaTidakWajib()) {
			if (alurSops != null && !alurSops.isEmpty()) {
				if (radiogroup != null && !radiogroup.getChildren().isEmpty() && rowProses != null && rowProses.isVisible()) {
					boolean ada = false;
					List<Component> components = radiogroup.getChildren();
					for (Component component : components) {
						if (component instanceof Radio && ((Radio) component).isChecked()) {
							ada = true;
							break;
						}
					}
					if (!ada) {
						MyMessageboxConfig.show(
								"Mohon Bapak/Ibu terlebih dahulu memilih Disposisi selanjutnya. Langkah yang dapat dilakukan: (1) periksa daftar pilihan disposisi yang tersedia; (2) pilih tujuan disposisi berikutnya; (3) lanjutkan menyimpan disposisi.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						return false;
					}
				}

				if (vboxPilihan != null && !vboxPilihan.getChildren().isEmpty() && rowProses != null && rowProses.isVisible()) {
					boolean ada = false;
					List<Component> components = vboxPilihan.getChildren();
					for (Component component : components) {
						if (component instanceof Checkbox && ((Checkbox) component).isChecked()) {
							ada = true;
							break;
						}
					}
					if (!ada) {
						MyMessageboxConfig.show(
								"Mohon Bapak/Ibu terlebih dahulu memilih Disposisi selanjutnya. Langkah yang dapat dilakukan: (1) periksa daftar pilihan disposisi yang tersedia; (2) pilih tujuan disposisi berikutnya; (3) lanjutkan menyimpan disposisi.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						return false;
					}
				}
			}
		}

		if (alurSop != null && alurSop.getCatatanWajibDiisi() && keterangan.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show(
					"Mohon Bapak/Ibu terlebih dahulu mengisi Catatan. Langkah yang dapat dilakukan: (1) klik kolom Catatan; (2) isikan catatan disposisi secara jelas; (3) lanjutkan menyimpan disposisi.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (waktu.getValue() == null) {
			MyMessageboxConfig.show(
					"Mohon Bapak/Ibu terlebih dahulu mengisi Waktu Pengajuan SOP. Langkah yang dapat dilakukan: (1) klik kolom Waktu Pengajuan; (2) pilih tanggal dan waktu pengajuan; (3) lanjutkan menyimpan disposisi.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (parameterTambahanListener != null && !parameterTambahanListener.validate()) {
			return false;
		}

		if (dokumen != null) {
			for (DokumenAlurSop dokumenAlurSop : dokumen) {
				if (dokumenAlurSop.getAktif() && dokumenAlurSop.getWajib()) {
					if (!alurSop.getBekukanFormTampilan()) {
						LampiranLain fileFotoLain = (LampiranLain) FileFotoLain.ambil(false, ref, DokumenAlurSop.class.getName() + "_alur_" + dokumenAlurSop.getId(), LampiranLain.class);
						if (fileFotoLain == null) {
							MyMessageboxConfig.showFormat(
									"Mohon Bapak/Ibu terlebih dahulu mengunggah dokumen \"{V1}\" yang bersifat wajib. Langkah yang dapat dilakukan: (1) klik tombol unggah pada dokumen tersebut; (2) pilih berkas dokumen yang sesuai; (3) lanjutkan menyimpan disposisi.",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, dokumenAlurSop.getNama());
							return false;
						}
					}
				}
			}
		}
		return true;
	}

	public static void checkAndSave(Session session, DisposisiAlurSop disposisiAlurSopTerakhir,
			DisposisiAlurSop disposisiAlurSopSetujui, DisposisiSop disposisiSop) {

		if (disposisiSop == null) return;
		boolean ada = false;
		Long endId = (disposisiSop.getDisposisiEnd() == null || disposisiSop.getDisposisiEnd().getId() == null) ? null : disposisiSop.getDisposisiEnd().getId();
		if (endId == null
				|| (disposisiAlurSopTerakhir != null && !endId.equals(disposisiAlurSopTerakhir.getId()))) {
			disposisiSop.setDisposisiEnd(disposisiAlurSopTerakhir);
			ada = true;
		}

		if (disposisiAlurSopSetujui != null && disposisiAlurSopSetujui.getId() != null) {
			if (disposisiSop.getDisposisiSetuju() == null
					|| (disposisiSop.getDisposisiSetuju() != null && disposisiAlurSopSetujui != null && !disposisiSop.getDisposisiSetuju().getId().equals(disposisiAlurSopSetujui.getId()))) {
				ada = true;
				disposisiSop.setDisposisiSetuju(disposisiAlurSopSetujui);
			}
		}

		if (ada) {
			Common.refreshUpdate(session, disposisiSop);
		}
	}

	public boolean onSave(Event event) throws Exception {
		if (!check()) {
			return false;
		}

		Session sessionUtama = null;
		boolean baru = false;

		try {
			sessionUtama = HibernateUtil.getSessionFactory().openSession();
			sessionUtama.getTransaction().begin();

			tbmuser = Common.getCurrentUser();

			// Tangkap identitas ASLI langkah dari DB SEBELUM dimodifikasi. Bila ADMIN (Common.getApakahAdmin)
			// menyunting langkah yang SUDAH ADA, identitas (aktor/pengaju) ini akan DIKEMBALIKAN di akhir agar
			// TIDAK tersimpan sebagai akun admin yang login — langkah tetap atas nama aktor asli alur sebelumnya.
			ais.database.model.Tbmuser pengajuAsli = null;
			String usernamePenggunaAsli = null;
			boolean adminEditLangkahLama = false;
			if (disposisiAlurSop != null && disposisiAlurSop.getId() != null && Common.getApakahAdmin()) {
				try {
					DisposisiAlurSop asliDb = (DisposisiAlurSop) sessionUtama.get(DisposisiAlurSop.class, disposisiAlurSop.getId());
					if (asliDb != null) {
						pengajuAsli = asliDb.getDiajukanOleh();
						usernamePenggunaAsli = asliDb.getUsernamePengguna();
						adminEditLangkahLama = true;
					}
				} catch (Exception ignoreAsli) { ais.common.ErrorAuditUtil.record(ignoreAsli, "auto-audit(empty-catch) src/ais/action/master/sop/DisposisiAlurSopAction.java:1567");
				}
			}

			if (disposisiAlurSop != null && disposisiAlurSop.getId() != null) {
				// FIX (ERROR ObjectNotFoundException "No row with the given identifier exists"):
				// session.load() mengembalikan PROXY tanpa memverifikasi baris masih ada -- baru
				// gagal belakangan (mis. saat setUsernamePengguna() di bawah memicu inisialisasi
				// lazy) bila baris sudah terhapus (mis. dibatalkan pengguna lain). session.get()
				// langsung query & mengembalikan null bila baris sudah tidak ada, sehingga bisa
				// ditangani dengan pesan yang jelas alih-alih ObjectNotFoundException mentah.
				DisposisiAlurSop disposisiAlurSopDb = (DisposisiAlurSop) sessionUtama.get(DisposisiAlurSop.class,
						disposisiAlurSop.getId());
				if (disposisiAlurSopDb == null) {
					MyMessageboxConfig.show(
							"Data disposisi ini sudah tidak ada (mungkin sudah dibatalkan/dihapus pengguna lain). "
									+ "Silakan muat ulang halaman.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
				disposisiAlurSop = disposisiAlurSopDb;
			} else {
				baru = true;
				disposisiAlurSop = new DisposisiAlurSop(hboxAktor.getAttribute("usernamePengguna"));
			}

			disposisiAlurSop.setUsernamePengguna(hboxAktor.getAttribute("usernamePengguna") == null ? "" : hboxAktor.getAttribute("usernamePengguna").toString());
			disposisiAlurSop.setDiajukanOleh(tbmuser);
			if (tbmuser != null) {
				disposisiAlurSop.setMahasiswa(tbmuser.getMahasiswa());
				disposisiAlurSop.setSiswa(tbmuser.getSiswa());
			}
			disposisiAlurSop.setDisposisiSop(disposisiSop);
			disposisiAlurSop.setAlurSop(alurSop);
			disposisiAlurSop.setWaktu(waktu.getValue());
			disposisiAlurSop.setKeterangan(keterangan.getValue());

			// ADMIN menyunting langkah lama: KEMBALIKAN identitas asli (aktor/pengaju) — jangan jadi akun admin.
			if (adminEditLangkahLama) {
				disposisiAlurSop.setUsernamePengguna(usernamePenggunaAsli == null ? "" : usernamePenggunaAsli);
				disposisiAlurSop.setDiajukanOleh(pengajuAsli);
				disposisiAlurSop.setMahasiswa(pengajuAsli == null ? null : pengajuAsli.getMahasiswa());
				disposisiAlurSop.setSiswa(pengajuAsli == null ? null : pengajuAsli.getSiswa());
			}

			if (parameterTambahanListener != null) {
				parameterTambahanListener.onSave(disposisiAlurSop);
			}
			disposisiAlurSop.setSelesai(setujuiData);
			disposisiAlurSop.setKembali(kembaliData);

			Common.refreshSaveOrUpdate(sessionUtama, disposisiAlurSop);

			if (disposisiAlurSop.getId() != null && !baru) {
				String sql = "delete from disposisi_alur_sop where sebelumnya=" + disposisiAlurSop.getId();
				sessionUtama.createSQLQuery(sql).executeUpdate();
			}

			sessionUtama.getTransaction().commit();
		} catch (org.hibernate.StaleStateException e) {
			if (sessionUtama != null && sessionUtama.getTransaction().isActive()) {
				try { sessionUtama.getTransaction().rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sop/DisposisiAlurSopAction.java:1613");}
			}
			ais.ui.util.MyMessageboxConfig.show(
				"Mohon maaf, data disposisi ini telah diubah oleh pengguna lain sehingga penyimpanan tidak dapat dilanjutkan. Langkah yang dapat dilakukan: (1) muat ulang halaman untuk memperoleh data terbaru; (2) periksa kembali perubahan yang Bapak/Ibu lakukan; (3) simpan kembali disposisi.",
				"Konflik Data", ais.ui.util.MyMessageboxConfig.OK, ais.ui.util.MyMessageboxConfig.EXCLAMATION);
			return false;
		} catch (Exception e) {
			if (sessionUtama != null && sessionUtama.getTransaction().isActive()) sessionUtama.getTransaction().rollback();
			ais.common.Common.tampilErrorJikaAdmin(e);
			// PERBAIKAN: sebelumnya pengguna biasa tidak melihat apa pun bila onSave() gagal
			// (tampilErrorJikaAdmin hanya mencatat ke ErrorLog, tidak menampilkan dialog) --
			// tombol "Setujui"/"Simpan" terkesan tidak berfungsi tanpa penjelasan. Tampilkan
			// pesan generik agar pengguna tahu penyimpanan GAGAL dan bisa melapor ke admin.
			ais.ui.util.MyMessageboxConfig.show(
					"Mohon maaf, disposisi ini gagal disimpan karena terjadi kesalahan pada sistem. Langkah yang dapat dilakukan: (1) muat ulang halaman lalu coba kembali; (2) bila kendala berlanjut, sampaikan ke admin beserta waktu kejadian ini agar dapat ditelusuri lebih lanjut.",
					"Gagal Menyimpan", ais.ui.util.MyMessageboxConfig.OK, ais.ui.util.MyMessageboxConfig.ERROR);
			return false;
		} finally {
			if (sessionUtama != null) { try { sessionUtama.clear(); sessionUtama.disconnect(); sessionUtama.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sop/DisposisiAlurSopAction.java:1624");} }
		}

		if (lampiranCatatanDisposisi != null) {
			Session streamingSession = null;
			try {
				streamingSession = StreamingHibernateUtil.getInstance().currentSession();
				streamingSession.getTransaction().begin();
				streamingSession.refresh(lampiranCatatanDisposisi);
				lampiranCatatanDisposisi.setRef(disposisiAlurSop.getId());
				streamingSession.update(lampiranCatatanDisposisi);
				streamingSession.getTransaction().commit();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				ais.common.Common.tampilErrorJikaAdmin(e);
			} finally {
				StreamingHibernateUtil.getInstance().closeSession();
			}
		}

		if (formSop != null) {
			Common.createDefaultTimer(new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					/*
					 * Update disposisi_sop dijalankan lewat KunciEntityHelper:
					 * (1) semua thread JVM ini yang menyentuh row yang sama antri
					 *     FIFO di aplikasi SEBELUM transaksi DB dibuka (bebas
					 *     deadlock antar thread aplikasi),
					 * (2) row dikunci FOR UPDATE NOWAIT + retry untuk pemegang
					 *     lock dari luar (tidak menggantung sampai
					 *     statement_timeout lalu gagal di tengah batch),
					 * (3) begin/commit/rollback/close diurus helper dalam satu
					 *     transaksi pendek.
					 */
					try {
						ais.database.hibernate.KunciEntityHelper.jalankanDenganKunci(DisposisiSop.class,
								disposisiSop.getId(), new ais.database.hibernate.KunciEntityHelper.PekerjaanTransaksi() {
							@Override
							public void kerjakan(Session sessionData, Object entityTerkunci) throws Exception {
								DisposisiSop disposisiSopOk = (DisposisiSop) entityTerkunci;
								JSONObject jsonObjectData = new JSONObject(
										disposisiSopOk.getProperti() != null ? disposisiSopOk.getProperti() : "{}");

								ais.database.model.sop.DataSop generalValueObject = formSop == null ? null : formSop.ambil();
								if (generalValueObject != null) {
									JSONObject jsonObject = new JSONObject();
									jsonObject.put("id", generalValueObject.getId());
									jsonObject.put("kode", generalValueObject.getKode());
									jsonObject.put("nama", generalValueObject.getNama());
									jsonObject.put("keterangan", keterangan.getValue());

									String key = formSop.ambilClass().getName();
									jsonObjectData.put(key, jsonObject);

									DisposisiAlurSop disposisiAlurSopData = (DisposisiAlurSop) sessionData
											.get(DisposisiAlurSop.class, disposisiAlurSop.getId());
									if (disposisiAlurSopData != null) {
										disposisiAlurSopData.setProperti(jsonObject.toString());
										sessionData.update(disposisiAlurSopData);
									}

									generalValueObject.setDisposisiSop(disposisiSopOk);
									sessionData.update(generalValueObject);
								}

								disposisiSopOk.setProperti(jsonObjectData.toString());
								// Jaga kolom kode DisposisiSop selalu = kode class di properti (selalu tampil).
								ais.action.master.sop.helper.SopKodeUtil.sinkronkanKode(disposisiSopOk);

								if (disposisiSopOk.getDisposisiEnd() == null || (disposisiSopOk.getDisposisiEnd() != null
										&& disposisiSopOk.getDisposisiEnd().getId() < disposisiAlurSop.getId())) {
									disposisiSopOk.setDisposisiEnd(disposisiAlurSop);
								}

								if (disposisiAlurSop.getAlurSop() != null
										&& (disposisiAlurSop.getAlurSop().getJikaProsesDisetujuiMakaSelesai()
												|| disposisiAlurSop.setujui())) {
									disposisiSopOk.setDisposisiSetuju(disposisiAlurSop);
								}

								sessionData.update(disposisiSopOk);
							}
						});
					} catch (Exception e) {
						// Row disposisi_sop sedang dikunci transaksi lain (sudah di-retry 3x oleh
						// KunciEntityHelper, FOR UPDATE NOWAIT). Kontensi sementara — bukan bug logika —
						// jadi minta pengguna mengulang & JANGAN catat sebagai error keras agar audit
						// tidak penuh noise. Error lain tetap dilaporkan seperti biasa.
						String pesanErr = e.getMessage() == null ? "" : e.getMessage();
						if (pesanErr.indexOf("dikunci transaksi lain") >= 0 || pesanErr.indexOf("lock timeout") >= 0
								|| pesanErr.indexOf("could not load an entity") >= 0) {
							MyMessageboxConfig.show(
									"Mohon maaf, data disposisi ini sedang diproses oleh pengguna lain. Langkah yang dapat dilakukan: (1) mohon tunggu beberapa saat; (2) muat ulang halaman bila diperlukan; (3) simpan atau teruskan disposisi kembali.",
									"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						} else {
							ais.common.Common.tampilErrorJikaAdmin(e);
						}
					}
				}
			});
		}

		if (baru) {
			Common.createDefaultTimer(new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					// PERBAIKAN PERFORMA: pembuatan PDF + pengiriman email pada cetakDisposisi
					// sebelumnya dijalankan SINKRON di thread event ZK sehingga klik "Tindak Lanjuti
					// SOP" membekukan halaman hingga belasan menit (apalagi bila penerima banyak atau
					// server email/jaringan lambat). Step disposisi sudah tersimpan sebelum titik ini,
					// sehingga pengiriman notifikasi cukup dijalankan di BACKGROUND THREAD agar UI
					// langsung responsif. cetakDisposisi membuka sesi Hibernate sendiri dan host-URL
					// memakai cache, jadi aman dipanggil di luar konteks ZK. Galat di sini hanya
					// menggagalkan notifikasi, bukan proses inti yang sudah tersimpan.
					new Thread(new Runnable() {
						@Override
						public void run() {
							try {
								TampilanAlurSopAction.cetakDisposisi(disposisiSop, true);
							} catch (Throwable t) {
								t.printStackTrace(); ais.common.ErrorAuditUtil.record(t, "auto-audit src/ais/action/master/sop/DisposisiAlurSopAction.java:1745");
							}
						}
					}).start();
				}
			}, "", false, 2500);
		}

		Session sessionPost = null;
		try {
			sessionPost = HibernateUtil.getSessionFactory().openSession();
			sessionPost.getTransaction().begin();
				try {
					sessionPost.createSQLQuery("SET LOCAL statement_timeout = '120s'").executeUpdate();
				} catch (Exception ignoreStatementTimeoutSetting) { ais.common.ErrorAuditUtil.record(ignoreStatementTimeoutSetting, "auto-audit(empty-catch) src/ais/action/master/sop/DisposisiAlurSopAction.java:1759");
				}

			// JARING PENGAMAN: jalur REVISI (membuat disposisi mundur ke aktor sebelumnya) hanya boleh
			// ditempuh bila langkah ini memang mengizinkan "kembali ke aktor sebelumnya" — kondisi yang
			// SAMA dengan yang memunculkan kontrol "Kembali ke alur disposisi sebelumnya" di form.
			// Tanpa ini, flag kembali yang ter-set keliru (mis. dari pilihan persetujuan) akan membuat
			// disposisi mundur berulang ke pengguna lain.
			boolean bolehKembaliKeSebelumnya = disposisiAlurSop.getAlurSop() != null
					&& Boolean.TRUE.equals(disposisiAlurSop.getAlurSop().getKembaliKeAktorSebelumnya());
			if (bolehKembaliKeSebelumnya && disposisiAlurSop.getKembali() != null && disposisiAlurSop.getKembali()
					&& disposisiAlurSop.getSebelumnya() != null) {
				AlurSop alurSopData = (AlurSop) sessionPost.createCriteria(AlurSop.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.idEq(disposisiAlurSop.getSebelumnya().getAlurSop().getId())).uniqueResult();

				if (alurSopData != null) {
					DisposisiAlurSop disposisiAlurSopSetelah = (DisposisiAlurSop) sessionPost.createCriteria(DisposisiAlurSop.class)
							.add(Restrictions.isNotNull("alurSop"))
							.add(Restrictions.eq("alurSop", alurSopData))
							.add(Restrictions.eq("disposisiSop", disposisiSop))
							.add(Restrictions.eq("sebelumnya", disposisiAlurSop)).setMaxResults(1).uniqueResult();

					if (disposisiAlurSopSetelah == null) {
						disposisiAlurSopSetelah = new DisposisiAlurSop();
						disposisiAlurSopSetelah.setWaktu(waktu.getValue());
						disposisiAlurSopSetelah.setSebelumnya(disposisiAlurSop);
						disposisiAlurSopSetelah.setWaktuMaksimal(waktuMaksimal.getValue());
						disposisiAlurSopSetelah.setDisposisiSop(disposisiSop);
						disposisiAlurSopSetelah.setAlurSop(alurSopData);

						Common.refreshSaveOrUpdate(sessionPost, disposisiAlurSopSetelah);
					}

					final DisposisiAlurSop a = disposisiAlurSopSetelah;
					Common.createDefaultTimerNoBusy((new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							BroadcastHelper.kirimEmailDisposisi(a);
						}
					}));
				}
			} else {
				DisposisiAlurSop disposisiAlurSopSetelah = null;
				if (selanjutnya != null) {
					for (AlurSop alurSopSelanjutnya : selanjutnya) {
						if (alurSopSelanjutnya != null && alurSopSelanjutnya.getId() != null) {
							AlurSop alurSopData = (AlurSop) sessionPost.createCriteria(AlurSop.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(Restrictions.idEq(alurSopSelanjutnya.getId())).uniqueResult();

							if (alurSopData != null) {
								disposisiAlurSopSetelah = (DisposisiAlurSop) sessionPost.createCriteria(DisposisiAlurSop.class)
										.add(Restrictions.isNotNull("alurSop"))
										.add(Restrictions.eq("alurSop", alurSopData))
										.add(Restrictions.eq("disposisiSop", disposisiSop))
										.add(Restrictions.eq("sebelumnya", disposisiAlurSop)).setMaxResults(1).uniqueResult();

								if (disposisiAlurSopSetelah == null) {
									disposisiAlurSopSetelah = new DisposisiAlurSop();
									disposisiAlurSopSetelah.setWaktu(waktu.getValue());
									disposisiAlurSopSetelah.setSebelumnya(disposisiAlurSop);
									disposisiAlurSopSetelah.setWaktuMaksimal(waktuMaksimal.getValue());
									disposisiAlurSopSetelah.setDisposisiSop(disposisiSop);
									disposisiAlurSopSetelah.setAlurSop(alurSopData);

									sessionPost.save(disposisiAlurSopSetelah);
								}

								final DisposisiAlurSop a = disposisiAlurSopSetelah;
								Common.createDefaultTimerNoBusy((new EventListener() {
									@Override
									public void onEvent(Event arg0) throws Exception {
										BroadcastHelper.kirimEmailDisposisi(a);
									}
								}));
							}
						}
					}
				}

				if (disposisiAlurSopSetelah != null) {
					disposisiAlurSop.setSetelahnya(disposisiAlurSopSetelah);
					Common.refreshSaveOrUpdate(sessionPost, disposisiAlurSop);
				}
			}
			sessionPost.getTransaction().commit();
		} catch (Exception e) {
			try {
					if (sessionPost != null && sessionPost.isOpen() && sessionPost.getTransaction() != null
							&& sessionPost.getTransaction().isActive()) {
						sessionPost.getTransaction().rollback();
					}
				} catch (Exception rollbackException) { ais.common.ErrorAuditUtil.record(rollbackException, "auto-audit(empty-catch) src/ais/action/master/sop/DisposisiAlurSopAction.java:1844");
				}
			ais.common.Common.tampilErrorJikaAdmin(e);
		} finally {
			if (sessionPost != null) { try { sessionPost.clear(); sessionPost.disconnect(); sessionPost.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sop/DisposisiAlurSopAction.java:1848");} }
		}

		if (lampiranLains != null && !lampiranLains.isEmpty()) {
			Session streamingSession = null;
			try {
				streamingSession = StreamingHibernateUtil.getInstance().currentSession();
				streamingSession.getTransaction().begin();
				for (LampiranLain lampiranLain : lampiranLains.values()) {
					streamingSession.refresh(lampiranLain);
					lampiranLain.setRef(disposisiAlurSop.getId());
					streamingSession.update(lampiranLain);
				}
				streamingSession.getTransaction().commit();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				ais.common.Common.tampilErrorJikaAdmin(e);
			} finally {
				StreamingHibernateUtil.getInstance().closeSession();
			}
		}

		if (lainMahasiswa != null && !lainMahasiswa.isEmpty()) {
			try {
				Session sessionData = StreamingHibernateUtil.getInstance().currentSession();
				sessionData.getTransaction().begin();
				for (Long id : lainMahasiswa.keySet()) {
					LampiranLain lain = lainMahasiswa.get(id);
					sessionData.refresh(lain);
					lain.setRef(disposisiAlurSop.getId());
					sessionData.update(lain);
				}
				sessionData.getTransaction().commit();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			} finally {
				StreamingHibernateUtil.getInstance().closeSession();
			}
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession(); // Untuk UI Filter dibolehkan
		Criteria criteria = session.createCriteria(DisposisiAlurSop.class)


				.add((start == null || end == null || start.getValue() == null || end.getValue() == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction("date(this_.waktu) between date('"
						+ Common.databaseDateFormat.get().format(start.getValue()) + "') and date('"
						+ Common.databaseDateFormat.get().format(end.getValue()) + "')")))

				.add(Restrictions.isNotNull("alurSop"))
				.add(Restrictions.or(Restrictions.isNotNull("diajukanOleh"),
						Restrictions.or(Restrictions.isNotNull("siswa"), Restrictions.isNotNull("mahasiswa"))))

				// PERBAIKAN 1: Gunakan LEFT_JOIN agar jika data disposisiSop null, data utamanya tidak ikut hilang
				.createAlias("disposisiSop", "disposisiSop", org.hibernate.criterion.CriteriaSpecification.LEFT_JOIN)
				.createAlias("alurSop", "alurSop", org.hibernate.criterion.CriteriaSpecification.LEFT_JOIN)
				.createAlias("alurSop.sop", "sop", org.hibernate.criterion.CriteriaSpecification.LEFT_JOIN)

				.add(Restrictions.or(AktorSop.buatCriterionPengaju(tbmuser, ""),
						AktorSop.buatCriterionPengaju(tbmuser, "disposisiSop")));

		if (order) criteria.addOrder(Order.desc("id"));

		criteria.add(searchnama == null || searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("keyword", searchnama.getValue().trim(), MatchMode.ANYWHERE));

		criteria.add(searchjurusan == null || searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
				? Restrictions.sqlRestriction("1=1")
				: Restrictions.or(Restrictions.isNull("sop.jurusan"), CommonSearchFilterHelper.eqSelectedWithId("sop.jurusan", searchjurusan, false)))

				.add(searchfakultas == null || searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("sop.fakultas"), CommonSearchFilterHelper.eqSelectedWithId("sop.fakultas", searchfakultas, false)))

				.add(searchsekolah == null || searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("sop.sekolah"), CommonSearchFilterHelper.eqSelectedWithId("sop.sekolah", searchsekolah, false)))

				.add(searchyayasan == null || searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("sop.yayasan"), CommonSearchFilterHelper.eqSelectedWithId("sop.yayasan", searchyayasan, false)));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		if (searchnama == null) return;

		Common.initPaging(initCriteria(false), paging);

		List<DisposisiAlurSop> disposisiAlurSopList = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(disposisiAlurSopList);
		grid.setRowRenderer(new DisposisiAlurSopRenderer());
		grid.setModelCheckMobile(strset);
	}
}
