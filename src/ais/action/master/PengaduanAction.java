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
import org.zkoss.zul.Html;
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
import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.master.helper.AmbilDataTbmuserBanbox;
import ais.action.master.helper.ParameterTambahanPengaduanListener;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.sekolah.helper.AmbilDataSiswaBanbox;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.action.report.Report;
import ais.action.report.format1.akademik.LaporanPengaduan;
import ais.action.report.helper.CommonReport;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.UIClassHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.JenisPengaduan;
import ais.database.model.KelompokParameterTambahanPengaduan;
import ais.database.model.Mahasiswa;
import ais.database.model.ParameterTambahan;
import ais.database.model.ParameterTambahanPengaduan;
import ais.database.model.Pegawai;
import ais.database.model.Pengaduan;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.Pejabat;
import ais.database.model.sekolah.Siswa;
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

/**
 * Controller/action ZK untuk pengaduan. Tipe ini merupakan titik masuk UI yang menghubungkan event
 * layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchkode}, {@code Textbox searchnama}, {@code boolean edit},
 * {@code boolean delete}, {@code Pengaduan pengaduan}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code
 * doAfterCompose()}, {@code init()}, {@code init()}, {@code initCriteria()}); pembacaan/pencarian ({@code
 * getindex()}, {@code onSearchDefault()}, {@code ambil()}, {@code ambilClass()}); mutasi data ({@code onSave()},
 * {@code setPersetujuan()}); pelaporan/ekspor ({@code cetakData()}); operasi domain lain ({@code onLaporan()},
 * {@code onJenisPengaduan()}, {@code onManajemenParameter()}, {@code onAdd()}, {@code form()}, {@code
 * generateCode()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
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
public class PengaduanAction extends GenericAutowireComposer
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

	private boolean edit = false;
	private boolean delete = false;

	private Pengaduan pengaduan;
	private MyToolbarbuttonConfig add;
	private MyDatebox waktu;
	private Combobox jenisPengaduan;

	private Tabpanel tabJenisPengaduan;
	private Tabpanel tabManajemenParameter;

	private Tabpanel tabLaporan;

	private ArrayList<Row> parameterRows;
	private HashMap<String, LampiranLain> lampiranLains;
	private ParameterTambahanPengaduanListener parameterTambahanListener;
	private MyTextbox nama;
	private Textbox keterangan;
	private DisposisiSop disposisiSop;
	private Label kode;
	private AmbilDataPegawaiBanbox pegawai;
	private Tbmuser tbmuser;

	private Checkbox searchaktif;

	private List<Long> punyaBawahan = null;
	private List<Long> punyaBawahanDosen = null;
	private AmbilDataMahasiswaBanbox mahasiswa;
	private AmbilDataSiswaBanbox siswa;
	private boolean approve;
	private Textbox tanggapan;
	private AmbilDataTbmuserBanbox diajukan;

	public void onLaporan(Event event) {
		if (tabLaporan.getChildren().size() == 0) {
			LaporanPengaduan window = new LaporanPengaduan();
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabLaporan);
		}
	}

	public void onJenisPengaduan(Event event) {
		if (tabJenisPengaduan.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabJenisPengaduan);
			MyInclude iframe = new MyInclude("/pages/master/jenis_pengaduan.zul");
			iframe.setParent(window);
		}
	}

	public void onManajemenParameter(Event event) {
		if (tabManajemenParameter.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabManajemenParameter);
			MyInclude iframe = new MyInclude("/pages/master/parameter_tambahan_pengaduan.zul");
			iframe.setParent(window);
		}
	}

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

		if (!Common.getApakahAdmin()) {
			tabManajemenParameter.setVisible(false);
			tabManajemenParameter.getLinkedTab().setVisible(false);

			tabJenisPengaduan.setVisible(false);
			tabJenisPengaduan.getLinkedTab().setVisible(false);
		}

		tbmuser = Common.getCurrentUser();

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		approve = CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		String[] contents = new String[] { "id", "diajukan", "pegawai", "mahasiswa", "siswa", "kode", "nama", "waktu",
				"jenisPengaduan", "parameterTambahan", "parameterTambahanInds", "keterangan", "tanggapan", "req",
				"res" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(Pengaduan.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, Pengaduan.class, contents);
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
						.add(Restrictions.eq("atasan.id", tbmuser.hakAkses().getJenisJabatan().getId()))
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

	class PengaduanRenderer extends ais.ui.util.MyRowRenderer {

		@SuppressWarnings("unchecked")
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Pengaduan pengaduan = (Pengaduan) arg1;

			Pegawai pegawai = pengaduan.getPegawai();
			Mahasiswa mahasiswa = pengaduan.getMahasiswa();
			Siswa siswa = pengaduan.getSiswa();

			if (mahasiswa != null) {
				Hbox hbox = new Hbox();
				hbox.setParent(arg0);
				CommonMedia.tampilkanGambarKecil(mahasiswa).setParent(hbox);

				Vbox vbox = new Vbox();
				vbox.setParent(hbox);
				new Label(mahasiswa.getNim()).setParent(vbox);

				Vbox vbox1 = RevisiHelper.createNewRevisi(Mahasiswa.class, mahasiswa, mahasiswa.getNama());
				vbox1.setParent(vbox);
			}

			else if (siswa != null) {
				Hbox hbox = new Hbox();
				hbox.setParent(arg0);
				CommonMedia.tampilkanGambarKecil(siswa).setParent(hbox);

				Vbox vbox = new Vbox();
				vbox.setParent(hbox);
				new Label(siswa.getNomorInduk()).setParent(vbox);

				Vbox vbox1 = RevisiHelper.createNewRevisi(Siswa.class, siswa, siswa.getNama());
				vbox1.setParent(vbox);
			}

			else if (pegawai != null) {
				Hbox hbox = new Hbox();
				hbox.setParent(arg0);
				CommonMedia.tampilkanGambarKecil(pegawai).setParent(hbox);

				Vbox vbox = new Vbox();
				vbox.setParent(hbox);
				new Label(pegawai.getMycode() == null ? "" : pegawai.getMycode()).setParent(vbox);

				Vbox vbox1 = RevisiHelper.createNewRevisi(Pegawai.class, pegawai,
						pegawai.getDosen() == null ? pegawai.getNama() : pegawai.getDosen().getNama());
				vbox1.setParent(vbox);
			} else if (pengaduan.getDiajukan() != null) {
				Hbox hbox = new Hbox();
				hbox.setParent(arg0);
				CommonMedia.tampilkanGambarKecil(pengaduan.getDiajukan()).setParent(hbox);

				Vbox vbox = new Vbox();
				vbox.setParent(hbox);
				new Label(pengaduan.getDiajukan().getUserId()).setParent(vbox);

				Vbox vbox1 = RevisiHelper.createNewRevisi(Tbmuser.class, pengaduan.getDiajukan(),
						pengaduan.getDiajukan().getUserNama());
				vbox1.setParent(vbox);
			}

			if ((pengaduan.getKode() == null || pengaduan.getKode().isEmpty())
					&& pengaduan.getJenisPengaduan() != null) {
				String noAgenda = generateCode(pengaduan.getJenisPengaduan(), true);
				pengaduan.setKode(noAgenda);
				Long currentIndex = getindex(pengaduan.getJenisPengaduan());
				pengaduan.setIndex(++currentIndex);
				Common.refreshUpdate(pengaduan);
			}

			Vbox a;
			(a = RevisiHelper.createNewRevisi(Pengaduan.class, pengaduan,
					Common.dateFormat5.get().format(pengaduan.getWaktu()))).setParent(arg0);
			a.appendChild(new Label(pengaduan.getKode()));

			if (pengaduan.getDisetujuiOleh() != null) {
				new Label("Disetujui oleh : " + pengaduan.getDisetujuiOleh().getUserNama()).setParent(a);
			}
			if (pengaduan.getSetujuiTanggal() != null) {
				new Label("Disetujui tgl : " + Common.dateFormat1.get().format(pengaduan.getSetujuiTanggal())).setParent(a);
			}

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new Label(pengaduan.getJenisPengaduan() == null ? "" : pengaduan.getJenisPengaduan().getNama())
					.setParent(vbox);

			JenisPengaduan j = pengaduan.getJenisPengaduan();
			Session session = HibernateUtil.currentSession();
			session.refresh(j);

			Vbox vbox2 = new Vbox();
			vbox2.setParent(arg0);

			for (KelompokParameterTambahanPengaduan kelompokParameterTambahanPengaduan : j
					.getKelompokParameterTambahanPengaduans()) {

				List<ParameterTambahan> parameterTambahans = ConstantValues.simpleList(
						session.createCriteria(ParameterTambahanPengaduan.class)
								.add(Restrictions.eq("kelompokParameterTambahanPengaduan",
										kelompokParameterTambahanPengaduan))
								.createAlias("parameterTambahan", "parameterTambahan")
								.createAlias("kelompokParameterTambahanPengaduan", "kelompokParameterTambahanPengaduan")
								.add(Restrictions.eq("parameterTambahan.aktif", true))
								.add(Restrictions.eq("kelompokParameterTambahanPengaduan.aktif", true))
								.setProjection(Projections.groupProperty("parameterTambahan.id")),
						ParameterTambahan.class, false);
				Collections.sort(parameterTambahans);

				for (ParameterTambahan parameterTambahan : parameterTambahans) {
					String jenis = kelompokParameterTambahanPengaduan.getId() + "->" + parameterTambahan.getId();

					String val = "";
					String[] spl = pengaduan.getParameterTambahanInds().split("\n");
					for (String d : spl) {
						String[] value = d.split("<=>");
						if (value[0].trim().equalsIgnoreCase(jenis)) {
							val = value.length > 1 ? value[1].trim() : "";
						}
					}
					vbox2.appendChild(new MyLabelKecilBold(parameterTambahan.getLabelInputan()));
					LampiranLain lampiranLain = LampiranLain.ambil(pengaduan.getId(), jenis);

					ParameterTambahan.tampil(vbox2, parameterTambahan, lampiranLain, val);
				}

			}

			if (pengaduan.getDisposisiSop() != null) {
				A aa;
				(aa = new A()).setParent(vbox2);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa, "SOP " + pengaduan.getDisposisiSop().getKeterangan() + " ("
						+ pengaduan.getDisposisiSop().getSop().getNama() + ")");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(pengaduan.getDisposisiSop().getId(), null, null, true,
								arg0.getTarget());
					}
				});
			}

			if (!pengaduan.getKeterangan().trim().isEmpty()) {
				new Label(pengaduan.getKeterangan()).setParent(vbox2);
			}

			if (!pengaduan.getTanggapan().trim().isEmpty()) {
				new Html("<hr>").setParent(vbox2);
				new Label(pengaduan.getTanggapan()).setParent(vbox2);
			}

			if (approve && ((tbmuser != null && pengaduan.getDisposisiSop() == null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
					&& tbmuser.getSiswa() == null && (pengaduan.getMahasiswa() != null || pengaduan.getSiswa() != null))

					||

					(

					tbmuser != null && tbmuser.getPegawai() != null && pengaduan.getPegawai() != null
							&& !pengaduan.getPegawai().getId().equals(tbmuser.getPegawai().getId()) && (

							(punyaBawahan != null && punyaBawahan.contains(pengaduan.getPegawai().getId())) ||

									(pengaduan.getPegawai().getAtasanlangsung() != null

											&& tbmuser.getPegawai().getId()
													.equals(pengaduan.getPegawai().getAtasanlangsung().getId()))

									||

									(pengaduan.getPegawai().getAtasanlangsung2() != null && tbmuser.getPegawai().getId()
											.equals(pengaduan.getPegawai().getAtasanlangsung2().getId()))

									||

									(pengaduan.getPegawai().getAtasanlangsung3() != null && tbmuser.getPegawai().getId()
											.equals(pengaduan.getPegawai().getAtasanlangsung3().getId()))

							)

					))) {

				final MyCheckboxConfig checkbox = new MyCheckboxConfig("Setujui");
				checkbox.setChecked(pengaduan.getSetujui());
				checkbox.setParent(arg0);
				arg0.setAttribute("checkbox", checkbox);
				checkbox.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event a) throws Exception {
						pengaduan.setSetujui(checkbox.isChecked());

						if (checkbox.isChecked()) {
							pengaduan.setDisetujuiOleh(tbmuser);
							pengaduan.setSetujuiTanggal(WaktuUtil.getDate());
						} else {
							pengaduan.setDisetujuiOleh(null);
							pengaduan.setSetujuiTanggal(null);
						}

						Common.refreshSaveOrUpdate(pengaduan);
						Common.clear(arg0);
						render(arg0, pengaduan);
					}
				});

			} else {
				new Label(pengaduan.getSetujui() ? "Ya" : "Tidak").setParent(arg0);
			}

			// kebab popup (⋯) via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			Hbox d = Common.copyEditDeleteButtons(edit, delete, pengaduan, PengaduanAction.this);
			aksiButtons.addAll(ais.ui.util.UIHelper.ambilItemAksi(d));

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setStyle("font-size:9px;");
			button.setOrient("vertical");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {
					onKHS(pengaduan);
				}

			});
			aksiButtons.add(button);

			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new Pengaduan());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		pengaduan = (Pengaduan) obj;
		init(pengaduan);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings("deprecation")
	@Override
	public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop disposisiSop,
			final MyToolbarbuttonConfig save, EventListener setujui) throws Exception {
		this.pengaduan = (Pengaduan) generalValueObject;
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

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pengguna (*)"));
		row.appendChild(diajukan = new AmbilDataTbmuserBanbox());
		diajukan.setAttribute("myValue", pengaduan.getDiajukan());
		diajukan.setAttribute("tbmuser", pengaduan.getDiajukan());
		diajukan.setValue(pengaduan.getDiajukan() == null ? "" : pengaduan.getDiajukan().getUserNama());
		diajukan.setWidth("90%");

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pegawai (*)"));
		row.appendChild(pegawai = new AmbilDataPegawaiBanbox());
		pegawai.setAttribute("myValue", pengaduan.getPegawai());
		pegawai.setAttribute("pegawai", pengaduan.getPegawai());
		pegawai.setValue(pengaduan.getPegawai() == null ? "" : pengaduan.getPegawai().getNama());
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Mahasiswa (*)"));
		row.appendChild(mahasiswa = new AmbilDataMahasiswaBanbox());
		mahasiswa.setAttribute("myValue", pengaduan.getMahasiswa());
		mahasiswa.setAttribute("mahasiswa", pengaduan.getMahasiswa());
		mahasiswa.setValue(pengaduan.getMahasiswa() == null ? "" : pengaduan.getMahasiswa().getNama());
		mahasiswa.setWidth("90%");

		Mahasiswa mahasiswaTerpilih = tbmuser == null ? null : tbmuser.getMahasiswa();
		if (mahasiswaTerpilih != null) {
			mahasiswa.setAttribute("myValue", mahasiswaTerpilih);
			mahasiswa.setAttribute("mahasiswa", mahasiswaTerpilih);
			mahasiswa.setValue(mahasiswaTerpilih.getNama());
			mahasiswa.setDisabled(true);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Siswa (*)"));
		row.appendChild(siswa = new AmbilDataSiswaBanbox());
		siswa.setAttribute("myValue", pengaduan.getSiswa());
		siswa.setAttribute("siswa", pengaduan.getSiswa());
		siswa.setValue(pengaduan.getSiswa() == null ? "" : pengaduan.getSiswa().getNama());
		siswa.setWidth("90%");

		Siswa siswaTerpilih = tbmuser == null ? null : tbmuser.getSiswa();
		if (siswaTerpilih != null) {
			siswa.setAttribute("myValue", siswaTerpilih);
			siswa.setAttribute("siswa", siswaTerpilih);
			siswa.setValue(siswaTerpilih.getNama());
			siswa.setDisabled(true);
		}

		EventListener eventListenerData = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Tbmuser t = (Tbmuser) diajukan.getAttribute("tbmuser");
				Mahasiswa m = (Mahasiswa) mahasiswa.getAttribute("mahasiswa");
				Siswa s = (Siswa) siswa.getAttribute("siswa");
				Pegawai p = (Pegawai) pegawai.getAttribute("pegawai");

				diajukan.getParent().setVisible(true);
				mahasiswa.getParent().setVisible(true);
				siswa.getParent().setVisible(true);
				pegawai.getParent().setVisible(true);

				if (t != null) {
					diajukan.getParent().setVisible(true);
					mahasiswa.getParent().setVisible(false);
					siswa.getParent().setVisible(false);
					pegawai.getParent().setVisible(false);
				} else if (m != null) {
					diajukan.getParent().setVisible(false);
					mahasiswa.getParent().setVisible(true);
					siswa.getParent().setVisible(false);
					pegawai.getParent().setVisible(false);
				} else if (s != null) {
					diajukan.getParent().setVisible(false);
					mahasiswa.getParent().setVisible(false);
					siswa.getParent().setVisible(true);
					pegawai.getParent().setVisible(false);
				} else if (p != null) {
					diajukan.getParent().setVisible(false);
					mahasiswa.getParent().setVisible(false);
					siswa.getParent().setVisible(false);
					pegawai.getParent().setVisible(true);
				}
			}
		};

		eventListenerData.onEvent(null);
		pegawai.setEventListener(eventListenerData);
		mahasiswa.setEventListener(eventListenerData);
		siswa.setEventListener(eventListenerData);
		diajukan.setEventListener(eventListenerData);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No. Agenda"));
		row.appendChild(kode = new Label(pengaduan.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul Pengaduan *"));
		row.appendChild(nama = new MyTextbox(pengaduan.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal dan Waktu *"));
		row.appendChild(waktu = new MyDatebox(pengaduan.getWaktu()));
		waktu.setFormat(Common.dateFormat3.get().toPattern());
		waktu.setWidth("90%");
		waktu.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pengaduan *"));
		row.appendChild(jenisPengaduan = new Combobox());
		jenisPengaduan.setWidth("90%");
		jenisPengaduan.setReadonly(true);

		MyFormRow rowUsernameDisposisi = new MyFormRow();
		rowUsernameDisposisi.setParent(rows);
		rowUsernameDisposisi.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		rowUsernameDisposisi.appendChild(keterangan = new Textbox(pengaduan.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(2);

		rowUsernameDisposisi = new MyFormRow();
		rowUsernameDisposisi.setParent(rows);
		rowUsernameDisposisi.appendChild(new ais.ui.util.MyLabelConfig("Tanggapan"));
		rowUsernameDisposisi.appendChild(tanggapan = new Textbox(pengaduan.getTanggapan()));
		tanggapan.setWidth("90%");
		tanggapan.setRows(2);

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

		final EventListener eventListenerJenisPengaduan = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.clear(rowsLampiran);

				JenisPengaduan j = (JenisPengaduan) (jenisPengaduan.getSelectedItem() == null ? null
						: jenisPengaduan.getSelectedItem().getValue());

				if (j != null) {

					if (pengaduan.getId() == null) {
						String noAgenda = generateCode(j, false);
						kode.setValue(noAgenda);
					}

					parameterRows = new ArrayList<Row>();
					lampiranLains = new HashMap<String, LampiranLain>();
					HibernateUtil.currentSession().refresh(j);

					Set<KelompokParameterTambahanPengaduan> kelompokParameterTambahanPengaduans = new TreeSet<KelompokParameterTambahanPengaduan>();
					for (KelompokParameterTambahanPengaduan kelompokParameterTambahanPengaduan : j
							.getKelompokParameterTambahanPengaduans()) {
						kelompokParameterTambahanPengaduans.add(kelompokParameterTambahanPengaduan);
					}

					parameterTambahanListener = new ParameterTambahanPengaduanListener(pengaduan,
							kelompokParameterTambahanPengaduans, parameterRows, lampiranLains, rowsLampiran);

					parameterTambahanListener.onEvent(null);
				}
			}

		};

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.insertCombo(jenisPengaduan, new String[] { "nama", "kode" }, "keterangan", JenisPengaduan.class,
						Restrictions.eq("aktif", true));
				Common.selectComboItem(jenisPengaduan, pengaduan.getJenisPengaduan());

				eventListenerJenisPengaduan.onEvent(arg0);
			}

		};

		jenisPengaduan.addEventListener("onChange", eventListenerJenisPengaduan);
		Common.createDefaultTimer(eventListener);

		return grid;
	}

	private String generateCode(JenisPengaduan j, boolean tambah) {

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

	private Long getindex(JenisPengaduan jenisPengaduan) {
		if (jenisPengaduan.getNomorSurat() == null) {
			return 0L;
		}
		Session session = HibernateUtil.currentSession();
		int tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		int bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		Date sekarang = WaktuUtil.getDate();
		Number indexO = (Number) session.createCriteria(Pengaduan.class)
				.createAlias("jenisPengaduan", "jenisPengaduan", Criteria.LEFT_JOIN)
				.createAlias("jenisPengaduan.nomorSurat", "nomorSurat", Criteria.LEFT_JOIN)

				.add(jenisPengaduan.getNomorSurat().getUrutBerdasarkanNomor()
						? Restrictions.eq("jenisPengaduan.nomorSurat", jenisPengaduan.getNomorSurat())

						: (jenisPengaduan.getNomorSurat().getUrutBerdasarkanKelompok()
								&& jenisPengaduan.getNomorSurat().getKelompokNomorSurat() != null
										? Restrictions.eq("nomorSurat.kelompokNomorSurat",
												jenisPengaduan.getNomorSurat().getKelompokNomorSurat())
										: Restrictions.sqlRestriction("true")))

				.add(jenisPengaduan.getNomorSurat().getResetUrutanTiapTahun() ? Restrictions.eq("tahun", tahun)
						: Restrictions.sqlRestriction("true"))

				.add(jenisPengaduan.getNomorSurat().getResetUrutanTiapBulan()
						? Restrictions.and(Restrictions.eq("tahun", tahun), Restrictions.eq("bulan", bulan))
						: Restrictions.sqlRestriction("true"))

				.add(jenisPengaduan.getNomorSurat().getResetTiap() != null
						&& (Common.dateFormat8.get().format(jenisPengaduan.getNomorSurat().getResetTiap())
								.equals(Common.dateFormat8.get().format(sekarang))
								|| jenisPengaduan.getNomorSurat().getResetTiap().before(sekarang))
										? Restrictions.ge("waktu", jenisPengaduan.getNomorSurat().getResetTiap())
										: Restrictions.sqlRestriction("true"))

				.setProjection(Projections.rowCount()).uniqueResult();

		Long index = indexO == null ? null : indexO.longValue();
		if (index == null) {
			index = 0L;
		}
		return ++index;
	}

	private void init(final Pengaduan pengaduan) throws Exception {
		this.pengaduan = pengaduan;
		addWindow.setTitle(pengaduan.getId() == null ? "Tambah Pengaduan" : "Ubah Pengaduan");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		disposisiSop=null;center.appendChild(form(pengaduan, disposisiSop, save, null));

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

		if (pegawai.getParent().isVisible() && pegawai.getAttribute("pegawai") == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Pegawai",
					"Kolom Pegawai belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Pegawai.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (mahasiswa.getParent().isVisible() && mahasiswa.getAttribute("mahasiswa") == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Mahasiswa",
					"Kolom Mahasiswa belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Mahasiswa.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (siswa.getParent().isVisible() && siswa.getAttribute("siswa") == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Siswa",
					"Kolom Siswa belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Siswa.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (jenisPengaduan.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Jenis Pengaduan",
					"Kolom Jenis Pengaduan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Jenis Pengaduan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		if (parameterTambahanListener != null && !parameterTambahanListener.validate()) {
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (pengaduan.getId() != null) {
			pengaduan = (Pengaduan) session.load(Pengaduan.class, pengaduan.getId());
		}
		pengaduan.setTanggapan(tanggapan.getValue());
		pengaduan.setMahasiswa((Mahasiswa) mahasiswa.getAttribute("mahasiswa"));
		pengaduan.setSiswa((Siswa) siswa.getAttribute("siswa"));
		pengaduan.setPegawai((Pegawai) pegawai.getAttribute("pegawai"));
		pengaduan.setWaktu(waktu.getValue());
		pengaduan.setJenisPengaduan((JenisPengaduan) (jenisPengaduan.getSelectedItem() == null ? null
				: jenisPengaduan.getSelectedItem().getValue()));
		pengaduan.setDiajukan((Tbmuser) diajukan.getAttribute("tbmuser"));
		pengaduan.setNama(nama.getValue());

		pengaduan.setKeterangan(keterangan.getValue());

		if (disposisiSop != null && disposisiSop.getId() != null) {
			pengaduan.setDisposisiSop(disposisiSop);
		}

		parameterTambahanListener.onSave(pengaduan);

		if (pengaduan.getId() != null) {

			if (pengaduan.getIndex() == null) {
				String noAgenda = generateCode(pengaduan.getJenisPengaduan(), true);
				kode.setValue(noAgenda);
				pengaduan.setKode(noAgenda);
				Long currentIndex = getindex(pengaduan.getJenisPengaduan());
				pengaduan.setIndex(++currentIndex);
			}

			Common.refreshUpdate(session, pengaduan);
		} else {
			if (pengaduan.getKode() == null) {
				String noAgenda = generateCode(pengaduan.getJenisPengaduan(), true);
				kode.setValue(noAgenda);
				pengaduan.setKode(noAgenda);
			}

			Long currentIndex = getindex(pengaduan.getJenisPengaduan());
			pengaduan.setIndex(++currentIndex);
			session.save(pengaduan);
		}

		if (!lampiranLains.isEmpty()) {
			Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
			streamingSession.getTransaction().begin();
			for (LampiranLain lampiranLain : lampiranLains.values()) {
				streamingSession.refresh(lampiranLain);
				lampiranLain.setRef(pengaduan.getId());
				streamingSession.update(lampiranLain);
			}
			streamingSession.getTransaction().commit();
			StreamingHibernateUtil.getInstance().closeSession();
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {

		Tbmuser tbmuser = Common.getCurrentUser();
		Pegawai existing = tbmuser == null ? null : tbmuser.ambilPegawai();
		Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
		Siswa siswa = tbmuser == null ? null : tbmuser.getSiswa();

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Pengaduan.class)

				.add(tbmuser == null || Common.getApakahAdmin() ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("diajukan", tbmuser))
				.add(existing == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("pegawai", existing))
				.add(mahasiswa == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("mahasiswa", mahasiswa))
				.add(siswa == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("siswa", siswa))

				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("setujui"), Restrictions.eq("setujui", false))
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria

				.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("keterangan", searchnama.getValue().trim(), MatchMode.ANYWHERE));

		if (punyaBawahan != null && !punyaBawahan.isEmpty() && punyaBawahanDosen != null
				&& !punyaBawahanDosen.isEmpty()) {
			org.hibernate.criterion.DetachedCriteria subDosen = org.hibernate.criterion.DetachedCriteria
					.forClass(Pegawai.class, "p").createAlias("p.dosen", "d")
					.add(Restrictions.in("d.id", punyaBawahanDosen))
					.setProjection(Projections.property("p.id"));
			criteria.add(

					Restrictions.or(Restrictions.isNotNull("siswa"),

							Restrictions.or(Restrictions.isNotNull("mahasiswa"),

									Restrictions.or(Restrictions.in("pegawai.id", punyaBawahan),
											org.hibernate.criterion.Subqueries.propertyIn("pegawai.id", subDosen)))));
		} else if (punyaBawahan != null && !punyaBawahan.isEmpty()) {
			criteria.add(

					Restrictions.or(Restrictions.isNotNull("siswa"),

							Restrictions.or(Restrictions.isNotNull("mahasiswa"),

									Restrictions.in("pegawai.id", punyaBawahan))));
		} else if (punyaBawahanDosen != null && !punyaBawahanDosen.isEmpty()) {
			org.hibernate.criterion.DetachedCriteria subDosen = org.hibernate.criterion.DetachedCriteria
					.forClass(Pegawai.class, "p").createAlias("p.dosen", "d")
					.add(Restrictions.in("d.id", punyaBawahanDosen))
					.setProjection(Projections.property("p.id"));
			criteria.add(

					Restrictions.or(Restrictions.isNotNull("siswa"),

							Restrictions.or(Restrictions.isNotNull("mahasiswa"),

									org.hibernate.criterion.Subqueries.propertyIn("pegawai.id", subDosen))));
		}

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Pengaduan> pengaduan = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(pengaduan);
		grid.setRowRenderer(new PengaduanRenderer());
		grid.setModelCheckMobile(strset);

	}

	@Override
	public String istilah() throws Exception {
		// TODO Auto-generated method stub
		return "Pengaduan";
	}

	@Override
	public DataSop ambil() throws Exception {
		// TODO Auto-generated method stub
		return pengaduan;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClass() throws Exception {
		// TODO Auto-generated method stub
		return Pengaduan.class;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public File cetakData(GeneralValueObject generalValueObject) throws Exception {
		Pengaduan pengaduan = (Pengaduan) generalValueObject;
		JenisPengaduan j = pengaduan.getJenisPengaduan();
		if (j == null) {
			return null;
		}

		LampiranLain lainMahaadministrasi = LampiranLain.ambil(j.getId(),
				LampiranLain.FILE_JRXML_LAYOUT_JENIS_PENGADUAN);

		if (lainMahaadministrasi == null) {
			return null;
		}

		Map parameters = LaporanPengaduan.generateParameter(j, null, null, pengaduan.getPegawai(),
				pengaduan.getMahasiswa(), pengaduan.getSiswa(), pengaduan);

		File file = Report.generateCompileFileReport(Report.PDF, parameters,
				lainMahaadministrasi.ambilFile().getAbsolutePath(), ais.ui.util.WaktuUtil.getDate());

		return file;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void onKHS(Pengaduan pengaduan) throws Exception {

		try {

			JenisPengaduan j = pengaduan.getJenisPengaduan();
			if (j == null) {
				return;
			}

			LampiranLain lainMahaadministrasi = LampiranLain.ambil(j.getId(),
					LampiranLain.FILE_JRXML_LAYOUT_JENIS_PENGADUAN);

			if (lainMahaadministrasi == null) {
				MyMessageboxConfig.show("File laporan Pengaduan belum diupload", "Peringatan", MyMessageboxConfig.OK,
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

			Map parameters = LaporanPengaduan.generateParameter(j, null, null, pengaduan.getPegawai(),
					pengaduan.getMahasiswa(), pengaduan.getSiswa(), pengaduan);

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
