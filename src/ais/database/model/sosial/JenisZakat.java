package ais.database.model.sosial;

import javax.persistence.*;
import org.hibernate.envers.Audited;

@Entity @Audited @org.hibernate.annotations.Entity(dynamicInsert=true,dynamicUpdate=true)
@Table(schema="public",name="jenis_zakat",uniqueConstraints=@UniqueConstraint(columnNames={"tenant_key","kode"}))
public class JenisZakat extends SocialRecord {
    private static final long serialVersionUID=1L; private String kode,nama,description,formulaKey; private Boolean active; private Integer sortOrder;
    @Column(name="kode",nullable=false,length=50) public String getKode(){return kode;} public void setKode(String v){kode=trim(v);}
    @Column(name="nama",nullable=false,length=255) public String getNama(){return nama;} public void setNama(String v){nama=trim(v);}
    @Column(name="description",columnDefinition="TEXT") public String getDescription(){return description;} public void setDescription(String v){description=v;}
    @Column(name="formula_key",nullable=false,length=60) public String getFormulaKey(){return formulaKey;} public void setFormulaKey(String v){formulaKey=trim(v);}
    @Column(name="active") public Boolean getActive(){return !Boolean.FALSE.equals(active);} public void setActive(Boolean v){active=v;}
    @Column(name="sort_order") public Integer getSortOrder(){return sortOrder==null?0:sortOrder;} public void setSortOrder(Integer v){sortOrder=v;}
}
