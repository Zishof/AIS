package ais.action.master.helper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zul.AbstractTreeModel;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Menu;

/**
 * {@link org.zkoss.zul.TreeModel} untuk komponen ZK {@code <tree>} yang menampilkan struktur
 * menu aplikasi AIS (entity {@link Menu}, tabel {@code menu}), dipakai oleh layar administrasi
 * menu ({@code ais.action.maintenance.MenuAction}, lihat method {@code onReloadTree}) untuk
 * menggambarkan seluruh pohon menu sistem secara lazy — tiap level anak baru di-query ke
 * database saat node dibuka/di-expand di UI, bukan dimuat sekaligus di awal.
 *
 * <p><b>Kunci hierarki BUKAN kolom {@code id}/{@code parentId} biasa</b>, melainkan sepasang
 * kolom {@code root} dan {@code child} pada {@link Menu}: setiap baris {@code Menu} punya nilai
 * {@code child} yang berfungsi sebagai "kunci lokal" node itu di dalam pohon, dan anak-anaknya
 * adalah semua baris {@code Menu} lain yang kolom {@code root}-nya sama dengan {@code child} milik
 * node induk. Root semu dari seluruh pohon adalah objek sentinel {@code new Menu(-1L, 0L)} yang
 * dibuat di constructor (root=-1 tidak pernah dipakai baris data mana pun, hanya penanda; child=0
 * berarti "seluruh menu level-atas", yaitu baris {@code Menu} yang kolom {@code root}-nya bernilai
 * 0). Pasangan root/child ini terpisah dari kolom {@code id} milik {@link Menu} — dua menu dengan
 * {@code id} berbeda tetap dianggap "sama sebagai anak" oleh tree ini bila nilai {@code child}-nya
 * sama, jadi konsistensi pohon bergantung sepenuhnya pada disiplin pengisian {@code root}/
 * {@code child} saat data menu dibuat, bukan pada foreign key formal.</p>
 *
 * <p><b>Kuirk yang perlu diketahui pemelihara:</b></p>
 * <ul>
 * <li>{@link #getChild(Object, int)} memeriksa {@code menus.size() < index}, padahal untuk index
 * valid seharusnya batasnya {@code <=} (indeks tertinggi yang valid adalah {@code size() - 1}).
 * Saat {@code index == menus.size()} pengecekan lolos ke cabang {@code else} dan
 * {@code menus.get(index)} akan melempar {@link IndexOutOfBoundsException}. Perilaku asli
 * dipertahankan apa adanya (tidak diperbaiki di sini) karena berada di luar cakupan
 * dokumentasi.</li>
 * <li>{@link #getIndexOfChild(Object, Object)} selalu mengembalikan {@code 0} tanpa memeriksa
 * argumennya — kontrak {@link org.zkoss.zul.TreeModel#getIndexOfChild} tidak diimplementasikan
 * secara penuh; ini tidak masalah selama komponen ZK yang memakai model ini tidak bergantung pada
 * nilai indeks yang akurat (mis. hanya render pohon read-only tanpa seleksi berbasis path).</li>
 * <li>{@link #getChildren(Menu)} memesan hasil dengan {@code nomorUrut}, lalu {@code root},
 * {@code child}, {@code label} — urutan {@code root} dan {@code child} di sini redundan karena
 * seluruh baris hasil sudah difilter memiliki {@code root} yang identik (hasil filter
 * {@link Restrictions#eq}), sehingga secara efektif pengurutan akhir hanya dipengaruhi
 * {@code nomorUrut} lalu {@code label}.</li>
 * </ul>
 *
 * @see AbstractTreeModel
 * @see Menu
 */
public class MenuTreeModel extends AbstractTreeModel {

	/**
	 *
	 */
	private static final long serialVersionUID = -5115651721345571411L;

	/**
	 * Membuat tree model dengan root semu {@code new Menu(-1L, 0L)} — objek ini tidak pernah
	 * disimpan ke database, hanya dipakai sebagai titik awal traversal ZK Tree; anak
	 * langsungnya adalah seluruh baris {@link Menu} dengan kolom {@code root} bernilai 0
	 * (menu level-atas).
	 */
	public MenuTreeModel() {
		super(new Menu(-1L, 0L));
	}

