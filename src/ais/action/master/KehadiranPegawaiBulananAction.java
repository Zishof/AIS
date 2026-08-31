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
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.KehadiranPegawaiBulanan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Controller/action ZK untuk kehadiran pegawai bulanan. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Paging paging}, {@code MyGrid grid},
 * {@code Textbox searchpegawai}, {@code Decimalbox searchtahun}, {@code Decimalbox searchbulan}, {@code
 * MyToolbarbuttonConfig find}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()},
 * {@code initCriteria()}); pembacaan/pencarian ({@code onSearchDefault()}). Bagian lain dari kontrak tetap
 * mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class KehadiranPegawaiBulananAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchpegawai;
	private Decimalbox searchtahun;
	private Decimalbox searchbulan;

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

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "tahun", "bulan", "pegawai.nama", "pegawai.mycode", "pegawai.code", "aktif",
				"masuk", "alpa", "sakit", "izin", "belum", "cuti", "tepatWaktu", "pulangcepat", "terlambat", "lembur",
				"keterangan", "oleh" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(KehadiranPegawaiBulanan.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, find, comp);

	}

	/**
	 * Renderer lokal untuk layar/komponen {@link KehadiranPegawaiBulananAction}. Kelas ini menerjemahkan satu item
	 * data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link KehadiranPegawaiBulananAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see KehadiranPegawaiBulananAction
	 */
	class KehadiranPegawaiBulananRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			KehadiranPegawaiBulanan kehadiranPegawaiBulanan = (KehadiranPegawaiBulanan) arg1;
			new Label(kehadiranPegawaiBulanan.getTahun() + "").setParent(arg0);
			new Label(kehadiranPegawaiBulanan.getBulan() + "").setParent(arg0);

			Vbox vb;
			(vb = RevisiHelper.createNewRevisi(KehadiranPegawaiBulanan.class, kehadiranPegawaiBulanan,
					kehadiranPegawaiBulanan.getPegawai() == null ? "-"
							: kehadiranPegawaiBulanan.getPegawai().getNama()))
					.setParent(arg0);
			CommonMedia.tampilkanGambarKecil(kehadiranPegawaiBulanan.getPegawai()).setParent(vb);

			new Label(Common.numberFormat.get().format(kehadiranPegawaiBulanan.getAktif())).setParent(arg0);
			new Label(Common.numberFormat.get().format(kehadiranPegawaiBulanan.getMasuk())).setParent(arg0);
			new Label(Common.numberFormat.get().format(kehadiranPegawaiBulanan.getAlpa())).setParent(arg0);
			new Label(Common.numberFormat.get().format(kehadiranPegawaiBulanan.getSakit())).setParent(arg0);
			new Label(Common.numberFormat.get().format(kehadiranPegawaiBulanan.getIzin())).setParent(arg0);
			new Label(Common.numberFormat.get().format(kehadiranPegawaiBulanan.getBelum())).setParent(arg0);
			new Label(Common.numberFormat.get().format(kehadiranPegawaiBulanan.getCuti())).setParent(arg0);
			new Label(Common.numberFormat.get().format(kehadiranPegawaiBulanan.getTepatWaktu())).setParent(arg0);
			new Label(Common.numberFormat.get().format(kehadiranPegawaiBulanan.getPulangcepat())).setParent(arg0);
			new Label(Common.numberFormat.get().format(kehadiranPegawaiBulanan.getTerlambat())).setParent(arg0);
			new Label(Common.numberFormat.get().format(kehadiranPegawaiBulanan.getLembur())).setParent(arg0);

			Vbox my = new Vbox();
			my.setParent(arg0);
			Common.infoDiuploadOleh(kehadiranPegawaiBulanan.getOlehId(), kehadiranPegawaiBulanan.getOleh(), my);
			new Label(Common.dateFormat5.get().format(kehadiranPegawaiBulanan.getTanggal_dirubah())).setParent(my);
		}

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KehadiranPegawaiBulanan.class)
				.add(Restrictions.isNotNull("pegawai"));

		if (order)
			criteria.addOrder(Order.desc("tahun")).addOrder(Order.desc("bulan")).addOrder(Order.desc("pegawai.id"));

		if (!searchpegawai.getValue().trim().isEmpty()) {
			criteria.createAlias("pegawai", "pegawai").add(Restrictions.or(
					Restrictions.ilike("pegawai.nama", searchpegawai.getValue().trim(), MatchMode.ANYWHERE),
					Restrictions.or(
							Restrictions.ilike("pegawai.mycode", searchpegawai.getValue().trim(), MatchMode.ANYWHERE),
							Restrictions.ilike("pegawai.code", searchpegawai.getValue().trim(), MatchMode.ANYWHERE)))

			);
		}
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<KehadiranPegawaiBulanan> kehadiranPegawaiBulanan = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(kehadiranPegawaiBulanan);
		grid.setRowRenderer(new KehadiranPegawaiBulananRenderer());
		grid.setModelCheckMobile(strset);

	}

}
