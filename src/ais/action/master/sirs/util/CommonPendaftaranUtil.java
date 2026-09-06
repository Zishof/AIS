package ais.action.master.sirs.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Button;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.East;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Window;

import ais.action.master.sirs.detail.RacikanDetailAction;
import ais.action.master.sirs.helper.AmbilDataAlatMedisBanbox;
import ais.action.master.sirs.helper.AmbilDataAsuransiBanbox;
import ais.action.master.sirs.helper.AmbilDataDokterBanyak;
import ais.action.master.sirs.helper.AmbilDataItemMedisBanbox;
import ais.action.master.sirs.helper.AmbilDataPasienBanbox;
import ais.action.master.sirs.helper.AmbilDataPendaftaranSemuaBanbox;
import ais.action.master.sirs.helper.AmbilDataRacikanBanbox;
import ais.action.master.sirs.helper.AmbilDataTindakanBanbox;
import ais.action.master.sirs.jadwal_dokter.AmbilJadwalBulanan;
import ais.action.master.sirs.jadwal_dokter.AmbilJadwalHarian;
import ais.common.Common;
import ais.common.CommonSirs;
import ais.common.ConstantValues;
import ais.common.listener.TransaksiListener;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.AlatMedis;
import ais.database.model.sirs.Asuransi;
import ais.database.model.sirs.Biaya;
import ais.database.model.sirs.BiayaAlatMedisPerKelas;
import ais.database.model.sirs.BiayaTindakanPerKelas;
import ais.database.model.sirs.BookingRegistrasi;
import ais.database.model.sirs.DetailTransaksiLayanan;
import ais.database.model.sirs.DetailTransaksiPasien;
import ais.database.model.sirs.DiagnosaPenyakit;
import ais.database.model.sirs.Diskon;
import ais.database.model.sirs.Dokter;
import ais.database.model.sirs.HargaJualItem;
import ais.database.model.sirs.ItemMedis;
import ais.database.model.sirs.JadwalDokter;
import ais.database.model.sirs.JenisBiayaLain;
import ais.database.model.sirs.KelasPerawatan;
import ais.database.model.sirs.PajakMedis;
import ais.database.model.sirs.PaketPerawatanDetail;
import ais.database.model.sirs.PaketPerawatanDetailPasien;
import ais.database.model.sirs.Pasien;
import ais.database.model.sirs.Pendaftaran;
import ais.database.model.sirs.Racikan;
import ais.database.model.sirs.RacikanDetail;
import ais.database.model.sirs.TarifKhususPunyaTindakan;
import ais.database.model.sirs.Tindakan;
import ais.database.model.sirs.TransaksiMedis;
import ais.database.model.sirs.TransaksiMedisDetail;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyTextbox;

/**
 * Tipe khusus untuk common pendaftaran util. Kelas ini memberi nama dan batas tanggung jawab yang
 * eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> gunakan tipe ini hanya untuk state dan operasi yang sesuai dengan nama
 * domainnya. Logika lintas domain harus didelegasikan ke service atau helper bersama supaya tidak muncul
 * implementasi paralel dengan hasil berbeda.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah inisialisasi/lifecycle ({@code initTransaksi()}, {@code
 * initBookingRegistrasi()}, {@code initPendaftaran()}, {@code initJadwalPemeriksaan()}, {@code
 * initJadwalPemeriksaan()}); validasi/perhitungan ({@code validasiTransaksiDetailPaket()}, {@code
 * validasiTransaksiDetailPaketFinal()}, {@code validasiTransaksiItem()}, {@code validasiTransaksiAlatMedis()},
 * {@code validasiTransaksiLayanan()}); mutasi data ({@code setDetailBiayaPaket()}, {@code setDetailBiaya()},
 * {@code setDetailBiaya()}, {@code setDetailBiaya()}, {@code setDetailBiaya()}, {@code setDetailBiayaPaket()});
 * operasi domain lain ({@code generateNomorAntrian()}, {@code generateNomorAntrian()}, {@code
 * riwayatPenyakitPasien()}, {@code riwayatPenyakitPasien()}, {@code dokterDanBidanPemeriksa()}, {@code
 * displayDetailPaket()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 */
public class CommonPendaftaranUtil {

	private static String formatTanggalLahirTransaksi(Pasien pasien) {
		Date tanggal = pasien == null ? null : pasien.getTanggalLahir();
		return tanggal == null ? "" : Common.dateFormat2.get().format(tanggal);
	}

	private static String formatUmurTransaksi(Pasien pasien) {
		Integer umur = pasien == null ? null : pasien.getUmur();
		return umur == null ? "" : umur + " thn";
	}

