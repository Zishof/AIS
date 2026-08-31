package ais.action.master.sekolah;

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

import ais.action.master.helper.ParameterTambahanPengajuanListener;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.sekolah.helper.AmbilDataSiswaBanbox;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.action.master.sop.helper.SopUtil;
import ais.action.report.Report;
import ais.action.report.format1.akademik.LaporanPengajuan;
import ais.action.report.format1.sekolah.LaporanRekapitulasiBerdasarkanIzinSiswa;
import ais.action.report.format1.sekolah.LaporanRekapitulasiBerdasarkanIzinSiswaRekap;
import ais.action.report.format1.sekolah.LaporanRekapitulasiBerdasarkanIzinSiswaRekapTotal;
import ais.action.report.helper.CommonReport;
import ais.common.Common;
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
import ais.database.model.ParameterTambahan;
import ais.database.model.ParameterTambahanPengajuan;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.PengajuanSiswa;
import ais.database.model.sekolah.Siswa;
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

/**
 * Controller/action ZK untuk pengajuan siswa. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Textbox searchnim}, {@code Combobox
 * searchyayasan}, {@code Combobox searchsekolah}, {@code Combobox searchTahunAjaran}; inisialisasi/lifecycle
 * ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code onTampilAsrama()}, {@code onTampilAsrama1()}, {@code onTampilAsrama2()}, {@code
 * onTampilAsrama3()}, {@code getindex()}, {@code ambil()}); mutasi data ({@code onSave()}, {@code
 * setPersetujuan()}); pelaporan/ekspor ({@code cetakData()}); operasi domain lain ({@code onLaporan()}, {@code
 * onJenisPengajuan()}, {@code onManajemenParameter()}, {@code onAdd()}, {@code form()}, {@code generateCode()}).
 * Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericAutowireComposer
 */
public class PengajuanSiswaAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault, FormSop {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchnim;
	private Combobox searchyayasan;
	private Combobox searchsekolah;
	protected Combobox searchTahunAjaran;
	private Combobox searchJenisPengajuan;
	protected Combobox jenisSemester;
	private MyCheckboxConfig persetujuan;
	private MyCheckboxConfig semesterPendek;

	private DisposisiSop disposisiSop;

	private ArrayList<Row> parameterRows;
	private HashMap<String, LampiranLain> lampiranLains;
	private ParameterTambahanPengajuanListener parameterTambahanListener;

	private AmbilDataSiswaBanbox siswa;
	private MyDatebox tanggal;
	private Textbox keterangan;
	private Combobox tahunAkademik;
	private Combobox ganjilGenap;

	private boolean edit = false;
	private boolean delete = false;

