package tacos;
import java.util.Collections;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.
                                          SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Data
@NoArgsConstructor(access=AccessLevel.PRIVATE, force=true)
@Document
public class User implements UserDetails {

  private static final long serialVersionUID = 1L;

  @Id
  private String id;
  
  private final String username;
  
  @JsonIgnore //modificacion para TC-10
  @ToString.Exclude
  private final String password;
  private final String fullname;
  private final String street;
  private final String city;
  private final String state;
  private final String zip;
  private final String phoneNumber;
  private final String email;
  private Set<String> roles = new HashSet<>(Collections.singleton("ROLE_USER")); //modificacion para TC-11

  //TC-11 - Constructor explícito para reconstruir usuarios almacenados en Mongo
  @PersistenceConstructor
  public User(String username, String password, String fullname, String street,
      String city, String state, String zip, String phoneNumber, String email) {
    this.username = username;
    this.password = password;
    this.fullname = fullname;
    this.street = street;
    this.city = city;
    this.state = state;
    this.zip = zip;
    this.phoneNumber = phoneNumber;
    this.email = email;
  }
  //Fin TC-11
  
  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    //modificacion para TC-11
    Set<String> effectiveRoles = roles == null || roles.isEmpty()
        ? Collections.singleton("ROLE_USER") : roles;
    return effectiveRoles.stream()
        .map(SimpleGrantedAuthority::new)
        .collect(Collectors.toList());
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

}
