package ais.action.master.library.helper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.zkoss.zul.AbstractTreeModel;

/**
 * Tipe khusus untuk online book tree model. Kelas ini memberi nama dan batas tanggung jawab yang
 * eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * AbstractTreeModel}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah operasi lokal: {@code getChild()}, {@code getChildCount()},
 * {@code isLeaf}(). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
 *
 * @see AbstractTreeModel
 */
public class OnlineBookTreeModel extends AbstractTreeModel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 995156587050265211L;

	public OnlineBookTreeModel(File root) {
		super(root);
	}

	@Override
	public Object getChild(Object arg0, int arg1) {
		File file = (File) arg0;
		File[] files = file.listFiles();
		List<File> list = new ArrayList<File>();
		for (File myFile : files) {
			if (myFile.isDirectory()
					&& !myFile.getName().equalsIgnoreCase("files")) {
				list.add(myFile);
			}
		}
		return list.get(arg1);
	}

	@Override
	public int getChildCount(Object arg0) {
		File file = (File) arg0;
		File[] files = file.listFiles();
		List<File> list = new ArrayList<File>();
		for (File myFile : files) {
			if (myFile.isDirectory()
					&& !myFile.getName().equalsIgnoreCase("files")) {
				list.add(myFile);
			}
		}
		return list.size();
	}

	@Override
	public boolean isLeaf(Object arg0) {
		return getChildCount(arg0) == 0;
	}

}
