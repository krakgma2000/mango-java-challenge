package com.mango.products.security;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import com.mango.products.entity.UserGrantEntity;
import com.mango.products.repository.CredentialsRepository;

@Component
public class GrantRegistry {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final Map<String, List<String>> grantsByUser;

    @Autowired
    public GrantRegistry(CredentialsRepository credentialsRepository) {
        this(credentialsRepository.findAllGrants());
    }

    GrantRegistry(List<UserGrantEntity> grants) {
        this.grantsByUser = grants.stream().collect(Collectors.groupingBy(UserGrantEntity::getUsername,
                Collectors.mapping(UserGrantEntity::getEndpoint, Collectors.toUnmodifiableList())));
    }

    public boolean isGranted(String username, String path) {
        return grantsByUser.getOrDefault(username, List.of()).stream()
                .anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }
}
