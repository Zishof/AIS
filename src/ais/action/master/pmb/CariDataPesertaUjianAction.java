package ais.action.master.pmb;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;

import ais.action.report.CommonReportHelper;
import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.CekKesehatan;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailBiaya;
import ais.database.model.Jurusan;
import ais.database.model.Kegiatan;
import ais.database.model.Konfigurasi;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

public class CariDataPesertaUjianAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1155733365712985677L;

	private Textbox noRegistrasi;

	private MyGrid grid;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	private Textbox pinPassword;
	private Row tampilanPin;

	private BiodataCalonMahasiswa biodataCalonMahasiswa;
	private North uploadMenu;
	private North menuLogin;
	private Combobox tahun;
	private Combobox bulan;
	private Combobox tanggal;

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);

		biodataCalonMahasiswa = Common.isLogin();
		if (biodataCalonMahasiswa != null) {
			afterLogin();
		} else {

			if (tampilanPin != null) { tampilanPin.setVisible(false); }
			MyComboitemConfig comboitem;
			for (int i = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) - 80; i < ais.ui.util.WaktuUtil
					.getCalendar().get(Calendar.YEAR) + 1; i++) {
				comboitem = new MyComboitemConfig();
				comboitem.setValue(i);
				comboitem.setLabel(i + "");
				tahun.appendChild(comboitem);
			}

			for (int i = 1; i <= 31; i++) {
				comboitem = new MyComboitemConfig();
				comboitem.setValue(i);
				comboitem.setLabel(i + "");
				tanggal.appendChild(comboitem);
			}
			Common.createComboBulan(bulan);
		}
		Common.initLaguage();
	}

	public void onLogin(Event event) throws Exception {
		if (noRegistrasi.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show(
					"Mohon maaf, Nomor Pendaftaran belum diisi. Langkah yang dapat dilakukan: (1) Ketikkan Nomor Pendaftaran Anda pada kolom yang tersedia; (2) Pastikan nomor sesuai dengan yang tertera pada bukti pendaftaran; (3) Ulangi proses masuk.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}
		Session session = HibernateUtil.currentSession();
		if (tampilanPin != null && tampilanPin.isVisible()) {
			if (pinPassword.getValue().trim().isEmpty()) {
				MyMessageboxConfig.show(
						"Mohon maaf, PIN / Kata Sandi belum diisi. Langkah yang dapat dilakukan: (1) Ketikkan PIN / Kata Sandi Anda pada kolom yang tersedia; (2) Pastikan penulisan huruf besar dan kecil sudah benar; (3) Ulangi proses masuk.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return;
			}
			biodataCalonMahasiswa = (BiodataCalonMahasiswa) ConstantValues.simpleObject(
					session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setMaxResults(1)

							.add(Restrictions.ilike("pinPassword", pinPassword.getValue().trim(), MatchMode.EXACT))
							.add(Restrictions.or(
									Restrictions.ilike("noRegistrasi", noRegistrasi.getValue().trim(), MatchMode.EXACT),
									Restrictions.ilike("noUjian", noRegistrasi.getValue().trim(), MatchMode.EXACT))),
					BiodataCalonMahasiswa.class);
		} else {
			biodataCalonMahasiswa = (BiodataCalonMahasiswa) ConstantValues.simpleObject(
					session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setMaxResults(1)

							.add(Restrictions.or(
									Restrictions.ilike("noRegistrasi", noRegistrasi.getValue().trim(), MatchMode.EXACT),
									Restrictions.ilike("noUjian", noRegistrasi.getValue().trim(), MatchMode.EXACT))),
					BiodataCalonMahasiswa.class);
		}
		if (biodataCalonMahasiswa == null) {
			if (tampilanPin != null && tampilanPin.isVisible()) {
				MyMessageboxConfig.showFormat(
						"Mohon maaf, data Calon Mahasiswa dengan Nomor Pendaftaran \"{V1}\" dan PIN / Kata Sandi yang Anda masukkan tidak ditemukan. Langkah yang dapat dilakukan: (1) Periksa kembali penulisan Nomor Pendaftaran dan PIN / Kata Sandi; (2) Pastikan tidak terdapat spasi berlebih; (3) Hubungi panitia penerimaan mahasiswa baru apabila masih mengalami kendala.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, noRegistrasi.getValue());
			} else {
				MyMessageboxConfig.showFormat(
						"Mohon maaf, data Calon Mahasiswa dengan Nomor Pendaftaran \"{V1}\" tidak ditemukan. Langkah yang dapat dilakukan: (1) Periksa kembali penulisan Nomor Pendaftaran; (2) Pastikan nomor sesuai dengan bukti pendaftaran; (3) Hubungi panitia penerimaan mahasiswa baru apabila masih mengalami kendala.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, noRegistrasi.getValue());
			}
			return;
		}

		if (biodataCalonMahasiswa != null) {

//			if (biodataCalonMahasiswa.getDitolak()) {
//				Messagebox.show(
//						"Maaf, Anda tidak diterima / ditolak untuk login, hubungi panitia untuk informasi lebih lanjut",
//						"PERINGATAN", Messagebox.OK, Messagebox.EXCLAMATION);
//				return;
//			}
//			if (biodataCalonMahasiswa.getMundur()) {
//				Messagebox.show("Maaf, Anda dinyatakan mengundurkan diri, hubungi panitia untuk informasi lebih lanjut",
//						"PERINGATAN", Messagebox.OK, Messagebox.EXCLAMATION);
//				return;
//			}

			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(biodataCalonMahasiswa.getTanggalLahir());
			int thn = calendar.get(Calendar.YEAR);
			int bln = calendar.get(Calendar.MONTH);
			int tgl = calendar.get(Calendar.DATE);
			boolean kondisiTglLahir = (tahun.getSelectedItem() == null ? false
					: tahun.getSelectedItem().getValue().equals(thn))
					&& (bulan.getSelectedItem() == null ? false : bulan.getSelectedItem().getValue().equals(bln))
					&& (tanggal.getSelectedItem() == null ? false : tanggal.getSelectedItem().getValue().equals(tgl));

			if (kondisiTglLahir) {
				Common.setLogin(biodataCalonMahasiswa);
				afterLogin();

			} else {
				MyMessageboxConfig.show(
						"Mohon maaf, Nomor Pendaftaran atau Tanggal Lahir yang Anda masukkan belum sesuai. Langkah yang dapat dilakukan: (1) Periksa kembali Nomor Pendaftaran Anda; (2) Pastikan pilihan Tanggal, Bulan, dan Tahun Lahir sudah benar; (3) Ulangi proses masuk.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			}
		} else {
			MyMessageboxConfig.show(
					"Mohon maaf, Nomor Pendaftaran Anda tidak ditemukan. Langkah yang dapat dilakukan: (1) Pastikan Anda telah mengikuti prosedur pembayaran dengan benar; (2) Periksa kembali penulisan Nomor Pendaftaran; (3) Hubungi panitia penerimaan mahasiswa baru apabila masih mengalami kendala.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
		}
	}

	public void afterLogin() throws Exception {
		onSearchDefault(null);

		menuLogin.setVisible(false);

		Common.clear(uploadMenu);

		uploadMenu.setHeight("0px");

//		MyGrid grid = new MyGrid();
//		grid.setWidth("100%");
//		grid.setParent(uploadMenu);
//		grid.setWidth("100%");
//		grid.setHeight("100%");
//
//		Columns columns = new Columns();
//		columns.setParent(grid);
//		MyColumnConfig column = new MyColumnConfig();
//		column.setParent(columns);
//		column.setWidth("40%");
//
//		column = new MyColumnConfig();
//		column.setParent(columns);
//
//		final Rows rows = new Rows();
//		rows.setParent(grid);
//
//		MyFormRow row = new MyFormRow();row.setValign("top");
////		row.setParent(rows);
//
//		row.appendChild(new ais.ui.util.MyLabelBold("Nomor Pendaftaran"));
//		row.appendChild(new ais.ui.util.MyLabelBold(biodataCalonMahasiswa.getNoRegistrasi()));
//
//		if (biodataCalonMahasiswa.getNoUjian() != null) {
////			row.setParent(rows);
//			row.appendChild(new ais.ui.util.MyLabelBold("Nomor Ujian"));
//			row.appendChild(new ais.ui.util.MyLabelBold(biodataCalonMahasiswa.getNoUjian()));
//		}
//
//		row = new MyFormRow();
////		row.setParent(rows);
//		row.appendChild(new ais.ui.util.MyLabelBold("Nama"));
//		row.appendChild(new ais.ui.util.MyLabelBold(biodataCalonMahasiswa.getNama()));
	}

	public void onSearchDefault(Event event) throws Exception {

		List<BiodataCalonMahasiswa> ruangPaketList = new ArrayList<BiodataCalonMahasiswa>();

		ruangPaketList.add(biodataCalonMahasiswa);

		ListModel strset = new SimpleListModel(ruangPaketList);
		grid.setRowRenderer(new CalonRenderer());
		grid.setModelCheckMobile(strset, true);

	}

	public static String genInfo(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		Kegiatan kegiatanDaftarUlang = biodataCalonMahasiswa.getPembayaranDaftarUlang();
		Integer smt = null;
		Jurusan prodiLulus = biodataCalonMahasiswa.getProdiLulus();
		List<DetailBiaya> detailBiayas = new ArrayList<DetailBiaya>();

		if (kegiatanDaftarUlang != null) {
			if (prodiLulus == null || prodiLulus.getId() == null) {
				Jurusan myjurusan1 = biodataCalonMahasiswa.getProdi1() == null ? biodataCalonMahasiswa.getProdi2()
						: biodataCalonMahasiswa.getProdi1();
				java.util.Collection<DetailBiaya> detailBiayas1 = PembayaranUtil.getInstance()
						.getDetailBiayaCalonMahasiswa(biodataCalonMahasiswa, kegiatanDaftarUlang.getJenisKegiatan(),
								myjurusan1, smt, true);

				detailBiayas.addAll(detailBiayas1);
			} else {
				java.util.Collection<DetailBiaya> detailBiayas1 = PembayaranUtil.getInstance()
						.getDetailBiayaCalonMahasiswa(biodataCalonMahasiswa, kegiatanDaftarUlang.getJenisKegiatan(),
								prodiLulus, 1, true);
				detailBiayas.addAll(detailBiayas1);
			}
		}
		return genInfo(biodataCalonMahasiswa, detailBiayas);
	}

	@SuppressWarnings("rawtypes")
	public static String genInfo(BiodataCalonMahasiswa biodataCalonMahasiswa, Collection detailBiayas) {

		if (biodataCalonMahasiswa.getMundur()) {
			return "Mengundurkan diri" + (biodataCalonMahasiswa.getKeterangan().isEmpty() ? ""
					: "<br>(Keterangan : " + biodataCalonMahasiswa.getKeterangan() + ")");
		}
		if (biodataCalonMahasiswa.getDitolak()) {
			return "Tidak Lulus" + (biodataCalonMahasiswa.getKeterangan().isEmpty() ? ""
					: "<br>(Keterangan : " + biodataCalonMahasiswa.getKeterangan() + ")");
		}
		if (biodataCalonMahasiswa.getProdiLulus() == null) {
			return "Belum Dinyatakan Lulus / Tidak" + (biodataCalonMahasiswa.getKeterangan().isEmpty() ? ""
					: "<br>(Keterangan : " + biodataCalonMahasiswa.getKeterangan() + ")");
		}

		if (biodataCalonMahasiswa.getPaket() != null && !biodataCalonMahasiswa.getPaket().getKeterangan().isEmpty()) {
			if (Common.bolehKonfigurasi("keterangan_paket_digunakan_sebagai_info_kelulusan_jika_diisi", Konfigurasi.TIDAK_AKTIF)) {
				return biodataCalonMahasiswa.getPaket().getKeterangan();
			}
		}

		Konfigurasi konfigurasi = Common.getKonfigurasi("informasi_kelulusan",
				"NIM Anda [nim], nim ini bisa Anda gunakan untuk login ke http://ecampus dengan username NIM password NIM.");
		Konfigurasi konfigurasiTambahan = Common.getKonfigurasi("informasi_kelulusan_tambahan",
				"Jika Anda belum melakukan pembayaran, silahkan lakukan pembayaran di ....(tanya ke akademik);Kode pembayaran dapat dilihat di ....(tanya ke akademik)");

		String info = "<ol>";
		info += "<li>"
				+ (biodataCalonMahasiswa == null || biodataCalonMahasiswa.getNim() == null
						|| biodataCalonMahasiswa.getNim().trim().isEmpty()
								? Common.getKonfigurasi("informasi_kelulusan_belum_dapat_nim",
										"Anda belum mendapatkan NIM").getNilai()
								: org.apache.commons.lang3.StringUtils.replace(konfigurasi.getNilai(), "[nim]", biodataCalonMahasiswa.getNim()))
				+ "</li>";

		Kegiatan kegiatanDaftarUlang = biodataCalonMahasiswa.getPembayaranDaftarUlang();
		if (kegiatanDaftarUlang != null) {
			if (!detailBiayas.isEmpty()) {
				info += "<li>Besaran biaya : <ul>";
				for (Object o : detailBiayas) {

					if (o instanceof PengaturanPembayaranBulanan) {
						DetailBiaya detailBiaya = ((PengaturanPembayaranBulanan) o).getDetailBiaya();
						info += "<li>" + detailBiaya.getItemBiaya().getNama() + " : "
								+ Common.numberFormat.get()
										.format((detailBiaya.getNilaiBiayaBaru() == null ? detailBiaya.getNilaiBiaya()
												: detailBiaya.getNilaiBiayaBaru()))
								+ "</li>";
					} else if (o instanceof DetailBiaya) {
						DetailBiaya detailBiaya = (DetailBiaya) o;
						info += "<li>" + detailBiaya.getItemBiaya().getNama() + " : "
								+ Common.numberFormat.get()
										.format((detailBiaya.getNilaiBiayaBaru() == null ? detailBiaya.getNilaiBiaya()
												: detailBiaya.getNilaiBiayaBaru()))
								+ "</li>";
					}
				}
				info += "</ul></li>";
			}

			Kegiatan kegiatan = kegiatanDaftarUlang;

			if (kegiatan != null) {
				List<CicilanPembayaran> cicilanPembayarans = kegiatan.ambilCicilan();
				info += "<li>Anda telah membayar : " + Common.numberFormat.get().format(kegiatan.getAmount()) + "<br>";
				if (!cicilanPembayarans.isEmpty()) {
					info += "Rincian : <br>";
					for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans) {
						info += cicilanPembayaran.getItemBiaya().getNama() + " : "
								+ Common.numberFormat.get().format(cicilanPembayaran.getNilai()) + "<br>";
					}

				}
				info += "</li>";
			}
		}
		info += "</li>";

		if (Common.bolehKonfigurasi("tampilkan_info_ukt_ke_di_pmb")) {
			try {
				if (biodataCalonMahasiswa.getStatusAwalMahasiswa() != null
						&& biodataCalonMahasiswa.getStatusAwalMahasiswa().getNama().toLowerCase().contains("ukt")) {
					info += "<li>Lulus di " + (biodataCalonMahasiswa.getStatusAwalMahasiswa().getNama()
							.replace("Baru-", "").replace("-", " ")) + "</li>";
				}
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
		}

		for (String tambahan : StringUtils.split(konfigurasiTambahan.getNilai(), ";")) {
			info += "<li>" + tambahan + "</li>";
		}

		return info + "</ol>" + (biodataCalonMahasiswa.getKeterangan().isEmpty() ? ""
				: "<br>(Keterangan : " + biodataCalonMahasiswa.getKeterangan() + ")");
	}

	class CalonRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final BiodataCalonMahasiswa biodataCalonMahasiswa = (BiodataCalonMahasiswa) arg1;

			CommonMedia.tampilkanGambarKecil(biodataCalonMahasiswa).setParent(arg0);

			new Label(biodataCalonMahasiswa.getNoRegistrasi()).setParent(arg0);
			new Label((biodataCalonMahasiswa.getNoUjian() == null ? "" : biodataCalonMahasiswa.getNoUjian())
					+ (biodataCalonMahasiswa.getMahasiswa() == null ? ""
							: " / " + biodataCalonMahasiswa.getMahasiswa().getNim()))
					.setParent(arg0);
			new Label(biodataCalonMahasiswa.getNama().toUpperCase()).setParent(arg0);

			if (Common.bolehKonfigurasi("integrasi_pmb_arkatama", Konfigurasi.TIDAK_AKTIF)) {
				new Label(biodataCalonMahasiswa.getProdiLulus() == null ? "" : "Lulus Berkas").setParent(arg0);
			} else {
				new Label(biodataCalonMahasiswa.getProdiLulus() == null ? ""
						: "Lulus di prodi " + (biodataCalonMahasiswa.getProdiLulus().getNama())).setParent(arg0);
			}

			CekKesehatan sehat = (CekKesehatan) HibernateUtil.currentSession().createCriteria(CekKesehatan.class)
					.add(Restrictions.eq("biodataCalonMahasiswa", biodataCalonMahasiswa)).setMaxResults(1)
					.uniqueResult();

			new Label(sehat == null ? "-" : sehat.getSehat()).setParent(arg0);

			new ais.ui.util.MyHtml(CariDataPesertaUjianAction.genInfo(biodataCalonMahasiswa)).setParent(arg0);

			Hbox toolbar = new Hbox();
			toolbar.setParent(arg0);
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ket. Lulus", "/img/Configure.gif");
			button.setVisible(biodataCalonMahasiswa.getProdiLulus() != null);
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					CommonReportHelper.onCetakSuratKeteranganLulus(biodataCalonMahasiswa, false);

				}
			});
			button.setParent(toolbar);

			if (sehat != null) {
				button = new MyToolbarbuttonConfig("Ket. Sehat", "/img/Configure.gif");
				button.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						CekKesehatanAction.onCetak(biodataCalonMahasiswa);
					}
				});
				button.setParent(toolbar);
			}
		}

	}

	public void onReset() {
		noRegistrasi.setValue("");

	}

}
