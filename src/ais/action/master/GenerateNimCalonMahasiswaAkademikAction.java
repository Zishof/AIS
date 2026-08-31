package ais.action.master;

import java.io.Serializable;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Bandbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;

import ais.action.master.pmb.nim.NimGenerator;
import ais.action.report.Report;
import org.zkoss.zul.Messagebox;
import ais.common.Common;
import ais.common.CommonPMB;
import ais.common.CommonPrivilages;
import ais.database.dao.CekKesehatanDao;
import ais.database.dao.DaoFactory;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.CekKesehatan;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;

/**
 * Controller/action ZK untuk generate nim calon mahasiswa akademik. Tipe ini merupakan titik masuk
 * UI yang menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus
 * oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Bandbox bandboxCalonMahasiswa}, {@code
 * BiodataCalonMahasiswa calonMahasiswa}, {@code Mahasiswa mahasiswa}, {@code BiodataMahasiswa biodataMahasiswa},
 * {@code String nim}, {@code MyGrid grids}, {@code Row rowNoRegistrasi}, {@code Row rowNoUjian};
 * inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}); pembacaan/pencarian ({@code
 * loadDataCalonMahasiswaLulus()}); pelaporan/ekspor ({@code onCetak()}, {@code onCetakKesehatan()}); operasi
 * domain lain ({@code onPilihCalonMahasiswa()}, {@code onGenerateNim()}, {@code onRegenerateNim()}). Bagian lain
 * dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class GenerateNimCalonMahasiswaAkademikAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6878830378969012479L;
	Bandbox bandboxCalonMahasiswa;
	BiodataCalonMahasiswa calonMahasiswa;
	Mahasiswa mahasiswa;
	BiodataMahasiswa biodataMahasiswa;
	String nim;
	private MyGrid grids;

	private Row rowNoRegistrasi;
	private Row rowNoUjian;
	private Row rowNim;
	private Row rowNama;
	private Row rowProdi;
	private Row rowTahunMasuk;
	private Row rowTahunAkademik;
	// private Row rowButton;

	private Label labelNoRegistrasi;
	private Label labelNoUjian;
	private Label labelNim;
	private Label prodi;
	private Label labelNamaMahasiswa;
	private Label labelTahunMasuk;
	private Label labelTahunAkademik;

	private MyButtonConfig buttonGenerateNim;
	private MyButtonConfig buttonRegenerateNim;
	private MyButtonConfig buttonCetak;
	private MyButtonConfig buttonCetakKesehatan;

	private NimGenerator nimGenerator;

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
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		nimGenerator = (NimGenerator) Class.forName(
				Common.getKonfigurasi("class_untuk_generate_nim", "ais.action.master.pmb.nim.DefaultNimGenerator")
						.getNilai().trim())
				.newInstance();

		if (buttonGenerateNim != null)   { buttonGenerateNim.setDisabled(true); }
		if (buttonRegenerateNim != null) { buttonRegenerateNim.setVisible(false); }

	}

	public void onPilihCalonMahasiswa() throws Exception {

		if (bandboxCalonMahasiswa.getValue().trim().equals("")) {
			MyMessageboxConfig.show(
					"Mohon Bapak/Ibu terlebih dahulu memilih calon mahasiswa yang akan diproses. Langkah yang dapat dilakukan: (1) klik kolom pencarian calon mahasiswa; (2) pilih nama calon mahasiswa yang dituju; (3) lanjutkan kembali proses ini.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		}
		buttonCetak.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub
				onCetak(mahasiswa);
			}
		});

		buttonCetakKesehatan.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub
				onCetakKesehatan();
			}
		});

		calonMahasiswa = (BiodataCalonMahasiswa) bandboxCalonMahasiswa.getAttribute("calonMahasiswa");

		mahasiswa = (Mahasiswa) HibernateUtil.currentSession().createCriteria(Mahasiswa.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("id"))
				.add(Restrictions.eq("nama", calonMahasiswa.getNama()))
				.add(Restrictions.eq("jurusan", calonMahasiswa.getProdiLulus()))
				.add(Restrictions.eq("tanggallahir", calonMahasiswa.getTanggalLahir()))
				.createCriteria("statusAwalMahasiswa").add(Restrictions.eq("id", new Long(2))).setMaxResults(1)
				.uniqueResult();

		Integer bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH);

		rowNoRegistrasi.setVisible(true);
		labelNoRegistrasi.setValue(calonMahasiswa.getNoRegistrasi());
		rowNoUjian.setVisible(true);
		labelNoUjian.setValue(calonMahasiswa.getNoUjian());
		rowNim.setVisible(true);
		// labelNim.setValue(mahasiswa == null ? " - "
		// : mahasiswa.getNim() == null ? " - " : mahasiswa.getNim());
		labelNim.setValue(calonMahasiswa.getNim() == null ? "-" : calonMahasiswa.getNim());
		rowNama.setVisible(true);
		labelNamaMahasiswa.setValue(calonMahasiswa.getNama());
		rowProdi.setVisible(true);
		prodi.setValue(calonMahasiswa.getProdiLulus() == null ? "" : calonMahasiswa.getProdiLulus().getNama());
		rowTahunMasuk.setVisible(true);
		// labelTahunMasuk.setValue(String.valueOf(ais.ui.util.WaktuUtil.getCalendar().get(
		// Calendar.YEAR)));
		labelTahunMasuk.setValue(calonMahasiswa.getTahun() == null
				? String.valueOf(ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR))
				: calonMahasiswa.getTahun().toString());
		rowTahunAkademik.setVisible(true);
		if (bulan > 2) {
			labelTahunAkademik.setValue(String.valueOf(ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR)) + "/"
					+ String.valueOf(ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) + 1));
		} else {
			labelTahunAkademik.setValue(String.valueOf(ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) - 1) + "/"
					+ String.valueOf(ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR)));
		}
		// rowButton.setVisible(true);
		String nim = "";
		if (mahasiswa != null) {
			if (mahasiswa.getNim() != null) {
				nim = mahasiswa.getNim();
			}
		}

		if (labelNim.getValue().trim().isEmpty() || labelNim.getValue().equals(nim))
			buttonGenerateNim.setDisabled(true);
		else
			buttonGenerateNim.setDisabled(false);

		// Tampilkan tombol Regenerate NIM jika calon mahasiswa sudah punya NIM
		boolean sudahAdaNim = calonMahasiswa.getNim() != null && !calonMahasiswa.getNim().trim().isEmpty();
		if (buttonRegenerateNim != null) buttonRegenerateNim.setVisible(sudahAdaNim);

	}

	public void onGenerateNim() throws Exception {
		calonMahasiswa = (BiodataCalonMahasiswa) bandboxCalonMahasiswa.getAttribute("calonMahasiswa");

		if (calonMahasiswa.getProdiLulus() == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, calon mahasiswa ini belum dinyatakan lulus sehingga NIM belum dapat digenerate. Langkah yang dapat dilakukan: (1) pastikan status kelulusan calon mahasiswa telah ditetapkan pada menu kelulusan; (2) tetapkan program studi kelulusan (prodi lulus) terlebih dahulu; (3) ulangi kembali proses generate NIM.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}

		else if (Common.bolehKonfigurasi("calon_mahasiswa_wajib_melakukan_pembayaran_daftar_ulang_mahasiswa_baru")
				&& !CommonPMB.isTagihanDaftarUlangNol(calonMahasiswa.getPembayaranDaftarUlang())
				&& (calonMahasiswa.getPembayaranDaftarUlang() == null
						|| calonMahasiswa.getPembayaranDaftarUlang().getPersentaseLunas() == null
						|| calonMahasiswa.getPembayaranDaftarUlang().getPersentaseLunas() < 0.01)) {
			MyMessageboxConfig.show(
					"Mohon maaf, calon mahasiswa ini belum menyelesaikan pembayaran biaya daftar ulang / biaya perkuliahan sehingga NIM belum dapat digenerate. Langkah yang dapat dilakukan: (1) pastikan calon mahasiswa telah melakukan pembayaran daftar ulang; (2) verifikasi status pembayaran pada menu pembayaran; (3) ulangi kembali proses generate NIM setelah pembayaran terkonfirmasi lunas.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		} else {
			if (calonMahasiswa.getNim() != null && !calonMahasiswa.getNim().trim().isEmpty()) {
				MyMessageboxConfig.showFormat(
						"Mohon maaf, calon mahasiswa yang bersangkutan telah memiliki NIM, yaitu \"{V1}\". Apabila Bapak/Ibu ingin mengganti NIM tersebut, silakan gunakan tombol Regenerate NIM.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, calonMahasiswa.getNim().trim());
				return;
			} else {
				nim = nimGenerator.generateNim(calonMahasiswa);
			}
			boolean izinkanNimDenganTandaHubung = CommonPMB.konfirmasiNimMengandungTandaJikaPerlu(nim,
					calonMahasiswa);
			if (!izinkanNimDenganTandaHubung) {
				return;
			}
			calonMahasiswa.setNim(nim);
			Session session = HibernateUtil.currentSession();
			mahasiswa = CommonPMB.saveMahasiswa(session, calonMahasiswa, nim, false,
					izinkanNimDenganTandaHubung);

			MyMessageboxConfig.showFormat(
					"NIM telah berhasil digenerate untuk calon mahasiswa yang bersangkutan, yaitu \"{V1}\". Silakan lanjutkan ke proses berikutnya.",
					"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, nim);

			buttonGenerateNim.setDisabled(true);
			onCetak(mahasiswa);
			labelNim.setValue(nim);

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					CommonPMB.copyLampiran(calonMahasiswa, mahasiswa);
				}
			});

			return;

		}
	}

	public void onRegenerateNim() throws Exception {
		calonMahasiswa = (BiodataCalonMahasiswa) bandboxCalonMahasiswa.getAttribute("calonMahasiswa");
		if (calonMahasiswa == null) {
			MyMessageboxConfig.show(
					"Mohon Bapak/Ibu terlebih dahulu memilih calon mahasiswa yang akan diproses. Langkah yang dapat dilakukan: (1) klik kolom pencarian calon mahasiswa; (2) pilih nama calon mahasiswa yang dituju; (3) lanjutkan kembali proses regenerate NIM.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}
		if (calonMahasiswa.getProdiLulus() == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, calon mahasiswa ini belum dinyatakan lulus sehingga NIM belum dapat diregenerate. Langkah yang dapat dilakukan: (1) pastikan status kelulusan calon mahasiswa telah ditetapkan pada menu kelulusan; (2) tetapkan program studi kelulusan (prodi lulus) terlebih dahulu; (3) ulangi kembali proses regenerate NIM.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}

		final String nimLama = calonMahasiswa.getNim() != null ? calonMahasiswa.getNim().trim() : "";

		// Generate NIM SEBELUM Messagebox karena DefaultNimGenerator memanggil
		// HibernateUtil.closeSession() secara internal — session baru dibuka di callback.
		final String nimBaru;
		try {
			nimBaru = nimGenerator.generateNim(calonMahasiswa);
		} catch (Exception ex) {
			MyMessageboxConfig.showFormat(
					"Mohon maaf, proses pembuatan NIM baru tidak dapat diselesaikan. Rincian kesalahan: {V1}. Langkah yang dapat dilakukan: (1) pastikan konfigurasi format penomoran NIM sudah benar; (2) periksa kembali kelengkapan data calon mahasiswa; (3) ulangi kembali proses regenerate NIM; (4) apabila masih gagal, mohon hubungi administrator sistem.",
					"Error", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, ex.getMessage());
			return;
		}

		Messagebox.show(
			"NIM saat ini : " + (nimLama.isEmpty() ? "-" : nimLama) + "\n"
			+ "NIM baru     : " + nimBaru + "\n\n"
			+ "Yakin ingin mengganti NIM dengan format terbaru?",
			"Konfirmasi Regenerate NIM",
			Messagebox.YES | Messagebox.NO, Messagebox.QUESTION,
			new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					if (!"onYes".equals(event.getName())) return;
					try {
						Session sess = HibernateUtil.currentSession();

						// Perbarui NIM di BiodataCalonMahasiswa
						calonMahasiswa.setNim(nimBaru);
						sess.update(calonMahasiswa);

						// Perbarui NIM di entitas Mahasiswa (jika sudah ada)
						if (mahasiswa != null && mahasiswa.getId() != null) {
							Mahasiswa mhsUpdate = (Mahasiswa) sess.get(Mahasiswa.class, mahasiswa.getId());
							if (mhsUpdate != null) {
								mhsUpdate.setNim(nimBaru);
								mahasiswa = mhsUpdate;
							}
						} else if (!nimLama.isEmpty()) {
							Mahasiswa mhsUpdate = (Mahasiswa) sess.createCriteria(Mahasiswa.class)
									.add(Restrictions.eq("nim", nimLama)).setMaxResults(1).uniqueResult();
							if (mhsUpdate != null) {
								mhsUpdate.setNim(nimBaru);
								mahasiswa = mhsUpdate;
							}
						}

						labelNim.setValue(nimBaru);
						if (buttonRegenerateNim != null) buttonRegenerateNim.setVisible(true);

						MyMessageboxConfig.showFormat(
							"NIM telah berhasil diperbarui. NIM lama: \"{V1}\". NIM baru: \"{V2}\". Silakan periksa kembali data mahasiswa untuk memastikan perubahan telah tersimpan dengan benar.",
							"Berhasil", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
							(nimLama.isEmpty() ? "-" : nimLama), nimBaru);
					} catch (Exception ex) {
						Common.tampilErrorJikaAdmin(ex);
					}
				}
			}
		);
	}

	@SuppressWarnings("rawtypes")
	public void loadDataCalonMahasiswaLulus(List calonMahasiswaLulus) {

		ListModel strset = new SimpleListModel(calonMahasiswaLulus);
		grids.setRowRenderer(new CalonMahasiswaLulusRenderer());
		grids.setModelCheckMobile(strset);

		grids.renderAll();
		grids.setOddRowSclass("non-odd");

	}

	class CalonMahasiswaLulusRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			BiodataCalonMahasiswa biodataCalonMahasiswa = (BiodataCalonMahasiswa) arg1;

			Session session = HibernateUtil.currentSession();

			Mahasiswa mahasiswa = (Mahasiswa) session.createCriteria(Mahasiswa.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.eq("nama", biodataCalonMahasiswa.getNama()))
					.setMaxResults(1).uniqueResult();

			new Label(mahasiswa == null ? "" : mahasiswa.getNim() == null ? "" : mahasiswa.getNim()).setParent(arg0);
			new Label(biodataCalonMahasiswa.getNama()).setParent(arg0);
			new Label(biodataCalonMahasiswa.getNoUjian()).setParent(arg0);
			new Label(biodataCalonMahasiswa.getProdiLulus().getNama()).setParent(arg0);

		}

	}

	@SuppressWarnings({ "unused" })
	private void onCetak(Mahasiswa mahasiswa) throws Exception {
		Map<String, Serializable> parameters = ais.common.HashMapGenerator.getRandStringSerializable();
		this.mahasiswa = mahasiswa;
		Session session = HibernateUtil.currentSession();

		if (mahasiswa == null || mahasiswa.getNim() == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, NIM untuk mahasiswa ini belum digenerate sehingga dokumen belum dapat dicetak. Silakan lakukan proses Generate NIM terlebih dahulu, kemudian ulangi kembali proses pencetakan.");
			return;

		}

		else {
			System.out.println("cetak dengan nim" + mahasiswa.getNim());
			parameters.put("nim", mahasiswa.getNim());
			calonMahasiswa.putPhoto(parameters);

			Report.generatePDFReport(Report.PDF, parameters, "Biodata_Nim", ais.ui.util.WaktuUtil.getDate());
		}

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void onCetakKesehatan() throws Exception {

		try {
			Map parameters = ais.common.HashMapGenerator.getRand();
			Session session = HibernateUtil.currentSession();
			CekKesehatan cekKesehatan = (CekKesehatan) session.createCriteria(CekKesehatan.class)
					.add(Restrictions.eq("biodataCalonMahasiswa", calonMahasiswa)).uniqueResult();

			if (cekKesehatan == null) {
				cekKesehatan = new CekKesehatan(calonMahasiswa);
				CekKesehatanDao cekKesehatanDao = DaoFactory.getInstance().getCekKesehatanDao();
				cekKesehatanDao.save(cekKesehatan);
			}
			// onSaveCekKesehatan();
			parameters.put("cek_kesehatan", cekKesehatan.getId());

			calonMahasiswa.putPhoto(parameters);

			Report.generatePDFReport(Report.PDF, parameters, "Cek_Kesehatan", ais.ui.util.WaktuUtil.getDate());
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

	}

}