	private PengajuanSiswa pengajuanSiswa;
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
			ais.action.report.format1.sekolah.LaporanRekapitulasiBerdasarkanIzin laporanRekapitulasiAsrama = new ais.action.report.format1.sekolah.LaporanRekapitulasiBerdasarkanIzin();
			laporanRekapitulasiAsrama.setHeight("100%");
			laporanRekapitulasiAsrama.setWidth("100%");
			laporanRekapitulasiAsrama.setParent(laporanAsrama);
		}
	}

	private Tabpanel laporanAsrama1;

	public void onTampilAsrama1(Event event) {
		if (laporanAsrama1.getChildren().size() == 0) {
			LaporanRekapitulasiBerdasarkanIzinSiswa laporanRekapitulasiAsrama = new LaporanRekapitulasiBerdasarkanIzinSiswa();
			laporanRekapitulasiAsrama.setHeight("100%");
			laporanRekapitulasiAsrama.setWidth("100%");
			laporanRekapitulasiAsrama.setParent(laporanAsrama1);
		}
	}

	private Tabpanel laporanAsrama2;

	public void onTampilAsrama2(Event event) {
		if (laporanAsrama2.getChildren().size() == 0) {
			LaporanRekapitulasiBerdasarkanIzinSiswaRekap laporanRekapitulasiAsrama = new LaporanRekapitulasiBerdasarkanIzinSiswaRekap();
			laporanRekapitulasiAsrama.setHeight("100%");
			laporanRekapitulasiAsrama.setWidth("100%");
			laporanRekapitulasiAsrama.setParent(laporanAsrama2);
		}
	}

	private Tabpanel laporanAsrama3;

	public void onTampilAsrama3(Event event) {
		if (laporanAsrama3.getChildren().size() == 0) {
			LaporanRekapitulasiBerdasarkanIzinSiswaRekapTotal laporanRekapitulasiAsrama = new LaporanRekapitulasiBerdasarkanIzinSiswaRekapTotal();
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
	private Siswa mhs = null;
	private Combobox jenisPengajuan;
	protected LampiranLain lainSiswa;
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

		if (execution.getParameter("siswa") != null) {
			mhs = (Siswa) HibernateUtil.currentSession().createCriteria(Siswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("siswa")))).uniqueResult();
			searchnama.setValue(mhs.getNama());
			searchnim.setValue(mhs.getNim());
			searchnama.setDisabled(true);
			searchnim.setDisabled(true);
		}

		else if (tbmuser.getSiswa() != null) {
			mhs = tbmuser.getSiswa();
			searchnama.setValue(tbmuser.getSiswa().getNama());
			searchnim.setValue(tbmuser.getSiswa().getNim());
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

		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

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
				"waktuSelesai", "siswa", "semester", "tahap", "tahunAkademik", "ganjilGenap", "semesterPendek",
				"keterangan", "persetujuan" };

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, PengajuanSiswa.class, contents);
		upload.setVisible(
				(add != null && add.isVisible()) && edit && delete && Common.getCurrentUser().getSiswa() == null && mhs == null);
		Common.appendKeToolbar(upload, add, comp);
	        FilterLanjutHelper.setup(comp);
}

	class PengajuanSiswaRenderer extends ais.ui.util.MyRowRenderer {

		@SuppressWarnings("unchecked")
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PengajuanSiswa pengajuanSiswa = (PengajuanSiswa) arg1;
			final Siswa siswa = pengajuanSiswa.getSiswa();
			Hbox hbox = new Hbox();
			hbox.setParent(arg0);
			if (tampilkan_gambar_mhs) {
				try {
					CommonMedia.tampilkanGambarKecil(siswa).setParent(hbox);
				} catch (Exception e) {
					new MyLabelKecil().setParent(arg0);
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
			}
			Vbox a;
			(a = RevisiHelper.createNewRevisi(PengajuanSiswa.class, pengajuanSiswa, siswa.getNim())).setParent(hbox);
			a.appendChild(new Label(siswa.getNama()));

			Vbox myvbox = new Vbox();
			myvbox.setParent(a);

			hbox = new Hbox();
			hbox.setParent(myvbox);
			LampiranLain.createDownloadUploadFileLain(hbox, pengajuanSiswa.getId(), PengajuanSiswa.class.getName(),
					"Lampiran", false, null, null, false, false, false, false);

			new Label(pengajuanSiswa.getJenisPengajuan().getNama()).setParent(arg0);

			new Label(pengajuanSiswa.getGanjilGenap()).setParent(arg0);
			new Label(pengajuanSiswa.getTahunAkademik()).setParent(arg0);
			new Label(siswa.getSekolah() == null ? "" : siswa.getSekolah().getNama()).setParent(arg0);
			new Label(siswa.getSekolah() == null || siswa.getSekolah().getYayasan() == null ? ""
					: siswa.getSekolah().getYayasan().getNama()).setParent(arg0);

			JenisPengajuan j = pengajuanSiswa.getJenisPengajuan();
			Session session = HibernateUtil.currentSession();
			session.refresh(j);

			Vbox vbox2 = new Vbox();
			vbox2.setParent(arg0);
			vbox2.appendChild(new Label(pengajuanSiswa.getKeterangan()));

			for (KelompokParameterTambahanPengajuan kelompokParameterTambahanPengajuan : j
					.getKelompokParameterTambahanPengajuans()) {

				List<ParameterTambahan> parameterTambahans = ConstantValues.simpleList(
						session.createCriteria(ParameterTambahanPengajuan.class)
								.add(Restrictions.eq("kelompokParameterTambahanPengajuan",
										kelompokParameterTambahanPengajuan))
								.createAlias("parameterTambahan", "parameterTambahan")
								.createAlias("kelompokParameterTambahanPengajuan", "kelompokParameterTambahanPengajuan")
								.add(Restrictions.eq("parameterTambahan.aktif", true))
								.add(Restrictions.eq("kelompokParameterTambahanPengajuanSiswa.aktif", true))
								.setProjection(Projections.groupProperty("parameterTambahan.id")),
						ParameterTambahan.class, false);
				Collections.sort(parameterTambahans);

				for (ParameterTambahan parameterTambahan : parameterTambahans) {
					String jenis = kelompokParameterTambahanPengajuan.getId() + "->" + parameterTambahan.getId();

					String val = "";
					String[] spl = pengajuanSiswa.getParameterTambahanInds().split("\n");
					for (String d : spl) {
						String[] value = d.split("<=>");
						if (value[0].trim().equalsIgnoreCase(jenis)) {
							val = value.length > 1 ? value[1].trim() : "";
						}
					}
					vbox2.appendChild(new MyLabelKecilBold(parameterTambahan.getLabelInputan()));
					LampiranLain lampiranLain = LampiranLain.ambil(pengajuanSiswa.getId(), jenis);

					ParameterTambahan.tampil(vbox2, parameterTambahan, lampiranLain, val);
				}

			}

			if (pengajuanSiswa.getDisposisiSop() != null) {
				A aa;
				(aa = new A()).setParent(vbox2);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa, "SOP " + pengajuanSiswa.getDisposisiSop().getKeterangan() + " ("
						+ pengajuanSiswa.getDisposisiSop().getSop().getNama() + ")");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(pengajuanSiswa.getDisposisiSop().getId(), null, null, true,
								arg0.getTarget());
					}
				});
			}

			new Label(((pengajuanSiswa.getTanggal() == null ? ""
					: Common.dateFormat1.get().format(pengajuanSiswa.getTanggal())))
					+ (pengajuanSiswa.getWaktuMulai() == null ? "" : " " + pengajuanSiswa.getWaktuMulai() + " ")
					+ (pengajuanSiswa.getTanggalSelesai() == null ? ""
							: "sd " + Common.dateFormat1.get().format(pengajuanSiswa.getTanggalSelesai()))

					+ (pengajuanSiswa.getWaktuSelesai() == null ? "" : " " + pengajuanSiswa.getWaktuSelesai())

			).setParent(arg0);
			new Label(pengajuanSiswa.getPersetujuan() ? "Sudah" : "Belum").setParent(arg0);

			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Permohonan", "/img/print.png");
			button.setOrient("vertical");
			button.setTooltiptext("Cetak Permohonan");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings("rawtypes")
				@Override
				public void onEvent(Event event) throws Exception {
//					Map parameters = ais.common.HashMapGenerator.getRandStringSerializable();

					Map parameters = LaporanPengajuan.generateParameter(pengajuanSiswa.getJenisPengajuan(), null, null,
							pengajuanSiswa.getSiswa(), pengajuanSiswa);

					Common.insertProperty(PengajuanSiswa.class, pengajuanSiswa, parameters, "");
					DisposisiAlurSop.parameterMap(pengajuanSiswa.getDisposisiSop(), parameters);
					parameters.put("id", pengajuanSiswa.getId());
					Report.generatePDFReport(Report.PDF, parameters, "Keterangan_Pengajuan",
							ais.ui.util.WaktuUtil.getDate());
				}

			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("Edit", "/img/svg/edit-box-line.svg");
			button.setOrient("vertical");
			if ((tbmuser.getSiswa() != null || mhs != null) && pengajuanSiswa.getPersetujuan()) {
				button.setDisabled(true);
			}
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(pengajuanSiswa);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("Persetujuan", "/img/print.png");
			button.setVisible(pengajuanSiswa.getPersetujuan());
			button.setOrient("vertical");
			button.setTooltiptext("Cetak Persetujuan");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings("rawtypes")
				@Override
				public void onEvent(Event event) throws Exception {
					Map parameters = LaporanPengajuan.generateParameter(pengajuanSiswa.getJenisPengajuan(), null, null,
							pengajuanSiswa.getSiswa(), pengajuanSiswa);
					parameters.put("id", pengajuanSiswa.getId());
					Common.insertProperty(PengajuanSiswa.class, pengajuanSiswa, parameters, "");
					DisposisiAlurSop.parameterMap(pengajuanSiswa.getDisposisiSop(), parameters);
					Report.generatePDFReport(Report.PDF, parameters, "Persetujuan_Pengajuan",
							ais.ui.util.WaktuUtil.getDate());
				}

			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
			button.setOrient("vertical");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
			if (pengajuanSiswa.getPersetujuan()) {
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
											if (SopUtil.hapusDisposisi(session, pengajuanSiswa.getDisposisiSop())) {

												Common.refreshDelete(session, pengajuanSiswa);

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
					onKHS(pengajuanSiswa);
				}

			});
			aksiButtons.add(button);

			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);

		}

	}

	public void onAdd(Event event) throws Exception {

		init(new PengajuanSiswa());
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

		this.pengajuanSiswa = (PengajuanSiswa) generalValueObject;
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null)) ? this.disposisiSop : disposisiSop;

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasa("label_siswa")));
		row.appendChild(siswa = new AmbilDataSiswaBanbox());
		siswa.setAttribute("siswa", pengajuanSiswa.getSiswa());
		siswa.setValue(pengajuanSiswa.getSiswa() == null ? "" : pengajuanSiswa.getSiswa().getNama());
		siswa.setWidth("90%");

		if (tbmuser.getSiswa() != null) {
			siswa.setValue(tbmuser.getSiswa().toString());
			siswa.setAttribute("siswa", tbmuser.getSiswa());
			siswa.setDisabled(true);
		} else if (mhs != null) {
			siswa.setValue(mhs.toString());
			siswa.setAttribute("siswa", mhs);
			siswa.setDisabled(true);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pengajuan *"));
		row.appendChild(jenisPengajuan = new Combobox());
		Common.insertCombo(jenisPengajuan, "nama", JenisPengajuan.class);
		Common.selectComboItem(jenisPengajuan, pengajuanSiswa.getJenisPengajuan());
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
		Common.selectComboItem(tahunAkademik, pengajuanSiswa.getTahunAkademik());
		tahunAkademik.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Ganjil / Genap *"));
		row.appendChild(ganjilGenap);
		Common.selectComboItem(ganjilGenap,
				pengajuanSiswa.getGanjilGenap() == null ? Perkuliahan.GANJIL : pengajuanSiswa.getGanjilGenap());
		ganjilGenap.setWidth("90%");
		ganjilGenap.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(Common.bolehKonfigurasi("terdapat_pengajuan_siswa_sp", Konfigurasi.TIDAK_AKTIF));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(semesterPendek = new MyCheckboxConfig("Semester Pendek"));
		semesterPendek.setChecked(pengajuanSiswa.getSemesterPendek());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No. Agenda"));
		row.appendChild(kode = new Label(pengajuanSiswa.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan / Alasan *"));
		row.appendChild(
				keterangan = new Textbox(pengajuanSiswa.getKeterangan() == null ? "" : pengajuanSiswa.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(4);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Persetujuan"));

		if (tbmuser.getSiswa() != null || mhs != null) {
			if (pengajuanSiswa.getId() == null) {
				pengajuanSiswa.setPersetujuan(false);
			}
			row.appendChild(new Label(pengajuanSiswa.getPersetujuan() ? "Sudah disetujui" : "Belum mensetujui"));
		} else {
			row.appendChild(persetujuan = new MyCheckboxConfig());
			persetujuan.setChecked(pengajuanSiswa.getPersetujuan());
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Permohonan"));
		row.appendChild(tanggal = new MyDatebox(pengajuanSiswa.getTanggal()));
		tanggal.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Waktu Permohonan"));
		row.appendChild(waktuMulai);
		waktuMulai.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Selesai"));
		row.appendChild(tanggalSelesai = new MyDatebox(pengajuanSiswa.getTanggalSelesai()));
		tanggalSelesai.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Waktu Selesai"));
		row.appendChild(waktuSelesai);
		waktuSelesai.setWidth("90%");

		try {
			waktuMulai.setValue(
					pengajuanSiswa.getWaktuMulai() == null || pengajuanSiswa.getWaktuMulai().trim().isEmpty() ? null
							: Common.timeFormat2.get().parse(pengajuanSiswa.getWaktuMulai()));
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try {
			waktuSelesai.setValue(
					pengajuanSiswa.getWaktuSelesai() == null || pengajuanSiswa.getWaktuSelesai().trim().isEmpty() ? null
							: Common.timeFormat2.get().parse(pengajuanSiswa.getWaktuSelesai()));
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lampiran Pengajuan"));
		Hbox hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, pengajuanSiswa.getId(), PengajuanSiswa.class.getName(),
				"Lampiran Pengajuan", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lainSiswa = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(row);

		Common.initKeterangan(rows,
				"Gunakan Contoh/Format Pengajuan apabila ada. Jika file lampiran pengajuan lebih dari satu file, zip dulu semua file tersebut");

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

					if (pengajuanSiswa.getId() == null || kode.getValue().isEmpty()) {
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
					pengajuanSiswa.setJenisPengajuan(j);
					parameterTambahanListener = new ParameterTambahanPengajuanListener(pengajuanSiswa,
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
		Number indexO = (Number) session.createCriteria(PengajuanSiswa.class)
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

	private void init(PengajuanSiswa pengajuanSiswa) throws Exception {

		addWindow.setTitle("Pengajuan Siswa");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		disposisiSop=null;center.appendChild(form(pengajuanSiswa, disposisiSop, save, null));

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
		if (siswa.getAttribute("siswa") == null) {
			MyMessageboxConfig.show("Data siswa harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (jenisPengajuan.getSelectedItem() == null || jenisPengajuan.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Jenis Pengajuan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
//		JenisPengajuan jp = (JenisPengajuan) jenisPengajuan.getSelectedItem().getValue();
		if (tahunAkademik.getSelectedItem() == null) {
			MyMessageboxConfig.show("Tahun Akademik harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (ganjilGenap.getSelectedItem() == null) {
			MyMessageboxConfig.show("Jenis Semester harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (keterangan.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show("Keterangan atau alasan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (parameterTambahanListener != null && !parameterTambahanListener.validate()) {
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (pengajuanSiswa.getId() != null) {
			pengajuanSiswa = (PengajuanSiswa) session.load(PengajuanSiswa.class, pengajuanSiswa.getId());
		}

		pengajuanSiswa.setTahunAkademik((String) tahunAkademik.getSelectedItem().getValue());
		pengajuanSiswa.setGanjilGenap((String) ganjilGenap.getSelectedItem().getValue());
		pengajuanSiswa.setSiswa((Siswa) siswa.getAttribute("siswa"));
		pengajuanSiswa.setKeterangan(keterangan.getValue());
		pengajuanSiswa.setTanggal(tanggal.getValue());

		pengajuanSiswa.setSemesterPendek(semesterPendek.isChecked());
		pengajuanSiswa.setJenisPengajuan((JenisPengajuan) jenisPengajuan.getSelectedItem().getValue());
		pengajuanSiswa.setTanggalSelesai(tanggalSelesai.getValue());

		pengajuanSiswa
				.setWaktuMulai(waktuMulai.getValue() == null ? null : Common.timeFormat2.get().format(waktuMulai.getValue()));
		pengajuanSiswa.setWaktuSelesai(
				waktuSelesai.getValue() == null ? null : Common.timeFormat2.get().format(waktuSelesai.getValue()));

		if (disposisiSop != null && disposisiSop.getId() != null) {
			pengajuanSiswa.setDisposisiSop(disposisiSop);
		}

		if (persetujuan != null) {
			pengajuanSiswa.setPersetujuan(persetujuan.isChecked());
		} else {
			pengajuanSiswa.setPersetujuan(false);
		}

		if (parameterTambahanListener != null)
			parameterTambahanListener.onSave(pengajuanSiswa);

		if (pengajuanSiswa.getId() != null) {

			if (pengajuanSiswa.getIndex() == null) {
				String noAgenda = generateCode(pengajuanSiswa.getJenisPengajuan(), true);
				kode.setValue(noAgenda);
				pengajuanSiswa.setKode(noAgenda);
				Long currentIndex = getindex(pengajuanSiswa.getJenisPengajuan());
				pengajuanSiswa.setIndex(++currentIndex);
			}

			Common.refreshUpdate(session, pengajuanSiswa);
		} else {
			if (pengajuanSiswa.getKode() == null || pengajuanSiswa.getKode().isEmpty()) {
				String noAgenda = generateCode(pengajuanSiswa.getJenisPengajuan(), true);
				kode.setValue(noAgenda);
				pengajuanSiswa.setKode(noAgenda);
			}

			Long currentIndex = getindex(pengajuanSiswa.getJenisPengajuan());
			pengajuanSiswa.setIndex(++currentIndex);
			session.save(pengajuanSiswa);
		}
		session.flush();

		if (lainSiswa != null && lainSiswa.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(lainSiswa);
				lainSiswa.setRef(pengajuanSiswa.getId());

				session.getTransaction().begin();
				session.update(lainSiswa);
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
		return "Pengajuan Siswa";
	}

	@Override
	public DataSop ambil() throws Exception {
		// TODO Auto-generated method stub
		return pengajuanSiswa;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClass() throws Exception {
		// TODO Auto-generated method stub
		return PengajuanSiswa.class;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public File cetakData(GeneralValueObject generalValueObject) throws Exception {
		PengajuanSiswa pengaduan = (PengajuanSiswa) generalValueObject;
		JenisPengajuan j = pengaduan.getJenisPengajuan();
		if (j == null) {
			return null;
		}

		LampiranLain lainMahaadministrasi = LampiranLain.ambil(j.getId(),
				LampiranLain.FILE_JRXML_LAYOUT_JENIS_PENGADUAN);

		if (lainMahaadministrasi == null) {
			return null;
		}

		Map parameters = LaporanPengajuan.generateParameter(j, null, null, pengaduan.getSiswa(), pengaduan);

		File file = Report.generateCompileFileReport(Report.PDF, parameters,
				lainMahaadministrasi.ambilFile().getAbsolutePath(), ais.ui.util.WaktuUtil.getDate());
		return file;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void onKHS(PengajuanSiswa pengajuan) throws Exception {

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

			Map parameters = LaporanPengajuan.generateParameter(j, null, null, pengajuan.getSiswa(), pengajuan);

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
		Criteria criteria = session.createCriteria(PengajuanSiswa.class)

				.add(searchTahunAjaran.getSelectedItem() == null
						|| searchTahunAjaran.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunAkademik", searchTahunAjaran.getSelectedItem().getValue()))

				.add(searchJenisPengajuan.getSelectedItem() == null
						|| searchJenisPengajuan.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("jenisPengajuan", searchJenisPengajuan.getSelectedItem().getValue()))

				.add(jenisSemester.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("ganjilGenap", jenisSemester.getSelectedItem().getValue()))

				.createAlias("siswa", "siswa")

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("siswa.sekolah", searchsekolah, false))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("siswa.yayasan", searchyayasan, false));

		if (order)
			criteria.addOrder(Order.desc("id"));

		criteria.add(Restrictions.ilike("siswa.namaSiswa", searchnama.getValue(), MatchMode.ANYWHERE))
				.add(Restrictions.or(
						Restrictions.ilike("siswa.nomorIndukNasional", searchnim.getValue(), MatchMode.ANYWHERE),
						Restrictions.ilike("siswa.nomorInduk", searchnim.getValue(), MatchMode.ANYWHERE)));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PengajuanSiswa> pengajuanSiswa = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(pengajuanSiswa);
		grid.setRowRenderer(new PengajuanSiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

	@Override
	public void setPersetujuan(boolean persetujuan) {

	}

}
