package ais.action.servlet.api;

import ais.database.model.koperasi.CaraPembayaranKoperasi;

/** Self-test murni untuk rumus finansial yang dipakai koreksi transaksi POS. */
public final class KantinKoreksiTransaksiSelfTest {

    private KantinKoreksiTransaksiSelfTest() { }

    private static int gagal = 0;

    private static void check(boolean nilai, String pesan) {
        if (nilai) System.out.println("LULUS  " + pesan);
        else {
            gagal++;
            System.out.println("GAGAL  " + pesan);
        }
    }

    private static CaraPembayaranKoperasi cara(String nama, boolean manual,
            boolean deposit, boolean hutang) {
        CaraPembayaranKoperasi hasil = new CaraPembayaranKoperasi();
        hasil.setNama(nama);
        hasil.setManual(Boolean.valueOf(manual));
        hasil.setMemotongDeposit(Boolean.valueOf(deposit));
        hasil.setMasukSebagaiHutang(Boolean.valueOf(hutang));
        return hasil;
    }

    private static double nominal(double total, CaraPembayaranKoperasi utama,
            CaraPembayaranKoperasi kedua, double nominalKedua, boolean hutang) {
        return KantinHelper.nominalMetodeKhusus(total, utama,
                kedua, nominalKedua, null, 0.0, null, 0.0, null, 0.0, hutang);
    }

    public static void main(String[] args) {
        CaraPembayaranKoperasi tunai = cara("Tunai", true, false, false);
        CaraPembayaranKoperasi kasbon = cara("Kasbon", true, false, true);
        CaraPembayaranKoperasi saldoLama = cara("Saldo", false, false, false);
        CaraPembayaranKoperasi voucher = cara("Voucher", true, true, false);
		CaraPembayaranKoperasi rewardSantri = cara("Reward Santri", false, false, true);

        check(Math.abs(nominal(100.0, tunai, kasbon, 40.0, true) - 40.0) < 0.001,
                "slot kedua Kasbon hanya menambah hutang sebesar nominal slotnya");
        check(Math.abs(nominal(100.0, kasbon, tunai, 40.0, true) - 60.0) < 0.001,
                "slot utama dihitung dari sisa total setelah split-payment");
        check(Math.abs(nominal(100.0, saldoLama, null, 0.0, false) - 100.0) < 0.001,
                "metode legacy manual=false tetap dikenali sebagai pemotong deposit");
        check(Math.abs(nominal(100.0, voucher, null, 0.0, false) - 100.0) < 0.001,
                "flag memotong-deposit eksplisit dikenali walau metode manual");
        check(Math.abs(nominal(100.0, tunai, null, 0.0, false)) < 0.001,
                "metode biasa tidak salah dianggap memotong deposit");
		check(Math.abs(nominal(100.0, rewardSantri, null, 0.0, true) - 100.0) < 0.001,
				"Reward Santri manual=false tetap dihitung penuh sebagai hutang");
		check(Math.abs(nominal(100.0, rewardSantri, null, 0.0, false)) < 0.001,
				"Reward Santri tidak dihitung ganda sebagai pemotong deposit");

        check(Math.abs(KantinHelper.proyeksiHutangSetelahKoreksi(350.0, 100.0, 150.0)
                - 400.0) < 0.001,
                "proyeksi hutang mengganti kontribusi nota lama tanpa double-count");
        check(Math.abs(KantinHelper.proyeksiHutangSetelahKoreksi(50.0, 100.0, 100.0)
                - 50.0) < 0.001,
                "pembayaran hutang agregat yang sudah terjadi tidak hilang saat koreksi netral");
        check(Math.abs(KantinHelper.tambahanDepositKoreksi(75.0, 100.0) - 25.0) < 0.001,
                "tambahan pemotongan deposit dihitung sebagai delta positif");
        check(Math.abs(KantinHelper.tambahanDepositKoreksi(100.0, 75.0)) < 0.001,
                "pengurangan pemotongan deposit tidak ditolak sebagai penambahan saldo");
		check(KantinHelper.totalAlokasiPembayaranCocok(1500.0 + 23500.0, 25000.0),
				"split Tunai 1.500 + Voucher 23.500 cocok dengan total 25.000");
		check(!KantinHelper.totalAlokasiPembayaranCocok(1500.0 + 23000.0, 25000.0),
				"alokasi split yang kurang dari total ditolak");
		check(!KantinHelper.totalAlokasiPembayaranCocok(Double.NaN, 25000.0),
				"nominal bukan angka ditolak fail-closed");

		check(KantinHelper.bolehEditTransaksiDetail(true, true, true, false, false),
				"detail dapat diedit hanya ketika semua gerbang terpenuhi");
		check(!KantinHelper.bolehEditTransaksiDetail(false, true, true, false, false),
				"akun tanpa hak tidak mendapat tombol edit");
		check(!KantinHelper.bolehEditTransaksiDetail(true, true, false, false, false),
				"kebijakan nonaktif menutup tombol edit");
		check(!KantinHelper.bolehEditTransaksiDetail(true, true, true, true, false),
				"transaksi posting tidak mendapat tombol edit");
		check(!KantinHelper.bolehEditTransaksiDetail(true, true, true, false, true),
				"transaksi ber-retur tidak mendapat tombol edit");
		check(!KantinHelper.bolehEditTransaksiDetail(true, false, true, false, false),
				"baris legacy tanpa header tidak mendapat tombol edit");
		check(KantinHelper.bolehAktifkanKebijakanEditTransaksi(true, true, false, false),
				"admin atau supervisor dapat mengaktifkan kebijakan untuk transaksi yang layak");
		check(!KantinHelper.bolehAktifkanKebijakanEditTransaksi(true, true, true, false)
				&& !KantinHelper.bolehAktifkanKebijakanEditTransaksi(true, true, false, true),
				"aktivasi tidak ditawarkan untuk transaksi posting atau ber-retur");

        System.out.println(gagal == 0
                ? "SEMUA ATURAN KOREKSI TRANSAKSI TERJAGA"
                : ("ADA " + gagal + " ATURAN YANG DILANGGAR"));
        if (gagal > 0) System.exit(1);
    }
}
