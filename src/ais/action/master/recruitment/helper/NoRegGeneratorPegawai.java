package ais.action.master.recruitment.helper;

import java.util.List;

import ais.database.model.recruitment.CalonPegawai;


public interface NoRegGeneratorPegawai {

	public String generateNoReg(CalonPegawai calonPegawai);

	public String generateNoReg(List<String> noRegPengecualian, CalonPegawai calonPegawai);

}
