package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
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
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.AmbilDataJamPerkuliahanBanbox;
import ais.action.master.helper.AmbilDataMatakuliahBanbox;
import ais.action.master.helper.AmbilDataRuangBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.common.OnSearchDefaultListener;
import ais.database.dao.DaoFactory;
import ais.database.dao.TemplatePerkuliahanDetailDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.JamPerkuliahan;
import ais.database.model.Jurusan;
import ais.database.model.Kurikulum;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.Matakuliah;
import ais.database.model.Perkuliahan;
import ais.database.model.Ruang;
import ais.database.model.Tbmuser;
import ais.database.model.TemplatePerkuliahan;
import ais.database.model.TemplatePerkuliahanDetail;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTimebox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk template perkuliahan detail. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code AmbilDataMatakuliahBanbox searchmatakuliah}, {@code AmbilDataDosenBanbox
 * searchdosen}, {@code Combobox searchhari}, {@code Textbox searchkelas}, {@code MyCheckboxConfig
 * searchparalel}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code init()},
 * {@code initCriteria()}); pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code onSave()},
 * {@code onSaveCopy()}); operasi domain lain ({@code copy()}, {@code generatePerkulihaanParalel()}, {@code
 * onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class TemplatePerkuliahanDetailAction extends GenericAutowireComposer implements OnSearchDefaultListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3786091220301468178L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private AmbilDataMatakuliahBanbox searchmatakuliah;
	private AmbilDataDosenBanbox searchdosen;
	private Combobox searchhari;
	private Textbox searchkelas;
	private MyCheckboxConfig searchparalel;
	private AmbilDataRuangBanbox searchruang;
	// private Combobox searchTahunAjaran;
	// private Combobox searchsemester;
	private Combobox searchwaktu;
	private Combobox searchprogram;
	private Combobox searchfakultas;
	private Combobox searchjurusan;

	private MyTimebox waktuMulai;
	private MyTimebox waktuSelesai;

	private Combobox fakultas;
	private Combobox jurusan;
	private Combobox matakuliah;
	private AmbilDataDosenBanbox dosen1;
	private AmbilDataDosenBanbox dosen2;
	// private Combobox semester;
	private MyCheckboxConfig merupakan_paralel;
	private Combobox templatePerkuliahanDetail_paralel;
	private Decimalbox kapasitasKelas;

	private MyCheckboxConfig merupakan_tanpa_jadwal_templatePerkuliahanDetail;
	private MyCheckboxConfig merupakan_tanpa_dosen;
	private MyCheckboxConfig merupakan_tanpa_ruangan;

	private Combobox waktu;
	private Textbox kelas;

	private Combobox hari;
	// private Combobox tahunAjaran;
	private Combobox kurikulum;

	private Combobox program;

	private TemplatePerkuliahanDetail templatePerkuliahanDetail;
	private MyToolbarbuttonConfig add;
	// TODO ayu
	private AmbilDataRuangBanbox ruang;
	private boolean edit;
	private boolean delete;

	private SimpleDateFormat dateFormat = new SimpleDateFormat("HH.mm");

	// private Tbmuser users;

	private TemplatePerkuliahan templatePerkuliahan;
	private Integer semester;
	private AmbilDataJamPerkuliahanBanbox jamPerkuliahan;

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

		templatePerkuliahan = (TemplatePerkuliahan) session.getAttribute("templatePerkuliahan");
		if (templatePerkuliahan == null) {
			return;
		} else {
			session.removeAttribute("templatePerkuliahan");
		}

		semester = (Integer) session.getAttribute("semester");
		if (semester == null) {
			return;
		} else {
			session.removeAttribute("semester");
		}

		searchmatakuliah.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);

			}
		});
		searchdosen.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);

			}
		});

		searchruang.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);

			}
		});

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.initFakultasDanJurusan(fakultas, jurusan, searchfakultas, searchjurusan);

		hari = new Combobox();
		for (String h : Common.haris) {
			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel(h);
			comboitem.setValue(h);
			searchhari.appendChild(comboitem);
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(h);
			comboitem.setValue(h);
			hari.appendChild(comboitem);

		}

		MyComboitemConfig comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel("Semua"); }
		if (comboitem != null) { comboitem.setValue(null); }
		searchhari.appendChild(comboitem);
		if (searchhari != null) { searchhari.setSelectedItem(comboitem); }

		waktu = new Combobox();
		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel("PAGI"); }
		if (comboitem != null) { comboitem.setValue("PAGI"); }
		waktu.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel("SIANG"); }
		if (comboitem != null) { comboitem.setValue("SIANG"); }
		waktu.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel("SORE"); }
		if (comboitem != null) { comboitem.setValue("SORE"); }
		waktu.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel("MALAM"); }
		if (comboitem != null) { comboitem.setValue("MALAM"); }
		waktu.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel("PAGI"); }
		if (comboitem != null) { comboitem.setValue("PAGI"); }
		searchwaktu.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel("SIANG"); }
		if (comboitem != null) { comboitem.setValue("SIANG"); }
		searchwaktu.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel("SORE"); }
		if (comboitem != null) { comboitem.setValue("SORE"); }
		searchwaktu.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel("MALAM"); }
		if (comboitem != null) { comboitem.setValue("MALAM"); }
		searchwaktu.appendChild(comboitem);

		program = Common.initPrograms(null);
		Common.initPrograms(searchprogram);

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.ambilDosen() != null && tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")) {
			Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
			searchdosen.setValue(dosen.getNama());
			searchdosen.setAttribute("myValue", dosen);
			searchdosen.setDisabled(true);
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
	        FilterLanjutHelper.setup(comp);
}

	/**
	 * Renderer lokal untuk layar/komponen {@link TemplatePerkuliahanDetailAction}. Kelas ini menerjemahkan satu
	 * item data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link TemplatePerkuliahanDetailAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see TemplatePerkuliahanDetailAction
	 */
	class TemplatePerkuliahanDetailRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final TemplatePerkuliahanDetail templatePerkuliahanDetail = (TemplatePerkuliahanDetail) arg1;

			new Label((templatePerkuliahanDetail.getHari() == null ? "" : templatePerkuliahanDetail.getHari()))
					.setParent(arg0);
			new Label(templatePerkuliahanDetail.getWaktu()).setParent(arg0);
			new Label(((templatePerkuliahanDetail.getWaktuMulai() == null ? ""
					: templatePerkuliahanDetail.getWaktuMulai()) == null
							? ""
							: (templatePerkuliahanDetail.getWaktuMulai() == null ? ""
									: templatePerkuliahanDetail.getWaktuMulai()))
					+ "-"
					+ ((templatePerkuliahanDetail.getWaktuSelesai() == null ? ""
							: templatePerkuliahanDetail.getWaktuSelesai()) == null ? ""
									: (templatePerkuliahanDetail.getWaktuSelesai() == null ? ""
											: templatePerkuliahanDetail.getWaktuSelesai()))).setParent(arg0);

			RevisiHelper.createNewRevisi(TemplatePerkuliahanDetail.class, templatePerkuliahanDetail,

					templatePerkuliahanDetail.getMatakuliah().getNama()
							+ (templatePerkuliahanDetail.getMerupakan_paralel() != null
									&& templatePerkuliahanDetail.getMerupakan_paralel() ? " (Paralel) " : ""))
					.setParent(arg0);

			new Label(templatePerkuliahanDetail.getDosen1() == null ? ""
					: templatePerkuliahanDetail.getDosen1().getNama()).setParent(arg0);
			new Label(templatePerkuliahanDetail.getDosen2() == null ? ""
					: templatePerkuliahanDetail.getDosen2().getNama()).setParent(arg0);
			new Label(templatePerkuliahanDetail.getRuang() == null ? ""
					: templatePerkuliahanDetail.getRuang().getKodeRuangan()).setParent(arg0);

			new Label((templatePerkuliahanDetail.getKapasitasKelas() == null ? ""
					: Common.numberFormat.get().format(templatePerkuliahanDetail.getKapasitasKelas()))).setParent(arg0);

			new Label(templatePerkuliahanDetail.getSemester()
					+ (templatePerkuliahanDetail.getKelas() == null || templatePerkuliahanDetail.getKelas().equals("")
							? ""
							: " " + templatePerkuliahanDetail.getKelas())).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(templatePerkuliahanDetail);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			// copy
			button = new MyToolbarbuttonConfig("", "/img/svg/edit-copy.svg");
			button.setTooltiptext("Copy Jadwal");
			button.setVisible(edit);

			// button.setVisible(true);
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					copy(templatePerkuliahanDetail);
					addWindow.setVisible(true);
					addWindow.onModal();
				}
			});
			button.setParent(toolbar);
			// end copy

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
										Common.refreshDelete((templatePerkuliahanDetail));
										onSearchDefault(event);
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

	@SuppressWarnings({ "unchecked" })
	public void copy(final TemplatePerkuliahanDetail templatePerkuliahanDetail) throws Exception {

		if (templatePerkuliahanDetail != null && templatePerkuliahanDetail.getId() != null) {
			Tbmuser tbmuser = Common.getCurrentUser();
			Fakultas userFakultas = tbmuser.ambilFakultas();
			Jurusan jurusan = tbmuser.ambilJurusan();
			if (userFakultas != null
					&& !userFakultas.getId().equals(templatePerkuliahanDetail.getJurusan().getFakultas().getId())) {
				MyMessageboxConfig.show(
						"Anda tidak boleh meng-copy jadwal template perkuliahan dari Fakultas "
								+ templatePerkuliahanDetail.getJurusan().getFakultas().getNama(),
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return;
			}
			if (jurusan != null && !jurusan.getId().equals(templatePerkuliahanDetail.getJurusan().getId())) {
				MyMessageboxConfig.show(
						"Anda tidak boleh meng-copy jadwal template perkuliahan dari Prodi "
								+ templatePerkuliahanDetail.getJurusan().getNama(),
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return;
			}
		}

		this.templatePerkuliahanDetail = templatePerkuliahanDetail;
		Common.clear(addWindow);
		addWindow.setTitle("Copy template jadwal Perkuliahan");
		addWindow.setWidth("590px");
		addWindow.setHeight("90%");

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

		Tbmuser tbmuser = Common.getCurrentUser();
		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		Common.selectComboItem(program, templatePerkuliahanDetail.getProgram() == null ? tbmuser.ambilProgram()
				: templatePerkuliahanDetail.getProgram());
		row.appendChild(program);
		program.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		Common.selectComboItem(fakultas,
				templatePerkuliahanDetail.getJurusan() == null ? templatePerkuliahan.getFakultas()
						: templatePerkuliahanDetail.getJurusan().getFakultas());
		row.appendChild(fakultas);
		fakultas.setWidth("90%");

		Common.insertCombo(jurusan, "nama", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
				Restrictions.eq("fakultas",
						templatePerkuliahanDetail.getJurusan() == null ? templatePerkuliahan.getFakultas()
								: templatePerkuliahanDetail.getJurusan().getFakultas()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		Common.pilihJurusan(jurusan,
				templatePerkuliahanDetail.getJurusan() == null ? templatePerkuliahan.getJurusan()
						: templatePerkuliahanDetail.getJurusan());
		row.appendChild(jurusan);
		jurusan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kurikulum"));
		row.appendChild(kurikulum = new Combobox());
		kurikulum.setWidth("90%");

		/**
		 * Event listener lokal milik {@link TemplatePerkuliahanDetailAction}. Kelas ini menangani event untuk komponen
		 * induk dan meneruskan pekerjaan domain ke method/service yang sudah tersedia.
		 *
		 * <p><b>Scope:</b> setiap instance terikat pada instance {@link TemplatePerkuliahanDetailAction} dan dapat
		 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
		 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code onEvent}(). Aturan bisnis bersama
		 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
		 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
		 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
		 * renderer/listener ini.</p>
		 *
		 * @see TemplatePerkuliahanDetailAction
		 */
		class KurikulumEventListener implements EventListener {
			@Override
			public void onEvent(Event event) throws Exception {
				Common.clear(kurikulum);
				kurikulum.setSelectedItem(null);
				if (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null) {
					return;
				}

				Jurusan myJurusan = (Jurusan) (jurusan.getSelectedItem() == null
						|| jurusan.getSelectedItem().getValue() == null ? null : jurusan.getSelectedItem().getValue());

				List<Kurikulum> kurikulums = HibernateUtil.currentSession().createCriteria(Kurikulum.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.addOrder(Order.desc("tahun")).add(Restrictions.eq("jurusan", myJurusan)).list();

				for (Kurikulum kurikulum : kurikulums) {
					org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
					comboitem.setLabel(kurikulum.getId() + "-" + kurikulum.getNama());
					comboitem.setValue(kurikulum);
					comboitem.setDescription(kurikulum.getNamaAsli() + " " + kurikulum.getTahun() + " "
							+ kurikulum.getTahunAkademik() + " " + kurikulum.getJenisSemester());
					TemplatePerkuliahanDetailAction.this.kurikulum.appendChild(comboitem);
				}
			}

		}

		KurikulumEventListener kurikulumEventListener = new KurikulumEventListener();

		jurusan.addEventListener("onChange", kurikulumEventListener);

		kurikulumEventListener.onEvent(null);

		Common.selectComboItem(kurikulum, templatePerkuliahanDetail.getKurikulum());

		//

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(new ais.ui.util.MyLabelConfig(semester + ""));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelas"));
		row.appendChild(kelas = new Textbox(
				templatePerkuliahanDetail.getKelas() == null ? "A" : templatePerkuliahanDetail.getKelas()));
		kelas.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasaConfig("Matakuliah")));

		row.appendChild(matakuliah = new Combobox());
		matakuliah.setWidth("90%");

		/**
		 * Event listener lokal milik {@link TemplatePerkuliahanDetailAction}. Kelas ini menangani event untuk komponen
		 * induk dan meneruskan pekerjaan domain ke method/service yang sudah tersedia.
		 *
		 * <p><b>Scope:</b> setiap instance terikat pada instance {@link TemplatePerkuliahanDetailAction} dan dapat
		 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
		 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code onEvent}(). Aturan bisnis bersama
		 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
		 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
		 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
		 * renderer/listener ini.</p>
		 *
		 * @see TemplatePerkuliahanDetailAction
		 */
		class MatakuliahEventListener implements EventListener {
			@Override
			public void onEvent(Event event) throws Exception {
				Common.clear(matakuliah);
				matakuliah.setSelectedItem(null);
				if (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null) {
					return;
				}
				if (kurikulum.getSelectedItem() == null) {
					return;
				}
				if (semester == null) {
					return;
				}
				List<KurikulumPunyaMatakuliah> kurikulumPunyaMatakuliahs = HibernateUtil.currentSession()
						.createCriteria(KurikulumPunyaMatakuliah.class)
						.add(kurikulum.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("kurikulum", kurikulum.getSelectedItem().getValue()))
						// .add(semester.toString()
						// .equalsIgnoreCase(Perkuliahan.GENAP)
						// ? Restrictions.in(
						// "semester", Common.genap) : Restrictions.in(
						// "semester", Common.ganjil))
						.add(semester == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("semester", semester))

						// .createCriteria("matakuliah")
						// if(order)criteria.addOrder(Order.asc("nama"))
						// .add(semester.toString()
						// .equalsIgnoreCase(Perkuliahan.GENAP)
						// ? Restrictions.in(
						// "semester", Common.genap) : Restrictions.in(
						// "semester", Common.ganjil))

						.list();

				for (KurikulumPunyaMatakuliah matakuliah : kurikulumPunyaMatakuliahs) {
					org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
					comboitem.setLabel(matakuliah.getMatakuliah().getNama());
					comboitem.setValue(matakuliah.getMatakuliah());
					String desc = "Kode: " + matakuliah.getMatakuliah().getKode() + ", Status: "
							+ matakuliah.getMatakuliah().getStatus() + ", SKS: " + matakuliah.getMatakuliah().getSks()
							+ (matakuliah.getTahap() == null ? "" : ", Tahap : " + matakuliah.getTahap());
					comboitem.setDescription(desc);
					TemplatePerkuliahanDetailAction.this.matakuliah.appendChild(comboitem);
				}
			}

		}

		MatakuliahEventListener matakuliahEventListener = new MatakuliahEventListener();

		kurikulum.addEventListener("onChange", matakuliahEventListener);

		matakuliahEventListener.onEvent(null);

		Common.selectComboItem(matakuliah, templatePerkuliahanDetail.getMatakuliah());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasa("label_dosen_1")));
		Hbox hbox = new Hbox();
		hbox.appendChild(dosen1 = new AmbilDataDosenBanbox());
		hbox.appendChild(merupakan_tanpa_dosen = new MyCheckboxConfig(Common.getBahasa("label_tanpa_dosen")));

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				dosen1.setVisible(!merupakan_tanpa_dosen.isChecked());
				if (merupakan_tanpa_dosen.isChecked()) {
					dosen1.removeAttribute("myValue");
					dosen1.removeAttribute("dosen");
				}
				dosen2.setVisible(!merupakan_tanpa_dosen.isChecked());
				if (merupakan_tanpa_dosen.isChecked()) {
					dosen2.removeAttribute("myValue");
					dosen2.removeAttribute("dosen");
				}

			}
		};

		merupakan_tanpa_dosen.addEventListener(Events.ON_CHECK, eventListener);
		merupakan_tanpa_dosen.setChecked(templatePerkuliahanDetail.getMerupakan_tanpa_dosen() != null
				&& templatePerkuliahanDetail.getMerupakan_tanpa_dosen());

		row.appendChild(hbox);
		dosen1.setValue(
				templatePerkuliahanDetail.getDosen1() == null ? "" : (templatePerkuliahanDetail.getDosen1().getNama()));
		if (tbmuser != null && tbmuser.ambilDosen() != null && tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")) {
			Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
			dosen1.setValue(dosen.getNama());
			dosen1.setAttribute("myValue", dosen);
			dosen1.setDisabled(true);
		}

		dosen1.setAttribute("myValue", templatePerkuliahanDetail.getDosen1());
		dosen1.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasa("label_dosen_2")));
		row.appendChild(dosen2 = new AmbilDataDosenBanbox());
		dosen2.setValue(
				templatePerkuliahanDetail.getDosen2() == null ? "" : (templatePerkuliahanDetail.getDosen2().getNama()));

		dosen2.setAttribute("myValue", templatePerkuliahanDetail.getDosen2());
		dosen2.setWidth("90%");
		eventListener.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Ruang"));

		hbox = new Hbox();
		hbox.appendChild(ruang = new AmbilDataRuangBanbox());
		hbox.appendChild(merupakan_tanpa_ruangan = new MyCheckboxConfig("Tanpa ruang"));
		eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				ruang.setVisible(!merupakan_tanpa_ruangan.isChecked());
				if (merupakan_tanpa_ruangan.isChecked()) {
					ruang.removeAttribute("ruang");
				}
				// kelas.setVisible(!merupakan_tanpa_ruangan.isChecked());

			}
		};

		merupakan_tanpa_ruangan.addEventListener(Events.ON_CHECK, eventListener);
		merupakan_tanpa_ruangan.setChecked(templatePerkuliahanDetail.getMerupakan_tanpa_ruangan() != null
				&& templatePerkuliahanDetail.getMerupakan_tanpa_ruangan());

		row.appendChild(hbox);
		ruang.setValue(templatePerkuliahanDetail.getRuang() == null ? ""
				: (templatePerkuliahanDetail.getRuang().getKodeRuangan()));
		ruang.setId("" + templatePerkuliahanDetail.getRuang() == null ? "ruang_-1" : "ruang_" + ruang.getId());
		ruang.setAttribute("ruang", templatePerkuliahanDetail.getRuang());
		ruang.setWidth("90%");
		eventListener.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kapasitas"));
		row.appendChild(kapasitasKelas = new Decimalbox(templatePerkuliahanDetail.getKapasitasKelas() == null ? null
				: new BigDecimal(templatePerkuliahanDetail.getKapasitasKelas())));
		kapasitasKelas.setWidth("90%");

		ruang.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (kapasitasKelas.getValue() == null) {
					Ruang myRuang = (Ruang) ruang.getAttribute("ruang");
					if (myRuang != null) {
						kapasitasKelas.setValue(myRuang.getKapasitasRuangan() == null ? null
								: new BigDecimal(myRuang.getKapasitasRuangan()));
					}
				}
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Waktu"));

		hbox = new Hbox();
		hbox.appendChild(waktu);
		hbox.appendChild(merupakan_tanpa_jadwal_templatePerkuliahanDetail = new MyCheckboxConfig("Tanpa jadwal"));
		eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				waktu.setVisible(!merupakan_tanpa_jadwal_templatePerkuliahanDetail.isChecked());
				waktuMulai.setVisible(!merupakan_tanpa_jadwal_templatePerkuliahanDetail.isChecked());
				waktuSelesai.setVisible(!merupakan_tanpa_jadwal_templatePerkuliahanDetail.isChecked());
				hari.setVisible(!merupakan_tanpa_jadwal_templatePerkuliahanDetail.isChecked());
				merupakan_paralel.setVisible(!merupakan_tanpa_jadwal_templatePerkuliahanDetail.isChecked());

				jamPerkuliahan.setVisible(!merupakan_tanpa_jadwal_templatePerkuliahanDetail.isChecked());

				if (merupakan_tanpa_jadwal_templatePerkuliahanDetail.isChecked()) {
					merupakan_paralel.setChecked(false);
					templatePerkuliahanDetail_paralel.setSelectedIndex(-1);
				}

				if (merupakan_tanpa_jadwal_templatePerkuliahanDetail.isChecked()) {
					waktu.setValue(null);
					waktuMulai.setValue(null);
					waktuSelesai.setValue(null);
					hari.setSelectedItem(null);
					jamPerkuliahan.setAttribute("jamPerkuliahan", null);
					jamPerkuliahan.setAttribute("myValue", null);
				}

			}
		};
		merupakan_tanpa_jadwal_templatePerkuliahanDetail.addEventListener(Events.ON_CHECK, eventListener);
		merupakan_tanpa_jadwal_templatePerkuliahanDetail
				.setChecked(templatePerkuliahanDetail.getMerupakan_tanpa_jadwal_perkuliahan() != null
						&& templatePerkuliahanDetail.getMerupakan_tanpa_jadwal_perkuliahan());
		row.appendChild(hbox);

		Common.selectComboItem(waktu, templatePerkuliahanDetail.getWaktu());
		waktu.setWidth("90%");

		Date dateMulai = null;
		Date dateSelesai = null;
		try {
			System.out.println("TemplatePerkuliahanDetail getWaktuMulai = "
					+ (templatePerkuliahanDetail.getWaktuMulai() == null ? ""
							: templatePerkuliahanDetail.getWaktuMulai()));
			if ((templatePerkuliahanDetail.getWaktuMulai() == null ? ""
					: templatePerkuliahanDetail.getWaktuMulai()) != null
					&& !(templatePerkuliahanDetail.getWaktuMulai() == null ? ""
							: templatePerkuliahanDetail.getWaktuMulai()).equals(""))
				dateMulai = dateFormat.parse((templatePerkuliahanDetail.getWaktuMulai() == null ? ""
						: templatePerkuliahanDetail.getWaktuMulai()));
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		try {
			if ((templatePerkuliahanDetail.getWaktuSelesai() == null ? ""
					: templatePerkuliahanDetail.getWaktuSelesai()) != null
					&& !(templatePerkuliahanDetail.getWaktuSelesai() == null ? ""
							: templatePerkuliahanDetail.getWaktuSelesai()).equals(""))
				dateSelesai = dateFormat.parse((templatePerkuliahanDetail.getWaktuSelesai() == null ? ""
						: templatePerkuliahanDetail.getWaktuSelesai()));
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jam Perkuliahan"));
		row.appendChild(jamPerkuliahan = new AmbilDataJamPerkuliahanBanbox(
				(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
						: jurusan.getSelectedItem().getValue())));
		jamPerkuliahan.setValue(templatePerkuliahanDetail.getJamPerkuliahan() == null ? ""
				: templatePerkuliahanDetail.getJamPerkuliahan().getNama());
		jamPerkuliahan.setAttribute("jamPerkuliahan", templatePerkuliahanDetail.getJamPerkuliahan());
		jamPerkuliahan.setAttribute("myValue", templatePerkuliahanDetail.getJamPerkuliahan());
		jamPerkuliahan.setWidth("90%");

		jurusan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				jamPerkuliahan.setJurusan(
						(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null
								? null
								: jurusan.getSelectedItem().getValue()));
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Waktu Mulai"));
		row.appendChild(waktuMulai = new MyTimebox(dateMulai == null ? ais.ui.util.WaktuUtil.getDate() : dateMulai));
		waktuMulai.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Waktu Selesai"));
		row.appendChild(
				waktuSelesai = new MyTimebox(dateSelesai == null ? ais.ui.util.WaktuUtil.getDate() : dateSelesai));
		waktuSelesai.setWidth("90%");

		EventListener jamPerkuliahanEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				JamPerkuliahan myJamPerkuliahan = (JamPerkuliahan) jamPerkuliahan.getAttribute("jamPerkuliahan");
				if (myJamPerkuliahan != null) {
					waktuMulai.setValue(myJamPerkuliahan.getMulai());
					waktuSelesai.setValue(myJamPerkuliahan.getSampai());
				}

				waktuMulai.setDisabled(myJamPerkuliahan != null);
				waktuSelesai.setDisabled(myJamPerkuliahan != null);
			}
		};

		jamPerkuliahan.setEventListener(jamPerkuliahanEventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Hari"));
		Common.selectComboItem(hari, null);
		row.appendChild(hari);
		hari.setWidth("90%");

		final MyFormRow myrow = new MyFormRow();
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label());
		row.appendChild(merupakan_paralel = new MyCheckboxConfig("Merupakan jadwal Perkuliahan paralel"));
		merupakan_paralel.setChecked(templatePerkuliahanDetail.getMerupakan_paralel() == null ? false
				: templatePerkuliahanDetail.getMerupakan_paralel());

		myrow.setVisible(false);
		myrow.setParent(rows);
		myrow.appendChild(new Label(ais.common.Common.getBahasaConfig("Paralel dari TemplatePerkuliahanDetail (wajib diisi)")));
		myrow.appendChild(templatePerkuliahanDetail_paralel = new Combobox());
		templatePerkuliahanDetail_paralel.setWidth("90%");
		if (merupakan_paralel.isChecked()) {
			myrow.setVisible(true);
			generatePerkulihaanParalel(true);
			Common.selectComboItem(templatePerkuliahanDetail_paralel,
					templatePerkuliahanDetail.getPerkuliahan_paralel());
		}

		merupakan_paralel.addEventListener(Events.ON_CHECK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (merupakan_paralel.isChecked()) {
					myrow.setVisible(true);
					generatePerkulihaanParalel(true);
					Common.selectComboItem(templatePerkuliahanDetail_paralel,
							templatePerkuliahanDetail.getPerkuliahan_paralel());
				} else {
					myrow.setVisible(false);
					Common.clear(templatePerkuliahanDetail_paralel);
					templatePerkuliahanDetail_paralel.setSelectedItem(null);
				}

			}
		});

		eventListener.onEvent(null);

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
				if (onSaveCopy(event)) {
					onSearchDefault(null);
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	@SuppressWarnings("unchecked")
	private void generatePerkulihaanParalel(Boolean isCopy) throws Exception {
		Common.clear(templatePerkuliahanDetail_paralel);

		if (program.getSelectedItem() == null || program.getSelectedItem().getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Program",
					"Kolom Program belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Program.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return;
		}
		if (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show(Common.getBahasaConfig("Jurusan") + " harus diisi", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		}
		if (semester == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Semester",
					"Kolom Semester belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Semester.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return;
		}
		if (matakuliah.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Matakuliah",
					"Kolom Matakuliah belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Matakuliah.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return;
		}

		List<TemplatePerkuliahanDetail> templatePerkuliahanDetail = HibernateUtil.currentSession()
				.createCriteria(TemplatePerkuliahanDetail.class).addOrder(Order.desc("id"))
				.add(this.templatePerkuliahanDetail.getId() == null || isCopy ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.templatePerkuliahanDetail.getId()))
				.add(Restrictions.or(Restrictions.eq("merupakan_paralel", false),
						Restrictions.isNull("merupakan_paralel")))

				.add(Restrictions.eq("semester", semester))
				.add(Restrictions.eq("templatePerkuliahan", templatePerkuliahan))

				.add(CommonSearchFilterHelper.eqSelectedWithId("jurusan", jurusan, false))

				.add(Restrictions.eq("program", program.getSelectedItem().getValue()))

				.add(Restrictions.eq("matakuliah", matakuliah.getSelectedItem().getValue()))

				.createCriteria("jurusan", Criteria.LEFT_JOIN)
				.add(CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false)).list();

		for (TemplatePerkuliahanDetail o : templatePerkuliahanDetail) {
			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel(
					(o.getDosen1() == null ? "" : o.getDosen1().getNama()) + " - " + o.getMatakuliah().getNama());
			comboitem.setValue(o);

			String deskripsi = "Dosen: " + (o.getDosen1() == null ? "" : o.getDosen1().getNama()) + ",Smt: "
					+ (o.getSemester() + (o.getKelas() == null || o.getKelas().equals("") ? "" : " " + o.getKelas()))
					+ ", Ruang: " + (o.getRuang() == null ? "" : o.getRuang().getKodeRuangan()) + ", Hari: "
					+ o.getHari() + ", Waktu: " + o.getWaktuMulai() + "-" + o.getWaktuSelesai();

			comboitem.setDescription(deskripsi);
			templatePerkuliahanDetail_paralel.appendChild(comboitem);
		}
	}

	public void onAdd(Event event) throws Exception {
		init(new TemplatePerkuliahanDetail());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings({ "unchecked" })
	private void init(final TemplatePerkuliahanDetail templatePerkuliahanDetail) throws Exception {

		if (templatePerkuliahanDetail != null && templatePerkuliahanDetail.getId() != null) {
			Tbmuser tbmuser = Common.getCurrentUser();
			Fakultas userFakultas = tbmuser.ambilFakultas();
			Jurusan jurusan = tbmuser.ambilJurusan();
			if (userFakultas != null
					&& !userFakultas.getId().equals(templatePerkuliahanDetail.getJurusan().getFakultas().getId())) {
				MyMessageboxConfig.show(
						"Anda tidak boleh mengubah jadwal template perkuliahan dari Fakultas "
								+ templatePerkuliahanDetail.getJurusan().getFakultas().getNama(),
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return;
			}
			if (jurusan != null && !jurusan.getId().equals(templatePerkuliahanDetail.getJurusan().getId())) {
				MyMessageboxConfig.show(
						"Anda tidak boleh mengubah jadwal template perkuliahan dari Prodi "
								+ templatePerkuliahanDetail.getJurusan().getNama(),
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return;
			}
		}

		this.templatePerkuliahanDetail = templatePerkuliahanDetail;
		Common.clear(addWindow);
		addWindow.setTitle(templatePerkuliahanDetail.getId() == null ? "Tambah Jadwal TemplatePerkuliahanDetail"
				: "Ubah Jadwal TemplatePerkuliahanDetail");
		addWindow.setWidth("590px");
		addWindow.setHeight("90%");

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

		Tbmuser tbmuser = Common.getCurrentUser();

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		Common.selectComboItem(program, templatePerkuliahanDetail.getProgram() == null ? tbmuser.ambilProgram()
				: templatePerkuliahanDetail.getProgram());
		row.appendChild(program);
		program.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		Common.selectComboItem(fakultas, templatePerkuliahanDetail.getJurusan() == null ? tbmuser.ambilFakultas()
				: templatePerkuliahanDetail.getJurusan().getFakultas());
		row.appendChild(fakultas);
		fakultas.setWidth("90%");

		Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
				Restrictions.eq("fakultas", templatePerkuliahanDetail.getJurusan() == null ? tbmuser.ambilFakultas()
						: templatePerkuliahanDetail.getJurusan().getFakultas()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		Common.pilihJurusan(jurusan, templatePerkuliahanDetail.getJurusan() == null ? tbmuser.ambilJurusan()
				: templatePerkuliahanDetail.getJurusan());
		row.appendChild(jurusan);
		jurusan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kurikulum"));
		row.appendChild(kurikulum = new Combobox());
		kurikulum.setWidth("90%");

		/**
		 * Listener lokal yang memuat pilihan kurikulum setelah program studi pada form detail berubah.
		 * Instance menangkap combobox dan state form milik {@link TemplatePerkuliahanDetailAction}; gunakan hanya pada
		 * event thread layar ini dan pertahankan query/validasi kurikulum pada alur induk.
		 *
		 * @see TemplatePerkuliahanDetailAction
		 */
		class KurikulumEventListener implements EventListener {
			@Override
			public void onEvent(Event event) throws Exception {
				Common.clear(kurikulum);
				kurikulum.setSelectedItem(null);
				if (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null) {
					return;
				}

				Jurusan myJurusan = (Jurusan) (jurusan.getSelectedItem() == null
						|| jurusan.getSelectedItem().getValue() == null ? null : jurusan.getSelectedItem().getValue());

				List<Kurikulum> kurikulums = HibernateUtil.currentSession().createCriteria(Kurikulum.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.addOrder(Order.desc("tahun")).add(Restrictions.eq("jurusan", myJurusan)).list();

				for (Kurikulum kurikulum : kurikulums) {
					org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
					comboitem.setLabel(kurikulum.getId() + "-" + kurikulum.getNama());
					comboitem.setValue(kurikulum);
					comboitem.setDescription(kurikulum.getNamaAsli() + " " + kurikulum.getTahun() + " "
							+ kurikulum.getTahunAkademik() + " " + kurikulum.getJenisSemester());
					TemplatePerkuliahanDetailAction.this.kurikulum.appendChild(comboitem);
				}
			}

		}

		KurikulumEventListener kurikulumEventListener = new KurikulumEventListener();

		jurusan.addEventListener("onChange", kurikulumEventListener);

		kurikulumEventListener.onEvent(null);

		Common.selectComboItem(kurikulum, templatePerkuliahanDetail.getKurikulum());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(new ais.ui.util.MyLabelConfig(semester + ""));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelas"));
		row.appendChild(kelas = new Textbox(
				templatePerkuliahanDetail.getKelas() == null ? "A" : templatePerkuliahanDetail.getKelas()));
		kelas.setWidth("90%");

		// matakuliah = new Combobox();

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasaConfig("Matakuliah")));

		row.appendChild(matakuliah = new Combobox());
		matakuliah.setWidth("90%");

		/**
		 * Listener lokal yang menyegarkan pilihan mata kuliah berdasarkan kurikulum dan semester pada form detail.
		 * Instance menangkap komponen form milik {@link TemplatePerkuliahanDetailAction}; jangan dibagikan lintas
		 * desktop/session atau dijadikan sumber query mata kuliah yang terpisah dari alur induk.
		 *
		 * @see TemplatePerkuliahanDetailAction
		 */
		class MatakuliahEventListener implements EventListener {
			@Override
			public void onEvent(Event event) throws Exception {
				Common.clear(matakuliah);
				matakuliah.setSelectedItem(null);
				if (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null) {
					return;
				}
				if (kurikulum.getSelectedItem() == null) {
					return;
				}
				if (semester == null) {
					return;
				}
				List<KurikulumPunyaMatakuliah> kurikulumPunyaMatakuliahs = HibernateUtil.currentSession()
						.createCriteria(KurikulumPunyaMatakuliah.class)
						.add(kurikulum.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("kurikulum", kurikulum.getSelectedItem().getValue()))

						.add(semester == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("semester", semester))

						.list();

				for (KurikulumPunyaMatakuliah matakuliah : kurikulumPunyaMatakuliahs) {
					org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
					comboitem.setLabel(matakuliah.getMatakuliah().getNama());
					comboitem.setValue(matakuliah.getMatakuliah());
					String desc = "Kode: " + matakuliah.getMatakuliah().getKode() + ", Status: "
							+ matakuliah.getMatakuliah().getStatus() + ", SKS: " + matakuliah.getMatakuliah().getSks()
							+ (matakuliah.getTahap() == null ? "" : ", Tahap : " + matakuliah.getTahap());
					comboitem.setDescription(desc);
					TemplatePerkuliahanDetailAction.this.matakuliah.appendChild(comboitem);
				}
			}

		}

		MatakuliahEventListener matakuliahEventListener = new MatakuliahEventListener();

		kurikulum.addEventListener("onChange", matakuliahEventListener);

		matakuliahEventListener.onEvent(null);

		Common.selectComboItem(matakuliah, templatePerkuliahanDetail.getMatakuliah());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasa("label_dosen_1")));
		Hbox hbox = new Hbox();
		hbox.appendChild(dosen1 = new AmbilDataDosenBanbox());
		hbox.appendChild(merupakan_tanpa_dosen = new MyCheckboxConfig(Common.getBahasa("label_tanpa_dosen")));

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				dosen1.setVisible(!merupakan_tanpa_dosen.isChecked());
				if (merupakan_tanpa_dosen.isChecked()) {
					dosen1.removeAttribute("myValue");
					dosen1.removeAttribute("dosen");
				}
				dosen2.setVisible(!merupakan_tanpa_dosen.isChecked());
				if (merupakan_tanpa_dosen.isChecked()) {
					dosen2.removeAttribute("myValue");
					dosen2.removeAttribute("dosen");
				}

			}
		};

		merupakan_tanpa_dosen.addEventListener(Events.ON_CHECK, eventListener);
		merupakan_tanpa_dosen.setChecked(templatePerkuliahanDetail.getMerupakan_tanpa_dosen() != null
				&& templatePerkuliahanDetail.getMerupakan_tanpa_dosen());

		row.appendChild(hbox);
		dosen1.setValue(
				templatePerkuliahanDetail.getDosen1() == null ? "" : (templatePerkuliahanDetail.getDosen1().getNama()));
		if (tbmuser != null && tbmuser.ambilDosen() != null && tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")) {
			Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
			dosen1.setValue(dosen.getNama());
			dosen1.setAttribute("myValue", dosen);
			dosen1.setDisabled(true);
		}

		dosen1.setAttribute("myValue", templatePerkuliahanDetail.getDosen1());
		dosen1.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasa("label_dosen_2")));
		row.appendChild(dosen2 = new AmbilDataDosenBanbox());
		dosen2.setValue(
				templatePerkuliahanDetail.getDosen2() == null ? "" : (templatePerkuliahanDetail.getDosen2().getNama()));

		dosen2.setAttribute("myValue", templatePerkuliahanDetail.getDosen2());
		dosen2.setWidth("90%");
		eventListener.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Ruang"));

		hbox = new Hbox();
		hbox.appendChild(ruang = new AmbilDataRuangBanbox());
		hbox.appendChild(merupakan_tanpa_ruangan = new MyCheckboxConfig("Tanpa ruang"));
		eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				ruang.setVisible(!merupakan_tanpa_ruangan.isChecked());
				if (merupakan_tanpa_ruangan.isChecked()) {
					ruang.removeAttribute("ruang");
				}
				// kelas.setVisible(!merupakan_tanpa_ruangan.isChecked());

			}
		};

		merupakan_tanpa_ruangan.addEventListener(Events.ON_CHECK, eventListener);
		merupakan_tanpa_ruangan.setChecked(templatePerkuliahanDetail.getMerupakan_tanpa_ruangan() != null
				&& templatePerkuliahanDetail.getMerupakan_tanpa_ruangan());

		row.appendChild(hbox);
		ruang.setValue(templatePerkuliahanDetail.getRuang() == null ? ""
				: (templatePerkuliahanDetail.getRuang().getKodeRuangan()));
		ruang.setId("" + templatePerkuliahanDetail.getRuang() == null ? "ruang_-1" : "ruang_" + ruang.getId());
		ruang.setAttribute("ruang", templatePerkuliahanDetail.getRuang());
		ruang.setWidth("90%");
		eventListener.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kapasitas"));
		row.appendChild(kapasitasKelas = new Decimalbox(templatePerkuliahanDetail.getKapasitasKelas() == null ? null
				: new BigDecimal(templatePerkuliahanDetail.getKapasitasKelas())));
		kapasitasKelas.setWidth("90%");

		ruang.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (kapasitasKelas.getValue() == null) {
					Ruang myRuang = (Ruang) ruang.getAttribute("ruang");
					if (myRuang != null) {
						kapasitasKelas.setValue(myRuang.getKapasitasRuangan() == null ? null
								: new BigDecimal(myRuang.getKapasitasRuangan()));
					}
				}
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Waktu"));

		hbox = new Hbox();
		hbox.appendChild(waktu);
		hbox.appendChild(merupakan_tanpa_jadwal_templatePerkuliahanDetail = new MyCheckboxConfig("Tanpa jadwal"));
		eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				waktu.setVisible(!merupakan_tanpa_jadwal_templatePerkuliahanDetail.isChecked());
				waktuMulai.setVisible(!merupakan_tanpa_jadwal_templatePerkuliahanDetail.isChecked());
				waktuSelesai.setVisible(!merupakan_tanpa_jadwal_templatePerkuliahanDetail.isChecked());
				hari.setVisible(!merupakan_tanpa_jadwal_templatePerkuliahanDetail.isChecked());
				merupakan_paralel.setVisible(!merupakan_tanpa_jadwal_templatePerkuliahanDetail.isChecked());

				jamPerkuliahan.setVisible(!merupakan_tanpa_jadwal_templatePerkuliahanDetail.isChecked());

				if (merupakan_tanpa_jadwal_templatePerkuliahanDetail.isChecked()) {
					merupakan_paralel.setChecked(false);
					templatePerkuliahanDetail_paralel.setSelectedIndex(-1);
				}

				if (merupakan_tanpa_jadwal_templatePerkuliahanDetail.isChecked()) {
					waktu.setValue(null);
					waktuMulai.setValue(null);
					waktuSelesai.setValue(null);
					hari.setSelectedItem(null);
					jamPerkuliahan.setAttribute("jamPerkuliahan", null);
					jamPerkuliahan.setAttribute("myValue", null);
				}

			}
		};
		merupakan_tanpa_jadwal_templatePerkuliahanDetail.addEventListener(Events.ON_CHECK, eventListener);
		merupakan_tanpa_jadwal_templatePerkuliahanDetail
				.setChecked(templatePerkuliahanDetail.getMerupakan_tanpa_jadwal_perkuliahan() != null
						&& templatePerkuliahanDetail.getMerupakan_tanpa_jadwal_perkuliahan());
		row.appendChild(hbox);

		Common.selectComboItem(waktu, templatePerkuliahanDetail.getWaktu());
		waktu.setWidth("90%");

		Date dateMulai = null;
		Date dateSelesai = null;
		try {
			System.out.println("TemplatePerkuliahanDetail getWaktuMulai = "
					+ (templatePerkuliahanDetail.getWaktuMulai() == null ? ""
							: templatePerkuliahanDetail.getWaktuMulai()));
			if ((templatePerkuliahanDetail.getWaktuMulai() == null ? ""
					: templatePerkuliahanDetail.getWaktuMulai()) != null
					&& !(templatePerkuliahanDetail.getWaktuMulai() == null ? ""
							: templatePerkuliahanDetail.getWaktuMulai()).equals(""))
				dateMulai = dateFormat.parse((templatePerkuliahanDetail.getWaktuMulai() == null ? ""
						: templatePerkuliahanDetail.getWaktuMulai()));
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		try {
			if ((templatePerkuliahanDetail.getWaktuSelesai() == null ? ""
					: templatePerkuliahanDetail.getWaktuSelesai()) != null
					&& !(templatePerkuliahanDetail.getWaktuSelesai() == null ? ""
							: templatePerkuliahanDetail.getWaktuSelesai()).equals(""))
				dateSelesai = dateFormat.parse((templatePerkuliahanDetail.getWaktuSelesai() == null ? ""
						: templatePerkuliahanDetail.getWaktuSelesai()));
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jam Perkuliahan"));
		row.appendChild(jamPerkuliahan = new AmbilDataJamPerkuliahanBanbox(
				(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
						: jurusan.getSelectedItem().getValue())));
		jamPerkuliahan.setValue(templatePerkuliahanDetail.getJamPerkuliahan() == null ? ""
				: templatePerkuliahanDetail.getJamPerkuliahan().getNama());
		jamPerkuliahan.setAttribute("jamPerkuliahan", templatePerkuliahanDetail.getJamPerkuliahan());
		jamPerkuliahan.setAttribute("myValue", templatePerkuliahanDetail.getJamPerkuliahan());
		jamPerkuliahan.setWidth("90%");

		jurusan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				jamPerkuliahan.setJurusan(
						(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null
								? null
								: jurusan.getSelectedItem().getValue()));
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Waktu Mulai"));
		row.appendChild(waktuMulai = new MyTimebox(dateMulai == null ? ais.ui.util.WaktuUtil.getDate() : dateMulai));
		waktuMulai.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Waktu Selesai"));
		row.appendChild(
				waktuSelesai = new MyTimebox(dateSelesai == null ? ais.ui.util.WaktuUtil.getDate() : dateSelesai));
		waktuSelesai.setWidth("90%");

		EventListener jamPerkuliahanEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				JamPerkuliahan myJamPerkuliahan = (JamPerkuliahan) jamPerkuliahan.getAttribute("jamPerkuliahan");
				if (myJamPerkuliahan != null) {
					waktuMulai.setValue(myJamPerkuliahan.getMulai());
					waktuSelesai.setValue(myJamPerkuliahan.getSampai());
				}

				waktuMulai.setDisabled(myJamPerkuliahan != null);
				waktuSelesai.setDisabled(myJamPerkuliahan != null);
			}
		};

		jamPerkuliahan.setEventListener(jamPerkuliahanEventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Hari"));
		Common.selectComboItem(hari,
				(templatePerkuliahanDetail.getHari() == null ? "" : templatePerkuliahanDetail.getHari()));
		row.appendChild(hari);
		hari.setWidth("90%");

		final MyFormRow myrow = new MyFormRow();
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label());
		row.appendChild(merupakan_paralel = new MyCheckboxConfig("Merupakan jadwal Perkuliahan paralel"));
		merupakan_paralel.setChecked(templatePerkuliahanDetail.getMerupakan_paralel() == null ? false
				: templatePerkuliahanDetail.getMerupakan_paralel());

		myrow.setVisible(false);
		myrow.setParent(rows);
		myrow.appendChild(new Label(ais.common.Common.getBahasaConfig("Paralel dari TemplatePerkuliahanDetail (wajib diisi)")));
		myrow.appendChild(templatePerkuliahanDetail_paralel = new Combobox());
		templatePerkuliahanDetail_paralel.setWidth("90%");
		if (merupakan_paralel.isChecked()) {
			myrow.setVisible(true);
			generatePerkulihaanParalel(false);
			Common.selectComboItem(templatePerkuliahanDetail_paralel,
					templatePerkuliahanDetail.getPerkuliahan_paralel());
		}

		merupakan_paralel.addEventListener(Events.ON_CHECK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (merupakan_paralel.isChecked()) {
					myrow.setVisible(true);
					generatePerkulihaanParalel(false);
					Common.selectComboItem(templatePerkuliahanDetail_paralel,
							templatePerkuliahanDetail.getPerkuliahan_paralel());
				} else {
					myrow.setVisible(false);
					Common.clear(templatePerkuliahanDetail_paralel);
					templatePerkuliahanDetail_paralel.setSelectedItem(null);
				}

			}
		});

		eventListener.onEvent(null);

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

		if (program.getSelectedItem() == null || program.getSelectedItem().getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Program",
					"Kolom Program belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Program.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show(Common.getBahasaConfig("Jurusan") + " harus diisi", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (semester == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Semester",
					"Kolom Semester belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Semester.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (matakuliah.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Matakuliah",
					"Kolom Matakuliah belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Matakuliah.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (dosen1.getAttribute("myValue") == null && !merupakan_tanpa_dosen.isChecked()) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Dosen 1",
					"Kolom Dosen 1 belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Dosen 1.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		// if (waktu.getSelectedItem() == null
		// && !merupakan_tanpa_jadwal_templatePerkuliahanDetail
		// .isChecked()) {
		// MyMessageboxConfig.show("Waktu harus diisi", "Peringatan",
		// MyMessageboxConfig.OK,
		// MyMessageboxConfig.INFORMATION);
		// return false;
		// }
		if (waktuMulai.getValue() == null && !merupakan_tanpa_jadwal_templatePerkuliahanDetail.isChecked()) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Waktu mulai",
					"Kolom Waktu mulai belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Waktu mulai.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (waktuSelesai.getValue() == null && !merupakan_tanpa_jadwal_templatePerkuliahanDetail.isChecked()) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Waktu selesai",
					"Kolom Waktu selesai belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Waktu selesai.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (hari.getSelectedItem() == null && !merupakan_tanpa_jadwal_templatePerkuliahanDetail.isChecked()) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Hari",
					"Kolom Hari belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Hari.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		if (ruang.getAttribute("ruang") == null && !merupakan_tanpa_ruangan.isChecked()) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Ruang",
					"Kolom Ruang belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Ruang.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (kapasitasKelas.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Kapasitas kelas",
					"Kolom Kapasitas kelas belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Kapasitas kelas.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		Ruang rng = (Ruang) ruang.getAttribute("ruang");
		if (rng != null && rng.getKapasitasRuangan() != null && kapasitasKelas.getValue() != null
				&& kapasitasKelas.getValue().intValue() > rng.getKapasitasRuangan()) {
			MyMessageboxConfig.show("Kapasitas ruang " + rng.getKapasitasRuangan()
					+ " sedangkan kapasitas kelas yang anda masukkan adalah " + kapasitasKelas.getValue().intValue(),
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (kelas.getValue().trim().equals("") && !merupakan_tanpa_ruangan.isChecked()) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Kelas",
					"Kolom Kelas belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Kelas.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (merupakan_paralel.isChecked() && templatePerkuliahanDetail_paralel.getSelectedItem() == null) {
			MyMessageboxConfig.show(
					"Jika anda ingin membuat jadwal Perkuliahan paralel, anda harus memilih jadwal template perkuliahan utama terlebih dahulu. Jika jadwal perkulihanan utama belum ada, anda harus membuat jadwal perluliahan non paralel baru, setelah itu, anda bisa menghubungkan ke jadwal template perkuliahan paralel yang akan anda buat",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Double waktuMulaiD = !waktuMulai.isVisible() || waktuMulai.getValue() == null ? null
				: Double.parseDouble(dateFormat.format(waktuMulai.getValue())) + 0.01;
		Double waktuSelesaiD = !waktuSelesai.isVisible() || waktuSelesai.getValue() == null ? null
				: Double.parseDouble(dateFormat.format(waktuSelesai.getValue())) - 0.01;

		if (waktuMulaiD != null && waktuSelesaiD != null && waktuMulaiD >= waktuSelesaiD) {
			MyMessageboxConfig.show(
					"Waktu mulai \"" + Common.timeFormat.get().format(waktuMulai.getValue())
							+ "\" tidak boleh lebih besar nilainya atau sama dengan waktu selesai \""
							+ Common.timeFormat.get().format(waktuSelesai.getValue()) + "\" ",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		int s = (Integer) semester;

		if (Common.checkKelasJadwalTemplatePerkuliahanDetail(templatePerkuliahan, templatePerkuliahanDetail.getId(),
				(Jurusan) jurusan.getSelectedItem().getValue(), (String) program.getSelectedItem().getValue(),
				!hari.isVisible() || hari.getSelectedItem() == null ? null
						: hari.getSelectedItem().getValue().toString(),
				waktuMulaiD, waktuSelesaiD, s % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL,
				kelas.getValue().trim(), (Integer) semester, (Matakuliah) matakuliah.getSelectedItem().getValue(),
				null) != null) {
			return false;
		}

		if (Common.checkJadwalTemplateRuangPerkuliahanDetail(templatePerkuliahan, templatePerkuliahanDetail.getId(),
				!ruang.isVisible() ? null : (Ruang) ruang.getAttribute("ruang"),
				!hari.isVisible() || hari.getSelectedItem() == null ? null
						: hari.getSelectedItem().getValue().toString(),
				waktuMulaiD, waktuSelesaiD, s % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL,
				(Jurusan) jurusan.getSelectedItem().getValue(), (Matakuliah) matakuliah.getSelectedItem().getValue(),
				kelas.getValue().trim(), null) != null) {
			return false;
		}
		if (Common.checkJadwalDosen(templatePerkuliahan, templatePerkuliahanDetail.getId(),
				!hari.isVisible() || hari.getSelectedItem() == null ? null
						: hari.getSelectedItem().getValue().toString(),
				waktuMulaiD, waktuSelesaiD, !dosen1.isVisible() ? null : (Dosen) dosen1.getAttribute("myValue"),
				s % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL, (Jurusan) jurusan.getSelectedItem().getValue(),
				(Matakuliah) matakuliah.getSelectedItem().getValue(), kelas.getValue().trim(), null) != null) {
			return false;
		}

		TemplatePerkuliahanDetailDao templatePerkuliahanDetailDao = DaoFactory.getInstance()
				.getTemplatePerkuliahanDetailDao();
		TemplatePerkuliahanDetail templatePerkuliahanDetailParalel = (TemplatePerkuliahanDetail) (templatePerkuliahanDetail_paralel
				.getSelectedItem() == null ? null : templatePerkuliahanDetail_paralel.getSelectedItem().getValue());

		if (templatePerkuliahanDetail.getId() != null) {
			if (templatePerkuliahanDetailParalel != null && templatePerkuliahanDetailParalel.getId() != null) {
				if (templatePerkuliahanDetail.getId().equals(templatePerkuliahanDetailParalel.getId())) {
					MyMessageboxConfig.show(
							"Anda tidak bisa membuat templatePerkuliahanDetail paralel ke dirinya sendiri",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}
			templatePerkuliahanDetail = templatePerkuliahanDetailDao.load(templatePerkuliahanDetail.getId());
		}

		if (!merupakan_paralel.isChecked()) {
			if (!merupakan_tanpa_ruangan.isChecked()
					&& Common.checkMatakuliahKesamaanBukanParalel(templatePerkuliahanDetail, templatePerkuliahan,
							(Jurusan) jurusan.getSelectedItem().getValue(), kelas.getValue().trim(),
							(Matakuliah) matakuliah.getSelectedItem().getValue(), s,
							(String) program.getSelectedItem().getValue(), null) != null) {
				return false;
			}
		}

		templatePerkuliahanDetail.setJamPerkuliahan((JamPerkuliahan) jamPerkuliahan.getAttribute("jamPerkuliahan"));

		templatePerkuliahanDetail.setKapasitasKelas(kapasitasKelas.getValue().intValue());
		templatePerkuliahanDetail.setMerupakan_tanpa_dosen(merupakan_tanpa_dosen.isChecked());
		templatePerkuliahanDetail
				.setMerupakan_tanpa_jadwal_perkuliahan(merupakan_tanpa_jadwal_templatePerkuliahanDetail.isChecked());
		templatePerkuliahanDetail.setMerupakan_tanpa_ruangan(merupakan_tanpa_ruangan.isChecked());
		templatePerkuliahanDetail.setPerkuliahan_paralel(templatePerkuliahanDetailParalel);
		templatePerkuliahanDetail.setMerupakan_paralel(merupakan_paralel.isChecked());

		templatePerkuliahanDetail.setWaktu((String) (!waktu.isVisible() ? null
				: waktu.getSelectedItem() == null ? null : waktu.getSelectedItem().getValue()));
		templatePerkuliahanDetail.setWaktuMulai(!waktuMulai.isVisible() || waktuMulai.getValue() == null ? null
				: dateFormat.format(waktuMulai.getValue()));
		templatePerkuliahanDetail.setWaktuSelesai(!waktuSelesai.isVisible() || waktuSelesai.getValue() == null ? null
				: dateFormat.format(waktuSelesai.getValue()));
		templatePerkuliahanDetail.setHari(!hari.isVisible() || hari.getSelectedItem() == null ? null
				: hari.getSelectedItem().getValue().toString());

		templatePerkuliahanDetail.setDosen1((Dosen) (dosen1.isVisible() ? dosen1.getAttribute("myValue") : null));
		templatePerkuliahanDetail.setDosen2((Dosen) (dosen2.isVisible() ? dosen2.getAttribute("myValue") : null));
		templatePerkuliahanDetail.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
						: jurusan.getSelectedItem().getValue()));
		templatePerkuliahanDetail.setMatakuliah(
				(Matakuliah) (matakuliah.getSelectedItem() == null ? null : matakuliah.getSelectedItem().getValue()));

		templatePerkuliahanDetail.setKelas(kelas.isVisible() ? kelas.getValue().trim() : "");
		templatePerkuliahanDetail.setRuang((Ruang) (ruang.isVisible() ? ruang.getAttribute("ruang") : null));

		templatePerkuliahanDetail.setSemester((Integer) semester);

		templatePerkuliahanDetail.setProgram(
				program.getSelectedItem() == null || program.getSelectedItem().getValue() == null ? "Reguler"
						: program.getSelectedItem().getValue().toString());
		templatePerkuliahanDetail.setKurikulum(
				(Kurikulum) (kurikulum.getSelectedItem() == null ? null : kurikulum.getSelectedItem().getValue()));

		templatePerkuliahanDetail.setTemplatePerkuliahan(templatePerkuliahan);
		if (templatePerkuliahanDetail.getId() != null) {
			templatePerkuliahanDetailDao.update(templatePerkuliahanDetail);
		} else {
			templatePerkuliahanDetailDao.save(templatePerkuliahanDetail);
		}
		return true;
	}

	public boolean onSaveCopy(Event event) throws Exception {

		if (program.getSelectedItem() == null || program.getSelectedItem().getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Program",
					"Kolom Program belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Program.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show(Common.getBahasaConfig("Jurusan") + " harus diisi", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (semester == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Semester",
					"Kolom Semester belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Semester.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (matakuliah.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Matakuliah",
					"Kolom Matakuliah belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Matakuliah.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (dosen1.getAttribute("myValue") == null && !merupakan_tanpa_dosen.isChecked()) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Dosen 1",
					"Kolom Dosen 1 belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Dosen 1.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		// if (waktu.getSelectedItem() == null
		// && !merupakan_tanpa_jadwal_templatePerkuliahanDetail
		// .isChecked()) {
		// MyMessageboxConfig.show("Waktu harus diisi", "Peringatan",
		// MyMessageboxConfig.OK,
		// MyMessageboxConfig.INFORMATION);
		// return false;
		// }
		if (waktuMulai.getValue() == null && !merupakan_tanpa_jadwal_templatePerkuliahanDetail.isChecked()) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Waktu mulai",
					"Kolom Waktu mulai belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Waktu mulai.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (waktuSelesai.getValue() == null && !merupakan_tanpa_jadwal_templatePerkuliahanDetail.isChecked()) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Waktu selesai",
					"Kolom Waktu selesai belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Waktu selesai.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (hari.getSelectedItem() == null && !merupakan_tanpa_jadwal_templatePerkuliahanDetail.isChecked()) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Hari",
					"Kolom Hari belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Hari.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		if (ruang.getAttribute("ruang") == null && !merupakan_tanpa_ruangan.isChecked()) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Ruang",
					"Kolom Ruang belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Ruang.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (kapasitasKelas.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Kapasitas kelas",
					"Kolom Kapasitas kelas belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Kapasitas kelas.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		Ruang rng = (Ruang) ruang.getAttribute("ruang");
		if (rng != null && rng.getKapasitasRuangan() != null && kapasitasKelas.getValue() != null
				&& kapasitasKelas.getValue().intValue() > rng.getKapasitasRuangan()) {
			MyMessageboxConfig.show("Kapasitas ruang " + rng.getKapasitasRuangan()
					+ " sedangkan kapasitas kelas yang anda masukkan adalah " + kapasitasKelas.getValue().intValue(),
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (kelas.getValue().trim().equals("") && !merupakan_tanpa_ruangan.isChecked()) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Kelas",
					"Kolom Kelas belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Kelas.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (merupakan_paralel.isChecked() && templatePerkuliahanDetail_paralel.getSelectedItem() == null) {
			MyMessageboxConfig.show(
					"Jika anda ingin membuat jadwal template perkuliahan paralel, anda harus memilih jadwal template perkuliahan utama terlebih dahulu. Jika jadwal perkulihanan utama belum ada, anda harus membuat jadwal perluliahan non paralel baru, setelah itu, anda bisa menghubungkan ke jadwal template perkuliahan paralel yang akan anda buat",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		TemplatePerkuliahanDetail templatePerkuliahanDetail = new TemplatePerkuliahanDetail();

		Double waktuMulaiD = !waktuMulai.isVisible() || waktuMulai.getValue() == null ? null
				: Double.parseDouble(dateFormat.format(waktuMulai.getValue())) + 0.01;
		Double waktuSelesaiD = !waktuSelesai.isVisible() || waktuSelesai.getValue() == null ? null
				: Double.parseDouble(dateFormat.format(waktuSelesai.getValue())) - 0.01;

		if (waktuMulaiD != null && waktuSelesaiD != null && waktuMulaiD >= waktuSelesaiD) {
			MyMessageboxConfig.show(
					"Waktu mulai \"" + Common.timeFormat.get().format(waktuMulai.getValue())
							+ "\" tidak boleh lebih besar nilainya atau sama dengan waktu selesai \""
							+ Common.timeFormat.get().format(waktuSelesai.getValue()) + "\" ",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		int s = (Integer) semester;

		if (Common.checkKelasJadwalTemplatePerkuliahanDetail(templatePerkuliahan, templatePerkuliahanDetail.getId(),
				(Jurusan) jurusan.getSelectedItem().getValue(), (String) program.getSelectedItem().getValue(),
				!hari.isVisible() || hari.getSelectedItem() == null ? null
						: hari.getSelectedItem().getValue().toString(),
				waktuMulaiD, waktuSelesaiD, s % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL,
				kelas.getValue().trim(), (Integer) semester, (Matakuliah) matakuliah.getSelectedItem().getValue(),
				null) != null) {
			return false;
		}

		if (Common.checkJadwalTemplateRuangPerkuliahanDetail(templatePerkuliahan, templatePerkuliahanDetail.getId(),
				!ruang.isVisible() ? null : (Ruang) ruang.getAttribute("ruang"),
				!hari.isVisible() || hari.getSelectedItem() == null ? null
						: hari.getSelectedItem().getValue().toString(),
				waktuMulaiD, waktuSelesaiD, s % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL,
				(Jurusan) jurusan.getSelectedItem().getValue(), (Matakuliah) matakuliah.getSelectedItem().getValue(),
				kelas.getValue().trim(), null) != null) {
			return false;
		}
		if (Common.checkJadwalDosen(templatePerkuliahan, templatePerkuliahanDetail.getId(),
				!hari.isVisible() || hari.getSelectedItem() == null ? null
						: hari.getSelectedItem().getValue().toString(),
				waktuMulaiD, waktuSelesaiD, !dosen1.isVisible() ? null : (Dosen) dosen1.getAttribute("myValue"),
				s % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL, (Jurusan) jurusan.getSelectedItem().getValue(),
				(Matakuliah) matakuliah.getSelectedItem().getValue(), kelas.getValue().trim(), null) != null) {
			return false;
		}
		TemplatePerkuliahanDetailDao templatePerkuliahanDetailDao = DaoFactory.getInstance()
				.getTemplatePerkuliahanDetailDao();

		if (!merupakan_paralel.isChecked()) {
			if (!merupakan_tanpa_ruangan.isChecked()
					&& Common.checkMatakuliahKesamaanBukanParalel(templatePerkuliahanDetail, templatePerkuliahan,
							(Jurusan) jurusan.getSelectedItem().getValue(), kelas.getValue().trim(),
							(Matakuliah) matakuliah.getSelectedItem().getValue(), s,
							(String) program.getSelectedItem().getValue(), null) != null) {
				return false;
			}
		}

		templatePerkuliahanDetail.setJamPerkuliahan((JamPerkuliahan) jamPerkuliahan.getAttribute("jamPerkuliahan"));
		templatePerkuliahanDetail.setKapasitasKelas(kapasitasKelas.getValue().intValue());
		templatePerkuliahanDetail.setMerupakan_tanpa_dosen(merupakan_tanpa_dosen.isChecked());
		templatePerkuliahanDetail
				.setMerupakan_tanpa_jadwal_perkuliahan(merupakan_tanpa_jadwal_templatePerkuliahanDetail.isChecked());
		templatePerkuliahanDetail.setMerupakan_tanpa_ruangan(merupakan_tanpa_ruangan.isChecked());
		templatePerkuliahanDetail.setPerkuliahan_paralel(
				(TemplatePerkuliahanDetail) (templatePerkuliahanDetail_paralel.getSelectedItem() == null ? null
						: templatePerkuliahanDetail_paralel.getSelectedItem().getValue()));
		templatePerkuliahanDetail.setMerupakan_paralel(merupakan_paralel.isChecked());

		templatePerkuliahanDetail.setWaktu((String) (!waktu.isVisible() ? null
				: waktu.getSelectedItem() == null ? null : waktu.getSelectedItem().getValue()));
		templatePerkuliahanDetail.setWaktuMulai(!waktuMulai.isVisible() || waktuMulai.getValue() == null ? null
				: dateFormat.format(waktuMulai.getValue()));
		templatePerkuliahanDetail.setWaktuSelesai(!waktuSelesai.isVisible() || waktuSelesai.getValue() == null ? null
				: dateFormat.format(waktuSelesai.getValue()));
		templatePerkuliahanDetail.setHari(!hari.isVisible() || hari.getSelectedItem() == null ? null
				: hari.getSelectedItem().getValue().toString());

		templatePerkuliahanDetail.setDosen1((Dosen) (dosen1.isVisible() ? dosen1.getAttribute("myValue") : null));
		templatePerkuliahanDetail.setDosen2((Dosen) (dosen2.isVisible() ? dosen2.getAttribute("myValue") : null));
		templatePerkuliahanDetail.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
						: jurusan.getSelectedItem().getValue()));
		templatePerkuliahanDetail.setMatakuliah(
				(Matakuliah) (matakuliah.getSelectedItem() == null ? null : matakuliah.getSelectedItem().getValue()));

		templatePerkuliahanDetail.setKelas(kelas.isVisible() ? kelas.getValue().trim() : "");
		templatePerkuliahanDetail.setRuang((Ruang) (ruang.isVisible() ? ruang.getAttribute("ruang") : null));

		templatePerkuliahanDetail.setSemester((Integer) semester);

		templatePerkuliahanDetail.setProgram(
				program.getSelectedItem() == null || program.getSelectedItem().getValue() == null ? "Reguler"
						: program.getSelectedItem().getValue().toString());
		templatePerkuliahanDetail.setKurikulum(
				(Kurikulum) (kurikulum.getSelectedItem() == null ? null : kurikulum.getSelectedItem().getValue()));

		templatePerkuliahanDetail.setTemplatePerkuliahan(templatePerkuliahan);
		if (templatePerkuliahanDetail.getId() != null) {
			templatePerkuliahanDetailDao.update(templatePerkuliahanDetail);
		} else {
			templatePerkuliahanDetailDao.save(templatePerkuliahanDetail);
		}

		return true;

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(TemplatePerkuliahanDetail.class);
		if (order)
			criteria.addOrder(Order.desc("id"));

		criteria.add(Restrictions.ilike("kelas", searchkelas.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchparalel.isChecked() ? Restrictions.or(Restrictions.sqlRestriction(
						"this_.id in (select perkuliahan_paralel from perkuliahan where perkuliahan_paralel is not null)"),
						Restrictions.eq("merupakan_paralel", true)) : Restrictions.sqlRestriction("1=1"))

				.add((searchruang == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchruang.getAttribute("ruang") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("ruang", searchruang.getAttribute("ruang"))))

				.add(Restrictions.eq("semester", semester))
				.add(Restrictions.eq("templatePerkuliahan", templatePerkuliahan))

				.add((searchdosen == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchdosen.getAttribute("myValue") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.eq("dosen1", searchdosen.getAttribute("myValue")),
								Restrictions.eq("dosen2", searchdosen.getAttribute("myValue")))))

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

				.add(searchprogram.getSelectedItem() == null || searchprogram.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("program", searchprogram.getSelectedItem().getValue()))

				.add(searchwaktu.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("waktu", searchwaktu.getSelectedItem().getValue()))
				.add((searchmatakuliah == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchmatakuliah.getAttribute("matakuliah") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("matakuliah", searchmatakuliah.getAttribute("matakuliah"))))
				.add(searchhari.getSelectedItem() == null || searchhari.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("hari", searchhari.getSelectedItem().getValue()))

				.createCriteria("jurusan", Criteria.LEFT_JOIN)
				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false));

		return criteria;

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<TemplatePerkuliahanDetail> templatePerkuliahanDetail = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(templatePerkuliahanDetail);
		grid.setRowRenderer(new TemplatePerkuliahanDetailRenderer());
		grid.setModelCheckMobile(strset);

	}
}
