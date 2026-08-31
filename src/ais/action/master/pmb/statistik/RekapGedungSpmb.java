package ais.action.master.pmb.statistik;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Gedung;
import ais.database.model.GelombangPendaftaran;
import ais.database.model.JenisSeleksi;
import ais.database.model.RuangPMB;
import ais.database.model.RuangPaketPMB;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.UIUtil;

/**
 * Tipe khusus untuk rekap gedung spmb. Kelas ini memberi nama dan batas tanggung jawab yang
 * eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyGrid grid}, {@code Combobox
 * jenisseleksisearch}, {@code Combobox searchTahunAjaran}, {@code Combobox searchGelombang}, {@code
 * MyToolbarbuttonConfig find}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()});
 * pembacaan/pencarian ({@code onSearchDefault()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau
 * interface yang disebut di atas.</p>
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
public class RekapGedungSpmb extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3173385938131248092L;

	private MyGrid grid;

	private Combobox jenisseleksisearch;
	private Combobox searchTahunAjaran;
	private Combobox searchGelombang;

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

		Common.insertCombo(jenisseleksisearch, "nama", JenisSeleksi.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		String tahunAkademikPenerimaanMahasiswaBaru = Common
				.getKonfigurasi("tahunAkademikPenerimaanMahasiswaBaru", Common.getCurrentTahunAkademik()).getNilai();

		Common.generateTahunAjaranDanSemua(searchTahunAjaran);
		Common.selectComboItem(searchTahunAjaran, Common.getCurrentTahunAkademik());

		Common.selectComboItem(searchTahunAjaran, tahunAkademikPenerimaanMahasiswaBaru);

		EventListener gelombangEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.insertCombo(searchGelombang, "nama", "tahunAkademik", GelombangPendaftaran.class,
						searchTahunAjaran.getSelectedItem() == null
								? Restrictions.and(
										Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
										Restrictions.sqlRestriction("true"))
								: Restrictions.eq("tahunAkademik", searchTahunAjaran.getSelectedItem().getValue()));
			}
		};

		gelombangEventListener.onEvent(null);
		searchTahunAjaran.addEventListener("onChange", gelombangEventListener);

		onSearchDefault(null);

		if (find != null) {
			MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Download", "/img/print.png");
			toolbarbutton.setParent(find.getParent());
			toolbarbutton.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					UIUtil.downloadGrid(grid);
				}
			});
		}
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Session session = HibernateUtil.currentSession();
		List<Gedung> gedungs = session.createCriteria(Gedung.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.asc("id")).setMaxResults(Common.MAX_RESULT).list();
		ListModel strset = new SimpleListModel(gedungs);
		grid.setRowRenderer(new BiodataCalonRenderer());
		grid.setModelCheckMobile(strset);

	}

	class BiodataCalonRenderer extends ais.ui.util.MyRowRenderer {

		@SuppressWarnings("unchecked")
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Gedung gedung = (Gedung) arg1;

			new Label(gedung.getNama()).setParent(arg0);

			List<RuangPMB> ruangPMBs = HibernateUtil.currentSession().createCriteria(RuangPMB.class)
					.add(Restrictions.eq("gedung", gedung))
					.add(searchTahunAjaran.getSelectedItem() == null
							|| searchTahunAjaran.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("1=1")
									: Restrictions.eq("tahunAkademik", searchTahunAjaran.getSelectedItem().getValue()))
					.list();
			new Label(ruangPMBs.size() + "").setParent(arg0);

			List<RuangPMB> ruangPMBPenuhs = HibernateUtil.currentSession().createCriteria(RuangPMB.class)
					.add(Restrictions.eq("gedung", gedung)).add(Restrictions.eq("penuh", 1))
					.add(searchTahunAjaran.getSelectedItem() == null
							|| searchTahunAjaran.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("1=1")
									: Restrictions.eq("tahunAkademik", searchTahunAjaran.getSelectedItem().getValue()))
					.list();

			new Label(ruangPMBPenuhs.size() + "").setParent(arg0);

			Integer kapasitasTotal = 0;
			for (RuangPMB r : ruangPMBs) {
				kapasitasTotal += r.getKapasitasRuangan();
			}

			new Label(kapasitasTotal + "").setParent(arg0);

			Integer ruangPaketPMBs = ((Number) HibernateUtil.currentSession().createCriteria(RuangPaketPMB.class)
					.createAlias("biodataCalonMahasiswa", "biodataCalonMahasiswa")
					.add(Restrictions.ne("biodataCalonMahasiswa.noUjian", ""))
					.add(Restrictions.isNotNull("biodataCalonMahasiswa.noUjian")).createCriteria("ruangPMB")
					.add(searchTahunAjaran.getSelectedItem() == null
							|| searchTahunAjaran.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("1=1")
									: Restrictions.eq("tahunAkademik", searchTahunAjaran.getSelectedItem().getValue()))
					.add(Restrictions.eq("gedung", gedung)).setProjection(Projections.rowCount()).uniqueResult())
					.intValue();
			new Label(ruangPaketPMBs + "").setParent(arg0);

		}

	}

}
