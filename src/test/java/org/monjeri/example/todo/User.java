package org.monjeri.example.todo;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class User {

  private final String username;
  private final String firstname;
  private final String lastname;
  private final String email;
  private final boolean active;
  private final Instant created;
  private final String password;
  private final boolean admin;

  public User(String username, String firstname, String lastname, String email, boolean active, Instant created, String password, boolean admin) {
    this.username = username;
    this.firstname = firstname;
    this.lastname = lastname;
    this.email = email;
    this.active = active;
    this.created = created;
    this.password = password;
    this.admin = admin;
  }

  public User(String username, String password, boolean admin) {
    this(username, null, null, null, true, Instant.now(), password, admin);
  }

  public boolean isAdmin() {
    return admin;
  }

  public String getPassword() {
    return password;
  }

  public String getUsername() {
    return username;
  }

  public String getFirstname() {
    return firstname;
  }

  public String getLastname() {
    return lastname;
  }

  public String getEmail() {
    return email;
  }

  public boolean isActive() {
    return active;
  }

  public Instant getCreated() {
    return created;
  }

  public User merge(User other) {
    return new User(
        Optional.ofNullable(other.username).orElse(username),
        Optional.ofNullable(other.firstname).orElse(firstname),
        Optional.ofNullable(other.lastname).orElse(lastname),
        Optional.ofNullable(other.email).orElse(email),
        other.active,
        Optional.ofNullable(other.created).orElse(created),
        Optional.ofNullable(other.password).orElse(password),
        other.admin
    );
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    User user = (User) o;
    return active == user.active &&
        admin == user.admin &&
        Objects.equals(username, user.username) &&
        Objects.equals(firstname, user.firstname) &&
        Objects.equals(lastname, user.lastname) &&
        Objects.equals(email, user.email) &&
        Objects.equals(created, user.created) &&
        Objects.equals(password, user.password);
  }

  @Override
  public int hashCode() {
    return Objects.hash(username, firstname, lastname, email, active, created, password, admin);
  }

  @Override
  public String toString() {
    return "User{" +
        "username='" + username + '\'' +
        ", firstname='" + firstname + '\'' +
        ", lastname='" + lastname + '\'' +
        ", email='" + email + '\'' +
        ", active=" + active +
        ", created=" + created +
        ", password=***" +
        ", admin=" + admin +
        '}';
  }
}
