package com.tamingthymeleaf.application.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Transactional(readOnly = true)
public interface UserRepository extends CrudRepository<User, UserId>, PagingAndSortingRepository<User, UserId>, UserRepositoryCustom, JpaSpecificationExecutor<User> {
    boolean existsByEmail(Email email);

    Optional<User> findByEmail(Email email);

    Page<User> findByBirthday(LocalDate birthday, Pageable pageable);
}
