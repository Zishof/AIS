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
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timebox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.master.helper.ParameterTambahanPengajuanListener;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.action.master.sop.helper.SopUtil;
import ais.action.report.Report;
import ais.action.report.format1.akademik.LaporanPengajuan;
import ais.action.report.format1.akademik.LaporanRekapitulasiBerdasarkanIzin;
import ais.action.report.format1.akademik.LaporanRekapitulasiBerdasarkanIzinMahasiswa;
import ais.action.report.format1.akademik.LaporanRekapitulasiBerdasarkanIzinMahasiswaRekap;
import ais.action.report.format1.akademik.LaporanRekapitulasiBerdasarkanIzinMahasiswaRekapTotal;
import ais.action.report.helper.CommonReport;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonSearchFilterHelper;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.UIClassHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.JenisPengajuan;
import ais.database.model.KelompokParameterTambahanPengajuan;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.ParameterTambahan;
import ais.database.model.ParameterTambahanPengajuan;
import ais.database.model.PengajuanMahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.LampiranLain;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiAlurSop;
import ais.database.model.sop.DisposisiSop;
import ais.database.model.surat.NomorSurat;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.FormSop;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyLabelKecilBold;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;
import ais.action.master.helper.FilterLanjutHelper;

public class PengajuanMahasiswaAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, FormSop {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchnim;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	protected Combobox searchTahunAjaran;
	private Combobox searchJenisPengajuan;
	protected Combobox jenisSemester;
	private MyCheckboxConfig persetujuan;
	private MyCheckboxConfig semesterPendek;

	private DisposisiSop disposisiSop;

	private ArrayList<Row> parameterRows;
	private HashMap<String, LampiranLain> lampiranLains;
	private ParameterTambahanPengajuanListener parameterTambahanListener;

	private AmbilDataMahasiswaBanbox mahasiswa;
	private MyDatebox tanggal;
	private Textbox keterangan;
	private Combobox tahunAkademik;
	private Combobox ganjilGenap;
	private Label lblSemester;

	private boolean edit = false;
	private boolean delete = false;

	private PengajuanMahasiswa pengajuanMahasiswa;
	private MyToolbarbuttonConfig add;

	private Tabpanel tabLaporan;

