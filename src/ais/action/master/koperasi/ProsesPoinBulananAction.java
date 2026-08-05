package ais.action.master.koperasi;

import java.util.Calendar;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Textbox;

import ais.common.Common;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Layar admin untuk memicu {@link ProsesPoinBulananHelper#prosesUntukBulan(int, int, String)} --
 * fitur "Voucher Pegawai". Dijalankan admin MANUAL bersamaan waktu proses gaji bulanan (syarat E
 * spesifikasi); lihat JavaDoc {@link ProsesPoinBulananHelper} untuk penjelasan kenapa ini sengaja
 * TIDAK dikaitkan otomatis ke posting gaji.
 *
 * <p>Idempoten -- boleh diklik berkali-kali untuk bulan yang sama, voucher yang sudah pernah
 * diterbitkan untuk (pegawai, kegiatan, periode) yang sama tidak akan diterbitkan dobel (lihat
 * ringkasan "dilewati" pada hasil).</p>
 */
public class ProsesPoinBulananAction extends GenericAutowireComposer {

	private static final long serialVersionUID = 1L;

	private static final String[] NAMA_BULAN = {
			"Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September",
			"Oktober", "November", "Desember"
	};

	private Combobox cboBulan;
	private Textbox txtTahun;
	private Label hasilLabel;
	private MyToolbarbuttonConfig btnProses;

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

		Calendar sekarang = Calendar.getInstance();
		for (int i = 0; i < NAMA_BULAN.length; i++) {
			Comboitem item = new Comboitem(NAMA_BULAN[i]);
			item.setValue(Integer.valueOf(i + 1));
			item.setParent(cboBulan);
			if (i == sekarang.get(Calendar.MONTH)) {
				cboBulan.setSelectedItem(item);
			}
		}
		txtTahun.setValue(String.valueOf(sekarang.get(Calendar.YEAR)));
	}

	public void onClick$btnProses(Event event) throws Exception {
		Comboitem terpilih = cboBulan.getSelectedItem();
		if (terpilih == null) {
			return;
		}
		int bulan = ((Integer) terpilih.getValue()).intValue();
		int tahun;
		try {
			tahun = Integer.parseInt(txtTahun.getValue().trim());
		} catch (Exception e) {
			MyMessageboxConfig.show("Tahun tidak valid.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		}

		String konfirmasi = "Proses poin voucher untuk periode " + terpilih.getLabel() + " " + tahun
				+ "?\n\nAman dijalankan berkali-kali -- voucher yang sudah pernah diterbitkan untuk "
				+ "periode ini tidak akan diterbitkan dobel.";
		int jawaban = MyMessageboxConfig.show(konfirmasi, "Konfirmasi", MyMessageboxConfig.YES | MyMessageboxConfig.NO,
				MyMessageboxConfig.QUESTION);
		if (jawaban != MyMessageboxConfig.YES) {
			return;
		}

		try {
			String prosesOleh = Common.getCurrentUser() == null ? "admin" : Common.getCurrentUser().getUserId();
			ProsesPoinBulananHelper.HasilProsesPoin hasil = ProsesPoinBulananHelper.prosesUntukBulan(tahun, bulan,
					prosesOleh);

			String ringkasan = "Selesai. Pegawai diproses: " + hasil.pegawaiDiproses + ", voucher diterbitkan: "
					+ hasil.voucherDiterbitkan + " (total Rp " + String.valueOf((long) hasil.totalNominal)
					+ "), dilewati (sudah pernah diterbitkan): " + hasil.dilewatiSudahAda + ".";
			hasilLabel.setValue(ringkasan);
			MyMessageboxConfig.show(ringkasan, "Info", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			try {
				MyMessageboxConfig.show("Gagal memproses poin, keterangan: " + e.getMessage(), "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/koperasi/ProsesPoinBulananAction.java"); }
		}
	}

}