	@SuppressWarnings("deprecation")
	public static EventListener initTransaksi(final Rows rows, Label kode, final TransaksiMedis transaksi,
			final TransaksiListener transaksiListener) throws Exception {

		Pasien pasien = transaksi.getPasien();
		final Checkbox bebas = new Checkbox();
		final AmbilDataPendaftaranSemuaBanbox pendaftaran = new AmbilDataPendaftaranSemuaBanbox(false);
		final AmbilDataPasienBanbox pasienBanbox = new AmbilDataPasienBanbox();
		final MyDatebox tanggalTransaksi = new MyDatebox(
				transaksi.getTanggalTransaksi() == null ? new Date() : transaksi.getTanggalTransaksi());
		final MyTextbox nama = new MyTextbox(transaksi.getNama());
		final Label komunitas = new Label(transaksi.getPendaftaran() == null ? ""
				: transaksi.getPendaftaran().getKomunitass().toString().replaceAll("\\[", "").replaceAll("\\]", ""));
		final Label asuransi = new Label(
				transaksi.getPendaftaran() == null || transaksi.getPendaftaran().getAsuransi() == null ? ""
						: transaksi.getPendaftaran().getAsuransi().toString());
		final Label umur = new Label(
				transaksi == null || transaksi.getUmur() == null ? "" : transaksi.getUmur() + " thn");
		final Label alamat = new Label(pasien == null ? "" : pasien.getAlamatLengkap());
		final KelasPerawatan kelasPerawatanAwal = transaksi.getKelasPerawatan();
		final Label kelasPerawatan = new Label(kelasPerawatanAwal == null || kelasPerawatanAwal.getNama() == null ? ""
				: kelasPerawatanAwal.getNama());
		final Label ttl = new Label(
				pasien == null ? "" : (pasien.getTempatLahir() == null ? "" : pasien.getTempatLahir() + " / "));
		final Label tglLahir = new Label(formatTanggalLahirTransaksi(pasien));
		final Label jenisKelamin = new Label(transaksi.getJenisKelamin());
		final Label jenisPasien = new Label(
				transaksi.getJenisPasien() == null ? "" : transaksi.getJenisPasien().toString());
		final MyTextbox keterangan = new MyTextbox(transaksi.getKeterangan() == null ? "" : transaksi.getKeterangan());

		final EventListener perubahanPasienListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Pendaftaran myPendaftaran = (Pendaftaran) ((arg0 != null && arg0.getData() instanceof Pendaftaran)
						? arg0.getData()
						: (pendaftaran.getAttribute("pendaftaran")));
				Pasien pasien = (Pasien) pasienBanbox.getAttribute("pasien");

				if (myPendaftaran == null && !bebas.isChecked()) {
					myPendaftaran = (Pendaftaran) HibernateUtil.currentSession().createCriteria(Pendaftaran.class)
							.add(Restrictions.eq("pasien", pasien)).add(Restrictions.eq("lunas", false))
							.addOrder(Order.desc("tanggalPendaftaran")).setMaxResults(1).uniqueResult();
				} else if (!bebas.isChecked()) {
					myPendaftaran = (Pendaftaran) HibernateUtil.currentSession().createCriteria(Pendaftaran.class)
							.add(Restrictions.idEq(myPendaftaran.getId())).uniqueResult();
				}

				pendaftaran.setAttribute("pendaftaran", myPendaftaran);
				pendaftaran.setValue(myPendaftaran == null ? "" : myPendaftaran.getKode());

				pasien = myPendaftaran == null ? ((Pasien) pasienBanbox.getAttribute("pasien"))
						: myPendaftaran.getPasien();
				pasienBanbox.setValue(myPendaftaran == null || myPendaftaran.getPasien() == null ? ""
						: myPendaftaran.getPasien().getKode());
				pasienBanbox.setAttribute("pasien", myPendaftaran == null ? null : myPendaftaran.getPasien());

				KelasPerawatan kelasPendaftaran = myPendaftaran == null ? null : myPendaftaran.getKelasPerawatan();
				kelasPerawatan.setValue(kelasPendaftaran == null || kelasPendaftaran.getNama() == null ? ""
						: kelasPendaftaran.getNama());

				pasienBanbox.setValue(pasien == null || pasien.getKode() == null ? "" : pasien.getKode().trim());

				if (!bebas.isChecked()) {
					nama.setValue(pasien == null ? "" : pasien.getNama());
				}

				umur.setValue(formatUmurTransaksi(pasien));

				alamat.setValue(pasien == null ? "" : pasien.getAlamatLengkap());
				ttl.setValue(
						pasien == null ? "" : (pasien.getTempatLahir() == null ? "" : pasien.getTempatLahir() + " / "));
				tglLahir.setValue(formatTanggalLahirTransaksi(pasien));

				jenisKelamin.setValue(
						pasien == null ? "" : pasien.getJenisKelamin() == null ? "" : pasien.getJenisKelamin());

				jenisPasien.setValue(myPendaftaran == null || myPendaftaran.getJenisPasien() == null ? ""
						: myPendaftaran.getJenisPasien().getNama());

				komunitas.setValue(myPendaftaran == null ? ""
						: myPendaftaran.getKomunitass().toString().replaceAll("\\[", "").replaceAll("\\]", ""));

				asuransi.setValue(myPendaftaran == null || myPendaftaran.getAsuransi() == null ? ""
						: myPendaftaran.getAsuransi().toString());

				if (bebas.isChecked()) {
					pendaftaran.setValue("");
					pendaftaran.setAttribute("pendaftaran", null);
					pasienBanbox.setValue("");
					pasienBanbox.setAttribute("pasien", null);
				}
				@SuppressWarnings("unchecked")
				List<Row> myRows = rows.getChildren();
				for (Row row : myRows) {
					if (row.getAttribute("hide") == null) {
						row.setVisible(!bebas.isChecked());
					}
				}
				pendaftaran.setDisabled(bebas.isChecked());
				pasienBanbox.setDisabled(bebas.isChecked());

				transaksiListener.onBerubah(bebas.isChecked(), myPendaftaran, pasien, nama.getValue(),
						tanggalTransaksi.getValue(),
						(myPendaftaran == null || myPendaftaran.getKelasPerawatan() == null ? ConstantValues.kelasNormal
								: myPendaftaran.getKelasPerawatan()),
						keterangan.getValue());
			}

		};

		Row row = new Row();
		row.setAttribute("hide", "no");
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode Transaksi")));
		row.appendChild(kode);

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Bebas")));
		row.appendChild(bebas);
		bebas.setChecked(transaksi.getBebas());
		bebas.addEventListener("onCheck", perubahanPasienListener);

		row = new Row();
		row.setAttribute("hide", "no");
		ais.ui.util.ZkCompat.setSpans(row, "4");
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Html("<hr>"));

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Pendaftaran")));
		row.appendChild(pendaftaran);
		pendaftaran.setAttribute("pendaftaran", transaksi.getPendaftaran());
		pendaftaran.setValue(transaksi.getPendaftaran() == null ? "" : transaksi.getPendaftaran().getKode());
		pendaftaran.setWidth("90%");
		pendaftaran.setEventListener(perubahanPasienListener);

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Pasien")));

		row.appendChild(pasienBanbox);
		pasienBanbox.setValue(transaksi.getPasien() == null ? "" : transaksi.getPasien().getKode());
		pasienBanbox.setAttribute("pasien", transaksi.getPasien());
		pasienBanbox.setEventListener(perubahanPasienListener);
		pasienBanbox.setWidth("90%");

		row = new Row();
		row.setAttribute("hide", "no");
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal")));
		row.appendChild(tanggalTransaksi);
		tanggalTransaksi.setFormat(Common.dateFormat3.get().toPattern());
		tanggalTransaksi.setWidth("90%");
		tanggalTransaksi.addEventListener("onChange", perubahanPasienListener);

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama")));
		row.appendChild(nama);
		nama.setWidth("90%");
		nama.addEventListener("onChange", perubahanPasienListener);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Komunitas")));
		row.appendChild(komunitas);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Asuransi")));
		row.appendChild(asuransi);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Umur")));
		row.appendChild(umur);

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Alamat")));
		row.appendChild(alamat);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kelas")));
		row.appendChild(kelasPerawatan);

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("TTL")));

		Hbox myHbox = new Hbox();
		myHbox.setParent(row);
		myHbox.appendChild(ttl);
		myHbox.appendChild(tglLahir);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Kelamin")));
		row.appendChild(jenisKelamin);

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Pasien")));
		row.appendChild(jenisPasien);

		row = new Row();
		ais.ui.util.ZkCompat.setSpans(row, "1,3");
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Keterangan")));
		row.appendChild(keterangan);
		keterangan.setWidth("95%");
		keterangan.setRows(2);
		keterangan.addEventListener("onChange", perubahanPasienListener);

		if (transaksi.getId() != null) {
			perubahanPasienListener.onEvent(new Event("", null, transaksi.getPendaftaran()));
		}

		return perubahanPasienListener;
	}

	@SuppressWarnings("deprecation")
	public static EventListener initBookingRegistrasi(Rows rows, Label kode, final BookingRegistrasi bookingRegistrasi,
			final EventListener myEventListener) throws Exception {

		final AmbilDataPasienBanbox mypasien = new AmbilDataPasienBanbox();
		// final Label kode = new Label(bookingRegistrasi.getKode());
		final MyDatebox tanggalBookingRegistrasi = new MyDatebox(
				bookingRegistrasi.getTanggalBookingRegistrasi() == null ? new Date()
						: bookingRegistrasi.getTanggalBookingRegistrasi());

		final Pasien pasien = bookingRegistrasi.getPasien();
		final Label nama = new Label(pasien == null ? "" : pasien.getNama());

		final Label komunitas = new Label(
				bookingRegistrasi.getKomunitass().toString().replaceAll("\\[", "").replaceAll("\\]", ""));
		final Label alamat = new Label(pasien == null ? "" : pasien.getAlamatLengkap());
		final Label umur = new Label(bookingRegistrasi == null || bookingRegistrasi.getPasien() == null ? ""
				: bookingRegistrasi.getPasien().getUmur().toString() + " thn");
		final Label jenisKelamin = new Label(
				pasien == null ? "" : pasien.getJenisKelamin() == null ? "" : pasien.getJenisKelamin());
		final Label jenisPasien = new Label(
				pasien == null || pasien.getJenisPasien() == null ? "" : pasien.getJenisPasien().getNama());
		final Label ttl = new Label(
				pasien == null ? "" : (pasien.getTempatLahir() == null ? "" : pasien.getTempatLahir() + " / "));
		final Label tglLahir = new Label(pasien == null ? null : Common.dateFormat2.get().format(pasien.getTanggalLahir()));
		final AmbilDataAsuransiBanbox myasuransi = new AmbilDataAsuransiBanbox();
		final MyTextbox keterangan = new MyTextbox(bookingRegistrasi.getKeterangan());

		final EventListener populateDataEventListener = new EventListener() {

			@SuppressWarnings({ "rawtypes", "unchecked" })
			@Override
			public void onEvent(Event arg0) throws Exception {

				Pasien pasien = (Pasien) mypasien.getAttribute("pasien");
				Asuransi asuransi = (Asuransi) myasuransi.getAttribute("asuransi");

				bookingRegistrasi.setAsuransi(asuransi);
				bookingRegistrasi.setPasien(pasien);
				bookingRegistrasi.setTanggalBookingRegistrasi(tanggalBookingRegistrasi.getValue());
				bookingRegistrasi.setKeterangan(keterangan.getValue());

				Map data = new HashMap();
				data.put("bookingRegistrasi", bookingRegistrasi);
				data.put("pasien", pasien);
				data.put("asuransi", asuransi);
				data.put("tanggalBookingRegistrasi", tanggalBookingRegistrasi.getValue());
				myEventListener.onEvent(new Event("", mypasien, data));
			}
		};

		EventListener perubahanPasienListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (arg0 != null && arg0.getData() != null && arg0.getData() instanceof Pasien) {
					bookingRegistrasi.setPasien((Pasien) arg0.getData());
					bookingRegistrasi.setAsuransi(bookingRegistrasi.getPasien().getAsuransi());
					mypasien.setValue(
							bookingRegistrasi.getPasien() == null ? "" : bookingRegistrasi.getPasien().getKode());
					mypasien.setAttribute("pasien", bookingRegistrasi.getPasien());
					mypasien.setDisabled(true);
				} else {
					mypasien.setDisabled(false);
				}

				Pasien pasien = (Pasien) mypasien.getAttribute("pasien");
				mypasien.setValue(pasien == null ? "" : pasien.getKode().trim());
				nama.setValue(pasien == null ? "" : pasien.getNama());

				alamat.setValue(pasien == null ? "" : pasien.getAlamatLengkap());

				ttl.setValue(
						pasien == null ? "" : (pasien.getTempatLahir() == null ? "" : pasien.getTempatLahir() + " / "));
				umur.setValue(pasien == null ? "" : pasien.getUmur().toString() + " thn");
				tglLahir.setValue(pasien == null ? null : Common.dateFormat2.get().format(pasien.getTanggalLahir()));
				jenisKelamin.setValue(
						pasien == null ? "" : pasien.getJenisKelamin() == null ? "" : pasien.getJenisKelamin());

				myasuransi
						.setValue(pasien == null || pasien.getAsuransi() == null ? "" : pasien.getAsuransi().getNama());
				myasuransi.setAttribute("asuransi", pasien == null ? null : pasien.getAsuransi());

				jenisPasien.setValue(
						pasien == null || pasien.getJenisPasien() == null ? "" : pasien.getJenisPasien().getNama());

				bookingRegistrasi.setPasien(pasien);
				bookingRegistrasi.setPasienKomunitas(bookingRegistrasi.getPasien());
				komunitas.setValue(
						bookingRegistrasi.getKomunitass().toString().replaceAll("\\[", "").replaceAll("\\]", ""));

				populateDataEventListener.onEvent(null);
			}
		};

		Row row = new Row();
		row.setAttribute("hide", "no");
		ais.ui.util.ZkCompat.setSpans(row, "4");
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Html("<hr>"));

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode Booking")));
		row.appendChild(kode);

		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Waktu Booking")));
		row.appendChild(tanggalBookingRegistrasi);
		tanggalBookingRegistrasi.setFormat(Common.dateFormat3.get().toPattern());
		tanggalBookingRegistrasi.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Pasien")));
		row.appendChild(mypasien);
		mypasien.setValue(bookingRegistrasi.getPasien() == null ? "" : bookingRegistrasi.getPasien().getKode());
		mypasien.setAttribute("pasien", bookingRegistrasi.getPasien());
		mypasien.setWidth("90%");

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama Pasien")));
		row.appendChild(nama);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Komunitas")));
		row.appendChild(komunitas);

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Alamat")));
		row.appendChild(alamat);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Umur")));
		row.appendChild(umur);

		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Kelamin")));
		row.appendChild(jenisKelamin);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Pasien")));
		row.appendChild(jenisPasien);

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("TTL")));

		Hbox myHbox = new Hbox();
		myHbox.setParent(row);
		myHbox.appendChild(ttl);
		myHbox.appendChild(tglLahir);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Asuransi")));
		row.appendChild(myasuransi);
		myasuransi.setValue(bookingRegistrasi.getAsuransi() == null ? "" : bookingRegistrasi.getAsuransi().getNama());
		myasuransi.setAttribute("asuransi", bookingRegistrasi.getAsuransi());
		myasuransi.setWidth("90%");

		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Catatan")));
		row.appendChild(keterangan);
		keterangan.setWidth("90%");
		keterangan.setRows(2);

		mypasien.setEventListener(perubahanPasienListener);
		myasuransi.setEventListener(populateDataEventListener);
		tanggalBookingRegistrasi.addEventListener("onChange", populateDataEventListener);
		keterangan.addEventListener("onChange", populateDataEventListener);
		return perubahanPasienListener;
	}

	@SuppressWarnings("deprecation")
	public static EventListener initPendaftaran(Rows rows, Label kode, final Pendaftaran pendaftaran,
			final EventListener myEventListener) throws Exception {

		if (pendaftaran != null && pendaftaran.getId() != null) {
			HibernateUtil.currentSession().refresh(pendaftaran);
		}

		final AmbilDataPasienBanbox mypasien = new AmbilDataPasienBanbox();
		// final Label kode = new Label(pendaftaran.getKode());
		final MyDatebox tanggalPendaftaran = new MyDatebox(
				pendaftaran.getTanggalPendaftaran() == null ? new Date() : pendaftaran.getTanggalPendaftaran());

		final Pasien pasien = pendaftaran.getPasien();
		final Label nama = new Label(pasien == null ? "" : pasien.getNama());

		final Label komunitas = new Label(
				pendaftaran.getKomunitass().toString().replaceAll("\\[", "").replaceAll("\\]", ""));
		final Label alamat = new Label(pasien == null ? "" : pasien.getAlamatLengkap());
		final Label umur = new Label(pendaftaran == null ? "" : pendaftaran.getUmur().toString() + " thn");
		final Label jenisKelamin = new Label(
				pasien == null ? "" : pasien.getJenisKelamin() == null ? "" : pasien.getJenisKelamin());
		final Label jenisPasien = new Label(
				pasien == null || pasien.getJenisPasien() == null ? "" : pasien.getJenisPasien().getNama());
		final Label ttl = new Label(
				pasien == null ? "" : (pasien.getTempatLahir() == null ? "" : pasien.getTempatLahir() + " / "));
		final Label tglLahir = new Label(pasien == null ? null : Common.dateFormat2.get().format(pasien.getTanggalLahir()));
		final AmbilDataAsuransiBanbox myasuransi = new AmbilDataAsuransiBanbox();
		final MyTextbox keterangan = new MyTextbox(pendaftaran.getKeterangan());

		final EventListener populateDataEventListener = new EventListener() {

			@SuppressWarnings({ "rawtypes", "unchecked" })
			@Override
			public void onEvent(Event arg0) throws Exception {

				Pasien pasien = (Pasien) mypasien.getAttribute("pasien");
				Asuransi asuransi = (Asuransi) myasuransi.getAttribute("asuransi");

				pendaftaran.setAsuransi(asuransi);
				pendaftaran.setPasien(pasien);
				pendaftaran.setTanggalPendaftaran(tanggalPendaftaran.getValue());
				pendaftaran.setKeterangan(keterangan.getValue());

				Map data = new HashMap();
				data.put("pendaftaran", pendaftaran);
				data.put("pasien", pasien);
				data.put("asuransi", asuransi);
				data.put("tanggalPendaftaran", tanggalPendaftaran.getValue());
				myEventListener.onEvent(new Event("", mypasien, data));
			}
		};

		EventListener perubahanPasienListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (arg0 != null && arg0.getData() != null && arg0.getData() instanceof Pasien) {
					pendaftaran.setPasien((Pasien) arg0.getData());
					pendaftaran.setAsuransi(pendaftaran.getPasien().getAsuransi());
					mypasien.setValue(pendaftaran.getPasien() == null ? "" : pendaftaran.getPasien().getKode());
					mypasien.setAttribute("pasien", pendaftaran.getPasien());
					mypasien.setDisabled(true);
				} else {
					mypasien.setDisabled(false);
				}

				Pasien pasien = (Pasien) mypasien.getAttribute("pasien");
				mypasien.setValue(pasien == null ? "" : pasien.getKode().trim());
				nama.setValue(pasien == null ? "" : pasien.getNama());

				alamat.setValue(pasien == null ? "" : pasien.getAlamatLengkap());

				ttl.setValue(
						pasien == null ? "" : (pasien.getTempatLahir() == null ? "" : pasien.getTempatLahir() + " / "));
				umur.setValue(pasien == null ? "" : pasien.getUmur().toString() + " thn");
				tglLahir.setValue(pasien == null ? null : Common.dateFormat2.get().format(pasien.getTanggalLahir()));
				jenisKelamin.setValue(
						pasien == null ? "" : pasien.getJenisKelamin() == null ? "" : pasien.getJenisKelamin());

				myasuransi
						.setValue(pasien == null || pasien.getAsuransi() == null ? "" : pasien.getAsuransi().getNama());
				myasuransi.setAttribute("asuransi", pasien == null ? null : pasien.getAsuransi());

				jenisPasien.setValue(
						pasien == null || pasien.getJenisPasien() == null ? "" : pasien.getJenisPasien().getNama());

				pendaftaran.setPasien(pasien);
				pendaftaran.setPasienKomunitas(pendaftaran.getPasien());
				komunitas.setValue(pendaftaran.getKomunitass().toString().replaceAll("\\[", "").replaceAll("\\]", ""));

				populateDataEventListener.onEvent(null);
			}
		};

		Row row = new Row();
		row.setAttribute("hide", "no");
		ais.ui.util.ZkCompat.setSpans(row, "4");
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Html("<hr>"));

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode")));
		row.appendChild(kode);

		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Waktu")));
		row.appendChild(tanggalPendaftaran);
		tanggalPendaftaran.setFormat(Common.dateFormat3.get().toPattern());
		tanggalPendaftaran.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Pasien")));
		row.appendChild(mypasien);
		mypasien.setValue(pendaftaran.getPasien() == null ? "" : pendaftaran.getPasien().getKode());
		mypasien.setAttribute("pasien", pendaftaran.getPasien());
		mypasien.setWidth("90%");

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama Pasien")));
		row.appendChild(nama);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Komunitas")));
		row.appendChild(komunitas);

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Alamat")));
		row.appendChild(alamat);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Umur")));
		row.appendChild(umur);

		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Kelamin")));
		row.appendChild(jenisKelamin);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Pasien")));
		row.appendChild(jenisPasien);

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("TTL")));

		Hbox myHbox = new Hbox();
		myHbox.setParent(row);
		myHbox.appendChild(ttl);

		myHbox.appendChild(tglLahir);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Asuransi")));
		row.appendChild(myasuransi);
		myasuransi.setValue(pendaftaran.getAsuransi() == null ? "" : pendaftaran.getAsuransi().getNama());
		myasuransi.setAttribute("asuransi", pendaftaran.getAsuransi());
		myasuransi.setWidth("90%");

		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Catatan")));
		row.appendChild(keterangan);
		keterangan.setWidth("90%");
		keterangan.setRows(2);

		mypasien.setEventListener(perubahanPasienListener);
		myasuransi.setEventListener(populateDataEventListener);
		tanggalPendaftaran.addEventListener("onChange", populateDataEventListener);
		keterangan.addEventListener("onChange", populateDataEventListener);
		return perubahanPasienListener;
	}

	@SuppressWarnings("deprecation")
	public static EventListener initJadwalPemeriksaan(Rows rows, final Pendaftaran pendaftaran, final String jenis,
			final EventListener myEventListener) throws Exception {

		final Label shiftDokter = new Label(
				pendaftaran.getJadwalDokter() == null ? "" : pendaftaran.getJadwalDokter().toString());
		final Label poly = new Label(pendaftaran.getPoly() == null ? "" : pendaftaran.getPoly().getNama());
		final Label dokter = new Label(pendaftaran.getDokter() == null ? "" : pendaftaran.getDokter().getNama());

		final Label nomorAntrian = new Label(
				pendaftaran.getNomorAntrian() == null ? "" : "" + pendaftaran.getNomorAntrian());

		final Label waktuPelayanan = new Label(pendaftaran.getDilayaniTanggal() == null ? ""
				: (Common.dateFormat4.get().format(pendaftaran.getDilayaniTanggal())));

		Row row = new Row();
		row.setAttribute("hide", "no");
		ais.ui.util.ZkCompat.setSpans(row, "4");
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Html("<hr>"));

		row = new Row();
		row.setAttribute("hide", "no");
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jadwal atau Piket")));
		final Button buttonCalendar = new ais.ui.util.MyButtonConfig("Cari dan ambil jadwal");

		final EventListener jadwalPerawatanEventListener = new EventListener() {

			@SuppressWarnings({ "rawtypes", "unchecked" })
			@Override
			public void onEvent(Event arg0) throws Exception {
				Object[] datas = (Object[]) arg0.getData();
				JadwalDokter jdwDokter = (JadwalDokter) datas[0];
				Date dilayaniTanggal = (Date) datas[1];
				Boolean disabled = (Boolean) datas[2];
				Pendaftaran pendaftaran = (Pendaftaran) datas[3];

				buttonCalendar.setDisabled(disabled);

				pendaftaran.setDilayaniTanggal(dilayaniTanggal);
				pendaftaran.setJadwalDokter(jdwDokter);

				if (jdwDokter != null && jdwDokter.getPoly() != null) {
					poly.setValue(jdwDokter.getPoly().getNama());
				} else {
					poly.setValue("");
				}

				if (jdwDokter != null && jdwDokter.getDokter() != null) {
					dokter.setValue(jdwDokter.getDokter().getNama());
				} else {
					dokter.setValue("");
				}

				Integer antrian = generateNomorAntrian(pendaftaran, jdwDokter);
				nomorAntrian.setValue(antrian == null ? "" : antrian.toString());

				waktuPelayanan.setValue(pendaftaran.getDilayaniTanggal() == null ? ""
						: (Common.dateFormat4.get().format(pendaftaran.getDilayaniTanggal())));

				shiftDokter.setValue(
						pendaftaran.getJadwalDokter() == null ? "" : pendaftaran.getJadwalDokter().toString());

				Map data = new HashMap();
				data.put("poly", jdwDokter.getPoly());
				data.put("dokter", jdwDokter.getDokter());
				data.put("jadwalDokter", jdwDokter);
				data.put("antrian", antrian);
				data.put("dilayaniTanggal", pendaftaran.getDilayaniTanggal());
				myEventListener.onEvent(new Event("", shiftDokter, data));
			}
		};

		row.appendChild(buttonCalendar);

		buttonCalendar.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				AmbilJadwalHarian ambilJadwalHarian = new AmbilJadwalHarian(pendaftaran.getTanggalPendaftaran(), jenis,
						pendaftaran, jadwalPerawatanEventListener);
				ambilJadwalHarian.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				ambilJadwalHarian.setHeight("97%");
				ambilJadwalHarian.setWidth("850px");
				ambilJadwalHarian.onModal();
			}
		});

		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Shift Tenaga Medis")));
		row.appendChild(shiftDokter);

		row = new Row();
		row.setAttribute("hide", "no");
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Poli")));
		row.appendChild(poly);

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tenaga Medis")));
		row.appendChild(dokter);

		row = new Row();
		row.setAttribute("hide", "no");
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nomor Antrian")));
		row.appendChild(nomorAntrian);
		nomorAntrian.setStyle("font-size: x-large;");

		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Waktu Pelayanan")));
		row.appendChild(waktuPelayanan);

		return jadwalPerawatanEventListener;

	}

	@SuppressWarnings("deprecation")
	public static void initJadwalPemeriksaan(Rows rows, final BookingRegistrasi bookingRegistrasi, final String jenis,
			final EventListener myEventListener) throws Exception {

		final Label shiftDokter = new Label(
				bookingRegistrasi.getJadwalDokter() == null ? "" : bookingRegistrasi.getJadwalDokter().toString());
		final Label poly = new Label(bookingRegistrasi.getPoly() == null ? "" : bookingRegistrasi.getPoly().getNama());
		final Label dokter = new Label(
				bookingRegistrasi.getDokter() == null ? "" : bookingRegistrasi.getDokter().getNama());

		final Label nomorAntrian = new Label(
				bookingRegistrasi.getNomorAntrian() == null ? "" : "" + bookingRegistrasi.getNomorAntrian());

		final Label waktuPelayanan = new Label(bookingRegistrasi.getDilayaniTanggal() == null ? ""
				: (Common.dateFormat4.get().format(bookingRegistrasi.getDilayaniTanggal())));

		Row row = new Row();
		row.setAttribute("hide", "no");
		ais.ui.util.ZkCompat.setSpans(row, "4");
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Html("<hr>"));

		row = new Row();
		row.setAttribute("hide", "no");
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Booking untuk jadwal")));
		final Button buttonCalendar = new ais.ui.util.MyButtonConfig("Cari dan ambil jadwal");

		final EventListener jadwalPerawatanEventListener = new EventListener() {

			@SuppressWarnings({ "rawtypes", "unchecked" })
			@Override
			public void onEvent(Event arg0) throws Exception {
				Object[] datas = (Object[]) arg0.getData();
				JadwalDokter jdwDokter = (JadwalDokter) datas[0];
				Date dilayaniTanggal = (Date) datas[1];
				bookingRegistrasi.setDilayaniTanggal(dilayaniTanggal);
				bookingRegistrasi.setJadwalDokter(jdwDokter);

				if (jdwDokter != null && jdwDokter.getPoly() != null) {
					poly.setValue(jdwDokter.getPoly().getNama());
				} else {
					poly.setValue("");
				}

				if (jdwDokter != null && jdwDokter.getDokter() != null) {
					dokter.setValue(jdwDokter.getDokter().getNama());
				} else {
					dokter.setValue("");
				}

				Integer antrian = generateNomorAntrian(bookingRegistrasi, jdwDokter);
				nomorAntrian.setValue(antrian == null ? "" : antrian.toString());

				waktuPelayanan.setValue(bookingRegistrasi.getDilayaniTanggal() == null ? ""
						: (Common.dateFormat4.get().format(bookingRegistrasi.getDilayaniTanggal())));

				shiftDokter.setValue(bookingRegistrasi.getJadwalDokter() == null ? ""
						: bookingRegistrasi.getJadwalDokter().toString());

				Map data = new HashMap();
				data.put("poly", jdwDokter.getPoly());
				data.put("dokter", jdwDokter.getDokter());
				data.put("jadwalDokter", jdwDokter);
				data.put("antrian", antrian);
				data.put("dilayaniTanggal", bookingRegistrasi.getDilayaniTanggal());
				myEventListener.onEvent(new Event("", shiftDokter, data));
			}
		};

		row.appendChild(buttonCalendar);

		buttonCalendar.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				AmbilJadwalBulanan ambilJadwalBulanan = new AmbilJadwalBulanan(
						bookingRegistrasi.getTanggalBookingRegistrasi(), jenis, jadwalPerawatanEventListener);
				ambilJadwalBulanan.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				ambilJadwalBulanan.setHeight("97%");
				ambilJadwalBulanan.setWidth("97%");
				ambilJadwalBulanan.setVisible(true);
				ambilJadwalBulanan.onModal();
			}
		});

		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Shift Tenaga Medis")));
		row.appendChild(shiftDokter);

		row = new Row();
		row.setAttribute("hide", "no");
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Poli")));
		row.appendChild(poly);

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tenaga Medis")));
		row.appendChild(dokter);

		row = new Row();
		row.setAttribute("hide", "no");
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nomor Antrian")));
		row.appendChild(nomorAntrian);
		nomorAntrian.setStyle("font-size: x-large;");

		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Waktu Pelayanan")));
		row.appendChild(waktuPelayanan);

	}

	public static Integer generateNomorAntrian(Pendaftaran pendaftaran, JadwalDokter jadwalDokter) {

		if (pendaftaran.getBookingRegistrasi() != null) {
			return pendaftaran.getBookingRegistrasi().getNomorAntrian();
		}

		Integer antrian = pendaftaran.getId() == null ? null : pendaftaran.getNomorAntrian();
		if (antrian == null) {

			if (jadwalDokter != null) {

				Integer antrianBooking = ((Integer) HibernateUtil.currentSession()
						.createCriteria(BookingRegistrasi.class).add(Restrictions.eq("jadwalDokter", jadwalDokter))
						.add(Restrictions.ge("dilayaniTanggal", new Date()))
						.setProjection(Projections.max("nomorAntrian")).uniqueResult());

				antrianBooking = (antrianBooking == null ? 0 : antrianBooking);

				antrian = ((Integer) HibernateUtil.currentSession().createCriteria(Pendaftaran.class)
						.add(Restrictions.eq("jadwalDokter", jadwalDokter))
						.add(Restrictions.eq("dilayaniTanggal", new Date()))
						.setProjection(Projections.max("nomorAntrian")).uniqueResult());

				antrian = (antrian == null ? 1 : antrian + 1);
				antrian += antrianBooking;
			}
		}

		return antrian;
	}

	public static Integer generateNomorAntrian(BookingRegistrasi bookingRegistrasi, JadwalDokter jadwalDokter) {

		Integer antrian = bookingRegistrasi.getId() == null ? null : bookingRegistrasi.getNomorAntrian();
		if (antrian == null) {

			if (jadwalDokter != null) {

				antrian = ((Integer) HibernateUtil.currentSession().createCriteria(BookingRegistrasi.class)
						.add(Restrictions.eq("jadwalDokter", jadwalDokter))
						.add(Restrictions.ge("dilayaniTanggal", new Date()))
						.setProjection(Projections.max("nomorAntrian")).uniqueResult());

				System.out.println("generateNomorAntrian antrian = " + antrian);

				antrian = (antrian == null ? 1 : antrian + 1);
			}
		}

		return antrian;
	}

	public static void riwayatPenyakitPasien(final South south, Pasien pasien) {
		Common.clear(south);
		south.setTitle("Riwayat penyakit pasien");
		ais.ui.util.ZkCompat.setFlex(south, true);

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(south);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Grid grid = new Grid();
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();

		columns.setParent(grid);

		Column column = new Column();
		column.setParent(columns);
		column.setLabel("Tanggal");
		column.setWidth("20%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Riwayat Penyakit");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Cetak");
		column.setWidth("10%");

		Rows rows = new Rows();
		rows.setParent(grid);

		@SuppressWarnings("unchecked")
		List<DiagnosaPenyakit> diagnosaPenyakits = HibernateUtil.currentSession().createCriteria(DiagnosaPenyakit.class)
				.addOrder(Order.desc("tanggal"))
				.add(pasien == null || pasien.getId() == null ? Restrictions.sqlRestriction("false")
						: Restrictions.eq("pasien", pasien))
				.list();

		for (final DiagnosaPenyakit diagnosaPenyakit : diagnosaPenyakits) {

			String penyakit = "";
			if (diagnosaPenyakit.getDiagnosaAwal1() != null) {
				penyakit += penyakit.isEmpty() ? diagnosaPenyakit.getDiagnosaAwal1().toString()
						: " ," + diagnosaPenyakit.getDiagnosaAwal1().toString();
			}
			if (diagnosaPenyakit.getDiagnosaAkhir1() != null) {
				penyakit += penyakit.isEmpty() ? diagnosaPenyakit.getDiagnosaAkhir1().toString()
						: " ," + diagnosaPenyakit.getDiagnosaAkhir1().toString();
			}

			if (diagnosaPenyakit.getDiagnosaAwal2() != null) {
				penyakit += penyakit.isEmpty() ? diagnosaPenyakit.getDiagnosaAwal2().toString()
						: " ," + diagnosaPenyakit.getDiagnosaAwal2().toString();
			}
			if (diagnosaPenyakit.getDiagnosaAkhir2() != null) {
				penyakit += penyakit.isEmpty() ? diagnosaPenyakit.getDiagnosaAkhir2().toString()
						: " ," + diagnosaPenyakit.getDiagnosaAkhir2().toString();
			}

			if (diagnosaPenyakit.getDiagnosaAwal3() != null) {
				penyakit += penyakit.isEmpty() ? diagnosaPenyakit.getDiagnosaAwal3().toString()
						: " ," + diagnosaPenyakit.getDiagnosaAwal3().toString();
			}
			if (diagnosaPenyakit.getDiagnosaAkhir3() != null) {
				penyakit += penyakit.isEmpty() ? diagnosaPenyakit.getDiagnosaAkhir3().toString()
						: " ," + diagnosaPenyakit.getDiagnosaAkhir3().toString();
			}

			if (penyakit.trim().isEmpty()) {
				continue;
			}

			Row row = new Row();
			row.setParent(rows);
			row.appendChild(new Label(Common.dateFormat3.get().format(diagnosaPenyakit.getTanggal())));
			row.appendChild(new Label(penyakit));

			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak Rekam Medis");
			button.setVisible(diagnosaPenyakit.getPendaftaran().getPasien() != null);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					CommonSirs.onCetakHasilDiagnosaPasien(diagnosaPenyakit);
				}

			});
			button.setParent(row);

		}
	}

	/**
	 * Varian <b>riwayat penyakit pasien</b> untuk wadah biasa ({@link org.zkoss.zul.Div}/Vlayout) —
	 * TANPA Borderlayout/South. Tinggi mengikuti KONTEN (tidak menyisakan celah besar seperti South di
	 * dalam Borderlayout yang di-flex). Judul dirender sebagai header biasa. Dipakai form rekam medis
	 * yang memakai tata letak Grid/Rows/Row (bukan Borderlayout). Overload {@code South} lama tetap ada
	 * agar pemanggil lain tidak terpengaruh.
	 */
	public static void riwayatPenyakitPasien(final org.zkoss.zul.Div wadah, Pasien pasien) {
		Common.clear(wadah);
		org.zkoss.zul.Div header = new org.zkoss.zul.Div();
		header.setStyle("background:#233876;color:#fff;font-weight:700;padding:8px 12px;margin-top:8px;border-radius:4px 4px 0 0;");
		header.appendChild(new Label("Riwayat penyakit pasien"));
		header.setParent(wadah);

		Grid grid = new Grid();
		grid.setParent(wadah);
		grid.setWidth("100%");
		// TANPA setHeight("100%") → grid setinggi kontennya (mepet ke atas, tanpa celah).

		Columns columns = new Columns();
		columns.setParent(grid);
		Column column = new Column();
		column.setParent(columns);
		column.setLabel("Tanggal");
		column.setWidth("20%");
		column = new Column();
		column.setParent(columns);
		column.setLabel("Riwayat Penyakit");
		column = new Column();
		column.setParent(columns);
		column.setLabel("Cetak");
		column.setWidth("10%");

		Rows rows = new Rows();
		rows.setParent(grid);

		@SuppressWarnings("unchecked")
		List<DiagnosaPenyakit> diagnosaPenyakits = HibernateUtil.currentSession().createCriteria(DiagnosaPenyakit.class)
				.addOrder(Order.desc("tanggal"))
				.add(pasien == null || pasien.getId() == null ? Restrictions.sqlRestriction("false")
						: Restrictions.eq("pasien", pasien))
				.list();

		for (final DiagnosaPenyakit diagnosaPenyakit : diagnosaPenyakits) {
			String penyakit = "";
			if (diagnosaPenyakit.getDiagnosaAwal1() != null) {
				penyakit += penyakit.isEmpty() ? diagnosaPenyakit.getDiagnosaAwal1().toString()
						: " ," + diagnosaPenyakit.getDiagnosaAwal1().toString();
			}
			if (diagnosaPenyakit.getDiagnosaAkhir1() != null) {
				penyakit += penyakit.isEmpty() ? diagnosaPenyakit.getDiagnosaAkhir1().toString()
						: " ," + diagnosaPenyakit.getDiagnosaAkhir1().toString();
			}
			if (diagnosaPenyakit.getDiagnosaAwal2() != null) {
				penyakit += penyakit.isEmpty() ? diagnosaPenyakit.getDiagnosaAwal2().toString()
						: " ," + diagnosaPenyakit.getDiagnosaAwal2().toString();
			}
			if (diagnosaPenyakit.getDiagnosaAkhir2() != null) {
				penyakit += penyakit.isEmpty() ? diagnosaPenyakit.getDiagnosaAkhir2().toString()
						: " ," + diagnosaPenyakit.getDiagnosaAkhir2().toString();
			}
			if (diagnosaPenyakit.getDiagnosaAwal3() != null) {
				penyakit += penyakit.isEmpty() ? diagnosaPenyakit.getDiagnosaAwal3().toString()
						: " ," + diagnosaPenyakit.getDiagnosaAwal3().toString();
			}
			if (diagnosaPenyakit.getDiagnosaAkhir3() != null) {
				penyakit += penyakit.isEmpty() ? diagnosaPenyakit.getDiagnosaAkhir3().toString()
						: " ," + diagnosaPenyakit.getDiagnosaAkhir3().toString();
			}
			if (penyakit.trim().isEmpty()) {
				continue;
			}
			Row row = new Row();
			row.setParent(rows);
			row.appendChild(new Label(Common.dateFormat3.get().format(diagnosaPenyakit.getTanggal())));
			row.appendChild(new Label(penyakit));
			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak Rekam Medis");
			button.setVisible(diagnosaPenyakit.getPendaftaran().getPasien() != null);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					CommonSirs.onCetakHasilDiagnosaPasien(diagnosaPenyakit);
				}
			});
			button.setParent(row);
		}
	}

	public static void dokterDanBidanPemeriksa(final East east, final Set<Dokter> setDokters,
			final EventListener eventListener) {
		Common.clear(east);
		east.setTitle("Dokter, Bidan, dan Perawat lain yang memeriksa");
		ais.ui.util.ZkCompat.setFlex(east, true);

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(east);

		North north = new North();
		north.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("25px");
		toolbar.setParent(north);
		Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("Ambil Dokter, Bidan, dan Perawat lain",
				"/img/user_male_add.png");
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				AmbilDataDokterBanyak ambilDataDokterBanyak = new AmbilDataDokterBanyak(
						new ArrayList<Dokter>(setDokters));
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataDokterBanyak);
				ambilDataDokterBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Dokter> dokters = (List<Dokter>) arg0.getData();
						setDokters.addAll(dokters);
						dokterDanBidanPemeriksa(east, setDokters, eventListener);
					}
				});
				ambilDataDokterBanyak.setWidth("95%");
				ambilDataDokterBanyak.setHeight("97%");
				ambilDataDokterBanyak.setVisible(true);
				ambilDataDokterBanyak.onModal();
			}

		});
		button.setParent(toolbar);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Grid grid = new Grid();
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();

		columns.setParent(grid);

		Column column = new Column();
		column.setParent(columns);
		column.setLabel("Kode");
		column.setWidth("20%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("50%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Kategori");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("8%");

		Rows rows = new Rows();
		rows.setParent(grid);

		for (final Dokter dokter : setDokters) {
			Row row = new Row();
			row.setParent(rows);

			row.appendChild(new Label(dokter.getKode()));
			row.appendChild(new Label(dokter.getNama()));
			row.appendChild(new Label(dokter.getKategori()));

			button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					for (Dokter t : setDokters) {
						if (t.getId().equals(dokter.getId())) {
							setDokters.remove(t);
							break;
						}
					}
					eventListener.onEvent(new Event("", arg0.getTarget(), setDokters));
					dokterDanBidanPemeriksa(east, setDokters, eventListener);
				}
			});
			button.setParent(row);
		}
	}

	@SuppressWarnings("unchecked")
	public static void displayDetailPaket(East east, Set<Tindakan> pakets) {

		Common.clear(east);
		east.setTitle("Paket obat, perawatan, dan peralatan medis");
		List<PaketPerawatanDetail> perawatanDetails = HibernateUtil.currentSession()
				.createCriteria(PaketPerawatanDetail.class).add(pakets.isEmpty() ? Restrictions.sqlRestriction("false")
						: Restrictions.in("paketPerawatan", pakets))
				.addOrder(Order.asc("paketPerawatan")).list();

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(east);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Grid grid = new Grid();
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();

		columns.setParent(grid);

		Column column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("40px");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Paket");
		column.setWidth("20%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Obat, Perawatan, dan Alat Medis");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Jenis");
		column.setWidth("20%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Qty");
		column.setWidth("20%");

		Rows rows = new Rows();
		rows.setParent(grid);

		for (PaketPerawatanDetail paketPerawatanDetail : perawatanDetails) {
			Row row = new Row();
			row.setParent(rows);

			if (paketPerawatanDetail.getItem() != null) {
				row.appendChild(new Label(""));
				row.appendChild(new Label(paketPerawatanDetail.getPaketPerawatan().getKode()));
				row.appendChild(new Label(paketPerawatanDetail.getItem().getNama()));
				row.appendChild(new Label(ais.common.Common.getBahasaConfig("Obat")));
				row.appendChild(new Label(Common.numberFormat.get().format(paketPerawatanDetail.getJumlah()) + " "
						+ (paketPerawatanDetail.getItem().getSatuanItem() == null ? ""
								: paketPerawatanDetail.getItem().getSatuanItem().getNama())));
			} else if (paketPerawatanDetail.getRacikan() != null) {

				new RacikanDetailAction(paketPerawatanDetail.getRacikan(), false).setParent(row);
				row.appendChild(new Label(paketPerawatanDetail.getPaketPerawatan().getKode()));
				new Label(paketPerawatanDetail.getRacikan().getKode()).setParent(row);
				new Label(paketPerawatanDetail.getRacikan().getNama()).setParent(row);
				new Label(Common.numberFormat.get().format(paketPerawatanDetail.getJumlah()) + " racik").setParent(row);
			}

			else if (paketPerawatanDetail.getTindakan() != null) {
				row.appendChild(new Label(""));
				row.appendChild(new Label(paketPerawatanDetail.getPaketPerawatan().getKode()));
				row.appendChild(new Label(paketPerawatanDetail.getTindakan().getNama()));
				row.appendChild(new Label(ais.common.Common.getBahasaConfig("Perawatan")));
				row.appendChild(new Label(Common.numberFormat.get().format(paketPerawatanDetail.getJumlah())));
			}

			else if (paketPerawatanDetail.getAlatMedis() != null) {
				row.appendChild(new Label(""));
				row.appendChild(new Label(paketPerawatanDetail.getPaketPerawatan().getKode()));
				row.appendChild(new Label(paketPerawatanDetail.getAlatMedis().getNama()));
				row.appendChild(new Label(ais.common.Common.getBahasaConfig("Alkes")));
				row.appendChild(new Label(Common.numberFormat.get().format(paketPerawatanDetail.getJumlah()) + " "
						+ (paketPerawatanDetail.getAlatMedis().getPer())));
			}
		}
	}

	/**
	 * Varian detail paket untuk wadah biasa ({@link org.zkoss.zul.Div}/Vlayout) — TANPA Borderlayout/East,
	 * tinggi mengikuti KONTEN. Judul jadi header biasa. Overload {@code East} lama tetap ada.
	 */
	@SuppressWarnings("unchecked")
	public static void displayDetailPaket(final org.zkoss.zul.Div wadah, Set<Tindakan> pakets) {
		Common.clear(wadah);
		org.zkoss.zul.Div header = new org.zkoss.zul.Div();
		header.setStyle("background:#233876;color:#fff;font-weight:700;padding:8px 12px;margin-top:8px;border-radius:4px 4px 0 0;");
		header.appendChild(new Label("Paket obat, perawatan, dan peralatan medis"));
		header.setParent(wadah);

		List<PaketPerawatanDetail> perawatanDetails = HibernateUtil.currentSession()
				.createCriteria(PaketPerawatanDetail.class).add(pakets.isEmpty() ? Restrictions.sqlRestriction("false")
						: Restrictions.in("paketPerawatan", pakets))
				.addOrder(Order.asc("paketPerawatan")).list();

		Grid grid = new Grid();
		grid.setParent(wadah);
		grid.setWidth("100%");
		Columns columns = new Columns();
		columns.setParent(grid);
		Column column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("40px");
		column = new Column();
		column.setParent(columns);
		column.setLabel("Paket");
		column.setWidth("20%");
		column = new Column();
		column.setParent(columns);
		column.setLabel("Obat, Perawatan, dan Alat Medis");
		column = new Column();
		column.setParent(columns);
		column.setLabel("Jenis");
		column.setWidth("20%");
		column = new Column();
		column.setParent(columns);
		column.setLabel("Qty");
		column.setWidth("20%");

		Rows rows = new Rows();
		rows.setParent(grid);

		for (PaketPerawatanDetail paketPerawatanDetail : perawatanDetails) {
			Row row = new Row();
			row.setParent(rows);
			if (paketPerawatanDetail.getItem() != null) {
				row.appendChild(new Label(""));
				row.appendChild(new Label(paketPerawatanDetail.getPaketPerawatan().getKode()));
				row.appendChild(new Label(paketPerawatanDetail.getItem().getNama()));
				row.appendChild(new Label(ais.common.Common.getBahasaConfig("Obat")));
				row.appendChild(new Label(Common.numberFormat.get().format(paketPerawatanDetail.getJumlah()) + " "
						+ (paketPerawatanDetail.getItem().getSatuanItem() == null ? ""
								: paketPerawatanDetail.getItem().getSatuanItem().getNama())));
			} else if (paketPerawatanDetail.getRacikan() != null) {
				new RacikanDetailAction(paketPerawatanDetail.getRacikan(), false).setParent(row);
				row.appendChild(new Label(paketPerawatanDetail.getPaketPerawatan().getKode()));
				new Label(paketPerawatanDetail.getRacikan().getKode()).setParent(row);
				new Label(paketPerawatanDetail.getRacikan().getNama()).setParent(row);
				new Label(Common.numberFormat.get().format(paketPerawatanDetail.getJumlah()) + " racik").setParent(row);
			} else if (paketPerawatanDetail.getTindakan() != null) {
				row.appendChild(new Label(""));
				row.appendChild(new Label(paketPerawatanDetail.getPaketPerawatan().getKode()));
				row.appendChild(new Label(paketPerawatanDetail.getTindakan().getNama()));
				row.appendChild(new Label(ais.common.Common.getBahasaConfig("Perawatan")));
				row.appendChild(new Label(Common.numberFormat.get().format(paketPerawatanDetail.getJumlah())));
			} else if (paketPerawatanDetail.getAlatMedis() != null) {
				row.appendChild(new Label(""));
				row.appendChild(new Label(paketPerawatanDetail.getPaketPerawatan().getKode()));
				row.appendChild(new Label(paketPerawatanDetail.getAlatMedis().getNama()));
				row.appendChild(new Label(ais.common.Common.getBahasaConfig("Alkes")));
				row.appendChild(new Label(Common.numberFormat.get().format(paketPerawatanDetail.getJumlah()) + " "
						+ (paketPerawatanDetail.getAlatMedis().getPer())));
			}
		}
	}

	@SuppressWarnings("unchecked")
	public static boolean validasiTransaksiDetailPaket(Set<Tindakan> pakets, final TransaksiMedis transaksi,
			String jenisPaket) throws Exception {
		Session session = HibernateUtil.currentSession();
		List<PaketPerawatanDetail> perawatanDetails = session.createCriteria(PaketPerawatanDetail.class).add(
				pakets.isEmpty() ? Restrictions.sqlRestriction("false") : Restrictions.in("paketPerawatan", pakets))
				.addOrder(Order.asc("paketPerawatan")).list();

		List<PaketPerawatanDetailPasien> perawatanDetailPasiens = new ArrayList<PaketPerawatanDetailPasien>();
		for (final PaketPerawatanDetail paketPerawatanDetail : perawatanDetails) {
			PaketPerawatanDetailPasien paketPerawatanDetailPasien = (PaketPerawatanDetailPasien) session
					.createCriteria(PaketPerawatanDetailPasien.class)
					.add(Restrictions.eq("paketPerawatanDetail", paketPerawatanDetail))
					.add(Restrictions.eq("pendaftaran", transaksi.getPendaftaran())).setMaxResults(1).uniqueResult();

			if ((paketPerawatanDetail.getItem() != null || paketPerawatanDetail.getRacikan() != null)
					&& jenisPaket.equals(PaketPerawatanDetail.PAKET_OBAT)) {
				if (paketPerawatanDetailPasien == null) {
					MyMessageboxConfig.show("Mohon maaf, Obat atau Racikan \"" + paketPerawatanDetail.getItem().toString()
							+ "\" dengan jumlah " + (Common.numberFormat.get().format(paketPerawatanDetail.getJumlah())) + " "
							+ (paketPerawatanDetail.getItem().getSatuanItem() == null ? ""
									: paketPerawatanDetail.getItem().getSatuanItem().getNama())
							+ " harus divalidasi terlebih dahulu. Langkah yang dapat dilakukan: (1) lakukan validasi terhadap item tersebut; (2) kemudian ulangi kembali proses ini.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				} else {
					paketPerawatanDetailPasien.setTransaksi(transaksi);
					session.update(paketPerawatanDetailPasien);
					perawatanDetailPasiens.add(paketPerawatanDetailPasien);
				}
			} else if (paketPerawatanDetail.getTindakan() != null
					&& jenisPaket.equals(PaketPerawatanDetail.PAKET_TINDAKAN)) {
				if (paketPerawatanDetailPasien == null) {
					MyMessageboxConfig.show("Mohon maaf, Perawatan atau Tindakan \"" + paketPerawatanDetail.getTindakan().toString()
							+ "\" dengan jumlah " + (Common.numberFormat.get().format(paketPerawatanDetail.getJumlah()))
							+ " harus divalidasi terlebih dahulu. Langkah yang dapat dilakukan: (1) lakukan validasi terhadap item tersebut; (2) kemudian ulangi kembali proses ini.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				} else {
					paketPerawatanDetailPasien.setTransaksi(transaksi);
					session.update(paketPerawatanDetailPasien);
					perawatanDetailPasiens.add(paketPerawatanDetailPasien);
				}
			} else if (paketPerawatanDetail.getAlatMedis() != null
					&& jenisPaket.equals(PaketPerawatanDetail.PAKET_ALAT_MEDIS)) {
				if (paketPerawatanDetailPasien == null) {
					MyMessageboxConfig.show(
							"Mohon maaf, Alat Medis atau Alat Kesehatan \"" + paketPerawatanDetail.getAlatMedis().toString()
									+ "\" dengan jumlah "
									+ (Common.numberFormat.get().format(paketPerawatanDetail.getJumlah())) + " "
									+ (paketPerawatanDetail.getAlatMedis().getPer() == null ? ""
											: paketPerawatanDetail.getAlatMedis().getPer())
									+ " harus divalidasi terlebih dahulu. Langkah yang dapat dilakukan: (1) lakukan validasi terhadap item tersebut; (2) kemudian ulangi kembali proses ini.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				} else {
					paketPerawatanDetailPasien.setTransaksi(transaksi);
					session.update(paketPerawatanDetailPasien);
					perawatanDetailPasiens.add(paketPerawatanDetailPasien);
				}
			}

		}

		transaksi.setValidasi(true);
		Common.refreshUpdate(session, (transaksi));

		session.createSQLQuery(
				"delete from sirs.detail_transaksi_pasien where racikan_detail in (select id from sirs.racikan_detail where racikan in (select id from sirs.racikan where transaksi_detail in (select id from sirs.transaksi_medis_detail where transaksi = "
						+ transaksi.getId() + ")));")
				.executeUpdate();
		session.createSQLQuery(
				"delete from sirs.transaksi_medis_detail where transaksi_detail in (select id from sirs.transaksi_medis_detail where transaksi = "
						+ transaksi.getId() + ");")
				.executeUpdate();

		session.createSQLQuery(
				"delete from sirs.detail_transaksi_layanan where transaksi_detail in (select id from sirs.transaksi_medis_detail where transaksi = "
						+ transaksi.getId() + ");")
				.executeUpdate();

		session.createSQLQuery(
				"delete from sirs.detail_transaksi_pasien where paket_perawatan_detail_pasien in (select id from sirs.paket_perawatan_detail_pasien where transaksi = "
						+ transaksi.getId() + ");")
				.executeUpdate();
		session.createSQLQuery(
				"delete from sirs.detail_transaksi_layanan where paket_perawatan_detail_pasien in (select id from sirs.paket_perawatan_detail_pasien where transaksi = "
						+ transaksi.getId() + ");")
				.executeUpdate();

		for (PaketPerawatanDetailPasien paketPerawatanDetailPasien : perawatanDetailPasiens) {

			if (paketPerawatanDetailPasien.getPaketPerawatanDetail().getRacikan() != null) {

				List<RacikanDetail> racikanDetails = session.createCriteria(RacikanDetail.class)
						.add(Restrictions.eq("racikan", paketPerawatanDetailPasien.getRacikanDigantiDengan())).list();
				for (RacikanDetail racikanDetail : racikanDetails) {
					DetailTransaksiPasien detailTransaksi = new DetailTransaksiPasien();

					detailTransaksi.setQtyBonus(0.0);
					detailTransaksi.setPasien(transaksi.getPasien());
					detailTransaksi.setRacikanDetail(racikanDetail);
					detailTransaksi.setPaketPerawatanDetailPasien(paketPerawatanDetailPasien);
					detailTransaksi.setItem(racikanDetail.getItem());
					detailTransaksi.setAmount(0.0);
					detailTransaksi.setKeterangan("Paket \""
							+ paketPerawatanDetailPasien.getPaketPerawatanDetail().getPaketPerawatan().toString()
							+ "\" dengan cara meracik " + racikanDetail.getItem());
					detailTransaksi.setKodeTransaksi(ConstantValues.apotikJual);
					detailTransaksi.setLokasi(transaksi.getLokasi());
					detailTransaksi.setQty(paketPerawatanDetailPasien.getJumlah());
					detailTransaksi.setTanggal(new Date());

					detailTransaksi.setDiskonPersen(0.0);
					detailTransaksi.setPajakPersen(0.0);

					session.save(detailTransaksi);
				}

			} else if (paketPerawatanDetailPasien.getItemDigantiDengan() != null) {
				DetailTransaksiPasien detailTransaksi = new DetailTransaksiPasien();

				detailTransaksi.setQtyBonus(0.0);
				detailTransaksi.setPasien(transaksi.getPasien());
				detailTransaksi.setPaketPerawatanDetailPasien(paketPerawatanDetailPasien);
				detailTransaksi.setItem(paketPerawatanDetailPasien.getItemDigantiDengan());
				detailTransaksi.setAmount(0.0);
				detailTransaksi.setKeterangan(
						"Paket \"" + paketPerawatanDetailPasien.getPaketPerawatanDetail().getPaketPerawatan().toString()
								+ "\": " + paketPerawatanDetailPasien.getItemDigantiDengan().getNama());
				detailTransaksi.setKodeTransaksi(ConstantValues.apotikJual);
				detailTransaksi.setLokasi(transaksi.getLokasi());
				detailTransaksi.setQty(paketPerawatanDetailPasien.getJumlah());
				detailTransaksi.setTanggal(new Date());

				detailTransaksi.setDiskonPersen(0.0);
				detailTransaksi.setPajakPersen(0.0);

				session.save(detailTransaksi);

			} else if (paketPerawatanDetailPasien.getTindakanDigantiDengan() != null) {
				DetailTransaksiLayanan detailTransaksiLayanan = new DetailTransaksiLayanan();

				detailTransaksiLayanan.setQtyBonus(0.0);
				detailTransaksiLayanan.setPasien(transaksi.getPasien());
				detailTransaksiLayanan.setPaketPerawatanDetailPasien(paketPerawatanDetailPasien);
				detailTransaksiLayanan.setTindakan(paketPerawatanDetailPasien.getTindakanDigantiDengan());
				detailTransaksiLayanan.setAmount(0.0);
				detailTransaksiLayanan.setKeterangan(
						"Paket \"" + paketPerawatanDetailPasien.getPaketPerawatanDetail().getPaketPerawatan().toString()
								+ "\": " + paketPerawatanDetailPasien.getTindakanDigantiDengan().getNama());
				detailTransaksiLayanan.setKodeTransaksi(ConstantValues.apotikJual);
				detailTransaksiLayanan.setLokasi(transaksi.getLokasi());
				detailTransaksiLayanan.setQty(paketPerawatanDetailPasien.getJumlah());
				detailTransaksiLayanan.setTanggal(new Date());

				detailTransaksiLayanan.setDiskonPersen(0.0);
				detailTransaksiLayanan.setPajakPersen(0.0);

				session.save(detailTransaksiLayanan);

			} else if (paketPerawatanDetailPasien.getAlatMedisDigantiDengan() != null) {
				DetailTransaksiLayanan detailTransaksiLayanan = new DetailTransaksiLayanan();

				detailTransaksiLayanan.setQtyBonus(0.0);
				detailTransaksiLayanan.setPasien(transaksi.getPasien());
				detailTransaksiLayanan.setPaketPerawatanDetailPasien(paketPerawatanDetailPasien);
				detailTransaksiLayanan.setAlatMedis(paketPerawatanDetailPasien.getAlatMedisDigantiDengan());
				detailTransaksiLayanan.setAmount(0.0);
				detailTransaksiLayanan.setKeterangan(
						"Paket \"" + paketPerawatanDetailPasien.getPaketPerawatanDetail().getPaketPerawatan().toString()
								+ "\": " + paketPerawatanDetailPasien.getAlatMedisDigantiDengan().getNama());
				detailTransaksiLayanan.setKodeTransaksi(ConstantValues.apotikJual);
				detailTransaksiLayanan.setLokasi(transaksi.getLokasi());
				detailTransaksiLayanan.setQty(paketPerawatanDetailPasien.getJumlah());
				detailTransaksiLayanan.setTanggal(new Date());

				detailTransaksiLayanan.setDiskonPersen(0.0);
				detailTransaksiLayanan.setPajakPersen(0.0);

				session.save(detailTransaksiLayanan);

			}

		}

		return true;
	}

	@SuppressWarnings("unchecked")
	public static void transaksiDetailPaket(East east, Set<Tindakan> pakets, final Pendaftaran pendaftaran,
			String jenisPaket) {

		Common.clear(east);
		Session session = HibernateUtil.currentSession();
		List<PaketPerawatanDetail> perawatanDetails = session.createCriteria(PaketPerawatanDetail.class).add(
				pakets.isEmpty() ? Restrictions.sqlRestriction("false") : Restrictions.in("paketPerawatan", pakets))
				.addOrder(Order.asc("paketPerawatan")).list();

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(east);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Grid grid = new Grid();
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();

		columns.setParent(grid);

		Column column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("40px");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Paket");
		column.setWidth("20%");

		column = new Column();
		column.setParent(columns);
		if (jenisPaket.equals(PaketPerawatanDetail.PAKET_OBAT)) {
			column.setLabel("Obat");
			east.setTitle("Obat-obat-an");
		} else if (jenisPaket.equals(PaketPerawatanDetail.PAKET_TINDAKAN)) {
			column.setLabel("Perawatan");
			east.setTitle("Tindakan dan perawatan");
		} else if (jenisPaket.equals(PaketPerawatanDetail.PAKET_ALAT_MEDIS)) {
			column.setLabel("Alat Medis");
			east.setTitle("Peralatan-peralatan medis");
		}

		column = new Column();
		column.setParent(columns);
		column.setLabel("Jenis");
		column.setWidth("20%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Qty");
		column.setWidth("20%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Valid");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Ganti");
		column.setWidth("10%");

		Rows rows = new Rows();
		rows.setParent(grid);

		for (final PaketPerawatanDetail paketPerawatanDetail : perawatanDetails) {

			PaketPerawatanDetailPasien paketPerawatanDetailPasien = (PaketPerawatanDetailPasien) session
					.createCriteria(PaketPerawatanDetailPasien.class)
					.add(Restrictions.eq("paketPerawatanDetail", paketPerawatanDetail))
					.add(Restrictions.eq("pendaftaran", pendaftaran)).setMaxResults(1).uniqueResult();

			if ((paketPerawatanDetail.getItem() != null || paketPerawatanDetail.getRacikan() != null)
					&& jenisPaket.equals(PaketPerawatanDetail.PAKET_OBAT)) {
				final RacikanDetailAction racikanDetailAction;
				Row row = new Row();
				row.setParent(rows);
				if (paketPerawatanDetail.getRacikan() != null) {
					(racikanDetailAction = new RacikanDetailAction(paketPerawatanDetail.getRacikan(), false))
							.setParent(row);
				} else {
					racikanDetailAction = null;
					row.appendChild(new Label(""));
				}
				row.appendChild(new Label(paketPerawatanDetail.getPaketPerawatan().getKode()));
				final Label nama;
				row.appendChild(nama = new Label(paketPerawatanDetailPasien != null
						? (paketPerawatanDetailPasien.getItemDigantiDengan() == null
								? paketPerawatanDetailPasien.getRacikanDigantiDengan().getNama()
								: paketPerawatanDetailPasien.getItemDigantiDengan().getNama())
						: (paketPerawatanDetail.getItem() == null ? paketPerawatanDetail.getRacikan().getNama()
								: paketPerawatanDetail.getItem().getNama())));
				row.appendChild(new Label("Obat/Racikan"));
				row.appendChild(new Label(Common.numberFormat.get().format(paketPerawatanDetail.getJumlah()) + " "
						+ (paketPerawatanDetail.getItem() == null ? ("racik")
								: paketPerawatanDetail.getItem().getSatuanItem() == null ? ""
										: paketPerawatanDetail.getItem().getSatuanItem().getNama())));

				final Checkbox checkbox = new Checkbox();
				checkbox.setParent(row);
				checkbox.setChecked(paketPerawatanDetailPasien != null);
				checkbox.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						PaketPerawatanDetailPasien paketPerawatanDetailPasien = (PaketPerawatanDetailPasien) session
								.createCriteria(PaketPerawatanDetailPasien.class)
								.add(Restrictions.eq("paketPerawatanDetail", paketPerawatanDetail))
								.add(Restrictions.eq("pendaftaran", pendaftaran)).setMaxResults(1).uniqueResult();
						if (checkbox.isChecked()) {
							if (paketPerawatanDetailPasien == null) {
								paketPerawatanDetailPasien = new PaketPerawatanDetailPasien();
								paketPerawatanDetailPasien.setPaketPerawatanDetail(paketPerawatanDetail);
								paketPerawatanDetailPasien.setItemDigantiDengan(paketPerawatanDetail.getItem());
								paketPerawatanDetailPasien.setRacikanDigantiDengan(paketPerawatanDetail.getRacikan());
								paketPerawatanDetailPasien.setJumlah(paketPerawatanDetail.getJumlah());
								paketPerawatanDetailPasien.setPendaftaran(pendaftaran);
								session.save(paketPerawatanDetailPasien);
								nama.setValue(paketPerawatanDetailPasien.getItemDigantiDengan() == null
										? paketPerawatanDetailPasien.getRacikanDigantiDengan().getNama()
										: paketPerawatanDetailPasien.getItemDigantiDengan().getNama());
							}
						} else {
							if (paketPerawatanDetailPasien != null) {
								session.delete(paketPerawatanDetailPasien);
							}
						}
					}
				});

				Button ganti = new ais.ui.util.MyButtonConfig("Ganti");
				ganti.setWidth("95%");
				ganti.setParent(row);
				ganti.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						Session session = HibernateUtil.currentSession();
						PaketPerawatanDetailPasien paketPerawatanDetailPasien = (PaketPerawatanDetailPasien) session
								.createCriteria(PaketPerawatanDetailPasien.class)
								.add(Restrictions.eq("paketPerawatanDetail", paketPerawatanDetail))
								.add(Restrictions.eq("pendaftaran", pendaftaran)).setMaxResults(1).uniqueResult();
						if (paketPerawatanDetailPasien == null) {
							paketPerawatanDetailPasien = new PaketPerawatanDetailPasien();
							paketPerawatanDetailPasien.setPaketPerawatanDetail(paketPerawatanDetail);
							paketPerawatanDetailPasien.setItemDigantiDengan(paketPerawatanDetail.getItem());
							paketPerawatanDetailPasien.setRacikanDigantiDengan(paketPerawatanDetail.getRacikan());
							paketPerawatanDetailPasien.setJumlah(paketPerawatanDetail.getJumlah());
							paketPerawatanDetailPasien.setPendaftaran(pendaftaran);
							session.save(paketPerawatanDetailPasien);
						}

						final Window window = new Window("Ganti dengan", "none", true);
						ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
						window.setHeight("80px");
						window.setWidth("400px");

						if (paketPerawatanDetail.getItem() != null) {

							final AmbilDataItemMedisBanbox banbox = new AmbilDataItemMedisBanbox();
							banbox.setWidth("95%");
							banbox.setParent(window);
							banbox.setValue(paketPerawatanDetailPasien.getItemDigantiDengan().toString());
							banbox.setAttribute("item", paketPerawatanDetailPasien.getItemDigantiDengan());
							banbox.setEventListener(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {

									ItemMedis item = (ItemMedis) banbox.getAttribute("item");
									if (item != null) {
										Session session = HibernateUtil.currentSession();
										PaketPerawatanDetailPasien paketPerawatanDetailPasien = (PaketPerawatanDetailPasien) session
												.createCriteria(PaketPerawatanDetailPasien.class)
												.add(Restrictions.eq("paketPerawatanDetail", paketPerawatanDetail))
												.add(Restrictions.eq("pendaftaran", pendaftaran)).setMaxResults(1)
												.uniqueResult();
										paketPerawatanDetailPasien.setItemDigantiDengan(item);
										paketPerawatanDetailPasien.setRacikanDigantiDengan(null);
										session.update(paketPerawatanDetailPasien);
										window.detach();

										checkbox.setChecked(true);
										checkbox.setDisabled(true);

										nama.setValue(paketPerawatanDetailPasien.getItemDigantiDengan().getNama());
									}
								}
							});
						} else {
							final AmbilDataRacikanBanbox banbox = new AmbilDataRacikanBanbox();
							banbox.setWidth("95%");
							banbox.setParent(window);
							banbox.setValue(paketPerawatanDetailPasien.getRacikanDigantiDengan().toString());
							banbox.setAttribute("racikan", paketPerawatanDetailPasien.getRacikanDigantiDengan());
							banbox.setEventListener(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {

									Racikan racikan = (Racikan) banbox.getAttribute("racikan");
									if (racikan != null) {
										Session session = HibernateUtil.currentSession();
										PaketPerawatanDetailPasien paketPerawatanDetailPasien = (PaketPerawatanDetailPasien) session
												.createCriteria(PaketPerawatanDetailPasien.class)
												.add(Restrictions.eq("paketPerawatanDetail", paketPerawatanDetail))
												.add(Restrictions.eq("pendaftaran", pendaftaran)).setMaxResults(1)
												.uniqueResult();
										paketPerawatanDetailPasien.setRacikanDigantiDengan(racikan);
										paketPerawatanDetailPasien.setItemDigantiDengan(null);
										session.update(paketPerawatanDetailPasien);
										window.detach();

										checkbox.setChecked(true);
										checkbox.setDisabled(true);

										nama.setValue(paketPerawatanDetailPasien.getRacikanDigantiDengan().getNama());

										racikanDetailAction.reInitRacikan(racikan, false);
									}
								}
							});
						}

						window.onModal();
					}
				});

			}

			else if (paketPerawatanDetail.getTindakan() != null
					&& jenisPaket.equals(PaketPerawatanDetail.PAKET_TINDAKAN)) {
				Row row = new Row();
				row.setParent(rows);
				row.appendChild(new Label(""));
				row.appendChild(new Label(paketPerawatanDetail.getPaketPerawatan().getKode()));
				final Label nama;
				row.appendChild(nama = new Label(paketPerawatanDetailPasien != null
						? paketPerawatanDetailPasien.getTindakanDigantiDengan().getNama()
						: paketPerawatanDetail.getTindakan().getNama()));
				row.appendChild(new Label(ais.common.Common.getBahasaConfig("Perawatan")));
				row.appendChild(new Label(Common.numberFormat.get().format(paketPerawatanDetail.getJumlah())));

				final Checkbox checkbox = new Checkbox();
				checkbox.setParent(row);
				checkbox.setChecked(paketPerawatanDetailPasien != null);
				checkbox.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						PaketPerawatanDetailPasien paketPerawatanDetailPasien = (PaketPerawatanDetailPasien) session
								.createCriteria(PaketPerawatanDetailPasien.class)
								.add(Restrictions.eq("paketPerawatanDetail", paketPerawatanDetail))
								.add(Restrictions.eq("pendaftaran", pendaftaran)).setMaxResults(1).uniqueResult();
						if (checkbox.isChecked()) {
							if (paketPerawatanDetailPasien == null) {
								paketPerawatanDetailPasien = new PaketPerawatanDetailPasien();
								paketPerawatanDetailPasien.setPaketPerawatanDetail(paketPerawatanDetail);
								paketPerawatanDetailPasien.setTindakanDigantiDengan(paketPerawatanDetail.getTindakan());
								paketPerawatanDetailPasien.setJumlah(paketPerawatanDetail.getJumlah());
								paketPerawatanDetailPasien.setPendaftaran(pendaftaran);
								session.save(paketPerawatanDetailPasien);
								nama.setValue(paketPerawatanDetailPasien.getTindakanDigantiDengan().getNama());
							}
						} else {
							if (paketPerawatanDetailPasien != null) {
								session.delete(paketPerawatanDetailPasien);
							}
						}
					}
				});

				Button ganti = new ais.ui.util.MyButtonConfig("Ganti");
				ganti.setWidth("95%");
				ganti.setParent(row);
				ganti.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						Session session = HibernateUtil.currentSession();
						PaketPerawatanDetailPasien paketPerawatanDetailPasien = (PaketPerawatanDetailPasien) session
								.createCriteria(PaketPerawatanDetailPasien.class)
								.add(Restrictions.eq("paketPerawatanDetail", paketPerawatanDetail))
								.add(Restrictions.eq("pendaftaran", pendaftaran)).setMaxResults(1).uniqueResult();
						if (paketPerawatanDetailPasien == null) {
							paketPerawatanDetailPasien = new PaketPerawatanDetailPasien();
							paketPerawatanDetailPasien.setPaketPerawatanDetail(paketPerawatanDetail);
							paketPerawatanDetailPasien.setTindakanDigantiDengan(paketPerawatanDetail.getTindakan());
							paketPerawatanDetailPasien.setJumlah(paketPerawatanDetail.getJumlah());
							paketPerawatanDetailPasien.setPendaftaran(pendaftaran);
							session.save(paketPerawatanDetailPasien);
						}

						final Window window = new Window("Ganti dengan", "none", true);
						ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
						window.setHeight("80px");
						window.setWidth("400px");

						final AmbilDataTindakanBanbox banbox = new AmbilDataTindakanBanbox();
						banbox.setWidth("95%");
						banbox.setParent(window);
						banbox.setValue(paketPerawatanDetailPasien.getTindakanDigantiDengan().toString());
						banbox.setAttribute("tindakan", paketPerawatanDetailPasien.getTindakanDigantiDengan());
						banbox.setEventListener(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								Tindakan tindakan = (Tindakan) banbox.getAttribute("tindakan");
								if (tindakan != null) {
									Session session = HibernateUtil.currentSession();
									PaketPerawatanDetailPasien paketPerawatanDetailPasien = (PaketPerawatanDetailPasien) session
											.createCriteria(PaketPerawatanDetailPasien.class)
											.add(Restrictions.eq("paketPerawatanDetail", paketPerawatanDetail))
											.add(Restrictions.eq("pendaftaran", pendaftaran)).setMaxResults(1)
											.uniqueResult();
									paketPerawatanDetailPasien.setTindakanDigantiDengan(tindakan);
									session.update(paketPerawatanDetailPasien);
									window.detach();

									checkbox.setChecked(true);
									checkbox.setDisabled(true);

									nama.setValue(paketPerawatanDetailPasien.getTindakanDigantiDengan().getNama());
								}
							}
						});

						window.onModal();
					}
				});

			}

			else if (paketPerawatanDetail.getAlatMedis() != null
					&& jenisPaket.equals(PaketPerawatanDetail.PAKET_ALAT_MEDIS)) {
				Row row = new Row();
				row.setParent(rows);
				row.appendChild(new Label(""));
				row.appendChild(new Label(paketPerawatanDetail.getPaketPerawatan().getKode()));
				final Label nama;
				row.appendChild(nama = new Label(paketPerawatanDetailPasien != null
						? paketPerawatanDetailPasien.getAlatMedisDigantiDengan().getNama()
						: paketPerawatanDetail.getAlatMedis().getNama()));
				row.appendChild(new Label(ais.common.Common.getBahasaConfig("Alkes")));
				row.appendChild(new Label(Common.numberFormat.get().format(paketPerawatanDetail.getJumlah()) + " "
						+ (paketPerawatanDetail.getAlatMedis().getPer())));

				final Checkbox checkbox = new Checkbox();
				checkbox.setParent(row);
				checkbox.setChecked(paketPerawatanDetailPasien != null);
				checkbox.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						PaketPerawatanDetailPasien paketPerawatanDetailPasien = (PaketPerawatanDetailPasien) session
								.createCriteria(PaketPerawatanDetailPasien.class)
								.add(Restrictions.eq("paketPerawatanDetail", paketPerawatanDetail))
								.add(Restrictions.eq("pendaftaran", pendaftaran)).setMaxResults(1).uniqueResult();
						if (checkbox.isChecked()) {
							if (paketPerawatanDetailPasien == null) {
								paketPerawatanDetailPasien = new PaketPerawatanDetailPasien();
								paketPerawatanDetailPasien.setPaketPerawatanDetail(paketPerawatanDetail);
								paketPerawatanDetailPasien
										.setAlatMedisDigantiDengan(paketPerawatanDetail.getAlatMedis());
								paketPerawatanDetailPasien.setJumlah(paketPerawatanDetail.getJumlah());
								paketPerawatanDetailPasien.setPendaftaran(pendaftaran);
								session.save(paketPerawatanDetailPasien);
								nama.setValue(paketPerawatanDetailPasien.getAlatMedisDigantiDengan().getNama());
							}
						} else {
							if (paketPerawatanDetailPasien != null) {
								session.delete(paketPerawatanDetailPasien);
							}
						}
					}
				});

				Button ganti = new ais.ui.util.MyButtonConfig("Ganti");
				ganti.setWidth("95%");
				ganti.setParent(row);
				ganti.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						Session session = HibernateUtil.currentSession();
						PaketPerawatanDetailPasien paketPerawatanDetailPasien = (PaketPerawatanDetailPasien) session
								.createCriteria(PaketPerawatanDetailPasien.class)
								.add(Restrictions.eq("paketPerawatanDetail", paketPerawatanDetail))
								.add(Restrictions.eq("pendaftaran", pendaftaran)).setMaxResults(1).uniqueResult();
						if (paketPerawatanDetailPasien == null) {
							paketPerawatanDetailPasien = new PaketPerawatanDetailPasien();
							paketPerawatanDetailPasien.setPaketPerawatanDetail(paketPerawatanDetail);
							paketPerawatanDetailPasien.setAlatMedisDigantiDengan(paketPerawatanDetail.getAlatMedis());
							paketPerawatanDetailPasien.setJumlah(paketPerawatanDetail.getJumlah());
							paketPerawatanDetailPasien.setPendaftaran(pendaftaran);
							session.save(paketPerawatanDetailPasien);
						}

						final Window window = new Window("Ganti dengan", "none", true);
						ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
						window.setHeight("80px");
						window.setWidth("400px");

						final AmbilDataAlatMedisBanbox banbox = new AmbilDataAlatMedisBanbox();
						banbox.setWidth("95%");
						banbox.setParent(window);
						banbox.setValue(paketPerawatanDetailPasien.getAlatMedisDigantiDengan().toString());
						banbox.setAttribute("alatMedis", paketPerawatanDetailPasien.getAlatMedisDigantiDengan());
						banbox.setEventListener(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								AlatMedis alatMedis = (AlatMedis) banbox.getAttribute("alatMedis");
								if (alatMedis != null) {
									Session session = HibernateUtil.currentSession();
									PaketPerawatanDetailPasien paketPerawatanDetailPasien = (PaketPerawatanDetailPasien) session
											.createCriteria(PaketPerawatanDetailPasien.class)
											.add(Restrictions.eq("paketPerawatanDetail", paketPerawatanDetail))
											.add(Restrictions.eq("pendaftaran", pendaftaran)).setMaxResults(1)
											.uniqueResult();
									paketPerawatanDetailPasien.setAlatMedisDigantiDengan(alatMedis);
									session.update(paketPerawatanDetailPasien);
									window.detach();

									checkbox.setChecked(true);
									checkbox.setDisabled(true);

									nama.setValue(paketPerawatanDetailPasien.getAlatMedisDigantiDengan().getNama());
								}
							}
						});

						window.onModal();
					}
				});
			}
		}
	}

	@SuppressWarnings("unchecked")
	public static void setDetailBiayaPaket(DetailTransaksiPasien detailTransaksi, ItemMedis item,
			KelasPerawatan kelasPerawatan, Integer nilaiPengganti) {
		Session session = HibernateUtil.currentSession();

		if (item != null) {
			HargaJualItem hargaJualItem = CommonTarifItem.getHargaJualItem(item, kelasPerawatan,
					detailTransaksi.getTransaksiDetail().getTransaksi().getPendaftaran().getDokter(),
					detailTransaksi.getTransaksiDetail().getTransaksi().getPendaftaran().getAsuransi(),
					detailTransaksi.getTransaksiDetail().getTransaksi().getPendaftaran().getKomunitass(),
					detailTransaksi.getTransaksiDetail().getTransaksi().getPendaftaran().getPasien());

			List<Biaya> biayas = session.createCriteria(Biaya.class)

					.add(Restrictions.isNull("detailTransaksi")).add(Restrictions.isNull("detailTransaksiLayanan"))
					.add(Restrictions.ne("jumlah", 0.0)).add(Restrictions.eq("hargaJualItem", hargaJualItem))
					.createAlias("jenisBiaya", "jenisBiaya").add(Restrictions.eq("jenisBiaya.aktif", true)).list();
			Double nilaiBiaya = 0.0;
			for (Biaya biaya : biayas) {
				nilaiBiaya += biaya.getJumlah();
			}
			hargaJualItem.setHargaJual(nilaiBiaya);

			for (Biaya biaya : biayas) {
				Biaya newBiaya = (Biaya) session.createCriteria(Biaya.class)
						.add(nilaiPengganti > 0 ? Restrictions.gt("jumlah", 0.0) : Restrictions.lt("jumlah", 0.0))
						.add(Restrictions.eq("jenisBiaya", biaya.getJenisBiaya()))
						.add(Restrictions.eq("hargaJualItem", hargaJualItem))
						.add(Restrictions.eq("detailTransaksi", detailTransaksi))

						.setMaxResults(1).uniqueResult();
				if (newBiaya == null) {
					newBiaya = (Biaya) biaya.clone();
					newBiaya.setId(null);
				}
				newBiaya.setJumlah(nilaiPengganti.doubleValue() * Math.abs(biaya.getJumlah()));
				newBiaya.setDetailTransaksi(detailTransaksi);
				session.saveOrUpdate(newBiaya);
			}
		}
	}

	public static DetailTransaksiLayanan setDetailBiaya(DetailTransaksiLayanan detailTransaksiLayanan,
			KelasPerawatan kelasPerawatan) {
		return setDetailBiaya(detailTransaksiLayanan, kelasPerawatan, HibernateUtil.currentSession());
	}

	@SuppressWarnings("unchecked")
	public static DetailTransaksiLayanan setDetailBiaya(DetailTransaksiLayanan detailTransaksiLayanan,
			KelasPerawatan kelasPerawatan, Session session) {

		Tindakan tindakan = detailTransaksiLayanan.getTindakan();
		AlatMedis alatMedis = detailTransaksiLayanan.getAlatMedis();

		if (tindakan != null) {

			TransaksiMedisDetail transaksiMedisDetail = detailTransaksiLayanan.getTransaksiDetail();
			TransaksiMedis transaksiMedis = transaksiMedisDetail == null ? null : transaksiMedisDetail.getTransaksi();

			BiayaTindakanPerKelas biayaTindakanPerKelas = CommonTarifTindakan.getBiayaTindakanPerKelas(tindakan,
					kelasPerawatan, transaksiMedisDetail == null ? null : transaksiMedisDetail.getDokter(),
					transaksiMedis == null || transaksiMedis.getPendaftaran() == null ? null
							: transaksiMedis.getPendaftaran().getAsuransi(),
					transaksiMedis == null || transaksiMedis.getPendaftaran() == null ? null
							: transaksiMedis.getPendaftaran().getKomunitass(),
					transaksiMedis == null ? null : transaksiMedis.getPasien());

			List<Biaya> biayas = session.createCriteria(Biaya.class)

					.add(Restrictions.isNull("detailTransaksiLayanan")).add(Restrictions.ne("jumlah", 0.0))
					.add(Restrictions.isNull("detailTransaksi"))

					.add(Restrictions.eq("biayaTindakanPerKelas", biayaTindakanPerKelas))
					.createAlias("jenisBiaya", "jenisBiaya").add(Restrictions.eq("jenisBiaya.aktif", true)).list();

			Double nilaiBiaya = 0.0;
			for (Biaya biaya : biayas) {
				nilaiBiaya += biaya.getJumlah();
			}
			biayaTindakanPerKelas.setBiaya(nilaiBiaya);

			detailTransaksiLayanan.setAmount(nilaiBiaya);

			detailTransaksiLayanan.setKeterangan(
					detailTransaksiLayanan.getKeterangan() + (" (qty: " + detailTransaksiLayanan.getQty() + " "));
			session.saveOrUpdate(detailTransaksiLayanan);

			for (Biaya biaya : biayas) {
				Biaya newBiaya = (Biaya) session.createCriteria(Biaya.class)
						.add(Restrictions.eq("jenisBiaya", biaya.getJenisBiaya()))
						.add(Restrictions.eq("biayaTindakanPerKelas", biayaTindakanPerKelas))
						.add(Restrictions.eq("detailTransaksiLayanan", detailTransaksiLayanan))

						.setMaxResults(1).uniqueResult();
				if (newBiaya == null) {
					newBiaya = (Biaya) biaya.clone();
					newBiaya.setId(null);
				}

				newBiaya.setJumlah((-detailTransaksiLayanan.getKodeTransaksi().getJenis())
						* detailTransaksiLayanan.getQty() * biaya.getJumlah());
				newBiaya.setDetailTransaksiLayanan(detailTransaksiLayanan);
				session.saveOrUpdate(newBiaya);
			}

			TransaksiMedisDetail transaksiDetail = detailTransaksiLayanan.getTransaksiDetail();
			if (transaksiDetail != null) {
				for (Diskon diskon : transaksiDetail.getDiskons()) {
					Biaya newBiaya = (Biaya) session.createCriteria(Biaya.class).add(Restrictions.eq("diskon", diskon))
							.add(Restrictions.eq("biayaTindakanPerKelas", biayaTindakanPerKelas))
							.add(Restrictions.eq("detailTransaksiLayanan", detailTransaksiLayanan)).setMaxResults(1)
							.uniqueResult();
					if (newBiaya == null) {
						newBiaya = new Biaya();
					}
					newBiaya.setBiayaTindakanPerKelas(biayaTindakanPerKelas);
					newBiaya.setDiskon(diskon);

					Double nilai = detailTransaksiLayanan.getAmount() * (diskon.getJumlah() / 100.0);

					newBiaya.setJumlah((-detailTransaksiLayanan.getKodeTransaksi().getJenis())
							* (-Math.abs(detailTransaksiLayanan.getQty() * nilai)));
					newBiaya.setDetailTransaksiLayanan(detailTransaksiLayanan);
					session.saveOrUpdate(newBiaya);
				}

				for (PajakMedis pajak : transaksiDetail.getPajaks()) {
					Biaya newBiaya = (Biaya) session.createCriteria(Biaya.class).add(Restrictions.eq("pajak", pajak))
							.add(Restrictions.eq("biayaTindakanPerKelas", biayaTindakanPerKelas))
							.add(Restrictions.eq("detailTransaksiLayanan", detailTransaksiLayanan)).setMaxResults(1)
							.uniqueResult();
					if (newBiaya == null) {
						newBiaya = new Biaya();
					}
					newBiaya.setBiayaTindakanPerKelas(biayaTindakanPerKelas);
					newBiaya.setPajak(pajak);
					Double nilai = detailTransaksiLayanan.getAmount() * (pajak.getJumlah() / 100.0);
					newBiaya.setJumlah((-detailTransaksiLayanan.getKodeTransaksi().getJenis())
							* detailTransaksiLayanan.getQty() * nilai);
					newBiaya.setDetailTransaksiLayanan(detailTransaksiLayanan);
					session.saveOrUpdate(newBiaya);
				}
			}

		} else if (alatMedis != null) {
			BiayaAlatMedisPerKelas biayaAlatMedisPerKelas = CommonTarifAlatMedis.getBiayaAlatMedisPerKelas(alatMedis,
					kelasPerawatan, detailTransaksiLayanan.getTransaksiDetail().getDokter(),
					detailTransaksiLayanan.getTransaksiDetail().getTransaksi().getPendaftaran().getAsuransi(),
					detailTransaksiLayanan.getTransaksiDetail().getTransaksi().getPendaftaran().getKomunitass(),
					detailTransaksiLayanan.getTransaksiDetail().getTransaksi().getPasien());

			List<Biaya> biayas = session.createCriteria(Biaya.class)

					.add(Restrictions.isNull("detailTransaksiLayanan")).add(Restrictions.ne("jumlah", 0.0))
					.add(Restrictions.isNull("detailTransaksi"))

					.add(Restrictions.eq("biayaAlatMedisPerKelas", biayaAlatMedisPerKelas))
					.createAlias("jenisBiaya", "jenisBiaya").add(Restrictions.eq("jenisBiaya.aktif", true)).list();
			Double nilaiBiaya = 0.0;
			for (Biaya biaya : biayas) {
				nilaiBiaya += biaya.getJumlah();
			}
			biayaAlatMedisPerKelas.setBiaya(nilaiBiaya);
			detailTransaksiLayanan.setAmount(nilaiBiaya);
			detailTransaksiLayanan.setKeterangan(detailTransaksiLayanan.getKeterangan()
					+ (" (qty: " + detailTransaksiLayanan.getQty() + " " + (alatMedis.getPer()) + ") "));

			session.saveOrUpdate(detailTransaksiLayanan);

			for (Biaya biaya : biayas) {
				Biaya newBiaya = (Biaya) session.createCriteria(Biaya.class)
						.add(Restrictions.eq("jenisBiaya", biaya.getJenisBiaya()))
						.add(Restrictions.eq("biayaAlatMedisPerKelas", biayaAlatMedisPerKelas))
						.add(Restrictions.eq("detailTransaksiLayanan", detailTransaksiLayanan)).setMaxResults(1)
						.uniqueResult();
				if (newBiaya == null) {
					newBiaya = (Biaya) biaya.clone();
					newBiaya.setId(null);
				}
				newBiaya.setJumlah((-detailTransaksiLayanan.getKodeTransaksi().getJenis())
						* detailTransaksiLayanan.getQty() * biaya.getJumlah());
				newBiaya.setDetailTransaksiLayanan(detailTransaksiLayanan);
				session.saveOrUpdate(newBiaya);
			}

			TransaksiMedisDetail transaksiDetail = detailTransaksiLayanan.getTransaksiDetail();
			if (transaksiDetail != null) {
				for (Diskon diskon : transaksiDetail.getDiskons()) {
					Biaya newBiaya = (Biaya) session.createCriteria(Biaya.class).add(Restrictions.eq("diskon", diskon))
							.add(Restrictions.eq("biayaAlatMedisPerKelas", biayaAlatMedisPerKelas))
							.add(Restrictions.eq("detailTransaksiLayanan", detailTransaksiLayanan)).setMaxResults(1)
							.uniqueResult();
					if (newBiaya == null) {
						newBiaya = new Biaya();
					}
					newBiaya.setBiayaAlatMedisPerKelas(biayaAlatMedisPerKelas);
					newBiaya.setDiskon(diskon);
					Double nilai = detailTransaksiLayanan.getAmount() * (diskon.getJumlah() / 100.0);
					newBiaya.setJumlah((-detailTransaksiLayanan.getKodeTransaksi().getJenis())
							* (-Math.abs(detailTransaksiLayanan.getQty() * nilai)));
					newBiaya.setDetailTransaksiLayanan(detailTransaksiLayanan);
					session.saveOrUpdate(newBiaya);
				}

				for (PajakMedis pajak : transaksiDetail.getPajaks()) {
					Biaya newBiaya = (Biaya) session.createCriteria(Biaya.class).add(Restrictions.eq("pajak", pajak))
							.add(Restrictions.eq("biayaAlatMedisPerKelas", biayaAlatMedisPerKelas))
							.add(Restrictions.eq("detailTransaksiLayanan", detailTransaksiLayanan)).setMaxResults(1)
							.uniqueResult();
					if (newBiaya == null) {
						newBiaya = new Biaya();
					}
					newBiaya.setBiayaAlatMedisPerKelas(biayaAlatMedisPerKelas);
					newBiaya.setPajak(pajak);
					Double nilai = detailTransaksiLayanan.getAmount() * (pajak.getJumlah() / 100.0);
					newBiaya.setJumlah((-detailTransaksiLayanan.getKodeTransaksi().getJenis())
							* detailTransaksiLayanan.getQty() * nilai);
					newBiaya.setDetailTransaksiLayanan(detailTransaksiLayanan);
					session.saveOrUpdate(newBiaya);
				}
			}
		}

		return detailTransaksiLayanan;
	}

	public static DetailTransaksiPasien setDetailBiaya(DetailTransaksiPasien detailTransaksi,
			KelasPerawatan kelasPerawatan) {
		return setDetailBiaya(detailTransaksi, kelasPerawatan, HibernateUtil.currentSession());
	}

	@SuppressWarnings("unchecked")
	public static DetailTransaksiPasien setDetailBiaya(DetailTransaksiPasien detailTransaksi,
			KelasPerawatan kelasPerawatan, Session session) {

		ItemMedis item = detailTransaksi.getItem();

		if (item != null) {
			HargaJualItem hargaJualItem = CommonTarifItem.getHargaJualItem(item, kelasPerawatan,
					detailTransaksi.getTransaksiDetail().getTransaksi().getPendaftaran().getDokter(),
					detailTransaksi.getTransaksiDetail().getTransaksi().getPendaftaran().getAsuransi(),
					detailTransaksi.getTransaksiDetail().getTransaksi().getPendaftaran().getKomunitass(),
					detailTransaksi.getTransaksiDetail().getTransaksi().getPendaftaran().getPasien());

			List<Biaya> biayas = session.createCriteria(Biaya.class).add(Restrictions.isNull("detailTransaksi"))
					.add(Restrictions.isNull("detailTransaksiLayanan")).add(Restrictions.ne("jumlah", 0.0))

					.add(Restrictions.eq("hargaJualItem", hargaJualItem)).createAlias("jenisBiaya", "jenisBiaya")
					.add(Restrictions.eq("jenisBiaya.aktif", true)).list();
			Double nilaiBiaya = 0.0;
			for (Biaya biaya : biayas) {
				nilaiBiaya += biaya.getJumlah();
			}
			hargaJualItem.setHargaJual(nilaiBiaya);

			detailTransaksi.setKeterangan(detailTransaksi.getKeterangan() + (" (qty: " + detailTransaksi.getQty() + " "
					+ (item.getSatuanItem() == null ? "" : item.getSatuanItem().getNama()) + ") "));
			detailTransaksi.setAmount(nilaiBiaya);
			System.out.println("detailTransaksi = " + detailTransaksi.getKeterangan());
			session.saveOrUpdate(detailTransaksi);

			for (Biaya biaya : biayas) {
				Biaya newBiaya = (Biaya) session.createCriteria(Biaya.class)
						.add(Restrictions.eq("jenisBiaya", biaya.getJenisBiaya()))
						.add(Restrictions.eq("hargaJualItem", hargaJualItem))
						.add(Restrictions.eq("detailTransaksi", detailTransaksi))

						.setMaxResults(1).uniqueResult();
				if (newBiaya == null) {
					newBiaya = (Biaya) biaya.clone();
					newBiaya.setId(null);
				}
				newBiaya.setJumlah((-detailTransaksi.getKodeTransaksi().getJenis()) * detailTransaksi.getQty()
						* biaya.getJumlah());
				newBiaya.setDetailTransaksi(detailTransaksi);
				session.saveOrUpdate(newBiaya);
			}

			TransaksiMedisDetail transaksiDetail = detailTransaksi.getTransaksiDetail();
			if (transaksiDetail != null) {
				for (Diskon diskon : transaksiDetail.getDiskons()) {
					Biaya newBiaya = (Biaya) session.createCriteria(Biaya.class).add(Restrictions.eq("diskon", diskon))
							.add(Restrictions.eq("hargaJualItem", hargaJualItem))
							.add(Restrictions.eq("detailTransaksi", detailTransaksi)).setMaxResults(1).uniqueResult();
					if (newBiaya == null) {
						newBiaya = new Biaya();
					}
					newBiaya.setHargaJualItem(hargaJualItem);
					newBiaya.setDiskon(diskon);
					Double nilai = detailTransaksi.getAmount() * (diskon.getJumlah() / 100.0);
					newBiaya.setJumlah((-detailTransaksi.getKodeTransaksi().getJenis())
							* (-Math.abs(detailTransaksi.getQty() * nilai)));
					newBiaya.setDetailTransaksi(detailTransaksi);
					session.saveOrUpdate(newBiaya);
				}

				for (PajakMedis pajak : transaksiDetail.getPajaks()) {
					Biaya newBiaya = (Biaya) session.createCriteria(Biaya.class).add(Restrictions.eq("pajak", pajak))
							.add(Restrictions.eq("hargaJualItem", hargaJualItem))
							.add(Restrictions.eq("detailTransaksi", detailTransaksi)).setMaxResults(1).uniqueResult();
					if (newBiaya == null) {
						newBiaya = new Biaya();
					}
					newBiaya.setHargaJualItem(hargaJualItem);
					newBiaya.setPajak(pajak);
					Double nilai = detailTransaksi.getAmount() * (pajak.getJumlah() / 100.0);
					newBiaya.setJumlah(
							(-detailTransaksi.getKodeTransaksi().getJenis()) * detailTransaksi.getQty() * nilai);
					newBiaya.setDetailTransaksi(detailTransaksi);
					session.saveOrUpdate(newBiaya);
				}
			}

			RacikanDetail racikanDetail = detailTransaksi.getRacikanDetail();
			if (racikanDetail != null) {
				for (Diskon diskon : racikanDetail.getDiskons()) {
					Biaya newBiaya = (Biaya) session.createCriteria(Biaya.class).add(Restrictions.eq("diskon", diskon))
							.add(Restrictions.eq("hargaJualItem", hargaJualItem))
							.add(Restrictions.eq("detailTransaksi", detailTransaksi)).setMaxResults(1).uniqueResult();
					if (newBiaya == null) {
						newBiaya = new Biaya();
					}
					newBiaya.setHargaJualItem(hargaJualItem);
					newBiaya.setDiskon(diskon);
					Double nilai = detailTransaksi.getAmount() * (diskon.getJumlah() / 100.0);
					newBiaya.setJumlah((-detailTransaksi.getKodeTransaksi().getJenis()) * (-Math.abs(nilai)));
					newBiaya.setDetailTransaksi(detailTransaksi);
					session.saveOrUpdate(newBiaya);
				}

				for (PajakMedis pajak : racikanDetail.getPajaks()) {
					Biaya newBiaya = (Biaya) session.createCriteria(Biaya.class).add(Restrictions.eq("pajak", pajak))
							.add(Restrictions.eq("hargaJualItem", hargaJualItem))
							.add(Restrictions.eq("detailTransaksi", detailTransaksi)).setMaxResults(1).uniqueResult();
					if (newBiaya == null) {
						newBiaya = new Biaya();
					}
					newBiaya.setHargaJualItem(hargaJualItem);
					newBiaya.setPajak(pajak);
					Double nilai = detailTransaksi.getAmount() * (pajak.getJumlah() / 100.0);
					newBiaya.setJumlah((-detailTransaksi.getKodeTransaksi().getJenis()) * nilai);
					newBiaya.setDetailTransaksi(detailTransaksi);
					session.saveOrUpdate(newBiaya);
				}
			}
		}

		return detailTransaksi;
	}

	@SuppressWarnings("unchecked")
	public static void setDetailBiayaPaket(DetailTransaksiLayanan detailTransaksiLayanan, Tindakan tindakan,
			KelasPerawatan kelasPerawatan, Integer nilaiPengganti) {

		Session session = HibernateUtil.currentSession();

		if (tindakan != null) {

			TarifKhususPunyaTindakan tarifKhususPunyaTindakan = null;
			if (detailTransaksiLayanan.getTransaksiDetail() != null
					&& detailTransaksiLayanan.getTransaksiDetail().getDokter() != null) {
				tarifKhususPunyaTindakan = (TarifKhususPunyaTindakan) session
						.createCriteria(TarifKhususPunyaTindakan.class)
						.add(Restrictions.eq("dokter", detailTransaksiLayanan.getTransaksiDetail().getDokter()))
						.add(Restrictions.eq("tindakan", tindakan)).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setMaxResults(1)
						.uniqueResult();
			}

			BiayaTindakanPerKelas biayaTindakanPerKelas = CommonTarifTindakan.getBiayaTindakanPerKelas(tindakan,
					kelasPerawatan, tarifKhususPunyaTindakan);

			List<Biaya> biayas = session.createCriteria(Biaya.class)

					.add(Restrictions.isNull("detailTransaksiLayanan")).add(Restrictions.ne("jumlah", 0.0))
					.add(Restrictions.isNull("detailTransaksi"))

					.add(Restrictions.eq("biayaTindakanPerKelas", biayaTindakanPerKelas))
					.createAlias("jenisBiaya", "jenisBiaya").add(Restrictions.eq("jenisBiaya.aktif", true)).list();

			Double nilaiBiaya = 0.0;
			for (Biaya biaya : biayas) {
				nilaiBiaya += biaya.getJumlah();
			}
			biayaTindakanPerKelas.setBiaya(nilaiBiaya);
			session.saveOrUpdate(detailTransaksiLayanan);

			for (Biaya biaya : biayas) {
				Biaya newBiaya = (Biaya) session.createCriteria(Biaya.class)
						.add(nilaiPengganti > 0 ? Restrictions.gt("jumlah", 0.0) : Restrictions.lt("jumlah", 0.0))
						.add(Restrictions.eq("jenisBiaya", biaya.getJenisBiaya()))
						.add(Restrictions.eq("biayaTindakanPerKelas", biayaTindakanPerKelas))
						.add(Restrictions.eq("detailTransaksiLayanan", detailTransaksiLayanan))

						.setMaxResults(1).uniqueResult();
				if (newBiaya == null) {
					newBiaya = (Biaya) biaya.clone();
					newBiaya.setId(null);
				}
				newBiaya.setJumlah(nilaiPengganti.doubleValue() * Math.abs(biaya.getJumlah()));
				newBiaya.setDetailTransaksiLayanan(detailTransaksiLayanan);
				session.saveOrUpdate(newBiaya);
			}

		}

	}

	@SuppressWarnings("unchecked")
	public static void setDetailBiayaPaket(DetailTransaksiLayanan detailTransaksiLayanan, AlatMedis alatMedis,
			KelasPerawatan kelasPerawatan, Integer nilaiPengganti) {
		Session session = HibernateUtil.currentSession();
		if (alatMedis != null) {

			BiayaAlatMedisPerKelas biayaAlatMedisPerKelas = CommonTarifAlatMedis.getBiayaAlatMedisPerKelas(alatMedis,
					kelasPerawatan,
					detailTransaksiLayanan.getTransaksiDetail().getTransaksi().getPendaftaran().getDokter(),
					detailTransaksiLayanan.getTransaksiDetail().getTransaksi().getPendaftaran().getAsuransi(),
					detailTransaksiLayanan.getTransaksiDetail().getTransaksi().getPendaftaran().getKomunitass(),
					detailTransaksiLayanan.getTransaksiDetail().getTransaksi().getPendaftaran().getPasien());

			List<Biaya> biayas = session.createCriteria(Biaya.class)

					.add(Restrictions.isNull("detailTransaksiLayanan")).add(Restrictions.ne("jumlah", 0.0))
					.add(Restrictions.isNull("detailTransaksi"))

					.add(Restrictions.eq("biayaAlatMedisPerKelas", biayaAlatMedisPerKelas))
					.createAlias("jenisBiaya", "jenisBiaya").add(Restrictions.eq("jenisBiaya.aktif", true)).list();
			Double nilaiBiaya = 0.0;
			for (Biaya biaya : biayas) {
				nilaiBiaya += biaya.getJumlah();
			}
			biayaAlatMedisPerKelas.setBiaya(nilaiBiaya);
			session.saveOrUpdate(detailTransaksiLayanan);

			for (Biaya biaya : biayas) {
				Biaya newBiaya = (Biaya) session.createCriteria(Biaya.class)
						.add(nilaiPengganti > 0 ? Restrictions.gt("jumlah", 0.0) : Restrictions.lt("jumlah", 0.0))
						.add(Restrictions.eq("jenisBiaya", biaya.getJenisBiaya()))
						.add(Restrictions.eq("biayaAlatMedisPerKelas", biayaAlatMedisPerKelas))
						.add(Restrictions.eq("detailTransaksiLayanan", detailTransaksiLayanan)).setMaxResults(1)
						.uniqueResult();
				if (newBiaya == null) {
					newBiaya = (Biaya) biaya.clone();
					newBiaya.setId(null);
				}
				newBiaya.setJumlah(nilaiPengganti.doubleValue() * Math.abs(biaya.getJumlah()));
				newBiaya.setDetailTransaksiLayanan(detailTransaksiLayanan);
				session.saveOrUpdate(newBiaya);

			}

		}
	}

	@SuppressWarnings("unchecked")
	public static void transaksiDetailPaketFinal(East east, Set<Tindakan> pakets, final Pendaftaran pendaftaran) {

		Common.clear(east);
		Session session = HibernateUtil.currentSession();
		List<PaketPerawatanDetail> perawatanDetails = session.createCriteria(PaketPerawatanDetail.class).add(
				pakets.isEmpty() ? Restrictions.sqlRestriction("false") : Restrictions.in("paketPerawatan", pakets))
				.addOrder(Order.asc("paketPerawatan")).list();

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(east);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Grid grid = new Grid();
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();

		columns.setParent(grid);

		Column column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("40px");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Paket");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);

		column.setLabel("Obat, perawatan, atau alat medis");
		east.setTitle("Obat-obat-an, Tindakan dan perawatan, Peralatan-peralatan medis");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Jenis");
		column.setWidth("15%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Qty");
		column.setWidth("15%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Valid");
		column.setWidth("5%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Ganti");
		column.setWidth("5%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Biaya Ganti");
		column.setWidth("15%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Jenis Biaya");
		column.setWidth("15%");

		Rows rows = new Rows();
		rows.setParent(grid);

		for (final PaketPerawatanDetail paketPerawatanDetail : perawatanDetails) {

			PaketPerawatanDetailPasien paketPerawatanDetailPasien = (PaketPerawatanDetailPasien) session
					.createCriteria(PaketPerawatanDetailPasien.class)
					.add(Restrictions.eq("paketPerawatanDetail", paketPerawatanDetail))
					.add(Restrictions.eq("pendaftaran", pendaftaran)).setMaxResults(1).uniqueResult();

			Row row = new Row();
			boolean adaGanti = false;

			if ((paketPerawatanDetail.getItem() != null || paketPerawatanDetail.getRacikan() != null)) {
				row.setParent(rows);
				if (paketPerawatanDetail.getRacikan() != null) {
					(new RacikanDetailAction(paketPerawatanDetail.getRacikan(), false)).setParent(row);
				} else {
					row.appendChild(new Label(""));
				}
				row.appendChild(new Label(paketPerawatanDetail.getPaketPerawatan().getKode()));
				row.appendChild(new Label(paketPerawatanDetailPasien != null
						? (paketPerawatanDetailPasien.getItemDigantiDengan() == null
								? paketPerawatanDetailPasien.getRacikanDigantiDengan().getNama()
								: paketPerawatanDetailPasien.getItemDigantiDengan().getNama())
						: (paketPerawatanDetail.getItem() == null ? paketPerawatanDetail.getRacikan().getNama()
								: paketPerawatanDetail.getItem().getNama())));
				row.appendChild(new Label("Obat/Racikan"));
				row.appendChild(new Label(Common.numberFormat.get().format(paketPerawatanDetail.getJumlah()) + " "
						+ (paketPerawatanDetail.getItem() == null ? ("racik")
								: paketPerawatanDetail.getItem().getSatuanItem() == null ? ""
										: paketPerawatanDetail.getItem().getSatuanItem().getNama())));

				row.appendChild(new Label(
						paketPerawatanDetailPasien != null && paketPerawatanDetailPasien.getTransaksi() != null ? "Ya"
								: "Tidak"));

				if (paketPerawatanDetailPasien != null && paketPerawatanDetailPasien.getItemDigantiDengan() != null) {
					adaGanti = !paketPerawatanDetailPasien.getItemDigantiDengan().getId()
							.equals(paketPerawatanDetailPasien.getPaketPerawatanDetail().getItem().getId());
				}

				if (paketPerawatanDetailPasien != null
						&& paketPerawatanDetailPasien.getRacikanDigantiDengan() != null) {
					adaGanti = !paketPerawatanDetailPasien.getRacikanDigantiDengan().getId()
							.equals(paketPerawatanDetailPasien.getPaketPerawatanDetail().getRacikan().getId());
				}

				row.appendChild(new Label(adaGanti ? "Ya" : "Tidak"));

			}

			else if (paketPerawatanDetail.getTindakan() != null) {

				row.setParent(rows);
				row.appendChild(new Label(""));
				row.appendChild(new Label(paketPerawatanDetail.getPaketPerawatan().getKode()));
				row.appendChild(new Label(paketPerawatanDetailPasien != null
						? paketPerawatanDetailPasien.getTindakanDigantiDengan().getNama()
						: paketPerawatanDetail.getTindakan().getNama()));
				row.appendChild(new Label(ais.common.Common.getBahasaConfig("Perawatan")));
				row.appendChild(new Label(Common.numberFormat.get().format(paketPerawatanDetail.getJumlah())));

				row.appendChild(new Label(
						paketPerawatanDetailPasien != null && paketPerawatanDetailPasien.getTransaksi() != null ? "Ya"
								: "Tidak"));

				if (paketPerawatanDetailPasien != null
						&& paketPerawatanDetailPasien.getTindakanDigantiDengan() != null) {
					adaGanti = !paketPerawatanDetailPasien.getTindakanDigantiDengan().getId()
							.equals(paketPerawatanDetailPasien.getPaketPerawatanDetail().getTindakan().getId());
				}

				row.appendChild(new Label(adaGanti ? "Ya" : "Tidak"));
			}

			else if (paketPerawatanDetail.getAlatMedis() != null) {

				row.setParent(rows);
				row.appendChild(new Label(""));
				row.appendChild(new Label(paketPerawatanDetail.getPaketPerawatan().getKode()));
				row.appendChild(new Label(paketPerawatanDetailPasien != null
						? paketPerawatanDetailPasien.getAlatMedisDigantiDengan().getNama()
						: paketPerawatanDetail.getAlatMedis().getNama()));
				row.appendChild(new Label(ais.common.Common.getBahasaConfig("Alkes")));
				row.appendChild(new Label(Common.numberFormat.get().format(paketPerawatanDetail.getJumlah()) + " "
						+ (paketPerawatanDetail.getAlatMedis().getPer())));

				row.appendChild(new Label(
						paketPerawatanDetailPasien != null && paketPerawatanDetailPasien.getTransaksi() != null ? "Ya"
								: "Tidak"));

				if (paketPerawatanDetailPasien != null
						&& paketPerawatanDetailPasien.getAlatMedisDigantiDengan() != null) {
					adaGanti = !paketPerawatanDetailPasien.getAlatMedisDigantiDengan().getId()
							.equals(paketPerawatanDetailPasien.getPaketPerawatanDetail().getAlatMedis().getId());
				}

				row.appendChild(new Label(adaGanti ? "Ya" : "Tidak"));
			}

			final boolean tempAdaGanti = adaGanti;

			final MyDoublebox amountGanti = new MyDoublebox(
					paketPerawatanDetailPasien == null ? 0.0 : paketPerawatanDetailPasien.getAmountGanti());
			final Combobox jenisBiayaLain = new Combobox();

			EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					Session session = HibernateUtil.currentSession();
					PaketPerawatanDetailPasien paketPerawatanDetailPasien = (PaketPerawatanDetailPasien) session
							.createCriteria(PaketPerawatanDetailPasien.class)
							.add(Restrictions.eq("paketPerawatanDetail", paketPerawatanDetail))
							.add(Restrictions.eq("pendaftaran", pendaftaran)).setMaxResults(1).uniqueResult();
					if (paketPerawatanDetailPasien == null) {
						paketPerawatanDetailPasien = new PaketPerawatanDetailPasien();
						paketPerawatanDetailPasien.setPaketPerawatanDetail(paketPerawatanDetail);
						paketPerawatanDetailPasien.setItemDigantiDengan(paketPerawatanDetail.getItem());
						paketPerawatanDetailPasien.setRacikanDigantiDengan(paketPerawatanDetail.getRacikan());
						paketPerawatanDetailPasien.setTindakanDigantiDengan(paketPerawatanDetail.getTindakan());
						paketPerawatanDetailPasien.setAlatMedisDigantiDengan(paketPerawatanDetail.getAlatMedis());
						paketPerawatanDetailPasien.setJumlah(paketPerawatanDetail.getJumlah());
						paketPerawatanDetailPasien.setPendaftaran(pendaftaran);
					}
					paketPerawatanDetailPasien.setAmountGanti(amountGanti.getValue());
					paketPerawatanDetailPasien
							.setJenisBiayaLain((JenisBiayaLain) (jenisBiayaLain.getSelectedItem() == null ? null
									: jenisBiayaLain.getSelectedItem().getValue()));
					paketPerawatanDetailPasien.setAdaGanti(tempAdaGanti);
					session.saveOrUpdate(paketPerawatanDetailPasien);
				}
			};

			amountGanti.setWidth("95%");
			amountGanti.setDisabled(!adaGanti);
			amountGanti.setParent(row);
			amountGanti.addEventListener("onChange", eventListener);

			Common.insertCombo(jenisBiayaLain, "nama", "akun", JenisBiayaLain.class,
					Restrictions.eq("jenis", JenisBiayaLain.PENJUALAN));
			Common.selectComboItem(jenisBiayaLain,
					paketPerawatanDetailPasien == null ? null : paketPerawatanDetailPasien.getJenisBiayaLain());

			jenisBiayaLain.setWidth("95%");
			jenisBiayaLain.setParent(row);
			jenisBiayaLain.setDisabled(!adaGanti);
			jenisBiayaLain.addEventListener("onChange", eventListener);

		}
	}

	@SuppressWarnings("unchecked")
	public static boolean validasiTransaksiDetailPaketFinal(Set<Tindakan> pakets, final TransaksiMedis transaksi)
			throws Exception {
		Session session = HibernateUtil.currentSession();
		List<PaketPerawatanDetail> perawatanDetails = session.createCriteria(PaketPerawatanDetail.class).add(
				pakets.isEmpty() ? Restrictions.sqlRestriction("false") : Restrictions.in("paketPerawatan", pakets))
				.addOrder(Order.asc("paketPerawatan")).list();

		List<PaketPerawatanDetailPasien> perawatanDetailPasiens = new ArrayList<PaketPerawatanDetailPasien>();
		for (final PaketPerawatanDetail paketPerawatanDetail : perawatanDetails) {
			PaketPerawatanDetailPasien paketPerawatanDetailPasien = (PaketPerawatanDetailPasien) session
					.createCriteria(PaketPerawatanDetailPasien.class)
					.add(Restrictions.eq("paketPerawatanDetail", paketPerawatanDetail))
					.add(Restrictions.eq("pendaftaran", transaksi.getPendaftaran())).setMaxResults(1).uniqueResult();

			if (paketPerawatanDetailPasien != null && paketPerawatanDetailPasien.getAdaGanti()
					&& paketPerawatanDetailPasien.getAmountGanti() > 0.0
					&& paketPerawatanDetailPasien.getJenisBiayaLain() == null) {
				MyMessageboxConfig.show("Mohon Bapak/Ibu memilih Jenis Biaya untuk penggantian obat terlebih dahulu karena data ini wajib diisi. Langkah yang dapat dilakukan: (1) pilih Jenis Biaya penggantian obat; (2) lanjutkan kembali proses penyimpanan.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}

			if ((paketPerawatanDetail.getItem() != null || paketPerawatanDetail.getRacikan() != null)) {
				if (paketPerawatanDetailPasien == null) {
					MyMessageboxConfig.show("Mohon maaf, Obat atau Racikan \"" + paketPerawatanDetail.getItem().toString()
							+ "\" dengan jumlah " + paketPerawatanDetail.getJumlah() + " "
							+ (paketPerawatanDetail.getItem().getSatuanItem() == null ? ""
									: paketPerawatanDetail.getItem().getSatuanItem().getNama())
							+ " harus divalidasi terlebih dahulu. Langkah yang dapat dilakukan: (1) lakukan validasi terhadap item tersebut; (2) kemudian ulangi kembali proses ini.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				} else {
					paketPerawatanDetailPasien.setTransaksi(transaksi);
					session.update(paketPerawatanDetailPasien);
					perawatanDetailPasiens.add(paketPerawatanDetailPasien);
				}
			} else if (paketPerawatanDetail.getTindakan() != null) {
				if (paketPerawatanDetailPasien == null) {
					MyMessageboxConfig.show("Mohon maaf, Perawatan atau Tindakan \"" + paketPerawatanDetail.getTindakan().toString()
							+ "\" dengan jumlah " + paketPerawatanDetail.getJumlah() + " " + " harus divalidasi terlebih dahulu. Langkah yang dapat dilakukan: (1) lakukan validasi terhadap item tersebut; (2) kemudian ulangi kembali proses ini.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				} else {
					paketPerawatanDetailPasien.setTransaksi(transaksi);
					session.update(paketPerawatanDetailPasien);
					perawatanDetailPasiens.add(paketPerawatanDetailPasien);
				}
			} else if (paketPerawatanDetail.getAlatMedis() != null) {
				if (paketPerawatanDetailPasien == null) {
					MyMessageboxConfig.show(
							"Mohon maaf, Alat Medis atau Alat Kesehatan \"" + paketPerawatanDetail.getAlatMedis().toString()
									+ "\" dengan jumlah " + paketPerawatanDetail.getJumlah() + " "
									+ (paketPerawatanDetail.getAlatMedis().getPer() == null ? ""
											: paketPerawatanDetail.getAlatMedis().getPer())
									+ " harus divalidasi terlebih dahulu. Langkah yang dapat dilakukan: (1) lakukan validasi terhadap item tersebut; (2) kemudian ulangi kembali proses ini.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				} else {
					paketPerawatanDetailPasien.setTransaksi(transaksi);
					session.update(paketPerawatanDetailPasien);
					perawatanDetailPasiens.add(paketPerawatanDetailPasien);
				}
			}

		}

		transaksi.setValidasi(true);
		Common.refreshUpdate(session, (transaksi));

		session.createSQLQuery("delete from sirs.detail_transaksi_pasien where pendaftaran = "
				+ transaksi.getPendaftaran().getId() + ";").executeUpdate();

		session.createSQLQuery("delete from sirs.detail_transaksi_layanan where pendaftaran = "
				+ transaksi.getPendaftaran().getId() + ";").executeUpdate();

		for (PaketPerawatanDetailPasien paketPerawatanDetailPasien : perawatanDetailPasiens) {

			if ((paketPerawatanDetailPasien.getTindakanDigantiDengan() != null
					|| paketPerawatanDetailPasien.getAlatMedisDigantiDengan() != null)
					&& paketPerawatanDetailPasien.getAdaGanti()) {
				DetailTransaksiLayanan detailTransaksiLayanan = new DetailTransaksiLayanan();

				detailTransaksiLayanan.setPendaftaran(transaksi.getPendaftaran());

				detailTransaksiLayanan.setQtyBonus(0.0);
				detailTransaksiLayanan.setPasien(transaksi.getPasien());
				detailTransaksiLayanan.setPaketPerawatanDetailPasien(paketPerawatanDetailPasien);
				detailTransaksiLayanan.setTindakan(paketPerawatanDetailPasien.getTindakanDigantiDengan());
				detailTransaksiLayanan.setAlatMedis(paketPerawatanDetailPasien.getAlatMedisDigantiDengan());
				detailTransaksiLayanan.setAmount(paketPerawatanDetailPasien.getAmountGanti());
				if (paketPerawatanDetailPasien.getTindakanDigantiDengan() != null) {
					detailTransaksiLayanan.setKeterangan("Biaya ganti \""
							+ paketPerawatanDetailPasien.getPaketPerawatanDetail().getPaketPerawatan().toString()
							+ "\": " + paketPerawatanDetailPasien.getTindakanDigantiDengan().getNama());
				} else if (paketPerawatanDetailPasien.getAlatMedisDigantiDengan() != null) {
					detailTransaksiLayanan.setKeterangan("Biaya ganti \""
							+ paketPerawatanDetailPasien.getPaketPerawatanDetail().getPaketPerawatan().toString()
							+ "\": " + paketPerawatanDetailPasien.getAlatMedisDigantiDengan().getNama());
				}
				detailTransaksiLayanan.setKodeTransaksi(ConstantValues.apotikJual);
				detailTransaksiLayanan.setLokasi(transaksi.getLokasi());
				detailTransaksiLayanan.setTanggal(new Date());
				detailTransaksiLayanan.setDiskonPersen(0.0);
				detailTransaksiLayanan.setPajakPersen(0.0);

				detailTransaksiLayanan.setQty(1.0);
				session.saveOrUpdate(detailTransaksiLayanan);

				Biaya newBiaya = (Biaya) session.createCriteria(Biaya.class)
						.add(Restrictions.eq("jenisBiayaLain", paketPerawatanDetailPasien.getJenisBiayaLain()))
						.add(Restrictions.eq("detailTransaksiLayanan", detailTransaksiLayanan)).setMaxResults(1)
						.uniqueResult();
				if (newBiaya == null) {
					newBiaya = new Biaya();
				}
				newBiaya.setJumlah(paketPerawatanDetailPasien.getAmountGanti());
				newBiaya.setAkun(paketPerawatanDetailPasien.getJenisBiayaLain().getAkun());
				newBiaya.setJenisBiayaLain(paketPerawatanDetailPasien.getJenisBiayaLain());
				newBiaya.setDetailTransaksiLayanan(detailTransaksiLayanan);
				session.saveOrUpdate(newBiaya);

			} else if ((paketPerawatanDetailPasien.getItemDigantiDengan() != null
					|| paketPerawatanDetailPasien.getRacikanDigantiDengan() != null)
					&& paketPerawatanDetailPasien.getAdaGanti()) {
				DetailTransaksiPasien detailTransaksi = new DetailTransaksiPasien();
				detailTransaksi.setPendaftaran(transaksi.getPendaftaran());

				detailTransaksi.setQtyBonus(0.0);
				detailTransaksi.setPasien(transaksi.getPasien());
				detailTransaksi.setPaketPerawatanDetailPasien(paketPerawatanDetailPasien);
				detailTransaksi.setItem(paketPerawatanDetailPasien.getItemDigantiDengan());
				detailTransaksi.setRacikan(paketPerawatanDetailPasien.getRacikanDigantiDengan());
				detailTransaksi.setAmount(paketPerawatanDetailPasien.getAmountGanti());
				if (paketPerawatanDetailPasien.getItemDigantiDengan() != null) {
					detailTransaksi.setKeterangan("Biaya ganti \""
							+ paketPerawatanDetailPasien.getPaketPerawatanDetail().getPaketPerawatan().toString()
							+ "\": " + paketPerawatanDetailPasien.getItemDigantiDengan().getNama());
				} else if (paketPerawatanDetailPasien.getRacikanDigantiDengan() != null) {
					detailTransaksi.setKeterangan("Biaya ganti \""
							+ paketPerawatanDetailPasien.getPaketPerawatanDetail().getPaketPerawatan().toString()
							+ "\": " + paketPerawatanDetailPasien.getRacikanDigantiDengan().getNama());
				}
				detailTransaksi.setKodeTransaksi(ConstantValues.apotikJual);
				detailTransaksi.setLokasi(transaksi.getLokasi());
				detailTransaksi.setQty(1.0);
				detailTransaksi.setTanggal(new Date());
				detailTransaksi.setDiskonPersen(0.0);
				detailTransaksi.setPajakPersen(0.0);
				session.saveOrUpdate(detailTransaksi);

				Biaya newBiaya = (Biaya) session.createCriteria(Biaya.class)
						.add(Restrictions.eq("jenisBiayaLain", paketPerawatanDetailPasien.getJenisBiayaLain()))
						.add(Restrictions.eq("detailTransaksi", detailTransaksi)).setMaxResults(1).uniqueResult();
				if (newBiaya == null) {
					newBiaya = new Biaya();
				}
				newBiaya.setJumlah(paketPerawatanDetailPasien.getAmountGanti());
				newBiaya.setAkun(paketPerawatanDetailPasien.getJenisBiayaLain().getAkun());
				newBiaya.setJenisBiayaLain(paketPerawatanDetailPasien.getJenisBiayaLain());
				newBiaya.setDetailTransaksi(detailTransaksi);
				session.saveOrUpdate(newBiaya);

			}
		}

		for (Tindakan paket : pakets) {
			DetailTransaksiLayanan detailTransaksiLayanan = new DetailTransaksiLayanan();

			detailTransaksiLayanan.setQtyBonus(0.0);
			detailTransaksiLayanan.setPasien(transaksi.getPasien());
			detailTransaksiLayanan.setPendaftaran(transaksi.getPendaftaran());

			detailTransaksiLayanan.setTindakan(paket);
			detailTransaksiLayanan.setKeterangan("Harga paket \"" + paket.toString());
			detailTransaksiLayanan.setKodeTransaksi(ConstantValues.apotikJual);
			detailTransaksiLayanan.setLokasi(transaksi.getLokasi());
			detailTransaksiLayanan.setQty(1.0);
			detailTransaksiLayanan.setTanggal(new Date());
			detailTransaksiLayanan.setDiskonPersen(0.0);
			detailTransaksiLayanan.setPajakPersen(0.0);

			setDetailBiaya(detailTransaksiLayanan, transaksi.getKelasPerawatan(), session);

		}

		return true;
	}

	public static void validasiTransaksiItem(TransaksiMedis transaksi, String SUMBER, Boolean hapusDulu)
			throws Exception {
		Session session = HibernateUtil.currentSession();

		transaksi.setValidasi(true);
		Common.refreshUpdate(session, (transaksi));

		@SuppressWarnings("unchecked")
		List<TransaksiMedisDetail> transaksiDetails = session.createCriteria(TransaksiMedisDetail.class)
				.add(Restrictions.or(Restrictions.isNotNull("item"), Restrictions.isNotNull("racikan")))
				.add(Restrictions.eq("transaksi", transaksi)).list();

		if (hapusDulu) {
			session.createSQLQuery(
					"delete from sirs.detail_transaksi_pasien where racikan_detail in (select id from sirs.racikan_detail where racikan in (select id from sirs.racikan where transaksi_detail in (select id from sirs.transaksi_medis_detail where transaksi = "
							+ transaksi.getId() + ")));")
					.executeUpdate();

			session.createSQLQuery(
					"delete from sirs.detail_transaksi_pasien where transaksi_detail in (select id from sirs.transaksi_medis_detail where transaksi = "
							+ transaksi.getId() + ");")
					.executeUpdate();

			session.createSQLQuery(
					"delete from sirs.detail_transaksi_layanan where transaksi_detail in (select id from sirs.transaksi_medis_detail where transaksi = "
							+ transaksi.getId() + ");")
					.executeUpdate();
		}

		for (TransaksiMedisDetail transaksiDetail : transaksiDetails) {

			if (transaksiDetail.getItem() != null) {
				DetailTransaksiPasien detailTransaksi = new DetailTransaksiPasien();

				detailTransaksi.setQtyBonus(0.0);
				detailTransaksi.setPasien(transaksi.getPasien());
				detailTransaksi.setTransaksiDetail(transaksiDetail);
				detailTransaksi.setItem(transaksiDetail.getItem());
				detailTransaksi.setAmount(transaksiDetail.getAmount() == null ? 0.0 : transaksiDetail.getAmount());
				detailTransaksi.setKeterangan(transaksiDetail.getItem().getNama());
				detailTransaksi.setKodeTransaksi(ConstantValues.apotikJual);
				detailTransaksi.setLokasi(transaksi.getLokasi());
				detailTransaksi.setQty(transaksiDetail.getQty() == null ? 0.0 : transaksiDetail.getQty());
				detailTransaksi.setTanggal(new Date());

				detailTransaksi.setDiskonPersen(transaksiDetail.getDiskonPersen());
				detailTransaksi.setPajakPersen(transaksiDetail.getPajakPersen());
				CommonPendaftaranUtil.setDetailBiaya(detailTransaksi, transaksi.getKelasPerawatan());
			}

			if (transaksiDetail.getRacikan() != null) {

				// Simpan layanan Jasa racik

				DetailTransaksiLayanan detailTransaksiLayanan = new DetailTransaksiLayanan();

				detailTransaksiLayanan.setDiskon(0.0);
				detailTransaksiLayanan.setKeterangan(
						transaksiDetail.getTindakan() == null ? "-" : transaksiDetail.getTindakan().toString());
				detailTransaksiLayanan.setLokasi(transaksiDetail.getTransaksi().getLokasi());
				detailTransaksiLayanan.setPajak(0.0);
				detailTransaksiLayanan.setPasien(transaksiDetail.getTransaksi().getPasien());
				detailTransaksiLayanan.setQty(transaksiDetail.getQty() == null ? 0.0 : transaksiDetail.getQty());
				detailTransaksiLayanan.setQtyBonus(0.0);
				detailTransaksiLayanan.setTanggal(new Date());
				detailTransaksiLayanan.setTindakan(transaksiDetail.getTindakan());
				detailTransaksiLayanan.setPendaftaran(transaksiDetail.getTransaksi().getPendaftaran());

				detailTransaksiLayanan.setDiskonPersen(transaksiDetail.getDiskonPersen());
				detailTransaksiLayanan.setPajakPersen(transaksiDetail.getPajakPersen());

				detailTransaksiLayanan.setTransaksiDetail(transaksiDetail);

				detailTransaksiLayanan.setKodeTransaksi(ConstantValues.jasaRacik);

				CommonPendaftaranUtil.setDetailBiaya(detailTransaksiLayanan, transaksi.getKelasPerawatan());

				@SuppressWarnings("unchecked")
				List<RacikanDetail> racikanDetails = session.createCriteria(RacikanDetail.class)
						.add(Restrictions.eq("racikan", transaksiDetail.getRacikan())).list();

				for (RacikanDetail racikanDetail : racikanDetails) {
					ItemMedis item = racikanDetail.getItem();
					if (item == null) {
						continue;
					}

					DetailTransaksiPasien detailTransaksi = new DetailTransaksiPasien();

					detailTransaksi.setRacikanDetail(racikanDetail);
					detailTransaksi.setQtyBonus(0.0);
					detailTransaksi.setPasien(transaksi.getPasien());
					detailTransaksi.setTransaksiDetail(transaksiDetail);
					detailTransaksi.setItem(racikanDetail.getItem());
					detailTransaksi.setAmount(racikanDetail.getHargaTransaksi());
					detailTransaksi.setKeterangan(racikanDetail.getItem().getNama());
					detailTransaksi.setKodeTransaksi(ConstantValues.apotikJual);
					detailTransaksi.setLokasi(transaksi.getLokasi());
					detailTransaksi.setQty(racikanDetail.getJumlah() * detailTransaksiLayanan.getQty());
					detailTransaksi.setTanggal(new Date());

					detailTransaksi.setDiskonPersen(racikanDetail.getDiskonPersen());

					detailTransaksi.setPajakPersen(racikanDetail.getPajakPersen());

					CommonPendaftaranUtil.setDetailBiaya(detailTransaksi, transaksi.getKelasPerawatan());

				}
			}
		}
	}

	public static void validasiTransaksiAlatMedis(TransaksiMedis transaksi, String SUMBER, Boolean hapusDulu)
			throws Exception {

		Session session = HibernateUtil.currentSession();

		transaksi.setValidasi(true);
		Common.refreshUpdate(session, (transaksi));

		@SuppressWarnings("unchecked")
		List<TransaksiMedisDetail> transaksiDetails = session.createCriteria(TransaksiMedisDetail.class)
				.add(Restrictions.isNotNull("alatMedis")).add(Restrictions.eq("transaksi", transaksi)).list();

		if (hapusDulu) {
			session.createSQLQuery(
					"delete from sirs.detail_transaksi_layanan where transaksi_detail in (select id from sirs.transaksi_medis_detail where transaksi = "
							+ transaksi.getId() + ");")
					.executeUpdate();
		}

		for (TransaksiMedisDetail transaksiDetail : transaksiDetails) {

			if (transaksiDetail.getAlatMedis() != null) {

				AlatMedis alatMedis = transaksiDetail.getAlatMedis();

				DetailTransaksiLayanan detailTransaksiLayanan = new DetailTransaksiLayanan();

				if (SUMBER.equals(TransaksiMedis.SUMBER_LAB)) {
					detailTransaksiLayanan.setKodeTransaksi(ConstantValues.lab);
				} else if (SUMBER.equals(TransaksiMedis.SUMBER_OPERASI)) {
					detailTransaksiLayanan.setKodeTransaksi(ConstantValues.operasi);
				} else if (SUMBER.equals(TransaksiMedis.SUMBER_RADIOLOGI)) {
					detailTransaksiLayanan.setKodeTransaksi(ConstantValues.radiologi);
				} else if (SUMBER.equals(TransaksiMedis.SUMBER_VK)) {
					detailTransaksiLayanan.setKodeTransaksi(ConstantValues.vk);
				} else if (SUMBER.equals(TransaksiMedis.SUMBER_RENAL_UNIT)) {
					detailTransaksiLayanan.setKodeTransaksi(ConstantValues.renalUnit);
				} else if (SUMBER.equals(TransaksiMedis.SUMBER_GIZI)) {
					detailTransaksiLayanan.setKodeTransaksi(ConstantValues.gizi);
				} else if (SUMBER.equals(TransaksiMedis.SUMBER_UGD)) {
					detailTransaksiLayanan.setKodeTransaksi(ConstantValues.ugd);
				} else {
					detailTransaksiLayanan.setKodeTransaksi(ConstantValues.lain);
				}

				detailTransaksiLayanan.setAmount(transaksiDetail.getAmount());

				detailTransaksiLayanan.setDiskon(0.0);
				detailTransaksiLayanan.setKeterangan(alatMedis.getNama() + ", "
						+ (transaksiDetail.getMulai() == null ? ""
								: Common.dateFormat3.get().format(transaksiDetail.getMulai()))
						+ " s.d " + (transaksiDetail.getSampai() == null ? ""
								: Common.dateFormat3.get().format(transaksiDetail.getSampai())));
				detailTransaksiLayanan.setLokasi(transaksiDetail.getTransaksi().getLokasi());
				detailTransaksiLayanan.setPajak(0.0);
				detailTransaksiLayanan.setPasien(transaksiDetail.getTransaksi().getPasien());
				detailTransaksiLayanan.setQty(transaksiDetail.getQty() == null ? 0.0 : transaksiDetail.getQty());
				detailTransaksiLayanan.setQtyBonus(0.0);
				detailTransaksiLayanan.setTanggal(new Date());
				detailTransaksiLayanan.setAlatMedis(alatMedis);
				detailTransaksiLayanan.setPendaftaran(transaksiDetail.getTransaksi().getPendaftaran());
				detailTransaksiLayanan.setTransaksiDetail(transaksiDetail);

				detailTransaksiLayanan.setDiskonPersen(transaksiDetail.getDiskonPersen());

				detailTransaksiLayanan.setPajakPersen(transaksiDetail.getPajakPersen());

				CommonPendaftaranUtil.setDetailBiaya(detailTransaksiLayanan, transaksi.getKelasPerawatan());

			}
		}
	}

	@SuppressWarnings("unchecked")
	public static void validasiTransaksiLayanan(TransaksiMedis transaksi, String SUMBER, Boolean hapusDulu)
			throws Exception {
		Session session = HibernateUtil.currentSession();

		transaksi.setValidasi(true);
		Common.refreshUpdate(session, (transaksi));

		List<TransaksiMedisDetail> transaksiDetails = session.createCriteria(TransaksiMedisDetail.class)
				.add(Restrictions.isNotNull("tindakan")).add(Restrictions.eq("transaksi", transaksi)).list();

		if (hapusDulu) {
			session.createSQLQuery(
					"delete from sirs.detail_transaksi_layanan where transaksi_detail in (select id from sirs.transaksi_medis_detail where transaksi = "
							+ transaksi.getId() + ");")
					.executeUpdate();
		}

		for (TransaksiMedisDetail transaksiDetail : transaksiDetails) {

			if (transaksiDetail.getTindakan() != null) {

				Tindakan tindakan = transaksiDetail.getTindakan();

				DetailTransaksiLayanan detailTransaksiLayanan = new DetailTransaksiLayanan();

				if (SUMBER.equals(TransaksiMedis.SUMBER_LAB)) {
					detailTransaksiLayanan.setKodeTransaksi(ConstantValues.lab);
				} else if (SUMBER.equals(TransaksiMedis.SUMBER_OPERASI)) {
					detailTransaksiLayanan.setKodeTransaksi(ConstantValues.operasi);
				} else if (SUMBER.equals(TransaksiMedis.SUMBER_RADIOLOGI)) {
					detailTransaksiLayanan.setKodeTransaksi(ConstantValues.radiologi);
				} else if (SUMBER.equals(TransaksiMedis.SUMBER_VK)) {
					detailTransaksiLayanan.setKodeTransaksi(ConstantValues.vk);
				} else if (SUMBER.equals(TransaksiMedis.SUMBER_RENAL_UNIT)) {
					detailTransaksiLayanan.setKodeTransaksi(ConstantValues.renalUnit);
				} else if (SUMBER.equals(TransaksiMedis.SUMBER_GIZI)) {
					detailTransaksiLayanan.setKodeTransaksi(ConstantValues.gizi);
				} else if (SUMBER.equals(TransaksiMedis.SUMBER_UGD)) {
					detailTransaksiLayanan.setKodeTransaksi(ConstantValues.ugd);
				} else {
					detailTransaksiLayanan.setKodeTransaksi(ConstantValues.lain);
				}

				detailTransaksiLayanan.setAmount(transaksiDetail.getAmount());

				detailTransaksiLayanan.setDiskon(0.0);
				detailTransaksiLayanan.setKeterangan(tindakan.getNama());
				detailTransaksiLayanan.setLokasi(transaksiDetail.getTransaksi().getLokasi());
				detailTransaksiLayanan.setPajak(0.0);
				detailTransaksiLayanan.setPasien(transaksiDetail.getTransaksi().getPasien());
				detailTransaksiLayanan.setQty(transaksiDetail.getQty() == null ? 0.0 : transaksiDetail.getQty());
				detailTransaksiLayanan.setQtyBonus(0.0);
				detailTransaksiLayanan.setTanggal(new Date());
				detailTransaksiLayanan.setTindakan(tindakan);
				detailTransaksiLayanan.setPendaftaran(transaksiDetail.getTransaksi().getPendaftaran());
				detailTransaksiLayanan.setTransaksiDetail(transaksiDetail);

				detailTransaksiLayanan.setDiskonPersen(transaksiDetail.getDiskonPersen());

				detailTransaksiLayanan.setPajakPersen(transaksiDetail.getPajakPersen());

				CommonPendaftaranUtil.setDetailBiaya(detailTransaksiLayanan, transaksi.getKelasPerawatan());

			}
		}
	}
}