	public void onLaporan(Event event) {
		if (tabLaporan.getChildren().size() == 0) {
			LaporanPengajuan window = new LaporanPengajuan();
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabLaporan);
		}
	}

	private Tabpanel jenisPengajuanTab;

	private boolean tampilkan_gambar_mhs = Common.bolehKonfigurasi("tampilkan_gambar_mhs");

	public void onJenisPengajuan(Event event) {
		if (jenisPengajuanTab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(jenisPengajuanTab);
			MyInclude iframe = new MyInclude("/pages/master/jenis_pengajuan.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel laporanAsrama;

	public void onTampilAsrama(Event event) {
		if (laporanAsrama.getChildren().size() == 0) {
			LaporanRekapitulasiBerdasarkanIzin laporanRekapitulasiAsrama = new LaporanRekapitulasiBerdasarkanIzin();
			laporanRekapitulasiAsrama.setHeight("100%");
			laporanRekapitulasiAsrama.setWidth("100%");
			laporanRekapitulasiAsrama.setParent(laporanAsrama);
		}
	}

	private Tabpanel laporanAsrama1;

	public void onTampilAsrama1(Event event) {
		if (laporanAsrama1.getChildren().size() == 0) {
			LaporanRekapitulasiBerdasarkanIzinMahasiswa laporanRekapitulasiAsrama = new LaporanRekapitulasiBerdasarkanIzinMahasiswa();
			laporanRekapitulasiAsrama.setHeight("100%");
			laporanRekapitulasiAsrama.setWidth("100%");
			laporanRekapitulasiAsrama.setParent(laporanAsrama1);
		}
	}

	private Tabpanel laporanAsrama2;

	public void onTampilAsrama2(Event event) {
		if (laporanAsrama2.getChildren().size() == 0) {
			LaporanRekapitulasiBerdasarkanIzinMahasiswaRekap laporanRekapitulasiAsrama = new LaporanRekapitulasiBerdasarkanIzinMahasiswaRekap();
			laporanRekapitulasiAsrama.setHeight("100%");
			laporanRekapitulasiAsrama.setWidth("100%");
			laporanRekapitulasiAsrama.setParent(laporanAsrama2);
		}
	}

	private Tabpanel laporanAsrama3;

	public void onTampilAsrama3(Event event) {
		if (laporanAsrama3.getChildren().size() == 0) {
			LaporanRekapitulasiBerdasarkanIzinMahasiswaRekapTotal laporanRekapitulasiAsrama = new LaporanRekapitulasiBerdasarkanIzinMahasiswaRekapTotal();
			laporanRekapitulasiAsrama.setHeight("100%");
			laporanRekapitulasiAsrama.setWidth("100%");
			laporanRekapitulasiAsrama.setParent(laporanAsrama3);
		}
	}

	private Tabpanel tabManajemenParameter;

	public void onManajemenParameter(Event event) {
		if (tabManajemenParameter.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabManajemenParameter);
			MyInclude iframe = new MyInclude("/pages/master/parameter_tambahan_pengajuan.zul");
			iframe.setParent(window);
		}
	}

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	private Tbmuser tbmuser = null;
	private Mahasiswa mhs = null;
	private Combobox jenisPengajuan;
	protected LampiranLain lainMahasiswa;
	private MyDatebox tanggalSelesai;

	private Timebox waktuMulai = new ais.ui.util.MyTimebox();
	private Timebox waktuSelesai = new ais.ui.util.MyTimebox();
	private Label kode;

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		tbmuser = Common.getCurrentUser();

		if (execution.getParameter("mahasiswa") != null) {
			mhs = (Mahasiswa) HibernateUtil.currentSession().createCriteria(Mahasiswa.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("mahasiswa")))).uniqueResult();
			searchnama.setValue(mhs.getNama());
			searchnim.setValue(mhs.getNim());
			searchnama.setDisabled(true);
			searchnim.setDisabled(true);
		}

		else if (tbmuser.getMahasiswa() != null) {
			mhs = tbmuser.getMahasiswa();
			searchnama.setValue(tbmuser.getMahasiswa().getNama());
			searchnim.setValue(tbmuser.getMahasiswa().getNim());
			searchnama.setDisabled(true);
			searchnim.setDisabled(true);
		}

		if (mhs != null) {
			jenisPengajuanTab.getLinkedTab().setVisible(false);
			jenisPengajuanTab.setVisible(false);
		}

		if (!Common.getApakahAdmin()) {
			tabManajemenParameter.setVisible(false);
			tabManajemenParameter.getLinkedTab().setVisible(false);

			jenisPengajuanTab.setVisible(false);
			jenisPengajuanTab.getLinkedTab().setVisible(false);
		}

		Common.generateTahunAjaranDanSemua(searchTahunAjaran);

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		Common.insertComboDanSemua(searchJenisPengajuan, "nama", JenisPengajuan.class);

		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GANJIL); }
		jenisSemester.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GENAP); }
		jenisSemester.appendChild(comboitem);

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		if (waktuMulai != null) { waktuMulai.setFormat(Common.timeFormat.get().toPattern()); }
		if (waktuSelesai != null) { waktuSelesai.setFormat(Common.timeFormat.get().toPattern()); }

		String[] contents = new String[] { "id", "jenisPengajuan", "tanggal", "waktuMulai", "tanggalSelesai",
				"waktuSelesai", "mahasiswa", "semester", "tahap", "tahunAkademik", "ganjilGenap", "semesterPendek",
				"keterangan", "persetujuan" };

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, PengajuanMahasiswa.class, contents);
		upload.setVisible(
				(add != null && add.isVisible()) && edit && delete && Common.getCurrentUser().getMahasiswa() == null && mhs == null);
		Common.appendKeToolbar(upload, add, comp);
	        FilterLanjutHelper.setup(comp);
}

	class PengajuanMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@SuppressWarnings("unchecked")
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PengajuanMahasiswa pengajuanMahasiswa = (PengajuanMahasiswa) arg1;
			final Mahasiswa mahasiswa = pengajuanMahasiswa.getMahasiswa();
			Hbox hbox = new Hbox();
			hbox.setParent(arg0);
			if (tampilkan_gambar_mhs) {
				try {
					CommonMedia.tampilkanGambarKecil(mahasiswa).setParent(hbox);
				} catch (Exception e) {
					new MyLabelKecil().setParent(arg0);
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
			}
			Vbox a;
			(a = RevisiHelper.createNewRevisi(PengajuanMahasiswa.class, pengajuanMahasiswa, mahasiswa.getNim()))
					.setParent(hbox);
			a.appendChild(new Label(mahasiswa.getNama()));

			Vbox myvbox = new Vbox();
			myvbox.setParent(a);

			hbox = new Hbox();
			hbox.setParent(myvbox);
			LampiranLain.createDownloadUploadFileLain(hbox, pengajuanMahasiswa.getId(),
					PengajuanMahasiswa.class.getName(), "Lampiran", false, null, null, false, false, false, false);

			new Label(pengajuanMahasiswa.getJenisPengajuan().getNama()).setParent(arg0);

			new Label(pengajuanMahasiswa.getSemester() + ""
					+ (pengajuanMahasiswa.getTahap() == null ? "" : " / Tahap " + pengajuanMahasiswa.getTahap())
					+ (pengajuanMahasiswa.getSemesterPendek() ? "(SP)" : "")).setParent(arg0);
			new Label(pengajuanMahasiswa.getTahunAkademik()).setParent(arg0);
			new Label(mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama()).setParent(arg0);
			new Label(mahasiswa.getJurusan() == null || mahasiswa.getJurusan().getFakultas() == null ? ""
					: mahasiswa.getJurusan().getFakultas().getNama()).setParent(arg0);

			JenisPengajuan j = pengajuanMahasiswa.getJenisPengajuan();
			Session session = HibernateUtil.currentSession();
			session.refresh(j);

			Vbox vbox2 = new Vbox();
			vbox2.setParent(arg0);
			vbox2.appendChild(new Label(pengajuanMahasiswa.getKeterangan()));

			for (KelompokParameterTambahanPengajuan kelompokParameterTambahanPengajuan : j
					.getKelompokParameterTambahanPengajuans()) {

				List<ParameterTambahan> parameterTambahans = ConstantValues.simpleList(
						session.createCriteria(ParameterTambahanPengajuan.class)
								.add(Restrictions.eq("kelompokParameterTambahanPengajuan",
										kelompokParameterTambahanPengajuan))
								.createAlias("parameterTambahan", "parameterTambahan")
								.createAlias("kelompokParameterTambahanPengajuan", "kelompokParameterTambahanPengajuan")
								.add(Restrictions.eq("parameterTambahan.aktif", true))
								.add(Restrictions.eq("kelompokParameterTambahanPengajuanMahasiswa.aktif", true))
								.setProjection(Projections.groupProperty("parameterTambahan.id")),
						ParameterTambahan.class, false);
				Collections.sort(parameterTambahans);

				for (ParameterTambahan parameterTambahan : parameterTambahans) {
					String jenis = kelompokParameterTambahanPengajuan.getId() + "->" + parameterTambahan.getId();

					String val = "";
					String[] spl = pengajuanMahasiswa.getParameterTambahanInds().split("\n");
					for (String d : spl) {
						String[] value = d.split("<=>");
						if (value[0].trim().equalsIgnoreCase(jenis)) {
							val = value.length > 1 ? value[1].trim() : "";
						}
					}
					vbox2.appendChild(new MyLabelKecilBold(parameterTambahan.getLabelInputan()));
					LampiranLain lampiranLain = LampiranLain.ambil(pengajuanMahasiswa.getId(), jenis);

					ParameterTambahan.tampil(vbox2, parameterTambahan, lampiranLain, val);
				}

			}

			if (pengajuanMahasiswa.getDisposisiSop() != null) {
				A aa;
				(aa = new A()).setParent(vbox2);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa, "SOP " + pengajuanMahasiswa.getDisposisiSop().getKeterangan() + " ("
						+ pengajuanMahasiswa.getDisposisiSop().getSop().getNama() + ")");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(pengajuanMahasiswa.getDisposisiSop().getId(), null, null, true,
								arg0.getTarget());
					}
				});
			}

			new Label(((pengajuanMahasiswa.getTanggal() == null ? ""
					: Common.dateFormat1.get().format(pengajuanMahasiswa.getTanggal())))
					+ (pengajuanMahasiswa.getWaktuMulai() == null ? "" : " " + pengajuanMahasiswa.getWaktuMulai() + " ")
					+ (pengajuanMahasiswa.getTanggalSelesai() == null ? ""
							: "sd " + Common.dateFormat1.get().format(pengajuanMahasiswa.getTanggalSelesai()))

					+ (pengajuanMahasiswa.getWaktuSelesai() == null ? "" : " " + pengajuanMahasiswa.getWaktuSelesai())

			).setParent(arg0);
			new Label(pengajuanMahasiswa.getPersetujuan() ? "Sudah" : "Belum").setParent(arg0);

			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Permohonan", "/img/print.png");
			button.setOrient("vertical");
			button.setTooltiptext("Cetak Permohonan");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings("rawtypes")
				@Override
				public void onEvent(Event event) throws Exception {
					Map parameters = ais.common.HashMapGenerator.getRandStringSerializable();
					Common.insertProperty(PengajuanMahasiswa.class, pengajuanMahasiswa, parameters, "");
					DisposisiAlurSop.parameterMap(pengajuanMahasiswa.getDisposisiSop(), parameters);
					parameters.put("id", pengajuanMahasiswa.getId());
					Report.generatePDFReport(Report.PDF, parameters, "Keterangan_Pengajuan",
							ais.ui.util.WaktuUtil.getDate());
				}

			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("Edit", "/img/svg/edit-box-line.svg");
			button.setOrient("vertical");
			if ((tbmuser.getMahasiswa() != null || mhs != null) && pengajuanMahasiswa.getPersetujuan()) {
				button.setDisabled(true);
			}
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(pengajuanMahasiswa);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("Persetujuan", "/img/print.png");
			button.setVisible(pengajuanMahasiswa.getPersetujuan());
			button.setOrient("vertical");
			button.setTooltiptext("Cetak Persetujuan");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings("rawtypes")
				@Override
				public void onEvent(Event event) throws Exception {
					Map parameters = ais.common.HashMapGenerator.getRandStringSerializable();

					// PERBAIKAN: Common.insertProperty(..., "") menulis ulang key "id" sebagai
					// String (lihat ManajemenProperty.java) -- taruh "id" bertipe Long SETELAH
					// insertProperty (sama seperti pola aman di tombol "Permohonan" di atas),
					// bukan sebelum, supaya tidak tertimpa String saat dipakai JasperReports
					// sbg parameter SQL bertipe Long -> ClassCastException.
					Common.insertProperty(PengajuanMahasiswa.class, pengajuanMahasiswa, parameters, "");
					DisposisiAlurSop.parameterMap(pengajuanMahasiswa.getDisposisiSop(), parameters);
					parameters.put("id", pengajuanMahasiswa.getId());

					Report.generatePDFReport(Report.PDF, parameters, "Persetujuan_Pengajuan",
							ais.ui.util.WaktuUtil.getDate());
				}

			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
			button.setOrient("vertical");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
			if (pengajuanMahasiswa.getPersetujuan()) {
				button.setDisabled(true);
			}
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

											Session session = HibernateUtil.currentSession();
											if (SopUtil.hapusDisposisi(session, pengajuanMahasiswa.getDisposisiSop())) {

												Common.refreshDelete(session, pengajuanMahasiswa);

												onSearchDefault(event);
											}
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
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("Cetak", "/img/print.png");
			button.setStyle("font-size:9px;");
			button.setOrient("vertical");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {
					onKHS(pengajuanMahasiswa);
				}

			});
			aksiButtons.add(button);

			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);

		}

	}

	public void onAdd(Event event) throws Exception {

		init(new PengajuanMahasiswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings("deprecation")
	@Override
	public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop disposisiSop,
			final MyToolbarbuttonConfig save, EventListener setujui) throws Exception {
		tahunAkademik = new Combobox();
		Common.generateTahunAjaranDanSemua(tahunAkademik);
		ganjilGenap = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		ganjilGenap.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		ganjilGenap.appendChild(comboitem);

		this.pengajuanMahasiswa = (PengajuanMahasiswa) generalValueObject;
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null)) ? this.disposisiSop : disposisiSop;

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasa("label_mahasiswa")));
		row.appendChild(mahasiswa = new AmbilDataMahasiswaBanbox());
		mahasiswa.setAttribute("mahasiswa", pengajuanMahasiswa.getMahasiswa());
		mahasiswa
				.setValue(pengajuanMahasiswa.getMahasiswa() == null ? "" : pengajuanMahasiswa.getMahasiswa().getNama());
		mahasiswa.setWidth("90%");
		tbmuser = Common.getCurrentUser();
		if (tbmuser.getMahasiswa() != null) {
			mahasiswa.setValue(tbmuser.getMahasiswa().toString());
			mahasiswa.setAttribute("mahasiswa", tbmuser.getMahasiswa());
			mahasiswa.setDisabled(true);
		} else if (mhs != null) {
			mahasiswa.setValue(mhs.toString());
			mahasiswa.setAttribute("mahasiswa", mhs);
			mahasiswa.setDisabled(true);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pengajuan *"));
		row.appendChild(jenisPengajuan = new Combobox());
		Common.insertCombo(jenisPengajuan, "nama", JenisPengajuan.class);
		Common.selectComboItem(jenisPengajuan, pengajuanMahasiswa.getJenisPengajuan());
		jenisPengajuan.setWidth("90%");
		jenisPengajuan.setReadonly(true);

		final MyFormRow rowFile = new MyFormRow();

		rowFile.setParent(rows);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(rowFile);
				rowFile.appendChild(new ais.ui.util.MyLabelConfig("Contoh/Format Pengajuan"));
				rowFile.setVisible(false);
				JenisPengajuan jp = (JenisPengajuan) (jenisPengajuan.getSelectedItem() == null ? null
						: jenisPengajuan.getSelectedItem().getValue());
				if (jp != null) {

					FileFotoLain fileFotoLain = FileFotoLain.ambil(false, jp.getId(), JenisPengajuan.class.getName(),
							LampiranLain.class);

					rowFile.setVisible(fileFotoLain != null);
					Vbox myvbox = new Vbox();
					myvbox.setParent(rowFile);

					Hbox hbox = new Hbox();
					hbox.setParent(myvbox);
					LampiranLain.createDownloadUploadFileLain(hbox, jp.getId(), JenisPengajuan.class.getName(),
							"Contoh/Format Pengajuan", false, null, null, false, false, false, false);
				}
			}
		};
		jenisPengajuan.addEventListener("onChange", eventListener);
		eventListener.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik *"));
		row.appendChild(tahunAkademik);
		Common.selectComboItem(tahunAkademik, pengajuanMahasiswa.getTahunAkademik());
		tahunAkademik.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Ganjil / Genap *"));
		row.appendChild(ganjilGenap);
		Common.selectComboItem(ganjilGenap,
				pengajuanMahasiswa.getGanjilGenap() == null ? Perkuliahan.GANJIL : pengajuanMahasiswa.getGanjilGenap());
		ganjilGenap.setWidth("90%");
		ganjilGenap.setReadonly(true);

		final MyFormRow rowSemester = new MyFormRow();
		rowSemester.setStyle("border:0px;background: transparent;");
		rowSemester.setParent(rows);
		rowSemester.appendChild(new Label(ais.common.Common.getBahasaConfig("Semester")));
		rowSemester.appendChild(lblSemester = new Label(
				pengajuanMahasiswa.getSemester() == null ? "" : pengajuanMahasiswa.getSemester().toString()));

		class SemesterEventListener implements EventListener {

			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub
				Common.clear(lblSemester);
				Integer semesterInt = 0;
				// Guard NPE: saat combo tahunAkademik/ganjilGenap belum/ tidak memiliki item terpilih
				// (mis. combo dibangun ulang / value null lalu onChange menyusul), getSelectedItem()
				// atau getValue() bisa null. Tanpa guard -> NullPointerException (log ECAMPUS).
				if (tahunAkademik.getSelectedItem() == null || tahunAkademik.getSelectedItem().getValue() == null
						|| ganjilGenap.getSelectedItem() == null
						|| ganjilGenap.getSelectedItem().getValue() == null) {
					lblSemester.setValue("");
					return;
				}
				Integer tahun = Integer
						.parseInt(StringUtils.split((String) tahunAkademik.getSelectedItem().getValue(), "/")[0]);

				if (Perkuliahan.GANJIL.equals(ganjilGenap.getSelectedItem().getValue())) {

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

				} else if (Perkuliahan.GENAP.equals(ganjilGenap.getSelectedItem().getValue())) {

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

		SemesterEventListener semesterEventListener = new SemesterEventListener();

		ganjilGenap.addEventListener("onChange", semesterEventListener);
		tahunAkademik.addEventListener("onChange", semesterEventListener);

		row = new MyFormRow();
		row.setVisible(Common.bolehKonfigurasi("terdapat_pengajuan_mahasiswa_sp", Konfigurasi.TIDAK_AKTIF));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(semesterPendek = new MyCheckboxConfig("Semester Pendek"));
		semesterPendek.setChecked(pengajuanMahasiswa.getSemesterPendek());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No. Agenda"));
		row.appendChild(kode = new Label(pengajuanMahasiswa.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan / Alasan *"));
		row.appendChild(keterangan = new Textbox(
				pengajuanMahasiswa.getKeterangan() == null ? "" : pengajuanMahasiswa.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(4);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Persetujuan"));

		if (tbmuser.getMahasiswa() != null || mhs != null) {
			if (pengajuanMahasiswa.getId() == null) {
				pengajuanMahasiswa.setPersetujuan(false);
			}
			row.appendChild(new Label(pengajuanMahasiswa.getPersetujuan() ? "Sudah disetujui" : "Belum mensetujui"));
		} else {
			row.appendChild(persetujuan = new MyCheckboxConfig());
			persetujuan.setChecked(pengajuanMahasiswa.getPersetujuan());
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Permohonan *"));
		row.appendChild(tanggal = new MyDatebox(pengajuanMahasiswa.getTanggal()));
		tanggal.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Waktu Permohonan *"));
		row.appendChild(waktuMulai);
		waktuMulai.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Selesai *"));
		row.appendChild(tanggalSelesai = new MyDatebox(pengajuanMahasiswa.getTanggalSelesai()));
		tanggalSelesai.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Waktu Selesai *"));
		row.appendChild(waktuSelesai);
		waktuSelesai.setWidth("90%");

		try {
			waktuMulai.setValue(
					pengajuanMahasiswa.getWaktuMulai() == null || pengajuanMahasiswa.getWaktuMulai().trim().isEmpty()
							? null
							: Common.timeFormat2.get().parse(pengajuanMahasiswa.getWaktuMulai()));
		} catch (java.text.ParseException e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/PengajuanMahasiswaAction.java:800");
			// format waktu tidak cocok, biarkan null
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try {
			waktuSelesai.setValue(pengajuanMahasiswa.getWaktuSelesai() == null
					|| pengajuanMahasiswa.getWaktuSelesai().trim().isEmpty() ? null
							: Common.timeFormat2.get().parse(pengajuanMahasiswa.getWaktuSelesai()));
		} catch (java.text.ParseException e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/PengajuanMahasiswaAction.java:809");
			// format waktu tidak cocok, biarkan null
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lampiran Pengajuan"));
		Hbox hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, pengajuanMahasiswa.getId(), PengajuanMahasiswa.class.getName(),
				"Lampiran Pengajuan", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lainMahasiswa = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(row);

		Common.initKeterangan(rows,
				"Gunakan Contoh/Format Pengajuan apabila ada. Jika file lampiran pengajuan lebih dari satu file, zip dulu semua file tersebut");

		semesterEventListener.onEvent(null);

		MyFormRow rowLampiran = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(rowLampiran, "2");
		rowLampiran.setParent(rows);

		final Grid gridLampiran = new Grid();
		gridLampiran.setSclass("fgrid");
		gridLampiran.setParent(rowLampiran);

		Columns columns = new Columns();
		columns.setParent(gridLampiran);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		final Rows rowsLampiran = new Rows();
		rowsLampiran.setParent(gridLampiran);

		final EventListener eventListenerJenisPengajuan = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.clear(rowsLampiran);

				JenisPengajuan j = (JenisPengajuan) (jenisPengajuan.getSelectedItem() == null ? null
						: jenisPengajuan.getSelectedItem().getValue());

				if (j != null) {

					if (pengajuanMahasiswa.getId() == null || kode.getValue().isEmpty()) {
						String noAgenda = generateCode(j, false);
						kode.setValue(noAgenda);
					}

					parameterRows = new ArrayList<Row>();
					lampiranLains = new HashMap<String, LampiranLain>();
					HibernateUtil.currentSession().refresh(j);

					Set<KelompokParameterTambahanPengajuan> kelompokParameterTambahanPengajuans = new TreeSet<KelompokParameterTambahanPengajuan>();
					for (KelompokParameterTambahanPengajuan kelompokParameterTambahanPengajuan : j
							.getKelompokParameterTambahanPengajuans()) {
						kelompokParameterTambahanPengajuans.add(kelompokParameterTambahanPengajuan);
					}

					parameterTambahanListener = new ParameterTambahanPengajuanListener(pengajuanMahasiswa,
							kelompokParameterTambahanPengajuans, parameterRows, lampiranLains, rowsLampiran);

					parameterTambahanListener.onEvent(null);
				}
			}

		};

		jenisPengajuan.addEventListener("onChange", eventListenerJenisPengajuan);
		Common.createDefaultTimer(eventListenerJenisPengajuan);

		return grid;
	}

	private String generateCode(JenisPengajuan j, boolean tambah) {

		try {
			if (j == null || j.getNomorSurat() == null) {
				return "";
			}

			Long index = j.getNomorSurat().getGunakanIndexUrut() ? j.getNomorSurat().getNomorIndex() : getindex(j);
			if (tambah) {
				NomorSurat.tambahIndexNomorSurat(j.getNomorSurat());
			}
			String noAgenda = j.getNomorSurat().format(index, tanggal.getValue());
			return noAgenda;
		} catch (Exception e) {
			return "";
		}
	}

	private Long getindex(JenisPengajuan jenisPengajuan) {
		if (jenisPengajuan.getNomorSurat() == null) {
			return 0L;
		}
		Session session = HibernateUtil.currentSession();
		int tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		int bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		Date sekarang = WaktuUtil.getDate();
		Number indexO = (Number) session.createCriteria(PengajuanMahasiswa.class)
				.createAlias("jenisPengajuan", "jenisPengajuan", Criteria.LEFT_JOIN)
				.createAlias("jenisPengajuan.nomorSurat", "nomorSurat", Criteria.LEFT_JOIN)

				.add(jenisPengajuan.getNomorSurat().getUrutBerdasarkanNomor()
						? Restrictions.eq("jenisPengajuan.nomorSurat", jenisPengajuan.getNomorSurat())

						: (jenisPengajuan.getNomorSurat().getUrutBerdasarkanKelompok()
								&& jenisPengajuan.getNomorSurat().getKelompokNomorSurat() != null
										? Restrictions.eq("nomorSurat.kelompokNomorSurat",
												jenisPengajuan.getNomorSurat().getKelompokNomorSurat())
										: Restrictions.sqlRestriction("true")))

				.add(jenisPengajuan.getNomorSurat().getResetUrutanTiapTahun() ? Restrictions.eq("tahun", tahun)
						: Restrictions.sqlRestriction("true"))

				.add(jenisPengajuan.getNomorSurat().getResetUrutanTiapBulan()
						? Restrictions.and(Restrictions.eq("tahun", tahun), Restrictions.eq("bulan", bulan))
						: Restrictions.sqlRestriction("true"))

				.add(jenisPengajuan.getNomorSurat().getResetTiap() != null
						&& (Common.dateFormat8.get().format(jenisPengajuan.getNomorSurat().getResetTiap())
								.equals(Common.dateFormat8.get().format(sekarang))
								|| jenisPengajuan.getNomorSurat().getResetTiap().before(sekarang))
										? Restrictions.ge("tanggal", jenisPengajuan.getNomorSurat().getResetTiap())
										: Restrictions.sqlRestriction("true"))

				.setProjection(Projections.rowCount()).uniqueResult();

		Long index = indexO == null ? null : indexO.longValue();
		if (index == null) {
			index = 0L;
		}
		return ++index;
	}

	private void init(PengajuanMahasiswa pengajuanMahasiswa) throws Exception {

		addWindow.setTitle("Pengajuan Mahasiswa");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		disposisiSop=null;center.appendChild(form(pengajuanMahasiswa, disposisiSop, save, null));

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
		if (jenisPengajuan.getSelectedItem() == null || jenisPengajuan.getSelectedItem().getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Jenis Pengajuan",
					"Kolom Jenis Pengajuan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Jenis Pengajuan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
//		JenisPengajuan jp = (JenisPengajuan) jenisPengajuan.getSelectedItem().getValue();
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

		if (tanggal.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Tanggal Permohonan",
					"Kolom Tanggal Permohonan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Tanggal Permohonan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		
		if (waktuMulai.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Waktu Permohonan",
					"Kolom Waktu Permohonan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Waktu Permohonan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		
		if (tanggalSelesai.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Tanggal Selesai",
					"Kolom Tanggal Selesai belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Tanggal Selesai.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		
		if (waktuSelesai.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Waktu Selesai",
					"Kolom Waktu Selesai belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Waktu Selesai.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		if (keterangan.getValue().trim().isEmpty()) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Keterangan atau alasan",
					"Kolom Keterangan atau alasan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Keterangan atau alasan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		if (parameterTambahanListener != null && !parameterTambahanListener.validate()) {
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (pengajuanMahasiswa.getId() != null) {
			pengajuanMahasiswa = (PengajuanMahasiswa) session.load(PengajuanMahasiswa.class,
					pengajuanMahasiswa.getId());
		}

		pengajuanMahasiswa.setSemester(Integer.parseInt(lblSemester.getValue()));
		pengajuanMahasiswa.setTahunAkademik((String) tahunAkademik.getSelectedItem().getValue());
		pengajuanMahasiswa.setGanjilGenap((String) ganjilGenap.getSelectedItem().getValue());
		pengajuanMahasiswa.setMahasiswa((Mahasiswa) mahasiswa.getAttribute("mahasiswa"));
		pengajuanMahasiswa.setKeterangan(keterangan.getValue());
		pengajuanMahasiswa.setTanggal(tanggal.getValue());

		pengajuanMahasiswa.setSemesterPendek(semesterPendek.isChecked());
		pengajuanMahasiswa.setJenisPengajuan((JenisPengajuan) jenisPengajuan.getSelectedItem().getValue());
		pengajuanMahasiswa.setTanggalSelesai(tanggalSelesai.getValue());

		pengajuanMahasiswa
				.setWaktuMulai(waktuMulai.getValue() == null ? null : Common.timeFormat2.get().format(waktuMulai.getValue()));
		pengajuanMahasiswa.setWaktuSelesai(
				waktuSelesai.getValue() == null ? null : Common.timeFormat2.get().format(waktuSelesai.getValue()));

		if (disposisiSop != null && disposisiSop.getId() != null) {
			pengajuanMahasiswa.setDisposisiSop(disposisiSop);
		}

		if (persetujuan != null) {
			pengajuanMahasiswa.setPersetujuan(persetujuan.isChecked());
		} else {
			pengajuanMahasiswa.setPersetujuan(false);
		}

		if (parameterTambahanListener != null)
			parameterTambahanListener.onSave(pengajuanMahasiswa);

		if (pengajuanMahasiswa.getId() != null) {

			if (pengajuanMahasiswa.getIndex() == null) {
				String noAgenda = generateCode(pengajuanMahasiswa.getJenisPengajuan(), true);
				kode.setValue(noAgenda);
				pengajuanMahasiswa.setKode(noAgenda);
				Long currentIndex = getindex(pengajuanMahasiswa.getJenisPengajuan());
				pengajuanMahasiswa.setIndex(++currentIndex);
			}

			Common.refreshUpdate(session, pengajuanMahasiswa);
		} else {
			if (pengajuanMahasiswa.getKode() == null || pengajuanMahasiswa.getKode().isEmpty()) {
				String noAgenda = generateCode(pengajuanMahasiswa.getJenisPengajuan(), true);
				kode.setValue(noAgenda);
				pengajuanMahasiswa.setKode(noAgenda);
			}

			Long currentIndex = getindex(pengajuanMahasiswa.getJenisPengajuan());
			pengajuanMahasiswa.setIndex(++currentIndex);
			session.save(pengajuanMahasiswa);
		}
		session.flush();

		if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(lainMahasiswa);
				lainMahasiswa.setRef(pengajuanMahasiswa.getId());

				session.getTransaction().begin();
				session.update(lainMahasiswa);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}
		}

		return true;
	}

	@Override
	public String istilah() throws Exception {
		// TODO Auto-generated method stub
		return "Pengajuan Mahasiswa";
	}

	@Override
	public DataSop ambil() throws Exception {
		// TODO Auto-generated method stub
		return pengajuanMahasiswa;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClass() throws Exception {
		// TODO Auto-generated method stub
		return PengajuanMahasiswa.class;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public File cetakData(GeneralValueObject generalValueObject) throws Exception {
		PengajuanMahasiswa pengaduan = (PengajuanMahasiswa) generalValueObject;
		JenisPengajuan j = pengaduan.getJenisPengajuan();
		if (j == null) {
			return null;
		}

		LampiranLain lainMahaadministrasi = LampiranLain.ambil(j.getId(),
				LampiranLain.FILE_JRXML_LAYOUT_JENIS_PENGADUAN);

		if (lainMahaadministrasi == null) {
			return null;
		}

		Map parameters = LaporanPengajuan.generateParameter(j, null, null, pengaduan.getMahasiswa(), pengaduan);

		File file = Report.generateCompileFileReport(Report.PDF, parameters,
				lainMahaadministrasi.ambilFile().getAbsolutePath(), ais.ui.util.WaktuUtil.getDate());
		return file;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void onKHS(PengajuanMahasiswa pengajuan) throws Exception {

		try {

			JenisPengajuan j = pengajuan.getJenisPengajuan();
			if (j == null) {
				return;
			}

			LampiranLain lainMahaadministrasi = LampiranLain.ambil(j.getId(),
					LampiranLain.FILE_JRXML_LAYOUT_JENIS_PENGADUAN);

			if (lainMahaadministrasi == null) {
				MyMessageboxConfig.show("File laporan Pengajuan belum diupload", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
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

			Map parameters = LaporanPengajuan.generateParameter(j, null, null, pengajuan.getMahasiswa(), pengajuan);

			File file = Report.generateCompileFileReport(Report.PDF, parameters,
					lainMahaadministrasi.ambilFile().getAbsolutePath(), ais.ui.util.WaktuUtil.getDate());

			CommonReport.tampilkanReportPDF(center, file);

			window.onModal();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PengajuanMahasiswa.class)

				.add(searchTahunAjaran.getSelectedItem() == null
						|| searchTahunAjaran.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunAkademik", searchTahunAjaran.getSelectedItem().getValue()))

				.add(searchJenisPengajuan.getSelectedItem() == null
						|| searchJenisPengajuan.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("jenisPengajuan", searchJenisPengajuan.getSelectedItem().getValue()))

				.add(jenisSemester.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("ganjilGenap", jenisSemester.getSelectedItem().getValue()))

				.createAlias("mahasiswa", "mahasiswa").createAlias("mahasiswa.jurusan", "jurusan")

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("mahasiswa.jurusan", searchjurusan, false))

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false));

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.add(Restrictions.ilike("mahasiswa.nama", searchnama.getValue(), MatchMode.ANYWHERE))
				.add(Restrictions.ilike("mahasiswa.nim", searchnim.getValue(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PengajuanMahasiswa> pengajuanMahasiswa = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(pengajuanMahasiswa);
		grid.setRowRenderer(new PengajuanMahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

	@Override
	public void setPersetujuan(boolean persetujuan) {

	}

}
