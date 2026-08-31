package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.report.format1.akademik.LaporanDaftarHadirDosen;
import ais.action.report.format1.akademik.LaporanDaftarHadirDosenHarian;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonOnSearchdefault;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.DosenDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyIframe;

/**
 * Tipe khusus untuk dosen action bisa ngajar. Kelas ini memberi nama dan batas tanggung jawab yang
 * eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyGrid grid}, {@code Paging paging},
 * {@code Textbox searchcode}, {@code Textbox searchnama}, {@code Combobox searchfakultas}, {@code Combobox
 * searchjurusan}, {@code BiodataDosenAction biodataDosenAction}, {@code Tabpanel laporanDosenHarian};
 * inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code onSearchDefault()}); operasi domain lain ({@code onLaporanPerDosen()}, {@code
 * onLaporanDosenHarian()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
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
public class DosenActionBisaNgajar extends GenericAutowireComposer implements CommonOnSearchdefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3786091220301468178L;
	private MyGrid grid;
	private Paging paging;
	private Textbox searchcode;
	private Textbox searchnama;
	private Combobox searchfakultas;
	private Combobox searchjurusan;

	private BiodataDosenAction biodataDosenAction;

	private Tabpanel laporanDosenHarian;
	private Tabpanel laporanPerDosen;

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

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	}

	public void onLaporanPerDosen(Event event) {

		if (laporanPerDosen.getChildren().size() == 0) {
			LaporanDaftarHadirDosen laporanDaftarHadirDosen = new LaporanDaftarHadirDosen();
			laporanDaftarHadirDosen.setHeight("100%");
			laporanDaftarHadirDosen.setWidth("100%");
			laporanDaftarHadirDosen.setParent(laporanPerDosen);
		}
	}

	public void onLaporanDosenHarian(Event event) {

		if (laporanDosenHarian.getChildren().size() == 0) {
			LaporanDaftarHadirDosenHarian laporanDaftarHadirDosenHarian = new LaporanDaftarHadirDosenHarian();
			laporanDaftarHadirDosenHarian.setHeight("100%");
			laporanDaftarHadirDosenHarian.setWidth("100%");
			laporanDaftarHadirDosenHarian.setParent(laporanDosenHarian);
		}
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link DosenActionBisaNgajar}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link DosenActionBisaNgajar} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see DosenActionBisaNgajar
	 */
	class DosenRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final Dosen dosen = (Dosen) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {
						session.setAttribute("selectedDosen", dosen);
						MyIframe include = new MyIframe("/pages/master/informasi_jadwal_ajar_dosen.zul");
						include.setHeight("500px");
						include.setWidth("100%");
						detail.appendChild(include);
					}
				}
			});

			CommonMedia.tampilkanGambarKecil(dosen).setParent(arg0);

			new Label(dosen.getCode() == null ? "" : dosen.getCode()).setParent(arg0);

			new Label(dosen.getNidn() == null ? "" : dosen.getNidn()).setParent(arg0);
			RevisiHelper.createNewRevisi(Dosen.class, dosen, dosen.getNama()).setParent(arg0);
			new Label(dosen.getStatusPegawai() == null ? "" : dosen.getStatusPegawai().getNama()).setParent(arg0);
			new Label(dosen.getGolongan()).setParent(arg0);
			new Label((dosen.getIkatanKerjaDosen() == null ? "" : dosen.getIkatanKerjaDosen().getNama()) + " "
					+ (dosen.getStatusKepegawaian() == null ? "" : dosen.getStatusKepegawaian().getNama()))
							.setParent(arg0);
			new Label(dosen.getEmail()).setParent(arg0);
			new Label(dosen.getTelp()).setParent(arg0);
			// new Label(dosen.getAlamat()).setParent(arg0);

			new Label(dosen.getPangkat()).setParent(arg0);
			new Label(dosen.getJabatan()).setParent(arg0);

			new Label(dosen.getFakultas() == null ? "" : dosen.getFakultas().getNama()).setParent(arg0);

			new Label(dosen.getJurusan() == null ? "" : dosen.getJurusan().getNama()).setParent(arg0);

			Hbox toolbar = new Hbox();

			// cek data available
			final MyCheckboxConfig chek = new MyCheckboxConfig("Mengajar prodi lain");
			chek.setVisible(true);
			chek.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					dosen.setMilikUniversitas(chek.isChecked() ? true : false);
					DosenDao dosenDao = DaoFactory.getInstance().getDosenDao();
					if (dosen.getId() != null) {
						dosenDao.update(dosen);
					} else {
						dosenDao.save(dosen);
					}
				}

			});
			chek.setChecked(dosen.getMilikUniversitas() != null && dosen.getMilikUniversitas() == true);
			// chek.setVisible(Common.getCurrentUser() != null
			// && Common.getCurrentUser() != null
			// && Common.getCurrentUser().hakAkses() != null
			// && Common.getCurrentUser().hakAkses() != null
			// && Common.getCurrentUser() != null
			// && Common.getCurrentUser().hakAkses() != null
			// && Common.getCurrentUser().hakAkses().getRoleId() != null
			// && (Common.getCurrentUser() != null
			// && Common.getCurrentUser().hakAkses() != null
			// && Common.getCurrentUser().hakAkses()
			// .getRoleId()
			// .equalsIgnoreCase(Tbmrole.ADMINISTRATOR)
			// || Common.getCurrentUser().hakAkses()
			// .getRoleId()
			// .equalsIgnoreCase(Tbmrole.DIKJAR)
			// || Common.getCurrentUser().hakAkses()
			// .getRoleId().equalsIgnoreCase("akdfak") || Common
			// .getCurrentUser().hakAkses().getRoleId()
			// .equalsIgnoreCase("admfak")));

			chek.setParent(toolbar);

			toolbar.setParent(arg0);
		}

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Dosen.class);
		if (order)
			criteria.addOrder(Order.asc("nama"));

		criteria.add(CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false))
				.add(CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

		.add(searchnama.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
				: Restrictions.ilike("nama", searchnama.getValue(), MatchMode.ANYWHERE))
				.add(searchcode.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("code", searchcode.getValue(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Dosen> dosen = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(dosen);
		grid.setRowRenderer(new DosenRenderer());
		grid.setModelCheckMobile(strset);

		if (biodataDosenAction != null) {
			biodataDosenAction.detach();
		}

	}

}
