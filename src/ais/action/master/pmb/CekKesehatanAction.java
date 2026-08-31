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
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;

import ais.action.report.Report;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.database.dao.CekKesehatanDao;
import ais.database.dao.DaoFactory;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.CekKesehatan;
import ais.database.model.Mahasiswa;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk cek kesehatan. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Bandbox bandboxCalonMahasiswa}, {@code
 * BiodataCalonMahasiswa calonMahasiswa}, {@code Mahasiswa mahasiswa}, {@code BiodataMahasiswa biodataMahasiswa},
 * {@code String nim}, {@code Center center}, {@code Textbox penyakit1}, {@code Textbox penyakit2};
 * inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}); validasi/perhitungan ({@code
 * onSaveCekKesehatan()}); pelaporan/ekspor ({@code onCetak()}); operasi domain lain ({@code onPsikotes()},
 * {@code onPilihCalonMahasiswa()}, {@code generateNoUrut()}). Bagian lain dari kontrak tetap mengikuti kelas
 * induk atau interface yang disebut di atas.</p>
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
public class CekKesehatanAction extends GenericAutowireComposer {

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
	private Textbox penyakit1;
	private Textbox penyakit2;
	private Textbox penyakit3;
	private Textbox penyakit4;
	private Textbox penyakit5;
	private Textbox rontgen1;
	private Textbox rontgen2;
	private Textbox rontgen3;
	private Textbox sehatTerbatas1;
	private Textbox sehatTerbatas2;
	private Textbox sehatTerbatas3;
	private Textbox sehatTerbatas4;

	private Textbox tekananDarah;
	private Textbox butaWarna;
	private Textbox narkoba;
	private Combobox status_sehat;
	private Textbox urut;
	// private Toolbar toolbar;
	//
	// private CekKesehatan cekKesehatan;

	private MyButtonConfig buttonSimpan;
	private MyButtonConfig buttonSimpanDanCetak;

	private Tabpanel chekPsikotes;

	public void onPsikotes(Event event) {
		if (chekPsikotes.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(chekPsikotes);
			MyInclude iframe = new MyInclude("/pages/master/cek_psikotest.zul");
			iframe.setParent(window);
		}
	}

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

