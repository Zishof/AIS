package ais.action.master.monitor;


import ais.common.CommonSearchFilterHelper;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Decimalbox;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;

import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Jenjang;
import ais.database.model.Konsentrasi;
import ais.database.model.Mahasiswa;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.StatusMahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.TunggakanMahasiswa;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyComboitemConfig;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk monitor mahasiswa sudah membayar. Tipe ini merupakan titik masuk UI
 * yang menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus
 * oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code PembayaranUtil pembayaranUtil}, {@code
 * MyGrid grid}, {@code Paging paging}, {@code Textbox searchnim}, {@code Textbox searchnama}, {@code Combobox
 * searchfakultas}, {@code Combobox searchjurusan}, {@code Decimalbox searchtahun}; inisialisasi/lifecycle
 * ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code initCriteria()}); pembacaan/pencarian ({@code
 * onSearchDefault()}, {@code loadData()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface
 * yang disebut di atas.</p>
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
public class MonitorMahasiswaSudahMembayarAction extends GenericAutowireComposer implements DataLoader {

	public static PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();
	/**
	 * 
	 */
	private static final long serialVersionUID = 3786091220301468178L;
	private MyGrid grid;
	private Paging paging;
	private Textbox searchnim;
	private Textbox searchnama;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Decimalbox searchtahun;
	private Combobox searchkonsentrasi;
	private Combobox searchstatus;
	private Combobox searchprogram;
	private Combobox searchStatusAwalMahasiswa;
	private Combobox searchjenjang;
	private Combobox kewarganegaraan;
	private MyCheckboxConfig searchdosenPA;
	private AmbilDataDosenBanbox searchdosen;

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

		Common.initPrograms(searchprogram);
		Common.insertCombo(searchstatus, new String[] { "nama", "kodeEpsbed" }, StatusMahasiswa.class);
		Common.insertCombo(searchStatusAwalMahasiswa, "nama", StatusAwalMahasiswa.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		kewarganegaraan = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		if (comboitem != null) { comboitem.setLabel(ais.database.model.Mahasiswa.WNI); }
		if (comboitem != null) { comboitem.setValue(ais.database.model.Mahasiswa.WNI); }
		kewarganegaraan.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(ais.database.model.Mahasiswa.WNA); }
		if (comboitem != null) { comboitem.setValue(ais.database.model.Mahasiswa.WNA); }
		kewarganegaraan.appendChild(comboitem);

		class SearchJurusanEventListener implements EventListener {

			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub
				Common.clear(searchkonsentrasi);
				searchkonsentrasi.setSelectedItem(null);
				if (searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null) {
					return;
				}
				Common.insertCombo(searchkonsentrasi, "nama", Konsentrasi.class,
						CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false));
			}

		}
		searchjurusan.addEventListener("onChange", new SearchJurusanEventListener());

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		// Apabila user berwenang hanya di fakultas tertentu, maka user hanya
		// boleh mengakses data fakultas atau jurusan tertentu
		Tbmuser tbmuser = Common.getCurrentUser();

		Common.insertComboDanSemua(searchjenjang, "nama", Jenjang.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		if (tbmuser != null && tbmuser.ambilDosen() != null && tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")) {
			Dosen mydosen = tbmuser.ambilDosen();
			searchdosen.setValue(mydosen.getNama());
			searchdosen.setAttribute("myValue", mydosen);
			searchdosen.setAttribute("dosen", mydosen);
			searchdosen.setDisabled(true);
		}
		searchdosen.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	        FilterLanjutHelper.setup(comp);
}

	class MahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final TunggakanMahasiswa tunggakanMahasiswa = (TunggakanMahasiswa) arg1;
			final Mahasiswa mahasiswa = tunggakanMahasiswa.getMahasiswa();

			RevisiHelper.createNewRevisi(TunggakanMahasiswa.class, tunggakanMahasiswa, mahasiswa.getNim())
					.setParent(arg0);

			new Label(mahasiswa.getNama()).setParent(arg0);
			new Label(mahasiswa.getTahunangkatan() + "").setParent(arg0);
			new Label(mahasiswa.getWarganegara() == null ? "" : mahasiswa.getWarganegara()).setParent(arg0);
			new Label(mahasiswa.getSemesterMulai() == null ? "" : mahasiswa.getSemesterMulai()).setParent(arg0);

			new Label(mahasiswa.getJurusan() == null || mahasiswa.getJurusan().getFakultas() == null ? ""
					: mahasiswa.getJurusan().getFakultas().getNama()).setParent(arg0);
			new Label(mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama()).setParent(arg0);

			new Label(tunggakanMahasiswa.getTahunAkademik()).setParent(arg0);
			new Label(tunggakanMahasiswa.getSemester() + "").setParent(arg0);
			new Label(tunggakanMahasiswa.getJumlahTunggakan() == null ? ""
					: Common.numberFormat.get().format(tunggakanMahasiswa.getJumlahTunggakan())).setParent(arg0);

			new Label(tunggakanMahasiswa.getKegiatan() == null ? "0"
					: Common.numberFormat.get().format(tunggakanMahasiswa.getKegiatan().getAmount())).setParent(arg0);
		}

	}

	public Criteria initCriteria(boolean order) {

		StatusMahasiswa statusMahasiswa = (StatusMahasiswa) (searchstatus.getSelectedItem() == null
				|| searchstatus.getSelectedItem().getValue() == null ? null
						: searchstatus.getSelectedItem().getValue());

		Criterion criteriaStatus = Restrictions.sqlRestriction("true");
		if (statusMahasiswa != null) {
			String sql = "this_.id in (select mahasiswa from history_status_mahasiswa where status_mahasiswa="
					+ statusMahasiswa.getId() + " and tahunakademik = '" + Common.getCurrentTahunAkademik()
					+ "' and semester%2=" + (Common.isNowSemensterGanjil() ? 1 : 0) + ")";
			System.out.println("sql=>" + sql);
			criteriaStatus = Restrictions.sqlRestriction(sql);
		}

		Session session = HibernateUtil.currentSession();
		Dosen dosen = (Dosen) searchdosen.getAttribute("myValue");
		Criteria criteria = session.createCriteria(TunggakanMahasiswa.class)
				.add(Restrictions.ge("jumlahTunggakan", 1.0))

				.add(Restrictions.isNotNull("kegiatan"))

				.createCriteria("mahasiswa")
				.add(dosen != null ? Restrictions.eq("dosen", dosen.getId()) : Restrictions.sqlRestriction("1=1"))
				.add(searchdosenPA.isChecked() ? Restrictions.isNull("dosen") : Restrictions.sqlRestriction("1=1"));
		if (order)
			criteria.addOrder(Order.desc("tahunangkatan")).addOrder(Order.desc("tahunangkatan"))
					.addOrder(Order.asc("nim"));
		criteria.add(searchnama.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
				: Restrictions.ilike("nama", searchnama.getValue(), MatchMode.ANYWHERE))
				.add(searchnim.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nim", searchnim.getValue(), MatchMode.ANYWHERE))
				.add(searchprogram.getSelectedItem() == null || searchprogram.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("program", searchprogram.getSelectedItem().getValue()))
				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))
				.add(criteriaStatus)
				.add(searchStatusAwalMahasiswa.getSelectedItem() == null
						|| searchStatusAwalMahasiswa.getSelectedItem().getValue() == null
						|| searchStatusAwalMahasiswa.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("statusAwalMahasiswa",
										searchStatusAwalMahasiswa.getSelectedItem().getValue()))
				.add(searchtahun.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahunangkatan", searchtahun.getValue().intValue()))

				.add(searchkonsentrasi.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("konsentrasi", searchkonsentrasi.getSelectedItem().getValue()))

				.createCriteria("jurusan", Criteria.LEFT_JOIN)

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false))
				.add(searchjenjang.getSelectedItem() == null || searchjenjang.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("jenjang", searchjenjang.getSelectedItem().getValue()));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<TunggakanMahasiswa> tunggakanMahasiswas = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(tunggakanMahasiswas);
		grid.setRowRenderer(new MahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

	@Override
	public void loadData(Object value) {
		onSearchDefault(null);
	}

}