	/**
	 * Mengambil semua anak langsung dari {@code parentMenu}, yaitu baris {@link Menu} yang
	 * kolom {@code root}-nya sama dengan nilai {@code child} milik {@code parentMenu}.
	 * Query dijalankan setiap kali dipanggil (tidak di-cache) lewat sesi Hibernate aktif
	 * ({@link HibernateUtil#currentSession()}), diurutkan berdasarkan {@code nomorUrut} lalu
	 * {@code label} (lihat catatan kuirk pengurutan di Javadoc class).
	 *
	 * @param parentMenu node induk (bisa berupa root semu dari constructor maupun {@link Menu}
	 *            hasil query sebelumnya)
	 * @return daftar {@link Menu} anak langsung, bisa kosong bila node adalah leaf
	 */
	@SuppressWarnings("unchecked")
	public List<Menu> getChildren(Menu parentMenu) {
		Session session = HibernateUtil.currentSession();
		List<Menu> menus = session.createCriteria(Menu.class).addOrder(Order.asc("nomorUrut"))
				.add(Restrictions.eq("root", parentMenu.getChild())).addOrder(Order.asc("root"))
				.addOrder(Order.asc("child")).addOrder(Order.asc("label")).list();
		return menus;
	}

	/**
	 * Mengambil satu anak pada posisi {@code index} dari hasil {@link #getChildren(Menu)}.
	 * Lihat catatan kuirk di Javadoc class: pengecekan batas indeks memakai {@code <} bukan
	 * {@code <=}, sehingga {@code index == getChildren(parent).size()} akan melempar
	 * {@link IndexOutOfBoundsException} alih-alih mengembalikan {@code null}.
	 *
	 * @param parent node induk, di-cast ke {@link Menu}
	 * @param index posisi anak yang diminta (0-based)
	 * @return {@link Menu} anak pada posisi tersebut, atau {@code null} bila {@code index}
	 *         melebihi jumlah anak (kecuali kasus tepat sama dengan jumlah anak, lihat kuirk)
	 */
	public Object getChild(Object parent, int index) {
		Menu parentMenu = (Menu) parent;

		List<Menu> menus = getChildren(parentMenu);

		Menu menu = null;

		if (menus.size() < index) {
			menu = null;
		} else {
			menu = menus.get(index);
		}

		return menu;
	}

	/**
	 * Menghitung jumlah anak langsung dari {@code parent} lewat {@code COUNT(*)} SQL
	 * (proyeksi {@link Projections#rowCount()}) dengan filter {@code root} yang sama seperti
	 * {@link #getChildren(Menu)}, tanpa memuat baris datanya.
	 *
	 * @param parent node induk, di-cast ke {@link Menu}
	 * @return jumlah baris {@link Menu} yang kolom {@code root}-nya sama dengan {@code child}
	 *         milik {@code parent}
	 */
	public int getChildCount(Object parent) {
		Menu parentMenu = (Menu) parent;

		Session session = HibernateUtil.currentSession();

		Integer count = ((Number) session.createCriteria(Menu.class).add(Restrictions.eq("root", parentMenu.getChild()))
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();

		return count;
	}

	/**
	 * Node dianggap leaf (tanpa anak, tidak bisa di-expand di UI Tree) bila
	 * {@link #getChildCount(Object)} bernilai 0 — memicu satu query {@code COUNT(*)} tambahan
	 * tiap kali ZK menggambar ulang node.
	 *
	 * @param node node yang diperiksa, di-cast ke {@link Menu}
	 * @return {@code true} bila node tidak punya anak
	 */
	public boolean isLeaf(Object node) {
		return (getChildCount(node) == 0);
	}

	/**
	 * Implementasi kontrak {@link org.zkoss.zul.TreeModel#getIndexOfChild(Object, Object)} yang
	 * disederhanakan menjadi selalu {@code 0} — argumen {@code arg0} (parent) dan {@code arg1}
	 * (child) tidak diperiksa sama sekali. Cukup untuk pemakaian tree read-only di layar
	 * administrasi menu yang tidak memerlukan indeks anak yang akurat.
	 *
	 * @param arg0 node induk (tidak dipakai)
	 * @param arg1 node anak yang dicari indeksnya (tidak dipakai)
	 * @return selalu {@code 0}
	 * @since 5.0.6
	 * @see org.zkoss.zul.TreeModel#getIndexOfChild(java.lang.Object,
	 *      java.lang.Object)
	 */
	@Override
	public int getIndexOfChild(Object arg0, Object arg1) {
		return 0;
	}

}