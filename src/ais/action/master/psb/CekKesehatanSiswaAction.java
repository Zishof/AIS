package ais.action.master.psb;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.image.AImage;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Bandbox;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

import ais.action.report.Report;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.file.FotoCalonSiswa;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.CekKesehatanSiswa;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Controller/action ZK untuk cek kesehatan siswa. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Bandbox bandboxCalonMahasiswa}, {@code
 * CalonSiswa calonMahasiswa}, {@code Mahasiswa mahasiswa}, {@code BiodataMahasiswa biodataMahasiswa}, {@code
 * String nim}, {@code Center center}, {@code Textbox penyakit1}, {@code Textbox penyakit2};
 * inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}); validasi/perhitungan ({@code
 * onSaveCekKesehatanSiswa()}); pelaporan/ekspor ({@code onCetak()}); operasi domain lain ({@code
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
public class CekKesehatanSiswaAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6878830378969012479L;
	Bandbox bandboxCalonMahasiswa;
	CalonSiswa calonMahasiswa;
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
	// private CekKesehatanSiswa cekKesehatanSiswa;

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

		status_sehat = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		if (comboitem != null) { comboitem.setLabel(CekKesehatanSiswa.Sehat); }
		if (comboitem != null) { comboitem.setValue(CekKesehatanSiswa.Sehat); }
		status_sehat.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(CekKesehatanSiswa.SehatTerbatas); }
		if (comboitem != null) { comboitem.setValue(CekKesehatanSiswa.SehatTerbatas); }
		status_sehat.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(CekKesehatanSiswa.Sakit); }
		if (comboitem != null) { comboitem.setValue(CekKesehatanSiswa.Sakit); }
		status_sehat.appendChild(comboitem);

		buttonSimpan.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub
				onSaveCekKesehatanSiswa();
			}
		});

		buttonSimpanDanCetak.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub
				CekKesehatanSiswaAction.onCetak(calonMahasiswa);
			}
		});

		if (buttonSimpan != null) { buttonSimpan.setDisabled(true); }
		if (buttonSimpanDanCetak != null) { buttonSimpanDanCetak.setDisabled(true); }

		String[] contents = new String[] { "id", "calonSiswa", "sehat", "penyakit1", "penyakit2", "penyakit3",
				"penyakit4", "penyakit5", "tekananDarah", "butaWarna", "rontgen1", "rontgen2", "rontgen3", "narkoba",
				"sehatTerbatas1", "sehatTerbatas2", "sehatTerbatas3", "sehatTerbatas4", "noUrut" };

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(new DataCriteria() {

			@Override
			public Criteria initCriteria(boolean order) {
				String tahunAkademikPenerimaanMahasiswaBaru = Common
						.getKonfigurasi("tahunAkademikPenerimaanMahasiswaBaru", Common.getCurrentTahunAkademik())
						.getNilai();

				return HibernateUtil.currentSession().createCriteria(CekKesehatanSiswa.class)
						.createAlias("calonSiswa", "calonSiswa")
						.add(Restrictions.eq("calonSiswa.tahunAkademik", tahunAkademikPenerimaanMahasiswaBaru))
						.addOrder(Order.asc("calonSiswa.noRegistrasi"));
			}
		}, contents);
		Common.appendKeToolbar(cetakToolbarbutton, buttonSimpan, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(new DataSearchDefault() {

			@Override
			public void onSearchDefault(Event event) {

			}
		}, CekKesehatanSiswa.class, contents);
		Common.appendKeToolbar(upload, buttonSimpan, comp);
	}

	public void onPilihCalonMahasiswa() throws Exception {

		if (bandboxCalonMahasiswa.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Calon Mahasiswa belum dipilih. Langkah yang dapat dilakukan: (1) Klik pada kolom pencarian Calon Mahasiswa; (2) Ketik nama atau nomor ujian untuk mencari; (3) Pilih Calon Mahasiswa dari daftar yang muncul, lalu ulangi proses ini.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		}

		buttonSimpan.setDisabled(false);
		buttonSimpanDanCetak.setDisabled(false);

		calonMahasiswa = (CalonSiswa) bandboxCalonMahasiswa.getAttribute("calonMahasiswa");

		Session session = HibernateUtil.currentSession();
		CekKesehatanSiswa cekKesehatanSiswa = (CekKesehatanSiswa) session.createCriteria(CekKesehatanSiswa.class)
				.add(Restrictions.eq("calonSiswa", calonMahasiswa)).uniqueResult();
		if (cekKesehatanSiswa != null) {

			cekKesehatanSiswa = (CekKesehatanSiswa) session.load(CekKesehatanSiswa.class, cekKesehatanSiswa.getId());
		}

		else {
			cekKesehatanSiswa = new CekKesehatanSiswa();
		}

		Common.clear(center);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid grids = new MyGrid();
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

		Row row;
		try {
			Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();

			FotoCalonSiswa calonSiswa = (FotoCalonSiswa) streamingSession.createCriteria(FotoCalonSiswa.class)
					.add(Restrictions.eq("calonSiswa", calonMahasiswa.getId())).setMaxResults(1).uniqueResult();

			row = new MyFormRow();
			row.setParent(rows);
			Vbox vbox = new Vbox();
			vbox.setHeight("100%");
			vbox.setWidth("100%");
			row.appendChild(vbox);
			final org.zkoss.zul.Image myImage = new org.zkoss.zul.Image("/img/administrator-icon_default.png");
			myImage.setWidth("100%");
			// myImage.setHeight("100px");
			vbox.appendChild(myImage);
			if (calonSiswa != null && calonSiswa.getFoto() != null) {
				try {
					AImage aImage = new AImage(calonSiswa.createLinkUri());
					myImage.setContent(aImage);
				} catch (IOException e) {
					Common.tampilErrorJikaAdmin(e);
				} catch (SQLException e) {
					Common.tampilErrorJikaAdmin(e);
				}

			}
		} catch (Exception e) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			Common.tampilErrorJikaAdmin(e);
		}

		Common.clear(urut);

		row = new MyFormRow();
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor Urut"));
		row.appendChild(urut = new Textbox(
				cekKesehatanSiswa.getNoUrut() == null ? generateNoUrut() : cekKesehatanSiswa.getNoUrut() + ""));
		urut.setDisabled(true);
		row.setParent(rows);

		row = new MyFormRow();
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(new ais.ui.util.MyLabelConfig(calonMahasiswa.getNama().toUpperCase()));
		row.setParent(rows);

		row = new MyFormRow();
		row.appendChild(new ais.ui.util.MyLabelConfig("No. Peserta"));
		row.appendChild(
				new ais.ui.util.MyLabelConfig(calonMahasiswa.getNoUjian() == null ? "" : calonMahasiswa.getNoUjian()));
		row.setParent(rows);

		row = new MyFormRow();
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));
		row.appendChild(new ais.ui.util.MyLabelConfig(!calonMahasiswa.getTelahDiterima() ? "Tidak diterima (ditolak)"
				: calonMahasiswa.getSekolah() == null ? "Belum Diterima" : calonMahasiswa.getSekolah().getNama()));
		row.setParent(rows);

		row = new MyFormRow();
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Kelamin"));
		row.appendChild(new ais.ui.util.MyLabelConfig(
				calonMahasiswa.getJenisKelamin() == null ? "" : calonMahasiswa.getJenisKelamin()));
		row.setParent(rows);

		row = new MyFormRow();
		row.appendChild(new ais.ui.util.MyLabelConfig("Tempat / Tanggal Lahir"));
		row.appendChild(new ais.ui.util.MyLabelConfig(calonMahasiswa.getTempatLahir() == null ? ""
				: calonMahasiswa.getTempatLahir() + " / " + calonMahasiswa.getTanggalLahir()));
		row.setParent(rows);

		row = new MyFormRow();
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Kesehatan"));
		row.appendChild(status_sehat);
		Common.selectComboItem(status_sehat,
				cekKesehatanSiswa.getSehat() == null ? null : cekKesehatanSiswa.getSehat());
		row.setParent(rows);

		row = new MyFormRow();
		row.appendChild(new ais.ui.util.MyLabelConfig("Penyakit 1"));
		row.appendChild(penyakit1 = new Textbox(
				cekKesehatanSiswa.getPenyakit1() == null ? "" : cekKesehatanSiswa.getPenyakit1()));
		row.setParent(rows);
		penyakit1.setWidth("90%");

		row = new MyFormRow();
		row.appendChild(new ais.ui.util.MyLabelConfig("Penyakit 2"));
		row.appendChild(penyakit2 = new Textbox(
				cekKesehatanSiswa.getPenyakit2() == null ? "" : cekKesehatanSiswa.getPenyakit2()));
		row.setParent(rows);
		penyakit2.setWidth("90%");

		row = new MyFormRow();
		row.appendChild(new ais.ui.util.MyLabelConfig("Penyakit 3"));
		row.appendChild(penyakit3 = new Textbox(
				cekKesehatanSiswa.getPenyakit3() == null ? "" : cekKesehatanSiswa.getPenyakit3()));
		row.setParent(rows);
		penyakit3.setWidth("90%");

		row = new MyFormRow();
		row.appendChild(new ais.ui.util.MyLabelConfig("Penyakit 4"));
		row.appendChild(penyakit4 = new Textbox(
				cekKesehatanSiswa.getPenyakit4() == null ? "" : cekKesehatanSiswa.getPenyakit4()));
		row.setParent(rows);
		penyakit4.setWidth("90%");

		row = new MyFormRow();
		row.appendChild(new ais.ui.util.MyLabelConfig("Penyakit 5"));
		row.appendChild(penyakit5 = new Textbox(
				cekKesehatanSiswa.getPenyakit5() == null ? "" : cekKesehatanSiswa.getPenyakit5()));
		row.setParent(rows);
		penyakit5.setWidth("90%");

		row = new MyFormRow();
		row.appendChild(new ais.ui.util.MyLabelConfig("Tekanan Darah"));
		row.appendChild(tekananDarah = new Textbox(
				cekKesehatanSiswa.getTekananDarah() == null ? "" : cekKesehatanSiswa.getTekananDarah()));
		row.setParent(rows);
		tekananDarah.setWidth("90%");

		row = new MyFormRow();
		row.appendChild(new ais.ui.util.MyLabelConfig("Buta Warna"));
		row.appendChild(butaWarna = new Textbox(
				cekKesehatanSiswa.getButaWarna() == null ? "" : cekKesehatanSiswa.getButaWarna()));
		row.setParent(rows);
		butaWarna.setWidth("90%");

		row = new MyFormRow();
		row.appendChild(new ais.ui.util.MyLabelConfig("Narkoba"));
		row.appendChild(
				narkoba = new Textbox(cekKesehatanSiswa.getNarkoba() == null ? "" : cekKesehatanSiswa.getNarkoba()));
		row.setParent(rows);
		narkoba.setWidth("90%");

		row = new MyFormRow();
		row.appendChild(new ais.ui.util.MyLabelConfig("Rontgen 1"));
		row.appendChild(
				rontgen1 = new Textbox(cekKesehatanSiswa.getRontgen1() == null ? "" : cekKesehatanSiswa.getRontgen1()));
		row.setParent(rows);
		rontgen1.setWidth("90%");

		row = new MyFormRow();
		row.appendChild(new ais.ui.util.MyLabelConfig("Rontgen 2"));
		row.appendChild(
				rontgen2 = new Textbox(cekKesehatanSiswa.getRontgen2() == null ? "" : cekKesehatanSiswa.getRontgen2()));
		row.setParent(rows);
		rontgen2.setWidth("90%");

		row = new MyFormRow();
		row.appendChild(new ais.ui.util.MyLabelConfig("Rontgen 3"));
		row.appendChild(
				rontgen3 = new Textbox(cekKesehatanSiswa.getRontgen3() == null ? "" : cekKesehatanSiswa.getRontgen3()));
		row.setParent(rows);
		rontgen3.setWidth("90%");

		row = new MyFormRow();
		row.appendChild(new ais.ui.util.MyLabelConfig("Sehat Terbatas 1"));
		row.appendChild(sehatTerbatas1 = new Textbox(
				cekKesehatanSiswa.getSehatTerbatas1() == null ? "" : cekKesehatanSiswa.getSehatTerbatas1()));
		row.setParent(rows);
		sehatTerbatas1.setWidth("90%");

		row = new MyFormRow();
		row.appendChild(new ais.ui.util.MyLabelConfig("Sehat Terbatas 2"));
		row.appendChild(sehatTerbatas2 = new Textbox(
				cekKesehatanSiswa.getSehatTerbatas2() == null ? "" : cekKesehatanSiswa.getSehatTerbatas2()));
		row.setParent(rows);
		sehatTerbatas2.setWidth("90%");

		row = new MyFormRow();
		row.appendChild(new ais.ui.util.MyLabelConfig("Sehat Terbatas 3"));
		row.appendChild(sehatTerbatas3 = new Textbox(
				cekKesehatanSiswa.getSehatTerbatas3() == null ? "" : cekKesehatanSiswa.getSehatTerbatas3()));
		row.setParent(rows);
		sehatTerbatas3.setWidth("90%");

		row = new MyFormRow();
		row.appendChild(new ais.ui.util.MyLabelConfig("Sehat Terbatas 4"));
		row.appendChild(sehatTerbatas4 = new Textbox(
				cekKesehatanSiswa.getSehatTerbatas4() == null ? "" : cekKesehatanSiswa.getSehatTerbatas4()));
		row.setParent(rows);
		sehatTerbatas4.setWidth("90%");

		StreamingHibernateUtil.getInstance().closeSession();
	}

	private void onSaveCekKesehatanSiswa() throws Exception {
		Session session = HibernateUtil.currentSession();

		if (status_sehat.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Status Kesehatan belum dipilih. Langkah yang dapat dilakukan: (1) Klik pada kolom Status Kesehatan; (2) Pilih salah satu status yang sesuai; (3) Ulangi proses penyimpanan. Jika masih mengalami kendala, hubungi Administrator.");
			return;
		}

		CekKesehatanSiswa cekKesehatanSiswa = (CekKesehatanSiswa) session.createCriteria(CekKesehatanSiswa.class)
				.add(Restrictions.eq("calonSiswa", calonMahasiswa)).uniqueResult();

		if (cekKesehatanSiswa != null) {
			cekKesehatanSiswa = (CekKesehatanSiswa) session.load(CekKesehatanSiswa.class, cekKesehatanSiswa.getId());
		} else {
			cekKesehatanSiswa = new CekKesehatanSiswa();
		}

		cekKesehatanSiswa.setCalonSiswa(calonMahasiswa);
		cekKesehatanSiswa.setPenyakit1(penyakit1.getValue());
		cekKesehatanSiswa.setPenyakit2(penyakit2.getValue());
		cekKesehatanSiswa.setPenyakit3(penyakit3.getValue());
		cekKesehatanSiswa.setPenyakit4(penyakit4.getValue());
		cekKesehatanSiswa.setPenyakit5(penyakit5.getValue());
		cekKesehatanSiswa.setRontgen1(rontgen1.getValue());
		cekKesehatanSiswa.setRontgen2(rontgen2.getValue());
		cekKesehatanSiswa.setRontgen3(rontgen3.getValue());
		cekKesehatanSiswa.setSehatTerbatas1(sehatTerbatas1.getValue());
		cekKesehatanSiswa.setSehatTerbatas2(sehatTerbatas2.getValue());
		cekKesehatanSiswa.setSehatTerbatas3(sehatTerbatas3.getValue());
		cekKesehatanSiswa.setSehatTerbatas4(sehatTerbatas4.getValue());
		cekKesehatanSiswa.setTekananDarah(tekananDarah.getValue());
		cekKesehatanSiswa.setButaWarna(butaWarna.getValue());
		cekKesehatanSiswa.setNarkoba(narkoba.getValue());
		cekKesehatanSiswa.setSehat(
				(String) (status_sehat.getSelectedItem() == null ? "" : status_sehat.getSelectedItem().getValue()));
		cekKesehatanSiswa.setNoUrut(urut.getValue());

		if (cekKesehatanSiswa.getId() != null) {
			session.update(cekKesehatanSiswa);
			MyMessageboxConfig.show("Calon mahasiswa dengan nomor ujian " + calonMahasiswa.getNoUjian() + " atas nama "
					+ calonMahasiswa.getNama() + " dinyatakan " + cekKesehatanSiswa.getSehat());
			return;
		} else {
			session.save(cekKesehatanSiswa);
			MyMessageboxConfig.show("Calon mahasiswa dengan nomor ujian " + calonMahasiswa.getNoUjian() + " atas nama "
					+ calonMahasiswa.getNama() + " dinyatakan " + cekKesehatanSiswa.getSehat());
			return;
		}

	}

	@SuppressWarnings({ "rawtypes" })
	public static void onCetak(CalonSiswa calonSiswa) throws Exception {

		Map parameters = ais.common.HashMapGenerator.getRand();
		Session session = HibernateUtil.currentSession();
		CekKesehatanSiswa cekKesehatanSiswa = (CekKesehatanSiswa) session.createCriteria(CekKesehatanSiswa.class)
				.add(Restrictions.eq("calonSiswa", calonSiswa)).uniqueResult();

		if (cekKesehatanSiswa == null) {
			cekKesehatanSiswa = new CekKesehatanSiswa(calonSiswa);
			session.save(cekKesehatanSiswa);
		}

		calonSiswa.putPhoto(parameters);

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
		Integer num = 0;
		num = (Integer) session1.createCriteria(CekKesehatanSiswa.class).setProjection(Projections.rowCount())
				.add(Restrictions.ilike("noUrut", date, MatchMode.START)).uniqueResult();
		// if (num == null) {
		// num = 0;
		// }

		HibernateUtil.closeSession();
		num += 1;
		no_urut = date + num + "";
		return no_urut;

	}
}
