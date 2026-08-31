package ais.action.master;

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
import org.zkoss.zul.Decimalbox;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CicilanPembayaran;
import ais.database.model.CicilanPembayaranGagal;
import ais.database.model.Fakultas;
import ais.database.model.Jenjang;
import ais.database.model.Jurusan;
import ais.database.model.Kegiatan;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk cicilan pembayaran gagal. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyGrid grid}, {@code Paging paging},
 * {@code Textbox searchnama}, {@code Combobox searchfakultas}, {@code Combobox jenissemester}, {@code Combobox
 * searchjurusan}, {@code Decimalbox searchtahun}, {@code Combobox searchjenjang}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code initCriteria()}); pembacaan/pencarian ({@code
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
public class CicilanPembayaranGagalAction extends GenericAutowireComposer implements DataCriteria {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730217402400328L;
	private MyGrid grid;

	private Paging paging;

	private Textbox searchnama;
	private Combobox searchfakultas;
	private Combobox jenissemester;
	private Combobox searchjurusan;
	private Decimalbox searchtahun;
	private Combobox searchjenjang;
	private MyDatebox start;
	private MyDatebox end;

	private MyToolbarbuttonConfig find;

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

		Common.insertComboDanSemua(searchjenjang, "nama", Jenjang.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GANJIL); }
		jenissemester.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GENAP); }
		jenissemester.appendChild(comboitem);

		comboitem = new MyComboitemConfig("Semua");
		if (comboitem != null) { comboitem.setValue(null); }
		jenissemester.appendChild(comboitem);
		if (jenissemester != null) { jenissemester.setSelectedItem(comboitem); }
		if (jenissemester != null) { jenissemester.setReadonly(true); }

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(CicilanPembayaranGagal.class, this, "kegiatan", "ke", "jenisPembayaran", "nilai",
				"tanggal", "itemBiaya", "keterangan");
		Common.appendKeToolbar(cetakToolbarbutton, find, comp);
	        FilterLanjutHelper.setup(comp);
}

	class CicilanPembayaranGagalRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final CicilanPembayaranGagal cicilanPembayaranGagal = (CicilanPembayaranGagal) arg1;
			final Kegiatan kegiatan = cicilanPembayaranGagal.getKegiatan();
			if (kegiatan.getMahasiswa() != null) {

				new Label(kegiatan.toString()).setParent(arg0);

				new Label(kegiatan.getMahasiswa() == null ? "" : kegiatan.getMahasiswa().getNim()).setParent(arg0);

				RevisiHelper
						.createNewRevisi(CicilanPembayaranGagal.class, kegiatan,
								kegiatan.getMahasiswa() == null ? "" : kegiatan.getMahasiswa().getNama())
						.setParent(arg0);

				new Label(kegiatan.getMahasiswa() == null || kegiatan.getMahasiswa().getJurusan() == null ? ""
						: kegiatan.getMahasiswa().getJurusan().getNama()).setParent(arg0);
				new Label(kegiatan.getMahasiswa() == null || kegiatan.getMahasiswa().getJurusan() == null
						|| kegiatan.getMahasiswa().getJurusan().getFakultas() == null ? ""
								: kegiatan.getMahasiswa().getJurusan().getFakultas().getNama()).setParent(arg0);
			} else if (kegiatan.getCalonMahasiswa() != null) {
				new Label(kegiatan.toString()).setParent(arg0);

				if (kegiatan.getJenisKegiatan() != null && kegiatan.getJenisKegiatan().getId()
						.equals(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId())) {
					new Label(kegiatan.getCalonMahasiswa() == null ? "" : kegiatan.getCalonMahasiswa().getNoUjian())
							.setParent(arg0);
				} else {
					new Label(
							kegiatan.getCalonMahasiswa() == null ? "" : kegiatan.getCalonMahasiswa().getNoRegistrasi())
									.setParent(arg0);
				}

				RevisiHelper
						.createNewRevisi(CicilanPembayaranGagal.class, kegiatan,
								kegiatan.getCalonMahasiswa() == null ? "" : kegiatan.getCalonMahasiswa().getNama())
						.setParent(arg0);

				new Label(kegiatan.getCalonMahasiswa() == null || kegiatan.getCalonMahasiswa().getProdiLulus() == null
						? "" : kegiatan.getCalonMahasiswa().getProdiLulus().getNama()).setParent(arg0);
				new Label(kegiatan.getCalonMahasiswa() == null || kegiatan.getCalonMahasiswa().getProdiLulus() == null
						|| kegiatan.getCalonMahasiswa().getProdiLulus().getFakultas() == null ? ""
								: kegiatan.getCalonMahasiswa().getProdiLulus().getFakultas().getNama()).setParent(arg0);
			}
			new Label(kegiatan.getSemster() + "").setParent(arg0);

			new Label(cicilanPembayaranGagal.getJenisPembayaran() == null ? ""
					: cicilanPembayaranGagal.getJenisPembayaran().getNama()).setParent(arg0);

			new Label(Common.dateFormat3.get().format(cicilanPembayaranGagal.getTanggal())).setParent(arg0);
			new Label(Common.numberFormat.get().format(cicilanPembayaranGagal.getNilai())).setParent(arg0);

			new Label(cicilanPembayaranGagal.getKeterangan()).setParent(arg0);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tidak Gagal", "/img/svg/warning-outline.svg");
			button.setTooltiptext("Tidak Gagal");
			button.setOrient("vertical");

			Tbmuser tbmuser = Common.getCurrentUser();
			button.setVisible(tbmuser != null 
					&& tbmuser.hakAkses() != null && tbmuser.hakAkses().getRoleId() != null
					&& tbmuser.hakAkses().getRoleId().trim().equalsIgnoreCase(Tbmrole.ADMINISTRATOR));
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					MyMessageboxConfig.show("Apakah yakin bahwa transaksi ini sukses ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

						@Override
						public void onEvent(Event event) throws Exception {
							int i = Integer.parseInt(event.getData().toString());
							if (i == MyMessageboxConfig.OK) {
								try {

									CicilanPembayaran cicilanPembayaran = Common
											.copyCicilanPembayaranKeSukses(cicilanPembayaranGagal);

									Common.refreshSaveOrUpdate(cicilanPembayaran);

									HibernateUtil.currentSession()
											.createSQLQuery("delete from cicilan_pembayaran_gagal where id="
													+ cicilanPembayaranGagal.getId())
											.executeUpdate();

									MyMessageboxConfig.show("Transaksi ini telah dipindahkan ke transaksi sukses",
											"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);

									onSearchDefault(null);

								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e); 
									MyMessageboxConfig
											.show("Data ini tidak dapat Reversal .., error-nya adalah sbagai berikut:"
													+ e.getMessage());
								}

							}

						}
					});

				}

			});
			button.setParent(arg0);
		}

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Fakultas fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null
				|| searchfakultas.getSelectedItem().getValue() == null
				|| searchfakultas.getSelectedItem().getValue() == null ? null
						: searchfakultas.getSelectedItem().getValue());
		Jurusan jurusan = (Jurusan) (searchjurusan.getSelectedItem() == null
				|| searchjurusan.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem().getValue() == null ? null
						: searchjurusan.getSelectedItem().getValue());

		Criteria criteria = session.createCriteria(CicilanPembayaranGagal.class).createAlias("kegiatan", "kegiatan")
				.add(Restrictions.gt("nilai", 0.01))

		.add((jenissemester.getSelectedItem() == null || jenissemester.getSelectedItem().getValue() == null
				? Restrictions.sqlRestriction("1=1")
				: (jenissemester.getSelectedItem().getValue().toString().equalsIgnoreCase(Perkuliahan.GENAP)
						? Restrictions.in("kegiatan.semster", Common.genap)
						: Restrictions.in("kegiatan.semster", Common.ganjil))));

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.createAlias("kegiatan.mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
				.createAlias("kegiatan.calonMahasiswa", "calonMahasiswa", Criteria.LEFT_JOIN)

		.createAlias("mahasiswa.jurusan", "jurusan", Criteria.LEFT_JOIN)
				.createAlias("calonMahasiswa.prodiLulus", "prodiLulus", Criteria.LEFT_JOIN)

		.add(searchtahun.getValue() == null ? Restrictions.sqlRestriction("1=1")
				: Restrictions.or(Restrictions.eq("mahasiswa.tahunangkatan", searchtahun.getValue().intValue()),
						Restrictions.eq("calonMahasiswa.tahun", searchtahun.getValue().intValue())))

		.add(jurusan == null ? Restrictions.sqlRestriction("1=1")
				: Restrictions.or(Restrictions.eq("mahasiswa.jurusan", jurusan),
						Restrictions.eq("calonMahasiswa.prodiLulus", jurusan)))

		.add((start == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (start.getValue() == null ? Restrictions.sqlRestriction("1=1")
				: Restrictions.sqlRestriction(
						"(this_.tanggal) >= ('" + Common.databaseDateFormat.get().format(start.getValue()) + " 00:00:00')")))

		.add((end == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (end.getValue() == null ? Restrictions.sqlRestriction("1=1")
				: Restrictions.sqlRestriction(
						"(this_.tanggal) <= ('" + Common.databaseDateFormat.get().format(end.getValue()) + " 23:59:59')")))

		.add(fakultas == null ? Restrictions.sqlRestriction("1=1")
				: Restrictions.or(Restrictions.eq("jurusan.fakultas", fakultas),
						Restrictions.eq("prodiLulus.fakultas", fakultas)))

		.add(Restrictions.or(
				Restrictions.or(
						Restrictions.or(
								Restrictions.ilike("mahasiswa.nim", searchnama.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("calonMahasiswa.nim", searchnama.getValue().trim(),
										MatchMode.ANYWHERE)),
				Restrictions.ilike("calonMahasiswa.noRegistrasi", searchnama.getValue().trim(), MatchMode.ANYWHERE)),
				Restrictions.ilike("calonMahasiswa.noUjian", searchnama.getValue().trim())));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<CicilanPembayaranGagal> cicilanPembayaranGagal = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(cicilanPembayaranGagal);
		grid.setRowRenderer(new CicilanPembayaranGagalRenderer());
		grid.setModelCheckMobile(strset);

	}
}
