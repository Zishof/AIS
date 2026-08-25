package ais.database.model.sosial;

import java.util.Date;
import javax.persistence.*;
import org.hibernate.envers.Audited;
import ais.database.model.Tbmuser;

@Entity @org.hibernate.annotations.Entity(dynamicInsert=true,dynamicUpdate=true) @Audited
@Table(schema="public",name="social_donor_identity",uniqueConstraints={@UniqueConstraint(columnNames={"tenant_key","tbmuser_id"}),@UniqueConstraint(columnNames={"tenant_key","donatur_id"})})
public class SocialDonorIdentity extends SocialRecord {
    private static final long serialVersionUID=1L;
    private Donatur donatur; private Tbmuser tbmuser; private String displayName,email,phone,donorType,privacyConsentVersion;
    private Boolean externalMember,anonymousDefault,communicationConsent; private Date lastLogin;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="donatur_id") public Donatur getDonatur(){return donatur;} public void setDonatur(Donatur v){donatur=v;}
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="tbmuser_id") public Tbmuser getTbmuser(){return tbmuser;} public void setTbmuser(Tbmuser v){tbmuser=v;}
    @Column(name="display_name",nullable=false,length=255) public String getDisplayName(){return displayName;} public void setDisplayName(String v){displayName=trim(v);}
    @Column(name="email",length=320) public String getEmail(){return email;} public void setEmail(String v){email=trim(v);}
    @Column(name="phone",length=40) public String getPhone(){return phone;} public void setPhone(String v){phone=trim(v);}
    @Column(name="donor_type",length=40) public String getDonorType(){return donorType;} public void setDonorType(String v){donorType=trim(v);}
    @Column(name="privacy_consent_version",length=40) public String getPrivacyConsentVersion(){return privacyConsentVersion;} public void setPrivacyConsentVersion(String v){privacyConsentVersion=trim(v);}
    @Column(name="external_member") public Boolean getExternalMember(){return Boolean.TRUE.equals(externalMember);} public void setExternalMember(Boolean v){externalMember=v;}
    @Column(name="anonymous_default") public Boolean getAnonymousDefault(){return Boolean.TRUE.equals(anonymousDefault);} public void setAnonymousDefault(Boolean v){anonymousDefault=v;}
    @Column(name="communication_consent") public Boolean getCommunicationConsent(){return Boolean.TRUE.equals(communicationConsent);} public void setCommunicationConsent(Boolean v){communicationConsent=v;}
    @Temporal(TemporalType.TIMESTAMP) @Column(name="last_login") public Date getLastLogin(){return lastLogin;} public void setLastLogin(Date v){lastLogin=v;}
}
