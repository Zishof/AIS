package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.text.SimpleDateFormat;
import java.util.Calendar;
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
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timebox;

import ais.action.master.helper.DetailpertemuanHelper;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Gedung;
import ais.database.model.Perkuliahan;
import ais.database.model.PesanRuangan;
import ais.database.model.Ruang;
import ais.database.model.Tbmuser;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyIframe;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk cari ruang kosong. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Textbox searchkodeRuangan}, {@code Textbox
 * searchkapasitasruangan}, {@code Combobox searchgedung}, {@code Combobox searchhari}; inisialisasi/lifecycle
 * ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code initCriteria()}); pembacaan/pencarian ({@code
 * onSearchDefault()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
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
public class CariRuangKosongAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	protected static final long serialVersionUID = 3786091228301468178L;
	protected MyWindow addWindow;
	protected Paging paging;
	protected MyGrid grid;

	private Textbox searchnama;
	private Textbox searchkodeRuangan;
	private Textbox searchkapasitasruangan;
	private Combobox searchgedung;

	protected Combobox searchhari;
	protected Combobox searchTahunAjaran;
	protected Combobox searchsemester;
	protected Combobox searchfakultas;
	protected Timebox searchWaktuMulai;
	protected Timebox searchWaktuSelesai;

	protected Integer semesterPendek = null;

	protected SimpleDateFormat dateFormat = new SimpleDateFormat("HH.mm");

	protected Tbmuser tbmuser = Common.getCurrentUser();
	private Double mulai;
	private Double selesai;

	private boolean edit = false;

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

		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		if (comboitem != null) { comboitem.setLabel(Perkuliahan.GENAP); }
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GENAP); }
		searchsemester.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(Perkuliahan.GANJIL); }
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GANJIL); }
		searchsemester.appendChild(comboitem);

		Common.selectComboItem(searchsemester, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

		if (searchWaktuMulai != null) { searchWaktuMulai.setFormat(dateFormat.toPattern()); }
		if (searchWaktuSelesai != null) { searchWaktuSelesai.setFormat(dateFormat.toPattern()); }

		Calendar mulai = ais.ui.util.WaktuUtil.getCalendar();
		mulai.set(Calendar.SECOND, 0);
		mulai.set(Calendar.MINUTE, 0);
		mulai.set(Calendar.HOUR_OF_DAY, 7);

		Calendar selesai = ais.ui.util.WaktuUtil.getCalendar();
		selesai.set(Calendar.SECOND, 0);
		selesai.set(Calendar.MINUTE, 10);
		selesai.set(Calendar.HOUR_OF_DAY, 9);

		if (searchWaktuMulai != null) { searchWaktuMulai.setValue(mulai.getTime()); }
		if (searchWaktuSelesai != null) { searchWaktuSelesai.setValue(selesai.getTime()); }

		Common.insertCombo(searchfakultas, new String[] { "nama", "kode" }, Fakultas.class,
				Restrictions.eq("aktif", true));

		Common.generateTahunAjaranDanSemua(searchTahunAjaran);
		Common.selectComboItem(searchTahunAjaran, Common.getCurrentTahunAkademik());

		for (String h : Common.haris) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(h);
			comboitem.setValue(h);
			searchhari.appendChild(comboitem);
		}

		try {
			searchhari.setSelectedIndex(ais.ui.util.WaktuUtil.getCalendar().get(Calendar.DAY_OF_WEEK) - 1);
		} catch (Exception e) {
			searchhari.setSelectedIndex(0);
			Common.tampilErrorJikaAdmin(e);
		}

		Common.insertCombo(searchgedung, "nama", Gedung.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);

		// Apabila user berwenang hanya di fakultas tertentu, maka user hanya
		// boleh mengakses data fakultas atau jurusan tertentu

		if (tbmuser.ambilFakultas() != null) {
			Common.selectComboItem(searchfakultas, tbmuser.ambilFakultas());
			searchfakultas.setDisabled(true);
		} else {
			searchfakultas.setDisabled(false);
		}

		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	        FilterLanjutHelper.setup(comp);
}

	class PerkuliahanRenderer extends ais.ui.util.MyRowRenderer {

		protected DetailpertemuanHelper detailpertemuanHelper = new DetailpertemuanHelper();

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Ruang ruang = (Ruang) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					// if (detail.getChildren().size() == 0) {
					// detail.setHeight("500px");
					Common.clear(detail);
					if (detail.isOpen()) {
						session.setAttribute("selectedRuang", ruang);
						session.setAttribute("selectedRuang1", ruang);

						ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
						groupbox.setStyle("min-height: 200px;");
						groupbox.setParent(detail);

						Tabbox tabbox = new Tabbox();
						tabbox.setParent(groupbox);
						tabbox.setHeight("100%");
						tabbox.setWidth("100%");

						Tabs tabs = new Tabs();
						tabs.setParent(tabbox);

						final MyTabConfig tabSoal = new MyTabConfig("Jadwal Penggunaan Ruangan");
						tabSoal.setParent(tabs);

						MyTabConfig tabJawaban = new MyTabConfig("Daftar Pemesanan Ruangan");
						tabJawaban.setParent(tabs);

						Tabpanels tabpanels = new Tabpanels();
						tabpanels.setParent(tabbox);

						Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
						tabpanelUtama.setParent(tabpanels);

						MyIframe include = new MyIframe(
								"/pages/master/kalender/perkuliahan/index.zul?ruang=" + ruang.getId());
						include.setHeight("700px");
						include.setWidth("100%");
						tabpanelUtama.appendChild(include);

						tabpanelUtama = new ais.ui.util.MyTabpanel();
						tabpanelUtama.setParent(tabpanels);

						include = new MyIframe("/pages/master/pesan_ruangan.zul");
						include.setHeight("700px");
						include.setWidth("100%");
						tabpanelUtama.appendChild(include);
					}
				}
			});

			RevisiHelper.createNewRevisi(Ruang.class, ruang, ruang.getNama()).setParent(arg0);

			new Label(ruang.getKodeRuangan()).setParent(arg0);
			new Label(ruang.getGedung() == null ? "" : ruang.getGedung().getNama()).setParent(arg0);
			new Label((ruang == null ? Ruang.getDefaultKapasitas() : ruang.getKapasitasRuangan()) == null ? ""
					: ruang.getKapasitasRuangan().toString()).setParent(arg0);
			new Label(ruang.getMerupakanRuangKelas().equals(1) ? "Ya" : "Tidak").setParent(arg0);
			new Label(ruang.getFakultas() == null ? "" : ruang.getFakultas().getNama()).setParent(arg0);

			Perkuliahan perkuliahan = null;

			if (!ruang.getAktif()) {
				arg0.setStyle("background-color: rgba(152,251,152,0.4);color:red;font-weight: bolder;font-size: 12px;");
			} else {

				Session session = HibernateUtil.currentSession();

				perkuliahan = (Perkuliahan) session.createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

						.add(semesterPendek == null ? Restrictions.isNull("statusSemesterPendek")
								: Restrictions.eq("statusSemesterPendek", semesterPendek))

						.add(Restrictions.eq("ruang", ruang))
						.add(searchhari.getSelectedItem() == null || searchhari.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1!=1")
								: Restrictions.eq("hari", searchhari.getSelectedItem().getValue()))

						.add(Restrictions.eq("tahunAjaran", searchTahunAjaran.getSelectedItem().getValue()))

						.add(Restrictions.sqlRestriction("this_.semester "
								+ ((searchsemester.getSelectedItem().getValue().equals(Perkuliahan.GENAP) ? " % 2 = 0 "
										: " % 2 = 1 "))
								+ ""))
						.add(Restrictions.sqlRestriction("(to_number(waktu_mulai,'999999.99') between " + mulai
								+ " and " + selesai + "   or  to_number(waktu_selesai,'999999.99') between " + mulai
								+ " and " + selesai + ")"))
						.setMaxResults(1).uniqueResult();

				if (perkuliahan != null) {
					arg0.setStyle("background-color: rgba(205,92,92,0.4);color:yellow;font-weight: bolder;font-size: 12px;");
				}
			}

			new Label(!ruang.getAktif() ? "Tidak Bisa Digunakan" : perkuliahan == null ? "Tersedia" : "Tidak Tersedia")
					.setParent(arg0);
			if (perkuliahan != null) {
				new Label(perkuliahan.info() + ". " + ruang.getKeterangan()).setParent(arg0);
			} else {
				new Label(ruang.getKeterangan()).setParent(arg0);
			}

			Hbox toolbar = new Hbox();
			toolbar.setVisible(tbmuser.getMahasiswa() == null);
			toolbar.setParent(arg0);
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Pesan Ruang ini", "/img/svg/edit-box-line.svg");
			button.setOrient("vertical");
			button.setTooltiptext("Pesan");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					PesanRuangan pesanRuangan = new PesanRuangan();
					pesanRuangan.setRuang(ruang);
					PesanRuanganAction.onAddExternal(event, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							MyMessageboxConfig
									.show("Pemesanan ruangan berhasil dilakukan.\nInformasi ruangan yang anda pesan :\n"
											+ arg0.getData(), "Informasi", 1, MyMessageboxConfig.INFORMATION);
						}
					}, pesanRuangan);
				}

			});
			button.setParent(toolbar);
		}
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Ruang.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.add(Restrictions.ilike("kodeRuangan", searchkodeRuangan.getValue(), MatchMode.ANYWHERE))
				.add(searchkapasitasruangan.getValue().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("kapasitasRuangan",
								Integer.parseInt(searchkapasitasruangan.getValue().toString())))
				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false))
				.add(searchgedung.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("gedung", searchgedung.getSelectedItem().getValue()));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) throws Exception {
		Common.initPaging(initCriteria(false), paging);

		mulai = !searchWaktuMulai.isVisible() || searchWaktuMulai.getValue() == null ? null
				: Double.parseDouble(dateFormat.format(searchWaktuMulai.getValue()));
		selesai = !searchWaktuSelesai.isVisible() || searchWaktuSelesai.getValue() == null ? null
				: Double.parseDouble(dateFormat.format(searchWaktuSelesai.getValue()));

		if (mulai == null || selesai == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Waktu mulai dan waktu selesai",
					"Kolom Waktu mulai dan waktu selesai belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Waktu mulai dan waktu selesai.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return;
		}

		if (searchTahunAjaran.getSelectedItem() == null || searchTahunAjaran.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Tahun akademik harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		if (searchsemester.getSelectedItem() == null) {
			MyMessageboxConfig.show("Jenis semester harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		List<Ruang> ruangs = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(ruangs);
		grid.setRowRenderer(new PerkuliahanRenderer());
		grid.setModelCheckMobile(strset);

	}
}
