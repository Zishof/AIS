package ais.action.master.kursus;

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
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.kursus.JenisPeserta;
import ais.database.model.kursus.ProdukPeserta;
import ais.database.model.kursus.TipePeserta;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Controller/action ZK untuk produk peserta. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Paging paging}, {@code MyGrid grid},
 * {@code Textbox searchkode}, {@code Combobox searchTipePeserta}, {@code Combobox searchJenisPeserta}, {@code
 * Combobox searchStatus}, {@code MyToolbarbuttonConfig add}; inisialisasi/lifecycle ({@code doBeforeCompose()},
 * {@code doAfterCompose()}, {@code initCriteria()}); pembacaan/pencarian ({@code onSearchDefault()}). Bagian
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
public class ProdukPesertaAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchkode;
	private Combobox searchTipePeserta;
	private Combobox searchJenisPeserta;
	private Combobox searchStatus;

	private MyToolbarbuttonConfig add;

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

		Common.insertComboDanSemua(searchTipePeserta, "nama", TipePeserta.class);
		Common.insertComboDanSemua(searchJenisPeserta, "nama", JenisPeserta.class);

		Comboitem comboitem = new Comboitem("Semua");
		if (comboitem != null) { comboitem.setValue(null); }
		searchStatus.appendChild(comboitem);
		comboitem = new Comboitem("Aktif");
		if (comboitem != null) { comboitem.setValue(true); }
		searchStatus.appendChild(comboitem);
		comboitem = new Comboitem("Tidak Aktif");
		if (comboitem != null) { comboitem.setValue(false); }
		searchStatus.appendChild(comboitem);
		if (searchStatus != null) { searchStatus.setSelectedIndex(0); }
		if (searchStatus != null) { searchStatus.setReadonly(true); }

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "kode", "pesertaKursus", "produkKursus",
				"pesertaPunyaProdukKursus.waktuBeli", "keterangan" };

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

	}

	class ProdukPesertaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final ProdukPeserta produkPeserta = (ProdukPeserta) arg1;

			if (produkPeserta.getPesertaKursus().getMahasiswa() != null) {
				CommonMedia.tampilkanGambarKecil(produkPeserta.getPesertaKursus().getMahasiswa()).setParent(arg0);
			} else if (produkPeserta.getPesertaKursus().getSiswa() != null) {
				CommonMedia.tampilkanGambarKecil(produkPeserta.getPesertaKursus().getSiswa()).setParent(arg0);
			} else if (produkPeserta.getPesertaKursus().getGuru() != null) {
				CommonMedia.tampilkanGambarKecil(produkPeserta.getPesertaKursus().getGuru()).setParent(arg0);
			} else if (produkPeserta.getPesertaKursus().getDosen() != null) {
				CommonMedia.tampilkanGambarKecil(produkPeserta.getPesertaKursus().getDosen()).setParent(arg0);
			} else if (produkPeserta.getPesertaKursus().getPegawai() != null) {
				CommonMedia.tampilkanGambarKecil(produkPeserta.getPesertaKursus().getPegawai()).setParent(arg0);
			} else if (produkPeserta.getPesertaKursus().getTbmuser() != null) {
				CommonMedia.tampilkanGambarKecil(produkPeserta.getPesertaKursus().getTbmuser()).setParent(arg0);
			} else {
				new Label("").setParent(arg0);
			}

			new Label(produkPeserta.getPesertaKursus().getKode()).setParent(arg0);

			RevisiHelper.createNewRevisi(ProdukPeserta.class, produkPeserta, produkPeserta.getPesertaKursus().getNama())
					.setParent(arg0);

			new Label(produkPeserta.getProdukKursus().getKode()).setParent(arg0);
			new Label(produkPeserta.getProdukKursus().getNama()).setParent(arg0);

			new Label(Common.dateFormat1.get().format(produkPeserta.getPesertaPunyaProdukKursus().getWaktuBeli()))
					.setParent(arg0);
			new Label(produkPeserta.getKeterangan()).setParent(arg0);
		}

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(ProdukPeserta.class).createAlias("pesertaKursus", "pesertaKursus")
				.createAlias("produkKursus", "produkKursus")

				.add(searchStatus.getSelectedItem() == null || searchStatus.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("aktif", searchStatus.getSelectedItem().getValue()))

				.createAlias("pesertaKursus.mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
				.createAlias("pesertaKursus.dosen", "dosen", Criteria.LEFT_JOIN);
		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria

				.add(searchTipePeserta.getSelectedItem() == null
						|| searchTipePeserta.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("pesertaKursus.tipePeserta",
										searchTipePeserta.getSelectedItem().getValue()))

				.add(searchJenisPeserta.getSelectedItem() == null
						|| searchJenisPeserta.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("pesertaKursus.jenisPeserta",
										searchJenisPeserta.getSelectedItem().getValue()))

				.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								Restrictions.ilike("pesertaKursus.kodeIdentitas", searchkode.getValue(),
										MatchMode.ANYWHERE),
								Restrictions.or(
										Restrictions.ilike("pesertaKursus.email", searchkode.getValue(),
												MatchMode.ANYWHERE),
										Restrictions.or(
												Restrictions.ilike("produkKursus.nama", searchkode.getValue(),
														MatchMode.ANYWHERE),
												Restrictions.ilike("produkKursus.kode", searchkode.getValue(),
														MatchMode.ANYWHERE)))));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<ProdukPeserta> produkPeserta = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(produkPeserta);
		grid.setRowRenderer(new ProdukPesertaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
