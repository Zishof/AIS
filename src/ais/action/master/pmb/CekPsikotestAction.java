package ais.action.master.pmb;

import java.util.Calendar;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Bandbox;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import ais.action.report.Report;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.CekPsikotest;
import ais.database.model.Mahasiswa;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Controller/action ZK untuk cek psikotest. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Bandbox bandboxCalonMahasiswa}, {@code
 * BiodataCalonMahasiswa calonMahasiswa}, {@code Mahasiswa mahasiswa}, {@code BiodataMahasiswa biodataMahasiswa},
 * {@code String nim}, {@code Center center}, {@code Textbox status_sehat}, {@code Textbox urut};
 * inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}); validasi/perhitungan ({@code
 * onSaveCekPsikotest()}); pelaporan/ekspor ({@code onCetak()}); operasi domain lain ({@code
 * onPilihCalonMahasiswa()}, {@code generateNoUrut()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau
 * interface yang disebut di atas.</p>
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
public class CekPsikotestAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6878830378969012479L;
	Bandbox bandboxCalonMahasiswa;
	BiodataCalonMahasiswa calonMahasiswa;
	Mahasiswa mahasiswa;
	BiodataMahasiswa biodataMahasiswa;
	String nim;
	// private MyWindow generateNim;
	// private MyGrid grids;
	private Center center;
	// private South south;

	private Textbox status_sehat;
	private Textbox urut;
	// private Toolbar toolbar;
	//
	// private CekPsikotest cekPsikotest;

	private MyButtonConfig buttonSimpan;
	private MyButtonConfig buttonSimpanDanCetak;

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
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		buttonSimpan.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub
				onSaveCekPsikotest();
			}
		});

		buttonSimpanDanCetak.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub
				CekPsikotestAction.onCetak(calonMahasiswa);
			}
		});

		if (buttonSimpan != null) { buttonSimpan.setDisabled(true); }
		if (buttonSimpanDanCetak != null) { buttonSimpanDanCetak.setDisabled(true); }

		String[] contents = new String[] { "id", "biodataCalonMahasiswa", "sehat", "noUrut" };

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(new DataCriteria() {

			@Override
			public Criteria initCriteria(boolean order) {
				String tahunAkademikPenerimaanMahasiswaBaru = Common
						.getKonfigurasi("tahunAkademikPenerimaanMahasiswaBaru", Common.getCurrentTahunAkademik())
						.getNilai();

				return HibernateUtil.currentSession().createCriteria(CekPsikotest.class)
						.createAlias("biodataCalonMahasiswa", "biodataCalonMahasiswa")
						.add(Restrictions.eq("biodataCalonMahasiswa.tahunAkademik",
								tahunAkademikPenerimaanMahasiswaBaru))
						.addOrder(Order.asc("biodataCalonMahasiswa.noRegistrasi"));
			}
		}, contents);
		Common.appendKeToolbar(cetakToolbarbutton, buttonSimpan, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(new DataSearchDefault() {

			@Override
			public void onSearchDefault(Event event) {

			}
		}, CekPsikotest.class, contents);
		Common.appendKeToolbar(upload, buttonSimpan, comp);
	}

	@SuppressWarnings("deprecation")
	public void onPilihCalonMahasiswa() throws Exception {

		if (bandboxCalonMahasiswa.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Calon Mahasiswa belum dipilih. Langkah yang dapat dilakukan: (1) klik pada kolom pencarian Calon Mahasiswa; (2) ketik nama atau nomor ujian calon mahasiswa; (3) pilih dari daftar yang muncul, lalu ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		}

		buttonSimpan.setDisabled(false);
		buttonSimpanDanCetak.setDisabled(false);

		calonMahasiswa = (BiodataCalonMahasiswa) bandboxCalonMahasiswa.getAttribute("calonMahasiswa");

		Session session = HibernateUtil.currentSession();
		CekPsikotest cekPsikotest = (CekPsikotest) session.createCriteria(CekPsikotest.class)
				.add(Restrictions.eq("biodataCalonMahasiswa", calonMahasiswa)).uniqueResult();

		if (cekPsikotest != null) {

			cekPsikotest = (CekPsikotest) session.load(CekPsikotest.class, cekPsikotest.getId());
		}

		else {
			cekPsikotest = new CekPsikotest();
		}

		Common.clear(center);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid grids = new MyGrid();
		grids.setSclass("fgrid");
		grids.setMold("paging");
		grids.setPageSize(25);
		grids.setParent(center);

		Columns columns = new Columns();
		columns.setParent(grids);

		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("20%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setWidth("90%");
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grids);

		Row row = new Row();row.setValign("top");
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		CommonMedia.tampilkanGambarKecil(calonMahasiswa).setParent(row);

		Common.clear(urut);

		row = new Row();
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor Urut"));
		row.appendChild(urut = new Textbox(
				cekPsikotest.getNoUrut() == null ? generateNoUrut() : cekPsikotest.getNoUrut() + ""));
		urut.setDisabled(true);
		row.setParent(rows);

		row = new Row();
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(new ais.ui.util.MyLabelConfig(calonMahasiswa.getNama().toUpperCase()));
		row.setParent(rows);

		row = new Row();
		row.appendChild(new ais.ui.util.MyLabelConfig("No. Peserta"));
		row.appendChild(
				new ais.ui.util.MyLabelConfig(calonMahasiswa.getNoUjian() == null ? "" : calonMahasiswa.getNoUjian()));
		row.setParent(rows);

		row = new Row();
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(new ais.ui.util.MyLabelConfig(calonMahasiswa.getMundur() ? "Mengundurkan diri"
				: calonMahasiswa.getDitolak() ? "Tidak diterima (ditolak)"
						: calonMahasiswa.getProdiLulus() == null ? "Belum Diterima"
								: calonMahasiswa.getProdiLulus().getFakultas().getNama()));
		row.setParent(rows);

		row = new Row();
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(new ais.ui.util.MyLabelConfig(calonMahasiswa.getMundur() ? "Mengundurkan diri"
				: calonMahasiswa.getDitolak() ? "Tidak diterima (ditolak)"
						: calonMahasiswa.getProdiLulus() == null ? "Belum Diterima"
								: calonMahasiswa.getProdiLulus().getNama()));
		row.setParent(rows);

		row = new Row();
		row.appendChild(new ais.ui.util.MyLabelConfig("Jalur Masuk"));
		row.appendChild(
				new Label(calonMahasiswa.getJenisSeleksi() == null ? "" : calonMahasiswa.getJenisSeleksi().getNama()));
		row.setParent(rows);

		row = new Row();
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Kelamin"));
		row.appendChild(new ais.ui.util.MyLabelConfig(
				calonMahasiswa.getJenisKelamin() == null ? "" : calonMahasiswa.getJenisKelamin()));
		row.setParent(rows);

		row = new Row();
		row.appendChild(new ais.ui.util.MyLabelConfig("Tempat / Tanggal Lahir"));
		row.appendChild(new ais.ui.util.MyLabelConfig(calonMahasiswa.getTempatLahir() == null ? ""
				: calonMahasiswa.getTempatLahir() + " / " + calonMahasiswa.getTanggalLahir()));
		row.setParent(rows);

		row = new Row();
		row.appendChild(new ais.ui.util.MyLabelConfig("Hasil Psikotest"));
		row.appendChild(status_sehat = new Textbox(cekPsikotest.getSehat()));
		status_sehat.setRows(10);
		status_sehat.setWidth("90%");
		row.setParent(rows);

	}

	private void onSaveCekPsikotest() throws Exception {
		Session session = HibernateUtil.currentSession();

		if (status_sehat.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show("Mohon maaf, hasil psikotes belum diisi. Langkah yang dapat dilakukan: (1) isi kolom hasil psikotes dengan nilai atau keterangan yang sesuai; (2) pastikan kolom tidak kosong sebelum menyimpan; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		}

		CekPsikotest cekPsikotest = (CekPsikotest) session.createCriteria(CekPsikotest.class)
				.add(Restrictions.eq("biodataCalonMahasiswa", calonMahasiswa)).uniqueResult();

		if (cekPsikotest == null) {
			cekPsikotest = new CekPsikotest();
		}

		cekPsikotest.setBiodataCalonMahasiswa(calonMahasiswa);

		cekPsikotest.setSehat(status_sehat.getValue());
		cekPsikotest.setNoUrut(urut.getValue());

		if (cekPsikotest.getId() != null) {
			Common.refreshUpdate(session, cekPsikotest);
			MyMessageboxConfig.show("Calon mahasiswa dengan nomor ujian " + calonMahasiswa.getNoUjian() + " atas nama "
					+ calonMahasiswa.getNama() + " dinyatakan " + cekPsikotest.getSehat());
			return;
		} else {
			session.save(cekPsikotest);
			MyMessageboxConfig.show("Calon mahasiswa dengan nomor ujian " + calonMahasiswa.getNoUjian() + " atas nama "
					+ calonMahasiswa.getNama() + " dinyatakan " + cekPsikotest.getSehat());
			return;
		}

	}

	@SuppressWarnings({ "rawtypes" })
	public static void onCetak(BiodataCalonMahasiswa calonMahasiswa) throws Exception {

		final Map parameters = ais.common.HashMapGenerator.getRand();
		Session session = HibernateUtil.currentSession();
		CekPsikotest cekPsikotest = (CekPsikotest) session.createCriteria(CekPsikotest.class)
				.add(Restrictions.eq("biodataCalonMahasiswa", calonMahasiswa)).uniqueResult();

		if (cekPsikotest == null) {
			cekPsikotest = new CekPsikotest(calonMahasiswa);
			session.save(cekPsikotest);
		}

		calonMahasiswa.putPhoto(parameters);

		Report.generatePDFReport(Report.PDF, parameters, "Cek_Psikotes", ais.ui.util.WaktuUtil.getDate());

	}

	private String generateNoUrut() {
		String no_urut = "";
		Integer tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		Integer bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH);
		Integer tanggal = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.DATE);
		bulan += 1;
		String date = tahun + "" + bulan + "" + tanggal + "";
		// String date = "";
		System.out.println(tahun);
		System.out.println(bulan + 1);
		System.out.println(tanggal);
		Session session1 = HibernateUtil.currentNativeSession();
		Number num = (Number) session1.createCriteria(CekPsikotest.class).setProjection(Projections.rowCount())
				.add(Restrictions.ilike("noUrut", date, MatchMode.START)).uniqueResult();
		// if (num == null) {
		// num = 0;
		// }

		HibernateUtil.closeSession();
		no_urut = date + ((num == null ? 0 : num.longValue()) + 1L) + "";
		return no_urut;

	}
}