		status_sehat = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		if (comboitem != null) { comboitem.setLabel(CekKesehatan.Sehat); }
		if (comboitem != null) { comboitem.setValue(CekKesehatan.Sehat); }
		status_sehat.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(CekKesehatan.SehatTerbatas); }
		if (comboitem != null) { comboitem.setValue(CekKesehatan.SehatTerbatas); }
		status_sehat.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(CekKesehatan.Sakit); }
		if (comboitem != null) { comboitem.setValue(CekKesehatan.Sakit); }
		status_sehat.appendChild(comboitem);

		buttonSimpan.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub
				onSaveCekKesehatan();
			}
		});

		buttonSimpanDanCetak.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub
				CekKesehatanAction.onCetak(calonMahasiswa);
			}
		});

		if (buttonSimpan != null) { buttonSimpan.setDisabled(true); }
		if (buttonSimpanDanCetak != null) { buttonSimpanDanCetak.setDisabled(true); }

		String[] contents = new String[] { "id", "biodataCalonMahasiswa", "biodataCalonMahasiswa.prodiLulus", "sehat",
				"penyakit1", "penyakit2", "penyakit3", "penyakit4", "penyakit5", "tekananDarah", "butaWarna",
				"rontgen1", "rontgen2", "rontgen3", "narkoba", "sehatTerbatas1", "sehatTerbatas2", "sehatTerbatas3",
				"sehatTerbatas4", "noUrut", "biodataCalonMahasiswa.prodi1", "biodataCalonMahasiswa.prodi2",
				"biodataCalonMahasiswa.prodi3", "biodataCalonMahasiswa.prodi4", "biodataCalonMahasiswa.prodi5" };

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(new DataCriteria() {

			@Override
			public Criteria initCriteria(boolean order) {
				String tahunAkademikPenerimaanMahasiswaBaru = Common
						.getKonfigurasi("tahunAkademikPenerimaanMahasiswaBaru", Common.getCurrentTahunAkademik())
						.getNilai();

				return HibernateUtil.currentSession().createCriteria(CekKesehatan.class)
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
		}, CekKesehatan.class, contents);
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
		CekKesehatan cekKesehatan = (CekKesehatan) session.createCriteria(CekKesehatan.class)
				.add(Restrictions.eq("biodataCalonMahasiswa", calonMahasiswa)).uniqueResult();
		CekKesehatanDao cekKesehatanDao = DaoFactory.getInstance().getCekKesehatanDao();
		if (cekKesehatan != null) {

			cekKesehatan = cekKesehatanDao.load(cekKesehatan.getId());
		}

		else {
			cekKesehatan = new CekKesehatan();
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
				cekKesehatan.getNoUrut() == null ? generateNoUrut() : cekKesehatan.getNoUrut() + ""));
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Kesehatan"));
		row.appendChild(status_sehat);
		Common.selectComboItem(status_sehat, cekKesehatan.getSehat() == null ? null : cekKesehatan.getSehat());
		row.setParent(rows);

		row = new Row();
		row.appendChild(new ais.ui.util.MyLabelConfig("Penyakit 1"));
		row.appendChild(
				penyakit1 = new Textbox(cekKesehatan.getPenyakit1() == null ? "" : cekKesehatan.getPenyakit1()));
		row.setParent(rows);
		penyakit1.setWidth("90%");

		row = new Row();
		row.appendChild(new ais.ui.util.MyLabelConfig("Penyakit 2"));
		row.appendChild(
				penyakit2 = new Textbox(cekKesehatan.getPenyakit2() == null ? "" : cekKesehatan.getPenyakit2()));
		row.setParent(rows);
		penyakit2.setWidth("90%");

		row = new Row();
		row.appendChild(new ais.ui.util.MyLabelConfig("Penyakit 3"));
		row.appendChild(
				penyakit3 = new Textbox(cekKesehatan.getPenyakit3() == null ? "" : cekKesehatan.getPenyakit3()));
		row.setParent(rows);
		penyakit3.setWidth("90%");

		row = new Row();
		row.appendChild(new ais.ui.util.MyLabelConfig("Penyakit 4"));
		row.appendChild(
				penyakit4 = new Textbox(cekKesehatan.getPenyakit4() == null ? "" : cekKesehatan.getPenyakit4()));
		row.setParent(rows);
		penyakit4.setWidth("90%");

		row = new Row();
		row.appendChild(new ais.ui.util.MyLabelConfig("Penyakit 5"));
		row.appendChild(
				penyakit5 = new Textbox(cekKesehatan.getPenyakit5() == null ? "" : cekKesehatan.getPenyakit5()));
		row.setParent(rows);
		penyakit5.setWidth("90%");

		row = new Row();
		row.appendChild(new ais.ui.util.MyLabelConfig("Tekanan Darah"));
		row.appendChild(tekananDarah = new Textbox(
				cekKesehatan.getTekananDarah() == null ? "" : cekKesehatan.getTekananDarah()));
		row.setParent(rows);
		tekananDarah.setWidth("90%");

		row = new Row();
		row.appendChild(new ais.ui.util.MyLabelConfig("Buta Warna"));
		row.appendChild(
				butaWarna = new Textbox(cekKesehatan.getButaWarna() == null ? "" : cekKesehatan.getButaWarna()));
		row.setParent(rows);
		butaWarna.setWidth("90%");

		row = new Row();
		row.appendChild(new ais.ui.util.MyLabelConfig("Narkoba"));
		row.appendChild(narkoba = new Textbox(cekKesehatan.getNarkoba() == null ? "" : cekKesehatan.getNarkoba()));
		row.setParent(rows);
		narkoba.setWidth("90%");

		row = new Row();
		row.appendChild(new ais.ui.util.MyLabelConfig("Rontgen 1"));
		row.appendChild(rontgen1 = new Textbox(cekKesehatan.getRontgen1() == null ? "" : cekKesehatan.getRontgen1()));
		row.setParent(rows);
		rontgen1.setWidth("90%");

		row = new Row();
		row.appendChild(new ais.ui.util.MyLabelConfig("Rontgen 2"));
		row.appendChild(rontgen2 = new Textbox(cekKesehatan.getRontgen2() == null ? "" : cekKesehatan.getRontgen2()));
		row.setParent(rows);
		rontgen2.setWidth("90%");

		row = new Row();
		row.appendChild(new ais.ui.util.MyLabelConfig("Rontgen 3"));
		row.appendChild(rontgen3 = new Textbox(cekKesehatan.getRontgen3() == null ? "" : cekKesehatan.getRontgen3()));
		row.setParent(rows);
		rontgen3.setWidth("90%");

		row = new Row();
		row.appendChild(new ais.ui.util.MyLabelConfig("Sehat Terbatas 1"));
		row.appendChild(sehatTerbatas1 = new Textbox(
				cekKesehatan.getSehatTerbatas1() == null ? "" : cekKesehatan.getSehatTerbatas1()));
		row.setParent(rows);
		sehatTerbatas1.setWidth("90%");

		row = new Row();
		row.appendChild(new ais.ui.util.MyLabelConfig("Sehat Terbatas 2"));
		row.appendChild(sehatTerbatas2 = new Textbox(
				cekKesehatan.getSehatTerbatas2() == null ? "" : cekKesehatan.getSehatTerbatas2()));
		row.setParent(rows);
		sehatTerbatas2.setWidth("90%");

		row = new Row();
		row.appendChild(new ais.ui.util.MyLabelConfig("Sehat Terbatas 3"));
		row.appendChild(sehatTerbatas3 = new Textbox(
				cekKesehatan.getSehatTerbatas3() == null ? "" : cekKesehatan.getSehatTerbatas3()));
		row.setParent(rows);
		sehatTerbatas3.setWidth("90%");

		row = new Row();
		row.appendChild(new ais.ui.util.MyLabelConfig("Sehat Terbatas 4"));
		row.appendChild(sehatTerbatas4 = new Textbox(
				cekKesehatan.getSehatTerbatas4() == null ? "" : cekKesehatan.getSehatTerbatas4()));
		row.setParent(rows);
		sehatTerbatas4.setWidth("90%");

	}

	private void onSaveCekKesehatan() throws Exception {
		Session session = HibernateUtil.currentSession();

		if (status_sehat.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, status kesehatan belum dipilih. Langkah yang dapat dilakukan: (1) pilih status kesehatan dari daftar yang tersedia; (2) pastikan pilihan telah tersorot sebelum menyimpan; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.");
			return;
		}

		CekKesehatan cekKesehatan = (CekKesehatan) session.createCriteria(CekKesehatan.class)
				.add(Restrictions.eq("biodataCalonMahasiswa", calonMahasiswa)).uniqueResult();

		if (cekKesehatan != null) {
			CekKesehatanDao cekKesehatanDao = DaoFactory.getInstance().getCekKesehatanDao();
			cekKesehatan = cekKesehatanDao.load(cekKesehatan.getId());
		} else {
			cekKesehatan = new CekKesehatan();
		}

		cekKesehatan.setBiodataCalonMahasiswa(calonMahasiswa);
		cekKesehatan.setPenyakit1(penyakit1.getValue());
		cekKesehatan.setPenyakit2(penyakit2.getValue());
		cekKesehatan.setPenyakit3(penyakit3.getValue());
		cekKesehatan.setPenyakit4(penyakit4.getValue());
		cekKesehatan.setPenyakit5(penyakit5.getValue());
		cekKesehatan.setRontgen1(rontgen1.getValue());
		cekKesehatan.setRontgen2(rontgen2.getValue());
		cekKesehatan.setRontgen3(rontgen3.getValue());
		cekKesehatan.setSehatTerbatas1(sehatTerbatas1.getValue());
		cekKesehatan.setSehatTerbatas2(sehatTerbatas2.getValue());
		cekKesehatan.setSehatTerbatas3(sehatTerbatas3.getValue());
		cekKesehatan.setSehatTerbatas4(sehatTerbatas4.getValue());
		cekKesehatan.setTekananDarah(tekananDarah.getValue());
		cekKesehatan.setButaWarna(butaWarna.getValue());
		cekKesehatan.setNarkoba(narkoba.getValue());
		cekKesehatan.setSehat(
				(String) (status_sehat.getSelectedItem() == null ? "" : status_sehat.getSelectedItem().getValue()));
		cekKesehatan.setNoUrut(urut.getValue());

		CekKesehatanDao cekKesehatanDao = DaoFactory.getInstance().getCekKesehatanDao();

		if (cekKesehatan.getId() != null) {
			cekKesehatanDao.update(cekKesehatan);
			MyMessageboxConfig.show("Calon mahasiswa dengan nomor ujian " + calonMahasiswa.getNoUjian() + " atas nama "
					+ calonMahasiswa.getNama() + " dinyatakan " + cekKesehatan.getSehat());
			return;
		} else {
			cekKesehatanDao.save(cekKesehatan);
			MyMessageboxConfig.show("Calon mahasiswa dengan nomor ujian " + calonMahasiswa.getNoUjian() + " atas nama "
					+ calonMahasiswa.getNama() + " dinyatakan " + cekKesehatan.getSehat());
			return;
		}

	}

	@SuppressWarnings({ "rawtypes" })
	public static void onCetak(BiodataCalonMahasiswa calonMahasiswa) throws Exception {

		final Map parameters = ais.common.HashMapGenerator.getRand();
		Session session = HibernateUtil.currentSession();
		CekKesehatan cekKesehatan = (CekKesehatan) session.createCriteria(CekKesehatan.class)
				.add(Restrictions.eq("biodataCalonMahasiswa", calonMahasiswa)).uniqueResult();

		if (cekKesehatan == null) {
			cekKesehatan = new CekKesehatan(calonMahasiswa);
			CekKesehatanDao cekKesehatanDao = DaoFactory.getInstance().getCekKesehatanDao();
			cekKesehatanDao.save(cekKesehatan);
		}

		calonMahasiswa.putPhoto(parameters);

		Report.generatePDFReport(Report.PDF, parameters, "Cek_Kesehatan", ais.ui.util.WaktuUtil.getDate());

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
		Number num = (Number) session1.createCriteria(CekKesehatan.class).setProjection(Projections.rowCount())
				.add(Restrictions.ilike("noUrut", date, MatchMode.START)).uniqueResult();
		// if (num == null) {
		// num = 0;
		// }

		HibernateUtil.closeSession();
		no_urut = date + ((num == null ? 0 : num.longValue()) + 1L) + "";
		return no_urut;

	}
}
