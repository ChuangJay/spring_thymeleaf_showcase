package com.tamingthymeleaf.application.user;

import com.tamingthymeleaf.application.user.web.EditUserFormData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface UserService {
    // tag::create[]
    User createUser(CreateUserParameters parameters);

    User createAdministrator(CreateUserParameters parameters);
    // end::create[]

    User editUser(UserId userId, EditUserParameters parameters);

    Page<User> getUsers(Pageable pageable);

    boolean userWithEmailExists(Email email);

    Optional<User> getUser(UserId userId);

    void deleteUser(UserId userId);

    Page<User> getUsers(EditUserFormData formData, Pageable pageable);
}
