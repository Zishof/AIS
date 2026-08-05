package ais.action.master.koperasi;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.FormulirKegiatan;
import ais.database.model.JenisFormulirKegiatan;
import ais.database.model.KonfigurasiPoinKegiatan;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Kelola nilai poin/voucher (Rupiah) untuk fitur "Voucher Pegawai" -- bonus saldo koperasi dari
 * kehadiran Kajian (offline/online) &amp; syarat Disiplin Kehadiran bulanan.
 *
 * <p><b>SENGAJA tidak punya tombol Tambah/Hapus.</b> Layar ini murni mengatur ANGKA (poin
 * offline/online, masa berlaku voucher, status aktif) untuk 4 baris {@link FormulirKegiatan} yang
 * dijamin selalu ada lewat {@link #pastikanDataDefaultAda()} (self-heal, idempoten, dipanggil
 * setiap layar ini dibuka -- baris yang sudah ada TIDAK ditimpa ulang). Menambah/mengubah kategori
 * kajian sungguhan (jadwal, lokasi, jadwal sesi Pertemuan) tetap lewat menu "Kegiatan"
 * ({@code FormulirKegiatanAction}) yang sudah ada -- dipisah supaya relasi ke sesi kehadiran tidak
 * dikelola dari dua tempat berbeda sekaligus.</p>
 *
 * <p>3 baris pertama ("Kajian Tafsir Al-Quran"/"Kajian Hadist"/"Kajian Al-Hikam") berelasi ke sesi
 * {@link ais.database.model.Pertemuan} sungguhan (kehadiran dicatat lewat layar self check-in
 * pegawai). Baris ke-4 ("Disiplin Kehadiran") SINTETIS -- tidak punya sesi Pertemuan, evaluasinya
 * murni dari data absensi harian pegawai (izin/sakit/cuti/telat), sehingga kolom "Poin Online"-nya
 * tidak relevan (dibiarkan 0 dari default, admin boleh isi ulang bila skema poinnya berubah).</p>
 */
public class KonfigurasiPoinKegiatanAction extends GenericAutowireComposer {

	private static final long serialVersionUID = 1L;

	private static final String[] NAMA_KAJIAN = {
			"Kajian Tafsir Al-Quran", "Kajian Hadist", "Kajian Al-Hikam", "Disiplin Kehadiran"
	};
	private static final String[] KETERANGAN_KAJIAN = {
			"Setiap hari Sabtu pagi. Lokasi: aula utama/sekitar Yayasan Al-Bahjah.",
			"Setiap hari Minggu pagi. Lokasi: aula utama/sekitar Yayasan Al-Bahjah.",
			"Setiap hari Senin malam. Lokasi: Masjid At Takwa Kota Cirebon.",
			"Baris sintetis (BUKAN kegiatan Kajian sungguhan, tidak punya sesi Pertemuan) -- mewakili "
					+ "syarat \"tidak pernah izin/sakit/cuti/telat dalam sebulan\", dievaluasi dari data "
					+ "absensi harian pegawai yang sudah ada, bukan dari kehadiran sesi."
	};
	private static final double[] POIN_OFFLINE_DEFAULT = { 7500, 7500, 7500, 4400 };
	private static final double[] POIN_ONLINE_DEFAULT = { 3000, 3000, 3000, 0 };
	private static final int DURASI_BERLAKU_HARI_DEFAULT = 365;

