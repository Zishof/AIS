package ais.common;

import java.util.HashMap;
import java.util.Map;

/**
 * Pengganti ringan {@code org.zkoss.zul.Paging} KHUSUS untuk jalur API/servlet (mis. LinimasaApi).
 *
 * <p><b>Kenapa perlu.</b> {@code org.zkoss.zul.Paging} adalah komponen UI ZK. Setter-nya
 * (setPageSize/setActivePage/setTotalSize) memanggil {@code Events.postEvent(...)} yang membutuhkan ZK
 * Desktop/Execution. Di servlet REST {@code Executions.getCurrent()} SELALU null, sehingga setiap setter
 * melempar {@link NullPointerException}. Lebih parah lagi: {@code setActivePage} milik ZK melempar
 * {@code WrongValueException} bila dipanggil SEBELUM totalSize diketahui (pageCount masih 1), sehingga
 * permintaan halaman &gt;= 2 diam-diam DIABAIKAN dan pengguna SELALU mendapat halaman 1.
 *
 * <p><b>Perilaku kelas ini.</b> Hanya menyimpan nilai + aritmatika: TANPA event, TANPA exception.
 * Halaman yang diminta disimpan apa adanya, lalu di-<i>clamp</i> ke rentang sah saat
 * {@link #getActivePage()} memakai totalSize/pageSize TERKINI — sehingga URUTAN pemanggilan setter tidak
 * lagi merusak hasil. Rumus jumlah halaman meniru ZK: {@code ceil(totalSize / pageSize)}, minimal 1.
 *
 * <p>Nama method sengaja dibuat SAMA dengan ZK Paging agar dapat dipakai sebagai pengganti langsung
 * (drop-in) tanpa mengubah kode pemanggil. Kompatibel Java 1.6 (tanpa lambda/diamond/Stream API).
 */
public class PagingApi {

	/** Default mengikuti ZK Paging. */
	private int pageSize = 20;
	private int totalSize = 0;
	private int activePage = 0;
	private final Map<String, Object> attributes = new HashMap<String, Object>();

	public void setPageSize(int size) {
		if (size > 0) {
			this.pageSize = size;
		}
	}

	public int getPageSize() {
		return pageSize;
	}

	public void setTotalSize(int size) {
		this.totalSize = size < 0 ? 0 : size;
	}

	public int getTotalSize() {
		return totalSize;
	}

	/** Jumlah halaman: ceil(totalSize/pageSize), minimal 1 (meniru perhitungan ZK). */
	public int getPageCount() {
		int ukuran = pageSize <= 0 ? 1 : pageSize;
		int jumlah = ((totalSize - 1) / ukuran) + 1;
		return jumlah <= 0 ? 1 : jumlah;
	}

	/**
	 * Simpan halaman yang DIMINTA, tanpa validasi/exception. Clamp sengaja DITUNDA ke
	 * {@link #getActivePage()} agar pemanggilan sebelum totalSize diketahui tidak lagi hilang.
	 */
	public void setActivePage(int page) {
		this.activePage = page < 0 ? 0 : page;
	}

	/** Halaman EFEKTIF: di-clamp ke [0, pageCount-1] memakai totalSize/pageSize terkini. */
	public int getActivePage() {
		int maksimal = getPageCount() - 1;
		if (maksimal < 0) {
			maksimal = 0;
		}
		if (activePage > maksimal) {
			return maksimal;
		}
		return activePage < 0 ? 0 : activePage;
	}

	/** Tidak relevan di API (tidak ada UI). Disediakan agar tetap drop-in terhadap ZK Paging. */
	public void setVisible(boolean visible) {
	}

	public boolean isVisible() {
		return totalSize > 0;
	}

	/** Properti TAMPILAN ZK (bentuk komponen). Tak relevan di API; disediakan agar tetap drop-in. */
	public void setMold(String mold) {
	}

	/** Properti TAMPILAN ZK (loncatan nomor halaman). Tak relevan di API; agar tetap drop-in. */
	public void setPageIncrement(int increment) {
	}

	public void setAttribute(String nama, Object nilai) {
		attributes.put(nama, nilai);
	}

	public Object getAttribute(String nama) {
		return attributes.get(nama);
	}

	public Object removeAttribute(String nama) {
		return attributes.remove(nama);
	}
}
