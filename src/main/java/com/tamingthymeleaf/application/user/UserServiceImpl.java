package com.tamingthymeleaf.application.user;

import com.tamingthymeleaf.application.user.web.EditUserFormData;
import jakarta.persistence.criteria.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserServiceImpl implements UserService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    // tag::create[]
    @Override
    public User createUser(CreateUserParameters parameters) {
        LOGGER.debug("Creating user {} ({})", parameters.getUserName().getFullName(), parameters.getEmail().asString());
        UserId userId = repository.nextId();
        String encodedPassword = passwordEncoder.encode(parameters.getPassword()); //<.>
        User user = User.createUser(userId,
                parameters.getUserName(),
                encodedPassword,
                parameters.getGender(),
                parameters.getBirthday(),
                parameters.getEmail(),
                parameters.getPhoneNumber());
        return repository.save(user);
    }

    @Override
    public User createAdministrator(CreateUserParameters parameters) {
        LOGGER.debug("Creating administrator {} ({})", parameters.getUserName().getFullName(), parameters.getEmail().asString());
        UserId userId = repository.nextId();
        User user = User.createAdministrator(userId,
                parameters.getUserName(),
                passwordEncoder.encode(parameters.getPassword()),
                parameters.getGender(),
                parameters.getBirthday(),
                parameters.getEmail(),
                parameters.getPhoneNumber());
        return repository.save(user);
    }
    // end::create[]

    // tag::editUser[]
    @Override
    public User editUser(UserId userId, EditUserParameters parameters) {
        User user = repository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId)); //<.>

        if (parameters.getVersion() != user.getVersion()) { //<.>
            throw new ObjectOptimisticLockingFailureException(User.class, user.getId().asString());
        }

        parameters.update(user); //<.>
        return user;
    }
    // end::editUser[]

    @Override
    @Transactional(readOnly = true)
    public Page<User> getUsers(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean userWithEmailExists(Email email) {
        return repository.existsByEmail(email);
    }

    @Override
    public Optional<User> getUser(UserId userId) {
        return repository.findById(userId);
    }

    // tag::deleteUser[]
    @Override
    public void deleteUser(UserId userId) {
        repository.deleteById(userId); //<.>
    }

    public Page<User> getUsers(EditUserFormData formData, Pageable pageable) {

        List<FormSpecification> paramList = new ArrayList<>();
        // Birthday
        if (formData.getBirthday() != null) {
            FormSpecification spec =
                    new FormSpecification(
                            new SearchCriteria("birthday","", "=", formData.getBirthday()));
            paramList.add(spec);
        }

        // first_name
        if (StringUtils.hasText(formData.getFirstName())) {
            FormSpecification spec =
                    new FormSpecification(
                            new SearchCriteria("userName", "firstName","like", formData.getFirstName()));
            paramList.add(spec);
        }

        // last_name
        if (StringUtils.hasText(formData.getLastName())) {
            FormSpecification spec =
                    new FormSpecification(
                            new SearchCriteria("userName", "lastName","like", formData.getLastName()));
            paramList.add(spec);
        }

        // gengder
        if (formData.getGender() != null) {
            FormSpecification spec =
                    new FormSpecification(
                            new SearchCriteria("gender","", "=", formData.getGender()));
            paramList.add(spec);
        }

        // TODO Specification 做不定參數查詢，如果是要以 join table 的 field 做查詢。需要其他的作法
//        // user_role
//        if (formData.getUserRole() != null) {
//            FormSpecification spec =
//                    new FormSpecification(
//                            new SearchCriteria("roles", "","=", formData.getUserRole().name()));
//            paramList.add(spec);
//        }

        // email
        if (StringUtils.hasText(formData.getEmail())) {
            FormSpecification spec =
                    new FormSpecification(
                            new SearchCriteria("email", "","like", formData.getEmail()));
            paramList.add(spec);
        }

        // phoneNumber
        if (StringUtils.hasText(formData.getPhoneNumber())) {
            FormSpecification spec =
                    new FormSpecification(
                            new SearchCriteria("phoneNumber", "","like", formData.getPhoneNumber()));
            paramList.add(spec);
        }

        Page<User> xx;
        if (!paramList.isEmpty()) {
            Specification resultSpecfication = paramList.get(0);
            for (int i = 1; i < paramList.size(); i++) {
                resultSpecfication = Specification.where(resultSpecfication).and(paramList.get(i));
            }
            xx = repository.findAll(resultSpecfication, pageable);
        } else {
            xx = repository.findAll(pageable);

        }
        return xx;
    }


    @Data
    @AllArgsConstructor
    class SearchCriteria {
        private String key;
        private String subKey;
        private String operation;
        private Object value;
    }


    @AllArgsConstructor
    class FormSpecification implements Specification<EditUserFormData> {

        private SearchCriteria criteria;

        @Override
        public Predicate toPredicate(Root<EditUserFormData> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
            if (criteria.getOperation().equalsIgnoreCase(">=_OR_IsNull")) {
                // ex: (startDate >= OR  StatDate is NUll)
                Predicate x = builder.greaterThanOrEqualTo(
                        root.<LocalDateTime>get(criteria.getKey()), (LocalDateTime) criteria.getValue());
                Predicate y = builder.isNull(
                        root.<LocalDateTime>get(criteria.getKey()));
                return builder.or(x, y);
            } else if (criteria.getOperation().equalsIgnoreCase("<=_OR_IsNull")) {
                Predicate x = builder.lessThanOrEqualTo(
                        root.<LocalDateTime>get(criteria.getKey()), (LocalDateTime) criteria.getValue());
                Predicate y = builder.isNull(
                        root.<LocalDateTime>get(criteria.getKey()));
                return builder.or(x, y);
            } else if (criteria.getOperation().equalsIgnoreCase("=")) {
                return builder.equal(root.get(criteria.getKey()), criteria.getValue());
            } else if (criteria.getOperation().equalsIgnoreCase("like")) {
                if (!criteria.getSubKey().isBlank()) {
                    return builder.like(root.get(criteria.getKey()).get(criteria.getSubKey()), "%" + criteria.getValue() + "%");
                } else {
                    return builder.like(root.get(criteria.getKey()).as(String.class), "%" + criteria.getValue() + "%");
                }

            } else if (criteria.getOperation().equalsIgnoreCase("in")) {
                Expression<String> exp = root.<String>get(criteria.getKey());
                List<Predicate> list = new ArrayList<>();
                List<String> list_source = (List<String>) criteria.getValue();
                list.add(exp.in(list_source));
                if (list.size() != 0) {
                    Predicate[] p = new Predicate[list.size()];
                    return builder.and(list.toArray(p));
                }
            }
            return null;
        }
    }

    // end::deleteUser[]
}