	private MyGrid grid;
	private boolean edit = false;

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
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);

		pastikanDataDefaultAda();
		muatData();
	}

	/**
	 * Self-heal: pastikan 3 {@link JenisFormulirKegiatan} (kategori Kajian) + 1 sintetis (Disiplin
	 * Kehadiran), 4 {@link FormulirKegiatan}, dan 4 {@link KonfigurasiPoinKegiatan} SELALU ada --
	 * dicek ulang (idempoten, cocok lewat nama PERSIS di {@link #NAMA_KAJIAN}) setiap layar ini
	 * dibuka. Baris yang SUDAH ADA sama sekali tidak disentuh (nilai yang pernah diubah admin lewat
	 * layar ini tidak pernah ditimpa ulang oleh nilai default).
	 */
	private void pastikanDataDefaultAda() {
		Session session = HibernateUtil.currentSession();
		for (int i = 0; i < NAMA_KAJIAN.length; i++) {
			try {
				JenisFormulirKegiatan jenis = (JenisFormulirKegiatan) session
						.createCriteria(JenisFormulirKegiatan.class)
						.add(Restrictions.eq("nama", NAMA_KAJIAN[i])).uniqueResult();
				if (jenis == null) {
					jenis = new JenisFormulirKegiatan();
					jenis.setNama(NAMA_KAJIAN[i]);
					jenis.setKeterangan(KETERANGAN_KAJIAN[i]);
					jenis.setAktif(true);
					Common.refreshSaveOrUpdate(session, jenis);
				}

				FormulirKegiatan kegiatan = (FormulirKegiatan) session.createCriteria(FormulirKegiatan.class)
						.add(Restrictions.eq("jenisFormulirKegiatan", jenis)).uniqueResult();
				if (kegiatan == null) {
					kegiatan = new FormulirKegiatan();
					kegiatan.setNama(NAMA_KAJIAN[i]);
					kegiatan.setJenisFormulirKegiatan(jenis);
					kegiatan.setKeterangan(KETERANGAN_KAJIAN[i]);
					Common.refreshSaveOrUpdate(session, kegiatan);
				}

				KonfigurasiPoinKegiatan konfigurasi = (KonfigurasiPoinKegiatan) session
						.createCriteria(KonfigurasiPoinKegiatan.class)
						.add(Restrictions.eq("formulirKegiatan", kegiatan)).uniqueResult();
				if (konfigurasi == null) {
					konfigurasi = new KonfigurasiPoinKegiatan();
					konfigurasi.setFormulirKegiatan(kegiatan);
					konfigurasi.setPoinOffline(POIN_OFFLINE_DEFAULT[i]);
					konfigurasi.setPoinOnline(POIN_ONLINE_DEFAULT[i]);
					konfigurasi.setDurasiBerlakuHari(DURASI_BERLAKU_HARI_DEFAULT);
					konfigurasi.setAktif(true);
					Common.refreshSaveOrUpdate(session, konfigurasi);
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}
	}

	class KonfigurasiPoinKegiatanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row row, Object obj) throws Exception {
			row.setValign("top");
			final KonfigurasiPoinKegiatan konfigurasi = (KonfigurasiPoinKegiatan) obj;
			final FormulirKegiatan kegiatan = konfigurasi.getFormulirKegiatan();

			new MyLabelConfig(kegiatan == null ? "" : kegiatan.getNama()).setParent(row);

			final Textbox txtOffline = new Textbox(formatBilangan(konfigurasi.getPoinOffline()));
			txtOffline.setWidth("90%");
			txtOffline.setDisabled(!edit);
			txtOffline.setParent(row);

			final Textbox txtOnline = new Textbox(formatBilangan(konfigurasi.getPoinOnline()));
			txtOnline.setWidth("90%");
			txtOnline.setDisabled(!edit);
			txtOnline.setParent(row);

			final Textbox txtDurasi = new Textbox(konfigurasi.getDurasiBerlakuHari() == null ? ""
					: String.valueOf(konfigurasi.getDurasiBerlakuHari()));
			txtDurasi.setWidth("90%");
			txtDurasi.setDisabled(!edit);
			txtDurasi.setParent(row);

			final MyCheckboxConfig chkAktif = new MyCheckboxConfig("Aktif");
			chkAktif.setChecked(konfigurasi.getAktif());
			chkAktif.setDisabled(!edit);
			chkAktif.setParent(row);

			MyToolbarbuttonConfig simpan = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
			simpan.setVisible(edit);
			simpan.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					simpanBaris(konfigurasi.getId(), txtOffline, txtOnline, txtDurasi, chkAktif);
				}
			});
			simpan.setParent(row);
		}
	}

	private String formatBilangan(Double nilai) {
		return nilai == null ? "0" : String.valueOf(nilai.longValue());
	}

	private double parseAngka(String s) {
		if (s == null) {
			return 0;
		}
		String bersih = s.replaceAll("[^0-9.\\-]", "");
		if (bersih.trim().isEmpty()) {
			return 0;
		}
		try {
			return Double.parseDouble(bersih);
		} catch (Exception e) {
			return 0;
		}
	}

	private void simpanBaris(Long id, Textbox txtOffline, Textbox txtOnline, Textbox txtDurasi,
			Checkbox chkAktif) {
		try {
			Session session = HibernateUtil.currentSession();
			KonfigurasiPoinKegiatan k = (KonfigurasiPoinKegiatan) session.load(KonfigurasiPoinKegiatan.class, id);

			double durasi = parseAngka(txtDurasi.getValue());
			k.setPoinOffline(parseAngka(txtOffline.getValue()));
			k.setPoinOnline(parseAngka(txtOnline.getValue()));
			k.setDurasiBerlakuHari(txtDurasi.getValue() == null || txtDurasi.getValue().trim().isEmpty() ? null
					: Integer.valueOf((int) durasi));
			k.setAktif(chkAktif.isChecked());
			Common.refreshSaveOrUpdate(session, k);

			MyMessageboxConfig.show("Nilai poin berhasil disimpan.", "Info", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			try {
				MyMessageboxConfig.show("Gagal menyimpan, keterangan: " + e.getMessage(), "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/koperasi/KonfigurasiPoinKegiatanAction.java"); }
		}
	}

	@SuppressWarnings("unchecked")
	private void muatData() {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KonfigurasiPoinKegiatan.class)
				.createAlias("formulirKegiatan", "fk").addOrder(Order.asc("fk.nama"));
		List<KonfigurasiPoinKegiatan> daftar = criteria.list();
		ListModel model = new SimpleListModel(daftar);
		grid.setRowRenderer(new KonfigurasiPoinKegiatanRenderer());
		grid.setModelCheckMobile(model);
	}

}
