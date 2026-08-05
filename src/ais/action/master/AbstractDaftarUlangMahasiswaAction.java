package ais.action.master;

import java.util.Collection;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Box;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Tabpanel;

import ais.action.master.helper.DaftarUlangPembayaranHelper;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailBiaya;
import ais.database.model.Kegiatan;
import ais.ui.util.MyLabelBoldAja;

/**
 * Base class bersama untuk {@code DaftarUlangMahasiswaLamaAction} (pembayaran Mahasiswa) dan
 * {@code DaftarUlangMahasiswaBaruAction} (pembayaran Calon Mahasiswa).
 *
 * <h3>Pola: shared behavior + accessor hook (rendah-risiko)</h3>
 * Kedua action hampir kembar; bedanya hanya <b>entitas pembayar</b> (Mahasiswa vs
 * BiodataCalonMahasiswa). Agar TIDAK perlu memindahkan puluhan field ber-autowire ZK ke base
 * (titik paling rawan), base ini <b>tidak memiliki field autowire</b>. Sebaliknya:
 *
 * <ul>
 * <li><b>Accessor hook</b> (abstrak) — subclass tetap menyimpan field-nya sendiri dan mengeksposnya
 *     lewat getter kecil (mis. {@link #getGridCicilan()}); base hanya memakai getter ini.</li>
 * <li><b>Perilaku bersama</b> (konkret, diwarisi) — logika yang identik di kedua action diletakkan
 *     SATU kali di sini dan didelegasikan ke {@link DaftarUlangPembayaranHelper} (stateless),
 *     sehingga salinan di tiap subclass dihapus tanpa kehilangan logika.</li>
 * </ul>
 *
 * Method besar yang berbeda per-entitas ({@code validasiPembayaran}, {@code menuBayar},
 * {@code listCicilan}) sengaja TETAP di masing-masing subclass: belum ada alur bersama yang
 * memanggilnya, sehingga memaksanya jadi hook hanya akan membuat pembungkus yang tak terpakai.
 * Ketika nanti diperlukan alur template bersama, hook spesifik bisa ditambahkan di sini.
 *
 * Java 1.7 / ZK 5.5.
 */
public abstract class AbstractDaftarUlangMahasiswaAction extends GenericAutowireComposer {

	private static final long serialVersionUID = 1L;

	// =========================================================================
	// ACCESSOR HOOK — state spesifik subclass (field autowire TETAP di subclass)
	// =========================================================================

	/** Grid daftar cicilan/angsuran (panel kanan). */
	protected abstract Grid getGridCicilan();

	/** Grid rincian item biaya (panel kiri, field {@code gridss}). */
	protected abstract Grid getGridBiaya();

	/** Daftar cicilan tersimpan + yang sedang diinput. */
	protected abstract List<CicilanPembayaran> getCicilanPembayarans();

	/** Semua item biaya (fallback "belum ada cicilan"); biasanya {@code itemBiayas.values()}. */
	protected abstract Collection<DetailBiaya> getSemuaItemBiaya();

	/** Label nilai "Dibayar" pada footer ringkasan. */
	protected abstract MyLabelBoldAja getFooterDibayar();

	/** Label terbilang "Dibayar". */
	protected abstract MyLabelBoldAja getFooterDibayarTerbilang();

	/** Label nilai "Total Bayar" pada footer ringkasan. */
	protected abstract MyLabelBoldAja getFooterTotal();

	/** Label terbilang "Total Bayar". */
	protected abstract MyLabelBoldAja getFooterTotalTerbilang();

	// =========================================================================
	// PERILAKU BERSAMA — diwarisi kedua subclass (delegasi ke helper stateless)
	// =========================================================================

	/** Cooldown anti-pembayaran-ganda (ms). */
	protected long getBayarCooldownMs() {
		return DaftarUlangPembayaranHelper.getBayarCooldownMs();
	}

	/** Signature himpunan pembayaran yang sedang diinput (untuk guard double-submit). */
	protected String buildBayarSignature(Kegiatan keg) {
		return DaftarUlangPembayaranHelper.buildBayarSignature(keg, getGridCicilan());
	}

	/** Nilai yang akan dibayar berdasar tampilan (footer + grid + cicilan tersimpan). */
	protected double hitungJumlahYangAkanDibayarDariTampilan() {
		return DaftarUlangPembayaranHelper.hitungJumlahYangAkanDibayarDariTampilan(getFooterDibayar(),
				getFooterTotal(), getGridCicilan(), getCicilanPembayarans());
	}

	/** Muat halaman lain ke dalam sebuah Tabpanel (sekali, bila masih kosong). */
	protected void loadIframeToTabpanel(Tabpanel panel, String url) {
		DaftarUlangPembayaranHelper.loadIframeToTabpanel(panel, url);
	}

	/** Daftar DetailBiaya yang masih perlu dibayar (item yang belum lunas). */
	protected List<DetailBiaya> updateDetalBiayaUntukDibayar() {
		return DaftarUlangPembayaranHelper.updateDetailBiayaUntukDibayar(getCicilanPembayarans(), getGridBiaya(),
				getSemuaItemBiaya());
	}

	/** Pasang kartu ringkasan pembayaran (Dibayar/Total Bayar + terbilang) dan kembalikan Box tombol. */
	protected Box pasangRingkasanBayar(Component parent) {
		return DaftarUlangPembayaranHelper.pasangRingkasanBayar(parent, getFooterDibayar(),
				getFooterDibayarTerbilang(), getFooterTotal(), getFooterTotalTerbilang());
	}
}
