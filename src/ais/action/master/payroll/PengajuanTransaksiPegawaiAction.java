package ais.action.master.payroll;

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
import ais.action.master.helper.RevisiHelper;
import ais.action.master.payroll.helper.ParameterTambahanPengajuanTransaksiPegawaiListener;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.action.report.Report;
import ais.action.report.format1.payroll.LaporanPengajuanTransaksiPegawai;
import ais.action.report.helper.CommonReport;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.UIClassHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.ParameterTambahan;
import ais.database.model.Pegawai;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.DaftarPengajuanTransfer;
import ais.database.model.file.LampiranLain;
import ais.database.model.payroll.JenisPengajuanTransaksiPegawai;
import ais.database.model.payroll.KelompokParameterTambahanPengajuanTransaksiPegawai;
import ais.database.model.payroll.ParameterTambahanPengajuanTransaksiPegawai;
import ais.database.model.payroll.PengajuanTransaksiPegawai;
import ais.database.model.payroll.TransaksiPegawai;
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
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyLabelKecilBold;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

/**
 * Controller/action ZK untuk pengajuan transaksi pegawai. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchkode}, {@code Textbox searchnama}, {@code Textbox
 * searchpegawai}, {@code AmbilDataSatuanKerjaBanbox searchparent}, {@code boolean edit}; inisialisasi/lifecycle
 * ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code init()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code getindex()}, {@code onSearchDefault()}, {@code ambil()}, {@code ambilClass()});
 * mutasi data ({@code onSave()}, {@code setPersetujuan()}); pelaporan/ekspor ({@code cetakData()}); operasi
 * domain lain ({@code onLaporan()}, {@code onJenisPengajuanTransaksiPegawai()}, {@code onManajemenParameter()},
 * {@code onAdd()}, {@code form()}, {@code generateCode()}). Bagian lain dari kontrak tetap mengikuti kelas induk
 * atau interface yang disebut di atas.</p>
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
public class PengajuanTransaksiPegawaiAction extends GenericAutowireComposer
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

	private boolean edit = false;
	private boolean delete = false;

	private PengajuanTransaksiPegawai pengajuanTransaksiPegawai;
	private MyToolbarbuttonConfig add;
	private MyDatebox waktu;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private Combobox jenisPengajuanTransaksiPegawai;

	private Tabpanel tabJenisPengajuanTransaksiPegawai;
	private Tabpanel tabManajemenParameter;

	private Tabpanel tabLaporan;

	private ArrayList<Row> parameterRows;
	private HashMap<String, LampiranLain> lampiranLains;
	private ParameterTambahanPengajuanTransaksiPegawaiListener parameterTambahanListener;
	private MyTextbox nama;
	private DisposisiSop disposisiSop;
	private Label kode;
	private AmbilDataPegawaiBanbox pegawai;
	private Tbmuser tbmuser;

	private Checkbox searchaktif;

	private List<Long> punyaBawahan = null;
	private List<Long> punyaBawahanDosen = null;

	public PengajuanTransaksiPegawaiAction() {
		super();
	}

	public PengajuanTransaksiPegawaiAction(boolean persetujuan) {
		super();
		this.persetujuan = persetujuan;
	}

	public void onLaporan(Event event) {
		if (tabLaporan.getChildren().size() == 0) {
			LaporanPengajuanTransaksiPegawai window = new LaporanPengajuanTransaksiPegawai(jenis);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabLaporan);
		}
	}

	public void onJenisPengajuanTransaksiPegawai(Event event) {
		if (tabJenisPengajuanTransaksiPegawai.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabJenisPengajuanTransaksiPegawai);
			MyInclude iframe = new MyInclude("/pages/master/payroll/jenis_pengajuan_transaksi_pegawai.zul");
			iframe.setParent(window);
		}
	}

	public void onManajemenParameter(Event event) {
		if (tabManajemenParameter.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabManajemenParameter);
			MyInclude iframe = new MyInclude(
					"/pages/master/payroll/parameter_tambahan_pengajuan_transaksi_pegawai.zul");
			iframe.setParent(window);
		}
	}

	private JenisPengajuanTransaksiPegawai jenis = null;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	private MyDatebox tanggalJatuhTempo;
	private MyDoublebox nilaiTransaksi;
	private MyIntbox jumlahAngsur;

	@SuppressWarnings("unchecked")
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();

		jenis = execution.getParameter("jenis") == null ? null
				: (JenisPengajuanTransaksiPegawai) ConstantValues.ambil(JenisPengajuanTransaksiPegawai.class.getName(),
						Long.parseLong(execution.getParameter("jenis")));

		if (!Common.getApakahAdmin()) {
			tabManajemenParameter.setVisible(false);
			tabManajemenParameter.getLinkedTab().setVisible(false);

			tabJenisPengajuanTransaksiPegawai.setVisible(false);
			tabJenisPengajuanTransaksiPegawai.getLinkedTab().setVisible(false);
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

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		String[] contents = new String[] { "id", "pegawai", "kode", "nama", "waktu", "satuanKerja", "tanggalJatuhTempo",
				"nilaiTransaksi", "jumlahAngsur", "jenisPengajuanTransaksiPegawai", "parameterTambahan",
				"parameterTambahanInds" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(PengajuanTransaksiPegawai.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, PengajuanTransaksiPegawai.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		if (tbmuser != null && tbmuser.getPegawai() != null) {
			Session session = HibernateUtil.currentSession();
			Criterion criterion = Restrictions.or(Restrictions.eq("atasanlangsung", tbmuser.getPegawai()),
					Restrictions.or(Restrictions.eq("atasanlangsung3", tbmuser.getPegawai()),
							Restrictions.eq("atasanlangsung2", tbmuser.getPegawai())));

			Criteria criteria = session.createCriteria(Pegawai.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

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
				List<Long> bawahanJabatan = session.createCriteria(Pegawai.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.or(Restrictions.eq("atasan.id", tbmuser.hakAkses().getJenisJabatan().getId()), Restrictions.or(Restrictions.eq("atasanPendukung.id", tbmuser.hakAkses().getJenisJabatan().getId()), Restrictions.eq("atasanPendukungCadangan.id", tbmuser.hakAkses().getJenisJabatan().getId())))   )
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
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.or(Restrictions.in("atasan.id", pejabats), Restrictions.or( Restrictions.in("atasanPendukung.id", pejabats),  Restrictions.in("atasanPendukungCadangan.id", pejabats))))
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
		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	class PengajuanTransaksiPegawaiRenderer extends ais.ui.util.MyRowRenderer {

		@SuppressWarnings("unchecked")
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PengajuanTransaksiPegawai pengajuanTransaksiPegawai = (PengajuanTransaksiPegawai) arg1;

			Pegawai pegawai = pengajuanTransaksiPegawai.getPegawai();
			Hbox hbox = new Hbox();
			hbox.setParent(arg0);
			CommonMedia.tampilkanGambarKecil(pegawai).setParent(hbox);

			Vbox vbox = new Vbox();
			vbox.setParent(hbox);
			new Label(pegawai.getMycode() == null ? "" : pegawai.getMycode()).setParent(vbox);

			Vbox vbox1 = RevisiHelper.createNewRevisi(Pegawai.class, pegawai,
					pegawai.getDosen() == null ? pegawai.getNama() : pegawai.getDosen().getNama());
			vbox1.setParent(vbox);

			if ((pengajuanTransaksiPegawai.getKode() == null || pengajuanTransaksiPegawai.getKode().isEmpty())
					&& pengajuanTransaksiPegawai.getJenisPengajuanTransaksiPegawai() != null) {
				String noAgenda = generateCode(pengajuanTransaksiPegawai.getJenisPengajuanTransaksiPegawai(), true);
				pengajuanTransaksiPegawai.setKode(noAgenda);
				Long currentIndex = getindex(pengajuanTransaksiPegawai.getJenisPengajuanTransaksiPegawai());
				pengajuanTransaksiPegawai.setIndex(++currentIndex);
				Common.refreshUpdate(pengajuanTransaksiPegawai);
			}

			Vbox a;
			(a = RevisiHelper.createNewRevisi(PengajuanTransaksiPegawai.class, pengajuanTransaksiPegawai,
					Common.dateFormat5.get().format(pengajuanTransaksiPegawai.getWaktu()))).setParent(arg0);
			a.appendChild(new Label(pengajuanTransaksiPegawai.getKode()));

			if (pengajuanTransaksiPegawai.getDiajukanOleh() != null) {
				new Label("Diajukan oleh : " + pengajuanTransaksiPegawai.getDiajukanOleh().getUserNama()).setParent(a);
			}

			if (pengajuanTransaksiPegawai.getDisetujuiOleh() != null) {
				new Label("Disetujui oleh : " + pengajuanTransaksiPegawai.getDisetujuiOleh().getUserNama())
						.setParent(a);
			}
			if (pengajuanTransaksiPegawai.getSetujuiTanggal() != null) {
				new Label("Disetujui tgl : " + Common.dateFormat1.get().format(pengajuanTransaksiPegawai.getSetujuiTanggal()))
						.setParent(a);
			}

			Vbox vbox2 = new Vbox();
			vbox2.setParent(arg0);
			new Label(pengajuanTransaksiPegawai.getJenisPengajuanTransaksiPegawai() == null ? ""
					: pengajuanTransaksiPegawai.getJenisPengajuanTransaksiPegawai().getNama()).setParent(vbox2);

			new Label("Tempo/per-tgl: " + Common.dateFormat1.get().format(pengajuanTransaksiPegawai.getTanggalJatuhTempo()))
					.setParent(vbox2);
			new Label("Jml angsuran : " + Common.numberFormat.get().format(pengajuanTransaksiPegawai.getJumlahAngsur()))
					.setParent(vbox2);
			new Label("Nilai transaksi : " + Common.numberFormat.get().format(pengajuanTransaksiPegawai.getNilaiTransaksi()))
					.setParent(vbox2);

			JenisPengajuanTransaksiPegawai j = pengajuanTransaksiPegawai.getJenisPengajuanTransaksiPegawai();
			Session session = HibernateUtil.currentSession();
			session.refresh(j);

			vbox2 = new Vbox();
			vbox2.setParent(arg0);

			for (KelompokParameterTambahanPengajuanTransaksiPegawai kelompokParameterTambahanPengajuanTransaksiPegawai : j
					.getKelompokParameterTambahanPengajuanTransaksiPegawais()) {

				List<ParameterTambahan> parameterTambahans = ConstantValues.simpleList(
						session.createCriteria(ParameterTambahanPengajuanTransaksiPegawai.class)
								.add(Restrictions.eq("kelompokParameterTambahanPengajuanTransaksiPegawai",
										kelompokParameterTambahanPengajuanTransaksiPegawai))
								.createAlias("parameterTambahan", "parameterTambahan")
								.createAlias("kelompokParameterTambahanPengajuanTransaksiPegawai",
										"kelompokParameterTambahanPengajuanTransaksiPegawai")
								.add(Restrictions.eq("parameterTambahan.aktif", true))
								.add(Restrictions.eq("kelompokParameterTambahanPengajuanTransaksiPegawai.aktif", true))
								.setProjection(Projections.groupProperty("parameterTambahan.id")),
						ParameterTambahan.class, false);
				Collections.sort(parameterTambahans);

				for (ParameterTambahan parameterTambahan : parameterTambahans) {
					String jenis = kelompokParameterTambahanPengajuanTransaksiPegawai.getId() + "->"
							+ parameterTambahan.getId();

					String val = "";
					String[] spl = pengajuanTransaksiPegawai.getParameterTambahanInds().split("\n");
					for (String d : spl) {
						String[] value = d.split("<=>");
						if (value[0].trim().equalsIgnoreCase(jenis)) {
							val = value.length > 1 ? value[1].trim() : "";
						}
					}
					vbox2.appendChild(new MyLabelKecilBold(parameterTambahan.getLabelInputan()));
					LampiranLain lampiranLain = LampiranLain.ambil(pengajuanTransaksiPegawai.getId(), jenis);
					ParameterTambahan.tampil(vbox2, parameterTambahan, lampiranLain, val);
				}

			}

			if (pengajuanTransaksiPegawai.getDisposisiSop() != null) {
				A aa;
				(aa = new A()).setParent(vbox2);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa, "SOP " + pengajuanTransaksiPegawai.getDisposisiSop().getKeterangan() + " ("
						+ pengajuanTransaksiPegawai.getDisposisiSop().getSop().getNama() + ")");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(pengajuanTransaksiPegawai.getDisposisiSop().getId(), null, null,
								true, arg0.getTarget());
					}
				});
			}

			if (tbmuser != null && pengajuanTransaksiPegawai.getDisposisiSop() == null && tbmuser.getPegawai() != null
					&& pengajuanTransaksiPegawai.getPegawai() != null
					&& !pengajuanTransaksiPegawai.getPegawai().getId().equals(tbmuser.getPegawai().getId()) && (

					(punyaBawahan != null && punyaBawahan.contains(pengajuanTransaksiPegawai.getPegawai().getId())) ||

							(pengajuanTransaksiPegawai.getPegawai().getAtasanlangsung() != null && tbmuser.getPegawai()
									.getId().equals(pengajuanTransaksiPegawai.getPegawai().getAtasanlangsung().getId()))

							||

							(pengajuanTransaksiPegawai.getPegawai().getAtasanlangsung2() != null
									&& tbmuser.getPegawai().getId().equals(
											pengajuanTransaksiPegawai.getPegawai().getAtasanlangsung2().getId()))

							||

							(pengajuanTransaksiPegawai.getPegawai().getAtasanlangsung3() != null
									&& tbmuser.getPegawai().getId().equals(
											pengajuanTransaksiPegawai.getPegawai().getAtasanlangsung3().getId()))

					)

			) {

				final MyCheckboxConfig checkbox = new MyCheckboxConfig("Setujui");
				checkbox.setChecked(pengajuanTransaksiPegawai.getSetujui());
				checkbox.setParent(arg0);
				arg0.setAttribute("checkbox", checkbox);
				checkbox.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event a) throws Exception {
						pengajuanTransaksiPegawai.setSetujui(checkbox.isChecked());

						if (checkbox.isChecked()) {
							pengajuanTransaksiPegawai.setDisetujuiOleh(tbmuser);
							pengajuanTransaksiPegawai.setSetujuiTanggal(WaktuUtil.getDate());
						} else {
							pengajuanTransaksiPegawai.setDisetujuiOleh(null);
							pengajuanTransaksiPegawai.setSetujuiTanggal(null);
						}

						Session session = HibernateUtil.currentSession();
						Common.refreshSaveOrUpdate(session, pengajuanTransaksiPegawai);
						populateTransaksi(session, pengajuanTransaksiPegawai);

						Common.clear(arg0);
						render(arg0, pengajuanTransaksiPegawai);
					}
				});

			} else {
				new Label(pengajuanTransaksiPegawai.getSetujui() ? "Ya" : "Tidak").setParent(arg0);
			}

			// Kolom aksi rapi (pola MahasiswaAction): semua tombol dibungkus kebab popup (⋯)
			// via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten antar layar.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			Hbox d = Common.copyEditDeleteButtons(edit, delete, pengajuanTransaksiPegawai,
					PengajuanTransaksiPegawaiAction.this);
			aksiButtons.addAll(ais.ui.util.UIHelper.ambilItemAksi(d));

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setStyle("font-size:9px;");
			button.setOrient("vertical");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {
					onKHS(pengajuanTransaksiPegawai);
				}

			});
			aksiButtons.add(button);

			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new PengajuanTransaksiPegawai());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		pengajuanTransaksiPegawai = (PengajuanTransaksiPegawai) obj;
		init(pengajuanTransaksiPegawai);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings("deprecation")
	@Override
	public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop disposisiSop,
			final MyToolbarbuttonConfig save, EventListener setujui) throws Exception {
		this.pengajuanTransaksiPegawai = (PengajuanTransaksiPegawai) generalValueObject;
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null)) ? this.disposisiSop : disposisiSop;

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

		Tbmuser tbmuser = Common.getCurrentUser();
		if (pengajuanTransaksiPegawai.getPegawai() == null && tbmuser != null && tbmuser.getPegawai() != null) {
			pengajuanTransaksiPegawai.setPegawai(tbmuser.getPegawai());
		}

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pegawai (*)"));
		row.appendChild(pegawai = new AmbilDataPegawaiBanbox(true));
		pegawai.setAttribute("myValue", pengajuanTransaksiPegawai.getPegawai());
		pegawai.setAttribute("pegawai", pengajuanTransaksiPegawai.getPegawai());
		pegawai.setValue(
				pengajuanTransaksiPegawai.getPegawai() == null ? "" : pengajuanTransaksiPegawai.getPegawai().getNama());
		pegawai.setWidth("90%");

		if (pengajuanTransaksiPegawai.getId() == null) {
			Pegawai pegawaiTerpilih = tbmuser == null ? null : tbmuser.getPegawai();
			if (pegawaiTerpilih != null) {
				pegawai.setAttribute("myValue", pegawaiTerpilih);
				pegawai.setAttribute("pegawai", pegawaiTerpilih);
				pegawai.setValue(pegawaiTerpilih.getNama());
				pegawai.setDisabled(true);
			}
		} else {
			pegawai.setDisabled(true);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Alasan Pengajuan *"));
		nama = new MyTextbox(pengajuanTransaksiPegawai.getNama());
		if (persetujuan) {
			row.appendChild(new Label(pengajuanTransaksiPegawai.getNama()));
		} else {
			row.appendChild(nama);
		}

		nama.setWidth("90%");
		nama.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal dan Waktu Pengajuan *"));

		if (pengajuanTransaksiPegawai.getId() == null) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.set(Calendar.HOUR_OF_DAY, 8);
			calendar.set(Calendar.MINUTE, 0);
			calendar.set(Calendar.SECOND, 0);
			pengajuanTransaksiPegawai.setWaktu(calendar.getTime());
		}
		waktu = new MyDatebox(pengajuanTransaksiPegawai.getWaktu());
		if (persetujuan) {
			row.appendChild(new Label(waktu.getValue() == null ? "" : Common.dateFormat3.get().format(waktu.getValue())));
		} else {
			row.appendChild(waktu);
		}

		waktu.setFormat(Common.dateFormat3.get().toPattern());
		waktu.setCols(6);
		waktu.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Mulai Jatuh Tempo *"));

		tanggalJatuhTempo = new MyDatebox(pengajuanTransaksiPegawai.getTanggalJatuhTempo());

		if (persetujuan) {
			row.appendChild(new Label(tanggalJatuhTempo.getValue() == null ? ""
					: Common.dateFormat1.get().format(tanggalJatuhTempo.getValue())));
		} else {
			row.appendChild(tanggalJatuhTempo);
		}

		tanggalJatuhTempo.setFormat(Common.dateFormat1.get().toPattern());
		tanggalJatuhTempo.setCols(6);
		tanggalJatuhTempo.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nilai Pengajuan *"));
		nilaiTransaksi = new MyDoublebox(pengajuanTransaksiPegawai.getNilaiTransaksi());
		if (persetujuan) {
			row.appendChild(new Label(Common.numberFormat.get().format(pengajuanTransaksiPegawai.getNilaiTransaksi())));
		} else {
			row.appendChild(nilaiTransaksi);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jumlah Angsuran *"));

		jumlahAngsur = new MyIntbox(pengajuanTransaksiPegawai.getJumlahAngsur());
		if (persetujuan) {
			row.appendChild(new Label(Common.numberFormat.get().format(pengajuanTransaksiPegawai.getJumlahAngsur())));
		} else {
			row.appendChild(jumlahAngsur);
		}

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		satuanKerja = new AmbilDataSatuanKerjaBanbox(true);
		satuanKerja.setValue(pengajuanTransaksiPegawai.getSatuanKerja() == null ? ""
				: pengajuanTransaksiPegawai.getSatuanKerja().getNama());
		satuanKerja.setAttribute("satuanKerja", pengajuanTransaksiPegawai.getSatuanKerja());

		if (persetujuan) {
			row.appendChild(new Label(pengajuanTransaksiPegawai.getSatuanKerja() == null ? ""
					: pengajuanTransaksiPegawai.getSatuanKerja().getNama()));
		} else {
			row.appendChild(satuanKerja);
		}

		satuanKerja.setWidth("90%");

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

				}
			}
		};

		pegawai.setEventListener(eventListenerPegawai);
		Common.createDefaultTimer(eventListenerPegawai);

		if (jenis != null) {
			pengajuanTransaksiPegawai.setJenisPengajuanTransaksiPegawai(jenis);
		}

		jenisPengajuanTransaksiPegawai = new Combobox();

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No. Pengajuan"));
		row.appendChild(kode = new Label(pengajuanTransaksiPegawai.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pengajuan *"));

		if (jenis != null || persetujuan) {
			row.appendChild(new Label(jenis.getNama()));
		} else {
			row.appendChild(jenisPengajuanTransaksiPegawai);
		}
		jenisPengajuanTransaksiPegawai.setWidth("90%");
		jenisPengajuanTransaksiPegawai.setReadonly(true);

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

		final EventListener eventListenerJenisPengajuanTransaksiPegawai = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.clear(rowsLampiran);

				JenisPengajuanTransaksiPegawai j = jenis != null ? jenis
						: (JenisPengajuanTransaksiPegawai) (jenisPengajuanTransaksiPegawai.getSelectedItem() == null
								? null
								: jenisPengajuanTransaksiPegawai.getSelectedItem().getValue());

				if (j != null) {

					if (pengajuanTransaksiPegawai.getId() == null) {
						String noAgenda = generateCode(j, false);
						kode.setValue(noAgenda);
					}

					parameterRows = new ArrayList<Row>();
					lampiranLains = new HashMap<String, LampiranLain>();
					HibernateUtil.currentSession().refresh(j);

					Set<KelompokParameterTambahanPengajuanTransaksiPegawai> kelompokParameterTambahanPengajuanTransaksiPegawais = new TreeSet<KelompokParameterTambahanPengajuanTransaksiPegawai>();
					for (KelompokParameterTambahanPengajuanTransaksiPegawai kelompokParameterTambahanPengajuanTransaksiPegawai : j
							.getKelompokParameterTambahanPengajuanTransaksiPegawais()) {
						kelompokParameterTambahanPengajuanTransaksiPegawais
								.add(kelompokParameterTambahanPengajuanTransaksiPegawai);
					}

					parameterTambahanListener = new ParameterTambahanPengajuanTransaksiPegawaiListener(
							pengajuanTransaksiPegawai, kelompokParameterTambahanPengajuanTransaksiPegawais,
							parameterRows, lampiranLains, rowsLampiran, persetujuan);

					parameterTambahanListener.onEvent(null);
				}
			}

		};

		if (jenis != null) {
			pengajuanTransaksiPegawai.setJenisPengajuanTransaksiPegawai(jenis);
		}

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.insertCombo(jenisPengajuanTransaksiPegawai, new String[] { "nama", "kode" }, "keterangan",
						JenisPengajuanTransaksiPegawai.class, Restrictions.eq("aktif", true));
				Common.selectComboItem(jenisPengajuanTransaksiPegawai,
						pengajuanTransaksiPegawai.getJenisPengajuanTransaksiPegawai());

				if (jenis != null) {
					jenisPengajuanTransaksiPegawai.setDisabled(true);
				}

				eventListenerJenisPengajuanTransaksiPegawai.onEvent(arg0);
			}

		};

		jenisPengajuanTransaksiPegawai.addEventListener("onChange", eventListenerJenisPengajuanTransaksiPegawai);
		Common.createDefaultTimer(eventListener);

		return grid;
	}

	private String generateCode(JenisPengajuanTransaksiPegawai j, boolean tambah) {

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

	private Long getindex(JenisPengajuanTransaksiPegawai jenisPengajuanTransaksiPegawai) {
		if (jenisPengajuanTransaksiPegawai.getNomorSurat() == null) {
			return 0L;
		}
		Session session = HibernateUtil.currentSession();
		int tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		int bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		Date sekarang = WaktuUtil.getDate();
		Number indexO = (Number) session.createCriteria(PengajuanTransaksiPegawai.class)
				.createAlias("jenisPengajuanTransaksiPegawai", "jenisPengajuanTransaksiPegawai", Criteria.LEFT_JOIN)
				.createAlias("jenisPengajuanTransaksiPegawai.nomorSurat", "nomorSurat", Criteria.LEFT_JOIN)

				.add(jenisPengajuanTransaksiPegawai.getNomorSurat().getUrutBerdasarkanNomor()
						? Restrictions.eq("jenisPengajuanTransaksiPegawai.nomorSurat",
								jenisPengajuanTransaksiPegawai.getNomorSurat())

						: (jenisPengajuanTransaksiPegawai.getNomorSurat().getUrutBerdasarkanKelompok()
								&& jenisPengajuanTransaksiPegawai.getNomorSurat().getKelompokNomorSurat() != null
										? Restrictions.eq("nomorSurat.kelompokNomorSurat",
												jenisPengajuanTransaksiPegawai.getNomorSurat().getKelompokNomorSurat())
										: Restrictions.sqlRestriction("true")))

				.add(jenisPengajuanTransaksiPegawai.getNomorSurat().getResetUrutanTiapTahun()
						? Restrictions.eq("tahun", tahun)
						: Restrictions.sqlRestriction("true"))

				.add(jenisPengajuanTransaksiPegawai.getNomorSurat().getResetUrutanTiapBulan()
						? Restrictions.and(Restrictions.eq("tahun", tahun), Restrictions.eq("bulan", bulan))
						: Restrictions.sqlRestriction("true"))

				.add(jenisPengajuanTransaksiPegawai.getNomorSurat().getResetTiap() != null
						&& (Common.dateFormat8.get().format(jenisPengajuanTransaksiPegawai.getNomorSurat().getResetTiap())
								.equals(Common.dateFormat8.get().format(sekarang))
								|| jenisPengajuanTransaksiPegawai.getNomorSurat().getResetTiap().before(sekarang))
										? Restrictions.ge("waktu",
												jenisPengajuanTransaksiPegawai.getNomorSurat().getResetTiap())
										: Restrictions.sqlRestriction("true"))

				.setProjection(Projections.rowCount()).uniqueResult();

		Long index = indexO == null ? null : indexO.longValue();
		if (index == null) {
			index = 0L;
		}
		return ++index;
	}

	private void init(final PengajuanTransaksiPegawai pengajuanTransaksiPegawai) throws Exception {
		this.pengajuanTransaksiPegawai = pengajuanTransaksiPegawai;
		addWindow.setTitle(pengajuanTransaksiPegawai.getId() == null ? "Tambah Pengajuan Pegawai" : "Ubah Pengajuan Pegawai");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		disposisiSop=null;center.appendChild(form(pengajuanTransaksiPegawai, disposisiSop, save, null));

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
					"Mohon maaf, data Pegawai wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Pegawai yang sesuai pada pilihan yang tersedia; (2) pastikan pilihan tidak dikosongkan; (3) simpan kembali data ini.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (jenisPengajuanTransaksiPegawai.getSelectedItem() == null && jenis == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, Jenis Pengajuan wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Jenis Pengajuan yang sesuai pada pilihan yang tersedia; (2) pastikan pilihan tidak dikosongkan; (3) simpan kembali data ini.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (parameterTambahanListener != null && !parameterTambahanListener.validate()) {
			return false;
		}
		Pegawai peg = (Pegawai) pegawai.getAttribute("pegawai");
		if (!peg.getAktif()) {
			MyMessageboxConfig.show(
					"Mohon maaf, data pegawai ini berstatus tidak aktif sehingga proses tidak dapat dilanjutkan. Langkah yang dapat dilakukan: (1) pastikan pegawai yang dipilih berstatus aktif; (2) aktifkan kembali data pegawai melalui menu Data Pegawai apabila diperlukan; (3) ulangi kembali proses ini.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}


		Session session = HibernateUtil.currentSession();
		if (pengajuanTransaksiPegawai.getId() != null) {
			pengajuanTransaksiPegawai = (PengajuanTransaksiPegawai) session.load(PengajuanTransaksiPegawai.class,
					pengajuanTransaksiPegawai.getId());

		}

		pengajuanTransaksiPegawai.setNilaiTransaksi(nilaiTransaksi.getValue());
		pengajuanTransaksiPegawai.setJumlahAngsur(jumlahAngsur.getValue());
		pengajuanTransaksiPegawai.setTanggalJatuhTempo(tanggalJatuhTempo.getValue());

		pengajuanTransaksiPegawai.setPegawai((Pegawai) pegawai.getAttribute("pegawai"));
		pengajuanTransaksiPegawai.setWaktu(waktu.getValue());
		pengajuanTransaksiPegawai.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
		pengajuanTransaksiPegawai.setJenisPengajuanTransaksiPegawai(jenis != null ? jenis
				: (JenisPengajuanTransaksiPegawai) (jenisPengajuanTransaksiPegawai.getSelectedItem() == null ? null
						: jenisPengajuanTransaksiPegawai.getSelectedItem().getValue()));

		pengajuanTransaksiPegawai.setNama(nama.getValue());

		if (disposisiSop != null && disposisiSop.getId() != null) {
			pengajuanTransaksiPegawai.setDisposisiSop(disposisiSop);
		}

		parameterTambahanListener.onSave(pengajuanTransaksiPegawai);

		if (pengajuanTransaksiPegawai.getId() != null) {

			if (pengajuanTransaksiPegawai.getIndex() == null) {
				String noAgenda = generateCode(pengajuanTransaksiPegawai.getJenisPengajuanTransaksiPegawai(), true);
				kode.setValue(noAgenda);
				pengajuanTransaksiPegawai.setKode(noAgenda);
				Long currentIndex = getindex(pengajuanTransaksiPegawai.getJenisPengajuanTransaksiPegawai());
				pengajuanTransaksiPegawai.setIndex(++currentIndex);
			}

			Common.refreshUpdate(session, pengajuanTransaksiPegawai);
			session.flush();
		} else {
			if (pengajuanTransaksiPegawai.getKode() == null) {
				String noAgenda = generateCode(pengajuanTransaksiPegawai.getJenisPengajuanTransaksiPegawai(), true);
				kode.setValue(noAgenda);
				pengajuanTransaksiPegawai.setKode(noAgenda);
			}
			pengajuanTransaksiPegawai.setDiajukanOleh(tbmuser);
			Long currentIndex = getindex(pengajuanTransaksiPegawai.getJenisPengajuanTransaksiPegawai());
			pengajuanTransaksiPegawai.setIndex(++currentIndex);
			session.save(pengajuanTransaksiPegawai);
			session.flush();
		}

		populateTransaksi(session, pengajuanTransaksiPegawai);

		if (!lampiranLains.isEmpty()) {
			Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
			streamingSession.getTransaction().begin();
			for (LampiranLain lampiranLain : lampiranLains.values()) {
				streamingSession.refresh(lampiranLain);
				lampiranLain.setRef(pengajuanTransaksiPegawai.getId());
				streamingSession.update(lampiranLain);
			}
			streamingSession.getTransaction().commit();
			StreamingHibernateUtil.getInstance().closeSession();
		}

		return true;
	}

	private void populateTransaksi(Session session, PengajuanTransaksiPegawai pengajuanTransaksiPegawai) {
		session.createSQLQuery("delete from payroll.transaksi_pegawai where pengajuan_transaksi_pegawai="
				+ pengajuanTransaksiPegawai.getId()).executeUpdate();

		if (pengajuanTransaksiPegawai.getSetujui()) {
			for (int i = 1; i <= pengajuanTransaksiPegawai.getJumlahAngsur(); i++) {
				TransaksiPegawai transaksiPegawai = new TransaksiPegawai();
				transaksiPegawai.setPengajuanTransaksiPegawai(pengajuanTransaksiPegawai);
				transaksiPegawai.setKe(i);
				session.save(transaksiPegawai);
				session.flush();
			}

			if (pengajuanTransaksiPegawai.getDaftarPengajuanTransfer() == null) {
				DaftarPengajuanTransfer.simpanPengajuanTransaksiPegawai(pengajuanTransaksiPegawai);
			}
		}
	}

	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private boolean persetujuan = false;

	public Criteria initCriteria(boolean order) {

		Tbmuser tbmuser = Common.getCurrentUser();
		Pegawai existing = null;
		if (tbmuser != null && tbmuser.ambilPegawai() != null) {
			existing = tbmuser.ambilPegawai();
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PengajuanTransaksiPegawai.class)
				.createAlias("pegawai", "pegawai", Criteria.LEFT_JOIN)
				.add(jenis == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("jenisPengajuanTransaksiPegawai", jenis))

				.add(existing == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("pegawai", existing))

				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("setujui"), Restrictions.eq("setujui", false))
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

		if (punyaBawahan != null && !punyaBawahan.isEmpty() && punyaBawahanDosen != null
				&& !punyaBawahanDosen.isEmpty()) {
			criteria

					.createAlias("diajukanOleh", "diajukanOleh", Criteria.LEFT_JOIN)
					.createAlias("pegawai.dosen", "dosen", Criteria.LEFT_JOIN).add(

							Restrictions.or(
									Restrictions.or(Restrictions.in("diajukanOleh.pegawai.id", punyaBawahan),
											Restrictions.in("diajukanOleh.dosen.id", punyaBawahanDosen)),
									Restrictions.or(Restrictions.in("pegawai.id", punyaBawahan),
											Restrictions.in("dosen.id", punyaBawahanDosen)))

					);

			searchparent.setDisabled(true);

		} else if (punyaBawahan != null && !punyaBawahan.isEmpty()) {
			criteria.createAlias("diajukanOleh", "diajukanOleh", Criteria.LEFT_JOIN)
					.add(Restrictions.or(Restrictions.in("diajukanOleh.pegawai.id", punyaBawahan),
							Restrictions.in("pegawai.id", punyaBawahan)));
			searchparent.setDisabled(true);
		} else if (punyaBawahanDosen != null && !punyaBawahanDosen.isEmpty()) {
			criteria.createAlias("diajukanOleh", "diajukanOleh", Criteria.LEFT_JOIN)
					.createAlias("pegawai.dosen", "dosen", Criteria.LEFT_JOIN)
					.add(Restrictions.or(Restrictions.in("diajukanOleh.dosen.id", punyaBawahanDosen),
							Restrictions.in("dosen.id", punyaBawahanDosen)));
			searchparent.setDisabled(true);
		} else {

			SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
			Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
			if (parent != null) {
				satuanKerjas.clear();
				satuanKerjas.add(parent);
				satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
			}
			criteria.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
					: Restrictions.or(Restrictions.isNull("satuanKerja"),
							Restrictions.or(
									parent == null ? Restrictions.isNull("satuanKerja")
											: Restrictions.sqlRestriction("false"),
									Restrictions.in("satuanKerja", satuanKerjas))));
		}

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PengajuanTransaksiPegawai> pengajuanTransaksiPegawai = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(pengajuanTransaksiPegawai);
		grid.setRowRenderer(new PengajuanTransaksiPegawaiRenderer());
		grid.setModelCheckMobile(strset);

	}

	@Override
	public String istilah() throws Exception {
		// TODO Auto-generated method stub
		return "Pengajuan Transaksi Pegawai";
	}

	@Override
	public DataSop ambil() throws Exception {
		// TODO Auto-generated method stub
		return pengajuanTransaksiPegawai;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClass() throws Exception {
		// TODO Auto-generated method stub
		return PengajuanTransaksiPegawai.class;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public File cetakData(GeneralValueObject generalValueObject) throws Exception {
		PengajuanTransaksiPegawai pengaduan = (PengajuanTransaksiPegawai) generalValueObject;
		JenisPengajuanTransaksiPegawai j = pengaduan.getJenisPengajuanTransaksiPegawai();
		if (j == null) {
			return null;
		}

		LampiranLain lainMahaadministrasi = LampiranLain.ambil(j.getId(),
				LampiranLain.FILE_JRXML_LAYOUT_JENIS_PENGAJUAN_TRANSAKSI_PEGAWAI);

		if (lainMahaadministrasi == null) {
			return null;
		}

		Map parameters = LaporanPengajuanTransaksiPegawai.generateParameter(j, null, null, pengaduan.getPegawai(),
				pengaduan, null);

		File file = Report.generateCompileFileReport(Report.PDF, parameters,
				lainMahaadministrasi.ambilFile().getAbsolutePath(), ais.ui.util.WaktuUtil.getDate());
		return file;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void onKHS(PengajuanTransaksiPegawai pengajuanTransaksiPegawai) throws Exception {

		try {

			JenisPengajuanTransaksiPegawai j = pengajuanTransaksiPegawai.getJenisPengajuanTransaksiPegawai();
			if (j == null) {
				return;
			}

			LampiranLain lainMahaadministrasi = LampiranLain.ambil(j.getId(),
					LampiranLain.FILE_JRXML_LAYOUT_JENIS_PENGAJUAN_TRANSAKSI_PEGAWAI);

			if (lainMahaadministrasi == null) {
				MyMessageboxConfig.show(
						"Mohon maaf, berkas (file) laporan Pengajuan Pegawai belum diunggah sehingga laporan tidak dapat ditampilkan. Langkah yang dapat dilakukan: (1) buka pengaturan Jenis Pengajuan Transaksi Pegawai; (2) unggah berkas layout laporan (JRXML) yang sesuai; (3) simpan, lalu ulangi kembali proses ini.",
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

			Map parameters = LaporanPengajuanTransaksiPegawai.generateParameter(j, null, null,
					pengajuanTransaksiPegawai.getPegawai(), pengajuanTransaksiPegawai, null);

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
		this.persetujuan = persetujuan;
	}
}
