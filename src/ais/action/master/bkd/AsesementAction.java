package ais.action.master.bkd;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import ais.ui.util.MyInclude;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;

import ais.action.master.helper.DetailArtikelHelper;
import ais.action.master.penelitiandanpengabdian.helper.PengajuanPenelitianDanPengabdianHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.model.PengumumanAkademis;
import ais.database.model.PenunjangKinerjaDosen;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk asesement. Tipe ini merupakan titik masuk UI yang menghubungkan event
 * layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Tabpanel ringkasan}, {@code Tabpanel
 * bidangPendidikan}, {@code Tabpanel bidangPenelitian}, {@code Tabpanel bidangPengabdian}, {@code Tabpanel
 * bidangPenunjang}; inisialisasi/lifecycle ({@code doAfterCompose()}); operasi domain lain ({@code
 * onBidangPenunjang()}, {@code onBidangPendidikan()}, {@code onBidangPenelitian()}, {@code
 * onBidangPengabdian()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
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
public class AsesementAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	private Tabpanel ringkasan;
	private Tabpanel bidangPendidikan;
	private Tabpanel bidangPenelitian;
	private Tabpanel bidangPengabdian;
	private Tabpanel bidangPenunjang;

	public void onBidangPenunjang(Event event) throws Exception {
		if (bidangPenunjang.getChildren().isEmpty()) {

			bidangPenunjang.setHeight("9000px");
			MyInclude iframe = new MyInclude("/pages/master/penunjang_kinerja_dosen.zul");
			iframe.setParent(bidangPenunjang);
		}

	}

	public void onBidangPendidikan(Event event) throws Exception {
		if (bidangPendidikan.getChildren().isEmpty()) {
			bidangPendidikan.setHeight("9000px");
			ais.ui.util.MyButtonTabbox btnTabPendidikan = ais.ui.util.MyButtonTabbox.buat(bidangPendidikan, "100%", new int[] { 0 });

			// Tab 0: Pengajaran - load immediately
			new MyInclude("/pages/master/bkd/penugasan_dosen_mengajar.zul").setParent(
					btnTabPendidikan.tambahTab(0, "Pengajaran", "/img/svg/chalkboard-teacher-light.svg"));
			btnTabPendidikan.tambahTabLazy(1, "Pembimbing", "/img/svg/chalkboard-user.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
				@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
					new MyInclude("/pages/master/bkd/bimbingan_skripsi.zul").setParent(panel);
				}
			});
			btnTabPendidikan.tambahTabLazy(2, "Penguji", "/img/svg/pencil-square.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
				@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
					new MyInclude("/pages/master/bkd/penguji_skripsi.zul").setParent(panel);
				}
			});
			btnTabPendidikan.tambahTabLazy(3, "Pembimbing KKN", "/img/svg/users.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
				@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
					new MyInclude("/pages/master/kkn/kelompok_kkn.zul").setParent(panel);
				}
			});
			btnTabPendidikan.tambahTabLazy(4, "Pembimbing PKL", "/img/svg/user-business.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
				@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
					new MyInclude("/pages/master/pkl/kelompok_pkl.zul").setParent(panel);
				}
			});
			btnTabPendidikan.tambahTabLazy(5, "Penulis Buku", "/img/svg/book.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
				@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
					new MyInclude("/pages/master/buku_bahan_ajar.zul").setParent(panel);
				}
			});
			btnTabPendidikan.tambahTabLazy(6, "Bidang Pendidikan lain-nya", "/img/svg/three-dots.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
				@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
					new MyInclude("/pages/master/penunjang_kinerja_dosen.zul?jenis=" + PenunjangKinerjaDosen.PENDIDIKAN).setParent(panel);
				}
			});
		}
	}

	public void onBidangPenelitian(Event event) throws Exception {
		if (bidangPenelitian.getChildren().isEmpty()) {
			bidangPenelitian.setHeight("9000px");
			ais.ui.util.MyButtonTabbox btnTabPenelitian = ais.ui.util.MyButtonTabbox.buat(bidangPenelitian, "100%", new int[] { 0 });

			// Tab 0: Penelitian - load immediately
			{
				org.zkoss.zul.Div panelPenelitian = btnTabPenelitian.tambahTab(0, "Penelitian", "/img/svg/journal-bookmark.svg");
				PengajuanPenelitianDanPengabdianHelper pengajuanHelper = new PengajuanPenelitianDanPengabdianHelper();
				MyWindow addWindowPengajuan = new MyWindow();
				addWindowPengajuan.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				pengajuanHelper.displayPengajuan(true, null, PengumumanAkademis.UNTUK_UMUM, null, panelPenelitian, addWindowPengajuan, ConstantValues.PENELITIAN, "8500px");
			}
			btnTabPenelitian.tambahTabLazy(1, "Publikasi Ilmiah", "/img/svg/journal-check.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
				@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
					DetailArtikelHelper detailArtikelHelper = new DetailArtikelHelper(null);
					MyWindow addWindowPengajuan = new MyWindow();
					addWindowPengajuan.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					detailArtikelHelper.displayPengajuan(true, null, PengumumanAkademis.UNTUK_UMUM, null, panel, addWindowPengajuan, "8500px");
				}
			});
			btnTabPenelitian.tambahTabLazy(2, "Bidang Penelitian lain-nya", "/img/svg/three-dots.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
				@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
					new MyInclude("/pages/master/penunjang_kinerja_dosen.zul?jenis=" + PenunjangKinerjaDosen.PENELITIAN).setParent(panel);
				}
			});
		}
	}

	public void onBidangPengabdian(Event event) throws Exception {
		if (bidangPengabdian.getChildren().isEmpty()) {
			bidangPengabdian.setHeight("9000px");
			ais.ui.util.MyButtonTabbox btnTabPengabdian = ais.ui.util.MyButtonTabbox.buat(bidangPengabdian, "100%", new int[] { 0 });

			// Tab 0: Pengabdian - load immediately
			{
				org.zkoss.zul.Div panelPengabdian = btnTabPengabdian.tambahTab(0, "Pengabdian", "/img/svg/user-follow-line.svg");
				PengajuanPenelitianDanPengabdianHelper pengajuanHelper = new PengajuanPenelitianDanPengabdianHelper();
				MyWindow addWindowPengajuan = new MyWindow();
				addWindowPengajuan.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				pengajuanHelper.displayPengajuan(true, null, PengumumanAkademis.UNTUK_UMUM, null, panelPengabdian, addWindowPengajuan, ConstantValues.PENGABDIAN, "8500px");
			}
			btnTabPengabdian.tambahTabLazy(1, "Bidang Pengabdian lain-nya", "/img/svg/three-dots.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
				@Override public void muat(org.zkoss.zul.Div panel) throws Exception {
					new MyInclude("/pages/master/penunjang_kinerja_dosen.zul?jenis=" + PenunjangKinerjaDosen.PENGABDIAN).setParent(panel);
				}
			});
		}
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

		MyInclude iframe = new MyInclude("/pages/master/bkd/asesor_memberikan_penilaian.zul");
		if (iframe != null) { iframe.setParent(ringkasan); }

	}

}
