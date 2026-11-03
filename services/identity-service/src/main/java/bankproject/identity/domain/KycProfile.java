package bankproject.identity.domain;

import java.time.LocalDate;

import bankproject.identity.config.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class KycProfile {

    private String phone;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    private String gender;

    @Column(name = "address_line")
    private String addressLine;

    private String city;

    private String state;

    @Column(name = "postal_code")
    private String postalCode;

    /** Tax id (PAN). Encrypted at rest. */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "pan_number")
    private String panNumber;

    /** National id (Aadhaar). Encrypted at rest. */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "government_id")
    private String governmentId;
}
