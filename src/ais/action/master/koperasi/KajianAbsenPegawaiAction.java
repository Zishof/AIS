package ais.action.master.koperasi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.FormulirKegiatan;
import ais.database.model.Pegawai;
import ais.database.model.Pertemuan;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Self check-in kehadiran Kajian untuk pegawai yang sedang login -- fitur "Voucher Pegawai".
 *
 * <p>Menampilkan sesi {@link Pertemuan} dari 3 kategori Kajian SUNGGUHAN (BUKAN baris sintetis
 * "Disiplin Kehadiran" -- itu dievaluasi terpisah dari data absensi harian, lihat
 * {@code AgregasiDisiplinKehadiranHelper}) dalam rentang {@link #HARI_KE_BELAKANG} hari ke belakang
 * s/d {@link #HARI_KE_DEPAN} hari ke depan, lengkap dengan status "sudah hadir" (offline/online)
 * atau dua tombol untuk absen sendiri.</p>
 *
 * <p><b>Konvensi encoding mode kehadiran (PENTING untuk pemeliharaan).</b> Format kolom
 * {@link Pertemuan#getAbsensi()} (ref,status,...,keterangan,...,jenis) tidak punya kolom "mode"
 * terpisah -- kolom {@code keterangan} dipakai KHUSUS di sini untuk menyimpan literal {@code
 * "ONLINE"} atau {@code "OFFLINE"} (BUKAN teks bebas seperti pemakaian {@code keterangan} di jalur
 * lain/akademik), supaya proses poin bulanan bisa membaca ulang mode kehadiran secara pasti lewat
 * {@link Pertemuan#retreiveAbsensiKeterangan(Long)} dan memilih nilai poin offline/online yang
 * sesuai dari {@link ais.database.model.KonfigurasiPoinKegiatan}.</p>
 *
 * <p><b>Di luar cakupan (sengaja).</b> Koreksi massal oleh admin (menambah/mengubah/menghapus
 * kehadiran pegawai LAIN) TIDAK dibangun di layar ini -- cakupannya murni self-service oleh pegawai
 * yang bersangkutan untuk dirinya sendiri.</p>
 */
public class KajianAbsenPegawaiAction extends GenericAutowireComposer {

	private static final long serialVersionUID = 1L;

	/** Sinkron dgn {@code KonfigurasiPoinKegiatanAction.NAMA_KAJIAN} indeks 0-2 -- TANPA baris sintetis "Disiplin Kehadiran" (indeks 3). */
	private static final String[] NAMA_KAJIAN_NYATA = {
			"Kajian Tafsir Al-Quran", "Kajian Hadist", "Kajian Al-Hikam"
	};
	private static final int HARI_KE_BELAKANG = 30;
	private static final int HARI_KE_DEPAN = 1;

	private MyGrid grid;
	private Label infoStatus;
	private Pegawai pegawaiSaya;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();

		pegawaiSaya = Common.getCurrentUser() == null ? null : Common.getCurrentUser().getPegawai();
		if (pegawaiSaya == null) {
			if (infoStatus != null) {
				infoStatus.setValue(
						"Akun ini tidak terhubung ke data Pegawai, sehingga tidak bisa mencatat kehadiran Kajian di sini.");
			}
			if (grid != null) {
				grid.setVisible(false);
			}
			return;
		}

		if (infoStatus != null) {
			infoStatus.setValue("Absen kehadiran Kajian 30 hari terakhir. Poin voucher diproses admin setelah kehadiran tercatat.");
		}
		muatData();
	}

	@SuppressWarnings("unchecked")
	private List<Pertemuan> ambilSesiKajian() {
		Session session = HibernateUtil.currentSession();
		List<FormulirKegiatan> daftarKegiatan = session.createCriteria(FormulirKegiatan.class)
				.add(Restrictions.in("nama", Arrays.asList(NAMA_KAJIAN_NYATA))).list();
		if (daftarKegiatan.isEmpty()) {
			return new ArrayList<Pertemuan>();
		}

		Calendar calMulai = Calendar.getInstance();
		calMulai.add(Calendar.DAY_OF_MONTH, -HARI_KE_BELAKANG);
		Date mulai = calMulai.getTime();
		Calendar calSampai = Calendar.getInstance();
		calSampai.add(Calendar.DAY_OF_MONTH, HARI_KE_DEPAN);
		Date sampai = calSampai.getTime();

		return session.createCriteria(Pertemuan.class)
				.add(Restrictions.in("formulirKegiatan", daftarKegiatan))
				.add(Restrictions.ge("tanggal", mulai))
				.add(Restrictions.le("tanggal", sampai))
				.addOrder(Order.desc("tanggal")).list();
	}

	private void muatData() {
		List<Pertemuan> daftar = ambilSesiKajian();
		ListModel model = new SimpleListModel(daftar);
		grid.setRowRenderer(new SesiKajianRenderer());
		grid.setModelCheckMobile(model);
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link KajianAbsenPegawaiAction}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link KajianAbsenPegawaiAction} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see KajianAbsenPegawaiAction
	 */
	class SesiKajianRenderer extends ais.ui.util.MyRowRenderer {
		@Override
		public void render(final Row row, Object obj) throws Exception {
			row.setValign("top");
			final Pertemuan pertemuan = (Pertemuan) obj;
			final FormulirKegiatan kegiatan = pertemuan.getFormulirKegiatan();

			new MyLabelConfig(kegiatan == null ? "" : kegiatan.getNama()).setParent(row);
			new Label(pertemuan.getTanggal() == null ? "" : Common.dateFormat6.get().format(pertemuan.getTanggal()))
					.setParent(row);

			String kode = pertemuan.retreiveAbsensiKode(pegawaiSaya.getId());
			boolean sudahHadir = "M".equals(kode);
			if (sudahHadir) {
				String mode = pertemuan.retreiveAbsensiKeterangan(pegawaiSaya.getId());
				new Label("✓ Hadir (" + (mode == null || mode.trim().isEmpty() ? "-" : mode) + ")")
						.setParent(row);
			} else {
				Hbox aksi = new Hbox();
				aksi.setSpacing("6px");
				ais.ui.util.MenuAksiBaris.pasangSelalu(aksi);
				aksi.setParent(row);

				MyToolbarbuttonConfig offline = new MyToolbarbuttonConfig("Hadir Offline", "/img/svg/check.svg");
				offline.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						catatKehadiran(pertemuan, "OFFLINE");
					}
				});
				offline.setParent(aksi);

				MyToolbarbuttonConfig online = new MyToolbarbuttonConfig("Hadir Online", "/img/svg/check.svg");
				online.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						catatKehadiran(pertemuan, "ONLINE");
					}
				});
				online.setParent(aksi);
			}
		}
	}

	private void catatKehadiran(Pertemuan pertemuan, String mode) {
		try {
			Session session = HibernateUtil.currentSession();
			Pertemuan p = (Pertemuan) session.load(Pertemuan.class, pertemuan.getId());
			p.populate(pegawaiSaya.getId(), ConstantValues.MASUK, mode, null,
					Common.timeFormat2.get().format(ais.ui.util.WaktuUtil.getDate()), "", "Pegawai");
			Common.refreshSaveOrUpdate(session, p);

			MyMessageboxConfig.show("Kehadiran Anda (" + mode + ") berhasil dicatat. Terima kasih.", "Info",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			muatData();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			try {
				MyMessageboxConfig.show("Gagal mencatat kehadiran, keterangan: " + e.getMessage(), "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/koperasi/KajianAbsenPegawaiAction.java"); }
		}
	}

}
