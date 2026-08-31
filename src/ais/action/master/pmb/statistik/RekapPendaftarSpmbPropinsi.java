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
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.UIUtil;

import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Toolbarbutton;

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.GelombangPendaftaran;
import ais.database.model.JenisSeleksi;
import ais.database.model.Perkuliahan;
import ais.database.model.Propinsi;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;

/**
 * Tipe khusus untuk rekap pendaftar spmb propinsi. Kelas ini memberi nama dan batas tanggung jawab
 * yang eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyGrid grid}, {@code Combobox
 * jenisseleksisearch}, {@code Combobox searchTahunAjaran}, {@code Combobox searchJenisSemester}, {@code Combobox
 * searchGelombang}, {@code MyToolbarbuttonConfig find}; inisialisasi/lifecycle ({@code doBeforeCompose()},
 * {@code doAfterCompose()}); pembacaan/pencarian ({@code onSearchDefault()}). Bagian lain dari kontrak tetap
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
public class RekapPendaftarSpmbPropinsi extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3173385938131248092L;

	private MyGrid grid;

	private Combobox jenisseleksisearch;
	private Combobox searchTahunAjaran;
	protected Combobox searchJenisSemester;
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

		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		searchJenisSemester.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		searchJenisSemester.appendChild(comboitem);

		Common.selectComboItem(searchJenisSemester,
				Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
		searchJenisSemester.setReadonly(true);

		Common.insertCombo(jenisseleksisearch, "nama", JenisSeleksi.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		String tahunAkademikPenerimaanMahasiswaBaru = Common
				.getKonfigurasi("tahunAkademikPenerimaanMahasiswaBaru", Common.getCurrentTahunAkademik()).getNilai();

		Common.generateTahunAjaranDanSemua(searchTahunAjaran);Common.selectComboItem(searchTahunAjaran, Common.getCurrentTahunAkademik());

		Common.selectComboItem(searchTahunAjaran, tahunAkademikPenerimaanMahasiswaBaru);

		EventListener gelombangEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.insertCombo(searchGelombang, "nama", "tahunAkademik", GelombangPendaftaran.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
								searchTahunAjaran.getSelectedItem() == null || searchTahunAjaran.getSelectedItem().getValue() == null  ? Restrictions.sqlRestriction("true")
										: Restrictions.eq("tahunAkademik",
												searchTahunAjaran.getSelectedItem().getValue())));
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
		List<Propinsi> propinsi = session.createCriteria(Propinsi.class).addOrder(Order.asc("id"))
				.setMaxResults(Common.MAX_RESULT).list();
		ListModel strset = new SimpleListModel(propinsi);
		grid.setRowRenderer(new PropinsiRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Renderer lokal untuk layar/komponen {@link RekapPendaftarSpmbPropinsi}. Kelas ini menerjemahkan satu item
	 * data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link RekapPendaftarSpmbPropinsi} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see RekapPendaftarSpmbPropinsi
	 */
	class PropinsiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final Propinsi propinsi = (Propinsi) arg1;
			new Label(propinsi.getNama() == null ? "" : propinsi.getNama()).setParent(arg0);
			Session session = HibernateUtil.currentSession();

			MyGrid grid = new MyGrid();
			grid.setWidth("100%");
			grid.setParent(arg0);
			Rows rows = new Rows();
			rows.setParent(grid);

			Columns columns = new Columns();
			columns.setParent(grid);
			MyColumnConfig column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Jumlah Laki-Laki");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Jumlah Perempuan");

			Number peminatLaki = ((Number) session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.setProjection(Projections.rowCount())
					.add(Restrictions.eq("semesterMulai", searchJenisSemester.getSelectedItem().getValue()))
					.add(Restrictions.eq("propinsiCalon", propinsi)).add(Restrictions.eq("jenisKelamin", "Laki-laki"))
					.add(jenisseleksisearch.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("jenisSeleksi", jenisseleksisearch.getSelectedItem().getValue()))
					.add(searchTahunAjaran.getSelectedItem() == null || searchTahunAjaran.getSelectedItem().getValue() == null  ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("tahunAkademik", searchTahunAjaran.getSelectedItem().getValue()))
					.add(searchGelombang.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("gelombangPendaftaran", searchGelombang.getSelectedItem().getValue()))

			.setMaxResults(1).uniqueResult()).intValue();

			Row row = new Row();row.setValign("top");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig(peminatLaki + ""));

			Number peminatPerempuan = ((Number) session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.setProjection(Projections.rowCount())
					.add(Restrictions.eq("semesterMulai", searchJenisSemester.getSelectedItem().getValue()))
					.add(Restrictions.eq("propinsiCalon", propinsi)).add(Restrictions.eq("jenisKelamin", "Perempuan"))
					.add(jenisseleksisearch.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("jenisSeleksi", jenisseleksisearch.getSelectedItem().getValue()))
					.add(searchTahunAjaran.getSelectedItem() == null || searchTahunAjaran.getSelectedItem().getValue() == null  ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("tahunAkademik", searchTahunAjaran.getSelectedItem().getValue()))
					.add(searchGelombang.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("gelombangPendaftaran", searchGelombang.getSelectedItem().getValue()))

			.setMaxResults(1).uniqueResult()).intValue();

			row.appendChild(new ais.ui.util.MyLabelConfig(peminatPerempuan + ""));

			// peserta ujian

			grid = new MyGrid();// grid.setOddRowSclass("non-odd");
			grid.setWidth("100%");
			grid.setParent(arg0);
			rows = new Rows();
			rows.setParent(grid);

			columns = new Columns();
			columns.setParent(grid);
			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Jumlah Laki-Laki");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Jumlah Perempuan");
			Number pendaftarLaki = ((Number) session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.setProjection(Projections.rowCount())
					.add(Restrictions.eq("semesterMulai", searchJenisSemester.getSelectedItem().getValue()))
					.add(Restrictions.eq("propinsiCalon", propinsi)).add(Restrictions.eq("jenisKelamin", "Laki-laki"))
					.add(jenisseleksisearch.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("jenisSeleksi", jenisseleksisearch.getSelectedItem().getValue()))
					.add(searchTahunAjaran.getSelectedItem() == null || searchTahunAjaran.getSelectedItem().getValue() == null  ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("tahunAkademik", searchTahunAjaran.getSelectedItem().getValue()))
					.add(searchGelombang.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("gelombangPendaftaran", searchGelombang.getSelectedItem().getValue()))

			.add(Restrictions.isNotNull("noUjian")).add(Restrictions.ne("noUjian", "")).setMaxResults(1).uniqueResult())
					.intValue();

			row = new Row();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig(pendaftarLaki + ""));

			Number pendaftarPerempuan = ((Number) session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.setProjection(Projections.rowCount())
					.add(Restrictions.eq("semesterMulai", searchJenisSemester.getSelectedItem().getValue()))
					.add(Restrictions.eq("propinsiCalon", propinsi)).add(Restrictions.eq("jenisKelamin", "Perempuan"))
					.add(jenisseleksisearch.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("jenisSeleksi", jenisseleksisearch.getSelectedItem().getValue()))
					.add(searchTahunAjaran.getSelectedItem() == null || searchTahunAjaran.getSelectedItem().getValue() == null  ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("tahunAkademik", searchTahunAjaran.getSelectedItem().getValue()))
					.add(searchGelombang.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("gelombangPendaftaran", searchGelombang.getSelectedItem().getValue()))

			.add(Restrictions.isNotNull("noUjian")).add(Restrictions.ne("noUjian", "")).setMaxResults(1).uniqueResult())
					.intValue();

			row.appendChild(new ais.ui.util.MyLabelConfig(pendaftarPerempuan + ""));
			// end peserta ujian

		}

	}

}
