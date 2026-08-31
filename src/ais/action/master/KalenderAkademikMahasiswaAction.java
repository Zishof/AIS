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
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;

import ais.action.master.sekolah.util.SekolahUtil;
import ais.action.report.format1.akademik.LaporanKalenderAkademik;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.KalenderAkademik;
import ais.database.model.Perkuliahan;
import ais.database.model.sekolah.Sekolah;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk kalender akademik mahasiswa. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Combobox searchTahunAjaran}, {@code
 * Paging paging}, {@code MyGrid grid}, {@code Combobox searchGanjilGenap}, {@code Textbox searchnama}, {@code
 * Combobox searchfakultas}, {@code Combobox searchjurusan}, {@code Combobox searchprogram};
 * inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code onSearchDefault()}); pelaporan/ekspor ({@code onCetakKalenderAkademik()}). Bagian
 * lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class KalenderAkademikMahasiswaAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7857406591620520676L;
	private Combobox searchTahunAjaran;
	private Paging paging;
	private MyGrid grid;

	private Combobox searchGanjilGenap;
	private Textbox searchnama;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Combobox searchprogram;
	private Combobox searchyayasan;
	private Combobox searchsekolah;
	private Checkbox searchhanyaDiFakultas;
	private Checkbox searchhanyaDiProdi;
	private Tabpanel panelKalenderAkademik;
	private Sekolah sekolah1;

	private Hbox hbFakultasLabel;
	private Hbox hbFakultas;

	private Hbox hbYayasanLabel;
	private Hbox hbYayasan;
	private boolean pt = false;
	private boolean ya = false;

	public void onCetakKalenderAkademik(Event event) {
		if (panelKalenderAkademik.getChildren().size() == 0) {
			LaporanKalenderAkademik laporanKalenderAkademik = new LaporanKalenderAkademik();
			laporanKalenderAkademik.setHeight("100%");
			laporanKalenderAkademik.setWidth("100%");
			laporanKalenderAkademik.setParent(panelKalenderAkademik);
		}
	}

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);

		Common.generateTahunAjaranDanSemua(searchTahunAjaran);
		Common.selectComboItem(searchTahunAjaran, Common.getCurrentTahunAkademik());

		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		if (comboitem != null) { comboitem.setLabel("Semua"); }
		if (comboitem != null) { comboitem.setValue(null); }
		searchGanjilGenap.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(Perkuliahan.GANJIL); }
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GANJIL); }
		searchGanjilGenap.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(Perkuliahan.GENAP); }
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GENAP); }
		searchGanjilGenap.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel("Semua"); }
		if (comboitem != null) { comboitem.setValue(null); }
		searchGanjilGenap.appendChild(comboitem);

		if (searchGanjilGenap != null) { searchGanjilGenap.setSelectedItem(comboitem); }

		if (searchTahunAjaran != null) { searchTahunAjaran.setReadonly(true); }
		if (searchGanjilGenap != null) { searchGanjilGenap.setReadonly(true); }

		Common.initPrograms(searchprogram);

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah, true, false);

		sekolah1 = SekolahUtil.getSekolah();

		boolean[] ptYa = Common.chekPtAtauSekolah();
		pt = ptYa[0];
		ya = ptYa[1];

		if (searchhanyaDiFakultas != null) { searchhanyaDiFakultas.setVisible(pt); }
		if (searchhanyaDiProdi != null) { searchhanyaDiProdi.setVisible(pt); }

		if (hbFakultasLabel != null) {
			hbFakultasLabel.setVisible(pt && searchfakultas.getChildren().size() > 1);
		}
		if (hbFakultas != null) {
			hbFakultas.setVisible(pt);
		}
		if (hbYayasanLabel != null) {
			hbYayasanLabel.setVisible(ya);
		}

		if (hbYayasan != null) {
			hbYayasan.setVisible(ya);
		}

		if (sekolah1 != null && sekolah1.getId() != null) {
			if (hbFakultasLabel != null) {
				hbFakultasLabel.setVisible(false);
			}
			if (hbFakultas != null) {
				hbFakultas.setVisible(false);
			}
			if (hbYayasanLabel != null) {
				hbYayasanLabel.setVisible(true);
			}

			if (hbYayasan != null) {
				hbYayasan.setVisible(true);
			}

			searchhanyaDiFakultas.setVisible(true);
			searchhanyaDiProdi.setVisible(true);
		}

		Common.initPaging50(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

	        FilterLanjutHelper.setup(comp);
}

	/**
	 * Renderer lokal untuk layar/komponen {@link KalenderAkademikMahasiswaAction}. Kelas ini menerjemahkan satu
	 * item data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link KalenderAkademikMahasiswaAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see KalenderAkademikMahasiswaAction
	 */
	class KalenderAkademikMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final KalenderAkademik kalenderAkademik = (KalenderAkademik) arg1;

			String tgl = Common.dateFormat4.get().format(kalenderAkademik.getTanggalMulai()) + " s.d "
					+ Common.dateFormat4.get().format(kalenderAkademik.getTanggalSelesai());
			if (kalenderAkademik.getTanggalSelesai().equals(kalenderAkademik.getTanggalMulai())) {
				tgl = Common.dateFormat4.get().format(kalenderAkademik.getTanggalMulai());
			}

			arg0.setStyle(kalenderAkademik.getWarna());

			new Label(tgl).setParent(arg0);

			new Label(kalenderAkademik.getNamaKegiatanAkademik()).setParent(arg0);
			new Label(kalenderAkademik.getDeskripsiKegiatanAkademik()).setParent(arg0);

			new Label(kalenderAkademik.getTahunAjaran()).setParent(arg0);
			new Label(kalenderAkademik.getGanjilGenap()).setParent(arg0);

			new Label(kalenderAkademik.getJumlahHari() + " Hari").setParent(arg0);

			new Label(kalenderAkademik.getStatus()).setParent(arg0);

		}
	}

	public Criteria initCriteria(boolean order) {
		// private Checkbox searchhanyaDiFakultas;
		// private Checkbox searchhanyaDiProdi;
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KalenderAkademik.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		if (order)
			criteria.addOrder(Order.asc("tanggalMulai"));
		if (order)
			criteria.addOrder(Order.asc("tanggalSelesai"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
				: Restrictions.or(
						Restrictions.ilike("namaKegiatanAkademik", searchnama.getValue().trim(), MatchMode.ANYWHERE),
						Restrictions.ilike("deskripsiKegiatanAkademik", searchnama.getValue().trim(),
								MatchMode.ANYWHERE)))

				.add(searchTahunAjaran.getSelectedItem() == null
						|| searchTahunAjaran.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunAjaran", searchTahunAjaran.getSelectedItem().getValue()))

				.add(searchGanjilGenap.getSelectedItem() == null
						|| searchGanjilGenap.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("ganjilGenap", searchGanjilGenap.getSelectedItem().getValue()))

				.add(searchhanyaDiFakultas.isChecked() ? Restrictions.isNotNull("fakultas")
						: Restrictions.sqlRestriction("1=1"))

				.add(searchhanyaDiProdi.isChecked() ? Restrictions.isNotNull("jurusan")
						: Restrictions.sqlRestriction("1=1"))

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("jurusan"),
								CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false)))

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("fakultas"),
								CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false)))

				.add(searchprogram.getSelectedItem() == null || searchprogram.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("program"),
								Restrictions.eq("program", searchprogram.getSelectedItem().getValue())))

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						|| searchyayasan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false))

		;
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging50(initCriteria(false), paging);

		List<KalenderAkademik> kalenderAkademik = ConstantValues.simpleList(
				initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE_50)
						.setFirstResult(Common.ROWS_COUNT_ON_PAGE_50 * (paging == null ? 0 : paging.getActivePage())),
				KalenderAkademik.class);

		ListModel strset = new SimpleListModel(kalenderAkademik);
		grid.setRowRenderer(new KalenderAkademikMahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
