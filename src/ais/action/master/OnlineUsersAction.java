package ais.action.master;

import java.util.ArrayList;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
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
import ais.common.CommonPrivilages;
import ais.common.SecurityFilter;
import ais.database.model.Dosen;
import ais.database.model.Mahasiswa;
import ais.database.model.OnlineUsers;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyGrid;

/**
 * Controller/action ZK untuk online users. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code org.zkoss.zul.Html dashboardHtml},
 * {@code org.zkoss.zul.Html progressHtml}, {@code Paging paging}, {@code MyGrid grid}, {@code Textbox
 * searchnama}, {@code boolean edit}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code
 * doAfterCompose()}); pembacaan/pencarian ({@code onSearchDefault()}, {@code refreshDashboardAman()}). Bagian
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
public class OnlineUsersAction extends GenericAutowireComposer  {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	private org.zkoss.zul.Html dashboardHtml;
	private org.zkoss.zul.Html progressHtml;


	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private boolean edit = false;

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

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		onSearchDefault(null);
		refreshDashboardAman();
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
		refreshDashboardAman();
		}
		});

	}

	/**
	 * Renderer lokal untuk layar/komponen {@link OnlineUsersAction}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link OnlineUsersAction} dan dapat mengakses state
	 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see OnlineUsersAction
	 */
	class OnlineUsersRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final OnlineUsers onlineUsers = (OnlineUsers) arg1;
			new Label(onlineUsers.getLogin() == null ? "" : onlineUsers.getLogin().getIp()).setParent(arg0);
			RevisiHelper.createNewRevisi(OnlineUsers.class, onlineUsers,
					onlineUsers.getLogin() == null || onlineUsers.getLogin().getLogin() == null ? ""
							: Common.dateFormat3.get().format(onlineUsers.getLogin().getLogin()))
					.setParent(arg0);

			Tbmuser tbmuser = onlineUsers.getTbmuser();
			Dosen dosen = onlineUsers.getDosen();
			Mahasiswa mahasiswa = onlineUsers.getMahasiswa();
			Siswa siswa = onlineUsers.getSiswa();

			String nama = siswa != null ? siswa.getNama() + " (" + siswa.getNomorIndukNasional() + ")"
					: mahasiswa != null ? mahasiswa.getNama() + " (" + mahasiswa.getNim() + ")"
							: dosen != null ? dosen.getNama() : tbmuser == null ? "" : tbmuser.getUserNama();

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			if (dosen != null) {
				CommonMedia.tampilkanGambarKecil(dosen).setParent(vbox);
			} else if (mahasiswa != null) {
				CommonMedia.tampilkanGambarKecil(mahasiswa).setParent(vbox);
			} else if (siswa != null) {
				CommonMedia.tampilkanGambarKecil(siswa).setParent(vbox);
			} else {
				CommonMedia.tampilkanGambarKecil(tbmuser).setParent(vbox);
			}
			vbox.appendChild(new Label(nama));

			new Label(tbmuser == null || tbmuser.hakAkses() == null
					? (siswa != null ? "Siswa"
							: mahasiswa != null ? Common.getBahasa("label_mahasiswa")
									: dosen != null ? Common.getBahasa("label_dosen") : "")
					: tbmuser.hakAkses().getRoleName()).setParent(arg0);
			if (siswa != null) {
				new Label(siswa.getYayasan() == null ? "" : siswa.getYayasan().getNama()).setParent(arg0);
				new Label(siswa.getSekolah() == null ? "" : siswa.getSekolah().getNama()).setParent(arg0);
			} else if (tbmuser != null && tbmuser.ambilGuru() != null) {
				new Label(tbmuser.ambilGuru().getYayasan() == null ? "" : tbmuser.ambilGuru().getYayasan().getNama()).setParent(arg0);
				new Label(tbmuser.ambilGuru().getSekolah() == null ? "" : tbmuser.ambilGuru().getSekolah().getNama()).setParent(arg0);
			} else {

				new Label(onlineUsers.getFakultas() == null ? "" : onlineUsers.getFakultas().getNama()).setParent(arg0);
				new Label(onlineUsers.getJurusan() == null ? "" : onlineUsers.getJurusan().getNama()).setParent(arg0);
			}

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(onlineUsers.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onlineUsers.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(onlineUsers);
				}
			});

		}

	}

	public void onSearchDefault(Event event) {
		String n = searchnama.getValue().trim();
		List<OnlineUsers> onlineUsers = new ArrayList<OnlineUsers>();
		for (OnlineUsers online : SecurityFilter.dataOnline.values()) {
			if (n.isEmpty() || online.getNama().toLowerCase().contains(n.toLowerCase())) {
				onlineUsers.add(online);
			}
		}

		ListModel strset = new SimpleListModel(onlineUsers);
		grid.setRowRenderer(new OnlineUsersRenderer());
		grid.setModelCheckMobile(strset);

	}


	private void refreshDashboardAman() {
		try {
			ais.action.master.helper.GenericActionDashboardHelper.refreshFromCriteria(dashboardHtml, progressHtml, this,
					"Dasbor Pengguna Online", "Lihat pengguna yang sedang aktif agar admin mudah memantau sesi dan aktivitas sistem saat ini.");
		} catch (Exception e) {
			try {
				ais.common.Common.tampilErrorJikaAdmin(e);
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/OnlineUsersAction.java:168");
			}
		}
	}

}
