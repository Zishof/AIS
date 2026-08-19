package ais.action.master.psb;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;

import ais.action.master.sekolah.psb.CommonReportPsb;
import ais.action.report.CommonReportHelper;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.CekKesehatanSiswa;
import ais.ui.util.MyColumnConfig;
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

	private CalonSiswa calonSiswa;
	private North uploadMenu;
	private North menuLogin;
	private Combobox tahun;
	private Combobox bulan;
	private Combobox tanggal;

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
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
		Common.initLaguage();
	}

	public void onLogin(Event event) throws Exception {
		if (noRegistrasi.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show("Mohon maaf, Nomor Pendaftaran belum diisi. Langkah yang dapat dilakukan: (1) Ketik nomor pendaftaran atau nomor ujian Anda pada kolom yang tersedia; (2) Pastikan tidak ada spasi di awal atau akhir; (3) Ulangi proses masuk. Jika masih mengalami kendala, hubungi panitia penerimaan siswa baru.", "PERINGATAN", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}
		Session session = HibernateUtil.currentSession();
		if (tampilanPin != null && tampilanPin.isVisible()) {
			if (pinPassword.getValue().trim().isEmpty()) {
				MyMessageboxConfig.show("Mohon maaf, PIN / Kata Sandi belum diisi. Langkah yang dapat dilakukan: (1) Ketik PIN atau kata sandi yang diberikan panitia pada kolom yang tersedia; (2) Pastikan penulisan sudah benar (perhatikan huruf besar/kecil); (3) Ulangi proses masuk. Jika masih mengalami kendala, hubungi panitia penerimaan siswa baru.", "PERINGATAN", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return;
			}
			calonSiswa = (CalonSiswa) ConstantValues.simpleObject(
					session.createCriteria(CalonSiswa.class).add(Restrictions.isNotNull("gelombangPendaftaranPsb"))
							.add(Restrictions.ilike("pinPassword", pinPassword.getValue().trim(), MatchMode.EXACT))
							.add(Restrictions.or(
									Restrictions.ilike("noRegistrasi", noRegistrasi.getValue().trim(), MatchMode.EXACT),
									Restrictions.ilike("noUjian", noRegistrasi.getValue().trim(), MatchMode.EXACT))),
					CalonSiswa.class);
		} else {
			calonSiswa = (CalonSiswa) ConstantValues.simpleObject(
					session.createCriteria(CalonSiswa.class).add(Restrictions.isNotNull("gelombangPendaftaranPsb"))
							.add(Restrictions.or(
									Restrictions.ilike("noRegistrasi", noRegistrasi.getValue().trim(), MatchMode.EXACT),
									Restrictions.ilike("noUjian", noRegistrasi.getValue().trim(), MatchMode.EXACT))),
					CalonSiswa.class);
		}
		if (calonSiswa == null) {
			if (tampilanPin != null && tampilanPin.isVisible()) {
				MyMessageboxConfig.show(
						"Mohon maaf, Calon Mahasiswa dengan nomor pendaftaran \""  + noRegistrasi.getValue()
								+ "\" dan PIN / Password yang dimasukkan tidak ditemukan. Langkah yang dapat dilakukan: (1) Periksa kembali penulisan nomor pendaftaran; (2) Pastikan PIN / Password sudah benar; (3) Hubungi panitia penerimaan siswa baru jika masih mengalami kendala.",
						"PERINGATAN", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			} else {
				MyMessageboxConfig.show(
						"Mohon maaf, Calon Mahasiswa dengan nomor pendaftaran \"" + noRegistrasi.getValue()
								+ "\" tidak ditemukan, atau waktu terakhir bisa masuk telah terlewat. Langkah yang dapat dilakukan: (1) Periksa kembali penulisan nomor pendaftaran; (2) Pastikan masih dalam periode waktu yang ditentukan; (3) Hubungi panitia penerimaan siswa baru jika masih mengalami kendala.",
						"PERINGATAN", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			}
			return;
		}

		if (calonSiswa != null) {

			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(calonSiswa.getTanggalLahir());
			int thn = calendar.get(Calendar.YEAR);
			int bln = calendar.get(Calendar.MONTH);
			int tgl = calendar.get(Calendar.DATE);
			boolean kondisiTglLahir = (tahun.getSelectedItem() == null ? false
					: tahun.getSelectedItem().getValue().equals(thn))
					&& (bulan.getSelectedItem() == null ? false : bulan.getSelectedItem().getValue().equals(bln))
					&& (tanggal.getSelectedItem() == null ? false : tanggal.getSelectedItem().getValue().equals(tgl));

			if (kondisiTglLahir) {

				onSearchDefault(null);

				menuLogin.setVisible(false);

				Common.clear(uploadMenu);

				uploadMenu.setHeight("90px");

				MyGrid grid = new MyGrid();
				grid.setWidth("100%");
				grid.setParent(uploadMenu);
				grid.setWidth("100%");
				grid.setHeight("100%");

				Columns columns = new Columns();
				columns.setParent(grid);
				MyColumnConfig column = new MyColumnConfig();
				column.setParent(columns);
				column.setWidth("40%");

				column = new MyColumnConfig();
				column.setParent(columns);

				final Rows rows = new Rows();
				rows.setParent(grid);

				MyFormRow row = new MyFormRow();row.setValign("top");
				row.setParent(rows);

				row.appendChild(new ais.ui.util.MyLabelBold("Nomor Pendaftaran"));
				row.appendChild(new ais.ui.util.MyLabelBold(calonSiswa.getNoRegistrasi()));

				if (calonSiswa.getNoUjian() != null) {
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelBold("Nomor Ujian"));
					row.appendChild(new ais.ui.util.MyLabelBold(calonSiswa.getNoUjian()));
				}

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelBold("Nama"));
				row.appendChild(new ais.ui.util.MyLabelBold(calonSiswa.getNama()));

			} else {
				MyMessageboxConfig.show("Mohon maaf, Nomor Referensi atau Tanggal Lahir yang Anda masukkan tidak sesuai dengan data kami. Langkah yang dapat dilakukan: (1) Periksa kembali nomor pendaftaran atau referensi yang dimasukkan; (2) Pastikan Tahun, Bulan, dan Tanggal Lahir sudah dipilih dengan benar; (3) Ulangi proses masuk. Jika masih mengalami kendala, hubungi panitia penerimaan siswa baru.",
						"PERINGATAN", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			}
		} else {
			MyMessageboxConfig.show(
					"Mohon maaf, nomor registrasi Anda tidak ditemukan dalam sistem. Langkah yang dapat dilakukan: (1) Pastikan Anda telah menyelesaikan prosedur pembayaran pendaftaran terlebih dahulu; (2) Periksa kembali nomor registrasi yang dimasukkan; (3) Hubungi panitia penerimaan siswa baru untuk konfirmasi status pendaftaran Anda. Jika masih mengalami kendala, hubungi Administrator.",
					"PERINGATAN", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
		}
	}

	public void onSearchDefault(Event event) throws Exception {

		List<CalonSiswa> ruangPaketList = new ArrayList<CalonSiswa>();

		ruangPaketList.add(calonSiswa);

		ListModel strset = new SimpleListModel(ruangPaketList);
		grid.setRowRenderer(new CalonRenderer());
		grid.setModelCheckMobile(strset, true);

	}

	public static String genInfo(CalonSiswa calonSiswa) {
		if (!calonSiswa.getTelahDiterima()) {
			return "";
		}
		if (calonSiswa.getSekolah() == null) {
			return "";
		}
		Konfigurasi konfigurasi = Common.getKonfigurasi("informasi_kelulusan_sekolah",
				"NISN Anda [nis], nis ini bisa Anda gunakan untuk login ke http://ecampus dengan username NISN password NISN.");
		Konfigurasi konfigurasiTambahan = Common.getKonfigurasi("informasi_kelulusan_tambahan_sekolah",
				"Jika Anda belum melakukan pembayaran, silahkan lakukan pembayaran di ....(tanya ke akademik);Kode pembayaran dapat dilihat di ....(tanya ke akademik)");

		String info = "<ol>";
		info += "<li>" + (calonSiswa == null || calonSiswa.getNim() == null || calonSiswa.getNim().trim().isEmpty()
				? "Anda belum mendapatkan NIS"
				: org.apache.commons.lang3.StringUtils.replace(konfigurasi.getNilai(), "[nis]", calonSiswa.getNim())) + "</li>";

		// JenisKegiatan jenisKegiatan = (JenisKegiatan)
		// HibernateUtil.currentSession().createCriteria(JenisKegiatan.class)
		// .add(Restrictions.or(Restrictions.isNull("aktif"),
		// Restrictions.eq("aktif", true)))
		// .add(Restrictions.ilike("namaKegiatan",
		// ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU, MatchMode.EXACT))
		// .uniqueResult();
		// if (jenisKegiatan != null) {
		// Jurusan prodiLulus = calonSiswa.getProdiLulus();
		// List<DetailBiaya> detailBiayas = new ArrayList<DetailBiaya>();
		// if (prodiLulus == null || prodiLulus.getId() == null) {
		// Jurusan myjurusan1 = calonSiswa.getProdi1() == null ?
		// calonSiswa.getProdi2()
		// : calonSiswa.getProdi1();
		// java.util.Collection<DetailBiaya> detailBiayas1 =
		// PembayaranUtil.getInstance()
		// .getDetailBiayaCalonMahasiswa(calonSiswa, jenisKegiatan, myjurusan1,
		// false);
		//
		// detailBiayas.addAll(detailBiayas1);
		// } else {
		// java.util.Collection<DetailBiaya> detailBiayas1 =
		// PembayaranUtil.getInstance()
		// .getDetailBiayaCalonMahasiswa(calonSiswa, jenisKegiatan, prodiLulus,
		// false);
		// detailBiayas.addAll(detailBiayas1);
		// }
		//
		// if (!detailBiayas.isEmpty()) {
		// info += "<li>Besaran biaya : <br>";
		// for (DetailBiaya detailBiaya : detailBiayas) {
		// info += detailBiaya.getItemBiaya().getNama() + " : "
		// + Common.numberFormat.get().format((detailBiaya.getNilaiBiayaBaru() == null
		// ? detailBiaya.getNilaiBiaya() : detailBiaya.getNilaiBiayaBaru()))
		// + "<br>";
		// }
		// info += "</li>";
		// }
		//
		// Kegiatan kegiatan = calonSiswa.ambilKegiatans(null, jenisKegiatan);
		//
		// if (kegiatan != null) {
		// List<CicilanPembayaran> cicilanPembayarans =
		// HibernateUtil.currentSession()
		// .createCriteria(CicilanPembayaran.class).add(Restrictions.isNotNull("itemBiaya"))
		// .add(Restrictions.eq("kegiatan",
		// kegiatan)).addOrder(Order.asc("tanggal"))
		// .addOrder(Order.asc("ke")).list();
		// info += "<li>Anda telah membayar : " +
		// Common.numberFormat.get().format(kegiatan.getAmount()) + "<br>";
		// if (!cicilanPembayarans.isEmpty()) {
		// info += "Rincian : <br>";
		// for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans) {
		// info += cicilanPembayaran.getItemBiaya().getNama() + " : "
		// + Common.numberFormat.get().format(cicilanPembayaran.getNilai()) + "<br>";
		// }
		//
		// }
		// info += "</li>";
		// }
		// }
		// info += "</li>";

		for (String tambahan : StringUtils.split(konfigurasiTambahan.getNilai(), ";")) {
			info += "<li>" + tambahan + "</li>";
		}

		return info + "</ol>";
	}

	class CalonRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final CalonSiswa calonSiswa = (CalonSiswa) arg1;

			CommonMedia.tampilkanGambarKecil(calonSiswa).setParent(arg0);

			new Label(calonSiswa.getNoRegistrasi()).setParent(arg0);
			new Label(calonSiswa.getNoUjian()).setParent(arg0);
			new Label(calonSiswa.getNama().toUpperCase()).setParent(arg0);
			new Label(calonSiswa.getTelahDiterima() == null || !calonSiswa.getTelahDiterima()
					? "Belum dinyatakan lulus/diterima."
					: "Selamat, Anda diterima di " + (calonSiswa.getSekolah().getNama())).setParent(arg0);

			CekKesehatanSiswa sehat = (CekKesehatanSiswa) HibernateUtil.currentSession()
					.createCriteria(CekKesehatanSiswa.class).add(Restrictions.eq("calonSiswa", calonSiswa))
					.setMaxResults(1).uniqueResult();

			new Label(sehat == null ? "-" : sehat.getSehat()).setParent(arg0);

			new ais.ui.util.MyHtml(CariDataPesertaUjianAction.genInfo(calonSiswa)).setParent(arg0);
			
			
			// kebab popup (⋯) via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ket. Lulus", "/img/Configure.gif");
			button.setVisible(calonSiswa.getTelahDiterima());
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					CommonReportHelper.onCetakSuratKeteranganLulus(calonSiswa);

				}
			});
			aksiButtons.add(button);
			
			
			button = new MyToolbarbuttonConfig("Pernyataan Ortu.", "/img/print.png");
			button.setOrient("vertical");
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					CommonReportPsb.onCetakPernyataanOrtu(calonSiswa);
				}
			});
			aksiButtons.add(button);
			
			button = new MyToolbarbuttonConfig("Pernyataan Siswa.", "/img/print.png");
			button.setOrient("vertical");
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					CommonReportPsb.onCetakPernyataanSiswa(calonSiswa);
				}
			});
			aksiButtons.add(button);

			// Susun semua tombol: max 3 per baris, rata tengah
			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);

			// Hbox toolbar = new Hbox();
			// toolbar.setParent(arg0);
			// MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ket.
			// Lulus", "/img/Configure.gif");
			// button.setVisible(calonSiswa.getProdiLulus() != null);
			// button.addEventListener("onClick", new EventListener() {
			//
			// @Override
			// public void onEvent(Event arg0) throws Exception {
			// CommonReportHelper.onCetakSuratKeteranganLulus(calonSiswa);
			//
			// }
			// });
			// button.setParent(toolbar);
			//
			// if (sehat != null) {
			// button = new MyToolbarbuttonConfig("Ket. Sehat",
			// "/img/Configure.gif");
			// button.addEventListener("onClick", new EventListener() {
			//
			// @Override
			// public void onEvent(Event arg0) throws Exception {
			// CekKesehatanSiswaAction.onCetak(calonSiswa);
			// }
			// });
			// button.setParent(toolbar);
			// }
		}

	}

	public void onReset() {
		noRegistrasi.setValue("");

	}

}
