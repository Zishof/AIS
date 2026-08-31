package ais.action.master;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;

import ais.common.Common;
import ais.database.model.file.FileFoto;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Controller/action ZK untuk download auto backup. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Textbox searchnama}, {@code MyGrid
 * grid}, {@code String lokasi_file_backup}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code
 * doAfterCompose()}); pembacaan/pencarian ({@code onSearchDefault()}). Bagian lain dari kontrak tetap mengikuti
 * kelas induk atau interface yang disebut di atas.</p>
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
public class DownloadAutoBackupAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	private Textbox searchnama;

	private MyGrid grid;

	private String lokasi_file_backup;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		lokasi_file_backup = Common.getKonfigurasi("lokasi_directory_file_backup", "/backup/").getNilai();

		onSearchDefault(null);

	}

	class TutorialRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final File video = new File(arg1.toString());

			Label a = new Label();
			a.setValue(video.getName());
			// a.setHref(url);
			// a.setTarget("_blank");
			a.setParent(arg0);

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig(video.getName(),
					FileFoto.icon(video.getName()));

			toolbarbutton.setOrient("vertical");
			toolbarbutton.setParent(toolbar);
			toolbarbutton.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					Filedownload.save(video, "application/backup");

				}

			});

			toolbar.setParent(arg0);
		}

	}

	public void onSearchDefault(Event event) {

		File[] videos = new File(lokasi_file_backup).listFiles();

		List<String> files = new ArrayList<String>();
		for (File v : videos) {
			if (v.getName().toLowerCase().contains(searchnama.getValue().trim().toLowerCase())) {

				if (v.getName().toLowerCase().trim().contains(".backup")) {
					files.add(v.getAbsolutePath());
				}
			}
		}

		Collections.sort(files);

		ListModel strset = new SimpleListModel(files);
		grid.setRowRenderer(new TutorialRenderer());
		grid.setModelCheckMobile(strset);

	}

}
