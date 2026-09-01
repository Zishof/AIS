package ais.action.master;

import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;

import ais.action.master.konfigurasi.SkemaKonfigurasi;
import ais.common.Common;
import ais.common.CommonPrivilages;

/**
 * Controller/action ZK untuk konfigurasi lkp. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * KonfigurasiNewAction}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah operasi lokal: {@code doBeforeCompose()}, {@code
 * doAfterCompose()}, {@code onTampil}(). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface
 * yang disebut di atas.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see KonfigurasiNewAction
 */
public class KonfigurasiLkpAction extends KonfigurasiNewAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterComposeOri(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		onTampil();

	}

	public void onTampil() {
		Rows rows = createSpan("Pengaturan Sasaran Kerja Pegawai");
		for (SkemaKonfigurasi.Butir butir : SkemaKonfigurasi.SKP) {
			rows.appendChild(baris(SkemaKonfigurasi.SKP, butir.kunci));
		}
	}

	/**
	 * Bangun satu baris konfigurasi dari skema bersama.
	 *
	 * <p>Label dan nilai bawaannya TIDAK ditulis di sini melainkan dibaca dari
	 * {@link SkemaKonfigurasi}, karena {@code Common.getKonfigurasi} menyimpan
	 * bawaan yang disebut pemanggil ketika barisnya belum ada — bila layar ini
	 * dan kontrak native menyebut bawaan berbeda, yang dibuka lebih dulu akan
	 * menetapkannya secara permanen.</p>
	 */
	private Row baris(java.util.List<SkemaKonfigurasi.Butir> skema, String kunci) {
		SkemaKonfigurasi.Butir b = SkemaKonfigurasi.cari(skema, kunci);
		if (b == null) {
			throw new IllegalStateException("Kunci konfigurasi tidak ada di skema: " + kunci);
		}
		if (SkemaKonfigurasi.SAKLAR.equals(b.tipe)) {
			return createRowActiveDefault(b.label, b.kunci, b.bawaan());
		}
		if (SkemaKonfigurasi.TEKS_PANJANG.equals(b.tipe)) {
			return createRowNilai(b.label, b.kunci, b.bawaan(), b.baris, null);
		}
		return createRowNilai(b.label, b.kunci, b.bawaan());
	}
}
