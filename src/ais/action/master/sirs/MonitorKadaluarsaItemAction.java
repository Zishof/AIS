package ais.action.master.sirs;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.SimpleListModel;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.sirs.detail.KadaluarsaAction;
import ais.action.report.format1.sirs.inventory.LaporanKadaluarsaWindow;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.Lokasi;
import ais.database.model.library.JenisItem;
import ais.database.model.sirs.ItemMedis;
import ais.database.model.sirs.SatuanItem;
import ais.ui.util.MyTextbox;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk monitor kadaluarsa item. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Grid grid}, {@code MyTextbox
 * searchnama}, {@code MyTextbox searchkode}, {@code MyTextbox searchbarcode}, {@code Combobox searchsatuanItem},
 * {@code Combobox searchjenisItem}, {@code Combobox searchlokasi}, {@code Lokasi myLokasi};
 * inisialisasi/lifecycle ({@code doAfterCompose()}); pembacaan/pencarian ({@code onSearchDefault()});
 * pelaporan/ekspor ({@code onCetak()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang
 * disebut di atas.</p>
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
public class MonitorKadaluarsaItemAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3786091220301468178L;

	private Grid grid;
	private MyTextbox searchnama;
	private MyTextbox searchkode;
	private MyTextbox searchbarcode;

	private Combobox searchsatuanItem;
	private Combobox searchjenisItem;
	private Combobox searchlokasi;

	private Lokasi myLokasi;

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			execution.sendRedirect("/logoff");
			return;
		}

		Common.insertCombo(searchsatuanItem, "nama", SatuanItem.class);
		Common.insertCombo(searchjenisItem, "nama", JenisItem.class);

		myLokasi = Common.getCurrentLokasi();
		Common.insertCombo(searchlokasi, "nama", Lokasi.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(searchlokasi, myLokasi);
		// searchlokasi.setDisabled(myLokasi != null);

		onSearchDefault(null);
	        FilterLanjutHelper.setup(comp);
}

	/**
	 * Renderer lokal untuk layar/komponen {@link MonitorKadaluarsaItemAction}. Kelas ini menerjemahkan satu item
	 * data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link MonitorKadaluarsaItemAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see MonitorKadaluarsaItemAction
	 */
	class MonitorkadaluarsaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			Object[] objects = (Object[]) arg1;
			Long itemId = ((Number) objects[0]).longValue();
			Date tanggalKadaluarsa = (Date) objects[2];
			String gudang = (String) objects[3];

			Calendar a = Calendar.getInstance();
			a.setTime(tanggalKadaluarsa);
			Calendar b = Calendar.getInstance();

			int days = Common.getBetweenTwoDates(b.getTime(), a.getTime());
			System.out.println("days = " + days);
			if (days <= 30) {
				arg0.setStyle("background-color:red;");
			} else if (days <= 90) {
				arg0.setStyle("background-color:yellow;");
			}

			ItemMedis item = (ItemMedis) ConstantValues.ambil(ItemMedis.class.getName(), itemId);

			new KadaluarsaAction(item).setParent(arg0);

			new Label(item.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(ItemMedis.class, item, item.getNama()).setParent(arg0);
			new Label(item.getBarcode()).setParent(arg0);
			new Label(item.getSatuanItem() == null ? "" : item.getSatuanItem().getNama()).setParent(arg0);
			new Label(item.getJenisItem() == null ? "" : item.getJenisItem().getNama()).setParent(arg0);
			new Label(tanggalKadaluarsa == null ? "" : Common.dateFormat2.get().format(tanggalKadaluarsa)).setParent(arg0);
			new Label(Common.numberFormat.get().format(days) + " hari").setParent(arg0);
			new Label(gudang).setParent(arg0);

		}
	}

	public void onCetak(Event event) throws InterruptedException {
		LaporanKadaluarsaWindow laporanKadaluarsaWindow = new LaporanKadaluarsaWindow();
		laporanKadaluarsaWindow.setTitle("Laporan Kadalursa");
		laporanKadaluarsaWindow.setHeight("95%");
		laporanKadaluarsaWindow.setWidth("95%");
		laporanKadaluarsaWindow.setClosable(true);
		page.getFirstRoot().appendChild(laporanKadaluarsaWindow);
		laporanKadaluarsaWindow.onModal();
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Session session = HibernateUtil.currentSession();

		JenisItem jenisItem = (JenisItem) (searchjenisItem.getSelectedItem() == null ? null
				: searchjenisItem.getSelectedItem().getValue());
		SatuanItem satuanItem = (SatuanItem) (searchsatuanItem.getSelectedItem() == null ? null
				: searchsatuanItem.getSelectedItem().getValue());

		Lokasi lokasi = (Lokasi) (searchlokasi.getSelectedItem() == null ? null
				: searchlokasi.getSelectedItem().getValue());

		String sql = "select a.item, max(c.nama) as nama_item, " + "max(a.tanggal_kadaluarsa) as tanggal_expired, "
				+ "max(d.nama) as lokasi from sirs.kadaluarsa a " + "left join sirs.item_medis c on (a.item = c.id) "
				+ "left join asset.lokasi d on (a.lokasi = d.id) " + "where 1=1 "
				+ (searchkode.getValue().trim().equals("") ? ""
						: " and c.kode ilike '%" + searchkode.getValue().trim() + "%' ")
				+ " "
				+ (searchnama.getValue().trim().equals("") ? ""
						: " and c.nama ilike '%" + searchnama.getValue().trim() + "%' ")
				+ "  "
				+ (searchbarcode.getValue().trim().equals("") ? ""
						: "and c.barcode ilike '%" + searchbarcode.getValue().trim() + "%'")
				+ "  and c.jenis_item = " + (jenisItem == null ? "c.jenis_item" : jenisItem.getId())
				+ " and a.lokasi = " + (lokasi == null ? "a.lokasi" : lokasi.getId()) + " and c.satuan_item = "
				+ (satuanItem == null ? "c.satuan_item" : satuanItem.getId())
				+ "  group by a.lokasi,a.item order by tanggal_expired asc";

		List<Object[]> item = session.createSQLQuery(sql).list();
		ListModel strset = new SimpleListModel(item);
		grid.setRowRenderer(new MonitorkadaluarsaRenderer());
		grid.setModel(strset);
		grid.renderAll();

	}

}
